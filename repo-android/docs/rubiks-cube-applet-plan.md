# 3D Rubik's Cube Applet: Architectural Implementation & Responsive UX Plan

This document details the software architectural blueprint and responsive user experience design for building a high-fidelity, standalone **3x3 Rubik's Cube 3D Puzzle Sandbox** in Jetpack Compose, emphasizing optimized form-factors for smartphones and tablets.

---

## 1. Problem Definition & Intentional UX Philosophy

Traditional mobile puzzle simulators suffer from clumsy controls and cramped viewports. Designing a 3D Rubik's Cube requires solving several interaction friction points:

### A. Viewport and Layout Degradation
* **The Scrolling Trap**: Wrapping a 3D viewport inside a scrollable layout causes drag gesture conflicts. Users attempting to swipe a cube slice may accidentally trigger page panning.
* **Layout Slicing**: On narrow mobile screens (320dp - 420dp width) in portrait, or vertically constrained screens (320dp - 450dp height) in landscape, pre-allocated elements such as control buttons, timers, and moves panels can block or shrink the 3D canvas, rendering touch targets too small to hit.

### B. Responsive Grid Adaptations (Compact vs. Regular)
Using Jetpack Compose's `LocalConfiguration.current`, our layout splits the workspace into optimized design structures depending on dynamic size classes:

| Layout Property | Compact Portrait (Phone) | Compact Landscape (Phone) | Regular Screen (Tablet/Foldable) |
| :--- | :--- | :--- | :--- |
| **Canvas Constraints** | Aspect ratio `1:1` filling upper `50%` of screen height. | `50%` width on left, taking full vertical safe area. | Left alignment, taking up `60%` horizontal grid area. |
| **Control Panels** | Bottom sliding card sheet containing tabbed slice turns. | Right column, vertically scrollable list of controls. | Right-side bento grid with analytical metrics and settings. |
| **Action Target Size** | `48.dp` to `56.dp` high-precision touch buttons. | Compact `44.dp` high-precision buttons. | Full `64.dp` action cards with descriptions. |
| **Pinch-to-Zoom Rail** | Invisible overlay margin with a pinch-to-zoom rail. | Invisible overlay scale rail. | Dedicated vertical slider overlay widget. |

---

## 2. 3D Architectural Engine & Rotation Mathematics

To render a 3D Rubik's Cube without high-overhead OpenGL native modules:
1. The 3D model represents a set of **27 individual minisc blocks (cubies)** indexed in coordinate space:
   $$X, Y, Z \in \{-1, 0, 1\}$$
2. Each cubie has its own center vector $\mathbf{C} = (x_c, y_c, z_c)$ and contains six outer face coordinate polygons (Up, Down, Left, Right, Front, Back).
3. The surface normals of each face are initialized in standard unit axes:
   $$\vec{\mathbf{n}}_{up} = (0, 1, 0), \quad \vec{\mathbf{n}}_{down} = (0, -1, 0), \quad \dots$$

```
           +--------------+
          /      UP      /|
         /    (Yellow)  / |
        +--------------+  |
        |              |  | RIGHT
        |    FRONT     |  | (Red)
        |    (Green)   | / All pieces indexed as:
        |              |/  X, Y, Z coordinates [-1, 0, 1]
        +--------------+
```

### A. Layer-Slice Angle Rotation
To animate or rotate a slice (e.g., rotating the top layer $Y = 1$ clockwise by an angle $\phi$), we isolate the 9 cubies where the coordinate condition matches $Y_c = 1$.
Each coordinate point $\mathbf{P} = (x, y, z)$ belonging to the target cubies is rotated around the $Y$-axis using horizontal rotational matrices:

$$x' = x \cos \phi + z \sin \phi$$
$$z' = -x \sin \phi + z \cos \phi$$
$$y' = y$$

When the turn completes ($\phi = \pm 90^\circ$ or $\pm \frac{\pi}{2}$ radians):
1. **Mathematical Snapping**: Round the computed vertex coordinates back to the nearest integer grid values $-1, 0, 1$.
2. **State Translation**: Swap the index states of the affected faces. For example, a clockwise rotation on $Y=1$ maps the Front face color to the Left face, Left to Back, Back to Right, and Right to Front.

### B. Perspective Camerawork Model
Points are projected onto 2D viewport coordinates using the **Sovereign Projection Pipeline**:

$$S_x = C_x + \frac{x'' \cdot F_L \cdot S_f}{z'' + D_c}$$
$$S_y = C_y + \frac{y'' \cdot F_L \cdot S_f}{z'' + D_c}$$

To prevent depth conflicts, the engine calculates the average depth of each face using its transformed $Z$-vector, then applies the **Painter's Algorithm** to render backplanes first, overwriting with close planes on top.

---

## 3. Responsive UX Refactoring Blueprint & File Tree

To construct this applet cleanly as a standalone component inside 🍓 FRAISE:

### Phase 1: Interactive Gestural Swiping (`BoxWithConstraints`)
Use dynamic width and height observers to ensure the interactive 3D Canvas updates on rotation changes without breaking layout boundaries:
```kotlin
@Composable
fun RubikInteractiveCanvas(
    cubeState: List<RubikMinisc>,
    yaw: Float,
    pitch: Float,
    scale: Float,
    onMoveSelected: (axis: String, slice: Int, clockwise: Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Drag gestures implementation
                // Detect coordinate angles and translate drags to cube slice turns
            }
    ) {
        val width = maxWidth
        val height = maxHeight
        // Calculate canvas dimensions ...
    }
}
```

### Phase 2: Compact Landscape Spacing Optimizer
On landscape responsive grids, stack buttons in a side-by-side arrangement:
- Minimize spacer sizes to a tight `4.dp` - `8.dp` range.
- Place the core game indicators (Move count, timer clock) in a compact top bar overlay directly on the canvas, freeing valuable screen area for manual slice rotators on the right side.

---

## 4. Immediate Practical Lab for Student Developers
(Appended to `/docs/training/PART-07-MOBILE.md` in compliance with rules)

Explore hands-on challenges to add puzzle moves validation and check-solved metrics!
