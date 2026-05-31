package com.example.cameraxapp.roguelike

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

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

    private val _lockedMonsterId = mutableStateOf<Int?>(null)
    val lockedMonsterId: State<Int?> get() = _lockedMonsterId

    private var joyX = 0f
    private var joyY = 0f
    private var currentCameraYaw = -0.65f
    private val _cameraYaw = mutableStateOf(-0.65f)
    val cameraYaw: State<Float> get() = _cameraYaw
    private var gameLoopJob: Job? = null
    private var lastAttackTime: Long = 0

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

        val updatedChar = charState.copy(playerX = _playerX.value)
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
        // Obsolete flat map movement
    }

    fun updateCameraYaw(yaw: Float) {
        currentCameraYaw = yaw
        _cameraYaw.value = yaw
    }

    fun updateJoystickInput(jx: Float, jy: Float, cameraYaw: Float) {
        // Obsolete
    }

    fun toggleTargetLock() {
        if (_lockedMonsterId.value != null) {
            _lockedMonsterId.value = null
            addCombatLog("Target lock-on deactivated.")
        } else {
            val nearest = findNearestRevealedMonster()
            if (nearest != null) {
                _lockedMonsterId.value = nearest.id
                addCombatLog("Locked-on to ${nearest.type}! 🎯")
            } else {
                addCombatLog("No visible monsters nearby to lock-on.")
            }
        }
    }

    fun enableAction() {
        val char = _characterState.value ?: return
        if (_status.value != GameStatus.EXPLORING) return

        val currentTile = _tiles.value.find { it.x == _playerX.value }
        if (currentTile != null) {
            when (currentTile.tileType) {
                "STAIRS_DOWN" -> {
                    advanceFloor(char)
                    return
                }
                "CHEST" -> {
                    openChest(_playerX.value)
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
                val curId = _playerX.value
                val adjPositions = planetNodes[curId]?.neighbors ?: emptyList()

                _monsters.value.filter { it.x in adjPositions }.forEach { monster ->
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
                val curId = _playerX.value
                val adjPositions = planetNodes[curId]?.neighbors ?: emptyList()
                val adjMonster = _monsters.value.find { m -> m.x in adjPositions }
                
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
        val random = java.util.Random()
        val armorMod = _inventory.value.find { it.type == "ARMOR" && it.isEquipped }?.statMod ?: 0
        var playerCurrentHp = char.currentHp
        val pPos = planetNodes[_playerX.value]?.position ?: return

        val updatedMonsters = _monsters.value.map { monster ->
            val mPos = planetNodes[monster.x]?.position ?: return@map monster
            val dist = (mPos - pPos).length()
            val isAdjacent = planetNodes[monster.x]?.neighbors?.contains(_playerX.value) == true

            if (isAdjacent) {
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
            } else if (dist <= 1.25f) { // roughly 5 tiles away on a planet of radius 1
                // Pathfinding step towards player
                val neighbors = planetNodes[monster.x]?.neighbors ?: emptyList()
                val nextMove = neighbors.minByOrNull { nId ->
                    val nPos = planetNodes[nId]?.position ?: return@minByOrNull Float.MAX_VALUE
                    (nPos - pPos).length()
                }

                if (nextMove != null) {
                    // Grid validation for monsters
                    val canMove = _tiles.value.find { it.x == nextMove }?.tileType != "WALL" &&
                            _monsters.value.none { it.x == nextMove } &&
                            nextMove != _playerX.value

                    if (canMove) {
                        monster.copy(x = nextMove)
                    } else {
                        monster
                    }
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

    private fun openChest(nodeId: Int) {
        val random = java.util.Random()
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
            if (it.x == nodeId) it.copy(tileType = "FLOOR") else it
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
            playerX = _playerX.value
        )
        _characterState.value = finalState
        saveCurrentTurnState()
    }

    val planetNodes = IcosphereGenerator.generate(3) // 642 nodes

    private fun findNextStep(start: Int, end: Int): Int? {
        val queue = ArrayDeque<Int>()
        val cameFrom = mutableMapOf<Int, Int>()
        queue.add(start)
        cameFrom[start] = start

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            if (curr == end) break
            
            planetNodes[curr]?.neighbors?.forEach { next ->
                if (!cameFrom.containsKey(next)) {
                    val tile = _tiles.value.find { it.x == next }
                    // Allow targeting monsters or chests, but don't walk through walls
                    if (tile?.tileType != "WALL") {
                        cameFrom[next] = curr
                        queue.add(next)
                    }
                }
            }
        }
        
        if (!cameFrom.containsKey(end)) return null
        
        var step = end
        while (cameFrom[step] != start) {
            val prev = cameFrom[step] ?: return null
            if (prev == start) return step
            step = prev
        }
        return step
    }

    fun movePlayerToNode(nodeId: Int) {
        val char = _characterState.value ?: return
        if (_status.value != GameStatus.EXPLORING) return

        val currentNodeId = _playerX.value
        
        var targetId = nodeId
        val isNeighbor = planetNodes[currentNodeId]?.neighbors?.contains(targetId) == true
        if (currentNodeId != targetId && !isNeighbor) {
            val step = findNextStep(currentNodeId, targetId)
            if (step != null) {
                targetId = step
            } else {
                return
            }
        }

        if (currentNodeId == targetId) {
            // Wait turn
            addCombatLog("You wait for an opening...")
            triggerMonsterTurns()
            _characterState.value = _characterState.value?.copy(turns = char.turns + 1)
            saveCurrentTurnState()
            return
        }

        val targetTile = _tiles.value.find { it.x == targetId }
        if (targetTile?.tileType == "WALL") {
            addCombatLog("Ouch! You bumped into a solid dungeon wall.")
            audioEngine.playMove()
            return
        }

        val monster = _monsters.value.find { it.x == targetId }
        if (monster != null) {
            performAttack(char, monster)
            triggerMonsterTurns()
            _characterState.value = _characterState.value?.copy(turns = char.turns + 1)
            saveCurrentTurnState()
            return
        }

        _playerX.value = targetId
        audioEngine.playMove()

        recalculateFogOfWar(targetId)
        triggerMonsterTurns()

        _characterState.value = char.copy(
            playerX = targetId,
            turns = char.turns + 1
        )
        saveCurrentTurnState()
    }

    private fun generateFloor(floorIndex: Int) {
        val nextTiles = mutableMapOf<Int, GameTile>()
        for (id in planetNodes.keys) {
            nextTiles[id] = GameTile(id, 0, "WALL", false)
        }

        val rnd = java.util.Random()
        
        // Use Drunkard's Walk / Flood Fill to carve a cavern network on the sphere
        val startNode = planetNodes.keys.random()
        var current = startNode
        var floorCount = 0
        val targetFloors = 200 // about 30% of the planet is floor
        
        while (floorCount < targetFloors) {
            if (nextTiles[current]?.tileType == "WALL") {
                nextTiles[current] = GameTile(current, 0, "FLOOR", false)
                floorCount++
            }
            if (rnd.nextFloat() < 0.1f) {
                current = startNode // Reset to avoid long singular snake paths
            } else {
                current = planetNodes[current]?.neighbors?.random() ?: startNode
            }
        }

        // Guarantee start node is floor
        val px = startNode
        nextTiles[px] = GameTile(px, 0, "FLOOR", false)

        val possibleFloors = nextTiles.values.filter { it.tileType == "FLOOR" }.map { it.x }

        // Exit staircase placement as far from px as possible
        val sx = possibleFloors.maxByOrNull { fId ->
            val v1 = planetNodes[px]!!.position
            val v2 = planetNodes[fId]!!.position
            (v1 - v2).length()
        } ?: possibleFloors.last()
        
        nextTiles[sx] = GameTile(sx, 0, "STAIRS_DOWN", false)

        // Randomly place 3 treasure chests
        val availableChestSpots = possibleFloors.filter { it != px && it != sx }.shuffled().take(3)
        for (cx in availableChestSpots) {
            nextTiles[cx] = GameTile(cx, 0, "CHEST", false)
        }

        // Place hostiles in random locations
        val floorMonsters = mutableListOf<MonsterState>()
        var globalMonsterIndex = 1
        val numMonsters = 8 + floorIndex * 2

        val availableMonsterSpots = possibleFloors.filter { 
            it != px && it != sx && !availableChestSpots.contains(it) && 
            (planetNodes[px]!!.position - planetNodes[it]!!.position).length() > 0.5f // Ensure distance from origin
        }.shuffled().take(numMonsters)

        for (idx in availableMonsterSpots.indices) {
            val hType = when {
                floorIndex >= 9 && idx == 0 -> "DRAGON"
                floorIndex >= 7 && rnd.nextBoolean() -> "NECROMANCER"
                floorIndex >= 4 && rnd.nextBoolean() -> "GOBLIN"
                else -> "SKELETON"
            }
            
            val hp = when (hType) {
                "DRAGON" -> 160 + floorIndex * 15
                "NECROMANCER" -> 50 + floorIndex * 7
                "GOBLIN" -> 35 + floorIndex * 5
                else -> 24 + floorIndex * 4
            }
            floorMonsters.add(MonsterState(globalMonsterIndex++, hType, hp, hp, availableMonsterSpots[idx], 0))
        }

        _playerX.value = px
        _tiles.value = nextTiles.values.toList()
        _monsters.value = floorMonsters

        recalculateFogOfWar(px)
    }

    private fun recalculateFogOfWar(heroId: Int) {
        val heroPos = planetNodes[heroId]?.position ?: return
        val revealDistance = 0.85f // Spherical distance threshold in 3D Euclidean space
        
        _tiles.value = _tiles.value.map { tile ->
            val tilePos = planetNodes[tile.x]?.position
            if (tilePos != null && (heroPos - tilePos).length() <= revealDistance) {
                tile.copy(revealed = true)
            } else {
                tile
            }
        }
    }

    private fun findNearestRevealedMonster(): MonsterState? {
        val revealedMonsters = _monsters.value.filter { m ->
            _tiles.value.find { it.x == m.x }?.revealed == true
        }

        val pPos = planetNodes[_playerX.value]?.position ?: return null
        return revealedMonsters.minByOrNull { m ->
            val mPos = planetNodes[m.x]?.position ?: return@minByOrNull Float.MAX_VALUE
            (mPos - pPos).length()
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
        dbHelper.saveActiveCharacter(char.copy(playerX = _playerX.value))
        dbHelper.saveMap(_tiles.value)
        dbHelper.saveMonsters(_monsters.value)
        dbHelper.saveInventory(_inventory.value)
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
