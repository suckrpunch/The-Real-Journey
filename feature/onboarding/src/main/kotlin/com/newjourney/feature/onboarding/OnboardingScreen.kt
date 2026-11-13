package com.newjourney.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.SecondaryButton
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.designsystem.component.TrackArtwork
import com.newjourney.core.model.AgeRange
import com.newjourney.core.model.Goal

@Composable
internal fun OnboardingRouteContent(
    onFinished: () -> Unit,
    onOpenTrack: (String) -> Unit,
    onStartMeditation: (Int) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Back walks through the questions; on the first screen it leaves the introduction.
    BackHandler {
        if (!viewModel.back()) {
            if (state.editing || state.step == OnboardingStep.PICKS) onFinished() else viewModel.skipAll(onFinished)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        TopRow(
            state = state,
            onBack = { if (!viewModel.back()) onFinished() },
            onSkip = { if (state.editing) onFinished() else viewModel.skipAll(onFinished) },
        )
        when (state.step) {
            OnboardingStep.WELCOME -> WelcomeStep(onBegin = viewModel::next)
            OnboardingStep.NAME -> NameStep(name = state.name, onNameChange = viewModel::setName, onContinue = viewModel::next)
            OnboardingStep.AGE -> AgeStep(selected = state.ageRange, onSelect = viewModel::setAge, onContinue = viewModel::next)
            OnboardingStep.GOALS -> GoalsStep(selected = state.goals, onToggle = viewModel::toggleGoal, onContinue = viewModel::next)
            OnboardingStep.PICKS -> PicksStep(
                state = state,
                onOpenTrack = onOpenTrack,
                onStartMeditation = { onStartMeditation(state.firstSessionMinutes) },
                onDone = onFinished,
            )
        }
    }
}

@Composable
private fun TopRow(state: OnboardingUiState, onBack: () -> Unit, onSkip: () -> Unit) {
    val showBack = state.questionNumber != null && !(state.editing && state.step == OnboardingStep.NAME)
    Row(
        Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(
                    stringResource(R.string.onboarding_back),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        state.questionNumber?.let { number ->
            Text(
                stringResource(R.string.onboarding_step, number, 3),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.step != OnboardingStep.PICKS) {
            TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(
                    stringResource(if (state.editing) R.string.onboarding_cancel else R.string.onboarding_skip),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun Heading(title: String, body: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        body?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WelcomeStep(onBegin: () -> Unit) {
    Spacer(Modifier.heightIn(min = 24.dp))
    Text(stringResource(R.string.onboarding_welcome_title), style = MaterialTheme.typography.displaySmall)
    Text(
        stringResource(R.string.onboarding_welcome_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        stringResource(R.string.onboarding_private),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PrimaryButton(text = stringResource(R.string.onboarding_welcome_begin), onClick = onBegin)
}

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit, onContinue: () -> Unit) {
    Heading(stringResource(R.string.onboarding_name_title), stringResource(R.string.onboarding_name_body))
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.onboarding_name_label)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onContinue() }),
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryButton(
        text = stringResource(if (name.isBlank()) R.string.onboarding_skip_question else R.string.onboarding_continue),
        onClick = onContinue,
    )
}

@Composable
private fun AgeStep(selected: AgeRange?, onSelect: (AgeRange) -> Unit, onContinue: () -> Unit) {
    Heading(stringResource(R.string.onboarding_age_title), stringResource(R.string.onboarding_age_body))
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AgeRange.entries.forEach { range ->
            val isSelected = range == selected
            ChoiceRow(
                label = stringResource(ageLabel(range)),
                modifier = Modifier.selectable(selected = isSelected, onClick = { onSelect(range) }, role = Role.RadioButton),
                selected = isSelected,
            ) { RadioButton(selected = isSelected, onClick = null) }
        }
    }
    PrimaryButton(
        text = stringResource(if (selected == null) R.string.onboarding_skip_question else R.string.onboarding_continue),
        onClick = onContinue,
    )
}

@Composable
private fun GoalsStep(selected: Set<Goal>, onToggle: (Goal) -> Unit, onContinue: () -> Unit) {
    Heading(stringResource(R.string.onboarding_goals_title), stringResource(R.string.onboarding_goals_body))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Goal.entries.forEach { goal ->
            val isChecked = goal in selected
            ChoiceRow(
                label = stringResource(goalLabel(goal)),
                modifier = Modifier.toggleable(value = isChecked, onValueChange = { onToggle(goal) }, role = Role.Checkbox),
                selected = isChecked,
            ) { Checkbox(checked = isChecked, onCheckedChange = null) }
        }
    }
    PrimaryButton(
        text = stringResource(if (selected.isEmpty()) R.string.onboarding_skip_question else R.string.onboarding_show_picks),
        onClick = onContinue,
    )
}

/** A large, easy-to-tap answer row. The whole row is the touch target. */
@Composable
private fun ChoiceRow(
    label: String,
    modifier: Modifier,
    selected: Boolean,
    control: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).then(modifier),
    ) {
        Row(
            Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            control()
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun PicksStep(
    state: OnboardingUiState,
    onOpenTrack: (String) -> Unit,
    onStartMeditation: () -> Unit,
    onDone: () -> Unit,
) {
    val name = state.name.trim().takeIf { it.isNotEmpty() }
    Heading(
        if (name != null) stringResource(R.string.onboarding_picks_title_named, name) else stringResource(R.string.onboarding_picks_title),
        stringResource(R.string.onboarding_picks_body),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.picks.forEach { pick ->
            PickCard(pick = pick, onClick = { onOpenTrack(pick.track.id) })
        }
    }
    if (state.editing) {
        PrimaryButton(text = stringResource(R.string.onboarding_done), onClick = onDone)
    } else {
        SoftCard {
            Text(stringResource(R.string.onboarding_first_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.onboarding_first_body, state.firstSessionMinutes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(
                text = stringResource(R.string.onboarding_first_begin, state.firstSessionMinutes),
                onClick = onStartMeditation,
                leading = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            )
        }
        SecondaryButton(
            text = stringResource(R.string.onboarding_to_home),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PickCard(pick: Pick, onClick: () -> Unit) {
    val minutes = ((pick.track.durationMs + 59_999) / 60_000).toInt()
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = stringResource(R.string.onboarding_pick_listen), onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TrackArtwork(pick.colorHex, Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pick.track.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOf(pick.place, stringResource(R.string.onboarding_pick_minutes, minutes))
                        .filter { it.isNotEmpty() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (pick.premium) {
                    Text(
                        stringResource(R.string.onboarding_pick_premium),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                if (pick.premium) Icons.Outlined.Lock else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun ageLabel(range: AgeRange): Int = when (range) {
    AgeRange.AGE_18_29 -> R.string.onboarding_age_18_29
    AgeRange.AGE_30_44 -> R.string.onboarding_age_30_44
    AgeRange.AGE_45_59 -> R.string.onboarding_age_45_59
    AgeRange.AGE_60_74 -> R.string.onboarding_age_60_74
    AgeRange.AGE_75_PLUS -> R.string.onboarding_age_75_plus
    AgeRange.PREFER_NOT_TO_SAY -> R.string.onboarding_age_none
}

private fun goalLabel(goal: Goal): Int = when (goal) {
    Goal.SLEEP -> R.string.onboarding_goal_sleep
    Goal.STRESS -> R.string.onboarding_goal_stress
    Goal.PEACE -> R.string.onboarding_goal_peace
    Goal.FOCUS -> R.string.onboarding_goal_focus
    Goal.DEEPER -> R.string.onboarding_goal_deeper
    Goal.ENERGY -> R.string.onboarding_goal_energy
}
