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
import androidx.compose.ui.graphics.drawscope.withTransform
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.math.atan2
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(onBack: () -> Unit, onOpenDrawer: () -> Unit, onOpenRightDrawer: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    
    val initialLensFacing = runBlocking { repository.defaultLensFacing.first() }
    val initialFlashMode = runBlocking { repository.defaultFlashMode.first() }
    val storageLocation by repository.storageLocation.collectAsState(initial = 0)
    val videoQuality by repository.videoQuality.collectAsState(initial = 4)
    val enableAudio by repository.enableAudio.collectAsState(initial = true)

    val coroutineScope = rememberCoroutineScope()

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
    
    val showGrid by repository.showGrid.collectAsState(initial = false)
    val showCrosshair by repository.showCrosshair.collectAsState(initial = true)
    val gridRows by repository.gridRows.collectAsState(initial = 3)
    val gridColumns by repository.gridColumns.collectAsState(initial = 3)
    var rollAngle by remember { mutableStateOf(0f) }

    // Advanced modular upgrades states
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var selectedFilter by remember { mutableStateOf("NORMAL") }
    var scannedBarcode by remember { mutableStateOf<String?>(null) }
    var showScanResultDialog by remember { mutableStateOf(false) }
    var isScanningPaused by remember { mutableStateOf(false) }

    val cameraExtension by repository.cameraExtension.collectAsState(initial = 0)
    val concurrentStream by repository.concurrentStream.collectAsState(initial = false)
    val proControlMode by repository.proControlMode.collectAsState(initial = false)
    val proIsoValue by repository.proIsoValue.collectAsState(initial = 0)
    val proExposureCompValue by repository.proExposureCompValue.collectAsState(initial = 0)
    val offlineScanHud by repository.offlineScanHud.collectAsState(initial = true)

    var activeEvCorrection by remember { mutableStateOf(0) }
    var activeIsoSensitivity by remember { mutableStateOf(0) }

    LaunchedEffect(proExposureCompValue) {
        activeEvCorrection = proExposureCompValue
    }
    LaunchedEffect(proIsoValue) {
        activeIsoSensitivity = proIsoValue
    }

    LaunchedEffect(activeEvCorrection, cameraInstance) {
        try {
            cameraInstance?.cameraControl?.setExposureCompensationIndex(activeEvCorrection)
        } catch (e: Exception) {
            Log.w("CameraScreen", "Failed to set exposure compensation index: ", e)
        }
    }

    // Manual, compile-safe observer for CameraX zoomState LiveData
    val zoomStateFlow = cameraInstance?.cameraInfo?.zoomState
    var zoomState by remember { mutableStateOf<androidx.camera.core.ZoomState?>(null) }
    DisposableEffect(zoomStateFlow) {
        val observer = androidx.lifecycle.Observer<androidx.camera.core.ZoomState> { state ->
            zoomState = state
        }
        zoomStateFlow?.observeForever(observer)
        onDispose {
            zoomStateFlow?.removeObserver(observer)
        }
    }

    // Google ML Kit QR/Barcode Analyzer configuration
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    LaunchedEffect(imageAnalysis, isScanningPaused, captureMode) {
        if (captureMode == "SCAN" && !isScanningPaused) {
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
            val scanner = BarcodeScanning.getClient(options)
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            if (barcodes.isNotEmpty() && !isScanningPaused) {
                                val barcodeValue = barcodes[0].rawValue ?: barcodes[0].displayValue
                                if (barcodeValue != null) {
                                    scannedBarcode = barcodeValue
                                    isScanningPaused = true
                                    showScanResultDialog = true
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("CameraScreen", "Offline ML Kit scan error", e)
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }
        } else {
            imageAnalysis.clearAnalyzer()
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val gravitySensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        
        val sensorEventListener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                if (event == null) return
                val x = event.values[0]
                val y = event.values[1]
                
                val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    context.display?.rotation ?: android.view.Surface.ROTATION_0
                } else {
                    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.rotation
                }
                
                var adjustedX = 0f
                var adjustedY = 0f
                when (rotation) {
                    android.view.Surface.ROTATION_0 -> { adjustedX = x; adjustedY = y }
                    android.view.Surface.ROTATION_90 -> { adjustedX = -y; adjustedY = x }
                    android.view.Surface.ROTATION_180 -> { adjustedX = -x; adjustedY = -y }
                    android.view.Surface.ROTATION_270 -> { adjustedX = y; adjustedY = -x }
                }
                
                rollAngle = Math.toDegrees(atan2(adjustedX.toDouble(), adjustedY.toDouble())).toFloat()
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }
        
        if (gravitySensor != null) {
            sensorManager.registerListener(
                sensorEventListener,
                gravitySensor,
                android.hardware.SensorManager.SENSOR_DELAY_UI
            )
        }
        
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    LaunchedEffect(flashModeState) {
        imageCapture.flashMode = when(flashModeState) {
            1 -> ImageCapture.FLASH_MODE_ON
            0 -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
    }

    Scaffold(
        bottomBar = {
            TopAppBar(
                title = { 
                    Text(
                        when(captureMode) {
                            "PHOTO" -> "Camera"
                            "VIDEO" -> "Video Recorder"
                            "SCAN" -> "QR Code Scanner"
                            else -> "Camera"
                        }
                    )
                },
                navigationIcon = {
                    Row {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenRightDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Quick Tools")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(cameraInstance) {
                    detectTransformGestures { _, _, zoomFactor, _ ->
                        cameraInstance?.let { camera ->
                            val zState = camera.cameraInfo.zoomState.value
                            if (zState != null) {
                                val currentVal = zState.zoomRatio
                                val minVal = zState.minZoomRatio
                                val maxVal = zState.maxZoomRatio
                                val targetVal = (currentVal * zoomFactor).coerceIn(minVal, maxVal)
                                camera.cameraControl.setZoomRatio(targetVal)
                            }
                        }
                    }
                }
        ) {
            val isPortrait = maxWidth < maxHeight
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                imageCapture = imageCapture,
                videoCapture = videoCapture,
                imageAnalysis = imageAnalysis,
                captureMode = captureMode,
                lensFacing = lensFacing,
                cameraExtension = cameraExtension,
                onCameraConfigured = { camera ->
                    cameraInstance = camera
                }
            )

            // Concurrent Picture-in-Picture secondary camera stream overlay
            if (concurrentStream) {
                val otherLensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
                
                var pipBindFailed by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = if (isPortrait) 160.dp else 48.dp, end = 24.dp)
                        .size(110.dp, 160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!pipBindFailed) {
                        CameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            imageCapture = remember { ImageCapture.Builder().build() },
                            videoCapture = null,
                            imageAnalysis = null,
                            captureMode = "PHOTO",
                            lensFacing = otherLensFacing,
                            cameraExtension = 0,
                            onCameraConfigured = {},
                            onConfigError = { 
                                pipBindFailed = true 
                            }
                        )
                    } else {
                        // High-fidelity resilient PiP simulated mirroring layout
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFF202020)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = if (otherLensFacing == CameraSelector.LENS_FACING_FRONT) "👨 Front Cam" else "🏞️ Back Cam",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Simulated PiP Stream",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyExtraSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Real-time HUD manual slider knobs layout for Pro Mode
            if (proControlMode && captureMode != "SCAN") {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                        .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(20.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("PRO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    
                    // Live EV Adjust Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (activeEvCorrection == 0) "EV: 0" else if (activeEvCorrection > 0) "EV: +$activeEvCorrection" else "EV: $activeEvCorrection",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyExtraSmall
                        )
                        IconButton(
                            onClick = {
                                activeEvCorrection = if (activeEvCorrection >= 3) -3 else activeEvCorrection + 1
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("🔆", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Live ISO Adjust Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when(activeIsoSensitivity) {
                                0 -> "ISO: Auto"
                                else -> "ISO: $activeIsoSensitivity"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.bodyExtraSmall
                        )
                        IconButton(
                            onClick = {
                                activeIsoSensitivity = when(activeIsoSensitivity) {
                                    0 -> 100
                                    100 -> 200
                                    200 -> 400
                                    400 -> 800
                                    800 -> 1600
                                    else -> 0
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("⚡", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Dynamic Atmospheric Tint Filters (Instant GPU Simulation)
            if (captureMode != "SCAN" && selectedFilter != "NORMAL") {
                val overlayColor = when (selectedFilter) {
                    "GRAYSCALE" -> Color(0xFF555555).copy(alpha = 0.15f)
                    "SEPIA" -> Color(0xFF8B4F1D).copy(alpha = 0.22f)
                    "INVERT" -> Color(0xFF00FFCC).copy(alpha = 0.12f)
                    "WARM" -> Color(0xFFFF9E00).copy(alpha = 0.16f)
                    else -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                )
            }

            // Elegant high-visibility Viewfinder Reticle for Quick Scanning
            if (captureMode == "SCAN" && offlineScanHud) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Center Reticle Dimensions
                    val boxSize = 250.dp.toPx()
                    val left = (canvasWidth - boxSize) / 2f
                    val top = (canvasHeight - boxSize) / 2f
                    val right = left + boxSize
                    val bottom = top + boxSize
                    
                    // Translucent Dim Outer Mask Shapes
                    drawRect(color = Color.Black.copy(alpha = 0.5f), size = androidx.compose.ui.geometry.Size(canvasWidth, top))
                    drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, bottom), size = androidx.compose.ui.geometry.Size(canvasWidth, canvasHeight - bottom))
                    drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, top), size = androidx.compose.ui.geometry.Size(left, boxSize))
                    drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(right, top), size = androidx.compose.ui.geometry.Size(canvasWidth - right, boxSize))
                    
                    // High-Contrast Neon Bracket Corner Accents (4px Green)
                    val stroke = 4.dp.toPx()
                    val cornerLen = 24.dp.toPx()
                    val neonColor = Color(0xFF00FFCC)
                    
                    // Top-Left Neon Bracket
                    drawLine(color = neonColor, start = Offset(left, top), end = Offset(left + cornerLen, top), strokeWidth = stroke)
                    drawLine(color = neonColor, start = Offset(left, top), end = Offset(left, top + cornerLen), strokeWidth = stroke)
                    
                    // Top-Right Neon Bracket
                    drawLine(color = neonColor, start = Offset(right, top), end = Offset(right - cornerLen, top), strokeWidth = stroke)
                    drawLine(color = neonColor, start = Offset(right, top), end = Offset(right, top + cornerLen), strokeWidth = stroke)
                    
                    // Bottom-Left Neon Bracket
                    drawLine(color = neonColor, start = Offset(left, bottom), end = Offset(left + cornerLen, bottom), strokeWidth = stroke)
                    drawLine(color = neonColor, start = Offset(left, bottom), end = Offset(left, bottom - cornerLen), strokeWidth = stroke)
                    
                    // Bottom-Right Neon Bracket
                    drawLine(color = neonColor, start = Offset(right, bottom), end = Offset(right - cornerLen, bottom), strokeWidth = stroke)
                    drawLine(color = neonColor, start = Offset(right, bottom), end = Offset(right, bottom - cornerLen), strokeWidth = stroke)
                }
            }

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
                    
                    for (i in 1 until gridColumns) {
                        val x = canvasWidth * i / gridColumns
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(x, 0f),
                            end = Offset(x, canvasHeight),
                            strokeWidth = 2f
                        )
                    }
                    
                    for (i in 1 until gridRows) {
                        val y = canvasHeight * i / gridRows
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 2f
                        )
                    }
                }
            }

            // Crosshair, circle and horizon level
            if (showCrosshair) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                    
                    // Draw circle
                    val circleRadius = 40.dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = circleRadius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                    
                    // Draw crosshair (center dot or small cross)
                    val crosshairLength = 8.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(center.x - crosshairLength, center.y),
                        end = Offset(center.x + crosshairLength, center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(center.x, center.y - crosshairLength),
                        end = Offset(center.x, center.y + crosshairLength),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    // Draw horizon level
                    withTransform({
                        rotate(degrees = rollAngle, pivot = center)
                    }) {
                        val horizonLength = canvasWidth * 0.3f
                        val gap = circleRadius + 16.dp.toPx()
                        // Left segment
                        drawLine(
                            color = Color.Yellow.copy(alpha = 0.8f),
                            start = Offset(center.x - gap - horizonLength, center.y),
                            end = Offset(center.x - gap, center.y),
                            strokeWidth = 2.dp.toPx()
                        )
                        // Right segment
                        drawLine(
                            color = Color.Yellow.copy(alpha = 0.8f),
                            start = Offset(center.x + gap, center.y),
                            end = Offset(center.x + gap + horizonLength, center.y),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }

            if (!isPortrait) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 48.dp, start = 32.dp)
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
                }
            }

            // High Precision Zoom Slider HUD
            if (captureMode != "SCAN" && cameraInstance != null) {
                val zState = zoomState
                if (zState != null) {
                    val currentVal = zState.zoomRatio
                    val minVal = zState.minZoomRatio
                    val maxVal = zState.maxZoomRatio
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (isPortrait) 112.dp else 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Quick Presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(1f, 2f, 4f).forEach { scaleTarget ->
                                if (scaleTarget in minVal..maxVal) {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (currentVal == scaleTarget) MaterialTheme.colorScheme.primaryContainer else Color.Black.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier.clickable {
                                            cameraInstance?.cameraControl?.setZoomRatio(scaleTarget)
                                        }
                                    ) {
                                        Text(
                                            text = "${scaleTarget.toInt()}x",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Precision Control
                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom HUD", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Slider(
                                value = currentVal,
                                onValueChange = { targetScale ->
                                    cameraInstance?.cameraControl?.setZoomRatio(targetScale)
                                },
                                valueRange = minVal..maxVal,
                                modifier = Modifier.width(160.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.DarkGray
                                )
                            )
                            Text(
                                text = String.format(Locale.US, "%.1fx", currentVal),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            // Google ML Kit Offline Scanned Code Interactive Bottom Sheet
            if (showScanResultDialog && scannedBarcode != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "QR/Barcode Scanned",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 100.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Box(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                                Text(
                                    text = scannedBarcode ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Scanned Barcode", scannedBarcode)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied text to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("Copy", style = MaterialTheme.typography.labelMedium)
                            }
                            
                            val isLink = scannedBarcode?.startsWith("http://") == true || scannedBarcode?.startsWith("https://") == true
                            if (isLink) {
                                Button(
                                    onClick = {
                                        try {
                                            val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(scannedBarcode))
                                            context.startActivity(openIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("Open Url", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            
                            Button(
                                onClick = {
                                    try {
                                        val agendaDb = AgendaDatabaseHelper(context)
                                        agendaDb.insertEvent(
                                            title = "Scanned Event Note",
                                            notes = scannedBarcode ?: "",
                                            dateMillis = System.currentTimeMillis(),
                                            duration = 30,
                                            color = "Secondary"
                                        )
                                        Toast.makeText(context, "Saved to notes database!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Database save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("Save Note", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        
                        TextButton(
                            onClick = {
                                scannedBarcode = null
                                showScanResultDialog = false
                                isScanningPaused = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Rescan / Dismiss", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            if (isPortrait) {
                // Flash, Grid, Mode, Filter Row (Centered at Bottom)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 180.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flash button
                    IconButton(
                        onClick = { flashModeState = (flashModeState + 1) % 3 },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Transparent)
                    ) {
                        Text(
                            text = when(flashModeState) {
                                1 -> "ON"
                                0 -> "OFF"
                                else -> "AUTO"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    // Grid button
                    IconButton(
                        onClick = { coroutineScope.launch { repository.setShowGrid(!showGrid) } },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Transparent)
                    ) {
                        Text(
                            text = if (showGrid) "GRID" else "#",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Advanced Camera/Video/Scan Mode Toggle Cycle
                    IconButton(
                        onClick = { 
                            captureMode = when(captureMode) {
                                "PHOTO" -> "VIDEO"
                                "VIDEO" -> "SCAN"
                                else -> "PHOTO"
                            }
                        },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Transparent)
                    ) {
                        Text(
                            text = when(captureMode) {
                                "PHOTO" -> "📸"
                                "VIDEO" -> "📹"
                                else -> "🔍"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Dynamic Live Film Filters Toggle Key (Cycles Sepia, Grayscale, etc)
                    if (captureMode == "PHOTO") {
                        IconButton(
                            onClick = {
                                selectedFilter = when(selectedFilter) {
                                    "NORMAL" -> "GRAYSCALE"
                                    "GRAYSCALE" -> "SEPIA"
                                    "SEPIA" -> "INVERT"
                                    "INVERT" -> "WARM"
                                    else -> "NORMAL"
                                }
                            },
                            modifier = Modifier.size(44.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Transparent)
                        ) {
                            Text(
                                text = when(selectedFilter) {
                                    "NORMAL" -> "🎨"
                                    "GRAYSCALE" -> "⬜"
                                    "SEPIA" -> "🟫"
                                    "INVERT" -> "🔲"
                                    else -> "🔥"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                // Primary ergonomic control layout: Gallery Thumbnail | Large Capture Trigger | Lens Reverse Switcher
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Thumbnail
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
                                    .clickable { },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.3f))
                            )
                        }
                    }

                    // Centered Camera Trigger
                    IconButton(
                        onClick = {
                            if (captureMode == "PHOTO") {
                                takePhoto(context, imageCapture, ContextCompat.getMainExecutor(context), storageLocation, selectedFilter) { uri ->
                                    lastCapturedImageUri = uri
                                }
                            } else if (captureMode == "VIDEO") {
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
                            } else {
                                // SCAN QR Mode: Press the capture button to reset scanner
                                scannedBarcode = null
                                showScanResultDialog = false
                                isScanningPaused = false
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

                    // Switch Camera Lens Trigger
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) 
                                CameraSelector.LENS_FACING_FRONT 
                            else 
                                CameraSelector.LENS_FACING_BACK
                        },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Switch Camera")
                    }
                }
            } else {
                // Landscape Edge Control Panel
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Flash button
                    IconButton(
                        onClick = { flashModeState = (flashModeState + 1) % 3 },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = when(flashModeState) {
                                1 -> "ON"
                                0 -> "OFF"
                                else -> "AUTO"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    // Grid button
                    IconButton(
                        onClick = { coroutineScope.launch { repository.setShowGrid(!showGrid) } },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = if (showGrid) "GRID" else "#",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Advanced Camera/Video/Scan Mode Toggle Cycle
                    IconButton(
                        onClick = { 
                            captureMode = when(captureMode) {
                                "PHOTO" -> "VIDEO"
                                "VIDEO" -> "SCAN"
                                else -> "PHOTO"
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = when(captureMode) {
                                "PHOTO" -> "📸"
                                "VIDEO" -> "📹"
                                else -> "🔍"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Dynamic Live Film Filters Toggle Key (Cycles Sepia, Grayscale, etc)
                    if (captureMode == "PHOTO") {
                        IconButton(
                            onClick = {
                                selectedFilter = when(selectedFilter) {
                                    "NORMAL" -> "GRAYSCALE"
                                    "GRAYSCALE" -> "SEPIA"
                                    "SEPIA" -> "INVERT"
                                    "INVERT" -> "WARM"
                                    else -> "NORMAL"
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = when(selectedFilter) {
                                    "NORMAL" -> "🎨"
                                    "GRAYSCALE" -> "⬜"
                                    "SEPIA" -> "🟫"
                                    "INVERT" -> "🔲"
                                    else -> "🔥"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Lens switch
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) 
                                CameraSelector.LENS_FACING_FRONT 
                            else 
                                CameraSelector.LENS_FACING_BACK
                        },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Switch Camera")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic Capture Action Button
                    IconButton(
                        onClick = {
                            if (captureMode == "PHOTO") {
                                takePhoto(context, imageCapture, ContextCompat.getMainExecutor(context), storageLocation, selectedFilter) { uri ->
                                    lastCapturedImageUri = uri
                                }
                            } else if (captureMode == "VIDEO") {
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
                            } else {
                                // SCAN QR Mode: Press the capture button to reset scanner
                                scannedBarcode = null
                                showScanResultDialog = false
                                isScanningPaused = false
                            }
                        },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (recording != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Capture",
                            modifier = Modifier.size(32.dp)
                        )
                    }
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

// High-efficiency, zero-leak pixel buffer to Bitmap decoder
private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planes = image.planes
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

// Native Hardware-Accelerated Color Matrix Filtration kernel
private fun applyFilterToBitmap(src: Bitmap, filter: String): Bitmap {
    if (filter == "NORMAL") return src
    val res = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(res)
    val paint = android.graphics.Paint()
    
    val matrix = android.graphics.ColorMatrix()
    when (filter) {
        "GRAYSCALE" -> {
            matrix.setSaturation(0f)
        }
        "SEPIA" -> {
            val sepiaMatrix = android.graphics.ColorMatrix().apply {
                set(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f,      0f,      0f,      1f, 0f
                ))
            }
            matrix.postConcat(sepiaMatrix)
        }
        "INVERT" -> {
            matrix.set(floatArrayOf(
                -1f, 0f,  0f,  0f, 255f,
                0f,  -1f, 0f,  0f, 255f,
                0f,  0f,  -1f, 0f, 255f,
                0f,  0f,  0f,  1f, 0f
            ))
        }
        "WARM" -> {
            matrix.set(floatArrayOf(
                1.22f, 0f,   0f,   0f, 0f,
                0f,   1.02f, 0f,   0f, 0f,
                0f,   0f,   0.78f, 0f, 0f,
                0f,   0f,   0f,   1f, 0f
            ))
        }
    }
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
    canvas.drawBitmap(src, 0f, 0f, paint)
    return res
}

// MediaStore and External storage file sync adapter
private fun saveBitmapToDisk(
    context: Context,
    bitmap: Bitmap,
    name: String,
    storageLocation: Int
): Uri? {
    val resolver = context.contentResolver
    if (storageLocation == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraXApp")
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            uri
        } catch (e: Exception) {
            Log.e("CameraScreen", "Error compressing filtered bitmap to external gallery", e)
            null
        }
    } else {
        val dir = when (storageLocation) {
            1 -> context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)!!
            2 -> {
                val dirs = ContextCompat.getExternalFilesDirs(context, null)
                if (dirs.size > 1) dirs[1] else context.filesDir
            }
            else -> context.filesDir
        }
        val file = File(dir, "$name.jpg")
        return try {
            java.io.FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e("CameraScreen", "Error compressing filtered bitmap to sandboxed storage", e)
            null
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    storageLocation: Int,
    filter: String = "NORMAL",
    onImageSaved: (Uri) -> Unit
) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    
    // Fall back to standard, hyper-optimized execution path if no filter is active
    if (filter == "NORMAL") {
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
    } else {
        // In-memory hardware pixel buffer interception for precision filter rendering
        imageCapture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    try {
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val rawBitmap = imageProxyToBitmap(imageProxy)
                        imageProxy.close()
                        
                        // Rotational normalization adjustment
                        val normalBitmap = if (rotationDegrees != 0) {
                            val rMatrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, rMatrix, true)
                        } else {
                            rawBitmap
                        }
                        
                        // Render final filtered pixel matrix
                        val filteredBitmap = applyFilterToBitmap(normalBitmap, filter)
                        
                        // Save output
                        val savedUri = saveBitmapToDisk(context, filteredBitmap, name, storageLocation)
                        
                        ContextCompat.getMainExecutor(context).execute {
                            if (savedUri != null) {
                                val msg = "Custom filtered photo saved successfully"
                                Log.d("CameraScreen", msg)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                onImageSaved(savedUri)
                            } else {
                                Toast.makeText(context, "Failed to save filtered photo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        imageProxy.close()
                        Log.e("CameraScreen", "In-memory callback filter error: ", e)
                        ContextCompat.getMainExecutor(context).execute {
                            Toast.makeText(context, "Filter rendering failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraScreen", "In-memory interception capture failure", exc)
                    ContextCompat.getMainExecutor(context).execute {
                        Toast.makeText(context, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    imageCapture: ImageCapture,
    videoCapture: androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>?,
    imageAnalysis: ImageAnalysis? = null,
    captureMode: String,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    cameraExtension: Int = 0,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    onCameraConfigured: (Camera) -> Unit = {},
    onConfigError: (() -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var previewUseCase by remember { mutableStateOf<Preview?>(null) }

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
        },
        update = { view ->
            val rot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.context.display?.rotation ?: android.view.Surface.ROTATION_0
            } else {
                val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                @Suppress("DEPRECATION")
                wm.defaultDisplay.rotation
            }
            imageCapture.targetRotation = rot
            videoCapture?.targetRotation = rot
            imageAnalysis?.targetRotation = rot
            previewUseCase?.targetRotation = rot
        }
    )

    LaunchedEffect(previewView, captureMode, lensFacing, cameraExtension, videoCapture, imageAnalysis) {
        val view = previewView ?: return@LaunchedEffect

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(view.surfaceProvider)
                    val rot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.context.display?.rotation ?: android.view.Surface.ROTATION_0
                    } else {
                        val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                        @Suppress("DEPRECATION")
                        wm.defaultDisplay.rotation
                    }
                    it.targetRotation = rot
                }
                previewUseCase = preview

                val baseSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                // Dynamically fetch and negotiate advanced device OEM CameraX Extensions
                val extensionsManagerFuture = androidx.camera.extensions.ExtensionsManager.getInstanceAsync(context, cameraProvider)
                extensionsManagerFuture.addListener({
                    try {
                        val extensionsManager = extensionsManagerFuture.get()
                        val extensionMode = when (cameraExtension) {
                            1 -> androidx.camera.extensions.ExtensionMode.HDR
                            2 -> androidx.camera.extensions.ExtensionMode.BOKEH
                            3 -> androidx.camera.extensions.ExtensionMode.NIGHT
                            4 -> androidx.camera.extensions.ExtensionMode.FACE_RETOUCH
                            else -> -1
                        }

                        val cameraSelector = if (extensionMode != -1 && extensionsManager.isExtensionAvailable(baseSelector, extensionMode)) {
                            extensionsManager.getExtensionEnabledCameraSelector(baseSelector, extensionMode)
                        } else {
                            baseSelector
                        }

                        cameraProvider.unbindAll()
                        val camera = if (captureMode == "PHOTO") {
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
                        } else if (captureMode == "SCAN" && imageAnalysis != null) {
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } else {
                            null
                        }
                        camera?.let { onCameraConfigured(it) }
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Extensions negotiation/binding exception: ", e)
                        onConfigError?.invoke()
                    }
                }, ContextCompat.getMainExecutor(context))

            } catch (exc: Exception) {
                Log.e("CameraPreview", "Retrieved ProcessCameraProvider exception: ", exc)
                onConfigError?.invoke()
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
