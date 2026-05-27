package com.example.cameraxapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
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
    val triggerNotification: (String) -> Unit = { msg: String ->
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
                it.isDirectory || !it.name.startsWith(".")
            }
            // Sort folder first, then sort alphabetically/by date
            validFiles.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } else if (selectedCategory == ExplorerCategory.IMAGES) {
            getRecursiveFiles(baseFolder, listOf("jpg", "png", "jpeg"))
        } else if (selectedCategory == ExplorerCategory.VIDEOS) {
            getRecursiveFiles(baseFolder, listOf("mp4"))
        } else if (selectedCategory == ExplorerCategory.DOCUMENTS) {
            getRecursiveFiles(baseFolder, listOf("txt", "md", "json", "csv", "xml", "html", "css", "js", "py", "sh"))
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
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<File?>(null) }

    // Toolkit operation state variables
    var isToolkitModeActive by remember { mutableStateOf(false) }
    var showInPlaceCompressSheet by remember { mutableStateOf(false) }
    var showInPlacePdfSheet by remember { mutableStateOf(false) }

    val selectedFilesContainsImages = remember(selectedFiles.size) {
        selectedFiles.any { !it.isDirectory && it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
    }

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

        if (isToolkitModeActive) {
            com.example.cameraxapp.media.ImagePdfScreen(
                baseFolder = baseFolder,
                onBack = { isToolkitModeActive = false },
                triggerNotification = triggerNotification
            )
            return@BoxWithConstraints
        }

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
                },
                onRename = { old, _ ->
                    fileToRename = old
                    showRenameDialog = true
                },
                triggerNotification = triggerNotification
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

                            if (selectedFilesContainsImages) {
                                IconButton(onClick = { showInPlaceCompressSheet = true }) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Compress Images", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { showInPlacePdfSheet = true }) {
                                    Icon(Icons.Filled.Create, contentDescription = "Compile to PDF", tint = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            if (selectedFiles.size == 1) {
                                IconButton(onClick = {
                                    fileToRename = selectedFiles.first()
                                    showRenameDialog = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Rename")
                                }
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
                            IconButton(onClick = { isToolkitModeActive = true }) {
                                Icon(Icons.Default.Build, contentDescription = "Media Toolkit")
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
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Quick folder creation option
                                    TextButton(
                                        onClick = { showCreateFolderDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Folder", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    // Quick file creation option
                                    TextButton(
                                        onClick = { showCreateFileDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("File", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
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
                                        },
                                        onRename = { old, _ ->
                                            fileToRename = old
                                            showRenameDialog = true
                                        },
                                        triggerNotification = triggerNotification
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

    // Dynamic file creation supporting any filename suffix (e.g. notes.md, data.csv)
    if (showCreateFileDialog) {
        var newFileName by remember { mutableStateOf("") }
        var initialFileContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Create New File") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("File Name (e.g., info.md, todo.txt)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = initialFileContent,
                        onValueChange = { initialFileContent = it },
                        label = { Text("Content (Optional)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFileName.isNotBlank()) {
                        val target = File(currentFolder, newFileName)
                        if (!target.exists()) {
                            try {
                                target.writeText(initialFileContent)
                                refreshCounter++
                                triggerNotification("File \"$newFileName\" created successfully")
                            } catch (e: Exception) {
                                triggerNotification("Failed to create file: ${e.localizedMessage}")
                            }
                        } else {
                            triggerNotification("A file with this name already exists")
                        }
                    }
                    showCreateFileDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog (Full Update capability for files and folders)
    if (showRenameDialog && fileToRename != null) {
        var renameNewName by remember(fileToRename) { mutableStateOf(fileToRename!!.name) }
        AlertDialog(
            onDismissRequest = { 
                showRenameDialog = false
                fileToRename = null
            },
            title = { Text("Rename Item") },
            text = {
                OutlinedTextField(
                    value = renameNewName,
                    onValueChange = { renameNewName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val currentRenameFile = fileToRename
                    if (currentRenameFile != null && renameNewName.isNotBlank() && renameNewName != currentRenameFile.name) {
                        val target = File(currentRenameFile.parentFile, renameNewName)
                        if (!target.exists()) {
                            if (currentRenameFile.renameTo(target)) {
                                refreshCounter++
                                triggerNotification("Renamed successfully to \"$renameNewName\"")
                                if (selectedFile == currentRenameFile) {
                                    selectedFile = target
                                }
                            } else {
                                triggerNotification("Rename failed")
                            }
                        } else {
                            triggerNotification("An item with this name already exists")
                        }
                    }
                    showRenameDialog = false
                    fileToRename = null
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRenameDialog = false
                    fileToRename = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // In-Place Image Compression Dialog
    if (showInPlaceCompressSheet) {
        var qualityVal by remember { mutableStateOf(75f) }
        var scaleVal by remember { mutableStateOf(80f) }
        var formatVal by remember { mutableStateOf(com.example.cameraxapp.media.ImageReducerEngine.OutputFormat.JPEG) }
        var isProcessingInPlace by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isProcessingInPlace) showInPlaceCompressSheet = false },
            title = {
                Text(
                    text = "Compress Selected Images",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "${selectedFiles.size} original files selected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Target Format Selectors
                    Text("Target Format", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        com.example.cameraxapp.media.ImageReducerEngine.OutputFormat.values().forEach { fmt ->
                            val isPicked = formatVal == fmt
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { formatVal = fmt },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isPicked) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)),
                                color = if (isPicked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = fmt.name.replace("_", " "),
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPicked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Quality slider input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Quality", style = MaterialTheme.typography.bodySmall)
                        Text("${qualityVal.toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = qualityVal,
                        onValueChange = { qualityVal = it },
                        valueRange = 1f..100f,
                        steps = 99,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Resolution scale slider input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Resolution Scale", style = MaterialTheme.typography.bodySmall)
                        Text("${scaleVal.toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = scaleVal,
                        onValueChange = { scaleVal = it },
                        valueRange = 10f..100f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isProcessingInPlace = true
                        scope.launch {
                            val destFolder = File(baseFolder, "Pictures/Compressed").apply { mkdirs() }
                            var compressedCount = 0
                            var totalSavedBytes = 0L

                            // Only process normal files which have image extensions
                            val imageFilesToProcess = selectedFiles.filter { !it.isDirectory && it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
                            
                            imageFilesToProcess.forEach { fileItem ->
                                try {
                                    val conf = com.example.cameraxapp.media.ImageReducerEngine.CompressionConfig(
                                        quality = qualityVal.toInt(),
                                        scale = scaleVal / 100f,
                                        targetFormat = formatVal
                                    )
                                    val result = com.example.cameraxapp.media.ImageReducerEngine.compressAndResizeImage(
                                        context = context,
                                        inputFile = fileItem,
                                        outputDirectory = destFolder,
                                        config = conf
                                    )
                                    totalSavedBytes += result.savedBytes
                                    compressedCount++
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            refreshCounter++
                            clearSelection()
                            showInPlaceCompressSheet = false
                            isProcessingInPlace = false
                            
                            val savedStr = if (totalSavedBytes < 1024) "$totalSavedBytes Bytes" else if (totalSavedBytes < 1024 * 1024) "${totalSavedBytes / 1024} KB" else "${totalSavedBytes / (1024 * 1024)} MB"
                            triggerNotification("In-place compression of $compressedCount images completed. Saved ~$savedStr!")
                        }
                    },
                    enabled = !isProcessingInPlace
                ) {
                    if (isProcessingInPlace) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Compress")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showInPlaceCompressSheet = false },
                    enabled = !isProcessingInPlace
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // In-Place PDF Compilation Dialog
    if (showInPlacePdfSheet) {
        var pdfPageSizeVal by remember { mutableStateOf(com.example.cameraxapp.media.PdfCompilationEngine.PageSize.A4) }
        var pdfOrientationVal by remember { mutableStateOf(com.example.cameraxapp.media.PdfCompilationEngine.PageOrientation.PORTRAIT) }
        var pdfMarginVal by remember { mutableStateOf(36) }
        var isProcessingInPlacePdf by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isProcessingInPlacePdf) showInPlacePdfSheet = false },
            title = {
                Text(
                    text = "Compile Images to PDF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "${selectedFiles.size} files will be rendered sequentially.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Page size selectors
                    Text("Page Dimensions", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        com.example.cameraxapp.media.PdfCompilationEngine.PageSize.values().forEach { sizeOpt ->
                            val isChosen = pdfPageSizeVal == sizeOpt
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { pdfPageSizeVal = sizeOpt },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isChosen) MaterialTheme.colorScheme.secondary else Color.Gray.copy(alpha = 0.3f)),
                                color = if (isChosen) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = sizeOpt.name.replace("_", " "),
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Printed Page bounds orientation options
                    Text("Orientation Layout", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        com.example.cameraxapp.media.PdfCompilationEngine.PageOrientation.values().forEach { orientOpt ->
                            val isChosen = pdfOrientationVal == orientOpt
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { pdfOrientationVal = orientOpt },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isChosen) MaterialTheme.colorScheme.secondary else Color.Gray.copy(alpha = 0.3f)),
                                color = if (isChosen) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = orientOpt.name,
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Canvas margins
                    Text("Fittings Margins", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "0.5\"" to 36,
                            "1.0\"" to 72,
                            "None" to 0
                        ).forEach { (label, valPx) ->
                            val isChosen = pdfMarginVal == valPx
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { pdfMarginVal = valPx },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isChosen) MaterialTheme.colorScheme.secondary else Color.Gray.copy(alpha = 0.3f)),
                                color = if (isChosen) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = label,
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isProcessingInPlacePdf = true
                        scope.launch {
                            val documentsFolder = File(baseFolder, "Documents/CompiledPDFs").apply { mkdirs() }
                            val outFilename = "CompiledInPlace_${System.currentTimeMillis()}.pdf"
                            val docOutput = File(documentsFolder, outFilename)

                            val compileConfig = com.example.cameraxapp.media.PdfCompilationEngine.PdfConfig(
                                pageSize = pdfPageSizeVal,
                                orientation = pdfOrientationVal,
                                marginPixels = pdfMarginVal
                            )

                            // Sequential filtering for on-disk images list
                            val imageFilesToProcess = selectedFiles.filter { !it.isDirectory && it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
                            
                            val didSucceed = com.example.cameraxapp.media.PdfCompilationEngine.compileImagesToPdf(
                                context = context,
                                imageFiles = imageFilesToProcess,
                                outputFile = docOutput,
                                config = compileConfig
                            )

                            refreshCounter++
                            clearSelection()
                            showInPlacePdfSheet = false
                            isProcessingInPlacePdf = false

                            if (didSucceed && docOutput.exists()) {
                                triggerNotification("Successfully compiled ${imageFilesToProcess.size} photos into PDF: ${outFilename}!")
                            } else {
                                triggerNotification("Canvas compiling returned error results")
                            }
                        }
                    },
                    enabled = !isProcessingInPlacePdf
                ) {
                    if (isProcessingInPlacePdf) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Compile")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showInPlacePdfSheet = false },
                    enabled = !isProcessingInPlacePdf
                ) {
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
    onDelete: (File) -> Unit,
    onRename: ((File, File) -> Unit)? = null,
    triggerNotification: (String) -> Unit
) {
    val context = LocalContext.current
    val isImg = file.extension.lowercase() in listOf("jpg", "png", "jpeg")
    val isVid = file.extension.lowercase() == "mp4"
    val isMarkdown = file.extension.lowercase() == "md"
    val isDoc = file.extension.lowercase() in listOf("txt", "json", "csv", "xml", "html", "css", "js", "py", "sh")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp, bottom = 64.dp),
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
            } else if (isMarkdown || isDoc) {
                // Specialized Renderer & Interactive Document Editor
                DocumentViewerEditor(
                    file = file,
                    isMarkdown = isMarkdown,
                    onContentChanged = {},
                    triggerNotification = triggerNotification
                )
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
            onRename?.let {
                IconButton(onClick = {
                    onRename(file, file)
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.White)
                }
            }
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

@Composable
fun DocumentViewerEditor(
    file: File,
    isMarkdown: Boolean,
    onContentChanged: () -> Unit,
    triggerNotification: (String) -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var textContent by remember(file) {
        mutableStateOf(
            try {
                file.readText()
            } catch (e: Exception) {
                "Failed to load content: ${e.localizedMessage}"
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Mode Selector / Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { isEditMode = false },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (!isEditMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (!isEditMode) MaterialTheme.colorScheme.onPrimary else Color.White
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Preview", fontSize = 12.sp)
                }
                TextButton(
                    onClick = { isEditMode = true },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isEditMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isEditMode) MaterialTheme.colorScheme.onPrimary else Color.White
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Edit Source", fontSize = 12.sp)
                }
            }

            if (isEditMode) {
                ElevatedButton(
                    onClick = {
                        try {
                            file.writeText(textContent)
                            triggerNotification("Document saved successfully")
                            isEditMode = false
                            onContentChanged()
                        } catch (e: Exception) {
                            triggerNotification("Failed to save: ${e.localizedMessage}")
                        }
                    },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check, 
                        contentDescription = "Save",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", fontSize = 12.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isEditMode) {
                OutlinedTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    placeholder = { Text("Type content here...", color = Color.Gray) }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFF1E1E1E))
                ) {
                    if (isMarkdown) {
                        MarkdownRenderer(content = textContent)
                    } else {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = textContent,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownRenderer(content: String) {
    val lines = content.lines()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = Color.White
    
    var inCodeBlock = false
    val codeBlockContent = StringBuilder()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (line in lines) {
            val trimmed = line.trim()
            
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = codeBlockContent.toString().trimEnd(),
                            color = Color(0xFFE0E0E0),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .padding(12.dp)
                                .horizontalScroll(rememberScrollState())
                        )
                    }
                    codeBlockContent.setLength(0)
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockContent.append(line).append("\n")
                continue
            }

            if (trimmed.startsWith("# ")) {
                Text(
                    text = parseMarkdownInline(trimmed.removePrefix("# "), primaryColor, onBackgroundColor),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                continue
            }
            if (trimmed.startsWith("## ")) {
                Text(
                    text = parseMarkdownInline(trimmed.removePrefix("## "), primaryColor, onBackgroundColor),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
                continue
            }
            if (trimmed.startsWith("### ")) {
                Text(
                    text = parseMarkdownInline(trimmed.removePrefix("### "), primaryColor, onBackgroundColor),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(top = 6.dp)
                )
                continue
            }
            if (trimmed.startsWith("#### ")) {
                Text(
                    text = parseMarkdownInline(trimmed.removePrefix("#### "), primaryColor, onBackgroundColor),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 4.dp)
                )
                continue
            }

            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                HorizontalDivider(
                    color = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                continue
            }

            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                val bulletText = trimmed.substring(2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = parseMarkdownInline(bulletText, primaryColor, onBackgroundColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                continue
            }

            val matchResult = "^(\\d+)\\.\\s+(.*)$".toRegex().find(trimmed)
            if (matchResult != null) {
                val num = matchResult.groupValues[1]
                val listText = matchResult.groupValues[2]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "$num. ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = parseMarkdownInline(listText, primaryColor, onBackgroundColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                continue
            }

            if (trimmed.startsWith(">")) {
                val quoteText = trimmed.removePrefix(">").trim()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(IntrinsicSize.Min)
                            .background(primaryColor)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = parseMarkdownInline(quoteText, primaryColor, onBackgroundColor),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = Color.LightGray
                    )
                }
                continue
            }

            if (trimmed.isNotEmpty()) {
                Text(
                    text = parseMarkdownInline(line, primaryColor, onBackgroundColor),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        if (inCodeBlock && codeBlockContent.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = codeBlockContent.toString().trimEnd(),
                    color = Color(0xFFE0E0E0),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(12.dp)
                        .horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

fun parseMarkdownInline(text: String, primaryColor: Color, onBackgroundColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("***", i) -> {
                    val end = text.indexOf("***", i + 3)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                            append(text.substring(i + 3, end))
                        }
                        i = end + 3
                    } else {
                        append("***")
                        i += 3
                    }
                }
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append("**")
                        i += 2
                    }
                }
                text.startsWith("*", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1) {
                        withStyle(style = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append("*")
                        i += 1
                    }
                }
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(style = SpanStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            background = Color(0xFF333333),
                            color = Color(0xFFFFB74D)
                        )) {
                            append(" " + text.substring(i + 1, end) + " ")
                        }
                        i = end + 1
                    } else {
                        append("`")
                        i += 1
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
