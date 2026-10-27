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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.exifinterface.media.ExifInterface
import app.stringer.data.Photos
import app.stringer.data.Press
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
        if (name.isEmpty()) return null
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
     * little contrast pushed in first, as newsprint swallows the mid-greys. In [colour] the photo is
     * separated into cyan, magenta, yellow and black, each plate screened at its own angle.
     */
    fun screen(src: Bitmap, width: Int, cells: Int, paper: Int, ink: Int, colour: Boolean = false): Bitmap {
        val height = src.height * width / src.width
        val small = Bitmap.createScaledBitmap(src, cells * 2, (cells * 2f * src.height / src.width).toInt().coerceAtLeast(2), true)
        val px = IntArray(small.width * small.height)
        small.getPixels(px, 0, small.width, 0, 0, small.width, small.height)
        val step = width.toFloat() / cells
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (!colour) {
            val canvas = Canvas(out)
            canvas.drawColor(paper)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
            dots(canvas, p, width, height, step, 45.0, small, px) { r, g, b -> grey(r, g, b) }
            return out
        }

        // Each plate is drawn as a mask, then laid over the paper as ink would be: the colours multiply.
        val n = width * height
        val acc = FloatArray(n * 3)
        for (i in 0 until n) {
            acc[i * 3] = ((paper shr 16) and 255) / 255f
            acc[i * 3 + 1] = ((paper shr 8) and 255) / 255f
            acc[i * 3 + 2] = (paper and 255) / 255f
        }
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val mc = Canvas(mask)
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        val m = IntArray(n)
        Plate.entries.forEach { plate ->
            mask.eraseColor(android.graphics.Color.BLACK)
            dots(mc, white, width, height, step, plate.angle, small, px) { r, g, b -> plate.cover(r, g, b) }
            mask.getPixels(m, 0, width, 0, 0, width, height)
            val tint = if (plate == Plate.K) ink else plate.ink
            val ir = ((tint shr 16) and 255) / 255f
            val ig = ((tint shr 8) and 255) / 255f
            val ib = (tint and 255) / 255f
            for (i in 0 until n) {
                val a = (m[i] and 255) / 255f
                if (a == 0f) continue
                acc[i * 3] *= 1f - a * (1f - ir)
                acc[i * 3 + 1] *= 1f - a * (1f - ig)
                acc[i * 3 + 2] *= 1f - a * (1f - ib)
            }
        }
        for (i in 0 until n) {
            m[i] = (255 shl 24) or ((acc[i * 3] * 255).toInt() shl 16) or ((acc[i * 3 + 1] * 255).toInt() shl 8) or (acc[i * 3 + 2] * 255).toInt()
        }
        out.setPixels(m, 0, width, 0, 0, width, height)
        return out
    }

    /** One screen of dots on a grid turned by [angle]; [cover] says how much of each cell the ink takes, 0..1. */
    private inline fun dots(canvas: Canvas, p: Paint, width: Int, height: Int, step: Float, angle: Double, small: Bitmap, px: IntArray, cover: (Float, Float, Float) -> Float) {
        val a = Math.toRadians(angle)
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
                    val k = cover(((c shr 16) and 255) / 255f, ((c shr 8) and 255) / 255f, (c and 255) / 255f)
                    val r = step * 0.74f * sqrt(k)
                    if (r > 0.35f) canvas.drawCircle(x, y, r, p)
                }
                v += step
            }
            u += step
        }
    }

    private fun grey(r: Float, g: Float, b: Float): Float {
        val lum = 0.299f * r + 0.587f * g + 0.114f * b
        return ((1f - lum).pow(1.4f) * 1.08f).coerceIn(0f, 1f)
    }

    /** The four plates of a colour press, at their usual screen angles, so the dots make rosettes, not moiré. */
    private enum class Plate(val angle: Double, val ink: Int) {
        C(15.0, 0xFF00A0E0.toInt()), M(75.0, 0xFFE0007A.toInt()), Y(0.0, 0xFFFFE600.toInt()), K(45.0, 0);

        fun cover(r: Float, g: Float, b: Float): Float {
            val k = 1f - maxOf(r, g, b)
            if (this == K) return (k.pow(1.3f) * 1.05f).coerceIn(0f, 1f)
            if (k >= 0.999f) return 0f
            val v = when (this) { C -> r; M -> g; else -> b }
            return ((1f - v - k) / (1f - k)).coerceIn(0f, 1f)
        }
    }

    /** A cached screened copy of a photo. */
    fun cached(c: Context, name: String, width: Int, cells: Int, paper: Int, ink: Int, colour: Boolean = false): Bitmap? {
        if (name.isEmpty()) return null
        val dir = File(c.cacheDir, "halftone").apply { mkdirs() }
        val f = File(dir, "$name-$width-$cells${if (colour) "-cmyk" else ""}.png")
        if (f.exists()) BitmapFactory.decodeFile(f.path)?.let { return it }
        val src = decode(c, name, maxOf(width, 600)) ?: return null
        val out = screen(src, width, cells, paper, ink, colour)
        runCatching { f.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        return out
    }
}

/** How the press prints, from the settings; read by every [Printed]. */
val LocalPress = staticCompositionLocalOf { Press.MONO }

/**
 * A filed frame as it will look in print. [cells] is the screen's coarseness: fewer is coarser.
 * With [Press.PHOTO] there is no screen: the photo goes in as shot.
 */
@Composable
fun Printed(photo: String, modifier: Modifier = Modifier, width: Int = 900, cells: Int = 110, crop: Boolean = true, press: Press = LocalPress.current) {
    val c = ctx()
    val paper = S.c.newsprint.toArgb()
    val ink = S.c.print.toArgb()
    val image by produceState<ImageBitmap?>(null, photo, width, cells, press) {
        value = withContext(Dispatchers.IO) {
            if (press == Press.PHOTO) Halftone.decode(c, photo, width)?.asImageBitmap()
            else Halftone.cached(c, photo, width, cells, paper, ink, press == Press.COLOUR)?.asImageBitmap()
        }
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
