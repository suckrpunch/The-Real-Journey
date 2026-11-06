package com.newjourney.feature.profile

import com.newjourney.core.database.SessionRepository
import com.newjourney.core.model.ProgressStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Progress worked out from every saved session. Recomputed whenever a session is saved. */
internal fun SessionRepository.progress(zone: ZoneId = ZoneId.systemDefault()): Flow<ProgressStats> =
    sessionsSince(Instant.EPOCH).map { ProgressStats.from(it, LocalDate.now(zone), zone) }

/** "2 h 35 min", "45 min". */
internal fun formatMinutes(minutes: Int, hourShort: String, minuteShort: String): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest $minuteShort"
        rest == 0 -> "$hours $hourShort"
        else -> "$hours $hourShort $rest $minuteShort"
    }
}

/** "08:30" in the phone's own clock style. */
internal fun formatMinuteOfDay(minuteOfDay: Int): String {
    val time = java.time.LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    return time.format(
        java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
            .withLocale(java.util.Locale.getDefault()),
    )
}
