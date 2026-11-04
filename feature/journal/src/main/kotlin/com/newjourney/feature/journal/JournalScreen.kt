package com.newjourney.feature.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.designsystem.component.moodColor
import com.newjourney.core.designsystem.component.moodLabel
import com.newjourney.core.model.Mood
import kotlinx.serialization.Serializable

/** Opens the Journal; [mood] is the enum name when the user tapped a mood on Home. */
@Serializable
data class JournalRoute(val mood: String? = null)

fun NavGraphBuilder.journalScreen() {
    composable<JournalRoute> { entry ->
        val moodName = entry.toRoute<JournalRoute>().mood
        JournalScreen(mood = Mood.entries.firstOrNull { it.name == moodName })
    }
}

/** Phase 1 starter. Writing, saving (encrypted), search and the calendar are built in a later phase. */
@Composable
internal fun JournalScreen(mood: Mood?) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SectionTitle(stringResource(R.string.journal_title))
        if (mood != null) {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(24.dp).background(moodColor(mood), CircleShape))
                    Text(
                        stringResource(R.string.journal_feeling, stringResource(moodLabel(mood))),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Text(
                    stringResource(R.string.journal_mood_note),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            stringResource(R.string.journal_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
