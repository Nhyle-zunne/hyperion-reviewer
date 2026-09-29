package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.ui.theme.LocalSpaghettiColors
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" Navigation Drawer:
 * Heavy grotesk typography, 3dp section rules, flat high contrast items, tomato red accents.
 */
@Composable
fun HyperionDrawerContent(
    viewModel: ReviewerViewModel,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isActivated by viewModel.isActivated.collectAsState()
    val isOicNwProUnlocked by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssProUnlocked by viewModel.isGmdssProUnlocked.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()

    ModalDrawerSheet(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp),
        drawerContainerColor = colors.surface,
        drawerContentColor = colors.foreground,
        drawerShape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Editorial Drawer Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Text(
                        text = "HYPERION",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        letterSpacing = (-0.8).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MARITIME LICENSURE REVIEWER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.muted,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp
                    )
                }
            }

            // 3dp Header Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(colors.accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // SECTION: MAIN
                DrawerNavRow(
                    label = "DASHBOARD",
                    icon = Icons.Default.Home,
                    isSelected = currentScreen is Screen.TrackSelection,
                    onClick = {
                        viewModel.navigateToDashboard()
                        onCloseDrawer()
                    }
                )

                // SECTION: EXAMINATION TRACKS
                MianSectionHeader(title = "EXAMINATION TRACKS")

                DrawerTrackRow(
                    title = "OIC-NW DECK OPERATIONAL",
                    subtitle = "STCW Regulation II/1 • 12 Competencies",
                    isSelected = selectedTrack == Track.DECK_OIC_NW,
                    isUnlocked = isOicNwProUnlocked || isActivated,
                    onClick = {
                        viewModel.selectTrack(Track.DECK_OIC_NW)
                        viewModel.navigateTo(Screen.Home)
                        onCloseDrawer()
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                DrawerTrackRow(
                    title = "GMDSS RADIO COMMUNICATIONS",
                    subtitle = "STCW Regulation IV/2 • GOC Standard",
                    isSelected = selectedTrack == Track.GMDSS_GOC,
                    isUnlocked = isGmdssProUnlocked || isActivated,
                    onClick = {
                        viewModel.selectTrack(Track.GMDSS_GOC)
                        viewModel.navigateTo(Screen.Home)
                        onCloseDrawer()
                    }
                )

                // SECTION: QUESTION BANK & TOOLS
                MianSectionHeader(title = "QUESTION BANK & TOOLS")

                DrawerNavRow(
                    label = "SEARCH QUESTION BANK",
                    icon = Icons.Default.Search,
                    isSelected = currentScreen is Screen.Search,
                    isLocked = !(isOicNwProUnlocked || isGmdssProUnlocked || isActivated),
                    onClick = {
                        viewModel.requireActivationOr {
                            viewModel.navigateTo(Screen.Search)
                        }
                        onCloseDrawer()
                    }
                )

                DrawerNavRow(
                    label = "BOOKMARKED QUESTIONS",
                    icon = Icons.Default.Bookmark,
                    isSelected = currentScreen is Screen.Bookmarks,
                    onClick = {
                        viewModel.navigateTo(Screen.Bookmarks)
                        onCloseDrawer()
                    }
                )

                DrawerNavRow(
                    label = "SESSION LOG & DIAGNOSTICS",
                    icon = Icons.Default.History,
                    isSelected = currentScreen is Screen.History,
                    onClick = {
                        viewModel.navigateTo(Screen.History)
                        onCloseDrawer()
                    }
                )

                DrawerNavRow(
                    label = "MISTAKES NOTEBOOK (VAULT)",
                    icon = Icons.Default.Info,
                    isSelected = currentScreen is Screen.Vault,
                    onClick = {
                        viewModel.navigateTo(Screen.Vault)
                        onCloseDrawer()
                    }
                )

                // SECTION: ACCOUNT & SUPPORT
                MianSectionHeader(title = "ACCOUNT & SUPPORT")

                DrawerNavRow(
                    label = "ACCOUNT & DEVICE HELP",
                    icon = Icons.AutoMirrored.Filled.Help,
                    isSelected = currentScreen is Screen.HowToAvail,
                    onClick = {
                        viewModel.navigateTo(Screen.HowToAvail)
                        onCloseDrawer()
                    }
                )

                DrawerNavRow(
                    label = "DEVICE REGISTRATION & LICENSE",
                    icon = Icons.Default.Key,
                    isSelected = currentScreen is Screen.Activation,
                    onClick = {
                        viewModel.navigateTo(Screen.Activation)
                        onCloseDrawer()
                    }
                )

                // SECTION: LEGAL & COMPLIANCE
                MianSectionHeader(title = "LEGAL & COMPLIANCE")

                DrawerNavRow(
                    label = "MARINA EXAM GUIDELINES",
                    icon = Icons.Default.Gavel,
                    isSelected = currentScreen is Screen.ExamGuidelines,
                    onClick = {
                        viewModel.navigateTo(Screen.ExamGuidelines)
                        onCloseDrawer()
                    }
                )

                DrawerNavRow(
                    label = "TERMS & ACADEMIC DISCLAIMER",
                    icon = Icons.Default.Gavel,
                    isSelected = currentScreen is Screen.TermsAndDisclaimer,
                    onClick = {
                        viewModel.navigateTo(Screen.TermsAndDisclaimer)
                        onCloseDrawer()
                    }
                )

                DrawerNavRow(
                    label = "ABOUT HYPERION",
                    icon = Icons.Default.Info,
                    isSelected = currentScreen is Screen.About,
                    onClick = {
                        viewModel.navigateTo(Screen.About)
                        onCloseDrawer()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun DrawerNavRow(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val rowBg = if (isSelected) colors.surface2 else Color.Transparent
    val contentColor = when {
        isSelected -> colors.accent
        isLocked -> colors.muted
        else -> colors.foreground
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(2.dp),
        color = rowBg,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(colors.accent)
                )
            }
        }
    }
}

@Composable
private fun DrawerTrackRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val topRuleColor = if (isSelected) colors.accent else colors.border

    MianRuleCard(
        ruleColor = topRuleColor,
        containerColor = if (isSelected) colors.surface2 else colors.surface,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) colors.accent else colors.foreground,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )

                ProStatusBadge(
                    isUnlocked = isUnlocked,
                    lockedLabel = "LOCKED",
                    unlockedLabel = "ACTIVE"
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                fontSize = 11.sp
            )
        }
    }
}
