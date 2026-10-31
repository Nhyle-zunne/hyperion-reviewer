package com.nhyle.hyperionmaritime.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhyle.hyperionmaritime.BuildConfig
import com.nhyle.hyperionmaritime.data.billing.BillingConstants
import com.nhyle.hyperionmaritime.data.billing.BillingCycle
import com.nhyle.hyperionmaritime.data.billing.PlayProductItem
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.ui.motion.HyperionDialog
import com.nhyle.hyperionmaritime.ui.store.StorePopupCard
import com.nhyle.hyperionmaritime.ui.store.findActivity
import com.nhyle.hyperionmaritime.ui.store.rememberStoreColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel

/**
 * Maps each locked reviewer track to its own Google Play subscription product.
 * A locked feature opens its exact track pass rather than an unrelated or generic pass.
 */
internal fun paywallProductIdForTrack(track: Track): String = when (track) {
    Track.DECK_OIC_NW -> BillingConstants.PRODUCT_OIC_NW_PASS
    Track.GMDSS_GOC -> BillingConstants.PRODUCT_GMDSS_PASS
    Track.ENGINE_OIC_EW -> BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS
    Track.DECK_MGT -> BillingConstants.PRODUCT_DECK_MGT_PASS
    Track.ENGINE_MGT -> BillingConstants.PRODUCT_ENGINE_MGT_PASS
}

/**
 * Locked-feature purchase UI. It intentionally reuses StoreScreen's StorePopupCard
 * so artwork, track statistics, benefits, plan selector, localized Play prices, and
 * the purchase action remain identical to the Bond Store.
 */
@Composable
fun PaywallDialog(
    viewModel: ReviewerViewModel,
    targetTrack: Track? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val billingManager = viewModel.billingManager
    val selectedTrack by viewModel.selectedTrack.collectAsStateWithLifecycle()
    val playEntitlements by billingManager.entitlements.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    // triggerPaywall(track) records the exact track. The selected track is only a
    // fallback for older callers that did not provide a target.
    val effectiveTrack = targetTrack ?: selectedTrack
    val productId = paywallProductIdForTrack(effectiveTrack)
    val storeColors = rememberStoreColors(isDark = isDarkMode)
    val isEntitled = remember(playEntitlements, effectiveTrack) {
        viewModel.isTrackUnlocked(effectiveTrack)
    }
    val activeBadgeText = "ACTIVE & UNLOCKED"

    var selectedCycle by rememberSaveable(effectiveTrack.name) {
        mutableStateOf(BillingCycle.YEARLY)
    }
    var sandboxConfirmProduct by remember { mutableStateOf<PlayProductItem?>(null) }
    var sandboxConfirmCycle by remember { mutableStateOf(BillingCycle.YEARLY) }

    HyperionDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 460.dp)
                .testTag("paywall_dialog_surface")
        ) {
            StorePopupCard(
                productId = productId,
                viewModel = viewModel,
                billingManager = billingManager,
                colors = storeColors,
                isEntitled = isEntitled,
                activeBadgeText = activeBadgeText,
                selectedCardCycle = selectedCycle,
                onSelectCycle = { selectedCycle = it },
                onPurchaseCycle = { cycle ->
                    selectedCycle = cycle
                    val activity = context.findActivity()
                    val details = billingManager.getProductDetails(productId)
                    if (activity != null && (details != null || !BuildConfig.DEBUG)) {
                        billingManager.launchBillingFlow(activity, productId, cycle)
                    } else if (activity != null) {
                        sandboxConfirmProduct = billingManager.getProductItem(productId, cycle)
                        sandboxConfirmCycle = cycle
                    }
                },
                onClose = onDismiss
            )
        }
    }

    // Keep the debug-only billing simulator for emulator/preview testing.
    // Production builds always use the real Google Play Billing flow above.
    if (BuildConfig.DEBUG) {
        sandboxConfirmProduct?.let { product ->
            HyperionDialog(
                onDismissRequest = { sandboxConfirmProduct = null },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("paywall_sandbox_dialog"),
                    shape = RoundedCornerShape(10.dp),
                    color = storeColors.cardBackground,
                    border = BorderStroke(1.5.dp, storeColors.accentGreen)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "SANDBOX PURCHASE SIMULATOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = storeColors.accentGreen
                        )
                        Text(
                            text = "Simulate Google Play subscription purchase:\n\n" +
                                "• Product: ${product.title}\n" +
                                "• Cycle: ${sandboxConfirmCycle.label}\n" +
                                "• Price: ${product.formattedPrice}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = storeColors.textPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { sandboxConfirmProduct = null },
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text("Cancel", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val productToSimulate = product.productId
                                    val cycleToSimulate = sandboxConfirmCycle
                                    sandboxConfirmProduct = null
                                    billingManager.simulatePurchaseForTesting(
                                        productToSimulate,
                                        cycleToSimulate
                                    )
                                    viewModel.refreshUnlockStates()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = storeColors.accentGreen,
                                    contentColor = if (storeColors.isDark) Color.Black else Color.White
                                ),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text(
                                    "Simulate",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
