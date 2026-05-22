package com.example.cameraxapp

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ==========================================
// MODELS & DATA STRUCTS
// ==========================================

data class DbFileItem(
    val file: File,
    val name: String,
    val size: Long,
    val path: String
)

data class DbColumnInfo(
    val name: String,
    val type: String,
    val isNullable: Boolean,
    val isPrimaryKey: Boolean,
    val defaultValue: String?
)

data class SqlResult(
    val columns: List<String>,
    val rows: List<List<String?>>,
    val message: String = "",
    val executionTimeMs: Long = 0,
    val success: Boolean = true
)

// ==========================================
// DB VIEWMODEL
// ==========================================

class DBViewModel : ViewModel() {

    private val _dbFiles = MutableStateFlow<List<DbFileItem>>(emptyList())
    val dbFiles: StateFlow<List<DbFileItem>> = _dbFiles.asStateFlow()

    private val _selectedDb = MutableStateFlow<DbFileItem?>(null)
    val selectedDb: StateFlow<DbFileItem?> = _selectedDb.asStateFlow()

    private val _tables = MutableStateFlow<List<String>>(emptyList())
    val tables: StateFlow<List<String>> = _tables.asStateFlow()

    private val _selectedTable = MutableStateFlow<String?>(null)
    val selectedTable: StateFlow<String?> = _selectedTable.asStateFlow()

    private val _columns = MutableStateFlow<List<DbColumnInfo>>(emptyList())
    val columns: StateFlow<List<DbColumnInfo>> = _columns.asStateFlow()

    // Row data paginated
    private val _tableRows = MutableStateFlow<List<Map<String, String?>>>(emptyList())
    val tableRows: StateFlow<List<Map<String, String?>>> = _tableRows.asStateFlow()

    private val _currentRowIds = MutableStateFlow<List<Long?>>(emptyList())
    val currentRowIds: StateFlow<List<Long?>> = _currentRowIds.asStateFlow()

    private val _totalRowsCount = MutableStateFlow(0)
    val totalRowsCount: StateFlow<Int> = _totalRowsCount.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _searchFilter = MutableStateFlow("")
    val searchFilter: StateFlow<String> = _searchFilter.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private val _infoState = MutableStateFlow<String?>(null)
    val infoState: StateFlow<String?> = _infoState.asStateFlow()

    // SQL Runner state
    private val _terminalResult = MutableStateFlow<SqlResult?>(null)
    val terminalResult: StateFlow<SqlResult?> = _terminalResult.asStateFlow()

    private val _terminalInput = MutableStateFlow("SELECT * FROM sqlite_master;")
    val terminalInput: StateFlow<String> = _terminalInput.asStateFlow()

    private var currentOpenDb: SQLiteDatabase? = null
    private val PAGE_SIZE = 25

    fun clearError() { _errorState.value = null }
    fun clearInfo() { _infoState.value = null }
    fun setTerminalInput(input: String) { _terminalInput.value = input }
    fun setSearchFilter(filter: String) { 
        _searchFilter.value = filter
        _currentPage.value = 0
        loadCurrentTableData()
    }

    /**
     * Scans local context databases directory. Auto-provisions a demo database structure if empty.
     */
    fun scanDatabases(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbFolder = context.getDatabasePath("test").parentFile
                if (dbFolder != null && !dbFolder.exists()) {
                    dbFolder.mkdirs()
                }

                val filesInDbFolder = dbFolder?.listFiles()?.toList() ?: emptyList()
                val filesInFilesFolder = context.filesDir.listFiles()?.toList() ?: emptyList()

                // filter valid SQLite extensions (.db, .sqlite, .sqlite3)
                val allFiles = (filesInDbFolder + filesInFilesFolder)
                    .filter { it.isFile && (it.extension in listOf("db", "sqlite", "sqlite3") || it.name == "app_internal.db") }
                    .distinctBy { it.absolutePath }
                    .map { File ->
                        DbFileItem(
                            file = File,
                            name = File.name,
                            size = File.length(),
                            path = File.absolutePath
                        )
                    }

                _dbFiles.value = allFiles

                // Auto-create a demo SQLite DB if none exist
                if (allFiles.isEmpty()) {
                    createDemoDatabase(context)
                }
            } catch (e: Exception) {
                _errorState.value = "Failed to scan databases: ${e.localizedMessage}"
            }
        }
    }

    private fun createDemoDatabase(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath("shop_warehouse.db")
                dbFile.parentFile?.mkdirs()
                val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)

                // Provision table 1: products
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS products (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        category TEXT DEFAULT 'General',
                        price REAL,
                        stock INTEGER,
                        sku TEXT UNIQUE
                    );
                """.trimIndent())

                // Insert seed rows if empty
                val checkCursor = db.rawQuery("SELECT count(*) FROM products", null)
                var count = 0
                if (checkCursor.moveToFirst()) {
                    count = checkCursor.getInt(0)
                }
                checkCursor.close()

                if (count == 0) {
                    db.execSQL("INSERT INTO products (name, category, price, stock, sku) VALUES ('Wireless Earbuds', 'Electronics', 49.99, 120, 'SKU-EAR-09');")
                    db.execSQL("INSERT INTO products (name, category, price, stock, sku) VALUES ('Ergonomic Mouse', 'Electronics', 25.40, 45, 'SKU-MOU-41');")
                    db.execSQL("INSERT INTO products (name, category, price, stock, sku) VALUES ('Classic Notebook', 'Office', 4.99, 900, 'SKU-NOT-52');")
                    db.execSQL("INSERT INTO products (name, category, price, stock, sku) VALUES ('Steel Thermos 1L', 'Outdoor', 18.25, 60, 'SKU-THM-12');")
                    db.execSQL("INSERT INTO products (name, category, price, stock, sku) VALUES ('USB-C Multi Hub', 'Electronics', 35.00, 15, 'SKU-HUB-91');")
                }

                // Provision table 2: log_entries
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS log_entries (
                        log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        level TEXT,
                        message TEXT,
                        timestamp TEXT
                    );
                """.trimIndent())

                val checkLogCursor = db.rawQuery("SELECT count(*) FROM log_entries", null)
                var logCount = 0
                if (checkLogCursor.moveToFirst()) {
                    logCount = checkLogCursor.getInt(0)
                }
                checkLogCursor.close()

                if (logCount == 0) {
                    db.execSQL("INSERT INTO log_entries (level, message, timestamp) VALUES ('INFO', 'Application started up', '2026-05-21 14:10:00');")
                    db.execSQL("INSERT INTO log_entries (level, message, timestamp) VALUES ('WARNING', 'High database queue size', '2026-05-21 14:12:15');")
                    db.execSQL("INSERT INTO log_entries (level, message, timestamp) VALUES ('ERROR', 'Failed network broadcast', '2026-05-21 14:14:02');")
                }

                db.close()
                scanDatabases(context)
            } catch (e: Exception) {
                _errorState.value = "Failed to provision demo database: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Create an empty new SQlite DB files
     */
    fun createEmptyDatabase(context: Context, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var cleanName = name.trim()
                if (!cleanName.endsWith(".db") && !cleanName.endsWith(".sqlite")) {
                    cleanName += ".db"
                }
                val dbFile = context.getDatabasePath(cleanName)
                dbFile.parentFile?.mkdirs()
                val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
                db.close()
                _infoState.value = "Database '$cleanName' created successfully."
                scanDatabases(context)
            } catch (e: Exception) {
                _errorState.value = "Create failed: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Delete entire database file
     */
    fun deleteDatabaseFile(context: Context, item: DbFileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (currentOpenDb != null && _selectedDb.value?.path == item.path) {
                    closeDatabase()
                }
                val deleted = item.file.delete()
                val journal = File(item.path + "-journal")
                if (journal.exists()) journal.delete()

                if (deleted) {
                    _infoState.value = "Database file deleted safely."
                } else {
                    _errorState.value = "Failed to delete file."
                }
                scanDatabases(context)
            } catch (e: Exception) {
                _errorState.value = "Delete database file error: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Opens a specific SQLite DB file and initializes metadata lists
     */
    fun selectDatabase(item: DbFileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                closeDatabase()
                currentOpenDb = SQLiteDatabase.openDatabase(item.path, null, SQLiteDatabase.OPEN_READWRITE)
                _selectedDb.value = item
                _selectedTable.value = null
                _columns.value = emptyList()
                _tableRows.value = emptyList()
                _totalRowsCount.value = 0
                _currentPage.value = 0
                _terminalResult.value = null
                
                loadTablesList()
            } catch (e: Exception) {
                try {
                    // fallback to read only if write permission issues are present
                    currentOpenDb = SQLiteDatabase.openDatabase(item.path, null, SQLiteDatabase.OPEN_READONLY)
                    _selectedDb.value = item
                    _selectedTable.value = null
                    _columns.value = emptyList()
                    _tableRows.value = emptyList()
                    _totalRowsCount.value = 0
                    _currentPage.value = 0
                    _terminalResult.value = null
                    loadTablesList()
                    _infoState.value = "Connected in Read-Only mode."
                } catch (eFallback: Exception) {
                    _errorState.value = "Failed to connect: ${eFallback.localizedMessage}"
                }
            }
        }
    }

    fun closeDatabase() {
        currentOpenDb?.let {
            if (it.isOpen) {
                it.close()
            }
        }
        currentOpenDb = null
        _selectedDb.value = null
        _tables.value = emptyList()
        _selectedTable.value = null
        _columns.value = emptyList()
        _tableRows.value = emptyList()
        _totalRowsCount.value = 0
    }

    private fun loadTablesList() {
        val db = currentOpenDb ?: return
        try {
            val list = mutableListOf<String>()
            val query = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_metadata' AND name NOT LIKE 'sqlite_sequence' ORDER BY name ASC;"
            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                do {
                    list.add(cursor.getString(0))
                } while (cursor.moveToNext())
            }
            cursor.close()
            _tables.value = list
        } catch (e: Exception) {
            _errorState.value = "Failed to extract tables list: ${e.localizedMessage}"
        }
    }

    /**
     * Select a table to view detail rows & schema definition
     */
    fun selectTable(tableName: String) {
        _selectedTable.value = tableName
        _currentPage.value = 0
        _searchFilter.value = ""
        viewModelScope.launch(Dispatchers.IO) {
            loadTableSchema(tableName)
            loadCurrentTableData()
        }
    }

    private fun loadTableSchema(tableName: String) {
        val db = currentOpenDb ?: return
        try {
            val list = mutableListOf<DbColumnInfo>()
            val cursor = db.rawQuery("PRAGMA table_info(\"$tableName\");", null)
            if (cursor.moveToFirst()) {
                do {
                    list.add(
                        DbColumnInfo(
                            name = cursor.getString(1),
                            type = cursor.getString(2),
                            isNullable = cursor.getInt(3) == 0,
                            isPrimaryKey = cursor.getInt(5) == 1,
                            defaultValue = cursor.getString(4)
                        )
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
            _columns.value = list
        } catch (e: Exception) {
            _errorState.value = "Failed schema load: ${e.localizedMessage}"
        }
    }

    /**
     * Loads current selections row count and data cursor paginated
     */
    fun loadCurrentTableData() {
        val db = currentOpenDb ?: return
        val tableName = _selectedTable.value ?: return
        val cols = _columns.value
        if (cols.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Determine rowcount filter
                val filter = _searchFilter.value.trim()
                val whereClause = if (filter.isNotEmpty()) {
                    val conditions = cols.map { "\"${it.name}\" LIKE ?" }.joinToString(" OR ")
                    "WHERE $conditions"
                } else ""

                val selectionArgs = if (filter.isNotEmpty()) {
                    Array(cols.size) { "%$filter%" }
                } else null

                // 1. Row count query
                val countCursor = db.rawQuery("SELECT count(*) FROM \"$tableName\" $whereClause", selectionArgs)
                var count = 0
                if (countCursor.moveToFirst()) {
                    count = countCursor.getInt(0)
                }
                countCursor.close()
                _totalRowsCount.value = count

                // 2. Fetch rows with rowid as explicit custom fallback helper
                // Check if table supports rowid query
                var hasRowId = true
                var queryStr = ""
                try {
                    val checkRowId = db.rawQuery("SELECT rowid FROM \"$tableName\" LIMIT 1", null)
                    checkRowId.close()
                    queryStr = "SELECT rowid AS _custom_row_id_, * FROM \"$tableName\" $whereClause LIMIT $PAGE_SIZE OFFSET ${_currentPage.value * PAGE_SIZE}"
                } catch (e: Exception) {
                    hasRowId = false
                    queryStr = "SELECT * FROM \"$tableName\" $whereClause LIMIT $PAGE_SIZE OFFSET ${_currentPage.value * PAGE_SIZE}"
                }

                val dataCursor = db.rawQuery(queryStr, selectionArgs)
                val rowList = mutableListOf<Map<String, String?>>()
                val rowIdList = mutableListOf<Long?>()

                val colNames = dataCursor.columnNames
                val rowIdIndex = colNames.indexOf("_custom_row_id_")

                if (dataCursor.moveToFirst()) {
                    do {
                        val rowMap = mutableMapOf<String, String?>()
                        var rowIdVal: Long? = null
                        if (hasRowId && rowIdIndex != -1) {
                            rowIdVal = dataCursor.getLong(rowIdIndex)
                        }

                        for (i in colNames.indices) {
                            val colName = colNames[i]
                            if (colName == "_custom_row_id_") continue

                            if (dataCursor.isNull(i)) {
                                rowMap[colName] = null
                            } else {
                                rowMap[colName] = dataCursor.getString(i)
                            }
                        }
                        rowList.add(rowMap)
                        rowIdList.add(rowIdVal)
                    } while (dataCursor.moveToNext())
                }
                dataCursor.close()

                _tableRows.value = rowList
                _currentRowIds.value = rowIdList
            } catch (e: Exception) {
                _errorState.value = "Failed row select: ${e.localizedMessage}"
            }
        }
    }

    fun nextPage() {
        if ((_currentPage.value + 1) * PAGE_SIZE < _totalRowsCount.value) {
            _currentPage.value += 1
            loadCurrentTableData()
        }
    }

    fun prevPage() {
        if (_currentPage.value > 0) {
            _currentPage.value -= 1
            loadCurrentTableData()
        }
    }

    // ==========================================
    // SCHEMA ALTER AND CREATE STATEMENTS
    // ==========================================

    fun createNewTable(tableName: String, cols: List<DbColumnInfo>) {
        val db = currentOpenDb ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (tableName.trim().isEmpty()) throw IllegalArgumentException("Table name cannot be blank")
                if (cols.isEmpty()) throw IllegalArgumentException("At least 1 column is required")

                val definitionsList = cols.map { col ->
                    val pkSuffix = if (col.isPrimaryKey) " PRIMARY KEY AUTOINCREMENT" else ""
                    val nnSuffix = if (!col.isNullable) " NOT NULL" else ""
                    val dfltSuffix = if (!col.defaultValue.isNullOrBlank()) " DEFAULT '${col.defaultValue}'" else ""
                    "\"${col.name}\" ${col.type}$pkSuffix$nnSuffix$dfltSuffix"
                }
                val query = "CREATE TABLE \"${tableName.trim()}\" (${definitionsList.joinToString(", ")});"
                db.execSQL(query)
                _infoState.value = "Table '${tableName}' created successfully."
                loadTablesList()
            } catch (e: Exception) {
                _errorState.value = "Create table failed: ${e.localizedMessage}"
            }
        }
    }

    fun dropTable(tableName: String) {
        val db = currentOpenDb ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.execSQL("DROP TABLE \"$tableName\";")
                _infoState.value = "Table '$tableName' dropped successfully."
                _selectedTable.value = null
                _columns.value = emptyList()
                _tableRows.value = emptyList()
                loadTablesList()
            } catch (e: Exception) {
                _errorState.value = "Drop table error: ${e.localizedMessage}"
            }
        }
    }

    fun addColumnToTable(tableName: String, col: DbColumnInfo) {
        val db = currentOpenDb ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nnSuffix = if (!col.isNullable && !col.defaultValue.isNullOrBlank()) " NOT NULL DEFAULT '${col.defaultValue}'" else ""
                val query = "ALTER TABLE \"$tableName\" ADD COLUMN \"${col.name}\" ${col.type}$nnSuffix;"
                db.execSQL(query)
                _infoState.value = "Column '${col.name}' added successfully."
                loadTableSchema(tableName)
                loadCurrentTableData()
            } catch (e: Exception) {
                _errorState.value = "Add column failed: ${e.localizedMessage}"
            }
        }
    }

    // ==========================================
    // ROW INSERT, EDIT, OR DELETE HANDLERS
    // ==========================================

    fun insertRowIntoTable(tableName: String, data: Map<String, String>) {
        val db = currentOpenDb ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // filter columns to map into insert statement (discard auto keys that are empty)
                val cleanCols = data.filter { (k, v) -> v.isNotBlank() }

                val colNames = cleanCols.keys.joinToString(", ") { "\"$it\"" }
                val valuePlaceholders = cleanCols.values.joinToString(", ") { "?" }
                val args = cleanCols.values.toTypedArray()

                val query = "INSERT INTO \"$tableName\" ($colNames) VALUES ($valuePlaceholders);"
                db.execSQL(query, args)
                _infoState.value = "Row inserted successfully."
                loadCurrentTableData()
            } catch (e: Exception) {
                _errorState.value = "Insertion error: ${e.localizedMessage}"
            }
        }
    }

    fun updateRowInTable(tableName: String, rowIdx: Int, updateMap: Map<String, String>) {
        val db = currentOpenDb ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowId = _currentRowIds.value.getOrNull(rowIdx)
                val query: String
                val args: Array<String>

                if (rowId != null) {
                    val updateClauses = updateMap.keys.joinToString(", ") { "\"$it\" = ?" }
                    query = "UPDATE \"$tableName\" SET $updateClauses WHERE rowid = ?;"
                    val arrayVals = updateMap.values.toMutableList()
                    arrayVals.add(rowId.toString())
                    args = arrayVals.toTypedArray()
                } else {
                    // fallback using pk
                    val pkCol = _columns.value.firstOrNull { it.isPrimaryKey }?.name ?: throw Exception("Table has no primary key or unique identifier.")
                    val pkVal = _tableRows.value[rowIdx][pkCol] ?: throw Exception("Row Primary Key value is null.")
                    val updateClauses = updateMap.keys.joinToString(", ") { "\"$it\" = ?" }
                    query = "UPDATE \"$tableName\" SET $updateClauses WHERE \"$pkCol\" = ?;"
                    val arrayVals = updateMap.values.toMutableList()
                    arrayVals.add(pkVal)
                    args = arrayVals.toTypedArray()
                }

                db.execSQL(query, args)
                _infoState.value = "Row updated successfully."
                loadCurrentTableData()
            } catch (e: Exception) {
                _errorState.value = "Update Row error: ${e.localizedMessage}"
            }
        }
    }

    fun deleteRowFromTable(tableName: String, rowIdx: Int) {
        val db = currentOpenDb ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowId = _currentRowIds.value.getOrNull(rowIdx)
                if (rowId != null) {
                    db.execSQL("DELETE FROM \"$tableName\" WHERE rowid = ?;", arrayOf(rowId.toString()))
                } else {
                    // Fallback to Primary Key
                    val pkCol = _columns.value.firstOrNull { it.isPrimaryKey }?.name ?: throw Exception("Unable to identify target row.")
                    val pkVal = _tableRows.value[rowIdx][pkCol] ?: throw Exception("Could not fetch Primary Key id.")
                    db.execSQL("DELETE FROM \"$tableName\" WHERE \"$pkCol\" = ?;", arrayOf(pkVal))
                }
                _infoState.value = "Row deleted successfully."
                loadCurrentTableData()
            } catch (e: Exception) {
                _errorState.value = "Failed row delete: ${e.localizedMessage}"
            }
        }
    }

    // ==========================================
    // AD-HOC TEXT SQL RUNNER
    // ==========================================

    fun executeTerminalSql(queryStr: String) {
        val db = currentOpenDb ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val queryClean = queryStr.trim()
            try {
                val isSelect = queryClean.lowercase().startsWith("select") || queryClean.lowercase().startsWith("pragma")
                if (isSelect) {
                    val cursor = db.rawQuery(queryClean, null)
                    val cols = cursor.columnNames.toList()
                    val rowsList = mutableListOf<List<String?>>()

                    if (cursor.moveToFirst()) {
                        do {
                            val singleRow = mutableListOf<String?>()
                            for (i in cols.indices) {
                                if (cursor.isNull(i)) {
                                    singleRow.add(null)
                                } else {
                                    singleRow.add(cursor.getString(i))
                                }
                            }
                            rowsList.add(singleRow)
                        } while (cursor.moveToNext())
                    }
                    cursor.close()
                    val duration = System.currentTimeMillis() - startTime
                    _terminalResult.value = SqlResult(
                        columns = cols,
                        rows = rowsList,
                        message = "Query executed successfully, fetched ${rowsList.size} records.",
                        executionTimeMs = duration,
                        success = true
                    )
                } else {
                    // Write command execution
                    db.execSQL(queryClean)
                    val duration = System.currentTimeMillis() - startTime
                    _terminalResult.value = SqlResult(
                        columns = emptyList(),
                        rows = emptyList(),
                        message = "SQL command executed successfully.",
                        executionTimeMs = duration,
                        success = true
                    )
                    // reloading lists in cases tables/schemas modified
                    loadTablesList()
                    _selectedTable.value?.let { loadTableSchema(it); loadCurrentTableData() }
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                _terminalResult.value = SqlResult(
                    columns = emptyList(),
                    rows = emptyList(),
                    message = "SQL Error: ${e.localizedMessage}",
                    executionTimeMs = duration,
                    success = false
                )
            }
        }
    }

    // Pragma quick actions
    fun runIntegrityCheck() {
        executeTerminalSql("PRAGMA integrity_check;")
    }

    fun runForeignKeyCheck() {
        executeTerminalSql("PRAGMA foreign_key_check;")
    }

    fun runVacuumOptimize() {
        executeTerminalSql("VACUUM;")
    }

    // ==========================================
    // DATA EXPORT BRIDGE
    // ==========================================

    fun exportTableToCSV(context: Context, tableName: String) {
        val db = currentOpenDb ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cursor = db.rawQuery("SELECT * FROM \"$tableName\"", null)
                val colNames = cursor.columnNames
                val stringBuilder = StringBuilder()

                // write headers
                stringBuilder.append(colNames.joinToString(",") { "\"$it\"" }).append("\n")

                // write cells
                if (cursor.moveToFirst()) {
                    do {
                        val rowCells = mutableListOf<String>()
                        for (i in colNames.indices) {
                            val value = if (cursor.isNull(i)) "" else cursor.getString(i).replace("\"", "\"\"")
                            rowCells.add("\"$value\"")
                        }
                        stringBuilder.append(rowCells.joinToString(",")).append("\n")
                    } while (cursor.moveToNext())
                }
                cursor.close()

                // Save to files folder
                val file = File(context.filesDir, "${tableName}_export_${System.currentTimeMillis() / 1000}.csv")
                file.writeText(stringBuilder.toString())
                _infoState.value = "CSV exported safely: ${file.name} in files directory."
            } catch (e: Exception) {
                _errorState.value = "CSV Export failure: ${e.localizedMessage}"
            }
        }
    }
}

// ==========================================
// CENTRAL JETPACK COMPOSE COMPOSABLE
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DBScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: DBViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // states
    val dbFiles by viewModel.dbFiles.collectAsState()
    val selectedDb by viewModel.selectedDb.collectAsState()
    val tables by viewModel.tables.collectAsState()
    val selectedTable by viewModel.selectedTable.collectAsState()
    val columns by viewModel.columns.collectAsState()
    val tableRows by viewModel.tableRows.collectAsState()
    val totalRowsCount by viewModel.totalRowsCount.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    
    val errorState by viewModel.errorState.collectAsState()
    val infoState by viewModel.infoState.collectAsState()

    val terminalResult by viewModel.terminalResult.collectAsState()
    val terminalInput by viewModel.terminalInput.collectAsState()

    // triggers scan database index
    LaunchedEffect(Unit) {
        viewModel.scanDatabases(context)
    }

    // Modal/Dialog states
    var showCreateDbDialog by remember { mutableStateOf(false) }
    var showCreateTableDialog by remember { mutableStateOf(false) }
    var showAddColDialog by remember { mutableStateOf(false) }
    var showAddRowDialog by remember { mutableStateOf(false) }
    var showEditRowDialogIdx by remember { mutableStateOf<Int?>(null) }
    var activeTabIdx by remember { mutableStateOf(0) } // 0: Rows Grid, 1: Columns Schema, 2: SQL Terminal, 3: Diagnostics

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Database Explorer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        selectedDb?.let {
                            Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        } ?: Text("Select database file", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "DrawerMenu")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.scanDatabases(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan")
                    }
                    if (selectedDb != null) {
                        IconButton(onClick = { viewModel.closeDatabase() }) {
                            Icon(Icons.Default.Close, contentDescription = "Disconnect")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            BoxWithConstraints {
                val isWide = maxWidth > 720.dp
                if (selectedDb == null) {
                    // Database Selection dashboard
                    DbSelectionLayout(
                        dbFiles = dbFiles,
                        onSelectFile = { viewModel.selectDatabase(it) },
                        onDeleteFile = { viewModel.deleteDatabaseFile(context, it) },
                        onCreateClick = { showCreateDbDialog = true }
                    )
                } else {
                    // Dashboard Work Area
                    if (isWide) {
                        // Two-Pane Workspace
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left tab panel selector
                            Card(
                                modifier = Modifier
                                    .width(240.dp)
                                    .fillMaxHeight()
                                    .padding(4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                SidebarTableSelector(
                                    tables = tables,
                                    selectedTable = selectedTable ?: "",
                                    onSelectTable = { viewModel.selectTable(it) },
                                    onCreateTableClick = { showCreateTableDialog = true }
                                )
                            }
                            // Right working panel tabs
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                WorkspaceTabRow(
                                    selectedTabIdx = activeTabIdx,
                                    onTabSelected = { activeTabIdx = it }
                                )
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    WorkspaceContentPane(
                                        activeTabIdx = activeTabIdx,
                                        selectedTable = selectedTable,
                                        columns = columns,
                                        rows = tableRows,
                                        totalRowsCount = totalRowsCount,
                                        currentPage = currentPage,
                                        searchFilter = searchFilter,
                                        terminalResult = terminalResult,
                                        terminalInput = terminalInput,
                                        viewModel = viewModel,
                                        onExportCSV = { viewModel.exportTableToCSV(context, it) },
                                        onAddColClick = { showAddColDialog = true },
                                        onAddRowClick = { showAddRowDialog = true },
                                        onEditRowClick = { showEditRowDialogIdx = it },
                                        onDeleteRowClick = { viewModel.deleteRowFromTable(selectedTable ?: "", it) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Compact Single-Pane view routing
                        var showTableDrawerMobile by remember { mutableStateOf(selectedTable == null) }
                        if (showTableDrawerMobile) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                SidebarTableSelector(
                                    tables = tables,
                                    selectedTable = selectedTable ?: "",
                                    onSelectTable = {
                                        viewModel.selectTable(it)
                                        showTableDrawerMobile = false
                                    },
                                    onCreateTableClick = { showCreateTableDialog = true }
                                )
                                if (selectedTable != null) {
                                    FloatingActionButton(
                                        onClick = { showTableDrawerMobile = false },
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Open View")
                                    }
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // top navigation banner
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(onClick = { showTableDrawerMobile = true }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tables Drawer")
                                    }
                                    Text(selectedTable ?: "No Table", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Spacer(Modifier.width(48.dp))
                                }

                                WorkspaceTabRow(
                                    selectedTabIdx = activeTabIdx,
                                    onTabSelected = { activeTabIdx = it }
                                )
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    WorkspaceContentPane(
                                        activeTabIdx = activeTabIdx,
                                        selectedTable = selectedTable,
                                        columns = columns,
                                        rows = tableRows,
                                        totalRowsCount = totalRowsCount,
                                        currentPage = currentPage,
                                        searchFilter = searchFilter,
                                        terminalResult = terminalResult,
                                        terminalInput = terminalInput,
                                        viewModel = viewModel,
                                        onExportCSV = { viewModel.exportTableToCSV(context, it) },
                                        onAddColClick = { showAddColDialog = true },
                                        onAddRowClick = { showAddRowDialog = true },
                                        onEditRowClick = { showEditRowDialogIdx = it },
                                        onDeleteRowClick = { viewModel.deleteRowFromTable(selectedTable ?: "", it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SNACKBAR DIAGNOSTICS & SUCCESS BANNER
            // ==========================================
            errorState?.let {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) { Text("DISMISS", color = Color.Yellow) }
                    }
                ) { Text(it) }
            }
            infoState?.let {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearInfo() }) { Text("OK", color = Color.White) }
                    }
                ) { Text(it) }
            }

            // ==========================================
            // DIALOG BUILDERS
            // ==========================================
            if (showCreateDbDialog) {
                var dbNameState by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreateDbDialog = false },
                    title = { Text("Create SQLite Database") },
                    text = {
                        OutlinedTextField(
                            value = dbNameState,
                            onValueChange = { dbNameState = it },
                            placeholder = { Text("e.g. app_cache") },
                            label = { Text("Database File Name") },
                            suffix = { Text(".db") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (dbNameState.isNotBlank()) {
                                viewModel.createEmptyDatabase(context, dbNameState)
                            }
                            showCreateDbDialog = false
                        }) { Text("Create") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDbDialog = false }) { Text("Cancel") }
                    }
                )
            }

            if (showCreateTableDialog) {
                var tblNameState by remember { mutableStateOf("") }
                var inputCols = remember { mutableStateListOf(DbColumnInfo("id", "INTEGER", false, true, null)) }
                AlertDialog(
                    onDismissRequest = { showCreateTableDialog = false },
                    title = { Text("Assemble Table Creator Wizard") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                            OutlinedTextField(
                                value = tblNameState,
                                onValueChange = { tblNameState = it },
                                label = { Text("Table Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Columns Definition:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))

                            inputCols.forEachIndexed { idx, col ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Column #${idx + 1}", fontWeight = FontWeight.SemiBold)
                                            if (idx > 0) {
                                                IconButton(onClick = { inputCols.removeAt(idx) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Col")
                                                }
                                            }
                                        }
                                        OutlinedTextField(
                                            value = col.name,
                                            onValueChange = { inputCols[idx] = col.copy(name = it) },
                                            label = { Text("Name") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            var expandedTypeMenu by remember { mutableStateOf(false) }
                                            Box(modifier = Modifier.weight(1f)) {
                                                OutlinedButton(
                                                    onClick = { expandedTypeMenu = true },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(col.type)
                                                }
                                                DropdownMenu(expanded = expandedTypeMenu, onDismissRequest = { expandedTypeMenu = false }) {
                                                    listOf("TEXT", "INTEGER", "REAL", "BLOB", "NUMERIC").forEach { type ->
                                                        DropdownMenuItem(text = { Text(type) }, onClick = {
                                                            inputCols[idx] = col.copy(type = type)
                                                            expandedTypeMenu = false
                                                        })
                                                    }
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(checked = col.isPrimaryKey, onCheckedChange = {
                                                    inputCols[idx] = col.copy(isPrimaryKey = it)
                                                })
                                                Text("PK", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { inputCols.add(DbColumnInfo("new_col", "TEXT", true, false, null)) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) { Text("+ Add Column") }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.createNewTable(tblNameState, inputCols)
                            showCreateTableDialog = false
                        }) { Text("Create Table") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateTableDialog = false }) { Text("Cancel") }
                    }
                )
            }

            if (showAddColDialog) {
                var newColName by remember { mutableStateOf("") }
                var newColType by remember { mutableStateOf("TEXT") }
                AlertDialog(
                    onDismissRequest = { showAddColDialog = false },
                    title = { Text("Alter Add Column") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newColName,
                                onValueChange = { newColName = it },
                                label = { Text("Column Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(16.dp))
                            var expandedMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { expandedMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Data Type: $newColType")
                                }
                                DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                                    listOf("TEXT", "INTEGER", "REAL", "BLOB", "NUMERIC").forEach { t ->
                                        DropdownMenuItem(text = { Text(t) }, onClick = {
                                            newColType = t
                                            expandedMenu = false
                                        })
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newColName.isNotBlank() && selectedTable != null) {
                                viewModel.addColumnToTable(selectedTable!!, DbColumnInfo(newColName, newColType, true, false, null))
                            }
                            showAddColDialog = false
                        }) { Text("Add") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddColDialog = false }) { Text("Cancel") }
                    }
                )
            }

            if (showAddRowDialog && selectedTable != null) {
                val inputMap = remember { mutableStateMapOf<String, String>() }
                AlertDialog(
                    onDismissRequest = { showAddRowDialog = false },
                    title = { Text("Insert New Row Record") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                            columns.forEach { col ->
                                // skip auto increment key on row inserts
                                if (col.isPrimaryKey && col.type == "INTEGER") return@forEach

                                val currentVal = inputMap[col.name] ?: ""
                                OutlinedTextField(
                                    value = currentVal,
                                    onValueChange = { inputMap[col.name] = it },
                                    label = { Text("${col.name} (${col.type})") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.insertRowIntoTable(selectedTable!!, inputMap.toMap())
                            showAddRowDialog = false
                        }) { Text("Insert") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddRowDialog = false }) { Text("Cancel") }
                    }
                )
            }

            showEditRowDialogIdx?.let { rowIdx ->
                val inputMap = remember { mutableStateMapOf<String, String>() }
                LaunchedEffect(rowIdx) {
                    val originalMap = tableRows[rowIdx]
                    originalMap.forEach { (k, v) ->
                        inputMap[k] = v ?: ""
                    }
                }
                AlertDialog(
                    onDismissRequest = { showEditRowDialogIdx = null },
                    title = { Text("Edit Row Details") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                            columns.forEach { col ->
                                // PK column shouldn't be altered easily
                                val cellVal = inputMap[col.name] ?: ""
                                OutlinedTextField(
                                    value = cellVal,
                                    onValueChange = { inputMap[col.name] = it },
                                    label = { Text(col.name) },
                                    placeholder = { Text(col.type) },
                                    enabled = !col.isPrimaryKey,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.updateRowInTable(selectedTable!!, rowIdx, inputMap.toMap().filterKeys { key ->
                                val orig = columns.firstOrNull { it.name == key }
                                orig != null && !orig.isPrimaryKey
                            })
                            showEditRowDialogIdx = null
                        }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditRowDialogIdx = null }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

// ==========================================
// SUB-COMPOSABLES WORKSPACE
// ==========================================

@Composable
fun DbSelectionLayout(
    dbFiles: List<DbFileItem>,
    onSelectFile: (DbFileItem) -> Unit,
    onDeleteFile: (DbFileItem) -> Unit,
    onCreateClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Connection Manager", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Button(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = "Add DB")
                Spacer(Modifier.width(4.dp))
                Text("New Database")
            }
        }
        Spacer(Modifier.height(8.dp))

        if (dbFiles.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info, 
                        contentDescription = "No db files", 
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No local SQLite database files found.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                    Text("Tap \"New Database\" to start editing.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dbFiles) { db ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFile(db) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Db Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(db.name, fontWeight = FontWeight.Bold)
                                    val sizeStr = if (db.size < 1024) "${db.size} B" else "${db.size / 1024} KB"
                                    Text("Size: $sizeStr • ${db.path}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            IconButton(onClick = { onDeleteFile(db) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Database File", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SidebarTableSelector(
    tables: List<String>,
    selectedTable: String,
    onSelectTable: (String) -> Unit,
    onCreateTableClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tables Indexed", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = onCreateTableClick) {
                Icon(Icons.Default.Add, contentDescription = "New Table", tint = MaterialTheme.colorScheme.primary)
            }
        }
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        if (tables.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No tables generated", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(tables) { table ->
                    val isSelected = (table == selectedTable)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectTable(table) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Table icon",
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = table,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspaceTabRow(selectedTabIdx: Int, onTabSelected: (Int) -> Unit) {
    val tabTitles = listOf("Table Records", "Schema structure", "Ad-hoc Terminal SQL", "Diagnostics Check")
    TabRow(selectedTabIndex = selectedTabIdx) {
        tabTitles.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIdx == index,
                onClick = { onTabSelected(index) },
                text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) }
            )
        }
    }
}

@Composable
fun WorkspaceContentPane(
    activeTabIdx: Int,
    selectedTable: String?,
    columns: List<DbColumnInfo>,
    rows: List<Map<String, String?>>,
    totalRowsCount: Int,
    currentPage: Int,
    searchFilter: String,
    terminalResult: SqlResult?,
    terminalInput: String,
    viewModel: DBViewModel,
    onExportCSV: (String) -> Unit,
    onAddColClick: () -> Unit,
    onAddRowClick: () -> Unit,
    onEditRowClick: (Int) -> Unit,
    onDeleteRowClick: (Int) -> Unit
) {
    when (activeTabIdx) {
        0 -> {
            // Tab 0: Table Records Grid
            if (selectedTable == null) {
                EmptyWorkspaceSplash()
            } else {
                RowViewerTab(
                    selectedTable = selectedTable,
                    columns = columns,
                    rows = rows,
                    totalRows = totalRowsCount,
                    currentPage = currentPage,
                    searchFilter = searchFilter,
                    onPrevPage = { viewModel.prevPage() },
                    onNextPage = { viewModel.nextPage() },
                    onFilterChange = { viewModel.setSearchFilter(it) },
                    onAddRowClick = onAddRowClick,
                    onEditRowClick = onEditRowClick,
                    onDeleteRowClick = onDeleteRowClick,
                    onExportCSV = onExportCSV
                )
            }
        }
        1 -> {
            // Tab 1: Schema Details
            if (selectedTable == null) {
                EmptyWorkspaceSplash()
            } else {
                SchemaViewerTab(
                    selectedTable = selectedTable,
                    columns = columns,
                    onAddColumn = onAddColClick,
                    onDropTable = { viewModel.dropTable(selectedTable) }
                )
            }
        }
        2 -> {
            // Tab 2: Terminal SQL free runner
            TerminalSqlTab(
                terminalInput = terminalInput,
                terminalResult = terminalResult,
                onInputChange = { viewModel.setTerminalInput(it) },
                onExecute = { viewModel.executeTerminalSql(it) }
            )
        }
        3 -> {
            // Tab 3: Diagnostics check parameters
            DiagnosticsTab(
                onIntegrity = { viewModel.runIntegrityCheck() },
                onForeignKey = { viewModel.runForeignKeyCheck() },
                onVacuum = { viewModel.runVacuumOptimize() },
                terminalResult = terminalResult
            )
        }
    }
}

@Composable
fun EmptyWorkspaceSplash() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info, 
                contentDescription = "Empty", 
                modifier = Modifier.size(72.dp),
                tint = Color.Gray.copy(alpha=0.3f)
            )
            Spacer(Modifier.height(16.dp))
            Text("Select Table from sidebar index panel", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun RowViewerTab(
    selectedTable: String,
    columns: List<DbColumnInfo>,
    rows: List<Map<String, String?>>,
    totalRows: Int,
    currentPage: Int,
    searchFilter: String,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onFilterChange: (String) -> Unit,
    onAddRowClick: () -> Unit,
    onEditRowClick: (Int) -> Unit,
    onDeleteRowClick: (Int) -> Unit,
    onExportCSV: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Controls Banner
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchFilter,
                onValueChange = onFilterChange,
                placeholder = { Text("Filter cells...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onAddRowClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Row")
                Text("Insert Row")
            }
            OutlinedButton(onClick = { onExportCSV(selectedTable) }) {
                Icon(Icons.Default.Share, contentDescription = "Export")
                Text("CSV")
            }
        }

        // Horizontal Grid scroll
        val scrollState = rememberScrollState()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                // Table title columns header row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 10.dp)
                ) {
                    Spacer(Modifier.width(80.dp).align(Alignment.CenterVertically)) // for edit click spacer actions
                    columns.forEach { col ->
                        Text(
                            text = col.name,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.width(160.dp).padding(horizontal = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Table Rows
                if (rows.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).width(400.dp), contentAlignment = Alignment.Center) {
                        Text("No records matched.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(rows.size) { idx ->
                            val row = rows[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(width = 0.5.dp, color = Color.LightGray.copy(alpha=0.5f))
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic row control triggers
                                Row(
                                    modifier = Modifier.width(80.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Cell",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { onEditRowClick(idx) }.size(20.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Cell",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.clickable { onDeleteRowClick(idx) }.size(20.dp)
                                    )
                                }

                                columns.forEach { col ->
                                    val cellValue = row[col.name] ?: "NULL"
                                    Text(
                                        text = cellValue,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(160.dp).padding(horizontal = 8.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pagination row footer
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fromIndex = currentPage * 25 + 1
            val toIndex = ((currentPage + 1) * 25).coerceAtMost(totalRows)
            Text(
                text = "Viewing $fromIndex to $toIndex of $totalRows rows",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onPrevPage, enabled = currentPage > 0) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Prev Page", modifier = Modifier.graphicsLayer(rotationZ = 180f))
                }
                IconButton(onClick = onNextPage, enabled = (currentPage + 1) * 25 < totalRows) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Next Page")
                }
            }
        }
    }
}

@Composable
fun SchemaViewerTab(
    selectedTable: String,
    columns: List<DbColumnInfo>,
    onAddColumn: () -> Unit,
    onDropTable: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Column Definitions Metadata", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddColumn, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Column")
                }
                Button(onClick = onDropTable, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Drop Table")
                }
            }
        }
        HorizontalDivider()

        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(columns) { col ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(col.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (col.isPrimaryKey) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "PRIMARY KEY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Type: ${col.type} • Nullable: ${col.isNullable}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        col.defaultValue?.let {
                            Text("Def: $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.background(Color.LightGray.copy(alpha=0.3f)).padding(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalSqlTab(
    terminalInput: String,
    terminalResult: SqlResult?,
    onInputChange: (String) -> Unit,
    onExecute: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ad-hoc Terminal SQL Runner", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = terminalInput,
            onValueChange = onInputChange,
            placeholder = { Text("Write SQL commands here ... e.g. SELECT * FROM sqlite_master;") },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { onExecute(terminalInput) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Run")
            Spacer(Modifier.width(4.dp))
            Text("Execute Query")
        }

        Spacer(Modifier.height(16.dp))
        Text("Execution Result Terminal:", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (terminalResult != null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (terminalResult.success) "✓ SUCCESS" else "✗ FAILURE",
                                color = if (terminalResult.success) Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Spent: ${terminalResult.executionTimeMs}ms",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = terminalResult.message,
                            color = if (terminalResult.success) Color.White else Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontFamily = FontFamily.Monospace
                        )
                        HorizontalDivider(color = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))

                        if (terminalResult.columns.isNotEmpty()) {
                            val state = rememberScrollState()
                            Column(modifier = Modifier.fillMaxSize().horizontalScroll(state)) {
                                Row(modifier = Modifier.background(Color.DarkGray).padding(4.dp)) {
                                    terminalResult.columns.forEach { col ->
                                        Text(
                                            text = col,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.width(120.dp).padding(horizontal = 4.dp),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(terminalResult.rows) { row ->
                                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                            row.forEach { cell ->
                                                Text(
                                                    text = cell ?: "NULL",
                                                    color = if (cell != null) Color.LightGray else Color.Gray,
                                                    modifier = Modifier.width(120.dp).padding(horizontal = 4.dp),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Ready to compile standard SQL inquiries.", color = Color.DarkGray, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticsTab(
    onIntegrity: () -> Unit,
    onForeignKey: () -> Unit,
    onVacuum: () -> Unit,
    terminalResult: SqlResult?
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("SQLite Integrity & Tuning Tools", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onIntegrity, modifier = Modifier.weight(1f)) {
                Text("Integrity Check")
            }
            Button(onClick = onForeignKey, modifier = Modifier.weight(1f)) {
                Text("FK Check")
            }
            Button(onClick = onVacuum, modifier = Modifier.weight(1f)) {
                Text("Vacuum Store")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Diagnostic Diagnostics Console Output:", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (terminalResult != null) {
                    Column {
                        Text(
                            text = "Result code check: ${if (terminalResult.success) "SUCCESS" else "FAILED"}",
                            color = if (terminalResult.success) Color.Green else Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = terminalResult.message,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        if (terminalResult.rows.isNotEmpty()) {
                            terminalResult.rows.forEach { row ->
                                Text(row.joinToString(" | ") { it ?: "NULL" }, color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Integrity diagnostic check logs will populate here.", color = Color.DarkGray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
