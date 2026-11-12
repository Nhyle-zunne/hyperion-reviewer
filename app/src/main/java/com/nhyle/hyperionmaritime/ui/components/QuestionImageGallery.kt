package com.nhyle.hyperionmaritime.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val bitmaps = remember(question.id,placement) { mutableStateMapOf<String, ImageBitmap>() }
    val failures = remember(question.id,placement) { mutableStateMapOf<String, Boolean>() }
    var enlarged by remember(question.id,placement) { mutableStateOf<String?>(null) }
    LaunchedEffect(question.id,placement,refs) {
        for (ref in refs) {
            val bytes = VerifiedQuestionMediaStore.load(context,question.track,ref)
            val decoded = bytes?.let(::decodeBounded)
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
                            .clickable { enlarged = ref.sha256 }
                    )
                    Text(ref.alt,style=MaterialTheme.typography.labelSmall)
                    Text("Tap image to zoom",style=MaterialTheme.typography.labelSmall)
                }
            } else if (failures[ref.sha256] == true) {
                Text(
                    "Diagram unavailable. Sign in and connect once to download this licensed image.",
                    style=MaterialTheme.typography.bodySmall
                )
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
            },
            confirmButton={
                OutlinedButton(onClick={enlarged=null}) { Text("Close") }
            }
        )
    }
}
