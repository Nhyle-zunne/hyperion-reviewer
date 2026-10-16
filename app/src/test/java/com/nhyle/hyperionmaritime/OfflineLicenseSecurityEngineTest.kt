package com.nhyle.hyperionmaritime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.data.util.OfflineLicenseStatus
import com.nhyle.hyperionmaritime.data.util.UnlockResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class OfflineLicenseSecurityEngineTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences(LicenseManager.SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        val altPrefs = context.getSharedPreferences(LicenseManager.PREFS_NAME, Context.MODE_PRIVATE)
        altPrefs.edit().clear().apply()
        LicenseManager.init(context)
    }

    @Test
    fun testUniversalDeviceRequestCodeMatchesExpectedPattern() {
        val requestCode = LicenseManager.getUniversalDeviceCode(context)
        val pattern = Regex("^HYPE-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}$")
        assertTrue(
            "Request code $requestCode must strictly match HYPE-xxxx-xxxx-xxxx format",
            pattern.matches(requestCode)
        )
    }

    @Test
    fun testOfflineKeyGenerationAndActivationFor1Week1Month1Year() {
        val reqCode = LicenseManager.getUniversalDeviceCode(context)

        // 1. Test 1 Week Pass (OIC Deck)
        val key1W = LicenseManager.generateActivationKey(reqCode, tier = "OIC", duration = "1W")
        assertTrue("Key must start with HYPE-OIC-1W-", key1W.startsWith("HYPE-OIC-1W-"))
        val result1W = LicenseManager.verifyAndUnlockKey(context, key1W)
        assertEquals(UnlockResult.OIC_NW_UNLOCKED, result1W)
        assertTrue(LicenseManager.isOfflineOicNwUnlocked(context))
        assertFalse(LicenseManager.isOfflineGmdssUnlocked(context))

        val info1W = LicenseManager.getOfflineLicenseInfo(context)
        assertNotNull(info1W)
        assertEquals("1W", info1W?.duration)
        assertEquals("1-Week Pass", info1W?.durationLabel)
        assertEquals("7 days remaining", info1W?.getFormattedTimeRemaining())

        // 2. Test 1 Month Pass (GMDSS GOC)
        val key1M = LicenseManager.generateActivationKey(reqCode, tier = "GOC", duration = "1M")
        assertTrue("Key must start with HYPE-GOC-1M-", key1M.startsWith("HYPE-GOC-1M-"))
        val result1M = LicenseManager.verifyAndUnlockKey(context, key1M)
        assertEquals(UnlockResult.GMDSS_UNLOCKED, result1M)
        assertTrue(LicenseManager.isOfflineGmdssUnlocked(context))

        val info1M = LicenseManager.getOfflineLicenseInfo(context)
        assertNotNull(info1M)
        assertEquals("1M", info1M?.duration)
        assertEquals("1-Month Pass", info1M?.durationLabel)

        // 3. Test 1 Year Pass (All Access Bundle)
        val key1Y = LicenseManager.generateActivationKey(reqCode, tier = "ALL", duration = "1Y")
        assertTrue("Key must start with HYPE-ALL-1Y-", key1Y.startsWith("HYPE-ALL-1Y-"))
        val result1Y = LicenseManager.verifyAndUnlockKey(context, key1Y)
        assertEquals(UnlockResult.HYPERION_UNLOCKED, result1Y)
        assertTrue(LicenseManager.isOfflineHyperionUnlocked(context))
        assertTrue(LicenseManager.isOfflineOicNwUnlocked(context))
        assertTrue(LicenseManager.isOfflineGmdssUnlocked(context))

        val info1Y = LicenseManager.getOfflineLicenseInfo(context)
        assertNotNull(info1Y)
        assertEquals("1Y", info1Y?.duration)
        assertEquals("1-Year Pass", info1Y?.durationLabel)
        assertTrue(info1Y!!.remainingTimeMs > 360L * 24L * 60L * 60L * 1000L)
    }

    @Test
    fun testAntiTamperLayer2DetectsClockRollbackAndLocksLicense() {
        val reqCode = LicenseManager.getUniversalDeviceCode(context)
        val key1M = LicenseManager.generateActivationKey(reqCode, tier = "ALL", duration = "1M")
        val unlockResult = LicenseManager.verifyAndUnlockKey(context, key1M)
        assertTrue(unlockResult.isSuccess)
        assertTrue(LicenseManager.isOfflineHyperionUnlocked(context))

        // Simulate advancing the high-water mark to Day 10
        val prefs = context.getSharedPreferences(LicenseManager.SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val futureWatermark = now + (10L * 24L * 60L * 60L * 1000L)
        prefs.edit().putLong(LicenseManager.KEY_OFFLINE_LAST_VERIFIED_EPOCH, futureWatermark).apply()

        // Checking status when device clock (now) is earlier than watermark must flag ClockTampered
        val status = LicenseManager.checkOfflineLicense(context)
        assertTrue("Status must be ClockTampered when clock is rolled back", status is OfflineLicenseStatus.ClockTampered)
        assertFalse("Offline OIC must be locked when clock is tampered", LicenseManager.isOfflineOicNwUnlocked(context))
        assertFalse("Offline Hyperion must be locked when clock is tampered", LicenseManager.isOfflineHyperionUnlocked(context))

        // Restoring the clock to match the high-water mark clears the tamper flag
        prefs.edit().putLong(LicenseManager.KEY_OFFLINE_LAST_VERIFIED_EPOCH, now).apply()
        val restoredStatus = LicenseManager.checkOfflineLicense(context)
        assertTrue("Status must be Active once clock is restored", restoredStatus is OfflineLicenseStatus.Active)
        assertTrue("Offline access is restored", LicenseManager.isOfflineHyperionUnlocked(context))
    }

    @Test
    fun testLegacyPermanentClearancePurge() {
        val prefs = context.getSharedPreferences(LicenseManager.SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("license_type", "LIFETIME")
            .putBoolean("is_hardware_lifetime", true)
            .putBoolean("is_fully_activated", true)
            .putBoolean("is_licensed", true)
            .putBoolean("is_all_access_unlocked", true)
            .apply()

        // Initialize license manager - must purge legacy permanent clearance
        LicenseManager.init(context)

        assertFalse("Legacy permanent clearance must be purged", prefs.getBoolean("is_hardware_lifetime", false))
        assertFalse("Legacy is_fully_activated must be purged", prefs.getBoolean("is_fully_activated", false))
        assertEquals("RESTRICTED", prefs.getString("license_type", ""))
        assertFalse("License must be restricted", prefs.getBoolean("is_licensed", false))
    }

    @Test
    fun testExpiredKeyRejectedUponReinstall() {
        val reqCode = LicenseManager.getUniversalDeviceCode(context)
        // Simulate a key issued 15 days ago for a 1-week pass (expired 7 days ago)
        val pastDays = ((System.currentTimeMillis() / (24L * 60L * 60L * 1000L)) - 15).toInt()
        val pastDayHex = pastDays.toString(16).uppercase().padStart(4, '0')
        val expiredKey = LicenseManager.generateActivationKey(reqCode, tier = "OIC", duration = "1W", issueDayHex = pastDayHex)

        val result = LicenseManager.verifyAndUnlockKey(context, expiredKey)
        assertEquals("Key issued in the past beyond duration must be rejected as KEY_EXPIRED", UnlockResult.KEY_EXPIRED, result)
        assertFalse(LicenseManager.isOfflineOicNwUnlocked(context))
    }

    @Test
    fun testCustomExpirationDateKeyActivationAndLifecycle() {
        val reqCode = LicenseManager.getUniversalDeviceCode(context)
        val now = System.currentTimeMillis()
        val currentDay = (now / (24L * 60L * 60L * 1000L)).toInt()

        // 1. Generate a custom expiry key set for exactly 42 days in the future
        val targetExpDay = currentDay + 42
        val expDayHex = targetExpDay.toString(16).uppercase().padStart(4, '0')
        val customKey = LicenseManager.generateActivationKey(
            requestCodeOrDevice = reqCode,
            tier = "ALL",
            duration = "EXP",
            issueDayHex = expDayHex
        )

        assertTrue("Custom key must start with HYPE-ALL-EXP-", customKey.startsWith("HYPE-ALL-EXP-"))
        val unlockResult = LicenseManager.verifyAndUnlockKey(context, customKey)
        assertEquals(UnlockResult.HYPERION_UNLOCKED, unlockResult)
        assertTrue(LicenseManager.isOfflineHyperionUnlocked(context))
        assertTrue(LicenseManager.isOfflineOicNwUnlocked(context))
        assertTrue(LicenseManager.isOfflineGmdssUnlocked(context))

        val info = LicenseManager.getOfflineLicenseInfo(context)
        assertNotNull(info)
        assertEquals("CUSTOM", info?.duration)
        assertEquals("Custom Term Pass", info?.durationLabel)
        assertTrue(info?.getFormattedTimeRemaining()?.startsWith("4") == true && info?.getFormattedTimeRemaining()?.endsWith("days remaining") == true)

        // 2. An expired custom key (target expiry was 2 days ago) must be rejected
        val pastExpDay = currentDay - 2
        val pastExpDayHex = pastExpDay.toString(16).uppercase().padStart(4, '0')
        val expiredCustomKey = LicenseManager.generateActivationKey(
            requestCodeOrDevice = reqCode,
            tier = "OIC",
            duration = "EXP",
            issueDayHex = pastExpDayHex
        )

        val expiredResult = LicenseManager.verifyAndUnlockKey(context, expiredCustomKey)
        assertEquals(UnlockResult.KEY_EXPIRED, expiredResult)
    }
}
