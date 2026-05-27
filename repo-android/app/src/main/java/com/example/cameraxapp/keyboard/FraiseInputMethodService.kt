package com.example.cameraxapp.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.*
import com.example.cameraxapp.AgendaDatabaseHelper
import com.example.cameraxapp.SettingsRepository
import com.example.cameraxapp.ClipboardItem
import com.example.cameraxapp.RetrofitClient
import com.example.cameraxapp.GeminiRequest
import com.example.cameraxapp.ContentPartList
import com.example.cameraxapp.Part
import com.example.cameraxapp.GenerationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FraiseInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val store = ViewModelStore()
    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var dbHelper: AgendaDatabaseHelper
    private lateinit var settingsRepo: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        dbHelper = AgendaDatabaseHelper(applicationContext)
        settingsRepo = SettingsRepository(applicationContext)
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        return ComposeView(this).apply {
            ViewTreeLifecycleOwner.set(this, this@FraiseInputMethodService)
            ViewTreeViewModelStoreOwner.set(this, this@FraiseInputMethodService)
            ViewTreeSavedStateRegistryOwner.set(this, this@FraiseInputMethodService)

            setContent {
                KeyboardRootScreen(
                    dbHelper = dbHelper,
                    settingsRepo = settingsRepo,
                    onKeyPress = { code -> handleKeyPress(code) },
                    onCommitText = { text -> commitText(text) },
                    onActionPress = { performEditorAction() },
                    onGetSelectedText = { getSelectedText() },
                    onReplaceSelection = { text -> replaceSelection(text) },
                    serviceScope = serviceScope
                )
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Check clipboard content on entry to auto-sync items securely into local clipboard SQLite
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text?.toString() ?: ""
                    if (text.isNotEmpty() && text.length < 2000) {
                        serviceScope.launch(Dispatchers.IO) {
                            dbHelper.addClipboardItem(text)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceJob.cancel()
    }

    private fun handleKeyPress(code: Int) {
        val ic = currentInputConnection ?: return
        when (code) {
            -1 -> ic.deleteSurroundingText(1, 0) // Backspace
            else -> ic.commitText(code.toChar().toString(), 1)
        }
    }

    private fun commitText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    private fun getSelectedText(): String {
        val ic = currentInputConnection ?: return ""
        return ic.getSelectedText(0)?.toString() ?: ""
    }

    private fun replaceSelection(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    private fun performEditorAction() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val actionId = info?.actionId ?: EditorInfo.IME_ACTION_NONE
        if (actionId != EditorInfo.IME_ACTION_NONE) {
            ic.performEditorAction(actionId)
        } else {
            ic.commitText("\n", 1)
        }
    }
}

@Composable
fun KeyboardRootScreen(
    dbHelper: AgendaDatabaseHelper,
    settingsRepo: SettingsRepository,
    onKeyPress: (Int) -> Unit,
    onCommitText: (String) -> Unit,
    onActionPress: () -> Unit,
    onGetSelectedText: () -> String,
    onReplaceSelection: (String) -> Unit,
    serviceScope: CoroutineScope
) {
    var shiftOn by remember { mutableStateOf(false) }
    var symbolsOn by remember { mutableStateOf(false) }
    var activeSubPanel by remember { mutableStateOf("none") } // "none", "clipboard", "templates", "ai"
    
    var aiStatusText by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }
    var clipboardItems by remember { mutableStateOf(listOf<ClipboardItem>()) }

    // Read adaptive settings from Datastore flow
    val themeMode by settingsRepo.themeMode.collectAsState(initial = 0)
    val colorTheme by settingsRepo.colorTheme.collectAsState(initial = 0)

    val isLumina = colorTheme == 1
    val backgroundColor = if (isLumina) Color(0xFF0C091A) else Color(0xFF1E1E1E)
    val keyBgOnColor = if (isLumina) Color(0xFF231E3D) else Color(0xFF333333)
    val keyBgOffColor = if (isLumina) Color(0xFF16112C) else Color(0xFF262626)
    val primaryTextColor = if (isLumina) Color(0xFFE0B0FF) else Color(0xFFE0E0E0)
    val accentColor = if (isLumina) Color(0xFF4DEEEA) else Color(0xFFFF5252)

    fun refreshClipboard() {
        serviceScope.launch(Dispatchers.IO) {
            val list = dbHelper.getAllClipboardItems()
            withContext(Dispatchers.Main) {
                clipboardItems = list
            }
        }
    }

    LaunchedEffect(activeSubPanel) {
        if (activeSubPanel == "clipboard") {
            refreshClipboard()
        }
    }

    fun triggerAiRewrite(tone: String) {
        var rawText = onGetSelectedText()
        if (rawText.trim().isEmpty()) {
            aiStatusText = "Copy/select text in the text field first!"
            return
        }

        isAiLoading = true
        aiStatusText = "Fraise AI rewriting..."

        serviceScope.launch {
            try {
                val apiKey = settingsRepo.geminiApiKey.first()
                if (apiKey.trim().isEmpty()) {
                    withContext(Dispatchers.Main) {
                        aiStatusText = "API Key missing! Set it in settings."
                        isAiLoading = false
                    }
                    return@launch
                }

                val promptText = when (tone) {
                    "professional" -> "Format the following text to have a highly professional, polite business tone, keeping it concise. Return ONLY the final formatted text directly, no explanations:\n\n$rawText"
                    "casual" -> "Format the following text to be casual, friendly, and natural. Return ONLY the final formatted text directly, no explanations:\n\n$rawText"
                    "summarize" -> "Rewrite the following text into a simple concise summary. Return ONLY the final summary, no explanations:\n\n$rawText"
                    else -> "Correct the grammar, spelling, and improve clarity of the following text, keeping its meaning. Return ONLY the final text, no explanations:\n\n$rawText"
                }

                val req = GeminiRequest(
                    contents = listOf(
                        ContentPartList(
                            role = "user",
                            parts = listOf(Part(text = promptText))
                        )
                    ),
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

                val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                withContext(Dispatchers.Main) {
                    if (result != null) {
                        onReplaceSelection(result.trim())
                        aiStatusText = "Replaced with AI rewrite! ✨"
                    } else {
                        aiStatusText = "Empty response received."
                    }
                    isAiLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    aiStatusText = "Error: ${e.localizedMessage ?: "Unknown error"}"
                    isAiLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(backgroundColor)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- CO-PILOT UTILITY STRIP ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(backgroundColor)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (activeSubPanel == "none") {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ AI Assist",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(keyBgOffColor, RoundedCornerShape(12.dp))
                            .clickable { activeSubPanel = "ai" }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    Text(
                        text = "📋 Clipboard",
                        color = primaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(keyBgOffColor, RoundedCornerShape(12.dp))
                            .clickable { activeSubPanel = "clipboard" }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    Text(
                        text = "📝 Templates",
                        color = primaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(keyBgOffColor, RoundedCornerShape(12.dp))
                            .clickable { activeSubPanel = "templates" }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "◀ Back to Keypad",
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { activeSubPanel = "none" }
                            .padding(8.dp)
                    )
                    Text(
                        text = when (activeSubPanel) {
                            "ai" -> "🍓 Gemini Writer Co-pilot"
                            "clipboard" -> "📋 SQLite Copy History"
                            else -> "📝 Fast Boilers"
                        },
                        color = primaryTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }
        }

        // --- LOWER MODULE AREA (SCREEN/GRID SPLITTER) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeSubPanel) {
                "ai" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (aiStatusText.isEmpty()) "Highlight text in any text field & choose an operation:" else aiStatusText,
                            color = primaryTextColor,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (isAiLoading) {
                            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { triggerAiRewrite("improve") },
                                    colors = ButtonDefaults.buttonColors(containerColor = keyBgOffColor),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("✨ Refine", color = primaryTextColor, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { triggerAiRewrite("professional") },
                                    colors = ButtonDefaults.buttonColors(containerColor = keyBgOffColor),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("👔 Pro", color = primaryTextColor, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { triggerAiRewrite("casual") },
                                    colors = ButtonDefaults.buttonColors(containerColor = keyBgOffColor),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("☕ Warm", color = primaryTextColor, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { triggerAiRewrite("summarize") },
                                    colors = ButtonDefaults.buttonColors(containerColor = keyBgOffColor),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("📝 Summarize", color = primaryTextColor, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                "clipboard" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp)
                    ) {
                        items(clipboardItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .background(keyBgOffColor, RoundedCornerShape(4.dp))
                                    .clickable { onCommitText(item.content) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (item.content.length > 55) item.content.take(52) + "..." else item.content,
                                    color = primaryTextColor,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "❌",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clickable {
                                            serviceScope.launch(Dispatchers.IO) {
                                                dbHelper.deleteClipboardItem(item.id)
                                                refreshClipboard()
                                            }
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                        if (clipboardItems.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Clipboard is empty. Copy some text system-wide!",
                                        color = primaryTextColor.copy(alpha = 0.5f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
                "templates" -> {
                    val quickBoilers = listOf(
                        "Hi, checking in!",
                        "Thanks, sounds good to me.",
                        "Let's schedule a brief call to sync on this.",
                        "Can you please provide more details?",
                        "Sorry, I am currently out of office right now.",
                        "Appreciate the quick turnaround, great work!",
                        "Could we please follow up on this tomorrow morning?"
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp)
                    ) {
                        items(quickBoilers) { template ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .background(keyBgOffColor, RoundedCornerShape(4.dp))
                                    .clickable { onCommitText(template) }
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = template,
                                    color = primaryTextColor,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                else -> {
                    // Standard soft-key layouts
                    val letters = if (symbolsOn) {
                        listOf(
                            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                            listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/"),
                            listOf("Shift", "=", "*", "\"", "'", ":", ";", "!", "?", "Back"),
                            listOf("ABC", "Space", "Done")
                        )
                    } else {
                        val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
                        val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
                        val row3 = listOf("Shift", "z", "x", "c", "v", "b", "n", "m", "Back")
                        val row4 = listOf("?123", "Space", "Done")
                        listOf(row1, row2, row3, row4)
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        letters.forEachIndexed { rowIndex, rowKeys ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (rowIndex == 1 && !symbolsOn) {
                                    Spacer(modifier = Modifier.width(16.dp))
                                }

                                rowKeys.forEach { key ->
                                    val isSpecialKey = key == "Shift" || key == "Back" || key == "?123" || key == "ABC" || key == "Done" || key == "Space"
                                    val keyBg = if (isSpecialKey) keyBgOnColor else keyBgOffColor
                                    val keyWeight = when (key) {
                                        "Space" -> 4.5f
                                        "Shift", "Back", "?123", "ABC", "Done" -> 1.5f
                                        else -> 1.0f
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(keyWeight)
                                            .padding(horizontal = 2.dp, vertical = 2.dp)
                                            .background(
                                                color = if (key == "Shift" && shiftOn) accentColor else keyBg,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                when (key) {
                                                    "Shift" -> {
                                                        shiftOn = !shiftOn
                                                    }
                                                    "Back" -> {
                                                        onKeyPress(-1)
                                                    }
                                                    "?123" -> {
                                                        symbolsOn = true
                                                    }
                                                    "ABC" -> {
                                                        symbolsOn = false
                                                    }
                                                    "Done" -> {
                                                        onActionPress()
                                                    }
                                                    "Space" -> {
                                                        onKeyPress(' '.toInt())
                                                    }
                                                    else -> {
                                                        val finalChar = if (shiftOn && !symbolsOn) {
                                                            key.uppercase()
                                                        } else {
                                                            key
                                                        }
                                                        onCommitText(finalChar)
                                                        if (shiftOn) {
                                                            shiftOn = false // Simple automatic lowercasing
                                                        }
                                                    }
                                                }
                                            }
                                            .padding(vertical = 11.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (key) {
                                                "Back" -> "⌫"
                                                "Shift" -> "⇧"
                                                "Space" -> "Space"
                                                else -> if (shiftOn && !symbolsOn && !isSpecialKey) key.uppercase() else key
                                            },
                                            color = if (key == "Shift" && shiftOn) backgroundColor else primaryTextColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                if (rowIndex == 1 && !symbolsOn) {
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
