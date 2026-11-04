package com.newjourney.feature.meditate.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RawRes
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.newjourney.core.model.BackgroundSound
import com.newjourney.core.model.TimerSettings
import com.newjourney.feature.meditate.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps a meditation running while the screen is off or the app is in the background.
 * It follows [MeditationTimer.state]: plays the bells and the looping background sound,
 * shows a notification with Pause and End on the lock screen, and rings the end bell on time.
 */
@AndroidEntryPoint
class MeditationService : Service() {

    @Inject lateinit var timer: MeditationTimer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null
    private var endJob: Job? = null
    private var fadeJob: Job? = null

    private var backgroundPlayer: ExoPlayer? = null
    private var bellPlayer: ExoPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var currentSessionId: Long? = null
    private var isForeground = false
    private var finishing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> timer.pause()
            ACTION_RESUME -> timer.resume()
            ACTION_END -> timer.endEarly()
        }
        // Android requires a foreground notification within seconds of starting.
        if (!isForeground) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(timer.state.value),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
            )
            isForeground = true
        }
        if (stateJob == null) {
            stateJob = scope.launch { timer.state.collect { onState(it) } }
        }
        return START_NOT_STICKY
    }

    private fun onState(state: TimerState) {
        when (state) {
            is TimerState.Running -> {
                finishing = false
                if (currentSessionId != state.session.id) {
                    currentSessionId = state.session.id
                    beginAudio(state.session.settings)
                } else {
                    resumeAudio()
                }
                val remaining = state.remainingMs()
                acquireWakeLock(remaining + WAKE_LOCK_MARGIN_MS)
                endJob?.cancel()
                endJob = scope.launch {
                    delay(remaining)
                    timer.complete()
                }
                showNotification(state)
            }

            is TimerState.Paused -> {
                endJob?.cancel()
                pauseAudio()
                releaseWakeLock()
                showNotification(state)
            }

            is TimerState.Finished -> {
                endJob?.cancel()
                if (!finishing) {
                    finishing = true
                    showNotification(state)
                    scope.launch {
                        finishAudio(ringEndBell = !state.endedEarly && state.session.settings.endBell)
                        // A new session may have started while the bell rang; keep running for it.
                        val now = timer.state.value
                        if (now !is TimerState.Running && now !is TimerState.Paused) shutDown()
                    }
                }
            }

            TimerState.Idle -> {
                endJob?.cancel()
                // While finishing, let the end bell ring out; shutDown follows it.
                if (!finishing) shutDown()
            }
        }
    }

    // ---- Sound ----

    private fun beginAudio(settings: TimerSettings) {
        if (settings.startBell) playBell()
        val sound = soundResource(settings.sound) ?: return
        val player = backgroundPlayer ?: createBackgroundPlayer().also { backgroundPlayer = it }
        player.setMediaItem(MediaItem.fromUri(rawUri(sound)))
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.volume = 0f
        player.prepare()
        player.play()
        fadeTo(player, 1f, FADE_IN_MS)
    }

    private fun pauseAudio() {
        fadeJob?.cancel()
        backgroundPlayer?.pause()
    }

    private fun resumeAudio() {
        val player = backgroundPlayer ?: return
        player.play()
        fadeTo(player, 1f, RESUME_FADE_MS)
    }

    private suspend fun finishAudio(ringEndBell: Boolean) {
        backgroundPlayer?.let { player ->
            fadeTo(player, 0f, FADE_OUT_MS).join()
            player.stop()
        }
        if (ringEndBell) {
            playBell()
            delay(BELL_RING_OUT_MS)
        }
    }

    private fun createBackgroundPlayer(): ExoPlayer =
        ExoPlayer.Builder(this)
            // The background sound manages audio focus, so it pauses for phone calls.
            .setAudioAttributes(mediaAttributes(), true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

    private fun bell(): ExoPlayer = bellPlayer ?: ExoPlayer.Builder(this)
        // The bell does not take audio focus, so it never interrupts the background sound.
        .setAudioAttributes(mediaAttributes(), false)
        .build()
        .also {
            it.setMediaItem(MediaItem.fromUri(rawUri(R.raw.bell)))
            it.prepare()
            bellPlayer = it
        }

    private fun playBell() {
        val player = bell()
        player.seekTo(0)
        player.play()
    }

    private fun fadeTo(player: ExoPlayer, target: Float, durationMs: Long): Job {
        fadeJob?.cancel()
        val startVolume = player.volume
        val steps = (durationMs / FADE_STEP_MS).toInt().coerceAtLeast(1)
        return scope.launch {
            for (step in 1..steps) {
                player.volume = startVolume + (target - startVolume) * step / steps
                delay(FADE_STEP_MS)
            }
        }.also { fadeJob = it }
    }

    private fun mediaAttributes() = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    @RawRes
    private fun soundResource(sound: BackgroundSound): Int? = when (sound) {
        BackgroundSound.SILENCE -> null
        BackgroundSound.RAIN -> R.raw.rain
        BackgroundSound.FOREST -> R.raw.forest
    }

    private fun rawUri(@RawRes res: Int): Uri = Uri.parse("android.resource://$packageName/$res")

    // ---- Staying awake ----

    /** A partial wake lock keeps the CPU (not the screen) awake so the end bell rings on time. */
    private fun acquireWakeLock(timeoutMs: Long) {
        releaseWakeLock()
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NewJourney:meditation").apply {
            setReferenceCounted(false)
            acquire(timeoutMs)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    // ---- Notification ----

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun showNotification(state: TimerState) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: TimerState): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_meditation)
            .setContentTitle(getString(R.string.notification_title))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent())

        val remainingMinutes = ((state.remainingMs() + 59_999) / 60_000).toInt()
        when (state) {
            is TimerState.Running -> builder
                .setContentText(resources.getQuantityString(R.plurals.notification_minutes_left, remainingMinutes, remainingMinutes))
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + state.remainingMs())
                .addAction(0, getString(R.string.timer_pause), actionIntent(ACTION_PAUSE))
                .addAction(0, getString(R.string.timer_end), actionIntent(ACTION_END))

            is TimerState.Paused -> builder
                .setContentText(resources.getQuantityString(R.plurals.notification_paused, remainingMinutes, remainingMinutes))
                .setShowWhen(false)
                .addAction(0, getString(R.string.timer_resume), actionIntent(ACTION_RESUME))
                .addAction(0, getString(R.string.timer_end), actionIntent(ACTION_END))

            else -> builder.setContentText(getString(R.string.notification_finishing)).setShowWhen(false)
        }
        return builder.build()
    }

    private fun openAppIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launch.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun actionIntent(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, MeditationService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // ---- Shutting down ----

    private fun shutDown() {
        endJob?.cancel()
        fadeJob?.cancel()
        backgroundPlayer?.release()
        backgroundPlayer = null
        bellPlayer?.release()
        bellPlayer = null
        releaseWakeLock()
        currentSessionId = null
        finishing = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        isForeground = false
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        backgroundPlayer?.release()
        bellPlayer?.release()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "meditation_session"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "com.newjourney.meditate.START"
        private const val ACTION_PAUSE = "com.newjourney.meditate.PAUSE"
        private const val ACTION_RESUME = "com.newjourney.meditate.RESUME"
        private const val ACTION_END = "com.newjourney.meditate.END"

        private const val FADE_IN_MS = 4_000L
        private const val RESUME_FADE_MS = 1_500L
        private const val FADE_OUT_MS = 3_000L
        private const val FADE_STEP_MS = 50L
        /** The bell recording is about 7.3 seconds; this lets it ring out fully. */
        private const val BELL_RING_OUT_MS = 8_000L
        private const val WAKE_LOCK_MARGIN_MS = 60_000L

        internal fun startIntent(context: Context): Intent =
            Intent(context, MeditationService::class.java).setAction(ACTION_START)
    }
}
