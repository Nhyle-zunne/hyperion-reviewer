package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import com.example.data.util.LicenseManager
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.example.data.model.Track
import com.example.data.repository.MaritimeCompetenceRegistry
import com.example.data.repository.MaritimeQuestionBank
import com.example.data.repository.ProQuestionRepository
import com.example.ui.components.LuminousDeskSupportButton
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.openMessengerSupport
import com.example.ui.theme.FailRed
import com.example.ui.theme.MaritimeGold
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.DrillDeliveryMode
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

/**
 * Step 2: Practice Drill Configuration Screen.
 * - Displays selected competence scope in header
 * - Mode selection: "Standard Drill" vs "Intensive Mode"
 * - Randomization card with two switches:
 *   1. "Randomize Question Order" (Sequential vs Shuffled)
 *   2. "Shuffle Choices (A/B/C/D)"
 * - Synchronized Dual Slider + Numeric input clamped to available questions
 */
@Composable
fun PracticeConfigScreen(
    viewModel: ReviewerViewModel,
    competenceId: String? = null,
    isCrpMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val isActivated by viewModel.isActivated.collectAsState()
    val context = LocalContext.current

    // Back navigation pops back to previous screen (Competence Selection or previous)
    BackHandler {
        viewModel.navigateBack()
    }

    val compInfo = competenceId?.let { MaritimeCompetenceRegistry.getCompetenceById(it, selectedTrack) }

    var selectedPart by remember(competenceId) { mutableStateOf<Int?>(null) }
    var showProLockedDialog by remember { mutableStateOf<String?>(null) }

    // Dynamic max available questions
    val maxAvailable = remember(competenceId, selectedPart, selectedTrack, isActivated, isCrpMode, context) {
        if (isCrpMode) {
            if (selectedTrack == Track.GMDSS_GOC) {
                when (competenceId) {
                    "C1" -> when (selectedPart) {
                        1 -> 500
                        2 -> 500
                        else -> 1000
                    }
                    "C2" -> 200
                    else -> 1200
                }
            } else {
                if (competenceId == null) {
                    2065
                } else {
                    MaritimeCompetenceRegistry.PIETRO_CRP_DECK_DISTRIBUTION[competenceId] ?: 100
                }
            }
        } else if (!isActivated) {
            5
        } else if (selectedTrack == Track.GMDSS_GOC) {
            if (competenceId != null) {
                if (selectedPart != null) {
                    ProQuestionRepository.getExpectedQuestionCount(competenceId, selectedPart!!)
                } else {
                    ProQuestionRepository.loadAllCompetenceQuestions(context, competenceId).size
                }
            } else {
                ProQuestionRepository.loadAllCompetenceQuestions(context, "C1").size +
                ProQuestionRepository.loadAllCompetenceQuestions(context, "C2").size
            }
        } else {
            if (competenceId == null) {
                MaritimeQuestionBank.getQuestionsByTrack(selectedTrack).size
            } else {
                MaritimeQuestionBank.getQuestionsByCompetence(competenceId, selectedTrack).size
            }
        }
    }

    val currentLicenseTier = when {
        LicenseManager.isHyperionProUnlocked(context) -> com.example.data.util.LicenseTier.HYPERION_ALL_ACCESS
        selectedTrack == Track.GMDSS_GOC && LicenseManager.isGmdssProUnlocked(context) -> com.example.data.util.LicenseTier.GMDSS
        selectedTrack != Track.GMDSS_GOC && LicenseManager.isOicNwProUnlocked(context) -> com.example.data.util.LicenseTier.OIC_NW
        LicenseManager.isOicNwProUnlocked(context) -> com.example.data.util.LicenseTier.OIC_NW
        LicenseManager.isGmdssProUnlocked(context) -> com.example.data.util.LicenseTier.GMDSS
        else -> com.example.data.util.LicenseTier.OIC_NW
    }
    val isOicNwModule = selectedTrack != Track.GMDSS_GOC
    val isGmdssModule = selectedTrack == Track.GMDSS_GOC
    val isVerified = when (currentLicenseTier) {
        com.example.data.util.LicenseTier.HYPERION_ALL_ACCESS -> true
        com.example.data.util.LicenseTier.OIC_NW -> isOicNwModule
        com.example.data.util.LicenseTier.GMDSS -> isGmdssModule
        else -> false
    }
    var showVerificationDialog by remember { mutableStateOf(false) }
    val isAdvancedLocked = !isVerified

    // Step 2: Delivery Format (Standard Drill vs Intensive Mode)
    var selectedDeliveryFormat by remember { mutableStateOf(DrillDeliveryMode.STANDARD) }

    // Step 3: Randomization Controls (Two switches)
    // In CRP Mode: Randomize Questions is disabled/locked, Shuffled Choices is enabled (ON) by default
    var randomizeQuestionOrder by remember { mutableStateOf(false) } // Default Sequential
    var shuffleChoices by remember { mutableStateOf(if (isCrpMode) true else isActivated) } // Default Shuffle choices ON for CRP / Pro

    // Step 4: Question count state - fixed to maxAvailable in CRP mode
    var questionCount by remember(maxAvailable, isActivated, isCrpMode) {
        mutableIntStateOf(if (isCrpMode) maxAvailable else if (!isActivated) minOf(5, maxAvailable) else maxAvailable)
    }
    var textFieldValue by remember(questionCount) {
        mutableStateOf(questionCount.toString())
    }

    fun updateCount(newVal: Int) {
        if (isCrpMode || !isActivated) return
        val clamped = newVal.coerceIn(1, maxAvailable)
        questionCount = clamped
        textFieldValue = clamped.toString()
    }

    val greetingTitle = if (compInfo != null) {
        if (isCrpMode) "MARINA CRP Drill: ${compInfo.code} - ${compInfo.title}" else "Configure Drill: ${compInfo.code} - ${compInfo.title}"
    } else {
        if (isCrpMode) "MARINA CRP Drill: All Competencies" else "Configure Drill: All Competencies"
    }

    val greetingSubtitle = if (compInfo != null) {
        if (isCrpMode) {
            "MARINA CRP REFERENCE DRILL • $maxAvailable Official Review Questions"
        } else {
            "${compInfo.functionName} • $maxAvailable Items"
        }
    } else {
        if (isCrpMode) {
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
            title = if (isCrpMode) "MARINA CRP PRACTICE" else "PRACTICE ALL QUESTIONS",
            subtitle = if (compInfo != null) compInfo.code else "All Competencies",
            showBackButton = true,
            onBackClick = { viewModel.navigateBack() },
            onLegalClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isCrpMode) {
                // Prominent Legal Disclaimer Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isCrpMode) PassGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (isCrpMode) "STEP 2 OF 2 · MARINA CRP PRACTICE" else "STEP 2 OF 2 • PRACTICE ALL QUESTIONS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCrpMode) PassGreen else MaterialTheme.colorScheme.onPrimaryContainer,
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
                            color = if (isCrpMode) PassGreen else MaterialTheme.colorScheme.primary
                        )

                        compInfo?.description?.let { desc ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // GMDSS Part Selector (if GMDSS track and specific competence selected)
            if (selectedTrack == Track.GMDSS_GOC && competenceId != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (isCrpMode) PassGreen else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCrpMode) "SELECT CRP PART OR ALL PARTS" else "SELECT SPECIFIC PART OR ALL PARTS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCrpMode) PassGreen else MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if (isCrpMode) {
                                if (competenceId == "C1") {
                                    androidx.compose.foundation.lazy.LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            FilterChip(
                                                selected = selectedPart == null,
                                                onClick = { selectedPart = null },
                                                label = { Text("All Parts (1,000 Qs)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PassGreen,
                                                    selectedLabelColor = PureWhite,
                                                    containerColor = Color(0xFFE2E8F0),
                                                    labelColor = Color(0xFF334155)
                                                )
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = selectedPart == 1,
                                                onClick = { selectedPart = 1 },
                                                label = { Text("Part 1 (500 Qs)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PassGreen,
                                                    selectedLabelColor = PureWhite,
                                                    containerColor = Color(0xFFE2E8F0),
                                                    labelColor = Color(0xFF334155)
                                                )
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = selectedPart == 2,
                                                onClick = { selectedPart = 2 },
                                                label = { Text("Part 2 (500 Qs)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PassGreen,
                                                    selectedLabelColor = PureWhite,
                                                    containerColor = Color(0xFFE2E8F0),
                                                    labelColor = Color(0xFF334155)
                                                )
                                            )
                                        }
                                    }
                                } else if (competenceId == "C2") {
                                    androidx.compose.foundation.lazy.LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            FilterChip(
                                                selected = true,
                                                onClick = { selectedPart = null },
                                                label = { Text("Part 1 (200 Qs)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PassGreen,
                                                    selectedLabelColor = PureWhite,
                                                    containerColor = Color(0xFFE2E8F0),
                                                    labelColor = Color(0xFF334155)
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                val parts = ProQuestionRepository.getPartsForCompetence(competenceId)
                                androidx.compose.foundation.lazy.LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        FilterChip(
                                            selected = selectedPart == null,
                                            onClick = { selectedPart = null },
                                            label = { Text("All Parts (${ProQuestionRepository.loadAllCompetenceQuestions(context, competenceId).size})", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF0F172A),
                                                selectedLabelColor = PureWhite,
                                                containerColor = Color(0xFFE2E8F0),
                                                labelColor = Color(0xFF334155)
                                            )
                                        )
                                    }
                                    items(parts.size) { index ->
                                        val pNum = parts[index]
                                        val count = ProQuestionRepository.getExpectedQuestionCount(competenceId, pNum)
                                        FilterChip(
                                            selected = selectedPart == pNum,
                                            onClick = { selectedPart = pNum },
                                            label = { Text("Part $pNum ($count)", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF0F172A),
                                                selectedLabelColor = PureWhite,
                                                containerColor = Color(0xFFE2E8F0),
                                                labelColor = Color(0xFF334155)
                                            )
                                        )
                                    }
                                }
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(14.dp)
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
                                    color = if (isStandardSelected) Color(0xFFFF5A67) else Color(0xFF2A2A2A),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isStandardSelected,
                                    onClick = { selectedDeliveryFormat = DrillDeliveryMode.STANDARD },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFFF5A67),
                                        unselectedColor = Color(0xFF555555)
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Standard Drill",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isCrpMode) PassGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = if (isCrpMode) "SEQUENTIAL ACTIVE" else "FIXED ITEMS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCrpMode) PassGreen else MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Progress through each question once with immediate rationale. Concludes with a session summary.",
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
                                .alpha(if (isVerified) 1f else 0.45f)
                                .clickable {
                                    if (isVerified) {
                                        selectedDeliveryFormat = DrillDeliveryMode.INTENSIVE
                                    } else {
                                        showVerificationDialog = true
                                    }
                                }
                                .border(
                                    width = if (isIntensiveSelected) 1.5.dp else 1.dp,
                                    color = if (isIntensiveSelected) Color(0xFFFF5A67) else Color(0xFF2A2A2A),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                            shape = RoundedCornerShape(10.dp)
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
                                            showVerificationDialog = true
                                        }
                                    },
                                    enabled = true,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFFF5A67),
                                        unselectedColor = Color(0xFF555555),
                                        disabledSelectedColor = Color(0xFF555555),
                                        disabledUnselectedColor = Color(0xFF555555)
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Intensive Mode",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "RECYCLING QUEUE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (!isVerified) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF1E1517), RoundedCornerShape(2.dp))
                                                    .border(1.dp, Color(0xFFFF5A67), RoundedCornerShape(2.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "LOCKED",
                                                    color = Color(0xFFFF5A67),
                                                    fontSize = 9.sp,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isCrpMode) "LOCKED · Sequential reference delivery only." else "Missed questions display rationale and recycle to the end of the queue. The drill only finishes when all questions are mastered.",
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
                        .alpha(if (isVerified) 1f else 0.45f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(14.dp)
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
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF1E1517), RoundedCornerShape(2.dp))
                                        .border(1.dp, Color(0xFFFF5A67), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LOCKED",
                                        color = Color(0xFFFF5A67),
                                        fontSize = 9.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Switch 1: Randomize Question Order (Sequential vs Shuffled)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isVerified) {
                                        randomizeQuestionOrder = !randomizeQuestionOrder
                                    } else {
                                        showVerificationDialog = true
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Randomize Question Order",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = if (randomizeQuestionOrder) "SHUFFLED" else "SEQUENTIAL",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (randomizeQuestionOrder) {
                                        "Questions are dynamically shuffled at the start of the drill."
                                    } else {
                                        "Questions appear in strict original database order (#1, #2, #3...)."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = if (isVerified) randomizeQuestionOrder else false,
                                onCheckedChange = {
                                    if (isVerified) {
                                        randomizeQuestionOrder = it
                                    } else {
                                        showVerificationDialog = true
                                    }
                                },
                                enabled = true,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = PureWhite,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                                    disabledUncheckedThumbColor = PureWhite.copy(alpha = 0.6f),
                                    disabledUncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.testTag("switch_randomize_question_order")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Switch 2: Shuffle Choices (A/B/C/D) - Enabled (ON) by default in CRP mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { shuffleChoices = !shuffleChoices }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Shuffle Choices (A/B/C/D)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (shuffleChoices) PassGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = if (shuffleChoices) "ENABLED (ON)" else "FIXED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (shuffleChoices) PassGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Permutes option letters to prevent letter memorization while preserving correct answer keys.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = shuffleChoices,
                                onCheckedChange = { shuffleChoices = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = PassGreen,
                                    uncheckedThumbColor = PureWhite,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.testTag("switch_shuffle_choices")
                            )
                        }
                    }
                }
            }

            // Step 4: Dual Question Count Selector (Slider + Numeric Input) - Fixed in CRP Mode
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isAdvancedLocked) 0.45f else 1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (isCrpMode) PassGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCrpMode) "3. QUESTION COUNT ($maxAvailable ITEMS POOL)" else "3. QUESTION COUNT (1 TO $maxAvailable AVAILABLE)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCrpMode) PassGreen else MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isAdvancedLocked) {
                                ClearanceRequiredBadge()
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isCrpMode) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PassGreen.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, PassGreen.copy(alpha = 0.3f)),
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
                                            text = "Fixed Pietro Question Pool: $maxAvailable Items",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Free practice automatically delivers all $maxAvailable sequential questions in this competency block.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // Quick Preset Chips (10, 25, 50, 100, All Available)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val presets = listOf(10, 25, 50, 100).filter { it <= maxAvailable }
                                for (preset in presets) {
                                    val isSelected = questionCount == preset
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { updateCount(preset) },
                                        label = { Text("$preset", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF0F172A),
                                            selectedLabelColor = PureWhite,
                                            containerColor = Color(0xFFE2E8F0),
                                            labelColor = Color(0xFF334155)
                                        )
                                    )
                                }
                                // All Available Chip
                                val isAllPreset = questionCount == maxAvailable
                                FilterChip(
                                    selected = isAllPreset,
                                    onClick = { updateCount(maxAvailable) },
                                    label = { Text("All ($maxAvailable)", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0F172A),
                                        selectedLabelColor = PureWhite,
                                        containerColor = Color(0xFFE2E8F0),
                                        labelColor = Color(0xFF334155)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dual Row: Interactive Slider + Numeric Type Box
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Slider(
                                        value = questionCount.toFloat(),
                                        onValueChange = { floatVal ->
                                            updateCount(floatVal.toInt())
                                        },
                                        valueRange = 1f..maxAvailable.toFloat().coerceAtLeast(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("practice_question_count_slider")
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "1 item",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$maxAvailable items",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                OutlinedTextField(
                                    value = textFieldValue,
                                    onValueChange = { input ->
                                        val digitsOnly = input.filter { it.isDigit() }
                                        textFieldValue = digitsOnly
                                        if (digitsOnly.isNotEmpty()) {
                                            val parsed = digitsOnly.toIntOrNull()
                                            if (parsed != null) {
                                                val clamped = parsed.coerceIn(1, maxAvailable)
                                                questionCount = clamped
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .width(88.dp)
                                        .testTag("practice_question_count_input"),
                                    label = { Text("Items", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Institutional Clearance Callout Banner if unverified
            if (!isVerified) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161918)),
                        border = BorderStroke(1.dp, Color(0xFFFF5A67).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5A67),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VERIFICATION REQUIRED",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5A67),
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Intensive recycling queues and question randomization require institutional terminal verification.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LuminousDeskSupportButton(
                                title = "CONTACT SUPPORT DESK ↗",
                                subtitle = "Direct candidate identity verification & seat clearance"
                            ) {
                                openMessengerSupport(context)
                            }
                        }
                    }
                }
            }

            // Launch Action Button
            item {
                val launchLabel = if (isCrpMode) {
                    "Start MARINA CRP Drill ($questionCount Items)"
                } else if (selectedDeliveryFormat == DrillDeliveryMode.INTENSIVE) {
                    "Start Intensive Mode Drill ($questionCount Items Queue)"
                } else {
                    "Start Standard Practice Drill ($questionCount Items)"
                }

                Button(
                    onClick = {
                        viewModel.startConfiguredPractice(
                            competenceId = competenceId,
                            selectedPart = selectedPart,
                            deliveryMode = selectedDeliveryFormat,
                            questionCount = questionCount,
                            shuffleQuestions = if (isCrpMode) false else randomizeQuestionOrder,
                            shuffleChoices = shuffleChoices,
                            isCrpMode = isCrpMode
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("launch_practice_drill_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCrpMode) PassGreen else MaterialTheme.colorScheme.primary,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (selectedDeliveryFormat == DrillDeliveryMode.INTENSIVE && !isCrpMode) Icons.Default.Autorenew else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = launchLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Pro Feature Lock Dialog
    if (showProLockedDialog != null) {
        AlertDialog(
            onDismissRequest = { showProLockedDialog = null },
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
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = showProLockedDialog!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProLockedDialog = null
                        viewModel.navigateTo(Screen.HowToAvail)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Account & Device Help", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showProLockedDialog = null }) {
                    Text("Continue Free Practice")
                }
            }
        )
    }

    // Institutional Verification Dialog
    if (showVerificationDialog) {
        AlertDialog(
            onDismissRequest = { showVerificationDialog = false },
            containerColor = Color(0xFF141414),
            title = {
                Text(
                    text = "VERIFICATION REQUIRED",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5A67)
                )
            },
            text = {
                Text(
                    text = "Intensive recycling queues and question randomization require institutional terminal verification.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVerificationDialog = false
                        openMessengerSupport(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1D1D1D),
                        contentColor = Color(0xFFFF5A67)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFF5A67)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CONTACT SUPPORT DESK ↗", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showVerificationDialog = false }
                ) {
                    Text("DISMISS", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ClearanceRequiredBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF1E1517),
        border = BorderStroke(1.dp, Color(0xFFFF5A67))
    ) {
        Text(
            text = "[CLEARANCE REQUIRED]",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = Color(0xFFFF5A67),
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
