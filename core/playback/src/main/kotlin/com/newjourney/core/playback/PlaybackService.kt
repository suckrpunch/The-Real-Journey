package com.newjourney.core.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.newjourney.core.database.LibraryRepository
import com.newjourney.core.database.SessionRepository
import com.newjourney.core.model.MIN_SAVED_SESSION_SECONDS
import com.newjourney.core.model.MeditationSession
import com.newjourney.core.model.SessionKind
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Plays the music library in the background, with the standard Android media controls on the
 * lock screen, in the notification shade, on headphones and in the car.
 *
 * It also remembers where you were in each track ("Continue listening") and saves a session
 * when a track is listened to all the way through.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var library: LibraryRepository
    @Inject lateinit var sessions: SessionRepository

    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionJob: Job? = null

    /** When the current track started playing from (near) the beginning, for saving a listen session. */
    private var listenStartedAt: Instant? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(AudioCache.mediaSourceFactory(this))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true, // pauses for calls and other apps
            )
            .setHandleAudioBecomingNoisy(true) // pauses when headphones are unplugged
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.addListener(listener(player))

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply {
                setSmallIcon(R.drawable.ic_stat_music)
            },
        )
        session = MediaSession.Builder(this, player)
            .apply { openAppIntent()?.let { setSessionActivity(it) } }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Swiping the app away stops the music only if it was not playing. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.player?.let { savePosition(it) }
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private fun listener(player: Player) = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            positionJob?.cancel()
            if (isPlaying) {
                if (player.currentPosition < START_TOLERANCE_MS && listenStartedAt == null) listenStartedAt = Instant.now()
                positionJob = scope.launch {
                    while (true) {
                        savePosition(player)
                        delay(SAVE_EVERY_MS)
                    }
                }
            } else {
                savePosition(player)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val previousFinished = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
            if (previousFinished) recordListen(player, previousIndex(player))
            listenStartedAt = if (player.currentPosition < START_TOLERANCE_MS) Instant.now() else null
            savePosition(player)
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) {
                recordListen(player, player.currentMediaItemIndex)
                // Finished: next time it starts from the beginning.
                player.currentMediaItem?.mediaId?.let { id -> scope.launch { library.rememberPosition(id, 0) } }
            }
        }
    }

    private fun previousIndex(player: Player): Int =
        if (player.currentMediaItemIndex > 0) player.currentMediaItemIndex - 1 else player.mediaItemCount - 1

    private fun recordListen(player: Player, index: Int) {
        val started = listenStartedAt ?: return
        listenStartedAt = null
        if (index !in 0 until player.mediaItemCount) return
        val item = player.getMediaItemAt(index)
        val seconds = (item.mediaMetadata.extras?.getLong(EXTRA_DURATION_MS) ?: 0L) / 1000
        if (seconds < MIN_SAVED_SESSION_SECONDS) return
        scope.launch {
            sessions.record(
                MeditationSession(
                    startedAt = started,
                    plannedSeconds = seconds.toInt(),
                    completedSeconds = seconds.toInt(),
                    completed = true,
                    kind = SessionKind.LISTEN,
                    trackId = item.mediaId,
                ),
            )
        }
    }

    private fun savePosition(player: Player) {
        val id = player.currentMediaItem?.mediaId ?: return
        if (player.playbackState == Player.STATE_ENDED) return
        val position = player.currentPosition
        scope.launch { library.rememberPosition(id, position) }
    }

    private fun openAppIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launch.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(this, 1, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    internal companion object {
        const val EXTRA_DURATION_MS = "duration_ms"
        private const val SAVE_EVERY_MS = 10_000L
        private const val START_TOLERANCE_MS = 5_000L
    }
}
