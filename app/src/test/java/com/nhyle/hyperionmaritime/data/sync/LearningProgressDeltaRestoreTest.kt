package com.nhyle.hyperionmaritime.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.local.MistakesVaultManager
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LearningProgressDeltaRestoreTest {
    @Test fun unchangedCloudBookmarkDoesNotReplaceExistingRoomRecords() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        MaritimeQuestionBank.init(context)
        val database = HyperionDatabase.getInstance(context)
        val id = "SYNC_DELTA_QUESTION_" + System.nanoTime()

        val question = JSONObject()
            .put("id", id)
            .put("track", "DECK_OIC_NW")
            .put("functionId", "F1")
            .put("competenceId", "C1")
            .put("topic", "Synchronization")
            .put("questionText", "Which choice represents a safe repeat sync?")
            .put("options", JSONArray(listOf("No rewrite", "Duplicate row", "Delete history", "Bypass license")))
            .put("correctAnswerIndex", 0)
            .put("difficulty", "EASY")
            .put("explanation", "An unchanged cloud snapshot must not rewrite local data.")
            .put("savedAt", 1_750_000_000_000L)

        fun snapshot() = JSONObject().put("storage", JSONObject().put(
            "hyperion-bookmarks",
            JSONObject().put("version", 0).put("state", JSONObject()
                .put("items", JSONArray().put(JSONObject(question.toString())))).toString()
        ))

        try {
            LearningProgressSyncAdapter.applyCloudSnapshot(context, snapshot())
            val firstBookmark = database.bookmarkDao().getAllBookmarksOnce().first { it.questionId == id }
            val firstRowId = rowId(database, "bookmarks", "questionId", id)
            val firstQuestionRow = rowId(database, "questions", "id", id)

            LearningProgressSyncAdapter.applyCloudSnapshot(context, snapshot())
            val secondBookmark = database.bookmarkDao().getAllBookmarksOnce().first { it.questionId == id }
            assertEquals(firstBookmark, secondBookmark)
            assertEquals("Same bookmark must not be reinserted", firstRowId, rowId(database, "bookmarks", "questionId", id))
            assertEquals("Same saved question must not be reinserted", firstQuestionRow, rowId(database, "questions", "id", id))

            question.put("savedAt", 1_750_000_001_000L)
            LearningProgressSyncAdapter.applyCloudSnapshot(context, snapshot())
            assertEquals(1_750_000_001_000L,
                database.bookmarkDao().getAllBookmarksOnce().first { it.questionId == id }.addedTimestamp)
        } finally {
            database.bookmarkDao().removeBookmark(id)
        }
    }

    @Test fun emptyCloudMistakesDoNotRewriteTheOfflineVaultFile() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        MaritimeQuestionBank.init(context)
        val vault = MistakesVaultManager.getVaultFile(context)
        val before = vault.takeIf { it.exists() }?.readBytes()
        try {
            if (vault.exists()) vault.delete()
            LearningProgressSyncAdapter.applyCloudSnapshot(context, JSONObject().put("storage", JSONObject()))
            assertFalse("An empty unchanged vault must not be needlessly recreated", vault.exists())
        } finally {
            if (before != null) vault.writeBytes(before) else vault.delete()
        }
    }

    private fun rowId(database: HyperionDatabase, table: String, key: String, id: String): Long {
        return database.openHelper.readableDatabase
            .query("SELECT rowid FROM $table WHERE $key=?", arrayOf(id)).use { cursor ->
                assertTrue(cursor.moveToFirst())
                cursor.getLong(0)
            }
    }
}
