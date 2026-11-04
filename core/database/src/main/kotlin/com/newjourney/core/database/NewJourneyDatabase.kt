package com.newjourney.core.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Entity(tableName = "meditation_session")
internal data class MeditationSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMs: Long,
    val plannedSeconds: Int,
    val completedSeconds: Int,
    val completed: Boolean,
    /** "timer" for now; "track" and "routine" join later. */
    val type: String = "timer",
)

@Dao
internal interface MeditationSessionDao {
    @Insert
    suspend fun insert(session: MeditationSessionEntity)

    @Query("SELECT * FROM meditation_session WHERE startedAtEpochMs >= :fromEpochMs ORDER BY startedAtEpochMs")
    fun sessionsSince(fromEpochMs: Long): kotlinx.coroutines.flow.Flow<List<MeditationSessionEntity>>

    @Query("SELECT COUNT(*) FROM meditation_session WHERE completed = 1")
    fun completedCount(): kotlinx.coroutines.flow.Flow<Int>
}

/**
 * The app's main on-device database. Journal entries get their own encrypted database later,
 * so private writing is kept separate from session statistics.
 */
@Database(entities = [MeditationSessionEntity::class], version = 1, exportSchema = true)
internal abstract class NewJourneyDatabase : RoomDatabase() {
    abstract fun sessionDao(): MeditationSessionDao
}

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NewJourneyDatabase =
        Room.databaseBuilder(context, NewJourneyDatabase::class.java, "new_journey.db").build()

    @Provides
    fun provideSessionDao(database: NewJourneyDatabase): MeditationSessionDao = database.sessionDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SessionRepositoryModule {
    @Binds
    abstract fun bindSessionRepository(impl: RoomSessionRepository): SessionRepository
}
