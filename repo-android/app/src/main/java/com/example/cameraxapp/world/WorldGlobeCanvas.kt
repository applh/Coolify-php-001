package com.example.cameraxapp.world

import android.graphics.Bitmap
import android.graphics.Matrix as AndroidMatrix
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.Shader as AndroidShader
import android.graphics.BitmapShader as AndroidBitmapShader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.*

// Self-contained 3D coordinate vector
data class WorldVector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: WorldVector3) = WorldVector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: WorldVector3) = WorldVector3(x - other.x, y - other.y, z - other.z)
    operator fun times(factor: Float) = WorldVector3(x * factor, y * factor, z * factor)
    
    fun length() = sqrt(x * x + y * y + z * z)
    
    fun normalize(): WorldVector3 {
        val len = length()
        return if (len > 0f) WorldVector3(x / len, y / len, z / len) else this
    }
    
    fun dot(other: WorldVector3) = x * other.x + y * other.y + z * other.z
    
    fun cross(other: WorldVector3) = WorldVector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun rotateX(ang: Float): WorldVector3 {
        val cos = cos(ang)
        val sin = sin(ang)
        return WorldVector3(x, y * cos - z * sin, y * sin + z * cos)
    }

    fun rotateY(ang: Float): WorldVector3 {
        val cos = cos(ang)
        val sin = sin(ang)
        return WorldVector3(x * cos + z * sin, y, -x * sin + z * cos)
    }

    fun rotateZ(ang: Float): WorldVector3 {
        val cos = cos(ang)
        val sin = sin(ang)
        return WorldVector3(x * cos - y * sin, x * sin + y * cos, z)
    }
}

// Data structure representing a triangulated polygon on the globe sphere mesh
data class SphereFace(
    val v0: WorldVector3, val v1: WorldVector3, val v2: WorldVector3,
    val uv0: Offset, val uv1: Offset, val uv2: Offset
)

object WorldGlobeGenerator {
    /**
     * Generates a coordinates list of triangular polygons forming a 3D sphere.
     */
    fun generateSphereMesh(density: Int, radius: Float): List<SphereFace> {
        val faces = mutableListOf<SphereFace>()
        val latSteps = density.coerceIn(8, 60)
        val lonSteps = (density * 2).coerceIn(16, 120)

        // Helper to get point on sphere
        fun getSpherePoint(latIdx: Int, lonIdx: Int): Pair<WorldVector3, Offset> {
            val theta = -PI.toFloat() / 2f + (latIdx.toFloat() / latSteps) * PI.toFloat()
            val phi = -PI.toFloat() + (lonIdx.toFloat() / lonSteps) * (2f * PI.toFloat())
            
            val x = radius * cos(theta) * cos(phi)
            val y = radius * sin(theta)
            val z = radius * cos(theta) * sin(phi)
            
            val u = lonIdx.toFloat() / lonSteps
            val v = 1f - (latIdx.toFloat() / latSteps) // invert so top is North pole
            
            return Pair(WorldVector3(x, y, z), Offset(u, v))
        }

        // Generate grid coordinates and sew together triangulated polygons
        for (i in 0 until latSteps) {
            for (j in 0 until lonSteps) {
                val p00 = getSpherePoint(i, j)
                val p10 = getSpherePoint(i + 1, j)
                val p01 = getSpherePoint(i, j + 1)
                val p11 = getSpherePoint(i + 1, j + 1)

                // Split quad cell into two triangles
                // Triangle A
                faces.add(SphereFace(p00.first, p10.first, p01.first, p00.second, p10.second, p01.second))
                // Triangle B
                faces.add(SphereFace(p01.first, p10.first, p11.first, p01.second, p10.second, p11.second))
            }
        }
        return faces
    }
}

/**
 * Affine textured triangle drawer.
 */
fun DrawScope.drawTexturedTriangle(
    p0: Offset, p1: Offset, p2: Offset,
    uv0: Offset, uv1: Offset, uv2: Offset,
    bitmap: Bitmap,
    lightMultiplier: Float,
    specular: Float,
    wireframeOnly: Boolean,
    gridLineColor: Color
) {
    if (wireframeOnly) {
        // Simple fast wireframe drawing mode
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }
        drawPath(
            path = path,
            color = gridLineColor.copy(alpha = 0.5f * lightMultiplier),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
        return
    }

    val width = bitmap.width.toFloat()
    val height = bitmap.height.toFloat()

    // Map source UV texture projection coordinates
    var u0 = uv0.x
    var u1 = uv1.x
    var u2 = uv2.x

    // Handle seamless wraparound wrapping to avoid horizontal tearing seam
    val minU = min(u0, min(u1, u2))
    val maxU = max(u0, max(u1, u2))
    if (maxU - minU > 0.5f) {
        if (u0 < 0.5f) u0 += 1.0f
        if (u1 < 0.5f) u1 += 1.0f
        if (u2 < 0.5f) u2 += 1.0f
    }

    val t0x = u0 * width
    val t0y = uv0.y * height
    val t1x = u1 * width
    val t1y = uv1.y * height
    val t2x = u2 * width
    val t2y = uv2.y * height

    val uDet = (t1x - t0x) * (t2y - t0y) - (t2x - t0x) * (t1y - t0y)
    if (abs(uDet) < 0.0001f) return

    val invDet = 1.0f / uDet
    
    val m00 = ((p1.x - p0.x) * (t2y - t0y) - (p2.x - p0.x) * (t1y - t0y)) * invDet
    val m01 = ((p2.x - p0.x) * (t1x - t0x) - (p1.x - p0.x) * (t2x - t0x)) * invDet
    val m02 = p0.x - m00 * t0x - m01 * t0y

    val m10 = ((p1.y - p0.y) * (t2y - t0y) - (p2.y - p0.y) * (t1y - t0y)) * invDet
    val m11 = ((p2.y - p0.y) * (t1x - t0x) - (p1.y - p0.y) * (t2x - t0x)) * invDet
    val m12 = p0.y - m10 * t0x - m11 * t0y

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        
        val path = AndroidPath().apply {
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }
        
        nativeCanvas.save()
        nativeCanvas.clipPath(path)
        
        val shader = AndroidBitmapShader(
            bitmap,
            AndroidShader.TileMode.REPEAT,
            AndroidShader.TileMode.CLAMP
        )
        val matrix = AndroidMatrix()
        matrix.setValues(floatArrayOf(
            m00, m01, m02,
            m10, m11, m12,
            0f,   0f,   1f
        ))
        shader.setLocalMatrix(matrix)
        
        val paint = AndroidPaint().apply {
            isAntiAlias = true
            setShader(shader)
            
            val r = (lightMultiplier * 255).coerceIn(0f, 255f).toInt()
            val g = (lightMultiplier * 255).coerceIn(0f, 255f).toInt()
            val b = (lightMultiplier * 255).coerceIn(0f, 255f).toInt()
            
            val brightnessFilter = android.graphics.ColorMatrix(floatArrayOf(
                r / 255f, 0f, 0f, 0f, specular * 255f,
                0f, g / 255f, 0f, 0f, specular * 255f,
                0f, 0f, b / 255f, 0f, specular * 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            colorFilter = android.graphics.ColorMatrixColorFilter(brightnessFilter)
        }
        nativeCanvas.drawPath(path, paint)
        nativeCanvas.restore()
    }
}
