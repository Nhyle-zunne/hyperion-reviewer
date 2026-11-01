package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.model.CompetenceInfo
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
 * Editorial MIAN "Spaghetti" Practice Competence Selection Screen:
 * 3dp top rules, sharp 2dp square containers, Heavy grotesk headings.
 */
@Composable
fun PracticeCompetenceSelectScreen(
    viewModel: ReviewerViewModel,
    isCrpMode: Boolean = false,
    isOperationalReview: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val isDark = colors.isDark
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val questionBankLoaded by MaritimeQuestionBank.isLoadedFlow.collectAsState()

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

    val isOicNwLicensed by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssLicensed by viewModel.isGmdssProUnlocked.collectAsState()
    val isHyperionLicensed by viewModel.isHyperionProUnlocked.collectAsState()
    val isTrackLicensedVm by viewModel.isTrackLicensed.collectAsState()
    val isTrackLicensed = isTrackLicensedVm || isHyperionLicensed

    val competences = MaritimeCompetenceRegistry.getCompetencesForTrack(selectedTrack)

    BackHandler {
        viewModel.navigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val isGmdssOpReview = (selectedTrack == Track.GMDSS_GOC) && isOperationalReview
        val totalTrackQuestions = if (!questionBankLoaded) 0 else if (isCrpMode) {
            MaritimeCompetenceRegistry.getTotalStandardReviewQuestionCount(selectedTrack, isProUnlocked = false)
        } else {
            MaritimeCompetenceRegistry.getTotalTrackQuestionCount(selectedTrack, isCrpMode = false, isProUnlocked = isTrackLicensed, isOperationalReview = isOperationalReview)
        }
        val formattedTotalQuestions = if (!questionBankLoaded) "…" else if (totalTrackQuestions >= 1000) "%,d".format(totalTrackQuestions) else totalTrackQuestions.toString()

        val screenTitle = if (isGmdssOpReview) {
            "GMDSS OPERATIONAL REVIEW"
        } else if (isCrpMode) {
            when (selectedTrack) {
                Track.DECK_OIC_NW -> "MARINA CRP PRACTICE"
                Track.ENGINE_OIC_EW -> "ENGINE OIC-EW STANDARD REVIEW"
                Track.DECK_MGT -> "DECK MANAGEMENT STANDARD REVIEW"
                Track.ENGINE_MGT -> "ENGINE MANAGEMENT STANDARD REVIEW"
                else -> "STANDARD REVIEW"
            }
        } else {
            "PRACTICE ALL QUESTIONS"
        }

        MaritimeTopBar(
            title = screenTitle,
            subtitle = if (isGmdssOpReview) {
                "GMDSS General Operator Certificate • $formattedTotalQuestions questions available"
            } else if (isCrpMode) {
                "STEP 1: SELECT COMPETENCE • ${selectedTrack.title} ($formattedTotalQuestions questions)"
            } else {
                "Practice across all ${competences.size} competencies ($formattedTotalQuestions questions available)"
            },
            showBackButton = false,
            onMenuClick = { viewModel.openDrawer() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

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
                        onClick = {
                            if (!isCrpMode) {
                                viewModel.navigateTo(Screen.PracticeConfig(competenceId = null, isCrpMode = isCrpMode, isOperationalReview = isOperationalReview))
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(containerSurfaceBrush)
                                .padding(18.dp)
                        ) {
                            Text(
                                text = if (isGmdssOpReview) "GMDSS OPERATIONAL REVIEW" else if (isCrpMode) screenTitle else "PRACTICE ALL QUESTIONS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isGmdssOpReview) {
                                    "Practice across all competencies ($formattedTotalQuestions questions available)."
                                } else if (isCrpMode && selectedTrack == Track.DECK_OIC_NW) {
                                    "Select an official STCW competence below to begin sequential or customized practice drilling ($formattedTotalQuestions total questions)."
                                } else if (isCrpMode) {
                                    "Select an STCW competence below to begin standard practice drilling ($formattedTotalQuestions total questions)."
                                } else {
                                    "Practice across all ${competences.size} competencies ($formattedTotalQuestions questions available)."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF15803D)
                            )
                        }
                    }
                }
            }

            item {
                MianSectionHeader(title = "COMPETENCE DIRECTORY (${competences.size})")
            }

            items(competences) { comp ->
                val isProForCount = if (isCrpMode) false else isTrackLicensed
                val formattedCount = if (questionBankLoaded) MaritimeCompetenceRegistry.getFormattedQuestionCount(comp.id, selectedTrack, isCrpMode, isProUnlocked = isProForCount, isOperationalReview = isOperationalReview) else "…"

                MianRuleCard(
                    ruleColor = colors.border,
                    containerColor = colors.surface,
                    onClick = {
                        viewModel.navigateTo(Screen.PracticeConfig(competenceId = comp.id, isCrpMode = isCrpMode, isOperationalReview = isOperationalReview))
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
                            val compSubtitle = if (isGmdssOpReview) {
                                if (comp.id == "C1") {
                                    "GMDSS Operational Review • ${com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.OPERATIONAL_REVIEW_LIMIT_C1} Questions"
                                } else {
                                    "GMDSS Operational Review • ${com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.OPERATIONAL_REVIEW_LIMIT_C2} Questions"
                                }
                            } else if (isCrpMode) {
                                "${comp.functionId} · $formattedCount official review questions"
                            } else {
                                "${comp.functionId} · $formattedCount questions available"
                            }
                            Text(
                                text = compSubtitle,
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
