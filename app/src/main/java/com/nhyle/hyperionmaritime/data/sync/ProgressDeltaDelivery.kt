package com.nhyle.hyperionmaritime.data.sync

import org.json.JSONArray
import org.json.JSONObject

/**
 * Offline-first V2 delivery boundary. NOT ENABLED by the production sync worker.
 * The current V1 snapshot path remains authoritative until native, PWA,
 * conflict-resolution and Play release gates pass.
 *
 * Calls are synchronous and must run off the UI thread under the existing
 * per-account progress sync lock. Identity MUST be checked against the current
 * signed Google/Supabase account, never local cached license alone.
 */
internal object ProgressDeltaFeature {
    const val ENABLED = false
}
internal interface ProgressDeltaTransport {
    fun currentSignedInUserId(): String?
    @Throws(Exception::class)
    fun send(request: JSONObject): Reply
    data class Reply(val statusCode: Int, val body: JSONObject?)
}

internal data class ProgressDeltaDeliveryResult(
    val status: Status,
    val conflicts: List<String> = emptyList()
) {
    enum class Status { NOTHING_TO_SEND, PAUSED, RETRY, CONFLICT, ACKNOWLEDGED }
}

internal class ProgressDeltaDelivery(
    private val outbox: ProgressDeltaOutbox,
    private val transport: ProgressDeltaTransport
) {
    /**
     * A persisted operation ALWAYS wins over new updates until acknowledged.
     * HTTP 409 is reported but never causes automatic deletion or rebase.
     * The caller must merge remote and local records before replaceConflicted.
     */
    fun deliver(owner: String, newId: String? = null, updates: JSONArray? = null): ProgressDeltaDeliveryResult {
        val prior = outbox.pending(owner)
        val operation = when {
            prior != null -> prior
            updates == null || updates.length() == 0 ->
                return ProgressDeltaDeliveryResult(ProgressDeltaDeliveryResult.Status.NOTHING_TO_SEND)
            newId == null -> throw IllegalArgumentException("New V2 operation requires UUID")
            else -> outbox.stage(owner,newId,updates)
        }
        if (transport.currentSignedInUserId() != owner)
            return ProgressDeltaDeliveryResult(ProgressDeltaDeliveryResult.Status.PAUSED)

        val request = JSONObject()
            .put("action","write-sections")
            .put("operation_id",operation.getString("operation_id"))
            .put("updates",JSONArray(operation.getJSONArray("updates").toString()))
        val response = try { transport.send(request) }
            catch (_: Exception) {
                return ProgressDeltaDeliveryResult(ProgressDeltaDeliveryResult.Status.RETRY)
            }
        if (transport.currentSignedInUserId() != owner)
            return ProgressDeltaDeliveryResult(ProgressDeltaDeliveryResult.Status.PAUSED)

        if (response.statusCode == 409) {
            val conflicts = response.body?.optJSONArray("conflicts")
                ?: return ProgressDeltaDeliveryResult(ProgressDeltaDeliveryResult.Status.RETRY)
            val ownKeys = HashSet<String>()
            for(i in 0 until operation.getJSONArray("updates").length())
                ownKeys.add(operation.getJSONArray("updates").getJSONObject(i).getString("key"))
            val keys = ArrayList<String>()
            for(i in 0 until conflicts.length()) {
                val key = conflicts.optString(i)
                if(key.isEmpty() || key !in ownKeys)
                    return ProgressDeltaDeliveryResult(ProgressDeltaDeliveryResult.Status.RETRY)
                keys.add(key)
            }
            if(keys.isEmpty())return ProgressDeltaDeliveryResult(ProgressDeltaDeliveryResult.Status.RETRY)
            return ProgressDeltaDeliveryResult(ProgressDeltaDeliveryResult.Status.CONFLICT,keys)
        }
        if(response.statusCode != 200 || response.body == null)
            return ProgressDeltaDeliveryResult(ProgressDeltaDeliveryResult.Status.RETRY)
        if(!outbox.acknowledge(owner,operation.getString("operation_id"),response.body))
            return ProgressDeltaDeliveryResult(ProgressDeltaDeliveryResult.Status.RETRY)
        return ProgressDeltaDeliveryResult(ProgressDeltaDeliveryResult.Status.ACKNOWLEDGED)
    }
}
