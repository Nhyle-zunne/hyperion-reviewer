package com.nhyle.hyperionmaritime.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.local.ActiveSessionEntity
import com.nhyle.hyperionmaritime.data.local.answeredQuestions
import com.nhyle.hyperionmaritime.data.local.isCompleted
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.ui.components.MianButton
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
    val regState by viewModel.registrationUiState.collectAsState()
    val rollingStreak by viewModel.rollingStreak.collectAsState()

    val totalExamsTaken = examRecords.size
    val averageScore = if (totalExamsTaken > 0) {
        examRecords.map { it.percentage }.average()
    } else 0.0

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
                        IconButton(onClick = { viewModel.navigateTo(Screen.History) }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = colors.foreground
                            )
                        }

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
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "STCW '78 AS AMENDED · MARINA CRP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            maxLines = 1
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
                            onDiscard = { viewModel.discardActiveSession() }
                        )
                    }
                }
            }

            // 4. Stats Strip (compact 3-cell bar)
            item {
                StatsStrip(
                    todayTime = "0:25",
                    streakDays = rollingStreak,
                    overallScore = averageScore
                )
            }

            // 5. Support Us Shortcut Card
            item {
                SupportUsShortcutCard(
                    onNavigateToStore = { viewModel.navigateTo(Screen.Store) }
                )
            }

            // 6. Compact Track List (Deck, GMDSS, Engine OIC-EW)
            // Track 1: DECK OIC-NW OPERATIONAL
            item {
                CompactTrackCard(
                    track = Track.DECK_OIC_NW,
                    title = "DECK OIC-NW OPERATIONAL",
                    subtitle = "6,445 QUESTIONS · 12 COMPETENCIES",
                    standardCode = "STCW REGULATION II/1",
                    isUnlocked = isOicNwProUnlocked,
                    timeLeft = regState.oicTimeLeft,
                    icon = Icons.Default.Explore,
                    breakdown = listOf(
                        "FUNCTION 1: Navigation at Operational Level (3,547 Qs)",
                        "FUNCTION 2: Cargo Handling & Stowage (1,031 Qs)",
                        "FUNCTION 3: Controlling Ship Operations (1,867 Qs)"
                    ),
                    onSelectTrack = {
                        viewModel.selectTrack(Track.DECK_OIC_NW)
                        viewModel.navigateTo(Screen.Home)
                    }
                )
            }

            // Track 2: GMDSS RADIO OPERATOR (GOC)
            item {
                CompactTrackCard(
                    track = Track.GMDSS_GOC,
                    title = "GMDSS RADIO OPERATOR (GOC)",
                    subtitle = "2,260 QUESTIONS · 2 COMPETENCIES",
                    standardCode = "STCW REGULATION IV/2",
                    isUnlocked = isGmdssProUnlocked,
                    timeLeft = regState.gmdssTimeLeft,
                    icon = Icons.Default.CellTower,
                    breakdown = listOf(
                        "COMPETENCE 1: Transmit & receive information using GMDSS subsystems and equipment and fulfilling the functional requirements of GMDSS (1,911 Qs)",
                        "COMPETENCE 2: Provide radio services in emergencies (349 Qs)"
                    ),
                    onSelectTrack = {
                        viewModel.selectTrack(Track.GMDSS_GOC)
                        viewModel.navigateTo(Screen.Home)
                    }
                )
            }

            // Track 3: ENGINE OIC-EW OPERATIONAL LEVEL
            item {
                CompactTrackCard(
                    track = Track.ENGINE_OIC_EW,
                    title = "ENGINE OIC-EW OPERATIONAL LEVEL",
                    subtitle = "STCW REGULATION III/1 · 4,800 Qs · 10 COMPETENCIES",
                    standardCode = "STCW REGULATION III/1",
                    isUnlocked = false,
                    badgeOverride = "COMING SOON",
                    icon = Icons.Default.Build,
                    breakdown = listOf(
                        "FUNCTION 1: Marine Engineering at Operational Level (1,800 Qs)",
                        "FUNCTION 2: Electrical, Electronic & Control Engineering (1,200 Qs)",
                        "FUNCTION 3: Maintenance & Repair (1,000 Qs)",
                        "FUNCTION 4: Controlling Ship Operations & Safety (800 Qs)"
                    ),
                    onSelectTrack = {
                        Toast.makeText(context, "Engine OIC-EW track is coming soon!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
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
    val trackLabel = when (session.track.uppercase()) {
        "DECK_OIC_NW" -> "DECK OIC-NW"
        "GMDSS_GOC" -> "GMDSS GOC"
        else -> session.track.replace('_', ' ')
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
                            text = "RESUME · $trackLabel ${session.mode.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Q ${session.answeredCount}/${session.totalQuestions} · ${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            fontSize = 10.sp
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
private fun StatsStrip(
    todayTime: String,
    streakDays: Int,
    overallScore: Double
) {
    val colors = LocalSpaghettiColors.current

    Surface(
        shape = RectangleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cell 1: TODAY
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
                Text(
                    text = todayTime,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(colors.border)
            )

            // Cell 2: STREAK
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "STREAK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.muted,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$streakDays DAYS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.accent,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(colors.border)
            )

            // Cell 3: OVERALL
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "OVERALL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.muted,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${overallScore.toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SupportUsShortcutCard(
    onNavigateToStore: () -> Unit
) {
    val colors = LocalSpaghettiColors.current

    Surface(
        shape = RectangleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToStore() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                        .background(colors.accent.copy(alpha = 0.12f), RectangleShape)
                        .border(1.dp, colors.accent, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Pass Store",
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "SUPPORT HYPERION · PASS STORE & UPGRADES",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 11.sp,
                        letterSpacing = (-0.1).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Unlock full question banks & offline licenses",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RectangleShape,
                color = colors.surface2,
                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "STORE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accent,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactTrackCard(
    track: Track,
    title: String,
    subtitle: String,
    standardCode: String,
    isUnlocked: Boolean,
    timeLeft: String? = null,
    badgeOverride: String? = null,
    icon: ImageVector,
    breakdown: List<String>,
    onSelectTrack: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RectangleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, if (isExpanded) colors.accent else colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(200))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsed Card Body (Tap toggles expand / collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 32x32dp Square Badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(colors.surface2, RectangleShape)
                        .border(1.dp, colors.border, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = colors.foreground,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title, Subtitle, & Status Badge Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 12.sp,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))

                    // License status and expiration info in a separate row beneath title & subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val badgeText = when {
                            badgeOverride != null -> badgeOverride
                            isUnlocked -> "VERIFIED"
                            else -> "UNVERIFIED"
                        }
                        val badgeColor = when {
                            isUnlocked -> PassGreen
                            badgeOverride != null -> colors.accent
                            else -> colors.muted
                        }

                        Surface(
                            shape = RectangleShape,
                            color = colors.surface2,
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = if (!timeLeft.isNullOrBlank() && isUnlocked) "$badgeText · $timeLeft" else badgeText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeColor,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expanded Breakdown & Action Pane
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface2)
                        .border(BorderStroke(1.dp, colors.border))
                        .padding(12.dp)
                ) {
                    Text(
                        text = standardCode,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accent,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        breakdown.forEach { item ->
                            Text(
                                text = "• $item",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.foreground,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    MianButton(
                        onClick = onSelectTrack,
                        accent = true,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (badgeOverride != null) "VIEW $title" else "ENTER ${if (track == Track.DECK_OIC_NW) "DECK" else "GMDSS"} REVIEW",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
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
