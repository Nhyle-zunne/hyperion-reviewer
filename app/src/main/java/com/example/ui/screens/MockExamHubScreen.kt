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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.CompetenceInfo
import com.example.data.model.Track
import com.example.data.repository.MaritimeCompetenceRegistry
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.TimerChip
import com.example.ui.theme.FailRed
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

enum class CompetenceStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

@Composable
fun MockExamHubScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val timeRemaining by viewModel.timeRemainingSeconds.collectAsState()
    val activeQuestions by viewModel.activeQuestions.collectAsState()
    val userStates by viewModel.userQuestionStates.collectAsState()
    val completedCompetences by viewModel.completedCompetences.collectAsState()

    var showSubmitDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Back navigation returns to Dashboard
    BackHandler {
        viewModel.navigateTo(Screen.Home)
    }

    val competences = MaritimeCompetenceRegistry.getCompetencesForTrack(selectedTrack)
    val groupedByFunction = competences.groupBy { it.functionId }

    val totalQuestions = activeQuestions.size
    val answeredCount = userStates.values.count { it.selectedIndex != null }
    val flaggedCount = userStates.values.count { it.isFlagged }
    val unansweredCount = (totalQuestions - answeredCount).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Master Top Bar with countdown timer & Back Button
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaritimeGoldBright
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedTrack == Track.DECK_OIC_NW) "OIC-NW Mock Examination" else "GMDSS GOC Mock Examination",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaritimeGoldBright,
                        maxLines = 1
                    )
                    Text(
                        text = "Master Competence Hub • $answeredCount / $totalQuestions Answered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Master Countdown Timer Chip
                TimerChip(secondsRemaining = timeRemaining)
            }
        }

        // Main List of Competence Cards by Function
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // Exam Banner Notice
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaritimeGoldBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Licensure Examination Protocol",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaritimeGoldBright
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select any competence to scroll and answer questions. You can revisit and revise answers anytime before submitting the overall exam.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Iterate through functions and competences
            groupedByFunction.forEach { (functionId, functionCompetences) ->
                item {
                    val functionTitle = MaritimeCompetenceRegistry.getFunctionTitle(functionId)
                    val functionItemsCount = functionCompetences.sumOf { it.officialTOSItems }

                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = functionTitle.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SeaCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$functionItemsCount Total Items in TOS Function",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(functionCompetences) { competence ->
                    val compQuestions = activeQuestions.filter { it.competenceId == competence.competenceId }
                    val compTotal = if (compQuestions.isNotEmpty()) compQuestions.size else competence.officialTOSItems
                    val compAnswered = compQuestions.count { userStates[it.id]?.selectedIndex != null }
                    val isExplicitlyCompleted = competence.competenceId in completedCompetences

                    val status = when {
                        isExplicitlyCompleted || (compTotal > 0 && compAnswered == compTotal) -> CompetenceStatus.COMPLETED
                        compAnswered > 0 -> CompetenceStatus.IN_PROGRESS
                        else -> CompetenceStatus.NOT_STARTED
                    }

                    MockCompetenceHubCard(
                        competence = competence,
                        itemCount = compTotal,
                        answeredCount = compAnswered,
                        status = status,
                        onClick = { viewModel.openMockCompetence(competence.competenceId) }
                    )
                }
            }
        }

        // Bottom-Right Sticky Action: "Submit Overall Exam"
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$answeredCount / $totalQuestions Answered",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (flaggedCount > 0) {
                        Text(
                            text = "$flaggedCount flagged for review",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaritimeGoldBright
                        )
                    } else if (unansweredCount > 0) {
                        Text(
                            text = "$unansweredCount items left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "All items answered",
                            style = MaterialTheme.typography.labelSmall,
                            color = PassGreen
                        )
                    }
                }

                Button(
                    onClick = { showSubmitDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaritimeGoldBright),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submit Overall Exam",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Submit Overall Exam Confirmation Dialog
    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = {
                Text(
                    text = "Submit Overall Exam?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You have answered $answeredCount out of $totalQuestions questions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (unansweredCount > 0) {
                        Text(
                            text = "⚠️ Warning: You have $unansweredCount unanswered questions. Unanswered questions will receive 0 points.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FailRed
                        )
                    }
                    Text(
                        text = "Once submitted, your official competence scores, Bloom's point weights, and overall pass/fail verdict will be calculated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        viewModel.submitExam()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaritimeGoldBright)
                ) {
                    Text("Confirm & Submit", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSubmitDialog = false }) {
                    Text("Review Exam")
                }
            }
        )
    }

    // Exit Exam Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text("Exit Mock Exam?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Your active session progress and remaining timer are saved automatically. You can resume this exam anytime from the Home dashboard.",
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
                    Text("Exit to Dashboard", color = PureWhite)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }) {
                    Text("Continue Exam")
                }
            }
        )
    }
}

@Composable
private fun MockCompetenceHubCard(
    competence: CompetenceInfo,
    itemCount: Int,
    answeredCount: Int,
    status: CompetenceStatus,
    onClick: () -> Unit
) {
    val statusText = when (status) {
        CompetenceStatus.NOT_STARTED -> "Not Started"
        CompetenceStatus.IN_PROGRESS -> "In Progress"
        CompetenceStatus.COMPLETED -> "Completed"
    }

    val statusColor = when (status) {
        CompetenceStatus.NOT_STARTED -> Color(0xFF9E9E9E)
        CompetenceStatus.IN_PROGRESS -> SeaCyan
        CompetenceStatus.COMPLETED -> PassGreen
    }

    val statusBg = when (status) {
        CompetenceStatus.NOT_STARTED -> Color(0xFF23252A)
        CompetenceStatus.IN_PROGRESS -> Color(0xFF0E2734)
        CompetenceStatus.COMPLETED -> Color(0xFF0F3222)
    }

    val borderStroke = when (status) {
        CompetenceStatus.COMPLETED -> BorderStroke(1.dp, PassGreen.copy(alpha = 0.5f))
        CompetenceStatus.IN_PROGRESS -> BorderStroke(1.dp, SeaCyan.copy(alpha = 0.5f))
        CompetenceStatus.NOT_STARTED -> BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Competence Code Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(statusBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = competence.code,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Center Content: Title and item count (STRICT RULE: NO SCORES OR PERCENTAGES)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${competence.code}: ${competence.title} — $itemCount Items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$answeredCount / $itemCount Answered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Status Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusBg,
                border = BorderStroke(0.75.dp, statusColor.copy(alpha = 0.6f))
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}
