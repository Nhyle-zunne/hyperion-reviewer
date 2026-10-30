package com.nhyle.hyperionmaritime.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LearningProgressDeletionLedgerTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("hyperion_learning_progress_deletions", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("hyperion_learning_progress_deletions", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun newerDeleteMarkerFiltersOlderBookmarkAndMistakeCopies() {
        val remote = JSONObject()
            .put("version", 1)
            .put("bookmarks", JSONObject().put("BOOKMARK_Q1", JSONObject().put("at", 2_000L).put("deleted", true)))
            .put("vault", JSONObject().put("MISTAKE_Q1", JSONObject().put("at", 3_000L).put("deleted", true)))
        val merged = LearningProgressDeletionLedger.merge(context, remote)

        assertTrue(LearningProgressDeletionLedger.isDeleted(merged, "bookmarks", "BOOKMARK_Q1", 1_000L))
        assertFalse(LearningProgressDeletionLedger.isDeleted(merged, "bookmarks", "BOOKMARK_Q1", 2_500L))
        assertTrue(LearningProgressDeletionLedger.isDeleted(merged, "vault", "MISTAKE_Q1", 2_000L))
    }

    @Test
    fun aNewerRestoreWinsOverAnOlderDeletionAndRepeatedMergeIsStable() {
        LearningProgressDeletionLedger.record(context, "bookmarks", "Q1", deleted = true, at = 1_000L)
        LearningProgressDeletionLedger.record(context, "bookmarks", "Q1", deleted = false, at = 2_000L)

        val olderRemoteDelete = JSONObject()
            .put("bookmarks", JSONObject().put("Q1", JSONObject().put("at", 1_500L).put("deleted", true)))
        val firstMerge = LearningProgressDeletionLedger.merge(context, olderRemoteDelete)
        val secondMerge = LearningProgressDeletionLedger.merge(context, olderRemoteDelete)

        assertFalse(LearningProgressDeletionLedger.isDeleted(firstMerge, "bookmarks", "Q1", 1_000L))
        assertFalse(LearningProgressDeletionLedger.isDeleted(secondMerge, "bookmarks", "Q1", 1_000L))
    }
    @Test
    fun conflictingMutationsAtTheSameTimestampPreferDeletion() {
        LearningProgressDeletionLedger.record(context, "bookmarks", "Q2", deleted = true, at = 5_000L)
        val sameTimeRestore = JSONObject()
            .put("bookmarks", JSONObject().put("Q2", JSONObject().put("at", 5_000L).put("deleted", false)))

        val merged = LearningProgressDeletionLedger.merge(context, sameTimeRestore)

        assertTrue(LearningProgressDeletionLedger.isDeleted(merged, "bookmarks", "Q2", 4_000L))
    }

}
