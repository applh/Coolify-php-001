# Part 7: Mobile & Multi-Platform Systems (100 Hours)

Bridge the gap between web and mobile with Android and Flutter.

## Modules (10 Hours Each)
- M061: Android Kotlin Basics
- M062: Android Jetpack Compose
- M063: Android Architecture (MVVM)
- M064: Flutter Dart Foundations
- M065: Flutter State Management
- M066: Cross-Platform API Integration
- M067: Mobile Hardware Access
- M068: App Publishing & Distribution
- M069: Embedded Systems Intro
- M070: Project: Cross-Platform App
- M071: Android Build, Gradle & Coolify Deployment

## Practical Labs

### 1. Flutter UI (50h)
**Reference**: `repo-flutter/lib/main.dart`
- **Task**: Explore the declarative UI structure.
- **Exercise**: Build a simple list view that fetches site names from the API.

### 2. Android Lifecycle (40h)
**Reference**: `repo-android/`
- **Task**: Trace an Activity lifecycle.
- **Exercise**: Implement a simple notification trigger in the Android app.

### 3. Android Compiling & Coolify Deployment (30h)
**Reference**: `repo-android/Dockerfile`, `repo-android/build.gradle`
- **Goal**: Understand how to compile a Java/Kotlin Android project using Gradle and deploy it as a standalone containerized app via Coolify.
- **Exercise**: Modify the Android Dockerfile to adjust Gradle heap sizes and deploy the container to distribute the APK.
- **Complexity**: Part 4

### 4. Gemini SDK Error Diagnosing and Model Configuration (20h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`
- **Goal**: Learn how standard client-side error parsing works in the Google Generative AI Android SDK and resolve API version / model mismatch issues.
- **Exercise**: Diagnose API error codes like 404 NOT_FOUND caused by incorrect model identifiers (e.g. `gemini-1.5-flash-001`) and standard serialization crashes related to `GRpcError` missing `details`.
- **Complexity**: Part 3

### 5. Multi-Platform Gemini API Model Discovery and Recovery (10h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`, `server.ts`, `repo-php/class/AIAgent.php`
- **Goal**: Master active model enumeration and troubleshooting using the official Generative Language REST APIs to recover from API deprecation states.
- **Exercise**: Write a custom script to query the active models list (`v1beta/models`) to find replacement models like `gemini-2.5-flash` when ancestral models are deprecated.
- **Complexity**: Part 3

### 6. Android AI Session Recording, Replaying, and Regression Benchmarking (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`
- **Goal**: Learn how to serialize real-time LLM telemetry (inputs, outputs, response latency, token count, estimated cost) into persistent JSON files and build an automated playback regression benchmarker.
- **Exercise**: Implement an offline-first replayer inside Android Jetpack Compose that iterates through archived session prompts, re-queries the Gemini API to measure delta speed/cost variations, and renders side-by-side performance comparisons.
- **Complexity**: Part 5

### 7. Segmented AI Format Selection & Official Imagen 3.0 API Integration (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`
- **Goal**: Understand how to implement custom output format selectors in Jetpack Compose and use the official Imagen 3.0 REST API to generate high-fidelity images with proper JSON payload structure and base64 response decoding.
- **Exercise**: Integrate dynamic aspect-ratio selection (such as 16:9 or 4:3) and extend the custom `Surface` selectors to support the newly selected aspect ratios in the REST payload.
- **Complexity**: Part 3

### 8. Lumina AI / Gemini Canvas: Advanced Image Synthesis & Scoped Storage in Jetpack Compose (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/StorageUtils.kt`
- **Goal**: Master full-stack image generation workflow using standard Gemini Developer API with `gemini-2.5-flash-image` and `gemini-3.1-flash-image-preview` models, managing MVVM UI states, Retrofit requests (including `aspectRatio` and `imageSize` configs), writing files using modern Android Scoped Storage, and implementing secure FileProvider system shares.
- **Exercise**: Expand the design panel to dynamically toggle or slide between Gemini models, integrate interactive inspiration prompt chips, decode response candidates into local bitmaps, stream them to the relative MediaStore path (`Pictures/GeminiCanvas`) under `IS_PENDING` policies, and share them securely using system shares with `Intent.ACTION_SEND`.
- **Complexity**: Part 4

### 9. Dynamic App Color Palette & Lumina AI Theme Swapper (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/ui/theme/Color.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/ui/theme/Theme.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/AppPreferences.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/SettingsScreen.kt`
- **Goal**: Learn how to implement persistent user-selected application-wide themes and design custom high-contrast material color schemes (incorporating custom glowing cyber-neon highlights) in modern Jetpack Compose.
- **Exercise**: Extend the custom `CameraXAppTheme` to allow the user to select amongst three color palettes: Standard Mono-Red, Lumina AI Glowing Aurora, and an additional custom "Sunset Emerald" theme with mint teal and golden accents. Make sure the selection is fully saved in `DataStore` preferences and loads instantly across screen transitions.
- **Complexity**: Part 3

### 10. Centralized Preferences-Driven AI Engine & Billing Dashboard (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/SettingsScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/AppPreferences.kt`
- **Goal**: Understand how to decouple application UI screens from API configuration parameters by utilizing a centralized DataStore-backed preference architecture, and construct interactive request cost/token tracking overlays.
- **Exercise**: Retrieve and bind DataStore flows directly into your screen composables using `collectAsState`, use these saved preferences (API Key, model path, aspect ratio, image size) to issue generative requests seamlessly on button triggers, and display granular metrics outlining generation latency, prompt token count, and calculated server costs.
- **Complexity**: Part 4

### 11. Multimodal Session-First AI Chat Workspace & Interactive Lightbox in Compose (20h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`, `repo-android/docs/ai-team-plan.md`, `repo-android/docs/ai-team-session-management.md`
- **Goal**: Learn how to transition a single-purpose Android applet into a full multimodal chat workspace. Master checkboxes beside action builders, smart default session persistence from historical flat-files manifest registries, and multitouch gestures (pinch-to-zoom/pan) to create custom gallery lightboxes.
- **Exercise**:
  1. Add a custom `Checkbox` beside the main chat submission button in compile-ready state. Integrate conditional routing to issue standard text/markdown responses when unchecked, and image bitmap downloads when checked.
  2. Implement an automatic scan during initialized launch in the `ViewModel` to load the highest-timestamp session from standard `sessions.json` local flat files by default.
  3. Add a plus icon toolbar shortcut to instantiate a brand-new chat workspace by rotating UUID state containers.
  4. Build a custom lightbox Dialog with Compose `PointerInputScope` gestural transformers detecting multitouch pinches, enabling zooming up to 4.0x and scrolling translations dynamically.
- **Complexity**: Part 4

### 12. Offline-First JSON Dialogue Segment Management & Thread Deletion Flows (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`
- **Goal**: Learn how to manage multi-session manifest registries containing individual conversation logs on Android. Understand how thread mutations, UUID generational resets, and dynamic deletions represent highly consistent data states.
- **Exercise**: Extend the custom `AITeamViewModel` to auto-serialize metadata into flat local files upon thread additions or session clears, and build out high-performance file-system prune routines to wipe deleted conversational JSON nodes without blocking UI rendering threads.
- **Complexity**: Part 4

### 13. Dynamic SQLite Database Management & Row CRUD in Jetpack Compose (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/DBScreen.kt`, `repo-android/docs/db-applet-plan.md`
- **Goal**: Master the concepts of dynamic SQLite database file mapping and runtime database manipulation without traditional object-relational mapping (such as Room). Understand how to query metadata tables, dynamic schema descriptors, and translate variable cursors into paginated list templates in Android Jetpack Compose.
- **Exercise**: Implement a dynamic row CRUD system in Jetpack Compose that visualizes table metadata (columns, primary keys, nullability) and renders an input form suited to any chosen schema. Write robust database update statements and integrate user alerts before executing column deletions or tables dropping.
- **Complexity**: Part 4

### 14. Android Agenda Applet: Calendar, AlarmManager, and WorkManager (15h)
**Reference**: `repo-android/docs/agenda-applet-plan.md`
- **Goal**: Master exact system-level background events scheduling in Android using `AlarmManager` and periodic work task routing using standard `WorkManager` workers, tied into custom offline calendar layouts in Jetpack Compose.
- **Exercise**: Implement a calendar events list view linked to an exact `AlarmManager` scheduler that triggers heads-up alarms, create a `BroadcastReceiver` to handle restoring active schedules after device reboots, and build a WorkManager routine mapping cron intervals to background script tasks.
- **Complexity**: Part 4

### 15. WorkManager & AlarmManager Multi-Task Background Suite (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/CronWorker.kt`, `repo-android/IMPLEMENTATION.md`
- **Goal**: Understand how to develop and chain multiple high-utility periodic tasks inside a unified Android WorkManager execution harness (`CronWorker.kt`), while respecting battery, networking, and platform constraints.
- **Exercise**: Create and register three brand-new periodic background automation tasks in `CronWorker.kt`: a weekly database optimizer issuing a `VACUUM` transaction on the SQLite database, a daily media integrity supervisor inspecting saved images and cleaning orphan records, and an environmental resource watchdog rescheduling heavy operations if battery levels fall below the 20% limit.
- **Complexity**: Part 4

### 16. Scheduled Background AI Session Replay & Periodic Task Routing (15h)
**Reference**: `repo-android/docs/ai-team-session-management.md`, `repo-android/app/src/main/java/com/example/cameraxapp/CronWorker.kt`
- **Goal**: Master the integration of local file-based AI session state objects with Android's system background triggers. Understand how to serialize complex cron instructions, fetch saved configurations dynamically on-boot, perform background network actions safely with WorkManager, and notify clients about scheduled updates.
- **Exercise**: Implement custom scheduling properties within the `SessionHeader` schema. Create a periodic subscription toggle in the Jetpack Compose chat view that registers a custom background job in the SQLite `TABLE_CRON_JOBS` index. Extend `CronWorker.kt` to identify scheduled AI sessions, parse raw history files, construct background prompts, dispatch queries to the Gemini API, update JSON threads asynchronously on run completion, and post a push notification.
- **Complexity**: Part 4

### 17. Multi-Entity Agenda Row CRUD & System Alarm Synchronization (20h)
**Reference**: `repo-android/docs/agenda-applet-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/AgendaDatabaseHelper.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`
- **Goal**: Master full Create, Read, Update, and Delete (CRUD) flows on Android SQLite databases alongside high-precision scheduling synchronizations with the operating system layer.
- **Exercise**: Extend the custom SQLite helper to support dynamic update queries for events, alarms, and cron expressions. In Jetpack Compose, build out Edit overlays initialized with selection state details, configure strict field pattern inputs validation, and handle exact side effect callbacks—clearing and rescheduling AlarmManager PendingIntents or restarting WorkManager loops on task modifications.
- **Complexity**: Part 4

### 18. Operational Autopilot AI Agent & Ambient System Prompts Customization (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/CronWorker.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/AppPreferences.kt`
- **Goal**: Learn how to implement customizable system instructions and ambient contextual injections for periodic background execution pipelines that integrate with Gemini chat API.
- **Exercise**: Implement a new DataStore key storing an "Autopilot Persona/Instruction" string. Modify `CronWorker.kt` to query this string on-the-fly when resuming an active AI session rerun job, injecting it as a system prompt directive to mold the tone of the automated response.
- **Complexity**: Part 4

### 19. Android Background Processing & System Autonomy (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`, `repo-android/app/src/main/AndroidManifest.xml`, `repo-android/docs/android-background-guide.md`
**Goal**: Understand the differences between exact scheduling with AlarmManager and deferrable background synchronization via WorkManager. Learn the critical difference between an "App Backgrounded" state and an "App Force Stopped" state caused by users swiping the app away in the Recents tray, which kills all OEM scheduling.
**Exercise**: Implement a scheduled notification using AlarmManager. Test the behavior by simply pressing "Home" versus "Swiping from Recents". Implement a mitigation strategy by analyzing how `BootCompletedReceiver` restores swiped alarms after a reboot, and research the architectural trade-offs of using a persistent Foreground Service to bypass Recents-swipe kills.
**Complexity**: Part 4

### 20. Android Dynamic Cronjob Framework & Foreground Services (20h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/cronjob/`, `repo-android/docs/android-background-guide.md`, `repo-android/docs/user-cronjob-library-plan.md`
- **Goal**: Understand how to develop a generic, database-driven cron engine using WorkManager, Room, and foreground service promotion for hardware access (like CameraX).
- **Exercise**: Explore the `CronJobScheduler` and `DynamicRouterWorker`. Add a new generic job type (e.g., "Network Sync") to the `CronJobDatabase` via Jetpack Compose UI, schedule it using the scheduler wrapper, and verify it executes without requiring a foreground service promotion. Modify the worker router to handle this new type appropriately.
- **Complexity**: Part 5

### 21. Android Jetpack Compose Icon Migration and Deprecation Resolution (10h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/cronjob/CronJobManagerScreen.kt`
- **Goal**: Understand the architectural design patterns of modern material design assets inside Jetpack Compose, specifically the transition from standard `Icons.Filled`/`Icons.Default` collections to unidirectional/internationalized `Icons.AutoMirrored.Filled` equivalents.
- **Exercise**: Learn to spot and resolve Android compiler warnings indicating icon deprecations, modify references from `Icons.Default.ArrowBack` to `Icons.AutoMirrored.Filled.ArrowBack`, and ensure correct package import optimization without leaving orphan imports.
- **Complexity**: Part 2

### 22. Scheduled Background Internet Downloader & Zero-Copy I/O (15h)
- **Reference**: `repo-android/docs/internet-download-cronjob-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/cronjob/`
- **Goal**: Master high-performance background networking, WorkManager schedules with connectivity constraints, and SQLite / Room schema migrations in Android.
- **Exercise**: Extend the local `CronJobEntity` database model to store nullable `downloadUrl` and `saveFileName` configurations. Update `DynamicRouterWorker.kt` to intercept `"HTTP_DOWNLOAD"` request types and implement a buffered streaming routine ($O(1)$ RAM footprint) that writes response bodies byte-by-byte into the app's standard local directory, gracefully retrying on socket timeouts or low storage alerts.
- **Complexity**: Part 4

### 23. Consolidated Backup & Restore of Multi-Database and DataStore States (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AppPreferences.kt`
- **Goal**: Master application-state saving and restoring across multiple storage mediums (Android DataStore, custom SQLiteOpenHelper, Room / SQLite DB) into a unified backup JSON payload.
- **Exercise**: Explore the updated `exportSettings` and `importSettings` implementations in `AppPreferences.kt`. Learn how to use Android SQL Cursor type reflection to generically serialize database columns dynamically without custom schema conversion code. Complete an exercise to safely handle and restore the app's persistent background WorkManager states post-import by triggering a reactive database-resynchronization cycle in `CronJobScheduler`.
- **Complexity**: Part 4

### 24. Jetpack CameraX Advancements: Zoom, Post-Processing Effects, & ML Kit Scanner (20h)
- **Reference**: `repo-android/docs/camera-applet-upgrade-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/CameraScreen.kt`
- **Goal**: Master exact Jetpack CameraX integrations combining gestural interactions (pinch-to-zoom), reactive slider state mapping, custom CPU/GPU pixel processing filters, and real-time offline ML Kit Vision Barcode analyzers. Understand advanced capabilities like the CameraX Extensions API, Concurrent Dual-Camera streaming, and Camera2 interoperability controls.
- **Exercise**: Examine the complete production-grade implementation inside `CameraScreen.kt`. Build custom pinch gestures on the `AndroidView` wrapper, verify post-capture pixel filter translations in `takePhoto`, and implement a new, fifth creative visual filter (such as a dynamic Vignette or Retro VHS noise filter overlay) into the color matrix filtration kernel. For an advanced challenge, build a mockup settings menu to toggle Device OEM Extensions (like HDR and Night Mode) using `ExtensionsManager`, and configure camera-interop bindings to programmatically override the ISO sensitivity based on manual slider states.
- **Complexity**: Part 4

### 25. Responsive Jetpack Compose Camera Layout & Orientation-Aware UX (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/CameraScreen.kt`
- **Goal**: Master orientation-aware UI design patterns in modern Jetpack Compose using `BoxWithConstraints`. Learn how to dynamically adapt complex interfaces, HUD overlays, and button configurations between portrait and landscape modes of a smartphone / tablet.
- **Exercise**: Implement a responsive camera control layer that adapts dynamically. Switch between a portrait ergonomically bottom-centered double row (utilities row and action buttons row) and a landscape sidebar layout. Ensure seamless state retention (e.g., zoom, current flash mode, filters) during orientation changes.
- **Complexity**: Part 3

### 26. Android ZIP-Archive Backup Manager & Automatic Cron Schedulers (15h)
- **Reference**: `repo-android/docs/backup-manager-plan.md`
- **Goal**: Master multi-faceted local data saving and restoring, zip-archive buffering streams, SQLite WAL flush sequences via checkpoints, dynamic test seed injection for rapid bootstrapping, and automated system backups scheduled through WorkManager and surviving app reinstallations.
- **Exercise**: Review the full Kotlin implementations highlighted in standard `backup-manager-plan.md`. Implement a password-protected zip streaming engine using custom `javax.crypto.CipherOutputStream` layers, write a test suite mapping automated mock ZIP profiles inside app assets, and configure a custom `"SYSTEM_BACKUP"` Cron Job type registered within `CronWorker.kt`.
- **Complexity**: Part 4

### 27. Jetpack Compose Internet Browser Applet with Scheduled I/O Automation & Script Injection (20h)
- **Reference**: `repo-android/docs/browser-applet-plan.md`
- **Goal**: Master professional web viewing in modern Jetpack Compose using secure AndroidView configurations, handle zero-copy multi-threaded download streams dynamically with ETA calculations, configure background headless crawling tasks on SQLite schemas via WorkManager routes, and develop a prompt-driven JavaScript userscript engine integrated with contemporary Google Generative AI client-side models (`gemini-2.5-flash`).
- **Exercise**: Check out the implementation details highlighted inside `browser-applet-plan.md`. Create a custom SQLite table schema for multi-tab management, implement an inline code generation prompt helper that maps raw text requests to prompt-directed sandboxed JavaScript using `generateUserScript()`, link downloads to a custom `BrowserDownloadManager` outputting updates via standard Foreground Notifications, and configure periodic background content checks by adding web scraping triggers inside the common `DynamicRouterWorker.kt` handler.
- **Complexity**: Part 5

### 28. Android Launcher UX Customization: Reordering, Circular Shapes & Startup Configuration (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/MainActivity.kt`, `repo-android/docs/launcher-applet-upgrade-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/AppPreferences.kt`
- **Goal**: Understand high-performance home screen configuration techniques using Jetpack Compose: dynamic card clipping using custom circular shapes, gestural long-press drag-and-drop mechanics inside vertical grids, multi-select active applet filters, and transaction-based cold-start route redirections in the MainActivity initialization lifecycle.
- **Exercise**: Design a custom `CircularAppletCard` clipped using `CircleShape` with centered vector icons and a text caption label beneath. Wire dynamic reordering parameters to serialized JSON sequence flows inside Jetpack Preferences DataStore, and write navigation redirect interceptors executing immediately upon activity cold-launches.
- **Complexity**: Part 4

### 29. Android Media & File Manager Architecture Evaluation & Hybrid Component Design (15h)
- **Reference**: `repo-android/docs/media-manager-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/ExplorerScreen.kt`
- **Goal**: Master system-level file-system architecture evaluations in Android, recursive folder directory walk-throughs, custom directory creation operations inside Jetpack Compose, secure cryptographic transformations for physical file locking, on-the-fly decryption streams, and interactive PIN authentication keypad overlays in Compose.
- **Exercise**: Review the full `media-manager-plan.md` and `ExplorerScreen.kt` implementations. Create an exercise where students configure a secondary key salt for the XOR stream cipher, implement a PIN reset flow requiring a master override pattern (e.g. entering "1234" three times securely prompts a SharedPreferences clear), and write an automated test ensuring that encrypted file structures remain scrambled and corrupted unless unlocked through the specific `SimpleCryptor` transformation.
- **Complexity**: Part 4

### 30. Android Architectural Layering, Safe SQL, and Standardized Dependency Injection (20h)
- **Reference**: `repo-android/docs/architectural-improvements-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/`
- **Goal**: Master the modular refactoring and isolation of monolithic ViewModels and Screens into Clean Architecture layers. Understand standard ViewModel factories, custom lifecycle-aware custom dependency injection (Service Locator Pattern), and thread-safe dynamic database streaming cursors targeting local Android filesystem DBs.
- **Exercise**: Implement the custom `AppDependencyContainer` and a matching `FeatureViewModelFactory` inside the Android codebase. Create a secure, safe query higher-order Kotlin extension function `safeQuery` on top of native `SQLiteDatabase` cursors to automatically prevent locked SQL database connections, and refactor a chosen monolithic screen's DB requests to use the safe query container via injected layers.
- **Complexity**: Part 5

### 31. Dynamic Plugin-Driven Applet Framework Contract and Registry (20h)
- **Reference**: `repo-android/docs/applet-plugin-framework-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/core/framework/`
- **Goal**: Understand the design and execution of modular, plug-and-play app architectures using shared dynamic interfaces, reflection-free plugin registries, automated Compose permission gateways, and asynchronous event bus lines using standard Kotlin SharedFlow pipelines safely.
- **Exercise**: Review the full dynamic framework specification highlighted in `applet-plugin-framework-plan.md`. Implement the unified `Applet` contract, construct an `AppletRegistry` single provider, bind navigation routing directly to live registrants using an automated `forEach` loop in `MainActivity.kt`, and build an asynchronous dispatch test verifying that missing permissions are checked on navigation endpoints automatically.
- **Complexity**: Part 5

### 32. Advanced CameraX Capabilities & Hardware negotiated OEM Extensions (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AppPreferences.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/CameraScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/SettingsScreen.kt`
- **Goal**: Master system-level integrations of modern CameraX camera pipelines, including physical OEM lens extensions negotiation through `ExtensionsManager` (HDR, Night, Portrait, Retouch), concurrent multi-lens picture-in-picture streams rendering layouts with resilient hardware failure fallback handlers, Pro Mode viewfinder manuals overrides (ISO, Exposure Value), and offline scanner hud overlays.
- **Exercise**: Review the complete Jetpack Compose and CameraX implementations. Create an exercise where students modify the on-screen manual Pro HUD toolbar to support fine-grained manual white-balance adjustments utilizing the CameraX Camera Control API. Integrate a custom Zoom slide indicator within the floating PiP window to control target picture-in-picture magnification levels.
- **Complexity**: Part 4

### 33. Jetpack Compose Rich Document Rendering & File CRUD Architecture (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/ExplorerScreen.kt`, `repo-android/docs/media-manager-plan.md`
- **Goal**: Understand advanced interactive file manipulation structures and single-screen dynamic document styling. Learn how to perform full filesystem operations (Create, Read, Update, Delete) from custom Compose layers, bind document states in double-pane layouts, and execute regex-based paragraph block parsing to construct responsive markdown viewports alongside plain text, code, and style elements without heavy external dependencies.
- **Exercise**: Implement an extension to the specialized `MarkdownRenderer` that supports checking checkbox bullet formats (e.g., `- [ ]` and `- [x]`). Render unchecked and checked states as live interactive material checkbox elements or toggleable visual states, allowing students to check off markdown to-do list items interactively, and update the raw file content on disk automatically.
- **Complexity**: Part 4

### 34. Custom Applet Registrations and Navigation Route Restructuring (5h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/core/framework/impl/FilesApplet.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/MainActivity.kt`
- **Goal**: Understand how to rename, deprecate, or structure dynamic route identifiers inside highly decoupled navigation systems. Learn how to map route display entries dynamically across customization screens and launcher grids safely without disrupting stored user pref profiles.
- **Exercise**: Create a custom validation script or handler that checks if a migration from ancient route structures (like `"explorer"`) to modern structures (like `"files"`) is necessary upon application boot, ensuring old bookmarks and shortcut layouts are gracefully updated to target `"files"`.
- **Complexity**: Part 2

### 35. Offline Play Services Map, Draggable Pin Geocoding Controls, and Color picker (20h)
- **Reference**: `repo-android/docs/agenda-map-picker-colors-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/AgendaDatabaseHelper.kt`
- **Goal**: Master integrating Google Maps SDK in Jetpack Compose, dropping draggable markers, reverse geocoding coordinates asynchronously into human addresses, linking events scheduling bidirectionally via calendar views and map views, and custom styling with color picker arrays in Compose.
- **Exercise**: Add dynamic geolocation fields (`latitude`, `longitude`, `location_name`) to SQLite helper. Develop an interactive maps Compose view linking events into visual custom map pins, and implement a responsive color tags picker that supports selectable color chips and formatted hex inputs verification.
- **Complexity**: Part 4

### 36. Leaflet Web-GIS Map Tab, Double-Pane Editing Wires, and Javascript-Kotlin Bridge (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/AgendaDatabaseHelper.kt`
- **Goal**: Master integrating an interactive Leaflet JS map inside Android `WebView` using dynamic HTML templating, supporting OSM/ArcGIS tile layers, bidirectionally plotting color-coded pins, handling coordinate pre-filling on map click popups, and configuring a bidirectional bridge to edit events from map popups.
- **Exercise**: Implement custom map markers with unique icons representing different event types (e.g., Star icons for "Secondary", Clock icons for "Alarms"), expand the Nominatim reverse geocoder to handle custom search parameters gracefully, and implement cached static OSM tile support for offline operations.
- **Complexity**: Part 4

### 37. AR Unified Viewfinder Modes & Multi-Image PDF Compilation Pipeline (20h)
- **Reference**: `repo-android/docs/camera-applet-upgrade-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/CameraScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/ExplorerScreen.kt`
- **Goal**: Master dual-engine camera scanning strategies (local offline contouring vs. Play Services API) configured via Settings, implement live on-screen tactile UX mode selector bands in Jetpack Compose, build Cross-Applet Image-to-PDF compilation pipelines, and support multi-format image and video saving selectors.
- **Exercise**:
  1. Extend `SettingsScreen` to add a radio-group selector for "Scanner Backend Service" (Contour Engine vs Play Services Doc Scanner API).
  2. In `CameraScreen.kt`, build a swipeable horizontal mode carousel containing `PHOTO`, `VIDEO`, `QR SCANNER`, and `DOC SCANNER` options with smooth slide transitions.
  3. Ensure selecting `DOC SCANNER` activates the dynamic canvas overlay drawing live page contours.
  4. In `ExplorerScreen.kt` (Files Applet), implement a contextual option card that displays when multiple `PNG/JPEG` assets are checked. Selecting "Merge to PDF" must invoke a localized background compilation worker utilizing Android's native `android.graphics.pdf.PdfDocument` API to sequence and bundle the images into a single PDF output.
  5. Add select options inside the `SettingsScreen` for camera file saving preferences (Images: compressed JPEG, lossless PNG, or WebP; Videos: standard MP4, MKV, or WebM). Update CameraX image/video capture bindings to initialize file options based on these configurations dynamically and save outputs with appropriate MIME-types and extensions.
- **Complexity**: Part 4

### 38. Jetpack Compose Context-Safe State Hoisting & Scoped Remembering in Dialogs (5h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`
- **Goal**: Learn how to manage CompositionLocal contexts (such as `LocalContext.current`) safely within asynchronous scopes and Composable `remember` blocks without causing compilation errors or memory leaks.
- **Exercise**: Identify and refactor instances where Composable getter properties are invoked inside non-composable callback lambdas (like the calculation lambda of `remember { ... }`). Correctly hoist context-aware states outside the remember function to guarantee compile-time safety and prevent runtime lifecycle/composition crashes.
- **Complexity**: Part 2

### 39. Jetpack Compose Dual-Engine Map Architecture & Google Maps JS API Integration (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/SettingsScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/AppPreferences.kt`
- **Goal**: Understand how to develop an application-level Dual-Engine Map rendering architecture in Android utilizing WebViews. Learn how to decouple the map components dynamically from rendering constraints, persist engine states (such as OpenStreetMap/Leaflet vs Google Maps JS API) in Jetpack DataStore, configure external billing/credentials structures via on-screen user inputs, and build unified Javascript-Kotlin callback interfaces.
- **Exercise**: Implement a system check or notification toast that triggers if the user boots the Google Maps JS engine without providing an API key, fallback gracefully to Leaflet GIS views, and explore extending the Google maps marker symbol paths to support dynamic sizing variables.
- **Complexity**: Part 4

### 40. Interactive WebView Diagnostics & Network Reachability Testing (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`
- **Goal**: Master on-device interactive diagnostic design patterns for web hybrid components inside Jetpack Compose, including mock JS-bridge simulations, web console error dispatch classification, CDN accessibility checks, and dynamic iframe dimensions monitoring.
- **Exercise**: Write a custom script in the diagnostic test page checking for specific CSS selectors and container resize callbacks, adding a dedicated "Clear Log Cache" button to reset the diagnostic state overlay.
- **Complexity**: Part 4

### 41. Clipboard Log Diagnostics & Bulk Copying Mechanics in Jetpack Compose (10h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/core/framework/impl/DebugApplet.kt`
- **Goal**: Master clipboard copy workflows, LocalClipboardManager interaction patterns, structured log string formatting, and dynamic multi-level bulk exporting in Android Jetpack Compose hybrid components.
- **Exercise**: Implement an action shortcut or menu to copy exclusively error-level logs containing active stack traces, with a custom formatting layout that highlights exception tags and timestamps.
- **Complexity**: Part 3

### 42. WebView Race Condition Elimination & Robust Library Loading (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`
- **Goal**: Master eliminating race conditions when loading external hybrid SDKs in native WebViews. Learn how to design robust `<head>` script callbacks that exist before external script loads, check for library namespaces securely, and delay execution through page lifecycle state listeners (`DOMContentLoaded`, `load`, and multiple backup `setTimeout` / `setInterval` refreshes).
- **Exercise**: Create a custom HTML testing page integrating a Leaflet/OpenStreetMap standard map with active click coordinators, and ensure the `initMap` loader checks both namespace `L` and DOM elements systematically to avoid JS-bridge crash regressions.
- **Complexity**: Part 4

### 43. Multi-Tab WebView Diagnostic Sandbox & Asynchronous Leaflet Recovery (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`
- **Goal**: Master full-stack diagnostics of multi-tab hybridization within Android WebViews. Understand how to design scrollable multi-tier diagnostic selectors in Jetpack Compose, build sandboxed test scopes verifying modern storage engines, geolocational adapters, and touch gesture handlers, and apply asynchronous polling fallback retry loops to guarantee Leaflet CDN initialization.
- **Exercise**: Create a custom Test Page 9 focusing on evaluating CSS viewport relative measurements (such as modern `dvh`/`dvw` dynamic sizing vs standard viewport units), adding a custom selector option within the scrollable Compose diagnostic ribbon, and routing its HTML generation code appropriately inside `AgendaScreen.kt`.
- **Complexity**: Part 4

### 44. Deferring Leaflet Map Creation & Custom Web Sizing in WebView (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`
- **Goal**: Understand WebView execution deferral to solve race conditions in CDN asset loading, and use precise percentages and absolute overlay CSS styling (such as `50vw`/`50vh` or centered `50%` layouts with deep borders) around hybrid frames.
- **Exercise**: Program a Javascript/HTML loader displaying a state-driven count-down timer (e.g. 10s wait) within the map component placeholder style, preventing Leaflet initialization via strict boolean lock gates. Then, set absolute viewport boundaries (`50%` width and height, centered with 6px thick high-contrast green diagnosis borders) to guarantee clear container containment and rendering feedback.
- **Complexity**: Part 3

### 45. Android Blackjack Applet: Custom Card Engine, Split Logic & Local Database Persistence (20h)
- **Reference**: `repo-android/docs/blackjack-applet-plan.md`
- **Goal**: Master developing professional-grade offline casino card games in Android using Jetpack Compose, implementing custom deck dealing logic with shuffle thresholds, complex multi-hand "Split" mechanics, and robust Local SQLite persistence of balances, bets, and wins/losses state statistics.
- **Exercise**: Review the full Kotlin implementations highlighted in `blackjack-applet-plan.md`. Extend the database helpers to store historical high scores, implement a basic strategy visual advisor overlay highlighting optimal Hit/Stand ratios based on current hand variables, and write a custom shuffle warning display triggering when more than 75% of the card shoe is depleted.
- **Complexity**: Part 4

### 46. Android WebView Direct HTML Injection & Base64 Map Workaround (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`
- **Goal**: Master eliminating CORS, file origin policy restrictions, and unescaped HTML special characters (such as '#' in hex colors or '%' in query parameters) inside Android hybrid WebView components by using direct Base64 HTML injection and responsive Box container boundaries.
- **Exercise**: Extend the `LeafletComposeMap` to accept custom Map Options (like `doubleClickZoom` or `dragging` toggles) as a dynamic configuration dictionary, serialize these options down to the WebView, and decode them inside the injected JS setup to mold the Leaflet map behavior on the fly.
- **Complexity**: Part 4

### 47. Android Leaflet Map HTML Live Sandbox & Webview Injection (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AgendaScreen.kt`
- **Goal**: Master dynamic WebView integration within Android Jetpack Compose, including loading live user-edited raw HTML/JS code safely into a sandbox environment, managing input states using OutlinedTextField controls, and maintaining bidirectional JavaScript communication over Android Bridges during live runtime updates.
- **Exercise**: Explore the newly implemented compact Material 3 dropdown preset menu in `LeafletMapViewPane`. Add a fifth premium custom preset tile provider option (such as Stamen Watercolor tile templates or custom marker animations) inside the `DropdownMenu` layout, and update the state mapping logic to inject the associated dynamic JS code block into the live sandbox editor instantly.
- **Complexity**: Part 4

### 48. Android Blackjack UX: Material 3 FABs, 3D Card Flips & Interactive Transaction Float Animations (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackViewModel.kt`
- **Goal**: Understand how to develop delightful UX interactions for mobile casino games in Jetpack Compose, including replacing standard buttons with custom Material 3 ExtendedFloatingActionButtons (FABs), executing 3D Y-axis card flip animations with reverse-mirrored back visuals, triggering staggered diagonal card deals using Compose target values with Spring kinematics, and designing interactive rising transaction floating badges.
- **Exercise**: Implement a dynamic emoji feedback animation overlay on the reactive Croupier's avatar when a player scores exactly 21, and enhance the transaction badge to play custom micro-haptic buzzes using Android's `Vibrator` utility.
- **Complexity**: Part 4

### 49. Android Blackjack UX Enhancement: Centered Results Badge Overlay & Dynamic Status Colorization (10h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackViewModel.kt`
- **Goal**: Learn how to implement high-impact end-of-round visual feedback overlays in Jetpack Compose. Understand how to design a centered badge icon with an exact transparency mapping of 0.6 using alpha graphics layers, and differentiate round-level financial states with color-coded layouts (green for money gains, red for money loss).
- **Exercise**: Add a custom scale animation or spring-based entry effect to the results badge dialog, making the centered emoji/payout indicator pulse into view when `GameState.PAYS_OUT` is initialized.
- **Complexity**: Part 3
 
### 50. Android Blackjack UX Adaptive Overlay, Multi-Value Aces, and Strategic Advice HUD Relocation (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/CardsModel.kt`
- **Goal**: Master the sizing of dynamic game elements using `LocalConfiguration` in Jetpack Compose to adapt to varying device aspects with `vmin` metrics. Learn how to compute and display soft vs. hard Ace card totals (e.g., "6/16") to handle multi-value Aces, and optimize the layout by contextually relocating the strategic advice banner directly above the player banner.
- **Exercise**: Implement a user preference that allows dynamically resizing the square overlay between 40% and 70% of the computed `vmin` setting.
- **Complexity**: Part 3

### 51. Android Blackjack Compact UI Layout, Dynamic Chip Wallet, and Circular Payout Modals (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`
- **Goal**: Understand how to optimize horizontal screen space on mobile layouts by migrating decentralized bankroll info panels directly inside the avatar profile cards. Learn how to transform standard rectangular dialog cards into perfect responsive circular elements utilizing `CircleShape` and managing text bound clipping.
- **Exercise**: Implement a secondary visual animation (such as a subtle rotating cyber-neon border stroke) around the circular payout popup, and add a setting to toggle between circular and standard rectangular round result modals.
- **Complexity**: Part 3

### 52. Android Blackjack Betting Speed-Dial & Circular Fan-Out Kinematics (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`
- **Goal**: Understand advanced interactive speed-dial animation kinematics in Jetpack Compose, including translating static horizontal lists of betting button chips into dynamic, floating, semi-circular fan-out dials using trigonometric functions (`cos`/`sin`) relative to a central floating action button.
- **Exercise**: Create a customized fanning pattern that expands the semi-circular betting arc from 90° up to 120° dynamically when the user rotates the device, allowing additional special high-risk $1000 and $5000 chip options to animate smoothly into view.
- **Complexity**: Part 4

### 53. Android Blackjack Betting UX: Conditional Wallet Handling & Corner Controls (10h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`
- **Goal**: Understand how to implement custom context-aware UI/UX states in Jetpack Compose, specifically hiding standard betting controls when a player's wallet balance is depleted and conditionally displaying a recovery reload action in the bottom-left corner.
- **Exercise**: Extend the bottom-left conditional controls to add a smooth fade-in/out transition using `AnimatedVisibility` when shifting from standard betting buttons (`CLEAR BET` / `SAME BET`) to the `RELOAD` button when the wallet balance hits 0.
- **Complexity**: Part 3

### 54. Android Blackjack UX: Unified Row Cards Layout with Adaptive Recommendation Avatar and Size-Matched Totals (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`
- **Goal**: Master full optimization of mobile horizontal screen real estate in Android Jetpack Compose by unifying decentralized gaming metrics (player identity, advice-driven avatar emojis, and hand score totals) onto the exact same horizontal layout row as the playing cards. Learn how to size non-card elements (`AvatarCard` and `TotalCard`) to precisely match standard card dimensions (e.g., `72.dp x 108.dp`), and implement reactive avatar state changes that parse and display responsive emojis matching active basic strategy coaching recommendations.
- **Exercise**: Create a custom transition or border highlight pulse effect on the `AvatarCard` whenever the coaching strategy shifts from "HIT" (👊) to "STAND" (✋), and add mini glowing indicators indicating whether the active advice matches standard Vegas rules.
- **Complexity**: Part 4

### 55. Android Blackjack UX: Player Card Row Total & Reordered Money Stacks (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`
- **Goal**: Learn how to restructure declarative layout models in Android Jetpack Compose. Master relocating score display modules directly alongside playing card viewports and reordering composite stats blocks dynamically, optimizing screen density and typography styling.
- **Exercise**: Create a custom user setting to toggle the horizontal position of `TotalCard` between being dealt before the first card or placed after the last card inside the `LazyRow` layout.
- **Complexity**: Part 3
 
### 56. Android Blackjack UX: Symmetrical Dealer Total Card Alignment (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`
- **Goal**: Understand how to enforce visual symmetry and structural layout consistency across both active player and dealer sections in Jetpack Compose. Master migrating score/total elements from title rows directly into card scrolling contexts (`LazyRow`) using reusable, state-aware UI elements like `TotalCard`.
- **Exercise**: Implement a subtle glowing border effect or specialized color accent on the dealer's `TotalCard` only when the dealer's score matches exactly 21.
- **Complexity**: Part 3

### 57. Android Blackjack UX: Inline Action Strategy Recommendations (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`
- **Goal**: Learn how to implement compact, inline context-aware feedback within parent header elements in Jetpack Compose. Master moving full-width alert banners into lightweight, pill-shaped strategy recommendation chips positioned dynamically at the end of the active player name header row.
- **Exercise**: Add a custom tooltip or tap animation to the inline `CompactStrategyAdvice` chip that explains the basic strategy rationale (e.g. why staying or hitting is recommended) when clicked.
- **Complexity**: Part 3

### 58. Android Blackjack UX: Persistent Cross-Session High Scores & Interactive Game Over Flows (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackViewModel.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackDatabaseHelper.kt`
- **Goal**: Master persistent schema modifications in SQLite on Android to support high score leaderboards, implement robust detection pipelines for game over states when the wallet balance drops below the minimum limit ($5), and design an immersive, interactive modal popup in Jetpack Compose to lock standard play, display cross-session peak wallet values, capture name registrations, and reset states safely back to $1000.
- **Exercise**: Create a custom setting that allows players to toggle between ascending and descending sort directions on the Hall of Fame leaderboard records list, and add a custom animation (such as a confetti or money shower particle effect) when a new high score is registered and saved.
- **Complexity**: Part 4

### 59. RogueCompose Applet: Procedural Dungeon Construction & Permadeath Auto-Saving (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/`
- **Goal**: Understand procedural level generation algorithms (random carver setups), symmetrical board states, local database persistence across device lifecycles, real-time tone synthesis overlays with `ToneGenerator`, and enforcing permadeath in a mobile RPG.
- **Exercise**: Implement a secondary merchant shop inside custom floor maps that allows players to trade gold for random health and mana potions, update the local SQLite database schema to support gold totals and potion inventories saving on every step, and trigger a synthesized chime when shopping.
- **Complexity**: Part 4

### 60. Android Image Selection, Multi-Option Size Reduction & PDF Document Compiler (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/media/ImageReducerEngine.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/media/PdfCompilationEngine.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/media/ImagePdfScreen.kt`
- **Goal**: Master system-level image loading, asynchronous lossy/lossless image compression and scaling with bitmap recycling caches inside background Coroutines scopes, and multi-page high-fidelity PDF Document generation using Android's native Graphics and `PdfDocument` canvas libraries in Jetpack Compose.
- **Exercise**: Implement a local test scenario that takes a multi-selected array of picture Uris, computes scale compression metrics dynamically displaying live simulated kilobyte savings, and merges them sequentially into a single target PDF document stored securely within the private Files directory.
- **Complexity**: Part 4

### 61. Android Applet Discovery & Launcher Integration Layout Refactoring (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/MainActivity.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/SettingsScreen.kt`
- **Goal**: Understand the configuration of modular, dynamically listable, and draggable applets on the mobile launcher. Master aligning registration arrays between Settings, Main Hub, and Startup landing configuration dialogs, and diagnosing index discrepancies where existing screens are compiled but omitted from UI layouts.
- **Exercise**: Add a brand-new custom background utility route `"system_monitor"` to all active launchers and layout configuration lists, define an appropriate high-contrast Material background color mapping within `getAppletColor`, and ensure list synchronization is preserved during user selection resets.
- **Complexity**: Part 3
 
### 62. Android Blackjack UX: Persistent Exit Mechanisms and Smooth Transition Faded Badge Overlays (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`
- **Goal**: Master the concepts of fluid, persistent user navigation patterns, and custom-interpolated animated transition effects in Jetpack Compose. Learn how to configure persistent action buttons spanning across multiple gameplay states without layout conflicts, and design time-sensitive mathematical scaling models converting configuration dimensions (`vmin`) dynamically to prevent screen clattering inside condensed overlays.
- **Exercise**: Implement an expanding/collapsing radial menu trigger on the bottom-left leave button that reveals additional utility items (e.g. sound toggle or help quick-view) with a smooth elastic entry animation when long-pressed.
- **Complexity**: Part 3

### 63. Android System Custom Keyboard: InputMethodService & Jetpack Compose IME Integration (15h)
- **Reference**: `repo-android/docs/custom-keyboard-plan.md`
- **Goal**: Master implementing a system-wide custom soft keyboard using Android's `InputMethodService` that hosts dynamic, theme-adaptive Jetpack Compose key setups (featuring tactile feedback controllers, database-hooked clipboard histories, and Gemini AI-prompter direct inline content streaming).
- **Exercise**: Author the target QWERTY keyboard composable, configure the lifecycle owners inside the custom IME Service structure to ensure crash-free execution, and write background handlers that enable users to run text selection selections through remote Gemini rewrite engines to update target fields inline.
- **Complexity**: Part 4

### 64. Custom Compose IME: Robust ViewTree Lifecycle and State Owner Registration (10h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/keyboard/FraiseInputMethodService.kt`
- **Goal**: Understand how to register system-level lifecycle, view-model, and saved-state registry owners on individual `ComposeView` objects when they are inflated outside standard Activities (like in custom `InputMethodService` or background broadcast systems). Learn why fully-qualified class-level setter methods (e.g., `ViewTreeLifecycleOwner.set`) are more robust and prevent Kotlin package-level extension-function resolution errors.
- **Exercise**: Explain the structural layout lifecycle of a virtual keyboard, and modify the IME View creation code to log current states during lifecycle transitions (ON_CREATE, ON_START, ON_DESTROY) to verify owners are fully registered.
- **Complexity**: Part 3

### 65. Android Code Quality: Non-Deprecated API Migration & Material 3 Alignment (5h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/keyboard/FraiseInputMethodService.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/media/ImagePdfScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/RoguelikeScreen.kt`
- **Goal**: Understand how to identify, resolve, and migrate deprecated Kotlin and Jetpack Compose component patterns to modern APIs. Master replacing `Char.toInt()` with the robust standard Kotlin `Char.code` property, upgrading deprecated `Divider` components to Material 3 standard `HorizontalDivider` components, and migrating legacy direction-based icons (such as `KeyboardArrowLeft` and `KeyboardArrowRight`) to official Compose `Icons.AutoMirrored.Filled` equivalents to honor strict modern layout standards.
- **Exercise**: Review the compiler warnings and deprecation logs in a Jetpack Compose project. Migrate any remaining directional cursor icons to their `Icons.AutoMirrored` counterparts, and convert deprecated simple custom dividers to modern Material 3 `HorizontalDivider` layouts with customized thick and color attributes.
- **Complexity**: Part 2

### 66. Android Responsive Jetpack Compose Gaming Canvas & Screen Orientation Metrics (15h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`, `repo-android/docs/blackjack-responsive-layout-plan.md`
- **Goal**: Learn how to implement highly responsive adaptive layouts in Jetpack Compose by targeting different device screen buckets (portrait and landscape orientations for both smartphones and premium tablets). Master configuring layout splits, dynamic scaling parameters (e.g. card sizes, avatar badges, spacer heights), and trigonometric speed-dial coordinates relative to dynamic viewport contexts.
- **Exercise**: Implement a secondary conditional check inside the mobile header to show extra descriptive tooltip badges when in landscape mode, allowing players to view standard Vegas rules information in the header without collapsing existing items.
- **Complexity**: Part 3

### 67. Sovereign 3D Compose Engine: Mathematical Projections, Depth Sorting, and Simulation Workspaces (20h)
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/threed/ThreeDWorkspaceScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/core/framework/impl/ThreeDApplet.kt`, `repo-android/docs/three-d-workspace-and-proposals.md`
- **Goal**: Understand how to develop a standalone 3D graphics rendering pipeline in pure Kotlin/Jetpack Compose, leveraging standard 3-axis rotation matrices, perspective camera projection formulas, Backface Culling normal vectors, and Painter's Algorithm sorting to resolve overlapping surfaces without hardware depth buffs or heavy external rendering frameworks.
- **Exercise**: Implement an additional chemical compound (e.g., Ethanol $C_2H_5OH$) in the Molecular generator, configuring correct atomic coordinates (Carbons in dark grey, Oxygens in red, Hydrogens in light grey), and adding appropriate connecting covalent bonds. Verify that rotating the molecule orbits all base nodes in perfect perspective alignment.
- **Complexity**: Part 4

### 68. Responsive 3D Rubik's Cube Puzzle Sandbox & Matrix State Snapping (25h)
- **Reference**: `repo-android/docs/rubiks-cube-applet-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/threed/ThreeDWorkspaceScreen.kt`
- **Goal**: Master the construction of high-precision multi-axial coordinate rotators and state snapping algorithms in a purely custom-projected 3D view. Understand how to design a smartphone-responsive UX that adapts canvas aspect-ratios, partitions controller sections dynamically based on Portrait versus Landscape screens, and handles gestural swipe vectors.
- **Exercise**: Implement a validation method inside the 3D Rubik's puzzle structure that checks if the current board state is fully solved (i.e. verifying that all active faces on each of the six outer planes share identical, homogeneous color values). Trigger a custom congratulatory particle effect overlay when solved.
- **Complexity**: Part 5

### 69. High-Fidelity 3D Shading & Texture Mapping in Sovereign Compose Engine (20h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/threed/ThreeDWorkspaceScreen.kt`, `repo-android/docs/three-d-workspace-and-proposals.md`
- **Goal**: Understand how to develop custom diffuse shading filters and perspective-correct texture mapping algorithms on custom drawing Canvases. Learn to calculate face normal vectors via cross products, apply Lambert and Phong light equations inside standard Jetpack Compose Canvas draw scopes, solve 2D affine transformation matrices for triangle mapping, and use clipped paths with local matrix-transformed `BitmapShader` configurations.
- **Exercise**: Implement an ambient intensity slider in the 3D scene options panel, and configure it to dynamically scale the background glow value ($I_{ambient}$) from 0.0 to 1.0. Verify that setting the intensity to 0.0 results in severe, high-contrast shadows on the unlit sides of rotated 3D geometries, while increasing it softens the contrast.
- **Complexity**: Part 4

### 70. Sovereign 3D Game Upgrades: Spatial Rotations & Parabolic Physics (20h) 🍓 NEW
- **Reference**: `repo-android/docs/rogue-blackjack-3d-upgrade-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/RoguelikeScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`
- **Goal**: Understand how to bridge 2D flat matrices (like turn-based RPG grids and card tables) into a three-dimensional coordinate engine. Master designing orthographic/elevated camera projection transformations, distance-based light attenuation to simulate 3D fog of war, physical parabolic dealing trajectories using 3D Bezier equations, and vertical Z-axis rendering for layered cylinders (stacked chips).
- **Exercise**: Author a prototype math model that calculates the standard X, Y, Z world space coordinate offsets for an item dealt to Player Hand #4 in 3D Blackjack, and implement the corresponding unit tests to verify the card starts in the shoe at (200, -180, -120) and finishes at your custom calculated target coordinates when progress reaches 1.0f.
- **Complexity**: Part 4

### 71. Volumetric Chip Stacking & Extruded Wood Rim 3D graphics (15h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackCanvas3D.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/blackjack/BlackjackScreen.kt`
- **Goal**: Learn how to assemble high-fidelity three-dimensional vectors into custom-drawn volumetric shapes inside a pure Android Canvas. Understand how to extrude wood rim curves downward to build solid 3D boundaries, and stack physical cylinders whose vertical extent correlates directly with dollar counts.
- **Exercise**: Modify the cylindrical chip stack routine to support blue high-roller $500 chips (using color `Color(0xFF1E88E5)`) and update the deconstruction math so that any bet above $500 utilizes these premium chips to conserve vertical cylinder stack heights.
- **Complexity**: Part 4

### 72. Responsive Mobile HUD Overlays & Player-Centric 3D Dynamic Camera (20h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/DungeonCanvas3D.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/RoguelikeScreen.kt`
- **Goal**: Master implementing highly responsive mobile HUD overlays that overlap rendering widgets on Android. Learn how to design a dynamic player-centric camera projection tracking live coordinates in 3D projection algorithms, and implementing space-saving collapsible displaying controls with dropdown panels in Jetpack Compose to maximize game viewing spaces.
- **Exercise**: Create a new customizable state inside the 3D Display Configuration dropdown panel (e.g., a "Camera Presets" dropdown containing Orthographic, Isometric, and Birdseye angles) and update the pitch/yaw rotation factors to snap the 3D viewport immediately to these angles when selected.
- **Complexity**: Part 4

### 73. 3D Camera Projection Alignment & Dynamic Zoom Range Control (12h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/DungeonCanvas3D.kt`
- **Goal**: Understand the coordinate alignments of three-dimensional orthographic/isometric drawing planes in custom graphics rendering components. Learn how incorrect vertical camera rotation bounds can invert perspective matrices, leading to underground views, and how to calibrate the tactile gestural panning calculations to maintain a consistent high-altitude viewport angle. Master designing rich zoom ranges from zoomed-out isometric maps down to near close-up views.
- **Exercise**: Implement a secondary toggle inside the display configuration panel that switches the manual zoom click increment from a constant `0.15f` step to a variable log-based proportional step relative to the current zoom factor, allowing smoother controls when zoomed in.
- **Complexity**: Part 4

### 74. 3D Coordinate Axis Alignment and Color-Guided D-Pad Controls in Jetpack Compose (15h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/DungeonCanvas3D.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/RoguelikeScreen.kt`
- **Goal**: Learn how to resolve 3D spatial navigation confusion in rotating orthographic and isometric camera systems. Master creating real-time rotating axis helpers at the character's feet using line primitives inside the Compose Canvas pipeline, synchronizing a floating orthographic 3D compass HUD in the corner of the viewport, and linking these indicators directly to color-coded, labeled direction buttons on the controller pad.
- **Exercise**: Implement a secondary slider or dynamic opacity variable in the display configuration dropdown panel that scales the rendering alpha of the 3D floor axis line helpers and labels from 0.0f (fully hidden) to 1.0f (completely solid), preserving ideal visual contrast across dim and bright rendering scenarios.
- **Complexity**: Part 4

### 75. 3D Navigation Alignment and Coordinate Inversion Techniques (15h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/DungeonCanvas3D.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/RoguelikeScreen.kt`
- **Goal**: Master calibration of spatial orientation systems and user input controllers (D-pads). Understand how physical movement directions (East/West swapping) of a character in the game grid map to on-screen visuals and learning to calibrate coordinate offsets inside interaction event handles.
- **Exercise**: Create a custom verification routine that ensures gamepad click directions trigger movement offsets matching on-screen interactive indicators precisely.
- **Complexity**: Part 4

### 76. MORIA Layout: Adaptive Screen-Fitting Dual Layout & Control Clustering (15h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/RoguelikeScreen.kt`
- **Goal**: Master implementing highly responsive dual-aspect dynamic layouts (using `BoxWithConstraints` to compute `vmin` viewport bounding boxes) that reorganize layouts based on screen rotation. Learn how to construct a side-by-side landscape setup (split-screen) and a top-to-bottom portrait layout. Acquire experience grouping action button clusters (Attack/Spell & Bag close together) and positioning directional controllers (D-pad right-bottom) without viewport overlaps.
- **Exercise**: Implement a secondary toggle button inside the left-bottom other-buttons panel that rotates the D-pad orientation clockwise by 45 degrees, updating coordinate offsets dynamically.
- **Complexity**: Part 4

### 77. MORIA Rogue UX optimization: Logs-Stats Swap and Organized Bag Categorization (10h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/RoguelikeScreen.kt`
- **Goal**: Master mobile HUD readability and organized collections representation in Jetpack Compose modal overlays. Understand the user friction of scanning mixed arrays, and learn how to sort and partition item lists into distinct, high-contrast visual categories (Weapons, Armors, Consumables) sorted descending by their structural value bonuses to support rapid decision-making. Learn how to place vital status metrics right next to the active action region (below bottom-scrolling message logs) to minimize scanning distances.
- **Exercise**: Implement a client-side search query input bar at the top of the `InventoryDrawerModal` that dynamically filters the item list by name as characters type, maintaining matching sorting-by-bonus priorities in real-time.
- **Complexity**: Part 3

### 78. Interactive 3D World Globe Applet: Parametric Spheres & Dynamic Texture Selectors (20h) 🍓 NEW
- **Reference**: `repo-android/docs/world-applet-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/world/`
- **Goal**: Understand how to map, project, and rotate a 3D spherical point cloud using parametric equations in Jetpack Compose, and build an interactive file-picker to dynamically load local device images onto the 3D sphere as a BitmapShader texture.
- **Exercise**: Implement a parametric grid density slider (e.g., latitude/longitude subdivision steps) and verify that lower density settings yield a lightweight polyhedral sphere while higher density settings yield high-precision smoothness.
- **Complexity**: Part 5

### 79. Interactive 3D World Globe Applet: Remote URL Texture Mapping and Quick Planet Presets (15h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/world/WorldScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/world/WorldViewModel.kt`
- **Goal**: Master remote resource retrieval, network-safety constraints, and background bitmap scaling operations in Android. Understand how to manage asynchronous URL text inputs, download images efficiently via HttpURLConnection inside Dispatchers.IO, dynamically downscale high-resolution planetary maps to optimize memory bandwidth, and build a quick-click preset deck that triggers immediate download and planetary mapping with a single touch.
- **Exercise**: Create a custom URL testing field with real-time network reachability validation, and implement a fallback system that loads the default high-contrast "Cyber Grid" procedural custom texture if a remote texture download suffers an SSL handshake failure or a 404 response.
- **Complexity**: Part 4

### 80. Studio Draw Applet: GIMP-Style Multi-Layer Graphics Compositing & Blend Modes in Jetpack Compose (20h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/draw/DrawModels.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/draw/DrawScreen.kt`
- **Goal**: Understand how layer-based graphic canvases model vector and raster inputs sequentially. Master creating GIMP-style layering stacks (visibility toggles, dynamic opacity alpha mappings, coordinate lockdowns, and blending algorithms) and rendering them cleanly via modern Compose Canvas' saveLayer graphics contexts.
- **Exercise**: Create an exercise where students add a new blending mode "Xor" or "ColorBurn/Darken" to the BlendMode dropdown selection menu, and verify their visual behaviors on contrasting background and drawing colors.
- **Complexity**: Part 4

### 81. Studio Draw Applet: Remote URL Sourcing & Center-Crop Aspect Ratio Adaption (15h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/draw/DrawViewModel.kt`
- **Goal**: Master remote HTTP download stream conversions safely using Coroutines & Dispatchers.IO, and implement professional-grade dynamic center-crop scaling math that scales loaded external bitmaps cleanly to completely cover custom project dimensions without skewing aspect ratios.
- **Exercise**: Introduce a toggle that allows loaded images to either center-crop (cover) or aspect-fit (contain, leaving solid colored boundaries) onto the workspace canvas.
- **Complexity**: Part 4

### 82. Studio Draw Applet: High-Resolution Composite Masterpiece Compiler Exports (15h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/draw/DrawViewModel.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/draw/DrawScreen.kt`
- **Goal**: Deep dive into offline graphics compositing calculations on hardware-accelerated bitmaps. Master exporting canvas layers into lossless PNG, space-efficient JPEG, or contemporary lossless/lossy WEBP formats, while offering dimension upscaling multipliers (e.g., 1.0x, 2.0x, 4.0x) that cleanly scale path vector stroke rendering coordinates and raster resolutions during the compiler draw phase.
- **Exercise**: Add a custom numeric height/width text input boundary fields selector with aspect-ratio locked toggles inside the export compiler dialogue card and verify that the compiled bitmap renders cleanly.
- **Complexity**: Part 4

### 83. 3D Globe Projection Alignment, Polar Indicators & Jetpack Compose Space De-cluttering (15h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/world/WorldScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/world/WorldGlobeCanvas.kt`
- **Goal**: Understand 3D projection mathematical inversions in 2D coordinate spaces, learn to plot geographic polar lines dynamically on rotating sphere grids, and master advanced layout optimization techniques (using dropdown selects and dialog modals) to declutter heavy smartphone interface designs.
- **Exercise**: Implement a secondary toggle inside the advanced settings modal to change the polar lines thickness from `3f` to a sliding range value (1f..8f), and verify that drawing updates reactive values instantaneously.
- **Complexity**: Part 4

### 84. Adaptive Layouts & Non-Nested Scroll Containers in Jetpack Compose (10h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/world/WorldScreen.kt`
- **Goal**: Master the design of adaptive, responsive mobile/tablet layouts in Jetpack Compose (`isMultiSplit` Row vs Compact Column). Understand the constraints of nesting vertical scrolls in Compose, and learn how to safely delegate `Modifier.verticalScroll` parameter-passing to let parent containers control scrollability of nested columns dynamically without infinite-height exceptions.
- **Exercise**: Implement dynamic visual fade indicators on the `WorldControlPanel` scrollable container that show/hide a subtle chevron shadow at the bottom to notify users that additional configuration items are available to scroll down to.
- **Complexity**: Part 4

### 85. Moria Roguelike: Enable Interaction, Potion Quick-drinking, and 3x3 Shortcut Grid (15h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/RoguelikeViewModel.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/RoguelikeScreen.kt`
- **Goal**: Understand how to decouple movement from action inside turn-based game loops on mobile devices, and master high-density handheld button layout grids built alongside custom Gamepad controllers in Jetpack Compose.
- **Exercise**: Create an exercise where students add an interactive status banner that changes color or size based on whether they have a consumable potion available in their current inventory, and toggle its visibility in the 3x3 grid dynamically.
- **Complexity**: Part 3

### 86. Moria Roguelike: Procedural 3D Detail Mapping & Real-Time Math Orbitals (15h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/DungeonCanvas3D.kt`
- **Goal**: Master procedural detailed decals and dynamic mathematical orbitals inside high-performance 3D vector graphics scopes using standard Compose Canvas libraries. Understand face offset calculations to eliminate Z-depth fighting (z-fighting) and how to map continuous infinite rotations onto 3D projection pipelines.
- **Exercise**: Create an exercise where students add a dynamic, pulsating particle effect or a secondary interlocking orbits layer to the selected Hero/player, utilizing the continuous animated ticker.
- **Complexity**: Part 4

### 87. Moria Roguelike: Interpolated Movement Matrices & Idle Wave Bobbing (12h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/DungeonCanvas3D.kt`
- **Goal**: Master real-time visual coordinate interpolation using key-based `Animatable` structures mapped dynamically from transient state matrices. Understand how to design complementary non-synchronized trigonometry-based breathing/bobbing animations for entities in high-performance custom 3D drawing pipelines.
- **Exercise**: Create an exercise where students add an animation duration modifier slider (from static instant snapping at `0ms` to slow elastic gliding at `1000ms`) inside display option filters, and test visual state transitions.
- **Complexity**: Part 4

### 88. Moria Roguelike: Procedural Volumetric Assets & Smooth Camera Sweeping (16h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/DungeonCanvas3D.kt`
- **Goal**: Master the implementation of complex volumetric 3D meshes (such as wood-and-iron treasure chests with hinges, and descending brick stairs) from scratch using custom coordinate vectors and light attenuation formulas. Understand how to program smooth multi-attribute camera preset sweeping transitions using Compose coroutine scopes while simultaneously supporting instant raw drag/zoom gesture inputs.
- **Exercise**: Create an exercise where students add an interactive toggle to let the gold lock on the chest glow or pulsate under real-time light waves, utilizing coordinate translations.
- **Complexity**: Part 4

### 89. Moria Roguelike: Upgraded 3D Low-Poly Entities and Class-Specific Helmets (15h) 🍓 NEW
- **Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/roguelike/DungeonCanvas3D.kt`
- **Goal**: Master procedural low-poly 3D modeling using multi-faceted custom vector geometry, class-specific headgears (steel plate helmets, conical wizard brimmed hats, rogue cowl hoods), and distinctive monster meshes (dragons with horns, necromancers with floating staff orbs, and long-eared goblins) rendered directly on Jetpack Compose Canvas with light shader attenuation calculations.
- **Exercise**: Implement a secondary custom helmet or crest for the Rogue class (e.g., adding a feathered cap or a metal half-mask by defining corresponding vertices, lines, and polygons) and ensure its shade values adapt to live ambient lighting.
- **Complexity**: Part 4

## Recommended Reading
- [Flutter Docs](https://docs.flutter.dev/)
