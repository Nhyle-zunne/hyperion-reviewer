package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Question
import com.example.data.model.QuizMode
import com.example.ui.components.MaritimeTopBar
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun BookmarksScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val bookmarkedQuestions = viewModel.getBookmarkedQuestions()

    BackHandler {
        viewModel.navigateTo(Screen.Home)
    }

    val isAppDark by viewModel.isDarkMode.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = isAppDark && isSystemDark

    val backgroundBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBg)
            .navigationBarsPadding()
    ) {
        MaritimeTopBar(
            title = "Saved Bookmarks",
            subtitle = "${bookmarkedQuestions.size} Starred Questions",
            showBackButton = true,
            onBackClick = { viewModel.navigateTo(Screen.Home) },
            onLegalClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (bookmarkedQuestions.isNotEmpty()) {
                item {
                    Button(
                        onClick = {
                            viewModel.startQuizSession(mode = QuizMode.BOOKMARKS_DRILL)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaritimeGoldBright),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Drill Bookmarks",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Practice All Bookmarked Items (${bookmarkedQuestions.size})",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                items(bookmarkedQuestions) { question ->
                    BookmarkQuestionCard(
                        question = question,
                        isDark = isDark,
                        onRemoveBookmark = { viewModel.toggleBookmark(question.id) }
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFCBD5E1))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Bookmarked Questions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the bookmark ribbon icon on any question during practice or mock exams to save high-yield questions here for quick revision.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkQuestionCard(
    question: Question,
    isDark: Boolean,
    onRemoveBookmark: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val borderStroke = if (isDark) {
        androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
    }

    val tagBg = if (isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color(0xFF0369A1).copy(alpha = 0.1f)
    }

    val tagTextColor = if (isDark) SeaCyan else Color(0xFF0369A1)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val removeBookmarkColor = if (isDark) MaritimeGoldBright else Color(0xFFB00020)

    val ansBg = if (isDark) {
        Color(0xFF2E7D32).copy(alpha = 0.2f)
    } else {
        Color(0xFF16A34A).copy(alpha = 0.12f)
    }
    val ansBorder = if (isDark) {
        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.35f))
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.35f))
    }
    val ansIconColor = if (isDark) Color(0xFF4CAF50) else Color(0xFF15803D)
    val ansTextColor = if (isDark) Color.White else Color(0xFF15803D)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = borderStroke
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = tagBg
                ) {
                    Text(
                        text = "${question.functionId} • ${question.competenceId} (${question.topic})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tagTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onRemoveBookmark,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkRemove,
                        contentDescription = "Remove Bookmark",
                        tint = removeBookmarkColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            val correctLetter = when (question.correctAnswerIndex) {
                0 -> "A"
                1 -> "B"
                2 -> "C"
                3 -> "D"
                else -> ""
            }
            val optionText = question.options.getOrNull(question.correctAnswerIndex) ?: ""
            val formattedAnswer = if (correctLetter.isNotEmpty()) "$correctLetter. $optionText" else optionText

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ansBg,
                border = ansBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Correct",
                        tint = ansIconColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = ansTextColor)) {
                                append("Correct Answer: ")
                            }
                            append(formattedAnswer)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = ansTextColor
                    )
                }
            }
        }
    }
}
