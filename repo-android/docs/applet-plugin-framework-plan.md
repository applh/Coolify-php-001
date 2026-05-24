# FRAISE Android Plugin-Driven Applet Framework Blueprint
*Authored by: Senior Software Architect*

This document outlines the architectural design and code-level blueprint to evolve the current multi-app hub into a dynamic, extensible **Plugin-Driven Applet Framework**. 

Rather than maintaining a rigid, monolithic navigation host and statically typed master-applet lists inside `MainActivity.kt`, this pattern establishes a formal **Applet Contract** and a central **Applet Registry Service**. Adding a new feature, utility, or custom screen becomes a "zero-touch" operation requiring no updates to launcher layouts, drawers, or core navigation tables.

---

## 1. Core Architectural Objectives
*   **Absolute Extensibility (Open-Closed Principle)**: The core system is open to extension (registering new applets) but closed to modification (never needing to touch `MainActivity.kt`, custom drawers, or launcher code).
*   **Enforced Lifecycle & Sandbox Isolation**: Applets are individually sandboxed and governed by central lifecycle bounds (initialization, activation, background schedules, and teardown).
*   **Automated Permission Gating**: Security configurations (such as standard camera or storage system permissions) are checked and requested dynamically on navigation boundaries by the core host, preventing crashes due to ungranted applet permissions.
*   **Decoupled cross-applet interactions**: Services communicate through a central, thread-safe asynchronous Event/Message Bus.

---

## 2. Technical Topology

```text
       ┌────────────────────────────────────────────────────────┐
       │                      MainActivity                      │
       └───────────────────────────┬────────────────────────────┘
                                   │
                                   ▼
       ┌────────────────────────────────────────────────────────┐
       │                     AppletRegistry                     │
       └───────────┬───────────────────────┬────────────────────┘
                   │                       │
      (Injected Core Services)       (Dynamic Route Generation)
                   │                       │
                   ▼                       ▼
       ┌──────────────────────┐ ┌───────────────────────────────┐
       │   AppletEventBus     │ │    Compose NavHost            │
       │   AppletScheduler    │ │   (Loops over registry items) │
       └──────────────────────┘ └──────────┬────────────────────┘
                                           │
                        ┌──────────────────┼──────────────────┐
                        ▼                  ▼                  ▼
                ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
                │ Camera Applet │  │ SQlite Applet │  │ Browser Applet│
                └───────────────┘  └───────────────┘  └───────────────┘
```

---

## 3. Structural Code Elements

### A. The Core Unified Applet Contract (`Applet.kt`)
Every individual utility or workflow screen is package-enclosed into a single class implementing this interface.

```kotlin
package com.example.cameraxapp.core.framework

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController

interface Applet {
    /**
     * Unique developer-facing string identifier mapped to NavHost routes (e.g. "camera_x_applet").
     */
    val id: String

    /**
     * Plaintext human-readable title shown inside launchers, Quick Drawers, and headers.
     */
    val name: String

    /**
     * Description explaining applet scope to end-users inside Settings toggles and launchers.
     */
    val description: String

    /**
     * Vector icon mapped to the specific applet inside drawers and hubs.
     */
    val icon: ImageVector

    /**
     * Set of Android manifest permissions required before launching the applet.
     * The core framework automatically halts transition and displays permission gates if missing.
     */
    val requiredPermissions: List<String>
        get() = emptyList()

    /**
     * Hook triggered by MainActivity on onCreate() initialization.
     * Allows applets to bootstrap background connections, database caches, or receivers.
     */
    fun onInitialize(context: Context) {}

    /**
     * Dynamic Jetpack Compose painter block representing the primary screen UI layout.
     */
    @Composable
    fun Content(
        navController: NavController,
        onOpenDrawer: () -> Unit,
        onOpenRightDrawer: () -> Unit
    )

    /**
     * Hook triggered when a global background Cron-Job wakes up this applet's routine.
     */
    fun onBackgroundCronTrigger(context: Context) {}

    /**
     * Hook triggered when the application lifecycle is shutting down or clear-caches are requested.
     */
    fun onDestroy() {}
}
```

---

### B. The Consolidated Central Registry (`AppletRegistry.kt`)
The registry acts as the single source of truth managing installed plugins, querying active states from local preferences, and dynamically dispensing view models or configurations.

```kotlin
package com.example.cameraxapp.core.framework

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

object AppletRegistry {
    private val _registeredApplets = mutableStateListOf<Applet>()
    val registeredApplets: List<Applet> get() = _registeredApplets

    /**
     * Registers an applet into the global pipeline during the cold launch sequence.
     */
    fun register(applet: Applet) {
        if (_registeredApplets.none { it.id == applet.id }) {
            _registeredApplets.add(applet)
        }
    }

    /**
     * Programmatic query returning a snapshot list of active, enabled applets.
     * Integrates with user selection filters on Settings screens.
     */
    fun getActiveApplets(activeIds: Set<String>): List<Applet> {
        if (activeIds.isEmpty()) return _registeredApplets
        // Settings/Launcher applets are marked immutable and protected
        return _registeredApplets.filter { activeIds.contains(it.id) || it.id == "settings" }
    }

    /**
     * Dispatches background scheduling execution blocks to active applets.
     */
    fun dispatchCronTriggers(context: Context) {
        _registeredApplets.forEach { applet ->
            applet.onBackgroundCronTrigger(context)
        }
    }
}
```

---

### C. Dynamic Routing Generation (`MainActivity.kt`)
By binding navigation routing configuration directly to the live `AppletRegistry` collections, the monolithic `NavHost` declarations are reduced into light, iterable loops.

#### Core Loop Substitution:
```kotlin
// Replacement of static nav composition:
NavHost(
    navController = navController,
    startDestination = "hub",
    modifier = Modifier.fillMaxSize()
) {
    // 1. Maintain the launcher hub entrypoint
    composable("hub") {
        HubScreen(
            navController = navController,
            registeredApplets = AppletRegistry.registeredApplets,
            repository = repository,
            onOpenDrawer = { scope.launch { leftDrawerState.open() } },
            onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
        )
    }

    // 2. Dynamically instantiate plugin routes
    AppletRegistry.registeredApplets.forEach { applet ->
        composable(applet.id) {
            // Intercept with automatic security permission checks!
            val hasAllPermissions = applet.requiredPermissions.all { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }

            if (hasAllPermissions) {
                applet.Content(
                    navController = navController,
                    onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                    onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
                )
            } else {
                // Renders generic permission gate with direct request launcher targeting applet spec
                DynamicPermissionGateScreen(
                    requiredPermissions = applet.requiredPermissions,
                    onPermissionsGranted = { navController.navigate(applet.id) },
                    onBackToHub = { navController.popBackStack("hub", inclusive = false) }
                )
            }
        }
    }
}
```

---

### D. System-Wide Shared Inter-Applet Message Bus (`AppletEventBus.kt`)
Provides a safe, modern Kotlin Coroutines `SharedFlow` backbone enabling disconnected applets to communicate with structured events without static dependencies on each other.

```kotlin
package com.example.cameraxapp.core.framework

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface AppletEvent

data class DatabaseChangedEvent(val dbName: String, val table: String) : AppletEvent
data class PhotoCapturedEvent(val filePath: String, val timestamp: Long) : AppletEvent
data class CronJobCompletedEvent(val jobId: String, val result: String) : AppletEvent

object AppletEventBus {
    private val _events = MutableSharedFlow<AppletEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    suspend fun publish(event: AppletEvent) {
        _events.emit(event)
    }
}
```

---

## 4. Multi-Applet Transformation Strategy

To implement this without breaking codebases midway, a 4-Step migration sequence ensures continuous build compilations.

### Phase 1: Establish the Framework Engine Contracts
*   Create files `Applet.kt`, `AppletRegistry.kt`, and `AppletEventBus.kt` inside package location `com.example.cameraxapp.core.framework`.
*   Establish `Application` initialization routines to initialize core registries.

### Phase 2: Convert Hardcoded Applet Screens into Independent Plugins
*   Refactor individual screens (such as `CameraScreen`, `ExplorerScreen`, `BrowserScreen`, etc.) to implement the generic `Applet` interface.
*   Enclose required permissions inside each respective class. For example, `requiredPermissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)` moves into the `CameraApplet` wrapper itself.

### Phase 3: Dynamic NavHost Loop and Permission Gateway setup
*   Modify `MainActivity.kt` to load dynamic composable routes from the registry snapshot.
*   Setup localized Dynamic Permission requests rather than checking on startup, enabling seamless lightweight runs when standard core features aren't used.

### Phase 4: Dynamic WorkManager and Cron Schedules Setup
*   Evolve background receivers like `CronReceiver` and `DynamicRouterWorker` to loop through active applets in the `AppletRegistry` and fire background trigger functions dynamically. 

---

## 5. Architectural Advantages & Scalability
1.  **True Isolation**: Zero interdependency across screen modules. Adding, replacing, or removing an applet is accomplished strictly by deleting or registering its implementation class.
2.  **Lean Startup Time**: Permissions are only processed on navigation borders, improving setup sequences because standard users do not have to accept camera and microphone controls upfront to read the file explorer.
3.  **Perfect Mock Integration**: Ideal for developers and and AI-led agent routines; new test benches can be mocked dynamically or loaded into the applet collection without modifying system shells.
