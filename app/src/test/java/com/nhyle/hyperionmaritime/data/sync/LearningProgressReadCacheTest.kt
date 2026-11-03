package com.nhyle.hyperionmaritime.data.sync

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LearningProgressReadCacheTest {
    private val hash = "a".repeat(64)
    private fun full(withHash: Boolean = true) = JSONObject().apply {
        put("data", JSONObject("""{"version":1,"storage":{"bookmarks":"saved","mistakes":"kept"},"activeSession":{"sessionId":"s1"}}"""))
        put("updated_at", "2026-11-03T01:00:00Z")
        if (withHash) put("snapshot_hash", hash)
    }
    private fun unchanged() = JSONObject().put("not_modified", true)
        .put("snapshot_hash", hash).put("updated_at", "2026-11-03T01:00:00Z")

    @Test fun unchangedReadRestoresAnImmutableCopyForMergingLocalEdits() {
        val cache = LearningProgressReadCache()
        assertFalse(cache.request("alice").has("known_snapshot_hash"))
        val initial = cache.resolve("alice", full())
        initial.getJSONObject("data").getJSONObject("storage").put("bookmarks", "local edit")
        assertEquals(hash, cache.request("alice").getString("known_snapshot_hash"))
        val restored = cache.resolve("alice", unchanged())
        assertEquals("saved", restored.getJSONObject("data").getJSONObject("storage").getString("bookmarks"))
        assertEquals("kept", restored.getJSONObject("data").getJSONObject("storage").getString("mistakes"))
        assertEquals("s1", restored.getJSONObject("data").getJSONObject("activeSession").getString("sessionId"))
        assertFalse(restored.optBoolean("not_modified"))
    }

    @Test fun changedRemoteSnapshotReplacesCachedCopyAndRevision() {
        val cache = LearningProgressReadCache()
        cache.resolve("alice", full())
        val updated = full().put("snapshot_hash", "b".repeat(64)).put("updated_at", "2026-11-03T01:01:00Z")
        updated.getJSONObject("data").getJSONObject("storage").put("bookmarks", "other device")
        cache.resolve("alice", updated)
        val reply = unchanged().put("snapshot_hash", "b".repeat(64)).put("updated_at", "2026-11-03T01:01:00Z")
        assertEquals("other device", cache.resolve("alice", reply).getJSONObject("data").getJSONObject("storage").getString("bookmarks"))
    }

    @Test fun accountSwitchAndExplicitClearRequireFullReads() {
        val cache = LearningProgressReadCache()
        cache.resolve("alice", full())
        assertFalse(cache.request("bob").has("known_snapshot_hash"))
        assertThrows(IOException::class.java) { cache.resolve("bob", unchanged()) }
        cache.resolve("alice", full())
        cache.clear()
        assertFalse(cache.request("alice").has("known_snapshot_hash"))
        assertThrows(IOException::class.java) { cache.resolve("alice", unchanged()) }
    }

    @Test fun mismatchedConfirmationNeverReusesStaleData() {
        for (reply in listOf(unchanged().put("snapshot_hash", "b".repeat(64)), unchanged().put("updated_at", "other"))) {
            val cache = LearningProgressReadCache()
            cache.resolve("alice", full())
            assertThrows(IOException::class.java) { cache.resolve("alice", reply) }
            assertFalse(cache.request("alice").has("known_snapshot_hash"))
        }
    }

    @Test fun legacyAndMissingRecordResponsesStayCompatibleWithoutCaching() {
        val cache = LearningProgressReadCache()
        assertEquals("saved", cache.resolve("alice", full(false)).getJSONObject("data").getJSONObject("storage").getString("bookmarks"))
        assertFalse(cache.request("alice").has("known_snapshot_hash"))
        cache.resolve("alice", full())
        val missing = cache.resolve("alice", JSONObject().put("data", JSONObject.NULL).put("updated_at", JSONObject.NULL))
        assertTrue(missing.isNull("data"))
        assertFalse(cache.request("alice").has("known_snapshot_hash"))
    }
}
