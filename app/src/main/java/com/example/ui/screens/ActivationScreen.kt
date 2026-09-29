package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.ProTier
import com.example.ui.components.DeviceVerificationCard
import com.example.ui.components.MaritimeTopBar
import com.example.ui.theme.FailRed
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun ActivationScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isOicNwProUnlocked by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssProUnlocked by viewModel.isGmdssProUnlocked.collectAsState()
    val isBothUnlocked = isOicNwProUnlocked && isGmdssProUnlocked
    val isAnyUnlocked = isOicNwProUnlocked || isGmdssProUnlocked
    val isDark = isSystemInDarkTheme()

    val screenBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardSurface = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) BorderStroke(1.dp, Color(0xFF334155)) else BorderStroke(1.dp, Color(0xFFCBD5E1))
    val primaryTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    var keyInput by remember { mutableStateOf("") }
    var activationError by remember { mutableStateOf<String?>(null) }

    BackHandler {
        viewModel.navigateBack()
    }

    /**
     * Formats incoming user text to uppercase and adds automatic dash grouping.
     * Preserves smooth deletion when user presses backspace.
     */
    fun formatLicenseKey(newVal: String, oldVal: String): String {
        val upper = newVal.uppercase().filter { it.isLetterOrDigit() || it == '-' }
        
        // If user is deleting characters, allow natural backspacing
        if (upper.length < oldVal.length) {
            return upper
        }

        val rawAlphanumeric = upper.filter { it.isLetterOrDigit() }
        if (rawAlphanumeric.length > 32) return oldVal

        // If user is typing without dashes or pasted raw alphanumeric string, auto-insert dashes every 4 chars
        if (!upper.contains("-")) {
            val sb = StringBuilder()
            for (i in rawAlphanumeric.indices) {
                sb.append(rawAlphanumeric[i])
                if ((i + 1) % 4 == 0 && i != rawAlphanumeric.lastIndex && (i + 1) < 24) {
                    sb.append("-")
                }
            }
            return sb.toString()
        }

        // If user typed beyond a 4-block without entering dash manually (e.g. ABCD-EFGHI -> ABCD-EFGH-I)
        val segments = upper.split("-")
        if (segments.any { it.length > 4 }) {
            val sb = StringBuilder()
            for (i in rawAlphanumeric.indices) {
                sb.append(rawAlphanumeric[i])
                if ((i + 1) % 4 == 0 && i != rawAlphanumeric.lastIndex && (i + 1) < 24) {
                    sb.append("-")
                }
            }
            return sb.toString()
        }

        return upper
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .navigationBarsPadding()
    ) {
        MaritimeTopBar(
            title = "Device Registration",
            showBackButton = true,
            onBackClick = { viewModel.navigateBack() },
            onMenuClick = { viewModel.openDrawer() },
            onLegalClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isAnyUnlocked) {
                val bannerTitle = when {
                    isBothUnlocked -> "Full Review Suite Active"
                    isOicNwProUnlocked -> "OIC-NW Deck Track Active"
                    else -> "GMDSS Radio Track Active"
                }
                val bannerDesc = when {
                    isBothUnlocked -> "Your terminal registration is active for this device. Full question repository, Intensive Mode recycling drill, and official timed mock exams are unlocked offline."
                    isOicNwProUnlocked -> "Deck Operational Level reviewer is permanently verified. Full 12-competence question bank, Intensive drills, and timed mock exams are unlocked offline."
                    else -> "GMDSS Radio Operator reviewer is permanently verified. Full GOC subsystems, emergency radio communications, and timed mock exams are unlocked offline."
                }

                // Status banner card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardSurface
                    ),
                    border = BorderStroke(1.5.dp, PassGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PassGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = bannerTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PassGreen
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PassGreen.copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, PassGreen)
                            ) {
                                Text(
                                    text = "VERIFIED",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PassGreen,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = bannerDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Standalone Device Verification & Activation Card
            DeviceVerificationCard(
                initialTier = ProTier.HYPERION_PRO
            )

            if (!isBothUnlocked) {
                // Key Input & Verification Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    border = cardBorder,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OFFLINE VERIFICATION",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Enter your verification key to complete offline terminal registration",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { input ->
                                keyInput = formatLicenseKey(input, keyInput)
                                activationError = null
                            },
                            label = { Text("Verification Key (e.g. XXXX-XXXX-XXXX-XXXX-XXXX)") },
                            placeholder = { Text("XXXX-XXXX-XXXX-XXXX-XXXX") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    if (keyInput.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                keyInput = ""
                                                activationError = null
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = secondaryTextColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text?.trim() ?: ""
                                            if (clip.isNotEmpty()) {
                                                val cleaned = clip.uppercase().filter { it.isLetterOrDigit() || it == '-' }
                                                val formatted = if (!cleaned.contains("-") && cleaned.length >= 8) {
                                                    val sb = StringBuilder()
                                                    val raw = cleaned.filter { it.isLetterOrDigit() }
                                                    for (i in raw.indices) {
                                                        sb.append(raw[i])
                                                        if ((i + 1) % 4 == 0 && i != raw.lastIndex && (i + 1) < 24) {
                                                            sb.append("-")
                                                        }
                                                    }
                                                    sb.toString()
                                                } else {
                                                    cleaned
                                                }
                                                keyInput = formatted
                                                activationError = null
                                                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.testTag("paste_key_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste from Clipboard",
                                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("license_key_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Error Message
                        AnimatedVisibility(visible = activationError != null) {
                            activationError?.let { err ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = FailRed.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, FailRed.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    Text(
                                        text = "⚠️ $err",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FailRed,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val result = viewModel.activateLicense(keyInput)
                                if (result.isSuccess) {
                                    activationError = null
                                    Toast.makeText(context, result.toastMessage, Toast.LENGTH_LONG).show()
                                } else {
                                    activationError = result.toastMessage
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("activate_license_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaritimeGoldBright,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Verify Terminal",
                                color = Color(0xFF0F172A),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.navigateTo(Screen.HowToAvail)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("view_how_to_avail_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaritimeGoldBright)
                        ) {
                            Text(
                                text = "Terminal Registration Guide",
                                color = MaritimeGoldBright,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Offline Assurance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardSurface),
                border = cardBorder
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SeaCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Offline Cryptographic Security",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SeaCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Hyperion uses on-device SHA-256 validation. No internet connection or tracking server is contacted. Activation persists securely on this device across launches.",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
