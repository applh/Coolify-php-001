package com.example.cameraxapp

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// BACKUP VIEWMODEL
// ==========================================

class BackupViewModel : ViewModel() {
    private val _snapshots = MutableStateFlow<List<FileSnapshotItem>>(emptyList())
    val snapshots: StateFlow<List<FileSnapshotItem>> = _snapshots.asStateFlow()

    private val _dbFolderSize = MutableStateFlow("0 KB")
    val dbFolderSize: StateFlow<String> = _dbFolderSize.asStateFlow()

    private val _sessionsSize = MutableStateFlow("0 KB")
    val sessionsSize: StateFlow<String> = _sessionsSize.asStateFlow()

    private val _datastoreSize = MutableStateFlow("0 KB")
    val datastoreSize: StateFlow<String> = _datastoreSize.asStateFlow()

    private val _mediaSize = MutableStateFlow("0 KB")
    val mediaSize: StateFlow<String> = _mediaSize.asStateFlow()

    private val _backupCronActive = MutableStateFlow(false)
    val backupCronActive: StateFlow<Boolean> = _backupCronActive.asStateFlow()

    private val _backupCronSchedule = MutableStateFlow("0 3 * * * (Daily 3 AM)")
    val backupCronSchedule: StateFlow<String> = _backupCronSchedule.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private val _infoState = MutableStateFlow<String?>(null)
    val infoState: StateFlow<String?> = _infoState.asStateFlow()

    fun clearNotifications() {
        _errorState.value = null
        _infoState.value = null
    }

    /**
     * Re-scans dynamic system storage file metrics for UI dashboard diagnostics.
     */
    fun scanSystemMetrics(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // DB Folder
                val dbFolder = context.getDatabasePath("agenda_hub.db").parentFile
                val dbSizeVal = if (dbFolder != null && dbFolder.exists()) getFolderSize(dbFolder) else 0L
                _dbFolderSize.value = formatSize(dbSizeVal)

                // Sessions folder
                val sessionsFolder = File(context.filesDir, "sessions")
                val sessSizeVal = if (sessionsFolder.exists()) getFolderSize(sessionsFolder) else 0L
                _sessionsSize.value = formatSize(sessSizeVal)

                // Datastore preferences
                val datastoreFolder = File(context.filesDir, "datastore")
                val datastoreSizeVal = if (datastoreFolder.exists()) getFolderSize(datastoreFolder) else 0L
                _datastoreSize.value = formatSize(datastoreSizeVal)

                // Media Pictures / GeminiCanvas folder
                val pics = File(context.filesDir, "Pictures")
                val canvas = File(context.filesDir, "GeminiCanvas")
                val mediaSizeVal = (if (pics.exists()) getFolderSize(pics) else 0L) +
                        (if (canvas.exists()) getFolderSize(canvas) else 0L)
                _mediaSize.value = formatSize(mediaSizeVal)

                // Query Cron database for Backup Active Status
                val dbHelper = AgendaDatabaseHelper(context)
                val jobs = dbHelper.getAllCronJobs()
                val backupJob = jobs.find { it.name.contains("Backup", ignoreCase = true) }
                _backupCronActive.value = backupJob?.isActive ?: false
                _backupCronSchedule.value = backupJob?.cronExpression ?: "0 3 * * * (Daily 3 AM)"

                // Fetch history archives
                loadSnapshotsList(context)
            } catch (e: Exception) {
                _errorState.value = "Failed scanning metrics: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads the stored list of archives in context.filesDir/backup_snapshots
     */
    fun loadSnapshotsList(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val backupDir = File(context.filesDir, "backup_snapshots")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val list = backupDir.listFiles { file -> file.isFile && file.name.endsWith(".zip") }
                ?.map { f ->
                    FileSnapshotItem(
                        name = f.name,
                        path = f.absolutePath,
                        sizeStr = formatSize(f.length()),
                        lastModified = f.lastModified()
                    )
                }?.sortedByDescending { it.lastModified } ?: emptyList()

            _snapshots.value = list
        }
    }

    /**
     * Generates a structural snapshot file manually and stores it locally.
     */
    fun triggerManualBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val backupDir = File(context.filesDir, "backup_snapshots")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val destFile = File(backupDir, "manual_backup_$timestamp.zip")

                FileOutputStream(destFile).use { fos ->
                    val ok = BackupManagerEngine.createBackupZip(context, fos)
                    if (ok) {
                        _infoState.value = "Manual ZIP generated successfully: ${destFile.name}"
                    } else {
                        _errorState.value = "Failed packaging filesystem."
                    }
                }
                scanSystemMetrics(context)
            } catch (e: Exception) {
                _errorState.value = "Backup creation exception: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Direct SAF streaming for reinstallation survival
     */
    fun createBackupDirectStream(context: Context, outStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val ok = BackupManagerEngine.createBackupZip(context, outStream)
                if (ok) {
                    _infoState.value = "Survival ZIP streamed to external storage successfully!"
                } else {
                    _errorState.value = "Failed streaming packages."
                }
                scanSystemMetrics(context)
            } catch (e: Exception) {
                _errorState.value = "External streaming crash: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Restores dynamic files directly from user selected file archive source.
     */
    fun restoreSelectedZip(context: Context, targetUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val inputStream = context.contentResolver.openInputStream(targetUri)
                if (inputStream != null) {
                    val ok = BackupManagerEngine.restoreBackupZip(context, inputStream)
                    if (ok) {
                        _infoState.value = "Success! Staged databases, preference datastores, and chats folder restored. Restarting system components..."
                        
                        // Re-sync Room Cron Jobs triggers
                        withContext(Dispatchers.Main) {
                            try {
                                com.example.cameraxapp.cronjob.CronJobScheduler.syncJobsFromDatabase(context)
                            } catch (e: Exception) {
                                Log.e("Backup", "Scheduler trigger synch failed: ${e.message}")
                            }
                        }
                    } else {
                        _errorState.value = "Database restoral check failed: Zip file has invalid schemas."
                    }
                } else {
                    _errorState.value = "Failed accessing chosen URI file."
                }
                scanSystemMetrics(context)
            } catch (e: Exception) {
                _errorState.value = "Restore exception: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Restores a local snapshots file list asset
     */
    fun restoreLocalFile(context: Context, file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val ok = file.inputStream().use { inputStream ->
                    BackupManagerEngine.restoreBackupZip(context, inputStream)
                }
                if (ok) {
                    _infoState.value = "Local archive folder restoral complete!"
                    withContext(Dispatchers.Main) {
                        try {
                            com.example.cameraxapp.cronjob.CronJobScheduler.syncJobsFromDatabase(context)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    _errorState.value = "Staging atomic restoration failed: Invalid payload."
                }
                scanSystemMetrics(context)
            } catch (e: Exception) {
                _errorState.value = "Failed restoring local snapshot: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes a local backup snapshots file
     */
    fun deleteLocalSnapshot(context: Context, file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    file.delete()
                    _infoState.value = "Deleted local archive file: ${file.name}"
                }
                loadSnapshotsList(context)
            } catch (e: Exception) {
                _errorState.value = "Failed deleting file: ${e.message}"
            }
        }
    }

    /**
     * Toggles active state for automated background WorkManager backups.
     */
    fun toggleCronAutomation(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val dbHelper = AgendaDatabaseHelper(context)
                val jobs = dbHelper.getAllCronJobs()
                val backupJob = jobs.find { it.name.contains("Backup", ignoreCase = true) }

                if (backupJob != null) {
                    val nextActiveState = !backupJob.isActive
                    dbHelper.updateCronStatus(backupJob.id, nextActiveState, backupJob.lastRunMillis, backupJob.status)
                    
                    if (nextActiveState) {
                        CronScheduler.scheduleExact(context, backupJob.id, 15L) // run every 15 mins for test, or schedule
                    } else {
                        CronScheduler.cancelExact(context, backupJob.id)
                    }
                    _infoState.value = "Automated Backup schedule has been " + (if (nextActiveState) "ENABLED" else "DISABLED")
                } else {
                    _errorState.value = "Automated schema backup cron job definition is missing."
                }
                scanSystemMetrics(context)
            } catch (e: Exception) {
                _errorState.value = "Fails toggling cron jobs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Tester instant bootstrap configuration profile. Seeding mock events, mock alarms, and fake discussions.
     */
    fun seedSampleQAData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                BackupManagerEngine.seedTesterMockProfile(context)
                _infoState.value = "Seeding completed! Generated mock events, alarms, configuration keys, and AI thread dialogue files instantly. ready for QA testing! 🚀"
                scanSystemMetrics(context)
            } catch (e: Exception) {
                _errorState.value = "Seeding failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Wipes active workspace state completely for QA testing resets.
     */
    fun runFullCleanReset(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                context.deleteDatabase("agenda_hub.db")
                context.deleteDatabase("cronjob_database")
                File(context.filesDir, "sessions").deleteRecursively()
                File(context.filesDir, "datastore").deleteRecursively()
                File(context.filesDir, "GeminiCanvas").deleteRecursively()
                _infoState.value = "Workspace wiped successfully! Environment is clean. You can import or seed data now."
                scanSystemMetrics(context)
            } catch (e: Exception) {
                _errorState.value = "Clean reset failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Helpers ---
    private fun getFolderSize(directory: File): Long {
        var length = 0L
        val files = directory.listFiles() ?: return 0L
        for (file in files) {
            length += if (file.isFile) file.length() else getFolderSize(file)
        }
        return length
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes.toDouble() / 1024.0
        if (kb < 1024.0) return String.format(Locale.getDefault(), "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.getDefault(), "%.1f MB", mb)
    }
}

data class FileSnapshotItem(
    val name: String,
    val path: String,
    val sizeStr: String,
    val lastModified: Long
)

// ==========================================
// BACKUP JETPACK COMPOSE UI SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenRightDrawer: () -> Unit,
    viewModel: BackupViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val snapshots by viewModel.snapshots.collectAsState()
    val dbSize by viewModel.dbFolderSize.collectAsState()
    val sessionsSize by viewModel.sessionsSize.collectAsState()
    val datastoreSize by viewModel.datastoreSize.collectAsState()
    val mediaSize by viewModel.mediaSize.collectAsState()
    val cronActive by viewModel.backupCronActive.collectAsState()
    val cronSchedule by viewModel.backupCronSchedule.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorState by viewModel.errorState.collectAsState()
    val infoState by viewModel.infoState.collectAsState()

    // SAF Create document stream contract
    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    viewModel.createBackupDirectStream(context, os)
                }
                Toast.makeText(context, "Backup exported outside sandbox!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed exporting document: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Open standard system zip select document contract
    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.restoreSelectedZip(context, it)
        }
    }

    // Refresh metrics on entrance
    LaunchedEffect(Unit) {
        viewModel.scanSystemMetrics(context)
    }

    // Toast error / info notifications
    LaunchedEffect(errorState, infoState) {
        errorState?.let {
            Toast.makeText(context, "⚠️ Error: $it", Toast.LENGTH_LONG).show()
            viewModel.clearNotifications()
        }
        infoState?.let {
            Toast.makeText(context, "✅ Info: $it", Toast.LENGTH_LONG).show()
            viewModel.clearNotifications()
        }
    }

    Scaffold(
        bottomBar = {
            TopAppBar(
                title = { Text("Backup Manager", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Navigation Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenRightDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Quick Tools Links")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Coverage diagnostics card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Storage Metrics Analyzer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { viewModel.scanSystemMetrics(context) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Metrics")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    MetricRow("Primary App Databases (SQLite)", dbSize)
                    MetricRow("AITeam Conversations Files", sessionsSize)
                    MetricRow("Shared Configurations & Preferences", datastoreSize)
                    MetricRow("Synthesized Lumina Images Media", mediaSize)

                    Spacer(modifier = Modifier.height(12.dp))
                    if (isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing actions...", fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Automations scheduling card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Automated System Backups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Allows automatic daily database captures syncing via WorkManager daemon, surviving crash states.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (cronActive) Color.Green else Color.Red,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (cronActive) "Automation STATUS: ACTIVE" else "Automation STATUS: IDLE",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "Daemon Loop Interval: every 15 mins (testing mode) / daily",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = cronActive,
                            onCheckedChange = { viewModel.toggleCronAutomation(context) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Main ZIP package operations actions card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Backup & Recovery Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.triggerManualBackup(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Handcrafted Local Backup")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            exportDocumentLauncher.launch("Fraise_Full_Backup_$timestamp.zip")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stream ZIP Outside Sandbox (SAF Document)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            importDocumentLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import & Apply External ZIP Backup")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Tester instant seeds & Wipe Card (Frictionless bootstrap)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "QA Tester Stability sandbox Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Instant 1-Click Bootstrap presets populated with realistic diagnostic events, alarm targets, configuration layouts and AI conversational trees. Wipe the environment for virgin trials instantly.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.seedSampleQAData(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Apply QA Test Seeds")
                        }
                        Button(
                            onClick = { viewModel.runFullCleanReset(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Wipe Workspace State")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Local snapshot list card
            Text(
                text = "Chronological Local Archives History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (snapshots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No local manual or automated snapshots registered yet.", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                snapshots.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Size: ${item.sizeStr} • Modified: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(item.lastModified))}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { viewModel.restoreLocalFile(context, File(item.path)) }) {
                                    Text("RESTORE", fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = { viewModel.deleteLocalSnapshot(context, File(item.path)) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("DELETE", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MetricRow(label: String, valStr: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = valStr, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
