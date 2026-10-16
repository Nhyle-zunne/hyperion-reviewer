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
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
// import com.nhyle.hyperionmaritime.data.util.NewTrackPreviewAccess
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
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedCompetenceFilter by viewModel.selectedCompetenceFilter.collectAsState()
    val selectedSearchTrack by viewModel.selectedSearchTrack.collectAsState()
    val isOicNwActive by viewModel.isOicNwActive.collectAsState()
    val isGmdssActive by viewModel.isGmdssActive.collectAsState()
    val isEwActive by viewModel.isEngineOicEwProUnlocked.collectAsState()
    val isEmActive by viewModel.isEngineMgtProUnlocked.collectAsState()
    val isDmActive by viewModel.isDeckMgtProUnlocked.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()

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
            subtitle = if (isUnlocked && searchQuery.isNotBlank()) "${searchResults.size} MATCHING ITEMS" else "KEYWORD & TOPIC INDEX",
            showBackButton = false,
            onMenuClick = onOpenDrawer
        )

        if (!isUnlocked) {
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
                            text = "TERMINAL ACCESS LOCKED: Active license required. Activate an OIC-NW Deck or GMDSS license in Activate Key or Pass Store to unlock the offline search index.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.navigateTo(Screen.Store) },
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

                // Tier 1: Track Selection Row
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // OIC-NW Track Chip
                            TrackChip(
                                title = "OIC-NW (DECK)",
                                isSelected = selectedSearchTrack == Track.DECK_OIC_NW,
                                isUnlocked = isOicNwActive,
                                isDarkMode = isDarkMode,
                                onClick = { viewModel.setSearchTrack(Track.DECK_OIC_NW) }
                            )

                            // GMDSS Track Chip
                            TrackChip(
                                title = "GMDSS (GOC)",
                                isSelected = selectedSearchTrack == Track.GMDSS_GOC,
                                isUnlocked = isGmdssActive,
                                isDarkMode = isDarkMode,
                                onClick = { viewModel.setSearchTrack(Track.GMDSS_GOC) }
                            )

                            // ENGINE OIC-EW Track Chip
                            TrackChip(
                                title = "EW",
                                isSelected = selectedSearchTrack == Track.ENGINE_OIC_EW,
                                isUnlocked = isEwActive,
                                isDarkMode = isDarkMode,
                                onClick = { viewModel.setSearchTrack(Track.ENGINE_OIC_EW) }
                            )

                            // ENGINE MGT Track Chip
                            TrackChip(
                                title = "ENGINE MGT",
                                isSelected = selectedSearchTrack == Track.ENGINE_MGT,
                                isUnlocked = isEmActive,
                                isDarkMode = isDarkMode,
                                onClick = { viewModel.setSearchTrack(Track.ENGINE_MGT) }
                            )

                            // DECK MGT Track Chip
                            TrackChip(
                                title = "DECK MGT",
                                isSelected = selectedSearchTrack == Track.DECK_MGT,
                                isUnlocked = isDmActive,
                                isDarkMode = isDarkMode,
                                onClick = { viewModel.setSearchTrack(Track.DECK_MGT) }
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

                    items(searchResults) { question ->
                        SearchResultCard(question = question)
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            if (!isUnlocked) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    question: Question
) {
    val colors = LocalSpaghettiColors.current
    var isExpanded by remember { mutableStateOf(false) }

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

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = colors.muted
                )
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
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    val correctOptText = question.options.getOrNull(question.correctAnswerIndex) ?: ""
                    val correctOptLetter = listOf("A", "B", "C", "D").getOrElse(question.correctAnswerIndex) { "?" }

                    Surface(
                        shape = RectangleShape,
                        color = colors.surface2,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = correctOptText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.success
                            )
                        }
                    }
                }
            }
        }
    }
}
