package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Difficulty
import com.example.data.model.UserQuestionState
import com.example.ui.theme.FailRed
import com.example.ui.theme.FailRedContainer
import com.example.ui.theme.MaritimeGold
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PassGreenContainer
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan

@Composable
fun DifficultyBadge(difficulty: Difficulty, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (difficulty) {
        Difficulty.EASY -> Pair(Color(0xFF0F3E38), Color(0xFF4ADE80))
        Difficulty.MODERATE -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
        Difficulty.DIFFICULT -> Pair(Color(0xFF43161C), Color(0xFFF87171))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = "${difficulty.label}",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun TimerChip(secondsRemaining: Int, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val hours = secondsRemaining / 3600
    val minutes = (secondsRemaining % 3600) / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    val isUrgent = secondsRemaining < 600 // Less than 10 mins

    val chipBg = if (isUrgent) {
        FailRedContainer
    } else if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Color(0xFF1E293B)
    }

    val chipBorder = if (isUrgent) {
        FailRed
    } else if (isDark) {
        MaterialTheme.colorScheme.outline
    } else {
        Color(0xFFFBBF24).copy(alpha = 0.5f)
    }

    val chipAccent = if (isUrgent) {
        FailRed
    } else if (isDark) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFFBBF24)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = chipBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            chipBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Timer",
                modifier = Modifier.size(16.dp),
                tint = chipAccent
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isUrgent) PureWhite else chipAccent
            )
        }
    }
}

@Composable
fun OptionCard(
    optionLetter: String,
    optionText: String,
    isSelected: Boolean,
    isCorrect: Boolean?, // null if not revealing, true/false if practice/review
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isDark = isSystemInDarkTheme()

    val targetBorderColor = when {
        isCorrect == true -> PassGreen
        isCorrect == false && isSelected -> FailRed
        isSelected -> if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7)
        else -> if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFE2E8F0)
    }

    val targetContainerColor = when {
        isCorrect == true -> if (isDark) PassGreenContainer.copy(alpha = 0.6f) else Color(0xFFDCFCE7)
        isCorrect == false && isSelected -> if (isDark) FailRedContainer.copy(alpha = 0.6f) else Color(0xFFFEE2E2)
        isSelected -> if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFE0F2FE)
        else -> if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)
    }

    val borderColor by animateColorAsState(targetValue = targetBorderColor, label = "border_color")
    val containerColor by animateColorAsState(targetValue = targetContainerColor, label = "container_color")

    val badgeColor = when {
        isCorrect == true -> PassGreen
        isCorrect == false && isSelected -> FailRed
        isSelected -> if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7)
        else -> if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else Color(0xFFE2E8F0)
    }

    val badgeTextColor = when {
        isCorrect == true || (isCorrect == false && isSelected) || isSelected -> PureWhite
        else -> if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1E293B)
    }

    val optionTextColor = when {
        isCorrect == true -> if (isDark) PassGreen else Color(0xFF15803D)
        isCorrect == false && isSelected -> if (isDark) Color(0xFFFF6B6B) else Color(0xFFDC2626)
        isSelected -> if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0C4A6E)
        else -> if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1E293B)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected || isCorrect != null) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDark) {
                if (isSelected) 3.dp else 1.dp
            } else {
                if (isSelected) 2.dp else 0.dp
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                if (isCorrect == true) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Correct",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (isCorrect == false && isSelected) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Incorrect",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = optionLetter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = badgeTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected && !isDark) FontWeight.Bold else FontWeight.Normal,
                color = optionTextColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuestionPaletteDialog(
    totalQuestions: Int,
    currentIndex: Int,
    questionStates: Map<String, UserQuestionState>,
    questionIds: List<String>,
    onSelectQuestion: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val answeredCount = questionIds.count { questionStates[it]?.selectedIndex != null }
    val flaggedCount = questionIds.count { questionStates[it]?.isFlagged == true }
    val unansweredCount = totalQuestions - answeredCount

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question Palette",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Legend row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(label = "Answered ($answeredCount)", color = SeaCyan)
                    LegendItem(label = "Flagged ($flaggedCount)", color = MaterialTheme.colorScheme.primary)
                    LegendItem(label = "Pending ($unansweredCount)", color = MaterialTheme.colorScheme.outline)
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(questionIds) { index, qId ->
                        val state = questionStates[qId]
                        val isAnswered = state?.selectedIndex != null
                        val isFlagged = state?.isFlagged == true
                        val isCurrent = index == currentIndex

                        val itemBg = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isAnswered -> SeaCyan.copy(alpha = 0.35f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        val textColor = when {
                            isCurrent -> PureWhite
                            isAnswered -> SeaCyan
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(itemBg)
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else if (isFlagged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectQuestion(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textColor
                            )

                            if (isFlagged) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = "Flagged",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Close Palette", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
