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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.ui.components.MaritimeTopBar
import com.nhyle.hyperionmaritime.ui.components.MianButton
import com.nhyle.hyperionmaritime.ui.components.MianRuleCard
import com.nhyle.hyperionmaritime.ui.components.MianSectionHeader
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Editorial MIAN "Spaghetti" License & Verification Screen:
 * Terminal verification, request code display, key entry.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LicenseScreen(
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

    val isActivated by viewModel.isActivated.collectAsState()
    val isOicNwProUnlocked by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssProUnlocked by viewModel.isGmdssProUnlocked.collectAsState()

    var keyInput by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var isValidationSuccess by remember { mutableStateOf(false) }

    val offlineInfo = remember(isActivated) {
        LicenseManager.getOfflineLicenseInfo(context)
    }

    val requestCode = remember {
        LicenseManager.getUniversalDeviceCode(context)
    }

    BackHandler {
        viewModel.navigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        MaritimeTopBar(
            title = "TERMINAL VERIFICATION",
            subtitle = "OFFLINE LICENSE STATUS",
            showBackButton = false,
            onMenuClick = onOpenDrawer
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status Card
            MianRuleCard(
                ruleColor = when {
                    offlineInfo?.isTampered == true -> colors.error
                    isActivated -> colors.success
                    else -> colors.accent
                },
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TERMINAL STATUS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = colors.surface2
                            ) {
                                Text(
                                    text = when {
                                        offlineInfo?.isTampered == true -> "TAMPERED"
                                        isActivated -> "ACTIVE"
                                        else -> "LOCKED"
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        offlineInfo?.isTampered == true -> colors.error
                                        isActivated -> colors.success
                                        else -> colors.accent
                                    },
                                    fontSize = 10.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = if (isActivated && offlineInfo?.isTampered != true) colors.success.copy(alpha = 0.15f) else colors.surface2
                            ) {
                                Text(
                                    text = when {
                                        offlineInfo?.isTampered == true -> "TIME MISMATCH"
                                        isActivated -> offlineInfo?.durationLabel?.uppercase() ?: "VERIFIED"
                                        else -> "VERIFICATION REQUIRED"
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        offlineInfo?.isTampered == true -> colors.error
                                        isActivated -> colors.success
                                        else -> colors.accent
                                    },
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            offlineInfo?.isTampered == true -> "Device clock rollback detected. Please restore the correct date and time in your device settings."
                            isActivated -> "Full question bank and timed mock examination access active on this terminal."
                            else -> "Please activate your terminal license to unlock all question banks and unlimited timed mock exams."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (offlineInfo?.isTampered == true) colors.error else colors.muted
                    )

                    if (isActivated && offlineInfo?.isTampered != true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = colors.success.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.success)
                        ) {
                            val activeText = if (offlineInfo != null && offlineInfo.isActive) {
                                "TERMINAL ACTIVATED // ${offlineInfo.durationLabel.uppercase()} • ${offlineInfo.getFormattedTimeRemaining().uppercase()}"
                            } else {
                                "TERMINAL ACTIVATED // ACCESS GRANTED"
                            }
                            Text(
                                text = activeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.success,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            com.nhyle.hyperionmaritime.ui.components.LuminousDeskSupportButton(
                title = "CONTACT VERIFICATION DESK ↗",
                subtitle = "Request institutional access or candidate clearance"
            )

            MianSectionHeader(title = "YOUR TERMINAL REQUEST CODE")

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
                            text = "DEVICE REQUEST CODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.muted,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "TERMINAL",
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
                                    Toast.makeText(context, "Request code and model copied", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("COPY", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
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

            MianSectionHeader(title = "ACTIVATION KEY ENTRY")

            MianRuleCard(
                ruleColor = colors.accent,
                containerColor = colors.surface,
                modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it.uppercase() },
                        placeholder = {
                            Text(
                                text = "HYPE-XXXX-XX-XXXX-XXXX-XXXX",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                color = colors.muted
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                val clip = clipboardManager.getText()
                                if (clip != null) {
                                    keyInput = clip.text.trim().uppercase()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = colors.foreground
                                )
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
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.foreground,
                            unfocusedTextColor = colors.foreground
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

                    if (validationMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = validationMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isValidationSuccess) colors.success else colors.error
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    var isRedeeming by remember { mutableStateOf(false) }

                    MianButton(
                        onClick = {
                            coroutineScope.launch {
                                isRedeeming = true
                                val (success, message) = viewModel.activateTerminal(keyInput.trim())
                                validationMessage = message
                                isValidationSuccess = success
                                if (success) {
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                                isRedeeming = false
                            }
                        },
                        accent = true,
                        enabled = keyInput.isNotBlank() && !isRedeeming,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isRedeeming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.foreground,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VERIFYING WITH SERVER...",
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
                                text = "ACTIVATE TERMINAL",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
