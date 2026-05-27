# Android Rogue-like RPG Adventure Game ("RogueCompose") Applet Plan

**Objective**: Extend the Android Multi-App Hub by introducing the **RogueCompose** applet (`RoguelikeScreen.kt`, `RoguelikeViewModel.kt`, and SQLite-based inventory/character telemetry saving engine). This tool implements a highly polished, offline-first turn-based rogue-like RPG adventure game with procedurally generated grid mazes, dynamic turn-based monster combat, multi-class progressions, and persistent equipment storage.

---

## 1. Architectural Highlights & Tech Stack

To ensure instant responsiveness, high-contrast visual grids, and zero-flicker turn transitions, the applet follows Jetpack Compose MVVM architecture bound to Kotlin Coroutines and a localized SQLite database transactional layer.

```
                    ┌────────────────────────┐
                    │   RoguelikeViewModel   │
                    └───────────┬────────────┘
                                │ State Flow (Player details, Active Map, Inventory, Log)
                                ▼
                    ┌────────────────────────┐
                    │   RoguelikeScreen UI   │
                    └───────────┬────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│  Procedural  │        │   Roguelike  │        │ SoundPool    │
│  Dungeon Gen │        │   Database   │        │ Audio Engine │
│ (BSP/Rooms/  │        │   Helper     │        │ (Swords, XP, │
│  Corridors)  │        │ (Saves/Metrics)│     │ Casts, Death)│
└──────────────┘        └──────────────┘        └──────────────┘
```

- **Procedural Dungoneering Engine**: A binary space partitioning (BSP) mapper or randomized room carver that runs server-side or local-side sequentially to generate distinct dungeon grid layouts (walls, corridors, rooms, loot, stairs) for every floor.
- **State Engine**: A structured turn-based state machine (`GameStatus` including `CHARACTER_SELECT`, `EXPLORING`, `COMBAT_LOG`, `INVENTORY_MODAL`, `GAME_OVER`).
- **Database Engine**: A localized SQLite manager registering character states, active floor layouts, gathered gold/inventory bags, and lifetime high-score trackers.
- **Low-Latency SoundPool Audio**: Android system `SoundPool` integration triggering micro-sounds for tile movements, weapon attacks/clangs, chest unlocks, spell-casting, and leveling chimes.

---

## 2. Core Features (The Proposed Scope)

### A. Procedural Tile Dungeon Generation
- **Dynamic Floor Maps**: Every game floor (1 to 20+) is procedurally constructed on an $18 \times 18$ grid. Floors consist of 3–5 rooms connected with carved corridors.
- **Fog of War**: To emphasize exploration, cells start covered in darkness (shrouded fog). Player movement reveals tiles within a 3-tile light radius.
- **Interactive Props**:
  - **Chests**: Contain random loot (Weapons, Armors, Potions) or Mimic monsters.
  - **Stairs Down**: Progresses the player to the next floor, raising difficulty & monster level.
  - **Shrines**: Touch to recover HP/Mana or receive temporary status buffs (e.g., "+3 Attack Strength").

### B. Dynamic Class Selection & RPG Progression Engine
At initial startup, players select from three iconic fantasy archetypes, each modifying core attributes:

| Class | Base HP | Base Mana | Special Trait | Starting Weapon |
| :--- | :--- | :--- | :--- | :--- |
| **Warrior** | 120 | 20 | *Shield Block*: +2 Base Def, High Strength. | Iron Sword |
| **Mage** | 75 | 100 | *Spellweaver*: Ranged Fireballs/Heal Spell. | Apprentice Staff|
| **Rogue** | 90 | 40 | *Critical Strike*: +15% Evasion & Crit Chance. | Poison Dagger |

- **Attributes**: `Strength` (raises physical damage), `Dexterity` (raises Crit/Evasion rates), `Intelligence` (raises magic potency/max mana), and `Vitality` (raises max health).
- **EXP & Levelling**: Killing beasts yields experience points. Leveling up offers choice metrics to allocate $+3$ attribute points, scaling combat readiness.

### C. Turn-Based Combat & Dynamic AI
- **Symmetrical Turns**: Whenever the player moves a tile or uses an item, monsters evaluate their turn.
- **Monster Classes**:
  - **Skeletons / Rats**: Low health, pursues close combat.
  - **Orcs / Goblins**: Intermediate damage, guards treasure hallways.
  - **Necromancers**: Ranged shadow bolt casters.
  - **Shadow Dragon (Boss)**: Guards the ancestral 20th floor with fire-breathing tiles.
- **Tactical Movement**: Players can strike enemies by moving directly *into* their adjacent tile, printing rich damage equations to a scrolling Event HUD.

### D. Inventory Suite & Equipment Management
- **Loot Progression**: Weapons (e.g., Wooden Staff $\rightarrow$ Iron Sword $\rightarrow$ Dragon Blade) and Armors (e.g., Cloth Robes $\rightarrow$ Steel Plate $\rightarrow$ Mythril Guard) provide explicit defensive and offensive stat increments.
- **Quick-Slot Consumables**:
  - **Healing Potions**: Recover raw HP.
  - **Mana Potions**: Refill magical resources.
  - **Scroll of Teleportation**: Instantly escape dangerous mobs to a random safe floor tile.
  - **Scroll of Fireball**: Deal area-of-effect damage to all adjacent hostiles.

### E. Elegant Dark-Fantasy Retro Layout & UX
- **The Obsidian Dungeon Theme**: A gorgeous, eye-friendly layout built on deep charcoal backdrops and glowing crimson/antique-gold borders. Matches retro ASCII themes but utilizes styled graphic tokens or custom Unicode emoji markers (🗡️, 🛡️, 🧪, 🧙, 💀, 🐉).
- **Haptic Directional Pad**: Floating elevated direction buttons at the footer for effortless one-handed grid navigation. Features micro-vibrations upon walking or striking hostiles.
- **Victory & Death Float Toasts**: Golden level-up halos expand on player level-ups, while tragic crimson screen-fades and scrolling obituaries outline player defeats.
- **Resilient Mobile Auto-Save**: Since mobile apps get closed frequently, the SQLite DB commits the complete map grid, chest states, player coordinate, and attributes on *every single turn*. Resuming the game loads the user exactly where they stepped.

---

## 3. Recommended Professional Features

### 1. Lifetime Hero Pantheon & Run Telemetry
- **Leaderboard SQLite Record**: Captures run histories: Character Class, deep floor reached, monster death cause, total turns survived, and inventory collection value.
- **Achievements Panel**: Unlock unique in-app medals (e.g., *Dragon Slayer*, *Pacifist Speedrunner*, *Potion Hoarder*) to reward different styles of playthroughs.

### 2. Tactical Magic & Skill Mechanics
- Special skill triggers based of selected classes:
  - **Warrior's Cleave**: Attacks all three adjacent horizontal tiles for $1.5\times$ damage (uses 10 Mana).
  - **Rogue's Stealth**: Turn invisible to hostile mobs for 5 turns (uses 15 Mana).
  - **Mage's Blizzard**: Freezes all monsters on the visible screen for 2 turns (uses 25 Mana).

### 3. Dynamic Merchant Shrines (Gold Economy)
- Spend collected dungeon coins at random Floor Merchants. Purchase potions, scrolls, or weapon reinforcement gems (`Weapon Level +1`) to survive deeper levels.

---

## 4. UI / UX Layout & Composition

The multi-app layout utilizes portrait elements optimized for flexible touch-targets.

```
┌────────────────────────────────────────────────────────┐
│ 🗡️ ROGUECOMPOSE       [Floor: 4]         [Gold: 140g]  │
├────────────────────────────────────────────────────────┤
│  Hero Info: Level 3 Warrior   [HP: 85/120]  [Mana: 8/20] │
│  Exp Bar: [=============>------] 72%                   │
├────────────────────────────────────────────────────────┤
│  Procedural Grid Stage (Fog of war active):            │
│  ▓  ▓  ▓  ▓  ▓  ▓  ▓  ▓  ▓  ▓                          │
│  ▓  .  .  .  ▓  .  💀 .  .  ▓                          │
│  ▓  .  🧙  .  ▓  .  .  🧪 .  ▓        ◄-- Tile Matrix   │
│  ▓  .  .  .  .  .  .  .  .  ▓                          │
│  ▓  ▓  ▓  ▓  ▓  ▓  ▓  ▓  ▓  ▓                          │
├────────────────────────────────────────────────────────┤
│  Scrolling Combat Logs:                                │
│  > You moved East.                                     │
│  > Skeleton misses you!                                │
│  > Strike! You dealt 14 dmg to Skeleton (Dead 💀).      │
├────────────────────────────────────────────────────────┤
│  Quick Slots:  [🧪 Potion]  [📜 Fireball]  [🎒 Bag]  │
├────────────────────────────────────────────────────────┤
│               Navigation Controller D-Pad:             │
│                        [▲ Up]                          │
│               [◀ Left] [● Wait] [▶ Right]              │
│                        [▼ Down]                        │
└────────────────────────────────────────────────────────┘
```

---

## 5. SQLite Storage Schemas & Models

`RoguelikeDatabaseHelper.kt` will handle real-time turn recovery structures to ensure bulletproof session preservation:

```sql
-- Track current alive adventure details
CREATE TABLE IF NOT EXISTS active_character (
    id INTEGER PRIMARY KEY DEFAULT 1,
    class TEXT NOT NULL,
    level INTEGER NOT NULL DEFAULT 1,
    current_hp INTEGER NOT NULL,
    max_hp INTEGER NOT NULL,
    current_mana INTEGER NOT NULL,
    max_mana INTEGER NOT NULL,
    exp INTEGER NOT NULL DEFAULT 0,
    gold INTEGER NOT NULL DEFAULT 0,
    strength INTEGER NOT NULL,
    dexterity INTEGER NOT NULL,
    intelligence INTEGER NOT NULL,
    vitality INTEGER NOT NULL,
    floor INTEGER NOT NULL DEFAULT 1,
    player_x INTEGER NOT NULL DEFAULT 2,
    player_y INTEGER NOT NULL DEFAULT 2
);

-- Active Floor Tile state persistence
CREATE TABLE IF NOT EXISTS active_floor_map (
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    tile_type TEXT NOT NULL,       -- WALL, FLOOR, STAIRS_DOWN, CHEST
    revealed INTEGER NOT NULL DEFAULT 0, -- 1=Visible, 0=Fog of War
    PRIMARY KEY (x, y)
);

-- Active Floor Hostiles placement
CREATE TABLE IF NOT EXISTS floor_monsters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    monster_type TEXT NOT NULL,    -- SKELETON, GOBLIN, NECROMANCER, DRAGON
    current_hp INTEGER NOT NULL,
    max_hp INTEGER NOT NULL,
    x INTEGER NOT NULL,
    y INTEGER NOT NULL
);

-- Backpack Inventory storage
CREATE TABLE IF NOT EXISTS inventory_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_name TEXT NOT NULL,
    item_type TEXT NOT NULL,       -- WEAPON, ARMOR, CONSUMABLE
    stat_mod INTEGER NOT NULL DEFAULT 0,
    is_equipped INTEGER NOT NULL DEFAULT 0 -- 1=True, 0=False
);
```

---

## 6. Symmetrical Turn Turn-Based Logic Core

Within `RoguelikeViewModel.kt`, player motion is strictly validated against the layout matrix, immediately prompting standard mob updates:

```kotlin
fun executePlayerTurn(action: PlayerAction) {
    if (currentGameStatus != GameStatus.EXPLORING) return

    val nextX = playerX + action.dx
    val nextY = playerY + action.dy

    // 1. Map/Boundary and Collide-Strike checking
    if (isWall(nextX, nextY)) {
        logCombatMessage("Ouch! You bumped into a solid stone wall.")
        return
    }

    val hostile = getMonsterAt(nextX, nextY)
    if (hostile != null) {
        // Attack sequence
        performWeaponAttack(hostile)
        triggerSystemHapticFeedback()
        checkMonsterDeathState(hostile)
    } else {
        // Standard movement
        movePlayerToCoordinates(nextX, nextY)
        revealFogOfWar(nextX, nextY)
        triggerWalkingSoundEffect()
        checkFloorProps(nextX, nextY)
    }

    // 2. Hostile Symmetrical Counter-Turn
    if (getAliveMonsters().isNotEmpty()) {
        executeMonsterTurns()
    }

    // 3. Persistent SQLite Commit
    saveCurrentTurnStateToDatabase()
    
    // 4. Game Over Checks
    evaluatePlayerSurvival()
}
```

---

## 7. Rollout Phases

1. **Phase 1: Domain Mapping & Generation Mechanics**
   - Implement `Tile` configurations, inventory attributes, and randomized carving algorithm models.
   - Run local validation sequences to ensure hallway lines successfully bridge procedural rooms.

2. **Phase 2: Bulletproof Turn-Based Auto-Save DB Schemas**
   - Construct SQLite drivers writing full character layouts, current room variables, inventory vectors, and map states.
   - Verify that booting the app immediately resumes map progression with identical player states.

3. **Phase 3: Classic Retro Felt & D-Pad Canvas Layout**
   - Design the modern high-contrast Obsidian dark theme UI with prominent floating direction controls.
   - Attach scrolling event monitors logging clean tactical notifications.

4. **Phase 4: Low-Latency SoundPool & Class Skills Expansion**
   - Connect localized SoundPool engines triggering tactile movement clicks, magic bursts, and steel combat clashes.
   - Enable special Warrior, Rogue, and Mage class skills with dynamic spell-ranges.

---

## 8. Questions and Suggestions for the User

Thank you for commissioning this epic RPG project! To tailor the adventure applet perfectly, I would love your thoughts on the following directions:

1. **Visual Style**: Do you prefer a **strictly classic fantasy emoji representation** (using robust icons for players, potions, dragons, and chests) or should we render a **custom 2D pixel grid or Canvas**?
2. **Audio Mood**: What kind of retro music layer would you like? We can construct a togglable chiptune synthesizer loop, or stick to low-latency classic arcade combat sounds.
3. **Save Game Count**: Do you want a single hardcore auto-saved slot (perpetual Permadeath - run deletes on death) or multiple save slots so you can explore custom concurrent classes?
