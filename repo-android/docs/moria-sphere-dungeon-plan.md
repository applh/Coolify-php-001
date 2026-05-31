# 🪐 Moria Roguelike: Continuous 3D Spherical World & Analogue Navigation Architecture Plan

This document presents the comprehensive architectural evolution, mathematical formalisms, and interface designs to transition **Moria** from a 2D discrete grid-constrained environment into a **continuous 3D Spherical World**.

Instead of navigating distinct grid blocks, the player roams freely in any arbitrary direction on a smooth planetary manifold. Staircases are replaced by **3D Teleportation Portals** that warp the player between celestial spheres.

---

## 1. System Paradigm Shift: Grid-Based vs. Continuous Spherical

To build a true continuous 3D sphere where the player is not locked to grid steps, we fundamentally redesign the underlying coordinate engine and camera behavior:

| Modality | Old Architecture (Grid-Based) | New Architecture (Continuous Spherical) |
| :--- | :--- | :--- |
| **Position State** | Discrete integer coordinates `(pX: Int, pY: Int)` | Continuous 3D vector $\mathbf{P} = (x, y, z)$ on unit sphere scaled by radius $R$. |
| **Directional Inputs** | Standard 4-button D-Pad (discrete jumps) | Virtual analog circle pad (continuous 360-degree vectors). |
| **Physics / Collision** | Integer grid index lookups (`Map[y][x] == WALL`) | Continuous radial distance spheres & great-circle barrier collision checks. |
| **Orientations / Yaw** | Camera snaps to $90^\circ$ quadrant intervals | Continuous orbital sliding around the player's local horizon. |
| **Floor Transitions** | Standard stairway meshes triggering static loads | Volumetric grav-well portals sucking players inside via dynamic orbital warps. |

---

## 2. Mathematical Engine: Continuous Navigation & Physics on a Sphere

To ensure complete mathematical stability across the entire sphere—especially avoiding division-by-zero or coordinate spinning anomalies at the North and South poles (known as *Gimbal Lock* and *Polar Singularities*)—we model all positions, orientations, and movements strictly using **3D Vector Algebra** rather than basic latitude/longitude integrals.

```
                   +-- [North Pole] (0, R, 0)
                 .  *  .
              .   \     .
            .      \      .
           .   p_t  \      .
          *----->   \       *
          |  \       \      |
          |   \       o-----+---- [Sphere Center] (0, 0, 0)
          |    \            |
           .    `----->    .
            .       v_mov .
              .         .
                 .   .
                   +-- [South Pole] (0, -R, 0)
```

### A. Position State Mechanics
Let the sphere be centered at the origin $\mathbf{O} = (0, 0, 0)$ with a physical radius $R$.
The player's coordinate is represented as a normalized unit direction vector $\hat{\mathbf{P}} = (x_p, y_p, z_p)$ where $\|\hat{\mathbf{P}}\| = 1.0$.
Their continuous position in world coordinates is:
$$\mathbf{P} = R \cdot \hat{\mathbf{P}}$$

### B. Mappings Dynamic Tangent Plane (Continuous Directions)
When the player stands on the planetoid at unit direction $\hat{\mathbf{P}}$, their local tangent plane defines the ground they walk on. The local cardinal directions are defined as:
1. **Local North Direction** ($\mathbf{T}_N$): The tangent pointing toward the physical North Pole $\mathbf{N} = (0, 1, 0)$:
   - Project the vector $(0, 1, 0)$ onto the plane perpendicular to $\hat{\mathbf{P}}$:
     $$\mathbf{T}_{N,\text{unnorm}} = (0, 1, 0) - (\hat{\mathbf{P}} \cdot (0, 1, 0)) \hat{\mathbf{P}}$$
   - Normalize the result:
     $$\mathbf{T}_N = \frac{\mathbf{T}_{N,\text{unnorm}}}{\|\mathbf{T}_{N,\text{unnorm}}\|}$$
2. **Local East Direction** ($\mathbf{T}_E$): The tangent perpendicular to both the position vector and local North:
     $$\mathbf{T}_E = \mathbf{T}_N \times \hat{\mathbf{P}}$$

*Note*: If the player stands exactly at the North Pole $(0, 1, 0)$ or South Pole $(0, -1, 0)$, $\mathbf{T}_{N,\text{unnorm}}$ collapses to zero. In this singular scenario, we use the fallback vector $(0, 0, 1)$ or reference the player's current velocity heading history to maintain directional continuity.

### C. Integrating Continuous Joystick Speeds
The virtual joystick outputs a normalized 2D movement offset vector $\mathbf{J} = (dx, dy)$ in the range $[-1.0, 1.0]$.
We translate this coordinate into 3D tangent velocities relative to the player's orientation:
$$\mathbf{V}_{\text{tangent}} = \left( dy \cdot \cos(\alpha) - dx \cdot \sin(\alpha) \right) \mathbf{T}_N + \left( dy \cdot \sin(\alpha) + dx \cdot \cos(\alpha) \right) \mathbf{T}_E$$
where $\alpha$ is the camera's current yaw perspective relative to the local North.

The player's new position $\hat{\mathbf{P}}_{t+1}$ on the sphere is integrated continuously using:
$$\hat{\mathbf{P}}_{t+1} = \text{Normalize}\left( \hat{\mathbf{P}}_t + \mathbf{V}_{\text{tangent}} \cdot \text{SpeedMultiplier} \cdot dt \right)$$
Multiplying by $R$ returns the new visual coordinate, keeping the player perfectly flush with the spherical terrain without any geometric drift.

---

## 3. UI Component: Virtual Analog Circle Pad

Traditional discrete step button layouts are refactored into a high-responsiveness **Floating Circle Pad** which acts as a 2D vector coordinate feeder.

```
       +-------------------------------+
       |       Circular Joy Pad        |
       |             .-"-.             |
       |           .'     '.           |
       |          /    .---. \         |
       |         |    (     ) | <---- [Thumb Indicator (dx, dy)]
       |         |     '---'  |        |
       |          \          /         |
       |           '.       .'         |
       |             '-...-'           |
       |         [Base Outer Ring]     |
       +-------------------------------+
```

### A. Touch Jetpack Compose Vector Coordinates Tracking
The Joystick is rendered inside standard Compose Canvas using a `pointerInput` block with `detectDragGestures`:
1. On initial click, the touch start coordinate is normalized.
2. During drag action:
   - Calculate coordinate distance relative to the base center $\mathbf{C} = (c_x, c_y)$.
   - Radius of bounding circle $L_{\text{max}} = 65\text{dp}$.
   - Let drag distance be $L = \sqrt{dx^2 + dy^2}$.
   - If $L > L_{\text{max}}$, normalize the thumb stick coordinate:
     $$dx_{\text{bounded}} = \frac{dx}{L} \cdot L_{\text{max}}, \quad dy_{\text{bounded}} = \frac{dy}{L} \cdot L_{\text{max}}$$
   - Feeds values $dx_{\text{norm}} = \frac{dx_{\text{bounded}}}{L_{\text{max}}}$ and $dy_{\text{norm}} = -\frac{dy_{\text{bounded}}}{L_{\text{max}}}$ into the loop.

---

## 4. Navigation Assistance: Polar Beacons & Geomagnetic Grids

Navigating an open, featureless sphere without grid tiles is highly prone to orientation confusion ("disorientation"). To resolve this, we implement two primary visual landmarks:

```
                  Polar Beacon column (Vertical projection)
                     |||
                     |||
               .     |||     .
            .  * * * *|* * * *  . <---- Parallels (Latitude Ring)
          .   *       |       *   .
         .   *        |        *   .
        *    *        |        *    *
        |====*========o========*====| <---- Meridians (Longitude Lines)
        *    *        |        *    *
         .   *        |        *   .
          .   *       |       *   .
            .  * * * *|* * * *   .
               .     |||      .
                     |||
                     |||
```

### A. Volumetric Polar Energy Columns (Auroral Beacons)
Concentric 3D wireframe cylinder columns extend radially outward at the poles:
* **North Pole**: Emerald Green cosmic beam centered at $(0, R, 0)$ reaching to height $2.5R$.
* **South Pole**: Royal Orchid Purple cosmic beam centered at $(0, -R, 0)$ reaching to height $2.5R$.
* **Math Model**: Composed of layered concentric outer rings stacked vertically, fading gradually in brightness in proportion to height. These pillars are rendered in pure emissive light directly into the rendering queue, visible from any horizon.

### B. Geomagnetic Parallels & Meridian Ribbons
A beautiful low-poly planetary mesh coordinates structure is projected on the ground to act as structural reference lines:
* **The Parallels (Latitude Lines)**: Concentric rings drawn every $15^\circ$ elevation angle, wrapping parallel to the equator.
* **The Meridians (Longitude Lines)**: Spherical path ribbons connecting the North and South poles, drawn every $30^\circ$ interval.
* **Math Model**: Represented as thin continuous line chains mapped through $\mathbf{V}(x, y, z)$ coordinates and depth-shaded dynamically to fade in the dark, giving the sensation of a planetary grid.

---

## 5. Portal Warp Engine: Celestial Sphere-to-Sphere Warp

Traditional staircase dungeons are refactored into **Atmospheric Gyro-Portals** that pull the player across independent planetoids.

```
                   Spherical Warp Transition
            
          Player ->   o .                  (Warp Transition)
                       \ ` .  
                        \    ` .  
                         \       v
                    [Current Core] -----> [Next Celestial Sphere]
```

### A. Geodesic Proximity Detection
Every portal on the sphere is stored with a 3D unit coordinate: $\hat{\mathbf{P}}_{\text{portal}}$.
The absolute spatial distance between player and portal is calculated using the spherical chords model:
$$D_{\text{spatial}} = \|\mathbf{P}_{\text{player}} - \mathbf{P}_{\text{portal}}\| = R \cdot \sqrt{(x_{\text{p}} - x_{\text{port}})^2 + (y_{\text{p}} - y_{\text{port}})^2 + (z_{\text{p}} - z_{\text{port}})^2}$$

If $D_{\text{spatial}} < R_{\text{portal\_field}}$ (e.g. $12\text{dp}$), a strong graviton vacuum pull vector is added to the joystick speed input, sucking the player toward the exact gate coordinate.

### B. Warp Sequence Animation State Machine
When player coordinate merges inside the portal core ($1.5\text{dp}$ threshold), a 900ms teleportation frame is triggered:
1. **Camera Spin Sweep**: Camera begins to orbit rapidly around the portal's radial norm vector while decreasing zoom parameters, displaying the planetoid retreating into the background void.
2. **Radial Shard Disintegration**: The player cylindrical mesh model decomposes into 64 floating diamond particle vertices expanding radially out:
   $$\mathbf{P}_{\text{shard}, i} = \mathbf{P}_{\text{player}} + \text{Velocity}_i \cdot dt \cdot \hat{\mathbf{n}}_{\text{sphere}}$$
3. **World Swapping**: Re-init database cache, generate the next sphere structure (different planet colors, different radius sizes, new boss positioning), and fade the camera back in from the new sphere's polar arrival pad.

---

## 6. Upgraded Implementation Roadmap

To upgrade Moria seamlessly without losing character level states, combat parameters, and high-scores structures stored in standard SQLite and state flows, we structure the continuous upgrade into 4 sequential phases:

### Phase 1: Coordinate Systems & Continuous Geodesic Physics (5h)
* Create `SphereVectorDungeon` helper class to map free placement coordinates.
* Implement 3D tangent vectors projection mapping for local North/East relative movement.
* Refactor player movement functions to support continuous floats instead of discrete map bounds indexing.

### Phase 2: Virtual Circle Joy-Pad Controls Overlay (4h)
* Replace discrete directional button grids in `HudPanel` with a fully custom gesture-tracking analogue canvas view.
* Formulate dragging boundary bindings to return smooth, proportional velocity coefficients inside the dynamic FPS rendering thread.

### Phase 3: Low-Poly Geomagnetic Grid Lines & Polar Beacon Auroras (5h)
* Implement mathematical parallels/meridian ribbon builders.
* Coordinate polar beacon coordinates drawing at $(0, R, 0)$ and $(0, -R, 0)$.
* Hook camera depth drawing to render wireframe landmarks in emissive glow colors.

### Phase 4: Portals Collision & Gravity Warp Mechanics (6h)
* Setup spatial distance proximity loops mapping inside `RoguelikeViewModel`.
* Build counter-rotating procedurally generated gyro rings.
* Integrate multi-stage warp animation phases (Disintegration, planet retraction, new planet injection fade-in) in `DungeonCanvas3D`.

---

## 7. Strategic Questions & Suggestions for User Feedback

### Q1: Continuous Spherical Obstacle & Enemy Collision Checks
* **The Constraint**: Since grids are gone, we cannot check `map[y][x]` to deter running into walls.
* **Architectural Suggestion**: We can represent world obstacles (like procedural lava vents, ancient obsidian crates, static dungeon ruins) as simple **Spherical Coordinate Bounding Spheres**:
  $$D_{\text{collision}} = \|\hat{\mathbf{P}}_{\text{player}} - \hat{\mathbf{P}}_{\text{obstacle}}\| < \theta_{\text{check}}$$
  If a collision is detected, we slide the player's movement vector along the tangent plane boundary to keep running smooth and fluid. Do you prefer this sliding cylinder physics model, or should we use hard stop vectors?

### Q2: Dynamic Spherical Combat mechanics
* **Architectural Suggestion**: In a continuous 3D world, enemies (e.g., Goblins, Arch-Mages) wander continuously along random geodesic paths towards the player coordinate.
* **Suggestion**: To make targeting feels highly polished, should we implement a **Magnetic Target Lock-On**? Pressing a locking button snaps the 3D camera to track the closest target's coordinate, assisting linear spellcasting or continuous forward slashing.
* **Alternative**: Do you prefer traditional directional slashing, where spells/attacks simply cast directly forward along the current movement heading?

### Q3: Dynamic Background Space Skybox
* **Suggestion**: To heighten the spatial depth as the camera rotates around the spheres, we can build a **Celestial Starry Nebula Background**. By rendering 120 randomized, distant star vector points that rotate at $0.15\times$ coordinate camera speed, we create a beautiful parallax depth effect that makes the player truly feel they are running on a celestial sphere floating in space!
