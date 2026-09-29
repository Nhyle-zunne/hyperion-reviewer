package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Key
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
import com.example.data.util.LicenseManager
import com.example.data.util.ProTier
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.MianButton
import com.example.ui.components.MianRuleCard
import com.example.ui.components.MianSectionHeader
import com.example.ui.theme.LocalSpaghettiColors
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" Account & Activation Help Screen:
 * Hardware binding overview, 3-step registration process, tier breakdown, support channels.
 */
@Composable
fun HowToAvailScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val context = LocalContext.current
    val isOicNwProUnlocked by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssProUnlocked by viewModel.isGmdssProUnlocked.collectAsState()
    val isHyperionProUnlocked by viewModel.isHyperionProUnlocked.collectAsState()

    val hardwareId = rememberHardwareId(context)
    val installSessionId = rememberSessionId(context)

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
            showBackButton = true,
            onBackClick = { viewModel.navigateBack() }
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
                            text = "HOW TO ACTIVATE HYPERION ALL ACCESS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            letterSpacing = (-0.2).sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Hyperion licenses are cryptographically paired to your physical device terminal. Follow the 3 steps below to register your device and obtain your unique activation key.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                MianSectionHeader(title = "DEVICE REGISTRATION STEPS")
            }

            item {
                StepCard(
                    stepNumber = "01",
                    title = "COPY YOUR DEVICE REQUEST CODE",
                    description = "Open the Device Registration screen to copy your unique terminal Request Code:",
                    extraInfo = "Hardware ID: $hardwareId • Session: $installSessionId"
                )
            }

            item {
                StepCard(
                    stepNumber = "02",
                    title = "TRANSMIT CODE TO SUPPORT",
                    description = "Send your Request Code along with your examinee details via official support channels to request your activation key.",
                    extraInfo = "Response time: Typically within minutes during active examination season."
                )
            }

            item {
                com.example.ui.components.LuminousDeskSupportButton(
                    title = "OPEN SUPPORT DESK ↗",
                    subtitle = "Direct candidate identity verification & seat clearance"
                )
            }

            item {
                StepCard(
                    stepNumber = "03",
                    title = "ENTER KEY & UNLOCK TERMINAL",
                    description = "Paste your 16-character Activation Key in the registration panel to immediately unlock full offline question banks and timed mock exams.",
                    extraInfo = "Key format: XXXX - XXXX - XXXX - XXXX"
                )
            }

            item {
                MianSectionHeader(title = "LICENSE TIERS")
            }

            item {
                TierCard(
                    title = "OIC-NW DECK (OPERATIONAL LEVEL)",
                    subtitle = "STCW Regulation II/1 • 12 Competencies, 200 items timed mock, full rationale",
                    isUnlocked = isOicNwProUnlocked,
                    codePrefix = "HYP-AUTH-OIC"
                )
            }

            item {
                TierCard(
                    title = "GMDSS GOC (RADIO OPERATOR)",
                    subtitle = "STCW Regulation IV/2 • 2 Competencies, 100 items timed mock, full rationale",
                    isUnlocked = isGmdssProUnlocked,
                    codePrefix = "HYP-AUTH-GOC"
                )
            }

            item {
                TierCard(
                    title = "HYPERION ALL ACCESS",
                    subtitle = "Complete Deck Operational + GMDSS Radio Reviewer package with all competencies",
                    isUnlocked = isHyperionProUnlocked,
                    codePrefix = "HYP-AUTH-ALL"
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))

                MianButton(
                    onClick = { viewModel.navigateTo(Screen.Activation) },
                    accent = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GO TO REGISTRATION & KEY INPUT",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun rememberHardwareId(context: android.content.Context): String {
    return androidx.compose.runtime.remember { LicenseManager.getPermanentHardwareId(context) }
}

@Composable
private fun rememberSessionId(context: android.content.Context): String {
    return androidx.compose.runtime.remember { LicenseManager.getInstallSessionId(context) }
}

@Composable
private fun StepCard(
    stepNumber: String,
    title: String,
    description: String,
    extraInfo: String
) {
    val colors = LocalSpaghettiColors.current

    MianRuleCard(
        ruleColor = colors.border,
        containerColor = colors.surface
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    letterSpacing = (-0.1).sp
                )
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
                    color = colors.muted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TierCard(
    title: String,
    subtitle: String,
    isUnlocked: Boolean,
    codePrefix: String
) {
    val colors = LocalSpaghettiColors.current

    MianRuleCard(
        ruleColor = if (isUnlocked) colors.success else colors.border,
        containerColor = colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = colors.surface2
                ) {
                    Text(
                        text = if (isUnlocked) "ACTIVE" else "LOCKED",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isUnlocked) colors.success else colors.muted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Request Code Prefix: $codePrefix",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.foreground,
                fontSize = 10.sp
            )
        }
    }
}
