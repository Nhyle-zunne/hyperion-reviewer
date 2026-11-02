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
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel

@Composable
fun KeywordQuestionText(
    viewModel: ReviewerViewModel,
    track: String,
    questionId: String,
    text: String,
    style: TextStyle
) {
    val enabled by viewModel.highlightKeywords.collectAsState()
    KeywordQuestionEditor(enabled, track, questionId, text, style)
}

@Composable
internal fun KeywordQuestionEditor(
    enabled: Boolean,
    track: String,
    questionId: String,
    text: String,
    style: TextStyle
) {
    val context = LocalContext.current
    val storage = remember(context) { QuestionKeywords(context) }
    val key = remember(track, questionId, text) { storage.key(track, questionId, text) }
    val words = remember(text) { Regex("[\\p{L}\\p{N}]+(?:['’\\-][\\p{L}\\p{N}]+)*").findAll(text).toList() }
    // Discard unsaved edits on navigation or when highlighting is switched off.
    var editing by remember(key, enabled) { mutableStateOf(false) }
    var selected by remember(key, enabled) { mutableStateOf(storage.load(key, words.size)) }
    val annotated = remember(text, selected, enabled, words) {
        buildAnnotatedString {
            var cursor = 0
            words.forEachIndexed { index, word ->
                append(text.substring(cursor, word.range.first))
                if (enabled && index in selected) {
                    withStyle(SpanStyle(background = Color(0xFFFFDF70), color = Color(0xFF171717))) { append(word.value) }
                } else append(word.value)
                cursor = word.range.last + 1
            }
            append(text.substring(cursor))
        }
    }
    Column(Modifier.fillMaxWidth()) {
        if (enabled && editing) {
            Text("Tap words to highlight or remove highlights.")
            ClickableText(modifier = Modifier.testTag("keyword_question_text"), text = annotated, style = style, onClick = { offset ->
                val index = words.indexOfFirst { offset in it.range }
                if (index >= 0) selected = if (index in selected) selected - index else selected + index
            })
        } else {
            Text(text = annotated, style = style)
        }
        if (enabled) {
            TextButton(modifier = Modifier.align(Alignment.End), onClick = {
                if (editing) storage.save(key, selected)
                editing = !editing
            }) { Text(if (editing) "Save Keywords" else "Edit Keyword") }
        }
    }
}
