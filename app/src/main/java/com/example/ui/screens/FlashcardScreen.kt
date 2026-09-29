package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Difficulty
import com.example.data.model.Question
import com.example.data.repository.MaritimeQuestionBank
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.MianButton
import com.example.ui.components.MianRuleButton
import com.example.ui.components.MianRuleCard
import com.example.ui.components.MianSectionHeader
import com.example.ui.components.SpaghettiProgressBar
import com.example.ui.theme.LocalSpaghettiColors
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" Flashcard Revision Screen:
 * Active recall study, tap-to-flip editorial card, 3dp top rules, sharp square badges.
 */
@Composable
fun FlashcardScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()

    var selectedComp by remember { mutableStateOf("ALL") }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val trackQuestions = remember(selectedTrack) {
        MaritimeQuestionBank.getQuestionsByTrack(selectedTrack, isProUnlocked = true)
    }

    val filteredQuestions = remember(trackQuestions, selectedComp) {
        if (selectedComp == "ALL") trackQuestions
        else trackQuestions.filter { it.competenceId == selectedComp }
    }

    val competences = remember(trackQuestions) {
        listOf("ALL") + trackQuestions.map { it.competenceId }.distinct().sorted()
    }

    BackHandler {
        viewModel.navigateBack()
    }

    if (filteredQuestions.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "NO FLASHCARDS FOUND",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.muted
                )
            }
        }
        return
    }

    val safeIndex = currentIndex.coerceIn(0, filteredQuestions.size - 1)
    val currentCard = filteredQuestions[safeIndex]
    val isBookmarked = bookmarkedIds.contains(currentCard.id)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = "FLASHCARD REVISION MODE",
            subtitle = "${filteredQuestions.size} CARDS • ACTIVE RECALL",
            showBackButton = true,
            onBackClick = { viewModel.navigateBack() }
        )

        // Progress Bar
        val progressFraction = (safeIndex + 1).toFloat() / filteredQuestions.size.coerceAtLeast(1)
        SpaghettiProgressBar(progress = progressFraction, height = 3.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Competence Selector Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(competences) { comp ->
                        val isSelected = selectedComp == comp
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = if (isSelected) colors.accent else colors.surface,
                            modifier = Modifier.clickable {
                                selectedComp = comp
                                currentIndex = 0
                                isFlipped = false
                            }
                        ) {
                            Text(
                                text = comp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) colors.accentForeground else colors.foreground,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Card Metadata Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CARD ${safeIndex + 1} OF ${filteredQuestions.size} • ${currentCard.competenceId}".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.muted,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleBookmark(currentCard.id) }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) colors.accent else colors.foreground
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // The Flashcard ("Rules, not boxes": 3dp top rule, flat surface, tap to flip)
                MianRuleCard(
                    ruleColor = if (isFlipped) colors.success else colors.accent,
                    containerColor = colors.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clickable { isFlipped = !isFlipped }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        AnimatedContent(
                            targetState = isFlipped,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                            label = "flashcard_content"
                        ) { flipped ->
                            if (!flipped) {
                                // FRONT: QUESTION
                                Column {
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
                                                text = "QUESTION PROMPT",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colors.muted,
                                                fontSize = 10.sp
                                            )
                                        }

                                        DifficultyBadge(difficulty = currentCard.difficulty)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = currentCard.questionText,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            lineHeight = 25.sp,
                                            letterSpacing = (-0.3).sp
                                        ),
                                        color = colors.foreground
                                    )
                                }
                            } else {
                                // BACK: ANSWER & RATIONALE
                                Column {
                                    val correctOptText = currentCard.options.getOrNull(currentCard.correctAnswerIndex) ?: ""
                                    val correctOptLetter = listOf("A", "B", "C", "D").getOrElse(currentCard.correctAnswerIndex) { "?" }

                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = colors.surface2
                                    ) {
                                        Text(
                                            text = "CORRECT ANSWER",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.success,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(verticalAlignment = Alignment.Top) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(colors.success),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = correctOptLetter,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = colors.primaryForeground
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = correctOptText,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.success,
                                            lineHeight = 22.sp
                                        )
                                    }

                                    if (!currentCard.rationale.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "MARINA RATIONALE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.muted,
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentCard.rationale,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.foreground,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = colors.muted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFlipped) "TAP TO FLIP TO QUESTION" else "TAP TO FLIP TO ANSWER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.muted,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // Bottom Navigation Controls
            Column {
                // 3dp Rule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colors.border)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MianRuleButton(
                        onClick = {
                            if (safeIndex > 0) {
                                currentIndex = safeIndex - 1
                                isFlipped = false
                            }
                        },
                        enabled = safeIndex > 0,
                        ruleColor = colors.border,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            tint = if (safeIndex > 0) colors.foreground else colors.muted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PREVIOUS",
                            fontWeight = FontWeight.ExtraBold,
                            color = if (safeIndex > 0) colors.foreground else colors.muted,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    MianButton(
                        onClick = {
                            if (safeIndex < filteredQuestions.size - 1) {
                                currentIndex = safeIndex + 1
                                isFlipped = false
                            }
                        },
                        enabled = safeIndex < filteredQuestions.size - 1,
                        accent = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "NEXT",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
