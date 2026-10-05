package com.example.carebrief.data

import com.example.carebrief.core.model.CareRecipient
import com.example.carebrief.core.model.DailyNote

private fun String.jsonEscape(): String = buildString {
    for (c in this@jsonEscape) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
    }
}

private fun str(value: String) = "\"${value.jsonEscape()}\""

/** Pure export builder: everything the app knows, as portable JSON. No Android APIs. */
fun buildExportJson(recipients: List<CareRecipient>, notes: List<DailyNote>): String {
    val people = recipients.joinToString(",\n      ") { person ->
        """{"id": ${str(person.id)}, "name": ${str(person.name)}, "age": ${person.age}, """ +
            """"careStatus": ${str(person.careStatus)}, "planStatus": ${str(person.planStatus.name)}, """ +
            """"lastNote": ${str(person.lastNoteLabel)}}"""
    }
    val entries = notes.joinToString(",\n      ") { note ->
        val cats = note.categories.joinToString(", ") { str(it) }
        """{"id": ${str(note.id)}, "recipientId": ${str(note.recipientId)}, """ +
            """"day": ${str(note.dayLabel)}, "time": ${str(note.timeLabel)}, """ +
            """"author": ${str(note.author)}, "content": ${str(note.content)}, """ +
            """"categories": [$cats]}"""
    }
    return """{
  "app": "CareBrief",
  "version": 1,
  "disclaimer": "AI-generated content is a draft requiring human review. Not a diagnosis.",
  "recipients": [
      $people
  ],
  "notes": [
      $entries
  ]
}"""
}
