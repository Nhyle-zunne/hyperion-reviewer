package com.nhyle.hyperionmaritime.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable helper function to open Facebook Messenger thread directly,
 * with graceful fallback to the mobile web link if the app is not installed.
 */
fun openMessengerSupport(context: Context) {
    val messengerThreadId = "1450729901448133"
    val webFallback = "https://m.me/$messengerThreadId"
    try {
        val appIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("fb-messenger://user-thread/$messengerThreadId")
        ).apply {
            setPackage("com.facebook.orca")
        }
        context.startActivity(appIntent)
    } catch (e: Exception) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(webFallback)
        )
        context.startActivity(webIntent)
    }
}

/**
 * LuminousDeskSupportButton:
 * High-visibility luminous glowing CTA with an animated cyan/emerald bloom aura
 * and shining linear gradient sweep effect for institutional verification and support.
 */
@Composable
fun LuminousDeskSupportButton(
    title: String = "OPEN SUPPORT DESK ↗",
    subtitle: String = "Direct candidate identity verification & seat clearance",
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clickAction = onClick ?: { openMessengerSupport(context) }
    val colors = com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors.current
    val isDark = colors.isDark

    val infiniteTransition = rememberInfiniteTransition(label = "luminousTransition")

    // 1. Pulsing ambient aura alpha (mint/cyan neon glow)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // 2. Continuous flowing shining sweep effect
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shape = RectangleShape

    val mintAura1 = if (isDark) Color(0xFF10B981) else Color(0xFF15803D)
    val cyanAura2 = if (isDark) Color(0xFF06D6A0) else Color(0xFF16A34A)
    val brightCyanHighlight = if (isDark) Color(0xFF00FFA3) else Color(0xFF15803D)
    val baseSurface = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val borderStrokeColor = if (isDark) Color(0xFF22C55E) else Color(0xFF15803D)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isDark) {
            // Exterior blurred ambient neon bloom behind the button in Dark Mode
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .blur(20.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                mintAura1.copy(alpha = glowAlpha * 0.55f),
                                cyanAura2.copy(alpha = glowAlpha * 0.70f),
                                brightCyanHighlight.copy(alpha = glowAlpha * 0.55f)
                            )
                        ),
                        shape = shape
                    )
            )
        }

        // Shining sweep border gradient brush
        val sweepBorderBrush = if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    mintAura1.copy(alpha = 0.6f),
                    brightCyanHighlight.copy(alpha = glowAlpha),
                    cyanAura2.copy(alpha = 0.7f),
                    brightCyanHighlight.copy(alpha = 0.9f),
                    mintAura1.copy(alpha = 0.6f)
                ),
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 400f, 200f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF15803D),
                    Color(0xFF16A34A),
                    Color(0xFF15803D)
                )
            )
        }

        // Container surface
        val containerSurfaceBrush = if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    baseSurface,
                    Color(0xFF14221C),
                    baseSurface
                ),
                start = Offset(shimmerOffset * 0.5f, 0f),
                end = Offset(shimmerOffset * 0.5f + 500f, 200f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF1F5F9),
                    Color(0xFFE2E8F0),
                    Color(0xFFF1F5F9)
                )
            )
        }

        // Button Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(containerSurfaceBrush)
                .border(width = 1.5.dp, brush = sweepBorderBrush, shape = shape)
                .clickable { clickAction() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RectangleShape)
                        .background(if (isDark) Color(0xFF064E3B).copy(alpha = 0.7f) else Color(0xFFE2E8F0))
                        .border(
                            width = 1.dp,
                            color = if (isDark) brightCyanHighlight.copy(alpha = glowAlpha) else Color(0xFF15803D),
                            shape = RectangleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = "Support Desk",
                        tint = if (isDark) brightCyanHighlight else Color(0xFF15803D),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF15803D),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Tag
            Surface(
                shape = RectangleShape,
                color = if (isDark) mintAura1.copy(alpha = 0.2f) else Color(0xFF15803D).copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) brightCyanHighlight.copy(alpha = 0.6f) else Color(0xFF15803D)
                )
            ) {
                Text(
                    text = "DISPATCH",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) brightCyanHighlight else Color(0xFF15803D),
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
