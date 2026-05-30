package com.example.cameraxapp.roguelike

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.util.Random

enum class GameStatus {
    CHARACTER_SELECT,
    EXPLORING,
    INVENTORY_MODAL,
    GAME_OVER,
    VICTORY,
    SCORES_SCREEN
}

class RoguelikeViewModel(context: Context) : ViewModel() {

    private val dbHelper = RoguelikeDatabaseHelper(context)
    val audioEngine = RoguelikeAudioEngine()

    // Game states
    private val _status = mutableStateOf(GameStatus.CHARACTER_SELECT)
    val status: State<GameStatus> get() = _status

    private val _characterState = mutableStateOf<CharacterState?>(null)
    val characterState: State<CharacterState?> get() = _characterState

    private val _playerX = mutableStateOf(2)
    val playerX: State<Int> get() = _playerX

    private val _playerY = mutableStateOf(2)
    val playerY: State<Int> get() = _playerY

    private val _tiles = mutableStateOf<List<GameTile>>(emptyList())
    val tiles: State<List<GameTile>> get() = _tiles

    private val _monsters = mutableStateOf<List<MonsterState>>(emptyList())
    val monsters: State<List<MonsterState>> get() = _monsters

    private val _inventory = mutableStateOf<List<InventoryItem>>(emptyList())
    val inventory: State<List<InventoryItem>> get() = _inventory

    private val _gameLogs = mutableStateOf<List<String>>(listOf("Select an adventure class to begin..."))
    val gameLogs: State<List<String>> get() = _gameLogs

    private val _highScores = mutableStateOf<List<HighScore>>(emptyList())
    val highScores: State<List<HighScore>> get() = _highScores

    init {
        // Load existing session if present, otherwise start clean
        val activeChar = dbHelper.loadActiveCharacter()
        if (activeChar != null) {
            _characterState.value = activeChar
            _playerX.value = activeChar.playerX
            _playerY.value = activeChar.playerY
            _tiles.value = dbHelper.loadMap()
            _monsters.value = dbHelper.loadMonsters()
            _inventory.value = dbHelper.loadInventory()
            _status.value = GameStatus.EXPLORING
            _gameLogs.value = listOf(
                "Welcome back, adventurer!",
                "Loaded level ${activeChar.level} ${activeChar.heroClass} from floor ${activeChar.floor}."
            )
        } else {
            _status.value = GameStatus.CHARACTER_SELECT
        }
        loadHighScores()
    }

    fun loadHighScores() {
        _highScores.value = dbHelper.getHighScores()
    }

    // --- Action Handlers ---

    fun selectClass(heroClass: String) {
        val charState = when (heroClass) {
            "Warrior" -> CharacterState(
                heroClass = "Warrior",
                currentHp = 120,
                maxHp = 120,
                currentMana = 20,
                maxMana = 20,
                strength = 14,
                dexterity = 8,
                intelligence = 5,
                vitality = 14,
                floor = 1,
                playerX = 2,
                playerY = 2,
                turns = 0
            )
            "Mage" -> CharacterState(
                heroClass = "Mage",
                currentHp = 75,
                maxHp = 75,
                currentMana = 100,
                maxMana = 100,
                strength = 5,
                dexterity = 8,
                intelligence = 15,
                vitality = 8,
                floor = 1,
                playerX = 2,
                playerY = 2,
                turns = 0
            )
            else -> CharacterState(
                heroClass = "Rogue",
                currentHp = 90,
                maxHp = 90,
                currentMana = 40,
                maxMana = 40,
                strength = 10,
                dexterity = 14,
                intelligence = 8,
                vitality = 10,
                floor = 1,
                playerX = 2,
                playerY = 2,
                turns = 0
            )
        }

        val startItems = when (heroClass) {
            "Warrior" -> listOf(
                InventoryItem(name = "Iron Sword", type = "WEAPON", statMod = 6, isEquipped = true),
                InventoryItem(name = "Wooden Shield", type = "ARMOR", statMod = 3, isEquipped = true),
                InventoryItem(name = "Health Potion", type = "CONSUMABLE", statMod = 40, isEquipped = false)
            )
            "Mage" -> listOf(
                InventoryItem(name = "Apprentice Staff", type = "WEAPON", statMod = 4, isEquipped = true),
                InventoryItem(name = "Cloth Robes", type = "ARMOR", statMod = 1, isEquipped = true),
                InventoryItem(name = "Mana Potion", type = "CONSUMABLE", statMod = 50, isEquipped = false)
            )
            else -> listOf(
                InventoryItem(name = "Poison Dagger", type = "WEAPON", statMod = 5, isEquipped = true),
                InventoryItem(name = "Leather Armor", type = "ARMOR", statMod = 2, isEquipped = true),
                InventoryItem(name = "Health Potion", type = "CONSUMABLE", statMod = 40, isEquipped = false)
            )
        }

        _characterState.value = charState
        _inventory.value = startItems
        _gameLogs.value = listOf("Welcome, level 1 $heroClass!", "Explore the procedurally generated corridors. Defeat the Dragon on Floor 10.")
        
        generateFloor(1)

        val updatedChar = charState.copy(playerX = _playerX.value, playerY = _playerY.value)
        _characterState.value = updatedChar
        saveCurrentTurnState()

        _status.value = GameStatus.EXPLORING
        audioEngine.playLevelUp()
    }

    fun openInventory() {
        if (_status.value == GameStatus.EXPLORING) {
            _status.value = GameStatus.INVENTORY_MODAL
        }
    }

    fun closeInventory() {
        if (_status.value == GameStatus.INVENTORY_MODAL) {
            _status.value = GameStatus.EXPLORING
        }
    }

    fun toggleScores() {
        if (_status.value == GameStatus.SCORES_SCREEN) {
            _status.value = GameStatus.CHARACTER_SELECT
        } else {
            _status.value = GameStatus.SCORES_SCREEN
        }
    }

    fun resetStatsAndScores() {
        dbHelper.resetHighScores()
        loadHighScores()
    }

    fun movePlayer(dx: Int, dy: Int) {
        val char = _characterState.value ?: return
        if (_status.value != GameStatus.EXPLORING) return

        val nextX = _playerX.value + dx
        val nextY = _playerY.value + dy

        // Edge boundaries check
        if (nextX < 0 || nextX >= 18 || nextY < 0 || nextY >= 18) return

        // Wall collision check
        val targetTile = _tiles.value.find { it.x == nextX && it.y == nextY }
        if (targetTile?.tileType == "WALL") {
            addCombatLog("Ouch! You bumped into a solid dungeon wall.")
            audioEngine.playMove() // quick sound for feedback
            return
        }

        // Hostile collision strike check
        val monster = _monsters.value.find { it.x == nextX && it.y == nextY }
        if (monster != null) {
            performAttack(char, monster)
            triggerMonsterTurns()
            _characterState.value = _characterState.value?.copy(turns = char.turns + 1)
            saveCurrentTurnState()
            return
        }

        // Standard walking movement
        _playerX.value = nextX
        _playerY.value = nextY
        audioEngine.playMove()

        recalculateFogOfWar(nextX, nextY)
        triggerMonsterTurns()

        _characterState.value = _characterState.value?.copy(
            playerX = nextX,
            playerY = nextY,
            turns = char.turns + 1
        )
        saveCurrentTurnState()
    }

    fun enableAction() {
        val char = _characterState.value ?: return
        if (_status.value != GameStatus.EXPLORING) return

        val currentTile = _tiles.value.find { it.x == _playerX.value && it.y == _playerY.value }
        if (currentTile != null) {
            when (currentTile.tileType) {
                "STAIRS_DOWN" -> {
                    advanceFloor(char)
                    return
                }
                "CHEST" -> {
                    openChest(_playerX.value, _playerY.value)
                    triggerMonsterTurns()
                    _characterState.value = _characterState.value?.copy(turns = char.turns + 1)
                    saveCurrentTurnState()
                    return
                }
            }
        }

        // Default or wait action
        addCombatLog("Nothing to interact with here. You wait a turn.")
        triggerMonsterTurns()
        _characterState.value = _characterState.value?.copy(turns = char.turns + 1)
        saveCurrentTurnState()
    }

    fun drinkHealthPotion() {
        val char = _characterState.value ?: return
        if (_status.value != GameStatus.EXPLORING) return
        val hpPotion = _inventory.value.find { it.name.contains("Health Potion") && it.type == "CONSUMABLE" }
        if (hpPotion != null) {
            useItem(hpPotion)
        } else {
            addCombatLog("No Health Potions available!")
        }
    }

    fun drinkManaPotion() {
        val char = _characterState.value ?: return
        if (_status.value != GameStatus.EXPLORING) return
        val mpPotion = _inventory.value.find { it.name.contains("Mana Potion") && it.type == "CONSUMABLE" }
        if (mpPotion != null) {
            useItem(mpPotion)
        } else {
            addCombatLog("No Mana Potions available!")
        }
    }

    fun useItem(item: InventoryItem) {
        val char = _characterState.value ?: return
        if (item.type != "CONSUMABLE") return

        audioEngine.playItem()
        if (item.name.contains("Health Potion")) {
            val healedHp = minOf(char.maxHp, char.currentHp + item.statMod)
            val delta = healedHp - char.currentHp
            _characterState.value = char.copy(currentHp = healedHp)
            addCombatLog("Consumed local Health Potion. Recovered $delta HP! 🧪")
        } else if (item.name.contains("Mana Potion")) {
            val healedMana = minOf(char.maxMana, char.currentMana + item.statMod)
            val delta = healedMana - char.currentMana
            _characterState.value = char.copy(currentMana = healedMana)
            addCombatLog("Consumed local Mana Potion. Recovered $delta Mana! 🧪")
        }

        // Consume item
        _inventory.value = _inventory.value.filter { it.id != item.id }
        saveCurrentTurnState()
    }

    fun equipItem(item: InventoryItem) {
        if (item.type == "CONSUMABLE") return

        // Unequip current items of the same type
        val updated = _inventory.value.map {
            if (it.type == item.type) {
                it.copy(isEquipped = it.id == item.id)
            } else {
                it
            }
        }
        _inventory.value = updated
        addCombatLog("Equipped ${item.name}! [Stat modifier: +${item.statMod}]")
        audioEngine.playItem()
        saveCurrentTurnState()
    }

    // Cast unique class magic spell
    fun castClassSpell() {
        val char = _characterState.value ?: return
        if (_status.value != GameStatus.EXPLORING) return

        when (char.heroClass) {
            "Warrior" -> {
                // Cleave spell
                if (char.currentMana < 6) {
                    addCombatLog("Not enough Mana (6 required).")
                    return
                }
                _characterState.value = char.copy(currentMana = char.currentMana - 6)
                audioEngine.playCast()
                addCombatLog("You execute a massive CLEAVE strike! ⚔️")
                
                // Strike all adjacent horizontal / vertical grids
                val adjPositions = listOf(
                    Pair(_playerX.value - 1, _playerY.value),
                    Pair(_playerX.value + 1, _playerY.value),
                    Pair(_playerX.value, _playerY.value - 1),
                    Pair(_playerX.value, _playerY.value + 1)
                )

                _monsters.value.filter { Pair(it.x, it.y) in adjPositions }.forEach { monster ->
                    dealSpellDamage(monster, (char.strength * 1.5).toInt())
                }
            }
            "Mage" -> {
                // Fireball ranged cast
                if (char.currentMana < 15) {
                    addCombatLog("Not enough Mana (15 required).")
                    return
                }
                _characterState.value = char.copy(currentMana = char.currentMana - 15)
                audioEngine.playCast()
                
                // Strike nearest monster on screen
                val targetMonster = findNearestRevealedMonster()
                if (targetMonster != null) {
                    addCombatLog("You cast a scorching FIREBALL at ${targetMonster.type}! 🔥")
                    dealSpellDamage(targetMonster, (char.intelligence * 2.5).toInt())
                } else {
                    addCombatLog("You release a Fireball but no monsters are in sight. Dust settles...")
                }
            }
            "Rogue" -> {
                // Steal / Backstab spell
                if (char.currentMana < 10) {
                    addCombatLog("Not enough Mana (10 required).")
                    return
                }
                _characterState.value = char.copy(currentMana = char.currentMana - 10)
                audioEngine.playCast()

                // High damage strike to adjacent monster
                val adjMonster = _monsters.value.find { m ->
                    Math.abs(m.x - _playerX.value) <= 1 && Math.abs(m.y - _playerY.value) <= 1
                }
                if (adjMonster != null) {
                    addCombatLog("You emerge from shadows executing a BACKSTAB strike! 🗡️")
                    dealSpellDamage(adjMonster, (char.dexterity * 3.0).toInt())
                } else {
                    addCombatLog("You conceal yourself in shadows, but there are no foes nearby.")
                }
            }
        }
        
        triggerMonsterTurns()
        _characterState.value = _characterState.value?.copy(turns = _characterState.value!!.turns + 1)
        saveCurrentTurnState()
    }

    fun restartGame() {
        _characterState.value = null
        dbHelper.clearAllActiveRun()
        _status.value = GameStatus.CHARACTER_SELECT
    }

    // --- Private Logics & Math Engine ---

    private fun performAttack(char: CharacterState, monster: MonsterState) {
        val weaponMod = _inventory.value.find { it.type == "WEAPON" && it.isEquipped }?.statMod ?: 0
        val baseDmg = char.strength + weaponMod
        
        val random = Random()
        val isCrit = random.nextFloat() < (0.05f + char.dexterity * 0.01f)
        val finalDmg = if (isCrit) baseDmg * 2 else baseDmg + random.nextInt(4)

        audioEngine.playAttack()
        val critLabel = if (isCrit) "CRITICAL HIT! " else ""
        addCombatLog("You strike the ${monster.type} for $critLabel$finalDmg damage!")

        val remainingHp = monster.currentHp - finalDmg
        if (remainingHp <= 0) {
            audioEngine.playHit()
            addCombatLog("You defeated the ${monster.type}! Collected ${monster.maxHp / 2} gold coins which fell on the floor. 💰")
            
            // Gain Experience & Gold
            val expGained = monster.maxHp / 2
            val goldGained = monster.maxHp / 2 + random.nextInt(5)
            
            _monsters.value = _monsters.value.filter { it.id != monster.id }
            
            // Check dragon victory condition
            if (monster.type == "DRAGON") {
                triggerVictorySequence(char)
                return
            }

            gainExpAndGold(char, expGained, goldGained)
        } else {
            _monsters.value = _monsters.value.map {
                if (it.id == monster.id) it.copy(currentHp = remainingHp) else it
            }
        }
    }

    private fun dealSpellDamage(monster: MonsterState, damage: Int) {
        val remainingHp = monster.currentHp - damage
        audioEngine.playHit()
        addCombatLog("Spell hits the ${monster.type} for $damage magic damage!")

        if (remainingHp <= 0) {
            addCombatLog("You vaporised the ${monster.type}! Collected ${monster.maxHp / 2} gold. 💰")
            val expGained = monster.maxHp / 2
            val goldGained = monster.maxHp / 2 + Random().nextInt(5)
            
            _monsters.value = _monsters.value.filter { it.id != monster.id }
            
            if (monster.type == "DRAGON") {
                triggerVictorySequence(_characterState.value!!)
                return
            }

            gainExpAndGold(_characterState.value!!, expGained, goldGained)
        } else {
            _monsters.value = _monsters.value.map {
                if (it.id == monster.id) it.copy(currentHp = remainingHp) else it
            }
        }
    }

    private fun gainExpAndGold(char: CharacterState, exp: Int, gold: Int) {
        var currentExp = char.exp + exp
        val reqExp = char.level * 100
        var level = char.level
        var maxHp = char.maxHp
        var maxMana = char.maxMana
        var strength = char.strength
        var dexterity = char.dexterity
        var intelligence = char.intelligence
        var vitality = char.vitality

        var leveledUp = false
        if (currentExp >= reqExp) {
            leveledUp = true
            currentExp -= reqExp
            level++
            
            // Scale attributes based of archetype
            when (char.heroClass) {
                "Warrior" -> {
                    strength += 3
                    vitality += 3
                    dexterity += 1
                }
                "Mage" -> {
                    intelligence += 4
                    maxMana += 25
                    vitality += 1
                }
                else -> { // Rogue
                    dexterity += 3
                    strength += 2
                    vitality += 2
                }
            }
            maxHp += (vitality * 2.5).toInt()
            addCombatLog("✨ LEVEL UP! You reached LEVEL $level! Health scaled up.")
            audioEngine.playLevelUp()
        }

        _characterState.value = char.copy(
            level = level,
            exp = currentExp,
            gold = char.gold + gold,
            maxHp = maxHp,
            currentHp = if (leveledUp) maxHp else minOf(maxHp, char.currentHp + 5), // heal 5 per kill
            maxMana = maxMana,
            currentMana = if (leveledUp) maxMana else minOf(maxMana, char.currentMana + 3),
            strength = strength,
            dexterity = dexterity,
            intelligence = intelligence,
            vitality = vitality
        )
    }

    private fun triggerMonsterTurns() {
        val char = _characterState.value ?: return
        val random = Random()
        val armorMod = _inventory.value.find { it.type == "ARMOR" && it.isEquipped }?.statMod ?: 0
        var playerCurrentHp = char.currentHp

        val updatedMonsters = _monsters.value.map { monster ->
            val dist = Math.sqrt(Math.pow((monster.x - _playerX.value).toDouble(), 2.0) + Math.pow((monster.y - _playerY.value).toDouble(), 2.0))
            if (dist <= 1.2) {
                // Symmetrical attack
                val baseMonsterDmg = when (monster.type) {
                    "DRAGON" -> 22 + char.floor
                    "NECROMANCER" -> 14 + char.floor
                    "GOBLIN" -> 10 + char.floor
                    else -> 6 + char.floor // SKELETON
                }
                val finalMonsterDmg = maxOf(1, baseMonsterDmg - armorMod)
                playerCurrentHp = maxOf(0, playerCurrentHp - finalMonsterDmg)
                addCombatLog("💀 The ${monster.type} strikes you for $finalMonsterDmg damage.")
                
                monster // no moves, stays and fights
            } else if (dist <= 5.0) {
                // Pathfinding step towards player
                val dxToPlayer = (_playerX.value - monster.x).coerceIn(-1, 1)
                val dyToPlayer = (_playerY.value - monster.y).coerceIn(-1, 1)
                
                val nextM_X = monster.x + dxToPlayer
                val nextM_Y = monster.y + dyToPlayer

                // Grid validation for monsters
                val canMove = _tiles.value.find { it.x == nextM_X && it.y == nextM_Y }?.tileType != "WALL" &&
                        _monsters.value.none { it.x == nextM_X && it.y == nextM_Y } &&
                        !(nextM_X == _playerX.value && nextM_Y == _playerY.value)

                if (canMove) {
                    monster.copy(x = nextM_X, y = nextM_Y)
                } else {
                    monster
                }
            } else {
                monster
            }
        }

        _monsters.value = updatedMonsters

        if (playerCurrentHp <= 0) {
            triggerGameOverSequence(char)
        } else {
            _characterState.value = char.copy(currentHp = playerCurrentHp)
        }
    }

    private fun triggerGameOverSequence(char: CharacterState) {
        addCombatLog("💔 Game Over! You fell victim to the dark dungeon on Floor ${char.floor}.")
        audioEngine.playDeath()
        _status.value = GameStatus.GAME_OVER

        // Commit career High Score
        dbHelper.saveHighScore(
            HighScore(
                heroClass = char.heroClass,
                level = char.level,
                floor = char.floor,
                gold = char.gold,
                turns = char.turns,
                survived = false
            )
        )
        // Hardcore permadeath cleanup
        dbHelper.clearAllActiveRun()
        loadHighScores()
    }

    private fun triggerVictorySequence(char: CharacterState) {
        addCombatLog("🎉 VICTORY! You have slain the Shadow Dragon and recovered the Ancestral Relic!")
        audioEngine.playLevelUp()
        _status.value = GameStatus.VICTORY

        dbHelper.saveHighScore(
            HighScore(
                heroClass = char.heroClass,
                level = char.level,
                floor = char.floor,
                gold = char.gold,
                turns = char.turns,
                survived = true
            )
        )
        dbHelper.clearAllActiveRun()
        loadHighScores()
    }

    private fun openChest(x: Int, y: Int) {
        val random = Random()
        val rolls = random.nextInt(100)
        audioEngine.playItem()

        if (rolls < 40) {
            val gained = 15 + random.nextInt(35) + _characterState.value!!.floor * 6
            _characterState.value = _characterState.value!!.copy(gold = _characterState.value!!.gold + gained)
            addCombatLog("You found a treasure chest! Smashed it open to acquire $gained gold! 💰")
        } else if (rolls < 70) {
            val isHeal = random.nextBoolean()
            val potionLabel = if (isHeal) "Health Potion" else "Mana Potion"
            val potency = if (isHeal) 45 else 60
            val item = InventoryItem(name = potionLabel, type = "CONSUMABLE", statMod = potency)
            _inventory.value = _inventory.value + item
            addCombatLog("Smashed chest open: Discovered a critical $potionLabel! 🧪")
        } else {
            val isWeapon = random.nextBoolean()
            val upgradedType = if (isWeapon) "WEAPON" else "ARMOR"
            val rating = 2 + random.nextInt(5) + _characterState.value!!.floor
            val upgradedLabel = if (isWeapon) {
                val weapons = listOf("Broadsword", "Spike Mace", "Arcane Staff", "Vandal Spear", "Mythril Blade")
                weapons[random.nextInt(weapons.size)] + " (+$rating)"
            } else {
                val armors = listOf("Steel Breastplate", "Kevlar Mail", "Enchanted Garb", "Obsidian Shield", "Plate Guard")
                armors[random.nextInt(armors.size)] + " (+$rating)"
            }
            val item = InventoryItem(name = upgradedLabel, type = upgradedType, statMod = rating)
            _inventory.value = _inventory.value + item
            addCombatLog("Jackpot Chest! Discovered ancient relics: $upgradedLabel [+ $rating]! 🎒")
        }

        // Change tile type from CHEST to FLOOR
        _tiles.value = _tiles.value.map {
            if (it.x == x && it.y == y) it.copy(tileType = "FLOOR") else it
        }
    }

    private fun advanceFloor(char: CharacterState) {
        val nextFloor = char.floor + 1
        if (nextFloor > 10) {
            triggerVictorySequence(char)
            return
        }

        addCombatLog("Descended narrow stairs down towards Floor $nextFloor... 🐉")
        audioEngine.playLevelUp()

        _characterState.value = char.copy(floor = nextFloor)
        generateFloor(nextFloor)
        
        // Auto-save map and character after floor loading
        val finalState = _characterState.value!!.copy(
            playerX = _playerX.value,
            playerY = _playerY.value
        )
        _characterState.value = finalState
        saveCurrentTurnState()
    }

    private fun generateFloor(floorIndex: Int) {
        val nextTiles = mutableMapOf<Pair<Int, Int>, GameTile>()
        // Initialize 18x18 grid completely filled with solid wall
        for (y in 0 until 18) {
            for (x in 0 until 18) {
                nextTiles[Pair(x, y)] = GameTile(x, y, "WALL", false)
            }
        }

        data class RoomSpec(val rx: Int, val ry: Int, val rw: Int, val rh: Int)
        val rooms = mutableListOf<RoomSpec>()
        val rnd = Random()

        // Place 4 procedural squares
        for (i in 0 until 4) {
            val rw = 3 + rnd.nextInt(3)
            val rh = 3 + rnd.nextInt(3)
            val rx = 1 + rnd.nextInt(18 - rw - 2)
            val ry = 1 + rnd.nextInt(18 - rh - 2)
            rooms.add(RoomSpec(rx, ry, rw, rh))
        }

        // Carve rooms
        for (room in rooms) {
            for (y in room.ry until room.ry + room.rh) {
                for (x in room.rx until room.rx + room.rw) {
                    nextTiles[Pair(x, y)] = GameTile(x, y, "FLOOR", false)
                }
            }
        }

        // Segment corridors horizontally and vertically
        for (i in 0 until rooms.size - 1) {
            val r1 = rooms[i]
            val r2 = rooms[i + 1]
            val cx1 = r1.rx + r1.rw / 2
            val cy1 = r1.ry + r1.rh / 2
            val cx2 = r2.rx + r2.rw / 2
            val cy2 = r2.ry + r2.rh / 2

            // Horizontal step Carver
            val minX = minOf(cx1, cx2)
            val maxX = maxOf(cx1, cx2)
            for (x in minX..maxX) {
                nextTiles[Pair(x, cy1)] = GameTile(x, cy1, "FLOOR", false)
            }

            // Vertical step Carver
            val minY = minOf(cy1, cy2)
            val maxY = maxOf(cy1, cy2)
            for (y in minY..maxY) {
                nextTiles[Pair(cx2, y)] = GameTile(cx2, y, "FLOOR", false)
            }
        }

        // Hero initial landing spawn
        val startingRoom = rooms[0]
        val px = startingRoom.rx + startingRoom.rw / 2
        val py = startingRoom.ry + startingRoom.rh / 2
        nextTiles[Pair(px, py)] = GameTile(px, py, "FLOOR", false)

        // Exit staircase placement in bottom-most room
        val lastRoom = rooms.last()
        var sx = lastRoom.rx + lastRoom.rw / 2
        var sy = lastRoom.ry + lastRoom.rh / 2
        if (sx == px && sy == py) {
            sx = (sx + 2) % 17
            sy = (sy + 2) % 17
        }
        nextTiles[Pair(sx, sy)] = GameTile(sx, sy, "STAIRS_DOWN", false)

        // Randomly place 3 treasure chests
        var chestPlaced = 0
        var mapTry = 0
        while (chestPlaced < 3 && mapTry < 40) {
            mapTry++
            val randomRoom = rooms[rnd.nextInt(rooms.size)]
            val cx = randomRoom.rx + rnd.nextInt(randomRoom.rw)
            val cy = randomRoom.ry + rnd.nextInt(randomRoom.rh)
            if ((cx != px || cy != py) && (cx != sx || cy != sy)) {
                nextTiles[Pair(cx, cy)] = GameTile(cx, cy, "CHEST", false)
                chestPlaced++
            }
        }

        // Place hostiles in rooms (scaling with depth)
        val floorMonsters = mutableListOf<MonsterState>()
        var globalMonsterIndex = 1

        for (idx in 1 until rooms.size) {
            val r = rooms[idx]
            val maxMCount = if (floorIndex >= 7) 2 else 1
            for (mc in 0 until maxMCount) {
                val mx = r.rx + rnd.nextInt(r.rw)
                val my = r.ry + rnd.nextInt(r.rh)
                if ((mx != sx || my != sy) && !(mx == px && my == py)) {
                    val hostileClass = when {
                        floorIndex >= 9 && idx == rooms.size - 1 -> "DRAGON" // Place boss on last rooms
                        floorIndex >= 7 && rnd.nextBoolean() -> "NECROMANCER"
                        floorIndex >= 4 && rnd.nextBoolean() -> "GOBLIN"
                        else -> "SKELETON"
                    }

                    val fullMonsterHp = when (hostileClass) {
                        "DRAGON" -> 160 + floorIndex * 15
                        "NECROMANCER" -> 50 + floorIndex * 7
                        "GOBLIN" -> 35 + floorIndex * 5
                        else -> 24 + floorIndex * 4
                    }

                    floorMonsters.add(
                        MonsterState(
                            id = globalMonsterIndex++,
                            type = hostileClass,
                            currentHp = fullMonsterHp,
                            maxHp = fullMonsterHp,
                            x = mx,
                            y = my
                        )
                    )
                }
            }
        }

        _playerX.value = px
        _playerY.value = py
        _tiles.value = nextTiles.values.toList()
        _monsters.value = floorMonsters

        recalculateFogOfWar(px, py)
    }

    private fun recalculateFogOfWar(heroX: Int, heroY: Int) {
        val lightBoundary = 3.5
        _tiles.value = _tiles.value.map { tile ->
            val pythDist = Math.sqrt(Math.pow((tile.x - heroX).toDouble(), 2.0) + Math.pow((tile.y - heroY).toDouble(), 2.0))
            if (pythDist <= lightBoundary) {
                tile.copy(revealed = true)
            } else {
                tile
            }
        }
    }

    private fun findNearestRevealedMonster(): MonsterState? {
        val revealedMonsters = _monsters.value.filter { m ->
            _tiles.value.find { it.x == m.x && it.y == m.y }?.revealed == true
        }

        return revealedMonsters.minByOrNull { m ->
            Math.sqrt(Math.pow((m.x - _playerX.value).toDouble(), 2.0) + Math.pow((m.y - _playerY.value).toDouble(), 2.0))
        }
    }

    private fun addCombatLog(feed: String) {
        val fullFeeds = _gameLogs.value.toMutableList()
        fullFeeds.add(feed)
        if (fullFeeds.size > 22) {
            fullFeeds.removeAt(0)
        }
        _gameLogs.value = fullFeeds
    }

    private fun saveCurrentTurnState() {
        val char = _characterState.value ?: return
        dbHelper.saveActiveCharacter(char.copy(playerX = _playerX.value, playerY = _playerY.value))
        dbHelper.saveMap(_tiles.value)
        dbHelper.saveMonsters(_monsters.value)
        dbHelper.saveInventory(_inventory.value)
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
