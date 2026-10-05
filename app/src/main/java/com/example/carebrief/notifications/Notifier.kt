package com.example.carebrief.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

const val CARE_CHANNEL_ID = "carebrief_reminders"
private const val TASK_NOTIFICATION_ID = 1001
private const val REVIEW_NOTIFICATION_ID = 1002

fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(CARE_CHANNEL_ID) != null) return
    manager.createNotificationChannel(
        NotificationChannel(
            CARE_CHANNEL_ID,
            "Care reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Pending task and care-plan review reminders." }
    )
}

fun canNotify(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ActivityCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

fun notifyTaskReminder(context: Context, pending: Int) {
    if (!canNotify(context)) return
    ensureChannel(context)
    val notification = NotificationCompat.Builder(context, CARE_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.sym_def_app_icon)
        .setContentTitle("CareBrief · Task reminder")
        .setContentText(taskReminderText(pending))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(TASK_NOTIFICATION_ID, notification)
}

fun notifyReviewReminder(context: Context, drafts: Int) {
    if (!canNotify(context)) return
    ensureChannel(context)
    val notification = NotificationCompat.Builder(context, CARE_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.sym_def_app_icon)
        .setContentTitle("CareBrief · Review reminder")
        .setContentText(reviewReminderText(drafts))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(REVIEW_NOTIFICATION_ID, notification)
}
