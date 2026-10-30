package com.nhyle.hyperionmaritime.data.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.os.SystemClock
import android.provider.Settings
import com.nhyle.hyperionmaritime.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ProTier(
    val title: String,
    val codePrefix: String,
    val subtitle: String,
    val shortName: String,
    val questionCount: String = ""
) {
    OIC_NW_PRO(
        title = "OIC-NW DECK (OPERATIONAL LEVEL)",
        codePrefix = "OIC",
        subtitle = "STCW Regulation II/1 • 12 Competencies, 200 items timed mock, full rationale",
        shortName = "Deck Operational",
        questionCount = "Total Question Bank: 6,445 questions"
    ),
    GMDSS_PRO(
        title = "GMDSS GOC (RADIO OPERATOR)",
        codePrefix = "GOC",
        subtitle = "STCW Regulation IV/2 • 2 Competencies, 100 items timed mock, full rationale",
        shortName = "GOC Radio Operator",
        questionCount = "Total Question Bank: 2,260 questions"
    ),
    HYPERION_PRO(
        title = "HYPERION ALL ACCESS",
        codePrefix = "ALL",
        subtitle = "Complete package: all 5 tracks (Deck OIC-NW, GMDSS GOC, Engine OIC-EW, Deck Management, Engine Management)",
        shortName = "All-Access Bundle",
        questionCount = "All 5 tracks, full question banks"
    ),
    ENGINE_OIC_EW_PRO(
        title = "OIC-EW ENGINE (OPERATIONAL LEVEL)",
        codePrefix = "EW",
        subtitle = "STCW Regulation III/1 • 13 Competencies, full rationale",
        shortName = "Engine Operational",
        questionCount = ""
    ),
    DECK_MGT_PRO(
        title = "DECK MANAGEMENT LEVEL",
        codePrefix = "DM",
        subtitle = "STCW Regulation II/2 • 18 Competencies, full rationale",
        shortName = "Deck Management",
        questionCount = ""
    ),
    ENGINE_MGT_PRO(
        title = "ENGINE MANAGEMENT LEVEL",
        codePrefix = "EM",
        subtitle = "STCW Regulation III/2 • 14 Competencies, full rationale",
        shortName = "Engine Management",
        questionCount = ""
    );

    companion object {
        val MASTER_PRO: ProTier get() = HYPERION_PRO
    }
}

enum class LicenseTier {
    OIC_NW,
    GMDSS,
    HYPERION_ALL_ACCESS,
    ENGINE_OIC_EW,
    DECK_MGT,
    ENGINE_MGT
}

enum class UnlockResult(val toastMessage: String, val isSuccess: Boolean) {
    TERMINAL_ACTIVATED("TERMINAL ACTIVATED · ACCESS GRANTED", true),
    OIC_NW_UNLOCKED("OIC-NW Deck Unlocked! Offline review access active.", true),
    GMDSS_UNLOCKED("GMDSS GOC Unlocked! Offline review access active.", true),
    HYPERION_UNLOCKED("Hyperion All Access Unlocked! Full offline access active.", true),
    ENGINE_OIC_EW_UNLOCKED("Engine OIC-EW Unlocked! Offline review access active.", true),
    DECK_MGT_UNLOCKED("Deck Management Unlocked! Offline review access active.", true),
    ENGINE_MGT_UNLOCKED("Engine Management Unlocked! Offline review access active.", true),
    CLOCK_ERROR("ERR: CLOCK TAMPERING DETECTED. RESTORE CORRECT SYSTEM TIME.", false),
    KEY_EXPIRED("ERR: ACTIVATION KEY HAS EXPIRED.", false),
    INVALID_KEY("ERR: INVALID OR UNRECOGNIZED ACTIVATION KEY", false);

    companion object {
        val MASTER_UNLOCKED: UnlockResult get() = TERMINAL_ACTIVATED
    }
}

data class OfflineLicenseInfo(
    val tier: String, // "ALL_ACCESS", "OIC_NW", "GMDSS_GOC"
    val duration: String, // "1W", "1M", "1Y"
    val activationEpoch: Long,
    val issueEpoch: Long,
    val expiryEpoch: Long,
    val isTampered: Boolean = false,
    val tamperReason: String? = null,
    val highWaterMarkEpoch: Long = 0L
) {
    val effectiveNow: Long
        get() = maxOf(System.currentTimeMillis(), activationEpoch, issueEpoch, highWaterMarkEpoch)

    val isExpired: Boolean
        get() = expiryEpoch > 0L && effectiveNow >= expiryEpoch

    val isActive: Boolean
        get() = !isTampered && !isExpired && expiryEpoch > 0L

    val remainingTimeMs: Long
        get() = (expiryEpoch - effectiveNow).coerceAtLeast(0L)

    val durationLabel: String
        get() = when (duration) {
            "1W" -> "1-Week Pass"
            "1M" -> "1-Month Pass"
            "1Y" -> "1-Year Pass"
            "EXP", "CUSTOM" -> "Custom Term Pass"
            else -> "Active Pass"
        }

    fun getFormattedTimeRemaining(): String {
        if (isTampered) return "Clock Tampered"
        if (isExpired) return "License Expired"
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
        if (expiryEpoch <= 0L) return "Active"
        val date = Date(expiryEpoch)
        val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return "Expires " + format.format(date)
    }

    fun getFullBadge(): String {
        if (isTampered) return "CLOCK TAMPERED · TIME RESTORATION REQUIRED"
        if (isExpired) return "OFFLINE LICENSE EXPIRED"
        return "${getFormattedTimeRemaining().uppercase()} • ${getFormattedExpiryDate().uppercase()}"
    }
}

sealed class OfflineLicenseStatus {
    object NotActive : OfflineLicenseStatus()
    data class Active(val info: OfflineLicenseInfo) : OfflineLicenseStatus()
    object Expired : OfflineLicenseStatus()
    data class ClockTampered(val reason: String) : OfflineLicenseStatus()
}

object LicenseManager {
    const val PREFS_NAME = "hyperion_license_prefs"
    const val SECURE_PREFS_NAME = "secure_license_prefs"

    private const val ACCOUNT_ENTITLEMENTS_PREFS = "hyperion_account_entitlements"
    private const val ACCOUNT_SYNCED_AT_KEY = "account_entitlements_synced_at"
    // Persist a high-water mark so changing the device clock backwards cannot extend
    // the 72-hour offline lease after the app has observed a later time.
    private const val ACCOUNT_LAST_VERIFIED_EPOCH_KEY = "account_entitlements_last_verified_epoch"
    private const val ACCOUNT_OWNER_USER_ID_KEY = "account_entitlements_owner_user_id"
    private const val ACCOUNT_CURRENT_USER_ID_KEY = "account_current_user_id"
    private const val ACCOUNT_EXPIRY_PREFIX = "account_entitlement_expiry_"
    private const val ACCOUNT_OFFLINE_GRACE_MS = 72L * 60L * 60L * 1000L
    @Volatile private var accountEntitlementPrefsCache: SharedPreferences? = null

    // Incremented whenever account grants are refreshed so screens such as Bond Store
    // can update even when the overall "any license active" Boolean remains unchanged.
    private val _accountEntitlementRevisionFlow = MutableStateFlow(0L)
    val accountEntitlementRevisionFlow: StateFlow<Long> = _accountEntitlementRevisionFlow.asStateFlow()

    const val KEY_OFFLINE_LICENSE = "is_offline_license"
    const val KEY_OFFLINE_TIER = "offline_license_tier"
    const val KEY_OFFLINE_DURATION = "offline_license_duration"
    const val KEY_OFFLINE_ACTIVATION_EPOCH = "offline_license_activation_epoch"
    const val KEY_OFFLINE_ISSUE_EPOCH = "offline_license_issue_epoch"
    const val KEY_OFFLINE_EXPIRY_EPOCH = "offline_license_expiry_epoch"
    const val KEY_OFFLINE_LAST_VERIFIED_EPOCH = "offline_license_last_verified_epoch"
    const val KEY_OFFLINE_LAST_ELAPSED_REALTIME = "offline_license_last_elapsed_realtime"
    const val KEY_OFFLINE_CLOCK_TAMPERED = "offline_clock_tampered"

    const val KEY_OIC_NW_PRO_UNLOCKED = "is_oic_nw_pro_unlocked"
    const val KEY_GMDSS_PRO_UNLOCKED = "is_gmdss_pro_unlocked"
    const val KEY_ENGINE_OIC_EW_PRO_UNLOCKED = "is_engine_oic_ew_pro_unlocked"
    const val KEY_DECK_MGT_PRO_UNLOCKED = "is_deck_mgt_pro_unlocked"
    const val KEY_ENGINE_MGT_PRO_UNLOCKED = "is_engine_mgt_pro_unlocked"
    const val KEY_LICENSE_ACTIVATED = "is_license_activated"

    const val OIC_NW_LICENSED = "OIC_NW_LICENSED"
    const val GMDSS_LICENSED = "GMDSS_LICENSED"
    const val ENGINE_OIC_EW_LICENSED = "ENGINE_OIC_EW_LICENSED"
    const val DECK_MGT_LICENSED = "DECK_MGT_LICENSED"
    const val ENGINE_MGT_LICENSED = "ENGINE_MGT_LICENSED"

    private val _isOicNwProUnlockedFlow = MutableStateFlow(false)
    val isOicNwProUnlockedFlow: StateFlow<Boolean> = _isOicNwProUnlockedFlow.asStateFlow()

    private val _isGmdssProUnlockedFlow = MutableStateFlow(false)
    val isGmdssProUnlockedFlow: StateFlow<Boolean> = _isGmdssProUnlockedFlow.asStateFlow()

    private val _isEngineOicEwProUnlockedFlow = MutableStateFlow(false)
    val isEngineOicEwProUnlockedFlow: StateFlow<Boolean> = _isEngineOicEwProUnlockedFlow.asStateFlow()

    private val _isDeckMgtProUnlockedFlow = MutableStateFlow(false)
    val isDeckMgtProUnlockedFlow: StateFlow<Boolean> = _isDeckMgtProUnlockedFlow.asStateFlow()

    private val _isEngineMgtProUnlockedFlow = MutableStateFlow(false)
    val isEngineMgtProUnlockedFlow: StateFlow<Boolean> = _isEngineMgtProUnlockedFlow.asStateFlow()

    private val _isProActivatedFlow = MutableStateFlow(false)
    val isProActivatedFlow: StateFlow<Boolean> = _isProActivatedFlow.asStateFlow()

    private val _isHyperionProUnlockedFlow = MutableStateFlow(false)
    val isHyperionProUnlockedFlow: StateFlow<Boolean> = _isHyperionProUnlockedFlow.asStateFlow()

    // Developer access is granted only after the signed-in user is approved by the
    // same server-side check-admin endpoint used by the website. Keep this process-local
    // and bind it to the current Supabase user so it cannot transfer between accounts.
    private val _isDeveloperAdminAccessFlow = MutableStateFlow(false)
    val isDeveloperAdminAccessFlow: StateFlow<Boolean> = _isDeveloperAdminAccessFlow.asStateFlow()
    @Volatile private var verifiedDeveloperAdminUserId: String? = null

    private val SALT_PART_A = byteArrayOf(0x12, 0x03, 0x0A, 0x1F, 0x08)
    private val SALT_PART_B = byteArrayOf(0x13, 0x15, 0x14, 0x05)
    private val SALT_PART_C = byteArrayOf(0x17, 0x1B, 0x08, 0x13, 0x0E, 0x13, 0x17, 0x1F, 0x05)
    private val SALT_PART_D = byteArrayOf(0x09, 0x1B, 0x16, 0x0E, 0x05)
    private val SALT_PART_E = byteArrayOf(0x68, 0x6A, 0x68, 0x6C, 0x05)
    private val SALT_PART_F = byteArrayOf(0x09, 0x1F, 0x19, 0x0F, 0x08, 0x1F, 0x05)
    private val SALT_PART_G = byteArrayOf(0x1B, 0x0F, 0x0E, 0x12)
    private const val SALT_XOR_KEY: Byte = 0x5A

    private fun decodeSaltChunk(chunk: ByteArray): String {
        val decoded = ByteArray(chunk.size)
        for (i in chunk.indices) {
            decoded[i] = (chunk[i].toInt() xor SALT_XOR_KEY.toInt()).toByte()
        }
        return String(decoded, Charsets.UTF_8)
    }

    fun getSecretSalt(): String {
        return decodeSaltChunk(SALT_PART_A) +
               decodeSaltChunk(SALT_PART_B) +
               decodeSaltChunk(SALT_PART_C) +
               decodeSaltChunk(SALT_PART_D) +
               decodeSaltChunk(SALT_PART_E) +
               decodeSaltChunk(SALT_PART_F) +
               decodeSaltChunk(SALT_PART_G)
    }

    val SECRET_SALT: String get() = getSecretSalt()

    fun init(context: Context) {
        // Purge legacy permanent licenses and clearance keys on start
        purgeLegacyPermanentLicenses(context)

        // Evaluate current offline status safely without destructive wiping
        checkOfflineLicense(context)

        val oic = isOicNwProUnlocked(context)
        val gmdss = isGmdssProUnlocked(context)
        val engineEw = isEngineOicEwProUnlocked(context)
        val deckMgt = isDeckMgtProUnlocked(context)
        val engineMgt = isEngineMgtProUnlocked(context)
        val hyp = oic && gmdss && engineEw && deckMgt && engineMgt
        val active = oic || gmdss || engineEw || deckMgt || engineMgt

        _isOicNwProUnlockedFlow.value = oic
        _isGmdssProUnlockedFlow.value = gmdss
        _isEngineOicEwProUnlockedFlow.value = engineEw
        _isDeckMgtProUnlockedFlow.value = deckMgt
        _isEngineMgtProUnlockedFlow.value = engineMgt
        _isHyperionProUnlockedFlow.value = hyp
        _isProActivatedFlow.value = active
    }

    private fun accountEntitlementPreferences(context: Context): SharedPreferences? {
        accountEntitlementPrefsCache?.let { return it }
        return try {
            synchronized(this) {
                accountEntitlementPrefsCache ?: run {
                    val appContext = context.applicationContext
                    val masterKey = MasterKey.Builder(appContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    EncryptedSharedPreferences.create(
                        appContext,
                        ACCOUNT_ENTITLEMENTS_PREFS,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    ).also { accountEntitlementPrefsCache = it }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Allows Robolectric tests to supply an isolated preferences implementation because
     * Android Keystore-backed EncryptedSharedPreferences is not available in that runtime.
     * Production code never calls this; app sessions always use the encrypted store above.
     */
    internal fun setAccountEntitlementPreferencesForTesting(prefs: SharedPreferences?) {
        synchronized(this) {
            accountEntitlementPrefsCache = prefs
        }
    }

    private fun accountExpiryKey(track: Track): String = ACCOUNT_EXPIRY_PREFIX + track.name

    fun isAccountEntitlementUnlocked(context: Context, track: Track): Boolean =
        isAccountTrackUnlocked(context.applicationContext, track)

    /**
     * Returns the server-issued website/account expiry only while that grant is still
     * valid for the current signed-in account and its bounded offline verification lease.
     * A zero result means the store must not display the website grant as active.
     */
    fun getAccountEntitlementExpiry(context: Context, track: Track): Long {
        val appContext = context.applicationContext
        if (!isAccountTrackUnlocked(appContext, track)) return 0L
        val prefs = accountEntitlementPreferences(appContext) ?: return 0L
        return prefs.getLong(accountExpiryKey(track), 0L)
    }

    /**
     * The monotonic display clock for account-backed expiry countdowns. It mirrors
     * the high-water mark used by account entitlement validation, so a small wall-clock
     * rollback cannot make the displayed remaining duration increase.
     */
    internal fun effectiveAccountEntitlementDisplayNow(
        nowEpochMs: Long,
        syncedAtEpochMs: Long,
        lastVerifiedEpochMs: Long
    ): Long = maxOf(nowEpochMs, syncedAtEpochMs, lastVerifiedEpochMs)

    fun getAccountEntitlementEffectiveNow(context: Context): Long {
        val now = System.currentTimeMillis()
        val prefs = accountEntitlementPreferences(context.applicationContext)
            ?: return now
        val ownerUserId = prefs.getString(ACCOUNT_OWNER_USER_ID_KEY, null)
        val currentUserId = prefs.getString(ACCOUNT_CURRENT_USER_ID_KEY, null)
        if (ownerUserId.isNullOrBlank() || ownerUserId != currentUserId) {
            return now
        }
        val syncedAt = prefs.getLong(ACCOUNT_SYNCED_AT_KEY, 0L)
        val lastVerified = prefs.getLong(ACCOUNT_LAST_VERIFIED_EPOCH_KEY, syncedAt)
        return effectiveAccountEntitlementDisplayNow(now, syncedAt, lastVerified)
    }

    /** Keep the offline lease's verification timestamp monotonic across device-clock rollback. */
    internal fun nextAccountEntitlementVerificationTime(
        nowEpochMs: Long,
        previousVerifiedEpochMs: Long
    ): Long = maxOf(nowEpochMs.coerceAtLeast(0L), previousVerifiedEpochMs.coerceAtLeast(0L))

    /** Pure account-grant validation shared by the access gate and deterministic tests. */
    internal fun isAccountEntitlementValid(
        ownerUserId: String?,
        currentUserId: String?,
        syncedAtEpochMs: Long,
        lastVerifiedEpochMs: Long,
        expiryEpochMs: Long,
        nowEpochMs: Long
    ): Boolean {
        if (ownerUserId.isNullOrBlank() || ownerUserId != currentUserId) return false
        if (syncedAtEpochMs <= 0L) return false

        // Older installs do not have a high-water mark yet. Treat their last verified
        // time as the original successful sync time, preserving the existing 72-hour lease.
        val highWaterMark = maxOf(syncedAtEpochMs, lastVerifiedEpochMs)
        val clockRollbackToleranceMs = 5L * 60L * 1000L
        if (nowEpochMs < highWaterMark - clockRollbackToleranceMs) return false

        // Within the small clock-skew tolerance, never let a lower wall clock move the
        // effective time backwards. Both the offline grace and server expiry use it.
        val effectiveNow = maxOf(nowEpochMs, highWaterMark)
        if (effectiveNow - syncedAtEpochMs > ACCOUNT_OFFLINE_GRACE_MS) return false
        return expiryEpochMs > effectiveNow
    }

    private fun isAccountTrackUnlocked(context: Context, track: Track): Boolean {
        val prefs = accountEntitlementPreferences(context) ?: return false
        val now = System.currentTimeMillis()
        val syncedAt = prefs.getLong(ACCOUNT_SYNCED_AT_KEY, 0L)
        val lastVerified = prefs.getLong(ACCOUNT_LAST_VERIFIED_EPOCH_KEY, syncedAt)
        val allowed = isAccountEntitlementValid(
            ownerUserId = prefs.getString(ACCOUNT_OWNER_USER_ID_KEY, null),
            currentUserId = prefs.getString(ACCOUNT_CURRENT_USER_ID_KEY, null),
            syncedAtEpochMs = syncedAt,
            lastVerifiedEpochMs = lastVerified,
            expiryEpochMs = prefs.getLong(accountExpiryKey(track), 0L),
            nowEpochMs = now
        )

        // Advance the persisted high-water mark at most once per minute to avoid
        // writing SharedPreferences on every question/access-gate check.
        if (allowed && now > lastVerified + 60L * 1000L) {
            prefs.edit().putLong(ACCOUNT_LAST_VERIFIED_EPOCH_KEY, now).apply()
        }
        return allowed
    }

    /** Stores active, server-read website entitlements with a bounded offline grace period. */
    fun saveAccountEntitlements(context: Context, userId: String, expiryByTrack: Map<Track, Long>): Boolean {
        if (userId.isBlank()) return false
        val prefs = accountEntitlementPreferences(context) ?: return false
        val previousVerifiedAt = prefs.getLong(
            ACCOUNT_LAST_VERIFIED_EPOCH_KEY,
            prefs.getLong(ACCOUNT_SYNCED_AT_KEY, 0L)
        )
        val syncedAt = nextAccountEntitlementVerificationTime(
            System.currentTimeMillis(),
            previousVerifiedAt
        )
        val editor = prefs.edit()
            .putString(ACCOUNT_OWNER_USER_ID_KEY, userId)
            .putString(ACCOUNT_CURRENT_USER_ID_KEY, userId)
            .putLong(ACCOUNT_SYNCED_AT_KEY, syncedAt)
            .putLong(ACCOUNT_LAST_VERIFIED_EPOCH_KEY, syncedAt)
        Track.values().forEach { track ->
            editor.putLong(accountExpiryKey(track), expiryByTrack[track] ?: 0L)
        }
        editor.apply()
        refreshAccountEntitlementFlows(context)
        return true
    }

    /** Returns the currently signed-in Hyperion account ID from encrypted account preferences. */
    fun getCurrentAccountUserId(context: Context): String? =
        accountEntitlementPreferences(context.applicationContext)
            ?.getString(ACCOUNT_CURRENT_USER_ID_KEY, null)
            ?.takeIf { it.isNotBlank() }

    fun setCurrentAccountUserId(context: Context, userId: String?) {
        val nextUserId = userId.orEmpty()
        if (verifiedDeveloperAdminUserId != null && verifiedDeveloperAdminUserId != nextUserId) {
            verifiedDeveloperAdminUserId = null
            _isDeveloperAdminAccessFlow.value = false
        }
        accountEntitlementPreferences(context)?.edit()
            ?.putString(ACCOUNT_CURRENT_USER_ID_KEY, nextUserId)
            ?.apply()
        refreshAccountEntitlementFlows(context)
    }

    /**
     * Apply developer access only after the server's ADMIN_EMAILS allowlist check
     * has returned isAdmin=true for this exact authenticated Supabase user.
     * The grant is process-local and is discarded on sign-out, account switch, or app restart.
     */
    fun setVerifiedDeveloperAdminAccess(context: Context, userId: String, enabled: Boolean) {
        val currentUserId = accountEntitlementPreferences(context)
            ?.getString(ACCOUNT_CURRENT_USER_ID_KEY, null)
        val verifiedUserId = userId.takeIf {
            enabled && it.isNotBlank() && it == currentUserId
        }
        verifiedDeveloperAdminUserId = verifiedUserId
        _isDeveloperAdminAccessFlow.value = verifiedUserId != null
        refreshAccountEntitlementFlows(context)
    }

    fun isDeveloperAdminAccessActive(context: Context): Boolean {
        val verifiedUserId = verifiedDeveloperAdminUserId ?: return false
        val currentUserId = accountEntitlementPreferences(context)
            ?.getString(ACCOUNT_CURRENT_USER_ID_KEY, null)
        return verifiedUserId == currentUserId
    }

    fun clearAccountEntitlements(context: Context) {
        verifiedDeveloperAdminUserId = null
        _isDeveloperAdminAccessFlow.value = false
        accountEntitlementPreferences(context)?.edit()?.clear()?.apply()
        refreshAccountEntitlementFlows(context)
    }

    fun refreshAccountEntitlementFlows(context: Context) {
        val oic = isOicNwProUnlocked(context)
        val gmdss = isGmdssProUnlocked(context)
        val ew = isEngineOicEwProUnlocked(context)
        val deckMgt = isDeckMgtProUnlocked(context)
        val engineMgt = isEngineMgtProUnlocked(context)
        _isOicNwProUnlockedFlow.value = oic
        _isGmdssProUnlockedFlow.value = gmdss
        _isEngineOicEwProUnlockedFlow.value = ew
        _isDeckMgtProUnlockedFlow.value = deckMgt
        _isEngineMgtProUnlockedFlow.value = engineMgt
        _isHyperionProUnlockedFlow.value = isHyperionProUnlocked(context) || (oic && gmdss && ew && deckMgt && engineMgt)
        _isProActivatedFlow.value = oic || gmdss || ew || deckMgt || engineMgt
        _isDeveloperAdminAccessFlow.value = isDeveloperAdminAccessActive(context)
        _accountEntitlementRevisionFlow.value = _accountEntitlementRevisionFlow.value + 1L
    }
    /**
     * Purges old permanent clearance / lifetime licenses that were issued under previous system.
     */
    fun purgeLegacyPermanentLicenses(context: Context) {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        val licenseType = prefs.getString("license_type", "") ?: ""
        val isHardwareLifetime = prefs.getBoolean("is_hardware_lifetime", false)
        val isFullyActivated = prefs.getBoolean("is_fully_activated", false)
        val expiryEpoch = prefs.getLong(KEY_OFFLINE_EXPIRY_EPOCH, 0L)

        // If it was marked as LIFETIME or permanent without an expiry epoch, wipe it clean
        if ((licenseType == "LIFETIME" || isHardwareLifetime || isFullyActivated) && expiryEpoch <= 0L) {
            prefs.edit()
                .remove("is_hardware_lifetime")
                .remove("is_fully_activated")
                .remove("is_full_access_unlocked")
                .putBoolean("is_licensed", false)
                .putBoolean("is_offline_license", false)
                .putString("license_type", "RESTRICTED")
                .putBoolean("is_oic_unlocked", false)
                .putBoolean("is_gmdss_unlocked", false)
                .putBoolean("is_engine_ew_unlocked", false)
                .putBoolean("is_deck_mgt_unlocked", false)
                .putBoolean("is_engine_mgt_unlocked", false)
                .putBoolean("is_all_access_unlocked", false)
                .apply()

            listOf(PREFS_NAME, "hyperion_prefs", "app_prefs", "license_prefs").forEach { name ->
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
            }
        }
    }

    /**
     * Universal Request Code format: HYPE-xxxx-xxxx-xxxx (e.g. HYPE-A8F2-771C-9E4B).
     * Single universal identifier bound deterministically to device hardware.
     */
    fun getUniversalDeviceCode(context: Context): String {
        val rawId = try {
            val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!id.isNullOrBlank()) id.filter { it.isLetterOrDigit() }.uppercase() else "A8F2771C9E4B"
        } catch (_: Exception) {
            "A8F2771C9E4B"
        }

        val hex12 = if (rawId.length >= 12) {
            rawId.take(12)
        } else {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(rawId.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02X".format(it) }.take(12)
        }
        return "HYPE-${hex12.substring(0, 4)}-${hex12.substring(4, 8)}-${hex12.substring(8, 12)}"
    }

    fun getDeviceCleanToken(context: Context): String {
        return getUniversalDeviceCode(context).removePrefix("HYPE-").replace("-", "").uppercase()
    }

    fun getPermanentHardwareId(context: Context): String = getUniversalDeviceCode(context)
    fun getOrGenerateRequestCode(context: Context): String = getUniversalDeviceCode(context)
    fun getRequestCodeForTier(context: Context, tier: ProTier): String = getUniversalDeviceCode(context)
    fun getMasterRequestCode(context: Context): String = getUniversalDeviceCode(context)
    fun getInstallSessionId(context: Context): String = getDeviceCleanToken(context).take(6)
    fun getInstallDate(context: Context): String = "Hardware-Bound"
    fun getOrGenerateInstallationToken(context: Context): String = getDeviceCleanToken(context).take(6)

    fun generateRequestCode(seed: String): String {
        val h = (seed.hashCode().toLong() and 0xFFFFFFFFL)
        val part1 = ((h shr 16) and 0xFFFFL).toString(16).uppercase().padStart(4, '0')
        val part2 = (h and 0xFFFFL).toString(16).uppercase().padStart(4, '0')
        return "HYP-$part1-$part2"
    }

    /**
     * 3-Layer Anti-Tamper Engine & Status Evaluator:
     * Layer 1: Cryptographic Issue Window
     * Layer 2: Monotonic High-Water Mark (Rollback Latch)
     * Layer 3: Hardware Monotonic Clock (SystemClock.elapsedRealtime)
     */
    fun checkOfflineLicense(context: Context): OfflineLicenseStatus {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        val isOffline = prefs.getBoolean(KEY_OFFLINE_LICENSE, false)
        if (!isOffline) {
            return OfflineLicenseStatus.NotActive
        }

        val expiryEpoch = prefs.getLong(KEY_OFFLINE_EXPIRY_EPOCH, 0L)
        val issueEpoch = prefs.getLong(KEY_OFFLINE_ISSUE_EPOCH, 0L)
        val lastVerifiedEpoch = prefs.getLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, 0L)
        val lastElapsedRealtime = prefs.getLong(KEY_OFFLINE_LAST_ELAPSED_REALTIME, 0L)
        val tier = prefs.getString(KEY_OFFLINE_TIER, "ALL_ACCESS") ?: "ALL_ACCESS"
        val duration = prefs.getString(KEY_OFFLINE_DURATION, "1M") ?: "1M"
        val activationEpoch = prefs.getLong(KEY_OFFLINE_ACTIVATION_EPOCH, 0L)

        val now = System.currentTimeMillis()
        val currentElapsed = SystemClock.elapsedRealtime()

        val timezoneGraceMs = 48L * 60L * 60L * 1000L

        // HIGH-WATER MARK: the latest time this phone ever showed.
        // It is only lowered by the internet-time check (healRollbackLatch).
        val healedHighWaterMark = maxOf(lastVerifiedEpoch, activationEpoch, issueEpoch)

        // LAYER 1: Hard clock check — system clock set earlier than license creation
        if (issueEpoch > 0L && now < (issueEpoch - timezoneGraceMs)) {
            prefs.edit().putBoolean(KEY_OFFLINE_CLOCK_TAMPERED, true).apply()
            val info = OfflineLicenseInfo(
                tier = tier,
                duration = duration,
                activationEpoch = activationEpoch,
                issueEpoch = issueEpoch,
                expiryEpoch = expiryEpoch,
                isTampered = true,
                tamperReason = "Device clock is set earlier than license issue date.",
                highWaterMarkEpoch = healedHighWaterMark
            )
            return OfflineLicenseStatus.ClockTampered(info.tamperReason!!)
        }

        // LAYER 2: Rollback Latch — the clock is more than 48 hours behind the latest time ever seen
        if (lastVerifiedEpoch > now + timezoneGraceMs) {
            prefs.edit().putBoolean(KEY_OFFLINE_CLOCK_TAMPERED, true).apply()
            val info = OfflineLicenseInfo(
                tier = tier,
                duration = duration,
                activationEpoch = activationEpoch,
                issueEpoch = issueEpoch,
                expiryEpoch = expiryEpoch,
                isTampered = true,
                tamperReason = "Device clock rollback detected. Restore correct device time.",
                highWaterMarkEpoch = healedHighWaterMark
            )
            return OfflineLicenseStatus.ClockTampered(info.tamperReason!!)
        }

        // Clear any previous tamper flag once clock is validated
        if (prefs.getBoolean(KEY_OFFLINE_CLOCK_TAMPERED, false)) {
            prefs.edit().putBoolean(KEY_OFFLINE_CLOCK_TAMPERED, false).apply()
        }

        val effectiveNow = maxOf(now, healedHighWaterMark)

        // EXPIRATION CHECK
        if (expiryEpoch > 0L && effectiveNow >= expiryEpoch) {
            revokeOfflineAccess(context)
            return OfflineLicenseStatus.Expired
        }

        // Advance High-Water Mark
        prefs.edit()
            .putLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, maxOf(lastVerifiedEpoch, now))
            .putLong(KEY_OFFLINE_LAST_ELAPSED_REALTIME, currentElapsed)
            .apply()

        val info = OfflineLicenseInfo(
            tier = tier,
            duration = duration,
            activationEpoch = activationEpoch,
            issueEpoch = issueEpoch,
            expiryEpoch = expiryEpoch,
            isTampered = false,
            tamperReason = null,
            highWaterMarkEpoch = healedHighWaterMark
        )
        return OfflineLicenseStatus.Active(info)
    }

    fun getOfflineLicenseInfo(context: Context): OfflineLicenseInfo? {
        val status = checkOfflineLicense(context)
        return when (status) {
            is OfflineLicenseStatus.Active -> status.info
            is OfflineLicenseStatus.ClockTampered -> {
                val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
                val actEpoch = prefs.getLong(KEY_OFFLINE_ACTIVATION_EPOCH, 0L)
                val issEpoch = prefs.getLong(KEY_OFFLINE_ISSUE_EPOCH, 0L)
                val lastVerEpoch = prefs.getLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, 0L)
                val highWater = maxOf(actEpoch, issEpoch, lastVerEpoch)
                OfflineLicenseInfo(
                    tier = prefs.getString(KEY_OFFLINE_TIER, "ALL_ACCESS") ?: "ALL_ACCESS",
                    duration = prefs.getString(KEY_OFFLINE_DURATION, "1M") ?: "1M",
                    activationEpoch = actEpoch,
                    issueEpoch = issEpoch,
                    expiryEpoch = prefs.getLong(KEY_OFFLINE_EXPIRY_EPOCH, 0L),
                    isTampered = true,
                    tamperReason = status.reason,
                    highWaterMarkEpoch = highWater
                )
            }
            else -> null
        }
    }

    /**
     * Validates and activates an entered key against the 3-layer offline security engine.
     */
    fun verifyAndUnlockKey(context: Context, enteredKey: String, selectedTier: ProTier? = null): UnlockResult {
        val cleanInput = enteredKey.trim().uppercase().replace(" ", "")
        if (cleanInput.isEmpty()) return UnlockResult.INVALID_KEY
        
        // Signed keys (HYPE2-) are the only accepted offline keys. Anything else is rejected below.
        if (SignedLicenseVerifier.isSignedFormat(cleanInput)) {
            val signed = SignedLicenseVerifier.verify(cleanInput, getDeviceCleanToken(context))
                ?: return UnlockResult.INVALID_KEY

            val blockedPlans = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_BLOCKED_PLANS, emptySet()) ?: emptySet()
            if ("ALL" in blockedPlans || signed.tier in blockedPlans) return UnlockResult.INVALID_KEY

            val signedNow = System.currentTimeMillis()
            val signedGraceMs = 48L * 60L * 60L * 1000L
            if (signedNow < 1704067200000L || signedNow < (signed.issueEpoch - signedGraceMs)) {
                return UnlockResult.CLOCK_ERROR
            }
            if (signedNow > signed.expiryEpoch) {
                return UnlockResult.KEY_EXPIRED
            }

            val (sOic, sGmdss, sEw, sDm, sEm, sAll, sTier) = when (signed.tier) {
                "OIC" -> Septuple(true, false, false, false, false, false, "OIC_NW")
                "GOC" -> Septuple(false, true, false, false, false, false, "GMDSS_GOC")
                "EW" -> Septuple(false, false, true, false, false, false, "ENGINE_OIC_EW")
                "DM" -> Septuple(false, false, false, true, false, false, "DECK_MGT")
                "EM" -> Septuple(false, false, false, false, true, false, "ENGINE_MGT")
                else -> Septuple(true, true, true, true, true, true, "ALL_ACCESS")
            }

            saveOfflineActivation(
                context = context,
                tier = sTier,
                duration = if (signed.duration == "EXP") "CUSTOM" else signed.duration,
                activationEpoch = signedNow,
                issueEpoch = signed.issueEpoch,
                expiryEpoch = signed.expiryEpoch,
                unlockOic = sOic,
                unlockGmdss = sGmdss,
                unlockEw = sEw,
                unlockDm = sDm,
                unlockEm = sEm,
                unlockAll = sAll
            )

            syncTrustedTime(context)

            return when (signed.tier) {
                "OIC" -> UnlockResult.OIC_NW_UNLOCKED
                "GOC" -> UnlockResult.GMDSS_UNLOCKED
                "EW" -> UnlockResult.ENGINE_OIC_EW_UNLOCKED
                "DM" -> UnlockResult.DECK_MGT_UNLOCKED
                "EM" -> UnlockResult.ENGINE_MGT_UNLOCKED
                else -> UnlockResult.HYPERION_UNLOCKED
            }
        }

        // Old SHA-256 key formats are no longer accepted. Only signed HYPE2- keys activate.
        return UnlockResult.INVALID_KEY
    }

    private data class Septuple(val oic: Boolean, val gmdss: Boolean, val ew: Boolean, val dm: Boolean, val em: Boolean, val all: Boolean, val tier: String)

    private fun saveOfflineActivation(
        context: Context,
        tier: String,
        duration: String,
        activationEpoch: Long,
        issueEpoch: Long,
        expiryEpoch: Long,
        unlockOic: Boolean,
        unlockGmdss: Boolean,
        unlockEw: Boolean,
        unlockDm: Boolean,
        unlockEm: Boolean,
        unlockAll: Boolean
    ) {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        
        // Preserve existing track entitlements so activating one module never destroys another
        val prevOic = prefs.getBoolean("is_oic_unlocked", false)
        val prevGmdss = prefs.getBoolean("is_gmdss_unlocked", false)
        val prevEw = prefs.getBoolean("is_engine_ew_unlocked", false) || prefs.getBoolean("is_ew_unlocked", false)
        val prevDm = prefs.getBoolean("is_deck_mgt_unlocked", false) || prefs.getBoolean("is_dm_unlocked", false)
        val prevEm = prefs.getBoolean("is_engine_mgt_unlocked", false) || prefs.getBoolean("is_em_unlocked", false)

        // Read independent previous track expiration epochs with backward compatibility
        val fallbackExp = prefs.getLong(KEY_OFFLINE_EXPIRY_EPOCH, 0L)
        val prevExpiryOic = prefs.getLong("offline_expiry_oic", fallbackExp)
        val prevExpiryGmdss = prefs.getLong("offline_expiry_gmdss", fallbackExp)
        val prevExpiryEw = prefs.getLong("offline_expiry_ew", fallbackExp)
        val prevExpiryDm = prefs.getLong("offline_expiry_dm", fallbackExp)
        val prevExpiryEm = prefs.getLong("offline_expiry_em", fallbackExp)
        val prevExpiryAll = prefs.getLong("offline_expiry_all", fallbackExp)

        val mergedOic = unlockAll || unlockOic || prevOic
        val mergedGmdss = unlockAll || unlockGmdss || prevGmdss
        val mergedEw = unlockAll || unlockEw || prevEw
        val mergedDm = unlockAll || unlockDm || prevDm
        val mergedEm = unlockAll || unlockEm || prevEm
        val mergedAll = unlockAll || (mergedOic && mergedGmdss && mergedEw && mergedDm && mergedEm)

        // Assign independent expiration per track
        val newExpiryOic = if (unlockAll || unlockOic) maxOf(prevExpiryOic, expiryEpoch) else prevExpiryOic
        val newExpiryGmdss = if (unlockAll || unlockGmdss) maxOf(prevExpiryGmdss, expiryEpoch) else prevExpiryGmdss
        val newExpiryEw = if (unlockAll || unlockEw) maxOf(prevExpiryEw, expiryEpoch) else prevExpiryEw
        val newExpiryDm = if (unlockAll || unlockDm) maxOf(prevExpiryDm, expiryEpoch) else prevExpiryDm
        val newExpiryEm = if (unlockAll || unlockEm) maxOf(prevExpiryEm, expiryEpoch) else prevExpiryEm
        val newExpiryAll = if (unlockAll) maxOf(prevExpiryAll, expiryEpoch) else prevExpiryAll

        val overallMaxExpiry = maxOf(newExpiryOic, newExpiryGmdss, newExpiryEw, newExpiryDm, newExpiryEm, newExpiryAll)

        prefs.edit()
            .putBoolean(KEY_OFFLINE_LICENSE, true)
            .putString("license_type", "OFFLINE_PASS")
            .putString(KEY_OFFLINE_TIER, if (mergedAll) "ALL_ACCESS" else tier)
            .putString(KEY_OFFLINE_DURATION, duration)
            .putLong(KEY_OFFLINE_ACTIVATION_EPOCH, activationEpoch)
            .putLong(KEY_OFFLINE_ISSUE_EPOCH, issueEpoch)
            .putLong(KEY_OFFLINE_EXPIRY_EPOCH, overallMaxExpiry)
            .putLong("offline_expiry_oic", newExpiryOic)
            .putLong("offline_expiry_gmdss", newExpiryGmdss)
            .putLong("offline_expiry_ew", newExpiryEw)
            .putLong("offline_expiry_dm", newExpiryDm)
            .putLong("offline_expiry_em", newExpiryEm)
            .putLong("offline_expiry_all", newExpiryAll)
            .putLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, activationEpoch)
            .putLong(KEY_OFFLINE_LAST_ELAPSED_REALTIME, SystemClock.elapsedRealtime())
            .putBoolean(KEY_OFFLINE_CLOCK_TAMPERED, false)
            .putBoolean("is_licensed", true)
            .putBoolean("is_oic_unlocked", mergedOic)
            .putBoolean("is_gmdss_unlocked", mergedGmdss)
            .putBoolean("is_engine_ew_unlocked", mergedEw)
            .putBoolean("is_ew_unlocked", mergedEw) // Saves both key formats to avoid mismatch
            .putBoolean("is_deck_mgt_unlocked", mergedDm)
            .putBoolean("is_dm_unlocked", mergedDm)
            .putBoolean("is_engine_mgt_unlocked", mergedEm)
            .putBoolean("is_em_unlocked", mergedEm)
            .putBoolean("is_all_access_unlocked", mergedAll)
            .putBoolean("is_full_access_unlocked", mergedAll)
            .remove("is_hardware_lifetime")
            .remove("is_fully_activated")
            .apply()

        // Keep all active tracks live in memory flows
        setOicNwProUnlocked(context, mergedOic)
        setGmdssProUnlocked(context, mergedGmdss)
        setEngineOicEwProUnlocked(context, mergedEw)
        setDeckMgtProUnlocked(context, mergedDm)
        setEngineMgtProUnlocked(context, mergedEm)

        if (mergedAll) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_LICENSE_ACTIVATED, true).apply()
            _isHyperionProUnlockedFlow.value = true
        }
    }

    fun revokeOfflineAccess(context: Context) {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_OFFLINE_LICENSE, false)
            .putString("license_type", "RESTRICTED")
            .remove(KEY_OFFLINE_EXPIRY_EPOCH)
            .remove(KEY_OFFLINE_ACTIVATION_EPOCH)
            .remove("offline_expiry_oic")
            .remove("offline_expiry_gmdss")
            .remove("offline_expiry_ew")
            .remove("offline_expiry_dm")
            .remove("offline_expiry_em")
            .remove("offline_expiry_all")
            .putBoolean("is_licensed", false)
            .putBoolean("is_oic_unlocked", false)
            .putBoolean("is_gmdss_unlocked", false)
            .putBoolean("is_engine_ew_unlocked", false)
            .putBoolean("is_ew_unlocked", false)
            .putBoolean("is_deck_mgt_unlocked", false)
            .putBoolean("is_dm_unlocked", false)
            .putBoolean("is_engine_mgt_unlocked", false)
            .putBoolean("is_em_unlocked", false)
            .putBoolean("is_all_access_unlocked", false)
            .putBoolean("is_full_access_unlocked", false)
            .apply()

        setOicNwProUnlocked(context, false)
        setGmdssProUnlocked(context, false)
        setEngineOicEwProUnlocked(context, false)
        setDeckMgtProUnlocked(context, false)
        setEngineMgtProUnlocked(context, false)
        setHyperionProUnlocked(context, false)
    }

    private const val KEY_BLOCKED_PLANS = "offline_blocked_plans"

    /**
     * Reads the real date from the internet (Google server) and locks any offline-key track
     * whose end date has already passed, even if the phone clock was set back.
     * If the phone is offline, nothing changes.
     */
    fun syncTrustedTime(context: Context) {
        val appContext = context.applicationContext
        kotlin.concurrent.thread(start = true, name = "trusted-time-sync") {
            try {
                val conn = java.net.URL("https://www.google.com/generate_204")
                    .openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "HEAD"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.instanceFollowRedirects = false
                val serverNow = try {
                    conn.connect()
                    if (conn.responseCode == 204) conn.date else 0L
                } finally {
                    conn.disconnect()
                }
                if (serverNow < 1704067200000L) return@thread
                healRollbackLatch(appContext, serverNow)
                expireByTrustedTime(appContext, serverNow)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Lowers the rollback mark only when the phone clock agrees with the internet time,
     * so a phone whose clock was once wrong can recover. A clock that is still set back is not healed.
     */
    private fun healRollbackLatch(context: Context, serverNow: Long) {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_OFFLINE_LICENSE, false)) return
        val toleranceMs = 48L * 60L * 60L * 1000L
        val lastVerified = prefs.getLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, 0L)
        val phoneClockIsCorrect = kotlin.math.abs(System.currentTimeMillis() - serverNow) <= toleranceMs
        if (phoneClockIsCorrect && lastVerified > serverNow + toleranceMs) {
            prefs.edit()
                .putLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, serverNow)
                .putBoolean(KEY_OFFLINE_CLOCK_TAMPERED, false)
                .apply()
            init(context)
        }
    }

    private fun expireByTrustedTime(context: Context, serverNow: Long) {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_OFFLINE_LICENSE, false)) return

        fun expired(track: Track): Boolean {
            val exp = getOfflineTrackExpiry(context, track)
            return exp > 0L && serverNow >= exp
        }

        val editor = prefs.edit()
        var changed = false
        if (expired(Track.DECK_OIC_NW)) {
            editor.putBoolean("is_oic_unlocked", false)
            setOicNwProUnlocked(context, false)
            changed = true
        }
        if (expired(Track.GMDSS_GOC)) {
            editor.putBoolean("is_gmdss_unlocked", false)
            setGmdssProUnlocked(context, false)
            changed = true
        }
        if (expired(Track.ENGINE_OIC_EW)) {
            editor.putBoolean("is_engine_ew_unlocked", false).putBoolean("is_ew_unlocked", false)
            setEngineOicEwProUnlocked(context, false)
            changed = true
        }
        if (expired(Track.DECK_MGT)) {
            editor.putBoolean("is_deck_mgt_unlocked", false).putBoolean("is_dm_unlocked", false)
            setDeckMgtProUnlocked(context, false)
            changed = true
        }
        if (expired(Track.ENGINE_MGT)) {
            editor.putBoolean("is_engine_mgt_unlocked", false).putBoolean("is_em_unlocked", false)
            setEngineMgtProUnlocked(context, false)
            changed = true
        }
        if (changed) {
            editor.putBoolean("is_all_access_unlocked", false)
            editor.putBoolean("is_full_access_unlocked", false)
            editor.apply()
            init(context)
        }
    }

    /**
     * Downloads the blocked_keys list (Firebase Remote Config) and applies it.
     * Format: DEVICECODE:PLAN,DEVICECODE:PLAN  (PLAN = ALL, OIC, GOC, EW, DM, EM)
     * If the phone is offline, nothing changes.
     */
    fun syncBlockedKeys(context: Context) {
        val appContext = context.applicationContext
        kotlin.concurrent.thread(start = true, name = "blocked-keys-sync") {
            try {
                if (com.google.firebase.FirebaseApp.getApps(appContext).isEmpty()) {
                    com.google.firebase.FirebaseApp.initializeApp(appContext)
                }
                val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
                try {
                    com.google.android.gms.tasks.Tasks.await(remoteConfig.fetchAndActivate())
                } catch (_: Exception) {
                    return@thread
                }
                applyBlockedKeys(appContext, remoteConfig.getString("blocked_keys"))
            } catch (_: Exception) {
            }
        }
    }

    fun applyBlockedKeys(context: Context, blockedList: String) {
        val me = getDeviceCleanToken(context)
        val blocked = blockedList.uppercase()
            .split(",", ";", "\n")
            .map { it.trim() }
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size != 2) return@mapNotNull null
                val device = parts[0].trim().removePrefix("HYPE-").replace("-", "")
                if (device == me) parts[1].trim() else null
            }
            .toSet()

        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putStringSet(KEY_BLOCKED_PLANS, blocked)

        if (blocked.isNotEmpty()) {
            val all = "ALL" in blocked
            val any = true
            if (any) {
                editor.putBoolean("is_all_access_unlocked", false)
                editor.putBoolean("is_full_access_unlocked", false)
            }
            if (all || "OIC" in blocked) {
                editor.putBoolean("is_oic_unlocked", false).remove("offline_expiry_oic")
                setOicNwProUnlocked(context, false)
            }
            if (all || "GOC" in blocked) {
                editor.putBoolean("is_gmdss_unlocked", false).remove("offline_expiry_gmdss")
                setGmdssProUnlocked(context, false)
            }
            if (all || "EW" in blocked) {
                editor.putBoolean("is_engine_ew_unlocked", false).putBoolean("is_ew_unlocked", false).remove("offline_expiry_ew")
                setEngineOicEwProUnlocked(context, false)
            }
            if (all || "DM" in blocked) {
                editor.putBoolean("is_deck_mgt_unlocked", false).putBoolean("is_dm_unlocked", false).remove("offline_expiry_dm")
                setDeckMgtProUnlocked(context, false)
            }
            if (all || "EM" in blocked) {
                editor.putBoolean("is_engine_mgt_unlocked", false).putBoolean("is_em_unlocked", false).remove("offline_expiry_em")
                setEngineMgtProUnlocked(context, false)
            }
        }
        editor.apply()

        // Recompute what is unlocked (Google Play purchases stay unlocked)
        init(context)
    }

    fun formatRemainingTime(expiryEpoch: Long, context: Context? = null): String {
        val now = System.currentTimeMillis()
        val effectiveNow = if (context != null) {
            val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
            val actEpoch = prefs.getLong(KEY_OFFLINE_ACTIVATION_EPOCH, 0L)
            val issEpoch = prefs.getLong(KEY_OFFLINE_ISSUE_EPOCH, 0L)
            val lastVerEpoch = prefs.getLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, 0L)

            val playPrefs = context.getSharedPreferences("hyperion_play_billing_secure_prefs", Context.MODE_PRIVATE)
            val playPurchaseTime = playPrefs.getLong("play_purchase_timestamp", 0L)
            val playHighWater = playPrefs.getLong("play_license_high_water_mark", 0L)

            maxOf(now, actEpoch, issEpoch, lastVerEpoch, playPurchaseTime, playHighWater)
        } else {
            now
        }

        val remainingMs = (expiryEpoch - effectiveNow).coerceAtLeast(0L)
        val totalSecs = remainingMs / 1000L
        val totalMins = totalSecs / 60L
        val totalHours = totalMins / 60L
        val days = (totalHours + 23L) / 24L
        val mins = totalMins % 60L

        return when {
            days > 1 -> "$days days remaining"
            totalHours > 1 -> "$totalHours hrs, $mins mins remaining"
            totalHours == 1L -> "1 hr, $mins mins remaining"
            mins > 0 -> "$mins mins remaining"
            else -> "< 1 min remaining"
        }
    }

    fun getOfflineTrackExpiry(context: Context, track: Track): Long {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        val allExp = prefs.getLong("offline_expiry_all", 0L)
        val key = when (track) {
            Track.DECK_OIC_NW -> "offline_expiry_oic"
            Track.GMDSS_GOC -> "offline_expiry_gmdss"
            Track.ENGINE_OIC_EW -> "offline_expiry_ew"
            Track.DECK_MGT -> "offline_expiry_dm"
            Track.ENGINE_MGT -> "offline_expiry_em"
        }
        val trackExp = prefs.getLong(key, prefs.getLong(KEY_OFFLINE_EXPIRY_EPOCH, 0L))
        return maxOf(allExp, trackExp)
    }

    fun getOfflineTrackTimeRemaining(context: Context, track: Track): String? {
        val isUnlocked = when (track) {
            Track.DECK_OIC_NW -> isOfflineOicNwUnlocked(context)
            Track.GMDSS_GOC -> isOfflineGmdssUnlocked(context)
            Track.ENGINE_OIC_EW -> isOfflineEngineOicEwUnlocked(context)
            Track.DECK_MGT -> isOfflineDeckMgtUnlocked(context)
            Track.ENGINE_MGT -> isOfflineEngineMgtUnlocked(context)
        }
        if (!isUnlocked) return null
        val exp = getOfflineTrackExpiry(context, track)
        if (exp <= 0L) return null
        return formatRemainingTime(exp, context)
    }

    fun getOfflineTrackFormattedExpiry(context: Context, track: Track): String? {
        val exp = getOfflineTrackExpiry(context, track)
        if (exp <= 0L) return null
        val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return "Expires " + format.format(Date(exp))
    }

    fun isOfflineOicNwUnlocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_OFFLINE_LICENSE, false)) {
            val legacy = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return legacy.getBoolean(KEY_OIC_NW_PRO_UNLOCKED, false)
        }
        if (prefs.getBoolean(KEY_OFFLINE_CLOCK_TAMPERED, false)) return false
        val isUnlocked = prefs.getBoolean("is_oic_unlocked", false) || prefs.getBoolean("is_all_access_unlocked", false) || prefs.getBoolean("is_full_access_unlocked", false)
        if (!isUnlocked) return false
        val exp = getOfflineTrackExpiry(context, Track.DECK_OIC_NW)
        val now = System.currentTimeMillis()
        val highWater = maxOf(prefs.getLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, 0L), prefs.getLong(KEY_OFFLINE_ACTIVATION_EPOCH, 0L))
        return exp > 0L && maxOf(now, highWater) < exp
    }

    fun isOfflineGmdssUnlocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_OFFLINE_LICENSE, false)) {
            val legacy = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return legacy.getBoolean(KEY_GMDSS_PRO_UNLOCKED, false)
        }
        if (prefs.getBoolean(KEY_OFFLINE_CLOCK_TAMPERED, false)) return false
        val isUnlocked = prefs.getBoolean("is_gmdss_unlocked", false) || prefs.getBoolean("is_all_access_unlocked", false) || prefs.getBoolean("is_full_access_unlocked", false)
        if (!isUnlocked) return false
        val exp = getOfflineTrackExpiry(context, Track.GMDSS_GOC)
        val now = System.currentTimeMillis()
        val highWater = maxOf(prefs.getLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, 0L), prefs.getLong(KEY_OFFLINE_ACTIVATION_EPOCH, 0L))
        return exp > 0L && maxOf(now, highWater) < exp
    }

    fun isOfflineEngineOicEwUnlocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_OFFLINE_LICENSE, false)) {
            val legacy = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return legacy.getBoolean(KEY_ENGINE_OIC_EW_PRO_UNLOCKED, false)
        }
        if (prefs.getBoolean(KEY_OFFLINE_CLOCK_TAMPERED, false)) return false
        val isUnlocked = prefs.getBoolean("is_engine_ew_unlocked", false) || prefs.getBoolean("is_ew_unlocked", false) || prefs.getBoolean("is_all_access_unlocked", false) || prefs.getBoolean("is_full_access_unlocked", false)
        if (!isUnlocked) return false
        val exp = getOfflineTrackExpiry(context, Track.ENGINE_OIC_EW)
        val now = System.currentTimeMillis()
        val highWater = maxOf(prefs.getLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, 0L), prefs.getLong(KEY_OFFLINE_ACTIVATION_EPOCH, 0L))
        return exp > 0L && maxOf(now, highWater) < exp
    }

    fun isOfflineDeckMgtUnlocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_OFFLINE_LICENSE, false)) {
            val legacy = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return legacy.getBoolean(KEY_DECK_MGT_PRO_UNLOCKED, false)
        }
        if (prefs.getBoolean(KEY_OFFLINE_CLOCK_TAMPERED, false)) return false
        val isUnlocked = prefs.getBoolean("is_deck_mgt_unlocked", false) || prefs.getBoolean("is_dm_unlocked", false) || prefs.getBoolean("is_all_access_unlocked", false) || prefs.getBoolean("is_full_access_unlocked", false)
        if (!isUnlocked) return false
        val exp = getOfflineTrackExpiry(context, Track.DECK_MGT)
        val now = System.currentTimeMillis()
        val highWater = maxOf(prefs.getLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, 0L), prefs.getLong(KEY_OFFLINE_ACTIVATION_EPOCH, 0L))
        return exp > 0L && maxOf(now, highWater) < exp
    }

    fun isOfflineEngineMgtUnlocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_OFFLINE_LICENSE, false)) {
            val legacy = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return legacy.getBoolean(KEY_ENGINE_MGT_PRO_UNLOCKED, false)
        }
        if (prefs.getBoolean(KEY_OFFLINE_CLOCK_TAMPERED, false)) return false
        val isUnlocked = prefs.getBoolean("is_engine_mgt_unlocked", false) || prefs.getBoolean("is_em_unlocked", false) || prefs.getBoolean("is_all_access_unlocked", false) || prefs.getBoolean("is_full_access_unlocked", false)
        if (!isUnlocked) return false
        val exp = getOfflineTrackExpiry(context, Track.ENGINE_MGT)
        val now = System.currentTimeMillis()
        val highWater = maxOf(prefs.getLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, 0L), prefs.getLong(KEY_OFFLINE_ACTIVATION_EPOCH, 0L))
        return exp > 0L && maxOf(now, highWater) < exp
    }

    fun isOfflineHyperionUnlocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        val allExp = prefs.getLong("offline_expiry_all", 0L)
        val now = System.currentTimeMillis()
        val highWater = maxOf(prefs.getLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, 0L), prefs.getLong(KEY_OFFLINE_ACTIVATION_EPOCH, 0L))
        val isAllAccessValid = (prefs.getBoolean("is_all_access_unlocked", false) || prefs.getBoolean("is_full_access_unlocked", false)) &&
            allExp > 0L && maxOf(now, highWater) < allExp

        return isAllAccessValid || (
            isOfflineOicNwUnlocked(context) &&
            isOfflineGmdssUnlocked(context) &&
            isOfflineEngineOicEwUnlocked(context) &&
            isOfflineDeckMgtUnlocked(context) &&
            isOfflineEngineMgtUnlocked(context)
        )
    }

    fun isOicNwProUnlocked(context: Context): Boolean {
        if (isDeveloperAdminAccessActive(context)) return true
        try {
            val billing = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
            if (billing.entitlements.value.oicNwUnlocked || billing.entitlements.value.allAccessUnlocked) {
                return true
            }
        } catch (_: Exception) {}
        return isAccountTrackUnlocked(context, Track.DECK_OIC_NW) || isOfflineOicNwUnlocked(context)
    }

    fun isGmdssProUnlocked(context: Context): Boolean {
        if (isDeveloperAdminAccessActive(context)) return true
        try {
            val billing = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
            if (billing.entitlements.value.gmdssUnlocked || billing.entitlements.value.allAccessUnlocked) {
                return true
            }
        } catch (_: Exception) {}
        return isAccountTrackUnlocked(context, Track.GMDSS_GOC) || isOfflineGmdssUnlocked(context)
    }

    fun isEngineOicEwProUnlocked(context: Context): Boolean {
        if (isDeveloperAdminAccessActive(context)) return true
        try {
            val billing = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
            if (billing.entitlements.value.engineOicEwUnlocked || billing.entitlements.value.allAccessUnlocked) {
                return true
            }
        } catch (_: Exception) {}
        return isAccountTrackUnlocked(context, Track.ENGINE_OIC_EW) || isOfflineEngineOicEwUnlocked(context)
    }

    fun isDeckMgtProUnlocked(context: Context): Boolean {
        if (isDeveloperAdminAccessActive(context)) return true
        try {
            val billing = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
            if (billing.entitlements.value.deckMgtUnlocked || billing.entitlements.value.allAccessUnlocked) {
                return true
            }
        } catch (_: Exception) {}
        return isAccountTrackUnlocked(context, Track.DECK_MGT) || isOfflineDeckMgtUnlocked(context)
    }

    fun isEngineMgtProUnlocked(context: Context): Boolean {
        if (isDeveloperAdminAccessActive(context)) return true
        try {
            val billing = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
            if (billing.entitlements.value.engineMgtUnlocked || billing.entitlements.value.allAccessUnlocked) {
                return true
            }
        } catch (_: Exception) {}
        return isAccountTrackUnlocked(context, Track.ENGINE_MGT) || isOfflineEngineMgtUnlocked(context)
    }

    fun isHyperionProUnlocked(context: Context): Boolean {
        if (isDeveloperAdminAccessActive(context)) return true
        try {
            val billing = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
            if (billing.entitlements.value.allAccessUnlocked || (billing.entitlements.value.oicNwUnlocked && billing.entitlements.value.gmdssUnlocked && billing.entitlements.value.engineOicEwUnlocked && billing.entitlements.value.deckMgtUnlocked && billing.entitlements.value.engineMgtUnlocked)) {
                return true
            }
        } catch (_: Exception) {}
        return Track.values().all { isAccountTrackUnlocked(context, it) } || isOfflineHyperionUnlocked(context)
    }

    fun isMasterProUnlocked(context: Context): Boolean = isHyperionProUnlocked(context)

    fun isTrackProUnlocked(context: Context, track: Track): Boolean {
        return when (track) {
            Track.DECK_OIC_NW -> isOicNwProUnlocked(context)
            Track.GMDSS_GOC -> isGmdssProUnlocked(context)
            Track.ENGINE_OIC_EW -> isEngineOicEwProUnlocked(context)
            Track.DECK_MGT -> isDeckMgtProUnlocked(context)
            Track.ENGINE_MGT -> isEngineMgtProUnlocked(context)
        }
    }

    fun hasAccessTo(context: Context, module: LicenseTier): Boolean {
        val oic = isOicNwProUnlocked(context)
        val gmdss = isGmdssProUnlocked(context)
        val ew = isEngineOicEwProUnlocked(context)
        val dm = isDeckMgtProUnlocked(context)
        val em = isEngineMgtProUnlocked(context)
        val hyp = isHyperionProUnlocked(context) || (oic && gmdss && ew && dm && em)
        return when (module) {
            LicenseTier.HYPERION_ALL_ACCESS -> hyp
            LicenseTier.OIC_NW -> oic || hyp
            LicenseTier.GMDSS -> gmdss || hyp
            LicenseTier.ENGINE_OIC_EW -> ew || hyp
            LicenseTier.DECK_MGT -> dm || hyp
            LicenseTier.ENGINE_MGT -> em || hyp
        }
    }

    fun isActivated(context: Context): Boolean {
        val active = isOicNwProUnlocked(context) || isGmdssProUnlocked(context) || isEngineOicEwProUnlocked(context) || isDeckMgtProUnlocked(context) || isEngineMgtProUnlocked(context)
        _isProActivatedFlow.value = active
        return active
    }

    fun setOicNwProUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OIC_NW_PRO_UNLOCKED, unlocked)
            .putBoolean(OIC_NW_LICENSED, unlocked)
            .apply()
        _isOicNwProUnlockedFlow.value = unlocked
        _isHyperionProUnlockedFlow.value = unlocked && _isGmdssProUnlockedFlow.value && _isEngineOicEwProUnlockedFlow.value && _isDeckMgtProUnlockedFlow.value && _isEngineMgtProUnlockedFlow.value
        _isProActivatedFlow.value = unlocked || _isGmdssProUnlockedFlow.value || _isEngineOicEwProUnlockedFlow.value || _isDeckMgtProUnlockedFlow.value || _isEngineMgtProUnlockedFlow.value
    }

    fun setGmdssProUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_GMDSS_PRO_UNLOCKED, unlocked)
            .putBoolean(GMDSS_LICENSED, unlocked)
            .apply()
        _isGmdssProUnlockedFlow.value = unlocked
        _isHyperionProUnlockedFlow.value = unlocked && _isOicNwProUnlockedFlow.value && _isEngineOicEwProUnlockedFlow.value && _isDeckMgtProUnlockedFlow.value && _isEngineMgtProUnlockedFlow.value
        _isProActivatedFlow.value = unlocked || _isOicNwProUnlockedFlow.value || _isEngineOicEwProUnlockedFlow.value || _isDeckMgtProUnlockedFlow.value || _isEngineMgtProUnlockedFlow.value
    }

    fun setEngineOicEwProUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENGINE_OIC_EW_PRO_UNLOCKED, unlocked)
            .putBoolean(ENGINE_OIC_EW_LICENSED, unlocked)
            .apply()
        _isEngineOicEwProUnlockedFlow.value = unlocked
        _isHyperionProUnlockedFlow.value = unlocked && _isOicNwProUnlockedFlow.value && _isGmdssProUnlockedFlow.value && _isDeckMgtProUnlockedFlow.value && _isEngineMgtProUnlockedFlow.value
        _isProActivatedFlow.value = unlocked || _isOicNwProUnlockedFlow.value || _isGmdssProUnlockedFlow.value || _isDeckMgtProUnlockedFlow.value || _isEngineMgtProUnlockedFlow.value
    }

    fun setDeckMgtProUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DECK_MGT_PRO_UNLOCKED, unlocked)
            .putBoolean(DECK_MGT_LICENSED, unlocked)
            .apply()
        _isDeckMgtProUnlockedFlow.value = unlocked
        _isHyperionProUnlockedFlow.value = unlocked && _isOicNwProUnlockedFlow.value && _isGmdssProUnlockedFlow.value && _isEngineOicEwProUnlockedFlow.value && _isEngineMgtProUnlockedFlow.value
        _isProActivatedFlow.value = unlocked || _isOicNwProUnlockedFlow.value || _isGmdssProUnlockedFlow.value || _isEngineOicEwProUnlockedFlow.value || _isEngineMgtProUnlockedFlow.value
    }

    fun setEngineMgtProUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENGINE_MGT_PRO_UNLOCKED, unlocked)
            .putBoolean(ENGINE_MGT_LICENSED, unlocked)
            .apply()
        _isEngineMgtProUnlockedFlow.value = unlocked
        _isHyperionProUnlockedFlow.value = unlocked && _isOicNwProUnlockedFlow.value && _isGmdssProUnlockedFlow.value && _isEngineOicEwProUnlockedFlow.value && _isDeckMgtProUnlockedFlow.value
        _isProActivatedFlow.value = unlocked || _isOicNwProUnlockedFlow.value || _isGmdssProUnlockedFlow.value || _isEngineOicEwProUnlockedFlow.value || _isDeckMgtProUnlockedFlow.value
    }

    fun setHyperionProUnlocked(context: Context, unlocked: Boolean) {
        setOicNwProUnlocked(context, unlocked)
        setGmdssProUnlocked(context, unlocked)
        setEngineOicEwProUnlocked(context, unlocked)
        setDeckMgtProUnlocked(context, unlocked)
        setEngineMgtProUnlocked(context, unlocked)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LICENSE_ACTIVATED, unlocked).apply()
        _isHyperionProUnlockedFlow.value = unlocked
        _isProActivatedFlow.value = unlocked
    }

    fun setMasterProUnlocked(context: Context, unlocked: Boolean) {
        setHyperionProUnlocked(context, unlocked)
    }

    fun setActivated(context: Context, activated: Boolean) {
        setHyperionProUnlocked(context, activated)
    }
}
