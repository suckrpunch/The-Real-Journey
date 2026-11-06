package com.newjourney.core.journal

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Entity(tableName = "journal_entry")
internal data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    /** Mood names as stored text, for example "PEACEFUL". */
    val mood: String?,
    val moodBefore: String?,
    val text: String,
    val sessionMinutes: Int?,
)

@Dao
internal interface JournalDao {
    @Insert
    suspend fun insert(entry: JournalEntryEntity): Long

    @Update
    suspend fun update(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entry WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM journal_entry WHERE id = :id")
    suspend fun byId(id: Long): JournalEntryEntity?

    @Query("SELECT * FROM journal_entry ORDER BY createdAtEpochMs DESC")
    fun all(): Flow<List<JournalEntryEntity>>

    /** Case-insensitive search in the written text. */
    @Query("SELECT * FROM journal_entry WHERE text LIKE '%' || :query || '%' ORDER BY createdAtEpochMs DESC")
    fun search(query: String): Flow<List<JournalEntryEntity>>
}

/**
 * The journal lives in its own database file, encrypted as a whole with SQLCipher, apart from
 * the session statistics. Nothing here is ever sent anywhere.
 */
@Database(entities = [JournalEntryEntity::class], version = 1, exportSchema = true)
internal abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}

@Module
@InstallIn(SingletonComponent::class)
internal object JournalDatabaseModule {
    @Provides
    @Singleton
    fun provideJournalDatabase(@ApplicationContext context: Context): JournalDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(JournalKey(context).passphrase())
        return Room.databaseBuilder(context, JournalDatabase::class.java, "journal.db")
            .openHelperFactory(factory)
            .build()
    }

    @Provides
    fun provideJournalDao(database: JournalDatabase): JournalDao = database.journalDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class JournalRepositoryModule {
    @Binds
    abstract fun bindJournalRepository(impl: RoomJournalRepository): JournalRepository
}
