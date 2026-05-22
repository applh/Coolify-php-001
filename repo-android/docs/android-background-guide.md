# Android Background Processing Guide

This document covers the core Android frameworks used in this project for background execution, precise scheduling, and system notifications.

## 1. WorkManager

`WorkManager` is the recommended Android Jetpack library for persistent, deferrable background work. We use it for tasks that must be executed reliably, even if the app ends or the device restarts.

**Key Concepts:**
- **Deferrable:** Appropriate for tasks like analytics syncing, database vacuuming, or media cleanup where exact timing is not required.
- **Constraints:** Work can be constrained by network availability, battery level, or charging state.
- **Usage in App:** Used under the hood by `CronScheduler` via `PeriodicWorkRequestBuilder` for periodic tasks.
- **Limitation:** `WorkManager` has a strict minimum interval of **15 minutes** for periodic tasks. Any interval less than 15 minutes will automatically be clamped to 15 minutes by the operating system. It is also subject to battery optimizations (Doze mode), which can cause execution times to drift slightly based on device state.

## 2. AlarmManager & Exact Timing Tasks

`AlarmManager` provides access to system alarm services. It allows you to schedule an application to run at a specific time in the future.

**Key Concepts:**
- **Exact Timing:** Used when events must happen at an exact time (e.g., calendar reminders, exact alarms).
- **Permissions:** Requires `SCHEDULE_EXACT_ALARM` on Android 12+ (API 31) to set precise schedules.
- **Usage in App:** 
  1. Used to trigger exact notifications for scheduled calendar events (Event Alarms).

## 3. Custom Receivers (`BroadcastReceiver`)

A `BroadcastReceiver` responds to system-wide or application-level broadcast announcements. 

**Key Concepts:**
- **AlarmReceiver:** Wakes up when the `AlarmManager` triggers an exact calendar or user alarm, parsing details to post a heads-up UI notification layout.
- **CronReceiver:** Wakes up to trigger background automation jobs for backwards compatibility to migrate old alarms over to WorkManager scheduling.
- **Execution Lifecycle:** Receivers have a short lifespan. Work that takes longer than a few seconds is safely delegated to `WorkManager` (e.g. `CronWorker`).

## 4. BootCompletedReceiver (`BroadcastReceiver`)

Android system broadcasts `Intent.ACTION_BOOT_COMPLETED` when the device finishes booting. 

**Key Concepts:**
- **Permission required:** Needs the `RECEIVE_BOOT_COMPLETED` permission in `AndroidManifest.xml`.
- **Usage in App:** Alarms set via `AlarmManager` are cleared by the OS upon reboot. `BootCompletedReceiver` listens for device restarts, queries the database for active alarms, and reschedules them tightly with `AlarmManager`. Note: `WorkManager` automatically handles periodic tasks across reboots, so we do not need to manually reschedule `CronScheduler` tasks using WorkManager on boot.

## 5. POST_NOTIFICATIONS Permission

Starting with Android 13 (API 33), apps must request runtime permission to send notifications.

**Key Concepts:**
- **Runtime Flow:** Instead of having permission granted at install time, users are explicitly prompted to grant `POST_NOTIFICATIONS` via `permissionsLauncher.launch()`.

## 6. OEM Battery Optimizations & "App Closed" Restrictions

If Background tasks, Alarms, or Cron Jobs fail to trigger automatically, it is overwhelmingly likely due to how the user "closed" the app, combined with device manufacturer (OEM) power-saving software.

**"App Closed" (Home/Back) vs. "Force Stopped" (Swipe from Recents):**
- **Normal Backgrounding (Working):** Pressing the Home button, switching to another app, or using the Back button to exit the app puts it in a cached state. `AlarmManager` and `WorkManager` **WILL** continue to work and will successfully wake up the app at the scheduled time (subject to the 15-minute WorkManager minimum).
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
