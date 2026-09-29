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
import com.example.ui.components.MaritimeTopBar
import com.example.ui.components.MianButton
import com.example.ui.components.MianRuleCard
import com.example.ui.components.MianSectionHeader
import com.example.ui.theme.LocalSpaghettiColors
import com.example.viewmodel.ReviewerViewModel
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

    val requestCode = remember {
        LicenseManager.getOrGenerateRequestCode(context)
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
            // Status Card
            MianRuleCard(
                ruleColor = if (isActivated) colors.success else colors.accent,
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

                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = colors.surface2
                        ) {
                            Text(
                                text = if (isActivated) "VERIFIED" else "VERIFICATION REQUIRED",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isActivated) colors.success else colors.accent,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isActivated) {
                            "Full question bank and timed mock examination access active on this terminal."
                        } else {
                            "Please activate your terminal license to unlock all question banks and unlimited timed mock exams."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                }
            }

            com.example.ui.components.LuminousDeskSupportButton(
                title = "CONTACT VERIFICATION DESK ↗",
                subtitle = "Request institutional access or candidate clearance"
            )

            MianSectionHeader(title = "YOUR TERMINAL REQUEST CODE")

            MianRuleCard(
                ruleColor = colors.border,
                containerColor = colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DEVICE REQUEST CODE:",
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
                                    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("COPY", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }
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
                            val result = viewModel.activateWithKey(keyInput.trim())
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
                            text = "VERIFY & UNLOCK",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
