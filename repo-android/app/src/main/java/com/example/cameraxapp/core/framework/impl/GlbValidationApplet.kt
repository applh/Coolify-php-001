package com.example.cameraxapp.core.framework.impl

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.cameraxapp.AppLogger
import com.example.cameraxapp.DebugLogEntry
import com.example.cameraxapp.core.framework.Applet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.math.Rotation

class GlbValidationApplet : Applet {
    override val id: String = "glb_validation"
    override val name: String = "GLB Validation"
    override val description: String = "Minimalist SceneView robot model validation and diagnostics."
    override val icon = Icons.Default.Build

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(
        navController: NavController,
        onOpenDrawer: () -> Unit,
        onOpenRightDrawer: () -> Unit
    ) {
        val context = LocalContext.current
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        val coroutineScope = rememberCoroutineScope()

        // 3D parameters states
        var scaleState by remember { mutableStateOf(0.7f) }
        var autoRotateState by remember { mutableStateOf(true) }

        // Live Logs states
        var logs by remember { mutableStateOf<List<DebugLogEntry>>(emptyList()) }
        var filterText by remember { mutableStateOf("") }
        var selectedLevel by remember { mutableStateOf("ALL") }

        // Fetch logs periodically to keep them real-time
        LaunchedEffect(Unit) {
            while (true) {
                logs = AppLogger.getLogs()
                delay(1200)
            }
        }

        // Configuration checks to split layout smoothly
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "GLB Viewer Validation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "SceneView framework tester & real-time logs debugger",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                        }
                    },
                    actions = {
                        IconButton(onClick = { AppLogger.clear() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Clear Logs")
                        }
                        IconButton(onClick = onOpenRightDrawer) {
                            Icon(Icons.Default.Settings, contentDescription = "Quick Tools")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (isLandscape) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // 3D View Screen
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            ThreeDViewPanel(
                                modifier = Modifier.fillMaxSize(),
                                scaleState = scaleState,
                                autoRotateState = autoRotateState,
                                onScaleChanged = { scaleState = it },
                                onAutoRotateToggle = { autoRotateState = !autoRotateState }
                            )
                        }

                        // Divider Line
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        // Logs Panel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            LogsPanelViewer(
                                logs = logs,
                                filterText = filterText,
                                selectedLevel = selectedLevel,
                                onFilterTextChange = { filterText = it },
                                onLevelSelected = { selectedLevel = it },
                                onClearLogs = {
                                    AppLogger.clear()
                                    logs = emptyList()
                                },
                                onForceRefresh = { logs = AppLogger.getLogs() }
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 3D View Screen (Top Half)
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            ThreeDViewPanel(
                                modifier = Modifier.fillMaxSize(),
                                scaleState = scaleState,
                                autoRotateState = autoRotateState,
                                onScaleChanged = { scaleState = it },
                                onAutoRotateToggle = { autoRotateState = !autoRotateState }
                            )
                        }

                        // Divider Line
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        // Logs Panel (Bottom Half)
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxWidth()
                        ) {
                            LogsPanelViewer(
                                logs = logs,
                                filterText = filterText,
                                selectedLevel = selectedLevel,
                                onFilterTextChange = { filterText = it },
                                onLevelSelected = { selectedLevel = it },
                                onClearLogs = {
                                    AppLogger.clear()
                                    logs = emptyList()
                                },
                                onForceRefresh = { logs = AppLogger.getLogs() }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ThreeDViewPanel(
        modifier: Modifier,
        scaleState: Float,
        autoRotateState: Boolean,
        onScaleChanged: (Float) -> Unit,
        onAutoRotateToggle: () -> Unit
    ) {
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val model = rememberModelInstance(modelLoader, "models/robot_expressive.glb")
        
        var rotationY by remember { mutableStateOf(0f) }
        
        LaunchedEffect(autoRotateState) {
            while (autoRotateState) {
                rotationY = (rotationY + 1.0f) % 360f
                delay(16)
            }
        }

        Box(modifier = modifier.background(Color(0xFF121214))) {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                cameraManipulator = rememberCameraManipulator()
            ) {
                model?.let {
                    ModelNode(
                        modelInstance = it,
                        scaleToUnits = scaleState,
                        rotation = Rotation(x = 0f, y = rotationY, z = 0f)
                    )
                }
            }

            // Dynamic HUD Info Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color(0xBB000000), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "TARGET: robot_expressive.glb (SceneView 4.17.0)",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (model != null) Color.Green else Color.Yellow)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (model != null) "Success! Model Loaded." else "Loading local robot GLB...",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick Controller Tools (Zoom controls and Rotate toggle)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .background(Color(0xDD202024), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Scale Label & Tweak buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Scale: ${String.format(Locale.US, "%.1fy", scaleState)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    IconButton(
                        onClick = { onScaleChanged((scaleState - 0.1f).coerceAtLeast(0.1f)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", color = Color.Cyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    IconButton(
                        onClick = { onScaleChanged((scaleState + 0.1f).coerceAtMost(2.5f)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", color = Color.Cyan, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Divider Line
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(Color.DarkGray)
                )

                // Rotation Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAutoRotateToggle() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (autoRotateState) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = "Rotation state",
                        tint = if (autoRotateState) Color.Green else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (autoRotateState) "AUTO-ROTATING" else "PAUSED",
                        color = if (autoRotateState) Color.Green else Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @Composable
    private fun LogsPanelViewer(
        logs: List<DebugLogEntry>,
        filterText: String,
        selectedLevel: String,
        onFilterTextChange: (String) -> Unit,
        onLevelSelected: (String) -> Unit,
        onClearLogs: () -> Unit,
        onForceRefresh: () -> Unit
    ) {
        val listState = rememberLazyListState()
        val filteredLogs = remember(logs, filterText, selectedLevel) {
            logs.filter { log ->
                val levelMatches = selectedLevel == "ALL" || log.level.equals(selectedLevel, ignoreCase = true)
                val textMatches = filterText.isBlank() || 
                        log.tag.contains(filterText, ignoreCase = true) || 
                        log.message.contains(filterText, ignoreCase = true)
                levelMatches && textMatches
            }
        }

        // Auto-scroll to top (newest logs) when logs count changes
        LaunchedEffect(filteredLogs.size) {
            if (filteredLogs.isNotEmpty()) {
                listState.animateScrollToItem(0)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1015))
                .padding(8.dp)
        ) {
            // Header Action controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Diagnostics Logs icon",
                        tint = Color.Cyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE SYSTEM OUTPUT",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onForceRefresh,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Enforce Refresh logs command",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Wipe debug registers",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Search filtering box
            OutlinedTextField(
                value = filterText,
                onValueChange = onFilterTextChange,
                placeholder = { Text("Filter keywords, tasks, or exceptions...", fontSize = 11.sp, color = Color.Gray) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Cyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedContainerColor = Color(0xFF1E2129),
                    unfocusedContainerColor = Color(0xFF16181F),
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Level filters Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val levels = remember { listOf("ALL", "DEBUG", "INFO", "WARN", "ERROR") }
                levels.forEach { level ->
                    val isSelected = selectedLevel == level
                    val levelBgColor = when (level) {
                        "ERROR" -> Color(0x33EF5350)
                        "WARN" -> Color(0x33FFCA28)
                        "INFO" -> Color(0x3326A69A)
                        "DEBUG" -> Color(0x3390A4AE)
                        else -> Color(0x3300E5FF)
                    }
                    val levelTextColor = when (level) {
                        "ERROR" -> Color(0xFFEF5350)
                        "WARN" -> Color(0xFFFFCA28)
                        "INFO" -> Color(0xFF26A69A)
                        "DEBUG" -> Color(0xFF90A4AE)
                        else -> Color(0xFF00E5FF)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) levelTextColor else levelBgColor)
                            .clickable { onLevelSelected(level) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = level,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else levelTextColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Monospace lines Terminal list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF07080B))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    Text(
                        text = "--- No matching real-time logs found in buffer ---",
                        color = Color.DarkGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredLogs) { log ->
                            LogLineItem(log = log)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LogLineItem(log: DebugLogEntry) {
        val levelColor = when (log.level.uppercase()) {
            "ERROR" -> Color(0xFFEF5350)
            "WARN" -> Color(0xFFFFCA28)
            "INFO" -> Color(0xFF26A69A)
            "DEBUG" -> Color(0xFF90A4AE)
            else -> Color(0xFFEEEEEE)
        }

        val df = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
        val timeStr = remember(log.timestamp) { df.format(Date(log.timestamp)) }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "[$timeStr]",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = "[${log.level.uppercase()}]",
                    color = levelColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = "${log.tag}:",
                    color = Color.Cyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = log.message,
                    color = Color(0xFFECEFF1),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 11.sp
                )
            }
            if (!log.stackTrace.isNullOrBlank()) {
                Text(
                    text = log.stackTrace,
                    color = Color(0xFFFF8A80),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                )
            }
        }
    }
}
