package com.example.cameraxapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object AppPreferences {
    val THEME_MODE = intPreferencesKey("theme_mode") // 0: System, 1: Light, 2: Dark
    val DEFAULT_LENS_FACING = intPreferencesKey("default_lens_facing") // 1: Back, 0: Front
    val DEFAULT_FLASH_MODE = intPreferencesKey("default_flash_mode") // 2: Auto, 1: On, 0: Off
    val SAVE_TO_PUBLIC = booleanPreferencesKey("save_to_public")
}

class SettingsRepository(private val context: Context) {
    val themeMode: Flow<Int> = context.dataStore.data.map { it[AppPreferences.THEME_MODE] ?: 0 }
    val defaultLensFacing: Flow<Int> = context.dataStore.data.map { it[AppPreferences.DEFAULT_LENS_FACING] ?: 1 }
    val defaultFlashMode: Flow<Int> = context.dataStore.data.map { it[AppPreferences.DEFAULT_FLASH_MODE] ?: 2 }
    val saveToPublic: Flow<Boolean> = context.dataStore.data.map { it[AppPreferences.SAVE_TO_PUBLIC] ?: false }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[AppPreferences.THEME_MODE] = mode }
    }

    suspend fun setDefaultLensFacing(facing: Int) {
        context.dataStore.edit { it[AppPreferences.DEFAULT_LENS_FACING] = facing }
    }

    suspend fun setDefaultFlashMode(mode: Int) {
        context.dataStore.edit { it[AppPreferences.DEFAULT_FLASH_MODE] = mode }
    }

    suspend fun setSaveToPublic(saveToPublic: Boolean) {
        context.dataStore.edit { it[AppPreferences.SAVE_TO_PUBLIC] = saveToPublic }
    }
}
