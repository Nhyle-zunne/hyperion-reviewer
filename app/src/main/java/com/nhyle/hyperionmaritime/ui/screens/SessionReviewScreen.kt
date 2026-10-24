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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.MianSectionHeader
import com.nhyle.hyperionmaritime.ui.components.QuestionPaletteDialog
import com.nhyle.hyperionmaritime.ui.components.SpaghettiProgressBar
import com.nhyle.hyperionmaritime.data.model.UserQuestionState
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel

/**
 * Read-only review of a COMPLETED session, laid out like Quick Scan.
 * Shows each question with the correct answer in green and the user's wrong pick in red.
 * It never resumes, edits or saves the session.
 */
@Composable
fun SessionReviewScreen(
    viewModel: ReviewerViewModel,
    sessionId: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val review by viewModel.sessionReview.collectAsState()
    val allItems = review?.items.orEmpty()

    val isMockExam = remember(review, allItems) {
        review?.title?.contains("Mock Exam", ignoreCase = true) == true ||
            allItems.map { it.question.competenceId }.distinct().size > 1
    }

    var selectedMockTab by rememberSaveable(sessionId) { mutableIntStateOf(0) } // 0: Breakdown, 1: Question Review
    var selectedCompetenceFilter by rememberSaveable(sessionId) { mutableStateOf<String?>(null) }
    var mockMissedOnly by rememberSaveable(sessionId) { mutableStateOf(false) }

    val competences = remember(allItems) {
        allItems.map { it.question.competenceId }.distinct()
    }
    val compSummaries = remember(allItems, competences) {
        val allComps = MaritimeCompetenceRegistry.getCompetencesForTrack(viewModel.selectedTrack.value)
        val compInfoMap = allComps.associateBy { it.competenceId }
        competences.map { compId ->
            val compItems = allItems.filter { it.question.competenceId == compId }
            val correctCount = compItems.count { it.isCorrect }
            val totalCount = compItems.size
            val percent = if (totalCount > 0) (correctCount.toDouble() / totalCount * 100.0) else 0.0
            val isPassed = percent >= 70.0
            val title = compInfoMap[compId]?.title ?: "Competence $compId"
            MockCompReviewSummary(
                competenceId = compId,
                title = title,
                score = correctCount,
                totalQuestions = totalCount,
                percentage = percent,
                isPassed = isPassed
            )
        }
    }
    val totalCorrect = remember(allItems) { allItems.count { it.isCorrect } }
    val totalAllCount = allItems.size
    val overallPercent = remember(totalCorrect, totalAllCount) {
        if (totalAllCount > 0) (totalCorrect.toDouble() / totalAllCount * 100.0) else 0.0
    }
    val isOverallPassed = remember(compSummaries) {
        compSummaries.isNotEmpty() && compSummaries.all { it.isPassed }
    }
    val overallStatusColor = if (isOverallPassed) Color(0xFF22C55E) else colors.error

    var missedOnly by rememberSaveable(sessionId) { mutableStateOf(false) }
    var currentIndex by rememberSaveable(sessionId) { mutableIntStateOf(0) }
    var showPalette by remember { mutableStateOf(false) }

    val missedCount = allItems.count { !it.isCorrect }
    val items = if (missedOnly) allItems.filter { !it.isCorrect } else allItems
    val total = items.size
    val index = if (total == 0) 0 else currentIndex.coerceIn(0, total - 1)
    val current = items.getOrNull(index)
    val paletteStates = items.associate { item ->
        item.question.id to UserQuestionState(
            questionId = item.question.id,
            selectedIndex = item.selectedIndex,
            isSubmitted = item.selectedIndex != null,
            isCorrect = item.isCorrect
        )
    }

    BackHandler { viewModel.navigateBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = if (isMockExam) "MOCK EXAM REVIEW" else "SESSION REVIEW",
            subtitle = review?.title?.uppercase() ?: "COMPLETED SESSION",
            showBackButton = true,
            onBackClick = { viewModel.navigateBack() }
        )

        if (isMockExam) {
            // MOCK EXAM MODE: SEPARATED BY COMPETENCE WITH TABS & QUICK SCAN
            TabRow(
                selectedTabIndex = selectedMockTab,
                containerColor = colors.surface,
                contentColor = colors.foreground,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedMockTab]),
                        height = 3.dp,
                        color = colors.accent
                    )
                },
                divider = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.border)
                    )
                }
            ) {
                Tab(
                    selected = selectedMockTab == 0,
                    onClick = { selectedMockTab = 0 },
                    text = {
                        Text(
                            text = "COMPETENCE BREAKDOWN",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = if (selectedMockTab == 0) colors.accent else colors.muted
                        )
                    }
                )
                Tab(
                    selected = selectedMockTab == 1,
                    onClick = { selectedMockTab = 1 },
                    text = {
                        Text(
                            text = "QUESTION REVIEW (${allItems.size})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = if (selectedMockTab == 1) colors.accent else colors.muted
                        )
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
            ) {
                if (selectedMockTab == 0) {
                    item {
                        MianRuleCard(
                            ruleColor = overallStatusColor,
                            containerColor = colors.surface
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
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
                                            text = if (isOverallPassed) "PASSED • 70% THRESHOLD MET" else "FAILED • BELOW 70% CUTOFF",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = overallStatusColor,
                                            fontSize = 11.sp,
                                            letterSpacing = 1.2.sp
                                        )
                                    }
                                    Text(
                                        text = "$totalCorrect / $totalAllCount ITEMS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.muted,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        Text(
                                            text = "${overallPercent.toInt()}%",
                                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = overallStatusColor,
                                            letterSpacing = (-1.5).sp
                                        )
                                        Text(
                                            text = "SCORE: $totalCorrect / $totalAllCount CORRECT",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.muted,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isOverallPassed) "COMPETENT" else "NYC",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = overallStatusColor
                                        )
                                        Text(
                                            text = if (isOverallPassed) "READY FOR LICENSURE" else "NOT YET COMPETENT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.muted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                SpaghettiProgressBar(progress = (overallPercent / 100.0).toFloat())
                            }
                        }
                    }

                    item {
                        MianSectionHeader(title = "COMPETENCE EVALUATION (70% MARINA CUTOFF)")
                    }

                    items(compSummaries) { comp ->
                        val compColor = if (comp.isPassed) Color(0xFF22C55E) else colors.error
                        MianRuleCard(
                            ruleColor = compColor,
                            containerColor = colors.surface,
                            onClick = {
                                selectedCompetenceFilter = comp.competenceId
                                mockMissedOnly = false
                                selectedMockTab = 1
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${comp.competenceId} • ${comp.title}".uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.foreground,
                                        letterSpacing = (-0.2).sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RectangleShape,
                                        color = colors.surface2,
                                        border = BorderStroke(1.dp, colors.border)
                                    ) {
                                        Text(
                                            text = if (comp.isPassed) "PASSED" else "FAILED",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = compColor,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "${comp.score} / ${comp.totalQuestions} items (${comp.percentage.toInt()}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.foreground
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                SpaghettiProgressBar(progress = (comp.percentage / 100.0).toFloat())

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "BROWSE QUESTIONS →",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp,
                                        color = colors.accent
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: QUESTION REVIEW (SEPARATED BY COMPETENCE)
                    if (compSummaries.size > 1) {
                        item {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    ReviewFilterChip(
                                        label = "ALL (${allItems.size})",
                                        selected = selectedCompetenceFilter == null
                                    ) {
                                        selectedCompetenceFilter = null
                                    }
                                }
                                items(compSummaries) { comp ->
                                    val isSelected = selectedCompetenceFilter == comp.competenceId
                                    ReviewFilterChip(
                                        label = "${comp.competenceId} (${comp.score}/${comp.totalQuestions})",
                                        selected = isSelected,
                                        borderColor = if (comp.isPassed) Color(0xFF22C55E) else colors.error
                                    ) {
                                        selectedCompetenceFilter = comp.competenceId
                                    }
                                }
                            }
                        }
                    }

                    val compItems = if (selectedCompetenceFilter == null) {
                        allItems
                    } else {
                        allItems.filter { it.question.competenceId.equals(selectedCompetenceFilter, ignoreCase = true) }
                    }
                    val missedInComp = compItems.count { !it.isCorrect }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ReviewFilterChip(
                                label = "ALL ${compItems.size}",
                                selected = !mockMissedOnly
                            ) {
                                mockMissedOnly = false
                            }
                            ReviewFilterChip(
                                label = "MISSED $missedInComp",
                                selected = mockMissedOnly
                            ) {
                                mockMissedOnly = true
                            }
                        }
                    }

                    val displayedCompItems = compItems.filter { !mockMissedOnly || !it.isCorrect }

                    if (displayedCompItems.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                shape = RectangleShape,
                                color = colors.surface2,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (compItems.isEmpty()) "No questions found for this competence." else "No missed questions for this competence!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.muted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(displayedCompItems.size) { compItemIdx ->
                            val item = displayedCompItems[compItemIdx]
                            val q = item.question
                            val selected = item.selectedIndex
                            val statusText = when {
                                selected == null -> "NOT ANSWERED"
                                item.isCorrect -> "CORRECT"
                                else -> "WRONG"
                            }
                            val statusColor = when {
                                selected == null -> Color(0xFFFFB300)
                                item.isCorrect -> Color(0xFF22C55E)
                                else -> colors.error
                            }

                            MianRuleCard(
                                ruleColor = statusColor,
                                containerColor = colors.surface
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
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
                                                text = "${q.competenceId} • ITEM ${compItemIdx + 1}/${displayedCompItems.size}".uppercase(),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                letterSpacing = 1.sp,
                                                color = colors.foreground
                                            )
                                        }
                                        Surface(
                                            shape = RectangleShape,
                                            color = colors.surface2,
                                            border = BorderStroke(1.dp, statusColor)
                                        ) {
                                            Text(
                                                text = statusText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                letterSpacing = 1.sp,
                                                color = statusColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "TOPIC: ${q.topic.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = q.questionText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.foreground,
                                        lineHeight = 22.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        q.options.forEachIndexed { optIndex, optionText ->
                                            val isCorrectOption = optIndex == q.correctAnswerIndex
                                            val isPicked = optIndex == selected
                                            val isWrongPick = isPicked && !isCorrectOption
                                            val optionLetter = when (optIndex) {
                                                0 -> "A"
                                                1 -> "B"
                                                2 -> "C"
                                                3 -> "D"
                                                else -> "${optIndex + 1}"
                                            }
                                            val green = Color(0xFF22C55E)
                                            val optBorderColor = when {
                                                isCorrectOption -> green
                                                isWrongPick -> colors.error
                                                else -> colors.border
                                            }
                                            val optBadgeBg = when {
                                                isCorrectOption -> green
                                                isWrongPick -> colors.error
                                                else -> colors.surface2
                                            }
                                            val optLetterColor = if (isCorrectOption || isWrongPick) Color.White else colors.muted

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RectangleShape,
                                                color = colors.surface2,
                                                border = BorderStroke(if (isCorrectOption || isWrongPick) 1.5.dp else 1.dp, optBorderColor)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RectangleShape)
                                                            .background(optBadgeBg)
                                                            .border(1.dp, optBorderColor),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = optionLetter,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 11.sp,
                                                            color = optLetterColor
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = optionText,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = colors.foreground,
                                                            lineHeight = 18.sp,
                                                            fontWeight = if (isCorrectOption || isPicked) FontWeight.Bold else FontWeight.Normal
                                                        )

                                                        if (isCorrectOption) {
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = green,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = if (isPicked) "YOUR ANSWER · CORRECT" else "CORRECT ANSWER",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = green,
                                                                    fontSize = 9.sp,
                                                                    letterSpacing = 0.5.sp
                                                                )
                                                            }
                                                        } else if (isWrongPick) {
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = null,
                                                                    tint = colors.error,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = "YOUR ANSWER",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = colors.error,
                                                                    fontSize = 9.sp,
                                                                    letterSpacing = 0.5.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (!q.rationale.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RectangleShape,
                                            color = colors.surface2.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, colors.border)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "EXPLANATION",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.muted,
                                                    fontSize = 10.sp,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = q.rationale,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.foreground,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // STANDARD PRACTICE DRILL REVIEW MODE (SINGLE QUESTION VIEWER)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReviewFilterChip(label = "ALL ${allItems.size}", selected = !missedOnly) {
                    missedOnly = false
                    currentIndex = 0
                }
                ReviewFilterChip(label = "MISSED $missedCount", selected = missedOnly) {
                    missedOnly = true
                    currentIndex = 0
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = review?.scoreText ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    color = colors.foreground
                )
            }

        if (current == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (allItems.isEmpty()) "The questions for this session are no longer available." else "No missed questions in this session.",
                    color = colors.muted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val q = current.question
            val selected = current.selectedIndex
            val statusText = when {
                selected == null -> "NOT ANSWERED"
                current.isCorrect -> "CORRECT"
                else -> "WRONG"
            }
            val statusColor = when {
                selected == null -> Color(0xFFFFB300)
                current.isCorrect -> Color(0xFF22C55E)
                else -> colors.error
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text(
                        text = "ITEM ${index + 1}/$total · ${q.competenceId}".uppercase(),
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
                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    MianRuleCard(
                        ruleColor = statusColor,
                        containerColor = colors.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "TOPIC: ${q.topic.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = q.questionText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.foreground,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                items(q.options.size) { optIndex ->
                    val optionText = q.options[optIndex]
                    val isCorrectOption = optIndex == q.correctAnswerIndex
                    val isPicked = optIndex == selected
                    val isWrongPick = isPicked && !isCorrectOption
                    val optionLetter = when (optIndex) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        3 -> "D"
                        else -> "${optIndex + 1}"
                    }
                    val green = Color(0xFF22C55E)
                    val borderColor = when {
                        isCorrectOption -> green
                        isWrongPick -> colors.error
                        else -> colors.border
                    }
                    val badgeBg = when {
                        isCorrectOption -> green
                        isWrongPick -> colors.error
                        else -> colors.surface2
                    }
                    val letterColor = if (isCorrectOption || isWrongPick) Color.White else colors.muted

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape,
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(if (isCorrectOption || isWrongPick) 1.5.dp else 1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RectangleShape)
                                    .background(badgeBg)
                                    .border(1.dp, borderColor),
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

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.foreground,
                                    lineHeight = 20.sp,
                                    fontWeight = if (isCorrectOption || isPicked) FontWeight.Bold else FontWeight.Normal
                                )

                                if (isCorrectOption) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Correct Answer",
                                            tint = green,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isPicked) "YOUR ANSWER · CORRECT" else "CORRECT ANSWER",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = green,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                                if (isPicked && !isCorrectOption) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isCorrectOption) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = "Your Answer",
                                            tint = if (isCorrectOption) green else colors.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "YOUR ANSWER",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isCorrectOption) green else colors.error,
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
                    Button(
                        onClick = { if (index > 0) currentIndex = index - 1 },
                        enabled = index > 0,
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
                            Text(text = "PREV", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Surface(
                        shape = RectangleShape,
                        color = colors.surface2,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .clip(RectangleShape)
                            .clickable { showPalette = true }
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
                                text = "Question ${index + 1} of $total",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { if (index < total - 1) currentIndex = index + 1 },
                        enabled = index < total - 1,
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
                            Text(text = "NEXT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
    }

    if (showPalette) {
        QuestionPaletteDialog(
            totalQuestions = total,
            currentIndex = index,
            userStates = paletteStates,
            bookmarkedIds = emptySet(),
            questionIds = items.map { it.question.id },
            onQuestionClick = { target -> currentIndex = target },
            onDismiss = { showPalette = false },
            onJumpToNumber = { target -> currentIndex = target }
        )
    }
}

@Composable
private fun ReviewFilterChip(
    label: String,
    selected: Boolean,
    borderColor: Color? = null,
    onClick: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    val strokeColor = when {
        selected -> colors.accent
        borderColor != null -> borderColor
        else -> colors.border
    }
    Surface(
        shape = RectangleShape,
        color = if (selected) colors.accent else colors.surface2,
        border = BorderStroke(1.dp, strokeColor),
        modifier = Modifier
            .clip(RectangleShape)
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            color = if (selected) colors.primaryForeground else colors.foreground
        )
    }
}

private data class MockCompReviewSummary(
    val competenceId: String,
    val title: String,
    val score: Int,
    val totalQuestions: Int,
    val percentage: Double,
    val isPassed: Boolean
)