package com.newjourney.app

import android.app.Application
import com.newjourney.core.notifications.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NewJourneyApplication : Application() {

    @Inject lateinit var reminders: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        // Make sure the planned reminders match the saved settings (for example after an update).
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { reminders.rescheduleAll() }
    }
}
