package com.example.carebrief.data

import androidx.room.withTransaction
import com.example.carebrief.core.database.AiInsightEntity
import com.example.carebrief.core.database.CareBriefDatabase
import com.example.carebrief.core.database.CarePlanEntity
import com.example.carebrief.core.database.TaskEntity
import com.example.carebrief.core.model.AiInsight
import com.example.carebrief.core.model.CarePlan
import com.example.carebrief.core.model.CarePlanStatus
import com.example.carebrief.core.model.CarePriority
import com.example.carebrief.core.model.CareTaskModel
import com.example.carebrief.core.model.InsightSeverity
import com.example.carebrief.core.model.TaskPriority
import com.example.carebrief.core.model.toCarePlanStatus
import com.example.carebrief.core.model.toCarePriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private const val LIST_SEP = "\u001F"

private fun List<String>.pack(): String = joinToString(LIST_SEP)
private fun String.unpack(): List<String> =
    if (isBlank()) emptyList() else split(LIST_SEP).map { it.trim() }.filter { it.isNotEmpty() }

// ---------------------------------------------------------------------------
// Mappers (entity <-> domain). Keep them pure for unit tests.
// ---------------------------------------------------------------------------

fun CarePlan.toEntity(now: Long = System.currentTimeMillis()): CarePlanEntity = CarePlanEntity(
    id = id,
    recipientId = recipientId,
    goal = goal,
    reason = reason,
    actionsRaw = actions.pack(),
    monitoringRaw = monitoringIndicators.pack(),
    priority = when (priority) {
        CarePriority.LOW -> "Low"; CarePriority.HIGH -> "High"; else -> "Normal"
    },
    status = status.name,
    reviewDateMillis = reviewDateMillis,
    reviewDateLabel = reviewDateLabel,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = now,
    approvedAtMillis = approvedAtMillis
)

fun CarePlanEntity.toModel(): CarePlan = CarePlan(
    id = id,
    recipientId = recipientId,
    goal = goal,
    reason = reason,
    actions = actionsRaw.unpack(),
    monitoringIndicators = monitoringRaw.unpack(),
    priority = priority.toCarePriority(),
    status = status.toCarePlanStatus(),
    reviewDateMillis = reviewDateMillis,
    reviewDateLabel = reviewDateLabel,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    approvedAtMillis = approvedAtMillis
)

fun CareTaskModel.toEntity(now: Long = System.currentTimeMillis()): TaskEntity = TaskEntity(
    id = id,
    carePlanId = carePlanId,
    recipientId = recipientId,
    title = title,
    description = description,
    dueDateMillis = dueDateMillis,
    dueLabel = dueLabel,
    priority = when (priority) {
        TaskPriority.LOW -> "Low"; TaskPriority.HIGH -> "High"; else -> "Normal"
    },
    category = category,
    frequency = frequency,
    completed = completed,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = now,
    sourceFingerprint = sourceFingerprint
)

fun TaskEntity.toModel(): CareTaskModel = CareTaskModel(
    id = id,
    carePlanId = carePlanId,
    recipientId = recipientId,
    title = title,
    description = description,
    dueDateMillis = dueDateMillis,
    dueLabel = dueLabel,
    priority = when (priority.trim().uppercase()) {
        "LOW" -> TaskPriority.LOW; "HIGH" -> TaskPriority.HIGH; else -> TaskPriority.NORMAL
    },
    category = category,
    frequency = frequency,
    completed = completed,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    sourceFingerprint = sourceFingerprint
)

fun AiInsight.toEntity(): AiInsightEntity = AiInsightEntity(
    id = id,
    recipientId = recipientId,
    title = title,
    description = description,
    evidence = evidence,
    frequency = frequency,
    frequencyCount = frequencyCount,
    frequencyTotal = frequencyTotal,
    severity = severity.name,
    createdAtMillis = createdAtMillis
)

fun AiInsightEntity.toModel(): AiInsight = AiInsight(
    id = id,
    recipientId = recipientId,
    title = title,
    description = description,
    evidence = evidence,
    frequency = frequency,
    frequencyCount = frequencyCount,
    frequencyTotal = frequencyTotal,
    severity = runCatching { InsightSeverity.valueOf(severity) }.getOrDefault(InsightSeverity.WATCH),
    createdAtMillis = createdAtMillis
)

// ---------------------------------------------------------------------------
// Repository seams — Flow-observable, suspending writes, offline-first.
// ---------------------------------------------------------------------------

interface CarePlanRepository {
    fun observeForRecipient(recipientId: String): Flow<List<CarePlan>>
    fun observeActiveForRecipient(recipientId: String): Flow<CarePlan?>
    suspend fun latestForRecipient(recipientId: String): CarePlan?
    suspend fun save(plan: CarePlan)
    suspend fun setStatus(planId: String, status: CarePlanStatus)
    suspend fun clear()
}

interface TaskRepository {
    fun observeForRecipient(recipientId: String): Flow<List<CareTaskModel>>
    fun observeForPlan(carePlanId: String): Flow<List<CareTaskModel>>
    suspend fun save(task: CareTaskModel)
    suspend fun saveAll(tasks: List<CareTaskModel>)
    suspend fun replaceForRecipient(recipientId: String, tasks: List<CareTaskModel>)
    suspend fun setCompleted(taskId: String, completed: Boolean)
    suspend fun delete(taskId: String)
    suspend fun countForRecipient(recipientId: String): Int
    suspend fun clear()
}

interface InsightRepository {
    fun observeForRecipient(recipientId: String): Flow<List<AiInsight>>
    suspend fun latestForRecipient(recipientId: String, limit: Int = 10): List<AiInsight>
    suspend fun saveAll(insights: List<AiInsight>)
    suspend fun clear()
}

class RoomCarePlanRepository(private val db: CareBriefDatabase) : CarePlanRepository {
    override fun observeForRecipient(recipientId: String): Flow<List<CarePlan>> =
        db.carePlanDao().observeForRecipient(recipientId).map { rows -> rows.map { it.toModel() } }

    override fun observeActiveForRecipient(recipientId: String): Flow<CarePlan?> =
        db.carePlanDao().observeActiveForRecipient(recipientId).map { it?.toModel() }

    override suspend fun latestForRecipient(recipientId: String): CarePlan? =
        db.carePlanDao().latestForRecipient(recipientId)?.toModel()

    override suspend fun save(plan: CarePlan) {
        db.carePlanDao().upsert(plan.toEntity())
    }

    override suspend fun setStatus(planId: String, status: CarePlanStatus) {
        db.carePlanDao().updateStatus(planId, status.name)
    }

    override suspend fun clear() = db.carePlanDao().clearAll()
}

class RoomTaskRepository(private val db: CareBriefDatabase) : TaskRepository {
    override fun observeForRecipient(recipientId: String): Flow<List<CareTaskModel>> =
        db.taskDao().observeForRecipient(recipientId).map { rows -> rows.map { it.toModel() } }

    override fun observeForPlan(carePlanId: String): Flow<List<CareTaskModel>> =
        db.taskDao().observeForPlan(carePlanId).map { rows -> rows.map { it.toModel() } }

    override suspend fun save(task: CareTaskModel) {
        db.taskDao().upsert(task.toEntity())
    }

    override suspend fun saveAll(tasks: List<CareTaskModel>) {
        db.taskDao().upsertAll(tasks.map { it.toEntity() })
    }

    override suspend fun replaceForRecipient(recipientId: String, tasks: List<CareTaskModel>) {
        db.withTransaction {
            db.taskDao().deleteForRecipient(recipientId)
            if (tasks.isNotEmpty()) db.taskDao().upsertAll(tasks.map { it.toEntity() })
        }
    }

    override suspend fun setCompleted(taskId: String, completed: Boolean) {
        db.taskDao().setCompleted(taskId, completed)
    }

    override suspend fun delete(taskId: String) = db.taskDao().deleteById(taskId)

    override suspend fun countForRecipient(recipientId: String): Int =
        db.taskDao().countForRecipient(recipientId)

    override suspend fun clear() = db.taskDao().clearAll()
}

class RoomInsightRepository(private val db: CareBriefDatabase) : InsightRepository {
    override fun observeForRecipient(recipientId: String): Flow<List<AiInsight>> =
        db.aiInsightDao().observeForRecipient(recipientId).map { rows -> rows.map { it.toModel() } }

    override suspend fun latestForRecipient(recipientId: String, limit: Int): List<AiInsight> =
        db.aiInsightDao().latestForRecipient(recipientId, limit).map { it.toModel() }

    override suspend fun saveAll(insights: List<AiInsight>) {
        db.aiInsightDao().upsertAll(insights.map { it.toEntity() })
    }

    override suspend fun clear() = db.aiInsightDao().clearAll()
}

/** Runtime Room adapters shared by stores that keep synchronous UI state flows. */
object PersistentRepositories {
    @Volatile var carePlans: CarePlanRepository? = null
    @Volatile var tasks: TaskRepository? = null
    @Volatile var insights: InsightRepository? = null
}

/** Factory for deterministic ids in tests/demo seeding. */
fun newCarePlanId(): String = UUID.randomUUID().toString()
fun newTaskId(): String = UUID.randomUUID().toString()
fun newInsightId(): String = UUID.randomUUID().toString()
