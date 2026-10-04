package com.example.carebrief.data

import com.example.carebrief.core.model.DailyNote
import com.example.carebrief.data.ai.AiCareAssistant
import com.example.carebrief.data.ai.DemoAiCareAssistant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Editable care-plan module. The AI provides the starting draft;
 * the caregiver owns every field afterwards. Single seam for
 * display (phase 11), editing and approval (phase 12).
 */
data class EditableCarePlan(
    val recipientId: String,
    val goal: String,
    val reason: String,
    val actions: List<String>,
    val monitoring: List<String>,
    val priority: String,
    val reviewDateLabel: String,
    val status: String // DRAFT or ACTIVE
)

class CarePlanStore(
    private val ai: AiCareAssistant = DemoAiCareAssistant()
) {
    private val flows = mutableMapOf<String, MutableStateFlow<EditableCarePlan?>>()
    private val lock = Any()

    private fun mutable(recipientId: String): MutableStateFlow<EditableCarePlan?> = synchronized(lock) {
        flows.getOrPut(recipientId) { MutableStateFlow(null) }
    }

    fun observe(recipientId: String): StateFlow<EditableCarePlan?> = mutable(recipientId)

    fun defaultFor(recipientId: String, notes: List<DailyNote>): EditableCarePlan {
        val draft = ai.generateCarePlanDraft(notes)
        val reason = if (notes.isEmpty()) {
            "No notes recorded yet. This starter draft can be shaped as observations arrive. " +
                "Review before using. Not a diagnosis."
        } else {
            draft.reason
        }
        return EditableCarePlan(
            recipientId = recipientId,
            goal = draft.goal,
            reason = reason,
            actions = draft.actions,
            monitoring = draft.monitoring,
            priority = "Normal",
            reviewDateLabel = defaultReviewDate(),
            status = "DRAFT"
        )
    }

    fun currentOrDefault(recipientId: String, notes: List<DailyNote>): EditableCarePlan =
        observe(recipientId).value ?: defaultFor(recipientId, notes)

    fun save(plan: EditableCarePlan) {
        mutable(plan.recipientId).value = plan
    }

    fun approve(recipientId: String) {
        mutable(recipientId).update { it?.copy(status = "ACTIVE") }
    }

    private fun defaultReviewDate(): String = try {
        LocalDate.now().plusDays(7).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    } catch (_: Exception) {
        "In 7 days"
    }

    companion object {
        val shared = CarePlanStore()
    }
}
