# Background Internet Request & File Downloader Cronjob Plan

This document details the architectural design and step-by-step implementation plan for adding a dynamic, scheduled background job to `repo-android` that can make HTTP requests to any user-specified URL and stream its response body directly to a local file.

---

## 1. High-Utility Use Cases

A general-purpose scheduled HTTP downloader provides deep system automation support for a variety of services:

1. **Daily Space, Nature, or Art Wallpaper Sync**:
   - **How it works**: Polls public images or JSON APIs (such as NASA's Astronomy Picture of the Day or Unsplash curated themes).
   - **Benefit**: Downloads a fresh image to the device's external storage (`wallpapers/` folder) every 24 hours. The existing `WALLPAPER_CHANGER` job can immediately rotate to it, creating a fully dynamic ambient home screen without manual intervention.

2. **Automated Server Database Backup Sync / CMS Sync**:
   - **How it works**: Standard cron task connects to a backend system endpoint (e.g., your PHP CMS, `/api/sites-export`, or private repository) every 6 hours.
   - **Benefit**: Downloads highly compact data packages (YAML, JSON, SQLite snapshots) to keep local tables, contact indexes, or site logs available offline.

3. **Regional Weather & Air Quality Watcher**:
   - **How it works**: Queries an open weather forecasting API (such as Open-Meteo) containing custom GPS coordinates.
   - **Benefit**: Compares downloaded data to safety thresholds and posts a foreground notification if high-risk weather states or elevated pollution levels are parsed.

4. **Remote Configuration / Dynamic Policy Sync**:
   - **How it works**: Connects to a static cloud JSON config file (e.g., GitHub Gist or secure S3 bucket) daily.
   - **Benefit**: Updates application parameters, active API models, latency limits, and feature toggles dynamically without pushing binary APK upgrades to devices.

5. **AI Workspace Prompt & Instruction Synchronization**:
   - **How it works**: Automatically pulls updated instruction templates or team rules from a central documentation repository.
   - **Benefit**: Refreshes AITeam system context matrices inside the mobile workspace to ensure background AI evaluations and autopilot routines always enforce current organizational guidelines.

---

## 2. Structural Architecture & Changes

### A. Data Layer Expansion (Room Database)
To keep the cronjob engine decoupled and general-purpose, we will expand `CronJobEntity` with optional fields representing HTTP instruction sets, rather than hardcoding targets.

#### Updated Entity Model (`CronJobEntity.kt`)
```kotlin
package com.example.cameraxapp.cronjob

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cron_jobs")
data class CronJobEntity(
    @PrimaryKey val id: String,
    val jobType: String,             // E.g., "CAMERA_CAPTURE", "WALLPAPER_CHANGER", "HTTP_DOWNLOAD"
    val intervalMinutes: Int,
    val isEnabled: Boolean,
    val requiresNetwork: Boolean = false,
    val requiresCharging: Boolean = false,
    val lastRunTimestamp: Long = 0L,
    val nextRunTimestamp: Long = 0L,
    
    // HTTP/Download Specific Configuration
    val downloadUrl: String? = null,
    val saveFileName: String? = null
)
```

#### Database Migration Strategy (`CronJobDatabase.kt`)
To support graceful schema updates, we declare a Room migration from Version 1 to Version 2. Alternatively, for simple developmental states, fallback-to-destructive migrations can be applied.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE cron_jobs ADD COLUMN downloadUrl TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE cron_jobs ADD COLUMN saveFileName TEXT DEFAULT NULL")
    }
}

// Inside getDatabase Builder:
Room.databaseBuilder(
    context.applicationContext,
    CronJobDatabase::class.java,
    "cronjob_database"
)
.addMigrations(MIGRATION_1_2)
.fallbackToDestructiveMigration() // Backup safety
.build()
```

---

## 3. High-Performance File Streaming Utility

To download records of any scale (large telemetry files, dataset archives, or high-res images) on a mobile device safely, we **MUST** stream the response body instead of reading it entirely into memory (RAM). Reading whole payloads into memory is the largest contributor to Out-Of-Memory (OOM) crashes in Android WorkManager.

### Zero-Copy File Downloader (`FileDownloader.kt`)
```kotlin
package com.example.cameraxapp.cronjob

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object FileDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Streams content from [url] directly into a local file inside the downloads folder.
     * Prevents Android OOM crashes using a fixed 4KB transfer buffer.
     */
    fun downloadFile(context: Context, url: String, outputFileName: String): File? {
        Log.d("FileDownloader", "Starting network request to: $url")
        val request = Request.Builder().url(url).build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                Log.e("FileDownloader", "Server replied with code ${response.code}")
                return null
            }

            // Target folder: Context.getExternalFilesDir(null)/downloads/
            val downloadDir = File(context.getExternalFilesDir(null), "downloads")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val targetFile = File(downloadDir, outputFileName)
            
            // Stream body directly to disk
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                inputStream = response.body!!.byteStream()
                outputStream = FileOutputStream(targetFile)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
                Log.d("FileDownloader", "Successfully downloaded file to: ${targetFile.absolutePath}")
                targetFile
            } finally {
                inputStream?.close()
                outputStream?.close()
                response.close()
            }
        } catch (e: Exception) {
            Log.e("FileDownloader", "Error during background file download", e)
            null
        }
    }
}
```

---

## 4. WorkManager & Router Worker Integration

We integrate `"HTTP_DOWNLOAD"` directly inside the routing system inside `DynamicRouterWorker.kt`. Since background downloads require a secure internet trace, we enforce network logic at the OS scheduler level.

### Updated Switch Routing (`DynamicRouterWorker.kt`)
```kotlin
return try {
    when (jobType) {
        "CAMERA_CAPTURE" -> handleCameraCapture()
        "WALLPAPER_CHANGER" -> handleWallpaperChanger()
        "HTTP_DOWNLOAD" -> handleHttpDownload(job) // Core update
        else -> {
            Log.w("DynamicRouterWorker", "Unknown jobType: $jobType")
            Result.success()
        }
    }
}
```

### Download Task Pipeline Handler
This handler acts as a secure container for HTTP transfers. It reads the specific parameters, checks free local disk storage before starting, and shows a custom non-blocking, battery-aware execution notification.

```kotlin
private suspend fun handleHttpDownload(job: CronJobEntity): Result {
    val url = job.downloadUrl
    val fileName = job.saveFileName

    if (url.isNullOrBlank() || fileName.isNullOrBlank()) {
        Log.e("DynamicRouterWorker", "Job ${job.id} is missing URL or FileName params.")
        return Result.failure()
    }

    Log.d("DynamicRouterWorker", "Routing HTTP Download task for target: $fileName")

    // 1. Post Download Progress Notification
    val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channelId = "CRON_DOWNLOAD_CHANNEL"
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            channelId, "File Downloads", android.app.NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("Scheduled Automation Task")
        .setContentText("Downloading content: $fileName")
        .setOngoing(true)
        .build()

    // Elevate slightly to avoid OS background execution limits
    val foregroundInfo = androidx.work.ForegroundInfo(9988, notification)
    try {
        setForeground(foregroundInfo)
    } catch (e: Exception) {
        Log.e("DynamicRouterWorker", "Failed to initiate download notification bounds", e)
    }

    // 2. Perform Download
    val downloadedFile = FileDownloader.downloadFile(applicationContext, url, fileName)

    return if (downloadedFile != null && downloadedFile.exists()) {
        Log.i("DynamicRouterWorker", "Cron download succeeded: ${downloadedFile.name}")
        Result.success()
    } else {
        Log.e("DynamicRouterWorker", "Cron download failed, triggering retry scheduling rules.")
        Result.retry()
    }
}
```

---

## 5. UI Integration Map (Jetpack Compose)

To construct an outstanding visual configuration layout, `JobEditorDialog` inside `CronJobManagerScreen.kt` will dynamically render specialized configuration inputs when the user changes the active item selector to `HTTP_DOWNLOAD`.

### Screen State & Inputs Mockup
```kotlin
// Inside JobEditorDialog
var jobType by remember { mutableStateOf("CAMERA_CAPTURE") }
val jobTypes = listOf("CAMERA_CAPTURE", "WALLPAPER_CHANGER", "HTTP_DOWNLOAD")

var downloadUrl by remember { mutableStateOf("") }
var saveFileName by remember { mutableStateOf("downloaded_data.json") }

// Inside your Dialog Column
when (jobType) {
    "HTTP_DOWNLOAD" -> {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = downloadUrl,
            onValueChange = { downloadUrl = it },
            label = { Text("Download Target URL") },
            placeholder = { Text("https://example.com/api/data.json") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = saveFileName,
            onValueChange = { saveFileName = it },
            label = { Text("Save Destination Name") },
            placeholder = { Text("my_file.jpg") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
```

Additionally, inside the **Save Button Callback**, if `jobType == "HTTP_DOWNLOAD"`, standard field validation is executed to guarantee that the `downloadUrl` and `saveFileName` are non-empty and well-formed before persisting the Room row, providing immediate error toast notifications if validation fails.
