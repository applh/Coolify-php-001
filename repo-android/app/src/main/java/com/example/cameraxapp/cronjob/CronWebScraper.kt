package com.example.cameraxapp.cronjob

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object CronWebScraper {

    /**
     * Fetch raw HTML, run high-performance tag stripping matches, and alert on matches.
     */
    fun performScrapeJob(
        context: Context,
        url: String,
        boundarySelector: String?,
        matchPattern: String?,
        jobId: String
    ): Boolean {
        Log.d("CronWebScraper", "Issuing lightweight web scrape on: $url")
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null

        return try {
            val endpoint = URL(url)
            connection = endpoint.openConnection() as HttpURLConnection
            connection.connectTimeout = 20000
            connection.readTimeout = 20000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android Crawler Autopilot - Lightweight Regex Engine)")
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.e("CronWebScraper", "Crawl target rejected connection: $responseCode")
                return false
            }

            inputStream = connection.inputStream
            val textBuilder = StringBuilder()
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                textBuilder.append(String(buffer, 0, bytesRead, StandardCharsets.UTF_8))
            }

            val rawHtml = textBuilder.toString()

            // Isolate targeted boundaries if configured
            val targetedSection = if (!boundarySelector.isNullOrBlank()) {
                val idx = rawHtml.indexOf(boundarySelector)
                if (idx != -1) {
                    val end = (idx + 3000).coerceAtMost(rawHtml.length)
                    rawHtml.substring(idx, end)
                } else {
                    rawHtml
                }
            } else {
                rawHtml
            }

            // Stripping markup tags using lightweight patterns
            val cleanPlainText = stripHtmlTags(targetedSection)

            var matchFound = false
            var matchingValue = ""

            if (!matchPattern.isNullOrBlank()) {
                val compiledPattern = Pattern.compile(matchPattern, Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
                val matcher = compiledPattern.matcher(cleanPlainText)
                if (matcher.find()) {
                    matchFound = true
                    matchingValue = matcher.group(0)?.trim() ?: "Matched pattern regex"
                }
            } else {
                // Change detection mode
                val currentHash = cleanPlainText.hashCode()
                matchFound = checkContentModified(context, jobId, currentHash)
                matchingValue = "Web content modification was detected."
            }

            if (matchFound) {
                dispatchTriggerNotification(context, url, matchingValue)
            }

            true
        } catch (e: Exception) {
            Log.e("CronWebScraper", "Crawl execution fault", e)
            false
        } finally {
            try {
                inputStream?.close()
                connection?.disconnect()
            } catch (ex: Exception) {
                Log.e("CronWebScraper", "Cleanup exception", ex)
            }
        }
    }

    private fun stripHtmlTags(html: String): String {
        var cleanText = html.replace("<script[^>]*?>.*?</script>".toRegex(RegexOption.IGNORE_CASE), "")
        cleanText = cleanText.replace("<style[^>]*?>.*?</style>".toRegex(RegexOption.IGNORE_CASE), "")
        cleanText = cleanText.replace("<[^>]*>".toRegex(), " ")
        cleanText = cleanText.replace("&nbsp;".toRegex(), " ")
        cleanText = cleanText.replace("\\s+".toRegex(), " ")
        return cleanText.trim()
    }

    private fun checkContentModified(context: Context, jobId: String, currentHash: Int): Boolean {
        val prefs = context.getSharedPreferences("AutopilotScrapeHash", Context.MODE_PRIVATE)
        val hashKey = "hash_$jobId"
        val lastSavedHash = prefs.getInt(hashKey, -1)

        if (lastSavedHash == -1) {
            prefs.edit().putInt(hashKey, currentHash).apply()
            return false
        }

        if (lastSavedHash != currentHash) {
            prefs.edit().putInt(hashKey, currentHash).apply()
            return true
        }

        return false
    }

    private fun dispatchTriggerNotification(context: Context, url: String, matchDetails: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "CRON_SCRAPE_CHANNEL"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Web Automation Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Automation Scraper Match")
            .setContentText(matchDetails)
            .setSubText(url)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(url.hashCode(), notification)
    }
}
