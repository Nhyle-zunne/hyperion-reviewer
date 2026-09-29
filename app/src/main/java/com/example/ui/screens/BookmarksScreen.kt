package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Question
import com.example.data.model.QuizMode
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.MianButton
import com.example.ui.components.MianRuleCard
import com.example.ui.components.MianSectionHeader
import com.example.ui.theme.LocalSpaghettiColors
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" Bookmarks Screen:
 * High contrast, 3dp top rules, sharp 2dp badges, tomato red highlights.
 */
@Composable
fun BookmarksScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val bookmarkedQuestions = viewModel.getBookmarkedQuestions()

    BackHandler {
        viewModel.navigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = "BOOKMARKED QUESTIONS",
            subtitle = "${bookmarkedQuestions.size} SAVED ITEMS",
            showBackButton = true,
            onBackClick = { viewModel.navigateBack() },
            onClearAllClick = if (bookmarkedQuestions.isNotEmpty()) {
                { viewModel.clearAllBookmarks() }
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
                                    viewModel.startQuiz(
                                        questions = bookmarkedQuestions,
                                        mode = QuizMode.PRACTICE
                                    )
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
                                    text = "PRACTICE ALL BOOKMARKS",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                item {
                    MianSectionHeader(title = "BOOKMARKED ITEMS")
                }

                items(bookmarkedQuestions) { question ->
                    BookmarkedQuestionCard(
                        question = question,
                        onRemove = { viewModel.toggleBookmark(question.id) }
                    )
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

    MianRuleCard(
        ruleColor = colors.border,
        containerColor = colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = colors.surface2
                ) {
                    Text(
                        text = "${question.functionId} · ${question.competenceId} • ITEM ${question.id}".uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }

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
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colors.foreground,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Correct option preview
            val correctOptText = question.options.getOrNull(question.correctAnswerIndex) ?: ""
            val correctOptLetter = listOf("A", "B", "C", "D").getOrElse(question.correctAnswerIndex) { "?" }

            Surface(
                shape = RoundedCornerShape(2.dp),
                color = colors.surface2,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(2.dp))
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = correctOptText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.success,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
