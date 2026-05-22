# Architecture Decisions Record

## 1. Gemini API Integration & Proxy Routing

### Context
The project contains multiple applications designed to interact with the Gemini API, primarily a web frontend (Vue.js) and an Android mobile application (`repo-android`). 

A bug emerged where a `404 NOT_FOUND` error was being thrown when calling the Gemini API, accompanied by a `MissingFieldException` in the Android SDK parsing. 

### Decisions & Corrections

1. **Model Version Upgrades and Verification (`gemini-2.5-flash`)**
   - The system was previously referencing older, deprecated models like `gemini-1.5-flash` and `gemini-1.5-flash-latest`.
   - **Fix**: The model names have been upgraded to `gemini-2.5-flash` across all applications (Android, server-side tasks, and PHP scripts). Deprecated models are no longer supported under standard `v1beta` endpoint configurations, causing 404 errors. Standardizing to `gemini-2.5-flash` resolves this permanently.

2. **Middleware API Server (`server.ts`) for Web Frontend**
   - **Why `server.ts` is involved**: For the Vue web application, the Gemini API is called via a Node.js express middleware router (`server.ts`).
   - **Reason**: The AI Studio platform architecture requires that web browser clients proxy their requests through an internal server to securely inject the protected `GEMINI_API_KEY` environment variable. Pushing the API key to the client's browser would create a security leak.

3. **Direct Client Integration for Android (`repo-android`)**
   - **Decision**: The Android application does **not** rely on `server.ts`. It directly utilizes the Google GenAI SDK for Android (`GenerativeModel`).
   - **Reason**: Mobile app builds handle secrets/keys natively through property injection during the build process, and maintaining absolute privacy by eliminating the middleware server is desirable and possible in the client-side binary. The Web application is subject to different constraints since its code is served directly over the network to a browser.

### Conclusion
- The **web application** uses a proxy model (`server.ts`) for strict security and infrastructure compliance.
- The **Android application** uses a direct client-to-API model for optimal privacy and lower latency.
- Both refer to `gemini-2.5-flash` reliably.

---

## 2. Android Agenda Applet Architectural Choices

### Context
Developing the new **Agenda** (Calendar, Alarm, and Cron) applet under `repo-android` requires system-level hooks that remain reliable and performant without draining resources. 

### Decisions & Justification

1. **Precision Alarms using `AlarmManager`**
   - **Choice**: Use native Android `AlarmManager` with standard exact scheduling methods (`setExactAndAllowWhileIdle` or `setAlarmClock`).
   - **Reason**: Jetpack WorkManager runs periodic tasks with a minimum interval of 15 minutes and can delay executions due to battery saver regimes. Alarms and calendars require exact, second-level accuracy even when the device is in Doze Mode.

2. **WorkManager for Cron Background Tasks**
   - **Choice**: Standard periodic background automation routines corresponding to specific Cron schemas are mapped onto standard `WorkManager` items.
   - **Reason**: Highly complex custom exact-looping implementations can easily get blocked, killed, or cause immense memory/battery drain. Jetpack's `WorkManager` provides OS-native safety guarantees, persistence, execution tracking, and battery-aware conditions (network status, loading status).

3. **Reboot Resilience via `BroadcastReceiver`**
   - **Choice**: Bind a `BootCompletedReceiver` to listen for the standard `android.intent.action.BOOT_COMPLETED` event.
   - **Reason**: The Android OS flushes all scheduled intents in the `AlarmManager` whenever a device shuts down or reboots. Listening to standard system boot signals enables scanning the Room/SQLite store and cleanly rescheduling existing calendar triggers or alarm clocks on start-up.

---

## 3. Dynamic User-Managed Cronjob Framework & Foreground Services

### Context
We introduced a user-facing Cronjob Manager within the Android app, which allows users to dynamically create and manage background tasks (such as taking intermittent photos in the background) via an updated Jetpack Compose UI. This capability is fundamentally different from statically compiled workers.

### Decisions & Justification

1. **Single Router Worker (`DynamicRouterWorker`)**
   - **Choice**: A unified WorkManager `CoroutineWorker` handles various user-defined tasks based on `jobType` passed through input payload arguments, rather than creating specialized worker classes for each job type.
   - **Reason**: Greatly simplifies the scheduling engine since it allows dynamic instantiation of WorkManager constraints and generic interval scheduling handled through a universal Room-based job configuration registry without code-bloat.

2. **Foreground Service Promotion (`setForeground()`) for Hardware Tasks**
   - **Choice**: Any Dynamic Router job executing a hardware-dependent action, such as `CAMERA_CAPTURE`, is programmed to explicitly call `setForeground()` alongside publishing an active foreground service payload matching the `camera` type within `AndroidManifest.xml`. 
   - **Reason**: Android 9+ prevents background apps from accessing sensitive hardware (camera/microphone) to maintain privacy. Promoting the background worker temporarily to a foreground service displays a mandated persistent notification ("Capturing photo") fulfilling Android's security constraints and enabling legitimate headless captures via CameraX use cases.

3. **Room Database as Scheduler Truth Source**
   - **Choice**: Use a dedicated `CronJobDatabase` holding schemas for cron assignments (types, intervals, states).
   - **Reason**: Provides a completely decoupled persistent state that allows standard `BootCompletedReceiver` scripts and `CronJobScheduler` objects to reliably pull unexecuted jobs and resupply them cleanly to WorkManager across process deaths or reboots.