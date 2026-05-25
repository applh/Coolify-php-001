package com.example.cameraxapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.webkit.WebView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// AGENDA VIEWMODEL
// ==========================================

class AgendaViewModel(private val context: Context) : ViewModel() {

    private val dbHelper = AgendaDatabaseHelper(context)

    private val _events = MutableStateFlow<List<AgendaEvent>>(emptyList())
    val events: StateFlow<List<AgendaEvent>> = _events.asStateFlow()

    private val _alarms = MutableStateFlow<List<AlarmInfo>>(emptyList())
    val alarms: StateFlow<List<AlarmInfo>> = _alarms.asStateFlow()

    private val _cronJobs = MutableStateFlow<List<CronJobInfo>>(emptyList())
    val cronJobs: StateFlow<List<CronJobInfo>> = _cronJobs.asStateFlow()

    private val _cronLogs = MutableStateFlow<List<CronLog>>(emptyList())
    val cronLogs: StateFlow<List<CronLog>> = _cronLogs.asStateFlow()

    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    val selectedCalendar: StateFlow<Calendar> = _selectedCalendar.asStateFlow()

    private val _selectedDay = MutableStateFlow(Calendar.getInstance())
    val selectedDay: StateFlow<Calendar> = _selectedDay.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadData()
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun loadData() {
        viewModelScope.launch {
            _events.value = dbHelper.getAllEvents()
            _alarms.value = dbHelper.getAllAlarms()
            _cronJobs.value = dbHelper.getAllCronJobs()
            _cronLogs.value = dbHelper.getCronLogs()
        }
    }

    // --- Month Navigator ---
    fun nextMonth() {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, 1)
        _selectedCalendar.value = cal
    }

    fun prevMonth() {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, -1)
        _selectedCalendar.value = cal
    }

    fun selectDay(day: Calendar) {
        _selectedDay.value = day
    }

    // --- Calendar CRUD & Alarms Setup ---
    fun addEvent(
        title: String,
        notes: String,
        hours: Int,
        minutes: Int,
        duration: Int,
        color: String,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null
    ) {
        viewModelScope.launch {
            val targetCal = _selectedDay.value.clone() as Calendar
            targetCal.set(Calendar.HOUR_OF_DAY, hours)
            targetCal.set(Calendar.MINUTE, minutes)
            targetCal.set(Calendar.SECOND, 0)
            targetCal.set(Calendar.MILLISECOND, 0)

            val eventId = dbHelper.insertEvent(
                title, notes, targetCal.timeInMillis, duration, color,
                latitude, longitude, locationName
            )
            if (eventId != -1L) {
                // Register alarm notification reminder for this calendar event
                scheduleEventAlarm(eventId.toInt(), targetCal.timeInMillis, title)
                _toastMessage.value = "Calendar event created with atomic alarm reminder!"
                loadData()
            }
        }
    }

    fun deleteEvent(id: Int) {
        viewModelScope.launch {
            dbHelper.deleteEvent(id)
            cancelEventAlarm(id)
            _toastMessage.value = "Calendar event removed successfully."
            loadData()
        }
    }

    fun updateEvent(
        id: Int,
        title: String,
        notes: String,
        hours: Int,
        minutes: Int,
        duration: Int,
        color: String,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null
    ) {
        viewModelScope.launch {
            cancelEventAlarm(id)
            val targetCal = _selectedDay.value.clone() as Calendar
            targetCal.set(Calendar.HOUR_OF_DAY, hours)
            targetCal.set(Calendar.MINUTE, minutes)
            targetCal.set(Calendar.SECOND, 0)
            targetCal.set(Calendar.MILLISECOND, 0)

            dbHelper.updateEvent(
                id, title, notes, targetCal.timeInMillis, duration, color,
                latitude, longitude, locationName
            )
            scheduleEventAlarm(id, targetCal.timeInMillis, title)
            _toastMessage.value = "Calendar event updated with rescheduled alarm reminder!"
            loadData()
        }
    }

    // --- High-Precision Alarm Managers ---
    fun addAlarm(hours: Int, minutes: Int, name: String) {
        viewModelScope.launch {
            val alarmCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hours)
                set(Calendar.MINUTE, minutes)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // If past target, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val alarmId = dbHelper.insertAlarm(alarmCal.timeInMillis, name, true)
            if (alarmId != -1L) {
                registerSystemAlarm(alarmId.toInt(), alarmCal.timeInMillis, name)
                _toastMessage.value = "Exact Alarm scheduled bypassing Standby Mode!"
                loadData()
            }
        }
    }

    fun toggleAlarm(alarm: AlarmInfo) {
        viewModelScope.launch {
            val nextState = !alarm.isActive
            dbHelper.updateAlarmStatus(alarm.id, nextState)

            if (nextState) {
                var alarmTime = alarm.timeMillis
                // If past target, roll forward to future
                if (alarmTime <= System.currentTimeMillis()) {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = alarmTime
                        while (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                    alarmTime = cal.timeInMillis
                }
                registerSystemAlarm(alarm.id, alarmTime, alarm.label)
                _toastMessage.value = "Alarm activated."
            } else {
                cancelSystemAlarm(alarm.id)
                _toastMessage.value = "Alarm deactivated."
            }
            loadData()
        }
    }

    fun deleteAlarm(alarm: AlarmInfo) {
        viewModelScope.launch {
            dbHelper.deleteAlarm(alarm.id)
            cancelSystemAlarm(alarm.id)
            _toastMessage.value = "Alarm deleted safely."
            loadData()
        }
    }

    fun updateAlarm(id: Int, hours: Int, minutes: Int, name: String, isActive: Boolean) {
        viewModelScope.launch {
            cancelSystemAlarm(id)
            val alarmCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hours)
                set(Calendar.MINUTE, minutes)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            dbHelper.updateAlarm(id, alarmCal.timeInMillis, name, isActive)
            if (isActive) {
                registerSystemAlarm(id, alarmCal.timeInMillis, name)
            }
            _toastMessage.value = "Exact Alarm updated safely!"
            loadData()
        }
    }

    // --- Background Cron Workers ---
    fun addCronJob(name: String, expression: String) {
        viewModelScope.launch {
            val cronId = dbHelper.insertCronJob(name, expression, true)
            if (cronId != -1L) {
                scheduleCronJob(cronId.toInt(), expression)
            }
            _toastMessage.value = "Custom Cron service registered successfully."
            loadData()
        }
    }

    fun toggleCron(job: CronJobInfo) {
        viewModelScope.launch {
            dbHelper.updateCronStatus(job.id, !job.isActive, job.lastRunMillis, job.status)
            if (!job.isActive) {
                scheduleCronJob(job.id, job.cronExpression)
                _toastMessage.value = "Cron Job is now active."
            } else {
                cancelCronJob(job.id)
                _toastMessage.value = "Cron Job paused."
            }
            loadData()
        }
    }

    fun deleteCronJob(jobId: Int) {
        viewModelScope.launch {
            dbHelper.deleteCronJob(jobId)
            cancelCronJob(jobId)
            _toastMessage.value = "Cron automation deleted successfully."
            loadData()
        }
    }

    fun updateCronJob(id: Int, name: String, expression: String, isActive: Boolean) {
        viewModelScope.launch {
            dbHelper.updateCronJob(id, name, expression, isActive)
            if (isActive) {
                scheduleCronJob(id, expression)
            } else {
                cancelCronJob(id)
            }
            _toastMessage.value = "Cron task configuration updated."
            loadData()
        }
    }

    fun runCronOnDemand(jobId: Int) {
        // Enqueue high fidelity WorkManager immediate job 1-shot
        val workRequest = OneTimeWorkRequestBuilder<CronWorker>()
            .setInputData(workDataOf("CRON_ID" to jobId))
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
        _toastMessage.value = "Worker enqueued on immediate daemon thread..."
        
        // Refresh a bit later to see telemetry logs
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            loadData()
        }
    }

    // --- Background WorkManager Setup ---
    private fun scheduleCronJob(cronId: Int, expression: String) {
        var intervalMinutes = 15L
        if (expression.startsWith("*/")) {
            val mins = expression.substringAfter("*/").substringBefore(" ").toLongOrNull()
            if (mins != null && mins >= 1L) {
                intervalMinutes = mins
            }
        }
        CronScheduler.scheduleExact(context, cronId, intervalMinutes)
    }

    private fun cancelCronJob(cronId: Int) {
        CronScheduler.cancelExact(context, cronId)
        WorkManager.getInstance(context).cancelUniqueWork("CRON_$cronId")
    }

    // --- Android System Alarms Wiring ----
    private fun registerSystemAlarm(alarmId: Int, timeMs: Long, label: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (BuildHelper.canScheduleExact(context)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeMs,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                timeMs,
                pendingIntent
            )
        }
    }

    private fun cancelSystemAlarm(alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleEventAlarm(eventId: Int, timeMs: Long, title: String) {
        val calendarAlarmId = eventId + 100000 // Offset avoid colliding with standard alarm IDs
        registerSystemAlarm(calendarAlarmId, timeMs, "Upcoming Appointment: $title")
    }

    private fun cancelEventAlarm(eventId: Int) {
        cancelSystemAlarm(eventId + 100000)
    }
}

// ==========================================
// CENTRAL COMPOSABLE SCREEN LAYOUTS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit, onOpenRightDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: AgendaViewModel = viewModel(factory = AgendaViewModelFactory(context))

    val events by viewModel.events.collectAsState()
    val alarms by viewModel.alarms.collectAsState()
    val cronJobs by viewModel.cronJobs.collectAsState()
    val cronLogs by viewModel.cronLogs.collectAsState()
    val selectedCalendar by viewModel.selectedCalendar.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var activeTabIdx by remember { mutableStateOf(0) } // 0: Calendar, 1: Alarms, 2: Cron, 3: Map View
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddAlarmDialog by remember { mutableStateOf(false) }
    var showAddCronDialog by remember { mutableStateOf(false) }

    var mapClickedLat by remember { mutableStateOf<Double?>(null) }
    var mapClickedLng by remember { mutableStateOf<Double?>(null) }

    // Editing State Trackers
    var editingEvent by remember { mutableStateOf<AgendaEvent?>(null) }
    var editingAlarm by remember { mutableStateOf<AlarmInfo?>(null) }
    var editingCron by remember { mutableStateOf<CronJobInfo?>(null) }

    // Deletion Confirm States
    var confirmDeleteEventId by remember { mutableStateOf<Int?>(null) }
    var confirmDeleteAlarm by remember { mutableStateOf<AlarmInfo?>(null) }
    var confirmDeleteCron by remember { mutableStateOf<CronJobInfo?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            // Simulated Toast using snackbars
            viewModel.clearToast()
        }
    }

    Scaffold(
        bottomBar = {
            TopAppBar(
                title = { Text("🍓 Fraise Agenda Hub") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Drawer")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Data")
                    }
                    IconButton(onClick = onOpenRightDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Quick Tools")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Workspace Tab Row
            TabRow(selectedTabIndex = activeTabIdx) {
                Tab(
                    selected = activeTabIdx == 0,
                    onClick = { activeTabIdx = 0 },
                    text = { Text("Calendar") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )
                Tab(
                    selected = activeTabIdx == 1,
                    onClick = { activeTabIdx = 1 },
                    text = { Text("Alarms") },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) }
                )
                Tab(
                    selected = activeTabIdx == 2,
                    onClick = { activeTabIdx = 2 },
                    text = { Text("Cron Task") },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) }
                )
                Tab(
                    selected = activeTabIdx == 3,
                    onClick = { activeTabIdx = 3 },
                    text = { Text("Map View") },
                    icon = { Icon(Icons.Default.Place, contentDescription = null) }
                )
            }

            // Central Pane switching
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeTabIdx) {
                    0 -> CalendarPlannerPane(
                        events = events,
                        selectedCalendar = selectedCalendar,
                        selectedDay = selectedDay,
                        onPrevMonth = { viewModel.prevMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onSelectDay = { viewModel.selectDay(it) },
                        onDeleteEvent = { confirmDeleteEventId = it },
                        onEditEventClick = { editingEvent = it },
                        onAddEventClick = {
                            mapClickedLat = null
                            mapClickedLng = null
                            showAddEventDialog = true
                        }
                    )
                    1 -> AlarmManagersPane(
                        alarms = alarms,
                        onToggleAlarm = { viewModel.toggleAlarm(it) },
                        onDeleteAlarm = { confirmDeleteAlarm = it },
                        onEditAlarmClick = { editingAlarm = it },
                        onAddAlarmClick = { showAddAlarmDialog = true }
                    )
                    2 -> CronSchedulerPane(
                        jobs = cronJobs,
                        logs = cronLogs,
                        onToggleCron = { viewModel.toggleCron(it) },
                        onDeleteCron = { confirmDeleteCron = it },
                        onEditCronClick = { editingCron = it },
                        onRunOnDemand = { viewModel.runCronOnDemand(it) },
                        onAddCronClick = { showAddCronDialog = true }
                    )
                    3 -> LeafletMapViewPane(
                        events = events,
                        onAddEventAt = { lat: Double, lng: Double ->
                            mapClickedLat = lat
                            mapClickedLng = lng
                            showAddEventDialog = true
                        },
                        onEditEvent = { id: Int ->
                            val found = events.find { it.id == id }
                            if (found != null) {
                                editingEvent = found
                            }
                        }
                    )
                }
            }

            // Success toast message banners
            toastMessage?.let {
                Snackbar(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(it)
                }
            }
        }
    }

    // Modal dialogs - Add creation Dialogs
    if (showAddEventDialog) {
        CalendarEventCreatorDialog(
            initialLatitude = mapClickedLat,
            initialLongitude = mapClickedLng,
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title, notes, hour, min, dur, color, lat, lng, name ->
                viewModel.addEvent(title, notes, hour, min, dur, color, lat, lng, name)
                showAddEventDialog = false
            }
        )
    }

    if (showAddAlarmDialog) {
        AlarmCreatorDialog(
            onDismiss = { showAddAlarmDialog = false },
            onConfirm = { hour, min, label ->
                viewModel.addAlarm(hour, min, label)
                showAddAlarmDialog = false
            }
        )
    }

    if (showAddCronDialog) {
        CronCreatorDialog(
            onDismiss = { showAddCronDialog = false },
            onConfirm = { name, expression ->
                viewModel.addCronJob(name, expression)
                showAddCronDialog = false
            }
        )
    }

    // Modal dialogs - Edit Dialogs
    editingEvent?.let { event ->
        CalendarEventEditorDialog(
            event = event,
            onDismiss = { editingEvent = null },
            onConfirm = { title, notes, hour, min, dur, color, lat, lng, name ->
                viewModel.updateEvent(event.id, title, notes, hour, min, dur, color, lat, lng, name)
                editingEvent = null
            }
        )
    }

    editingAlarm?.let { alarm ->
        AlarmEditorDialog(
            alarm = alarm,
            onDismiss = { editingAlarm = null },
            onConfirm = { hour, min, label, active ->
                viewModel.updateAlarm(alarm.id, hour, min, label, active)
                editingAlarm = null
            }
        )
    }

    editingCron?.let { job ->
        CronEditorDialog(
            job = job,
            onDismiss = { editingCron = null },
            onConfirm = { name, expression, active ->
                viewModel.updateCronJob(job.id, name, expression, active)
                editingCron = null
            }
        )
    }

    // Modal dialogs - Deletion Confirmations (Anti-click-error safeguards)
    confirmDeleteEventId?.let { eventId ->
        AlertDialog(
            onDismissRequest = { confirmDeleteEventId = null },
            title = { Text("Delete Event Confirmation") },
            text = { Text("Are you absolutely sure you want to permanently delete this calendar appointment and its registered alarm remainder?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteEvent(eventId)
                        confirmDeleteEventId = null
                    }
                ) {
                    Text("Delete Permanently", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteEventId = null }) { Text("Cancel") }
            }
        )
    }

    confirmDeleteAlarm?.let { alarm ->
        AlertDialog(
            onDismissRequest = { confirmDeleteAlarm = null },
            title = { Text("Delete Alarm Clock Confirmation") },
            text = { Text("This will permanently delete precise system schedule clock \"${alarm.label}\" from your database. Confirm deletion?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteAlarm(alarm)
                        confirmDeleteAlarm = null
                    }
                ) {
                    Text("Delete Permanently", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAlarm = null }) { Text("Cancel") }
            }
        )
    }

    confirmDeleteCron?.let { job ->
        AlertDialog(
            onDismissRequest = { confirmDeleteCron = null },
            title = { Text("Delete Cron Service Confirmation") },
            text = { Text("This will permanently disable background WorkManager task execution for custom cron automation script \"${job.name}\". Continue?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteCronJob(job.id)
                        confirmDeleteCron = null
                    }
                ) {
                    Text("Delete Permanently", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCron = null }) { Text("Cancel") }
            }
        )
    }
}

// ==========================================
// SUB-PANES AND COMPOSABLES
// ==========================================

@Composable
fun CalendarPlannerPane(
    events: List<AgendaEvent>,
    selectedCalendar: Calendar,
    selectedDay: Calendar,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Calendar) -> Unit,
    onDeleteEvent: (Int) -> Unit,
    onEditEventClick: (AgendaEvent) -> Unit,
    onAddEventClick: () -> Unit
) {
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dayFormat = SimpleDateFormat("d", Locale.getDefault())

    // Month Calculations
    val gridDays = remember(selectedCalendar) { getMonthDaysGrid(selectedCalendar) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Month Header Navigator
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
            }
            Text(
                text = monthYearFormat.format(selectedCalendar.time),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
            }
        }

        // Weeks title header
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            val weeks = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
            weeks.forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }

        // Month 7x5 or 7x6 grid
        Box(modifier = Modifier.height(260.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(gridDays) { calendarCell ->
                    if (calendarCell == null) {
                        Box(modifier = Modifier.aspectRatio(1f)) // filler space
                    } else {
                        val isSelected = isSameDay(calendarCell, selectedDay)
                        val isCurrentMonth = isSameMonth(calendarCell, selectedCalendar)
                        val dayEvents = events.filter { isSameDayInMillis(it.dateMillis, calendarCell) }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1.2f)
                                .background(
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        isSameDay(calendarCell, Calendar.getInstance()) -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectDay(calendarCell) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayFormat.format(calendarCell.time),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                                if (dayEvents.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        dayEvents.take(3).forEach { ev ->
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(
                                                        color = if (ev.colorTag == "Secondary") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape
                                                    )
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Daily Ledger List
        val selectedDayFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDayFormat.format(selectedDay.time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onAddEventClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Calendar Event")
            }
        }

        val todayEvents = events.filter { isSameDayInMillis(it.dateMillis, selectedDay) }

        if (todayEvents.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No appointments scheduled for today.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todayEvents) { item ->
                    val hourFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.colorTag == "Secondary") MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Time: ${hourFormat.format(Date(item.dateMillis))} (${item.durationMin} mins)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                if (item.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(item.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onEditEventClick(item) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Event", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDeleteEvent(item.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Event", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmManagersPane(
    alarms: List<AlarmInfo>,
    onToggleAlarm: (AlarmInfo) -> Unit,
    onDeleteAlarm: (AlarmInfo) -> Unit,
    onEditAlarmClick: (AlarmInfo) -> Unit,
    onAddAlarmClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Exact Alarm Clocks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Sub-second wakeups bypassing Standby Mode", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(
                onClick = onAddAlarmClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Alarm")
            }
        }

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No precise system alarms scheduled.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alarms) { alarm ->
                    val clockFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (alarm.isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = clockFormat.format(Date(alarm.timeMillis)),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    ),
                                    color = if (alarm.isActive) MaterialTheme.colorScheme.onSurface else Color.Gray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(alarm.label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = alarm.isActive,
                                    onCheckedChange = { onToggleAlarm(alarm) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { onEditAlarmClick(alarm) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Alarm", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { onDeleteAlarm(alarm) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Alarm", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CronSchedulerPane(
    jobs: List<CronJobInfo>,
    logs: List<CronLog>,
    onToggleCron: (CronJobInfo) -> Unit,
    onDeleteCron: (CronJobInfo) -> Unit,
    onEditCronClick: (CronJobInfo) -> Unit,
    onRunOnDemand: (Int) -> Unit,
    onAddCronClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Cron Background Engine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("WorkManager period scheduling tasks", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(
                onClick = onAddCronClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Cron")
            }
        }

        // Active Cron jobs
        Text("Registered Cron Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        Box(modifier = Modifier.height(180.dp)) {
            if (jobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Cron configurations present.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(jobs) { job ->
                        val dateFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(job.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text("Expression: ${job.cronExpression}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    val lastRunText = if (job.lastRunMillis == 0L) "Never run" else "Last run: ${dateFmt.format(Date(job.lastRunMillis))}"
                                    Text(lastRunText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when (job.status) {
                                                "SUCCESS" -> Color(0xFFE8F5E9)
                                                "FAILED" -> Color(0xFFFFEBEE)
                                                else -> Color(0xFFECEFF1)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = job.status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (job.status) {
                                            "SUCCESS" -> Color(0xFF2E7D32)
                                            "FAILED" -> Color(0xFFC62828)
                                            else -> Color.Gray
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { onRunOnDemand(job.id) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Run Instantly", tint = MaterialTheme.colorScheme.primary)
                                }
                                Switch(
                                    checked = job.isActive,
                                    onCheckedChange = { onToggleCron(job) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { onEditCronClick(job) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Cron", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { onDeleteCron(job) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Cron", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Diagnostic logs
        Text("Telemetry System Console Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Console Idle. Launch workers to view outputs.", color = Color(0xFF808080), fontFamily = FontFamily.Monospace)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(logs) { log ->
                        val logDate = SimpleDateFormat("HH:mm:ss.S", Locale.getDefault()).format(Date(log.runTimeMillis))
                        val statusColor = if (log.status == "SUCCESS") Color(0xFF4CAF50) else Color(0xFFF44336)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "[$logDate] ",
                                color = Color(0xFF9E9E9E),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "[${log.status}] ",
                                color = statusColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "(${log.durationMs}ms) ${log.message}",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// MODALS / POPUP BUILDERS
// ==========================================

@Composable
fun AgendaColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    val presets = listOf(
        "#4CAF50" to "Green",
        "#2196F3" to "Blue",
        "#9C27B0" to "Purple",
        "#E91E63" to "Pink",
        "#FF9800" to "Orange",
        "#009688" to "Teal",
        "#F44336" to "Red",
        "#008080" to "Teal Accent",
        "#5D4037" to "Brown",
        "#607D8B" to "Grey"
    )

    var customHexText by remember { mutableStateOf(selectedColor.replace("#", "")) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Event Visual Accent Tag", style = MaterialTheme.typography.titleSmall)
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.take(5).forEach { (hex, name) ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                        .clickable {
                            onColorSelected(hex)
                            customHexText = hex.replace("#", "")
                        }
                        .padding(2.dp)
                ) {
                    if (selectedColor.equals(hex, ignoreCase = true)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.drop(5).forEach { (hex, name) ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                        .clickable {
                            onColorSelected(hex)
                            customHexText = hex.replace("#", "")
                        }
                        .padding(2.dp)
                ) {
                    if (selectedColor.equals(hex, ignoreCase = true)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = customHexText,
            onValueChange = { 
                customHexText = it
                if (it.length == 6) {
                    try {
                        val parsed = "#$it"
                        android.graphics.Color.parseColor(parsed)
                        onColorSelected(parsed)
                    } catch (e: Exception) {}
                }
            },
            leadingIcon = { Text("#") },
            label = { Text("Custom Color Hex Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LeafletMapViewPane(
    events: List<AgendaEvent>,
    onAddEventAt: (Double, Double) -> Unit,
    onEditEvent: (Int) -> Unit
) {
    val context = LocalContext.current
    var diagnosticMode by remember { mutableStateOf(0) }
    val repo = remember { SettingsRepository(context) }
    val defaultLat by repo.mapDefaultLatitude.collectAsState(initial = 48.8566)
    val defaultLng by repo.mapDefaultLongitude.collectAsState(initial = 2.3522)
    val defaultZoom by repo.mapDefaultZoom.collectAsState(initial = 12f)
    val defaultLayer by repo.mapLastLayerType.collectAsState(initial = 1)
    val mapEngineType by repo.mapEngineType.collectAsState(initial = 0)
    val googleMapsApiKey by repo.googleMapsApiKey.collectAsState(initial = "")

    val tileUrl = when(defaultLayer) {
        2 -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
        else -> "https://tile.openstreetmap.org/{z}/{y}/{x}.png"
    }

    // Build JSON event markers
    val jsonEventsBuilder = StringBuilder("[")
    events.filter { it.latitude != null && it.longitude != null }.forEachIndexed { idx, ev ->
        if (idx > 0) jsonEventsBuilder.append(",")
        jsonEventsBuilder.append("""
            {
                "id": ${ev.id},
                "title": "${ev.title.replace("\"", "\\\"")}",
                "notes": "${ev.notes.replace("\"", "\\\"")}",
                "lat": ${ev.latitude},
                "lng": ${ev.longitude},
                "color": "${ev.colorTag}"
            }
        """.trimIndent())
    }
    jsonEventsBuilder.append("]")
    val markersJson = jsonEventsBuilder.toString()

    val mapHtml = remember(defaultLat, defaultLng, defaultZoom, defaultLayer, markersJson, mapEngineType, googleMapsApiKey, diagnosticMode) {
        if (diagnosticMode == 1) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                    body { font-family: sans-serif; padding: 16px; background: #FFF9C4; color: #333; line-height: 1.4; }
                    h1 { color: #F57F17; font-size: 18px; margin-top: 0; }
                    .box { border: 1px solid #FBC02D; padding: 10px; background: #FFFde7; border-radius: 4px; margin-bottom: 10px; }
                    button { background: #F57F17; color: white; border: none; padding: 10px 14px; border-radius: 4px; font-weight: bold; cursor: pointer; }
                </style>
            </head>
            <body>
                <h1>🧪 Test Page 1: JS-Bridge Check</h1>
                <div class="box">
                    <p><b>Page URL:</b> <span id="url-text">Checking...</span></p>
                    <p><b>Kotlin Bridge Status:</b> <span id="bridge-status">Checking...</span></p>
                </div>
                <div class="box">
                    <p>Verify callback communication: Click below to dispatch coordinate simulation to Kotlin.</p>
                    <button onclick="testBridge()">Simulate addEventAt(1.23, 4.56)</button>
                </div>
                <script>
                    console.log("Diagnostic Test Page 1: Initializing.");
                    document.getElementById('url-text').innerText = window.location.href;
                    var bridgeExists = (typeof window.AndroidBridge !== 'undefined');
                    document.getElementById('bridge-status').innerText = bridgeExists ? "✅ Present" : "❌ Not found";
                    if (!bridgeExists) {
                        console.error("Javascript Error: window.AndroidBridge is undefined!");
                    } else {
                        console.log("Javascript Success: bridge registered.");
                    }
                    function testBridge() {
                        if (bridgeExists) {
                            console.log("Diag: dispatching select coordinates to kotlin.");
                            window.AndroidBridge.addEventAt(1.23, 4.56);
                        } else {
                            alert("Native bridge unregistered.");
                        }
                    }
                </script>
            </body>
            </html>
            """.trimIndent()
        } else if (diagnosticMode == 2) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                    body { font-family: sans-serif; padding: 16px; background: #E3F2FD; color: #333; line-height: 1.4; }
                    h1 { color: #1565C0; font-size: 18px; margin-top: 0; }
                    .box { border: 1px solid #90CAF9; padding: 10px; background: #FFF; border-radius: 4px; margin-bottom: 10px; }
                </style>
            </head>
            <body>
                <h1>🧪 Test Page 2: CDN Network Reachability</h1>
                <div class="box">
                    <p><b>1. OpenStreetMap Tile Server Connection:</b></p>
                    <p id="osm-status">Testing connection...</p>
                </div>
                <div class="box">
                    <p><b>2. Cloudflare CDN (Leaflet JS/CSS) Reachability:</b></p>
                    <p id="leaflet-status">Testing connection...</p>
                </div>
                <script>
                    console.log("Diagnostic Test Page 2: Initializing.");
                    var img = new Image();
                    img.onload = function() {
                        document.getElementById('osm-status').innerText = "✅ Connection successful (Tiles can load)";
                        console.log("Diag Success: Connection to OpenStreetMap tiles established.");
                    };
                    img.onerror = function() {
                        document.getElementById('osm-status').innerText = "❌ Connection failed (Offline or server blocked)";
                        console.error("Diag Error: OSM tile image failed loading.");
                    };
                    img.src = "https://tile.openstreetmap.org/12/2048/1360.png?diag=" + Date.now();

                    fetch("https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js", { method: 'HEAD', mode: 'no-cors' })
                    .then(function() {
                        document.getElementById('leaflet-status').innerText = "✅ CDN responds (Leaflet SDK accessible)";
                        console.log("Diag Success: Cloudflare Leaflet JS is reachable.");
                    })
                    .catch(function(e) {
                        document.getElementById('leaflet-status').innerText = "❌ Reachability failed (Offline or CDN blocked)";
                        console.error("Diag Error: Leaflet CDN unreachable: " + e.message);
                    });
                </script>
            </body>
            </html>
            """.trimIndent()
        } else if (diagnosticMode == 3) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                    body { font-family: sans-serif; padding: 16px; background: #E8F5E9; color: #333; line-height: 1.4; }
                    h1 { color: #2E7D32; font-size: 18px; margin-top: 0; }
                    .box { border: 1px solid #A5D6A7; padding: 10px; background: #FFF; border-radius: 4px; margin-bottom: 10px; }
                </style>
                <script>
                    var verifyCalled = false;
                    function verifyLeaflet() {
                        if (verifyCalled) return;
                        if (typeof L !== 'undefined') {
                            var leafletCheck = document.getElementById('leaflet-check');
                            var mapCheck = document.getElementById('map-check');
                            if (leafletCheck && mapCheck) {
                                leafletCheck.innerText = "✅ Leaflet L is present. Version: " + L.version;
                                console.log("Diag Success: Leaflet L reference works.");
                                try {
                                    var testMap = L.map('dummy-layout').setView([0,0], 1);
                                    mapCheck.innerText = "✅ SUCCESS (Map instance created)";
                                    console.log("Diag Success: Leaflet Map object successfully instantiated.");
                                    verifyCalled = true;
                                } catch(e) {
                                    mapCheck.innerText = "❌ Creation failed (" + e.message + ")";
                                    console.error("Diag Error: Map instantiation failed: " + e.message);
                                }
                            }
                        } else {
                            var leafletCheck = document.getElementById('leaflet-check');
                            if (leafletCheck) {
                                leafletCheck.innerText = "❌ Leaflet Namespace is undefined!";
                                console.error("Diag Error: 'L' namespace is missing.");
                            }
                        }
                    }

                    document.addEventListener('DOMContentLoaded', verifyLeaflet);
                    window.addEventListener('load', verifyLeaflet);
                    setTimeout(verifyLeaflet, 200);
                    setTimeout(verifyLeaflet, 500);
                    setTimeout(verifyLeaflet, 1000);
                </script>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" onerror="console.error('Diag Error: Leaflet CSS CDN fails to load.')" />
                <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js" onerror="console.error('Diag Error: Leaflet JS CDN fails to load.')" onload="verifyLeaflet()"></script>
            </head>
            <body>
                <h1>🧪 Test Page 3: Leaflet Integrity API Check</h1>
                <div class="box">
                    <p><b>Leaflet Library Native Namespace:</b> <span id="leaflet-check">Checking...</span></p>
                    <p><b>Map DOM Creation:</b> <span id="map-check">Pending Leaflet load...</span></p>
                </div>
                <div id="dummy-layout" style="display:none; width:10px; height:10px;"></div>
                <script>
                    console.log("Diagnostic Test Page 3: Initializing.");
                </script>
            </body>
            </html>
            """.trimIndent()
        } else if (diagnosticMode == 4) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                    body { font-family: sans-serif; padding: 16px; background: #FFF3E0; color: #333; line-height: 1.4; }
                    h1 { color: #E65100; font-size: 18px; margin-top: 0; }
                    .box { border: 1px solid #FFB74D; padding: 10px; background: #FFF; border-radius: 4px; margin-bottom: 10px; }
                    .tile-preview { max-width: 100%; border: 1px solid #ddd; margin: 10px 0; display: block; }
                </style>
            </head>
            <body>
                <h1>🧪 Test Page 4: DOM Image Sizing & Mixed Content Security Check</h1>
                <div class="box">
                    <p><b>Checking image loading from standard tile servers:</b></p>
                    <p id="image-status">Loading image element...</p>
                    <img id="test-tile" class="tile-preview" src="https://tile.openstreetmap.org/12/2048/1360.png" />
                </div>
                <div class="box">
                    <p><b>Image Dimensions Check:</b></p>
                    <p id="size-status">Checking dimensions...</p>
                </div>
                <script>
                    console.log("Diagnostic Test Page 4: Initializing.");
                    var img = document.getElementById('test-tile');
                    img.onload = function() {
                        document.getElementById('image-status').innerText = "✅ Image loaded successfully in DOM.";
                        var w = img.naturalWidth;
                        var h = img.naturalHeight;
                        document.getElementById('size-status').innerText = "✅ DOM Loaded Dimensions: " + w + "x" + h + "px";
                        console.log("Diag Success: DOM Image loaded. Size: " + w + "x" + h);
                    };
                    img.onerror = function() {
                        document.getElementById('image-status').innerText = "❌ Image failed loading in DOM!";
                        document.getElementById('size-status').innerText = "❌ Dimensions: N/A";
                        console.error("Diag Error: DOM Image load failed.");
                    };
                </script>
            </body>
            </html>
            """.trimIndent()
        } else if (diagnosticMode == 5) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; font-family: sans-serif; background: #F3E5F5; }
                    .container { padding: 16px; box-sizing: border-box; }
                    h1 { color: #4A148C; font-size: 18px; margin-top: 0; }
                    .box { border: 1px solid #BA68C8; padding: 10px; background: #FFF; border-radius: 4px; margin-bottom: 10px; }
                    #map-sim { width: 100%; height: 200px; background: #eee; border: 2px dashed #888; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🧪 Test Page 5: Container Sizing & CSS Sandbox Check</h1>
                    <div class="box">
                        <p>We test container height. Leaflet maps fail completely if outer elements have 0 height (often due to % height parent elements).</p>
                        <p><b>Window Inner Size:</b> <span id="win-size">Checking...</span></p>
                        <p><b>Simulated Map Element (#map-sim):</b></p>
                        <ul>
                            <li>Computed Height: <span id="comp-height">Checking...</span></li>
                            <li>Computed Width: <span id="comp-width">Checking...</span></li>
                            <li>Client Sizing: <span id="client-size">Checking...</span></li>
                        </ul>
                    </div>
                </div>
                <div id="map-sim"></div>
                <script>
                    console.log("Diagnostic Test Page 5: Initializing.");
                    function checkSize() {
                        document.getElementById('win-size').innerText = window.innerWidth + "x" + window.innerHeight + "px";
                        var el = document.getElementById('map-sim');
                        var computedStyle = window.getComputedStyle(el);
                        document.getElementById('comp-height').innerText = computedStyle.height;
                        document.getElementById('comp-width').innerText = computedStyle.width;
                        document.getElementById('client-size').innerText = el.clientWidth + "x" + el.clientHeight + "px";
                        console.log("Diag Success: Window size is " + window.innerWidth + "x" + window.innerHeight + ", map-sim is " + el.clientWidth + "x" + el.clientHeight);
                    }
                    window.addEventListener('resize', checkSize);
                    window.addEventListener('load', checkSize);
                    checkSize();
                    setTimeout(checkSize, 200);
                </script>
            </body>
            </html>
            """.trimIndent()
        } else if (diagnosticMode == 6) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; font-family: sans-serif; background: #ECEFF1; }
                    .container { padding: 16px; box-sizing: border-box; display: flex; flex-direction: column; height: 100%; }
                    h1 { color: #37474F; font-size: 18px; margin-top: 0; }
                    .box { border: 1px solid #78909C; padding: 10px; background: #FFF; border-radius: 4px; margin-bottom: 10px; }
                    #touch-pad { flex-grow: 1; background: #CFD8DC; border: 2px dashed #546E7A; min-height: 200px; display: flex; align-items: center; justify-content: center; font-weight: bold; color: #37474F; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🧪 Test Page 6: Gestures & Touch Hijacking Check</h1>
                    <div class="box">
                        <p>Interact with the slate below. Drag, tap, pinch. If events list remains empty, the Android host composables are capturing events before they hit the WebView!</p>
                        <p><b>Last Event:</b> <span id="last-event">No touch yet</span></p>
                        <p><b>Status:</b> <span id="event-stats">Active</span></p>
                    </div>
                    <div id="touch-pad">TOUCH & DRAG SLATE</div>
                </div>
                <script>
                    console.log("Diagnostic Test Page 6: Initializing.");
                    var pad = document.getElementById('touch-pad');
                    var counter = 0;
                    function reportEvent(name, x, y) {
                        counter++;
                        document.getElementById('last-event').innerText = name + " (x: " + Math.round(x) + ", y: " + Math.round(y) + ")";
                        document.getElementById('event-stats').innerText = "Count: " + counter;
                        console.log("Diag Success: Touch recorded - " + name + " [" + x + "," + y + "]");
                    }
                    pad.addEventListener('touchstart', function(e) {
                        var t = e.touches[0];
                        reportEvent('touchstart', t.clientX, t.clientY);
                    });
                    pad.addEventListener('touchmove', function(e) {
                        var t = e.touches[0];
                        reportEvent('touchmove', t.clientX, t.clientY);
                    });
                    pad.addEventListener('touchend', function(e) {
                        reportEvent('touchend', 0, 0);
                    });
                    pad.addEventListener('mousedown', function(e) {
                        reportEvent('mousedown', e.clientX, e.clientY);
                    });
                    pad.addEventListener('mousemove', function(e) {
                        if (e.buttons > 0) {
                            reportEvent('drag/mousemove', e.clientX, e.clientY);
                        }
                    });
                </script>
            </body>
            </html>
            """.trimIndent()
        } else if (diagnosticMode == 7) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                    body { font-family: sans-serif; padding: 16px; background: #F5E0E0; color: #333; line-height: 1.4; }
                    h1 { color: #8C1A1A; font-size: 18px; margin-top: 0; }
                    .box { border: 1px solid #E59898; padding: 10px; background: #FFF; border-radius: 4px; margin-bottom: 10px; }
                </style>
            </head>
            <body>
                <h1>🧪 Test Page 7: Browser Storage & Cookies Capability Check</h1>
                <div class="box">
                    <p><b>Local Storage Check:</b> <span id="ls-status">Checking...</span></p>
                    <p><b>Session Storage Check:</b> <span id="ss-status">Checking...</span></p>
                    <p><b>Cookies Check:</b> <span id="cookie-status">Checking...</span></p>
                    <p><b>Indexed Database (IndexedDB) Access:</b> <span id="idb-status">Checking...</span></p>
                </div>
                <script>
                    console.log("Diagnostic Test Page 7: Initializing.");
                    try {
                        localStorage.setItem('diag_test', 'works');
                        var val = localStorage.getItem('diag_test');
                        document.getElementById('ls-status').innerText = (val === 'works') ? "✅ Read & Write Success" : "❌ Unexpected value: " + val;
                        console.log("Diag Success: LocalStorage is functional.");
                    } catch(e) {
                        document.getElementById('ls-status').innerText = "❌ Failed: " + e.message;
                        console.error("Diag Error: LocalStorage write failed: " + e.message);
                    }

                    try {
                        sessionStorage.setItem('diag_test', 'works_session');
                        var val = sessionStorage.getItem('diag_test');
                        document.getElementById('ss-status').innerText = (val === 'works_session') ? "✅ Read & Write Success" : "❌ Unexpected value: " + val;
                        console.log("Diag Success: SessionStorage is functional.");
                    } catch(e) {
                        document.getElementById('ss-status').innerText = "❌ Failed: " + e.message;
                        console.error("Diag Error: SessionStorage write failed: " + e.message);
                    }

                    try {
                        document.cookie = "diag_cookie=enabled_diag; max-age=60; path=/";
                        var cookies = document.cookie;
                        document.getElementById('cookie-status').innerText = (cookies.indexOf('diag_cookie=enabled_diag') !== -1) ? "✅ Configured successfully" : "❌ Disallowed/Blocked";
                        console.log("Diag Success: Cookies are functional.");
                    } catch(e) {
                        document.getElementById('cookie-status').innerText = "❌ Blocked (" + e.message + ")";
                        console.error("Diag Error: Cookies write failed: " + e.message);
                    }

                    try {
                        var request = indexedDB.open("DiagDatabase", 1);
                        request.onsuccess = function() {
                            document.getElementById('idb-status').innerText = "✅ Interface and Database operations available";
                            console.log("Diag Success: IndexedDB is functional.");
                        };
                        request.onerror = function() {
                            document.getElementById('idb-status').innerText = "❌ Access permission denied";
                            console.error("Diag Error: IndexedDB open failed.");
                        };
                    } catch(e) {
                        document.getElementById('idb-status').innerText = "❌ Interface disabled (" + e.message + ")";
                        console.error("Diag Error: IndexedDB disabled: " + e.message);
                    }
                </script>
            </body>
            </html>
            """.trimIndent()
        } else if (diagnosticMode == 8) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <style>
                    body { font-family: sans-serif; padding: 16px; background: #E0F2F1; color: #333; line-height: 1.4; }
                    h1 { color: #004D40; font-size: 18px; margin-top: 0; }
                    .box { border: 1px solid #4DB6AC; padding: 10px; background: #FFF; border-radius: 4px; margin-bottom: 10px; }
                </style>
            </head>
            <body>
                <h1>🧪 Test Page 8: Geolocation Capabilities & Access Check</h1>
                <div class="box">
                    <p><b>Is Geolocation Property Present?</b> <span id="has-geo">Checking...</span></p>
                    <p><b>Current Location coordinates:</b> <span id="geo-status">Press button to request...</span></p>
                    <button style="margin-top: 5px; padding: 8px 12px; background: #00796B; color: white; border: none; border-radius: 4px; cursor: pointer;" onclick="getGeo()">Request Location</button>
                </div>
                <script>
                    console.log("Diagnostic Test Page 8: Initializing.");
                    var hasGeo = ("geolocation" in navigator);
                    document.getElementById('has-geo').innerText = hasGeo ? "✅ Yes (navigator.geolocation exists)" : "❌ No (Unsupported by browser engine)";
                    
                    function getGeo() {
                        if (!hasGeo) {
                            alert("Geolocation not supported.");
                            return;
                        }
                        document.getElementById('geo-status').innerText = "Requesting details from Android Core...";
                        navigator.geolocation.getCurrentPosition(
                            function(pos) {
                                document.getElementById('geo-status').innerText = "✅ Lat: " + pos.coords.latitude + ", Lng: " + pos.coords.longitude + " (Accuracy: " + pos.coords.accuracy + "m)";
                                console.log("Diag Success: Geolocation reading succeeded.");
                            },
                            function(err) {
                                var codeName = "UNKNOWN";
                                if (err.code === 1) codeName = "PERMISSION_DENIED";
                                else if (err.code === 2) codeName = "POSITION_UNAVAILABLE";
                                else if (err.code === 3) codeName = "TIMEOUT";
                                document.getElementById('geo-status').innerText = "❌ Failed (Error code: " + err.code + " - " + codeName + " - " + err.message + ")";
                                console.error("Diag Error: Geolocation failed: " + err.message);
                            },
                            { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 }
                        );
                    }
                </script>
            </body>
            </html>
            """.trimIndent()
        } else if (mapEngineType == 1) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <script>
                    console.log("HTML Init: Google Maps template loaded. System clock: " + Date.now());
                    window.addEventListener('error', function(e) {
                        console.error("HTML Runtime Error: " + e.message + " @ " + e.filename + ":" + e.lineno);
                    }, true);
                </script>
                <style>
                    body, html, #map {
                        margin: 0; padding: 0; width: 100%; height: 100%; font-family: -apple-system, sans-serif;
                    }
                    #search-box {
                        position: absolute; top: 12px; left: 12px; right: 12px; z-index: 1000;
                        display: flex; background: white; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.15);
                        padding: 6px; gap: 6px;
                    }
                    #search-input {
                        flex-grow: 1; border: none; outline: none; padding: 8px; font-size: 14px; border-radius: 4px;
                    }
                    #search-btn {
                        background: #E91E63; color: white; border: none; padding: 8px 14px;
                        border-radius: 6px; font-weight: bold; font-size: 13px; cursor: pointer;
                    }
                    #search-btn:active {
                        background: #c2185b;
                    }
                </style>
            </head>
            <body>
                <div id="search-box">
                    <input type="text" id="search-input" placeholder="Search address, city..." onkeydown="if(event.key==='Enter') doSearch()" />
                    <button id="search-btn" onclick="doSearch()">Search</button>
                </div>
                <div id="map"></div>
                <script>
                    var map;
                    var tempMarker = null;
                    var tempInfoWindow = null;

                    function initMap() {
                        var myLatLng = {lat: $defaultLat, lng: $defaultLng};
                        map = new google.maps.Map(document.getElementById('map'), {
                            zoom: $defaultZoom,
                            center: myLatLng,
                            zoomControl: true,
                            zoomControlOptions: {
                                position: google.maps.ControlPosition.BOTTOM_RIGHT
                            },
                            mapTypeControl: false,
                            streetViewControl: false,
                            fullscreenControl: false
                        });

                        // Add event markers
                        var events = $markersJson;
                        events.forEach(function(ev) {
                            var color = ev.color || '#4CAF50';
                            var marker = new google.maps.Marker({
                                position: {lat: ev.lat, lng: ev.lng},
                                map: map,
                                title: ev.title,
                                icon: {
                                    path: google.maps.SymbolPath.CIRCLE,
                                    fillColor: color,
                                    fillOpacity: 1,
                                    strokeColor: '#FFFFFF',
                                    strokeWeight: 2,
                                    scale: 8
                                }
                            });

                            var infowindow = new google.maps.InfoWindow({
                                content: "<b>" + ev.title + "</b><br>" + 
                                         ev.notes + "<br>" +
                                         "<button style='margin-top:5px; padding:4px 8px; font-size:11px;' onclick='AndroidBridge.editEvent(" + ev.id + ")'>Edit Event</button>"
                            });

                            marker.addListener('click', function() {
                                infowindow.open(map, marker);
                            });
                        });

                        // Handle map tap to drop a temporary pin to add event
                        map.addListener('click', function(e) {
                            var lat = e.latLng.lat();
                            var lng = e.latLng.lng();
                            
                            if (tempMarker) {
                                tempMarker.setPosition(e.latLng);
                            } else {
                                tempMarker = new google.maps.Marker({
                                    position: e.latLng,
                                    map: map,
                                    draggable: true
                                });
                                
                                tempMarker.addListener('dragend', function() {
                                    var pos = tempMarker.getPosition();
                                    reverseGeocode(pos.lat(), pos.lng());
                                });
                            }
                            
                            reverseGeocode(lat, lng);
                        });
                    }

                    function reverseGeocode(lat, lng) {
                        fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng, {
                            headers: { 'User-Agent': 'FraiseAgendaApp/1.0' }
                        })
                        .then(r => r.json())
                        .then(data => {
                            var name = data.display_name || (lat.toFixed(5) + ', ' + lng.toFixed(5));
                            showTempMarkerPopup(lat, lng, name);
                        })
                        .catch(() => {
                            var name = lat.toFixed(5) + ', ' + lng.toFixed(5);
                            showTempMarkerPopup(lat, lng, name);
                        });
                    }

                    function showTempMarkerPopup(lat, lng, name) {
                        if (!tempInfoWindow) {
                            tempInfoWindow = new google.maps.InfoWindow();
                        }
                        tempInfoWindow.setContent(
                            "<b>Selected Location</b><br>" + name + "<br>" +
                            "<button style='margin-top:5px; background:#4CAF50; color:white; border:none; padding:4px 8px; border-radius:4px;' onclick='AndroidBridge.addEventAt(" + lat + "," + lng + ")'>Schedule Event</button>"
                        );
                        if (tempMarker) {
                            tempInfoWindow.open(map, tempMarker);
                        }
                    }

                    function doSearch() {
                        var query = document.getElementById('search-input').value;
                        if (!query) return;
                        document.getElementById('search-btn').innerText = '...';
                        fetch('https://nominatim.openstreetmap.org/search?format=json&limit=1&q=' + encodeURIComponent(query), {
                            headers: { 'User-Agent': 'FraiseAgendaApp/1.0' }
                        })
                        .then(r => r.json())
                        .then(results => {
                            document.getElementById('search-btn').innerText = 'Search';
                            if (results.length > 0) {
                                var first = results[0];
                                var lat = parseFloat(first.lat);
                                var lng = parseFloat(first.lon);
                                var name = first.display_name;
                                var latLng = new google.maps.LatLng(lat, lng);
                                map.setCenter(latLng);
                                map.setZoom(14);
                                
                                if (tempMarker) {
                                    tempMarker.setPosition(latLng);
                                } else {
                                    tempMarker = new google.maps.Marker({
                                        position: latLng,
                                        map: map,
                                        draggable: true
                                    });
                                    
                                    tempMarker.addListener('dragend', function() {
                                        var pos = tempMarker.getPosition();
                                        reverseGeocode(pos.lat(), pos.lng());
                                    });
                                }
                                
                                showTempMarkerPopup(lat, lng, name);
                            } else {
                                alert("Address not found.");
                            }
                        })
                        .catch(() => {
                            document.getElementById('search-btn').innerText = 'Search';
                            alert("Search failed.");
                        });
                    }
                </script>
                <script src="https://maps.googleapis.com/maps/api/js?key=${googleMapsApiKey}&callback=initMap" async defer onerror="console.error('Failed to load Google Maps script (API key issue or internet failure)')" onload="console.log('Google Maps API script fetched successfully')"></script>
            </body>
            </html>
            """.trimIndent()
        } else {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <script>
                    console.log("HTML Init: Leaflet map template loaded. System clock: " + Date.now());
                    window.addEventListener('error', function(e) {
                        console.error("HTML Runtime Error: " + e.message + " @ " + e.filename + ":" + e.lineno);
                    }, true);

                    var map;
                    var mapInitialized = false;
                    var tempMarker = null;

                    function initMap() {
                        if (mapInitialized) return;
                        if (typeof L === 'undefined') {
                            console.error("Critical: 'L' object is undefined! Map rendering will crash.");
                            return;
                        }
                        var mapContainer = document.getElementById('map');
                        if (!mapContainer) return;

                        mapInitialized = true;
                        console.log("Initializing Leaflet map instance centered on [lat: $defaultLat, lng: $defaultLng]");
                        map = L.map('map', { zoomControl: false }).setView([$defaultLat, $defaultLng], $defaultZoom);
                        console.log("Leaflet map created successfully.");
                        L.control.zoom({ position: 'bottomright' }).addTo(map);

                        L.tileLayer('$tileUrl', {
                            maxZoom: 19,
                            attribution: '© OSM'
                        }).addTo(map);
                        console.log("Leaflet Tile layer set with tileUrl: $tileUrl");

                        // Add event markers
                        var events = $markersJson;
                        events.forEach(function(ev) {
                            var color = ev.color || '#4CAF50';
                            var iconElement = document.createElement('div');
                            iconElement.className = 'custom-marker';
                            iconElement.style.backgroundColor = color;
                            
                            var customIcon = L.divIcon({
                                html: iconElement,
                                className: 'dummy',
                                iconSize: [18, 18],
                                iconAnchor: [9, 9]
                            });

                            var marker = L.marker([ev.lat, ev.lng], { icon: customIcon }).addTo(map);
                            marker.bindPopup(
                                "<b>" + ev.title + "</b><br>" + 
                                ev.notes + "<br>" +
                                "<button style='margin-top:5px; padding:4px 8px; font-size:11px;' onclick='AndroidBridge.editEvent(" + ev.id + ")'>Edit Event</button>"
                            );
                        });

                        // Drop temporary marker on map click
                        map.on('click', function(e) {
                            var lat = e.latlng.lat;
                            var lng = e.latlng.lng;
                            
                            if (tempMarker) {
                                tempMarker.setLatLng([lat, lng]);
                            } else {
                                tempMarker = L.marker([lat, lng], { draggable: true }).addTo(map);
                            }
                            
                            reverseGeocode(lat, lng, function(name) {
                                tempMarker.bindPopup(
                                    "<b>Selected Location</b><br>" + name + "<br>" +
                                    "<button style='margin-top:5px; background:#4CAF50; color:white; border:none; padding:4px 8px; border-radius:4px;' onclick='AndroidBridge.addEventAt(" + lat + "," + lng + ")'>Schedule Event</button>"
                                ).openPopup();
                            });
                        });

                        fixMapSize();
                    }

                    function tryInitMap() {
                        if (typeof L !== 'undefined' && document.getElementById('map')) {
                            initMap();
                        }
                    }

                    function fixMapSize() {
                        if (map) {
                            map.invalidateSize();
                        }
                    }

                    function reverseGeocode(lat, lng, callback) {
                        fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng, {
                            headers: { 'User-Agent': 'FraiseAgendaApp/1.0' }
                        })
                        .then(r => r.json())
                        .then(data => {
                            var name = data.display_name || (lat.toFixed(5) + ', ' + lng.toFixed(5));
                            callback(name);
                        })
                        .catch(() => {
                            callback(lat.toFixed(5) + ', ' + lng.toFixed(5));
                        });
                    }

                    function doSearch() {
                        var query = document.getElementById('search-input').value;
                        if (!query) return;
                        document.getElementById('search-btn').innerText = '...';
                        fetch('https://nominatim.openstreetmap.org/search?format=json&limit=1&q=' + encodeURIComponent(query), {
                            headers: { 'User-Agent': 'FraiseAgendaApp/1.0' }
                        })
                        .then(r => r.json())
                        .then(results => {
                            document.getElementById('search-btn').innerText = 'Search';
                            if (results.length > 0) {
                                var first = results[0];
                                var lat = parseFloat(first.lat);
                                var lng = parseFloat(first.lon);
                                var name = first.display_name;
                                if (map) {
                                    map.setView([lat, lng], 14);
                                    if (tempMarker) {
                                        tempMarker.setLatLng([lat, lng]);
                                    } else {
                                        tempMarker = L.marker([lat, lng], { draggable: true }).addTo(map);
                                    }
                                    tempMarker.bindPopup(
                                        "<b>Found Checkpoint</b><br>" + name + "<br>" +
                                        "<button style='margin-top:5px; background:#4CAF50; color:white; border:none; padding:4px 8px; border-radius:4px;' onclick='AndroidBridge.addEventAt(" + lat + "," + lng + ")'>Schedule Event</button>"
                                    ).openPopup();
                                }
                            } else {
                                alert("Address not found.");
                            }
                        })
                        .catch(() => {
                            document.getElementById('search-btn').innerText = 'Search';
                            alert("Search failed.");
                        });
                    }

                    window.addEventListener('load', fixMapSize);
                    window.addEventListener('resize', fixMapSize);
                    document.addEventListener('DOMContentLoaded', function() {
                        tryInitMap();
                        fixMapSize();
                    });
                    setTimeout(tryInitMap, 100);
                    setTimeout(tryInitMap, 300);
                    setTimeout(tryInitMap, 600);
                    setTimeout(tryInitMap, 1200);
                    setTimeout(tryInitMap, 2500);
                    setInterval(fixMapSize, 1500);
                </script>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" onerror="console.error('Failed to load Leaflet CSS CDN!')" onload="console.log('Leaflet CSS CDN loaded.')" />
                <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js" onerror="console.error('Failed to load Leaflet JS CDN!')" onload="tryInitMap()"></script>
                <style>
                    body, html {
                        margin: 0; padding: 0; width: 100%; height: 100%; font-family: -apple-system, sans-serif;
                    }
                    #map {
                        width: 100vw; height: 100vh;
                        border: 6px solid #4CAF50 !important; /* Green diagnosis border around the map */
                        box-sizing: border-box;
                    }
                    /* Diagnostic borders on each individual GIS tile layer */
                    .leaflet-tile {
                        border: 1px solid #FF5722 !important;
                    }
                    #search-box {
                        position: absolute; top: 12px; left: 12px; right: 12px; z-index: 1000;
                        display: flex; background: white; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.15);
                        padding: 6px; gap: 6px;
                    }
                    #search-input {
                        flex-grow: 1; border: none; outline: none; padding: 8px; font-size: 14px; border-radius: 4px;
                    }
                    #search-btn {
                        background: #E91E63; color: white; border: none; padding: 8px 14px;
                        border-radius: 6px; font-weight: bold; font-size: 13px; cursor: pointer;
                    }
                    #search-btn:active {
                        background: #c2185b;
                    }
                    .custom-marker {
                        width: 14px;
                        height: 14px;
                        border-radius: 50%;
                        border: 2px solid white;
                        box-shadow: 0 0 4px rgba(0,0,0,0.4);
                    }
                </style>
            </head>
            <body>
                <div id="search-box">
                    <input type="text" id="search-input" placeholder="Search address, city..." onkeydown="if(event.key==='Enter') doSearch()" />
                    <button id="search-btn" onclick="doSearch()">Search</button>
                </div>
                <div id="map"></div>
            </body>
            </html>
            """.trimIndent()
        }
    }

    val webView = remember {
        android.webkit.WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
            }
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    consoleMessage?.let {
                        val msg = "Console: ${it.message()} @ L:${it.lineNumber()} of ${it.sourceId()}"
                        when (it.messageLevel()) {
                            android.webkit.ConsoleMessage.MessageLevel.ERROR -> AppLogger.e("LeafletMapViewPane", msg)
                            android.webkit.ConsoleMessage.MessageLevel.WARNING -> AppLogger.w("LeafletMapViewPane", msg)
                            else -> AppLogger.d("LeafletMapViewPane", msg)
                        }
                    }
                    return true
                }
            }
            webViewClient = object : android.webkit.WebViewClient() {
                override fun onReceivedError(
                    view: android.webkit.WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        AppLogger.e("LeafletMapViewPane", "WebView Error: ${error?.description} for URL: ${request?.url}")
                    } else {
                        AppLogger.e("LeafletMapViewPane", "WebView Error for URL: ${request?.url}")
                    }
                }

                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    AppLogger.i("LeafletMapViewPane", "WebView page load completed. URL: $url")
                    val width = view?.width ?: 0
                    val height = view?.height ?: 0
                    AppLogger.d("LeafletMapViewPane", "WebView bounds: ${width}x${height}px")
                    if (width == 0 || height == 0) {
                        AppLogger.w("LeafletMapViewPane", "Warning: WebView width or height is 0. Map rendering might be invisible.")
                    }
                }
            }
            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun addEventAt(lat: Double, lng: Double) {
                    post {
                        onAddEventAt(lat, lng)
                    }
                }

                @android.webkit.JavascriptInterface
                fun editEvent(id: Int) {
                    post {
                        onEditEvent(id)
                    }
                }
            }, "AndroidBridge")
        }
    }

    LaunchedEffect(mapHtml) {
        webView.loadDataWithBaseURL("https://agenda.local/map_view.html", mapHtml, "text/html", "UTF-8", null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )

        // Diagnostic floating control bar overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp, start = 16.dp, end = 16.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔧 WebView Diagnostics (Senior Dev)",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val btnColorSelected = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                val btnColorNormal = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)

                Button(
                    onClick = { diagnosticMode = 0 },
                    colors = if (diagnosticMode == 0) btnColorSelected else btnColorNormal,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Standard Map", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { diagnosticMode = 1 },
                    colors = if (diagnosticMode == 1) btnColorSelected else btnColorNormal,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("1: Bridge", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { diagnosticMode = 2 },
                    colors = if (diagnosticMode == 2) btnColorSelected else btnColorNormal,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("2: Network", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { diagnosticMode = 3 },
                    colors = if (diagnosticMode == 3) btnColorSelected else btnColorNormal,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("3: Leaflet", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { diagnosticMode = 4 },
                    colors = if (diagnosticMode == 4) btnColorSelected else btnColorNormal,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("4: Images", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { diagnosticMode = 5 },
                    colors = if (diagnosticMode == 5) btnColorSelected else btnColorNormal,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("5: Sizing", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { diagnosticMode = 6 },
                    colors = if (diagnosticMode == 6) btnColorSelected else btnColorNormal,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("6: Gestures", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { diagnosticMode = 7 },
                    colors = if (diagnosticMode == 7) btnColorSelected else btnColorNormal,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("7: Storage", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { diagnosticMode = 8 },
                    colors = if (diagnosticMode == 8) btnColorSelected else btnColorNormal,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("8: Geo", fontSize = 10.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun LeafletComposeMap(
    initialLatitude: Double,
    initialLongitude: Double,
    initialZoom: Float,
    layerStyle: Int,
    modifier: Modifier = Modifier,
    onLocationSelected: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    val mapHtml = remember(initialLatitude, initialLongitude, initialZoom, layerStyle) {
        val tileUrl = when(layerStyle) {
            2 -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
            else -> "https://tile.openstreetmap.org/{z}/{y}/{x}.png"
        }
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <script>
                console.log("HTML Init: WebCompose Picker Leaflet template loaded.");
                window.addEventListener('error', function(e) {
                    console.error("HTML Runtime Error: " + e.message + " @ " + e.filename + ":" + e.lineno);
                }, true);
            </script>
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.css" onerror="console.error('Failed to load Picker Leaflet CSS CDN!')" onload="console.log('Picker Leaflet CSS CDN loaded successfully.')" />
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js" onerror="console.error('Failed to load Picker Leaflet JS CDN!')" onload="tryInitPickerMap()"></script>
            <style>
                body, html {
                    margin: 0; padding: 0; width: 100%; height: 100%; font-family: -apple-system, sans-serif;
                }
                #map {
                    width: 100vw; height: 100vh;
                    border: 6px solid #2196F3 !important; /* Blue diagnosis border around the picker map */
                    box-sizing: border-box;
                }
                /* Diagnostic borders on each individual GIS tile layer */
                .leaflet-tile {
                    border: 1px solid #FF5722 !important;
                }
                #search-box {
                    position: absolute; top: 12px; left: 12px; right: 12px; z-index: 1000;
                    display: flex; background: white; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.15);
                    padding: 6px; gap: 6px;
                }
                #search-input {
                    flex-grow: 1; border: none; outline: none; padding: 8px; font-size: 14px; border-radius: 4px;
                }
                #search-btn {
                    background: #E91E63; color: white; border: none; padding: 8px 14px;
                    border-radius: 6px; font-weight: bold; font-size: 13px; cursor: pointer;
                }
                #search-btn:active {
                    background: #c2185b;
                }
            </style>
        </head>
        <body>
            <div id="search-box">
                <input type="text" id="search-input" placeholder="Search address, city..." onkeydown="if(event.key==='Enter') doSearch()" />
                <button id="search-btn" onclick="doSearch()">Search</button>
            </div>
            <div id="map"></div>
            <script>
                var map;
                var mapInitialized = false;
                var marker;

                function initPickerMap() {
                    if (mapInitialized) return;
                    if (typeof L === 'undefined') {
                        console.error("Critical Error: Leaflet namespace 'L' is undefined in Map Picker. Will retry...");
                        return;
                    }
                    mapInitialized = true;
                    console.log("Initializing Map Picker centering on [$initialLatitude, $initialLongitude]");
                    map = L.map('map', { zoomControl: false }).setView([$initialLatitude, $initialLongitude], $initialZoom);
                    L.control.zoom({ position: 'bottomright' }).addTo(map);

                    L.tileLayer('$tileUrl', {
                        maxZoom: 19,
                        attribution: '© OSM'
                    }).addTo(map);

                    marker = L.marker([$initialLatitude, $initialLongitude], { draggable: true }).addTo(map);
                    marker.bindPopup("<b>Select Location</b><br>Drag me or click map.").openPopup();

                    marker.on('dragend', function() {
                        var latlng = marker.getLatLng();
                        updateMarker(latlng.lat, latlng.lng, true);
                    });

                    map.on('click', function(e) {
                        updateMarker(e.latlng.lat, e.latlng.lng, true);
                    });

                    fixMapSize();
                }

                function tryInitPickerMap() {
                    if (typeof L !== 'undefined' && document.getElementById('map')) {
                        initPickerMap();
                    }
                }

                // Auto-resize / invalidate size to prevent blank map issues in WebView on layout/creation updates
                function fixMapSize() {
                    if (map) {
                        map.invalidateSize();
                    }
                }
                window.addEventListener('load', function() {
                    tryInitPickerMap();
                    fixMapSize();
                });
                window.addEventListener('resize', fixMapSize);
                document.addEventListener('DOMContentLoaded', function() {
                    tryInitPickerMap();
                    fixMapSize();
                });
                setTimeout(tryInitPickerMap, 100);
                setTimeout(tryInitPickerMap, 300);
                setTimeout(tryInitPickerMap, 600);
                setTimeout(tryInitPickerMap, 1200);
                setTimeout(tryInitPickerMap, 2500);

                function reportLocation(lat, lng, address) {
                    if (window.AndroidBridge) {
                        try {
                            window.AndroidBridge.selectLocation(lat, lng, address);
                        } catch(e) {}
                    }
                }

                function reverseGeocode(lat, lng, callback) {
                    fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng, {
                        headers: { 'User-Agent': 'FraiseAgendaApp/1.0' }
                    })
                    .then(r => r.json())
                    .then(data => {
                        var name = data.display_name || (lat.toFixed(5) + ', ' + lng.toFixed(5));
                        callback(name);
                    })
                    .catch(() => {
                        callback(lat.toFixed(5) + ', ' + lng.toFixed(5));
                    });
                }

                function updateMarker(lat, lng, shouldGeocode) {
                    if (!marker) return;
                    marker.setLatLng([lat, lng]);
                    if (shouldGeocode) {
                        reverseGeocode(lat, lng, function(name) {
                            marker.setPopupContent("<b>Location:</b><br>" + name).openPopup();
                            reportLocation(lat, lng, name);
                        });
                    } else {
                        reportLocation(lat, lng, lat.toFixed(5) + ", " + lng.toFixed(5));
                    }
                }

                // Search standard address
                function doSearch() {
                    var query = document.getElementById('search-input').value;
                    if (!query) return;
                    document.getElementById('search-btn').innerText = '...';
                    fetch('https://nominatim.openstreetmap.org/search?format=json&limit=1&q=' + encodeURIComponent(query), {
                        headers: { 'User-Agent': 'FraiseAgendaApp/1.0' }
                    })
                    .then(r => r.json())
                    .then(results => {
                        document.getElementById('search-btn').innerText = 'Search';
                        if (results.length > 0) {
                            var first = results[0];
                            var lat = parseFloat(first.lat);
                            var lng = parseFloat(first.lon);
                            var name = first.display_name;
                            if (map) {
                                map.setView([lat, lng], 14);
                            }
                            updateMarker(lat, lng, false);
                            if (marker) {
                                marker.setPopupContent("<b>Found:</b><br>" + name).openPopup();
                            }
                            reportLocation(lat, lng, name);
                        } else {
                            alert("Address not found.");
                        }
                    })
                    .catch(() => {
                        document.getElementById('search-btn').innerText = 'Search';
                        alert("Search failed.");
                    });
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    val webView = remember {
        android.webkit.WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
            }
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    consoleMessage?.let {
                        val msg = "Console: ${it.message()} @ L:${it.lineNumber()} of ${it.sourceId()}"
                        when (it.messageLevel()) {
                            android.webkit.ConsoleMessage.MessageLevel.ERROR -> AppLogger.e("LeafletComposeMap", msg)
                            android.webkit.ConsoleMessage.MessageLevel.WARNING -> AppLogger.w("LeafletComposeMap", msg)
                            else -> AppLogger.d("LeafletComposeMap", msg)
                        }
                    }
                    return true
                }
            }
            webViewClient = object : android.webkit.WebViewClient() {
                override fun onReceivedError(
                    view: android.webkit.WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        AppLogger.e("LeafletComposeMap", "WebView Error: ${error?.description} for URL: ${request?.url}")
                    } else {
                        AppLogger.e("LeafletComposeMap", "WebView Error for URL: ${request?.url}")
                    }
                }

                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    AppLogger.i("LeafletComposeMap", "WebView page load completed. URL: $url")
                    val width = view?.width ?: 0
                    val height = view?.height ?: 0
                    AppLogger.d("LeafletComposeMap", "WebView bounds: ${width}x${height}px")
                    if (width == 0 || height == 0) {
                        AppLogger.w("LeafletComposeMap", "Warning: WebView width or height is 0. Map rendering might be invisible.")
                    }
                }
            }
            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun selectLocation(lat: Double, lng: Double, address: String) {
                    post {
                        onLocationSelected(lat, lng, address)
                    }
                }
            }, "AndroidBridge")
        }
    }

    LaunchedEffect(mapHtml) {
        webView.loadDataWithBaseURL("https://agenda.local/map_picker.html", mapHtml, "text/html", "UTF-8", null)
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { webView },
        modifier = modifier
    )
}

@Composable
fun CalendarEventCreatorDialog(
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, notes: String, hour: Int, min: Int, duration: Int, color: String, latitude: Double?, longitude: Double?, locationName: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var hourText by remember { mutableStateOf("9") }
    var minText by remember { mutableStateOf("00") }
    var durationText by remember { mutableStateOf("30") }
    var selectedColor by remember { mutableStateOf("#4CAF50") }

    var latitude by remember { mutableStateOf<Double?>(initialLatitude) }
    var longitude by remember { mutableStateOf<Double?>(initialLongitude) }
    var locationName by remember {
        mutableStateOf<String?>(
            if (initialLatitude != null) "${String.format(Locale.US, "%.4f", initialLatitude)}, ${String.format(Locale.US, "%.4f", initialLongitude ?: 0.0)}" else null
        )
    }

    var showMapPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Calendar Appointment") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Appointment Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Details & Context") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { hourText = it },
                            label = { Text("Hour (24h)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minText,
                            onValueChange = { minText = it },
                            label = { Text("Minute") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Duration (mins)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (locationName != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { showMapPicker = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Selected Location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(locationName ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    Text("${String.format(Locale.US, "%.5f", latitude ?: 0.0)}, ${String.format(Locale.US, "%.5f", longitude ?: 0.0)}", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = {
                                    latitude = null
                                    longitude = null
                                    locationName = null
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showMapPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick Location on Map")
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    AgendaColorPicker(
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val hr = hourText.toIntOrNull() ?: 12
                        val mn = minText.toIntOrNull() ?: 0
                        val dur = durationText.toIntOrNull() ?: 30
                        onConfirm(title, notes, hr, mn, dur, selectedColor, latitude, longitude, locationName)
                    }
                }
            ) {
                Text("Insert Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showMapPicker) {
        val context = LocalContext.current
        val repo = remember(context) { SettingsRepository(context) }
        val defaultLat by repo.mapDefaultLatitude.collectAsState(initial = 48.8566)
        val defaultLng by repo.mapDefaultLongitude.collectAsState(initial = 2.3522)
        val defaultZoom by repo.mapDefaultZoom.collectAsState(initial = 12f)
        val defaultLayer by repo.mapLastLayerType.collectAsState(initial = 1)

        AlertDialog(
            onDismissRequest = { showMapPicker = false },
            title = { Text("Map GeoPicker Panel") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    LeafletComposeMap(
                        initialLatitude = latitude ?: defaultLat,
                        initialLongitude = longitude ?: defaultLng,
                        initialZoom = defaultZoom,
                        layerStyle = defaultLayer,
                        modifier = Modifier.fillMaxSize(),
                        onLocationSelected = { lat, lng, addr ->
                            latitude = lat
                            longitude = lng
                            locationName = addr
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showMapPicker = false }) {
                    Text("Select Pin Location")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMapPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AlarmCreatorDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, min: Int, label: String) -> Unit
) {
    var label by remember { mutableStateOf("Wake up!") }
    var hourText by remember { mutableStateOf("7") }
    var minText by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Alarm Clock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it },
                        label = { Text("Hour (0-23)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minText,
                        onValueChange = { minText = it },
                        label = { Text("Minute (0-59)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hr = hourText.toIntOrNull() ?: 7
                    val mn = minText.toIntOrNull() ?: 30
                    onConfirm(hr, mn, label)
                }
            ) {
                Text("Schedule Clock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CronCreatorDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, expression: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var expression by remember { mutableStateOf("*/15 * * * *") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Cron Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Media Buffer Cleaner") },
                    label = { Text("Cron Task Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expression,
                    onValueChange = { expression = it },
                    label = { Text("Cron Expression schema") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && expression.isNotBlank()) {
                        onConfirm(name, expression)
                    }
                }
            ) {
                Text("Register Cron")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CalendarEventEditorDialog(
    event: AgendaEvent,
    onDismiss: () -> Unit,
    onConfirm: (title: String, notes: String, hour: Int, min: Int, duration: Int, color: String, latitude: Double?, longitude: Double?, locationName: String?) -> Unit
) {
    val initialCal = Calendar.getInstance().apply { timeInMillis = event.dateMillis }
    var title by remember { mutableStateOf(event.title) }
    var notes by remember { mutableStateOf(event.notes) }
    var hourText by remember { mutableStateOf(initialCal.get(Calendar.HOUR_OF_DAY).toString()) }
    var minText by remember { mutableStateOf(initialCal.get(Calendar.MINUTE).toString()) }
    var durationText by remember { mutableStateOf(event.durationMin.toString()) }
    var selectedColor by remember { mutableStateOf(event.colorTag) }

    var latitude by remember { mutableStateOf<Double?>(event.latitude) }
    var longitude by remember { mutableStateOf<Double?>(event.longitude) }
    var locationName by remember { mutableStateOf<String?>(event.locationName) }

    var showMapPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Calendar Appointment") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Appointment Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Details & Context") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { hourText = it },
                            label = { Text("Hour (24h)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minText,
                            onValueChange = { minText = it },
                            label = { Text("Minute") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Duration (mins)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (locationName != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { showMapPicker = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Selected Location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(locationName ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    Text("${String.format(Locale.US, "%.5f", latitude ?: 0.0)}, ${String.format(Locale.US, "%.5f", longitude ?: 0.0)}", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = {
                                    latitude = null
                                    longitude = null
                                    locationName = null
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showMapPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick Location on Map")
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    AgendaColorPicker(
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val hr = hourText.toIntOrNull() ?: 12
                        val mn = minText.toIntOrNull() ?: 0
                        val dur = durationText.toIntOrNull() ?: 30
                        onConfirm(title, notes, hr, mn, dur, selectedColor, latitude, longitude, locationName)
                    }
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showMapPicker) {
        val context = LocalContext.current
        val repo = remember(context) { SettingsRepository(context) }
        val defaultLat by repo.mapDefaultLatitude.collectAsState(initial = 48.8566)
        val defaultLng by repo.mapDefaultLongitude.collectAsState(initial = 2.3522)
        val defaultZoom by repo.mapDefaultZoom.collectAsState(initial = 12f)
        val defaultLayer by repo.mapLastLayerType.collectAsState(initial = 1)

        AlertDialog(
            onDismissRequest = { showMapPicker = false },
            title = { Text("Map GeoPicker Panel") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    LeafletComposeMap(
                        initialLatitude = latitude ?: defaultLat,
                        initialLongitude = longitude ?: defaultLng,
                        initialZoom = defaultZoom,
                        layerStyle = defaultLayer,
                        modifier = Modifier.fillMaxSize(),
                        onLocationSelected = { lat, lng, addr ->
                            latitude = lat
                            longitude = lng
                            locationName = addr
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showMapPicker = false }) {
                    Text("Select Pin Location")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMapPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AlarmEditorDialog(
    alarm: AlarmInfo,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, min: Int, label: String, isActive: Boolean) -> Unit
) {
    val initialCal = Calendar.getInstance().apply { timeInMillis = alarm.timeMillis }
    var label by remember { mutableStateOf(alarm.label) }
    var hourText by remember { mutableStateOf(initialCal.get(Calendar.HOUR_OF_DAY).toString()) }
    var minText by remember { mutableStateOf(initialCal.get(Calendar.MINUTE).toString()) }
    var isActive by remember { mutableStateOf(alarm.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Alarm Clock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it },
                        label = { Text("Hour (0-23)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minText,
                        onValueChange = { minText = it },
                        label = { Text("Minute (0-59)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                    Text("Set Alarm Active", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hr = hourText.toIntOrNull() ?: 7
                    val mn = minText.toIntOrNull() ?: 30
                    onConfirm(hr, mn, label, isActive)
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CronEditorDialog(
    job: CronJobInfo,
    onDismiss: () -> Unit,
    onConfirm: (name: String, expression: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(job.name) }
    var expression by remember { mutableStateOf(job.cronExpression) }
    var isActive by remember { mutableStateOf(job.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Cron Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Cron Task Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expression,
                    onValueChange = { expression = it },
                    label = { Text("Cron Expression schema") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                    Text("Enable Automation Schedule", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && expression.isNotBlank()) {
                        onConfirm(name, expression, isActive)
                    }
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ==========================================
// VIEWMODEL FACTORIES
// ==========================================

class AgendaViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgendaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AgendaViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ==========================================
// CALENDAR MATHEMATICAL UTILS
// ==========================================

fun getMonthDaysGrid(selectedMonthCalendar: Calendar): List<Calendar?> {
    val grid = mutableListOf<Calendar?>()
    val cal = selectedMonthCalendar.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed sunday to saturday
    for (i in 0 until firstDayOfWeek) {
        grid.add(null) // Empty pads for first week padding
    }

    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (d in 1..maxDays) {
        val cell = cal.clone() as Calendar
        cell.set(Calendar.DAY_OF_MONTH, d)
        grid.add(cell)
    }

    // Pad last week to maintain grid aesthetics
    while (grid.size % 7 != 0) {
        grid.add(null)
    }

    return grid
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun isSameMonth(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
}

fun isSameDayInMillis(millis: Long, targetCal: Calendar): Boolean {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return isSameDay(cal, targetCal)
}
