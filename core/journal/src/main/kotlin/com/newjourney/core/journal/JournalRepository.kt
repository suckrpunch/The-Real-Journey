package com.newjourney.core.journal

import com.newjourney.core.model.JournalEntry
import com.newjourney.core.model.Mood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

interface JournalRepository {
    /** All entries, newest first. */
    val entries: Flow<List<JournalEntry>>

    fun search(query: String): Flow<List<JournalEntry>>

    suspend fun entry(id: Long): JournalEntry?

    /** Saves a new entry (id 0) or updates an existing one. Returns its id. */
    suspend fun save(entry: JournalEntry): Long

    suspend fun delete(id: Long)
}

@Singleton
internal class RoomJournalRepository @Inject constructor(
    // A Provider, so the encrypted database is only opened when the journal is first used.
    private val dao: Provider<JournalDao>,
) : JournalRepository {

    override val entries: Flow<List<JournalEntry>>
        get() = dao.get().all().map { rows -> rows.map { it.toModel() } }

    override fun search(query: String): Flow<List<JournalEntry>> =
        dao.get().search(escapeLike(query.trim())).map { rows -> rows.map { it.toModel() } }

    override suspend fun entry(id: Long): JournalEntry? = dao.get().byId(id)?.toModel()

    override suspend fun save(entry: JournalEntry): Long {
        val now = Instant.now()
        return if (entry.id == 0L) {
            dao.get().insert(entry.copy(updatedAt = now).toEntity())
        } else {
            dao.get().update(entry.copy(updatedAt = now).toEntity())
            entry.id
        }
    }

    override suspend fun delete(id: Long) = dao.get().delete(id)

    /** % and _ are wildcards in SQL LIKE; remove them so a typed % or _ cannot match everything. */
    private fun escapeLike(query: String): String = query.replace("%", "").replace("_", " ")
}

private fun JournalEntryEntity.toModel() = JournalEntry(
    id = id,
    createdAt = Instant.ofEpochMilli(createdAtEpochMs),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMs),
    mood = mood.toMood(),
    moodBefore = moodBefore.toMood(),
    text = text,
    sessionMinutes = sessionMinutes,
)

private fun JournalEntry.toEntity() = JournalEntryEntity(
    id = id,
    createdAtEpochMs = createdAt.toEpochMilli(),
    updatedAtEpochMs = updatedAt.toEpochMilli(),
    mood = mood?.name,
    moodBefore = moodBefore?.name,
    text = text,
    sessionMinutes = sessionMinutes,
)

private fun String?.toMood(): Mood? = this?.let { name -> Mood.entries.firstOrNull { it.name == name } }
