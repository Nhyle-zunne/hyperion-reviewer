package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.ui.motion.HyperionDialog
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.MianSectionHeader
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

/**
 * Editorial MIAN "Spaghetti" Search Screen:
 * Dual-tier filter architecture (Track -> Competence), strict license lockout, and zero-rounding industrial styling.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedCompetenceFilter by viewModel.selectedCompetenceFilter.collectAsStateWithLifecycle()
    val selectedSearchTrack by viewModel.selectedSearchTrack.collectAsStateWithLifecycle()
    val selectedTrack by viewModel.selectedTrack.collectAsStateWithLifecycle()
    val isOicNwActive by viewModel.isOicNwActive.collectAsStateWithLifecycle()
    val isGmdssActive by viewModel.isGmdssActive.collectAsStateWithLifecycle()
    val isEwActive by viewModel.isEngineOicEwProUnlocked.collectAsStateWithLifecycle()
    val isEmActive by viewModel.isEngineMgtProUnlocked.collectAsStateWithLifecycle()
    val isDmActive by viewModel.isDeckMgtProUnlocked.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()

    fun isTrackLicensed(track: Track): Boolean = when (track) {
        Track.DECK_OIC_NW -> isOicNwActive
        Track.GMDSS_GOC -> isGmdssActive
        Track.ENGINE_OIC_EW -> isEwActive
        Track.DECK_MGT -> isDmActive
        Track.ENGINE_MGT -> isEmActive
    }

    val hasAnyActiveLicense = isOicNwActive || isGmdssActive || isEwActive || isDmActive || isEmActive || isUnlocked


    // Auto-switch away from a locked track if the user owns a different active pass
    androidx.compose.runtime.LaunchedEffect(hasAnyActiveLicense, isOicNwActive, isGmdssActive, isEwActive, isDmActive, isEmActive) {
        if (hasAnyActiveLicense && !isTrackLicensed(selectedSearchTrack)) {
            val firstActive = when {
                isOicNwActive -> Track.DECK_OIC_NW
                isGmdssActive -> Track.GMDSS_GOC
                isEwActive -> Track.ENGINE_OIC_EW
                isDmActive -> Track.DECK_MGT
                isEmActive -> Track.ENGINE_MGT
                else -> null
            }
            if (firstActive != null) {
                viewModel.setSearchTrack(firstActive)
            }
        }
    }

    val oicCompetencies = listOf("ALL", "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "C10", "C11", "C12")
    val gmdssCompetencies = listOf("ALL", "C1", "C2")
    val currentCompetencies = when (selectedSearchTrack) {
        Track.DECK_OIC_NW -> oicCompetencies
        Track.GMDSS_GOC -> gmdssCompetencies
        else -> listOf("ALL") + MaritimeCompetenceRegistry.getCompetencesForTrack(selectedSearchTrack).map { it.competenceId }
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
            title = "SEARCH QUESTION BANK",
            subtitle = if (hasAnyActiveLicense && searchQuery.isNotBlank()) "${searchResults.size} MATCHING ITEMS" else "KEYWORD & TOPIC INDEX",
            showBackButton = false,
            onMenuClick = onOpenDrawer
        )

        if (!hasAnyActiveLicense) {
            // Full Lockout State (Zero Active Licenses)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RectangleShape,
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = colors.accent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "TERMINAL ACCESS LOCKED",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "TERMINAL ACCESS LOCKED: Active license required. Activate an offline key or subscribe in Pass Store to unlock the offline search index.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.triggerPaywall(selectedTrack) },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.accentForeground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "VIEW PASS STORE",
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
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

                    // Editorial Search Input Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = {
                            Text(
                                text = "SEARCH BY KEYWORD, TOPIC, OR ITEM #...",
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
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
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

                // Tier 1: Track Selection Grid (Clean 2-Row Full-Width Layout)
                item {
                    Column {
                        Text(
                            text = "SELECT REVIEW TRACK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.muted,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Row 1: Operational Deck & Radio (50% / 50% split)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TrackChip(
                                    title = "OIC-NW (DECK)",
                                    isSelected = selectedSearchTrack == Track.DECK_OIC_NW,
                                    isUnlocked = isOicNwActive,
                                    isDarkMode = isDarkMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (isOicNwActive) viewModel.setSearchTrack(Track.DECK_OIC_NW)
                                        else viewModel.triggerPaywall(Track.DECK_OIC_NW)
                                    }
                                )
                                TrackChip(
                                    title = "GMDSS (GOC)",
                                    isSelected = selectedSearchTrack == Track.GMDSS_GOC,
                                    isUnlocked = isGmdssActive,
                                    isDarkMode = isDarkMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (isGmdssActive) viewModel.setSearchTrack(Track.GMDSS_GOC)
                                        else viewModel.triggerPaywall(Track.GMDSS_GOC)
                                    }
                                )
                            }

                            // Row 2: Engine & Management (Equal 3-way split)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TrackChip(
                                    title = "ENGINE EW",
                                    isSelected = selectedSearchTrack == Track.ENGINE_OIC_EW,
                                    isUnlocked = isEwActive,
                                    isDarkMode = isDarkMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (isEwActive) viewModel.setSearchTrack(Track.ENGINE_OIC_EW)
                                        else viewModel.triggerPaywall(Track.ENGINE_OIC_EW)
                                    }
                                )
                                TrackChip(
                                    title = "DECK MGT",
                                    isSelected = selectedSearchTrack == Track.DECK_MGT,
                                    isUnlocked = isDmActive,
                                    isDarkMode = isDarkMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (isDmActive) viewModel.setSearchTrack(Track.DECK_MGT)
                                        else viewModel.triggerPaywall(Track.DECK_MGT)
                                    }
                                )
                                TrackChip(
                                    title = "ENGINE MGT",
                                    isSelected = selectedSearchTrack == Track.ENGINE_MGT,
                                    isUnlocked = isEmActive,
                                    isDarkMode = isDarkMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (isEmActive) viewModel.setSearchTrack(Track.ENGINE_MGT)
                                        else viewModel.triggerPaywall(Track.ENGINE_MGT)
                                    }
                                )
                            }
                        }
                    }

                // Tier 2: Dynamic Competence Selection Row
                item {
                    Column {
                        Text(
                            text = "FILTER BY COMPETENCE (${selectedSearchTrack.shortName})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.muted,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentCompetencies.forEach { comp ->
                                val isSelected = (comp == "ALL" && selectedCompetenceFilter == null) ||
                                        (selectedCompetenceFilter == comp)

                                val borderColor = if (isSelected) {
                                    if (isDarkMode) Color(0xFF22C55E) else Color(0xFF15803D)
                                } else {
                                    if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
                                }
                                val surfColor = if (isSelected) {
                                    if (isDarkMode) Color(0xFF0F172A) else Color(0xFFDCFCE7)
                                } else {
                                    if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFFFF)
                                }
                                val txtColor = if (isSelected) {
                                    if (isDarkMode) Color(0xFF4ADE80) else Color(0xFF14532D)
                                } else {
                                    if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF0F172A)
                                }

                                Surface(
                                    shape = RectangleShape,
                                    color = surfColor,
                                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                                    modifier = Modifier.clickable {
                                        if (comp == "ALL") {
                                            viewModel.setCompetenceFilter(null)
                                        } else {
                                            viewModel.setCompetenceFilter(comp)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = comp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = txtColor,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (searchQuery.isBlank() && searchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ENTER SEARCH KEYWORDS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.foreground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Type search terms to query the offline ${selectedSearchTrack.shortName} question bank.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "NO MATCHING QUESTIONS FOUND",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.foreground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try adjusting your search terms or clearing the competency filter.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.muted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    item {
                        MianSectionHeader(title = "MATCHING QUESTIONS (${searchResults.size})")
                    }

                    items(
                        items = searchResults,
                        key = { "${it.competenceId}_${it.id}" }
                    ) { question ->
                        SearchResultCard(
                            question = question,
                            isBookmarked = bookmarkedIds.contains(question.id),
                            onToggleBookmark = { viewModel.toggleBookmark(question.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackChip(
    title: String,
    isSelected: Boolean,
    isUnlocked: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        !isUnlocked -> if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        isSelected -> if (isDarkMode) Color(0xFF22C55E) else Color(0xFF15803D)
        else -> if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
    }
    val backgroundColor = when {
        !isUnlocked -> if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        isSelected -> if (isDarkMode) Color(0xFF0F172A) else Color(0xFFDCFCE7)
        else -> if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    }
    val textColor = when {
        !isUnlocked -> if (isDarkMode) Color(0xFF475569) else Color(0xFF94A3B8)
        isSelected -> if (isDarkMode) Color(0xFF4ADE80) else Color(0xFF14532D)
        else -> if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF0F172A)
    }

    Surface(
        shape = RectangleShape,
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false,
                letterSpacing = 0.3.sp
            )
            if (!isUnlocked) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    question: Question,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    var isExpanded by androidx.compose.runtime.saveable.rememberSaveable(question.id) { 
        mutableStateOf(false) 
    }

    MianRuleCard(
        ruleColor = colors.border,
        containerColor = colors.surface,
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
                        text = "${question.functionId} · ${question.competenceId} • ITEM ${question.id}".uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark Question",
                            tint = if (isBookmarked) colors.accent else colors.muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
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
                maxLines = if (isExpanded) Int.MAX_VALUE else 2
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
