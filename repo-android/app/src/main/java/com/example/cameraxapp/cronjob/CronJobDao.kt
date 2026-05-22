package com.example.cameraxapp.cronjob

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CronJobDao {
    @Query("SELECT * FROM cron_jobs")
    fun getAllJobsFlow(): Flow<List<CronJobEntity>>

    @Query("SELECT * FROM cron_jobs")
    suspend fun getAllJobs(): List<CronJobEntity>

    @Query("SELECT * FROM cron_jobs WHERE isEnabled = 1")
    suspend fun getEnabledJobs(): List<CronJobEntity>

    @Query("SELECT * FROM cron_jobs WHERE id = :id")
    suspend fun getJobById(id: String): CronJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: CronJobEntity)

    @Update
    suspend fun updateJob(job: CronJobEntity)

    @Query("DELETE FROM cron_jobs WHERE id = :id")
    suspend fun deleteJobById(id: String)
}
