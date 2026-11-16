package com.newjourney.feature.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.newjourney.core.datastore.AppLanguage
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.SoftCard

/**
 * Language: shows the current choice; tapping it opens a short list. Each language is written in
 * its own words (Nederlands, Deutsch …) so people can always find their own.
 */
@Composable
internal fun LanguageCard() {
    val context = LocalContext.current
    // The screen reopens after a change, so reading the choice once is enough.
    val current = remember { AppLanguage.current(context) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val currentName = current?.let(::languageName) ?: stringResource(R.string.language_system)

    SoftCard {
        Text(stringResource(R.string.language_title), style = MaterialTheme.typography.titleLarge)
        Surface(
            onClick = { choosing = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .heightIn(min = MinTouchTarget + 8.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    currentName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }

    if (choosing) {
        LanguageDialog(
            current = current,
            onChoose = { tag ->
                choosing = false
                if (tag != current) context.findActivity()?.let { AppLanguage.set(it, tag) }
            },
            onDismiss = { choosing = false },
        )
    }
}

@Composable
private fun LanguageDialog(current: String?, onChoose: (String?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                LanguageOption(stringResource(R.string.language_system), selected = current == null) { onChoose(null) }
                AppLanguage.supported.forEach { tag ->
                    LanguageOption(languageName(tag), selected = current == tag) { onChoose(tag) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.language_cancel)) } },
    )
}

@Composable
private fun LanguageOption(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
    }
}

/** Each language in its own words, so it is recognisable whatever language the app is in now. */
private fun languageName(tag: String): String = when (tag) {
    "nl" -> "Nederlands"
    "de" -> "Deutsch"
    "fr" -> "Français"
    "es" -> "Español"
    else -> "English"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
