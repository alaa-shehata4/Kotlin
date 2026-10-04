package com.example.carebrief.data

import com.example.carebrief.core.model.CareRecipient
import com.example.carebrief.core.model.DailyNote
import com.example.carebrief.core.model.PlanStatus
import com.example.carebrief.core.model.StructuredObservations
import com.example.carebrief.core.database.CareBriefDatabase
import com.example.carebrief.core.database.NoteEntity
import com.example.carebrief.core.database.RecipientEntity
import androidx.room.withTransaction
import com.example.carebrief.data.local.DemoData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
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
    fun observeAllNotes(): Flow<List<DailyNote>>
    suspend fun addNote(
        recipientId: String,
        content: String,
        categories: List<String>,
        author: String,
        structuredObservations: StructuredObservations? = null
    ): DailyNote

    suspend fun setPlanStatus(recipientId: String, status: PlanStatus)

    /** Clears all data and restores the demo dataset. */
    suspend fun resetDemoData()
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

    override fun observeAllNotes(): Flow<List<DailyNote>> = notes

    override suspend fun addNote(
        recipientId: String,
        content: String,
        categories: List<String>,
        author: String,
        structuredObservations: StructuredObservations?
    ): DailyNote {
        val now = LocalDateTime.now()
        val note = createDailyNote(recipientId, content, categories, author, structuredObservations, now)
        notes.value = listOf(note) + notes.value
        // Refresh recipient's "last note" label + pending count stays truthful.
        recipients.value = recipients.value.map {
            if (it.id == recipientId) it.copy(lastNoteLabel = "Today, ${note.timeLabel}") else it
        }
        return note
    }

    override suspend fun setPlanStatus(recipientId: String, status: PlanStatus) {
        recipients.value = recipients.value.map {
            if (it.id == recipientId) it.copy(planStatus = status) else it
        }
    }

    override suspend fun resetDemoData() {
        recipients.value = DemoData.recipients
        notes.value = DemoData.sarahNotes + DemoData.michaelNotes + DemoData.aminaNotes
    }

    companion object {
        /** Replaced with the Room-backed repository by CareBriefApp at startup. */
        @Volatile
        var shared: CareBriefRepository = DemoCareBriefRepository()
    }
}

class RoomCareBriefRepository(private val database: CareBriefDatabase) : CareBriefRepository {
    override fun observeRecipients(): Flow<List<CareRecipient>> =
        database.recipientDao().observeAll().map { rows -> rows.map(RecipientEntity::toModel) }

    override fun searchRecipients(query: String): Flow<List<CareRecipient>> =
        observeRecipients().map { people ->
            val needle = query.trim().lowercase()
            if (needle.isEmpty()) people else people.filter { it.name.lowercase().contains(needle) }
        }

    override fun observeRecipient(recipientId: String): Flow<CareRecipient?> =
        observeRecipients().map { people -> people.firstOrNull { it.id == recipientId } }

    override fun observeNotes(recipientId: String): Flow<List<DailyNote>> =
        database.noteDao().observeForRecipient(recipientId).map { rows -> rows.map(NoteEntity::toModel) }

    override fun observeAllNotes(): Flow<List<DailyNote>> =
        database.noteDao().observeAll().map { rows -> rows.map(NoteEntity::toModel) }

    override suspend fun addNote(
        recipientId: String,
        content: String,
        categories: List<String>,
        author: String,
        structuredObservations: StructuredObservations?
    ): DailyNote {
        val now = LocalDateTime.now()
        val note = createDailyNote(recipientId, content, categories, author, structuredObservations, now)
        database.noteDao().insert(note.toEntity(now))
        database.recipientDao().updateLastNote(recipientId, "Today, ${note.timeLabel}")
        return note
    }

    override suspend fun setPlanStatus(recipientId: String, status: PlanStatus) {
        database.recipientDao().updatePlanStatus(recipientId, status.name)
    }

    suspend fun seedIfEmpty() {
        database.withTransaction {
            if (database.recipientDao().count() != 0) return@withTransaction
            database.recipientDao().upsertAll(DemoData.recipients.map(CareRecipient::toEntity))
            val notes = DemoData.sarahNotes + DemoData.michaelNotes + DemoData.aminaNotes
            database.noteDao().insertAll(notes.map(DailyNote::toSeedEntity))
        }
    }

    override suspend fun resetDemoData() {
        database.withTransaction {
            database.noteDao().clearAll()
            database.recipientDao().clearAll()
            database.recipientDao().upsertAll(DemoData.recipients.map(CareRecipient::toEntity))
            val notes = DemoData.sarahNotes + DemoData.michaelNotes + DemoData.aminaNotes
            database.noteDao().insertAll(notes.map(DailyNote::toSeedEntity))
        }
    }
}

private fun CareRecipient.toEntity() = RecipientEntity(
    id, name, age, careStatus, planStatus.name, lastNoteLabel, pendingTasks, initials
)

private fun createDailyNote(
    recipientId: String,
    content: String,
    categories: List<String>,
    author: String,
    structuredObservations: StructuredObservations?,
    now: LocalDateTime
): DailyNote {
    val trimmed = content.trim()
    require(trimmed.length >= 10) { "Please add a little more detail (at least 10 characters)." }
    return DailyNote(
        id = UUID.randomUUID().toString(),
        recipientId = recipientId,
        dayLabel = "Today",
        timeLabel = now.format(DateTimeFormatter.ofPattern("HH:mm")),
        author = author.ifBlank { "Caregiver" },
        content = trimmed,
        categories = categories.ifEmpty { listOf("Other") },
        structuredObservations = structuredObservations,
        recordedAtMillis = now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
}

private fun RecipientEntity.toModel() = CareRecipient(
    id = id,
    name = name,
    age = age,
    careStatus = careStatus,
    planStatus = runCatching { PlanStatus.valueOf(planStatus) }.getOrDefault(PlanStatus.NONE),
    lastNoteLabel = lastNoteLabel,
    pendingTasks = pendingTasks,
    initials = initials
)

private fun DailyNote.toEntity(recordedAt: LocalDateTime) = NoteEntity(
    id = id,
    recipientId = recipientId,
    timestamp = recordedAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
    dayLabel = dayLabel,
    timeLabel = timeLabel,
    author = author,
    content = content,
    categoriesCsv = categories.joinToString("|"),
    mood = structuredObservations?.mood,
    mobility = structuredObservations?.mobility,
    appetite = structuredObservations?.appetite,
    sleep = structuredObservations?.sleep
)

private fun DailyNote.toSeedEntity(): NoteEntity {
    val daysAgo = when (val label = dayLabel.lowercase()) {
        "today" -> 0L
        "yesterday" -> 1L
        else -> Regex("(\\d+) days ago").matchEntire(label)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }
    val time = runCatching { LocalTime.parse(timeLabel, DateTimeFormatter.ofPattern("HH:mm")) }
        .getOrDefault(LocalTime.NOON)
    return toEntity(LocalDate.now().minusDays(daysAgo).atTime(time))
}

private fun NoteEntity.toModel() = DailyNote(
    id = id,
    recipientId = recipientId,
    dayLabel = dayLabel,
    timeLabel = timeLabel,
    author = author,
    content = content,
    categories = categoriesCsv.split('|').filter(String::isNotBlank),
    structuredObservations = if (mood == null && mobility == null && appetite == null && sleep == null) null
    else StructuredObservations(mood, mobility, appetite, sleep),
    recordedAtMillis = timestamp
)
