package com.example.ui.components

import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
 * Official HYPERION Medallion Seal Emblem.
 * Faithfully renders the official embossed gold maritime medal:
 * - Outer metallic gold circular band with engraved "HYPERION", "By HyperionNavTech", "W", and "E"
 * - Deep nautical navy inner dial with concentric gold navigation rings
 * - Radiant 8-pointed golden compass star at 12 o'clock
 * - Mariner anchor passing through diagonally with stock and ring at top-right
 * - Center open navigation book with sextant (left page) and compass rose (right page)
 * - Anchor flukes cradling the book with "S" mark on the crown
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
                        Color(0xFFEAB308),
                        Color(0xFFD4AF37),
                        Color(0xFF92400E)
                    )
                )
            )
            .border(2.dp, Color(0xFFFDE047), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.width / 2f

            // 1. Outer Gold Bevel Rim
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFEF08A), Color(0xFFD4AF37), Color(0xFF78350F)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Outer Thin Groove
            drawCircle(
                color = Color(0xFF78350F),
                radius = radius * 0.98f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Inner Boundary Ring of Gold Band
            val innerRingRadius = radius * 0.72f
            drawCircle(
                color = Color(0xFF78350F),
                radius = innerRingRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 2. Inner Deep Maritime Navy Dial
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0F1E36), Color(0xFF070F20), Color(0xFF030712)),
                    center = center,
                    radius = innerRingRadius
                ),
                radius = innerRingRadius,
                center = center
            )

            // Concentric Gold Navigation Ring inside Dial
            drawCircle(
                color = Color(0xFFD4AF37).copy(alpha = 0.5f),
                radius = innerRingRadius * 0.92f,
                center = center,
                style = Stroke(width = 0.75.dp.toPx())
            )

            // 3. Arched Text on the Gold Ring: "HYPERION", "By HyperionNavTech", "W", "E"
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                val textRadius = radius * 0.84f

                // Top Arc for "HYPERION"
                val topPath = AndroidPath().apply {
                    addArc(
                        center.x - textRadius,
                        center.y - textRadius,
                        center.x + textRadius,
                        center.y + textRadius,
                        195f,
                        150f
                    )
                }

                val titlePaint = Paint().apply {
                    color = Color(0xFF0A0F1D).toArgb()
                    textSize = (radius * 0.22f).coerceAtLeast(10f)
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                native.drawTextOnPath("HYPERION", topPath, 0f, 0f, titlePaint)

                // Bottom Arc for "By HyperionNavTech"
                val bottomPath = AndroidPath().apply {
                    addArc(
                        center.x - textRadius,
                        center.y - textRadius,
                        center.x + textRadius,
                        center.y + textRadius,
                        15f,
                        150f
                    )
                }

                val subPaint = Paint().apply {
                    color = Color(0xFF0A0F1D).toArgb()
                    textSize = (radius * 0.12f).coerceAtLeast(7f)
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                native.drawTextOnPath("By HyperionNavTech", bottomPath, 0f, (radius * 0.08f), subPaint)

                // West "W" and East "E"
                val cardinalPaint = Paint().apply {
                    color = Color(0xFF0A0F1D).toArgb()
                    textSize = (radius * 0.16f).coerceAtLeast(8f)
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                native.drawText("W", center.x - radius * 0.85f, center.y + (cardinalPaint.textSize / 3f), cardinalPaint)
                native.drawText("E", center.x + radius * 0.85f, center.y + (cardinalPaint.textSize / 3f), cardinalPaint)
            }

            // 4. Horizontal Compass Pointer Needles (from book pointing to W and E)
            val pointerLen = innerRingRadius * 0.88f
            val pointerWidth = innerRingRadius * 0.12f
            // Left pointer
            val westPointer = Path().apply {
                moveTo(center.x - pointerLen, center.y)
                lineTo(center.x - innerRingRadius * 0.45f, center.y - pointerWidth)
                lineTo(center.x - innerRingRadius * 0.45f, center.y + pointerWidth)
                close()
            }
            drawPath(westPointer, Color(0xFFD4AF37))
            drawPath(westPointer, Color(0xFF92400E), style = Stroke(0.5.dp.toPx()))

            // Right pointer
            val eastPointer = Path().apply {
                moveTo(center.x + pointerLen, center.y)
                lineTo(center.x + innerRingRadius * 0.45f, center.y - pointerWidth)
                lineTo(center.x + innerRingRadius * 0.45f, center.y + pointerWidth)
                close()
            }
            drawPath(eastPointer, Color(0xFFD4AF37))
            drawPath(eastPointer, Color(0xFF92400E), style = Stroke(0.5.dp.toPx()))

            // 5. Anchor Diagonal Stock, Shackle, and Shank (behind book at top-right)
            val anchorShackleCenter = Offset(center.x + innerRingRadius * 0.46f, center.y - innerRingRadius * 0.48f)
            drawCircle(
                color = Color(0xFFFDE047),
                radius = innerRingRadius * 0.12f,
                center = anchorShackleCenter,
                style = Stroke(width = 2.dp.toPx())
            )
            // Stock crossbar
            drawLine(
                color = Color(0xFFD4AF37),
                start = Offset(anchorShackleCenter.x - innerRingRadius * 0.18f, anchorShackleCenter.y - innerRingRadius * 0.08f),
                end = Offset(anchorShackleCenter.x + innerRingRadius * 0.14f, anchorShackleCenter.y + innerRingRadius * 0.18f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Shank running down into center
            drawLine(
                color = Color(0xFFF59E0B),
                start = anchorShackleCenter,
                end = Offset(center.x + innerRingRadius * 0.1f, center.y - innerRingRadius * 0.1f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 6. Radiant 8-Point Compass Star at 12 o'clock
            val starCenter = Offset(center.x, center.y - innerRingRadius * 0.48f)
            val starRadius = innerRingRadius * 0.38f

            // North ray (longest)
            drawStarRay(starCenter, 0f, starRadius, starRadius * 0.28f, Color(0xFFFEF08A), Color(0xFFB45309))
            // South ray
            drawStarRay(starCenter, 180f, starRadius * 0.7f, starRadius * 0.24f, Color(0xFFF59E0B), Color(0xFFD97706))
            // East & West rays
            drawStarRay(starCenter, 90f, starRadius * 0.65f, starRadius * 0.24f, Color(0xFFFDE047), Color(0xFFB45309))
            drawStarRay(starCenter, 270f, starRadius * 0.65f, starRadius * 0.24f, Color(0xFFB45309), Color(0xFFFEF08A))
            // 4 Diagonals
            for (ang in listOf(45f, 135f, 225f, 315f)) {
                drawStarRay(starCenter, ang, starRadius * 0.45f, starRadius * 0.18f, Color(0xFFFBBF24), Color(0xFF78350F))
            }

            // 7. Center Open Navigation Book
            val bookWidth = innerRingRadius * 0.90f
            val bookHeight = innerRingRadius * 0.48f
            val bookTop = center.y - innerRingRadius * 0.16f

            // Book pages background
            val bookPath = Path().apply {
                moveTo(center.x, bookTop)
                // Left page curve
                cubicTo(
                    center.x - bookWidth * 0.25f, bookTop - bookHeight * 0.1f,
                    center.x - bookWidth * 0.45f, bookTop - bookHeight * 0.05f,
                    center.x - bookWidth * 0.5f, bookTop + bookHeight * 0.05f
                )
                lineTo(center.x - bookWidth * 0.5f, bookTop + bookHeight)
                cubicTo(
                    center.x - bookWidth * 0.45f, bookTop + bookHeight * 0.9f,
                    center.x - bookWidth * 0.25f, bookTop + bookHeight * 0.9f,
                    center.x, bookTop + bookHeight
                )
                // Right page curve
                cubicTo(
                    center.x + bookWidth * 0.25f, bookTop + bookHeight * 0.9f,
                    center.x + bookWidth * 0.45f, bookTop + bookHeight * 0.9f,
                    center.x + bookWidth * 0.5f, bookTop + bookHeight
                )
                lineTo(center.x + bookWidth * 0.5f, bookTop + bookHeight * 0.05f)
                cubicTo(
                    center.x + bookWidth * 0.45f, bookTop - bookHeight * 0.05f,
                    center.x + bookWidth * 0.25f, bookTop - bookHeight * 0.1f,
                    center.x, bookTop
                )
                close()
            }
            drawPath(
                path = bookPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFEF9C3), Color(0xFFFDE68A), Color(0xFFFEF9C3))
                )
            )
            drawPath(bookPath, Color(0xFF78350F), style = Stroke(1.dp.toPx()))

            // Book central spine division
            drawLine(
                color = Color(0xFF78350F),
                start = Offset(center.x, bookTop),
                end = Offset(center.x, bookTop + bookHeight),
                strokeWidth = 1.5.dp.toPx()
            )

            // Left Page: Illustrated Sextant
            val sextantCenter = Offset(center.x - bookWidth * 0.25f, bookTop + bookHeight * 0.5f)
            val sextantR = bookHeight * 0.28f
            drawArc(
                color = Color(0xFF78350F),
                startAngle = 120f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(sextantCenter.x - sextantR, sextantCenter.y - sextantR),
                size = Size(sextantR * 2, sextantR * 2),
                style = Stroke(1.dp.toPx())
            )
            drawLine(
                color = Color(0xFF78350F),
                start = Offset(sextantCenter.x, sextantCenter.y - sextantR * 0.8f),
                end = Offset(sextantCenter.x - sextantR * 0.5f, sextantCenter.y + sextantR * 0.7f),
                strokeWidth = 0.8.dp.toPx()
            )
            drawLine(
                color = Color(0xFF78350F),
                start = Offset(sextantCenter.x, sextantCenter.y - sextantR * 0.8f),
                end = Offset(sextantCenter.x + sextantR * 0.5f, sextantCenter.y + sextantR * 0.7f),
                strokeWidth = 0.8.dp.toPx()
            )
            drawLine(
                color = Color(0xFF78350F),
                start = Offset(sextantCenter.x, sextantCenter.y - sextantR * 0.8f),
                end = Offset(sextantCenter.x, sextantCenter.y + sextantR * 0.8f),
                strokeWidth = 1.2.dp.toPx()
            )

            // Right Page: Navigational Compass Rose
            val compassCenter = Offset(center.x + bookWidth * 0.25f, bookTop + bookHeight * 0.5f)
            val compassR = bookHeight * 0.32f
            drawCircle(
                color = Color(0xFF78350F),
                radius = compassR,
                center = compassCenter,
                style = Stroke(0.8.dp.toPx())
            )
            drawLine(
                color = Color(0xFF78350F),
                start = Offset(compassCenter.x, compassCenter.y - compassR),
                end = Offset(compassCenter.x, compassCenter.y + compassR),
                strokeWidth = 0.75.dp.toPx()
            )
            drawLine(
                color = Color(0xFF78350F),
                start = Offset(compassCenter.x - compassR, compassCenter.y),
                end = Offset(compassCenter.x + compassR, compassCenter.y),
                strokeWidth = 0.75.dp.toPx()
            )

            // 8. Anchor Crown & Flukes cradling the book at the bottom
            val crownY = center.y + innerRingRadius * 0.58f
            // Fluke curve
            val flukePath = Path().apply {
                moveTo(center.x - innerRingRadius * 0.62f, center.y + innerRingRadius * 0.16f)
                cubicTo(
                    center.x - innerRingRadius * 0.55f, crownY + innerRingRadius * 0.22f,
                    center.x + innerRingRadius * 0.55f, crownY + innerRingRadius * 0.22f,
                    center.x + innerRingRadius * 0.62f, center.y + innerRingRadius * 0.16f
                )
            }
            drawPath(flukePath, Color(0xFFF59E0B), style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round))
            drawPath(flukePath, Color(0xFFFEF08A), style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))

            // Fluke Arrowheads
            val leftArrow = Path().apply {
                moveTo(center.x - innerRingRadius * 0.66f, center.y + innerRingRadius * 0.13f)
                lineTo(center.x - innerRingRadius * 0.56f, center.y + innerRingRadius * 0.10f)
                lineTo(center.x - innerRingRadius * 0.60f, center.y + innerRingRadius * 0.22f)
                close()
            }
            drawPath(leftArrow, Color(0xFFFDE047))
            drawPath(leftArrow, Color(0xFF92400E), style = Stroke(0.5.dp.toPx()))

            val rightArrow = Path().apply {
                moveTo(center.x + innerRingRadius * 0.66f, center.y + innerRingRadius * 0.13f)
                lineTo(center.x + innerRingRadius * 0.56f, center.y + innerRingRadius * 0.10f)
                lineTo(center.x + innerRingRadius * 0.60f, center.y + innerRingRadius * 0.22f)
                close()
            }
            drawPath(rightArrow, Color(0xFFFDE047))
            drawPath(rightArrow, Color(0xFF92400E), style = Stroke(0.5.dp.toPx()))

            // Anchor Crown Point at bottom
            val crownPoint = Path().apply {
                moveTo(center.x, crownY + innerRingRadius * 0.24f)
                lineTo(center.x - innerRingRadius * 0.08f, crownY + innerRingRadius * 0.10f)
                lineTo(center.x + innerRingRadius * 0.08f, crownY + innerRingRadius * 0.10f)
                close()
            }
            drawPath(crownPoint, Color(0xFFF59E0B))

            // "S" letter on the Anchor Crown
            drawIntoCanvas { canvas ->
                val sPaint = Paint().apply {
                    color = Color(0xFF0A0F1D).toArgb()
                    textSize = (innerRingRadius * 0.18f).coerceAtLeast(8f)
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText("S", center.x, crownY + innerRingRadius * 0.06f, sPaint)
            }
        }
    }
}

private fun DrawScope.drawStarRay(
    center: Offset,
    angleDeg: Float,
    length: Float,
    width: Float,
    highlightColor: Color,
    shadowColor: Color
) {
    rotate(angleDeg, pivot = center) {
        val leftRay = Path().apply {
            moveTo(center.x, center.y)
            lineTo(center.x, center.y - length)
            lineTo(center.x - width / 2f, center.y)
            close()
        }
        drawPath(leftRay, highlightColor)

        val rightRay = Path().apply {
            moveTo(center.x, center.y)
            lineTo(center.x, center.y - length)
            lineTo(center.x + width / 2f, center.y)
            close()
        }
        drawPath(rightRay, shadowColor)
    }
}

/**
 * Full Hyperion Nautical Logo with Typography.
 * Displays the official medallion emblem paired with authentic "HYPERION By HyperionNavTech" branding.
 */
@Composable
fun HyperionLogo(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 80.dp,
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
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "HYPERION",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    fontFamily = FontFamily.Serif
                ),
                color = MaterialTheme.colorScheme.primary
            )
            if (showSubtitle) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "By HyperionNavTech",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Serif
                    ),
                    color = MaritimeGoldBright
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "MARITIME LICENSURE REVIEWER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Aligned with MARINA & STCW Competency Standards",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
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
                    color = MaterialTheme.colorScheme.primary
                )
                if (showSubtitle) {
                    Text(
                        text = "By HyperionNavTech",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        color = MaritimeGoldBright
                    )
                    Text(
                        text = "Maritime Licensure Reviewer",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
