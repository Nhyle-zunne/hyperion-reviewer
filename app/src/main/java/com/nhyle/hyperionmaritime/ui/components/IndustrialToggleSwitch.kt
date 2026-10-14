package com.nhyle.hyperionmaritime.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom Skeuomorphic Industrial Toggle Switch:
 * - 76dp width, 36dp height, strict RectangleShape (0.dp radius)
 * - Track: #5DB32B when ON (checked) with "ON" text; #8E8E93 when OFF (!checked) with "OFF" text
 * - Thumb: 34dp x 30dp sharp rectangular slider with white-to-offwhite vertical gradient
 */
@Composable
fun IndustrialToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val trackColor = if (checked) Color(0xFF5DB32B) else Color(0xFF8E8E93)
    val borderColor = if (checked) Color(0xFF478E20) else Color(0xFF6E6E73)

    // Smooth horizontal translation between start (3.dp) and end (39.dp)
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 39.dp else 3.dp,
        animationSpec = tween(durationMillis = 180),
        label = "industrial_switch_thumb_offset"
    )

    Box(
        modifier = modifier
            .size(width = 76.dp, height = 36.dp)
            .shadow(elevation = 1.dp, shape = RectangleShape)
            .clip(RectangleShape)
            .background(trackColor)
            .border(width = 1.dp, color = borderColor, shape = RectangleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Track Status Labels (ON on left when checked, OFF on right when unchecked)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentAlignment = if (checked) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            if (checked) {
                Text(
                    text = "ON",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            } else {
                Text(
                    text = "OFF",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(end = 2.dp)
                )
            }
        }

        // Skeuomorphic Raised Rectangular Slider Thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(width = 34.dp, height = 30.dp)
                .shadow(elevation = 3.dp, shape = RectangleShape)
                .clip(RectangleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFE2E8F0)
                        )
                    )
                )
                .border(width = 1.dp, color = Color(0xFFD1D5DB), shape = RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            // Mechanical grip ridges on thumb
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(12.dp)
                        .background(Color(0xFFCBD5E1), shape = RectangleShape)
                )
                Spacer(modifier = Modifier.width(2.5.dp))
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(12.dp)
                        .background(Color(0xFFCBD5E1), shape = RectangleShape)
                )
                Spacer(modifier = Modifier.width(2.5.dp))
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(12.dp)
                        .background(Color(0xFFCBD5E1), shape = RectangleShape)
                )
            }
        }
    }
}
