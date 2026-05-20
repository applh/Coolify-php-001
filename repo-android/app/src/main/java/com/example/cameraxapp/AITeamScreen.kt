package com.example.cameraxapp

import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(val role: String, val text: String, val isError: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITeamScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val geminiApiKey by repository.geminiApiKey.collectAsState(initial = "")
    val storageLocation by repository.storageLocation.collectAsState(initial = 0)

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var currentPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Team") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (geminiApiKey.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Please set your Gemini API Key in Settings.", color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        val align = if (message.role == "user") androidx.compose.ui.Alignment.End else androidx.compose.ui.Alignment.Start
                        val color = if (message.isError) MaterialTheme.colorScheme.errorContainer
                        else if (message.role == "user") MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start) {
                            Surface(
                                color = color,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = message.text,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = currentPrompt,
                        onValueChange = { currentPrompt = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask the AI team...") },
                        enabled = !isGenerating
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (currentPrompt.isNotBlank() && !isGenerating) {
                                val prompt = currentPrompt
                                currentPrompt = ""
                                isGenerating = true
                                messages = messages + ChatMessage("user", prompt)

                                coroutineScope.launch {
                                    val generativeModel = GenerativeModel(
                                        modelName = "gemini-pro",
                                        apiKey = geminiApiKey
                                    )
                                    try {
                                        val response = withContext(Dispatchers.IO) {
                                            generativeModel.generateContent(prompt)
                                        }
                                        val responseText = response.text ?: "Empty response"
                                        messages = messages + ChatMessage("model", responseText)
                                        
                                        // Save to disk
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
                                                val file = File(rootDir, filename)
                                                val content = "# Prompt: $prompt\n\n$responseText"
                                                file.writeText(content)
                                            }
                                        }
                                        
                                    } catch (e: Exception) {
                                        messages = messages + ChatMessage("model", "Error: ${e.message}", isError = true)
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            }
                        },
                        enabled = currentPrompt.isNotBlank() && !isGenerating && geminiApiKey.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}
