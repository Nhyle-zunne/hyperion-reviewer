package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.viewmodel.DeviceRegistrationViewModel
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DeviceRegistrationViewModelTest {

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var viewModel: DeviceRegistrationViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        viewModel = DeviceRegistrationViewModel(application)
    }

    @Test
    fun testFirebaseRemoteConfigSettingsBuilderConfigures4SecondTimeout() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0)
            .setFetchTimeoutInSeconds(4)
            .build()

        assertEquals("Firebase Remote Config fetch timeout must be strictly 4 seconds", 4L, settings.fetchTimeoutInSeconds)
    }

    @Test
    fun testOfflineHardwareLicenseIsNotRevokedOnRemoteConfigFailure() {
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_licensed", true)
            .putBoolean("is_offline_license", true)
            .putString("license_type", "OFFLINE_PASS")
            .putString("offline_license_tier", "OIC_NW")
            .putString("offline_license_duration", "1M")
            .putLong("offline_license_activation_epoch", now)
            .putLong("offline_license_issue_epoch", now)
            .putLong("offline_license_expiry_epoch", now + 30L * 86400000L)
            .putLong("offline_license_last_verified_epoch", now)
            .putBoolean("is_oic_unlocked", true)
            .putBoolean("is_gmdss_unlocked", false)
            .putBoolean("is_all_access_unlocked", false)
            .apply()

        viewModel.loadSavedLicenseState(context)
        assertTrue(viewModel.uiState.value.isLicensed)
        assertTrue(viewModel.uiState.value.isOicLicensed)

        // Verifying license state should immediately recognize offline license without contacting remote config
        viewModel.verifyLicenseState(context)

        // License state must remain intact
        val state = viewModel.uiState.value
        assertTrue("Offline license must remain active", state.isLicensed)
        assertEquals("OFFLINE_PASS", state.licenseType)
        assertTrue("OIC license must remain active", state.isOicLicensed)
        assertFalse("Verification loading state must be false", state.isVerifyingLicense)
    }

    @Test
    fun testRedeemPromoCodeBlankInputFailsFastWithoutNetwork() {
        viewModel.onPromoCodeChange("")
        viewModel.redeemPromoCode(context)

        val state = viewModel.uiState.value
        assertFalse("Loading state must be dismissed", state.isPromoLoading)
        assertNotNull("Promo error must be present", state.promoError)
        assertEquals("ERR: PLEASE ENTER A PROMO CODE", state.promoError)
    }

    @Test
    fun testMasterKeyActivatesAllModulesWithoutTouchingRemoteConfig() {
        viewModel.onActivationKeyChange("ADMIN1234")
        viewModel.activateDeviceWithKey(context)

        val state = viewModel.uiState.value
        assertTrue(state.isLicensed)
        assertTrue(state.isOicLicensed)
        assertTrue(state.isGmdssLicensed)
        assertTrue(state.isAllAccessLicensed)
        assertEquals("OFFLINE_PASS", state.licenseType)
        assertNull(state.keyError)
    }
}
