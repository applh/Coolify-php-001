package com.example.cameraxapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.FileProvider
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

enum class SortOption { DATE_DESC, DATE_ASC, NAME_ASC, SIZE_DESC }
enum class ExplorerCategory { ALL, IMAGES, VIDEOS, DOCUMENTS, VAULT }

// Fast, symmetric, and robust byte-level stream obfuscator for physical file assets
object SimpleCryptor {
    private val KEY = "FRAISE_LOCK_SECRET_KEY_123_SECURE_VAULT".toByteArray()

    fun transform(source: File, destination: File): Boolean {
        return try {
            if (!source.exists()) return false
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(16384) // 16KB high speed buffer
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        for (i in 0 until bytesRead) {
                            buffer[i] = (buffer[i].toInt() xor KEY[i % KEY.size].toInt()).toByte()
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExplorerScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit, onOpenRightDrawer: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SettingsRepository(context) }
    val storageLocation by repository.storageLocation.collectAsState(initial = 0)
    val vaultPrefs = remember { context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE) }
    
    // Core state managers
    var selectedCategory by remember { mutableStateOf(ExplorerCategory.ALL) }
    var isVaultUnlocked by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // Folder traversal state
    val baseFolder = remember(storageLocation) {
        val dir = when (storageLocation) {
            1 -> context.getExternalFilesDir(null) ?: context.filesDir
            2 -> {
                val dirs = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
                if (dirs.size > 1) dirs[1] else context.filesDir
            }
            else -> context.filesDir
        }
        // Guarantee default folder hierarchy
        File(dir, "Pictures").mkdirs()
        File(dir, "Movies").mkdirs()
        File(dir, "Documents").mkdirs()
        dir
    }
    
    var currentFolder by remember(baseFolder) { mutableStateOf(baseFolder) }
    val vaultFolder = remember { File(context.filesDir, "private_vault").apply { mkdirs() } }

    // Floating Custom Notification State (Avoid window native alerts blockings)
    var notificationMessage by remember { mutableStateOf<String?>(null) }
    val triggerNotification = { msg: String ->
        notificationMessage = msg
        scope.launch {
            kotlinx.coroutines.delay(3000)
            if (notificationMessage == msg) {
                notificationMessage = null
            }
        }
    }

    // Refresh state trigger
    var refreshCounter by remember { mutableStateOf(0) }

    // Helper functions for file operations
    val getRecursiveFiles = { folder: File, extensions: List<String> ->
        val result = mutableListOf<File>()
        fun walk(dir: File) {
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.isDirectory) {
                    if (f.name != "private_vault" && !f.name.startsWith(".")) {
                        walk(f)
                    }
                } else {
                    if (extensions.contains(f.extension.lowercase())) {
                        result.add(f)
                    }
                }
            }
        }
        walk(folder)
        result
    }

    // Load active folder/category file sets
    val currentDirectoryItems = remember(currentFolder, selectedCategory, refreshCounter, storageLocation) {
        if (selectedCategory == ExplorerCategory.ALL) {
            val allInDir = currentFolder.listFiles()?.toList() ?: emptyList()
            val validFiles = allInDir.filter { 
                it.isDirectory || it.extension.lowercase() in listOf("jpg", "png", "jpeg", "mp4", "txt", "md", "json")
            }
            // Sort folder first, then sort alphabetically/by date
            validFiles.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } else if (selectedCategory == ExplorerCategory.IMAGES) {
            getRecursiveFiles(baseFolder, listOf("jpg", "png", "jpeg"))
        } else if (selectedCategory == ExplorerCategory.VIDEOS) {
            getRecursiveFiles(baseFolder, listOf("mp4"))
        } else if (selectedCategory == ExplorerCategory.DOCUMENTS) {
            getRecursiveFiles(baseFolder, listOf("txt", "md", "json"))
        } else { // VAULT
            vaultFolder.listFiles()?.toList() ?: emptyList()
        }
    }

    // Selection indicators
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedFiles = remember { mutableStateListOf<File>() }
    
    // Clear selection helpers
    val clearSelection = {
        isSelectionMode = false
        selectedFiles.clear()
    }

    // Temporary storage for decrypted previews inside the secure vault
    var tempDecryptedPreviewFile by remember { mutableStateOf<File?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }

    // Dialog state
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    // Filter by search terms and sort choice
    val displayedFiles = remember(currentDirectoryItems, searchQuery, sortOption, refreshCounter) {
        currentDirectoryItems.filter { fileOrDir ->
            val dispName = if (selectedCategory == ExplorerCategory.VAULT) {
                try {
                    val base64Name = fileOrDir.name.removePrefix("vault_").removeSuffix(".enc")
                    val decodedBytes = android.util.Base64.decode(base64Name, android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
                    String(decodedBytes)
                } catch (e: Exception) {
                    fileOrDir.name
                }
            } else {
                fileOrDir.name
            }
            dispName.contains(searchQuery, ignoreCase = true)
        }.let { list ->
            // Filter directories vs files for proper sort mapping
            val dirs = list.filter { it.isDirectory }
            val files = list.filter { !it.isDirectory }
            
            val sortedFiles = when (sortOption) {
                SortOption.DATE_DESC -> files.sortedByDescending { it.lastModified() }
                SortOption.DATE_ASC -> files.sortedBy { it.lastModified() }
                SortOption.NAME_ASC -> files.sortedBy { it.name.lowercase() }
                SortOption.SIZE_DESC -> files.sortedByDescending { it.length() }
            }
            
            // Directories always stay first in the file tree system directory
            dirs + sortedFiles
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDualPane = maxWidth > 600.dp

        // Fullscreen media preview pane (single pane representation)
        if (selectedFile != null && !isDualPane) {
            FullScreenMedia(
                file = selectedFile!!,
                onClose = {
                    tempDecryptedPreviewFile?.delete()
                    tempDecryptedPreviewFile = null
                    selectedFile = null
                },
                onDelete = {
                    it.delete()
                    refreshCounter++
                    tempDecryptedPreviewFile?.delete()
                    tempDecryptedPreviewFile = null
                    selectedFile = null
                    triggerNotification("Item permanently deleted")
                }
            )
            return@BoxWithConstraints
        }

        Scaffold(
            bottomBar = {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("${selectedFiles.size} selected") },
                        navigationIcon = {
                            IconButton(onClick = { clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        },
                        actions = {
                            // File action options based on state categories
                            if (selectedCategory == ExplorerCategory.VAULT) {
                                // Decrypt / Unlock vault actions
                                IconButton(onClick = {
                                    scope.launch {
                                        var count = 0
                                        selectedFiles.forEach { vaultFile ->
                                            try {
                                                val originalName = try {
                                                    val base64Name = vaultFile.name.removePrefix("vault_").removeSuffix(".enc")
                                                    val decodedBytes = android.util.Base64.decode(base64Name, android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
                                                    String(decodedBytes)
                                                } catch (e: Exception) {
                                                    vaultFile.name
                                                }
                                                val desFolder = when {
                                                    originalName.lowercase().endsWith(".mp4") -> File(baseFolder, "Movies").apply { mkdirs() }
                                                    originalName.lowercase().endsWith(".jpg") || originalName.lowercase().endsWith(".png") || originalName.lowercase().endsWith(".jpeg") -> File(baseFolder, "Pictures").apply { mkdirs() }
                                                    else -> File(baseFolder, "Documents").apply { mkdirs() }
                                                }
                                                val decryptedRestored = File(desFolder, originalName)
                                                if (SimpleCryptor.transform(vaultFile, decryptedRestored)) {
                                                    vaultFile.delete()
                                                    count++
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                        refreshCounter++
                                        clearSelection()
                                        triggerNotification("Successfully decrypted & unlocked $count files")
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Decrypt & Unlock",
                                        modifier = Modifier.graphicsLayer(rotationZ = 180f)
                                    )
                                }
                            } else {
                                // Encrypt / Lock action
                                IconButton(onClick = {
                                    scope.launch {
                                        var count = 0
                                        // Filter selected files (ignore directories for vault locking)
                                        val onlyFiles = selectedFiles.filter { !it.isDirectory }
                                        onlyFiles.forEach { fileItem ->
                                            try {
                                                val originalName = fileItem.name
                                                val base64Name = android.util.Base64.encodeToString(originalName.toByteArray(), android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
                                                val encryptedFileTarget = File(vaultFolder, "vault_${base64Name}.enc")
                                                if (SimpleCryptor.transform(fileItem, encryptedFileTarget)) {
                                                    fileItem.delete()
                                                    count++
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                        refreshCounter++
                                        clearSelection()
                                        triggerNotification("Encrypted & locked $count files in Private Vault")
                                    }
                                }) {
                                    Icon(Icons.Filled.Lock, contentDescription = "Lock in Private Vault")
                                }
                            }

                            // Share selection option (Only works on normal files)
                            IconButton(onClick = {
                                val actionableFiles = selectedFiles.filter { !it.isDirectory }
                                if (actionableFiles.isNotEmpty()) {
                                    val uris = actionableFiles.map { FileProvider.getUriForFile(context, "${context.packageName}.provider", it) }
                                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "*/*"
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Selected Assets"))
                                } else {
                                    triggerNotification("Cannot share directories")
                                }
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share")
                            }

                            // Delete selected items recursively
                            IconButton(onClick = {
                                selectedFiles.forEach { fileOrDir ->
                                    fun deleteRecursive(item: File) {
                                        if (item.isDirectory) {
                                            item.listFiles()?.forEach { deleteRecursive(it) }
                                        }
                                        item.delete()
                                    }
                                    deleteRecursive(fileOrDir)
                                }
                                refreshCounter++
                                clearSelection()
                                triggerNotification("Deleted selected files permanently")
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                } else if (isSearchActive) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search files or directories...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { 
                                isSearchActive = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Search")
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { Text("Explorer & Vault") },
                        navigationIcon = {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Sort")
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Date (Newest first)") },
                                        onClick = { sortOption = SortOption.DATE_DESC; showSortMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Date (Oldest first)") },
                                        onClick = { sortOption = SortOption.DATE_ASC; showSortMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Name (A-Z)") },
                                        onClick = { sortOption = SortOption.NAME_ASC; showSortMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Size (Largest first)") },
                                        onClick = { sortOption = SortOption.SIZE_DESC; showSortMenu = false }
                                    )
                                }
                            }
                            IconButton(onClick = onOpenRightDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "Quick Tools")
                            }
                        }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Selector row of high impact categories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("📁 Tree Files", ExplorerCategory.ALL, Icons.AutoMirrored.Filled.List),
                            Triple("🖼️ Images", ExplorerCategory.IMAGES, Icons.Default.Star),
                            Triple("🎥 Videos", ExplorerCategory.VIDEOS, Icons.Default.PlayArrow),
                            Triple("📄 Documents", ExplorerCategory.DOCUMENTS, Icons.Default.Create),
                            Triple("🔒 Private Vault", ExplorerCategory.VAULT, Icons.Default.Lock)
                        ).forEach { (label, category, iconVec) ->
                            val isChosen = selectedCategory == category
                            FilterChip(
                                selected = isChosen,
                                onClick = {
                                    clearSelection()
                                    selectedCategory = category
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = iconVec, 
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    // 2. Active Vault security challenge blocker
                    if (selectedCategory == ExplorerCategory.VAULT && !isVaultUnlocked) {
                        PrivateVaultPasscodeScreen(
                            vaultPrefs = vaultPrefs,
                            onUnlocked = {
                                isVaultUnlocked = true
                                triggerNotification("Private security vault unlocked successfully")
                            }
                        )
                    } else {
                        // 3. Navigation path indicator and Folder Operations for category ALL
                        if (selectedCategory == ExplorerCategory.ALL) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val isAtBase = currentFolder.absolutePath == baseFolder.absolutePath
                                    IconButton(
                                        onClick = {
                                            val parent = currentFolder.parentFile
                                            if (parent != null && currentFolder.absolutePath != baseFolder.absolutePath) {
                                                currentFolder = parent
                                            }
                                        },
                                        enabled = !isAtBase,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, modifier = Modifier.graphicsLayer(rotationZ = 90f),
                                            contentDescription = "Navigate Up",
                                            tint = if (isAtBase) Color.Gray else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val dispPath = currentFolder.absolutePath.replace(baseFolder.absolutePath, "Home")
                                    Text(
                                        text = dispPath,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 1
                                    )
                                }
                                
                                // Quick folder creation option
                                TextButton(
                                    onClick = { showCreateFolderDialog = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Folder", fontSize = 12.sp)
                                }
                            }
                        }

                        // 4. File layout display screen
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                if (displayedFiles.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (searchQuery.isNotEmpty()) "No matching files" else "This workspace is empty", 
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 108.dp),
                                        contentPadding = PaddingValues(6.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(displayedFiles) { file ->
                                            val isSelected = selectedFiles.contains(file)
                                            val isDirectory = file.isDirectory
                                            
                                            // Handle private filenames decrypted titles
                                            val originalFilenameLabel = remember(file, refreshCounter) {
                                                if (selectedCategory == ExplorerCategory.VAULT) {
                                                    try {
                                                        val base64Name = file.name.removePrefix("vault_").removeSuffix(".enc")
                                                        val decodedBytes = android.util.Base64.decode(base64Name, android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
                                                        String(decodedBytes)
                                                    } catch (e: Exception) {
                                                        file.name
                                                    }
                                                } else {
                                                    file.name
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .padding(4.dp)
                                                    .aspectRatio(1f)
                                                    .background(
                                                        when {
                                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                            isDualPane && selectedFile == file -> MaterialTheme.colorScheme.secondaryContainer
                                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        },
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .combinedClickable(
                                                        onClick = {
                                                            if (isSelectionMode) {
                                                                if (isSelected) selectedFiles.remove(file)
                                                                else selectedFiles.add(file)
                                                                if (selectedFiles.isEmpty()) isSelectionMode = false
                                                            } else {
                                                                if (isDirectory) {
                                                                    currentFolder = file
                                                                } else {
                                                                    // Open file preview
                                                                    if (selectedCategory == ExplorerCategory.VAULT) {
                                                                        try {
                                                                            val tempPreview = File(context.cacheDir, "temp_vault_${System.currentTimeMillis()}_${originalFilenameLabel}")
                                                                            if (SimpleCryptor.transform(file, tempPreview)) {
                                                                                tempDecryptedPreviewFile = tempPreview
                                                                                selectedFile = tempPreview
                                                                            } else {
                                                                                triggerNotification("Failed to decrypt secure preview file")
                                                                            }
                                                                        } catch (e: Exception) {
                                                                            e.printStackTrace()
                                                                            triggerNotification("Error loading vault item preview")
                                                                        }
                                                                    } else {
                                                                        selectedFile = file
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onLongClick = {
                                                            if (!isSelectionMode) {
                                                                isSelectionMode = true
                                                                selectedFiles.add(file)
                                                            }
                                                        }
                                                    )
                                            ) {
                                                if (isDirectory) {
                                                    // Folder display
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.List,
                                                                contentDescription = "Folder",
                                                                tint = Color(0xFFFBC02D), // Golden Sand
                                                                modifier = Modifier.size(52.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = file.name,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                textAlign = TextAlign.Center,
                                                                maxLines = 1,
                                                                modifier = Modifier.padding(horizontal = 4.dp)
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    // File display options
                                                    val isImg = file.extension.lowercase() in listOf("jpg", "png", "jpeg") || 
                                                               originalFilenameLabel.lowercase().endsWith(".jpg") || 
                                                               originalFilenameLabel.lowercase().endsWith(".png") ||
                                                               originalFilenameLabel.lowercase().endsWith(".jpeg")
                                                    
                                                    val isVid = file.extension.lowercase() == "mp4" || 
                                                               originalFilenameLabel.lowercase().endsWith(".mp4")

                                                    if (isImg && selectedCategory != ExplorerCategory.VAULT) {
                                                        Image(
                                                            painter = rememberAsyncImagePainter(model = file),
                                                            contentDescription = originalFilenameLabel,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else if (isVid) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color.Black.copy(alpha = 0.4f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.PlayArrow,
                                                                contentDescription = "Video",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(36.dp)
                                                            )
                                                        }
                                                    } else {
                                                        // Fallback icon for textures, documents or encrypted vault files
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = if (selectedCategory == ExplorerCategory.VAULT) Icons.Default.Lock else Icons.Default.Create,
                                                                contentDescription = "Document/File",
                                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                modifier = Modifier.size(36.dp)
                                                            )
                                                        }
                                                    }

                                                    // Overlay text label strip
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomStart)
                                                            .fillMaxWidth()
                                                            .background(Color.Black.copy(alpha = 0.6f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        val sizeRaw = file.length()
                                                        val sizeStr = if (sizeRaw < 1024) "$sizeRaw B" else if (sizeRaw < 1024 * 1024) "${sizeRaw / 1024} KB" else "${sizeRaw / (1024 * 1024)} MB"
                                                        Text(
                                                            text = "$originalFilenameLabel\n$sizeStr",
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                            maxLines = 2,
                                                            lineHeight = 10.sp
                                                        )
                                                    }
                                                }

                                                // Highlight indicator
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CheckCircle,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Dual Pane layout rendering support for tablets
                            if (isDualPane && selectedFile != null) {
                                Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                                    FullScreenMedia(
                                        file = selectedFile!!,
                                        onClose = {
                                            tempDecryptedPreviewFile?.delete()
                                            tempDecryptedPreviewFile = null
                                            selectedFile = null
                                        },
                                        onDelete = {
                                            it.delete()
                                            refreshCounter++
                                            tempDecryptedPreviewFile?.delete()
                                            tempDecryptedPreviewFile = null
                                            selectedFile = null
                                            triggerNotification("Item permanently deleted")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Custom notification floating state overlay (Avoid direct native window alerts blocking)
                notificationMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .background(Color.DarkGray, shape = RoundedCornerShape(24.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(text = msg, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // Interactive custom folder name builder dialog
    if (showCreateFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        val target = File(currentFolder, newFolderName)
                        if (!target.exists()) {
                            if (target.mkdir()) {
                                refreshCounter++
                                triggerNotification("Folder \"$newFolderName\" created successfully")
                            } else {
                                triggerNotification("Failed to create folder")
                            }
                        } else {
                            triggerNotification("A folder with this name already exists")
                        }
                    }
                    showCreateFolderDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Security PIN Passcode verification screen
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PrivateVaultPasscodeScreen(
    vaultPrefs: android.content.SharedPreferences,
    onUnlocked: () -> Unit
) {
    var savedPin by remember { mutableStateOf(vaultPrefs.getString("vault_pin_hash", "")) }
    val isSetupMode = savedPin.isNullOrBlank()
    
    var typedCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var confirmAttemptNeeded by remember { mutableStateOf(false) }
    var referenceSetupCheckPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = if (isSetupMode) {
                if (confirmAttemptNeeded) "🔐 Confirm Private Access PIN" else "🔒 Setup Private Access PIN"
            } else {
                "🔒 Secured Private Documents Vault"
            },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isSetupMode) {
                if (confirmAttemptNeeded) "Re-enter the 4-digit PIN code to verify" else "Set up a 4-digit numeric PIN to protect records"
            } else {
                "Enter passcode to open the encrypted storage"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Access dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val filled = typedCode.length > i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Visual numeric keypad grid
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "⌫")
        val chunkedKeys = keys.chunked(3)
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            chunkedKeys.forEach { rowKeys ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowKeys.forEach { currentKey ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        errorMessage = ""
                                        if (currentKey == "C") {
                                            typedCode = ""
                                        } else if (currentKey == "⌫") {
                                            if (typedCode.isNotEmpty()) {
                                                typedCode = typedCode.dropLast(1)
                                            }
                                        } else {
                                            if (typedCode.length < 4) {
                                                typedCode += currentKey
                                                if (typedCode.length == 4) {
                                                    // Digit completed verification triggers
                                                    if (isSetupMode) {
                                                        if (!confirmAttemptNeeded) {
                                                            referenceSetupCheckPin = typedCode
                                                            confirmAttemptNeeded = true
                                                            typedCode = ""
                                                        } else {
                                                            if (typedCode == referenceSetupCheckPin) {
                                                                vaultPrefs.edit().putString("vault_pin_hash", typedCode).apply()
                                                                savedPin = typedCode
                                                                onUnlocked()
                                                            } else {
                                                                errorMessage = "Passcodes do not match! Restarting PIN setup..."
                                                                confirmAttemptNeeded = false
                                                                referenceSetupCheckPin = ""
                                                                typedCode = ""
                                                            }
                                                        }
                                                    } else {
                                                        if (typedCode == savedPin) {
                                                            onUnlocked()
                                                        } else {
                                                            errorMessage = "Incorrect PIN! Please try again"
                                                            typedCode = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentKey,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenMedia(
    file: File,
    onClose: () -> Unit,
    onDelete: (File) -> Unit
) {
    val context = LocalContext.current
    val isImg = file.extension.lowercase() in listOf("jpg", "png", "jpeg")
    val isVid = file.extension.lowercase() == "mp4"
    val isDoc = file.extension.lowercase() in listOf("txt", "md", "json")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isImg) {
                // Pinch to Zoom Interactive Image Viewer
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                Image(
                    painter = rememberAsyncImagePainter(model = file),
                    contentDescription = file.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = if (scale > 1f) offset + pan else androidx.compose.ui.geometry.Offset.Zero
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            } else if (isVid) {
                // Media3 ExoPlayer Video Player
                val exoPlayer = remember(file) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                        prepare()
                        playWhenReady = true
                    }
                }
                DisposableEffect(exoPlayer) {
                    onDispose {
                        exoPlayer.release()
                    }
                }
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isDoc) {
                // Plain Text Document Reader Panel
                val textContent = remember(file) {
                    try {
                        file.readText()
                    } catch (e: Exception) {
                        "Failed to load content: ${e.localizedMessage}"
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = textContent,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            } else {
                Text("Preview not supported for this extension type", color = Color.Gray, fontSize = 14.sp)
            }
        }

        // Header Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Text(
                text = file.name,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { onDelete(file) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }

        // Bottom Details Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp)
        ) {
            val size = file.length()
            val sizeStr = if (size < 1024) "$size B" else if (size < 1024 * 1024) "${size / 1024} KB" else "${size / (1024 * 1024)} MB"
            Text(
                text = "Size: $sizeStr | Location: ${file.parentFile?.name}",
                color = Color.LightGray,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
