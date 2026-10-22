package app.stringer.data

import android.content.Context
import app.stringer.AppContainer
import app.stringer.ru
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * The demo build's newsroom: four weeks of a correspondent's frames on stock photos, an open
 * investigation and a telegram in hand. Only for `-Pdemo`, screenshots and renders.
 */
object DemoSeed {
    /** Picsum id, assignment, days back from today, hour, caption in English and Russian, weather, front page. */
    private class Frame(val photo: String, val task: String, val daysAgo: Int, val hour: Int, val en: String, val ru: String, val weather: String = "", val front: Boolean = false, val fallback: Boolean = false)

    private val frames = listOf(
        Frame("p237", "sh103", 24, 10, "Seen from above, he didn't mind at all.", "Сверху он был совсем не против.", "sun"),
        Frame("p785", "se019", 23, 14, "Spruce tips, picked up by the garages.", "Еловые почки, подобрала у гаражей."),
        Frame("p175", "le022", 22, 18, "The courtyard clock says it's always seven.", "Часы во дворе уверяют, что всегда семь.", front = true),
        Frame("p129", "ch083", 18, 17, "Two on the bench by the river, every evening.", "Двое на скамейке у реки, каждый вечер.", "sun", front = true),
        Frame("p146", "tr111", 17, 9, "Someone's tricycle chained up like a grown-up bike.", "Чей-то трёхколёсный прикован, как взрослый велосипед."),
        Frame("p301", "ch197", 16, 13, "The shortcut everyone pretends isn't there.", "Тропинка, которой «нет», но ходят все.", "wet", fallback = true),
        Frame("p195", "we152", 15, 21, "Lanes of bulbs over the back street.", "Гирлянды над переулком за рынком."),
        Frame("p63", "sh001", 14, 8, "The reddest thing this morning was a coffee.", "Самым красным этим утром оказался кофе."),
        Frame("p76", "ch011", 11, 12, "Door and bike, both waiting for their owner.", "Дверь и велосипед — оба ждут хозяина.", "sun", front = true),
        Frame("p169", "fa082", 10, 16, "Two dachshunds, one very important smell.", "Две таксы и один очень важный запах."),
        Frame("p152", "gr103", 10, 11, "Petunias, the last ones on Lipová street.", "Петунии, последние на Липовой."),
        Frame("p99", "tr011", 9, 15, "An old cart wheel by the museum fence.", "Старое тележное колесо у музейного забора.", fallback = true),
        Frame("p56", "we145", 8, 22, "The tram stop, out of focus on purpose.", "Остановка трамвая, нарочно не в фокусе.", "rain"),
        Frame("p815", "se025", 7, 19, "Framed the sunset with two hands.", "Поймала закат в ладони.", "sun"),
        Frame("p167", "gr069", 3, 10, "The maples gave up overnight.", "Клёны сдались за одну ночь.", "wind", front = true),
        Frame("p54", "we027", 2, 13, "The biggest cloud over the district today.", "Самое большое облако над районом сегодня.", "sun"),
        Frame("p755", "se044", 1, 9, "Coffee on the bench before the walk.", "Кофе на скамейке перед прогулкой.", "frost"),
        Frame("p103", "se004", 0, 11, "Shoes, grass, and nowhere to hurry.", "Кеды, трава и некуда спешить.", "sun"),
    )

    suspend fun seedIfEmpty(context: Context, c: AppContainer) {
        val dao = c.db.shots()
        if (dao.all().isNotEmpty()) return
        val ru = context.ru
        val today = LocalDate.now()
        frames.forEach { f ->
            val at = today.minusDays(f.daysAgo.toLong()).atTime(LocalTime.of(f.hour, 14)).millis()
            val a = c.catalogue[f.task]
            dao.insert(
                Shot(
                    assignment = f.task, rubric = (a?.rubric ?: Rubric.READERS).name, at = at, photo = copy(context, f.photo, "d-${f.photo}.jpg"),
                    caption = if (ru) f.ru else f.en, fallback = f.fallback, front = f.front, weather = f.weather,
                ),
            )
        }
        // An investigation under way: the first doors of the district and a few digits.
        val doorAt = today.minusDays(11).atTime(12, 20).millis()
        dao.insert(Shot(case = "doors", slot = "1", at = doorAt, photo = copy(context, "p76", "c-door1.jpg"), caption = if (ru) "Синяя дверь у сараев" else "The blue door by the sheds"))
        dao.insert(Shot(case = "digits", slot = "7", at = doorAt + 3_600_000, photo = copy(context, "p175", "c-digit7.jpg"), caption = if (ru) "Семёрка на часах" else "A seven on the clock"))
        c.settings.update {
            it.copy(
                onboarded = true,
                name = if (ru) "Саша Ветрова" else "Sasha Vetrova",
                feminine = true,
                telegram = Telegram(id = "ch003", at = System.currentTimeMillis() - 25 * 60_000L, weather = "sun"),
            )
        }
    }

    /** A demo asset copied into the photos folder under its own name, so binning one frame never takes another's photo. */
    private fun copy(context: Context, asset: String, name: String): String {
        val out = Photos.file(context, name)
        if (!out.exists()) {
            runCatching {
                context.assets.open("demo/$asset.jpg").use { i -> out.outputStream().use { i.copyTo(it) } }
            }.onFailure {
                // Renders run without the demo assets packed; they read the photos from the source folder.
                System.getProperty("render.photos")?.let { dir -> File(dir, "$asset.jpg").takeIf { f -> f.exists() }?.copyTo(out, overwrite = true) }
            }
        }
        return name
    }
}
