package com.nhyle.hyperionmaritime.data.sync

import org.json.JSONObject
import java.io.IOException

/** One immutable, account-scoped cloud response; never persisted to device storage. */
internal class LearningProgressReadCache {
    private var owner: String? = null
    private var cachedResponse: String? = null

    @Synchronized
    fun clear() {
        owner = null
        cachedResponse = null
    }

    @Synchronized
    fun request(userId: String): JSONObject {
        if (owner != userId) clear()
        val request = JSONObject().put("action", "read")
        cachedResponse?.let { request.put("known_snapshot_hash", JSONObject(it).getString("snapshot_hash")) }
        return request
    }

    @Synchronized
    fun resolve(userId: String, response: JSONObject): JSONObject {
        if (response.optBoolean("not_modified")) {
            val cached = cachedResponse?.takeIf { owner == userId }?.let(::JSONObject)
            if (cached == null || response.optString("snapshot_hash") != cached.optString("snapshot_hash") ||
                response.optString("updated_at") != cached.optString("updated_at")) {
                clear()
                throw IOException("Saved progress confirmation did not match the local cloud copy.")
            }
            // Return a newly parsed copy: the merge adapter mutates nested JSON objects.
            return cached
        }
        clear()
        val data = response.optJSONObject("data")
        if (data != null && data.optInt("version") == 1 && data.optJSONObject("storage") != null &&
            response.optString("snapshot_hash").matches(Regex("[0-9a-f]{64}")) &&
            response.optString("updated_at").isNotBlank()) {
            owner = userId
            cachedResponse = response.toString()
        }
        return response
    }
}
