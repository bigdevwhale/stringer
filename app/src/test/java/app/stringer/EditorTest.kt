package app.stringer

import app.stringer.data.Catalogue
import app.stringer.data.Daypart
import app.stringer.data.Rubric
import app.stringer.data.Season
import app.stringer.data.Shot
import app.stringer.data.Weather
import app.stringer.data.inflect
import app.stringer.data.millis
import app.stringer.domain.Case
import app.stringer.domain.Editor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

class EditorTest {
    private val catalogue = Catalogue.parse(File("src/main/assets/assignments.tsv").readText(), File("src/main/assets/things.tsv").readText())

    @Test fun catalogueIsWhole() {
        assertTrue("at least three years of walks", catalogue.assignments.size >= 3 * 365)
        assertEquals(catalogue.assignments.size, catalogue.assignments.map { it.id }.toSet().size)
        catalogue.assignments.forEach { a ->
            assertTrue("${a.id} has both languages", a.en.isNotBlank() && a.ru.isNotBlank())
            assertTrue("${a.id} is in a rubric the editor sends", a.rubric in Rubric.assigned)
            assertTrue("${a.id} has no stray braces", !inflect(a.ru, true).contains('{') && !inflect(a.ru, false).contains('|'))
            assertNotNull("${a.id} thing ${a.thing} has a fallback row", catalogue.fallback(a.thing))
        }
    }

    @Test fun inflectsBothWays() {
        assertEquals("где бы ты ни был", inflect("где бы ты ни {был|была}", false))
        assertEquals("где бы ты ни была", inflect("где бы ты ни {был|была}", true))
        assertEquals("Не торопилась.", inflect("Не торопил{ся|ась}.", true))
    }

    @Test fun eveningAndSeasons() {
        assertEquals(Daypart.DAY, Editor.daypart(LocalDateTime.of(2026, 10, 22, 12, 0)))
        assertEquals(Daypart.DUSK, Editor.daypart(LocalDateTime.of(2026, 10, 22, 17, 30)))
        assertEquals(Daypart.DARK, Editor.daypart(LocalDateTime.of(2026, 10, 22, 21, 0)))
        assertEquals(Daypart.DARK, Editor.daypart(LocalDateTime.of(2026, 12, 22, 7, 0)))
        assertEquals(Season.WINTER, Editor.season(LocalDateTime.of(2027, 1, 5, 12, 0)))
        // Snow is never sent in July, rain-only tasks never without rain.
        val july = LocalDateTime.of(2026, 7, 10, 12, 0)
        repeat(200) { i ->
            val a = Editor.pick(catalogue, emptyList(), emptyList(), null, july, true, Random(i))!!
            assertTrue(a.id, a.fits(Season.SUMMER) && a.weather == null)
        }
    }

    /**
     * Three years of daily walks, each at some hour of the day, in whatever weather the season brings:
     * not a single assignment comes twice, in either kind of paper.
     */
    @Test fun threeYearsWithoutRepeats() {
        for (daily in listOf(false, true)) {
            val r = Random(if (daily) 7 else 3)
            val shots = mutableListOf<Shot>()
            var day = LocalDate.of(2026, 10, 22)
            repeat(3 * 365 + 1) {
                val at = day.atTime(8 + r.nextInt(14), r.nextInt(60))
                val a = Editor.pick(catalogue, shots, emptyList(), weatherFor(Editor.season(at), r), at, daily, r)
                assertNotNull("a telegram on $day", a)
                shots += Shot(id = shots.size + 1L, assignment = a!!.id, rubric = a.rubric.name, at = at.millis(), photo = "x")
                day = day.plusDays(1)
            }
            val ids = shots.map { it.assignment }
            assertEquals("daily=$daily: ${ids.size - ids.toSet().size} repeats in three years", ids.size, ids.toSet().size)
        }
    }

    @Test fun thinRubricsComeFirst() {
        val at = LocalDateTime.of(2026, 10, 21, 12, 0)
        val week = at.millis()
        // A week full of doors and pigeons: the next telegram should rarely be either.
        val busy = (1..8).map { Shot(id = it.toLong(), assignment = "", rubric = if (it % 2 == 0) Rubric.CHRONICLE.name else Rubric.FAUNA.name, at = week, photo = "x") }
        val picks = (0 until 300).map { Editor.pick(catalogue, busy, emptyList(), null, at, false, Random(it))!!.rubric }
        val share = picks.count { it == Rubric.CHRONICLE || it == Rubric.FAUNA } / picks.size.toFloat()
        assertTrue("busy rubrics took $share of telegrams", share < 0.12f)
    }

    @Test fun weeksAndRanks() {
        val w = Editor.week(LocalDateTime.of(2026, 10, 22, 12, 0).millis())
        assertEquals(202643L, w)
        assertEquals(LocalDate.of(2026, 10, 19), Editor.monday(w))
        assertEquals(3, Editor.issueNumber(w, 202641L))
        assertEquals(1, Editor.issueNumber(202701L, 202701L))
        assertEquals(54, Editor.issueNumber(202701L, 202601L)) // 2026 has 53 ISO weeks
        assertEquals(Editor.Rank.TRAINEE, Editor.rank(0))
        assertEquals(Editor.Rank.STRINGER, Editor.rank(10))
        assertEquals(Editor.Rank.CHIEF, Editor.rank(900))
    }

    @Test fun casesHaveTheirSlots() {
        assertEquals(33, Case.ALPHABET.slots(true).size)
        assertEquals(26, Case.ALPHABET.slots(false).size)
        assertEquals(10, Case.DIGITS.slots(true).size)
        val shots = Case.DIGITS.slots(false).mapIndexed { i, s -> Shot(id = i + 1L, case = "digits", slot = s, photo = "x") }
        assertTrue(Case.DIGITS.done(shots, false))
    }

    private fun weatherFor(s: Season, r: Random): Weather? {
        val x = r.nextInt(100)
        return when (s) {
            Season.WINTER -> when { x < 40 -> null; x < 58 -> Weather.SNOW; x < 72 -> Weather.FROST; x < 82 -> Weather.SUN; x < 90 -> Weather.FOG; else -> Weather.WIND }
            Season.SPRING -> when { x < 40 -> null; x < 60 -> Weather.SUN; x < 75 -> Weather.RAIN; x < 88 -> Weather.WET; else -> Weather.WIND }
            Season.SUMMER -> when { x < 35 -> null; x < 70 -> Weather.SUN; x < 82 -> Weather.RAIN; x < 92 -> Weather.WET; else -> Weather.WIND }
            Season.AUTUMN -> when { x < 35 -> null; x < 52 -> Weather.RAIN; x < 66 -> Weather.WET; x < 78 -> Weather.SUN; x < 88 -> Weather.FOG; else -> Weather.WIND }
        }
    }
}
