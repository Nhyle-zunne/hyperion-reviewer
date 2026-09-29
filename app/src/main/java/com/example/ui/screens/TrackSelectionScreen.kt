package com.example.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ActiveSessionEntity
import com.example.data.local.isCompleted
import com.example.data.local.answeredQuestions
import com.example.data.model.Track
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
 * Editorial MIAN "Spaghetti" Track Selection Homepage:
 * Heavy grotesk typography, 3dp top rules, high contrast flat surfaces, sharp 2dp badges.
 */
@Composable
fun TrackSelectionScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val activeSession by viewModel.activeSession.collectAsState()
    val examRecords by viewModel.examRecords.collectAsState()
    val isOicNwProUnlocked by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssProUnlocked by viewModel.isGmdssProUnlocked.collectAsState()
    val isActivated by viewModel.isActivated.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Editorial Top Bar
        Surface(
            color = colors.surface,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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

                        IconButton(onClick = { viewModel.navigateTo(Screen.Activation) }) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Activation",
                                tint = colors.foreground
                            )
                        }
                    }
                }

                // 3dp Header Rule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colors.border)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Hero Poster Banner
                MianRuleCard(
                    ruleColor = colors.accent,
                    containerColor = colors.surface
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = colors.surface2
                        ) {
                            Text(
                                text = "STCW '78 AS AMENDED • MARINA CRP",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground,
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "SELECT EXAMINATION TRACK",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            letterSpacing = (-0.6).sp,
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Independent licensure examination reviewer for Filipino Merchant Marine Officers and GOC Radio Operators. Select your qualification track below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            // Resume active session card (if exists)
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

            item {
                MianSectionHeader(title = "LICENSURE TRACKS")
            }

            // Track 1: OIC-NW Deck Operational Level
            item {
                TrackCard(
                    track = Track.DECK_OIC_NW,
                    title = "OIC-NW DECK OPERATIONAL LEVEL",
                    standardCode = "STCW REGULATION II/1",
                    description = "Officer in Charge of a Navigational Watch (Operational Level). 6,455 total questions across 12 competencies, 200 items benchmark mock examination.",
                    breakdown = listOf(
                        "FUNCTION 1: Navigation at Operational Level",
                        "FUNCTION 2: Cargo Handling & Stowage",
                        "FUNCTION 3: Controlling Ship Operation & Care for Persons",
                        "TOTAL QUESTION BANK: 6,455 questions across 12 competencies"
                    ),
                    isUnlocked = isOicNwProUnlocked || isActivated,
                    icon = Icons.Default.Explore,
                    onSelectTrack = {
                        viewModel.selectTrack(Track.DECK_OIC_NW)
                        viewModel.navigateTo(Screen.Home)
                    }
                )
            }

            // Track 2: GMDSS General Operator Certificate (GOC)
            item {
                TrackCard(
                    track = Track.GMDSS_GOC,
                    title = "GMDSS RADIO OPERATOR (GOC)",
                    standardCode = "STCW REGULATION IV/2",
                    description = "General Operator's Certificate for the Global Maritime Distress and Safety System. 2,260 total questions across all competencies, 100 items benchmark mock examination.",
                    breakdown = listOf(
                        "COMPETENCE 1: Radio Regulations & Subsystems",
                        "COMPETENCE 2: Distress, Urgency, Safety & Search and Rescue (SAR)",
                        "TOTAL QUESTION BANK: 2,260 questions across all competencies"
                    ),
                    isUnlocked = isGmdssProUnlocked || isActivated,
                    icon = Icons.Default.CellTower,
                    onSelectTrack = {
                        viewModel.selectTrack(Track.GMDSS_GOC)
                        viewModel.navigateTo(Screen.Home)
                    }
                )
            }

            item {
                MianSectionHeader(title = "EXAMINATION POLICIES & INFORMATION")
            }

            item {
                MianRuleCard(
                    ruleColor = colors.border,
                    containerColor = colors.surface,
                    onClick = { viewModel.navigateTo(Screen.ExamGuidelines) }
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
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "Guidelines",
                                tint = colors.foreground,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MARINA EXAM GUIDELINES & SCORING",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground,
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "TOS distribution, permit requirements, 70% competence cutoff, and Bloom's taxonomy weighted points.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                                textAlign = TextAlign.Start
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Go",
                            tint = colors.muted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: Track,
    title: String,
    standardCode: String,
    description: String,
    breakdown: List<String>,
    isUnlocked: Boolean,
    icon: ImageVector,
    onSelectTrack: () -> Unit
) {
    val colors = LocalSpaghettiColors.current

    MianRuleCard(
        ruleColor = colors.border,
        containerColor = colors.surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                        text = standardCode,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }

                ProStatusBadge(
                    isUnlocked = isUnlocked,
                    lockedLabel = "VERIFICATION REQUIRED",
                    unlockedLabel = "VERIFIED TERMINAL"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = colors.foreground,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    letterSpacing = (-0.2).sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface2)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                breakdown.forEach { item ->
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foreground,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            MianButton(
                onClick = onSelectTrack,
                accent = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ENTER ${if (track == Track.DECK_OIC_NW) "DECK" else "GMDSS"} REVIEW",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
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
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RESUME ACTIVE SESSION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accent,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = colors.surface2
                ) {
                    Text(
                        text = "${session.track.replace('_', ' ')} ${session.mode}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "QUESTION ${session.answeredCount} / ${session.totalQuestions} • ${(progressFraction * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.foreground
            )

            Spacer(modifier = Modifier.height(6.dp))

            SpaghettiProgressBar(progress = progressFraction)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MianButton(
                    onClick = onResume,
                    accent = true,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("CONTINUE", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                MianRuleButton(
                    onClick = onDiscard,
                    ruleColor = colors.border,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("DISCARD", fontWeight = FontWeight.Bold, color = colors.muted, fontSize = 12.sp)
                }
            }
        }
    }
}
