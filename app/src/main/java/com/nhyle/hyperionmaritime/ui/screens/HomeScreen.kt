package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.local.ActiveSessionEntity
import com.nhyle.hyperionmaritime.data.local.isCompleted
import com.nhyle.hyperionmaritime.data.local.answeredQuestions
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianButton
import com.nhyle.hyperionmaritime.ui.components.MianRuleButton
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.MianSectionHeader
import com.nhyle.hyperionmaritime.ui.components.ProStatusBadge
import com.nhyle.hyperionmaritime.ui.components.SpaghettiProgressBar
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

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
    val isEngineOicEwProUnlocked by viewModel.isEngineOicEwProUnlocked.collectAsState()
    val isDeckMgtProUnlocked by viewModel.isDeckMgtProUnlocked.collectAsState()
    val isEngineMgtProUnlocked by viewModel.isEngineMgtProUnlocked.collectAsState()
    val isHyperionProUnlocked by viewModel.isHyperionProUnlocked.collectAsState()
    val isTrackUnlocked = when (selectedTrack) {
        Track.DECK_OIC_NW -> isOicNwProUnlocked
        Track.GMDSS_GOC -> isGmdssProUnlocked
        Track.ENGINE_OIC_EW -> isEngineOicEwProUnlocked
        Track.DECK_MGT -> isDeckMgtProUnlocked
        Track.ENGINE_MGT -> isEngineMgtProUnlocked
    }
    val isActivated by viewModel.isActivated.collectAsState()
    val isAnyProUnlocked = isOicNwProUnlocked || isGmdssProUnlocked || isEngineOicEwProUnlocked || isDeckMgtProUnlocked || isEngineMgtProUnlocked || isHyperionProUnlocked || isActivated
    val regState by viewModel.registrationUiState.collectAsState()
    val trackTimeLeft = when (selectedTrack) {
        Track.DECK_OIC_NW -> regState.oicTimeLeft
        Track.GMDSS_GOC -> regState.gmdssTimeLeft
        Track.ENGINE_OIC_EW -> regState.ewTimeLeft
        Track.DECK_MGT -> regState.dmTimeLeft
        Track.ENGINE_MGT -> regState.emTimeLeft
    }
    var showMockExamWarningDialog by remember { mutableStateOf(false) }
    var showSearchLockedDialog by remember { mutableStateOf(false) }
    var showUpgradeRequiredDialog by remember { mutableStateOf(false) }
    var lockedFeatureName by remember { mutableStateOf("") }

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
            title = when (selectedTrack) {
                Track.DECK_OIC_NW -> "OIC-NW Operational Track"
                Track.GMDSS_GOC -> "GMDSS Radio Track"
                Track.ENGINE_OIC_EW -> "Engine OIC-EW Track"
                Track.ENGINE_MGT -> "Engine Management Track"
                Track.DECK_MGT -> "Deck Management Track"
            },
            showBackButton = false,
            onMenuClick = { viewModel.openDrawer() },
            onLegalClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) },
            onSearchClick = {
                if (isTrackUnlocked) {
                    viewModel.navigateTo(Screen.Search)
                } else {
                    lockedFeatureName = "Question Bank Search"
                    showUpgradeRequiredDialog = true
                }
            },
            isSearchLocked = !isTrackUnlocked,
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
                HeroBanner(
                    track = selectedTrack,
                    isUnlocked = isTrackUnlocked,
                    timeLeft = trackTimeLeft
                )
            }

            // Resume Active Session Card (if unfinished session exists)
            activeSession?.let { session ->
                if (!session.isCompleted && session.answeredQuestions < session.totalQuestions) {
                    item {
                        ResumeActiveSessionCard(
                            session = session,
                            onResume = { viewModel.resumeSession(session.track, session.id) },
                            onDiscard = { viewModel.discardActiveSession(session.id, session.track) }
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
                val c1Val = com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.OPERATIONAL_REVIEW_LIMIT_C1
                val c2Val = com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.OPERATIONAL_REVIEW_LIMIT_C2
                val totalVal = c1Val + c2Val
                val totalStr = "%,d".format(totalVal)
                val c1Str = c1Val.toString()
                val c2Str = c2Val.toString()
                item {
                    ModeLauncherCard(
                        title = "GMDSS Operational Review",
                        subtitle = "Standard review • $totalStr questions across 2 operational competencies (C1: $c1Str questions, C2: $c2Str questions)",
                        badge = "STANDARD REVIEW",
                        badgeColor = colors.success,
                        icon = Icons.Default.School,
                        accentColor = colors.success,
                        onClick = {
                            viewModel.navigateTo(Screen.PracticeCompetenceSelect(isCrpMode = false, isOperationalReview = true))
                        }
                    )
                }
            } else {
                val totalCount = com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.getTotalStandardReviewQuestionCount(selectedTrack, isProUnlocked = false)
                val totalStr = "%,d".format(totalCount)
                val compCount = com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.getCompetencesForTrack(selectedTrack).size
                item {
                    ModeLauncherCard(
                        title = when (selectedTrack) {
                            Track.ENGINE_OIC_EW -> "ENGINE OIC-EW OPERATIONAL REVIEW"
                            Track.DECK_MGT -> "DECK MANAGEMENT STANDARD REVIEW"
                            Track.ENGINE_MGT -> "ENGINE MANAGEMENT STANDARD REVIEW"
                            else -> "Standard Review"
                        },
                        subtitle = "Standard review • $totalStr questions across $compCount competencies",
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
                val compCount = MaritimeCompetenceRegistry.getCompetencesForTrack(selectedTrack).size
                val qCount = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(selectedTrack)
                val qCountStr = "%,d".format(qCount)

                ModeLauncherCard(
                    title = "Practice All Questions",
                    subtitle = "Practice across all $compCount competencies ($qCountStr questions available).",
                    badge = if (isTrackUnlocked) "CUSTOMIZABLE" else "LOCKED",
                    badgeColor = if (isTrackUnlocked) colors.foreground else colors.muted,
                    icon = Icons.Default.Tune,
                    accentColor = colors.foreground,
                    isLuminous = isTrackUnlocked,
                    isLocked = !isTrackUnlocked,
                    onClick = {
                        if (isTrackUnlocked) {
                            viewModel.navigateTo(Screen.PracticeCompetenceSelect(isCrpMode = false, isOperationalReview = false))
                        } else {
                            lockedFeatureName = "Practice All Questions"
                            showUpgradeRequiredDialog = true
                        }
                    }
                )
            }

            item {
                ModeLauncherCard(
                    title = "Official Timed Mock Exam",
                    subtitle = when (selectedTrack) {
                        Track.DECK_OIC_NW -> "Competency-based timing • 1 min/item • 70% Cutoff"
                        Track.GMDSS_GOC -> "Competency-based timing • Operator standard • 70% Cutoff"
                        Track.ENGINE_OIC_EW, Track.DECK_MGT, Track.ENGINE_MGT -> "Competency-based timing • 1 min/item • 70% Cutoff"
                    },
                    badge = if (isTrackUnlocked) "STCW TOS MOCK" else "LOCKED",
                    badgeColor = if (isTrackUnlocked) colors.accent else colors.muted,
                    icon = Icons.Default.Timer,
                    accentColor = colors.accent,
                    isLocked = !isTrackUnlocked,
                    onClick = {
                        if (isTrackUnlocked) {
                            showMockExamWarningDialog = true
                        } else {
                            lockedFeatureName = "Official Timed Mock Exam"
                            showUpgradeRequiredDialog = true
                        }
                    }
                )
            }

            item {
                ModeLauncherCard(
                    title = "Quick Scan Material",
                    subtitle = "Direct question & answer reference review",
                    badge = if (isTrackUnlocked) "QUICK SCAN" else "LOCKED",
                    badgeColor = if (isTrackUnlocked) colors.success else colors.muted,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    accentColor = if (isTrackUnlocked) colors.success else colors.accent,
                    isLocked = !isTrackUnlocked,
                    onClick = {
                        if (isTrackUnlocked) {
                            viewModel.navigateTo(Screen.QuickScanCompetenceSelect(selectedTrack))
                        } else {
                            lockedFeatureName = "Quick Scan Material"
                            showUpgradeRequiredDialog = true
                        }
                    }
                )
            }

            item {
                ModeLauncherCard(
                    title = "Flashcard Revision Mode",
                    subtitle = "Active recall study • Tap to flip cards • Key verification & Bloom's taxonomy breakdown",
                    badge = if (isTrackUnlocked) "ACTIVE RECALL" else "LOCKED",
                    badgeColor = if (isTrackUnlocked) colors.foreground else colors.muted,
                    icon = Icons.Default.Style,
                    accentColor = colors.foreground,
                    isLocked = !isTrackUnlocked,
                    onClick = {
                        if (isTrackUnlocked) {
                            viewModel.navigateTo(Screen.Flashcards)
                        } else {
                            lockedFeatureName = "Flashcard Revision Mode"
                            showUpgradeRequiredDialog = true
                        }
                    }
                )
            }

            item {
                ModeLauncherCard(
                    title = "Mistakes Notebook (Vault)",
                    subtitle = "$totalMistakes questions to redeem. Removed after 2 consecutive correct answers.",
                    badge = if (!isTrackUnlocked) "LOCKED" else if (totalMistakes > 0) "$totalMistakes ITEMS" else "EMPTY",
                    badgeColor = if (isTrackUnlocked && totalMistakes > 0) colors.accent else colors.muted,
                    icon = Icons.Default.AutoFixHigh,
                    accentColor = colors.accent,
                    isLocked = !isTrackUnlocked,
                    onClick = {
                        if (isTrackUnlocked) {
                            viewModel.navigateTo(Screen.Vault)
                        } else {
                            lockedFeatureName = "Mistakes Notebook (Vault)"
                            showUpgradeRequiredDialog = true
                        }
                    }
                )
            }

            item {
                ModeLauncherCard(
                    title = "Session Log",
                    subtitle = "Past mock exam attempts, Bloom's scoring, and 70% competence diagnostics",
                    badge = if (!isTrackUnlocked) "LOCKED" else if (examRecords.isNotEmpty()) "${examRecords.size} LOGS" else "EMPTY",
                    badgeColor = if (isTrackUnlocked) colors.foreground else colors.muted,
                    icon = Icons.Default.Analytics,
                    accentColor = colors.foreground,
                    isLocked = !isTrackUnlocked,
                    onClick = {
                        if (isTrackUnlocked) {
                            viewModel.navigateTo(Screen.History)
                        } else {
                            lockedFeatureName = "Session Log & History"
                            showUpgradeRequiredDialog = true
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Professional Chegg-style Upgrade Required Dialog
    if (showUpgradeRequiredDialog) {
        val trackTitle = when (selectedTrack) {
            Track.DECK_OIC_NW -> "OIC-NW Deck"
            Track.GMDSS_GOC -> "GMDSS Radio Operator"
            Track.ENGINE_OIC_EW -> "Engine OIC-EW"
            Track.ENGINE_MGT -> "Engine Management"
            Track.DECK_MGT -> "Deck Management"
        }
        Dialog(
            onDismissRequest = { showUpgradeRequiredDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .widthIn(max = 420.dp)
                    .background(colors.surface)
                    .border(1.5.dp, colors.border, RectangleShape)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(colors.surface2)
                                .border(1.dp, colors.border),
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
                        Text(
                            text = "UPGRADE REQUIRED",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Access to $lockedFeatureName requires an active $trackTitle license pass.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check the Hyperion Store to view available upgrade tracks and unlock full question banks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            onClick = {
                                showUpgradeRequiredDialog = false
                                viewModel.navigateTo(Screen.Store)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RectangleShape,
                            color = colors.accent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "YES",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.accentForeground,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Surface(
                            onClick = { showUpgradeRequiredDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RectangleShape,
                            color = colors.surface2,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "NO",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.foreground,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Official Mock Exam Confirmation Modal
    if (showMockExamWarningDialog) {
        AlertDialog(
            onDismissRequest = { showMockExamWarningDialog = false },
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            shape = RectangleShape,
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
                OutlinedButton(
                    onClick = { showMockExamWarningDialog = false },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("CANCEL", color = colors.foreground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Search Locked Modal
    if (showSearchLockedDialog) {
        AlertDialog(
            onDismissRequest = { showSearchLockedDialog = false },
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            shape = RectangleShape,
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
                OutlinedButton(
                    onClick = { showSearchLockedDialog = false },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("CANCEL", color = colors.foreground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun HeroBanner(track: Track, isUnlocked: Boolean, timeLeft: String? = null) {
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
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.border)
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

                val days = Regex("(\\d+)\\s+days?").find(timeLeft ?: "")?.groupValues?.get(1)?.toIntOrNull()
                val isExpiredFlag = timeLeft?.contains("Expired") == true || timeLeft?.contains("Tampered") == true || (days != null && days <= 0)
                val statusLabel = when {
                    !isUnlocked -> "UNVERIFIED"
                    isExpiredFlag -> "EXPIRED"
                    days != null && days <= 7 -> "EXPIRING · ${days}D"
                    days != null -> "VERIFIED · ${days}D"
                    else -> "VERIFIED TERMINAL"
                }
                ProStatusBadge(
                    isUnlocked = isUnlocked,
                    lockedLabel = "UNVERIFIED",
                    unlockedLabel = statusLabel
                )
            }

            if (isUnlocked && !timeLeft.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF00E599).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "⏱ TIME LEFT: $timeLeft",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color(0xFF00E599),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
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
                text = when (track) {
                    Track.DECK_OIC_NW -> "OIC-NW OPERATIONAL REVIEWER"
                    Track.GMDSS_GOC -> "GMDSS RADIO OPERATOR REVIEWER"
                    Track.ENGINE_OIC_EW -> "OIC-EW OPERATIONAL REVIEWER"
                    Track.DECK_MGT -> "DECK MANAGEMENT REVIEWER"
                    Track.ENGINE_MGT -> "ENGINE MANAGEMENT REVIEWER"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.accent,
                letterSpacing = (-0.2).sp,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(6.dp))

            val qCount = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(track)
            val qCountStr = "%,d".format(qCount)
            val compCount = MaritimeCompetenceRegistry.getCompetencesForTrack(track).size

            Text(
                text = when (track) {
                    Track.DECK_OIC_NW -> "Functions 1, 2, and 3 • $qCountStr total questions across $compCount competencies • Bloom's taxonomy weighted scoring"
                    Track.GMDSS_GOC -> "GOC Standard Radio Equipment, Distress & Safety Procedures • $qCountStr total questions available"
                    Track.ENGINE_OIC_EW, Track.DECK_MGT, Track.ENGINE_MGT -> {
                        val funcList = if (track == Track.DECK_MGT) "1, 2, and 3" else "1, 2, 3, and 4"
                        "Functions $funcList • $qCountStr total questions across $compCount competencies • Bloom's taxonomy weighted scoring"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                lineHeight = 18.sp,
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
    isLuminous: Boolean = false,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    val isDark = colors.isDark

    if (isLuminous) {
        val infiniteTransition = rememberInfiniteTransition(label = "luminousTransition")

        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        val shimmerOffset by infiniteTransition.animateFloat(
            initialValue = -300f,
            targetValue = 1200f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        )

        val mintAura1 = if (isDark) Color(0xFF10B981) else Color(0xFF15803D)
        val cyanAura2 = if (isDark) Color(0xFF06D6A0) else Color(0xFF16A34A)
        val brightCyanHighlight = if (isDark) Color(0xFF00FFA3) else Color(0xFF15803D)
        val baseSurface = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)

        val sweepBorderBrush = if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    mintAura1.copy(alpha = 0.6f),
                    brightCyanHighlight.copy(alpha = glowAlpha),
                    cyanAura2.copy(alpha = 0.7f),
                    brightCyanHighlight.copy(alpha = 0.9f),
                    mintAura1.copy(alpha = 0.6f)
                ),
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 400f, 200f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF15803D),
                    Color(0xFF16A34A),
                    Color(0xFF15803D)
                )
            )
        }

        val containerSurfaceBrush = if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    baseSurface,
                    Color(0xFF14221C),
                    baseSurface
                ),
                start = Offset(shimmerOffset * 0.5f, 0f),
                end = Offset(shimmerOffset * 0.5f + 500f, 200f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF1F5F9),
                    Color(0xFFE2E8F0),
                    Color(0xFFF1F5F9)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isDark) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .blur(20.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    mintAura1.copy(alpha = glowAlpha * 0.55f),
                                    cyanAura2.copy(alpha = glowAlpha * 0.70f),
                                    brightCyanHighlight.copy(alpha = glowAlpha * 0.55f)
                                )
                            ),
                            shape = RectangleShape
                        )
                )
            }

            MianRuleCard(
                ruleColor = if (isDark) brightCyanHighlight else Color(0xFF15803D),
                containerColor = Color.Transparent,
                border = BorderStroke(1.5.dp, sweepBorderBrush),
                onClick = onClick
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(containerSurfaceBrush)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RectangleShape)
                                    .background(if (isDark) Color(0xFF064E3B).copy(alpha = 0.7f) else Color(0xFFE2E8F0))
                                    .border(
                                        width = 1.dp,
                                        color = if (isDark) brightCyanHighlight.copy(alpha = glowAlpha) else Color(0xFF15803D),
                                        shape = RectangleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = if (isDark) brightCyanHighlight else Color(0xFF15803D),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                letterSpacing = (-0.1).sp,
                                textAlign = TextAlign.Start
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Surface(
                            shape = RectangleShape,
                            color = if (isDark) mintAura1.copy(alpha = 0.2f) else Color(0xFF15803D).copy(alpha = 0.12f),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) brightCyanHighlight.copy(alpha = 0.6f) else Color(0xFF15803D)
                            )
                        ) {
                            Text(
                                text = badge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) brightCyanHighlight else Color(0xFF15803D),
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = subtitle,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF15803D),
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    } else {
        val containerBg = if (isLocked) {
            if (isDark) Color(0xFF0B0F17) else Color(0xFFE2E8F0)
        } else {
            colors.surface
        }
        val ruleBorder = if (isLocked) {
            if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
        } else {
            colors.border
        }
        val cardIcon = if (isLocked) Icons.Default.Lock else icon
        val iconTint = if (isLocked) colors.accent else accentColor

        MianRuleCard(
            ruleColor = ruleBorder,
            containerColor = containerBg,
            onClick = onClick
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Row: Left has Icon + Title, Right has anchored Badge Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RectangleShape)
                                .background(if (isLocked) (if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)) else colors.surface2)
                                .border(1.dp, ruleBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = cardIcon,
                                contentDescription = title,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = title.uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLocked) (if (isDark) colors.muted else Color(0xFF334155)) else colors.foreground,
                            letterSpacing = (-0.1).sp,
                            textAlign = TextAlign.Start
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = RectangleShape,
                        color = if (isLocked) (if (isDark) colors.surface2 else Color(0xFFCBD5E1).copy(alpha = 0.5f)) else colors.surface2,
                        border = BorderStroke(1.dp, ruleBorder)
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor,
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Description Column placed below the header row with Modifier.fillMaxWidth()
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = subtitle,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLocked) (if (isDark) colors.muted.copy(alpha = 0.7f) else Color(0xFF475569)) else colors.muted,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Start
                    )
                }
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
    val cleanTrack = session.track.removeSuffix("_MISTAKES")
    val trackName = when (cleanTrack) {
        Track.DECK_OIC_NW.name -> "OIC-NW"
        Track.GMDSS_GOC.name -> "GMDSS"
        Track.ENGINE_OIC_EW.name -> "EW"
        Track.DECK_MGT.name -> "DECK MGT"
        Track.ENGINE_MGT.name -> "ENGINE MGT"
        else -> cleanTrack.replace('_', ' ')
    }
    val modeStr = if (session.mode.contains("MOCK")) "MOCK" else if (session.mode.contains("MISTAKES") || session.competenceId == "MISTAKES") "Mistakes" else "PRACTICE"
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
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.border)
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
