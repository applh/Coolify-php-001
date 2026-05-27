# Android Image Selection, Size Reduction & PDF Compiler Plan

This document details the architectural, technical, and implementation plan for adding a professional-grade **Image Selection, Multi-Option File Size Reduction, and PDF Document Compiler** feature suite to `repo-android`.

---

## 1. Architectural Strategy: New Applet vs. Files Applet Integration

To offer the most seamless, optimized, and cohesive user experience, we weigh two core architectural configurations:

### Option A: Create a Dedicated "Image & PDF" Applet
- **Advantages**: Complete isolation from other applets; dedicated workspace purely optimized for conversion.
- **Disadvantages**: Introduces launcher clutter; forces the user to exit their File Explorer and re-navigate visual hierarchies; creates a disconnected "sandboxed imports" flow.

### Option B: Upgrade & Consolidate inside the "Files" Applet (RECOMMENDED)
* **Advantages**: 
  - **Eliminates Launcher Clutter**: Keeps the dashboard unified in a lean, single-purpose structure.
  - **Contextual In-Place Action Flow**: Allows users to select files *already existing* in their local sandboxed partitions (e.g. decrypted images inside the Vault, files in `/Pictures`) and immediately process them via context actions (e.g. long-press or tap "More" -> "Compress" or "Compile to PDF").
  - **Reduces UI Copy Friction**: No need to duplicate photo selection routines. The file explorer's rich thumbnail grid, path details, categorization, and sorting are reused completely.
  - **Workflow B Dual Access**: Still provides a dedicated workspace for system-wide files! We can add a specialized "Media Toolkit" button in the Files main drawer or header, opening a standalone compiler frame with access to the system visual picker (`PickMultipleVisualMedia`).
- **Disadvantages**: Moderate increase in the `ExplorerScreen.kt` navigation graph, easily isolated into structured modular sub-views or helper classes.

### Architectural Decision Statement
We choose **Option B (Consolidate into the "Files" Applet)**. This ensures that users get the best of both worlds:
1. **The Context-First Action**: Process existing local files directly from file lists.
2. **The Workspace-First Tool**: A designated toolbox nested naturally in the Files layout to import external files via standard Contracts, configure scales, predict file-size benefits, and run operations in clean modern screens.

---

## 2. Dynamic End-User Workflows

By upgrading the Files applet, we introduce a dual-channel modern workflow.

### Workflow A: Contextual In-Place Conversions
1. **Browsing**: User browses files in the standard "Files" explorer, filtered to **Images** or viewing all directories.
2. **Selection**: User activates standard multi-select mode (e.g. combined-clickable long-press gestures) and highlights 1 or more images.
3. **Context Action Sheet**: A persistent action bar animatedly slides in at the bottom.
4. **Operations Selection**: User taps **"Compress Size"** or **"Compile to PDF"**:
   - *Compress Size*: Opens a lightweight bottom-sheet dialog within the file viewer to dial in target format (JPEG, PNG, WebP), Scale Slider, and Quality Slider. Clicking "Confirm" runs the job on local background coroutines and refreshes the file grid immediately.
   - *Compile to PDF*: Opens a lightweight popup requesting page size configs (A4, Letter, Original) and file-name formatting. Refreshes the local `Documents/` repository instantly upon successful completion.

### Workflow B: Standalone "Media Toolkit" Dashboard Workspace
1. **Access**: User swiping opens the Files side navigation drawer or clicks on the header "Toolkit" icon to access the **Media & Document Tools Workspace**.
2. **Import external assets**: User clicks "Select Source Images", triggering the modern `PickMultipleVisualMedia` contract. Selected image files display inside an elegant horizontal queue.
3. **Queue Sorting/Ordering**: User can easily long-press and drag-reorder images or delete single item items prior to compilation.
4. **Parameters fine-tuning**:
   - **Quality Slider**: Set quality scale from 1% to 100%. Shows live prediction of storage byte reductions.
   - **Dimension Scale**: Set pixel scaling from 10% to 100% preserving aspect ratios.
   - **Target Format Transmuxer**: Transmux on the fly to JPEG, PNG, or WebP.
5. **Execution**: Dynamic progress indicators with an overlay shimmer play while the native, zero-dependency `PdfCompilationEngine` runs under `Dispatchers.Default`.
6. **Unified Actions Feed Log**: Formats output logs at the bottom of the toolkit workspace (e.g. showing processing times and final output sizes).

---

## 3. High-Level Component Layout Configuration

```
               +------------------------------------------------------+
               |               Standard Drawer Launcher               |
               +------------------------------------------------------+
                                          |
                                          v
               +------------------------------------------------------+
               |              Integrated Files Applet                 |
               +--------------------------+---------------------------+
                                          |
                +-------------------------+-------------------------+
                |                                                   |
                v                                                   v
  +---------------------------+                        +---------------------------+
  |    Files Explorer Site    |                        |   Media Toolkit Workspace |
  | (Symmetric Grid / Lists)  |                        | (Activity Result Pickers) |
  +-------------+-------------+                        +-------------+-------------+
                |                                                   |
                | (Multi-Selected Items Folder Paths)               | (picked system media lists)
                v                                                   v
  +--------------------------------------------------------------------------------+
  |                               Files ViewModel                                  |
  +---------------------------------------+----------------------------------------+
                                          |
                     +--------------------+--------------------+
                     |                                         |
                     v                                         v
        +----------------------------+            +----------------------------+
        |     ImageReducerEngine     |            |    PdfCompilationEngine    |
        |  (In-Place bitmap recycles) |            |  (Native Platform Canvas)  |
        +-------------+--------------+            +-------------+--------------+
                      |                                         |
                      v                                         v
        +-------------+-----------------------------------------+--------------+
        |                 Clean Storage Subdirectories                         |
        |      - /Pictures/Compressed (WebP, JPG outputs)                       |
        |      - /Documents/CompiledPDFs (Clean Standardized PDF structures)    |
        +----------------------------------------------------------------------+
```

---

## 4. High-Performance Image Compression & Resizing Engine

We safeguard local hardware caches and prevent Out Of Memory (OOM) alerts. We do this by employing **in-place Bitmap sampling**, cooperative Kotlin coroutines, and custom canvas color recycles.

Below is the type-safe Kotlin API design for the image reducer engine:

```kotlin
package com.example.cameraxapp.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageReducerEngine {

    enum class OutputFormat { JPEG, PNG, WEBP_LOSSY, WEBP_LOSSLESS }

    data class CompressionConfig(
        val scalePercent: Float = 1.0f, // 0.1f - 1.0f
        val quality: Int = 80,          // 1 - 100
        val targetFormat: OutputFormat = OutputFormat.JPEG
    )

    data class CompressionResult(
        val outputFilePath: String,
        val originalSize: Long,
        val compressedSize: Long,
        val savedPercent: Float
    )

    /**
     * Resizes and compresses an image securely on Dispatchers.IO.
     * Implements down-sampling checks to prevent OutOfMemory crashes on high-res hardware.
     */
    suspend fun compressImage(
        context: Context,
        sourceUri: Uri,
        config: CompressionConfig,
        outputDirectory: File
    ): CompressionResult = withContext(Dispatchers.IO) {
        val originalSize = getUriSize(context, sourceUri)
        
        // 1. Decode bounds first to analyze raw dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(sourceUri).use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        // Calculate dynamic down-sampling factor for extremely large imagery
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, 2048, 2048)
        options.inJustDecodeBounds = false

        // 2. Decode the resized input stream
        val sampledBitmap = context.contentResolver.openInputStream(sourceUri).use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalStateException("Unable to decode bitmap stream")

        // 3. Process fractional scale resizing if applicable
        val finalBitmap = if (config.scalePercent < 1.0f) {
            val targetWidth = (sampledBitmap.width * config.scalePercent).toInt().coerceAtLeast(1)
            val targetHeight = (sampledBitmap.height * config.scalePercent).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true)
            if (scaled != sampledBitmap) {
                sampledBitmap.recycle() // Clean the intermediate bitmap memory immediately
            }
            scaled
        } else {
            sampledBitmap
        }

        // 4. Create the target file paths
        val extension = when (config.targetFormat) {
            OutputFormat.JPEG -> "jpg"
            OutputFormat.PNG -> "png"
            OutputFormat.WEBP_LOSSY, OutputFormat.WEBP_LOSSLESS -> "webp"
        }
        
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
        val outputFile = File(outputDirectory, "RED_${UUID.randomUUID()}.$extension")

        // 5. Compress and write to disk
        val format = interstateFormat(config.targetFormat)

        FileOutputStream(outputFile).use { outStream ->
            finalBitmap.compress(format, config.quality, outStream)
        }

        // Clean heap allocations
        finalBitmap.recycle()

        val compressedSize = outputFile.length()
        val savedPercent = if (originalSize > 0) {
            (1.0f - (compressedSize.toFloat() / originalSize.toFloat())) * 100f
        } else {
            0f
        }

        CompressionResult(
            outputFilePath = outputFile.absolutePath,
            originalSize = originalSize,
            compressedSize = compressedSize,
            savedPercent = savedPercent
        )
    }

    private fun interstateFormat(targetFormat: OutputFormat): Bitmap.CompressFormat {
        return when (targetFormat) {
            OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG
            OutputFormat.WEBP_LOSSY -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
            OutputFormat.WEBP_LOSSLESS -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getUriSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                it.length
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
```

---

## 5. Zero-Dependency PDF Compilation Pipeline

By leveraging the Android Framework’s built-in `PdfDocument` engine, we avoid adding heavy, error-prone external third-party native binders. 
The implementation renders sequentially ordered visual bitmaps matching correct layout metrics safely on custom page dimensions.

```kotlin
package com.example.cameraxapp.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PdfCompilationEngine {

    enum class PageSize { A4, LETTER, ORIGINAL_IMAGE_SIZE }
    enum class PageOrientation { PORTRAIT, LANDSCAPE }

    data class PdfConfig(
        val pageSize: PageSize = PageSize.A4,
        val orientation: PageOrientation = PageOrientation.PORTRAIT,
        val marginPixels: Int = 36 // ~0.5 inch under standard 72-points-per-inch PDF formats
    )

    /**
     * Chronologically compiles multiple selected image URIs into a multi-page PDF document.
     * Prevents OOM by handling dynamic canvas matrix transforms and in-place bitmap recycling.
     */
    suspend fun compileImagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        config: PdfConfig,
        outputDirectory: File
    ): File = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) throw IllegalArgumentException("Selected images file list cannot be empty")
        
        val pdfDocument = PdfDocument()

        // PDF coordinate systems scale in standard 72 points per inch boundaries:
        // A4: 595 x 842 points | Letter: 612 x 792 points
        val baseWidth = if (config.pageSize == PageSize.LETTER) 612 else 595
        val baseHeight = if (config.pageSize == PageSize.LETTER) 792 else 842

        val targetWidth = if (config.orientation == PageOrientation.LANDSCAPE) baseHeight else baseWidth
        val targetHeight = if (config.orientation == PageOrientation.LANDSCAPE) baseWidth else baseHeight

        try {
            imageUris.forEachIndexed { index, uri ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2 // Memory safeguard for large photo compiler layouts
                }
                
                val sourceBitmap = context.contentResolver.openInputStream(uri).use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: return@forEachIndexed

                val pageW = if (config.pageSize == PageSize.ORIGINAL_IMAGE_SIZE) sourceBitmap.width else targetWidth
                val pageH = if (config.pageSize == PageSize.ORIGINAL_IMAGE_SIZE) sourceBitmap.height else targetHeight

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Calculate scaling factor to safely fit inside margins
                val usableW = pageW - (config.marginPixels * 2)
                val usableH = pageH - (config.marginPixels * 2)

                val scaleX = usableW.toFloat() / sourceBitmap.width.toFloat()
                val scaleY = usableH.toFloat() / sourceBitmap.height.toFloat()
                val scale = Math.min(scaleX, scaleY).coerceAtMost(1.0f)

                val drawW = (sourceBitmap.width * scale).toInt()
                val drawH = (sourceBitmap.height * scale).toInt()

                // Symmetrical aligning coordinates
                val startX = config.marginPixels + (usableW - drawW) / 2
                val startY = config.marginPixels + (usableH - drawH) / 2

                val destRect = Rect(startX, startY, startX + drawW, startY + drawH)
                
                canvas.drawBitmap(sourceBitmap, null, destRect, null)
                pdfDocument.finishPage(page)
                
                sourceBitmap.recycle()
            }

            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs()
            }

            val pdfFile = File(outputDirectory, "DOC_${UUID.randomUUID()}.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfFile

        } finally {
            pdfDocument.close()
        }
    }
}
```

---

## 6. Layout Views & Interactivity Designs

Instead of creating complete multi-screen file overheads, we craft these screens in a modular way with smooth state transitions:

1. **Horizontal Action Sheet Layout**:
   - Anchors perfectly inside the existing `ExplorerScreen.kt` using sleek slide-up animations.
   - Shows icons for **[🎨 Compress]** and **[📄 PDF Link]** inside an elegant high-contrast row.
   - Touching buttons launches structured, custom overlay Sheets (reusing local notifications to avoid `window.alert` blockings).

2. **Standalone "Toolkit" Layout Screen**:
   - Fully loaded inside `src/main/java/com/example/cameraxapp/media/ImagePdfScreen.kt`.
   - Connected seamlessly through the main left-side menu navigation drawer.
   - Hosts clean slider controls, visual thumbnail list boxes, live bytes estimator chips (exposing real savings potential), and a text activity terminal tracing step-by-step compression details.

---

## 7. Storage, Sandbox, and FileProviders

To assure robust security compliance with Android Scoped Storage:

- **Saved Directories**:
  - Image files: `/Pictures/Compressed/` inside private external directory arrays.
  - Documents: `/Documents/CompiledPDFs/` within isolated scoped application blocks.
- **Grant System Permissions (Shared URIs)**:
  - We leverage `androidx.core.content.FileProvider` directly to securely grant exposure permissions. This enables other installed apps (Mail providers, Messaging sites, System viewers) to preview compiled results without compromising application safety boundaries.

---

## 8. Staged Rollout Timeline & Progression

We execute the implementation cleanly across 5 structured phases:

- **Phase 1: Dual-Core Binding & PhotoPicker Inputs** (Complete layout structures of both contextual dialog actions and the standalone toolbox side drawer path).
- **Phase 2: Core `ImageReducerEngine` Coding** (Write the custom bitmap matrix scaler and downsampler, enforcing strict zero-dependency metrics).
- **Phase 3: Native `PdfCompilationEngine` Coding** (Develop custom margin-fitting coordinates systems, layout grids, and multi-page compiler loops).
- **Phase 4: Feedback Loops & UI Wiring** (Integrate progress tracking, state shimmers, files view refresh triggers, and file providers).
- **Phase 5: Automated Verification** (`compile_applet` confirmation, lint checks, and complete visual inspection).
