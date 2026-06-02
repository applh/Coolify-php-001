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

## 4. Troubleshooting Guide

If you encounter GLB errors in the wild, check the following:

- **`Invalid GLB magic: ...`**: The file is likely not binary. Ensure it's not a `.gltf` plaintext JSON file renamed to `.glb`.
- **`Missing JSON chunk`**: Check if Git LFS corrupted the file or if it was manipulated via an improper network stream transfer.
- **OOM (Out Of Memory) during load**: Check the size of the embedded PNG/JPEG textures inside the GLB. Recommend baking them into single compressed texture sets.
