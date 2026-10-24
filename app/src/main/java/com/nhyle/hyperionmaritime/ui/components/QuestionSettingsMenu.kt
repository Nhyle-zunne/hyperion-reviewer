package com.nhyle.hyperionmaritime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import kotlin.math.roundToInt

@Composable
fun QuestionSettingsMenu(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier,
    showDrillAutomations: Boolean = false,
    showOnlyTextSize: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val textScale by viewModel.questionTextScale.collectAsState()
    val isDirectAnswer by viewModel.isDirectAnswer.collectAsState()
    val isAutoNextCorrect by viewModel.isAutoNextCorrect.collectAsState()
    val isAutoNextWrong by viewModel.isAutoNextWrong.collectAsState()
    val colors = LocalSpaghettiColors.current

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("question_settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = colors.foreground
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(colors.surface)
                .border(1.dp, colors.border, RectangleShape)
                .widthIn(min = 260.dp, max = 300.dp)
        ) {
            // Header
            Text(
                text = if (showOnlyTextSize) "TEXT SIZE SETTINGS" else "DISPLAY SETTINGS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.muted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )

            HorizontalDivider(color = colors.border, thickness = 0.5.dp)

            if (!showOnlyTextSize) {
                // 1. Light / Dark Mode Toggle
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isDarkMode) "Night Ops" else "Daylight",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.foreground
                                )
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.toggleDarkMode(it) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    },
                    onClick = { viewModel.toggleDarkMode(!isDarkMode) }
                )

                HorizontalDivider(color = colors.border, thickness = 0.5.dp)
            }

            // 2. Text Size Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Text Size",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.foreground
                        )
                    }
                    Text(
                        text = "${(textScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decrease font size
                    OutlinedButton(
                        onClick = { viewModel.decreaseQuestionTextScale() },
                        enabled = textScale > 0.86f,
                        shape = RectangleShape,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("A-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }

                    // Reset to 100%
                    OutlinedButton(
                        onClick = { viewModel.setQuestionTextScale(1.0f) },
                        shape = RectangleShape,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("100%", style = MaterialTheme.typography.labelSmall)
                    }

                    // Increase font size
                    OutlinedButton(
                        onClick = { viewModel.increaseQuestionTextScale() },
                        enabled = textScale < 1.34f,
                        shape = RectangleShape,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("A+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Drill & Practice Automations Section
            if (showDrillAutomations) {
                HorizontalDivider(color = colors.border, thickness = 0.5.dp)

                Text(
                    text = "PRACTICE AUTOMATIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.muted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )

                HorizontalDivider(color = colors.border, thickness = 0.5.dp)

                // 1. Direct Answer Toggle
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Direct Answer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.foreground
                                )
                                Text(
                                    text = "Confirm without Check button",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.muted
                                )
                            }
                            Switch(
                                checked = isDirectAnswer,
                                onCheckedChange = { viewModel.toggleDirectAnswer(it) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    },
                    onClick = { viewModel.toggleDirectAnswer(!isDirectAnswer) }
                )

                // 2. Auto-Next on Correct
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Auto-Next (Correct)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.foreground
                                )
                                Text(
                                    text = "Advance when correct",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.muted
                                )
                            }
                            Switch(
                                checked = isAutoNextCorrect,
                                onCheckedChange = { viewModel.toggleAutoNextCorrect(it) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    },
                    onClick = { viewModel.toggleAutoNextCorrect(!isAutoNextCorrect) }
                )

                // 3. Auto-Next on Wrong
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Auto-Next (Wrong)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.foreground
                                )
                                Text(
                                    text = "Advance when incorrect",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.muted
                                )
                            }
                            Switch(
                                checked = isAutoNextWrong,
                                onCheckedChange = { viewModel.toggleAutoNextWrong(it) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    },
                    onClick = { viewModel.toggleAutoNextWrong(!isAutoNextWrong) }
                )
            }
        }
    }
}