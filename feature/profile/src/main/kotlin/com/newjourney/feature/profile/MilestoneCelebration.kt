package com.newjourney.feature.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.newjourney.core.database.SessionRepository
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.SecondaryButton
import com.newjourney.core.designsystem.component.milestoneTitle
import com.newjourney.core.designsystem.living.breathe
import com.newjourney.core.designsystem.living.rememberReduceMotion
import com.newjourney.core.designsystem.living.rememberSeconds
import com.newjourney.core.designsystem.theme.NewJourneyTheme
import com.newjourney.core.model.Milestone
import com.newjourney.core.model.MilestoneProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Watches for milestones the user has reached but not yet seen celebrated, wherever they were
 * reached (the timer or listening to music). One at a time, oldest first.
 */
@HiltViewModel
internal class MilestoneCelebrationViewModel @Inject constructor(
    sessions: SessionRepository,
    private val settings: UserSettingsRepository,
) : ViewModel() {

    val pending: StateFlow<Milestone?> = combine(sessions.progress(), settings.seenMilestones) { stats, seen ->
        val reached = stats.achieved.map { it.milestone }
        if (seen == null) {
            // First run with this feature: what was reached before is history, not news.
            settings.markMilestonesSeen(reached.map { it.name }.toSet())
            null
        } else {
            stats.achieved
                .filter { it.milestone.name !in seen }
                .sortedWith(compareBy<MilestoneProgress>({ it.achievedAt }, { it.milestone.ordinal }))
                .firstOrNull()
                ?.milestone
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun seen(milestone: Milestone) {
        viewModelScope.launch { settings.markMilestonesSeen(setOf(milestone.name)) }
    }
}

/**
 * Shows a newly reached milestone as a popup over whatever screen is open, so it is not missed.
 * Place once at the top of the app.
 */
@Composable
fun MilestoneCelebration(onOpenMilestones: () -> Unit, enabled: Boolean = true) {
    val viewModel: MilestoneCelebrationViewModel = hiltViewModel()
    val milestone by viewModel.pending.collectAsStateWithLifecycle()
    val current = milestone ?: return
    if (!enabled) return
    MilestoneDialog(
        milestone = current,
        onClose = { viewModel.seen(current) },
        onOpenMilestones = {
            viewModel.seen(current)
            onOpenMilestones()
        },
    )
}

@Composable
private fun MilestoneDialog(milestone: Milestone, onClose: () -> Unit, onOpenMilestones: () -> Unit) {
    val reduceMotion = rememberReduceMotion()
    // The badge rises gently into place; with reduced motion it is simply there.
    val arrive = remember(milestone) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(milestone) {
        if (!reduceMotion) arrive.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
    }
    val fadeIn = remember(milestone) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(milestone) {
        if (!reduceMotion) fadeIn.animateTo(1f, tween(durationMillis = 700, delayMillis = 250))
    }
    val seconds by rememberSeconds(!reduceMotion)
    val glow = MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                    // A soft halo that breathes behind the badge.
                    Canvas(Modifier.size(200.dp)) {
                        val b = if (reduceMotion) 0.5f else breathe(seconds, period = 5f)
                        drawCircle(
                            Brush.radialGradient(
                                listOf(glow.copy(alpha = 0.22f + 0.12f * b), Color.Transparent),
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.minDimension / 2,
                            ),
                        )
                    }
                    MilestoneBadge(
                        milestone,
                        earned = true,
                        size = 150.dp,
                        modifier = Modifier.graphicsLayer {
                            val s = 0.6f + 0.4f * arrive.value
                            scaleX = s
                            scaleY = s
                            alpha = arrive.value.coerceIn(0f, 1f)
                        },
                    )
                }
                Column(
                    Modifier.graphicsLayer { alpha = fadeIn.value },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = NewJourneyTheme.extraColors.premium,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            stringResource(R.string.milestone_popup_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        stringResource(milestoneTitle(milestone)),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.milestone_popup_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrimaryButton(text = stringResource(R.string.milestone_popup_close), onClick = onClose)
                    SecondaryButton(
                        text = stringResource(R.string.milestone_popup_view),
                        onClick = onOpenMilestones,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
