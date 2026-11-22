package com.newjourney.feature.meditate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.newjourney.core.designsystem.component.PremiumNoteSheet
import com.newjourney.core.designsystem.R as DesignR

/** A small lock and "Premium", next to a choice that is part of Premium. Calm, never bright. */
@Composable
internal fun PremiumTag(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(stringResource(DesignR.string.premium_label), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** The note for a sound, bell or voice that is part of Premium. */
@Composable
internal fun PremiumSettingSheet(onDismiss: () -> Unit) {
    PremiumNoteSheet(
        title = stringResource(R.string.premium_setting_title),
        body = stringResource(R.string.premium_setting_body),
        onDismiss = onDismiss,
    )
}
