package com.nhyle.hyperionmaritime.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceRegistrationViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReviewerViewModel.DeviceRegistrationUiState())
    val uiState: StateFlow<ReviewerViewModel.DeviceRegistrationUiState> = _uiState.asStateFlow()

    init {
        loadSavedLicenseState(application)
    }

    fun loadCurrentLicense(context: Context) {
        loadSavedLicenseState(context)
    }

    fun autoVerifyOnLaunch(context: Context) {
        loadSavedLicenseState(context)
    }

    fun onActivationKeyChange(input: String) {
        _uiState.update {
            it.copy(
                activationKeyInput = input.uppercase(),
                keyError = null,
                activationKeyError = null
            )
        }
    }

    fun onPromoCodeChange(input: String) {
        _uiState.update {
            it.copy(
                promoCodeInput = input.uppercase(),
                promoError = null
            )
        }
    }

    // ----------------------------------------------------------------------------
    // 1. OFFLINE ACTIVATION (ADMIN1234 / SHA-256 HARDWARE KEYS)
    // ----------------------------------------------------------------------------
    fun activateDeviceWithKey(context: Context) {
        val cleanKey = _uiState.value.activationKeyInput.trim().uppercase()
        if (cleanKey.isBlank()) {
            _uiState.update { it.copy(keyError = "ERR: PLEASE ENTER AN ACTIVATION KEY") }
            return
        }

        val result = com.nhyle.hyperionmaritime.data.util.LicenseManager.verifyAndUnlockKey(context, cleanKey)
        if (!result.isSuccess) {
            _uiState.update { it.copy(keyError = result.toastMessage) }
            return
        }

        val offlineInfo = com.nhyle.hyperionmaritime.data.util.LicenseManager.getOfflineLicenseInfo(context)
        val formattedStatus = if (offlineInfo != null) {
            "ACTIVE (${offlineInfo.durationLabel.uppercase()} • ${offlineInfo.getFormattedTimeRemaining().uppercase()})"
        } else {
            "ACTIVE (TERMINAL ACTIVATED)"
        }

        val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        val isOic = prefs.getBoolean("is_oic_unlocked", false)
        val isGmdss = prefs.getBoolean("is_gmdss_unlocked", false)
        val isAll = prefs.getBoolean("is_all_access_unlocked", false) || (isOic && isGmdss)

        _uiState.update {
            it.copy(
                isLicensed = true,
                licenseType = "OFFLINE_PASS",
                isOicLicensed = isOic,
                isGmdssLicensed = isGmdss,
                isAllAccessLicensed = isAll,
                statusMessage = formattedStatus,
                keyError = null,
                promoError = null
            )
        }
    }

    private fun validateKey(cleanKey: String): Boolean {
        return com.nhyle.hyperionmaritime.data.util.LicenseManager.verifyAndUnlockKey(getApplication(), cleanKey).isSuccess
    }

    private fun saveHardwareActivation(
        context: Context,
        unlockOic: Boolean,
        unlockGmdss: Boolean,
        unlockAll: Boolean
    ) {
        val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        val isComplete = unlockAll || (unlockOic && unlockGmdss)

        prefs.edit()
            .putBoolean("is_licensed", true)
            .putBoolean("is_offline_license", true) // HARD FIREWALL FOR OFFLINE
            .putString("license_type", "LIFETIME")
            .putBoolean("is_oic_unlocked", unlockOic)
            .putBoolean("is_gmdss_unlocked", unlockGmdss)
            .putBoolean("is_all_access_unlocked", isComplete)
            .putBoolean("is_full_access_unlocked", isComplete)
            .remove("active_promo_code")
            .remove("promo_expiry_epoch")
            .apply()

        if (unlockOic) com.nhyle.hyperionmaritime.data.util.LicenseManager.setOicNwProUnlocked(context, true)
        if (unlockGmdss) com.nhyle.hyperionmaritime.data.util.LicenseManager.setGmdssProUnlocked(context, true)
        if (isComplete) com.nhyle.hyperionmaritime.data.util.LicenseManager.setHyperionProUnlocked(context, true)
        com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository.clearCache()

        val offlineInfo = com.nhyle.hyperionmaritime.data.util.LicenseManager.getOfflineLicenseInfo(context)
        val formattedStatus = if (offlineInfo != null) {
            "ACTIVE (${offlineInfo.durationLabel.uppercase()} • ${offlineInfo.getFormattedTimeRemaining().uppercase()})"
        } else {
            "ACTIVE (OFFLINE PASS)"
        }

        _uiState.update {
            it.copy(
                isLicensed = true,
                licenseType = "OFFLINE_PASS",
                isOicLicensed = unlockOic,
                isGmdssLicensed = unlockGmdss,
                isAllAccessLicensed = isComplete,
                activationKeyInput = "",
                keyError = null,
                promoError = null,
                statusMessage = formattedStatus
            )
        }
    }

    // ----------------------------------------------------------------------------
    // 2. ONLINE PROMO REDEMPTION (FIREBASE)
    // ----------------------------------------------------------------------------
    fun redeemPromoCode(context: Context) {
        if (_uiState.value.isLicensed) {
            _uiState.update { it.copy(promoError = "YOU ALREADY HAVE AN ACTIVE LICENSE") }
            return
        }

        val cleanCode = _uiState.value.promoCodeInput.trim().uppercase()
        if (cleanCode.isBlank()) {
            _uiState.update { it.copy(promoError = "ERR: PLEASE ENTER A PROMO CODE") }
            return
        }

        _uiState.update { it.copy(isPromoLoading = true, promoError = null) }

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                        com.google.firebase.FirebaseApp.initializeApp(context)
                    }
                    val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
                    val settings = com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(0)
                        .setFetchTimeoutInSeconds(4)
                        .build()
                    remoteConfig.setConfigSettingsAsync(settings)

                    try {
                        com.google.android.gms.tasks.Tasks.await(
                            remoteConfig.fetchAndActivate(),
                            4,
                            java.util.concurrent.TimeUnit.SECONDS
                        )
                    } catch (_: Exception) {
                        try {
                            com.google.android.gms.tasks.Tasks.await(
                                remoteConfig.activate(),
                                4,
                                java.util.concurrent.TimeUnit.SECONDS
                            )
                        } catch (_: Exception) {}
                    }

                    val serverCode = (
                        remoteConfig.getString("promo_code_hash").takeIf { it.isNotBlank() }
                        ?: remoteConfig.getString("promo_code")
                    ).trim().uppercase()
                    val isActive = remoteConfig.getBoolean("promo_is_active")
                    val expiryEpoch = remoteConfig.getLong("promo_code_expiry_epoch")
                    val now = System.currentTimeMillis()

                    if (!isActive) throw IllegalStateException("PROMO CODE IS CURRENTLY DEACTIVATED")
                    if (expiryEpoch > 0L && now >= expiryEpoch) throw IllegalStateException("PROMO CODE HAS EXPIRED")
                    if (cleanCode != serverCode) throw IllegalStateException("INVALID PROMO CODE")

                    // Promo codes grant All-Access clearance
                    val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("is_full_access_unlocked", true)
                        .putBoolean("is_oic_unlocked", true)
                        .putBoolean("is_gmdss_unlocked", true)
                        .putBoolean("is_all_access_unlocked", true)
                        .putBoolean("is_licensed", true)
                        .putBoolean("is_offline_license", false) // Online Promo Pass
                        .putString("license_type", "PROMO_PASS")
                        .putString("active_license_tier", "ALL_ACCESS")
                        .putString("active_promo_code", cleanCode)
                        .putLong("promo_expiry_epoch", expiryEpoch)
                        .apply()

                    com.nhyle.hyperionmaritime.data.util.LicenseManager.setMasterProUnlocked(context, true)
                    com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository.clearCache()

                    true
                }

                if (result) {
                    _uiState.update {
                        it.copy(
                            isPromoLoading = false,
                            isLicensed = true,
                            licenseType = "PROMO_PASS",
                            activeTier = "ALL_ACCESS",
                            isOicLicensed = true,
                            isGmdssLicensed = true,
                            isAllAccessLicensed = true,
                            promoError = null,
                            statusMessage = "ACTIVE (PROMO PASS // ALL ACCESS)"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("DeviceRegistrationVM", "Remote config promo fetch failed or timed out: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isPromoLoading = false,
                        promoError = if (e is java.util.concurrent.TimeoutException || e.message?.contains("timeout", ignoreCase = true) == true) {
                            "ERR: CONNECTION TIMEOUT (4S). PLEASE CHECK NETWORK."
                        } else {
                            "ERR: ${e.message ?: "ACTIVATION FAILED"}"
                        }
                    )
                }
            }
        }
    }

    // ----------------------------------------------------------------------------
    // 3. DEFENSIVE REGISTRY STATUS VERIFICATION (NEVER WIPES OFFLINE KEYS)
    // ----------------------------------------------------------------------------
    fun verifyLicenseState(context: Context) {
        val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        val isOffline = prefs.getBoolean("is_offline_license", false)
        val type = prefs.getString("license_type", "") ?: ""
        val isOic = prefs.getBoolean("is_oic_unlocked", false)
        val isGmdss = prefs.getBoolean("is_gmdss_unlocked", false)
        val isAll = prefs.getBoolean("is_all_access_unlocked", false)

        // OFFLINE KEYS: Immediate exit. Never query Firebase. Never wipe license.
        val offlineInfo = com.nhyle.hyperionmaritime.data.util.LicenseManager.getOfflineLicenseInfo(context)
        if (isOffline && offlineInfo != null && offlineInfo.isActive && (isOic || isGmdss || isAll)) {
            _uiState.update {
                it.copy(
                    isVerifyingLicense = false,
                    isLicensed = true,
                    licenseType = "OFFLINE_PASS",
                    isOicLicensed = isOic || isAll,
                    isGmdssLicensed = isGmdss || isAll,
                    isAllAccessLicensed = isAll,
                    statusMessage = "ACTIVE (${offlineInfo.durationLabel.uppercase()} • ${offlineInfo.getFormattedTimeRemaining().uppercase()})"
                )
            }
            return
        }

        // Unlicensed / Restricted: Immediately sync UI state to locked
        if (type == "RESTRICTED" || type.isBlank() || (!isOffline && type != "PROMO_PASS")) {
            _uiState.update {
                it.copy(
                    isVerifyingLicense = false,
                    isLicensed = false,
                    licenseType = "RESTRICTED",
                    activeTier = "NONE",
                    isOicLicensed = false,
                    isGmdssLicensed = false,
                    isAllAccessLicensed = false,
                    statusMessage = "RESTRICTED (UNLICENSED)"
                )
            }
            return
        }

        // ONLY PROMO PASSES CHECK FIREBASE
        if (type == "PROMO_PASS") {
            val expiryEpoch = prefs.getLong("promo_expiry_epoch", 0L)
            val now = System.currentTimeMillis()

            if (expiryEpoch > 0L && now >= expiryEpoch) {
                revokePromoAccess(context)
                return
            }

            _uiState.update { it.copy(isVerifyingLicense = true) }

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                        com.google.firebase.FirebaseApp.initializeApp(context)
                    }
                    val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
                    val settings = com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(0)
                        .setFetchTimeoutInSeconds(4)
                        .build()
                    remoteConfig.setConfigSettingsAsync(settings)

                    try {
                        com.google.android.gms.tasks.Tasks.await(
                            remoteConfig.fetchAndActivate(),
                            4,
                            java.util.concurrent.TimeUnit.SECONDS
                        )
                    } catch (_: Exception) {
                        try {
                            com.google.android.gms.tasks.Tasks.await(
                                remoteConfig.activate(),
                                4,
                                java.util.concurrent.TimeUnit.SECONDS
                            )
                        } catch (_: Exception) {}
                    }

                    val isActive = remoteConfig.getBoolean("promo_is_active")
                    val serverExpiry = remoteConfig.getLong("promo_code_expiry_epoch")
                    val currentEpoch = System.currentTimeMillis()

                    withContext(Dispatchers.Main) {
                        if (!isActive || (serverExpiry > 0L && currentEpoch >= serverExpiry)) {
                            revokePromoAccess(context)
                        } else {
                            _uiState.update {
                                it.copy(
                                    isVerifyingLicense = false,
                                    statusMessage = "ACTIVE (PROMO PASS // ALL ACCESS)"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("DeviceRegistrationVM", "Remote config verification failed or timed out: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isVerifyingLicense = false) }
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------------------
    // 4. LOAD SAVED LICENSE ON INIT
    // ----------------------------------------------------------------------------
    fun loadSavedLicenseState(context: Context) {
        val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        val isOicOffline = prefs.getBoolean("is_oic_unlocked", false)
        val isGmdssOffline = prefs.getBoolean("is_gmdss_unlocked", false)
        val isAllOffline = prefs.getBoolean("is_all_access_unlocked", false) || (isOicOffline && isGmdssOffline)
        val isOffline = prefs.getBoolean("is_offline_license", false)
        val type = prefs.getString("license_type", "RESTRICTED") ?: "RESTRICTED"
        val expiryEpoch = prefs.getLong("promo_expiry_epoch", 0L)
        val now = System.currentTimeMillis()
        val reqKey = com.nhyle.hyperionmaritime.data.util.LicenseManager.getOrGenerateRequestCode(context)

        // Google Play Store Billing Entitlements
        val billingManager = com.nhyle.hyperionmaritime.data.billing.BillingManager.getInstance(context)
        val entitlements = billingManager.entitlements.value
        val isOicBilling = entitlements.oicNwUnlocked
        val isGmdssBilling = entitlements.gmdssUnlocked
        val isAllAccessBilling = entitlements.allAccessUnlocked || (isOicBilling && isGmdssBilling)

        // 1. Promo Pass
        if (type == "PROMO_PASS" && (isOicOffline || isGmdssOffline || isAllOffline)) {
            if (expiryEpoch > 0L && now >= expiryEpoch) {
                revokePromoAccess(context)
                return
            }
            _uiState.update {
                it.copy(
                    requestKey = reqKey,
                    isLicensed = true,
                    licenseType = "PROMO_PASS",
                    isOicLicensed = true,
                    isGmdssLicensed = true,
                    isAllAccessLicensed = true,
                    statusMessage = "ACTIVE (PROMO PASS // ALL ACCESS)",
                    keyError = null,
                    promoError = null
                )
            }
            return
        }

        // 2. Google Play Store Pass & Offline Hardware Licenses
        val offlineInfo = com.nhyle.hyperionmaritime.data.util.LicenseManager.getOfflineLicenseInfo(context)
        val oicOfflineActive = offlineInfo?.isActive == true && (offlineInfo.tier in listOf("ALL_ACCESS", "OIC_NW", "ALL", "OIC"))
        val gmdssOfflineActive = offlineInfo?.isActive == true && (offlineInfo.tier in listOf("ALL_ACCESS", "GMDSS_GOC", "ALL", "GOC"))
        val allOfflineActive = offlineInfo?.isActive == true && (offlineInfo.tier in listOf("ALL_ACCESS", "ALL"))

        val oicPlayExp = if (entitlements.oicNwPlan?.isExpired == false) entitlements.oicNwPlan!!.expiryTimestamp else 0L
        val gmdssPlayExp = if (entitlements.gmdssPlan?.isExpired == false) entitlements.gmdssPlan!!.expiryTimestamp else 0L
        val allPlayExp = if (entitlements.allAccessPlan?.isExpired == false) entitlements.allAccessPlan!!.expiryTimestamp else 0L

        val effOicExp = maxOf(if (oicOfflineActive) offlineInfo!!.expiryEpoch else 0L, oicPlayExp, allPlayExp)
        val effGmdssExp = maxOf(if (gmdssOfflineActive) offlineInfo!!.expiryEpoch else 0L, gmdssPlayExp, allPlayExp)

        val hasDirectAllAccess = (allPlayExp > now) || allOfflineActive
        val effOic = (effOicExp > now) || isOicBilling || oicOfflineActive
        val effGmdss = (effGmdssExp > now) || isGmdssBilling || gmdssOfflineActive
        val effAll = hasDirectAllAccess || (effOic && effGmdss)

        if (effOic || effGmdss || effAll) {
            val status = when {
                hasDirectAllAccess -> {
                    val exp = maxOf(allPlayExp, if (allOfflineActive) offlineInfo!!.expiryEpoch else 0L)
                    val timeStr = if (exp > 0L) " • ${com.nhyle.hyperionmaritime.data.util.LicenseManager.formatRemainingTime(exp, context).uppercase()}" else ""
                    "ACTIVE (ALL ACCESS$timeStr)"
                }
                effOic && effGmdss -> {
                    val minExp = if (effOicExp > 0L && effGmdssExp > 0L) minOf(effOicExp, effGmdssExp) else maxOf(effOicExp, effGmdssExp)
                    val timeStr = if (minExp > 0L) com.nhyle.hyperionmaritime.data.util.LicenseManager.formatRemainingTime(minExp, context).uppercase() else ""
                    if (timeStr.isNotEmpty()) "ACTIVE: $timeStr" else "ACTIVE (ALL ACCESS)"
                }
                effOic -> {
                    val timeStr = if (effOicExp > 0L) " • ${com.nhyle.hyperionmaritime.data.util.LicenseManager.formatRemainingTime(effOicExp, context).uppercase()}" else ""
                    "ACTIVE (OIC-NW DECK$timeStr)"
                }
                effGmdss -> {
                    val timeStr = if (effGmdssExp > 0L) " • ${com.nhyle.hyperionmaritime.data.util.LicenseManager.formatRemainingTime(effGmdssExp, context).uppercase()}" else ""
                    "ACTIVE (GMDSS GOC$timeStr)"
                }
                else -> "ACTIVE PASS"
            }

            val licType = if (isOicBilling || isGmdssBilling || isAllAccessBilling) "SUBSCRIPTION" else "OFFLINE_PASS"

            _uiState.update {
                it.copy(
                    requestKey = reqKey,
                    isLicensed = true,
                    licenseType = licType,
                    isOicLicensed = effOic,
                    isGmdssLicensed = effGmdss,
                    isAllAccessLicensed = effAll,
                    statusMessage = status,
                    keyError = null,
                    promoError = null
                )
            }
            return
        } else if (isOffline && offlineInfo != null && offlineInfo.isTampered) {
            _uiState.update {
                it.copy(
                    requestKey = reqKey,
                    isLicensed = false,
                    licenseType = "RESTRICTED",
                    statusMessage = "CLOCK TAMPERED // RESTORE SYSTEM TIME",
                    keyError = offlineInfo.tamperReason,
                    promoError = null
                )
            }
            return
        }

        // 4. Unlicensed (Free Tier)
        _uiState.update {
            it.copy(
                requestKey = reqKey,
                isLicensed = false,
                licenseType = "RESTRICTED",
                isOicLicensed = false,
                isGmdssLicensed = false,
                isAllAccessLicensed = false,
                statusMessage = "RESTRICTED (UNLICENSED)",
                keyError = null,
                promoError = null
            )
        }
    }

    fun checkLicenseStatusOnline(context: Context) {
        verifyLicenseState(context)
    }

    fun revokeAllAccess(context: Context) {
        revokePromoAccess(context)
    }

    fun revokePromoAccess(context: Context) {
        val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_full_access_unlocked", false)
            .putBoolean("is_all_access_unlocked", false)
            .putBoolean("is_oic_unlocked", false)
            .putBoolean("is_gmdss_unlocked", false)
            .putBoolean("is_licensed", false)
            .putBoolean("is_offline_license", false)
            .putString("license_type", "RESTRICTED")
            .remove("promo_expiry_epoch")
            .remove("active_promo_code")
            .apply()

        listOf("hyperion_license_prefs", "hyperion_prefs", "app_prefs", "license_prefs", "${context.packageName}_preferences").forEach { name ->
            try {
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
            } catch (_: Exception) {}
        }

        com.nhyle.hyperionmaritime.data.util.LicenseManager.setMasterProUnlocked(context, false)
        com.nhyle.hyperionmaritime.data.util.LicenseManager.setOicNwProUnlocked(context, false)
        com.nhyle.hyperionmaritime.data.util.LicenseManager.setGmdssProUnlocked(context, false)
        com.nhyle.hyperionmaritime.data.util.LicenseManager.setHyperionProUnlocked(context, false)
        com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository.clearCache()

        _uiState.update {
            it.copy(
                isLicensed = false,
                licenseType = "RESTRICTED",
                isOicLicensed = false,
                isGmdssLicensed = false,
                isAllAccessLicensed = false,
                isVerifyingLicense = false,
                statusMessage = "RESTRICTED (UNLICENSED)"
            )
        }
    }

    fun revokeLicenseSilently(context: Context) {
        revokePromoAccess(context)
    }
}
