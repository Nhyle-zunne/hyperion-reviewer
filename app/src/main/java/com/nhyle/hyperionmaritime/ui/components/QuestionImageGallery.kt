package com.nhyle.hyperionmaritime.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import androidx.compose.ui.semantics.Role
import com.nhyle.hyperionmaritime.data.repository.VerifiedQuestionMediaStore

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
fun QuestionImageGallery(question: Question, placement: String) {
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
            access = currentAccess()
        }
    }
    val owner = access.first
    if (owner == null || !access.second) return
    val scope = listOf(question.id, question.track, placement, refs, owner)
    val bitmaps = remember(scope) { mutableStateMapOf<String, ImageBitmap>() }
    val failures = remember(scope) { mutableStateMapOf<String, Boolean>() }
    var enlarged by remember(scope) { mutableStateOf<String?>(null) }
    var retry by remember(scope) { mutableStateOf(0) }
    LaunchedEffect(scope, retry) {
        for (ref in refs) {
            if (bitmaps.containsKey(ref.sha256)) continue
            failures.remove(ref.sha256)
            val bytes = VerifiedQuestionMediaStore.load(context, question.track, ref)
            val decoded = withContext(Dispatchers.Default) { bytes?.let(::decodeBounded) }
            currentCoroutineContext().ensureActive()
            if (currentAccess() != (owner to true)) return@LaunchedEffect
            if (decoded != null) bitmaps[ref.sha256] = decoded
            else failures[ref.sha256] = true
        }
    }

    Column(modifier=Modifier.fillMaxWidth().padding(top=10.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        refs.forEach { ref ->
            val bitmap = bitmaps[ref.sha256]
            if (bitmap != null) {
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Image(
                        bitmap=bitmap,
                        contentDescription=ref.alt,
                        contentScale=ContentScale.Fit,
                        modifier=Modifier.fillMaxWidth().heightIn(max=420.dp)
                            .clickable(onClickLabel = "Enlarge diagram", role = Role.Button) { enlarged = ref.sha256 }
                    )
                    Text(ref.alt,style=MaterialTheme.typography.labelSmall)
                    Text("Tap image to zoom",style=MaterialTheme.typography.labelSmall)
                }
            } else if (failures[ref.sha256] == true) {
                Column {
                    Text(
                        "Diagram unavailable. Sign in and connect once to download this licensed image.",
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
        AlertDialog(
            onDismissRequest={enlarged=null},
            title={Text("Question diagram")},
            text={
                Column {
                Image(
                    bitmap=selected,
                    contentDescription=refs.firstOrNull { it.sha256==enlarged }?.alt,
                    contentScale=ContentScale.Fit,
                    modifier=Modifier.fillMaxWidth().heightIn(max=580.dp)
                        .graphicsLayer(scaleX=scale,scaleY=scale)
                        .pointerInput(enlarged) {
                            detectTransformGestures { _,_,zoom,_ ->
                                scale=(scale*zoom).coerceIn(1f,4f)
                            }
                        }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { scale = (scale / 1.5f).coerceAtLeast(1f) }, enabled = scale > 1f) { Text("Zoom out") }
                    OutlinedButton(onClick = { scale = (scale * 1.5f).coerceAtMost(4f) }, enabled = scale < 4f) { Text("Zoom in") }
                }
                }
            },
            confirmButton={
                OutlinedButton(onClick={enlarged=null}) { Text("Close") }
            }
        )
    }
}
