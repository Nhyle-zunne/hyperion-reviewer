package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.local.MistakeEntry
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.QuizMode
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianButton
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.MianSectionHeader
import com.nhyle.hyperionmaritime.ui.theme.CanvasDark
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" Mistakes Vault Screen:
 * High contrast editorial diagnostics, 3dp top rules, red theme accents, individual & clear-all deletion.
 */
@Composable
fun VaultScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val mistakeEntries by viewModel.mistakeEntries.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val requiredMistakePasses by viewModel.requiredMistakePasses.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }

    val trackMistakes = mistakeEntries.filter { it.track.equals(selectedTrack.name, ignoreCase = true) }
    val questionsToRedeem = trackMistakes.map { entry ->
        MaritimeQuestionBank.ALL_QUESTIONS.find { it.id == entry.questionId } ?: entry.question
    }

    val filteredMistakes = remember(trackMistakes, searchQuery) {
        if (searchQuery.isBlank()) {
            trackMistakes
        } else {
            val query = searchQuery.trim().lowercase()
            trackMistakes.filter { entry ->
                val q = MaritimeQuestionBank.ALL_QUESTIONS.find { it.id == entry.questionId } ?: entry.question
                q.questionText.lowercase().contains(query) ||
                q.id.lowercase().contains(query) ||
                q.functionId.lowercase().contains(query) ||
                q.competenceId.lowercase().contains(query) ||
                q.options.any { it.lowercase().contains(query) } ||
                q.explanation.lowercase().contains(query)
            }
        }
    }

    BackHandler {
        viewModel.navigateBack()
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = RectangleShape,
            containerColor = colors.surface,
            title = {
                Text(
                    text = "CLEAR MISTAKES NOTEBOOK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foreground
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove all recorded mistakes? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllMistakes()
                        showClearDialog = false
                    },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.background == CanvasDark) Color(0xFFEF4444) else Color(0xFFDC2626),
                        contentColor = Color.White
                    )
                ) {
                    Text("CLEAR ALL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearDialog = false },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.foreground)
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = "MISTAKES NOTEBOOK (VAULT)",
            subtitle = "${trackMistakes.size} ITEMS TO REDEEM",
            showBackButton = false,
            onMenuClick = onOpenDrawer,
            onClearAllClick = if (trackMistakes.isNotEmpty()) {
                { showClearDialog = true }
            } else null
        )

        if (trackMistakes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = colors.success,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "MISTAKES VAULT IS EMPTY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Questions answered incorrectly during practice or mock exams are stored here for targeted redemption.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    MianRuleCard(
                        ruleColor = Color(0xFFEF4444),
                        containerColor = colors.surface
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "REDEMPTION QUEUE (${trackMistakes.size} QUESTIONS)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.foreground,
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rule: A question is automatically redeemed and removed from your vault after $requiredMistakePasses consecutive correct answer${if (requiredMistakePasses > 1) "s" else ""}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            MianButton(
                                onClick = {
                                    if (questionsToRedeem.isNotEmpty()) {
                                        viewModel.navigateTo(Screen.PracticeConfig(isMistakesMode = true))
                                    }
                                },
                                accent = true,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "START VAULT REDEMPTION PRACTICE",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "SEARCH MISTAKES...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.muted,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = colors.foreground,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = colors.foreground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.foreground,
                            unfocusedTextColor = colors.foreground,
                            cursorColor = colors.accent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    MianSectionHeader(title = if (searchQuery.isBlank()) "CURRENT VAULT ITEMS" else "SEARCH RESULTS (${filteredMistakes.size})")
                }

                if (filteredMistakes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NO MATCHING MISTAKES FOUND",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.muted
                            )
                        }
                    }
                } else {
                    items(filteredMistakes) { mistake ->
                        val question = MaritimeQuestionBank.ALL_QUESTIONS.find { it.id == mistake.questionId } ?: mistake.question
                        VaultMistakeCard(
                            mistake = mistake,
                            question = question,
                            requiredPasses = requiredMistakePasses,
                            onRemove = { viewModel.removeMistake(mistake.questionId) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun VaultMistakeCard(
    mistake: MistakeEntry,
    question: Question,
    requiredPasses: Int = 2,
    onRemove: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    val isDark = colors.background == CanvasDark
    var isExpanded by androidx.compose.runtime.saveable.rememberSaveable(mistake.questionId) { 
        mutableStateOf(false) 
    }
    
    // Red theme styling
    val cardBorderColor = Color(0xFFEF4444)
    val badgeBg = if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
    val badgeText = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)

    MianRuleCard(
        ruleColor = cardBorderColor,
        containerColor = colors.surface,
        border = BorderStroke(1.5.dp, cardBorderColor),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RectangleShape,
                    color = colors.surface2
                ) {
                    Text(
                        text = "${mistake.competenceId} • ITEM ${mistake.questionId}".uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RectangleShape,
                        color = badgeBg,
                        border = BorderStroke(1.dp, cardBorderColor)
                    ) {
                        Text(
                            text = "${mistake.consecutiveCorrect.coerceAtMost(requiredPasses)}/$requiredPasses CORRECT",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeText,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove mistake",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand choices",
                        tint = colors.muted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colors.foreground,
                lineHeight = 20.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Failed ${mistake.incorrectCount} time(s) • Last seen: ${mistake.lastAnsweredDate}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                fontSize = 11.sp
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val letters = listOf("A", "B", "C", "D", "E", "F")
                    val correctOptText = question.options.getOrNull(question.correctAnswerIndex) ?: ""
                    val correctOptLetter = letters.getOrElse(question.correctAnswerIndex) { "?" }
                    val otherOptions = question.options.mapIndexed { idx, text -> idx to text }
                        .filter { (idx, text) -> idx != question.correctAnswerIndex && text.isNotBlank() }

                    // 1. Correct Answer: Listed first and emphasized
                    Surface(
                        shape = RectangleShape,
                        color = colors.surface2,
                        border = BorderStroke(1.dp, colors.success.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RectangleShape)
                                    .background(colors.success),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = correctOptLetter,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = colors.primaryForeground
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = colors.success,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "CORRECT ANSWER",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.success,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = correctOptText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.foreground,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // 2. Other Options: Listed below the correct answer
                    otherOptions.forEach { (idx, optText) ->
                        val optLetter = letters.getOrElse(idx) { "?" }
                        Surface(
                            shape = RectangleShape,
                            color = colors.surface2.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RectangleShape)
                                        .background(colors.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = optLetter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = colors.muted
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = optText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.foreground,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
