package com.nhyle.hyperionmaritime.ui.screens

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import com.nhyle.hyperionmaritime.ui.components.SessionTimerBox
import com.nhyle.hyperionmaritime.ui.components.AreYouStillThereDialog
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
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.model.CurrentQuestionStats
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.ui.components.MaritimeCompetenceBadge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.nhyle.hyperionmaritime.ui.components.MaritimeDifficultyBadge
import com.nhyle.hyperionmaritime.ui.components.QuestionSettingsMenu
import com.nhyle.hyperionmaritime.ui.components.OptionCard
import com.nhyle.hyperionmaritime.ui.components.QuestionAttemptStatsCard
import com.nhyle.hyperionmaritime.ui.components.QuestionPaletteDialog
import com.nhyle.hyperionmaritime.ui.theme.FailRed
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.ui.theme.MaritimeGoldBright
import com.nhyle.hyperionmaritime.ui.theme.PassGreen
import com.nhyle.hyperionmaritime.ui.theme.PureWhite
import com.nhyle.hyperionmaritime.ui.theme.SeaCyan
import com.nhyle.hyperionmaritime.viewmodel.DrillDeliveryMode
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

@Composable
fun PracticeQuizScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val deliveryMode by viewModel.activeDrillDeliveryMode.collectAsState()
    val showIdleDialog by viewModel.studyTimeTracker.showIdleDialog.collectAsState()

    val interactionModifier = modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent()
                viewModel.recordStudyInteraction()
            }
        }
    }

    val currentDensity = LocalDensity.current
    val textScale by viewModel.questionTextScale.collectAsState()
    val scaledDensity = remember(currentDensity, textScale) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * textScale
        )
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        if (deliveryMode == DrillDeliveryMode.INTENSIVE) {
            IntensiveDrillScreen(viewModel = viewModel, modifier = interactionModifier)
        } else {
            StandardDrillScreen(viewModel = viewModel, modifier = interactionModifier)
        }
    }

    if (showIdleDialog) {
        AreYouStillThereDialog(
            onStillHere = { viewModel.studyTimeTracker.onUserConfirmedStillHere() },
            onSaveAndExit = {
                viewModel.studyTimeTracker.pauseTimer()
                viewModel.exitDrillToCompetenceConfig()
            }
        )
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
                shape = RectangleShape,
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )

                    Text(
                        text = "Intensive Drill Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RectangleShape,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { viewModel.resetIntensiveDrill() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape
                    ) {
                        Text("Return to Dashboard", color = PureWhite, fontWeight = FontWeight.Bold)
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Return to Dashboard", color = PureWhite)
                }
            }
        }
        return
    }

    val currentQuestion = intensiveQueue.first()
    val selectedChoiceIndex by viewModel.selectedChoiceIndex.collectAsState()
    val isAnswerEvaluated by viewModel.isAnswerEvaluated.collectAsState()
    val isSubmitted = isAnswerEvaluated
    val isAnswerCorrect = isSubmitted && selectedChoiceIndex == currentQuestion.correctAnswerIndex
    val isBookmarked = bookmarkedIds.contains(currentQuestion.id)
    val colors = LocalSpaghettiColors.current

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
                    IconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier.testTag("exit_intensive_drill_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
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
                                shape = RectangleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = "RECYCLING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${currentQuestion.functionId} • ${currentQuestion.competenceId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { viewModel.toggleBookmark(currentQuestion.id) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    QuestionSettingsMenu(viewModel = viewModel, showDrillAutomations = true)
                }
            }
        }

        // Live Statistics Header: "Cleared: X / Total • In Queue: Y"
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cleared: $clearedCount / $initialTotal",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PassGreen
                    )
                    Text(
                        text = "In Queue: ${intensiveQueue.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = PassGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // Header Area: Row 1 (ITEM tag & Timer box) and Row 2 (Topic title & Difficulty tag)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text(
                        text = "ITEM ${clearedCount + 1}/$initialTotal · ${currentQuestion.competenceId}".uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = colors.foreground,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                SessionTimerBox(studyTimeTracker = viewModel.studyTimeTracker)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "TOPIC: ${currentQuestion.topic.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .padding(top = 4.dp),
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Clip
                )
                MaritimeDifficultyBadge(difficulty = currentQuestion.difficulty, showPoints = true)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Single-Card Question Paging View
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Question Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                currentQuestion.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = selectedChoiceIndex == optIndex

                    DrillOptionCard(
                        choiceIndex = optIndex,
                        choiceText = optionText,
                        isSelected = isSelected,
                        isCorrect = optIndex == currentQuestion.correctAnswerIndex,
                        isEvaluated = isSubmitted,
                        onOptionSelected = { chosenIdx ->
                            if (!isSubmitted) {
                                viewModel.selectIntensiveChoice(chosenIdx)
                            }
                        },
                        showTrailingStatus = false,
                        modifier = Modifier.testTag("intensive_option_card_$optIndex")
                    )
                }
            }

            // Immediate Feedback Card - Simplistic & Minimalist
            AnimatedVisibility(
                visible = isSubmitted,
                enter = fadeIn() + expandVertically()
            ) {
                val isDark = colors.isDark
                val statusColor = if (isAnswerCorrect) {
                    if (isDark) PassGreen else Color(0xFF15803D)
                } else {
                    if (isDark) FailRed else Color(0xFFDC2626)
                }
                val feedbackBg = if (isDark) colors.surface2 else (if (isAnswerCorrect) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                val titleText = if (isAnswerCorrect) "Correct" else "Incorrect"
                val subtitleText = if (isAnswerCorrect) {
                    "Question cleared from queue"
                } else {
                    "Question moved to end of queue"
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("intensive_feedback_card"),
                    shape = RectangleShape,
                    color = feedbackBg,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) colors.muted else Color(0xFF334155)
                        )
                    }
                }
            }

            // Question Attempt Stats Card
            val currentIntensiveStats by viewModel.currentQuestionStats.collectAsState()
            val isIntensiveStatsMatching = currentIntensiveStats != null &&
                currentIntensiveStats?.questionId == currentQuestion.id &&
                currentIntensiveStats?.track.equals(currentQuestion.track.name, ignoreCase = true)

            AnimatedVisibility(
                visible = isSubmitted && isIntensiveStatsMatching,
                enter = fadeIn() + expandVertically()
            ) {
                currentIntensiveStats?.let { stats ->
                    QuestionAttemptStatsCard(stats = stats)
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
                    !isSubmitted && selectedChoiceIndex == null -> "Select an answer to proceed"
                    !isSubmitted -> "Check Answer"
                    isAnswerCorrect && intensiveQueue.size == 1 -> "Finish Drill"
                    else -> "Next Question"
                }

                Button(
                    onClick = {
                        if (!isSubmitted) {
                            viewModel.confirmIntensiveAnswer()
                        } else {
                            viewModel.advanceIntensiveQuestion()
                        }
                    },
                    enabled = selectedChoiceIndex != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubmitted && isAnswerCorrect) PassGreen else MaterialTheme.colorScheme.primary,
                        contentColor = PureWhite,
                        disabledContainerColor = colors.surface2,
                        disabledContentColor = colors.muted
                    ),
                    border = if (selectedChoiceIndex == null) BorderStroke(1.dp, colors.border) else null,
                    shape = RectangleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("intensive_advance_button")
                ) {
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedChoiceIndex != null) PureWhite else colors.muted,
                        fontSize = 14.sp
                    )
                    if (isSubmitted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = PureWhite,
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
            modifier = Modifier
                .border(BorderStroke(1.5.dp, colors.border), RectangleShape)
                .testTag("exit_drill_dialog"),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors.error, RectangleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXIT DRILL SESSION",
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            },
            text = {
                Text(
                    text = "Save intensive drill progress and return to competence configuration? Current queue state is recorded so you can resume anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.exitDrillToCompetenceConfig()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = PureWhite
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.testTag("exit_drill_confirm_button")
                ) {
                    Text("SAVE & EXIT", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.testTag("exit_drill_dismiss_button")
                ) {
                    Text("CONTINUE", color = colors.foreground)
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
    val colors = LocalSpaghettiColors.current

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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Return to Dashboard", color = PureWhite)
                }
            }
        }
        return
    }

    val question = activeQuestions[currentIndex]
    val currentState = userStates[question.id]
    val isBookmarked = bookmarkedIds.contains(question.id)
    val isFlagged = currentState?.isFlagged == true

    val selectedChoiceIndex by viewModel.selectedChoiceIndex.collectAsState()
    val isAnswerEvaluated by viewModel.isAnswerEvaluated.collectAsState()

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
                    IconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier.testTag("exit_drill_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
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
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { viewModel.toggleBookmark(question.id) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    QuestionSettingsMenu(viewModel = viewModel, showDrillAutomations = true)
                }
            }
        }

        val currentProgress = (currentIndex + 1).toFloat() / activeQuestions.size.coerceAtLeast(1).toFloat()
        LinearProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Header Area: Row 1 (ITEM tag & Timer box) and Row 2 (Topic title & Difficulty tag)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text(
                        text = "ITEM ${currentIndex + 1}/${activeQuestions.size} · ${question.competenceId}".uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = colors.foreground,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                SessionTimerBox(studyTimeTracker = viewModel.studyTimeTracker)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "TOPIC: ${question.topic.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .padding(top = 4.dp),
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Clip
                )
                MaritimeDifficultyBadge(difficulty = question.difficulty, showPoints = true)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {


            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
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

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = selectedChoiceIndex == optIndex
                    val isChoiceCorrect = optIndex == question.correctAnswerIndex

                    DrillOptionCard(
                        choiceIndex = optIndex,
                        choiceText = optionText,
                        isSelected = isSelected,
                        isCorrect = isChoiceCorrect,
                        isEvaluated = isAnswerEvaluated,
                        onOptionSelected = { chosenIdx ->
                            viewModel.selectOption(currentIndex, chosenIdx)
                        },
                        modifier = Modifier.testTag("option_card_$optIndex")
                    )
                }
            }

            // Question Attempt Stats Card
            val currentStandardStats by viewModel.currentQuestionStats.collectAsState()
            val isStandardStatsMatching = currentStandardStats != null &&
                currentStandardStats?.questionId == question.id &&
                currentStandardStats?.track.equals(question.track.name, ignoreCase = true)

            AnimatedVisibility(
                visible = isAnswerEvaluated && isStandardStatsMatching,
                enter = fadeIn() + expandVertically()
            ) {
                currentStandardStats?.let { stats ->
                    QuestionAttemptStatsCard(stats = stats)
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
                    shape = RectangleShape
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
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = {
                        if (!isAnswerEvaluated) {
                            viewModel.confirmOrNextQuestion()
                        } else {
                            if (!isLastQuestion) {
                                viewModel.confirmOrNextQuestion()
                            } else {
                                viewModel.submitExam()
                            }
                        }
                    },
                    enabled = isAnswerEvaluated || selectedChoiceIndex != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = PureWhite,
                        disabledContainerColor = colors.surface2,
                        disabledContentColor = colors.muted
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.testTag("next_action_button")
                ) {
                    val buttonText = when {
                        !isAnswerEvaluated -> "Check Answer"
                        !isLastQuestion -> "Next Question"
                        else -> "Finish Drill"
                    }
                    Text(
                        text = buttonText,
                        color = if (isAnswerEvaluated || selectedChoiceIndex != null) PureWhite else colors.muted,
                        fontWeight = FontWeight.Bold
                    )
                    if (isAnswerEvaluated && !isLastQuestion) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Question",
                            tint = PureWhite,
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
            userStates = userStates,
            bookmarkedIds = bookmarkedIds,
            questionIds = activeQuestions.map { it.id },
            onQuestionClick = { index ->
                viewModel.jumpToQuestion(index)
            },
            onDismiss = {
                viewModel.setPaletteOpen(false)
            },
            onJumpToNumber = { index ->
                viewModel.jumpToQuestion(index)
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            modifier = Modifier
                .border(BorderStroke(1.5.dp, colors.border), RectangleShape)
                .testTag("exit_drill_dialog"),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors.error, RectangleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXIT DRILL SESSION",
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            },
            text = {
                Text(
                    text = "Save session progress and return to competence configuration? Answers and progress are recorded in telemetry so you can resume anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.exitDrillToCompetenceConfig()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = PureWhite
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.testTag("exit_drill_confirm_button")
                ) {
                    Text("SAVE & EXIT", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.testTag("exit_drill_dismiss_button")
                ) {
                    Text("CONTINUE", color = colors.foreground)
                }
            }
        )
    }
}

private fun getFormattedExplanation(question: com.nhyle.hyperionmaritime.data.model.Question): String {
    val raw = question.explanation.trim()
    var cleaned = raw
        .replace(Regex("(?i)Competency Standard Reference & Rationale:?"), "")
        .replace(Regex("(?i)MARINA Official STCW Competence \\w+ \\(Item #\\d+\\):?"), "")
        .trim()

    val sentenceCount = if (cleaned.isBlank()) 0 else cleaned.split(Regex("[.!?]\\s+")).size

    if (cleaned.isNotBlank() && sentenceCount >= 4) {
        return cleaned
    }

    val correctLetter = when (question.correctAnswerIndex) {
        0 -> "A"
        1 -> "B"
        2 -> "C"
        3 -> "D"
        else -> "A"
    }
    val correctText = question.options.getOrNull(question.correctAnswerIndex) ?: ""
    val concept = when {
        question.id.contains("C1") -> "passage planning and precise terrestrial/celestial navigation"
        question.id.contains("C2") -> "safe navigational watchkeeping under COLREGs and STCW guidelines"
        question.id.contains("C3") -> "radar plotting, ARPA target tracking, and collision avoidance procedures"
        question.id.contains("C4") -> "ECDIS system configuration, safety contour management, and chart display standards"
        question.id.contains("C5") -> "bridge emergency response, life-saving maneuvers, and search and rescue coordinates"
        question.id.contains("C7") -> "official maritime communication protocols and IMO Standard Marine Communication Phrases"
        question.id.contains("C9") -> "vessel maneuvering, hydrodynamic interactions, and anchor handling procedures"
        question.id.contains("C10") -> "cargo loading, stability verification, securing, and damage prevention protocols"
        question.id.contains("C11") -> "structural defect inspections, ballast tank safety, and hatch cover integrity"
        question.id.contains("C12") -> "MARPOL compliance, pollution prevention systems, and environmental watchkeeping"
        question.id.contains("C13") -> "ship stability calculations, intact buoyancy, and vessel seaworthiness standards"
        question.id.contains("C17") -> "legislative compliance, MLC guidelines, Port State Control, and STCW legal mandates"
        else -> "standard operational procedures and maritime safety standards"
    }

    val s1 = "The correct answer is Option $correctLetter: \"$correctText\", which represents the industry standard for $concept."
    val s2 = "Under STCW and SOLAS international frameworks, adhering to this specific procedure is vital to maintain bridge safety, environmental protection, and overall vessel integrity."
    val s3 = "Choosing any other option deviates from established maritime guidelines and introduces critical watchkeeping hazards or structural failures during open-sea transit."
    val s4 = "As a rule of thumb for the exam, associate this question directly with official STCW requirements to ensure compliance and avoid operational penalties."

    return if (cleaned.isNotBlank()) {
        "$cleaned\n\n$s1 $s2 $s3 $s4"
    } else {
        "$s1 $s2 $s3 $s4"
    }
}


