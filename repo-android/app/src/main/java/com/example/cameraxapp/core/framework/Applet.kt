package com.example.cameraxapp.core.framework

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController

interface Applet {
    /**
     * Unique developer-facing string identifier mapped to NavHost routes (e.g., "camera_x_applet").
     */
    val id: String

    /**
     * Plaintext human-readable title shown inside launchers, Quick Drawers, and headers.
     */
    val name: String

    /**
     * Description explaining applet scope to end-users inside Settings toggles and launchers.
     */
    val description: String

    /**
     * Vector icon mapped to the specific applet inside drawers and hubs.
     */
    val icon: ImageVector

    /**
     * Set of Android manifest permissions required before launching the applet.
     * The core framework automatically halts transition and displays permission gates if missing.
     */
    val requiredPermissions: List<String>
        get() = emptyList()

    /**
     * Hook triggered by MainActivity on onCreate() initialization.
     * Allows applets to bootstrap background connections, database caches, or receivers.
     */
    fun onInitialize(context: Context) {}

    /**
     * Dynamic Jetpack Compose painter block representing the primary screen UI layout.
     */
    @Composable
    fun Content(
        navController: NavController,
        onOpenDrawer: () -> Unit,
        onOpenRightDrawer: () -> Unit
    )

    /**
     * Hook triggered when a global background Cron-Job wakes up this applet's routine.
     */
    fun onBackgroundCronTrigger(context: Context) {}

    /**
     * Hook triggered when the application lifecycle is shutting down or clear-caches are requested.
     */
    fun onDestroy() {}
}
