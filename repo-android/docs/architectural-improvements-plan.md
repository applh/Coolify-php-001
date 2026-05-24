# FRAISE Android Architectural Assessment & Implementation Plan
*Authored by: Senior Software Architect*

This document provides a comprehensive architectural assessment of the current state of the FRAISE Android platform codebase (`repo-android`), evaluates systemic structural challenges, and establishes a concrete, step-by-step master implementation plan for high-yield architectural improvements and refactoring.

---

## 1. Executive Status & Architectural Evaluation
The FRAISE Android platform is a feature-rich, multi-functional offline-first operating suite (Camera, File Explorer, AI Team, Database Manager, Agenda/Cron Manager, sandboxed Browser, Wallpaper Scheduler, Backups). It exhibits exceptional functional capabilities and utilizes modern technologies like Jetpack Compose, CameraX, WorkManager, Room, DataStore, and the Google GenAI SDK.

However, the rapid accumulation of feature sets has led to severe **architectural density and structural bloat**.

### Core Technical Debt Metrics:
1. **Monolithic Code Placement**: File sizes are highly dense. Screen files contain complete feature packages:
   - `DBScreen.kt` (>1800 lines): Merges DB scanning, metadata tables introspection, dynamic SQLite column CRUD operations, visual design grids, custom table generation utilities, SQL command parser thread routing, and `DBViewModel` in a single file.
   - `AITeamScreen.kt` (>1600 lines): Merges ViewModel, Markdown syntax visualizers, pinch-pan Lightbox gestures, image generation AspectRatio adapters, session local-file serialization, and network dispatching.
2. **ViewModel Instantiation Anti-Patterns**: ViewModels are declared without dedicated Factories or Dependency Injection scopes within standard Compose composables.
   - For example: In `MainActivity.kt` lines 342-344: `remember { BrowserViewModel(...) }` bypasses standard VM lifecycle retention.
   - In `DBScreen.kt` line 754: ViewModels are fetched with `viewModel()` without injecting the repository context, leading to tight coupling with app context scopes.
3. **Database Leaks & Manual Query Parsing**: Direct low-level SQLite queries using traditional `SqliteOpenHelper` loops manually extract cursors with raw column strings. If an unhandled exception triggers inside a parsing loop prior to reaching `cursor.close()`, it risks leaking database file descriptors and locking the SQLite WAL journals.
4. **Scattered/Stale Core Themes**: Application-wide style parameters (colors, gradients, margins) are hardcoded inline, reducing compliance with Material You standards.

---

## 2. Target Clean Architecture Blueprint
To achieve absolute modularity, scalability, and robust performance, we propose a migration from a flat package layout to a **Layered Clean Architecture** structure:

```text
com.example.cameraxapp/
│
├── core/                         # Shared infrastructure and shared modules
│   ├── ui/                       # Shared design tokens and components
│   │   ├── theme/                # Universal Material You Theme tokens
│   │   └── components/           # Base Cards, Drawers, Floating Bars
│   ├── network/                  # Rest clients, OkHttp config, Gemini proxy builders
│   └── database/                 # Central DB Helpers and unified SQLite Connection pools
│
├── domain/                       # Pure Kotlin enterprise business logic definitions
│   ├── model/                    # Standard data models (e.g. AgendaEvent, CronJobEntity)
│   └── repository/               # Contract interfaces for data integration rules
│
├── data/                         # Concrete platform implementations of domain contracts
│   ├── repository/               # Repository implementations (e.g., AgendaRepositoryImpl)
│   ├── local/                    # SQLite database connectors, cursor builders, Dao handlers
│   └── remote/                   # HTTP zero-copy network clients
│
└── features/                     # Feature packages isolating specific applets
    ├── ai/                       # Chat, Multimodal Workspace, Imagen 3.0
    │   ├── presentation/         # Ui Composables & ViewModels
    │   └── domain/               # Session storage controllers & AI models
    ├── database/                 # Dynamic SQLite tool, terminal, dynamic tables dumper
    ├── agenda_cron/              # Alarms scheduling, WorkManager DynamicRouter, logs
    ├── browser/                  # Sandbox browser, script loaders
    └── config/                   # Settings, DataStore preference files
```

---

## 3. Core Architectural Improvement Directives

### A. Establish Central Dependency Injection (Service Locator Pattern)
To resolve the ViewModel initialization anti-patterns without forcing a full Hilt configuration shift immediately, we implement a persistent, thread-safe dependencies provider tied directly to the `Application` lifecycle context.

#### Dependency Container Contract:
```kotlin
package com.example.cameraxapp.core.di

import android.content.Context
import com.example.cameraxapp.AgendaDatabaseHelper
import com.example.cameraxapp.SettingsRepository
import com.example.cameraxapp.cronjob.CronJobDatabase

class AppDependencyContainer(private val context: Context) {
    val databaseHelper: AgendaDatabaseHelper by lazy {
        AgendaDatabaseHelper(context.applicationContext)
    }
    
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context.applicationContext)
    }
    
    val cronJobDatabase: CronJobDatabase by lazy {
        CronJobDatabase.getDatabase(context.applicationContext)
    }
}
```

#### Customized ViewModel Provider Factory:
```kotlin
package com.example.cameraxapp.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FeatureViewModelFactory(
    private val container: AppDependencyContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(com.example.cameraxapp.browser.BrowserViewModel::class.java) -> {
                val dlManager = com.example.cameraxapp.browser.BrowserDownloadManager(container.databaseHelper.context)
                com.example.cameraxapp.browser.BrowserViewModel(container.databaseHelper, dlManager) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
```

---

### B. Eliminate Direct Cursor Loops via Typed Safe-Query Executions
To secure database transaction integrity and solve memory leakage bugs, direct raw selections are wrapped in high-order extension utilities that guarantee cursor closure under standard kotlin execution blocks.

#### Thread-Safe SQL Query Wrapper:
```kotlin
package com.example.cameraxapp.core.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend inline fun <T> SQLiteDatabase.safeQuery(
    sql: String,
    selectionArgs: Array<String>? = null,
    crossinline transform: (Cursor) -> T
): List<T> = withContext(Dispatchers.IO) {
    val list = mutableListOf<T>()
    var cursor: Cursor? = null
    try {
        cursor = rawQuery(sql, selectionArgs)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(transform(cursor))
            } while (cursor.moveToNext())
        }
    } finally {
        cursor?.close() // Guarded against exception leakages
    }
    return@withContext list
}
```

---

### C. Standardize Multi-Applet Screen Navigation
Modernize the entry sequence within `MainActivity.kt` to load ViewModels properly using lifecycle boundaries:

```kotlin
// In MainActivity NavHost declarations:
composable("browser") {
    val factory = remember { FeatureViewModelFactory(appDependencyContainer) }
    val browserViewModel: BrowserViewModel = viewModel(factory = factory)
    BrowserScreen(viewModel = browserViewModel)
}
```

---

## 4. Architectural Refactoring & Rollout Roadmap

```
┌─────────────────────────────────┐
│ PHASE 1: Dependency Injection   │ ──> Standardize lifecycle scopes and decouple Settings, Database Helper components.
└─────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ PHASE 2: Core Platform Layering │ ──> Extract generic SQLite Query builders with standard safe Cursor wrappers.
└─────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│ PHASE 3: Applet Monolith Splits │ ──> Fracture DBScreen.kt & AITeamScreen.kt into presentation / domain packages.
└─────────────────────────────────┘
```

### Rollout Implementation Checklist:

- [ ] **Phase 1: DI Architecture Setup (Target: Week 1)**
  - Integrate `AppDependencyContainer` within `MainActivity.kt` setup sequence.
  - Implement the `FeatureViewModelFactory` to handle lazy initialization.
  - Test config state durability across simulated system kills.

- [ ] **Phase 2: Database Layer Refactoring (Target: Week 2)**
  - Inject `safeQuery` closures into `AgendaDatabaseHelper.kt`.
  - Replace unchecked cursor loops in `DBScreen` view models.
  - Inspect SQLite WAL log outputs to certify complete lock-free execution.

- [ ] **Phase 3: Package Splitting and Separation (Target: Week 3)**
  - Move UI components out of `DBScreen.kt` and create `com.example.cameraxapp.features.db.components`.
  - Extract Chat state machine flows from `AITeamScreen.kt` to separate files.
  - Measure compilation metrics and ensure there are no circular dependencies.

---

## 5. Summary of Architectural Advantages
1. **Zero Database leaks**: Automated cursor reclamation prevents locked SQLite state journals.
2. **Deterministic Durability**: Correct `ViewModelProvider` usage prevents ViewModel destruction on screen configuration swaps or layout switches.
3. **Pristine Modular Isolation**: Each individual applet has its own structural directory package, minimizing code modifications risk profiles during future feature additions.
