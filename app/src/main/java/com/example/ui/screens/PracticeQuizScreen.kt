package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Psychology
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Difficulty
import com.example.ui.components.OptionCard
import com.example.ui.components.QuestionPaletteDialog
import com.example.ui.theme.FailRed
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.DrillDeliveryMode
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun PracticeQuizScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val deliveryMode by viewModel.activeDrillDeliveryMode.collectAsState()

    if (deliveryMode == DrillDeliveryMode.INTENSIVE) {
        IntensiveDrillScreen(viewModel = viewModel, modifier = modifier)
    } else {
        StandardDrillScreen(viewModel = viewModel, modifier = modifier)
    }
}

/**
 * Intensive Mode Drill Screen with dynamic recycling queue.
 * - If answer is correct: remove item from queue and mark cleared.
 * - If answer is wrong: show rationale and push item to the very end of the queue.
 * - Drill ends only when all questions are answered correctly.
 * - Live stats: "Cleared: X / Total • In Queue: Y"
 */
@Composable
private fun IntensiveDrillScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val intensiveQueue by viewModel.intensiveQueue.collectAsState()
    val initialTotal by viewModel.intensiveInitialTotal.collectAsState()
    val clearedCount by viewModel.intensiveClearedCount.collectAsState()
    val currentQuestionState by viewModel.intensiveCurrentQuestionState.collectAsState()
    val isCompleted by viewModel.intensiveCompleted.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    // Completion View
    if (isCompleted) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, PassGreen)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaritimeGoldBright,
                        modifier = Modifier.size(56.dp)
                    )

                    Text(
                        text = "Intensive Drill Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PassGreen.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, PassGreen)
                    ) {
                        Text(
                            text = "ALL $initialTotal QUESTIONS MASTERED",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PassGreen,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = "Every question in this intensive session has been successfully answered and cleared through spaced repetition reinforcement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { viewModel.resetIntensiveDrill() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaritimeGoldBright),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Return to Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    if (intensiveQueue.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "No questions in queue.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = { viewModel.navigateTo(Screen.Home) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaritimeGoldBright)
                ) {
                    Text("Return to Dashboard", color = Color.Black)
                }
            }
        }
        return
    }

    val currentQuestion = intensiveQueue.first()
    val isSubmitted = currentQuestionState?.selectedIndex != null
    val isAnswerCorrect = isSubmitted && currentQuestionState?.selectedIndex == currentQuestion.correctAnswerIndex
    val isBookmarked = bookmarkedIds.contains(currentQuestion.id)

    val progressFraction = if (initialTotal > 0) clearedCount.toFloat() / initialTotal.toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Intensive Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFF59E0B)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Intensive Drill",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0x33F59E0B)
                            ) {
                                Text(
                                    text = "RECYCLING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B),
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${currentQuestion.functionId} • ${currentQuestion.competenceId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }

                IconButton(onClick = { viewModel.toggleBookmark(currentQuestion.id) }) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) MaritimeGoldBright else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Statistics Header: "Cleared: X / Total • In Queue: Y"
        Surface(
            color = Color(0xFF0F1E36),
            border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cleared: $clearedCount / $initialTotal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PassGreen
                    )
                    Text(
                        text = "In Queue: ${intensiveQueue.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PassGreen,
                    trackColor = Color(0xFF334155)
                )
            }
        }

        // Single-Card Question Paging View
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Difficulty chip & Topic row
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
                        text = "Competence: ${currentQuestion.competenceId}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (currentQuestion.difficulty) {
                        Difficulty.EASY -> Color(0xFF0F3E38)
                        Difficulty.MODERATE -> Color(0xFF382D12)
                        Difficulty.DIFFICULT -> Color(0xFF43161C)
                    },
                    border = BorderStroke(
                        1.dp,
                        when (currentQuestion.difficulty) {
                            Difficulty.EASY -> PassGreen
                            Difficulty.MODERATE -> MaritimeGoldBright
                            Difficulty.DIFFICULT -> FailRed
                        }
                    )
                ) {
                    Text(
                        text = when (currentQuestion.difficulty) {
                            Difficulty.EASY -> "Easy"
                            Difficulty.MODERATE -> "Moderate"
                            Difficulty.DIFFICULT -> "Difficult"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (currentQuestion.difficulty) {
                            Difficulty.EASY -> PassGreen
                            Difficulty.MODERATE -> MaritimeGoldBright
                            Difficulty.DIFFICULT -> FailRed
                        }
                    )
                }
            }

            Text(
                text = "TOPIC: ${currentQuestion.topic.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = SeaCyan,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )

            // Question Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = currentQuestion.questionText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Options
            val optionLetters = listOf("A", "B", "C", "D")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                currentQuestion.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = currentQuestionState?.selectedIndex == optIndex
                    val isCorrect = if (isSubmitted) {
                        when {
                            optIndex == currentQuestion.correctAnswerIndex -> true
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
                            if (!isSubmitted) {
                                viewModel.selectIntensiveOption(optIndex)
                            }
                        }
                    )
                }
            }

            // Immediate Rationale Card
            AnimatedVisibility(
                visible = isSubmitted,
                enter = fadeIn() + expandVertically()
            ) {
                val statusColor = if (isAnswerCorrect) PassGreen else FailRed
                val statusText = if (isAnswerCorrect) "CORRECT — ITEM CLEARED ✓" else "INCORRECT — RECYCLED TO END ↺"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.7f))
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
                            text = currentQuestion.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isAnswerCorrect) {
                            Text(
                                text = "✓ This question is permanently removed from the active queue.",
                                style = MaterialTheme.typography.labelSmall,
                                color = PassGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "↺ This question has been re-inserted at the very end of the queue. You will see it again until answered correctly.",
                                style = MaterialTheme.typography.labelSmall,
                                color = FailRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Bottom Bar for Intensive Mode
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val buttonText = when {
                    !isSubmitted -> "Select an answer to proceed"
                    isAnswerCorrect && intensiveQueue.size == 1 -> "Finish Drill (All Mastered! 🎉)"
                    isAnswerCorrect -> "Next Question (Cleared ✓)"
                    else -> "Next Question (Recycled ↺)"
                }

                Button(
                    onClick = {
                        if (isSubmitted) {
                            viewModel.advanceIntensiveQuestion()
                        }
                    },
                    enabled = isSubmitted,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAnswerCorrect) PassGreen else Color(0xFFF59E0B),
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF334155),
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("intensive_advance_button")
                ) {
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (isSubmitted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Intensive Drill?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to stop this intensive drill? Current progress will be saved.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.resetIntensiveDrill()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Exit Drill", color = PureWhite)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }) {
                    Text("Continue Drill")
                }
            }
        )
    }
}

/**
 * Standard Drill Mode Screen.
 */
@Composable
private fun StandardDrillScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val activeQuestions by viewModel.activeQuestions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val userStates by viewModel.userQuestionStates.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val isPaletteOpen by viewModel.isPaletteOpen.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    if (activeQuestions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "No questions found for this session.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = { viewModel.navigateTo(Screen.Home) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaritimeGoldBright)
                ) {
                    Text("Return to Dashboard", color = Color.Black)
                }
            }
        }
        return
    }

    val question = activeQuestions[currentIndex]
    val currentState = userStates[question.id]
    val isBookmarked = bookmarkedIds.contains(question.id)
    val isFlagged = currentState?.isFlagged == true
    val isSubmitted = currentState?.selectedIndex != null

    val isLastQuestion = currentIndex >= activeQuestions.size - 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Practice Top App Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaritimeGoldBright
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
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { viewModel.toggleFlag(currentIndex) }) {
                        Icon(
                            imageVector = if (isFlagged) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                            contentDescription = "Flag Question",
                            tint = if (isFlagged) MaritimeGoldBright else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

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

        val currentProgress = (currentIndex + 1).toFloat() / activeQuestions.size.coerceAtLeast(1).toFloat()
        LinearProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaritimeGoldBright,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        text = "Question ${currentIndex + 1} of ${activeQuestions.size} • ${question.competenceId}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (question.difficulty) {
                            Difficulty.EASY -> PassGreen
                            Difficulty.MODERATE -> MaritimeGoldBright
                            Difficulty.DIFFICULT -> FailRed
                        }
                    )
                }
            }

            Text(
                text = "TOPIC: ${question.topic.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = SeaCyan,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val optionLetters = listOf("A", "B", "C", "D")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            }

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

                        if (!isAnswerCorrect) {
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

        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
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
                    Text("Previous")
                }

                IconButton(onClick = { viewModel.setPaletteOpen(true) }) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Question Palette",
                        tint = MaritimeGoldBright
                    )
                }

                Button(
                    onClick = {
                        if (!isLastQuestion) {
                            viewModel.nextQuestion()
                        } else {
                            viewModel.submitExam()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaritimeGoldBright),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (!isLastQuestion) "Next" else "Finish Drill",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isLastQuestion) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Question",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (isPaletteOpen) {
        QuestionPaletteDialog(
            totalQuestions = activeQuestions.size,
            currentIndex = currentIndex,
            questionStates = userStates,
            questionIds = activeQuestions.map { it.id },
            onSelectQuestion = { index ->
                viewModel.jumpToQuestion(index)
            },
            onDismiss = {
                viewModel.setPaletteOpen(false)
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text("Exit Session?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Exit Session? Your progress and answers will be saved so you can resume later.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.navigateTo(Screen.Home)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Exit Session", color = PureWhite)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }) {
                    Text("Continue")
                }
            }
        )
    }
}
