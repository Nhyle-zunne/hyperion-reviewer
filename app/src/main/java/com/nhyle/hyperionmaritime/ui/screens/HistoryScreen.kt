package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import com.nhyle.hyperionmaritime.data.local.DeletedSession
import com.nhyle.hyperionmaritime.data.local.expirationLabel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.nhyle.hyperionmaritime.ui.theme.PureWhite
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.local.ExamRecordEntity
import com.nhyle.hyperionmaritime.data.local.SessionJsonHelper
import com.nhyle.hyperionmaritime.data.local.dateFormatted
import com.nhyle.hyperionmaritime.data.local.score
import com.nhyle.hyperionmaritime.ui.components.DeleteSessionLogConfirmationDialog
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianButton
import com.nhyle.hyperionmaritime.ui.components.MianRuleButton
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.MianSectionHeader
import com.nhyle.hyperionmaritime.ui.components.SpaghettiProgressBar
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" History & Diagnostics Screen:
 * Exam session archive, multi-item batch management, competence breakdown audit.
 */
@Composable
fun HistoryScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val examRecords by viewModel.examRecords.collectAsState()
    val studyStreak by viewModel.studyStreak.collectAsState()
    val deletedSessionsState by viewModel.deletedSessions.collectAsState(initial = emptyList())

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedRecordIds by remember { mutableStateOf(setOf<String>()) }
    var recordToView by remember { mutableStateOf<ExamRecordEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var sessionPendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showRecentlyDeletedDialog by remember { mutableStateOf(false) }
    var sessionToPermanentlyDelete by remember { mutableStateOf<DeletedSession?>(null) }

    BackHandler {
        if (sessionPendingDeleteId != null) {
            sessionPendingDeleteId = null
        } else if (isSelectionMode) {
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
                subtitle = if (examRecords.size == 1) "1 LOG" else "${examRecords.size} LOGS",
                showBackButton = false,
                onMenuClick = onOpenDrawer,
                onClearAllClick = if (examRecords.isNotEmpty()) {
                    { viewModel.clearHistory() }
                } else null
            )
        }

        // Persistent RECENTLY DELETED Action Bar directly below top app bar
        OutlinedButton(
            onClick = { showRecentlyDeletedDialog = true },
            shape = RectangleShape,
            border = BorderStroke(1.dp, colors.border),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = colors.surface
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("history_recently_deleted_bar")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RECENTLY DELETED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
            }
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
                            .clip(RectangleShape)
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

                    // Duolingo-style Streak Tracker Banner
                    MianRuleCard(
                        ruleColor = Color(0xFFFF9800), // Energetic Flame Orange
                        containerColor = colors.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFFF9800).copy(alpha = 0.15f), RectangleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🔥",
                                        fontSize = 22.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "$studyStreak DAY STUDY STREAK",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.foreground,
                                        letterSpacing = (-0.2).sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (studyStreak > 0) "You're on fire! Keep up daily reviews." else "Complete a review today to start your streak!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.muted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RectangleShape,
                                color = Color(0xFFFF9800).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$studyStreak DAYS",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFF9800),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                item {

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
                                    shape = RectangleShape,
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

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "TEST SCORES OVER TIME",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.muted,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val sortedRecords = examRecords.sortedBy { it.timestamp }
                            if (sortedRecords.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .background(colors.surface2, RectangleShape)
                                        .padding(8.dp)
                                ) {
                                    val lineColor = colors.accent
                                    val passedColor = colors.success
                                    val failedColor = colors.error
                                    val textColor = colors.muted

                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        val width = size.width
                                        val height = size.height
                                        val count = sortedRecords.size

                                        // Draw 70% passing threshold line
                                        val passingY = height * (1f - 0.7f)
                                        drawLine(
                                            color = textColor.copy(alpha = 0.3f),
                                            start = androidx.compose.ui.geometry.Offset(0f, passingY),
                                            end = androidx.compose.ui.geometry.Offset(width, passingY),
                                            strokeWidth = 1.dp.toPx(),
                                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )

                                        if (count == 1) {
                                            val pt = sortedRecords.first()
                                            val py = height * (1f - (pt.percentage.toFloat() / 100f).coerceIn(0f, 1f))
                                            drawCircle(
                                                color = if (pt.isPassed) passedColor else failedColor,
                                                radius = 5.dp.toPx(),
                                                center = androidx.compose.ui.geometry.Offset(width / 2f, py)
                                            )
                                        } else {
                                            val stepX = width / (count - 1).toFloat()
                                            val points = sortedRecords.mapIndexed { index, record ->
                                                val x = index * stepX
                                                val y = height * (1f - (record.percentage.toFloat() / 100f).coerceIn(0f, 1f))
                                                androidx.compose.ui.geometry.Offset(x, y)
                                            }

                                            for (i in 0 until points.size - 1) {
                                                drawLine(
                                                    color = lineColor,
                                                    start = points[i],
                                                    end = points[i + 1],
                                                    strokeWidth = 2.5.dp.toPx()
                                                )
                                            }

                                            points.forEachIndexed { index, offset ->
                                                val record = sortedRecords[index]
                                                drawCircle(
                                                    color = if (record.isPassed) passedColor else failedColor,
                                                    radius = 4.dp.toPx(),
                                                    center = offset
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    MianSectionHeader(title = "EXAM ATTEMPTS (LONG PRESS TO SELECT)")
                }

                items(examRecords) { record ->
                    val isSelected = selectedRecordIds.contains(record.id)
                    val isMock = record.mode.contains("MOCK", ignoreCase = true)
                    val isInProgress = record.detailsSummary.startsWith("IN_PROGRESS", ignoreCase = true) || (!record.detailsSummary.startsWith("COMPLETED", ignoreCase = true) && !isMock)
                    val statusText = when {
                        isInProgress -> "IN PROGRESS"
                        isMock && record.isPassed -> "PASSED"
                        isMock -> "FAILED"
                        record.isPassed -> "PASSED"
                        else -> "COMPLETED"
                    }
                    val statusColor = when {
                        isInProgress -> Color(0xFFFFB300)
                        record.isPassed -> colors.success
                        else -> colors.error
                    }

                    val isMistakes = record.mode.equals("MISTAKES", ignoreCase = true) || record.competence.equals("MISTAKES", ignoreCase = true)
                    val titleText = if (isMistakes) {
                        "Mistakes"
                    } else if (isMock) {
                        val mockTrackTag = when (record.track.removeSuffix("_MISTAKES")) {
                            "DECK_OIC_NW" -> "OIC-NW"
                            "GMDSS_GOC" -> "GMDSS"
                            "ENGINE_OIC_EW" -> "OIC-EW"
                            "DECK_MGT" -> "DECK MGT"
                            "ENGINE_MGT" -> "ENGINE MGT"
                            else -> record.track.replace('_', ' ')
                        }
                        "MOCK EXAM · $mockTrackTag"
                    } else {
                        buildString {
                            val scope = com.nhyle.hyperionmaritime.data.local.DrillSessionFormatter.formatScopeLabel(record.competence, record.part, record.track)
                            if (scope.isNotBlank() && scope != record.track.replace('_', '-')) {
                                append(scope)
                            } else {
                                append(record.track.replace('_', ' '))
                                if (!record.drillTitle.isNullOrBlank()) {
                                    append(" • ${record.drillTitle}")
                                } else {
                                    append(" • ${record.mode}")
                                }
                            }
                        }.uppercase()
                    }

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
                                    } else if (isInProgress) {
                                        recordToView = record
                                    } else {
                                        viewModel.openSessionReview(record.id)
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
                                        text = titleText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.foreground,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RectangleShape,
                                            color = colors.surface2,
                                            border = BorderStroke(1.dp, colors.border)
                                        ) {
                                            Text(
                                                text = statusText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = statusColor,
                                                fontSize = 10.sp
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (sessionPendingDeleteId == null) {
                                                    sessionPendingDeleteId = record.id
                                                }
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("delete_session_btn_${record.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete Session",
                                                tint = colors.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val sessionStudyTimeMs by viewModel.getSessionStudyTimeMs(record.id).collectAsState(initial = null)
                                val sessionTimeText = if (sessionStudyTimeMs != null && sessionStudyTimeMs!! > 0L) {
                                    com.nhyle.hyperionmaritime.data.studytime.StudyTimeCalculator.formatDurationFull(sessionStudyTimeMs!!)
                                } else {
                                    "—"
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        Text(
                                            text = "${record.score} / ${record.totalQuestions} items (${record.percentage.toInt()}%)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.foreground
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = record.dateFormatted,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.muted,
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.muted,
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                text = "TIME: $sessionTimeText",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (sessionTimeText == "—") colors.muted else (if (colors.isDark) Color(0xFF4ADE80) else Color(0xFF15803D)),
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    if (!isInProgress) {
                                        Surface(
                                            shape = RectangleShape,
                                            color = colors.surface2,
                                            border = BorderStroke(1.dp, colors.border)
                                        ) {
                                            Text(
                                                text = "VIEW AUDIT »",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.accent,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                if (isInProgress && !isMock) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    MianButton(
                                        onClick = { viewModel.resumeIncompleteExamRecord(record) },
                                        accent = true,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "CONTINUE PRACTICE DRILL ➔",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
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
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            shape = RectangleShape,
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
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            shape = RectangleShape,
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
                Button(
                    onClick = {
                        viewModel.deleteExamRecords(selectedRecordIds.toList())
                        isSelectionMode = false
                        selectedRecordIds = emptySet()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = PureWhite
                    ),
                    shape = RectangleShape
                ) {
                    Text("DELETE", color = PureWhite, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("CANCEL", color = colors.foreground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Individual Session Log Delete Confirmation Dialog
    sessionPendingDeleteId?.let { targetId ->
        DeleteSessionLogConfirmationDialog(
            sessionId = targetId,
            onConfirmDelete = { idToDelete ->
                sessionPendingDeleteId = null
                viewModel.deleteExamRecord(idToDelete)
            },
            onDismiss = {
                sessionPendingDeleteId = null
            }
        )
    }

    // Recently Deleted Sessions Dialog
    if (showRecentlyDeletedDialog) {
        AlertDialog(
            onDismissRequest = { showRecentlyDeletedDialog = false },
            modifier = Modifier
                .border(BorderStroke(1.5.dp, colors.border), RectangleShape)
                .testTag("recently_deleted_dialog"),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.accent, RectangleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Recently Deleted Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        modifier = Modifier.testTag("recently_deleted_dialog_title")
                    )
                }
            },
            text = {
                if (deletedSessionsState.isEmpty()) {
                    Text(
                        text = "No recently deleted sessions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        modifier = Modifier.testTag("recently_deleted_empty_text")
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(deletedSessionsState, key = { ds -> ds.sessionId }) { ds ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, colors.border, RectangleShape)
                                    .background(colors.surface2)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ds.label,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.foreground
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${ds.date} • Progress: ${ds.progress}",
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.muted
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = ds.expirationLabel,
                                        style = TextStyle(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.warning
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(
                                        onClick = { sessionToPermanentlyDelete = ds },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = colors.error,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "PERMANENTLY DELETE",
                                                style = TextStyle(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = colors.error
                                                )
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.restoreDeletedSession(ds)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.primaryForeground
                                    ),
                                    shape = RectangleShape,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("restore_session_${ds.sessionId}")
                                ) {
                                    Text(
                                        text = "RESTORE",
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRecentlyDeletedDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.primaryForeground
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.testTag("recently_deleted_close_button")
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    // Permanent Delete Confirmation Dialog
    sessionToPermanentlyDelete?.let { targetSession ->
        AlertDialog(
            onDismissRequest = { sessionToPermanentlyDelete = null },
            modifier = Modifier
                .border(BorderStroke(1.5.dp, colors.border), RectangleShape)
                .testTag("permanent_delete_dialog"),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Permanently Delete Session Log?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete this session log? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeleteSession(targetSession)
                        sessionToPermanentlyDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = PureWhite
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.testTag("permanent_delete_confirm_button")
                ) {
                    Text("PERMANENTLY DELETE", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { sessionToPermanentlyDelete = null },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("CANCEL", color = colors.foreground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
