package com.example.cameraxapp.roguelike

import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cameraxapp.core.math3d.RenderItem3D
import com.example.cameraxapp.core.math3d.Vector3
import kotlin.math.*

@Composable
fun DungeonCanvas3D(
    tiles: List<GameTile>,
    monsters: List<MonsterState>,
    pX: Int,
    pY: Int,
    heroClass: String,
    modifier: Modifier = Modifier
) {
    var yawAngle by remember { mutableStateOf(-0.35f) }
    var pitchAngle by remember { mutableStateOf(-0.55f) }
    var zoomScale by remember { mutableStateOf(1.4f) }
    var lightingStrength by remember { mutableStateOf(1.5f) }

    val textPainter = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    yawAngle = (yawAngle + pan.x * 0.007f)
                    pitchAngle = (pitchAngle - pan.y * 0.007f).coerceIn(-1.4f, -0.2f)
                    zoomScale = (zoomScale * zoom).coerceIn(0.4f, 3.0f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cX = width / 2f
            val cY = height / 2f

            // 3D Scene setup constraints
            val cameraZ = 320f
            val dFactor = 280f
            val W_s = 22f

            // Projections math mapping
            fun projectPoint(v: Vector3): Offset {
                val rot = v.rotateY(yawAngle).rotateX(pitchAngle)
                val denom = rot.z + cameraZ
                val sx = cX + (rot.x * dFactor * zoomScale) / if (denom != 0f) denom else 1f
                val sy = cY + (rot.y * dFactor * zoomScale) / if (denom != 0f) denom else 1f
                return Offset(sx, sy)
            }

            fun getRotatedZDeep(v: Vector3): Float {
                return v.rotateY(yawAngle).rotateX(pitchAngle).z
            }

            // Define light source centered at Player position (Y is light height overhead)
            val pLightX = (pX - 9f) * W_s
            val pLightZ = (pY - 9f) * W_s
            val lightSource = Vector3(pLightX, -W_s * 1.5f, pLightZ)

            val drawPipeline = mutableListOf<RenderItem3D>()

            // 1. Traverse and construct floor grids and Wall high-cubes
            tiles.forEach { tile ->
                if (tile.revealed) {
                    val tileX = (tile.x - 9f) * W_s
                    val tileZ = (tile.y - 9f) * W_s
                    val floorY = W_s * 0.5f

                    if (tile.tileType == "WALL") {
                        // Elevated brick block
                        buildWallCube(
                            cx = tileX, cy = floorY - W_s / 2f, cz = tileZ,
                            sizeX = W_s * 0.96f, sizeY = W_s, sizeZ = W_s * 0.96f,
                            color = Color(0xFF3E352C),
                            outList = drawPipeline,
                            lightSource = lightSource,
                            lightingStrength = lightingStrength
                        )
                    } else {
                        // Drawing flat Ground tile
                        val half = W_s / 2f
                        val corners = listOf(
                            Vector3(tileX - half, floorY, tileZ - half),
                            Vector3(tileX + half, floorY, tileZ - half),
                            Vector3(tileX + half, floorY, tileZ + half),
                            Vector3(tileX - half, floorY, tileZ + half)
                        )

                        // Light calculation
                        val dist = (Vector3(tileX, floorY, tileZ) - lightSource).length()
                        val attenuation = 1f / (1.0f + (0.0016f / lightingStrength) * dist * dist)
                        val lightFactor = (0.10f * lightingStrength + 0.90f * attenuation).coerceIn(0f, 1f)

                        val tileColor = when (tile.tileType) {
                            "STAIRS_DOWN" -> Color(0xFF2E1C3F)
                            "CHEST" -> Color(0xFF4A341E)
                            else -> Color(0xFF141312)
                        }

                        val shadedCol = Color(
                            red = (tileColor.red * lightFactor).coerceIn(0f, 1f),
                            green = (tileColor.green * lightFactor).coerceIn(0f, 1f),
                            blue = (tileColor.blue * lightFactor).coerceIn(0f, 1f),
                            alpha = 1f
                        )

                        drawPipeline.add(RenderItem3D.Polygon(corners, shadedCol, depth = 0f))

                        if (tile.tileType == "STAIRS_DOWN") {
                            drawPipeline.add(RenderItem3D.TextLabel(Vector3(tileX, floorY - 3f, tileZ), "🪜", Color.White, sizeMultiplier = 0.9f, depth = 0f))
                        } else if (tile.tileType == "CHEST") {
                            drawPipeline.add(RenderItem3D.TextLabel(Vector3(tileX, floorY - 3f, tileZ), "📦", Color.White, sizeMultiplier = 0.85f, depth = 0f))
                        }
                    }
                }
            }

            // 2. Build monsters volumetric shapes with status overlays
            monsters.forEach { monster ->
                val monsterTileRevealed = tiles.find { it.x == monster.x && it.y == monster.y }?.revealed ?: false
                if (monsterTileRevealed) {
                    val mX = (monster.x - 9f) * W_s
                    val mZ = (monster.y - 9f) * W_s
                    val mY = W_s * 0.15f

                    val monsterCol = when (monster.type) {
                        "DRAGON" -> Color(0xFFC62828)
                        "NECROMANCER" -> Color(0xFF6A1B9A)
                        "GOBLIN" -> Color(0xFF2E7D32)
                        else -> Color(0xFF757575)
                    }

                    val glyph = when (monster.type) {
                        "DRAGON" -> "🐉"
                        "NECROMANCER" -> "🔮"
                        "GOBLIN" -> "👺"
                        else -> "💀"
                    }

                    // Build 3D Octahedron entity representation
                    buildVolumetricOctahedron(
                        cx = mX, cy = mY, cz = mZ,
                        radX = W_s * 0.32f, radY = W_s * 0.45f, radZ = W_s * 0.32f,
                        color = monsterCol,
                        outList = drawPipeline,
                        lightSource = lightSource,
                        lightingStrength = lightingStrength
                    )

                    // Add floating tag emoji above the entity heading
                    drawPipeline.add(RenderItem3D.TextLabel(Vector3(mX, mY - W_s * 0.70f, mZ), glyph, Color.White, sizeMultiplier = 1.0f, depth = 0f))
                    
                    val hpPercent = (monster.currentHp * 100 / monster.maxHp).toString() + "%"
                    drawPipeline.add(RenderItem3D.TextLabel(Vector3(mX, mY - W_s * 1.05f, mZ), hpPercent, Color(0xFF81C784), sizeMultiplier = 0.65f, depth = 0f))
                }
            }

            // 3. Build player hero entity shape
            val pWorldX = (pX - 9f) * W_s
            val pWorldZ = (pY - 9f) * W_s
            val pWorldY = W_s * 0.2f

            val playerCol = Color(0xFFFF9100)
            val pGlyph = when (heroClass) {
                "Warrior" -> "🛡️"
                "Mage" -> "🧙"
                else -> "🗡️"
            }

            // Core 3D model
            buildVolumetricOctahedron(
                cx = pWorldX, cy = pWorldY, cz = pWorldZ,
                radX = W_s * 0.30f, radY = W_s * 0.45f, radZ = W_s * 0.30f,
                color = playerCol,
                outList = drawPipeline,
                lightSource = lightSource,
                lightingStrength = lightingStrength
            )

            // Dynamic float action labels
            drawPipeline.add(RenderItem3D.TextLabel(Vector3(pWorldX, pWorldY - W_s * 0.85f, pWorldZ), pGlyph, Color.White, sizeMultiplier = 1.1f, depth = 0f))
            drawPipeline.add(RenderItem3D.TextLabel(Vector3(pWorldX, pWorldY - W_s * 1.25f, pWorldZ), "HERO", Color(0xFFFFD700), sizeMultiplier = 0.65f, depth = 0f))

            // 4. Sort Items using Painter's depth model (Z further is drawn first)
            val sortedList = drawPipeline.sortedByDescending { item ->
                when (item) {
                    is RenderItem3D.Polygon -> {
                        var sumZ = 0f
                        item.pts.forEach { sumZ += getRotatedZDeep(it) }
                        sumZ / item.pts.size
                    }
                    is RenderItem3D.Sphere -> {
                        getRotatedZDeep(item.center)
                    }
                    is RenderItem3D.Line -> {
                        (getRotatedZDeep(item.start) + getRotatedZDeep(item.end)) / 2f
                    }
                    is RenderItem3D.TextLabel -> {
                        getRotatedZDeep(item.position)
                    }
                }
            }

            // Draw primitive lists
            sortedList.forEach { item ->
                when (item) {
                    is RenderItem3D.Polygon -> {
                        val path = Path()
                        val screenPts = item.pts.map { projectPoint(it) }
                        if (screenPts.isNotEmpty()) {
                            path.moveTo(screenPts[0].x, screenPts[0].y)
                            for (i in 1 until screenPts.size) {
                                path.lineTo(screenPts[i].x, screenPts[i].y)
                            }
                            path.close()

                            // Fill
                            drawPath(path, color = item.color)
                            // Stroke
                            drawPath(path, color = Color.Black.copy(alpha = 0.35f), style = Stroke(width = 0.8f))
                        }
                    }
                    is RenderItem3D.TextLabel -> {
                        val pLabel = projectPoint(item.position)
                        drawIntoCanvas { canvas ->
                            textPainter.color = item.color.toArgb()
                            textPainter.textSize = 10.dp.toPx() * item.sizeMultiplier
                            canvas.nativeCanvas.drawText(
                                item.text,
                                pLabel.x,
                                pLabel.y,
                                textPainter
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        // Floating Instructions corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .border(0.5.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text("DRAG / PINCH TO ROTATE & ZOOM", fontSize = 8.sp, color = Color.Yellow, letterSpacing = 0.5.sp)
        }

        // 3D HUD Settings Control Panel Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1: Zoom Manual & Reset Controls
            Row(
                modifier = Modifier.padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🔍 ZOOM: ${(zoomScale * 100).toInt()}%",
                    fontSize = 9.sp,
                    color = Color.White,
                    modifier = Modifier.width(85.dp)
                )
                // Zoom Out Button (-)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF2E2E2E), RoundedCornerShape(4.dp))
                        .clickable { zoomScale = (zoomScale - 0.15f).coerceIn(0.4f, 3.0f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", color = Color.White, fontSize = 12.sp)
                }
                // Zoom In Button (+)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF2E2E2E), RoundedCornerShape(4.dp))
                        .clickable { zoomScale = (zoomScale + 0.15f).coerceIn(0.4f, 3.0f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 12.sp)
                }
                // Reset Button
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .height(24.dp)
                        .padding(horizontal = 6.dp)
                        .background(Color(0xFF424242), RoundedCornerShape(4.dp))
                        .clickable { zoomScale = 1.4f },
                    contentAlignment = Alignment.Center
                ) {
                    Text("RESET", color = Color.White, fontSize = 8.sp)
                }
            }

            // Row 2: Lighting Strength Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "💡 TORCH: ${(lightingStrength * 100).toInt()}%",
                    fontSize = 9.sp,
                    color = Color.White,
                    modifier = Modifier.width(85.dp)
                )
                
                // Dim Preset Button
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 6.dp)
                        .background(if (lightingStrength == 0.8f) Color(0xFFFF9100) else Color(0xFF2E2E2E), RoundedCornerShape(4.dp))
                        .clickable { lightingStrength = 0.8f },
                    contentAlignment = Alignment.Center
                ) {
                    Text("DIM", color = Color.White, fontSize = 8.sp)
                }
                
                // Normal Preset Button
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 6.dp)
                        .background(if (lightingStrength == 1.5f) Color(0xFFFF9100) else Color(0xFF2E2E2E), RoundedCornerShape(4.dp))
                        .clickable { lightingStrength = 1.5f },
                    contentAlignment = Alignment.Center
                ) {
                    Text("TORCH", color = Color.White, fontSize = 8.sp)
                }

                // Bright Preset Button
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 6.dp)
                        .background(if (lightingStrength == 2.2f) Color(0xFFFF9100) else Color(0xFF2E2E2E), RoundedCornerShape(4.dp))
                        .clickable { lightingStrength = 2.2f },
                    contentAlignment = Alignment.Center
                ) {
                    Text("LANTERN", color = Color.White, fontSize = 8.sp)
                }

                // Max Preset Button
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 6.dp)
                        .background(if (lightingStrength == 3.0f) Color(0xFFFF9100) else Color(0xFF2E2E2E), RoundedCornerShape(4.dp))
                        .clickable { lightingStrength = 3.0f },
                    contentAlignment = Alignment.Center
                ) {
                    Text("SUNLIGHT", color = Color.White, fontSize = 8.sp)
                }
            }
        }
    }
}

private fun buildWallCube(
    cx: Float, cy: Float, cz: Float,
    sizeX: Float, sizeY: Float, sizeZ: Float,
    color: Color,
    outList: MutableList<RenderItem3D>,
    lightSource: Vector3,
    lightingStrength: Float
) {
    val hx = sizeX / 2f
    val hy = sizeY / 2f
    val hz = sizeZ / 2f

    val v1 = Vector3(cx - hx, cy - hy, cz - hz)
    val v2 = Vector3(cx + hx, cy - hy, cz - hz)
    val v3 = Vector3(cx + hx, cy + hy, cz - hz)
    val v4 = Vector3(cx - hx, cy + hy, cz - hz)

    val v5 = Vector3(cx - hx, cy - hy, cz + hz)
    val v6 = Vector3(cx + hx, cy - hy, cz + hz)
    val v7 = Vector3(cx + hx, cy + hy, cz + hz)
    val v8 = Vector3(cx - hx, cy + hy, cz + hz)

    val faces = listOf(
        listOf(v1, v2, v3, v4),
        listOf(v6, v5, v8, v7),
        listOf(v1, v5, v6, v2),
        listOf(v4, v3, v7, v8),
        listOf(v1, v4, v8, v5),
        listOf(v2, v6, v7, v3)
    )

    faces.forEach { pts ->
        val faceCenter = Vector3(
            pts.sumOf { it.x.toDouble() }.toFloat() / 4f,
            pts.sumOf { it.y.toDouble() }.toFloat() / 4f,
            pts.sumOf { it.z.toDouble() }.toFloat() / 4f
        )
        val dist = (faceCenter - lightSource).length()
        val attenuation = 1f / (1.0f + (0.0016f / lightingStrength) * dist * dist)
        val lightFactor = (0.16f * lightingStrength + 0.84f * attenuation).coerceIn(0f, 1f)

        val topBonus = if (pts == faces[2]) 1.2f else 0.88f
        val finalFactor = (lightFactor * topBonus).coerceIn(0f, 1f)

        val shadedCol = Color(
            red = (color.red * finalFactor).coerceIn(0f, 1f),
            green = (color.green * finalFactor).coerceIn(0f, 1f),
            blue = (color.blue * finalFactor).coerceIn(0f, 1f),
            alpha = 1f
        )

        outList.add(RenderItem3D.Polygon(pts, shadedCol, depth = 0f))
    }
}

private fun buildVolumetricOctahedron(
    cx: Float, cy: Float, cz: Float,
    radX: Float, radY: Float, radZ: Float,
    color: Color,
    outList: MutableList<RenderItem3D>,
    lightSource: Vector3,
    lightingStrength: Float
) {
    val top = Vector3(cx, cy - radY, cz)
    val bottom = Vector3(cx, cy + radY, cz)
    val v1 = Vector3(cx - radX, cy, cz - radZ)
    val v2 = Vector3(cx + radX, cy, cz - radZ)
    val v3 = Vector3(cx + radX, cy, cz + radZ)
    val v4 = Vector3(cx - radX, cy, cz + radZ)

    val faces = listOf(
        listOf(top, v2, v1),
        listOf(top, v3, v2),
        listOf(top, v4, v3),
        listOf(top, v1, v4),
        listOf(bottom, v1, v2),
        listOf(bottom, v2, v3),
        listOf(bottom, v3, v4),
        listOf(bottom, v4, v1)
    )

    faces.forEach { pts ->
        val faceCenter = Vector3(
            pts.sumOf { it.x.toDouble() }.toFloat() / 3f,
            pts.sumOf { it.y.toDouble() }.toFloat() / 3f,
            pts.sumOf { it.z.toDouble() }.toFloat() / 3f
        )
        val dist = (faceCenter - lightSource).length()
        val attenuation = 1f / (1.0f + (0.001f / lightingStrength) * dist * dist)
        val lightFactor = (0.22f * lightingStrength + 0.78f * attenuation).coerceIn(0f, 1f)

        val shadedCol = Color(
            red = (color.red * lightFactor).coerceIn(0f, 1f),
            green = (color.green * lightFactor).coerceIn(0f, 1f),
            blue = (color.blue * lightFactor).coerceIn(0f, 1f),
            alpha = 1f
        )

        outList.add(RenderItem3D.Polygon(pts, shadedCol, depth = 0f))
    }
}
