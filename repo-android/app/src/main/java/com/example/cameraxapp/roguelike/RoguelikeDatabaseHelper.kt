package com.example.cameraxapp.roguelike

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class CharacterState(
    val heroClass: String,
    val level: Int = 1,
    val currentHp: Int,
    val maxHp: Int,
    val currentMana: Int,
    val maxMana: Int,
    val exp: Int = 0,
    val gold: Int = 0,
    val strength: Int,
    val dexterity: Int,
    val intelligence: Int,
    val vitality: Int,
    val floor: Int = 1,
    val playerX: Int = 2,
    val playerY: Int = 2,
    val turns: Int = 0
)

data class GameTile(
    val x: Int,
    val y: Int,
    val tileType: String, // WALL, FLOOR, STAIRS_DOWN, CHEST
    val revealed: Boolean
)

data class MonsterState(
    val id: Int = 0,
    val type: String, // SKELETON, GOBLIN, NECROMANCER, DRAGON
    val currentHp: Int,
    val maxHp: Int,
    val x: Int,
    val y: Int
)

data class InventoryItem(
    val id: Int = (1..100000000).random(),
    val name: String,
    val type: String, // WEAPON, ARMOR, CONSUMABLE
    val statMod: Int = 0,
    val isEquipped: Boolean = false
)

data class HighScore(
    val id: Int = 0,
    val heroClass: String,
    val level: Int,
    val floor: Int,
    val gold: Int,
    val turns: Int,
    val survived: Boolean,
    val timestamp: String = ""
)

class RoguelikeDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
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
                player_y INTEGER NOT NULL DEFAULT 2,
                turns INTEGER NOT NULL DEFAULT 0
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS active_floor_map (
                x INTEGER NOT NULL,
                y INTEGER NOT NULL,
                tile_type TEXT NOT NULL,
                revealed INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (x, y)
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS floor_monsters (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                monster_type TEXT NOT NULL,
                current_hp INTEGER NOT NULL,
                max_hp INTEGER NOT NULL,
                x INTEGER NOT NULL,
                y INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS inventory_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_name TEXT NOT NULL,
                item_type TEXT NOT NULL,
                stat_mod INTEGER NOT NULL DEFAULT 0,
                is_equipped INTEGER NOT NULL DEFAULT 0
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS roguelike_highscores (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                hero_class TEXT NOT NULL,
                level INTEGER NOT NULL,
                floor INTEGER NOT NULL,
                gold INTEGER NOT NULL,
                turns INTEGER NOT NULL,
                survived INTEGER NOT NULL DEFAULT 0,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS active_character")
        db.execSQL("DROP TABLE IF EXISTS active_floor_map")
        db.execSQL("DROP TABLE IF EXISTS floor_monsters")
        db.execSQL("DROP TABLE IF EXISTS inventory_items")
        db.execSQL("DROP TABLE IF EXISTS roguelike_highscores")
        onCreate(db)
    }

    // --- Active Character Storage ---
    fun saveActiveCharacter(charState: CharacterState) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM active_character")
            val values = ContentValues().apply {
                put("id", 1)
                put("class", charState.heroClass)
                put("level", charState.level)
                put("current_hp", charState.currentHp)
                put("max_hp", charState.maxHp)
                put("current_mana", charState.currentMana)
                put("max_mana", charState.maxMana)
                put("exp", charState.exp)
                put("gold", charState.gold)
                put("strength", charState.strength)
                put("dexterity", charState.dexterity)
                put("intelligence", charState.intelligence)
                put("vitality", charState.vitality)
                put("floor", charState.floor)
                put("player_x", charState.playerX)
                put("player_y", charState.playerY)
                put("turns", charState.turns)
            }
            db.insertWithOnConflict("active_character", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadActiveCharacter(): CharacterState? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT class, level, current_hp, max_hp, current_mana, max_mana, exp, gold, strength, dexterity, intelligence, vitality, floor, player_x, player_y, turns FROM active_character LIMIT 1", null)
        var state: CharacterState? = null
        if (cursor.moveToFirst()) {
            state = CharacterState(
                heroClass = cursor.getString(0),
                level = cursor.getInt(1),
                currentHp = cursor.getInt(2),
                maxHp = cursor.getInt(3),
                currentMana = cursor.getInt(4),
                maxMana = cursor.getInt(5),
                exp = cursor.getInt(6),
                gold = cursor.getInt(7),
                strength = cursor.getInt(8),
                dexterity = cursor.getInt(9),
                intelligence = cursor.getInt(10),
                vitality = cursor.getInt(11),
                floor = cursor.getInt(12),
                playerX = cursor.getInt(13),
                playerY = cursor.getInt(14),
                turns = cursor.getInt(15)
            )
        }
        cursor.close()
        return state
    }

    // --- Active Floor Map Storage ---
    fun saveMap(tiles: List<GameTile>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM active_floor_map")
            for (tile in tiles) {
                val cv = ContentValues().apply {
                    put("x", tile.x)
                    put("y", tile.y)
                    put("tile_type", tile.tileType)
                    put("revealed", if (tile.revealed) 1 else 0)
                }
                db.insert("active_floor_map", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadMap(): List<GameTile> {
        val list = mutableListOf<GameTile>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT x, y, tile_type, revealed FROM active_floor_map", null)
        while (cursor.moveToNext()) {
            list.add(
                GameTile(
                    x = cursor.getInt(0),
                    y = cursor.getInt(1),
                    tileType = cursor.getString(2),
                    revealed = cursor.getInt(3) == 1
                )
            )
        }
        cursor.close()
        return list
    }

    // --- Active Monsters ---
    fun saveMonsters(monsters: List<MonsterState>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM floor_monsters")
            for (monster in monsters) {
                val cv = ContentValues().apply {
                    put("monster_type", monster.type)
                    put("current_hp", monster.currentHp)
                    put("max_hp", monster.maxHp)
                    put("x", monster.x)
                    put("y", monster.y)
                }
                db.insert("floor_monsters", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadMonsters(): List<MonsterState> {
        val list = mutableListOf<MonsterState>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, monster_type, current_hp, max_hp, x, y FROM floor_monsters", null)
        while (cursor.moveToNext()) {
            list.add(
                MonsterState(
                    id = cursor.getInt(0),
                    type = cursor.getString(1),
                    currentHp = cursor.getInt(2),
                    maxHp = cursor.getInt(3),
                    x = cursor.getInt(4),
                    y = cursor.getInt(5)
                )
            )
        }
        cursor.close()
        return list
    }

    // --- Inventory ---
    fun saveInventory(items: List<InventoryItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM inventory_items")
            for (item in items) {
                val cv = ContentValues().apply {
                    put("item_name", item.name)
                    put("item_type", item.type)
                    put("stat_mod", item.statMod)
                    put("is_equipped", if (item.isEquipped) 1 else 0)
                }
                db.insert("inventory_items", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadInventory(): List<InventoryItem> {
        val list = mutableListOf<InventoryItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, item_name, item_type, stat_mod, is_equipped FROM inventory_items", null)
        while (cursor.moveToNext()) {
            list.add(
                InventoryItem(
                    id = cursor.getInt(0),
                    name = cursor.getString(1),
                    type = cursor.getString(2),
                    statMod = cursor.getInt(3),
                    isEquipped = cursor.getInt(4) == 1
                )
            )
        }
        cursor.close()
        return list
    }

    // --- Highscores ---
    fun saveHighScore(score: HighScore) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("hero_class", score.heroClass)
            put("level", score.level)
            put("floor", score.floor)
            put("gold", score.gold)
            put("turns", score.turns)
            put("survived", if (score.survived) 1 else 0)
        }
        db.insert("roguelike_highscores", null, cv)
    }

    fun getHighScores(): List<HighScore> {
        val list = mutableListOf<HighScore>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, hero_class, level, floor, gold, turns, survived, timestamp FROM roguelike_highscores ORDER BY floor DESC, level DESC, gold DESC LIMIT 15", null)
        while (cursor.moveToNext()) {
            list.add(
                HighScore(
                    id = cursor.getInt(0),
                    heroClass = cursor.getString(1),
                    level = cursor.getInt(2),
                    floor = cursor.getInt(3),
                    gold = cursor.getInt(4),
                    turns = cursor.getInt(5),
                    survived = cursor.getInt(6) == 1,
                    timestamp = cursor.getString(7) ?: ""
                )
            )
        }
        cursor.close()
        return list
    }

    fun resetHighScores() {
        val db = writableDatabase
        db.execSQL("DELETE FROM roguelike_highscores")
    }

    // --- Permadeath Reset ---
    fun clearAllActiveRun() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM active_character")
            db.execSQL("DELETE FROM active_floor_map")
            db.execSQL("DELETE FROM floor_monsters")
            db.execSQL("DELETE FROM inventory_items")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private const val DATABASE_NAME = "roguelike_adventure.db"
        private const val DATABASE_VERSION = 1
    }
}
