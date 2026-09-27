package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MaritimeGold
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.NauticalNavyDark
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaCyan
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom vector-based nautical logo component in pure Jetpack Compose.
 * Motif: A stylized mariner's compass rose integrated with a golden sextant arc
 * and modern geometric helm wheel, accented with navy blue and gold trim.
 */
@Composable
fun HyperionEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A),
                        NauticalNavyDark,
                        Color(0xFF030712)
                    )
                )
            )
            .border(2.dp, MaritimeGoldBright, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.9f)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.width / 2f

            // 1. Draw outer helm wheel rim
            drawCircle(
                color = MaritimeGold.copy(alpha = 0.4f),
                radius = radius * 0.92f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 2. Draw Helm wheel spoke handles (8 handles around circumference)
            val spokeCount = 8
            for (i in 0 until spokeCount) {
                val angle = (i * (360f / spokeCount)) * (Math.PI.toFloat() / 180f)
                val spokeStart = Offset(
                    center.x + (radius * 0.72f) * cos(angle),
                    center.y + (radius * 0.72f) * sin(angle)
                )
                val spokeEnd = Offset(
                    center.x + (radius * 0.98f) * cos(angle),
                    center.y + (radius * 0.98f) * sin(angle)
                )
                drawLine(
                    color = MaritimeGoldBright,
                    start = spokeStart,
                    end = spokeEnd,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Spoke knob
                drawCircle(
                    color = MaritimeGoldBright,
                    radius = 2.dp.toPx(),
                    center = spokeEnd
                )
            }

            // 3. Draw Sextant graduated index arc (subtle arc in lower quadrant)
            drawArc(
                color = SeaCyan.copy(alpha = 0.7f),
                startAngle = 110f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.65f, center.y - radius * 0.65f),
                size = Size(radius * 1.3f, radius * 1.3f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Graduated tic marks on sextant arc
            for (deg in 120..240 step 15) {
                val rad = deg * (Math.PI.toFloat() / 180f)
                val inner = Offset(
                    center.x + (radius * 0.61f) * cos(rad),
                    center.y + (radius * 0.61f) * sin(rad)
                )
                val outer = Offset(
                    center.x + (radius * 0.69f) * cos(rad),
                    center.y + (radius * 0.69f) * sin(rad)
                )
                drawLine(
                    color = MaritimeGoldBright,
                    start = inner,
                    end = outer,
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 4. Stylized 8-Point Compass Rose
            // Secondary points (NE, SE, SW, NW)
            for (angle in listOf(45f, 135f, 225f, 315f)) {
                rotate(angle, pivot = center) {
                    drawCompassPoint(
                        center = center,
                        length = radius * 0.52f,
                        width = radius * 0.16f,
                        primaryColor = SeaCyan.copy(alpha = 0.85f),
                        secondaryColor = SeaCyan.copy(alpha = 0.4f)
                    )
                }
            }

            // Primary cardinal points (N, E, S, W) - larger, prominent gold
            for (angle in listOf(0f, 90f, 180f, 270f)) {
                rotate(angle, pivot = center) {
                    val isNorth = angle == 0f
                    drawCompassPoint(
                        center = center,
                        length = radius * 0.75f,
                        width = radius * 0.22f,
                        primaryColor = if (isNorth) MaritimeGoldBright else MaritimeGold,
                        secondaryColor = if (isNorth) Color(0xFFB45309) else Color(0xFF78350F)
                    )
                }
            }

            // 5. Central Hub with Golden Sextant Pivot & Compass Bezel
            drawCircle(
                color = NauticalNavyDark,
                radius = radius * 0.22f,
                center = center
            )
            drawCircle(
                color = MaritimeGoldBright,
                radius = radius * 0.22f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = MaritimeGoldBright,
                radius = radius * 0.10f,
                center = center
            )
        }
    }
}

private fun DrawScope.drawCompassPoint(
    center: Offset,
    length: Float,
    width: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    // Left half (light / highlighted)
    val leftPath = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x, center.y - length)
        lineTo(center.x - width / 2f, center.y)
        close()
    }
    drawPath(leftPath, color = primaryColor)

    // Right half (shadowed facet)
    val rightPath = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x, center.y - length)
        lineTo(center.x + width / 2f, center.y)
        close()
    }
    drawPath(rightPath, color = secondaryColor)
}

/**
 * Full Hyperion Nautical Logo with Typography.
 * Bold, modern serif/sans-serif title "HYPERION" with subtitle "MARINA Licensure Reviewer".
 */
@Composable
fun HyperionLogo(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 64.dp,
    showSubtitle: Boolean = true,
    isCenterAligned: Boolean = true
) {
    if (isCenterAligned) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HyperionEmblem(size = emblemSize)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "HYPERION",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    fontFamily = FontFamily.Serif
                ),
                color = MaritimeGoldBright
            )
            if (showSubtitle) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "MARITIME LICENSURE REVIEWER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    ),
                    color = PureWhite.copy(alpha = 0.85f)
                )
                Text(
                    text = "Aligned with MARINA & STCW Competency Standards",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.5.sp
                    ),
                    color = SeaCyan.copy(alpha = 0.85f)
                )
            }
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HyperionEmblem(size = emblemSize)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "HYPERION",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.Serif
                    ),
                    color = MaritimeGoldBright
                )
                if (showSubtitle) {
                    Text(
                        text = "Maritime Licensure Reviewer",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = PureWhite.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
