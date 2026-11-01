package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.MianSectionHeader
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

/**
 * Screen 1: Quick Scan Competence Selection (Bypass Drill Setup)
 * Lists all competencies for the selected track with total question counts.
 * Tapping a competence directly launches the Quick Scan Viewer initialized to Question #1.
 */
@Composable
fun QuickScanCompetenceSelectScreen(
    viewModel: ReviewerViewModel,
    track: Track? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val currentSelectedTrack by viewModel.selectedTrack.collectAsState()
    val questionBankLoaded by MaritimeQuestionBank.isLoadedFlow.collectAsState()
    val activeTrack = track ?: currentSelectedTrack

    val competences = MaritimeCompetenceRegistry.getCompetencesForTrack(activeTrack)

    BackHandler {
        viewModel.navigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val trackLabel = when (activeTrack) {
            Track.DECK_OIC_NW -> "OIC-NW DECK"
            Track.GMDSS_GOC -> "GMDSS GOC"
            Track.ENGINE_OIC_EW -> "ENGINE OIC-EW"
            Track.ENGINE_MGT -> "ENGINE MANAGEMENT"
            Track.DECK_MGT -> "DECK MANAGEMENT"
        }
        MaritimeTopBar(
            title = "QUICK SCAN MATERIAL",
            subtitle = "SELECT COMPETENCE • $trackLabel",
            showBackButton = true,
            onBackClick = { viewModel.navigateBack() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                MianRuleCard(
                    ruleColor = colors.accent,
                    containerColor = colors.surface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "DIRECT QUESTION & ANSWER REFERENCE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            letterSpacing = (-0.2).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sequential read-only review with correct answers pre-highlighted. Tap any competence below to immediately open the reader at Question #1 without drill configuration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                MianSectionHeader(title = "COMPETENCE DIRECTORY (${competences.size})")
            }

            // Check if there is a last visited competence for this track
            val lastCompId = viewModel.getQuickScanLastCompetence(activeTrack)
            val lastIndex = if (lastCompId != null) viewModel.getQuickScanLastIndex(activeTrack, lastCompId) else 0
            val lastCompInfo = competences.find { it.competenceId == lastCompId }

            if (lastCompId != null && lastIndex > 0 && lastCompInfo != null) {
                item {
                    MianRuleCard(
                        ruleColor = Color(0xFFFF9800),
                        containerColor = colors.surface
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RESUME QUICK SCAN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFF9800),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "QUESTION #${lastIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.muted
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${lastCompInfo.code}: ${lastCompInfo.title}".uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            androidx.compose.material3.Button(
                                onClick = {
                                    viewModel.navigateTo(
                                        Screen.QuickScanViewer(
                                            competenceId = lastCompId,
                                            track = activeTrack
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF9800),
                                    contentColor = Color.Black
                                ),
                                shape = RectangleShape
                            ) {
                                Text("GO BACK TO WHERE YOU LEFT OFF (Q #${lastIndex + 1}) ➔", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            items(competences) { comp ->
                val totalQuestions = if (questionBankLoaded) MaritimeQuestionBank.getQuestionsByCompetence(comp.competenceId, activeTrack, isProUnlocked = true).size else null
                val savedIndex = viewModel.getQuickScanLastIndex(activeTrack, comp.competenceId)

                MianRuleCard(
                    ruleColor = if (savedIndex > 0) Color(0xFFFF9800) else colors.border,
                    containerColor = colors.surface,
                    onClick = {
                        // ZERO CONFIGURATION: Immediate direct launch
                        viewModel.navigateTo(
                            Screen.QuickScanViewer(
                                competenceId = comp.competenceId,
                                track = activeTrack
                            )
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Top header row: Code tag + Title on left, Badge on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RectangleShape,
                                    color = colors.surface2,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Text(
                                        text = comp.code,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.accent,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = "${comp.code}: ${comp.title}".uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.foreground,
                                    letterSpacing = (-0.1).sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (savedIndex > 0) {
                                    Surface(
                                        shape = RectangleShape,
                                        color = Color(0xFFFF9800).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, Color(0xFFFF9800))
                                    ) {
                                        Text(
                                            text = "Q #${savedIndex + 1}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFF9800),
                                            fontSize = 9.sp,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RectangleShape,
                                    color = colors.surface2,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Text(
                                        text = "READ-ONLY",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.success,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Full-width subtitle explicitly showing total question count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (totalQuestions == null) "… Review Questions • ${comp.functionId}" else "$totalQuestions Review Questions • ${comp.functionId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )

                            androidx.compose.material3.Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open",
                                tint = colors.muted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
