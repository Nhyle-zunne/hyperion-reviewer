package com.nhyle.hyperionmaritime.data.sync

import org.json.JSONArray
import org.json.JSONObject

/**
 * V2 preparation only. Does not change the existing Room/full-snapshot sync.
 *
 * Once the V2 Edge Function and durable, owner-scoped retry outbox are deployed,
 * this planner can emit changed question items rather than sending the whole
 * bookmark/mistake bank. All invalid/missing versions fall back to v1 recovery;
 * the caller MUST NOT acknowledge or clear local changes in that case.
 */
internal object ProgressSectionDeltaPlanner {
    private const val BOOKMARKS = "hyperion-bookmarks"
    private const val VAULT = "hyperion-vault"
    private const val DELETIONS = "hyperion-progress-deletions"
    private const val MAX_BYTES = 524288
    private val supported = setOf(
        "hyperion-history", "hyperion-study-time", BOOKMARKS, VAULT,
        "hyperion-saved-practice", "hyperion-mock-exam-plan-v1",
        "hyperion-question-attempt-history", DELETIONS
    )
    private val versionPattern = Regex("^[a-fA-F0-9]{32}$")

    private fun fallback() = JSONObject().put("needsFullRecovery",true)
        .put("updates",JSONArray())

    private fun stringOrNull(storage: JSONObject, key: String): String? {
        val value = storage.opt(key)
        if (value == null || value == JSONObject.NULL) return null
        require(value is String) { "Unexpected progress field type" }
        return value
    }
    private fun same(a: String?, b: String?): Boolean {
        if (a == b) return true
        return LearningProgressStorageComparator.sameStorage(
            JSONObject().put("field",a ?: JSONObject.NULL),
            JSONObject().put("field",b ?: JSONObject.NULL)
        )
    }
    private fun sameObject(a: JSONObject, b: JSONObject): Boolean =
        same(a.toString(),b.toString())

    private fun wrapper(raw: String?): JSONObject {
        val result = if (raw == null) JSONObject().put("version",0)
            .put("state",JSONObject().put("items",JSONArray())) else JSONObject(raw)
        val items = result.optJSONObject("state")?.optJSONArray("items")
            ?: throw IllegalArgumentException("Invalid saved question collection")
        val found = mutableSetOf<String>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: throw IllegalArgumentException("Invalid question")
            val id = item.optString("id")
            require(id.isNotBlank() && found.add(id)) { "Question ID missing/duplicated" }
        }
        return result
    }
    private fun byId(array: JSONArray): Map<String,JSONObject> = buildMap {
        for(i in 0 until array.length()){
            val q=array.getJSONObject(i)
            put(q.getString("id"),q)
        }
    }
    private fun ledger(raw: String?): JSONObject {
        val result = if(raw==null) JSONObject().put("version",1)
            .put("bookmarks",JSONObject()).put("vault",JSONObject()) else JSONObject(raw)
        for(group in listOf("bookmarks","vault")){
            if(result.has(group) && result.optJSONObject(group)==null)
                throw IllegalArgumentException("Invalid progress tombstones")
        }
        return result
    }
    private fun canDelete(
        ledger: JSONObject, collection: String, id: String, old: JSONObject
    ): Boolean {
        val marker=ledger.optJSONObject(collection)?.optJSONObject(id) ?: return false
        val time=old.optLong(if(collection=="bookmarks") "savedAt" else "lastWrongAt",0)
        return marker.optBoolean("deleted",false) && marker.has("at") &&
            marker.optLong("at",-1L)>=time
    }
    private fun compactCollection(
        key:String, before:String?, after:String?, nextLedger:JSONObject
    ): String? {
        val previous=wrapper(before)
        val current=wrapper(after)
        val priorItems=byId(previous.getJSONObject("state").getJSONArray("items"))
        val newItems=current.getJSONObject("state").getJSONArray("items")
        val currentItems=byId(newItems)
        val collection=if(key==BOOKMARKS) "bookmarks" else "vault"
        for((id,old) in priorItems) {
            if(!currentItems.containsKey(id) && !canDelete(nextLedger,collection,id,old))
                throw IllegalArgumentException("Removal without a tombstone")
        }

        val priorMetadata=JSONObject(previous.toString())
        priorMetadata.getJSONObject("state").put("items",JSONArray())
        val newMetadata=JSONObject(current.toString())
        newMetadata.getJSONObject("state").put("items",JSONArray())
        if(!sameObject(priorMetadata,newMetadata))return after

        val changes=JSONArray()
        for(i in 0 until newItems.length()){
            val value=newItems.getJSONObject(i)
            val previousValue=priorItems[value.getString("id")]
            if(previousValue==null || !sameObject(value,previousValue))changes.put(value)
        }
        if(changes.length()==0)return null
        newMetadata.getJSONObject("state").put("items",changes)
        return newMetadata.toString()
    }

    private fun compactLedger(before:String?,after:String?):String? {
        val prior=ledger(before)
        val current=ledger(after)
        val output=JSONObject().put("version",1)
        var changed=false
        for(group in listOf("bookmarks","vault")){
            val old=prior.optJSONObject(group) ?: JSONObject()
            val newer=current.optJSONObject(group) ?: JSONObject()
            val changedEntries=JSONObject()
            val keys=newer.keys()
            while(keys.hasNext()){
                val id=keys.next()
                val marker=newer.optJSONObject(id) ?: throw IllegalArgumentException("Invalid deletion marker")
                require(marker.opt("deleted") is Boolean && marker.opt("at") is Number){
                    "Malformed deletion mutation"
                }
                val priorMarker=old.optJSONObject(id)
                if(priorMarker==null || !sameObject(priorMarker,marker)){
                    changedEntries.put(id,marker)
                    changed=true
                }
            }
            val previousKeys=old.keys()
            while(previousKeys.hasNext()){
                require(newer.has(previousKeys.next())){ "Missing older deletion marker" }
            }
            output.put(group,changedEntries)
        }
        return if(changed)output.toString() else null
    }

    fun plan(
        previousStorage: JSONObject,
        currentStorage: JSONObject,
        versions: JSONObject,
        dirtyKeys: List<String>
    ): JSONObject {
        if(dirtyKeys.size>8 || dirtyKeys.size!=dirtyKeys.toSet().size)return fallback()
        return try {
            val mutations=JSONArray()
            val nextLedger=ledger(stringOrNull(currentStorage,DELETIONS))
            for(key in dirtyKeys){
                val version=versions.optString(key,"")
                if(key !in supported || !versionPattern.matches(version))return fallback()
                val previous=stringOrNull(previousStorage,key)
                val current=stringOrNull(currentStorage,key)
                if(same(previous,current))continue
                val value=when(key){
                    BOOKMARKS,VAULT -> compactCollection(key,previous,current,nextLedger)
                    DELETIONS -> compactLedger(previous,current)
                    else -> current?.also { JSONObject(it) }
                }
                if(value!=null && value.toByteArray(Charsets.UTF_8).size>MAX_BYTES)return fallback()
                if(value!=null || (current==null && key!=BOOKMARKS && key!=VAULT)){
                    mutations.put(JSONObject().put("key",key)
                        .put("value",value ?: JSONObject.NULL)
                        .put("expected_version",version))
                }
            }
            for(key in listOf(BOOKMARKS,VAULT)){
                if(key !in dirtyKeys)continue
                val previous=stringOrNull(previousStorage,key)
                val current=stringOrNull(currentStorage,key)
                if(previous==null || current==null)continue
                val oldItems=byId(wrapper(previous).getJSONObject("state").getJSONArray("items"))
                val currentItems=byId(wrapper(current).getJSONObject("state").getJSONArray("items"))
                if(oldItems.keys.any { it !in currentItems } && DELETIONS !in dirtyKeys)return fallback()
            }
            JSONObject().put("needsFullRecovery",false).put("updates",mutations)
        } catch (_: Exception) { fallback() }
    }
}
