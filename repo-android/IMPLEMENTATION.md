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
  - **Create/Read**: Already partially implemented via capturing and viewing. Read needs to be expanded to support grouping or sorting.
  - **Update**: Implement file renaming functionality (e.g., via a dialog) and potentially basic image editing intents if required.
  - **Delete**: Support batch deletion by enabling a multi-select mode in the grid view.
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

## General Architectural Guidelines

- **UI Architecture**: Adopt the **MVI (Model-View-Intent)** or **MVVM** pattern. Each screen should have a distinct `ViewModel` to process user actions and emit immutable UI state data classes.
- **Navigation**: utilize **Jetpack Navigation Compose**. The `MainActivity` should host a `NavHost` bridging the top-level destinations, integrating smoothly with the global Navigation Drawer.
- **Dependency Injection**: Consider using **Hilt** (or equivalent) to wire the view models with their respective repositories (Camera, Storage, User Preferences) seamlessly.
