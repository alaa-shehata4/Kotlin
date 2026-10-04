package com.example.carebrief.data

import com.example.carebrief.core.model.CarePlanDraft
import com.example.carebrief.data.ai.AiCareAssistant
import com.example.carebrief.data.ai.AiProviders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val updatedAtMillis: Long = System.currentTimeMillis()
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

    private fun mutable(recipientId: String): MutableStateFlow<List<CareTask>> = synchronized(lock) {
        flows.getOrPut(recipientId) { MutableStateFlow(emptyList()) }
    }

    fun observe(recipientId: String): StateFlow<List<CareTask>> = mutable(recipientId)

    /** Generates tasks from [draft] unless this recipient already has tasks. */
    fun ensureGenerated(recipientId: String, draft: CarePlanDraft) {
        val flow = mutable(recipientId)
        if (flow.value.isNotEmpty()) return
        flow.value = ai.generateTaskDrafts(draft).mapIndexed { index, task ->
            CareTask(
                id = UUID.randomUUID().toString(),
                recipientId = recipientId,
                title = task.title,
                category = task.category,
                frequency = task.frequency,
                dueLabel = if (task.frequency == "Weekly") "This week" else "Today",
                priority = if (index == 0) "High" else "Normal",
                completed = false
            )
        }
    }

    fun toggle(recipientId: String, taskId: String) {
        val flow = mutable(recipientId)
        flow.value = flow.value.map {
            if (it.id == taskId) it.copy(completed = !it.completed, updatedAtMillis = System.currentTimeMillis()) else it
        }
    }

    /** Clears all generated tasks (used by Settings → Reset demo data). */
    fun clear() = synchronized(lock) {
        flows.values.forEach { it.value = emptyList() }
    }

    companion object {
        val shared = TaskStore()
    }
}
