package app.stringer.ui.issue

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import java.io.FileOutputStream

/**
 * The issue off to a real printer, through the phone's own print service (or "Save as PDF").
 * The page is cut into sheets only between its blocks ([breaks]), so a frame is never split and a
 * rubric's head never ends a sheet. Every sheet is laid out as a whole page: newsprint inside a
 * thin rule, with white margins round it.
 */
object Printer {
    /** White paper round the page, in points (10 mm). */
    private const val MARGIN = 28f

    /** The widths the page may print at, as a share of the sheet's, widest first. */
    private val WIDTHS = listOf(1f, 0.95f, 0.9f, 0.85f, 0.8f, 0.75f)

    /** [edge] is the page's border in pixels (1dp), which the sheets draw afresh. */
    fun print(activity: Activity, page: Bitmap, name: String, breaks: List<Int>, edge: Int) {
        val pm = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        // A captured layer can come back as a hardware bitmap, whose pixels can't be read.
        val soft = if (page.config == Bitmap.Config.HARDWARE) page.copy(Bitmap.Config.ARGB_8888, false) else page
        pm.print(name, Adapter(activity, soft, name, breaks, edge), attrs)
    }

    /**
     * Where to cut a page [height] pixels tall into sheets holding [first] pixels on the first and
     * [rest] on each after it: at the lowest break that still fits, or, with none, where it falls.
     */
    fun cuts(height: Int, first: Int, rest: Int, breaks: List<Int>): List<Int> {
        val out = mutableListOf(0)
        while (height - out.last() > if (out.size == 1) first else rest) {
            val start = out.last()
            val span = if (out.size == 1) first else rest
            // A break in the sheet's top quarter would leave it nearly empty: better to cut through.
            out += breaks.lastOrNull { it in (start + span / 4)..(start + span) } ?: (start + span)
        }
        out += height
        return out
    }

    private class Adapter(
        private val c: Context,
        private val page: Bitmap,
        private val name: String,
        private val breaks: List<Int>,
        private val edge: Int,
    ) : PrintDocumentAdapter() {
        private var attrs: PrintAttributes? = null
        private var cuts: List<Int> = emptyList()

        /** The share of the sheet's width the page prints at. */
        private var width = 1f
        private val paper = page.getPixel(page.width / 2, edge * 4)
        private val rule = page.getPixel(0, page.height / 2)

        /** Space above a continued sheet's first block, as the page has above its masthead (12dp). */
        private val lead = edge * 12

        override fun onLayout(old: PrintAttributes?, new: PrintAttributes, cancel: CancellationSignal, callback: LayoutResultCallback, extras: Bundle?) {
            if (cancel.isCanceled) return callback.onLayoutCancelled()
            attrs = new
            val size = (new.mediaSize ?: PrintAttributes.MediaSize.ISO_A4).let { if (it.isPortrait) it else it.asPortrait() }
            val m = new.minMargins ?: PrintAttributes.Margins.NO_MARGINS
            val w = (size.widthMils - m.leftMils - m.rightMils) * 72f / 1000 - MARGIN * 2
            val h = (size.heightMils - m.topMils - m.bottomMils) * 72f / 1000 - MARGIN * 2
            // A block often takes more than half a sheet at full width, leaving the rest blank. So the
            // page may print a little narrower, centred, if that saves a sheet; the widest that does wins.
            val best = WIDTHS.map { k ->
                val fits = (h * (page.width - edge * 2) / (w * k)).toInt()
                k to cuts(page.height, fits, fits - lead, breaks)
            }.minBy { it.second.size }
            width = best.first
            cuts = best.second
            val info = PrintDocumentInfo.Builder("$name.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(cuts.size - 1)
                .build()
            callback.onLayoutFinished(info, new != old)
        }

        override fun onWrite(pages: Array<out PageRange>, destination: ParcelFileDescriptor, cancel: CancellationSignal, callback: WriteResultCallback) {
            val pdf = PrintedPdfDocument(c, attrs ?: return callback.onWriteFailed(null))
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            try {
                for (i in 0 until cuts.size - 1) {
                    if (cancel.isCanceled) return callback.onWriteCancelled()
                    if (pages.none { it == PageRange.ALL_PAGES || i in it.start..it.end }) continue
                    val p = pdf.startPage(i)
                    val area = RectF(p.info.contentRect).apply { inset(MARGIN, MARGIN) }
                    sheet(p.canvas, area, cuts[i], cuts[i + 1], i > 0, paint)
                    pdf.finishPage(p)
                }
                FileOutputStream(destination.fileDescriptor).use { pdf.writeTo(it) }
                callback.onWriteFinished(pages)
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                pdf.close()
            }
        }

        /** One sheet: newsprint and its rule, and the slice of the page from [top] to [bottom]. */
        private fun sheet(canvas: Canvas, area: RectF, top: Int, bottom: Int, continued: Boolean, paint: Paint) {
            val side = area.width() * (1 - width) / 2
            area.inset(side, 0f)
            canvas.drawRect(area, Paint().apply { color = paper })
            val scale = area.width() / (page.width - edge * 2)
            val src = Rect(edge, maxOf(top, edge), page.width - edge, minOf(bottom, page.height - edge))
            val y = area.top + if (continued) lead * scale else 0f
            canvas.drawBitmap(page, src, RectF(area.left, y, area.right, y + src.height() * scale), paint)
            val stroke = maxOf(edge * scale, 0.5f)
            canvas.drawRect(area, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rule; style = Paint.Style.STROKE; strokeWidth = stroke })
        }
    }
}
