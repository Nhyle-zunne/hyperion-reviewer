package com.nhyle.hyperionmaritime.ui.motion
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nhyle.hyperionmaritime.viewmodel.Screen
import kotlinx.coroutines.launch

internal fun Screen.motionDepth(): Int = when (this) {
    is Screen.TermsOnboarding -> 0
    is Screen.TrackSelection -> 1
    is Screen.Home -> 2
    is Screen.PracticeCompetenceSelect -> 3
    is Screen.PracticeConfig -> 4
    is Screen.MockHub -> 3
    is Screen.MockCompetence -> 4
    is Screen.QuickScanCompetenceSelect -> 3
    is Screen.QuickScanViewer -> 4
    is Screen.Flashcards, is Screen.Topics -> 3
    is Screen.Quiz, is Screen.FreePractice, is Screen.Results -> 5
    is Screen.SessionReview -> 4
    else -> 3
}

fun AnimatedContentTransitionScope<Screen>.hyperionPageTransition(): ContentTransform {
    val forward = targetState.motionDepth() >= initialState.motionDepth()
    val settle = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )
    val enter = fadeIn(tween(durationMillis = 240, delayMillis = 30, easing = FastOutSlowInEasing)) +
        slideInHorizontally(settle) { width ->
            val distance = (width * 0.07f).toInt().coerceAtLeast(20)
            if (forward) distance else -distance
        }
    val exit = fadeOut(tween(durationMillis = 150, easing = FastOutSlowInEasing)) +
        slideOutHorizontally(tween(durationMillis = 200, easing = FastOutSlowInEasing)) { width ->
            val distance = (width * 0.04f).toInt().coerceAtLeast(12)
            if (forward) -distance else distance
        }
    return (enter togetherWith exit).using(
        SizeTransform(clip = false) { _, _ -> snap<IntSize>() }
    )
}

@Composable
fun HyperionDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        val scale = remember { Animatable(0.96f) }
        val alpha = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            launch {
                alpha.animateTo(1f, tween(durationMillis = 170, easing = LinearOutSlowInEasing))
            }
            launch {
                scale.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
        Box(
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
                transformOrigin = TransformOrigin.Center
            }
        ) {
            content()
        }
    }
}