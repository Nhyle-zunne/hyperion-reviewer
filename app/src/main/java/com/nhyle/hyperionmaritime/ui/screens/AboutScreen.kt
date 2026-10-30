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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
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
            subtitle = "THE MARITIME LEARNING PLATFORM",
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
                        text = "Hyperion is an independent maritime examination-preparation platform for deck officers, marine engineers, and radio operators. It combines question-based practice, competency-focused review, timed mock examinations, and tools that help learners revisit difficult material and build a consistent study routine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hyperion is not an official government, MARINA, maritime administration, or examination-authority service. It does not guarantee examination results or professional certification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        lineHeight = 19.sp
                    )
                }
            }

            MianSectionHeader(title = "REVIEWER PATHWAYS")
            AboutItemCard(
                title = "DECK OPERATIONAL",
                description = "OIC-NW preparation and competency-based practice.",
                icon = Icons.Default.Navigation
            )
            AboutItemCard(
                title = "DECK MANAGEMENT",
                description = "Chief Mate and Master Mariner preparation.",
                icon = Icons.Default.Description
            )
            AboutItemCard(
                title = "ENGINE OPERATIONAL",
                description = "OIC-EW preparation and competency-based practice.",
                icon = Icons.Default.Storage
            )
            AboutItemCard(
                title = "ENGINE MANAGEMENT",
                description = "Second Engineer and Chief Engineer preparation.",
                icon = Icons.Default.Psychology
            )
            AboutItemCard(
                title = "RADIO OPERATOR",
                description = "GMDSS GOC preparation.",
                icon = Icons.Default.Security
            )

            MianSectionHeader(title = "STUDY TOOLS")
            AboutItemCard(
                title = "PRACTICE AND QUICK SCAN",
                description = "Practice sessions, competence-focused drills, Quick Scan, and targeted question review.",
                icon = Icons.Default.Search
            )
            AboutItemCard(
                title = "MOCK EXAMINATIONS",
                description = "Timed exam-style practice with results and review tools.",
                icon = Icons.Default.Description
            )
            AboutItemCard(
                title = "STUDY HISTORY AND REVIEW",
                description = "Bookmarks, mistakes notebook, saved sessions, history, study-time summaries, and streak tracking.",
                icon = Icons.Default.History
            )
            AboutItemCard(
                title = "LOCAL-FIRST AND OFFLINE USE",
                description = "Android study records are stored locally. Supported question content and licensed features can be used offline subject to the app's current access checks.",
                icon = Icons.Default.Storage
            )

            MianSectionHeader(title = "ACCOUNTS AND LICENSES")
            AboutItemCard(
                title = "OPTIONAL ANDROID SIGN-IN",
                description = "Google sign-in is optional on Android. When signed in with the same Hyperion account on the website, eligible website and Google Play entitlements can be synchronized across platforms.",
                icon = Icons.Default.Info
            )
            AboutItemCard(
                title = "SEPARATE STUDY DATA",
                description = "Android study history stays on the device. Supported website/PWA learning progress syncs to the website account separately. License synchronization does not copy study history between platforms.",
                icon = Icons.Default.Security
            )
            AboutItemCard(
                title = "PLATFORM BILLING",
                description = "Google Play handles Android subscription billing. Website purchases use website checkout and PayMongo. Linking eligible entitlements to one Hyperion account does not merge billing, cancellation, or refund processes.",
                icon = Icons.Default.Description
            )
            AboutItemCard(
                title = "ACCOUNT DELETION",
                description = "You can permanently delete your Hyperion account from My Hyperion Account. Deletion removes linked licenses and account data, but does not cancel Google Play billing or erase local Android study records.",
                icon = Icons.Default.Gavel
            )

            MianSectionHeader(title = "LEGAL & COMPLIANCE")
            MianRuleCard(
                ruleColor = colors.border,
                containerColor = colors.surface,
                onClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
            ) {
                LegalNavigationRow(
                    icon = Icons.Default.Gavel,
                    title = "TERMS OF USE",
                    description = "Educational-use terms, platform billing, licenses, and account rules."
                )
            }

            MianRuleCard(
                ruleColor = colors.border,
                containerColor = colors.surface,
                onClick = { viewModel.navigateTo(Screen.PrivacyPolicy) }
            ) {
                LegalNavigationRow(
                    icon = Icons.Default.Security,
                    title = "PRIVACY POLICY",
                    description = "Personal information, account data, providers, retention, and deletion."
                )
            }

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
private fun LegalNavigationRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    val colors = LocalSpaghettiColors.current
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
                fontSize = 11.sp
            )
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

            Column(modifier = Modifier.weight(1f)) {
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
