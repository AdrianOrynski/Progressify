package com.example.progressify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object NotificationScheduler {

    const val CHANNEL_ID = "task_reminders"
    private const val NOTIFY_BEFORE_MS     = 15 * 60 * 1000L
    private const val MIN_DURATION_MINUTES = 30L

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminds you 15 minutes before a task deadline"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun schedule(context: Context, task: Task) {
        val duration = task.getDuration() ?: return
        if (duration.toMinutes() < MIN_DURATION_MINUTES) return

        val endMillis = task.endTime?.toDate()?.time ?: return
        val notifyAt  = endMillis - NOTIFY_BEFORE_MS
        if (notifyAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyAt, buildIntent(context, task))
    }

    fun cancel(context: Context, task: Task) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(buildIntent(context, task))
    }

    private fun buildIntent(context: Context, task: Task): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            Intent(context, TaskNotificationReceiver::class.java).apply {
                putExtra(TaskNotificationReceiver.EXTRA_TASK_ID,    task.id)
                putExtra(TaskNotificationReceiver.EXTRA_TASK_TITLE, task.title)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
