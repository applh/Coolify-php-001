package com.example.cameraxapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    val themeMode by repository.themeMode.collectAsState(initial = 0)
    val lensFacing by repository.defaultLensFacing.collectAsState(initial = 1)
    val flashMode by repository.defaultFlashMode.collectAsState(initial = 2)
    val storageLocation by repository.storageLocation.collectAsState(initial = 0)
    val videoQuality by repository.videoQuality.collectAsState(initial = 4)
    val enableAudio by repository.enableAudio.collectAsState(initial = true)

    val hasSdCard = ContextCompat.getExternalFilesDirs(context, null).size > 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
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
        }
    }
}
