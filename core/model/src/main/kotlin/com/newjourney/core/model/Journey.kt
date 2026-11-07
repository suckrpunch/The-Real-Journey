package com.newjourney.core.model

import java.time.Instant

/**
 * The six stages of The Journey Within. A stage is reached by the total number of meditations,
 * never by days in a row, so the journey only moves forward. Stages 1 and 2 are free; stage 3 can
 * be previewed by everyone; stages 4–6 open fully with Premium. Progress itself counts for everyone.
 */
enum class JourneyStage(val sessionsToArrive: Int, val access: Access) {
    AWAKENING(1, Access.FREE),
    FOUNDATION(7, Access.FREE),
    INNER_BALANCE(20, Access.PREVIEW),
    PATH_OF_AWARENESS(50, Access.PREMIUM),
    DEEPER_SELF(100, Access.PREMIUM),
    INNER_SANCTUARY(200, Access.PREMIUM),
    ;

    enum class Access {
        FREE,

        /** Everyone can read the reflection; the rest opens with Premium. */
        PREVIEW,
        PREMIUM,
    }

    /** Whether this stage's reflection can be read. */
    fun canRead(isPremium: Boolean): Boolean = isPremium || access != Access.PREMIUM
}

data class StageProgress(
    val stage: JourneyStage,
    /** When the stage was reached: the start of the session that reached it. null = still ahead. */
    val arrivedAt: Instant?,
) {
    val arrived: Boolean get() = arrivedAt != null
}

data class JourneyProgress(
    val sessions: Int,
    val stages: List<StageProgress>,
) {
    /** The furthest stage reached, or null before the first meditation. */
    val current: JourneyStage? get() = stages.lastOrNull { it.arrived }?.stage

    val next: JourneyStage? get() = stages.firstOrNull { !it.arrived }?.stage

    /** 0..1 along the way from the current stage to the next. */
    val towardsNext: Float
        get() {
            val nextStage = next ?: return 1f
            val from = current?.sessionsToArrive ?: 0
            return ((sessions - from).toFloat() / (nextStage.sessionsToArrive - from)).coerceIn(0f, 1f)
        }

    /** Stages reached by the session that started at [sessionStart]. */
    fun reachedBy(sessionStart: Instant): List<JourneyStage> =
        stages.filter { it.arrivedAt == sessionStart }.map { it.stage }

    companion object {
        fun from(sessions: List<MeditationSession>): JourneyProgress {
            val sorted = sessions.sortedBy { it.startedAt }
            return JourneyProgress(
                sessions = sorted.size,
                stages = JourneyStage.entries.map { stage ->
                    StageProgress(stage, sorted.getOrNull(stage.sessionsToArrive - 1)?.startedAt)
                },
            )
        }
    }
}

/** Words and colour for a stage, from journey.json so they can be rewritten without code changes. */
data class StageContent(
    val stage: JourneyStage,
    val name: String,
    /** One line under the name, for example "Where every journey begins". */
    val tagline: String,
    /** The reflection that opens when the stage is reached. Paragraphs separated by blank lines. */
    val reflection: String,
    /** What opens at this stage, in plain words. */
    val opens: String,
    /** "#RRGGBB" for the stage's artwork. */
    val color: String,
)
