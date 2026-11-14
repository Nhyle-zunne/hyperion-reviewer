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
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.IOException
import com.nhyle.hyperionmaritime.data.util.AssetCrypto
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

internal enum class QuestionMediaFailure(val description: String) {
    LICENSE_REQUIRED("Diagram unavailable. An active license for this track is required."),
    SIGN_IN_REQUIRED("Diagram unavailable. This image is not bundled in this app version. Sign in to download the newer diagram, or update the app."),
    SESSION_EXPIRED("Diagram unavailable. Your saved sign-in could not be refreshed. Open Account to reconnect, or update the app to use its bundled diagrams."),
    NOT_AUTHORIZED("Diagram unavailable. The image service could not verify this account’s track access. Sync your license or update the app."),
    NOT_FOUND("Diagram unavailable. This diagram is not included in this app version and could not be found online. Update the app."),
    CONNECTION_FAILED("Diagram unavailable. This image is not bundled in this app version and the download failed. Retry when connected, or update the app."),
    INTEGRITY_FAILED("Diagram unavailable. The image failed verification. Retry or update the app."),
    BUNDLE_DAMAGED("Diagram unavailable. This app’s bundled image is damaged. Install the latest app update."),
    ACCOUNT_CHANGED("Diagram unavailable. The active account or track access changed. Reopen the question."),
    SERVICE_UNAVAILABLE("Diagram unavailable. The image service is temporarily unavailable. Retry or update the app."),
}

internal class QuestionMediaException(val reason: QuestionMediaFailure) : IOException(reason.description)

/** Encrypted APK diagrams first; owner-scoped cache/network only for newer refs.
 * Bundled images use the same local paid-track gate as the packaged questions,
 * including verified admin, Google Play and device licenses without a sign-in.
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

    internal fun bundledPath(track: Track, ref: QuestionImageRef): String {
        if (!sha.matches(ref.sha256)) throw QuestionMediaException(QuestionMediaFailure.INTEGRITY_FAILED)
        val ext = extension(ref.mime) ?: throw QuestionMediaException(QuestionMediaFailure.INTEGRITY_FAILED)
        return "question-media/${track.name}/${ref.sha256}.$ext.enc"
    }

    internal fun readBundled(track: Track, ref: QuestionImageRef, openAsset: (String) -> InputStream): ByteArray? {
        val path = bundledPath(track, ref)
        val input = try {
            openAsset(path)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: FileNotFoundException) {
            return null
        } catch (_: IOException) {
            throw QuestionMediaException(QuestionMediaFailure.BUNDLE_DAMAGED)
        }
        return try {
            val encrypted = input.use { stream ->
                val output = ByteArrayOutputStream()
                val part = ByteArray(8192)
                while (true) {
                    val count = stream.read(part)
                    if (count < 0) break
                    if (output.size() + count > MAX_BYTES + 28) throw IOException("Oversized bundled diagram")
                    output.write(part, 0, count)
                }
                output.toByteArray()
            }
            val bytes = AssetCrypto.decrypt(encrypted)
            if (!verifies(bytes, ref)) throw IOException("Invalid bundled diagram")
            bytes
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw QuestionMediaException(QuestionMediaFailure.BUNDLE_DAMAGED)
        }
    }

    suspend fun load(context: Context, track: Track, ref: QuestionImageRef): ByteArray? = try {
        loadForDisplay(context, track, ref)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) { null }

    suspend fun loadForDisplay(
        context: Context,
        track: Track,
        ref: QuestionImageRef,
        openBundledAsset: (String) -> InputStream = { context.assets.open(it) },
    ): ByteArray = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        if (!LicenseManager.isTrackProUnlocked(app, track)) throw QuestionMediaException(QuestionMediaFailure.LICENSE_REQUIRED)
        val owner = LicenseManager.getCurrentAccountUserId(app)
        fun stillAllowed() = LicenseManager.isTrackProUnlocked(app, track) &&
            LicenseManager.getCurrentAccountUserId(app) == owner

        readBundled(track, ref, openBundledAsset)?.let { bundled ->
            currentCoroutineContext().ensureActive()
            if (!stillAllowed()) throw QuestionMediaException(QuestionMediaFailure.ACCOUNT_CHANGED)
            return@withContext bundled
        }
        // Old saved sessions can refer to images added after this APK was built.
        // Only that optional fallback needs an authenticated account and network.
        val validOwner = owner?.takeIf { uuid.matches(it) }
            ?: throw QuestionMediaException(QuestionMediaFailure.SIGN_IN_REQUIRED)
        val destination = fileFor(app, validOwner, track, ref)
        if (destination.isFile) {
            val local = runCatching {
                if (destination.length() !in 16L..MAX_BYTES.toLong()) null else destination.readBytes()
            }.getOrNull()
            if (local != null && verifies(local, ref) && stillAllowed()) return@withContext local
            destination.delete()
        }

        val session = try {
            SupabaseAccountManager.authenticatedQuestionMediaSession(app)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { null }
        val (authenticatedOwner, jwt) = session ?: throw QuestionMediaException(QuestionMediaFailure.SESSION_EXPIRED)
        currentCoroutineContext().ensureActive()
        if (authenticatedOwner != validOwner || !stillAllowed()) throw QuestionMediaException(QuestionMediaFailure.ACCOUNT_CHANGED)
        val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
            "/functions/v1/question-media?track=${track.name}&sha256=${ref.sha256}&mime=${java.net.URLEncoder.encode(ref.mime,"UTF-8")}"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("Authorization", "Bearer $jwt")
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Accept", ref.mime)
            when (connection.responseCode) {
                200 -> Unit
                401 -> throw QuestionMediaException(QuestionMediaFailure.SESSION_EXPIRED)
                403 -> throw QuestionMediaException(QuestionMediaFailure.NOT_AUTHORIZED)
                404 -> throw QuestionMediaException(QuestionMediaFailure.NOT_FOUND)
                else -> throw QuestionMediaException(QuestionMediaFailure.SERVICE_UNAVAILABLE)
            }
            if (connection.getHeaderField("X-Hyperion-Media-SHA256") != ref.sha256 ||
                connection.contentType?.substringBefore(';') != ref.mime) throw QuestionMediaException(QuestionMediaFailure.INTEGRITY_FAILED)
            val bytes = boundedBytes(connection)
            currentCoroutineContext().ensureActive()
            if (!verifies(bytes, ref)) throw QuestionMediaException(QuestionMediaFailure.INTEGRITY_FAILED)
            if (!stillAllowed()) throw QuestionMediaException(QuestionMediaFailure.ACCOUNT_CHANGED)
            runCatching {
                destination.parentFile?.mkdirs()
                val tmp = File(destination.parentFile, "${UUID.randomUUID()}.part")
                try {
                    FileOutputStream(tmp).use { out -> out.write(bytes); out.fd.sync() }
                    if (!tmp.renameTo(destination) && !destination.exists()) throw IOException("Image could not be cached")
                } finally { tmp.delete() }
            }
            currentCoroutineContext().ensureActive()
            if (!stillAllowed()) throw QuestionMediaException(QuestionMediaFailure.ACCOUNT_CHANGED)
            bytes
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: QuestionMediaException) {
            throw error
        } catch (_: Exception) {
            throw QuestionMediaException(QuestionMediaFailure.CONNECTION_FAILED)
        } finally { connection.disconnect() }
    }
}
