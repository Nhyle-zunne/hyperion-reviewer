package com.nhyle.hyperionmaritime.data.hype

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Supported reward pass types purchasable via Hype currency.
 */
enum class HypeRewardType(
    val title: String,
    val trackTag: String,
    val coversDeck: Boolean,
    val coversRadio: Boolean
) {
    DECK_OIC_NW(
        title = "DECK OIC-NW PASS",
        trackTag = "DECK TRACK",
        coversDeck = true,
        coversRadio = false
    ),
    GMDSS_GOC(
        title = "GMDSS GOC PASS",
        trackTag = "RADIO TRACK",
        coversDeck = false,
        coversRadio = true
    ),
    ALL_ACCESS(
        title = "HYPERION ALL-ACCESS PASS",
        trackTag = "ALL-ACCESS",
        coversDeck = true,
        coversRadio = true
    )
}

/**
 * Represents an active or historical temporary pass unlocked using Hype.
 */
data class HypePassRecord(
    val id: String,
    val rewardType: HypeRewardType,
    val durationDays: Int,
    val startTimeEpoch: Long,
    val expiryTimeEpoch: Long,
    val costHype: Int
) {
    val coversDeck: Boolean get() = rewardType.coversDeck
    val coversRadio: Boolean get() = rewardType.coversRadio
    val coversAll: Boolean get() = rewardType == HypeRewardType.ALL_ACCESS

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        return expiryTimeEpoch > 0L && now >= expiryTimeEpoch
    }

    val remainingTimeMs: Long
        get() = (expiryTimeEpoch - System.currentTimeMillis()).coerceAtLeast(0L)

    fun getFormattedRemainingTime(): String {
        if (isExpired()) return "Expired"
        val remainingMs = remainingTimeMs
        val totalSecs = remainingMs / 1000L
        val totalMins = totalSecs / 60L
        val totalHours = totalMins / 60L
        val days = totalHours / 24L
        val hours = totalHours % 24L
        val mins = totalMins % 60L

        return when {
            days > 0 -> "$days d ${hours}h remaining"
            totalHours > 0 -> "${totalHours}h ${mins}m remaining"
            mins > 0 -> "${mins}m remaining"
            else -> "< 1 min remaining"
        }
    }

    fun getFormattedExpiryDate(): String {
        if (expiryTimeEpoch <= 0L) return "Active"
        val date = Date(expiryTimeEpoch)
        val format = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        return "Expires " + format.format(date)
    }
}

/**
 * Definition of an available redemption option in the Study Streak store.
 */
data class HypeRedemptionOption(
    val id: String,
    val name: String,
    val rewardType: HypeRewardType,
    val durationDays: Int,
    val costHype: Int,
    val isRecommended: Boolean = false,
    val isMysteryLocked: Boolean = false
)

/**
 * Historical record of a redeemed pass for user audit.
 */
data class HypeRedemptionHistoryItem(
    val id: String,
    val rewardName: String,
    val durationDays: Int,
    val costHype: Int,
    val redeemedEpoch: Long,
    val expiryEpoch: Long
) {
    fun getFormattedRedeemedDate(): String {
        val date = Date(redeemedEpoch)
        val format = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }
}

/**
 * Result returned after performing a daily check-in.
 */
data class CheckInResult(
    val success: Boolean,
    val isSeventhDayReward: Boolean,
    val hypeAwarded: Int,
    val currentStreak: Int,
    val message: String
)

/**
 * Detailed reason why a reward pass may be blocked from redemption.
 */
sealed class RedemptionBlockReason {
    data class OverlappingLicense(
        val licenseName: String,
        val expiryTime: String? = null
    ) : RedemptionBlockReason()

    data class InsufficientHype(
        val required: Int,
        val current: Int
    ) : RedemptionBlockReason()

    object MysteryLocked : RedemptionBlockReason()
    object None : RedemptionBlockReason()

    val isBlocked: Boolean get() = this !is None

    fun getFormattedReason(): String = when (this) {
        is OverlappingLicense -> {
            if (expiryTime != null) {
                "BLOCKED: $licenseName ACTIVE ($expiryTime)"
            } else {
                "BLOCKED: $licenseName ACTIVE"
            }
        }
        is InsufficientHype -> "INSUFFICIENT HYPE (NEED $required)"
        is MysteryLocked -> "CLASSIFIED · LOCKED"
        is None -> "AVAILABLE"
    }
}
