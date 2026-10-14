package com.nhyle.hyperionmaritime.data.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
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
import com.nhyle.hyperionmaritime.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Subscription billing cycles supported by Hyperion Maritime Exam Reviewer.
 */
enum class BillingCycle(val label: String, val shortLabel: String, val durationDays: Int) {
    WEEKLY("Weekly", "wk", 7),
    MONTHLY("Monthly", "mo", 30),
    YEARLY("Yearly (Save ~48%)", "yr", 365);

    val durationMs: Long get() = durationDays.toLong() * 24L * 60L * 60L * 1000L
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
    val highWaterMarkEpoch: Long = 0L
) {
    // Google Play Subscriptions are server-authoritative and never expire based on local device clock.
    val isExpired: Boolean
        get() = false

    val effectiveNow: Long
        get() = maxOf(System.currentTimeMillis(), purchaseTimestamp, highWaterMarkEpoch)

    val remainingTimeMs: Long
        get() = if (isTerminalLicense) Long.MAX_VALUE else (expiryTimestamp - effectiveNow).coerceAtLeast(0L)

    fun getFormattedTimeRemaining(): String {
        if (isTerminalLicense) return "Active Plan"
        if (isExpired) return "Subscription Expired"
        val remainingMs = remainingTimeMs
        val totalSecs = remainingMs / 1000L
        val totalMins = totalSecs / 60L
        val totalHours = totalMins / 60L
        // Ceil rounding for remaining days (e.g. 6 days 23 hours = 7 days remaining)
        val days = (totalHours + 23L) / 24L
        val hours = totalHours % 24L
        val mins = totalMins % 60L

        return when {
            days > 1 -> "$days days remaining"
            totalHours > 1 -> "$totalHours hrs, $mins mins remaining"
            totalHours == 1L -> "1 hr, $mins mins remaining"
            mins > 0 -> "$mins mins remaining"
            else -> "< 1 min remaining"
        }
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
        if (isExpired) return "EXPIRED"
        return "${getFormattedTimeRemaining().uppercase()} • ${getFormattedExpiryDate().uppercase()}"
    }
}

/**
 * Standard Product IDs, Base Plans, and fallback metadata for Hyperion Google Play Subscriptions.
 * Converted to ProductType.SUBS (Auto-Renewing Subscriptions across Weekly, Monthly, Yearly cycles).
 */
object BillingConstants {
    const val PRODUCT_OIC_NW_PASS = "hyperion_oic_nw_pass"
    const val PRODUCT_GMDSS_PASS = "hyperion_gmdss_pass"
    const val PRODUCT_ALL_ACCESS_PASS = "hyperion_all_access_pass"

    val ALL_PRODUCT_IDS = listOf(
        PRODUCT_OIC_NW_PASS,
        PRODUCT_GMDSS_PASS,
        PRODUCT_ALL_ACCESS_PASS
    )

    // Base Plan IDs mapped in Google Play Console
    const val BASE_PLAN_OIC_WEEKLY = "oic-nw-weekly"
    const val BASE_PLAN_OIC_MONTHLY = "oic-nw-monthly"
    const val BASE_PLAN_OIC_YEARLY = "oic-nw-yearly"

    const val BASE_PLAN_GMDSS_WEEKLY = "gmdss-weekly"
    const val BASE_PLAN_GMDSS_MONTHLY = "gmdss-monthly"
    const val BASE_PLAN_GMDSS_YEARLY = "gmdss-yearly"

    const val BASE_PLAN_ALL_ACCESS_WEEKLY = "all-access-weekly"
    const val BASE_PLAN_ALL_ACCESS_MONTHLY = "all-access-monthly"
    const val BASE_PLAN_ALL_ACCESS_YEARLY = "all-access-yearly"

    // Standard Fallback Display Prices across billing cycles
    val DEFAULT_PRICES_YEARLY = mapOf(
        PRODUCT_OIC_NW_PASS to "₱350 / year",
        PRODUCT_GMDSS_PASS to "₱250 / year",
        PRODUCT_ALL_ACCESS_PASS to "₱500 / year"
    )

    val DEFAULT_PRICES_MONTHLY = mapOf(
        PRODUCT_OIC_NW_PASS to "₱50 / month",
        PRODUCT_GMDSS_PASS to "₱40 / month",
        PRODUCT_ALL_ACCESS_PASS to "₱80 / month"
    )

    val DEFAULT_PRICES_WEEKLY = mapOf(
        PRODUCT_OIC_NW_PASS to "₱20 / week",
        PRODUCT_GMDSS_PASS to "₱15 / week",
        PRODUCT_ALL_ACCESS_PASS to "₱35 / week"
    )

    // Backward-compatible default price map (Yearly)
    val DEFAULT_PRICES = DEFAULT_PRICES_YEARLY

    val DEFAULT_SHORT_PRICES_YEARLY = mapOf(
        PRODUCT_OIC_NW_PASS to "₱350/yr",
        PRODUCT_GMDSS_PASS to "₱250/yr",
        PRODUCT_ALL_ACCESS_PASS to "₱500/yr"
    )

    val DEFAULT_SHORT_PRICES_MONTHLY = mapOf(
        PRODUCT_OIC_NW_PASS to "₱50/mo",
        PRODUCT_GMDSS_PASS to "₱40/mo",
        PRODUCT_ALL_ACCESS_PASS to "₱80/mo"
    )

    val DEFAULT_SHORT_PRICES_WEEKLY = mapOf(
        PRODUCT_OIC_NW_PASS to "₱20/wk",
        PRODUCT_GMDSS_PASS to "₱15/wk",
        PRODUCT_ALL_ACCESS_PASS to "₱35/wk"
    )

    fun getFallbackPrice(productId: String, cycle: BillingCycle = BillingCycle.YEARLY): String = when (cycle) {
        BillingCycle.WEEKLY -> DEFAULT_PRICES_WEEKLY[productId] ?: "₱35 / week"
        BillingCycle.MONTHLY -> DEFAULT_PRICES_MONTHLY[productId] ?: "₱80 / month"
        BillingCycle.YEARLY -> DEFAULT_PRICES_YEARLY[productId] ?: "₱500 / year"
    }

    fun getFallbackShortPrice(productId: String, cycle: BillingCycle = BillingCycle.YEARLY): String = when (cycle) {
        BillingCycle.WEEKLY -> DEFAULT_SHORT_PRICES_WEEKLY[productId] ?: "₱35/wk"
        BillingCycle.MONTHLY -> DEFAULT_SHORT_PRICES_MONTHLY[productId] ?: "₱80/mo"
        BillingCycle.YEARLY -> DEFAULT_SHORT_PRICES_YEARLY[productId] ?: "₱500/yr"
    }

    val PRODUCT_NAMES = mapOf(
        PRODUCT_OIC_NW_PASS to "Deck OIC-NW Pass",
        PRODUCT_GMDSS_PASS to "GMDSS GOC Pass",
        PRODUCT_ALL_ACCESS_PASS to "Hyperion All-Access Pass"
    )

    val PRODUCT_DESCRIPTIONS = mapOf(
        PRODUCT_OIC_NW_PASS to "Unlocks all 12 competencies, full question bank (6,445 items), rationale, and mock exams for Deck OIC-NW.",
        PRODUCT_GMDSS_PASS to "Unlocks all GMDSS competencies, radio communications bank (2,260 items), and mock exams.",
        PRODUCT_ALL_ACCESS_PASS to "Unlocks both Deck OIC-NW and GMDSS GOC tracks with full rationale and offline access."
    )
}

/**
 * State representation of active Google Play entitlements with active plan metadata.
 */
data class TrackEntitlements(
    val oicNwPlan: ActivePlanInfo? = null,
    val gmdssPlan: ActivePlanInfo? = null,
    val allAccessPlan: ActivePlanInfo? = null,
    val billingConnected: Boolean = false,
    val pendingPurchaseProductId: String? = null,
    val errorMessage: String? = null,
    val cachedBasePlan: String? = null,
    val cachedPurchaseTime: Long = 0L,
    val isClockTampered: Boolean = false,
    private val explicitOicNwUnlocked: Boolean? = null,
    private val explicitGmdssUnlocked: Boolean? = null,
    private val explicitAllAccessUnlocked: Boolean? = null
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
                (oicNwPlan != null && !oicNwPlan.isExpired && gmdssPlan != null && !gmdssPlan.isExpired)

    fun isTrackUnlocked(track: Track): Boolean {
        if (allAccessUnlocked) return true
        return when (track) {
            Track.DECK_OIC_NW -> oicNwUnlocked
            Track.GMDSS_GOC -> gmdssUnlocked
            else -> false
        }
    }

    fun getActivePlanForTrack(track: Track): ActivePlanInfo? {
        if (allAccessPlan != null && !allAccessPlan.isExpired) return allAccessPlan
        return when (track) {
            Track.DECK_OIC_NW -> if (oicNwPlan != null && !oicNwPlan.isExpired) oicNwPlan else null
            Track.GMDSS_GOC -> if (gmdssPlan != null && !gmdssPlan.isExpired) gmdssPlan else null
            else -> null
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

        private const val KEY_OIC_TOKEN = "play_token_oic_nw"
        private const val KEY_GMDSS_TOKEN = "play_token_gmdss"
        private const val KEY_ALL_ACCESS_TOKEN = "play_token_all_access"

        private const val KEY_PURCHASE_TIME = "play_purchase_timestamp"
        private const val KEY_BASE_PLAN = "play_base_plan_id"
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

        fun cycleFromBasePlanOrPeriod(basePlanId: String?, period: String?): BillingCycle {
            val base = basePlanId?.lowercase() ?: ""
            val per = period?.uppercase() ?: ""
            return when {
                base.contains("yearly") || per == "P1Y" || per == "P365D" || per == "P12M" -> BillingCycle.YEARLY
                base.contains("monthly") || per == "P1M" || per == "P30D" -> BillingCycle.MONTHLY
                base.contains("weekly") || per == "P1W" || per == "P7D" -> BillingCycle.WEEKLY
                else -> BillingCycle.MONTHLY
            }
        }
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

    suspend fun queryPurchasesAsync() = withContext(Dispatchers.IO) {
        if (!billingClient.isReady) {
            Log.w(TAG, "BillingClient not ready for queryPurchasesAsync")
            return@withContext
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        try {
            val result = billingClient.queryPurchasesAsync(params)
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(result.purchasesList)
            } else {
                Log.w(TAG, "queryPurchasesAsync response not OK: ${result.billingResult.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception restoring purchases: ${e.message}")
        }
    }

    fun queryPurchasesOnResume() {
        scope.launch {
            if (billingClient.isReady) {
                queryPurchasesAsync()
            } else {
                startConnection()
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        scope.launch {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    _entitlements.update { it.copy(pendingPurchaseProductId = null, errorMessage = null) }
                    purchases?.let { processPurchases(it) }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    Log.i(TAG, "Purchase cancelled by user")
                    _entitlements.update { it.copy(pendingPurchaseProductId = null, errorMessage = null) }
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    Log.i(TAG, "Item already owned. Re-querying purchases to restore entitlement.")
                    _entitlements.update { it.copy(pendingPurchaseProductId = null, errorMessage = null) }
                    queryPurchasesAsync()
                }
                else -> {
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

    private suspend fun processPurchases(purchases: List<Purchase>) = withContext(Dispatchers.IO) {
        var oicPlan: ActivePlanInfo? = null
        var gmdssPlan: ActivePlanInfo? = null
        var allAccessPlan: ActivePlanInfo? = null
        var pendingProduct: String? = null
        var lastPurchaseTime = 0L
        var lastBasePlan: String? = null

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
                            else -> "play_plan_cycle_default"
                        }
                        val requestedCycle = lastRequestedCycles[productId]
                            ?: securePrefs.getString(cycleKey, null)
                                ?.let { runCatching { BillingCycle.valueOf(it) }.getOrNull() }

                        // If no cycle explicitly requested or if it was defaulted to WEEKLY from legacy bug, prefer MONTHLY
                        val effectiveCycle = when {
                            requestedCycle == BillingCycle.YEARLY -> BillingCycle.YEARLY
                            requestedCycle == BillingCycle.MONTHLY -> BillingCycle.MONTHLY
                            requestedCycle == BillingCycle.WEEKLY && lastRequestedCycles.containsKey(productId) -> BillingCycle.WEEKLY
                            else -> BillingCycle.MONTHLY
                        }

                        val offer = details?.subscriptionOfferDetails?.firstOrNull { offer ->
                            val basePlan = offer.basePlanId.lowercase()
                            val phases = offer.pricingPhases.pricingPhaseList
                            val period = phases.firstOrNull()?.billingPeriod?.uppercase() ?: ""
                            when (effectiveCycle) {
                                BillingCycle.WEEKLY -> basePlan.contains("weekly") || period == "P1W" || period == "P7D"
                                BillingCycle.MONTHLY -> basePlan.contains("monthly") || period == "P1M" || period == "P30D"
                                BillingCycle.YEARLY -> basePlan.contains("yearly") || period == "P1Y" || period == "P365D" || period == "P12M"
                            }
                        } ?: details?.subscriptionOfferDetails?.firstOrNull()

                        val cycle = effectiveCycle
                        val cycleDurationMs = cycle.durationMs
                        val purchaseTime = if (purchase.purchaseTime > 0L) purchase.purchaseTime else System.currentTimeMillis()
                        val now = System.currentTimeMillis()
                        val savedWaterMark = securePrefs.getLong("play_license_high_water_mark", 0L)
                        val currentWaterMark = maxOf(savedWaterMark, purchaseTime, now)

                        // A subscription returning PurchaseState.PURCHASED from Google Play is actively entitled.
                        // Advance expiration forward across recurring cycles so users are never locked out while active in Google Play.
                        var expiryTime = purchaseTime + cycleDurationMs
                        while (expiryTime <= now && cycleDurationMs > 0L) {
                            expiryTime += cycleDurationMs
                        }

                        when (productId) {
                            BillingConstants.PRODUCT_OIC_NW_PASS -> {
                                oicPlan = ActivePlanInfo(
                                    cycle = cycle,
                                    purchaseToken = purchase.purchaseToken,
                                    purchaseTimestamp = purchaseTime,
                                    expiryTimestamp = expiryTime,
                                    highWaterMarkEpoch = currentWaterMark
                                )
                            }
                            BillingConstants.PRODUCT_GMDSS_PASS -> {
                                gmdssPlan = ActivePlanInfo(
                                    cycle = cycle,
                                    purchaseToken = purchase.purchaseToken,
                                    purchaseTimestamp = purchaseTime,
                                    expiryTimestamp = expiryTime,
                                    highWaterMarkEpoch = currentWaterMark
                                )
                            }
                            BillingConstants.PRODUCT_ALL_ACCESS_PASS -> {
                                val plan = ActivePlanInfo(
                                    cycle = cycle,
                                    purchaseToken = purchase.purchaseToken,
                                    purchaseTimestamp = purchaseTime,
                                    expiryTimestamp = expiryTime,
                                    highWaterMarkEpoch = currentWaterMark
                                )
                                allAccessPlan = plan
                                oicPlan = plan
                                gmdssPlan = plan
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

        val savedWaterMark = securePrefs.getLong("play_license_high_water_mark", 0L)
        val now = System.currentTimeMillis()
        val activeWaterMark = maxOf(savedWaterMark, lastPurchaseTime)
        val isClockTampered = activeWaterMark > 0L && now < (activeWaterMark - 60_000L)
        if (!isClockTampered) {
            editor.putLong("play_license_high_water_mark", maxOf(activeWaterMark, now))
        }

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

        val hasOic = oicPlan != null && !oicPlan.isExpired
        val hasGmdss = gmdssPlan != null && !gmdssPlan.isExpired
        val hasAll = allAccessPlan != null && !allAccessPlan.isExpired

        val signature = computeIntegrityChecksum(hasOic, hasGmdss, hasAll)
        editor.putString(KEY_SIGNATURE_HASH, signature)
        editor.putLong(KEY_PURCHASE_TIME, lastPurchaseTime)
        editor.apply()

        _entitlements.update {
            TrackEntitlements(
                oicNwPlan = oicPlan,
                gmdssPlan = gmdssPlan,
                allAccessPlan = allAccessPlan,
                billingConnected = true,
                pendingPurchaseProductId = pendingProduct,
                errorMessage = null,
                cachedBasePlan = lastBasePlan,
                cachedPurchaseTime = lastPurchaseTime,
                isClockTampered = isClockTampered
            )
        }
    }

    private fun migrateLegacyWeeklyFallbacks() {
        val migrationKey = "billing_migrated_weekly_fallback_v3"
        if (!securePrefs.getBoolean(migrationKey, false)) {
            val editor = securePrefs.edit()
            val oicCycle = securePrefs.getString("play_plan_cycle_oic", null)
            val oicToken = securePrefs.getString(KEY_OIC_TOKEN, null)
            val hasOic = securePrefs.getBoolean(KEY_OIC_NW_UNLOCKED, false)
            if (oicCycle == BillingCycle.WEEKLY.name || (hasOic && !oicToken.isNullOrBlank() && oicCycle == null)) {
                editor.putString("play_plan_cycle_oic", BillingCycle.MONTHLY.name)
                val purchaseTime = securePrefs.getLong("play_purchase_time_oic", 0L)
                if (purchaseTime > 0L) {
                    val correctedExpiry = purchaseTime + BillingCycle.MONTHLY.durationMs
                    editor.putLong("play_expiry_time_oic", correctedExpiry)
                }
            }

            val gmdssCycle = securePrefs.getString("play_plan_cycle_gmdss", null)
            val gmdssToken = securePrefs.getString(KEY_GMDSS_TOKEN, null)
            val hasGmdss = securePrefs.getBoolean(KEY_GMDSS_UNLOCKED, false)
            if (gmdssCycle == BillingCycle.WEEKLY.name || (hasGmdss && !gmdssToken.isNullOrBlank() && gmdssCycle == null)) {
                editor.putString("play_plan_cycle_gmdss", BillingCycle.MONTHLY.name)
                val purchaseTime = securePrefs.getLong("play_purchase_time_gmdss", 0L)
                if (purchaseTime > 0L) {
                    val correctedExpiry = purchaseTime + BillingCycle.MONTHLY.durationMs
                    editor.putLong("play_expiry_time_gmdss", correctedExpiry)
                }
            }

            val allAccessCycle = securePrefs.getString("play_plan_cycle_all_access", null)
            val allToken = securePrefs.getString(KEY_ALL_ACCESS_TOKEN, null)
            val hasAll = securePrefs.getBoolean(KEY_ALL_ACCESS_UNLOCKED, false)
            if (allAccessCycle == BillingCycle.WEEKLY.name || (hasAll && !allToken.isNullOrBlank() && allAccessCycle == null)) {
                editor.putString("play_plan_cycle_all_access", BillingCycle.MONTHLY.name)
                val purchaseTime = securePrefs.getLong("play_purchase_time_all_access", 0L)
                if (purchaseTime > 0L) {
                    val correctedExpiry = purchaseTime + BillingCycle.MONTHLY.durationMs
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

        val oicCycle = oicCycleName?.let { runCatching { BillingCycle.valueOf(it) }.getOrNull() } ?: BillingCycle.MONTHLY
        val gmdssCycle = gmdssCycleName?.let { runCatching { BillingCycle.valueOf(it) }.getOrNull() } ?: BillingCycle.MONTHLY
        val allAccessCycle = allAccessCycleName?.let { runCatching { BillingCycle.valueOf(it) }.getOrNull() } ?: BillingCycle.MONTHLY

        val now = System.currentTimeMillis()
        val oicPurchase = securePrefs.getLong("play_purchase_time_oic", if (purchaseTime > 0L) purchaseTime else now)
        var oicExpiry = securePrefs.getLong("play_expiry_time_oic", oicPurchase + oicCycle.durationMs)
        if (oic && oicExpiry < oicPurchase + oicCycle.durationMs) {
            oicExpiry = oicPurchase + oicCycle.durationMs
        }

        val gmdssPurchase = securePrefs.getLong("play_purchase_time_gmdss", if (purchaseTime > 0L) purchaseTime else now)
        var gmdssExpiry = securePrefs.getLong("play_expiry_time_gmdss", gmdssPurchase + gmdssCycle.durationMs)
        if (gmdss && gmdssExpiry < gmdssPurchase + gmdssCycle.durationMs) {
            gmdssExpiry = gmdssPurchase + gmdssCycle.durationMs
        }

        val allAccessPurchase = securePrefs.getLong("play_purchase_time_all_access", if (purchaseTime > 0L) purchaseTime else now)
        var allAccessExpiry = securePrefs.getLong("play_expiry_time_all_access", allAccessPurchase + allAccessCycle.durationMs)
        if (allAccess && allAccessExpiry < allAccessPurchase + allAccessCycle.durationMs) {
            allAccessExpiry = allAccessPurchase + allAccessCycle.durationMs
        }

        val playHighWaterMark = securePrefs.getLong("play_license_high_water_mark", 0L)

        val oicPlan = if (oic) ActivePlanInfo(
            cycle = oicCycle,
            purchaseToken = securePrefs.getString(KEY_OIC_TOKEN, null),
            purchaseTimestamp = oicPurchase,
            expiryTimestamp = oicExpiry,
            highWaterMarkEpoch = playHighWaterMark
        ) else null

        val gmdssPlan = if (gmdss) ActivePlanInfo(
            cycle = gmdssCycle,
            purchaseToken = securePrefs.getString(KEY_GMDSS_TOKEN, null),
            purchaseTimestamp = gmdssPurchase,
            expiryTimestamp = gmdssExpiry,
            highWaterMarkEpoch = playHighWaterMark
        ) else null

        val allAccessPlan = if (allAccess) ActivePlanInfo(
            cycle = allAccessCycle,
            purchaseToken = securePrefs.getString(KEY_ALL_ACCESS_TOKEN, null),
            purchaseTimestamp = allAccessPurchase,
            expiryTimestamp = allAccessExpiry,
            highWaterMarkEpoch = playHighWaterMark
        ) else null

        val hasOic = oicPlan != null && !oicPlan.isExpired
        val hasGmdss = gmdssPlan != null && !gmdssPlan.isExpired
        val hasAll = allAccessPlan != null && !allAccessPlan.isExpired

        val storedHash = securePrefs.getString(KEY_SIGNATURE_HASH, null)
        val expectedHash = computeIntegrityChecksum(hasOic, hasGmdss, hasAll)

        val isValid = storedHash == null || storedHash == expectedHash

        if (isValid) {
            _entitlements.update {
                it.copy(
                    oicNwPlan = if (hasOic) oicPlan else null,
                    gmdssPlan = if (hasGmdss) gmdssPlan else null,
                    allAccessPlan = if (hasAll) allAccessPlan else null,
                    cachedPurchaseTime = purchaseTime,
                    cachedBasePlan = basePlan
                )
            }
        } else {
            Log.w(TAG, "Cached entitlement hash integrity check failed. Clearing corrupted Play cache.")
            securePrefs.edit().clear().apply()
        }
    }

    private fun computeIntegrityChecksum(oic: Boolean, gmdss: Boolean, all: Boolean): String {
        val raw = "HYP_PLAY_ENT_${appContext.packageName}_${oic}_${gmdss}_${all}_SEA_SAFE"
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun getOfferToken(productId: String, cycle: BillingCycle = BillingCycle.YEARLY): String? {
        val details = _productsDetails.value[productId] ?: return null
        val offers = details.subscriptionOfferDetails ?: return null

        val matchedOffer = offers.firstOrNull { offer ->
            val basePlan = offer.basePlanId.lowercase()
            val phases = offer.pricingPhases.pricingPhaseList
            val period = phases.firstOrNull()?.billingPeriod?.uppercase() ?: ""

            when (cycle) {
                BillingCycle.WEEKLY -> basePlan.contains("weekly") || period == "P1W"
                BillingCycle.MONTHLY -> basePlan.contains("monthly") || period == "P1M"
                BillingCycle.YEARLY -> basePlan.contains("yearly") || period == "P1Y"
            }
        }

        return matchedOffer?.offerToken ?: offers.firstOrNull()?.offerToken
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
            else -> "play_plan_cycle_default"
        }
        try {
            securePrefs.edit().putString(cycleKey, cycle.name).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist requested cycle: ${e.message}")
        }

        val details = _productsDetails.value[productId]
        if (details == null) {
            Log.w(TAG, "ProductDetails for $productId not loaded yet. Attempting fresh query.")
            scope.launch { queryProductDetails() }
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                .setDebugMessage("Subscription details not available yet. Please check your connection.")
                .build()
        }

        val offerToken = getOfferToken(productId, cycle)

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        if (!offerToken.isNullOrBlank()) {
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }

        val productDetailsParamsList = listOf(productDetailsParamsBuilder.build())

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        _entitlements.update { it.copy(pendingPurchaseProductId = productId, errorMessage = null) }
        return billingClient.launchBillingFlow(activity, flowParams)
    }

    fun getProductItem(productId: String, cycle: BillingCycle = BillingCycle.YEARLY): PlayProductItem {
        val details = _productsDetails.value[productId]
        val ent = _entitlements.value

        val isOwned = when (productId) {
            BillingConstants.PRODUCT_ALL_ACCESS_PASS -> ent.allAccessUnlocked || (ent.oicNwUnlocked && ent.gmdssUnlocked)
            BillingConstants.PRODUCT_OIC_NW_PASS -> ent.oicNwUnlocked || ent.allAccessUnlocked
            BillingConstants.PRODUCT_GMDSS_PASS -> ent.gmdssUnlocked || ent.allAccessUnlocked
            else -> false
        }

        val isPending = ent.pendingPurchaseProductId == productId

        val matchedOffer = details?.subscriptionOfferDetails?.firstOrNull { offer ->
            val basePlan = offer.basePlanId.lowercase()
            val phases = offer.pricingPhases.pricingPhaseList
            val period = phases.firstOrNull()?.billingPeriod?.uppercase() ?: ""

            when (cycle) {
                BillingCycle.WEEKLY -> basePlan.contains("weekly") || period == "P1W"
                BillingCycle.MONTHLY -> basePlan.contains("monthly") || period == "P1M"
                BillingCycle.YEARLY -> basePlan.contains("yearly") || period == "P1Y"
            }
        } ?: details?.subscriptionOfferDetails?.firstOrNull()

        val subPrice = matchedOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
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
        val durationMs = cycle.durationMs
        val expiryTime = now + durationMs
        val mockToken = "mock_sandbox_sub_token_${productId}_$now"
        val editor = securePrefs.edit()

        var oicPlan = _entitlements.value.oicNwPlan
        var gmdssPlan = _entitlements.value.gmdssPlan
        var allAccessPlan = _entitlements.value.allAccessPlan

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
                editor.putBoolean(KEY_ALL_ACCESS_UNLOCKED, true)
                editor.putBoolean(KEY_OIC_NW_UNLOCKED, true)
                editor.putBoolean(KEY_GMDSS_UNLOCKED, true)
                editor.putString("play_plan_cycle_all_access", cycle.name)
                editor.putString("play_plan_cycle_oic", cycle.name)
                editor.putString("play_plan_cycle_gmdss", cycle.name)
                editor.putString(KEY_ALL_ACCESS_TOKEN, mockToken)
                editor.putString(KEY_OIC_TOKEN, mockToken)
                editor.putString(KEY_GMDSS_TOKEN, mockToken)
                editor.putLong("play_purchase_time_all_access", now)
                editor.putLong("play_expiry_time_all_access", expiryTime)
                editor.putLong("play_purchase_time_oic", now)
                editor.putLong("play_expiry_time_oic", expiryTime)
                editor.putLong("play_purchase_time_gmdss", now)
                editor.putLong("play_expiry_time_gmdss", expiryTime)
            }
        }

        editor.putLong(KEY_PURCHASE_TIME, now)
        val hasOic = oicPlan != null && !oicPlan.isExpired
        val hasGmdss = gmdssPlan != null && !gmdssPlan.isExpired
        val hasAll = allAccessPlan != null && !allAccessPlan.isExpired

        val signature = computeIntegrityChecksum(hasOic, hasGmdss, hasAll)
        editor.putString(KEY_SIGNATURE_HASH, signature)
        editor.apply()

        _entitlements.update {
            it.copy(
                oicNwPlan = oicPlan,
                gmdssPlan = gmdssPlan,
                allAccessPlan = allAccessPlan,
                pendingPurchaseProductId = null,
                errorMessage = null,
                cachedPurchaseTime = now
            )
        }
    }
}
