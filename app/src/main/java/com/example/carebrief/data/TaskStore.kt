package com.example.carebrief.data

import android.content.Context
import android.content.SharedPreferences
import com.example.carebrief.core.model.CarePlanDraft
import com.example.carebrief.data.ai.AiCareAssistant
import com.example.carebrief.data.ai.AiProviders
import org.json.JSONArray
import org.json.JSONObject
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
    val sourceFingerprint: String = ""
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

    fun observe(recipientId: String): StateFlow<List<CareTask>> = mutable(recipientId)

    /** Regenerates when the plan changes, preserving state for unchanged actions. */
    fun ensureGenerated(recipientId: String, draft: CarePlanDraft) {
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
                sourceFingerprint = fingerprint
            )
        }
        flow.value = generated
        persist(recipientId, generated)
    }

    fun toggle(recipientId: String, taskId: String) {
        val flow = mutable(recipientId)
        flow.value = flow.value.map {
            if (it.id == taskId) it.copy(completed = !it.completed, updatedAtMillis = System.currentTimeMillis()) else it
        }
        persist(recipientId, flow.value)
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
                sourceFingerprint = value.optString("sourceFingerprint", "")
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

    companion object {
        val shared = TaskStore()
    }
}
