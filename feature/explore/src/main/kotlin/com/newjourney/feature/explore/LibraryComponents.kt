package com.newjourney.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.TrackArtwork
import com.newjourney.core.designsystem.theme.NewJourneyTheme
import com.newjourney.core.model.Track
import com.newjourney.core.model.TrackTier

/** "3:42" or "1:02:05". */
internal fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

/** A whole-row button for a track: artwork, title, frequency and length. */
@Composable
internal fun TrackRow(
    track: Track,
    colorHex: String?,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val minutes = ((track.durationMs + 30_000) / 60_000).toInt()
    val spoken = buildList {
        add(track.title)
        track.subtitle?.let { add(it) }
        add(stringResource(R.string.track_minutes_spoken, minutes))
        if (locked) add(stringResource(com.newjourney.core.designsystem.R.string.premium_label))
    }.joinToString(", ")
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = spoken },
    ) {
        Row(
            Modifier
                .heightIn(min = 80.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TrackArtwork(colorHex, Modifier.size(64.dp).clip(MaterialTheme.shapes.medium))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(track.subtitle, formatDuration(track.durationMs)).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (locked || track.tier == TrackTier.PREMIUM) {
                    Text(
                        stringResource(com.newjourney.core.designsystem.R.string.premium_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = NewJourneyTheme.extraColors.premium,
                    )
                }
            }
        }
    }
}

/** A large, labelled Back button in the same place on every inner screen. */
@Composable
internal fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onBack, modifier = modifier.heightIn(min = MinTouchTarget), shape = CircleShape) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        Text(
            stringResource(R.string.back),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
