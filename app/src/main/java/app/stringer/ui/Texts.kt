package app.stringer.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import app.stringer.R
import app.stringer.data.Rubric
import app.stringer.data.Shot
import app.stringer.data.Weather
import app.stringer.data.inflect
import app.stringer.domain.Case
import app.stringer.domain.Editor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Words and dates in the app's language. */
object T {
    private fun locale(c: Context): Locale = c.resources.configuration.locales[0]
    private fun zone() = ZoneId.systemDefault()

    fun plural(c: Context, id: Int, n: Int): String = c.resources.getQuantityString(id, n, n)

    /** "24 October", or with the year when it isn't this one. */
    fun date(c: Context, at: Long, today: LocalDate = LocalDate.now()): String {
        val d = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate()
        return d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "d MMMM" else "d MMMM yyyy", locale(c)))
    }

    /** "Sunday, 26 October 2026": the dateline of an issue. */
    fun dateline(c: Context, d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale(c))).replaceFirstChar { it.titlecase(locale(c)) }

    /** "20–26 October" for an issue's week. */
    fun span(c: Context, week: Long): String {
        val a = Editor.monday(week)
        val b = a.plusDays(6)
        val l = locale(c)
        return if (a.month == b.month) "${a.dayOfMonth}–${b.format(DateTimeFormatter.ofPattern("d MMMM", l))}"
        else "${a.format(DateTimeFormatter.ofPattern("d MMM", l))} – ${b.format(DateTimeFormatter.ofPattern("d MMM", l))}"
    }

    fun time(at: Long): String = Instant.ofEpochMilli(at).atZone(zone()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

    /** "today", "yesterday", "5 days ago", or the date for anything older than two weeks. */
    fun ago(c: Context, at: Long, today: LocalDate = LocalDate.now()): String {
        val d = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate()
        val days = java.time.temporal.ChronoUnit.DAYS.between(d, today).toInt()
        return when {
            days <= 0 -> c.getString(R.string.today)
            days == 1 -> c.getString(R.string.yesterday)
            days < 14 -> plural(c, R.plurals.days_ago, days)
            else -> date(c, at, today)
        }
    }

    fun rubric(c: Context, r: Rubric): String = c.getString(
        when (r) {
            Rubric.CHRONICLE -> R.string.r_chronicle
            Rubric.FAUNA -> R.string.r_fauna
            Rubric.WEATHER -> R.string.r_weather
            Rubric.GREEN -> R.string.r_green
            Rubric.TRANSPORT -> R.string.r_transport
            Rubric.LETTERS -> R.string.r_letters
            Rubric.SELF -> R.string.r_self
            Rubric.TEXTURE -> R.string.r_texture
            Rubric.FINDS -> R.string.r_finds
            Rubric.SHAPES -> R.string.r_shapes
            Rubric.READERS -> R.string.r_readers
        },
    )

    fun weather(c: Context, w: Weather?): String = c.getString(
        when (w) {
            null -> R.string.w_plain
            Weather.SUN -> R.string.w_sun
            Weather.RAIN -> R.string.w_rain
            Weather.WET -> R.string.w_wet
            Weather.SNOW -> R.string.w_snow
            Weather.FROST -> R.string.w_frost
            Weather.FOG -> R.string.w_fog
            Weather.WIND -> R.string.w_wind
        },
    )

    fun rank(c: Context, r: Editor.Rank, feminine: Boolean): String = inflect(
        c.getString(
            when (r) {
                Editor.Rank.TRAINEE -> R.string.rank_trainee
                Editor.Rank.STRINGER -> R.string.rank_stringer
                Editor.Rank.REPORTER -> R.string.rank_reporter
                Editor.Rank.PHOTOGRAPHER -> R.string.rank_photographer
                Editor.Rank.COLUMNIST -> R.string.rank_columnist
                Editor.Rank.CHIEF -> R.string.rank_chief
            },
        ),
        feminine,
    )

    fun case(c: Context, k: Case): String = c.getString(
        when (k) {
            Case.ALPHABET -> R.string.case_alphabet
            Case.DIGITS -> R.string.case_digits
            Case.DOORS -> R.string.case_doors
            Case.CATS -> R.string.case_cats
        },
    )

    fun caseBrief(c: Context, k: Case): String = c.getString(
        when (k) {
            Case.ALPHABET -> R.string.case_alphabet_b
            Case.DIGITS -> R.string.case_digits_b
            Case.DOORS -> R.string.case_doors_b
            Case.CATS -> R.string.case_cats_b
        },
    )

    /** The masthead: the correspondent's own name for the paper, or the default. */
    fun paper(c: Context, name: String): String = name.ifBlank { c.getString(R.string.paper_default) }

    /**
     * The editor's line in the margin for a filed frame: the same line for the same frame every time,
     * picked from what happened: the first ever, a fallback, at night, straight away, or much later.
     */
    fun remark(c: Context, shot: Shot, telegramAt: Long, count: Int, feminine: Boolean): String {
        val hour = Instant.ofEpochMilli(shot.at).atZone(zone()).hour
        val pool = when {
            count <= 1 -> R.array.remark_first
            shot.inCase -> R.array.remark_case
            shot.fallback -> R.array.remark_fallback
            telegramAt > 0 && shot.at - telegramAt < 20 * 60_000L -> R.array.remark_quick
            telegramAt > 0 && shot.at - telegramAt > 6 * 3_600_000L -> R.array.remark_late
            hour >= 21 || hour < 6 -> R.array.remark_night
            Weather.code(shot.weather) in setOf(Weather.RAIN, Weather.SNOW, Weather.FOG) -> R.array.remark_weather
            else -> R.array.remark_plain
        }
        val lines = c.resources.getStringArray(pool)
        return inflect(lines[((shot.id * 31 + shot.at / 1000) % lines.size).toInt().let { if (it < 0) it + lines.size else it }], feminine)
    }

    /** Old-fashioned small ads that fill an issue's empty columns. */
    fun ads(c: Context): List<String> = c.resources.getStringArray(R.array.ads).toList()
}

object Language {
    const val SYSTEM = ""
    const val EN = "en"
    const val RU = "ru"

    fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')

    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(if (tag == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }
}

@Composable
fun ctx(): Context = LocalContext.current
