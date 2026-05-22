# Android Background Processing Guide

This document covers the core Android frameworks used in this project for background execution, precise scheduling, and system notifications.

## 1. WorkManager
`WorkManager` is the recommended Android Jetpack library for persistent, deferrable background work. We use it for tasks that must be executed reliably, even if the app ends or the device restarts.

**Key Concepts:**
- **Deferrable:** Appropriate for tasks like analytics syncing, database vacuuming, or media cleanup where exact timing is not required.
- **Constraints:** Work can be constrained by network availability, battery level, or charging state.
- **Usage in App:** Used under the hood by `CronReceiver` as a `OneTimeWorkRequest` to execute heavy lifting off the main thread. We previously used `PeriodicWorkRequestBuilder`, but due to Android's App Standby restrictions (minimum 15-minute wait, heavy OS-driven delays), we replaced the periodic scheduler with an exact AlarmManager chain.

## 2. AlarmManager & Exact Timing Tasks
`AlarmManager` provides access to system alarm services. It allows you to schedule an application to run at a specific time in the future.

**Key Concepts:**
- **Exact Timing:** Used when events must happen at an exact time (e.g., calendar reminders, exact cron schedules, alarm clocks).
- **Permissions:** Requires `SCHEDULE_EXACT_ALARM` on Android 12+ (API 31) to set precise schedules.
- **Usage in App:** 
  1. Used to trigger exact notifications for scheduled calendar events (Event Alarms).
  2. **CronScheduler:** We use exact alarms chaining (AlarmManager launches `CronReceiver`, which does the work and schedules the *next* Alarm) to guarantee exact Cron intervals, completely bypassing `WorkManager`'s 15-minute minimum limit and aggressive Doze Mode drifting.

## 3. Custom Receivers (`BroadcastReceiver`)
A `BroadcastReceiver` responds to system-wide or application-level broadcast announcements. 

**Key Concepts:**
- **AlarmReceiver:** Wakes up when the `AlarmManager` triggers an exact calendar or user alarm, parsing details to post a heads-up UI notification layout.
- **CronReceiver:** Wakes up to trigger background automation jobs. It immediately enqueues a `OneTimeWorkRequest` to do the heavy background processing, and calculates/registers the next exact alarm time for the cron cadence.
- **Execution Lifecycle:** Receivers have a short lifespan. Work that takes longer than a few seconds is safely delegated to `WorkManager`.

## 4. BootCompletedReceiver (`BroadcastReceiver`)
Android system broadcasts `Intent.ACTION_BOOT_COMPLETED` when the device finishes booting. 

**Key Concepts:**
- **Permission required:** Needs the `RECEIVE_BOOT_COMPLETED` permission in `AndroidManifest.xml`.
- **Usage in App:** Alarms set via `AlarmManager` are cleared by the OS upon reboot. `BootCompletedReceiver` listens for device restarts, queries the database for active alarms and cron configurations, and reschedules them tightly with `AlarmManager` so no scheduled events or automation intervals are lost.

## 5. POST_NOTIFICATIONS Permission
Starting with Android 13 (API 33), apps must request runtime permission to send notifications.

**Key Concepts:**
- **Runtime Flow:** Instead of having permission granted at install time, users are explicitly prompted to grant `POST_NOTIFICATIONS` via `permissionsLauncher.launch()`.

## 6. OEM Battery Optimizations & "App Closed" Restrictions
If Background tasks, Alarms, or Cron Jobs fail to trigger automatically, it is overwhelmingly likely due to how the user "closed" the app, combined with device manufacturer (OEM) power-saving software.

**"App Closed" (Home/Back) vs. "Force Stopped" (Swipe from Recents):**
- **Normal Backgrounding (Working):** Pressing the Home button, switching to another app, or using the Back button to exit the app puts it in a cached state. `AlarmManager` and `WorkManager` **WILL** continue to work and will successfully wake up the app at the scheduled time.
- **Force Stop (Failing):** Manually swiping the app away from the Recent Apps list is treated by most major OEMs (Samsung, Xiaomi, OnePlus, Huawei) as a user-initiated **Force Stop**. 

**The Consequence of a Force Stop:**
When an app is swiped from recents:
1. The process is immediately killed.
2. The OS unilaterally **cancels all standard active alarms** (`AlarmManager`).
3. The OS **suspends all `WorkManager` queues**.
4. The app **will NOT trigger** any background tasks until the user manually taps the app icon again to relaunch it, or the device is rebooted (triggering our `BootCompletedReceiver`).

**Troubleshooting Battery Killers & Swipes:**
- Users must explicitly navigate to system **Settings > Apps > Your App > Battery** and set it to "Unrestricted" or "Ignore Battery Optimizations". 
- **Workarounds:** On devices with aggressive Recents killing, users often need to "Lock" the app in the Recents tray (e.g., pulling down on the app card to show a padlock icon) to prevent accidental swiping from destroying scheduled crons. For guaranteed background execution even when swiped, Android requires a `Foreground Service` (which displays a persistent, un-swipeable notification).
