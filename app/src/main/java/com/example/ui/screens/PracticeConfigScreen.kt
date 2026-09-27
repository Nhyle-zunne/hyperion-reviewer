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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.Track
import com.example.data.repository.MaritimeCompetenceRegistry
import com.example.data.repository.MaritimeQuestionBank
import com.example.data.repository.ProQuestionRepository
import com.example.ui.components.MaritimeTopBar
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
    modifier: Modifier = Modifier
) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val isActivated by viewModel.isActivated.collectAsState()
    val context = LocalContext.current

    // Back navigation returns to Step 1: Competence Selection
    BackHandler {
        viewModel.navigateTo(Screen.PracticeCompetenceSelect)
    }

    val compInfo = competenceId?.let { MaritimeCompetenceRegistry.getCompetenceById(it, selectedTrack) }

    var selectedPart by remember(competenceId) { mutableStateOf<Int?>(null) }

    // Dynamic max available questions
    val maxAvailable = remember(competenceId, selectedPart, selectedTrack, isActivated, context) {
        if (!isActivated) {
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

    // Step 2: Delivery Format (Standard Drill vs Intensive Mode)
    var deliveryMode by remember { mutableStateOf(DrillDeliveryMode.STANDARD) }

    // Step 3: Randomization Controls (Two switches) - locked in Free Reviewer Mode
    var randomizeQuestionOrder by remember { mutableStateOf(false) } // Default Sequential
    var shuffleChoices by remember { mutableStateOf(isActivated) } // Default Shuffle choices if Pro

    // Step 4: Question count state
    var questionCount by remember(maxAvailable, isActivated) {
        mutableIntStateOf(if (!isActivated) minOf(5, maxAvailable) else minOf(25, maxAvailable).coerceAtLeast(1))
    }
    var textFieldValue by remember(questionCount) {
        mutableStateOf(questionCount.toString())
    }

    fun updateCount(newVal: Int) {
        if (!isActivated) return
        val clamped = newVal.coerceIn(1, maxAvailable)
        questionCount = clamped
        textFieldValue = clamped.toString()
    }

    val greetingTitle = if (compInfo != null) {
        "Configure Drill: ${compInfo.code} - ${compInfo.title}"
    } else {
        "Configure Drill: All Competencies"
    }

    val greetingSubtitle = if (compInfo != null) {
        "${compInfo.functionName} • $maxAvailable Items Available"
    } else {
        "${selectedTrack.title} • Full Pool ($maxAvailable Items Available)"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        MaritimeTopBar(
            title = "Drill Setup",
            subtitle = if (compInfo != null) compInfo.code else "All Competencies",
            showBackButton = true,
            onBackClick = { viewModel.navigateTo(Screen.PracticeCompetenceSelect) },
            onLegalClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Examinee Greeting Banner with Selected Competence Title
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                    border = BorderStroke(1.5.dp, MaritimeGoldBright),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaritimeGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "STEP 2 OF 2 • PRACTICE DRILL CONFIGURATION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaritimeGoldBright,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = greetingTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = greetingSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = SeaCyan
                        )

                        compInfo?.description?.let { desc ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite.copy(alpha = 0.7f),
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
                        border = BorderStroke(1.dp, SeaCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = SeaCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SELECT SPECIFIC PART OR ALL PARTS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SeaCyan,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
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
                                            selectedContainerColor = SeaCyan,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF1E293B),
                                            labelColor = PureWhite
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
                                            selectedContainerColor = SeaCyan,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF1E293B),
                                            labelColor = PureWhite
                                        )
                                    )
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
                                tint = MaritimeGoldBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "1. DRILL DELIVERY FORMAT",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaritimeGoldBright,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Option 1: Standard Drill
                        val isStandard = deliveryMode == DrillDeliveryMode.STANDARD
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isStandard) 2.dp else 1.dp,
                                    color = if (isStandard) MaritimeGoldBright else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { deliveryMode = DrillDeliveryMode.STANDARD }
                                .testTag("drill_mode_standard"),
                            color = if (isStandard) MaritimeGoldBright.copy(alpha = 0.12f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isStandard) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isStandard) MaritimeGoldBright else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Standard Drill",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isStandard) MaritimeGoldBright else PureWhite
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SeaCyan.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "FIXED ITEMS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SeaCyan,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Progress through each question once with immediate rationale. Concludes with a session summary.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PureWhite.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Option 2: Intensive Mode (Dynamic Recycling Queue)
                        val isIntensive = deliveryMode == DrillDeliveryMode.INTENSIVE
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isIntensive) 2.dp else 1.dp,
                                    color = if (isIntensive) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { deliveryMode = DrillDeliveryMode.INTENSIVE }
                                .testTag("drill_mode_intensive"),
                            color = if (isIntensive) Color(0x22F59E0B) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isIntensive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isIntensive) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Intensive Mode",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isIntensive) Color(0xFFF59E0B) else PureWhite
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0x33F59E0B)
                                        ) {
                                            Text(
                                                text = "RECYCLING QUEUE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF59E0B),
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Missed questions display rationale and recycle to the end of the queue. The drill only finishes when all questions are mastered.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PureWhite.copy(alpha = 0.7f),
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = SeaCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2. RANDOMIZATION & SEQUENCING",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SeaCyan,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Switch 1: Randomize Question Order (Sequential vs Shuffled)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { randomizeQuestionOrder = !randomizeQuestionOrder }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Randomize Question Order",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (randomizeQuestionOrder) Color(0x33F59E0B) else SeaCyan.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (randomizeQuestionOrder) "SHUFFLED" else "SEQUENTIAL",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (randomizeQuestionOrder) MaritimeGoldBright else SeaCyan,
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
                                    color = PureWhite.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = randomizeQuestionOrder,
                                onCheckedChange = { randomizeQuestionOrder = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = SeaCyan,
                                    uncheckedThumbColor = PureWhite,
                                    uncheckedTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.testTag("switch_randomize_question_order")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Switch 2: Shuffle Choices (A/B/C/D)
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
                                        color = PureWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (shuffleChoices) PassGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (shuffleChoices) "RANDOMIZED" else "FIXED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (shuffleChoices) PassGreen else Color.LightGray,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Permutes option letters to prevent letter memorization while preserving correct answer keys.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PureWhite.copy(alpha = 0.7f),
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
                                    uncheckedTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.testTag("switch_shuffle_choices")
                            )
                        }
                    }
                }
            }

            // Step 4: Dual Question Count Selector (Slider + Numeric Input)
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
                                tint = MaritimeGoldBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3. QUESTION COUNT (1 TO $maxAvailable AVAILABLE)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaritimeGoldBright,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

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
                                        selectedContainerColor = MaritimeGoldBright,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = PureWhite
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
                                    selectedContainerColor = MaritimeGoldBright,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = PureWhite
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
                                        thumbColor = MaritimeGoldBright,
                                        activeTrackColor = MaritimeGoldBright,
                                        inactiveTrackColor = Color(0xFF334155)
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
                                        color = PureWhite.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "$maxAvailable items",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PureWhite.copy(alpha = 0.5f)
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
                                    focusedBorderColor = MaritimeGoldBright,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                )
                            )
                        }
                    }
                }
            }

            // Launch Action Button
            item {
                val launchLabel = if (deliveryMode == DrillDeliveryMode.INTENSIVE) {
                    "Start Intensive Mode Drill ($questionCount Items Queue)"
                } else {
                    "Start Standard Practice Drill ($questionCount Items)"
                }

                Button(
                    onClick = {
                        viewModel.startConfiguredPractice(
                            competenceId = competenceId,
                            selectedPart = selectedPart,
                            deliveryMode = deliveryMode,
                            questionCount = questionCount,
                            shuffleQuestions = randomizeQuestionOrder,
                            shuffleChoices = shuffleChoices
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("launch_practice_drill_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deliveryMode == DrillDeliveryMode.INTENSIVE) Color(0xFFF59E0B) else MaritimeGoldBright,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (deliveryMode == DrillDeliveryMode.INTENSIVE) Icons.Default.Autorenew else Icons.Default.PlayArrow,
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
}
