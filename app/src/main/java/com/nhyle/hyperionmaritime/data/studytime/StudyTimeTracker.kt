package com.nhyle.hyperionmaritime.data.studytime

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * Manages study-time tracking for active sessions:
 * - High-precision SystemClock.elapsedRealtime() measurement (immune to wall-clock shifts)
 * - 1-second display ticks that isolate recompositions to the timer box
 * - 3-minute idle timeout with retroactive pause to last interaction
 * - Automatic segment generation on pause/resume with local midnight splitting
 * - 10-second periodic persistence of elapsed session time
 */
class StudyTimeTracker(
    private val scope: CoroutineScope,
    private val saveSegmentCallback: suspend (List<StudyTimeSegmentEntity>) -> Unit,
    private val saveElapsedProgressCallback: suspend (sessionId: String, elapsedRealtimeMs: Long) -> Unit
) {

    companion object {
        const val TAG = "StudyTimeTracker"
        const val IDLE_TIMEOUT_MINUTES = 3
        const val IDLE_TIMEOUT_MS = IDLE_TIMEOUT_MINUTES * 60 * 1000L
        const val MAX_VALID_SEGMENT_MS = 6L * 3600 * 1000 // 6 hours
        const val AUTO_SAVE_INTERVAL_MS = 10_000L // 10 seconds
    }

    // Active session configuration
    private var currentSessionId: String = ""
    private var currentTrackId: String = ""
    private var currentCompetence: String? = null
    private var currentPart: String? = null
    private var currentMode: String = "DRILL" // EXAM or DRILL
    private var currentDrillFormat: String? = null // STANDARD, INTENSIVE, or null

    // Timing state
    private var totalAccumulatedMs: Long = 0L // Accumulated from previous segments
    private var segmentStartRealtime: Long = 0L // SystemClock.elapsedRealtime() when segment began
    private var segmentStartUtc: Long = 0L // System.currentTimeMillis() when segment began
    private var lastInteractionRealtime: Long = 0L
    private var isRunning: Boolean = false
    private var isAppInBackground: Boolean = false

    // Ticking and auto-save jobs
    private var tickJob: Job? = null
    private var autoSaveJob: Job? = null

    // Exposed StateFlows for UI
    private val _elapsedTimeString = MutableStateFlow("00:00:00")
    val elapsedTimeString: StateFlow<String> = _elapsedTimeString.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _showIdleDialog = MutableStateFlow(false)
    val showIdleDialog: StateFlow<Boolean> = _showIdleDialog.asStateFlow()

    /**
     * Starts or resumes study time tracking for a session.
     */
    fun startSession(
        sessionId: String,
        trackId: String,
        competence: String?,
        part: String?,
        mode: String,
        drillFormat: String?,
        initialAccumulatedMs: Long = 0L
    ) {
        // If already tracking the same session and running, just record interaction
        if (currentSessionId == sessionId && isRunning) {
            recordUserInteraction()
            return
        }

        // Close any active segment first if switching sessions
        if (isRunning) {
            pauseTimerInternal(recordSegment = true, pauseTimeRealtime = SystemClock.elapsedRealtime())
        }

        val previousAccumulated = if (currentSessionId == sessionId) totalAccumulatedMs else 0L
        currentSessionId = sessionId
        currentTrackId = trackId
        currentCompetence = competence
        currentPart = if (trackId.equals("GMDSS_GOC", ignoreCase = true)) part else null
        currentMode = mode
        currentDrillFormat = drillFormat
        totalAccumulatedMs = maxOf(previousAccumulated, initialAccumulatedMs.coerceAtLeast(0L))

        val nowRealtime = SystemClock.elapsedRealtime()
        lastInteractionRealtime = nowRealtime
        _showIdleDialog.value = false
        _isPaused.value = false

        resumeTimer()
    }

    /**
     * Resumes the timer and starts a new study segment stretch.
     */
    fun resumeTimer() {
        if (isRunning || currentSessionId.isBlank()) return

        val nowRealtime = SystemClock.elapsedRealtime()
        val nowUtc = System.currentTimeMillis()

        segmentStartRealtime = nowRealtime
        segmentStartUtc = nowUtc
        lastInteractionRealtime = nowRealtime
        isRunning = true
        _isPaused.value = false
        _showIdleDialog.value = false

        updateDisplayTime()
        startTickJob()
        startAutoSaveJob()
    }

    /**
     * Explicit pause (e.g. Save & Exit, pause button).
     */
    fun pauseTimer() {
        if (!isRunning) return
        val nowRealtime = SystemClock.elapsedRealtime()
        pauseTimerInternal(recordSegment = true, pauseTimeRealtime = nowRealtime)
    }

    /**
     * App lifecycle onStop notification.
     */
    fun onAppBackground() {
        isAppInBackground = true
        if (isRunning) {
            val nowRealtime = SystemClock.elapsedRealtime()
            pauseTimerInternal(recordSegment = true, pauseTimeRealtime = nowRealtime)
        }
    }

    /**
     * App lifecycle onStart / onResume notification.
     */
    fun onAppForeground() {
        isAppInBackground = false
        if (!isRunning && currentSessionId.isNotBlank() && !_showIdleDialog.value) {
            resumeTimer()
        }
    }

    /**
     * Notifies tracker of user interaction (touch, tap, scroll).
     */
    fun recordUserInteraction() {
        lastInteractionRealtime = SystemClock.elapsedRealtime()
    }

    /**
     * User responded "I'M STILL HERE" to the idle dialog.
     */
    fun onUserConfirmedStillHere() {
        _showIdleDialog.value = false
        recordUserInteraction()
        resumeTimer()
    }

    /**
     * Internal pause routine with optional retroactive timestamp and segment recording.
     */
    private fun pauseTimerInternal(recordSegment: Boolean, pauseTimeRealtime: Long) {
        if (!isRunning) return
        isRunning = false
        _isPaused.value = true

        tickJob?.cancel()
        autoSaveJob?.cancel()

        val clampedPauseTime = pauseTimeRealtime.coerceAtLeast(segmentStartRealtime)
        val segmentDurationMs = clampedPauseTime - segmentStartRealtime
        totalAccumulatedMs += segmentDurationMs

        updateDisplayTime()

        if (recordSegment && segmentDurationMs > 0L) {
            val endUtc = segmentStartUtc + segmentDurationMs
            createAndSaveSegments(
                startUtc = segmentStartUtc,
                endUtc = endUtc,
                durationMs = segmentDurationMs
            )
        }

        // Persist progress to local storage on pause
        val sid = currentSessionId
        val totalMs = totalAccumulatedMs
        scope.launch(Dispatchers.IO) {
            try {
                saveElapsedProgressCallback(sid, totalMs)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist elapsed study progress on pause", e)
            }
        }
    }

    /**
     * Splits segments across local timezone midnight if applicable, validates duration, and saves.
     */
    private fun createAndSaveSegments(startUtc: Long, endUtc: Long, durationMs: Long) {
        if (durationMs <= 0 || durationMs > MAX_VALID_SEGMENT_MS) {
            Log.w(TAG, "Discarded invalid study segment with duration: $durationMs ms")
            return
        }

        val zoneId = ZoneId.systemDefault()
        val startDate = Instant.ofEpochMilli(startUtc).atZone(zoneId).toLocalDate()
        val endDate = Instant.ofEpochMilli(endUtc).atZone(zoneId).toLocalDate()

        val segments = mutableListOf<StudyTimeSegmentEntity>()

        if (startDate == endDate) {
            segments.add(
                StudyTimeSegmentEntity(
                    segmentId = UUID.randomUUID().toString(),
                    sessionId = currentSessionId,
                    trackId = currentTrackId,
                    competence = currentCompetence,
                    part = currentPart,
                    mode = currentMode,
                    drillFormat = currentDrillFormat,
                    startEpochMs = startUtc,
                    endEpochMs = endUtc,
                    durationMs = durationMs
                )
            )
        } else {
            // Crossed midnight: split at local midnight
            val midnightZdt = startDate.plusDays(1).atStartOfDay(zoneId)
            val midnightEpochUtc = midnightZdt.toInstant().toEpochMilli()

            val seg1Duration = (midnightEpochUtc - startUtc).coerceAtLeast(0L)
            val seg2Duration = (endUtc - midnightEpochUtc).coerceAtLeast(0L)

            if (seg1Duration in 1..MAX_VALID_SEGMENT_MS) {
                segments.add(
                    StudyTimeSegmentEntity(
                        segmentId = UUID.randomUUID().toString(),
                        sessionId = currentSessionId,
                        trackId = currentTrackId,
                        competence = currentCompetence,
                        part = currentPart,
                        mode = currentMode,
                        drillFormat = currentDrillFormat,
                        startEpochMs = startUtc,
                        endEpochMs = midnightEpochUtc,
                        durationMs = seg1Duration
                    )
                )
            }
            if (seg2Duration in 1..MAX_VALID_SEGMENT_MS) {
                segments.add(
                    StudyTimeSegmentEntity(
                        segmentId = UUID.randomUUID().toString(),
                        sessionId = currentSessionId,
                        trackId = currentTrackId,
                        competence = currentCompetence,
                        part = currentPart,
                        mode = currentMode,
                        drillFormat = currentDrillFormat,
                        startEpochMs = midnightEpochUtc,
                        endEpochMs = endUtc,
                        durationMs = seg2Duration
                    )
                )
            }
        }

        if (segments.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                try {
                    saveSegmentCallback(segments)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save study time segments", e)
                }
            }
        }
    }

    /**
     * 1-second UI tick job: checks idle timeout and updates elapsed time string.
     */
    private fun startTickJob() {
        tickJob?.cancel()
        tickJob = scope.launch(Dispatchers.Main) {
            while (isActive && isRunning) {
                val nowRealtime = SystemClock.elapsedRealtime()

                // Check 3-minute idle timeout
                val idleDelta = nowRealtime - lastInteractionRealtime
                if (idleDelta >= IDLE_TIMEOUT_MS) {
                    // Pause retroactively to the last interaction time
                    pauseTimerInternal(recordSegment = true, pauseTimeRealtime = lastInteractionRealtime)
                    if (!isAppInBackground) {
                        _showIdleDialog.value = true
                    }
                    break
                }

                updateDisplayTime()
                delay(1000L)
            }
        }
    }

    /**
     * 10-second background job to persist elapsed running time so app kill loses at most 10s.
     */
    private fun startAutoSaveJob() {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch(Dispatchers.IO) {
            while (isActive && isRunning) {
                delay(AUTO_SAVE_INTERVAL_MS)
                if (isRunning && currentSessionId.isNotBlank()) {
                    val currentTotal = getCurrentTotalElapsedMs()
                    try {
                        saveElapsedProgressCallback(currentSessionId, currentTotal)
                    } catch (e: Exception) {
                        Log.e(TAG, "Auto-save elapsed progress error", e)
                    }
                }
            }
        }
    }

    private fun updateDisplayTime() {
        val totalMs = getCurrentTotalElapsedMs()
        _elapsedTimeString.value = formatDuration(totalMs)
    }

    fun getCurrentTotalElapsedMs(): Long {
        return if (isRunning) {
            val delta = SystemClock.elapsedRealtime() - segmentStartRealtime
            totalAccumulatedMs + delta.coerceAtLeast(0L)
        } else {
            totalAccumulatedMs
        }
    }

    private fun formatDuration(millis: Long): String {
        val totalSecs = (millis / 1000L).coerceAtLeast(0L)
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    }
}
