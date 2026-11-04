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
 * A calm note shown when a free user taps a premium length. No countdowns, no pressure:
 * it explains, then closes with one large button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumDurationSheet(minutes: Int, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.premium_duration_title, minutes),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(R.string.premium_duration_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(text = stringResource(R.string.premium_duration_ok), onClick = onDismiss)
        }
    }
}
