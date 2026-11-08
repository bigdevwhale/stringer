package app.stringer.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.stringer.R
import app.stringer.data.Photos
import app.stringer.ui.art.Halftone
import app.stringer.ui.ctx
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/** The frame shapes the paper prints in: upright as a column, square, wide as the lead. */
private val SHAPES = listOf(0.8f to "4:5", 1f to "1:1", 1.25f to "5:4", 1.5f to "3:2")

/**
 * Cropping a frame before it goes to press (#7): the photo moves and zooms under a fixed frame, the
 * frame's shape is one of the paper's, and what is inside is saved as a new photo. [onCropped] gets
 * its name; the original is left to the caller.
 */
@Composable
fun CropDialog(photo: String, onDismiss: () -> Unit, onCropped: (String) -> Unit) {
    val c = ctx()
    val scope = rememberCoroutineScope()
    val bitmap by produceState<Bitmap?>(null, photo) {
        value = withContext(Dispatchers.IO) { Halftone.decode(c, photo, 2000) }
    }
    var shape by remember { mutableStateOf(0.8f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var busy by remember { mutableStateOf(false) }
    var viewW by remember { mutableFloatStateOf(1f) }

    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(S.c.print).statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
            Text(stringResource(R.string.crop), style = SType.Title.copy(color = S.c.newsprint))
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.crop_hint), style = SType.Small.copy(color = S.c.newsprint.copy(alpha = 0.7f)))
            Spacer(Modifier.weight(1f))
            val b = bitmap
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val boxW = constraints.maxWidth.toFloat()
                val boxH = boxW / shape
                SideEffect { viewW = boxW }
                // The frame fits the width when it is wide enough, the height otherwise.
                Box(
                    Modifier.fillMaxWidth().aspectRatio(shape).clipToBounds().border(2.dp, S.c.newsprint)
                        .pointerInput(b, shape) {
                            if (b == null) return@pointerInput
                            detectTransformGestures { _, p, z, _ ->
                                val nz = (zoom * z).coerceIn(1f, 6f)
                                val base = max(boxW / b.width, boxH / b.height)
                                val w = b.width * base * nz
                                val h = b.height * base * nz
                                val mx = ((w - boxW) / 2).coerceAtLeast(0f)
                                val my = ((h - boxH) / 2).coerceAtLeast(0f)
                                zoom = nz
                                pan = Offset((pan.x + p.x).coerceIn(-mx, mx), (pan.y + p.y).coerceIn(-my, my))
                            }
                        },
                ) {
                    if (b != null) {
                        val image = remember(b) { b.asImageBitmap() }
                        Canvas(Modifier.fillMaxSize()) {
                            val base = max(size.width / b.width, size.height / b.height)
                            val w = b.width * base * zoom
                            val h = b.height * base * zoom
                            drawImage(
                                image,
                                dstOffset = IntOffset((size.width / 2 - w / 2 + pan.x).toInt(), (size.height / 2 - h / 2 + pan.y).toInt()),
                                dstSize = IntSize(w.toInt(), h.toInt()),
                            )
                            // Thirds, as in a viewfinder.
                            for (i in 1..2) {
                                drawLine(Color.White.copy(alpha = 0.35f), Offset(size.width * i / 3, 0f), Offset(size.width * i / 3, size.height), 1f)
                                drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, size.height * i / 3), Offset(size.width, size.height * i / 3), 1f)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                SHAPES.forEach { (r, label) ->
                    Chip(label, shape == r) { shape = r; zoom = 1f; pan = Offset.Zero }
                }
            }
            Spacer(Modifier.weight(1f))
            Row {
                SoftButton(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                PrimaryButton(
                    stringResource(R.string.crop_done), {
                        val bm = b ?: return@PrimaryButton
                        if (busy) return@PrimaryButton
                        busy = true
                        val z = zoom; val p = pan; val r = shape; val w = viewW
                        scope.launch {
                            val name = withContext(Dispatchers.IO) { Photos.crop(c, bm, r, z, p.x / w, p.y / w) }
                            busy = false
                            if (name != null) onCropped(name) else onDismiss()
                        }
                    },
                    Modifier.weight(1f), enabled = b != null && !busy, icon = Icons.Rounded.Check,
                )
            }
        }
    }
}
