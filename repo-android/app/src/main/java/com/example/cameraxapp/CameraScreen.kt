package com.example.cameraxapp

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    
    val initialLensFacing = runBlocking { repository.defaultLensFacing.first() }
    val initialFlashMode = runBlocking { repository.defaultFlashMode.first() }
    val saveToPublic by repository.saveToPublic.collectAsState(initial = false)

    val imageCapture = remember { ImageCapture.Builder().build() }
    var lensFacing by remember { mutableStateOf(if (initialLensFacing == 1) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT) }
    var flashModeState by remember { mutableStateOf(initialFlashMode) } // 0: Off, 1: On, 2: Auto
    var lastCapturedImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(flashModeState) {
        imageCapture.flashMode = when(flashModeState) {
            1 -> ImageCapture.FLASH_MODE_ON
            0 -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { flashModeState = (flashModeState + 1) % 3 }) {
                        Text(
                            text = when(flashModeState) {
                                1 -> "ON"
                                0 -> "OFF"
                                else -> "AUTO"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                imageCapture = imageCapture,
                lensFacing = lensFacing
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Last Image Thumbnail
                Box(
                    modifier = Modifier.size(56.dp)
                ) {
                    if (lastCapturedImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(lastCapturedImageUri),
                            contentDescription = "Last Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .clickable { }, // Nav to explorer could be here but for now just clickable
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Shutter Button
                IconButton(
                    onClick = {
                        takePhoto(context, imageCapture, ContextCompat.getMainExecutor(context), saveToPublic) { uri ->
                            lastCapturedImageUri = uri
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Capture",
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Lens Switch
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) 
                            CameraSelector.LENS_FACING_FRONT 
                        else 
                            CameraSelector.LENS_FACING_BACK
                    },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Switch Camera")
                }
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    saveToPublic: Boolean,
    onImageSaved: (Uri) -> Unit
) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    
    var tempFile: File? = null

    val outputOptions = if (saveToPublic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraXApp")
        }
        ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
    } else {
        val dir = if (saveToPublic) {
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)!!
        } else {
            context.filesDir
        }
        tempFile = File(dir, "$name.jpg")
        ImageCapture.OutputFileOptions.Builder(tempFile).build()
    }

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val msg = "Photo saved successfully"
                Log.d("CameraScreen", msg)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                val uri = output.savedUri ?: if (tempFile != null) Uri.fromFile(tempFile) else null
                uri?.let { onImageSaved(it) }
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
                Toast.makeText(context, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    imageCapture: ImageCapture,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                this.scaleType = scaleType
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))

            previewView
        }
    )
}
