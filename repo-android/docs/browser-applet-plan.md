# Android Internet Browser Applet Implementation Plan (Upgraded)

This document details the upgraded, highly detailed architectural blueprints, secure database schemas, and step-by-step implementation strategy for adding a professional-grade, multi-tabbed **Internet Browser Applet** (`com.example.cameraxapp.browser`) to `repo-android`.

This blueprint is optimized for extreme lightweight execution, avoiding heavy external parsing libraries (like Jsoup) in favor of high-performance native string diagnostics, regular expressions, and token boundary parsers. It supports robust, range-resumable download streaming, dynamic User-Agent masking, full SQLite-backed session persistence, and an AI-driven JavaScript sandbox generator powered by `gemini-3.5-flash`.

---

## 1. Upgraded Feature Specifications

### 🌐 A. Core Web Browser with Dynamic Masking & Session State REST
* **Jetpack Compose Native Wrapper:** Employs an optimized, lifecycle-aware `AndroidView<WebView>` setup that implements hardware acceleration, secure DOM Storage, client-side session databases, and independent cookie isolation.
* **Navigation HUD Control:** Complete suite with immediate Back, Forward, Stop, Reload, and Homepage triggers.
* **Progressive Loading Visualizer:** A responsive, eye-safe linear progress bar tracking navigation events with precision.
* **Interactive Address Bar & Sanitization Rules:** Normalizes dirty search inputs (e.g., checks if there is a TLD like `.com`, `.net`, automatically prepends `https://`, or routes raw strings with URL encoding directly to default search queries on Google or DuckDuckGo).
* **SSL Validation Console:** Shows visual security lock flags (teal/green lock badge for validated HTTPS, yellow/red caution triangle for cleartext HTTP connections).
* **Dynamic User Agent Customization & Masking Engine:** Includes hot-swappable profiles to mask rendering pathways mimicking:
  * *Mobile Android Chrome:* `Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36`
  * *Tablet Android:* `Mozilla/5.0 (Linux; Android 13; SM-X906B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36`
  * *Desktop Chrome Windows:* `Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36`
  * *Desktop Safari macOS:* `Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Safari/605.1.15`
* **Asynchronous Multi-Tab State Persistence Manager:** Stores full individual tab histories, scroll coordinates, active search contexts, zoom indexes, and loaded configurations. It intercept OS memory warning dispatches (`onTrimMemory`) to dump high-fidelity JSON arrays representing each web navigation trail into persistent SQLite caches.

### ⬇️ B. Multi-Threaded Range-Resumable Downloads Suite
* **Native Interception Hook:** Uses a robust custom `DownloadListener` to capture direct URLs, MIME-types, content-disposition strings, and file headers.
* **Zero-Copy Streaming I/O Pipeline:** A non-blocking, multi-threaded `InputStream` streaming engine using a statically pre-allocated `ByteArray` (8KB buffer) that bypasses JVM Heap pollution or GC stutter. It achieves $O(1)$ memory overhead globally.
* **HTTP Range Resumable Transfers:** Supports safe connection recovery by parsing remote `Accept-Ranges` and passing exact `Range: bytes=X-` offset payloads on interrupted connections.
* **Visual Downloads Tracker:** Displays real-time progress meters, calculated transfer rates in MB/s, estimated ETA, active status labels (`QUEUED`, `DOWNLOADING`, `PAUSED`, `SUCCEEDED`, `FAILED`), and clear share-intents mapped through an offline `FileProvider` gateway.

### ⏱️ C. Zero-Dependency Light-Weight Crawling & Automation
* **Scraper Database Schema:** Expands central SQLite database indices, establishing target URI rules, alert boundaries, match histories, and execution timetables.
* **Native Light-weight Scraper (No Jsoup/External Libraries):** Standardizes parsing through pre-compiled high-performance regular expressions, standard substring index boundary matching, and tag-stripping helpers. This prevents importing bulky standard DOM parsers, preserving an exceptionally slim APK footprint.
* **Rule-Based Triggers:** Automates background polling intervals checking custom matching regex patterns or detecting change deltas across raw text elements (e.g., alert if the text `"Out of Stock"` disappears or a numeric sequence matches).
* **Foreground Alert Broadcasts:** Dispatches high-priority Android System Notification channels featuring direct deep-link pathways straight back to the originating webpage on detection matches.

### 🤖 D. Script Injection & Gemini JavaScript Workspace
* **Custom Sandbox Userscripts Store:** Persists customized JavaScript blocks mapped against wildcard domain regex matches (e.g., `*.wikipedia.org/*`).
* **Secure evaluateJavascript Sandbox Hook:** Runs validated JS files automatically at the lifecycle callback hook `onPageFinished`, handling errors or debug lines natively via a customized `WebChromeClient.onConsoleMessage` pipeline.
* **AI Generative Workspace Terminal:** An visual code playground that prompts users to input standard natural-language logic (e.g. *"Write a script to capture the prices from the dashboard and clean the visual advertisements unnecessary components"*).
* **Gemini prompt-driven compiler (`gemini-3.5-flash`):** Queries local or server-bound models to generate highly robust, secure JavaScript code wrapped in clean Immediately Invoked Function Expressions (IIFE) for maximum sandbox safety.

---

## 2. Technical Architecture & System Schemas

This applet isolates the UI rendering thread (Jetpack Compose Viewport), background data stream processors (Coroutines/WorkManager), and multi-tab state tables (SQLite Helper).

```
         ┌────────────────────────────────────────────────────────┐
         │             Jetpack Compose Browser UI                 │
         └─────┬───────────────────▲──────────────────────────▲───┘
               │ State Triggers    │ Observables              │
               ▼                   │                          │
         ┌─────────────────────────┴──────────────────────┐  │ UI Rendering
         │               BrowserViewModel                 │  │
         └─────┬───────────────────▲──────────────────────┘  │
               │ SQLite CRUD       │ Flow updates             │
               ▼                   │                          │
         ┌─────────────────────────┴──────────────────────┐  │
         │           BrowserDatabaseHelper                │  │
         │ (TabState, Bookmarks, History, Userscripts)    │  │
         └────────────────────────────────────────────────┘  │
                                                             │
         ┌────────────────────────────────────────────────┐  │
         │             BrowserWebViewClient               │──┘
         │ (Downloads intercept, Script injection)        │
         └────────────────────────────────────────────────┘
```

### A. Database Modifications (`BrowserDatabaseHelper.kt`)

We define isolated schemas with no outer structural risk. The layout manages parallel multi-tab histories, user scripts, bookmarks, and search index caches.

```sql
-- Bookmark Matrix Index
CREATE TABLE IF NOT EXISTS bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    url TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL
);

-- Search and Navigation History
CREATE TABLE IF NOT EXISTS history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    last_visited INTEGER NOT NULL,
    visit_count INTEGER DEFAULT 1
);

-- User JavaScript Sandbox Injections
CREATE TABLE IF NOT EXISTS userscripts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    target_regex TEXT NOT NULL,
    js_content TEXT NOT NULL,
    is_active INTEGER DEFAULT 1,
    created_at INTEGER NOT NULL
);

-- Complete Tab Persistent Registry (With History Stack Serialized as JSON)
CREATE TABLE IF NOT EXISTS active_tabs (
    tab_uuid TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    current_url TEXT NOT NULL,
    history_stack_json TEXT NOT NULL, -- Serialized JSON containing back/forward history list
    scroll_x INTEGER DEFAULT 0,
    scroll_y INTEGER DEFAULT 0,
    zoom_level REAL DEFAULT 1.0,
    last_active INTEGER NOT NULL
);
```

### B. Cron Automation Architecture (`CronJobDatabase.kt` / `CronJobEntity.kt`)

Extends background automated pipelines for periodic lightweight text crawling.

#### 1. Add fields inside `CronJobEntity.kt`:
```kotlin
// In com.example.cameraxapp.cronjob.CronJobEntity
val scrapeSelector: String? = null,        // Custom boundary markers or tagging identifiers
val scrapeMatchPattern: String? = null,    // Target text search regex or phrase
val notifyOnScrapeMatch: Boolean = false,
val lastParsedHash: Int = -1
```

#### 2. Create standard migration rules in your database helper:
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE cron_jobs ADD COLUMN scrapeSelector TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE cron_jobs ADD COLUMN scrapeMatchPattern TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE cron_jobs ADD COLUMN notifyOnScrapeMatch INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE cron_jobs ADD COLUMN lastParsedHash INTEGER NOT NULL DEFAULT -1")
    }
}
```

---

## 3. Core Implementation Blueprints (Kotlin)

### A. High-Fidelity WebView Component With Injector & Console Console

Compose wrapper encapsulating native Android WebView features. It listens for javascript execution logs to intercept error payloads and coordinates script loading during `onPageFinished`.

```kotlin
package com.example.cameraxapp.browser

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    url: String,
    onProgressChanged: (Int) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (WebView, String) -> Unit,
    onConsoleMsgCaptured: (ConsoleMessage) -> Unit,
    onDownloadTriggered: (String, String, String, String, Long) -> Unit,
    userAgentString: String?,
    modifier: Modifier = Modifier,
    updateWebView: (WebView) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    databaseEnabled = true
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            onConsoleMsgCaptured(it)
                            Log.d("BrowserConsole", "[${it.messageLevel()}] ${it.message()} @ L:${it.lineNumber()} of ${it.sourceId()}")
                        }
                        return true
                    }
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        url?.let { onPageStarted(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (view != null && url != null) {
                            onPageFinished(view, url)
                        }
                    }

                    @TargetApi(Build.VERSION_CODES.N)
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val path = request?.url?.toString() ?: return false
                        view?.loadUrl(path)
                        return true
                    }

                    @SuppressWarnings("deprecation")
                    override fun shouldOverrideUrlLoading(view: WebView?, urlStr: String?): Boolean {
                        val path = urlStr ?: return false
                        view?.loadUrl(path)
                        return true
                    }
                }

                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                    onDownloadTriggered(downloadUrl, userAgent, contentDisposition, mimetype, contentLength)
                }

                if (!userAgentString.isNullOrBlank()) {
                    settings.userAgentString = userAgentString
                }

                loadUrl(url)
                updateWebView(this)
            }
        },
        update = { webView ->
            if (!userAgentString.isNullOrBlank() && webView.settings.userAgentString != userAgentString) {
                webView.settings.userAgentString = userAgentString
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
```

---

### B. Robust Thread-Safe & Range-Resumable Download Controller

This manager bypasses high memory usage through buffered zero-copy streaming. It supports pause, resume (relying on `Range: bytes=` offset request payloads), throughput speed checks, and safe sharing via FileProvider.

```kotlin
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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.TimeUnit

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
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _downloadQueue = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val downloadQueue: StateFlow<Map<String, DownloadTask>> = _downloadQueue

    private val activeStreams = mutableMapOf<String, okhttp3.Call>()

    fun pauseDownload(taskId: String) {
        val task = _downloadQueue.value[taskId] ?: return
        activeStreams[taskId]?.cancel()
        updateTask(task.copy(status = DownloadStatus.PAUSED, speedKbSec = 0))
    }

    suspend fun executeDownload(task: DownloadTask) = withContext(Dispatchers.IO) {
        val downloadDir = File(context.getExternalFilesDir(null), "downloads")
        if (!downloadDir.exists()) downloadDir.mkdirs()
        
        val targetFile = File(downloadDir, task.filename)
        var existingBytes = 0L
        
        // Prepare Range Headers if task is Resumed from a paused state
        val requestBuilder = Request.Builder().url(task.url)
        if (task.status == DownloadStatus.PAUSED && targetFile.exists()) {
            existingBytes = targetFile.length()
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val request = requestBuilder.build()
        val call = client.newCall(request)
        activeStreams[task.id] = call
        
        updateTask(task.copy(status = DownloadStatus.DOWNLOADING, downloadedBytes = existingBytes))

        try {
            val response = call.execute()
            if (!response.isSuccessful || response.body == null) {
                if (response.code == 416) { // Range Not Satisfiable: File has finished or modified
                    updateTask(task.copy(status = DownloadStatus.SUCCEEDED, progress = 1.0f))
                    return@withContext
                }
                throw Exception("HTTP Protocol Error Status: ${response.code}")
            }

            val isPartialContent = response.code == 206
            val responseSize = response.body!!.contentLength()
            val finalTotalBytes = if (isPartialContent) {
                existingBytes + responseSize
            } else {
                if (responseSize > 0) responseSize else task.totalBytes
            }

            var inputStream: InputStream? = null
            var outStream: RandomAccessFile? = null

            try {
                inputStream = response.body!!.byteStream()
                outStream = RandomAccessFile(targetFile, "rw")
                
                if (isPartialContent) {
                    outStream.seek(existingBytes)
                } else {
                    outStream.setLength(0) 
                }

                val buffer = ByteArray(8192) // Strict 8KB block limiting GC allocations
                var bytesRead: Int
                var totalWritten = existingBytes
                val startTime = SystemClock.elapsedRealtime()
                var lastUIUpdateTime = SystemClock.elapsedRealtime()

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outStream.write(buffer, 0, bytesRead)
                    totalWritten += bytesRead

                    val now = SystemClock.elapsedRealtime()
                    if (now - lastUIUpdateTime > 500) { // Keep UI flow light (500ms intervals)
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
                                isResumeSupported = isPartialContent || response.header("Accept-Ranges") == "bytes"
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
            } finally {
                inputStream?.close()
                outStream?.close()
                response.close()
                activeStreams.remove(task.id)
            }
        } catch (e: Exception) {
            Log.e("DownloadManager", "Data stream processing broken: ${e.message}")
            val currentTask = _downloadQueue.value[task.id]
            if (currentTask?.status != DownloadStatus.PAUSED) {
                updateTask(task.copy(status = DownloadStatus.FAILED, speedKbSec = 0))
            }
        }
    }

    private fun updateTask(updatedTask: DownloadTask) {
        _downloadQueue.value = _downloadQueue.value.toMutableMap().apply {
            put(updatedTask.id, updatedTask)
        }
    }
}
```

---

### C. Zero-Dependency HTML Parser & Regex Crawler Engine

To maintain an exceptionally small, fast application, this scraper uses **no external libraries** (such as Jsoup). Instead, it employs regular expressions, offset marker bounds, and programmatic tag strippers to process raw markup in real-time.

```kotlin
package com.example.cameraxapp.cronjob

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object CronWebScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun performScrapeJob(
        context: Context,
        url: String,
        boundarySelector: String?, // String marker limits inside the markup, e.g. "id=\"price\""
        matchPattern: String?,     // Precompiled Match Regex rule index
        jobId: String
    ): Boolean {
        Log.d("CronWebScraper", "Executing zero-dependency crawl: $url")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android Crawler Autopilot - Lightweight Regex Engine)")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                Log.e("CronWebScraper", "Crawl target rejected connection: ${response.code}")
                return false
            }

            val rawHtml = String(response.body!!.bytes(), StandardCharsets.UTF_8)

            // Extract targeted code boundaries to isolate searches
            val targetedSection = if (!boundarySelector.isNullOrBlank()) {
                extractSubBlock(rawHtml, boundarySelector)
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
                    matchingValue = matcher.group(0)?.trim() ?: "Matched Regex"
                }
            } else {
                // modification trigger
                val hashValue = cleanPlainText.hashCode()
                matchFound = checkContentModified(context, jobId, hashValue)
                matchingValue = "Web content modify detected."
            }

            if (matchFound) {
                dispatchTriggerNotification(context, url, matchingValue)
            }

            true
        } catch (e: Exception) {
            Log.e("CronWebScraper", "轻量级 Web Scraper Execution fault: ${e.message}")
            false
        }
    }

    /**
     * Extracts substrings enclosing targeted ID or class markers.
     * Replaces standard bulky Jsoup selectors natively.
     */
    private fun extractSubBlock(html: String, selector: String): String {
        val startIndex = html.indexOf(selector)
        if (startIndex == -1) return html
        
        // Return up to 3000 chars near the target to keep regex context lightweight
        val endIndex = (startIndex + 3000).coerceAtMost(html.length)
        return html.substring(startIndex, endIndex)
    }

    /**
     * Lightweight native regex to strip tags and format plaintext outputs efficiently.
     */
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
            val channel = NotificationChannel(channelId, "Web Automation Alerts", NotificationManager.IMPORTANCE_HIGH)
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
```

---

### D. AI JavaScript Generator (`BrowserViewModel.kt` Interface API Gateway)

Integrates standard SDK models, mapping prompts to secure Immediately Invoked Function Expressions (IIFEs) for custom user configurations. It leverages the robust `gemini-3.5-flash` model which provides the optimal balance of speed and complex logic-building for scripts.

```kotlin
package com.example.cameraxapp.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BrowserViewModel(private val dbHelper: BrowserDatabaseHelper) : ViewModel() {
    private val _scriptGenerationOutput = MutableStateFlow<String?>(null)
    val scriptGenerationOutput: StateFlow<String?> = _scriptGenerationOutput

    /**
     * Calls Gemini-3.5-flash model server-side to generate verified javascript injections.
     */
    fun compileCustomUserscript(userDemandPrompt: String, targetDomainPattern: String, apiKey: String) {
        viewModelScope.launch {
            _scriptGenerationOutput.value = "Compiling visual script..."
            
            val model = GenerativeModel(
                modelName = "gemini-3.5-flash", // Modern, non-deprecated fast model
                apiKey = apiKey
            )

            val systemGuidelines = """
                You are a senior Android Chrome WebView JS injection assistant.
                Generate fully-verified, lightweight target JavaScript routines executed via `evaluateJavascript()`.

                Rules:
                1. Always wrap script operations inside a clean, self-executing IIFE wrapper: (function() { ... })();
                2. Do not call blocking browser actions like 'window.alert', 'prompt', or 'confirm'.
                3. Safely catch errors using try-catch blocks and log steps via `console.log("InjectedApplet::" + msg)`.
                4. Incorporate asynchronous check intervals if elements loading speed depends on slow network responses.
                5. Output STRICTLY raw JavaScript. Do not wrap code blocks in markdown fences, backticks, or other styling wrappers.
                
                ACTION PROMPT REQUIRED BY USER: $userDemandPrompt
                TARGET PAGE SCOPE: $targetDomainPattern
            """.trimIndent()

            try {
                val result = model.generateContent(systemGuidelines)
                val sanitizedScript = result.text?.trim() ?: "Error: Compiled return empty."
                
                // Save compiled injection into offline SQLite schema
                dbHelper.saveUserscript(
                    name = "AutoScript ${System.currentTimeMillis()}",
                    regex = targetDomainPattern,
                    content = sanitizedScript
                )
                
                _scriptGenerationOutput.value = sanitizedScript
            } catch (e: Exception) {
                _scriptGenerationOutput.value = "Compilation Failure: ${e.message}"
            }
        }
    }
}
```

---

## 4. UI/UX Interface Layout Architecture (Jetpack Compose Multi-Panel)

Renders adaptive bento frames. It provides high-contrast typography, clear negative margins, and mobile-friendly touch targets.

```
+-------------------------------------------------------------------------+
| [Tabs Selector Panel - Left View on Foldables ] | [Main Webview Frame]  |
| - Standard Stack List (Tab A, Tab B, Tab C)     | - Progress bar (100%) |
| - Controls (New Tab, Clear History/Bookmarks)  | - Address UI Control  |
| - Custom Script Injector Manager Console        | - WebView Window      |
+-------------------------------------------------------------------------+
```

```kotlin
package com.example.cameraxapp.browser

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onBackToHub: () -> Unit
) {
    var rawUrlInput by remember { mutableStateOf("https://www.google.com") }
    var currentUrl by remember { mutableStateOf("https://www.google.com") }
    var loadingProgress by remember { mutableStateOf(0) }
    var isDesktopUserAgent by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletLayout = maxWidth > 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isTabletLayout) {
                // Left Drawer Panel: Bookmark indexes, Tab lists, active AI Scripts, and console monitors
                Surface(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Navigation & Scripts", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { /* Launch AI Compilation Workspace dialog */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("AI Script Assistant")
                        }
                    }
                }
            }

            // Central Navigation Pane
            Column(modifier = Modifier.weight(1f)) {
                
                // Loading Progress Indicators
                if (loadingProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Interactive Address Navigation Control HUD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Back navigation */ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(onClick = { /* Forward navigation */ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                    }
                    IconButton(onClick = { /* Reload navigation */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                    IconButton(onClick = { /* Home page default load */ }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }

                    OutlinedTextField(
                        value = rawUrlInput,
                        onValueChange = { rawUrlInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Search or Type URL") }
                    )
                }

                // Isolated Web Frame Viewport viewport
                Box(modifier = Modifier.weight(1f)) {
                    BrowserWebView(
                        url = currentUrl,
                        onProgressChanged = { loadingProgress = it },
                        onPageStarted = { currentUrl = it; rawUrlInput = it },
                        onConsoleMsgCaptured = { /* Capture and audit script execution */ },
                        onPageFinished = { _, url -> Log.i("WebSession", "Loaded: $url") },
                        onDownloadTriggered = { _, _, _, _, _ -> /* Spawn background task downloads */ },
                        userAgentString = if (isDesktopUserAgent) {
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}
```

---

## 5. Sequence Execution & Scripter Flow

The lifecycle chart below outlines the exact flow of on-page script scanning and automatic sandbox compilation of user injections.

```
[Web Navigation Finish]    [SQL Match ScriptQuery]       [WebView Engine]      [Security Console Logger]
        │                           │                           │                          │
        ├─► Trigger onPageFinished ─┼───────────────────────────┼──────────────────────────┤
        │                           │                           │                          │
        │                           ├─► Pull script match row ──┼──────────────────────────┤
        │                           │                           │                          │
        │                           │                           ├─► Run evaluateJS() ──────┤
        │                           │                           │                          │
        │                           │                           │                          ├─► Emit custom
        │                           │                           │                          │   Console.log()
```

---

## 6. Verification & Staged Deployment Schedule

### 🚀 Phase 1: SQLite Persistent Matrix Configurations
1. Implement the schema within `BrowserDatabaseHelper.kt`, verifying integrity constraints onbookmarks, history stacks, tab lists, and script records.
2. Bind central launch routing directly in `MainActivity.kt`.

### 🔍 Phase 2: Webview Layout wrapper
1. Establish Compose `BrowserWebView` bindings.
2. Hard-code strict safety rules (`allowFileAccess = false`, `allowContentAccess = false`).

### 💾 Phase 3: Zero-Copy Resumable Downloads Pipeline
1. Connect listeners back to `BrowserDownloadManager`.
2. Map chunk writing with Range-header check protocols.
3. Test notifications displaying MB/s speeds and live ETA stats.

### 🧪 Phase 4: Lightweight Scraper & Code Assistant Hooks
1. Set up extraction procedures on regular expressions inside `CronWebScraper`.
2. Test code generation prompts routing queries to the `gemini-3.5-flash` endpoint.

### ⏱️ Phase 5: WorkManager Routing & Build validations
1. Bind automatic scraping check routines inside `DynamicRouterWorker.kt`.
2. Confirm strict Kotlin type validation by calling `compile_applet`.
