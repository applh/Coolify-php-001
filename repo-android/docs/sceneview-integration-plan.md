# Sceneview Integration Plan: 3D Android Applets & GLB Upgrades

This engineering plan details the evaluation and architectural implementation strategy to integrate **Sceneview** into the `repo-android` codebase. We compare Sceneview with LibGDX for mobile integration, design a pristine Jetpack Compose render loop, outline asset management strategies for GLB files, and suggest enhancements to existing/new applets.

---

## ⚖️ 1. Technical Evaluation: Sceneview vs. LibGDX for Compose Applets

When building 3D layouts natively within an Android wrapper designed using modern **Jetpack Compose**, choosing the right 3D graphics rendering engine is critical for battery longevity, memory stability, and code clarity.

### Comparative Decision Matrix

| Dimension | Sceneview (Recommended) | LibGDX (Alternative) |
| :--- | :--- | :--- |
| **PBR Quality** | 🌟 **Elite**: Engineered atop Google's **Filament**, a physically-based rendering engine matching industrial CAD standards. | ⚠️ **Basic**: Relies on legacy fixed-function or custom vertex-fragment shaders. Requires manual writing of normal mapping and shadows math. |
| **Compose Integration**| 🌟 **Excellent**: Out-of-the-box support for declarative Jetpack Compose wrappers (`io.github.sceneview.SceneView`). Fits perfectly inside standard UI trees. | ⚠️ **Awkward**: Forces wrapping a full native application listener inside a custom `AndroidView`. Game lifecycle conflicts with Compose compositions. |
| **GLB Format Support** | 🌟 **Seamless**: Built-in, native glTF 2.0 and GLB loaders. Automatically unpacks hierarchy nodes, maps embedded WebP/PNG textures, and links skeleton rigs. | ⚠️ **Complex**: Requires manual extensions (like `libgdx-gltf`) to parse GLB containers, decode buffers on the CPU, and map assets to core libraries. |
| **Joint Animation** | 🌟 **Hardware Accelerated**: Performs Linear Blend Skinning (LBS) on vertex weights natively on the GPU during rasterization. | ⚠️ **CPU Bound**: Often transforms skeletal matrices on secondary CPU threads, causing runtime slowdowns under dense polygonal setups. |
| **Memory Footprint** | 🌟 **Optimized**: Shares textures and renders natively. Uses mapped native memory limits to keep the JVM garbage collector clear. | ⚠️ **Heavy**: Deploys a bulky engine instance containing game loops, audio, inputs, and asset management frameworks. |

### Final Architecture Recommendation

For the **Moria Roguelike (RuguelikeApplet)**, **World Applet**, and **Blackjack Applet**, **Sceneview emerges as the absolute winner**. It maintains high design compliance, guarantees seamless integration inside our nested Compose menus, and renders breathtaking PBR materials with a minimal performance footprint. 

*LibGDX should only be preferred if compiling a standalone, immersive pure-game environment requiring native 2D/3D physics engines (like Box2D or Bullet) and running entirely outside Android's standard UI layout guidelines.*

---

## 🛠️ 2. Step-by-Step Sceneview Gradle Integration

### Step A: Update SDK limits in `/repo-android/app/build.gradle`
Sceneview requires modern graphics APIs and works best on **minSdk 24** (Android 7.0 and above) to support Vulkan and optimized OpenGL pipelines:

```groovy
android {
    defaultConfig {
        // Required for Sceneview Filament engine compatibility
        minSdk 24
        targetSdk 34
    }
}
```

### Step B: Append Maven & Sceneview Dependencies
Inject Sceneview core and GLB animation dependencies in `app/build.gradle`:

```groovy
dependencies {
    // Sceneview Jetpack Compose and 3D Asset Loader Dependencies
    def sceneview_version = "1.2.3" // Target stable release
    implementation "io.github.sceneview:arsceneview:$sceneview_version" // For standard AR components
    implementation "io.github.sceneview:sceneview:$sceneview_version"   // Non-AR 3D engine context
}
```

---

## 🧩 3. Jetpack Compose Render Loop Blueprint

The following Kotlin snippet demonstrates the exact architecture for compiling a declarative **3D Model Preview Component** inside Jetpack Compose:

```kotlin
package com.example.cameraxapp.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation

@Composable
fun GlbModelCanvas(
    assetPath: String, // e.g., "models/warrior_mesh.glb"
    autoRotate: Boolean = true,
    animationClipIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Set up the scene view with memory-cached properties and dynamic parameters
    SceneView(
        modifier = modifier.fillMaxSize(),
        childNodes = remember(assetPath) {
            val modelNode = ModelNode(
                context = context,
                glbFileLocation = assetPath,
                autoAnimate = true,
                scaleToUnits = 1.0f,
                centerOrigin = Position(0.0f, 0.0f, 0.0f)
            ).apply {
                // Initialize default rotation and position
                position = Position(y = -0.5f, z = -1.5f)
                rotation = Rotation(y = 180f)
                
                // Play skeletal skeletal keyframes
                playAnimation(animationClipIndex)
            }
            listOf(modelNode)
        },
        onFrame = { frame ->
            if (autoRotate) {
                // Safely update transformation matrices tick by tick
                childNodes.forEach { node ->
                    if (node is ModelNode) {
                        node.rotation = Rotation(
                            x = node.rotation.x,
                            y = node.rotation.y + 0.5f, // Continuous yaw rotation
                            z = node.rotation.z
                        )
                    }
                }
            }
        }
    )
}
```

---

## ⚡ 4. GLB Upgrades for Existing Android Applets

Integrating Sceneview transforms our existing custom drawings into a console-grade 3D dashboard:

```
                      📊 APPLET 3D UPGRADE PIPELINE
                      
    [Existing Applet]            [Aesthetic Upgrade with Sceneview]
   ────────────────────────────────────────────────────────────────────
    Moria Roguelike      ───►  • Replace pixel tiles with low-poly GLB meshes
                                • Track character attacks using skeletal animations
                                
    World Applet         ───►  • Load spherical, PBR textured Earth meshes
                                • Bind live markers dynamically in 3D space
                                
    Blackjack Applet     ───►  • Animate 3D physical card flips using Slerp math
                                • Build soft shadows and reflection highlight paths
```

### A. Moria Roguelike (`RoguelikeScreen`)
* **Mesh Swap**: Standard 2D blocks are replaced by 3D environmental components. Walls, doors, floors, and treasure chests are loaded as separate, static GLB files from assets.
* **Animated Hero & Enemy Models**: Replace static player points with beautiful, low-poly animated character meshes (e.g., knight models or slime monsters).
* **Bone State Interpolation**: Play skeletal animations based on current character stats:
  * IDLE: default loop
  * WALKING: synchronized leg swings
  * ATTACKING: high-speed linear mesh sweep transitions

### B. World Globe Applet (`WorldScreen`)
* **Hi-Res Earth Globe**: Load a high-fidelity 3D sphere GLB mapped with physical displacement, diffuse, normal, and specular maps to render realistic oceans, clouds, and landmass shadows.
* **Dynamic Node Pinning**: Programmatically calculate spherical coordinates $(r, \theta, \phi)$ from latitude and longitude and place interactive 3D indicators or floating placards pointing directly to locations.
* **Camera Orbiting Paths**: Implement physical camera drag gestures using Sceneview coordinates, giving users realistic momentum controls when spinning the planet.

### C. Blackjack Applet (`BlackjackScreen`)
* **Physical Poker Table**: Swap flat tables with a fully textured 3D casino layout complete with velvet patterns, chip boxes, and high-fidelity lighting setups.
* **Skeletal Card Flips**: Run custom keyframe sequences representing realistic card dealing motions, sliding pathways across the table, and real-time vertical flips.
* **3D Chips Placement**: Render stacks of realistic, dynamic 3D chips casting physical soft shadows onto the felt table based on input bets.

---

## 🌟 5. Proposing New Useful Applets Built with Sceneview

With Sceneview integrated into our dependencies stack, we can introduce two highly useful, distinct 3D experiences:

### Option A: 3D Rubik's Cube & Spatial Puzzle Applet (`RubiksApplet`)
* **Interactive Geometry**: Renders a dynamic $3\times3$ Rubik's Cube inside standard layouts.
* **Complex Gestural Interlock**: Users tap custom outer blocks or use drag vectors to rotate standard cube faces ($R, L, U, D, F, B$) in real time.
* **Slerp Rotational Physics**: All slice movement sequences use custom matrix rotations and smooth spherical animations to snap layers into place accurately.
* **Algorithmic Solver Integration**: Connect standard solving formulas to visual button panels, executing continuous movement cycles step-by-step to show solutions.

### Option B: 3D Physics Sandbox / Base Builder (`SandboxApplet`)
* **Modular Block Placement**: A 3D grid environment letting users select various building blocks (steel lattices, wood blocks, engines, wheels) and tap the grid to place them.
* **Dynamic Gravity Simulator**: Trigger rigid-body gravity calculations. Observe simulated object collisions, collapses, or dynamic rotations inside the scene.
* **PBR Structural Heatmaps**: Track pressure distributions on steel beams and update shader emissive colors to show stress points or load distributions in real time.

---

## 📝 6. Implementation Milestones

```
  Milestone 1: Dependency Calibration
    └─ Configure minSdk 24, load Sceneview dependencies, and align Java 17 properties.
  Milestone 2: Asset Structuring
    └─ Package low-poly, optimized .glb assets inside 'app/src/main/assets/models/'.
  Milestone 3: 3D Globe Implementation
    └─ Reconstruct WorldScreen using Earth.glb, placing spherical vector markers.
  Milestone 4: Dungeons & Monsters Sandbox
    └─ Swap Moria tiles with environment meshes, binding hero walking and chest opens.
  Milestone 5: Physics Solver (Rubik's)
    └─ Build the Rubik's puzzle structure, handling multi-index slice animations.
```
