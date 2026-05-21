# AI Team Session Management - Technical Implementation Plan

This document outlines the architecture, data structures, and step-by-step implementation plan for adding fully functional, robust **Multi-Session Multi-Turn AI Conversations** (Text + Image) in the `repo-android` Lumina AI Team applet.

---

## 1. System Topology & File-Based Storage

As defined in our initial architectural decisions, Lumina AI session logs are persisted strictly as **flat JSON files** in the application's local file store (`context.filesDir/AITeam/`). This guarantees complete local persistence, isolates user histories, and allows the File Explorer applet to browse generated markdown, plain text, and visual outputs without custom database readers.

### Directory Structure & Hierarchy

```text
context.filesDir/
└── AITeam/
    ├── sessions_manifest.json           <-- Central index tracking all historical sessions
    └── sessions/
        ├── session_a0f2b8fd-c9c0.json  <-- Text-based multi-turn dialog sequence for Session A
        ├── session_f7e15ae1-102c.json  <-- Text-based multi-turn dialog sequence for Session B
        └── ...
```

- **Manifest Storage (`sessions_manifest.json`)**: Minimizes boot overhead by only storing Session Metadata (ID, Title, timestamp of last activity, token stats, and visual thumbnails references). Loaded on-click or on-startup.
- **Session Segment Files (`sessions/session_{uuid}.json`)**: Contains the absolute conversational history (messages array), ensuring rapid, lazy loading of individual sessions without loading the user's complete history into random-access memory (RAM).

---

## 2. Standardized Data Models (Moshi Compatible)

To maintain clean JSON serialization and deserialization via the Square Moshi parser, the following Kotlin types are used.

### A. Session Manifest Header
Represents an individual session item inside `sessions_manifest.json`.

```kotlin
package com.example.cameraxapp.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SessionHeader(
    val id: String,                 // UUID format
    val title: String,              // Human-readable summary (e.g., "Designing Kotlin Enums")
    val startTime: Long,            // Milliseconds epoch
    val lastActiveTime: Long,       // Milliseconds epoch for sorting (newest first)
    val totalTokens: Int,           // Rolling cumulative value
    val modelPreset: String,        // e.g., "gemini-2.5-flash-image" or "gemini-2.5-flash"
    val highlightsCount: Int        // Number of visual assets generated in this session
)
```

### B. Dialogue Message Entities
A sequence of these represents a single session's detail file inside `sessions/session_{id}.json`.

```kotlin
package com.example.cameraxapp.models

import com.squareup.moshi.JsonClass

enum class ChatRole {
    USER,
    MODEL
}

enum class Modality {
    TEXT,
    IMAGE
}

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val id: String,                 // UUID for list identification
    val role: ChatRole,             // USER or MODEL
    val content: String,            // Markdown text or description prompt
    val modality: Modality,         // TEXT or IMAGE
    val timestamp: Long,            // Log timestamp
    val imageUrl: String? = null,   // Scoped storage URI if modality == IMAGE
    val durationMs: Long? = null,   // Performance metric
    val calculatedCost: Double? = null // Cost estimation
)

@JsonClass(generateAdapter = true)
data class SessionSegment(
    val sessionId: String,
    val messages: List<ChatMessage>
)
```

---

## 3. Serialization & Disk I/O Layer (Thread Safety)

To prevent locking the main Android UI thread during large JSON saves, Disk operations are managed via Kotlin Coroutines bound to the `Dispatchers.IO` pool.

### File Controller Reference Blueprint
This helper interface encapsulates robust JSON reading/writing operations.

```kotlin
package com.example.cameraxapp.storage

import android.content.Context
import com.example.cameraxapp.models.ChatMessage
import com.example.cameraxapp.models.SessionHeader
import com.example.cameraxapp.models.SessionSegment
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class SessionStorageController(private val context: Context) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val baseDir = File(context.filesDir, "AITeam")
    private val sessionsDir = File(baseDir, "sessions")

    init {
        if (!baseDir.exists()) baseDir.mkdirs()
        if (!sessionsDir.exists()) sessionsDir.mkdirs()
    }

    // --- MANIFEST OPERATIONS ---

    suspend fun getSessionHeaders(): List<SessionHeader> = withContext(Dispatchers.IO) {
        val manifestFile = File(baseDir, "sessions_manifest.json")
        if (!manifestFile.exists()) return@withContext emptyList()
        return@withContext try {
            val json = manifestFile.readText()
            val listType = Types.newParameterizedType(List::class.java, SessionHeader::class.java)
            val adapter = moshi.adapter<List<SessionHeader>>(listType)
            adapter.fromJson(json)?.sortedByDescending { it.lastActiveTime } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveSessionHeaders(headers: List<SessionHeader>) = withContext(Dispatchers.IO) {
        try {
            val manifestFile = File(baseDir, "sessions_manifest.json")
            val listType = Types.newParameterizedType(List::class.java, SessionHeader::class.java)
            val adapter = moshi.adapter<List<SessionHeader>>(listType)
            val json = adapter.toJson(headers)
            manifestFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- CONVERSATION SEGMENT OPERATIONS ---

    suspend fun getSessionSegment(sessionId: String): SessionSegment? = withContext(Dispatchers.IO) {
        val sessionFile = File(sessionsDir, "session_${sessionId}.json")
        if (!sessionFile.exists()) return@withContext null
        return@withContext try {
            val json = sessionFile.readText()
            val adapter = moshi.adapter(SessionSegment::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveSessionSegment(segment: SessionSegment) = withContext(Dispatchers.IO) {
        try {
            val sessionFile = File(sessionsDir, "session_${segment.sessionId}.json")
            val adapter = moshi.adapter(SessionSegment::class.java)
            val json = adapter.toJson(segment)
            sessionFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            // Delete segment file
            val sessionFile = File(sessionsDir, "session_${sessionId}.json")
            if (sessionFile.exists()) sessionFile.delete()

            // Update Manifest Header Index
            val headers = getSessionHeaders().filterNot { it.id == sessionId }
            saveSessionHeaders(headers)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
```

---

## 4. State Machine & ViewModel Architecture

The ViewModel acts as the single-source-of-truth governing active user prompts, selection state headers, operational modes, and responses from the Gemini API.

We'll design the upgraded `AITeamViewModel` to house:
- **`activeSessionId: StateFlow<String?>`**: Triggers the system to load the dialogue segments when altered.
- **`manifestState: StateFlow<List<SessionHeader>>`**: Feeds the UI's historical session drawer or floating navigation tabs.
- **`currentTimeline: StateFlow<List<ChatMessage>>`**: Represents the ordered rendering list of text and images in the active session.
- **`uiState: StateFlow<ConversationUiState>`**: Reflects loading, busy, streaming (text updates), success, and error diagnostic states.

```kotlin
sealed interface ConversationUiState {
    object Idle : ConversationUiState
    object Connecting : ConversationUiState
    data class Success(val responseText: String) : ConversationUiState
    data class Error(val errorMessage: String) : ConversationUiState
}
```

### Workflows for Common Session Triggers

```text
A. BOOT / AUTO-RESUME WORKFLOW
App Start ──> Check Manifest ──> Find Newest Session Header ──> Load Session Segment ──> Display in App Screen
                                            │
                                            └──> If manifest is empty ──> Trigger StartNewSession()

B. START NEW SESSION WORKFLOW
Toolbar Button "+" click ──> Generate UUID ──> Clear Current Chat Screen ──> Save Empty Manifest Entry ──> Redraw UI

C. APPEND MULTI-TURN RESPONSE WORKFLOW
User Prompt Submitted ──> Add Message USER (TEXT) to currentTimeline ──> Save Local File ──> Hit Gemini network endpoint 
       ──> Stream Text / Base64 Pixel payload ──> Add Message MODEL to currentTimeline ──> Save Local File ──> Refresh screen
```

---

## 5. Jetpack Compose UI Components Mockup Layout

The screen adapts beautifully between narrow screens (portrait phone) and wide displays (landscape tablets, ChromeOS, split-screen browsers) through a modern, responsive **Dual-Pane Split Canvas** whenever possible.

### Layout Elements & Hierarchy (Bento Grid Style)

1. **Toolbar Header**:
   - Left-aligned menu drawer icon.
   - Branded text `"Lumina AI"` (Gemini Hub).
   - Right-aligned floating Action elements:
     - **"New Session" button (`+` Icon)**: Immediately invokes the UUID purge operation to clear the main canvas and start a new conversation branch.
     - **"History" Drawer Toggle**: Dispatches floating sidebar to pick prior headers.
2. **Conversational Scroller (Main Window)**:
   - Dynamic bubbles tailored with specialized rounded corners mapping back and forth for `USER` and `MODEL`.
   - Markdown components rendering bold text, inline bullet items and elegant dark-mode code snippets.
3. **Control Bottom Dock**:
   - Flat input field.
   - Checkbox: `[ ] Generate Image` (Toggles the conditional dispatch block redirecting the prompt logic).
   - Synthesize Action Send button.

### Lightbox Touch gestural overlay card UI
When tapping on any generated image in the chat canvas, a high-resolution overlay opens with pinch-to-zoom mathematical operations (using Compose scale tracking mechanics):

```kotlin
Modifier.graphicsLayer {
    scaleX = scale
    scaleY = scale
    translationX = offset.x
    translationY = offset.y
}
```

---

## 6. Detailed Implementation & Integration Schedule

To guarantee a stable, zero-regression build cycle, the feature is rolled out sequentially.

### Phase 1: Data Schema Verification & Storage Control Tests
- Create data models under `com.example.cameraxapp.models`.
- Implement `SessionStorageController` under `com.example.cameraxapp.storage`.
- Write structural unit files or local tests to verify flat JSON reads, writes, and manifest indexing run correctly on Android environments.

### Phase 2: Refactoring ViewModel States
- Update `AITeamScreen.kt` to define a single orchestrating `AITeamViewModel` replacing the simple standalone `ImageGeneratorViewModel`.
- Integrate current settings repository properties (such as key retrievals).
- Set up automatic session recover flow: scans the manifest on VM init and restores context.

### Phase 3: Text Conversation Pipeline via Gemini Retrofit API
- Enhance the current `GeminiApi` client to support text-generation endpoints (e.g., standard text request payload using `models/gemini-2.5-flash:generateContent`).
- Handle streaming text rendering inside Jetpack Compose, appending models' outputs sequentially.
- Trigger auto-saves to individual JSON segments on every prompt-response transaction completion.

### Phase 4: UI Scaffolding & Responsive Layout Integration
- Create the sidebar list in Compose which pulls from `manifestState`.
- Incorporate nice floating styles, touch visual feedback, and transition animations during thread changes.
- Add delete actions directly beside session items in the list.

### Phase 5: Checkbox Image and Gestural Lightbox
- Wire up the conditional checkpoint image toggle in the bottom input pane.
- Intercept the input prompt: if checked, dispatch content requests to image models, receive Base64 bytes, and write `.png` visual artifacts to Scoped Storage.
- Hook up the dismissible overlay lightbox dialog modal complete with multitouch gesture tracking, completing the Lumina Canvas specification.

---

## 7. Automated Periodic Rerun & Session Replication Specification

To empower local batch-processing workflows—such as generating a daily intelligence digest, repeating automated status audits, or scheduled image content generation branches—the application framework integrates saved Multi-Turn Sessions with Android's SQLite-backed background scheduling system (`CronWorker.kt` / `WorkManager`).

### A. Extended Schema for Scheduling Control
To support periodic executions, we extend the active JSON schemas (`SessionHeader` and `SessionSegment`) with attributes that store and maintain execution instructions:

```kotlin
// Append to com.example.cameraxapp.models.SessionHeader
data class SessionHeader(
    val id: String,                 
    val title: String,              
    val startTime: Long,            
    val lastActiveTime: Long,       
    val totalTokens: Int,           
    val modelPreset: String,        
    val highlightsCount: Int,
    // --- Scheduling Configurations ---
    val isPeriodic: Boolean = false,          // Toggle active cron scheduling
    val cronExpression: String? = null,        // e.g. "0 9 * * *" (Every day at 9 AM)
    val autoTriggerPrompt: String? = null,    // Prompt automatically executed on wake (e.g., "Analyze current storage logs and summarize")
    val systemInstructionsOverride: String? = null, // Optional system constraints
    val scheduleActive: Boolean = false,       // Current active scheduler toggle state
    val lastScheduledRunTime: Long = 0L,       // Millisecond tracking
    val executionCount: Int = 0                // Total background run rounds
)
```

### B. SQLite Database Alignment (`AgendaDatabaseHelper.kt`)
When a user marks an AI Team Session as "Periodic" in the Compose UI:
1.  **Register Cron Record**: Insert corresponding values into `TABLE_CRON_JOBS` via `AgendaDatabaseHelper`:
    -   `COL_CRON_NAME` -> `"AI Session: ${sessionHeader.title}"`
    -   `COL_CRON_EXPRESSION` -> `sessionHeader.cronExpression ?: "0 9 * * *"`
    -   `COL_CRON_IS_ACTIVE` -> `if (sessionHeader.scheduleActive) 1 else 0`
2.  **Metadata Association**: Save the Unique session UUID into the `cron_jobs` metadata context (or serialize inside the job's parameters matching `"CRON_SESSION_ID: [UUID]"`) to allow instant retrieval upon background worker wake-up.

### C. Background Compilation & Service Execution Pipeline in `CronWorker`
When `CronWorker.kt` triggers on the designated cron alarm execution step:

```text
                  [CronWorker wakes up from WorkManager / AlarmManager]
                                           │
                    [Retrieve CRON_ID and identify target Session UUID]
                                           │
                [Read session JSON segment from context.filesDir/AITeam/]
                                           │
               [Load Saved API Key from Cryptographic AppPreferences Store]
                                           │
         [Assemble active message history + Append autoTriggerPrompt text payload]
                                           │
            [Dispatch REST query: gemini-2.5-flash / models/generateContent]
                                           │
            [Await Response ──> Generate Local Markdown File / Save Image]
                                           │
           [Update JSON segments logs & Increment executionCount manifest metric]
                                           │
                     [Write success status to SQLite TABLE_CRON_LOGS]
                                           │
               [Display High-Contrast Local Push Notification Toast Summary]
```

### D. Detailed Implementation Operations for Developers

To implement the automated background rerun loop, edit `CronWorker.kt` to handle AI Team schedules:

1.  **Extract Parameters**: Read the target Session UUID from the job name or custom data inputs:
    ```kotlin
    val sessionUuid = job.name.substringAfter("AI Session: ", "").trim()
    if (sessionUuid.isNotEmpty()) {
        executeScheduledSessionRerun(sessionUuid, dbHelper, job.id)
    }
    ```
2.  **Restore Context & Prepare Prompt**:
    *   Initialize `SessionStorageController` and load the saved `SessionSegment` using `getSessionSegment(sessionUuid)`.
    *   Retrieve the corresponding `SessionHeader` from the manifest.
    *   Collect the historical conversational array (e.g., standard text lines or image cues).
3.  **Perform Non-Blocking Network Dispatch**:
    *   Verify immediate internet network status in WorkManager.
    *   Load the stored, decrypted `GEMINI_API_KEY` from `AppPreferences`.
    *   If the key is missing or blank, write `FAILED` telemetry status containing `"Missing Gemini API Key configuration"` and terminate gracefully.
    *   Assemble a retrofitted payload: append `sessionHeader.autoTriggerPrompt` as a new `ChatMessage(role = ChatRole.USER)` sequence.
4.  **Save Results & Notify System**:
    *   Append the response `ChatMessage(role = ChatRole.MODEL)` to the timeline list file.
    *   Update tokens metrics, last active timestamps, and increments `executionCount` in the manifest file.
    *   Write the execution metrics to `dbHelper.addCronLog()`.
    *   Issue a local `Notification` notifying the user that the AI workspace ran background updates successfully.

