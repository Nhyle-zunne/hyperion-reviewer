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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
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
import com.nhyle.hyperionmaritime.ui.motion.HyperionDialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.local.ActiveSessionEntity
import com.nhyle.hyperionmaritime.data.local.isCompleted
import com.nhyle.hyperionmaritime.data.local.answeredQuestions
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.QuestionSettingsMenu
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
    val isDarkMode = colors.isDark
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
    var showMockFunctionSelectionDialog by remember { mutableStateOf(false) }
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

    val currentDensity = LocalDensity.current
    val textScale by viewModel.questionTextScale.collectAsState()
    val scaledDensity = remember(currentDensity, textScale) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * textScale
        )
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
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
            onBookmarksClick = {
                if (isTrackUnlocked) {
                    viewModel.navigateTo(Screen.Bookmarks)
                } else {
                    lockedFeatureName = "Bookmarked Questions"
                    showUpgradeRequiredDialog = true
                }
            },
            bookmarkCount = totalBookmarks,
            settingsMenu = {
                QuestionSettingsMenu(
                    viewModel = viewModel,
                    showOnlyTextSize = true
                )
            }
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

            // Compact Performance Overview Section (Tightened spacing)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(colors.border)
                    )
                    Text(
                        text = "PERFORMANCE OVERVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Start
                    )
                    StatsGrid(
                        examsCount = totalExamsTaken,
                        avgScore = averageScore,
                        mistakeCount = totalMistakes,
                        bookmarkCount = totalBookmarks
                    )
                }
            }

            // Compact Examination Modes Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(colors.border)
                    )
                    Text(
                        text = "EXAMINATION MODES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }

            val freeReviewerTitle = when (selectedTrack) {
                Track.DECK_OIC_NW -> "OIC-NW FREE REVIEWER"
                Track.GMDSS_GOC -> "GMDSS FREE REVIEWER"
                Track.ENGINE_OIC_EW -> "ENGINE OIC-EW FREE REVIEWER"
                Track.DECK_MGT -> "DECK MANAGEMENT FREE REVIEWER"
                Track.ENGINE_MGT -> "ENGINE MANAGEMENT FREE REVIEWER"
            }

            val freeReviewerSubtitle = when (selectedTrack) {
                Track.DECK_OIC_NW -> "2,065 questions across 12 competencies (F1, F2, F3)."
                Track.GMDSS_GOC -> {
                    val c1 = com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.OPERATIONAL_REVIEW_LIMIT_C1
                    val c2 = com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.OPERATIONAL_REVIEW_LIMIT_C2
                    "%,d questions across 2 operational competencies.".format(c1 + c2)
                }
                else -> {
                    val totalCount = com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.getTotalStandardReviewQuestionCount(selectedTrack, isProUnlocked = false)
                    val compCount = com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.getCompetencesForTrack(selectedTrack).size
                    "%,d questions across %d competencies.".format(totalCount, compCount)
                }
            }

            val compCount = MaritimeCompetenceRegistry.getCompetencesForTrack(selectedTrack).size
            val qCount = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(selectedTrack)
            val qCountStr = "%,d".format(qCount)

            // Row 1: Free Reviewer (Top-Left) + Practice All Questions (Top-Right)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CompactModeBox(
                            title = freeReviewerTitle,
                            subtitle = freeReviewerSubtitle,
                            badge = "FREE ACCESS",
                            badgeColor = colors.foreground,
                            icon = Icons.Default.School,
                            accentColor = colors.foreground,
                            isLocked = false,
                            onClick = {
                                if (selectedTrack == Track.GMDSS_GOC) {
                                    viewModel.navigateTo(Screen.PracticeCompetenceSelect(isCrpMode = false, isOperationalReview = true))
                                } else {
                                    viewModel.navigateTo(Screen.PracticeCompetenceSelect(isCrpMode = true))
                                }
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        CompactModeBox(
                            title = "PRACTICE ALL QUESTIONS",
                            subtitle = "$qCountStr questions across $compCount competencies.",
                            badge = if (isTrackUnlocked) "CUSTOMIZABLE" else "LOCKED",
                            badgeColor = if (isTrackUnlocked) colors.accent else colors.muted,
                            icon = Icons.Default.Tune,
                            accentColor = colors.accent,
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
                }
            }

            // Row 2: Quick Scan Material (Below Free Reviewer) + Timed Mock Exam (Below Practice All)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CompactModeBox(
                            title = "QUICK SCAN MATERIAL",
                            subtitle = "Direct question & answer reference review.",
                            badge = if (isTrackUnlocked) "QUICK SCAN" else "LOCKED",
                            badgeColor = if (isTrackUnlocked) colors.accent else colors.muted,
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            accentColor = colors.accent,
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

                    Box(modifier = Modifier.weight(1f)) {
                        CompactModeBox(
                            title = "TIMED MOCK EXAM",
                            subtitle = "70% Cutoff • 1 min/item • Function lock-in",
                            badge = if (isTrackUnlocked) "STCW MOCK" else "LOCKED",
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
                }
            }

            // Row 3: Flashcards + Mistakes Notebook
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CompactModeBox(
                            title = "FLASHCARD REVISION",
                            subtitle = "Active recall • Tap to flip cards • Key verification",
                            badge = if (isTrackUnlocked) "ACTIVE RECALL" else "LOCKED",
                            badgeColor = if (isTrackUnlocked) colors.accent else colors.muted,
                            icon = Icons.Default.Style,
                            accentColor = colors.accent,
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

                    Box(modifier = Modifier.weight(1f)) {
                        CompactModeBox(
                            title = "MISTAKES NOTEBOOK",
                            subtitle = "$totalMistakes questions to redeem. Cleared after 2 correct.",
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
                }
            }

            // Row 4: Session Log + Search Bank
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CompactModeBox(
                            title = "SESSION LOG",
                            subtitle = "Past mock exam attempts & diagnostics.",
                            badge = if (!isTrackUnlocked) "LOCKED" else if (examRecords.isNotEmpty()) "${examRecords.size} LOGS" else "EMPTY",
                            badgeColor = if (isTrackUnlocked) colors.accent else colors.muted,
                            icon = Icons.Default.Analytics,
                            accentColor = colors.accent,
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

                    Box(modifier = Modifier.weight(1f)) {
                        CompactModeBox(
                            title = "SEARCH QUESTION BANK",
                            subtitle = "Search by keyword, item code, or competence.",
                            badge = if (isTrackUnlocked) "DATABASE" else "LOCKED",
                            badgeColor = if (isTrackUnlocked) colors.accent else colors.muted,
                            icon = Icons.Default.Search,
                            accentColor = colors.accent,
                            isLocked = !isTrackUnlocked,
                            onClick = {
                                if (isTrackUnlocked) {
                                    viewModel.navigateTo(Screen.Search)
                                } else {
                                    lockedFeatureName = "Question Bank Search"
                                    showUpgradeRequiredDialog = true
                                }
                            }
                        )
                    }
                }
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
        HyperionDialog(
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
                        text = "Check the Bond Store to view available upgrade tracks and unlock full question banks.",
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
                        showMockFunctionSelectionDialog = true
                    },
                    accent = true
                ) {
                    Text("PROCEED", fontWeight = FontWeight.ExtraBold)
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

    // Function Selection Modal for Mock Exam
    if (showMockFunctionSelectionDialog) {
        val allTrackCompetences = remember(selectedTrack) {
            MaritimeCompetenceRegistry.getCompetencesForTrack(selectedTrack)
        }
        val distinctFunctions = remember(allTrackCompetences) {
            allTrackCompetences.map { it.functionId }.distinct()
        }
        var selectedFunctions by remember { mutableStateOf<Set<String>>(emptySet()) }
        val allSelected = distinctFunctions.isNotEmpty() && selectedFunctions.size == distinctFunctions.size

        AlertDialog(
            onDismissRequest = { showMockFunctionSelectionDialog = false },
            modifier = Modifier
                .border(BorderStroke(1.5.dp, colors.border), RectangleShape)
                .fillMaxWidth(0.95f),
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
                        text = "SELECT EXAM FUNCTIONS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose which functions to include in this Mock Examination. Nothing is selected by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Toggle All Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedFunctions.isEmpty()) "0 FUNCTIONS SELECTED" else "${selectedFunctions.size} OF ${distinctFunctions.size} SELECTED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.muted,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = if (allSelected) "DESELECT ALL" else "SELECT ALL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accent,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier
                                .clip(RectangleShape)
                                .clickable {
                                    selectedFunctions = if (allSelected) emptySet() else distinctFunctions.toSet()
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(distinctFunctions) { funcId ->
                            val isSelected = funcId in selectedFunctions
                            val funcTitle = MaritimeCompetenceRegistry.getFunctionTitle(funcId, selectedTrack)
                            val permitItems = MaritimeCompetenceRegistry.getPermitItemsForFunction(funcId, selectedTrack)

                            Surface(
                                shape = RectangleShape,
                                color = if (isSelected) {
                                    if (isDarkMode) Color(0xFF0F172A) else Color(0xFFDCFCE7)
                                } else {
                                    colors.surface2
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        if (isDarkMode) Color(0xFF22C55E) else Color(0xFF15803D)
                                    } else {
                                        colors.border
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RectangleShape)
                                    .clickable {
                                        selectedFunctions = if (isSelected) {
                                            selectedFunctions - funcId
                                        } else {
                                            selectedFunctions + funcId
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RectangleShape)
                                            .background(
                                                if (isSelected) {
                                                    if (isDarkMode) Color(0xFF22C55E) else Color(0xFF15803D)
                                                } else {
                                                    Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = if (isSelected) {
                                                    if (isDarkMode) Color(0xFF22C55E) else Color(0xFF15803D)
                                                } else {
                                                    colors.muted
                                                },
                                                shape = RectangleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = funcId.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) {
                                                    if (isDarkMode) Color(0xFF4ADE80) else Color(0xFF14532D)
                                                } else {
                                                    colors.foreground
                                                },
                                                fontSize = 11.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = "$permitItems items",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.muted,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = funcTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = colors.foreground,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                MianButton(
                    onClick = {
                        if (selectedFunctions.isNotEmpty()) {
                            showMockFunctionSelectionDialog = false
                            viewModel.startTimedMockExam(selectedTrack, selectedFunctions)
                        }
                    },
                    enabled = selectedFunctions.isNotEmpty(),
                    accent = true
                ) {
                    Text("PROCEED", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showMockFunctionSelectionDialog = false },
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
                .padding(16.dp)
        ) {
            // Tier 1: Authority Spec & Single Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text(
                        text = "STCW '78 AS AMENDED",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Start
                    )
                }

                val countdownText = timeLeft.orEmpty()
                val days = Regex("(\\d+)\\s+days?").find(countdownText)?.groupValues?.get(1)?.toIntOrNull()
                val hasSubDayCountdown = days == null && (
                    Regex("(\\d+)\\s+hrs?").containsMatchIn(countdownText) ||
                    Regex("(\\d+)\\s+mins?").containsMatchIn(countdownText) ||
                    countdownText.contains("< 1 min", ignoreCase = true)
                )
                val needsOnlineVerification = countdownText.contains("Reconnect to verify", ignoreCase = true)
                val isExpiredFlag = countdownText.contains("Expired", ignoreCase = true) ||
                    countdownText.contains("Tampered", ignoreCase = true)
                val statusLabel = when {
                    !isUnlocked -> "UNVERIFIED"
                    needsOnlineVerification -> "VERIFY ONLINE"
                    isExpiredFlag -> "EXPIRED"
                    days != null && days <= 7 -> "EXPIRING · ${days}D"
                    days != null -> "VERIFIED · ${days}D"
                    hasSubDayCountdown -> "EXPIRING · <1D"
                    else -> "VERIFIED TERMINAL"
                }
                ProStatusBadge(
                    isUnlocked = isUnlocked,
                    lockedLabel = "UNVERIFIED",
                    unlockedLabel = statusLabel
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tier 2: Category Eyebrow & Authoritative Track Title
            Text(
                text = "HYPERION MARITIME LICENSURE",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = colors.muted,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp
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
                fontWeight = FontWeight.Black,
                color = colors.foreground,
                fontSize = 15.sp,
                letterSpacing = (-0.2).sp,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tier 3: Structured Metadata Chips
            val qCount = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(track)
            val qCountStr = "%,d".format(qCount)
            val compCount = MaritimeCompetenceRegistry.getCompetencesForTrack(track).size
            val functionsStr = when (track) {
                Track.DECK_OIC_NW -> "FUNCTIONS 1-3"
                Track.GMDSS_GOC -> "GOC DISTRESS"
                Track.DECK_MGT -> "FUNCTIONS 1-3"
                else -> "FUNCTIONS 1-4"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "$qCountStr ITEMS",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        fontSize = 8.5.sp,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "$compCount COMPETENCIES",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        fontSize = 8.5.sp,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = functionsStr,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        fontSize = 8.5.sp,
                        maxLines = 1
                    )
                }
            }

            // Tier 4: Clean Expiry Footer (if unlocked with time remaining)
            if (isUnlocked && !timeLeft.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Access Pass: $timeLeft",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.accent,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                }
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
private fun CompactModeBox(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    accentColor: Color,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalSpaghettiColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp)
            .clip(RectangleShape)
            .background(colors.surface)
            .border(
                1.dp,
                if (isLocked) colors.border.copy(alpha = 0.5f) else colors.border,
                RectangleShape
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Accent Line (Store-style aesthetic)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(if (isLocked) colors.muted.copy(alpha = 0.35f) else accentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: Icon box + Monospace Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (isLocked) colors.surface2 else accentColor.copy(alpha = 0.12f),
                                RectangleShape
                            )
                            .border(
                                1.dp,
                                if (isLocked) colors.border.copy(alpha = 0.4f) else accentColor.copy(alpha = 0.3f),
                                RectangleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else icon,
                            contentDescription = null,
                            tint = if (isLocked) colors.muted else accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (isLocked) colors.surface2 else badgeColor.copy(alpha = 0.12f),
                                RectangleShape
                            )
                            .border(
                                1.dp,
                                if (isLocked) colors.border.copy(alpha = 0.4f) else badgeColor.copy(alpha = 0.35f),
                                RectangleShape
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            color = if (isLocked) colors.muted else badgeColor,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1
                        )
                    }
                }

                // Middle: Mode Title and Subtitle
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isLocked) colors.muted else colors.foreground,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        fontSize = 9.5.sp,
                        lineHeight = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom: Action status indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLocked) "REQUIRES UPGRADES" else "SELECT MODE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLocked) colors.muted.copy(alpha = 0.8f) else accentColor.copy(alpha = 0.85f),
                        letterSpacing = 0.5.sp
                    )

                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (isLocked) colors.muted.copy(alpha = 0.6f) else accentColor,
                        modifier = Modifier.size(11.dp)
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
