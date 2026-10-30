package com.nhyle.hyperionmaritime.data.sync

import android.content.Context
import org.json.JSONObject

/**
 * Persists per-item delete/restore markers outside Room so study synchronization
 * does not require a destructive database migration.
 */
object LearningProgressDeletionLedger {
    private const val PREFS_NAME = "hyperion_learning_progress_deletions"
    private const val PREFS_KEY = "ledger_json"

    fun record(context: Context, collection: String, id: String, deleted: Boolean, at: Long = System.currentTimeMillis()) {
        if (collection != "bookmarks" && collection != "vault") return
        if (id.isBlank()) return
        val ledger = read(context)
        val items = ledger.optJSONObject(collection) ?: JSONObject().also { ledger.put(collection, it) }
        val previous = items.optJSONObject(id)
        if (!deleted && previous?.optBoolean("deleted", false) != true) return
        val mutationAt = maxOf(at, (previous?.optLong("at", 0L) ?: 0L) + 1L)
        items.put(id, JSONObject().put("at", mutationAt).put("deleted", deleted))
        save(context, ledger)
    }

    fun merge(context: Context, remote: JSONObject?): JSONObject {
        val local = read(context)
        val merged = JSONObject().put("version", 1)
        for (collection in listOf("bookmarks", "vault")) {
            val localItems = local.optJSONObject(collection) ?: JSONObject()
            val remoteItems = remote?.optJSONObject(collection) ?: JSONObject()
            val combined = JSONObject()
            val ids = LinkedHashSet<String>()
            localItems.keys().forEach { ids.add(it) }
            remoteItems.keys().forEach { ids.add(it) }
            for (id in ids) {
                val localMutation = localItems.optJSONObject(id)
                val remoteMutation = remoteItems.optJSONObject(id)
                val chosen = when {
                    localMutation == null -> remoteMutation
                    remoteMutation == null -> localMutation
                    localMutation.optLong("at", 0L) > remoteMutation.optLong("at", 0L) -> localMutation
                    remoteMutation.optLong("at", 0L) > localMutation.optLong("at", 0L) -> remoteMutation
                    else -> JSONObject()
                        .put("at", localMutation.optLong("at", 0L))
                        .put("deleted", localMutation.optBoolean("deleted") || remoteMutation.optBoolean("deleted"))
                }
                if (chosen != null) combined.put(id, JSONObject(chosen.toString()))
            }
            merged.put(collection, combined)
        }
        save(context, merged)
        return merged
    }

    fun isDeleted(ledger: JSONObject, collection: String, id: String, itemTimestamp: Long): Boolean {
        if (collection != "bookmarks" && collection != "vault") return false
        val mutation = ledger.optJSONObject(collection)?.optJSONObject(id) ?: return false
        return mutation.optBoolean("deleted", false) && mutation.optLong("at", 0L) >= itemTimestamp
    }

    fun isDeleted(context: Context, collection: String, id: String, itemTimestamp: Long): Boolean =
        isDeleted(read(context), collection, id, itemTimestamp)

    fun read(context: Context): JSONObject {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(PREFS_KEY, null)
        val parsed = runCatching { raw?.let { JSONObject(it) } }.getOrNull()
        return JSONObject()
            .put("version", 1)
            .put("bookmarks", JSONObject(parsed?.optJSONObject("bookmarks")?.toString() ?: "{}"))
            .put("vault", JSONObject(parsed?.optJSONObject("vault")?.toString() ?: "{}"))
    }

    private fun save(context: Context, ledger: JSONObject) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_KEY, ledger.toString())
            .apply()
    }
}
