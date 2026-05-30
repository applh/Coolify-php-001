package com.example.cameraxapp.draw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DrawViewModel(context: Context) : ViewModel() {

    val canvasWidth = mutableStateOf(1080)
    val canvasHeight = mutableStateOf(1080)

    private val _layers = mutableStateListOf<DrawLayer>()
    val layers: List<DrawLayer> get() = _layers

    private val _activeLayerId = mutableStateOf<String?>(null)
    val activeLayerId: State<String?> = _activeLayerId

    // Tooling preferences
    val currentBrushColor = mutableStateOf(android.graphics.Color.BLACK)
    val currentBrushSize = mutableStateOf(15f)
    val activeTool = mutableStateOf("Pen") // "Pen", "Eraser", "Circle", "Rectangle", "Line"

    // Background asynchronous downloads tasks status
    private val _isDownloading = mutableStateOf(false)
    val isDownloading: State<Boolean> = _isDownloading

    // Toast and Dialog feedback helper states
    val actionFeedbackMessage = mutableStateOf<String?>(null)

    // History tracking
    private val undoStack = mutableListOf<List<DrawLayer>>()
    private val redoStack = mutableListOf<List<DrawLayer>>()

    init {
        resetCanvas(1080, 1080)
    }

    fun resetCanvas(width: Int, height: Int) {
        canvasWidth.value = width
        canvasHeight.value = height
        _layers.clear()
        undoStack.clear()
        redoStack.clear()

        // 1. Initial background solid raster layer
        val defaultBg = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
        }
        val bgLayer = DrawLayer(
            id = "bg_layer_solid",
            name = "Background Solid",
            type = LayerType.RASTER_IMAGE,
            bitmap = defaultBg
        )

        // 2. Initial vector drawing layer
        val inkLayer = DrawLayer(
            id = "ink_layer_default",
            name = "Main Vector Ink",
            type = LayerType.VECTOR_INK
        )

        _layers.add(bgLayer)
        _layers.add(inkLayer)
        _activeLayerId.value = inkLayer.id
    }

    fun saveToUndoStack() {
        val backup = _layers.map { it.duplicate() }
        undoStack.add(backup)
        if (undoStack.size > 20) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentBackup = _layers.map { it.duplicate() }
            redoStack.add(currentBackup)

            val previousState = undoStack.removeAt(undoStack.size - 1)
            _layers.clear()
            _layers.addAll(previousState)

            // Validate active target ID
            if (_layers.none { it.id == _activeLayerId.value }) {
                _activeLayerId.value = _layers.lastOrNull()?.id
            }
            actionFeedbackMessage.value = "Undone changes"
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentBackup = _layers.map { it.duplicate() }
            undoStack.add(currentBackup)

            val nextState = redoStack.removeAt(redoStack.size - 1)
            _layers.clear()
            _layers.addAll(nextState)

            if (_layers.none { it.id == _activeLayerId.value }) {
                _activeLayerId.value = _layers.lastOrNull()?.id
            }
            actionFeedbackMessage.value = "Redone changes"
        }
    }

    fun getActiveLayer(): DrawLayer? {
        return _layers.find { it.id == _activeLayerId.value }
    }

    fun selectLayer(id: String) {
        _activeLayerId.value = id
    }

    fun addBlankInkLayer() {
        saveToUndoStack()
        val num = _layers.count { it.type == LayerType.VECTOR_INK } + 1
        val newLayer = DrawLayer(
            id = UUID.randomUUID().toString(),
            name = "Ink Layer $num",
            type = LayerType.VECTOR_INK
        )
        _layers.add(newLayer)
        _activeLayerId.value = newLayer.id
        actionFeedbackMessage.value = "Added blank Ink Layer"
    }

    fun duplicateActiveLayer() {
        val active = getActiveLayer() ?: return
        saveToUndoStack()
        val duplicated = active.duplicate()
        val index = _layers.indexOfFirst { it.id == active.id }
        if (index != -1) {
            _layers.add(index + 1, duplicated)
            _activeLayerId.value = duplicated.id
            actionFeedbackMessage.value = "Duplicated target layer"
        }
    }

    fun deleteActiveLayer() {
        if (_layers.size <= 1) {
            actionFeedbackMessage.value = "Cannot delete last remaining layer"
            return
        }
        val active = getActiveLayer() ?: return
        saveToUndoStack()
        val index = _layers.indexOfFirst { it.id == active.id }
        if (index != -1) {
            _layers.removeAt(index)
            val nextActiveIdx = if (index >= _layers.size) _layers.size - 1 else index
            _activeLayerId.value = _layers[nextActiveIdx].id
            actionFeedbackMessage.value = "Layer deleted"
        }
    }

    fun moveLayerUp(id: String) {
        val index = _layers.indexOfFirst { it.id == id }
        if (index != -1 && index < _layers.size - 1) {
            saveToUndoStack()
            val element = _layers.removeAt(index)
            _layers.add(index + 1, element)
        }
    }

    fun moveLayerDown(id: String) {
        val index = _layers.indexOfFirst { it.id == id }
        if (index > 0) {
            saveToUndoStack()
            val element = _layers.removeAt(index)
            _layers.add(index - 1, element)
        }
    }

    fun updateLayerOpacity(id: String, opacity: Float) {
        val index = _layers.indexOfFirst { it.id == id }
        if (index != -1) {
            _layers[index] = _layers[index].copy(opacity = opacity)
        }
    }

    fun updateLayerVisibility(id: String, isVisible: Boolean) {
        val index = _layers.indexOfFirst { it.id == id }
        if (index != -1) {
            _layers[index] = _layers[index].copy(isVisible = isVisible)
        }
    }

    fun updateLayerLocked(id: String, isLocked: Boolean) {
        val index = _layers.indexOfFirst { it.id == id }
        if (index != -1) {
            _layers[index] = _layers[index].copy(isLocked = isLocked)
        }
    }

    fun updateLayerBlendMode(id: String, blendMode: BlendMode) {
        val index = _layers.indexOfFirst { it.id == id }
        if (index != -1) {
            saveToUndoStack()
            _layers[index] = _layers[index].copy(blendMode = blendMode)
        }
    }

    fun renameLayer(id: String, newName: String) {
        val index = _layers.indexOfFirst { it.id == id }
        if (index != -1) {
            _layers[index] = _layers[index].copy(name = newName)
        }
    }

    /**
     * Merges active layer onto the layer directly below it in our ordered cascade stack.
     */
    fun mergeActiveLayerDown() {
        val active = getActiveLayer() ?: return
        val index = _layers.indexOfFirst { it.id == active.id }
        if (index <= 0) {
            actionFeedbackMessage.value = "No layer below to merge with"
            return
        }

        saveToUndoStack()
        val lowerLayer = _layers[index - 1]

        if (active.type == LayerType.VECTOR_INK && lowerLayer.type == LayerType.VECTOR_INK) {
            // Merge vectors directly
            lowerLayer.strokes.addAll(active.strokes)
            _layers.removeAt(index)
            _activeLayerId.value = lowerLayer.id
            actionFeedbackMessage.value = "Merged ink vectors down"
        } else {
            // Mixed or raster conversion merge: render composite of just these two sub-layers
            val combinedBitmap = Bitmap.createBitmap(canvasWidth.value, canvasHeight.value, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(combinedBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Draw lower layer
            drawLayerToNativeCanvas(lowerLayer, canvas, paint, 1.0f)
            // Draw upper (active) layer with its transparency & blend properties applied
            paint.alpha = (active.opacity * 255).toInt()
            paint.xfermode = getPorterDuffXfermode(active.blendMode)
            drawLayerToNativeCanvas(active, canvas, paint, 1.0f)

            // Standardize lower layer as a RASTER image containing unified details
            val mergedLayer = DrawLayer(
                id = lowerLayer.id,
                name = "Merged Stack",
                type = LayerType.RASTER_IMAGE,
                bitmap = combinedBitmap,
                isVisible = lowerLayer.isVisible,
                isLocked = lowerLayer.isLocked,
                opacity = 1.0f,
                blendMode = lowerLayer.blendMode
            )

            _layers[index - 1] = mergedLayer
            _layers.removeAt(index)
            _activeLayerId.value = mergedLayer.id
            actionFeedbackMessage.value = "Merged layer states into Raster background"
        }
    }

    /**
     * Clear all vector strokes or reset image content on active selected layer.
     */
    fun clearActiveLayer() {
        val active = getActiveLayer() ?: return
        if (active.isLocked) {
            actionFeedbackMessage.value = "Layer is locked!"
            return
        }
        saveToUndoStack()
        if (active.type == LayerType.VECTOR_INK) {
            active.strokes.clear()
            // Quick mutable force update trigger
            val index = _layers.indexOfFirst { it.id == active.id }
            if (index != -1) {
                _layers[index] = active.copy(strokes = mutableListOf())
            }
        } else {
            val emptyBitmap = Bitmap.createBitmap(canvasWidth.value, canvasHeight.value, Bitmap.Config.ARGB_8888)
            emptyBitmap.eraseColor(android.graphics.Color.WHITE)
            val index = _layers.indexOfFirst { it.id == active.id }
            if (index != -1) {
                _layers[index] = active.copy(bitmap = emptyBitmap)
            }
        }
        actionFeedbackMessage.value = "Layer cleared"
    }

    /**
     * Sourcing support: Add local image
     */
    fun addLocalDeviceImage(bitmap: Bitmap, name: String = "Imported Layer") {
        saveToUndoStack()
        val scaled = scaleBitmapToCanvas(bitmap, canvasWidth.value, canvasHeight.value)
        val newRasterLayer = DrawLayer(
            id = UUID.randomUUID().toString(),
            name = name,
            type = LayerType.RASTER_IMAGE,
            bitmap = scaled
        )
        _layers.add(newRasterLayer)
        _activeLayerId.value = newRasterLayer.id
        actionFeedbackMessage.value = "Imported image layer"
    }

    /**
     * Sourcing support: Web downloader engine
     */
    fun downloadUrlImage(context: Context, urlString: String) {
        viewModelScope.launch {
            _isDownloading.value = true
            val downloaded = withContext(Dispatchers.IO) {
                try {
                    val url = URL(urlString)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.doInput = true
                    conn.connectTimeout = 8000
                    conn.readTimeout = 12000
                    conn.connect()
                    val input: InputStream = conn.inputStream
                    BitmapFactory.decodeStream(input)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            _isDownloading.value = false
            if (downloaded != null) {
                addLocalDeviceImage(downloaded, "Web Layer")
            } else {
                actionFeedbackMessage.value = "Failed to download image from web"
            }
        }
    }

    private fun scaleBitmapToCanvas(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcWidth = source.width
        val srcHeight = source.height
        
        // Calculate dynamic aspect fit
        val scale = Math.max(targetWidth.toFloat() / srcWidth, targetHeight.toFloat() / srcHeight)
        val scaledWidth = (srcWidth * scale).toInt()
        val scaledHeight = (srcHeight * scale).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        
        // Center crop onto project canvas resolution bounds
        val xOffset = (scaledWidth - targetWidth) / 2
        val yOffset = (scaledHeight - targetHeight) / 2
        
        return Bitmap.createBitmap(scaledBitmap, xOffset.coerceAtLeast(0), yOffset.coerceAtLeast(0), targetWidth, targetHeight)
    }

    /**
     * Saves drawing projects into public Gallery or Sandbox directories.
     */
    fun saveCompositeArtwork(
        context: Context,
        format: String,  // "PNG", "JPEG", "WEBP"
        quality: Int,    // 0 to 100
        dimensionScale: Float // 1.0f, 2.0f, 4.0f
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val completeBitmap = compileCompositeImage(dimensionScale)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val extension = format.lowercase()
            val fileName = "StudioDraw_${timestamp}.$extension"
            
            val mimeType = when (format) {
                "PNG" -> "image/png"
                "JPEG" -> "image/jpeg"
                "WEBP" -> "image/webp"
                else -> "image/png"
            }

            val compressFormat = when (format) {
                "PNG" -> Bitmap.CompressFormat.PNG
                "JPEG" -> Bitmap.CompressFormat.JPEG
                "WEBP" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.PNG
            }

            val resultUri = saveMediaToStorage(context, completeBitmap, fileName, mimeType, compressFormat, quality)
            withContext(Dispatchers.Main) {
                if (resultUri != null) {
                    actionFeedbackMessage.value = "Saved successfully to Photos Gallery!"
                } else {
                    actionFeedbackMessage.value = "Error saving compiled masterpiece"
                }
            }
        }
    }

    private fun saveMediaToStorage(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        mimeType: String,
        compressFormat: Bitmap.CompressFormat,
        quality: Int
    ): Uri? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StudioDraw")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                try {
                    resolver.openOutputStream(imageUri).use { os ->
                        if (os != null) {
                            bitmap.compress(compressFormat, quality, os)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                    return imageUri
                } catch (e: Exception) {
                    e.printStackTrace()
                    resolver.delete(imageUri, null, null)
                }
            }
        } else {
            // Legacy fallbacks
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val drawDir = File(picturesDir, "StudioDraw")
            if (!drawDir.exists()) drawDir.mkdirs()
            val file = File(drawDir, fileName)
            try {
                FileOutputStream(file).use { fos ->
                    bitmap.compress(compressFormat, quality, fos)
                }
                return Uri.fromFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * Renders a DrawLayer cleanly onto a standard Android Graphics Canvas.
     */
    private fun drawLayerToNativeCanvas(layer: DrawLayer, canvas: Canvas, paint: Paint, scale: Float) {
        if (!layer.isVisible) return
        
        when (layer.type) {
            LayerType.RASTER_IMAGE -> {
                layer.bitmap?.let { sourceBitmap ->
                    val srcRect = Rect(0, 0, sourceBitmap.width, sourceBitmap.height)
                    val dstRect = Rect(0, 0, (canvasWidth.value * scale).toInt(), (canvasHeight.value * scale).toInt())
                    canvas.drawBitmap(sourceBitmap, srcRect, dstRect, paint)
                }
            }
            LayerType.VECTOR_INK -> {
                for (stroke in layer.strokes) {
                    val pathPaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeWidth = stroke.strokeWidth * scale
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        color = stroke.color
                        alpha = ((layer.opacity * (android.graphics.Color.alpha(stroke.color) / 255f)) * 255).toInt()
                        if (stroke.isEraser) {
                            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                        }
                    }

                    if (stroke.points.size > 1) {
                        val path = android.graphics.Path()
                        path.moveTo(stroke.points[0].x * scale, stroke.points[0].y * scale)
                        for (i in 1 until stroke.points.size) {
                            path.lineTo(stroke.points[i].x * scale, stroke.points[i].y * scale)
                        }
                        canvas.drawPath(path, pathPaint)
                    } else if (stroke.points.isNotEmpty()) {
                        val p = stroke.points[0]
                        val dotPaint = Paint().apply {
                            isAntiAlias = true
                            style = Paint.Style.FILL
                            color = stroke.color
                            alpha = ((layer.opacity * (android.graphics.Color.alpha(stroke.color) / 255f)) * 255).toInt()
                            if (stroke.isEraser) {
                                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                            }
                        }
                        canvas.drawCircle(p.x * scale, p.y * scale, (stroke.strokeWidth / 2f) * scale, dotPaint)
                    }
                }
            }
        }
    }

    /**
     * Flattens background layer stacks into a standard single raster target.
     */
    fun compileCompositeImage(scaleFactor: Float = 1.0f): Bitmap {
        val scaledWidth = (canvasWidth.value * scaleFactor).toInt()
        val scaledHeight = (canvasHeight.value * scaleFactor).toInt()

        val compositeBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(compositeBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (layer in _layers) {
            if (!layer.isVisible) continue

            paint.alpha = (layer.opacity * 255).toInt()
            paint.xfermode = if (layer.id == "bg_layer_solid") null else getPorterDuffXfermode(layer.blendMode)

            drawLayerToNativeCanvas(layer, canvas, paint, scaleFactor)
        }
        return compositeBitmap
    }

    private fun getPorterDuffXfermode(blendMode: BlendMode): PorterDuffXfermode? {
        return when (blendMode) {
            BlendMode.SrcOver -> null
            BlendMode.Multiply -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            BlendMode.Screen -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            BlendMode.Plus -> PorterDuffXfermode(PorterDuff.Mode.ADD)
            BlendMode.SrcAtop -> PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            BlendMode.DstOver -> PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
            else -> null
        }
    }
}
