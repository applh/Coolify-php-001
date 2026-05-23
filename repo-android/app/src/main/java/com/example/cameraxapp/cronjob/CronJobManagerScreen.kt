package com.example.cameraxapp.cronjob

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CronJobManagerScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit, onOpenRightDrawer: () -> Unit) {
    val context = LocalContext.current
    val database = remember { CronJobDatabase.getDatabase(context) }
    val dao = remember { database.cronJobDao() }
    val scope = rememberCoroutineScope()
    
    val allJobs by dao.getAllJobsFlow().collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            TopAppBar(
                title = { Text("Cronjob Manager") },
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
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (allJobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No cron jobs configured.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(allJobs) { job ->
                        JobItemCard(
                            job = job,
                            onToggle = { isEnabled ->
                                scope.launch {
                                    val updatedJob = job.copy(isEnabled = isEnabled)
                                    dao.updateJob(updatedJob)
                                    if (isEnabled) {
                                        CronJobScheduler.scheduleJob(context, updatedJob)
                                    } else {
                                        CronJobScheduler.cancelJob(context, updatedJob.id)
                                    }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    CronJobScheduler.cancelJob(context, job.id)
                                    dao.deleteJobById(job.id)
                                }
                            }
                        )
                    }
                }
            }
        }
        
        if (showCreateDialog) {
            JobEditorDialog(
                onDismiss = { showCreateDialog = false },
                onSave = { jobType, intervalMinutes, reqNet, reqCharge, url, fileName ->
                    scope.launch {
                        val newJob = CronJobEntity(
                            id = UUID.randomUUID().toString(),
                            jobType = jobType,
                            intervalMinutes = intervalMinutes,
                            isEnabled = true,
                            requiresNetwork = reqNet,
                            requiresCharging = reqCharge,
                            downloadUrl = url,
                            saveFileName = fileName
                        )
                        dao.insertJob(newJob)
                        CronJobScheduler.scheduleJob(context, newJob)
                        showCreateDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun JobItemCard(
    job: CronJobEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Type: ${job.jobType}", style = MaterialTheme.typography.titleMedium)
                Switch(checked = job.isEnabled, onCheckedChange = onToggle)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Interval: ${job.intervalMinutes} mins")
            Text("Network Req: ${job.requiresNetwork} | Charging Req: ${job.requiresCharging}")
            if (job.jobType == "HTTP_DOWNLOAD") {
                Spacer(modifier = Modifier.height(4.dp))
                Text("URL: ${job.downloadUrl ?: "(none)"}", style = MaterialTheme.typography.bodySmall)
                Text("File: ${job.saveFileName ?: "(none)"}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
            }
        }
    }
}

@Composable
fun JobEditorDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int, Boolean, Boolean, String?, String?) -> Unit
) {
    var jobType by remember { mutableStateOf("CAMERA_CAPTURE") }
    val jobTypes = listOf("CAMERA_CAPTURE", "WALLPAPER_CHANGER", "HTTP_DOWNLOAD")
    
    var intervalMinutes by remember { mutableStateOf(15f) }
    var reqNet by remember { mutableStateOf(false) }
    var reqCharge by remember { mutableStateOf(false) }

    var downloadUrl by remember { mutableStateOf("") }
    var saveFileName by remember { mutableStateOf("downloaded_file.jpg") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Cronjob") },
        text = {
            Column {
                Text("Job Type:")
                jobTypes.forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = jobType == type,
                            onClick = { 
                                jobType = type
                                if (type == "HTTP_DOWNLOAD") {
                                    reqNet = true
                                }
                            }
                        )
                        Text(type)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Interval (minutes): ${intervalMinutes.toInt()}")
                Slider(
                    value = intervalMinutes,
                    onValueChange = { intervalMinutes = it },
                    valueRange = 15f..1440f,
                    steps = 1425
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reqNet, onCheckedChange = { reqNet = it })
                    Text("Requires Wifi/Network")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reqCharge, onCheckedChange = { reqCharge = it })
                    Text("Requires Charging")
                }

                if (jobType == "HTTP_DOWNLOAD") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        label = { Text("Download URL") },
                        placeholder = { Text("https://example.com/api/data") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveFileName,
                        onValueChange = { saveFileName = it },
                        label = { Text("Save Filename") },
                        placeholder = { Text("data.json") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                val finalUrl = if (jobType == "HTTP_DOWNLOAD") downloadUrl else null
                val finalFileName = if (jobType == "HTTP_DOWNLOAD") saveFileName else null
                onSave(jobType, intervalMinutes.toInt(), reqNet, reqCharge, finalUrl, finalFileName) 
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
