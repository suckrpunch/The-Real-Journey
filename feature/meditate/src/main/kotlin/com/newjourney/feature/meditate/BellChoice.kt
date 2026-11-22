package com.newjourney.feature.meditate

import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.newjourney.core.designsystem.component.PillShape
import com.newjourney.core.model.BellSound

/**
 * "Bell sound": one pill per bell. Choosing a bell plays it once, so people can hear the difference.
 * Without Premium, the Tibetan bell shows a lock and explains Premium instead.
 */
@Composable
internal fun BellChoice(
    selected: BellSound,
    onSelect: (BellSound) -> Unit,
    modifier: Modifier = Modifier,
    isPremium: Boolean = true,
    onLocked: () -> Unit = {},
) {
    val preview = rememberSoundPreview()
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.setup_bell_sound), style = MaterialTheme.typography.bodyLarge)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BellSound.entries.forEach { bell ->
                val isSelected = bell == selected
                val locked = bell.isPremium && !isPremium
                Surface(
                    shape = PillShape,
                    color = if (isSelected) colors.primaryContainer else Color.Transparent,
                    contentColor = colors.onSurface,
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) colors.primary else colors.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .clip(PillShape)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                if (locked) {
                                    onLocked()
                                } else {
                                    onSelect(bell)
                                    preview.play(bellResource(bell))
                                }
                            },
                        ),
                ) {
                    Column(
                        Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            stringResource(bellLabel(bell)),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                        if (locked) PremiumTag()
                    }
                }
            }
        }
    }
}

internal fun bellLabel(bell: BellSound): Int = when (bell) {
    BellSound.SINGING_BOWL -> R.string.bell_classic
    BellSound.TIBETAN -> R.string.bell_tibetan
}

@RawRes
private fun bellResource(bell: BellSound): Int = when (bell) {
    BellSound.SINGING_BOWL -> R.raw.bell
    BellSound.TIBETAN -> R.raw.bell_tibetan
}

/**
 * Plays one short sample at a time (a bell, the first words of a voice); a new choice stops the
 * previous one. With [play]'s limit it stops after that long. Released when the screen goes.
 */
internal class SoundPreview(private val context: android.content.Context) {
    private var player: MediaPlayer? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val stopper = Runnable { stop() }

    fun play(@RawRes res: Int, limitMs: Long? = null) {
        stop()
        // create() prepares the player, so it plays on the media volume straight away.
        player = MediaPlayer.create(context, res)?.apply {
            setOnCompletionListener { stop() }
            start()
        }
        if (limitMs != null) handler.postDelayed(stopper, limitMs)
    }

    fun stop() {
        handler.removeCallbacks(stopper)
        player?.release()
        player = null
    }
}

@Composable
internal fun rememberSoundPreview(): SoundPreview {
    val context = LocalContext.current.applicationContext
    val preview = remember { SoundPreview(context) }
    DisposableEffect(preview) { onDispose { preview.stop() } }
    return preview
}
