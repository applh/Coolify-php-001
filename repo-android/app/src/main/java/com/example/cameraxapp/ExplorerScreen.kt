package com.example.cameraxapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var currentDir by remember { mutableStateOf(context.getExternalFilesDir(null) ?: context.filesDir) }
    var files by remember(currentDir) { mutableStateOf(currentDir.listFiles()?.toList() ?: emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explorer: ${currentDir.name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (currentDir != context.filesDir && currentDir.parentFile != null) {
                ListItem(
                    headlineContent = { Text(".. (Go up)") },
                    leadingContent = { Icon(Icons.Default.List, contentDescription = null) },
                    modifier = Modifier.clickable {
                        currentDir = currentDir.parentFile!!
                    }
                )
            }

            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No files found in this directory")
                }
            } else {
                LazyColumn {
                    items(files) { file ->
                        FileItem(file) {
                            if (file.isDirectory) {
                                currentDir = file
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileItem(file: File, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(file.name) },
        supportingContent = { Text(if (file.isDirectory) "Directory" else "${file.length() / 1024} KB") },
        leadingContent = {
            Icon(
                if (file.isDirectory) Icons.Default.List else Icons.Default.Info,
                contentDescription = null
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
