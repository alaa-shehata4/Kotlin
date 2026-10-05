package com.example.carebrief.core.model

/**
 * Phase 26 — canonical domain models.
 *
 * Field names follow implementation_plan.md § Phase 26. UI-friendly display
 * labels (dayLabel/timeLabel/lastNoteLabel/initials) are kept alongside the
 * canonical timestamps so lists can render without date formatting on the
 * main thread, while sorting/filtering always uses the millis fields.
 */
data class CareRecipient(
    val id: String,
    val name: String,
    val age: Int,
    val careStatus: String,
    val planStatus: PlanStatus,
    val lastNoteLabel: String,
    val pendingTasks: Int,
    val initials: String,
    /** Canonical creation timestamp (Phase 26: createdAt). */
    val createdAtMillis: Long = 0L,
    /** Optional avatar reference (Phase 26: avatar). Null = initials avatar. */
    val avatarUri: String? = null
)

enum class PlanStatus { ACTIVE, DRAFT, NONE }

data class DailyNote(
    val id: String,
    val recipientId: String,
    /** Canonical timestamp (Phase 26: timestamp). */
    val timestampMillis: Long = 0L,
    val dayLabel: String,
    val timeLabel: String,
    val author: String,
    val content: String,
    val categories: List<String>,
    val mood: String? = null,
    val mobility: String? = null,
    val appetite: String? = null,
    val sleep: String? = null,
    val structuredObservations: StructuredObservations? = null,
    val recordedAtMillis: Long = 0L
) {
    /** Back-compat accessor: single canonical timestamp for sorting. */
    val timestamp: Long get() = timestampMillis.takeIf { it != 0L } ?: recordedAtMillis
}

data class StructuredObservations(
    val mood: String? = null,
    val mobility: String? = null,
    val appetite: String? = null,
    val sleep: String? = null
)

/** Phase 26: AiInsight — explainable insight with evidence, never a diagnosis. */
enum class InsightSeverity { INFO, WATCH, CONCERN }

data class AiInsight(
    val id: String,
    val recipientId: String,
    val title: String,
    val description: String,
    /** Human-readable evidence, e.g. "3 notes over the last 5 days mentioned…". */
    val evidence: String,
    /** Machine-usable phrasing, e.g. "Observed in 3 of 5 recent notes." */
    val frequency: String,
    val frequencyCount: Int = 0,
    val frequencyTotal: Int = 0,
    val severity: InsightSeverity = InsightSeverity.WATCH,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/** Phase 26: CarePlan — persistent care-plan with lifecycle status. */
enum class CarePlanStatus { DRAFT, ACTIVE, ARCHIVED }

enum class CarePriority { LOW, NORMAL, HIGH }

data class CarePlan(
    val id: String,
    val recipientId: String,
    val goal: String,
    val reason: String,
    val actions: List<String>,
    val monitoringIndicators: List<String>,
    val priority: CarePriority = CarePriority.NORMAL,
    val status: CarePlanStatus = CarePlanStatus.DRAFT,
    /** Nullable review deadline in epoch millis (0 = unset). */
    val reviewDateMillis: Long = 0L,
    val reviewDateLabel: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val approvedAtMillis: Long = 0L
)

/** Phase 26: Task — actionable item linked to a plan and a recipient. */
enum class TaskPriority { LOW, NORMAL, HIGH }

data class CareTaskModel(
    val id: String,
    /** Nullable: ad-hoc tasks may exist before a plan is approved. */
    val carePlanId: String? = null,
    val recipientId: String,
    val title: String,
    val description: String = "",
    /** Nullable due date in epoch millis (0 = no concrete date, use dueLabel). */
    val dueDateMillis: Long = 0L,
    val dueLabel: String = "Today",
    val priority: TaskPriority = TaskPriority.NORMAL,
    val category: String = "General",
    val frequency: String = "Daily",
    val completed: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val sourceFingerprint: String = ""
)

data class AttentionItem(
    val title: String,
    val description: String
)

data class ActivityItem(
    val title: String,
    val subtitle: String
)

data class CarePlanDraft(
    val goal: String,
    val reason: String,
    val actions: List<String>,
    val monitoring: List<String>
)

/**diff-friendly helpers kept next to the models so Room mappers stay thin. */
fun CarePlan.toDraft(): CarePlanDraft = CarePlanDraft(
    goal = goal,
    reason = reason,
    actions = actions,
    monitoring = monitoringIndicators
)

fun CarePriority.toLabel(): String = when (this) {
    CarePriority.LOW -> "Low"
    CarePriority.NORMAL -> "Normal"
    CarePriority.HIGH -> "High"
}

fun String.toCarePriority(): CarePriority = when (trim().uppercase()) {
    "LOW" -> CarePriority.LOW
    "HIGH" -> CarePriority.HIGH
    else -> CarePriority.NORMAL
}

fun String.toCarePlanStatus(): CarePlanStatus = runCatching {
    CarePlanStatus.valueOf(trim().uppercase())
}.getOrDefault(CarePlanStatus.DRAFT)
