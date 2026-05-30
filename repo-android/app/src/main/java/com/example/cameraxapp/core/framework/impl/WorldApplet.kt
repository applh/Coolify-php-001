package com.example.cameraxapp.core.framework.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.cameraxapp.core.framework.Applet
import com.example.cameraxapp.world.WorldScreen
import com.example.cameraxapp.world.WorldViewModel

class WorldApplet : Applet {
    override val id: String = "world_globe"
    override val name: String = "World Globe"
    override val description: String = "Interactive 3D Virtual globe with custom texture mapping support."
    override val icon = Icons.Default.Star

    @Composable
    override fun Content(
        navController: NavController,
        onOpenDrawer: () -> Unit,
        onOpenRightDrawer: () -> Unit
    ) {
        val context = LocalContext.current
        val viewModel = remember { WorldViewModel(context) }
        WorldScreen(
            onBack = { navController.popBackStack() },
            viewModel = viewModel
        )
    }
}
