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
    val colorTag: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null
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

data class DebugLogEntry(
    val id: Int,
    val timestamp: Long,
    val tag: String,
    val level: String,
    val message: String,
    val stackTrace: String? = null
)

data class ClipboardItem(
    val id: Int,
    val content: String,
    val timestamp: Long
)

class AgendaDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "agenda_hub.db"
        private const val DATABASE_VERSION = 3

        // Event Table
        const val TABLE_EVENTS = "agenda_events"
        const val COL_EVENT_ID = "id"
        const val COL_EVENT_TITLE = "title"
        const val COL_EVENT_NOTES = "notes"
        const val COL_EVENT_DATE_MILLIS = "date_millis"
        const val COL_EVENT_DURATION = "duration_min"
        const val COL_EVENT_COLOR = "color_tag"
        const val COL_EVENT_LATITUDE = "latitude"
        const val COL_EVENT_LONGITUDE = "longitude"
        const val COL_EVENT_LOCATION_NAME = "location_name"

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

        // Debug Log Table
        const val TABLE_DEBUG_LOGS = "debug_logs"
        const val COL_DEBUG_ID = "id"
        const val COL_DEBUG_TIMESTAMP = "timestamp"
        const val COL_DEBUG_TAG = "tag"
        const val COL_DEBUG_LEVEL = "level"
        const val COL_DEBUG_MESSAGE = "message"
        const val COL_DEBUG_STACK_TRACE = "stack_trace"

        // Keyboard Clipboard Table
        const val TABLE_CLIPBOARD = "keyboard_clipboard"
        const val COL_CLIPBOARD_ID = "id"
        const val COL_CLIPBOARD_CONTENT = "content"
        const val COL_CLIPBOARD_TIMESTAMP = "timestamp"
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CLIPBOARD (
                $COL_CLIPBOARD_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CLIPBOARD_CONTENT TEXT NOT NULL,
                $COL_CLIPBOARD_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent())
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
                $COL_EVENT_COLOR TEXT NOT NULL,
                $COL_EVENT_LATITUDE REAL,
                $COL_EVENT_LONGITUDE REAL,
                $COL_EVENT_LOCATION_NAME TEXT
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

        // Create debug logs table
        db.execSQL("""
            CREATE TABLE $TABLE_DEBUG_LOGS (
                $COL_DEBUG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_DEBUG_TIMESTAMP INTEGER NOT NULL,
                $COL_DEBUG_TAG TEXT NOT NULL,
                $COL_DEBUG_LEVEL TEXT NOT NULL,
                $COL_DEBUG_MESSAGE TEXT NOT NULL,
                $COL_DEBUG_STACK_TRACE TEXT
            )
        """.trimIndent())

        // Create keyboard clipboard table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CLIPBOARD (
                $COL_CLIPBOARD_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CLIPBOARD_CONTENT TEXT NOT NULL,
                $COL_CLIPBOARD_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent())

        // Populate seed items
        seedDefaultData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_EVENTS ADD COLUMN $COL_EVENT_LATITUDE REAL")
                db.execSQL("ALTER TABLE $TABLE_EVENTS ADD COLUMN $COL_EVENT_LONGITUDE REAL")
                db.execSQL("ALTER TABLE $TABLE_EVENTS ADD COLUMN $COL_EVENT_LOCATION_NAME TEXT")
            } catch (e: Exception) {
                // Fallback: Drop and recreate if alters fail
                db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_ALARMS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_CRON_JOBS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_CRON_LOGS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_DEBUG_LOGS")
                onCreate(db)
                return
            }
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS $TABLE_DEBUG_LOGS (
                        $COL_DEBUG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_DEBUG_TIMESTAMP INTEGER NOT NULL,
                        $COL_DEBUG_TAG TEXT NOT NULL,
                        $COL_DEBUG_LEVEL TEXT NOT NULL,
                        $COL_DEBUG_MESSAGE TEXT NOT NULL,
                        $COL_DEBUG_STACK_TRACE TEXT
                    )
                """.trimIndent())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun seedDefaultData(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()

        // 1. Seed standard Calendar Events
        val eventValues = ContentValues().apply {
            put(COL_EVENT_TITLE, "Applet Integration Review")
            put(COL_EVENT_NOTES, "Confirm Agenda Month Grid, WorkManager worker threads and exact sleep wakeups.")
            put(COL_EVENT_DATE_MILLIS, now + (3 * 3600 * 1000)) // 3 hours from now
            put(COL_EVENT_DURATION, 45)
            put(COL_EVENT_COLOR, "#008080")
            put(COL_EVENT_LATITUDE, 48.8566)
            put(COL_EVENT_LONGITUDE, 2.3522)
            put(COL_EVENT_LOCATION_NAME, "Paris, France")
        }
        db.insert(TABLE_EVENTS, null, eventValues)

        val eventValues2 = ContentValues().apply {
            put(COL_EVENT_TITLE, "System Health Audit")
            put(COL_EVENT_NOTES, "Audit alarm scheduler boot listener resilience and DB SQLite integrity checklists.")
            put(COL_EVENT_DATE_MILLIS, now + (24 * 3600 * 1000)) // tomorrow
            put(COL_EVENT_DURATION, 60)
            put(COL_EVENT_COLOR, "#E91E63")
            put(COL_EVENT_LATITUDE, 37.7749)
            put(COL_EVENT_LONGITUDE, -122.4194)
            put(COL_EVENT_LOCATION_NAME, "San Francisco, CA")
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

        val cron3 = ContentValues().apply {
            put(COL_CRON_NAME, "Wallpaper Rotator")
            put(COL_CRON_EXPRESSION, "*/15 * * * *")
            put(COL_CRON_IS_ACTIVE, 0)
            put(COL_CRON_LAST_RUN, 0)
            put(COL_CRON_STATUS, "IDLE")
        }
        db.insert(TABLE_CRON_JOBS, null, cron3)

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
                val latIndex = cursor.getColumnIndex(COL_EVENT_LATITUDE)
                val lngIndex = cursor.getColumnIndex(COL_EVENT_LONGITUDE)
                val locIndex = cursor.getColumnIndex(COL_EVENT_LOCATION_NAME)

                val latitude = if (latIndex != -1 && !cursor.isNull(latIndex)) cursor.getDouble(latIndex) else null
                val longitude = if (lngIndex != -1 && !cursor.isNull(lngIndex)) cursor.getDouble(lngIndex) else null
                val locationName = if (locIndex != -1) cursor.getString(locIndex) else null

                list.add(
                    AgendaEvent(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TITLE)),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_NOTES)) ?: "",
                        dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVENT_DATE_MILLIS)),
                        durationMin = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_DURATION)),
                        colorTag = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_COLOR)),
                        latitude = latitude,
                        longitude = longitude,
                        locationName = locationName
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertEvent(
        title: String,
        notes: String,
        dateMillis: Long,
        duration: Int,
        color: String,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EVENT_TITLE, title)
            put(COL_EVENT_NOTES, notes)
            put(COL_EVENT_DATE_MILLIS, dateMillis)
            put(COL_EVENT_DURATION, duration)
            put(COL_EVENT_COLOR, color)
            if (latitude != null) put(COL_EVENT_LATITUDE, latitude) else putNull(COL_EVENT_LATITUDE)
            if (longitude != null) put(COL_EVENT_LONGITUDE, longitude) else putNull(COL_EVENT_LONGITUDE)
            if (locationName != null) put(COL_EVENT_LOCATION_NAME, locationName) else putNull(COL_EVENT_LOCATION_NAME)
        }
        return db.insert(TABLE_EVENTS, null, values)
    }

    fun deleteEvent(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_EVENTS, "$COL_EVENT_ID = ?", arrayOf(id.toString()))
    }

    fun updateEvent(
        id: Int,
        title: String,
        notes: String,
        dateMillis: Long,
        duration: Int,
        color: String,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null
    ): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EVENT_TITLE, title)
            put(COL_EVENT_NOTES, notes)
            put(COL_EVENT_DATE_MILLIS, dateMillis)
            put(COL_EVENT_DURATION, duration)
            put(COL_EVENT_COLOR, color)
            if (latitude != null) put(COL_EVENT_LATITUDE, latitude) else putNull(COL_EVENT_LATITUDE)
            if (longitude != null) put(COL_EVENT_LONGITUDE, longitude) else putNull(COL_EVENT_LONGITUDE)
            if (locationName != null) put(COL_EVENT_LOCATION_NAME, locationName) else putNull(COL_EVENT_LOCATION_NAME)
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

    // --- Debug Logs Handling API ---
    fun getAllDebugLogs(): List<DebugLogEntry> {
        val list = mutableListOf<DebugLogEntry>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DEBUG_LOGS ORDER BY $COL_DEBUG_TIMESTAMP DESC LIMIT 1000", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    DebugLogEntry(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_DEBUG_ID)),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DEBUG_TIMESTAMP)),
                        tag = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEBUG_TAG)),
                        level = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEBUG_LEVEL)),
                        message = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEBUG_MESSAGE)),
                        stackTrace = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEBUG_STACK_TRACE))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun addDebugLog(tag: String, level: String, message: String, stackTrace: String?): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_DEBUG_TIMESTAMP, System.currentTimeMillis())
            put(COL_DEBUG_TAG, tag)
            put(COL_DEBUG_LEVEL, level)
            put(COL_DEBUG_MESSAGE, message)
            put(COL_DEBUG_STACK_TRACE, stackTrace)
        }
        return db.insert(TABLE_DEBUG_LOGS, null, values)
    }

    fun clearDebugLogs(): Int {
        val db = writableDatabase
        return db.delete(TABLE_DEBUG_LOGS, null, null)
    }

    // --- Keyboard Clipboard API ---
    fun getAllClipboardItems(): List<ClipboardItem> {
        val list = mutableListOf<ClipboardItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CLIPBOARD ORDER BY $COL_CLIPBOARD_TIMESTAMP DESC LIMIT 50", null)
        try {
            if (cursor.moveToFirst()) {
                do {
                    list.add(
                        ClipboardItem(
                            id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CLIPBOARD_ID)),
                            content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIPBOARD_CONTENT)),
                            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CLIPBOARD_TIMESTAMP))
                        )
                    )
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
        }
        return list
    }

    fun addClipboardItem(content: String): Long {
        if (content.trim().isEmpty()) return -1
        val db = writableDatabase
        try {
            // Avoid duplicate items in recent clipboard
            db.delete(TABLE_CLIPBOARD, "$COL_CLIPBOARD_CONTENT = ?", arrayOf(content))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val values = ContentValues().apply {
            put(COL_CLIPBOARD_CONTENT, content)
            put(COL_CLIPBOARD_TIMESTAMP, System.currentTimeMillis())
        }
        return db.insert(TABLE_CLIPBOARD, null, values)
    }

    fun deleteClipboardItem(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_CLIPBOARD, "$COL_CLIPBOARD_ID = ?", arrayOf(id.toString()))
    }
}
