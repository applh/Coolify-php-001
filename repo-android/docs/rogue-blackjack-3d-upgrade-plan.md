# 🍓 FRAISE Android Games: 3D Engine Upgrade Implementation Plan

This document details the architectural blueprints, mathematical projection equations, and Jetpack Compose canvas integration strategies to upgrade **RogueCompose (Rogue-like RPG)** and **Blackjack Pro** using our standard, high-performance **Sovereign 3D Compose Engine**. 

By reusing the perspective projections, rotational coordinate matrices, backface culling normal computations, and depth-sorting Painter's Algorithm from our 3D Vector Workspace (`ThreeDWorkspaceScreen.kt`), we will upgrade both games into elegant, spatial experiences with 0% external library dependencies.

---

## 1. Unified 3D Architectural Topography

To maintain instant app cold-starts, lightweight APK footprints, and absolute memory safety, both games will bypass heavy native engines (OpenGL ES, Vulkan, Filament) and render physical 3D elements entirely within standard Jetpack Compose custom drawing canals (`Canvas`). 

### Core 3D Rendering Pipeline Integration
```
 ┌────────────────────────────────────────────────────────┐
 │   Game State Storage (SQLite / StateFlow)             │
 └──────────────────────────┬─────────────────────────────┘
                            │ OnStateChanged (Coordinates, Actions)
                            ▼
 ┌────────────────────────────────────────────────────────┐
 │   Coordinate Translation & Mesh Reconstruction         │
 │   - Map 2D Grid / Table Slots -> 3D Vector coordinates   │
 └──────────────────────────┬─────────────────────────────┘
                            │ Vertex List {x, y, z}
                            ▼
 ┌────────────────────────────────────────────────────────┐
 │   Sovereign 3D Engine Math Processing                 │
 │   - Pitch/Yaw/Roll Rotational Transformations          │
 │   - focal length (FL) perspective projections          │
 │   - Face normal cross product calculation              │
 │   - Lambertian Diffuse & Phong Specular lighting       │
 └──────────────────────────┬─────────────────────────────┘
                            │ Transformed & Shaded Polygons
                            ▼
 ┌────────────────────────────────────────────────────────┐
 │   Depth Sorting Pipeline (Painter's Algorithm)         │
 │   - Sort descending by average polygon depth Z_avg     │
 │   - Backface Culling (Exclude Z_normal > 0 faces)      │
 └──────────────────────────┬─────────────────────────────┘
                            │ Sorted Primitive Lists
                            ▼
 ┌────────────────────────────────────────────────────────┐
 │   Jetpack Compose DrawScope Canvas Canvas             │
 │   - drawPath / drawTexturedTriangle                   │
 │   - Inline BitmapShaders & Clipping masks              │
 └────────────────────────────────────────────────────────┘
```

---

## 2. 3D Rogue-like RPG Engine ("Rogue3D Compose")

Moving the $18 \times 18$ flat procedural ASCII/unicode dungeon grid into a spatial, immersive coordinate environment.

### A. Dynamic Coordinate Map Grid Projection
We translate the flat dungeon coordinates $(u, v) \in [0, 17]^2$ into 3D world space $(\mathbf{X}_w, \mathbf{Y}_w, \mathbf{Z}_w)$.
* **Ground Tiles**: Rendered as flat horizontal polygons lying on the ground plane:
  $$\mathbf{V}_{ground} = \left( u \cdot W_s - C_o, \ v \cdot W_s - C_o, \ 0 \right)$$
  Where $W_s$ represents the 3D wall size scale coefficient (e.g., $40.0\text{f}$) and $C_o$ represents map offset coordinates ($18 \times 40 / 2 = 360\text{f}$) to center the floor.
* **Raised Wall Cubes**: Rendered as full 3D hexahedrons composed of 6 rectangular polygon faces. The bottom vertices of the cube sit at $Z=0$ and the ceiling nodes rise to $Z = -W_s$ (in 3D world coords, a negative Z extends out from the canvas towards the camera).

### B. Isometric Camera Presentation Perspective
To ensure perfect tactical visibility and layout clarity, the game utilizes a single high-performance presentation perspective:
* **Elevated Orthographic Isometric Camera**:
  * The camera is positioned at an elevated orthographic angle ($45^\circ$ Pitch, $35^\circ$ Yaw).
  * This angle allows rapid spatial recognition of the surrounding coordinate rooms, hallway intersections, and dynamic entities without perspective distortion or blind-spots typical of first-person viewports.

### C. Low-Poly Entity Meshes
Instead of flat flat tiles, players and beasts are projected as distinct 3D volumetric models:
* **The Hero (Player)**: A 3D dual-tiered cylinder structure or an extruded prism with an orange glowing outline.
* **Goblins / Rats**: Low-poly red octahedron prisms (8 faces) that pulsate slightly during idle turns using temporal sine waves ($\sin(\text{time})$).
* **Necromancer / Magic Lich**: Deep violet hollow icosahedrons (20 faces) that float above the ground level.
* **Chests**: A split 3D bounding box layout where the lid mesh rotates upwards by angle $\phi = 60^\circ$ around its back edge axis upon state verification (chest opened in SQLite).
* **Stairs Up/Down**: Layered 3D descending slab steps that run sequentially down in coordinate steps.

### D. 3D Moving Area Light & Fog of War
Rather than flat tiles shrouded in absolute monochrome, we exploit our 3D Lighting pipeline:
* **Player-Centered Point Light**: A spherical light source emanates directly from the player position $(X_p, Y_p, Z_p)$.
* **Distance Attenuation Model**: The light intensity coefficient representing polygon shading scales inversely with distance:
  $$I_{\text{distance}} = \frac{1.0}{1.0 + \gamma \cdot D_e^2}$$
  Where $D_e = \|\mathbf{P}_{poly\_center} - \mathbf{C}_{player}\|$ and $\gamma$ represents our custom falloff attenuation coefficient (e.g. $0.008\text{f}$).
* Polygons representing distant corridors automatically fade into deep shadows (intensity $I_{total} \le 0.05\text{f}$), generating a pristine medieval atmosphere.

---

## 3. 3D Blackjack Pro Casino Table ("Blackjack3D")

Upgrading standard flat felts into a high-octane 3D card layout featuring spatial physics-based movements.

### A. Elliptical 3D Casino Table Surface
We project a curved 3D bounding plane representing the croupier outline:
* Table bounds are formulated as an elliptical arc mesh swept in coordinate space:
  $$x = A \cdot \cos(\varphi), \quad y = B \cdot \sin(\varphi), \quad z = e \cdot \sin^2(\varphi)$$
  *Where $A=300\text{f}$ and $B=220\text{f}$ trace the semi-circular croupier curvature.*
* Polygons in the mesh are shaded with a custom **Felt Deep Green** or **High-Roller Burgundy** gradient that reacts dynamically to ambient studio spotlights.
* Gentle **Programmatic Table Sway**: To inject dynamic life into the table's spatial feeling without sensor overhead, we implement a lightweight mathematics sway driven by standard Jetpack Compose endless animation timers rather than physical gyro sensors. This guarantees 100% platform compatibility (working flawlessly on tablets and virtual machinery lacking hardware gyroscopes) with absolute 0% extra battery drain.

### B. Spatial Card Flipping & Parabolic Dealing Physics
Cards are represented as thin, flat 3D polygonal quads consisting of 2 triangles, having front and back texture components.

1. **The Parabolic Deal Path**:
   * Dealt cards originate from the coordinate offset of the 3D card shoe:
     $$\mathbf{S}_{shoe} = (200, -180, -120)$$
   * Cards travel along a 3D Bezier curve path to their target slot position on the felt ($X_t, Y_t, Z_t$):
     $$\mathbf{P}(t) = (1-t)^2 \mathbf{S}_{shoe} + 2(1-t)t \mathbf{P}_{mid} + t^2 \mathbf{P}_{target}$$
     *Where $\mathbf{P}_{mid}$ lifts the card upwards in the coordinate plane to simulate air-resistance.*
2. **The 3D Flip Anim**:
   * While traveling, cards execute a $180^\circ$ rotation around their longitudinal axis (Y-axis rotation):
     $$\theta_{Roll}(t) = 180^\circ \cdot (1 - t)$$
   * Evaluated inside our 3D projection model, cards turn from their red/blue patterned back face to expose their numerical suits seamlessly.

### C. 3D Stackable Cylinder Chips
Bets are represented as actual cylinder entities constructed dynamically:
* A 3D cylinder consists of a circle cap, a circle base, and 12-16 side rectangle strips.
* Values in bets stack dynamically along the vertical axis (Z-axis):
  * A $\$25$ bet stacks five $\$5$ cylinders.
  * Adding another chip pushes the target cylinder vertical coordinate:
    $Z_{\text{base\_next}} = Z_{\text{base}} - (N_{\text{chips}} \cdot T_{\text{chip}})$ (where $T_{\text{chip}} \approx 8.0\text{f}$ is the cylinder thickness).
* Dynamic Phong specular equations create a shiny plastic sheen looking highlight over the chip rims.

---

## 4. Shading & Texture Pipeline Configurations

To dress our polygon geometries with high-fidelity files:

```kotlin
// Theoretical Shader Translation mapping to our Compose Canvas pipeline:

fun ApplyIlluminationFilter(
    poly: RenderItem3D.Polygon,
    cameraViewDir: Vector3,
    scene: SceneOptions
): Color {
    // 1. Calculate Face Unit Normal Vector via cross-product
    val v0 = poly.pts[0]
    val v1 = poly.pts[1]
    val v2 = poly.pts[2]
    val edge1 = v1 - v0
    val edge2 = v2 - v0
    val normal = edge1.cross(edge2).normalize()
    
    // Backface Culling check
    if (scene.backfaceCulling && normal.dot(cameraViewDir) > 0f) {
        return Color.Transparent // Culled out immediately
    }
    
    // 2. Compute Incident light Vector dot-product (Lambertian Shading)
    val diffuseDot = maxOf(0f, normal.dot(scene.lightDirection.normalize()))
    val diffuse = diffuseDot * scene.diffuseIntensity
    
    // 3. Compute View specular reflex reflection (Phong Highlight)
    // R = 2 * (N . L) * N - L
    val reflectDir = (normal * (2f * normal.dot(scene.lightDirection))) - scene.lightDirection
    val specularFactor = maxOf(0f, reflectDir.dot(cameraViewDir.normalize()))
    val specular = if (diffuseDot > 0f) {
        pow(specularFactor.toDouble(), scene.shininess.toDouble()).toFloat() * scene.specularIntensity
    } else 0f
    
    // Combine light attributes scaled against base color channels to output target shaded color
    val r = ((poly.baseColor.red * (scene.ambientIntensity + diffuse)) + specular).coerceIn(0f, 1f)
    val g = ((poly.baseColor.green * (scene.ambientIntensity + diffuse)) + specular).coerceIn(0f, 1f)
    val b = ((poly.baseColor.blue * (scene.ambientIntensity + diffuse)) + specular).coerceIn(0f, 1f)
    
    return Color(red = r, green = g, blue = b, alpha = poly.baseColor.alpha)
}
```

---

## 5. Technical Mappings: Data Structures & Model Upgrades

### RogueCompose Model Extensions (`RoguelikeViewModel.kt`)
We add a core rotation state to support smooth orbital panning inside our view loops:
```kotlin
data class Rogue3DState(
    val yawAngle: Float = 45f, // Programmatic orbit yaw angle
    val pitchAngle: Float = 35f, // Fixed isometric pitch elevation
    val zoomFactor: Float = 1.0f,
    val activeAnimationsList: List<DynamicEntity3D> = emptyList()
)

data class DynamicEntity3D(
    val id: String,
    val startCoords: Vector3,
    val currentCoords: Vector3,
    val targetCoords: Vector3,
    val progress: Float = 0f,
    val meshColor: Color
)
```

### Blackjack Pro Model Extensions (`BlackjackViewModel.kt`)
Each playing card receives tracking vectors to enable coordinate translations:
```kotlin
data class Card3DState(
    val id: String,
    val currentPosition: Vector3,
    val baseRotation: Vector3, // rotation around roll/pitch/yaw angles
    val travelProgress: Float = 1.0f, // 0=In Shoe, 1=On Table Slot
    val scaleFactor: Float = 1.0f
)
```

---

## 6. Phased Rollout Roadmap

### Phase 1: Shared Mathematical Vector Foundations
* Standardize `Vector3` properties, `getFaceNormal` multipliers, and matrix multipliers into a common coordinate package (`com.example.cameraxapp.core.math3d`).
* Verify coordinate mappings with simple local unit tests to ensure rotation results math outputs perfectly.

### Phase 2: Isometric 3D Grid Panning for RogueCompose
* Transition existing flat tile grid loop in `RoguelikeScreen.kt` to call our custom 3D drawing pipeline inside the Canvas draw scope.
* Convert wall coordinates to 3D rectangular box polygons and bind touch drags to orbit pitch and yaw coordinate directions dynamically.

### Phase 3: Spatial Felt Layout & Dealing Parabolics for Blackjack
* Replace green flat screen components with the curved perspective semi-circular blackjack felt.
* Write the parabolic interpolation animations inside `BlackjackViewModel.kt` to trigger dealing offsets, and implement Card 3D rotation rolls on tick updates.

### Phase 4: Dynamic Lighting Enhancements & Culling Optimizations
* Enable backface culling to prune polygons facing away from the camera, doubling FPS drawing execution speeds.
* Set ambient and specular light properties under customizable sliders, letting users explore material behaviors dynamically!
