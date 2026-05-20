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

## Recommended Reading
- [Flutter Docs](https://docs.flutter.dev/)
