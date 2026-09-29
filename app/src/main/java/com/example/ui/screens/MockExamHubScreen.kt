package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Lock
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
    val isExamSubmitted by viewModel.isExamSubmitted.collectAsState()

    var showSubmitDialog by remember { mutableStateOf(false) }
    var showLockInDialog by remember { mutableStateOf(false) }
    var pendingCompetenceId by remember { mutableStateOf<String?>(null) }
    var showLockedCompetenceDialog by remember { mutableStateOf(false) }

    // Intercept back navigation when exam is in progress: cannot exit without submitting
    BackHandler(enabled = !isExamSubmitted) {
        showLockInDialog = true
    }

    val competences = MaritimeCompetenceRegistry.getCompetencesForTrack(selectedTrack)
    val groupedByFunction = competences.groupBy { it.functionId }

    val totalQuestions = activeQuestions.size
    val answeredCount = userStates.values.count { it.selectedIndex != null }
    val flaggedCount = userStates.values.count { it.isFlagged }
    val unansweredCount = (totalQuestions - answeredCount).coerceAtLeast(0)

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val headerBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFF0F172A)
    val headerTitleColor = if (isDark) MaritimeGoldBright else Color(0xFFFFFFFF)
    val headerSubColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF38BDF8)
    val headerIconColor = if (isDark) MaritimeGoldBright else Color(0xFFFFFFFF)
    val headerBorder = if (isDark) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline) else BorderStroke(1.dp, Color(0xFF1E293B))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF1F5F9))
    ) {
        // Master Top Bar with countdown timer & Back Button
        Surface(
            color = headerBg,
            border = headerBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (!isExamSubmitted) {
                        showLockInDialog = true
                    } else {
                        viewModel.navigateTo(Screen.Home)
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = headerIconColor
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedTrack == Track.DECK_OIC_NW) "OIC-NW Mock Examination" else "GMDSS GOC Mock Examination",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = headerTitleColor,
                        maxLines = 1
                    )
                    Text(
                        text = if (isExamSubmitted) {
                            "Master Competence Hub • Examination Submitted (Review Mode)"
                        } else {
                            "Master Competence Hub • $answeredCount/$totalQuestions Answered • $unansweredCount items left"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = headerSubColor
                    )
                }
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
                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF),
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Licensure Examination Protocol",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0F172A)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select any competence to begin. Timers are competency-based (1 minute per item) and only count down while actively inside that specific competence module.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Iterate through functions and competences
            groupedByFunction.forEach { (functionId, functionCompetences) ->
                item {
                    val functionTitle = MaritimeCompetenceRegistry.getFunctionTitle(functionId)
                    val permitItems = MaritimeCompetenceRegistry.getPermitItemsForFunction(functionId, selectedTrack)
                    val compQuestionsForFunc = activeQuestions.filter { it.functionId == functionId }
                    val functionItemsCount = if (compQuestionsForFunc.isNotEmpty()) compQuestionsForFunc.size else permitItems

                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = functionTitle.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) SeaCyan else Color(0xFF0284C7),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$functionItemsCount Items • MARINA Official Permit",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)
                        )
                    }
                }

                items(functionCompetences) { competence ->
                    val permitCount = MaritimeCompetenceRegistry.getPermitItemCount(competence.competenceId, selectedTrack)
                    val compQuestions = activeQuestions.filter { it.competenceId == competence.competenceId }
                    val compTotal = if (compQuestions.isNotEmpty()) compQuestions.size else permitCount
                    val compAnswered = compQuestions.count { userStates[it.id]?.selectedIndex != null }
                    val isExplicitlyCompleted = competence.competenceId in completedCompetences

                    val status = when {
                        isExplicitlyCompleted -> CompetenceStatus.COMPLETED
                        compAnswered > 0 -> CompetenceStatus.IN_PROGRESS
                        else -> CompetenceStatus.NOT_STARTED
                    }

                    MockCompetenceHubCard(
                        competence = competence,
                        itemCount = compTotal,
                        answeredCount = compAnswered,
                        status = status,
                        isLocked = isExplicitlyCompleted && !isExamSubmitted,
                        onClick = {
                            if (isExamSubmitted) {
                                viewModel.openMockCompetence(competence.competenceId)
                            } else if (isExplicitlyCompleted) {
                                showLockedCompetenceDialog = true
                            } else {
                                pendingCompetenceId = competence.competenceId
                            }
                        }
                    )
                }
            }
        }

        // Bottom-Right Sticky Action: "Submit Overall Exam"
        val bottomBarBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        val bottomBarBorder = if (isDark) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline) else BorderStroke(1.dp, Color(0xFFCBD5E1))
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
                Column {
                    Text(
                        text = "$answeredCount / $totalQuestions Answered • $unansweredCount items left",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (flaggedCount > 0) {
                        Text(
                            text = "$flaggedCount flagged for review",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (unansweredCount > 0) {
                        Text(
                            text = "$unansweredCount items left to complete",
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

                val submitBtnBg = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7)
                Button(
                    onClick = {
                        if (isExamSubmitted) {
                            viewModel.navigateTo(Screen.Results)
                        } else {
                            showSubmitDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = submitBtnBg,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = if (isExamSubmitted) Icons.Default.CheckCircle else Icons.Default.Send,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExamSubmitted) "View Results" else "Submit Overall Exam",
                        color = PureWhite,
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Confirm & Submit", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSubmitDialog = false }) {
                    Text("Review Exam")
                }
            }
        )
    }

    // Strict Exam Lock-In Confirmation Dialog (Non-dismissible)
    if (showLockInDialog) {
        AlertDialog(
            onDismissRequest = { /* Non-dismissible by tapping outside */ },
            title = {
                Text(
                    text = "Mock Examination In Progress",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
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
                        containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7),
                        contentColor = PureWhite
                    )
                ) {
                    Text("Submit Exam Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLockInDialog = false }) {
                    Text("Continue Exam")
                }
            }
        )
    }

    // Entry Confirmation Dialog (Before Opening a Competence)
    if (pendingCompetenceId != null) {
        AlertDialog(
            onDismissRequest = { pendingCompetenceId = null },
            title = {
                Text(
                    text = "Start Competence Confirmation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Once you enter this competence, you CANNOT exit, switch, or return to the main hub until all questions in this competence have been answered and submitted. Are you ready to begin?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cid = pendingCompetenceId!!
                        pendingCompetenceId = null
                        viewModel.openMockCompetence(cid)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7),
                        contentColor = PureWhite
                    )
                ) {
                    Text("Begin Competence", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingCompetenceId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Locked Competence Warning Dialog (Cannot retake in this session)
    if (showLockedCompetenceDialog) {
        AlertDialog(
            onDismissRequest = { showLockedCompetenceDialog = false },
            title = {
                Text(
                    text = "Competence Completed & Locked",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This competence has already been completed and submitted for this session. It cannot be retaken in this active examination session.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLockedCompetenceDialog = false },
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
}

@Composable
private fun MockCompetenceHubCard(
    competence: CompetenceInfo,
    itemCount: Int,
    answeredCount: Int,
    status: CompetenceStatus,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

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

    val borderStroke = if (isDark) {
        when (status) {
            CompetenceStatus.COMPLETED -> BorderStroke(1.dp, PassGreen.copy(alpha = 0.5f))
            CompetenceStatus.IN_PROGRESS -> BorderStroke(1.dp, SeaCyan.copy(alpha = 0.5f))
            CompetenceStatus.NOT_STARTED -> BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
        }
    } else {
        BorderStroke(1.dp, Color(0xFFCBD5E1))
    }

    val cardBgColor = if (isDark) {
        MaterialTheme.colorScheme.surface
    } else {
        Color(0xFFFFFFFF)
    }

    val badgeBg = if (isDark) {
        statusBg
    } else {
        Color(0xFF0284C7).copy(alpha = 0.12f)
    }

    val badgeTextColor = if (isDark) {
        statusColor
    } else {
        Color(0xFF0F172A)
    }

    val badgeBorder = if (isDark) {
        null
    } else {
        BorderStroke(1.dp, Color(0xFF94A3B8))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
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
                    .background(badgeBg)
                    .let { if (badgeBorder != null) it.border(badgeBorder, CircleShape) else it },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = competence.code,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = badgeTextColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Center Content: Title and item count (STRICT RULE: NO SCORES OR PERCENTAGES)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${competence.code}: ${competence.title} — $itemCount Items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$answeredCount / $itemCount Answered",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right Status Badge
            val statusBgColor = if (isDark) {
                statusBg
            } else {
                when (status) {
                    CompetenceStatus.NOT_STARTED -> Color(0xFFE2E8F0)
                    CompetenceStatus.IN_PROGRESS -> Color(0xFFFEF3C7)
                    CompetenceStatus.COMPLETED -> Color(0xFFDCFCE7)
                }
            }
            val statusTextColor = if (isDark) {
                statusColor
            } else {
                when (status) {
                    CompetenceStatus.NOT_STARTED -> Color(0xFF475569)
                    CompetenceStatus.IN_PROGRESS -> Color(0xFFB45309)
                    CompetenceStatus.COMPLETED -> Color(0xFF15803D)
                }
            }
            val statusBorder = if (isDark) {
                BorderStroke(0.75.dp, statusColor.copy(alpha = 0.6f))
            } else {
                when (status) {
                    CompetenceStatus.NOT_STARTED -> BorderStroke(1.dp, Color(0xFFCBD5E1))
                    CompetenceStatus.IN_PROGRESS -> BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                    CompetenceStatus.COMPLETED -> BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.4f))
                }
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusBgColor,
                border = statusBorder
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = statusTextColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isLocked) "Completed (Locked)" else statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor
                    )
                }
            }
        }
    }
}
