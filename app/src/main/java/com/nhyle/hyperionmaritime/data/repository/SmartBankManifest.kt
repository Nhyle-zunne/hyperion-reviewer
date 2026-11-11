package com.nhyle.hyperionmaritime.data.repository

import com.nhyle.hyperionmaritime.data.model.Track
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Immutable manifest entry for a single *encrypted* source file. */
internal data class SmartBankSource(val path: String, val version: String)

internal data class SmartBankRelease(
    val revision: String,
    val sources: List<SmartBankSource>
)

/**
 * Pure, fail-closed V1 manifest protocol shared with the PWA's
 * github-content-sha256-v1 source versions.
 *
 * The signed-in Supabase Edge route supplies the expected opaque versions.
 * The licensed Worker delivery supplies encrypted bytes and the published
 * file's SHA-256 and size. We compute SHA256(JSON.stringify([path,sha,size]))
 * to bind the downloaded file to the separate manifest verification.
 */
internal object SmartBankManifest {
    private val sha256 = Regex("^[a-f0-9]{64}$")
    private val revision = Regex("^[a-f0-9]{40}$")
    private val path = Regex("^(deck|gmdss|engine_ew|deck_mgt|engine_mgt)/[a-z0-9_]+\\.csv\\.enc$")

    fun trackForPath(source: String): Track? = when {
        source.startsWith("deck/") -> Track.DECK_OIC_NW
        source.startsWith("gmdss/") -> Track.GMDSS_GOC
        source.startsWith("engine_ew/") -> Track.ENGINE_OIC_EW
        source.startsWith("deck_mgt/") -> Track.DECK_MGT
        source.startsWith("engine_mgt/") -> Track.ENGINE_MGT
        else -> null
    }

    fun sourceForAssetPath(assetPath: String): String? {
        val normalized = assetPath.removePrefix("questions/").removeSuffix(".enc")
        if (!normalized.endsWith(".csv")) return null
        val canonical = if (normalized.startsWith("pro/")) {
            "gmdss/" + normalized.removePrefix("pro/")
        } else normalized
        val source = "$canonical.enc"
        return if (path.matches(source)) source else null
    }

    fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun version(path: String, ciphertextSha: String, bytes: Int): String {
        require(this.path.matches(path) && sha256.matches(ciphertextSha) && bytes in 29..1048576) {
            "Invalid published ciphertext fingerprint"
        }
        val encoded = JSONArray().put(path).put(ciphertextSha).put(bytes).toString()
        return digest(encoded.toByteArray(Charsets.UTF_8))
    }

    fun bundledFiles(manifest: JSONObject, track: Track): Map<String, Pair<String, Int>> {
        require(manifest.optInt("version") == 1 && manifest.optBoolean("encrypted"))
        val array = manifest.getJSONArray("files")
        val files = linkedMapOf<String, Pair<String, Int>>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val source = item.getString("path")
            if (!path.matches(source)) throw IllegalArgumentException("Invalid asset manifest path")
            if (trackForPath(source) != track) continue
            require(source !in files)
            val hash = item.getString("sha256")
            val size = item.getInt("bytes")
            require(sha256.matches(hash) && size in 29..1048576)
            files[source] = hash to size
        }
        require(files.isNotEmpty()) { "No bundled source manifest for this track" }
        return files
    }

    fun parseRemote(response: JSONObject, track: Track, expectedPaths: Set<String>): SmartBankRelease {
        require(response.optString("track") == track.name)
        require(response.optString("versionFormat") == "github-content-sha256-v1")
        val bankRevision = response.optString("bankRevision")
        require(revision.matches(bankRevision))
        val versions = response.getJSONArray("versions")
        val files = linkedMapOf<String, SmartBankSource>()
        for (i in 0 until versions.length()) {
            val item = versions.getJSONObject(i)
            val source = item.getString("path")
            val hash = item.optString("fullHash")
            require(path.matches(source) && trackForPath(source) == track &&
                sha256.matches(hash) && source in expectedPaths && source !in files) {
                "Invalid or unlicensed remote question-bank source"
            }
            files[source] = SmartBankSource(source, hash)
        }
        require(files.keys == expectedPaths) {
            "The APK parser and remote source catalog differ; keep the bundled bank"
        }
        return SmartBankRelease(bankRevision, files.values.toList())
    }

    /**
     * A verified stored override, or the packaged asset when its hash matches,
     * costs zero network bytes. Missing/corrupt overrides must be downloaded.
     */
    fun needsDownload(
        release: SmartBankRelease,
        bundled: Map<String, Pair<String, Int>>,
        verifiedStored: Map<String, Pair<String, Int>>
    ): List<SmartBankSource> {
        return release.sources.filter { source ->
            val bundledFingerprint = bundled[source.path]?.let { (sha, bytes) ->
                version(source.path, sha, bytes)
            }
            val storedFingerprint = verifiedStored[source.path]?.let { (sha, bytes) ->
                version(source.path, sha, bytes)
            }
            source.version != bundledFingerprint && source.version != storedFingerprint
        }
    }
}
