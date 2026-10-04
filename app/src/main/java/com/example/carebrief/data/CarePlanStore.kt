package com.example.carebrief.data

import android.content.Context
import android.content.SharedPreferences
import com.example.carebrief.core.model.DailyNote
import com.example.carebrief.data.ai.AiCareAssistant
import com.example.carebrief.data.ai.AiProviders
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val status: String, // DRAFT or ACTIVE
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val reviewDateMillis: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val approvedAtMillis: Long = 0L
)

class CarePlanStore(
    private val ai: AiCareAssistant = AiProviders.current()
) {
    private val flows = mutableMapOf<String, MutableStateFlow<EditableCarePlan?>>()
    private val lock = Any()
    @Volatile private var preferences: SharedPreferences? = null

    fun restore(context: Context) = synchronized(lock) {
        preferences = context.getSharedPreferences("carebrief_care_plans", Context.MODE_PRIVATE)
        preferences?.all?.forEach { (recipientId, raw) ->
            val plan = runCatching { decodePlan(raw as String) }.getOrNull() ?: return@forEach
            mutable(recipientId).value = plan
        }
    }

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
            status = "DRAFT",
            reviewDateMillis = LocalDate.now().plusDays(7)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    fun currentOrDefault(recipientId: String, notes: List<DailyNote>): EditableCarePlan =
        observe(recipientId).value ?: defaultFor(recipientId, notes)

    fun save(plan: EditableCarePlan) {
        val previous = mutable(plan.recipientId).value
        val now = System.currentTimeMillis()
        val saved = plan.copy(
            createdAtMillis = previous?.createdAtMillis ?: plan.createdAtMillis.takeIf { it > 0L } ?: now,
            approvedAtMillis = when {
                plan.status == "ACTIVE" -> previous?.approvedAtMillis?.takeIf { it > 0L } ?: now
                else -> previous?.approvedAtMillis ?: plan.approvedAtMillis
            },
            updatedAtMillis = now
        )
        mutable(plan.recipientId).value = saved
        preferences?.edit()?.putString(plan.recipientId, encodePlan(saved))?.apply()
    }

    fun approve(recipientId: String) {
        val plan = mutable(recipientId).value ?: return
        val now = System.currentTimeMillis()
        val approved = plan.copy(
            status = "ACTIVE",
            approvedAtMillis = plan.approvedAtMillis.takeIf { it > 0L } ?: now,
            updatedAtMillis = now
        )
        mutable(recipientId).value = approved
        preferences?.edit()?.putString(recipientId, encodePlan(approved))?.apply()
    }

    /** Clears all edited plans (used by Settings → Reset demo data). */
    fun clear() = synchronized(lock) {
        flows.values.forEach { it.value = null }
        preferences?.edit()?.clear()?.apply()
    }

    private fun encodePlan(plan: EditableCarePlan): String = JSONObject().apply {
        put("recipientId", plan.recipientId)
        put("goal", plan.goal)
        put("reason", plan.reason)
        put("actions", JSONArray(plan.actions))
        put("monitoring", JSONArray(plan.monitoring))
        put("priority", plan.priority)
        put("reviewDateLabel", plan.reviewDateLabel)
        put("reviewDateMillis", plan.reviewDateMillis)
        put("createdAtMillis", plan.createdAtMillis)
        put("approvedAtMillis", plan.approvedAtMillis)
        put("status", plan.status)
        put("updatedAtMillis", plan.updatedAtMillis)
    }.toString()

    private fun decodePlan(raw: String): EditableCarePlan {
        val value = JSONObject(raw)
        return EditableCarePlan(
            recipientId = value.getString("recipientId"),
            goal = value.getString("goal"),
            reason = value.getString("reason"),
            actions = value.getJSONArray("actions").toStringList(),
            monitoring = value.getJSONArray("monitoring").toStringList(),
            priority = value.optString("priority", "Normal"),
            reviewDateLabel = value.optString("reviewDateLabel", defaultReviewDate()),
            status = value.optString("status", "DRAFT"),
            updatedAtMillis = value.optLong("updatedAtMillis", System.currentTimeMillis()),
            reviewDateMillis = value.optLong("reviewDateMillis", 0L),
            createdAtMillis = value.optLong("createdAtMillis", value.optLong("updatedAtMillis", System.currentTimeMillis())),
            approvedAtMillis = value.optLong("approvedAtMillis", 0L)
        )
    }

    private fun JSONArray.toStringList(): List<String> =
        List(length()) { index -> optString(index) }

    private fun defaultReviewDate(): String = try {
        LocalDate.now().plusDays(7).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    } catch (_: Exception) {
        "In 7 days"
    }

    companion object {
        val shared = CarePlanStore()
    }
}
