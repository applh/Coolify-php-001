package com.example.cameraxapp.roguelike

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
    val playerY by viewModel.playerY

    var is3DMode by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🗡️ RogueCompose RPG", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (status == GameStatus.EXPLORING || status == GameStatus.INVENTORY_MODAL) {
                        IconButton(onClick = { is3DMode = !is3DMode }) {
                            Text(
                                text = if (is3DMode) "2D Retro" else "3D Cube",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                    if (status != GameStatus.CHARACTER_SELECT && status != GameStatus.SCORES_SCREEN) {
                        IconButton(onClick = { viewModel.restartGame() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Forfeit", tint = Color.Red)
                        }
                    }
                    IconButton(onClick = { viewModel.toggleScores() }) {
                        Icon(Icons.Default.Info, contentDescription = "Leaderboard", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White
                )
            )
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
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 1. Level & Attributes Header
                            StatsHeader(char = char)

                            // 2. Playable 2D Grid Canvas or 3D Isometric View
                            if (is3DMode) {
                                DungeonCanvas3D(
                                    tiles = tiles,
                                    monsters = monsters,
                                    pX = playerX,
                                    pY = playerY,
                                    heroClass = char.heroClass,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .border(1.5.dp, Color(0xFF332211), RoundedCornerShape(4.dp))
                                        .background(Color.Black)
                                )
                            } else {
                                DungeonCanvas(
                                    tiles = tiles,
                                    monsters = monsters,
                                    pX = playerX,
                                    pY = playerY,
                                    heroClass = char.heroClass,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .border(1.5.dp, Color(0xFF332211), RoundedCornerShape(4.dp))
                                        .background(Color.Black)
                                )
                            }

                            // 3. Game combat logger console
                            CombatLoggerView(logs = logs)

                            // 4. Inventory quick belt / Spell triggers
                            FloatingActionsBelt(
                                char = char,
                                inventory = inventory,
                                onOpenInventory = { viewModel.openInventory() },
                                onCastSpell = { viewModel.castClassSpell() }
                            )

                            // 5. Floating Touch Controller
                            TouchDPad(onMove = { dx, dy -> viewModel.movePlayer(dx, dy) })
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
fun StatsHeader(char: CharacterState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(0.7.dp, Color(0xFF2D2A27), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // General Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${char.heroClass} [Lv ${char.level}]",
                    color = Color(0xFFFFD700),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Floor ${char.floor}/10",
                    color = Color(0xFFE57373),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "💰 ${char.gold}g",
                    color = Color(0xFFFFEE58),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))

            // HP Bar
            val hpRatio = char.currentHp.toFloat() / char.maxHp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("HP: ", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(36.dp))
                LinearProgressIndicator(
                    progress = { hpRatio.coerceIn(0f, 1f) },
                    color = Color(0xFFE57373),
                    trackColor = Color(0xFF331111),
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                )
                Text(" ${char.currentHp}/${char.maxHp}", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(2.dp))

            // Mana Bar
            val mpRatio = if (char.maxMana == 0) 0f else char.currentMana.toFloat() / char.maxMana
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MANA: ", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(36.dp))
                LinearProgressIndicator(
                    progress = { mpRatio.coerceIn(0f, 1f) },
                    color = Color(0xFF64B5F6),
                    trackColor = Color(0xFF112233),
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                )
                Text(" ${char.currentMana}/${char.maxMana}", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(2.dp))

            // EXP Bar
            val expRatio = char.exp.toFloat() / (char.level * 100)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("EXP: ", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(36.dp))
                LinearProgressIndicator(
                    progress = { expRatio.coerceIn(0f, 1f) },
                    color = Color(0xFF81C784),
                    trackColor = Color(0xFF112211),
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                )
                Text(" ${char.exp}/${char.level * 100}", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
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
                tile.tileType == "STAIRS_DOWN" -> Color(0xFF2B163B) // Arcane deep violet staircase
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
fun CombatLoggerView(logs: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .padding(vertical = 4.dp)
            .border(0.6.dp, Color(0xFF332A20), RoundedCornerShape(4.dp))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
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
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
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
fun TouchDPad(onMove: (Int, Int) -> Unit) {
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

        // Row 2: LEFT | WAIT | RIGHT
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

            // WAIT turn option
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF2C2216), RoundedCornerShape(20))
                    .clickable { onMove(0, 0) },
                contentAlignment = Alignment.Center
            ) {
                Text("⏳", color = Color.White, fontSize = 14.sp)
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
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(items) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFF1F1F1F), RoundedCornerShape(4.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

                                if (item.type == "CONSUMABLE") {
                                    Button(
                                        onClick = { onUse(item) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Use", color = Color.White, fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { onEquip(item) },
                                        enabled = !item.isEquipped,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0D47A1),
                                            disabledContainerColor = Color.Transparent
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(if (item.isEquipped) "Armed" else "Equip", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
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
