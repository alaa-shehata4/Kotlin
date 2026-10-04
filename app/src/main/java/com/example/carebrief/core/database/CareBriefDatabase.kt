package com.example.carebrief.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recipients")
data class RecipientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val careStatus: String,
    val planStatus: String,
    val lastNoteLabel: String,
    val pendingTasks: Int,
    val initials: String
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val recipientId: String,
    val timestamp: Long,
    val dayLabel: String,
    val timeLabel: String,
    val author: String,
    val content: String,
    val categoriesCsv: String
)

@Dao
interface RecipientDao {
    @Query("SELECT * FROM recipients ORDER BY name")
    fun observeAll(): Flow<List<RecipientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RecipientEntity>)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE recipientId = :recipientId ORDER BY timestamp DESC")
    fun observeForRecipient(recipientId: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)
}

@Database(entities = [RecipientEntity::class, NoteEntity::class], version = 1, exportSchema = false)
abstract class CareBriefDatabase : RoomDatabase() {
    abstract fun recipientDao(): RecipientDao
    abstract fun noteDao(): NoteDao
}
