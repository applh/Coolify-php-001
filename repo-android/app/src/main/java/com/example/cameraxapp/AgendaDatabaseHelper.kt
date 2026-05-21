package com.example.cameraxapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class AgendaEvent(
    val id: Int,
    val title: String,
    val notes: String,
    val dateMillis: Long,
    val durationMin: Int,
    val colorTag: String
)

data class AlarmInfo(
    val id: Int,
    val timeMillis: Long,
    val label: String,
    val isActive: Boolean
)

data class CronJobInfo(
    val id: Int,
    val name: String,
    val cronExpression: String,
    val isActive: Boolean,
    val lastRunMillis: Long,
    val status: String
)

data class CronLog(
    val id: Int,
    val cronId: Int,
    val runTimeMillis: Long,
    val durationMs: Long,
    val status: String,
    val message: String
)

class AgendaDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "agenda_hub.db"
        private const val DATABASE_VERSION = 1

        // Event Table
        const val TABLE_EVENTS = "agenda_events"
        const val COL_EVENT_ID = "id"
        const val COL_EVENT_TITLE = "title"
        const val COL_EVENT_NOTES = "notes"
        const val COL_EVENT_DATE_MILLIS = "date_millis"
        const val COL_EVENT_DURATION = "duration_min"
        const val COL_EVENT_COLOR = "color_tag"

        // Alarm Table
        const val TABLE_ALARMS = "alarms"
        const val COL_ALARM_ID = "id"
        const val COL_ALARM_TIME_MILLIS = "time_millis"
        const val COL_ALARM_LABEL = "label"
        const val COL_ALARM_IS_ACTIVE = "is_active"

        // Cron Table
        const val TABLE_CRON_JOBS = "cron_jobs"
        const val COL_CRON_ID = "id"
        const val COL_CRON_NAME = "name"
        const val COL_CRON_EXPRESSION = "cron_expression"
        const val COL_CRON_IS_ACTIVE = "is_active"
        const val COL_CRON_LAST_RUN = "last_run_millis"
        const val COL_CRON_STATUS = "status"

        // Cron Log Table
        const val TABLE_CRON_LOGS = "cron_logs"
        const val COL_LOG_ID = "id"
        const val COL_LOG_CRON_ID = "cron_id"
        const val COL_LOG_RUN_TIME = "run_time_millis"
        const val COL_LOG_DURATION = "duration_ms"
        const val COL_LOG_STATUS = "status"
        const val COL_LOG_MESSAGE = "log_message"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create events table
        db.execSQL("""
            CREATE TABLE $TABLE_EVENTS (
                $COL_EVENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_EVENT_TITLE TEXT NOT NULL,
                $COL_EVENT_NOTES TEXT,
                $COL_EVENT_DATE_MILLIS INTEGER NOT NULL,
                $COL_EVENT_DURATION INTEGER NOT NULL,
                $COL_EVENT_COLOR TEXT NOT NULL
            )
        """.trimIndent())

        // Create alarms table
        db.execSQL("""
            CREATE TABLE $TABLE_ALARMS (
                $COL_ALARM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ALARM_TIME_MILLIS INTEGER NOT NULL,
                $COL_ALARM_LABEL TEXT,
                $COL_ALARM_IS_ACTIVE INTEGER NOT NULL
            )
        """.trimIndent())

        // Create cron table
        db.execSQL("""
            CREATE TABLE $TABLE_CRON_JOBS (
                $COL_CRON_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CRON_NAME TEXT NOT NULL,
                $COL_CRON_EXPRESSION TEXT NOT NULL,
                $COL_CRON_IS_ACTIVE INTEGER NOT NULL,
                $COL_CRON_LAST_RUN INTEGER NOT NULL,
                $COL_CRON_STATUS TEXT NOT NULL
            )
        """.trimIndent())

        // Create cron logs table
        db.execSQL("""
            CREATE TABLE $TABLE_CRON_LOGS (
                $COL_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LOG_CRON_ID INTEGER NOT NULL,
                $COL_LOG_RUN_TIME INTEGER NOT NULL,
                $COL_LOG_DURATION INTEGER NOT NULL,
                $COL_LOG_STATUS TEXT NOT NULL,
                $COL_LOG_MESSAGE TEXT
            )
        """.trimIndent())

        // Populate seed items
        seedDefaultData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALARMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CRON_JOBS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CRON_LOGS")
        onCreate(db)
    }

    private fun seedDefaultData(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()

        // 1. Seed standard Calendar Events
        val eventValues = ContentValues().apply {
            put(COL_EVENT_TITLE, "Applet Integration Review")
            put(COL_EVENT_NOTES, "Confirm Agenda Month Grid, WorkManager worker threads and exact sleep wakeups.")
            put(COL_EVENT_DATE_MILLIS, now + (3 * 3600 * 1000)) // 3 hours from now
            put(COL_EVENT_DURATION, 45)
            put(COL_EVENT_COLOR, "Primary")
        }
        db.insert(TABLE_EVENTS, null, eventValues)

        val eventValues2 = ContentValues().apply {
            put(COL_EVENT_TITLE, "System Health Audit")
            put(COL_EVENT_NOTES, "Audit alarm scheduler boot listener resilience and DB SQLite integrity checklists.")
            put(COL_EVENT_DATE_MILLIS, now + (24 * 3600 * 1000)) // tomorrow
            put(COL_EVENT_DURATION, 60)
            put(COL_EVENT_COLOR, "Secondary")
        }
        db.insert(TABLE_EVENTS, null, eventValues2)

        // 2. Seed default high-precision Alarm Clocks
        val alarmValues = ContentValues().apply {
            put(COL_ALARM_TIME_MILLIS, now + (15 * 60 * 1000)) // 15 mins from now
            put(COL_ALARM_LABEL, "Morning Scrum Sync")
            put(COL_ALARM_IS_ACTIVE, 1)
        }
        db.insert(TABLE_ALARMS, null, alarmValues)

        // 3. Seed modular Cron Automation tasks
        val cron1 = ContentValues().apply {
            put(COL_CRON_NAME, "Temp Cache Sweeper")
            put(COL_CRON_EXPRESSION, "*/15 * * * *")
            put(COL_CRON_IS_ACTIVE, 1)
            put(COL_CRON_LAST_RUN, now - (10 * 60 * 1000))
            put(COL_CRON_STATUS, "SUCCESS")
        }
        val cronId1 = db.insert(TABLE_CRON_JOBS, null, cron1)

        val cron2 = ContentValues().apply {
            put(COL_CRON_NAME, "App State Backup Sync")
            put(COL_CRON_EXPRESSION, "0 * * * *")
            put(COL_CRON_IS_ACTIVE, 0)
            put(COL_CRON_LAST_RUN, 0)
            put(COL_CRON_STATUS, "IDLE")
        }
        db.insert(TABLE_CRON_JOBS, null, cron2)

        // 4. Seed status Telemetry logs
        val log1 = ContentValues().apply {
            put(COL_LOG_CRON_ID, cronId1)
            put(COL_LOG_RUN_TIME, now - (10 * 60 * 1000))
            put(COL_LOG_DURATION, 125)
            put(COL_LOG_STATUS, "SUCCESS")
            put(COL_LOG_MESSAGE, "Cleared 42 expired media cache indices.")
        }
        db.insert(TABLE_CRON_LOGS, null, log1)
    }

    // --- Events Handling API ---
    fun getAllEvents(): List<AgendaEvent> {
        val list = mutableListOf<AgendaEvent>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_EVENTS ORDER BY $COL_EVENT_DATE_MILLIS ASC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    AgendaEvent(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TITLE)),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_NOTES)) ?: "",
                        dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVENT_DATE_MILLIS)),
                        durationMin = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_DURATION)),
                        colorTag = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_COLOR))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertEvent(title: String, notes: String, dateMillis: Long, duration: Int, color: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EVENT_TITLE, title)
            put(COL_EVENT_NOTES, notes)
            put(COL_EVENT_DATE_MILLIS, dateMillis)
            put(COL_EVENT_DURATION, duration)
            put(COL_EVENT_COLOR, color)
        }
        return db.insert(TABLE_EVENTS, null, values)
    }

    fun deleteEvent(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_EVENTS, "$COL_EVENT_ID = ?", arrayOf(id.toString()))
    }

    fun updateEvent(id: Int, title: String, notes: String, dateMillis: Long, duration: Int, color: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EVENT_TITLE, title)
            put(COL_EVENT_NOTES, notes)
            put(COL_EVENT_DATE_MILLIS, dateMillis)
            put(COL_EVENT_DURATION, duration)
            put(COL_EVENT_COLOR, color)
        }
        return db.update(TABLE_EVENTS, values, "$COL_EVENT_ID = ?", arrayOf(id.toString()))
    }

    // --- Alarms Handling API ---
    fun getAllAlarms(): List<AlarmInfo> {
        val list = mutableListOf<AlarmInfo>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ALARMS ORDER BY $COL_ALARM_TIME_MILLIS ASC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    AlarmInfo(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ALARM_ID)),
                        timeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ALARM_TIME_MILLIS)),
                        label = cursor.getString(cursor.getColumnIndexOrThrow(COL_ALARM_LABEL)) ?: "Alarm",
                        isActive = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ALARM_IS_ACTIVE)) == 1
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertAlarm(timeMillis: Long, label: String, isActive: Boolean): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ALARM_TIME_MILLIS, timeMillis)
            put(COL_ALARM_LABEL, label)
            put(COL_ALARM_IS_ACTIVE, if (isActive) 1 else 0)
        }
        return db.insert(TABLE_ALARMS, null, values)
    }

    fun updateAlarmStatus(id: Int, isActive: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ALARM_IS_ACTIVE, if (isActive) 1 else 0)
        }
        return db.update(TABLE_ALARMS, values, "$COL_ALARM_ID = ?", arrayOf(id.toString()))
    }

    fun updateAlarm(id: Int, timeMillis: Long, label: String, isActive: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ALARM_TIME_MILLIS, timeMillis)
            put(COL_ALARM_LABEL, label)
            put(COL_ALARM_IS_ACTIVE, if (isActive) 1 else 0)
        }
        return db.update(TABLE_ALARMS, values, "$COL_ALARM_ID = ?", arrayOf(id.toString()))
    }

    fun deleteAlarm(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_ALARMS, "$COL_ALARM_ID = ?", arrayOf(id.toString()))
    }

    // --- Cron Schedules API ---
    fun getAllCronJobs(): List<CronJobInfo> {
        val list = mutableListOf<CronJobInfo>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CRON_JOBS ORDER BY $COL_CRON_ID ASC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    CronJobInfo(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CRON_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COL_CRON_NAME)),
                        cronExpression = cursor.getString(cursor.getColumnIndexOrThrow(COL_CRON_EXPRESSION)),
                        isActive = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CRON_IS_ACTIVE)) == 1,
                        lastRunMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CRON_LAST_RUN)),
                        status = cursor.getString(cursor.getColumnIndexOrThrow(COL_CRON_STATUS))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertCronJob(name: String, expression: String, isActive: Boolean): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CRON_NAME, name)
            put(COL_CRON_EXPRESSION, expression)
            put(COL_CRON_IS_ACTIVE, if (isActive) 1 else 0)
            put(COL_CRON_LAST_RUN, 0L)
            put(COL_CRON_STATUS, "IDLE")
        }
        return db.insert(TABLE_CRON_JOBS, null, values)
    }

    fun updateCronStatus(id: Int, isActive: Boolean, lastRun: Long, status: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CRON_IS_ACTIVE, if (isActive) 1 else 0)
            put(COL_CRON_LAST_RUN, lastRun)
            put(COL_CRON_STATUS, status)
        }
        return db.update(TABLE_CRON_JOBS, values, "$COL_CRON_ID = ?", arrayOf(id.toString()))
    }

    fun updateCronJob(id: Int, name: String, expression: String, isActive: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CRON_NAME, name)
            put(COL_CRON_EXPRESSION, expression)
            put(COL_CRON_IS_ACTIVE, if (isActive) 1 else 0)
        }
        return db.update(TABLE_CRON_JOBS, values, "$COL_CRON_ID = ?", arrayOf(id.toString()))
    }

    fun deleteCronJob(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_CRON_JOBS, "$COL_CRON_ID = ?", arrayOf(id.toString()))
    }

    // --- Telemetry logs API ---
    fun getCronLogs(cronId: Int? = null): List<CronLog> {
        val list = mutableListOf<CronLog>()
        val db = readableDatabase
        val selection = if (cronId != null) "$COL_LOG_CRON_ID = ?" else null
        val selectionArgs = if (cronId != null) arrayOf(cronId.toString()) else null
        val cursor = db.query(TABLE_CRON_LOGS, null, selection, selectionArgs, null, null, "$COL_LOG_RUN_TIME DESC")
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    CronLog(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_ID)),
                        cronId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_CRON_ID)),
                        runTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_RUN_TIME)),
                        durationMs = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_DURATION)),
                        status = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_STATUS)),
                        message = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_MESSAGE)) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun addCronLog(cronId: Int, runTime: Long, duration: Long, status: String, message: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_LOG_CRON_ID, cronId)
            put(COL_LOG_RUN_TIME, runTime)
            put(COL_LOG_DURATION, duration)
            put(COL_LOG_STATUS, status)
            put(COL_LOG_MESSAGE, message)
        }
        return db.insert(TABLE_CRON_LOGS, null, values)
    }
}
