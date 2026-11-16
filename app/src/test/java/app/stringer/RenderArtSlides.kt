package app.stringer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import app.stringer.data.DemoSeed
import app.stringer.data.Press
import app.stringer.ui.Home
import app.stringer.ui.art.LocalPress
import app.stringer.ui.issue.Breaks
import app.stringer.ui.issue.Capture
import app.stringer.ui.issue.Page
import app.stringer.ui.issue.Slides
import app.stringer.ui.theme.StringerTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.roundToInt

/** The demo's last issue as the cards «Share» sends (run with `-Prender`), to app/build/art/slides. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtSlides {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "slides").apply { mkdirs() } }

    @Test fun slides() {
        val app = ApplicationProvider.getApplicationContext<StringerApp>()
        val h = runBlocking {
            val c = app.container
            DemoSeed.seedIfEmpty(app, c)
            Home(c.db.shots().all(), c.settings.now(), c.catalogue, System.currentTimeMillis(), app.ru)
        }
        val week = h.issues.first { it != h.week }
        val capture = Capture()
        val breaks = Breaks()
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalPress provides Press.COLOUR) {
                StringerTheme {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp)) {
                        Box(Modifier.drawWithContent { capture.record(this) }) { Page(week, h, null, breaks) }
                    }
                }
            }
        }
        repeat(6) {
            rule.mainClock.advanceTimeBy(500)
            rule.waitForIdle()
            Thread.sleep(400)
        }
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        // The page is recorded as it's drawn: draw the window once, as the screen would.
        val shot = rule.runOnUiThread {
            val view = rule.activity.window.decorView
            view.draw(android.graphics.Canvas(android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)))
            capture.snapshot()
        }!!
        val k = shot.scale
        val density = app.resources.displayMetrics.density
        val files = Slides.save(app, shot.bitmap, h.issueNumber(week), breaks.ys().map { (it * k).roundToInt() }, (density * k).roundToInt().coerceAtLeast(1))
        out.listFiles()?.forEach { it.delete() }
        files.forEach { it.copyTo(File(out, it.name), overwrite = true) }
    }
}
