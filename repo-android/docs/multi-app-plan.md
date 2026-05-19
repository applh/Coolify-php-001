# Multi-App Hub Implementation Plan

The goal is to transform the current `repo-android` project into a Multi-App Hub that serves several "applets" from a single launcher screen.

## Proposed Architecture

1.  **Hub Launcher**: A central screen appearing on app start that allows users to choose which applet to launch.
2.  **Navigation**: Use Jetpack Compose Navigation to handle switching between applets.
3.  **Applets**:
    *   **CameraX App**: Existing functionality migrated to a specific destination.
    *   **File Explorer**: New destination to browse local files.
    *   **Shared Settings**: New destination for configuring global app behavior.

## Phase 1: Infrastructure
- Add Navigation dependency to `build.gradle`.
- Define a `NavHost` in `MainActivity`.
- Create a `HubScreen` as the default destination.

## Phase 2: Applet Scaffolding
- **CameraX**: Wrap existing `CameraScreen` into the navigation flow.
- **File Explorer**: Implement a basic storage crawler and list view using Jetpack Compose.
- **Shared Settings**: Implement a simple settings UI with `DataStore` or `SharedPreferences`.

## Phase 3: Refinement
- Add permission handling per applet where needed.
- Polish the UI with a consistent design system.
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

## Technical Details
- **Namespace**: `com.example.hubapp` (or keep `com.example.cameraxapp` and rename later).
- **Minimum SDK**: 21 (as currently defined).
- **Target SDK**: 35.
