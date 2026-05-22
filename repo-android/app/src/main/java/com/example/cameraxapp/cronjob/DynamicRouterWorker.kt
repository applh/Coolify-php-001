package com.example.cameraxapp.cronjob

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DynamicRouterWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString("jobId") ?: return Result.failure()
        val jobType = inputData.getString("jobType") ?: return Result.failure()

        Log.d("DynamicRouterWorker", "Executing job: $jobId of type: $jobType")

        val database = CronJobDatabase.getDatabase(applicationContext)
        val dao = database.cronJobDao()

        val job = dao.getJobById(jobId)
        if (job == null || !job.isEnabled) {
            Log.d("DynamicRouterWorker", "Job $jobId not found or disabled. Cancel work.")
            return Result.success()
        }

        return try {
            when (jobType) {
                "CAMERA_CAPTURE" -> handleCameraCapture()
                "WALLPAPER_CHANGER" -> handleWallpaperChanger()
                else -> {
                    Log.w("DynamicRouterWorker", "Unknown jobType: $jobType")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Log.e("DynamicRouterWorker", "Error executing job $jobId", e)
            Result.retry()
        }.also {
            // Update lastRunTimestamp
            dao.updateJob(job.copy(
                lastRunTimestamp = System.currentTimeMillis(),
                nextRunTimestamp = System.currentTimeMillis() + job.intervalMinutes * 60 * 1000L
            ))
        }
    }

    private suspend fun handleCameraCapture(): Result {
        Log.d("DynamicRouterWorker", "Handling CAMERA_CAPTURE...")
        
        // Promote to Foreground Service
        val notificationId = 12345
        val channelId = "CRON_JOB_BACKGROUND"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Background Tasks",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Capturing Background Photo")
            .setContentText("Cronjob is taking a picture...")
            .setOngoing(true)
            .build()
            
        val foregroundInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 14+ specific foreground service type for camera
            val foregroundServiceType = if (android.os.Build.VERSION.SDK_INT >= 34) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            } else {
                0
            }
            androidx.work.ForegroundInfo(notificationId, notification, foregroundServiceType)
        } else {
            androidx.work.ForegroundInfo(notificationId, notification)
        }
        
        try {
            setForeground(foregroundInfo)
        } catch (e: Exception) {
            Log.e("DynamicRouterWorker", "Failed to set foreground service", e)
            return Result.failure()
        }

        val file = CameraCaptureManager.captureImage(applicationContext)
        return if (file != null) {
            Log.d("DynamicRouterWorker", "CAMERA_CAPTURE success: \${file.absolutePath}")
            Result.success()
        } else {
            Log.e("DynamicRouterWorker", "CAMERA_CAPTURE failed")
            Result.retry()
        }
    }

    private suspend fun handleWallpaperChanger(): Result {
        Log.d("DynamicRouterWorker", "Handling WALLPAPER_CHANGER...")
        
        val imageDir = java.io.File(applicationContext.getExternalFilesDir(null), "wallpapers")
        if (!imageDir.exists()) imageDir.mkdirs()
        
        val internalImages = imageDir.listFiles { file -> 
            file.isFile && (file.extension.equals("jpg", true) || file.extension.equals("png", true)) 
        }?.toList() ?: emptyList()
        
        val settingsRepo = com.example.cameraxapp.SettingsRepository(applicationContext)
        val externalUriString = kotlinx.coroutines.flow.first(settingsRepo.wallpaperFolderUri)
        val externalImages = mutableListOf<androidx.documentfile.provider.DocumentFile>()
        
        if (externalUriString.isNotEmpty()) {
            try {
                val treeUri = android.net.Uri.parse(externalUriString)
                val docTree = androidx.documentfile.provider.DocumentFile.fromTreeUri(applicationContext, treeUri)
                if (docTree != null && docTree.isDirectory) {
                    docTree.listFiles().forEach { docFile ->
                        if (docFile.isFile && (docFile.type?.startsWith("image/") == true || docFile.name?.endsWith(".jpg", true) == true || docFile.name?.endsWith(".png", true) == true)) {
                            externalImages.add(docFile)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        if (internalImages.isNotEmpty() || externalImages.isNotEmpty()) {
            val totalSize = internalImages.size + externalImages.size
            val randomIndex = (0 until totalSize).random()
            
            val wallpaperManager = android.app.WallpaperManager.getInstance(applicationContext)
            var bitmap: android.graphics.Bitmap? = null
            var selectedName = ""
            
            if (randomIndex < internalImages.size) {
                val file = internalImages[randomIndex]
                bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                selectedName = file.name
            } else {
                val docFile = externalImages[randomIndex - internalImages.size]
                val inputStream = applicationContext.contentResolver.openInputStream(docFile.uri)
                bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                selectedName = docFile.name ?: "Unknown Document"
            }

            if (bitmap != null) {
                wallpaperManager.setBitmap(bitmap)
                Log.d("DynamicRouterWorker", "Wallpaper changed successfully to $selectedName")
                return Result.success()
            } else {
                Log.e("DynamicRouterWorker", "Wallpaper rotation failed: Couldn't decode $selectedName")
                return Result.retry()
            }
        } else {
            Log.w("DynamicRouterWorker", "Wallpaper rotation skipped: No images found.")
            return Result.success() // Success because there's just nothing to do
        }
    }
}
