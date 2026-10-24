package com.nhyle.hyperionmaritime.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.ui.motion.HyperionDialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LicenseAuthenticationDialog(
    isVisible: Boolean
) {
    if (!isVisible) return

    val colors = com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors.current
    val isDark = colors.isDark
    val dialogBg = if (isDark) Color(0xFF0B1321) else Color.White
    val borderColor = if (isDark) Color(0xFF00E599).copy(alpha = 0.5f) else Color(0xFF16A34A).copy(alpha = 0.5f)
    val indicatorColor = if (isDark) Color(0xFF00E599) else Color(0xFF16A34A)
    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtextColor = if (isDark) Color(0xFF8E9BAE) else Color(0xFF64748B)

    HyperionDialog(
        onDismissRequest = {}, // Non-cancelable while verifying
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = indicatorColor,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(38.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "AUTHENTICATING LICENSE CLEARANCE",
                    color = titleColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Querying cryptographic server registry...",
                    color = subtextColor,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
