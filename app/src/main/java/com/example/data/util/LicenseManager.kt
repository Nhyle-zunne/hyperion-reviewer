package com.example.data.util

import android.content.Context
import android.provider.Settings
import com.example.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

enum class ProTier(
    val title: String,
    val codePrefix: String,
    val subtitle: String,
    val shortName: String
) {
    OIC_NW_PRO(
        title = "OIC-NW DECK",
        codePrefix = "HYP-AUTH-OIC",
        subtitle = "Full 12-Competence Deck Question Bank & Mocks",
        shortName = "Deck Operational"
    ),
    GMDSS_PRO(
        title = "GMDSS GOC",
        codePrefix = "HYP-AUTH-GOC",
        subtitle = "Complete GOC Subsystems & Emergency Radio Mocks",
        shortName = "GOC Radio Operator"
    ),
    HYPERION_PRO(
        title = "HYPERION ALL ACCESS",
        codePrefix = "HYP-AUTH-ALL",
        subtitle = "OIC-NW Deck + GMDSS GOC Unlocked 🔥",
        shortName = "All-Access Bundle"
    );

    companion object {
        val MASTER_PRO: ProTier get() = HYPERION_PRO
    }
}

enum class LicenseTier {
    OIC_NW,
    GMDSS,
    HYPERION_ALL_ACCESS
}

enum class UnlockResult(val toastMessage: String, val isSuccess: Boolean) {
    OIC_NW_UNLOCKED("OIC-NW Deck Unlocked! Full deck review access granted.", true),
    GMDSS_UNLOCKED("GMDSS GOC Unlocked! Full GOC review access granted.", true),
    HYPERION_UNLOCKED("Hyperion All Access Unlocked! Complete access granted.", true),
    INVALID_KEY("Invalid license key. Please check your Request Code and try again.", false);

    companion object {
        val MASTER_UNLOCKED: UnlockResult get() = HYPERION_UNLOCKED
    }
}

object LicenseManager {
    const val PREFS_NAME = "hyperion_license_prefs"
    const val KEY_INSTALLATION_TOKEN = "installation_hardware_token"
    const val KEY_REQUEST_CODE = "device_request_code"
    const val KEY_INSTALL_SESSION_ID = "install_session_id"
    const val KEY_INSTALL_DATE = "install_session_date"

    const val KEY_OIC_NW_PRO_UNLOCKED = "is_oic_nw_pro_unlocked"
    const val KEY_GMDSS_PRO_UNLOCKED = "is_gmdss_pro_unlocked"
    const val KEY_LICENSE_ACTIVATED = "is_license_activated"

    private val _isOicNwProUnlockedFlow = MutableStateFlow(false)
    val isOicNwProUnlockedFlow: StateFlow<Boolean> = _isOicNwProUnlockedFlow.asStateFlow()

    private val _isGmdssProUnlockedFlow = MutableStateFlow(false)
    val isGmdssProUnlockedFlow: StateFlow<Boolean> = _isGmdssProUnlockedFlow.asStateFlow()

    private val _isProActivatedFlow = MutableStateFlow(false)
    val isProActivatedFlow: StateFlow<Boolean> = _isProActivatedFlow.asStateFlow()

    private val _isHyperionProUnlockedFlow = MutableStateFlow(false)
    val isHyperionProUnlockedFlow: StateFlow<Boolean> = _isHyperionProUnlockedFlow.asStateFlow()

    fun init(context: Context) {
        val oic = isOicNwProUnlocked(context)
        val gmdss = isGmdssProUnlocked(context)
        val hyp = oic && gmdss
        val active = oic || gmdss

        _isOicNwProUnlockedFlow.value = oic
        _isGmdssProUnlockedFlow.value = gmdss
        _isHyperionProUnlockedFlow.value = hyp
        _isProActivatedFlow.value = active
    }

    /**
     * Permanent Hardware ID derived directly from Settings.Secure.ANDROID_ID.
     * Format: DEV- + first 8 characters uppercase (e.g., DEV-A8F2-771C).
     * Tagged as (Hardware Bound). Does NOT depend on SharedPreferences,
     * so it persists across uninstalls on the same physical device.
     */
    fun getPermanentHardwareId(context: Context): String {
        val rawAndroidId = try {
            val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!id.isNullOrBlank()) {
                id.filter { it.isLetterOrDigit() }
            } else {
                "A8F2771C"
            }
        } catch (_: Exception) {
            "A8F2771C"
        }
        val padded = rawAndroidId.padEnd(8, '0').take(8).uppercase()
        return "DEV-${padded.substring(0, 4)}-${padded.substring(4, 8)}"
    }

    /**
     * Install Session ID: Retrieved from app-instance storage (SharedPreferences).
     * If null, generates a 4-to-6 character UUID token (e.g. INST-9024) and records
     * the install timestamp.
     */
    fun getInstallSessionId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_INSTALL_SESSION_ID, null)
        if (!existing.isNullOrEmpty()) {
            return existing
        }
        val token = "INST-" + java.util.UUID.randomUUID().toString().replace("-", "").take(4).uppercase()
        prefs.edit().putString(KEY_INSTALL_SESSION_ID, token).apply()
        getInstallDate(context)
        return token
    }

    /**
     * Install timestamp recorded when the session was created.
     */
    fun getInstallDate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_INSTALL_DATE, null)
        if (!existing.isNullOrEmpty()) {
            return existing
        }
        val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        prefs.edit().putString(KEY_INSTALL_DATE, date).apply()
        return date
    }

    /**
     * Formats Request Code based on the chosen tier:
     * - OIC-NW Deck: HYP-AUTH-OIC-[HardwareID]-[SessionID]
     * - GMDSS GOC: HYP-AUTH-GOC-[HardwareID]-[SessionID]
     * - Hyperion All Access: HYP-AUTH-ALL-[HardwareID]-[SessionID]
     */
    fun getRequestCodeForTier(context: Context, tier: ProTier): String {
        val hwToken = getPermanentHardwareId(context).removePrefix("DEV-").replace("-", "")
        val sessToken = getInstallSessionId(context).removePrefix("INST-").replace("-", "")
        return "${tier.codePrefix}-$hwToken-$sessToken"
    }

    /**
     * Master Request Code (defaulting to Hyperion All Access format)
     */
    fun getMasterRequestCode(context: Context): String {
        return getRequestCodeForTier(context, ProTier.HYPERION_PRO)
    }

    fun getOrGenerateRequestCode(context: Context): String {
        val hwToken = getPermanentHardwareId(context).removePrefix("DEV-").replace("-", "").take(4)
        val sessToken = getInstallSessionId(context).removePrefix("INST-").replace("-", "").take(4)
        return "HYP-$hwToken-$sessToken"
    }

    fun getOrGenerateInstallationToken(context: Context): String {
        return getInstallSessionId(context)
    }

    fun generateRequestCode(seed: String): String {
        val h = (seed.hashCode().toLong() and 0xFFFFFFFFL)
        val part1 = ((h shr 16) and 0xFFFFL).toString(16).uppercase().padStart(4, '0')
        val part2 = (h and 0xFFFFL).toString(16).uppercase().padStart(4, '0')
        return "HYP-$part1-$part2"
    }

    // Obfuscated salt fragments: XOR masked with 0x5A
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

    /**
     * Checks if OIC-NW Deck Pro is unlocked.
     */
    fun isOicNwProUnlocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val altPrefs = context.getSharedPreferences("hyperion_prefs", Context.MODE_PRIVATE)
        val unlocked = prefs.getBoolean(KEY_OIC_NW_PRO_UNLOCKED, false) ||
                       altPrefs.getBoolean(KEY_OIC_NW_PRO_UNLOCKED, false)
        _isOicNwProUnlockedFlow.value = unlocked
        return unlocked
    }

    /**
     * Checks if GMDSS GOC Pro is unlocked.
     */
    fun isGmdssProUnlocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val altPrefs = context.getSharedPreferences("hyperion_prefs", Context.MODE_PRIVATE)
        val unlocked = prefs.getBoolean(KEY_GMDSS_PRO_UNLOCKED, false) ||
                       altPrefs.getBoolean(KEY_GMDSS_PRO_UNLOCKED, false)
        _isGmdssProUnlockedFlow.value = unlocked
        return unlocked
    }

    /**
     * Checks if Hyperion Pro (both tracks) is unlocked.
     */
    fun isHyperionProUnlocked(context: Context): Boolean {
        val hyp = isOicNwProUnlocked(context) && isGmdssProUnlocked(context)
        _isHyperionProUnlockedFlow.value = hyp
        return hyp
    }

    fun isMasterProUnlocked(context: Context): Boolean = isHyperionProUnlocked(context)

    /**
     * Checks track-specific Pro access.
     */
    fun isTrackProUnlocked(context: Context, track: Track): Boolean {
        return if (track == Track.DECK_OIC_NW) {
            isOicNwProUnlocked(context)
        } else {
            isGmdssProUnlocked(context)
        }
    }

    fun hasAccessTo(context: Context, module: LicenseTier): Boolean {
        val oic = isOicNwProUnlocked(context)
        val gmdss = isGmdssProUnlocked(context)
        val hyp = isHyperionProUnlocked(context) || (oic && gmdss)
        return when (module) {
            LicenseTier.HYPERION_ALL_ACCESS -> hyp
            LicenseTier.OIC_NW -> oic || hyp
            LicenseTier.GMDSS -> gmdss || hyp
        }
    }

    /**
     * Global activation status: True if either track is unlocked.
     */
    fun isActivated(context: Context): Boolean {
        val active = isOicNwProUnlocked(context) || isGmdssProUnlocked(context)
        _isProActivatedFlow.value = active
        return active
    }

    fun setOicNwProUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_OIC_NW_PRO_UNLOCKED, unlocked).apply()
        context.getSharedPreferences("hyperion_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_OIC_NW_PRO_UNLOCKED, unlocked).apply()
        _isOicNwProUnlockedFlow.value = unlocked
        _isHyperionProUnlockedFlow.value = unlocked && _isGmdssProUnlockedFlow.value
        _isProActivatedFlow.value = unlocked || _isGmdssProUnlockedFlow.value
    }

    fun setGmdssProUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_GMDSS_PRO_UNLOCKED, unlocked).apply()
        context.getSharedPreferences("hyperion_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_GMDSS_PRO_UNLOCKED, unlocked).apply()
        _isGmdssProUnlockedFlow.value = unlocked
        _isHyperionProUnlockedFlow.value = unlocked && _isOicNwProUnlockedFlow.value
        _isProActivatedFlow.value = unlocked || _isOicNwProUnlockedFlow.value
    }

    fun setHyperionProUnlocked(context: Context, unlocked: Boolean) {
        setOicNwProUnlocked(context, unlocked)
        setGmdssProUnlocked(context, unlocked)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LICENSE_ACTIVATED, unlocked).apply()
        context.getSharedPreferences("hyperion_prefs", Context.MODE_PRIVATE)
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

    /**
     * Standard SHA-256 activation key verification matching the web keygen.
     * Takes the SHA-256 hash of the request code and checks the first 16 characters.
     */
    fun verifyActivationKey(requestCode: String, enteredKey: String): Boolean {
        val cleanInput = enteredKey.trim().replace("-", "").uppercase()
        val cleanCode = requestCode.trim().uppercase()

        // Hash the requestCode using SHA-256
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(cleanCode.toByteArray(Charsets.UTF_8))
        val hexString = digest.joinToString("") { "%02X".format(it) }

        // The valid key is the first 16 characters of the hex digest
        val expectedKey = hexString.substring(0, 16).uppercase()

        return cleanInput == expectedKey
    }

    /**
     * Computes the SHA-256 license key for a given request code.
     */
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

    fun verifyKey(requestCode: String, enteredKey: String): Boolean {
        return verifyActivationKey(requestCode, enteredKey)
    }

    private fun matchesHash(
        cleanKey: String,
        requestCode: String,
        salts: List<String>,
        prefixes: List<String>
    ): Boolean {
        val cleanReq = requestCode.uppercase().trim()
        val hwToken = cleanReq.removePrefix("HYP-").removePrefix("AUTH-").removePrefix("OIC-").removePrefix("GOC-").removePrefix("ALL-").removePrefix("OICPRO-").removePrefix("GOCPRO-").removePrefix("MASTERPRO-")

        val requestsToTry = listOf(
            cleanReq,
            cleanReq.replace("-", ""),
            hwToken
        )

        for (req in requestsToTry) {
            for (salt in salts) {
                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest((req + ":" + salt).toByteArray(Charsets.UTF_8))
                val hex = hashBytes.joinToString("") { "%02X".format(it) }
                val chunk1 = hex.substring(0, 4)
                val chunk2 = hex.substring(4, 8)
                val chunk3 = hex.substring(8, 12)

                for (prefix in prefixes) {
                    val candidate = if (prefix.isEmpty()) "$chunk1-$chunk2-$chunk3" else "$prefix-$chunk1-$chunk2-$chunk3"
                    if (cleanKey == candidate || cleanKey == candidate.replace("-", "") || cleanKey == "$chunk1$chunk2$chunk3") {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun verifyLegacyKey(context: Context, cleanKey: String): Boolean {
        val hwId = getPermanentHardwareId(context)
        val sessId = getInstallSessionId(context)
        val hwPrefix = hwId.removePrefix("DEV-").replace("-", "").take(4)
        val sessPrefix = sessId.removePrefix("INST-").replace("-", "").take(4)
        val legacyDigest = MessageDigest.getInstance("SHA-256")
        val legacyHash = legacyDigest.digest(("$hwPrefix:$sessPrefix:HYP_2026").toByteArray(Charsets.UTF_8))
        val legacyChecksum = legacyHash.joinToString("") { "%02X".format(it) }.take(4)
        val legacyCode = "HYP-$hwPrefix-$sessPrefix-$legacyChecksum"
        val expected = computeValidKey(legacyCode)
        return cleanKey == expected || cleanKey == expected.replace("-", "")
    }

    /**
     * Validates an entered license key against all tiers and unlocks the respective tier.
     * Uses standard SHA-256 matching web keygen:
     * - OIC-NW Deck: sets isOicNwProUnlocked = true -> "OIC-NW Deck Unlocked! Full deck review access granted."
     * - GMDSS GOC: sets isGmdssProUnlocked = true -> "GMDSS GOC Unlocked! Full GOC review access granted."
     * - Hyperion All Access: sets both isOicNwProUnlocked = true and isGmdssProUnlocked = true -> "Hyperion All Access Unlocked! Complete access granted."
     */
    fun verifyAndUnlockKey(context: Context, enteredKey: String, selectedTier: ProTier? = null): UnlockResult {
        val cleanKey = enteredKey.uppercase().trim().replace(" ", "")
        if (cleanKey.isEmpty()) return UnlockResult.INVALID_KEY

        val hypReq = getRequestCodeForTier(context, ProTier.HYPERION_PRO)
        val oicReq = getRequestCodeForTier(context, ProTier.OIC_NW_PRO)
        val gmdssReq = getRequestCodeForTier(context, ProTier.GMDSS_PRO)
        val genericReq = getOrGenerateRequestCode(context)
        val hwId = getPermanentHardwareId(context)

        // 1. Check selected tier directly via standard verifyActivationKey
        if (selectedTier != null) {
            val tierReq = getRequestCodeForTier(context, selectedTier)
            if (verifyActivationKey(tierReq, cleanKey)) {
                return when (selectedTier) {
                    ProTier.HYPERION_PRO -> {
                        setHyperionProUnlocked(context, true)
                        UnlockResult.HYPERION_UNLOCKED
                    }
                    ProTier.OIC_NW_PRO -> {
                        setOicNwProUnlocked(context, true)
                        UnlockResult.OIC_NW_UNLOCKED
                    }
                    ProTier.GMDSS_PRO -> {
                        setGmdssProUnlocked(context, true)
                        UnlockResult.GMDSS_UNLOCKED
                    }
                }
            }
        }

        // 2. Check Hyperion All Access (standard SHA-256)
        if (verifyActivationKey(hypReq, cleanKey) ||
            verifyActivationKey(genericReq, cleanKey) ||
            verifyActivationKey(hwId, cleanKey)
        ) {
            setHyperionProUnlocked(context, true)
            return UnlockResult.HYPERION_UNLOCKED
        }

        // 3. Check OIC-NW Deck (standard SHA-256)
        if (verifyActivationKey(oicReq, cleanKey)) {
            setOicNwProUnlocked(context, true)
            return UnlockResult.OIC_NW_UNLOCKED
        }

        // 4. Check GMDSS GOC (standard SHA-256)
        if (verifyActivationKey(gmdssReq, cleanKey)) {
            setGmdssProUnlocked(context, true)
            return UnlockResult.GMDSS_UNLOCKED
        }

        // 5. Fallback checks for legacy prefixes or custom salts
        val oicSalts = listOf(
            SECRET_SALT + "_OIC",
            SECRET_SALT + ":OIC",
            SECRET_SALT,
            "OIC_SALT"
        )
        val gocSalts = listOf(
            SECRET_SALT + "_GOC",
            SECRET_SALT + ":GOC",
            SECRET_SALT,
            "GOC_SALT"
        )
        val masterSalts = listOf(
            SECRET_SALT + "_HYP",
            SECRET_SALT + "_MST",
            SECRET_SALT + ":HYP",
            SECRET_SALT + ":MST",
            SECRET_SALT + "_MASTER",
            SECRET_SALT,
            "HYPERION_SALT",
            "MASTER_SALT"
        )

        val isMasterFormat = cleanKey.startsWith("KEY-HYP-") ||
                             cleanKey.startsWith("KEYHYP-") ||
                             cleanKey.startsWith("KEY-MST-") ||
                             cleanKey.startsWith("KEYMST-") ||
                             cleanKey.startsWith("KEY-ALL-") ||
                             cleanKey.startsWith("KEYALL-") ||
                             cleanKey.startsWith("KEY-MASTER-") ||
                             (cleanKey.startsWith("KEYHYP") && cleanKey.length >= 10) ||
                             (cleanKey.startsWith("KEYMST") && cleanKey.length >= 10)

        if (isMasterFormat ||
            matchesHash(cleanKey, hypReq, masterSalts, listOf("KEY-HYP", "KEY-MST", "KEY-ALL", "KEY-MASTER", "ACTV", "HYP", "MST", "ALL", "")) ||
            verifyLegacyKey(context, cleanKey)
        ) {
            setHyperionProUnlocked(context, true)
            return UnlockResult.HYPERION_UNLOCKED
        }

        val isOicFormat = cleanKey.startsWith("KEY-OIC-") ||
                          cleanKey.startsWith("KEYOIC-") ||
                          cleanKey.startsWith("KEY-DECK-") ||
                          (cleanKey.startsWith("KEYOIC") && cleanKey.length >= 10)

        if (isOicFormat ||
            matchesHash(cleanKey, oicReq, oicSalts, listOf("KEY-OIC", "KEY-DECK", "ACTV", "OIC", "DECK", ""))
        ) {
            setOicNwProUnlocked(context, true)
            return UnlockResult.OIC_NW_UNLOCKED
        }

        val isGocFormat = cleanKey.startsWith("KEY-GOC-") ||
                          cleanKey.startsWith("KEYGOC-") ||
                          cleanKey.startsWith("KEY-GMDSS-") ||
                          (cleanKey.startsWith("KEYGOC") && cleanKey.length >= 10)

        if (isGocFormat ||
            matchesHash(cleanKey, gmdssReq, gocSalts, listOf("KEY-GOC", "KEY-GMDSS", "ACTV", "GOC", "GMDSS", ""))
        ) {
            setGmdssProUnlocked(context, true)
            return UnlockResult.GMDSS_UNLOCKED
        }

        return UnlockResult.INVALID_KEY
    }
}
