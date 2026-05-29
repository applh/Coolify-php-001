package com.example.cameraxapp.core.math3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.*

data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(factor: Float) = Vector3(x * factor, y * factor, z * factor)
    operator fun div(factor: Float) = if (factor != 0f) Vector3(x / factor, y / factor, z / factor) else this
    
    fun length() = sqrt(x * x + y * y + z * z)
    
    fun normalize(): Vector3 {
        val len = length()
        return if (len > 0f) Vector3(x / len, y / len, z / len) else this
    }
    
    fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z
    
    fun cross(other: Vector3) = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun rotateX(ang: Float): Vector3 {
        val cos = cos(ang)
        val sin = sin(ang)
        return Vector3(x, y * cos - z * sin, y * sin + z * cos)
    }

    fun rotateY(ang: Float): Vector3 {
        val cos = cos(ang)
        val sin = sin(ang)
        return Vector3(x * cos + z * sin, y, -x * sin + z * cos)
    }

    fun rotateZ(ang: Float): Vector3 {
        val cos = cos(ang)
        val sin = sin(ang)
        return Vector3(x * cos - y * sin, x * sin + y * cos, z)
    }
}

sealed class RenderItem3D {
    abstract val depth: Float

    data class Polygon(
        val pts: List<Vector3>,
        val color: Color,
        val outlineColor: Color = Color.Black.copy(alpha = 0.4f),
        val isFilled: Boolean = true,
        val strokeWidth: Float = 1.5f,
        val tag: String = "",
        override val depth: Float
    ) : RenderItem3D()

    data class Sphere(
        val center: Vector3,
        val radius: Float,
        val color: Color,
        val label: String = "",
        val labelColor: Color = Color.White,
        override val depth: Float
    ) : RenderItem3D()

    data class Line(
        val start: Vector3,
        val end: Vector3,
        val color: Color,
        val strokeWidth: Float = 2f,
        override val depth: Float
    ) : RenderItem3D()

    data class TextLabel(
        val position: Vector3,
        val text: String,
        val color: Color,
        val sizeMultiplier: Float = 1.0f,
        override val depth: Float
    ) : RenderItem3D()
}
