package com.example.cameraxapp.blackjack

import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
fun BlackjackCanvas3D(
    dealerCards: List<Card>,
    isSecondHidden: Boolean,
    playerHands: List<BlackjackHand>,
    activeHandIndex: Int,
    gameState: GameState,
    walletBalance: Int,
    activeBet: Int,
    tableFeltStyleId: Int,
    modifier: Modifier = Modifier
) {
    var yawAngle by remember { mutableStateOf(0.0f) }
    var pitchAngle by remember { mutableStateOf(-0.55f) }
    val zoomScale by remember { mutableStateOf(0.95f) }

    val textPainter = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
    }

    // Determine visual table colors mapping
    val feltColor = when (tableFeltStyleId) {
        0 -> Color(0xFF0F5A35) // Elegant Vegas Emerald
        1 -> Color(0xFF6B1B29) // High-Roller Burgundy
        2 -> Color(0xFF1E2D4A) // Royal Indigo Lounge
        else -> Color(0xFF0F5A35)
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Rotate the board
                    yawAngle = (yawAngle + dragAmount.x * 0.007f)
                    pitchAngle = (pitchAngle - dragAmount.y * 0.007f).coerceIn(-1.3f, -0.3f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cX = width / 2f
            val cY = height / 2f

            // Project point helper
            val cameraZ = 340f
            val dFactor = 320f

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

            val drawPipeline = mutableListOf<RenderItem3D>()

            // 1. Draw 3D Casino Felt board
            val tableY = 45f
            buildCasinoTablePrimitives(tableY, feltColor, drawPipeline)

            // 2. Gold Text Inscription Projected on Felt
            drawPipeline.add(RenderItem3D.TextLabel(Vector3(0f, tableY - 0.5f, -25f), "BLACKJACK PAYS 3 TO 2", Color(0xFFFFD700).copy(alpha = 0.5f), sizeMultiplier = 0.9f, depth = 0f))
            drawPipeline.add(RenderItem3D.TextLabel(Vector3(0f, tableY - 0.5f, -5f), "Dealer Stands on Soft 17 • Insurance Pays 2 to 1", Color.White.copy(alpha = 0.35f), sizeMultiplier = 0.65f, depth = 0f))

            // 3. Render Dealer Hand
            if (dealerCards.isNotEmpty()) {
                val dealerBaseX = -((dealerCards.size - 1) * 11f)
                dealerCards.forEachIndexed { dIdx, card ->
                    val isSecret = dIdx == 1 && isSecondHidden
                    val cx = dealerBaseX + dIdx * 22f
                    build3DCard(
                        cx = cx, cy = tableY - 1f, cz = -90f,
                        isFaceUp = !isSecret,
                        card = card,
                        outList = drawPipeline,
                        cardAngleY = 0f
                    )
                }
                // Dealer Score Indicator Badge
                val scoreVal = if (isSecondHidden && dealerCards.size >= 2) {
                    dealerCards.firstOrNull()?.rank?.value ?: 0
                } else {
                    calculateHandScore(dealerCards)
                }
                drawPipeline.add(RenderItem3D.TextLabel(Vector3(0f, tableY - 2f, -114f), "DEALER SCORE: $scoreVal", Color.Yellow, sizeMultiplier = 0.75f, depth = 0f))
            } else {
                drawPipeline.add(RenderItem3D.TextLabel(Vector3(0f, tableY - 2f, -90f), "🤵 DEALER VACANT (AWAITING BETS)", Color.LightGray.copy(alpha = 0.6f), sizeMultiplier = 0.75f, depth = 0f))
            }

            // 4. Render Player Hands (Supporting Split indexes!)
            if (playerHands.isNotEmpty()) {
                val totalH = playerHands.size
                playerHands.forEachIndexed { hIdx, hand ->
                    // Position horizontal seats for splits
                    val seatX = if (totalH == 1) 0f else (hIdx - (totalH - 1) / 2.0f) * 110f
                    val seatZ = 80f

                    val isHandActive = gameState == GameState.PLAYER_TURN && hIdx == activeHandIndex

                    // Draw player card circle halo
                    val haloCol = if (isHandActive) Color(0xFFE0B0FF) else Color.White.copy(alpha = 0.15f)
                    val r = 26f
                    val ringPoints = mutableListOf<Vector3>()
                    for (seg in 0..12) {
                        val th = (seg * 2 * PI / 12f).toFloat()
                        ringPoints.add(Vector3(seatX + cos(th) * r, tableY - 0.4f, seatZ + sin(th) * r))
                    }
                    drawPipeline.add(RenderItem3D.Polygon(ringPoints, Color.Black.copy(alpha = 0.25f), outlineColor = haloCol, strokeWidth = 1.5f, depth = 0f))

                    // Draw Hand cards
                    if (hand.cards.isNotEmpty()) {
                        val cardBaseX = seatX - ((hand.cards.size - 1) * 10f)
                        hand.cards.forEachIndexed { cIdx, card ->
                            build3DCard(
                                cx = cardBaseX + cIdx * 20f,
                                cy = tableY - 1f - cIdx * 0.3f, // Elevate subsequent stacked cards
                                cz = seatZ,
                                isFaceUp = true,
                                card = card,
                                outList = drawPipeline,
                                cardAngleY = 0f
                            )
                        }

                        // Hand Score Indicator
                        val handScr = hand.getScoreDisplay()
                        val scoreLabel = "POINTS: $handScr" + if (isHandActive) " (ACTIVE)" else ""
                        val scoreCol = if (hand.isBust) Color(0xFFFF5252) else if (isHandActive) Color(0xFFE0B0FF) else Color.Yellow
                        drawPipeline.add(RenderItem3D.TextLabel(Vector3(seatX, tableY - 2f, seatZ + 38f), scoreLabel, scoreCol, sizeMultiplier = 0.7f, depth = 0f))
                    } else {
                        drawPipeline.add(RenderItem3D.TextLabel(Vector3(seatX, tableY - 1f, seatZ), "NO CARDS", Color.Gray, sizeMultiplier = 0.6f, depth = 0f))
                    }

                    // Render Chip Stack physically next to hand circle
                    if (hand.bet > 0) {
                        buildChipStack(seatX - 38f, tableY - 1f, seatZ - 20f, hand.bet, drawPipeline)
                        drawPipeline.add(RenderItem3D.TextLabel(Vector3(seatX - 38f, tableY - 2f, seatZ + 12f), "$${hand.bet}", Color.Yellow, sizeMultiplier = 0.6f, depth = 0f))
                    }
                }
            } else {
                // No active hands yet/Place Bet state, render single empty circle
                val ringPoints = mutableListOf<Vector3>()
                val r = 26f
                for (seg in 0..12) {
                    val th = (seg * 2 * PI / 12f).toFloat()
                    ringPoints.add(Vector3(cos(th) * r, tableY - 0.4f, 80f + sin(th) * r))
                }
                drawPipeline.add(RenderItem3D.Polygon(ringPoints, Color.Black.copy(alpha = 0.2f), outlineColor = Color.Yellow.copy(alpha = 0.5f), strokeWidth = 1.2f, depth = 0f))
                drawPipeline.add(RenderItem3D.TextLabel(Vector3(0f, tableY - 1f, 80f), "PLACE BET TO START", Color.Yellow.copy(alpha = 0.8f), sizeMultiplier = 0.75f, depth = 0f))
            }

            // 5. Apply Painter's Algorithm: Sort items by depth (back to front, Z positive is closer)
            val sortedItems = drawPipeline.sortedByDescending { item ->
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

            // 6. Draw Sorted Elements
            sortedItems.forEach { item ->
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

                            drawPath(path, color = item.color)
                            if (item.outlineColor != Color.Transparent) {
                                drawPath(path, color = item.outlineColor, style = Stroke(width = item.strokeWidth * zoomScale))
                            }
                        }
                    }
                    is RenderItem3D.Line -> {
                        val p1 = projectPoint(item.start)
                        val p2 = projectPoint(item.end)
                        drawLine(color = item.color, start = p1, end = p2, strokeWidth = item.strokeWidth * zoomScale)
                    }
                    is RenderItem3D.TextLabel -> {
                        val pLabel = projectPoint(item.position)
                        drawIntoCanvas { canvas ->
                            textPainter.color = item.color.toArgb()
                            textPainter.textSize = (9.dp.toPx() * item.sizeMultiplier)
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

        // Angle Swivel Guide Overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .border(0.5.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text("DRAG FELT TO ROTATE CAMERA", fontSize = 8.sp, color = Color(0xFFFFD700), letterSpacing = 0.5.sp)
        }
    }
}

private fun buildCasinoTablePrimitives(
    tableY: Float,
    feltColor: Color,
    outList: MutableList<RenderItem3D>
) {
    // Generate an elegant mahogany Wood Rim border semi-circle table
    val numSegments = 16
    val innerR = 170f
    val outerR = 185f
    
    // Felt tabletop flat semi-circle
    val feltPoints = mutableListOf<Vector3>()
    // Core points of felt semicircle
    for (i in 0..numSegments) {
        val rad = (i * PI / numSegments).toFloat()
        val c = cos(rad)
        val s = sin(rad)
        feltPoints.add(Vector3(c * innerR, tableY, -50f + s * innerR))
    }
    // Connect base diameter edge
    feltPoints.add(Vector3(-innerR, tableY, -50f))
    outList.add(RenderItem3D.Polygon(feltPoints, feltColor, outlineColor = feltColor.copy(alpha = 0.7f), depth = 0f))

    // Draw solid elegant Wood panels border on the semi-circle edge
    for (i in 0 until numSegments) {
        val rad1 = (i * PI / numSegments).toFloat()
        val rad2 = ((i + 1) * PI / numSegments).toFloat()

        val pIn1 = Vector3(cos(rad1) * innerR, tableY, -50f + sin(rad1) * innerR)
        val pIn2 = Vector3(cos(rad2) * innerR, tableY, -50f + sin(rad2) * innerR)
        val pOut1 = Vector3(cos(rad1) * outerR, tableY, -50f + sin(rad1) * outerR)
        val pOut2 = Vector3(cos(rad2) * outerR, tableY, -50f + sin(rad2) * outerR)

        // Wood rim face
        val rimPts = listOf(pIn1, pIn2, pOut2, pOut1)
        outList.add(RenderItem3D.Polygon(rimPts, Color(0xFF5A2A18), outlineColor = Color.Black.copy(alpha = 0.4f), depth = 0f))

        // Give height/depth to the wood rim by extruding down
        val deepY = tableY + 12f
        val exPts = listOf(
            pOut1,
            pOut2,
            pOut2.copy(y = deepY),
            pOut1.copy(y = deepY)
        )
        // Shade panels depending on coordinate to simulate beautiful golden lightning
        val shadeFactor = 0.7f + 0.3f * abs(cos(rad1))
        val darkWood = Color(
            red = (0.28f * shadeFactor).coerceIn(0f, 1f),
            green = (0.13f * shadeFactor).coerceIn(0f, 1f),
            blue = (0.07f * shadeFactor).coerceIn(0f, 1f),
            alpha = 1f
        )
        outList.add(RenderItem3D.Polygon(exPts, darkWood, outlineColor = Color.Transparent, depth = 0f))
    }
}

private fun build3DCard(
    cx: Float, cy: Float, cz: Float,
    isFaceUp: Boolean,
    card: Card?,
    outList: MutableList<RenderItem3D>,
    cardAngleY: Float = 0f
) {
    val hw = 15f
    val hh = 22f
    val tilt = -0.15f // Tilt card slightly face forward towards camera angle

    // Rotate vertices and slide to center location
    val pts = listOf(
        Vector3(-hw, 0f, -hh).rotateX(tilt).rotateY(cardAngleY) + Vector3(cx, cy, cz),
        Vector3(hw, 0f, -hh).rotateX(tilt).rotateY(cardAngleY) + Vector3(cx, cy, cz),
        Vector3(hw, 0f, hh).rotateX(tilt).rotateY(cardAngleY) + Vector3(cx, cy, cz),
        Vector3(-hw, 0f, hh).rotateX(tilt).rotateY(cardAngleY) + Vector3(cx, cy, cz)
    )

    // Flat shadow of the card lying on table to give superb grounded depth
    val shadowPoints = pts.map { it.copy(y = it.y + 1.2f) }
    outList.add(RenderItem3D.Polygon(shadowPoints, Color.Black.copy(alpha = 0.35f), outlineColor = Color.Transparent, isFilled = true, depth = 0f))

    // Body vector
    outList.add(RenderItem3D.Polygon(pts, Color.White, outlineColor = Color(0xFFD0D0D0), isFilled = true, depth = 0f))

    if (!isFaceUp || card == null) {
        // Red velvet textured pattern for dealer cardback
        val center = (pts[0] + pts[2]) * 0.5f
        outList.add(RenderItem3D.TextLabel(center - Vector3(0f, 1.2f, 0f), "🎴", Color(0xFFC62828), sizeMultiplier = 1.1f, depth = 0f))
        
        // Custom cross-hatch detail lines
        val m1 = (pts[0] + pts[1]) * 0.5f
        val m2 = (pts[2] + pts[3]) * 0.5f
        outList.add(RenderItem3D.Line(m1, m2, Color(0xFFC62828).copy(alpha = 0.3f), strokeWidth = 1f, depth = 0f))
    } else {
        val center = (pts[0] + pts[2]) * 0.5f
        val textColor = if (card.suit.isRed) Color(0xFFE53935) else Color(0xFF212121)

        // Center Suit symbol
        outList.add(RenderItem3D.TextLabel(center - Vector3(0f, 1.2f, 0f), card.suit.symbol + card.rank.representation, textColor, sizeMultiplier = 0.85f, depth = 0f))

        // Index letters
        val topLeft = pts[0] * 0.72f + center * 0.28f - Vector3(0f, 1f, 0f)
        val bottomRight = pts[2] * 0.72f + center * 0.28f - Vector3(0f, 1f, 0f)
        outList.add(RenderItem3D.TextLabel(topLeft, card.rank.representation, textColor, sizeMultiplier = 0.52f, depth = 0f))
        outList.add(RenderItem3D.TextLabel(bottomRight, card.suit.symbol, textColor, sizeMultiplier = 0.52f, depth = 0f))
    }
}

private fun buildChipStack(
    cx: Float, cy: Float, cz: Float,
    betAmount: Int,
    outList: MutableList<RenderItem3D>
) {
    if (betAmount <= 0) return

    var remainder = betAmount
    val blackCount = remainder / 100
    remainder %= 100
    val greenCount = remainder / 25
    remainder %= 25
    val redCount = remainder / 5
    remainder %= 5
    val whiteCount = remainder

    val stacks = listOf(
        Pair(whiteCount, Color.White),
        Pair(redCount, Color(0xFFE53935)), // Red ($5)
        Pair(greenCount, Color(0xFF2E7D32)), // Green ($25)
        Pair(blackCount, Color(0xFF212121)) // Black ($100)
    )

    var currentY = cy
    val r = 7.5f
    val h = 1.8f

    stacks.forEach { (count, color) ->
        for (i in 0 until count) {
            val topY = currentY - h

            // Draw cylindrical 3D stack facets
            val numSegments = 8
            val topVerts = mutableListOf<Vector3>()
            val botVerts = mutableListOf<Vector3>()
            for (j in 0..numSegments) {
                val theta = (j * 2 * PI / numSegments).toFloat()
                val dx = cos(theta) * r
                val dz = sin(theta) * r
                topVerts.add(Vector3(cx + dx, topY, cz + dz))
                botVerts.add(Vector3(cx + dx, currentY, cz + dz))
            }

            // Cylinder flat top surface
            outList.add(RenderItem3D.Polygon(topVerts, color, outlineColor = Color.White.copy(alpha = 0.4f), strokeWidth = 1f, depth = 0f))

            // Cylinder side facet bands
            for (j in 0 until numSegments) {
                val segmentPts = listOf(topVerts[j], topVerts[j+1], botVerts[j+1], botVerts[j])
                val sideShade = 0.62f + 0.38f * abs(cos((j * 2 * PI / numSegments).toFloat()))
                val shadedSideCol = Color(
                    red = (color.red * sideShade).coerceIn(0f, 1f),
                    green = (color.green * sideShade).coerceIn(0f, 1f),
                    blue = (color.blue * sideShade).coerceIn(0f, 1f),
                    alpha = 1f
                )
                outList.add(RenderItem3D.Polygon(segmentPts, shadedSideCol, outlineColor = Color.Transparent, depth = 0f))
            }

            // Move pointer up for next chip
            currentY -= h + 0.5f
        }
    }
}
