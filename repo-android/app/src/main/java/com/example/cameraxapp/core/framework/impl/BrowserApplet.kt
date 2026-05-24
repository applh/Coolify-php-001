package com.example.cameraxapp.core.framework.impl

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cameraxapp.core.di.AppDependencyContainer
import com.example.cameraxapp.core.di.FeatureViewModelFactory
import com.example.cameraxapp.core.framework.Applet
import com.example.cameraxapp.browser.BrowserScreen
import com.example.cameraxapp.browser.BrowserViewModel

class BrowserApplet(private val container: AppDependencyContainer) : Applet {
    override val id: String = "browser"
    override val name: String = "Browser"
    override val description: String = "Web tools with safe JS sandbox script injection"
    override val icon = Icons.Default.Search

    @Composable
    override fun Content(
        navController: NavController,
        onOpenDrawer: () -> Unit,
        onOpenRightDrawer: () -> Unit
    ) {
        val factory = FeatureViewModelFactory(container)
        val browserViewModel: BrowserViewModel = viewModel(factory = factory)
        val geminiApiKeySaved by container.settingsRepository.geminiApiKey.collectAsState(initial = "")

        BrowserScreen(
            viewModel = browserViewModel,
            apiKey = geminiApiKeySaved,
            onBackToHub = { navController.popBackStack() },
            modifier = Modifier.fillMaxSize()
        )
    }
}
