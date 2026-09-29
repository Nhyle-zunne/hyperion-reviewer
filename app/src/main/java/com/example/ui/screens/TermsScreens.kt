package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LegalAgreementText
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.MianButton
import com.example.ui.components.MianRuleCard
import com.example.ui.components.MianSectionHeader
import com.example.ui.theme.LocalSpaghettiColors
import com.example.viewmodel.ReviewerViewModel

/**
 * Editorial MIAN "Spaghetti" Terms & Academic Disclaimer Screen:
 * Independent developer disclaimer, STCW/MARINA compliance notice, mandatory acceptance modal.
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
    modifier: Modifier = Modifier
) {
    TermsAndDisclaimerScreenContent(
        isMandatoryOnboarding = false,
        onBackClick = { viewModel.navigateBack() },
        onAccepted = {},
        modifier = modifier
    )
}

@Composable
fun TermsAndDisclaimerScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TermsAndDisclaimerScreenContent(
        isMandatoryOnboarding = false,
        onBackClick = onBackClick,
        onAccepted = {},
        modifier = modifier
    )
}

@Composable
fun TermsAndDisclaimerScreen(
    viewModel: ReviewerViewModel,
    isMandatoryOnboarding: Boolean = false,
    onAccepted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TermsAndDisclaimerScreenContent(
        isMandatoryOnboarding = isMandatoryOnboarding,
        onBackClick = { viewModel.navigateBack() },
        onAccepted = onAccepted,
        modifier = modifier
    )
}

@Composable
fun TermsAndDisclaimerScreenContent(
    isMandatoryOnboarding: Boolean = false,
    onBackClick: () -> Unit = {},
    onAccepted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    var isAccepted by remember { mutableStateOf(!isMandatoryOnboarding) }
    var hasScrolledToBottom by remember { mutableStateOf(!isMandatoryOnboarding) }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0 && scrollState.value >= (scrollState.maxValue - 80)) {
            hasScrolledToBottom = true
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
                title = "TERMS & DISCLAIMER",
                subtitle = "INDEPENDENT ACADEMIC COMPLIANCE",
                showBackButton = true,
                onBackClick = onBackClick
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
                                text = "TERMS OF USE & ACADEMIC DISCLAIMER",
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

            MianSectionHeader(title = "2. INTELLECTUAL PROPERTY & STCW REFERENCES")

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

            if (isMandatoryOnboarding && !hasScrolledToBottom) {
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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isAccepted,
                                onCheckedChange = { isAccepted = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.accent,
                                    checkmarkColor = colors.accentForeground
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "I understand and accept that Hyperion is an independent supplementary reviewer and is not affiliated with MARINA.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.foreground,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        MianButton(
                            onClick = onAccepted,
                            enabled = isAccepted && hasScrolledToBottom,
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
