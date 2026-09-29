package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MaritimeTopBar
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.PassGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen

@Composable
fun HowToAvailScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val isOicNwProUnlocked by viewModel.isOicNwProUnlocked.collectAsState()
    val isGmdssProUnlocked by viewModel.isGmdssProUnlocked.collectAsState()

    BackHandler {
        viewModel.navigateBack()
    }

    val openFacebookAction = {
        val fbUrl = "https://www.facebook.com/hyperionbynhyle/"
        val intent = try {
            context.packageManager.getPackageInfo("com.facebook.katana", 0)
            Intent(Intent.ACTION_VIEW, Uri.parse("fb://facewebmodal/f?href=$fbUrl"))
        } catch (e: Exception) {
            Intent(Intent.ACTION_VIEW, Uri.parse(fbUrl))
        }
        context.startActivity(intent)
    }

    val screenBg = if (isDark) Color(0xFF0B132B) else Color(0xFFF1F5F9)
    val cardSurface = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val primaryTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .navigationBarsPadding()
    ) {
        MaritimeTopBar(
            title = "Device Registration & Support",
            showBackButton = true,
            onBackClick = { viewModel.navigateBack() },
            onMenuClick = { viewModel.openDrawer() },
            onLegalClick = { viewModel.navigateTo(Screen.TermsAndDisclaimer) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("device_support_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_support_hero_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF162032) else Color(0xFF1E293B)
                    ),
                    border = BorderStroke(1.5.dp, MaritimeGoldBright.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaritimeGoldBright.copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Help,
                                        contentDescription = "Device Registration & Support",
                                        tint = MaritimeGoldBright,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Device Registration & Support",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Serif
                                    ),
                                    color = PureWhite
                                )
                                Text(
                                    text = "Hyperion Maritime Examination Reviewer",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaritimeGoldBright
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Verify device access, request institutional verification, or contact support.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhite.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Supported Review Modules Section
            item {
                Text(
                    text = "SUPPORTED REVIEW MODULES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            // Module 1: OIC-NW Deck Operational Review Module
            item {
                ReviewModuleCard(
                    title = "OIC-NW Deck Operational Review Module",
                    subtitle = "Deck Operational Level Competencies",
                    highlights = listOf(
                        "Full access to all 3 Functions (12 Competencies) • 6,444 items",
                        "Global Search Bank, Intensive Mode, Question Randomizer, and Timed Official Mock Exams",
                        "100% offline access & verified device registration"
                    ),
                    icon = Icons.Default.Navigation,
                    accentColor = MaterialTheme.colorScheme.primary,
                    isUnlocked = isOicNwProUnlocked,
                    cardSurface = cardSurface,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor
                )
            }

            // Module 2: GMDSS Radio Operator Module
            item {
                ReviewModuleCard(
                    title = "GMDSS Radio Operator Module",
                    subtitle = "General Operator's Certificate Modules",
                    highlights = listOf(
                        "General Operator's Certificate modules • 2,260 items",
                        "Global Search Bank, Practice Drills, and Timed Official Mock Exams",
                        "100% offline access & verified device registration"
                    ),
                    icon = Icons.Default.Radio,
                    accentColor = SeaCyan,
                    isUnlocked = isGmdssProUnlocked,
                    cardSurface = cardSurface,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor
                )
            }

            // Module 3: Complete Hyperion Study Suite
            item {
                ReviewModuleCard(
                    title = "Complete Hyperion Study Suite",
                    subtitle = "Comprehensive Deck & Radio Operator Suite",
                    highlights = listOf(
                        "Full institutional access across all deck and radio operator competencies",
                        "Complete dual module repository (8,704 total question bank)",
                        "Full device verification and comprehensive offline review toolkit"
                    ),
                    icon = Icons.Default.WorkspacePremium,
                    accentColor = MaritimeGoldBright,
                    isUnlocked = isOicNwProUnlocked && isGmdssProUnlocked,
                    isFeatured = true,
                    cardSurface = cardSurface,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor
                )
            }

            // 3-Step Registration & Assistance Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "3-STEP INSTITUTIONAL REGISTRATION",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Step 1 Card (Clickable -> Navigates to ActivationScreen)
            item {
                InteractiveRegistrationStepCard(
                    stepNumber = 1,
                    title = "1. Open Device Registration",
                    description = "Navigate to the Device Registration tab to locate your unique device identifier.",
                    icon = Icons.Default.PhoneAndroid,
                    accentColor = MaritimeGoldBright,
                    badgeText = "Open tab",
                    actionIcon = Icons.AutoMirrored.Filled.ArrowForward,
                    cardSurface = cardSurface,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    testTag = "step_1_activation_card",
                    onClick = {
                        viewModel.navigateTo(Screen.Activation)
                    }
                )
            }

            // Step 2 Card (Clickable -> Launches Facebook)
            item {
                InteractiveRegistrationStepCard(
                    stepNumber = 2,
                    title = "2. Contact Help Desk",
                    description = "For student enrollment, institutional access, or technical verification, contact our support team via Facebook.",
                    icon = Icons.Default.Send,
                    accentColor = SeaCyan,
                    badgeText = "Contact desk",
                    actionIcon = Icons.AutoMirrored.Filled.OpenInNew,
                    cardSurface = cardSurface,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    testTag = "step_2_facebook_card",
                    onClick = openFacebookAction
                )
            }

            // Step 3 Card (Informational)
            item {
                StaticRegistrationStepCard(
                    stepNumber = 3,
                    title = "3. Apply Verification Key",
                    description = "Enter your issued verification code to finalize offline access on this device.",
                    icon = Icons.Default.Key,
                    accentColor = PassGreen,
                    cardSurface = cardSurface,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    testTag = "step_3_unlock_card"
                )
            }

            // Glowing Action Button
            item {
                Spacer(modifier = Modifier.height(6.dp))
                AnimatedGlowingSupportButton(
                    onClick = openFacebookAction,
                    modifier = Modifier.testTag("contact_facebook_button")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InteractiveRegistrationStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String,
    actionIcon: ImageVector,
    cardSurface: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.2.dp, accentColor),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stepNumber.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = primaryTextColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 48.dp)
            )
        }
    }
}

@Composable
private fun StaticRegistrationStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    cardSurface: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, accentColor),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = primaryTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 48.dp)
            )
        }
    }
}

@Composable
private fun AnimatedGlowingSupportButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "support_glow_pulse")
    
    // Animate glowing stroke / translation
    val offsetProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_shimmer"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF0052D4),
            Color(0xFF1877F2),
            Color(0xFF00D2FF),
            Color(0xFF1877F2),
            Color(0xFF0052D4)
        ),
        startX = offsetProgress,
        endX = offsetProgress + 600f
    )

    val outerGlowColor = Color(0xFF00D2FF).copy(alpha = glowAlpha)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color(0xFF1877F2),
                spotColor = Color(0xFF00D2FF)
            )
            .drawBehind {
                drawRoundRect(
                    color = outerGlowColor,
                    style = Stroke(width = 3.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(14.dp))
            .background(buttonGradient)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Contact Support",
                tint = PureWhite,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Contact Support on Facebook",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.2.sp
                ),
                color = PureWhite
            )
        }
    }
}

@Composable
private fun ReviewModuleCard(
    title: String,
    subtitle: String,
    highlights: List<String>,
    icon: ImageVector,
    accentColor: Color,
    isUnlocked: Boolean,
    cardSurface: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    isFeatured: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_module_${title.take(6).lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardSurface
        ),
        border = if (isFeatured) {
            BorderStroke(1.5.dp, MaritimeGoldBright)
        } else if (isUnlocked) {
            BorderStroke(1.dp, PassGreen)
        } else {
            BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFeatured) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = primaryTextColor
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor
                        )
                    }
                }

                if (isUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PassGreen.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, PassGreen)
                    ) {
                        Text(
                            text = "ACTIVE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PassGreen,
                            fontSize = 10.sp
                        )
                    }
                } else if (isFeatured) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaritimeGoldBright.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, MaritimeGoldBright)
                    ) {
                        Text(
                            text = "FULL ACCESS",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaritimeGoldBright,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            highlights.forEach { highlight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = highlight,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
