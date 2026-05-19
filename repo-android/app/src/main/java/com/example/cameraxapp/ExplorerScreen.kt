package com.example.cameraxapp

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
import androidx.compose.material.icons.Icons
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
import androidx.core.content.FileProvider
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import java.io.File

enum class SortOption { DATE_DESC, DATE_ASC, NAME_ASC, SIZE_DESC }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExplorerScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val storageLocation by repository.storageLocation.collectAsState(initial = 0)
    
    val loadFiles = {
        val rootFiles = when(storageLocation) {
            1 -> {
                val pics = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)?.listFiles()?.toList() ?: emptyList()
                val vids = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)?.listFiles()?.toList() ?: emptyList()
                pics + vids
            }
            2 -> {
                val dirs = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
                val extDir = if (dirs.size > 1) dirs[1] else null
                extDir?.listFiles()?.toList() ?: emptyList()
            }
            else -> {
                context.filesDir.listFiles()?.toList() ?: emptyList()
            }
        }
        rootFiles.filter { it.extension == "jpg" || it.extension == "mp4" }
    }
    
    var allFiles by remember(storageLocation) { mutableStateOf(loadFiles()) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedFiles = remember { mutableStateListOf<File>() }

    val displayedFiles = remember(allFiles, searchQuery, sortOption) {
        allFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
            .let { list ->
                when (sortOption) {
                    SortOption.DATE_DESC -> list.sortedByDescending { it.lastModified() }
                    SortOption.DATE_ASC -> list.sortedBy { it.lastModified() }
                    SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                    SortOption.SIZE_DESC -> list.sortedByDescending { it.length() }
                }
            }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDualPane = maxWidth > 600.dp

        if (selectedFile != null && !isDualPane) {
            FullScreenMedia(
                file = selectedFile!!,
                onClose = { selectedFile = null },
                onDelete = {
                    it.delete()
                    allFiles = loadFiles()
                    selectedFile = null
                }
            )
            return@BoxWithConstraints
        }

        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("${selectedFiles.size} selected") },
                        navigationIcon = {
                            IconButton(onClick = { 
                                isSelectionMode = false
                                selectedFiles.clear()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                if (selectedFiles.isNotEmpty()) {
                                    val uris = selectedFiles.map { FileProvider.getUriForFile(context, "${context.packageName}.provider", it) }
                                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "*/*"
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                                }
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                selectedFiles.forEach { it.delete() }
                                allFiles = loadFiles()
                                isSelectionMode = false
                                selectedFiles.clear()
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
                                placeholder = { Text("Search files...") },
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
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close Search")
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { Text("Explorer") },
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
                                    Icon(Icons.Default.List, contentDescription = "Sort")
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
                        }
                    )
                }
            }
        ) { padding ->
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (displayedFiles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No files found")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedFiles) { file ->
                                val isSelected = selectedFiles.contains(file)
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .aspectRatio(1f)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                isDualPane && selectedFile == file -> MaterialTheme.colorScheme.secondaryContainer
                                                else -> Color.Transparent
                                            }
                                        )
                                        .padding(if (isSelected) 4.dp else 0.dp)
                                        .combinedClickable(
                                            onClick = {
                                                if (isSelectionMode) {
                                                    if (isSelected) selectedFiles.remove(file)
                                                    else selectedFiles.add(file)
                                                    if (selectedFiles.isEmpty()) isSelectionMode = false
                                                } else {
                                                    selectedFile = file
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
                                    Image(
                                        painter = rememberAsyncImagePainter(model = file),
                                        contentDescription = file.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (file.extension == "mp4") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Video",
                                                tint = Color.White,
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isDualPane && selectedFile != null) {
                    Box(modifier = Modifier.weight(1.5f).fillMaxHeight()) {
                        FullScreenMedia(
                            file = selectedFile!!,
                            onClose = { selectedFile = null },
                            onDelete = {
                                it.delete()
                                allFiles = loadFiles()
                                selectedFile = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenMedia(file: File, onClose: () -> Unit, onDelete: (File) -> Unit) {
    val context = LocalContext.current
    var showRenameDialog by remember { mutableStateOf(false) }
    
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(file.nameWithoutExtension) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        val newFile = File(file.parentFile, "$newName.${file.extension}")
                        if (file.renameTo(newFile)) {
                            // Ideally, we update the UI to reflect new name, but we can just close
                            onClose()
                        }
                    }
                    showRenameDialog = false
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showRenameDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename")
                    }
                    IconButton(onClick = {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (file.extension == "mp4") "video/mp4" else "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { onDelete(file) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            if (file.extension == "mp4") {
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                        prepare()
                        playWhenReady = true
                    }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        exoPlayer.release()
                    }
                }
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                Image(
                    painter = rememberAsyncImagePainter(model = file),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                val newOffset = offset + pan
                                // simple boundary to avoid jumping out too far could be added, but keeping it simple
                                offset = if (scale > 1f) newOffset else androidx.compose.ui.geometry.Offset.Zero
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            }
        }
    }
}
