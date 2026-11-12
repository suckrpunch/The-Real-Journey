package com.newjourney.core.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Entity(tableName = "meditation_session")
internal data class MeditationSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMs: Long,
    val plannedSeconds: Int,
    val completedSeconds: Int,
    val completed: Boolean,
    /** "timer", "music" (timer with a track) or "listen" (a track played to the end). */
    val type: String = "timer",
    /** The track that played, if any. Added in version 2. */
    val trackId: String? = null,
)

@Entity(tableName = "favorite_track")
internal data class FavoriteTrackEntity(
    @PrimaryKey val trackId: String,
    val savedAtEpochMs: Long,
)

@Entity(tableName = "playback_history")
internal data class PlaybackHistoryEntity(
    @PrimaryKey val trackId: String,
    val lastPlayedAtEpochMs: Long,
    /** Where the listener was, so "Continue listening" picks up there. */
    val positionMs: Long,
)

@Entity(tableName = "routine")
internal data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val minutes: Int,
    val startBell: Boolean,
    val endBell: Boolean,
    /** A [com.newjourney.core.model.BackgroundSound] name. */
    val sound: String,
    val createdAtEpochMs: Long,
)

@Dao
internal interface MeditationSessionDao {
    @Insert
    suspend fun insert(session: MeditationSessionEntity)

    @Query("SELECT * FROM meditation_session WHERE startedAtEpochMs >= :fromEpochMs ORDER BY startedAtEpochMs")
    fun sessionsSince(fromEpochMs: Long): Flow<List<MeditationSessionEntity>>

    @Query("SELECT COUNT(*) FROM meditation_session WHERE completed = 1")
    fun completedCount(): Flow<Int>
}

@Dao
internal interface LibraryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_track WHERE trackId = :trackId")
    suspend fun removeFavorite(trackId: String)

    @Query("SELECT trackId FROM favorite_track ORDER BY savedAtEpochMs DESC")
    fun favoriteIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_track WHERE trackId = :trackId)")
    suspend fun isFavorite(trackId: String): Boolean

    @Upsert
    suspend fun upsertHistory(entry: PlaybackHistoryEntity)

    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAtEpochMs DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<PlaybackHistoryEntity>>
}

@Dao
internal interface RoutineDao {
    @Query("SELECT * FROM routine ORDER BY createdAtEpochMs, id")
    fun all(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine WHERE id = :id")
    suspend fun get(id: Long): RoutineEntity?

    /** Returns the new row id, or -1 when an existing row was updated. */
    @Upsert
    suspend fun upsert(routine: RoutineEntity): Long

    @Query("DELETE FROM routine WHERE id = :id")
    suspend fun delete(id: Long)
}

/**
 * The app's main on-device database. Journal entries get their own encrypted database later,
 * so private writing is kept separate from session statistics.
 */
@Database(
    entities = [
        MeditationSessionEntity::class,
        FavoriteTrackEntity::class,
        PlaybackHistoryEntity::class,
        RoutineEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
internal abstract class NewJourneyDatabase : RoomDatabase() {
    abstract fun sessionDao(): MeditationSessionDao
    abstract fun libraryDao(): LibraryDao
    abstract fun routineDao(): RoutineDao
}

/** Version 1 → 2: adds favourites and listening history, and the track of a session. Keeps all sessions. */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `meditation_session` ADD COLUMN `trackId` TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `favorite_track` (`trackId` TEXT NOT NULL, " +
                "`savedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`trackId`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playback_history` (`trackId` TEXT NOT NULL, " +
                "`lastPlayedAtEpochMs` INTEGER NOT NULL, `positionMs` INTEGER NOT NULL, PRIMARY KEY(`trackId`))",
        )
    }
}

/** Version 2 → 3: adds saved routines. Keeps everything else. */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `routine` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `minutes` INTEGER NOT NULL, `startBell` INTEGER NOT NULL, " +
                "`endBell` INTEGER NOT NULL, `sound` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL)",
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NewJourneyDatabase =
        Room.databaseBuilder(context, NewJourneyDatabase::class.java, "new_journey.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideSessionDao(database: NewJourneyDatabase): MeditationSessionDao = database.sessionDao()

    @Provides
    fun provideLibraryDao(database: NewJourneyDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun provideRoutineDao(database: NewJourneyDatabase): RoutineDao = database.routineDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    abstract fun bindSessionRepository(impl: RoomSessionRepository): SessionRepository

    @Binds
    abstract fun bindLibraryRepository(impl: RoomLibraryRepository): LibraryRepository

    @Binds
    abstract fun bindRoutineRepository(impl: RoomRoutineRepository): RoutineRepository
}
