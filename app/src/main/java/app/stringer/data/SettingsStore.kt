package app.stringer.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

/** The telegram in hand: which assignment, when it came, the weather reported, and swaps used. */
data class Telegram(
    val id: String = "",
    val at: Long = 0,
    val weather: String = "",
    val swaps: Int = 0,
    /** Sent as an urgent "flash" for the evening light, not the walk's own task. */
    val flash: Boolean = false,
) {
    val open: Boolean get() = id.isNotEmpty()
}

/** The press: black dots on newsprint, a four-colour screen, or the photo printed as it was shot. */
enum class Press {
    MONO, COLOUR, PHOTO;

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: MONO
    }
}

data class Settings(
    val onboarded: Boolean = false,
    /** The paper's masthead. Blank: the default name in the app's language. */
    val paper: String = "",
    /** The correspondent's byline. */
    val name: String = "",
    /** Russian assignments and the editor speak to the correspondent as a woman. */
    val feminine: Boolean = false,
    /** The big-city daily: trickier twists mixed in. Off: the district weekly, the plain ones. */
    val daily: Boolean = false,
    /** How the photos go into print: the black screen, four colour plates, or as shot. */
    val press: Press = Press.MONO,
    val telegram: Telegram = Telegram(),
    /** Assignments swapped away lately, kept out of the next few telegrams. */
    val skipped: List<String> = emptyList(),
    /** The day (since the epoch) an evening flash was last sent. */
    val flashDay: Long = 0,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        paper = this[PAPER] ?: "",
        name = this[NAME] ?: "",
        feminine = this[FEMININE] ?: false,
        daily = this[DAILY] ?: false,
        press = Press.of(this[PRESS]),
        telegram = Telegram(
            id = this[TG_ID] ?: "",
            at = this[TG_AT] ?: 0,
            weather = this[TG_WEATHER] ?: "",
            swaps = this[TG_SWAPS] ?: 0,
            flash = this[TG_FLASH] ?: false,
        ),
        skipped = (this[SKIPPED] ?: "").split(',').filter { it.isNotBlank() },
        flashDay = this[FLASH_DAY] ?: 0,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[PAPER] = s.paper
            it[NAME] = s.name
            it[FEMININE] = s.feminine
            it[DAILY] = s.daily
            it[PRESS] = s.press.name
            it[TG_ID] = s.telegram.id
            it[TG_AT] = s.telegram.at
            it[TG_WEATHER] = s.telegram.weather
            it[TG_SWAPS] = s.telegram.swaps
            it[TG_FLASH] = s.telegram.flash
            it[SKIPPED] = s.skipped.takeLast(40).joinToString(",")
            it[FLASH_DAY] = s.flashDay
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val PAPER = stringPreferencesKey("paper")
        val NAME = stringPreferencesKey("name")
        val FEMININE = booleanPreferencesKey("feminine")
        val DAILY = booleanPreferencesKey("daily")
        val PRESS = stringPreferencesKey("press")
        val TG_ID = stringPreferencesKey("tg_id")
        val TG_AT = longPreferencesKey("tg_at")
        val TG_WEATHER = stringPreferencesKey("tg_weather")
        val TG_SWAPS = intPreferencesKey("tg_swaps")
        val TG_FLASH = booleanPreferencesKey("tg_flash")
        val SKIPPED = stringPreferencesKey("skipped")
        val FLASH_DAY = longPreferencesKey("flash_day")
    }
}
