package com.newjourney.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.newjourney.core.billing.EntitlementRepository
import com.newjourney.core.catalog.CatalogRepository
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.model.AgeRange
import com.newjourney.core.model.Goal
import com.newjourney.core.model.Recommendations
import com.newjourney.core.model.Track
import com.newjourney.core.model.TrackTier
import com.newjourney.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A suggested track with where it lives in the library, for example "Sleep · Deep sleep". */
data class Pick(val track: Track, val place: String, val colorHex: String?, val premium: Boolean)

/** The introduction, one question per screen. */
enum class OnboardingStep { WELCOME, NAME, AGE, GOALS, PICKS }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val name: String = "",
    val ageRange: AgeRange? = null,
    val goals: Set<Goal> = emptySet(),
    /** Opened from Profile to change the answers, rather than on first launch. */
    val editing: Boolean = false,
    val picks: List<Pick> = emptyList(),
    val firstSessionMinutes: Int = 5,
) {
    /** Steps that show "Step n of 3". */
    val questionNumber: Int? get() = when (step) {
        OnboardingStep.NAME -> 1
        OnboardingStep.AGE -> 2
        OnboardingStep.GOALS -> 3
        else -> null
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val settings: UserSettingsRepository,
    catalog: CatalogRepository,
    entitlements: EntitlementRepository,
) : ViewModel() {

    private val editing = savedStateHandle.toRoute<OnboardingRoute>().editing

    private val step = savedStateHandle.getStateFlow(
        KEY_STEP,
        if (editing) OnboardingStep.NAME.name else OnboardingStep.WELCOME.name,
    )
    private val name = savedStateHandle.getStateFlow(KEY_NAME, "")
    private val age = savedStateHandle.getStateFlow<String?>(KEY_AGE, null)
    private val goals = savedStateHandle.getStateFlow(KEY_GOALS, arrayListOf<String>())

    init {
        // When changing earlier answers, start from what was saved.
        if (editing && savedStateHandle.get<Boolean>(KEY_LOADED) != true) {
            viewModelScope.launch {
                val profile = settings.userProfile.first()
                savedStateHandle[KEY_NAME] = profile.name
                savedStateHandle[KEY_AGE] = profile.ageRange?.name
                savedStateHandle[KEY_GOALS] = ArrayList(profile.goals.map { it.name })
                savedStateHandle[KEY_LOADED] = true
            }
        }
    }

    val uiState: StateFlow<OnboardingUiState> = combine(
        combine(step, name, age) { s, n, a -> Triple(s, n, a) },
        goals,
        catalog.catalog,
        entitlements.entitlement,
    ) { (s, n, a), g, cat, entitlement ->
        val chosenGoals = Goal.fromStored(g.toSet())
        val ageRange = AgeRange.fromStored(a)
        OnboardingUiState(
            step = OnboardingStep.valueOf(s),
            name = n,
            ageRange = ageRange,
            goals = chosenGoals,
            editing = editing,
            picks = cat?.let { c ->
                Recommendations.pick(c, chosenGoals, entitlement.isPremium).map { track ->
                    val category = c.category(track.categoryId)
                    val sub = category?.subcategories?.firstOrNull { it.id == track.subcategoryId }
                    Pick(
                        track = track,
                        place = listOfNotNull(category?.name, sub?.name).joinToString(" · "),
                        colorHex = sub?.color,
                        premium = track.tier == TrackTier.PREMIUM && !entitlement.isPremium,
                    )
                }
            }.orEmpty(),
            firstSessionMinutes = Recommendations.firstSessionMinutes(ageRange),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState(editing = editing))

    fun setName(value: String) {
        savedStateHandle[KEY_NAME] = value.take(UserProfile.MAX_NAME_LENGTH)
    }

    fun setAge(value: AgeRange) {
        savedStateHandle[KEY_AGE] = value.name
    }

    fun toggleGoal(goal: Goal) {
        val current = goals.value.toMutableSet()
        if (!current.remove(goal.name)) current.add(goal.name)
        savedStateHandle[KEY_GOALS] = ArrayList(current)
    }

    /** Moves on. After the goals the answers are saved, so the suggestions screen can be left any way. */
    fun next() {
        val current = OnboardingStep.valueOf(step.value)
        if (current == OnboardingStep.GOALS) saveAnswers()
        OnboardingStep.entries.getOrNull(current.ordinal + 1)?.let { savedStateHandle[KEY_STEP] = it.name }
    }

    /** Returns true when there was an earlier step to go back to. */
    fun back(): Boolean {
        val current = OnboardingStep.valueOf(step.value)
        val first = if (editing) OnboardingStep.NAME else OnboardingStep.WELCOME
        if (current == first || current == OnboardingStep.PICKS) return false
        savedStateHandle[KEY_STEP] = OnboardingStep.entries[current.ordinal - 1].name
        return true
    }

    /** Skips the whole introduction; nothing is asked again. */
    fun skipAll(onDone: () -> Unit) {
        viewModelScope.launch {
            settings.setIntroductionDone()
            onDone()
        }
    }

    private fun saveAnswers() {
        val profile = UserProfile(
            name = name.value,
            ageRange = AgeRange.fromStored(age.value),
            goals = Goal.fromStored(goals.value.toSet()),
        )
        viewModelScope.launch {
            settings.setUserProfile(profile)
            settings.setIntroductionDone()
        }
    }

    private companion object {
        const val KEY_STEP = "onboarding_step"
        const val KEY_NAME = "onboarding_name"
        const val KEY_AGE = "onboarding_age"
        const val KEY_GOALS = "onboarding_goals"
        const val KEY_LOADED = "onboarding_loaded"
    }
}
