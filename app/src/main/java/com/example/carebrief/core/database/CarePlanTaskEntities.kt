package com.example.carebrief.core.database

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Phase 27 — persistent care plans, tasks and AI analysis results.
 *
 * All tables stay local-first: no backend, fully observable via Flow,
 * all writes are suspending (never block the UI thread).
 */

// ---------------------------------------------------------------------------
// Care plans
// ---------------------------------------------------------------------------

@Entity(tableName = "care_plans")
data class CarePlanEntity(
    @PrimaryKey val id: String,
    val recipientId: String,
    val goal: String,
    val reason: String,
    /** Actions stored as unit-separator delimited text ( unlikely in prose). */
    val actionsRaw: String,
    val monitoringRaw: String,
    val priority: String,
    val status: String,
    val reviewDateMillis: Long = 0L,
    val reviewDateLabel: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val approvedAtMillis: Long = 0L
)

@Dao
interface CarePlanDao {
    @Query("SELECT * FROM care_plans WHERE recipientId = :recipientId ORDER BY updatedAtMillis DESC")
    fun observeForRecipient(recipientId: String): Flow<List<CarePlanEntity>>

    @Query("SELECT * FROM care_plans WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<CarePlanEntity?>

    @Query("SELECT * FROM care_plans WHERE recipientId = :recipientId AND status = :status ORDER BY updatedAtMillis DESC LIMIT 1")
    fun observeActiveForRecipient(recipientId: String, status: String = "ACTIVE"): Flow<CarePlanEntity?>

    @Query("SELECT * FROM care_plans WHERE recipientId = :recipientId ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun latestForRecipient(recipientId: String): CarePlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: CarePlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CarePlanEntity>)

    @Update
    suspend fun update(plan: CarePlanEntity)

    @Query("UPDATE care_plans SET status = :status, updatedAtMillis = :now WHERE id = :planId")
    suspend fun updateStatus(planId: String, status: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM care_plans WHERE recipientId = :recipientId")
    suspend fun deleteForRecipient(recipientId: String)

    @Query("DELETE FROM care_plans")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM care_plans")
    suspend fun count(): Int
}

// ---------------------------------------------------------------------------
// Tasks
// ---------------------------------------------------------------------------

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val carePlanId: String?,
    val recipientId: String,
    val title: String,
    val description: String = "",
    val dueDateMillis: Long = 0L,
    val dueLabel: String = "Today",
    val priority: String = "Normal",
    val category: String = "General",
    val frequency: String = "Daily",
    val completed: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val sourceFingerprint: String = ""
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE recipientId = :recipientId ORDER BY completed ASC, createdAtMillis ASC")
    fun observeForRecipient(recipientId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE carePlanId = :carePlanId ORDER BY completed ASC, createdAtMillis ASC")
    fun observeForPlan(carePlanId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE recipientId = :recipientId AND completed = 0")
    fun observePendingCount(recipientId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TaskEntity>)

    @Query("UPDATE tasks SET completed = :completed, updatedAtMillis = :now WHERE id = :taskId")
    suspend fun setCompleted(taskId: String, completed: Boolean, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String)

    @Query("DELETE FROM tasks WHERE recipientId = :recipientId")
    suspend fun deleteForRecipient(recipientId: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM tasks WHERE recipientId = :recipientId")
    suspend fun countForRecipient(recipientId: String): Int
}

// ---------------------------------------------------------------------------
// AI analysis results (persisted so they survive offline / process death)
// ---------------------------------------------------------------------------

@Entity(tableName = "ai_insights")
data class AiInsightEntity(
    @PrimaryKey val id: String,
    val recipientId: String,
    val title: String,
    val description: String,
    val evidence: String,
    val frequency: String,
    val frequencyCount: Int = 0,
    val frequencyTotal: Int = 0,
    val severity: String = "WATCH",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface AiInsightDao {
    @Query("SELECT * FROM ai_insights WHERE recipientId = :recipientId ORDER BY createdAtMillis DESC")
    fun observeForRecipient(recipientId: String): Flow<List<AiInsightEntity>>

    @Query("SELECT * FROM ai_insights WHERE recipientId = :recipientId ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun latestForRecipient(recipientId: String, limit: Int = 10): List<AiInsightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AiInsightEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AiInsightEntity)

    @Query("DELETE FROM ai_insights WHERE recipientId = :recipientId")
    suspend fun deleteForRecipient(recipientId: String)

    @Query("DELETE FROM ai_insights")
    suspend fun clearAll()
}
