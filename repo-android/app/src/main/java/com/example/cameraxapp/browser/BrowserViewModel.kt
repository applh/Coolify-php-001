package com.example.cameraxapp.browser

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cameraxapp.ContentPartList
import com.example.cameraxapp.GenerationConfig
import com.example.cameraxapp.GeminiRequest
import com.example.cameraxapp.Part
import com.example.cameraxapp.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowserViewModel(
    private val dbHelper: BrowserDatabaseHelper,
    val downloadsManager: BrowserDownloadManager
) : ViewModel() {

    private val _bookmarks = MutableStateFlow<List<BookmarkInfo>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkInfo>> = _bookmarks

    private val _history = MutableStateFlow<List<HistoryInfo>>(emptyList())
    val history: StateFlow<List<HistoryInfo>> = _history

    private val _scripts = MutableStateFlow<List<UserscriptInfo>>(emptyList())
    val scripts: StateFlow<List<UserscriptInfo>> = _scripts

    private val _scriptGenerationStatus = MutableStateFlow<String?>(null)
    val scriptGenerationStatus: StateFlow<String?> = _scriptGenerationStatus

    val downloadQueue = downloadsManager.downloadQueue

    init {
        loadLocalStorageData()
    }

    fun loadLocalStorageData() {
        viewModelScope.launch(Dispatchers.IO) {
            _bookmarks.value = dbHelper.getAllBookmarks()
            _history.value = dbHelper.getAllHistory()
            _scripts.value = dbHelper.getAllUserscripts()
        }
    }

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.addBookmark(title, url)
            _bookmarks.value = dbHelper.getAllBookmarks()
        }
    }

    fun deleteBookmark(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteBookmark(id)
            _bookmarks.value = dbHelper.getAllBookmarks()
        }
    }

    fun recordVisit(title: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.recordVisit(title, url)
            _history.value = dbHelper.getAllHistory()
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.clearHistory()
            _history.value = emptyList()
        }
    }

    fun deleteUserscript(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteUserscript(id)
            _scripts.value = dbHelper.getAllUserscripts()
        }
    }

    fun toggleUserscript(id: Int, active: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.toggleUserscript(id, active)
            _scripts.value = dbHelper.getAllUserscripts()
        }
    }

    fun getScriptsForUrl(url: String, callback: (List<UserscriptInfo>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val scriptsToRun = dbHelper.getScriptsForUrl(url)
            withContext(Dispatchers.Main) {
                callback(scriptsToRun)
            }
        }
    }

    fun triggerFileDownload(url: String, filename: String, length: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = DownloadTask(
                url = url,
                filename = filename,
                totalBytes = length
            )
            downloadsManager.executeDownload(task)
        }
    }

    fun compileCustomUserscript(userDemandPrompt: String, targetDomainPattern: String, apiKey: String) {
        if (apiKey.isBlank()) {
            _scriptGenerationStatus.value = "Error: Gemini API Key is missing. Configure it in Settings."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _scriptGenerationStatus.value = "Compiling visual script..."

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

            val request = GeminiRequest(
                contents = listOf(
                    ContentPartList(
                        role = "user",
                        parts = listOf(Part(text = systemGuidelines))
                    )
                ),
                generationConfig = GenerationConfig()
            )

            try {
                // Call Google Gemini API utilizing the unified RetrofitClient
                val response = RetrofitClient.geminiApi.generateContent(
                    model = "gemini-2.5-flash",
                    apiKey = apiKey,
                    request = request
                )

                val generatedCode = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (generatedCode.isNullOrBlank()) {
                    _scriptGenerationStatus.value = "Error: Compiled return empty payload."
                } else {
                    // Extract script safely from any residual GPT/Gemini formatting or fences
                    val cleanScript = generatedCode
                        .replace("```javascript", "")
                        .replace("```js", "")
                        .replace("```", "")
                        .trim()

                    dbHelper.saveUserscript(
                        name = "AutoScript ${System.currentTimeMillis()}",
                        regex = targetDomainPattern,
                        content = cleanScript
                    )
                    _scriptGenerationStatus.value = "Script compiled successfully and saved!"
                    _scripts.value = dbHelper.getAllUserscripts()
                }
            } catch (e: Exception) {
                Log.e("BrowserViewModel", "Gemini Script compilation failure", e)
                _scriptGenerationStatus.value = "Compilation Failure: ${e.message}"
            }
        }
    }
}
