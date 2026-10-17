package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.model.LegalAgreementText
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianButton
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.MianSectionHeader
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel

/**
 * Terms and Conditions Screen:
 * Official app policies, non-affiliation disclosures, and mandatory Google Play subscription billing terms.
 */
@Composable
fun TermsOnboardingScreen(
    onAcceptTerms: () -> Unit,
    modifier: Modifier = Modifier
) {
    TermsAndDisclaimerScreenContent(
        isMandatoryOnboarding = true,
        onBackClick = {},
        onAccepted = onAcceptTerms,
        modifier = modifier
    )
}

@Composable
fun TermsOnboardingScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    TermsOnboardingScreen(
        onAcceptTerms = { viewModel.acceptTerms() },
        modifier = modifier
    )
}

@Composable
fun TermsAndDisclaimerScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    modifier: Modifier = Modifier
) {
    TermsAndDisclaimerScreenContent(
        isMandatoryOnboarding = false,
        onBackClick = { viewModel.navigateBack() },
        onOpenDrawer = onOpenDrawer,
        onAccepted = {},
        modifier = modifier
    )
}

@Composable
fun TermsAndDisclaimerScreen(
    onBackClick: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TermsAndDisclaimerScreenContent(
        isMandatoryOnboarding = false,
        onBackClick = onBackClick,
        onOpenDrawer = onOpenDrawer,
        onAccepted = {},
        modifier = modifier
    )
}

@Composable
fun TermsAndDisclaimerScreen(
    viewModel: ReviewerViewModel,
    isMandatoryOnboarding: Boolean = false,
    onAccepted: () -> Unit = {},
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    modifier: Modifier = Modifier
) {
    TermsAndDisclaimerScreenContent(
        isMandatoryOnboarding = isMandatoryOnboarding,
        onBackClick = { viewModel.navigateBack() },
        onOpenDrawer = onOpenDrawer,
        onAccepted = onAccepted,
        modifier = modifier
    )
}

@Composable
fun TermsAndDisclaimerScreenContent(
    isMandatoryOnboarding: Boolean = false,
    onBackClick: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onAccepted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    var isAccepted by rememberSaveable { mutableStateOf(!isMandatoryOnboarding) }
    var hasFullyScrolled by rememberSaveable { mutableStateOf(!isMandatoryOnboarding) }
    val scrollState = rememberScrollState()

    val hasReachedBottom by remember {
        derivedStateOf {
            !scrollState.canScrollForward || (scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 20)
        }
    }

    LaunchedEffect(hasReachedBottom) {
        if (hasReachedBottom) {
            hasFullyScrolled = true
        }
    }

    BackHandler(enabled = !isMandatoryOnboarding) {
        onBackClick()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (!isMandatoryOnboarding) {
            MaritimeTopBar(
                title = "TERMS AND CONDITIONS",
                subtitle = "OFFICIAL APP POLICIES & SUBSCRIPTION TERMS",
                showBackButton = false,
                onMenuClick = onOpenDrawer
            )
        } else {
            // Mandatory Onboarding Top Header
            Surface(
                color = colors.surface,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Column {
                            Text(
                                text = "HYPERION",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "TERMS AND CONDITIONS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.accent,
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.accent)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Prominent Non-Affiliation Disclaimer
            MianRuleCard(
                ruleColor = colors.accent,
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "OFFICIAL NON-AFFILIATION NOTICE",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accent,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = LegalAgreementText.DISCLAIMER_SUMMARY,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        lineHeight = 18.sp
                    )
                }
            }

            MianSectionHeader(title = "1. PURPOSE & INDEPENDENT NATURE")

            MianRuleCard(
                ruleColor = colors.border,
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LegalAgreementText.SECTION_1_PURPOSE,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        lineHeight = 18.sp
                    )
                }
            }

            MianSectionHeader(title = "2. INTELLECTUAL PROPERTY & COMPETENCY REFERENCES")

            MianRuleCard(
                ruleColor = colors.border,
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LegalAgreementText.SECTION_2_IP,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        lineHeight = 18.sp
                    )
                }
            }

            MianSectionHeader(title = "3. LIMITATION OF LIABILITY & PRIVACY")

            MianRuleCard(
                ruleColor = colors.border,
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LegalAgreementText.SECTION_3_LIABILITY,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        lineHeight = 18.sp
                    )
                }
            }

MianSectionHeader(title = "4. SUBSCRIPTION BILLING & AUTO-RENEWAL POLICY")

            MianRuleCard(
                ruleColor = colors.accent,
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GOOGLE PLAY SUBSCRIPTION DISCLOSURE",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accent,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = LegalAgreementText.SECTION_4_SUBSCRIPTION,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        lineHeight = 19.sp
                    )
                }
            }

            MianSectionHeader(title = "5. OFFLINE ACCESS & HARDWARE INTEGRITY")

            MianRuleCard(
                ruleColor = colors.border,
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OFFLINE LICENSING & MARITIME PERSISTENCE",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accent,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Offline activation passes (1-Week, 1-Month, 1-Year, Custom) are cryptographically bound to the unique hardware token of this terminal and are non-transferable. Google Play subscriptions and offline passes maintain local persistence to guarantee uninterrupted study access during deep-sea voyages without active internet connectivity. System time manipulation or clock tampering triggers security latches requiring system time restoration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        lineHeight = 18.sp
                    )
                }
            }

            if (isMandatoryOnboarding && !hasFullyScrolled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Scroll down",
                        tint = colors.muted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PLEASE SCROLL TO BOTTOM TO ACCEPT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.muted,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Acceptance Bar (Mandatory Onboarding Mode)
        if (isMandatoryOnboarding) {
            Surface(
                color = colors.surface,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.border)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            if (!hasFullyScrolled) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = colors.surface2
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "↓ Please scroll to the bottom to accept the terms",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accent,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = colors.surface2
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = colors.success,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Terms reviewed",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.success,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isAccepted,
                                onCheckedChange = {
                                    if (hasFullyScrolled) {
                                        isAccepted = it
                                    }
                                },
                                enabled = hasFullyScrolled,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.accent,
                                    checkmarkColor = colors.accentForeground,
                                    disabledCheckedColor = colors.border,
                                    disabledUncheckedColor = colors.border
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "I understand and accept the Terms and Conditions, privacy policy, and subscription billing terms.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasFullyScrolled) colors.foreground else colors.muted,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = hasFullyScrolled) {
                                        isAccepted = !isAccepted
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        MianButton(
                            onClick = onAccepted,
                            enabled = hasFullyScrolled && isAccepted,
                            accent = true,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ACCEPT & ENTER REVIEWER",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
