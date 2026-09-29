package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FailRed
import com.example.ui.theme.MaritimeGold
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun ExamGuidelinesScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isDark = isSystemInDarkTheme()

    BackHandler {
        viewModel.navigateBack()
    }

    val deepNavy = Color(0xFF0F172A)
    val cardSurface = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) BorderStroke(1.dp, Color(0xFF334155)) else BorderStroke(1.dp, Color(0xFFE2E8F0))
    val primaryTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        // Official Deep Navy Top Bar with Back Navigation
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = deepNavy,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier.testTag("exam_guidelines_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column {
                    Text(
                        text = "MARINA Examination Guidelines",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Text(
                        text = "STCW Theoretical Assessment Rules & TOS Benchmark",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaritimeGoldBright
                    )
                }
            }
        }

        // Segmented Track Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFF1E293B),
            contentColor = PureWhite,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaritimeGoldBright,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == 0) MaritimeGoldBright else Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OIC-NW Deck Operational",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) PureWhite else Color(0xFF94A3B8)
                        )
                    }
                }
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == 1) MaritimeGoldBright else Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GMDSS Radio Operator (GOC)",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) PureWhite else Color(0xFF94A3B8)
                        )
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedTab == 0) {
                // Deck OIC-NW Specifications
                item {
                    TrackHeaderCard(
                        title = "OIC-NW Deck Operational Level",
                        timeWindow = "8:00 AM – 12:00 NN (4 Hours / 200 items in 200 mins)",
                        cutoff = "Strictly 70% in each individual competence",
                        totalItems = "200 Items Total",
                        cardSurface = cardSurface,
                        cardBorder = cardBorder,
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                }

                item {
                    CompetencyBreakdownCard(
                        functionTitle = "Function 1: Navigation at the Operational Level",
                        itemCount = "125 items total",
                        accentColor = SeaCyan,
                        competencies = listOf(
                            "Competence 1: Plan and conduct a passage and determine position" to "35 items",
                            "Competence 2: Maintain a safe navigational watch" to "20 items",
                            "Competence 3: Use of Radar and ARPA to maintain safety of navigation" to "15 items",
                            "Competence 4: Use of ECDIS to maintain safety of navigation" to "10 items",
                            "Competence 5: Respond to emergencies" to "10 items",
                            "Competence 7: Use IMO SMCP & English in written and oral form" to "20 items",
                            "Competence 9: Manoeuvre the ship" to "15 items"
                        ),
                        cardSurface = cardSurface,
                        cardBorder = cardBorder,
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                }

                item {
                    CompetencyBreakdownCard(
                        functionTitle = "Function 2: Cargo Handling and Stowage",
                        itemCount = "30 items total",
                        accentColor = MaritimeGoldBright,
                        competencies = listOf(
                            "Competence 10: Monitor loading, stowage, securing, and unloading of cargoes" to "15 items",
                            "Competence 11: Inspect and report defects/damage to cargo spaces, hatch covers, and ballast tanks" to "15 items"
                        ),
                        cardSurface = cardSurface,
                        cardBorder = cardBorder,
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                }

                item {
                    CompetencyBreakdownCard(
                        functionTitle = "Function 3: Ship Operation & Care for Persons on Board",
                        itemCount = "45 items total",
                        accentColor = PassGreen,
                        competencies = listOf(
                            "Competence 12: Ensure compliance with pollution-prevention requirements" to "15 items",
                            "Competence 13: Maintain seaworthiness of the ship" to "15 items",
                            "Competence 17: Monitor compliance with legislative requirements" to "15 items"
                        ),
                        cardSurface = cardSurface,
                        cardBorder = cardBorder,
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                }
            } else {
                // GMDSS GOC Specifications
                item {
                    TrackHeaderCard(
                        title = "GMDSS Radio Operator (GOC)",
                        timeWindow = "8:00 AM – 10:00 AM (2 Hours / 100 items in 100 mins)",
                        cutoff = "Strictly 70% in each individual competence",
                        totalItems = "100 Items Total",
                        cardSurface = cardSurface,
                        cardBorder = cardBorder,
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                }

                item {
                    CompetencyBreakdownCard(
                        functionTitle = "GMDSS Radio Communications Competencies",
                        itemCount = "100 items total",
                        accentColor = MaritimeGoldBright,
                        competencies = listOf(
                            "Competence 1: Transmit and receive information using GMDSS subsystems and equipment" to "85 items",
                            "Competence 2: Provide radio services in emergencies" to "15 items"
                        ),
                        cardSurface = cardSurface,
                        cardBorder = cardBorder,
                        primaryText = primaryTextColor,
                        secondaryText = secondaryTextColor
                    )
                }
            }

            // Shared Official MARINA Rules & Guidance Cards
            item {
                Text(
                    text = "OFFICIAL MARINA EXAMINATION RULES",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    letterSpacing = 1.sp
                )
            }

            // 1. Policy & Timelines
            item {
                GuidanceCard(
                    icon = Icons.Default.Gavel,
                    iconColor = SeaCyan,
                    title = "Policy & Examination Timelines",
                    cardSurface = cardSurface,
                    cardBorder = cardBorder,
                    primaryText = primaryTextColor,
                    secondaryText = secondaryTextColor,
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GuidanceBullet(
                                title = "Single Sitting Rule:",
                                text = "All functions of the theoretical examination must be taken in a single scheduled sitting.",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                            GuidanceBullet(
                                title = "70% Passing Cutoff:",
                                text = "An examinee must attain a minimum score of 70% in EACH individual competence to pass.",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                            GuidanceBullet(
                                title = "3-Year Completion Limit:",
                                text = "Pursuant to STCW Circular 2018-08, candidates have a maximum of 3 years to complete all competencies.",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                            GuidanceBullet(
                                title = "Remedial & Retake Intervals:",
                                text = "Remedial examination may be taken after 1 month up to 1 year; removal examination between 12 and 18 months; full retake after 6 months if failed.",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                        }
                    }
                )
            }

            // 2. Bloom's Taxonomy Weighted Scoring
            item {
                GuidanceCard(
                    icon = Icons.Default.Calculate,
                    iconColor = MaritimeGoldBright,
                    title = "Bloom's Taxonomy Weighted Scoring",
                    cardSurface = cardSurface,
                    cardBorder = cardBorder,
                    primaryText = primaryTextColor,
                    secondaryText = secondaryTextColor,
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Items are weighted according to cognitive depth:",
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryTextColor
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                WeightTag("Easy (K/U)", "3 Points", SeaCyan)
                                WeightTag("Moderate (App)", "6 Points", MaritimeGoldBright)
                                WeightTag("Difficult (Ana)", "9 Points", FailRed)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Official Formula:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor
                                    )
                                    Text(
                                        text = "Final Score (%) = (Total Raw Score Earned / Total Maximum Score) × 100%",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        fontWeight = FontWeight.Bold,
                                        color = SeaCyan
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Example: 10 items (3 Easy @3, 5 Mod @6, 2 Diff @9) = 57 Max Pts.\n• 36 / 57 pts = 63.16% (FAILED)\n• 48 / 57 pts = 84.21% (PASSED)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = secondaryTextColor,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // 3. Reporting & Forfeiture
            item {
                GuidanceCard(
                    icon = Icons.Default.AccessTime,
                    iconColor = FailRed,
                    title = "Reporting & Forfeiture Protocol",
                    cardSurface = cardSurface,
                    cardBorder = cardBorder,
                    primaryText = primaryTextColor,
                    secondaryText = secondaryTextColor,
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GuidanceBullet(
                                title = "30-Minute Early Reporting:",
                                text = "Examinees must report strictly 30 minutes prior to scheduled exam time. Late examinees will be barred and slot forfeited.",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                            GuidanceBullet(
                                title = "Excused Absences:",
                                text = "Only accepted with documented proof: Force majeure, death of immediate family member, severe medical condition, or immediate vessel deployment.",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                        }
                    }
                )
            }

            // 4. Permit & Documents Required
            item {
                GuidanceCard(
                    icon = Icons.Default.Description,
                    iconColor = SeaCyan,
                    title = "Permit & Valid Identification",
                    cardSurface = cardSurface,
                    cardBorder = cardBorder,
                    primaryText = primaryTextColor,
                    secondaryText = secondaryTextColor,
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GuidanceBullet(
                                title = "Long Size Bond Paper:",
                                text = "Examination Permit MUST be printed strictly on LONG SIZE (8.5 × 13 inches) bond paper.",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                            GuidanceBullet(
                                title = "Identification:",
                                text = "Original Seafarer's Record Book (SRB), Seafarer's Identity Document (SID), or MARINA ID (or 2 valid government IDs). Photocopies strictly rejected.",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                            GuidanceBullet(
                                title = "Exam Tools:",
                                text = "Black or blue ballpoint pen, standard basic non-programmable calculator.",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                        }
                    }
                )
            }

            // 5. Attire & Grooming
            item {
                GuidanceCard(
                    icon = Icons.Default.Face,
                    iconColor = MaritimeGoldBright,
                    title = "Prescribed Uniform & Grooming",
                    cardSurface = cardSurface,
                    cardBorder = cardBorder,
                    primaryText = primaryTextColor,
                    secondaryText = secondaryTextColor,
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GuidanceBullet(
                                title = "Type B Marine Uniform:",
                                text = "White short-sleeved polo with epaulets, black slacks, black socks, and polished black leather shoes.",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                            GuidanceBullet(
                                title = "Epaulet Stripes:",
                                text = "NO STRIPES on shoulder boards for OIC examinees (2 stripes for licensed OIC, 3 for Chief Mate/2E).",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                            GuidanceBullet(
                                title = "Grooming Standard:",
                                text = "Plain white undershirt (no prints). Standard 2x3 barber haircut, cleanly shaven (strictly NO beards, moustaches, or goatees).",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                        }
                    }
                )
            }

            // 6. Results & MARINA Support
            item {
                GuidanceCard(
                    icon = Icons.Default.Email,
                    iconColor = PassGreen,
                    title = "Results Release & Inquiries",
                    cardSurface = cardSurface,
                    cardBorder = cardBorder,
                    primaryText = primaryTextColor,
                    secondaryText = secondaryTextColor,
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GuidanceBullet(
                                title = "MISMO Portal:",
                                text = "Official results are posted to your MISMO account under the Assessment tab within 1 working day.",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                            GuidanceBullet(
                                title = "Official Support Contact:",
                                text = "For exam discrepancies or schedule issues, email: stcwo_ead@marina.gov.ph",
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor
                            )
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TrackHeaderCard(
    title: String,
    timeWindow: String,
    cutoff: String,
    totalItems: String,
    cardSurface: Color,
    cardBorder: BorderStroke,
    primaryText: Color,
    secondaryText: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface),
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryText
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PassGreen.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, PassGreen)
                ) {
                    Text(
                        text = totalItems,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PassGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = SeaCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeWindow,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaritimeGoldBright,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Passing Standard: $cutoff",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText
                )
            }
        }
    }
}

@Composable
private fun CompetencyBreakdownCard(
    functionTitle: String,
    itemCount: String,
    accentColor: Color,
    competencies: List<Pair<String, String>>,
    cardSurface: Color,
    cardBorder: BorderStroke,
    primaryText: Color,
    secondaryText: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface),
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = functionTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = itemCount,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = cardBorder.brush?.let { Color.Transparent } ?: Color(0xFF334155).copy(alpha = 0.3f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                competencies.forEach { (comp, items) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = comp,
                            style = MaterialTheme.typography.bodySmall,
                            color = primaryText,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = items,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidanceCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    cardSurface: Color,
    cardBorder: BorderStroke,
    primaryText: Color,
    secondaryText: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface),
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
private fun GuidanceBullet(
    title: String,
    text: String,
    primaryText: Color,
    secondaryText: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaritimeGoldBright
        )
        Text(
            text = buildString {
                append(title)
                append(" ")
                append(text)
            },
            style = MaterialTheme.typography.bodySmall,
            color = secondaryText,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun WeightTag(
    label: String,
    pts: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, color)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 10.sp
            )
            Text(
                text = pts,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 11.sp
            )
        }
    }
}
