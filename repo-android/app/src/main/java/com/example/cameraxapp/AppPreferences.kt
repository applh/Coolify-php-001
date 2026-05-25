package com.example.cameraxapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import android.content.ContentValues
import android.database.Cursor
import org.json.JSONArray
import com.example.cameraxapp.cronjob.CronJobDatabase
import com.example.cameraxapp.cronjob.CronJobEntity
import com.example.cameraxapp.cronjob.CronJobScheduler

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
    val LAUNCHER_APPLET_ORDER = stringPreferencesKey("launcher_applet_order")
    val LAUNCHER_ACTIVE_APPLETS = stringPreferencesKey("launcher_active_applets")
    val STARTUP_DEFAULT_APPLET = stringPreferencesKey("startup_default_applet")
    val CAMERA_EXTENSION = intPreferencesKey("camera_extension") // 0: Disabled, 1: HDR, 2: Portrait, 3: Night, 4: FaceRetouch
    val CONCURRENT_STREAM = booleanPreferencesKey("concurrent_stream")
    val PRO_CONTROL_MODE = booleanPreferencesKey("pro_control_mode")
    val PRO_ISO_VALUE = intPreferencesKey("pro_iso_value")
    val PRO_EXPOSURE_COMP_VALUE = intPreferencesKey("pro_exposure_comp_value")
    val OFFLINE_SCAN_HUD = booleanPreferencesKey("offline_scan_hud")
    val MAP_DEFAULT_LATITUDE = stringPreferencesKey("map_default_latitude")
    val MAP_DEFAULT_LONGITUDE = stringPreferencesKey("map_default_longitude")
    val MAP_DEFAULT_ZOOM = floatPreferencesKey("map_default_zoom")
    val MAP_LAST_LAYER_TYPE = intPreferencesKey("map_last_layer_type")
    val SCANNER_SERVICE = intPreferencesKey("scanner_service") // 0: Contour Tracing, 1: Play Services Document Scanner
    val IMAGE_SAVE_FORMAT = intPreferencesKey("image_save_format") // 0: JPEG, 1: PNG, 2: WebP
    val VIDEO_CONTAINER_FORMAT = intPreferencesKey("video_container_format") // 0: MP4, 1: MKV, 2: WebM
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
    val launcherAppletOrder: Flow<String> = context.dataStore.data.map { it[AppPreferences.LAUNCHER_APPLET_ORDER] ?: "" }
    val launcherActiveApplets: Flow<String> = context.dataStore.data.map { it[AppPreferences.LAUNCHER_ACTIVE_APPLETS] ?: "" }
    val startupDefaultRoute: Flow<String> = context.dataStore.data.map { it[AppPreferences.STARTUP_DEFAULT_APPLET] ?: "hub" }
    val cameraExtension: Flow<Int> = context.dataStore.data.map { it[AppPreferences.CAMERA_EXTENSION] ?: 0 }
    val concurrentStream: Flow<Boolean> = context.dataStore.data.map { it[AppPreferences.CONCURRENT_STREAM] ?: false }
    val proControlMode: Flow<Boolean> = context.dataStore.data.map { it[AppPreferences.PRO_CONTROL_MODE] ?: false }
    val proIsoValue: Flow<Int> = context.dataStore.data.map { it[AppPreferences.PRO_ISO_VALUE] ?: 0 }
    val proExposureCompValue: Flow<Int> = context.dataStore.data.map { it[AppPreferences.PRO_EXPOSURE_COMP_VALUE] ?: 0 }
    val offlineScanHud: Flow<Boolean> = context.dataStore.data.map { it[AppPreferences.OFFLINE_SCAN_HUD] ?: true }
    val mapDefaultLatitude: Flow<Double> = context.dataStore.data.map { (it[AppPreferences.MAP_DEFAULT_LATITUDE] ?: "48.8566").toDoubleOrNull() ?: 48.8566 }
    val mapDefaultLongitude: Flow<Double> = context.dataStore.data.map { (it[AppPreferences.MAP_DEFAULT_LONGITUDE] ?: "2.3522").toDoubleOrNull() ?: 2.3522 }
    val mapDefaultZoom: Flow<Float> = context.dataStore.data.map { it[AppPreferences.MAP_DEFAULT_ZOOM] ?: 12.0f }
    val mapLastLayerType: Flow<Int> = context.dataStore.data.map { it[AppPreferences.MAP_LAST_LAYER_TYPE] ?: 1 }
    val scannerService: Flow<Int> = context.dataStore.data.map { it[AppPreferences.SCANNER_SERVICE] ?: 0 }
    val imageSaveFormat: Flow<Int> = context.dataStore.data.map { it[AppPreferences.IMAGE_SAVE_FORMAT] ?: 0 }
    val videoContainerFormat: Flow<Int> = context.dataStore.data.map { it[AppPreferences.VIDEO_CONTAINER_FORMAT] ?: 0 }

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

    suspend fun setLauncherAppletOrder(orderJson: String) {
        context.dataStore.edit { it[AppPreferences.LAUNCHER_APPLET_ORDER] = orderJson }
    }

    suspend fun setLauncherActiveApplets(activeJson: String) {
        context.dataStore.edit { it[AppPreferences.LAUNCHER_ACTIVE_APPLETS] = activeJson }
    }

    suspend fun setStartupDefaultRoute(route: String) {
        context.dataStore.edit { it[AppPreferences.STARTUP_DEFAULT_APPLET] = route }
    }

    suspend fun setCameraExtension(mode: Int) {
        context.dataStore.edit { it[AppPreferences.CAMERA_EXTENSION] = mode }
    }

    suspend fun setConcurrentStream(enabled: Boolean) {
        context.dataStore.edit { it[AppPreferences.CONCURRENT_STREAM] = enabled }
    }

    suspend fun setProControlMode(enabled: Boolean) {
        context.dataStore.edit { it[AppPreferences.PRO_CONTROL_MODE] = enabled }
    }

    suspend fun setProIsoValue(value: Int) {
        context.dataStore.edit { it[AppPreferences.PRO_ISO_VALUE] = value }
    }

    suspend fun setProExposureCompValue(value: Int) {
        context.dataStore.edit { it[AppPreferences.PRO_EXPOSURE_COMP_VALUE] = value }
    }

    suspend fun setOfflineScanHud(enabled: Boolean) {
        context.dataStore.edit { it[AppPreferences.OFFLINE_SCAN_HUD] = enabled }
    }

    suspend fun setMapDefaultCoordinates(lat: Double, lng: Double) {
        context.dataStore.edit { prefs ->
            prefs[AppPreferences.MAP_DEFAULT_LATITUDE] = lat.toString()
            prefs[AppPreferences.MAP_DEFAULT_LONGITUDE] = lng.toString()
        }
    }

    suspend fun setMapDefaultZoom(zoom: Float) {
        context.dataStore.edit { prefs ->
            prefs[AppPreferences.MAP_DEFAULT_ZOOM] = zoom
        }
    }

    suspend fun setMapLastLayerType(type: Int) {
        context.dataStore.edit { prefs ->
            prefs[AppPreferences.MAP_LAST_LAYER_TYPE] = type
        }
    }

    suspend fun setScannerService(service: Int) {
        context.dataStore.edit { it[AppPreferences.SCANNER_SERVICE] = service }
    }

    suspend fun setImageSaveFormat(format: Int) {
        context.dataStore.edit { it[AppPreferences.IMAGE_SAVE_FORMAT] = format }
    }

    suspend fun setVideoContainerFormat(format: Int) {
        context.dataStore.edit { it[AppPreferences.VIDEO_CONTAINER_FORMAT] = format }
    }

    suspend fun resetLauncherConfig() {
        context.dataStore.edit { preferences ->
            preferences.remove(AppPreferences.LAUNCHER_APPLET_ORDER)
            preferences.remove(AppPreferences.LAUNCHER_ACTIVE_APPLETS)
            preferences[AppPreferences.STARTUP_DEFAULT_APPLET] = "hub"
        }
    }

    suspend fun exportSettings(uri: Uri) {
        val preferences = context.dataStore.data.first()
        val prefsJson = JSONObject()
        preferences.asMap().forEach { (key, value) ->
            prefsJson.put(key.name, value)
        }

        val json = JSONObject()
        json.put("preferences", prefsJson)

        // 1. Export agenda_hub.db
        val agendaHelper = AgendaDatabaseHelper(context)
        val agendaDb = agendaHelper.readableDatabase
        try {
            json.put("agenda_events", exportTable(agendaDb, "agenda_events"))
            json.put("alarms", exportTable(agendaDb, "alarms"))
            json.put("agenda_cron_jobs", exportTable(agendaDb, "cron_jobs"))
            json.put("agenda_cron_logs", exportTable(agendaDb, "cron_logs"))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            agendaDb.close()
        }

        // 2. Export room_cron_jobs
        val roomDb = CronJobDatabase.getDatabase(context)
        try {
            val jobs = roomDb.cronJobDao().getAllJobs()
            val jobsArray = JSONArray()
            jobs.forEach { job ->
                jobsArray.put(cronJobToJSON(job))
            }
            json.put("room_cron_jobs", jobsArray)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        context.contentResolver.openOutputStream(uri)?.use {
            it.write(json.toString(4).toByteArray())
        }
    }

    private fun exportTable(db: android.database.sqlite.SQLiteDatabase, tableName: String): JSONArray {
        val jsonArray = JSONArray()
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT * FROM \"$tableName\"", null)
            val columnNames = cursor.columnNames
            if (cursor.moveToFirst()) {
                do {
                    val obj = JSONObject()
                    for (i in columnNames.indices) {
                        val colName = columnNames[i]
                        when (cursor.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> obj.put(colName, JSONObject.NULL)
                            Cursor.FIELD_TYPE_INTEGER -> obj.put(colName, cursor.getLong(i))
                            Cursor.FIELD_TYPE_FLOAT -> obj.put(colName, cursor.getDouble(i))
                            Cursor.FIELD_TYPE_STRING -> obj.put(colName, cursor.getString(i))
                            Cursor.FIELD_TYPE_BLOB -> { /* BLOB support optional, omitted for now */ }
                        }
                    }
                    jsonArray.put(obj)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return jsonArray
    }

    private fun cronJobToJSON(job: CronJobEntity): JSONObject {
        return JSONObject().apply {
            put("id", job.id)
            put("jobType", job.jobType)
            put("intervalMinutes", job.intervalMinutes)
            put("isEnabled", job.isEnabled)
            put("requiresNetwork", job.requiresNetwork)
            put("requiresCharging", job.requiresCharging)
            put("lastRunTimestamp", job.lastRunTimestamp)
            put("nextRunTimestamp", job.nextRunTimestamp)
            put("downloadUrl", job.downloadUrl ?: JSONObject.NULL)
            put("saveFileName", job.saveFileName ?: JSONObject.NULL)
        }
    }

    private fun jsonToCronJob(obj: JSONObject): CronJobEntity {
        return CronJobEntity(
            id = obj.getString("id"),
            jobType = obj.getString("jobType"),
            intervalMinutes = obj.getInt("intervalMinutes"),
            isEnabled = obj.getBoolean("isEnabled"),
            requiresNetwork = obj.optBoolean("requiresNetwork", false),
            requiresCharging = obj.optBoolean("requiresCharging", false),
            lastRunTimestamp = obj.optLong("lastRunTimestamp", 0L),
            nextRunTimestamp = obj.optLong("nextRunTimestamp", 0L),
            downloadUrl = if (obj.isNull("downloadUrl")) null else obj.getString("downloadUrl"),
            saveFileName = if (obj.isNull("saveFileName")) null else obj.getString("saveFileName")
        )
    }

    private fun importTable(db: android.database.sqlite.SQLiteDatabase, tableName: String, jsonArray: JSONArray) {
        db.execSQL("DELETE FROM \"$tableName\"")
        for (idx in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(idx)
            val values = ContentValues()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (obj.isNull(key)) {
                    values.putNull(key)
                } else {
                    when (val item = obj.get(key)) {
                        is Int -> values.put(key, item)
                        is Long -> values.put(key, item)
                        is Double -> values.put(key, item)
                        is Boolean -> values.put(key, if (item) 1 else 0)
                        is String -> values.put(key, item)
                    }
                }
            }
            db.insert(tableName, null, values)
        }
    }

    suspend fun importSettings(uri: Uri) {
        try {
            val jsonText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return
            val json = JSONObject(jsonText)
            
            // Check consolidated format vs legacy flat format
            val hasPreferencesObject = json.has("preferences")
            val prefsToImport = if (hasPreferencesObject) {
                json.getJSONObject("preferences")
            } else {
                json
            }

            // Restore preferences DataStore
            context.dataStore.edit { preferences ->
                val keys = prefsToImport.keys()
                while (keys.hasNext()) {
                    val keyName = keys.next()
                    when (val value = prefsToImport.get(keyName)) {
                        is Int -> preferences[intPreferencesKey(keyName)] = value
                        is Boolean -> preferences[booleanPreferencesKey(keyName)] = value
                        is String -> preferences[stringPreferencesKey(keyName)] = value
                        is Float -> preferences[floatPreferencesKey(keyName)] = value
                        is Double -> preferences[doublePreferencesKey(keyName)] = value
                        is Long -> preferences[longPreferencesKey(keyName)] = value
                    }
                }
            }

            // Restore SQLite / AgendaDatabaseHelper databases
            if (hasPreferencesObject) {
                val agendaHelper = AgendaDatabaseHelper(context)
                val agendaDb = agendaHelper.writableDatabase
                try {
                    if (json.has("agenda_events")) {
                        importTable(agendaDb, "agenda_events", json.getJSONArray("agenda_events"))
                    }
                    if (json.has("alarms")) {
                        importTable(agendaDb, "alarms", json.getJSONArray("alarms"))
                    }
                    if (json.has("agenda_cron_jobs")) {
                        importTable(agendaDb, "cron_jobs", json.getJSONArray("agenda_cron_jobs"))
                    }
                    if (json.has("agenda_cron_logs")) {
                        importTable(agendaDb, "cron_logs", json.getJSONArray("agenda_cron_logs"))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    agendaDb.close()
                }

                // Restore Room / CronJobDatabase database
                if (json.has("room_cron_jobs")) {
                    val roomDb = CronJobDatabase.getDatabase(context)
                    try {
                        roomDb.openHelper.writableDatabase.execSQL("DELETE FROM cron_jobs")
                        val jobsArray = json.getJSONArray("room_cron_jobs")
                        for (idx in 0 until jobsArray.length()) {
                            val jobObj = jobsArray.getJSONObject(idx)
                            val job = jsonToCronJob(jobObj)
                            roomDb.cronJobDao().insertJob(job)
                        }
                        // Re-sync imported cronjobs to WorkManager
                        CronJobScheduler.syncJobsFromDatabase(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
