package app.stringer.data

import android.content.Context
import app.stringer.domain.Editor
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

/** Everything that changes the paper: telegrams sent and swapped, frames filed, moved and binned. */
class Newsroom(private val context: Context, private val db: StringerDatabase, private val settings: SettingsStore, val catalogue: Catalogue) {
    private val dao get() = db.shots()

    /** "I'm going out": the editor sends a telegram for the weather reported. */
    suspend fun telegram(weather: Weather?, at: LocalDateTime = LocalDateTime.now(), random: Random = Random.Default): Assignment? {
        val s = settings.now()
        val a = Editor.pick(catalogue, dao.all(), s.skipped, weather, at, s.daily, random) ?: return null
        settings.update { it.copy(telegram = Telegram(a.id, System.currentTimeMillis(), weather?.code.orEmpty(), 0)) }
        return a
    }

    /** An urgent flash for the evening light. Once a day at most. */
    suspend fun flash(at: LocalDateTime = LocalDateTime.now(), random: Random = Random.Default): Assignment? {
        val s = settings.now()
        val day = LocalDate.now().toEpochDay()
        if (s.flashDay == day) return null
        val a = Editor.pick(catalogue, dao.all(), s.skipped, Weather.code(s.telegram.weather), at, s.daily, random, eveningOnly = true) ?: return null
        settings.update { it.copy(telegram = Telegram(a.id, System.currentTimeMillis(), it.telegram.weather, 0, flash = true), flashDay = day) }
        return a
    }

    /** One swap per telegram: the editor grumbles and sends another. */
    suspend fun swap(at: LocalDateTime = LocalDateTime.now(), random: Random = Random.Default): Assignment? {
        val s = settings.now()
        val t = s.telegram
        if (!t.open || t.swaps >= SWAPS) return null
        val skipped = s.skipped + t.id
        val a = Editor.pick(catalogue, dao.all(), skipped, Weather.code(t.weather), at, s.daily, random, eveningOnly = t.flash) ?: return null
        settings.update { it.copy(telegram = t.copy(id = a.id, swaps = t.swaps + 1), skipped = skipped) }
        return a
    }

    /** Back home without a frame: the telegram is put away, no harm done. */
    suspend fun dismiss() = settings.update { it.copy(telegram = Telegram()) }

    /** A frame for the telegram in hand goes to the paper, and the telegram is closed. */
    suspend fun file(photo: String, caption: String, fallback: Boolean): Long {
        val s = settings.now()
        val a = catalogue[s.telegram.id]
        val id = dao.insert(
            Shot(
                assignment = a?.id.orEmpty(), rubric = (a?.rubric ?: Rubric.READERS).name, photo = photo,
                caption = caption.trim(), fallback = fallback, weather = s.telegram.weather,
            ),
        )
        settings.update { it.copy(telegram = Telegram()) }
        return id
    }

    /** A letter to the editor: any frame, no telegram needed. */
    suspend fun letter(photo: String, caption: String): Long = dao.insert(Shot(rubric = Rubric.READERS.name, photo = photo, caption = caption.trim()))

    /**
     * The correspondent's own article: a headline, the text, and a photo if there is one. With [id]
     * the article is rewritten in place; a photo it no longer uses goes to the bin.
     */
    suspend fun article(id: Long?, headline: String, body: String, photo: String): Long {
        val old = id?.let { dao.get(it) }
        if (old == null) return dao.insert(Shot(rubric = Rubric.READERS.name, photo = photo, caption = headline.trim(), body = body.trim()))
        dao.update(old.copy(photo = photo, caption = headline.trim(), body = body.trim()))
        if (old.photo != photo) Photos.delete(context, old.photo)
        return old.id
    }

    /** A frame for an investigation's slot; a frame already there is replaced. */
    suspend fun fileCase(case: String, slot: String, photo: String, caption: String): Long {
        dao.all().filter { it.case == case && it.slot == slot }.forEach { bin(it.id) }
        return dao.insert(Shot(case = case, slot = slot, photo = photo, caption = caption.trim()))
    }

    suspend fun caption(id: Long, caption: String) {
        val s = dao.get(id) ?: return
        dao.update(s.copy(caption = caption.trim()))
    }

    /** This one frame goes in without the screen, or back under it. */
    suspend fun plain(id: Long, on: Boolean) {
        val s = dao.get(id) ?: return
        dao.update(s.copy(plain = on))
    }

    /** Onto the front page of its week, taking the place of whatever was there. */
    suspend fun front(id: Long) {
        val s = dao.get(id) ?: return
        val week = Editor.week(s.at)
        dao.clearFront(Editor.weekStart(week), Editor.weekEnd(week))
        dao.update(s.copy(front = true))
    }

    /** Into the bin, with its photo. */
    suspend fun bin(id: Long) {
        val s = dao.get(id) ?: return
        dao.delete(id)
        Photos.delete(context, s.photo)
    }

    companion object {
        const val SWAPS = 1
    }
}

object Photos {
    fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }
    fun file(context: Context, name: String) = File(dir(context), name)
    fun fresh(context: Context): File = file(context, "s-${System.currentTimeMillis()}.jpg")

    fun delete(context: Context, name: String?) {
        if (name.isNullOrBlank()) return
        file(context, name).delete()
        File(context.cacheDir, "halftone").listFiles { f -> f.name.startsWith(name) }?.forEach { it.delete() }
    }
}

/** For tests and the demo: a wall-clock moment in the phone's zone. */
fun LocalDateTime.millis(zone: ZoneId = ZoneId.systemDefault()): Long = atZone(zone).toInstant().toEpochMilli()
