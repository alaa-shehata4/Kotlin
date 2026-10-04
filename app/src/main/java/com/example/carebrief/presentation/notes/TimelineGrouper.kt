package com.example.carebrief.presentation.notes

import com.example.carebrief.core.model.DailyNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TimelineGroup(val dayLabel: String, val notes: List<DailyNote>)

/** Pure chronology helper: groups notes by day, preserving first-seen order. */
object TimelineGrouper {
    fun group(notes: List<DailyNote>): List<TimelineGroup> {
        val order = LinkedHashMap<String, MutableList<DailyNote>>()
        notes.forEach { note ->
            order.getOrPut(note.dayLabel) { mutableListOf() }.add(note)
        }
        return order.map { (day, list) -> TimelineGroup(day, list.toList()) }
    }
}

/** Note IDs included in a completed analysis during this app process. */
object TimelineAnalysisHistory {
    private val mutableAnalyzedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val analyzedNoteIds = mutableAnalyzedNoteIds.asStateFlow()

    fun markAnalyzed(noteIds: Collection<String>) {
        mutableAnalyzedNoteIds.value = mutableAnalyzedNoteIds.value + noteIds
    }
}

/** Pure filter: null category/day means "all". */
fun filterNotes(notes: List<DailyNote>, category: String?, day: String?): List<DailyNote> =
    notes.filter { note ->
        (category == null || note.categories.any { it.equals(category, ignoreCase = true) }) &&
            (day == null || note.dayLabel.equals(day, ignoreCase = true))
    }
