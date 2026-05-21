package com.example.cameraxapp

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

// --- THEME COLOR PALETTE (BENTO GRID STYLE) ---
val DeepOnyx = Color(0xFF1C1B1F)
val SlatePurple = Color(0xFF2B2930)
val LavenderTint = Color(0xFFD0BCFF)
val OnPrimaryPurple = Color(0xFF381E72)
val LavenderContainer = Color(0xFF4A4458)
val OnLavenderContainer = Color(0xFFEADDFF)
val BorderColor = Color(0xFF49454F)

// --- RETROFIT & GEMINI PROTOCOL CLIENT ---

data class GeminiRequest(
    val contents: List<ContentPartList>,
    val generationConfig: GenerationConfig
)

data class ContentPartList(
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
    val imageConfig: ImageConfig,
    val responseModalities: List<String> = listOf("TEXT", "IMAGE")
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

// --- ARCHITECTURAL STATE MACHINE (MVVM) ---

data class SessionItem(
    val id: String,
    val prompt: String,
    val storageUri: String?,
    val model: String,
    val timestamp: Long,
    val durationMs: Long,
    val tokensUsed: Int,
    val calculatedCost: Double
)

sealed interface UiState {
    object Idle : UiState
    object Generating : UiState
    data class Success(
        val bitmap: Bitmap,
        val prompt: String,
        val storageUri: String?,
        val model: String,
        val durationMs: Long,
        val tokensUsed: Int,
        val calculatedCost: Double
    ) : UiState
    data class Error(val message: String) : UiState
}

data class HistoryItem(
    val id: String,
    val prompt: String,
    val bitmap: Bitmap,
    val storageUri: String?,
    val model: String,
    val timestamp: Long,
    val durationMs: Long,
    val tokensUsed: Int,
    val calculatedCost: Double
)

class ImageGeneratorViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private fun saveBitmapToFile(context: Context, id: String, bitmap: Bitmap) {
        try {
            val file = java.io.File(context.filesDir, "session_${id}.png")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadBitmapFromFile(context: Context, id: String): Bitmap? {
        return try {
            val file = java.io.File(context.filesDir, "session_${id}.png")
            if (file.exists()) {
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadHistory(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessionsFile = java.io.File(context.filesDir, "sessions.json")
                if (!sessionsFile.exists()) return@launch

                val jsonStr = sessionsFile.readText()
                val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SessionItem::class.java)
                val adapter = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter<List<SessionItem>>(type)
                val sessionItems = adapter.fromJson(jsonStr) ?: emptyList()

                val loadedItems = sessionItems.mapNotNull { session ->
                    val bitmap = loadBitmapFromFile(context, session.id)
                    if (bitmap != null) {
                        HistoryItem(
                            id = session.id,
                            prompt = session.prompt,
                            bitmap = bitmap,
                            storageUri = session.storageUri,
                            model = session.model,
                            timestamp = session.timestamp,
                            durationMs = session.durationMs,
                            tokensUsed = session.tokensUsed,
                            calculatedCost = session.calculatedCost
                        )
                    } else null
                }

                withContext(Dispatchers.Main) {
                    _history.value = loadedItems
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveHistory(context: Context, items: List<HistoryItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessionItems = items.map { item ->
                    SessionItem(
                        id = item.id,
                        prompt = item.prompt,
                        storageUri = item.storageUri,
                        model = item.model,
                        timestamp = item.timestamp,
                        durationMs = item.durationMs,
                        tokensUsed = item.tokensUsed,
                        calculatedCost = item.calculatedCost
                    )
                }

                val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, SessionItem::class.java)
                val adapter = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter<List<SessionItem>>(type)
                val jsonStr = adapter.toJson(sessionItems)

                val sessionsFile = java.io.File(context.filesDir, "sessions.json")
                sessionsFile.writeText(jsonStr)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generateImage(context: Context, apiKey: String, prompt: String, model: String, ratio: String, size: String) {
        if (prompt.trim().isEmpty()) {
            _uiState.value = UiState.Error("Prompt description cannot be empty. Please paint your thoughts in words first.")
            return
        }

        if (apiKey.trim().isEmpty()) {
            _uiState.value = UiState.Error("API Access Key is required. Please secure your credential key in Settings under AI Settings first.")
            return
        }

        _uiState.value = UiState.Generating
        val startTime = System.currentTimeMillis()

        viewModelScope.launch {
            try {
                val req = GeminiRequest(
                    contents = listOf(
                        ContentPartList(
                            parts = listOf(Part(text = prompt))
                        )
                    ),
                    generationConfig = GenerationConfig(
                        imageConfig = ImageConfig(
                            aspectRatio = ratio,
                            imageSize = size
                        )
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.geminiApi.generateContent(
                        model = model,
                        apiKey = apiKey,
                        request = req
                    )
                }

                // Parse generated content candidate
                val contentParts = response.candidates?.firstOrNull()?.content?.parts
                val inlinePart = contentParts?.firstOrNull { it.inlineData != null && it.inlineData.data != null }
                val textPart = contentParts?.firstOrNull { it.text != null }

                if (inlinePart?.inlineData?.data != null) {
                    val base64Data = inlinePart.inlineData.data!!
                    val bitmap = withContext(Dispatchers.IO) {
                        StorageUtils.base64ToBitmap(base64Data)
                    }

                    if (bitmap != null) {
                        // Secure MediaStore Gallery writing under Scoped storage policies
                        val savedUriStr = withContext(Dispatchers.IO) {
                            StorageUtils.saveImageToGallery(context, bitmap, prompt)
                        }

                        val durationMs = System.currentTimeMillis() - startTime
                        val tokensUsed = (prompt.length / 4) + 4
                        val inputCost = (tokensUsed.toDouble() / 1000.0) * 0.000075
                        val baseImageCost = when (model) {
                            "gemini-3.1-flash-image-preview" -> when (size) {
                                "512px" -> 0.02
                                "1K" -> 0.035
                                else -> 0.05
                            }
                            else -> when (size) {
                                "512px" -> 0.015
                                "1K" -> 0.03
                                else -> 0.045
                            }
                        }
                        val calculatedCost = inputCost + baseImageCost

                        val newItemId = UUID.randomUUID().toString()
                        withContext(Dispatchers.IO) {
                            saveBitmapToFile(context, newItemId, bitmap)
                        }

                        _uiState.value = UiState.Success(
                            bitmap = bitmap,
                            prompt = prompt,
                            storageUri = savedUriStr,
                            model = model,
                            durationMs = durationMs,
                            tokensUsed = tokensUsed,
                            calculatedCost = calculatedCost
                        )

                        // Update history feed list in local cache memory Catalog
                        val newItem = HistoryItem(
                            id = newItemId,
                            prompt = prompt,
                            bitmap = bitmap,
                            storageUri = savedUriStr,
                            model = model,
                            timestamp = System.currentTimeMillis(),
                            durationMs = durationMs,
                            tokensUsed = tokensUsed,
                            calculatedCost = calculatedCost
                        )
                        val updatedFeed = listOf(newItem) + _history.value
                        _history.value = updatedFeed
                        saveHistory(context, updatedFeed)
                    } else {
                        _uiState.value = UiState.Error("Pixelation failure: Stream fetched successfully but failed to assemble bitmap bytes cleanly.")
                    }
                } else if (textPart?.text != null) {
                    _uiState.value = UiState.Error("AI returned metadata instead of an image: ${textPart.text}")
                } else {
                    _uiState.value = UiState.Error("Fermion Collision: The Gemini model resolved text but did not dispatch visual bits. Please alter aspects or prompt.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error(e.message ?: "Quantum phase error inside Retrofit channel. Check your network or credentials validity.")
            }
        }
    }

    fun selectHistoryItem(item: HistoryItem) {
        _uiState.value = UiState.Success(
            bitmap = item.bitmap,
            prompt = item.prompt,
            storageUri = item.storageUri,
            model = item.model,
            durationMs = item.durationMs,
            tokensUsed = item.tokensUsed,
            calculatedCost = item.calculatedCost
        )
    }

    fun clearState() {
        _uiState.value = UiState.Idle
    }
}

// --- USER INTERFACE COMPOSABLES ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITeamScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    
    // Core preferences state flowing
    val geminiApiKeySaved by repository.geminiApiKey.collectAsState(initial = "")
    val aiModelPreset by repository.aiModel.collectAsState(initial = "gemini-2.5-flash-image")
    val aiRatioPreset by repository.aiRatio.collectAsState(initial = "1:1")
    val aiSizePreset by repository.aiSize.collectAsState(initial = "1K")

    // Instantiating ViewModel for single-screen operation scoping
    val viewModel = remember { ImageGeneratorViewModel(repository) }
    val uiState by viewModel.uiState.collectAsState()
    val historyFeed by viewModel.history.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHistory(context)
    }

    // Prompt studio states
    var promptInputText by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    // Layout configuration curves mapping 
    val CardCurve = RoundedCornerShape(26.dp)
    val InputCurve = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeepOnyx
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            
            // 🌟 TOP HEADER BAR SLOTS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                            contentDescription = "Open Sidebar Navigation",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = LavenderTint,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = OnPrimaryPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Lumina AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "GEMINI CANVAS ENGINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = LavenderTint
                        )
                    }
                }

                // Quick clean toggle button back home
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(20.dp),
                    color = SlatePurple,
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 🍱 BENTO COMPOSABLE FRAMES COLUMN
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // Bento Grid Frame 2: Creative Art Studio (Prompt Engineering Panel)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardCurve,
                    colors = CardDefaults.cardColors(containerColor = SlatePurple),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎨 Lumina Design Studio",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Small row displaying current model, ratio and resolution sizing configuration presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presetText = "Preset: Model: " + (if (aiModelPreset == "gemini-2.5-flash-image") "Standard" else "High-Detail") + " | " + aiRatioPreset + " | " + aiSizePreset
                            Text(
                                text = presetText,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = LavenderTint,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large detailed instructions text field box
                        OutlinedTextField(
                            value = promptInputText,
                            onValueChange = { promptInputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            placeholder = { Text("What visual masterworks shall we synthesize today? Describe styles, materials, palettes, or environments...") },
                            shape = InputCurve,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LavenderTint,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sparkly active create button
                        val isGenerating = uiState is UiState.Generating
                        Button(
                            onClick = {
                                viewModel.generateImage(
                                    context = context,
                                    apiKey = geminiApiKeySaved,
                                    prompt = promptInputText,
                                    model = aiModelPreset,
                                    ratio = aiRatioPreset,
                                    size = aiSizePreset
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !isGenerating,
                            shape = InputCurve,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LavenderTint,
                                contentColor = OnPrimaryPurple,
                                disabledContainerColor = LavenderTint.copy(alpha = 0.5f),
                                disabledContentColor = OnPrimaryPurple.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = OnPrimaryPurple,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "PIGMENTING PIXELS...",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            } else {
                                Text(
                                    text = "⭐ SYNTHESIZE MASTERPIECE",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                        }

                        if (geminiApiKeySaved.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Gemini API Key is missing. Please configure your developer API Key in Settings first.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Bento Grid Frame 3: Masterpiece Visual Canvas Render Panel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardCurve,
                    colors = CardDefaults.cardColors(containerColor = SlatePurple),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🖼️ Masterpiece Canvas Frame",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Unified UI State Visual Rendering Box wrapper
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(
                                    when (selectedRatioStr) {
                                        "16:9" -> 16f / 9f
                                        "9:16" -> 9f / 16f
                                        "4:3" -> 4f / 3f
                                        "3:4" -> 3f / 4f
                                        else -> 1f
                                    }
                                )
                                .clip(RoundedCornerShape(26.dp))
                                .background(DeepOnyx),
                            contentAlignment = Alignment.Center
                        ) {
                            when (val state = uiState) {
                                is UiState.Idle -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Text("⚛️", fontSize = 42.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Art Studio Idle",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Formulate a creative description in the studio panel and fire the generator to render canvas pixels.",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                                is UiState.Generating -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = LavenderTint)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Aligning neural nodes...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Charging $aiModelPreset",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LavenderTint
                                        )
                                    }
                                }
                                is UiState.Success -> {
                                    Image(
                                        bitmap = state.bitmap.asImageBitmap(),
                                        contentDescription = "AI Generated Artwork",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                is UiState.Error -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        Text("⚠️", fontSize = 36.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        SelectionContainer {
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { viewModel.clearState() },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = LavenderContainer)
                                        ) {
                                            Text("Clear Diagnostic Message", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Overlay contextual details for generated SUCCESS visual canvas state
                        val successState = uiState as? UiState.Success
                        AnimatedVisibility(
                            visible = successState != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            if (successState != null) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    // Notification Checkbox "Stored successfully in Gallery"
                                    if (successState.storageUri != null) {
                                        Surface(
                                            color = Color(0xFF1B5E20),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("✅", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Stored successfully in Device Gallery!\nPath: Pictures/GeminiCanvas",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Descriptive Prompt & Info
                                    Surface(
                                        color = DeepOnyx,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, BorderColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "Active Prompt: \"${successState.prompt}\"",
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Engine profile output on ${successState.model}",
                                                fontSize = 11.sp,
                                                color = LavenderTint,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))
                                            Divider(color = BorderColor)
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = "📊 Request Performance Metrics & Billables",
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Generation Time:", fontSize = 11.sp, color = Color.Gray)
                                                Text("${(successState.durationMs / 100.0) / 10.0}s", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Estimated Prompt Tokens:", fontSize = 11.sp, color = Color.Gray)
                                                Text("${successState.tokensUsed} tokens", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Estimated Cost:", fontSize = 11.sp, color = Color.Gray)
                                                Text(String.format("$%.5f", successState.calculatedCost), fontSize = 11.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Action Share Anchor Row
                                    Button(
                                        onClick = {
                                            StorageUtils.shareImage(
                                                context = context,
                                                bitmap = successState.bitmap,
                                                prompt = successState.prompt
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = InputCurve,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LavenderContainer,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("✈️ SHARE ARTWORK CHANNELS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bento Grid Frame 4: Horizontal scrolling session history logs/Highlights
                AnimatedVisibility(visible = historyFeed.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = CardCurve,
                        colors = CardDefaults.cardColors(containerColor = SlatePurple),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🕒 Session Highlights Gallery",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(historyFeed) { item ->
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(DeepOnyx)
                                            .border(
                                                BorderStroke(
                                                    2.dp,
                                                    if ((uiState as? UiState.Success)?.prompt == item.prompt) LavenderTint else Color.Transparent
                                                ),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable { viewModel.selectHistoryItem(item) },
                                        contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    bitmap = item.bitmap.asImageBitmap(),
                                                    contentDescription = "History preview",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
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
