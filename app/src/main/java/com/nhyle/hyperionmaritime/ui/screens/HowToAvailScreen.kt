package com.nhyle.hyperionmaritime.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.data.util.ProTier
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.MianSectionHeader
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" Account & Activation Help Screen:
 * Hardware binding overview, 3-step registration process, tier breakdown, support channels.
 */
@Composable
fun HowToAvailScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val context = LocalContext.current

    BackHandler {
        viewModel.navigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = "ACCOUNT & DEVICE REGISTRATION",
            subtitle = "TERMINAL ACTIVATION GUIDE",
            showBackButton = false,
            onMenuClick = onOpenDrawer
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                MianRuleCard(
                    ruleColor = colors.accent,
                    containerColor = colors.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = colors.surface2
                        ) {
                            Text(
                                text = "DEVICE HARDWARE-BOUND SECURITY",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "HOW TO GET HYPERION ACCESS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            letterSpacing = (-0.2).sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Choose a plan in the Hyperion Store, complete your purchase, and your selected access will activate automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                MianSectionHeader(title = "PURCHASE & ACCESS STEPS")
            }

            item {
                StepCard(
                    stepNumber = "01",
                    title = "OPEN HYPERION STORE",
                    description = "Open the Hyperion Store to view available OIC-NW, GMDSS, and All Access plans.",
                    extraInfo = "Tap to open store & view available plans ↗",
                    onClick = { viewModel.navigateTo(Screen.Store) },
                    isClickable = true
                )
            }

            item {
                StepCard(
                    stepNumber = "02",
                    title = "CHOOSE YOUR PLAN",
                    description = "Select your preferred weekly, monthly, or yearly subscription and complete your purchase through Google Play.",
                    extraInfo = "Supported cycles: Weekly • Monthly • Yearly (Google Play security)"
                )
            }

            item {
                StepCard(
                    stepNumber = "03",
                    title = "ACCESS UNLOCKED",
                    description = "Once your purchase is confirmed, your selected access is activated automatically and ready to use.",
                    extraInfo = "Instant automatic entitlement • Ready for immediate offline review"
                )
            }

            item {
                com.nhyle.hyperionmaritime.ui.components.LuminousDeskSupportButton(
                    title = "HAVING TROUBLES/ISSUES?",
                    subtitle = "Contact us via Facebook Messenger for quick candidate assistance & inquiries"
                )
            }


            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: String,
    title: String,
    description: String,
    extraInfo: String,
    onClick: (() -> Unit)? = null,
    isClickable: Boolean = false
) {
    val colors = LocalSpaghettiColors.current

    MianRuleCard(
        ruleColor = if (isClickable) colors.accent else colors.border,
        containerColor = colors.surface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = colors.surface2
            ) {
                Text(
                    text = stepNumber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.accent
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        letterSpacing = (-0.1).sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isClickable) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open",
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.foreground,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = extraInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isClickable) colors.accent else colors.muted,
                    fontWeight = if (isClickable) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            }
        }
    }
}
