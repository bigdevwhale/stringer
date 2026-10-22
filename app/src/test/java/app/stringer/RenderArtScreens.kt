package app.stringer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.stringer.data.DemoSeed
import app.stringer.domain.Case
import app.stringer.ui.Go
import app.stringer.ui.Home
import app.stringer.ui.archive.ArchiveScreen
import app.stringer.ui.card.CardScreen
import app.stringer.ui.cases.CaseScreen
import app.stringer.ui.cases.CasesScreen
import app.stringer.ui.desk.DeskScreen
import app.stringer.ui.develop.DevelopScreen
import app.stringer.ui.issue.IssueScreen
import app.stringer.ui.onboarding.WelcomeScreen
import app.stringer.ui.shot.ShotScreen
import app.stringer.ui.telegram.TelegramScreen
import app.stringer.ui.theme.S
import app.stringer.ui.theme.StringerTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo newsroom, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    private fun home(): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<StringerApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(app, c)
        Home(c.db.shots().all(), c.settings.now(), c.catalogue, System.currentTimeMillis(), app.ru)
    }

    private fun shot(name: String, h: Home, content: @Composable (Home, Go) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val nav = rememberNavController()
            StringerTheme { Box(Modifier.fillMaxSize().background(S.c.bg)) { content(h, Go(nav)) } }
        }
        // Halftones are screened off the main thread: let them land.
        repeat(6) {
            rule.mainClock.advanceTimeBy(500)
            rule.waitForIdle()
            Thread.sleep(400)
        }
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun lastWeek(h: Home) = h.issues.first { it != h.week }

    @Test fun welcome() = home().let { shot("0-welcome", it) { _, _ -> WelcomeScreen {} } }
    @Test fun desk() = home().let { shot("1-desk", it) { h, go -> DeskScreen(h, go) } }
    @Test fun telegram() = home().let { shot("2-telegram", it) { h, go -> TelegramScreen(h, go) } }
    @Test fun develop() = home().let { shot("3-proof", it) { h, go -> DevelopScreen("d-p146.jpg", "tg", "", "", h, go) } }
    @Test fun filed() = home().let { h -> shot("4-filed", h) { hh, go -> ShotScreen(hh.shots.first { it.photo == "d-p76.jpg" }.id, hh, go) } }
    @Test fun issue() = home().let { h -> shot("5-issue", h) { hh, go -> IssueScreen(lastWeek(hh), hh, go) } }
    @Test fun archive() = home().let { shot("6-archive", it) { h, go -> ArchiveScreen(h, go) } }
    @Test fun cases() = home().let { shot("7-cases", it) { h, go -> CasesScreen(h, go) } }
    @Test fun case() = home().let { shot("8-doors", it) { h, go -> CaseScreen(Case.DOORS, h, go) } }
    @Test fun card() = home().let { shot("9-card", it) { h, go -> CardScreen(h, go) } }
}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
