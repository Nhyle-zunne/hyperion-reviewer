package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.LicenseManager
import com.example.ui.components.MaritimeTopBar
import com.example.ui.theme.FailRed
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun ActivationScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isActivatedState by viewModel.isActivated.collectAsState()
    val isActivated = LicenseManager.isActivated(context) || isActivatedState
    val clipboardManager = LocalClipboardManager.current

    val requestCode = viewModel.deviceRequestCode
    var keyInput by remember { mutableStateOf("") }
    var activationError by remember { mutableStateOf<String?>(null) }
    var copiedFeedback by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateTo(Screen.Home)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        MaritimeTopBar(
            title = "Activation & License",
            showBackButton = true,
            onBackClick = { viewModel.navigateTo(Screen.Home) },
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
            if (isActivated) {
                // Green "Hyperion Pro Active" status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0A2E20)
                    ),
                    border = BorderStroke(1.5.dp, PassGreen)
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
                                    text = "Hyperion Pro Active",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PassGreen
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PassGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "ACTIVE",
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
                            text = "Your license is permanently activated for this device. Full question repository, Intensive Mode recycling drill, and official timed mock exams are completely unlocked offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                // Device Request Code Card with Copy ID Button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = SeaCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DEVICE REQUEST CODE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SeaCyan,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Provide this unique hardware identifier to obtain your offline SHA-256 license key:",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Code Display Box with Copy ID Button
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F1E36),
                            border = BorderStroke(1.dp, SeaCyan.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = requestCode,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaritimeGoldBright,
                                    modifier = Modifier.testTag("device_request_code_text")
                                )

                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(requestCode))
                                        copiedFeedback = true
                                        Toast.makeText(context, "Request Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SeaCyan,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("copy_request_code_button")
                                ) {
                                    Icon(
                                        imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "Copy ID",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (copiedFeedback) "Copied!" else "Copy ID",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Key Input & Activation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = MaritimeGoldBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OFFLINE LICENSE ACTIVATION",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaritimeGoldBright,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Enter your activation key to permanently activate Hyperion Pro:",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = {
                                keyInput = it
                                activationError = null
                            },
                            label = { Text("Activation Key (e.g. ACTV-XXXX-XXXX-XXXX)") },
                            placeholder = { Text("ACTV-XXXX-XXXX-XXXX") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("license_key_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaritimeGoldBright,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
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
                                val success = viewModel.activateLicense(keyInput)
                                if (success) {
                                    activationError = null
                                    Toast.makeText(context, "Hyperion Pro activated successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    activationError = "Invalid key for this device identifier."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("activate_license_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaritimeGoldBright,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Activate Pro",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Offline Assurance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Quick Access to Free Practice
            if (!isActivated) {
                OutlinedButton(
                    onClick = { viewModel.startFreePractice() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SeaCyan),
                    border = BorderStroke(1.dp, SeaCyan.copy(alpha = 0.6f))
                ) {
                    Text("Return to Free Practice (No Activation Required)")
                }
            }
        }
    }
}
