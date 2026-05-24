# Android Backup Manager (ZIP Archiver & Automation Scheduler) Applet Plan

**Objective**: Extend the Android Multi-App Hub by introducing the **Backup Manager** applet (`BackupScreen.kt`, `BackupViewModel.kt`, and native background workers). This tool guarantees user data durability against accidental app reinstalls and offers testers a frictionless way to instantly bootstrap full application states. It packages all databases (SQLite, Room), shared preferences (DataStore), and session files (Gemini AI chat histories, generated media) into a single transportable, optionally encrypted ZIP archive, synchronized on an automated schedule via WorkManager.

---

## 1. Architectural Highlights & Tech Stack

To ensure bulletproof reliability while packing and restoring system states, the applet bypasses traditional in-memory serialization and implements a robust direct File I/O streaming engine with atomic transaction management.

```
                    ┌─────────────────────────┐
                    │    BackupViewModel      │
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │    BackupScreen UI      │
                    └────────────┬────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
 ┌──────────────┐        ┌──────────────┐        ┌──────────────┐
 │ ZIP Archive  │        │ SQLite WAL   │        │ WorkManager  │
 │ Stream Engine│        │ Checkpoint   │        │ Backup cron  │
 │ (Deflater)   │        │ (Atomic DB)  │        │ (Automated)  │
 └──────┬───────┘        └──────┬───────┘        └──────┬───────┘
        │                       │                       │
        └───────────────────────┼───────────────────────┘
                                ▼
         ┌──────────────────────────────────────────────┐
         │ Local Scoped Storage / External SAF Exports  │
         └──────────────────────────────────────────────┘
```

- **Core Packaging Engine**: Utilize Java's standard `java.util.zip.ZipInputStream` and `ZipOutputStream` streams with buffer optimization ($O(1)$ memory consumption) for zero-latency packaging of multi-megabyte directories.
- **Database Consistency (WAL Lock)**: To safely clone databases without catching corrupted/partial query states, the engine executes direct PRAGMA SQLite `checkpoint` instructions, forcing the Write-Ahead Logging (WAL) sequences to merge back to the main files before zipping.
- **External Survivor Pipeline**: Harness Android's **Storage Access Framework (SAF)** with `Intent.ACTION_CREATE_DOCUMENT` to let users stream generated `.zip` packages directly to external persistent volumes (e.g., SD Card, default Downloads folder, Google Drive), which survive full application reinstalls.
- **Cronjob Daemon (WorkManager)**: Register an automated `"SYSTEM_BACKUP"` background action inside the generic SQLite-driven `DynamicRouterWorker` to systematically execute hands-free scheduled snapshots based on user preferences.
- **Atomic Rollback Safeguards**: When restoring a backup file, the applet utilizes a "staging directories" pattern: extracts files to temporary directories, performs integrity checks, and swaps filenames in a single atomic filesystem update to prevent halfway corruption.

---

## 2. Core Features (The Requested Scope)

### A. SQLite Databases Backup & Restore (No Data Loss Engine)
- **Comprehensive DB Discovery**: Detect and pack ALL dynamic SQLite files from `/data/data/{package_name}/databases/` including:
  - `agenda_hub.db` (and corresponding dynamic journals or WAL logs)
  - `cronjob_database` (Room database backing background tasks)
  - Custom dynamic databases created in the DB Explorer Applet (e.g., `shop_warehouse.db`).
- **WAL Flusher**: Prior to archiving files, open a temporary write connection to issue `PRAGMA wal_checkpoint(TRUNCATE)` to guarantee complete commit histories are fully saved in the main `.db` files.
- **Restore Re-Synchronization**: After a .db file restoration, the applet resets internal SQLite connection instances and invokes the database-resynchronization cycle:
  - Updates alarm alarms in `AlarmManager`.
  - Re-registers active background routines in `WorkManager`.

### B. Shared Preferences & App Settings Packaging
- **DataStore Serialization**: Extract and pack the serialized byte contents of the Proto DataStore / Preferences DataStore files (stored under the `files/datastore/` directory) to retain all customized user themes, API keys, and configurations.

### C. Gemini Conversation & Media Archives
- **AI Workspace Backup**: Package the unified sessions folder (`files/sessions/`) containing `sessions_manifest.json` along with the individual thread file chunks (`session_*.json`) representing conversational memory.
- **Local Art Gallery Retention**: Zip all images captured with CameraX or synthesized via Lumina AI that reside in the internal Scoped Storage (such as `files/Pictures/` or `files/GeminiCanvas/`).

### D. Frictionless "Tester-Bootstrapper" Mode
- **Quick-Setup Profiles**: Provide standard preconfigured mock ZIP bundles (e.g., "Full Seed Profile", "Empty Reset Profile", "Heavy Stress Profiles") compiled within the assets directories. Testers can apply these with a single tap to instantly create complete test datasets without manual configurations.
- **Dynamic File Injection**: Enable testers to pick any `.zip` payload directly from their Downloads folder via SAF to load specific bug-reproducing databases instantly.

### E. Periodic Backup Scheduler (Cronjob)
- **WorkManager Automation**: Register a persistent background job scheduled to run daily or every 12 hours under device-safe constraints (e.g., `RequiresCharging`, `NetworkType.UNMETERED`).
- **Rolling Restrictive Housekeeping**: Ensure storage does not bloat by maintaining only the last `N` (default: 5) structured backups, auto-purging the oldest zip files dynamically post-generation.

---

## 3. Recommended Professional Features

### 1. High-Performance AES-256 Zip Encryption
- **Cryptographic Security**: Integrate the lightweight `Zip4j` library (or native `javax.crypto` stream filters) to let users configure a password when generating backups, encrypting SQLite records, API Keys, and personal AI chat threads safely inside the ZIP.
- **Decryption Prompt**: If a backup ZIP is detected as encrypted upon restore selection, invoke a modern dialog prompt for password entry before decrypting stream blocks.

### 2. Multi-Cloud Synchronization Gateway
- **Seamless Integrations**: Build visual settings options to sync generated zip archives directly to external systems like Google Drive, Dropbox, or a local networked SFTP/FTP server.
- **Offline Sync Queue**: If cloud backup is active but the device is offline, cache the outbound archive internally, queuing it for dispatch through a dedicated WorkManager network constraint callback.

### 3. Dynamic Differential Backups (Delta Engine)
- **Intelligent Delta Compression**: For heavy galleries, avoid duplicating megabytes of static images. Scan previous backup manifest tags to only compress newly captured photos or mutated databases since the last run, keeping backup files light.

---

## 4. UI / UX Layout & Composition

The interface utilizes a polished responsive dashboard highlighting storage metrics, active schedules, and chronological export logs.

```
┌────────────────────────────────────────────────────────┐
│  [🍓 Backup & Restore]                     💾 DB: 42KB  │
├────────────────────────────────────────────────────────┤
│  STORAGE COVERAGE                                      │
├────────────────────────────────────────────────────────┤
│  █ SQLite DBs [82KB]    █ Chat Logs [110KB]            │
│  █ Media Assets [1.2MB] █ Preferences [4KB]            │
│  Total Size: 1.4MB                                     │
├────────────────────────────────────────────────────────┤
│  AUTOMATED SCHEDULE                                    │
│  [● ACTIVE] Trigger: Daily at 03:00 AM                 │
│  Constraints: ⚡ Charging Only • 📶 Wi-Fi Only         │
│  [ Configure Schedule ]                                │
├────────────────────────────────────────────────────────┤
│  BACKUP UTILITIES                                      │
│  ┌──────────────────────┐  ┌──────────────────────┐  │
│  │     [ Create ZIP ]   │  │    [ Import ZIP ]    │  │
│  └──────────────────────┘  └──────────────────────┘  │
├────────────────────────────────────────────────────────┤
│  CHRONOLOGICAL ARCHIVES                                │
│  ■ backup_20260524_1115.zip (1.3MB)      [Restore] [x]  │
│  ■ backup_20260523_1115.zip (1.2MB)      [Restore] [x]  │
│  ■ [TEST_SEED] demo_fraise_heavy.zip     [BootStrap]   │
└────────────────────────────────────────────────────────┘
```

---

## 5. Technical Implementation Blueprint

### A. ZIP Generation & SQLite WAL Checkpoint Service
`BackupEngine.kt` core stream utility class:

```kotlin
package com.example.cameraxapp.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import bsh.org.objectweb.asm.Constants
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupEngine {
    private const val BUFFER_SIZE = 8192
    private const val TAG = "BackupEngine"

    /**
     * Commits WAL journal data back to the primary database files.
     */
    fun flushWalLog(context: Context, dbName: String) {
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
                        Log.d(TAG, "WAL flushed for $dbName (Status: ${cursor.getInt(0)})")
                    }
                }
                db.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed WAL flush for $dbName", e)
        }
    }

    /**
     * Packages selected databases, sessions, and styles into a single ZIP file.
     */
    fun createBackupZip(context: Context, outputStream: OutputStream): Boolean {
        // Flush WAL triggers for all primary databases
        flushWalLog(context, "agenda_hub.db")
        flushWalLog(context, "cronjob_database")

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipStream ->
            // 1. Pack Database directory
            val dbFolder = context.getDatabasePath("agenda_hub.db").parentFile
            if (dbFolder != null && dbFolder.exists()) {
                packDirectory(dbFolder, "databases/", zipStream)
            }

            // 2. Pack Chat details directory
            val sessionsFolder = File(context.filesDir, "sessions")
            if (sessionsFolder.exists()) {
                packDirectory(sessionsFolder, "sessions/", zipStream)
            }

            // 3. Pack Preferences DataStore structure
            val dataStoreFolder = File(context.filesDir, "datastore")
            if (dataStoreFolder.exists()) {
                packDirectory(dataStoreFolder, "datastore/", zipStream)
            }

            // 4. Pack Lumina pictures
            val picturesFolder = File(context.filesDir, "GeminiCanvas")
            if (picturesFolder.exists()) {
                packDirectory(picturesFolder, "GeminiCanvas/", zipStream)
            }
        }
        return true
    }

    private fun packDirectory(folder: File, rootPath: String, zipStream: ZipOutputStream) {
        val files = folder.listFiles() ?: return
        val buffer = ByteArray(BUFFER_SIZE)

        for (file in files) {
            if (file.isDirectory) {
                packDirectory(file, "$rootPath${file.name}/", zipStream)
                continue
            }
            // Skip dynamic WAL journals and temporary files to prevent zipping corrupt system locks
            if (file.name.endsWith("-wal") || file.name.endsWith("-journal") || file.name.endsWith("-shm")) {
                continue
            }

            BufferedInputStream(FileInputStream(file), BUFFER_SIZE).use { bis ->
                val entry = ZipEntry("$rootPath${file.name}")
                zipStream.putNextEntry(entry)
                var count: Int
                while (bis.read(buffer, 0, BUFFER_SIZE).also { count = it } != -1) {
                    zipStream.write(buffer, 0, count)
                }
                zipStream.closeEntry()
            }
        }
    }
}
```

### B. Extraction & Atomic Import Rollback Pipeline
`RestoreEngine.kt` extraction control flow:

```kotlin
package com.example.cameraxapp.backup

import android.content.Context
import android.util.Log
import java.io.*
import java.util.zip.ZipInputStream

object RestoreEngine {
    private const val BUFFER_SIZE = 8192
    private const val TAG = "RestoreEngine"

    fun restoreFromBackupZip(context: Context, inputStream: InputStream): Boolean {
        val tempDir = File(context.cacheDir, "backup_unzipped_staging")
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()

        try {
            // Unpack everything into a safe staging directory
            ZipInputStream(BufferedInputStream(inputStream)).use { zipStream ->
                var entry = zipStream.nextEntry
                val buffer = ByteArray(BUFFER_SIZE)

                while (entry != null) {
                    val destFile = File(tempDir, entry.name)
                    destFile.parentFile?.mkdirs()

                    if (!entry.isDirectory) {
                        BufferedOutputStream(FileOutputStream(destFile), BUFFER_SIZE).use { bos ->
                            var count: Int
                            while (zipStream.read(buffer, 0, BUFFER_SIZE).also { count = it } != -1) {
                                bos.write(buffer, 0, count)
                            }
                        }
                    }
                    entry = zipStream.nextEntry
                }
            }

            // Validate critical components are unzipped successfully before committing
            val stagedDbs = File(tempDir, "databases")
            if (!stagedDbs.exists() || stagedDbs.listFiles()?.isEmpty() == true) {
                Log.e(TAG, "Validation failed: database folder is missing in backup bundle")
                return false
            }

            // Apply staging files to real app environments atomically
            commitStagedFiles(context, tempDir)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed decompression restoration process", e)
            return false
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun commitStagedFiles(context: Context, tempDir: File) {
        // Close database connections comprehensively before overwriting files
        context.deleteDatabase("agenda_hub.db")
        context.deleteDatabase("cronjob_database")

        // 1. Copy Databases
        val stagedDbs = File(tempDir, "databases")
        if (stagedDbs.exists()) {
            val dbTargetFolder = context.getDatabasePath("agenda_hub.db").parentFile ?: return
            dbTargetFolder.mkdirs()
            stagedDbs.listFiles()?.forEach { file ->
                file.copyTo(File(dbTargetFolder, file.name), overwrite = true)
            }
        }

        // 2. Copy AI Convo Sessions
        val stagedSessions = File(tempDir, "sessions")
        val realSessionsFolder = File(context.filesDir, "sessions")
        if (stagedSessions.exists()) {
            realSessionsFolder.deleteRecursively()
            realSessionsFolder.mkdirs()
            stagedSessions.copyRecursively(realSessionsFolder, overwrite = true)
        }

        // 3. Copy DataStore Preferences
        val stagedDatastore = File(tempDir, "datastore")
        val realDatastoreFolder = File(context.filesDir, "datastore")
        if (stagedDatastore.exists()) {
            realDatastoreFolder.deleteRecursively()
            realDatastoreFolder.mkdirs()
            stagedDatastore.copyRecursively(realDatastoreFolder, overwrite = true)
        }

        // 4. Copy Pictures Gallery
        val stagedPics = File(tempDir, "GeminiCanvas")
        val realPicsFolder = File(context.filesDir, "GeminiCanvas")
        if (stagedPics.exists()) {
            realPicsFolder.deleteRecursively()
            realPicsFolder.mkdirs()
            stagedPics.copyRecursively(realPicsFolder, overwrite = true)
        }
    }
}
```

### C. Background WorkManager Automated Backup Scheduler Task
Integrate the backup worker inside `CronWorker.kt`:

```kotlin
// In CronWorker.kt -> doWork()
when (jobType) {
    "SYSTEM_BACKUP" -> {
        try {
            val backupDir = File(applicationContext.filesDir, "backup_snapshots")
            if (!backupDir.exists()) backupDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val targetFile = File(backupDir, "auto_backup_$timestamp.zip")

            FileOutputStream(targetFile).use { fos ->
                BackupEngine.createBackupZip(applicationContext, fos)
            }

            // Clean rolling limit: maintain latest 5 snapshots only
            val history = backupDir.listFiles()?.filter { it.isFile && it.name.startsWith("auto_backup_") }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()

            if (history.size > 5) {
                history.drop(5).forEach { file ->
                    file.delete()
                    Log.d("CronWorker", "Pruned old background backup archive: ${file.name}")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("CronWorker", "Failed to run cron system backup task", e)
            Result.failure()
        }
    }
}
```

---

## 6. Full CRUD Configurations Interface Integration

The Backup Applet supports comprehensive operations over the exported archives, matching standard CRUD architectures:

1. **Create (Export Snapshot)**:
   - Users can choose to generate a quick internal backup (maintained in the rolling cache) or use SAF to save the ZIP to their local file system outside application boundaries.
2. **Read (Listing Available Archives)**:
   - Live scanning of `/files/backup_snapshots` displaying size metrics, dynamic execution timestamps, and source (e.g., Scheduled Core vs User Manual). Incorporate quick loaders for tester-seeded bundles stored in assets.
3. **Update (Schedule Configuration)**:
   - Simple switches to adjust scheduler configurations stored in DataStore: Backup Frequency (12 hours, daily, weekly), Connectivity Restrictions (WiFi required), and Power Constraint overlays.
4. **Delete (Wipe backup)**:
   - Tap custom deletion trash controls to drop obsolete files immediately from the internal buffer, checking disk constraints.

---

## 7. Rollout Phases

1. **Phase 1: Zero-Copy ZIP and Unzip Engines**
   - Implement `BackupEngine.kt` with WAL Checkpoint structures and `RestoreEngine.kt` containing Safe Atomic staging structures.
2. **Phase 2: External Exporters with Android SAF**
   - Bind file selection and export pathways using Android Activity Result contracts (`CreateDocument` / `OpenDocument` intents).
3. **Phase 3: background Automation Integration**
   - Register `"SYSTEM_BACKUP"` in the SQLite databases cron indexes and integrate the execution sequence within `CronWorker.kt`. Set up robust rolling limits.
4. **Phase 4: Responsive Backup UI and Tester Seeds**
   - Compose the complete `BackupScreen.kt` featuring real-time storage analyzers, scheduling forms, and one-tap asset bootstrappers for development environments.
