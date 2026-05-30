# 3D World Globe Applet: Architectural Implementation & Texture Mapping Plan

This document details the software architectural blueprint and interactive user experience design for building a high-fidelity, standalone **3D World Globe Custom Applet (World)** in Jetpack Compose, utilizing our high-performance **Sovereign 3D Compose Engine**.

---

## 1. Problem Definition & Intentional UX Philosophy

Virtual globes normally require bulky C++ native libraries (like Google Earth or Cesium) or complex WebViews, leading to slow startup times and resource-heavy execution. Implementing a responsive, offline-capable 3D World Globe in pure Jetpack Compose requires resolving several core challenges:

### A. Viewport and Interactive Touches
* **Gesture Parsing**: Multi-touch handling must differentiate between single-finger dragging (to rotate/orbit the Earth) and two-finger pinching (to zoom into specific geographic coordinates) without jitter.
* **Aspect-Ratio Adaptability**: The sphere should adjust its bounds dynamically to fit both Compact Portrait (smartphones) and Regular Horizontal layouts (tablets/foldable devices) without distorting the sphere into an ellipsoid.

### B. Device Responsive Layouts (Compact vs. Regular)

| Layout Property | Compact Portrait (Phone) | Compact Landscape (Phone) | Regular Screen (Tablet/Foldable) |
| :--- | :--- | :--- | :--- |
| **Canvas Viewport** | Aspect ratio `1:1` filling upper screen boundaries. | Left half of the landscape split-screen. | Left-aligned 3D workbench filling `65%` width. |
| **Texture Selector & UI Controls** | Floating control sheet or bottom-attached sheet. | Right-side scrollable side panel. | Right-side analytical dashboard with bento card slots. |
| **Action Touch Targets** | Minimum `48.dp` floating buttons with clear contrast icons. | Compact `44.dp` toolbar buttons. | Robust `56.dp` action buttons with textual descriptions. |

---

## 2. Mathematical Spherical Projection & UV Mapping

To build a flawless 3D sphere rendered through Jetpack Compose's Canvas draw scope without native OpenGL wrappers:

### A. Parametric Sphere Generation
We generate a coordinate cloud of vertices representing the sphere by iterating across Latitude step variables $\theta$ and Longitude step variables $\phi$:

* Let $R$ be the radius of the sphere (e.g., $150\text{dp}$).
* Let $\theta \in \left[-\frac{\pi}{2}, \frac{\pi}{2}\right]$ be the Latitude angle, subdivided into $N_{\text{lat}}$ divisions.
* Let $\phi \in [-\pi, \pi]$ be the Longitude angle, subdivided into $N_{\text{lon}}$ divisions.

For each index pair $(i, j)$ with $0 \le i \le N_{\text{lat}}$ and $0 \le j \le N_{\text{lon}}$:
$$\theta_i = -\frac{\pi}{2} + \frac{i \cdot \pi}{N_{\text{lat}}}$$
$$\phi_j = -\pi + \frac{j \cdot 2\pi}{N_{\text{lon}}}$$

The 3D Cartesian vertex coordinate $\mathbf{V}_{i,j} = (x, y, z)$ is computed as:
$$x = R \cos \theta_i \cos \phi_j$$
$$y = R \sin \theta_i$$
$$z = R \cos \theta_i \sin \phi_j$$

```
               North Pole (+Y)
                   +--+
                 /      \
               /  (0,R,0) \
             +--------------+  <-- Latitude Theta (i/N_lat)
            /|              |\
           / |              | \
          +--+--------------+--+ <-- Equator (Y=0)
           \ |              | /
            \|              |/
             +--------------+  <-- Longitude Phi (j/N_lon)
               \          /
                 \  +--+ /
               South Pole (-Y)
```

### B. Map Texture UV Alignment
To project a flat 2D Earth map onto the 3D sphere, each vertex $\mathbf{V}_{i,j}$ matches a normalized texture coordinate $\mathbf{UV}_{i,j} = (u, v) \in [0, 1]^2$:
$$u = \frac{j}{N_{\text{lon}}}$$
$$v = \frac{i}{N_{\text{lat}}}$$

### C. Triangulated Mesh Tessellation
Every grid cell bounded by vertices $(i, j)$, $(i+1, j)$, $(i, j+1)$, and $(i+1, j+1)$ is split into two triangular polygons:
1. **Triangle A**: Vertices $[\mathbf{V}_{i,j}, \mathbf{V}_{i+1,j}, \mathbf{V}_{i,j+1}]$ with texture coordinates $[\mathbf{UV}_{i,j}, \mathbf{UV}_{i+1,j}, \mathbf{UV}_{i,j+1}]$.
2. **Triangle B**: Vertices $[\mathbf{V}_{i+1,j}, \mathbf{V}_{i+1,j+1}, \mathbf{V}_{i,j+1}]$ with texture coordinates $[\mathbf{UV}_{i+1,j}, \mathbf{UV}_{i+1,j+1}, \mathbf{UV}_{i,j+1}]$.

---

## 3. High-Performance Software Shading & Painter's Sorting

To prevent performance lagging under dense subdivisions:
1. **Camera Position rotation**: The coordinate points $\mathbf{V}_{i,j}$ are transformed in real-time according to the current touch-pivoted horizontal yaw angle $\alpha_y$ and vertical pitch angle $\alpha_x$.
2. **Backface Culling**: For each face, we calculate the surface unit normal $\mathbf{\hat{N}} = (\mathbf{P_1} - \mathbf{P_0}) \times (\mathbf{P_2} - \mathbf{P_0})$. If its rotated $Z$-axis normal points away from the viewport camera ($Z > 0$), we skip drawing the face altogether to double rendering frame rates.
3. **Painter's Algorithm**: Sort all active triangles back-to-front (descending by calculated average depth $Z_{\text{depth}}$) before issuing the custom drawing loops, ensuring perfect rendering overlap sequence.
4. **Lambertian Diffuse Illumination**: Compute the flat-shading scale multiplier for each polygon face:
   $$I_{\text{diffuse}} = I_{\text{ambient}} + I_{\text{direct}} \cdot \max(0, \mathbf{\hat{N}} \cdot \mathbf{\hat{L}})$$
   *Where $\mathbf{\hat{L}}$ is the directional sun vector*. This light factor dictates the brightness calibration of the rendered texture bitmaps segment.

---

## 4. Architectural Integration & Dynamic Texture Loading

We construct this applet cleanly as a standalone component inside 🍓 FRAISE (`repo-android`):

### A. New File Structure
```
repo-android/app/src/main/java/com/example/cameraxapp/
│
├── core/framework/impl/
│   └── WorldApplet.kt                # Applet descriptor, icon, naming, navigation hooks
│
└── world/
    ├── WorldScreen.kt                # Primary interactive Composable split viewport layouts
    ├── WorldViewModel.kt             # View-Model managing touch states, texture image URIs, and thread caches
    └── WorldGlobeCanvas.kt           # Custom drawing on Canvas, UV affine mapping calculations
```

### B. Device Image Selector (GetContent)
We use Jetpack Compose's state-driven `rememberLauncherForActivityResult` inside `WorldScreen` to trigger the Android system photo picker, decoding the result asynchronously on high-speed background IO coroutine threads:

```kotlin
val textureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri: Uri? ->
    if (uri != null) {
        viewModel.loadCustomTexture(context, uri)
    }
}
```

Inside `WorldViewModel`, decode the local image input stream into standard `ImageBitmap`:
```kotlin
fun loadCustomTexture(context: Context, uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    _customTexture.value = bitmap.asImageBitmap()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
```

---

## 5. Sequential Step-by-Step Implementation Flow

### Step 1: Bootstrap the Applet Descriptor (`WorldApplet.kt`)
* Implement the custom `Applet` interface.
* ID: `"world_globe"`. Name: `"World 3D Globe"`. Description: `"Interactive 3D Virtual globe with custom texture mapping support."`.
* Assign a distinctive globe vector launcher icon, e.g., `Icons.Default.Place` or similar.
* Map its `Content` rendering block to launch `WorldScreen`.

### Step 2: Register Applet in `MainActivity.kt`
* Add `AppletRegistry.register(WorldApplet())` during application startup inside `onCreate()`.

### Step 3: Author the Triangulated Canvas Shader (`WorldGlobeCanvas.kt`)
* Adopt the triangulated affine coordinate drawing math using standard `android.graphics.BitmapShader` aligned to Compose's native drawing matrices (`android.graphics.Matrix()`).
* Support falling back to a pre-packaged Earth map asset (e.g. `earth_map_low_res.jpg` inside Android raw resources) if no user custom photo is selected.

### Step 4: Handle standard drag and zoom Gestures
* Integrate Compose `pointerInput(Unit)` with `detectTransformGestures` or low-level manual coordinate drag detectors to calculate delta yaw and pitch offsets, mapping scaling inputs directly to scale size variables with strict containment bounds to prevent coordinate inversion.

---

## 6. Immediate Practical Lab for Student Developers
(Appended to `/docs/training/PART-07-MOBILE.md` in compliance with instruction rules)

### 78. Interactive 3D World Globe Applet: Parametric Spheres & Dynamic Texture Selectors (20h) 🍓 NEW
* **Reference**: `repo-android/docs/world-applet-plan.md`, `repo-android/app/src/main/java/com/example/cameraxapp/world/`
* **Goal**: Understand how to map, project, and rotate a 3D spherical point cloud using parametric equations in Jetpack Compose, and build an interactive file-picker to dynamically load local device images onto the 3D sphere as a BitmapShader texture.
* **Exercise**: Implement a parametric grid density slider (e.g., latitude/longitude subdivision steps) and verify that lower density settings yield a lightweight polyhedral sphere while higher density settings yield high-precision smoothness.
* **Complexity**: Part 5
