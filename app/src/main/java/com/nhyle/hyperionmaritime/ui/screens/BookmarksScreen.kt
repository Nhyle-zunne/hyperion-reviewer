package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.QuizMode
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianButton
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.MianSectionHeader
import com.nhyle.hyperionmaritime.ui.theme.CanvasDark
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" Bookmarks Screen:
 * High contrast, 3dp top rules, sharp 2dp badges, bookmark management.
 */
@Composable
fun BookmarksScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val isActivated by viewModel.isActivated.collectAsState()
    val bookmarkedQuestions = remember(bookmarkedIds, isActivated) {
        val list = MaritimeQuestionBank.ALL_QUESTIONS.filter { it.id in bookmarkedIds }
        if (isActivated) list else list.take(5)
    }
    var searchQuery by remember { mutableStateOf("") }
    val filteredQuestions = remember(bookmarkedQuestions, searchQuery) {
        if (searchQuery.isBlank()) {
            bookmarkedQuestions
        } else {
            val query = searchQuery.trim().lowercase()
            bookmarkedQuestions.filter { q ->
                q.questionText.lowercase().contains(query) ||
                q.id.lowercase().contains(query) ||
                q.functionId.lowercase().contains(query) ||
                q.competenceId.lowercase().contains(query) ||
                q.options.any { it.lowercase().contains(query) } ||
                q.explanation.lowercase().contains(query)
            }
        }
    }
    var showClearDialog by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateBack()
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "CLEAR ALL BOOKMARKS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove all bookmarked questions? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllBookmarks()
                        showClearDialog = false
                    },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.background == CanvasDark) Color(0xFFEF4444) else Color(0xFFDC2626),
                        contentColor = Color.White
                    )
                ) {
                    Text("CLEAR ALL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearDialog = false },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.foreground)
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = "BOOKMARKED QUESTIONS",
            subtitle = "${bookmarkedQuestions.size} SAVED ITEMS",
            showBackButton = false,
            onMenuClick = onOpenDrawer,
            onClearAllClick = if (bookmarkedQuestions.isNotEmpty()) {
                { showClearDialog = true }
            } else null
        )

        if (bookmarkedQuestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NO BOOKMARKS YET",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bookmark questions during practice or mock exams to review them here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    MianRuleCard(
                        ruleColor = colors.accent,
                        containerColor = colors.surface
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "SAVED QUESTIONS (${bookmarkedQuestions.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground,
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Targeted review session focusing exclusively on your bookmarked question repository.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            MianButton(
                                onClick = {
                                    viewModel.navigateTo(Screen.PracticeConfig(isBookmarksMode = true))
                                },
                                accent = true,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "START BOOKMARKS DRILL",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "SEARCH BOOKMARKS...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.muted,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = colors.foreground,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = colors.foreground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.foreground,
                            unfocusedTextColor = colors.foreground,
                            cursorColor = colors.accent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    MianSectionHeader(title = if (searchQuery.isBlank()) "BOOKMARKED ITEMS" else "SEARCH RESULTS (${filteredQuestions.size})")
                }

                if (filteredQuestions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NO MATCHING BOOKMARKS FOUND",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.muted
                            )
                        }
                    }
                } else {
                    items(filteredQuestions) { question ->
                        BookmarkedQuestionCard(
                            question = question,
                            onRemove = { viewModel.removeBookmark(question.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BookmarkedQuestionCard(
    question: Question,
    onRemove: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    var isExpanded by androidx.compose.runtime.saveable.rememberSaveable(question.id) { 
        mutableStateOf(false) 
    }

    MianRuleCard(
        ruleColor = colors.border,
        containerColor = colors.surface,
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RectangleShape,
                    color = colors.surface2
                ) {
                    Text(
                        text = "${question.functionId} · ${question.competenceId} • ITEM ${question.id}".uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkRemove,
                            contentDescription = "Remove bookmark",
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand choices",
                        tint = colors.muted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colors.foreground,
                lineHeight = 20.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val letters = listOf("A", "B", "C", "D", "E", "F")
                    val correctOptText = question.options.getOrNull(question.correctAnswerIndex) ?: ""
                    val correctOptLetter = letters.getOrElse(question.correctAnswerIndex) { "?" }
                    val otherOptions = question.options.mapIndexed { idx, text -> idx to text }
                        .filter { (idx, text) -> idx != question.correctAnswerIndex && text.isNotBlank() }

                    // 1. Correct Answer: Listed first and emphasized
                    Surface(
                        shape = RectangleShape,
                        color = colors.surface2,
                        border = BorderStroke(1.dp, colors.success.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RectangleShape)
                                    .background(colors.success),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = correctOptLetter,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = colors.primaryForeground
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = colors.success,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "CORRECT ANSWER",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.success,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = correctOptText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.foreground,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // 2. Other Options: Listed below the correct answer
                    otherOptions.forEach { (idx, optText) ->
                        val optLetter = letters.getOrElse(idx) { "?" }
                        Surface(
                            shape = RectangleShape,
                            color = colors.surface2.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RectangleShape)
                                        .background(colors.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = optLetter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = colors.muted
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = optText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.foreground,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
