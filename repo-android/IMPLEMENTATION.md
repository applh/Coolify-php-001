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

## General Architectural Guidelines

- **UI Architecture**: Adopt the **MVI (Model-View-Intent)** or **MVVM** pattern. Each screen should have a distinct `ViewModel` to process user actions and emit immutable UI state data classes.
- **Navigation**: utilize **Jetpack Navigation Compose**. The `MainActivity` should host a `NavHost` bridging the three top-level destinations, optionally wrapped with a `BottomNavigation` bar.
- **Dependency Injection**: Consider using **Hilt** (or equivalent) to wire the view models with their respective repositories (Camera, Storage, User Preferences) seamlessly.
