package com.nhyle.hyperionmaritime

import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nhyle.hyperionmaritime.ui.components.DeleteSessionLogConfirmationDialog
import com.nhyle.hyperionmaritime.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DeleteSessionConfirmationDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testDialogDisplaysCorrectContentAndButtons() {
        var confirmedId: String? = null
        var dismissed = false

        composeTestRule.setContent {
            AppTheme(isDarkMode = true) {
                DeleteSessionLogConfirmationDialog(
                    sessionId = "session_test_123",
                    onConfirmDelete = { confirmedId = it },
                    onDismiss = { dismissed = true }
                )
            }
        }

        // Verify title and message
        composeTestRule.onNodeWithTag("delete_session_dialog_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_session_dialog_title").assertTextEquals("Delete Session Log?")
        composeTestRule.onNodeWithTag("delete_session_dialog_message").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_session_dialog_message").assertTextEquals("Are you sure you want to delete this session log?")

        // Verify buttons
        composeTestRule.onNodeWithTag("delete_session_confirm_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_session_cancel_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("DELETE").assertIsDisplayed()
        composeTestRule.onNodeWithText("CANCEL").assertIsDisplayed()
    }

    @Test
    fun testCancelKeepsSession() {
        var confirmedId: String? = null
        var dismissed = false

        composeTestRule.setContent {
            AppTheme(isDarkMode = false) {
                DeleteSessionLogConfirmationDialog(
                    sessionId = "session_test_cancel",
                    onConfirmDelete = { confirmedId = it },
                    onDismiss = { dismissed = true }
                )
            }
        }

        // Tap CANCEL
        composeTestRule.onNodeWithTag("delete_session_cancel_button").performClick()

        assertTrue("Dialog should be dismissed on CANCEL", dismissed)
        assertEquals("Session must NOT be deleted on CANCEL", null, confirmedId)
    }

    @Test
    fun testDeleteRemovesExactSession() {
        var confirmedId: String? = null
        var dismissed = false

        composeTestRule.setContent {
            AppTheme(isDarkMode = true) {
                DeleteSessionLogConfirmationDialog(
                    sessionId = "session_exact_999",
                    onConfirmDelete = { confirmedId = it },
                    onDismiss = { dismissed = true }
                )
            }
        }

        // Tap DELETE
        composeTestRule.onNodeWithTag("delete_session_confirm_button").performClick()

        assertEquals("Should delete the exact session ID", "session_exact_999", confirmedId)
        assertFalse("Dismiss should not be called directly on delete", dismissed)
    }

    @Test
    fun testTwoQuickTapsDoNotDeleteMultipleSessions() {
        var deleteCallCount = 0
        var confirmedId: String? = null

        composeTestRule.setContent {
            AppTheme(isDarkMode = true) {
                DeleteSessionLogConfirmationDialog(
                    sessionId = "session_multi_tap",
                    onConfirmDelete = {
                        deleteCallCount++
                        confirmedId = it
                    },
                    onDismiss = {}
                )
            }
        }

        // Tap DELETE twice rapidly
        composeTestRule.onNodeWithTag("delete_session_confirm_button").performClick()
        composeTestRule.onNodeWithTag("delete_session_confirm_button").performClick()

        assertEquals("onConfirmDelete must be called exactly once despite quick taps", 1, deleteCallCount)
        assertEquals("session_multi_tap", confirmedId)
    }

    @Test
    fun testLightAndDarkModeRendering() {
        // Light mode
        composeTestRule.setContent {
            AppTheme(isDarkMode = false) {
                Surface {
                    DeleteSessionLogConfirmationDialog(
                        sessionId = "light_mode_session",
                        onConfirmDelete = {},
                        onDismiss = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("delete_session_log_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete Session Log?").assertIsDisplayed()

        // Dark mode
        composeTestRule.setContent {
            AppTheme(isDarkMode = true) {
                Surface {
                    DeleteSessionLogConfirmationDialog(
                        sessionId = "dark_mode_session",
                        onConfirmDelete = {},
                        onDismiss = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("delete_session_log_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete Session Log?").assertIsDisplayed()
    }

    @Test
    fun testScreenLevelGuardPreventsOpeningMultipleDialogs() {
        var sessionPendingDeleteId: String? by mutableStateOf(null)
        val deletedIds = mutableListOf<String>()

        composeTestRule.setContent {
            AppTheme(isDarkMode = true) {
                sessionPendingDeleteId?.let { targetId ->
                    DeleteSessionLogConfirmationDialog(
                        sessionId = targetId,
                        onConfirmDelete = { id ->
                            sessionPendingDeleteId = null
                            deletedIds.add(id)
                        },
                        onDismiss = {
                            sessionPendingDeleteId = null
                        }
                    )
                }
            }
        }

        // Initially no dialog
        composeTestRule.onNodeWithTag("delete_session_log_dialog").assertDoesNotExist()

        // User taps delete on session A
        if (sessionPendingDeleteId == null) {
            sessionPendingDeleteId = "session_A"
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("delete_session_log_dialog").assertIsDisplayed()

        // Rapid tap on session B while dialog is open is ignored by the guard
        if (sessionPendingDeleteId == null) {
            sessionPendingDeleteId = "session_B"
        }
        composeTestRule.waitForIdle()

        // Confirm deletion
        composeTestRule.onNodeWithTag("delete_session_confirm_button").performClick()
        composeTestRule.waitForIdle()

        // Only session A was deleted
        assertEquals(listOf("session_A"), deletedIds)
        // Dialog is now closed
        composeTestRule.onNodeWithTag("delete_session_log_dialog").assertDoesNotExist()
    }
}
