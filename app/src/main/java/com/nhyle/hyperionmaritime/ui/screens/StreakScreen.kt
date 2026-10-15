package com.nhyle.hyperionmaritime.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.nhyle.hyperionmaritime.data.hype.HypeRedemptionOption
import com.nhyle.hyperionmaritime.data.hype.RedemptionBlockReason
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.store.StoreColors
import com.nhyle.hyperionmaritime.ui.store.rememberStoreColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel

/**
 * Editorial Terminal HUD Study Streak Screen with Hype Currency and Redemption Engine.
 *
 * STRICT SHAPE RULE ENFORCEMENT:
 * Every component, card, button, tag, and dialog is strictly rectangular with 0dp corner radius (RectangleShape).
 * No rounded corners or pill shapes.
 */
@Composable
fun StreakScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    modifier: Modifier = Modifier
) {
    val isDarkModeState by viewModel.isDarkMode.collectAsState()
    val isDark = isDarkModeState
    val colors = rememberStoreColors(isDark)

    val hypeBalance by viewModel.hypeBalance.collectAsState()
    val streakCount by viewModel.rollingStreak.collectAsState()
    val hasCheckedInToday by viewModel.hasCheckedInToday.collectAsState()
    val streakBroken by viewModel.streakBroken.collectAsState()
    val activePass by viewModel.activeHypePass.collectAsState()
    val timeUntilResetMs by viewModel.timeUntilResetMs.collectAsState()
    val redemptionHistory by viewModel.hypeRedemptionHistory.collectAsState()
    val checkInFeedback by viewModel.checkInFeedbackMessage.collectAsState()

    // Active Entitlements for Blocking Evaluation
    val isOicNwProUnlocked by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssProUnlocked by viewModel.isGmdssProUnlocked.collectAsState()
    val isHyperionProUnlocked by viewModel.isHyperionProUnlocked.collectAsState()

    // Confirmation Dialog State
    var selectedOptionToRedeem by remember { mutableStateOf<HypeRedemptionOption?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.storeBackground)
    ) {
        MaritimeTopBar(
            title = "STUDY STREAK",
            subtitle = "Consistency & Hype Vault",
            showBackButton = false,
            onMenuClick = onOpenDrawer
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP HYPE BALANCE HUD PANEL
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, colors.borderInactive), RectangleShape)
                    .testTag("hype_balance_hud_card"),
                shape = RectangleShape,
                color = colors.cardBackground
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                    text = "OFFLINE VAULT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = if (colors.isDark) Color.Black else Color.White
                                )
                            }
                            Text(
                                text = "AVAILABLE BALANCE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = colors.textSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .border(1.dp, colors.borderInactive, RectangleShape)
                                .background(colors.boxInactiveFill, RectangleShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "DEVICE BOUND",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.textMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$hypeBalance",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 36.sp
                            ),
                            color = colors.accentGreen
                        )
                        Text(
                            text = "HYPE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = colors.textPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Earn +20 Hype by completing 7 consecutive daily check-ins. Redeem Hype below for temporary offline reviewer access passes.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        color = colors.textSecondary
                    )
                }
            }

            // 2. ACTIVE TEMPORARY PASS BANNER (if active)
            if (activePass != null && !activePass!!.isExpired()) {
                val pass = activePass!!
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, colors.accentGreen), RectangleShape)
                        .testTag("active_hype_pass_banner"),
                    shape = RectangleShape,
                    color = colors.accentGreen.copy(alpha = 0.08f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(colors.accentGreen, RectangleShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE PASS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = if (colors.isDark) Color.Black else Color.White
                                    )
                                }
                                Text(
                                    text = pass.rewardType.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    ),
                                    color = colors.textPrimary
                                )
                            }

                            Text(
                                text = pass.getFormattedRemainingTime().uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = colors.accentGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${pass.getFormattedExpiryDate()} • Granted via Hype Redemption (${pass.costHype} Hype)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = colors.textSecondary
                        )
                    }
                }
            }

            // 3. ROLLING 7-DAY STREAK HUD CARD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, colors.borderInactive), RectangleShape)
                    .testTag("rolling_streak_card"),
                shape = RectangleShape,
                color = colors.cardBackground
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ROLLING 7-DAY STREAK",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 11.sp
                            ),
                            color = colors.textPrimary
                        )

                        Text(
                            text = "$streakCount / 7 DAYS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            ),
                            color = colors.accentGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sad Face Broken Streak Indicator
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
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "(×_×)",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = colors.failRed
                                )
                                Column {
                                    Text(
                                        text = "STREAK LOST. START AGAIN TODAY.",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        ),
                                        color = colors.failRed
                                    )
                                    Text(
                                        text = "A full calendar day was missed. Earned Hype is never lost! Check in now to restart Day 1.",
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

                    // 7-Slot Progress Boxes Row (Strict RectangleShape, 0dp Radius)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (slot in 1..7) {
                            val isChecked = slot <= streakCount
                            val isTodayTarget = if (hasCheckedInToday) {
                                slot == streakCount
                            } else {
                                slot == (streakCount + 1).coerceAtMost(7)
                            }
                            val isRewardSlot = slot == 7

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
                                                isRewardSlot -> colors.accentGreen.copy(alpha = 0.4f)
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
                                        text = "DAY $slot",
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

                                    if (isRewardSlot) {
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

                    if (checkInFeedback != null) {
                        Text(
                            text = checkInFeedback ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.accentGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Manual Check-in Action Bar
                    if (!hasCheckedInToday) {
                        Button(
                            onClick = { viewModel.performDailyCheckIn() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("streak_manual_checkin_button"),
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accentGreen,
                                contentColor = if (colors.isDark) Color.Black else Color.White
                            )
                        ) {
                            Text(
                                text = "CHECK IN TODAY",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(colors.boxInactiveFill, RectangleShape)
                                .border(1.dp, colors.borderInactive, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.accentGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "CHECKED IN TODAY",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "• NEXT RESET IN ${HypePassManager.formatCountdown(timeUntilResetMs).uppercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 4. HYPE REDEMPTION SECTION
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HYPE REDEMPTION // ACCESS PASSES",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 11.sp
                            ),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Trade earned Hype for temporary full reviewer access. Overlapping licenses block redemption.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = colors.textSecondary
                        )
                    }
                }

                // Reward Cards
                HypePassManager.availableOptions.forEach { option ->
                    val blockReason = HypePassManager.evaluateBlockReason(
                        option = option,
                        isDeckActive = isOicNwProUnlocked,
                        isRadioActive = isGmdssProUnlocked,
                        isAllAccessActive = isHyperionProUnlocked,
                        activePass = activePass,
                        userBalance = hypeBalance
                    )

                    RedemptionRewardCard(
                        option = option,
                        blockReason = blockReason,
                        colors = colors,
                        onRedeemClick = {
                            selectedOptionToRedeem = option
                        }
                    )
                }
            }

            // 5. REDEMPTION HISTORY LOG (if any items)
            if (redemptionHistory.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, colors.borderInactive), RectangleShape)
                        .testTag("redemption_history_card"),
                    shape = RectangleShape,
                    color = colors.cardBackground
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "REDEMPTION HISTORY LOG",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 11.sp
                            ),
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            redemptionHistory.take(5).forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.boxInactiveFill, RectangleShape)
                                        .border(1.dp, colors.borderInactive, RectangleShape)
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = item.rewardName,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                ),
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = "${item.durationDays}-Day Duration • Cost: ${item.costHype} Hype",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                color = colors.textSecondary
                                            )
                                        }

                                        Text(
                                            text = item.getFormattedRedeemedDate(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp
                                            ),
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. HOW STREAKS WORK INSTRUCTION BOX
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, colors.borderInactive), RectangleShape),
                shape = RectangleShape,
                color = colors.cardBackground
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "HOW STREAKS & HYPE WORK",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• One check-in per local calendar day. Day resets at 00:00 local device time.\n" +
                               "• 7-day rolling cycle: checking in for 7 consecutive days awards +20 Hype.\n" +
                               "• On Day 7, Hype is awarded and the rolling counter restarts at Day 0.\n" +
                               "• Missing a full day breaks the streak and resets the counter to 0.\n" +
                               "• Earned Hype is never lost when a streak breaks.\n" +
                               "• Device-only vault: everything functions 100% offline at sea.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 18.sp
                        ),
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // CONFIRMATION DIALOG (Strict RectangleShape, 0dp Corner Radius)
    if (selectedOptionToRedeem != null) {
        val option = selectedOptionToRedeem!!
        val remainingAfter = hypeBalance - option.costHype

        Dialog(
            onDismissRequest = { selectedOptionToRedeem = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(BorderStroke(1.dp, colors.borderInactive), RectangleShape)
                    .testTag("redemption_confirm_dialog"),
                shape = RectangleShape,
                color = colors.cardBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "CONFIRM HYPE REDEMPTION",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 13.sp
                        ),
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.boxInactiveFill, RectangleShape)
                            .border(1.dp, colors.borderInactive, RectangleShape)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "REWARD PASS:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    ),
                                    color = colors.accentGreen
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "DURATION:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "${option.durationDays} DAYS (INSTANT ACTIVATION)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = colors.textPrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "COST:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "${option.costHype} HYPE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    ),
                                    color = colors.accentGreen
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "BALANCE AFTER:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "$remainingAfter HYPE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Once confirmed, ${option.costHype} Hype will be deducted immediately, and full access will be active for ${option.durationDays} days.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedOptionToRedeem = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("cancel_redemption_btn"),
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, colors.borderInactive),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.textSecondary
                            )
                        ) {
                            Text(
                                text = "CANCEL",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.redeemHypePass(option)
                                selectedOptionToRedeem = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("confirm_redemption_btn"),
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accentGreen,
                                contentColor = if (colors.isDark) Color.Black else Color.White
                            )
                        ) {
                            Text(
                                text = "CONFIRM",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Reward Box Card matching the StoreScreen terminal aesthetic.
 * Strict RectangleShape, 1dp border, letter-spaced labels, small status tags.
 */
@Composable
private fun RedemptionRewardCard(
    option: HypeRedemptionOption,
    blockReason: RedemptionBlockReason,
    colors: StoreColors,
    onRedeemClick: () -> Unit
) {
    val isMystery = option.isMysteryLocked && !HypePassManager.isSuperRewardUnlocked
    val isBlocked = blockReason.isBlocked

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    1.dp,
                    if (option.isRecommended && !isBlocked) colors.accentGreen.copy(alpha = 0.8f) else colors.borderInactive
                ),
                RectangleShape
            )
            .testTag("reward_card_${option.id}"),
        shape = RectangleShape,
        color = if (isMystery) colors.storeBackground else colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Tags Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (option.isRecommended) {
                        Box(
                            modifier = Modifier
                                .background(colors.accentGreen, RectangleShape)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                ),
                                color = if (colors.isDark) Color.Black else Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(colors.boxInactiveFill, RectangleShape)
                            .border(1.dp, colors.borderInactive, RectangleShape)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isMystery) "CLASSIFIED" else option.rewardType.trackTag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            ),
                            color = if (option.isRecommended) colors.accentGreen else colors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(colors.boxInactiveFill, RectangleShape)
                            .border(1.dp, colors.borderInactive, RectangleShape)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isMystery) "???" else "${option.durationDays}-DAY PASS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            ),
                            color = colors.textPrimary
                        )
                    }
                }

                // Cost Tag
                Text(
                    text = "${option.costHype} HYPE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    ),
                    color = if (isBlocked) colors.textMuted else colors.accentGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reward Name & Description
            Text(
                text = if (isMystery) "???" else option.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                ),
                color = if (isMystery) colors.textMuted else colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isMystery) {
                    "Secret tier currently locked. Cannot be redeemed."
                } else {
                    "Grants ${option.durationDays} days of full offline reviewer access for ${option.rewardType.title}."
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action / Status Button
            if (!isBlocked) {
                Button(
                    onClick = onRedeemClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("redeem_btn_${option.id}"),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentGreen,
                        contentColor = if (colors.isDark) Color.Black else Color.White
                    )
                ) {
                    Text(
                        text = "REDEEM PASS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(colors.boxInactiveFill, RectangleShape)
                        .border(1.dp, colors.borderInactive, RectangleShape)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isMystery) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = blockReason.getFormattedReason(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = colors.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
