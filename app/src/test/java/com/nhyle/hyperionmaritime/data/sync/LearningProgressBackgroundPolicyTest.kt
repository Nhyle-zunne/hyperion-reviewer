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
}
