package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompetenceInfo
import com.example.data.repository.MaritimeCompetenceRegistry
import com.example.data.repository.MaritimeQuestionBank
import com.example.ui.components.MaritimeTopBar
import com.example.ui.theme.MaritimeGold
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.NauticalNavyDark
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

/**
 * Step 1: Competence Selection Screen (PracticeCompetenceSelectScreen).
 * When the user chooses Practice Drill, first display a dedicated list of competencies
 * (e.g., C1, C2, C3... or "All Competencies").
 * Tapping a competence selects it and navigates forward into the configuration screen.
 */
@Composable
fun PracticeCompetenceSelectScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()

    // Back navigation to Track Dashboard
    BackHandler {
        viewModel.navigateTo(Screen.Home)
    }

    val competences = MaritimeCompetenceRegistry.getCompetencesForTrack(selectedTrack)
    val groupedByFunction = competences.groupBy { it.functionId }
    val totalTrackItems = MaritimeQuestionBank.getQuestionsByTrack(selectedTrack).size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        MaritimeTopBar(
            title = "Select Competence",
            subtitle = "${selectedTrack.shortName} • Step 1 of 2",
            showBackButton = true,
            onBackClick = { viewModel.navigateTo(Screen.Home) },
            onLegalClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                    border = BorderStroke(1.dp, SeaCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SeaCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = SeaCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Practice Drill Setup Workflow",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Text(
                                text = "Choose a target competence or select the entire pool to configure question count & ordering.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // "All Competencies" Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.navigateTo(Screen.PracticeConfig(null)) }
                        .testTag("select_all_competencies_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.5.dp, MaritimeGoldBright),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                                .background(MaritimeGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaritimeGoldBright,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaritimeGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "FULL QUESTION POOL",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaritimeGoldBright,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "All Competencies ($totalTrackItems Questions)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Text(
                                text = "Comprehensive practice covering all ${competences.size} STCW competencies for this track.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite.copy(alpha = 0.75f)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Configure",
                            tint = MaritimeGoldBright,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "OR SELECT SPECIFIC STCW COMPETENCE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SeaCyan,
                    letterSpacing = 1.sp
                )
            }

            // Competency cards grouped by Function
            for ((funcId, compList) in groupedByFunction) {
                item {
                    val funcIcon = when (funcId) {
                        "F1" -> Icons.Default.Explore
                        "F2" -> Icons.Default.Inventory2
                        "F3" -> Icons.Default.Security
                        else -> Icons.Default.CellTower
                    }
                    val funcTitle = MaritimeCompetenceRegistry.getFunctionTitle(funcId)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    ) {
                        Icon(
                            imageVector = funcIcon,
                            contentDescription = null,
                            tint = SeaCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = funcTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SeaCyan
                        )
                    }
                }

                items(compList) { comp ->
                    val itemCount = MaritimeQuestionBank.getQuestionsByCompetence(comp.competenceId, selectedTrack).size

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.navigateTo(Screen.PracticeConfig(comp.competenceId)) }
                            .testTag("competence_card_${comp.competenceId}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SeaCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = comp.code,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SeaCyan
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = comp.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$itemCount Questions Available • ${comp.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PureWhite.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Select",
                                tint = SeaCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
