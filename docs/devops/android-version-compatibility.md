# Android Gradle Version Compatibility & Troubleshooting Guide

This guide explains how to identify, prevent, and resolve common build errors in the `repo-android` companion app that are caused by compiler plugin, build toolchain, and dependency version mismatches. It specifically addresses alignment between **Kotlin**, **Gradle**, **Android Gradle Plugin (AGP)**, **KSP (Kotlin Symbol Processing)**, and **SceneView (Filament)**.

---

## 1. Deconstructing Common Compilation Errors

### Error Type 1: Metadata Incompatibility (The Metadata Gap)
During compilation (at tasks like `:app:kspDebugKotlin` or `:app:compileDebugKotlin`), you may see:
```text
e: file://.../jetified-sceneview-4.17.0-api.jar!/META-INF/sceneview_release.kotlin_module Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.3.0, expected version is 2.1.0.
e: file://.../jetified-kotlin-stdlib-2.3.21.jar!/META-INF/kotlin-stdlib.kotlin_module Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.3.0, expected version is 2.1.0.
```

#### Why This Happens
Kotlin libraries distribute serialized binary metadata (`.kotlin_module` files) inside their compiled directories to expose types to downstream modules.
* **The expectation gap:** The local compiler's active Kotlin plugin version is legacy/locked (e.g., `2.1.20`), which supports reading up to metadata version `2.1.0`.
* **The library mismatch:** The dependency (such as `sceneview:4.17.0` or a transient modern library like `kotlin-stdlib-2.3.21`) was compiled using a newer Kotlin compiler (e.g., `2.3.x`), which exports metadata with major version `2.3.0`.
* **The result:** The local compiler halts the build because it cannot parse type declarations in the newer deserialized module headers.

---

### Error Type 2: NoSuchMethodError (The AGP / KSP Incompatibility)
During top-level project evaluation or build startup:
```text
* What went wrong:
An exception occurred applying plugin request [id: 'com.google.devtools.ksp']
> java.lang.NoSuchMethodError: 'void com.android.build.api.variant.AndroidComponentsExtension.addKspConfigurations(boolean)'
  at com.google.devtools.ksp.gradle.KspConfigurations$3$1.execute(KspConfigurations.kt:114)
```

#### Why This Happens
The KSP Gradle plugin works closely with the Android Gradle Plugin (AGP) API. Internally, KSP implements support for AGP Variant Extension APIs to register and attach annotation processing directory sources.
* **The mismatched call:** Newer versions of the KSP Gradle plugin (such as KSP series `2.2.x` and `2.3.x`) call optimized internal helper configurations like `addKspConfigurations(boolean)` on AGP's `AndroidComponentsExtension` interface.
* **The missing method:** Legacy versions of AGP (such as AGP `8.6.0` or older) do not expose this method.
* **The result:** Class loading fails at runtime, throwing a `NoSuchMethodError` as the JVM tries to resolve the method reference, instantly failing the Gradle sync or task initialization phase.

---

## 2. Definitive Version Coexistence & Dependency Matrix

The table below outlines verified, stable version combinations that avoid both metadata errors and class-loading/`NoSuchMethodError` crashes.

| Compatibility Profile | Kotlin Version (`org.jetbrains.kotlin.android`) | Kotlin Compose Compiler | KSP Gradle Plugin (`com.google.devtools.ksp`) | Android Gradle Plugin (AGP) | Minimum Gradle Wrapper Version | Target JDK Version | SceneView (Filament) Support Range | Android SDK `compileSdk` / `targetSdk` |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Profile A (Modern Stable - Recommended)** | `2.1.20` | `2.1.20` (Inline) | `2.1.20-1.0.32` | `8.6.0` | `8.7` | `Java 17` | `io.github.sceneview:sceneview:4.15.0` | `35` |
| **Profile B (Ultra-Legacy Recovery)** | `2.0.21` | `2.0.21` (Inline) | `2.0.21-1.0.28` | `8.4.2` | `8.5` | `Java 17` | `io.github.sceneview:sceneview:4.11.2` | `34` |
| **Profile C (Advanced Beta/Future)** | `2.3.21` | `2.3.21` (Inline) | `2.3.21-2.0.x` | `>= 8.8.0-beta01` | `8.9` | `Java 17` | `io.github.sceneview:sceneview:4.17.0` | `35` |

> [!WARNING]
> **Using Profile A is highly recommended** because it perfectly aligns with Android Gradle Plugin (AGP) `8.6.0` and preserves modern Kotlin language features while avoiding bytecode compatibility crashes with SceneView and Room structures.

---

## 3. Step-by-Step Transition / Alignment Guides

### Aligning Your Project to Profile A (Stable & Functional)

#### Step 1: Configure Top-Level Plugins (`repo-android/build.gradle`)
Ensure your parent `build.gradle` defines matching, strictly bounded versions for the plugins. Avoid using dynamic wildcards (`+` or `latest`):

```groovy
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id 'com.android.application' version '8.6.0' apply false
    id 'com.android.library' version '8.6.0' apply false
    id 'org.jetbrains.kotlin.android' version '2.1.20' apply false
    id 'org.jetbrains.kotlin.plugin.compose' version '2.1.20' apply false
    id 'com.google.devtools.ksp' version '2.1.20-1.0.32' apply false
}
```

#### Step 2: Configure Module Configuration (`repo-android/app/build.gradle`)
Apply the matching plugins, compiler configurations, forcing targets to JDK 17 bytecode, and declare matching SceneView variants:

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'org.jetbrains.kotlin.plugin.compose'
    id 'com.google.devtools.ksp'
}

android {
    namespace 'com.example.cameraxapp'
    compileSdk 35

    defaultConfig {
        applicationId "com.example.cameraxapp"
        minSdk 24
        targetSdk 35
        versionCode 2
        versionName "1.1"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
    }
    buildFeatures {
        compose true
        buildConfig true
    }
}

dependencies {
    // 3D Visual Rendering aligned with Kotlin 2.1.x metadata
    def sceneview_version = "4.15.0"
    implementation "io.github.sceneview:arsceneview:$sceneview_version"
    implementation "io.github.sceneview:sceneview:$sceneview_version"

    // Room Database processing (Processed by compiler matched with Kotlin 2.1.20 KSP)
    def room_version = "2.6.1"
    implementation "androidx.room:room-runtime:$room_version"
    implementation "androidx.room:room-ktx:$room_version"
    ksp "androidx.room:room-compiler:$room_version"
}
```

#### Step 3: Handle Gradle Wrapper Alignment (`repo-android/gradle/wrapper/gradle-wrapper.properties`)
Verify that Gradle 8.7 is declared as the execution layer. Ensure the distribution URL matches:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

---

## 4. Resetting and Purging Corrupted Daemons & Cache Transformed Metadata

Sometimes Gradle caches compiler output and transformed dependencies. Even after editing build files, you may continue to see stale compile failures unless you perform a hard reset. 

Run these commands in sequence inside the root repository directory `/repo-android`:

```bash
# 1. Stop any persistent Gradle daemon instances lingering in the system memory
./gradlew --stop

# 2. Tell Gradle to clean build output and delete incremental task graphs
./gradlew clean

# 3. Clear transformed dependency configurations selectively to force re-jetification
rm -rf ~/.gradle/caches/transforms-4/
rm -rf ~/.gradle/caches/journal-1/

# 4. Clear Gradle metadata lock files
find ~/.gradle/caches/ -name "*.lock" -type f -delete 2>/dev/null || true

# 5. Compile cleanly with fresh resolution
./gradlew assembleDebug
```

---

## 5. Defensive Architectural Rules for Android Projects

1. **Rule of Kotlin-KSP Version Correspondence:**
   KSP runs on top of the physical Kotlin compiler. Standard KSP version names are structured as `[Kotlin_Version]-[KSP_Engine_Release]`. For example:
   * Correct: Kotlin `2.1.20` paired with `2.1.20-1.0.32`.
   * Incorrect: Kotlin `2.1.20` paired with `2.0.21-1.0.28` (Compilation fails with language syntax errors or compiler diagnostic exceptions).

2. **Compose Compiler is Built-In:**
   From Kotlin `2.0.0` onwards, the Jetpack Compose compiler is delivered *inside* the main Kotlin plugin. You no longer need to configure separate Compose compiler blocks with distinct versions. Just apply the top-level `org.jetbrains.kotlin.plugin.compose` plugin matching the main Kotlin plugin version.

3. **Prevent Transitive Version Shadowing:**
   Some remote libraries pull in `kotlin-stdlib` compiled on newer compiler versions (such as pulling in `kotlin-stdlib-2.3.21` transitively). To prevent these files from introducing newer incompatible metadata versioning, specify a strict resolution model inside your app module's `android` block:
   ```groovy
   android {
       configurations.all {
           resolutionStrategy {
               eachDependency { DependencyResolveDetails details ->
                   if (details.requested.group == 'org.jetbrains.kotlin') {
                       // Lock all transitive stdlib compilation utilities to active compiler level
                       details.useVersion '2.1.20'
                   }
               }
           }
       }
   }
   ```
4. **SceneView (Filament) Release Bounds:**
   SceneView `4.16.x` and `4.17.x` are built against Kotlin releases with modern `2.3.0` metadata outputs. Avoid utilizing dynamic boundaries (e.g. `4.+`) to keep minor releases from introducing breaks into your continuous builds automatically.
