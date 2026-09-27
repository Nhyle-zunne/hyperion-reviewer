package com.example.data.util

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

object LicenseManager {
    const val PREFS_NAME = "hyperion_license_prefs"
    const val KEY_INSTALLATION_TOKEN = "installation_hardware_token"
    const val KEY_REQUEST_CODE = "device_request_code"

    private val _isProActivatedFlow = MutableStateFlow(false)
    val isProActivatedFlow: StateFlow<Boolean> = _isProActivatedFlow.asStateFlow()

    fun init(context: Context) {
        _isProActivatedFlow.value = isActivated(context)
    }

    // Obfuscated salt fragments: XOR masked with 0x5A to prevent detection via simple string search in decompilers
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

    /**
     * Dynamically decodes and reassembles the secret salt at runtime.
     */
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
     * Checks if the app license is currently activated.
     */
    fun isActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_license_activated", false)) {
            _isProActivatedFlow.value = true
            return true
        }
        val altPrefs = context.getSharedPreferences("hyperion_prefs", Context.MODE_PRIVATE)
        val active = altPrefs.getBoolean("is_license_activated", false)
        _isProActivatedFlow.value = active
        return active
    }

    /**
     * Sets the license activation state across preference stores.
     */
    fun setActivated(context: Context, activated: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("is_license_activated", activated).apply()
        context.getSharedPreferences("hyperion_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("is_license_activated", activated).apply()
        _isProActivatedFlow.value = activated
    }

    /**
     * Retrieves or generates the installation-bound hardware token.
     * Checks SharedPreferences for a stored installation token.
     * If it does not exist (first launch after an install), mixes ANDROID_ID with System.currentTimeMillis()
     * and saves it permanently to SharedPreferences.
     * If it already exists, returns the saved token (ensuring stability across launches and updates).
     * Whenever the user uninstalls the app or clears app data, this token is wiped, forcing a brand new
     * timestamp and Request Code on the next launch.
     */
    fun getOrGenerateInstallationToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_INSTALLATION_TOKEN, null)
        if (!existing.isNullOrEmpty()) {
            return existing
        }

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "DEVICE_FALLBACK"
        } catch (_: Exception) {
            "DEVICE_FALLBACK"
        }
        val timestamp = System.currentTimeMillis()
        val newToken = "${androidId}_${timestamp}"
        prefs.edit().putString(KEY_INSTALLATION_TOKEN, newToken).apply()
        return newToken
    }

    /**
     * Returns the installation-bound Request Code, generating it if necessary.
     * Checks SharedPreferences for an existing valid "HYP-XXXX-XXXX" code.
     * If it does not exist, generates it from the installation token and saves it permanently.
     */
    fun getOrGenerateRequestCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_REQUEST_CODE, null)
        val validFormat = Regex("^HYP-[0-9A-F]{4}-[0-9A-F]{4}$")
        if (stored != null && validFormat.matches(stored)) {
            return stored
        }

        val token = getOrGenerateInstallationToken(context)
        val newCode = generateRequestCode(token)
        prefs.edit().putString(KEY_REQUEST_CODE, newCode).apply()
        return newCode
    }

    /**
     * Deterministically generates a persistent formatted device Request Code
     * in standardized format: "HYP-XXXX-XXXX" (e.g., HYP-A1B2-C3D4)
     * using the device identifier or persistent seed.
     */
    fun generateRequestCode(deviceSeed: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest((deviceSeed + "HYP_REQ_SEED_2026").toByteArray(Charsets.UTF_8))
        val hex = hash.joinToString("") { "%02X".format(it) }
        return "HYP-${hex.substring(0, 4)}-${hex.substring(4, 8)}"
    }

    /**
     * Computes the matching SHA-256 license key for a given request code.
     * Verification rule:
     * Calculate SHA-256 of (DeviceRequestCode + ":" + SecretSalt).
     * Take the first 12 hex characters in uppercase, grouped into 3 chunks of 4:
     * "ACTV-" + chunk1 + "-" + chunk2 + "-" + chunk3.
     * Formatted as: ACTV-XXXX-XXXX-XXXX (e.g., ACTV-1A2B-3C4D-5E6F)
     */
    fun computeValidKey(requestCode: String): String {
        val cleanReq = requestCode.uppercase().trim()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest((cleanReq + ":" + SECRET_SALT).toByteArray(Charsets.UTF_8))
        val hex = hash.joinToString("") { "%02X".format(it) }
        val chunk1 = hex.substring(0, 4)
        val chunk2 = hex.substring(4, 8)
        val chunk3 = hex.substring(8, 12)
        return "ACTV-$chunk1-$chunk2-$chunk3"
    }

    /**
     * Validates whether an entered license key matches the expected cryptographic key.
     * Returns true if valid for this device request code, false otherwise.
     */
    fun verifyKey(requestCode: String, enteredKey: String): Boolean {
        val cleanKey = enteredKey.uppercase().trim().replace(" ", "")
        if (cleanKey.isEmpty()) return false
        val expected = computeValidKey(requestCode)
        return cleanKey == expected || cleanKey == expected.replace("-", "")
    }
}
