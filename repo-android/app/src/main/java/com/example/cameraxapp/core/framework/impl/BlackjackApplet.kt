package com.example.cameraxapp.core.framework.impl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.cameraxapp.blackjack.BlackjackScreen
import com.example.cameraxapp.blackjack.BlackjackViewModel
import com.example.cameraxapp.core.framework.Applet

class BlackjackApplet : Applet {
    override val id: String = "blackjack"
    override val name: String = "Blackjack"
    override val description: String = "Casino-grade offline Blackjack with Basic Strategy Helper Coach HUD"
    override val icon = Icons.Default.Star

    @Composable
    override fun Content(
        navController: NavController,
        onOpenDrawer: () -> Unit,
        onOpenRightDrawer: () -> Unit
    ) {
        val context = LocalContext.current
        val viewModel = remember { BlackjackViewModel(context) }
        BlackjackScreen(
            onBack = { navController.popBackStack() },
            viewModel = viewModel
        )
    }
}
