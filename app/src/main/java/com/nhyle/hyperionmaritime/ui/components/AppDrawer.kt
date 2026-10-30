package com.nhyle.hyperionmaritime.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.nhyle.hyperionmaritime.ui.motion.HyperionDialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.nhyle.hyperionmaritime.data.auth.SupabaseAccountManager
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" Navigation Drawer:
 * Heavy grotesk typography, 3dp section rules, flat high contrast items, tomato red accents.
 */
@Composable
fun HyperionDrawerContent(
    viewModel: ReviewerViewModel,
    onCloseDrawer: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isActivated by viewModel.isActivated.collectAsStateWithLifecycle()
    val isOicNwProUnlocked by viewModel.isOicNwProUnlocked.collectAsStateWithLifecycle()
    val isGmdssProUnlocked by viewModel.isGmdssProUnlocked.collectAsStateWithLifecycle()
    val isEwActive by viewModel.isEngineOicEwProUnlocked.collectAsStateWithLifecycle()
    val isDmActive by viewModel.isDeckMgtProUnlocked.collectAsStateWithLifecycle()
    val isEmActive by viewModel.isEngineMgtProUnlocked.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val accountState by SupabaseAccountManager.state.collectAsStateWithLifecycle()

    val hasAnyActiveLicense = isOicNwProUnlocked || isGmdssProUnlocked || isEwActive || isDmActive || isEmActive || isActivated || isUnlocked
    var lockedFeatureName by remember { mutableStateOf<String?>(null) }

    var isQuestionBankExpanded by remember { mutableStateOf(true) }
    var isAccountSupportExpanded by remember { mutableStateOf(false) }
    var isLegalExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp),
        drawerContainerColor = colors.surface,
        drawerContentColor = colors.foreground,
        drawerShape = RectangleShape
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
                    Button(
                        onClick = onAccountClick,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.accentForeground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (accountState.email.isNullOrBlank()) "SIGN IN WITH GOOGLE" else "MY HYPERION ACCOUNT",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                        onCloseDrawer()
                        viewModel.navigateToDashboard()
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Appearance / Dark Mode Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RectangleShape)
                                .background(colors.surface2)
                                .border(1.dp, colors.border),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Theme Mode",
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DARK MODE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isDarkMode) "Night Ops Active" else "Daylight Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IndustrialToggleSwitch(
                        checked = isDarkMode,
                        onCheckedChange = { isChecked ->
                            viewModel.toggleDarkMode(isChecked)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                DrawerNavRow(
                    label = "STUDY STREAK",
                    icon = Icons.Default.WorkspacePremium,
                    isSelected = currentScreen is Screen.Streak,
                    onClick = {
                        onCloseDrawer()
                        viewModel.navigateTo(Screen.Streak)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                DrawerNavRow(
                    label = "STUDY TIME",
                    icon = Icons.Default.AccessTime,
                    isSelected = currentScreen is Screen.StudyTime,
                    onClick = {
                        onCloseDrawer()
                        viewModel.navigateTo(Screen.StudyTime)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Transferred & Highlighted Bond Store Button
                DrawerNavRow(
                    label = "BOND STORE",
                    icon = Icons.Default.WorkspacePremium,
                    isSelected = currentScreen is Screen.Store,
                    isHighlighted = true,
                    onClick = {
                        onCloseDrawer()
                        viewModel.navigateTo(Screen.Store)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // SECTION 1: QUESTION BANK & TOOLS (Collapsible Dropdown)
                DrawerCollapsibleHeader(
                    title = "QUESTION BANK & TOOLS",
                    isExpanded = isQuestionBankExpanded,
                    onToggle = { isQuestionBankExpanded = !isQuestionBankExpanded }
                )

                AnimatedVisibility(
                    visible = isQuestionBankExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        DrawerNavRow(
                            label = "SEARCH QUESTION BANK",
                            icon = Icons.Default.Search,
                            isSelected = currentScreen is Screen.Search,
                            isLocked = !hasAnyActiveLicense,
                            onClick = {
                                if (hasAnyActiveLicense) {
                                    onCloseDrawer()
                                    viewModel.navigateTo(Screen.Search)
                                } else {
                                    lockedFeatureName = "the Question Bank Search index"
                                }
                            }
                        )

                        DrawerNavRow(
                            label = "BOOKMARKED QUESTIONS",
                            icon = Icons.Default.Bookmark,
                            isSelected = currentScreen is Screen.Bookmarks,
                            isLocked = !hasAnyActiveLicense,
                            onClick = {
                                if (hasAnyActiveLicense) {
                                    onCloseDrawer()
                                    viewModel.navigateTo(Screen.Bookmarks)
                                } else {
                                    lockedFeatureName = "Bookmarked Questions"
                                }
                            }
                        )

                        DrawerNavRow(
                            label = "SESSION LOG & DIAGNOSTICS",
                            icon = Icons.Default.History,
                            isSelected = currentScreen is Screen.History,
                            isLocked = !hasAnyActiveLicense,
                            onClick = {
                                if (hasAnyActiveLicense) {
                                    onCloseDrawer()
                                    viewModel.navigateTo(Screen.History)
                                } else {
                                    lockedFeatureName = "Session Log & Diagnostics"
                                }
                            }
                        )

                        DrawerNavRow(
                            label = "MISTAKES NOTEBOOK (VAULT)",
                            icon = Icons.Default.Info,
                            isSelected = currentScreen is Screen.Vault,
                            isLocked = !hasAnyActiveLicense,
                            onClick = {
                                if (hasAnyActiveLicense) {
                                    onCloseDrawer()
                                    viewModel.navigateTo(Screen.Vault)
                                } else {
                                    lockedFeatureName = "the Mistakes Notebook"
                                }
                            }
                        )

                        DrawerNavRow(
                            label = "MTESTM IMPORT",
                            icon = Icons.Default.UploadFile,
                            isSelected = currentScreen is Screen.MTestMImport,
                            onClick = {
                                onCloseDrawer()
                                viewModel.navigateTo(Screen.MTestMImport)
                            }
                        )
                    }
                }

                lockedFeatureName?.let { feature ->
                    HyperionDialog(
                        onDismissRequest = { lockedFeatureName = null },
                        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
                    ) {
                        Surface(
                            shape = RectangleShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = colors.accent,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "ACTIVE LICENSE REQUIRED",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.foreground,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "An active license is required to access $feature. You can purchase a pass in the Pass Store or activate an offline key.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { lockedFeatureName = null },
                                        shape = RectangleShape,
                                        border = BorderStroke(1.dp, colors.border),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = colors.foreground
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Text(
                                            text = "LATER",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            lockedFeatureName = null
                                            onCloseDrawer()
                                            viewModel.navigateTo(Screen.Store)
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.accent,
                                            contentColor = colors.accentForeground
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Text(
                                            text = "STORE",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 2: ACCOUNT & SUPPORT (Collapsible Dropdown)
                DrawerCollapsibleHeader(
                    title = "ACCOUNT & SUPPORT",
                    isExpanded = isAccountSupportExpanded,
                    onToggle = { isAccountSupportExpanded = !isAccountSupportExpanded }
                )

                AnimatedVisibility(
                    visible = isAccountSupportExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        DrawerNavRow(
                            label = "ACCOUNT & DEVICE HELP",
                            icon = Icons.AutoMirrored.Filled.Help,
                            isSelected = currentScreen is Screen.HowToAvail,
                            onClick = {
                                onCloseDrawer()
                                viewModel.navigateTo(Screen.HowToAvail)
                            }
                        )

                                            }
                }

                // SECTION 3: LEGAL & COMPLIANCE (Collapsible Dropdown)
                DrawerCollapsibleHeader(
                    title = "LEGAL & COMPLIANCE",
                    isExpanded = isLegalExpanded,
                    onToggle = { isLegalExpanded = !isLegalExpanded }
                )

                AnimatedVisibility(
                    visible = isLegalExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        DrawerNavRow(
                            label = "Terms and Conditions",
                            icon = Icons.Default.Gavel,
                            isSelected = currentScreen is Screen.TermsAndDisclaimer,
                            onClick = {
                                onCloseDrawer()
                                viewModel.navigateTo(Screen.TermsAndDisclaimer)
                            }
                        )

                        DrawerNavRow(
                            label = "Privacy Policy",
                            icon = Icons.Default.Lock,
                            isSelected = currentScreen is Screen.PrivacyPolicy,
                            onClick = {
                                onCloseDrawer()
                                viewModel.navigateTo(Screen.PrivacyPolicy)
                            }
                        )

                        DrawerNavRow(
                            label = "ABOUT HYPERION",
                            icon = Icons.Default.Info,
                            isSelected = currentScreen is Screen.About,
                            onClick = {
                                onCloseDrawer()
                                viewModel.navigateTo(Screen.About)
                            }
                        )
                    }
                }

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
    isHighlighted: Boolean = false,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val isDark = colors.isDark
    val rowBg = when {
        isSelected -> colors.surface2
        isHighlighted -> colors.accent.copy(alpha = if (isDark) 0.15f else 0.10f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isSelected -> colors.accent
        isHighlighted -> colors.accent.copy(alpha = 0.8f)
        else -> null
    }
    val contentColor = when {
        isSelected || isHighlighted -> colors.accent
        isLocked -> colors.muted
        else -> colors.foreground
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RectangleShape,
        color = rowBg,
        border = if (borderColor != null) BorderStroke(1.dp, borderColor) else null,
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
                fontWeight = if (isSelected || isHighlighted) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = if (label.length > 18) 11.sp else 12.sp,
                letterSpacing = if (label.length > 18) 0.3.sp else 0.6.sp,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (badgeText != null) {
                Surface(
                    shape = RectangleShape,
                    color = colors.accent,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accentForeground,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RectangleShape)
                        .background(colors.accent)
                )
            }
        }
    }
}

@Composable
private fun DrawerCollapsibleHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    Surface(
        shape = RectangleShape,
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accent,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
