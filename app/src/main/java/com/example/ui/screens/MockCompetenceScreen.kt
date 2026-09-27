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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Question
import com.example.data.repository.MaritimeCompetenceRegistry
import com.example.ui.components.TimerChip
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun MockCompetenceScreen(
    viewModel: ReviewerViewModel,
    competenceId: String,
    modifier: Modifier = Modifier
) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val timeRemaining by viewModel.timeRemainingSeconds.collectAsState()
    val userStates by viewModel.userQuestionStates.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }

    // Back button prompts confirmation
    BackHandler {
        showExitDialog = true
    }

    val compInfo = MaritimeCompetenceRegistry.getCompetenceById(competenceId, selectedTrack)
    val compQuestions = viewModel.getQuestionsForCompetence(competenceId)

    val answeredCount = compQuestions.count { userStates[it.id]?.selectedIndex != null }
    val flaggedCount = compQuestions.count { userStates[it.id]?.isFlagged == true }
    val totalQuestions = compQuestions.size
    val progress = if (totalQuestions > 0) answeredCount.toFloat() / totalQuestions.toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar: Back to Hub + Title + Timer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showExitDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaritimeGoldBright
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${compInfo?.code ?: competenceId}: ${compInfo?.title ?: "Competence Drills"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "$answeredCount / $totalQuestions Answered" + if (flaggedCount > 0) " • $flaggedCount Flagged" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaritimeGoldBright
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TimerChip(secondsRemaining = timeRemaining)
            }
        }

        // Progress bar for this competence
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaritimeGoldBright,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Continuous Vertical Scroll List: All items on one continuous page
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continuous Scroll View: Answer, flag, and review all $totalQuestions questions freely. When finished, tap 'Submit Competence' below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            itemsIndexed(compQuestions, key = { _, q -> q.id }) { index, question ->
                val state = userStates[question.id]
                val isFlagged = state?.isFlagged == true
                val selectedIndex = state?.selectedIndex

                MockQuestionItemCard(
                    itemNumber = index + 1,
                    totalItems = totalQuestions,
                    question = question,
                    selectedIndex = selectedIndex,
                    isFlagged = isFlagged,
                    onSelectOption = { optIdx ->
                        viewModel.selectOptionForQuestion(question.id, optIdx)
                    },
                    onToggleFlag = {
                        viewModel.toggleFlagForQuestion(question.id)
                    }
                )
            }

            // End of list spacer
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Bottom Navigation Bar with Bottom-Right Competence Action: "Submit Competence"
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
                        text = "$answeredCount of $totalQuestions Answered",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (answeredCount == totalQuestions) PassGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (answeredCount == totalQuestions) "All questions answered" else "${totalQuestions - answeredCount} items remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        viewModel.markCompetenceCompleted(competenceId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PassGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Submit Competence",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text("Exit Session?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Exit Session? Your progress and answers will be saved so you can resume later.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.navigateTo(Screen.MockHub)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Exit to Hub", color = PureWhite)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }) {
                    Text("Continue")
                }
            }
        )
    }
}

/**
 * Question Card for Mock Exam:
 * - Displays item number (e.g. "#12")
 * - Question text
 * - Radio options (A, B, C, D)
 * - Flag icon
 * - STRICT RULE: Absolutely NO point values (+3, +6, +9 pts) or difficulty chips shown!
 */
@Composable
private fun MockQuestionItemCard(
    itemNumber: Int,
    totalItems: Int,
    question: Question,
    selectedIndex: Int?,
    isFlagged: Boolean,
    onSelectOption: (Int) -> Unit,
    onToggleFlag: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isFlagged) 1.5.dp else if (selectedIndex != null) 1.dp else 0.75.dp,
            color = if (isFlagged) MaritimeGoldBright else if (selectedIndex != null) SeaCyan.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Item Number badge and Flag Icon (NO POINT VALUES OR DIFFICULTY CHIPS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (selectedIndex != null) SeaCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (selectedIndex != null) SeaCyan else MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "#$itemNumber of $totalItems",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIndex != null) SeaCyan else MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onToggleFlag,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFlagged) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                        contentDescription = "Flag Item",
                        tint = if (isFlagged) MaritimeGoldBright else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Question Text
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Radio Options (A, B, C, D)
            val optionLetters = listOf("A", "B", "C", "D")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = selectedIndex == optIndex
                    val letter = optionLetters.getOrElse(optIndex) { "${optIndex + 1}" }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectOption(optIndex) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) SeaCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.75.dp,
                            color = if (isSelected) SeaCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) SeaCyan else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                lineHeight = 18.sp
                            )

                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectOption(optIndex) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = SeaCyan,
                                    unselectedColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
