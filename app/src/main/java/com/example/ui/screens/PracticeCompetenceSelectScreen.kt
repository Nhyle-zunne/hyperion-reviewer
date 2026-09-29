package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompetenceInfo
import com.example.data.model.Track
import com.example.data.repository.MaritimeCompetenceRegistry
import com.example.data.repository.MaritimeQuestionBank
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.MianRuleCard
import com.example.ui.components.MianSectionHeader
import com.example.ui.theme.LocalSpaghettiColors
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" Practice Competence Selection Screen:
 * 3dp top rules, sharp 2dp square containers, Heavy grotesk headings.
 */
@Composable
fun PracticeCompetenceSelectScreen(
    viewModel: ReviewerViewModel,
    isCrpMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val selectedTrack by viewModel.selectedTrack.collectAsState()

    val competences = if (selectedTrack == Track.DECK_OIC_NW) {
        MaritimeCompetenceRegistry.ALL_DECK_COMPETENCES
    } else {
        MaritimeCompetenceRegistry.ALL_GMDSS_COMPETENCES
    }

    BackHandler {
        viewModel.navigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = if (isCrpMode) "MARINA CRP PRACTICE" else "PRACTICE ALL QUESTIONS",
            subtitle = if (isCrpMode) {
                "STEP 1: SELECT COMPETENCE • ${selectedTrack.name.replace('_', ' ')}"
            } else if (selectedTrack == Track.DECK_OIC_NW) {
                "Practice across all 12 competencies (6,455 questions available)"
            } else {
                "Practice across all competencies (2,260 questions available)"
            },
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
                            text = if (isCrpMode) "MARINA CRP REFERENCE PRACTICE" else "PRACTICE ALL QUESTIONS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            letterSpacing = (-0.2).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isCrpMode) {
                                "Select an official STCW competence below to begin sequential or customized practice drilling."
                            } else if (selectedTrack == Track.DECK_OIC_NW) {
                                "Practice across all 12 competencies (6,455 questions available)."
                            } else {
                                "Practice across all competencies (2,260 questions available)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted
                        )
                    }
                }
            }

            item {
                MianSectionHeader(title = "COMPETENCE DIRECTORY (${competences.size})")
            }

            items(competences) { comp ->
                val formattedCount = MaritimeCompetenceRegistry.getFormattedOfficialCrpQuestionCount(comp.id, selectedTrack)

                MianRuleCard(
                    ruleColor = colors.border,
                    containerColor = colors.surface,
                    onClick = {
                        viewModel.navigateTo(Screen.PracticeConfig(competenceId = comp.id, isCrpMode = isCrpMode))
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = colors.surface2
                        ) {
                            Text(
                                text = comp.id,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.accent,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${comp.id}: ${comp.title}".uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground,
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${comp.functionId} · $formattedCount official review questions",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                                fontSize = 11.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Select",
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
