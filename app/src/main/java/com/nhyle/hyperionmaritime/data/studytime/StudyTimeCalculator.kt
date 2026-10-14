package com.nhyle.hyperionmaritime.data.studytime

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TrackStudyStats(
    val trackId: String,
    val displayName: String,
    val color: Color,
    val totalDurationMs: Long,
    val percentage: Float
)

data class DayDetailStats(
    val totalDurationMs: Long,
    val maxFocusDurationMs: Long,
    val startTimeFormatted: String,
    val endTimeFormatted: String
)

object StudyTimeCalculator {

    const val MAX_GAP_FOR_CONTINUOUS_FOCUS_MS = 120_000L // 2 minutes

    fun filterSegmentsForDay(
        segments: List<StudyTimeSegmentEntity>,
        targetDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<StudyTimeSegmentEntity> {
        return segments.filter { seg ->
            val segDate = Instant.ofEpochMilli(seg.startEpochMs).atZone(zoneId).toLocalDate()
            segDate == targetDate
        }
    }

    fun filterSegmentsBetweenDates(
        segments: List<StudyTimeSegmentEntity>,
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<StudyTimeSegmentEntity> {
        return segments.filter { seg ->
            val segDate = Instant.ofEpochMilli(seg.startEpochMs).atZone(zoneId).toLocalDate()
            !segDate.isBefore(startDate) && !segDate.isAfter(endDate)
        }
    }

    fun calculateTotalDurationMs(segments: List<StudyTimeSegmentEntity>): Long {
        return segments.sumOf { it.durationMs }
    }

    /**
     * Max Focus Time: the longest continuous run of segments with gaps under 2 minutes.
     */
    fun calculateMaxFocusTimeMs(segments: List<StudyTimeSegmentEntity>): Long {
        if (segments.isEmpty()) return 0L
        val sorted = segments.sortedBy { it.startEpochMs }

        var maxRun = 0L
        var currentRun = sorted[0].durationMs

        for (i in 0 until sorted.size - 1) {
            val current = sorted[i]
            val next = sorted[i + 1]
            val gap = next.startEpochMs - current.endEpochMs

            if (gap in 0..MAX_GAP_FOR_CONTINUOUS_FOCUS_MS) {
                currentRun += next.durationMs
            } else {
                if (currentRun > maxRun) maxRun = currentRun
                currentRun = next.durationMs
            }
        }
        if (currentRun > maxRun) maxRun = currentRun
        return maxRun
    }

    /**
     * Computes DayDetailStats: Total, Max Focus, Start Time, and End Time (with "+1" if past midnight).
     */
    fun calculateDayDetailStats(
        segments: List<StudyTimeSegmentEntity>,
        targetDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DayDetailStats {
        val daySegments = filterSegmentsForDay(segments, targetDate, zoneId)
        if (daySegments.isEmpty()) {
            return DayDetailStats(
                totalDurationMs = 0L,
                maxFocusDurationMs = 0L,
                startTimeFormatted = "—",
                endTimeFormatted = "—"
            )
        }

        val sorted = daySegments.sortedBy { it.startEpochMs }
        val firstStart = sorted.first().startEpochMs
        val lastEnd = sorted.maxOf { it.endEpochMs }

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        val startZdt = Instant.ofEpochMilli(firstStart).atZone(zoneId)
        val endZdt = Instant.ofEpochMilli(lastEnd).atZone(zoneId)

        val startTimeStr = startZdt.format(timeFormatter)
        val isPastMidnight = endZdt.toLocalDate().isAfter(targetDate)
        val endTimeStr = if (isPastMidnight) {
            "${endZdt.format(timeFormatter)} (+1)"
        } else {
            endZdt.format(timeFormatter)
        }

        return DayDetailStats(
            totalDurationMs = calculateTotalDurationMs(daySegments),
            maxFocusDurationMs = calculateMaxFocusTimeMs(daySegments),
            startTimeFormatted = startTimeStr,
            endTimeFormatted = endTimeStr
        )
    }

    /**
     * Hourly study minutes for the 24 hours of targetDate (indices 0..23).
     */
    fun calculateHourlyMinutes(
        segments: List<StudyTimeSegmentEntity>,
        targetDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): IntArray {
        val hourlySecs = IntArray(24) { 0 }
        val daySegments = filterSegmentsForDay(segments, targetDate, zoneId)

        for (seg in daySegments) {
            val startZdt = Instant.ofEpochMilli(seg.startEpochMs).atZone(zoneId)
            val endZdt = Instant.ofEpochMilli(seg.endEpochMs).atZone(zoneId)

            val startHour = startZdt.hour
            val endHour = endZdt.hour

            if (startHour == endHour) {
                hourlySecs[startHour] += (seg.durationMs / 1000L).toInt()
            } else {
                // Distribute across hours
                val secInFirstHour = 3600 - (startZdt.minute * 60 + startZdt.second)
                hourlySecs[startHour] += secInFirstHour
                for (h in (startHour + 1) until endHour.coerceAtMost(23)) {
                    hourlySecs[h] += 3600
                }
                if (endHour in 0..23) {
                    val secInLastHour = endZdt.minute * 60 + endZdt.second
                    hourlySecs[endHour] += secInLastHour
                }
            }
        }

        return IntArray(24) { idx -> (hourlySecs[idx] / 60).coerceIn(0, 60) }
    }

    /**
     * Breakdown by track for the given list of segments.
     * Excludes tracks with 0 study time. Never includes an "Other" category.
     */
    fun calculateTrackBreakdown(segments: List<StudyTimeSegmentEntity>): List<TrackStudyStats> {
        val totalMs = calculateTotalDurationMs(segments)
        if (totalMs <= 0L) return emptyList()

        val grouped = segments.groupBy { it.trackId }
        return grouped.map { (trackId, trackSegs) ->
            val trackTotal = trackSegs.sumOf { it.durationMs }
            val fraction = (trackTotal.toDouble() / totalMs.toDouble()).toFloat()
            val entry = TrackRegistry.getEntry(trackId)
            TrackStudyStats(
                trackId = trackId,
                displayName = entry.displayName,
                color = entry.accentColor,
                totalDurationMs = trackTotal,
                percentage = fraction * 100f
            )
        }.sortedByDescending { it.totalDurationMs }
    }

    /**
     * Cumulative study minutes across the 24 hours of the day (returns points (hour, cumulativeMinutes)).
     */
    fun calculateCumulativeMinutes(
        segments: List<StudyTimeSegmentEntity>,
        targetDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<Pair<Float, Float>> {
        val hourly = calculateHourlyMinutes(segments, targetDate, zoneId)
        val points = mutableListOf<Pair<Float, Float>>()
        points.add(0f to 0f)

        var runningTotalMin = 0f
        for (h in 0 until 24) {
            runningTotalMin += hourly[h].toFloat()
            points.add((h + 1).toFloat() to runningTotalMin)
        }
        return points
    }

    fun formatDurationHMM(millis: Long): String {
        val totalMin = (millis / 60_000L).coerceAtLeast(0L)
        val h = totalMin / 60
        val m = totalMin % 60
        return String.format(Locale.US, "%d:%02d", h, m)
    }

    fun formatDurationFull(millis: Long): String {
        val totalSecs = (millis / 1000L).coerceAtLeast(0L)
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    fun formatDurationShortHoursMins(millis: Long): String {
        val totalMin = (millis / 60_000L).coerceAtLeast(0L)
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
