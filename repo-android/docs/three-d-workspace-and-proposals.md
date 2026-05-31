# 3D Vector Workspace: Architectural Designs & App Proposals

This document details the architectural foundation of the **3D Workspace suite** implemented in 🍓 FRAISE (inside `repo-android`), and details an upgraded implementation plan for integrating high-fidelity **3D Lights (Illumination models)** and **3D Textures (Texture-Mapping)** inside our custom standard Jetpack Compose Canvas rendering pipeline.

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

## 3. Real-Time 3D Lighting & Shading Architecture

To elevate flat polygon forms into realistic 3D volumes, we specify a localized, high-performance shading pipeline based on vector mathematics processed entirely during standard Jetpack Compose Canvas draw cycles.

### A. Surface Normal Vector Computation
For any 3D polygon face with vertices specified in counter-clockwise order ($\mathbf{P_0}, \mathbf{P_1}, \mathbf{P_2}$), we define the unnormalized normal vector $\mathbf{N}_u$ via vector cross-products:
$$\mathbf{N}_u = (\mathbf{P_1} - \mathbf{P_0}) \times (\mathbf{P_2} - \mathbf{P_0})$$

To obtain the unit face normal $\mathbf{\hat{N}}$, we dividing by its magnitude:
$$\mathbf{\hat{N}} = \frac{\mathbf{N}_u}{\|\mathbf{N}_u\|}$$

This face normal $\mathbf{\hat{N}}$ dictates how light bounces off the polygon flat-plane.

### B. Illumination Model (Phong & Lambert Refraction)
For a given scene lighting system, we define:
1. **Ambient Light ($I_{ambient}$)**: Omnidirectional background glow that illuminates all parts of the scene uniformly.
   $$I_{ambient} = K_a \cdot C_{ambient}$$
2. **Diffuse Light ($I_{diffuse}$)**: Light intensity based on the angle of incidence between the unit face normal $\mathbf{\hat{N}}$ and a normalized directional light vector $\mathbf{\hat{L}}$ pointing to the light source.
   By Lambert's Cosine Law:
   $$I_{diffuse} = K_d \cdot C_{light} \cdot \max(0, \mathbf{\hat{N}} \cdot \mathbf{\hat{L}})$$
3. **Specular Light ($I_{specular}$)**: Highlight glints reflecting off shiny surfaces. Let $\mathbf{\hat{V}}$ be the normalized view reflection direction pointing to the virtual camera, and $\mathbf{\hat{R}}$ be the perfect reflection bounce vector of the light:
   $$\mathbf{\hat{R}} = 2(\mathbf{\hat{N}} \cdot \mathbf{\hat{L}})\mathbf{\hat{N}} - \mathbf{\hat{L}}$$
   By Phong's Reflection Model:
   $$I_{specular} = K_s \cdot C_{light} \cdot \max(0, \mathbf{\hat{R}} \cdot \mathbf{\hat{V}})^\alpha$$
   *Where $\alpha$ represents the material shininess coefficient (high value generates clean tight highlight spots, low value generates wide blurry spots).*

The accumulated final light intensity factor $I_{total}$ scales the base polygon color:
$$I_{total} = I_{ambient} + I_{diffuse} + I_{specular}$$

### C. Shading Modes Implementation
* **Flat Shading (Pre-compiled)**: Surface normal is computed once per polygon. The color of all points in the polygon is identical, based on the face normal. Outstanding for low-poly or high-performance renderings.
* **Gouraud Shading**: Surface normals are computed per-vertex by averaging neighboring polygon face normals. Light intensities are calculated at each vertex, then bilinear-interpolated across the 2D projected screen coordinate plane.
* **Phong Shading (Per-Pixel)**: Virtual normals are interpolated per-pixel across the screen space, and the illumination model is executed on every drawn pixel (high CPU usage for custom Canvas, optimized by sub-sampling or path gradients).

---

## 4. 3D Texture Mapping on Custom Compose Canvas

Texturing projects a 2D source image (`ImageBitmap`) onto an oriented 3D polygon, mapping texture space coordinate targets $(u, v) \in [0, 1]^2$ to 3D vertices $(\mathbf{P}_0, \mathbf{P}_1, \mathbf{P}_2)$.

### A. Affine Coordinate Transformations
For standard triangles, we calculate a 2D affine transformation matrix $\mathbf{M}$ mapping the texture coordinates $(u, v)$ to the screen-space coordinates $(x_s, y_s)$:
$$\begin{bmatrix} x_s \\ y_s \\ 1 \end{bmatrix} = \mathbf{M} \begin{bmatrix} u \\ v \\ 1 \end{bmatrix}$$

For a triangle with three vertices $\mathbf{V}_0, \mathbf{V}_1, \mathbf{V}_2$ having projected screen coordinates $(x_0, y_0), (x_1, y_1), (x_2, y_2)$ corresponding to texture coordinates $(u_0, v_0), (u_1, v_1), (u_2, v_2)$, we compute $\mathbf{M}$ by solving the linear system:
$$\begin{bmatrix} x_0 & x_1 & x_2 \\ y_0 & y_1 & y_2 \\ 1 & 1 & 1 \end{bmatrix} = \mathbf{M} \begin{bmatrix} u_0 & u_1 & u_2 \\ v_0 & v_1 & v_2 \\ 1 & 1 & 1 \end{bmatrix}$$
$$\mathbf{M} = \begin{bmatrix} x_0 & x_1 & x_2 \\ y_0 & y_1 & y_2 \\ 1 & 1 & 1 \end{bmatrix} \begin{bmatrix} u_0 & u_1 & u_2 \\ v_0 & v_1 & v_2 \\ 1 & 1 & 1 \end{bmatrix}^{-1}$$

In Jetpack Compose, this matrix is applied to the standard `android.graphics.Matrix` object inside the canvas draw scope:
```kotlin
val matrix = android.graphics.Matrix()
matrix.setValues(floatArrayOf(
    M00, M01, M02,
    M10, M11, M12,
    0f,   0f,   1f
))
```
Using `drawScope.drawIntoCanvas { canvas -> ... }` with a `BitmapShader` initialized with the `ImageBitmap` and configured with the local transform `matrix`, the texture is clipped and drawn exactly inside the projected screen triangle paths!

### B. Perspective-Correct Texture Mapping
Pure linear affine mapping on projected coordinates causes the texture to stretch and warp unnaturally during steep camera tilts (the classic "chevron" distortion). 

To ensure **perspective correctness**, we must divide the texture coordinates by the corresponding rotated depth component $Z$ before interpolation, and interpolate $1/Z$ concurrently:
$$u_{\text{interp}}(t) = \frac{(u/z)_{\text{interp}}}{(1/z)_{\text{interp}}}, \quad v_{\text{interp}}(t) = \frac{(v/z)_{\text{interp}}}{(1/z)_{\text{interp}}}$$

Because standard Android UI paths render statically, we outline two low-overhead implementation paradigms:
1. **Dynamic Mesh Subdivision**: Subdividing large polygons into micro-triangles. For each micro-triangle, affine transformation matrices approximate perspective mapping with negligible linear errors.
2. **Procedural Shaders / Bitmap Shader Brushes**: Using linear `ShaderBrush` with customized multi-stop coordinate offsets or built-in noise bands.

---

## 5. Architectural Implementation Blueprint (Kotlin / Compose)

Below is the concrete code design blueprint containing the upgraded data structures, light configurations, vector math wrappers, and canvas integration pipelines.

### A. Upgraded Data Structures
```kotlin
// In RenderItem3D.kt
sealed class RenderItem3D {
    abstract val depth: Float

    data class Polygon(
        val pts: List<Vector3>,
        val texCoords: List<Offset>? = null, // UV coords mapping (0f..1f) to each vertex
        val textureBitmap: ImageBitmap? = null, // Matched texture asset
        val baseColor: Color,
        val normal: Vector3? = null, // Sensed or calculated face unit normal
        val isDoubleSided: Boolean = false,
        override val depth: Float
    ) : RenderItem3D()
    
    // Spheres, Lines, etc. remain supported
}

// In SceneOptions.kt
data class SceneOptions(
    val renderingStyle: String = "shaded", // shaded, wireframe, lines, textured
    val ambientIntensity: Float = 0.25f,
    val diffuseIntensity: Float = 0.75f,
    val specularIntensity: Float = 0.45f,
    val shininess: Float = 16f,
    val lightDirection: Vector3 = Vector3(0.5f, -1.0f, -0.5f).normalize(), // Directional vector relative to world coord.
    val backfaceCulling: Boolean = true,
    val showGrid: Boolean = true
)
```

### B. Vector Normals & Shading Implementation Functions
```kotlin
// Calculate unit face normal from 3 vertices
fun getFaceNormal(p0: Vector3, p1: Vector3, p2: Vector3): Vector3 {
    val edge1 = p1 - p0
    val edge2 = p2 - p0
    return edge1.cross(edge2).normalize()
}

// Apply Lambert & Phong illumination to base color
fun calculateShadedColor(
    baseColor: Color,
    normal: Vector3,
    rotatedLightDir: Vector3,
    viewDir: Vector3,
    options: SceneOptions
): Color {
    // 1. Ambient lighting coefficient
    val ambient = options.ambientIntensity
    
    // 2. Diffuse shading (Lambertian Cosine law)
    // rotatedLightDir points to the light source
    val diffuseFactor = maxOf(0f, normal.dot(rotatedLightDir))
    val diffuse = diffuseFactor * options.diffuseIntensity
    
    // 3. Specular highlight (Phong Model)
    // R = 2 * (N . L) * N - L
    val reflection = (normal * (2f * normal.dot(rotatedLightDir))) - rotatedLightDir
    val specularFactor = maxOf(0f, reflection.dot(viewDir))
    val specular = if (diffuseFactor > 0f) {
        pow(specularFactor.toDouble(), options.shininess.toDouble()).toFloat() * options.specularIntensity
    } else 0f

    // Mix color components scaling red, green, and blue
    val r = ((baseColor.red * (ambient + diffuse)) + specular).coerceIn(0f, 1f)
    val g = ((baseColor.green * (ambient + diffuse)) + specular).coerceIn(0f, 1f)
    val b = ((baseColor.blue * (ambient + diffuse)) + specular).coerceIn(0f, 1f)
    
    return Color(red = r, green = g, blue = b, alpha = baseColor.alpha)
}
```

### C. Triangulated Affine Texture-Mapping Drawer on Compose Canvas
The following routine solves the affine equations and draws a textured face using Android native paint pipelines inside Compose's `onDraw` phase:

```kotlin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

fun DrawScope.drawTexturedTriangle(
    p0: Offset, p1: Offset, p2: Offset, // Projected screen coordinates
    uv0: Offset, uv1: Offset, uv2: Offset, // UV texture coords mapping
    bitmap: ImageBitmap
) {
    val nativeBitmap = bitmap.asAndroidBitmap()
    val width = nativeBitmap.width.toFloat()
    val height = nativeBitmap.height.toFloat()
    
    // Translate normalized UV coords (0..1) to actual bitmap pixels
    val t0x = uv0.x * width
    val t0y = uv0.y * height
    val t1x = uv1.x * width
    val t1y = uv1.y * height
    val t2x = uv2.x * width
    val t2y = uv2.y * height

    // Calculate Affine Matrix from Texture (u,v) -> Screen (x,y)
    // Formula: [X_screen] = [M_affine] x [U_texture]
    // M = X * U^-1
    val uDet = (t1x - t0x) * (t2y - t0y) - (t2x - t0x) * (t1y - t0y)
    if (abs(uDet) < 0.0001f) return // Avoid divide-by-zero

    val invDet = 1.0f / uDet
    
    // M coefficients:
    val m00 = ((p1.x - p0.x) * (t2y - t0y) - (p2.x - p0.x) * (t1y - t0y)) * invDet
    val m01 = ((p2.x - p0.x) * (t1x - t0x) - (p1.x - p0.x) * (t2x - t0x)) * invDet
    val m02 = p0.x - m00 * t0x - m01 * t0y

    val m10 = ((p1.y - p0.y) * (t2y - t0y) - (p2.y - p0.y) * (t1y - t0y)) * invDet
    val m11 = ((p2.y - p0.y) * (t1x - t0x) - (p1.y - p0.y) * (t2x - t0x)) * invDet
    val m12 = p0.y - m10 * t0x - m11 * t0y

    // Draw triangle utilizing path clip and native matrix paint brush shaders
    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        
        // 1. Specify clipping path corresponding to the projected screen coordinates
        val path = android.graphics.Path().apply {
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }
        
        nativeCanvas.save()
        nativeCanvas.clipPath(path)
        
        // 2. Set up shader and transform matrix
        val shader = android.graphics.BitmapShader(
            nativeBitmap,
            android.graphics.Shader.TileMode.CLAMP,
            android.graphics.Shader.TileMode.CLAMP
        )
        val matrix = android.graphics.Matrix()
        matrix.setValues(floatArrayOf(
            m00, m01, m02,
            m10, m11, m12,
            0f,   0f,   1f
        ))
        shader.setLocalMatrix(matrix)
        
        // 3. Paint on clipped canvas bounds
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            setShader(shader)
        }
        nativeCanvas.drawPath(path, paint)
        nativeCanvas.restore()
    }
}
```

---

## 6. Practical Usage and Interaction Flow

1. **Touch Dragging Orbit:** Dragging in any direction inside the dark cosmic viewport shifts the `yawAngle` and `pitchAngle` values instantly, updating the projection model.
2. **Double Draw Buffering:** All simulation cogs generate their respective polygons dynamically, pipeline-sorting and calculating shading coefficients on every tick of the animation loop.
3. **Lighting Sliders Control:** Users can customize light source direction vectors, ambient light intensity, and specular highlight factors directly from the slider controls to observe shifts in metallic/matte rendering dynamically.
4. **Responsive Side Layout:** Users can switch modes dynamically via the elegant sidebar, scale the drawing size, toggle Y-axis auto-rotation, and tune parameters from sliders without any lagging performance.

---

## 7. GLB (glTF Binary) File Format & Skeletal Animation Architecture

To support third-party animated models dynamically within our Android 3D applet suite without relying on oversized SDKs, we construct a custom, high-durability parsing and render-loop integration roadmap for the **GLB (glTF 2.0 Binary)** container format. This architecture enables the application to load complete 3D polygon structures, map embedded image textures, reconstruct bone-joint skeleton rig hierarchies, and play smooth keyframe animations using custom shaders or standard `Canvas` rendering.

### A. The GLB Binary File layout

A `.glb` file is a single consolidated binary stream containing the glTF asset details, texture image resources, and physical vertex/animation binary arrays. It is subdivided into three physical blocks:

1. **The 12-Byte Header**:
   - `Magic` (4 bytes): Must be exactly `0x46546C67` (ASCII string `"glTF"`).
   - `Version` (4 bytes): Uint32, typically structure version `2` (representing glTF 2.0).
   - `Total Length` (4 bytes): Uint32, total length of the physical file in bytes including headers.

2. **Chunk 0: The JSON Payload (glTF Schema)**:
   - `Length` (4 bytes): Length of the JSON payload.
   - `Type` (4 bytes): Must be exactly `0x4E4F534A` (ASCII string `"JSON"`).
   - `Data`: A UTF-8 string representing the glTF structure (nodes, materials, meshes, cameras, skins, animations, buffers, and accessor mappings).

3. **Chunk 1: The Binary Buffer (BIN)**:
   - `Length` (4 bytes): Length of the binary payload.
   - `Type` (4 bytes): Must be exactly `0x004E4942` (ASCII string `"BIN\0"`).
   - `Data`: Raw byte buffer containing vertex positions, index lists, UV matrices, texture bitmaps, skin weight mappings, and rotational keyframe vectors.

```
┌───────────────────────────────── GLB FILE CONTAINER ────────────────────────────────┐
│  HEADER (12 Bytes)                                                                  │
│  ├─ Magic: "glTF" (0x46546C67)                                                      │
│  ├─ Version: 2                                                                      │
│  └─ Length: Total Bytes                                                            │
├─────────────────────────────────────────────────────────────────────────────────────┤
│  CHUNK 0: JSON SCHEMA (Type: 0x4E4F534A)                                            │
│  └─ Nodes, Mesh Accessors, Materials, Bones/Skins, Animations, Buffers Metadata     │
├─────────────────────────────────────────────────────────────────────────────────────┤
│  CHUNK 1: BINARY CHUNK (Type: 0x004E4942)                                           │
│  └─ Position/Normal/UV arrays [x,y,z], [u,v], Skin weights, Animations, PNG/JPGs    │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

### B. High-Performance Binary Stream Parser

In Kotlin, we bypass manual, memory-heavy stream copying by wrapping the asset input directly as an optimized, zero-copy `ByteBuffer` mapped in native memory.

```kotlin
// In GLBParser.kt
class GLBParser(private val context: Context) {
    
    data class ParsedGLB(
        val vertices: FloatArray,
        val indices: IntArray,
        val uvs: FloatArray?,
        val texture: Bitmap?,
        val jointIndices: IntArray?,
        val jointWeights: FloatArray?,
        val skinHierarchy: SkinNode?,
        val animations: List<GLBAnimation>
    )

    fun parseAsset(filePath: String): ParsedGLB {
        val assetFD = context.assets.openFd(filePath)
        val channel = FileInputStream(assetFD.fileDescriptor).channel
        val byteBuffer = channel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFD.startOffset,
            assetFD.declaredLength
        ).apply { order(ByteOrder.LITTLE_ENDIAN) }

        // 1. Verify Header
        val magic = byteBuffer.int
        if (magic != 0x46546C67) throw IllegalArgumentException("Not a valid GLB asset")
        val version = byteBuffer.int
        val totalLength = byteBuffer.int

        // 2. Parse JSON Chunk
        val jsonLength = byteBuffer.int
        val jsonType = byteBuffer.int
        if (jsonType != 0x4E4F534A) throw IllegalArgumentException("Expected JSON chunk")
        val jsonBytes = ByteArray(jsonLength)
        byteBuffer.get(jsonBytes)
        val jsonString = String(jsonBytes, Charsets.UTF_8)
        val gltfJson = JSONObject(jsonString)

        // 3. Keep reference to BIN chunk offset
        val binLength = byteBuffer.int
        val binType = byteBuffer.int
        if (binType != 0x004E4942) throw IllegalArgumentException("Expected BIN chunk")
        
        // Wrap BIN buffer view bounds inside a custom sub-buffer
        val binOffset = byteBuffer.position()
        byteBuffer.position(binOffset)
        val binBuffer = byteBuffer.slice().order(ByteOrder.LITTLE_ENDIAN)

        return extractEntities(gltfJson, binBuffer)
    }
}
```

---

### C. Texture and Material Extractors

Within the JSON chunk, standard images represent indices in the main database linked to `bufferViews`. We can extract these embedded textures efficiently by reading the mapped buffer limits and decoding the raw memory into Android `Bitmap` matrices.

```kotlin
// In GLBParser.kt (extractEntities scope)
fun extractTexture(gltf: JSONObject, bin: ByteBuffer): Bitmap? {
    if (!gltf.has("images")) return null
    val images = gltf.getJSONArray("images")
    if (images.length() == 0) return null
    
    val targetImage = images.getJSONObject(0)
    val bufferViewIndex = targetImage.getInt("bufferView")
    val bufferViews = gltf.getJSONArray("bufferViews")
    val view = bufferViews.getJSONObject(bufferViewIndex)
    
    val offset = view.getInt("byteOffset")
    val length = view.getInt("byteLength")
    
    // Read directly into memory segment without duplicates
    val rawImageBytes = ByteArray(length)
    bin.position(offset)
    bin.get(rawImageBytes)
    
    // Decode highly-compressed formats (WebP, PNG, JPEG) instantly via native APIs
    val options = BitmapFactory.Options().apply { inMutable = true }
    return BitmapFactory.decodeByteArray(rawImageBytes, 0, length, options)
}
```

---

### D. Skeletal Rigging & Joint Hierarchies (Skins)

Skeletal animations map characters to a skeletal tree. A **Skin** in glTF links individual joint IDs to corresponding internal node matrices.
To construct the dynamic spatial transform $\mathbf{T}_{joint}$, we traverse the skeleton tree from root down to child nodes recursively. Let $\mathbf{L}_{joint}$ be the local TRS (Translation, Rotation, Scale) transformation matrix of a joint, and $\mathbf{P}_{parent}$ be the parent joint transformation matrix relative to the model origin space:

$$\mathbf{G}_{joint} = \mathbf{P}_{parent} \times \mathbf{L}_{joint}$$

To project a bone vertex accurately back into standard joint local coordinates, we apply the **Inverse Bind Matrix** ($\mathbf{B}_{inv}$) defined in the GLB JSON:

$$\mathbf{M}_{joint} = \mathbf{G}_{joint} \times \mathbf{B}_{inv}$$

```kotlin
// In SkinNode.kt
data class SkinNode(
    val id: Int,
    val name: String,
    val parentId: Int?,
    val inverseBindMatrix: Matrix4,
    var localRotation: Quaternion = Quaternion.Identity,
    var localTranslation: Vector3 = Vector3.Zero,
    var localScale: Vector3 = Vector3.One,
    val children: MutableList<SkinNode> = mutableListOf()
) {
    // Traverse parent dependencies and resolve world transforms
    fun resolveWorldMatrix(parentWorld: Matrix4, outputJointMatrices: Array<Matrix4>) {
        val localTRS = Matrix4.fromTRS(localTranslation, localRotation, localScale)
        val worldMatrix = parentWorld * localTRS
        
        // Final joint palette multiplier
        outputJointMatrices[id] = worldMatrix * inverseBindMatrix
        
        for (child in children) {
            child.resolveWorldMatrix(worldMatrix, outputJointMatrices)
        }
    }
}
```

---

### E. Linear Blend Skinning (LBS) Shader Mathematics

To smooth polygon skin bends (e.g., knee joints or shoulder swings), vertices are influenced by up to 4 joints simultaneously. Let:
- $\mathbf{P}_{base}$ be the coordinate position of the vertex in binding pose space.
- $J_0, J_1, J_2, J_3$ be the joint indices assigned to the vertex.
- $w_0, w_1, w_2, w_3$ be the corresponding blend weights satisfying $\sum_{i=0}^3 w_i = 1.0$.

The transformed animated vertex position $\mathbf{P}'$ is defined using **Linear Blend Skinning (LBS)**:

$$\mathbf{P}' = \sum_{i=0}^3 w_i \cdot \left( \mathbf{M}_{J_i} \times \mathbf{P}_{base} \right)$$

This calculation is highly demanding. We outline two execution approaches:
1. **CPU Skinning (Sovereign Canvas Canvas)**: Vertex positions are transformed sequentially inside background threads on the CPU prior to Painter's sorting. Great for lightweight models ($<2000$ vertices) mapping directly to custom Compose drawings.
2. **GPU Vertex Shader (Filament Engine)**: Joint transformation matrices are pushed as shader Uniform values, and the LBS calculation runs inside native hardware shader blocks on every frame tick:
   ```glsl
   #version 300 es
   in vec3 position;
   in vec4 joints;
   in vec4 weights;
   uniform mat4 jointMatrices[64];
   
   void main() {
       mat4 skinMatrix = 
           weights.x * jointMatrices[int(joints.x)] +
           weights.y * jointMatrices[int(joints.y)] +
           weights.z * jointMatrices[int(joints.z)] +
           weights.w * jointMatrices[int(joints.w)];
       gl_Position = projectionView * skinMatrix * vec4(position, 1.0);
   }
   ```

---

### F. Animation Keyframe Tracks & Quaternions Interpolation

Animations in GLB are defined as channels mapping targeted node properties (translation, rotation, or scale) to accessor arrays containing physical timetables.

For any current timestamp $t \in [t_{key, k}, t_{key, k+1}]$:
1. **Ratio factor**: $\tau = \frac{t - t_{key, k}}{t_{key, k+1} - t_{key, k}} \in [0, 1]$
2. **Translation/Scale Interpolation (Linear)**: 
   $$\mathbf{V}_{\text{interp}} = (1 - \tau)\mathbf{V}_k + \tau\mathbf{V}_{k+1}$$
3. **Rotation Interpolation (Spherical Linear - Slerp)**: Rotations are interpolated using **Slerp** inside unit quaternion spheres to eliminate angular speed changes and tumbling errors:
   $$\mathbf{Slerp}(\mathbf{q}_k, \mathbf{q}_{k+1}, \tau) = \frac{\sin((1-\tau)\omega)}{\sin\omega}\mathbf{q}_k + \frac{\sin(\tau\omega)}{\sin\omega}\mathbf{q}_{k+1}$$
   *Where $\cos\omega = \mathbf{q}_k \cdot \mathbf{q}_{k+1}$ represents the dot product of the quaternions.*

```kotlin
// In GLBAnimationChannel.kt
class GLBAnimationChannel(
    val targetNodeId: Int,
    val targetProperty: AnimProperty, // TRANSLATION, ROTATION, SCALE
    val timestamps: FloatArray,
    val keyframes: FloatArray // Flat float arrays [x,y,z] or quaternions [x,y,z,w]
) {
    enum class AnimProperty { TRANSLATION, ROTATION, SCALE }

    fun evaluate(time: Float): StepValue {
        val clampedTime = time.coerceIn(timestamps.first(), timestamps.last())
        var idx = timestamps.binarySearch(clampedTime)
        if (idx < 0) {
            idx = -idx - 2
        }
        idx = idx.coerceIn(0, timestamps.size - 2)
        
        val t0 = timestamps[idx]
        val t1 = timestamps[idx + 1]
        val progress = (clampedTime - t0) / (t1 - t0)

        return when (targetProperty) {
            AnimProperty.TRANSLATION, AnimProperty.SCALE -> {
                val offset0 = idx * 3
                val offset1 = (idx + 1) * 3
                val val0 = Vector3(keyframes[offset0], keyframes[offset0+1], keyframes[offset0+2])
                val val1 = Vector3(keyframes[offset1], keyframes[offset1+1], keyframes[offset1+2])
                StepValue.VecValue(Vector3.lerp(val0, val1, progress))
            }
            AnimProperty.ROTATION -> {
                val offset0 = idx * 4
                val offset1 = (idx + 1) * 4
                val q0 = Quaternion(keyframes[offset0], keyframes[offset0+1], keyframes[offset0+2], keyframes[offset0+3])
                val q1 = Quaternion(keyframes[offset1], keyframes[offset1+1], keyframes[offset1+2], keyframes[offset1+3])
                StepValue.QuatValue(Quaternion.slerp(q0, q1, progress))
            }
        }
    }
}
```

---

### G. Comprehensive Integration Milestones

```
  Milestone 1: Buffer Mapper
    └─ Read GLB Asset binary streams using memory-mapped buffers to prevent native crashes.
  Milestone 2: Schema Parser
    └─ Decode JSON schema details, locating nodes, image bufferViews, and keyframe indexes.
  Milestone 3: Texture Loader
    └─ Extract embedded PNG/WebP images as local Bitmaps and assign to dynamic Canvas paint.
  Milestone 4: Joint Rigging
    └─ Traverse bone hierarchies and multiply inverse bind matrices to resolve joint positions.
  Milestone 5: Animation Channels
    └─ Intercalate scale vectors and interpolate rotations using Spherical Linear (Slerp) math.
  Milestone 6: LBS Blend Core
    └─ Bind bone calculations to custom draw pipelines or push matrix arrays to hardware GPU shaders.
```

---

## 8. Development Roadmap & Next Steps

1. **Local Asset Integration**: Pre-bundle lightweight animated GLB figures (such as low-poly knight heroes and chest traps) in the compile asset directories (`repo-android/app/src/main/assets/models/`).
2. **Caching Texture Shaders**: Cache the constructed `BitmapShader` values and computed inverse bind matrices during level generation to sustain 60 FPS performance.
3. **PBR Shader Attenuation Control**: Extend the custom light sliders on the dynamic UI to let users scale physical roughness offsets, metallic thresholds, and ambient occlusion parameters in real time.

