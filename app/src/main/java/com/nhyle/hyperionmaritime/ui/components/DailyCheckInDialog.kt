package com.nhyle.hyperionmaritime.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nhyle.hyperionmaritime.data.hype.HypePassManager
import com.nhyle.hyperionmaritime.ui.store.rememberStoreColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel

/**
 * Tactical Terminal HUD Daily Study Check-in Popup Dialog.
 *
 * STRICT SHAPE RULE ENFORCEMENT:
 * Every container, button, card, border, and badge is strictly 0dp corner radius (RectangleShape).
 * No rounded corners or pill shapes.
 */
@Composable
fun DailyCheckInDialog(
    viewModel: ReviewerViewModel,
    onDismiss: () -> Unit
) {
    val isDarkModeState by viewModel.isDarkMode.collectAsState()
    val isDark = isDarkModeState
    val colors = rememberStoreColors(isDark)

    val hypeBalance by viewModel.hypeBalance.collectAsState()
    val streakCount by viewModel.rollingStreak.collectAsState()
    val hasCheckedInToday by viewModel.hasCheckedInToday.collectAsState()
    val streakBroken by viewModel.streakBroken.collectAsState()
    val timeUntilResetMs by viewModel.timeUntilResetMs.collectAsState()
    val checkInFeedback by viewModel.checkInFeedbackMessage.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Outer Terminal HUD Box with 1dp border and RectangleShape
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .border(BorderStroke(1.dp, colors.borderInactive), RectangleShape)
                .testTag("daily_checkin_dialog_surface"),
            shape = RectangleShape,
            color = colors.cardBackground,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar with Title and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(colors.accentGreen, RectangleShape)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "TERMINAL HUD",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = if (colors.isDark) Color.Black else Color.White
                                )
                            }
                            Text(
                                text = "DAILY CHECK-IN",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp
                                ),
                                color = colors.textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("daily_checkin_dismiss_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hype Balance HUD Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.boxInactiveFill, RectangleShape)
                        .border(1.dp, colors.borderInactive, RectangleShape)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AVAILABLE BALANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = colors.textSecondary
                            )
                            Text(
                                text = "$hypeBalance HYPE",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                ),
                                color = colors.accentGreen
                            )
                        }

                        Box(
                            modifier = Modifier
                                .border(1.dp, colors.accentGreen.copy(alpha = 0.6f), RectangleShape)
                                .background(colors.accentGreen.copy(alpha = 0.12f), RectangleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "DEVICE-ONLY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = colors.accentGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sad Face Banner if Streak was Lost
                AnimatedVisibility(
                    visible = streakBroken,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.failRed.copy(alpha = 0.12f), RectangleShape)
                            .border(1.dp, colors.failRed, RectangleShape)
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "(×_×)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = colors.failRed
                            )
                            Column {
                                Text(
                                    text = "STREAK LOST. START AGAIN TODAY.",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    ),
                                    color = colors.failRed
                                )
                                Text(
                                    text = "Missed calendar day detected. Check in now to begin Day 1.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp
                                    ),
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                if (streakBroken) {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 7-Slot Rolling Progress Row Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROLLING 7-DAY STREAK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        ),
                        color = colors.textPrimary
                    )

                    Text(
                        text = "7TH DAY AWARDS +20 HYPE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = colors.accentGreen
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 7-Slot Progress Box Row (Strict RectangleShape, 0dp Radius)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (slotIndex in 1..7) {
                        val isChecked = slotIndex <= streakCount
                        val isTodayTarget = if (hasCheckedInToday) {
                            slotIndex == streakCount
                        } else {
                            slotIndex == (streakCount + 1).coerceAtMost(7)
                        }
                        val isRewardDay = slotIndex == 7

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .background(
                                    when {
                                        isChecked -> colors.accentGreen.copy(alpha = 0.15f)
                                        isTodayTarget -> colors.boxInactiveFill
                                        else -> colors.cardBackground
                                    },
                                    RectangleShape
                                )
                                .border(
                                    BorderStroke(
                                        if (isTodayTarget) 1.5.dp else 1.dp,
                                        when {
                                            isChecked -> colors.accentGreen
                                            isTodayTarget -> colors.accentGreen.copy(alpha = 0.8f)
                                            isRewardDay -> colors.accentGreen.copy(alpha = 0.4f)
                                            else -> colors.borderInactive
                                        }
                                    ),
                                    RectangleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 1.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DAY $slotIndex",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isChecked || isTodayTarget) colors.textPrimary else colors.textMuted,
                                    maxLines = 1,
                                    softWrap = false
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                if (isRewardDay) {
                                    Text(
                                        text = "+20 HYPE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = Color(0xFF00FF66),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                } else if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Checked",
                                        tint = colors.accentGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else if (isTodayTarget) {
                                    Text(
                                        text = "TODAY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp
                                        ),
                                        color = colors.accentGreen,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                } else {
                                    Text(
                                        text = "-",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Feedback message if check-in was clicked
                if (checkInFeedback != null) {
                    Text(
                        text = checkInFeedback ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.accentGreen,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Check-in or Dismiss Actions
                if (!hasCheckedInToday) {
                    Button(
                        onClick = {
                            viewModel.performDailyCheckIn()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("popup_checkin_button"),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentGreen,
                            contentColor = if (colors.isDark) Color.Black else Color.White
                        )
                    ) {
                        Text(
                            text = "CHECK IN NOW",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("popup_dismiss_button"),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, colors.borderInactive),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.textSecondary
                        )
                    ) {
                        Text(
                            text = "LATER",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                } else {
                    // Already checked in today state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.boxInactiveFill, RectangleShape)
                            .border(1.dp, colors.borderInactive, RectangleShape)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CHECKED IN TODAY",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = colors.accentGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "NEXT RESET IN ${HypePassManager.formatCountdown(timeUntilResetMs).uppercase()}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("popup_got_it_button"),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.boxInactiveFill,
                            contentColor = colors.textPrimary
                        ),
                        border = BorderStroke(1.dp, colors.borderInactive)
                    ) {
                        Text(
                            text = "GOT IT",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
