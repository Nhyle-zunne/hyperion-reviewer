package com.nhyle.hyperionmaritime.data.repository

import android.content.Context
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
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.IOException
import com.nhyle.hyperionmaritime.data.util.AssetCrypto
import java.security.MessageDigest

internal enum class QuestionMediaFailure(val description: String) {
    LICENSE_REQUIRED("Diagram unavailable. An active license for this track is required."),
    NOT_BUNDLED("Diagram unavailable. Update the app to install this diagram for offline study."),
    INTEGRITY_FAILED("Diagram unavailable. The image failed verification. Retry or update the app."),
    BUNDLE_DAMAGED("Diagram unavailable. This app’s bundled image is damaged. Install the latest app update."),
    ACCOUNT_CHANGED("Diagram unavailable. The active account or track access changed. Reopen the question."),
}

internal class QuestionMediaException(val reason: QuestionMediaFailure) : IOException(reason.description)

/** Encrypted APK diagrams first; owner-scoped legacy cache for newer refs. No image network requests.
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
        // Preserve previously verified images from older app versions locally.
        // New diagrams arrive in app updates, never through an image service.
        currentCoroutineContext().ensureActive()
        if (!stillAllowed()) throw QuestionMediaException(QuestionMediaFailure.ACCOUNT_CHANGED)
        val validOwner = owner?.takeIf { uuid.matches(it) }
            ?: throw QuestionMediaException(QuestionMediaFailure.NOT_BUNDLED)
        val destination = fileFor(app, validOwner, track, ref)
        if (destination.isFile) {
            val local = runCatching {
                if (destination.length() !in 16L..MAX_BYTES.toLong()) null else destination.readBytes()
            }.getOrNull()
            val verified = local != null && verifies(local, ref)
            currentCoroutineContext().ensureActive()
            if (!stillAllowed()) throw QuestionMediaException(QuestionMediaFailure.ACCOUNT_CHANGED)
            if (verified) return@withContext local!!
            destination.delete()
        }
        throw QuestionMediaException(QuestionMediaFailure.NOT_BUNDLED)
    }
}
