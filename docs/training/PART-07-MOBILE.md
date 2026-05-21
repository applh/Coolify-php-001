# Part 7: Mobile & Multi-Platform Systems (100 Hours)

Bridge the gap between web and mobile with Android and Flutter.

## Modules (10 Hours Each)
- M061: Android Kotlin Basics
- M062: Android Jetpack Compose
- M063: Android Architecture (MVVM)
- M064: Flutter Dart Foundations
- M065: Flutter State Management
- M066: Cross-Platform API Integration
- M067: Mobile Hardware Access
- M068: App Publishing & Distribution
- M069: Embedded Systems Intro
- M070: Project: Cross-Platform App
- M071: Android Build, Gradle & Coolify Deployment

## Practical Labs

### 1. Flutter UI (50h)
**Reference**: `repo-flutter/lib/main.dart`
- **Task**: Explore the declarative UI structure.
- **Exercise**: Build a simple list view that fetches site names from the API.

### 2. Android Lifecycle (40h)
**Reference**: `repo-android/`
- **Task**: Trace an Activity lifecycle.
- **Exercise**: Implement a simple notification trigger in the Android app.

### 3. Android Compiling & Coolify Deployment (30h)
**Reference**: `repo-android/Dockerfile`, `repo-android/build.gradle`
- **Goal**: Understand how to compile a Java/Kotlin Android project using Gradle and deploy it as a standalone containerized app via Coolify.
- **Exercise**: Modify the Android Dockerfile to adjust Gradle heap sizes and deploy the container to distribute the APK.
- **Complexity**: Part 4

### 4. Gemini SDK Error Diagnosing and Model Configuration (20h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`
- **Goal**: Learn how standard client-side error parsing works in the Google Generative AI Android SDK and resolve API version / model mismatch issues.
- **Exercise**: Diagnose API error codes like 404 NOT_FOUND caused by incorrect model identifiers (e.g. `gemini-1.5-flash-001`) and standard serialization crashes related to `GRpcError` missing `details`.
- **Complexity**: Part 3

### 5. Multi-Platform Gemini API Model Discovery and Recovery (10h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`, `server.ts`, `repo-php/class/AIAgent.php`
- **Goal**: Master active model enumeration and troubleshooting using the official Generative Language REST APIs to recover from API deprecation states.
- **Exercise**: Write a custom script to query the active models list (`v1beta/models`) to find replacement models like `gemini-2.5-flash` when ancestral models are deprecated.
- **Complexity**: Part 3

### 6. Android AI Session Recording, Replaying, and Regression Benchmarking (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`
- **Goal**: Learn how to serialize real-time LLM telemetry (inputs, outputs, response latency, token count, estimated cost) into persistent JSON files and build an automated playback regression benchmarker.
- **Exercise**: Implement an offline-first replayer inside Android Jetpack Compose that iterates through archived session prompts, re-queries the Gemini API to measure delta speed/cost variations, and renders side-by-side performance comparisons.
- **Complexity**: Part 5

### 7. Segmented AI Format Selection & Official Imagen 3.0 API Integration (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`
- **Goal**: Understand how to implement custom output format selectors in Jetpack Compose and use the official Imagen 3.0 REST API to generate high-fidelity images with proper JSON payload structure and base64 response decoding.
- **Exercise**: Integrate dynamic aspect-ratio selection (such as 16:9 or 4:3) and extend the custom `Surface` selectors to support the newly selected aspect ratios in the REST payload.
- **Complexity**: Part 3

### 8. Lumina AI / Gemini Canvas: Advanced Image Synthesis & Scoped Storage in Jetpack Compose (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/StorageUtils.kt`
- **Goal**: Master full-stack image generation workflow using standard Gemini Developer API with `gemini-2.5-flash-image` and `gemini-3.1-flash-image-preview` models, managing MVVM UI states, Retrofit requests (including `aspectRatio` and `imageSize` configs), writing files using modern Android Scoped Storage, and implementing secure FileProvider system shares.
- **Exercise**: Expand the design panel to dynamically toggle or slide between Gemini models, integrate interactive inspiration prompt chips, decode response candidates into local bitmaps, stream them to the relative MediaStore path (`Pictures/GeminiCanvas`) under `IS_PENDING` policies, and share them securely using system shares with `Intent.ACTION_SEND`.
- **Complexity**: Part 4

### 9. Dynamic App Color Palette & Lumina AI Theme Swapper (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/ui/theme/Color.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/ui/theme/Theme.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/AppPreferences.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/SettingsScreen.kt`
- **Goal**: Learn how to implement persistent user-selected application-wide themes and design custom high-contrast material color schemes (incorporating custom glowing cyber-neon highlights) in modern Jetpack Compose.
- **Exercise**: Extend the custom `CameraXAppTheme` to allow the user to select amongst three color palettes: Standard Mono-Red, Lumina AI Glowing Aurora, and an additional custom "Sunset Emerald" theme with mint teal and golden accents. Make sure the selection is fully saved in `DataStore` preferences and loads instantly across screen transitions.
- **Complexity**: Part 3

### 10. Centralized Preferences-Driven AI Engine & Billing Dashboard (15h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/SettingsScreen.kt`, `repo-android/app/src/main/java/com/example/cameraxapp/AppPreferences.kt`
- **Goal**: Understand how to decouple application UI screens from API configuration parameters by utilizing a centralized DataStore-backed preference architecture, and construct interactive request cost/token tracking overlays.
- **Exercise**: Retrieve and bind DataStore flows directly into your screen composables using `collectAsState`, use these saved preferences (API Key, model path, aspect ratio, image size) to issue generative requests seamlessly on button triggers, and display granular metrics outlining generation latency, prompt token count, and calculated server costs.
- **Complexity**: Part 4

### 11. Multimodal Session-First AI Chat Workspace & Interactive Lightbox in Compose (20h)
**Reference**: `repo-android/app/src/main/java/com/example/cameraxapp/AITeamScreen.kt`, `repo-android/docs/ai-team-plan.md`, `repo-android/docs/ai-team-session-management.md`
- **Goal**: Learn how to transition a single-purpose Android applet into a full multimodal chat workspace. Master checkboxes beside action builders, smart default session persistence from historical flat-files manifest registries, and multitouch gestures (pinch-to-zoom/pan) to create custom gallery lightboxes.
- **Exercise**:
  1. Add a custom `Checkbox` beside the main chat submission button in compile-ready state. Integrate conditional routing to issue standard text/markdown responses when unchecked, and image bitmap downloads when checked.
  2. Implement an automatic scan during initialized launch in the `ViewModel` to load the highest-timestamp session from standard `sessions.json` local flat files by default.
  3. Add a plus icon toolbar shortcut to instantiate a brand-new chat workspace by rotating UUID state containers.
  4. Build a custom lightbox Dialog with Compose `PointerInputScope` gestural transformers detecting multitouch pinches, enabling zooming up to 4.0x and scrolling translations dynamically.
- **Complexity**: Part 4

## Recommended Reading
- [Flutter Docs](https://docs.flutter.dev/)
