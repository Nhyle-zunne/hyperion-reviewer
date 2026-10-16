package com.nhyle.hyperionmaritime.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.nhyle.hyperionmaritime.data.local.DeletedSession
import com.nhyle.hyperionmaritime.data.local.expirationLabel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.TextButton
import com.nhyle.hyperionmaritime.ui.theme.PureWhite
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.nhyle.hyperionmaritime.data.local.DrillSessionEntity
import com.nhyle.hyperionmaritime.data.local.DrillSessionFormatter
import com.nhyle.hyperionmaritime.ui.components.DeleteSessionLogConfirmationDialog
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.data.model.Track
// import com.nhyle.hyperionmaritime.data.util.NewTrackPreviewAccess
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository
import com.nhyle.hyperionmaritime.ui.components.IndustrialToggleSwitch
import com.nhyle.hyperionmaritime.ui.components.LuminousDeskSupportButton
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.openMessengerSupport
import com.nhyle.hyperionmaritime.ui.theme.FailRed
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.ui.theme.MaritimeGold
import com.nhyle.hyperionmaritime.ui.theme.MaritimeGoldBright
import com.nhyle.hyperionmaritime.ui.theme.PassGreen
import com.nhyle.hyperionmaritime.ui.theme.PureWhite
import com.nhyle.hyperionmaritime.ui.theme.SeaCyan
import com.nhyle.hyperionmaritime.viewmodel.DrillDeliveryMode
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

/**
 * Step 2: Practice Drill Configuration Screen.
 * - Displays selected competence scope in header
 * - Mode selection: "Standard Drill" vs "Intensive Mode"
 * - Randomization card with two switches:
 *   1. "Randomize Question Order" (Sequential vs Shuffled)
 *   2. "Shuffle Choices (A/B/C/D)"
 * - Synchronized Dual Slider + Numeric input clamped to available questions
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeConfigScreen(
    viewModel: ReviewerViewModel,
    competenceId: String? = null,
    isCrpMode: Boolean = false,
    isOperationalReview: Boolean = false,
    modifier: Modifier = Modifier
) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val isActivated by viewModel.isActivated.collectAsState()
    val isDarkModePref by viewModel.isDarkMode.collectAsState()
    val context = LocalContext.current
    val colors = LocalSpaghettiColors.current
    val isDarkState = isDarkModePref

    var sessionPendingDeleteId by remember { mutableStateOf<String?>(null) }
    val deletedSessionsState by viewModel.deletedSessions.collectAsState(initial = emptyList())
    var showRecentlyDeletedDialog by remember { mutableStateOf(false) }
    var sessionToPermanentlyDelete by remember { mutableStateOf<DeletedSession?>(null) }

    // Back navigation pops back to previous screen (Competence Selection), or cancels active delete dialog
    BackHandler {
        if (sessionPendingDeleteId != null) {
            sessionPendingDeleteId = null
        } else {
            viewModel.navigateTo(Screen.PracticeCompetenceSelect(isCrpMode = isCrpMode, isOperationalReview = isOperationalReview))
        }
    }

    val compInfo = competenceId?.let { MaritimeCompetenceRegistry.getCompetenceById(it, selectedTrack) }

    val recentSessions: List<DrillSessionEntity> by viewModel.getRecentSessions(selectedTrack.name, competenceId)
        .collectAsState(initial = emptyList())

    var selectedPart by remember(competenceId) { mutableStateOf<Int?>(null) }
    var showProLockedDialog by remember { mutableStateOf<String?>(null) }

    val isGmdssOpReview = (selectedTrack == Track.GMDSS_GOC) && isOperationalReview
    val isOicNwLicensed by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssLicensed by viewModel.isGmdssProUnlocked.collectAsState()
    val isHyperionLicensed by viewModel.isHyperionProUnlocked.collectAsState()
    val isTrackLicensedVm by viewModel.isTrackLicensed.collectAsState()
    val isTrackLicensed = isTrackLicensedVm || isHyperionLicensed
    val isVerified = isTrackLicensed
    val isFreeTier = !isVerified
    // showFixedPool should be true for CRP Mode OR GMDSS Operational Review (unconditionally per latest request)
    val showFixedPool = isCrpMode || (selectedTrack == Track.GMDSS_GOC && isOperationalReview)
    var showVerificationDialog by remember { mutableStateOf(false) }
    val isAdvancedLocked = !isVerified

    // Dynamic max available questions
    val maxAvailable = remember(competenceId, selectedPart, selectedTrack, isCrpMode, context, isTrackLicensed, isOperationalReview) {
        val part = selectedPart
        if (selectedTrack == Track.GMDSS_GOC) {
            if (isOperationalReview) {
                if (competenceId != null) {
                    val total = MaritimeCompetenceRegistry.getDynamicQuestionCount(competenceId, selectedTrack, isOperationalReview = true)
                    when (competenceId) {
                        "C1" -> minOf(total, 900)
                        "C2" -> minOf(total, 130)
                        else -> total
                    }
                } else {
                    MaritimeCompetenceRegistry.getTotalTrackQuestionCount(selectedTrack, isOperationalReview = true)
                }
            } else {
                if (competenceId != null) {
                    if (part != null) {
                        ProQuestionRepository.getExpectedQuestionCount(competenceId, part)
                            .takeIf { it > 0 } ?: ProQuestionRepository.loadRawPartQuestions(context, competenceId, part).size
                    } else {
                        when (competenceId) {
                            "C1" -> 1911
                            "C2" -> 349
                            else -> 2260
                        }
                    }
                } else {
                    2260
                }
            }
        } else if (isCrpMode && selectedTrack == Track.DECK_OIC_NW) {
            if (competenceId == null) {
                2065
            } else {
                MaritimeCompetenceRegistry.CRP_DECK_DISTRIBUTION[competenceId] ?: 100
            }
        } else {
            if (competenceId == null) {
                MaritimeQuestionBank.getQuestionsByTrack(selectedTrack, isProUnlocked = true).size
            } else {
                MaritimeQuestionBank.getQuestionsByCompetence(competenceId, selectedTrack, isProUnlocked = true).size
            }
        }
    }.coerceAtLeast(1)

    // Step 2: Delivery Format (Standard Drill vs Intensive Mode)
    var selectedDeliveryFormat by remember { mutableStateOf(DrillDeliveryMode.STANDARD) }

    val drillConfig by viewModel.drillConfiguration.collectAsState()
    var prioritizeUnseen by remember { mutableStateOf(drillConfig.prioritizeUnseen) }

    // Step 3: Randomization Controls (Two switches)
    // In CRP Mode: Randomize Questions is disabled/locked, Shuffled Choices is enabled (ON) by default
    var randomizeQuestionOrder by remember { mutableStateOf(false) } // Default Sequential
    var shuffleChoices by remember { mutableStateOf(if (isCrpMode) true else isActivated) } // Default Shuffle choices ON for CRP / Pro

    // Step 4: Dual Range state (Start and End Item Selector) - Fixed in CRP Mode & GMDSS
    // Note: showFixedPool is defined above for consistent scoping

    var startItem by remember(maxAvailable) { mutableIntStateOf(1) }
    var endItem by remember(maxAvailable, isActivated, showFixedPool) {
        mutableIntStateOf(if (showFixedPool) maxAvailable else if (!isActivated) minOf(5, maxAvailable) else maxAvailable)
    }
    var startTextFieldValue by remember(startItem) {
        mutableStateOf(startItem.toString())
    }
    var endTextFieldValue by remember(endItem) {
        mutableStateOf(endItem.toString())
    }

    androidx.compose.runtime.LaunchedEffect(maxAvailable, showFixedPool) {
        startItem = 1
        endItem = if (showFixedPool) maxAvailable else if (!isActivated) minOf(5, maxAvailable) else maxAvailable
        startTextFieldValue = "1"
        endTextFieldValue = endItem.toString()
    }

    fun updateRange(newStart: Int, newEnd: Int) {
        if (showFixedPool || !isActivated) return
        val clampedStart = newStart.coerceIn(1, maxAvailable)
        val clampedEnd = newEnd.coerceIn(clampedStart, maxAvailable)
        startItem = clampedStart
        endItem = clampedEnd
        startTextFieldValue = clampedStart.toString()
        endTextFieldValue = clampedEnd.toString()
    }

    val greetingTitle = if (compInfo != null) {
        if (isGmdssOpReview) "GMDSS Drill: ${compInfo.code} - ${compInfo.title}"
        else if (isCrpMode) "MARINA CRP Drill: ${compInfo.code} - ${compInfo.title}"
        else "Configure Drill: ${compInfo.code} - ${compInfo.title}"
    } else {
        if (isGmdssOpReview) "GMDSS Drill: All Competencies"
        else if (isCrpMode) "MARINA CRP Drill: All Competencies"
        else "Configure Drill: All Competencies"
    }

    val greetingSubtitle = if (compInfo != null) {
        if (isGmdssOpReview) {
            "GMDSS Radio Operations • $maxAvailable Items"
        } else if (isCrpMode) {
            "MARINA CRP REFERENCE DRILL • $maxAvailable Official Review Questions"
        } else {
            "${compInfo.functionName} • $maxAvailable Items"
        }
    } else {
        if (isGmdssOpReview) {
            "GMDSS General Operator Certificate • Full Pool ($maxAvailable Items Available)"
        } else if (isCrpMode) {
            "MARINA CRP REFERENCE DRILL • $maxAvailable Official Review Questions"
        } else {
            "${selectedTrack.title} • Full Pool ($maxAvailable Items Available)"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        MaritimeTopBar(
            title = if (isGmdssOpReview) "GMDSS OPERATIONAL REVIEW" else if (isCrpMode) "MARINA CRP PRACTICE" else "PRACTICE ALL QUESTIONS",
            subtitle = if (compInfo != null) compInfo.code else "All Competencies",
            showBackButton = false,
            onMenuClick = { viewModel.openDrawer() },
            onLegalClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isCrpMode && !isGmdssOpReview) {
                // Prominent Legal Disclaimer Banner
                item {
                    Surface(
                        shape = RectangleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Note: This free practice mode is independently developed based on MARINA CRP references and is not an official MARINA-endorsed examination tool.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Examinee Greeting Banner with Selected Competence Title
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    shape = RectangleShape
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = RectangleShape,
                            color = if (showFixedPool) PassGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (showFixedPool) PassGreen else MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = if (isGmdssOpReview) "STEP 2 OF 2 • GMDSS OPERATIONAL REVIEW" else if (isCrpMode) "STEP 2 OF 2 · MARINA CRP PRACTICE" else "STEP 2 OF 2 • PRACTICE ALL QUESTIONS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (showFixedPool) PassGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = greetingTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = greetingSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (showFixedPool) PassGreen else MaterialTheme.colorScheme.primary
                        )

                        compInfo?.description?.let { desc ->
                            Spacer(modifier = Modifier.height(4.dp))
                            val displayDesc = if (!isOperationalReview && selectedTrack == Track.GMDSS_GOC) {
                                if (compInfo.competenceId == "C1") {
                                    "GMDSS Subsystems & Operational Procedures • 1,911 Questions across 10 parts (DSC, NAVTEX, EPIRB, SART, Inmarsat-C, VHF/MF/HF)"
                                } else {
                                    "Emergency Radio Communications & SAR Procedures • 349 Questions across 2 parts (Distress alerting, SAR on-scene communications)"
                                }
                            } else {
                                desc
                            }
                            Text(
                                text = displayDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Dedicated Competence Session Log & Telemetry Box (Permanent, simplistic, boxed style)
            item {
                val isDark = isDarkState
                val containerBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
                val containerBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                val resumeColor = if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)
                val accentActionColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_sessions_box"),
                    shape = RectangleShape,
                    colors = CardDefaults.cardColors(containerColor = containerBg),
                    border = BorderStroke(1.dp, containerBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                            // Row 1: Green square marker and Title "SESSION LOG & TELEMETRY"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (recentSessions.any { !it.isCompleted }) resumeColor else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)), RectangleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SESSION LOG & TELEMETRY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            }

                            // Row 2: Three actions spread evenly across width (SpaceBetween) with compact vertical height
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (recentSessions.any { !it.isCompleted }) {
                                    Box(
                                        modifier = Modifier
                                            .wrapContentWidth()
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "RESUME",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = resumeColor
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { showRecentlyDeletedDialog = true }
                                        .testTag("recently_deleted_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "RECENTLY DELETED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentActionColor
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { viewModel.navigateTo(Screen.History) }
                                        .testTag("view_all_session_logs_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "ARCHIVE »",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentActionColor
                                    )
                                }
                            }

                        if (recentSessions.isNotEmpty()) {
                            CompactSessionLogTable(
                                sessions = recentSessions,
                                onResumeSession = { session ->
                                    viewModel.resumeDrillSession(session)
                                },
                                onDeleteSession = { session ->
                                    if (sessionPendingDeleteId == null) {
                                        sessionPendingDeleteId = session.id
                                    }
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RectangleShape)
                                    .background(if (isDark) Color(0xFF0B132B) else Color(0xFFF8FAFC))
                                    .padding(vertical = 10.dp, horizontal = 10.dp)
                                    .testTag("session_log_empty_box")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "NO SESSIONS RECORDED FOR THIS COMPETENCE",
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    )
                                    Text(
                                        text = "Active and completed drills for this competence are preserved here automatically.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.navigateTo(Screen.History) },
                                            shape = RectangleShape,
                                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("empty_open_session_log_button")
                                        ) {
                                            Text(
                                                text = "OPEN SESSION LOG ARCHIVE »",
                                                style = TextStyle(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = accentActionColor
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }



            if (selectedTrack == Track.GMDSS_GOC && !isOperationalReview) {
                item {
                    val availableParts = if (competenceId != null) {
                        ProQuestionRepository.getPartsForCompetence(competenceId)
                    } else {
                        emptyList()
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("part_selector_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                        shape = RectangleShape
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SELECT SPECIFIC PART OR ALL PARTS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (competenceId != null) {
                                    "Filter questions by specific part module (${availableParts.size} parts available) or practice all parts together."
                                } else {
                                    "Drilling all GMDSS competencies (2,260 questions available). Select a specific competence to filter by individual part."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val isAllSelected = selectedPart == null
                            val allPartsCount = when (competenceId) {
                                "C1" -> 1911
                                "C2" -> 349
                                else -> 2260
                            }
                            OutlinedButton(
                                onClick = {
                                    selectedPart = null
                                },
                                shape = RectangleShape,
                                border = BorderStroke(
                                    width = if (isAllSelected) 2.dp else 1.dp,
                                    color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isAllSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("part_selector_all")
                            ) {
                                Text(
                                    text = "All Parts ($allPartsCount)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            availableParts.chunked(2).forEach { rowParts ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowParts.forEach { part ->
                                        val isSelected = selectedPart == part
                                        val partCount = ProQuestionRepository.getExpectedQuestionCount(competenceId ?: "C1", part)
                                        val label = if (partCount > 0) "Part $part ($partCount)" else "Part $part"
                                        OutlinedButton(
                                            onClick = {
                                                selectedPart = part
                                            },
                                            shape = RectangleShape,
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            ),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("part_selector_part_$part")
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    if (rowParts.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // Step 2: Drill Delivery Format Selection (Standard Drill vs Intensive Mode)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    shape = RectangleShape
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "1. DRILL DELIVERY FORMAT",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Option 1: Standard Drill Card
                        val isStandardSelected = selectedDeliveryFormat == DrillDeliveryMode.STANDARD
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDeliveryFormat = DrillDeliveryMode.STANDARD }
                                .border(
                                    width = if (isStandardSelected) 1.5.dp else 1.dp,
                                    color = if (isStandardSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RectangleShape
                                ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RectangleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isStandardSelected,
                                    onClick = { selectedDeliveryFormat = DrillDeliveryMode.STANDARD },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "Standard Drill",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RectangleShape,
                                            color = if (showFixedPool) PassGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, if (showFixedPool) PassGreen else MaterialTheme.colorScheme.outline)
                                        ) {
                                            Text(
                                                text = if (showFixedPool) "SEQUENTIAL ACTIVE" else "FIXED ITEMS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (showFixedPool) PassGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Progress through each question once with immediate answer verification. Concludes with a session summary.",
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Option 2: Intensive Mode Card
                        val isIntensiveSelected = selectedDeliveryFormat == DrillDeliveryMode.INTENSIVE
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isVerified) 1f else 0.38f)
                                .clickable {
                                    if (isVerified) {
                                        selectedDeliveryFormat = DrillDeliveryMode.INTENSIVE
                                    } else {
                                        Toast.makeText(context, "Requires Verification", Toast.LENGTH_SHORT).show()
                                        showVerificationDialog = true
                                    }
                                }
                                .border(
                                    width = if (isIntensiveSelected) 1.5.dp else 1.dp,
                                    color = if (isIntensiveSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RectangleShape
                                ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RectangleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isIntensiveSelected,
                                    onClick = {
                                        if (isVerified) {
                                            selectedDeliveryFormat = DrillDeliveryMode.INTENSIVE
                                        } else {
                                            Toast.makeText(context, "Requires Verification", Toast.LENGTH_SHORT).show()
                                            showVerificationDialog = true
                                        }
                                    },
                                    enabled = isVerified,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.outline,
                                        disabledSelectedColor = MaterialTheme.colorScheme.outline,
                                        disabledUnselectedColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "Intensive Mode",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(
                                                shape = RectangleShape,
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                            ) {
                                                Text(
                                                    text = "RECYCLING QUEUE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            if (!isVerified) {
                                                Surface(
                                                    shape = RectangleShape,
                                                    color = colors.surface2,
                                                    border = BorderStroke(1.dp, colors.border)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Lock,
                                                            contentDescription = null,
                                                            tint = colors.muted,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "LOCKED",
                                                            color = colors.muted,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            letterSpacing = 0.8.sp,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (!isVerified) "Requires Verification" else "Missed questions recycle to the end of the queue. The drill only finishes when all questions are mastered.",
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Step 3: Randomization & Sequencing Card (Two switches together)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isVerified) 1f else 0.38f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    shape = RectangleShape
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2. RANDOMIZATION & SEQUENCING",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (!isVerified) {
                                Surface(
                                    shape = RectangleShape,
                                    color = colors.surface2,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = colors.muted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "LOCKED",
                                            color = colors.muted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Switch 1: Randomize Question Order (Sequential vs Shuffled)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                                    .clickable {
                                        if (isVerified) {
                                            randomizeQuestionOrder = !randomizeQuestionOrder
                                        } else {
                                            Toast.makeText(context, "Requires Verification", Toast.LENGTH_SHORT).show()
                                            showVerificationDialog = true
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Randomize Question Order",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        shape = RectangleShape,
                                        border = BorderStroke(1.dp, if (!isVerified) (if (isDarkState) Color(0xFF64748B) else Color(0xFF94A3B8)) else if (randomizeQuestionOrder) (if (isDarkState) Color(0xFF22C55E) else Color(0xFF16A34A)) else (if (isDarkState) Color(0xFF64748B) else Color(0xFFCBD5E1))),
                                        color = if (randomizeQuestionOrder) (if (isDarkState) Color(0xFF14532D).copy(alpha = 0.2f) else Color(0xFFDCFCE7)) else Color.Transparent
                                    ) {
                                        Text(
                                            text = if (!isVerified) "LOCKED" else if (randomizeQuestionOrder) "SHUFFLED" else "SEQUENTIAL",
                                            color = if (!isVerified) (if (isDarkState) Color(0xFF94A3B8) else Color(0xFF64748B)) else if (randomizeQuestionOrder) (if (isDarkState) Color(0xFF4ADE80) else Color(0xFF15803D)) else (if (isDarkState) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Clip
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (!isVerified) "Requires Verification" else if (randomizeQuestionOrder) {
                                        "Questions are dynamically shuffled at the start of the drill."
                                    } else {
                                        "Questions appear in strict original database order (#1, #2, #3...)."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            IndustrialToggleSwitch(
                                checked = if (isVerified) randomizeQuestionOrder else false,
                                onCheckedChange = {
                                    if (isVerified) {
                                        randomizeQuestionOrder = it
                                    } else {
                                        Toast.makeText(context, "Requires Verification", Toast.LENGTH_SHORT).show()
                                        showVerificationDialog = true
                                    }
                                },
                                modifier = Modifier.testTag("switch_randomize_question_order")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Switch 2: Shuffle Choices (A/B/C/D) - Enabled (ON) by default in CRP mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                                    .clickable { shuffleChoices = !shuffleChoices }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Shuffle Choices (A/B/C/D)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        shape = RectangleShape,
                                        border = BorderStroke(1.dp, if (shuffleChoices) (if (isDarkState) Color(0xFF22C55E) else Color(0xFF16A34A)) else (if (isDarkState) Color(0xFF64748B) else Color(0xFFCBD5E1))),
                                        color = if (shuffleChoices) (if (isDarkState) Color(0xFF14532D).copy(alpha = 0.2f) else Color(0xFFDCFCE7)) else Color.Transparent
                                    ) {
                                        Text(
                                            text = if (shuffleChoices) "SHUFFLED" else "DEFAULT",
                                            color = if (shuffleChoices) (if (isDarkState) Color(0xFF4ADE80) else Color(0xFF15803D)) else (if (isDarkState) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Clip
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Permutes option letters to prevent letter memorization while preserving correct answer keys.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            IndustrialToggleSwitch(
                                checked = shuffleChoices,
                                onCheckedChange = { shuffleChoices = it },
                                modifier = Modifier.testTag("switch_shuffle_choices")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Switch 3: Prioritize Unseen Items
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                                    .clickable {
                                        val next = !prioritizeUnseen
                                        prioritizeUnseen = next
                                        viewModel.setPrioritizeUnseen(next)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Prioritize Unseen Items",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        shape = RectangleShape,
                                        border = BorderStroke(1.dp, if (prioritizeUnseen) (if (isDarkState) Color(0xFF22C55E) else Color(0xFF16A34A)) else (if (isDarkState) Color(0xFF64748B) else Color(0xFFCBD5E1))),
                                        color = if (prioritizeUnseen) (if (isDarkState) Color(0xFF14532D).copy(alpha = 0.2f) else Color(0xFFDCFCE7)) else Color.Transparent
                                    ) {
                                        Text(
                                            text = if (prioritizeUnseen) "PRIORITIZED" else "DEFAULT",
                                            color = if (prioritizeUnseen) (if (isDarkState) Color(0xFF4ADE80) else Color(0xFF15803D)) else (if (isDarkState) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Clip
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Serve unseen questions first to improve question bank coverage.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            IndustrialToggleSwitch(
                                checked = prioritizeUnseen,
                                onCheckedChange = {
                                    prioritizeUnseen = it
                                    viewModel.setPrioritizeUnseen(it)
                                },
                                modifier = Modifier.testTag("switch_prioritize_unseen")
                            )
                        }
                    }
                }
            }

            // Step 4: Dual Question Range Selector (RangeSlider + Start/End Inputs) - Fixed in CRP Mode & GMDSS
            val showFixedPoolLocal = isCrpMode || (selectedTrack == Track.GMDSS_GOC && isOperationalReview)
            val selectedItemCount = if (showFixedPoolLocal) maxAvailable else (endItem - startItem + 1).coerceAtLeast(1)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    shape = RectangleShape
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (showFixedPoolLocal) PassGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showFixedPoolLocal) "3. QUESTION COUNT ($maxAvailable ITEMS POOL)" else "3. QUESTION RANGE SELECTOR (#$startItem TO #$endItem)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (showFixedPoolLocal) PassGreen else MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isAdvancedLocked || showFixedPoolLocal) {
                                Spacer(modifier = Modifier.width(6.dp))
                                ClearanceRequiredBadge()
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (showFixedPoolLocal) {
                            Surface(
                                shape = RectangleShape,
                                color = PassGreen.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, PassGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PassGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Fixed Question Pool: $maxAvailable Items",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Free practice automatically delivers all sequential questions in this competency block.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // Quick Range Preset Chips (1-25, 1-50, 1-100, All Available)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val presets = listOf(25, 50, 100).filter { it <= maxAvailable }
                                for (preset in presets) {
                                    val isSelected = startItem == 1 && endItem == preset
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { updateRange(1, preset) },
                                        shape = RectangleShape,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        label = { Text("1–$preset", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                                // All Available Chip
                                val isAllPreset = startItem == 1 && endItem == maxAvailable
                                FilterChip(
                                    selected = isAllPreset,
                                    onClick = { updateRange(1, maxAvailable) },
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    label = { Text("All (1–$maxAvailable)", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dual-Thumb RangeSlider (Start and End Item Selector)
                            val sliderRange = startItem.toFloat()..endItem.toFloat().coerceAtLeast(startItem.toFloat())
                            val totalValueRange = 1f..maxAvailable.toFloat().coerceAtLeast(1f)

                            RangeSlider(
                                value = sliderRange,
                                onValueChange = { range ->
                                    if (isActivated) {
                                        val s = range.start.toInt().coerceIn(1, maxAvailable)
                                        val e = range.endInclusive.toInt().coerceIn(s, maxAvailable)
                                        startItem = s
                                        endItem = e
                                        startTextFieldValue = s.toString()
                                        endTextFieldValue = e.toString()
                                    }
                                },
                                valueRange = totalValueRange,
                                enabled = isActivated,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("practice_question_range_slider")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Item #1",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Selected: $selectedItemCount items",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Item #$maxAvailable",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Dual Text Inputs: Start Item & End Item
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = startTextFieldValue,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        startTextFieldValue = digits
                                        digits.toIntOrNull()?.let { num ->
                                            val clamped = num.coerceIn(1, endItem)
                                            startItem = clamped
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("practice_range_start_input"),
                                    label = { Text("Start Item (#)", fontSize = 11.sp) },
                                    singleLine = true,
                                    shape = RectangleShape,
                                    enabled = isActivated,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )

                                OutlinedTextField(
                                    value = endTextFieldValue,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        endTextFieldValue = digits
                                        digits.toIntOrNull()?.let { num ->
                                            val clamped = num.coerceIn(startItem, maxAvailable)
                                            endItem = clamped
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("practice_range_end_input"),
                                    label = { Text("End Item (#)", fontSize = 11.sp) },
                                    singleLine = true,
                                    shape = RectangleShape,
                                    enabled = isActivated,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            // Scope Summary Display
                            Surface(
                                shape = RectangleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Session Scope:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Item #$startItem to #$endItem ($selectedItemCount Questions)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Institutional Clearance Callout Banner if unverified
            if (!isVerified) {
                item {
                    MianRuleCard(
                        ruleColor = colors.border,
                        containerColor = colors.surface,
                        onClick = { showVerificationDialog = true }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "To unlock full access including the config page, please purchase via our store or activate a key.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RectangleShape,
                                color = colors.accent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CHECK PASS STORE ↗",
                                        style = MaterialTheme.typography.labelMedium,
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

            // Launch Action Button
            item {
                val launchLabel = when {
                    selectedDeliveryFormat == DrillDeliveryMode.INTENSIVE -> "Start Reviewing (Intensive Mode)"
                    isCrpMode -> "Start Reviewing (MARINA CRP)"
                    isGmdssOpReview -> "Start Reviewing (GMDSS Drill)"
                    else -> "Start Reviewing ($selectedItemCount Items: #$startItem–#$endItem)"
                }

                Button(
                    onClick = {
                        val launchStart = if (showFixedPool) 1 else startItem
                        val launchEnd = if (showFixedPool) maxAvailable else endItem
                        val launchCount = if (showFixedPool) maxAvailable else (endItem - startItem + 1).coerceAtLeast(1)

                        viewModel.startConfiguredPractice(
                            competenceId = competenceId,
                            selectedPart = selectedPart,
                            deliveryMode = selectedDeliveryFormat,
                            questionCount = launchCount,
                            startItem = launchStart,
                            endItem = launchEnd,
                            shuffleQuestions = randomizeQuestionOrder,
                            shuffleChoices = shuffleChoices,
                            isCrpMode = isCrpMode,
                            prioritizeUnseen = prioritizeUnseen,
                            isOperationalReview = isOperationalReview
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("launch_practice_drill_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PassGreen,
                        contentColor = PureWhite
                    ),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selectedDeliveryFormat == DrillDeliveryMode.INTENSIVE) Icons.Default.Autorenew else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = launchLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Delete Session Log Confirmation Dialog
    sessionPendingDeleteId?.let { targetId ->
        DeleteSessionLogConfirmationDialog(
            sessionId = targetId,
            onConfirmDelete = { idToDelete ->
                sessionPendingDeleteId = null
                viewModel.deleteExamRecord(idToDelete)
            },
            onDismiss = {
                sessionPendingDeleteId = null
            }
        )
    }

    // Recently Deleted Sessions Dialog
    if (showRecentlyDeletedDialog) {
        AlertDialog(
            onDismissRequest = { showRecentlyDeletedDialog = false },
            modifier = Modifier
                .border(BorderStroke(1.5.dp, colors.border), RectangleShape)
                .testTag("recently_deleted_dialog"),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(colors.accent, RectangleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Recently Deleted Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        modifier = Modifier.testTag("recently_deleted_dialog_title")
                    )
                }
            },
            text = {
                if (deletedSessionsState.isEmpty()) {
                    Text(
                        text = "No recently deleted sessions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        modifier = Modifier.testTag("recently_deleted_empty_text")
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(deletedSessionsState, key = { ds -> ds.sessionId }) { ds ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, colors.border, RectangleShape)
                                    .background(colors.surface2)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ds.label,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.foreground
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${ds.date} • Progress: ${ds.progress}",
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.muted
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = ds.expirationLabel,
                                        style = TextStyle(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.warning
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(
                                        onClick = { sessionToPermanentlyDelete = ds },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = colors.error,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "PERMANENTLY DELETE",
                                                style = TextStyle(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = colors.error
                                                )
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.restoreDeletedSession(ds)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.primaryForeground
                                    ),
                                    shape = RectangleShape,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("restore_session_${ds.sessionId}")
                                ) {
                                    Text(
                                        text = "RESTORE",
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRecentlyDeletedDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.primaryForeground
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.testTag("recently_deleted_close_button")
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    // Permanent Delete Confirmation Dialog
    sessionToPermanentlyDelete?.let { targetSession ->
        AlertDialog(
            onDismissRequest = { sessionToPermanentlyDelete = null },
            modifier = Modifier
                .border(BorderStroke(1.5.dp, colors.border), RectangleShape)
                .testTag("permanent_delete_dialog"),
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Permanently Delete Session Log?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete this session log? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeleteSession(targetSession)
                        sessionToPermanentlyDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = PureWhite
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.testTag("permanent_delete_confirm_button")
                ) {
                    Text("PERMANENTLY DELETE", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { sessionToPermanentlyDelete = null },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("CANCEL", color = colors.foreground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Pro Feature Lock Dialog
    if (showProLockedDialog != null) {
        AlertDialog(
            onDismissRequest = { showProLockedDialog = null },
            modifier = Modifier.border(BorderStroke(1.5.dp, colors.border), RectangleShape),
            shape = RectangleShape,
            containerColor = colors.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaritimeGoldBright,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Pro Mode Feature",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.foreground
                )
            },
            text = {
                Text(
                    text = showProLockedDialog!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProLockedDialog = null
                        viewModel.navigateTo(Screen.Store)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.primaryForeground
                    ),
                    shape = RectangleShape
                ) {
                    Text("Account & Device Help", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showProLockedDialog = null },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text("Continue Free Practice", color = colors.foreground)
                }
            }
        )
    }

    // Store Purchase Confirm Dialog
    if (showVerificationDialog) {
        Dialog(
            onDismissRequest = { showVerificationDialog = false },
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
                        text = "To unlock full access including the config page, please purchase via our store or activate a key.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Would you like to check the Hyperion Store to view available track passes?",
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
                                showVerificationDialog = false
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
                            onClick = { showVerificationDialog = false },
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
}

@Composable
private fun ClearanceRequiredBadge() {
    val colors = LocalSpaghettiColors.current
    Surface(
        shape = RectangleShape,
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "LOCKED",
                color = colors.muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun CompactSessionLogTable(
    sessions: List<DrillSessionEntity>,
    onResumeSession: (DrillSessionEntity) -> Unit,
    onDeleteSession: (DrillSessionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (sessions.isEmpty()) return

    val colors = LocalSpaghettiColors.current
    val isDark = colors.isDark
    val tableBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val tableBg = if (isDark) Color(0xFF0B132B) else Color(0xFFFFFFFF)
    val headerBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val dividerColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val rowEvenBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val rowOddBg = if (isDark) Color(0xFF131D35) else Color(0xFFF8FAFC)
    val primaryText = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, tableBorder, RectangleShape)
            .background(tableBg)
            .testTag("session_log_table")
    ) {
        // --- TABLE HEADER ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(text = "SESSION", modifier = Modifier.weight(1f), isHeader = true, isDark = isDark)
            TableDivider(color = dividerColor)
            TableCell(text = "PASSING", modifier = Modifier.width(50.dp), isHeader = true, alignEnd = true, isDark = isDark)
            TableDivider(color = dividerColor)
            TableCell(text = "PROGRESS", modifier = Modifier.width(64.dp), isHeader = true, alignEnd = true, isDark = isDark)
            TableDivider(color = dividerColor)
            TableCell(text = "ACTION", modifier = Modifier.width(68.dp), isHeader = true, alignCenter = true, isDark = isDark)
            TableDivider(color = dividerColor)
            TableCell(text = "DEL", modifier = Modifier.width(38.dp), isHeader = true, alignCenter = true, isDark = isDark)
        }

        // Horizontal grid divider
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(dividerColor))

        // --- SESSION DATA ROWS (UP TO 3) ---
        sessions.take(3).forEachIndexed { index, session ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (index % 2 == 0) rowEvenBg else rowOddBg)
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable resume area across session info, passing, progress, and action
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onResumeSession(session) }
                        .testTag("resume_session_${session.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Combined Competence & Timestamp column (two neat lines inside one column)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = session.scopeLabel,
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = primaryText
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = session.formattedTimestamp,
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = secondaryText
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                    TableDivider(color = dividerColor)
                    // Passing Rate column
                    val passColor = if (session.passingRate >= 70.0) (if (isDark) Color(0xFF22C55E) else Color(0xFF15803D)) else (if (isDark) Color(0xFFF87171) else Color(0xFFDC2626))
                    TableCell(
                        text = String.format(java.util.Locale.US, "%.1f%%", session.passingRate),
                        modifier = Modifier.width(50.dp),
                        isHeader = false,
                        alignEnd = true,
                        textColor = passColor,
                        isDark = isDark
                    )
                    TableDivider(color = dividerColor)
                    // Progress column
                    TableCell(
                        text = "${session.answeredCount}/${session.totalItems}",
                        modifier = Modifier.width(64.dp),
                        isHeader = false,
                        alignEnd = true,
                        textColor = primaryText,
                        isDark = isDark
                    )
                    TableDivider(color = dividerColor)
                    // Action / Resume trigger
                    val actionText = if (session.isCompleted) "COMPLETED" else "RESUME »"
                    val actionColor = if (session.isCompleted) (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) else (if (isDark) Color(0xFF22C55E) else Color(0xFF15803D))
                    TableCell(
                        text = actionText,
                        modifier = Modifier.width(68.dp),
                        isHeader = false,
                        alignCenter = true,
                        textColor = actionColor,
                        isDark = isDark
                    )
                }
                TableDivider(color = dividerColor)
                // Individual delete button (isolated click)
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { onDeleteSession(session) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_compact_session_${session.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete session",
                            tint = secondaryText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (index < sessions.take(3).lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)))
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    modifier: Modifier = Modifier,
    isHeader: Boolean,
    alignEnd: Boolean = false,
    alignCenter: Boolean = false,
    textColor: Color = Color.Unspecified,
    isDark: Boolean = true
) {
    val headerColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 2.dp),
        style = TextStyle(
            fontSize = if (isHeader) 8.5.sp else 9.5.sp,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = if (isHeader) headerColor else textColor,
            textAlign = when {
                alignEnd -> TextAlign.End
                alignCenter -> TextAlign.Center
                else -> TextAlign.Start
            }
        ),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip
    )
}

@Composable
private fun TableDivider(color: Color = Color(0xFF334155)) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(14.dp)
            .background(color)
    )
}
