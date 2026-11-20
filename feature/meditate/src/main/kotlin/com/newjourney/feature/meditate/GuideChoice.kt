package com.newjourney.feature.meditate

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.newjourney.core.model.VoiceGuide
import java.util.Locale

/**
 * "Guided meditation": no voice, Isla or Aiden. Choosing a voice plays its first words, so people
 * can hear who will guide them. The voice leads in after the start bell and back out before the end.
 */
@Composable
internal fun GuideChoice(selected: VoiceGuide, onSelect: (VoiceGuide) -> Unit, modifier: Modifier = Modifier) {
    val preview = rememberSoundPreview()
    Column(modifier) {
        Column(Modifier.selectableGroup()) {
            VoiceGuide.entries.forEach { guide ->
                val isSelected = guide == selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 38.dp)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                onSelect(guide)
                                val sample = guideSample(guide)
                                if (sample != null) preview.play(sample, limitMs = PREVIEW_MS) else preview.stop()
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Text(
                        stringResource(guideLabel(guide)),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.guide_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        // The recordings are in English; say so when the app is in another language.
        if (Locale.getDefault().language != "en") {
            Text(
                stringResource(R.string.guide_english),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun guideLabel(guide: VoiceGuide): Int = when (guide) {
    VoiceGuide.NONE -> R.string.guide_none
    VoiceGuide.FEMALE -> R.string.guide_female
    VoiceGuide.MALE -> R.string.guide_male
}

@RawRes
private fun guideSample(guide: VoiceGuide): Int? = when (guide) {
    VoiceGuide.NONE -> null
    VoiceGuide.FEMALE -> R.raw.guide_female_intro
    VoiceGuide.MALE -> R.raw.guide_male_intro
}

/** Long enough to hear the voice's first sentence. */
private const val PREVIEW_MS = 9_000L
