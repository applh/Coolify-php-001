package com.example.cameraxapp.cronjob

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object FileDownloader {

    /**
     * Streams content from [urlString] directly into a local file inside the downloads folder.
     * Prevents Android OOM crashes using a fixed 4KB transfer buffer.
     */
    fun downloadFile(context: Context, urlString: String, outputFileName: String): File? {
        Log.d("FileDownloader", "Starting network request to: $urlString")
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.e("FileDownloader", "Server replied with code $responseCode")
                return null
            }

            // Target folder: Context.getExternalFilesDir(null)/downloads/
            val downloadDir = File(context.getExternalFilesDir(null), "downloads")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val targetFile = File(downloadDir, outputFileName)
            
            inputStream = connection.inputStream
            outputStream = FileOutputStream(targetFile)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            Log.d("FileDownloader", "Successfully downloaded file to: ${targetFile.absolutePath}")
            targetFile
        } catch (e: Exception) {
            Log.e("FileDownloader", "Error during background file download", e)
            null
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
                connection?.disconnect()
            } catch (e: Exception) {
                Log.e("FileDownloader", "Cleanup failed", e)
            }
        }
    }
}
