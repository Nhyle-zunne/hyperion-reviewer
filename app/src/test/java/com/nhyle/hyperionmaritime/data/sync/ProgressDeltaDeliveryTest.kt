package com.nhyle.hyperionmaritime.data.sync

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ProgressDeltaDeliveryTest {
    private val owner = "11111111-1111-4111-8111-111111111111"
    private val other = "22222222-2222-4222-8222-222222222222"
    private val opId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    private val nextId = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"

    private class InMemoryStore : ProgressDeltaStore {
        private val content = mutableMapOf<String,String>()
        override fun read(key: String): String? = content[key]
        override fun write(key: String,value: String): Boolean {
            content[key]=value; return true
        }
        override fun delete(key: String): Boolean {
            content.remove(key); return true
        }
    }
    private class FakeTransport(var identity: String?) : ProgressDeltaTransport {
        val requests = mutableListOf<String>()
        var fail = false
        var reply = ProgressDeltaTransport.Reply(503,null)
        override fun currentSignedInUserId() = identity
        override fun send(request: JSONObject): ProgressDeltaTransport.Reply {
            requests.add(request.toString())
            if(fail)throw IllegalStateException("Synthetic network lost reply")
            return reply
        }
    }
    private fun changes()=JSONArray().put(JSONObject()
        .put("key","hyperion-history")
        .put("expected_version","a".repeat(32))
        .put("value","""{"version":0,"state":{"entries":[{"id":"h1"}]}}"""))
    private fun ack(operationId: String) = JSONObject()
        .put("ok",true)
        .put("operation_id",operationId)
        .put("versions",JSONObject().put("hyperion-history","b".repeat(32)))

    @Test fun deliveryIsGatedAndCloudOutboxIsDurableAcrossNetworkFailuresAndLogout() {
        assertFalse(ProgressDeltaFeature.ENABLED)
        val outbox=ProgressDeltaOutbox(InMemoryStore())
        val network=FakeTransport(owner)
        val worker=ProgressDeltaDelivery(outbox,network)
        network.fail=true
        assertEquals(ProgressDeltaDeliveryResult.Status.RETRY,
            worker.deliver(owner,opId,changes()).status)
        assertEquals(1,network.requests.size)
        assertEquals(opId,outbox.pending(owner)?.getString("operation_id"))

        network.identity=other
        assertEquals(ProgressDeltaDeliveryResult.Status.PAUSED,
            worker.deliver(owner,nextId,changes()).status)
        assertEquals(1,network.requests.size)
        assertEquals(opId,outbox.pending(owner)?.getString("operation_id"))

        network.identity=owner
        network.fail=false
        network.reply=ProgressDeltaTransport.Reply(200,ack(opId))
        assertEquals(ProgressDeltaDeliveryResult.Status.ACKNOWLEDGED,
            worker.deliver(owner,nextId,changes()).status)
        assertNull(outbox.pending(owner))
        assertEquals("An uncertain network response replays exact immutable operation bytes",
            network.requests[0], network.requests[1])
    }

    @Test fun falseAcknowledgementAndConflictNeverDiscardStoredOperations() {
        val outbox=ProgressDeltaOutbox(InMemoryStore())
        val network=FakeTransport(owner)
        val worker=ProgressDeltaDelivery(outbox,network)
        network.reply=ProgressDeltaTransport.Reply(200,ack(nextId))
        assertEquals(ProgressDeltaDeliveryResult.Status.RETRY,
            worker.deliver(owner,opId,changes()).status)
        assertEquals(opId,outbox.pending(owner)?.getString("operation_id"))

        network.reply=ProgressDeltaTransport.Reply(409,
            JSONObject().put("conflicts",JSONArray().put("hyperion-history")))
        val result=worker.deliver(owner)
        assertEquals(ProgressDeltaDeliveryResult.Status.CONFLICT,result.status)
        assertEquals(listOf("hyperion-history"),result.conflicts)
        assertEquals(opId,outbox.pending(owner)?.getString("operation_id"))

        network.reply=ProgressDeltaTransport.Reply(409,
            JSONObject().put("conflicts",JSONArray().put("other-account-data")))
        assertEquals(ProgressDeltaDeliveryResult.Status.RETRY,
            worker.deliver(owner).status)
        assertNotNull(outbox.pending(owner))
    }

    @Test fun switchingAccountsDuringRequestCannotAcknowledgeOldAccountData() {
        val outbox=ProgressDeltaOutbox(InMemoryStore())
        val network=object: ProgressDeltaTransport {
            var identity=owner
            override fun currentSignedInUserId(): String? = identity
            override fun send(request: JSONObject): ProgressDeltaTransport.Reply {
                identity=other
                return ProgressDeltaTransport.Reply(200,ack(opId))
            }
        }
        val worker=ProgressDeltaDelivery(outbox,network)
        assertEquals(ProgressDeltaDeliveryResult.Status.PAUSED,
            worker.deliver(owner,opId,changes()).status)
        assertNotNull(outbox.pending(owner))
        assertNull(outbox.pending(other))
    }
}
