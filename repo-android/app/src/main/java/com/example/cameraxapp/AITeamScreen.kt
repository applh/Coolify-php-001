package com.example.cameraxapp

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- THEME COLOR PALETTE (BENTO GRID STYLE) ---
val DeepOnyx = Color(0xFF1C1B1F)
val SlatePurple = Color(0xFF2B2930)
val LavenderTint = Color(0xFFD0BCFF)
val OnPrimaryPurple = Color(0xFF381E72)
val LavenderContainer = Color(0xFF4A4458)
val OnLavenderContainer = Color(0xFFEADDFF)
val BorderColor = Color(0xFF49454F)

// --- AI SESSION DATA MODELS (MOSHI COMPATIBLE) ---

data class SessionHeader(
    val id: String,
    val title: String,
    val startTime: Long,
    val lastActiveTime: Long,
    val totalTokens: Int,
    val modelPreset: String,
    val highlightsCount: Int,
    val latestSummary: String? = null
)

enum class ChatRole {
    USER,
    MODEL
}

enum class Modality {
    TEXT,
    IMAGE
}

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val modality: Modality,
    val timestamp: Long,
    val imageUrl: String? = null,   // Scoped storage URI or local file path
    val durationMs: Long? = null,
    val calculatedCost: Double? = null,
    val tokensUsed: Int? = null,
    val modelName: String? = null
)

data class SessionSegment(
    val sessionId: String,
    val messages: List<ChatMessage>
)

// --- RETROFIT & GEMINI PROTOCOL CLIENT ---

data class GeminiRequest(
    val contents: List<ContentPartList>,
    val generationConfig: GenerationConfig
)

data class ContentPartList(
    val role: String? = null,
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String
)

data class GenerationConfig(
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String> = listOf("TEXT")
)

data class ImageConfig(
    val aspectRatio: String,
    val imageSize: String? = null
)

data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val content: ResponseContent? = null
)

data class ResponseContent(
    val parts: List<ResponsePart>? = null
)

data class ResponsePart(
    val text: String? = null,
    val inlineData: ResponseInlineData? = null
)

data class ResponseInlineData(
    val mimeType: String? = null,
    val data: String? = null
)

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val geminiApi: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }
}

// --- LOCAL CONVERSATIONAL STORAGE CONTROLLER (MOSHI-BASED) ---

class SessionStorageController(private val context: Context) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val baseDir = java.io.File(context.filesDir, "AITeam")
    private val sessionsDir = java.io.File(baseDir, "sessions")
    private val imagesDir = java.io.File(baseDir, "images")

    init {
        if (!baseDir.exists()) baseDir.mkdirs()
        if (!sessionsDir.exists()) sessionsDir.mkdirs()
        if (!imagesDir.exists()) imagesDir.mkdirs()
        
        // Handle migration of legacy sessions.json if present
        migrateLegacyHistory()
    }

    private fun migrateLegacyHistory() {
        try {
            val legacyFile = java.io.File(context.filesDir, "sessions.json")
            val manifestFile = java.io.File(baseDir, "sessions_manifest.json")
            if (legacyFile.exists() && !manifestFile.exists()) {
                // Read legacy items and create corresponding session files
                val jsonStr = legacyFile.readText()
                val legacyType = com.squareup.moshi.Types.newParameterizedType(List::class.java, SessionItemLegacy::class.java)
                val legacyAdapter = moshi.adapter<List<SessionItemLegacy>>(legacyType)
                val legacyItems = legacyAdapter.fromJson(jsonStr) ?: emptyList()

                val headers = mutableListOf<SessionHeader>()
                for (item in legacyItems) {
                    val legacyPng = java.io.File(context.filesDir, "session_${item.id}.png")
                    val localImageFile = java.io.File(imagesDir, "img_${item.id}.png")
                    if (legacyPng.exists()) {
                        legacyPng.copyTo(localImageFile, overwrite = true)
                    }

                    val titleText = if (item.prompt.length > 25) item.prompt.take(25) + "..." else item.prompt
                    headers.add(
                        SessionHeader(
                            id = item.id,
                            title = titleText,
                            startTime = item.timestamp,
                            lastActiveTime = item.timestamp,
                            totalTokens = item.tokensUsed,
                            modelPreset = item.model,
                            highlightsCount = 1,
                            latestSummary = item.prompt
                        )
                    )

                    val chatMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = ChatRole.USER,
                        content = item.prompt,
                        modality = Modality.IMAGE,
                        timestamp = item.timestamp,
                        imageUrl = localImageFile.absolutePath,
                        durationMs = item.durationMs,
                        calculatedCost = item.calculatedCost,
                        tokensUsed = item.tokensUsed,
                        modelName = item.model
                    )

                    val segment = SessionSegment(sessionId = item.id, messages = listOf(chatMessage))
                    val segmentFile = java.io.File(sessionsDir, "session_${item.id}.json")
                    val segmentAdapter = moshi.adapter(SessionSegment::class.java)
                    segmentFile.writeText(segmentAdapter.toJson(segment))
                }

                val manifestAdapter = moshi.adapter<List<SessionHeader>>(
                    com.squareup.moshi.Types.newParameterizedType(List::class.java, SessionHeader::class.java)
                )
                manifestFile.writeText(manifestAdapter.toJson(headers))
                
                legacyFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getSessionHeaders(): List<SessionHeader> = withContext(Dispatchers.IO) {
        val manifestFile = java.io.File(baseDir, "sessions_manifest.json")
        if (!manifestFile.exists()) return@withContext emptyList()
        return@withContext try {
            val json = manifestFile.readText()
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, SessionHeader::class.java)
            val adapter = moshi.adapter<List<SessionHeader>>(listType)
            adapter.fromJson(json)?.sortedByDescending { it.lastActiveTime } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveSessionHeaders(headers: List<SessionHeader>) = withContext(Dispatchers.IO) {
        try {
            val manifestFile = java.io.File(baseDir, "sessions_manifest.json")
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, SessionHeader::class.java)
            val adapter = moshi.adapter<List<SessionHeader>>(listType)
            val json = adapter.toJson(headers.sortedByDescending { it.lastActiveTime })
            manifestFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getSessionSegment(sessionId: String): SessionSegment? = withContext(Dispatchers.IO) {
        val sessionFile = java.io.File(sessionsDir, "session_${sessionId}.json")
        if (!sessionFile.exists()) return@withContext null
        return@withContext try {
            val json = sessionFile.readText()
            val adapter = moshi.adapter(SessionSegment::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveSessionSegment(segment: SessionSegment) = withContext(Dispatchers.IO) {
        try {
            val sessionFile = java.io.File(sessionsDir, "session_${segment.sessionId}.json")
            val adapter = moshi.adapter(SessionSegment::class.java)
            val json = adapter.toJson(segment)
            sessionFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLocalImageFile(messageId: String): java.io.File {
        return java.io.File(imagesDir, "img_${messageId}.png")
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            val sessionFile = java.io.File(sessionsDir, "session_${sessionId}.json")
            if (sessionFile.exists()) sessionFile.delete()

            val headers = getSessionHeaders().filterNot { it.id == sessionId }
            saveSessionHeaders(headers)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Legacy structure wrapper for safe migration
data class SessionItemLegacy(
    val id: String,
    val prompt: String,
    val storageUri: String?,
    val model: String,
    val timestamp: Long,
    val durationMs: Long,
    val tokensUsed: Int,
    val calculatedCost: Double
)

// --- REVISED ARCHITECTURAL STATE MACHINE (MVVM) ---

sealed interface UiState {
    object Idle : UiState
    object Processing : UiState
    data class Success(val message: String) : UiState
    data class Error(val message: String) : UiState
}

class AITeamViewModel(private val repository: SettingsRepository, context: Context) : ViewModel() {
    private val controller = SessionStorageController(context)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _sessionsManifest = MutableStateFlow<List<SessionHeader>>(emptyList())
    val sessionsManifest: StateFlow<List<SessionHeader>> = _sessionsManifest.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _activeMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeMessages: StateFlow<List<ChatMessage>> = _activeMessages.asStateFlow()

    init {
        loadManifestAndResume()
    }

    fun loadManifestAndResume() {
        viewModelScope.launch {
            val headers = controller.getSessionHeaders()
            _sessionsManifest.value = headers
            if (headers.isNotEmpty()) {
                val latest = headers.first()
                loadSession(latest.id)
            } else {
                startNewSession()
            }
        }
    }

    fun startNewSession() {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val newHeader = SessionHeader(
                id = newId,
                title = "New Dialogue Branch",
                startTime = timestamp,
                lastActiveTime = timestamp,
                totalTokens = 0,
                modelPreset = "gemini-2.5-flash",
                highlightsCount = 0,
                latestSummary = "Dialogue thread started."
            )

            val updatedList = listOf(newHeader) + _sessionsManifest.value
            _sessionsManifest.value = updatedList
            controller.saveSessionHeaders(updatedList)

            val segment = SessionSegment(sessionId = newId, messages = emptyList())
            controller.saveSessionSegment(segment)

            _activeSessionId.value = newId
            _activeMessages.value = emptyList()
            _uiState.value = UiState.Idle
        }
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            val segment = controller.getSessionSegment(sessionId)
            _activeSessionId.value = sessionId
            _activeMessages.value = segment?.messages ?: emptyList()
            _uiState.value = UiState.Idle
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            controller.deleteSession(sessionId)
            val updatedManifest = controller.getSessionHeaders()
            _sessionsManifest.value = updatedManifest

            if (updatedManifest.isNotEmpty()) {
                loadSession(updatedManifest.first().id)
            } else {
                startNewSession()
            }
        }
    }

    fun clearActiveSessionMessages() {
        val sessionId = _activeSessionId.value ?: return
        viewModelScope.launch {
            val emptySegment = SessionSegment(sessionId = sessionId, messages = emptyList())
            controller.saveSessionSegment(emptySegment)
            _activeMessages.value = emptyList()

            // Update title
            val timestamp = System.currentTimeMillis()
            val headers = _sessionsManifest.value.map { header ->
                if (header.id == sessionId) {
                    header.copy(
                        title = "Cleared Dialogue",
                        lastActiveTime = timestamp,
                        totalTokens = 0,
                        highlightsCount = 0,
                        latestSummary = "Dialogue content cleared."
                    )
                } else header
            }
            _sessionsManifest.value = headers
            controller.saveSessionHeaders(headers)
        }
    }

    fun sendMessage(
        context: Context,
        prompt: String,
        isImageMode: Boolean,
        apiKey: String,
        imageModelPreset: String,
        imageRatio: String,
        imageSize: String
    ) {
        val sessionId = _activeSessionId.value ?: return
        if (prompt.trim().isEmpty()) {
            _uiState.value = UiState.Error("Prompt content cannot be empty. Please formulate a description first.")
            return
        }

        if (apiKey.trim().isEmpty()) {
            _uiState.value = UiState.Error("Gemini API Key is missing. Please secure your API key in Settings first.")
            return
        }

        _uiState.value = UiState.Processing
        val startTime = System.currentTimeMillis()

        // Append user message immediately
        val userMsgId = UUID.randomUUID().toString()
        val userMsg = ChatMessage(
            id = userMsgId,
            role = ChatRole.USER,
            content = prompt.trim(),
            modality = Modality.TEXT,
            timestamp = startTime
        )
        val updatedMessages = _activeMessages.value + userMsg
        _activeMessages.value = updatedMessages

        viewModelScope.launch {
            try {
                if (isImageMode) {
                    // IMAGE GENERATION FLOW
                    val apiModelName = if (imageModelPreset == "gemini-3.1-flash-image-preview") "gemini-3.1-flash-image-preview" else "gemini-2.5-flash-image"
                    
                    val req = GeminiRequest(
                        contents = listOf(
                            ContentPartList(
                                parts = listOf(Part(text = prompt.trim()))
                            )
                        ),
                        generationConfig = GenerationConfig(
                            imageConfig = ImageConfig(
                                aspectRatio = imageRatio,
                                imageSize = imageSize
                            ),
                            responseModalities = listOf("TEXT", "IMAGE")
                        )
                    )

                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.geminiApi.generateContent(
                            model = apiModelName,
                            apiKey = apiKey,
                            request = req
                        )
                    }

                    val contentParts = response.candidates?.firstOrNull()?.content?.parts
                    val inlinePart = contentParts?.firstOrNull { it.inlineData != null && it.inlineData.data != null }
                    val textPart = contentParts?.firstOrNull { it.text != null }

                    if (inlinePart?.inlineData?.data != null) {
                        val base64Data = inlinePart.inlineData.data!!
                        val bitmap = withContext(Dispatchers.IO) {
                            StorageUtils.base64ToBitmap(base64Data)
                        }

                        if (bitmap != null) {
                            // Save to local cached directory
                            val modelMsgId = UUID.randomUUID().toString()
                            val localFile = controller.getLocalImageFile(modelMsgId)
                            withContext(Dispatchers.IO) {
                                java.io.FileOutputStream(localFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                            }

                            // Write to phone Gallery as fallback and user asset
                            val galleryUriStr = withContext(Dispatchers.IO) {
                                StorageUtils.saveImageToGallery(context, bitmap, prompt)
                            }

                            val durationMs = System.currentTimeMillis() - startTime
                            // Pricing formula
                            val tokensUsed = (prompt.length / 4) + 4
                            val inputCost = (tokensUsed.toDouble() / 1000.0) * 0.000075
                            val baseImageCost = when (imageSize) {
                                "512px" -> 0.015
                                "1K" -> 0.03
                                "2K" -> 0.045
                                else -> 0.05
                            }
                            val calculatedCost = inputCost + baseImageCost

                            val modelMsg = ChatMessage(
                                id = modelMsgId,
                                role = ChatRole.MODEL,
                                content = "Generated canvas matching prompt \"$prompt\"",
                                modality = Modality.IMAGE,
                                timestamp = System.currentTimeMillis(),
                                imageUrl = localFile.absolutePath,
                                durationMs = durationMs,
                                calculatedCost = calculatedCost,
                                tokensUsed = tokensUsed,
                                modelName = apiModelName
                            )

                            val finalMessages = _activeMessages.value + modelMsg
                            _activeMessages.value = finalMessages

                            // Save individual segment
                            controller.saveSessionSegment(SessionSegment(sessionId = sessionId, messages = finalMessages))

                            // Update manifest header details
                            updateHeaderMeta(
                                sessionId = sessionId,
                                newTokens = tokensUsed,
                                incHighlights = 1,
                                summary = "Visual output generated.",
                                fallbackTitle = prompt
                            )

                            _uiState.value = UiState.Success("Masterpiece rendered successfully!")
                        } else {
                            _uiState.value = UiState.Error("Pixelation error: Received successful response but failed to construct bitmap from raw image bytes.")
                        }
                    } else if (textPart?.text != null) {
                        _uiState.value = UiState.Error("Gemini Engine returned textual metadata instead of image pixels: ${textPart.text}")
                    } else {
                        _uiState.value = UiState.Error("Fermion Crash: The Gemini model processed the prompt but did not supply visual elements. Please rewrite your prompt.")
                    }

                } else {
                    // TEXT CONVERSATIONAL DIALOG FLOW
                    val apiModelName = "gemini-2.1-flash" // Stable text flash fallback model representation

                    // Multi-turn message building
                    val apiContents = mutableListOf<ContentPartList>()
                    for (msg in updatedMessages) {
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

                    val req = GeminiRequest(
                        contents = apiContents,
                        generationConfig = GenerationConfig(
                            responseModalities = listOf("TEXT")
                        )
                    )

                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.geminiApi.generateContent(
                            model = "gemini-2.5-flash",
                            apiKey = apiKey,
                            request = req
                        )
                    }

                    val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                    if (responseText != null) {
                        val durationMs = System.currentTimeMillis() - startTime
                        val tokensUsed = (prompt.length / 4) + (responseText.length / 4)
                        val calculatedCost = (tokensUsed.toDouble() / 1000.0) * 0.000075

                        val modelMsg = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            role = ChatRole.MODEL,
                            content = responseText,
                            modality = Modality.TEXT,
                            timestamp = System.currentTimeMillis(),
                            durationMs = durationMs,
                            calculatedCost = calculatedCost,
                            tokensUsed = tokensUsed,
                            modelName = "gemini-2.5-flash"
                        )

                        val finalMessages = _activeMessages.value + modelMsg
                        _activeMessages.value = finalMessages

                        // Save individual segment
                        controller.saveSessionSegment(SessionSegment(sessionId = sessionId, messages = finalMessages))

                        // Update manifest header details
                        val textHeading = if (prompt.length > 25) prompt.take(25) + "..." else prompt
                        updateHeaderMeta(
                            sessionId = sessionId,
                            newTokens = tokensUsed,
                            incHighlights = 0,
                            summary = if (responseText.length > 40) responseText.take(40) + "..." else responseText,
                            fallbackTitle = textHeading
                        )

                        _uiState.value = UiState.Success("Received response successfully.")
                    } else {
                        _uiState.value = UiState.Error("No text response candidate parsed from the model candidates.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error(e.message ?: "Quantum phase offset in network request. Check connection or API credentials.")
                
                // Rollback user message if request failed cleanly
                _activeMessages.value = _activeMessages.value.filterNot { it.id == userMsgId }
            }
        }
    }

    private suspend fun updateHeaderMeta(
        sessionId: String,
        newTokens: Int,
        incHighlights: Int,
        summary: String,
        fallbackTitle: String
    ) {
        val currentHeaders = controller.getSessionHeaders()
        val timestamp = System.currentTimeMillis()
        var headerUpdated = false

        val updatedHeaders = currentHeaders.map { header ->
            if (header.id == sessionId) {
                headerUpdated = true
                val assignedTitle = if (header.title == "New Dialogue Branch" || header.title.isEmpty()) {
                    fallbackTitle
                } else header.title

                header.copy(
                    title = assignedTitle,
                    lastActiveTime = timestamp,
                    totalTokens = header.totalTokens + newTokens,
                    highlightsCount = header.highlightsCount + incHighlights,
                    latestSummary = summary
                )
            } else header
        }

        val finalHeaders = if (!headerUpdated) {
            val newHeader = SessionHeader(
                id = sessionId,
                title = fallbackTitle,
                startTime = timestamp,
                lastActiveTime = timestamp,
                totalTokens = newTokens,
                modelPreset = "gemini-2.5-flash",
                highlightsCount = incHighlights,
                latestSummary = summary
            )
            listOf(newHeader) + updatedHeaders
        } else {
            updatedHeaders
        }

        val sortedFinal = finalHeaders.sortedByDescending { it.lastActiveTime }
        _sessionsManifest.value = sortedFinal
        controller.saveSessionHeaders(sortedFinal)
    }

    fun clearState() {
        _uiState.value = UiState.Idle
    }
}

// --- USER INTERFACE COMPOSABLES WITH INTEGRAL DOUBLE-COLUMN LAYOUT ---

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AITeamScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }

    // Saved core options flow mapping back to local State
    val geminiApiKeySaved by repository.geminiApiKey.collectAsState(initial = "")
    val aiModelPreset by repository.aiModel.collectAsState(initial = "gemini-2.5-flash-image")
    val aiRatioPreset by repository.aiRatio.collectAsState(initial = "1:1")
    val aiSizePreset by repository.aiSize.collectAsState(initial = "1K")

    // Scoped Multi-turn session states
    val viewModel = remember { AITeamViewModel(repository, context) }
    val uiState by viewModel.uiState.collectAsState()
    val manifestFeed by viewModel.sessionsManifest.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val conversationList by viewModel.activeMessages.collectAsState()

    var textPromptInput by remember { mutableStateOf("") }
    var isImageModeChecked by remember { mutableStateOf(false) }

    // Drawer state on mobile
    var mobileSidebarDropdownToggled by remember { mutableStateOf(false) }

    // Pinch-to-zoom zoomable Lightbox overlays
    var activeLightboxMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    // Scroll automatically to newest bubble
    LaunchedEffect(conversationList.size) {
        if (conversationList.isNotEmpty()) {
            listState.animateScrollToItem(conversationList.size - 1)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DeepOnyx)) {
        val isTabletSize = maxWidth > 680.dp
        
        // Main structural Row (Dual-Pane Responsive Design)
        Row(modifier = Modifier.fillMaxSize()) {
            
            // PANEL 1: Permanent left sidebar on Tablet displays
            if (isTabletSize) {
                Surface(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    color = SlatePurple,
                    border = BorderStroke(end = 1.dp, color = BorderColor)
                ) {
                    SessionSidebarContent(
                        manifestList = manifestFeed,
                        activeId = activeSessionId,
                        onSessionSelect = { id -> viewModel.loadSession(id) },
                        onNewSession = { viewModel.startNewSession() },
                        onDeleteSession = { id -> viewModel.deleteSession(id) }
                    )
                }
            }

            // PANEL 2: Chat dialog scroller screen frame
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(bottom = 8.dp)
            ) {
                // Toolbar Layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open App Drawer",
                                tint = Color.White
                            )
                        }
                        
                        // Mobile past sessions history expander icon
                        if (!isTabletSize) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { mobileSidebarDropdownToggled = true }) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "Show Past Dialogues",
                                    tint = LavenderTint
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = LavenderTint,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = OnPrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Lumina Team",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.SansSerif
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "MULTI-TURN SESSION HUB",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = LavenderTint
                            )
                        }
                    }

                    // Flush Dialogue / Return home
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { viewModel.clearActiveSessionMessages() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Clear Current Dialogue history",
                                tint = Color.LightGray
                            )
                        }
                        Surface(
                            onClick = onBack,
                            shape = RoundedCornerShape(20.dp),
                            color = SlatePurple,
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderColor)

                // Main dialogue feed scrolling viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (conversationList.isEmpty()) {
                        EmptyDialoguePlaceholder(
                            imageModelPreset = aiModelPreset,
                            isImageMode = isImageModeChecked
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(conversationList) { item ->
                                ChatMessageBubble(
                                    message = item,
                                    onImageTap = { msg -> activeLightboxMessage = msg },
                                    onCopyText = { txt ->
                                        clipboardManager.setText(AnnotatedString(txt))
                                    }
                                )
                            }
                        }
                    }

                    // Floating prompt indicator for processing operations
                    if (uiState is UiState.Processing) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            color = SlatePurple.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = LavenderTint,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = if (isImageModeChecked) "Painting pixels into visual reality..." else "Formulating response dialog matrix...",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderColor)

                // Input control pane docked below scroller
                OutlinedInputPane(
                    promptText = textPromptInput,
                    onPromptTextChange = { textPromptInput = it },
                    isImageMode = isImageModeChecked,
                    onImageModeChange = { isImageModeChecked = it },
                    isProcessing = uiState is UiState.Processing,
                    onSend = {
                        viewModel.sendMessage(
                            context = context,
                            prompt = textPromptInput,
                            isImageMode = isImageModeChecked,
                            apiKey = geminiApiKeySaved,
                            imageModelPreset = aiModelPreset,
                            imageRatio = aiRatioPreset,
                            imageSize = aiSizePreset
                        )
                        textPromptInput = ""
                    },
                    hasKey = geminiApiKeySaved.isNotEmpty(),
                    modelStr = if (isImageModeChecked) aiModelPreset else "gemini-2.5-flash",
                    ratioStr = aiRatioPreset,
                    sizeStr = aiSizePreset
                )
            }
        }

        // MOBILE SCREEN MODAL DRAWER OVERLAY
        if (!isTabletSize && mobileSidebarDropdownToggled) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize().clickable { mobileSidebarDropdownToggled = false })
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(280.dp)
                            .align(Alignment.CenterStart),
                        color = SlatePurple,
                        tonalElevation = 8.dp
                    ) {
                        SessionSidebarContent(
                            manifestList = manifestFeed,
                            activeId = activeSessionId,
                            onSessionSelect = { id ->
                                viewModel.loadSession(id)
                                mobileSidebarDropdownToggled = false
                            },
                            onNewSession = {
                                viewModel.startNewSession()
                                mobileSidebarDropdownToggled = false
                            },
                            onDeleteSession = { id -> viewModel.deleteSession(id) }
                        )
                    }
                }
            }
        }

        // FULL SCREEN PINCH-TO-ZOOM LIGHTBOX MODAL OVERLAY
        activeLightboxMessage?.let { msg ->
            LightboxOverlay(
                imageUrl = msg.imageUrl,
                title = msg.content,
                modelUsed = msg.modelName ?: "Lumina Engine",
                tokens = msg.tokensUsed ?: 0,
                cost = msg.calculatedCost ?: 0.0,
                onDismiss = { activeLightboxMessage = null }
            )
        }
    }
}

// --- COMPOSE HELPER PANELS & ELEMENT CARDS ---

@Composable
fun SessionSidebarContent(
    manifestList: List<SessionHeader>,
    activeId: String?,
    onSessionSelect: (String) -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Text(
            text = "Dialogue History",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = LavenderTint,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = onNewSession,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LavenderTint, contentColor = OnPrimaryPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("NEW TIMELINE", fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(manifestList) { header ->
                val isActive = header.id == activeId
                Surface(
                    onClick = { onSessionSelect(header.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isActive) LavenderContainer else Color.Transparent,
                    border = BorderStroke(1.dp, if (isActive) LavenderTint.copy(alpha = 0.5f) else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = header.title,
                                color = if (isActive) Color.White else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = header.latestSummary ?: "Start chatting...",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        IconButton(
                            onClick = { onDeleteSession(header.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Thread permanently",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDialoguePlaceholder(imageModelPreset: String, isImageMode: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔮", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Dialogue Node Initialized",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isImageMode) {
                    "Model loaded: $imageModelPreset.\nSend a creative descriptive prompt to paint a new visual canvas."
                } else {
                    "Multi-turn Gemini neural connection is active.\nFormulate your thoughts in dialogue questions."
                },
                color = Color.Gray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onImageTap: (ChatMessage) -> Unit,
    onCopyText: (String) -> Unit
) {
    val isUser = message.role == ChatRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onCopyText(message.content) }
                    ),
                color = if (isUser) LavenderTint else SlatePurple,
                border = if (isUser) null else BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.modality == Modality.IMAGE) {
                        val file = java.io.File(message.imageUrl ?: "")
                        val bitmap = remember(message.imageUrl) {
                            if (file.exists()) {
                                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                            } else null
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Generated artwork inline",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageTap(message) },
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(DeepOnyx)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🖼️ Missing image raw references.", color = Color.Gray, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    SelectionContainer {
                        Text(
                            text = message.content,
                            color = if (isUser) OnPrimaryPurple else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            if (!isUser) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    message.modelName?.let {
                        Text(
                            text = "via " + it.uppercase(Locale.US),
                            fontSize = 9.sp,
                            color = LavenderTint,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    message.durationMs?.let {
                        Text(
                            text = "Time: ${(it / 100.0) / 10.0}s",
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                    message.calculatedCost?.let {
                        Text(
                            text = String.format("Cost: $%.5f", it),
                            fontSize = 9.sp,
                            color = Color(0xFF81C784),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OutlinedInputPane(
    promptText: String,
    onPromptTextChange: (String) -> Unit,
    isImageMode: Boolean,
    onImageModeChange: (Boolean) -> Unit,
    isProcessing: Boolean,
    onSend: () -> Unit,
    hasKey: Boolean,
    modelStr: String,
    ratioStr: String,
    sizeStr: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SlatePurple),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = onPromptTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 120.dp),
                    placeholder = { 
                        Text(
                            text = if (isImageMode) {
                                "Ask Lumina to synthesize any masterpiece art scene..."
                            } else {
                                "Converse with Gemini on design topics..."
                            },
                            fontSize = 11.sp
                        ) 
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavenderTint,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSend,
                    enabled = !isProcessing && promptText.trim().isNotEmpty() && hasKey,
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (promptText.trim().isEmpty() || !hasKey || isProcessing) {
                                BorderColor
                            } else {
                                LavenderTint
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Submit Dialogue prompt",
                        tint = if (promptText.trim().isEmpty() || !hasKey || isProcessing) {
                            Color.Gray
                        } else {
                            OnPrimaryPurple
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Switch(
                        checked = isImageMode,
                        onCheckedChange = onImageModeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnPrimaryPurple,
                            checkedTrackColor = LavenderTint,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = BorderColor
                        )
                    )
                    Text(
                        text = "⭐ Generate Image Canvas",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isImageMode) LavenderTint else Color.White
                    )
                }

                Text(
                    text = if (isImageMode) {
                        "Target: $modelStr | $ratioStr | $sizeStr"
                    } else {
                        "Target: gemini-2.1-flash (dialog text)"
                    },
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!hasKey) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠️ Gemini API Key is missing. Configure it in Settings menu.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --- FULL SIZE PINCH-TO-ZOOM TRANSLATION LIGHTBOX VIEW COMPOSABLE ---

@Composable
fun LightboxOverlay(
    imageUrl: String?,
    title: String,
    modelUsed: String,
    tokens: Int,
    cost: Double,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val gesturalState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += offsetChange
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            },
        color = Color.Black.copy(alpha = 0.95f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                val file = java.io.File(imageUrl ?: "")
                val bitmap = remember(imageUrl) {
                    if (file.exists()) {
                        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    } else null
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Zoomed masterwork render workspace",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .transformable(state = gesturalState),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("🖼️ File reference not found.", color = Color.White)
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(20.dp))
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Exit lightbox screen", tint = Color.White)
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Active Prompt: \"$title\"",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Profile config rendering inside: $modelUsed",
                        fontSize = 11.sp,
                        color = LavenderTint,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format("Calculated performance bill: %d tokens (~$%.5f)", tokens, cost),
                        fontSize = 10.sp,
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
