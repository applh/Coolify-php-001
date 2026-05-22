package com.example.cameraxapp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import android.graphics.BitmapFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { AgendaDatabaseHelper(context) }
    var cronJob by remember { mutableStateOf<CronJobInfo?>(null) }
    var images by remember { mutableStateOf<List<File>>(emptyList()) }
    var currentWallpaper by remember { mutableStateOf<File?>(null) } // This could just be the list of images.

    // Refresh state
    fun refreshData() {
        cronJob = dbHelper.getAllCronJobs().find { it.name.contains("Wallpaper", ignoreCase = true) }
        val imageDir = File(context.getExternalFilesDir(null), "wallpapers")
        if (!imageDir.exists()) imageDir.mkdirs()
        images = imageDir.listFiles { file -> 
            file.isFile && (file.extension.equals("jpg", true) || file.extension.equals("png", true)) 
        }?.toList() ?: emptyList()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val imageDir = File(context.getExternalFilesDir(null), "wallpapers")
                if (!imageDir.exists()) imageDir.mkdirs()
                
                val newFile = File(imageDir, "wallpaper_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(newFile)
                
                inputStream?.copyTo(outputStream)
                
                inputStream?.close()
                outputStream.close()
                refreshData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallpaper Rotator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Wallpaper")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Auto-Rotator Engine", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active (Every 15 mins)")
                        Switch(
                            checked = cronJob?.isActive == true,
                            onCheckedChange = { active ->
                                cronJob?.let { job ->
                                    dbHelper.updateCronStatus(job.id, active, job.lastRunMillis, job.status)
                                    // Make sure WorkManager is updated if active
                                    if (active) {
                                        CronScheduler.scheduleExact(context, job.id, 15)
                                    }
                                    refreshData()
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Status: ${cronJob?.status ?: "Unknown"} (Last run: ${if (cronJob?.lastRunMillis == 0L) "Never" else "Recently"})",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("Wallpaper Library (${images.size} items)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Text("Tap + to add images from your device.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            
            if (images.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No wallpapers found.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { image ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                // Manual set
                                val wallpaperManager = android.app.WallpaperManager.getInstance(context)
                                val bitmap = BitmapFactory.decodeFile(image.absolutePath)
                                if (bitmap != null) {
                                    wallpaperManager.setBitmap(bitmap)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val bitmap = BitmapFactory.decodeFile(image.absolutePath)
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = image.name,
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.size(64.dp).background(Color.Gray, RoundedCornerShape(8.dp)))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(image.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("${image.length() / 1024} KB", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = {
                                    if (image.exists()) {
                                        image.delete()
                                        refreshData()
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
