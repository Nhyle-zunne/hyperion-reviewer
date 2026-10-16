package com.nhyle.hyperionmaritime.ui.components

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.data.util.ProTier
import com.nhyle.hyperionmaritime.data.util.UnlockResult
import com.nhyle.hyperionmaritime.ui.theme.FailRed
import com.nhyle.hyperionmaritime.ui.theme.MaritimeGoldBright
import com.nhyle.hyperionmaritime.ui.theme.PassGreen
import com.nhyle.hyperionmaritime.ui.theme.SeaCyan

/**
 * Standalone Device Verification & Activation Card
 * Displays hardware-bound & session diagnostics, tier selector, dynamic Pro Request Code,
 * 1-tap "Copy Verification Details" action, and light/dark mode contrast tokens.
 */
@Composable
fun DeviceVerificationCard(
    modifier: Modifier = Modifier,
    initialTier: ProTier = ProTier.HYPERION_PRO,
    onTierChanged: ((ProTier) -> Unit)? = null,
    onDetailsCopied: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val colors = com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors.current
    val isDark = colors.isDark

    var selectedTier by remember { mutableStateOf(initialTier) }

    val actualHwId = remember { LicenseManager.getPermanentHardwareId(context) }
    val actualSessionId = remember { LicenseManager.getInstallSessionId(context) }
    val actualInstallDate = remember { LicenseManager.getInstallDate(context) }
    val deviceModel = remember { "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}" }
    val actualRequestCode = LicenseManager.getRequestCodeForTier(context, selectedTier)

    var copiedFeedback by remember { mutableStateOf(false) }

    // Contrast tokens based on active theme
    val cardSurface = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val cardBorder = if (isDark) BorderStroke(1.dp, Color(0xFF334155)) else BorderStroke(1.dp, Color(0xFFCBD5E1))
    val primaryText = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val codeBoxFill = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val codeBoxBorderColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val codeTextColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0F172A)
    val copyButtonBg = Color(0xFF0284C7)

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("device_verification_card"),
            shape = RectangleShape,
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            border = cardBorder,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with Hardware / Session Identifier Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DEVICE VERIFICATION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Surface(
                        shape = RectangleShape,
                        color = if (isDark) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFFE0F2FE),
                        border = BorderStroke(0.5.dp, if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                    ) {
                        Text(
                            text = "OFFLINE VERIFICATION",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Module / Suite Selection Cards
                Text(
                    text = "SELECT REVIEW MODULE / SUITE:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProTier.values().forEach { tier ->
                        val isSelected = tier == selectedTier
                        val tierBg = if (isSelected) {
                            if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
                        } else {
                            if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFE2E8F0).copy(alpha = 0.5f)
                        }
                        val tierBorder = if (isSelected) {
                            BorderStroke(1.5.dp, if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                        } else {
                            BorderStroke(0.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                        }

                        val tierDisplayTitle = when (tier) {
                            ProTier.OIC_NW_PRO -> "OIC-NW Deck Operational"
                            ProTier.GMDSS_PRO -> "GMDSS Radio Communications"
                            ProTier.ENGINE_OIC_EW_PRO -> "OIC-EW Engine Operational"
                            ProTier.DECK_MGT_PRO -> "Deck Management Level"
                            ProTier.ENGINE_MGT_PRO -> "Engine Management Level"
                            ProTier.HYPERION_PRO -> "Full Maritime Review Suite"
                        }
                        val tierDisplaySub = when (tier) {
                            ProTier.OIC_NW_PRO -> "Full 12-Competence Deck Question Bank & Mocks"
                            ProTier.GMDSS_PRO -> "Complete GOC Subsystems & Emergency Radio Mocks"
                            ProTier.ENGINE_OIC_EW_PRO -> "Full 13-Competence Engine Question Bank & Mocks"
                            ProTier.DECK_MGT_PRO -> "Full 18-Competence Management Question Bank & Mocks"
                            ProTier.ENGINE_MGT_PRO -> "Full 14-Competence Management Question Bank & Mocks"
                            ProTier.HYPERION_PRO -> "Complete Deck Operational + GMDSS Suite"
                        }

                        Surface(
                            shape = RectangleShape,
                            color = tierBg,
                            border = tierBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RectangleShape)
                                .clickable {
                                    selectedTier = tier
                                    onTierChanged?.invoke(tier)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) else secondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = tierDisplayTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryText
                                        )
                                        if (tier == ProTier.HYPERION_PRO) {
                                            Surface(
                                                shape = RectangleShape,
                                                color = MaritimeGoldBright.copy(alpha = 0.2f),
                                                border = BorderStroke(0.5.dp, MaritimeGoldBright)
                                            ) {
                                                Text(
                                                    text = "RECOMMENDED",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaritimeGoldBright,
                                                    fontSize = 8.5.sp
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = tierDisplaySub,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryText,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SelectionContainer enables manual text selection
                SelectionContainer {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Permanent Hardware ID
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Device ID:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryText,
                                fontSize = 12.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = actualHwId,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = primaryText,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RectangleShape,
                                    color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ) {
                                    Text(
                                        text = "(Hardware Bound)",
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = secondaryText,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        // Install Session ID
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Session:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryText,
                                fontSize = 12.sp
                            )
                            Text(
                                text = actualSessionId,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = primaryText,
                                fontSize = 13.sp
                            )
                        }

                        // Selected Track / Suite
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val labelPrefix = when (selectedTier) {
                                ProTier.OIC_NW_PRO, ProTier.GMDSS_PRO, ProTier.ENGINE_OIC_EW_PRO, ProTier.DECK_MGT_PRO, ProTier.ENGINE_MGT_PRO -> "Track:"
                                ProTier.HYPERION_PRO -> "Suite:"
                            }
                            val suiteName = when (selectedTier) {
                                ProTier.OIC_NW_PRO -> "OIC-NW Deck Operational"
                                ProTier.GMDSS_PRO -> "GMDSS Radio Communications"
                                ProTier.ENGINE_OIC_EW_PRO -> "OIC-EW Engine Operational"
                                ProTier.DECK_MGT_PRO -> "Deck Management Level"
                                ProTier.ENGINE_MGT_PRO -> "Engine Management Level"
                                ProTier.HYPERION_PRO -> "Full Maritime Review Suite"
                            }

                            Text(
                                text = labelPrefix,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryText,
                                fontSize = 12.sp
                            )
                            Text(
                                text = suiteName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                fontSize = 12.sp
                            )
                        }

                        // Install Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Date:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryText,
                                fontSize = 12.sp
                            )
                            Text(
                                text = actualInstallDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryText,
                                fontSize = 12.sp
                            )
                        }

                        // Device Model
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Model:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryText,
                                fontSize = 12.sp
                            )
                            Text(
                                text = deviceModel,
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryText,
                                fontSize = 12.sp
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                            thickness = 0.8.dp
                        )

                        // Master Request Code Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TERMINAL REGISTRATION CODE:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Request Code Box with Dual-Theme Contrast Styling
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_code_box"),
                            shape = RectangleShape,
                            color = codeBoxFill,
                            border = BorderStroke(1.5.dp, codeBoxBorderColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = actualRequestCode,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = codeTextColor,
                                    modifier = Modifier.testTag("device_request_code_text")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Single "Copy All Details" Action Button
                Button(
                    onClick = {
                        val trackOrSuite = when (selectedTier) {
                            ProTier.OIC_NW_PRO -> "Track: OIC-NW Deck Operational"
                            ProTier.GMDSS_PRO -> "Track: GMDSS Radio Communications"
                            ProTier.ENGINE_OIC_EW_PRO -> "Track: OIC-EW Engine Operational"
                            ProTier.DECK_MGT_PRO -> "Track: Deck Management Level"
                            ProTier.ENGINE_MGT_PRO -> "Track: Engine Management Level"
                            ProTier.HYPERION_PRO -> "Suite: Full Maritime Review Suite"
                        }
                        val fullDetails = """
                            Device ID: $actualHwId
                            Session: $actualSessionId
                            $trackOrSuite
                            Date: $actualInstallDate
                            Model: $deviceModel
                            Terminal Code: $actualRequestCode
                        """.trimIndent()

                        clipboardManager.setText(AnnotatedString(fullDetails))
                        copiedFeedback = true
                        Toast.makeText(context, "All registration details copied to clipboard!", Toast.LENGTH_SHORT).show()
                        onDetailsCopied?.invoke()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("copy_verification_details_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = copyButtonBg,
                        contentColor = Color.White
                    ),
                    shape = RectangleShape,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy Verification Details",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (copiedFeedback) "Copied All Details!" else "Copy Verification Details",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // User Guidance Caption below the card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = secondaryText,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Tip: Copy or screenshot this card. If you ever reinstall or reset your phone, your Device Request Code verifies your terminal registration.",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Standalone ActivationDialog for modal activation flows
 */
@Composable
fun ActivationDialog(
    onDismissRequest: () -> Unit,
    initialTier: ProTier = ProTier.HYPERION_PRO,
    onActivationSuccess: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors.current
    val isDark = colors.isDark
    val dialogCanvas = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val primaryText = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    var currentTier by remember { mutableStateOf(initialTier) }
    var keyInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RectangleShape,
            color = dialogCanvas,
            border = BorderStroke(1.5.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dialog Header: "Verify Terminal Access" with subtitle "Verify offline access for this device"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = MaritimeGoldBright,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Verify Terminal Access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText
                            )
                        }
                        Text(
                            text = "Verify offline access for this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryText,
                            fontSize = 11.5.sp
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                )

                // Device Verification & Tier Card
                DeviceVerificationCard(
                    initialTier = currentTier,
                    onTierChanged = { tier ->
                        currentTier = tier
                        errorMessage = null
                    }
                )

                // License Key Input
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ENTER VERIFICATION KEY:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { input ->
                            keyInput = input.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '-' }
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("license_key_input_field"),
                        placeholder = {
                            Text(
                                "HYPE-XXXX-XX-XXXX-XXXX-XXXX",
                                color = secondaryText.copy(alpha = 0.6f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                            focusedTextColor = primaryText,
                            unfocusedTextColor = primaryText
                        ),
                        shape = RectangleShape
                    )

                    errorMessage?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.labelSmall,
                            color = FailRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var isVerifying by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isVerifying = true
                                val result = LicenseManager.verifyAndUnlockKey(context, keyInput, currentTier)
                                if (result.isSuccess) {
                                    val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
                                    prefs.edit()
                                        .putBoolean("is_full_access_unlocked", true)
                                        .putBoolean("is_hardware_lifetime", true)
                                        .apply()
                                    Toast.makeText(context, result.toastMessage, Toast.LENGTH_LONG).show()
                                    onActivationSuccess?.invoke()
                                    onDismissRequest()
                                } else {
                                    errorMessage = result.toastMessage
                                }
                                isVerifying = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("activate_pro_submit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaritimeGoldBright,
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = "Verify Terminal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}
