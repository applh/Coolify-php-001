package com.example.cameraxapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class CronReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cronId = intent.getIntExtra("CRON_ID", -1)
        if (cronId == -1) return

        // Fire the worker immediately to do the heavy lifting asynchronously
        val workRequest = OneTimeWorkRequestBuilder<CronWorker>()
            .setInputData(workDataOf("CRON_ID" to cronId))
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)

        // Reschedule next exact tick if it's still active
        val dbHelper = AgendaDatabaseHelper(context)
        val job = dbHelper.getAllCronJobs().find { it.id == cronId }
        
        if (job != null && job.isActive) {
            var intervalMinutes = 15L
            if (job.cronExpression.startsWith("*/")) {
                val mins = job.cronExpression.substringAfter("*/").substringBefore(" ").toLongOrNull()
                if (mins != null && mins >= 1L) intervalMinutes = mins
            }
            CronScheduler.scheduleExact(context, cronId, intervalMinutes)
        }
    }
}

object CronScheduler {
    fun scheduleExact(context: Context, cronId: Int, intervalMinutes: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CronReceiver::class.java).apply {
            putExtra("CRON_ID", cronId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            cronId + 500000, // Offset to avoid collision with standard alarms
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000)

        try {
            if (BuildHelper.canScheduleExact(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback for cases where exact alarms are explicitly revoked
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelExact(context: Context, cronId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CronReceiver::class.java).apply {
            putExtra("CRON_ID", cronId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            cronId + 500000,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
