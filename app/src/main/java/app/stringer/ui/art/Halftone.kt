package app.stringer.ui.art

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.exifinterface.media.ExifInterface
import app.stringer.data.Photos
import app.stringer.ui.ctx
import app.stringer.ui.theme.S
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The print shop: a photo becomes a newspaper halftone, black dots on newsprint on a grid turned
 * 45°, the way a district paper's press would screen it. Results are cached by photo and size.
 */
object Halftone {
    /** Where a test or the demo can point at its own folder of photos. */
    var source: ((Context, String) -> File)? = null

    fun file(c: Context, name: String): File = source?.invoke(c, name) ?: Photos.file(c, name)

    /** The photo upright and no wider than [max], or null if it isn't there. */
    fun decode(c: Context, name: String, max: Int): Bitmap? {
        val f = file(c, name)
        if (!f.exists()) return null
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        var s = 1
        while (o.outWidth / (s * 2) >= max) s *= 2
        val b = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s }) ?: return null
        val turn = runCatching {
            when (ExifInterface(f.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        val upright = if (turn == 0f) b else Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(turn) }, true)
        return if (upright.width > max) Bitmap.createScaledBitmap(upright, max, upright.height * max / upright.width, true) else upright
    }

    /**
     * Screens [src] at [cells] dots across. Each dot's area follows the darkness under it, with a
     * little contrast pushed in first, as newsprint swallows the mid-greys.
     */
    fun screen(src: Bitmap, width: Int, cells: Int, paper: Int, ink: Int): Bitmap {
        val height = src.height * width / src.width
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(paper)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
        val small = Bitmap.createScaledBitmap(src, cells * 2, (cells * 2f * src.height / src.width).toInt().coerceAtLeast(2), true)
        val px = IntArray(small.width * small.height)
        small.getPixels(px, 0, small.width, 0, 0, small.width, small.height)
        val step = width.toFloat() / cells
        val a = Math.toRadians(45.0)
        val ca = cos(a).toFloat()
        val sa = sin(a).toFloat()
        val diag = (width + height).toFloat()
        var u = -diag
        while (u < diag) {
            var v = -diag
            while (v < diag) {
                val x = u * ca - v * sa
                val y = u * sa + v * ca
                if (x > -step && y > -step && x < width + step && y < height + step) {
                    val sx = (x / width * small.width).toInt().coerceIn(0, small.width - 1)
                    val sy = (y / height * small.height).toInt().coerceIn(0, small.height - 1)
                    val c = px[sy * small.width + sx]
                    val lum = (0.299f * ((c shr 16) and 255) + 0.587f * ((c shr 8) and 255) + 0.114f * (c and 255)) / 255f
                    val dark = ((1f - lum).pow(1.4f) * 1.08f).coerceIn(0f, 1f)
                    val r = step * 0.74f * sqrt(dark)
                    if (r > 0.35f) canvas.drawCircle(x, y, r, p)
                }
                v += step
            }
            u += step
        }
        return out
    }

    /** A cached screened copy of a photo. */
    fun cached(c: Context, name: String, width: Int, cells: Int, paper: Int, ink: Int): Bitmap? {
        val dir = File(c.cacheDir, "halftone").apply { mkdirs() }
        val f = File(dir, "$name-$width-$cells.png")
        if (f.exists()) BitmapFactory.decodeFile(f.path)?.let { return it }
        val src = decode(c, name, maxOf(width, 600)) ?: return null
        val out = screen(src, width, cells, paper, ink)
        runCatching { f.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        return out
    }
}

/** A filed frame as it will look in print. [cells] is the screen's coarseness: fewer is coarser. */
@Composable
fun Printed(photo: String, modifier: Modifier = Modifier, width: Int = 900, cells: Int = 110, crop: Boolean = true) {
    val c = ctx()
    val paper = S.c.newsprint.toArgb()
    val ink = S.c.print.toArgb()
    val image by produceState<ImageBitmap?>(null, photo, width, cells) {
        value = withContext(Dispatchers.IO) { Halftone.cached(c, photo, width, cells, paper, ink)?.asImageBitmap() }
    }
    Box(modifier.background(S.c.surface2)) {
        image?.let { Image(it, null, Modifier.matchParentSize(), contentScale = if (crop) ContentScale.Crop else ContentScale.Fit) }
    }
}

/** The photo itself, in colour: what the correspondent actually saw. */
@Composable
fun Original(photo: String, modifier: Modifier = Modifier, max: Int = 1200, crop: Boolean = true) {
    val c = ctx()
    val image by produceState<ImageBitmap?>(null, photo) {
        value = withContext(Dispatchers.IO) { Halftone.decode(c, photo, max)?.asImageBitmap() }
    }
    Box(modifier.background(S.c.surface2)) {
        image?.let { Image(it, null, Modifier.matchParentSize(), contentScale = if (crop) ContentScale.Crop else ContentScale.Fit) }
    }
}
