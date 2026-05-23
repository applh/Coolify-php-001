package com.example.cameraxapp

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.List
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
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

data class WallpaperItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val isLocal: Boolean,
    val file: File? = null,
    val documentFile: DocumentFile? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit, onOpenRightDrawer: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { AgendaDatabaseHelper(context) }
    val settingsRepo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    
    var cronJob by remember { mutableStateOf<CronJobInfo?>(null) }
    var images by remember { mutableStateOf<List<WallpaperItem>>(emptyList()) }
    var externalFolderPath by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun refreshData() {
        isLoading = true
        scope.launch {
            cronJob = dbHelper.getAllCronJobs().find { it.name.contains("Wallpaper", ignoreCase = true) }
            
            val newImages = mutableListOf<WallpaperItem>()
            
            // Local files
            val imageDir = File(context.getExternalFilesDir(null), "wallpapers")
            if (!imageDir.exists()) imageDir.mkdirs()
            imageDir.listFiles { f -> 
                f.isFile && (f.extension.equals("jpg", true) || f.extension.equals("png", true)) 
            }?.forEach { f ->
                newImages.add(WallpaperItem(Uri.fromFile(f), f.name, f.length(), true, file = f))
            }
            
            // External folder
            val externalUriString = settingsRepo.wallpaperFolderUri.first()
            if (externalUriString.isNotEmpty()) {
                externalFolderPath = externalUriString
                try {
                    val treeUri = Uri.parse(externalUriString)
                    val docTree = DocumentFile.fromTreeUri(context, treeUri)
                    if (docTree != null && docTree.isDirectory) {
                        docTree.listFiles().forEach { docFile ->
                            if (docFile.isFile && (docFile.type?.startsWith("image/") == true || docFile.name?.endsWith(".jpg", true) == true || docFile.name?.endsWith(".png", true) == true)) {
                                newImages.add(WallpaperItem(docFile.uri, docFile.name ?: "Unknown", docFile.length(), false, documentFile = docFile))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                externalFolderPath = ""
            }
            
            images = newImages
            isLoading = false
        }
    }

    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isLoading = true
                val imageDir = File(context.getExternalFilesDir(null), "wallpapers")
                if (!imageDir.exists()) imageDir.mkdirs()
                
                uris.forEach { uri ->
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val newFile = File(imageDir, "wp_${System.currentTimeMillis()}.jpg")
                        val outputStream = FileOutputStream(newFile)
                        inputStream?.copyTo(outputStream)
                        inputStream?.close()
                        outputStream.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                refreshData()
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, 
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    settingsRepo.setWallpaperFolderUri(uri.toString())
                    refreshData()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        bottomBar = {
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
                    IconButton(onClick = onOpenRightDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Quick Tools")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Add Folder")
                }
                FloatingActionButton(onClick = { multipleImagePickerLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Files")
                }
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
                                    if (active) {
                                        CronScheduler.scheduleExact(context, job.id, 15)
                                    }
                                    cronJob = job.copy(isActive = active)
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
            
            if (externalFolderPath.isNotEmpty()) {
                Text("Syncing from folder: ${Uri.parse(externalFolderPath).lastPathSegment}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Start))
            }
            Text("Tap + to add files or link a folder.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (images.isEmpty()) {
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
                                scope.launch {
                                    try {
                                        val wallpaperManager = android.app.WallpaperManager.getInstance(context)
                                        val inputStream = context.contentResolver.openInputStream(image.uri)
                                        val bitmap = BitmapFactory.decodeStream(inputStream)
                                        if (bitmap != null) {
                                            wallpaperManager.setBitmap(bitmap)
                                        }
                                        inputStream?.close()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var loadedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                                
                                LaunchedEffect(image.uri) {
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(image.uri)
                                        // Use sample size for thumbnail to avoid OOM
                                        val options = BitmapFactory.Options().apply {
                                            inJustDecodeBounds = true
                                            BitmapFactory.decodeStream(inputStream, null, this)
                                        }
                                        inputStream?.close()
                                        
                                        var scale = 1
                                        if (options.outHeight > 128 || options.outWidth > 128) {
                                            scale = Math.pow(2.0, Math.round(Math.log(128.0 / Math.max(options.outHeight, options.outWidth)) / Math.log(0.5)).toDouble()).toInt()
                                        }
                                        
                                        val inputStream2 = context.contentResolver.openInputStream(image.uri)
                                        val options2 = BitmapFactory.Options().apply { inSampleSize = scale }
                                        loadedBitmap = BitmapFactory.decodeStream(inputStream2, null, options2)
                                        inputStream2?.close()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                
                                if (loadedBitmap != null) {
                                    Image(
                                        bitmap = loadedBitmap!!.asImageBitmap(),
                                        contentDescription = image.name,
                                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.size(64.dp).background(Color.Gray, RoundedCornerShape(8.dp)))
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(image.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                    Text("${image.size / 1024} KB" + if(!image.isLocal) " (External)" else "", style = MaterialTheme.typography.bodySmall)
                                }
                                
                                if (image.isLocal) {
                                    IconButton(onClick = {
                                        if (image.file?.exists() == true) {
                                            image.file.delete()
                                            refreshData()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Local File")
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
