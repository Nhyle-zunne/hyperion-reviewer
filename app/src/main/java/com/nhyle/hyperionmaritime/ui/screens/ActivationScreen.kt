package com.nhyle.hyperionmaritime.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.luminance
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.data.util.ProTier
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianButton
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.MianSectionHeader
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Editorial MIAN "Spaghetti" Device Registration & Activation Screen:
 * Hardware-paired license management, tier selection, key validation.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ActivationScreen(
    viewModel: ReviewerViewModel,
    onOpenDrawer: () -> Unit = { viewModel.openDrawer() },
    modifier: Modifier = Modifier
) {
    val colors = LocalSpaghettiColors.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val promoBringIntoViewRequester = remember { BringIntoViewRequester() }
    val density = LocalDensity.current
    var promoFieldSize by remember { mutableStateOf(IntSize.Zero) }

    val isActivated by viewModel.isActivated.collectAsState()
    val isOicNwProUnlocked by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssProUnlocked by viewModel.isGmdssProUnlocked.collectAsState()
    val isHyperionProUnlocked by viewModel.isHyperionProUnlocked.collectAsState()

    var selectedTier by remember { mutableStateOf(ProTier.OIC_NW_PRO) }
    var keyInput by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var isValidationSuccess by remember { mutableStateOf(false) }

    val requestCode = remember(selectedTier) {
        LicenseManager.getRequestCodeForTier(context, selectedTier)
    }

    BackHandler {
        viewModel.navigateBack()
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.autoVerifyOnLaunch(context)
    }

    val regState by viewModel.registrationUiState.collectAsState()

    if (regState.isVerifyingOnLaunch || regState.isVerifyingLicense) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { /* Non-dismissible while verifying */ }) {
            androidx.compose.material3.Card(
                shape = RoundedCornerShape(4.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = colors.accent,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AUTHENTICATING LICENSE CLEARANCE",
                        color = colors.foreground,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Querying cryptographic server registry...",
                        color = colors.muted,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = "ACTIVATE KEY",
            subtitle = "OFFLINE KEY ACTIVATION & SUBSCRIPTION STATUS",
            showBackButton = false,
            onMenuClick = onOpenDrawer
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ============================================================================
            // THEME-AWARE TACTICAL LICENSE STATUS BAR WITH EXPIRATION & TIME LEFT
            // ============================================================================
            val isDark = colors.isDark

            // Theme-Aware Tokens: Time Left Banner & Badge
            val timeLeftContainerColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF071E13) else androidx.compose.ui.graphics.Color(0xFFDCFCE7)
            val timeLeftBorderColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF00E676) else androidx.compose.ui.graphics.Color(0xFF16A34A)
            val timeLeftTextColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF00E676) else androidx.compose.ui.graphics.Color(0xFF15803D)

            // Theme-Aware Tokens: Verify Registry Status Button
            val verifyButtonBgColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF0F141C) else androidx.compose.ui.graphics.Color(0xFFF1F5F9)
            val verifyButtonBorderColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF26354A) else androidx.compose.ui.graphics.Color(0xFFCBD5E1)
            val verifyButtonContentColor = if (isDark) androidx.compose.ui.graphics.Color(0xFFF0F6FC) else androidx.compose.ui.graphics.Color(0xFF0F172A)

            val statusAccentColor = if (regState.isLicensed) {
                if (isDark) androidx.compose.ui.graphics.Color(0xFF00E676) else androidx.compose.ui.graphics.Color(0xFF15803D)
            } else {
                if (isDark) androidx.compose.ui.graphics.Color(0xFFEF4444) else androidx.compose.ui.graphics.Color(0xFFDC2626)
            }
            val statusCardBorder = if (isDark) {
                statusAccentColor.copy(alpha = 0.6f)
            } else {
                if (regState.isLicensed) androidx.compose.ui.graphics.Color(0xFF86EFAC) else androidx.compose.ui.graphics.Color(0xFFFECACA)
            }
            val statusBadgeBg = if (isDark) {
                statusAccentColor.copy(alpha = 0.15f)
            } else {
                if (regState.isLicensed) androidx.compose.ui.graphics.Color(0xFFDCFCE7) else androidx.compose.ui.graphics.Color(0xFFFEE2E2)
            }
            val statusBadgeBorder = if (isDark) {
                statusAccentColor.copy(alpha = 0.5f)
            } else {
                if (regState.isLicensed) androidx.compose.ui.graphics.Color(0xFF22C55E).copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color(0xFFEF4444).copy(alpha = 0.6f)
            }

            androidx.compose.material3.Card(
                shape = RoundedCornerShape(4.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = colors.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    statusCardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TERMINAL CLEARANCE STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.muted,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = when {
                                    regState.isLicensed && regState.isOicLicensed && regState.isGmdssLicensed && regState.isEwLicensed && regState.isDmLicensed && regState.isEmLicensed -> {
                                        "ACTIVE (ALL-ACCESS PASS)"
                                    }
                                    regState.isLicensed && regState.isAllAccessLicensed && regState.licenseType == "SUBSCRIPTION" -> "ACTIVE (ALL-ACCESS SUBSCRIPTION)"
                                    regState.isLicensed && regState.isOicLicensed && regState.licenseType == "SUBSCRIPTION" -> "ACTIVE (OIC-NW DECK SUBSCRIPTION)"
                                    regState.isLicensed && regState.isGmdssLicensed && regState.licenseType == "SUBSCRIPTION" -> "ACTIVE (GMDSS GOC SUBSCRIPTION)"
                                    regState.isLicensed && regState.isEwLicensed && regState.licenseType == "SUBSCRIPTION" -> "ACTIVE (ENGINE OIC-EW SUBSCRIPTION)"
                                    regState.isLicensed && regState.isDmLicensed && regState.licenseType == "SUBSCRIPTION" -> "ACTIVE (DECK MANAGEMENT SUBSCRIPTION)"
                                    regState.isLicensed && regState.isEmLicensed && regState.licenseType == "SUBSCRIPTION" -> "ACTIVE (ENGINE MANAGEMENT SUBSCRIPTION)"
                                    regState.isLicensed && regState.licenseType == "OFFLINE_PASS" -> "ACTIVE (OFFLINE PASS)"
                                    regState.isLicensed && regState.licenseType == "PROMO_PASS" -> "ACTIVE (PROMO PASS)"
                                    regState.isLicensed -> "ACTIVE (VERIFIED)"
                                    else -> "RESTRICTED (UNLICENSED)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = statusAccentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Status Pill Badge (LOCKED / VERIFIED)
                        Surface(
                            color = statusBadgeBg,
                            shape = RoundedCornerShape(2.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                statusBadgeBorder
                            )
                        ) {
                            Text(
                                text = if (regState.isLicensed) "VERIFIED" else "UNVERIFIED",
                                color = statusAccentColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (regState.isLicensed) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val activeTimeLeft = when {
                            regState.isAllAccessLicensed -> regState.allAccessTimeLeft
                            regState.isOicLicensed && !regState.isGmdssLicensed && !regState.isEwLicensed && !regState.isDmLicensed && !regState.isEmLicensed -> regState.oicTimeLeft
                            regState.isGmdssLicensed && !regState.isOicLicensed && !regState.isEwLicensed && !regState.isDmLicensed && !regState.isEmLicensed -> regState.gmdssTimeLeft
                            regState.isEwLicensed && !regState.isOicLicensed && !regState.isGmdssLicensed && !regState.isDmLicensed && !regState.isEmLicensed -> regState.ewTimeLeft
                            regState.isDmLicensed && !regState.isOicLicensed && !regState.isGmdssLicensed && !regState.isEwLicensed && !regState.isEmLicensed -> regState.dmTimeLeft
                            regState.isEmLicensed && !regState.isOicLicensed && !regState.isGmdssLicensed && !regState.isEwLicensed && !regState.isDmLicensed -> regState.emTimeLeft
                            else -> regState.allAccessTimeLeft ?: regState.oicTimeLeft ?: regState.gmdssTimeLeft ?: regState.ewTimeLeft ?: regState.dmTimeLeft ?: regState.emTimeLeft
                        }
                        if (!activeTimeLeft.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = timeLeftContainerColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, timeLeftBorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⏱ TIME LEFT: $activeTimeLeft",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = timeLeftTextColor,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tactical Outline Sync Button (matches secondary buttons in light & dark mode)
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.verifyLicenseState(context) },
                        enabled = !regState.isCheckingStatus && !regState.isVerifyingOnLaunch && !regState.isVerifyingLicense,
                        shape = RoundedCornerShape(2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, verifyButtonBorderColor),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = verifyButtonBgColor,
                            contentColor = verifyButtonContentColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        if (regState.isCheckingStatus || regState.isVerifyingLicense) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = if (isDark) androidx.compose.ui.graphics.Color(0xFF00E676) else androidx.compose.ui.graphics.Color(0xFF15803D),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SYNCING WITH REGISTRY...",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.muted,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = if (isDark) androidx.compose.ui.graphics.Color(0xFF94A3B8) else androidx.compose.ui.graphics.Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VERIFY REGISTRY STATUS",
                                style = MaterialTheme.typography.labelMedium,
                                color = verifyButtonContentColor,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // Header Card
            MianRuleCard(
                ruleColor = colors.accent,
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "TERMINAL LICENSE & KEY ACTIVATION",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hyperion licenses operate via encrypted offline activation keys or Google Play auto-renewing subscriptions paired to your terminal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            }

            // Tier Selector
            MianSectionHeader(title = "SELECT LICENSE TIER")

            ProTier.values().filter { it != ProTier.HYPERION_PRO }.forEach { tier ->
                val isTierSelected = selectedTier == tier
                val isAllAccessActive = regState.isAllAccessLicensed || (regState.isOicLicensed && regState.isGmdssLicensed && regState.isEwLicensed && regState.isDmLicensed && regState.isEmLicensed)
                val isTierUnlocked = when (tier) {
                    ProTier.OIC_NW_PRO -> regState.isOicLicensed || isAllAccessActive
                    ProTier.GMDSS_PRO -> regState.isGmdssLicensed || isAllAccessActive
                    ProTier.ENGINE_OIC_EW_PRO -> regState.isEwLicensed || isAllAccessActive
                    ProTier.DECK_MGT_PRO -> regState.isDmLicensed || isAllAccessActive
                    ProTier.ENGINE_MGT_PRO -> regState.isEmLicensed || isAllAccessActive
                    ProTier.HYPERION_PRO -> isAllAccessActive
                }
                val tierTimeLeft = when (tier) {
                    ProTier.OIC_NW_PRO -> regState.oicTimeLeft ?: if (isAllAccessActive) regState.allAccessTimeLeft else null
                    ProTier.GMDSS_PRO -> regState.gmdssTimeLeft ?: if (isAllAccessActive) regState.allAccessTimeLeft else null
                    ProTier.ENGINE_OIC_EW_PRO -> regState.ewTimeLeft ?: if (isAllAccessActive) regState.allAccessTimeLeft else null
                    ProTier.DECK_MGT_PRO -> regState.dmTimeLeft ?: if (isAllAccessActive) regState.allAccessTimeLeft else null
                    ProTier.ENGINE_MGT_PRO -> regState.emTimeLeft ?: if (isAllAccessActive) regState.allAccessTimeLeft else null
                    ProTier.HYPERION_PRO -> regState.allAccessTimeLeft ?: if (regState.isOicLicensed && regState.isGmdssLicensed && regState.isEwLicensed && regState.isDmLicensed && regState.isEmLicensed) regState.oicTimeLeft else null
                }

                MianRuleCard(
                    ruleColor = if (isTierSelected) colors.accent else colors.border,
                    containerColor = if (isTierSelected) colors.surface2 else colors.surface,
                    onClick = { selectedTier = tier }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tier.title.uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isTierSelected) colors.accent else colors.foreground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tier.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                                fontSize = 11.sp
                            )
                            val tierTrack = when (tier) {
                                ProTier.OIC_NW_PRO -> com.nhyle.hyperionmaritime.data.model.Track.DECK_OIC_NW
                                ProTier.GMDSS_PRO -> com.nhyle.hyperionmaritime.data.model.Track.GMDSS_GOC
                                ProTier.ENGINE_OIC_EW_PRO -> com.nhyle.hyperionmaritime.data.model.Track.ENGINE_OIC_EW
                                ProTier.DECK_MGT_PRO -> com.nhyle.hyperionmaritime.data.model.Track.DECK_MGT
                                ProTier.ENGINE_MGT_PRO -> com.nhyle.hyperionmaritime.data.model.Track.ENGINE_MGT
                                else -> null
                            }
                            val displayQuestionCount = if (tier.questionCount.isNotBlank()) {
                                tier.questionCount
                            } else if (tierTrack != null) {
                                val total = com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.getTotalTrackQuestionCount(tierTrack)
                                "Total Question Bank: %,d questions".format(total)
                            } else ""

                            if (displayQuestionCount.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = colors.surface2
                                ) {
                                    Text(
                                        text = displayQuestionCount,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.foreground,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = if (isTierUnlocked) {
                                if (isDark) androidx.compose.ui.graphics.Color(0xFF00E676).copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color(0xFFDCFCE7)
                            } else colors.surface2,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isTierUnlocked) {
                                    if (isDark) androidx.compose.ui.graphics.Color(0xFF00E676) else androidx.compose.ui.graphics.Color(0xFF16A34A)
                                } else colors.border
                            )
                        ) {
                            Text(
                                text = if (isTierUnlocked) "VERIFIED" else "UNVERIFIED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isTierUnlocked) {
                                    if (isDark) androidx.compose.ui.graphics.Color(0xFF00E676) else androidx.compose.ui.graphics.Color(0xFF15803D)
                                } else {
                                    if (isDark) androidx.compose.ui.graphics.Color(0xFF8E9BAE) else androidx.compose.ui.graphics.Color(0xFF64748B)
                                },
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Institutional Verification Desk Button
            com.nhyle.hyperionmaritime.ui.components.LuminousDeskSupportButton(
                title = "CONTACT VERIFICATION DESK ↗",
                subtitle = "Request institutional access or candidate clearance"
            )

            // Request Code Display
            MianSectionHeader(title = "YOUR DEVICE REQUEST CODE")

            MianRuleCard(
                ruleColor = colors.border,
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val deviceModel = remember {
                        val mfg = android.os.Build.MANUFACTURER.uppercase()
                        val mdl = android.os.Build.MODEL.uppercase()
                        if (mdl.startsWith(mfg)) mdl else "$mfg $mdl"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TERMINAL REQUEST CODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.muted,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = selectedTier.shortName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.muted
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = colors.surface2,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = requestCode,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.accent,
                                modifier = Modifier.weight(1f)
                            )

                            MianButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("$requestCode ($deviceModel)"))
                                    Toast.makeText(context, "Request Code and model copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(text = "COPY", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEVICE MODEL:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = colors.muted
                        )

                        Text(
                            text = deviceModel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = colors.accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Key Input Panel
            MianSectionHeader(title = "ENTER ACTIVATION KEY")

            MianRuleCard(
                ruleColor = colors.accent,
                containerColor = colors.surface,
                modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it.uppercase()
                            viewModel.onActivationKeyChange(it.uppercase())
                        },
                        enabled = !(regState.licenseType == "LIFETIME" && regState.isAllAccessLicensed),
                        placeholder = {
                            Text(
                                text = if (regState.licenseType == "LIFETIME" && regState.isAllAccessLicensed) "YOU ALREADY HAVE FULL ALL-ACCESS CLEARANCE" else "HYPE-XXXX-XX-XXXX-XXXX-XXXX",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                color = colors.muted
                            )
                        },
                        trailingIcon = {
                            if (!(regState.licenseType == "LIFETIME" && regState.isAllAccessLicensed)) {
                                IconButton(onClick = {
                                    val clip = clipboardManager.getText()
                                    if (clip != null) {
                                        keyInput = clip.text.trim().uppercase()
                                        viewModel.onActivationKeyChange(keyInput)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = colors.foreground
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(2.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface2,
                            unfocusedContainerColor = colors.surface2,
                            disabledContainerColor = colors.surface2.copy(alpha = 0.5f),
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.foreground,
                            unfocusedTextColor = colors.foreground,
                            disabledTextColor = colors.muted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    coroutineScope.launch {
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            }
                    )

                    val activeKeyErr = regState.keyError ?: regState.activationKeyError
                    if (activeKeyErr != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeKeyErr,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.error
                        )
                    } else if (regState.licenseType == "LIFETIME" && regState.isAllAccessLicensed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "TERMINAL ACTIVATED // LIFETIME ALL-ACCESS LICENSE ACTIVE",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.success
                        )
                    } else if (regState.licenseType == "LIFETIME" && (regState.isOicLicensed || regState.isGmdssLicensed)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val activeModules = buildString {
                            if (regState.isOicLicensed) append("OIC-NW")
                            if (regState.isOicLicensed && regState.isGmdssLicensed) append(" & ")
                            if (regState.isGmdssLicensed) append("GMDSS GOC")
                        }
                        Text(
                            text = "ACTIVE MODULES: $activeModules (LIFETIME)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.success
                        )
                    } else if (regState.licenseType == "PROMO_PASS" && regState.isLicensed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PROMO PASS ACTIVE // TEMPORARY CLEARANCE GRANTED",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.success
                        )
                    } else if (regState.isLicensed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "YOU ALREADY HAVE AN ACTIVE LICENSE (${regState.licenseType})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.muted
                        )
                    } else if (validationMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = validationMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isValidationSuccess) colors.success else colors.error
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    MianButton(
                        onClick = {
                            if (regState.licenseType == "LIFETIME" && regState.isAllAccessLicensed) {
                                Toast.makeText(context, "ALL MODULES ARE ALREADY ACTIVATED ON THIS TERMINAL", Toast.LENGTH_SHORT).show()
                                return@MianButton
                            }
                            viewModel.activateDeviceWithKey(context)
                            val result = viewModel.activateWithKey(keyInput.trim(), selectedTier)
                            validationMessage = result.toastMessage
                            isValidationSuccess = result.isSuccess
                            if (result.isSuccess) {
                                Toast.makeText(context, result.toastMessage, Toast.LENGTH_LONG).show()
                            }
                        },
                        accent = true,
                        enabled = !(regState.licenseType == "LIFETIME" && regState.isAllAccessLicensed) && keyInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (regState.licenseType == "LIFETIME" && regState.isAllAccessLicensed) "TERMINAL ACTIVATED" else "ACTIVATE TERMINAL",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            MianSectionHeader(title = "REDEEM PROMO CODE (ONLINE REQUIRED)")

            MianRuleCard(
                ruleColor = colors.accent,
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ONLINE PROMO CODE / VOUCHER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.accent,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = regState.promoCodeInput,
                        onValueChange = { input -> viewModel.onPromoCodeChange(input.uppercase()) },
                        enabled = !regState.isLicensed,
                        placeholder = {
                            Text(
                                text = if (regState.isLicensed) "YOU ALREADY HAVE AN ACTIVE LICENSE" else "ENTER YOUR PROMO CODE HERE",
                                color = androidx.compose.ui.graphics.Color(0xFF64748B)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(2.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface2,
                            unfocusedContainerColor = colors.surface2,
                            disabledContainerColor = colors.surface2.copy(alpha = 0.5f),
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.foreground,
                            unfocusedTextColor = colors.foreground,
                            disabledTextColor = colors.muted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(promoBringIntoViewRequester)
                            .onGloballyPositioned { coordinates ->
                                promoFieldSize = coordinates.size
                            }
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    coroutineScope.launch {
                                        val extraBottom = with(density) { 12.dp.toPx() }
                                        val width = promoFieldSize.width.toFloat()
                                        val height = promoFieldSize.height.toFloat()
                                        val targetRect = if (height > 0f) {
                                            Rect(
                                                left = 0f,
                                                top = 0f,
                                                right = width,
                                                bottom = height + extraBottom
                                            )
                                        } else null
                                        promoBringIntoViewRequester.bringIntoView(targetRect)
                                        kotlinx.coroutines.delay(200)
                                        promoBringIntoViewRequester.bringIntoView(targetRect)
                                    }
                                }
                            }
                    )

                    if (regState.promoError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = regState.promoError!!,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.error
                        )
                    } else if (regState.isLicensed && regState.licenseType == "PROMO_PASS") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PROMO PASS APPLIED // CLEARANCE GRANTED",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.success
                        )
                    } else if (regState.isLicensed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "YOU ALREADY HAVE AN ACTIVE LICENSE (${regState.licenseType})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.muted
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    MianButton(
                        onClick = {
                            viewModel.redeemPromoCode(context)
                        },
                        accent = true,
                        enabled = !regState.isLicensed && regState.promoCodeInput.isNotBlank() && !regState.isPromoLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (regState.isPromoLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.foreground,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VERIFYING PROMO...",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (regState.isLicensed) "PROMO ACTIVE" else "REDEEM PROMO CODE",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
