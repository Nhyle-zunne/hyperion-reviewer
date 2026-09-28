package com.example.ui.components

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.util.LicenseManager

/**
 * Standalone Device Verification & Activation Card
 * Provides hardware-bound & session diagnostics, high contrast Master Request Code display,
 * 1-tap "Copy Verification Details" action, and light/dark mode contrast tokens.
 */
@Composable
fun DeviceVerificationCard(
    modifier: Modifier = Modifier,
    permanentHardwareId: String? = null,
    installSessionId: String? = null,
    installDate: String? = null,
    requestCode: String? = null,
    onDetailsCopied: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()

    val actualHwId = permanentHardwareId ?: remember { LicenseManager.getPermanentHardwareId(context) }
    val actualSessionId = installSessionId ?: remember { LicenseManager.getInstallSessionId(context) }
    val actualInstallDate = installDate ?: remember { LicenseManager.getInstallDate(context) }
    val actualRequestCode = requestCode ?: remember { LicenseManager.getMasterRequestCode(context) }
    val deviceModel = remember { "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}" }

    var copiedFeedback by remember { mutableStateOf(false) }

    // Explicit contrast tokens based on active theme
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
            shape = RoundedCornerShape(14.dp),
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
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFFE0F2FE),
                        border = BorderStroke(0.5.dp, if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                    ) {
                        Text(
                            text = "OFFLINE LICENSE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                                    shape = RoundedCornerShape(4.dp),
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
                                text = "MASTER REQUEST CODE:",
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
                            shape = RoundedCornerShape(10.dp),
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
                                        letterSpacing = 2.sp
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
                        val fullDetails = """
                            Device ID: $actualHwId
                            Session: $actualSessionId
                            Date: $actualInstallDate
                            Model: $deviceModel
                            Request Code: $actualRequestCode
                        """.trimIndent()

                        clipboardManager.setText(AnnotatedString(fullDetails))
                        copiedFeedback = true
                        Toast.makeText(context, "All activation details copied to clipboard!", Toast.LENGTH_SHORT).show()
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
                    shape = RoundedCornerShape(10.dp),
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
                text = "Tap Copy or screenshot this card. If you ever reinstall or reset your phone, your Permanent Hardware ID verifies your previous purchase even if your install session changes.",
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
    permanentHardwareId: String? = null,
    installSessionId: String? = null,
    installDate: String? = null,
    requestCode: String? = null,
    onActivate: (String) -> Boolean
) {
    val isDark = isSystemInDarkTheme()
    val dialogCanvas = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = dialogCanvas,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DeviceVerificationCard(
                    permanentHardwareId = permanentHardwareId,
                    installSessionId = installSessionId,
                    installDate = installDate,
                    requestCode = requestCode
                )
            }
        }
    }
}
