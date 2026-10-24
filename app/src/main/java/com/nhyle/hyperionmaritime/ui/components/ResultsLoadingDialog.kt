package com.nhyle.hyperionmaritime.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import kotlinx.coroutines.delay

/**
 * Full-screen 3-second suspense popup shown after finishing a drill, practice or exam.
 * The screen behind stays hidden until it finishes.
 */
@Composable
fun ResultsLoadingDialog(onFinished: () -> Unit) {
    val colors = LocalSpaghettiColors.current
    val step = remember { mutableIntStateOf(0) }
    val lines = listOf(
        "CHECKING CORRECT ANSWERS",
        "CALCULATING SCORE",
        "ANALYZING PERFORMANCE",
        "PREPARING YOUR RESULTS"
    )

    BackHandler(enabled = true) { /* blocked while results are being prepared */ }

    LaunchedEffect(Unit) {
        delay(800L)
        step.intValue = 1
        delay(800L)
        step.intValue = 2
        delay(700L)
        step.intValue = 3
        delay(700L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 420.dp)
                .background(colors.surface)
                .border(BorderStroke(1.5.dp, colors.border), RectangleShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colors.accent)
                )

                Spacer(modifier = Modifier.height(22.dp))

                CircularProgressIndicator(
                    modifier = Modifier.size(52.dp),
                    color = colors.accent,
                    strokeWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "PROCESSING RESULTS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface2)
                        .border(1.dp, colors.border)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    lines.forEachIndexed { index, line ->
                        if (index <= step.intValue) {
                            val done = index < step.intValue
                            Text(
                                text = if (done) "> [OK] $line" else "> $line...",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (done) FontWeight.Normal else FontWeight.Bold,
                                color = if (done) colors.success else colors.foreground,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}