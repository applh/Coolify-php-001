# Android Gradle Version Compatibility & Troubleshooting Guide

This guide explains how to identify, prevent, and resolve common build errors in the `repo-android` companion app that are caused by compiler plugin and dependency version mismatches. It specifically addresses alignment between **Kotlin**, **Gradle**, **KSP (Kotlin Symbol Processing)**, and **SceneView (Filament)**.

---

## 1. Deconstructing the Metadata Incompatibility Error

During the compilation step, typically at the KSP or Kotlin code generation task (`:app:kspDebugKotlin` or `:app:compileDebugKotlin`), you may encounter the following build failure:

```text
e: file://.../jetified-sceneview-4.17.0-api.jar!/META-INF/sceneview_release.kotlin_module Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.3.0, expected version is 2.1.0.
e: file://.../jetified-kotlin-stdlib-2.3.21.jar!/META-INF/kotlin-stdlib.kotlin_module Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.3.0, expected version is 2.1.0.
```

### Why This Happens
Kotlin libraries distribute serialized binary metadata (`.kotlin_module` files) inside their jars. This metadata exposes compiled types, functions, and parameters to other Kotlin modules.
* **The expectation gap:** The local compiler's active Kotlin plugin Version is `2.1.20`, which understands up to metadata version `2.1.0`.
* **The library mismatch:** The dependency (such as `sceneview:4.17.0` or a modern `kotlin-stdlib` transient dependency) was compiled with Kotlin compiler version `2.3.x`, generating metadata version `2.3.0`.
* **The result:** The local compiler version `2.1` compiler fails to load and digest the newer `2.3` metadata, halting the build.

---

## 2. Solution Paths

There are two primary ways to resolve Kotlin module metadata mismatches. 

### Solution A: Modern Upgrade (Recommended Future-Proof Method)
To use modern SceneView (`4.16.x` / `4.17.x`) and get full JVM/Compose optimizations, upgrade the root project's Kotlin compiler and relative annotation processors to match the target metadata level.

| Component | Target Version | Description |
| :--- | :--- | :--- |
| **Kotlin Compiler / Plugin** | `2.3.21` | Supports compiling and reading `2.3.0` metadata structures. |
| **Kotlin Compose Compiler** | `2.3.21` | Directly paired to the compiler from Kotlin 2.x onwards. |
| **KSP Gradle Plugin** | `2.3.9` | Supports annotation processing for Room and Retrofit. |
| **SceneView (3D Engine)** | `4.17.0` or `4.16.10` | Embedded 3D/AR engine compiled with Kotlin 2.3. |
| **Android Gradle Plugin** | `8.6.0` | Root build plugin alignment. |

### Solution B: Downstream Downgrade (Legacy Maintenance Method)
If the project's Kotlin compiler must remain locked at `2.1.20` or earlier, you must downgrade SceneView and related tools to versions compiled with older metadata profiles.

| Component | Target Version | Description |
| :--- | :--- | :--- |
| **Kotlin Compiler / Plugin** | `2.1.20` | Native locked compiler profile. |
| **Kotlin Compose Compiler** | `2.1.20` | Integrated compiler profile. |
| **KSP Gradle Plugin** | `2.1.20-1.0.32` | Paired KSP processor version. |
| **SceneView (3D Engine)** | `<= 4.11.2` | Old variants compiled with older Kotlin structures. |

---

## 3. Concrete Configuration Verification

### Root Configuration (`repo-android/build.gradle`)
Ensure plugins are declared in your top-level build script utilizing the **Modern Upgrade** pattern, ensuring proper class loading alignments:

```groovy
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id 'com.android.application' version '8.6.0' apply false
    id 'com.android.library' version '8.6.0' apply false
    id 'org.jetbrains.kotlin.android' version '2.3.21' apply false
    id 'org.jetbrains.kotlin.plugin.compose' version '2.3.21' apply false
    id 'com.google.devtools.ksp' version '2.3.9' apply false
}
```

### App Configuration (`repo-android/app/build.gradle`)
Apply the matching plugins, compile variables, and ensure KSP processes files like Room database compilers correctly:

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'org.jetbrains.kotlin.plugin.compose'
    id 'com.google.devtools.ksp'
}

android {
    compileSdk 35
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
    }
    // ...
}

dependencies {
    // 3D Visual Assets Rendering
    def sceneview_version = "4.17.0"
    implementation "io.github.sceneview:arsceneview:$sceneview_version"
    implementation "io.github.sceneview:sceneview:$sceneview_version"

    // Room DB processing through active KSP compiler
    def room_version = "2.6.1"
    implementation "androidx.room:room-runtime:$room_version"
    implementation "androidx.room:room-ktx:$room_version"
    ksp "androidx.room:room-compiler:$room_version"
}
```

---

## 4. Cleaning Corrupted Gradle Caches

In some environments, even after changing plugin versions, Gradle's daemon may cache parsed/jetified metadata. Follow these terminal steps to reset local caches cleanly:

```bash
cd repo-android

# 1. Kill any active Gradle daemons in memory
./gradlew --stop

# 2. Command clean project structures
./gradlew clean

# 3. Clear transformed dependency caches selectively if errors persist
rm -rf ~/.gradle/caches/transforms-4/
rm -rf ~/.gradle/caches/journal-1/

# 4. Compile cleanly from scratch
./gradlew assembleDebug
```

---

## 5. Architectural Recommendations

1. **Avoid "+" Version Constraints:** Never utilize declarations like `implementation "io.github.sceneview:sceneview:4.+"` inside dependencies block, as minor revisions can dynamically bump the minimum required Kotlin compiler versions, breaking cloud CI compiles.
2. **KSP strictly coupled to Compiler:** For every Kotlin minor version modification (e.g. going from `2.1` to `2.3`), you **must** lookup and update the KSP version in GPP (Gradle Plugin Portal) or Maven Central. KSP will decline to run if its minor build variant does not match the active Kotlin compiler minor variant.
3. **Java Version Bounds:** Kotlin 2.x and SceneView/Filament 3D compilers generally require targeting Java 17 structures. Ensure `jvmTarget` and `JavaVersion` are aligned to `17` within build options profiles.
