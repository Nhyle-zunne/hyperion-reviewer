package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Difficulty
import com.example.data.model.Track
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.OptionCard
import com.example.ui.theme.FailRed
import com.example.ui.theme.MaritimeGold
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun FreePracticeScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val activeQuestions by viewModel.activeQuestions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val userStates by viewModel.userQuestionStates.collectAsState()
    val isActivated by viewModel.isActivated.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()

    BackHandler {
        viewModel.navigateTo(Screen.Home)
    }

    if (activeQuestions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "Initializing Free Practice sample items...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PureWhite
                )
                Button(
                    onClick = { viewModel.startFreePractice() },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaCyan)
                ) {
                    Text("Load Sample Questions", color = Color.Black)
                }
            }
        }
        return
    }

    val question = activeQuestions[currentIndex]
    val currentState = userStates[question.id]
    val isSubmitted = currentState?.selectedIndex != null
    val isBookmarked = bookmarkedIds.contains(question.id)
    val isLastQuestion = currentIndex >= activeQuestions.size - 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        val isGmdss = activeQuestions.firstOrNull()?.track == Track.GMDSS_GOC
        val totalPoolText = if (isGmdss) "1,200 Items Bank" else "2,065 Items Bank"
        val bannerPoolText = if (isGmdss) "FREE TIER • 1,200 CRP ITEMS" else "FREE TIER • 2,065 CRP ITEMS"

        MaritimeTopBar(
            title = "MARINA CRP Practice",
            subtitle = "Free Tier • $totalPoolText",
            showBackButton = true,
            onBackClick = { viewModel.navigateTo(Screen.Home) },
            onMenuClick = { viewModel.openDrawer() },
            onLegalClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
        )

        // Free Tier Header Banner
        Surface(
            color = Color(0xFF0F1E36),
            border = BorderStroke(0.5.dp, PassGreen.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = PassGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bannerPoolText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PassGreen
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PassGreen.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "NO ACTIVATION REQUIRED",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PassGreen,
                        fontSize = 9.sp
                    )
                }
            }
        }

        // Progress bar
        val currentProgress = (currentIndex + 1).toFloat() / activeQuestions.size.coerceAtLeast(1).toFloat()
        LinearProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = PassGreen,
            trackColor = Color(0xFF1E293B)
        )

        // Scrollable Question Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Prominent Legal Disclaimer Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Note: This free practice mode is independently developed based on MARINA CRP references and is not an official MARINA-endorsed examination tool.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Index & Difficulty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "Question ${currentIndex + 1} of ${activeQuestions.size} • ${question.competenceId}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (question.difficulty) {
                        Difficulty.EASY -> Color(0xFF0F3E38)
                        Difficulty.MODERATE -> Color(0xFF382D12)
                        Difficulty.DIFFICULT -> Color(0xFF43161C)
                    },
                    border = BorderStroke(
                        1.dp,
                        when (question.difficulty) {
                            Difficulty.EASY -> PassGreen
                            Difficulty.MODERATE -> MaterialTheme.colorScheme.primary
                            Difficulty.DIFFICULT -> FailRed
                        }
                    )
                ) {
                    Text(
                        text = when (question.difficulty) {
                            Difficulty.EASY -> "Easy (+3 pts)"
                            Difficulty.MODERATE -> "Moderate (+6 pts)"
                            Difficulty.DIFFICULT -> "Difficult (+9 pts)"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (question.difficulty) {
                            Difficulty.EASY -> PassGreen
                            Difficulty.MODERATE -> MaterialTheme.colorScheme.primary
                            Difficulty.DIFFICULT -> FailRed
                        }
                    )
                }
            }

            Text(
                text = "TOPIC: ${question.topic.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )

            // Question Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Options
            val optionLetters = listOf("A", "B", "C", "D")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = currentState?.selectedIndex == optIndex
                    val isCorrect = if (isSubmitted) {
                        when {
                            optIndex == question.correctAnswerIndex -> true
                            isSelected -> false
                            else -> null
                        }
                    } else null

                    OptionCard(
                        optionLetter = optionLetters.getOrElse(optIndex) { "?" },
                        optionText = optionText,
                        isSelected = isSelected,
                        isCorrect = isCorrect,
                        onClick = {
                            viewModel.selectOption(currentIndex, optIndex)
                        }
                    )
                }
            }

            // Rationale Card
            AnimatedVisibility(
                visible = isSubmitted,
                enter = fadeIn() + expandVertically()
            ) {
                val isAnswerCorrect = currentState?.selectedIndex == question.correctAnswerIndex
                val statusColor = if (isAnswerCorrect) PassGreen else FailRed
                val statusText = if (isAnswerCorrect) "CORRECT (+${question.difficulty.points} pts)" else "INCORRECT (0 pts)"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Competency Standard Reference & Rationale:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Promotion Card for Pro Tier
            if (!isActivated) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Unlock All 1,000+ Questions in Hyperion Pro",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Get Intensive Recycling Drill mode, full 200/100-question timed Mock Board Exams, and the complete competence bank.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.navigateTo(Screen.HowToAvail) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Account & Device Help", color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Navigation Footer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.previousQuestion() },
                    enabled = currentIndex > 0,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Previous")
                }

                Button(
                    onClick = {
                        if (!isLastQuestion) {
                            viewModel.nextQuestion()
                        } else {
                            viewModel.navigateTo(Screen.Home)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeaCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (!isLastQuestion) "Next Question" else "Finish Sample Drill",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isLastQuestion) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
