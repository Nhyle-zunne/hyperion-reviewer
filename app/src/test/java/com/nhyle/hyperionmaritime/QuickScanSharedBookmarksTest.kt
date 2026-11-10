package com.nhyle.hyperionmaritime

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.model.shuffleOptionsStable
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.ReviewerRepository
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.QuestionBookmarkButton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class QuickScanSharedBookmarksTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun quickScanBookmarkIsImmediatelyLeftOfSettingsAndToggles() {
        var saved by mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                MaritimeTopBar(
                    title = "QUICK SCAN • C1",
                    showBackButton = true,
                    questionBookmarkAction = {
                        QuestionBookmarkButton(
                            isBookmarked = saved,
                            onClick = { saved = !saved }
                        )
                    },
                    settingsMenu = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        }

        val bookmark = compose.onNodeWithTag("question_bookmark_button")
        val settings = compose.onNodeWithContentDescription("Settings")
        bookmark.assertExists()
        settings.assertExists()
        compose.waitForIdle()

        val bookmarkBounds = bookmark.fetchSemanticsNode().boundsInRoot
        val settingsBounds = settings.fetchSemanticsNode().boundsInRoot
        assertTrue("Bookmark must be to the left of Settings",
            bookmarkBounds.right <= settingsBounds.left)

        bookmark.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not saved")
        )
        bookmark.performClick()
        compose.waitForIdle()
        bookmark.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Saved")
        )
        bookmark.performClick()
        compose.waitForIdle()
        bookmark.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not saved")
        )
    }

    @Test
    fun quickScanAndPracticeUseTheSameQuestionIdsAndRoomBookmarksForAllTracks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MaritimeQuestionBank.init(context)
        val db = Room.inMemoryDatabaseBuilder(context, HyperionDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = ReviewerRepository(db)
            val ids = mutableSetOf<String>()

            for (track in Track.entries) {
                // Both modes refer to this canonical bank. Quick Scan may shuffle
                // options for display, but the question identity must not change.
                val base = MaritimeQuestionBank.getQuestionsByTrack(track).firstOrNull()
                    ?: error("Missing question bank for ${track.name}")
                val quickScan = MaritimeQuestionBank.getQuestionsByCompetence(
                    base.competenceId, track, isProUnlocked = true
                ).first { it.id == base.id }
                val displayed = shuffleOptionsStable(quickScan, "quickscan")
                assertEquals("Quick Scan must use the canonical ID for ${track.name}",
                    base.id, displayed.id)
                assertTrue("Question IDs must be track-specific", ids.add(base.id))

                // This is the same repository operation used by the Practice
                // bookmark action and the Quick Scan bookmark action.
                repository.toggleBookmark(displayed.id, currentBookmarked = false)
                assertTrue(db.bookmarkDao().getAllBookmarksOnce()
                    .any { it.questionId == base.id })
                assertTrue(repository.observeBookmarkIds().first().contains(base.id))
            }

            assertEquals(Track.entries.size, db.bookmarkDao().getAllBookmarksOnce().size)

            // Removing a bookmark from either mode removes the one shared record.
            val first = ids.first()
            repository.toggleBookmark(first, currentBookmarked = true)
            assertFalse(db.bookmarkDao().getAllBookmarksOnce()
                .any { it.questionId == first })
            assertEquals(Track.entries.size - 1,
                db.bookmarkDao().getAllBookmarksOnce().size)
        } finally {
            db.close()
        }
    }
}
