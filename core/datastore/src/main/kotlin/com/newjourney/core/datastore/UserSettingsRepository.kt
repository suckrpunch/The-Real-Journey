package com.newjourney.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.newjourney.core.model.BackgroundSound
import com.newjourney.core.model.ThemeMode
import com.newjourney.core.model.ThemePreference
import com.newjourney.core.model.TimerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Small on-device settings: theme now; daily goal and reminder times join here later. */
interface UserSettingsRepository {
    val themePreference: Flow<ThemePreference>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setNightSchedule(startHour: Int, endHour: Int)

    /** Quotes the user saved with the heart button. Moves to the synced database in Phase 3. */
    val favoriteQuoteIds: Flow<Set<String>>
    suspend fun toggleFavoriteQuote(quoteId: String)

    /** Bells and background sound, remembered so Home's Begin uses the user's last choices. */
    val timerSettings: Flow<TimerSettings>
    suspend fun setTimerSettings(settings: TimerSettings)
}

@Singleton
internal class DataStoreUserSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserSettingsRepository {

    override val themePreference: Flow<ThemePreference> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            val defaults = ThemePreference()
            ThemePreference(
                mode = prefs[Keys.THEME_MODE]
                    ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: defaults.mode,
                nightStartHour = prefs[Keys.NIGHT_START] ?: defaults.nightStartHour,
                nightEndHour = prefs[Keys.NIGHT_END] ?: defaults.nightEndHour,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setNightSchedule(startHour: Int, endHour: Int) {
        dataStore.edit {
            it[Keys.NIGHT_START] = startHour.coerceIn(0, 23)
            it[Keys.NIGHT_END] = endHour.coerceIn(0, 23)
        }
    }

    override val favoriteQuoteIds: Flow<Set<String>> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[Keys.FAVORITE_QUOTES] ?: emptySet() }

    override suspend fun toggleFavoriteQuote(quoteId: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_QUOTES] ?: emptySet()
            prefs[Keys.FAVORITE_QUOTES] = if (quoteId in current) current - quoteId else current + quoteId
        }
    }

    override val timerSettings: Flow<TimerSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            val defaults = TimerSettings()
            TimerSettings(
                startBell = prefs[Keys.START_BELL] ?: defaults.startBell,
                endBell = prefs[Keys.END_BELL] ?: defaults.endBell,
                sound = prefs[Keys.BACKGROUND_SOUND]
                    ?.let { stored -> BackgroundSound.entries.firstOrNull { it.name == stored } }
                    ?: defaults.sound,
            )
        }

    override suspend fun setTimerSettings(settings: TimerSettings) {
        dataStore.edit {
            it[Keys.START_BELL] = settings.startBell
            it[Keys.END_BELL] = settings.endBell
            it[Keys.BACKGROUND_SOUND] = settings.sound.name
        }
    }

    private object Keys {
        val START_BELL = booleanPreferencesKey("timer_start_bell")
        val END_BELL = booleanPreferencesKey("timer_end_bell")
        val BACKGROUND_SOUND = stringPreferencesKey("timer_background_sound")
        val FAVORITE_QUOTES = stringSetPreferencesKey("favorite_quote_ids")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NIGHT_START = intPreferencesKey("night_start_hour")
        val NIGHT_END = intPreferencesKey("night_end_hour")
    }
}
