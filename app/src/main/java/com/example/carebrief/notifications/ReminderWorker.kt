package com.example.carebrief.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.carebrief.data.CarePlanStore
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.SettingsStore
import com.example.carebrief.data.TaskStore
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

private const val REMINDER_WORK = "carebrief_daily_reminder"

/**
 * Daily local reminder. Reads the persistent repository (survives
 * process death) and notifies only when something needs attention.
 * No backend, fully offline.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val enabled = SettingsStore(appContext).notificationsEnabled.first()
        if (!enabled) return Result.success()
        val recipients = DemoCareBriefRepository.shared.observeRecipients().first()
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val endOfToday = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
        val pendingTasks = recipients.sumOf { recipient ->
            TaskStore.shared.observe(recipient.id).value.count { task ->
                !task.completed && (task.dueDateMillis <= 0L || task.dueDateMillis <= endOfToday)
            }
        }
        val dueReviews = recipients.count { recipient ->
            val plan = CarePlanStore.shared.observe(recipient.id).value
            plan?.status == "ACTIVE" && plan.reviewDateMillis > 0L &&
                Instant.ofEpochMilli(plan.reviewDateMillis).atZone(zone).toLocalDate() <= today
        }
        if (pendingTasks > 0) notifyTaskReminder(appContext, pendingTasks)
        if (dueReviews > 0) notifyReviewReminder(appContext, dueReviews)
        return Result.success()
    }
}

object ReminderScheduler {
    fun setEnabled(context: Context, enabled: Boolean) {
        val work = WorkManager.getInstance(context)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .build()
            work.enqueueUniquePeriodicWork(REMINDER_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            work.cancelUniqueWork(REMINDER_WORK)
        }
    }
}
