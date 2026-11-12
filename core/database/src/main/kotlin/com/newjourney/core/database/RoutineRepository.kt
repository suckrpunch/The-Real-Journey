package com.newjourney.core.database

import com.newjourney.core.model.BackgroundSound
import com.newjourney.core.model.Routine
import com.newjourney.core.model.TimerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The user's saved routines, oldest first, so their order stays where they put it. */
interface RoutineRepository {
    val routines: Flow<List<Routine>>
    suspend fun get(id: Long): Routine?

    /** Adds the routine, or replaces it when [Routine.id] is not 0. Returns its id. */
    suspend fun save(routine: Routine): Long
    suspend fun delete(id: Long)
}

@Singleton
internal class RoomRoutineRepository @Inject constructor(
    private val dao: RoutineDao,
) : RoutineRepository {

    override val routines: Flow<List<Routine>> = dao.all().map { rows -> rows.map { it.toModel() } }

    override suspend fun get(id: Long): Routine? = dao.get(id)?.toModel()

    override suspend fun save(routine: Routine): Long {
        val existing = if (routine.id != 0L) dao.get(routine.id) else null
        return dao.upsert(
            RoutineEntity(
                id = routine.id,
                name = routine.name,
                minutes = routine.minutes,
                startBell = routine.settings.startBell,
                endBell = routine.settings.endBell,
                sound = routine.settings.sound.name,
                createdAtEpochMs = existing?.createdAtEpochMs ?: System.currentTimeMillis(),
            ),
        ).let { if (it == -1L) routine.id else it }
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    private fun RoutineEntity.toModel() = Routine(
        id = id,
        name = name,
        minutes = minutes,
        settings = TimerSettings(
            startBell = startBell,
            endBell = endBell,
            sound = BackgroundSound.entries.firstOrNull { it.name == sound } ?: BackgroundSound.SILENCE,
        ),
    )
}
