package com.example.cameraxapp.core.framework.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.cameraxapp.core.framework.Applet
import com.example.cameraxapp.roguelike.RoguelikeScreen
import com.example.cameraxapp.roguelike.RoguelikeViewModel

class RoguelikeApplet : Applet {
    override val id: String = "roguecompose"
    override val name: String = "MORIA"
    override val description: String = "Symmetrical turn-based procedural rogue-like RPG adventure with SQLite persistent saving."
    override val icon = Icons.Default.Star

    @Composable
    override fun Content(
        navController: NavController,
        onOpenDrawer: () -> Unit,
        onOpenRightDrawer: () -> Unit
    ) {
        val context = LocalContext.current
        val viewModel = remember { RoguelikeViewModel(context) }
        RoguelikeScreen(
            onBack = { navController.popBackStack() },
            viewModel = viewModel
        )
    }
}
