package com.nhyle.hyperionmaritime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.studytime.DayDetailStats
import com.nhyle.hyperionmaritime.data.studytime.StudyTimeCalculator
import com.nhyle.hyperionmaritime.data.studytime.StudyTimeSegmentEntity
import com.nhyle.hyperionmaritime.data.studytime.TrackStudyStats
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dedicated Study Time Statistics Screen:
 * - Tabs: PERIOD, DAY, WEEK, MONTH, TREND (Rectangular segmented control with 0dp corners)
 * - Month calendar with intensity shading, small square legend, square outline on selected day, month total
 * - Day detail panel: TOTAL STUDY TIME, MAX FOCUS TIME, START TIME, END TIME (+1 if post-midnight)
 * - Cumulative line chart with square line caps and no rounded markers
 * - Donut chart breakdown by track with square legend (no "Other" category)
 * - Hourly timeline grid of square cells
 * - Daily square bar totals for WEEK, MONTH, and comparative TREND
 * - Safe empty state box: "NO STUDY TIME RECORDED"
 * - Dark & Light mode contrast >= 4.5:1
 */
@Composable
fun StudyTimeScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val isDark = colors.isDark

    BackHandler {
        viewModel.navigateToDashboard()
    }

    val allSegments by viewModel.getAllStudySegments().collectAsState(initial = emptyList())

    // Tabs: 0: PERIOD, 1: DAY, 2: WEEK, 3: MONTH, 4: TREND
    var selectedTabIdx by remember { mutableStateOf(1) } // Default to DAY tab

    // Date navigation states
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedYearMonth by remember { mutableStateOf(YearMonth.now()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = "STUDY TIME",
            subtitle = "Telemetry & Analytics",
            showBackButton = false,
            onMenuClick = onOpenDrawer
        )

        // 1. Rectangular Segmented Control (0dp corners)
        val tabLabels = listOf("PERIOD", "DAY", "WEEK", "MONTH", "TREND")
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RectangleShape,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                tabLabels.forEachIndexed { idx, label ->
                    val isSelected = selectedTabIdx == idx
                    val selectedBg = if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)
                    val unselectedBg = Color.Transparent
                    val textColor = if (isSelected) Color.White else colors.muted

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isSelected) selectedBg else unselectedBg, RectangleShape)
                            .clickable { selectedTabIdx = idx }
                            .padding(vertical = 10.dp)
                            .testTag("study_time_tab_$label"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = textColor
                        )
                    }
                }
            }
        }

        // 2. Tab Content (Scrollable)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTabIdx) {
                0 -> PeriodTabContent(segments = allSegments, isDark = isDark)
                1 -> DayTabContent(
                    segments = allSegments,
                    selectedDate = selectedDate,
                    onDateChanged = { selectedDate = it },
                    isDark = isDark
                )
                2 -> WeekTabContent(
                    segments = allSegments,
                    anchorDate = selectedDate,
                    onAnchorDateChanged = { selectedDate = it },
                    isDark = isDark
                )
                3 -> MonthTabContent(
                    segments = allSegments,
                    selectedYearMonth = selectedYearMonth,
                    selectedDate = selectedDate,
                    onYearMonthChanged = { selectedYearMonth = it },
                    onDaySelected = {
                        selectedDate = it
                        selectedTabIdx = 1 // Switch to DAY tab when selecting day in calendar
                    },
                    isDark = isDark
                )
                4 -> TrendTabContent(segments = allSegments, isDark = isDark)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ===================================================================================
// DAY TAB CONTENT
// ===================================================================================

@Composable
private fun DayTabContent(
    segments: List<StudyTimeSegmentEntity>,
    selectedDate: LocalDate,
    onDateChanged: (LocalDate) -> Unit,
    isDark: Boolean
) {
    val colors = LocalSpaghettiColors.current
    val daySegments = remember(segments, selectedDate) {
        StudyTimeCalculator.filterSegmentsForDay(segments, selectedDate)
    }
    val dayStats = remember(daySegments, selectedDate) {
        StudyTimeCalculator.calculateDayDetailStats(segments, selectedDate)
    }
    val trackBreakdown = remember(daySegments) {
        StudyTimeCalculator.calculateTrackBreakdown(daySegments)
    }

    // Date Selector Bar (0dp corners)
    DateSelectorBar(
        title = selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd (EEE)", Locale.US)).uppercase(),
        onPrevious = { onDateChanged(selectedDate.minusDays(1)) },
        onNext = { onDateChanged(selectedDate.plusDays(1)) },
        isDark = isDark
    )

    if (daySegments.isEmpty()) {
        EmptyStudyTimeBox(isDark = isDark)
        return
    }

    // Day Detail Panel (0dp corners)
    DayDetailPanel(stats = dayStats, isDark = isDark)

    // Cumulative Line Chart for Selected Day
    CumulativeLineChartPanel(
        segments = daySegments,
        selectedDate = selectedDate,
        isDark = isDark
    )

    // Breakdown by Track (Donut Chart with Square Legend)
    TrackBreakdownDonutPanel(
        breakdown = trackBreakdown,
        isDark = isDark
    )

    // Hourly Timeline Grid
    HourlyTimelineGridPanel(
        segments = daySegments,
        selectedDate = selectedDate,
        isDark = isDark
    )
}

// ===================================================================================
// MONTH TAB CONTENT
// ===================================================================================

@Composable
private fun MonthTabContent(
    segments: List<StudyTimeSegmentEntity>,
    selectedYearMonth: YearMonth,
    selectedDate: LocalDate,
    onYearMonthChanged: (YearMonth) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    isDark: Boolean
) {
    val monthStart = selectedYearMonth.atDay(1)
    val monthEnd = selectedYearMonth.atEndOfMonth()
    val monthSegments = remember(segments, selectedYearMonth) {
        StudyTimeCalculator.filterSegmentsBetweenDates(segments, monthStart, monthEnd)
    }
    val monthTotalMs = remember(monthSegments) {
        StudyTimeCalculator.calculateTotalDurationMs(monthSegments)
    }
    val trackBreakdown = remember(monthSegments) {
        StudyTimeCalculator.calculateTrackBreakdown(monthSegments)
    }

    // Month Selector Bar (0dp corners)
    DateSelectorBar(
        title = selectedYearMonth.format(DateTimeFormatter.ofPattern("yyyy.MM", Locale.US)),
        onPrevious = { onYearMonthChanged(selectedYearMonth.minusMonths(1)) },
        onNext = { onYearMonthChanged(selectedYearMonth.plusMonths(1)) },
        isDark = isDark
    )

    // Month Calendar Grid with Intensity Shading & Legend
    MonthCalendarGrid(
        yearMonth = selectedYearMonth,
        segments = monthSegments,
        selectedDate = selectedDate,
        monthTotalMs = monthTotalMs,
        onDayClick = onDaySelected,
        isDark = isDark
    )

    if (monthSegments.isEmpty()) {
        EmptyStudyTimeBox(isDark = isDark)
        return
    }

    // Daily Bar Totals for Month
    val daysInMonth = selectedYearMonth.lengthOfMonth()
    val dailyTotals = remember(monthSegments, selectedYearMonth) {
        (1..daysInMonth).map { day ->
            val date = selectedYearMonth.atDay(day)
            val dSegs = StudyTimeCalculator.filterSegmentsForDay(monthSegments, date)
            date to StudyTimeCalculator.calculateTotalDurationMs(dSegs)
        }
    }
    val activeDays = dailyTotals.count { it.second > 0L }
    val dailyAvgMs = if (activeDays > 0) monthTotalMs / activeDays else 0L

    DailyBarTotalsPanel(
        title = "DAILY TOTALS · ${selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)).uppercase()}",
        dailyData = dailyTotals.map { "${it.first.dayOfMonth}" to it.second },
        totalMs = monthTotalMs,
        avgMs = dailyAvgMs,
        isDark = isDark
    )

    // Breakdown by Track for Month
    TrackBreakdownDonutPanel(
        breakdown = trackBreakdown,
        isDark = isDark
    )
}

// ===================================================================================
// WEEK TAB CONTENT
// ===================================================================================

@Composable
private fun WeekTabContent(
    segments: List<StudyTimeSegmentEntity>,
    anchorDate: LocalDate,
    onAnchorDateChanged: (LocalDate) -> Unit,
    isDark: Boolean
) {
    // Determine Monday..Sunday week window
    val dayOfWeek = anchorDate.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
    val weekStart = anchorDate.minusDays((dayOfWeek - 1).toLong())
    val weekEnd = weekStart.plusDays(6)

    val weekSegments = remember(segments, weekStart, weekEnd) {
        StudyTimeCalculator.filterSegmentsBetweenDates(segments, weekStart, weekEnd)
    }
    val weekTotalMs = remember(weekSegments) {
        StudyTimeCalculator.calculateTotalDurationMs(weekSegments)
    }
    val trackBreakdown = remember(weekSegments) {
        StudyTimeCalculator.calculateTrackBreakdown(weekSegments)
    }

    // Week Selector Bar (0dp corners)
    val formatter = DateTimeFormatter.ofPattern("MM.dd", Locale.US)
    DateSelectorBar(
        title = "${weekStart.format(formatter)} – ${weekEnd.format(formatter)} (${weekStart.year})",
        onPrevious = { onAnchorDateChanged(anchorDate.minusWeeks(1)) },
        onNext = { onAnchorDateChanged(anchorDate.plusWeeks(1)) },
        isDark = isDark
    )

    if (weekSegments.isEmpty()) {
        EmptyStudyTimeBox(isDark = isDark)
        return
    }

    // 7 Daily Bar Totals
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val dayLabels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val dailyData = days.mapIndexed { idx, d ->
        val dSegs = StudyTimeCalculator.filterSegmentsForDay(weekSegments, d)
        dayLabels[idx] to StudyTimeCalculator.calculateTotalDurationMs(dSegs)
    }
    val activeDays = dailyData.count { it.second > 0L }
    val dailyAvgMs = if (activeDays > 0) weekTotalMs / activeDays else 0L

    DailyBarTotalsPanel(
        title = "WEEKLY DAILY TOTALS",
        dailyData = dailyData,
        totalMs = weekTotalMs,
        avgMs = dailyAvgMs,
        isDark = isDark
    )

    // Breakdown by Track
    TrackBreakdownDonutPanel(
        breakdown = trackBreakdown,
        isDark = isDark
    )
}

// ===================================================================================
// PERIOD TAB CONTENT
// ===================================================================================

@Composable
private fun PeriodTabContent(
    segments: List<StudyTimeSegmentEntity>,
    isDark: Boolean
) {
    if (segments.isEmpty()) {
        EmptyStudyTimeBox(isDark = isDark)
        return
    }

    val totalMs = remember(segments) {
        StudyTimeCalculator.calculateTotalDurationMs(segments)
    }
    val allDates = remember(segments) {
        segments.map {
            java.time.Instant.ofEpochMilli(it.startEpochMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        }.distinct()
    }
    val avgMs = if (allDates.isNotEmpty()) totalMs / allDates.size else 0L
    val trackBreakdown = remember(segments) {
        StudyTimeCalculator.calculateTrackBreakdown(segments)
    }

    val colors = LocalSpaghettiColors.current
    val panelBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val panelBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    // Overall Period Overview
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = panelBg,
        border = BorderStroke(1.dp, panelBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ALL-TIME STUDY RECORD",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "TOTAL TIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = StudyTimeCalculator.formatDurationShortHoursMins(totalMs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column {
                    Text(
                        text = "ACTIVE DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${allDates.size} Days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column {
                    Text(
                        text = "DAILY AVERAGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = StudyTimeCalculator.formatDurationShortHoursMins(avgMs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    // Breakdown by Track
    TrackBreakdownDonutPanel(
        breakdown = trackBreakdown,
        isDark = isDark
    )
}

// ===================================================================================
// TREND TAB CONTENT
// ===================================================================================

@Composable
private fun TrendTabContent(
    segments: List<StudyTimeSegmentEntity>,
    isDark: Boolean
) {
    if (segments.isEmpty()) {
        EmptyStudyTimeBox(isDark = isDark)
        return
    }

    val colors = LocalSpaghettiColors.current
    val today = LocalDate.now()
    val thisWeekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val thisWeekEnd = thisWeekStart.plusDays(6)

    val lastWeekStart = thisWeekStart.minusWeeks(1)
    val lastWeekEnd = thisWeekStart.minusDays(1)

    val currentPeriodSegs = remember(segments) {
        StudyTimeCalculator.filterSegmentsBetweenDates(segments, thisWeekStart, thisWeekEnd)
    }
    val prevPeriodSegs = remember(segments) {
        StudyTimeCalculator.filterSegmentsBetweenDates(segments, lastWeekStart, lastWeekEnd)
    }

    val currentTotalMs = remember(currentPeriodSegs) {
        StudyTimeCalculator.calculateTotalDurationMs(currentPeriodSegs)
    }
    val prevTotalMs = remember(prevPeriodSegs) {
        StudyTimeCalculator.calculateTotalDurationMs(prevPeriodSegs)
    }

    val deltaMs = currentTotalMs - prevTotalMs
    val deltaMins = deltaMs / 60_000L

    val panelBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val panelBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val positiveColor = if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)
    val negativeColor = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = panelBg,
        border = BorderStroke(1.dp, panelBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "WEEK-OVER-WEEK TREND (THIS WEEK VS LAST WEEK)",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "THIS WEEK",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = StudyTimeCalculator.formatDurationShortHoursMins(currentTotalMs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.foreground,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text(
                        text = "LAST WEEK",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = StudyTimeCalculator.formatDurationShortHoursMins(prevTotalMs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "DELTA",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                        fontSize = 10.sp
                    )
                    val deltaSign = if (deltaMs >= 0) "+" else ""
                    val deltaColor = if (deltaMs >= 0) positiveColor else negativeColor
                    Text(
                        text = "$deltaSign${StudyTimeCalculator.formatDurationShortHoursMins(Math.abs(deltaMs))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = deltaColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    // Comparison Bars
    val compareData = listOf(
        "LAST WEEK" to prevTotalMs,
        "THIS WEEK" to currentTotalMs
    )
    DailyBarTotalsPanel(
        title = "PERIOD COMPARISON (SQUARE BARS)",
        dailyData = compareData,
        totalMs = currentTotalMs,
        avgMs = currentTotalMs / 7,
        isDark = isDark
    )
}

// ===================================================================================
// UI SUBCOMPONENTS
// ===================================================================================

@Composable
private fun DateSelectorBar(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isDark: Boolean
) {
    val colors = LocalSpaghettiColors.current
    val barBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val barBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = barBg,
        border = BorderStroke(1.dp, barBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous",
                    tint = colors.foreground,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = colors.foreground,
                letterSpacing = 0.5.sp
            )

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next",
                    tint = colors.foreground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DayDetailPanel(
    stats: DayDetailStats,
    isDark: Boolean
) {
    val colors = LocalSpaghettiColors.current
    val panelBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val panelBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = panelBg,
        border = BorderStroke(1.dp, panelBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "DAY TELEMETRY & FOCUS SUMMARY",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: TOTAL STUDY TIME
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC), RectangleShape)
                        .border(1.dp, panelBorder, RectangleShape)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "TOTAL TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = StudyTimeCalculator.formatDurationShortHoursMins(stats.totalDurationMs),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                        )
                    }
                }

                // Metric 2: MAX FOCUS TIME
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC), RectangleShape)
                        .border(1.dp, panelBorder, RectangleShape)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "MAX FOCUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = StudyTimeCalculator.formatDurationShortHoursMins(stats.maxFocusDurationMs),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.foreground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 3: START TIME
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC), RectangleShape)
                        .border(1.dp, panelBorder, RectangleShape)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "START TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stats.startTimeFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.foreground
                        )
                    }
                }

                // Metric 4: END TIME
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC), RectangleShape)
                        .border(1.dp, panelBorder, RectangleShape)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "END TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stats.endTimeFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.foreground
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cumulative line chart for the selected day:
 * Square line caps (StrokeCap.Square) and no rounded markers.
 */
@Composable
private fun CumulativeLineChartPanel(
    segments: List<StudyTimeSegmentEntity>,
    selectedDate: LocalDate,
    isDark: Boolean
) {
    val colors = LocalSpaghettiColors.current
    val panelBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val panelBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val gridColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val lineColor = if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)

    val points = remember(segments, selectedDate) {
        StudyTimeCalculator.calculateCumulativeMinutes(segments, selectedDate)
    }
    val maxMinutes = points.maxOfOrNull { it.second }?.coerceAtLeast(60f) ?: 60f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = panelBg,
        border = BorderStroke(1.dp, panelBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "CUMULATIVE STUDY TIME (24 HOURS)",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("cumulative_line_chart")
            ) {
                val w = size.width
                val h = size.height

                // Draw background horizontal grid lines (0dp/square)
                val gridSteps = 3
                for (g in 0..gridSteps) {
                    val y = h - (h / gridSteps) * g
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Build path with square joins/caps
                val path = Path()
                points.forEachIndexed { idx, pt ->
                    val x = (pt.first / 24f) * w
                    val y = h - (pt.second / maxMinutes) * h
                    if (idx == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Square,
                        join = StrokeJoin.Miter
                    )
                )

                // Draw square point markers at every recorded hour point
                points.forEach { pt ->
                    if (pt.second > 0f) {
                        val px = (pt.first / 24f) * w
                        val py = h - (pt.second / maxMinutes) * h
                        val boxSize = 4.dp.toPx()
                        drawRect(
                            color = lineColor,
                            topLeft = Offset(px - boxSize / 2, py - boxSize / 2),
                            size = Size(boxSize, boxSize)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // X-axis 24h labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = colors.muted
                    )
                }
            }
        }
    }
}

/**
 * Donut chart for breakdown by track with square legend:
 * - Donut allowed as exception; container and legends strictly square.
 * - No "Other" or non-study category.
 */
@Composable
private fun TrackBreakdownDonutPanel(
    breakdown: List<TrackStudyStats>,
    isDark: Boolean
) {
    val colors = LocalSpaghettiColors.current
    val panelBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val panelBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = panelBg,
        border = BorderStroke(1.dp, panelBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "BREAKDOWN BY TRACK",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (breakdown.isEmpty()) {
                Text(
                    text = "No study time recorded in this period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
                return@Column
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Donut Chart Canvas
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        var startAngle = -90f
                        val strokeWidth = 18.dp.toPx()
                        val arcSize = size.width - strokeWidth

                        breakdown.forEach { item ->
                            val sweep = (item.percentage / 100f) * 360f
                            drawArc(
                                color = item.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += sweep
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Square Legend Beside Donut
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    breakdown.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Square color indicator (0dp)
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(item.color, RectangleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.foreground,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${StudyTimeCalculator.formatDurationShortHoursMins(item.totalDurationMs)} (${String.format(Locale.US, "%.1f", item.percentage)}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = colors.muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hourly timeline for the selected day: grid of square cells by hour.
 */
@Composable
private fun HourlyTimelineGridPanel(
    segments: List<StudyTimeSegmentEntity>,
    selectedDate: LocalDate,
    isDark: Boolean
) {
    val colors = LocalSpaghettiColors.current
    val panelBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val panelBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    val hourlyMins = remember(segments, selectedDate) {
        StudyTimeCalculator.calculateHourlyMinutes(segments, selectedDate)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = panelBg,
        border = BorderStroke(1.dp, panelBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "HOURLY TIMELINE (24 SQUARE CELLS)",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6 Columns x 4 Rows = 24 square cells
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0 until 6) {
                            val hour = row * 6 + col
                            val mins = hourlyMins[hour]
                            val cellColor = when {
                                mins >= 45 -> if (isDark) Color(0xFF22C55E) else Color(0xFF15803D)
                                mins >= 20 -> if (isDark) Color(0xFF16A34A).copy(alpha = 0.7f) else Color(0xFF4ADE80)
                                mins > 0 -> if (isDark) Color(0xFF14532D).copy(alpha = 0.5f) else Color(0xFFDCFCE7)
                                else -> if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            }
                            val textMuted = if (mins > 0 && isDark) Color.White else colors.muted

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .background(cellColor, RectangleShape)
                                    .border(1.dp, panelBorder, RectangleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = String.format(Locale.US, "%02d:00", hour),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = textMuted
                                    )
                                    if (mins > 0) {
                                        Text(
                                            text = "${mins}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Month calendar with intensity shading, small square legend, and square outline on selected day.
 */
@Composable
private fun MonthCalendarGrid(
    yearMonth: YearMonth,
    segments: List<StudyTimeSegmentEntity>,
    selectedDate: LocalDate,
    monthTotalMs: Long,
    onDayClick: (LocalDate) -> Unit,
    isDark: Boolean
) {
    val colors = LocalSpaghettiColors.current
    val panelBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val panelBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val accentGreen = if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)

    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7 // 0 for Sun.. 6 for Sat

    val dailyMinutes = remember(segments, yearMonth) {
        (1..daysInMonth).associate { d ->
            val date = yearMonth.atDay(d)
            val daySegs = StudyTimeCalculator.filterSegmentsForDay(segments, date)
            val durationMs = StudyTimeCalculator.calculateTotalDurationMs(daySegs)
            d to (durationMs / 60_000L)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = panelBg,
        border = BorderStroke(1.dp, panelBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Days of Week
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.muted
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Calendar weeks
            val totalCells = firstDayOfWeek + daysInMonth
            val totalWeeks = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (w in 0 until totalWeeks) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = w * 7 + col
                            val dayNum = cellIndex - firstDayOfWeek + 1

                            if (dayNum in 1..daysInMonth) {
                                val thisDate = yearMonth.atDay(dayNum)
                                val mins = dailyMinutes[dayNum] ?: 0L
                                val isSelected = thisDate == selectedDate

                                // Intensity level shading
                                val cellBg = when {
                                    mins >= 180 -> if (isDark) Color(0xFF22C55E) else Color(0xFF15803D)
                                    mins >= 60 -> if (isDark) Color(0xFF16A34A).copy(alpha = 0.7f) else Color(0xFF4ADE80)
                                    mins > 0 -> if (isDark) Color(0xFF14532D).copy(alpha = 0.5f) else Color(0xFFDCFCE7)
                                    else -> if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                }
                                val textColor = if (mins >= 60) Color.White else colors.foreground

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .background(cellBg, RectangleShape)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) (if (isDark) Color.White else Color(0xFF0F172A)) else panelBorder,
                                            shape = RectangleShape
                                        )
                                        .clickable { onDayClick(thisDate) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$dayNum",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = textColor
                                        )
                                        if (mins > 0) {
                                            val h = mins / 60
                                            val m = mins % 60
                                            Text(
                                                text = String.format(Locale.US, "%d:%02d", h, m),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Empty square slot
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Intensity Legend (Low-to-High small squares) & Month Total at bottom right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "LESS",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = colors.muted
                    )
                    listOf(
                        if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        if (isDark) Color(0xFF14532D).copy(alpha = 0.5f) else Color(0xFFDCFCE7),
                        if (isDark) Color(0xFF16A34A).copy(alpha = 0.7f) else Color(0xFF4ADE80),
                        if (isDark) Color(0xFF22C55E) else Color(0xFF15803D)
                    ).forEach { legColor ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(legColor, RectangleShape)
                                .border(0.5.dp, panelBorder, RectangleShape)
                        )
                    }
                    Text(
                        text = "MORE",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = colors.muted
                    )
                }

                // Month Total at Bottom Right
                Text(
                    text = "TOTAL: ${StudyTimeCalculator.formatDurationShortHoursMins(monthTotalMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentGreen,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Daily square bar totals panel for WEEK, MONTH, and TREND.
 */
@Composable
private fun DailyBarTotalsPanel(
    title: String,
    dailyData: List<Pair<String, Long>>,
    totalMs: Long,
    avgMs: Long,
    isDark: Boolean
) {
    val colors = LocalSpaghettiColors.current
    val panelBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val panelBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val barColor = if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)

    val maxVal = dailyData.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = panelBg,
        border = BorderStroke(1.dp, panelBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                    letterSpacing = 1.sp
                )

                Text(
                    text = "AVG: ${StudyTimeCalculator.formatDurationShortHoursMins(avgMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.muted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Square Bars Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                dailyData.forEach { (label, duration) ->
                    val barFraction = (duration.toFloat() / maxVal.toFloat()).coerceIn(0.04f, 1f)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((100 * barFraction).dp)
                                .background(if (duration > 0L) barColor else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)), RectangleShape)
                                .border(1.dp, panelBorder, RectangleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Labels below bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                dailyData.forEach { (label, _) ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = colors.muted,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Standard empty state box: "NO STUDY TIME RECORDED"
 */
@Composable
private fun EmptyStudyTimeBox(isDark: Boolean) {
    val colors = LocalSpaghettiColors.current
    val panelBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val panelBorder = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_study_time_box"),
        shape = RectangleShape,
        color = panelBg,
        border = BorderStroke(1.dp, panelBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8), RectangleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "NO STUDY TIME RECORDED",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.muted,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
