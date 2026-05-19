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
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.PendingRecording
import androidx.core.util.Consumer
import androidx.camera.video.FileOutputOptions
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
fun CameraScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    
    val initialLensFacing = runBlocking { repository.defaultLensFacing.first() }
    val initialFlashMode = runBlocking { repository.defaultFlashMode.first() }
    val storageLocation by repository.storageLocation.collectAsState(initial = 0)
    val videoQuality by repository.videoQuality.collectAsState(initial = 4)
    val enableAudio by repository.enableAudio.collectAsState(initial = true)

    val imageCapture = remember { ImageCapture.Builder().build() }
    val videoCapture = remember(videoQuality) {
        val quality = when(videoQuality) {
            0 -> Quality.SD
            1 -> Quality.HD
            2 -> Quality.FHD
            3 -> Quality.UHD
            else -> Quality.HIGHEST
        }
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(quality, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
            .build()
        VideoCapture.withOutput(recorder)
    }
    
    var captureMode by remember { mutableStateOf("PHOTO") }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var recordingDurationNanos by remember { mutableStateOf(0L) }
    
    var lensFacing by remember { mutableStateOf(if (initialLensFacing == 1) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT) }
    var flashModeState by remember { mutableStateOf(initialFlashMode) } // 0: Off, 1: On, 2: Auto
    var lastCapturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showGrid by remember { mutableStateOf(false) }

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
                title = { Text(if (captureMode == "PHOTO") "Camera" else "Video") },
                navigationIcon = {
                    Row {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showGrid = !showGrid }) {
                        Text(
                            text = if (showGrid) "GRID" else "#",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    IconButton(onClick = { captureMode = if (captureMode == "PHOTO") "VIDEO" else "PHOTO" }) {
                        Text(
                            text = if (captureMode == "PHOTO") "🎥" else "📷",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
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
                videoCapture = videoCapture,
                captureMode = captureMode,
                lensFacing = lensFacing
            )

            if (recording != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(Color.Red.copy(alpha = 0.7f), androidx.compose.foundation.shape.CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val minutes = java.util.concurrent.TimeUnit.NANOSECONDS.toMinutes(recordingDurationNanos)
                    val seconds = java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(recordingDurationNanos) -
                                  java.util.concurrent.TimeUnit.MINUTES.toSeconds(minutes)
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (showGrid) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(canvasWidth / 3f, 0f),
                        end = Offset(canvasWidth / 3f, canvasHeight),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(canvasWidth * 2f / 3f, 0f),
                        end = Offset(canvasWidth * 2f / 3f, canvasHeight),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(0f, canvasHeight / 3f),
                        end = Offset(canvasWidth, canvasHeight / 3f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(0f, canvasHeight * 2f / 3f),
                        end = Offset(canvasWidth, canvasHeight * 2f / 3f),
                        strokeWidth = 2f
                    )
                }
            }

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
                        if (captureMode == "PHOTO") {
                            takePhoto(context, imageCapture, ContextCompat.getMainExecutor(context), storageLocation) { uri ->
                                lastCapturedImageUri = uri
                            }
                        } else {
                            if (recording != null) {
                                recording?.stop()
                                recording = null
                            } else {
                                recording = startVideoRecording(
                                    context, 
                                    videoCapture, 
                                    ContextCompat.getMainExecutor(context), 
                                    storageLocation, 
                                    enableAudio,
                                    onVideoSaved = { uri ->
                                        lastCapturedImageUri = uri
                                    },
                                    onDurationUpdate = { nanos ->
                                        recordingDurationNanos = nanos
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (recording != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
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

private fun startVideoRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    executor: Executor,
    storageLocation: Int,
    enableAudio: Boolean,
    onVideoSaved: (Uri) -> Unit,
    onDurationUpdate: (Long) -> Unit
): Recording {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    
    var tempFile: File? = null

    val pendingRecording = if (storageLocation == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraXApp")
        }
        val options = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()
        videoCapture.output.prepareRecording(context, options)
    } else {
        val dir = when (storageLocation) {
            1 -> context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)!!
            2 -> {
                val dirs = ContextCompat.getExternalFilesDirs(context, null)
                if (dirs.size > 1) dirs[1] else context.filesDir
            }
            else -> context.filesDir
        }
        tempFile = File(dir, "$name.mp4")
        val options = FileOutputOptions.Builder(tempFile).build()
        videoCapture.output.prepareRecording(context, options)
    }

    var recordSetup = pendingRecording
    if (enableAudio && ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        recordSetup = recordSetup.withAudioEnabled()
    }

    return recordSetup.start(executor, Consumer { recordEvent ->
        when (recordEvent) {
            is VideoRecordEvent.Start -> {
                Log.d("CameraScreen", "Video recording started")
                onDurationUpdate(0L)
            }
            is VideoRecordEvent.Status -> {
                onDurationUpdate(recordEvent.recordingStats.recordedDurationNanos)
            }
            is VideoRecordEvent.Finalize -> {
                if (!recordEvent.hasError()) {
                    val msg = "Video saved successfully"
                    Log.d("CameraScreen", msg)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    val uri = recordEvent.outputResults.outputUri
                    onVideoSaved(uri)
                } else {
                    Log.e("CameraScreen", "Video recording error: ${recordEvent.error}")
                }
            }
        }
    })
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    storageLocation: Int,
    onImageSaved: (Uri) -> Unit
) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    
    var tempFile: File? = null

    val outputOptions = if (storageLocation == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
        val dir = when (storageLocation) {
            1 -> context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)!!
            2 -> {
                val dirs = ContextCompat.getExternalFilesDirs(context, null)
                if (dirs.size > 1) dirs[1] else context.filesDir
            }
            else -> context.filesDir
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
    videoCapture: androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>?,
    captureMode: String,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                this.scaleType = scaleType
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                previewView = this
            }
        }
    )

    LaunchedEffect(previewView, captureMode, lensFacing, videoCapture) {
        val view = previewView ?: return@LaunchedEffect

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                if (captureMode == "PHOTO") {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } else if (captureMode == "VIDEO" && videoCapture != null) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        videoCapture
                    )
                }
            } catch (exc: Exception) {
                Log.e("CameraPreview", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
