package com.nhyle.hyperionmaritime.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Compact, zero-jitter industrial option card with circular radio indicator.
 * Eliminates letter badges ("A", "B", "C", "D") and locks dimensions across
 * Default, Selected, and Evaluated states.
 */
@Composable
fun DrillOptionCard(
    choiceIndex: Int,
    choiceText: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isEvaluated: Boolean,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showTrailingStatus: Boolean = true
) {
    val colors = com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors.current
    val isDark = colors.isDark
    val neutralBorder = colors.border
    val defaultBg = colors.surface
    val selectedPreBg = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
    val defaultTextColor = colors.foreground
    val unselectedRadioBorder = colors.muted

    val evalCorrectBorder = if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)
    val evalCorrectBg = if (isDark) Color(0xFF22C55E).copy(alpha = 0.15f) else Color(0xFFDCFCE7)
    val evalCorrectText = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
    val evalCorrectIcon = if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)

    val evalWrongBorder = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
    val evalWrongBg = if (isDark) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFFFEE2E2)
    val evalWrongText = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
    val evalWrongIcon = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)

    val selectHighlightBorder = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp) // Uniform compact baseline height
            .clickable(enabled = !isEvaluated) {
                onOptionSelected(choiceIndex)
            }
            .testTag("option_card_$choiceIndex"),
        shape = RectangleShape,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isEvaluated && isSelected && !isCorrect -> evalWrongBorder
                isEvaluated && isCorrect -> evalCorrectBorder
                !isEvaluated && isSelected -> selectHighlightBorder
                else -> neutralBorder
            }
        ),
        color = when {
            isEvaluated && isSelected && !isCorrect -> evalWrongBg
            isEvaluated && isCorrect -> evalCorrectBg
            !isEvaluated && isSelected -> selectedPreBg
            else -> defaultBg
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp), // Fixed vertical padding that never changes
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Radio Indicator (Fixed 20.dp dimension across all states)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(
                        width = 2.dp,
                        color = when {
                            isEvaluated && isSelected && !isCorrect -> evalWrongBorder
                            isEvaluated && isCorrect -> evalCorrectBorder
                            isSelected -> selectHighlightBorder
                            else -> unselectedRadioBorder
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected || (isEvaluated && isCorrect)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = when {
                                    isEvaluated && isSelected && !isCorrect -> evalWrongBorder
                                    isEvaluated && isCorrect -> evalCorrectBorder
                                    else -> selectHighlightBorder
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Option Text
            Text(
                text = choiceText,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isEvaluated && isSelected && !isCorrect -> evalWrongText
                    isEvaluated && isCorrect -> evalCorrectText
                    else -> defaultTextColor
                },
                modifier = Modifier.weight(1f)
            )

            // Optional trailing status indicator (Check/Cross icon, fixed 20.dp box to prevent shifts)
            if (showTrailingStatus) {
                if (isEvaluated) {
                    if (isCorrect) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Correct",
                            tint = evalCorrectIcon,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Incorrect",
                            tint = evalWrongIcon,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(20.dp)) // Equal placeholder prevents text reflow
                    }
                } else {
                    Spacer(modifier = Modifier.size(20.dp)) // Equal placeholder prevents text reflow
                }
            }
        }
    }
}
