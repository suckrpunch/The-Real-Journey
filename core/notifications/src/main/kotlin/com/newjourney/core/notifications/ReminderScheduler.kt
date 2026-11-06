package com.newjourney.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.model.ReminderSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

internal enum class ReminderType(val requestCode: Int) {
    DAILY(2001),
    EVENING(2002),
    WEEKLY(2003),

    /** A daily check, late afternoon, for the "we're here when you're ready" note. */
    RETURN_CHECK(2004),
}

/**
 * Plans each reminder with AlarmManager. Alarms use a 10-minute window instead of an exact time:
 * no special permission is needed and Android can group them with other wake-ups to save battery.
 * Each alarm plans only its next occurrence; the receiver plans the one after.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: UserSettingsRepository,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    /** Plans or cancels every reminder to match the saved settings. Safe to call any time. */
    suspend fun rescheduleAll() {
        apply(settings.reminderSettings.first())
    }

    internal suspend fun scheduleNext(type: ReminderType) {
        val current = settings.reminderSettings.first()
        if (isOn(type, current)) set(type, nextTrigger(type, current)) else cancel(type)
    }

    private fun apply(reminders: ReminderSettings) {
        ReminderType.entries.forEach { type ->
            if (isOn(type, reminders)) set(type, nextTrigger(type, reminders)) else cancel(type)
        }
    }

    private fun isOn(type: ReminderType, r: ReminderSettings): Boolean = when (type) {
        ReminderType.DAILY -> r.dailyOn
        ReminderType.EVENING -> r.eveningOn
        ReminderType.WEEKLY -> r.weeklyOn
        ReminderType.RETURN_CHECK -> r.encourageOn
    }

    private fun nextTrigger(type: ReminderType, r: ReminderSettings, now: ZonedDateTime = ZonedDateTime.now()): Long {
        val zone = ZoneId.systemDefault()
        return when (type) {
            ReminderType.DAILY -> nextAt(r.dailyMinuteOfDay, now)
            ReminderType.EVENING -> nextAt(r.eveningMinuteOfDay, now)
            ReminderType.RETURN_CHECK -> nextAt(RETURN_CHECK_MINUTE, now)
            ReminderType.WEEKLY -> {
                var date: LocalDate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                var at = date.atTime(timeOf(WEEKLY_MINUTE)).atZone(zone)
                if (!at.isAfter(now)) {
                    date = date.plusWeeks(1)
                    at = date.atTime(timeOf(WEEKLY_MINUTE)).atZone(zone)
                }
                at.toInstant().toEpochMilli()
            }
        }
    }

    private fun nextAt(minuteOfDay: Int, now: ZonedDateTime): Long {
        var at = now.toLocalDate().atTime(timeOf(minuteOfDay)).atZone(now.zone)
        if (!at.isAfter(now)) at = at.plusDays(1)
        return at.toInstant().toEpochMilli()
    }

    private fun timeOf(minuteOfDay: Int) = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

    private fun set(type: ReminderType, triggerAtMs: Long) {
        alarms.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMs, WINDOW_MS, pendingIntent(type))
    }

    private fun cancel(type: ReminderType) {
        alarms.cancel(pendingIntent(type))
    }

    private fun pendingIntent(type: ReminderType): PendingIntent = PendingIntent.getBroadcast(
        context,
        type.requestCode,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_PREFIX + type.name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    internal companion object {
        const val ACTION_PREFIX = "com.newjourney.reminder."
        private const val WINDOW_MS = 10 * 60 * 1000L
        private const val RETURN_CHECK_MINUTE = 17 * 60 + 30
        private const val WEEKLY_MINUTE = 19 * 60
    }
}
