# Autonomous Android Compilation & Self-Healing Agent

This architectural guideline defines how to configure an autonomous **Android Self-Healing Coding Agent** inside Google AI Studio. This agent automatically intercepts Kotlin/Gradle edits, executes remote Gradle compilations inside the sandboxed environment, extracts compiler trace bugs, and self-corrects the code in a repeating cycle until a valid APK compiles successfully.

---

## 🔄 1. The Autonomous Self-Healing Lifecycle

When a developer prompts an agent to add/modify features in `/repo-android` (e.g. Jetpack Compose UI changes, CameraX modules, background data synchronization), the agent executes the following post-edit validation loop:

```
        🛸 [ Developer Prompts Feature Change ]
                        │
                        ▼
           [ Agent Modifies Source Code ]
                        │
                        ▼
      [ Intercept Action: Gradle Compilation ] ──> Runs `gradle assembleDebug`
                        │
         ┌──────────────┴──────────────┐
         ▼                             ▼
  [ Build Fails ]               [ Build Succeeds ]
         │                             │
         ▼                             ▼
  [ Extract Compiler Errors ]    [ Save APK Output ]
  - File path & line numbers           │
  - Missing dependencies / imports     ▼
  - Deprecated symbol calls      🎉 [ Report Success & APK Path ]
         │
         ▼
  [ Apply Surgical Edits ]
         │
         └─────> (Repeat Loop; Capped at 3 cycles)
```

---

## 🛠️ 2. The Verification Suite: Gradle Integration

Because static code parsing alone cannot catch complex Compose compiler or transitive type crashes, the agent invokes the platform's native Gradle runtime inside `/repo-android` during its verification pass.

### Execution Commands

To compile and verify the Android module inside the developer workspace, the agent navigates to `/repo-android` and runs the optimized Gradle assemble targets:

```bash
# Execute local clean and assemble debug tasks
gradle --project-dir repo-android clean assembleDebug
```

*Tip*: Run standard Gradle builds with specific flag parameter configs to maximize speed and bypass unnecessary check-suites:
- `--no-daemon`: Prevents the background daemon JVM from locking memory inside the Cloud Run runtime.
- `--parallel`: Allows concurrent compile steps for separate modules.
- `-x test`: Skips standard unit tests during rapid compilation verifying loops (focusing exclusively on compiler syntax).

---

## 🤖 3. Enforcing the Automatic Compiler Loop via custom Rules

To guarantee that the model *never* stops at a broken, uncompleted compilation, developers can add a permanent directive inside the project's root **`AGENTS.md`** or **`GEMINI.md`**. This instructs any acting agent to run the Gradle loop automatically after every edit:

### System Prompt Blueprint for the self-healing Android Agent

```text
You are the Autonomous Android Engineer (Kotlin & Compose Specialist).
Your primary objective is to maintain a buildable codebase at all times.

### Operational Directives (Post-Edit Hook):
1. Whenever you modify any file inside `/repo-android` (including Kotlin, multi-module gradle configs, or manifests), you are STRICTLY FORBIDDEN from ending your turn without verifying compilation.
2. Immediately run the compilation command:
   `gradle --project-dir repo-android assembleDebug`
3. If the build succeeds, capture the path of the generated debug APK and announce it in your final summary.
4. If the build fails:
   - Carefully read the output trace. Locate the exact file name, line sequence number, and compile diagnostics (e.g., Unresolved reference, Type mismatch, Deprecated call).
   - Use your `view_file` tool directly on the broken file around the error line.
   - Construct a surgical replacement using `edit_file` to fix the reference or code structure.
   - Re-run the compile command.
5. Limit the self-healing cycles to a maximum of 3 iterations. If compilation fails after 3 turns, stop, output the exact compiler errors, and request human guidance.
```

---

## 🩺 4. Common Kotlin/Compose Compiler Diagnostics and Fix Mappings

An expert agent keeps a lookup dictionary of common Android API pitfalls, utilizing modern Kotlin alternatives.

### Hazard A: Deprecated Icons inside Jetpack Compose
*   **Compile Error**: `Unresolved reference: Icons.Filled...` (Often triggered when using direction-specific graphics like back-button arrows).
*   **Root Cause**: Jetpack Compose deprecates standard bi-directional arrows inside the default `Icons` collections to support mirror behaviors (RTL layouts).
*   **Correct Mapping**: Replace with auto-mirrored icons.
    ```kotlin
    // ❌ Deprecated and fails compile
    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
    
    // ✅ Modern and supported
    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    ```

### Hazard B: CameraX LifeCycle Binding Collisions
*   **Compile Error**: `Type mismatch: inferred type is LifecycleOwner but LocalLifecycleOwner.current was expected`
*   **Correct Mapping**: Ensure standard Compose lifecycle hooks match imports securely.
    ```kotlin
    // Import correct LocalLifecycleOwner
    import androidx.compose.ui.platform.LocalLifecycleOwner
    ```

### Hazard C: Missing XML Manifest Activity Definitions
*   **Compile Error**: Gradle build succeeds but assembly verification fails or target app applets crash on launch due to unregistered layouts.
*   **Correct Mapping**: Verify that any newly compiled custom `Activity` maps inside `AndroidManifest.xml` beneath the `<application>` node matching structural intents.

---

## 💸 5. Token Transaction Economics of Healing Loops

Because Gradle compiles consume considerable processing time, executing them sequentially is inexpensive in terms of computational pricing, but can consume context tokens if builds emit massive amounts of redundant standard logs.

To optimize token usage during healing loops:
1.  **Log Compression**: Instruct the agent to run Gradle with `-q` (quiet mode) or filter stdout using regex lines inside script runners to target only the `[ERROR]` block elements and suppress lengthy success traces.
2.  **Scope Context Caching**: Keep the cached codebase stable. Ensure that build artifacts (`build/` folders) are strictly listed in `.gitignore` or `.dockerignore` so that transient Gradle metadata does not poison the AI Studio Context Cache, preserving your **50% input billing discounts**.
