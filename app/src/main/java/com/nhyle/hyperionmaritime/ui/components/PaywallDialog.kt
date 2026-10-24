package com.nhyle.hyperionmaritime.ui.components

import android.app.Activity
import android.content.Context
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.ui.motion.HyperionDialog
import androidx.compose.ui.window.DialogProperties
import com.nhyle.hyperionmaritime.data.billing.BillingConstants
import com.nhyle.hyperionmaritime.data.billing.BillingCycle
import com.nhyle.hyperionmaritime.data.billing.PlayProductItem
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.ui.store.findActivity
import com.nhyle.hyperionmaritime.ui.theme.FailRed
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.ui.theme.PassGreen
import com.nhyle.hyperionmaritime.ui.theme.WarningAmber
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen

/**
 * Ultra-Compact Industrial Dark-Mode Paywall Dialog for Hyperion Maritime Exam Reviewer.
 *
 * Implements:
 * - Real-time localized pricing from Google Play across Weekly, Monthly, and Yearly cycles.
 * - Segmented cycle selector tab bar [ Weekly ] | [ Monthly ] | [ Yearly (Save ~48%) ].
 * - Free Preview 5-question limit ceiling notice banner.
 * - Target Track vs All-Access Pass presentation.
 * - Clean "Continue Free Preview" dismiss button.
 * - Alternative offline terminal key entry shortcut.
 * - Safe Activity unwrapping via Context.findActivity().
 * - Mock Sandbox Confirmation Dialog for browser emulator / preview testing.
 */
@Composable
fun PaywallDialog(
    viewModel: ReviewerViewModel,
    targetTrack: Track? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val context = LocalContext.current
    val billingManager = viewModel.billingManager

    val entitlements by billingManager.entitlements.collectAsState()
    var selectedCycle by rememberSaveable { mutableStateOf(BillingCycle.YEARLY) }

    val productItems = remember(entitlements, selectedCycle) {
        billingManager.getAllProductItems(selectedCycle)
    }

    val scrollState = rememberScrollState()
    var sandboxConfirmProduct by remember { mutableStateOf<PlayProductItem?>(null) }

    val isOicOffline = LicenseManager.isOicNwProUnlocked(context)
    val isGmdssOffline = LicenseManager.isGmdssProUnlocked(context)
    val isAllAccessOffline = LicenseManager.isHyperionProUnlocked(context) || (isOicOffline && isGmdssOffline)

    HyperionDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .padding(16.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .testTag("paywall_dialog_surface"),
            shape = RoundedCornerShape(12.dp),
            color = colors.background,
            border = BorderStroke(1.5.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(colors.accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, colors.accent.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "STCW MARITIME PASS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = colors.accent
                            )
                            Text(
                                text = "Unlock Full Question Bank",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = colors.foreground
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("paywall_dismiss_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Paywall",
                            tint = colors.muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Free Preview Ceiling Notice Banner (Dense)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(6.dp))
                        .border(1.dp, colors.accent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "FREE PREVIEW LIMIT REACHED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                ),
                                color = colors.accent
                            )
                            Text(
                                text = "You have completed the 5-question preview limit. Unlock a pass to access complete banks and timed exams.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 13.sp),
                                color = colors.foreground
                            )
                        }
                    }
                }

                // Segmented Cycle Selector
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BillingCycle.values().forEach { cycle ->
                            val isSelected = selectedCycle == cycle
                            val containerColor = if (isSelected) colors.accent else Color.Transparent
                            val contentColor = if (isSelected) Color.Black else colors.muted

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .background(containerColor, RoundedCornerShape(4.dp))
                                    .clickable { selectedCycle = cycle }
                                    .padding(horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cycle.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = if (cycle == BillingCycle.YEARLY) 9.5.sp else 10.5.sp
                                    ),
                                    color = contentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Error banner
                AnimatedVisibility(
                    visible = entitlements.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FailRed.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .border(1.dp, FailRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = entitlements.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = FailRed
                        )
                    }
                }

                // Filter items to show Target Track + All Access (or all 3)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    productItems.forEach { item ->
                        val isHighlighted = targetTrack?.let { track ->
                            (track == Track.DECK_OIC_NW && item.productId == BillingConstants.PRODUCT_OIC_NW_PASS) ||
                            (track == Track.GMDSS_GOC && item.productId == BillingConstants.PRODUCT_GMDSS_PASS) ||
                            (item.productId == BillingConstants.PRODUCT_ALL_ACCESS_PASS)
                        } ?: (item.productId == BillingConstants.PRODUCT_ALL_ACCESS_PASS)

                        val isOfflineUnlocked = when (item.productId) {
                            BillingConstants.PRODUCT_ALL_ACCESS_PASS -> isAllAccessOffline
                            BillingConstants.PRODUCT_OIC_NW_PASS -> isOicOffline || isAllAccessOffline
                            BillingConstants.PRODUCT_GMDSS_PASS -> isGmdssOffline || isAllAccessOffline
                            else -> false
                        }
                        val isUnlocked = isOfflineUnlocked || item.isOwned

                        CompactPaywallCard(
                            item = item,
                            cycle = selectedCycle,
                            isHighlighted = isHighlighted,
                            isUnlocked = isUnlocked,
                            onPurchase = {
                                val details = billingManager.getProductDetails(item.productId)
                                val activity = context.findActivity()
                                if (activity != null) {
                                    if (details != null || !com.nhyle.hyperionmaritime.BuildConfig.DEBUG) {
                                        billingManager.launchBillingFlow(activity, item.productId, selectedCycle)
                                    } else if (com.nhyle.hyperionmaritime.BuildConfig.DEBUG) {
                                        sandboxConfirmProduct = item
                                    }
                                }
                            }
                        )
                    }
                }

                // Action Buttons Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("paywall_continue_preview_button")
                    ) {
                        Text(
                            text = "Continue Free Preview",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = colors.muted
                            )
                        )
                    }

                    TextButton(
                        onClick = {
                            onDismiss()
                            viewModel.navigateTo(Screen.License)
                        },
                        modifier = Modifier.testTag("paywall_terminal_key_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Offline Key",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                        )
                    }
                }
            }
        }
    }

    // Sandbox Simulator Confirmation Dialog (Debug Only)
    if (com.nhyle.hyperionmaritime.BuildConfig.DEBUG) {
        sandboxConfirmProduct?.let { product ->
            HyperionDialog(
                onDismissRequest = { sandboxConfirmProduct = null },
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("paywall_sandbox_dialog"),
                shape = RoundedCornerShape(10.dp),
                color = colors.surface,
                border = BorderStroke(1.5.dp, colors.accent)
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
                        color = colors.accent
                    )
                    Text(
                        text = "Simulate Google Play subscription purchase:\n\n• Product: ${product.title}\n• Cycle: ${selectedCycle.label}\n• Price: ${product.formattedPrice}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = colors.foreground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { sandboxConfirmProduct = null },
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val id = product.productId
                                val cycle = selectedCycle
                                sandboxConfirmProduct = null
                                billingManager.simulatePurchaseForTesting(id, cycle)
                                viewModel.refreshUnlockStates()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PassGreen,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("Simulate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun CompactPaywallCard(
    item: PlayProductItem,
    cycle: BillingCycle,
    isHighlighted: Boolean,
    isUnlocked: Boolean,
    onPurchase: () -> Unit
) {
    val colors = LocalSpaghettiColors.current
    val borderColor = when {
        isUnlocked -> PassGreen.copy(alpha = 0.5f)
        isHighlighted -> PassGreen
        else -> colors.border
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("paywall_card_${item.productId}"),
        shape = RoundedCornerShape(8.dp),
        color = colors.surface,
        border = BorderStroke(if (isHighlighted) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = colors.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.productId == BillingConstants.PRODUCT_ALL_ACCESS_PASS) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = PassGreen,
                            contentColor = Color.Black
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 7.5.sp
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = item.formattedPrice,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = if (isUnlocked) PassGreen else colors.accent
                )
            }

            if (isUnlocked) {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = colors.background,
                        disabledContentColor = PassGreen
                    ),
                    border = BorderStroke(1.dp, PassGreen.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Text(
                        text = "ACTIVE & UNLOCKED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            } else {
                Button(
                    onClick = onPurchase,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHighlighted) PassGreen else colors.accent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("paywall_purchase_button_${item.productId}")
                ) {
                    Text(
                        text = "UNLOCK — ${item.shortFormattedPrice}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
