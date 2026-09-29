package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompetenceScore
import com.example.data.model.ExamResult
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

    var selectedTab by remember { mutableStateOf(0) } // 0: Breakdown, 1: Question Review

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = "EXAMINATION RESULTS",
            subtitle = "${result.track} • MARINA EVALUATION",
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
                                shape = RoundedCornerShape(2.dp),
                                color = colors.surface2
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
                        containerColor = colors.surface
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
                                    shape = RoundedCornerShape(2.dp),
                                    color = colors.surface2
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
                        }
                    }
                }
            } else {
                // TAB 1: QUESTION REVIEW
                item {
                    MianSectionHeader(title = "FULL ITEM-BY-ITEM AUDIT")
                }

                items(result.questionResults) { itemResult ->
                    var isExpanded by remember { mutableStateOf(false) }
                    val itemColor = if (itemResult.isCorrect) colors.success else colors.error

                    MianRuleCard(
                        ruleColor = itemColor,
                        containerColor = colors.surface,
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (itemResult.isCorrect) colors.success else colors.accent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (itemResult.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = colors.primaryForeground,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = "${itemResult.competenceId} • ITEM ${itemResult.questionId}".uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.foreground,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = colors.muted
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = itemResult.questionText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.foreground,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 2
                            )

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    Text(
                                        text = "YOUR ANSWER: ${itemResult.selectedOptionText ?: "Unanswered"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (itemResult.isCorrect) colors.success else colors.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!itemResult.isCorrect) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "CORRECT ANSWER: ${itemResult.correctOptionText}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.success,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!itemResult.rationale.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "RATIONALE: ${itemResult.rationale}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.muted
                                        )
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

                MianButton(
                    onClick = { viewModel.startTimedMockExam(viewModel.selectedTrack.value) },
                    accent = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retake",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RETAKE MOCK EXAM",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                MianRuleButton(
                    onClick = { viewModel.finishExamToHome() },
                    ruleColor = colors.border,
                    modifier = Modifier.fillMaxWidth()
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
