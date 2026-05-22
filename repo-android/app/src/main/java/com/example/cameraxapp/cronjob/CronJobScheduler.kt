package com.example.cameraxapp.cronjob

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CronJobScheduler {

    fun scheduleJob(context: Context, job: CronJobEntity) {
        val workManager = WorkManager.getInstance(context)

        if (!job.isEnabled) {
            cancelJob(context, job.id)
            return
        }

        val constraintsBuilder = Constraints.Builder()
        if (job.requiresNetwork) {
            constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED)
        }
        if (job.requiresCharging) {
            constraintsBuilder.setRequiresCharging(true)
        }

        // Enforce minimum 15 minutes logic automatically handled by WorkManager but we ensure input is safe
        val interval = maxOf(job.intervalMinutes.toLong(), 15L)

        val workRequest = PeriodicWorkRequestBuilder<DynamicRouterWorker>(
            interval, TimeUnit.MINUTES
        )
            .setConstraints(constraintsBuilder.build())
            .setInputData(
                workDataOf(
                    "jobId" to job.id,
                    "jobType" to job.jobType
                )
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            job.id,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelJob(context: Context, jobId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(jobId)
    }

    fun syncJobsFromDatabase(context: Context) {
        // Run in background
        CoroutineScope(Dispatchers.IO).launch {
            val database = CronJobDatabase.getDatabase(context)
            val allJobs = database.cronJobDao().getAllJobs()
            allJobs.forEach { job ->
                if (job.isEnabled) {
                    scheduleJob(context, job)
                } else {
                    cancelJob(context, job.id)
                }
            }
        }
    }
}
