package com.nhyle.hyperionmaritime.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import androidx.compose.ui.semantics.Role
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import com.nhyle.hyperionmaritime.data.repository.VerifiedQuestionMediaStore
import com.nhyle.hyperionmaritime.data.repository.QuestionMediaException
import kotlinx.coroutines.CancellationException

/**
 * Keep diagrams proportionate on phones, tablets and split-screen windows.
 * The bounds use percentages, while caps prevent oversized tablets or tall diagrams.
 */
internal fun questionDiagramThumbnailSize(availableWidth: Dp, screenHeight: Dp, imageWidth: Int, imageHeight: Int): DpSize {
    if (imageWidth <= 0 || imageHeight <= 0) return DpSize(0.dp, 0.dp)
    val maxWidth = minOf(availableWidth, 900.dp)
    val maxHeight = minOf(screenHeight * 0.48f, 540.dp)
    val aspect = imageWidth.toFloat() / imageHeight
    val width = minOf(maxWidth, maxHeight * aspect)
    return DpSize(width, width / aspect)
}

internal fun boundedDiagramZoom(value: Float): Float = value.coerceIn(1f, 2.5f)

private fun decodeBounded(bytes: ByteArray): ImageBitmap? {
    val probe = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes,0,bytes.size,probe)
    if (probe.outWidth !in 1..4096 || probe.outHeight !in 1..4096 ||
        probe.outWidth.toLong() * probe.outHeight > 16_000_000L) return null
    var sample = 1
    while (probe.outWidth / sample > 1600 || probe.outHeight / sample > 1600) sample *= 2
    return runCatching {
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
    }.getOrNull()
}

/**
 * Only renders when the question itself is accessible and a private,
 * hash-verified image is available. No direct Storage URL or embedded data
 * URI is ever exposed. Missing media never makes a text question unusable.
 */
@Composable
fun QuestionImageGallery(question: Question, placement: String, freeReviewer: Boolean = false) {
    val refs = question.imageAttachments.filter { it.placement == placement }
    if (refs.isEmpty()) return
    val context = LocalContext.current
    val revision by LicenseManager.accountEntitlementRevisionFlow.collectAsState()
    val unlocked by when (question.track) {
        Track.DECK_OIC_NW -> LicenseManager.isOicNwProUnlockedFlow
        Track.GMDSS_GOC -> LicenseManager.isGmdssProUnlockedFlow
        Track.ENGINE_OIC_EW -> LicenseManager.isEngineOicEwProUnlockedFlow
        Track.DECK_MGT -> LicenseManager.isDeckMgtProUnlockedFlow
        Track.ENGINE_MGT -> LicenseManager.isEngineMgtProUnlockedFlow
    }.collectAsState()
    fun currentAccess() = LicenseManager.getCurrentAccountUserId(context) to
        LicenseManager.isTrackProUnlocked(context, question.track)
    var access by remember(context, question.track, revision, unlocked) { mutableStateOf(currentAccess()) }
    // Expiry can pass without an entitlement event. This checks local state only.
    LaunchedEffect(context, question.track, revision, unlocked) {
        while (true) {
            delay(1_000)
            access = withContext(Dispatchers.IO) { currentAccess() }
        }
    }
    val owner = access.first
    if (!access.second && !freeReviewer) return
    val scope = listOf(question.id, question.track, placement, refs, owner, freeReviewer)
    val bitmaps = remember(scope) { mutableStateMapOf<String, ImageBitmap>() }
    val failures = remember(scope) { mutableStateMapOf<String, String>() }
    var enlarged by remember(scope) { mutableStateOf<String?>(null) }
    var retry by remember(scope) { mutableStateOf(0) }
    LaunchedEffect(scope, retry) {
        for (ref in refs) {
            if (bitmaps.containsKey(ref.sha256)) continue
            failures.remove(ref.sha256)
            var failure: String? = null
            val bytes = try {
                VerifiedQuestionMediaStore.loadForDisplay(
                    context, question.track, ref, freeQuestion = if (freeReviewer) question else null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: QuestionMediaException) {
                failure = error.reason.description
                null
            }
            val decoded = withContext(Dispatchers.Default) { bytes?.let(::decodeBounded) }
            currentCoroutineContext().ensureActive()
            if (!freeReviewer && currentAccess() != (owner to true)) return@LaunchedEffect
            if (decoded != null) bitmaps[ref.sha256] = decoded
            else failures[ref.sha256] = failure ?: "Diagram unavailable. This image could not be displayed. Retry or update the app."
        }
    }

    Column(modifier=Modifier.fillMaxWidth().padding(top=10.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        refs.forEach { ref ->
            val bitmap = bitmaps[ref.sha256]
            if (bitmap != null) {
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val size = questionDiagramThumbnailSize(maxWidth, screenHeight, bitmap.width, bitmap.height)
                    Image(
                        bitmap = bitmap,
                        contentDescription = ref.alt,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.width(size.width).height(size.height)
                            .clickable(onClickLabel = "Enlarge diagram", role = Role.Button) { enlarged = ref.sha256 }
                    )
                }
            } else if (failures.containsKey(ref.sha256)) {
                Column {
                    Text(
                        failures.getValue(ref.sha256),
                        style=MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(onClick = { retry++ }) { Text("Retry") }
                }
            } else {
                CircularProgressIndicator(modifier=Modifier.padding(8.dp))
            }
        }
    }
    val selected = enlarged?.let(bitmaps::get)
    if (selected != null) {
        var scale by remember(enlarged) { mutableStateOf(1f) }
        var pan by remember(enlarged) { mutableStateOf(Offset.Zero) }
        var viewport by remember(enlarged) { mutableStateOf(IntSize.Zero) }

        fun boundedPan(offset: Offset, zoom: Float): Offset {
            if (viewport.width == 0 || viewport.height == 0) return Offset.Zero
            val fit = minOf(viewport.width.toFloat() / selected.width, viewport.height.toFloat() / selected.height)
            val horizontal = ((selected.width * fit * zoom - viewport.width) / 2f).coerceAtLeast(0f)
            val vertical = ((selected.height * fit * zoom - viewport.height) / 2f).coerceAtLeast(0f)
            return Offset(offset.x.coerceIn(-horizontal, horizontal), offset.y.coerceIn(-vertical, vertical))
        }

        fun setBoundedScale(requested: Float) {
            scale = boundedDiagramZoom(requested)
            pan = boundedPan(pan, scale)
        }

        Dialog(
            onDismissRequest = { enlarged = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()
                            .onSizeChanged { viewport = it; pan = boundedPan(pan, scale) }
                            .pointerInput(enlarged) {
                                detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                                    val nextScale = boundedDiagramZoom(scale * gestureZoom)
                                    pan = boundedPan(pan + gesturePan, nextScale)
                                    scale = nextScale
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = selected,
                            contentDescription = refs.firstOrNull { it.sha256 == enlarged }?.alt,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = pan.x
                                translationY = pan.y
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.76f), RectangleShape)
                            .testTag("diagram_zoom_controls")
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { setBoundedScale(scale - 0.25f) },
                            enabled = scale > 1f,
                            shape = RectangleShape,
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.semantics { contentDescription = "Zoom out" }
                        ) { Text("−") }
                        Text(
                            (scale * 100).roundToInt().toString() + "%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        OutlinedButton(
                            onClick = { setBoundedScale(scale + 0.25f) },
                            enabled = scale < 2.5f,
                            shape = RectangleShape,
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.semantics { contentDescription = "Zoom in" }
                        ) { Text("+") }
                        OutlinedButton(
                            onClick = { scale = 1f; pan = Offset.Zero },
                            shape = RectangleShape,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text("Reset") }
                    }
                }
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(44.dp)
                        .background(Color(0xD9C62828), RectangleShape)
                        .clickable(onClickLabel = "Close image", role = Role.Button) { enlarged = null }
                        .semantics { contentDescription = "Close image" },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
        }
    }
}
