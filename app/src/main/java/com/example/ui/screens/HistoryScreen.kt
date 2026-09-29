package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExamRecordEntity
import com.example.data.local.SessionJsonHelper
import com.example.data.local.dateFormatted
import com.example.data.local.score
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.MianButton
import com.example.ui.components.MianRuleButton
import com.example.ui.components.MianRuleCard
import com.example.ui.components.MianSectionHeader
import com.example.ui.components.SpaghettiProgressBar
import com.example.ui.theme.LocalSpaghettiColors
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" History & Diagnostics Screen:
 * Exam session archive, multi-item batch management, competence breakdown audit.
 */
@Composable
fun HistoryScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val examRecords by viewModel.examRecords.collectAsState()

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedRecordIds by remember { mutableStateOf(setOf<String>()) }
    var recordToView by remember { mutableStateOf<ExamRecordEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedRecordIds = emptySet()
        } else {
            viewModel.navigateBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (isSelectionMode) {
            // Selection Mode Bar
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
                                    isSelectionMode = false
                                    selectedRecordIds = emptySet()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = colors.foreground
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${selectedRecordIds.size} SELECTED",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    selectedRecordIds = if (selectedRecordIds.size == examRecords.size) {
                                        emptySet()
                                    } else {
                                        examRecords.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Select All",
                                    tint = colors.foreground
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (selectedRecordIds.isNotEmpty()) {
                                        showDeleteConfirmDialog = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = if (selectedRecordIds.isNotEmpty()) colors.accent else colors.muted
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.accent)
                    )
                }
            }
        } else {
            MaritimeTopBar(
                title = "SESSION LOG & DIAGNOSTICS",
                subtitle = "${examRecords.size} PAST ATTEMPTS",
                showBackButton = true,
                onBackClick = { viewModel.navigateBack() }
            )
        }

        if (examRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NO EXAM SESSIONS RECORDED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Complete practice drills or mock exams to view competency diagnostics and history here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    val passedCount = examRecords.count { it.isPassed }
                    val avgPercentage = examRecords.map { it.percentage }.average()

                    MianRuleCard(
                        ruleColor = colors.accent,
                        containerColor = colors.surface
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CUMULATIVE PERFORMANCE",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.foreground,
                                    letterSpacing = (-0.2).sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = colors.surface2
                                ) {
                                    Text(
                                        text = "$passedCount / ${examRecords.size} PASSED",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (passedCount > 0) colors.success else colors.muted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "AVERAGE SCORE: ${avgPercentage.toInt()}%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (avgPercentage >= 70.0) colors.success else colors.accent,
                                letterSpacing = (-0.8).sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            SpaghettiProgressBar(progress = (avgPercentage / 100.0).toFloat())
                        }
                    }
                }

                item {
                    MianSectionHeader(title = "EXAM ATTEMPTS (LONG PRESS TO SELECT)")
                }

                items(examRecords) { record ->
                    val isSelected = selectedRecordIds.contains(record.id)
                    val statusColor = if (record.isPassed) colors.success else colors.error

                    MianRuleCard(
                        ruleColor = statusColor,
                        containerColor = if (isSelected) colors.surface2 else colors.surface,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    isSelectionMode = true
                                    selectedRecordIds = selectedRecordIds + record.id
                                },
                                onTap = {
                                    if (isSelectionMode) {
                                        selectedRecordIds = if (isSelected) {
                                            selectedRecordIds - record.id
                                        } else {
                                            selectedRecordIds + record.id
                                        }
                                        if (selectedRecordIds.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    } else {
                                        recordToView = record
                                    }
                                }
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelectionMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedRecordIds = if (checked) {
                                            selectedRecordIds + record.id
                                        } else {
                                            selectedRecordIds - record.id
                                        }
                                        if (selectedRecordIds.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = colors.accent,
                                        checkmarkColor = colors.accentForeground
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${record.track.replace('_', ' ')} • ${record.mode}".uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.muted,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = colors.surface2
                                    ) {
                                        Text(
                                            text = if (record.isPassed) "PASSED" else "FAILED",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = statusColor,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = "${record.score} / ${record.totalQuestions} items (${record.percentage.toInt()}%)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.foreground
                                    )

                                    Text(
                                        text = record.dateFormatted,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.muted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Detail Audit Modal for a past exam record
    recordToView?.let { record ->
        val breakdown = SessionJsonHelper.deserializeCompetenceBreakdown(record.competenceBreakdownJson)
        val statusColor = if (record.isPassed) colors.success else colors.error

        AlertDialog(
            onDismissRequest = { recordToView = null },
            shape = RoundedCornerShape(2.dp),
            containerColor = colors.surface,
            title = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "EXAM ATTEMPT DETAILS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground
                    )
                    Text(
                        text = "${record.track} • ${record.dateFormatted}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Score: ${record.score} / ${record.totalQuestions} (${record.percentage.toInt()}%) • ${if (record.isPassed) "PASSED" else "FAILED"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )

                    if (breakdown.isNotEmpty()) {
                        Text(
                            text = "COMPETENCY BREAKDOWN:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.muted
                        )
                        breakdown.forEach { comp ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${comp.competenceId}: ${comp.score}/${comp.totalQuestions}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.foreground
                                )
                                Text(
                                    text = "${comp.percentage.toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (comp.percentage >= 70.0) colors.success else colors.error
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                MianButton(onClick = { recordToView = null }) {
                    Text("CLOSE", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    // Delete Confirmation Modal
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
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
                        text = "DELETE SELECTED RECORDS?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete ${selectedRecordIds.size} session record(s)?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                MianButton(
                    onClick = {
                        viewModel.deleteExamRecords(selectedRecordIds.toList())
                        isSelectionMode = false
                        selectedRecordIds = emptySet()
                        showDeleteConfirmDialog = false
                    },
                    accent = true
                ) {
                    Text("DELETE", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("CANCEL", color = colors.muted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
