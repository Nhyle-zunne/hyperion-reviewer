package com.nhyle.hyperionmaritime.data.sync

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable V2 progress retry journal. This is deliberately NOT wired to the
 * production V1 sync worker until the server/client compatibility gates pass.
 *
 * A caller must verify the currently authenticated account before every send,
 * hold the existing per-account sync mutex, and retry the exact saved payload
 * after a timeout. Replacing an operation is allowed ONLY after a definite
 * HTTP 409 (the server guarantees that batch was not committed).
 */
internal interface ProgressDeltaStore {
    fun read(key: String): String?
    fun write(key: String, value: String): Boolean
    fun delete(key: String): Boolean
}

internal class ProgressDeltaOutbox(private val store: ProgressDeltaStore) {
    companion object {
        private const val PREFIX = "hyperion:delta-outbox:"
        private const val MAX_SECTION_BYTES = 524288
        private val mutex = Any()
        private val uuid = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
        )
        private val version = Regex("^[a-fA-F0-9]{32}$")
        private val sections = setOf(
            "hyperion-history", "hyperion-study-time", "hyperion-vault",
            "hyperion-bookmarks", "hyperion-saved-practice",
            "hyperion-mock-exam-plan-v1", "hyperion-question-attempt-history",
            "hyperion-progress-deletions"
        )

        /**
         * Never fall back to unencrypted preferences if the keystore is
         * unavailable: an unsent delta can contain question text and answers.
         */
        fun encrypted(context: Context): ProgressDeltaOutbox {
            val app = context.applicationContext
            val masterKey = MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val prefs = EncryptedSharedPreferences.create(
                app, "hyperion_progress_delta_v2_encrypted", masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            return ProgressDeltaOutbox(object : ProgressDeltaStore {
                override fun read(key: String): String? = prefs.getString(key, null)
                override fun write(key: String, value: String): Boolean =
                    prefs.edit().putString(key, value).commit()
                override fun delete(key: String): Boolean =
                    prefs.edit().remove(key).commit()
            })
        }
    }

    private fun key(owner: String): String {
        require(uuid.matches(owner)) { "Invalid progress account owner" }
        return PREFIX + owner.lowercase()
    }

    private fun validateUpdates(updates: JSONArray) {
        require(updates.length() in 1..8) { "Invalid progress delta size" }
        val unique = HashSet<String>()
        for (index in 0 until updates.length()) {
            val item = updates.optJSONObject(index)
                ?: throw IllegalArgumentException("Invalid progress delta item")
            val section = item.optString("key", "")
            require(section in sections && unique.add(section)) { "Duplicate or unknown progress section" }
            require(item.has("value")) { "Missing progress delta value" }
            val value = item.opt("value")
            require(value == JSONObject.NULL || value is String) { "Invalid progress delta data" }
            if (value is String) {
                require(value.toByteArray(Charsets.UTF_8).size <= MAX_SECTION_BYTES) {
                    "Progress delta section exceeds transport limit"
                }
            }
            require(version.matches(item.optString("expected_version", ""))) {
                "Missing or invalid progress section version"
            }
        }
    }

    private fun validate(owner: String, operation: JSONObject): JSONObject {
        require(operation.optInt("version", -1) == 1 &&
            operation.optString("owner", "") == owner &&
            uuid.matches(operation.optString("operation_id", ""))) {
            "Progress operation belongs to another account or is invalid"
        }
        validateUpdates(operation.optJSONArray("updates")
            ?: throw IllegalArgumentException("Invalid progress operation updates"))
        return operation
    }

    /** A corrupt outbox is never discarded silently; local V1 recovery remains available. */
    fun pending(owner: String): JSONObject? = synchronized(mutex) {
        val raw = store.read(key(owner)) ?: return@synchronized null
        try {
            validate(owner, JSONObject(raw))
        } catch (error: Exception) {
            throw IllegalStateException(
                "Unverified saved progress operation retained for recovery", error
            )
        }
    }

    /** Durable write occurs before a single network request can be attempted. */
    fun stage(owner: String, operationId: String, updates: JSONArray): JSONObject =
        synchronized(mutex) {
            pending(owner)?.let { return@synchronized it }
            require(uuid.matches(operationId)) { "Invalid progress operation id" }
            validateUpdates(updates)
            val operation = JSONObject()
                .put("version", 1)
                .put("owner", owner)
                .put("operation_id", operationId)
                .put("updates", JSONArray(updates.toString()))
                .put("created_at", System.currentTimeMillis())
            validate(owner, operation)
            check(store.write(key(owner), operation.toString())) {
                "Could not durably save progress operation"
            }
            val saved = pending(owner)
            check(saved?.optString("operation_id") == operationId) {
                "Progress operation could not be verified after saving"
            }
            saved!!
        }

    /** Never acknowledge on HTTP failure, missing section version or another account's reply. */
    fun acknowledge(owner: String, operationId: String, response: JSONObject): Boolean =
        synchronized(mutex) {
            val saved = pending(owner) ?: return@synchronized false
            if (saved.optString("operation_id") != operationId ||
                response.optBoolean("ok", false) != true ||
                response.optString("operation_id") != operationId
            ) return@synchronized false

            val versions = response.optJSONObject("versions") ?: return@synchronized false
            val updates = saved.getJSONArray("updates")
            for (index in 0 until updates.length()) {
                if (!version.matches(versions.optString(
                        updates.getJSONObject(index).getString("key"), ""
                    ))
                ) return@synchronized false
            }
            check(store.delete(key(owner))) { "Could not confirm progress ACK on disk" }
            true
        }

    /**
     * Caller MUST pass a server-confirmed 409. A timeout is ambiguous, and
     * its operation must be replayed unchanged to obtain the idempotent ACK.
     */
    fun replaceConflicted(
        owner: String,
        oldOperationId: String,
        newOperationId: String,
        updates: JSONArray,
        conflictConfirmed: Boolean
    ): JSONObject = synchronized(mutex) {
        require(conflictConfirmed) { "Unconfirmed network failure cannot replace a delta" }
        val saved = pending(owner)
        require(saved?.optString("operation_id") == oldOperationId) {
            "Progress conflict rebase requires the pending original"
        }
        require(uuid.matches(newOperationId) && newOperationId != oldOperationId) {
            "Conflict rebase requires a fresh operation id"
        }
        validateUpdates(updates)
        val replacement = JSONObject()
            .put("version", 1)
            .put("owner", owner)
            .put("operation_id", newOperationId)
            .put("updates", JSONArray(updates.toString()))
            .put("created_at", System.currentTimeMillis())
        validate(owner, replacement)
        check(store.write(key(owner), replacement.toString())) {
            "Could not durably persist rebased progress operation"
        }
        val verified = pending(owner)
        check(verified?.optString("operation_id") == newOperationId) {
            "Rebased progress operation could not be verified"
        }
        verified!!
    }
}
