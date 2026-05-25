# Architecture Decisions Record

## 1. Gemini API Integration & Proxy Routing

### Context
The project contains multiple applications designed to interact with the Gemini API, primarily a web frontend (Vue.js) and an Android mobile application (`repo-android`). 

A bug emerged where a `404 NOT_FOUND` error was being thrown when calling the Gemini API, accompanied by a `MissingFieldException` in the Android SDK parsing. 

### Decisions & Corrections

1. **Model Version Upgrades and Verification (`gemini-2.5-flash`)**
   - The system was previously referencing older, deprecated models like `gemini-1.5-flash` and `gemini-1.5-flash-latest`.
   - **Fix**: The model names have been upgraded to `gemini-2.5-flash` across all applications (Android, server-side tasks, and PHP scripts). Deprecated models are no longer supported under standard `v1beta` endpoint configurations, causing 404 errors. Standardizing to `gemini-2.5-flash` resolves this permanently.

2. **Middleware API Server (`server.ts`) for Web Frontend**
   - **Why `server.ts` is involved**: For the Vue web application, the Gemini API is called via a Node.js express middleware router (`server.ts`).
   - **Reason**: The AI Studio platform architecture requires that web browser clients proxy their requests through an internal server to securely inject the protected `GEMINI_API_KEY` environment variable. Pushing the API key to the client's browser would create a security leak.

3. **Direct Client Integration for Android (`repo-android`)**
   - **Decision**: The Android application does **not** rely on `server.ts`. It directly utilizes the Google GenAI SDK for Android (`GenerativeModel`).
   - **Reason**: Mobile app builds handle secrets/keys natively through property injection during the build process, and maintaining absolute privacy by eliminating the middleware server is desirable and possible in the client-side binary. The Web application is subject to different constraints since its code is served directly over the network to a browser.

### Conclusion
- The **web application** uses a proxy model (`server.ts`) for strict security and infrastructure compliance.
- The **Android application** uses a direct client-to-API model for optimal privacy and lower latency.
- Both refer to `gemini-2.5-flash` reliably.

---

## 2. Android Agenda Applet Architectural Choices

### Context
Developing the new **Agenda** (Calendar, Alarm, and Cron) applet under `repo-android` requires system-level hooks that remain reliable and performant without draining resources. 

### Decisions & Justification

1. **Precision Alarms using `AlarmManager`**
   - **Choice**: Use native Android `AlarmManager` with standard exact scheduling methods (`setExactAndAllowWhileIdle` or `setAlarmClock`).
   - **Reason**: Jetpack WorkManager runs periodic tasks with a minimum interval of 15 minutes and can delay executions due to battery saver regimes. Alarms and calendars require exact, second-level accuracy even when the device is in Doze Mode.

2. **WorkManager for Cron Background Tasks**
   - **Choice**: Standard periodic background automation routines corresponding to specific Cron schemas are mapped onto standard `WorkManager` items.
   - **Reason**: Highly complex custom exact-looping implementations can easily get blocked, killed, or cause immense memory/battery drain. Jetpack's `WorkManager` provides OS-native safety guarantees, persistence, execution tracking, and battery-aware conditions (network status, loading status).

3. **Reboot Resilience via `BroadcastReceiver`**
   - **Choice**: Bind a `BootCompletedReceiver` to listen for the standard `android.intent.action.BOOT_COMPLETED` event.
   - **Reason**: The Android OS flushes all scheduled intents in the `AlarmManager` whenever a device shuts down or reboots. Listening to standard system boot signals enables scanning the Room/SQLite store and cleanly rescheduling existing calendar triggers or alarm clocks on start-up.

---

## 3. Dynamic User-Managed Cronjob Framework & Foreground Services

### Context
We introduced a user-facing Cronjob Manager within the Android app, which allows users to dynamically create and manage background tasks (such as taking intermittent photos in the background) via an updated Jetpack Compose UI. This capability is fundamentally different from statically compiled workers.

### Decisions & Justification

1. **Single Router Worker (`DynamicRouterWorker`)**
   - **Choice**: A unified WorkManager `CoroutineWorker` handles various user-defined tasks based on `jobType` passed through input payload arguments, rather than creating specialized worker classes for each job type.
   - **Reason**: Greatly simplifies the scheduling engine since it allows dynamic instantiation of WorkManager constraints and generic interval scheduling handled through a universal Room-based job configuration registry without code-bloat.

2. **Foreground Service Promotion (`setForeground()`) for Hardware Tasks**
   - **Choice**: Any Dynamic Router job executing a hardware-dependent action, such as `CAMERA_CAPTURE`, is programmed to explicitly call `setForeground()` alongside publishing an active foreground service payload matching the `camera` type within `AndroidManifest.xml`. 
   - **Reason**: Android 9+ prevents background apps from accessing sensitive hardware (camera/microphone) to maintain privacy. Promoting the background worker temporarily to a foreground service displays a mandated persistent notification ("Capturing photo") fulfilling Android's security constraints and enabling legitimate headless captures via CameraX use cases.

3. **Room Database as Scheduler Truth Source**
   - **Choice**: Use a dedicated `CronJobDatabase` holding schemas for cron assignments (types, intervals, states).
   - **Reason**: Provides a completely decoupled persistent state that allows standard `BootCompletedReceiver` scripts and `CronJobScheduler` objects to reliably pull unexecuted jobs and resupply them cleanly to WorkManager across process deaths or reboots.

---

## 4. Background Scheduled File Downloader Implementation

### Context
Allowing users to schedule custom background downloads (e.g., wallpapers, weather reports, data syncs, remote configurations) requires robust network-storage routines. We need to prevent Android Out-Of-Memory (OOM) errors and handle standard network dropouts and storage limits gracefully.

### Decisions & Justification

1. **Zero-Copy Byte-Streaming for Downloads**
   - **Choice**: Stream the socket connection response body directly to a local file using a fixed 4KB memory buffer ($O(1)$ memory footprint).
   - **Reason**: Reading full response payloads directly into RAM before writing is a standard contributor to Out-Of-Memory (OOM) crashes in background worker threads, particularly on memory-constrained mobile devices. In-stream piping avoids memory accumulation completely.

2. **WorkManager-Level Network Constraint Coupling**
   - **Choice**: Leverage the `requiresNetwork` database column to configure WorkManager's native `NetworkType.CONNECTED` constraints.
   - **Reason**: Performing manual polling checks for connectivity within the active thread wastes battery and CPU, causing unneeded execution cycles. Enrolling network demands into WorkManager's constraint system guarantees execution only when network criteria are fully met.

3. **Decoupled Database Config Parameters**
   - **Choice**: Add dynamic parameters (`downloadUrl` and `saveFileName`) directly into the Room database entity, mapping them onto optional nullable strings.
   - **Reason**: Decoupling configuration from standard tasks allows the Dynamic Scheduler to remain a generic engine. Users can schedule several distinct download cronjobs pointing to multiple endpoints concurrently.

---

## 5. Consolidated Application-State Backup and Restore System

### Context
When users export settings, they expect a complete recovery of the app's operational state. This is especially true for customized "applets" like alarms, events, cron schedules, and log entries. Previously, only simple Key-Value pairs with DataStore settings were exported.

### Decisions & Justification

1. **Nested Multi-Context Serialization Block**
   - **Choice**: Upgrade the flat JSON export document to a nested multi-database JSON structure. Store preferences under a dedicated `"preferences"` configuration object, SQLite tables (such as events, alarms, and cron configs) under respective arrays, and Room entities (like the new downloader cronjob rules) under a `"room_cron_jobs"` serialization array.
   - **Reason**: Simplifies parsing while providing an elegant structural namespace that isolates each subsystem's concerns within a single payload.

2. **Self-Introspecting SQLite Table Dumper**
   - **Choice**: Utilize standard Android standard `Cursor.getType(...)` introspection inside a generic table-traversal schema loop.
   - **Reason**: Eliminates the code-bloat and maintenance cost of writing custom model-to-JSON serializers for independent SQLite tables. If any tables, columns, or values are altered, the generic introspector dynamically serializes strings, longs, doubles, and null blocks without failing or requiring compile-time logic revisions.

3. **Silent Backward-Compatible Import Routing**
   - **Choice**: Program the backup parser to detect the presence of the `"preferences"` key. If it is missing, treat the JSON document as a legacy flat key-value map.
   - **Reason**: Ensures that users with existing settings files generated on earlier versions of the software can import them successfully without causing app crashes or configuration lockouts.

4. **Post-Import WorkManager Resynchronization**
   - **Choice**: Explicitly invoke `CronJobScheduler.syncJobsFromDatabase(context)` immediately after the Room database rows are completely cleared and restored from an exported JSON document.
   - **Reason**: Restoring database parameters is not enough for active schedules. Forcing an active resynchronization scan triggers background system registrations, meaning that background file downloads, notifications, and scheduled capturing scripts re-enroll in WorkManager automatically without requiring user interactions or a phone reboot.

---

## 6. Advanced AR Viewfinder, Native Perspective Rectification & Multi-Format Slices

### Context
Upgrading the local Camera applet to robust AR scanner capability requires:
1. Translating raw vision coordinates from high-speed `ImageAnalysis` frames into physical screen viewport coordinates.
2. Drawing responsive, interactive framing feedback (glow anchors and scanning bars) without causing rendering jitter or UI bottlenecks.
3. Flat-projecting angled, skewed boundaries of photographed document pages into clean, flat rectangles (perspective correction).
4. Providing flexible output format choices in user options (e.g., JPEG, PNG, WebP, MP4, MKV, WebM) aligned with Scoped Storage file-system conventions.

### Decisions & Justification

1. **Flexible Coordinate Mapping (`ARCoordinateTranslator`)**
   - **Choice**: Implemented a standalone, lightweight aspect-ratio coordinate scaling translator. It maps ML Kit Vision and analysis coordinates into screen pixels while respecting camera lens source positions (front versus rear mirror flips) and variable sensor rotations.
   - **Reason**: Standard viewfinders operate on center-crop or center-fit scales. Translating coordinates without scale adaptations draws offset bounding boxes. Native scaling mapping ensures high-accuracy overlay centering.

2. **Glow Bracket Holographic Interception Overlays (`ARScanOverlay` & `ARDocumentOverlay`)**
   - **Choice**: Configured specialized, hardware-accelerated Canvas composables drawing vector framing lines directly above the preview stream.
   - **Reason**: Using traditional layouts or nested layout elements to construct tracking rectangles causes layout pass overhead and sluggish user experiences. Jetpack Compose's vector `Canvas` renders path lines on the GPU with negligible resource draw.

3. **Zero-Dependency Vector Flat Projection (`SimplePerspectiveEngine`)**
   - **Choice**: Coded an elegant Poly-to-Poly matrix projection engine utilizing Android's native graphics pipeline (`android.graphics.Matrix.setPolyToPoly`).
   - **Reason**: Avoids pulling massive, heavy external dependency libraries like OpenCV which drastically increase the final APK size of the mobile package. Exploiting native, low-level Android hardware vector projections allows on-the-go perspective warping under $O(1)$ memory loads.

4. **Multi-Format Media Exporters & Scoped Storage Integration**
   - **Choice**: Extended `takePhoto` and `startVideoRecording` to support target format configurations (Images: JPEG, PNG, WebP; Videos: MP4, MKV, WebM) utilizing conditional MediaStore insertion records.
   - **Reason**: Integrates seamlessly with Android's modern Sandboxed layout and Scoped Storage configurations. Assigning matching MIME types and extensions depending on config states guarantees successful registrations with systemic indexers, making photo assets discoverable across external file-explorer and gallery apps instantly.
