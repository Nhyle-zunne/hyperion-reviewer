package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.data.model.CompetenceScore
import com.example.data.model.ExamResult
import com.example.data.model.Question
import com.example.ui.components.MaritimeTopBar
import com.example.ui.theme.FailRed
import com.example.ui.theme.FailRedContainer
import com.example.ui.theme.MaritimeGold
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PassGreenContainer
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun ResultsScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val examResult by viewModel.lastExamResult.collectAsState()
    val activeQuestions by viewModel.activeQuestions.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Breakdown, 1: Question Review

    BackHandler {
        viewModel.navigateTo(Screen.Home)
    }

    if (examResult == null) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                Text("No exam results available.", color = MaterialTheme.colorScheme.onSurface)
            }
        }
        return
    }

    val result = examResult!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MaritimeTopBar(
            title = "Examination Diagnostic",
            subtitle = result.track.shortName,
            showBackButton = true,
            onBackClick = { viewModel.navigateTo(Screen.Home) },
            onLegalClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Big Status Banner
            item {
                ResultStatusBanner(result = result)
            }

            // Score Overview Card
            item {
                ScoreOverviewCard(result = result)
            }

            // Tabs: Breakdown vs Review
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaritimeGoldBright
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "TOS Breakdown",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Review Items (${result.totalQuestions})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                // Itemized Competence Breakdown Table
                item {
                    Text(
                        text = "ITEMIZED COMPETENCE SCORES",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaritimeGoldBright,
                        letterSpacing = 1.sp
                    )
                }

                items(result.competenceScores) { compScore ->
                    CompetenceResultRow(compScore = compScore)
                }

                item {
                    // MARINA Policy Footer
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Licensure Assessment Rule:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaritimeGoldBright
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Candidates must obtain at least 70.0% in every competence. A failure in any single competence requires remedial resit of that function.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Question Review Items
                items(activeQuestions) { question ->
                    val userState = result.questionStates[question.id]
                    QuestionReviewCard(question = question, userSelectedIndex = userState?.selectedIndex)
                }
            }

            // Bottom Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val missedCount = result.totalQuestions - result.correctCount
                    if (missedCount > 0) {
                        Button(
                            onClick = { viewModel.practiceMissedQuestionsFromExam() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FailRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Practice Missed",
                                tint = PureWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Practice Missed Questions ($missedCount)", color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.retakeCurrentExam() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retake",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retake Exam")
                        }

                        Button(
                            onClick = { viewModel.navigateTo(Screen.Home) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaritimeGoldBright),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStatusBanner(result: ExamResult) {
    val isPassed = result.isOverallPassed
    val bannerBg = if (isPassed) PassGreenContainer else FailRedContainer
    val bannerBorder = if (isPassed) PassGreen else FailRed
    val bannerIcon = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Dangerous
    val bannerText = if (isPassed) "PASSED LICENSURE STANDARD" else "FAILED — REMEDIAL REQUIRED"
    val subText = if (isPassed) {
        "Candidate satisfied the 70.0% threshold across all tested competences."
    } else {
        "Candidate failed to achieve the mandatory 70.0% threshold in one or more competences."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bannerBg),
        border = androidx.compose.foundation.BorderStroke(2.dp, bannerBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = bannerIcon,
                contentDescription = bannerText,
                tint = bannerBorder,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = bannerText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = PureWhite
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = PureWhite.copy(alpha = 0.9f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ScoreOverviewCard(result: ExamResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.1f%%", result.overallPercentage),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (result.overallPercentage >= 70.0) PassGreen else FailRed
                )
                Text(
                    text = "Weighted Score",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${result.rawPointsEarned}/${result.maxPointsPossible}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaritimeGoldBright
                )
                Text(
                    text = "Bloom's Points",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${result.correctCount}/${result.totalQuestions}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Items Correct",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompetenceResultRow(compScore: CompetenceScore) {
    val statusColor = if (compScore.isPassed) PassGreen else FailRed
    val statusText = if (compScore.isPassed) "PASSED" else "FAILED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = compScore.competenceId,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaritimeGoldBright,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = compScore.competenceTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Items: ${compScore.correctCount}/${compScore.questionCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Raw: ${compScore.rawPointsEarned}/${compScore.maxPointsPossible} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = String.format("%.1f%%", compScore.percentage),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun QuestionReviewCard(question: Question, userSelectedIndex: Int?) {
    var expanded by remember { mutableStateOf(false) }
    val isCorrect = userSelectedIndex == question.correctAnswerIndex
    val statusColor = if (isCorrect) PassGreen else FailRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${question.functionId} • ${question.competenceId} (${question.topic})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaritimeGoldBright,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isCorrect) "CORRECT (+${question.difficulty.points} pts)" else "MISSED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Show user answer vs correct answer
            val letters = listOf("A", "B", "C", "D")
            val userChoiceText = if (userSelectedIndex != null) "${letters[userSelectedIndex]}: ${question.options[userSelectedIndex]}" else "No answer"
            val correctChoiceText = "${letters[question.correctAnswerIndex]}: ${question.options[question.correctAnswerIndex]}"

            Text(
                text = "Your Answer: $userChoiceText",
                style = MaterialTheme.typography.bodySmall,
                color = if (isCorrect) PassGreen else FailRed
            )

            if (!isCorrect) {
                Text(
                    text = "Correct Answer: $correctChoiceText",
                    style = MaterialTheme.typography.bodySmall,
                    color = PassGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Hide Rationale" else "Show Explanation & IMO Rationale",
                    style = MaterialTheme.typography.labelSmall,
                    color = SeaCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = SeaCyan,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
