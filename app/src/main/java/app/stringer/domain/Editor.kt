package app.stringer.domain

import app.stringer.data.Assignment
import app.stringer.data.Catalogue
import app.stringer.data.Daypart
import app.stringer.data.Rubric
import app.stringer.data.Season
import app.stringer.data.Shot
import app.stringer.data.Weather
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.IsoFields
import kotlin.random.Random

/** The editor's rules: what to send, when it's evening, which week an issue is, and the ranks. */
object Editor {
    /** Rough sunrise and sunset by month at about 50–55° north, in hours. Good enough to tell a lamp-lit street from a sunny one. */
    private val SUNRISE = doubleArrayOf(8.8, 7.9, 6.9, 6.0, 5.0, 4.4, 4.8, 5.6, 6.6, 7.5, 8.4, 9.0)
    private val SUNSET = doubleArrayOf(16.4, 17.4, 18.4, 19.8, 20.7, 21.4, 21.2, 20.3, 19.0, 17.8, 16.4, 15.9)

    fun daypart(t: LocalDateTime): Daypart {
        val h = t.hour + t.minute / 60.0
        val m = t.monthValue - 1
        return when {
            h < SUNRISE[m] - 0.3 -> Daypart.DARK
            h < SUNSET[m] - 1.2 -> Daypart.DAY
            h < SUNSET[m] + 0.6 -> Daypart.DUSK
            else -> Daypart.DARK
        }
    }

    /** Minutes until sunset, if it's still ahead today. */
    fun toSunset(t: LocalDateTime): Int? {
        val h = t.hour + t.minute / 60.0
        val left = SUNSET[t.monthValue - 1] - h
        return if (left > 0) (left * 60).toInt() else null
    }

    fun season(t: LocalDateTime) = Season.of(t.monthValue)

    /**
     * The next telegram. Ordinary things first, in whatever rubric this week's issue is thinnest; the
     * weather's own tasks when the correspondent reports that weather; the evening's when it's evening.
     * Nothing already shot, nothing just swapped away, not the same kind of thing as the last few frames.
     * Once every fitting card has been used, the oldest ones come round again.
     */
    fun pick(
        catalogue: Catalogue,
        shots: List<Shot>,
        skipped: List<String>,
        weather: Weather?,
        at: LocalDateTime,
        daily: Boolean,
        random: Random,
        eveningOnly: Boolean = false,
    ): Assignment? {
        val season = season(at)
        val part = daypart(at)
        val fitting = catalogue.assignments.filter { a ->
            a.fits(season) && a.fits(part) && a.fits(weather) && (!eveningOnly || a.time == Daypart.DUSK || a.time == Daypart.DARK)
        }
        if (fitting.isEmpty()) return null
        val used = shots.mapNotNull { s -> s.assignment.takeIf { it.isNotEmpty() } }
        val usedSet = used.toSet()
        val recentThings = shots.filter { it.assignment.isNotEmpty() }.takeLast(6).mapNotNull { catalogue[it.assignment]?.thing }.toSet()
        // The weekly sends only the plain twists, until they run out: then the trickier ones rather than a repeat.
        val level = fitting.filter { daily || it.level == 1 }
        var fresh = level.filter { it.id !in usedSet && it.id !in skipped }
        if (fresh.isEmpty()) fresh = fitting.filter { it.id !in usedSet && it.id !in skipped }
        if (fresh.isEmpty()) fresh = fitting.filter { it.id !in usedSet }
        if (fresh.isEmpty()) {
            // The whole fitting stack is used: deal again, oldest first.
            val order = used.withIndex().associate { it.value to it.index }
            val oldest = fitting.sortedBy { order[it.id] ?: -1 }.take(maxOf(1, fitting.size / 4))
            return oldest[random.nextInt(oldest.size)]
        }
        val varied = fresh.filter { it.thing !in recentThings }.ifEmpty { fresh }

        // Weather and evening get their own tasks most of the time: they're the ones that can't wait.
        val special = varied.filter { (weather != null && it.weather != null) || (part != Daypart.DAY && (it.time == Daypart.DUSK || it.time == Daypart.DARK)) }
        if (special.isNotEmpty() && (eveningOnly || random.nextFloat() < 0.65f)) return special[random.nextInt(special.size)]
        val plain = varied.filter { it.weather == null }.ifEmpty { varied }

        // Fill the thinnest rubric of the week.
        val week = week(at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        val thisWeek = shots.filter { week(it.at) == week }.groupingBy { it.rubricOf }.eachCount()
        val weights = plain.map { a ->
            val n = thisWeek[a.rubric] ?: 0
            val w = 1.0 / (1 + n * 1.5) * (if (daily && a.level == 2) 1.4 else 1.0) * (if (a.seasons.isNotEmpty()) 1.6 else 1.0)
            a to w
        }
        var r = random.nextDouble() * weights.sumOf { it.second }
        for ((a, w) in weights) {
            r -= w
            if (r <= 0) return a
        }
        return plain.last()
    }

    /** A week as one number, 202643: the issue it goes into. */
    fun week(at: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val d = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
        return d.get(IsoFields.WEEK_BASED_YEAR) * 100L + d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    }

    /** Monday of an issue's week. */
    fun monday(week: Long): LocalDate =
        LocalDate.of((week / 100).toInt(), 6, 1).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week % 100).with(java.time.DayOfWeek.MONDAY)

    fun weekStart(week: Long, zone: ZoneId = ZoneId.systemDefault()): Long = monday(week).atStartOfDay(zone).toInstant().toEpochMilli()
    fun weekEnd(week: Long, zone: ZoneId = ZoneId.systemDefault()): Long = monday(week).plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()

    /** The paper's own numbering: the first week anything was filed is issue №1. */
    fun issueNumber(week: Long, first: Long): Int {
        val a = monday(first)
        val b = monday(week)
        return (java.time.temporal.ChronoUnit.WEEKS.between(a, b) + 1).toInt().coerceAtLeast(1)
    }

    /** Ranks on the press card, by frames published. */
    enum class Rank(val from: Int) { TRAINEE(0), STRINGER(10), REPORTER(40), PHOTOGRAPHER(100), COLUMNIST(250), CHIEF(500) }

    fun rank(published: Int): Rank = Rank.entries.last { published >= it.from }
    fun nextRank(published: Int): Rank? = Rank.entries.firstOrNull { it.from > published }

    /** Days in a row with at least one frame, counting back from today (or yesterday, if today is still empty). */
    fun streak(shots: List<Shot>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int {
        val days = shots.map { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() }.toSet()
        var d = if (today in days) today else today.minusDays(1)
        var n = 0
        while (d in days) { n++; d = d.minusDays(1) }
        return n
    }

    /** Which rubrics this week's issue still lacks. */
    fun missing(shots: List<Shot>, week: Long): List<Rubric> {
        val have = shots.filter { !it.inCase && week(it.at) == week }.map { it.rubricOf }.toSet()
        return Rubric.assigned.filter { it !in have }
    }
}
