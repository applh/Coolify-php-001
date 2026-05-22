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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
    fun addEvent(title: String, notes: String, hours: Int, minutes: Int, duration: Int, color: String) {
        viewModelScope.launch {
            val targetCal = _selectedDay.value.clone() as Calendar
            targetCal.set(Calendar.HOUR_OF_DAY, hours)
            targetCal.set(Calendar.MINUTE, minutes)
            targetCal.set(Calendar.SECOND, 0)
            targetCal.set(Calendar.MILLISECOND, 0)

            val eventId = dbHelper.insertEvent(title, notes, targetCal.timeInMillis, duration, color)
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

    fun updateEvent(id: Int, title: String, notes: String, hours: Int, minutes: Int, duration: Int, color: String) {
        viewModelScope.launch {
            cancelEventAlarm(id)
            val targetCal = _selectedDay.value.clone() as Calendar
            targetCal.set(Calendar.HOUR_OF_DAY, hours)
            targetCal.set(Calendar.MINUTE, minutes)
            targetCal.set(Calendar.SECOND, 0)
            targetCal.set(Calendar.MILLISECOND, 0)

            dbHelper.updateEvent(id, title, notes, targetCal.timeInMillis, duration, color)
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
            if (mins != null && mins >= 15L) {
                intervalMinutes = mins
            }
        }
        val workRequest = PeriodicWorkRequestBuilder<CronWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf("CRON_ID" to cronId))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "CRON_$cronId",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun cancelCronJob(cronId: Int) {
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
fun AgendaScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: AgendaViewModel = viewModel(factory = AgendaViewModelFactory(context))

    val events by viewModel.events.collectAsState()
    val alarms by viewModel.alarms.collectAsState()
    val cronJobs by viewModel.cronJobs.collectAsState()
    val cronLogs by viewModel.cronLogs.collectAsState()
    val selectedCalendar by viewModel.selectedCalendar.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var activeTabIdx by remember { mutableStateOf(0) } // 0: Calendar Planner, 1: Alarm Managers, 2: Cron Schedulers
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddAlarmDialog by remember { mutableStateOf(false) }
    var showAddCronDialog by remember { mutableStateOf(false) }

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
        topBar = {
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
                        onAddEventClick = { showAddEventDialog = true }
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
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title, notes, hour, min, dur, color ->
                viewModel.addEvent(title, notes, hour, min, dur, color)
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
            onConfirm = { title, notes, hour, min, dur, color ->
                viewModel.updateEvent(event.id, title, notes, hour, min, dur, color)
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
fun CalendarEventCreatorDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, notes: String, hour: Int, min: Int, duration: Int, color: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var hourText by remember { mutableStateOf("9") }
    var minText by remember { mutableStateOf("00") }
    var durationText by remember { mutableStateOf("30") }
    var isColorSecondary by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Calendar Appointment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Appointment Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Details & Context") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Duration (mins)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isColorSecondary,
                        onCheckedChange = { isColorSecondary = it }
                    )
                    Text("Apply Secondary Alert Tag (Orange dot)", style = MaterialTheme.typography.bodyMedium)
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
                        val tag = if (isColorSecondary) "Secondary" else "Primary"
                        onConfirm(title, notes, hr, mn, dur, tag)
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
    onConfirm: (title: String, notes: String, hour: Int, min: Int, duration: Int, color: String) -> Unit
) {
    val initialCal = Calendar.getInstance().apply { timeInMillis = event.dateMillis }
    var title by remember { mutableStateOf(event.title) }
    var notes by remember { mutableStateOf(event.notes) }
    var hourText by remember { mutableStateOf(initialCal.get(Calendar.HOUR_OF_DAY).toString()) }
    var minText by remember { mutableStateOf(initialCal.get(Calendar.MINUTE).toString()) }
    var durationText by remember { mutableStateOf(event.durationMin.toString()) }
    var isColorSecondary by remember { mutableStateOf(event.colorTag == "Secondary") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Calendar Appointment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Appointment Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Details & Context") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Duration (mins)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isColorSecondary,
                        onCheckedChange = { isColorSecondary = it }
                    )
                    Text("Apply Secondary Alert Tag (Orange dot)", style = MaterialTheme.typography.bodyMedium)
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
                        val tag = if (isColorSecondary) "Secondary" else "Primary"
                        onConfirm(title, notes, hr, mn, dur, tag)
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
