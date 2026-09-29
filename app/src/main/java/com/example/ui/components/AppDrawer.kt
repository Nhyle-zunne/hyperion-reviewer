package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizMode
import com.example.data.model.Track
import com.example.ui.theme.FailRed
import com.example.ui.theme.MaritimeGold
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun HyperionDrawerContent(
    viewModel: ReviewerViewModel,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isActivated by viewModel.isActivated.collectAsState()
    val isOicNwProUnlocked by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssProUnlocked by viewModel.isGmdssProUnlocked.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()

    ModalDrawerSheet(
        modifier = modifier
            .fillMaxHeight()
            .width(310.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Drawer Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            shadowElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            HyperionEmblem(size = 48.dp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HYPERION",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 3.sp,
                                    fontFamily = FontFamily.Serif
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "By Nhyle",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                ),
                                color = MaritimeGoldBright
                            )
                            Text(
                                text = "Maritime Licensure Reviewer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Aligned with MARINA & STCW Competency Standards",
                        style = MaterialTheme.typography.labelSmall,
                        color = SeaCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Pill
                    val (statusLabel, isPillActive) = when {
                        isOicNwProUnlocked && isGmdssProUnlocked -> "HYPERION SUITE • VERIFIED" to true
                        isOicNwProUnlocked -> "OIC-NW TRACK • VERIFIED" to true
                        isGmdssProUnlocked -> "GMDSS TRACK • VERIFIED" to true
                        else -> "STANDARD ACCESS" to false
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isPillActive) PassGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp,
                            if (isPillActive) PassGreen else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPillActive) Icons.Default.CheckCircle else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isPillActive) PassGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isPillActive) PassGreen else MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            NavigationDrawerItem(
                label = { Text("Home", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home"
                    )
                },
                selected = currentScreen is Screen.TrackSelection,
                onClick = {
                    onCloseDrawer()
                    viewModel.navigateTo(Screen.TrackSelection)
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_home"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Section 1: Track Switching
            DrawerSectionHeader("EXAMINATION TRACKS")

             NavigationDrawerItem(
                label = {
                    Text(
                        text = "OIC-NW Deck Operational",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "OIC-NW Deck Operational",
                        tint = if (selectedTrack == Track.DECK_OIC_NW) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                badge = {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isOicNwProUnlocked) PassGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isOicNwProUnlocked) "VERIFIED" else "STANDARD",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOicNwProUnlocked) PassGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                },
                selected = selectedTrack == Track.DECK_OIC_NW && (currentScreen is Screen.Home || currentScreen is Screen.PracticeCompetenceSelect || currentScreen is Screen.MockHub || currentScreen is Screen.Flashcards),
                onClick = {
                    onCloseDrawer()
                    viewModel.setTrack(Track.DECK_OIC_NW)
                    viewModel.navigateTo(Screen.Home)
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_deck_oic_nw"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            NavigationDrawerItem(
                label = {
                    Text(
                        text = "GMDSS Radio Communications",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "GMDSS Radio Communications",
                        tint = if (selectedTrack == Track.GMDSS_GOC) SeaCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                badge = {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isGmdssProUnlocked) PassGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isGmdssProUnlocked) "VERIFIED" else "STANDARD",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGmdssProUnlocked) PassGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                },
                selected = selectedTrack == Track.GMDSS_GOC && (currentScreen is Screen.Home || currentScreen is Screen.PracticeCompetenceSelect || currentScreen is Screen.MockHub || currentScreen is Screen.Flashcards),
                onClick = {
                    onCloseDrawer()
                    viewModel.setTrack(Track.GMDSS_GOC)
                    viewModel.navigateTo(Screen.Home)
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_gmdss_goc"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = SeaCyan.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Section 2: Study Tools & Search Bank
            DrawerSectionHeader("QUESTION BANK & TOOLS")

            val isAnyProUnlocked = isOicNwProUnlocked || isGmdssProUnlocked || isActivated

            NavigationDrawerItem(
                label = { Text("Search Bank", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = if (isAnyProUnlocked) Icons.Default.Search else Icons.Default.Lock,
                        contentDescription = "Search Bank",
                        tint = if (isAnyProUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                badge = {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isAnyProUnlocked) PassGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isAnyProUnlocked) "PRO" else "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAnyProUnlocked) PassGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                },
                selected = currentScreen is Screen.Search,
                onClick = {
                    onCloseDrawer()
                    if (isAnyProUnlocked) {
                        viewModel.navigateTo(Screen.Search)
                    } else {
                        viewModel.navigateTo(Screen.Activation)
                    }
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_search"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Section 3: Account & Support
            DrawerSectionHeader("ACCOUNT & SUPPORT")

            NavigationDrawerItem(
                label = { Text("Account & Device Help", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = "Account & Device Help",
                        tint = MaritimeGoldBright
                    )
                },
                badge = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaritimeGoldBright.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "SUPPORT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaritimeGoldBright,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                selected = currentScreen is Screen.HowToAvail,
                onClick = {
                    onCloseDrawer()
                    viewModel.navigateTo(Screen.HowToAvail)
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_how_to_avail"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaritimeGoldBright.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            NavigationDrawerItem(
                label = { Text("Device Registration", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                badge = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isActivated) PassGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = when {
                                isOicNwProUnlocked && isGmdssProUnlocked -> "HYPERION PRO"
                                isOicNwProUnlocked -> "OIC PRO"
                                isGmdssProUnlocked -> "GMDSS PRO"
                                else -> "LOCKED"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActivated) PassGreen else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                selected = currentScreen is Screen.Activation,
                onClick = {
                    onCloseDrawer()
                    viewModel.navigateTo(Screen.Activation)
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_activation"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Section 4: Legal & Compliance
            DrawerSectionHeader("LEGAL & COMPLIANCE")

            NavigationDrawerItem(
                label = { Text("MARINA Exam Guidelines", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaritimeGoldBright
                    )
                },
                selected = currentScreen is Screen.ExamGuidelines,
                onClick = {
                    onCloseDrawer()
                    viewModel.navigateTo(Screen.ExamGuidelines)
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_exam_guidelines"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaritimeGoldBright.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            NavigationDrawerItem(
                label = { Text("Terms & Academic Disclaimer", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = SeaCyan
                    )
                },
                selected = currentScreen is Screen.TermsAndDisclaimer,
                onClick = {
                    onCloseDrawer()
                    viewModel.navigateTo(Screen.TermsAndDisclaimer)
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_terms"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = SeaCyan.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            NavigationDrawerItem(
                label = { Text("About Hyperion", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                selected = currentScreen is Screen.About,
                onClick = {
                    onCloseDrawer()
                    viewModel.navigateTo(Screen.About)
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_about"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            val isDarkMode by viewModel.isDarkMode.collectAsState()

            // Theme Switch Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Theme Mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Dark Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { enabled ->
                            viewModel.toggleDarkMode(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("theme_mode_switch")
                    )
                }
            }

            // Drawer Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Hyperion Maritime Reviewer v2.4.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Text(
                    text = "100% Offline-First Architecture",
                    style = MaterialTheme.typography.labelSmall,
                    color = SeaCyan.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.tertiary,
        letterSpacing = 1.sp,
        fontSize = 10.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
    )
}
