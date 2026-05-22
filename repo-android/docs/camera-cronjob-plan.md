# Camera Cronjob Implementation Plan

## Objective
Implement a background cronjob that automatically captures a photo using the device's camera every 15 minutes and saves it to local storage.

## Background & Constraints
As detailed in `android-background-guide.md`, Android imposes strict limitations on background execution and hardware access to preserve battery and user privacy:
1. **15-Minute Minimum:** `WorkManager` has a strict minimum interval of 15 minutes for periodic tasks. This perfectly aligns with the requirement.
2. **Background Camera Access:** Since Android 9 (API 28), accessing the camera while the app is in the background is prohibited. To legally access the camera, the process must hold foreground priority. In our implementation, we must transition our WorkManager worker into a **Foreground Service** (using `setForeground()`) to display a notification and gain legitimate access to the camera hardware.
3. **App Closed State:** Swiping the app from 'Recents' (Force Stop) will suspend WorkManager queues on many OEM devices. Users will need to avoid force-closing the app or lock the app in the Recents tray for guaranteed 24/7 background execution.

## Requirements
1. **WorkManager Setup**: Configure a `PeriodicWorkRequest` running every 15 minutes.
2. **Headless Camera Access**: Use CameraX (or Camera2) without a visible Surface/Preview to capture an image in the background.
3. **Foreground Service Promotion**: Promote the worker to a foreground service to comply with OS privacy rules.
4. **Storage**: Save the captured images silently to app-specific local storage, avoiding the need for extensive permissions.

## Technical Approach

### 1. Permissions & Manifest Updates
In `AndroidManifest.xml`, we need to request necessary permissions:
- `android.permission.CAMERA` (with `android.hardware.camera` feature)
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_CAMERA` (Required on Android 14+)
- `android.permission.POST_NOTIFICATIONS` (To show the required foreground service notification)

### 2. Camera Worker (`CoroutineWorker`)
Create `CameraPeriodicWorker` extending `CoroutineWorker`:
- Scheduled via WorkManager with a `15.toMinutes()` interval.
- Overrides `doWork()`.
- **Foreground Promotion:** The first step in `doWork()` is calling `setForeground(...)` with `ForegroundInfo` to show a transient "Capturing photo..." notification. This satisfies Android's requirement for camera access.
- Initializes the camera capture mechanism, takes a picture, unbinds, and returns `Result.success()`.

### 3. Headless Camera Capture (CameraX)
Using CameraX typically requires a `LifecycleOwner` (like an Activity or Fragment). For background workers:
- We can implement a custom `LifecycleOwner` that starts its lifecycle upon capture start and destroys it immediately after the image is saved.
- We configure an `ImageCapture` use case (ignoring `Preview` use case since there is no UI).
- Bind the `ImageCapture` use case to the `ProcessCameraProvider` with our custom lifecycle.
- Execute `imageCapture.takePicture()`.

### 4. Storage & State
- **Storage Location:** Save to `context.filesDir/camera_cron/` with a timestamp-based filename.
- **UI Enabling:** Add a toggle in `SettingsScreen.kt` or `CameraScreen.kt` to start/stop the cronjob. Before starting WorkManager, ensure we prompt the user for Camera and Notification runtime permissions.

## Step-by-Step Implementation Plan
1. **Add CameraX Dependencies**: Ensure `camera-core`, `camera-camera2`, and `camera-lifecycle` are available in `build.gradle`.
2. **Update AndroidManifest**: Add camera and foreground service permissions. Ensure Service types include `camera`.
3. **Create Custom LifecycleOwner**: Develop `BackgroundLifecycleOwner` to drive CameraX in a headless environment.
4. **Develop `CameraPeriodicWorker`**: Implement the `WorkManager` class handling foreground promotion and the capture process.
5. **Build `CameraCaptureManager`**: Encapsulate the CameraX logic to initialize, bind `ImageCapture`, take a picture to a File, and tear down.
6. **Integrate with UI**: Implement a toggle to initialize scheduling. Validate permission states.
7. **Testing**: Run the app, start the cronjob, place it in the background, and verify that every ~15 minutes the notification flashes and a new picture lands in local storage.
