package com.example.cameraxapp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit, onOpenRightDrawer: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    val themeMode by repository.themeMode.collectAsState(initial = 0)
    val colorTheme by repository.colorTheme.collectAsState(initial = 0)
    val lensFacing by repository.defaultLensFacing.collectAsState(initial = 1)
    val flashMode by repository.defaultFlashMode.collectAsState(initial = 2)
    val storageLocation by repository.storageLocation.collectAsState(initial = 0)
    val videoQuality by repository.videoQuality.collectAsState(initial = 4)
    val enableAudio by repository.enableAudio.collectAsState(initial = true)
    val showCrosshair by repository.showCrosshair.collectAsState(initial = true)
    val showGrid by repository.showGrid.collectAsState(initial = false)
    val gridRows by repository.gridRows.collectAsState(initial = 3)
    val gridColumns by repository.gridColumns.collectAsState(initial = 3)
    val geminiApiKey by repository.geminiApiKey.collectAsState(initial = "")
    val aiModel by repository.aiModel.collectAsState(initial = "gemini-2.5-flash-image")
    val aiRatio by repository.aiRatio.collectAsState(initial = "1:1")
    val aiSize by repository.aiSize.collectAsState(initial = "1K")
    val publicGalleryName by repository.publicGalleryName.collectAsState(initial = "GeminiCanvas")
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showGalleryNameDialog by remember { mutableStateOf(false) }
    var showStartAppletDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val hasSdCard = ContextCompat.getExternalFilesDirs(context, null).size > 1

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                repository.exportSettings(it)
                Toast.makeText(context, "Settings exported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                repository.importSettings(it)
                Toast.makeText(context, "Settings imported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        bottomBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    Row {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenRightDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Quick Tools")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            ListItem(
                headlineContent = { Text("Theme Mode") },
                supportingContent = { 
                    Text(when(themeMode) {
                        0 -> "System Default"
                        1 -> "Light Mode"
                        2 -> "Dark Mode"
                        else -> "Unknown"
                    })
                },
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        repository.setThemeMode((themeMode + 1) % 3)
                    }
                }
            )

            ListItem(
                headlineContent = { Text("App Color Palette") },
                supportingContent = {
                    Text(when(colorTheme) {
                        0 -> "Initial Grayscale & Red Theme"
                        1 -> "Lumina AI (Glowing Aurora) Theme"
                        else -> "Initial Grayscale & Red Theme"
                    })
                },
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        repository.setColorTheme((colorTheme + 1) % 2)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("AI Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Gemini API Key") },
                supportingContent = { Text(if (geminiApiKey.isEmpty()) "Not set" else "********" + geminiApiKey.takeLast(4)) },
                modifier = Modifier.clickable {
                    showApiKeyDialog = true
                }
            )

            ListItem(
                headlineContent = { Text("AI Image Model") },
                supportingContent = {
                    Text(when(aiModel) {
                        "gemini-2.5-flash-image" -> "Standard (Flash 2.5)"
                        "gemini-3.1-flash-image-preview" -> "High-Detail (Flash 3.1)"
                        else -> "Standard (Flash 2.5)"
                    })
                },
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        val nextModel = if (aiModel == "gemini-2.5-flash-image") "gemini-3.1-flash-image-preview" else "gemini-2.5-flash-image"
                        repository.setAiModel(nextModel)
                    }
                }
            )

            ListItem(
                headlineContent = { Text("AI Image Aspect Ratio") },
                supportingContent = { Text(aiRatio) },
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        val ratios = listOf("1:1", "16:9", "4:3", "9:16", "3:4")
                        val nextIndex = (ratios.indexOf(aiRatio) + 1) % ratios.size
                        repository.setAiRatio(ratios[nextIndex])
                    }
                }
            )

            ListItem(
                headlineContent = { Text("AI Image Generation Size") },
                supportingContent = { Text(aiSize) },
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        val sizes = listOf("512px", "1K", "2K", "4K")
                        val nextIndex = (sizes.indexOf(aiSize) + 1) % sizes.size
                        repository.setAiSize(sizes[nextIndex])
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Camera Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Default Lens Facing") },
                supportingContent = { Text(if (lensFacing == 1) "Back Camera" else "Front Camera") },
                modifier = Modifier.clickable {
                    coroutineScope.launch { repository.setDefaultLensFacing(if (lensFacing == 1) 0 else 1) }
                }
            )

            ListItem(
                headlineContent = { Text("Default Flash Mode") },
                supportingContent = { 
                    Text(when(flashMode) {
                        0 -> "Off"
                        1 -> "On"
                        2 -> "Auto"
                        else -> "Auto"
                    })
                },
                modifier = Modifier.clickable {
                    coroutineScope.launch { repository.setDefaultFlashMode((flashMode + 1) % 3) }
                }
            )

            ListItem(
                headlineContent = { Text("Video Quality") },
                supportingContent = { 
                    Text(when(videoQuality) {
                        0 -> "SD"
                        1 -> "HD"
                        2 -> "FHD"
                        3 -> "UHD"
                        else -> "Highest"
                    })
                },
                modifier = Modifier.clickable {
                    coroutineScope.launch { repository.setVideoQuality((videoQuality + 1) % 5) }
                }
            )

            ListItem(
                headlineContent = { Text("Record Audio") },
                supportingContent = { Text(if (enableAudio) "Enabled" else "Disabled") },
                modifier = Modifier.clickable {
                    coroutineScope.launch { repository.setEnableAudio(!enableAudio) }
                }
            )

            ListItem(
                headlineContent = { Text("Display Crosshair & Horizon") },
                supportingContent = { Text(if (showCrosshair) "Visible" else "Hidden") },
                modifier = Modifier.clickable {
                    coroutineScope.launch { repository.setShowCrosshair(!showCrosshair) }
                }
            )

            ListItem(
                headlineContent = { Text("Display Grid") },
                supportingContent = { Text(if (showGrid) "Visible" else "Hidden") },
                modifier = Modifier.clickable {
                    coroutineScope.launch { repository.setShowGrid(!showGrid) }
                }
            )

            if (showGrid) {
                ListItem(
                    headlineContent = { Text("Grid Lines (Rows)") },
                    supportingContent = { Text("$gridRows") },
                    modifier = Modifier.clickable {
                        coroutineScope.launch { 
                            val nextRows = if (gridRows >= 5) 2 else gridRows + 1
                            repository.setGridRows(nextRows) 
                        }
                    }
                )

                ListItem(
                    headlineContent = { Text("Grid Lines (Columns)") },
                    supportingContent = { Text("$gridColumns") },
                    modifier = Modifier.clickable {
                        coroutineScope.launch { 
                            val nextCols = if (gridColumns >= 5) 2 else gridColumns + 1
                            repository.setGridColumns(nextCols) 
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Storage Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Storage Location") },
                supportingContent = { 
                    Text(when(storageLocation) {
                        0 -> "Internal App Storage"
                        1 -> "Public Shared Directory"
                        2 -> "SD Card (External Storage)"
                        else -> "Internal App Storage"
                    })
                },
                modifier = Modifier.clickable {
                    val maxVal = if (hasSdCard) 2 else 1
                    coroutineScope.launch { repository.setStorageLocation((storageLocation + 1) % (maxVal + 1)) }
                }
            )

            ListItem(
                headlineContent = { Text("Public Gallery Folder Name") },
                supportingContent = { Text(publicGalleryName) },
                modifier = Modifier.clickable {
                    showGalleryNameDialog = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Backup & Restore", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Export Settings") },
                supportingContent = { Text("Save your preferences to a file") },
                modifier = Modifier.clickable {
                    exportLauncher.launch("settings-backup.json")
                }
            )

            ListItem(
                headlineContent = { Text("Import Settings") },
                supportingContent = { Text("Restore your preferences from a file") },
                modifier = Modifier.clickable {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Launcher & Desktop UX Customization", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            val startupDefaultRoute by repository.startupDefaultRoute.collectAsState(initial = "hub")
            val launcherActiveAppletsStr by repository.launcherActiveApplets.collectAsState(initial = "")

            val activeRoutes = remember(launcherActiveAppletsStr) {
                if (launcherActiveAppletsStr.isEmpty()) {
                    listOf("camera", "explorer", "ai_team", "cronjobs", "db", "agenda", "wallpaper", "backup", "settings", "browser").toSet()
                } else {
                    try {
                        val arr = org.json.JSONArray(launcherActiveAppletsStr)
                        val set = mutableSetOf<String>()
                        for (i in 0 until arr.length()) {
                            set.add(arr.getString(i))
                        }
                        set.add("settings") // Settings is permanently active
                        set
                    } catch (e: Exception) {
                        emptySet()
                    }
                }
            }

            val routeDisplayNames = remember {
                mapOf(
                    "hub" to "Main Launcher Hub",
                    "camera" to "Camera Screen",
                    "explorer" to "File Explorer",
                    "ai_team" to "AI Workspace",
                    "cronjobs" to "Cron Task Management",
                    "db" to "SQLite Database Inspector",
                    "agenda" to "Agenda & Calendar Scheduler",
                    "wallpaper" to "Wallpaper Auto-Rotation",
                    "backup" to "Backup Manager",
                    "settings" to "Global App Settings",
                    "browser" to "Sandbox Web Browser"
                )
            }

            ListItem(
                headlineContent = { Text("Default Startup Screen") },
                supportingContent = { Text("Starts on " + (routeDisplayNames[startupDefaultRoute] ?: "Main Launcher Hub")) },
                modifier = Modifier.clickable {
                    showStartAppletDialog = true
                }
            )

            ListItem(
                headlineContent = { Text("Reset Desktop Layout") },
                supportingContent = { Text("Restore circular applets positions, active items and routing defaults") },
                modifier = Modifier.clickable {
                    showResetConfirmDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Active Applet Visibilities", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 4.dp))

            val configurableApplets = remember {
                listOf(
                    "camera" to "Camera Screen",
                    "explorer" to "File Explorer",
                    "ai_team" to "AI Workspace",
                    "cronjobs" to "Cron Task Management",
                    "db" to "SQLite Database Inspector",
                    "agenda" to "Agenda & Calendar Scheduler",
                    "wallpaper" to "Wallpaper Auto-Rotation",
                    "backup" to "Backup Manager",
                    "browser" to "Sandbox Web Browser"
                )
            }

            configurableApplets.forEach { (route, name) ->
                val isActive = activeRoutes.contains(route)
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text(if (isActive) "Shown on launcher grid" else "Hidden from launcher") },
                    trailingContent = {
                        Switch(
                            checked = isActive,
                            onCheckedChange = { checked ->
                                val newRoutes = activeRoutes.toMutableSet()
                                if (checked) {
                                    newRoutes.add(route)
                                } else {
                                    newRoutes.remove(route)
                                }
                                newRoutes.add("settings") // protect settings
                                val json = org.json.JSONArray(newRoutes.toList())
                                coroutineScope.launch {
                                    repository.setLauncherActiveApplets(json.toString())
                                }
                            }
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showApiKeyDialog) {
            var tempKey by remember { mutableStateOf(geminiApiKey) }
            AlertDialog(
                onDismissRequest = { showApiKeyDialog = false },
                title = { Text("Set Gemini API Key") },
                text = {
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch { repository.setGeminiApiKey(tempKey) }
                        showApiKeyDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApiKeyDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showGalleryNameDialog) {
            var tempName by remember { mutableStateOf(publicGalleryName) }
            AlertDialog(
                onDismissRequest = { showGalleryNameDialog = false },
                title = { Text("Set Public Gallery Folder Name") },
                text = {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Folder Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch { repository.setPublicGalleryName(tempName) }
                        showGalleryNameDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGalleryNameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showStartAppletDialog) {
            val routeDisplayNames = remember {
                mapOf(
                    "hub" to "Main Launcher Hub",
                    "camera" to "Camera Screen",
                    "explorer" to "File Explorer",
                    "ai_team" to "AI Workspace",
                    "cronjobs" to "Cron Task Management",
                    "db" to "SQLite Database Inspector",
                    "agenda" to "Agenda & Calendar Scheduler",
                    "wallpaper" to "Wallpaper Auto-Rotation",
                    "backup" to "Backup Manager",
                    "settings" to "Global App Settings",
                    "browser" to "Sandbox Web Browser"
                )
            }
            AlertDialog(
                onDismissRequest = { showStartAppletDialog = false },
                title = { Text("Select Startup Destination") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        routeDisplayNames.forEach { (route, displayName) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            repository.setStartupDefaultRoute(route)
                                        }
                                        showStartAppletDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = startupDefaultRoute == route,
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.setStartupDefaultRoute(route)
                                        }
                                        showStartAppletDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(displayName, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStartAppletDialog = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = { Text("Reset Desktop Layout") },
                text = { Text("Are you sure you want to reset customized item drag positions, active switches, and startup landing back to default?") },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            repository.resetLauncherConfig()
                            Toast.makeText(context, "Desktop layout reset successfully", Toast.LENGTH_SHORT).show()
                        }
                        showResetConfirmDialog = false
                    }) {
                        Text("Reset Layout", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
