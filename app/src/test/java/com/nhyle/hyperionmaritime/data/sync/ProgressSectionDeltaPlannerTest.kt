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
class ProgressSectionDeltaPlannerTest {
    private val bookmarks = "hyperion-bookmarks"
    private val vault = "hyperion-vault"
    private val deletions = "hyperion-progress-deletions"
    private val history = "hyperion-history"
    private fun items(vararg data: JSONObject) = JSONObject().put("version",0)
        .put("state",JSONObject().put("items",JSONArray(data.toList()))).toString()
    private fun bookmark(id: String, time: Long = 100L) = JSONObject().put("id",id)
        .put("savedAt",time).put("track","DECK_OIC_NW").put("questionText","Synthetic question")
    private fun mistake(count: Int) = JSONObject().put("id","v1").put("track","DECK_OIC_NW")
        .put("lastWrongAt",100L).put("wrongCount",count)
    private fun ledger(deleted: Boolean = false) = JSONObject().put("version",1)
        .put("bookmarks",if (deleted) JSONObject().put("q2",JSONObject().put("at",500).put("deleted",true)) else JSONObject())
        .put("vault",JSONObject()).toString()
    private fun versions() = JSONObject().put(bookmarks,"a".repeat(32))
        .put(vault,"b".repeat(32)).put(deletions,"c".repeat(32)).put(history,"d".repeat(32))

    @Test fun oneBookmarkAndOneMistakeProduceMinimalDeltaWithTombstone() {
        val old = JSONObject().put(bookmarks,items(bookmark("q1"),bookmark("q2")))
            .put(vault,items(mistake(1))).put(deletions,ledger())
            .put(history,JSONObject().put("version",0)
                .put("state",JSONObject().put("entries",JSONArray().put(JSONObject().put("id","h1")))).toString())
        val current = JSONObject(old.toString())
        current.put(bookmarks,items(bookmark("q1"),bookmark("q3",200L)))
            .put(vault,items(mistake(2)))
            .put(deletions,ledger(true))
        val plan = ProgressSectionDeltaPlanner.plan(old,current,versions(),
            listOf(bookmarks,vault,deletions))
        assertFalse(plan.optBoolean("needsFullRecovery"))
        val updates = plan.getJSONArray("updates")
        assertEquals(3,updates.length())
        assertEquals("q3",JSONObject(updates.getJSONObject(0).getString("value"))
            .getJSONObject("state").getJSONArray("items").getJSONObject(0).getString("id"))
        assertEquals(1,JSONObject(updates.getJSONObject(0).getString("value"))
            .getJSONObject("state").getJSONArray("items").length())
        assertEquals(2,JSONObject(updates.getJSONObject(1).getString("value"))
            .getJSONObject("state").getJSONArray("items").getJSONObject(0).getInt("wrongCount"))
        assertEquals(1,JSONObject(updates.getJSONObject(2).getString("value"))
            .getJSONObject("bookmarks").length())
        assertEquals("a".repeat(32),updates.getJSONObject(0).getString("expected_version"))
    }

    @Test fun noTombstoneAndUnknownSectionVersionCannotSilentlyLoseData() {
        val old=JSONObject().put(bookmarks,items(bookmark("q1"),bookmark("q2")))
            .put(deletions,ledger())
        val current=JSONObject(old.toString()).put(bookmarks,items(bookmark("q1")))
        val missingDeletion=ProgressSectionDeltaPlanner.plan(old,current,versions(),listOf(bookmarks))
        assertTrue(missingDeletion.optBoolean("needsFullRecovery"))
        assertEquals(0,missingDeletion.getJSONArray("updates").length())
        val missingVersion=ProgressSectionDeltaPlanner.plan(old,
            JSONObject(old.toString()).put(bookmarks,items(bookmark("q1"),bookmark("q2"),bookmark("q3"))),
            JSONObject(),listOf(bookmarks))
        assertTrue(missingVersion.optBoolean("needsFullRecovery"))
    }

    @Test fun changedFormattingDoesNotCauseAnotherCloudWrite() {
        val one=items(bookmark("q1"))
        val old=JSONObject().put(bookmarks,one)
        val pretty=JSONObject().put(bookmarks,JSONObject(one).toString(3))
        val plan=ProgressSectionDeltaPlanner.plan(old,pretty,versions(),listOf(bookmarks))
        assertFalse(plan.optBoolean("needsFullRecovery"))
        assertEquals(0,plan.getJSONArray("updates").length())
    }

    @Test fun oversizedSectionsRemainInRecoveryPath() {
        val original=JSONObject().put(history,"{}")
        val large=JSONObject().put(history,JSONObject().put("content","a".repeat(524289)).toString())
        val plan=ProgressSectionDeltaPlanner.plan(original,large,versions(),listOf(history))
        assertTrue(plan.optBoolean("needsFullRecovery"))
        assertEquals(0,plan.getJSONArray("updates").length())
    }
}
