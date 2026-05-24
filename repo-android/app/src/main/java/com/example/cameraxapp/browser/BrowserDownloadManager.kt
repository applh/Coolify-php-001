package com.example.cameraxapp.browser

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val filename: String,
    val totalBytes: Long,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val speedKbSec: Long = 0L,
    val etaSeconds: Long = -1L,
    val isResumeSupported: Boolean = false
)

enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, SUCCEEDED, FAILED
}

class BrowserDownloadManager(private val context: Context) {

    private val _downloadQueue = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val downloadQueue: StateFlow<Map<String, DownloadTask>> = _downloadQueue

    private val pausedTasks = mutableSetOf<String>()

    fun pauseDownload(taskId: String) {
        val task = _downloadQueue.value[taskId] ?: return
        pausedTasks.add(taskId)
        updateTask(task.copy(status = DownloadStatus.PAUSED, speedKbSec = 0))
    }

    suspend fun executeDownload(task: DownloadTask) = withContext(Dispatchers.IO) {
        val downloadDir = File(context.getExternalFilesDir(null), "downloads")
        if (!downloadDir.exists()) downloadDir.mkdirs()
        
        val targetFile = File(downloadDir, task.filename)
        var existingBytes = 0L
        
        // Prepare Connection range offset
        val url = URL(task.url)
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outStream: RandomAccessFile? = null
        
        pausedTasks.remove(task.id)
        updateTask(task.copy(status = DownloadStatus.DOWNLOADING, downloadedBytes = existingBytes))

        try {
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"

            if (task.status == DownloadStatus.PAUSED && targetFile.exists()) {
                existingBytes = targetFile.length()
                connection.setRequestProperty("Range", "bytes=$existingBytes-")
            }

            connection.connect()
            val responseCode = connection.responseCode
            
            val isPartialContent = responseCode == HttpURLConnection.HTTP_PARTIAL
            if (responseCode !in 200..299 && !isPartialContent) {
                if (responseCode == 416) { // Range not satisfiable, file probably completed or altered
                    updateTask(task.copy(status = DownloadStatus.SUCCEEDED, progress = 1.0f, downloadedBytes = task.totalBytes))
                    return@withContext
                }
                throw Exception("HTTP Protocol Error Status: $responseCode")
            }

            val responseSize = connection.contentLength.toLong()
            val finalTotalBytes = if (isPartialContent) {
                existingBytes + responseSize
            } else {
                if (responseSize > 0) responseSize else task.totalBytes
            }

            inputStream = connection.inputStream
            outStream = RandomAccessFile(targetFile, "rw")
            
            if (isPartialContent) {
                outStream.seek(existingBytes)
            } else {
                outStream.setLength(0) 
                existingBytes = 0L
            }

            val buffer = ByteArray(8192) // Keep allocation static (8KB buffer) to prevent OOM
            var bytesRead: Int
            var totalWritten = existingBytes
            val startTime = SystemClock.elapsedRealtime()
            var lastUIUpdateTime = SystemClock.elapsedRealtime()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (pausedTasks.contains(task.id)) {
                    Log.d("BrowserDownload", "Download paused: ${task.filename}")
                    return@withContext
                }

                outStream.write(buffer, 0, bytesRead)
                totalWritten += bytesRead

                val now = SystemClock.elapsedRealtime()
                if (now - lastUIUpdateTime > 500) { // Keep UI dispatch lightweight
                    val elapsedSec = (now - startTime) / 1000.0
                    val bytesTransferredThisRun = totalWritten - existingBytes
                    val speed_B_s = if (elapsedSec > 0) bytesTransferredThisRun / elapsedSec else 0.0
                    val speedKb = (speed_B_s / 1024).toLong()
                    
                    val percent = if (finalTotalBytes > 0) totalWritten.toFloat() / finalTotalBytes else 0f
                    val eta = if (speed_B_s > 0 && finalTotalBytes > 0) {
                        ((finalTotalBytes - totalWritten) / speed_B_s).toLong()
                    } else {
                        -1L
                    }

                    _downloadQueue.value = _downloadQueue.value.toMutableMap().apply {
                        put(task.id, task.copy(
                            downloadedBytes = totalWritten,
                            totalBytes = finalTotalBytes,
                            status = DownloadStatus.DOWNLOADING,
                            progress = percent.coerceIn(0f, 1f),
                            speedKbSec = speedKb,
                            etaSeconds = eta,
                            isResumeSupported = isPartialContent || connection?.getHeaderField("Accept-Ranges") == "bytes"
                        ))
                    }
                    lastUIUpdateTime = now
                }
            }

            _downloadQueue.value = _downloadQueue.value.toMutableMap().apply {
                put(task.id, task.copy(
                    downloadedBytes = finalTotalBytes,
                    status = DownloadStatus.SUCCEEDED,
                    progress = 1.0f,
                    speedKbSec = 0,
                    etaSeconds = 0
                ))
            }
            Log.d("BrowserDownload", "Download Succeeded: ${targetFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e("BrowserDownload", "Download exception: ${e.message}", e)
            val currentTask = _downloadQueue.value[task.id]
            if (currentTask?.status != DownloadStatus.PAUSED) {
                updateTask(task.copy(status = DownloadStatus.FAILED, speedKbSec = 0))
            }
        } finally {
            try {
                inputStream?.close()
                outStream?.close()
                connection?.disconnect()
            } catch (e: Exception) {
                Log.e("BrowserDownload", "Cleanup exception", e)
            }
        }
    }

    private fun updateTask(updatedTask: DownloadTask) {
        _downloadQueue.value = _downloadQueue.value.toMutableMap().apply {
            put(updatedTask.id, updatedTask)
        }
    }

    /**
     * Gets public sharing intent for the successfully saved asset.
     */
    fun getShareUri(filename: String): Uri? {
        val downloadDir = File(context.getExternalFilesDir(null), "downloads")
        val file = File(downloadDir, filename)
        if (!file.exists()) return null

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }
}
