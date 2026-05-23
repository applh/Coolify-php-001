package com.example.cameraxapp.cronjob

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CronJobEntity::class], version = 2, exportSchema = false)
abstract class CronJobDatabase : RoomDatabase() {
    abstract fun cronJobDao(): CronJobDao

    companion object {
        @Volatile
        private var INSTANCE: CronJobDatabase? = null

        fun getDatabase(context: Context): CronJobDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CronJobDatabase::class.java,
                    "cronjob_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
