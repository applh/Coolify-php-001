package com.example.cameraxapp.core.di

import android.content.Context
import com.example.cameraxapp.AgendaDatabaseHelper
import com.example.cameraxapp.SettingsRepository
import com.example.cameraxapp.browser.BrowserDatabaseHelper
import com.example.cameraxapp.browser.BrowserDownloadManager

/**
 * A central, lifecycle-scoped Dependency Container implementing the Service Locator pattern.
 * Provides thread-safe lazy-cached access to singletons like Helpers and Repositories.
 */
class AppDependencyContainer(private val context: Context) {
    val appContext: Context = context.applicationContext

    val agendaDatabaseHelper: AgendaDatabaseHelper by lazy {
        AgendaDatabaseHelper(appContext)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    val browserDatabaseHelper: BrowserDatabaseHelper by lazy {
        BrowserDatabaseHelper(appContext)
    }

    val browserDownloadManager: BrowserDownloadManager by lazy {
        BrowserDownloadManager(appContext)
    }
}
