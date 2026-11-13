package com.nhyle.hyperionmaritime.data.repository

import android.content.Context
import com.nhyle.hyperionmaritime.BuildConfig
import com.nhyle.hyperionmaritime.data.auth.SupabaseAccountManager
import com.nhyle.hyperionmaritime.data.model.QuestionImageRef
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

/**
 * Licensed on-demand question images, stored in Android's private filesDir.
 * No public URLs, image bytes inside APK/CSV, or background Supabase polling.
 * Files are scoped to owner + paid track, named by SHA-256, and reverified
 * before display. A saved image never grants access after license expiry.
 */
internal object VerifiedQuestionMediaStore {
    private const val MAX_BYTES = 524288
    private val uuid = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
    private val sha = Regex("^[a-f0-9]{64}$")
    private fun extension(mime: String): String? = when (mime) {
        "image/webp" -> "webp"
        "image/png" -> "png"
        "image/jpeg" -> "jpg"
        else -> null
    }
    private fun fileFor(context: Context, owner: String, track: Track, ref: QuestionImageRef): File {
        require(uuid.matches(owner) && sha.matches(ref.sha256))
        val ext = requireNotNull(extension(ref.mime))
        return File(context.filesDir, "verified-question-media-v1/${owner.lowercase()}/${track.name}/${ref.sha256}.$ext")
    }
    private fun signatureMatches(bytes: ByteArray, mime: String): Boolean {
        if (bytes.size !in 16..MAX_BYTES) return false
        return when (mime) {
            "image/png" -> listOf(137,80,78,71,13,10,26,10)
                .allIndexed { i, value -> (bytes[i].toInt() and 255) == value }
            "image/jpeg" -> (bytes[0].toInt() and 255) == 255 &&
                (bytes[1].toInt() and 255) == 216 && (bytes[2].toInt() and 255) == 255 &&
                (bytes[bytes.size-2].toInt() and 255) == 255 &&
                (bytes[bytes.size-1].toInt() and 255) == 217
            "image/webp" -> String(bytes.copyOfRange(0,4),Charsets.US_ASCII) == "RIFF" &&
                String(bytes.copyOfRange(8,12),Charsets.US_ASCII) == "WEBP"
            else -> false
        }
    }
    private inline fun <T> List<T>.allIndexed(predicate: (Int,T)->Boolean):Boolean =
        indices.all { predicate(it, get(it)) }
    private fun verifies(bytes: ByteArray, ref: QuestionImageRef): Boolean =
        signatureMatches(bytes, ref.mime) &&
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) } == ref.sha256

    private suspend fun boundedBytes(connection: HttpURLConnection): ByteArray {
        val declared = connection.contentLength
        if (declared > MAX_BYTES) throw IllegalStateException("Image exceeds private media limit")
        val output = ByteArrayOutputStream()
        connection.inputStream.use { input ->
            val part = ByteArray(8192)
            while (true) {
                currentCoroutineContext().ensureActive()
                val len = input.read(part)
                if (len < 0) break
                if (output.size() + len > MAX_BYTES) throw IllegalStateException("Oversized image")
                output.write(part,0,len)
            }
        }
        return output.toByteArray()
    }

    suspend fun load(context: Context, track: Track, ref: QuestionImageRef): ByteArray? =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            if (!LicenseManager.isTrackProUnlocked(app, track)) return@withContext null
            val owner = LicenseManager.getCurrentAccountUserId(app)
                ?.takeIf { uuid.matches(it) } ?: return@withContext null
            val destination = runCatching { fileFor(app,owner,track,ref) }.getOrNull()
                ?: return@withContext null
            if (destination.isFile) {
                val local = runCatching {
                    if (destination.length() !in 16L..MAX_BYTES.toLong()) null
                    else destination.readBytes()
                }.getOrNull()
                if (local != null && verifies(local,ref) &&
                    LicenseManager.isTrackProUnlocked(app,track) &&
                    LicenseManager.getCurrentAccountUserId(app) == owner) {
                    return@withContext local
                }
                destination.delete()
            }

            val session = try {
                SupabaseAccountManager.authenticatedQuestionMediaSession(app)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            val (authenticatedOwner, jwt) = session ?: return@withContext null
            currentCoroutineContext().ensureActive()
            if (authenticatedOwner != owner) return@withContext null
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/functions/v1/question-media?track=${track.name}&sha256=${ref.sha256}&mime=${java.net.URLEncoder.encode(ref.mime,"UTF-8")}"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection)
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.setRequestProperty("Authorization","Bearer $jwt")
                connection.setRequestProperty("apikey",BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                connection.setRequestProperty("Accept",ref.mime)
                if (connection.responseCode != 200 ||
                    connection.getHeaderField("X-Hyperion-Media-SHA256") != ref.sha256 ||
                    connection.contentType?.substringBefore(';') != ref.mime) {
                    return@withContext null
                }
                val bytes = boundedBytes(connection)
                currentCoroutineContext().ensureActive()
                if (!verifies(bytes,ref) ||
                    !LicenseManager.isTrackProUnlocked(app,track) ||
                    LicenseManager.getCurrentAccountUserId(app) != owner) return@withContext null
                // Atomic local-only cache. A duplicate on-demand download must
                // never leave partial bytes under a valid hash filename.
                runCatching {
                    destination.parentFile?.mkdirs()
                    val tmp = File(destination.parentFile,"${UUID.randomUUID()}.part")
                    try {
                        FileOutputStream(tmp).use { out ->
                            out.write(bytes)
                            out.fd.sync()
                        }
                        if (!tmp.renameTo(destination) && !destination.exists()) {
                            throw IllegalStateException("Image could not be cached")
                        }
                    } finally { tmp.delete() }
                }
                currentCoroutineContext().ensureActive()
                if (LicenseManager.getCurrentAccountUserId(app) == owner &&
                    LicenseManager.isTrackProUnlocked(app, track)) bytes else null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }
}
