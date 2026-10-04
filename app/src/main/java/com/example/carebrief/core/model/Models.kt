package com.example.carebrief.core.model

data class CareRecipient(
    val id: String,
    val name: String,
    val age: Int,
    val careStatus: String,
    val planStatus: PlanStatus,
    val lastNoteLabel: String,
    val pendingTasks: Int,
    val initials: String
)

enum class PlanStatus { ACTIVE, DRAFT, NONE }

data class DailyNote(
    val id: String,
    val recipientId: String,
    val dayLabel: String,
    val timeLabel: String,
    val author: String,
    val content: String,
    val categories: List<String>
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
