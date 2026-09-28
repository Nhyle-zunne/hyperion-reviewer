package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Warning
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
    val isExamSubmitted by viewModel.isExamSubmitted.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }
    var showLockInDialog by remember { mutableStateOf(false) }

    // Intercept back button to enforce lock-in for Mock Exams
    BackHandler {
        if (quizMode == QuizMode.MOCK_EXAM && !isExamSubmitted) {
            showLockInDialog = true
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

    val isDark = isSystemInDarkTheme()
    val headerBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFF0F172A)
    val headerTitleColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFFFFFFFF)
    val headerSubColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF38BDF8)
    val headerIconColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFFFFFFF)
    val headerBorder = if (isDark) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline) else BorderStroke(1.dp, Color(0xFF1E293B))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF1F5F9))
    ) {
        // Quiz Top Bar
        Surface(
            color = headerBg,
            border = headerBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isMockExam && !isExamSubmitted) {
                        IconButton(
                            onClick = { showLockInDialog = true },
                            enabled = false
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Exam Locked",
                                tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.85f)
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            viewModel.navigateTo(Screen.Home)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Quiz",
                                tint = headerIconColor
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Question ${currentIndex + 1} of ${activeQuestions.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = headerTitleColor
                        )
                        Text(
                            text = "${question.functionId} • ${question.competenceId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = headerSubColor,
                            fontWeight = if (!isDark) FontWeight.SemiBold else FontWeight.Normal
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
                            tint = if (isFlagged) (if (isDark) MaterialTheme.colorScheme.primary else Color(0xFFFBBF24)) else headerIconColor
                        )
                    }

                    // Bookmark star
                    IconButton(onClick = { viewModel.toggleBookmark(question.id) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) (if (isDark) MaterialTheme.colorScheme.primary else Color(0xFFFBBF24)) else headerIconColor
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
            color = if (isDark) MaterialTheme.colorScheme.primary else MaritimeGoldBright,
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
                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFBAE6FD))
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
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0369A1)
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
                color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE0F2FE),
                border = if (isDark) null else BorderStroke(1.dp, Color(0xFFBAE6FD))
            ) {
                Text(
                    text = "${question.competenceId}: ${question.topic}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) SeaCyan else Color(0xFF0369A1),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Question Text Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFFFFF)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFCBD5E1))
            ) {
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = if (isDark) FontWeight.Medium else FontWeight.SemiBold,
                        lineHeight = 22.sp
                    ),
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A),
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

            // Practice Mode Immediate Rationale Card
            AnimatedVisibility(
                visible = isSubmitted,
                enter = fadeIn() + expandVertically()
            ) {
                val isAnswerCorrect = currentState?.selectedIndex == question.correctAnswerIndex
                val statusColor = if (isAnswerCorrect) PassGreen else FailRed
                val statusText = if (isAnswerCorrect) "• CORRECT (+1 pt)" else "• INCORRECT (0 pts)"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                    border = BorderStroke(1.dp, if (isDark) statusColor.copy(alpha = 0.6f) else statusColor.copy(alpha = 0.4f))
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

                        if (!isAnswerCorrect && !isMockExam) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Added to Mistakes Notebook. Review correctly once to clear.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Bottom Navigation Bar
        Surface(
            color = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFFFFF),
            border = if (isDark) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline) else BorderStroke(1.dp, Color(0xFFCBD5E1))
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
                    Text("Prev")
                }

                // Question Palette Grid button
                IconButton(onClick = { viewModel.setPaletteOpen(true) }) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Question Palette",
                        tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7)
                    )
                }

                if (isMockExam) {
                    Button(
                        onClick = { viewModel.setShowSubmitDialog(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7),
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Submit Exam", color = PureWhite, fontWeight = FontWeight.Bold)
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7),
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (currentIndex < activeQuestions.size - 1) "Next" else "Finish",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = PureWhite,
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Yes, Submit", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowSubmitDialog(false) }) {
                    Text("Continue Exam")
                }
            }
        )
    }

    // Strict Mock Examination Lock-In Dialog
    if (showLockInDialog) {
        AlertDialog(
            onDismissRequest = { /* Non-dismissible by tapping outside */ },
            title = {
                Text(
                    text = "Mock Examination In Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Official mock examinations cannot be exited until all competencies are completed and submitted. If you wish to finish now, you must submit your overall exam.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLockInDialog = false
                        viewModel.submitExam()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Submit Exam Now", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLockInDialog = false }) {
                    Text("Continue Exam")
                }
            }
        )
    }

    // Exit Exam Dialog (for practice mode)
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Save & Exit", color = PureWhite, fontWeight = FontWeight.Bold)
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

private fun getFormattedExplanation(question: com.example.data.model.Question): String {
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
