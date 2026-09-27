package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LegalAgreementText
import com.example.ui.components.HyperionEmblem
import com.example.ui.components.MaritimeTopBar
import com.example.ui.theme.MaritimeGold
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan

/**
 * First-launch mandatory terms and academic disclaimer gate.
 * The examinee must scroll completely to the bottom of the formal agreement
 * before the acceptance checkbox is enabled.
 */
@Composable
fun TermsOnboardingScreen(
    onAcceptTerms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var isAccepted by remember { mutableStateOf(false) }

    // Scroll-to-bottom enforcement
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0 && scrollState.value >= (scrollState.maxValue - 24)) {
            hasScrolledToBottom = true
        } else if (scrollState.maxValue == 0) {
            hasScrolledToBottom = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Formal Header Banner
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    HyperionEmblem(size = 28.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "HYPERION",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaritimeGoldBright,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Terms of Service & Academic Disclaimer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )

                Text(
                    text = "Mandatory First-Launch Compliance Review",
                    style = MaterialTheme.typography.labelSmall,
                    color = SeaCyan
                )
            }
        }

        // Compliance Highlights Banner
        Surface(
            color = Color(0xFF0F1E36),
            border = BorderStroke(1.dp, MaritimeGoldBright.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaritimeGoldBright,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Independent study reviewer • Not affiliated with MARINA or IMO • Synthetic practice questions only",
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
            }
        }

        // Scrollable Legal Text Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag("terms_scroll_container")
            ) {
                Text(
                    text = LegalAgreementText.TITLE,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaritimeGoldBright,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = LegalAgreementText.LAST_REVISED,
                    style = MaterialTheme.typography.labelSmall,
                    color = SeaCyan,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = LegalAgreementText.FULL_AGREEMENT_TEXT,
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite.copy(alpha = 0.9f),
                            lineHeight = 20.sp,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PassGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "End of Legal Agreement",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PassGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Sticky Bottom Acceptance Panel
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Scroll helper notice
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (hasScrolledToBottom) PassGreen.copy(alpha = 0.15f) else MaritimeGold.copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        if (hasScrolledToBottom) PassGreen.copy(alpha = 0.5f) else MaritimeGoldBright.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasScrolledToBottom) Icons.Default.CheckCircle else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (hasScrolledToBottom) PassGreen else MaritimeGoldBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasScrolledToBottom) {
                                "✓ Document read to bottom. You may now acknowledge acceptance."
                            } else {
                                "↓ Scroll to the very end of the agreement to enable acknowledgment"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (hasScrolledToBottom) PassGreen else MaritimeGoldBright,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Checkbox: strictly disabled until scrolled to the very end
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = hasScrolledToBottom) {
                            if (hasScrolledToBottom) {
                                isAccepted = !isAccepted
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAccepted,
                        onCheckedChange = { if (hasScrolledToBottom) isAccepted = it },
                        enabled = hasScrolledToBottom,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaritimeGoldBright,
                            checkmarkColor = Color.Black,
                            uncheckedColor = if (hasScrolledToBottom) MaritimeGoldBright else Color.Gray,
                            disabledCheckedColor = MaritimeGoldBright.copy(alpha = 0.5f),
                            disabledUncheckedColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("terms_acceptance_checkbox")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I have read, understood, and accept the Terms & Conditions and Academic Disclaimer",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasScrolledToBottom) PureWhite else PureWhite.copy(alpha = 0.45f),
                        fontWeight = if (isAccepted) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Continue button
                Button(
                    onClick = onAcceptTerms,
                    enabled = hasScrolledToBottom && isAccepted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("continue_to_hyperion_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaritimeGoldBright,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF262626),
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue to Hyperion",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Permanent Read-Only Viewer for Terms & Conditions, Academic Disclaimer, and Data Privacy.
 * Accessible anytime from the top bar or home screen footer without demanding re-acceptance.
 */
@Composable
fun TermsAndDisclaimerScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        MaritimeTopBar(
            title = "Terms & Disclaimer",
            subtitle = "Legal Agreement & Privacy",
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                border = BorderStroke(1.5.dp, MaritimeGoldBright)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HyperionEmblem(size = 32.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HYPERION",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaritimeGoldBright,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Maritime Licensure Reviewer",
                                style = MaterialTheme.typography.labelSmall,
                                color = PureWhite.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SeaCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Aligned with MARINA & STCW Competency Standards",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SeaCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "This document sets forth the formal terms of service, academic disclaimer, non-affiliation declaration, and data privacy policy for users of Hyperion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Key Regulatory Declarations
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "KEY COMPLIANCE PRINCIPLES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaritimeGoldBright,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ComplianceBulletItem(
                        icon = Icons.Default.Shield,
                        title = "Independent Status",
                        description = "Not affiliated with, endorsed by, or authorized by MARINA, IMO, PRC, or CHED."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ComplianceBulletItem(
                        icon = Icons.Default.Info,
                        title = "Synthetic Study Content",
                        description = "All questions, problems, and keys are synthetic educational exercises formulated solely for theoretical self-evaluation."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ComplianceBulletItem(
                        icon = Icons.Default.Lock,
                        title = "Privacy First",
                        description = "Offline-first architecture. Personal identifiers and examination records are stored strictly on-device."
                    )
                }
            }

            // Full Unabridged Text
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LegalAgreementText.TITLE,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaritimeGoldBright
                    )

                    Text(
                        text = LegalAgreementText.LAST_REVISED,
                        style = MaterialTheme.typography.labelSmall,
                        color = SeaCyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = LegalAgreementText.FULL_AGREEMENT_TEXT,
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite.copy(alpha = 0.9f),
                        lineHeight = 21.sp,
                        fontSize = 12.sp
                    )
                }
            }

            // Return Button
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaritimeGoldBright,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Return to Application",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ComplianceBulletItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SeaCyan,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = PureWhite.copy(alpha = 0.75f),
                fontSize = 11.sp
            )
        }
    }
}
