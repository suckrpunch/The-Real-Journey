package com.newjourney.core.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.newjourney.core.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** What the music player is doing, for the player screen and the mini player. */
data class NowPlaying(
    val trackId: String,
    val title: String,
    val subtitle: String?,
    val isPlaying: Boolean,
    val durationMs: Long,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    /** True when the track could not be played, usually because it is online and there is no connection. */
    val failed: Boolean = false,
)

/**
 * The screens' handle on [PlaybackService]. Commands sent before the connection is ready
 * are run as soon as it is.
 */
@Singleton
class MusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ready = CompletableDeferred<MediaController>()
    private var connecting = false
    private var controller: MediaController? = null

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    /** Current position; read it on a short timer from the screen while it is visible. */
    fun positionMs(): Long = controller?.currentPosition ?: 0L

    /** Plays [tracks] in order, starting at [startIndex] from [startPositionMs]. */
    fun play(tracks: List<Track>, startIndex: Int, startPositionMs: Long = 0L) = withController { controller ->
        controller.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, startPositionMs)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() = withController {
        when {
            it.playerError != null -> {
                // Try again, for example once the internet connection is back.
                it.prepare()
                it.play()
            }
            it.isPlaying -> it.pause()
            else -> it.play()
        }
    }
    fun pause() = withController { it.pause() }
    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }
    fun next() = withController { it.seekToNextMediaItem() }

    /** Like most players: back to the start of the track, or to the previous track if near the start. */
    fun previous() = withController { it.seekToPrevious() }

    /** Starts connecting early, so the mini player can show music that is already playing. */
    fun connect() {
        if (connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val controller = runCatching { future.get() }.getOrNull() ?: run {
                    connecting = false
                    return@addListener
                }
                controller.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) = publish(player)
                })
                publish(controller)
                this.controller = controller
                ready.complete(controller)
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun withController(action: (MediaController) -> Unit) {
        connect()
        scope.launch { action(ready.await()) }
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        _nowPlaying.value = item?.let {
            NowPlaying(
                trackId = it.mediaId,
                title = it.mediaMetadata.title?.toString().orEmpty(),
                subtitle = it.mediaMetadata.subtitle?.toString(),
                isPlaying = player.isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING),
                durationMs = player.duration.takeIf { d -> d > 0 }
                    ?: it.mediaMetadata.extras?.getLong(PlaybackService.EXTRA_DURATION_MS) ?: 0L,
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem(),
                failed = player.playerError != null,
            )
        }
    }
}

internal fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri(audioUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtist(subtitle)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(Bundle().apply { putLong(PlaybackService.EXTRA_DURATION_MS, durationMs) })
                .build(),
        )
        .build()
