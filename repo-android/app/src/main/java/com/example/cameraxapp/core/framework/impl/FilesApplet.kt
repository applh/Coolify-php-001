package com.example.cameraxapp.core.framework.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.cameraxapp.core.framework.Applet
import com.example.cameraxapp.ExplorerScreen

class FilesApplet : Applet {
    override val id: String = "files"
    override val name: String = "Files"
    override val description: String = "Browse local files"
    override val icon = Icons.Default.Menu

    @Composable
    override fun Content(
        navController: NavController,
        onOpenDrawer: () -> Unit,
        onOpenRightDrawer: () -> Unit
    ) {
        ExplorerScreen(
            onBack = { navController.popBackStack() },
            onOpenDrawer = onOpenDrawer,
            onOpenRightDrawer = onOpenRightDrawer
        )
    }
}
