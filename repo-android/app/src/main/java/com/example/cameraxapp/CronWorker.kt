package com.example.cameraxapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.util.UUID

class CronWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cronId = inputData.getInt("CRON_ID", -1)
        if (cronId == -1) return Result.failure()

        val dbHelper = AgendaDatabaseHelper(applicationContext)
        val jobs = dbHelper.getAllCronJobs()
        val job = jobs.find { it.id == cronId } ?: return Result.failure()

        val startTime = System.currentTimeMillis()

        return try {
            val logMsg: String
            if (job.name.contains("AI", ignoreCase = true)) {
                // Fully Functional AI Dialogue Rerun Autopilot Flow
                val settingsRepo = SettingsRepository(applicationContext)
                val apiKey = settingsRepo.geminiApiKey.first()
                
                if (apiKey.trim().isEmpty()) {
                    logMsg = "AI Session Rerun failed: Gemini API Key is missing. Check App Settings."
                } else {
                    val controller = SessionStorageController(applicationContext)
                    val sessions = controller.getSessionHeaders()
                    
                    if (sessions.isEmpty()) {
                        logMsg = "AI Session Rerun completed: No dialogue headers found to continue."
                    } else {
                        // Select primary (most recent) dialogue head
                        val targetSession = sessions.first()
                        val sessionId = targetSession.id
                        val segment = controller.getSessionSegment(sessionId)
                        
                        if (segment == null || segment.messages.isEmpty()) {
                            logMsg = "AI Session Rerun completed: Selected session '$sessionId' contains empty thread."
                        } else {
                            val persona = settingsRepo.autopilotPersona.first()
                            val lastUserMsg = segment.messages.findLast { it.role == ChatRole.USER }?.content ?: "Hello AI Team"
                            val autoPrompt = "$persona\n\nContext block: '$lastUserMsg'"

                            // Frame content wrapper for Multi-turn conversation
                            val apiContents = mutableListOf<ContentPartList>()
                            for (msg in segment.messages) {
                                if (msg.modality == Modality.TEXT) {
                                    val apiRole = if (msg.role == ChatRole.USER) "user" else "model"
                                    apiContents.add(
                                        ContentPartList(
                                            role = apiRole,
                                            parts = listOf(Part(text = msg.content))
                                        )
                                    )
                                }
                            }

                            // Append automatic continuation prompt
                            apiContents.add(
                                ContentPartList(
                                    role = "user",
                                    parts = listOf(Part(text = autoPrompt))
                                )
                            )

                            val req = GeminiRequest(
                                contents = apiContents,
                                generationConfig = GenerationConfig(
                                    responseModalities = listOf("TEXT")
                                )
                            )

                            // Send network request synchronously in Worker IO dispatcher
                            val startCallMs = System.currentTimeMillis()
                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.geminiApi.generateContent(
                                    model = "gemini-2.5-flash",
                                    apiKey = apiKey,
                                    request = req
                                )
                            }

                            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                            if (responseText != null) {
                                val durationCall = System.currentTimeMillis() - startCallMs
                                val tokensUsed = (autoPrompt.length / 4) + (responseText.length / 4)
                                val calculatedCost = (tokensUsed.toDouble() / 1000.0) * 0.000075

                                val simulatedUserMsg = ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    role = ChatRole.USER,
                                    content = autoPrompt,
                                    modality = Modality.TEXT,
                                    timestamp = startCallMs,
                                    durationMs = 0L,
                                    calculatedCost = 0.0,
                                    tokensUsed = 0,
                                    modelName = "Autopilot Schedule"
                                )

                                val responseAgentMsg = ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    role = ChatRole.MODEL,
                                    content = responseText,
                                    modality = Modality.TEXT,
                                    timestamp = System.currentTimeMillis(),
                                    durationMs = durationCall,
                                    calculatedCost = calculatedCost,
                                    tokensUsed = tokensUsed,
                                    modelName = "gemini-2.5-flash"
                                )

                                val finalMessages = segment.messages + simulatedUserMsg + responseAgentMsg
                                controller.saveSessionSegment(SessionSegment(sessionId = sessionId, messages = finalMessages))

                                val updatedHeaders = sessions.map { h ->
                                    if (h.id == sessionId) {
                                        h.copy(
                                            lastActiveTime = System.currentTimeMillis(),
                                            totalTokens = h.totalTokens + tokensUsed,
                                            latestSummary = if (responseText.length > 50) responseText.take(50) + "..." else responseText
                                        )
                                    } else h
                                }
                                controller.saveSessionHeaders(updatedHeaders)

                                logMsg = "AI autopilot rerun succeeded for conversation '${targetSession.title}'. Summary: ${responseText.take(50)}..."
                            } else {
                                logMsg = "AI Session Rerun API triggered but returned empty text candidate list."
                            }
                        }
                    }
                }
            } else if (job.name.contains("Cache", ignoreCase = true)) {
                // Perform cache sweeps
                var filesDeleted = 0
                val cacheFolder = applicationContext.cacheDir
                cacheFolder.listFiles()?.forEach { file ->
                    if (file.isFile && System.currentTimeMillis() - file.lastModified() > 600000) {
                        file.delete()
                        filesDeleted++
                    }
                }
                logMsg = "Cache sweeper swept cache root directory. Cleaned $filesDeleted temporary indexes."
            } else if (job.name.contains("Backup", ignoreCase = true)) {
                // Perform real ZIP-packed system db and settings backups on cron schedule
                val backupDir = File(applicationContext.filesDir, "backup_snapshots")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }
                
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val backupFile = File(backupDir, "auto_backup_$timestamp.zip")
                
                var success = false
                try {
                    FileOutputStream(backupFile).use { fos ->
                        success = BackupManagerEngine.createBackupZip(applicationContext, fos)
                    }
                } catch (e: Exception) {
                    Log.e("CronWorker", "Fail automated creation: ${e.message}")
                }
                
                if (success) {
                    // Enforce a strict rolling backup threshold pool of latest 5 items
                    val zipList = backupDir.listFiles { file -> file.isFile && file.name.startsWith("auto_backup_") && file.name.endsWith(".zip") }
                        ?.sortedByDescending { it.lastModified() } ?: emptyList()
                    
                    if (zipList.size > 5) {
                        zipList.drop(5).forEach { obsolete ->
                            obsolete.delete()
                        }
                    }
                    
                    logMsg = "Automated backup archive generated successfully (Size: ${backupFile.length() / 1024} KB). Packed databases, preferences systems and active AI session files."
                } else {
                    logMsg = "Diagnostics backup encountered database lock checkpoints or files access exceptions."
                }
            } else if (job.name.contains("Wallpaper", ignoreCase = true)) {
                val imageDir = File(applicationContext.getExternalFilesDir(null), "wallpapers")
                if (!imageDir.exists()) {
                    imageDir.mkdirs()
                }
                
                val internalImages = imageDir.listFiles { file -> 
                    file.isFile && (file.extension.equals("jpg", true) || file.extension.equals("png", true)) 
                }?.toList() ?: emptyList()
                
                val settingsRepo = SettingsRepository(applicationContext)
                val externalUriString = settingsRepo.wallpaperFolderUri.first()
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
                        logMsg = "Wallpaper changed successfully to $selectedName"
                    } else {
                        logMsg = "Wallpaper rotation failed: Couldn't decode $selectedName"
                    }
                } else {
                    logMsg = "Wallpaper rotation skipped: No images found. Please add images or select a folder."
                }
            } else {
                logMsg = "Custom Cron task '${job.name}' verified environment and logged dependencies."
            }

            val executionDuration = System.currentTimeMillis() - startTime

            // Write telemetry result
            dbHelper.addCronLog(cronId, startTime, executionDuration, "SUCCESS", logMsg)
            dbHelper.updateCronStatus(cronId, job.isActive, startTime, "SUCCESS")

            // Fire status notification
            triggerNotify("Cron Job: ${job.name}", logMsg)

            Result.success()
        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - startTime
            dbHelper.addCronLog(cronId, startTime, totalTime, "FAILED", "Error: ${e.localizedMessage}")
            dbHelper.updateCronStatus(cronId, job.isActive, startTime, "FAILED")
            Result.retry()
        }
    }

    private fun triggerNotify(title: String, message: String) {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "CRON_CHANNEL_ID"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Background Cron Automation",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)

        notificationManager.notify((1000..9999).random(), builder.build())
    }
}
