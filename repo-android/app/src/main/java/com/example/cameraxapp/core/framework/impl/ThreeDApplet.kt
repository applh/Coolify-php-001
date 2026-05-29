package com.example.cameraxapp.core.framework.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.cameraxapp.core.framework.Applet
import com.example.cameraxapp.threed.ThreeDWorkspaceScreen

class ThreeDApplet : Applet {
    override val id: String = "threed_sandbox"
    override val name: String = "3D Workspace"
    override val description: String = "Interactive 3D structural, molecular, topographic and mechanical engineering workbench with local configurations."
    override val icon = Icons.Default.Build

    @Composable
    override fun Content(
        navController: NavController,
        onOpenDrawer: () -> Unit,
        onOpenRightDrawer: () -> Unit
    ) {
        ThreeDWorkspaceScreen(
            onBack = { navController.popBackStack() },
            onOpenDrawer = onOpenDrawer,
            onOpenRightDrawer = onOpenRightDrawer
        )
    }
}
