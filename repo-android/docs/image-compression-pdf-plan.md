# Android Image Selection, Size Reduction & PDF Compiler Plan

This document details the architectural, technical, and implementation plan for adding a professional-grade **Image Selection, Multi-Option File Size Reduction, and PDF Document compiler** feature suite to `repo-android`.

Our goals are to implement high-performance image processing pipelines with a 0KB third-party dependency footprint, ensuring maximum performance, zero memory leaks, and seamless multi-image compiler workflows entirely in Jetpack Compose, Kotlin Coroutines, and the native Android SDK.

---

## 1. Architectural Goals & Core Requirements

1. **System-Level Image Selection**:
   - Utilize modern, standardized Jetpack activity-result contracts (`PickMultipleVisualMedia`) to safely query the local device storage.
   - Support arbitrary selection bounds (dynamic list limits, drag-reorder lists, item subtraction).

2. **Advanced, Fine-Grained File Size Reduction Options**:
   - **Quality Lossy Compression**: Sliding scale quality compression (JPEG & Lossy WebP) from 1% to 100%.
   - **Scale Resizing**: Fractional scale scaling (e.g., 25%, 50%, 75%, 100% boundary downsizes) preserving original aspect ratios.
   - **Format Transmuxing**: Ability to convert raw heavy PNGs, BMPs, or HEIC files dynamically into optimized JPEG or lossless/lossy WebP files on disk.

3. **High-Fidelity Image-To-PDF Compilation Pipeline**:
   - Stream sequentially ordered selected images into sequential system PDF pages.
   - Native platform graphics pipeline rendering through `android.graphics.pdf.PdfDocument`.
   - Multi-option canvas fits: "Stretch to Fit Letter/A4", "Auto/Match Native Image Bounds", or "Symmetrical Margin Padding (0.5 inch / 1.0 inch)".
   - Multi-threaded rendering using `Dispatchers.Default` background processing arrays, preventing any UI freeze or JANK.

---

## 2. Component Block Diagram

```
                       +-----------------------------------+
                       |    Jetpack Activity Launcher      |
                       |  ActivityResultContracts.Picker   |
                       +-----------------+-----------------+
                                         |
                                         v (Uri Selection List)
+----------------------------------------+---------------------------------------+
|  [ImagePdfScreen] - Main Jetpack Compose Interface Layout Frame                 |
+--------------------------------------------------------------------------------+
|  1. Horizon Gallery List (Selected Images Carousel, Thumbnails, Reorder Buttons)|
|  2. Compression Parameter Panel (Sliders, Format Radio Selectors, Dimension Scale) |
|  3. Compilation Actions Footer (Process Compression | Compile PDF Documents)     |
+----------------------------------------+---------------------------------------+
                                         |
                       +-----------------+-----------------+
                       |       ImagePdfViewModel          |
                       +--------+-----------------+--------+
                                |                 |
                                v                 v
       +------------------------+---+         +---+------------------------+
       |   ImageReducerEngine      |         |    PdfCompilationEngine    |
       |  (Coroutines & Bitmaps)    |         |    (Native PdfDocument)    |
       +------------+---------------+         +------------+---------------+
                    |                                      |
                    v (Processed Compressed JPG/WebP)     v (High-Fidelity PDF Output)
       +------------+--------------------------------------+------------+
       |             Scoped Storage Target Location Paths                  |
       |     (Pictures/Compressed/  |  Documents/CompiledPDFs/)            |
       +-------------------------------------------------------------------+
```

---

## 3. High-Performance Image Compression & Resizing Engine

We avoid main-thread performance degradation and out-of-memory (OOM) anomalies by employing **in-place Bitmap sampling**, cooperative Kotlin coroutines, and automated canvas color recycles.

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
import java.io.InputStream
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
        val format = when (config.targetFormat) {
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

## 4. Zero-Dependency PDF Compilation Pipeline

By leveraging the Android Framework’s built-in `PdfDocument` engine, we avoid loading heavy, error-prone external native library binders. The engine iterates through the selected layout lists, samples images to target page bounds dynamically, and draws them inside standardized page canvases.

```kotlin
package com.example.cameraxapp.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
        val marginPixels: Int = 36 // ~0.5 inch under standard 72-dpi document formats
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

        // Page sizes configurations under standard 72 points per inch rule:
        // A4: 595 x 842 points | Letter: 612 x 792 points
        val baseWidth = if (config.pageSize == PageSize.LETTER) 612 else 595
        val baseHeight = if (config.pageSize == PageSize.LETTER) 792 else 842

        val targetWidth = if (config.orientation == PageOrientation.LANDSCAPE) baseHeight else baseWidth
        val targetHeight = if (config.orientation == PageOrientation.LANDSCAPE) baseWidth else baseHeight

        try {
            imageUris.forEachIndexed { index, uri ->
                // Decode sampling bitmap
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2 // Downsample by 2 for memory safety in multi-page compilations
                }
                
                val sourceBitmap = context.contentResolver.openInputStream(uri).use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: return@forEachIndexed

                // Set computed dimensional states
                val pageW = if (config.pageSize == PageSize.ORIGINAL_IMAGE_SIZE) sourceBitmap.width else targetWidth
                val pageH = if (config.pageSize == PageSize.ORIGINAL_IMAGE_SIZE) sourceBitmap.height else targetHeight

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Calculate scales preserving aspect-ratio bounds inside page padding margins
                val usableW = pageW - (config.marginPixels * 2)
                val usableH = pageH - (config.marginPixels * 2)

                val scaleX = usableW.toFloat() / sourceBitmap.width.toFloat()
                val scaleY = usableH.toFloat() / sourceBitmap.height.toFloat()
                val scale = Math.min(scaleX, scaleY).coerceAtMost(1.0f)

                val drawW = (sourceBitmap.width * scale).toInt()
                val drawH = (sourceBitmap.height * scale).toInt()

                // Center coordinates alignments inside margins
                val startX = config.marginPixels + (usableW - drawW) / 2
                val startY = config.marginPixels + (usableH - drawH) / 2

                val destRect = Rect(startX, startY, startX + drawW, startY + drawH)
                
                // Draw imagery, finish page, and recycle native pointer structures immediately
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

## 5. Screen Interface layout (Jetpack Compose View)

We construct an elegant, single-view dashboard designed with high-density card units and standard grid configurations.

```
+------------------------------------------------------------+
|  [<-]  Image Toolkit Dashboard                             |
+------------------------------------------------------------+
|  Horizontal Selected Files Queue:                       [+]|
|  +----------------+  +----------------+  +----------------+|
|  | [🖼️ Image 1]   |  | [🖼️ Image 2]   |  | [+] Add Images ||
|  |  (1.4 MB)      |  |  (590 KB)      |  |                ||
|  |     [Delete]   |  |     [Delete]   |  |                ||
|  +----------------+  +----------------+  +----------------+|
+------------------------------------------------------------+
|  ⚙️ FILE-SIZE REDUCTION CONFIGURATION                       |
+------------------------------------------------------------+
|  Target Format:  (*) JPEG    ( ) PNG    ( ) WebP (Lossy)  |
|                                                            |
|  Compression Quality (75%):                                |
|  [======================o-------------] 1% to 100%         |
|                                                            |
|  Dimension Scale (50%):                                    |
|  [===========o------------------------] 10% to 100%        |
|                                                            |
|  [⚡ Execute Image Compressions] -> (Predict size savings)  |
+------------------------------------------------------------+
|  📄 PDF DOCUMENT COMPILATION OPTIONS                       |
+------------------------------------------------------------+
|  Sheet Layout Size:  (*) A4    ( ) Letter  ( ) Match Image |
|                                                            |
|  Orientation:         (*) Portrait    ( ) Landscape         |
|                                                            |
|  [📂 Build Unified PDF Document]                          |
+------------------------------------------------------------+
|  📊 Active Operation Workspace Output Terminal             |
|  - Log: Selected absolute media paths validated.           |
|  - Log: Starting background image compilation loops...     |
+------------------------------------------------------------+
```

### Premium Interaction UX Properties:
- **Real-Time Savings Predictor**: Computes preview measurements and provides immediate feedback indicating simulated disk bytes savings.
- **Async Execution Shimmer State**: Buttons transition into high-fidelity loading overlays while process computations execute in background coroutines.
- **Unified Actions Feed Log**: Formats runtime event statements in clean monospaced display layouts at the bottom of the interface.

---

## 6. Permissions, Storage, and Provider Configurations

We strictly satisfy modern Android Scoped Storage and security guide patterns.

### Scoped Storage Directory Locations:
- Compressed Images are stored locally inside the application's isolated pictures directory:  
  `context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/Compressed/"`
- Compiled PDFs are output locally inside the internal private documents directory:  
  `context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/CompiledPDFs/"`

### Application Sharing Providers Configuration:
To allow outer system utilities or email apps to access and load generated PDFs/Images, we configure a system `FileProvider` mapping:

```xml
<!-- Inside AndroidManifest.xml -->
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_provider_paths" />
</provider>
```

```xml
<!-- Inside res/xml/file_provider_paths.xml -->
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="compressed_images" path="Pictures/Compressed" />
    <external-files-path name="compiled_pdfs" path="Documents/CompiledPDFs" />
</paths>
```

---

## 7. Staged Implementation & Rollout Schedule

### Phase 1: Core Layout Setup & PhotoPicker bindings
1. Develop `ImagePdfScreen.kt` featuring a responsive layout containing the visual image picker carousel.
2. Bind the modern `ActivityResultContracts.PickMultipleVisualMedia` launcher to register selection changes into Compose states.
3. Incorporate detailed custom sliders for Scale resizing (10% to 100%) and Quality compression (1% to 100%).

### Phase 2: Implement `ImageReducerEngine` and Memory Recycling
1. Code `ImageReducerEngine.kt` to load local file input streams, safely down-sample large pixels, and execute fractional matrix scaling.
2. Direct output outputs into the secure private storage subdirectory (`/Pictures/Compressed/`).
3. Add in-place `.recycle()` bitmap cleanups to safeguard system background caches, passing lint criteria.

### Phase 3: Implement Native `PdfCompilationEngine`
1. Construct `PdfCompilationEngine.kt` to serialize sequential Uri models from lists.
2. Incorporate custom layout mappings to scale and center image rectangles inside designated printable canvas limits (Margin bounds, A4 vs. Letter layout geometries).
3. Securely emit structural document results and register them inside the local filesystem (`/Documents/CompiledPDFs/`).

### Phase 4: Wires & Progress Indicators Integration
1. Wire components to the interactive status log, displaying absolute compression speeds and saved space measurements.
2. Add dynamic shared triggers using standard Android `Intent.ACTION_SEND` and file targets enabled using `FileProvider`.

### Phase 5: Verification and Build Validation
1. Verify flawless compilation parameters by executing `compile_applet`.
2. Ensure consistent compliance with all Material theme styles and layout sizing.
