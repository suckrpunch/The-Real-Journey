package com.newjourney.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.newjourney.core.model.ThemeMode
import com.newjourney.core.model.ThemePreference
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

    private object Keys {
        val FAVORITE_QUOTES = stringSetPreferencesKey("favorite_quote_ids")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NIGHT_START = intPreferencesKey("night_start_hour")
        val NIGHT_END = intPreferencesKey("night_end_hour")
    }
}
