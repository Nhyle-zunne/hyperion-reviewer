package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Gavel
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.model.LegalAgreementText
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianButton
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

@Composable
fun TermsOnboardingScreen(
    onAcceptTerms: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TermsAndDisclaimerScreenContent(
        isMandatoryOnboarding = true,
        onBackClick = {},
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
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
        onOpenPrivacyPolicy = { viewModel.navigateTo(Screen.PrivacyPolicy) },
        modifier = modifier
    )
}

@Composable
fun TermsAndDisclaimerScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    onOpenPrivacyPolicy: () -> Unit = { viewModel.navigateTo(Screen.PrivacyPolicy) },
    modifier: Modifier = Modifier
) {
    TermsAndDisclaimerScreenContent(
        isMandatoryOnboarding = false,
        onBackClick = { viewModel.navigateBack() },
        onOpenDrawer = onOpenDrawer,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        onAccepted = {},
        modifier = modifier
    )
}

@Composable
fun TermsAndDisclaimerScreen(
    onBackClick: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TermsAndDisclaimerScreenContent(
        isMandatoryOnboarding = false,
        onBackClick = onBackClick,
        onOpenDrawer = onOpenDrawer,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
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
    onOpenPrivacyPolicy: () -> Unit = { viewModel.navigateTo(Screen.PrivacyPolicy) },
    modifier: Modifier = Modifier
) {
    TermsAndDisclaimerScreenContent(
        isMandatoryOnboarding = isMandatoryOnboarding,
        onBackClick = { viewModel.navigateBack() },
        onOpenDrawer = onOpenDrawer,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        onAccepted = onAccepted,
        modifier = modifier
    )
}

@Composable
fun TermsAndDisclaimerScreenContent(
    isMandatoryOnboarding: Boolean = false,
    onBackClick: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onAccepted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    var isAccepted by rememberSaveable { mutableStateOf(!isMandatoryOnboarding) }
    var hasFullyScrolled by rememberSaveable { mutableStateOf(!isMandatoryOnboarding) }
    val scrollState = rememberScrollState()

    val hasReachedBottom by remember {
        derivedStateOf {
            !scrollState.canScrollForward ||
                (scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 20)
        }
    }

    LaunchedEffect(hasReachedBottom) {
        if (hasReachedBottom) hasFullyScrolled = true
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
                title = "TERMS OF USE",
                subtitle = "HYPERION MARITIME · LEGAL AGREEMENT",
                showBackButton = false,
                onMenuClick = onOpenDrawer
            )
        } else {
            Surface(color = colors.surface, shadowElevation = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text = "HYPERION MARITIME",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "TERMS OF USE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accent,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp
                        )
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
                            text = "INDEPENDENT EDUCATIONAL RESOURCE",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accent,
                            letterSpacing = 0.35.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = LegalAgreementText.DISCLAIMER_SUMMARY,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        lineHeight = 19.sp
                    )
                }
            }

            MianRuleCard(
                ruleColor = colors.border,
                containerColor = colors.surface
            ) {
                Text(
                    text = LegalAgreementText.FULL_AGREEMENT_TEXT,
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.foreground,
                    lineHeight = 20.sp
                )
            }

            MianRuleCard(
                ruleColor = colors.accent,
                containerColor = colors.surface,
                onClick = onOpenPrivacyPolicy
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "READ THE PRIVACY POLICY",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accent,
                        letterSpacing = 0.55.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Review what information the Android app and website process, how account and license synchronization works, how long information is retained, and how to request account deletion.",
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

        if (isMandatoryOnboarding) {
            Surface(color = colors.surface, shadowElevation = 0.dp) {
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
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = colors.surface2
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    if (hasFullyScrolled) {
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
                                    } else {
                                        Text(
                                            text = "↓ Please scroll to the bottom to accept",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accent,
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
                                onCheckedChange = { if (hasFullyScrolled) isAccepted = it },
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
                                text = "I have read and accept the Terms of Use and acknowledge the Privacy Policy.",
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
