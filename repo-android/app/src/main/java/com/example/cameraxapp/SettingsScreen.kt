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
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.Locale
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.text.font.FontWeight

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
    val startupDefaultRoute by repository.startupDefaultRoute.collectAsState(initial = "hub")
    val cameraExtension by repository.cameraExtension.collectAsState(initial = 0)
    val concurrentStream by repository.concurrentStream.collectAsState(initial = false)
    val proControlMode by repository.proControlMode.collectAsState(initial = false)
    val proIsoValue by repository.proIsoValue.collectAsState(initial = 0)
    val proExposureCompValue by repository.proExposureCompValue.collectAsState(initial = 0)
    val offlineScanHud by repository.offlineScanHud.collectAsState(initial = true)
    val scannerService by repository.scannerService.collectAsState(initial = 0)
    val imageSaveFormat by repository.imageSaveFormat.collectAsState(initial = 0)
    val videoContainerFormat by repository.videoContainerFormat.collectAsState(initial = 0)
    val mapDefaultLat by repository.mapDefaultLatitude.collectAsState(initial = 48.8566)
    val mapDefaultLng by repository.mapDefaultLongitude.collectAsState(initial = 2.3522)
    val mapDefaultZoom by repository.mapDefaultZoom.collectAsState(initial = 12.0f)
    val mapLastLayerType by repository.mapLastLayerType.collectAsState(initial = 1)
    val mapEngineType by repository.mapEngineType.collectAsState(initial = 0)
    val googleMapsApiKey by repository.googleMapsApiKey.collectAsState(initial = "")

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showGoogleMapsKeyDialog by remember { mutableStateOf(false) }
    var showGalleryNameDialog by remember { mutableStateOf(false) }
    var showStartAppletDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showMapCoordsDialog by remember { mutableStateOf(false) }

    // Collapsible states
    var appearanceExpanded by remember { mutableStateOf(false) }
    var aiSettingsExpanded by remember { mutableStateOf(false) }
    var cameraSettingsExpanded by remember { mutableStateOf(false) }
    var storageExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }
    var mappingExpanded by remember { mutableStateOf(false) }
    var launcherExpanded by remember { mutableStateOf(false) }

    val hasSdCard = ContextCompat.getExternalFilesDirs(context, null).size > 1

    // Hardware camera settings collected once at top level to avoid state loss on collapse/expand
    var cameraCount by remember { mutableStateOf(0) }
    var maxZoomSupported by remember { mutableStateOf(1f) }
    var flashSupported by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val infos = cameraProvider.availableCameraInfos
            cameraCount = infos.size
            if (infos.isNotEmpty()) {
                val primaryInfo = infos[0]
                primaryInfo.zoomState.value?.let {
                    maxZoomSupported = it.maxZoomRatio
                }
                flashSupported = primaryInfo.hasFlashUnit()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Launcher / applets configuration lifted to top level to avoid state loss
    val launcherActiveAppletsStr by repository.launcherActiveApplets.collectAsState(initial = "")

    val activeRoutes = remember(launcherActiveAppletsStr) {
        if (launcherActiveAppletsStr.isEmpty()) {
            listOf("camera", "files", "ai_team", "cronjobs", "db", "agenda", "wallpaper", "backup", "settings", "browser").toSet()
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
            "files" to "Files Applet",
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

    val configurableApplets = remember {
        listOf(
            "camera" to "Camera Screen",
            "files" to "Files Applet",
            "ai_team" to "AI Workspace",
            "cronjobs" to "Cron Task Management",
            "db" to "SQLite Database Inspector",
            "agenda" to "Agenda & Calendar Scheduler",
            "wallpaper" to "Wallpaper Auto-Rotation",
            "backup" to "Backup Manager",
            "browser" to "Sandbox Web Browser"
        )
    }

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

            // Appearance Collapsible Panel
            CollapsibleSection(
                title = "Appearance Settings",
                expanded = appearanceExpanded,
                onToggle = { appearanceExpanded = !appearanceExpanded }
            ) {
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
            }

            // AI settings Collapsible Panel
            CollapsibleSection(
                title = "AI Engine Parameters",
                expanded = aiSettingsExpanded,
                onToggle = { aiSettingsExpanded = !aiSettingsExpanded }
            ) {
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
            }

            // Camera Settings Collapsible Panel
            CollapsibleSection(
                title = "Camera Core & Video Settings",
                expanded = cameraSettingsExpanded,
                onToggle = { cameraSettingsExpanded = !cameraSettingsExpanded }
            ) {
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

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Advanced Camera Capabilities", 
                    style = MaterialTheme.typography.titleSmall, 
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                ListItem(
                    headlineContent = { Text("Device OEM Extension") },
                    supportingContent = { 
                        Text(when(cameraExtension) {
                            0 -> "Disabled (Normal ImageCapture)"
                            1 -> "HDR Mode (High Dynamic Range)"
                            2 -> "Portrait Mode (Bokeh Background Depth)"
                            3 -> "Night Mode (Low-Light Intensity Boost)"
                            4 -> "Face Retouch Mode (Digital Smoothing)"
                            else -> "Disabled"
                        })
                    },
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            repository.setCameraExtension((cameraExtension + 1) % 5)
                        }
                    }
                )

                ListItem(
                    headlineContent = { Text("Concurrent Dual-Cam Layout") },
                    supportingContent = { Text(if (concurrentStream) "Active (Picture-in-Picture Stream)" else "Inactive (Single Sensor Focus)") },
                    trailingContent = {
                        Switch(
                            checked = concurrentStream,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { repository.setConcurrentStream(checked) }
                            }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("Pro Mode Control Console") },
                    supportingContent = { Text(if (proControlMode) "Manual ISO & EV Controls Active" else "Auto-Exposure & Sensitivity") },
                    trailingContent = {
                        Switch(
                            checked = proControlMode,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { repository.setProControlMode(checked) }
                            }
                        )
                    }
                )

                if (proControlMode) {
                    ListItem(
                        headlineContent = { Text("Manual Exposure Value (EV)") },
                        supportingContent = { 
                            Text(if (proExposureCompValue == 0) "Normal (0 EV)" else if (proExposureCompValue > 0) "+${proExposureCompValue} EV" else "${proExposureCompValue} EV") 
                        },
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                val nextEv = if (proExposureCompValue >= 3) -3 else proExposureCompValue + 1
                                repository.setProExposureCompValue(nextEv)
                            }
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Manual Sensor Sensitivity (ISO)") },
                        supportingContent = { 
                            Text(when(proIsoValue) {
                                0 -> "Auto ISO"
                                100 -> "ISO 100 (Sunlight)"
                                200 -> "ISO 200 (Overcast)"
                                400 -> "ISO 400 (Indoors)"
                                800 -> "ISO 800 (Dim Light)"
                                1600 -> "ISO 1600 (Night)"
                                else -> "Auto ISO"
                            })
                        },
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                val nextIso = when(proIsoValue) {
                                    0 -> 100
                                    100 -> 200
                                    200 -> 400
                                    400 -> 800
                                    800 -> 1600
                                    else -> 0
                                }
                                repository.setProIsoValue(nextIso)
                            }
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text("Offline QR/Barcode Scanner HUD") },
                    supportingContent = { Text(if (offlineScanHud) "Green Neon Bracket Guides Active" else "Minimal Clean Frame View") },
                    trailingContent = {
                        Switch(
                            checked = offlineScanHud,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { repository.setOfflineScanHud(checked) }
                            }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("Document Scanner Engine") },
                    supportingContent = {
                        Text(when(scannerService) {
                            0 -> "Engine A (Local Contours & Perspective Correction)"
                            1 -> "Engine B (Play Services Document Scanner)"
                            else -> "Engine A (Local Contours)"
                        })
                    },
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            repository.setScannerService((scannerService + 1) % 2)
                        }
                    }
                )

                ListItem(
                    headlineContent = { Text("Image Output Save Format") },
                    supportingContent = {
                        Text(when(imageSaveFormat) {
                            0 -> "Compressed JPEG"
                            1 -> "Lossless PNG"
                            2 -> "Modern WebP"
                            else -> "Compressed JPEG"
                        })
                    },
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            repository.setImageSaveFormat((imageSaveFormat + 1) % 3)
                        }
                    }
                )

                ListItem(
                    headlineContent = { Text("Video Output Container Format") },
                    supportingContent = {
                        Text(when(videoContainerFormat) {
                            0 -> "Standard MP4 (H.264)"
                            1 -> "Multi-codec MKV"
                            2 -> "Optimized WebM"
                            else -> "Standard MP4"
                        })
                    },
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            repository.setVideoContainerFormat((videoContainerFormat + 1) % 3)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                val runtime = Runtime.getRuntime()
                val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)
                val availableMemoryMB = runtime.freeMemory() / (1024 * 1024)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Hardware & Camera Capabilities Catalog",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text("• Camera Sensors Discovered: ${cameraCount} active lenses", style = MaterialTheme.typography.bodySmall)
                        Text("• Hardware Flash Support: ${if (flashSupported) "Available" else "Not Detected / Locked"}", style = MaterialTheme.typography.bodySmall)
                        Text("• Native Max Optical Zoom: ${String.format(Locale.US, "%.1fx", maxZoomSupported)}", style = MaterialTheme.typography.bodySmall)
                        Text("• Target OS API Level: SDK ${android.os.Build.VERSION.SDK_INT}", style = MaterialTheme.typography.bodySmall)
                        Text("• Assigned VM Memory Budget: ${maxMemoryMB} MB (Free: ${availableMemoryMB} MB)", style = MaterialTheme.typography.bodySmall)
                        Text("• Active Sub-routines: CameraX Preview, Auto-Focus, Audio-Muxer, ML Scan Kernel", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Storage Configuration Collapsible Panel
            CollapsibleSection(
                title = "Storage Configuration",
                expanded = storageExpanded,
                onToggle = { storageExpanded = !storageExpanded }
            ) {
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
            }

            // Backup & Restore Collapsible Panel
            CollapsibleSection(
                title = "Backup & Restore Preferences",
                expanded = backupExpanded,
                onToggle = { backupExpanded = !backupExpanded }
            ) {
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
            }

            // Mapping Preferences Collapsible Panel
            CollapsibleSection(
                title = "Mapping Preferences Settings",
                expanded = mappingExpanded,
                onToggle = { mappingExpanded = !mappingExpanded }
            ) {
                ListItem(
                    headlineContent = { Text("Default Map Center") },
                    supportingContent = { Text("Coordinates: $mapDefaultLat, $mapDefaultLng") },
                    modifier = Modifier.clickable {
                        showMapCoordsDialog = true
                    }
                )

                ListItem(
                    headlineContent = { Text("Default Zoom Level") },
                    supportingContent = { Text("Zoom Ratio: $mapDefaultZoom") },
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            val nextZoom = if (mapDefaultZoom >= 18f) 8f else mapDefaultZoom + 2f
                            repository.setMapDefaultZoom(nextZoom)
                        }
                    }
                )

                ListItem(
                    headlineContent = { Text("Default Layer Style") },
                    supportingContent = {
                        Text(when(mapLastLayerType) {
                            1 -> "Normal Road Map Layer"
                            2 -> "Satellite High-Resolution Image Layer"
                            3 -> "Hybrid Label Overlay Layer"
                            else -> "Normal Road Map Layer"
                        })
                    },
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            val nextStyle = (mapLastLayerType % 3) + 1
                            repository.setMapLastLayerType(nextStyle)
                        }
                    }
                )

                ListItem(
                    headlineContent = { Text("Map Rendering Engine") },
                    supportingContent = {
                        Text(when(mapEngineType) {
                            0 -> "OpenStreetMap (Leaflet Webview)"
                            1 -> "Google Maps (API Webview)"
                            else -> "OpenStreetMap (Leaflet Webview)"
                        })
                    },
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            val nextEngine = if (mapEngineType == 0) 1 else 0
                            repository.setMapEngineType(nextEngine)
                        }
                    }
                )

                ListItem(
                    headlineContent = { Text("Google Maps API Key") },
                    supportingContent = { Text(if (googleMapsApiKey.isEmpty()) "Not set" else "********" + googleMapsApiKey.takeLast(4)) },
                    modifier = Modifier.clickable {
                        showGoogleMapsKeyDialog = true
                    }
                )
            }

            // Launcher & Desktop UX Customization Collapsible Panel
            CollapsibleSection(
                title = "Launcher & Desktop UX Customization",
                expanded = launcherExpanded,
                onToggle = { launcherExpanded = !launcherExpanded }
            ) {
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
                Text(
                    text = "Active Applet Visibilities", 
                    style = MaterialTheme.typography.titleSmall, 
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.secondary
                )

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

        if (showGoogleMapsKeyDialog) {
            var tempKey by remember { mutableStateOf(googleMapsApiKey) }
            AlertDialog(
                onDismissRequest = { showGoogleMapsKeyDialog = false },
                title = { Text("Set Google Maps API Key") },
                text = {
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("Google Maps API Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch { repository.setGoogleMapsApiKey(tempKey) }
                        showGoogleMapsKeyDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoogleMapsKeyDialog = false }) {
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

        if (showMapCoordsDialog) {
            var tempLatTxt by remember { mutableStateOf(mapDefaultLat.toString()) }
            var tempLngTxt by remember { mutableStateOf(mapDefaultLng.toString()) }
            AlertDialog(
                onDismissRequest = { showMapCoordsDialog = false },
                title = { Text("Set Fallback Map Location") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tempLatTxt,
                            onValueChange = { tempLatTxt = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = tempLngTxt,
                            onValueChange = { tempLngTxt = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val lat = tempLatTxt.toDoubleOrNull() ?: 48.8566
                        val lng = tempLngTxt.toDoubleOrNull() ?: 2.3522
                        coroutineScope.launch { repository.setMapDefaultCoordinates(lat, lng) }
                        showMapCoordsDialog = false
                    }) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMapCoordsDialog = false }) {
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
                    "files" to "Files Applet",
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

@Composable
fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    content = content
                )
            }
        }
    }
}
