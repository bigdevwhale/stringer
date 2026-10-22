package app.stringer.data

import android.content.Context

/**
 * The paper's rubrics. Every assignment belongs to one, and the Sunday issue is laid out by them.
 * [READERS] is the free frame: anything the correspondent saw, sent in as a letter to the editor.
 */
enum class Rubric(val file: String) {
    CHRONICLE("chronicle"),
    FAUNA("fauna"),
    WEATHER("weather"),
    GREEN("green"),
    TRANSPORT("transport"),
    LETTERS("letters"),
    SELF("self"),
    TEXTURE("texture"),
    FINDS("finds"),
    SHAPES("shapes"),
    READERS("readers");

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s || it.file == s } ?: READERS

        /** The rubrics the editor sends assignments for. */
        val assigned = entries.filter { it != READERS }
    }
}

enum class Season { WINTER, SPRING, SUMMER, AUTUMN;
    companion object {
        fun code(s: String) = when (s) {
            "w" -> WINTER
            "sp" -> SPRING
            "su" -> SUMMER
            "au" -> AUTUMN
            else -> null
        }

        /** Northern-hemisphere seasons by month. */
        fun of(month: Int) = when (month) {
            12, 1, 2 -> WINTER
            3, 4, 5 -> SPRING
            6, 7, 8 -> SUMMER
            else -> AUTUMN
        }
    }
}

/** What it's like outside, as the correspondent reports it before going out. [WET] is "it rained, it's stopped". */
enum class Weather(val code: String) {
    SUN("sun"), RAIN("rain"), WET("wet"), SNOW("snow"), FROST("frost"), FOG("fog"), WIND("wind");

    companion object {
        fun code(s: String?) = entries.firstOrNull { it.code == s }
    }
}

/** Light: the plain day, the evening glow, and after dark. */
enum class Daypart(val code: String) {
    DAY("day"), DUSK("dusk"), DARK("dark");

    companion object {
        fun code(s: String?) = entries.firstOrNull { it.code == s }
    }
}

/**
 * One assignment: an ordinary thing with a twist. [seasons] empty means any time of year; [weather]
 * and [time] null mean any. [level] 2 is for the big-city daily. [thing] links to the fallback.
 */
data class Assignment(
    val id: String,
    val rubric: Rubric,
    val thing: String,
    val level: Int,
    val seasons: Set<Season>,
    val weather: Weather?,
    val time: Daypart?,
    val en: String,
    val ru: String,
) {
    /** The task in the paper's language; Russian carries {masc|fem} for the correspondent. */
    fun text(ru: Boolean, feminine: Boolean) = if (ru) inflect(this.ru, feminine) else en

    /** Weather tasks only on their weather. "Wet" is any time after rain, rain itself included. */
    fun fits(w: Weather?): Boolean = when (weather) {
        null -> true
        Weather.WET -> w == Weather.WET || w == Weather.RAIN
        Weather.SUN -> w == Weather.SUN || w == Weather.FROST
        else -> w == weather
    }

    /** Evening tasks need the evening; lamps and lit windows work from dusk on. Untimed ones go any time. */
    fun fits(d: Daypart): Boolean = when (time) {
        null -> true
        Daypart.DAY -> d != Daypart.DARK
        Daypart.DUSK -> d == Daypart.DUSK
        Daypart.DARK -> d != Daypart.DAY
    }

    fun fits(s: Season) = seasons.isEmpty() || s in seasons
}

/** "Not there? Then any door." [en] and [ru] are blank for the abstract tasks: any frame counts. */
data class Fallback(val en: String, val ru: String) {
    fun text(ru: Boolean) = if (ru) this.ru else en
    val blank get() = en.isBlank()
}

/** Picks one side of every {masc|fem} in a Russian line. */
fun inflect(s: String, feminine: Boolean): String =
    Regex("""\{([^|{}]*)\|([^|{}]*)\}""").replace(s) { if (feminine) it.groupValues[2] else it.groupValues[1] }

/** The editor's card file, read once from assets: 1271 assignments, three years of walks. */
class Catalogue(val assignments: List<Assignment>, private val fallbacks: Map<String, Fallback>) {
    private val byId = assignments.associateBy { it.id }
    operator fun get(id: String): Assignment? = byId[id]
    fun fallback(thing: String): Fallback = fallbacks[thing] ?: Fallback("", "")

    companion object {
        fun load(context: Context): Catalogue = parse(
            context.assets.open("assignments.tsv").bufferedReader().use { it.readText() },
            context.assets.open("things.tsv").bufferedReader().use { it.readText() },
        )

        fun parse(tsv: String, things: String): Catalogue = Catalogue(
            tsv.lineSequence().drop(1).filter { it.isNotBlank() }.map { line ->
                val f = line.split('\t')
                Assignment(
                    id = f[0], rubric = Rubric.of(f[1]), thing = f[2], level = f[3].toInt(),
                    seasons = f[4].split(',').mapNotNull { Season.code(it.trim()) }.toSet(),
                    weather = Weather.code(f[5]), time = Daypart.code(f[6]), en = f[7], ru = f[8],
                )
            }.toList(),
            things.lineSequence().drop(1).filter { it.isNotBlank() }.associate { line ->
                val f = line.split('\t')
                f[0] to Fallback(f.getOrElse(1) { "" }, f.getOrElse(2) { "" })
            },
        )
    }
}
