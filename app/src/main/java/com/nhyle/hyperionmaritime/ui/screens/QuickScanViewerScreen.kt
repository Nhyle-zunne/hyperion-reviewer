package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.model.shuffleOptionsStable
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.ui.components.AreYouStillThereDialog
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.SessionTimerBox
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow

/**
 * Screen 2: Quick Scan Question Viewer
 * Dedicated, read-only sequential study viewer where correct answers are automatically
 * highlighted in green on load without tapping or guessing, and without any rationale block.
 */
@Composable
fun QuickScanViewerScreen(
    viewModel: ReviewerViewModel,
    competenceId: String,
    track: Track? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val currentSelectedTrack by viewModel.selectedTrack.collectAsState()
    val activeTrack = track ?: currentSelectedTrack

    // Pure sequential order without shuffling
    val questions = remember(competenceId, activeTrack) {
        MaritimeQuestionBank.getQuestionsByCompetence(competenceId, activeTrack, isProUnlocked = true)
    }

    val competenceInfo = remember(competenceId, activeTrack) {
        val allComps = MaritimeCompetenceRegistry.getCompetencesForTrack(activeTrack)
        allComps.find { it.competenceId == competenceId }
    }

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpInputText by remember { mutableStateOf("") }
    var jumpInputError by remember { mutableStateOf<String?>(null) }

    val totalQuestions = questions.size
    val rawCurrentQuestion = questions.getOrNull(currentIndex)
    val currentQuestion = remember(rawCurrentQuestion?.id) {
        rawCurrentQuestion?.let { shuffleOptionsStable(it, "quickscan") }
    }

    val savedIndex = remember(competenceId, activeTrack) {
        viewModel.getQuickScanLastIndex(activeTrack, competenceId)
    }
    var showResumePrompt by rememberSaveable { mutableStateOf(savedIndex > 0 && savedIndex < totalQuestions) }

    LaunchedEffect(currentIndex) {
        if (questions.isNotEmpty() && currentIndex in questions.indices) {
            viewModel.saveQuickScanProgress(activeTrack, competenceId, currentIndex)
        }
    }

    val showIdleDialog by viewModel.studyTimeTracker.showIdleDialog.collectAsState()

    LaunchedEffect(competenceId, activeTrack) {
        viewModel.startQuickScanStudyTracking(competenceId, activeTrack)
    }

    LaunchedEffect(currentIndex) {
        if (questions.isNotEmpty() && currentIndex in questions.indices) {
            viewModel.saveQuickScanProgress(activeTrack, competenceId, currentIndex)
        }
    }

    val interactionModifier = modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent()
                viewModel.studyTimeTracker.recordUserInteraction()
            }
        }
    }

    BackHandler {
        viewModel.pauseQuickScanStudyTracking()
        viewModel.navigateBack()
    }

    Column(
        modifier = interactionModifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 1. Header with back button returning to competence list
        MaritimeTopBar(
            title = "QUICK SCAN • ${competenceId.uppercase()}",
            subtitle = competenceInfo?.title?.uppercase() ?: "COMPETENCE REFERENCE REVIEW",
            showBackButton = true,
            onBackClick = {
                viewModel.pauseQuickScanStudyTracking()
                viewModel.navigateBack()
            }
        )

        if (questions.isNotEmpty() && currentQuestion != null) {
            // HUD Sub-Bar Row (Directly underneath top bar divider line, matching Standard Drill)
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
                            text = "ITEM ${currentIndex + 1}/$totalQuestions · $competenceId".uppercase(),
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
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        if (showIdleDialog) {
            AreYouStillThereDialog(
                onStillHere = { viewModel.studyTimeTracker.onUserConfirmedStillHere() },
                onSaveAndExit = {
                    viewModel.pauseQuickScanStudyTracking()
                    viewModel.navigateBack()
                }
            )
        }

        if (questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No questions found for competence $competenceId.",
                    color = colors.muted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else if (currentQuestion != null) {
            // 2. Question & Options Content (Scrollable)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Question Card
                item {
                    MianRuleCard(
                        ruleColor = colors.accent,
                        containerColor = colors.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Question metadata chip
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
                                        text = "QUESTION ${currentIndex + 1} OF $totalQuestions",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.accent,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Surface(
                                    shape = RectangleShape,
                                    color = colors.surface2,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Text(
                                        text = currentQuestion.difficulty.name,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.muted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Question Stem Text
                            Text(
                                text = currentQuestion.questionText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.foreground,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                // Options List (A, B, C, D)
                items(currentQuestion.options.size) { optIndex ->
                    val optionText = currentQuestion.options[optIndex]
                    val isCorrect = optIndex == currentQuestion.correctAnswerIndex
                    val optionLetter = when (optIndex) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        3 -> "D"
                        else -> "${optIndex + 1}"
                    }

                    // Highlight letter of correct answer on left side and green highlight around the box
                    val containerColor = colors.surface
                    val borderColor = if (isCorrect) Color(0xFF22C55E) else colors.border
                    val textColor = colors.foreground
                    val letterBadgeBg = if (isCorrect) Color(0xFF22C55E) else colors.surface2
                    val letterBadgeBorder = if (isCorrect) Color(0xFF16A34A) else colors.border
                    val letterColor = if (isCorrect) Color.White else colors.muted

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape,
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        border = BorderStroke(if (isCorrect) 1.5.dp else 1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Option Letter Box (highlighted on the left side for correct answer)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RectangleShape)
                                    .background(letterBadgeBg)
                                    .border(1.dp, letterBadgeBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = optionLetter,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = letterColor
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Option Text
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    lineHeight = 20.sp,
                                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                )

                                if (isCorrect) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Correct Answer",
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "CORRECT ANSWER",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF22C55E),
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Bottom Control Bar (Fixed)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button (Left)
                    Button(
                        onClick = {
                            if (currentIndex > 0) currentIndex--
                        },
                        enabled = currentIndex > 0,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surface2,
                            contentColor = colors.foreground,
                            disabledContainerColor = colors.surface2.copy(alpha = 0.4f),
                            disabledContentColor = colors.muted.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, colors.border),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PREV",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Jump to Question Card (Center)
                    Surface(
                        shape = RectangleShape,
                        color = colors.surface2,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .clip(RectangleShape)
                            .clickable {
                                jumpInputText = "${currentIndex + 1}"
                                jumpInputError = null
                                showJumpDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Jump",
                                tint = colors.accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Question ${currentIndex + 1} of $totalQuestions",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Next Button (Right)
                    Button(
                        onClick = {
                            if (currentIndex < totalQuestions - 1) currentIndex++
                        },
                        enabled = currentIndex < totalQuestions - 1,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.primaryForeground,
                            disabledContainerColor = colors.surface2.copy(alpha = 0.4f),
                            disabledContentColor = colors.muted.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, colors.border),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NEXT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
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

    // Jump to Question Dialog
    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            containerColor = colors.surface,
            shape = RectangleShape,
            title = {
                Text(
                    text = "JUMP TO QUESTION",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a question number between 1 and $totalQuestions:",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )

                    OutlinedTextField(
                        value = jumpInputText,
                        onValueChange = { input ->
                            jumpInputText = input.filter { it.isDigit() }
                            jumpInputError = null
                        },
                        shape = RectangleShape,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val target = jumpInputText.toIntOrNull()
                                if (target != null && target in 1..totalQuestions) {
                                    currentIndex = target - 1
                                    showJumpDialog = false
                                } else {
                                    jumpInputError = "Enter 1 to $totalQuestions"
                                }
                            }
                        ),
                        isError = jumpInputError != null,
                        supportingText = {
                            if (jumpInputError != null) {
                                Text(text = jumpInputError!!, color = colors.accent, fontSize = 11.sp)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.foreground,
                            unfocusedTextColor = colors.foreground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = jumpInputText.toIntOrNull()
                        if (target != null && target in 1..totalQuestions) {
                            currentIndex = target - 1
                            showJumpDialog = false
                        } else {
                            jumpInputError = "Enter 1 to $totalQuestions"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.primaryForeground
                    ),
                    shape = RectangleShape
                ) {
                    Text("JUMP", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showJumpDialog = false },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("CANCEL", color = colors.foreground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showResumePrompt && savedIndex > 0 && savedIndex < totalQuestions) {
        AlertDialog(
            onDismissRequest = { showResumePrompt = false },
            containerColor = colors.surface,
            shape = RectangleShape,
            title = {
                Text(
                    text = "RESUME PREVIOUS READING?",
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = (-0.2).sp
                )
            },
            text = {
                Text(
                    text = "You previously left off at Question #${savedIndex + 1} of $totalQuestions in ${competenceId.uppercase()}.\n\nWould you like to go back to where you previously left off?",
                    color = colors.muted,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentIndex = savedIndex
                        showResumePrompt = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.accentForeground
                    ),
                    shape = RectangleShape
                ) {
                    Text("YES, GO TO Q #${savedIndex + 1}", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        currentIndex = 0
                        showResumePrompt = false
                    },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("NO, START AT Q #1", color = colors.foreground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        )
    }
}
