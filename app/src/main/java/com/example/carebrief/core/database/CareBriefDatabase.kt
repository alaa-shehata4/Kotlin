package com.example.carebrief.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    val initials: String,
    /** Phase 26/27: canonical creation timestamp. 0 = seeded legacy row. */
    val createdAtMillis: Long = 0L,
    /** Phase 26/27: optional avatar reference. Null = initials avatar. */
    val avatarUri: String? = null
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
    val categoriesCsv: String,
    val mood: String? = null,
    val mobility: String? = null,
    val appetite: String? = null,
    val sleep: String? = null
)

@Dao
interface RecipientDao {
    @Query("SELECT * FROM recipients ORDER BY name")
    fun observeAll(): Flow<List<RecipientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RecipientEntity>)

    @Query("SELECT COUNT(*) FROM recipients")
    suspend fun count(): Int

    @Query("UPDATE recipients SET lastNoteLabel = :label WHERE id = :recipientId")
    suspend fun updateLastNote(recipientId: String, label: String)

    @Query("UPDATE recipients SET planStatus = :status WHERE id = :recipientId")
    suspend fun updatePlanStatus(recipientId: String, status: String)

    @Query("DELETE FROM recipients")
    suspend fun clearAll()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE recipientId = :recipientId ORDER BY timestamp DESC")
    fun observeForRecipient(recipientId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NoteEntity>)

    @Query("DELETE FROM notes")
    suspend fun clearAll()
}

@Database(
    entities = [
        RecipientEntity::class,
        NoteEntity::class,
        CarePlanEntity::class,
        TaskEntity::class,
        AiInsightEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class CareBriefDatabase : RoomDatabase() {
    abstract fun recipientDao(): RecipientDao
    abstract fun noteDao(): NoteDao
    abstract fun carePlanDao(): CarePlanDao
    abstract fun taskDao(): TaskDao
    abstract fun aiInsightDao(): AiInsightDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN mood TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN mobility TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN appetite TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN sleep TEXT")
            }
        }

        /**
         * Phase 27: recipients gain createdAt/avatar columns; new tables for
         * care plans, tasks and persisted AI insights. All async via Room;
         * callers observe Flow and never block the UI thread.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipients ADD COLUMN createdAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE recipients ADD COLUMN avatarUri TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `care_plans` (" +
                        "`id` TEXT NOT NULL, `recipientId` TEXT NOT NULL, " +
                        "`goal` TEXT NOT NULL, `reason` TEXT NOT NULL, " +
                        "`actionsRaw` TEXT NOT NULL, `monitoringRaw` TEXT NOT NULL, " +
                        "`priority` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                        "`reviewDateMillis` INTEGER NOT NULL, `reviewDateLabel` TEXT NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_care_plans_recipientId` ON `care_plans` (`recipientId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tasks` (" +
                        "`id` TEXT NOT NULL, `carePlanId` TEXT, `recipientId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                        "`dueDateMillis` INTEGER NOT NULL, `dueLabel` TEXT NOT NULL, " +
                        "`priority` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                        "`frequency` TEXT NOT NULL, `completed` INTEGER NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_recipientId` ON `tasks` (`recipientId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_carePlanId` ON `tasks` (`carePlanId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ai_insights` (" +
                        "`id` TEXT NOT NULL, `recipientId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                        "`evidence` TEXT NOT NULL, `frequency` TEXT NOT NULL, " +
                        "`frequencyCount` INTEGER NOT NULL, `frequencyTotal` INTEGER NOT NULL, " +
                        "`severity` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_insights_recipientId` ON `ai_insights` (`recipientId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE care_plans ADD COLUMN approvedAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN sourceFingerprint TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
