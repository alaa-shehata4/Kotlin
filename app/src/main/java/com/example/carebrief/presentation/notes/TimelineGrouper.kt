package com.example.carebrief.presentation.notes

import com.example.carebrief.core.model.DailyNote

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
