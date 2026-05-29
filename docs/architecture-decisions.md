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

---

## 7. Dynamic WebView Diagnostics & Device Auditing Suite

### Context
Maintaining web hybrid components (such as Leaflet GIS maps) inside Android WebViews presents silent runtime failures (broken JS bridges, blocked mixed-content CDN assets, container 0-height collapses, touch event hijacking by native parent Composable constraints, storage exception locks, and missing geolocation permissions). Finding the root origin of a map failure can be extremely difficult for developers and end-users.

### Decisions & Justification

1. **Multi-Tab Scrollable Diagnostic Ribbon**
   - **Choice**: Implemented a scrollable container in Jetpack Compose featuring a total of nine distinct runtime diagnostic tests (Modes 0 to 8: Standard Map, JS Bridge, CDN Reachability, Leaflet API integrity, DOM Image loading, CSS Container height, Gesture Touch-pad capturing, Sandboxed Storage engines, and Android-core routed Geolocation inputs).
   - **Reason**: Decoupling the tests into independent micro-environments allows targeted, separate evaluations of the different subsystems (Network vs JS Logic vs Composable layout vs Android Permissions) without cascading failures. Placing buttons inside a horizontally scrollable container prevents crowding in dense portrait screens.

2. **Touch-Pad Gesture Capture Sandboxing (Test Mode 6)**
   - **Choice**: Designed a custom gesture touch slate that intercepts, logs, and displays raw mouse/touch and multi-touch coordinates.
   - **Reason**: Detects if native Compose container scopes (such as swipe-to-dismiss layers or tabbed page navigators) are capturing input pointer sweeps before they reach the nested WebView, ensuring standard user interactivity (like panning Leaflet maps) remains fluid.

3. **Multi-Tier Sandboxed Storage Testing (Test Mode 7)**
   - **Choice**: Evaluated and confirmed simultaneous operations across LocalStorage, SessionStorage, dynamic Cookie configuration, and full IndexedDB instances in the sandboxed WebView context.
   - **Reason**: Ensures storage limits or security profiles do not lock standard mapping caching layers, allowing cached map tiles to write to local storage seamlessly on offline configurations.

---

## 8. Web Map Picker Race-Condition Elimination & Robust Asynchronous Loading

### Context
In the coordinate-picker components (`LeafletComposeMap`), Leaflet scripts are loaded asynchronously from cloud CDNs. Intermittent cellular networks or WebView thread priorities can cause the main inline map initialization script to execute *before* the external Leaflet script has completely loaded and instantiated the global `L` namespace, leading to immediate JS runtime crashes and blank screens.

### Decisions & Justification

1. **Self-Introspecting Polling Loader (`tryInitPickerMap`)**
   - **Choice**: Refructured the JavaScript block to group all Leaflet initialization statements inside an asynchronous function `initPickerMap()`, guarded by a dynamic namespace check (`typeof L !== 'undefined'`).
   - **Reason**: Completely breaks dependencies on immediate, synchronous script execution. If the global `L` namespace is not ready yet, initialization is aborted cleanly without crashing, and scheduled to retry.

2. **Multi-Chained Lifecycles and Polling Resiliency**
   - **Choice**: Registered three backup event listeners (`load`, `resize`, `DOMContentLoaded`) and five staggered time-interval execution fallbacks (`setTimeout`) ranging from 100ms up to 2500ms.
   - **Reason**: Coordinates with web browser rendering life states. High-speed, cached page parses trigger initialization almost immediately at 100ms, whereas slower networks gracefully boot up on later intervals once CDN downloads complete.

3. **Double-Safeguarded Marker Updates**
   - **Choice**: Guarded all marker or reverse geocoding methods with explicit safety null checks (e.g., `if (!marker) return` and `if (map) { map.setView(...) }`).
   - **Reason**: Avoids runtime exceptions if the user attempts to search for an address or click elements before Leaflet has fully initialized the map coordinates, guaranteeing absolute script reliability under any loading state.

4. **Visual Layout Diagnostic Borders (Map and GIS Tiles)**
   - **Choice**: Integrated high-contrast custom layout borders around map containers (green `#4CAF50` for standard map, blue `#2196F3` for map location picker) paired with vivid orange borders (`#FF5722`) on individual map layer image tiles (`.leaflet-tile`).
   - **Reason**: Allows developers and quality auditors to visually confirm layout initialization, size correctness, and successful asset fetch within standard Android WebView sandbox environments.

---

## 9. Android WebView Direct HTML Injection & Base64 Map Workaround

### Context
When loading dynamic HTML maps (e.g. Leaflet OSM or Google Maps JS API templates) into Android WebViews inside a Jetpack Compose UI state, several security and rendering issues can cause the maps to display as a completely blank screen:
1. Origin Restrictions: Modern Android WebViews (particularly on Android 10+) enforce strict security policies that can block relative resource loads or external CDN script requests when the base HTML is loaded via `loadDataWithBaseURL` with custom hostname schemes (e.g. `https://localhost/map_view.html`).
2. CSS/HTML Special Character Encoding: Dynamic color definitions (e.g. '#' in hex tags, or '%' in queries) can break WebView parsing of inline strings unless they are heavily escaped or base64 encoded.
3. Measurement & Parent Collapse: Under dynamic Compose layouts, deep nested WebView composables (`AndroidView`) can easily collapse to a measured width or height of 0 on initial measurement or subsequent recompositions, rendering the canvas invisible.

### Decisions & Justification

1. **Direct HTML Injection via Base64 Payload Encoding (`loadData` base64)**
   - **Choice**: Convert the HTML template strings directly to byte arrays and encode them as Base64 strings using `android.util.Base64.encodeToString(..., Base64.NO_PADDING or Base64.NO_WRAP)`, then load them via `webView.loadData(encodedHtml, "text/html; charset=utf-8", "base64")`.
   - **Reason**: Direct Base64 injection completely bypasses all origin-based URL security boundaries and Mixed Content policies since the WebView is treating the string as raw data payload instead of a sandboxed local file URL. Furthermore, this technique natively handles all occurrences of `#` (extremely common in map marker color hex codes) and `%` characters without any manual escape/regex overhead, ensuring 100% rendering fidelity.

2. **Outer Compaction Box Container (`androidx.compose.foundation.layout.Box`)**
   - **Choice**: Wrapped the `AndroidView` inside an explicit Compose `Box` container with `Modifier.fillMaxSize()`.
   - **Reason**: Forces Compose's layout subsystem to negotiate correct, non-zero parent measurement constraints before allocating bounds to the nested `AndroidView`. This completely eliminates the 0x0 scale collapses that frequently happen upon dynamic list updates or screen state transitions.

---

## 10. Android Blackjack UX: Unified Row Cards Layout with Adaptive Recommendation Avatar and Size-Matched Totals

### Context
In mobile casino games, preserving and optimizing horizontal screen real estate is critical. Previously, the strategic advice HUD, player statistics, total wallet balance, active bets, and rank badges were spread out over bulky decentralized panels, taking up portrait view space and cluttering the card progression flow. In addition, the player's cards and hand score totals were rendered on different vertical blocks, hindering readability. The player avatar acted as a static placeholder without dynamically reflecting basic strategy recommendations.

### Decisions & Justification

1. **Unified Horizontal Flow (Vegas Strip Layout)**
   - **Choice**: Moved the player's avatar profile, hand score totals, and physical card deck assets onto a single, cohesive horizontal row inside a scrollable `LazyRow` layout.
   - **Reason**: Translates the gaming table into an intuitive left-to-right progression similar to real card placements on a blackjack table (Avatar first, then Total, then drawn playing cards). This leaves the surrounding screen entirely clean, spacious, and dedicated to gaming tension.

2. **Perfect Dimensional Sizing Match (`72.dp x 108.dp`)**
   - **Choice**: Encapsulated the player avatar indicator (`AvatarCard`) and the score metric panel (`TotalCard`) into custom Material 3 box containers styled with exact dimensions matching the playing card views (Width: `72.dp`, Height: `108.dp`).
   - **Reason**: Establishes immaculate, professional structural rhythm and geometric consistency across the entire horizontal row. The layout alignment looks beautiful, unified, and intentional instead of feeling disjointed or offset.

3. **Strategic Advice-Driven Avatar State Changes**
   - **Choice**: Linked the player's avatar card dynamically to the strategic coaching advice engine string. When the coach recommends a strategic hit, stand, double down, or split, the avatar's emoji dynamically responds (e.g., Hit 👊, Stand ✋, Split ✂️, Double Down 🚀, Win 🥳, Lose 😭).
   - **Reason**: Enhances gameplay engagement and visual instruction loops. Players receive immediate tactile and visual feedback reinforcing their strategy options right in their avatar badge without needing to scan background status texts or read external message lines.

---

## 11. Zero-Dependency Image Compression and PDF Compilation Toolkit

### Context
Allowing users within the "Files" applet to run heavy offline media operations (photo scaling/downscaling, image format transcoding, and multi-page document creation) requires robust and memory-dense pipelines. We must perform these actions offline without introducing external binary libraries (such as iText, Apache PDFBox, or specialized image processors) that would excessively bloat the application's package footprint and introduce security or licensing complexities.

### Decisions & Justification

1. **Zero-Dependency Native BitmapFactory & Matrix Scaling Pipeline**
   - **Choice**: Implemented background image scaling and format transcoding inside `ImageReducerEngine.kt` using native `BitmapFactory` alongside `android.graphics.Matrix` fractional scaling.
   - **Reason**: Leverages firmware-optimized OS components. Solves Android heap memory limitations by utilizing a two-step decoding strategy: first loading bounds-only metadata (`inJustDecodeBounds = true`) to calculate the optimal mathematical sample size (`inSampleSize`), and then decoding the downscaled bitmap safely to prevent Out-of-Memory (OOM) exceptions.

2. **Native Graphics `PdfDocument` Canvas Building**
   - **Choice**: Used the Android framework's native `android.graphics.pdf.PdfDocument` API to compile images sequentially as vector/raster canvases within standard paper standard dimensions (such as A4 or Letter sizes).
   - **Reason**: Avoids heavy third-party PDF generators. Utilizing native `PdfDocument` ensures complete compliance with zero-dependency goals, zero licensing overhead, and minimal footprint increase, while providing standard margin calculations, orientation layouts, and perfect rendering accuracy.

3. **Dual Access Paths: Context-First & Dedicated Media Terminal**
   - **Choice**: Supported both **Workflow A** (contextual image scale/pdf merge shortcuts directly inside `ExplorerScreen.kt`'s selection action row) and **Workflow B** (a fully-interactive dedicated dashboard screen `ImagePdfScreen.kt` complete with multiple picker integrations, real-time file-size prediction heuristics, and a monospaced terminal activity log).
   - **Reason**: Enhances user flexibility. Simple in-place conversions can be triggered immediately without changing screens, whereas advanced, complex document compilation tasks are executed inside a standalone workspace featuring detailed processing statuses.

---

## 12. Multi-Driver Persistence & Database Access Layers (PostgreSQL & SQLite)

### Context
To support both simple low-resource installations and high-traffic or multi-container enterprise deployments of the multi-tenant PHP CMS, we need to offer an alternative to standard flat-file and localized SQLite databases. Specifically, the system must support **PostgreSQL** as a persistence layer while preserving **SQLite** as a fully zero-dependency alternative.

### Decisions & Justification

1. **Environment-Driven Configuration over Build-Time Locking**
   - **Choice**: Store connection configs using generic environment variables (`DB_DRIVER=sqlite` or `DB_DRIVER=pgsql`) parsed at boot.
   - **Reason**: Allows immediate environment transformations. System operators can swap persistence adapters dynamically without code changes or container image rebuilds.

2. **Lightweight Repository Isolation instead of Bloating with full ORMs**
   - **Choice**: Standardize on custom Repositories (e.g., `VisitsRepository`) paired with a dynamic `DB` Singleton PDO manager. We explicitly avoid heavyweight PHP ORMs like Eloquent or Doctrine.
   - **Reason**: Retains maximum page performance. Massive ORMs bring dozens of packages that bloat `/vendor`, degrade bootstrapping execution speeds, and reduce portability. Custom Repositories achieve query safety, dynamic schema translation, and pristine code maintainability under a zero-dependency footprint.

3. **Database-Agnostic Migration Hook**
   - **Choice**: Run a specialized `DatabaseMigrator::initTables()` sequence reading the active driver dynamically on system init.
   - **Reason**: Eliminates the risk of asynchronous migration state drift. Automatically sets up index patterns and dialect-appropriate field types on boot whether the system is loading onto a local SQLite container or connecting to a managed remote PostgreSQL cluster.

---

## 13. Sovereign 3D Game Upgrades: Spatial Rotations, Perspective Projections, and Volumetric Chip Physics

### Context
Upgrading turn-based RPG grids (Roguelike) and casino card tables (Blackjack) to high-fidelity three-dimensional views requires:
1. Projecting 3D geometric nodes cleanly onto a 2D screen viewport.
2. Resolving rendering overlaps (depth ordering) correctly without the performance overhead or dependency debt of massive external game engines like Unity or Unreal.
3. Visualizing physical quantities (like dollar bets and flat card covers) as realistic volumetric solids on the desktop.

### Decisions & Justification

1. **Unified Custom Math Pipeline (`SovereignEngine3D`)**
   - **Choice**: Established a localized 3D math package featuring `Vector3` matrix rotators, perspective projections, dynamic lighting vectors with square falloff, and robust `RenderItem3D` algebraic sealed classes.
   - **Reason**: Eliminates dependency debt. Running lightweight native math transformations on `Canvas` draw scopes provides high-performance, frame-rate stable renders with complete customizability.

2. **Yaw/Pitch Touch-Controlled Camera Orbits & Bounds Correction**
   - **Choice**: Coupled Compose pointer drag events to floating Yaw and Pitch state parameters, allowing users to physically "swivel" or orbit around the dungeon grid or casino card table felt in real-time. Crucially, the pitch angle is constrained to positive values (e.g., `0.15f` to `1.48f`) and its touch drag sign set to match correct physical panning, keeping the camera above the floor/walls rather than sliding underground or inverting the perspective matrices.
   - **Reason**: Enhances gaming feedback. Correcting the camera's orbital constraints avoids getting stuck underground or rendering inverted perspective matrices (Painter's sorting of front/back cells remains correct). Furthermore, expanding the zoom bounds from `0.15f` up to `10.0f` allows players to easily view complex layouts in their entirety or scrutinize fine details close-up.

3. **Grounded Flat Card Shadows and Extruded Wood Rims**
   - **Choice**: Programmed the card engine to draw slightly offset black polygons underneath card bodies at a higher Y felt plane. Extruded wood panel boundaries were generated by connecting rim facets in a 3D loop.
   - **Reason**: Emphasizes spatial realism. Drawing realistic ground shadows gives cards a visible "levitating" or physical placement look on the table. The extruded wood panel provides real geometric thickness and premium visual depth to the casino environment.

4. **Volumetric Stacked Chips Cylinders**
   - **Choice**: Built 3D chip stacks by programmatically deconstructing dollar amounts into the fewest red ($5), green ($25), and black ($100) cylinders, drawing dynamic top faces and shaded side bands that adapt to camera light sources.
   - **Reason**: Conveys bet value physically. Taller physical stacks correspond to higher bets, giving gamblers immediate, rewarding visual and psychological feedback when playing high stakes.


