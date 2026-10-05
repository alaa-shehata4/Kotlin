package com.example.carebrief.data

import android.content.Context
import android.content.SharedPreferences
import com.example.carebrief.core.model.CarePlanDraft
import com.example.carebrief.core.model.CareTaskModel
import com.example.carebrief.core.model.TaskPriority
import com.example.carebrief.data.ai.AiCareAssistant
import com.example.carebrief.data.ai.AiProviders
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class CareTask(
    val id: String,
    val recipientId: String,
    val title: String,
    val category: String,
    val frequency: String,
    val dueLabel: String,
    val priority: String,
    val completed: Boolean,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long = 0L,
    val sourceFingerprint: String = "",
    val createdAtMillis: Long = updatedAtMillis
)

/**
 * Task module. Tasks are generated once per recipient from the current
 * care-plan draft via the AI seam, then completed/undone locally.
 */
class TaskStore(
    private val ai: AiCareAssistant = AiProviders.current()
) {
    private val flows = mutableMapOf<String, MutableStateFlow<List<CareTask>>>()
    private val lock = Any()
    @Volatile private var preferences: SharedPreferences? = null
    @Volatile private var roomRepository: TaskRepository? = null
    @Volatile private var persistenceScope: CoroutineScope? = null
    private val observingRecipients = mutableSetOf<String>()
    private val loadedRecipients = mutableSetOf<String>()
    private val pendingDrafts = mutableMapOf<String, CarePlanDraft>()

    fun restore(context: Context) = synchronized(lock) {
        preferences = context.getSharedPreferences("carebrief_tasks", Context.MODE_PRIVATE)
        preferences?.all?.forEach { (recipientId, raw) ->
            val tasks = runCatching { decodeTasks(raw as String) }.getOrNull() ?: return@forEach
            mutable(recipientId).value = tasks
        }
    }

    private fun mutable(recipientId: String): MutableStateFlow<List<CareTask>> = synchronized(lock) {
        flows.getOrPut(recipientId) { MutableStateFlow(emptyList()) }
    }

    fun observe(recipientId: String): StateFlow<List<CareTask>> {
        observeRoom(recipientId)
        return mutable(recipientId)
    }

    fun attach(repository: TaskRepository, scope: CoroutineScope) {
        roomRepository = repository
        persistenceScope = scope
        val recipientIds = synchronized(lock) { flows.keys.toList() } +
            (preferences?.all?.keys?.toList() ?: emptyList())
        recipientIds.distinct().forEach(::observeRoom)
    }

    private fun observeRoom(recipientId: String) {
        val repository = roomRepository ?: return
        val scope = persistenceScope ?: return
        if (!synchronized(lock) { observingRecipients.add(recipientId) }) return
        scope.launch {
            var firstEmission = true
            repository.observeForRecipient(recipientId).collect { stored ->
                val flow = mutable(recipientId)
                if (firstEmission) {
                    firstEmission = false
                    val cached = flow.value
                    if (stored.isNotEmpty()) {
                        flow.value = stored.map { it.toCareTask() }
                        preferences?.edit()?.remove(recipientId)?.apply()
                    } else if (cached.isNotEmpty()) {
                        if (runCatching { repository.replaceForRecipient(recipientId, cached.map { it.toModel() }) }.isSuccess) {
                            preferences?.edit()?.remove(recipientId)?.apply()
                        }
                    } else {
                        preferences?.edit()?.remove(recipientId)?.apply()
                    }
                    synchronized(lock) { loadedRecipients.add(recipientId) }
                    val pending = synchronized(lock) { pendingDrafts.remove(recipientId) }
                    if (pending != null) ensureGenerated(recipientId, pending)
                } else {
                    flow.value = stored.map { it.toCareTask() }
                }
            }
        }
    }

    /** Regenerates when the plan changes, preserving state for unchanged actions. */
    fun ensureGenerated(recipientId: String, draft: CarePlanDraft) {
        if (roomRepository != null && !synchronized(lock) { recipientId in loadedRecipients }) {
            synchronized(lock) { pendingDrafts[recipientId] = draft }
            observeRoom(recipientId)
            return
        }
        val flow = mutable(recipientId)
        val fingerprint = fingerprint(draft)
        if (flow.value.isNotEmpty() && flow.value.all { it.sourceFingerprint == fingerprint }) return
        val previous = flow.value.associateBy { it.title to it.frequency }
        val today = LocalDate.now()
        val generated = ai.generateTaskDrafts(draft).map { task ->
            val existing = previous[task.title to task.frequency]
            val dueDate = existing?.dueDateMillis?.takeIf { it > 0L } ?:
                today.plusDays(if (task.frequency == "Weekly") 7 else 0)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            CareTask(
                id = existing?.id ?: UUID.randomUUID().toString(),
                recipientId = recipientId,
                title = task.title,
                category = existing?.category ?: task.category,
                frequency = task.frequency,
                dueLabel = existing?.dueLabel ?: dueLabel(dueDate),
                priority = existing?.priority ?: "Normal",
                completed = existing?.completed ?: false,
                updatedAtMillis = existing?.updatedAtMillis ?: System.currentTimeMillis(),
                dueDateMillis = dueDate,
                sourceFingerprint = fingerprint,
                createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
            )
        }
        flow.value = generated
        persist(recipientId, generated)
    }

    fun toggle(recipientId: String, taskId: String) {
        val flow = mutable(recipientId)
        val task = flow.value.firstOrNull { it.id == taskId } ?: return
        val now = System.currentTimeMillis()
        val completed = !task.completed
        val updated = flow.value.map {
            if (it.id == taskId) it.copy(completed = completed, updatedAtMillis = now) else it
        }
        // Keep the completed occurrence visible, and add the next due occurrence
        // for recurring actions. Undoing a completion only reopens that occurrence.
        val nextDueDate = if (completed) nextOccurrence(task, LocalDate.now()) else null
        flow.value = if (nextDueDate == null) updated else updated + task.copy(
            id = UUID.randomUUID().toString(),
            completed = false,
            createdAtMillis = now,
            dueDateMillis = nextDueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            dueLabel = dueLabel(nextDueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()),
            updatedAtMillis = now
        )
        persist(recipientId, flow.value)
    }

    private fun nextOccurrence(task: CareTask, today: LocalDate): LocalDate? {
        val intervalDays = when (task.frequency.lowercase()) {
            "daily" -> 1L
            "weekly" -> 7L
            else -> return null
        }
        val dueDate = if (task.dueDateMillis > 0L) {
            Instant.ofEpochMilli(task.dueDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        } else today
        var nextDate = dueDate.plusDays(intervalDays)
        while (nextDate.isBefore(today)) nextDate = nextDate.plusDays(intervalDays)
        return nextDate
    }

    fun update(task: CareTask) {
        val flow = mutable(task.recipientId)
        flow.value = flow.value.map { if (it.id == task.id) task else it }
        persist(task.recipientId, flow.value)
    }

    /** Clears all generated tasks (used by Settings → Reset demo data). */
    fun clear() = synchronized(lock) {
        flows.values.forEach { it.value = emptyList() }
        preferences?.edit()?.clear()?.apply()
    }

    /**
     * Pure-Kotlin draft fingerprint (no org.json: Android framework JSON
     * throws "not mocked" in local JVM unit tests). Length-prefixing keeps
     * ["ab", "c"] distinct from ["a", "bc"].
     */
    private fun fingerprint(draft: CarePlanDraft): String =
        listOf(
            draft.goal,
            draft.reason,
            draft.actions.joinToString("\u001F"),
            draft.monitoring.joinToString("\u001F")
        ).joinToString("\u001E") { "${it.length}:$it" }

    private fun persist(recipientId: String, tasks: List<CareTask>) {
        val repository = roomRepository
        val scope = persistenceScope
        if (repository != null && scope != null) {
            scope.launch {
                val latest = mutable(recipientId).value
                runCatching {
                    repository.replaceForRecipient(recipientId, latest.map { it.toModel() })
                }
            }
            return
        }
        val prefs = preferences ?: return // unit tests / pre-restore: memory only
        prefs.edit().putString(recipientId, JSONArray().apply {
            tasks.forEach { task ->
                put(JSONObject().apply {
                    put("id", task.id)
                    put("recipientId", task.recipientId)
                    put("title", task.title)
                    put("category", task.category)
                    put("frequency", task.frequency)
                    put("dueLabel", task.dueLabel)
                    put("priority", task.priority)
                    put("completed", task.completed)
                    put("updatedAtMillis", task.updatedAtMillis)
                    put("createdAtMillis", task.createdAtMillis)
                    put("dueDateMillis", task.dueDateMillis)
                    put("sourceFingerprint", task.sourceFingerprint)
                })
            }
        }.toString())?.apply()
    }

    private fun decodeTasks(raw: String): List<CareTask> {
        val values = JSONArray(raw)
        return List(values.length()) { index ->
            val value = values.getJSONObject(index)
            CareTask(
                id = value.getString("id"),
                recipientId = value.getString("recipientId"),
                title = value.getString("title"),
                category = value.optString("category", "General"),
                frequency = value.optString("frequency", "Daily"),
                dueLabel = value.optString("dueLabel", "Today"),
                priority = value.optString("priority", "Normal"),
                completed = value.optBoolean("completed", false),
                updatedAtMillis = value.optLong("updatedAtMillis", System.currentTimeMillis()),
                dueDateMillis = value.optLong("dueDateMillis", 0L),
                sourceFingerprint = value.optString("sourceFingerprint", ""),
                createdAtMillis = value.optLong("createdAtMillis", value.optLong("updatedAtMillis", System.currentTimeMillis()))
            )
        }
    }

    private fun dueLabel(timestamp: Long): String {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return when (date) {
            LocalDate.now() -> "Today"
            LocalDate.now().plusDays(1) -> "Tomorrow"
            else -> date.format(DateTimeFormatter.ofPattern("d MMM"))
        }
    }

    private fun CareTask.toModel() = CareTaskModel(
        id = id,
        carePlanId = recipientId,
        recipientId = recipientId,
        title = title,
        description = "",
        dueDateMillis = dueDateMillis,
        dueLabel = dueLabel,
        priority = when (priority.trim().lowercase()) {
            "low" -> TaskPriority.LOW
            "high" -> TaskPriority.HIGH
            else -> TaskPriority.NORMAL
        },
        category = category,
        frequency = frequency,
        completed = completed,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        sourceFingerprint = sourceFingerprint
    )

    private fun CareTaskModel.toCareTask() = CareTask(
        id = id,
        recipientId = recipientId,
        title = title,
        category = category,
        frequency = frequency,
        dueLabel = dueLabel,
        priority = when (priority) {
            TaskPriority.LOW -> "Low"
            TaskPriority.HIGH -> "High"
            TaskPriority.NORMAL -> "Normal"
        },
        completed = completed,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        dueDateMillis = dueDateMillis,
        sourceFingerprint = sourceFingerprint
    )

    companion object {
        val shared = TaskStore()
    }
}
