package com.example.cameraxapp

import android.content.Context
import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// --- DATA STRUCTURES ---

data class ChatMessageRecord(
    val role: String, // "user", "model", "error"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latencyMs: Long = 0L,
    val tokenEstimate: Int = 0,
    val costEstimate: Double = 0.0
)

data class SessionRecord(
    val sessionId: String,
    val timestamp: Long,
    val modelName: String,
    val totalCost: Double,
    val totalLatencyMs: Long,
    val queryCount: Int,
    var userRanking: Int, // 1 to 5 stars, 0 means unrated
    var userNotes: String,
    val messages: List<ChatMessageRecord>
)

data class ReplayComparison(
    val originalLatency: Long,
    val replayLatency: Long,
    val originalCost: Double,
    val replayCost: Double,
    val latencyDeltaPercent: Double,
    val costDeltaPercent: Double
)

// --- JSON CONFIG UTILITIES ---

fun sessionToJson(session: SessionRecord): String {
    val obj = JSONObject()
    obj.put("sessionId", session.sessionId)
    obj.put("timestamp", session.timestamp)
    obj.put("modelName", session.modelName)
    obj.put("totalCost", session.totalCost)
    obj.put("totalLatencyMs", session.totalLatencyMs)
    obj.put("queryCount", session.queryCount)
    obj.put("userRanking", session.userRanking)
    obj.put("userNotes", session.userNotes)
    
    val msgsArray = JSONArray()
    for (msg in session.messages) {
        val msgObj = JSONObject()
        msgObj.put("role", msg.role)
        msgObj.put("text", msg.text)
        msgObj.put("timestamp", msg.timestamp)
        msgObj.put("latencyMs", msg.latencyMs)
        msgObj.put("tokenEstimate", msg.tokenEstimate)
        msgObj.put("costEstimate", msg.costEstimate)
        msgsArray.put(msgObj)
    }
    obj.put("messages", msgsArray)
    return obj.toString(2)
}

fun jsonToSession(jsonStr: String): SessionRecord {
    val obj = JSONObject(jsonStr)
    val sessionId = obj.optString("sessionId", "")
    val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
    val modelName = obj.optString("modelName", "gemini-2.5-flash")
    val totalCost = obj.optDouble("totalCost", 0.0)
    val totalLatencyMs = obj.optLong("totalLatencyMs", 0L)
    val queryCount = obj.optInt("queryCount", 0)
    val userRanking = obj.optInt("userRanking", 0)
    val userNotes = obj.optString("userNotes", "")
    
    val msgsArray = obj.optJSONArray("messages") ?: JSONArray()
    val messages = mutableListOf<ChatMessageRecord>()
    for (i in 0 until msgsArray.length()) {
        val msgObj = msgsArray.getJSONObject(i)
        messages.add(
            ChatMessageRecord(
                role = msgObj.optString("role", ""),
                text = msgObj.optString("text", ""),
                timestamp = msgObj.optLong("timestamp", System.currentTimeMillis()),
                latencyMs = msgObj.optLong("latencyMs", 0L),
                tokenEstimate = msgObj.optInt("tokenEstimate", 0),
                costEstimate = msgObj.optDouble("costEstimate", 0.0)
            )
        )
    }
    return SessionRecord(
        sessionId = sessionId,
        timestamp = timestamp,
        modelName = modelName,
        totalCost = totalCost,
        totalLatencyMs = totalLatencyMs,
        queryCount = queryCount,
        userRanking = userRanking,
        userNotes = userNotes,
        messages = messages
    )
}

// --- FILE PATH & FILE OPERATIONS ---

fun loadSessionsFromDisk(context: Context, storageLocation: Int): List<SessionRecord> {
    val list = mutableListOf<SessionRecord>()
    val rootDir = when(storageLocation) {
        1 -> context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        2 -> {
            val dirs = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
            if (dirs.size > 1) dirs[1] else context.filesDir
        }
        else -> context.filesDir
    } ?: return emptyList()

    val files = rootDir.listFiles() ?: return emptyList()
    val sessionFiles = files.filter { it.name.startsWith("SESSION_") && it.name.endsWith(".json") }
        .sortedByDescending { it.lastModified() }

    for (file in sessionFiles) {
        try {
            val jsonStr = file.readText()
            val session = jsonToSession(jsonStr)
            list.add(session)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return list
}

fun saveSessionToDisk(context: Context, storageLocation: Int, session: SessionRecord) {
    val rootDir = when(storageLocation) {
        1 -> context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        2 -> {
            val dirs = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
            if (dirs.size > 1) dirs[1] else context.filesDir
        }
        else -> context.filesDir
    } ?: return

    try {
        val file = File(rootDir, "${session.sessionId}.json")
        val jsonStr = sessionToJson(session)
        file.writeText(jsonStr)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun deleteSessionFromDisk(context: Context, storageLocation: Int, sessionId: String) {
    val rootDir = when(storageLocation) {
        1 -> context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        2 -> {
            val dirs = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
            if (dirs.size > 1) dirs[1] else context.filesDir
        }
        else -> context.filesDir
    } ?: return

    try {
        val file = File(rootDir, "$sessionId.json")
        if (file.exists()) {
            file.delete()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// --- MAIN COMPOSE SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITeamScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val geminiApiKey by repository.geminiApiKey.collectAsState(initial = "")
    val storageLocation by repository.storageLocation.collectAsState(initial = 0)

    // Current Active Tab
    var activeTab by remember { mutableStateOf(0) } // 0: Active Chat, 1: Benchmarks & Replays

    // Current Session State
    var activeSessionId by remember { mutableStateOf("SESSION_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())) }
    var currentSessionMessages by remember { mutableStateOf(listOf<ChatMessageRecord>()) }
    var currentRanking by remember { mutableStateOf(0) }
    var currentNotes by remember { mutableStateOf("") }

    var currentPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf("Text") } // "Text" or "Image"

    // List of All Saved Sessions
    var savedSessions by remember { mutableStateOf(listOf<SessionRecord>()) }
    var statsTrigger by remember { mutableStateOf(0) }
    
    var isSessionInitialized by remember { mutableStateOf(false) }

    // Selected Session for Inspection
    var selectedSession by remember { mutableStateOf<SessionRecord?>(null) }

    // Replay State
    var isReplaying by remember { mutableStateOf(false) }
    var replayStepsLog by remember { mutableStateOf(listOf<String>()) }
    var replayComparisonResult by remember { mutableStateOf<ReplayComparison?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Query disk sessions when storage, focus tab or triggers mutate
    LaunchedEffect(storageLocation, activeTab, statsTrigger) {
        withContext(Dispatchers.IO) {
            val loadedSessions = loadSessionsFromDisk(context, storageLocation)
            savedSessions = loadedSessions
            
            if (!isSessionInitialized) {
                if (loadedSessions.isNotEmpty()) {
                    val lastSession = loadedSessions.first()
                    activeSessionId = lastSession.sessionId
                    currentSessionMessages = lastSession.messages
                    currentRanking = lastSession.userRanking
                    currentNotes = lastSession.userNotes
                }
                isSessionInitialized = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Team Tools") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    if (activeTab == 0) {
                        TextButton(onClick = {
                            activeSessionId = "SESSION_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            currentSessionMessages = emptyList()
                            currentRanking = 0
                            currentNotes = ""
                        }) {
                            Text("New Session", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Tab Header Row
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Active Chat", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Benchmarks", fontWeight = FontWeight.Bold) }
                )
            }

            if (geminiApiKey.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Please set your Gemini API Key in Settings.", color = MaterialTheme.colorScheme.error)
                }
            } else {
                when (activeTab) {
                    0 -> {
                        // --- TAB 0: CHAT INTERFACE ---
                        Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                            
                            // Active Session Live Metadata Banner
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val totalCost = currentSessionMessages.sumOf { it.costEstimate }
                                    val totalLat = currentSessionMessages.sumOf { it.latencyMs }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Active Session: ${activeSessionId.takeLast(13)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Telemetry: ${currentSessionMessages.size} msgs | $${String.format(Locale.US, "%.5f", totalCost)} | ${totalLat}ms",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        // Realtime Star Evaluator
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            for (star in 1..5) {
                                                val starSymbol = if (star <= currentRanking) "★" else "☆"
                                                Text(
                                                    text = starSymbol,
                                                    fontSize = 20.sp,
                                                    color = if (star <= currentRanking) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .clickable {
                                                            currentRanking = star
                                                            val tempNotes = currentNotes
                                                            coroutineScope.launch(Dispatchers.IO) {
                                                                val actRecord = SessionRecord(
                                                                    sessionId = activeSessionId,
                                                                    timestamp = System.currentTimeMillis(),
                                                                    modelName = "gemini-2.5-flash",
                                                                    totalCost = totalCost,
                                                                    totalLatencyMs = totalLat,
                                                                    queryCount = currentSessionMessages.filter { it.role == "model" }.size,
                                                                    userRanking = star,
                                                                    userNotes = tempNotes,
                                                                    messages = currentSessionMessages
                                                                )
                                                                saveSessionToDisk(context, storageLocation, actRecord)
                                                                statsTrigger++
                                                            }
                                                        }
                                                        .padding(horizontal = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Conversation Area
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (currentSessionMessages.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(120.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Start a conversation. Session records are auto-archived with telemetry diagnostics for replay testing.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 280.dp)
                                            )
                                        }
                                    }
                                } else {
                                    items(currentSessionMessages) { message ->
                                        val isUser = message.role == "user"
                                        val isError = message.role == "error"
                                        val color = if (isError) MaterialTheme.colorScheme.errorContainer
                                        else if (isUser) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.secondaryContainer

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                        ) {
                                            Surface(
                                                color = color,
                                                shape = MaterialTheme.shapes.medium
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    SelectionContainer {
                                                        Text(
                                                            text = message.text,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(
                                                        modifier = Modifier.align(Alignment.End),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (!isUser && message.latencyMs > 0) {
                                                            Text(
                                                                text = "⏱️ ${message.latencyMs}ms  ",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                            )
                                                        }
                                                        if (!isUser && message.costEstimate > 0.0) {
                                                            Text(
                                                                text = "💰 $${String.format(Locale.US, "%.5f", message.costEstimate)}  ",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                            )
                                                        }
                                                        Text(
                                                            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp)),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (isGenerating) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                        }
                                    }
                                }
                            }

                            // Output Format Segment Selector
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Output: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    onClick = { if (!isGenerating) selectedFormat = "Text" },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (selectedFormat == "Text") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "Text",
                                        color = if (selectedFormat == "Text") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(
                                    onClick = { if (!isGenerating) selectedFormat = "Image" },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (selectedFormat == "Image") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "Image (Imagen API)",
                                        color = if (selectedFormat == "Image") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Dynamic Input Bar
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = currentPrompt,
                                    onValueChange = { currentPrompt = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text(if (selectedFormat == "Image") "Describe the image to generate..." else "Ask the AI team...") },
                                    enabled = !isGenerating
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (currentPrompt.isNotBlank() && !isGenerating) {
                                            val prompt = currentPrompt
                                            currentPrompt = ""
                                            isGenerating = true
                                            
                                            val userMsg = ChatMessageRecord(
                                                role = "user",
                                                text = prompt,
                                                timestamp = System.currentTimeMillis()
                                            )
                                            currentSessionMessages = currentSessionMessages + userMsg

                                            coroutineScope.launch {
                                                val generativeModel = GenerativeModel(
                                                    modelName = "gemini-2.5-flash",
                                                    apiKey = geminiApiKey
                                                )
                                                
                                                var file: File? = null
                                                withContext(Dispatchers.IO) {
                                                    val rootDir = when(storageLocation) {
                                                        1 -> context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                                                        2 -> {
                                                            val dirs = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
                                                            if (dirs.size > 1) dirs[1] else context.filesDir
                                                        }
                                                        else -> context.filesDir
                                                    }
                                                    
                                                    if (rootDir != null) {
                                                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                                        val prefix = if (prompt.length > 20) prompt.substring(0, 20).replace(Regex("[^a-zA-Z0-9]"), "_") else prompt.replace(Regex("[^a-zA-Z0-9]"), "_")
                                                        val filename = "AI_${timestamp}_${prefix}.md"
                                                        file = File(rootDir, filename)
                                                        file?.writeText("# Prompt: $prompt\n\n")
                                                    }
                                                }
                                                
                                                val startTime = System.currentTimeMillis()
                                                try {
                                                                                               val isImagePrompt = selectedFormat == "Image" || prompt.startsWith("/image ", ignoreCase = true) || prompt.lowercase(Locale.US).startsWith("generate image")
                                                    val responseText = if (isImagePrompt) {
                                                        val imagePrompt = if (prompt.startsWith("/image ", ignoreCase = true)) prompt.substring(7).trim() else prompt
                                                        withContext(Dispatchers.IO) {
                                                            var resultText = ""
                                                            var respStrForDump = ""
                                                            try {
                                                                val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-002:generateImages?key=$geminiApiKey")
                                                                val conn = url.openConnection() as java.net.HttpURLConnection
                                                                conn.requestMethod = "POST"
                                                                conn.setRequestProperty("Content-Type", "application/json")
                                                                conn.doOutput = true
                                                                val escapedPrompt = imagePrompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ")
                                                                val jsonInputString = "{\"prompt\": \"$escapedPrompt\", \"config\": {\"numberOfImages\": 1, \"outputMimeType\": \"image/jpeg\", \"aspectRatio\": \"1:1\"}}"
                                                                
                                                                conn.outputStream.use { os ->
                                                                    val input = jsonInputString.toByteArray(Charsets.UTF_8)
                                                                    os.write(input, 0, input.size)
                                                                }
                                                                
                                                                val respCode = conn.responseCode
                                                                val respStr = if (respCode in 200..299) {
                                                                    conn.inputStream.bufferedReader().use { it.readText() }
                                                                } else {
                                                                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                                                                }
                                                                respStrForDump = respStr
                                                                
                                                                if (respCode in 200..299) {
                                                                    val root = org.json.JSONObject(respStr)
                                                                    val genImages = root.optJSONArray("generatedImages")
                                                                    var b64 = ""
                                                                    if (genImages != null && genImages.length() > 0) {
                                                                        val imgObj = genImages.getJSONObject(0).optJSONObject("image")
                                                                        b64 = imgObj?.optString("imageBytes", "") ?: ""
                                                                    }
                                                                    if (b64.isNotEmpty()) {
                                                                        val imgBytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                                                        val rootDir = when(storageLocation) {
                                                                            1 -> context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                                                                            2 -> {
                                                                                val dirs = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
                                                                                if (dirs.size > 1) dirs[1] else context.filesDir
                                                                            }
                                                                            else -> context.filesDir
                                                                        }
                                                                        if (rootDir != null) {
                                                                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                                                            val safePrefix = if (imagePrompt.length > 20) imagePrompt.substring(0, 20).replace(Regex("[^a-zA-Z0-9]"), "_") else imagePrompt.replace(Regex("[^a-zA-Z0-9]"), "_")
                                                                            val imgFile = File(rootDir, "AI_${timestamp}_${safePrefix}.jpg")
                                                                            imgFile.writeBytes(imgBytes)
                                                                            resultText = "✨ Image generated via Imagen and saved to: ${imgFile.absolutePath}"
                                                                        } else {
                                                                            resultText = "Error: Could not determine storage location for image.\n\n[RAW RESPONSE DUMP]:\n$respStr"
                                                                        }
                                                                    } else {
                                                                        resultText = "Error: Something went wrong while trying to deserialize a response from the server (Success HTTP 200 but image data was empty).\n\n[RAW RESPONSE DUMP]:\n$respStr"
                                                                    }
                                                                } else {
                                                                    resultText = "Error: Something went wrong while trying to deserialize a response from the server (HTTP Error $respCode).\n\n[RAW RESPONSE DUMP]:\n$respStr"
                                                                }
                                                            } catch (ex: Exception) {
                                                                val partialDump = if (respStrForDump.isNotEmpty()) "\n\n[RAW RESPONSE DUMP]:\n$respStrForDump" else ""
                                                                resultText = "Exception during image generation: ${ex.message}$partialDump\n\n[STACKTRACE]:\n${ex.stackTraceToString()}"
                                                            }
                                                            resultText
                                                        }
                                                    } else {
                                                        try {
                                                            val response = withContext(Dispatchers.IO) {
                                                                generativeModel.generateContent(prompt)
                                                            }
                                                            response.text ?: "Empty response"
                                                        } catch (sdkEx: Exception) {
                                                            val msg = sdkEx.message ?: ""
                                                            if (msg.contains("deserialize") || msg.contains("SerializationException")) {
                                                                "Error in SDK Content Generation: ${sdkEx.message}\n\n[SDK DESERIALIZATION DIAGNOSTICS]: This typically indicates that the Gemini API returned a HTTP error response (like 400 Bad Request or 404 Model Not Found) which the Android SDK failed to deserialize due to a known deserializer limitation. Check that your API key is correct and valid.\n\nStacktrace:\n${sdkEx.stackTraceToString()}"
                                                            } else {
                                                                "Error in SDK Content Generation: ${sdkEx.message}\n\nStacktrace:\n${sdkEx.stackTraceToString()}"
                                                            }
                                                        }
                                                    }
                                                    val endTime = System.currentTimeMillis()
                                                    val latency = endTime - startTime
                                                    
                                                    // Estimations
                                                    val inputTokens = Math.ceil(prompt.length / 4.0).toInt().coerceAtLeast(1)
                                                    val outputTokens = Math.ceil(responseText.length / 4.0).toInt().coerceAtLeast(1)
                                                    val inputCost = inputTokens * 0.000000075
                                                    val outputCost = outputTokens * 0.000000300
                                                    val stepCost = inputCost + outputCost
                                                    
                                                    val modelMsg = ChatMessageRecord(
                                                        role = "model",
                                                        text = responseText,
                                                        timestamp = endTime,
                                                        latencyMs = latency,
                                                        tokenEstimate = inputTokens + outputTokens,
                                                        costEstimate = stepCost
                                                    )
                                                    
                                                    currentSessionMessages = currentSessionMessages + modelMsg
                                                    
                                                    withContext(Dispatchers.IO) {
                                                        file?.appendText(responseText)
                                                    }
                                                    
                                                    withContext(Dispatchers.IO) {
                                                        val updatedCost = currentSessionMessages.sumOf { it.costEstimate }
                                                        val updatedLat = currentSessionMessages.sumOf { it.latencyMs }
                                                        val qCount = currentSessionMessages.filter { it.role == "model" }.size
                                                        
                                                        val sessionRecord = SessionRecord(
                                                            sessionId = activeSessionId,
                                                            timestamp = System.currentTimeMillis(),
                                                            modelName = "gemini-2.5-flash",
                                                            totalCost = updatedCost,
                                                            totalLatencyMs = updatedLat,
                                                            queryCount = qCount,
                                                            userRanking = currentRanking,
                                                            userNotes = currentNotes,
                                                            messages = currentSessionMessages
                                                        )
                                                        saveSessionToDisk(context, storageLocation, sessionRecord)
                                                        statsTrigger++
                                                    }
                                                    
                                                } catch (e: Exception) {
                                                    val endTime = System.currentTimeMillis()
                                                    val latency = endTime - startTime
                                                    
                                                    val errMsg = ChatMessageRecord(
                                                        role = "error",
                                                        text = "Error: ${e.message}",
                                                        timestamp = endTime,
                                                        latencyMs = latency
                                                    )
                                                    currentSessionMessages = currentSessionMessages + errMsg
                                                    
                                                    withContext(Dispatchers.IO) {
                                                        file?.appendText("\n\nError: ${e.message}")
                                                        
                                                        val updatedCost = currentSessionMessages.sumOf { it.costEstimate }
                                                        val updatedLat = currentSessionMessages.sumOf { it.latencyMs }
                                                        val qCount = currentSessionMessages.filter { it.role == "model" }.size
                                                        
                                                        val sessionRecord = SessionRecord(
                                                            sessionId = activeSessionId,
                                                            timestamp = System.currentTimeMillis(),
                                                            modelName = "gemini-2.5-flash",
                                                            totalCost = updatedCost,
                                                            totalLatencyMs = updatedLat,
                                                            queryCount = qCount,
                                                            userRanking = currentRanking,
                                                            userNotes = currentNotes,
                                                            messages = currentSessionMessages
                                                        )
                                                        saveSessionToDisk(context, storageLocation, sessionRecord)
                                                        statsTrigger++
                                                    }
                                                } finally {
                                                    isGenerating = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = currentPrompt.isNotBlank() && !isGenerating && geminiApiKey.isNotEmpty()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                                }
                            }
                        }
                    }
                    1 -> {
                        // --- TAB 1: DEVELOPER BENCHMARKS ---
                        Column(modifier = Modifier.weight(1f).fillMaxSize().padding(16.dp)) {
                            if (selectedSession != null) {
                                // --- SELECTED SESSION SUBVIEW ---
                                val session = selectedSession!!
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { 
                                            selectedSession = null 
                                            replayComparisonResult = null
                                            replayStepsLog = emptyList()
                                        }
                                    ) {
                                        Text("← Back to List", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                deleteSessionFromDisk(context, storageLocation, session.sessionId)
                                                selectedSession = null
                                                statsTrigger++
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Archive ID: ${session.sessionId}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Column {
                                                Text("Model", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(session.modelName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            }
                                            Column {
                                                Text("Queries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${session.queryCount}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            }
                                            Column {
                                                Text("Latency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${session.totalLatencyMs}ms", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            }
                                            Column {
                                                Text("Telemetry Cost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("$${String.format(Locale.US, "%.5f", session.totalCost)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        // Robust visual horizontal divider line
                                        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text("Post-Session Evaluation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        ) {
                                            Text("Session Quality Rating:  ", style = MaterialTheme.typography.bodySmall)
                                            for (star in 1..5) {
                                                val isFilled = star <= session.userRanking
                                                Text(
                                                    text = if (isFilled) "★" else "☆",
                                                    fontSize = 22.sp,
                                                    color = if (isFilled) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .clickable {
                                                            session.userRanking = star
                                                            coroutineScope.launch(Dispatchers.IO) {
                                                                saveSessionToDisk(context, storageLocation, session)
                                                                statsTrigger++
                                                            }
                                                        }
                                                        .padding(horizontal = 4.dp)
                                                )
                                            }
                                        }

                                        var notesInput by remember(session.sessionId) { mutableStateOf(session.userNotes) }
                                        OutlinedTextField(
                                            value = notesInput,
                                            onValueChange = { notesInput = it },
                                            label = { Text("Developer review & benchmark observations", fontSize = 12.sp) },
                                            modifier = Modifier.fillMaxWidth().height(80.dp),
                                            textStyle = MaterialTheme.typography.bodySmall
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = {
                                                session.userNotes = notesInput
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    saveSessionToDisk(context, storageLocation, session)
                                                    statsTrigger++
                                                }
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Save Review Notes", fontSize = 11.sp)
                                        }
                                    }
                                }

                                var expandedSect by remember { mutableStateOf(0) } // 0: Trace Timeline, 1: Script Replay, 2: Raw JSON Schema

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    ElevatedButton(onClick = { expandedSect = 0 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.elevatedButtonColors(containerColor = if (expandedSect == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                                        Text("Trace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    ElevatedButton(onClick = { expandedSect = 1 }, modifier = Modifier.weight(1.2f), colors = ButtonDefaults.elevatedButtonColors(containerColor = if (expandedSect == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                                        Text("Replay Test", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    ElevatedButton(onClick = { expandedSect = 2 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.elevatedButtonColors(containerColor = if (expandedSect == 2) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                                        Text("Raw JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    when (expandedSect) {
                                        0 -> {
                                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(session.messages) { msg ->
                                                    val isUser = msg.role == "user"
                                                    val isError = msg.role == "error"
                                                    val bulletColor = if (isError) MaterialTheme.colorScheme.error else if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(bulletColor.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
                                                            .padding(10.dp)
                                                    ) {
                                                        Column {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = if (isUser) "Prompt (User)" else if (isError) "Failure Trace" else "Response (AI)",
                                                                    fontWeight = FontWeight.Bold,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = bulletColor
                                                                )
                                                                Spacer(modifier = Modifier.weight(1f))
                                                                Text(
                                                                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(msg.timestamp)),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            SelectionContainer {
                                                                Text(msg.text, style = MaterialTheme.typography.bodySmall)
                                                            }
                                                            if (!isUser) {
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text(
                                                                    text = "Metrics: Latency ${msg.latencyMs}ms || Est. Cost $${String.format(Locale.US, "%.5f", msg.costEstimate)} (${msg.tokenEstimate} tokens)",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        1 -> {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Text(
                                                    text = "Automatic Script Runner / Replay Test",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "This executes original user prompts sequentially to test model improvements & track latency/cost regressions.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            isReplaying = true
                                                            replayComparisonResult = null
                                                            replayStepsLog = listOf("Initiating regression replay simulation...", "Retrieving prompts from session record index...")
                                                            coroutineScope.launch {
                                                                val prompts = session.messages.filter { it.role == "user" }
                                                                if (prompts.isEmpty()) {
                                                                    replayStepsLog = replayStepsLog + "⚠️ No user prompts found in session archive to replay."
                                                                    isReplaying = false
                                                                    return@launch
                                                                }

                                                                val generativeModel = GenerativeModel(
                                                                    modelName = "gemini-2.5-flash",
                                                                    apiKey = geminiApiKey
                                                                )

                                                                var reTotalLat = 0L
                                                                var reTotalCost = 0.0

                                                                for ((i, p) in prompts.withIndex()) {
                                                                    replayStepsLog = replayStepsLog + "🔄 Running step [${i+1}/${prompts.size}]: Prompt: \"${if (p.text.length > 25) p.text.substring(0, 25) + "..." else p.text}\""
                                                                    val start = System.currentTimeMillis()
                                                                    try {
                                                                        val isImagePrompt = p.text.startsWith("/image ", ignoreCase = true) || p.text.lowercase(Locale.US).startsWith("generate image")
                                                                        val contentText = if (isImagePrompt) {
                                                                            val imagePrompt = if (p.text.startsWith("/image ", ignoreCase = true)) p.text.substring(7).trim() else p.text
                                                                            withContext(Dispatchers.IO) {
                                                                                val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateContent?key=$geminiApiKey")
                                                                                val conn = url.openConnection() as java.net.HttpURLConnection
                                                                                conn.requestMethod = "POST"
                                                                                conn.setRequestProperty("Content-Type", "application/json")
                                                                                conn.doOutput = true
                                                                                val escapedPrompt = imagePrompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ")
                                                                                val jsonInputString = "{\"contents\": [{\"parts\": [{\"text\": \"$escapedPrompt\"}]}], \"config\": {\"imageConfig\": {\"aspectRatio\": \"1:1\", \"imageSize\": \"1K\"}}}"
                                                                                conn.outputStream.use { os ->
                                                                                    val input = jsonInputString.toByteArray(Charsets.UTF_8)
                                                                                    os.write(input, 0, input.size)
                                                                                }
                                                                                val respCode = conn.responseCode
                                                                                val respStr = if (respCode in 200..299) {
                                                                                    conn.inputStream.bufferedReader().use { it.readText() }
                                                                                } else {
                                                                                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                                                                                }
                                                                                if (respCode in 200..299) "✨ Image generated via replay" else "Error HTTP $respCode: $respStr"
                                                                            }
                                                                        } else {
                                                                            val resp = withContext(Dispatchers.IO) {
                                                                                generativeModel.generateContent(p.text)
                                                                            }
                                                                            resp.text ?: ""
                                                                        }
                                                                        val end = System.currentTimeMillis()
                                                                        val delta = end - start
                                                                        
                                                                        val inToks = Math.ceil(p.text.length / 4.0).toInt().coerceAtLeast(1)
                                                                        val outToks = Math.ceil(contentText.length / 4.0).toInt().coerceAtLeast(1)
                                                                        val stepCost = (inToks * 0.000000075) + (outToks * 0.000000300)

                                                                        reTotalLat += delta
                                                                        reTotalCost += stepCost

                                                                        replayStepsLog = replayStepsLog + "  ↳ ✅ Success: ${delta}ms | Cost: $${String.format(Locale.US, "%.5f", stepCost)}"
                                                                    } catch (err: Exception) {
                                                                        replayStepsLog = replayStepsLog + "  ↳ ❌ Failed: ${err.message}"
                                                                    }
                                                                }

                                                                val latDiff = reTotalLat - session.totalLatencyMs
                                                                val costDiff = reTotalCost - session.totalCost

                                                                replayComparisonResult = ReplayComparison(
                                                                    originalLatency = session.totalLatencyMs,
                                                                    replayLatency = reTotalLat,
                                                                    originalCost = session.totalCost,
                                                                    replayCost = reTotalCost,
                                                                    latencyDeltaPercent = if (session.totalLatencyMs > 0) (latDiff.toDouble() / session.totalLatencyMs) * 100.0 else 0.0,
                                                                    costDeltaPercent = if (session.totalCost > 0.0) (costDiff / session.totalCost) * 100.0 else 0.0
                                                                )
                                                                replayStepsLog = replayStepsLog + "🏁 Benchmark Replay Finished successfully."
                                                                isReplaying = false
                                                            }
                                                        },
                                                        enabled = !isReplaying
                                                    ) {
                                                        Text(if (isReplaying) "Evaluating..." else "Run Replay Benchmark ⚡")
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                if (replayComparisonResult != null) {
                                                    val comp = replayComparisonResult!!
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Text("Replay Comparison Report", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                                Text("Metric", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                                                Text("Original", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                                                Text("Replay", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                                                Text("Delta %", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                                            }
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                                                            Spacer(modifier = Modifier.height(4.dp))

                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                                Text("Latency", style = MaterialTheme.typography.bodySmall)
                                                                Text("${comp.originalLatency}ms", style = MaterialTheme.typography.bodySmall)
                                                                Text("${comp.replayLatency}ms", style = MaterialTheme.typography.bodySmall)
                                                                val pct = String.format(Locale.US, "%+.1f%%", comp.latencyDeltaPercent)
                                                                Text(
                                                                    text = pct,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = if (comp.latencyDeltaPercent <= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                                Text("Est Cost", style = MaterialTheme.typography.bodySmall)
                                                                Text("$${String.format(Locale.US, "%.5f", comp.originalCost)}", style = MaterialTheme.typography.bodySmall)
                                                                Text("$${String.format(Locale.US, "%.5f", comp.replayCost)}", style = MaterialTheme.typography.bodySmall)
                                                                val pct = String.format(Locale.US, "%+.1f%%", comp.costDeltaPercent)
                                                                Text(
                                                                    text = pct,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = if (comp.costDeltaPercent <= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }

                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            val speedUpPct = -comp.latencyDeltaPercent
                                                            if (speedUpPct > 0) {
                                                                Text("🚀 Replay executed ${String.format(Locale.US, "%.1f%%", speedUpPct)} faster than original!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                                            } else if (speedUpPct < 0) {
                                                                Text("⌛ Replay was slower by ${String.format(Locale.US, "%.1f%%", -speedUpPct)}.", fontSize = 11.sp, color = Color(0xFFC62828))
                                                            } else {
                                                                Text("⚖️ Latency remained identical.", fontSize = 11.sp)
                                                            }
                                                        }
                                                    }
                                                }

                                                if (replayStepsLog.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text("Replay Logs:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    LazyColumn(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp))
                                                            .padding(8.dp)
                                                    ) {
                                                        items(replayStepsLog) { logLine ->
                                                            Text(
                                                                text = logLine,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontSize = 11.sp,
                                                                color = if (logLine.contains("✅")) Color(0xFF2E7D32) else if (logLine.contains("❌")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        2 -> {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Text("Session JSON Archive Schema", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                Text("Copy this format to scripts or testing tools to replay the telemetry sequence offline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                val prettyJson = remember(session.sessionId) { sessionToJson(session) }
                                                SelectionContainer(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                                    Text(
                                                        text = prettyJson,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                                                            .padding(12.dp)
                                                            .verticalScroll(rememberScrollState())
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // --- MAIN BENCHMARK DASHBOARD / ARCHIVE CATALOG ---
                                if (savedSessions.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No archived sessions recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    val totalSessions = savedSessions.size
                                    val totalCostSum = savedSessions.sumOf { it.totalCost }
                                    val avgLatencySec = if (totalSessions > 0) (savedSessions.sumOf { it.totalLatencyMs } / 1000.0) / totalSessions else 0.0
                                    val sessionsWithRating = savedSessions.filter { it.userRanking > 0 }
                                    val avgRating = if (sessionsWithRating.isNotEmpty()) sessionsWithRating.sumOf { it.userRanking }.toDouble() / sessionsWithRating.size else 0.0

                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    text = "Active Telemetry Dashboard",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    DashboardMetric("Archived", "$totalSessions", Modifier.weight(1.0f))
                                                    DashboardMetric("Total Cost", String.format(Locale.US, "$%.4f", totalCostSum), Modifier.weight(1.1f))
                                                    DashboardMetric("Avg Latency", String.format(Locale.US, "%.2fs", avgLatencySec), Modifier.weight(1.1f))
                                                    DashboardMetric("Avg Rating", if (avgRating > 0) String.format(Locale.US, "%.1f★", avgRating) else "N/A", Modifier.weight(1.0f))
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Archived Developer Sessions",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        LazyColumn(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(savedSessions) { session ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { selectedSession = session },
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = session.sessionId.replace("SESSION_", ""),
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Row {
                                                                for (s in 1..5) {
                                                                    Text(
                                                                        text = if (s <= session.userRanking) "★" else "☆",
                                                                        color = if (s <= session.userRanking) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        fontSize = 12.sp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                        ) {
                                                            Text(
                                                                text = "${session.queryCount} queries",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = "💰 $${String.format(Locale.US, "%.5f", session.totalCost)}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = "⏱️ ${session.totalLatencyMs}ms",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        if (session.userNotes.isNotBlank()) {
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Text(
                                                                text = "📝 Note: ${session.userNotes}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
