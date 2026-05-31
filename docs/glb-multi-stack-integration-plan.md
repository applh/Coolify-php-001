# Implementation Plan: Multi-Stack GLB & 3D Asset Integration

This document outlines the multi-stack engineering plan to integrate **GLB (glTF 2.0 Binary)** asset loading, parsing, material extraction, skeleton rigging, and animation pipelines across all backend, web, and mobile repositories in the workspace.

---

## 🎯 1. Recommended Libraries for GLB Management by Ecosystem

To manage, compress, parse, and render `.glb` files across different technology stacks, we select optimized, production-ready libraries for each language environment:

| Ecosystem | Recommended Library / SDK | Primary Role & Features |
| :--- | :--- | :--- |
| **Android (Kotlin)** | **Google Filament** / **Sceneview** | Real-time physically based rendering (PBR) engine. Filament provides native binary parsing, high-performance skeletal LBS skinning via Vulkan/OpenGL, and smooth keyframe animation transitions. Sceneview wraps Filament for Jetpack Compose. |
| **Flutter (Dart)** | **model_viewer_plus** / **Flutter 3D Controller** | Integrates Google's `<model-viewer>` component inside native webviews, supporting animations, camera orbits, and WebGL rendering. For pure native 3D, **three_dart** or **flutter_scene** (Filament bindings) are preferred. |
| **React (TypeScript)**| **Three.js** / **React Three Fiber (R3F)** / **@gltf-transform/core** | Industry-standard web 3D. R3F wraps Three.js for declarative React. `GLTFLoader` automates material, skeleton, and animation parsing. `@gltf-transform` enables server-side or build-time optimization, mesh simplification, and metadata editing. |
| **Vue (TypeScript)** | **TresJS** / **TroisJS** / **@google/model-viewer** | Declarative 3D frameworks for Vue 3 based on Three.js. For simple zero-configuration rendering, the standard `<model-viewer>` web component provides outstanding responsiveness. |
| **Python** | **pygltf** / **glTF-Transform (CLI/Node integration)** / **Open3D** | `pygltf` allows fast Python binary reading, JSON header extraction, and coordinate array manipulation. `Open3D` or `trimesh` offers extensive mathematical tools for vertex calculations, mesh analysis, and texture-baking operations. |
| **Go** | **qmuntal/gltf** - **fogleman/gg** | `qmuntal/gltf` is a highly efficient Go decoder/encoder for glTF 2.0 and GLB. It allows Go microservices to slice buffers, unpack texture files, and manipulate binary data pipelines at native speeds. |
| **PHP** | **Assimp (via native extension)** / **Three.js (Embedded)** | PHP backend parses GLB headers, buffers, and textures directly via standard file stream seek/read operations (`fopen`, `fread`). Exposes server-side model verification, format-conversions, and embeds client-side scripts for preview. |
| **Rust** | **gltf-rs** / **Bevy (Wasm/Native)** | `gltf` is the standard zero-allocation Rust crate for parsing glTF/GLB formats. `Bevy` is a modular entity-component-system (ECS) game engine offering superb low-level rendering, bone transformations, and skeletal animations. |

---

## 🛠️ 2. Multi-Stack Feature Integration Blueprints

Achieving feature-level integration of GLB 3D files requires coordinating frontend render loops, asset pipelines, and backend storage systems. Below are the functional designs for each repository in our workspace.

---

### A. React & Vue Web Clients (`repo-react` & `repo-vue`)
Web environments are the primary interaction portal for 3D visualizations. We integrate the dynamic canvas inside the existing multi-tenant dashboards.

#### 1. Architecture Flow
```text
[HTTP Client / Sandbox Iframe]
            │
            ▼ (Fetches site static resources)
    [React/Vue Router] ──► [GLB Preview Component]
                                 │
         ┌───────────────────────┴───────────────────────┐
         ▼                                               ▼ (If Rich Interactivity Requested)
 [Google Model-Viewer Component]             [Three.js / React Three Fiber Canvas]
  • Zero-Build performance                    • Custom shader uniforms & variables
  • Lighting & camera presets                 • Bone-node mesh vertex bone transformation
  • Animation clip selection                  • Dynamic coordinate picker inputs
```

#### 2. Key Web Integration Steps
* **Declarative Loading**: Embed React Three Fiber `<Canvas>` elements or Vue TresJS nodes. Feed files via `useLoader(GLTFLoader, '/models/asset.glb')`.
* **Dynamic Animations**: Access `gltf.animations` array, map them to an `AnimationMixer`, and start loops via `mixer.clipAction(clip).play()`. Keep playback synchronized using standard requestAnimationFrame ticks.
* **Ambient Lighting controls**: Bind custom reactive sliders to Three.js properties (color, intensity, position) to observe metallic and shiny material shifts dynamically.
* **Canvas Dimension Resiliency**: Guard WebGL surfaces inside styled relative-sizing containers using `ResizeObserver` callbacks to prevent WebGL viewport crashes on container modifications.

---

### B. Android Native Application (`repo-android`)
The native Kotlin application houses the 3D Roguelike (Moria) and Casino (Blackjack) suites. 3D performance must remain stable under hardware limitations.

#### 1. Implementation Architecture
* **Sovereign Engine (CPU-based Canvas)**: For lightweight models ($<2000$ polygons), write custom binary buffers and parsing logic in Kotlin. Run matrix rotations, depth sorting (Painter's Algorithm), and blend weights directly on secondary CPU threads before calling `Canvas.drawPath()`.
* **Filament Engine (GPU-based Shaders)**: Integrate Google Filament bindings inside Compose via standard `AndroidView`. Bind the GLB file properties using an asset loader, and load the vertex transformations, bone weights, and textures directly into GPU registers.

#### 2. Technical Roadmap & Thread Safety
* **Zero-Copy Memory-Mapped Buffers**: Feed files using Kotlin `FileChannel.MapMode.READ_ONLY` mapped directly inside memory buffers to prevent garbage collection allocations and avoid lag spikes.
* **LBS Matrix GPU Skins**: Upload joint palette structures as array matrices directly into uniform buffers on every frame tick. Let vertex shaders calculate vertex-bend coordinates natively using standard Linear Blend Skinning math.

---

### C. Flutter Cross-Platform Client (`repo-flutter`)
Flutter serves as the cross-platform compiler wrapper. It must integrate 3D rendering gracefully inside the Dart render pipeline.

#### 1. Integration Strategies
* **HTML/WebGL Bridging**: Leverage `model_viewer_plus` to render GLB files. The component injects a sandboxed WebGL browser instance under Dart coordinates.
* **Native scene graph (flutter_scene)**: Utilizing Flutter's modern graphics engine (Impeller), compile custom C++ binders to parse the GLB asset using glTF schemas, decode materials, and execute 3D keyframe timelines natively.

---

### D. Go Backend (`repo-go`)
The Go microservices perform super-fast backend asset-processing, file metadata validation, and coordinate extraction.

#### 1. Primary Go Roles
* **Parser & Schema Validator**: Use `qmuntal/gltf` to read incoming GLB uploads. Validate that the files contain legitimate glTF 2.0 schemas, block corrupt byte structures, and confirm that polygon counts, joints, and animations sit within acceptable server thresholds.
* **Coordinate Decimator**: Extract raw binary positions from `bufferViews`, execute high-speed decimation math in Go routines, slice index offsets, and save highly compressed, downscaled GLB files back to disk.

---

### E. Python Data & API Backend (`repo-python`)
Python provides the backbone for AI automation pipelines, geometric dataset analysis, and coordinate processing.

#### 1. Python Architecture Features
* **Metadata Extractors**: Write a FastAPI route `/api/gltf/analyze` reading upload objects. Run `pygltf` to output file breakdowns matching physical byte scales:
  ```python
  from pygltf import glTF2
  
  gltf = glTF2.load_binary("char_hero.glb")
  # Expose mesh counts, active skins, and animation names immediately as a JSON diagnostic
  ```
* **Baking Textures & Compression**: Integrate the `gltf-pipeline` Node tools or configure standard `trimesh` tasks to automate textures layout modifications, transcode raw high-density PNG files into downscaled WebP formats, and compress the asset using Draco mesh compression algorithms.

---

### F. PHP MVC Stack (`repo-php`)
The reference PHP deployment manages multi-tenant file storage, backup configurations, and dynamic media asset serving.

#### 1. PHP Architecture Features
* **Raw Binary Chunk Classifier**: PHP opens GLB file streams directly to inspect binary offsets. It reads the 12-byte header, verifies the magic bytes (`0x46546C67`), extracts Chunk 0 (JSON payload) to isolate the properties schema, and locates Chunk 1 (BIN buffer limits).
* **Image Asset Extractors**: Read the specified `bufferViews` mapping embedded WebP, PNG, or JPEG graphics, extract the coordinates into raw binary buffers, and serve them dynamically as independent image routes (`GET /api/media/texture?glb=char.glb&index=0`). This enables the web client to inspect textures on disk without parsing the full model asset.
* **Backup Management Integration**: Ensure that when a site's workspace is zipped using `CMS::zipSite`, active GLB formats within content folders are compressed and packaged seamlessly without losing directory structures or altering relative texture paths.

---

### G. Rust System Engine (`repo-rust`)
The Rust service provides bare-metal execution speeds, low-level binary manipulation, and high-safety processing.

#### 1. Rust Integration Specifications
* **Headless Binary Pipeline**: Leverage `gltf` crate to map indices and process buffers with zero memory allocations.
* **Dynamic Format Compilers**: Provide command-line features or WebAssembly endpoints parsing OBJ, FBX, or STL raw models and compiling them directly down into single, memory-optimized `.glb` binary containers.

---

## 📅 3. Multi-Phase Integration Milestones

```
  Phase 1: Binary Validator
    ├─ Implement binary inspectors in Go and PHP validating file signatures and schemas.
    └─ Create standard analytical FastAPI routes under Python inspecting models metadata.
  Phase 2: Client Canvas
    ├─ Integrate Model-Viewer and React Three Fiber previews in web and Flutter structures.
    └─ Configure Compose-native custom buffers and mapped files in Android (Moria/Blackjack).
  Phase 3: Skeletal & Textures Sync
    ├─ Extract embedded PNG/WebP files as Bitmaps/Textures dynamically on PHP.
    └─ Traversal of bone structures and execution of LBS joint skin shaders on Android/R3F.
  Phase 4: Animation & Cohesive Loops
    └─ Interpolate timelines, run Slerp quaternions calculations, and synchronize cycles.
    └─ Compile clean training labs in training folders documentation.
```
