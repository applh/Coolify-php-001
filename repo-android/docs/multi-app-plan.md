# Multi-App Hub Implementation Plan

The goal is to transform the current `repo-android` project into a Multi-App Hub that serves several "applets" from a single launcher screen.

## Proposed Architecture

1.  **Hub Launcher**: A central screen appearing on app start that allows users to choose which applet to launch.
2.  **Navigation**: Use Jetpack Compose Navigation to handle switching between applets.
3.  **Applets**:
    *   **CameraX App**: Existing functionality migrated to a specific destination.
    *   **File Explorer**: New destination to browse local files.
    *   **Shared Settings**: New destination for configuring global app behavior.
    *   **DB (SQLite Manage)**: New destination for managing database files with full CRUD on schemas and rows.
    *   **Backup Manager**: New destination to back up and restore application databases, settings, and media using robust ZIP-archives and background cron automations.

## Phase 1: Infrastructure
- Add Navigation dependency to `build.gradle`.
- Define a `NavHost` in `MainActivity`.
- Create a `HubScreen` as the default destination.

## Phase 2: Applet Scaffolding
- **CameraX**: Wrap existing `CameraScreen` into the navigation flow.
- **File Explorer**: Implement a basic storage crawler and list view using Jetpack Compose.
- **Shared Settings**: Implement a simple settings UI with `DataStore` or `SharedPreferences`.
- **DB (SQLite Manage)**: Scaffold standard dynamic raw SQLite binder connections, file lists, and basic schema queries.

## Phase 3: Refinement
- Add permission handling per applet where needed (including Storage Access Framework for outside SQLite databases).
- Polish the UI with a consistent design system (neon glowing cyber accents, dark surfaces, robust responsive tables).
- Add "Back to Hub" navigation in each applet.

## Phase 4: Professional Features

**Objective**: Elevate the functionality of each applet to professional-grade standards suitable for power users.

### 1. Camera Applet
- **Advanced Controls**: Add manual focus, exposure compensation, and white balance locks.
- **Pro Overlays**: Implement grid lines (rule of thirds, golden ratio), histogram, and level indicator.
- **Format Support**: Enable RAW capture support (DNG) alongside JPEG.
- **Enhanced Utilities**: Add barcode/QR scanning integration and face detection overlays.

### 2. File Explorer Applet
- **Advanced Navigation**: Introduce crumb-trail navigation and a dual-pane layout option for expanding screen real estate (tablets/foldables).
- **Comprehensive CRUD**: Enable ZIP creation/extraction, secure file deletion, and batch operations (rename, move, copy).
- **Deep Search & Filter**: Add regex-based search, sorting by meta-attributes (EXIF data, size, extension), and hidden file toggles.
- **Network Support**: Integrate SMB/FTP access for remote file management.

### 3. Settings Applet
- **Granular Customization**: Provide deep theming options (Material You dynamic colors, custom accent hex codes) and font scale overrides.
- **Security**: Implement biometric lock (fingerprint/face) for accessing specific applets or sensitive settings.
- **Backup & Restore**: Allow exporting and importing of app configurations as JSON files.
- **Analytics & Logs**: Provide built-in developer options including readable crash logs, storage usage breakdowns, and performance profiling toggles.

### 4. DB Applet (SQLite Explorer)
- **Advanced Terminal**: Build an interactive SQL query terminal with history tracking and performance execution timers.
- **CSV/JSON Bridges**: Formulate robust CSV and JSON database importing/exporting pipelines with structural column auto-mapping.
- **Diagnostics Dashboard**: Integrate one-tap Pragma integrity examinations and VACUUM sweeps.
- **Advanced BLOB Decoders**: Add visual interactive lightboxes to dynamically view binary BLOB fields as text, image, or hex formats.

### 5. Backup Manager Applet
- **Zero-Copy ZIP Streams**: Package diverse databases (SQLite, Room), global settings (DataStore), and session files into a single ZIP using memory-efficient buffering streams.
- **Bootstrapper Profiles**: Enable full test profile seed arrays for rapid tester environment setup.
- **Atomic Rollback Restoration**: Use temp-directory staging logic to guarantee atomic folder overrides without risking database/settings halfway-corruption.
- **Background Cron Automations**: Integrate scheduled daily system database snapshots within WorkManager, backed by rolling limits.
- **Safe External Survival**: Expose Storage Access Framework (SAF) document creators, saving backups to persistent paths surviving app reinstalls.

## Technical Details
- **Namespace**: `com.example.hubapp` (or keep `com.example.cameraxapp` and rename later).
- **Minimum SDK**: 21 (as currently defined).
- **Target SDK**: 35.
