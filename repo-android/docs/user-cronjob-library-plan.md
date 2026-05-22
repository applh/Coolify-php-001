# Dynamic User-Managed Cronjob Library Implementation Plan

## Objective
Build a flexible, user-facing Cronjob Manager within the Android app that allows users to dynamically create, configure, and manage background tasks. Instead of hardcoding background jobs (like the camera capture), this library provides a generic engine utilizing Android's `WorkManager` backed by a local SQLite (Room) database.

## System Architecture

### 1. Data Layer (Room Database)
A single truth source for all scheduled user jobs.
- **Entity (`CronJobEntity`)**: 
  - `id`: Unique identifier (UUID/String)
  - `jobType`: Enum/String (e.g., `CAMERA_CAPTURE`, `SYNC_DATA`, `CLEANUP`)
  - `intervalMinutes`: Integer (Minimum 15, enforced by `WorkManager`)
  - `isEnabled`: Boolean
  - `requiresNetwork`: Boolean
  - `requiresCharging`: Boolean
  - `lastRunTimestamp`: Long
  - `nextRunTimestamp`: Long
- **DAO (`CronJobDao`)**: Standard CRUD operations (insert, update, delete, get active jobs).

### 2. Job Dispatcher & Scheduler Manager
A centralized Kotlin object or Singleton class (`CronJobScheduler`) to interface with `WorkManager`.
- **`scheduleJob(job: CronJobEntity)`**:
  - Cancels any existing WorkManager task with `job.id` as the unique tag.
  - Generates `Constraints` based on `job.requiresNetwork` and `job.requiresCharging`.
  - Enqueues a `PeriodicWorkRequestBuilder` using the user's `intervalMinutes`.
  - Sets the `jobType` and `id` in the `Data` payload of the WorkRequest to pass to the Worker.
- **`cancelJob(jobId: String)`**: Calls `WorkManager.getInstance(context).cancelUniqueWork(jobId)`.
- **`syncJobsFromDatabase()`**: Called on app startup or Boot (via `BootCompletedReceiver`) to ensure `WorkManager` has cleanly scheduled everything marked `isEnabled = true` in the DB.

### 3. The Universal Worker (`DynamicRouterWorker`)
Instead of one Worker per task, we use a single router worker.
- **`doWork()` execution**:
  1. Reads standard parameters from `inputData` (`jobId`, `jobType`).
  2. Routes the logic using a `when(jobType)` switch statement.
  3. If `jobType == "CAMERA_CAPTURE"`, it performs foreground promotion and captures the image.
  4. If `jobType == "WALLPAPER_CHANGER"`, it runs the wallpaper rotation logic.
  5. Updates the `lastRunTimestamp` in the Room Database upon success.

### 4. UI Library (Jetpack Compose)
A generic Settings/List view for users to manage their cronjobs.
- **`JobManagerScreen`**: Lists all available/configurable jobs.
- **`JobEditorDialog/Screen`**: Allows users to:
  - Toggle the job On/Off.
  - Adjust a slider for the interval (15 mins up to 24 hours).
  - Checkboxes for "Requires Wi-Fi" or "Requires Charging".
- **State Management**: Uses a `ViewModel` (`CronJobViewModel`) observing the Room database flows to keep the UI in sync with actual scheduled states.

## Phased Implementation Strategy

### Phase 1: Core Database & Engine
- Implement the Room Entities and DAOs for Job configuration.
- Build the `CronJobScheduler` wrapper around `WorkManager`.
- Implement testing stubs for `DynamicRouterWorker` doing basic logging.

### Phase 2: Action Implementations
- Migrate the existing Headless `CameraCaptureManager` logic into the generic router.
- Ensure Foreground Service limitations are respected when the generic worker dispatches a "Hardware/Camera" job.

### Phase 3: User Interface
- Create the Jetpack Compose screens for `JobManagerScreen` and settings sliders.
- Connect the UI toggles to update the Database, and immediately call `CronJobScheduler.syncJobsFromDatabase()`.

## Constraints & Considerations
- **15-Minute Minimum**: UI sliders must visibly enforce a 15-minute bottom limit due to Android OS rules.
- **Foreground Service Granularity**: If a user schedules multiple hardware-intensive jobs, the `DynamicRouterWorker` must accurately apply `setForeground` only when required (e.g., camera capture needs it, basic data sync might not).
- **Permissions**: The UI needs a centralized Permission Manager. When a user enables a camera cronjob, the UI must intercept and request runtime permissions before setting `isEnabled = true` in the database.
