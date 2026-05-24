package com.example.cameraxapp.browser

import android.webkit.ConsoleMessage
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WebTab(
    val id: String,
    val title: String,
    val url: String,
    val progress: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    apiKey: String,
    onBackToHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Tab state management
    var tabs = remember { mutableStateListOf(WebTab("default", "New Tab", "https://www.google.com")) }
    var activeTabId by remember { mutableStateOf("default") }
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    // Address and loading states
    var rawUrlInput by remember(activeTabId) { mutableStateOf(TextFieldValue(activeTab.url)) }
    var loadingProgress by remember { mutableStateOf(0) }
    var sslSecure by remember { mutableStateOf(false) }

    // User agent profiling configurations
    var selectedUserAgentProfile by remember { mutableStateOf("Mobile") }
    val userAgentString = when (selectedUserAgentProfile) {
        "Tablet" -> "Mozilla/5.0 (Linux; Android 13; SM-X906B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
        "Desktop Windows" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
        "Desktop Safari" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Safari/605.1.15"
        else -> "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36" // Mobile Default
    }

    // Panels toggle states
    var isSidePanelOpen by remember { mutableStateOf(false) }
    var showAiScriptCompilerDialog by remember { mutableStateOf(false) }
    var activeSideTab by remember { mutableStateOf("Bookmarks") } // Bookmarks, History, Scripts, Downloads

    // Database states
    val bookmarks by viewModel.bookmarks.collectAsState()
    val history by viewModel.history.collectAsState()
    val scripts by viewModel.scripts.collectAsState()
    val downloadQueue by viewModel.downloadQueue.collectAsState()
    val compilationStatus by viewModel.scriptGenerationStatus.collectAsState()

    var activeWebViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Helper functions
    val navigateToUrl: (String) -> Unit = { urlString ->
        var destination = urlString.trim()
        if (destination.isNotBlank()) {
            if (!destination.startsWith("http://") && !destination.startsWith("https://")) {
                // If it looks like a valid domain, prepend https, otherwise treat as search query
                if (destination.contains(".") && !destination.contains(" ")) {
                    destination = "https://$destination"
                } else {
                    destination = "https://www.google.com/search?q=${java.net.URLEncoder.encode(destination, "UTF-8")}"
                }
            }
            rawUrlInput = TextFieldValue(destination)
            sslSecure = destination.startsWith("https://")
            
            // Log target visit
            viewModel.recordVisit(activeTab.title, destination)
            activeWebViewInstance?.loadUrl(destination)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTabletLayout = maxWidth > 650.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Adaptive Side Panel for Tablets or Left side Drawer info
            if (isTabletLayout || isSidePanelOpen) {
                Surface(
                    modifier = Modifier
                        .width(if (isTabletLayout) 280.dp else 250.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Browser Console",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!isTabletLayout) {
                                IconButton(onClick = { isSidePanelOpen = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                        }

                        // Navigation Tabs for Utility menus
                        TabRow(
                            selectedTabIndex = when (activeSideTab) {
                                "Bookmarks" -> 0
                                "History" -> 1
                                "Scripts" -> 2
                                else -> 3
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Tab(
                                selected = activeSideTab == "Bookmarks",
                                onClick = { activeSideTab = "Bookmarks" },
                                text = { Text("Book", fontSize = 11.sp) }
                            )
                            Tab(
                                selected = activeSideTab == "History",
                                onClick = { activeSideTab = "History" },
                                text = { Text("Hist", fontSize = 11.sp) }
                            )
                            Tab(
                                selected = activeSideTab == "Scripts",
                                onClick = { activeSideTab = "Scripts" },
                                text = { Text("Script", fontSize = 11.sp) }
                            )
                            Tab(
                                selected = activeSideTab == "Downloads",
                                onClick = { activeSideTab = "Downloads" },
                                text = { Text("Down", fontSize = 11.sp) }
                            )
                        }

                        // Content of Active Side Panel
                        Box(modifier = Modifier.weight(1f)) {
                            when (activeSideTab) {
                                "Bookmarks" -> {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        if (bookmarks.isEmpty()) {
                                            item {
                                                Text(
                                                    "No bookmarks yet",
                                                    modifier = Modifier.padding(16.dp),
                                                    color = Color.Gray,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                        items(bookmarks) { bookmark ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { navigateToUrl(bookmark.url) }
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        bookmark.title,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        bookmark.url,
                                                        color = Color.Gray,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(onClick = { viewModel.deleteBookmark(bookmark.id) }) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = Color.Red,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }
                                }
                                "History" -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Button(
                                            onClick = { viewModel.clearHistory() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp)
                                        ) {
                                            Text("Clear History", fontSize = 12.sp)
                                        }
                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            if (history.isEmpty()) {
                                                item {
                                                    Text(
                                                        "No history registered",
                                                        modifier = Modifier.padding(16.dp),
                                                        color = Color.Gray,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                            items(history) { record ->
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { navigateToUrl(record.url) }
                                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                                ) {
                                                    Text(
                                                        record.title,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        record.url,
                                                        color = Color.Gray,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            }
                                        }
                                    }
                                }
                                "Scripts" -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Button(
                                            onClick = { showAiScriptCompilerDialog = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Create,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("AI Compile Script", fontSize = 12.sp)
                                        }
                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            if (scripts.isEmpty()) {
                                                item {
                                                    Text(
                                                        "No userscripts configured",
                                                        modifier = Modifier.padding(16.dp),
                                                        color = Color.Gray,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                            items(scripts) { script ->
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Text(
                                                            script.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                        Text(
                                                            "Pattern: ${script.targetRegex}",
                                                            color = Color.Gray,
                                                            fontSize = 11.sp,
                                                            maxLines = 1
                                                        )
                                                        Spacer(Modifier.height(6.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Checkbox(
                                                                    checked = script.isActive,
                                                                    onCheckedChange = { viewModel.toggleUserscript(script.id, it) },
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                                Spacer(Modifier.width(4.dp))
                                                                Text("Active", fontSize = 11.sp)
                                                            }
                                                            IconButton(onClick = { viewModel.deleteUserscript(script.id) }) {
                                                                Icon(
                                                                    Icons.Default.Delete,
                                                                    contentDescription = "Delete",
                                                                    tint = Color.Red,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                "Downloads" -> {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        val values = downloadQueue.values.toList()
                                        if (values.isEmpty()) {
                                            item {
                                                Text(
                                                    "Queue is empty",
                                                    modifier = Modifier.padding(16.dp),
                                                    color = Color.Gray,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                        items(values) { task ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        task.filename,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        "Status: ${task.status.name}",
                                                        fontSize = 11.sp,
                                                        color = when (task.status) {
                                                            DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
                                                            DownloadStatus.SUCCEEDED -> Color.Green
                                                            DownloadStatus.FAILED -> Color.Red
                                                            else -> Color.Gray
                                                        }
                                                    )
                                                    Spacer(Modifier.height(4.dp))
                                                    LinearProgressIndicator(
                                                        progress = { task.progress },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                    Spacer(Modifier.height(4.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            "${task.speedKbSec} KB/s | ETA: ${if (task.etaSeconds > 0) "${task.etaSeconds}s" else "N/A"}",
                                                            fontSize = 10.sp,
                                                            color = Color.Gray
                                                        )
                                                        if (task.status == DownloadStatus.DOWNLOADING) {
                                                            IconButton(onClick = { viewModel.downloadsManager.pauseDownload(task.id) }) {
                                                                Icon(
                                                                    Icons.Default.Close,
                                                                    contentDescription = "Cancel",
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        } else if (task.status == DownloadStatus.SUCCEEDED) {
                                                            IconButton(onClick = {
                                                                val uri = viewModel.downloadsManager.getShareUri(task.filename)
                                                                if (uri != null) {
                                                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                        type = "*/*"
                                                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                    }
                                                                    context.startActivity(android.content.Intent.createChooser(intent, "Share Downloaded Asset"))
                                                                }
                                                            }) {
                                                                Icon(
                                                                    Icons.Default.Share,
                                                                    contentDescription = "Share",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
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
                    }
                }
            }

            // Central Navigation / Web Frame Container
            Column(modifier = Modifier.weight(1f)) {
                // Multi-Tab top bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(0.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(tabs) { tab ->
                                val isActive = tab.id == activeTabId
                                Row(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isActive) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                        .clickable { activeTabId = tab.id }
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 100.dp)
                                    )
                                    if (tabs.size > 1) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close Tab",
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable {
                                                    val index = tabs.indexOf(tab)
                                                    tabs.remove(tab)
                                                    if (isActive) {
                                                        activeTabId = tabs[if (index > 0) index - 1 else 0].id
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(onClick = {
                            val newId = UUID_Generator()
                            tabs.add(WebTab(newId, "New Tab", "https://www.google.com"))
                            activeTabId = newId
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "New Tab")
                        }
                    }
                }

                // Loading Status Progress Bar
                if (loadingProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Interactive HUD Address / Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { activeWebViewInstance?.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(onClick = { activeWebViewInstance?.goForward() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                    }
                    IconButton(onClick = { activeWebViewInstance?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                    IconButton(onClick = { navigateToUrl("https://www.google.com") }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }

                    // SSL Flag indicator + Address Input Bar
                    OutlinedTextField(
                        value = rawUrlInput,
                        onValueChange = { rawUrlInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = if (sslSecure) Icons.Default.Lock else Icons.Default.Warning,
                                contentDescription = "SSL Status",
                                tint = if (sslSecure) Color(0xFF00C853) else Color(0xFFFFAB00),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Bookmark this site",
                                    tint = if (bookmarks.any { it.url == activeTab.url }) Color(0xFFFFD600) else Color.Gray,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            viewModel.addBookmark(activeTab.title, activeTab.url)
                                            Toast
                                                .makeText(context, "Added Bookmark", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Go",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { navigateToUrl(rawUrlInput.text) }
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        placeholder = { Text("Search or Type URL", fontSize = 12.sp) }
                    )

                    // Hamburger options menu
                    var showDropdownMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showDropdownMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Console Tools Side panel") },
                                onClick = {
                                    showDropdownMenu = false
                                    isSidePanelOpen = !isSidePanelOpen
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compile Sandbox Custom Userscript") },
                                onClick = {
                                    showDropdownMenu = false
                                    showAiScriptCompilerDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("User Agent: Mobile") },
                                onClick = {
                                    showDropdownMenu = false
                                    selectedUserAgentProfile = "Mobile"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("User Agent: Tablet") },
                                onClick = {
                                    showDropdownMenu = false
                                    selectedUserAgentProfile = "Tablet"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("User Agent: Desktop Windows") },
                                onClick = {
                                    showDropdownMenu = false
                                    selectedUserAgentProfile = "Desktop Windows"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("User Agent: Desktop Safari") },
                                onClick = {
                                    showDropdownMenu = false
                                    selectedUserAgentProfile = "Desktop Safari"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Back To App Hub") },
                                onClick = {
                                    showDropdownMenu = false
                                    onBackToHub()
                                }
                            )
                        }
                    }
                }

                // Isolated Web Frame Viewport
                Box(modifier = Modifier.weight(1f)) {
                    BrowserWebView(
                        url = activeTab.url,
                        onProgressChanged = { progressVal ->
                            loadingProgress = progressVal
                            val index = tabs.indexOfFirst { it.id == activeTabId }
                            if (index != -1) {
                                tabs[index] = tabs[index].copy(progress = progressVal)
                            }
                        },
                        onPageStarted = { url ->
                            rawUrlInput = TextFieldValue(url)
                            sslSecure = url.startsWith("https://")
                            val index = tabs.indexOfFirst { it.id == activeTabId }
                            if (index != -1) {
                                tabs[index] = tabs[index].copy(url = url)
                            }
                        },
                        onConsoleMsgCaptured = { consoleMsg ->
                            Log.i("InjectedAppletConsole", "[${consoleMsg.messageLevel()}] ${consoleMsg.message()}")
                        },
                        onPageFinished = { view, url ->
                            val pageTitle = view.title ?: "Web Page"
                            val index = tabs.indexOfFirst { it.id == activeTabId }
                            if (index != -1) {
                                tabs[index] = tabs[index].copy(title = pageTitle, url = url)
                            }
                            viewModel.recordVisit(pageTitle, url)

                            // Load and run configured JS userscripts matching this URL
                            viewModel.getScriptsForUrl(url) { listScripts ->
                                listScripts.forEach { script ->
                                    Log.d("BrowserScreen", "Injecting Sandbox Script: ${script.name}")
                                    view.evaluateJavascript(script.jsContent, null)
                                }
                            }
                        },
                        onDownloadTriggered = { downloadUrl, _, contentDisposition, mimetype, contentLength ->
                            // Custom listener interception hooks downloads directly
                            val filenameFromHeader = if (contentDisposition.contains("filename=")) {
                                contentDisposition.substringAfter("filename=").substringBefore(";").replace("\"", "").trim()
                            } else {
                                UUID_Generator() + if (mimetype.contains("pdf")) ".pdf" else ".bin"
                            }
                            viewModel.triggerFileDownload(downloadUrl, filenameFromHeader, contentLength)
                            activeSideTab = "Downloads"
                            isSidePanelOpen = true
                            Toast
                                .makeText(context, "Queued background file download: $filenameFromHeader", Toast.LENGTH_LONG)
                                .show()
                        },
                        userAgentString = userAgentString,
                        updateWebView = { activeWebViewInstance = it }
                    )
                }
            }
        }
    }

    // AI Generative Script compiler workspace dialog
    if (showAiScriptCompilerDialog) {
        var compilerUserDemandPrompt by remember { mutableStateOf("") }
        var compilerTargetDomain by remember { mutableStateOf(activeTab.url) }

        AlertDialog(
            onDismissRequest = { showAiScriptCompilerDialog = false },
            title = {
                Text(
                    "AI Generative Sandbox Scripter",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Generates custom automated scripts executed in a background secure sandbox on target pages.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = compilerTargetDomain,
                        onValueChange = { compilerTargetDomain = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Target domain pattern regex", fontSize = 11.sp) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = compilerUserDemandPrompt,
                        onValueChange = { compilerUserDemandPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("What logic should this script perform?", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Capture all table items and trim ad containers", fontSize = 11.sp) }
                    )
                    
                    if (!compilationStatus.isNullOrEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = compilationStatus ?: "",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.compileCustomUserscript(
                            userDemandPrompt = compilerUserDemandPrompt,
                            targetDomainPattern = compilerTargetDomain,
                            apiKey = apiKey
                        )
                    }
                ) {
                    Text("Compile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiScriptCompilerDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

// Compact helper to avoid standard library dependency mismatches
private fun UUID_Generator(): String {
    return java.util.UUID.randomUUID().toString().take(8)
}
