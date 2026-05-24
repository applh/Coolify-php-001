package com.example.cameraxapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import com.example.cameraxapp.cronjob.CronJobDatabase
import com.example.cameraxapp.cronjob.CronJobEntity
import com.example.cameraxapp.cronjob.CronJobScheduler
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManagerEngine {
    private const val TAG = "BackupManagerEngine"
    private const val BUFFER_SIZE = 8192

    /**
     * Issues WAL checkpoints for all databases to merge dynamic logs and journals
     * back to primary db files prior to copying.
     */
    fun checkpointDatabases(context: Context) {
        val dbNames = listOf("agenda_hub.db", "cronjob_database")
        for (dbName in dbNames) {
            try {
                val dbFile = context.getDatabasePath(dbName)
                if (dbFile.exists()) {
                    val db = SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READWRITE
                    )
                    db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE);", null).use { cursor ->
                        if (cursor.moveToFirst()) {
                            Log.d(TAG, "WAL flush checkpoint completed for $dbName (status: ${cursor.getInt(0)})")
                        }
                    }
                    db.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed checkpointing WAL for $dbName: ${e.message}")
            }
        }
    }

    /**
     * Packages SQLite databases, Shared Preferences, and AI chat sessions into one ZIP file.
     */
    fun createBackupZip(context: Context, outStream: OutputStream): Boolean {
        checkpointDatabases(context)
        ZipOutputStream(BufferedOutputStream(outStream)).use { zos ->
            // 1. Package databases directory
            val dbFolder = context.getDatabasePath("agenda_hub.db").parentFile
            if (dbFolder != null && dbFolder.exists()) {
                packDirectory(dbFolder, "databases/", zos)
            }

            // 2. Package AI Team conversations (sessions) directory
            val sessionsFolder = File(context.filesDir, "sessions")
            if (sessionsFolder.exists()) {
                packDirectory(sessionsFolder, "sessions/", zos)
            }

            // 3. Package system-level datastores preferences folder
            val dataStoreFolder = File(context.filesDir, "datastore")
            if (dataStoreFolder.exists()) {
                packDirectory(dataStoreFolder, "datastore/", zos)
            }

            // 4. Package traditional android shared_prefs files
            val sharedPrefsFolder = File(context.filesDir.parent, "shared_prefs")
            if (sharedPrefsFolder.exists()) {
                packDirectory(sharedPrefsFolder, "shared_prefs/", zos)
            }

            // 5. Package Lumina Pictures & Gemini Canvas media directories
            val picturesFolder2 = File(context.filesDir, "Pictures")
            if (picturesFolder2.exists()) {
                packDirectory(picturesFolder2, "Pictures/", zos)
            }
            val canvasFolder = File(context.filesDir, "GeminiCanvas")
            if (canvasFolder.exists()) {
                packDirectory(canvasFolder, "GeminiCanvas/", zos)
            }
        }
        return true
    }

    private fun packDirectory(folder: File, relativePath: String, zos: ZipOutputStream) {
        val files = folder.listFiles() ?: return
        val buffer = ByteArray(BUFFER_SIZE)

        for (file in files) {
            if (file.isDirectory) {
                packDirectory(file, "$relativePath${file.name}/", zos)
                continue
            }
            // Skip dynamic locks or transient journal files
            val nameLc = file.name.lowercase()
            if (nameLc.endsWith("-wal") || nameLc.endsWith("-shm") || nameLc.endsWith("-journal")) {
                continue
            }

            try {
                BufferedInputStream(FileInputStream(file), BUFFER_SIZE).use { bis ->
                    val zipEntryName = "$relativePath${file.name}"
                    zos.putNextEntry(ZipEntry(zipEntryName))
                    var count: Int
                    while (bis.read(buffer, 0, BUFFER_SIZE).also { count = it } != -1) {
                        zos.write(buffer, 0, count)
                    }
                    zos.closeEntry()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error packaging archive file ${file.absolutePath}: ${e.message}")
            }
        }
    }

    /**
     * Unpacks a ZIP archive into staging and applies file overrides atomically.
     */
    fun restoreBackupZip(context: Context, inputStream: InputStream): Boolean {
        val stageDir = File(context.cacheDir, "backup_restoration_stage")
        if (stageDir.exists()) {
            stageDir.deleteRecursively()
        }
        stageDir.mkdirs()

        try {
            // Unpack everything into cache staging area
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry = zis.nextEntry
                val buffer = ByteArray(BUFFER_SIZE)

                while (entry != null) {
                    val destFile = File(stageDir, entry.name)
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(destFile), BUFFER_SIZE).use { bos ->
                            var count: Int
                            while (zis.read(buffer, 0, BUFFER_SIZE).also { count = it } != -1) {
                                bos.write(buffer, 0, count)
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
            }

            // Verify integrity (check thatdatabases file and datastore are staged)
            val stagedDatabases = File(stageDir, "databases")
            if (!stagedDatabases.exists() || stagedDatabases.listFiles()?.isEmpty() == true) {
                Log.e(TAG, "Verification check failed: No valid databases located in zip file.")
                return false
            }

            // Execute atomic commit updates
            commitStagedFiles(context, stageDir)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Decompression staging restoration crashed", e)
            return false
        } finally {
            stageDir.deleteRecursively()
        }
    }

    private fun commitStagedFiles(context: Context, stageDir: File) {
        // Enforce DB closed states first
        try {
            context.deleteDatabase("agenda_hub.db")
            context.deleteDatabase("cronjob_database")
        } catch (e: Exception) {
            Log.e(TAG, "Closing/Deleting existing databases failed: ${e.message}")
        }

        // 1. Copy Restore Databases Folder cleanly
        val stagedDatabases = File(stageDir, "databases")
        if (stagedDatabases.exists()) {
            val realDbFolder = context.getDatabasePath("agenda_hub.db").parentFile ?: return
            realDbFolder.mkdirs()
            stagedDatabases.listFiles()?.forEach { file ->
                file.copyTo(File(realDbFolder, file.name), overwrite = true)
            }
        }

        // 2. Restore Sessions Folder cleanly
        val stagedSessions = File(stageDir, "sessions")
        val realSessionsFolder = File(context.filesDir, "sessions")
        if (stagedSessions.exists()) {
            realSessionsFolder.deleteRecursively()
            realSessionsFolder.mkdirs()
            stagedSessions.copyRecursively(realSessionsFolder, overwrite = true)
        }

        // 3. Restore Preferences DataStores cleanly
        val stagedDatastore = File(stageDir, "datastore")
        val realDatastoreFolder = File(context.filesDir, "datastore")
        if (stagedDatastore.exists()) {
            realDatastoreFolder.deleteRecursively()
            realDatastoreFolder.mkdirs()
            stagedDatastore.copyRecursively(realDatastoreFolder, overwrite = true)
        }

        // 4. Restore Traditional SharedPreferences cleanly
        val stagedSharedPrefs = File(stageDir, "shared_prefs")
        val realSharedPrefsFolder = File(context.filesDir.parent, "shared_prefs")
        if (stagedSharedPrefs.exists()) {
            realSharedPrefsFolder.deleteRecursively()
            realSharedPrefsFolder.mkdirs()
            stagedSharedPrefs.copyRecursively(realSharedPrefsFolder, overwrite = true)
        }

        // 5. Restore Media Galleries cleanly
        val stagedPictures = File(stageDir, "Pictures")
        val realPicturesFolder = File(context.filesDir, "Pictures")
        if (stagedPictures.exists()) {
            realPicturesFolder.deleteRecursively()
            realPicturesFolder.mkdirs()
            stagedPictures.copyRecursively(realPicturesFolder, overwrite = true)
        }

        val stagedCanvas = File(stageDir, "GeminiCanvas")
        val realCanvasFolder = File(context.filesDir, "GeminiCanvas")
        if (stagedCanvas.exists()) {
            realCanvasFolder.deleteRecursively()
            realCanvasFolder.mkdirs()
            stagedCanvas.copyRecursively(realCanvasFolder, overwrite = true)
        }
    }

    /**
     * Code-generates populated test events, alarms, settings values, and AI discussions
     * to serve as a 1-tap fast bootstrapper utility for sandbox testers.
     */
    fun seedTesterMockProfile(context: Context) {
        val now = System.currentTimeMillis()

        // 1. Setup DataStore settings placeholder keys
        try {
            val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            sharedPrefs.edit().apply {
                putString("theme_mode", "2") // dark active
                putString("color_theme", "1") // lumina active
                putString("gemini_api_key", "AI_STUDIO_TEST_BETA_KEY")
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Clear and Insert mock calendar events & configurations using SQLite Database helper
        try {
            val dbHelper = AgendaDatabaseHelper(context)
            val db = dbHelper.writableDatabase

            db.execSQL("DELETE FROM agenda_events")
            db.execSQL("DELETE FROM alarms")
            db.execSQL("DELETE FROM cron_jobs")

            // Events
            val e1 = ContentValues().apply {
                put("title", "[TESTER] QA Stability Review")
                put("notes", "Auto-seeded via Backup bootstrap panel. Test WAL backups.")
                put("date_millis", now + (1800 * 1000)) // 30 mins
                put("duration", 45)
                put("color", "Primary")
            }
            db.insert("agenda_events", null, e1)

            val e2 = ContentValues().apply {
                put("title", "[TESTER] Heavy Multi-App Simulation")
                put("notes", "Simulates continuous file access and background WorkManager triggers.")
                put("date_millis", now + (2 * 3600 * 1000))
                put("duration", 90)
                put("color", "Secondary")
            }
            db.insert("agenda_events", null, e2)

            // Alarms
            val a1 = ContentValues().apply {
                put("time_millis", now + (600 * 1000)) // 10 mins
                put("label", "Automated Playwright Suite")
                put("is_active", 1)
            }
            db.insert("alarms", null, a1)

            // Cron jobs
            val cronBackup = ContentValues().apply {
                put("name", "App State Backup Sync")
                put("expression", "*/15 * * * *")
                put("is_active", 1)
                put("last_run", now - 300000)
                put("status", "SUCCESS")
            }
            db.insert("cron_jobs", null, cronBackup)

            db.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed seeding SQLite agenda database: ${e.message}")
        }

        // 3. Setup mock AI Conversations folders
        try {
            val sessionsDir = File(context.filesDir, "sessions")
            if (sessionsDir.exists()) {
                sessionsDir.deleteRecursively()
            }
            sessionsDir.mkdirs()

            // Manifest
            val manifestFile = File(context.filesDir, "sessions_manifest.json")
            val manifestContent = """
                [
                  {
                    "id": "mock_test_session_1",
                    "title": "Android Backup Architectures",
                    "latestSummary": "Discussing sandbox backup restore and ZIP buffers",
                    "lastActiveTime": $now,
                    "totalTokens": 1420
                  }
                ]
            """.trimIndent()
            FileOutputStream(manifestFile).use { it.write(manifestContent.toByteArray()) }

            // Segment Thread
            val threadFile = File(sessionsDir, "session_mock_test_session_1.json")
            val threadContent = """
                {
                  "sessionId": "mock_test_session_1",
                  "messages": [
                    {
                      "id": "m1",
                      "role": "USER",
                      "content": "How do we secure Android local directories from loss?",
                      "modality": "TEXT",
                      "timestamp": ${now - 60000},
                      "durationMs": 0,
                      "calculatedCost": 0.0,
                      "tokensUsed": 10,
                      "modelName": "system"
                    },
                    {
                      "id": "m2",
                      "role": "MODEL",
                      "content": "Implement standard Java ZIP Buffering Streams and merge transactions using WAL sqlite checkpoint checkpoints.",
                      "modality": "TEXT",
                      "timestamp": ${now - 30000},
                      "durationMs": 1200,
                      "calculatedCost": 0.0001,
                      "tokensUsed": 24,
                      "modelName": "gemini-2.5-flash"
                    }
                  ]
                }
            """.trimIndent()
            FileOutputStream(threadFile).use { it.write(threadContent.toByteArray()) }

        } catch (e: Exception) {
            Log.e(TAG, "Failed seeding AI conversations: ${e.message}")
        }
    }
}
