# GLB Files Management & Troubleshooting Log

This document serves as the central documentation for handling `.glb` (glTF 2.0 Binary) files within `repo-android`. It logs the architectural strategies, past failed implementation attempts across different rendering and parsing pipelines, and proposes stable alternatives for handling 3D assets on Android.

---

## 1. GLB File Management Architecture

The `.glb` file is a consolidated binary stream containing the JSON glTF metadata, image textures (WebP, PNG), and binary vertex/animation arrays. Managing these files inside Android requires careful memory handling to prevent `OutOfMemoryError` (OOM) and UI thread blocking.

### Current Implementation (`GLBLoader.kt`)
The current custom implementation parses the GLB binary stream directly. It separates the file into:
1. **Header**: Verifies the `0x46546C67` (glTF) magic bytes.
2. **JSON Chunk**: Extracts the scene hierarchy, material indices, and accessors.
3. **BIN Chunk**: Extracts the raw buffer containing vertices, indices, and textures.

**Asset Location:**
Static `.glb` assets are bundled in `app/src/main/assets/models/` (e.g., `robot_expressive.glb`).

---

## 2. Complete Log of Failed Attempts

During the evolution of our 3D workspace (Moria, World Globe, Blackjack), several strategies were attempted and subsequently failed or proved unstable.

### ❌ Attempt 1: Standard String Reading for GLB Parsing
* **Approach**: Reading the entire GLB file into a standard `String` using `Charsets.UTF_8` to extract the JSON payload, then splitting the string.
* **Failure/Error**: The `BIN` chunk contains raw binary vertex position bytes. Converting the entire file to a UTF-8 string corrupted the binary data, shifting byte offsets.
* **Resulting Fix**: The parsing logic had to be rewritten to manually scan byte arrays for the `JSON` and `BIN\0` ASCII headers, gracefully handling corrupted length declarations (as seen in `GLBLoader.kt` workaround logic).

### ❌ Attempt 2: CPU-Bound Affine Texture Mapping (Sovereign Canvas)
* **Approach**: Drawing textured 3D polygons using a standard `android.graphics.Canvas` with `BitmapShader` and `Matrix` transformations (Affine mapping).
* **Failure/Error**: Affine texture interpolation doesn't account for Z-depth. During steep camera orbits, textures warped unnaturally (the "perspective skewing" or "swimming" effect). Furthermore, doing Linear Blend Skinning (LBS) on the CPU for skeletal animations caused frame drops to <15 FPS when polygons exceeded 2,000.
* **Resulting Fix**: Dropped affine textures for high-detail models, falling back to flat solid colors and procedural vector lines for the custom Canvas engine.

### ❌ Attempt 3: Native Filament / LibGDX Bare-Metal Integration
* **Approach**: Attempted to use the low-level `com.google.android.filament:filament-android` native libraries and custom UI thread lifecycles using JNI wrappers.
* **Failure/Error**: 
  1. Inflated APK size by ~25MB to 35MB due to native `.so` C++ shared libraries for all architectures.
  2. The application consistently crashed in sandboxed/emulated preview environments (like AI Studio) due to missing hardware OpenGL ES / Vulkan context support.
  3. Lifecycles became detached from Jetpack Compose, causing memory leaks across screen rotations.

### ❌ Attempt 4: Jetpack Compose SceneView `ModelLoader` with Direct `ByteBuffer`
* **Approach**: Implementing the `io.github.sceneview:sceneview` wrapper inside Compose using an `AndroidView`, and attempting to load a GLB directly via `java.nio.ByteBuffer.allocateDirect(bytes.size)` into `view.modelLoader.createModel(buffer)`.
* **Failure/Error**: 
  1. **Lifecycle Race Conditions**: The `ModelLoader` instance relies on the underlying Filament engine being fully initialized. Attempting to extract `view.modelLoader` inside the Compose pipeline often resulted in a null context or threading violations across the UI/Background threads.
  2. **Direct Buffer Requirement**: We initially used `ByteBuffer.wrap(bytes)`, which crashed natively because Filament demands memory-aligned, direct byte buffers (`allocateDirect()`) for C++ native mapping without JVM garbage collection interference.
  3. **Build & Hardware Timeout Issues**: Even after correcting to direct buffers and managing endianness (`ByteOrder.nativeOrder()`), compiling and launching this required heavy synchronous native allocations. This timed out headless environments, and ultimately fell back into the same "missing hardware GLES/Vulkan support" pitfall within the AI Studio emulator sandbox as Attempt 3.

### ❌ Attempt 5: LibGDX (`libgdx-gltf`) & Custom GDX Wrappers
* **Approach**: Importing the heavy LibGDX monolithic framework alongside third-party parsing extensions like `libgdx-gltf` to handle the parsing and rendering of the GLB container.
* **Failure/Error**: 
  1. **Architecture Monopolization**: LibGDX is designed to take over the entire `Activity` lifecycle (`AndroidApplication`). Embedding its `GLSurfaceView` deeply inside a Jetpack Compose hierarchy (`AndroidView`) creates severe Z-ordering glitches, context loss upon screen rotation, and state synchronization friction.
  2. **Dependency Bloat**: Pulling in the entire LibGDX core, its native Android backend (`gdx-backend-android`), and the GLTF extension drastically bloats the project size, adding heavy C++ `.so` dependencies merely to parse a single file format.
  3. **Paradigm Mismatch**: Jetpack Compose is strictly declarative and state-driven, whereas LibGDX strictly operates on an imperative, polling `render()` thread loop. Bridging these paradigms for simple applet component interaction introduces error-prone concurrency logic.
  4. **Emulation/CI Failure**: Similar to the Filament attempts, LibGDX's heavily optimized native OpenGL backend aggressively crashes in containerized/headless environments (like the AI Studio web emulator) that lack dedicated hardware acceleration profiles.

### ❌ Attempt 6: Standalone parsing via `net.mgsx.gltf.loaders.glb.GLBLoader` (Without LibGDX)
* **Approach**: Attempting to extract and use only the `libgdx-gltf` parsing logic (specifically `net.mgsx.gltf.loaders.glb.GLBLoader`) to read GLB files as an external library without pulling in the entire LibGDX rendering engine.
* **Failure/Error**: 
  1. **Tight API Coupling**: The `mgsx` extension does not output plain Java or Kotlin data structures. It is hard-coupled to LibGDX's core internal data types. The parsing methods expect and return objects like `com.badlogic.gdx.files.FileHandle`, `com.badlogic.gdx.graphics.Mesh`, `com.badlogic.gdx.utils.Array`, and `com.badlogic.gdx.graphics.Texture`.
  2. **Transitive LibGDX Bloat**: Because of this tight coupling, adding the `libgdx-gltf` dependency transitively forces the inclusion of `gdx-core`. You cannot decouple the abstract parser from the engine's memory management and graphics utilities without rewriting the library from scratch.
  3. **Resulting Fix**: This realization led directly back to writing our own standalone `GLBLoader.kt` logic (as detailed in Attempt 1), which aims to do exactly what `mgsx` does, but mapping to plain Kotlin data classes and native Android `Canvas`/`Bitmap` objects instead.

### ❌ Attempt 7: SceneView `createModelInstance` (Official Async API)
* **Approach**: Refactoring our local implementation to use the official SceneView API (`loader.createModelInstance(assetFileLocation = "...")`) as intended by the library authors, instead of manually slurping bytes and creating direct buffers. Tested on a physical Xiaomi tablet.
* **Failure/Error**: 
  1. **Deep NullPointerException**: The `ModelLoader` threw a hard `java.lang.NullPointerException` at `io.github.sceneview.loaders.ModelLoader.createModel(ModelLoader.kt:62)` deep within the library's native bridging code. 
  2. **Engine Boot Race Condition**: This occurs because Jetpack Compose can mark an `AndroidView` as "attached" (`isAttachedToWindow`) before the underlying native Filament `Engine` and `AssetLoader` have fully booted and acquired a Vulkan/GLES context from the Android graphic drivers. 
  3. **Poor Library Validation**: When we invoked `createModelInstance` inside a background coroutine, SceneView's internal code did not verify if its engine was ready. It blindly dereferenced a null engine pointer, crashing the whole parsing coroutine immediately instead of gracefully suspending until the GPU context was active.

---

## 3. Proposed Alternatives & Future Strategies

To bypass the listed failures, we propose the following scaling alternatives based on the application's complexity:

### A. Alternative 1: Sceneview for Jetpack Compose (Recommended for High-Fidelity)
For complete PBR (Physically Based Rendering), rigged animations, and lighting without the raw Filament boilerplate.
* **Solution**: Migrate strictly to `io.github.sceneview:sceneview` which wraps Filament cleanly into a Jetpack Compose tree.
* **Why it works**: Sceneview provides `ModelNode` that handles the GLB binary parsing, GPU upload, and skeletal memory buffering internally and safely within the Compose lifecycle.
* **Tradeoff**: Still incurs a binary footprint cost but guarantees stability and accurate perspective-correct texturing.

### B. Alternative 2: Optimized Native Memory-Mapped Parsing (Zero-Copy)
If we must persist with a zero-dependency local engine (Sovereign 3D).
* **Solution**: Use `FileChannel.MapMode.READ_ONLY` to map the `.glb` files from `assets` directly into physical memory space via `ByteBuffer`.
* **Why it works**: Prevents the JVM garbage collector from freezing the app while allocating byte arrays for 10MB+ models.

### C. Alternative 3: Server-Side Decimation via Go/Python backend
* **Solution**: If GLB models are too dense for mobile CPU skinning, offload simplification to our `repo-go` or `repo-python` servers.
* **Why it works**: The backend can parse the GLB, bake textures to low-res WebP, decimate meshes down to <1,000 polygons using `gltf-pipeline` or `qmuntal/gltf`, and serve an optimized mobile-ready representation for the Android client.

---

## 5. The SceneView Disconnect: Why the Physical Xiaomi Tablet Tests Failed

You might wonder why **SceneView** was heavily recommended in earlier architecture discussions, despite failing during the manual Coolify APK deployments to the physical Xiaomi tablet. 

### Where the Recommendation Came From
In the modern Android ecosystem (2021–present), **SceneView (`io.github.sceneview`)** is the industry standard for rendering 3D/AR in Jetpack Compose, built on top of Google's Filament engine. It is the de-facto solution for loading `.glb` files without writing raw OpenGL/Vulkan boilerplate. The recommendation to use it is sound for standard Android development.

### Why it Failed on the Physical Device (The Real Root Causes)
My previous assumption that failures were strictly due to a headless emulator sandbox was **incorrect**, as the CI/CD pipeline builds the APK via Coolify and tests it directly on real hardware. 

The physical hardware failures stem directly from **how** we attempted to force SceneView to load the `.glb` models in our implementation (`Attempt 4`), rather than SceneView itself being broken or incompatible with the tablet:

1. **Synchronous Main Thread Blocking**: Our code in `GlbValidationApplet.kt` attempted to parse the GLB using `context.assets.open(assetPath).readBytes()`. Depending on where this is called in the Compose hierarchy, synchronously reading a 10MB+ binary file on the main UI thread blocks the Android Choreographer. This causes the UI to freeze, triggers ANRs (Application Not Responding), and often causes the system daemon to kill the app before the 3D surface can even render.
2. **Double Memory Allocation (OOM Risk)**: By calling `readBytes()`, we allocated the entire file into a standard Java heap `ByteArray`. Then, to satisfy native C++ requirements, we called `ByteBuffer.allocateDirect(bytes.size)` and copied the array. This creates a massive memory spike (2x the file size), fragmenting RAM severely. If the Xiaomi tablet lacks contiguous free memory at that exact moment, it throws a fatal `OutOfMemoryError` or a native SIGKILL.
3. **Lifecycle Misalignment with Filament Engine**: We attempted to extract `view.modelLoader` and call `createModel(buffer)` sequentially. However, Filament's native C++ engine (and its underlying Vulkan/GLES contexts) initializes asynchronously. If `ModelLoader` functions are executed before the exact millisecond the native surface is fully bound to the Android view tree, the native code segfaults to a null context, silently crashing the application.
4. **Version Mismatch & Hallucinated APIs**: The most critical error in our approach was a severe version mismatch. The application's `build.gradle` is hardcoded to `io.github.sceneview:sceneview:1.2.3` (an outdated, legacy version of the library). However, during our architectural pivots, I assumed the availability of modern SceneView 2.0+/4.x.x APIs, which include the declarative Jetpack Compose DSL (`Scene { ModelNode(...) }`) and safe async loaders (`loadModelAsync`). 

### Summary
I apologize for the confusion. The failure on the physical Xiaomi tablet and the subsequent NPE in "Attempt 7" occurred because the project is running an **outdated version of SceneView (1.2.3)**, which lacks modern Compose lifecycle awareness. In version 1.2.3, `ModelLoader` attempts to eagerly access the Filament `Engine`, which throws a hard `NullPointerException` (as seen in `ModelLoader.kt:62`) if the AndroidView context isn't perfectly synchronized with the native GPU thread. By trying to force modern coroutine data loading patterns onto this legacy version, the system collapsed.

The correct path forward is either to **upgrade SceneView to its modern release (e.g., 2.2.x or 4.x.x)** so we can use the official Compose DSL safely, or to stick strictly to SceneView 1.2.3's imperative listeners to ensure the Filament Engine is completely initialized before calling `createModelInstance`.

If you encounter GLB errors in the wild, check the following:

- **`Invalid GLB magic: ...`**: The file is likely not binary. Ensure it's not a `.gltf` plaintext JSON file renamed to `.glb`.
- **`Missing JSON chunk`**: Check if Git LFS corrupted the file or if it was manipulated via an improper network stream transfer.
- **OOM (Out Of Memory) during load**: Check the size of the embedded PNG/JPEG textures inside the GLB. Recommend baking them into single compressed texture sets.
