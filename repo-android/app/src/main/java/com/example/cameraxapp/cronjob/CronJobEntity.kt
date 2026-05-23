package com.example.cameraxapp.cronjob

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cron_jobs")
data class CronJobEntity(
    @PrimaryKey val id: String,
    val jobType: String,
    val intervalMinutes: Int,
    val isEnabled: Boolean,
    val requiresNetwork: Boolean = false,
    val requiresCharging: Boolean = false,
    val lastRunTimestamp: Long = 0L,
    val nextRunTimestamp: Long = 0L,
    val downloadUrl: String? = null,
    val saveFileName: String? = null
)
