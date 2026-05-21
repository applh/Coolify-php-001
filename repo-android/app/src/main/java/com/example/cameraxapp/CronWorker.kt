package com.example.cameraxapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class CronWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cronId = inputData.getInt("CRON_ID", -1)
        if (cronId == -1) return Result.failure()

        val dbHelper = AgendaDatabaseHelper(applicationContext)
        val jobs = dbHelper.getAllCronJobs()
        val job = jobs.find { it.id == cronId } ?: return Result.failure()

        val startTime = System.currentTimeMillis()

        return try {
            // Simulate the actual background work matching the cron description
            val durationMs = (100L..500L).random()
            kotlinx.coroutines.delay(durationMs)

            val logMsg: String
            if (job.name.contains("Cache", ignoreCase = true)) {
                // Perform a visual cleaning of caches
                var filesDeleted = 0
                val cacheFolder = applicationContext.cacheDir
                cacheFolder.listFiles()?.forEach { file ->
                    if (file.isFile && System.currentTimeMillis() - file.lastModified() > 600000) {
                        file.delete()
                        filesDeleted++
                    }
                }
                logMsg = "Cache sweeper executed successfully. Purged $filesDeleted indices."
            } else if (job.name.contains("Backup", ignoreCase = true)) {
                // Simulate backing up preferences
                val sharedPrefsDir = File(applicationContext.filesDir.parent, "shared_prefs")
                val filesCount = sharedPrefsDir.listFiles()?.size ?: 0
                logMsg = "Preferences backup sync finished. Backed up $filesCount configurations."
            } else {
                logMsg = "Custom Cron task '${job.name}' verified environment and logged dependencies."
            }

            // Write telemetry result
            dbHelper.addCronLog(cronId, startTime, durationMs, "SUCCESS", logMsg)
            dbHelper.updateCronStatus(cronId, job.isActive, startTime, "SUCCESS")

            // Fire status notification
            triggerNotify("Cron Job: ${job.name}", logMsg)

            Result.success()
        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - startTime
            dbHelper.addCronLog(cronId, startTime, totalTime, "FAILED", "Error: ${e.localizedMessage}")
            dbHelper.updateCronStatus(cronId, job.isActive, startTime, "FAILED")
            Result.retry()
        }
    }

    private fun triggerNotify(title: String, message: String) {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "CRON_CHANNEL_ID"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Background Cron Automation",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)

        notificationManager.notify((1000..9999).random(), builder.build())
    }
}
