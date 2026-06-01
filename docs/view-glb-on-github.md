# Documentation: Viewing 3D GLB Files on GitHub

This guide provides instructions on how to use GitHub's built-in interactive 3D model viewer to inspect `.glb` assets and details the integration of the newly registered expressive robot asset in the 🍓 FRAISE workspace.

---

## 🎨 1. GitHub's Native Interactive 3D Viewer

GitHub includes a native, high-performance **WebGL-based 3D renderer** for standard 3D formats, including:
* **Binary glTF (.glb)**
* **ASCII/JSON glTF (.gltf)**
* **STL (.stl)**
* **Wavefront OBJ (.obj)**

When you upload or view these files in a GitHub repository using your web browser, GitHub hosts them with a fully responsive canvas.

### How to View the Model on GitHub
1. Navigate to the repository of your choice.
2. Browse through the file explorer to locating any `.glb` file. For our Android app's local copy, the path is:
   ```text
   repo-android/app/src/main/assets/models/robot_expressive.glb
   ```
3. Click on the file name.
4. GitHub will automatically initiate the **WebGL Canvas** and load the 3D model.

### Viewer Navigation Controls
Once the model is loaded, you can interact with it using these standard mouse and touch gestures:
* **Orbit / Rotate**: Click and drag with the left mouse button (or swipe with one finger on mobile) to inspect the model's geometry and skeletal structure from any angle.
* **Pan**: Click and drag with the right mouse button (or swipe with two fingers) to slide the model horizontally or vertically across the screen.
* **Zoom**: Use the scroll wheel (or pinch-to-zoom gesture on touch devices) to zoom in on complex texture mappings or look deeper at individual nodes.
* **Lighting Angle**: GitHub includes a contextual light vector setting. Click the gear icon or drag with secondary controls to test how diffuse shading shifts across metallic or glossy PBR materials.

---

## 🤖 2. The Expressive Robot Asset (`robot_expressive.glb`)

We have downloaded and integrated a production-ready, rigged 3D avatar within the native Android workspace (`repo-android`).

### Asset Details & Origin
* **Asset Name**: `robot_expressive.glb`
* **Local Workspace Path**: `repo-android/app/src/main/assets/models/robot_expressive.glb`
* **Source URL**: [https://github.com/applh/kaimera/raw/refs/heads/main/app/src/main/assets/robot_expressive.glb](https://github.com/applh/kaimera/raw/refs/heads/main/app/src/main/assets/robot_expressive.glb)
* **Design Features**: Rigged skeletal joints, low-poly count optimized for mobile GPUs, full physically-based rendering (PBR) metallic textures, and pre-packaged keyframe blendshapes for facial/body expressions.

---

## 🛠️ 3. Quick Verification Steps in Sceneview (Fraise)

To validate compiling, parsing, and rendering of files on mobile devices under standard Google Filament configurations, we have integrated a default validator step inside the **World Globe** Applet of our application.

### Validation Walkthrough
1. **Launch the platform**: Open the Android companion applet dashboard.
2. **Access World Globe**: Open the **World Globe** applet (Interactive 3D Virtual globe applet).
3. **Select the preset**: In the bottom control card, locate the **Quick 3D Presets** row.
4. **Trigger Robot Render**: Click the newly registered **Robot** button.
5. **Verify output**:
   * The system cleans the existing Sceneview hierarchy on background threads (`Dispatchers.IO`).
   * The background `ModelLoader` reads `/app/src/main/assets/models/robot_expressive.glb` directly from device memories in zero-copy chunks.
   * A sovereign `ModelNode` is instantiated on the UI thread (`Dispatchers.Main`) and attached to the center coordinates `(0, 0, -2)`, rendering the 3D humanoid robot alone against the high-contrast viewport.
