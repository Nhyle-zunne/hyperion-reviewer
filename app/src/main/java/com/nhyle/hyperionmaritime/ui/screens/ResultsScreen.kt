package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nhyle.hyperionmaritime.data.util.InAppReviewHelper
import com.nhyle.hyperionmaritime.data.util.findActivity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.model.CompetenceScore
import com.nhyle.hyperionmaritime.data.model.ExamResult
import com.nhyle.hyperionmaritime.data.model.QuizMode
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
 * Editorial MIAN "Spaghetti" Examination Results Screen:
 * High contrast editorial diagnostics, 3dp top rules, Pass/Fail semantic indicators,
 * competence-by-competence 70% evaluation, full item review.
 */
@Composable
fun ResultsScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val examResult by viewModel.lastExamResult.collectAsState()
    val activeQuestions by viewModel.activeQuestions.collectAsState()
    val activeCompetenceId by viewModel.activeCompetenceId.collectAsState()
    val activePartLabel by viewModel.activePartLabel.collectAsState()
    val isCrpMode by viewModel.isCrpMode.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Breakdown, 1: Question Review
    var selectedCompetenceFilter by remember { mutableStateOf<String?>(null) }
    var reviewMissedOnly by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.finishExamToHome()
    }

    if (examResult == null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "NO EXAM RESULTS AVAILABLE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.muted
                )
            }
        }
        return
    }

    val result = examResult!!
    val isPassed = result.isPassed
    val statusColor = if (isPassed) colors.success else colors.error
    val isMockExam = result.mode == QuizMode.MOCK_EXAM
    val showResultsLoading by viewModel.showResultsLoading.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(result, showResultsLoading) {
        // Trigger review for completed mock exams OR passed drills (never on failure)
        val eligibleForReview = if (isMockExam) true else isPassed
        if (eligibleForReview && !showResultsLoading) {
            context.findActivity()?.let { activity ->
                InAppReviewHelper.maybePromptReview(activity)
            }
        }
    }

    // Resolve drill identity (e.g., C1, Part 4, or specific title)
    val compId = activeCompetenceId
        ?: result.drillTitle?.split(" ")?.firstOrNull { it.matches(Regex("^[C|c]\\d+$")) }
        ?: if (result.competenceScores.size == 1) result.competenceScores.first().competenceId else null
    val partLabel = activePartLabel

    val headerTitle = if (isMockExam) "EXAMINATION RESULTS" else "PRACTICE DRILL RESULTS"
    val headerSubtitle = when {
        isMockExam -> "${result.track} • MARINA EVALUATION"
        !result.drillTitle.isNullOrBlank() -> "${result.track} • ${result.drillTitle}"
        compId != null && partLabel != null -> "${result.track} • $compId ${partLabel.uppercase()}"
        compId != null -> "${result.track} • $compId DRILL"
        result.mode == QuizMode.VAULT_DRILL || result.mode == QuizMode.MISTAKES -> "${result.track} • Mistakes"
        result.mode == QuizMode.BOOKMARKS_DRILL -> "${result.track} • BOOKMARKS DRILL"
        else -> "${result.track} • PRACTICE DRILL"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = headerTitle,
            subtitle = headerSubtitle,
            showBackButton = true,
            onBackClick = { viewModel.finishExamToHome() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Overall Score Poster Banner
                MianRuleCard(
                    ruleColor = statusColor,
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
                                    text = if (isPassed) "PASSED • 70% THRESHOLD MET" else "FAILED • BELOW 70% CUTOFF",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.2.sp
                                )
                            }

                            Text(
                                text = "${result.score} / ${result.totalQuestions} ITEMS",
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
                                    text = "${result.percentage.toInt()}%",
                                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor,
                                    letterSpacing = (-1.5).sp
                                )
                                Text(
                                    text = "WEIGHTED SCORE: ${result.weightedScore.toInt()} / ${result.totalPossibleWeightedScore.toInt()} PTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.muted,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isPassed) "COMPETENT" else "NYC",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor
                                )
                                Text(
                                    text = if (isPassed) "READY FOR LICENSURE" else "NOT YET COMPETENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.muted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SpaghettiProgressBar(progress = (result.percentage / 100.0).toFloat())
                    }
                }
            }

            // Tabs: Breakdown vs Question Review
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colors.surface,
                    contentColor = colors.foreground,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
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
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "COMPETENCE BREAKDOWN",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = if (selectedTab == 0) colors.accent else colors.muted
                            )
                        }
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "QUESTION REVIEW (${result.totalQuestions})",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = if (selectedTab == 1) colors.accent else colors.muted
                            )
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                // TAB 0: COMPETENCE BREAKDOWN
                item {
                    MianSectionHeader(title = "COMPETENCE EVALUATION (70% MARINA CUTOFF)")
                }

                items(result.competenceBreakdown) { comp ->
                    val isCompPassed = comp.percentage >= 70.0
                    val compColor = if (isCompPassed) colors.success else colors.error

                    MianRuleCard(
                        ruleColor = compColor,
                        containerColor = colors.surface,
                        onClick = {
                            selectedCompetenceFilter = comp.competenceId
                            reviewMissedOnly = false
                            selectedTab = 1
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
                                        text = if (isCompPassed) "PASSED" else "FAILED",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = compColor,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${comp.score} / ${comp.totalQuestions} items (${comp.percentage.toInt()}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.foreground
                                )

                                Text(
                                    text = "${comp.weightedScore.toInt()} / ${comp.totalPossibleWeightedScore.toInt()} weighted pts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted
                                )
                            }

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
                // TAB 1: QUESTION REVIEW (SEPARATED BY COMPETENCE & QUICK SCAN)
                if (result.competenceScores.size > 1) {
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                ResultsFilterChip(
                                    label = "ALL (${result.questionResults.size})",
                                    selected = selectedCompetenceFilter == null
                                ) {
                                    selectedCompetenceFilter = null
                                }
                            }
                            items(result.competenceScores) { comp ->
                                ResultsFilterChip(
                                    label = "${comp.competenceId} (${comp.score}/${comp.totalQuestions})",
                                    selected = selectedCompetenceFilter == comp.competenceId,
                                    borderColor = if (comp.isPassed) Color(0xFF22C55E) else colors.error
                                ) {
                                    selectedCompetenceFilter = comp.competenceId
                                }
                            }
                        }
                    }
                }

                val compItems = if (selectedCompetenceFilter == null) {
                    result.questionResults
                } else {
                    result.questionResults.filter { it.competenceId.equals(selectedCompetenceFilter, ignoreCase = true) }
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
                        ResultsFilterChip(
                            label = "ALL ${compItems.size}",
                            selected = !reviewMissedOnly
                        ) {
                            reviewMissedOnly = false
                        }
                        ResultsFilterChip(
                            label = "MISSED $missedInComp",
                            selected = reviewMissedOnly
                        ) {
                            reviewMissedOnly = true
                        }
                    }
                }

                val displayedItems = compItems.filter { !reviewMissedOnly || !it.isCorrect }

                if (displayedItems.isEmpty()) {
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
                    items(displayedItems.size) { idx ->
                        val itemResult = displayedItems[idx]
                        val fullQ = remember(activeQuestions, itemResult.questionId) {
                            activeQuestions.firstOrNull { it.id == itemResult.questionId }
                        }
                        val isUnanswered = itemResult.selectedOptionIndex == null && itemResult.selectedOptionText == null
                        val statusColor = when {
                            isUnanswered -> Color(0xFFFFB300)
                            itemResult.isCorrect -> Color(0xFF22C55E)
                            else -> colors.error
                        }
                        val statusText = when {
                            isUnanswered -> "NOT ANSWERED"
                            itemResult.isCorrect -> "CORRECT"
                            else -> "WRONG"
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
                                            text = "${itemResult.competenceId} • ITEM ${idx + 1}/${displayedItems.size}".uppercase(),
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

                                if (fullQ != null && fullQ.topic.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "TOPIC: ${fullQ.topic.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = itemResult.questionText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.foreground,
                                    lineHeight = 22.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (fullQ != null && fullQ.options.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        fullQ.options.forEachIndexed { optIndex, optionText ->
                                            val isCorrectOption = optIndex == fullQ.correctAnswerIndex
                                            val isPicked = optIndex == itemResult.selectedOptionIndex
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
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RectangleShape,
                                            color = colors.surface2,
                                            border = BorderStroke(1.dp, if (itemResult.isCorrect) Color(0xFF22C55E) else colors.error)
                                        ) {
                                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (itemResult.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = if (itemResult.isCorrect) Color(0xFF22C55E) else colors.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "YOUR ANSWER: ${itemResult.selectedOptionText ?: "Unanswered"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (itemResult.isCorrect) Color(0xFF22C55E) else colors.error
                                                )
                                            }
                                        }

                                        if (!itemResult.isCorrect && itemResult.correctOptionText.isNotBlank()) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RectangleShape,
                                                color = colors.surface2,
                                                border = BorderStroke(1.dp, Color(0xFF22C55E))
                                            ) {
                                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color(0xFF22C55E),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "CORRECT: ${itemResult.correctOptionText}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF22C55E)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                val explanation = fullQ?.rationale ?: itemResult.rationale
                                if (!explanation.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RectangleShape,
                                        color = colors.surface2,
                                        border = BorderStroke(1.dp, colors.border)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "EXPLANATION & MARITIME RATIONALE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colors.primary,
                                                fontSize = 10.sp,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = explanation,
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

            // Bottom Actions
            item {
                Spacer(modifier = Modifier.height(10.dp))

                val retakeButtonText = when {
                    isMockExam -> "RETAKE MOCK EXAM"
                    !result.drillTitle.isNullOrBlank() -> "RETAKE ${result.drillTitle}"
                    compId != null && partLabel != null -> "RETAKE $compId ${partLabel.uppercase()}"
                    compId != null -> "RETAKE $compId DRILL"
                    result.mode == QuizMode.VAULT_DRILL || result.mode == QuizMode.MISTAKES -> "RETAKE MISTAKES DRILL"
                    result.mode == QuizMode.BOOKMARKS_DRILL -> "RETAKE BOOKMARKS DRILL"
                    else -> "RETAKE PRACTICE DRILL"
                }

                MianButton(
                    onClick = {
                        if (isMockExam) {
                            viewModel.startTimedMockExam(viewModel.selectedTrack.value)
                        } else {
                            viewModel.retakePracticeDrill()
                        }
                    },
                    accent = true,
                    modifier = Modifier.fillMaxWidth().testTag("retake_action_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retake",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = retakeButtonText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                if (!isMockExam) {
                    Spacer(modifier = Modifier.height(8.dp))

                    MianRuleButton(
                        onClick = {
                            viewModel.navigateTo(
                                Screen.PracticeConfig(
                                    competenceId = compId,
                                    isCrpMode = isCrpMode,
                                    track = viewModel.selectedTrack.value
                                )
                            )
                        },
                        ruleColor = colors.border,
                        modifier = Modifier.fillMaxWidth().testTag("change_drill_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Change Settings",
                            tint = colors.foreground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CHANGE DRILL SETTINGS",
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                MianRuleButton(
                    onClick = { viewModel.finishExamToHome() },
                    ruleColor = colors.border,
                    modifier = Modifier.fillMaxWidth().testTag("return_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = colors.foreground,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RETURN TO DASHBOARD",
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ResultsFilterChip(
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
