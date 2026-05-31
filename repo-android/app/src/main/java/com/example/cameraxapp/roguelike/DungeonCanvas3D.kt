package com.example.cameraxapp.roguelike

import android.graphics.Paint as AndroidPaint
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import com.example.cameraxapp.core.math3d.RenderItem3D
import com.example.cameraxapp.core.math3d.Vector3
import kotlin.math.*
import kotlinx.coroutines.launch

@Composable
fun DungeonCanvas3D(
    tiles: List<GameTile>,
    monsters: List<MonsterState>,
    pX: Int,
    pY: Int,
    heroClass: String,
    modifier: Modifier = Modifier
) {
    var yawAngle by remember { mutableStateOf(-0.65f) }
    var pitchAngle by remember { mutableStateOf(0.75f) }
    var zoomScale by remember { mutableStateOf(4.8f) }
    var lightingStrength by remember { mutableStateOf(1.5f) }
    val coroutineScope = rememberCoroutineScope()

    var fps by remember { mutableStateOf(60) }
    LaunchedEffect(Unit) {
        var frameCount = 0
        var lastTime = System.currentTimeMillis()
        while (true) {
            withFrameNanos {
                frameCount++
                val now = System.currentTimeMillis()
                val elapsed = now - lastTime
                if (elapsed >= 1000) {
                    fps = (frameCount * 1000 / elapsed).toInt()
                    frameCount = 0
                    lastTime = now
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "DungeonOrbit")
    val timeAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitTime"
    )

    val animPX by animateFloatAsState(
        targetValue = pX.toFloat(),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "PlayerX"
    )
    val animPY by animateFloatAsState(
        targetValue = pY.toFloat(),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "PlayerY"
    )

    // Track smooth state transitions for individual monsters to prevent instant snapping when they move
    val monsterAnims = remember { mutableStateMapOf<Int, Pair<Animatable<Float, AnimationVector1D>, Animatable<Float, AnimationVector1D>>>() }

    // Synchronize animation targets and clean up obsolete IDs
    val currentIds = remember(monsters) { monsters.map { it.id }.toSet() }
    val obsoleteIds = monsterAnims.keys.filter { it !in currentIds }
    obsoleteIds.forEach { monsterAnims.remove(it) }

    monsters.forEach { monster ->
        val entry = monsterAnims.getOrPut(monster.id) {
            Pair(
                Animatable(monster.x.toFloat()),
                Animatable(monster.y.toFloat())
            )
        }
        LaunchedEffect(monster.id, monster.x) {
            entry.first.animateTo(monster.x.toFloat(), animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
        }
        LaunchedEffect(monster.id, monster.y) {
            entry.second.animateTo(monster.y.toFloat(), animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
        }
    }

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
                    pitchAngle = (pitchAngle + pan.y * 0.007f).coerceIn(0.15f, 1.48f)
                    zoomScale = (zoomScale * zoom).coerceIn(0.15f, 10.0f)
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

            // Orbit follow camera: offset and rotate all vectors relative to the player's character position
            val pWorldX = (animPX - 9f) * W_s
            val pWorldZ = (animPY - 9f) * W_s
            val targetCenter = Vector3(pWorldX, 0f, pWorldZ)

            val cosYaw = cos(yawAngle)
            val sinYaw = sin(yawAngle)
            val cosPitch = cos(pitchAngle)
            val sinPitch = sin(pitchAngle)

            class PrecalculatedPoint(val rotZ: Float, val proj: Offset)
            val vertexCache = mutableMapOf<Vector3, PrecalculatedPoint>()

            fun getOrComputePoint(v: Vector3): PrecalculatedPoint {
                return vertexCache.getOrPut(v) {
                    val rel = v - targetCenter
                    val rx = rel.x * cosYaw + rel.z * sinYaw
                    val ryHalf = -rel.x * sinYaw + rel.z * cosYaw
                    val ry = rel.y * cosPitch - ryHalf * sinPitch
                    val rz = rel.y * sinPitch + ryHalf * cosPitch

                    val denom = rz + cameraZ
                    val sx = cX + (rx * dFactor * zoomScale) / if (denom != 0f) denom else 1f
                    val sy = cY + (ry * dFactor * zoomScale) / if (denom != 0f) denom else 1f
                    PrecalculatedPoint(rz, Offset(sx, sy))
                }
            }

            // Projections math mapping using vertex caching
            fun projectPoint(v: Vector3): Offset {
                return getOrComputePoint(v).proj
            }

            fun getRotatedZDeep(v: Vector3): Float {
                return getOrComputePoint(v).rotZ
            }

            // Define light source centered at Player position (Y is light height overhead)
            val pLightX = (animPX - 9f) * W_s
            val pLightZ = (animPY - 9f) * W_s
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

                        if (tile.tileType != "STAIRS_DOWN") {
                            drawPipeline.add(RenderItem3D.Polygon(corners, shadedCol, depth = 0f))
                        }

                        // Procedural weathered flagstone lines on ground tiles
                        if (tile.tileType != "STAIRS_DOWN" && tile.tileType != "CHEST") {
                            val flagLineCol = Color(0xFF2B2825).copy(alpha = 0.6f)
                            val floorYOffset = floorY - 0.2f
                            
                            // Outer borders
                            drawPipeline.add(RenderItem3D.Line(Vector3(tileX - half, floorYOffset, tileZ - half), Vector3(tileX + half, floorYOffset, tileZ - half), flagLineCol, 1.2f, 0f))
                            drawPipeline.add(RenderItem3D.Line(Vector3(tileX + half, floorYOffset, tileZ - half), Vector3(tileX + half, floorYOffset, tileZ + half), flagLineCol, 1.2f, 0f))
                            drawPipeline.add(RenderItem3D.Line(Vector3(tileX + half, floorYOffset, tileZ + half), Vector3(tileX - half, floorYOffset, tileZ + half), flagLineCol, 1.2f, 0f))
                            drawPipeline.add(RenderItem3D.Line(Vector3(tileX - half, floorYOffset, tileZ + half), Vector3(tileX - half, floorYOffset, tileZ - half), flagLineCol, 1.2f, 0f))

                            // Staggered paving joints
                            if ((tile.x + tile.y) % 2 == 1) {
                                drawPipeline.add(RenderItem3D.Line(Vector3(tileX, floorYOffset, tileZ - half), Vector3(tileX, floorYOffset, tileZ + half), flagLineCol, 0.8f, 0f))
                            } else {
                                drawPipeline.add(RenderItem3D.Line(Vector3(tileX - half, floorYOffset, tileZ), Vector3(tileX + half, floorYOffset, tileZ), flagLineCol, 0.8f, 0f))
                            }
                        }

                        if (tile.tileType == "STAIRS_DOWN") {
                            // Render 3D stairs stepping downward
                            build3DStairs(
                                cx = tileX, cy = floorY, cz = tileZ,
                                sizeX = W_s, sizeY = W_s * 1.1f, sizeZ = W_s,
                                outList = drawPipeline,
                                lightSource = lightSource,
                                lightingStrength = lightingStrength
                            )
                        } else if (tile.tileType == "CHEST") {
                            // Render 3D chest with wood paneling, iron straps and dual brass locks
                            build3DChest(
                                cx = tileX, cy = floorY - 2.5f, cz = tileZ,
                                sizeX = W_s * 0.52f, sizeY = W_s * 0.45f, sizeZ = W_s * 0.42f,
                                outList = drawPipeline,
                                lightSource = lightSource,
                                lightingStrength = lightingStrength
                            )
                        }
                    }
                }
            }

            // 2. Build monsters volumetric shapes with status overlays, sliding transitions, and breathing bobbing curves
            monsters.forEach { monster ->
                val monsterTileRevealed = tiles.find { it.x == monster.x && it.y == monster.y }?.revealed ?: false
                if (monsterTileRevealed) {
                    val mAnimX = monsterAnims[monster.id]?.first?.value ?: monster.x.toFloat()
                    val mAnimY = monsterAnims[monster.id]?.second?.value ?: monster.y.toFloat()
                    val mX = (mAnimX - 9f) * W_s
                    val mZ = (mAnimY - 9f) * W_s
                    
                    val monsterBobbing = sin(timeAngle * 2.2f + (monster.id * 0.5f)) * (W_s * 0.05f)
                    val mY = W_s * 0.15f + monsterBobbing

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

                    // Build upgraded 3D low poly entity representation
                    buildProceduralMonsterLowPoly(
                        cx = mX, cy = mY, cz = mZ,
                        radX = W_s * 0.32f, radY = W_s * 0.45f, radZ = W_s * 0.32f,
                        monsterType = monster.type,
                        baseColor = monsterCol,
                        outList = drawPipeline,
                        lightSource = lightSource,
                        lightingStrength = lightingStrength
                    )

                    // Type-specific animated indicators
                    when (monster.type) {
                        "DRAGON" -> {
                            val numSegs = 10
                            val pulseRadius = W_s * (0.65f + 0.15f * sin(timeAngle * 3f))
                            for (i in 0 until numSegs) {
                                val a1 = timeAngle + (i * 2f * PI.toFloat() / numSegs)
                                val a2 = timeAngle + ((i + 1) * 2f * PI.toFloat() / numSegs)
                                val startPt = Vector3(mX + pulseRadius * cos(a1), mY, mZ + pulseRadius * sin(a1))
                                val endPt = Vector3(mX + pulseRadius * cos(a2), mY, mZ + pulseRadius * sin(a2))
                                drawPipeline.add(RenderItem3D.Line(startPt, endPt, Color(0xFFEF5350), 3f, 0f))
                            }
                        }
                        "NECROMANCER" -> {
                            val numSegs = 6
                            val ringRadius = W_s * 0.45f
                            for (i in 0 until numSegs) {
                                val a1 = -timeAngle * 1.2f + (i * 2f * PI.toFloat() / numSegs)
                                val a2 = -timeAngle * 1.2f + ((i + 1) * 2f * PI.toFloat() / numSegs)
                                val startPt = Vector3(mX + ringRadius * cos(a1), mY - W_s * 0.15f, mZ + ringRadius * sin(a1))
                                val endPt = Vector3(mX + ringRadius * cos(a2), mY - W_s * 0.15f, mZ + ringRadius * sin(a2))
                                drawPipeline.add(RenderItem3D.Line(startPt, endPt, Color(0xFFBA68C8), 2.2f, 0f))
                            }
                        }
                        "GOBLIN" -> {
                            val numSegs = 6
                            val ringRadius = W_s * 0.42f
                            for (i in 0 until numSegs) {
                                val a1 = timeAngle * 1.8f + (i * 2f * PI.toFloat() / numSegs)
                                val a2 = timeAngle * 1.8f + ((i + 1) * 2f * PI.toFloat() / numSegs)
                                val startPt = Vector3(mX + ringRadius * cos(a1), mY + W_s * 0.1f, mZ + ringRadius * sin(a1))
                                val endPt = Vector3(mX + ringRadius * cos(a2), mY + W_s * 0.1f, mZ + ringRadius * sin(a2))
                                drawPipeline.add(RenderItem3D.Line(startPt, endPt, Color(0xFF81C784), 1.8f, 0f))
                            }
                        }
                    }

                    // Add floating tag emoji above the entity heading
                    drawPipeline.add(RenderItem3D.TextLabel(Vector3(mX, mY - W_s * 0.70f, mZ), glyph, Color.White, sizeMultiplier = 1.0f, depth = 0f))
                    
                    val hpPercent = (monster.currentHp * 100 / monster.maxHp).toString() + "%"
                    drawPipeline.add(RenderItem3D.TextLabel(Vector3(mX, mY - W_s * 1.05f, mZ), hpPercent, Color(0xFF81C784), sizeMultiplier = 0.65f, depth = 0f))
                }
            }

            // 3. Build player hero entity shape with subtle breathing float animation
            val playerBobbing = sin(timeAngle * 2.5f) * (W_s * 0.05f)
            val pWorldY = W_s * 0.2f + playerBobbing

            val playerCol = Color(0xFFFF9100)
            val pGlyph = when (heroClass) {
                "Warrior" -> "🛡️"
                "Mage" -> "🧙"
                else -> "🗡️"
            }

            // Core 3D player model
            buildProceduralPlayerLowPoly(
                cx = pWorldX, cy = pWorldY, cz = pWorldZ,
                radX = W_s * 0.30f, radY = W_s * 0.45f, radZ = W_s * 0.30f,
                heroClass = heroClass,
                outList = drawPipeline,
                lightSource = lightSource,
                lightingStrength = lightingStrength
            )

            // Dynamic float action labels with breathing float sequence
            drawPipeline.add(RenderItem3D.TextLabel(Vector3(pWorldX, pWorldY - W_s * 0.85f, pWorldZ), pGlyph, Color.White, sizeMultiplier = 1.1f, depth = 0f))
            drawPipeline.add(RenderItem3D.TextLabel(Vector3(pWorldX, pWorldY - W_s * 1.25f, pWorldZ), "HERO", Color(0xFFFFD700), sizeMultiplier = 0.65f, depth = 0f))

            // Class-specific animated graphic rings (Waist Orbitals)
            when (heroClass) {
                "Warrior" -> {
                    val numSegs = 8
                    val ringRadius = W_s * 0.5f
                    for (i in 0 until numSegs) {
                        val a1 = timeAngle + (i * 2f * PI.toFloat() / numSegs)
                        val a2 = timeAngle + ((i + 1) * 2f * PI.toFloat() / numSegs)
                        val startPt = Vector3(pWorldX + ringRadius * cos(a1), pWorldY, pWorldZ + ringRadius * sin(a1))
                        val endPt = Vector3(pWorldX + ringRadius * cos(a2), pWorldY, pWorldZ + ringRadius * sin(a2))
                        drawPipeline.add(RenderItem3D.Line(startPt, endPt, Color(0xFFFFD700), 2.5f, 0f))
                    }
                }
                "Mage" -> {
                    val numSegs = 8
                    val ringRadius = W_s * 0.48f
                    for (i in 0 until numSegs) {
                        val a1 = -timeAngle * 1.5f + (i * 2f * PI.toFloat() / numSegs)
                        val a2 = -timeAngle * 1.5f + ((i + 1) * 2f * PI.toFloat() / numSegs)
                        val startPt = Vector3(
                            pWorldX + ringRadius * cos(a1),
                            pWorldY - W_s * 0.2f + 2f * sin(a1),
                            pWorldZ + ringRadius * sin(a1)
                        )
                        val endPt = Vector3(
                            pWorldX + ringRadius * cos(a2),
                            pWorldY - W_s * 0.2f + 2f * sin(a2),
                            pWorldZ + ringRadius * sin(a2)
                        )
                        drawPipeline.add(RenderItem3D.Line(startPt, endPt, Color(0xFF64B5F6), 2.0f, 0f))
                    }
                }
                else -> { // Rogue
                    val numSegs = 8
                    val ringRadius = W_s * 0.52f
                    for (i in 0 until numSegs) {
                        val a1 = timeAngle * 2.0f + (i * 2f * PI.toFloat() / numSegs)
                        val a2 = timeAngle * 2.0f + ((i + 1) * 2f * PI.toFloat() / numSegs)
                        val startPt = Vector3(pWorldX + ringRadius * cos(a1), pWorldY + W_s * 0.1f, pWorldZ + ringRadius * sin(a1))
                        val endPt = Vector3(pWorldX + ringRadius * cos(a2), pWorldY + W_s * 0.1f, pWorldZ + ringRadius * sin(a2))
                        drawPipeline.add(RenderItem3D.Line(startPt, endPt, Color(0xFFCE93D8), 1.8f, 0f))
                    }
                }
            }

            // matched direction orientation helper colors
            val ColorNorth = Color(0xFF3A86C8) // Sky Blue
            val ColorSouth = Color(0xFFE76F51) // Warm Coral / Orange
            val ColorWest = Color(0xFF9B5DE5)  // Orchid Purple
            val ColorEast = Color(0xFF2EC4B6)  // Teal Green

            // 3D Axis Helpers to show player orientation in world space at feet height
            val floorY = W_s * 0.5f
            val floorAxisY = floorY - 2f
            val axisLength = W_s * 1.2f
            val labelOffset = W_s * 1.6f

            // North (-Z)
            drawPipeline.add(RenderItem3D.Line(
                start = Vector3(pWorldX, floorAxisY, pWorldZ),
                end = Vector3(pWorldX, floorAxisY, pWorldZ - axisLength),
                color = ColorNorth,
                strokeWidth = 3f,
                depth = 0f
            ))
            drawPipeline.add(RenderItem3D.TextLabel(
                position = Vector3(pWorldX, floorAxisY, pWorldZ - labelOffset),
                text = "N",
                color = ColorNorth,
                sizeMultiplier = 0.8f,
                depth = 0f
            ))

            // South (+Z)
            drawPipeline.add(RenderItem3D.Line(
                start = Vector3(pWorldX, floorAxisY, pWorldZ),
                end = Vector3(pWorldX, floorAxisY, pWorldZ + axisLength),
                color = ColorSouth,
                strokeWidth = 3f,
                depth = 0f
            ))
            drawPipeline.add(RenderItem3D.TextLabel(
                position = Vector3(pWorldX, floorAxisY, pWorldZ + labelOffset),
                text = "S",
                color = ColorSouth,
                sizeMultiplier = 0.8f,
                depth = 0f
            ))

            // West (+X) - Swapped to fix visual inversion relative to pad
            drawPipeline.add(RenderItem3D.Line(
                start = Vector3(pWorldX, floorAxisY, pWorldZ),
                end = Vector3(pWorldX + axisLength, floorAxisY, pWorldZ),
                color = ColorWest,
                strokeWidth = 3f,
                depth = 0f
            ))
            drawPipeline.add(RenderItem3D.TextLabel(
                position = Vector3(pWorldX + labelOffset, floorAxisY, pWorldZ),
                text = "W",
                color = ColorWest,
                sizeMultiplier = 0.8f,
                depth = 0f
            ))

            // East (-X) - Swapped to fix visual inversion relative to pad
            drawPipeline.add(RenderItem3D.Line(
                start = Vector3(pWorldX, floorAxisY, pWorldZ),
                end = Vector3(pWorldX - axisLength, floorAxisY, pWorldZ),
                color = ColorEast,
                strokeWidth = 3f,
                depth = 0f
            ))
            drawPipeline.add(RenderItem3D.TextLabel(
                position = Vector3(pWorldX - labelOffset, floorAxisY, pWorldZ),
                text = "E",
                color = ColorEast,
                sizeMultiplier = 0.8f,
                depth = 0f
            ))

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
                    is RenderItem3D.Line -> {
                        val pStart = projectPoint(item.start)
                        val pEnd = projectPoint(item.end)
                        drawLine(
                            color = item.color,
                            start = pStart,
                            end = pEnd,
                            strokeWidth = item.strokeWidth
                        )
                    }
                    is RenderItem3D.Sphere -> {
                        val pCenter = projectPoint(item.center)
                        val rotZ = getRotatedZDeep(item.center)
                        val denom = rotZ + cameraZ
                        val radiusRatio = if (denom > 0f) dFactor / denom else 1f
                        val screenRadius = item.radius * zoomScale * radiusRatio
                        if (screenRadius > 0.5f) {
                            drawCircle(
                                color = item.color,
                                center = pCenter,
                                radius = screenRadius
                            )
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.35f),
                                center = pCenter,
                                radius = screenRadius,
                                style = Stroke(width = 0.8f)
                            )
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

            // -------------------------------------------------------------
            // 3D HUD Compass (UI overlay drawn on top of the 3D scene)
            // -------------------------------------------------------------
            val compassRadius = 24.dp.toPx()
            val hudCX = 45.dp.toPx()
            val hudCY = 85.dp.toPx()
            val hudCenter = Offset(hudCX, hudCY)

            // Draw circular backings
            drawCircle(
                color = Color.Black.copy(alpha = 0.65f),
                radius = compassRadius,
                center = hudCenter
            )
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.3f),
                radius = compassRadius,
                center = hudCenter,
                style = Stroke(width = 1.dp.toPx())
            )

            // Axes definitions
            val lineLen = compassRadius * 0.62f
            val labelLen = compassRadius * 0.98f

            fun rotateForHud(v: Vector3): Vector3 {
                val rx = v.x * cosYaw + v.z * sinYaw
                val ryHalf = -v.x * sinYaw + v.z * cosYaw
                val ry = v.y * cosPitch - ryHalf * sinPitch
                val rz = v.y * sinPitch + ryHalf * cosPitch
                return Vector3(rx, ry, rz)
            }

            // Direction Vectors (W/E Swapped to fix visual inversion relative to pad)
            val rotN = rotateForHud(Vector3(0f, 0f, -1f))
            val rotS = rotateForHud(Vector3(0f, 0f, 1f))
            val rotW = rotateForHud(Vector3(1f, 0f, 0f))
            val rotE = rotateForHud(Vector3(-1f, 0f, 0f))

            // Helper to draw compass leg & text
            fun drawCompassLeg(rotDir: Vector3, label: String, color: Color) {
                val endPos = hudCenter + Offset(rotDir.x * lineLen, rotDir.y * lineLen)
                drawLine(
                    color = color,
                    start = hudCenter,
                    end = endPos,
                    strokeWidth = 2.dp.toPx()
                )
                
                // Slightly adjust baseline for centered letters
                val labelPos = hudCenter + Offset(rotDir.x * labelLen, rotDir.y * labelLen)
                drawIntoCanvas { canvas ->
                    textPainter.color = color.toArgb()
                    textPainter.textSize = 8.dp.toPx()
                    canvas.nativeCanvas.drawText(
                        label,
                        labelPos.x,
                        labelPos.y + 3.dp.toPx(),
                        textPainter
                    )
                }
            }

            // Draw all 4 legs
            drawCompassLeg(rotN, "N", ColorNorth)
            drawCompassLeg(rotS, "S", ColorSouth)
            drawCompassLeg(rotW, "W", ColorWest)
            drawCompassLeg(rotE, "E", ColorEast)
        }

        var showSettings by remember { mutableStateOf(false) }

        // Top Controls Panel (Instructions & Gear button)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "🖐️ SWIPE TO ROTATE • PINCH TO ZOOM",
                    fontSize = 8.sp,
                    color = Color(0xFFFFD700),
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Real-time FPS Dashboard Badge (Stylized Graphical representation)
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                    .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "⚡",
                    fontSize = 8.sp,
                    color = Color(0xFFFFD700)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(bottom = 1.dp)
                ) {
                    val activeColor = when {
                        fps >= 50 -> Color(0xFF4CAF50) // Emerald/Green
                        fps >= 30 -> Color(0xFFFFC107) // Amber/Yellow
                        else -> Color(0xFFF44336)      // Crimson/Red
                    }
                    val numLit = when {
                        fps >= 55 -> 4
                        fps >= 40 -> 3
                        fps >= 22 -> 2
                        else -> 1
                    }
                    for (barIdx in 1..4) {
                        val barHeight = 4.dp + (barIdx * 2).dp
                        val color = if (barIdx <= numLit) activeColor else Color.DarkGray.copy(alpha = 0.4f)
                        Box(
                            modifier = Modifier
                                .size(2.dp, barHeight)
                                .background(color, RoundedCornerShape(0.5.dp))
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .background(if (showSettings) Color(0xFFFFD700) else Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .clickable { showSettings = !showSettings }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (showSettings) "CLOSE 🛠️" else "VIEW OPT ⚙️",
                    fontSize = 8.5.sp,
                    color = if (showSettings) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Expanded floating settings panel dropdown menu
        if (showSettings) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 44.dp, end = 8.dp)
                    .width(220.dp)
                    .background(Color.Black.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🛠️ DISPLAY CONFIG",
                    fontSize = 11.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Row 1: Zoom Control
                Row(
                    modifier = Modifier.padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🔍 ZOOM: ${(zoomScale * 100).toInt()}%",
                        fontSize = 9.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Color(0xFF222222), RoundedCornerShape(4.dp))
                                .clickable { zoomScale = (zoomScale - 0.15f).coerceIn(0.15f, 10.0f) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Color(0xFF222222), RoundedCornerShape(4.dp))
                                .clickable { zoomScale = (zoomScale + 0.15f).coerceIn(0.15f, 10.0f) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .height(22.dp)
                                .padding(horizontal = 5.dp)
                                .background(Color(0xFF333333), RoundedCornerShape(4.dp))
                                .clickable { zoomScale = 4.8f },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("RST", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Row: Camera Presets Sweep Controls
                Text(
                    text = "🎥 CAMERA SWEEP PRESETS",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(top = 2.dp, bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val cameraConfigs = listOf(
                        Triple("ISO", -0.65f, 0.75f),
                        Triple("TOP", 0f, 1.45f),
                        Triple("FRONT", 0f, 0.35f)
                    )
                    cameraConfigs.forEach { (label, targetYaw, targetPitch) ->
                        val active = abs(yawAngle - targetYaw) < 0.08f && abs(pitchAngle - targetPitch) < 0.08f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .background(if (active) Color(0xFFFFD700) else Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        launch {
                                            animate(
                                                initialValue = yawAngle,
                                                targetValue = targetYaw,
                                                animationSpec = tween(650, easing = FastOutSlowInEasing)
                                            ) { v, _ -> yawAngle = v }
                                        }
                                        launch {
                                            animate(
                                                initialValue = pitchAngle,
                                                targetValue = targetPitch,
                                                animationSpec = tween(650, easing = FastOutSlowInEasing)
                                            ) { v, _ -> pitchAngle = v }
                                        }
                                    }
                                    val targetZoom = when (label) {
                                        "TOP" -> 6.0f
                                        "FRONT" -> 5.4f
                                        else -> 4.8f
                                    }
                                    coroutineScope.launch {
                                        animate(
                                            initialValue = zoomScale,
                                            targetValue = targetZoom,
                                            animationSpec = tween(650, easing = FastOutSlowInEasing)
                                        ) { v, _ -> zoomScale = v }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.Black else Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Custom separator line spacer
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(bottom = 8.dp)
                )

                // Row 2: Lighting Control
                Text(
                    text = "💡 TORCH STRENGTH",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        "DIM" to 0.8f,
                        "TORCH" to 1.5f,
                        "LANTERN" to 2.2f,
                        "SUN" to 3.0f
                    )
                    presets.forEach { (label, value) ->
                        val active = lightingStrength == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .background(if (active) Color(0xFFFF9100) else Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                                .clickable { lightingStrength = value },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.Black else Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
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
        listOf(v1, v2, v3, v4), // back face
        listOf(v6, v5, v8, v7), // front face
        listOf(v1, v5, v6, v2), // top face
        listOf(v4, v3, v7, v8), // bottom face
        listOf(v1, v4, v8, v5), // left face
        listOf(v2, v6, v7, v3)  // right face
    )

    faces.forEachIndexed { index, pts ->
        val faceCenter = Vector3(
            pts.sumOf { it.x.toDouble() }.toFloat() / 4f,
            pts.sumOf { it.y.toDouble() }.toFloat() / 4f,
            pts.sumOf { it.z.toDouble() }.toFloat() / 4f
        )
        val dist = (faceCenter - lightSource).length()
        val attenuation = 1f / (1.0f + (0.0016f / lightingStrength) * dist * dist)
        val lightFactor = (0.16f * lightingStrength + 0.84f * attenuation).coerceIn(0f, 1f)

        val topBonus = if (index == 2) 1.2f else 0.88f
        val finalFactor = (lightFactor * topBonus).coerceIn(0f, 1f)

        val shadedCol = Color(
            red = (color.red * finalFactor).coerceIn(0f, 1f),
            green = (color.green * finalFactor).coerceIn(0f, 1f),
            blue = (color.blue * finalFactor).coerceIn(0f, 1f),
            alpha = 1f
        )

        outList.add(RenderItem3D.Polygon(pts, shadedCol, depth = 0f))

        // Procedural bevel and masonry joint outlines (Z-fighting is resolved by slight outward offset vectors)
        val mortarCol = Color(0xFF1E1610).copy(alpha = 0.65f)
        val lineW = 1.2f

        when (index) {
            1 -> { // Front face (Z = cz + hz + 0.15f)
                val zO = cz + hz + 0.15f
                val y1 = cy - hy * 0.33f
                val y2 = cy + hy * 0.33f
                outList.add(RenderItem3D.Line(Vector3(cx - hx, y1, zO), Vector3(cx + hx, y1, zO), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(cx - hx, y2, zO), Vector3(cx + hx, y2, zO), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(cx, cy - hy, zO), Vector3(cx, y1, zO), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(cx - hx * 0.5f, y1, zO), Vector3(cx - hx * 0.5f, y2, zO), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(cx + hx * 0.5f, y1, zO), Vector3(cx + hx * 0.5f, y2, zO), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(cx, y2, zO), Vector3(cx, cy + hy, zO), mortarCol, lineW, 0f))
            }
            4 -> { // Left face (X = cx - hx - 0.15f)
                val xO = cx - hx - 0.15f
                val y1 = cy - hy * 0.33f
                val y2 = cy + hy * 0.33f
                outList.add(RenderItem3D.Line(Vector3(xO, y1, cz - hz), Vector3(xO, y1, cz + hz), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(xO, y2, cz - hz), Vector3(xO, y2, cz + hz), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(xO, cy - hy, cz), Vector3(xO, y1, cz), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(xO, y1, cz - hz * 0.5f), Vector3(xO, y2, cz - hz * 0.5f), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(xO, y1, cz + hz * 0.5f), Vector3(xO, y2, cz + hz * 0.5f), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(xO, y2, cz), Vector3(xO, cy + hy, cz), mortarCol, lineW, 0f))
            }
            5 -> { // Right face (X = cx + hx + 0.15f)
                val xO = cx + hx + 0.15f
                val y1 = cy - hy * 0.33f
                val y2 = cy + hy * 0.33f
                outList.add(RenderItem3D.Line(Vector3(xO, y1, cz - hz), Vector3(xO, y1, cz + hz), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(xO, y2, cz - hz), Vector3(xO, y2, cz + hz), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(xO, cy - hy, cz), Vector3(xO, y1, cz), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(xO, y1, cz - hz * 0.5f), Vector3(xO, y2, cz - hz * 0.5f), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(xO, y1, cz + hz * 0.5f), Vector3(xO, y2, cz + hz * 0.5f), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(xO, y2, cz), Vector3(xO, cy + hy, cz), mortarCol, lineW, 0f))
            }
            2 -> { // Top face (Y = cy - hy - 0.15f)
                val yO = cy - hy - 0.15f
                outList.add(RenderItem3D.Line(Vector3(cx, yO, cz - hz), Vector3(cx, yO, cz + hz), mortarCol, lineW, 0f))
                outList.add(RenderItem3D.Line(Vector3(cx - hx, yO, cz), Vector3(cx + hx, yO, cz), mortarCol, lineW, 0f))
            }
        }
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

private fun build3DStairs(
    cx: Float, cy: Float, cz: Float,
    sizeX: Float, sizeY: Float, sizeZ: Float,
    outList: MutableList<RenderItem3D>,
    lightSource: Vector3,
    lightingStrength: Float
) {
    val stoneCol = Color(0xFFFF9100)
    val hx = sizeX / 2f
    val hz = sizeZ / 2f
    val numSteps = 4
    val stepZSize = sizeZ / numSteps

    for (i in 0 until numSteps) {
        val stepTopY = cy + (i + 1) * (sizeY / (numSteps + 1))
        val zStart = cz - hz + i * stepZSize
        val zEnd = cz - hz + (i + 1) * stepZSize

        val p1 = Vector3(cx - hx, stepTopY, zStart)
        val p2 = Vector3(cx + hx, stepTopY, zStart)
        val p3 = Vector3(cx + hx, stepTopY, zEnd)
        val p4 = Vector3(cx - hx, stepTopY, zEnd)

        val faceCenter = Vector3(cx, stepTopY, (zStart + zEnd) / 2f)
        val dist = (faceCenter - lightSource).length()
        val attenuation = 1f / (1.0f + (0.0016f / lightingStrength) * dist * dist)
        val lightFactor = (0.15f * lightingStrength + 0.85f * attenuation).coerceIn(0.12f, 1f)

        val shadedStepCol = Color(
            red = (stoneCol.red * lightFactor).coerceIn(0f, 1f),
            green = (stoneCol.green * lightFactor).coerceIn(0f, 1f),
            blue = (stoneCol.blue * lightFactor).coerceIn(0f, 1f),
            alpha = 1f
        )

        outList.add(RenderItem3D.Polygon(listOf(p1, p2, p3, p4), shadedStepCol, depth = 0f))

        val nextTopY = if (i == numSteps - 1) cy + sizeY else cy + (i + 2) * (sizeY / (numSteps + 1))
        val vf1 = Vector3(cx - hx, stepTopY, zEnd)
        val vf2 = Vector3(cx + hx, stepTopY, zEnd)
        val vf3 = Vector3(cx + hx, nextTopY, zEnd)
        val vf4 = Vector3(cx - hx, nextTopY, zEnd)

        val frontFaceCol = Color(
            red = (stoneCol.red * lightFactor * 0.72f).coerceIn(0f, 1f),
            green = (stoneCol.green * lightFactor * 0.72f).coerceIn(0f, 1f),
            blue = (stoneCol.blue * lightFactor * 0.72f).coerceIn(0f, 1f),
            alpha = 1f
        )
        outList.add(RenderItem3D.Polygon(listOf(vf1, vf2, vf3, vf4), frontFaceCol, depth = 0f))

        val edgeCol = Color(0xFFFFCC80).copy(alpha = 0.5f)
        outList.add(RenderItem3D.Line(vf1, vf2, edgeCol, 1.5f, 0f))
    }
}

private fun build3DChest(
    cx: Float, cy: Float, cz: Float,
    sizeX: Float, sizeY: Float, sizeZ: Float,
    outList: MutableList<RenderItem3D>,
    lightSource: Vector3,
    lightingStrength: Float
) {
    val woodCol = Color(0xFF4E342E)
    val goldCol = Color(0xFFFFB300)
    val ironCol = Color(0xFF263238)

    val hx = sizeX / 2f
    val hy = sizeY / 2f
    val hz = sizeZ / 2f

    val baseBottomY = cy + hy * 0.35f
    val baseTopY = cy - hy * 0.15f
    val lidPeakY = cy - hy * 0.95f

    val b1 = Vector3(cx - hx, baseBottomY, cz - hz)
    val b2 = Vector3(cx + hx, baseBottomY, cz - hz)
    val b3 = Vector3(cx + hx, baseTopY, cz - hz)
    val b4 = Vector3(cx - hx, baseTopY, cz - hz)

    val b5 = Vector3(cx - hx, baseBottomY, cz + hz)
    val b6 = Vector3(cx + hx, baseBottomY, cz + hz)
    val b7 = Vector3(cx + hx, baseTopY, cz + hz)
    val b8 = Vector3(cx - hx, baseTopY, cz + hz)

    val lowerBoxFaces = listOf(
        listOf(b1, b2, b3, b4),
        listOf(b6, b5, b8, b7),
        listOf(b1, b5, b8, b4),
        listOf(b2, b6, b7, b3),
        listOf(b3, b7, b8, b4)
    )

    lowerBoxFaces.forEachIndexed { fIdx, pts ->
        val faceCenter = Vector3(
            pts.sumOf { it.x.toDouble() }.toFloat() / 4f,
            pts.sumOf { it.y.toDouble() }.toFloat() / 4f,
            pts.sumOf { it.z.toDouble() }.toFloat() / 4f
        )
        val dist = (faceCenter - lightSource).length()
        val attenuation = 1f / (1.0f + (0.0016f / lightingStrength) * dist * dist)
        val lightFactor = (0.24f * lightingStrength + 0.76f * attenuation).coerceIn(0f, 1f)

        val sideShade = if (fIdx == 1) 1.15f else 0.88f
        val finalFactor = (lightFactor * sideShade).coerceIn(0f, 1f)

        val shadedWoodCol = Color(
            red = (woodCol.red * finalFactor).coerceIn(0f, 1f),
            green = (woodCol.green * finalFactor).coerceIn(0f, 1f),
            blue = (woodCol.blue * finalFactor).coerceIn(0f, 1f),
            alpha = 1f
        )
        outList.add(RenderItem3D.Polygon(pts, shadedWoodCol, depth = 0f))
    }

    val edgeOffset = 0.08f
    outList.add(RenderItem3D.Line(Vector3(cx - hx, baseBottomY, cz - hz - edgeOffset), Vector3(cx - hx, baseTopY, cz - hz - edgeOffset), ironCol, 2f, 0f))
    outList.add(RenderItem3D.Line(Vector3(cx + hx, baseBottomY, cz - hz - edgeOffset), Vector3(cx + hx, baseTopY, cz - hz - edgeOffset), ironCol, 2f, 0f))
    outList.add(RenderItem3D.Line(Vector3(cx - hx, baseBottomY, cz + hz + edgeOffset), Vector3(cx - hx, baseTopY, cz + hz + edgeOffset), ironCol, 2f, 0f))
    outList.add(RenderItem3D.Line(Vector3(cx + hx, baseBottomY, cz + hz + edgeOffset), Vector3(cx + hx, baseTopY, cz + hz + edgeOffset), ironCol, 2f, 0f))

    val lb1 = Vector3(cx - hx, baseTopY, cz - hz)
    val lb2 = Vector3(cx - hx * 0.75f, lidPeakY, cz - hz)
    val lb3 = Vector3(cx + hx * 0.75f, lidPeakY, cz - hz)
    val lb4 = Vector3(cx + hx, baseTopY, cz - hz)

    val lf1 = Vector3(cx - hx, baseTopY, cz + hz)
    val lf2 = Vector3(cx - hx * 0.75f, lidPeakY, cz + hz)
    val lf3 = Vector3(cx + hx * 0.75f, lidPeakY, cz + hz)
    val lf4 = Vector3(cx + hx, baseTopY, cz + hz)

    val lidFacets = listOf(
        listOf(lb1, lb2, lf2, lf1),
        listOf(lb2, lb3, lf3, lf2),
        listOf(lb3, lb4, lf4, lf3)
    )

    val backCapPts = listOf(lb1, lb2, lb3, lb4)
    val frontCapPts = listOf(lf1, lf2, lf3, lf4)

    listOf(backCapPts, frontCapPts).forEachIndexed { capIdx, capPts ->
        val faceCenter = Vector3(
            capPts.sumOf { it.x.toDouble() }.toFloat() / 4f,
            capPts.sumOf { it.y.toDouble() }.toFloat() / 4f,
            capPts.sumOf { it.z.toDouble() }.toFloat() / 4f
        )
        val dist = (faceCenter - lightSource).length()
        val attenuation = 1f / (1.0f + (0.0016f / lightingStrength) * dist * dist)
        val lightFactor = (0.24f * lightingStrength + 0.76f * attenuation).coerceIn(0f, 1f)
        val shade = if (capIdx == 1) 1.2f else 0.85f
        val finalFactor = (lightFactor * shade).coerceIn(0f, 1f)

        val shadedWoodCol = Color(
            red = (woodCol.red * finalFactor).coerceIn(0f, 1f),
            green = (woodCol.green * finalFactor).coerceIn(0f, 1f),
            blue = (woodCol.blue * finalFactor).coerceIn(0f, 1f),
            alpha = 1f
        )
        outList.add(RenderItem3D.Polygon(capPts, shadedWoodCol, depth = 0f))
    }

    lidFacets.forEachIndexed { fIdx, pts ->
        val faceCenter = Vector3(
            pts.sumOf { it.x.toDouble() }.toFloat() / 4f,
            pts.sumOf { it.y.toDouble() }.toFloat() / 4f,
            pts.sumOf { it.z.toDouble() }.toFloat() / 4f
        )
        val dist = (faceCenter - lightSource).length()
        val attenuation = 1f / (1.0f + (0.0016f / lightingStrength) * dist * dist)
        val lightFactor = (0.24f * lightingStrength + 0.76f * attenuation).coerceIn(0f, 1f)

        val slantShade = when (fIdx) {
            1 -> 1.3f
            else -> 0.95f
        }
        val finalFactor = (lightFactor * slantShade).coerceIn(0f, 1f)

        val shadedWoodCol = Color(
            red = (woodCol.red * finalFactor).coerceIn(0f, 1f),
            green = (woodCol.green * finalFactor).coerceIn(0f, 1f),
            blue = (woodCol.blue * finalFactor).coerceIn(0f, 1f),
            alpha = 1f
        )
        outList.add(RenderItem3D.Polygon(pts, shadedWoodCol, depth = 0f))
    }

    val lockZ = cz + hz + 0.12f
    val lockP1 = Vector3(cx - hx * 0.15f, baseTopY - hy * 0.12f, lockZ)
    val lockP2 = Vector3(cx + hx * 0.15f, baseTopY - hy * 0.12f, lockZ)
    val lockP3 = Vector3(cx + hx * 0.15f, baseTopY + hy * 0.28f, lockZ)
    val lockP4 = Vector3(cx - hx * 0.15f, baseTopY + hy * 0.28f, lockZ)

    val distL = (Vector3(cx, baseTopY, lockZ) - lightSource).length()
    val attenL = 1f / (1.0f + (0.0016f / lightingStrength) * distL * distL)
    val lightL = (0.35f * lightingStrength + 0.65f * attenL).coerceIn(0f, 1f)

    val shadedGoldCol = Color(
        red = (goldCol.red * lightL).coerceIn(0f, 1f),
        green = (goldCol.green * lightL).coerceIn(0f, 1f),
        blue = (goldCol.blue * lightL).coerceIn(0f, 1f),
        alpha = 1f
    )
    outList.add(RenderItem3D.Polygon(listOf(lockP1, lockP2, lockP3, lockP4), shadedGoldCol, depth = 0f))
}

private fun buildProceduralPlayerLowPoly(
    cx: Float, cy: Float, cz: Float,
    radX: Float, radY: Float, radZ: Float,
    heroClass: String,
    outList: MutableList<RenderItem3D>,
    lightSource: Vector3,
    lightingStrength: Float
) {
    val steelCol = Color(0xFF607D8B)
    val leatherCol = Color(0xFF5D4037)
    val clothCol = Color(0xFF0288D1)

    fun shadeColor(originalCol: Color, faceCenter: Vector3): Color {
        val dist = (faceCenter - lightSource).length()
        val attenuation = 1f / (1.0f + (0.0016f / lightingStrength) * dist * dist)
        val lightFactor = (0.24f * lightingStrength + 0.76f * attenuation).coerceIn(0f, 1f)
        return Color(
            red = (originalCol.red * lightFactor).coerceIn(0f, 1f),
            green = (originalCol.green * lightFactor).coerceIn(0f, 1f),
            blue = (originalCol.blue * lightFactor).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    // 1. Torso: Faceted rectangular pyramid standing upright
    val botY = cy + radY * 0.4f
    val midY = cy - radY * 0.15f
    val topY = cy - radY * 0.45f

    val b1 = Vector3(cx - radX * 0.5f, botY, cz - radZ * 0.5f)
    val b2 = Vector3(cx + radX * 0.5f, botY, cz - radZ * 0.5f)
    val b3 = Vector3(cx + radX * 0.5f, botY, cz + radZ * 0.5f)
    val b4 = Vector3(cx - radX * 0.5f, botY, cz + radZ * 0.5f)

    val m1 = Vector3(cx - radX * 0.75f, midY, cz - radZ * 0.75f)
    val m2 = Vector3(cx + radX * 0.75f, midY, cz - radZ * 0.75f)
    val m3 = Vector3(cx + radX * 0.75f, midY, cz + radZ * 0.75f)
    val m4 = Vector3(cx - radX * 0.75f, midY, cz + radZ * 0.75f)

    val torsoCol = when (heroClass) {
        "Warrior" -> steelCol
        "Mage" -> clothCol
        else -> leatherCol
    }

    // Lower Torso
    val lowerTorsoFaces = listOf(
        listOf(b1, b2, m2, m1),
        listOf(b2, b3, m3, m2),
        listOf(b3, b4, m4, m3),
        listOf(b4, b1, m1, m4)
    )
    lowerTorsoFaces.forEach { pts ->
        val midPt = Vector3(
            pts.sumOf { it.x.toDouble() }.toFloat() / 4f,
            pts.sumOf { it.y.toDouble() }.toFloat() / 4f,
            pts.sumOf { it.z.toDouble() }.toFloat() / 4f
        )
        outList.add(RenderItem3D.Polygon(pts, shadeColor(torsoCol, midPt), depth = 0f))
    }

    // Upper Torso shoulders
    val sCenter = Vector3(cx, topY, cz)
    val upperTorsoFaces = listOf(
        listOf(m1, m2, sCenter),
        listOf(m2, m3, sCenter),
        listOf(m3, m4, sCenter),
        listOf(m4, m1, sCenter)
    )
    upperTorsoFaces.forEach { pts ->
        val midPt = Vector3(
            pts.sumOf { it.x.toDouble() }.toFloat() / 3f,
            pts.sumOf { it.y.toDouble() }.toFloat() / 3f,
            pts.sumOf { it.z.toDouble() }.toFloat() / 3f
        )
        outList.add(RenderItem3D.Polygon(pts, shadeColor(torsoCol, midPt), depth = 0f))
    }

    // 2. Class-specific Helmets/hats/hoods
    when (heroClass) {
        "Warrior" -> {
            val h1 = Vector3(cx - radX * 0.4f, topY, cz - radZ * 0.4f)
            val h2 = Vector3(cx + radX * 0.4f, topY, cz - radZ * 0.4f)
            val h3 = Vector3(cx + radX * 0.4f, topY, cz + radZ * 0.4f)
            val h4 = Vector3(cx - radX * 0.4f, topY, cz + radZ * 0.4f)

            val ht1 = Vector3(cx - radX * 0.4f, cy - radY * 0.85f, cz - radZ * 0.4f)
            val ht2 = Vector3(cx + radX * 0.4f, cy - radY * 0.85f, cz - radZ * 0.4f)
            val ht3 = Vector3(cx + radX * 0.4f, cy - radY * 0.85f, cz + radZ * 0.4f)
            val ht4 = Vector3(cx - radX * 0.4f, cy - radY * 0.85f, cz + radZ * 0.4f)

            val helmetFaces = listOf(
                listOf(h1, h2, ht2, ht1),
                listOf(h3, h4, ht4, ht3),
                listOf(h1, ht1, ht4, h4),
                listOf(h2, h3, ht3, ht2),
                listOf(ht1, ht2, ht3, ht4)
            )
            helmetFaces.forEach { pts ->
                val midPt = Vector3(
                    pts.sumOf { it.x.toDouble() }.toFloat() / 4f,
                    pts.sumOf { it.y.toDouble() }.toFloat() / 4f,
                    pts.sumOf { it.z.toDouble() }.toFloat() / 4f
                )
                outList.add(RenderItem3D.Polygon(pts, shadeColor(steelCol, midPt), depth = 0f))
            }

            val vL = cz + radZ * 0.41f
            val vp1 = Vector3(cx - radX * 0.3f, cy - radY * 0.5f, vL)
            val vp2 = Vector3(cx + radX * 0.3f, cy - radY * 0.5f, vL)
            val vp3 = Vector3(cx + radX * 0.3f, cy - radY * 0.65f, vL)
            val vp4 = Vector3(cx - radX * 0.3f, cy - radY * 0.65f, vL)
            outList.add(RenderItem3D.Polygon(listOf(vp1, vp2, vp3, vp4), shadeColor(Color(0xFFFFB300), Vector3(cx, cy - radY * 0.58f, vL)), depth = 0f))
        }
        "Mage" -> {
            val brimR = radX * 0.9f
            val brimVertices = (0..7).map { i ->
                val ang = (i * 2f * PI.toFloat() / 8f)
                Vector3(cx + brimR * cos(ang), topY, cz + brimR * sin(ang))
            }
            outList.add(RenderItem3D.Polygon(brimVertices, shadeColor(Color(0xFF311B92), Vector3(cx, topY, cz)), depth = 0f))

            val coneTip = Vector3(cx, cy - radY * 1.0f, cz)
            (0..7).forEach { i ->
                val pts = listOf(brimVertices[i], brimVertices[(i + 1) % 8], coneTip)
                val midPt = Vector3(
                    pts.sumOf { it.x.toDouble() }.toFloat() / 3f,
                    pts.sumOf { it.y.toDouble() }.toFloat() / 3f,
                    pts.sumOf { it.z.toDouble() }.toFloat() / 3f
                )
                outList.add(RenderItem3D.Polygon(pts, shadeColor(Color(0xFF1A237E), midPt), depth = 0f))
            }
        }
        else -> {
            val h1 = Vector3(cx - radX * 0.38f, topY, cz - radZ * 0.38f)
            val h2 = Vector3(cx + radX * 0.38f, topY, cz - radZ * 0.38f)
            val h3 = Vector3(cx + radX * 0.38f, topY, cz + radZ * 0.38f)
            val h4 = Vector3(cx - radX * 0.38f, topY, cz + radZ * 0.38f)
            val hoodApex = Vector3(cx, cy - radY * 0.85f, cz)

            val hoodFaces = listOf(
                listOf(h1, h2, hoodApex),
                listOf(h2, h3, hoodApex),
                listOf(h3, h4, hoodApex),
                listOf(h4, h1, hoodApex)
            )
            hoodFaces.forEach { pts ->
                val midPt = Vector3(
                    pts.sumOf { it.x.toDouble() }.toFloat() / 3f,
                    pts.sumOf { it.y.toDouble() }.toFloat() / 3f,
                    pts.sumOf { it.z.toDouble() }.toFloat() / 3f
                )
                outList.add(RenderItem3D.Polygon(pts, shadeColor(Color(0xFF1B5E20), midPt), depth = 0f))
            }

            val vL = cz + radZ * 0.35f
            val faceW = radX * 0.22f
            val eye1 = Vector3(cx - faceW * 0.5f, cy - radY * 0.55f, vL)
            val eye2 = Vector3(cx + faceW * 0.5f, cy - radY * 0.55f, vL)
            outList.add(RenderItem3D.Line(eye1, eye1 + Vector3(faceW * 0.4f, 0f, 0.05f), Color.Red, 3.5f, 0f))
            outList.add(RenderItem3D.Line(eye2, eye2 - Vector3(faceW * 0.4f, 0f, -0.05f), Color.Red, 3.5f, 0f))
        }
    }
}

private fun buildProceduralMonsterLowPoly(
    cx: Float, cy: Float, cz: Float,
    radX: Float, radY: Float, radZ: Float,
    monsterType: String,
    baseColor: Color,
    outList: MutableList<RenderItem3D>,
    lightSource: Vector3,
    lightingStrength: Float
) {
    fun shadeColor(originalCol: Color, faceCenter: Vector3): Color {
        val dist = (faceCenter - lightSource).length()
        val attenuation = 1f / (1.0f + (0.0016f / lightingStrength) * dist * dist)
        val lightFactor = (0.24f * lightingStrength + 0.76f * attenuation).coerceIn(0f, 1f)
        return Color(
            red = (originalCol.red * lightFactor).coerceIn(0f, 1f),
            green = (originalCol.green * lightFactor).coerceIn(0f, 1f),
            blue = (originalCol.blue * lightFactor).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    when (monsterType) {
        "DRAGON" -> {
            val top = Vector3(cx, cy - radY * 0.6f, cz)
            val bottom = Vector3(cx, cy + radY * 0.6f, cz)
            val v1 = Vector3(cx - radX * 1.1f, cy, cz - radZ * 1.1f)
            val v2 = Vector3(cx + radX * 1.1f, cy, cz - radZ * 1.1f)
            val v3 = Vector3(cx + radX * 1.1f, cy, cz + radZ * 1.1f)
            val v4 = Vector3(cx - radX * 1.1f, cy, cz + radZ * 1.1f)

            val baseFaces = listOf(
                listOf(top, v2, v1), listOf(top, v3, v2), listOf(top, v4, v3), listOf(top, v1, v4),
                listOf(bottom, v1, v2), listOf(bottom, v2, v3), listOf(bottom, v3, v4), listOf(bottom, v4, v1)
            )
            baseFaces.forEach { pts ->
                val midPt = Vector3(
                    pts.sumOf { it.x.toDouble() }.toFloat() / 3f,
                    pts.sumOf { it.y.toDouble() }.toFloat() / 3f,
                    pts.sumOf { it.z.toDouble() }.toFloat() / 3f
                )
                outList.add(RenderItem3D.Polygon(pts, shadeColor(baseColor, midPt), depth = 0f))
            }

            val h1Base = top
            val h1Outer = Vector3(cx - radX * 1.2f, cy - radY * 1.1f, cz - radZ * 0.4f)
            outList.add(RenderItem3D.Line(h1Base, h1Outer, Color(0xFFFFD700), 3.5f, 0f))

            val h2Outer = Vector3(cx + radX * 1.2f, cy - radY * 1.1f, cz - radZ * 0.4f)
            outList.add(RenderItem3D.Line(h1Base, h2Outer, Color(0xFFFFD700), 3.5f, 0f))
        }
        "NECROMANCER" -> {
            val topY = cy - radY * 0.6f
            val baseBottomY = cy + radY * 0.6f

            val topCenter = Vector3(cx, topY, cz)
            val nb1 = Vector3(cx - radX * 0.5f, baseBottomY, cz - radZ * 0.5f)
            val nb2 = Vector3(cx + radX * 0.5f, baseBottomY, cz - radZ * 0.5f)
            val nb3 = Vector3(cx + radX * 0.5f, baseBottomY, cz + radZ * 0.5f)
            val nb4 = Vector3(cx - radX * 0.5f, baseBottomY, cz + radZ * 0.5f)

            val staffFaces = listOf(
                listOf(topCenter, nb2, nb1),
                listOf(topCenter, nb3, nb2),
                listOf(topCenter, nb4, nb3),
                listOf(topCenter, nb1, nb4)
            )
            staffFaces.forEach { pts ->
                val midPt = Vector3(
                    pts.sumOf { it.x.toDouble() }.toFloat() / 3f,
                    pts.sumOf { it.y.toDouble() }.toFloat() / 3f,
                    pts.sumOf { it.z.toDouble() }.toFloat() / 3f
                )
                outList.add(RenderItem3D.Polygon(pts, shadeColor(Color(0xFF212121), midPt), depth = 0f))
            }

            val crystalCore = Vector3(cx, cy - radY * 0.1f, cz)
            outList.add(RenderItem3D.Sphere(
                center = crystalCore,
                radius = radX * 0.55f,
                color = Color(0xFFE040FB),
                depth = 0f
            ))

            val orb1 = Vector3(cx - radX * 0.8f, cy - radY * 0.1f, cz)
            val orb2 = Vector3(cx + radX * 0.8f, cy - radY * 0.1f, cz)
            outList.add(RenderItem3D.Sphere(center = orb1, radius = radX * 0.18f, color = Color(0xFF6A1B9A), depth = 0f))
            outList.add(RenderItem3D.Sphere(center = orb2, radius = radX * 0.18f, color = Color(0xFF6A1B9A), depth = 0f))
        }
        "GOBLIN" -> {
            val top = Vector3(cx, cy - radY * 0.6f, cz)
            val bottom = Vector3(cx, cy + radY * 0.6f, cz)
            val v1 = Vector3(cx - radX * 0.8f, cy, cz - radZ * 0.8f)
            val v2 = Vector3(cx + radX * 0.8f, cy, cz - radZ * 0.8f)
            val v3 = Vector3(cx + radX * 0.8f, cy, cz + radZ * 0.8f)
            val v4 = Vector3(cx - radX * 0.8f, cy, cz + radZ * 0.8f)

            val baseFaces = listOf(
                listOf(top, v2, v1), listOf(top, v3, v2), listOf(top, v4, v3), listOf(top, v1, v4),
                listOf(bottom, v1, v2), listOf(bottom, v2, v3), listOf(bottom, v3, v4), listOf(bottom, v4, v1)
            )
            baseFaces.forEach { pts ->
                val midPt = Vector3(
                    pts.sumOf { it.x.toDouble() }.toFloat() / 3f,
                    pts.sumOf { it.y.toDouble() }.toFloat() / 3f,
                    pts.sumOf { it.z.toDouble() }.toFloat() / 3f
                )
                outList.add(RenderItem3D.Polygon(pts, shadeColor(baseColor, midPt), depth = 0f))
            }

            val eLeftTip = Vector3(cx - radX * 1.5f, cy - radY * 0.1f, cz)
            val darkerGoblin = Color(
                red = (baseColor.red * 0.85f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.85f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.85f).coerceIn(0f, 1f),
                alpha = baseColor.alpha
            )
            outList.add(RenderItem3D.Polygon(
                listOf(top, v1, eLeftTip),
                shadeColor(darkerGoblin, Vector3(cx - radX * 0.8f, cy, cz)),
                depth = 0f
            ))

            val eRightTip = Vector3(cx + radX * 1.5f, cy - radY * 0.1f, cz)
            outList.add(RenderItem3D.Polygon(
                listOf(top, v2, eRightTip),
                shadeColor(darkerGoblin, Vector3(cx + radX * 0.8f, cy, cz)),
                depth = 0f
            ))
        }
        else -> {
            val top = Vector3(cx, cy - radY, cz)
            val bottom = Vector3(cx, cy + radY, cz)
            val v1 = Vector3(cx - radX, cy, cz - radZ)
            val v2 = Vector3(cx + radX, cy, cz - radZ)
            val v3 = Vector3(cx + radX, cy, cz + radZ)
            val v4 = Vector3(cx - radX, cy, cz + radZ)

            val faces = listOf(
                listOf(top, v2, v1), listOf(top, v3, v2), listOf(top, v4, v3), listOf(top, v1, v4),
                listOf(bottom, v1, v2), listOf(bottom, v2, v3), listOf(bottom, v3, v4), listOf(bottom, v4, v1)
            )
            faces.forEach { pts ->
                val midPt = Vector3(
                    pts.sumOf { it.x.toDouble() }.toFloat() / 3f,
                    pts.sumOf { it.y.toDouble() }.toFloat() / 3f,
                    pts.sumOf { it.z.toDouble() }.toFloat() / 3f
                )
                outList.add(RenderItem3D.Polygon(pts, shadeColor(baseColor, midPt), depth = 0f))
            }
        }
    }
}
