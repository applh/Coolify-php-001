# Android Draw (Professional-Grade Layer-Based Drawing) Applet Plan

**Objective**: Extend the Android Multi-App Hub by introducing the **Draw** applet (`DrawScreen.kt`, `DrawViewModel.kt`, and SQLite-based drawing project storage). This tool implements a comprehensive, professional-grade drawing studio with GIMP-style layer management, dynamic vector pen strokes, external image sourcing (from local storage or web URL downloaders), customizable project canvas dimensions, and multi-format/multi-size exports.

---

## 1. Architectural Highlights & Tech Stack

To ensure pristine responsiveness, zero-lag brush rendering, and exact layer compositing matching advanced desktop experiences, the applet utilizes standard Jetpack Compose MVVM architecture combined with localized low-impact Android SQLite storage, Kotlin Coroutines, and the hardware-accelerated Compose Canvas drawing scope.

```
                    ┌────────────────────────┐
                    │     DrawViewModel      │
                    └───────────┬────────────┘
                                │ State Flow (Project, Layer Stack, Active Layer, Tools)
                                ▼
                    ┌────────────────────────┐
                    │     DrawScreen UI      │
                    └───────────┬────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│ Compose      │        │  Draw Project│        │ HttpUrlConnection
│ Canvas &     │        │  Database    │        │ Web Image    │
│ DrawScope    │        │  Helper      │        │ Downloader   │
│ (Path/Bitmap)│        │ (Layers/CRUD)│        │ (Disk/Cache) │
└──────────────┘        └──────────────┘        └──────────────┘
```

- **Compositing State Model**: Draw projects are composed of ordered layers. Drawing actions are performed on the currently selected layer. Layers can hold vector pen strokes or standard raster background images, rendering sequentially from bottom to top using custom canvas paint blending.
- **Project Database**: A dedicated local SQL database tracking `draw_projects`, `draw_layers`, and `draw_strokes` to capture canvas metadata, layer stack arrangements, and coordinate arrays so work is safely persisted across sessions.
- **Asynchronous Sourcing Engine**: URL input initiates secure background downloads (utilizing Kotlin's standard async Coroutines) to fetch web images, process them safely into memory-efficient Bitmaps, and cache them inside the app sandbox before adding them as backgrounds or secondary project layers.
- **Performance Optimizations**: Drawing trajectories are smooth-interpolated via Bezier curve mathematical formulations (`Path.quadraticTo`) to eliminate standard linear discretization artifacts. Canvas panning/zooming leverages Compose pointer gesture filters without dirtying coordinate vector matrices.

---

## 2. Core Features (The Requested Scope)

### A. Professional GIMP-like Layer Management System
- **Layer Stack Interface**: A dedicated floating sidebar or sliding drawer displaying the current ordered layers with micro-thumbnails, visible toggles, and customized user-facing names.
- **Layer Properties Panel**:
  - *Dynamic Opacity*: Independent alpha slider (0.0f to 1.0f) mapping to Jetpack Compose layers.
  - *Visibility State*: An eye toggle icon to instantly hide/unhide target layers from the composite rendering.
  - *Layer Lock*: A lock toggle to prevent accidental editing or translation of finalized layers.
  - *Blend Modes*: Dedicated selector specifying compositing rules per-layer:
    - `Normal` (standard alpha over)
    - `Multiply` (darkens values)
    - `Screen` (brightens values)
    - `Overlay` (boosts contrast)
    - `Plus` / `Addition` (linear color integration)
- **Layer Manipulation Utilities**:
  - *Reordering*: Shift layers up or down standard stack positions dynamically.
  - *Creation & Duplication*: Sprout fresh blank layers or clone existing bitmap and vector layers with identical attributes.
  - *Flatten / Merge*: Merge down an active layer onto its lower companion, or flatten the entire stack into a standard singular raster layer.

### B. High-Fidelity Drawing Engine & Tools
- **Precision Brushes**:
  - *Pen / Pencil Mode*: Standard hard-edge solid vector path rendering.
  - *Brush Mode*: Smooth-edge anti-aliased path strokes simulating calligraphy markers.
  - *Eraser Mode*: Targeted alpha-erasure using standard PorterDuff `DST_OUT` compositing to clear details on the selected active layer.
- **Vector Shape Drawers**: Clickable overlay selectors to dynamically construct symmetrical geometric overlays: Straight Lines, Concentric Rectangles / Squares, Ovals / Circles, and Highlight Arrows.
- **Brush Thickness & Dynamic Color Picker**:
  - Thickness adjustments via an intuitive slider showing exact real-time diameter previews on a hover card.
  - Comprehensive Color Palette: Visual grid of pre-seeded swatches, standard RGB/HSV color field sliders, and direct custom Hexadecimal format inputs (e.g., `#FF4433`).

### C. Unified Background Sourcing
- **Blank Project Initialization**: Prompt the user to configure core canvas dimensions (e.g., 1080x1080 Square, 1080x1920 Phone aspect, 1920x1080 HD Layout) and initialize with a solid selection base fill.
- **Local Device Sourcing**: Integrate standard Android storage file-picker selectors via `ActivityResultContracts.GetContent` and `ACTION_OPEN_DOCUMENT`. Scrapes permission-safe local files to spawn a new raster layer matching canvas aspect layouts.
- **Internet URL Downloader Engine**: An intuitive downloader dialogue card that allows inputting web image links (e.g., HTTPS addresses). Fetches source data cleanly using an asynchronous Coroutines task stream, displays dynamic loader spinner rings, decompresses streams safely into memory, and inserts the result as an active background layer.

### D. Multi-Format & Multi-Size Saving Suite
- **Interactive Compile Drawer**:
  - *Output Formats*: Export composite canvases to lossless **PNG**, space-efficient **JPEG** (featuring a 0-100% quality compression bounds controller), or modern **WEBP**.
  - *Output Dimension Scaling*: Allow users to select output size options before rendering:
    - `1.0x` (Current Canvas Aspect)
    - `2.0x / HD` (Double Resolution upscaling)
    - `4.0x / UHD` (Studio Master upscaling)
    - `Custom Resolution` (Direct height/width text field parameter override with aspect lock toggle)
- **Local Sandbox & Scoped Storage Media Saving**:
  - Safely record file buffers under standard Android cache blocks or export to the user's phone Gallery using Android `ContentValues` and `MediaStore`.

---

## 3. UI / UX Layout & Composition

The layout leverages a full-screen optimized drawing orientation featuring floating contextual widgets and quick side drawer configurations to maximize useable canvas space.

```
┌────────────────────────────────────────────────────────┐
│ 🖌️ DRAW STUDIO      [📁 Import]  [💾 Export]  [≡ Layers]│
├────────────────────────────────────────────────────────┤
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │                                                  │  │
│  │                                                  │  │
│  │                  DRAW CANVAS AREA                │  │
│  │                (Pinch to Zoom/Pan)               │  │
│  │                                                  │  │
│  │                                                  │  │
│  │                                                  │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
├────────────────────────────────────────────────────────┤
│  Layer List Overlay [≡ Layers Clicked]:                │
│  ┌──────────────────────────────────────────────────┐  │
│  │ [👁️] Layer 2: Pen Overlay      [🔒] [Opacity: 80%]│  │
│  │ [👁️] Layer 1: Background Image  [🔓] [Opacity: 100%]│  │
│  │  [➕ Add Layer]    [🗑️ Delete]     [▲ Up]   [▼ Down]  │  │
│  └──────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────┤
│  Tools:   [✏️ Pen]  [🖌️ Brush]  [🧼 Erase]  [📐 Shape]   │
├────────────────────────────────────────────────────────┤
│  Brush:   ( Size: 12dp )  [● Color Picker Swatch]     │
└────────────────────────────────────────────────────────┘
```

---

## 4. Technical Implementation Blueprint

### A. Domain Layer and Composite Models
`DrawLayer.kt` represents the structural properties of each individual layer in a multi-layer project stack:

```kotlin
package com.example.cameraxapp.draw

import android.graphics.Bitmap
import android.graphics.Path
import androidx.compose.ui.graphics.BlendMode

enum class LayerType {
    VECTOR_INK,  // Holds dynamic pen paths
    RASTER_IMAGE // Holds loaded bitmap background or web assets
}

data class DrawnStroke(
    val path: Path,
    val color: Int,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

data class DrawLayer(
    val id: String,
    var name: String,
    val type: LayerType,
    var isVisible: Boolean = true,
    var isLocked: Boolean = false,
    var opacity: Float = 1.0f,
    var blendMode: BlendMode = BlendMode.SrcOver,
    // Layer Data Components
    var bitmap: Bitmap? = null, // Set if LayerType is RASTER_IMAGE
    val strokes: MutableList<DrawnStroke> = mutableListOf() // Set if LayerType is VECTOR_INK
) {
    fun duplicate(): DrawLayer {
        val dupStrokes = strokes.map { it.copy() }.toMutableList()
        val dupBitmap = bitmap?.copy(bitmap?.config ?: Bitmap.Config.ARGB_8888, true)
        return DrawLayer(
            id = java.util.UUID.randomUUID().toString(),
            name = "$name (Copy)",
            type = type,
            isVisible = isVisible,
            isLocked = isLocked,
            opacity = opacity,
            blendMode = blendMode,
            bitmap = dupBitmap,
            strokes = dupStrokes
        )
    }
}
```

### B. Core Canvas Rendering State and Management
`DrawViewModel.kt` handles layer actions, path calculations, URL image downloads, and projects compiling:

```kotlin
package com.example.cameraxapp.draw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.BlendMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class DrawViewModel : ViewModel() {

    private val _layers = mutableStateListOf<DrawLayer>()
    val layers: List<DrawLayer> get() = _layers

    private val _activeLayerId = mutableStateOf<String?>(null)
    val activeLayerId: State<String?> = _activeLayerId

    private val _isDownloading = mutableStateOf(false)
    val isDownloading: State<Boolean> = _isDownloading

    init {
        // Initialize with default background layer and ink layer
        resetProject(1080, 1080)
    }

    fun resetProject(width: Int, height: Int) {
        _layers.clear()
        
        // Lower Background Color Layer
        val bgLayer = DrawLayer(
            id = "bg_layer",
            name = "Background Fill",
            type = LayerType.RASTER_IMAGE,
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.WHITE)
            }
        )
        // Primary Vector Drawing Layer
        val drawingLayer = DrawLayer(
            id = "draw_layer",
            name = "Vector Ink Layer",
            type = LayerType.VECTOR_INK
        )
        
        _layers.add(bgLayer)
        _layers.add(drawingLayer)
        _activeLayerId.value = drawingLayer.id
    }

    fun getActiveLayer(): DrawLayer? {
        return _layers.find { it.id == _activeLayerId.value }
    }

    fun selectLayer(id: String) {
        _activeLayerId.value = id
    }

    fun addBlankLayer() {
        val activeCount = _layers.count { it.type == LayerType.VECTOR_INK }
        val newLayer = DrawLayer(
            id = java.util.UUID.randomUUID().toString(),
            name = "Ink Layer ${activeCount + 1}",
            type = LayerType.VECTOR_INK
        )
        _layers.add(newLayer)
        _activeLayerId.value = newLayer.id
    }

    fun deleteActiveLayer() {
        if (_layers.size > 1 && _activeLayerId.value != null) {
            val idx = _layers.indexOfFirst { it.id == _activeLayerId.value }
            _layers.removeAt(idx)
            val newActiveIdx = if (idx >= _layers.size) _layers.size - 1 else idx
            _activeLayerId.value = _layers[newActiveIdx].id
        }
    }

    fun moveLayerUp(id: String) {
        val index = _layers.indexOfFirst { it.id == id }
        if (index != -1 && index < _layers.size - 1) {
            val element = _layers.removeAt(index)
            _layers.add(index + 1, element)
        }
    }

    fun moveLayerDown(id: String) {
        val index = _layers.indexOfFirst { it.id == id }
        if (index > 0) {
            val element = _layers.removeAt(index)
            _layers.add(index - 1, element)
        }
    }

    /**
     * Downloads an image from the web and inserts it as a background Layer.
     */
    fun downloadAndAddImageLayer(imageUrl: String, context: Context) {
        viewModelScope.launch {
            _isDownloading.value = true
            val downloadedBitmap = withContext(Dispatchers.IO) {
                try {
                    val url = URL(imageUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connectTimeout = 10000
                    connection.readTimeout = 15000
                    connection.connect()
                    
                    val input: InputStream = connection.inputStream
                    BitmapFactory.decodeStream(input)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            
            _isDownloading.value = false
            if (downloadedBitmap != null) {
                val newRasterLayer = DrawLayer(
                    id = java.util.UUID.randomUUID().toString(),
                    name = "Web Background Overlay",
                    type = LayerType.RASTER_IMAGE,
                    bitmap = downloadedBitmap
                )
                // Add downloaded image layer above the active background layer
                _layers.add(1, newRasterLayer)
                _activeLayerId.value = newRasterLayer.id
            }
        }
    }

    fun addLocalImageLayer(bitmap: Bitmap) {
        val newRasterLayer = DrawLayer(
            id = java.util.UUID.randomUUID().toString(),
            name = "Imported Layer",
            type = LayerType.RASTER_IMAGE,
            bitmap = bitmap
        )
        _layers.add(newRasterLayer)
        _activeLayerId.value = newRasterLayer.id
    }
}
```

### C. GIMP-Style Layer Canvas Compositer Rule Architecture
To produce high-quality pixel composite combinations resembling GIMP or professional graphics software, compiling combines nested layer transparency values with specific Blend Modes sequentially into an output bitmap:

```kotlin
fun compileCompositeImage(
    layers: List<DrawLayer>,
    width: Int,
    height: Int,
    scaleFactor: Float = 1.0f
): Bitmap {
    val scaledWidth = (width * scaleFactor).toInt()
    val scaledHeight = (height * scaleFactor).toInt()

    // Create target canvas bitmap frame
    val compositeBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(compositeBitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    for (layer in layers) {
        if (!layer.isVisible) continue

        // Track layers configurations
        paint.alpha = (layer.opacity * 255).toInt()
        paint.xfermode = when (layer.blendMode) {
            BlendMode.SrcOver -> android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_OVER)
            BlendMode.Multiply -> android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY)
            BlendMode.Screen -> android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN)
            BlendMode.Plus -> android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.ADD)
            BlendMode.Overlay -> android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.OVERLAY)
            else -> android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_OVER)
        }

        when (layer.type) {
            LayerType.RASTER_IMAGE -> {
                layer.bitmap?.let { sourceBitmap ->
                    val srcRect = android.graphics.Rect(0, 0, sourceBitmap.width, sourceBitmap.height)
                    val dstRect = android.graphics.Rect(0, 0, scaledWidth, scaledHeight)
                    canvas.drawBitmap(sourceBitmap, srcRect, dstRect, paint)
                }
            }
            LayerType.VECTOR_INK -> {
                // Compile vector pen path geometries
                for (stroke in layer.strokes) {
                    val pathPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = stroke.strokeWidth * scaleFactor
                        color = stroke.color
                        alpha = ((layer.opacity * (if (stroke.isEraser) 1.0f else (android.graphics.Color.alpha(stroke.color) / 255f))) * 255).toInt()
                        if (stroke.isEraser) {
                            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
                        }
                    }
                    
                    val scaleMatrix = android.graphics.Matrix().apply {
                        postScale(scaleFactor, scaleFactor)
                    }
                    val scaledPath = Path(stroke.path)
                    scaledPath.transform(scaleMatrix)
                    canvas.drawPath(scaledPath, pathPaint)
                }
            }
        }
    }
    return compositeBitmap
}
```

### D. SQLite Layered Projects Backup Helpers
`DrawDatabaseHelper.kt` tracks metadata metrics and lets users write layered backups:

```kotlin
package com.example.cameraxapp.draw

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DrawDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Project Catalog Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS draw_projects (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                width INTEGER NOT NULL,
                height INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                modified_at INTEGER NOT NULL
            )
        """)

        // GIMP Layer Stack Mapping Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS draw_layers (
                id TEXT PRIMARY KEY,
                project_id TEXT NOT NULL,
                name TEXT NOT NULL,
                layer_type TEXT NOT NULL,
                stack_index INTEGER NOT NULL,
                opacity REAL NOT NULL,
                is_visible INTEGER NOT NULL,
                is_locked INTEGER NOT NULL,
                blend_mode TEXT NOT NULL,
                local_bitmap_path TEXT, // Path to stored layer binary
                FOREIGN KEY(project_id) REFERENCES draw_projects(id) ON DELETE CASCADE
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    companion object {
        private const val DATABASE_NAME = "draw_studio_projects.db"
        private const val DATABASE_VERSION = 1
    }
}
```

---

## 5. Rollout Phases

1. **Phase 1: Composite Architecture & Data Modeling**
   - Create core `DrawLayer`, `DrawnStroke`, and `LayerType` objects.
   - Craft the core layer compiler arithmetic merging complex blend modes natively.

2. **Phase 2: Canvas Controls & Dynamic Gesture Systems**
   - Build out the custom Compose Canvas responsive view.
   - Configure multitouch zoom-scaling and pan-translation matrices mapping coordinates smoothly during pointer inputs.
   - Implement shape calculators and freehand Bezier brush paths.

3. **Phase 3: GIMP-Style Layer Control List Component**
   - Code out the ordered right-hand or left-hand drawer containing thumbnails, visibility eye icons, locked states, blend dropdowns, and stack movers.
   - Ensure clear selection rings isolate the currently highlighted drawing targets.

4. **Phase 4: Unified Loading Engine (Storage Files & Async Web URL)**
   - Build the download worker leveraging Kotlin Coroutines to isolate web inputs cleanly and inject them safely as project assets.
   - Construct standard Android intent dispatchers opening phone galleries.

5. **Phase 5: Custom Sized Compiler Exports & Database Saves**
   - Define export options rendering canvases to high-resolution master formats.
   - Set up standard SQLite database structures saving active project states to ensure offline integrity.
