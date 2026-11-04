package com.newjourney.core.model

/** How the user wants the app's colours chosen. */
enum class ThemeMode {
    /** Follow the phone's dark-theme setting. */
    SYSTEM,

    /** Always the light, warm day theme. */
    DAY,

    /** Always the dark, midnight night theme. */
    NIGHT,

    /** Night theme between [ThemePreference.nightStartHour] and [ThemePreference.nightEndHour]. */
    SCHEDULED,
}

data class ThemePreference(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    /** Hour of day (0–23) when the night theme starts in scheduled mode. */
    val nightStartHour: Int = 20,
    /** Hour of day (0–23) when the day theme returns in scheduled mode. */
    val nightEndHour: Int = 7,
) {
    /** Decides whether the night theme should show right now. */
    fun useNightTheme(currentHour: Int, systemIsDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemIsDark
        ThemeMode.DAY -> false
        ThemeMode.NIGHT -> true
        ThemeMode.SCHEDULED -> isWithinNightHours(currentHour)
    }

    private fun isWithinNightHours(hour: Int): Boolean = when {
        nightStartHour == nightEndHour -> false
        // e.g. 20:00 → 07:00 crosses midnight
        nightStartHour > nightEndHour -> hour >= nightStartHour || hour < nightEndHour
        // e.g. 01:00 → 06:00 within one day
        else -> hour in nightStartHour until nightEndHour
    }
}
