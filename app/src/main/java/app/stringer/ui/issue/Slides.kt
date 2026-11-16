package app.stringer.ui.issue

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import app.stringer.R
import app.stringer.ui.components.PrimaryButton
import app.stringer.ui.components.SoftButton
import app.stringer.ui.theme.S
import app.stringer.ui.theme.SType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The issue for social networks: one tall page is shrunk by a feed into an unreadable strip, so it
 * goes out as a carousel of portrait cards instead, 4:5 as a feed shows a post whole. Like printed
 * sheets, the cards are cut only between blocks, each a whole page on the newsroom table.
 */
object Slides {
    private const val W = 1080
    private const val H = 1350

    /** The table round the page, in pixels. */
    private const val MARGIN = 48f

    /** The newsroom table (the app's background), round each card's page. */
    private const val TABLE = 0xFFECE6D8.toInt()

    /** A card's page may sit a little narrower to save a card, but not so narrow it reads small. */
    private val WIDTHS = listOf(1f, 0.93f, 0.86f)

    /**
     * Lays [page] out on cards and saves them as JPEG files in the cache, replacing any from before.
     * [breaks] and [edge] are as for [Printer.print].
     */
    fun save(c: Context, page: Bitmap, number: Int, breaks: List<Int>, edge: Int): List<File> {
        val src = Printer.soft(page)
        val dir = File(c.cacheDir, "issues").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val paper = src.getPixel(src.width / 2, edge * 4)
        val rule = src.getPixel(0, src.height / 2)
        val lead = edge * 12
        val (width, cuts) = Printer.fit(src.height, src.width - edge * 2, W - MARGIN * 2, H - MARGIN * 2, lead, breaks, WIDTHS)
        val card = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(card)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val many = cuts.size > 2
        return try {
            (0 until cuts.size - 1).map { i ->
                canvas.drawColor(TABLE)
                val area = RectF(MARGIN, MARGIN, W - MARGIN, H - MARGIN)
                Printer.sheet(canvas, area, src, edge, paper, rule, lead, width, cuts[i], cuts[i + 1], i > 0, paint)
                // Feeds sort shared files by name: the number keeps the cards in the paper's order.
                val f = File(dir, if (many) "issue-$number-${(i + 1).toString().padStart(2, '0')}.jpg" else "issue-$number.jpg")
                f.outputStream().use { card.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                f
            }
        } finally {
            card.recycle()
        }
    }

    /** Offers the cards to whatever takes images: one as a picture, several as an album. */
    fun share(c: Context, files: List<File>) {
        val uris = files.map { FileProvider.getUriForFile(c, "${c.packageName}.files", it) }
        if (uris.isEmpty()) return
        val send = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
        }.apply {
            type = "image/jpeg"
            // The read grant travels with the clip, for every card at once.
            clipData = ClipData.newUri(c.contentResolver, null, uris[0]).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { c.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Toast.makeText(c, R.string.no_share, Toast.LENGTH_LONG).show() }
    }
}

/**
 * The cards as they'll go out, before they do: swiped through one by one, with the next peeking in
 * at the side, then sent on with «Share».
 */
@Composable
fun SlidesPreview(files: List<File>, onDismiss: () -> Unit, onShare: () -> Unit) {
    // Half size is plenty for a phone's screen, and keeps ten cards light.
    val cards by produceState<List<ImageBitmap>?>(null, files) {
        value = withContext(Dispatchers.IO) {
            files.mapNotNull { f -> BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 2 })?.asImageBitmap() }
        }
    }
    val pager = rememberPagerState { files.size }

    // Edge to edge, so the dark covers the bars too and the issue doesn't show under them.
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(S.c.print).statusBarsPadding().navigationBarsPadding().padding(vertical = 20.dp)) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(stringResource(R.string.share_preview), style = SType.Title.copy(color = S.c.newsprint))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.share_preview_hint), style = SType.Small.copy(color = S.c.newsprint.copy(alpha = 0.7f)))
            }
            Spacer(Modifier.weight(1f))
            HorizontalPager(pager, Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 36.dp), pageSpacing = 12.dp) { i ->
                val card = cards?.getOrNull(i)
                Box(Modifier.fillMaxWidth().aspectRatio(0.8f).background(S.c.bg)) {
                    if (card != null) Image(card, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }
            Spacer(Modifier.height(14.dp))
            if (files.size > 1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    repeat(files.size) { i ->
                        val on = i == pager.currentPage
                        Box(Modifier.size(if (on) 8.dp else 6.dp).clip(CircleShape).background(S.c.newsprint.copy(alpha = if (on) 1f else 0.4f)).align(Alignment.CenterVertically))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.padding(horizontal = 20.dp)) {
                SoftButton(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f).align(Alignment.CenterVertically))
                Spacer(Modifier.width(10.dp))
                PrimaryButton(stringResource(R.string.share), onShare, Modifier.weight(1f))
            }
        }
    }
}
