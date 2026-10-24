package com.nhyle.hyperionmaritime.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.ui.motion.HyperionDialog
import androidx.compose.ui.window.DialogProperties
import com.nhyle.hyperionmaritime.data.studytime.StudyTimeTracker
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors

/**
 * Rectangular Session Timer Box placed directly under the top bar, right-aligned:
 * - 0dp corner radius (RectangleShape)
 * - HH:MM:SS count-up display in monospaced/tabular numerals
 * - Independent 1-second ticks isolating recomposition strictly to this box
 * - "PAUSED" tag when paused
 * - Light and dark mode support with >= 4.5:1 contrast
 */
@Composable
fun SessionTimerBox(
    studyTimeTracker: StudyTimeTracker,
    modifier: Modifier = Modifier
) {
    val timeText by studyTimeTracker.elapsedTimeString.collectAsState()
    val isPaused by studyTimeTracker.isPaused.collectAsState()
    val colors = LocalSpaghettiColors.current
    val isDark = colors.isDark

    val boxBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val boxBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val timeColor = if (isPaused) {
        if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    } else {
        if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
    }
    val pausedTagBg = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
    val pausedTagText = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            shape = RectangleShape,
            color = boxBg,
            border = BorderStroke(1.dp, boxBorder),
            modifier = Modifier.testTag("session_timer_box")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Timer indicator / icon
                if (isPaused) {
                    Box(
                        modifier = Modifier
                            .background(pausedTagBg, RectangleShape)
                            .border(1.dp, pausedTagText.copy(alpha = 0.5f), RectangleShape)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "PAUSED",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = pausedTagText,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(timeColor, RectangleShape)
                    )
                }

                // Monospaced tabular numerals
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = timeColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Modal box dialog shown on 3-minute idle timeout:
 * "ARE YOU STILL THERE?" with rectangular 0dp buttons "I'M STILL HERE" and "SAVE & EXIT".
 * Modal prevents any taps outside from passing through.
 */
@Composable
fun AreYouStillThereDialog(
    onStillHere: () -> Unit,
    onSaveAndExit: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    val isDark = colors.isDark

    val dialogBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val dialogBorder = if (isDark) Color(0xFF334155) else Color(0xFF94A3B8)
    val accentColor = if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)

    HyperionDialog(
        onDismissRequest = { /* Modal: does not dismiss on tap outside */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 400.dp)
                .background(dialogBg, RectangleShape)
                .border(1.dp, dialogBorder, RectangleShape)
                .testTag("idle_timeout_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top accent line (0dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(accentColor)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9), RectangleShape)
                            .border(1.dp, dialogBorder, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "ARE YOU STILL THERE?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.foreground,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "The study timer paused after 3 minutes of inactivity. Idle minutes are not counted towards your study time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Rectangular "I'M STILL HERE"
                    Button(
                        onClick = onStillHere,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("still_here_button"),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "I'M STILL HERE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Rectangular "SAVE & EXIT"
                    OutlinedButton(
                        onClick = onSaveAndExit,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("idle_save_exit_button"),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, dialogBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.foreground
                        )
                    ) {
                        Text(
                            text = "SAVE & EXIT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
