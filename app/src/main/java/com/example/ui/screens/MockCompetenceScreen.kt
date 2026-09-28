package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Question
import com.example.data.repository.MaritimeCompetenceRegistry
import com.example.ui.components.TimerChip
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun MockCompetenceScreen(
    viewModel: ReviewerViewModel,
    competenceId: String,
    modifier: Modifier = Modifier
) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val timeRemaining by viewModel.timeRemainingSeconds.collectAsState()
    val userStates by viewModel.userQuestionStates.collectAsState()
    val isExamSubmitted by viewModel.isExamSubmitted.collectAsState()
    val completedCompetences by viewModel.completedCompetences.collectAsState()

    val isCompetenceFinished = (competenceId in completedCompetences) || isExamSubmitted

    var showCompetenceLockedDialog by remember { mutableStateOf(false) }
    var showSubmitConfirmationDialog by remember { mutableStateOf(false) }
    var showEarlySubmitConfirmationDialog by remember { mutableStateOf(false) }

    // Intercept system back press when competence is active: strictly cannot exit mid-competence!
    BackHandler(enabled = !isCompetenceFinished) {
        showCompetenceLockedDialog = true
    }

    // When competence is finished/submitted, navigating back returns to MockHub
    BackHandler(enabled = isCompetenceFinished) {
        viewModel.navigateTo(Screen.MockHub)
    }

    val compInfo = MaritimeCompetenceRegistry.getCompetenceById(competenceId, selectedTrack)
    val compQuestions = viewModel.getQuestionsForCompetence(competenceId)

    val answeredCount = compQuestions.count { userStates[it.id]?.selectedIndex != null }
    val flaggedCount = compQuestions.count { userStates[it.id]?.isFlagged == true }
    val totalQuestions = compQuestions.size
    val progress = if (totalQuestions > 0) answeredCount.toFloat() / totalQuestions.toFloat() else 0f

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val headerBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFF0F172A)
    val headerTitleColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFFFFFFFF)
    val headerSubColor = if (isDark) MaritimeGoldBright else Color(0xFF38BDF8)
    val headerIconColor = if (isDark) MaritimeGoldBright else Color(0xFFFFFFFF)
    val headerBorder = if (isDark) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline) else BorderStroke(1.dp, Color(0xFF1E293B))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF1F5F9))
    ) {
        // Top Bar: Back to Hub + Title + Timer
        Surface(
            color = headerBg,
            border = headerBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompetenceFinished) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.MockHub) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = headerIconColor
                        )
                    }
                } else {
                    IconButton(
                        onClick = { showCompetenceLockedDialog = true },
                        enabled = false
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Competence Locked",
                            tint = if (isDark) headerIconColor.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${compInfo?.code ?: competenceId}: ${compInfo?.title ?: "Competence Drills"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = headerTitleColor,
                        maxLines = 1
                    )
                    Text(
                        text = if (isExamSubmitted) {
                            "$answeredCount / $totalQuestions Answered • Review Mode"
                        } else {
                            "$answeredCount / $totalQuestions Answered" + if (flaggedCount > 0) " • $flaggedCount Flagged" else ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = headerSubColor,
                        fontWeight = if (!isDark) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TimerChip(secondsRemaining = timeRemaining)
            }
        }

        // Progress bar for this competence
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaritimeGoldBright,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Continuous Vertical Scroll List: All items on one continuous page
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF),
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continuous Scroll View: Answer, flag, and review all $totalQuestions questions freely. When finished, tap 'Submit Competence' below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569)
                        )
                    }
                }
            }

            itemsIndexed(compQuestions, key = { _, q -> q.id }) { index, question ->
                val state = userStates[question.id]
                val isFlagged = state?.isFlagged == true
                val selectedIndex = state?.selectedIndex

                MockQuestionItemCard(
                    itemNumber = index + 1,
                    totalItems = totalQuestions,
                    question = question,
                    selectedIndex = selectedIndex,
                    isFlagged = isFlagged,
                    isExamSubmitted = isExamSubmitted,
                    onSelectOption = { optIdx ->
                        if (!isExamSubmitted) {
                            viewModel.selectOptionForQuestion(question.id, optIdx)
                        }
                    },
                    onToggleFlag = {
                        if (!isExamSubmitted) {
                            viewModel.toggleFlagForQuestion(question.id)
                        }
                    }
                )
            }

            // End of list spacer
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Bottom Navigation Bar with Bottom-Right Competence Action: "Submit Competence"
        val bottomBarBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        val bottomBarBorder = if (isDark) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline) else BorderStroke(1.dp, Color(0xFFCBD5E1))
        val submitBtnBg = if (isDark) PassGreen else Color(0xFF0284C7)
        val submitTextColor = if (isDark) Color.Black else PureWhite
        val allAnswered = answeredCount >= totalQuestions && totalQuestions > 0

        Surface(
            color = bottomBarBg,
            border = bottomBarBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "$answeredCount of $totalQuestions Answered",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (answeredCount == totalQuestions) PassGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (answeredCount == totalQuestions) "All questions answered" else "${totalQuestions - answeredCount} items remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (isExamSubmitted || isCompetenceFinished) {
                    Button(
                        onClick = { viewModel.navigateTo(Screen.MockHub) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7),
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Return to Hub",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!allAnswered) {
                            OutlinedButton(
                                onClick = { showEarlySubmitConfirmationDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Finish & Submit",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (allAnswered) {
                                    showSubmitConfirmationDialog = true
                                }
                            },
                            enabled = allAnswered,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) PassGreen else Color(0xFF16A34A),
                                contentColor = if (isDark) Color.Black else PureWhite,
                                disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE2E8F0),
                                disabledContentColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                tint = if (allAnswered) (if (isDark) Color.Black else PureWhite) else (if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF94A3B8)),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Submit Competence",
                                color = if (allAnswered) (if (isDark) Color.Black else PureWhite) else (if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF94A3B8)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Non-dismissible Competence Locked Dialog
    if (showCompetenceLockedDialog) {
        AlertDialog(
            onDismissRequest = { /* Non-dismissible */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {
                Text(
                    text = "Competence Locked",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You must complete all items in this competence before returning to the exam hub. Exiting mid-competence is not permitted.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCompetenceLockedDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7),
                        contentColor = PureWhite
                    )
                ) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Submit Competence Confirmation Dialog (When all items are answered)
    if (showSubmitConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirmationDialog = false },
            title = {
                Text(
                    text = "Submit Competence?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "All $totalQuestions items in this competence have been answered. Once submitted, this competence will be marked as Completed and locked from retaking in this session. Return to exam hub?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmationDialog = false
                        viewModel.markCompetenceCompleted(competenceId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) PassGreen else Color(0xFF16A34A),
                        contentColor = if (isDark) Color.Black else PureWhite
                    )
                ) {
                    Text("Submit & Return to Hub", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSubmitConfirmationDialog = false }) {
                    Text("Review Answers")
                }
            }
        )
    }

    // Finish & Submit Early Confirmation Dialog (With unanswered items)
    if (showEarlySubmitConfirmationDialog) {
        val remaining = (totalQuestions - answeredCount).coerceAtLeast(0)
        AlertDialog(
            onDismissRequest = { showEarlySubmitConfirmationDialog = false },
            title = {
                Text(
                    text = "Finish & Submit Competence Early?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You have answered $answeredCount of $totalQuestions items ($remaining items remaining). Submitting now will finalize your answers and lock this competence from being retaken in this session.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEarlySubmitConfirmationDialog = false
                        viewModel.markCompetenceCompleted(competenceId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7),
                        contentColor = PureWhite
                    )
                ) {
                    Text("Submit Anyway", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEarlySubmitConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Question Card for Mock Exam:
 * - Displays item number (e.g. "#12")
 * - Question text
 * - Radio options (A, B, C, D)
 * - Flag icon
 * - STRICT RULE: Absolutely NO point values (+3, +6, +9 pts) or difficulty chips shown!
 */
@Composable
private fun MockQuestionItemCard(
    itemNumber: Int,
    totalItems: Int,
    question: Question,
    selectedIndex: Int?,
    isFlagged: Boolean,
    isExamSubmitted: Boolean,
    onSelectOption: (Int) -> Unit,
    onToggleFlag: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFFFFF)
    val isCorrect = selectedIndex == question.correctAnswerIndex
    val cardBorder = if (isDark) {
        BorderStroke(
            width = if (isFlagged) 1.5.dp else if (selectedIndex != null) 1.dp else 0.75.dp,
            color = if (isFlagged) MaritimeGoldBright else if (selectedIndex != null) SeaCyan.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
        )
    } else {
        BorderStroke(
            width = if (isFlagged) 1.5.dp else 1.dp,
            color = if (isFlagged) Color(0xFFD97706) else if (selectedIndex != null) Color(0xFF0284C7) else Color(0xFFCBD5E1)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Item Number badge and status / Flag Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val numBadgeBg = if (isDark) {
                    if (selectedIndex != null) SeaCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color(0xFFE0F2FE)
                }
                val numBadgeColor = if (isDark) {
                    if (selectedIndex != null) SeaCyan else MaterialTheme.colorScheme.onSurface
                } else {
                    Color(0xFF0369A1)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = numBadgeBg,
                    border = BorderStroke(1.dp, if (selectedIndex != null) (if (isDark) SeaCyan else Color(0xFF0284C7)) else (if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFBAE6FD)))
                ) {
                    Text(
                        text = "#$itemNumber of $totalItems",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = numBadgeColor
                    )
                }

                if (isExamSubmitted) {
                    val statusText = when {
                        selectedIndex == null -> "UNANSWERED"
                        isCorrect -> "CORRECT"
                        else -> "INCORRECT"
                    }
                    val statusBg = when {
                        selectedIndex == null -> Color(0xFF64748B).copy(alpha = 0.15f)
                        isCorrect -> PassGreen.copy(alpha = 0.2f)
                        else -> Color(0xFFDC2626).copy(alpha = 0.2f)
                    }
                    val statusColor = when {
                        selectedIndex == null -> Color(0xFF64748B)
                        isCorrect -> if (isDark) PassGreen else Color(0xFF15803D)
                        else -> if (isDark) Color(0xFFFF6B6B) else Color(0xFFDC2626)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                } else {
                    IconButton(
                        onClick = onToggleFlag,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFlagged) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                            contentDescription = "Flag Item",
                            tint = if (isFlagged) MaritimeGoldBright else (if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Question Text
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = if (isDark) FontWeight.Medium else FontWeight.SemiBold,
                    lineHeight = 22.sp
                ),
                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Radio Options (A, B, C, D)
            val optionLetters = listOf("A", "B", "C", "D")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = selectedIndex == optIndex
                    val isThisCorrect = optIndex == question.correctAnswerIndex
                    val letter = optionLetters.getOrElse(optIndex) { "${optIndex + 1}" }

                    val optBg = when {
                        isExamSubmitted && isThisCorrect -> if (isDark) PassGreen.copy(alpha = 0.2f) else Color(0xFFDCFCE7)
                        isExamSubmitted && isSelected && !isThisCorrect -> if (isDark) Color(0xFFDC2626).copy(alpha = 0.2f) else Color(0xFFFEE2E2)
                        isDark -> if (isSelected) SeaCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else -> if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC)
                    }
                    val optBorder = when {
                        isExamSubmitted && isThisCorrect -> BorderStroke(1.5.dp, if (isDark) PassGreen else Color(0xFF16A34A))
                        isExamSubmitted && isSelected && !isThisCorrect -> BorderStroke(1.5.dp, Color(0xFFDC2626))
                        isDark -> BorderStroke(if (isSelected) 1.5.dp else 0.75.dp, if (isSelected) SeaCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        else -> BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0))
                    }
                    val letterBg = when {
                        isExamSubmitted && isThisCorrect -> if (isDark) PassGreen else Color(0xFF16A34A)
                        isExamSubmitted && isSelected && !isThisCorrect -> Color(0xFFDC2626)
                        isSelected -> if (isDark) SeaCyan else Color(0xFF0284C7)
                        isDark -> MaterialTheme.colorScheme.surfaceVariant
                        else -> Color(0xFFE2E8F0)
                    }
                    val letterTextColor = when {
                        isExamSubmitted && (isThisCorrect || isSelected) -> PureWhite
                        isSelected -> if (isDark) Color.Black else PureWhite
                        isDark -> MaterialTheme.colorScheme.onSurface
                        else -> Color(0xFF1E293B)
                    }
                    val optionTextColor = when {
                        isExamSubmitted && isThisCorrect -> if (isDark) PassGreen else Color(0xFF15803D)
                        isExamSubmitted && isSelected && !isThisCorrect -> if (isDark) Color(0xFFFF6B6B) else Color(0xFFDC2626)
                        isSelected -> if (isDark) Color.White else Color(0xFF0C4A6E)
                        isDark -> MaterialTheme.colorScheme.onSurface
                        else -> Color(0xFF1E293B)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (!isExamSubmitted) Modifier.clickable { onSelectOption(optIndex) }
                                else Modifier
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = optBg,
                        border = optBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(letterBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = letterTextColor
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected && !isDark) FontWeight.Bold else FontWeight.Normal,
                                color = optionTextColor,
                                modifier = Modifier.weight(1f),
                                lineHeight = 18.sp
                            )

                            RadioButton(
                                selected = isSelected,
                                onClick = if (!isExamSubmitted) { { onSelectOption(optIndex) } } else null,
                                enabled = !isExamSubmitted,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (isDark) SeaCyan else Color(0xFF0284C7),
                                    unselectedColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }

            // If submitted: Display Competency Standard Reference & Answer Rationale
            if (isExamSubmitted) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "OFFICIAL ANSWER RATIONALE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) MaritimeGoldBright else Color(0xFF0369A1),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = question.explanation.ifBlank {
                                val correctLetter = optionLetters.getOrElse(question.correctAnswerIndex) { "A" }
                                val correctText = question.options.getOrNull(question.correctAnswerIndex) ?: ""
                                "The correct answer is Option $correctLetter: \"$correctText\" according to official MARINA STCW examination standards."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1E293B),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
