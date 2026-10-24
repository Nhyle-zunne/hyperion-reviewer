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
}
