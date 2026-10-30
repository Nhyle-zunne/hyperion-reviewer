package com.nhyle.hyperionmaritime.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.CalendarMonth
import com.nhyle.hyperionmaritime.ui.motion.HyperionDialog
import androidx.compose.ui.window.DialogProperties
import android.app.DatePickerDialog
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.nhyle.hyperionmaritime.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.local.ActiveSessionEntity
import com.nhyle.hyperionmaritime.data.local.answeredQuestions
import com.nhyle.hyperionmaritime.data.local.isCompleted
import com.nhyle.hyperionmaritime.data.model.Track
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.nhyle.hyperionmaritime.ui.components.MianButton
import com.nhyle.hyperionmaritime.ui.components.QuestionSettingsMenu
import com.nhyle.hyperionmaritime.ui.components.SpaghettiProgressBar
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.ui.theme.PassGreen
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

/**
 * Compact MIAN "Spaghetti" Dashboard / Track Selection Screen:
 * Rectangular 0dp corners, 1dp borders, high-density layout. All 3 tracks fit without scrolling.
 */
@Composable
fun TrackSelectionScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val context = LocalContext.current
    val activeSession by viewModel.activeSession.collectAsState()
    val examRecords by viewModel.examRecords.collectAsState()
    val isOicNwProUnlocked by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssProUnlocked by viewModel.isGmdssProUnlocked.collectAsState()
    val isEwProUnlocked by viewModel.isEngineOicEwProUnlocked.collectAsState()
    val isDmProUnlocked by viewModel.isDeckMgtProUnlocked.collectAsState()
    val isEmProUnlocked by viewModel.isEngineMgtProUnlocked.collectAsState()
    val regState by viewModel.registrationUiState.collectAsState()
    val rollingStreak by viewModel.rollingStreak.collectAsState()
    val marinaExamTargetMillis by viewModel.marinaExamTargetMillis.collectAsState()

    val totalExamsTaken = examRecords.size
    val averageScore = if (totalExamsTaken > 0) {
        examRecords.map { it.percentage }.average()
    } else 0.0

    var selectedDetailsTrack by remember { mutableStateOf<Track?>(null) }

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
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
        // 1. Top Bar
        Surface(
            color = colors.surface,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.openDrawer() },
                        modifier = Modifier.testTag("track_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = colors.foreground
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "HYPERION",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "MARITIME LICENSURE REVIEWER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.muted,
                            fontSize = 9.sp,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QuestionSettingsMenu(
                            viewModel = viewModel,
                            showOnlyTextSize = true
                        )

                        IconButton(onClick = { viewModel.navigateTo(Screen.Store) }) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Pass Store",
                                tint = colors.foreground
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(colors.border)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // 2. Header Block (compact ~48dp)
                Surface(
                    shape = RectangleShape,
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = "SELECT EXAMINATION TRACK",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            letterSpacing = (-0.3).sp,
                            maxLines = 1,
                            softWrap = true
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "STCW '78 AS AMENDED · MARINA CRP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            softWrap = true
                        )
                    }
                }
            }

            // 3. Resume Strip (compact ~56dp, shown only when active session exists)
            activeSession?.let { session ->
                if (!session.isCompleted && session.answeredQuestions < session.totalQuestions) {
                    item {
                        ActiveSessionResumeRow(
                            session = session,
                            onResume = { viewModel.resumeSession(session.track, session.id) },
                            onDiscard = { viewModel.discardActiveSession(session.id, session.track) }
                        )
                    }
                }
            }

            // 4. Tactical Readiness & Streak Terminal Card
            item {
                TacticalReadinessTerminal(
                    viewModel = viewModel,
                    rollingStreak = rollingStreak,
                    marinaExamTargetMillis = marinaExamTargetMillis,
                    overallScore = averageScore
                )
            }

            // 5. 2x2 Grid of Square Tiles
            item {
                val deckTotal = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.DECK_OIC_NW)
                val gmdssTotal = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.GMDSS_GOC)
                val deckSeenCount = viewModel.getSeenQuestionIds(Track.DECK_OIC_NW).size
                val gmdssSeenCount = viewModel.getSeenQuestionIds(Track.GMDSS_GOC).size
                val deckProgress = if (deckTotal > 0) ((deckSeenCount.toFloat() / deckTotal.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
                val gmdssProgress = if (gmdssTotal > 0) ((gmdssSeenCount.toFloat() / gmdssTotal.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

                DashboardGrid(
                    viewModel = viewModel,
                    isOicNwProUnlocked = isOicNwProUnlocked,
                    isGmdssProUnlocked = isGmdssProUnlocked,
                    isEwProUnlocked = isEwProUnlocked,
                    isDmProUnlocked = isDmProUnlocked,
                    isEmProUnlocked = isEmProUnlocked,
                    regState = regState,
                    deckProgress = deckProgress,
                    gmdssProgress = gmdssProgress,
                    onSelectTrack = { track ->
                        viewModel.selectTrack(track)
                        viewModel.navigateTo(Screen.Home)
                    },
                    onOpenWebsite = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://hyperionmaritime.org")))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Unable to open website", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenDetails = { track ->
                        selectedDetailsTrack = track
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    selectedDetailsTrack?.let { track ->
        val isUnlocked = when (track) {
            Track.DECK_OIC_NW -> isOicNwProUnlocked
            Track.GMDSS_GOC -> isGmdssProUnlocked
            Track.ENGINE_OIC_EW -> isEwProUnlocked
            Track.DECK_MGT -> isDmProUnlocked
            Track.ENGINE_MGT -> isEmProUnlocked
        }
        val timeLeft = when (track) {
            Track.DECK_OIC_NW -> regState.oicTimeLeft
            Track.GMDSS_GOC -> regState.gmdssTimeLeft
            Track.ENGINE_OIC_EW -> regState.ewTimeLeft
            Track.DECK_MGT -> regState.dmTimeLeft
            Track.ENGINE_MGT -> regState.emTimeLeft
        }
        TrackDetailsPopup(
            track = track,
            isUnlocked = isUnlocked,
            timeLeft = timeLeft,
            onDismiss = { selectedDetailsTrack = null },
            onEnterReview = {
                viewModel.selectTrack(track)
                viewModel.navigateTo(Screen.Home)
                selectedDetailsTrack = null
            }
        )
    }

    }
}

@Composable
private fun ActiveSessionResumeRow(
    session: ActiveSessionEntity,
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    val progressFraction = session.answeredCount.toFloat() / session.totalQuestions.coerceAtLeast(1)
    val cleanTrack = session.track.removeSuffix("_MISTAKES")
    val trackLabel = when (cleanTrack.uppercase()) {
        "DECK_OIC_NW" -> "DECK OIC-NW"
        "GMDSS_GOC" -> "GMDSS GOC"
        else -> cleanTrack.replace('_', ' ')
    }

    Surface(
        shape = RectangleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.accent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(colors.accent.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, colors.accent, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RESUME · $trackLabel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            maxLines = 2,
                            softWrap = true
                        )
                        val modeDisplay = if (session.mode.equals("MISTAKES", ignoreCase = true) || session.competenceId.equals("MISTAKES", ignoreCase = true)) "MISTAKES" else session.mode.uppercase()
                        Text(
                            text = "$modeDisplay · Q ${session.answeredCount}/${session.totalQuestions} · ${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.accentForeground
                        ),
                        shape = RectangleShape,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = "CONTINUE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(colors.surface2, RectangleShape)
                            .border(1.dp, colors.border, RectangleShape)
                            .clickable { onDiscard() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Discard Session",
                            tint = colors.muted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            SpaghettiProgressBar(progress = progressFraction)
        }
    }
}



@Composable
private fun DashboardGrid(
    viewModel: ReviewerViewModel,
    isOicNwProUnlocked: Boolean,
    isGmdssProUnlocked: Boolean,
    isEwProUnlocked: Boolean,
    isDmProUnlocked: Boolean,
    isEmProUnlocked: Boolean,
    regState: ReviewerViewModel.DeviceRegistrationUiState,
    deckProgress: Int,
    gmdssProgress: Int,
    onSelectTrack: (Track) -> Unit,
    onOpenWebsite: () -> Unit,
    onOpenDetails: (Track) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val deckTotal = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.DECK_OIC_NW)
        val gmdssTotal = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.GMDSS_GOC)

        // Row 1: Deck & GMDSS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ActiveTrackTile(
                    track = Track.DECK_OIC_NW,
                    title = "DECK OIC-NW",
                    regulation = "STCW REGULATION II/1",
                    questionNumber = if (deckTotal >= 1000) "%,d".format(deckTotal) else deckTotal.toString(),
                    competencyAndMock = "12 COMPETENCIES · 200-ITEM MOCK",
                    progressPercent = deckProgress,
                    isUnlocked = isOicNwProUnlocked,
                    timeLeft = regState.oicTimeLeft,
                    icon = Icons.Default.Explore,
                    onClickTile = { onOpenDetails(Track.DECK_OIC_NW) },
                    onClickEnter = { onSelectTrack(Track.DECK_OIC_NW) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                ActiveTrackTile(
                    track = Track.GMDSS_GOC,
                    title = "GMDSS RADIO (GOC)",
                    regulation = "STCW REGULATION IV/2",
                    questionNumber = if (gmdssTotal >= 1000) "%,d".format(gmdssTotal) else gmdssTotal.toString(),
                    competencyAndMock = "2 COMPETENCIES · 100-ITEM MOCK",
                    progressPercent = gmdssProgress,
                    isUnlocked = isGmdssProUnlocked,
                    timeLeft = regState.gmdssTimeLeft,
                    icon = Icons.Default.CellTower,
                    onClickTile = { onOpenDetails(Track.GMDSS_GOC) },
                    onClickEnter = { onSelectTrack(Track.GMDSS_GOC) }
                )
            }
        }

        val ewTotal = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.ENGINE_OIC_EW)
        val ewSeen = viewModel.getSeenQuestionIds(Track.ENGINE_OIC_EW).size
        val ewProgress = if (ewTotal > 0) ((ewSeen.toFloat() / ewTotal.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

        val emTotal = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.ENGINE_MGT)
        val emSeen = viewModel.getSeenQuestionIds(Track.ENGINE_MGT).size
        val emProgress = if (emTotal > 0) ((emSeen.toFloat() / emTotal.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

        val dmTotal = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.DECK_MGT)
        val dmSeen = viewModel.getSeenQuestionIds(Track.DECK_MGT).size
        val dmProgress = if (dmTotal > 0) ((dmSeen.toFloat() / dmTotal.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

        // Row 2: ENGINE OIC-EW | ENGINE MANAGEMENT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ActiveTrackTile(
                    track = Track.ENGINE_OIC_EW,
                    title = "ENGINE OIC-EW",
                    regulation = "STCW REGULATION III/1",
                    questionNumber = if (ewTotal >= 1000) "%,d".format(ewTotal) else ewTotal.toString(),
                    competencyAndMock = "13 COMPETENCIES · 200-ITEM MOCK",
                    progressPercent = ewProgress,
                    isUnlocked = isEwProUnlocked,
                    timeLeft = regState.ewTimeLeft,
                    badgeOverride = null,
                    icon = Icons.Default.Build,
                    onClickTile = { onOpenDetails(Track.ENGINE_OIC_EW) },
                    onClickEnter = { onSelectTrack(Track.ENGINE_OIC_EW) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                ActiveTrackTile(
                    track = Track.ENGINE_MGT,
                    title = "ENGINE MANAGEMENT",
                    regulation = "STCW REGULATION III/2",
                    questionNumber = if (emTotal >= 1000) "%,d".format(emTotal) else emTotal.toString(),
                    competencyAndMock = "14 COMPETENCIES · 200-ITEM MOCK",
                    progressPercent = emProgress,
                    isUnlocked = isEmProUnlocked,
                    timeLeft = regState.emTimeLeft,
                    badgeOverride = null,
                    icon = Icons.Default.Construction,
                    onClickTile = { onOpenDetails(Track.ENGINE_MGT) },
                    onClickEnter = { onSelectTrack(Track.ENGINE_MGT) }
                )
            }
        }

        // Row 3: DECK MANAGEMENT | PassStoreTile
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ActiveTrackTile(
                    track = Track.DECK_MGT,
                    title = "DECK MANAGEMENT",
                    regulation = "STCW REGULATION II/2",
                    questionNumber = if (dmTotal >= 1000) "%,d".format(dmTotal) else dmTotal.toString(),
                    competencyAndMock = "18 COMPETENCIES · 200-ITEM MOCK",
                    progressPercent = dmProgress,
                    isUnlocked = isDmProUnlocked,
                    timeLeft = regState.dmTimeLeft,
                    badgeOverride = null,
                    icon = Icons.Default.Navigation,
                    onClickTile = { onOpenDetails(Track.DECK_MGT) },
                    onClickEnter = { onSelectTrack(Track.DECK_MGT) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                WebsiteTile(onClick = onOpenWebsite)
            }
        }
    }
}

@Composable
private fun ActiveTrackTile(
    track: Track,
    title: String,
    regulation: String,
    questionNumber: String,
    competencyAndMock: String,
    progressPercent: Int,
    isUnlocked: Boolean,
    timeLeft: String?,
    icon: ImageVector,
    badgeOverride: String? = null,
    onClickTile: () -> Unit,
    onClickEnter: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    val progressFraction = progressPercent.toFloat() / 100f

    Surface(
        shape = RectangleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .height(205.dp)
            .clickable { onClickTile() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(colors.surface2, RectangleShape)
                        .border(1.dp, colors.border, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = colors.foreground,
                        modifier = Modifier.size(16.dp)
                    )
                }

                val days = Regex("(\\d+)\\s+days?").find(timeLeft ?: "")?.groupValues?.get(1)?.toIntOrNull()
                val isExpiredFlag = timeLeft?.contains("Expired") == true || timeLeft?.contains("Tampered") == true || (days != null && days <= 0)
                val badgeText = when {
                    badgeOverride != null -> badgeOverride
                    !isUnlocked -> "FREE TIER"
                    isExpiredFlag -> "EXPIRED"
                    days != null && days <= 7 -> "EXPIRING · ${days}D"
                    days != null -> "VERIFIED · ${days}D"
                    else -> "VERIFIED"
                }
                val badgeColor = when {
                    badgeOverride != null -> if (isUnlocked) PassGreen else colors.muted
                    !isUnlocked -> colors.muted
                    badgeText.startsWith("EXPIRING") -> Color(0xFFF59E0B)
                    badgeText == "EXPIRED" -> Color(0xFFEF4444)
                    else -> PassGreen
                }

                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, badgeColor)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    fontSize = 11.sp,
                    letterSpacing = (-0.2).sp,
                    softWrap = true
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = regulation,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    fontSize = 9.sp,
                    softWrap = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = questionNumber,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    fontSize = 21.sp,
                    softWrap = true
                )
                Text(
                    text = "QUESTIONS AVAILABLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    softWrap = true
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = competencyAndMock,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    fontSize = 10.sp,
                    softWrap = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "PROGRESS $progressPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        softWrap = true
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.surface2, RectangleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                .background(colors.accent, RectangleShape)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.accent, RectangleShape)
                        .clickable { onClickEnter() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Enter Review",
                        tint = colors.accentForeground,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedEngineTile() {
    val colors = LocalSpaghettiColors.current

    Surface(
        shape = RectangleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.muted.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(colors.surface2, RectangleShape)
                        .border(1.dp, colors.muted.copy(alpha = 0.4f), RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = colors.muted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.muted.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "COMING SOON",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.muted,
                        fontSize = 8.sp,
                        letterSpacing = 0.5.sp,
                        softWrap = true
                    )
                }
            }

            Column {
                Text(
                    text = "ENGINE OIC-EW",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.muted,
                    fontSize = 11.sp,
                    letterSpacing = (-0.2).sp,
                    softWrap = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(colors.muted.copy(alpha = 0.3f), RectangleShape)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(colors.muted.copy(alpha = 0.3f), RectangleShape)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "QUESTION BANK IN PREPARATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    fontSize = 9.sp,
                    softWrap = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.surface2, RectangleShape)
                        .border(1.dp, colors.muted.copy(alpha = 0.4f), RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = colors.muted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WebsiteTile(
    onClick: () -> Unit
) {
    val colors = LocalSpaghettiColors.current

    Surface(
        shape = RectangleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(205.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Hyperion Maritime app logo",
                    modifier = Modifier.size(42.dp),
                    contentScale = ContentScale.Fit
                )

                Surface(
                    shape = RectangleShape,
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "OFFICIAL",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Column {
                Text(
                    text = "HYPERION MARITIME",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    fontSize = 12.sp,
                    letterSpacing = (-0.2).sp,
                    softWrap = true
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "OFFICIAL WEBSITE",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "hyperionmaritime.org",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    fontSize = 14.sp,
                    softWrap = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "VISIT WEBSITE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.surface2, RectangleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .background(colors.accent, RectangleShape)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.accent, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Hyperion Maritime website",
                        tint = colors.accentForeground,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackDetailsPopup(
    track: Track,
    isUnlocked: Boolean,
    timeLeft: String?,
    onDismiss: () -> Unit,
    onEnterReview: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    val competences = MaritimeCompetenceRegistry.getCompetencesForTrack(track)
    val count = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(track)
    val totalQuestions = if (count >= 1000) "%,d".format(count) else count.toString()
    val mockSize = when (track) {
        Track.DECK_OIC_NW -> 200
        Track.GMDSS_GOC -> 100
        else -> track.mockQuestions
    }
    val title = when (track) {
        Track.DECK_OIC_NW -> "DECK OIC-NW OPERATIONAL LEVEL"
        Track.GMDSS_GOC -> "GMDSS RADIO OPERATOR (GOC)"
        Track.ENGINE_OIC_EW -> "ENGINE OIC-EW OPERATIONAL LEVEL"
        Track.ENGINE_MGT -> "ENGINE MANAGEMENT LEVEL"
        Track.DECK_MGT -> "DECK MANAGEMENT LEVEL"
    }
    val regulation = when (track) {
        Track.DECK_OIC_NW -> "STCW REGULATION II/1"
        Track.GMDSS_GOC -> "STCW REGULATION IV/2"
        Track.ENGINE_OIC_EW -> "STCW REGULATION III/1"
        Track.ENGINE_MGT -> "STCW REGULATION III/2"
        Track.DECK_MGT -> "STCW REGULATION II/2"
    }
    val icon = when (track) {
        Track.DECK_OIC_NW -> Icons.Default.Explore
        Track.GMDSS_GOC -> Icons.Default.CellTower
        Track.ENGINE_OIC_EW -> Icons.Default.Build
        Track.ENGINE_MGT -> Icons.Default.Construction
        Track.DECK_MGT -> Icons.Default.Navigation
    }

    HyperionDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RectangleShape,
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(colors.accent)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(colors.surface2, RectangleShape)
                                    .border(1.dp, colors.border, RectangleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = colors.foreground,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.foreground,
                                    fontSize = 12.sp,
                                    softWrap = true
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = regulation,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accent,
                                    fontSize = 10.sp,
                                    softWrap = true
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val days = Regex("(\\d+)\\s+days?").find(timeLeft ?: "")?.groupValues?.get(1)?.toIntOrNull()
                                val isExpiredFlag = timeLeft?.contains("Expired") == true || timeLeft?.contains("Tampered") == true || (days != null && days <= 0)
                                val fullDetailsText = when {
                                    !isUnlocked -> "FREE TIER"
                                    isExpiredFlag -> "EXPIRED"
                                    days != null && days <= 7 -> "EXPIRING · ${days}D"
                                    days != null -> "VERIFIED · ${days}D"
                                    else -> "VERIFIED"
                                }
                                Text(
                                    text = fullDetailsText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) PassGreen else colors.muted,
                                    fontSize = 9.sp,
                                    softWrap = true
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(colors.surface2, RectangleShape)
                                .border(1.dp, colors.border, RectangleShape)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.foreground,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface2)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RectangleShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "QUESTIONS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.muted,
                                    fontSize = 9.sp,
                                    softWrap = true
                                )
                                Text(
                                    text = "$totalQuestions",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.foreground,
                                    fontSize = 13.sp,
                                    softWrap = true
                                )
                            }
                        }

                        Surface(
                            shape = RectangleShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "COMPETENCIES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.muted,
                                    fontSize = 9.sp,
                                    softWrap = true
                                )
                                Text(
                                    text = "${competences.size}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.accent,
                                    fontSize = 13.sp,
                                    softWrap = true
                                )
                            }
                        }

                        Surface(
                            shape = RectangleShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "MOCK EXAM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.muted,
                                    fontSize = 9.sp,
                                    softWrap = true
                                )
                                Text(
                                    text = "$mockSize Qs",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.foreground,
                                    fontSize = 13.sp,
                                    softWrap = true
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (track) {
                            Track.DECK_OIC_NW -> {
                                val functions = listOf(
                                    "FUNCTION 1 · NAVIGATION" to competences.filter { it.functionId == "F1" },
                                    "FUNCTION 2 · CARGO HANDLING" to competences.filter { it.functionId == "F2" },
                                    "FUNCTION 3 · SHIP OPERATIONS" to competences.filter { it.functionId == "F3" }
                                )

                                functions.forEach { (funcHeader, comps) ->
                                    item {
                                        Text(
                                            text = funcHeader,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.accent,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            softWrap = true
                                        )
                                    }
                                    comps.forEach { comp ->
                                        item {
                                            Surface(
                                                shape = RectangleShape,
                                                color = colors.surface2,
                                                border = BorderStroke(1.dp, colors.border),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "COMPETENCE ${comp.competenceId}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = colors.foreground,
                                                            fontSize = 10.sp,
                                                            softWrap = true
                                                        )
                                                        Text(
                                                            text = "${comp.officialTOSItems} Qs",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colors.accent,
                                                            fontSize = 10.sp,
                                                            softWrap = true
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = comp.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = colors.muted,
                                                        fontSize = 11.sp,
                                                        softWrap = true
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Track.GMDSS_GOC -> {
                                competences.forEach { comp ->
                                    item {
                                        Surface(
                                            shape = RectangleShape,
                                            color = colors.surface2,
                                            border = BorderStroke(1.dp, colors.border),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "COMPETENCE ${comp.competenceId}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = colors.foreground,
                                                        fontSize = 10.sp,
                                                        softWrap = true
                                                    )
                                                    val countText = if (comp.competenceId == "C1") "1,911 Qs" else "349 Qs"
                                                    Text(
                                                        text = countText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.accent,
                                                        fontSize = 10.sp,
                                                        softWrap = true
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = comp.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.muted,
                                                    fontSize = 11.sp,
                                                    softWrap = true
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                val functionGroups = competences.groupBy { it.functionId }
                                functionGroups.forEach { (funcId, comps) ->
                                    val funcNum = funcId.removePrefix("F")
                                    val funcTitle = MaritimeCompetenceRegistry.getFunctionTitle(funcId, track).uppercase()
                                    val funcHeader = "FUNCTION $funcNum · $funcTitle"
                                    item {
                                        Text(
                                            text = funcHeader,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.accent,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            softWrap = true
                                        )
                                    }
                                    comps.forEach { comp ->
                                        item {
                                            Surface(
                                                shape = RectangleShape,
                                                color = colors.surface2,
                                                border = BorderStroke(1.dp, colors.border),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "COMPETENCE ${comp.competenceId}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = colors.foreground,
                                                            fontSize = 10.sp,
                                                            softWrap = true
                                                        )
                                                        val loadedCount = MaritimeCompetenceRegistry.getDynamicQuestionCount(comp.competenceId, track, isProUnlocked = true)
                                                        Text(
                                                            text = "$loadedCount Qs",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colors.accent,
                                                            fontSize = 10.sp,
                                                            softWrap = true
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = comp.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = colors.muted,
                                                        fontSize = 11.sp,
                                                        softWrap = true
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))

                    Surface(
                        shape = RectangleShape,
                        color = colors.surface2,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            MianButton(
                                onClick = onEnterReview,
                                accent = true,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val buttonText = when (track) {
                                    Track.DECK_OIC_NW -> "ENTER DECK REVIEW"
                                    Track.GMDSS_GOC -> "ENTER GMDSS REVIEW"
                                    Track.ENGINE_OIC_EW -> "ENTER EW REVIEW"
                                    Track.ENGINE_MGT -> "ENTER ENGINE MGT REVIEW"
                                    Track.DECK_MGT -> "ENTER DECK MGT REVIEW"
                                }
                                Text(
                                    text = buttonText,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    softWrap = true
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticalReadinessTerminal(
    viewModel: ReviewerViewModel,
    rollingStreak: Int,
    marinaExamTargetMillis: Long,
    overallScore: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalSpaghettiColors.current
    val hypeBalance by viewModel.hypeBalance.collectAsState()
    val allStudySegments by remember { viewModel.getAllStudySegments() }.collectAsState(initial = emptyList())
    val todayStudyText = remember(allStudySegments) {
        com.nhyle.hyperionmaritime.data.studytime.StudyTimeCalculator.formatDurationHMM(
            com.nhyle.hyperionmaritime.data.studytime.StudyTimeCalculator.calculateTotalDurationMs(
                com.nhyle.hyperionmaritime.data.studytime.StudyTimeCalculator.filterSegmentsForDay(
                    allStudySegments,
                    java.time.LocalDate.now()
                )
            )
        )
    }

    val formattedDate = remember(marinaExamTargetMillis) {
        if (marinaExamTargetMillis > 0) {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            formatter.format(Date(marinaExamTargetMillis))
        } else {
            ""
        }
    }

    val daysRemaining = remember(marinaExamTargetMillis) {
        if (marinaExamTargetMillis > 0) {
            val now = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val target = Calendar.getInstance().apply {
                timeInMillis = marinaExamTargetMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val diffMs = target - now
            (diffMs / (1000 * 60 * 60 * 24)).toInt()
        } else {
            0
        }
    }

    val badgeText = when {
        marinaExamTargetMillis <= 0L -> "SET DATE →"
        daysRemaining > 0 -> "T-MINUS $daysRemaining DAYS"
        daysRemaining == 0 -> "TODAY!"
        else -> "${-daysRemaining} DAYS AGO"
    }

    Surface(
        shape = RectangleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top Section: Marina Exam Countdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                val calendar = Calendar.getInstance()
                                if (marinaExamTargetMillis > 0L) {
                                    calendar.timeInMillis = marinaExamTargetMillis
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selectedCalendar = Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth, 0, 0, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        viewModel.setMarinaExamTargetMillis(selectedCalendar.timeInMillis)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).apply {
                                    datePicker.minDate = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                }.show()
                            },
                            onLongPress = {
                                viewModel.clearMarinaExamTarget()
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MARINA LICENSURE EXAM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (marinaExamTargetMillis > 0L) "TARGET: $formattedDate" else "SET TARGET DATE",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.foreground,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RectangleShape,
                    color = if (marinaExamTargetMillis > 0L) colors.surface2 else colors.accent,
                    border = BorderStroke(1.dp, colors.accent),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (marinaExamTargetMillis > 0L) colors.accent else colors.background,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            Spacer(modifier = Modifier.height(10.dp))

            // Middle Section: Study Telemetry Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Col 1: TODAY
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = todayStudyText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 13.sp
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(colors.border))

                // Col 2: OVERALL ACCURACY
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "OVERALL ACCURACY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${overallScore.toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 13.sp
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(colors.border))

                // Col 3: OFFLINE VAULT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "OFFLINE VAULT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "$hypeBalance HYPE",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accent,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Section: Integrated 7-Day Streak HUD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                viewModel.navigateTo(Screen.Streak)
                            }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROLLING 7-DAY STREAK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${rollingStreak.coerceIn(0, 7)} / 7 DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 7-Day Box Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..7) {
                        val isCompleted = i <= rollingStreak
                        val isCurrent = i == (rollingStreak + 1).coerceAtMost(7)
                        val boxColor = if (isCompleted) colors.accent.copy(alpha = 0.15f) else colors.surface2
                        val borderColor = if (isCompleted || isCurrent) colors.accent else colors.border

                        Surface(
                            shape = RectangleShape,
                            color = boxColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = if (i == 7) "D7" else "D$i",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCompleted) colors.accent else colors.muted
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier.height(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (i == 7) {
                                            Text(
                                                text = "+20H",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.accent
                                            )
                                        } else if (isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "-",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = colors.muted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


