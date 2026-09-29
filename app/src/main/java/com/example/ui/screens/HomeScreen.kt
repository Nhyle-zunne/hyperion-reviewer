package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ActiveSessionEntity
import com.example.data.local.isCompleted
import com.example.data.local.answeredQuestions
import com.example.data.model.Track
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.MianButton
import com.example.ui.components.MianRuleButton
import com.example.ui.components.MianRuleCard
import com.example.ui.components.MianSectionHeader
import com.example.ui.components.ProStatusBadge
import com.example.ui.components.SpaghettiProgressBar
import com.example.ui.theme.LocalSpaghettiColors
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" HomeScreen:
 * Heavy grotesk typography, rules instead of boxed cards, 2dp corners, flat surfaces, tomato red accents.
 */
@Composable
fun HomeScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
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
    val averageScore = if (totalExamsTaken > 0) {
        trackExams.map { it.percentage }.average()
    } else 0.0

    BackHandler {
        viewModel.navigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding()
    ) {
        MaritimeTopBar(
            title = if (selectedTrack == Track.DECK_OIC_NW) "OIC-NW Operational Track" else "GMDSS Radio Track",
            showBackButton = true,
            onBackClick = { viewModel.navigateBack() },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HeroBanner(track = selectedTrack, isUnlocked = isTrackUnlocked)
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
                // Section Header: PERFORMANCE
                MianSectionHeader(title = "PERFORMANCE OVERVIEW")
            }

            item {
                // Live KPI Stats Grid (Editorial 4-column layout)
                StatsGrid(
                    examsCount = totalExamsTaken,
                    avgScore = averageScore,
                    mistakeCount = totalMistakes,
                    bookmarkCount = totalBookmarks
                )
            }

            item {
                // Section Header: EXAMINATION MODES
                MianSectionHeader(title = "EXAMINATION MODES")
            }

            // Quick Mode Launchers
            if (!isTrackUnlocked) {
                item {
                    MianRuleCard(
                        ruleColor = colors.accent,
                        containerColor = colors.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.surface2),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TERMINAL VERIFICATION REQUIRED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.accent,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.2.sp,
                                    textAlign = TextAlign.Start
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Verify terminal to access the complete question bank and timed competency mock exams.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted,
                                    textAlign = TextAlign.Start
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            MianButton(
                                onClick = { viewModel.navigateTo(Screen.HowToAvail) },
                                accent = true
                            ) {
                                Text(
                                    text = "HELP",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTrack == Track.DECK_OIC_NW) {
                item {
                    ModeLauncherCard(
                        title = "MARINA CRP-Based Practice",
                        subtitle = "Standard review • 2,065 official review questions across 12 competencies (F1: 1,531, F2: 205, F3: 329)\nNote: Independently developed review based on MARINA references.",
                        badge = "STANDARD REVIEW",
                        badgeColor = colors.success,
                        icon = Icons.Default.School,
                        accentColor = colors.success,
                        onClick = {
                            viewModel.navigateTo(Screen.PracticeCompetenceSelect(isCrpMode = true))
                        }
                    )
                }
            } else if (selectedTrack == Track.GMDSS_GOC) {
                item {
                    ModeLauncherCard(
                        title = "MARINA CRP-Based Practice",
                        subtitle = "Standard review • 1,200 official review questions (C1: 1,000 [2 Parts, 500 each], C2: 200 items)\nNote: Independently developed review based on MARINA references.",
                        badge = "STANDARD REVIEW",
                        badgeColor = colors.success,
                        icon = Icons.Default.School,
                        accentColor = colors.success,
                        onClick = {
                            viewModel.navigateTo(Screen.PracticeCompetenceSelect(isCrpMode = true))
                        }
                    )
                }
            }

            item {
                ModeLauncherCard(
                    title = "Practice All Questions",
                    subtitle = if (selectedTrack == Track.DECK_OIC_NW) {
                        "Practice across all 12 competencies (6,455 questions available)."
                    } else {
                        "Practice across all competencies (2,260 questions available)."
                    },
                    badge = if (isTrackUnlocked) "CUSTOMIZABLE" else "VERIFICATION REQUIRED",
                    badgeColor = if (isTrackUnlocked) colors.foreground else colors.accent,
                    icon = Icons.Default.Tune,
                    accentColor = colors.foreground,
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
                    badgeColor = colors.accent,
                    icon = Icons.Default.Timer,
                    accentColor = colors.accent,
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
                    badgeColor = colors.foreground,
                    icon = Icons.Default.Style,
                    accentColor = colors.foreground,
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
                    badgeColor = if (totalMistakes > 0) colors.accent else colors.muted,
                    icon = Icons.Default.AutoFixHigh,
                    accentColor = colors.accent,
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
                    badgeColor = colors.foreground,
                    icon = Icons.Default.Analytics,
                    accentColor = colors.foreground,
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
                    badgeColor = colors.foreground,
                    icon = Icons.Default.Gavel,
                    accentColor = colors.foreground,
                    onClick = {
                        viewModel.navigateTo(Screen.ExamGuidelines)
                    }
                )
            }

            item {
                // MARINA Policy Notice Card
                MarinaPolicyCard(
                    onClick = { viewModel.navigateTo(Screen.ExamGuidelines) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Official Mock Exam Confirmation Modal
    if (showMockExamWarningDialog) {
        AlertDialog(
            onDismissRequest = { showMockExamWarningDialog = false },
            shape = RoundedCornerShape(2.dp),
            containerColor = colors.surface,
            title = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "START OFFICIAL TIMED MOCK EXAM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You are about to start an official MARINA format timed mock examination.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.foreground
                    )
                    Text(
                        text = "• Passing Cutoff: 70.0% per competence\n• Timing: 1 minute per item\n• Session lock-in applies once started",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            },
            confirmButton = {
                MianButton(
                    onClick = {
                        showMockExamWarningDialog = false
                        viewModel.startTimedMockExam(selectedTrack)
                    },
                    accent = true
                ) {
                    Text("START EXAM", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMockExamWarningDialog = false },
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("CANCEL", color = colors.muted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Search Locked Modal
    if (showSearchLockedDialog) {
        AlertDialog(
            onDismissRequest = { showSearchLockedDialog = false },
            shape = RoundedCornerShape(2.dp),
            containerColor = colors.surface,
            title = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "QUESTION BANK SEARCH LOCKED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground
                    )
                }
            },
            text = {
                Text(
                    text = "Full question bank search is available for verified terminals. Register your device to unlock keyword search across all competencies.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                MianButton(
                    onClick = {
                        showSearchLockedDialog = false
                        viewModel.navigateTo(Screen.HowToAvail)
                    },
                    accent = true
                ) {
                    Text("HELP & ACTIVATION", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSearchLockedDialog = false },
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("CANCEL", color = colors.muted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun HeroBanner(track: Track, isUnlocked: Boolean) {
    val colors = LocalSpaghettiColors.current

    MianRuleCard(
        ruleColor = colors.accent,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = colors.surface2
                ) {
                    Text(
                        text = "STCW '78 AS AMENDED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Start
                    )
                }

                ProStatusBadge(
                    isUnlocked = isUnlocked,
                    lockedLabel = "VERIFICATION REQUIRED",
                    unlockedLabel = "VERIFIED TERMINAL"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "HYPERION",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.foreground,
                letterSpacing = (-0.8).sp,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (track == Track.DECK_OIC_NW) {
                    "OIC-NW OPERATIONAL REVIEWER"
                } else {
                    "GMDSS RADIO OPERATOR REVIEWER"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accent,
                letterSpacing = (-0.2).sp,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (track == Track.DECK_OIC_NW) {
                    "Functions 1, 2, and 3 • 6,455 total questions across 12 competencies • Bloom's taxonomy weighted scoring"
                } else {
                    "GOC Standard Radio Equipment, Distress & Safety Procedures • 2,260 total questions available"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                textAlign = TextAlign.Start
            )
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
    val colors = LocalSpaghettiColors.current

    MianRuleCard(
        ruleColor = colors.border,
        containerColor = colors.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatColumn(
                title = "EXAMS TAKEN",
                value = "$examsCount",
                color = colors.foreground,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(colors.border)
            )

            StatColumn(
                title = "AVG SCORE",
                value = if (examsCount > 0) "${avgScore.toInt()}%" else "--",
                color = if (avgScore >= 70.0) colors.success else if (examsCount > 0) colors.accent else colors.foreground,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(colors.border)
            )

            StatColumn(
                title = "MISTAKES",
                value = "$mistakeCount",
                color = if (mistakeCount > 0) colors.accent else colors.foreground,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(colors.border)
            )

            StatColumn(
                title = "BOOKMARKS",
                value = "$bookmarkCount",
                color = colors.foreground,
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
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = LocalSpaghettiColors.current.muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Start
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
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    letterSpacing = (-0.1).sp,
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(2.dp),
                color = colors.surface2
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
private fun MarinaPolicyCard(
    onClick: () -> Unit = {}
) {
    val colors = LocalSpaghettiColors.current

    MianRuleCard(
        ruleColor = colors.border,
        containerColor = colors.surface,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side icon
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = colors.foreground
                )
                // Right side action label
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VIEW RULES",
                        color = Color(0xFFFF5A67),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "→",
                        color = Color(0xFFFF5A67),
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = "MARINA EVALUATION & WEIGHTED SCORING",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.foreground,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "Passing Threshold: 70.0% per competence. A candidate passes only if obtaining >= 70% in EVERY competence within the function. Easy: 3 pts, Moderate: 6 pts, Difficult: 9 pts. Tap to view full guidelines & permit requirements.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ResumeActiveSessionCard(
    session: ActiveSessionEntity,
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    val trackName = if (session.track == Track.DECK_OIC_NW.name) "OIC-NW" else if (session.track == Track.GMDSS_GOC.name) "GMDSS" else session.track.replace('_', ' ')
    val modeStr = if (session.mode.contains("MOCK")) "MOCK" else "PRACTICE"
    val progressFraction = session.answeredCount.toFloat() / session.totalQuestions.coerceAtLeast(1)

    MianRuleCard(
        ruleColor = colors.accent,
        containerColor = colors.surface
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
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONTINUE REVIEW",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accent,
                        letterSpacing = 0.6.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = colors.surface2
                ) {
                    Text(
                        text = "$trackName $modeStr",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "QUESTION ${session.answeredCount} / ${session.totalQuestions} • ${(progressFraction * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.foreground
            )

            Spacer(modifier = Modifier.height(8.dp))

            SpaghettiProgressBar(progress = progressFraction)

            if (session.timeRemainingSeconds > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val mins = session.timeRemainingSeconds / 60
                val secs = session.timeRemainingSeconds % 60
                Text(
                    text = "TIME REMAINING: ${mins}M ${String.format("%02d", secs)}S",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.muted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MianButton(
                    onClick = onResume,
                    accent = true,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONTINUE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                MianRuleButton(
                    onClick = onDiscard,
                    ruleColor = colors.border,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "DISCARD",
                        fontWeight = FontWeight.Bold,
                        color = colors.muted,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
