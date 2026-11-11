package com.nhyle.hyperionmaritime.data.sync

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Compare the *study data* in version-1 progress snapshots, not their transport
 * timestamps. Zustand stores JSON documents as strings under the storage map:
 * ordinary string equality would treat different object key order as a new edit.
 *
 * Arrays remain ordered, and malformed or ordinary strings compare exactly.
 * No question text, saved answers, or per-account records are omitted.
 */
internal object LearningProgressStorageComparator {
    fun sameStorage(left: JSONObject?, right: JSONObject?): Boolean {
        if (left == null || right == null) return left == null && right == null
        return canonicalStorage(left) == canonicalStorage(right)
    }

    fun storageFingerprint(storage: JSONObject): String {
        val bytes = canonicalStorage(storage).toByteArray(StandardCharsets.UTF_8)
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun canonicalStorage(storage: JSONObject): String {
        return storage.keys().asSequence().toList().sorted().joinToString(",", "{", "}") { key ->
            JSONObject.quote(key) + ":" + canonicalStorageField(storage.opt(key))
        }
    }

    private fun canonicalStorageField(value: Any?): String {
        if (value is String) {
            val trimmed = value.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                runCatching { JSONObject(trimmed) }.getOrNull()?.let { return canonicalJson(it) }
            }
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                runCatching { JSONArray(trimmed) }.getOrNull()?.let { return canonicalJson(it) }
            }
        }
        return canonicalJson(value)
    }

    private fun canonicalJson(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(",", "{", "}") { key ->
            JSONObject.quote(key) + ":" + canonicalJson(value.opt(key))
        }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { index ->
            canonicalJson(value.opt(index))
        }
        is String -> JSONObject.quote(value)
        is Boolean, is Number -> value.toString()
        else -> JSONObject.quote(value.toString())
    }
}
