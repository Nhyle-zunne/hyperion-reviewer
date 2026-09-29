package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Difficulty
import com.example.data.model.QuizMode
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.MianButton
import com.example.ui.components.MianRuleButton
import com.example.ui.components.MianRuleCard
import com.example.ui.components.OptionCard
import com.example.ui.components.QuestionPaletteDialog
import com.example.ui.components.SpaghettiProgressBar
import com.example.ui.components.TimerChip
import com.example.ui.theme.LocalSpaghettiColors
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" QuizScreen:
 * Visually dominant question prompt, 3dp rules instead of boxed cards, sharp square badges,
 * strict left alignment, flat high contrast surfaces, single tomato red accent.
 */
@Composable
fun QuizScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val activeQuestions by viewModel.activeQuestions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val userStates by viewModel.userQuestionStates.collectAsState()
    val quizMode by viewModel.activeQuizMode.collectAsState()
    val timeRemaining by viewModel.timeRemainingSeconds.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val isPaletteOpen by viewModel.isPaletteOpen.collectAsState()
    val showSubmitDialog by viewModel.showSubmitDialog.collectAsState()
    val isExamSubmitted by viewModel.isExamSubmitted.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }
    var showLockInDialog by remember { mutableStateOf(false) }

    // Intercept back button to enforce lock-in for Mock Exams
    BackHandler {
        if (quizMode == QuizMode.MOCK_EXAM && !isExamSubmitted) {
            showLockInDialog = true
        } else {
            viewModel.navigateBack()
        }
    }

    if (activeQuestions.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "NO QUESTIONS LOADED FOR THIS SESSION",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.muted
                )
            }
        }
        return
    }

    val question = activeQuestions[currentIndex]
    val currentState = userStates[question.id]
    val isBookmarked = bookmarkedIds.contains(question.id)
    val isFlagged = currentState?.isFlagged == true
    val isSubmitted = currentState?.isSubmitted == true || isExamSubmitted
    val optionLetters = listOf("A", "B", "C", "D")
    val isMockExam = quizMode == QuizMode.MOCK_EXAM
    val isTimed = isMockExam && timeRemaining > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ==========================================
        // 1. EDITORIAL QUIZ HEADER
        // ==========================================
        Surface(
            color = colors.surface,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (isMockExam && !isExamSubmitted) {
                                    showLockInDialog = true
                                } else {
                                    showExitDialog = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isMockExam && !isExamSubmitted) Icons.Default.Lock else Icons.Default.Close,
                                contentDescription = "Exit",
                                tint = if (isMockExam && !isExamSubmitted) colors.accent else colors.foreground
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Column {
                            Text(
                                text = "QUESTION ${currentIndex + 1} / ${activeQuestions.size}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground,
                                letterSpacing = (-0.2).sp
                            )
                            Text(
                                text = if (isMockExam) "MOCK EXAM • ${question.competenceId}" else "${question.functionId} • ${question.competenceId}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.muted,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isTimed) {
                            TimerChip(secondsRemaining = timeRemaining)
                        }

                        IconButton(
                            onClick = { viewModel.toggleBookmark(question.id) }
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) colors.accent else colors.foreground
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleFlag(currentIndex) }
                        ) {
                            Icon(
                                imageVector = if (isFlagged) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                                contentDescription = "Flag for review",
                                tint = if (isFlagged) colors.accent else colors.foreground
                            )
                        }

                        IconButton(
                            onClick = { viewModel.openQuestionPalette() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Question Palette",
                                tint = colors.foreground
                            )
                        }
                    }
                }

                // 3dp Header Progress Bar (Tomato Red fill on Border track)
                val progressFraction = (currentIndex + 1).toFloat() / activeQuestions.size.coerceAtLeast(1)
                SpaghettiProgressBar(progress = progressFraction, height = 3.dp)
            }
        }

        // ==========================================
        // 2. SCROLLABLE QUESTION CONTENT
        // ==========================================
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category, Taxonomy & Difficulty Metadata Row (Strict left alignment, 2dp corners)
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
                        text = "${question.functionId} · ${question.competenceId}".uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Start
                    )
                }

                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = colors.surface2
                ) {
                    Text(
                        text = when (question.difficulty) {
                            Difficulty.EASY -> "EASY (+3 PTS)"
                            Difficulty.MODERATE -> "MODERATE (+6 PTS)"
                            Difficulty.DIFFICULT -> "DIFFICULT (+9 PTS)"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (question.difficulty) {
                            Difficulty.EASY -> colors.success
                            Difficulty.MODERATE -> colors.foreground
                            Difficulty.DIFFICULT -> colors.accent
                        },
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }

            // Topic Tag
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = colors.surface2
            ) {
                Text(
                    text = "${question.competenceId}: ${question.topic}".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.muted,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Visually Dominant Question Text Card ("Rules, not boxes": single 3dp top rule, flat surface)
            MianRuleCard(
                ruleColor = colors.border,
                containerColor = colors.surface
            ) {
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 25.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = colors.foreground,
                    textAlign = TextAlign.Start,
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
                        if (!isExamSubmitted) {
                            viewModel.selectOption(currentIndex, optIndex)
                        }
                    }
                )
            }

            // Practice Mode Immediate Rationale Card ("Rules, not boxes")
            AnimatedVisibility(
                visible = isSubmitted,
                enter = fadeIn() + expandVertically()
            ) {
                val isAnswerCorrect = currentState?.selectedIndex == question.correctAnswerIndex
                val statusColor = if (isAnswerCorrect) colors.success else colors.accent
                val statusText = if (isAnswerCorrect) "• CORRECT (+1 PT)" else "• INCORRECT (0 PTS)"

                MianRuleCard(
                    ruleColor = statusColor,
                    containerColor = colors.surface2
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Start
                            )
                        }

                        if (!question.rationale.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "OFFICIAL MARINA RATIONALE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.muted,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = question.rationale,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.foreground,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // ==========================================
        // 3. EDITORIAL BOTTOM CONTROL BAR
        // ==========================================
        Surface(
            color = colors.surface,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 3dp Top Rule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colors.border)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentIndex > 0) {
                        MianRuleButton(
                            onClick = { viewModel.previousQuestion() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous",
                                tint = colors.foreground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PREVIOUS",
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    val isLastQuestion = currentIndex == activeQuestions.size - 1

                    if (isLastQuestion || isMockExam) {
                        MianButton(
                            onClick = {
                                if (isLastQuestion) {
                                    viewModel.openSubmitDialog()
                                } else {
                                    viewModel.nextQuestion()
                                }
                            },
                            accent = isLastQuestion,
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(
                                text = if (isLastQuestion) "SUBMIT EXAM" else "NEXT",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        MianButton(
                            onClick = { viewModel.nextQuestion() },
                            accent = true,
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(
                                text = "NEXT",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 4. DIALOGS & OVERLAYS
    // ==========================================

    // Question Palette Modal
    if (isPaletteOpen) {
        QuestionPaletteDialog(
            totalQuestions = activeQuestions.size,
            currentIndex = currentIndex,
            userStates = userStates,
            bookmarkedIds = bookmarkedIds,
            questionIds = activeQuestions.map { it.id },
            onQuestionClick = { index -> viewModel.goToQuestion(index) },
            onDismiss = { viewModel.closeQuestionPalette() }
        )
    }

    // Submit Exam Modal
    if (showSubmitDialog) {
        val answeredCount = userStates.values.count { it.selectedIndex != null }
        val unansweredCount = activeQuestions.size - answeredCount

        AlertDialog(
            onDismissRequest = { viewModel.closeSubmitDialog() },
            shape = RoundedCornerShape(2.dp),
            containerColor = colors.surface,
            title = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SUBMIT EXAMINATION",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you ready to submit your exam for MARINA scoring?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.foreground
                    )
                    Text(
                        text = "• Answered: $answeredCount items\n• Unanswered: $unansweredCount items",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unansweredCount > 0) colors.accent else colors.muted
                    )
                }
            },
            confirmButton = {
                MianButton(
                    onClick = {
                        viewModel.closeSubmitDialog()
                        viewModel.submitExam()
                    },
                    accent = true
                ) {
                    Text("CONFIRM SUBMIT", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.closeSubmitDialog() },
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("CONTINUE EXAM", color = colors.muted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Mock Exam Lock-in Modal
    if (showLockInDialog) {
        AlertDialog(
            onDismissRequest = { showLockInDialog = false },
            shape = RoundedCornerShape(2.dp),
            containerColor = colors.surface,
            title = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "MOCK EXAM SESSION LOCKED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground
                    )
                }
            },
            text = {
                Text(
                    text = "Official timed mock examination is active. Leaving the screen will submit your current answers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                MianButton(
                    onClick = { showLockInDialog = false }
                ) {
                    Text("RESUME", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLockInDialog = false
                        viewModel.submitExam()
                    },
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("SUBMIT & EXIT", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Exit Confirmation Modal
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            shape = RoundedCornerShape(2.dp),
            containerColor = colors.surface,
            title = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "EXIT SESSION?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground
                    )
                }
            },
            text = {
                Text(
                    text = "Your current progress will be saved to your active session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                MianButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.exitQuiz()
                    }
                ) {
                    Text("EXIT", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false },
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("CANCEL", color = colors.muted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
