package com.nhyle.hyperionmaritime.data.sync

import org.junit.Assert.*
import org.junit.Test

class LearningProgressBackgroundPolicyTest {
    private val now = 1_800_000_000_000L
    private val hour = LearningProgressBackgroundPolicy.REMOTE_REFRESH_INTERVAL_MS

    @Test fun unchangedRecentlySynchronizedProgressSkipsOnlyTheOneShot() {
        assertTrue(LearningProgressBackgroundPolicy.canSkipBackgroundSync(
            "same", "same", now - 6 * 60_000L, now
        ))
        assertFalse(LearningProgressBackgroundPolicy.canSkipBackgroundSync(
            "new-answer", "same", now - 6 * 60_000L, now
        ))
        assertFalse(LearningProgressBackgroundPolicy.canSkipBackgroundSync(
            "same", null, now - 6 * 60_000L, now
        ))
    }

    @Test fun oldOrUntrustedTimestampsCannotSuppressCloudRefresh() {
        assertFalse(LearningProgressBackgroundPolicy.canSkipBackgroundSync("same", "same", now - hour, now))
        assertFalse(LearningProgressBackgroundPolicy.canSkipBackgroundSync("same", "same", 0, now))
        assertFalse(LearningProgressBackgroundPolicy.canSkipBackgroundSync("same", "same", now + 1, now))
    }
    @Test fun recentFiveMinuteCooldownOnlySkipsFullyAcknowledgedUnchangedStudyData() {
        val recent = now - 60_000L
        assertTrue(LearningProgressBackgroundPolicy.canSkipRecentSync(
            "verified-snapshot", "verified-snapshot", recent, now
        ))
        // A new answer, bookmark, session or deletion changes the local hash:
        // the pending data must sync even though the last upload was recent.
        assertFalse(LearningProgressBackgroundPolicy.canSkipRecentSync(
            "changed-study-session", "verified-snapshot", recent, now
        ))
        assertFalse(LearningProgressBackgroundPolicy.canSkipRecentSync(
            "new-bookmark", "verified-snapshot", now - 100L, now
        ))
        assertFalse(LearningProgressBackgroundPolicy.canSkipRecentSync(
            "verified-snapshot", null, recent, now
        ))
        assertFalse(LearningProgressBackgroundPolicy.canSkipRecentSync(
            "", "", recent, now
        ))
    }

    @Test fun fiveMinuteBoundaryAndClockRollbackAlwaysAllowRefresh() {
        val fiveMinutes = LearningProgressBackgroundPolicy.RECENT_SUCCESS_COOLDOWN_MS
        assertFalse(LearningProgressBackgroundPolicy.canSkipRecentSync(
            "same", "same", now - fiveMinutes, now
        ))
        assertFalse(LearningProgressBackgroundPolicy.canSkipRecentSync(
            "same", "same", now - fiveMinutes - 1L, now
        ))
        assertFalse(LearningProgressBackgroundPolicy.canSkipRecentSync(
            "same", "same", now + 1L, now
        ))
        assertFalse(LearningProgressBackgroundPolicy.canSkipRecentSync(
            "same", "same", 0L, now
        ))
    }

    @Test fun unchangedStudyStillRefreshesCloudWithinHourlyLimit() {
        // The short cooldown expires after five minutes; a one-shot can still
        // suppress its own redundant request until the hourly refresh threshold.
        assertFalse(LearningProgressBackgroundPolicy.canSkipRecentSync(
            "same", "same", now - 6 * 60_000L, now
        ))
        assertTrue(LearningProgressBackgroundPolicy.canSkipBackgroundSync(
            "same", "same", now - 6 * 60_000L, now
        ))
        assertFalse(LearningProgressBackgroundPolicy.canSkipBackgroundSync(
            "same", "same", now - LearningProgressBackgroundPolicy.REMOTE_REFRESH_INTERVAL_MS, now
        ))
    }

}
