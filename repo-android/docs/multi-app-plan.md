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

## Technical Details
- **Namespace**: `com.example.hubapp` (or keep `com.example.cameraxapp` and rename later).
- **Minimum SDK**: 21 (as currently defined).
- **Target SDK**: 35.
