package com.nhyle.hyperionmaritime.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.nhyle.hyperionmaritime.data.local.QuestionKeywords

/** One draft and save transaction for a question and its choices. */
class KeywordEditorState internal constructor(
    val enabled: Boolean,
    internal val stemKey: String,
    choices: List<Pair<String, String>>,
    load: (String, Int) -> Set<Int>,
    private val persist: (Map<String, Set<Int>>) -> Boolean
) {
    var editing by mutableStateOf(false)
        private set
    var saveFailed by mutableStateOf(false)
        private set
    private val choiceKeys = choices.associate { (key, text) -> text to key }
    private var draft by mutableStateOf((listOf(stemKey) + choices.map { it.first })
        .associateWith { load(it, Int.MAX_VALUE) })

    internal fun choiceKey(text: String): String = choiceKeys.getValue(text)
    internal fun selected(key: String): Set<Int> = draft[key].orEmpty()
    internal fun toggle(key: String, token: Int) {
        if (!enabled || !editing) return
        val selected = selected(key)
        draft = draft + (key to if (token in selected) selected - token else selected + token)
    }
    internal fun editOrSave() {
        if (!enabled) return
        if (!editing) {
            editing = true
        } else {
            val saved = runCatching { persist(draft) }.getOrDefault(false)
            saveFailed = !saved
            if (saved) editing = false
        }
    }
}

@Composable
fun rememberKeywordEditorState(
    enabled: Boolean,
    track: String,
    questionId: String,
    text: String,
    choices: List<String> = emptyList()
): KeywordEditorState {
    val context = LocalContext.current
    val storage = remember(context) { QuestionKeywords(context) }
    // Choice order is presentation only; navigation, revisions and disabling discard the draft.
    return remember(storage, enabled, track, questionId, text, choices.sorted()) {
        val key = storage.key(track, questionId, text)
        KeywordEditorState(enabled, key, choices.distinct().map { storage.choiceKey(key, it) to it },
            storage::load, storage::saveAll)
    }
}

@Composable
internal fun KeywordQuestionEditor(enabled: Boolean, track: String, questionId: String, text: String, style: TextStyle) {
    KeywordQuestionEditor(rememberKeywordEditorState(enabled, track, questionId, text), text, style)
}

@Composable
fun KeywordQuestionEditor(state: KeywordEditorState, text: String, style: TextStyle) {
    Column(Modifier.fillMaxWidth()) {
        if (state.enabled && state.editing) Text("Tap words or symbols in the question and choices to highlight or remove highlights.")
        KeywordHighlightText(state, state.stemKey, text, style, Modifier.testTag("keyword_question_text"))
        if (state.saveFailed) Text("Could not save keywords. Please try again.")
        if (state.enabled) {
            TextButton(modifier = Modifier.align(Alignment.End), onClick = state::editOrSave) {
                Text(if (state.editing) "Save Keywords" else "Edit Keyword")
            }
        }
    }
}

@Composable
fun KeywordChoiceText(state: KeywordEditorState, text: String, style: TextStyle, modifier: Modifier = Modifier) {
    KeywordHighlightText(state, state.choiceKey(text), text, style, modifier.testTag("keyword_choice_$text"))
}

@Composable
private fun KeywordHighlightText(state: KeywordEditorState, key: String, text: String, style: TextStyle, modifier: Modifier) {
    val tokens = remember(text) { QuestionKeywords.tokens(text) }
    val selected = state.selected(key)
    val annotated = remember(text, selected, state.enabled) {
        buildAnnotatedString {
            // Tokens retain legacy IDs but are rendered in their original text order.
            var cursor = 0
            tokens.withIndex().sortedBy { it.value.range.first }.forEach { (id, token) ->
                append(text.substring(cursor, token.range.first))
                if (state.enabled && id in selected) {
                    withStyle(SpanStyle(background = Color(0xFFFFDF70), color = Color(0xFF171717))) { append(token.value) }
                } else append(token.value)
                cursor = token.range.last + 1
            }
            append(text.substring(cursor))
        }
    }
    if (state.enabled && state.editing) {
        ClickableText(modifier = modifier, text = annotated, style = style, onClick = { offset ->
            val id = tokens.indexOfFirst { offset in it.range }
            if (id >= 0) state.toggle(key, id)
        })
    } else Text(text = annotated, style = style, modifier = modifier)
}
