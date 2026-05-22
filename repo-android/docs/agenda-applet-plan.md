# Android Agenda (Calendar, Alarm, & Cron Scheduler) Applet Plan

**Objective**: Extend the Android Multi-App Hub by introducing the **Agenda** applet (`AgendaScreen.kt`, `AgendaViewModel.kt`, and native Android alarm/background schedulers). This tool integrates a visual offline-first interactive calendar planner, a high-precision real-time alarm system resilient to reboots, and a robust background task scheduling harness mapping Cron schemas.

---

## 1. Architectural Highlights & Tech Stack

To ensure bulletproof reliability for tasks, alarms, and background work, the applet utilizes the core Android platform APIs alongside Jetpack Compose MVVM architecture.

```
                    ┌────────────────────────┐
                    │  AgendaViewModel (Row)  │
                    └───────────┬────────────┘
                                │ State Flow
                                ▼
                    ┌────────────────────────┐
                    │    AgendaScreen UI     │
                    └───────────┬────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│  CalendarX   │        │ AlarmManager │        │ WorkManager  │
│  Month/Day   │        │ Engine       │        │ Cron Engine  │
│  State Grid  │        │ (Exact Clock)│        │ (Background) │
└──────────────┘        └──────────────┘        └──────────────┘
```

- **Scheduler Engines**:
  - **AlarmManager Service** for sub-second precise temporal events (alarms, timers), bypassing system sleep policies.
  - **WorkManager Service** for resilient, battery-aware background tasks executing parsed cron triggers.
- **Underlying Database**: A standalone local database table `agenda_events` and `cron_jobs` (scaffolded via standard Room or direct SQLite dynamic layers mapped in the hub) to persist tasks and history.
- **Event Broadcasters**: Native `BroadcastReceiver` hooks to re-register active alarms upon system boot completes.
- **Theme and Layout**: Tailored Jetpack Compose layouts ensuring high WCAG contrast, subtle micro-animations for days transitions, and elegant space structures using off-whites and slate tones.

---

## 2. Core Features (The Requested Scope)

### A. Calendar Features
- **Visual Month Grid**: A fluid calendar view utilizing a 7x6 custom Grid. Days with scheduled events are marked with minimalist colored accent dots.
- **Agenda Ledger List**: A chronologically structured daily ledger placed below the month view. Swiping down on days opens a full-screen Day view showing hour-by-hour grids.
- **Dynamic Event Creator**: An interactive form to catalog new appointments details (Title, Notes, Target Date, Duration, and custom Color Tags).
- **Interactive Reminders**: Link direct notification guidelines up to 15 minutes prior to any calendar event trigger.

### B. Precision Alarm Features
- **Precision Scheduling**: Utilizing Android’s `AlarmManager` with specialized policies (`setAlarmClock` or `setExactAndAllowWhileIdle`) targeting exact wakeups bypassing system standby / Doze Mode.
- **State System Surviving Reboots**: A dedicated BroadcastReceiver (`BootCompletedReceiver`) listening for standard intent `ACTION_BOOT_COMPLETED` to scan the local SQLite database and restore any active alarms on power-up.
- **Ringer & Vibration Looper Overlay**: A custom full-screen alarm trigger layout (`AlarmActiveActivity` or Dialog) activating audio via standard `RingtoneManager` along with custom vibro-patterns via `Vibrator` / `VibrationEffect`.
- **Intelligent Controls**: Provide standard drag-to-dismiss rings or snooze buttons rescheduling custom timers.

### C. Cron Features (Background Schedulers)
- **Cron Definition Parser**: Build or bundle a simple standard cron string reader mapping sequences (e.g. `*/15 * * * *` for every 15 minutes) into millisecond delays and intervals.
- **Harnessing WorkManager**: Convert periodic crons into registered WorkManager `PeriodicWorkRequest` items containing native constraints (e.g. `NetworkType.CONNECTED` or `RequiresCharging`). Note: Android enforces a strict **15-minute minimum interval** for all periodic tasks using WorkManager.
- **Telemetry Console Tracker**: Run-history database logs showing past executables, execution time/latencies, network states, and exit statuses (Success, Warning, Failed).
- **On-Demand Running**: A manual play button next to cron entries running tests synchronously inside immediate background worker threads.
- **Pre-Configured Background Automation Routines**:
  - **Cache Sweeper & Trash Purger**: Run on battery-friendly conditions (e.g. every 6 hours) to clear thumbnail caches, old log files, and intermediate media exports older than 10 minutes.
  - **Auto-Backup Generator**: Daily automated JSON snapshot exports of the active calendar schedules, alarm states, and cron configurations, placing backups in secure sandboxed Scoped Storage.
  - **Database Optimizer & Log Vacuum**: Weekly housekeeper that deletes expired log telemetry statements (older than 30 days) and issues sqlite `VACUUM` statements to release unused disk sectors.
  - **Media Integrity Warden**: Daily validation checking recorded files in files/MediaStore directory, updating database references, and parsing metadata values.
  - **System Resource Watchdog**: Real-time throttling based on system health checks (pausing background transfers or batch operations if battery is below 20% or thermal throttling is active).

---

## 3. Recommended Professional Features

These features elevate the Agenda applet from a basic schedule tool into an advanced system utility:

### 1. Google Calendar / CalDAV Sync Relay
- **Bi-Directional Bridge**: Map standard content providers to read local Google Calendar provider folders or CalDAV calendars with active syncing triggers.
- **Conflict Resolver**: Visual split-screen resolve wizard to handle calendar events overlap or duplicate task records.

### 2. Event-Driven Action Routines (Macro Automation)
- **Macro Command Executor**: Allow users to bind Cron jobs or Alarm triggers to run distinct on-device shell routines or system settings (e.g., automatically toggle Silent Mode during a calendar event, or trigger a webhook request when a task runs).
- **Automation Log**: View-system showing logs of automation scripts and action cascades launched directly by background jobs.

### 3. Integrated System Periodic Core Tasks
- **EXIF Tag Enrichment Service**: Background worker that matches captured media with current geoparser/location cache tags, and appends locations post-capture.
- **Precision Calendar Wakeup Notifier**: High-precision `AlarmManager` helper that wakes up exactly at sunrise or user-configured times (e.g. 7:00 AM) to build a custom local notification ledger summarizing all daily calendar items.

---

## 4. UI / UX Layout & Composition

The layout leverages a responsive mobile view with rich high-contrast theme variations.

```
┌────────────────────────────────────────────────────────┐
│  [Agenda Hub]                         🕒 Next: 09:30   │
├────────────────────────────────────────────────────────┤
│  ◀  May 2026  ▶                                        │
│  Su  Mo  Tu  We  Th  Fr  Sa                            │
│  --  --  --  --   1   2   3                            │
│   4   5   6   7   8   9  10                            │
│  11  12  13  14  [15] 16  17   ◄-- Selected Month grid │
│  18  19  20  21  22  23  24                            │
│  25  26  27  28  29  30  31                            │
├────────────────────────────────────────────────────────┤
│  DAILY EVENTS (May 15)                                 │
│  ○ 09:00 - UI Align Audit                              │
│  ● 14:00 - Client Sync Demo (Green Label)             │
├────────────────────────────────────────────────────────┤
│  Tabs: [ Calendar ]   [ Alarms ]   [ Cron Diagnostics ]│
└────────────────────────────────────────────────────────┘
```

---

## 5. Technical Implementation Blueprint

### A. Alarm Receiver and Boot Completed Receiver Setup
Configure permissions in the manifest (`AndroidManifest.xml`):
```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.VIBRATE" />
```

`BootCompletedReceiver.kt` sample declaration:
```kotlin
package com.example.cameraxapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Initiate background routine to restore alarms
            val serviceIntent = Intent(context, RescheduleAlarmsService::class.java)
            context.startService(serviceIntent)
        }
    }
}
```

### B. Precision Alarm Triggering
`AlarmScheduler.kt` implementation design pattern:
```kotlin
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarmId: Int, triggerTimeMs: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Exact scheduling bypassing Doze Mode
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMs,
            pendingIntent
        )
    }
}
```

### C. Cron Runner utilizing WorkManager
`CronWorker.kt` background execution frame:
```kotlin
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CronWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val cronTaskId = inputData.getString("CRON_TASK_ID") ?: return Result.failure()
        return try {
            // Execute the system automation routine here (e.g. sync directories, clear caches)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

---

## 6. Unified Full CRUD Operations Specification

To ensure a seamless administrative experience matching professional patterns, the Agenda system features full Create, Read, Update, and Delete (CRUD) pipelines for all three underlying entities: Calendar Events, Exact Alarms, and Cron Tasks.

### A. SQLite Storage Updates API (`AgendaDatabaseHelper.kt`)

We must implement high-performance, synchronous SQLite transaction mappings to write entity updates safely:

```kotlin
// 1. Calendar Event Updates
fun updateEvent(id: Int, title: String, notes: String, dateMillis: Long, duration: Int, color: String): Int {
    val db = writableDatabase
    val values = ContentValues().apply {
        put(COL_EVENT_TITLE, title)
        put(COL_EVENT_NOTES, notes)
        put(COL_EVENT_DATE_MILLIS, dateMillis)
        put(COL_EVENT_DURATION, duration)
        put(COL_EVENT_COLOR, color)
    }
    return db.update(TABLE_EVENTS, values, "$COL_EVENT_ID = ?", arrayOf(id.toString()))
}

// 2. Alarm Meta & Time Updates 
fun updateAlarm(id: Int, timeMillis: Long, label: String, isActive: Boolean): Int {
    val db = writableDatabase
    val values = ContentValues().apply {
        put(COL_ALARM_TIME_MILLIS, timeMillis)
        put(COL_ALARM_LABEL, label)
        put(COL_ALARM_IS_ACTIVE, if (isActive) 1 else 0)
    }
    return db.update(TABLE_ALARMS, values, "$COL_ALARM_ID = ?", arrayOf(id.toString()))
}

// 3. Cron Task Metadata & Rule Updates
fun updateCronJob(id: Int, name: String, expression: String, isActive: Boolean): Int {
    val db = writableDatabase
    val values = ContentValues().apply {
        put(COL_CRON_NAME, name)
        put(COL_CRON_EXPRESSION, expression)
        put(COL_CRON_IS_ACTIVE, if (isActive) 1 else 0)
    }
    return db.update(TABLE_CRON_JOBS, values, "$COL_CRON_ID = ?", arrayOf(id.toString()))
}
```

### B. State Management Flow (`AgendaViewModel.kt`)

The ViewModel manages live selection overlays, edit flags, and exact side-effect scheduling on update triggers:

1.  **Selection State Tracking**:
    ```kotlin
    private val _editingEvent = MutableStateFlow<AgendaEvent?>(null)
    val editingEvent = _editingEvent.asStateFlow()

    private val _editingAlarm = MutableStateFlow<AlarmInfo?>(null)
    val editingAlarm = _editingAlarm.asStateFlow()

    private val _editingCron = MutableStateFlow<CronJobInfo?>(null)
    val editingCron = _editingCron.asStateFlow()
    ```

2.  **Temporal Rescheduling Logic & Native Side Effects**:
    *   **Updating a Calendar Event**: When an event's time changes, locate the previous event ID, call `cancelEventAlarm(eventId)` to clear its registered `PendingIntent` from `AlarmManager`, call `updateEvent(...)`, and immediately register a new alarm with the updated milliseconds using `scheduleEventAlarm(...)`.
    *   **Updating an Alarm**: Time updates require dynamic recalculation. If the updated timestamp falls prior to the current system time, roll the trigger forward +24 hours automatically. Cancel the old PendingIntent and establish a fresh one via `setExactAndAllowWhileIdle`.
    *   **Updating Cron Jobs**: If the user alters the text-descriptor cron expression rules, cancel active WorkManager cron items, re-parse the interval sequence, and launch a fresh, verified `PeriodicWorkRequest` under charging and low-battery guard constraints.

### C. UI Composition & Interaction Flows (`AgendaScreen.kt`)

We establish cohesive Compose dialog modals, sheet structures, and swipe triggers to empower editing:

1.  **Form Dialog Sheets (`EditEventDialog` / `EditAlarmDialog` / `EditCronDialog`)**:
    *   Initialize text field states with the properties of the selected entity.
    *   Integrate full form validation: verify title lengths, prevent past alarms generation without rollover, check raw cron expressions parsing validity (e.g. Reject malformed symbols using regex parsing safeguards).
    *   Save and Cancel triggers directly reset the target selection states.
2.  **Interaction Touch-points**:
    *   **Long-Press Gestures**: Long-pressing any card displays a custom Popover overlay menu with options `[Edit Details]` and `[Delete Task]`.
    *   **High-Contrast Action Lists**: Add an editing pencil icon on list elements that opens the dynamic edit overlay on tap.
    *   **Deletion Confirmations**: To avoid physical click errors, tapping Delete presents a clean custom Modal warning before the SQLite deletion executes.

---

## 7. Rollout Phases

1. **Phase 1: Basic Calendar UI & Local Persistence**
   - Compose the monthly grid, detail event entry wizards, and core date models.
   - Scaffold the Event database table configuration rules under native storage.
2. **Phase 2: Exact Alarm Handlers & Boot Listeners**
   - Implement `AlarmManager` helpers, precise broadcast handlers, and Android Manifest bindings.
   - Set up the Wake-Overlay screen activating systems Ringtone/Vibrate drivers safely.
3. **Phase 3: WorkManager Cron Schedulers**
   - Wire standard string-to-timer interpreters for basic cron patterns.
   - Integrate the diagnostics monitoring board showing scheduling execution logs.
4. **Phase 4: Optimization, Sync, and Advanced Automation features**
   - Complete custom swipe integrations for views transitioning.
   - Add macro-action scripts and system conflict handlers.
   - Deploy dynamic and validated row CRUD pipelines across entities.
