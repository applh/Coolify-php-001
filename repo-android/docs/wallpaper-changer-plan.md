# Wallpaper Changer Implementation Plan

## Architectural Decision: Another Applet or a Cronjob?

To properly support changing the wallpaper every 15 minutes on Android without excessive battery consumption and with a usable interface, the solution should utilize **both**.

1. **A Cronjob (for the Engine)**: Changing a wallpaper reliably every 15 minutes in the background requires the Android Jetpack `WorkManager`. Since this repository already implements a robust `WorkManager` wrapper via `CronWorker.kt`, the actual rotation logic should be managed as a **Cron Job**.
2. **An Applet (for the User Interface)**: You need a frontend to let the user configure which wallpapers should be selected, see the rotational history, and toggle the cronjob. This will be implemented as a new UI Applet (`WallpaperScreen.kt`).

---

## 1. UI Applet: `WallpaperScreen.kt`

We will add a new screen in the navigation graph in `MainActivity.kt`:

*   **Image Management**: Allow users to browse and select images (from local storage, camera output, or potentially generated via AI media features).
*   **Settings Toggle**: Provide a switch that interacts with `AgendaDatabaseHelper` to enable or disable the "Wallpaper Rotator" cronjob.
*   **Preview**: Show the currently active wallpaper and what image is queued up next.

## 2. Background Engine: `CronWorker.kt` modifications

We will leverage the existing scheduling structure to handle the actual rotation. 

*   **Database Seeding**: We will adjust `AgendaDatabaseHelper.kt`'s `seedDefaultData()` to seed a new modular cron task:
    ```kotlin
    val cronWallpaper = ContentValues().apply {
        put(COL_CRON_NAME, "Wallpaper Rotator")
        put(COL_CRON_EXPRESSION, "*/15 * * * *") // Every 15 minutes
        put(COL_CRON_IS_ACTIVE, 0) // Disabled by default
        put(COL_CRON_LAST_RUN, 0)
        put(COL_CRON_STATUS, "IDLE")
    }
    ```
*   **Execution Logic**: In `CronWorker.kt`, we add an execution branch for the wallpaper task:
    ```kotlin
    } else if (job.name.contains("Wallpaper", ignoreCase = true)) {
        // 1. Query the WallpaperApplet settings for the target image directory/list.
        // 2. Select the next image (random or sequential).
        // 3. Utilize `android.app.WallpaperManager` to apply the bitmap as the active system wallpaper.
        // 4. Log the result to the Cron Log telemetry database.
    }
    ```

## 3. Permissions & Requirements

*   We must add the `SET_WALLPAPER` permission to `AndroidManifest.xml`:
    `<uses-permission android:name="android.permission.SET_WALLPAPER" />`
*   Optionally add `<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />` depending on where the user draws their images from.

## Summary 
By treating the feature as a **Cronjob**, we satisfy the 15-minute background execution constraint natively without keeping the app alive. By adding an **Applet**, we give users full transparency and configuration control over the engine.
