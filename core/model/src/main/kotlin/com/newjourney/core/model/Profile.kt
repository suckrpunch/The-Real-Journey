package com.newjourney.core.model

/** What brings someone to the app. Chosen in the introduction; several may be chosen. */
enum class Goal {
    SLEEP, STRESS, PEACE, FOCUS, DEEPER, ENERGY;

    companion object {
        fun fromStored(names: Set<String>): Set<Goal> = names.mapNotNull { n -> entries.firstOrNull { it.name == n } }.toSet()
    }
}

/** An age range rather than an exact age: enough to suggest a gentle start, nothing more. */
enum class AgeRange {
    AGE_18_29, AGE_30_44, AGE_45_59, AGE_60_74, AGE_75_PLUS, PREFER_NOT_TO_SAY;

    companion object {
        fun fromStored(name: String?): AgeRange? = entries.firstOrNull { it.name == name }
    }
}

/** What the user told the app about themselves. Stored only on the phone. */
data class UserProfile(
    val name: String = "",
    val ageRange: AgeRange? = null,
    val goals: Set<Goal> = emptySet(),
) {
    /** The name as it should appear in a greeting, or null when none was given. */
    val displayName: String? get() = name.trim().takeIf { it.isNotEmpty() }

    companion object {
        const val MAX_NAME_LENGTH: Int = 30
        fun cleanName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(MAX_NAME_LENGTH).trim()
    }
}

/**
 * Suggestions after the introduction (and on Home), based on the user's goals.
 * Uses only the catalog, so new music is picked up automatically.
 */
object Recommendations {

    /** For each goal: the category to look in and its sub-collections in order of preference. */
    private val sources: Map<Goal, Pair<String, List<String>>> = mapOf(
        Goal.SLEEP to ("sleep" to listOf("deep-sleep", "before-bed", "night-meditation")),
        Goal.STRESS to ("relaxation" to listOf("stress", "anxiety", "inner-peace", "emotional-release")),
        Goal.PEACE to ("relaxation" to listOf("inner-peace", "emotional-release", "stress")),
        Goal.FOCUS to ("focus" to listOf("concentration", "clarity", "deep-focus", "creative")),
        Goal.DEEPER to ("deep" to listOf("spiritual", "mindfulness", "consciousness", "exploration")),
        Goal.ENERGY to ("chakras" to emptyList()),
    )

    /** Goals used when someone skipped the question: a calm, broad mix. */
    val DefaultGoals: Set<Goal> = setOf(Goal.PEACE, Goal.SLEEP, Goal.ENERGY)

    /**
     * Up to [count] tracks, taking turns between the chosen goals so each is represented.
     * Free tracks come first unless the listener has Premium, so a new listener can always press play.
     */
    fun pick(catalog: Catalog, goals: Set<Goal>, isPremium: Boolean, count: Int = 3): List<Track> {
        val chosen = Goal.entries.filter { it in goals.ifEmpty { DefaultGoals } }
        val perGoal = chosen.map { goal -> candidates(catalog, goal, isPremium).toMutableList() }
        val picked = LinkedHashMap<String, Track>()
        while (picked.size < count && perGoal.any { it.isNotEmpty() }) {
            for (list in perGoal) {
                while (list.isNotEmpty()) {
                    val track = list.removeAt(0)
                    if (track.id !in picked) {
                        picked[track.id] = track
                        break
                    }
                }
                if (picked.size >= count) break
            }
        }
        return picked.values.toList()
    }

    private fun candidates(catalog: Catalog, goal: Goal, isPremium: Boolean): List<Track> {
        val (categoryId, preferred) = sources.getValue(goal)
        val tracks = catalog.tracksInCategory(categoryId)
        return tracks.sortedWith(
            compareBy<Track>(
                { if (isPremium || it.tier == TrackTier.FREE) 0 else 1 },
                { preferred.indexOf(it.subcategoryId).let { i -> if (i < 0) preferred.size else i } },
                { tracks.indexOf(it) },
            ),
        )
    }

    /** A gentle first session: 5 minutes for older listeners or when age wasn't given, otherwise 10. */
    fun firstSessionMinutes(age: AgeRange?): Int = when (age) {
        AgeRange.AGE_18_29, AgeRange.AGE_30_44, AgeRange.AGE_45_59 -> 10
        else -> 5
    }
}
