package com.newjourney.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class RecentTrack(val trackId: String, val lastPlayedAtEpochMs: Long, val positionMs: Long)

/** Favourite tracks and listening history, kept on the phone. Synced to the account later. */
interface LibraryRepository {
    val favoriteIds: Flow<List<String>>
    suspend fun setFavorite(trackId: String, favorite: Boolean)
    suspend fun isFavorite(trackId: String): Boolean

    fun recent(limit: Int = 10): Flow<List<RecentTrack>>
    suspend fun rememberPosition(trackId: String, positionMs: Long)
}

@Singleton
internal class RoomLibraryRepository @Inject constructor(
    private val dao: LibraryDao,
) : LibraryRepository {

    override val favoriteIds: Flow<List<String>> = dao.favoriteIds()

    override suspend fun setFavorite(trackId: String, favorite: Boolean) {
        if (favorite) {
            dao.addFavorite(FavoriteTrackEntity(trackId, System.currentTimeMillis()))
        } else {
            dao.removeFavorite(trackId)
        }
    }

    override suspend fun isFavorite(trackId: String): Boolean = dao.isFavorite(trackId)

    override fun recent(limit: Int): Flow<List<RecentTrack>> = dao.recent(limit).map { rows ->
        rows.map { RecentTrack(it.trackId, it.lastPlayedAtEpochMs, it.positionMs) }
    }

    override suspend fun rememberPosition(trackId: String, positionMs: Long) {
        dao.upsertHistory(PlaybackHistoryEntity(trackId, System.currentTimeMillis(), positionMs.coerceAtLeast(0)))
    }
}
