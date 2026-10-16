package com.nhyle.hyperionmaritime.data.util

import android.content.Context
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
        subtitle = "Complete Deck Operational + GMDSS Radio Reviewer package with all competencies",
        shortName = "All-Access Bundle",
        questionCount = "Total Question Bank: 8,705 questions (6,445 OIC-NW + 2,260 GMDSS)"
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
    TERMINAL_ACTIVATED("TERMINAL ACTIVATED // ACCESS GRANTED", true),
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
        if (isTampered) return "CLOCK TAMPERED // TIME RESTORATION REQUIRED"
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

    // Obfuscated salt fragments: XOR masked with 0x5A -> "HYPERION_MARITIME_SALT_2026_SECURE_AUTH"
    private val SALT_PART_A = byteArrayOf(0x12, 0x03, 0x0A, 0x1F, 0x08) // "HYPER"
    private val SALT_PART_B = byteArrayOf(0x13, 0x15, 0x14, 0x05) // "ION_"
    private val SALT_PART_C = byteArrayOf(0x17, 0x1B, 0x08, 0x13, 0x0E, 0x13, 0x17, 0x1F, 0x05) // "MARITIME_"
    private val SALT_PART_D = byteArrayOf(0x09, 0x1B, 0x16, 0x0E, 0x05) // "SALT_"
    private val SALT_PART_E = byteArrayOf(0x68, 0x6A, 0x68, 0x6C, 0x05) // "2026_"
    private val SALT_PART_F = byteArrayOf(0x09, 0x1F, 0x19, 0x0F, 0x08, 0x1F, 0x05) // "SECURE_"
    private val SALT_PART_G = byteArrayOf(0x1B, 0x0F, 0x0E, 0x12) // "AUTH"
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
        // Enforce removal of legacy permanent licenses
        purgeLegacyPermanentLicenses(context)

        // Evaluate current offline status
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

    fun computeValidKey(
        requestCode: String,
        prefix: String = "",
        salt: String = ""
    ): String {
        val cleanReq = requestCode.uppercase().trim()
        val digest = MessageDigest.getInstance("SHA-256")
        val inputBytes = if (salt.isEmpty()) cleanReq.toByteArray(Charsets.UTF_8) else (cleanReq + ":" + salt).toByteArray(Charsets.UTF_8)
        val hash = digest.digest(inputBytes)
        val hex = hash.joinToString("") { "%02X".format(it) }
        val chunk1 = hex.substring(0, 4)
        val chunk2 = hex.substring(4, 8)
        val chunk3 = hex.substring(8, 12)
        val chunk4 = hex.substring(12, 16)
        val formatted = "$chunk1-$chunk2-$chunk3-$chunk4"
        return if (prefix.isEmpty()) formatted else "$prefix-$formatted"
    }

    fun verifyActivationKey(requestCode: String, enteredKey: String): Boolean {
        val cleanInput = enteredKey.trim().replace("-", "").uppercase()
        val cleanCode = requestCode.trim().uppercase()

        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(cleanCode.toByteArray(Charsets.UTF_8))
        val hexString = digest.joinToString("") { "%02X".format(it) }
        val expectedKey = hexString.substring(0, 16).uppercase()

        return cleanInput == expectedKey
    }

    fun verifyKey(requestCode: String, enteredKey: String): Boolean {
        return verifyActivationKey(requestCode, enteredKey)
    }

    /**
     * Computes the 8-character cryptographic signature for the activation key.
     */
    fun computeSignature(cleanDeviceToken: String, tier: String, duration: String, issueDayHex: String): String {
        val payload = "$cleanDeviceToken:$tier:$duration:$issueDayHex:$SECRET_SALT"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(payload.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02X".format(it) }
        return hex.take(8).uppercase()
    }

    /**
     * Generates a valid offline activation key for a given request code, tier, and duration.
     */
    fun generateActivationKey(
        requestCodeOrDevice: String,
        tier: String,
        duration: String,
        issueDayHex: String? = null
    ): String {
        val cleanDevice = requestCodeOrDevice.uppercase()
            .removePrefix("HYPE-")
            .replace("HYPE", "")
            .replace("-", "")
            .trim()
            .take(12)

        val dayHex = issueDayHex ?: run {
            val days = (System.currentTimeMillis() / (24L * 60L * 60L * 1000L)).toInt()
            days.toString(16).uppercase().padStart(4, '0')
        }

        val normTier = when (tier.uppercase().trim()) {
            "OIC", "DECK", "OIC_NW" -> "OIC"
            "GOC", "GMDSS", "GMDSS_GOC" -> "GOC"
            "EW", "ENGINE_EW", "ENGINE_OIC_EW" -> "EW"
            "DM", "DECK_MGT" -> "DM"
            "EM", "ENGINE_MGT" -> "EM"
            else -> "ALL"
        }

        val normDur = when (duration.uppercase().trim()) {
            "1W", "WEEK", "7D" -> "1W"
            "1Y", "YEAR", "365D" -> "1Y"
            "EXP", "EX", "CUSTOM" -> "EXP"
            else -> "1M"
        }

        val sig = computeSignature(cleanDevice, normTier, normDur, dayHex)
        return "HYPE-$normTier-$normDur-$dayHex-${sig.substring(0, 4)}-${sig.substring(4, 8)}"
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

        val highWaterMark = maxOf(lastVerifiedEpoch, activationEpoch, issueEpoch)

        // LAYER 1: Current system clock is set substantially before the license was even created (48h grace for global timezones)
        val timezoneGraceMs = 48L * 60L * 60L * 1000L
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
                highWaterMarkEpoch = highWaterMark
            )
            return OfflineLicenseStatus.ClockTampered(info.tamperReason!!)
        }

        // LAYER 2: Monotonic High-Water Mark (Clock Rollback Latch)
        // Allow up to 60 seconds grace for minor time adjustments / timezone sync
        if (highWaterMark > 0L && now < (highWaterMark - 60_000L)) {
            prefs.edit().putBoolean(KEY_OFFLINE_CLOCK_TAMPERED, true).apply()
            val info = OfflineLicenseInfo(
                tier = tier,
                duration = duration,
                activationEpoch = activationEpoch,
                issueEpoch = issueEpoch,
                expiryEpoch = expiryEpoch,
                isTampered = true,
                tamperReason = "Device clock rollback detected. Restore correct device time.",
                highWaterMarkEpoch = highWaterMark
            )
            return OfflineLicenseStatus.ClockTampered(info.tamperReason!!)
        }

        // LAYER 3: Hardware Monotonic Clock (Uptime Delta)
        if (lastElapsedRealtime > 0L && currentElapsed >= lastElapsedRealtime) {
            val deltaWallTime = now - lastVerifiedEpoch
            if (deltaWallTime < -60_000L) {
                prefs.edit().putBoolean(KEY_OFFLINE_CLOCK_TAMPERED, true).apply()
                val info = OfflineLicenseInfo(
                    tier = tier,
                    duration = duration,
                    activationEpoch = activationEpoch,
                    issueEpoch = issueEpoch,
                    expiryEpoch = expiryEpoch,
                    isTampered = true,
                    tamperReason = "Device clock manipulation detected while device was running.",
                    highWaterMarkEpoch = highWaterMark
                )
                return OfflineLicenseStatus.ClockTampered(info.tamperReason!!)
            }
        }

        // Restore tamper flag if valid system time has been restored
        if (prefs.getBoolean(KEY_OFFLINE_CLOCK_TAMPERED, false)) {
            prefs.edit().putBoolean(KEY_OFFLINE_CLOCK_TAMPERED, false).apply()
        }

        val effectiveNow = maxOf(now, highWaterMark)

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
            highWaterMarkEpoch = highWaterMark
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

        // Developer Test Override: Grants 30-day All Access pass for local QA
        if (cleanInput == "ADMIN1234") {
            val now = System.currentTimeMillis()
            val expiry = now + (30L * 24L * 60L * 60L * 1000L)
            saveOfflineActivation(
                context = context,
                tier = "ALL_ACCESS",
                duration = "1M",
                activationEpoch = now,
                issueEpoch = now,
                expiryEpoch = expiry,
                unlockOic = true,
                unlockGmdss = true,
                unlockEw = true,
                unlockDm = true,
                unlockEm = true,
                unlockAll = true
            )
            return UnlockResult.TERMINAL_ACTIVATED
        }

        // Parse key structure: HYPE-[TIER]-[DURATION]-[ISSUEDAY/EXPDAY]-[SIG1]-[SIG2]
        // or compact HYPE[TIER][DUR][ISSUEDAY/EXPDAY][SIG]
        val regexWithDashes = Regex("^HYPE-(ALL|OIC|GOC|EW|DM|EM)-(1W|1M|1Y|EXP|EX)-([0-9A-F]{4})-([0-9A-F]{4})-([0-9A-F]{4})$")
        val regexCompact = Regex("^HYPE(ALL|OIC|GOC|EW|DM|EM)(1W|1M|1Y|EXP|EX)([0-9A-F]{4})([0-9A-F]{8})$")

        var tierPart = ""
        var durPart = ""
        var dayPart = ""
        var sigPart = ""

        val matchDashes = regexWithDashes.find(cleanInput)
        if (matchDashes != null) {
            tierPart = matchDashes.groupValues[1]
            durPart = matchDashes.groupValues[2]
            dayPart = matchDashes.groupValues[3]
            sigPart = matchDashes.groupValues[4] + matchDashes.groupValues[5]
        } else {
            val matchCompact = regexCompact.find(cleanInput)
            if (matchCompact != null) {
                tierPart = matchCompact.groupValues[1]
                durPart = matchCompact.groupValues[2]
                dayPart = matchCompact.groupValues[3]
                sigPart = matchCompact.groupValues[4]
            }
        }

        if (tierPart.isEmpty() || durPart.isEmpty() || dayPart.isEmpty() || sigPart.isEmpty()) {
            return UnlockResult.INVALID_KEY
        }

        val cleanDevice = getDeviceCleanToken(context)
        val expectedSig = computeSignature(cleanDevice, tierPart, durPart, dayPart)

        if (sigPart != expectedSig) {
            return UnlockResult.INVALID_KEY
        }

        val now = System.currentTimeMillis()

        val expiryEpoch: Long
        val issueEpoch: Long
        val storedDuration: String

        if (durPart == "EXP" || durPart == "EX") {
            // dayPart encodes the exact target expiration calendar date in epoch days
            val expDayEpoch = try {
                dayPart.toLong(16) * (24L * 60L * 60L * 1000L)
            } catch (_: Exception) {
                return UnlockResult.INVALID_KEY
            }

            // Expiration is set to the very end of that chosen day (23:59:59.999 UTC)
            val expCeiling = expDayEpoch + (23L * 3600L * 1000L + 59L * 60L * 1000L + 59L * 1000L)
            if (now >= expCeiling) {
                return UnlockResult.KEY_EXPIRED
            }

            // Clock sanity: device clock must not be set back to before year 2024
            if (now < 1704067200000L) {
                return UnlockResult.CLOCK_ERROR
            }

            expiryEpoch = expCeiling
            issueEpoch = now
            storedDuration = "CUSTOM"
        } else {
            val parsedIssueEpoch = try {
                dayPart.toLong(16) * (24L * 60L * 60L * 1000L)
            } catch (_: Exception) {
                return UnlockResult.INVALID_KEY
            }

            // LAYER 1 Check: System clock substantially earlier than issue date (48h global timezone grace window)
            val timezoneGraceMs = 48L * 60L * 60L * 1000L
            if (now < (parsedIssueEpoch - timezoneGraceMs)) {
                return UnlockResult.CLOCK_ERROR
            }

            val durationMs = when (durPart) {
                "1W" -> 7L * 24L * 60L * 60L * 1000L
                "1Y" -> 365L * 24L * 60L * 60L * 1000L
                else -> 30L * 24L * 60L * 60L * 1000L // "1M" default
            }

            // The absolute ceiling past which the key can no longer be activated or reused
            val absoluteKeyCeiling = parsedIssueEpoch + durationMs + (12L * 60L * 60L * 1000L)
            if (now >= absoluteKeyCeiling) {
                return UnlockResult.KEY_EXPIRED
            }

            // Candidate gets at most durationMs from now, capped by the key's absolute ceiling.
            expiryEpoch = minOf(now + durationMs, absoluteKeyCeiling)
            issueEpoch = parsedIssueEpoch
            storedDuration = durPart
        }

        val (unlockOic, unlockGmdss, unlockEw, unlockDm, unlockEm, unlockAll, resolvedTier) = when (tierPart) {
            "OIC" -> Septuple(true, false, false, false, false, false, "OIC_NW")
            "GOC" -> Septuple(false, true, false, false, false, false, "GMDSS_GOC")
            "EW" -> Septuple(false, false, true, false, false, false, "ENGINE_OIC_EW")
            "DM" -> Septuple(false, false, false, true, false, false, "DECK_MGT")
            "EM" -> Septuple(false, false, false, false, true, false, "ENGINE_MGT")
            else -> Septuple(true, true, true, true, true, true, "ALL_ACCESS")
        }

        saveOfflineActivation(
            context = context,
            tier = resolvedTier,
            duration = storedDuration,
            activationEpoch = now,
            issueEpoch = issueEpoch,
            expiryEpoch = expiryEpoch,
            unlockOic = unlockOic,
            unlockGmdss = unlockGmdss,
            unlockEw = unlockEw,
            unlockDm = unlockDm,
            unlockEm = unlockEm,
            unlockAll = unlockAll
        )

        return when (tierPart) {
            "OIC" -> UnlockResult.OIC_NW_UNLOCKED
            "GOC" -> UnlockResult.GMDSS_UNLOCKED
            "EW" -> UnlockResult.ENGINE_OIC_EW_UNLOCKED
            "DM" -> UnlockResult.DECK_MGT_UNLOCKED
            "EM" -> UnlockResult.ENGINE_MGT_UNLOCKED
            else -> UnlockResult.HYPERION_UNLOCKED
        }
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
        prefs.edit()
            .putBoolean(KEY_OFFLINE_LICENSE, true)
            .putString("license_type", "OFFLINE_PASS")
            .putString(KEY_OFFLINE_TIER, tier)
            .putString(KEY_OFFLINE_DURATION, duration)
            .putLong(KEY_OFFLINE_ACTIVATION_EPOCH, activationEpoch)
            .putLong(KEY_OFFLINE_ISSUE_EPOCH, issueEpoch)
            .putLong(KEY_OFFLINE_EXPIRY_EPOCH, expiryEpoch)
            .putLong(KEY_OFFLINE_LAST_VERIFIED_EPOCH, activationEpoch)
            .putLong(KEY_OFFLINE_LAST_ELAPSED_REALTIME, SystemClock.elapsedRealtime())
            .putBoolean(KEY_OFFLINE_CLOCK_TAMPERED, false)
            .putBoolean("is_licensed", true)
            .putBoolean("is_oic_unlocked", unlockOic)
            .putBoolean("is_gmdss_unlocked", unlockGmdss)
            .putBoolean("is_engine_ew_unlocked", unlockEw)
            .putBoolean("is_deck_mgt_unlocked", unlockDm)
            .putBoolean("is_engine_mgt_unlocked", unlockEm)
            .putBoolean("is_all_access_unlocked", unlockAll)
            .putBoolean("is_full_access_unlocked", unlockAll)
            .remove("is_hardware_lifetime")
            .remove("is_fully_activated")
            .apply()

        setOicNwProUnlocked(context, unlockOic)
        setGmdssProUnlocked(context, unlockGmdss)
        setEngineOicEwProUnlocked(context, unlockEw)
        setDeckMgtProUnlocked(context, unlockDm)
        setEngineMgtProUnlocked(context, unlockEm)
        setHyperionProUnlocked(context, unlockAll)
    }

    fun revokeOfflineAccess(context: Context) {
        val prefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_OFFLINE_LICENSE, false)
            .putString("license_type", "RESTRICTED")
            .remove(KEY_OFFLINE_EXPIRY_EPOCH)
            .remove(KEY_OFFLINE_ACTIVATION_EPOCH)
            .putBoolean("is_licensed", false)
            .putBoolean("is_oic_unlocked", false)
            .putBoolean("is_gmdss_unlocked", false)
            .putBoolean("is_engine_ew_unlocked", false)
            .putBoolean("is_deck_mgt_unlocked", false)
            .putBoolean("is_engine_mgt_unlocked", false)
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

    fun isOfflineOicNwUnlocked(context: Context): Boolean {
        val status = checkOfflineLicense(context)
        return when (status) {
            is OfflineLicenseStatus.Active -> {
                val tier = status.info.tier
                tier == "ALL_ACCESS" || tier == "OIC_NW" || tier == "ALL" || tier == "OIC"
            }
            is OfflineLicenseStatus.ClockTampered, is OfflineLicenseStatus.Expired -> false
            OfflineLicenseStatus.NotActive -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getBoolean(KEY_OIC_NW_PRO_UNLOCKED, false)
            }
        }
    }

    fun isOfflineGmdssUnlocked(context: Context): Boolean {
        val status = checkOfflineLicense(context)
        return when (status) {
            is OfflineLicenseStatus.Active -> {
                val tier = status.info.tier
                tier == "ALL_ACCESS" || tier == "GMDSS_GOC" || tier == "ALL" || tier == "GOC"
            }
            is OfflineLicenseStatus.ClockTampered, is OfflineLicenseStatus.Expired -> false
            OfflineLicenseStatus.NotActive -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getBoolean(KEY_GMDSS_PRO_UNLOCKED, false)
            }
        }
    }

    fun isOfflineEngineOicEwUnlocked(context: Context): Boolean {
        val status = checkOfflineLicense(context)
        return when (status) {
            is OfflineLicenseStatus.Active -> {
                val tier = status.info.tier
                tier == "ALL_ACCESS" || tier == "ENGINE_OIC_EW" || tier == "ALL" || tier == "EW"
            }
            is OfflineLicenseStatus.ClockTampered, is OfflineLicenseStatus.Expired -> false
            OfflineLicenseStatus.NotActive -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getBoolean(KEY_ENGINE_OIC_EW_PRO_UNLOCKED, false)
            }
        }
    }

    fun isOfflineDeckMgtUnlocked(context: Context): Boolean {
        val status = checkOfflineLicense(context)
        return when (status) {
            is OfflineLicenseStatus.Active -> {
                val tier = status.info.tier
                tier == "ALL_ACCESS" || tier == "DECK_MGT" || tier == "ALL" || tier == "DM"
            }
            is OfflineLicenseStatus.ClockTampered, is OfflineLicenseStatus.Expired -> false
            OfflineLicenseStatus.NotActive -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getBoolean(KEY_DECK_MGT_PRO_UNLOCKED, false)
            }
        }
    }

    fun isOfflineEngineMgtUnlocked(context: Context): Boolean {
        val status = checkOfflineLicense(context)
        return when (status) {
            is OfflineLicenseStatus.Active -> {
                val tier = status.info.tier
                tier == "ALL_ACCESS" || tier == "ENGINE_MGT" || tier == "ALL" || tier == "EM"
            }
            is OfflineLicenseStatus.ClockTampered, is OfflineLicenseStatus.Expired -> false
            OfflineLicenseStatus.NotActive -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getBoolean(KEY_ENGINE_MGT_PRO_UNLOCKED, false)
            }
        }
    }

    fun isOfflineHyperionUnlocked(context: Context): Boolean {
        val status = checkOfflineLicense(context)
        return when (status) {
            is OfflineLicenseStatus.Active -> {
                val tier = status.info.tier
                tier == "ALL_ACCESS" || tier == "ALL"
            }
            is OfflineLicenseStatus.ClockTampered, is OfflineLicenseStatus.Expired -> false
            OfflineLicenseStatus.NotActive -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getBoolean(KEY_LICENSE_ACTIVATED, false)
            }
        }
    }

    fun isOicNwProUnlocked(context: Context): Boolean {
        try {
            val billing = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
            if (billing.entitlements.value.oicNwUnlocked || billing.entitlements.value.allAccessUnlocked) {
                return true
            }
        } catch (_: Exception) {}
        return isOfflineOicNwUnlocked(context)
    }

    fun isGmdssProUnlocked(context: Context): Boolean {
        try {
            val billing = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
            if (billing.entitlements.value.gmdssUnlocked || billing.entitlements.value.allAccessUnlocked) {
                return true
            }
        } catch (_: Exception) {}
        return isOfflineGmdssUnlocked(context)
    }

    fun isEngineOicEwProUnlocked(context: Context): Boolean {
        try {
            val billing = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
            if (billing.entitlements.value.engineOicEwUnlocked || billing.entitlements.value.allAccessUnlocked) {
                return true
            }
        } catch (_: Exception) {}
        return isOfflineEngineOicEwUnlocked(context)
    }

    fun isDeckMgtProUnlocked(context: Context): Boolean {
        try {
            val billing = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
            if (billing.entitlements.value.deckMgtUnlocked || billing.entitlements.value.allAccessUnlocked) {
                return true
            }
        } catch (_: Exception) {}
        return isOfflineDeckMgtUnlocked(context)
    }

    fun isEngineMgtProUnlocked(context: Context): Boolean {
        try {
            val billing = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
            if (billing.entitlements.value.engineMgtUnlocked || billing.entitlements.value.allAccessUnlocked) {
                return true
            }
        } catch (_: Exception) {}
        return isOfflineEngineMgtUnlocked(context)
    }

    fun isHyperionProUnlocked(context: Context): Boolean {
        try {
            val billing = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
            if (billing.entitlements.value.allAccessUnlocked || (billing.entitlements.value.oicNwUnlocked && billing.entitlements.value.gmdssUnlocked && billing.entitlements.value.engineOicEwUnlocked && billing.entitlements.value.deckMgtUnlocked && billing.entitlements.value.engineMgtUnlocked)) {
                return true
            }
        } catch (_: Exception) {}
        return isOfflineHyperionUnlocked(context)
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

    fun validateLegacyKeyDirect(context: Context, cleanKey: String): Boolean {
        return verifyAndUnlockKey(context, cleanKey).isSuccess
    }
}
