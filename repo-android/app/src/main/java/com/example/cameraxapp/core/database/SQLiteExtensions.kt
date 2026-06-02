package com.example.cameraxapp.core.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes a clean SQLite database query on a background disk dispatcher, ensuring
 * that cursor iteration completes properly and locks/file descriptors are freed
 * through an automated outer finally block closure.
 */
suspend inline fun <T> SQLiteDatabase.safeQuery(
    sql: String,
    selectionArgs: Array<String>? = null,
    crossinline transform: (Cursor) -> T
): List<T> = withContext(Dispatchers.IO) {
    val list = mutableListOf<T>()
    var cursor: Cursor? = null
    try {
        cursor = rawQuery(sql, selectionArgs)
        if (cursor.moveToFirst()) {
            do {
                list.add(transform(cursor))
            } while (cursor.moveToNext())
        }
    } finally {
        cursor?.close()
    }
    return@withContext list
}
