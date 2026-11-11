package com.nhyle.hyperionmaritime.data.sync

/**
 * Skips only redundant app-background one-shots. Hourly and explicit sync
 * must still refresh the cloud so other-device edits and entitlements converge.
 */
internal object LearningProgressBackgroundPolicy {
    const val REMOTE_REFRESH_INTERVAL_MS = 60 * 60 * 1000L

    fun canSkipBackgroundSync(
        currentLocalHash: String,
        verifiedLocalHash: String?,
        lastSuccessfulSyncAt: Long,
        now: Long
    ): Boolean {
        if (verifiedLocalHash.isNullOrEmpty() || currentLocalHash != verifiedLocalHash) return false
        if (lastSuccessfulSyncAt <= 0L || now < lastSuccessfulSyncAt) return false
        return now - lastSuccessfulSyncAt < REMOTE_REFRESH_INTERVAL_MS
    }
}
