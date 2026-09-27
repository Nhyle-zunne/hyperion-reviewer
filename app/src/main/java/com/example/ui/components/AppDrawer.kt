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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
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
                    .background(Color(0xFF0F1E36))
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HyperionEmblem(size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HYPERION",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 3.sp,
                                    fontFamily = FontFamily.Serif
                                ),
                                color = MaritimeGoldBright
                            )
                            Text(
                                text = "Maritime Licensure Reviewer",
                                style = MaterialTheme.typography.labelSmall,
                                color = PureWhite.copy(alpha = 0.9f)
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
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isActivated) PassGreen.copy(alpha = 0.2f) else MaritimeGold.copy(alpha = 0.2f),
                        border = BorderStroke(
                            1.dp,
                            if (isActivated) PassGreen else MaritimeGoldBright
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isActivated) Icons.Default.CheckCircle else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isActivated) PassGreen else MaritimeGoldBright,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isActivated) "HYPERION PRO • UNLOCKED" else "FREE TIER • LIMITED ACCESS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isActivated) PassGreen else MaritimeGoldBright,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 1: Track Switching
            DrawerSectionHeader("EXAMINATION TRACKS")

            NavigationDrawerItem(
                label = {
                    Text(
                        text = "Deck Operational Level (OIC-NW)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Deck OIC-NW",
                        tint = if (selectedTrack == Track.DECK_OIC_NW) MaritimeGoldBright else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                badge = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedTrack == Track.DECK_OIC_NW) MaritimeGoldBright.copy(alpha = 0.2f) else Color.Transparent
                    ) {
                        Text(
                            text = if (isActivated) "200 ITEMS" else "FREE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTrack == Track.DECK_OIC_NW) MaritimeGoldBright else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                    selectedContainerColor = MaritimeGold.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            NavigationDrawerItem(
                label = {
                    Text(
                        text = "GMDSS Radio Operator (GOC)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "GMDSS GOC",
                        tint = if (selectedTrack == Track.GMDSS_GOC) SeaCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                badge = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedTrack == Track.GMDSS_GOC) SeaCyan.copy(alpha = 0.2f) else Color.Transparent
                    ) {
                        Text(
                            text = if (isActivated) "100 ITEMS" else "FREE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTrack == Track.GMDSS_GOC) SeaCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

            // Section 2: Study & Examination Modes
            DrawerSectionHeader("EXAMINATION & DRILL MODES")

            NavigationDrawerItem(
                label = { Text("STCW Reviewer", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "STCW Reviewer",
                        tint = PassGreen
                    )
                },
                selected = currentScreen is Screen.Topics,
                onClick = {
                    onCloseDrawer()
                    viewModel.navigateTo(Screen.Topics)
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_stcw_reviewer"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = PassGreen.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            NavigationDrawerItem(
                label = { Text("Practice Drill Configuration", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = SeaCyan
                    )
                },
                selected = currentScreen is Screen.PracticeCompetenceSelect || currentScreen is Screen.PracticeConfig,
                onClick = {
                    onCloseDrawer()
                    viewModel.navigateTo(Screen.PracticeCompetenceSelect)
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_practice_config"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = SeaCyan.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            NavigationDrawerItem(
                label = { Text("Official Timed Mock Exam", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaritimeGoldBright
                    )
                },
                badge = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isActivated) PassGreen.copy(alpha = 0.2f) else MaritimeGold.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isActivated) "TIMED" else "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActivated) PassGreen else MaritimeGoldBright,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                selected = currentScreen is Screen.MockHub || currentScreen is Screen.MockCompetence,
                onClick = {
                    onCloseDrawer()
                    viewModel.requireActivationOr {
                        viewModel.startQuizSession(mode = QuizMode.MOCK_EXAM)
                    }
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_mock_exam"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaritimeGold.copy(alpha = 0.15f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            NavigationDrawerItem(
                label = { Text("Flashcard Revision", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B)
                    )
                },
                selected = currentScreen is Screen.Flashcards,
                onClick = {
                    onCloseDrawer()
                    viewModel.navigateTo(Screen.Flashcards)
                },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("drawer_item_flashcards"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0x22F59E0B),
                    unselectedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Section 3: Account & Licensing
            DrawerSectionHeader("ACCOUNT & LICENSING")

            NavigationDrawerItem(
                label = { Text("Activation & License", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaritimeGoldBright
                    )
                },
                badge = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isActivated) PassGreen.copy(alpha = 0.2f) else FailRed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isActivated) "ACTIVATED" else "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActivated) PassGreen else MaritimeGoldBright,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
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
                    selectedContainerColor = MaritimeGold.copy(alpha = 0.15f),
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
                        tint = PureWhite.copy(alpha = 0.85f)
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
                    selectedContainerColor = PureWhite.copy(alpha = 0.1f),
                    unselectedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Drawer Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Hyperion Maritime Reviewer v2.4.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Text(
                    text = "100% Offline-First Architecture",
                    style = MaterialTheme.typography.labelSmall,
                    color = SeaCyan.copy(alpha = 0.6f),
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
        color = MaritimeGoldBright,
        letterSpacing = 1.sp,
        fontSize = 10.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
    )
}
