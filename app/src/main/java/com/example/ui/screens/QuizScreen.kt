package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Difficulty
import com.example.data.model.QuizMode
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.OptionCard
import com.example.ui.components.QuestionPaletteDialog
import com.example.ui.components.TimerChip
import com.example.ui.theme.FailRed
import com.example.ui.theme.MaritimeGold
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun QuizScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val activeQuestions by viewModel.activeQuestions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val userStates by viewModel.userQuestionStates.collectAsState()
    val quizMode by viewModel.activeQuizMode.collectAsState()
    val timeRemaining by viewModel.timeRemainingSeconds.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val isPaletteOpen by viewModel.isPaletteOpen.collectAsState()
    val showSubmitDialog by viewModel.showSubmitDialog.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }

    // Intercept back button to confirm exit
    BackHandler {
        if (quizMode == QuizMode.MOCK_EXAM) {
            showExitDialog = true
        } else {
            viewModel.navigateTo(Screen.Home)
        }
    }

    if (activeQuestions.isEmpty()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                Text("No questions found for this session.", color = MaterialTheme.colorScheme.onSurface)
            }
        }
        return
    }

    val question = activeQuestions[currentIndex]
    val currentState = userStates[question.id]
    val isBookmarked = bookmarkedIds.contains(question.id)
    val isFlagged = currentState?.isFlagged == true

    val isMockExam = quizMode == QuizMode.MOCK_EXAM
    val isSubmitted = currentState?.isSubmitted == true || (!isMockExam && currentState?.selectedIndex != null)

    val optionLetters = listOf("A", "B", "C", "D")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Quiz Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (isMockExam) showExitDialog = true else viewModel.navigateTo(Screen.Home)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Quiz",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column {
                        Text(
                            text = "Question ${currentIndex + 1} of ${activeQuestions.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${question.functionId} • ${question.competenceId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaritimeGoldBright
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isMockExam) {
                        TimerChip(secondsRemaining = timeRemaining)
                    }

                    // Flag button (useful in Mock)
                    IconButton(onClick = { viewModel.toggleFlag(currentIndex) }) {
                        Icon(
                            imageVector = if (isFlagged) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                            contentDescription = "Flag Question",
                            tint = if (isFlagged) MaritimeGoldBright else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Bookmark star
                    IconButton(onClick = { viewModel.toggleBookmark(question.id) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaritimeGoldBright else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Sleek Material 3 LinearProgressIndicator at top
        val currentProgress = (currentIndex + 1).toFloat() / activeQuestions.size.coerceAtLeast(1).toFloat()
        LinearProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaritimeGoldBright,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Main Question Scroll Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Index badge & Question Weight Pill Chip row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = if (isMockExam) {
                            "Question ${currentIndex + 1} of ${activeQuestions.size} • Mock Exam"
                        } else {
                            "Question ${currentIndex + 1} of ${activeQuestions.size} • ${question.competenceId}"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Question weight pill chip: "Easy (+3 pts)", "Moderate (+6 pts)", or "Difficult (+9 pts)"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (question.difficulty) {
                        Difficulty.EASY -> Color(0xFF0F3E38)
                        Difficulty.MODERATE -> Color(0xFF382D12)
                        Difficulty.DIFFICULT -> Color(0xFF43161C)
                    },
                    border = BorderStroke(
                        1.dp,
                        when (question.difficulty) {
                            Difficulty.EASY -> PassGreen
                            Difficulty.MODERATE -> MaritimeGoldBright
                            Difficulty.DIFFICULT -> FailRed
                        }
                    )
                ) {
                    Text(
                        text = when (question.difficulty) {
                            Difficulty.EASY -> "Easy (+3 pts)"
                            Difficulty.MODERATE -> "Moderate (+6 pts)"
                            Difficulty.DIFFICULT -> "Difficult (+9 pts)"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (question.difficulty) {
                            Difficulty.EASY -> Color(0xFF4ADE80)
                            Difficulty.MODERATE -> Color(0xFFFBBF24)
                            Difficulty.DIFFICULT -> Color(0xFFF87171)
                        }
                    )
                }
            }

            // Topic Tag
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${question.competenceId}: ${question.topic}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SeaCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Question Text Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(18.dp)
                )
            }

            // 4 Options (A, B, C, D)
            question.options.forEachIndexed { optIndex, optionText ->
                val isSelected = currentState?.selectedIndex == optIndex
                val isCorrect = if (isSubmitted) {
                    when {
                        optIndex == question.correctAnswerIndex -> true
                        isSelected -> false
                        else -> null
                    }
                } else null

                OptionCard(
                    optionLetter = optionLetters.getOrElse(optIndex) { "?" },
                    optionText = optionText,
                    isSelected = isSelected,
                    isCorrect = isCorrect,
                    onClick = {
                        viewModel.selectOption(currentIndex, optIndex)
                    }
                )
            }

            // Practice Mode Immediate Rationale Card
            AnimatedVisibility(
                visible = isSubmitted,
                enter = fadeIn() + expandVertically()
            ) {
                val isAnswerCorrect = currentState?.selectedIndex == question.correctAnswerIndex
                val statusColor = if (isAnswerCorrect) PassGreen else FailRed
                val statusText = if (isAnswerCorrect) "CORRECT (+${question.difficulty.points} pts)" else "INCORRECT (0 pts)"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Competency Standard Reference & Rationale:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaritimeGoldBright
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )

                        if (!isAnswerCorrect && !isMockExam) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Added to Mistakes Notebook. Answer correctly twice to clear.",
                                style = MaterialTheme.typography.labelSmall,
                                color = FailRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Bottom Navigation Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.previousQuestion() },
                    enabled = currentIndex > 0,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prev")
                }

                // Question Palette Grid button
                IconButton(onClick = { viewModel.setPaletteOpen(true) }) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Question Palette",
                        tint = MaritimeGoldBright
                    )
                }

                if (isMockExam) {
                    Button(
                        onClick = { viewModel.setShowSubmitDialog(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaritimeGoldBright),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Submit Exam", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (currentIndex < activeQuestions.size - 1) {
                                viewModel.nextQuestion()
                            } else {
                                viewModel.submitExam()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaritimeGoldBright),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (currentIndex < activeQuestions.size - 1) "Next" else "Finish",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Question Palette Dialog
    if (isPaletteOpen) {
        QuestionPaletteDialog(
            totalQuestions = activeQuestions.size,
            currentIndex = currentIndex,
            questionStates = userStates,
            questionIds = activeQuestions.map { it.id },
            onSelectQuestion = { viewModel.jumpToQuestion(it) },
            onDismiss = { viewModel.setPaletteOpen(false) }
        )
    }

    // Submit Exam Confirmation Dialog
    if (showSubmitDialog) {
        val answeredCount = activeQuestions.count { userStates[it.id]?.selectedIndex != null }
        val unansweredCount = activeQuestions.size - answeredCount

        AlertDialog(
            onDismissRequest = { viewModel.setShowSubmitDialog(false) },
            title = {
                Text("Submit Mock Examination?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("You have answered $answeredCount of ${activeQuestions.size} questions.")
                    if (unansweredCount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚠️ Warning: $unansweredCount questions are unanswered and will be marked as 0 points.",
                            color = FailRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Are you ready to finalize and calculate your competency weighted grade?")
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitExam() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaritimeGoldBright)
                ) {
                    Text("Yes, Submit", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowSubmitDialog(false) }) {
                    Text("Continue Exam")
                }
            }
        )
    }

    // Exit Exam Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Pause / Exit Exam?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Your progress, answers, and remaining time are automatically saved. You can resume this session anytime from the dashboard.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.navigateTo(Screen.Home)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaritimeGoldBright)
                ) {
                    Text("Save & Exit", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Resume")
                }
            }
        )
    }
}
