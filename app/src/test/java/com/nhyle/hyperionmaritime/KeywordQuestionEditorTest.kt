package com.nhyle.hyperionmaritime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.createComposeRule
import com.nhyle.hyperionmaritime.data.local.QuestionKeywords
import com.nhyle.hyperionmaritime.ui.components.KeywordQuestionEditor
import com.nhyle.hyperionmaritime.ui.components.KeywordEditorState
import com.nhyle.hyperionmaritime.ui.components.rememberKeywordEditorState
import com.nhyle.hyperionmaritime.ui.screens.DrillOptionCard
import androidx.compose.foundation.layout.Column
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KeywordQuestionEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pendingDrillAnswerCannotSubmitDuringKeywordEditing() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank.init(app)
        com.nhyle.hyperionmaritime.data.util.LicenseManager.setActivated(app, true)
        com.nhyle.hyperionmaritime.data.util.LicenseManager.setOicNwProUnlocked(app, true)
        com.nhyle.hyperionmaritime.data.util.LicenseManager.init(app)
        val viewModel = com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel(app)
        viewModel.setTrack(com.nhyle.hyperionmaritime.data.model.Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice("C1", 10, false, false, false)
        assertTrue(viewModel.activeQuestions.value.isNotEmpty())
        viewModel.setHighlightKeywords(true)
        viewModel.selectOptionForQuestion(viewModel.activeQuestions.value.first().id, 0)
        compose.setContent {
            MaterialTheme { com.nhyle.hyperionmaritime.ui.screens.PracticeQuizScreen(viewModel) }
        }
        compose.onNodeWithText("Edit Keyword").performScrollTo().performClick()
        compose.onNodeWithText("Check Answer").assertIsNotEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.runOnIdle { assertFalse(viewModel.isAnswerEvaluated.value) }
        compose.onNodeWithText("Save Keywords").performScrollTo().performClick()
        compose.onNodeWithText("Check Answer").performClick()
        compose.runOnIdle { assertTrue(viewModel.isAnswerEvaluated.value) }
    }

    @Test fun choicesEditWithoutAnsweringThenSaveRestoresAnswering() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("question_keywords", Context.MODE_PRIVATE).edit().clear().commit()
        val store = QuestionKeywords(context)
        var answers = 0
        var enabled by mutableStateOf(true)
        var questionId by mutableStateOf("choices")
        var options by mutableStateOf(listOf("Ship, 12%", "Other"))
        compose.setContent {
            MaterialTheme {
                val state = rememberKeywordEditorState(enabled, "DECK", questionId, "Stem?", options)
                Column {
                    KeywordQuestionEditor(state, "Stem?", MaterialTheme.typography.bodyLarge)
                    options.forEachIndexed { index, choice ->
                        DrillOptionCard(index, choice, false, false, false, { answers++ }, keywordState = state)
                    }
                }
            }
        }
        compose.onNodeWithTag("option_card_0").performClick()
        compose.runOnIdle { assertEquals(1, answers) }
        compose.onNodeWithText("Edit Keyword").performClick()
        compose.onNodeWithTag("option_card_0").assertIsNotEnabled()
        compose.onNodeWithTag("option_card_0").performSemanticsAction(SemanticsActions.OnClick)
        compose.runOnIdle { assertEquals(1, answers) }
        tapToken("keyword_choice_Ship, 12%", 4) // comma
        tapToken("keyword_choice_Ship, 12%", 8) // percent
        tapToken("keyword_question_text", 4) // question mark
        compose.onNodeWithText("Save Keywords").performClick()
        val stem = store.key("DECK", "choices", "Stem?")
        compose.runOnIdle {
            assertEquals(setOf(2, 3), store.load(store.choiceKey(stem, "Ship, 12%"), 4))
            assertEquals(setOf(1), store.load(stem, 2))
            options = options.reversed()
        }
        compose.onNodeWithTag("option_card_0").performClick()
        compose.runOnIdle { assertEquals(2, answers) }
        compose.onNodeWithText("Edit Keyword").performClick()
        tapToken("keyword_choice_Ship, 12%", 4)
        compose.runOnIdle { enabled = false }
        compose.runOnIdle { enabled = true }
        compose.onNodeWithText("Edit Keyword").performClick()
        compose.onNodeWithText("Save Keywords").performClick()
        compose.runOnIdle {
            assertEquals(setOf(2, 3), store.load(store.choiceKey(stem, "Ship, 12%"), 4))
        }
        compose.onNodeWithText("Edit Keyword").performClick()
        tapToken("keyword_question_text", 0)
        compose.runOnIdle { questionId = "next" }
        compose.onNodeWithText("Save Keywords").assertDoesNotExist()
        compose.runOnIdle { assertEquals(setOf(1), store.load(stem, 2)) }
    }

    @Test fun failedSaveRetainsEditingAndDraft() {
        val state = KeywordEditorState(true, "stem", emptyList(), { _, _ -> emptySet() }, { false })
        compose.setContent {
            MaterialTheme { KeywordQuestionEditor(state, "Stem?", MaterialTheme.typography.bodyLarge) }
        }
        compose.onNodeWithText("Edit Keyword").performClick()
        tapToken("keyword_question_text", 4)
        compose.onNodeWithText("Save Keywords").performClick()
        compose.onNodeWithText("Save Keywords").assertIsDisplayed()
        compose.onNodeWithText("Could not save keywords. Please try again.").assertIsDisplayed()
        compose.runOnIdle { assertTrue(state.editing); assertEquals(setOf(1), state.selected("stem")) }
    }

    private fun tapToken(tag: String, offset: Int) {
        val layouts = mutableListOf<TextLayoutResult>()
        val node = compose.onNodeWithTag(tag, useUnmergedTree = true)
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        node.performTouchInput { click(layouts.single().getBoundingBox(offset).center) }
    }

    @Test fun editingRequiresSaveAndTogglePreservesSelection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("question_keywords", Context.MODE_PRIVATE).edit().clear().commit()
        val storage = QuestionKeywords(context)
        val key = storage.key("DECK", "editor", "Vessel")
        var enabled by mutableStateOf(true)
        compose.setContent {
            MaterialTheme { KeywordQuestionEditor(enabled, "DECK", "editor", "Vessel", MaterialTheme.typography.bodyLarge) }
        }
        compose.onNodeWithText("Edit Keyword").performClick()
        compose.onNodeWithText("Save Keywords").assertIsDisplayed()
        run {
            val layouts = mutableListOf<TextLayoutResult>()
            val node = compose.onNodeWithTag("keyword_question_text")
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            node.performTouchInput { click(layouts.single().getBoundingBox(1).center) }
        }
        compose.runOnIdle { assertTrue(storage.load(key, 1).isEmpty()) }
        compose.onNodeWithText("Save Keywords").performClick()
        compose.runOnIdle { assertEquals(setOf(0), storage.load(key, 1)); enabled = false }
        compose.onNodeWithText("Edit Keyword").assertDoesNotExist()
        compose.runOnIdle { enabled = true }
        compose.onNodeWithText("Edit Keyword").performClick()
        run {
            val layouts = mutableListOf<TextLayoutResult>()
            val node = compose.onNodeWithTag("keyword_question_text")
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            node.performTouchInput { click(layouts.single().getBoundingBox(1).center) }
        }
        compose.onNodeWithText("Save Keywords").performClick()
        compose.runOnIdle { assertTrue(storage.load(key, 1).isEmpty()) }
    }
}
