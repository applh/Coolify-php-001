package com.example.cameraxapp.blackjack

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class PlayerStats(
    val handsPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val pushes: Int = 0,
    val blackjacks: Int = 0,
    val maxWin: Int = 0,
    val maxStreak: Int = 0,
    val currentStreak: Int = 0
)

data class BlackjackSettings(
    val dealerSoft17Hit: Boolean = true, // true size: Hit soft 17 (Vegas Strip), false: Stand all 17s (Atlantic City)
    val doubleAnyTwo: Boolean = true,    // true: Double on any first 2 cards, false: Double on 9-11 only
    val insuranceAvail: Boolean = true,  // true: Allows buying insurance against dealer ace
    val maxSplits: Int = 3,              // Split limit maximum (e.g. split into up to 4 hands)
    val showStrategyHud: Boolean = true, // Strategy advice overlay visibility
    val showCardCountingHud: Boolean = true, // Card counting statistics HUD visibility
    val tableColor: Int = 0              // 0: Classic Emerald, 1: Burgundy Red, 2: Royal Indigo
)

class BlackjackDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Persistent Player Wallet Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS blackjack_wallet (
                id INTEGER PRIMARY KEY DEFAULT 1,
                balance INTEGER NOT NULL DEFAULT 1000
            )
        """)

        // Cumulative Metrics Tracking Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS blackjack_stats (
                id INTEGER PRIMARY KEY DEFAULT 1,
                hands_played INTEGER NOT NULL DEFAULT 0,
                wins INTEGER NOT NULL DEFAULT 0,
                losses INTEGER NOT NULL DEFAULT 0,
                pushes INTEGER NOT NULL DEFAULT 0,
                blackjacks INTEGER NOT NULL DEFAULT 0,
                max_win INTEGER NOT NULL DEFAULT 0,
                max_streak INTEGER NOT NULL DEFAULT 0,
                current_streak INTEGER NOT NULL DEFAULT 0
            )
        """)

        // Persistent Custom Table Settings Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS blackjack_settings (
                id INTEGER PRIMARY KEY DEFAULT 1,
                dealer_soft_17_hit INTEGER NOT NULL DEFAULT 1,
                double_any_two INTEGER NOT NULL DEFAULT 1,
                insurance_avail INTEGER NOT NULL DEFAULT 1,
                max_splits INTEGER NOT NULL DEFAULT 3,
                show_strategy_hud INTEGER NOT NULL DEFAULT 1,
                show_card_counting_hud INTEGER NOT NULL DEFAULT 1,
                table_color INTEGER NOT NULL DEFAULT 0
            )
        """)

        // Bootstrap initial rows
        db.execSQL("INSERT OR IGNORE INTO blackjack_wallet (id, balance) VALUES (1, 1000)")
        db.execSQL("INSERT OR IGNORE INTO blackjack_stats (id, hands_played, wins, losses, pushes, blackjacks, max_win, max_streak, current_streak) VALUES (1, 0, 0, 0, 0, 0, 0, 0, 0)")
        db.execSQL("INSERT OR IGNORE INTO blackjack_settings (id, dealer_soft_17_hit, double_any_two, insurance_avail, max_splits, show_strategy_hud, show_card_counting_hud, table_color) VALUES (1, 1, 1, 1, 3, 1, 1, 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Upgrade procedures if necessary
    }

    fun getBalance(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT balance FROM blackjack_wallet WHERE id = 1", null)
        var balance = 1000
        if (cursor.moveToFirst()) {
            balance = cursor.getInt(0)
        }
        cursor.close()
        return balance
    }

    fun updateBalance(newBalance: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("balance", newBalance)
        }
        db.update("blackjack_wallet", values, "id = 1", null)
    }

    fun getStats(): PlayerStats {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM blackjack_stats WHERE id = 1", null)
        var stats = PlayerStats()
        if (cursor.moveToFirst()) {
            stats = PlayerStats(
                handsPlayed = cursor.getInt(1),
                wins = cursor.getInt(2),
                losses = cursor.getInt(3),
                pushes = cursor.getInt(4),
                blackjacks = cursor.getInt(5),
                maxWin = cursor.getInt(6),
                maxStreak = cursor.getInt(7),
                currentStreak = cursor.getInt(8)
            )
        }
        cursor.close()
        return stats
    }

    fun recordGameRound(outcome: String, winAmount: Int) {
        val db = writableDatabase
        val stats = getStats()

        val isWin = outcome == "WIN" || outcome == "BLACKJACK"
        val isLoss = outcome == "LOSS" || outcome == "BUST"
        val isPush = outcome == "PUSH"
        val isBJ = outcome == "BLACKJACK"

        val nextStreak = if (isWin) {
            stats.currentStreak + 1
        } else if (isLoss) {
            0
        } else {
            stats.currentStreak // Pushes maintain streak
        }

        val nextMaxStreak = if (nextStreak > stats.maxStreak) nextStreak else stats.maxStreak

        val values = ContentValues().apply {
            put("hands_played", stats.handsPlayed + 1)
            put("wins", stats.wins + if (isWin) 1 else 0)
            put("losses", stats.losses + if (isLoss) 1 else 0)
            put("pushes", stats.pushes + if (isPush) 1 else 0)
            put("blackjacks", stats.blackjacks + if (isBJ) 1 else 0)
            if (winAmount > stats.maxWin) {
                put("max_win", winAmount)
            }
            put("current_streak", nextStreak)
            put("max_streak", nextMaxStreak)
        }
        db.update("blackjack_stats", values, "id = 1", null)
    }

    fun getSettings(): BlackjackSettings {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM blackjack_settings WHERE id = 1", null)
        var settings = BlackjackSettings()
        if (cursor.moveToFirst()) {
            settings = BlackjackSettings(
                dealerSoft17Hit = cursor.getInt(1) == 1,
                doubleAnyTwo = cursor.getInt(2) == 1,
                insuranceAvail = cursor.getInt(3) == 1,
                maxSplits = cursor.getInt(4),
                showStrategyHud = cursor.getInt(5) == 1,
                showCardCountingHud = cursor.getInt(6) == 1,
                tableColor = cursor.getInt(7)
            )
        }
        cursor.close()
        return settings
    }

    fun updateSettings(settings: BlackjackSettings) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("dealer_soft_17_hit", if (settings.dealerSoft17Hit) 1 else 0)
            put("double_any_two", if (settings.doubleAnyTwo) 1 else 0)
            put("insurance_avail", if (settings.insuranceAvail) 1 else 0)
            put("max_splits", settings.maxSplits)
            put("show_strategy_hud", if (settings.showStrategyHud) 1 else 0)
            put("show_card_counting_hud", if (settings.showCardCountingHud) 1 else 0)
            put("table_color", settings.tableColor)
        }
        db.update("blackjack_settings", values, "id = 1", null)
    }

    fun resetStats() {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("hands_played", 0)
            put("wins", 0)
            put("losses", 0)
            put("pushes", 0)
            put("blackjacks", 0)
            put("max_win", 0)
            put("max_streak", 0)
            put("current_streak", 0)
        }
        db.update("blackjack_stats", values, "id = 1", null)
    }

    companion object {
        private const val DATABASE_NAME = "blackjack_hub.db"
        private const val DATABASE_VERSION = 1
    }
}
