package com.example.cameraxapp

import android.content.Context
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.CopyOnWriteArrayList

object AppLogger {
    private const val GENERAL_TAG = "FraiseApp"
    private var appContext: Context? = null
    private val inMemoryLogs = CopyOnWriteArrayList<InMemoryLogEntry>()

    data class InMemoryLogEntry(
        val timestamp: Long,
        val tag: String,
        val level: String,
        val message: String,
        val stackTrace: String?
    )

    fun init(context: Context) {
        appContext = context.applicationContext
        // Seed an initial start log
        i("System", "AppLogger initialized successfully.")
    }

    private fun getDbHelper(): AgendaDatabaseHelper? {
        return appContext?.let { AgendaDatabaseHelper(it) }
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeLog(tag, "DEBUG", message, null)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        writeLog(tag, "INFO", message, null)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        writeLog(tag, "WARN", message, null)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val stackTrace = throwable?.let { getStackTraceString(it) }
        Log.e(tag, "$message : ${throwable?.message}", throwable)
        writeLog(tag, "ERROR", message, stackTrace)
    }

    private fun writeLog(tag: String, level: String, message: String, stackTrace: String?) {
        val now = System.currentTimeMillis()
        
        // Maintain in-memory buffer up to 500 records
        if (inMemoryLogs.size >= 500) {
            inMemoryLogs.removeAt(0)
        }
        inMemoryLogs.add(InMemoryLogEntry(now, tag, level, message, stackTrace))

        // Persist to database
        try {
            getDbHelper()?.addDebugLog(tag, level, message, stackTrace)
        } catch (e: Exception) {
            Log.e(GENERAL_TAG, "Failed to persist log to SQLite database: ${e.message}")
        }
    }

    fun getLogs(): List<DebugLogEntry> {
        val dbHelper = getDbHelper()
        if (dbHelper != null) {
            try {
                return dbHelper.getAllDebugLogs()
            } catch (e: Exception) {
                Log.e(GENERAL_TAG, "Failed to query SQLite logs, falling back to in-memory: ${e.message}")
            }
        }
        // Fallback to memory
        return inMemoryLogs.mapIndexed { index, entry ->
            DebugLogEntry(
                id = index,
                timestamp = entry.timestamp,
                tag = entry.tag,
                level = entry.level,
                message = entry.message,
                stackTrace = entry.stackTrace
            )
        }.reversed()
    }

    fun clear() {
        inMemoryLogs.clear()
        try {
            getDbHelper()?.clearDebugLogs()
        } catch (e: Exception) {
            Log.e(GENERAL_TAG, "Failed to clear SQLite logs: ${e.message}")
        }
    }

    private fun getStackTraceString(t: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }
}
