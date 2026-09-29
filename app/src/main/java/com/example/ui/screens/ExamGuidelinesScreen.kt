package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.MianRuleCard
import com.example.ui.components.MianSectionHeader
import com.example.ui.theme.LocalSpaghettiColors
import com.example.viewmodel.ReviewerViewModel

/**
 * Editorial MIAN "Spaghetti" MARINA Examination Guidelines Screen:
 * Official Table of Specifications (TOS), passing criteria, weighted scoring, dress code & permit guidelines.
 */
@Composable
fun ExamGuidelinesScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    var selectedTab by remember { mutableIntStateOf(0) }

    BackHandler {
        viewModel.navigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = "MARINA EXAM GUIDELINES",
            subtitle = "STCW THEORETICAL ASSESSMENT RULES & TOS BENCHMARK",
            showBackButton = true,
            onBackClick = { viewModel.navigateBack() }
        )

        // Segmented Track Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.foreground,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = colors.accent,
                    height = 3.dp
                )
            },
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border)
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
                            tint = if (selectedTab == 0) colors.accent else colors.muted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OIC-NW DECK OPERATIONAL",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            color = if (selectedTab == 0) colors.accent else colors.muted
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
                            tint = if (selectedTab == 1) colors.accent else colors.muted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GMDSS RADIO OPERATOR (GOC)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            color = if (selectedTab == 1) colors.accent else colors.muted
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedTab == 0) {
                // Deck OIC-NW Specifications
                item {
                    TrackHeaderCard(
                        title = "OIC-NW DECK OPERATIONAL LEVEL",
                        timeWindow = "8:00 AM – 12:00 NN (4 Hours / 200 items in 200 mins)",
                        cutoff = "Strictly 70.0% in each individual competence",
                        totalItems = "200 ITEMS TOTAL"
                    )
                }

                item {
                    MianSectionHeader(title = "FUNCTION 1: NAVIGATION AT THE OPERATIONAL LEVEL (125 ITEMS)")
                }

                item {
                    CompetencyBreakdownCard(
                        functionTitle = "FUNCTION 1: NAVIGATION",
                        itemCount = "125 items total",
                        competencies = listOf(
                            "Competence 1: Plan and conduct a passage and determine position" to "35 items",
                            "Competence 2: Maintain a safe navigational watch" to "20 items",
                            "Competence 3: Use of Radar and ARPA to maintain safety of navigation" to "15 items",
                            "Competence 4: Use of ECDIS to maintain safety of navigation" to "10 items",
                            "Competence 5: Respond to emergencies" to "10 items",
                            "Competence 7: Use IMO SMCP & English in written and oral form" to "20 items",
                            "Competence 9: Manoeuvre the ship" to "15 items"
                        )
                    )
                }

                item {
                    MianSectionHeader(title = "FUNCTION 2: CARGO HANDLING AND STOWAGE (30 ITEMS)")
                }

                item {
                    CompetencyBreakdownCard(
                        functionTitle = "FUNCTION 2: CARGO HANDLING & STOWAGE",
                        itemCount = "30 items total",
                        competencies = listOf(
                            "Competence 10: Monitor loading, stowage, securing, and unloading of cargoes" to "15 items",
                            "Competence 11: Inspect and report defects/damage to cargo spaces, hatch covers, and ballast tanks" to "15 items"
                        )
                    )
                }

                item {
                    MianSectionHeader(title = "FUNCTION 3: SHIP OPERATION & CARE FOR PERSONS (45 ITEMS)")
                }

                item {
                    CompetencyBreakdownCard(
                        functionTitle = "FUNCTION 3: CONTROLLING SHIP OPERATION",
                        itemCount = "45 items total",
                        competencies = listOf(
                            "Competence 12: Ensure compliance with pollution prevention requirements" to "15 items",
                            "Competence 13: Maintain seaworthiness of the ship" to "15 items",
                            "Competence 17: Apply leadership and teamworking skills" to "15 items"
                        )
                    )
                }
            } else {
                // GMDSS Specifications
                item {
                    TrackHeaderCard(
                        title = "GMDSS GENERAL OPERATOR'S CERTIFICATE (GOC)",
                        timeWindow = "8:00 AM – 10:00 AM (2 Hours / 100 items in 120 mins)",
                        cutoff = "Strictly 70.0% in each individual competence",
                        totalItems = "100 ITEMS TOTAL"
                    )
                }

                item {
                    MianSectionHeader(title = "COMPETENCE 1: RADIO REGULATIONS & SUBSYSTEMS (85 ITEMS)")
                }

                item {
                    CompetencyBreakdownCard(
                        functionTitle = "COMPETENCE 1: RADIO SUBSYSTEMS",
                        itemCount = "85 items total",
                        competencies = listOf(
                            "VHF/MF/HF Radio Communications & Digital Selective Calling (DSC)" to "30 items",
                            "Satellite Systems: Inmarsat-C, FleetBroadband, Iridium GMDSS" to "25 items",
                            "Search and Rescue Locating Devices: EPIRB, SART, AIS-SART" to "15 items",
                            "Maritime Safety Information (MSI): NAVTEX, SafetyNET, Enhanced Group Call" to "15 items"
                        )
                    )
                }

                item {
                    MianSectionHeader(title = "COMPETENCE 2: DISTRESS & SAFETY PROCEDURES (15 ITEMS)")
                }

                item {
                    CompetencyBreakdownCard(
                        functionTitle = "COMPETENCE 2: DISTRESS PROCEDURES",
                        itemCount = "15 items total",
                        competencies = listOf(
                            "GMDSS Distress, Urgency and Safety Communications Protocols" to "8 items",
                            "Search and Rescue (SAR) Procedures & On-Scene Coordination" to "7 items"
                        )
                    )
                }
            }

            // General Exam Rules & Requirements (Both Tracks)
            item {
                MianSectionHeader(title = "OFFICIAL MARINA EXAMINATION POLICIES")
            }

            item {
                EvaluationSummaryCard()
            }

            item {
                GeneralRuleCard(
                    title = "PASSING THRESHOLD & CUTOFF POLICY",
                    content = "To pass the examination, a candidate MUST obtain at least 70.0% in EVERY competence within the function. Scoring 100% in C1 and 65% in C2 results in an overall FAILURE for the function. All competencies are equally mandatory.",
                    icon = Icons.Default.Security
                )
            }

            item {
                GeneralRuleCard(
                    title = "EXAM PERMIT & IDENTIFICATION",
                    content = "Examinees must present their official MARINA Examination Permit (printed) along with their valid Seafarer's Identification and Record Book (SIRB) or National ID. Digital permits on mobile phones are strictly prohibited in the exam hall.",
                    icon = Icons.Default.Gavel
                )
            }

            item {
                GeneralRuleCard(
                    title = "DRESS CODE & CONDUCT",
                    content = "Standard Maritime Uniform or White Polo/Barong with dark slacks and black formal leather shoes is mandatory. Electronic devices, smartwatches, and programmable calculators are strictly forbidden.",
                    icon = Icons.Default.Info
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TrackHeaderCard(
    title: String,
    timeWindow: String,
    cutoff: String,
    totalItems: String
) {
    val colors = LocalSpaghettiColors.current

    MianRuleCard(
        ruleColor = colors.accent,
        containerColor = colors.surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                        text = "TABLE OF SPECIFICATIONS (TOS)",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = colors.surface2
                ) {
                    Text(
                        text = totalItems,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accent,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.foreground,
                letterSpacing = (-0.2).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "• Time Window: $timeWindow",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.foreground
                )
                Text(
                    text = "• Passing Requirement: $cutoff",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.success,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CompetencyBreakdownCard(
    functionTitle: String,
    itemCount: String,
    competencies: List<Pair<String, String>>
) {
    val colors = LocalSpaghettiColors.current

    MianRuleCard(
        ruleColor = colors.border,
        containerColor = colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = functionTitle.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    letterSpacing = (-0.1).sp,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = colors.surface2
                ) {
                    Text(
                        text = itemCount.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.muted,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                competencies.forEach { (compName, quota) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface2)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = compName,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.foreground,
                            modifier = Modifier.weight(1f),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = quota.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accent,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EvaluationSummaryCard() {
    val colors = LocalSpaghettiColors.current

    MianRuleCard(
        ruleColor = colors.accent,
        containerColor = colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "BLOOM'S TAXONOMY WEIGHTED SCORING",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.foreground,
                letterSpacing = (-0.1).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Official MARINA theoretical examinations evaluate questions across three cognitive difficulty levels with weighted scoring points:",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = colors.surface2,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "EASY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = colors.success)
                        Text(text = "3 PTS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = colors.foreground)
                        Text(text = "Knowledge & Recall", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = colors.muted)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = colors.surface2,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "MODERATE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = colors.foreground)
                        Text(text = "6 PTS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = colors.foreground)
                        Text(text = "Understanding & App", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = colors.muted)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = colors.surface2,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "DIFFICULT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = colors.accent)
                        Text(text = "9 PTS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = colors.foreground)
                        Text(text = "Analysis & Eval", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = colors.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralRuleCard(
    title: String,
    content: String,
    icon: ImageVector
) {
    val colors = LocalSpaghettiColors.current

    MianRuleCard(
        ruleColor = colors.border,
        containerColor = colors.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.foreground,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    letterSpacing = (-0.1).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
