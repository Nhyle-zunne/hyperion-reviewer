package com.nhyle.hyperionmaritime.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.model.CurrentQuestionStats
import com.nhyle.hyperionmaritime.ui.theme.FailRed
import com.nhyle.hyperionmaritime.ui.theme.PassGreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.UserQuestionState
import com.nhyle.hyperionmaritime.ui.theme.FailRed
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.ui.theme.PassGreen

/**
 * MIAN "Spaghetti" Italian poster section header:
 * Horizontal 3dp rule across top, uppercase micro-label, Heavy grotesk, flush left.
 */
@Composable
fun MianSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    ruleColor: Color? = null,
    ruleHeight: Dp = 3.dp
) {
    val colors = LocalSpaghettiColors.current
    val effectiveRuleColor = ruleColor ?: colors.border

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ruleHeight)
                .background(effectiveRuleColor)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = colors.foreground,
            fontSize = 11.sp,
            letterSpacing = 1.4.sp,
            textAlign = TextAlign.Start
        )
    }
}

/**
 * Universal MIAN "Rules, not boxes" Card:
 * Replaces enclosed card borders with a single 3dp top rule and explicit border.
 * Sharp RectangleShape corners, flat background, zero elevation.
 */
@Composable
fun MianRuleCard(
    modifier: Modifier = Modifier,
    ruleColor: Color? = null,
    ruleHeight: Dp = 3.dp,
    containerColor: Color? = null,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val colors = LocalSpaghettiColors.current
    val effectiveRuleColor = ruleColor ?: colors.border
    val effectiveContainerColor = containerColor ?: colors.surface
    val effectiveBorder = border ?: BorderStroke(1.5.dp, colors.border)

    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = clickableModifier.fillMaxWidth(),
        shape = RectangleShape,
        color = effectiveContainerColor,
        border = effectiveBorder,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ruleHeight)
                    .background(effectiveRuleColor)
            )
            content()
        }
    }
}

/**
 * High contrast flat button with strict rectangular edges.
 * Primary: Foreground background with dark text in dark mode / white in light mode.
 * Accent: Tomato red for critical actions.
 */
@Composable
fun MianButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalSpaghettiColors.current
    val bg = when {
        accent -> colors.accent
        else -> colors.primary
    }
    val fg = when {
        accent -> colors.accentForeground
        else -> colors.primaryForeground
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = fg,
            disabledContainerColor = colors.border,
            disabledContentColor = colors.muted
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Flat transparent button with a 3dp top rule and strict rectangular shape.
 */
@Composable
fun MianRuleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ruleColor: Color? = null,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalSpaghettiColors.current
    val effectiveRuleColor = ruleColor ?: colors.border

    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RectangleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(effectiveRuleColor)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                content()
            }
        }
    }
}

/**
 * Category & difficulty badges:
 * Uppercase, FontWeight.ExtraBold (800), size 11sp, strict square corners with 1dp border.
 */
@Composable
fun DifficultyBadge(
    difficulty: Difficulty,
    modifier: Modifier = Modifier,
    showPoints: Boolean = false
) {
    val colors = LocalSpaghettiColors.current
    val isDark = colors.isDark

    val textColor = when (difficulty) {
        Difficulty.EASY -> if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
        Difficulty.MODERATE -> if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E)
        Difficulty.DIFFICULT -> if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
    }

    val label = when (difficulty) {
        Difficulty.EASY -> if (showPoints) "EASY (+3 PTS)" else "EASY"
        Difficulty.MODERATE -> if (showPoints) "MODERATE (+6 PTS)" else "MODERATE"
        Difficulty.DIFFICULT -> if (showPoints) "DIFFICULT (+9 PTS)" else "DIFFICULT"
    }

    Surface(
        modifier = modifier,
        shape = RectangleShape,
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            fontSize = 11.sp,
            letterSpacing = 1.0.sp,
            textAlign = TextAlign.Start
        )
    }
}

/**
 * Timer chip:
 * Sharp 2dp virtually square shape, flat surface, 3dp top rule.
 */
@Composable
fun TimerChip(secondsRemaining: Int, modifier: Modifier = Modifier) {
    val colors = LocalSpaghettiColors.current
    val hours = secondsRemaining / 3600
    val minutes = (secondsRemaining % 3600) / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    val isUrgent = secondsRemaining < 600
    val ruleColor = if (isUrgent) colors.accent else colors.border

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(2.dp),
        color = colors.surface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(ruleColor)
            )
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    modifier = Modifier.size(14.dp),
                    tint = if (isUrgent) colors.accent else colors.foreground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUrgent) colors.accent else colors.foreground,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

/**
 * MIAN Option Tile for Quiz & Practice:
 * Flat surface, 0dp side borders, topped by a 3dp line.
 * Unselected: Border color (#2A2A2A)
 * Selected: Tomato Red Accent (#FF5A67 / #C8001A)
 * Evaluated: Pass Green / Fail Red
 * Badge: sharp 2dp square.
 * Option text: FontWeight.Bold (700), strict left alignment.
 */
@Composable
fun OptionCard(
    choiceIndex: Int,
    choiceText: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isEvaluated: Boolean,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    com.nhyle.hyperionmaritime.ui.screens.DrillOptionCard(
        choiceIndex = choiceIndex,
        choiceText = choiceText,
        isSelected = isSelected,
        isCorrect = isCorrect,
        isEvaluated = isEvaluated,
        onOptionSelected = onOptionSelected,
        modifier = modifier
    )
}

@Composable
fun OptionCard(
    optionLetter: String = "",
    optionText: String,
    isSelected: Boolean,
    isCorrect: Boolean?, // null if not revealing, true/false if practice/review
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    com.nhyle.hyperionmaritime.ui.screens.DrillOptionCard(
        choiceIndex = 0,
        choiceText = optionText,
        isSelected = isSelected,
        isCorrect = isCorrect == true,
        isEvaluated = isCorrect != null,
        onOptionSelected = { if (enabled) onClick() },
        modifier = modifier
    )
}

/**
 * Editorial Spaghetti Progress Bar:
 * 3-4dp height, no gradients, foreground track with tomato red fill.
 */
@Composable
fun SpaghettiProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp
) {
    val colors = LocalSpaghettiColors.current
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colors.border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = clampedProgress)
                .height(height)
                .background(colors.accent)
        )
    }
}

/**
 * Pro & Verification Status Badge:
 * 2dp square, uppercase, 11sp, bold.
 */
@Composable
fun ProStatusBadge(
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    lockedLabel: String = "UNVERIFIED",
    unlockedLabel: String = "VERIFIED"
) {
    val colors = LocalSpaghettiColors.current
    val bg = colors.surface2
    val textColor = if (isUnlocked) colors.success else colors.muted
    val borderColor = if (isUnlocked) colors.success.copy(alpha = 0.5f) else colors.border

    Surface(
        modifier = modifier,
        shape = RectangleShape,
        color = bg,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Text(
            text = (if (isUnlocked) unlockedLabel else lockedLabel).uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Start
        )
    }
}

/**
 * Question Palette Dialog:
 * Editorial poster modal with square minimal cells and 3dp top rule.
 */
@Composable
fun QuestionPaletteDialog(
    totalQuestions: Int,
    currentIndex: Int,
    userStates: Map<String, UserQuestionState>,
    bookmarkedIds: Set<String>,
    questionIds: List<String>,
    onQuestionClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RectangleShape,
            color = colors.surface,
            border = BorderStroke(1.5.dp, colors.border),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 3dp Top Rule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colors.accent)
                )

                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUESTION PALETTE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            textAlign = TextAlign.Start
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.muted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$totalQuestions TOTAL QUESTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.muted,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 44.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.height(300.dp)
                    ) {
                        itemsIndexed((0 until totalQuestions).toList()) { index, _ ->
                            val isCurrent = index == currentIndex
                            val qId = questionIds.getOrNull(index) ?: ""
                            val state = userStates[qId]
                            val isAnswered = state?.selectedIndex != null
                            val isBookmarked = bookmarkedIds.contains(qId)
                            val isFlagged = state?.isFlagged == true

                            val isSubmittedEvaluated = state?.isSubmitted == true

                            val cellBg = when {
                                isCurrent -> colors.accent
                                isSubmittedEvaluated && state?.isCorrect == true -> PassGreen
                                isSubmittedEvaluated && state?.isCorrect == false -> FailRed
                                isAnswered -> colors.surface2
                                else -> colors.surface
                            }

                            val cellTextColor = when {
                                isCurrent -> colors.accentForeground
                                isSubmittedEvaluated -> Color.White
                                isAnswered -> colors.foreground
                                else -> colors.muted
                            }

                            val cellRuleColor = when {
                                isCurrent -> colors.accent
                                isSubmittedEvaluated && state?.isCorrect == true -> PassGreen
                                isSubmittedEvaluated && state?.isCorrect == false -> FailRed
                                isAnswered -> colors.foreground
                                else -> colors.border
                            }

                            Surface(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable {
                                        onQuestionClick(index)
                                        onDismiss()
                                    },
                                shape = RectangleShape,
                                color = cellBg,
                                border = BorderStroke(1.dp, colors.border),
                                shadowElevation = 0.dp
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(cellRuleColor)
                                    )
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontWeight = if (isCurrent || isAnswered) FontWeight.ExtraBold else FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = cellTextColor
                                        )

                                        if (isFlagged) {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = "Flagged",
                                                tint = colors.accent,
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .align(Alignment.TopEnd)
                                                    .padding(2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    MianButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "CLOSE",
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionPaletteDialog(
    questionIds: List<String>,
    currentIndex: Int,
    questionStates: Map<String, UserQuestionState>,
    bookmarkedIds: Set<String> = emptySet(),
    onSelectQuestion: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    QuestionPaletteDialog(
        totalQuestions = questionIds.size,
        currentIndex = currentIndex,
        userStates = questionStates,
        bookmarkedIds = bookmarkedIds,
        questionIds = questionIds,
        onQuestionClick = onSelectQuestion,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

/**
 * Theme-aware, high-contrast Difficulty Badge for Questions:
 * - Light Mode: Container #F1F5F9, border 1.dp #CBD5E1, RectangleShape,
 *   EASY: #15803D, MODERATE: #92400E, DIFFICULT: #B91C1C.
 * - Dark Mode: Container #1E293B, border 1.dp #334155, RectangleShape,
 *   EASY: #4ADE80, MODERATE: #FBBF24, DIFFICULT: #F87171.
 */
@Composable
fun MaritimeDifficultyBadge(
    difficulty: Difficulty,
    modifier: Modifier = Modifier,
    showPoints: Boolean = false
) {
    val colors = LocalSpaghettiColors.current
    val isDark = colors.isDark

    val textColor = when (difficulty) {
        Difficulty.EASY -> if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
        Difficulty.MODERATE -> if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E)
        Difficulty.DIFFICULT -> if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
    }

    val label = when (difficulty) {
        Difficulty.EASY -> if (showPoints) "EASY (+3 PTS)" else "EASY"
        Difficulty.MODERATE -> if (showPoints) "MODERATE (+6 PTS)" else "MODERATE"
        Difficulty.DIFFICULT -> if (showPoints) "DIFFICULT (+9 PTS)" else "DIFFICULT"
    }

    Surface(
        shape = RectangleShape,
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Start
        )
    }
}

/**
 * Companion Competence / Function Header Badge:
 * Strict RectangleShape, 1.dp border, identical height and padding for seamless row alignment.
 */
@Composable
fun MaritimeCompetenceBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current

    Surface(
        shape = RectangleShape,
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = colors.foreground,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
fun QuestionAttemptStatsCard(
    stats: CurrentQuestionStats,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val totalAttempts = stats.totalAttempts

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("question_attempt_stats_card"),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ITEM MASTERY & ATTEMPTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )
                if (totalAttempts > 0) {
                    val passRate = (stats.allTimeCorrect.toFloat() / totalAttempts.toFloat() * 100).toInt()
                    Text(
                        text = "$passRate% Accuracy",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (passRate >= 70) colors.success else colors.error,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = stats.formattedSummary(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                modifier = Modifier.testTag("question_stats_summary_text")
            )

            // Attempt History Bars (Latest up to 24 attempts in chronological order)
            if (stats.recentAttempts.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .testTag("question_stats_bars_row"),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        stats.recentAttempts.forEach { isCorrect ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (isCorrect) colors.success else colors.error, RectangleShape)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (totalAttempts > 24) "Last 24 attempts (All-time: $totalAttempts)" else "Chronological attempts (${stats.recentAttempts.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "Oldest → Newest",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

