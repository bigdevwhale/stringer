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
 * The page is cut into A4 sheets, each cut moved up to a blank row so no frame is split in two.
 */
object Printer {
    fun print(activity: Activity, page: Bitmap, name: String) {
        val pm = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        // A captured layer can come back as a hardware bitmap, whose pixels can't be read for the cuts.
        val soft = if (page.config == Bitmap.Config.HARDWARE) page.copy(Bitmap.Config.ARGB_8888, false) else page
        pm.print(name, Adapter(activity, soft, name), attrs)
    }

    /** Where to cut [page] into sheets of [ratio] height to width. */
    fun cuts(page: Bitmap, ratio: Float): List<Int> {
        val sheet = (page.width * ratio).toInt()
        val row = IntArray(page.width)
        val out = mutableListOf(0)
        while (page.height - out.last() > sheet) {
            val ideal = out.last() + sheet
            // Look back up to a quarter sheet for a row of plain paper; otherwise cut where it falls.
            val blank = (ideal downTo ideal - sheet / 4).firstOrNull { y ->
                page.getPixels(row, 0, page.width, 0, y, page.width, 1)
                plain(row)
            }
            out += blank ?: ideal
        }
        out += page.height
        return out
    }

    /** A row with nothing printed on it but the outer border: every pixel inside matches the paper. */
    private fun plain(row: IntArray): Boolean {
        val inset = row.size / 20
        val paper = row[row.size / 2]
        for (i in inset until row.size - inset) if (row[i] != paper) return false
        return true
    }

    private class Adapter(private val c: Context, private val page: Bitmap, private val name: String) : PrintDocumentAdapter() {
        private var attrs: PrintAttributes? = null
        private var cuts: List<Int> = emptyList()

        override fun onLayout(old: PrintAttributes?, new: PrintAttributes, cancel: CancellationSignal, callback: LayoutResultCallback, extras: Bundle?) {
            if (cancel.isCanceled) return callback.onLayoutCancelled()
            attrs = new
            val size = new.mediaSize ?: PrintAttributes.MediaSize.ISO_A4
            val portrait = if (size.isPortrait) size else size.asPortrait()
            cuts = cuts(page, portrait.heightMils.toFloat() / portrait.widthMils)
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
                    if (pages.none { i in it.start..it.end } && pages.none { it == PageRange.ALL_PAGES }) continue
                    val p = pdf.startPage(i)
                    draw(p.canvas, p.info.contentRect.let { RectF(it) }, cuts[i], cuts[i + 1], paint)
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

        /** One slice of the page, as wide as the sheet allows, from the top. */
        private fun draw(canvas: Canvas, area: RectF, top: Int, bottom: Int, paint: Paint) {
            val scale = area.width() / page.width
            val dst = RectF(area.left, area.top, area.right, area.top + (bottom - top) * scale)
            canvas.drawBitmap(page, Rect(0, top, page.width, bottom), dst, paint)
        }
    }
}
