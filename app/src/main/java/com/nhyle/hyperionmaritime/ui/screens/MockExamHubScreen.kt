package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.model.CompetenceInfo
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.ui.theme.MaritimeGoldBright
import com.nhyle.hyperionmaritime.ui.theme.PassGreen
import com.nhyle.hyperionmaritime.ui.theme.PureWhite
import com.nhyle.hyperionmaritime.ui.theme.SeaCyan
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

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

    val isDark = isSystemInDarkTheme()
    val colors = LocalSpaghettiColors.current
    val headerBg = colors.surface
    val headerTitleColor = colors.foreground
    val headerSubColor = colors.muted
    val headerIconColor = colors.foreground
    val headerBorder = BorderStroke(1.dp, colors.border)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Master Top Bar with countdown timer & Back Button
        Surface(
            color = headerBg,
            border = headerBorder,
            shape = RectangleShape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.openDrawer()
                    },
                    modifier = Modifier.testTag("topbar_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = headerIconColor
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (selectedTrack) {
                            Track.DECK_OIC_NW -> "OIC-NW Mock Examination"
                            Track.GMDSS_GOC -> "GMDSS GOC Mock Examination"
                            Track.ENGINE_OIC_EW -> "Engine OIC-EW Mock Examination"
                            Track.ENGINE_MGT -> "Engine Management Mock Examination"
                            Track.DECK_MGT -> "Deck Management Mock Examination"
                        },
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
            // Exam Banner Notice - Box UI (RectangleShape)
            item {
                Surface(
                    shape = RectangleShape,
                    color = colors.surface,
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(colors.primary)
                        )
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Licensure Examination Protocol",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.foreground,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select any competence to begin. Timers are competency-based (1 minute per item) and only count down while actively inside that specific competence module.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Iterate through functions and competences
            groupedByFunction.forEach { (functionId, functionCompetences) ->
                item {
                    val functionTitle = MaritimeCompetenceRegistry.getFunctionTitle(functionId, selectedTrack)
                    val permitItems = MaritimeCompetenceRegistry.getPermitItemsForFunction(functionId, selectedTrack)
                    val compQuestionsForFunc = activeQuestions.filter { it.functionId == functionId }
                    val functionItemsCount = if (compQuestionsForFunc.isNotEmpty()) compQuestionsForFunc.size else permitItems

                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = functionTitle.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$functionItemsCount Items • MARINA Official Permit",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted
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

        // Bottom Sticky Action Bar: "Submit Overall Exam" - Box UI (RectangleShape)
        Surface(
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            shape = RectangleShape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "$answeredCount / $totalQuestions Answered",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (flaggedCount > 0) {
                        Text(
                            text = "$flaggedCount flagged for review",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (unansweredCount > 0) {
                        Text(
                            text = "$unansweredCount items left to complete",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "All items answered",
                            style = MaterialTheme.typography.labelSmall,
                            color = PassGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isExamSubmitted) {
                            viewModel.navigateTo(Screen.Results)
                        } else {
                            showSubmitDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.primaryForeground
                    ),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Icon(
                        imageVector = if (isExamSubmitted) Icons.Default.CheckCircle else Icons.Default.Send,
                        contentDescription = null,
                        tint = colors.primaryForeground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExamSubmitted) "View Results" else "Submit Overall Exam",
                        color = colors.primaryForeground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }

    // Submit Overall Exam Confirmation Dialog
    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Submit Overall Exam?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You have answered $answeredCount out of $totalQuestions questions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.foreground
                    )
                    if (unansweredCount > 0) {
                        Text(
                            text = "⚠️ Warning: You have $unansweredCount unanswered questions. Unanswered questions will receive 0 points.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error
                        )
                    }
                    Text(
                        text = "Once submitted, your official competence scores, Bloom's point weights, and overall pass/fail verdict will be calculated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
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
                        containerColor = colors.primary,
                        contentColor = colors.primaryForeground
                    ),
                    shape = RectangleShape
                ) {
                    Text("Confirm & Submit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSubmitDialog = false },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("Review Exam", color = colors.foreground)
                }
            }
        )
    }

    // Strict Exam Lock-In Confirmation Dialog (Non-dismissible)
    if (showLockInDialog) {
        AlertDialog(
            onDismissRequest = { /* Non-dismissible by tapping outside */ },
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Mock Examination In Progress",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.foreground
                )
            },
            text = {
                Text(
                    text = "Official mock examinations cannot be exited until all competencies are completed and submitted. If you wish to finish now, you must submit your overall exam.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLockInDialog = false
                        viewModel.submitExam()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.primaryForeground
                    ),
                    shape = RectangleShape
                ) {
                    Text("Submit Exam Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLockInDialog = false },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("Continue Exam", color = colors.foreground)
                }
            }
        )
    }

    // Entry Confirmation Dialog (Before Opening a Competence)
    if (pendingCompetenceId != null) {
        AlertDialog(
            onDismissRequest = { pendingCompetenceId = null },
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Start Competence Confirmation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
            },
            text = {
                Text(
                    text = "Once you enter this competence, you CANNOT exit, switch, or return to the main hub until all questions in this competence have been answered and submitted. Are you ready to begin?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
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
                        containerColor = colors.primary,
                        contentColor = colors.primaryForeground
                    ),
                    shape = RectangleShape
                ) {
                    Text("Begin Competence", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingCompetenceId = null },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("Cancel", color = colors.foreground)
                }
            }
        )
    }

    // Locked Competence Warning Dialog (Cannot retake in this session)
    if (showLockedCompetenceDialog) {
        AlertDialog(
            onDismissRequest = { showLockedCompetenceDialog = false },
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Competence Completed & Locked",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
            },
            text = {
                Text(
                    text = "This competence has already been completed and submitted for this session. It cannot be retaken in this active examination session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLockedCompetenceDialog = false },
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
    val isDark = isSystemInDarkTheme()
    val colors = LocalSpaghettiColors.current

    val statusText = when (status) {
        CompetenceStatus.NOT_STARTED -> "Not Started"
        CompetenceStatus.IN_PROGRESS -> "In Progress"
        CompetenceStatus.COMPLETED -> "Completed"
    }

    val statusColor = when (status) {
        CompetenceStatus.NOT_STARTED -> colors.muted
        CompetenceStatus.IN_PROGRESS -> colors.primary
        CompetenceStatus.COMPLETED -> PassGreen
    }

    val statusBgColor = when (status) {
        CompetenceStatus.NOT_STARTED -> if (isDark) Color(0xFF23252A) else Color(0xFFE2E8F0)
        CompetenceStatus.IN_PROGRESS -> if (isDark) colors.primary.copy(alpha = 0.15f) else Color(0xFFDCFCE7)
        CompetenceStatus.COMPLETED -> if (isDark) Color(0xFF0F3222) else Color(0xFFDCFCE7)
    }

    val statusBorder = when (status) {
        CompetenceStatus.NOT_STARTED -> BorderStroke(1.dp, colors.border)
        CompetenceStatus.IN_PROGRESS -> BorderStroke(1.dp, colors.primary)
        CompetenceStatus.COMPLETED -> BorderStroke(1.dp, PassGreen)
    }

    val borderStroke = when (status) {
        CompetenceStatus.COMPLETED -> BorderStroke(1.dp, PassGreen.copy(alpha = 0.5f))
        CompetenceStatus.IN_PROGRESS -> BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f))
        CompetenceStatus.NOT_STARTED -> BorderStroke(1.dp, colors.border)
    }

    val badgeBg = when (status) {
        CompetenceStatus.NOT_STARTED -> colors.input
        CompetenceStatus.IN_PROGRESS -> colors.primary.copy(alpha = 0.12f)
        CompetenceStatus.COMPLETED -> PassGreen.copy(alpha = 0.12f)
    }

    val badgeTextColor = when (status) {
        CompetenceStatus.NOT_STARTED -> colors.foreground
        CompetenceStatus.IN_PROGRESS -> colors.primary
        CompetenceStatus.COMPLETED -> PassGreen
    }

    val badgeBorderColor = when (status) {
        CompetenceStatus.NOT_STARTED -> colors.border
        CompetenceStatus.IN_PROGRESS -> colors.primary
        CompetenceStatus.COMPLETED -> PassGreen
    }

    // Box UI Card with RectangleShape
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .clickable(onClick = onClick),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Competence Code Badge - Box UI (RectangleShape)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(badgeBg, RectangleShape)
                    .border(1.dp, badgeBorderColor, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = competence.code,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = badgeTextColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Center Content: Title and item count
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${competence.code}: ${competence.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right Status Badge anchored to top-right - Box UI (RectangleShape)
                    Surface(
                        shape = RectangleShape,
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
                                    tint = statusColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (isLocked) "Completed (Locked)" else statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$answeredCount / $itemCount Answered • $itemCount Items",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            }
        }
    }
}
