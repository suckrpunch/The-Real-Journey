package com.newjourney.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.newjourney.core.designsystem.R

/**
 * A calm note shown when a free user taps something that is part of Premium.
 * No countdowns, no pressure: it explains, then closes with one large button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumNoteSheet(title: String, body: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton(text = stringResource(R.string.premium_duration_ok), onClick = onDismiss)
        }
    }
}

/** The note for a session length that is part of Premium. */
@Composable
fun PremiumDurationSheet(minutes: Int, onDismiss: () -> Unit) {
    PremiumNoteSheet(
        title = stringResource(R.string.premium_duration_title, minutes),
        body = stringResource(R.string.premium_duration_body),
        onDismiss = onDismiss,
    )
}

/** The note for a track that is part of Premium. */
@Composable
fun PremiumTrackSheet(onDismiss: () -> Unit) {
    PremiumNoteSheet(
        title = stringResource(R.string.premium_track_title),
        body = stringResource(R.string.premium_track_body),
        onDismiss = onDismiss,
    )
}
