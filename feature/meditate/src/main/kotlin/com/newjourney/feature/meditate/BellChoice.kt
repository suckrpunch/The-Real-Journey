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
 */
@Composable
internal fun BellChoice(selected: BellSound, onSelect: (BellSound) -> Unit, modifier: Modifier = Modifier) {
    val preview = rememberBellPreview()
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.setup_bell_sound), style = MaterialTheme.typography.bodyLarge)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BellSound.entries.forEach { bell ->
                val isSelected = bell == selected
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
                                onSelect(bell)
                                preview.play(bellResource(bell))
                            },
                        ),
                ) {
                    Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(bellLabel(bell)),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
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

/** Plays one bell at a time; a new choice stops the previous one. Released when the screen goes. */
private class BellPreview(private val context: android.content.Context) {
    private var player: MediaPlayer? = null

    fun play(@RawRes res: Int) {
        stop()
        // create() prepares the player, so it plays on the media volume straight away.
        player = MediaPlayer.create(context, res)?.apply {
            setOnCompletionListener { stop() }
            start()
        }
    }

    fun stop() {
        player?.release()
        player = null
    }
}

@Composable
private fun rememberBellPreview(): BellPreview {
    val context = LocalContext.current.applicationContext
    val preview = remember { BellPreview(context) }
    DisposableEffect(preview) { onDispose { preview.stop() } }
    return preview
}
