package com.example.cameraxapp.draw

import android.graphics.Bitmap
import androidx.compose.ui.graphics.BlendMode

enum class LayerType {
    VECTOR_INK,
    RASTER_IMAGE
}

data class DrawnStroke(
    val points: List<androidx.compose.ui.geometry.Offset>,
    val color: Int,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

data class DrawLayer(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    val type: LayerType,
    var isVisible: Boolean = true,
    var isLocked: Boolean = false,
    var opacity: Float = 1.0f,
    var blendMode: BlendMode = BlendMode.SrcOver,
    var bitmap: Bitmap? = null, // Used if LayerType is RASTER_IMAGE
    val strokes: MutableList<DrawnStroke> = mutableListOf() // Used if LayerType is VECTOR_INK
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
