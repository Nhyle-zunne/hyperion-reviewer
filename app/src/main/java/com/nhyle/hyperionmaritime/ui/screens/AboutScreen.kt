package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.BuildConfig
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.MianSectionHeader
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

/**
 * Product information and the current Android app/platform capabilities.
 */
@Composable
fun AboutScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current

    BackHandler {
        viewModel.navigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = "ABOUT HYPERION",
            subtitle = "THE MARITIME LICENSURE REVIEWER",
            showBackButton = false,
            onMenuClick = onOpenDrawer
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MianRuleCard(
                ruleColor = colors.accent,
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "HYPERION MARITIME",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        letterSpacing = (-0.8).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "LEARNING SHOULDN'T BE A LUXURY.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accent,
                        fontSize = 10.sp,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Hyperion is an independent study companion for maritime candidates preparing for theoretical licensure examinations. It brings question-based practice, competency review, mock examinations, and revision tools together so learners can study at their own pace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hyperion is not MARINA, a government service, maritime administration, or examination authority. It is not affiliated with, endorsed by, or authorized by those bodies and does not guarantee examination results or professional certification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        lineHeight = 19.sp
                    )
                }
            }

            MianSectionHeader(title = "REVIEWER TRACKS")

            MianRuleCard(
                ruleColor = colors.border,
                containerColor = colors.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    AboutBullet("OIC-NW Deck — Operational Level")
                    AboutBullet("Deck Management Level — Chief Mate and Master Mariner preparation")
                    AboutBullet("OIC-EW Engine — Operational Level")
                    AboutBullet("Engine Management Level — Second Engineer and Chief Engineer preparation")
                    AboutBullet("GMDSS GOC — Radio Operator preparation")
                    Text(
                        text = "Available question sets, competencies, and paid access may vary by track, app version, and license.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        lineHeight = 18.sp
                    )
                }
            }

            MianSectionHeader(title = "TOOLS FOR YOUR STUDY ROUTINE")

            AboutItemCard(
                title = "LOCAL STUDY STORAGE",
                description = "Android practice and mock-exam history, bookmarks, mistakes, saved sessions, study time, streak data, and preferences are stored locally on this device.",
                icon = Icons.Default.Storage
            )

            AboutItemCard(
                title = "PRACTICE & MOCK EXAM TOOLS",
                description = "Competency-focused practice, Quick Scan review, question-bank search, timed mock examinations, answer review, and progress summaries.",
                icon = Icons.Default.Psychology
            )

            AboutItemCard(
                title = "LICENSE VERIFICATION",
                description = "Google Play subscriptions, eligible Hyperion account entitlements, and signed device-bound offline activation passes are checked according to their source and applicable verification rules.",
                icon = Icons.Default.Security
            )

            MianSectionHeader(title = "ACCOUNTS & CROSS-PLATFORM ACCESS")

            MianRuleCard(
                ruleColor = colors.border,
                containerColor = colors.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Google sign-in on Android is optional. You can use local study features and make Google Play purchases without signing in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "When you sign in to Android and the website using the same Hyperion account and synchronization succeeds, eligible active website licenses and Google Play entitlements may be recognized on both platforms.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "This is license synchronization, not study-progress synchronization. Android study records remain on this device; supported website/PWA learning progress is synchronized separately to your website account. Using the same email address alone does not link purchases.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "Google Play continues to manage and bill Play purchases. Website licenses are handled through website checkout and PayMongo. The billing, cancellation, and refund processes remain separate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        lineHeight = 18.sp
                    )
                }
            }

            MianSectionHeader(title = "LEGAL, PRIVACY & SUPPORT")

            AboutNavigationCard(
                title = "TERMS OF USE & SUBSCRIPTION POLICY",
                description = "Educational disclaimer, intellectual property, use restrictions, billing, and account deletion terms.",
                icon = Icons.Default.Gavel,
                onClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
            )

            AboutNavigationCard(
                title = "PRIVACY POLICY",
                description = "Information processed by Android and the website, account synchronization, retention, providers, and deletion choices.",
                icon = Icons.Default.Security,
                onClick = { viewModel.navigateTo(Screen.PrivacyPolicy) }
            )

            AboutNavigationCard(
                title = "EXAM GUIDELINES",
                description = "Reference information about examination structure and preparation. Always verify current requirements with official sources.",
                icon = Icons.Default.Description,
                onClick = { viewModel.navigateTo(Screen.ExamGuidelines) }
            )

            AboutNavigationCard(
                title = "ABOUT HYPERION SUPPORT",
                description = "Email: hyperionmaritimex@gmail.com\nWebsite: hyperionmaritime.org",
                icon = Icons.Default.Info,
                onClick = { viewModel.navigateTo(Screen.HowToAvail) }
            )

            Surface(
                shape = RoundedCornerShape(2.dp),
                color = colors.surface2,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "VERSION ${BuildConfig.VERSION_NAME} (BUILD ${BuildConfig.VERSION_CODE})",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.muted,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AboutBullet(text: String) {
    val colors = LocalSpaghettiColors.current
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "•",
            color = colors.accent,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.foreground,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun AboutNavigationCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    MianRuleCard(
        ruleColor = colors.border,
        containerColor = colors.surface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.foreground,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun AboutItemCard(
    title: String,
    description: String,
    icon: ImageVector
) {
    val colors = LocalSpaghettiColors.current

    MianRuleCard(
        ruleColor = colors.border,
        containerColor = colors.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.foreground,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    letterSpacing = (-0.1).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
