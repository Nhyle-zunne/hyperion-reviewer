package com.nhyle.hyperionmaritime.ui.store

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhyle.hyperionmaritime.data.billing.BillingConstants
import com.nhyle.hyperionmaritime.data.billing.BillingCycle
import com.nhyle.hyperionmaritime.data.billing.PlayProductItem
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.ui.components.HyperionDrawerButton
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class StoreColors(
    val isDark: Boolean,
    val storeBackground: Color,
    val cardBackground: Color,
    val boxInactiveFill: Color,
    val borderInactive: Color,
    val accentGreen: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val failRed: Color
)

@Composable
fun rememberStoreColors(isDark: Boolean): StoreColors {
    return remember(isDark) {
        if (isDark) {
            StoreColors(
                isDark = true,
                storeBackground = Color(0xFF0F141C),
                cardBackground = Color(0xFF161F2E),
                boxInactiveFill = Color(0xFF1A2433),
                borderInactive = Color(0xFF26354A),
                accentGreen = Color(0xFF00E676),
                textPrimary = Color(0xFFF0F6FC),
                textSecondary = Color(0xFF8B949E),
                textMuted = Color(0xFF6E7681),
                failRed = Color(0xFFFF5252)
            )
        } else {
            StoreColors(
                isDark = false,
                storeBackground = Color(0xFFF1F5F9),
                cardBackground = Color(0xFFFFFFFF),
                boxInactiveFill = Color(0xFFE2E8F0),
                borderInactive = Color(0xFFCBD5E1),
                accentGreen = Color(0xFF008744),
                textPrimary = Color(0xFF0F172A),
                textSecondary = Color(0xFF475569),
                textMuted = Color(0xFF64748B),
                failRed = Color(0xFFDC2626)
            )
        }
    }
}

data class SelectedOffer(
    val productId: String,
    val cycle: BillingCycle
) : java.io.Serializable

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun openGooglePlaySubscriptions(context: Context) {
    val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
    val playStoreIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.android.vending")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(playStoreIntent)
    } catch (e: Exception) {
        val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(browserIntent)
        } catch (_: Exception) {}
    }
}

@Composable
fun StoreScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToDeviceRegistration: () -> Unit = { viewModel.navigateTo(Screen.Activation) },
    onNavigateToTerms: () -> Unit = { viewModel.navigateTo(Screen.TermsAndDisclaimer) },
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val billingManager = viewModel.billingManager

    val isDarkModeState by viewModel.isDarkMode.collectAsState()
    val isDark = isDarkModeState
    val colors = rememberStoreColors(isDark = isDark)

    val entitlements by billingManager.entitlements.collectAsStateWithLifecycle()
    val selectedTrack by viewModel.selectedTrack.collectAsState()

    var selectedOffer by rememberSaveable {
        mutableStateOf<SelectedOffer?>(null)
    }

    var sandboxConfirmProduct by remember { mutableStateOf<PlayProductItem?>(null) }
    var sandboxConfirmCycle by remember { mutableStateOf(BillingCycle.YEARLY) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreFeedbackMessage by remember { mutableStateOf<String?>(null) }

    var showExpiredPopup by rememberSaveable { mutableStateOf(false) }
    var expiredPopupMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var hasHandledExpiredPopup by rememberSaveable { mutableStateOf(false) }

    val isOicOffline = LicenseManager.isOfflineOicNwUnlocked(context)
    val isGmdssOffline = LicenseManager.isOfflineGmdssUnlocked(context)
    val isEwOffline = LicenseManager.isOfflineEngineOicEwUnlocked(context)
    val isDmOffline = LicenseManager.isOfflineDeckMgtUnlocked(context)
    val isEmOffline = LicenseManager.isOfflineEngineMgtUnlocked(context)
    val isAllAccessOffline = LicenseManager.isOfflineHyperionUnlocked(context)

    val hasExpiredPlan = (entitlements.oicNwPlan?.isExpired == true) ||
            (entitlements.gmdssPlan?.isExpired == true) ||
            (entitlements.engineOicEwPlan?.isExpired == true) ||
            (entitlements.deckMgtPlan?.isExpired == true) ||
            (entitlements.engineMgtPlan?.isExpired == true) ||
            (entitlements.allAccessPlan?.isExpired == true)

    androidx.compose.runtime.LaunchedEffect(hasExpiredPlan) {
        if (hasExpiredPlan && !hasHandledExpiredPopup) {
            expiredPopupMessage = "Your Hyperion subscription pass or terminal license has expired or been revoked. Please select a pass below or enter a valid key to restore access."
            showExpiredPopup = true
            hasHandledExpiredPopup = true
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            SharpTerminalTopBar(
                title = "HYPERION STORE",
                subtitle = "COMPREHENSIVE REVIEWER PASSES",
                colors = colors,
                onOpenDrawer = onOpenDrawer
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = colors.storeBackground,
        modifier = modifier
            .fillMaxSize()
            .testTag("store_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(
                visible = entitlements.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.failRed.copy(alpha = 0.12f), RectangleShape)
                        .border(1.dp, colors.failRed, RectangleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ERROR: ${entitlements.errorMessage ?: ""}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.failRed,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AnimatedVisibility(
                visible = restoreFeedbackMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.accentGreen.copy(alpha = 0.12f), RectangleShape)
                        .border(1.dp, colors.accentGreen, RectangleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SYSTEM: ${restoreFeedbackMessage ?: ""}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.accentGreen,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val offlineInfo = LicenseManager.getOfflineLicenseInfo(context)
            val now = System.currentTimeMillis()

            val isOfflineOicCovered = offlineInfo?.isActive == true && (offlineInfo.tier in listOf("ALL_ACCESS", "OIC_NW", "ALL", "OIC"))
            val isOfflineGmdssCovered = offlineInfo?.isActive == true && (offlineInfo.tier in listOf("ALL_ACCESS", "GMDSS_GOC", "ALL", "GOC"))
            val isOfflineEwCovered = offlineInfo?.isActive == true && (offlineInfo.tier in listOf("ALL_ACCESS", "ENGINE_OIC_EW", "ALL", "EW"))
            val isOfflineDmCovered = offlineInfo?.isActive == true && (offlineInfo.tier in listOf("ALL_ACCESS", "DECK_MGT", "ALL", "DM"))
            val isOfflineEmCovered = offlineInfo?.isActive == true && (offlineInfo.tier in listOf("ALL_ACCESS", "ENGINE_MGT", "ALL", "EM"))
            val isOfflineAllCovered = offlineInfo?.isActive == true && (offlineInfo.tier in listOf("ALL_ACCESS", "ALL"))

            val oicOfflineExp = if (isOfflineOicCovered) offlineInfo!!.expiryEpoch else 0L
            val gmdssOfflineExp = if (isOfflineGmdssCovered) offlineInfo!!.expiryEpoch else 0L
            val ewOfflineExp = if (isOfflineEwCovered) offlineInfo!!.expiryEpoch else 0L
            val dmOfflineExp = if (isOfflineDmCovered) offlineInfo!!.expiryEpoch else 0L
            val emOfflineExp = if (isOfflineEmCovered) offlineInfo!!.expiryEpoch else 0L
            val allOfflineExp = if (isOfflineAllCovered) offlineInfo!!.expiryEpoch else 0L

            val oicPlayExp = if (entitlements.oicNwPlan?.isExpired == false) entitlements.oicNwPlan!!.expiryTimestamp else 0L
            val gmdssPlayExp = if (entitlements.gmdssPlan?.isExpired == false) entitlements.gmdssPlan!!.expiryTimestamp else 0L
            val ewPlayExp = if (entitlements.engineOicEwPlan?.isExpired == false) entitlements.engineOicEwPlan!!.expiryTimestamp else 0L
            val dmPlayExp = if (entitlements.deckMgtPlan?.isExpired == false) entitlements.deckMgtPlan!!.expiryTimestamp else 0L
            val emPlayExp = if (entitlements.engineMgtPlan?.isExpired == false) entitlements.engineMgtPlan!!.expiryTimestamp else 0L
            val allPlayExp = if (entitlements.allAccessPlan?.isExpired == false) entitlements.allAccessPlan!!.expiryTimestamp else 0L

            val effectiveOicExp = maxOf(oicOfflineExp, oicPlayExp, allPlayExp, allOfflineExp)
            val effectiveGmdssExp = maxOf(gmdssOfflineExp, gmdssPlayExp, allPlayExp, allOfflineExp)
            val effectiveEwExp = maxOf(ewOfflineExp, ewPlayExp, allPlayExp, allOfflineExp)
            val effectiveDmExp = maxOf(dmOfflineExp, dmPlayExp, allPlayExp, allOfflineExp)
            val effectiveEmExp = maxOf(emOfflineExp, emPlayExp, allPlayExp, allOfflineExp)

            val hasDirectAllAccess = (allPlayExp > now) || (allOfflineExp > now)
            val hasActiveOic = (effectiveOicExp > now) || isOicOffline || entitlements.oicNwUnlocked
            val hasActiveGmdss = (effectiveGmdssExp > now) || isGmdssOffline || entitlements.gmdssUnlocked
            val hasActiveEw = (effectiveEwExp > now) || isEwOffline || entitlements.engineOicEwUnlocked
            val hasActiveDm = (effectiveDmExp > now) || isDmOffline || entitlements.deckMgtUnlocked
            val hasActiveEm = (effectiveEmExp > now) || isEmOffline || entitlements.engineMgtUnlocked
            val hasSeparateDualTracks = (hasActiveOic && hasActiveGmdss && hasActiveEw && hasActiveDm && hasActiveEm) && !hasDirectAllAccess

            val effectiveAllAccessExp = if (hasDirectAllAccess) {
                maxOf(allPlayExp, allOfflineExp)
            } else if (hasSeparateDualTracks) {
                minOf(effectiveOicExp, effectiveGmdssExp, effectiveEwExp, effectiveDmExp, effectiveEmExp)
            } else {
                0L
            }

            val hasActiveAllAccess = hasDirectAllAccess || hasSeparateDualTracks

            val isAnyOffline = isAllAccessOffline || isOicOffline || isGmdssOffline || isEwOffline || isDmOffline || isEmOffline
            val isAnyPlayPlan = entitlements.allAccessUnlocked || entitlements.oicNwUnlocked || entitlements.gmdssUnlocked || entitlements.engineOicEwUnlocked || entitlements.deckMgtUnlocked || entitlements.engineMgtUnlocked
            if (isAnyOffline || isAnyPlayPlan) {
                ActiveLicenseOverviewCard(
                    selectedTrack = selectedTrack,
                    isAllAccess = hasActiveAllAccess,
                    isOic = hasActiveOic,
                    isGmdss = hasActiveGmdss,
                    isEw = hasActiveEw,
                    isDm = hasActiveDm,
                    isEm = hasActiveEm,
                    hasDirectAllAccess = hasDirectAllAccess,
                    hasSeparateDualTracks = hasSeparateDualTracks,
                    effectiveAllAccessExp = effectiveAllAccessExp,
                    effectiveOicExp = effectiveOicExp,
                    effectiveGmdssExp = effectiveGmdssExp,
                    effectiveEwExp = effectiveEwExp,
                    effectiveDmExp = effectiveDmExp,
                    effectiveEmExp = effectiveEmExp,
                    oicPlayExp = oicPlayExp,
                    oicOfflineExp = oicOfflineExp,
                    gmdssPlayExp = gmdssPlayExp,
                    gmdssOfflineExp = gmdssOfflineExp,
                    ewPlayExp = ewPlayExp,
                    ewOfflineExp = ewOfflineExp,
                    dmPlayExp = dmPlayExp,
                    dmOfflineExp = dmOfflineExp,
                    emPlayExp = emPlayExp,
                    emOfflineExp = emOfflineExp,
                    isOffline = isAnyOffline,
                    isPlay = isAnyPlayPlan,
                    offlineInfo = offlineInfo,
                    entitlements = entitlements,
                    colors = colors,
                    onNavigateToDeviceRegistration = onNavigateToDeviceRegistration
                )
            }

            BillingConstants.ALL_PRODUCT_IDS
                .filter { it != BillingConstants.PRODUCT_ALL_ACCESS_PASS }
                .forEach { productId ->
                    val isEntitled = when (productId) {
                        BillingConstants.PRODUCT_OIC_NW_PASS -> hasActiveOic
                        BillingConstants.PRODUCT_GMDSS_PASS -> hasActiveGmdss
                        BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> hasActiveEw
                        BillingConstants.PRODUCT_DECK_MGT_PASS -> hasActiveDm
                        BillingConstants.PRODUCT_ENGINE_MGT_PASS -> hasActiveEm
                        else -> false
                    }

                    val activeBadgeText = when (productId) {
                        BillingConstants.PRODUCT_OIC_NW_PASS -> {
                            if (effectiveOicExp > now) "ACTIVE: ${formatRemainingDuration(effectiveOicExp).uppercase()}"
                            else if (offlineInfo?.isTampered == true) "CLOCK TAMPERED"
                            else "ACTIVE: UNLOCKED"
                        }
                        BillingConstants.PRODUCT_GMDSS_PASS -> {
                            if (effectiveGmdssExp > now) "ACTIVE: ${formatRemainingDuration(effectiveGmdssExp).uppercase()}"
                            else if (offlineInfo?.isTampered == true) "CLOCK TAMPERED"
                            else "ACTIVE: UNLOCKED"
                        }
                        BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> {
                            if (effectiveEwExp > now) "ACTIVE: ${formatRemainingDuration(effectiveEwExp).uppercase()}"
                            else if (offlineInfo?.isTampered == true) "CLOCK TAMPERED"
                            else "ACTIVE: UNLOCKED"
                        }
                        BillingConstants.PRODUCT_DECK_MGT_PASS -> {
                            if (effectiveDmExp > now) "ACTIVE: ${formatRemainingDuration(effectiveDmExp).uppercase()}"
                            else if (offlineInfo?.isTampered == true) "CLOCK TAMPERED"
                            else "ACTIVE: UNLOCKED"
                        }
                        BillingConstants.PRODUCT_ENGINE_MGT_PASS -> {
                            if (effectiveEmExp > now) "ACTIVE: ${formatRemainingDuration(effectiveEmExp).uppercase()}"
                            else if (offlineInfo?.isTampered == true) "CLOCK TAMPERED"
                            else "ACTIVE: UNLOCKED"
                        }
                        else -> "ACTIVE: UNLOCKED"
                    }

                    val isCardSelected = selectedOffer?.productId == productId
                    val selectedCardCycle = if (isCardSelected) selectedOffer?.cycle else null

                    SharpProductCard(
                        productId = productId,
                        viewModel = viewModel,
                        billingManager = billingManager,
                        colors = colors,
                        isEntitled = isEntitled,
                        activeBadgeText = activeBadgeText,
                        isCardSelected = isCardSelected,
                        selectedCardCycle = selectedCardCycle,
                        onSelectCycle = { cycle ->
                            selectedOffer = SelectedOffer(productId, cycle)
                        },
                        onPurchaseCycle = { cycle ->
                            selectedOffer = SelectedOffer(productId, cycle)
                            val activity = context.findActivity()
                            val details = billingManager.getProductDetails(productId)
                            if (activity != null && details != null) {
                                billingManager.launchBillingFlow(activity, productId, cycle)
                            } else if (activity != null && !com.nhyle.hyperionmaritime.BuildConfig.DEBUG) {
                                billingManager.launchBillingFlow(activity, productId, cycle)
                            } else {
                                val item = billingManager.getProductItem(productId, cycle)
                                sandboxConfirmProduct = item
                                sandboxConfirmCycle = cycle
                            }
                        }
                    )
                }

            AnimatedVisibility(
                visible = scrollState.canScrollForward && scrollState.value < 40,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "SCROLL DOWN FOR MORE PASSES ↓",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = colors.textMuted,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.boxInactiveFill, RectangleShape)
                    .border(1.dp, colors.borderInactive, RectangleShape)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = colors.accentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "OFFLINE CONTINUITY • SECURE CACHE: Verified Play subscriptions persist at sea without cellular link.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            lineHeight = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (!isRestoring) {
                            isRestoring = true
                            restoreFeedbackMessage = null
                            scope.launch {
                                billingManager.queryPurchasesAsync()
                                delay(800)
                                isRestoring = false
                                restoreFeedbackMessage = "Purchase restoration scan completed."
                            }
                        }
                    },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.borderInactive),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.cardBackground,
                        contentColor = colors.textMuted
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("restore_purchases_button")
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 1.5.dp,
                            color = colors.accentGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "RESTORE PURCHASES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onNavigateToDeviceRegistration,
                    shape = RectangleShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.cardBackground,
                        contentColor = colors.textMuted
                    ),
                    border = BorderStroke(1.dp, colors.borderInactive),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(38.dp)
                        .testTag("enter_terminal_key_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ENTER TERMINAL KEY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.textMuted,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    openGooglePlaySubscriptions(context)
                },
                shape = RectangleShape,
                border = BorderStroke(1.dp, colors.borderInactive),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.cardBackground,
                    contentColor = colors.textMuted
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("manage_cancel_subscription_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MANAGE / CANCEL SUBSCRIPTION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textMuted,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onNavigateToTerms,
                shape = RectangleShape,
                border = BorderStroke(1.dp, colors.borderInactive),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.cardBackground,
                    contentColor = colors.textMuted
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("terms_and_conditions_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TERMS AND CONDITIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textMuted,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showExpiredPopup) {
        Dialog(
            onDismissRequest = { showExpiredPopup = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("license_expired_dialog"),
                shape = RectangleShape,
                color = colors.cardBackground,
                border = BorderStroke(1.dp, colors.failRed)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = colors.failRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "NOTICE: PASS / LICENSE EXPIRED",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = colors.failRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { showExpiredPopup = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = expiredPopupMessage
                            ?: "Your Hyperion subscription pass or terminal license has expired or been revoked. Please select a pass below or enter an active key to restore clearance.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        ),
                        color = colors.textPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showExpiredPopup = false
                                onNavigateToDeviceRegistration()
                            },
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, colors.borderInactive),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.cardBackground,
                                contentColor = colors.textMuted
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text(
                                text = "ENTER KEY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { showExpiredPopup = false },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accentGreen,
                                contentColor = if (colors.isDark) Color.Black else Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text(
                                text = "RENEW PASS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    sandboxConfirmProduct?.let { product ->
        Dialog(
            onDismissRequest = { sandboxConfirmProduct = null },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("sandbox_confirm_dialog"),
                shape = RectangleShape,
                color = colors.cardBackground,
                border = BorderStroke(2.dp, colors.accentGreen)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GOOGLE PLAY SANDBOX SIMULATOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = colors.accentGreen
                        )
                        IconButton(
                            onClick = { sandboxConfirmProduct = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "Simulate Google Play subscription purchase in emulator / browser preview:\n\n• Product: ${product.title}\n• Cycle: ${sandboxConfirmCycle.name}\n• Reference Price: ${product.formattedPrice}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        ),
                        color = colors.textPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { sandboxConfirmProduct = null },
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, colors.borderInactive),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.boxInactiveFill,
                                contentColor = colors.textMuted
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("sandbox_cancel_button")
                        ) {
                            Text(
                                text = "CANCEL",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                val id = product.productId
                                val cycle = sandboxConfirmCycle
                                sandboxConfirmProduct = null
                                billingManager.simulatePurchaseForTesting(id, cycle)
                                viewModel.refreshUnlockStates()
                            },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accentGreen,
                                contentColor = if (colors.isDark) Color.Black else Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("sandbox_confirm_button")
                        ) {
                            Text(
                                text = "SIMULATE UNLOCK",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharpTerminalTopBar(
    title: String,
    subtitle: String,
    colors: StoreColors,
    onOpenDrawer: () -> Unit
) {
    Surface(
        shape = RectangleShape,
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.borderInactive),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HyperionDrawerButton(
                    onOpenDrawer = onOpenDrawer,
                    modifier = Modifier.testTag("store_drawer_button")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        ),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SharpProductCard(
    productId: String,
    viewModel: ReviewerViewModel,
    billingManager: com.nhyle.hyperionmaritime.data.billing.BillingManager,
    colors: StoreColors,
    isEntitled: Boolean,
    activeBadgeText: String,
    isCardSelected: Boolean,
    selectedCardCycle: BillingCycle?,
    onSelectCycle: (BillingCycle) -> Unit,
    onPurchaseCycle: (BillingCycle) -> Unit
) {
    val cardBorderColor = when {
        isCardSelected -> colors.accentGreen
        isEntitled -> colors.accentGreen.copy(alpha = 0.4f)
        else -> colors.borderInactive
    }
    val cardBorderWidth = if (isCardSelected) 2.dp else 1.dp

    val title = when (productId) {
        BillingConstants.PRODUCT_OIC_NW_PASS -> "DECK OIC-NW PASS"
        BillingConstants.PRODUCT_GMDSS_PASS -> "GMDSS GOC PASS"
        BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> "ENGINE OIC-EW PASS"
        BillingConstants.PRODUCT_DECK_MGT_PASS -> "DECK MANAGEMENT PASS"
        BillingConstants.PRODUCT_ENGINE_MGT_PASS -> "ENGINE MANAGEMENT PASS"
        else -> productId.uppercase()
    }

    val track = when (productId) {
        BillingConstants.PRODUCT_OIC_NW_PASS -> Track.DECK_OIC_NW
        BillingConstants.PRODUCT_GMDSS_PASS -> Track.GMDSS_GOC
        BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> Track.ENGINE_OIC_EW
        BillingConstants.PRODUCT_DECK_MGT_PASS -> Track.DECK_MGT
        BillingConstants.PRODUCT_ENGINE_MGT_PASS -> Track.ENGINE_MGT
        else -> null
    }
    val countText = if (track != null) viewModel.getTrackQuestionCountFormatted(track) else "Full Question Bank"

    val featureBullets = when (productId) {
        BillingConstants.PRODUCT_OIC_NW_PASS -> "$countText • All 12 Competencies • Timed Mock Exams • Offline at Sea"
        BillingConstants.PRODUCT_GMDSS_PASS -> "$countText • Radio Protocols & Mock Exams • Offline at Sea"
        BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> "$countText • 13 Competencies • Mock Exams • Offline at Sea"
        BillingConstants.PRODUCT_DECK_MGT_PASS -> "$countText • 18 Competencies • Mock Exams • Offline at Sea"
        BillingConstants.PRODUCT_ENGINE_MGT_PASS -> "$countText • 14 Competencies • Mock Exams • Offline at Sea"
        else -> ""
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_$productId"),
        shape = RectangleShape,
        color = colors.cardBackground,
        border = BorderStroke(cardBorderWidth, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                val trackBadge = when (productId) {
                    BillingConstants.PRODUCT_OIC_NW_PASS -> "DECK TRACK"
                    BillingConstants.PRODUCT_GMDSS_PASS -> "RADIO TRACK"
                    BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS -> "ENGINE TRACK"
                    BillingConstants.PRODUCT_DECK_MGT_PASS -> "DECK MANAGEMENT"
                    BillingConstants.PRODUCT_ENGINE_MGT_PASS -> "ENGINE MANAGEMENT"
                    else -> null
                }

                if (trackBadge != null) {
                    Box(
                        modifier = Modifier
                            .background(colors.boxInactiveFill, RectangleShape)
                            .border(1.dp, colors.borderInactive, RectangleShape)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trackBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            ),
                            color = colors.textMuted,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = featureBullets,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    lineHeight = 13.5.sp
                ),
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                BillingCycle.values().forEach { cycle ->
                    val isCycleActive = selectedCardCycle == cycle
                    val cycleItem = billingManager.getProductItem(productId, cycle)

                    SharpCycleBox(
                        cycle = cycle,
                        formattedPrice = cycleItem.shortFormattedPrice,
                        savingsPercent = cycleItem.savingsPercentage,
                        isSelected = isCycleActive,
                        colors = colors,
                        onClick = {
                            onSelectCycle(cycle)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cycle_box_${productId}_${cycle.name.lowercase()}")
                    )
                }
            }

            if (isEntitled) {
                Surface(
                    shape = RectangleShape,
                    color = colors.storeBackground,
                    border = BorderStroke(1.dp, colors.accentGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("purchase_button_$productId")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colors.accentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeBadgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = colors.accentGreen,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                if (selectedCardCycle == null) {
                    Button(
                        onClick = { },
                        enabled = false,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = colors.boxInactiveFill,
                            disabledContentColor = colors.textMuted
                        ),
                        border = BorderStroke(1.dp, colors.borderInactive),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("purchase_button_$productId")
                    ) {
                        Text(
                            text = "SELECT A PLAN TO UNLOCK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = colors.textMuted,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    val selectedItem = billingManager.getProductItem(productId, selectedCardCycle)
                    val buttonText = "UNLOCK — ${selectedItem.shortFormattedPrice}"

                    Button(
                        onClick = {
                            onPurchaseCycle(selectedCardCycle)
                        },
                        enabled = true,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentGreen,
                            contentColor = if (colors.isDark) Color.Black else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("purchase_button_$productId")
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharpCycleBox(
    cycle: BillingCycle,
    formattedPrice: String,
    savingsPercent: Int?,
    isSelected: Boolean,
    colors: StoreColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boxBorderColor = if (isSelected) colors.accentGreen else colors.borderInactive
    val boxBorderWidth = if (isSelected) 2.dp else 1.dp
    val boxBg = if (isSelected) colors.boxInactiveFill else colors.cardBackground

    Surface(
        shape = RectangleShape,
        color = boxBg,
        border = BorderStroke(boxBorderWidth, boxBorderColor),
        modifier = modifier
            .height(50.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when (cycle) {
                        BillingCycle.WEEKLY -> "WEEKLY"
                        BillingCycle.MONTHLY -> "MONTHLY"
                        BillingCycle.YEARLY -> "YEARLY"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.sp
                    ),
                    color = if (isSelected) colors.textPrimary else colors.textMuted,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (savingsPercent != null && cycle == BillingCycle.YEARLY) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "-$savingsPercent%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        ),
                        color = colors.accentGreen,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formattedPrice,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                ),
                color = if (isSelected) colors.accentGreen else colors.textPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun formatRemainingDuration(expiryEpoch: Long): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val now = System.currentTimeMillis()
    val prefs = context.getSharedPreferences("hyperion_secure_license_v2", Context.MODE_PRIVATE)
    val actEpoch = prefs.getLong("offline_activation_epoch", 0L)
    val issEpoch = prefs.getLong("offline_issue_epoch", 0L)
    val lastVerEpoch = prefs.getLong("offline_last_verified_epoch", 0L)

    val playPrefs = context.getSharedPreferences("hyperion_play_billing_secure_prefs", Context.MODE_PRIVATE)
    val playPurchaseTime = playPrefs.getLong("play_purchase_timestamp", 0L)
    val playHighWater = playPrefs.getLong("play_license_high_water_mark", 0L)

    val effectiveNow = maxOf(now, actEpoch, issEpoch, lastVerEpoch, playPurchaseTime, playHighWater)
    val remainingMs = (expiryEpoch - effectiveNow).coerceAtLeast(0L)
    val totalHours = remainingMs / (1000L * 60L * 60L)
    val totalMins = remainingMs / (1000L * 60L)
    val days = (totalHours + 23L) / 24L
    val hours = totalHours % 24L
    val mins = totalMins % 60L

    return when {
        days > 1 -> "$days days remaining"
        totalHours > 1 -> "$totalHours hrs, $mins mins remaining"
        totalHours == 1L -> "1 hr, $mins mins remaining"
        totalMins > 0 -> "$totalMins mins remaining"
        else -> "< 1 min remaining"
    }
}

private fun formatExpiryDateString(expiryEpoch: Long): String {
    if (expiryEpoch <= 0L) return "Active"
    val date = java.util.Date(expiryEpoch)
    val format = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return "Expires " + format.format(date)
}

private data class ActivePassSummary(
    val title: String,
    val activeExp: Long,
    val isOffline: Boolean,
    val isPlay: Boolean
)

@Composable
private fun ActiveLicenseOverviewCard(
    selectedTrack: Track,
    isAllAccess: Boolean,
    isOic: Boolean,
    isGmdss: Boolean,
    isEw: Boolean,
    isDm: Boolean,
    isEm: Boolean,
    hasDirectAllAccess: Boolean,
    hasSeparateDualTracks: Boolean,
    effectiveAllAccessExp: Long,
    effectiveOicExp: Long,
    effectiveGmdssExp: Long,
    effectiveEwExp: Long,
    effectiveDmExp: Long,
    effectiveEmExp: Long,
    oicPlayExp: Long,
    oicOfflineExp: Long,
    gmdssPlayExp: Long,
    gmdssOfflineExp: Long,
    ewPlayExp: Long,
    ewOfflineExp: Long,
    dmPlayExp: Long,
    dmOfflineExp: Long,
    emPlayExp: Long,
    emOfflineExp: Long,
    isOffline: Boolean,
    isPlay: Boolean,
    offlineInfo: com.nhyle.hyperionmaritime.data.util.OfflineLicenseInfo?,
    entitlements: com.nhyle.hyperionmaritime.data.billing.TrackEntitlements,
    colors: StoreColors,
    onNavigateToDeviceRegistration: () -> Unit
) {
    val isTampered = offlineInfo?.isTampered == true
    val now = System.currentTimeMillis()

    val activePasses = remember(
        isOic, isGmdss, isEw, isDm, isEm,
        effectiveOicExp, effectiveGmdssExp, effectiveEwExp, effectiveDmExp, effectiveEmExp,
        oicOfflineExp, gmdssOfflineExp, ewOfflineExp, dmOfflineExp, emOfflineExp,
        oicPlayExp, gmdssPlayExp, ewPlayExp, dmPlayExp, emPlayExp,
        entitlements.oicNwUnlocked, entitlements.gmdssUnlocked,
        entitlements.engineOicEwUnlocked, entitlements.deckMgtUnlocked, entitlements.engineMgtUnlocked
    ) {
        val list = mutableListOf<ActivePassSummary>()
        if (isOic) {
            list.add(
                ActivePassSummary(
                    title = "DECK OIC-NW PASS",
                    activeExp = effectiveOicExp,
                    isOffline = oicOfflineExp > now,
                    isPlay = (oicPlayExp > now) || entitlements.oicNwUnlocked
                )
            )
        }
        if (isGmdss) {
            list.add(
                ActivePassSummary(
                    title = "GMDSS GOC PASS",
                    activeExp = effectiveGmdssExp,
                    isOffline = gmdssOfflineExp > now,
                    isPlay = (gmdssPlayExp > now) || entitlements.gmdssUnlocked
                )
            )
        }
        if (isEw) {
            list.add(
                ActivePassSummary(
                    title = "ENGINE OIC-EW PASS",
                    activeExp = effectiveEwExp,
                    isOffline = ewOfflineExp > now,
                    isPlay = (ewPlayExp > now) || entitlements.engineOicEwUnlocked
                )
            )
        }
        if (isDm) {
            list.add(
                ActivePassSummary(
                    title = "DECK MANAGEMENT PASS",
                    activeExp = effectiveDmExp,
                    isOffline = dmOfflineExp > now,
                    isPlay = (dmPlayExp > now) || entitlements.deckMgtUnlocked
                )
            )
        }
        if (isEm) {
            list.add(
                ActivePassSummary(
                    title = "ENGINE MANAGEMENT PASS",
                    activeExp = effectiveEmExp,
                    isOffline = emOfflineExp > now,
                    isPlay = (emPlayExp > now) || entitlements.engineMgtUnlocked
                )
            )
        }
        list
    }

    val headerTitle = when {
        activePasses.size > 1 -> "CURRENT ACTIVE LICENSES (${activePasses.size} ACTIVE)"
        else -> "CURRENT ACTIVE LICENSE"
    }

    val borderColor = if (isTampered) colors.failRed else colors.accentGreen
    val statusBg = if (isTampered) colors.failRed else colors.accentGreen
    val statusTextColor = if (colors.isDark) Color.Black else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_license_overview_card"),
        shape = RectangleShape,
        color = colors.cardBackground,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isTampered) Icons.Default.Close else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isTampered) colors.failRed else colors.accentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = colors.textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(statusBg, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isTampered) "TAMPERED" else "ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = statusTextColor
                    )
                }
            }

            if (activePasses.isNotEmpty()) {
                activePasses.forEachIndexed { index, pass ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.borderInactive)
                                .padding(vertical = 4.dp)
                        )
                    }

                    val durationText = when {
                        isTampered -> "CLOCK ROLLBACK DETECTED"
                        pass.activeExp > 0L -> "ACTIVE: ${formatRemainingDuration(pass.activeExp).uppercase()}"
                        offlineInfo != null && offlineInfo.isActive -> "ACTIVE: ${offlineInfo.getFormattedTimeRemaining().uppercase()}"
                        else -> "ACTIVE REVIEWER PASS"
                    }

                    val channelLabel = when {
                        pass.isOffline && pass.isPlay -> "OFFLINE + PLAY STORE"
                        pass.isOffline -> "OFFLINE LICENSE KEY"
                        else -> "GOOGLE PLAY STORE"
                    }

                    val expirySubtitle = when {
                        isTampered -> "ERROR: System time set earlier than verification record. Restore correct device time."
                        pass.activeExp > 0L -> "Valid until ${formatExpiryDateString(pass.activeExp)}"
                        offlineInfo != null && offlineInfo.isActive -> "Valid offline at sea • ${offlineInfo.getFormattedExpiryDate()}"
                        else -> "Active terminal authorization"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (index > 0) 4.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = pass.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = durationText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = if (isTampered) colors.failRed else colors.accentGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Box(
                                modifier = Modifier
                                    .background(colors.boxInactiveFill, RectangleShape)
                                    .border(1.dp, colors.borderInactive, RectangleShape)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = channelLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp
                                    ),
                                    color = colors.textMuted
                                )
                            }
                        }

                        Text(
                            text = expirySubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                lineHeight = 12.sp
                            ),
                            color = if (isTampered) colors.failRed else colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}