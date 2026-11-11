package com.nhyle.hyperionmaritime.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nhyle.hyperionmaritime.BuildConfig
import com.nhyle.hyperionmaritime.data.auth.SupabaseAccountManager
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.util.AssetCrypto
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Version-pinned, encrypted on-disk bank overrides. NOT enabled for automatic
 * production update checks until the Worker delivery endpoint and Android CI
 * have been verified. Existing bundled .enc files remain the fallback.
 *
 * Only licensed, signed-in owners can use remote overrides. The manifest and
 * source bytes are obtained from different authenticated endpoints, with each
 * ciphertext bound by SHA256([logical path, ciphertext SHA256, byte length]).
 * The active manifest is switched using an encrypted preferences commit only
 * AFTER every new file is completely downloaded and checked.
 */
internal object VerifiedRemoteBankStore {
    private const val WORKER = "https://hyperionmaritime.org/api/question-bank/encrypted-source"
    private const val MAX_SOURCE_BYTES = 1048576
    private const val MAX_MANIFEST_BYTES = 131072
    private val ownerPattern = Regex("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-8][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$")
    private val shaPattern = Regex("^[a-f0-9]{64}$")

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context.applicationContext,
        "hyperion_verified_remote_banks_v1",
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun storageKey(owner: String, track: Track): String {
        require(ownerPattern.matches(owner))
        return "bank-v1:${owner.lowercase()}:${track.name}"
    }

    private fun dataDir(context: Context, owner: String): File {
        require(ownerPattern.matches(owner))
        return File(context.filesDir, "verified-bank-updates-v1/${owner.lowercase()}")
    }

    private fun sourceFile(context: Context, owner: String, sha: String): File {
        require(shaPattern.matches(sha))
        return File(dataDir(context, owner), "$sha.enc")
    }

    private fun readBounded(stream: InputStream, maximum: Int): ByteArray {
        val buffer = ByteArray(8192)
        val output = java.io.ByteArrayOutputStream()
        stream.use {
            while (true) {
                val size = it.read(buffer)
                if (size < 0) break
                if (output.size() + size > maximum) throw IOException("Question-bank response exceeds safety limit")
                output.write(buffer, 0, size)
            }
        }
        return output.toByteArray()
    }

    private fun post(url: String, jwt: String, body: JSONObject, limit: Int): Pair<HttpURLConnection, ByteArray> {
        require(url.startsWith("https://"))
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Authorization", "Bearer $jwt")
        connection.setRequestProperty("Content-Type", "application/json")
        if (url.startsWith(BuildConfig.SUPABASE_URL.trimEnd('/'))) {
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        }
        connection.doOutput = true
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status != 200) throw IOException("Question-bank update service returned HTTP $status")
            return connection to readBounded(connection.inputStream, limit)
        } catch (error: Exception) {
            connection.disconnect()
            throw error
        }
    }

    private fun remoteManifest(track: Track, jwt: String, bundled: Set<String>): SmartBankRelease {
        val request = JSONObject().put("action", "content-versions").put("track", track.name)
        val (connection, payload) = post(
            BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/question-bank",
            jwt, request, MAX_MANIFEST_BYTES
        )
        try {
            return SmartBankManifest.parseRemote(
                JSONObject(String(payload, Charsets.UTF_8)), track, bundled
            )
        } finally { connection.disconnect() }
    }

    private fun verifiedLocalEntries(context: Context, owner: String, rawState: JSONObject): Map<String, Pair<String, Int>> {
        val result = mutableMapOf<String, Pair<String, Int>>()
        val keys = rawState.keys()
        while (keys.hasNext()) {
            val path = keys.next()
            if (SmartBankManifest.trackForPath(path) == null) continue
            val descriptor = rawState.optJSONObject(path) ?: continue
            val sha = descriptor.optString("sha256")
            val bytes = descriptor.optInt("bytes", -1)
            if (!shaPattern.matches(sha) || bytes !in 29..MAX_SOURCE_BYTES) continue
            val file = sourceFile(context,owner,sha)
            val content = runCatching { readBounded(file.inputStream(), MAX_SOURCE_BYTES) }.getOrNull()
            if (content != null && content.size == bytes && SmartBankManifest.digest(content) == sha &&
                runCatching { AssetCrypto.decrypt(content).isNotEmpty() }.getOrDefault(false)) {
                result[path] = sha to bytes
            }
        }
        return result
    }

    /** The question loader uses this only if the same owner still has a valid license. */
    fun openLicensedCsv(context: Context, assetPath: String): InputStream? {
        val source = SmartBankManifest.sourceForAssetPath(assetPath) ?: return null
        val track = SmartBankManifest.trackForPath(source) ?: return null
        if (!LicenseManager.isTrackProUnlocked(context, track)) return null
        val owner = SupabaseAccountManager.savedSignedInOwnerId(context) ?: return null
        return runCatching {
            val metadata = prefs(context).getString(storageKey(owner,track),null) ?: return null
            val sources = JSONObject(metadata).optJSONObject("sources") ?: return null
            val descriptor = sources.optJSONObject(source) ?: return null
            val sha = descriptor.optString("sha256")
            val bytes = descriptor.optInt("bytes", -1)
            if (!shaPattern.matches(sha) || bytes !in 29..MAX_SOURCE_BYTES) return null
            val encrypted = readBounded(sourceFile(context,owner,sha).inputStream(), MAX_SOURCE_BYTES)
            if (encrypted.size != bytes || SmartBankManifest.digest(encrypted) != sha) return null
            ByteArrayInputStream(AssetCrypto.decrypt(encrypted))
        }.getOrNull()
    }

    private fun stageCiphertext(context: Context, owner: String, sha: String, bytes: ByteArray) {
        require(SmartBankManifest.digest(bytes) == sha)
        // Verify the AES-GCM tag BEFORE making the ciphertext eligible to read.
        require(AssetCrypto.decrypt(bytes).isNotEmpty()) { "Invalid encrypted question source" }
        val target = sourceFile(context, owner, sha)
        if (target.isFile && runCatching {
                SmartBankManifest.digest(readBounded(target.inputStream(), MAX_SOURCE_BYTES)) == sha
            }.getOrDefault(false)) return
        check(target.parentFile?.mkdirs() == true || target.parentFile?.isDirectory == true)
        val temp = File.createTempFile("incoming-", ".tmp", target.parentFile)
        try {
            FileOutputStream(temp).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            if (!temp.renameTo(target)) throw IOException("Could not activate verified ciphertext file")
        } finally { temp.delete() }
    }

    private fun download(
        context: Context,
        owner: String,
        track: Track,
        release: SmartBankRelease,
        source: SmartBankSource,
        jwt: String
    ): Pair<String, Int> {
        val request = JSONObject().put("track",track.name)
            .put("path",source.path).put("bankRevision",release.revision)
        val (connection, bytes) = post(WORKER,jwt,request,MAX_SOURCE_BYTES)
        try {
            val sha = connection.getHeaderField("X-Hyperion-Source-SHA256") ?: ""
            val declaredSize = connection.getHeaderField("X-Hyperion-Source-Bytes")?.toIntOrNull() ?: -1
            if (connection.getHeaderField("X-Hyperion-Bank-Revision") != release.revision ||
                !shaPattern.matches(sha) || declaredSize != bytes.size ||
                declaredSize !in 29..MAX_SOURCE_BYTES ||
                SmartBankManifest.version(source.path,sha,declaredSize) != source.version ||
                SmartBankManifest.digest(bytes) != sha) {
                throw IOException("Encrypted question-bank hash/version verification failed")
            }
            stageCiphertext(context,owner,sha,bytes)
            return sha to declaredSize
        } finally { connection.disconnect() }
    }

    /** Explicit licensed refresh API; caller must supply a fresh matching user JWT. */
    suspend fun refreshLicensedTrack(
        context: Context,
        track: Track,
        owner: String,
        jwt: String,
        stillAuthorized: () -> Boolean
    ): Int = withContext(Dispatchers.IO) {
        require(ownerPattern.matches(owner) && jwt.isNotBlank())
        if (!LicenseManager.isTrackProUnlocked(context, track) || !stillAuthorized()) {
            throw IOException("The requested track is not currently licensed")
        }
        val bundled = context.assets.open("questions/hyperion-manifest.json").use {
            SmartBankManifest.bundledFiles(
                JSONObject(String(readBounded(it,MAX_MANIFEST_BYTES),Charsets.UTF_8)), track
            )
        }
        val expected = remoteManifest(track,jwt,bundled.keys)
        val current = prefs(context).getString(storageKey(owner,track),null)
            ?.let { JSONObject(it).optJSONObject("sources") } ?: JSONObject()
        val existing = verifiedLocalEntries(context,owner,current)
        val needed = SmartBankManifest.needsDownload(expected,bundled,existing)
        val additions = mutableMapOf<String, Pair<String,Int>>()
        for (source in needed) {
            if (!stillAuthorized() || !LicenseManager.isTrackProUnlocked(context,track)) {
                throw IOException("Account or track entitlement changed during download")
            }
            additions[source.path] = download(context,owner,track,expected,source,jwt)
        }
        // A published revision can advance while several parts are fetched.
        // Never activate a mixed release, including on an interrupted download.
        val verifiedAgain = remoteManifest(track,jwt,bundled.keys)
        if (verifiedAgain != expected) {
            throw IOException("Question-bank release changed during update; previous bank retained")
        }
        if (!stillAuthorized() || !LicenseManager.isTrackProUnlocked(context,track)) {
            throw IOException("Account/entitlement changed before activation")
        }
        val activated = JSONObject()
        for (source in expected.sources) {
            val matchingBundled = bundled[source.path]?.let {
                SmartBankManifest.version(source.path,it.first,it.second) == source.version
            } == true
            if (matchingBundled) continue // Fall through to unchanged APK asset.
            val metadata = additions[source.path] ?: existing[source.path]
                ?: throw IOException("Missing verified source for activation")
            if (SmartBankManifest.version(source.path,metadata.first,metadata.second) != source.version) {
                throw IOException("An unchanged bank source failed verification")
            }
            activated.put(source.path, JSONObject()
                .put("sha256",metadata.first).put("bytes",metadata.second))
        }
        val state = JSONObject().put("revision",expected.revision)
            .put("versionFormat","github-content-sha256-v1")
            .put("sources",activated)
        check(prefs(context).edit().putString(storageKey(owner,track),state.toString()).commit()) {
            "Could not atomically activate the new verified bank manifest"
        }
        needed.size
    }
}
