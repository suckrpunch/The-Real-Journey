package com.newjourney.core.database

import com.newjourney.core.model.MeditationSession
import com.newjourney.core.model.SessionKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Saves finished meditations and answers questions about them. The source for streaks and progress. */
interface SessionRepository {
    suspend fun record(session: MeditationSession)
    fun sessionsSince(from: Instant): Flow<List<MeditationSession>>
    fun completedSessionCount(): Flow<Int>
}

@Singleton
internal class RoomSessionRepository @Inject constructor(
    private val dao: MeditationSessionDao,
) : SessionRepository {

    override suspend fun record(session: MeditationSession) {
        dao.insert(
            MeditationSessionEntity(
                startedAtEpochMs = session.startedAt.toEpochMilli(),
                plannedSeconds = session.plannedSeconds,
                completedSeconds = session.completedSeconds,
                completed = session.completed,
                type = session.kind.storedName,
                trackId = session.trackId,
            ),
        )
    }

    override fun sessionsSince(from: Instant): Flow<List<MeditationSession>> =
        dao.sessionsSince(from.toEpochMilli()).map { rows ->
            rows.map {
                MeditationSession(
                    startedAt = Instant.ofEpochMilli(it.startedAtEpochMs),
                    plannedSeconds = it.plannedSeconds,
                    completedSeconds = it.completedSeconds,
                    completed = it.completed,
                    kind = SessionKind.fromStored(it.type),
                    trackId = it.trackId,
                )
            }
        }

    override fun completedSessionCount(): Flow<Int> = dao.completedCount()
}
