# 3D Vector Workspace: Architectural Designs & App Proposals

This document details the architectural foundation of the **3D Workspace suite** implemented in 🍓 FRAISE (inside `repo-android`), and proposes several high-fidelity application concepts that benefit from interactive, low-overhead 3D visualizations in mobile environments.

---

## 1. High-Value 3D Android Application Proposals

While mobile environments are visually constrained compared to desktop or dedicated workstation setups, 3D views offer profound interactive advantages for specific workspaces. The following five paradigms represent high-value opportunities for mobile 3D visualizations:

### A. Molecular Biology Workbench & Chemistry Sandbox
* **The Need:** Structural chemistry and molecular bonds are highly spatial. Flat, 2D representations fail to convey tetrahedral geometry, chiral molecules (mirror image structures), or double-helix offsets.
* **The Benefit:** Real-time 3D allows students, researchers, and biochemists to twist and isolate bases or atoms, inspect covalent bond lengths, measure angular angles, and visualize space-filling electron cloud thresholds directly in their hand.
* **Our Solution:** The *Molecular Analytics Mode* visualizes complexes like Caffeine ($C_8H_{10}N_4O_2$), Water ($H_2O$), DNA base segments, and Buckyballs (C60) with custom radial-shaded spheres and volumetric bond lines.

### B. Architectural Layout CAD & Exploded Blueprint Viewer
* **The Need:** Blueprints are traditionally rendered as horizontal 2D planes, forcing the construction lead or architect to mentally synthesize floors and vertical conduits.
* **The Benefit:** Users can orbit and peek inside a building layout. By applying an "Exploded CAD Split" slider, floor levels rise vertically apart like physical dollhouses, rendering internal rooms, furniture models, doors, and plumbing pipes visible without obstruction.
* **Our Solution:** The *CAD Architecture Mode* supports multi-story elevation splitting, day/sunset/night lighting cycles that dynamically shift polygon colors, and rotatable doors using hinge math.

### C. Topographic Elevation Mapping & GIS Flight Radar
* **The Need:** Analysts, rescue teams, hikers, and aviation dispatchers must analyze slopes, elevations, water flows, valleys, and coordinates dynamically.
* **The Benefit:** Interactively panning terrain highlights ridges and lowlands. Overlaying simulated water tables shows potential flood lines, and rendering flyover coordinate nodes guides safe flights.
* **Our Solution:** The *Topographic Elevation Mode* utilizes procedural multi-frequency noise grids, customized land height coloration layers from coastal sand through green plains to snow caps, an interactive sea-level glider, and an orbiting drone with automatic heading vectors.

### D. Kinematic Transmission & Exploded Gear Engine Simulator
* **The Need:** Mechanical cogs, planetary transmission systems, pistons, and engines have complex mechanical interactions that are difficult to explain in standard textbook diagrams.
* **The Benefit:** 3D rotational mechanics explain meshing gear speeds, timing alignments, sliding strokes of cylinder pistons, and part structures. Turning "Exploded Assemblies" outward allows mechanic diagnostics and instructional guidelines.
* **Our Solution:** The *Gear & Piston Kinematics Mode* animates three intermeshing spurred gears with calculated tooth mesh offsets and an engine crankshaft piston linked to a cylinder joint.

### E. Spatial Reasoning 3D Puzzles (Rubik's Cubes)
* **The Need:** Puzzles requiring dynamic rotational symmetry require rapid spatial orientation across multiple coordinate planes.
* **The Benefit:** Users can inspect alternative corners and rotate slice matrices in natural 3D.
* **Our Solution:** The *3D Puzzle Sandbox Mode* renders a full 27-block (3x3) Rubik's cube, allowing users to select and revolve slice rings along X, Y, or Z matrices with proper cell translation snapping.

---

## 2. Theoretical Mathematics & Rendering Architecture

Our **Sovereign 3D Compose Engine** executes real-time depth sorting and perspective projections entirely on standard Jetpack Compose custom drawing canals (`Canvas`), bypassing bulky Native OpenGL, Vulkan, or Filament dependencies to ensure instant app cold-starts and maximum stability.

### A. Rotational Transformations
To rotate any point $\mathbf{v} = (x, y, z)$ in 3D around the X, Y, and Z coordinate axes by angles $\theta_x$ (Pitch), $\theta_y$ (Yaw), and $\theta_z$ (Roll), we apply matrix rotation vectors:

* **Rotation Y (Horizontal Yaw):**
  $$x' = x \cos \theta_y + z \sin \theta_y$$
  $$z' = -x \sin \theta_y + z \cos \theta_y$$

* **Rotation X (Vertical Pitch):**
  $$y' = y \cos \theta_x - z' \sin \theta_x$$
  $$z'' = y \sin \theta_x + z' \cos \theta_x$$

* **Rotation Z (Bank/Roll):**
  $$x'' = x' \cos \theta_z - y' \sin \theta_z$$
  $$y'' = x' \sin \theta_z + y' \cos \theta_z$$

### B. Perspective Projection Model
To map the transformed 3D point $(x'', y'', z'')$ onto flat 2D screen coordinates $(S_x, S_y)$, we apply focal compression matrices taking the distance of the camera into account:

$$S_x = C_x + \frac{x'' \cdot F_L \cdot S_f}{z'' + D_c}$$
$$S_y = C_y + \frac{y'' \cdot F_L \cdot S_f}{z'' + D_c}$$

Where:
* $(C_x, C_y)$ is the midpoint center of the viewport Canvas.
* $F_L$ represents the lens focal length (e.g., $500\text{px}$).
* $S_f$ represents the scale factor magnification adjusted by pinch-to-zoom sliders.
* $D_c$ represents camera perspective offset depth (e.g., $350\text{f}$).

### C. Painter's Algorithm for Depth Conflict Resolution
Because a raw canvas lacks a hardware depth buffer, rendering faces out of sequence causes distant surfaces to overlap close elements, producing visual projection glitches. 

We solve the hidden-surface problem cleanly using the **Painter's Algorithm**:
1. Every graphic primitive is encapsulated as a `RenderItem3D` (Spheres, Lines, or Polygons).
2. During the pipeline stage, we calculate the average transformed $Z$ depth component of each element.
   * For a sphere: $Z_{depth} = Z_{center}$.
   * For a polygon with $N$ vertices: $Z_{depth} = \frac{1}{N} \sum_{i=1}^N Z_{vertex\_i}$.
3. We sort the list of primitives descending by depth value before drawing:
   * **Furthest** primitives ($Z$ positive) are rendered first.
   * **Closest** primitives ($Z$ negative) are rendered last, overwriting backplanes correctly.
4. If backface culling is enabled, we calculate the polygon's 3D surface normal:
   $$\vec{\mathbf{Normal}} = (\mathbf{P_1} - \mathbf{P_0}) \times (\mathbf{P_2} - \mathbf{P_0})$$
   If the normal's transformed $Z$-vector points away from the camera ($Z > 0$), we prune it entirely to double draw performance.

---

## 3. Practical Usage and Interaction Flow

1. **Touch Dragging Orbit:** Dragging in any direction inside the dark cosmic viewport shifts the `yawAngle` and `pitchAngle` values instantly, updating the projection model.
2. **Double Draw Buffering:** All simulation cogs generate their respective polygons dynamically, pipeline-sorting them on every tick of the animation loop.
3. **Responsive Side Layout:** Users can switch modes dynamically via the elegant sidebar, scale the drawing size, toggle Y-axis auto-rotation, and tune parameters from sliders without any lagging performance.
