package com.nhyle.hyperionmaritime.data.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.nhyle.hyperionmaritime.BuildConfig
import com.nhyle.hyperionmaritime.data.auth.SupabaseAccountManager
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset

/**
 * Creates the Google Play obfuscated account identifier for the signed-in Hyperion user.
 * Google Play limits this value to 64 characters and recommends a one-way hash rather
 * than personally identifiable information. Older purchases remain linkable without it.
 */
internal fun obfuscatedHyperionAccountId(userId: String?): String? {
    val normalizedUserId = userId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(("hyperion-maritime:" + normalizedUserId).toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}

/**
 * Subscription billing cycles supported by Hyperion Maritime Exam Reviewer.
 */
enum class BillingCycle(val label: String, val shortLabel: String, val durationDays: Int) {
    WEEKLY("Weekly", "wk", 7),
    MONTHLY("Monthly", "mo", 30),
    YEARLY("Yearly (Save ~48%)", "yr", 365);

    /**
     * Adds one real billing period in UTC. Monthly and yearly Play plans are calendar
     * periods (P1M/P1Y), not fixed 30/365-day durations.
     */
    fun nextBillingPeriod(startEpochMs: Long): Long {
        val start = Instant.ofEpochMilli(startEpochMs).atZone(ZoneOffset.UTC)
        val next = when (this) {
            WEEKLY -> start.plusWeeks(1)
            MONTHLY -> start.plusMonths(1)
            YEARLY -> start.plusYears(1)
        }
        return next.toInstant().toEpochMilli()
    }

    // Nominal millisecond duration retained only for legacy callers/metadata.
    // Do not use this for subscription expiry calculations; use nextBillingPeriod().
    val durationMs: Long get() = durationDays.toLong() * 24L * 60L * 60L * 1000L
}

/**
 * Matches the recurring billing period of a Play subscription offer. When an offer
 * contains a trial or introductory phase, the final pricing phase is the base plan's
 * recurring period, so it takes precedence over the offer's base-plan display name.
 */
/**
 * Preserve a cycle explicitly saved when the checkout was launched. The monthly
 * fallback exists only for very old purchases with no recorded cycle metadata.
 */
internal fun resolveRequestedBillingCycle(requestedCycle: BillingCycle?): BillingCycle =
    requestedCycle ?: BillingCycle.MONTHLY

/**
 * A Store selection is only a pending intention until Google Play returns a newer
 * purchased subscription. Opening checkout and cancelling must not rewrite the
 * duration metadata of an already-owned subscription.
 */
internal fun isNewPlaySubscriptionPurchase(
    previousPurchaseToken: String?,
    currentPurchaseToken: String,
    previousPurchaseTimeEpochMs: Long,
    currentPurchaseTimeEpochMs: Long
): Boolean {
    val tokenWasReplaced = !previousPurchaseToken.isNullOrBlank() &&
        currentPurchaseToken.isNotBlank() &&
        previousPurchaseToken != currentPurchaseToken
    val purchaseTimeAdvanced = currentPurchaseTimeEpochMs > 0L &&
        currentPurchaseTimeEpochMs > previousPurchaseTimeEpochMs
    return tokenWasReplaced || purchaseTimeAdvanced
}

internal fun resolvePurchasedBillingCycle(
    activeCycle: BillingCycle?,
    pendingCycle: BillingCycle?,
    isNewPurchase: Boolean
): BillingCycle =
    if (pendingCycle != null && isNewPurchase) pendingCycle
    else activeCycle ?: BillingCycle.MONTHLY

internal fun BillingCycle.matchesPlayOffer(basePlanId: String, billingPeriods: List<String>): Boolean {
    val recurringPeriod = billingPeriods.lastOrNull()?.uppercase().orEmpty()
    val recognizedPeriod = recurringPeriod in setOf("P1W", "P7D", "P1M", "P30D", "P1Y", "P365D", "P12M")
    if (recognizedPeriod) {
        return when (this) {
            BillingCycle.WEEKLY -> recurringPeriod == "P1W" || recurringPeriod == "P7D"
            BillingCycle.MONTHLY -> recurringPeriod == "P1M" || recurringPeriod == "P30D"
            BillingCycle.YEARLY -> recurringPeriod == "P1Y" || recurringPeriod == "P365D" || recurringPeriod == "P12M"
        }
    }

    val base = basePlanId.lowercase()
    return when (this) {
        BillingCycle.WEEKLY -> base.contains("weekly")
        BillingCycle.MONTHLY -> base.contains("monthly")
        BillingCycle.YEARLY -> base.contains("yearly") || base.contains("annual")
    }
}

/**
 * Local fallback only: BillingClient exposes subscription signup time, not its live
 * renewal expiry. The server-verified SubscriptionsV2 expiry must override this estimate
 * whenever the user has linked the purchase to a Hyperion account.
 */
internal fun estimateNextSubscriptionExpiry(
    purchaseTimeEpochMs: Long,
    nowEpochMs: Long,
    cycle: BillingCycle
): Long {
    if (purchaseTimeEpochMs <= 0L) return 0L
    var expiry = cycle.nextBillingPeriod(purchaseTimeEpochMs)
    var periodsAdvanced = 0
    while (expiry <= nowEpochMs && periodsAdvanced < 10_000) {
        // Play permanently adjusts monthly renewal dates after a short-month
        // rollover (Jan 31 -> Feb 28 -> Mar 28), so advance from the last expiry.
        val next = cycle.nextBillingPeriod(expiry)
        if (next <= expiry) return expiry
        expiry = next
        periodsAdvanced++
    }
    return expiry
}

/** Select exact Google Play server expiry when available, otherwise a local estimate. */
internal fun subscriptionDisplayExpiry(
    serverVerifiedExpiryEpochMs: Long,
    estimatedLocalExpiryEpochMs: Long
): Long = serverVerifiedExpiryEpochMs.takeIf { it > 0L } ?: estimatedLocalExpiryEpochMs

/**
 * Consistent countdown formatter for dashboard and Store. Uses the actual supplied expiry,
 * not the 72-hour offline ownership-verification lease.
 */
fun formatSubscriptionTimeRemaining(
    expiryEpochMs: Long,
    nowEpochMs: Long = System.currentTimeMillis()
): String {
    if (expiryEpochMs <= 0L) return "Expiry unavailable"
    val remainingMs = expiryEpochMs - nowEpochMs
    if (remainingMs <= 0L) return "Subscription Expired"
    if (remainingMs < 60_000L) return "< 1 min remaining"

    val totalMinutes = remainingMs / 60_000L
    val totalHours = totalMinutes / 60L
    val minutes = totalMinutes % 60L

    // Count a positive remainder of a full day as that remaining day. A seven-day
    // purchase therefore stays at 7 days until it has less than six full days left.
    // Switch to hours/minutes only during the final 24 hours.
    if (remainingMs >= 24L * 60L * 60L * 1000L) {
        val dayMs = 24L * 60L * 60L * 1000L
        val days = (remainingMs + dayMs - 1L) / dayMs
        val dayUnit = if (days == 1L) "day" else "days"
        return "$days $dayUnit remaining"
    }

    val hourUnit = if (totalHours == 1L) "hr" else "hrs"
    val minuteUnit = if (minutes == 1L) "min" else "mins"
    return when {
        totalHours > 0L && minutes > 0L -> "$totalHours $hourUnit, $minutes $minuteUnit remaining"
        totalHours > 0L -> "$totalHours $hourUnit remaining"
        totalMinutes > 0L -> "$totalMinutes ${if (totalMinutes == 1L) "min" else "mins"} remaining"
        else -> "< 1 min remaining"
    }
}

/**
 * Active subscription plan metadata identifying the specific cycle, purchase timestamp, expiry timestamp, and remaining validity.
 */
data class ActivePlanInfo(
    val cycle: BillingCycle = BillingCycle.YEARLY,
    val isTerminalLicense: Boolean = false,
    val purchaseToken: String? = null,
    val purchaseTimestamp: Long = System.currentTimeMillis(),
    val expiryTimestamp: Long = System.currentTimeMillis() + (365L * 24L * 60L * 60L * 1000L),
    val highWaterMarkEpoch: Long = 0L,
    val lastVerifiedAtEpoch: Long = purchaseTimestamp
) {
    val effectiveNow: Long
        get() = maxOf(System.currentTimeMillis(), highWaterMarkEpoch)

    /** A local Play cache must be refreshed online within the bounded at-sea grace period. */
    val requiresOnlineVerification: Boolean
        get() {
            if (isTerminalLicense) return false
            val now = System.currentTimeMillis()
            val clockRolledBack = highWaterMarkEpoch > 0L &&
                now < highWaterMarkEpoch - BillingConstants.CLOCK_ROLLBACK_TOLERANCE_MS
            val verificationMissing = lastVerifiedAtEpoch <= 0L
            val verificationStale = !verificationMissing &&
                effectiveNow - lastVerifiedAtEpoch > BillingConstants.OFFLINE_VERIFICATION_GRACE_MS
            return clockRolledBack || verificationMissing || verificationStale
        }

    /** Cached access is rejected after either the server period estimate or offline lease ends. */
    val isExpired: Boolean
        get() = !isTerminalLicense && (
            requiresOnlineVerification ||
                expiryTimestamp <= 0L ||
                effectiveNow >= expiryTimestamp
            )

    /**
     * Actual subscription period remaining, used for user-facing expiry displays.
     * This is deliberately independent from the shorter offline verification lease.
     */
    val subscriptionRemainingTimeMs: Long
        get() {
            if (isTerminalLicense) return Long.MAX_VALUE
            if (expiryTimestamp <= 0L) return 0L
            return (expiryTimestamp - effectiveNow).coerceAtLeast(0L)
        }

    /**
     * Remaining time in the locally cached ownership-verification lease.
     * This is a security boundary for offline access, not the subscription's expiry.
     */
    val remainingTimeMs: Long
        get() {
            if (isTerminalLicense) return Long.MAX_VALUE
            if (requiresOnlineVerification) return 0L
            val leaseRemaining = (lastVerifiedAtEpoch + BillingConstants.OFFLINE_VERIFICATION_GRACE_MS - effectiveNow)
                .coerceAtLeast(0L)
            return minOf(subscriptionRemainingTimeMs, leaseRemaining)
        }

    fun getFormattedTimeRemaining(): String {
        if (isTerminalLicense) return "Active Plan"
        if (requiresOnlineVerification) return "Reconnect to verify subscription"
        if (isExpired) return "Subscription Expired"
        return formatSubscriptionTimeRemaining(expiryTimestamp, effectiveNow)
    }

    fun getFormattedExpiryDate(): String {
        if (isTerminalLicense) return "Active"
        if (expiryTimestamp <= 0L) return "Active"
        val date = java.util.Date(expiryTimestamp)
        val format = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
        return "Expires " + format.format(date)
    }

    fun getFullExpiryBadge(): String {
        if (isTerminalLicense) return "ACTIVE PLAN"
        if (requiresOnlineVerification) return "VERIFY ONLINE"
        if (isExpired) return "EXPIRED"
        return "${getFormattedTimeRemaining().uppercase()} • ${getFormattedExpiryDate().uppercase()}"
    }
}

/**
 * Standard Product IDs, Base Plans, and fallback metadata for Hyperion Google Play Subscriptions.
 * Converted to ProductType.SUBS (Auto-Renewing Subscriptions across Weekly, Monthly, Yearly cycles).
 */
internal fun canRefreshPlayVerification(
    hasValidatedInternet: Boolean,
    nowEpochMs: Long,
    savedHighWaterMarkEpochMs: Long
): Boolean {
    if (!hasValidatedInternet) return false
    return savedHighWaterMarkEpochMs <= 0L ||
        nowEpochMs >= savedHighWaterMarkEpochMs - BillingConstants.CLOCK_ROLLBACK_TOLERANCE_MS
}

/**
 * An offline Play Billing snapshot is not authoritative for revocation. It may be empty
 * because the Play Store cannot refresh ownership while disconnected, so do not replace
 * or clear the last successfully verified local subscription cache from that snapshot.
 */
internal fun shouldApplyPlayPurchaseSnapshot(hasValidatedInternet: Boolean): Boolean =
    hasValidatedInternet

object BillingConstants {
    // Match the account-linked license cache: allow up to 72 hours at sea after
    // the last successful Google Play ownership query, then require online verification.
    const val OFFLINE_VERIFICATION_GRACE_MS = 72L * 60L * 60L * 1000L
    const val CLOCK_ROLLBACK_TOLERANCE_MS = 5L * 60L * 1000L

    const val PRODUCT_OIC_NW_PASS = "hyperion_oic_nw_pass"
    const val PRODUCT_GMDSS_PASS = "hyperion_gmdss_pass"
    const val PRODUCT_ALL_ACCESS_PASS = "hyperion_all_access_pass"
    const val PRODUCT_ENGINE_OIC_EW_PASS = "hyperion_engine_oic_ew_pass"
    const val PRODUCT_DECK_MGT_PASS = "hyperion_deck_mgt_pass"
    const val PRODUCT_ENGINE_MGT_PASS = "hyperion_engine_mgt_pass"

    val ALL_PRODUCT_IDS = listOf(
        PRODUCT_OIC_NW_PASS,
        PRODUCT_GMDSS_PASS,
        PRODUCT_ALL_ACCESS_PASS,
        PRODUCT_ENGINE_OIC_EW_PASS,
        PRODUCT_DECK_MGT_PASS,
        PRODUCT_ENGINE_MGT_PASS
    )

    // Standard Fallback Display Prices across billing cycles
    val DEFAULT_PRICES_YEARLY = mapOf(
        PRODUCT_OIC_NW_PASS to "₱350 / year",
        PRODUCT_GMDSS_PASS to "₱250 / year",
        PRODUCT_ALL_ACCESS_PASS to "₱500 / year",
        PRODUCT_ENGINE_OIC_EW_PASS to "₱350 / year",
        PRODUCT_DECK_MGT_PASS to "₱700 / year",
        PRODUCT_ENGINE_MGT_PASS to "₱700 / year"
    )

    val DEFAULT_PRICES_MONTHLY = mapOf(
        PRODUCT_OIC_NW_PASS to "₱50 / month",
        PRODUCT_GMDSS_PASS to "₱40 / month",
        PRODUCT_ALL_ACCESS_PASS to "₱80 / month",
        PRODUCT_ENGINE_OIC_EW_PASS to "₱50 / month",
        PRODUCT_DECK_MGT_PASS to "₱100 / month",
        PRODUCT_ENGINE_MGT_PASS to "₱100 / month"
    )

    val DEFAULT_PRICES_WEEKLY = mapOf(
        PRODUCT_OIC_NW_PASS to "₱20 / week",
        PRODUCT_GMDSS_PASS to "₱15 / week",
        PRODUCT_ALL_ACCESS_PASS to "₱35 / week",
        PRODUCT_ENGINE_OIC_EW_PASS to "₱20 / week",
        PRODUCT_DECK_MGT_PASS to "₱40 / week",
        PRODUCT_ENGINE_MGT_PASS to "₱40 / week"
    )

    // Backward-compatible default price map (Yearly)
    val DEFAULT_PRICES = DEFAULT_PRICES_YEARLY

    val DEFAULT_SHORT_PRICES_YEARLY = mapOf(
        PRODUCT_OIC_NW_PASS to "₱350/yr",
        PRODUCT_GMDSS_PASS to "₱250/yr",
        PRODUCT_ALL_ACCESS_PASS to "₱500/yr",
        PRODUCT_ENGINE_OIC_EW_PASS to "₱350/yr",
        PRODUCT_DECK_MGT_PASS to "₱700/yr",
        PRODUCT_ENGINE_MGT_PASS to "₱700/yr"
    )

    val DEFAULT_SHORT_PRICES_MONTHLY = mapOf(
        PRODUCT_OIC_NW_PASS to "₱50/mo",
        PRODUCT_GMDSS_PASS to "₱40/mo",
        PRODUCT_ALL_ACCESS_PASS to "₱80/mo",
        PRODUCT_ENGINE_OIC_EW_PASS to "₱50/mo",
        PRODUCT_DECK_MGT_PASS to "₱100/mo",
        PRODUCT_ENGINE_MGT_PASS to "₱100/mo"
    )

    val DEFAULT_SHORT_PRICES_WEEKLY = mapOf(
        PRODUCT_OIC_NW_PASS to "₱20/wk",
        PRODUCT_GMDSS_PASS to "₱15/wk",
        PRODUCT_ALL_ACCESS_PASS to "₱35/wk",
        PRODUCT_ENGINE_OIC_EW_PASS to "₱20/wk",
        PRODUCT_DECK_MGT_PASS to "₱40/wk",
        PRODUCT_ENGINE_MGT_PASS to "₱40/wk"
    )

    fun getFallbackPrice(productId: String, cycle: BillingCycle = BillingCycle.YEARLY): String = when (cycle) {
        BillingCycle.WEEKLY -> DEFAULT_PRICES_WEEKLY[productId] ?: "₱20 / week"
        BillingCycle.MONTHLY -> DEFAULT_PRICES_MONTHLY[productId] ?: "₱50 / month"
        BillingCycle.YEARLY -> DEFAULT_PRICES_YEARLY[productId] ?: "₱350 / year"
    }

    fun getFallbackShortPrice(productId: String, cycle: BillingCycle = BillingCycle.YEARLY): String = when (cycle) {
        BillingCycle.WEEKLY -> DEFAULT_SHORT_PRICES_WEEKLY[productId] ?: "₱20/wk"
        BillingCycle.MONTHLY -> DEFAULT_SHORT_PRICES_MONTHLY[productId] ?: "₱50/mo"
        BillingCycle.YEARLY -> DEFAULT_SHORT_PRICES_YEARLY[productId] ?: "₱350/yr"
    }

    val PRODUCT_NAMES = mapOf(
        PRODUCT_OIC_NW_PASS to "Deck OIC-NW Pass",
        PRODUCT_GMDSS_PASS to "GMDSS GOC Pass",
        PRODUCT_ALL_ACCESS_PASS to "Hyperion All-Access Pass",
        PRODUCT_ENGINE_OIC_EW_PASS to "Engine OIC-EW Pass",
        PRODUCT_DECK_MGT_PASS to "Deck Management Pass",
        PRODUCT_ENGINE_MGT_PASS to "Engine Management Pass"
    )

    val PRODUCT_DESCRIPTIONS = mapOf(
        PRODUCT_OIC_NW_PASS to "Unlocks all 12 competencies, full question bank (6,445 items), rationale, and mock exams for Deck OIC-NW.",
        PRODUCT_GMDSS_PASS to "Unlocks all GMDSS competencies, radio communications bank (2,260 items), and mock exams.",
        PRODUCT_ALL_ACCESS_PASS to "Unlocks all 5 tracks (Deck OIC-NW, GMDSS GOC, Engine OIC-EW, Deck Management, Engine Management) with full rationale and offline access.",
        PRODUCT_ENGINE_OIC_EW_PASS to "Unlocks all Engine OIC-EW competencies, full question bank, and mock exams.",
        PRODUCT_DECK_MGT_PASS to "Unlocks all Deck Management competencies, full question bank, and mock exams.",
        PRODUCT_ENGINE_MGT_PASS to "Unlocks all Engine Management competencies, full question bank, and mock exams."
    )
}

/**
 * State representation of active Google Play entitlements with active plan metadata.
 */
data class PlayPurchaseForAccountLinking(val productId: String, val purchaseToken: String)

internal fun collectPlayPurchasesForAccountLinking(
    allAccessToken: String?,
    oicNwToken: String?,
    gmdssToken: String?,
    engineOicEwToken: String?,
    deckMgtToken: String?,
    engineMgtToken: String?
): List<PlayPurchaseForAccountLinking> = listOf(
    allAccessToken?.let { PlayPurchaseForAccountLinking(BillingConstants.PRODUCT_ALL_ACCESS_PASS, it) },
    oicNwToken?.let { PlayPurchaseForAccountLinking(BillingConstants.PRODUCT_OIC_NW_PASS, it) },
    gmdssToken?.let { PlayPurchaseForAccountLinking(BillingConstants.PRODUCT_GMDSS_PASS, it) },
    engineOicEwToken?.let { PlayPurchaseForAccountLinking(BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS, it) },
    deckMgtToken?.let { PlayPurchaseForAccountLinking(BillingConstants.PRODUCT_DECK_MGT_PASS, it) },
    engineMgtToken?.let { PlayPurchaseForAccountLinking(BillingConstants.PRODUCT_ENGINE_MGT_PASS, it) }
).filterNotNull()
    .filter { it.purchaseToken.isNotBlank() && !it.purchaseToken.startsWith("mock_sandbox_sub_token_") }
    .distinctBy { it.purchaseToken }

data class TrackEntitlements(
    val oicNwPlan: ActivePlanInfo? = null,
    val gmdssPlan: ActivePlanInfo? = null,
    val allAccessPlan: ActivePlanInfo? = null,
    val engineOicEwPlan: ActivePlanInfo? = null,
    val deckMgtPlan: ActivePlanInfo? = null,
    val engineMgtPlan: ActivePlanInfo? = null,
    val billingConnected: Boolean = false,
    val pendingPurchaseProductId: String? = null,
    val errorMessage: String? = null,
    val cachedBasePlan: String? = null,
    val cachedPurchaseTime: Long = 0L,
    val isClockTampered: Boolean = false,
    private val explicitOicNwUnlocked: Boolean? = null,
    private val explicitGmdssUnlocked: Boolean? = null,
    private val explicitAllAccessUnlocked: Boolean? = null,
    private val explicitEngineOicEwUnlocked: Boolean? = null,
    private val explicitDeckMgtUnlocked: Boolean? = null,
    private val explicitEngineMgtUnlocked: Boolean? = null
) {
    // Secondary constructor for backward compatibility
    constructor(
        oicNwUnlocked: Boolean = false,
        gmdssUnlocked: Boolean = false,
        allAccessUnlocked: Boolean = false,
        billingConnected: Boolean = false,
        pendingPurchaseProductId: String? = null,
        errorMessage: String? = null,
        cachedBasePlan: String? = null,
        cachedPurchaseTime: Long = 0L
    ) : this(
        oicNwPlan = if (oicNwUnlocked) ActivePlanInfo(cycle = BillingCycle.YEARLY) else null,
        gmdssPlan = if (gmdssUnlocked) ActivePlanInfo(cycle = BillingCycle.YEARLY) else null,
        allAccessPlan = if (allAccessUnlocked) ActivePlanInfo(cycle = BillingCycle.YEARLY) else null,
        billingConnected = billingConnected,
        pendingPurchaseProductId = pendingPurchaseProductId,
        errorMessage = errorMessage,
        cachedBasePlan = cachedBasePlan,
        cachedPurchaseTime = cachedPurchaseTime,
        isClockTampered = false,
        explicitOicNwUnlocked = if (oicNwUnlocked) true else null,
        explicitGmdssUnlocked = if (gmdssUnlocked) true else null,
        explicitAllAccessUnlocked = if (allAccessUnlocked) true else null
    )

    val isConnected: Boolean get() = billingConnected

    val oicNwUnlocked: Boolean
        get() = (explicitOicNwUnlocked == true) || (oicNwPlan != null && !oicNwPlan.isExpired) || (allAccessPlan != null && !allAccessPlan.isExpired)

    val gmdssUnlocked: Boolean
        get() = (explicitGmdssUnlocked == true) || (gmdssPlan != null && !gmdssPlan.isExpired) || (allAccessPlan != null && !allAccessPlan.isExpired)

    val allAccessUnlocked: Boolean
        get() = (explicitAllAccessUnlocked == true) || (allAccessPlan != null && !allAccessPlan.isExpired) ||
                (oicNwPlan != null && !oicNwPlan.isExpired && gmdssPlan != null && !gmdssPlan.isExpired && engineOicEwPlan != null && !engineOicEwPlan.isExpired && deckMgtPlan != null && !deckMgtPlan.isExpired && engineMgtPlan != null && !engineMgtPlan.isExpired)

    val engineOicEwUnlocked: Boolean
        get() = (explicitEngineOicEwUnlocked == true) || (engineOicEwPlan != null && !engineOicEwPlan.isExpired) || (allAccessPlan != null && !allAccessPlan.isExpired)

    val deckMgtUnlocked: Boolean
        get() = (explicitDeckMgtUnlocked == true) || (deckMgtPlan != null && !deckMgtPlan.isExpired) || (allAccessPlan != null && !allAccessPlan.isExpired)

    val engineMgtUnlocked: Boolean
        get() = (explicitEngineMgtUnlocked == true) || (engineMgtPlan != null && !engineMgtPlan.isExpired) || (allAccessPlan != null && !allAccessPlan.isExpired)

    fun isTrackUnlocked(track: Track): Boolean {
        if (allAccessUnlocked) return true
        return when (track) {
            Track.DECK_OIC_NW -> oicNwUnlocked
            Track.GMDSS_GOC -> gmdssUnlocked
            Track.ENGINE_OIC_EW -> engineOicEwUnlocked
            Track.DECK_MGT -> deckMgtUnlocked
            Track.ENGINE_MGT -> engineMgtUnlocked
        }
    }

    fun getActivePlanForTrack(track: Track): ActivePlanInfo? {
        if (allAccessPlan != null && !allAccessPlan.isExpired) return allAccessPlan
        return when (track) {
            Track.DECK_OIC_NW -> if (oicNwPlan != null && !oicNwPlan.isExpired) oicNwPlan else null
            Track.GMDSS_GOC -> if (gmdssPlan != null && !gmdssPlan.isExpired) gmdssPlan else null
            Track.ENGINE_OIC_EW -> if (engineOicEwPlan != null && !engineOicEwPlan.isExpired) engineOicEwPlan else null
            Track.DECK_MGT -> if (deckMgtPlan != null && !deckMgtPlan.isExpired) deckMgtPlan else null
            Track.ENGINE_MGT -> if (engineMgtPlan != null && !engineMgtPlan.isExpired) engineMgtPlan else null
        }
    }

    fun getFormattedTimeRemainingForTrack(track: Track): String {
        val plan = getActivePlanForTrack(track)
        return plan?.getFormattedTimeRemaining() ?: "Not Subscribed"
    }

    fun getExpiryDateForTrack(track: Track): String {
        val plan = getActivePlanForTrack(track)
        return plan?.getFormattedExpiryDate() ?: ""
    }
}

/**
 * UI-friendly item descriptor combining Play Store ProductDetails, selected cycle, and entitlement status.
 */
data class PlayProductItem(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val shortFormattedPrice: String = formattedPrice,
    val isOwned: Boolean,
    val isPending: Boolean,
    val selectedCycle: BillingCycle = BillingCycle.YEARLY,
    val savingsPercentage: Int? = null,
    val rawProductDetails: ProductDetails? = null
)

/**
 * Enterprise Google Play Core Billing Manager for Hyperion Maritime Reviewer.
 * Configured for Auto-Renewing Subscriptions (ProductType.SUBS) with Google Play Billing 7.x.
 */
class BillingManager private constructor(context: Context) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private const val TAG = "BillingManager"
        private const val PREFS_NAME = "hyperion_play_billing_secure_prefs"

        private const val KEY_OIC_NW_UNLOCKED = "play_entitlement_oic_nw"
        private const val KEY_GMDSS_UNLOCKED = "play_entitlement_gmdss"
        private const val KEY_ALL_ACCESS_UNLOCKED = "play_entitlement_all_access"
        private const val KEY_ENGINE_OIC_EW_UNLOCKED = "play_entitlement_engine_ew"
        private const val KEY_DECK_MGT_UNLOCKED = "play_entitlement_deck_mgt"
        private const val KEY_ENGINE_MGT_UNLOCKED = "play_entitlement_engine_mgt"

        private const val KEY_OIC_TOKEN = "play_token_oic_nw"
        private const val KEY_GMDSS_TOKEN = "play_token_gmdss"
        private const val KEY_ALL_ACCESS_TOKEN = "play_token_all_access"
        private const val KEY_ENGINE_OIC_EW_TOKEN = "play_token_engine_ew"
        private const val KEY_DECK_MGT_TOKEN = "play_token_deck_mgt"
        private const val KEY_ENGINE_MGT_TOKEN = "play_token_engine_mgt"

        private const val KEY_PURCHASE_TIME = "play_purchase_timestamp"
        private const val KEY_BASE_PLAN = "play_base_plan_id"
        private const val KEY_PLAY_HIGH_WATER_MARK = "play_license_high_water_mark"
        private const val KEY_PLAY_LAST_VERIFIED_AT = "play_license_last_verified_at"
        private const val KEY_PLAY_CACHE_MIGRATION_COMPLETE = "play_license_cache_migration_v1"
        private const val KEY_SIGNATURE_HASH = "play_entitlements_hash"
        private const val MAX_RECONNECT_DELAY_MS = 32000L

        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun resetForTesting() {
            synchronized(this) {
                INSTANCE?.resetEntitlementsForTesting()
                INSTANCE = null
            }
        }

        fun cycleFromBasePlanOrPeriod(basePlanId: String?, period: String?): BillingCycle =
            BillingCycle.values().firstOrNull {
                it.matchesPlayOffer(basePlanId.orEmpty(), listOfNotNull(period))
            } ?: BillingCycle.MONTHLY
    }

    fun resetEntitlementsForTesting() {
        try {
            securePrefs.edit().clear().apply()
        } catch (_: Exception) {}
        _entitlements.value = TrackEntitlements()
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _entitlements = MutableStateFlow(TrackEntitlements())
    val entitlements: StateFlow<TrackEntitlements> = _entitlements.asStateFlow()

    private val _productsDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productsDetails: StateFlow<Map<String, ProductDetails>> = _productsDetails.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val lastRequestedCycles = java.util.concurrent.ConcurrentHashMap<String, BillingCycle>()
    @Volatile private var checkoutFlowProductId: String? = null

    private var reconnectDelayMs = 1000L

    private val securePrefs: SharedPreferences by lazy {
        initSecurePreferences(appContext)
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        scope.launch {
            loadCachedEntitlements()
            startConnection()
            SubscriptionSyncWorker.schedule(appContext)
        }
    }

    private fun hasValidatedInternet(): Boolean {
        val connectivity = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun initSecurePreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences init failed, falling back to private prefs with HMAC checksum: ${e.message}")
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun startConnection() {
        if (billingClient.isReady) {
            _entitlements.update { it.copy(billingConnected = true) }
            return
        }
        if (_isConnecting.value) return

        _isConnecting.value = true
        try {
            billingClient.startConnection(this)
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting billing connection: ${e.message}")
            _isConnecting.value = false
            scheduleReconnect()
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        _isConnecting.value = false
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.i(TAG, "Google Play BillingClient setup finished successfully")
            reconnectDelayMs = 1000L
            _entitlements.update { it.copy(billingConnected = true, errorMessage = null) }

            scope.launch {
                queryProductDetails()
                queryPurchasesAsync()
                syncAccountAfterPlayPurchaseChanged()
            }
        } else {
            Log.w(TAG, "Billing setup failed: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
            _entitlements.update {
                it.copy(
                    billingConnected = false,
                    errorMessage = if (billingResult.responseCode != BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                        billingResult.debugMessage
                    } else null
                )
            }
            scheduleReconnect()
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "Google Play Billing service disconnected. Scheduling exponential backoff reconnect.")
        _isConnecting.value = false
        _entitlements.update { it.copy(billingConnected = false) }
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(reconnectDelayMs)
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            startConnection()
        }
    }

    suspend fun queryProductDetails() = withContext(Dispatchers.IO) {
        if (!billingClient.isReady) {
            Log.w(TAG, "BillingClient not ready when querying product details")
            return@withContext
        }

        val productList = BillingConstants.ALL_PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        try {
            val result = billingClient.queryProductDetails(params)
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val detailsList = result.productDetailsList ?: emptyList()
                val detailsMap = detailsList.associateBy { it.productId }
                _productsDetails.value = detailsMap
                Log.i(TAG, "Loaded ${detailsList.size} subscription products from Google Play")
            } else {
                Log.e(TAG, "queryProductDetails failed: ${result.billingResult.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying product details: ${e.message}")
        }
    }

    suspend fun queryProductDetailsAsync() = queryProductDetails()

    fun isTrackUnlocked(track: Track): Boolean = _entitlements.value.isTrackUnlocked(track)

    /**
     * Returns Play tokens from the current BillingClient-owned subscription snapshot.
     * Tokens are only sent to Hyperion's authenticated backend for Google verification.
     */
    fun getOwnedPurchasesForAccountLinking(): List<PlayPurchaseForAccountLinking> {
        val owned = _entitlements.value
        return collectPlayPurchasesForAccountLinking(
            allAccessToken = owned.allAccessPlan?.purchaseToken,
            oicNwToken = owned.oicNwPlan?.purchaseToken,
            gmdssToken = owned.gmdssPlan?.purchaseToken,
            engineOicEwToken = owned.engineOicEwPlan?.purchaseToken,
            deckMgtToken = owned.deckMgtPlan?.purchaseToken,
            engineMgtToken = owned.engineMgtPlan?.purchaseToken
        )
    }

    /**
     * Refreshes the Play-owned subscription snapshot. The Boolean is true only when
     * BillingClient returned an authoritative successful snapshot while the device
     * had validated internet; callers must not link a stale cached token after failure.
     */
    suspend fun queryPurchasesAsync(): Boolean = withContext(Dispatchers.IO) {
        if (!billingClient.isReady) {
            Log.w(TAG, "BillingClient not ready for queryPurchasesAsync")
            return@withContext false
        }
        if (!hasValidatedInternet()) {
            Log.i(TAG, "Skipping Play ownership refresh without validated internet")
            return@withContext false
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        try {
            val result = billingClient.queryPurchasesAsync(params)
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // Return success only if processPurchases actually applied this snapshot.
                // Connectivity can disappear between the query and the cache update.
                processPurchases(result.purchasesList)
            } else {
                Log.w(TAG, "queryPurchasesAsync response not OK: ${result.billingResult.debugMessage}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception restoring purchases: ${e.message}")
            false
        }
    }

    fun queryPurchasesOnResume() {
        scope.launch {
            // MainActivity calls this after the Play sheet closes and the app resumes.
            // A still-pending transaction is preserved by the purchase snapshot itself.
            checkoutFlowProductId = null
            if (!billingClient.isReady) startConnection()

            // A disconnected BillingClient can take a moment to reconnect. Do not skip
            // the foreground entitlement refresh just because Play was reconnecting.
            val connected = withTimeoutOrNull(15_000L) {
                _entitlements.first { it.billingConnected }
            } != null
            if (!connected) return@launch

            // Refresh local ownership first, then ask the authenticated server to read
            // Google's current SubscriptionsV2 expiryTime. This picks up auto-renewals,
            // grace-period changes, and accelerated license-tester renewals.
            queryPurchasesAsync()
            if (hasValidatedInternet()) {
                syncAccountAfterPlayPurchaseChanged()
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        checkoutFlowProductId = null
        scope.launch {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    _entitlements.update { it.copy(pendingPurchaseProductId = null, errorMessage = null) }
                    val hasCompletedPurchase = purchases.orEmpty().any {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    purchases?.forEach { purchase ->
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                            val ackParams = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                            billingClient.acknowledgePurchase(ackParams)
                        }
                    }
                    queryPurchasesAsync()
                    if (hasCompletedPurchase) {
                        syncAccountAfterPlayPurchaseChanged()
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    val productId = _entitlements.value.pendingPurchaseProductId
                    productId?.let { clearPendingBillingCycle(it) }
                    Log.i(TAG, "Purchase cancelled by user")
                    _entitlements.update { it.copy(pendingPurchaseProductId = null, errorMessage = null) }
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    val productId = _entitlements.value.pendingPurchaseProductId
                    productId?.let { clearPendingBillingCycle(it) }
                    Log.i(TAG, "Item already owned. Re-querying purchases to restore entitlement.")
                    _entitlements.update { it.copy(pendingPurchaseProductId = null, errorMessage = null) }
                    queryPurchasesAsync()
                    syncAccountAfterPlayPurchaseChanged()
                }
                else -> {
                    val productId = _entitlements.value.pendingPurchaseProductId
                    productId?.let { clearPendingBillingCycle(it) }
                    Log.e(TAG, "Purchase flow error: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
                    _entitlements.update {
                        it.copy(
                            pendingPurchaseProductId = null,
                            errorMessage = billingResult.debugMessage.ifBlank { "Purchase could not be completed (Code ${billingResult.responseCode})" }
                        )
                    }
                }
            }
        }
    }

    private fun clearPendingBillingCycle(productId: String) {
        val cycleKey = when (productId) {
            BillingConstants.PRODUCT_OIC_NW_PASS -> "play_plan_cycle_oic"
            BillingConstants.PRODUCT_GMDSS_PASS -> "play_plan_cycle_gmdss"
            BillingConstants.PRODUCT_ALL_ACCESS_PASS -> "play_plan_cycle_all_access"
            BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> "play_plan_cycle_engine_ew"
            BillingConstants.PRODUCT_DECK_MGT_PASS -> "play_plan_cycle_deck_mgt"
            BillingConstants.PRODUCT_ENGINE_MGT_PASS -> "play_plan_cycle_engine_mgt"
            else -> return
        }
        securePrefs.edit().remove("${cycleKey}_pending").apply()
        lastRequestedCycles.remove(productId)
        if (checkoutFlowProductId == productId) checkoutFlowProductId = null
    }

    private suspend fun syncAccountAfterPlayPurchaseChanged() {
        try {
            SupabaseAccountManager.syncAfterPlayPurchaseIfSignedIn(appContext)
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Google Play purchase was processed, but Hyperion account sync could not start. Retry from My Hyperion Account.",
                e
            )
        }
    }

    private suspend fun processPurchases(purchases: List<Purchase>): Boolean = withContext(Dispatchers.IO) {
        // BillingClient may expose a locally cached or empty snapshot while offline.
        // Without validated internet it cannot be used to revoke the last known-good
        // subscription cache. Wait for an online ownership query before mutating it.
        if (!shouldApplyPlayPurchaseSnapshot(hasValidatedInternet())) {
            Log.i(TAG, "Skipping offline Play ownership snapshot; preserving last verified subscription cache")
            return@withContext false
        }

        var oicPlan: ActivePlanInfo? = null
        var gmdssPlan: ActivePlanInfo? = null
        var allAccessPlan: ActivePlanInfo? = null
        var engineOicEwPlan: ActivePlanInfo? = null
        var deckMgtPlan: ActivePlanInfo? = null
        var engineMgtPlan: ActivePlanInfo? = null
        var pendingProduct: String? = null
        var lastPurchaseTime = 0L
        var lastBasePlan: String? = null

        val queryTimeEpoch = System.currentTimeMillis()
        val savedHighWaterMark = securePrefs.getLong(KEY_PLAY_HIGH_WATER_MARK, 0L)
        val priorVerifiedAt = securePrefs.getLong(KEY_PLAY_LAST_VERIFIED_AT, 0L)
        val mayRefreshVerification = canRefreshPlayVerification(
            hasValidatedInternet = hasValidatedInternet(),
            nowEpochMs = queryTimeEpoch,
            savedHighWaterMarkEpochMs = savedHighWaterMark
        )
        // A local Play Store snapshot while offline is not a fresh server verification.
        // It may preserve the existing lease, but must never renew its 72-hour deadline.
        val verifiedAtEpoch = if (mayRefreshVerification) queryTimeEpoch else priorVerifiedAt

        val editor = securePrefs.edit()

        for (purchase in purchases) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!purchase.isAcknowledged) {
                        val ackParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        val ackResult = billingClient.acknowledgePurchase(ackParams)
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.i(TAG, "Acknowledged purchase for: ${purchase.products}")
                        } else {
                            Log.e(TAG, "Failed to acknowledge purchase: ${ackResult.debugMessage}")
                        }
                    }

                    if (purchase.purchaseTime > lastPurchaseTime) {
                        lastPurchaseTime = purchase.purchaseTime
                    }

                    for (productId in purchase.products) {
                        val details = _productsDetails.value[productId]
                        val cycleKey = when (productId) {
                            BillingConstants.PRODUCT_OIC_NW_PASS -> "play_plan_cycle_oic"
                            BillingConstants.PRODUCT_GMDSS_PASS -> "play_plan_cycle_gmdss"
                            BillingConstants.PRODUCT_ALL_ACCESS_PASS -> "play_plan_cycle_all_access"
                            BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> "play_plan_cycle_engine_ew"
                            BillingConstants.PRODUCT_DECK_MGT_PASS -> "play_plan_cycle_deck_mgt"
                            BillingConstants.PRODUCT_ENGINE_MGT_PASS -> "play_plan_cycle_engine_mgt"
                            else -> "play_plan_cycle_default"
                        }
                        val purchaseTimeKey = when (productId) {
                            BillingConstants.PRODUCT_OIC_NW_PASS -> "play_purchase_time_oic"
                            BillingConstants.PRODUCT_GMDSS_PASS -> "play_purchase_time_gmdss"
                            BillingConstants.PRODUCT_ALL_ACCESS_PASS -> "play_purchase_time_all_access"
                            BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> "play_purchase_time_engine_ew"
                            BillingConstants.PRODUCT_DECK_MGT_PASS -> "play_purchase_time_deck_mgt"
                            BillingConstants.PRODUCT_ENGINE_MGT_PASS -> "play_purchase_time_engine_mgt"
                            else -> "play_purchase_time_default"
                        }
                        val purchaseTokenKey = when (productId) {
                            BillingConstants.PRODUCT_OIC_NW_PASS -> KEY_OIC_TOKEN
                            BillingConstants.PRODUCT_GMDSS_PASS -> KEY_GMDSS_TOKEN
                            BillingConstants.PRODUCT_ALL_ACCESS_PASS -> KEY_ALL_ACCESS_TOKEN
                            BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> KEY_ENGINE_OIC_EW_TOKEN
                            BillingConstants.PRODUCT_DECK_MGT_PASS -> KEY_DECK_MGT_TOKEN
                            BillingConstants.PRODUCT_ENGINE_MGT_PASS -> KEY_ENGINE_MGT_TOKEN
                            else -> "play_purchase_token_default"
                        }
                        val pendingCycleKey = "${cycleKey}_pending"
                        val activeCycle = securePrefs.getString(cycleKey, null)
                            ?.let { runCatching { BillingCycle.valueOf(it) }.getOrNull() }
                        val pendingCycle = lastRequestedCycles[productId]
                            ?: securePrefs.getString(pendingCycleKey, null)
                                ?.let { runCatching { BillingCycle.valueOf(it) }.getOrNull() }
                        val previousPurchaseTime = securePrefs.getLong(
                            purchaseTimeKey,
                            if (activeCycle != null) securePrefs.getLong(KEY_PURCHASE_TIME, 0L) else 0L
                        )
                        val previousPurchaseToken = securePrefs.getString(purchaseTokenKey, null)
                        val purchaseTime = if (purchase.purchaseTime > 0L) purchase.purchaseTime else queryTimeEpoch
                        val selectedCycleWasPurchased = isNewPlaySubscriptionPurchase(
                            previousPurchaseToken = previousPurchaseToken,
                            currentPurchaseToken = purchase.purchaseToken,
                            previousPurchaseTimeEpochMs = previousPurchaseTime,
                            currentPurchaseTimeEpochMs = purchase.purchaseTime
                        )
                        val cycle = resolvePurchasedBillingCycle(
                            activeCycle = activeCycle,
                            pendingCycle = pendingCycle,
                            isNewPurchase = selectedCycleWasPurchased
                        )

                        // Confirm a pending Store selection only when Google Play reports a
                        // newer purchase or a replacement purchase token. A cancelled checkout
                        // or restore of the same purchase keeps its previously saved duration.
                        if (pendingCycle != null) {
                            val checkoutStillOpen = _entitlements.value.pendingPurchaseProductId == productId
                            if (selectedCycleWasPurchased || !checkoutStillOpen) {
                                editor.remove(pendingCycleKey)
                                lastRequestedCycles.remove(productId)
                            }
                        }

                        val offer = details?.subscriptionOfferDetails?.firstOrNull { candidate ->
                            cycle.matchesPlayOffer(
                                basePlanId = candidate.basePlanId,
                                billingPeriods = candidate.pricingPhases.pricingPhaseList.map { it.billingPeriod }
                            )
                        }
                        val currentWaterMark = maxOf(savedHighWaterMark, purchaseTime, queryTimeEpoch)
                        val effectiveQueryTime = maxOf(queryTimeEpoch, currentWaterMark)

                        // BillingClient Purchase.getPurchaseTime() is the original subscription
                        // signup time. Use calendar-period arithmetic for the offline display
                        // fallback; linked accounts receive the exact expiry from Play's API.
                        val expiryTime = estimateNextSubscriptionExpiry(
                            purchaseTimeEpochMs = purchaseTime,
                            nowEpochMs = effectiveQueryTime,
                            cycle = cycle
                        )

                        when (productId) {
                            BillingConstants.PRODUCT_OIC_NW_PASS -> {
                                oicPlan = ActivePlanInfo(
                                    cycle = cycle,
                                    purchaseToken = purchase.purchaseToken,
                                    purchaseTimestamp = purchaseTime,
                                    expiryTimestamp = expiryTime,
                                    highWaterMarkEpoch = currentWaterMark,
                                    lastVerifiedAtEpoch = verifiedAtEpoch
                                )
                            }
                            BillingConstants.PRODUCT_GMDSS_PASS -> {
                                gmdssPlan = ActivePlanInfo(
                                    cycle = cycle,
                                    purchaseToken = purchase.purchaseToken,
                                    purchaseTimestamp = purchaseTime,
                                    expiryTimestamp = expiryTime,
                                    highWaterMarkEpoch = currentWaterMark,
                                    lastVerifiedAtEpoch = verifiedAtEpoch
                                )
                            }
                            BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> {
                                engineOicEwPlan = ActivePlanInfo(
                                    cycle = cycle,
                                    purchaseToken = purchase.purchaseToken,
                                    purchaseTimestamp = purchaseTime,
                                    expiryTimestamp = expiryTime,
                                    highWaterMarkEpoch = currentWaterMark,
                                    lastVerifiedAtEpoch = verifiedAtEpoch
                                )
                            }
                            BillingConstants.PRODUCT_DECK_MGT_PASS -> {
                                deckMgtPlan = ActivePlanInfo(
                                    cycle = cycle,
                                    purchaseToken = purchase.purchaseToken,
                                    purchaseTimestamp = purchaseTime,
                                    expiryTimestamp = expiryTime,
                                    highWaterMarkEpoch = currentWaterMark,
                                    lastVerifiedAtEpoch = verifiedAtEpoch
                                )
                            }
                            BillingConstants.PRODUCT_ENGINE_MGT_PASS -> {
                                engineMgtPlan = ActivePlanInfo(
                                    cycle = cycle,
                                    purchaseToken = purchase.purchaseToken,
                                    purchaseTimestamp = purchaseTime,
                                    expiryTimestamp = expiryTime,
                                    highWaterMarkEpoch = currentWaterMark,
                                    lastVerifiedAtEpoch = verifiedAtEpoch
                                )
                            }
                            BillingConstants.PRODUCT_ALL_ACCESS_PASS -> {
                                val plan = ActivePlanInfo(
                                    cycle = cycle,
                                    purchaseToken = purchase.purchaseToken,
                                    purchaseTimestamp = purchaseTime,
                                    expiryTimestamp = expiryTime,
                                    highWaterMarkEpoch = currentWaterMark,
                                    lastVerifiedAtEpoch = verifiedAtEpoch
                                )
                                allAccessPlan = plan
                                oicPlan = plan
                                gmdssPlan = plan
                                engineOicEwPlan = plan
                                deckMgtPlan = plan
                                engineMgtPlan = plan
                            }
                        }
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    Log.i(TAG, "Purchase is pending verification: ${purchase.products}")
                    pendingProduct = purchase.products.firstOrNull()
                }
                else -> {
                    Log.w(TAG, "Non-purchased state encountered: ${purchase.purchaseState}")
                }
            }
        }

        // Google Play Subscriptions are verified server-side; device clock changes do not invalidate active purchases
        val isClockTampered = false

        // Keep Google Play's last-successful-query timestamp separate from purchase time.
        // Purchase.purchaseTime never changes after renewal, so it cannot be used as an offline lease.
        // Synchronize Secure SharedPreferences disk cache strictly with active Google Play purchases
        if (oicPlan != null) {
            editor.putBoolean(KEY_OIC_NW_UNLOCKED, true)
            editor.putString("play_plan_cycle_oic", oicPlan.cycle.name)
            editor.putString(KEY_OIC_TOKEN, oicPlan.purchaseToken)
            editor.putLong("play_purchase_time_oic", oicPlan.purchaseTimestamp)
            editor.putLong("play_expiry_time_oic", oicPlan.expiryTimestamp)
        } else {
            editor.putBoolean(KEY_OIC_NW_UNLOCKED, false)
            editor.remove(KEY_OIC_TOKEN)
            editor.remove("play_purchase_time_oic")
            editor.remove("play_expiry_time_oic")
            editor.remove("play_plan_cycle_oic")
        }

        if (gmdssPlan != null) {
            editor.putBoolean(KEY_GMDSS_UNLOCKED, true)
            editor.putString("play_plan_cycle_gmdss", gmdssPlan.cycle.name)
            editor.putString(KEY_GMDSS_TOKEN, gmdssPlan.purchaseToken)
            editor.putLong("play_purchase_time_gmdss", gmdssPlan.purchaseTimestamp)
            editor.putLong("play_expiry_time_gmdss", gmdssPlan.expiryTimestamp)
        } else {
            editor.putBoolean(KEY_GMDSS_UNLOCKED, false)
            editor.remove(KEY_GMDSS_TOKEN)
            editor.remove("play_purchase_time_gmdss")
            editor.remove("play_expiry_time_gmdss")
            editor.remove("play_plan_cycle_gmdss")
        }

        if (allAccessPlan != null) {
            editor.putBoolean(KEY_ALL_ACCESS_UNLOCKED, true)
            editor.putString("play_plan_cycle_all_access", allAccessPlan.cycle.name)
            editor.putString(KEY_ALL_ACCESS_TOKEN, allAccessPlan.purchaseToken)
            editor.putLong("play_purchase_time_all_access", allAccessPlan.purchaseTimestamp)
            editor.putLong("play_expiry_time_all_access", allAccessPlan.expiryTimestamp)
        } else {
            editor.putBoolean(KEY_ALL_ACCESS_UNLOCKED, false)
            editor.remove(KEY_ALL_ACCESS_TOKEN)
            editor.remove("play_purchase_time_all_access")
            editor.remove("play_expiry_time_all_access")
            editor.remove("play_plan_cycle_all_access")
        }

        if (engineOicEwPlan != null) {
            editor.putBoolean(KEY_ENGINE_OIC_EW_UNLOCKED, true)
            editor.putString("play_plan_cycle_engine_ew", engineOicEwPlan.cycle.name)
            editor.putString(KEY_ENGINE_OIC_EW_TOKEN, engineOicEwPlan.purchaseToken)
            editor.putLong("play_purchase_time_engine_ew", engineOicEwPlan.purchaseTimestamp)
            editor.putLong("play_expiry_time_engine_ew", engineOicEwPlan.expiryTimestamp)
        } else {
            editor.putBoolean(KEY_ENGINE_OIC_EW_UNLOCKED, false)
            editor.remove(KEY_ENGINE_OIC_EW_TOKEN)
            editor.remove("play_purchase_time_engine_ew")
            editor.remove("play_expiry_time_engine_ew")
            editor.remove("play_plan_cycle_engine_ew")
        }

        if (deckMgtPlan != null) {
            editor.putBoolean(KEY_DECK_MGT_UNLOCKED, true)
            editor.putString("play_plan_cycle_deck_mgt", deckMgtPlan.cycle.name)
            editor.putString(KEY_DECK_MGT_TOKEN, deckMgtPlan.purchaseToken)
            editor.putLong("play_purchase_time_deck_mgt", deckMgtPlan.purchaseTimestamp)
            editor.putLong("play_expiry_time_deck_mgt", deckMgtPlan.expiryTimestamp)
        } else {
            editor.putBoolean(KEY_DECK_MGT_UNLOCKED, false)
            editor.remove(KEY_DECK_MGT_TOKEN)
            editor.remove("play_purchase_time_deck_mgt")
            editor.remove("play_expiry_time_deck_mgt")
            editor.remove("play_plan_cycle_deck_mgt")
        }

        if (engineMgtPlan != null) {
            editor.putBoolean(KEY_ENGINE_MGT_UNLOCKED, true)
            editor.putString("play_plan_cycle_engine_mgt", engineMgtPlan.cycle.name)
            editor.putString(KEY_ENGINE_MGT_TOKEN, engineMgtPlan.purchaseToken)
            editor.putLong("play_purchase_time_engine_mgt", engineMgtPlan.purchaseTimestamp)
            editor.putLong("play_expiry_time_engine_mgt", engineMgtPlan.expiryTimestamp)
        } else {
            editor.putBoolean(KEY_ENGINE_MGT_UNLOCKED, false)
            editor.remove(KEY_ENGINE_MGT_TOKEN)
            editor.remove("play_purchase_time_engine_mgt")
            editor.remove("play_expiry_time_engine_mgt")
            editor.remove("play_plan_cycle_engine_mgt")
        }

        val hasOic = oicPlan != null && !oicPlan.isExpired
        val hasGmdss = gmdssPlan != null && !gmdssPlan.isExpired
        val hasAll = allAccessPlan != null && !allAccessPlan.isExpired
        val hasEw = engineOicEwPlan != null && !engineOicEwPlan.isExpired
        val hasDm = deckMgtPlan != null && !deckMgtPlan.isExpired
        val hasEm = engineMgtPlan != null && !engineMgtPlan.isExpired
        // Use raw owned-purchase presence for cache lifecycle. An expired offline lease
        // must retain its old timestamp; clearing it would trigger the one-time legacy migration.

        val signature = computeIntegrityChecksum(hasOic, hasGmdss, hasAll, hasEw, hasDm, hasEm)
        editor.putString(KEY_SIGNATURE_HASH, signature)
        editor.putLong(KEY_PURCHASE_TIME, lastPurchaseTime)

        val hasAnyOwnedPurchase = oicPlan != null || gmdssPlan != null ||
            allAccessPlan != null || engineOicEwPlan != null || deckMgtPlan != null || engineMgtPlan != null

        val observedProductIds = purchases.flatMap { it.products }.toSet()
        val checkoutProductId = checkoutFlowProductId
        val productCycleKeys = mapOf(
            BillingConstants.PRODUCT_OIC_NW_PASS to "play_plan_cycle_oic",
            BillingConstants.PRODUCT_GMDSS_PASS to "play_plan_cycle_gmdss",
            BillingConstants.PRODUCT_ALL_ACCESS_PASS to "play_plan_cycle_all_access",
            BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS to "play_plan_cycle_engine_ew",
            BillingConstants.PRODUCT_DECK_MGT_PASS to "play_plan_cycle_deck_mgt",
            BillingConstants.PRODUCT_ENGINE_MGT_PASS to "play_plan_cycle_engine_mgt"
        )
        productCycleKeys.forEach { (productId, cycleKey) ->
            // Clear only stale/unowned pending choices. Keep a choice while its checkout
            // is open or while Play reports that product as pending/purchased.
            if (productId !in observedProductIds && checkoutProductId != productId) {
                editor.remove("${cycleKey}_pending")
                lastRequestedCycles.remove(productId)
            }
        }

        // Persist every observed clock advance, but advance the verification lease only
        // when Android reports a validated internet connection and the clock is monotonic.
        editor.putLong(
            KEY_PLAY_HIGH_WATER_MARK,
            maxOf(savedHighWaterMark, queryTimeEpoch, lastPurchaseTime)
        )
        if (hasAnyOwnedPurchase && mayRefreshVerification) {
            editor.putLong(KEY_PLAY_LAST_VERIFIED_AT, queryTimeEpoch)
        } else if (!hasAnyOwnedPurchase) {
            // A successful empty Play snapshot revokes all local Play plans immediately.
            editor.remove(KEY_PLAY_LAST_VERIFIED_AT)
        }
        editor.apply()

        _entitlements.update {
            TrackEntitlements(
                oicNwPlan = oicPlan,
                gmdssPlan = gmdssPlan,
                allAccessPlan = allAccessPlan,
                engineOicEwPlan = engineOicEwPlan,
                deckMgtPlan = deckMgtPlan,
                engineMgtPlan = engineMgtPlan,
                billingConnected = true,
                pendingPurchaseProductId = pendingProduct,
                errorMessage = null,
                cachedBasePlan = lastBasePlan,
                cachedPurchaseTime = lastPurchaseTime,
                isClockTampered = isClockTampered
            )
        }
        true
    }

    private fun migrateLegacyWeeklyFallbacks() {
        val migrationKey = "billing_migrated_weekly_fallback_v3"
        if (!securePrefs.getBoolean(migrationKey, false)) {
            val editor = securePrefs.edit()
            val oicCycle = securePrefs.getString("play_plan_cycle_oic", null)
            val oicToken = securePrefs.getString(KEY_OIC_TOKEN, null)
            val hasOic = securePrefs.getBoolean(KEY_OIC_NW_UNLOCKED, false)
            if (hasOic && !oicToken.isNullOrBlank() && oicCycle == null) {
                editor.putString("play_plan_cycle_oic", BillingCycle.MONTHLY.name)
                val purchaseTime = securePrefs.getLong("play_purchase_time_oic", 0L)
                if (purchaseTime > 0L) {
                    val correctedExpiry = BillingCycle.MONTHLY.nextBillingPeriod(purchaseTime)
                    editor.putLong("play_expiry_time_oic", correctedExpiry)
                }
            }

            val gmdssCycle = securePrefs.getString("play_plan_cycle_gmdss", null)
            val gmdssToken = securePrefs.getString(KEY_GMDSS_TOKEN, null)
            val hasGmdss = securePrefs.getBoolean(KEY_GMDSS_UNLOCKED, false)
            if (hasGmdss && !gmdssToken.isNullOrBlank() && gmdssCycle == null) {
                editor.putString("play_plan_cycle_gmdss", BillingCycle.MONTHLY.name)
                val purchaseTime = securePrefs.getLong("play_purchase_time_gmdss", 0L)
                if (purchaseTime > 0L) {
                    val correctedExpiry = BillingCycle.MONTHLY.nextBillingPeriod(purchaseTime)
                    editor.putLong("play_expiry_time_gmdss", correctedExpiry)
                }
            }

            val allAccessCycle = securePrefs.getString("play_plan_cycle_all_access", null)
            val allToken = securePrefs.getString(KEY_ALL_ACCESS_TOKEN, null)
            val hasAll = securePrefs.getBoolean(KEY_ALL_ACCESS_UNLOCKED, false)
            if (hasAll && !allToken.isNullOrBlank() && allAccessCycle == null) {
                editor.putString("play_plan_cycle_all_access", BillingCycle.MONTHLY.name)
                val purchaseTime = securePrefs.getLong("play_purchase_time_all_access", 0L)
                if (purchaseTime > 0L) {
                    val correctedExpiry = BillingCycle.MONTHLY.nextBillingPeriod(purchaseTime)
                    editor.putLong("play_expiry_time_all_access", correctedExpiry)
                }
            }
            editor.putBoolean(migrationKey, true)
            editor.apply()
        }
    }

    private fun loadCachedEntitlements() {
        migrateLegacyWeeklyFallbacks()

        val oic = securePrefs.getBoolean(KEY_OIC_NW_UNLOCKED, false)
        val gmdss = securePrefs.getBoolean(KEY_GMDSS_UNLOCKED, false)
        val allAccess = securePrefs.getBoolean(KEY_ALL_ACCESS_UNLOCKED, false)
        val purchaseTime = securePrefs.getLong(KEY_PURCHASE_TIME, 0L)
        val basePlan = securePrefs.getString(KEY_BASE_PLAN, null)

        val oicCycleName = securePrefs.getString("play_plan_cycle_oic", null)
        val gmdssCycleName = securePrefs.getString("play_plan_cycle_gmdss", null)
        val allAccessCycleName = securePrefs.getString("play_plan_cycle_all_access", null)
        val engineEwCycleName = securePrefs.getString("play_plan_cycle_engine_ew", null)
        val deckMgtCycleName = securePrefs.getString("play_plan_cycle_deck_mgt", null)
        val engineMgtCycleName = securePrefs.getString("play_plan_cycle_engine_mgt", null)

        val oicCycle = oicCycleName?.let { runCatching { BillingCycle.valueOf(it) }.getOrNull() } ?: BillingCycle.MONTHLY
        val gmdssCycle = gmdssCycleName?.let { runCatching { BillingCycle.valueOf(it) }.getOrNull() } ?: BillingCycle.MONTHLY
        val allAccessCycle = allAccessCycleName?.let { runCatching { BillingCycle.valueOf(it) }.getOrNull() } ?: BillingCycle.MONTHLY
        val engineEwCycle = engineEwCycleName?.let { runCatching { BillingCycle.valueOf(it) }.getOrNull() } ?: BillingCycle.MONTHLY
        val deckMgtCycle = deckMgtCycleName?.let { runCatching { BillingCycle.valueOf(it) }.getOrNull() } ?: BillingCycle.MONTHLY
        val engineMgtCycle = engineMgtCycleName?.let { runCatching { BillingCycle.valueOf(it) }.getOrNull() } ?: BillingCycle.MONTHLY

        val now = System.currentTimeMillis()
        val oicPurchase = securePrefs.getLong("play_purchase_time_oic", if (purchaseTime > 0L) purchaseTime else now)
        var oicExpiry = securePrefs.getLong("play_expiry_time_oic", oicCycle.nextBillingPeriod(oicPurchase))
        if (oic && oicExpiry < oicCycle.nextBillingPeriod(oicPurchase)) {
            oicExpiry = oicCycle.nextBillingPeriod(oicPurchase)
        }

        val gmdssPurchase = securePrefs.getLong("play_purchase_time_gmdss", if (purchaseTime > 0L) purchaseTime else now)
        var gmdssExpiry = securePrefs.getLong("play_expiry_time_gmdss", gmdssCycle.nextBillingPeriod(gmdssPurchase))
        if (gmdss && gmdssExpiry < gmdssCycle.nextBillingPeriod(gmdssPurchase)) {
            gmdssExpiry = gmdssCycle.nextBillingPeriod(gmdssPurchase)
        }

        val allAccessPurchase = securePrefs.getLong("play_purchase_time_all_access", if (purchaseTime > 0L) purchaseTime else now)
        var allAccessExpiry = securePrefs.getLong("play_expiry_time_all_access", allAccessCycle.nextBillingPeriod(allAccessPurchase))
        if (allAccess && allAccessExpiry < allAccessCycle.nextBillingPeriod(allAccessPurchase)) {
            allAccessExpiry = allAccessCycle.nextBillingPeriod(allAccessPurchase)
        }

        var playHighWaterMark = securePrefs.getLong(KEY_PLAY_HIGH_WATER_MARK, 0L)
        var playLastVerifiedAt = securePrefs.getLong(KEY_PLAY_LAST_VERIFIED_AT, 0L)

        // Older app builds did not persist the last successful Play-query timestamp.
        // Give an existing encrypted cache one bounded migration grace, then require
        // a fresh Google Play query. New builds persist the timestamp on every query.
        val hasLegacyCachedPlayPlan = oic || gmdss || allAccess ||
            securePrefs.getBoolean(KEY_ENGINE_OIC_EW_UNLOCKED, false) ||
            securePrefs.getBoolean(KEY_DECK_MGT_UNLOCKED, false) ||
            securePrefs.getBoolean(KEY_ENGINE_MGT_UNLOCKED, false)
        val migrationAlreadyComplete = securePrefs.getBoolean(KEY_PLAY_CACHE_MIGRATION_COMPLETE, false)
        if (!migrationAlreadyComplete && hasLegacyCachedPlayPlan && playLastVerifiedAt <= 0L) {
            // Existing installations need one bounded transition grace because their old
            // cache did not record query time. New purchases must never receive this grace.
            playLastVerifiedAt = now
            playHighWaterMark = maxOf(playHighWaterMark, now)
        }
        if (!migrationAlreadyComplete) {
            securePrefs.edit()
                .putBoolean(KEY_PLAY_CACHE_MIGRATION_COMPLETE, true)
                .putLong(KEY_PLAY_LAST_VERIFIED_AT, playLastVerifiedAt)
                .putLong(KEY_PLAY_HIGH_WATER_MARK, playHighWaterMark)
                .apply()
        }

        val oicPlan = if (oic) ActivePlanInfo(
            cycle = oicCycle,
            purchaseToken = securePrefs.getString(KEY_OIC_TOKEN, null),
            purchaseTimestamp = oicPurchase,
            expiryTimestamp = oicExpiry,
            highWaterMarkEpoch = playHighWaterMark,
            lastVerifiedAtEpoch = playLastVerifiedAt
        ) else null

        val gmdssPlan = if (gmdss) ActivePlanInfo(
            cycle = gmdssCycle,
            purchaseToken = securePrefs.getString(KEY_GMDSS_TOKEN, null),
            purchaseTimestamp = gmdssPurchase,
            expiryTimestamp = gmdssExpiry,
            highWaterMarkEpoch = playHighWaterMark,
            lastVerifiedAtEpoch = playLastVerifiedAt
        ) else null

        val allAccessPlan = if (allAccess) ActivePlanInfo(
            cycle = allAccessCycle,
            purchaseToken = securePrefs.getString(KEY_ALL_ACCESS_TOKEN, null),
            purchaseTimestamp = allAccessPurchase,
            expiryTimestamp = allAccessExpiry,
            highWaterMarkEpoch = playHighWaterMark,
            lastVerifiedAtEpoch = playLastVerifiedAt
        ) else null

        val hasEw = securePrefs.getBoolean(KEY_ENGINE_OIC_EW_UNLOCKED, false)
        val ewPurchase = securePrefs.getLong("play_purchase_time_engine_ew", if (purchaseTime > 0L) purchaseTime else now)
        val ewExpiry = securePrefs.getLong("play_expiry_time_engine_ew", engineEwCycle.nextBillingPeriod(ewPurchase))
        val engineEwPlan = if (hasEw) ActivePlanInfo(
            cycle = engineEwCycle,
            purchaseToken = securePrefs.getString(KEY_ENGINE_OIC_EW_TOKEN, null),
            purchaseTimestamp = ewPurchase,
            expiryTimestamp = ewExpiry,
            highWaterMarkEpoch = playHighWaterMark,
            lastVerifiedAtEpoch = playLastVerifiedAt
        ) else null

        val hasDm = securePrefs.getBoolean(KEY_DECK_MGT_UNLOCKED, false)
        val dmPurchase = securePrefs.getLong("play_purchase_time_deck_mgt", if (purchaseTime > 0L) purchaseTime else now)
        val dmExpiry = securePrefs.getLong("play_expiry_time_deck_mgt", deckMgtCycle.nextBillingPeriod(dmPurchase))
        val deckMgtPlan = if (hasDm) ActivePlanInfo(
            cycle = deckMgtCycle,
            purchaseToken = securePrefs.getString(KEY_DECK_MGT_TOKEN, null),
            purchaseTimestamp = dmPurchase,
            expiryTimestamp = dmExpiry,
            highWaterMarkEpoch = playHighWaterMark,
            lastVerifiedAtEpoch = playLastVerifiedAt
        ) else null

        val hasEm = securePrefs.getBoolean(KEY_ENGINE_MGT_UNLOCKED, false)
        val emPurchase = securePrefs.getLong("play_purchase_time_engine_mgt", if (purchaseTime > 0L) purchaseTime else now)
        val emExpiry = securePrefs.getLong("play_expiry_time_engine_mgt", engineMgtCycle.nextBillingPeriod(emPurchase))
        val engineMgtPlan = if (hasEm) ActivePlanInfo(
            cycle = engineMgtCycle,
            purchaseToken = securePrefs.getString(KEY_ENGINE_MGT_TOKEN, null),
            purchaseTimestamp = emPurchase,
            expiryTimestamp = emExpiry,
            highWaterMarkEpoch = playHighWaterMark,
            lastVerifiedAtEpoch = playLastVerifiedAt
        ) else null

        val hasOicActive = oicPlan != null && !oicPlan.isExpired
        val hasGmdssActive = gmdssPlan != null && !gmdssPlan.isExpired
        val hasAllActive = allAccessPlan != null && !allAccessPlan.isExpired
        val hasEwActive = engineEwPlan != null && !engineEwPlan.isExpired
        val hasDmActive = deckMgtPlan != null && !deckMgtPlan.isExpired
        val hasEmActive = engineMgtPlan != null && !engineMgtPlan.isExpired

        // Migration-Safe Entitlement Loading: Re-sign hash if valid plans exist rather than wiping cache
        val anyActive = hasOicActive || hasGmdssActive || hasAllActive || hasEwActive || hasDmActive || hasEmActive
        val expectedHash = computeIntegrityChecksum(hasOicActive, hasGmdssActive, hasAllActive, hasEwActive, hasDmActive, hasEmActive)
        
        // Auto-heal integrity hash on app version upgrades
        securePrefs.edit().putString(KEY_SIGNATURE_HASH, expectedHash).apply()

        _entitlements.update {
            it.copy(
                oicNwPlan = if (hasOicActive) oicPlan else null,
                gmdssPlan = if (hasGmdssActive) gmdssPlan else null,
                allAccessPlan = if (hasAllActive) allAccessPlan else null,
                engineOicEwPlan = if (hasEwActive) engineEwPlan else null,
                deckMgtPlan = if (hasDmActive) deckMgtPlan else null,
                engineMgtPlan = if (hasEmActive) engineMgtPlan else null,
                cachedPurchaseTime = purchaseTime,
                cachedBasePlan = basePlan
            )
        }
    }

    private fun computeIntegrityChecksum(oic: Boolean, gmdss: Boolean, all: Boolean, ew: Boolean, dm: Boolean, em: Boolean): String {
        val raw = "HYP_PLAY_ENT_${appContext.packageName}_${oic}_${gmdss}_${all}_${ew}_${dm}_${em}_SEA_SAFE"
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun getOfferToken(productId: String, cycle: BillingCycle = BillingCycle.YEARLY): String? {
        val details = _productsDetails.value[productId] ?: return null
        val offers = details.subscriptionOfferDetails ?: return null

        val matchedOffer = offers.firstOrNull { offer ->
            cycle.matchesPlayOffer(
                basePlanId = offer.basePlanId,
                billingPeriods = offer.pricingPhases.pricingPhaseList.map { it.billingPeriod }
            )
        }

        // Never silently fall back to a different base plan (e.g. yearly when the
        // user explicitly selected weekly). The caller will show an unavailable-plan
        // message instead of charging for a different duration.
        return matchedOffer?.offerToken
    }

    fun launchBillingFlow(
        activity: Activity,
        productId: String,
        cycle: BillingCycle = BillingCycle.YEARLY
    ): BillingResult {
        lastRequestedCycles[productId] = cycle
        val cycleKey = when (productId) {
            BillingConstants.PRODUCT_OIC_NW_PASS -> "play_plan_cycle_oic"
            BillingConstants.PRODUCT_GMDSS_PASS -> "play_plan_cycle_gmdss"
            BillingConstants.PRODUCT_ALL_ACCESS_PASS -> "play_plan_cycle_all_access"
            BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> "play_plan_cycle_engine_ew"
            BillingConstants.PRODUCT_DECK_MGT_PASS -> "play_plan_cycle_deck_mgt"
            BillingConstants.PRODUCT_ENGINE_MGT_PASS -> "play_plan_cycle_engine_mgt"
            else -> "play_plan_cycle_default"
        }
        try {
            securePrefs.edit().putString("${cycleKey}_pending", cycle.name).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist pending duration selection: ${e.message}")
        }

        val details = _productsDetails.value[productId]
        if (details == null) {
            clearPendingBillingCycle(productId)
            Log.w(TAG, "ProductDetails for $productId not loaded yet. Attempting fresh query.")
            scope.launch { queryProductDetails() }
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                .setDebugMessage("Subscription details not available yet. Please check your connection.")
                .build()
        }

        val offerToken = getOfferToken(productId, cycle)
        if (offerToken.isNullOrBlank()) {
            clearPendingBillingCycle(productId)
            Log.e(TAG, "No Google Play offer matched the selected cycle: product=$productId, cycle=$cycle")
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                .setDebugMessage("The selected subscription duration is unavailable. Refresh the store and choose another available plan.")
                .build()
        }

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        if (!offerToken.isNullOrBlank()) {
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }

        val productDetailsParamsList = listOf(productDetailsParamsBuilder.build())

        val flowBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)

        // Bind this new Play purchase to the signed-in Hyperion account when available.
        // Sign-in is optional; users without an app account can still purchase normally.
        obfuscatedHyperionAccountId(LicenseManager.getCurrentAccountUserId(appContext))
            ?.let(flowBuilder::setObfuscatedAccountId)

        val flowParams = flowBuilder.build()

        checkoutFlowProductId = productId
        _entitlements.update { it.copy(pendingPurchaseProductId = productId, errorMessage = null) }
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            clearPendingBillingCycle(productId)
            _entitlements.update {
                it.copy(
                    pendingPurchaseProductId = null,
                    errorMessage = result.debugMessage.ifBlank { "Google Play could not start this purchase." }
                )
            }
        }
        return result
    }

    fun getProductItem(productId: String, cycle: BillingCycle = BillingCycle.YEARLY): PlayProductItem {
        val details = _productsDetails.value[productId]
        val ent = _entitlements.value

        val isOwned = when (productId) {
            BillingConstants.PRODUCT_ALL_ACCESS_PASS -> ent.allAccessUnlocked || (ent.oicNwUnlocked && ent.gmdssUnlocked && ent.engineOicEwUnlocked && ent.deckMgtUnlocked && ent.engineMgtUnlocked)
            BillingConstants.PRODUCT_OIC_NW_PASS -> ent.oicNwUnlocked || ent.allAccessUnlocked
            BillingConstants.PRODUCT_GMDSS_PASS -> ent.gmdssUnlocked || ent.allAccessUnlocked
            BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> ent.engineOicEwUnlocked || ent.allAccessUnlocked
            BillingConstants.PRODUCT_DECK_MGT_PASS -> ent.deckMgtUnlocked || ent.allAccessUnlocked
            BillingConstants.PRODUCT_ENGINE_MGT_PASS -> ent.engineMgtUnlocked || ent.allAccessUnlocked
            else -> false
        }

        val isPending = ent.pendingPurchaseProductId == productId

        val matchedOffer = details?.subscriptionOfferDetails?.firstOrNull { offer ->
            cycle.matchesPlayOffer(
                basePlanId = offer.basePlanId,
                billingPeriods = offer.pricingPhases.pricingPhaseList.map { it.billingPeriod }
            )
        }

        // The last phase is the ongoing recurring base-plan price. Earlier
        // phases can be free trials or introductory prices and are not the renewal price.
        val subPrice = matchedOffer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
        val fallbackPrice = BillingConstants.getFallbackPrice(productId, cycle)
        val fallbackShort = BillingConstants.getFallbackShortPrice(productId, cycle)

        val formattedPrice = subPrice
            ?: details?.oneTimePurchaseOfferDetails?.formattedPrice
            ?: if (isOwned) "ACTIVE" else fallbackPrice

        val shortPrice = subPrice ?: fallbackShort

        val title = details?.title?.substringBefore(" (") ?: BillingConstants.PRODUCT_NAMES[productId] ?: productId
        val desc = details?.description ?: BillingConstants.PRODUCT_DESCRIPTIONS[productId] ?: ""

        val savings = when (cycle) {
            BillingCycle.YEARLY -> 48
            BillingCycle.MONTHLY -> 20
            BillingCycle.WEEKLY -> null
        }

        return PlayProductItem(
            productId = productId,
            title = title,
            description = desc,
            formattedPrice = formattedPrice,
            shortFormattedPrice = shortPrice,
            isOwned = isOwned,
            isPending = isPending,
            selectedCycle = cycle,
            savingsPercentage = savings,
            rawProductDetails = details
        )
    }

    fun getAllProductItems(cycle: BillingCycle = BillingCycle.YEARLY): List<PlayProductItem> {
        return BillingConstants.ALL_PRODUCT_IDS.map { getProductItem(it, cycle) }
    }

    fun getProductDetails(productId: String): ProductDetails? {
        return _productsDetails.value[productId]
    }

    /**
     * Simulates a successful subscription purchase for testing, browser emulator, and sandbox previews
     * where live Google Play Billing accounts cannot complete transactions.
     * Guarded strictly for DEBUG builds.
     */
    fun simulatePurchaseForTesting(productId: String, cycle: BillingCycle = BillingCycle.YEARLY) {
        if (!BuildConfig.DEBUG) {
            Log.w(TAG, "Sandbox purchase simulation is strictly forbidden in RELEASE builds.")
            return
        }
        lastRequestedCycles[productId] = cycle
        val now = System.currentTimeMillis()
        val expiryTime = cycle.nextBillingPeriod(now)
        val mockToken = "mock_sandbox_sub_token_${productId}_$now"
        val editor = securePrefs.edit()

        var oicPlan = _entitlements.value.oicNwPlan
        var gmdssPlan = _entitlements.value.gmdssPlan
        var allAccessPlan = _entitlements.value.allAccessPlan
        var engineOicEwPlan = _entitlements.value.engineOicEwPlan
        var deckMgtPlan = _entitlements.value.deckMgtPlan
        var engineMgtPlan = _entitlements.value.engineMgtPlan

        when (productId) {
            BillingConstants.PRODUCT_OIC_NW_PASS -> {
                oicPlan = ActivePlanInfo(
                    cycle = cycle,
                    purchaseToken = mockToken,
                    purchaseTimestamp = now,
                    expiryTimestamp = expiryTime
                )
                editor.putBoolean(KEY_OIC_NW_UNLOCKED, true)
                editor.putString("play_plan_cycle_oic", cycle.name)
                editor.putString(KEY_OIC_TOKEN, mockToken)
                editor.putLong("play_purchase_time_oic", now)
                editor.putLong("play_expiry_time_oic", expiryTime)
            }
            BillingConstants.PRODUCT_GMDSS_PASS -> {
                gmdssPlan = ActivePlanInfo(
                    cycle = cycle,
                    purchaseToken = mockToken,
                    purchaseTimestamp = now,
                    expiryTimestamp = expiryTime
                )
                editor.putBoolean(KEY_GMDSS_UNLOCKED, true)
                editor.putString("play_plan_cycle_gmdss", cycle.name)
                editor.putString(KEY_GMDSS_TOKEN, mockToken)
                editor.putLong("play_purchase_time_gmdss", now)
                editor.putLong("play_expiry_time_gmdss", expiryTime)
            }
            BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> {
                engineOicEwPlan = ActivePlanInfo(
                    cycle = cycle,
                    purchaseToken = mockToken,
                    purchaseTimestamp = now,
                    expiryTimestamp = expiryTime
                )
                editor.putBoolean(KEY_ENGINE_OIC_EW_UNLOCKED, true)
                editor.putString("play_plan_cycle_engine_ew", cycle.name)
                editor.putString(KEY_ENGINE_OIC_EW_TOKEN, mockToken)
                editor.putLong("play_purchase_time_engine_ew", now)
                editor.putLong("play_expiry_time_engine_ew", expiryTime)
            }
            BillingConstants.PRODUCT_DECK_MGT_PASS -> {
                deckMgtPlan = ActivePlanInfo(
                    cycle = cycle,
                    purchaseToken = mockToken,
                    purchaseTimestamp = now,
                    expiryTimestamp = expiryTime
                )
                editor.putBoolean(KEY_DECK_MGT_UNLOCKED, true)
                editor.putString("play_plan_cycle_deck_mgt", cycle.name)
                editor.putString(KEY_DECK_MGT_TOKEN, mockToken)
                editor.putLong("play_purchase_time_deck_mgt", now)
                editor.putLong("play_expiry_time_deck_mgt", expiryTime)
            }
            BillingConstants.PRODUCT_ENGINE_MGT_PASS -> {
                engineMgtPlan = ActivePlanInfo(
                    cycle = cycle,
                    purchaseToken = mockToken,
                    purchaseTimestamp = now,
                    expiryTimestamp = expiryTime
                )
                editor.putBoolean(KEY_ENGINE_MGT_UNLOCKED, true)
                editor.putString("play_plan_cycle_engine_mgt", cycle.name)
                editor.putString(KEY_ENGINE_MGT_TOKEN, mockToken)
                editor.putLong("play_purchase_time_engine_mgt", now)
                editor.putLong("play_expiry_time_engine_mgt", expiryTime)
            }
            BillingConstants.PRODUCT_ALL_ACCESS_PASS -> {
                allAccessPlan = ActivePlanInfo(
                    cycle = cycle,
                    purchaseToken = mockToken,
                    purchaseTimestamp = now,
                    expiryTimestamp = expiryTime
                )
                oicPlan = ActivePlanInfo(
                    cycle = cycle,
                    purchaseToken = mockToken,
                    purchaseTimestamp = now,
                    expiryTimestamp = expiryTime
                )
                gmdssPlan = ActivePlanInfo(
                    cycle = cycle,
                    purchaseToken = mockToken,
                    purchaseTimestamp = now,
                    expiryTimestamp = expiryTime
                )
                engineOicEwPlan = ActivePlanInfo(
                    cycle = cycle,
                    purchaseToken = mockToken,
                    purchaseTimestamp = now,
                    expiryTimestamp = expiryTime
                )
                deckMgtPlan = ActivePlanInfo(
                    cycle = cycle,
                    purchaseToken = mockToken,
                    purchaseTimestamp = now,
                    expiryTimestamp = expiryTime
                )
                engineMgtPlan = ActivePlanInfo(
                    cycle = cycle,
                    purchaseToken = mockToken,
                    purchaseTimestamp = now,
                    expiryTimestamp = expiryTime
                )
                editor.putBoolean(KEY_ALL_ACCESS_UNLOCKED, true)
                editor.putBoolean(KEY_OIC_NW_UNLOCKED, true)
                editor.putBoolean(KEY_GMDSS_UNLOCKED, true)
                editor.putBoolean(KEY_ENGINE_OIC_EW_UNLOCKED, true)
                editor.putBoolean(KEY_DECK_MGT_UNLOCKED, true)
                editor.putBoolean(KEY_ENGINE_MGT_UNLOCKED, true)
                editor.putString("play_plan_cycle_all_access", cycle.name)
                editor.putString("play_plan_cycle_oic", cycle.name)
                editor.putString("play_plan_cycle_gmdss", cycle.name)
                editor.putString("play_plan_cycle_engine_ew", cycle.name)
                editor.putString("play_plan_cycle_deck_mgt", cycle.name)
                editor.putString("play_plan_cycle_engine_mgt", cycle.name)
                editor.putString(KEY_ALL_ACCESS_TOKEN, mockToken)
                editor.putString(KEY_OIC_TOKEN, mockToken)
                editor.putString(KEY_GMDSS_TOKEN, mockToken)
                editor.putString(KEY_ENGINE_OIC_EW_TOKEN, mockToken)
                editor.putString(KEY_DECK_MGT_TOKEN, mockToken)
                editor.putString(KEY_ENGINE_MGT_TOKEN, mockToken)
                editor.putLong("play_purchase_time_all_access", now)
                editor.putLong("play_expiry_time_all_access", expiryTime)
                editor.putLong("play_purchase_time_oic", now)
                editor.putLong("play_expiry_time_oic", expiryTime)
                editor.putLong("play_purchase_time_gmdss", now)
                editor.putLong("play_expiry_time_gmdss", expiryTime)
                editor.putLong("play_purchase_time_engine_ew", now)
                editor.putLong("play_expiry_time_engine_ew", expiryTime)
                editor.putLong("play_purchase_time_deck_mgt", now)
                editor.putLong("play_expiry_time_deck_mgt", expiryTime)
                editor.putLong("play_purchase_time_engine_mgt", now)
                editor.putLong("play_expiry_time_engine_mgt", expiryTime)
            }
        }

        editor.putLong(KEY_PURCHASE_TIME, now)
        val hasOic = oicPlan != null && !oicPlan.isExpired
        val hasGmdss = gmdssPlan != null && !gmdssPlan.isExpired
        val hasAll = allAccessPlan != null && !allAccessPlan.isExpired
        val hasEw = engineOicEwPlan != null && !engineOicEwPlan.isExpired
        val hasDm = deckMgtPlan != null && !deckMgtPlan.isExpired
        val hasEm = engineMgtPlan != null && !engineMgtPlan.isExpired

        val signature = computeIntegrityChecksum(hasOic, hasGmdss, hasAll, hasEw, hasDm, hasEm)
        editor.putString(KEY_SIGNATURE_HASH, signature)
        editor.apply()

        _entitlements.update {
            it.copy(
                oicNwPlan = oicPlan,
                gmdssPlan = gmdssPlan,
                allAccessPlan = allAccessPlan,
                engineOicEwPlan = engineOicEwPlan,
                deckMgtPlan = deckMgtPlan,
                engineMgtPlan = engineMgtPlan,
                pendingPurchaseProductId = null,
                errorMessage = null,
                cachedPurchaseTime = now
            )
        }
    }
}
