package com.example.cameraxapp.core.framework

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface AppletEvent

data class DatabaseChangedEvent(val dbName: String, val table: String) : AppletEvent
data class PhotoCapturedEvent(val filePath: String, val timestamp: Long) : AppletEvent
data class CronJobCompletedEvent(val jobId: String, val result: String) : AppletEvent

object AppletEventBus {
    private val _events = MutableSharedFlow<AppletEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    suspend fun publish(event: AppletEvent) {
        _events.emit(event)
    }
}
