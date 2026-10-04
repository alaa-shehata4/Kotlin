package com.example.carebrief.data.ai

import com.example.carebrief.core.model.CarePlanDraft
import com.example.carebrief.core.model.DailyNote

/**
 * AI seam. Demo provider works fully offline with deterministic output.
 * A remote LLM adapter can be added later behind this same interface
 * (secrets belong on a backend, never in the APK).
 */
data class TaskDraft(
    val title: String,
    val category: String,
    val frequency: String
)

data class PotentialConcern(
    val category: String,
    val evidenceCount: Int,
    val supportingNoteIds: List<String> = emptyList()
)

interface AiCareAssistant {
    fun summarizeNotes(notes: List<DailyNote>): List<String>
    fun analyzePatterns(notes: List<DailyNote>): Map<String, Int>
    fun analyzePotentialConcerns(notes: List<DailyNote>): List<PotentialConcern>
    fun generateCarePlanDraft(notes: List<DailyNote>): CarePlanDraft
    fun generateSuggestedTasks(draft: CarePlanDraft): List<String>

    /** Structured tasks derived from a draft's actions. */
    fun generateTaskDrafts(draft: CarePlanDraft): List<TaskDraft> =
        generateSuggestedTasks(draft).map { TaskDraft(it, "General", "Daily") }
}

class DemoAiCareAssistant : AiCareAssistant {
    override fun summarizeNotes(notes: List<DailyNote>): List<String> =
        analyzePotentialConcerns(notes).map { concern ->
            val evidence = "Observed in ${concern.evidenceCount} of ${notes.size} recent notes."
            when (concern.category) {
                "Nutrition" -> "Reduced appetite observations may warrant review. $evidence"
                "Energy" -> "Fatigue or tiredness was observed and may be useful to discuss with the care team. $evidence"
                "Sleep" -> "Interrupted or restless sleep was observed and may warrant continued tracking. $evidence"
                else -> "${concern.category} observations may warrant review. $evidence"
            }
        }

    override fun analyzePatterns(notes: List<DailyNote>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        notes.forEach { note ->
            note.categories.forEach { c -> counts[c] = (counts[c] ?: 0) + 1 }
        }
        return counts.toList().sortedByDescending { it.second }.toMap()
    }

    override fun analyzePotentialConcerns(notes: List<DailyNote>): List<PotentialConcern> {
        fun matching(predicate: (DailyNote) -> Boolean) = notes.filter(predicate)
        val nutrition = matching { note ->
            val text = note.content.lowercase()
            note.structuredObservations?.appetite in listOf("Reduced", "Poor") ||
                listOf("reduced", "very little", "untouched", "not very hungry", "wasn't hungry").any { text.contains(it) }
        }
        val energy = matching { note ->
            val text = note.content.lowercase()
            text.contains("fatigue") || text.contains("tired")
        }
        val sleep = matching { note ->
            val text = note.content.lowercase()
            note.structuredObservations?.sleep in listOf("Interrupted", "Poor") ||
                text.contains("interrupted") || text.contains("restless")
        }
        return listOf("Nutrition" to nutrition, "Energy" to energy, "Sleep" to sleep)
            .filter { it.second.isNotEmpty() }
            .map { (category, matches) ->
                PotentialConcern(category, matches.size, matches.map(DailyNote::id))
            }
    }

    override fun generateCarePlanDraft(notes: List<DailyNote>): CarePlanDraft {
        val concerns = analyzePotentialConcerns(notes)
        if (concerns.isEmpty()) return CarePlanDraft(
            goal = "Support consistent care observations.",
            reason = "No repeated nutrition, energy, or sleep concerns were identified in the available notes.",
            actions = listOf(
                "Continue recording daily observations.",
                "Review notable changes with the appropriate care professional."
            ),
            monitoring = listOf("General observations")
        )

        val areas = concerns.map { it.category }
        val evidence = concerns.joinToString("; ") { concern ->
            "${concern.category.lowercase()} observations in ${concern.evidenceCount} of ${notes.size} notes"
        }
        val actions = buildList {
            if ("Nutrition" in areas) {
                add("Track meal intake daily.")
                add("Record appetite observations.")
            }
            if ("Energy" in areas) add("Note changes in energy and fatigue.")
            if ("Sleep" in areas) add("Record sleep quality and interruptions.")
            add("Review significant changes with the appropriate care professional.")
        }
        return CarePlanDraft(
            goal = "Support monitoring of ${areas.joinToString().lowercase()}.",
            reason = "Based on recent notes: $evidence.",
            actions = actions,
            monitoring = areas
        )
    }

    override fun generateSuggestedTasks(draft: CarePlanDraft): List<String> =
        generateTaskDrafts(draft).map { it.title }

    override fun generateTaskDrafts(draft: CarePlanDraft): List<TaskDraft> =
        draft.actions.map { action ->
            val lower = action.lowercase()
            val title = when {
                "meal intake" in lower -> "Record today's meal intake"
                "appetite" in lower -> "Record appetite observation"
                "energy" in lower || "mood" in lower -> "Note energy and mood changes"
                "escalate" in lower || "review" in lower || "procedure" in lower ->
                    "Review weekly pattern with the care team"
                else -> action.trim().trimEnd('.').replaceFirstChar { it.uppercase() }
            }
            val category = when {
                "meal" in lower || "appetite" in lower || "nutrition" in lower -> "Nutrition"
                "sleep" in lower || "rest" in lower -> "Sleep"
                "energy" in lower || "fatigue" in lower -> "Energy"
                "mood" in lower -> "Mood"
                "mobility" in lower || "walk" in lower -> "Mobility"
                "medication" in lower -> "Medication"
                "escalate" in lower || "review" in lower -> "Review"
                else -> "General"
            }
            val frequency = if ("weekly" in lower || "escalate" in lower || "review" in lower) "Weekly" else "Daily"
            TaskDraft(title, category, frequency)
        }
}

/** Placeholder for a future backend-connected implementation. Disabled by default. */
class RemoteAiCareAssistant : AiCareAssistant {
    override fun summarizeNotes(notes: List<DailyNote>): List<String> =
        error("Remote AI is not configured. Connect via backend; never embed API keys in the app.")

    override fun analyzePatterns(notes: List<DailyNote>): Map<String, Int> =
        error("Remote AI is not configured.")

    override fun analyzePotentialConcerns(notes: List<DailyNote>): List<PotentialConcern> =
        error("Remote AI is not configured.")

    override fun generateCarePlanDraft(notes: List<DailyNote>): com.example.carebrief.core.model.CarePlanDraft =
        error("Remote AI is not configured.")

    override fun generateSuggestedTasks(draft: com.example.carebrief.core.model.CarePlanDraft): List<String> =
        error("Remote AI is not configured.")
}

/**
 * Process-wide AI selection, driven by the Settings screen.
 * ViewModels read [current] at construction so a provider change
 * applies to newly opened screens. Demo works fully offline.
 */
object AiProviders {
    @Volatile
    var useRemote: Boolean = false

    fun current(): AiCareAssistant =
        if (useRemote) RemoteAiCareAssistant() else DemoAiCareAssistant()
}
