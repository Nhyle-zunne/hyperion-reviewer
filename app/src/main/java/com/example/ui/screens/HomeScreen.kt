package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ActiveSessionEntity
import com.example.data.local.isCompleted
import com.example.data.local.answeredQuestions
import com.example.data.local.ExamRecordEntity
import com.example.data.model.QuizMode
import com.example.data.model.Track
import com.example.data.repository.MaritimeQuestionBank
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.ProStatusBadge
import com.example.ui.theme.FailRed
import com.example.ui.theme.MaritimeGold
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.NauticalNavyDark
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun HomeScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val mistakeEntries by viewModel.mistakeEntries.collectAsState()
    val examRecords by viewModel.examRecords.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val isOicNwProUnlocked by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssProUnlocked by viewModel.isGmdssProUnlocked.collectAsState()
    val isHyperionProUnlocked by viewModel.isHyperionProUnlocked.collectAsState()
    val isTrackUnlocked = if (selectedTrack == Track.DECK_OIC_NW) isOicNwProUnlocked else isGmdssProUnlocked
    val isActivated by viewModel.isActivated.collectAsState()
    val isAnyProUnlocked = isOicNwProUnlocked || isGmdssProUnlocked || isHyperionProUnlocked || isActivated
    var showMockExamWarningDialog by remember { mutableStateOf(false) }
    var showSearchLockedDialog by remember { mutableStateOf(false) }

    val trackMistakes = mistakeEntries.filter { it.track.equals(selectedTrack.name, ignoreCase = true) }
    val totalMistakes = trackMistakes.size
    val totalBookmarks = bookmarkedIds.size

    val trackExams = examRecords.filter { it.track == selectedTrack.name }
    val totalExamsTaken = trackExams.size
    val passedExams = trackExams.count { it.isPassed }
    val averageScore = if (totalExamsTaken > 0) {
        trackExams.map { it.percentage }.average()
    } else 0.0

    BackHandler {
        viewModel.navigateTo(Screen.TrackSelection)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        MaritimeTopBar(
            title = if (selectedTrack == Track.DECK_OIC_NW) "OIC-NW Operational Track" else "GMDSS Radio Track",
            showBackButton = true,
            onBackClick = { viewModel.navigateTo(Screen.TrackSelection) },
            onMenuClick = { viewModel.openDrawer() },
            onLegalClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) },
            onSearchClick = {
                if (isAnyProUnlocked) {
                    viewModel.navigateTo(Screen.Search)
                } else {
                    showSearchLockedDialog = true
                }
            },
            isSearchLocked = !isAnyProUnlocked,
            onHistoryClick = { viewModel.navigateTo(Screen.History) },
            onBookmarksClick = { viewModel.navigateTo(Screen.Bookmarks) },
            onVaultClick = { viewModel.navigateTo(Screen.Vault) },
            bookmarkCount = totalBookmarks,
            vaultCount = totalMistakes
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Hero Banner
                HeroBanner(track = selectedTrack, isUnlocked = isTrackUnlocked)
            }

            item {
                // Live KPI Stats Grid
                StatsGrid(
                    examsCount = totalExamsTaken,
                    avgScore = averageScore,
                    mistakeCount = totalMistakes,
                    bookmarkCount = totalBookmarks
                )
            }

            // Resume Active Session Card (if unfinished session exists)
            activeSession?.let { session ->
                if (!session.isCompleted && session.answeredQuestions < session.totalQuestions) {
                    item {
                        ResumeActiveSessionCard(
                            session = session,
                            onResume = { viewModel.resumeSession(session.track, session.id) },
                            onDiscard = { viewModel.discardActiveSession() }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "EXAMINATION MODES",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    letterSpacing = 1.sp
                )
            }

            // Quick Mode Launchers
            if (!isTrackUnlocked) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Full Question Bank & Mock Exam Verification Required",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Verify your terminal to access the complete question bank and timed competency mock exams.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.navigateTo(Screen.HowToAvail) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = PureWhite
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Account & Device Help", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (selectedTrack == Track.DECK_OIC_NW) {
                item {
                    ModeLauncherCard(
                        title = "MARINA CRP-Based Practice",
                        subtitle = "Standard review • 2,065 items across 12 competencies\nNote: This free practice mode is independently developed based on MARINA CRP references and is not an official MARINA-endorsed examination tool.",
                        badge = "STANDARD REVIEW",
                        badgeColor = PassGreen,
                        icon = Icons.Default.School,
                        accentColor = PassGreen,
                        onClick = {
                            viewModel.navigateTo(Screen.PracticeCompetenceSelect(isCrpMode = true))
                        }
                    )
                }
            } else if (selectedTrack == Track.GMDSS_GOC) {
                item {
                    ModeLauncherCard(
                        title = "MARINA CRP-Based Practice",
                        subtitle = "Standard review • 1,200 items (C1: 2 Parts [500 each], C2: 200 items)\nNote: This free practice mode is independently developed based on MARINA CRP references and is not an official MARINA-endorsed examination tool.",
                        badge = "STANDARD REVIEW",
                        badgeColor = PassGreen,
                        icon = Icons.Default.School,
                        accentColor = PassGreen,
                        onClick = {
                            viewModel.navigateTo(Screen.PracticeCompetenceSelect(isCrpMode = true))
                        }
                    )
                }
            }

            item {
                ModeLauncherCard(
                    title = "Practice Drill Configuration",
                    subtitle = "Two-step setup: select competence, then configure sequential/randomized ordering & item count",
                    badge = if (isTrackUnlocked) "CUSTOMIZABLE" else "VERIFICATION REQUIRED",
                    badgeColor = if (isTrackUnlocked) SeaCyan else MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.Tune,
                    accentColor = SeaCyan,
                    onClick = {
                        viewModel.requireActivationOr {
                            viewModel.navigateTo(Screen.PracticeCompetenceSelect())
                        }
                    }
                )
            }

            item {
                ModeLauncherCard(
                    title = "Official Timed Mock Exam",
                    subtitle = if (selectedTrack == Track.DECK_OIC_NW) "Competency-based timing • 1 min/item • 70% Cutoff" else "Competency-based timing • Operator standard • 70% Cutoff",
                    badge = if (isTrackUnlocked) "STCW TOS MOCK" else "VERIFICATION REQUIRED",
                    badgeColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.Timer,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        viewModel.requireActivationOr {
                            showMockExamWarningDialog = true
                        }
                    }
                )
            }

            item {
                ModeLauncherCard(
                    title = "Flashcard Revision Mode",
                    subtitle = "Active recall study • Tap to flip cards • Rationale & Bloom's taxonomy breakdown",
                    badge = "ACTIVE RECALL",
                    badgeColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.Style,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        viewModel.navigateTo(Screen.Flashcards)
                    }
                )
            }

            item {
                ModeLauncherCard(
                    title = "Mistakes Notebook (Vault)",
                    subtitle = "$totalMistakes questions to redeem. Removed after 2 consecutive correct answers.",
                    badge = if (totalMistakes > 0) "$totalMistakes ITEMS" else "EMPTY",
                    badgeColor = if (totalMistakes > 0) FailRed else MaterialTheme.colorScheme.outline,
                    icon = Icons.Default.AutoFixHigh,
                    accentColor = FailRed,
                    onClick = {
                        viewModel.navigateTo(Screen.Vault)
                    }
                )
            }

            item {
                ModeLauncherCard(
                    title = "Session Log",
                    subtitle = "Past mock exam attempts, Bloom's scoring, and 70% competence diagnostics",
                    badge = if (examRecords.isNotEmpty()) "${examRecords.size} LOGS" else "EMPTY",
                    badgeColor = SeaCyan,
                    icon = Icons.Default.Analytics,
                    accentColor = SeaCyan,
                    onClick = {
                        viewModel.navigateTo(Screen.History)
                    }
                )
            }

            item {
                ModeLauncherCard(
                    title = "MARINA Exam Guidelines",
                    subtitle = "TOS item distribution, permit & uniform rules, 70% passing cutoff, and Bloom's scoring",
                    badge = "OFFICIAL RULES",
                    badgeColor = MaritimeGoldBright,
                    icon = Icons.Default.Info,
                    accentColor = MaritimeGoldBright,
                    onClick = {
                        viewModel.navigateTo(Screen.ExamGuidelines)
                    }
                )
            }

            item {
                // MARINA Strict Passing Policy Card
                MarinaPolicyCard(
                    onClick = { viewModel.navigateTo(Screen.ExamGuidelines) }
                )
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
                        .padding(vertical = 8.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SeaCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Legal Disclaimer, Academic Terms & Privacy Agreement",
                            style = MaterialTheme.typography.labelSmall,
                            color = SeaCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Official Mock Exam Warning Dialog
    if (showMockExamWarningDialog) {
        AlertDialog(
            onDismissRequest = { showMockExamWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaritimeGoldBright,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Official Mock Exam Warning",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "⚠️ Strict MARINA Board Conditions",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "This is an official timed assessment. Timers are competency-based (allocated strictly per competency upon start) and cannot be paused. Exiting early records an incomplete score. Ensure you are ready before proceeding.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMockExamWarningDialog = false
                        viewModel.startQuizSession(mode = QuizMode.MOCK_EXAM)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showMockExamWarningDialog = false }
                ) {
                    Text("Go Back")
                }
            }
        )
    }

    // Pro-Gated Search Locked Dialog
    if (showSearchLockedDialog) {
        AlertDialog(
            onDismissRequest = { showSearchLockedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Question Bank Search Verification",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Full-text search across all 7,644+ Licensure Reviewer questions, STCW conventions, and MARINA regulations requires terminal verification.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "Verify your terminal to enable offline search across all deck and radio operator competencies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSearchLockedDialog = false
                        viewModel.navigateTo(Screen.HowToAvail)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Account & Device Help", fontWeight = FontWeight.Bold, color = PureWhite)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSearchLockedDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeroBanner(track: Track, isUnlocked: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "STCW '78 AS AMENDED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        ProStatusBadge(
                            isUnlocked = isUnlocked,
                            lockedLabel = "VERIFICATION REQUIRED",
                            unlockedLabel = "VERIFIED TERMINAL"
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isUnlocked) PassGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isUnlocked) "VERIFIED TERMINAL" else "OFFLINE MODE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) PassGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (track == Track.DECK_OIC_NW) "OIC-NW Deck Operational Review" else "GMDSS Radio Operator Review",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (track == Track.DECK_OIC_NW) {
                        "3 Functions (12 Competencies) • 6,444 Questions Bank\n• Function 1: Navigation & Passage Planning (C1, C2, C3, C4, C5, C7, C9)\n• Function 2: Cargo Handling & Stowage (C10, C11)\n• Function 3: Ship Operation & Safety (C12, C13, C17)"
                    } else {
                        "2 Competencies (12 Parts) • 2,260 Questions Bank\n• C1: GMDSS Radio Operations & Subsystems (10 Parts • 1,911 Items)\n• C2: Emergency Communications & SAR Procedures (2 Parts • 349 Items)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(
    examsCount: Int,
    avgScore: Double,
    mistakeCount: Int,
    bookmarkCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                title = "Mocks Taken",
                value = "$examsCount",
                color = SeaCyan,
                modifier = Modifier.weight(1f)
            )

            VerticalDivider(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            StatColumn(
                title = "Avg. Score",
                value = if (examsCount > 0) String.format("%.0f%%", avgScore) else "--",
                color = if (avgScore >= 70.0) PassGreen else Color(0xFFB45309),
                modifier = Modifier.weight(1f)
            )

            VerticalDivider(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            StatColumn(
                title = "Error Vault",
                value = "$mistakeCount",
                color = if (mistakeCount > 0) FailRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            VerticalDivider(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            StatColumn(
                title = "Bookmarks",
                value = "$bookmarkCount",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatColumn(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ModeLauncherCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun MarinaPolicyCard(
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "MARINA Policy",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MARINA Evaluation & Weighted Scoring",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "View All →",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaritimeGoldBright
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Passing Threshold: 70.0% per competence. A candidate passes only if obtaining >= 70% in EVERY competence within the function. Easy: 3 pts, Moderate: 6 pts, Difficult: 9 pts. Tap to view full guidelines & permit requirements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ResumeActiveSessionCard(
    session: ActiveSessionEntity,
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    val trackName = if (session.track == Track.DECK_OIC_NW.name) "OIC-NW" else if (session.track == Track.GMDSS_GOC.name) "GMDSS" else session.track.replace('_', ' ')
    val modeStr = if (session.mode.contains("MOCK")) "MOCK" else "PRACTICE"
    val progressFraction = session.answeredCount.toFloat() / session.totalQuestions.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESUME ACTIVE SESSION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$trackName $modeStr",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Progress: Answered ${session.answeredCount} / ${session.totalQuestions} questions",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (session.timeRemainingSeconds > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val mins = session.timeRemainingSeconds / 60
                val secs = session.timeRemainingSeconds % 60
                Text(
                    text = "Time Remaining: ${mins}m ${String.format("%02d", secs)}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaritimeGoldBright,
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Resume Exam",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Discard & Reset",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
