package com.nhyle.hyperionmaritime.data.sync

/**
 * Skip background network work only when the locally persisted study snapshot
 * still matches the last fully acknowledged cloud synchronization. An elapsed
 * cooldown alone must NEVER suppress newly answered questions, bookmarks,
 * mistakes or saved sessions.
 */
internal object LearningProgressBackgroundPolicy {
    const val REMOTE_REFRESH_INTERVAL_MS = 60 * 60 * 1000L
    const val RECENT_SUCCESS_COOLDOWN_MS = 5 * 60 * 1000L

    /**
     * Prevents redundant immediate retry/workmanager follow-ups. Dirty progress
     * always bypasses the five-minute cooldown, even if the prior sync was
     * successful seconds ago.
     */
    fun canSkipRecentSync(
        currentLocalHash: String,
        verifiedLocalHash: String?,
        lastSuccessfulSyncAt: Long,
        now: Long
    ): Boolean = matchesAcknowledgedSnapshot(
        currentLocalHash, verifiedLocalHash, lastSuccessfulSyncAt, now, RECENT_SUCCESS_COOLDOWN_MS
    )

    /**
     * Skips only redundant app-background one-shots. Hourly and explicit sync
     * must still refresh the cloud so cross-device edits and licenses converge.
     */
    fun canSkipBackgroundSync(
        currentLocalHash: String,
        verifiedLocalHash: String?,
        lastSuccessfulSyncAt: Long,
        now: Long
    ): Boolean = matchesAcknowledgedSnapshot(
        currentLocalHash, verifiedLocalHash, lastSuccessfulSyncAt, now, REMOTE_REFRESH_INTERVAL_MS
    )

    private fun matchesAcknowledgedSnapshot(
        currentLocalHash: String,
        verifiedLocalHash: String?,
        lastSuccessfulSyncAt: Long,
        now: Long,
        freshnessWindowMs: Long
    ): Boolean {
        if (currentLocalHash.isEmpty() || verifiedLocalHash.isNullOrEmpty() ||
            currentLocalHash != verifiedLocalHash) return false
        if (lastSuccessfulSyncAt <= 0L || now < lastSuccessfulSyncAt) return false
        return now - lastSuccessfulSyncAt < freshnessWindowMs
    }
}
