package com.example.cameraxapp.core.framework

import android.content.Context
import androidx.compose.runtime.mutableStateListOf

object AppletRegistry {
    private val _registeredApplets = mutableStateListOf<Applet>()
    val registeredApplets: List<Applet> get() = _registeredApplets

    /**
     * Registers an applet into the global pipeline during the cold launch sequence.
     */
    fun register(applet: Applet) {
        if (_registeredApplets.none { it.id == applet.id }) {
            _registeredApplets.add(applet)
        }
    }

    /**
     * Programmatic query returning a snapshot list of active, enabled applets.
     * Integrates with user selection filters inside Settings screen.
     */
    fun getActiveApplets(activeIds: Set<String>): List<Applet> {
        if (activeIds.isEmpty()) return _registeredApplets
        // Settings applet is marked immutable and protected so users are never locked out
        return _registeredApplets.filter { activeIds.contains(it.id) || it.id == "settings" }
    }

    /**
     * Dispatches background scheduling execution blocks to active applets.
     */
    fun dispatchCronTriggers(context: Context) {
        _registeredApplets.forEach { applet ->
            try {
                applet.onBackgroundCronTrigger(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
