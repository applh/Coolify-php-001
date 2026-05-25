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

## Recommended Reading
- [Flutter Docs](https://docs.flutter.dev/)
