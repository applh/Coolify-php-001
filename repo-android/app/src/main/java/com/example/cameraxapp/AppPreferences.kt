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
    val STORAGE_LOCATION = intPreferencesKey("storage_location") // 0: Internal, 1: Public, 2: SD Card
    val VIDEO_QUALITY = intPreferencesKey("video_quality") // 0: SD, 1: HD, 2: FHD, 3: UHD, 4: HIGHEST
    val ENABLE_AUDIO = booleanPreferencesKey("enable_audio")
    val SHOW_CROSSHAIR = booleanPreferencesKey("show_crosshair")
    val SHOW_GRID = booleanPreferencesKey("show_grid")
    val GRID_ROWS = intPreferencesKey("grid_rows")
    val GRID_COLUMNS = intPreferencesKey("grid_columns")
    val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    val COLOR_THEME = intPreferencesKey("color_theme") // 0: Standard, 1: Lumina AI Theme
    val AI_MODEL = stringPreferencesKey("ai_model")
    val AI_RATIO = stringPreferencesKey("ai_ratio")
    val AI_SIZE = stringPreferencesKey("ai_size")
    val PUBLIC_GALLERY_NAME = stringPreferencesKey("public_gallery_name")
    val AUTOPILOT_PERSONA = stringPreferencesKey("autopilot_persona")
    val WALLPAPER_FOLDER_URI = stringPreferencesKey("wallpaper_folder_uri")
}

class SettingsRepository(private val context: Context) {
    val themeMode: Flow<Int> = context.dataStore.data.map { it[AppPreferences.THEME_MODE] ?: 0 }
    val colorTheme: Flow<Int> = context.dataStore.data.map { it[AppPreferences.COLOR_THEME] ?: 0 }
    val defaultLensFacing: Flow<Int> = context.dataStore.data.map { it[AppPreferences.DEFAULT_LENS_FACING] ?: 1 }
    val defaultFlashMode: Flow<Int> = context.dataStore.data.map { it[AppPreferences.DEFAULT_FLASH_MODE] ?: 2 }
    val storageLocation: Flow<Int> = context.dataStore.data.map { it[AppPreferences.STORAGE_LOCATION] ?: 0 }
    val videoQuality: Flow<Int> = context.dataStore.data.map { it[AppPreferences.VIDEO_QUALITY] ?: 4 }
    val enableAudio: Flow<Boolean> = context.dataStore.data.map { it[AppPreferences.ENABLE_AUDIO] ?: true }
    val showCrosshair: Flow<Boolean> = context.dataStore.data.map { it[AppPreferences.SHOW_CROSSHAIR] ?: true }
    val showGrid: Flow<Boolean> = context.dataStore.data.map { it[AppPreferences.SHOW_GRID] ?: false }
    val gridRows: Flow<Int> = context.dataStore.data.map { it[AppPreferences.GRID_ROWS] ?: 3 }
    val gridColumns: Flow<Int> = context.dataStore.data.map { it[AppPreferences.GRID_COLUMNS] ?: 3 }
    val geminiApiKey: Flow<String> = context.dataStore.data.map { it[AppPreferences.GEMINI_API_KEY] ?: "" }
    val aiModel: Flow<String> = context.dataStore.data.map { it[AppPreferences.AI_MODEL] ?: "gemini-2.5-flash-image" }
    val aiRatio: Flow<String> = context.dataStore.data.map { it[AppPreferences.AI_RATIO] ?: "1:1" }
    val aiSize: Flow<String> = context.dataStore.data.map { it[AppPreferences.AI_SIZE] ?: "1K" }
    val publicGalleryName: Flow<String> = context.dataStore.data.map { it[AppPreferences.PUBLIC_GALLERY_NAME] ?: "GeminiCanvas" }
    val autopilotPersona: Flow<String> = context.dataStore.data.map { it[AppPreferences.AUTOPILOT_PERSONA] ?: "Continuous Autopilot Periodic Review: Critique and expand on our previous prompt concept with updated insights or direct project guidelines." }
    val wallpaperFolderUri: Flow<String> = context.dataStore.data.map { it[AppPreferences.WALLPAPER_FOLDER_URI] ?: "" }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[AppPreferences.THEME_MODE] = mode }
    }

    suspend fun setDefaultLensFacing(facing: Int) {
        context.dataStore.edit { it[AppPreferences.DEFAULT_LENS_FACING] = facing }
    }

    suspend fun setDefaultFlashMode(mode: Int) {
        context.dataStore.edit { it[AppPreferences.DEFAULT_FLASH_MODE] = mode }
    }

    suspend fun setStorageLocation(location: Int) {
        context.dataStore.edit { it[AppPreferences.STORAGE_LOCATION] = location }
    }

    suspend fun setVideoQuality(quality: Int) {
        context.dataStore.edit { it[AppPreferences.VIDEO_QUALITY] = quality }
    }

    suspend fun setEnableAudio(enable: Boolean) {
        context.dataStore.edit { it[AppPreferences.ENABLE_AUDIO] = enable }
    }

    suspend fun setShowCrosshair(show: Boolean) {
        context.dataStore.edit { it[AppPreferences.SHOW_CROSSHAIR] = show }
    }

    suspend fun setShowGrid(show: Boolean) {
        context.dataStore.edit { it[AppPreferences.SHOW_GRID] = show }
    }

    suspend fun setGridRows(rows: Int) {
        context.dataStore.edit { it[AppPreferences.GRID_ROWS] = rows }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { it[AppPreferences.GRID_COLUMNS] = columns }
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { it[AppPreferences.GEMINI_API_KEY] = key }
    }

    suspend fun setColorTheme(theme: Int) {
        context.dataStore.edit { it[AppPreferences.COLOR_THEME] = theme }
    }

    suspend fun setAiModel(model: String) {
        context.dataStore.edit { it[AppPreferences.AI_MODEL] = model }
    }

    suspend fun setAiRatio(ratio: String) {
        context.dataStore.edit { it[AppPreferences.AI_RATIO] = ratio }
    }

    suspend fun setAiSize(size: String) {
        context.dataStore.edit { it[AppPreferences.AI_SIZE] = size }
    }

    suspend fun setPublicGalleryName(name: String) {
        context.dataStore.edit { it[AppPreferences.PUBLIC_GALLERY_NAME] = name }
    }

    suspend fun setAutopilotPersona(persona: String) {
        context.dataStore.edit { it[AppPreferences.AUTOPILOT_PERSONA] = persona }
    }

    suspend fun setWallpaperFolderUri(uri: String) {
        context.dataStore.edit { it[AppPreferences.WALLPAPER_FOLDER_URI] = uri }
    }
}
