package com.nhyle.hyperionmaritime.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.ui.motion.HyperionDialog
import com.nhyle.hyperionmaritime.viewmodel.DeviceRegistrationViewModel

@Composable
fun DeviceRegistrationScreen(
    viewModel: DeviceRegistrationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.autoVerifyOnLaunch(context)
    }

    if (uiState.isVerifyingOnLaunch) {
        HyperionDialog(onDismissRequest = { /* Non-dismissible while verifying */ }) {
            Card(
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1321)),
                border = BorderStroke(1.dp, Color(0xFF00E599).copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF00E599),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AUTHENTICATING LICENSE CLEARANCE",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Querying cryptographic server registry...",
                        color = Color(0xFF8E9BAE),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Determine theme brightness
    val colors = com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors.current
    val isDark = colors.isDark

    // Adaptive Theme Palette matching the cards below
    val cardBackground = MaterialTheme.colorScheme.surface
    val primaryTextColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    val statusAccentColor = if (uiState.isLicensed) {
        if (isDark) Color(0xFF00E676) else Color(0xFF15803D)
    } else {
        if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
    }

    val buttonBackground = if (isDark) Color(0xFF0F141C) else Color(0xFFF1F5F9)
    val buttonBorderColor = if (isDark) Color(0xFF26354A) else Color(0xFFCBD5E1)
    val buttonTextColor = if (isDark) Color(0xFFF0F6FC) else Color(0xFF0F172A)

    Column(modifier = modifier.fillMaxWidth()) {
        // ============================================================================
        // ADAPTIVE TOP TERMINAL STATUS CARD
        // ============================================================================
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardBackground // Matches TERMINAL LICENSE MANAGER
            ),
            border = BorderStroke(
                width = 1.dp,
                color = statusAccentColor.copy(alpha = if (isDark) 0.8f else 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            color = secondaryTextColor,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = when {
                                uiState.isLicensed && uiState.licenseType == "OFFLINE_PASS" -> "ACTIVE (OFFLINE PASS)"
                                uiState.isLicensed && uiState.licenseType == "SUBSCRIPTION" -> "ACTIVE (SUBSCRIPTION)"
                                uiState.isLicensed && uiState.licenseType == "PROMO_PASS" -> "ACTIVE (PROMO PASS)"
                                uiState.isLicensed -> "ACTIVE (LICENSED)"
                                else -> "RESTRICTED (UNLICENSED)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = statusAccentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Status Pill Badge (UNVERIFIED / VERIFIED)
                    Surface(
                        color = statusAccentColor.copy(alpha = if (isDark) 0.15f else 0.10f),
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, statusAccentColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (uiState.isLicensed) "VERIFIED" else "UNVERIFIED",
                            color = statusAccentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Adaptive Registry Sync Button
                OutlinedButton(
                    onClick = { viewModel.verifyLicenseState(context) },
                    enabled = !uiState.isVerifyingLicense,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, buttonBorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = buttonBackground,
                        contentColor = buttonTextColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    if (uiState.isVerifyingLicense) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = statusAccentColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYNCING WITH REGISTRY...",
                            style = MaterialTheme.typography.labelMedium,
                            color = secondaryTextColor,
                            letterSpacing = 1.sp
                        )
                    } else {
                        Text(
                            text = "VERIFY REGISTRY STATUS",
                            style = MaterialTheme.typography.labelMedium,
                            color = buttonTextColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // ============================================================================
        // SELECT LICENSE TIER CARD
        // ============================================================================
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            border = BorderStroke(1.dp, buttonBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SELECT LICENSE TIER",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryTextColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // OIC-NW DECK CARD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OIC-NW DECK (OPERATIONAL LEVEL)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "STCW Regulation II/1 • 12 Competencies",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor
                        )
                    }
                    val oicBadgeColor = if (uiState.isOicLicensed) Color(0xFF00E599) else Color(0xFF8E9BAE)
                    Surface(
                        color = if (uiState.isOicLicensed) Color(0xFF00E599).copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (uiState.isOicLicensed) Color(0xFF00E599) else (if (isDark) Color(0xFF1E2D4A) else Color(0xFFCBD5E1))
                        )
                    ) {
                        Text(
                            text = if (uiState.isOicLicensed) "VERIFIED" else "UNVERIFIED",
                            color = oicBadgeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // GMDSS GOC CARD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GMDSS GOC (RADIO OPERATOR)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "STCW Regulation IV/2 • 2 Competencies",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor
                        )
                    }
                    val gmdssBadgeColor = if (uiState.isGmdssLicensed) Color(0xFF00E599) else Color(0xFF8E9BAE)
                    Surface(
                        color = if (uiState.isGmdssLicensed) Color(0xFF00E599).copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (uiState.isGmdssLicensed) Color(0xFF00E599) else (if (isDark) Color(0xFF1E2D4A) else Color(0xFFCBD5E1))
                        )
                    ) {
                        Text(
                            text = if (uiState.isGmdssLicensed) "VERIFIED" else "UNVERIFIED",
                            color = gmdssBadgeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // HYPERION ALL ACCESS CARD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HYPERION ALL ACCESS",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Complete Deck Operational + GMDSS Radio Reviewer package",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor
                        )
                    }
                    val allAccessBadgeColor = if (uiState.isAllAccessLicensed) Color(0xFF00E599) else Color(0xFF8E9BAE)
                    Surface(
                        color = if (uiState.isAllAccessLicensed) Color(0xFF00E599).copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (uiState.isAllAccessLicensed) Color(0xFF00E599) else (if (isDark) Color(0xFF1E2D4A) else Color(0xFFCBD5E1))
                        )
                    ) {
                        Text(
                            text = if (uiState.isAllAccessLicensed) "VERIFIED" else "UNVERIFIED",
                            color = allAccessBadgeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
