package com.example.carebrief.data

import com.example.carebrief.core.model.CareRecipient
import com.example.carebrief.core.model.DailyNote
import com.example.carebrief.core.model.PlanStatus
import com.example.carebrief.data.local.DemoData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Small repository seam. Demo adapter is fully offline and deterministic.
 * A Room-backed adapter can replace it later without touching callers.
 */
interface CareBriefRepository {
    fun observeRecipients(): Flow<List<CareRecipient>>
    fun searchRecipients(query: String): Flow<List<CareRecipient>>
    fun observeRecipient(recipientId: String): Flow<CareRecipient?>
    fun observeNotes(recipientId: String): Flow<List<DailyNote>>
    suspend fun addNote(
        recipientId: String,
        content: String,
        categories: List<String>,
        author: String
    ): DailyNote

    fun setPlanStatus(recipientId: String, status: PlanStatus)
}

class DemoCareBriefRepository : CareBriefRepository {
    private val recipients = MutableStateFlow(DemoData.recipients)
    private val notes = MutableStateFlow(
        DemoData.sarahNotes + DemoData.michaelNotes + DemoData.aminaNotes
    )

    override fun observeRecipients(): Flow<List<CareRecipient>> = recipients

    override fun searchRecipients(query: String): Flow<List<CareRecipient>> =
        recipients.map { list ->
            val q = query.trim().lowercase()
            if (q.isEmpty()) list else list.filter { it.name.lowercase().contains(q) }
        }

    override fun observeRecipient(recipientId: String): Flow<CareRecipient?> =
        recipients.map { list -> list.find { it.id == recipientId } }

    override fun observeNotes(recipientId: String): Flow<List<DailyNote>> =
        notes.map { list -> list.filter { it.recipientId == recipientId } }

    override suspend fun addNote(
        recipientId: String,
        content: String,
        categories: List<String>,
        author: String
    ): DailyNote {
        val trimmed = content.trim()
        require(trimmed.length >= 10) { "Please add a little more detail (at least 10 characters)." }
        val now = LocalDateTime.now()
        val note = DailyNote(
            id = UUID.randomUUID().toString(),
            recipientId = recipientId,
            dayLabel = "Today",
            timeLabel = now.format(DateTimeFormatter.ofPattern("HH:mm")),
            author = author.ifBlank { "Caregiver" },
            content = trimmed,
            categories = categories.ifEmpty { listOf("Other") }
        )
        notes.value = listOf(note) + notes.value
        // Refresh recipient's "last note" label + pending count stays truthful.
        recipients.value = recipients.value.map {
            if (it.id == recipientId) it.copy(lastNoteLabel = "Today, ${note.timeLabel}") else it
        }
        return note
    }

    override fun setPlanStatus(recipientId: String, status: PlanStatus) {
        recipients.value = recipients.value.map {
            if (it.id == recipientId) it.copy(planStatus = status) else it
        }
    }

    companion object {
        /** App-wide singleton so notes added in the editor appear everywhere. */
        val shared = DemoCareBriefRepository()
    }
}
