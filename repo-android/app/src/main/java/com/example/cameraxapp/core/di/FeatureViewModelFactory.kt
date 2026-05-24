package com.example.cameraxapp.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cameraxapp.browser.BrowserViewModel

/**
 * Standardized ViewModel provider factory allowing the platform to instantiate view models
 * using appropriate clean architecture parameters from the DI container.
 */
class FeatureViewModelFactory(
    private val container: AppDependencyContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(BrowserViewModel::class.java) -> {
                BrowserViewModel(
                    dbHelper = container.browserDatabaseHelper,
                    downloadsManager = container.browserDownloadManager
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
