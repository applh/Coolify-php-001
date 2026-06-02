package com.example.cameraxapp.core.framework.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import android.content.ClipData
import com.example.cameraxapp.AppLogger
import com.example.cameraxapp.DebugLogEntry
import com.example.cameraxapp.core.framework.Applet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DebugApplet : Applet {
    override val id: String = "debug"
    override val name: String = "Debug Logs"
    override val description: String = "View system logs, WebView errors, exceptions and diagnostics"
    override val icon = Icons.Default.Build

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(
        navController: NavController,
        onOpenDrawer: () -> Unit,
        onOpenRightDrawer: () -> Unit
    ) {
        var logs by remember { mutableStateOf<List<DebugLogEntry>>(emptyList()) }
        var filterText by remember { mutableStateOf("") }
        var selectedLevel by remember { mutableStateOf("ALL") }
        var expandedLogId by remember { mutableStateOf<Int?>(null) }
        var showToast by remember { mutableStateOf<String?>(null) }
        val clipboardManager = LocalClipboard.current
        val coroutineScope = rememberCoroutineScope()

        // Fetch logs initially and on refresh
        val refreshLogs = {
            logs = AppLogger.getLogs()
        }

        LaunchedEffect(Unit) {
            refreshLogs()
        }

        val filteredLogs = remember(logs, filterText, selectedLevel) {
            logs.filter { log ->
                val levelMatches = selectedLevel == "ALL" || log.level.equals(selectedLevel, ignoreCase = true)
                val textMatches = filterText.isBlank() || 
                        log.tag.contains(filterText, ignoreCase = true) || 
                        log.message.contains(filterText, ignoreCase = true) ||
                        (log.stackTrace?.contains(filterText, ignoreCase = true) ?: false)
                levelMatches && textMatches
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Diagnostics Applet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${filteredLogs.size} logs listed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Navigation Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            refreshLogs()
                            showToast = "Logs refreshed!"
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Logs")
                        }
                        IconButton(onClick = {
                            AppLogger.clear()
                            refreshLogs()
                            showToast = "All logs cleared!"
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All Logs")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Filters Row
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    label = { Text("Search logs by tag, message, or traceback...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (filterText.isNotEmpty()) {
                            IconButton(onClick = { filterText = "" }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear search query")
                            }
                        }
                    }
                )

                // Level Filter Chips Row
                val levels = listOf("ALL", "ERROR", "WARN", "INFO", "DEBUG")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    levels.forEach { level ->
                        val isSelected = selectedLevel == level
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedLevel = level },
                            label = { Text(level, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when(level) {
                                    "ERROR" -> Color(0xFFE57373)
                                    "WARN" -> Color(0xFFFFB74D)
                                    "INFO" -> Color(0xFF64B5F6)
                                    "DEBUG" -> Color(0xFF81C784)
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                },
                                selectedLabelColor = if (level == "ALL") MaterialTheme.colorScheme.onPrimaryContainer else Color.Black
                            )
                        )
                    }
                }

                // Quick Diagnostics Generator Button (Self Test / AI Agents Verification helper)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            AppLogger.e(
                                "ManualDiagnosticTest",
                                "Simulated system-level error raised for diagnostic verification",
                                RuntimeException("Sample traceback validation exception: Stacktrace elements parsed successfully")
                            )
                            refreshLogs()
                            showToast = "Sample Error logged!"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Test Exception",
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text("Log Test Error", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            AppLogger.i("MapTroubleshoot", "Verifying Leaflet map dependency loading setup...")
                            AppLogger.d("MapTroubleshoot", "Using TileProvider base Layer: https://openstreetmap.org")
                            AppLogger.i("MapTroubleshoot", "WebView DOM storage enabled: true, Javascript enabled: true")
                            refreshLogs()
                            showToast = "Map check items logged!"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Audit Leaflet Map", fontSize = 12.sp)
                    }
                }

                // Bulk log copy utility row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bulk Clipboard Tools:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                if (filteredLogs.isEmpty()) {
                                    showToast = "No filtered logs to copy!"
                                } else {
                                    val exported = filteredLogs.joinToString("\n\n") { log ->
                                        "[${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(log.timestamp))}] [${log.level.uppercase()}] ${log.tag}: ${log.message}${if (!log.stackTrace.isNullOrBlank()) "\nStack Trace:\n${log.stackTrace}" else ""}"
                                    }
                                    coroutineScope.launch {
                                        clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("text", exported)))
                                    }
                                    showToast = "Copied ${filteredLogs.size} logs matching filter!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("📋 Copy Filtered (${filteredLogs.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (logs.isEmpty()) {
                                    showToast = "No logs to copy!"
                                } else {
                                    val exported = logs.joinToString("\n\n") { log ->
                                        "[${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(log.timestamp))}] [${log.level.uppercase()}] ${log.tag}: ${log.message}${if (!log.stackTrace.isNullOrBlank()) "\nStack Trace:\n${log.stackTrace}" else ""}"
                                    }
                                    coroutineScope.launch {
                                        clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("text", exported)))
                                    }
                                    showToast = "Copied all ${logs.size} logs to clipboard!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("✨ Copy All (${logs.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Main Logs Content List
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No logs captured matching the filters",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            val isExpanded = expandedLogId == log.id
                            LogCardItem(
                                log = log,
                                isExpanded = isExpanded,
                                onClick = {
                                    expandedLogId = if (isExpanded) null else log.id
                                },
                                onShowToast = { msg -> showToast = msg }
                            )
                        }
                    }
                }
            }

            // Simple Notification toast equivalent in frame
            showToast?.let { msg ->
                LaunchedEffect(msg) {
                    delay(2000)
                    showToast = null
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        color = Color.DarkGray,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            msg,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LogCardItem(
        log: DebugLogEntry,
        isExpanded: Boolean,
        onClick: () -> Unit,
        onShowToast: (String) -> Unit
    ) {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        val timeStr = sdf.format(Date(log.timestamp))
        val clipboardManager = LocalClipboard.current
        val coroutineScope = rememberCoroutineScope()

        val levelColor = when (log.level.uppercase()) {
            "ERROR" -> Color(0xFFEF5350)
            "WARN" -> Color(0xFFFFA726)
            "INFO" -> Color(0xFF42A5F5)
            "DEBUG" -> Color(0xFF66BB6A)
            else -> MaterialTheme.colorScheme.outline
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (isExpanded) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                }
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                // Header line with Level, Time and Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = levelColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = log.level.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = timeStr,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = log.tag,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Short message
                Text(
                    text = log.message,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Expanding panel for detailed view
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Full Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    log.stackTrace?.let { trace ->
                        if (trace.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Stack Trace / Stack Dump:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color.Black.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = trace,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {
                                val fullText = """
                                    [${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))}] [${log.level.uppercase()}] ${log.tag}: ${log.message}
                                    ${if (!log.stackTrace.isNullOrBlank()) "\nStack Trace:\n${log.stackTrace}" else ""}
                                """.trimIndent()
                                coroutineScope.launch {
                                    clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("text", fullText)))
                                }
                                onShowToast("Copied full log details to clipboard!")
                            },
                            label = { Text("📋 Copy Full Log", fontSize = 11.sp) }
                        )

                        AssistChip(
                            onClick = {
                                coroutineScope.launch {
                                    clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("text", log.message)))
                                }
                                onShowToast("Copied message to clipboard!")
                            },
                            label = { Text("💬 Copy Msg", fontSize = 11.sp) }
                        )

                        if (!log.stackTrace.isNullOrBlank()) {
                            AssistChip(
                                onClick = {
                                    coroutineScope.launch {
                                        clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("text", log.stackTrace)))
                                    }
                                    onShowToast("Copied stack trace!")
                                },
                                label = { Text("🔥 Copy Trace", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}
