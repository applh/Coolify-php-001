package com.example.cameraxapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class CronReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Kept for backwards compatibility if needed for existing alarms
        val cronId = intent.getIntExtra("CRON_ID", -1)
        if (cronId != -1) {
            val dbHelper = AgendaDatabaseHelper(context)
            val job = dbHelper.getAllCronJobs().find { it.id == cronId }
            if (job != null && job.isActive) {
                var intervalMinutes = 15L
                if (job.cronExpression.startsWith("*/")) {
                    val mins = job.cronExpression.substringAfter("*/").substringBefore(" ").toLongOrNull()
                    if (mins != null && mins >= 15L) intervalMinutes = mins
                }
                CronScheduler.scheduleExact(context, cronId, intervalMinutes)
            }
        }
    }
}

object CronScheduler {
    fun scheduleExact(context: Context, cronId: Int, intervalMinutes: Long) {
        val safeInterval = if (intervalMinutes < 15L) 15L else intervalMinutes
        
        val constraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<CronWorker>(safeInterval, TimeUnit.MINUTES)
            .setInputData(workDataOf("CRON_ID" to cronId))
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "CRON_$cronId",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelExact(context: Context, cronId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("CRON_$cronId")
    }
}
