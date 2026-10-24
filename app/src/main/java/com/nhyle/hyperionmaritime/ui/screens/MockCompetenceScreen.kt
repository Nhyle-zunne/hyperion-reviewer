package com.nhyle.hyperionmaritime.ui.screens
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.QuestionSettingsMenu
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.ui.components.TimerChip
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.ui.theme.MaritimeGoldBright
import com.nhyle.hyperionmaritime.ui.theme.PassGreen
import com.nhyle.hyperionmaritime.ui.theme.PureWhite
import com.nhyle.hyperionmaritime.ui.theme.SeaCyan
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

@Composable
fun MockCompetenceScreen(
    viewModel: ReviewerViewModel,
    competenceId: String,
    modifier: Modifier = Modifier
) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val timeRemaining by viewModel.timeRemainingSeconds.collectAsState()
    val userStates by viewModel.userQuestionStates.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
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
        viewModel.navigateBack()
    }

    val compInfo = MaritimeCompetenceRegistry.getCompetenceById(competenceId, selectedTrack)
    val compQuestions = viewModel.getQuestionsForCompetence(competenceId)

    val answeredCount = compQuestions.count { userStates[it.id]?.selectedIndex != null }
    val flaggedCount = compQuestions.count { userStates[it.id]?.isFlagged == true }
    val totalQuestions = compQuestions.size
    val progress = if (totalQuestions > 0) answeredCount.toFloat() / totalQuestions.toFloat() else 0f

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val colors = LocalSpaghettiColors.current
    val headerBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFF0F172A)
    val headerTitleColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFFFFFFFF)
    val headerSubColor = if (isDark) MaritimeGoldBright else Color(0xFF38BDF8)
    val headerIconColor = if (isDark) MaritimeGoldBright else Color(0xFFFFFFFF)
    val headerBorder = if (isDark) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline) else BorderStroke(1.dp, Color(0xFF1E293B))

    val currentDensity = LocalDensity.current
    val textScale by viewModel.questionTextScale.collectAsState()
    val scaledDensity = remember(currentDensity, textScale) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * textScale
        )
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
        // Compact Exam Header
        val minutes = timeRemaining / 60
        val seconds = timeRemaining % 60
        val formattedRemainingTime = String.format("%02d:%02d", minutes, seconds)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: Security / Exam designation badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Session Secured",
                    tint = colors.muted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "SIMULATED EXAM",
                    color = colors.muted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Right side: Compact countdown readout + Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.error),
                    shape = RectangleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = colors.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formattedRemainingTime,
                            color = colors.error,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                QuestionSettingsMenu(viewModel = viewModel)
            }
        }

        HorizontalDivider(color = colors.border, thickness = 1.dp)

        // Continuous Vertical Scroll List: All items on one continuous page with safety fallback
        if (compQuestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "INITIALIZING EXAM MATRIX...",
                    color = Color(0xFF888888),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
        } else {
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
                        shape = RectangleShape,
                        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF),
                        shadowElevation = 0.dp,
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
                    val selectedIndex = state?.selectedIndex

                    MockQuestionItemCard(
                        itemNumber = index + 1,
                        totalItems = totalQuestions,
                        question = question,
                        selectedIndex = selectedIndex,
                        isFlagged = false,
                        isBookmarked = bookmarkedIds.contains(question.id),
                        isExamSubmitted = isExamSubmitted,
                        onSelectOption = { optIdx ->
                            if (!isExamSubmitted) {
                                viewModel.selectOptionForQuestion(question.id, optIdx)
                            }
                        },
                        onToggleBookmark = {
                            viewModel.toggleBookmark(question.id)
                        }
                    )
                }

                // End of list spacer
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Bottom Navigation Bar with Stacked Column Footer Layout
        val allAnswered = answeredCount >= totalQuestions && totalQuestions > 0

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(BorderStroke(1.dp, colors.border))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Full-width status readout:
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$answeredCount of $totalQuestions Answered",
                    color = colors.foreground,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${maxOf(0, totalQuestions - answeredCount)} items remaining",
                    color = colors.muted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons:
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isExamSubmitted || isCompetenceFinished) {
                    Button(
                        onClick = { viewModel.navigateTo(Screen.MockHub) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.primaryForeground
                        ),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = "Return to Hub",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { showEarlySubmitConfirmationDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.error
                        ),
                        border = BorderStroke(1.dp, colors.error),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = "Finish & Submit",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            if (allAnswered) {
                                showSubmitConfirmationDialog = true
                            }
                        },
                        enabled = allAnswered,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.primaryForeground,
                            disabledContainerColor = colors.surface2,
                            disabledContentColor = colors.muted
                        ),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, if (allAnswered) colors.primary else colors.border)
                    ) {
                        Text(
                            text = "✓ Submit Competence",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
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
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Competence Locked",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
            },
            text = {
                Text(
                    text = "You must complete all items in this competence before returning to the exam hub. Exiting mid-competence is not permitted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCompetenceLockedDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.primaryForeground
                    ),
                    shape = RectangleShape
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
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Submit Competence?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
            },
            text = {
                Text(
                    text = "All $totalQuestions items in this competence have been answered. Once submitted, this competence will be marked as Completed and locked from retaking in this session. Return to exam hub?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmationDialog = false
                        viewModel.markCompetenceCompleted(competenceId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.primaryForeground
                    ),
                    shape = RectangleShape
                ) {
                    Text("Submit & Return to Hub", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSubmitConfirmationDialog = false },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("Review Answers", color = colors.foreground)
                }
            }
        )
    }

    // Finish & Submit Early Confirmation Dialog (With unanswered items)
    if (showEarlySubmitConfirmationDialog) {
        val remaining = (totalQuestions - answeredCount).coerceAtLeast(0)
        AlertDialog(
            onDismissRequest = { showEarlySubmitConfirmationDialog = false },
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Finish & Submit Competence Early?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
            },
            text = {
                Text(
                    text = "You have answered $answeredCount of $totalQuestions items ($remaining items remaining). Submitting now will finalize your answers and lock this competence from being retaken in this session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEarlySubmitConfirmationDialog = false
                        viewModel.markCompetenceCompleted(competenceId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.primaryForeground
                    ),
                    shape = RectangleShape
                ) {
                    Text("Submit Anyway", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEarlySubmitConfirmationDialog = false },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("Cancel", color = colors.foreground)
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
    isBookmarked: Boolean,
    isExamSubmitted: Boolean,
    onSelectOption: (Int) -> Unit,
    onToggleBookmark: () -> Unit
) {
    val isDark = LocalSpaghettiColors.current.isDark
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
            color = if (isFlagged) Color(0xFFD97706) else if (selectedIndex != null) Color(0xFF075985) else Color(0xFF64748B)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                    shape = RectangleShape,
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
                        shape = RectangleShape,
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
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else (if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B))
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
                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black
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
                        else -> BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF075985) else Color(0xFF64748B))
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
                        isSelected -> if (isDark) Color.White else Color.Black
                        isDark -> MaterialTheme.colorScheme.onSurface
                        else -> Color.Black
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (!isExamSubmitted) Modifier.clickable { onSelectOption(optIndex) }
                                else Modifier
                            ),
                        shape = RectangleShape,
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
                                .background(letterBg, RectangleShape),
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
                                    unselectedColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFF64748B)
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
                    shape = RectangleShape,
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
