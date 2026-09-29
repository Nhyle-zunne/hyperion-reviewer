package com.example.ui.screens

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
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.LicenseManager
import com.example.data.util.ProTier
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.MianButton
import com.example.ui.components.MianRuleCard
import com.example.ui.components.MianSectionHeader
import com.example.ui.theme.LocalSpaghettiColors
import com.example.viewmodel.ReviewerViewModel
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
    val isHyperionProUnlocked by viewModel.isHyperionProUnlocked.collectAsState()

    var selectedTier by remember { mutableStateOf(ProTier.HYPERION_PRO) }
    var keyInput by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var isValidationSuccess by remember { mutableStateOf(false) }

    val requestCode = remember(selectedTier) {
        LicenseManager.getRequestCodeForTier(context, selectedTier)
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
            title = "DEVICE REGISTRATION & LICENSE",
            subtitle = "TERMINAL SECURITY & ACTIVATION",
            showBackButton = true,
            onBackClick = { viewModel.navigateBack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card
            MianRuleCard(
                ruleColor = colors.accent,
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "TERMINAL LICENSE MANAGER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hyperion licenses are cryptographically verified against your physical device hardware token.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            }

            // Tier Selector
            MianSectionHeader(title = "SELECT LICENSE TIER")

            ProTier.values().forEach { tier ->
                val isTierSelected = selectedTier == tier
                val isTierUnlocked = when (tier) {
                    ProTier.OIC_NW_PRO -> isOicNwProUnlocked
                    ProTier.GMDSS_PRO -> isGmdssProUnlocked
                    ProTier.HYPERION_PRO -> isHyperionProUnlocked
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
                        verticalAlignment = Alignment.CenterVertically
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
                        }

                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = colors.surface2
                        ) {
                            Text(
                                text = if (isTierUnlocked) "VERIFIED" else "LOCKED",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isTierUnlocked) colors.success else colors.muted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Institutional Verification Desk Button
            com.example.ui.components.LuminousDeskSupportButton(
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
                    Text(
                        text = "TERMINAL REQUEST CODE (${selectedTier.shortName}):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.muted,
                        fontSize = 10.sp
                    )

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
                                    clipboardManager.setText(AnnotatedString(requestCode))
                                    Toast.makeText(context, "Request Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(text = "COPY", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }
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
                        onValueChange = { keyInput = it.uppercase() },
                        placeholder = {
                            Text(
                                text = "XXXX - XXXX - XXXX - XXXX",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
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
                                        delay(150)
                                        bringIntoViewRequester.bringIntoView()
                                        scrollState.animateScrollTo(scrollState.maxValue)
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

                    MianButton(
                        onClick = {
                            val result = viewModel.activateWithKey(keyInput.trim(), selectedTier)
                            validationMessage = result.toastMessage
                            isValidationSuccess = result.isSuccess
                            if (result.isSuccess) {
                                Toast.makeText(context, result.toastMessage, Toast.LENGTH_LONG).show()
                            }
                        },
                        accent = true,
                        enabled = keyInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
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

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
