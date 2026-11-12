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

    @Test fun imageOnlyPromptDoesNotOfferEmptyKeywordEditing() {
        compose.setContent {
            MaterialTheme { KeywordQuestionEditor(true, "DECK", "image-only", " ", MaterialTheme.typography.bodyLarge) }
        }
        compose.onNodeWithText("Edit Keyword").assertDoesNotExist()
        compose.onNodeWithTag("keyword_question_text").assertDoesNotExist()
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
