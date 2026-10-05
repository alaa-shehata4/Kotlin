package com.example.carebrief.data

import com.example.carebrief.core.model.DailyNote
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun analysisNotes(notes: List<DailyNote>, range: AnalysisRange, today: LocalDate = LocalDate.now()): List<DailyNote> {
    if (range == AnalysisRange.ALL_NOTES) return notes
    val firstIncludedDay = today.minusDays(6)
    return notes.filter { note ->
        noteDate(note, today)?.let { !it.isBefore(firstIncludedDay) && !it.isAfter(today) } ?: true
    }
}

private fun noteDate(note: DailyNote, today: LocalDate): LocalDate? {
    val timestamp = note.timestamp
    if (timestamp > 0L) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val daysAgo = when (val label = note.dayLabel.lowercase()) {
        "today" -> 0L
        "yesterday" -> 1L
        else -> Regex("(\\d+) days ago").matchEntire(label)?.groupValues?.get(1)?.toLongOrNull()
    }
    return daysAgo?.let(today::minusDays)
}
