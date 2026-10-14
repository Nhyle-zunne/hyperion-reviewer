package com.nhyle.hyperionmaritime.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.R
import com.nhyle.hyperionmaritime.ui.theme.MaritimeGoldBright

/**
 * Official HYPERION Medallion Emblem.
 * Directly renders static drawable asset R.drawable.app_logo.
 */
@Composable
fun HyperionEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 90.dp
) {
    Image(
        painter = painterResource(id = R.drawable.hyperion_seal),
        contentDescription = "Hyperion Official Seal",
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentScale = ContentScale.Fit
    )
}

/**
 * Full Hyperion Nautical Logo with Official Typography.
 * Renders the static official emblem paired with "HYPERION" and "By HyperionNavTech".
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
                    text = "By Nhyle",
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
                        text = "By Nhyle",
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
