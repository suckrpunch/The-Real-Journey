package com.newjourney.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.newjourney.core.model.BackgroundSound
import com.newjourney.core.model.DailyGoal
import com.newjourney.core.model.ReminderSettings
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

    /** Minutes a day the user aims for. Shown on Home; never used to scold. */
    val dailyGoalMinutes: Flow<Int>
    suspend fun setDailyGoalMinutes(minutes: Int)

    val reminderSettings: Flow<ReminderSettings>
    suspend fun setReminderSettings(settings: ReminderSettings)

    /** The day (epoch day) the "we're here when you're ready" note was last sent, or null. */
    val lastEncouragementDay: Flow<Long?>
    suspend fun setLastEncouragementDay(epochDay: Long?)
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

    private val safeData = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    override val dailyGoalMinutes: Flow<Int> = safeData.map { it[Keys.DAILY_GOAL] ?: DailyGoal.DEFAULT_MINUTES }

    override suspend fun setDailyGoalMinutes(minutes: Int) {
        dataStore.edit { it[Keys.DAILY_GOAL] = minutes.coerceIn(1, 180) }
    }

    override val reminderSettings: Flow<ReminderSettings> = safeData.map { prefs ->
        val d = ReminderSettings()
        ReminderSettings(
            dailyOn = prefs[Keys.R_DAILY_ON] ?: d.dailyOn,
            dailyMinuteOfDay = prefs[Keys.R_DAILY_TIME] ?: d.dailyMinuteOfDay,
            dailySkipIfDone = prefs[Keys.R_DAILY_SKIP] ?: d.dailySkipIfDone,
            eveningOn = prefs[Keys.R_EVENING_ON] ?: d.eveningOn,
            eveningMinuteOfDay = prefs[Keys.R_EVENING_TIME] ?: d.eveningMinuteOfDay,
            weeklyOn = prefs[Keys.R_WEEKLY_ON] ?: d.weeklyOn,
            encourageOn = prefs[Keys.R_ENCOURAGE_ON] ?: d.encourageOn,
        )
    }

    override suspend fun setReminderSettings(settings: ReminderSettings) {
        dataStore.edit {
            it[Keys.R_DAILY_ON] = settings.dailyOn
            it[Keys.R_DAILY_TIME] = Math.floorMod(settings.dailyMinuteOfDay, 24 * 60)
            it[Keys.R_DAILY_SKIP] = settings.dailySkipIfDone
            it[Keys.R_EVENING_ON] = settings.eveningOn
            it[Keys.R_EVENING_TIME] = Math.floorMod(settings.eveningMinuteOfDay, 24 * 60)
            it[Keys.R_WEEKLY_ON] = settings.weeklyOn
            it[Keys.R_ENCOURAGE_ON] = settings.encourageOn
        }
    }

    override val lastEncouragementDay: Flow<Long?> = safeData.map { it[Keys.LAST_ENCOURAGEMENT] }

    override suspend fun setLastEncouragementDay(epochDay: Long?) {
        dataStore.edit {
            if (epochDay == null) it.remove(Keys.LAST_ENCOURAGEMENT) else it[Keys.LAST_ENCOURAGEMENT] = epochDay
        }
    }

    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal_minutes")
        val R_DAILY_ON = booleanPreferencesKey("reminder_daily_on")
        val R_DAILY_TIME = intPreferencesKey("reminder_daily_minute_of_day")
        val R_DAILY_SKIP = booleanPreferencesKey("reminder_daily_skip_if_done")
        val R_EVENING_ON = booleanPreferencesKey("reminder_evening_on")
        val R_EVENING_TIME = intPreferencesKey("reminder_evening_minute_of_day")
        val R_WEEKLY_ON = booleanPreferencesKey("reminder_weekly_on")
        val R_ENCOURAGE_ON = booleanPreferencesKey("reminder_encourage_on")
        val LAST_ENCOURAGEMENT = longPreferencesKey("last_encouragement_epoch_day")
        val START_BELL = booleanPreferencesKey("timer_start_bell")
        val END_BELL = booleanPreferencesKey("timer_end_bell")
        val BACKGROUND_SOUND = stringPreferencesKey("timer_background_sound")
        val FAVORITE_QUOTES = stringSetPreferencesKey("favorite_quote_ids")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NIGHT_START = intPreferencesKey("night_start_hour")
        val NIGHT_END = intPreferencesKey("night_end_hour")
    }
}
