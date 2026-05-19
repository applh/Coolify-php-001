package com.example.cameraxapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val saveToPublic by repository.saveToPublic.collectAsState(initial = false)
    
    val currentDir = if (saveToPublic) {
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.filesDir
    } else {
        context.filesDir
    }
    
    var files by remember(currentDir) { mutableStateOf(currentDir.listFiles()?.filter { it.extension == "jpg" }?.toList() ?: emptyList()) }
    var selectedFile by remember { mutableStateOf<File?>(null) }

    if (selectedFile != null) {
        FullScreenImage(
            file = selectedFile!!,
            onClose = { selectedFile = null },
            onDelete = {
                it.delete()
                files = currentDir.listFiles()?.filter { f -> f.extension == "jpg" }?.toList() ?: emptyList()
                selectedFile = null
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explorer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No photos found in this directory")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(files) { file ->
                        Image(
                            painter = rememberAsyncImagePainter(model = file),
                            contentDescription = file.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .clickable { selectedFile = file }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImage(file: File, onClose: () -> Unit, onDelete: (File) -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
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
            Image(
                painter = rememberAsyncImagePainter(model = file),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
