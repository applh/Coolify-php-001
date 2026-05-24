# AI Agent Applet Complexity & Prompting Guide for Android Multi-App Hub

This guide serves as an authoritative playbook for AI Coding Agents and developers modifying or extending the sub-applets in the Android Multi-App Hub (`repo-android`). It details the complexity ranking of each applet, common failure modes, underlying hazards (e.g., lifecycle states, scoped storage, hardware-bound threading), and precise prompting blueprints.

---

## 1. The AI Agent Complexity Scale

To set boundaries for autonomous modifications and minimize regressions, this hub ranks applets across five distinct tiers:

| Tier | Category | Description | Primary Operational Hurdles |
|---|---|---|---|
| **Tier 1** | **Low** | Pure Compose UI. Synchronous local state binding or basic SharedPreferences key storage. | Minimal. Standard state flow updates. |
| **Tier 2** | **Medium-Low** | Basic integrations with secondary Android APIs; standard background scheduling timers. | Permission-checking, background broadcast lifecycle. |
| **Tier 3** | **Medium** | Lifecycle-connected media streaming (CameraX), scoped I/O directories, dynamic local layouts. | Scoped Storage boundaries, UI-to-Engine rendering loops. |
| **Tier 4** | **High** | Dynamic low-level drivers, sqlite binary readers, parsing metadata, network sandbox isolation. | SQL Injection, dynamic Cursor projections, dynamic script evaluation. |
| **Tier 5** | **Critical** | Dynamic WorkManager orchestration, background stream threading, multitouch gestural matrix layers, AI team processes. | Concurrency locks, memory-bound buffers, resource leaks in background. |

---

## 2. Comprehensive Applet Complexity & Tech Stack Matrix

| Applet Name | Route | Tech Stack / Architecture Key Elements | Primary Hazards | Complexity Tier |
|---|---|---|---|---|
| **Home Hub Screen** | `hub` | Navigation Host, LazyGrids, Drawer controllers | Route desynchronization, drawer lock states | **Tier 1 (Low)** |
| **Settings Applet** | `settings` | Kotlin Flows, `SharedPreferences` / `DataStore` | State stutter, key typings | **Tier 1 (Low)** |
| **Agenda Applet** | `agenda` | SQLite calendars, Local alarms, `AlarmManager` | Permission post-notifications, exact timing | **Tier 2 (Medium-Low)** |
| **Wallpaper Applet** | `wallpaper` | `WallpaperManager` API, bitmap decoding files | Bitmap out-of-memory, context permissions | **Tier 2 (Medium-Low)** |
| **Camera Applet** | `camera` | `CameraX`, surface providers, DNG metadata | Lifecycle binding, viewfinder aspect failures | **Tier 3 (Medium)** |
| **Explorer Applet** | `explorer` | File crawler, byte operations, dual-pane layouts | Target path traversal, hanging thread I/O | **Tier 3 (Medium)** |
| **DB SQLite Applet** | `db` | Dynamic SQLiteDatabase, Pragma engine, custom table viewer | SQL projection leaks, type mapping failure | **Tier 4 (High)** |
| **Browser Applet** | `browser` | Composable `WebView`, download bridges, local download DB | Memory leak in WebView, sandbox bypasses | **Tier 4 (High)** |
| **Backup Manager** | `backup` | Zipping Streams, WorkManager back ground, SAF | Heap exhaustion, partial zip corruptions | **Tier 5 (Critical)** |
| **AI Team Applet** | `ai_team` | Gemini Multimodal API, Gestural zoom Lightbox, dynamic markdown | Multi-touch canvas drift, session JSON mismatch | **Tier 5 (Critical)** |
| **Cronjobs Applet** | `cronjobs` | Dynamic worker router, DB CronJobDao, scrape loops | WorkManager schedule drops, boot timing | **Tier 5 (Critical)** |

---

## 3. Deep Dive: Architectural Profiles & Prompting Blueprints

---

### Tier 1 Applets

#### A. Settings Applet (`settings`)
- **Technical Profile**: Integrates global visual preferences (primary theme color indices, light/dark modes) utilizing Kotlin flow streams mapped to `SettingsRepository` and Jetpack Compose.
- **AI Pitfall / Failure Mode**: Breaking the dynamic stream propagation by creating local non-recollected states within the screen view, leading to UI stutter or settings changes that don't apply system-wide.
- **Safe Developer Pattern**:
  ```kotlin
  val themeMode by repository.themeMode.collectAsState(initial = 0)
  // Ensure changes are dispatched to Dispatchers.IO asynchronously
  scope.launch(Dispatchers.IO) {
      repository.setThemeMode(newMode)
  }
  ```
- **Prompting Blueprint**:
  > "Modify the settings screen to add a toggle for [new limit/preference]. Hook this toggle into the `AppPreferences` or `SettingsRepository` using a state flow pattern. Do not manage this state purely inside the Compose layout; ensure changes write directly to database/preferences on the IO dispatcher and propagate reactive updates instantly."

#### B. Home Hub Screen (`hub`)
- **Technical Profile**: Standard landing destination inside `MainActivity`. Manages standard `ModalNavigationDrawer` on the left side and a complementary tool drawer on the right side.
- **AI Pitfall / Failure Mode**: Violating Route consistency by inserting arbitrary navigation destinations not configured in the routing graphs or introducing layout-breaking hardcoded dimensions that cause card clipping.
- **Safe Developer Pattern**: Always read the existing `applets` configuration list in `MainActivity.kt` and use standard Jetpack Navigation actions to route securely.
- **Prompting Blueprint**:
  > "Add a new launcher card to the main hub. Define a clear navigation route `new_route` in the `applets` array inside `MainActivity.kt`, map a robust custom icon from Material design to it, and hook its click listener onto `navController.navigate` while popping up to the hub destination cleanly to avoid navigation heap leaks."

---

### Tier 2 Applets

#### C. Agenda Applet (`agenda`)
- **Technical Profile**: Stores tasks inside SQLite database files, providing calendar views, list trackers, and localized `AlarmManager` reminders.
- **AI Pitfall / Failure Mode**: Setting raw, imprecise alarms that cause battery draining, triggering scheduling activities directly from the UI thread, or neglecting Android 13 (`POST_NOTIFICATIONS`) prompt rules.
- **Safe Developer Pattern**: Always route scheduling actions to a robust background scheduler helpers and query execution safety flags:
  ```kotlin
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      if (!alarmManager.canScheduleExactAlarms()) {
          // Fallback gracefully
      }
  }
  ```
- **Prompting Blueprint**:
  > "Add an alarm notification trigger to the Agenda task manager. Check for standard permissions (`POST_NOTIFICATIONS` and `SCHEDULE_EXACT_ALARM`) composable-safely first. Integrate a safe fallback logic using `scheduleInexact` if exact alarm capability is denied on Android 12+."

#### D. Wallpaper Applet (`wallpaper`)
- **Technical Profile**: Connects to the local directories, reads user-selected images, decodes them safely into bitmap objects, and pushes them to the system `WallpaperManager` API.
- **AI Pitfall / Failure Mode**: Loading heavy images directly into main memory (causing Out Of Memory crashes), or using hardcoded paths that survive neither system updates nor different device resolutions.
- **Safe Developer Pattern**: Always scale bitmaps prior to passing them to the system managers to conserve device memory:
  ```kotlin
  val options = BitmapFactory.Options().apply { inSampleSize = 4 }
  val bitmap = BitmapFactory.decodeFile(imagePath, options)
  ```
- **Prompting Blueprint**:
  > "Extend the wallpaper changer to dynamically scale high-resolution images to match target screen resolutions. Enforce safe image streaming inside target coroutines, wrapping `WallpaperManager.setBitmap()` within a try-catch catching `IOException` and safety limits."

---

### Tier 3 Applets

#### E. CameraX Applet (`camera`)
- **Technical Profile**: Binds system camera devices dynamically to Compose lifecycles using `ProcessCameraProvider`. Supports advanced overlay guidelines, level indicators, manual exposure, and barcode parsers.
- **AI Pitfall / Failure Mode**: Failing to decouple camera previews from Compose state re-compositions, resulting in persistent viewfinder freezes or camera resource leaks on orientation changes.
- **Safe Developer Pattern**: Wrap lifecycle binds inside clean disposable composable blocks:
  ```kotlin
  DisposableEffect(lifecycleOwner) {
      val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
      onDispose {
          cameraProvider.unbindAll()
      }
  }
  ```
- **Prompting Blueprint**:
  > "Introduce a manual ISO/exposure slider to the Camera screen. Bind the values to the active `camera.cameraControl` reference. Ensure the slider changes do not trigger a complete lifecycle rebuild of the `PreviewView` or rebind `CameraSelector` inside the Compose stack."

#### F. File Explorer Applet (`explorer`)
- **Technical Profile**: Performs direct disk structural crawlers over local storage spaces and handles complex operations like directories zipping, secure batch deletion, sorting, and hidden properties toggles.
- **AI Pitfall / Failure Mode**: Running search operations or directory listing crawlers directly on the Main UI Thread (which triggers ANRs—Application Not Responding dialogs) or ignoring path traversal sequences (`../`).
- **Safe Developer Pattern**:
  ```kotlin
  suspend fun listFilesSecurely(dir: File): List<File> = withContext(Dispatchers.IO) {
      if (!dir.canonicalPath.startsWith(allowedRoot.canonicalPath)) {
          throw SecurityException("Path traversal caught")
      }
      dir.listFiles()?.toList() ?: emptyList()
  }
  ```
- **Prompting Blueprint**:
  > "Add index-based recursive search to the explorer applet. Direct the exploration logic to a background dispatcher (`Dispatchers.IO`). Enforce file-path boundary validation against path-traversal input injection securely."

---

### Tier 4 Applets

#### G. DB SQLite Applet (`db`)
- **Technical Profile**: Interacts with local databases. Offers full dynamic database connections, structural Schema CRUD builders, arbitrary SQL code run terminal engines, CSV/JSON transport bridges, and dynamic row listings.
- **AI Pitfall / Failure Mode**: Hardcoding SQL queries, producing memory-leaking Unclosed database cursors, or falling back to simple class string casting for raw dynamic values (which breaks on binary objects or null fields).
- **Safe Developer Pattern**: Use auto-closing cursors and structured queries:
  ```kotlin
  database.rawQuery(query, null).use { cursor ->
       while (cursor.moveToNext()) {
           // extract dynamically index-by-index
       }
  }
  ```
- **Prompting Blueprint**:
  > "Add a CSV export trigger to the DB SQLite query workspace. Use the cursor's column projections to automatically map headings. Wrap cursor iterations inside a `.use {}` statement block, executing the write pipeline dynamically on `Dispatchers.IO` with an active buffer scale of 8KB."

#### H. Browser Applet (`browser`)
- **Technical Profile**: Utilizes custom `WebView` engines configured with strict safe-play environments, intercepting file download activities, and executing script integrations using secure javascript bridge injections.
- **AI Pitfall / Failure Mode**: Enabling JavaScript without configuring site sandbox constraints, leaking context structures, or omitting the proper destruction of `WebView` elements on exit.
- **Safe Developer Pattern**:
  ```kotlin
  // In the Composable setup
  AndroidView(
      factory = { ctx ->
          WebView(ctx).apply {
              settings.apply {
                  javaScriptEnabled = true
                  allowFileAccess = false
              }
          }
      },
      update = { /* update code */ }
  )
  ```
- **Prompting Blueprint**:
  > "Implement an ad-blocking list interceptor in the Browser's web client. Use `shouldInterceptRequest` within a custom `WebViewClient`. Maintain the list of blacklisted domain rules inside a local memory cache database, checking URLs without slowing down UI loading."

---

### Tier 5 Applets (Critical / Deep Concurrency & Engines)

#### I. Backup Manager Applet (`backup`)
- **Technical Profile**: Packages active directories of SQLite DBs, global pref values, and session media streams directly into singular ZIP structures using buffered stream pipelines. Employs transaction-safe restoration routines, atomic file replacements, and backgrounds schedules.
- **AI Pitfall / Failure Mode**: Exhausting system memory through direct raw buffers loads, partial zipping corruption during application kills, or overwriting active database paths while they are opened.
- **Safe Developer Pattern**: Write to temp buffers first and perform atomic renaming:
  ```kotlin
  val tempFile = File(tempDirectory, "backup_staging.zip")
  ZipOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { zos ->
       // process zipper logic...
  }
  if (!tempFile.renameTo(targetBackupFile)) {
       throw IOException("Failed to finalize backup atomically")
  }
  ```
- **Prompting Blueprint**:
  > "Implement atomic rollback file restoration in the Backup Manager. Create a safe temporary directory, stage extracted files inside it, and perform structural schema validations before calling atomic renaming paths. Implement a rollback switch to recover original states if any error occurs."

#### J. AI Team Applet (`ai_team`)
- **Technical Profile**: Rich UI multimodal console managing markdown parsers, session timeline manifests, background WorkManager diagnostic summarizing routines, and gestural multi-touch zoom and pan picture lightboxes.
- **AI Pitfall / Failure Mode**: Mixing parent layout offset translations with Compose touch pointer inputs, leading to canvas drift, or failing to lock zoom boundaries securely, crashing zoom scales into division by zero.
- **Safe Developer Pattern**:
  ```kotlin
  // Lock scales securely inside limits
  val scale = remember { mutableStateOf(1f) }
  val offset = remember { mutableStateOf(Offset.Zero) }
  
  Modifier.pointerInput(Unit) {
      detectTransformGestures { _, pan, zoom, _ ->
          scale.value = (scale.value * zoom).coerceIn(1f, 5f)
          offset.value += pan * scale.value
      }
  }
  ```
- **Prompting Blueprint**:
  > "Add dynamic scaling to the image zoom lightbox. Isolate the pan and zoom states strictly from parent scroll controllers. Bind boundaries (`scale.coerceIn(1f, 5f)`) and map the offset transform to hardware-accelerated graphics layers using `.graphicsLayer {}` composable filters."

#### K. Cronjobs Applet (`cronjobs`)
- **Technical Profile**: Interacts with the `AlarmManager` and low-level `BroadcastReceiver` entries to dispatch activities under custom intervals. Integrates dynamic route tasks (site scraping triggers, image downloads, camera diagnostics summary snaps).
- **AI Pitfall / Failure Mode**: Registering dynamic routing workers inside classes that undergo sudden system termination, neglecting power-saving limits (Doze mode, App Standby buckets), or breaking database consistency when workers run during active app sessions.
- **Safe Developer Pattern**: Enforce WorkManager constraints to keep execution scheduled:
  ```kotlin
  val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .setRequiresBatteryNotLow(true)
      .build()
  ```
- **Prompting Blueprint**:
  > "Add a background WebScraper task that periodically runs under WorkManager. Build dynamic action filters in `DynamicRouterWorker.kt`. Ensure database locks are avoided by utilizing separate isolated SQLite connections or closing transactions quickly using scoped scopes."

---

## 4. Key Lessons & Prompt Engineering Guidelines

When prompting an AI Coder Agent to work inside the Android Repository, apply these three core patterns to get optimal outputs:

### 1. Concrete Layout Directives
Always guide the AI to manage density and responsiveness:
- Prefer Jetpack Compose's **`LazyVerticalGrid`** or **`BoxWithConstraints`** rather than fixed width/height cards.
- Mention target sizes (`Min touch target of 44.dp`, `maxWidth > 600.dp` checks for columns splitting).

### 2. Thread Safety and Coexistence Rules
Explicitly request:
- All dynamic I/O must run asynchronously inside `CoroutineScope(Dispatchers.IO)`.
- Never access database cursors or low-level APIs on the main thread.
- Direct-mapping of Android resource states to values (Resource identifiers, clean try-catches on System resources).

### 3. Permission & Device API Preconditions
Before modifying any hardware-level feature (Camera, Wallpaper, Alarms, Notifications):
- Prompt to **validate permission states** inside Compose.
- Provide a clear, polished permission-prompt screen to prevent runtime app crashes.

---

## 5. Verification Checklist

Before accepting a generated patch or committing changes, verify the structure:
1. **Linter Check**: Run `lint_applet` to identify syntax omissions or deprecated imports.
2. **Build Success**: Call `compile_applet` to test structural compiles.
3. **No Deprecations**: Ensure no deprecated icons (like `Icons.Filled.ArrowBack` instead of `Icons.AutoMirrored.Filled.ArrowBack`) are written.
4. **No Leak Checks**: Check that all dynamic instances (SQLite connections, WebViews, File Streams) are enclosed in try-with-resources (`use` blocks or dynamic `onDispose` hooks).
