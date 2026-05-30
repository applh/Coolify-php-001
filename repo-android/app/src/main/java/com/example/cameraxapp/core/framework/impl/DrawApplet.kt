package com.example.cameraxapp.core.framework.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.cameraxapp.draw.DrawScreen
import com.example.cameraxapp.draw.DrawViewModel
import com.example.cameraxapp.core.framework.Applet

class DrawApplet : Applet {
    override val id: String = "draw_studio"
    override val name: String = "Studio Draw"
    override val description: String = "Professional GIMP-style layered drawing board with download engines"
    override val icon = Icons.Default.Create

    @Composable
    override fun Content(
        navController: NavController,
        onOpenDrawer: () -> Unit,
        onOpenRightDrawer: () -> Unit
    ) {
        val context = LocalContext.current
        val viewModel = remember { DrawViewModel(context) }
        DrawScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onOpenDrawer = onOpenDrawer,
            onOpenRightDrawer = onOpenRightDrawer
        )
    }
}
