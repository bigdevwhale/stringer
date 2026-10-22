package app.stringer.ui

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.stringer.container
import app.stringer.data.Assignment
import app.stringer.data.Catalogue
import app.stringer.data.Settings
import app.stringer.data.Shot
import app.stringer.domain.Case
import app.stringer.domain.Editor
import app.stringer.ru
import app.stringer.ui.archive.ArchiveScreen
import app.stringer.ui.card.CardScreen
import app.stringer.ui.cases.CaseScreen
import app.stringer.ui.cases.CasesScreen
import app.stringer.ui.components.rememberNow
import app.stringer.ui.desk.DeskScreen
import app.stringer.ui.develop.DevelopScreen
import app.stringer.ui.issue.IssueScreen
import app.stringer.ui.onboarding.WelcomeScreen
import app.stringer.ui.settings.SettingsScreen
import app.stringer.ui.shot.ShotScreen
import app.stringer.ui.telegram.TelegramScreen
import app.stringer.ui.theme.S
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Everything filed, plus what follows from it, at one moment. Screens read through here. */
@Immutable
class Home(
    val shots: List<Shot>,
    val settings: Settings,
    val catalogue: Catalogue,
    val now: Long,
    val ru: Boolean,
) {
    val zone: ZoneId = ZoneId.systemDefault()
    val today: LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val clock: LocalDateTime = Instant.ofEpochMilli(now).atZone(zone).toLocalDateTime()
    val feminine get() = settings.feminine

    fun shot(id: Long) = shots.firstOrNull { it.id == id }
    fun assignment(s: Shot): Assignment? = catalogue[s.assignment]

    /** The task line as printed under a frame: the assignment, or the rubric for a free frame. */
    fun task(s: Shot): String = assignment(s)?.text(ru, feminine).orEmpty()

    /** The telegram in hand, if any. */
    val telegram: Assignment? = catalogue[settings.telegram.id]

    /** Frames for the weekly issues: everything but investigations. */
    val published: List<Shot> = shots.filter { !it.inCase }

    val week: Long = Editor.week(now)
    val firstWeek: Long = shots.minOfOrNull { Editor.week(it.at) } ?: week
    fun issueNumber(w: Long) = Editor.issueNumber(w, firstWeek)

    fun inWeek(w: Long): List<Shot> = published.filter { Editor.week(it.at) == w }

    /** Weeks with something in them, the newest first: the bound volume. */
    val issues: List<Long> = published.map { Editor.week(it.at) }.distinct().sortedDescending()

    val rank = Editor.rank(shots.size)
    val nextRank = Editor.nextRank(shots.size)
    val streak = Editor.streak(shots, today, zone)

    val daypart = Editor.daypart(clock)
    val toSunset = Editor.toSunset(clock)

    /** The evening flash is due: dusk now, nothing in hand, none sent today. */
    val flashDue: Boolean = daypart == app.stringer.data.Daypart.DUSK && !settings.telegram.open && settings.flashDay != today.toEpochDay()

    fun case(k: Case) = k.filed(shots)
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun telegram() = nav.navigate("telegram") { launchSingleTop = true }
    fun issue(week: Long) = nav.navigate("issue/$week") { launchSingleTop = true }
    fun archive() = nav.navigate("archive") { launchSingleTop = true }
    fun cases() = nav.navigate("cases") { launchSingleTop = true }
    fun case(k: Case) = nav.navigate("case/${k.key}") { launchSingleTop = true }
    fun card() = nav.navigate("card") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun shot(id: Long) = nav.navigate("shot/$id") { launchSingleTop = true }
    fun back() { nav.popBackStack() }

    /** A frame just taken, to be captioned: for the telegram, a letter, or an investigation slot. */
    fun develop(photo: String, mode: String, case: String = "", slot: String = "") =
        nav.navigate("develop/$photo?mode=$mode&case=$case&slot=${Uri.encode(slot)}") { launchSingleTop = true }

    /** Filed: show it with the editor's mark, the desk underneath. */
    fun filed(id: Long) = nav.navigate("shot/$id") { popUpTo("desk") }

    fun desk() { nav.popBackStack("desk", false) }
}

@Composable
fun StringerRoot(settings: Settings) {
    val context = ctx()
    val container = context.container
    val nav = rememberNavController()
    val start = remember { if (settings.onboarded) "desk" else "welcome" }

    val shots by container.db.shots().observe().collectAsStateWithLifecycle(initialValue = emptyList())
    val now = rememberNow(60_000)
    val ru = context.ru
    val home = remember(shots, settings, now, ru) { Home(shots, settings, container.catalogue, now, ru) }
    val go = remember(nav) { Go(nav) }

    // Onboarded elsewhere (the demo seed): leave the welcome screen.
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("desk") { popUpTo("welcome") { inclusive = true }; launchSingleTop = true }
    }

    Box(Modifier.fillMaxSize().background(S.c.bg)) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(S.c.bg),
            enterTransition = { fadeIn(tween(260)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable("welcome") { WelcomeScreen { nav.navigate("desk") { popUpTo("welcome") { inclusive = true } } } }
            composable("desk") { DeskScreen(home, go) }
            composable("telegram") { TelegramScreen(home, go) }
            composable(
                "develop/{photo}?mode={mode}&case={case}&slot={slot}",
                arguments = listOf(
                    navArgument("photo") { type = NavType.StringType },
                    navArgument("mode") { type = NavType.StringType; defaultValue = "tg" },
                    navArgument("case") { type = NavType.StringType; defaultValue = "" },
                    navArgument("slot") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { e ->
                val a = e.arguments
                DevelopScreen(a?.getString("photo").orEmpty(), a?.getString("mode") ?: "tg", a?.getString("case").orEmpty(), a?.getString("slot").orEmpty(), home, go)
            }
            composable("shot/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e -> ShotScreen(e.arguments?.getLong("id") ?: 0L, home, go) }
            composable("issue/{week}", arguments = listOf(navArgument("week") { type = NavType.LongType })) { e -> IssueScreen(e.arguments?.getLong("week") ?: home.week, home, go) }
            composable("archive") { ArchiveScreen(home, go) }
            composable("cases") { CasesScreen(home, go) }
            composable("case/{key}", arguments = listOf(navArgument("key") { type = NavType.StringType })) { e ->
                Case.of(e.arguments?.getString("key").orEmpty())?.let { CaseScreen(it, home, go) }
            }
            composable("card") { CardScreen(home, go) }
            composable("settings") { SettingsScreen(home, go) }
        }
    }
}
