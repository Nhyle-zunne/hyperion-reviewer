package com.nhyle.hyperionmaritime

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.data.util.OfflineLicenseStatus
import com.nhyle.hyperionmaritime.data.util.UnlockResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SignedKeyActivationTest {

    private lateinit var context: Context

    // Test keys for the fake device A1B2C3D4E5F6, plan EW
    // Valid: 2026-01-01 to 2099-12-31
    private val validKey =
        "HYPE2-AICAICEQ-OIR2DMWD-2TS7NBVE-Q77RFD7J-4IRMKCP7-5ZS5MA37-MGY7NXGQ-" +
            "72FQWY3V-3J4Y3IZ6-P3NMTE23-BNL4AENG-BEBDMGHP-OXLZ6K6E-GV7LZQ32-5ODGQ4A6-67WA"
    // Expired: 2025-01-01 to 2025-12-31
    private val expiredKey =
        "HYPE2-AICAIBZD-BCH2DMWD-2TS7NWPO-H7XDNNUD-W5JABYQO-IDW6WGFD-MO55QSWR-" +
            "AFHL54NS-TWQEUTDJ-IO43JKJR-AXZGTDPO-2HPMSZJI-3YQ7UTVD-LMXQF3EA-DY3KN5EU-T7CQ"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ANDROID_ID, "a1b2c3d4e5f6a1b2")
        context.getSharedPreferences(LicenseManager.SECURE_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences(LicenseManager.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        LicenseManager.init(context)
    }

    @Test
    fun validSignedKeyUnlocksOnlyEngineEw() {
        assertEquals("HYPE-A1B2-C3D4-E5F6", LicenseManager.getUniversalDeviceCode(context))
        val result = LicenseManager.verifyAndUnlockKey(context, validKey)
        assertEquals(UnlockResult.ENGINE_OIC_EW_UNLOCKED, result)
        assertTrue(LicenseManager.isOfflineEngineOicEwUnlocked(context))
        assertFalse(LicenseManager.isOfflineOicNwUnlocked(context))
        assertFalse(LicenseManager.isOfflineGmdssUnlocked(context))
    }

    @Test
    fun expiredSignedKeyIsRejected() {
        val result = LicenseManager.verifyAndUnlockKey(context, expiredKey)
        assertEquals(UnlockResult.KEY_EXPIRED, result)
        assertFalse(LicenseManager.isOfflineEngineOicEwUnlocked(context))
    }

    @Test
    fun licenseWithIssueDateInTheFutureIsLocked() {
        assertTrue(LicenseManager.verifyAndUnlockKey(context, validKey).isSuccess)
        val prefs = context.getSharedPreferences(LicenseManager.SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        val tenDaysMs = 10L * 24L * 60L * 60L * 1000L
        prefs.edit().putLong(LicenseManager.KEY_OFFLINE_ISSUE_EPOCH, System.currentTimeMillis() + tenDaysMs).apply()

        val status = LicenseManager.checkOfflineLicense(context)
        assertTrue(status is OfflineLicenseStatus.ClockTampered)
        assertFalse(LicenseManager.isOfflineEngineOicEwUnlocked(context))
    }

    @Test
    fun clockSetBackMoreThanTwoDaysIsLocked() {
        assertTrue(LicenseManager.verifyAndUnlockKey(context, validKey).isSuccess)
        val prefs = context.getSharedPreferences(LicenseManager.SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        val tenDaysMs = 10L * 24L * 60L * 60L * 1000L
        prefs.edit().putLong(LicenseManager.KEY_OFFLINE_LAST_VERIFIED_EPOCH, System.currentTimeMillis() + tenDaysMs).apply()

        val status = LicenseManager.checkOfflineLicense(context)
        assertTrue(status is OfflineLicenseStatus.ClockTampered)
        assertFalse(LicenseManager.isOfflineEngineOicEwUnlocked(context))
    }

    @Test
    fun clockSetBackLessThanTwoDaysIsNotLocked() {
        assertTrue(LicenseManager.verifyAndUnlockKey(context, validKey).isSuccess)
        val prefs = context.getSharedPreferences(LicenseManager.SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        val oneDayMs = 24L * 60L * 60L * 1000L
        prefs.edit().putLong(LicenseManager.KEY_OFFLINE_LAST_VERIFIED_EPOCH, System.currentTimeMillis() + oneDayMs).apply()

        assertTrue(LicenseManager.checkOfflineLicense(context) is OfflineLicenseStatus.Active)
        assertTrue(LicenseManager.isOfflineEngineOicEwUnlocked(context))
    }
}