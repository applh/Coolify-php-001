package com.example.cameraxapp.browser

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class BookmarkInfo(
    val id: Int,
    val title: String,
    val url: String,
    val createdAt: Long
)

data class HistoryInfo(
    val id: Int,
    val title: String,
    val url: String,
    val lastVisited: Long,
    val visitCount: Int
)

data class UserscriptInfo(
    val id: Int,
    val name: String,
    val targetRegex: String,
    val jsContent: String,
    val isActive: Boolean,
    val createdAt: Long
)

data class TabState(
    val tabUuid: String,
    val title: String,
    val currentUrl: String,
    val historyStackJson: String,
    val scrollX: Int,
    val scrollY: Int,
    val zoomLevel: Float,
    val lastActive: Long
)

class BrowserDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "browser_hub.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_BOOKMARKS = "bookmarks"
        const val TABLE_HISTORY = "history"
        const val TABLE_USERSCRIPTS = "userscripts"
        const val TABLE_TABS = "active_tabs"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_BOOKMARKS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                url TEXT NOT NULL UNIQUE,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_HISTORY (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                url TEXT NOT NULL,
                last_visited INTEGER NOT NULL,
                visit_count INTEGER DEFAULT 1
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_USERSCRIPTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                target_regex TEXT NOT NULL,
                js_content TEXT NOT NULL,
                is_active INTEGER DEFAULT 1,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_TABS (
                tab_uuid TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                current_url TEXT NOT NULL,
                history_stack_json TEXT NOT NULL,
                scroll_x INTEGER DEFAULT 0,
                scroll_y INTEGER DEFAULT 0,
                zoom_level REAL DEFAULT 1.0,
                last_active INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Simple upgrade strategy for non-critical developer applet
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKMARKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERSCRIPTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TABS")
        onCreate(db)
    }

    // --- Bookmarks Helpers ---
    fun addBookmark(title: String, url: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", title)
            put("url", url)
            put("created_at", System.currentTimeMillis())
        }
        val id = db.insertWithOnConflict(TABLE_BOOKMARKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return id != -1L
    }

    fun deleteBookmark(id: Int): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_BOOKMARKS, "id = ?", arrayOf(id.toString())) > 0
    }

    fun getAllBookmarks(): List<BookmarkInfo> {
        val list = mutableListOf<BookmarkInfo>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_BOOKMARKS ORDER BY created_at DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    BookmarkInfo(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        url = cursor.getString(cursor.getColumnIndexOrThrow("url")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // --- History Helpers ---
    fun recordVisit(title: String, url: String) {
        val db = writableDatabase
        // Check if existing
        val cursor = db.rawQuery("SELECT id, visit_count FROM $TABLE_HISTORY WHERE url = ?", arrayOf(url))
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val count = cursor.getInt(cursor.getColumnIndexOrThrow("visit_count"))
            val values = ContentValues().apply {
                put("title", title)
                put("last_visited", System.currentTimeMillis())
                put("visit_count", count + 1)
            }
            db.update(TABLE_HISTORY, values, "id = ?", arrayOf(id.toString()))
        } else {
            val values = ContentValues().apply {
                put("title", title)
                put("url", url)
                put("last_visited", System.currentTimeMillis())
                put("visit_count", 1)
            }
            db.insert(TABLE_HISTORY, null, values)
        }
        cursor.close()
    }

    fun clearHistory() {
        writableDatabase.execSQL("DELETE FROM $TABLE_HISTORY")
    }

    fun getAllHistory(): List<HistoryInfo> {
        val list = mutableListOf<HistoryInfo>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_HISTORY ORDER BY last_visited DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    HistoryInfo(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        url = cursor.getString(cursor.getColumnIndexOrThrow("url")),
                        lastVisited = cursor.getLong(cursor.getColumnIndexOrThrow("last_visited")),
                        visitCount = cursor.getInt(cursor.getColumnIndexOrThrow("visit_count"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // --- Userscripts Helpers ---
    fun saveUserscript(name: String, regex: String, content: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("target_regex", regex)
            put("js_content", content)
            put("is_active", 1)
            put("created_at", System.currentTimeMillis())
        }
        val id = db.insert(TABLE_USERSCRIPTS, null, values)
        return id != -1L
    }

    fun deleteUserscript(id: Int): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_USERSCRIPTS, "id = ?", arrayOf(id.toString())) > 0
    }

    fun toggleUserscript(id: Int, active: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("is_active", if (active) 1 else 0)
        }
        db.update(TABLE_USERSCRIPTS, values, "id = ?", arrayOf(id.toString()))
    }

    fun getAllUserscripts(): List<UserscriptInfo> {
        val list = mutableListOf<UserscriptInfo>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERSCRIPTS ORDER BY created_at DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    UserscriptInfo(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        targetRegex = cursor.getString(cursor.getColumnIndexOrThrow("target_regex")),
                        jsContent = cursor.getString(cursor.getColumnIndexOrThrow("js_content")),
                        isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1,
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getScriptsForUrl(url: String): List<UserscriptInfo> {
        val db = readableDatabase
        val allScripts = getAllUserscripts().filter { it.isActive }
        return allScripts.filter { script ->
            try {
                val pattern = script.targetRegex.toRegex(RegexOption.IGNORE_CASE)
                pattern.containsMatchIn(url)
            } catch (e: Exception) {
                // If it's a simple wildcard/glob, convert to basic contains or regex pattern
                val simpleRegex = script.targetRegex
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .toRegex(RegexOption.IGNORE_CASE)
                simpleRegex.containsMatchIn(url)
            }
        }
    }

    // --- Tab persistence Helpers ---
    fun saveTabState(tab: TabState) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("tab_uuid", tab.tabUuid)
            put("title", tab.title)
            put("current_url", tab.currentUrl)
            put("history_stack_json", tab.historyStackJson)
            put("scroll_x", tab.scrollX)
            put("scroll_y", tab.scrollY)
            put("zoom_level", tab.zoomLevel)
            put("last_active", tab.lastActive)
        }
        db.insertWithOnConflict(TABLE_TABS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteTabState(tabUuid: String) {
        writableDatabase.delete(TABLE_TABS, "tab_uuid = ?", arrayOf(tabUuid))
    }

    fun getAllTabs(): List<TabState> {
        val list = mutableListOf<TabState>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TABS ORDER BY last_active DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    TabState(
                        tabUuid = cursor.getString(cursor.getColumnIndexOrThrow("tab_uuid")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        currentUrl = cursor.getString(cursor.getColumnIndexOrThrow("current_url")),
                        historyStackJson = cursor.getString(cursor.getColumnIndexOrThrow("history_stack_json")),
                        scrollX = cursor.getInt(cursor.getColumnIndexOrThrow("scroll_x")),
                        scrollY = cursor.getInt(cursor.getColumnIndexOrThrow("scroll_y")),
                        zoomLevel = cursor.getFloat(cursor.getColumnIndexOrThrow("zoom_level")),
                        lastActive = cursor.getLong(cursor.getColumnIndexOrThrow("last_active"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}
