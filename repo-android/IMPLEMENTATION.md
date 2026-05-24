# Android Application Implementation Plan

This document outlines the detailed implementation plan for the core features in the Android application: Camera, File Explorer, and Settings.

## 1. Camera Feature (`CameraScreen.kt`)

**Objective**: Build a robust, fully functional camera interface using the CameraX framework.

**Implementation Steps**:
- **CameraX Setup & Permissions**: 
  - Request `CAMERA` runtime permission using Activity Result APIs or Accompanist.
  - Initialize the `ProcessCameraProvider` and bind the camera lifecycle to the current Compose lifecycle.
- **Preview Use Case**: 
  - Embed `androidx.camera.view.PreviewView` into Compose using `AndroidView`.
  - Configure resolution, aspect ratio, and target rotation.
- **Image Capture Use Case**: 
  - Implement an `ImageCapture` use case bound alongside the Preview.
  - Create a custom Shutter Button in Compose.
  - On shutter click, trigger image capture and save the resulting JPEG to the application's internal files directory (or MediaStore for public access).
- **Core Controls**:
  - Add a **Lens Switch** toggle to swap between the front and back cameras.
  - Add a **Flash Toggle** (Auto, On, Off).
  - Display a floating thumbnail preview of the most recently captured image (navigates to Explorer on click).

## 2. File Explorer Feature (`ExplorerScreen.kt`)

**Objective**: Allow users to browse, view, and manage media captured by the app.

**Implementation Steps**:
- **Data Layer / Repository**: 
  - Build a repository that queries the local file system (app's internal storage) or `MediaStore` to fetch a list of captured images.
  - Yield updates via a Kotlin `Flow` so the UI automatically updates when new photos are taken.
- **Grid Interface**: 
  - Use `LazyVerticalGrid` to display a responsive masonry or uniform grid of image thumbnails.
  - Integrate an asynchronous image loading library (like **Coil**) for memory-efficient thumbnail rendering.
- **File Management & Detail View**:
  - Implement a full-screen mode to view an image when tapped.
  - Add actions/buttons to **Share** (using native Android ShareSheet) and **Delete** media files.
- **Empty State**: Show a helpful "No photos yet" illustration / message when the directory is empty.

## 3. Settings Feature (`SettingsScreen.kt`)

**Objective**: Provide user-configurable application preferences.

**Implementation Steps**:
- **Preferences Storage**: 
  - Setup Jetpack `DataStore` (Preferences) to persist user settings asynchronously.
  - Create a `SettingsRepository` to expose preferences as `StateFlow` streams.
- **Theme Configuration**: 
  - Include settings to select the App Theme (System Default, Light Mode, Dark Mode).
  - Observe this preference at the `MainActivity` level to dynamically update the customized `MaterialTheme`.
- **Camera Configurations**: 
  - Default Lens Facing choice (Start with Front vs Back).
  - Default Flash mode choice.
- **Storage Configuration**: 
  - Toggle between saving to internal app directories (private) vs public directories (accessible by other gallery apps).
- **UI Implementation**:
  - Use standard Compose list components (row items with icons, text, and trailing controls like `Switch` or dropdown icons).
  - Sub-dialogs for multiple-choice selections.

## 4. Next Implementation Phase

**Objective**: Extend the application with advanced camera capabilities and robust file management.

**Implementation Steps**:
- **Video Recording**:
  - Integrate CameraX `VideoCapture` and `Recorder` APIs to support video capture alongside image capture in `CameraScreen.kt`.
  - Update the Shutter Button to handle both short taps (photo) and long presses or toggle modes (video).
  - Ensure necessary `RECORD_AUDIO` permissions are requested if audio is required.
- **Enhanced File Explorer View (Zoom & Pan)**:
  - Add pinch-to-zoom and pan functionality to the full-screen image view in `ExplorerScreen.kt` using Compose's `transformable` modifier or dedicated zoomable libraries (e.g., `Telephoto`).
  - For videos, integrate `ExoPlayer` within the full-screen view with standard playback controls (play, pause, seek), allowing users to properly preview video recordings.
- **Full CRUD on File Explorer**:
  - **Create/Read**: Expanded to support creating new files with any file suffix dynamically. Read includes general file-system support and folder-tree traversal.
  - **Update / Edit Mode**: Specialized Markdown (.md) document editor for editing and saving on-the-fly, alongside custom folder/file renaming overlays (Update actions).
  - **Delete**: Support batch deletion by enabling a multi-select mode in the grid view and individual deletions.
- **SD Card Storage Integration**:
  - Expand the `SettingsScreen` to offer "SD Card" as a storage location choice when an external volume is mounted.
  - Use `ContextCompat.getExternalFilesDirs` to retrieve paths to removable media, and handle permissions/SAF (Storage Access Framework) interactions if writing outside app-specific directories.

## 5. Common UX and Responsive Design

**Objective**: Ensure a professional, cohesive user experience across all applets with responsive layouts that adapt to window resizes (e.g., split-screen, tablets, ChromeOS, foldables).

**Implementation Steps**:
- **Modal Navigation Drawer for All Applets**:
  - Implement a `ModalNavigationDrawer` at the root of the app that provides centralized access to the Camera, File Explorer, and Settings screens.
  - Remove standard bottom navigation bars in favor of the drawer to maintain consistency and save vertical screen estate, particularly on landscape or resizable window orientations.
- **Responsive Layout Design**:
  - Use `WindowSizeClass` (Compact, Medium, Expanded) to adapt the UI dynamically as the window resizes.
  - Automatically adjust grid columns in the File Explorer (e.g., 3 on Compact, 5 on Medium, 7 on Expanded).
  - On larger screens (Medium/Expanded), consider showing the drawer persistently (`PermanentNavigationDrawer`) or side-by-side with content for better space utilization.
  - Maximize the Camera preview surface contextually, avoiding cropped areas on unusual aspect ratios.

## 6. Professional Features

**Objective**: Elevate the functionality of each applet to professional-grade standards suitable for power users.

**Implementation Steps**:
- **Camera Applet (`CameraScreen.kt`)**: 
  - Add manual focus, exposure compensation, and white balance locks.
  - Implement grid lines (rule of thirds, golden ratio), histogram, and level indicator overlays.
  - Enable RAW capture support (DNG) alongside JPEG.
  - Add barcode/QR scanning integration and face detection overlays.
- **File Explorer Applet (`ExplorerScreen.kt`)**: 
  - Introduce crumb-trail navigation and a dual-pane layout option for expanding screen real estate (tablets/foldables).
  - Enable ZIP creation/extraction, secure file deletion, and batch operations (rename, move, copy).
  - Add regex-based search, sorting by meta-attributes (EXIF data, size, extension), and hidden file toggles.
  - Integrate SMB/FTP access for remote file management.
- **Settings Applet (`SettingsScreen.kt`)**: 
  - Provide deep theming options (Material You dynamic colors, custom accent hex codes) and font scale overrides.
  - Implement biometric lock (fingerprint/face) for accessing specific applets or sensitive settings.
  - Allow exporting and importing of app configurations as JSON files.
  - Provide built-in developer options including readable crash logs, storage usage breakdowns, and performance profiling toggles.

## 7. AI Team Applet (`AITeamScreen.kt`)

**Objective**: Provide a simple chat interface capable of generating text and images via the Gemini API, while saving all inputs and outputs to the local file system.

**Implementation Steps**:
- Refer to `docs/ai-team-plan.md` for the detailed feature rollout and API integration strategy.

## 8. Supported Media File Formats & Implementation Plan

**Objective**: Explicitly define the media formats handled by the application (images, video, audio, text) and outline the steps to implement or enhance their support.

### Supported & Planned Formats

*   **Images**: 
    *   **JPEG (`.jpg` / `.jpeg`)**: Currently supported for standard image capture via CameraX.
    *   **RAW (`.dng`)**: Planned for professional camera features.
    *   **PNG (`.png`)**: Supported via AI Team generated imagery and downloads.
*   **Video**:
    *   **MP4 (`.mp4`)**: Planned for standard video recording via CameraX `VideoCapture` and `Recorder` APIs.
*   **Audio**:
    *   **AAC (`.m4a` format)**: Planned as the audio track within MP4 video recordings. Standalone voice memos/audio recordings could be supported via `MediaRecorder`.
*   **Text/Documents**:
    *   **Markdown (`.md`)**: Supported for AI Team session outputs and formatted chat logs.
    *   **Plain Text (`.txt`)**: Supported for raw logs or code snippets.
    *   **JSON (`.json`)**: Supported for exporting/importing app configurations and schemas.
*   **PDF (`.pdf`)**: Not currently supported natively, but could be integrated via PDF viewer intents or generated from AI markdown logs in the future.

### Implementation Plan for Media Formats

1.  **Image Format Hooks (Phase 1 - In Progress)**:
    *   Ensure the `ImageCapture` use case properly passes the `JPEG` output format to the MediaStore/File system.
    *   Integrate Coil in `ExplorerScreen.kt` to decode and display JPEGs, PNGs, and eventually DNG thumbnails seamlessly.
2.  **Video & Audio Support (Phase 2)**:
    *   Add CameraX `Recorder` configured to output `MPEG_4` format.
    *   Wire up audio recording permissions and muxing to ensure AAC audio is bound to the MP4 file.
    *   In `ExplorerScreen.kt`, implement an `ExoPlayer` instance to decode and playback `.mp4` files.
3.  **RAW Image Pipeline (Phase 3)**:
    *   Add a toggle in `SettingsScreen.kt` for "Pro Mode" which enables RAW capture.
    *   Implement saving logic for `.dng` (Digital Negative) formats in the camera repository.
    *   Ensure the media gallery can display RAW files, potentially requiring a custom decoder if standard Coil limits apply.
4.  **Text & PDF Output Generation (Phase 4)**:
    *   Finalize AI Team save logic to write formatted `.md` files to the local file system.
    *   *(Future)* Add a utility to format markdown output into a structured `.pdf` document using Android's native `PdfDocument` printing APIs for shareability.

## 9. Gemini API Media Handling & Implementation Plan

**Objective**: Define how the Gemini API integrates directly with the application's supported media file formats for multimodal inputs and outputs in the AI Team applet.

### Gemini API Supported Media Formats

*   **Image Inputs (`.jpg`, `.jpeg`, `.png`, `.webp`, `.heic`)**:
    *   Fully supported via Gemini Multimodal capabilities (e.g., `gemini-1.5-flash` or `gemini-1.5-pro`). Users can attach images captured from the `CameraScreen` directly to their AI prompts within the `AITeamScreen`.
*   **Video & Audio Inputs (`.mp4`, `.m4a`, `.mp3`, `.wav`)**:
    *   Supported by the `gemini-1.5` models. Audio from voice notes or videos captured via CameraX will be passed utilizing the Gemini SDK/File API to allow the model to transcribe or analyze temporal media.
*   **Document Inputs (`.pdf`, `.txt`, `.md`, `.csv`)**:
    *   PDF and text analysis is supported natively. Users can select document formats from the file system to prompt Gemini for summarization, code analysis, or data extraction.

### Gemini API Output Formats

*   **Markdown & Plain Text (`.md`, `.txt`)**:
    *   The default text response format. Rich text, code blocks, and formatted lists from the Gemini API will be rendered as Markdown in the application and saved as `.md` files to the local file system.
*   **Structured JSON (`.json`)**:
    *   By enforcing a `responseSchema` or using `responseMimeType = "application/json"`, the API returns machine-readable JSON. The app will save these as `.json` files, which are useful for exporting tabular data or configurations generated by the model.
*   **Generated Images (`.png`, `.jpg`)**:
    *   Using integrations with image generation APIs (e.g., Imagen integration or DALL-E) alongside Gemini, output images will be downloaded and saved directly into the internal storage as `.png` or `.jpg` formats. These will immediately appear in the `ExplorerScreen`.
*   **Code Snippets**:
    *   Output code (e.g., Python, Kotlin, HTML) wrapped in Markdown code blocks can be extracted and saved as their native file formats (e.g., `.kt`, `.py`) depending on user intent parsed from the conversation.
*   **Audio Outputs (`.mp3`, `.wav`)**:
    *   Generated via Gemini's TTS capabilities or associated audio models. Audio streams will be captured and saved directly to the device storage for playback.
*   **Video Outputs (`.mp4`, `.gif`)**:
    *   Generated via integrations with video synthesis APIs. The application will handle the asynchronous downloading of these media payloads and write them to the File Explorer.
*   **Scalable Vector Graphics (`.svg`)**:
    *   Raw vector images generated as markup code inside the Gemini text responses. These will be parsed, extracted, and explicitly saved as native `.svg` files.

### Implementation Plan for Gemini Media Integration

1.  **Phase 1: Basic Multimodal Images (In Progress)**:
    *   Integrate the Gemini Android SDK (`generativeai`) to accept `Bitmap` instances natively for the `generateContent` stream.
    *   Add an "Attachment" button to the `AITeamScreen` input bar, allowing users to pick JPEGs/PNGs from the internal `ExplorerScreen` storage to send with their text prompts.
2.  **Phase 2: Long-Form Video & Audio Processing**:
    *   Implement upload handling to correctly send `MP4` / `M4A` file streams using the Gemini File API for files that exceed base payload limitations.
    *   Show progress indicators for large media files uploading to the Gemini File API before standard prompt execution.
3.  **Phase 3: Document & PDF Analysis**:
    *   Utilize Android's Storage Access Framework (SAF) to let users select PDFs or Text files outside the app's internal directory.
    *   Format the byte streams to Gemini with the correct MIME type (e.g., `application/pdf`) and render the contextualized response in the chat timeline.
4.  **Phase 4: Structured Data Generation & Export**:
    *   Configure strict schema requirements for prompts requesting structured output (e.g., asking Gemini to analyze an image and return object coordinates).
    *   Write the resulting structured data directly to the local file system (e.g., `analysis.json`) so it can be parsed or rendered by the File Explorer Applet.
5.  **Phase 5: Audio & Voice Synthesis (TTS)**:
    *   Integrate Gemini's Text-to-Speech routes (or native Android `TextToSpeech` as a fallback).
    *   Implement file streaming logic to save generated audio buffers as `.mp3` or `.wav` files and include playback UI directly in the chat stream.
6.  **Phase 6: SVG Rendering & Document Extraction**:
    *   Build a markdown parse step to aggressively capture raw HTML/SVG blocks emitted by the model.
    *   Export these snippets natively to `.svg` files and use vector-compatible rendering engines (e.g., `androidsvg`) to display previews instead of just showing code blocks.
7.  **Phase 7: Video Generation Integration**:
    *   Add worker tasks to interface with asynchronous video generation models triggered by Gemini text/image outputs.
    *   Handle backend polling, async `MP4`/`GIF` downloads, and write directly to the app's central media repository.

## 10. Database Manager Applet (`DBScreen.kt`)

**Objective**: Equip the Android application with an offline-first visual database manager to create and operate SQLite files, enabling dynamic table definitions and dynamic paginated data manipulation without requiring hardcoded schema models.

**Implementation Steps**:
- **Dynamic Connection Engine**:
  - Build a custom SQLite binder utilizing the native `android.database.sqlite.SQLiteDatabase` API.
  - Expose a `DBRepository` capable of dynamically querying table schemas (`PRAGMA table_info`), available database indices, and system metrics.
- **Dynamic UI Data Grid**:
  - Implement a multi-pane layout: standard collapsible navigation sidebar indexing available tables on the left, and a multi-directional scrollable data table container on the right.
  - Render an on-screen grid with pagination support using database `LIMIT` and `OFFSET` bounds for maximum query performance.
- **Visual Schema & Data CRUD**:
  - Build a user-friendly table designer screen to draft table names, select data types (TEXT, INTEGER, REAL, BLOB), and assign PRIMARY KEY constraints.
  - Implement dynamic form-builders that read column definitions to dynamically structure record insertion/edit Dialogs.
  - Implement drop-down actions to trigger drop table actions with prompt safety conformations.

## 11. Custom Background Automation & Periodic Tasks Suite (`CronWorker.kt` / `AlarmReceiver.kt`)

**Objective**: Harness Android's native system scheduling capabilities (`WorkManager` and `AlarmManager`) to run periodic, battery-optimized, and resilient operational routines that ensure local database hygiene, file storage health, and system telemetry monitoring.

### Catalog of Activatable Periodic Tasks

To provide an elite administrative dashboard, the application outlines a series of modular background routines configured via standard cron representations or exact timers:

1.  **Cache Sweeper & Storage Optimizer (Active)**:
    *   **Goal**: Automatically sweep transient directories to maintain optimal physical disk footprints.
    *   **Android Trigger**: `WorkManager` scheduled periodically with constraints `NetworkType.NOT_REQUIRED` and `RequiresBatteryNotLow` (Note: minimum 15-minute intervals apply).
    *   **Mechanism**: Safely crawls `applicationContext.cacheDir`, verifying file-modification headers. Destroys non-essential log dumps or cached visual thumbnails older than 10 minutes (or user-defined time bounds).

2.  **App State & User Preference Backup Engine**:
    *   **Goal**: Create a portable snapshot of local database transactions and `DataStore` key-value pairs to prevent data loss.
    *   **Android Trigger**: Periodic overnight runner (e.g., `0 2 * * *` - daily at 2 AM) under `RequiresDeviceIdle` and `RequiresCharging` constraints.
    *   **Mechanism**: Serializes the SQLite tables and preferences into a compressed `backup_[timestamp].json` payload written to Scoped Storage under a secure `/backups/` route.

3.  **Database Log Rotator & SQLite Vacuum**:
    *   **Goal**: Prevent index thrashing and disk footprint bloat of the active `agenda_hub.db` file.
    *   **Android Trigger**: Monthly or weekly cron execution (`0 0 * * 0` - Sundays at midnight).
    *   **Mechanism**: Trims telemetry history logs older than 30 days from `cron_logs`, re-indexes key tables, and issues a standard native database `VACUUM` statement to release unneeded file blocks back to the OS.

4.  **Media Directory Integrity Warden**:
    *   **Goal**: Ensure the consistency of photos taken by `CameraX` and visual assets stored by the `AI Team` explorer.
    *   **Android Trigger**: Semi-daily periodic job.
    *   **Mechanism**: Scans the targeted local storage folders, verifying file-header descriptors against known signatures. Identifies and prunes corrupted zero-byte images or partial streaming downloads, and updates the local files list dynamically.

5.  **Exif Content & Geotag Enrichment Worker**:
    *   **Goal**: Automatically enrich captured media EXIF coordinates when localized permissions are granted seamlessly post-processing.
    *   **Android Trigger**: Deferred background queue triggered after camera activities or on a low-priority delay.
    *   **Mechanism**: Uses `androidx.exifinterface.media.ExifInterface` to read and supplement JPEG headers, aligning photo file timestamps with system GPS location offsets retrieved during capture.

6.  **Daily Morning Agenda Digest Notifier**:
    *   **Goal**: Provide push notifications summarizing the user's active schedules and exact calendar alarms for the day ahead.
    *   **Android Trigger**: Precise morning wakeup using `AlarmManager` with `setExactAndAllowWhileIdle` at 7:00 AM.
    *   **Mechanism**: Queries the local `agenda_events` SQLite database to aggregate upcoming records, then issues a custom styled local `Notification` summary with action links.

7.  **Battery & Resource Governor Guardian**:
    *   **Goal**: Adapt background sync frequency dynamically to prevent battery drain or mobile data overuse.
    *   **Android Trigger**: Broadcast-receiver listening to battery level and internet connection broadcasts.
    *   **Mechanism**: Adjusts WorkManager constraint thresholds on other background tasks — scaling down sync tasks to "manual-only" if battery falls below 20% or enters Battery Saver mode.

## General Architectural Guidelines

- **UI Architecture**: Adopt the **MVI (Model-View-Intent)** or **MVVM** pattern. Each screen should have a distinct `ViewModel` to process user actions and emit immutable UI state data classes.
- **Navigation**: utilize **Jetpack Navigation Compose**. The `MainActivity` should host a `NavHost` bridging the top-level destinations, integrating smoothly with the global Navigation Drawer.
- **Dependency Injection**: Consider using **Hilt** (or equivalent) to wire the view models with their respective repositories (Camera, Storage, User Preferences) seamlessly.
