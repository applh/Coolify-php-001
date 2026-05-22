package com.example.cameraxapp.cronjob

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HeadlessLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    init {
        registry.currentState = Lifecycle.State.INITIALIZED
    }

    fun start() {
        registry.currentState = Lifecycle.State.STARTED
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

object CameraCaptureManager {
    suspend fun captureImage(context: Context): File? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val imageCapture = ImageCapture.Builder().build()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    val lifecycleOwner = HeadlessLifecycleOwner()
                    lifecycleOwner.start()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageCapture
                    )

                    val outputDir = File(context.filesDir, "cron_photos").apply { mkdirs() }
                    val photoFile = File(outputDir, "capture_${System.currentTimeMillis()}.jpg")

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    val executor = Executors.newSingleThreadExecutor()

                    imageCapture.takePicture(
                        outputOptions,
                        executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                // Must tear down from main thread
                                ContextCompat.getMainExecutor(context).execute {
                                    lifecycleOwner.stop()
                                    cameraProvider.unbindAll()
                                }
                                executor.shutdown()
                                continuation.resume(photoFile)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraCaptureManager", "Photo capture failed: \${exception.message}", exception)
                                ContextCompat.getMainExecutor(context).execute {
                                    lifecycleOwner.stop()
                                    cameraProvider.unbindAll()
                                }
                                executor.shutdown()
                                continuation.resume(null)
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("CameraCaptureManager", "Failed to bind CameraX use cases", e)
                    continuation.resume(null)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
}
