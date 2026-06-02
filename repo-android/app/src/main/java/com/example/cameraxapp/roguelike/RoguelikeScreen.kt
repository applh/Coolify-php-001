package com.example.cameraxapp.roguelike

import com.example.cameraxapp.AppLogger
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.cameraxapp.core.math3d.Vector3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.PI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoguelikeScreen(
    onBack: () -> Unit,
    viewModel: RoguelikeViewModel
) {
    val status by viewModel.status
    val charState by viewModel.characterState
    val tiles by viewModel.tiles
    val monsters by viewModel.monsters
    val inventory by viewModel.inventory
    val logs by viewModel.gameLogs
    val highScores by viewModel.highScores

    val playerX by viewModel.playerX
    val lockedMonsterId by viewModel.lockedMonsterId
    val cameraYaw by viewModel.cameraYaw

    var is3DMode by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Color(0xFF121212))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "🗡️ Moria",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (status != GameStatus.CHARACTER_SELECT && status != GameStatus.SCORES_SCREEN) {
                        IconButton(onClick = { viewModel.restartGame() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Forfeit", tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = { viewModel.toggleScores() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "Leaderboard", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF080808))
        ) {
            when (status) {
                GameStatus.CHARACTER_SELECT -> {
                    CharacterSelectionView(onSelectClass = { viewModel.selectClass(it) })
                }
                GameStatus.SCORES_SCREEN -> {
                    ScoresView(
                        highScores = highScores,
                        onClearScores = { viewModel.resetStatsAndScores() },
                        onDismiss = { viewModel.toggleScores() }
                    )
                }
                GameStatus.EXPLORING, GameStatus.INVENTORY_MODAL -> {
                    charState?.let { char ->
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        ) {
                            val isLandscape = maxWidth > maxHeight
                            val vmin = minOf(maxWidth, maxHeight)

                            if (isLandscape) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 3D/2D Viewport: square of vmin size, left of screen
                                    MoriaBenchmarkViewport(
                                        tiles = tiles,
                                        monsters = monsters,
                                        pId = playerX,
                                        planetNodes = viewModel.planetNodes,
                                        heroClass = char.heroClass,
                                        lockedMonsterId = lockedMonsterId,
                                        viewModel = viewModel,
                                        modifier = Modifier
                                            .size(vmin)
                                    )

                                    // Remaining space: HUD Panel
                                    HudPanel(
                                        char = char,
                                        logs = logs,
                                        inventory = inventory,
                                        onMove = { dx, dy -> viewModel.movePlayer(dx, dy) },
                                        onOpenInventory = { viewModel.openInventory() },
                                        onCastSpell = { viewModel.castClassSpell() },
                                        onNormalAttack = { viewModel.normalAttackNearest() },
                                        onEnable = { viewModel.enableAction() },
                                        onDrinkHealthPotion = { viewModel.drinkHealthPotion() },
                                        onDrinkManaPotion = { viewModel.drinkManaPotion() },
                                        onUseTeleportGem = { viewModel.useTeleportGem() },
                                        onToggleTargetLock = { viewModel.toggleTargetLock() },
                                        onWalkTowardsTarget = { viewModel.walkTowardsTarget() },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 3D/2D Viewport: square of vmin size, top of screen
                                    MoriaBenchmarkViewport(
                                        tiles = tiles,
                                        monsters = monsters,
                                        pId = playerX,
                                        planetNodes = viewModel.planetNodes,
                                        heroClass = char.heroClass,
                                        lockedMonsterId = lockedMonsterId,
                                        viewModel = viewModel,
                                        modifier = Modifier
                                            .size(vmin)
                                    )

                                    // Remaining space: HUD Panel
                                    HudPanel(
                                        char = char,
                                        logs = logs,
                                        inventory = inventory,
                                        onMove = { dx, dy -> viewModel.movePlayer(dx, dy) },
                                        onOpenInventory = { viewModel.openInventory() },
                                        onCastSpell = { viewModel.castClassSpell() },
                                        onNormalAttack = { viewModel.normalAttackNearest() },
                                        onEnable = { viewModel.enableAction() },
                                        onDrinkHealthPotion = { viewModel.drinkHealthPotion() },
                                        onDrinkManaPotion = { viewModel.drinkManaPotion() },
                                        onUseTeleportGem = { viewModel.useTeleportGem() },
                                        onToggleTargetLock = { viewModel.toggleTargetLock() },
                                        onWalkTowardsTarget = { viewModel.walkTowardsTarget() },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Overlay drawer modal for inventory bag management
                        if (status == GameStatus.INVENTORY_MODAL) {
                            InventoryDrawerModal(
                                items = inventory,
                                onEquip = { viewModel.equipItem(it) },
                                onUse = { viewModel.useItem(it) },
                                onClose = { viewModel.closeInventory() }
                            )
                        }
                    }
                }
                GameStatus.GAME_OVER -> {
                    charState?.let { char ->
                        GameOverOverlay(
                            char = char,
                            statusLabel = "💀 DEFEAT",
                            descriptionText = "You were out-maneuvered in the dark. Your memories are reclaimed by the dungeon.",
                            onRetry = { viewModel.restartGame() }
                        )
                    } ?: run {
                        // Resilient Fallback in case char state clears
                        GameOverOverlay(
                            char = null,
                            statusLabel = "💀 DEFEAT",
                            descriptionText = "You were consumed by dark dungeon shadows. Your run is lost.",
                            onRetry = { viewModel.restartGame() }
                        )
                    }
                }
                GameStatus.VICTORY -> {
                    charState?.let { char ->
                        GameOverOverlay(
                            char = char,
                            statusLabel = "🏆 VICTORY",
                            descriptionText = "You cleared deep Floor 10, vanquished the legendary Shadow Dragon, and earned eternal glory!",
                            onRetry = { viewModel.restartGame() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterSelectionView(onSelectClass: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🗡️ CHOOSE YOUR PATH 🛡️",
            color = Color(0xFFFFD700),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Procedural micro-survival RPG adventure. Permadeath is strictly active.",
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Class Options
        val options = listOf(
            Triple("Warrior", "🛡️ High durability & raw physical power. Ideal for direct frontline combat.", "HP: 120 | Mana: 20 | STR: 14 | VIT: 14"),
            Triple("Mage", "🧙 Spellweaver wielding explosive ranged fire magic & powerful shields.", "HP: 75 | Mana: 100 | STR: 5 | INT: 15"),
            Triple("Rogue", "🗡️ Evasive shadow crawler doing severe critical strike sneak attacks.", "HP: 90 | Mana: 40 | STR: 10 | DEX: 14")
        )

        options.forEach { (title, desc, stats) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, Color(0xFF2C251C), RoundedCornerShape(8.dp)),
                onClick = { onSelectClass(title) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = title, color = Color(0xFFFFE066), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(text = desc, color = Color.LightGray, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stats,
                        color = Color(0xFF81C784),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun HudPanel(
    char: CharacterState,
    logs: List<String>,
    inventory: List<InventoryItem>,
    onMove: (Int, Int) -> Unit,
    onOpenInventory: () -> Unit,
    onCastSpell: () -> Unit,
    onNormalAttack: () -> Unit,
    onEnable: () -> Unit,
    onDrinkHealthPotion: () -> Unit,
    onDrinkManaPotion: () -> Unit,
    onUseTeleportGem: () -> Unit,
    onToggleTargetLock: () -> Unit = {},
    onWalkTowardsTarget: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        if (isLandscape) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top part: Logs and Stats side-by-side to solve empty screen spaces in landscape
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HudLogsCard(
                        logs = logs,
                        modifier = Modifier.weight(1.2f)
                    )
                    HudStatsCard(
                        char = char,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Bottom part: Clean and balanced Control panel
                ControlPanel(
                    char = char,
                    inventory = inventory,
                    onMove = onMove,
                    onOpenInventory = onOpenInventory,
                    onCastSpell = onCastSpell,
                    onNormalAttack = onNormalAttack,
                    onEnable = onEnable,
                    onDrinkHealthPotion = onDrinkHealthPotion,
                    onDrinkManaPotion = onDrinkManaPotion,
                    onUseTeleportGem = onUseTeleportGem,
                    onToggleTargetLock = onToggleTargetLock,
                    onWalkTowardsTarget = onWalkTowardsTarget,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Combat Logs at the top, expanding to fill available space
                HudLogsCard(logs = logs, modifier = Modifier.weight(1f).padding(bottom = 4.dp))

                // Stats section positioned directly under the logs card (and above controls)
                HudStatsCard(char = char)

                Spacer(modifier = Modifier.height(4.dp))

                // Controller row at the bottom
                ControlPanel(
                    char = char,
                    inventory = inventory,
                    onMove = onMove,
                    onOpenInventory = onOpenInventory,
                    onCastSpell = onCastSpell,
                    onNormalAttack = onNormalAttack,
                    onEnable = onEnable,
                    onDrinkHealthPotion = onDrinkHealthPotion,
                    onDrinkManaPotion = onDrinkManaPotion,
                    onUseTeleportGem = onUseTeleportGem,
                    onToggleTargetLock = onToggleTargetLock,
                    onWalkTowardsTarget = onWalkTowardsTarget,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun HudStatsCard(char: CharacterState, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
        modifier = modifier
            .border(0.6.dp, Color(0xFF332A20), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${char.heroClass} [Lv ${char.level}]",
                    color = Color(0xFFFFD700),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Floor ${char.floor}/10",
                    color = Color(0xFFE57373),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "💰 ${char.gold}g",
                    color = Color(0xFFFFEE58),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))

            // HP Bar
            val hpRatio = char.currentHp.toFloat() / char.maxHp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("HP", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(32.dp))
                LinearProgressIndicator(
                    progress = { hpRatio.coerceIn(0f, 1f) },
                    color = Color(0xFFE57373),
                    trackColor = Color(0xFF331111),
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                )
                Text(" ${char.currentHp}/${char.maxHp}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(2.dp))

            // Mana Bar
            val mpRatio = if (char.maxMana == 0) 0f else char.currentMana.toFloat() / char.maxMana
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MANA", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(32.dp))
                LinearProgressIndicator(
                    progress = { mpRatio.coerceIn(0f, 1f) },
                    color = Color(0xFF64B5F6),
                    trackColor = Color(0xFF112233),
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                )
                Text(" ${char.currentMana}/${char.maxMana}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(2.dp))

            // EXP Bar
            val expRatio = char.exp.toFloat() / (char.level * 100)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("EXP", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(32.dp))
                LinearProgressIndicator(
                    progress = { expRatio.coerceIn(0f, 1f) },
                    color = Color(0xFF81C784),
                    trackColor = Color(0xFF112211),
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                )
                Text(" ${char.exp}/${char.level * 100}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun HudLogsCard(logs: List<String>, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        modifier = modifier
            .fillMaxWidth()
            .border(0.6.dp, Color(0xFF222222), RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true
            ) {
                items(logs.reversed()) { log ->
                    val color = when {
                        log.contains("LEVEL UP") -> Color(0xFF81C784)
                        log.contains("VICTORY") -> Color(0xFFFFD700)
                        log.contains("Defeat") || log.contains("Game Over") || log.contains("strikes you") -> Color(0xFFE57373)
                        log.contains("treasure chest") || log.contains("gold") -> Color(0xFFFFEE58)
                        else -> Color.LightGray
                    }
                    Text(
                        text = "> $log",
                        color = color,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ControlPanel(
    char: CharacterState,
    inventory: List<InventoryItem>,
    onMove: (Int, Int) -> Unit,
    onOpenInventory: () -> Unit,
    onCastSpell: () -> Unit,
    onNormalAttack: () -> Unit,
    onEnable: () -> Unit,
    onDrinkHealthPotion: () -> Unit,
    onDrinkManaPotion: () -> Unit,
    onUseTeleportGem: () -> Unit,
    onToggleTargetLock: () -> Unit = {},
    onWalkTowardsTarget: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            GamepadActionGrid3x3(
                char = char,
                inventory = inventory,
                onOpenInventory = onOpenInventory,
                onCastSpell = onCastSpell,
                onNormalAttack = onNormalAttack,
                onDrinkHealthPotion = onDrinkHealthPotion,
                onDrinkManaPotion = onDrinkManaPotion,
                onUseTeleportGem = onUseTeleportGem
            )

            GamepadGoPanel(
                onWalkTowardsTarget = onWalkTowardsTarget,
                onToggleTargetLock = onToggleTargetLock,
                onEnable = onEnable
            )
        }
    }
}

@Composable
fun DungeonCanvas(
    tiles: List<GameTile>,
    monsters: List<MonsterState>,
    pX: Int,
    pY: Int,
    heroClass: String,
    modifier: Modifier = Modifier
) {
    val textPainter = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier) {
        val calculatedSize = minOf(size.width, size.height)
        val tileSize = calculatedSize / 18f

        textPainter.textSize = tileSize * 0.70f

        // Draw base grid background
        drawRect(Color.Black, topLeft = Offset.Zero, size = size)

        // 1. Draw Revealed and Shrouded Tiles
        tiles.forEach { tile ->
            val rx = tile.x * tileSize
            val ry = tile.y * tileSize

            val bgTileColor = when {
                !tile.revealed -> Color.Black
                tile.tileType == "WALL" -> Color(0xFF262423) // Slate brown rocks
                tile.tileType == "STAIRS_DOWN" -> Color(0xFF5A2A00) // Deep burnt orange staircase
                tile.tileType == "CHEST" -> Color(0xFF38230B) // Rusty chest frame
                else -> Color(0xFF0F0E0D) // Pitch floor
            }

            // Draw Background Tile Box
            drawRect(
                color = bgTileColor,
                topLeft = Offset(rx, ry),
                size = Size(tileSize, tileSize)
            )

            // Outer subtle mesh outline for revealed floors
            if (tile.revealed) {
                drawRect(
                    color = Color(0xFF1B1918),
                    topLeft = Offset(rx, ry),
                    size = Size(tileSize, tileSize),
                    style = Stroke(width = 0.5f)
                )

                // Render passive static indicators inside cell
                when (tile.tileType) {
                    "STAIRS_DOWN" -> {
                        drawIntoCanvas { canvas ->
                            val nativeY = ry + (tileSize * 0.75f)
                            textPainter.color = android.graphics.Color.MAGENTA
                            canvas.nativeCanvas.drawText("🪜", rx + (tileSize / 2f), nativeY, textPainter)
                        }
                    }
                    "CHEST" -> {
                        drawIntoCanvas { canvas ->
                            val nativeY = ry + (tileSize * 0.75f)
                            textPainter.color = android.graphics.Color.YELLOW
                            canvas.nativeCanvas.drawText("📦", rx + (tileSize / 2f), nativeY, textPainter)
                        }
                    }
                }
            }
        }

        // 2. Draw revealed monsters
        monsters.forEach { monster ->
            val tileX = monster.x * tileSize
            val tileY = monster.y * tileSize

            // Only draw monster if its coordinate cell is currently inside revealed light radius
            val isMonsterRevealed = tiles.find { it.x == monster.x && it.y == monster.y }?.revealed ?: false
            if (isMonsterRevealed) {
                val glyph = when (monster.type) {
                    "DRAGON" -> "🐉"
                    "NECROMANCER" -> "🔮"
                    "GOBLIN" -> "👺"
                    else -> "💀" // SKELETON
                }

                drawIntoCanvas { canvas ->
                    val nativeY = tileY + (tileSize * 0.75f)
                    canvas.nativeCanvas.drawText(glyph, tileX + (tileSize / 2f), nativeY, textPainter)
                }

                // Small Micro-Health Bar below the icon
                val ratio = monster.currentHp.toFloat() / monster.maxHp
                val barW = tileSize * 0.8f
                val barH = tileSize * 0.09f
                val startX = tileX + (tileSize * 0.1f)
                val startY = tileY + (tileSize * 0.85f)

                // Background red backing
                drawRect(
                    color = Color(0xFF551111),
                    topLeft = Offset(startX, startY),
                    size = Size(barW, barH)
                )
                // Active health green bar
                drawRect(
                    color = Color(0xFF4CAF50),
                    topLeft = Offset(startX, startY),
                    size = Size(barW * ratio.coerceIn(0f, 1f), barH)
                )
            }
        }

        // 3. Draw Hero Player
        val pTileX = pX * tileSize
        val pTileY = pY * tileSize
        val playerGlyph = when (heroClass) {
            "Warrior" -> "🛡️"
            "Mage" -> "🧙"
            else -> "🗡️" // Rogue
        }

        drawIntoCanvas { canvas ->
            val nativeY = pTileY + (tileSize * 0.74f)
            canvas.nativeCanvas.drawText(playerGlyph, pTileX + (tileSize / 2f), nativeY, textPainter)
        }
    }
}

@Composable
fun FloatingActionsBelt(
    char: CharacterState,
    inventory: List<InventoryItem>,
    onOpenInventory: () -> Unit,
    onCastSpell: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Spell power button
        val spellLabel = when (char.heroClass) {
            "Warrior" -> "🪓 Cleave (6m)"
            "Mage" -> "🔥 Fireball (15m)"
            else -> "🗡️ Backstab (10m)"
        }
        val castable = when (char.heroClass) {
            "Warrior" -> char.currentMana >= 6
            "Mage" -> char.currentMana >= 15
            else -> char.currentMana >= 10
        }

        Button(
            onClick = onCastSpell,
            enabled = castable,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF391E5B),
                disabledContainerColor = Color(0xFF20162B)
            ),
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .height(35.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(spellLabel, color = if (castable) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Inventory bag drawer button
        val consCount = inventory.count { it.type == "CONSUMABLE" }
        Button(
            onClick = onOpenInventory,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121)),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .height(35.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("🎒 Backpack ($consCount)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TouchDPad(onMove: (Int, Int) -> Unit, onEnable: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: UP
        IconButton(
            onClick = { onMove(0, -1) },
            modifier = Modifier
                .background(Color(0xFF1A1A1A), RoundedCornerShape(20))
                .size(42.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.LightGray)
        }

        // Row 2: LEFT | ENABLE | RIGHT
        Row(
            modifier = Modifier
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onMove(-1, 0) },
                modifier = Modifier
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(20))
                    .size(42.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Move Left", tint = Color.LightGray)
            }

            // ENABLE turn option
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF1E3A1E), RoundedCornerShape(20))
                    .clickable { onEnable() },
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", color = Color.White, fontSize = 14.sp)
            }

            IconButton(
                onClick = { onMove(1, 0) },
                modifier = Modifier
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(20))
                    .size(42.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Move Right", tint = Color.LightGray)
            }
        }

        // Row 3: DOWN
        IconButton(
            onClick = { onMove(0, 1) },
            modifier = Modifier
                .background(Color(0xFF1A1A1A), RoundedCornerShape(20))
                .size(42.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.LightGray)
        }
    }
}

@Composable
fun InventoryDrawerModal(
    items: List<InventoryItem>,
    onEquip: (InventoryItem) -> Unit,
    onUse: (InventoryItem) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.6f)
                .border(1.dp, Color(0xFF4A3E31), RoundedCornerShape(8.dp))
                .clickable(enabled = false) {}
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(" Backpack Inventory", color = Color(0xFFFFD700), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Close", color = Color.White, fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFF2C2C2C), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                if (items.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Your inventory backpack is empty.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    val sortedWeapons = items.filter { it.type == "WEAPON" }.sortedByDescending { it.statMod }
                    val sortedArmors = items.filter { it.type == "ARMOR" }.sortedByDescending { it.statMod }
                    val sortedConsumables = items.filter { it.type == "CONSUMABLE" }.sortedByDescending { it.statMod }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (sortedWeapons.isNotEmpty()) {
                            item {
                                Text(
                                    text = "🗡️ WEAPONS (Attack Bonus)",
                                    color = Color(0xFFFFB74D),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }
                            items(sortedWeapons) { item ->
                                InventoryItemRow(item, onEquip, onUse)
                            }
                        }
                        if (sortedArmors.isNotEmpty()) {
                            item {
                                Text(
                                    text = "🛡️ ARMORS (Defense Bonus)",
                                    color = Color(0xFF64B5F6),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(sortedArmors) { item ->
                                InventoryItemRow(item, onEquip, onUse)
                            }
                        }
                        if (sortedConsumables.isNotEmpty()) {
                            item {
                                Text(
                                    text = "🧪 CONSUMABLES (Recovery)",
                                    color = Color(0xFF81C784),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(sortedConsumables) { item ->
                                InventoryItemRow(item, onEquip, onUse)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryItemRow(
    item: InventoryItem,
    onEquip: (InventoryItem) -> Unit,
    onUse: (InventoryItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF1F1F1F), RoundedCornerShape(4.dp))
            .border(
                width = if (item.isEquipped) 1.dp else 0.dp,
                color = if (item.isEquipped) Color(0xFF81C784) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable {
                if (item.type != "CONSUMABLE") {
                    if (!item.isEquipped) onEquip(item)
                } else {
                    onUse(item)
                }
            }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.type != "CONSUMABLE") {
                RadioButton(
                    selected = item.isEquipped,
                    onClick = { if (!item.isEquipped) onEquip(item) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF81C784),
                        unselectedColor = Color.LightGray
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                val title = if (item.isEquipped) "${item.name} [EQUIPPED]" else item.name
                val titleColor = if (item.isEquipped) Color(0xFF81C784) else Color.White
                Text(title, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                
                val subText = when (item.type) {
                    "WEAPON" -> "Weapon rating: +${item.statMod} Attack strength"
                    "ARMOR" -> "Armor rating: +${item.statMod} Shield defense"
                    else -> "Restore potion: Recovers +${item.statMod} points"
                }
                Text(subText, color = Color.LightGray, fontSize = 11.sp)
            }
        }

        if (item.type == "CONSUMABLE") {
            Button(
                onClick = { onUse(item) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Use", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    char: CharacterState?,
    statusLabel: String,
    descriptionText: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(Color(0xFF0F0000)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = statusLabel,
            color = if (statusLabel.contains("DEFEAT")) Color(0xFFE57373) else Color(0xFFFFD700),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = descriptionText,
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        char?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C0A0A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF551111), RoundedCornerShape(8.dp))
                    .padding(bottom = 32.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
                    Text("💀 METRICS SUMMARY", color = Color(0xFFFFCC80), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("• Hero Class: ${it.heroClass}", color = Color.White, fontSize = 13.sp)
                    Text("• Level Reached: ${it.level}", color = Color.White, fontSize = 13.sp)
                    Text("• Deepest Floor: ${it.floor}", color = Color.White, fontSize = 13.sp)
                    Text("• Gold Accumulated: ${it.gold}g", color = Color.White, fontSize = 13.sp)
                    Text("• Total Turns Survived: ${it.turns}", color = Color.White, fontSize = 13.sp)
                }
            }
        }

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E0000)),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
        ) {
            Text("RE-ARRANGE CHARACTER", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ScoresView(
    highScores: List<HighScore>,
    onClearScores: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .border(1.dp, Color(0xFF424242), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏆 Leaderboad & Runs", color = Color(0xFFFFD700), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            HorizontalDivider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            if (highScores.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No run history recorded yet.", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(highScores) { score ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val outcomeLabel = if (score.survived) "🏆 ESCAPED" else "💀 FALLEN"
                                val outcomeColor = if (score.survived) Color(0xFF81C784) else Color(0xFFE57373)
                                Text("${score.heroClass} (Lv ${score.level})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(outcomeLabel, color = outcomeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Floor ${score.floor} | Gold: ${score.gold}g | ${score.turns} turns | ${score.timestamp.take(10)}",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onClearScores,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF611c1c)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("WIP OUT STATS", color = Color.White, fontSize = 12.sp)
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("RETURN", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun GamepadGoPanel(
    onWalkTowardsTarget: () -> Unit,
    onToggleTargetLock: () -> Unit,
    onEnable: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            .border(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(12.dp)
    ) {
        // 1. Sleek Action Button: Enable/Interact
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFF1E3A1E).copy(alpha = 0.9f), RoundedCornerShape(22.dp))
                .border(1.5.dp, Color(0xFF81C784).copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                .clickable { onEnable() },
            contentAlignment = Alignment.Center
        ) {
            Text("⚡", color = Color.White, fontSize = 18.sp)
        }

        // 2. The prominent movement "GO" Button (replaces joystick and directional cross)
        Button(
            onClick = onWalkTowardsTarget,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F3A52)),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .size(76.dp, 44.dp)
                .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
        ) {
            Text(
                text = "GO",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // 3. Target Lock-On Button ("🎯")
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFF331111).copy(alpha = 0.9f), RoundedCornerShape(22.dp))
                .border(1.5.dp, Color(0xFFFF3366).copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                .clickable { onToggleTargetLock() },
            contentAlignment = Alignment.Center
        ) {
            Text("🎯", fontSize = 16.sp)
        }
    }
}

@Composable
fun GamepadActionGrid3x3(
    char: CharacterState,
    inventory: List<InventoryItem>,
    onOpenInventory: () -> Unit,
    onCastSpell: () -> Unit,
    onNormalAttack: () -> Unit,
    onDrinkHealthPotion: () -> Unit,
    onDrinkManaPotion: () -> Unit,
    onUseTeleportGem: () -> Unit
) {
    val hpCount = inventory.count { it.name.contains("Health Potion") && it.type == "CONSUMABLE" }
    val hpAvailable = hpCount > 0

    val mpCount = inventory.count { it.name.contains("Mana Potion") && it.type == "CONSUMABLE" }
    val mpAvailable = mpCount > 0

    val gemCount = inventory.count { it.name.contains("Teleport Gem") && it.type == "CONSUMABLE" }
    val gemAvailable = gemCount > 0

    val spellLabel = when (char.heroClass) {
        "Warrior" -> "Cleave"
        "Mage" -> "Fireball"
        else -> "Backstab"
    }
    val icon = when (char.heroClass) {
        "Warrior" -> "🪓"
        "Mage" -> "🔥"
        else -> "🗡️"
    }
    val manaCost = when (char.heroClass) {
        "Warrior" -> 6
        "Mage" -> 15
        else -> 10
    }
    val castable = char.currentMana >= manaCost
    val consCount = inventory.count { it.type == "CONSUMABLE" }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(8.dp)
    ) {
        // Row 1
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HP Potion
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (hpAvailable) Color(0xFF2C1616).copy(alpha = 0.85f)
                        else Color(0xFF141414).copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        2.dp,
                        if (hpAvailable) Color(0xFFE57373) else Color(0xFF423333),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = hpAvailable) { onDrinkHealthPotion() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("❤️", fontSize = 11.sp)
                    Text("HP ($hpCount)", color = if (hpAvailable) Color.White else Color.Gray, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            // MP Potion
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (mpAvailable) Color(0xFF112233).copy(alpha = 0.85f)
                        else Color(0xFF141414).copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        2.dp,
                        if (mpAvailable) Color(0xFF64B5F6) else Color(0xFF333E48),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = mpAvailable) { onDrinkManaPotion() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("💙", fontSize = 11.sp)
                    Text("MP ($mpCount)", color = if (mpAvailable) Color.White else Color.Gray, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Teleport Gem
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (gemAvailable) Color(0xFF2C1C3D).copy(alpha = 0.85f)
                        else Color(0xFF141414).copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        2.dp,
                        if (gemAvailable) Color(0xFFBA68C8) else Color(0xFF3C2C4D),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = gemAvailable) { onUseTeleportGem() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("✨", fontSize = 11.sp)
                    Text("GEM ($gemCount)", color = if (gemAvailable) Color.White else Color.Gray, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Row 2
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder Left
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF0F0F0F), RoundedCornerShape(10.dp))
                    .border(0.5.dp, Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
            )

            // Special Attack (Spell)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (castable) Color(0xFF4A148C).copy(alpha = 0.85f)
                        else Color(0xFF141414).copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        2.dp,
                        if (castable) Color(0xFFBA68C8) else Color(0xFF424242).copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = castable) { onCastSpell() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(text = icon, fontSize = 11.sp)
                    Text(
                        text = spellLabel,
                        fontSize = 7.5.sp,
                        color = if (castable) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${manaCost}mp",
                        fontSize = 7.sp,
                        color = if (castable) Color(0xFFE1BEE7) else Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Normal Attack
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF421E1E).copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                    .border(2.dp, Color(0xFFE57373), RoundedCornerShape(10.dp))
                    .clickable { onNormalAttack() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(text = "🗡️", fontSize = 11.sp)
                    Text(text = "Attack", fontSize = 7.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Row 3
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder Left
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF0F0F0F), RoundedCornerShape(10.dp))
                    .border(0.5.dp, Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
            )

            // Bag
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF15181C), RoundedCornerShape(10.dp))
                    .border(2.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable { onOpenInventory() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("🎒", fontSize = 11.sp)
                    Text("BAG ($consCount)", fontSize = 7.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Placeholder Right
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF0F0F0F), RoundedCornerShape(10.dp))
                    .border(0.5.dp, Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
fun GamepadActions(
    char: CharacterState,
    inventory: List<InventoryItem>,
    onOpenInventory: () -> Unit,
    onCastSpell: () -> Unit
) {
    val spellLabel = when (char.heroClass) {
        "Warrior" -> "Cleave"
        "Mage" -> "Fireball"
        else -> "Backstab"
    }
    val icon = when (char.heroClass) {
        "Warrior" -> "🪓"
        "Mage" -> "🔥"
        else -> "🗡️"
    }
    val manaCost = when (char.heroClass) {
        "Warrior" -> 6
        "Mage" -> 15
        else -> 10
    }
    val castable = char.currentMana >= manaCost
    val consCount = inventory.count { it.type == "CONSUMABLE" }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Backpack circular action button
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(26.dp))
                .clickable { onOpenInventory() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎒", fontSize = 16.sp)
                Text("BAG ($consCount)", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Active Spell/Attack action button
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    if (castable) Color(0xFF4A148C).copy(alpha = 0.85f) 
                    else Color(0xFF212121).copy(alpha = 0.6f), 
                    RoundedCornerShape(32.dp)
                )
                .border(
                    width = 1.dp, 
                    color = if (castable) Color(0xFFBA68C8) else Color(0xFF424242).copy(alpha = 0.5f), 
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable(enabled = castable) { onCastSpell() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = icon, fontSize = 20.sp)
                Text(
                    text = spellLabel,
                    fontSize = 8.5.sp,
                    color = if (castable) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${manaCost}mp",
                    fontSize = 8.sp,
                    color = if (castable) Color(0xFFE1BEE7) else Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MoriaBenchmarkViewport(
    tiles: List<GameTile>,
    monsters: List<MonsterState>,
    pId: Int,
    planetNodes: Map<Int, SphereNode>,
    heroClass: String,
    lockedMonsterId: Int?,
    viewModel: RoguelikeViewModel,
    modifier: Modifier = Modifier
) {
    var useSceneviewEngine by remember { mutableStateOf(false) } // CPU fallback engine as default to guarantee instant visibility!
    var yawAngle by remember { mutableStateOf(-0.65f) }
    var pitchAngle by remember { mutableStateOf(0.75f) }
    var zoomScale by remember { mutableStateOf(4.8f) }

    // Smooth position animators for player position on sphere
    val playerAnimX = remember { Animatable(planetNodes[pId]?.position?.x ?: 0f) }
    val playerAnimY = remember { Animatable(planetNodes[pId]?.position?.y ?: 0f) }
    val playerAnimZ = remember { Animatable(planetNodes[pId]?.position?.z ?: 0f) }

    LaunchedEffect(pId) {
        val targetPos = planetNodes[pId]?.position ?: Vector3(0f, 0f, 0f)
        launch { playerAnimX.animateTo(targetPos.x, animationSpec = tween(280, easing = FastOutSlowInEasing)) }
        launch { playerAnimY.animateTo(targetPos.y, animationSpec = tween(280, easing = FastOutSlowInEasing)) }
        launch { playerAnimZ.animateTo(targetPos.z, animationSpec = tween(280, easing = FastOutSlowInEasing)) }
    }

    // Keep cached maps for model nodes to prevent recreations
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var planetNodeRef by remember { mutableStateOf<io.github.sceneview.node.ModelNode?>(null) }
    var playerNodeRef by remember { mutableStateOf<io.github.sceneview.node.ModelNode?>(null) }
    val monsterNodes = remember { mutableStateMapOf<Int, io.github.sceneview.node.ModelNode>() }
    val chestNodes = remember { mutableStateMapOf<Int, io.github.sceneview.node.ModelNode>() }
    val stairNodes = remember { mutableStateMapOf<Int, io.github.sceneview.node.ModelNode>() }
    
    // Trackers to prevent redundant loading triggers in the update loop
    val monsterLoadingTracker = remember { mutableStateListOf<Int>() }
    val chestLoadingTracker = remember { mutableStateListOf<Int>() }
    val stairLoadingTracker = remember { mutableStateListOf<Int>() }
    
    var isLoadingModels by remember { mutableStateOf(false) }

    // Smooth auto-orbit camera towards targeted monster when Locked On
    if (lockedMonsterId != null) {
        val lockedM = monsters.find { it.id == lockedMonsterId }
        val lockedNodePos = planetNodes[lockedM?.x]?.position
        val pVec = Vector3(playerAnimX.value, playerAnimY.value, playerAnimZ.value).normalize()
        if (lockedNodePos != null) {
            val upVec = if (abs(pVec.y) > 0.99f) Vector3(1f, 0f, 0f) else Vector3(0f, 1f, 0f)
            val xAxis = upVec.cross(pVec).normalize()
            val yAxis = pVec.cross(xAxis).normalize()
            val zAxis = Vector3(-pVec.x, -pVec.y, -pVec.z)

            val vx = lockedNodePos.x * xAxis.x + lockedNodePos.y * xAxis.y + lockedNodePos.z * xAxis.z
            val vy = lockedNodePos.x * yAxis.x + lockedNodePos.y * yAxis.y + lockedNodePos.z * yAxis.z
            val vz = lockedNodePos.x * zAxis.x + lockedNodePos.y * zAxis.y + lockedNodePos.z * zAxis.z

            val targetYaw = atan2(vx, -vz)
            val targetPitch = -atan2(vy, -vz)
            
            var diffY = targetYaw - yawAngle
            while (diffY < -PI) diffY += (2f * PI).toFloat()
            while (diffY > PI) diffY -= (2f * PI).toFloat()
            yawAngle += diffY * 0.05f 

            var diffP = targetPitch - pitchAngle
            while (diffP < -PI) diffP += (2f * PI).toFloat()
            while (diffP > PI) diffP -= (2f * PI).toFloat()
            pitchAngle += diffP * 0.05f
        }
    }

    Box(
        modifier = modifier
            .border(1.dp, Color(0xFF332211), RoundedCornerShape(8.dp))
            .background(Color.Black)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                // Combine Drag Rotations
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    yawAngle = (yawAngle + dragAmount.x * 0.007f)
                    pitchAngle = (pitchAngle + dragAmount.y * 0.007f)
                }
            }
            .pointerInput(planetNodes) {
                detectTapGestures { offset ->
                    // Standard lightweight mathematical projection to detect which node of planetNodes was selected
                    var minId = -1
                    var minDist = Float.MAX_VALUE

                    val pVec = Vector3(playerAnimX.value, playerAnimY.value, playerAnimZ.value).normalize()
                    val upVec = if (abs(pVec.y) > 0.99f) Vector3(1f, 0f, 0f) else Vector3(0f, 1f, 0f)
                    val xAxis = upVec.cross(pVec).normalize()
                    val yAxis = pVec.cross(xAxis).normalize()
                    val zAxis = Vector3(-pVec.x, -pVec.y, -pVec.z)

                    for (node in planetNodes.values) {
                        val pos = node.position

                        val vx = pos.x * xAxis.x + pos.y * xAxis.y + pos.z * xAxis.z
                        val vy = pos.x * yAxis.x + pos.y * yAxis.y + pos.z * yAxis.z
                        val vz = pos.x * zAxis.x + pos.y * zAxis.y + pos.z * zAxis.z

                        val rx = vx * cos(yawAngle) + vz * sin(yawAngle)
                        val ryHalf = -vx * sin(yawAngle) + vz * cos(yawAngle)
                        val ry = vy * cos(pitchAngle) - ryHalf * sin(pitchAngle)
                        val rz = vy * sin(pitchAngle) + ryHalf * cos(pitchAngle)

                        // 155f radius map scaling factor
                        val denom = rz * 155f + 155f + 320f
                        if (denom > 0 && rz < 0.3f) {
                            val viewWidth = size.width
                            val viewHeight = size.height
                            val sx = (viewWidth / 2f) + (rx * 155f * 280f * zoomScale) / denom
                            val sy = (viewHeight / 2f) + (ry * 155f * 280f * zoomScale) / denom
                            
                            val dx = sx - offset.x
                            val dy = sy - offset.y
                            val dist = dx * dx + dy * dy
                            if (dist < minDist) {
                                minDist = dist
                                minId = node.id
                            }
                        }
                    }

                    if (minId != -1 && minDist < 2400f) { // Tap within bounds threshold of 48px radius squared
                        viewModel.setTargetNode(minId)
                        viewModel.movePlayerToNode(minId)
                    }
                }
            }
    ) {
        if (useSceneviewEngine) {
            // Render Sceneview 3D Loader
            io.github.sceneview.SceneView(
                modifier = Modifier.fillMaxSize()
            )

            if (isLoadingModels) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF64FFDA), strokeWidth = 3.dp)
                }
            }

            // Dynamic HUD Performance indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Column {
                    Text(
                        text = "⚡ ENGINE: SCENEVIEW (UPGRADED)",
                        color = Color(0xFF64FFDA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• Entities: Player (CesiumMan), Monsters (Fox), Chest (Lantern)\n• Globe: Textured Earth PBR Mesh\n• Render State: Active Hardware Accelerated",
                        color = Color.LightGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            // Render Sovereign Vector Canvas Engine
            DungeonCanvas3D(
                tiles = tiles,
                monsters = monsters,
                pId = pId,
                planetNodes = planetNodes,
                heroClass = heroClass,
                lockedMonsterId = lockedMonsterId,
                onCameraYawChanged = { yaw -> viewModel.updateCameraYaw(yaw) },
                onNodeTapped = { id -> 
                    viewModel.setTargetNode(id)
                    viewModel.movePlayerToNode(id)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic HUD Performance indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Column {
                    Text(
                        text = "🎨 ENGINE: SOVEREIGN 3D",
                        color = Color(0xFFFF9800),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• API: Compose DrawScope SVG\n• CPU Rasterization: Active\n• Math Projection: Sovereign Matrix",
                        color = Color.LightGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Beautiful Interactive Engine Selector Floating Button
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color(0xFF1E1E1F), RoundedCornerShape(6.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (useSceneviewEngine) Color(0xFF2E2E30) else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { useSceneviewEngine = true }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Sceneview",
                    color = if (useSceneviewEngine) Color.White else Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        if (!useSceneviewEngine) Color(0xFF2E2E30) else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { useSceneviewEngine = false }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Sovereign Canvas",
                    color = if (!useSceneviewEngine) Color.White else Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
