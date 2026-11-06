package com.newjourney.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.newjourney.core.database.SessionRepository
import com.newjourney.core.datastore.UserSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Wakes at a reminder's time, decides whether a note is welcome, shows it, and plans the next one. */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var settings: UserSettingsRepository
    @Inject lateinit var sessions: SessionRepository

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.action
            ?.removePrefix(ReminderScheduler.ACTION_PREFIX)
            ?.let { name -> ReminderType.entries.firstOrNull { it.name == name } }
            ?: return
        val pending = goAsync()
        scope.launch {
            try {
                handle(context, type)
            } finally {
                scheduler.scheduleNext(type)
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context, type: ReminderType) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        when (type) {
            ReminderType.DAILY -> {
                val reminders = settings.reminderSettings.first()
                val meditatedToday = sessions.sessionsSince(today.atStartOfDay(zone).toInstant()).first().isNotEmpty()
                if (reminders.dailySkipIfDone && meditatedToday) return
                notify(context, type, R.string.reminder_daily_title, context.getString(R.string.reminder_daily_body))
            }

            ReminderType.EVENING ->
                notify(context, type, R.string.reminder_evening_title, context.getString(R.string.reminder_evening_body))

            ReminderType.WEEKLY -> {
                val monday = today.with(DayOfWeek.MONDAY)
                val week = sessions.sessionsSince(monday.atStartOfDay(zone).toInstant()).first()
                // A week without sessions gets no summary: nothing to report, nothing to feel bad about.
                if (week.isEmpty()) return
                val days = week.map { it.startedAt.atZone(zone).toLocalDate() }.toSet().size
                val minutes = week.sumOf { it.completedSeconds } / 60
                val body = context.resources.getQuantityString(R.plurals.reminder_weekly_days, days, days) + " " +
                    context.resources.getQuantityString(R.plurals.reminder_weekly_minutes, minutes, minutes)
                notify(context, type, R.string.reminder_weekly_title, body)
            }

            ReminderType.RETURN_CHECK -> {
                val recent = sessions.sessionsSince(Instant.now().minus(Duration.ofDays(RETURN_WINDOW_DAYS))).first()
                // Never-started or long-gone users are left in peace; this is only for a short pause.
                val last = recent.maxOfOrNull { it.startedAt } ?: return
                val lastDay = last.atZone(zone).toLocalDate()
                if (ChronoUnit.DAYS.between(lastDay, today) < RETURN_AFTER_DAYS) return
                val sentOn = settings.lastEncouragementDay.first()
                // Once per pause: only if nothing was sent since the last session.
                if (sentOn != null && sentOn >= lastDay.toEpochDay()) return
                notify(context, type, R.string.reminder_return_title, context.getString(R.string.reminder_return_body))
                settings.setLastEncouragementDay(today.toEpochDay())
            }
        }
    }

    private fun notify(context: Context, type: ReminderType, titleRes: Int, body: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            PendingIntent.getActivity(context, type.requestCode, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(context.getString(titleRes))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            manager.notify(type.requestCode, notification)
        } catch (_: SecurityException) {
            // Notification permission was withdrawn; nothing to do.
        }
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "reminders"
        const val RETURN_AFTER_DAYS = 3L
        const val RETURN_WINDOW_DAYS = 30L
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

/** Puts reminders back after a restart, an app update, or a change of clock or time zone. */
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
