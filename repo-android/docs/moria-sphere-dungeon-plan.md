# 🪐 Moria Roguelike: Spherical Dungeon & Portal Teleportation Architecture Plan

This document presents the formal technical analysis, architectural design, mathematical models, and implementation blueprint to upgrade the 3D Moria applet into a **Spherical Dungeon Roguelike**. 

Instead of generating a flat level map on a 2D plane, Moria will project the floor grids, walls, players, and monsters onto the three-dimensional surface of a sphere. Stairwells will be replaced by high-fidelity 3D portals that trigger seamless warp transitions to separate spheres representing deeper, distinct dungeon domains.

---

## 1. Architectural Strategy & Core Concepts

To stay aligned with our architectural core of a lightweight, highly responsive, zero-dependency APK that runs seamlessly in the AI Studio preview window, we continue to utilize the CPU-bound **Sovereign 3D Projection Engine** on top of Jetpack Compose Canvas.

### Flat coordinates vs. Spherical Geometry
In standard 2D flat dungeons, tiles are rendered linearly:
$$\mathbf{P}_{\text{flat}} = \left( (x - c_x) \cdot W_s, \ 0, \ (y - c_y) \cdot W_s \right)$$

On a spherical ground of radius $R$:
1. **The Ground Surface**: Ground tiles are constructed as 3D curved polygons resting precisely on a spherical shell of radius $R$.
2. **The Wall Heights**: Wall elements are extruded radially outwards from radius $R$ to $R + H_{\text{wall}}$.
3. **Finite But Unbounded Navigation**:东/西 (East/West) and 南/北 (South/North) movement wrap seamlessly, turning the dungeon into a finite, closed spherical planetoid.
4. **Interactive Portals**: Multicolored rotating, glowing 3D rings that act as gravity wells. Colliding with a portal teleports the player's coordinate frame onto a brand-new sphere (the next floor) with distinct geometry, enemy rosters, and lighting variables.

---

## 2. Mathematical Coordinate Mapping Engine

We define two robust mathematical models for wrapping a discrete grid onto a spherical surface. The **Cube-Mapped Sphere (Spherified Cube)** is chosen as the standard for high-performance grid mechanics to avoid polar pinching.

### Option A: Standard Spherical Projection (Polar Coordinates)
For a grid of width $W$ and height $H$, we normalize local coordinates $(x, y)$ into angles $\theta$ (polar latitude) and $\phi$ (azimuthal longitude):

$$\phi = \frac{x}{W} \cdot 2\pi - \pi \quad \in [-\pi, \ \pi]$$
$$\theta = \frac{y}{H} \cdot \pi - \frac{\pi}{2} \quad \in [-\frac{\pi}{2}, \ \frac{\pi}{2}]$$

The 3D point $\mathbf{V}$ at ground level (radius $R$) is:
$$\mathbf{V}(x, y) = \begin{pmatrix}
R \cdot \cos(\theta) \cdot \sin(\phi) \\
R \cdot \sin(\theta) \\
R \cdot \cos(\theta) \cdot \cos(\phi)
\end{pmatrix}$$

* **Pros**: Simple trigonometric transformation.
* **Cons**: Severe polar distortion ("pinching") at the top ($y = 0$) and bottom ($y = H$) where meridians converge. Ground tile polygons shrink to thin triangles.

### Option B: Cube-Mapped Sphere (Spherified Cube) — PREFERRED
To maintain uniform square-grid tiles across the entire sphere with zero structural pinching, we treat the dungeon as a **Cube of 6 Faces** (Front, Back, Left, Right, Top, Bottom). Each face contains a standard $18 \times 18$ local grid coordinate buffer. 

To spherify the cube, we project any point $(x, y, z)$ on the unit cube outwards to the unit sphere, then scale by radius $R$:

$$\mathbf{V}_{\text{sphere}} = R \cdot \frac{\mathbf{V}_{\text{cube}}}{\|\mathbf{V}_{\text{cube}}\|}$$

```
                +--------------+
                |    TOP (5)   |
                +--------------+
+--------------++--------------++--------------++--------------+
|   LEFT (2)   ||  FRONT (1)   ||  RIGHT (3)   ||   BACK (4)   |
+--------------++--------------++--------------++--------------+
                +--------------+
                |  BOTTOM (6)  |
                +--------------+
```

When a player moves off the boundary of local grid $X = 17$ on the **FRONT** face, they transition seamlessly to $X = 0$ on the **RIGHT** face, with their heading vector conserved. 

---

## 3. Extruding 3D Meshes Radially

To render volumetric objects on the spherical surface, we calculate offset vectors pointing **radially outwards** from the sphere center $\mathbf{O} = (0, 0, 0)$.

### A. Spherical Floor Tiles
For a tile $(x, y)$ spanning boundaries $x_1 \to x_2$ and $y_1 \to y_2$:
1. We compute the 4 corner coordinates on the flat plane face.
2. Spherify each corner point by dividing by its magnitude and multiplying by the target sphere radius $R$.
3. Pass the resulting 3D corner vectors to `RenderItem3D.Polygon` to build the curved floor.

### B. Radial Wall Extrusion
Instead of pushing vertical coordinate steps in the flat $Y$-axis, walls are extruded outwards along their normal vector $\hat{\mathbf{n}}$:

$$\hat{\mathbf{n}} = \frac{\mathbf{V}_{\text{ground}}}{\|\mathbf{V}_{\text{ground}}\|}$$
$$\mathbf{V}_{\text{wall\_top}} = \mathbf{V}_{\text{ground}} + H_{\text{wall}} \cdot \hat{\mathbf{n}}$$

This ensures walls tilt outwards perfectly like towers on a small planetoid, creating a beautiful spatial curvature as the player rotates the camera!

```
               Wall Top (Radius R + H)
               +---------+
              /         /
             /   Wall  /
            /         /
           +---------+  Ground Level (Radius R)
           \         \
            \ Sphere  \
             \ Center  \
              +---------+ (0,0,0)
```

---

## 4. Portals: The 3D Warp Gates

Portals replace the traditional flat descent staircase. We implement portals as **Procedural Low-Poly Gyroscopes** consisting of two concentric, counter-rotating rings.

### Portal Mesh Mathematics
For a portal centered at coordinate position $\mathbf{C}_{\text{portal}}$ on the sphere surface:
1. We define the local tangent plane vectors $\mathbf{U}$ and $\mathbf{W}$ which sit perpendicular to the sphere surface normal $\hat{\mathbf{n}}$:
   $$\mathbf{U} \cdot \hat{\mathbf{n}} = 0, \quad \mathbf{W} \cdot \hat{\mathbf{n}} = 0, \quad \mathbf{U} \perp \mathbf{W}$$
2. We generate circular rings using the current dynamic looping angle $\alpha(t)$:
   $$\mathbf{P}_{\text{ring1}, i} = \mathbf{C}_{\text{portal}} + r_{\text{outer}} \cdot \left( \cos(\phi_i + \alpha) \cdot \mathbf{U} + \sin(\phi_i + \alpha) \cdot \mathbf{W} \right)$$
   $$\mathbf{P}_{\text{ring2}, i} = \mathbf{C}_{\text{portal}} + r_{\text{inner}} \cdot \left( \cos(\phi_i - 1.5\alpha) \cdot \mathbf{U} + \sin(\phi_i - 1.5\alpha) \cdot \mathbf{W} \right)$$
3. **Lighting & Glow Indicator**: Vertices of the portal are assigned a self-illuminated glowing lilac or cosmic cyan color (`Color(0xFF00E5FF)`), completely bypassing ambient light attenuation calculations to shine brightly even in pitch-black corridors.

---

## 5. Sphere-to-Sphere Level Transitions

When a player enters a Portal tile and triggers "Interaction" (Warp):
1. **Dynamic Rotation Lock**: The UI freezes touch rotations and executes a 750ms automatic circular warp sweep animation.
2. **State Re-Initialization**:
   * The current sphere resources are cached/saved in SQL.
   * A completely new sphere map is procedural-carved with unique themes:
     * **Sphere 1 (Floor 1)**: Emerald Moss Planetoid (Ambient Green Light).
     * **Sphere 5 (Floor 5)**: Obsidian Lava Core (Radial Crimson Volcanic Shuts).
     * **Sphere 10 (Floor 10 - Boss)**: Shadow Void Sphere (Pitch-black background with a central radiant star lighting the platform).
3. **Warp Particles**: The rendering thread injects 50 floating geometric diamond shards drifting outwards along radial pathways to create an intensive voxel teleportation sequence.

---

## 6. Kotlin Implementation Reference Boilerplate

The following Kotlin code slices demonstrate the spatial projection math and rendering integration to support this model on top of `DungeonCanvas3D`:

```kotlin
package com.example.cameraxapp.roguelike

import androidx.compose.ui.graphics.Color
import com.example.cameraxapp.core.math3d.RenderItem3D
import com.example.cameraxapp.core.math3d.Vector3
import kotlin.math.*

/**
 * Spatial Engine responsible for projecting 2D coordinate cells onto 3D Spherical Coordinate Space.
 */
class SphereDungeonProjection(
    val radius: Float = 100f,
    val wallHeight: Float = 15f
) {
    /**
     * Projects a grid coordinate (local flat plane) to a perfect spherical coordinates vector.
     * Grid bounds map: x in [0, 18], y in [0, 18] representing one face of the planetoid.
     */
    fun projectGridToSphere(gridX: Float, gridY: Float, heightOffset: Float = 0f): Vector3 {
        // Normalize coordinates relative to centered 18x18 limits
        val cx = 9f
        val cy = 9f
        val dx = (gridX - cx) / 18f // range [-0.5, 0.5]
        val dy = (gridY - cy) / 18f // range [-0.5, 0.5]

        // Map using standard azimuthal projection scales
        val longitude = dx * 2f * PI.toFloat() // span radians
        val latitude = dy * PI.toFloat()       // span radians

        // Compute base spherical vector components
        val rSum = radius + heightOffset
        val rx = rSum * cos(latitude) * sin(longitude)
        val ry = rSum * sin(latitude)
        val rz = rSum * cos(latitude) * cos(longitude)

        return Vector3(rx, ry, rz)
    }

    /**
     * Recursively projects flat ground tile polygons onto the spherical shell boundaries.
     */
    fun buildSphericalFloorTile(
        tile: GameTile, 
        tileSize: Float,
        shadingColor: Color
    ): RenderItem3D.Polygon {
        val half = tileSize / 2f
        val x = tile.x.toFloat()
        val y = tile.y.toFloat()

        // Extract 4 corners flat
        val c1 = projectGridToSphere(x - half, y - half)
        val c2 = projectGridToSphere(x + half, y - half)
        val c3 = projectGridToSphere(x + half, y + half)
        val c4 = projectGridToSphere(x - half, y + half)

        return RenderItem3D.Polygon(listOf(c1, c2, c3, c4), shadingColor, depth = 0f)
    }

    /**
     * Builds an outward-facing extruded wall block on the sphere.
     */
    fun buildSphericalWall(
        tile: GameTile,
        tileSize: Float,
        wallColor: Color,
        outPipeline: MutableList<RenderItem3D>
    ) {
        val half = tileSize / 2f
        val x = tile.x.toFloat()
        val y = tile.y.toFloat()

        // 4 ground coordinate vertices
        val g1 = projectGridToSphere(x - half, y - half)
        val g2 = projectGridToSphere(x + half, y - half)
        val g3 = projectGridToSphere(x + half, y + half)
        val g4 = projectGridToSphere(x - half, y + half)

        // 4 corresponding top extruded vertices (projected outwards along ground normals)
        val t1 = projectGridToSphere(x - half, y - half, wallHeight)
        val t2 = projectGridToSphere(x + half, y - half, wallHeight)
        val t3 = projectGridToSphere(x + half, y + half, wallHeight)
        val t4 = projectGridToSphere(x - half, y + half, wallHeight)

        // Generate 6 bounding faces
        outPipeline.add(RenderItem3D.Polygon(listOf(g1, g2, g3, g4), wallColor, depth = 0f)) // Bottom cap
        outPipeline.add(RenderItem3D.Polygon(listOf(t1, t2, t3, t4), wallColor, depth = 0f)) // Top cap
        outPipeline.add(RenderItem3D.Polygon(listOf(g1, g2, t2, t1), wallColor, depth = 0f)) // Side 1
        outPipeline.add(RenderItem3D.Polygon(listOf(g2, g3, t3, t2), wallColor, depth = 0f)) // Side 2
        outPipeline.add(RenderItem3D.Polygon(listOf(g3, g4, t4, t3), wallColor, depth = 0f)) // Side 3
        outPipeline.add(RenderItem3D.Polygon(listOf(g4, g1, t1, t4), wallColor, depth = 0f)) // Side 4
    }

    /**
     * Renders a glowing, procedural 3-dimensional portal warp gate on the sphere's surface.
     */
    fun buildPortalMesh(
        portalX: Float,
        portalY: Float,
        timeAngle: Float,
        outPipeline: MutableList<RenderItem3D>
    ) {
        val center = projectGridToSphere(portalX, portalY, 1f)
        val norm = center.normalized() // Normal pointing radially out

        // Construct 2 coordinate axes orthogonally perpendicular to the sphere surface normal
        val tempU = if (abs(norm.x) < 0.9f) Vector3(1f, 0f, 0f) else Vector3(0f, 1f, 0f)
        val uAxis = (tempU - norm * (tempU.dot(norm))).normalized()
        val wAxis = norm.cross(uAxis).normalized()

        // Build outer rotating ring
        val numSegments = 12
        val radiusOuter = 12f
        val portalColor = Color(0xFF00E5FF) // Electric Neon Cyan

        val outerPts = (0..numSegments).map { i ->
            val angle = (i * 2f * PI.toFloat() / numSegments) + timeAngle
            center + uAxis * (cos(angle) * radiusOuter) + wAxis * (sin(angle) * radiusOuter)
        }

        for (i in 0 until numSegments) {
            outPipeline.add(RenderItem3D.Line(outerPts[i], outerPts[i + 1], portalColor, 4.5f, 0f))
        }

        // Build inner inverse rotating ring
        val radiusInner = 7f
        val innerPts = (0..numSegments).map { i ->
            val angle = (i * 2f * PI.toFloat() / numSegments) - timeAngle * 1.5f
            center + uAxis * (cos(angle) * radiusInner) + wAxis * (sin(angle) * radiusInner)
        }

        for (i in 0 until numSegments) {
            outPipeline.add(RenderItem3D.Line(innerPts[i], innerPts[i + 1], Color(0xFFD500F9), 3f, 0f)) // Deep Magenta
        }
    }
}
```

---

## 7. Development Milestones & Implementation Rollout

To upgrade Moria cleanly without breaking existing MVVM state handling or SQL registries in SQLite, we structure the rollout into 4 strict developmental phases:

```
+------------------------------------------------------------+
| Phase 1: Mathematics & Spheroid Grid Mapping Architecture  |
| - Build SphereDungeonProjection helper class.              |
| - Verify radial polygon vertex transformations coordinate. |
+------------------------------------------------------------+
                             |
                             v
+------------------------------------------------------------+
| Phase 2: Render Pipeline Integration & Camera Follow Hook   |
| - Update DungeonCanvas3D.kt ground and wall block draws.   |
| - Anchor light source relative to radial player position.  |
+------------------------------------------------------------+
                             |
                             v
+------------------------------------------------------------+
| Phase 3: Portal Mechanics & Gravity Well Teleports         |
| - Replace Stairway textures with procedural warp mesh.    |
| - Hook Action buttons to trigger warp sweeps in viewmodel. |
+------------------------------------------------------------+
                             |
                             v
+------------------------------------------------------------+
| Phase 4: Dynamic Theme Scaling & Visual Shard Particles    |
| - Inject colorful planetoid configurations dynamically.    |
| - Write 3D particle controllers for coordinate teleports.  |
+------------------------------------------------------------+
```

---

## 8. Questions & Suggestions for Code optimization

### Q1: Coordinate Wrap-Around Boundaries
* **Suggestion**: When walking off the grid edges on the sphere surface, instead of standard wall blocks stopping progress, coordinates should wrap. East wrapping into West is direct ($\phi \to \phi + 2\pi$), but North/South wrapping crosses the poles.
* **Architecture recommendation**: Implement **Cube-Mapped Surface Traversal** (Option B) for perfect $90^\circ$ coordinate transfers across 6 square map segments, or enforce **Polar Containment Borders** if utilizing single-segment Spherical coordinates (Option A).

### Q2: 3D Shading Optimization
* **Suggestion**: Shading curved tiles demands dynamic facing equations. Calculate the geometric normal $\hat{\mathbf{n}}_{\text{polygon}}$ for every flat polygon of a curved tile and shade via:
  $$\text{ShadeFactor} = \hat{\mathbf{n}}_{\text{polygon}} \cdot \hat{\mathbf{L}}_{\text{light\_vector}}$$
  This produces smooth, gorgeous, planet-like shadows as the player orbits the camera around their character!
