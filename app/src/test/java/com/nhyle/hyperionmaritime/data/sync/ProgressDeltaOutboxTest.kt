package com.nhyle.hyperionmaritime.data.sync

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ProgressDeltaOutboxTest {
    private val alice = "11111111-1111-4111-8111-111111111111"
    private val bob = "22222222-2222-4222-8222-222222222222"
    private val firstId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    private val secondId = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
    private val thirdId = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"

    private class MemoryStore : ProgressDeltaStore {
        val data = mutableMapOf<String, String>()
        var writeFails = false
        var deleteFails = false
        override fun read(key: String): String? = data[key]
        override fun write(key: String, value: String): Boolean {
            if (writeFails) return false
            data[key] = value
            return true
        }
        override fun delete(key: String): Boolean {
            if (deleteFails) return false
            data.remove(key)
            return true
        }
    }

    private fun updates(version: String = "a".repeat(32), value: String = """{"state":{"items":[{"id":"q1"}]},"version":0}""") =
        JSONArray().put(
            JSONObject().put("key", "hyperion-bookmarks").put("value", value)
                .put("expected_version", version)
        )

    private fun ack(id: String, value: String = "b".repeat(32)) =
        JSONObject().put("ok", true).put("operation_id", id)
            .put("versions", JSONObject().put("hyperion-bookmarks", value))

    @Test fun queuedOperationSurvivesRestartAndCannotBeReplacedByAnotherEdit() {
        val backing = MemoryStore()
        val outbox = ProgressDeltaOutbox(backing)
        val initial = outbox.stage(alice, firstId, updates())
        assertEquals(alice, initial.getString("owner"))
        assertEquals(1, initial.getJSONArray("updates").length())

        val anotherEdit = outbox.stage(alice, secondId, updates(value = "newer edit"))
        assertEquals(firstId, anotherEdit.getString("operation_id"))
        val reloaded = ProgressDeltaOutbox(backing)
        assertEquals(
            initial.toString(), reloaded.pending(alice)?.toString()
        )
        assertNull(reloaded.pending(bob))
        val bobOperation = reloaded.stage(bob, thirdId, updates())
        assertEquals(thirdId, bobOperation.getString("operation_id"))
        assertEquals(firstId, reloaded.pending(alice)?.getString("operation_id"))
    }

    @Test fun onlyExactServerAcknowledgementDeletesDurableWork() {
        val backing = MemoryStore()
        val outbox = ProgressDeltaOutbox(backing)
        outbox.stage(alice, firstId, updates())
        assertFalse(outbox.acknowledge(alice, firstId, ack(secondId)))
        assertFalse(outbox.acknowledge(alice, firstId, ack(firstId, "invalid")))
        assertFalse(outbox.acknowledge(bob, firstId, ack(firstId)))
        assertNotNull(outbox.pending(alice))

        backing.deleteFails = true
        assertThrows(IllegalStateException::class.java) {
            outbox.acknowledge(alice, firstId, ack(firstId))
        }
        assertNotNull(outbox.pending(alice))
        backing.deleteFails = false
        assertTrue(outbox.acknowledge(alice, firstId, ack(firstId)))
        assertNull(outbox.pending(alice))
    }

    @Test fun networkFailureCannotChangeRetryBytesButConfirmedConflictCan() {
        val backing = MemoryStore()
        val outbox = ProgressDeltaOutbox(backing)
        outbox.stage(alice, firstId, updates())
        assertThrows(IllegalArgumentException::class.java) {
            outbox.replaceConflicted(
                alice, firstId, secondId, updates("c".repeat(32)), false
            )
        }
        assertEquals(firstId, outbox.pending(alice)?.getString("operation_id"))

        backing.writeFails = true
        assertThrows(IllegalStateException::class.java) {
            outbox.replaceConflicted(
                alice, firstId, secondId, updates("c".repeat(32)), true
            )
        }
        assertEquals(firstId, outbox.pending(alice)?.getString("operation_id"))
        backing.writeFails = false

        val replacement = outbox.replaceConflicted(
            alice, firstId, secondId, updates("c".repeat(32)), true
        )
        assertEquals(secondId, replacement.getString("operation_id"))
        assertEquals("c".repeat(32), replacement.getJSONArray("updates")
            .getJSONObject(0).getString("expected_version"))
        assertFalse(outbox.acknowledge(alice, firstId, ack(firstId)))
        assertNotNull(outbox.pending(alice))
        assertTrue(outbox.acknowledge(alice, secondId, ack(secondId)))
    }

    @Test fun badStorageAndFailuresNeverSilentlyDiscardProgress() {
        val backing = MemoryStore()
        val outbox = ProgressDeltaOutbox(backing)
        backing.writeFails = true
        assertThrows(IllegalStateException::class.java) {
            outbox.stage(alice, firstId, updates())
        }
        assertNull(outbox.pending(alice))
        backing.writeFails = false
        assertThrows(IllegalArgumentException::class.java) {
            outbox.stage(alice, firstId, updates(value = "x".repeat(524289)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            outbox.stage(alice, firstId, updates(version = "invalid"))
        }

        backing.data["hyperion:delta-outbox:$alice"] = "{corrupted-json"
        assertThrows(IllegalStateException::class.java) { outbox.pending(alice) }
        assertEquals("{corrupted-json", backing.data["hyperion:delta-outbox:$alice"])
    }
}
