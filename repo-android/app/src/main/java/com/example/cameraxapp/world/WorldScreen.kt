package com.example.cameraxapp.world

import android.content.Context
import com.example.cameraxapp.AppLogger
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

// ==========================================
// 1. REACTIVE WORLD VIEW MODEL (TEXTURES STORES)
// ==========================================

class WorldViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext

    // 3D parameters states
    val scale = MutableStateFlow(1.0f)
    val pitch = MutableStateFlow(-0.2f) // RotX
    val yaw = MutableStateFlow(0.0f)   // RotY

    // Sliders & Custom parameters
    val density = MutableStateFlow(24) // Latitude Steps density
    val autoRotate = MutableStateFlow(true)
    val lightIntensity = MutableStateFlow(0.85f)
    val specularShininess = MutableStateFlow(0.35f)
    val wireframeMode = MutableStateFlow(false)
    val useSceneview = MutableStateFlow(false)
    val glbModelUrl = MutableStateFlow("https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/Earth/glTF-Binary/Earth.glb")

    // Network and Custom loading states
    val urlInputValue = MutableStateFlow("")
    val downloadStatus = MutableStateFlow("Idle") // "Idle" | "Downloading..." | "Success" | "Error: <msg>"

    // Prebuilt bitmaps references
    private lateinit var earthBitmapPreset: Bitmap
    private lateinit var cyberGridBitmapPreset: Bitmap
    
    // Loaded custom picture bitmap
    private val _customBitmap = MutableStateFlow<Bitmap?>(null)
    val customBitmap: StateFlow<Bitmap?> = _customBitmap

    // Current selection code (0 = Earth, 1 = Cyber Grid, 2 = Custom Photo)
    val activeTextureType = MutableStateFlow(0)

    // Active bitmap for drawing computation
    private val _activeBitmap = MutableStateFlow<Bitmap?>(null)
    val activeBitmap: StateFlow<Bitmap?> = _activeBitmap

    init {
        // Build preset bitmaps asynchronously inside background workers
        viewModelScope.launch(Dispatchers.Default) {
            earthBitmapPreset = generateEarthPreset()
            cyberGridBitmapPreset = generateCyberGridPreset()
            _activeBitmap.value = earthBitmapPreset
        }

        // Auto rotation engine update loop
        viewModelScope.launch {
            while (true) {
                if (autoRotate.value) {
                    yaw.value = (yaw.value + 0.006f) % (2f * PI.toFloat())
                }
                delay(16)
            }
        }
    }

    fun downloadTextureFromUrl(urlString: String) {
        val trimmed = urlString.trim()
        if (trimmed.isBlank()) {
            downloadStatus.value = "Error: Please enter a non-empty texture URL"
            return
        }
        downloadStatus.value = "Downloading..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL(trimmed)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 12000
                conn.readTimeout = 15000
                conn.connect()
                
                val code = conn.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    downloadStatus.value = "Error: Server returned HTTP Code $code"
                    return@launch
                }
                
                conn.inputStream.use { stream ->
                    val rawBmp = BitmapFactory.decodeStream(stream)
                    if (rawBmp != null) {
                        // Max width budget to keep drawing smooth
                        val resizedBmp = if (rawBmp.width > 1024) {
                            val ratio = 1024f / rawBmp.width
                            val newHt = (rawBmp.height * ratio).toInt()
                            Bitmap.createScaledBitmap(rawBmp, 1024, newHt, true)
                        } else {
                            rawBmp
                        }
                        
                        setCustomTexture(resizedBmp)
                        selectTexture(2)
                        downloadStatus.value = "Success"
                    } else {
                        downloadStatus.value = "Error: Failed to parse photo file as texture bitmap"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                downloadStatus.value = "Error: " + (e.localizedMessage ?: "Network connection failed")
            }
        }
    }

    fun selectTexture(type: Int) {
        activeTextureType.value = type
        viewModelScope.launch(Dispatchers.Default) {
            when (type) {
                0 -> _activeBitmap.value = earthBitmapPreset
                1 -> _activeBitmap.value = cyberGridBitmapPreset
                2 -> _activeBitmap.value = _customBitmap.value ?: earthBitmapPreset
            }
        }
    }

    fun setCustomTexture(bitmap: Bitmap) {
        _customBitmap.value = bitmap
        if (activeTextureType.value == 2) {
            _activeBitmap.value = bitmap
        }
    }

    // Procedural creation of high detail continent maps
    private fun generateEarthPreset(): Bitmap {
        val bitmap = Bitmap.createBitmap(512, 256, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }

        // Planet dark navy space ocean
        paint.color = 0xFF0D1B2A.toInt()
        canvas.drawRect(0f, 0f, 512f, 256f, paint)

        // Depth navigation lines on oceans
        paint.color = 0xFF1B263B.toInt()
        paint.strokeWidth = 1f
        for (x in 0..512 step 32) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), 256f, paint)
        }
        for (y in 0..256 step 32) {
            canvas.drawLine(0f, y.toFloat(), 512f, y.toFloat(), paint)
        }

        // Draw continents standard green pastel
        paint.color = 0xFF2D6A4F.toInt()

        // Americas continent shapes (North America)
        val naPath = android.graphics.Path().apply {
            moveTo(60f, 30f)
            lineTo(175f, 25f)
            lineTo(195f, 65f)
            lineTo(155f, 135f)
            lineTo(125f, 135f)
            lineTo(110f, 95f)
            lineTo(65f, 105f)
            close()
        }
        canvas.drawPath(naPath, paint)

        // Americas continent shapes (South America)
        paint.color = 0xFF40916C.toInt()
        val saPath = android.graphics.Path().apply {
            moveTo(125f, 135f)
            lineTo(155f, 135f)
            lineTo(185f, 175f)
            lineTo(205f, 195f)
            lineTo(165f, 235f)
            lineTo(140f, 205f)
            close()
        }
        canvas.drawPath(saPath, paint)

        // Africa Continent shapes
        paint.color = 0xFF52B788.toInt()
        val africaPath = android.graphics.Path().apply {
            moveTo(215f, 115f)
            lineTo(285f, 115f)
            lineTo(315f, 155f)
            lineTo(285f, 215f)
            lineTo(255f, 215f)
            lineTo(240f, 155f)
            close()
        }
        canvas.drawPath(africaPath, paint)

        // Europe and Asia Continent shapes
        paint.color = 0xFF1B4332.toInt()
        val eurasiaPath = android.graphics.Path().apply {
            moveTo(215f, 115f)
            lineTo(195f, 65f)
            lineTo(235f, 25f)
            lineTo(375f, 15f)
            lineTo(455f, 35f)
            lineTo(475f, 85f)
            lineTo(435f, 135f)
            lineTo(355f, 115f)
            lineTo(295f, 115f)
            close()
        }
        canvas.drawPath(eurasiaPath, paint)

        // Australia island
        paint.color = 0xFF74C69D.toInt()
        val ausPath = android.graphics.Path().apply {
            moveTo(405f, 175f)
            lineTo(455f, 185f)
            lineTo(445f, 215f)
            lineTo(395f, 205f)
            close()
        }
        canvas.drawPath(ausPath, paint)

        // Antarctica (Frost cap bottom)
        paint.color = 0xFFF8F9FA.toInt()
        canvas.drawRect(0f, 235f, 512f, 256f, paint)

        return bitmap
    }

    // Procedural creation of glowing sci-fi coordinates network
    private fun generateCyberGridPreset(): Bitmap {
        val bitmap = Bitmap.createBitmap(512, 256, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }

        // Night cosmic sky black
        paint.color = 0xFF05050A.toInt()
        canvas.drawRect(0f, 0f, 512f, 256f, paint)

        // Neon cyber grid lines
        paint.color = 0xFF00FFCC.toInt()
        paint.strokeWidth = 1.5f
        paint.style = android.graphics.Paint.Style.STROKE

        for (i in 0..256 step 16) {
            canvas.drawLine(0f, i.toFloat(), 512f, i.toFloat(), paint)
        }
        for (j in 0..512 step 16) {
            canvas.drawLine(j.toFloat(), 0f, j.toFloat(), 256f, paint)
        }

        // Connective light nodes inside array
        paint.color = 0xFFFF007F.toInt()
        paint.style = android.graphics.Paint.Style.FILL
        for (i in 0..256 step 32) {
            for (j in 0..512 step 32) {
                canvas.drawCircle(j.toFloat(), i.toFloat(), 2.5f, paint)
            }
        }

        return bitmap
    }
}

// ==========================================
// 2. PRIMARY WORLD 3D SCREEN COMPOSABLE
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldScreen(
    onBack: () -> Unit,
    viewModel: WorldViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Gather settings state flows
    val yawAngle by viewModel.yaw.collectAsState()
    val pitchAngle by viewModel.pitch.collectAsState()
    val scaleFactor by viewModel.scale.collectAsState()

    val globeDensity by viewModel.density.collectAsState()
    val autoRotateEnabled by viewModel.autoRotate.collectAsState()
    val lightFactor by viewModel.lightIntensity.collectAsState()
    val specularValue by viewModel.specularShininess.collectAsState()
    val isWireframeOnly by viewModel.wireframeMode.collectAsState()
    val useSceneviewEnabled by viewModel.useSceneview.collectAsState()
    val glbModelUrlValue by viewModel.glbModelUrl.collectAsState()

    val currentActiveType by viewModel.activeTextureType.collectAsState()
    val currentBitmap by viewModel.activeBitmap.collectAsState()
    val uploadedPhoto by viewModel.customBitmap.collectAsState()

    val urlInputValue by viewModel.urlInputValue.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()

    // Activity picker setup for loading custom phone gallery photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri).use { stream ->
                        val importedBmp = BitmapFactory.decodeStream(stream)
                        if (importedBmp != null) {
                            // Automatically scale down mega-pixel user photos to keep 3D texture mapping high speed
                            val resizedBmp = if (importedBmp.width > 1024) {
                                val ratio = 1024f / importedBmp.width
                                val newHt = (importedBmp.height * ratio).toInt()
                                Bitmap.createScaledBitmap(importedBmp, 1024, newHt, true)
                            } else {
                                importedBmp
                            }
                            viewModel.setCustomTexture(resizedBmp)
                            viewModel.selectTexture(2)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Adapt to responsive tablet vs phone viewports
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMultiSplit = screenWidth >= 660

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("World 3D Globe", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        }
    ) { innerPadding ->
        if (isMultiSplit) {
            // Horizontal Split Row: Left = 3D Sphere Canvas, Right = Floating Settings Drawer Panel
            Row(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1.3f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Globe3DInteractiveBox(
                        yaw = yawAngle,
                        pitch = pitchAngle,
                        scale = scaleFactor,
                        density = globeDensity,
                        light = lightFactor,
                        specular = specularValue,
                        wireframe = isWireframeOnly,
                        bitmap = currentBitmap,
                        autoRotate = autoRotateEnabled,
                        useSceneview = useSceneviewEnabled,
                        glbModelUrl = glbModelUrlValue,
                        onYawChange = { viewModel.yaw.value = it },
                        onPitchChange = { viewModel.pitch.value = it },
                        onScaleChange = { viewModel.scale.value = it },
                        onAutoRotateToggle = { viewModel.autoRotate.value = it }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1.0f)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(0.dp)
                        )
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .padding(16.dp)
                ) {
                    WorldControlPanel(
                        density = globeDensity,
                        autoRotate = autoRotateEnabled,
                        lightIntensity = lightFactor,
                        specular = specularValue,
                        wireframe = isWireframeOnly,
                        activeTexture = currentActiveType,
                        hasUploaded = uploadedPhoto != null,
                        urlInputValue = urlInputValue,
                        downloadStatus = downloadStatus,
                        useSceneview = useSceneviewEnabled,
                        glbModelUrl = glbModelUrlValue,
                        onDensityChange = { viewModel.density.value = it },
                        onAutoRotateToggle = { viewModel.autoRotate.value = it },
                        onLightChange = { viewModel.lightIntensity.value = it },
                        onSpecularChange = { viewModel.specularShininess.value = it },
                        onWireframeToggle = { viewModel.wireframeMode.value = it },
                        onSelectTexture = { viewModel.selectTexture(it) },
                        onLaunchPicker = { photoPickerLauncher.launch("image/*") },
                        onUrlValueChange = { viewModel.urlInputValue.value = it },
                        onDownloadClick = { viewModel.downloadTextureFromUrl(it) },
                        onUseSceneviewToggle = { viewModel.useSceneview.value = it },
                        onGlbModelUrlChange = { viewModel.glbModelUrl.value = it },
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
        } else {
            // Vertical Compact Viewport (Standard Device phone layout)
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Globe3DInteractiveBox(
                        yaw = yawAngle,
                        pitch = pitchAngle,
                        scale = scaleFactor,
                        density = globeDensity,
                        light = lightFactor,
                        specular = specularValue,
                        wireframe = isWireframeOnly,
                        bitmap = currentBitmap,
                        autoRotate = autoRotateEnabled,
                        useSceneview = useSceneviewEnabled,
                        glbModelUrl = glbModelUrlValue,
                        onYawChange = { viewModel.yaw.value = it },
                        onPitchChange = { viewModel.pitch.value = it },
                        onScaleChange = { viewModel.scale.value = it },
                        onAutoRotateToggle = { viewModel.autoRotate.value = it }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .padding(16.dp)
                ) {
                    WorldControlPanel(
                        density = globeDensity,
                        autoRotate = autoRotateEnabled,
                        lightIntensity = lightFactor,
                        specular = specularValue,
                        wireframe = isWireframeOnly,
                        activeTexture = currentActiveType,
                        hasUploaded = uploadedPhoto != null,
                        urlInputValue = urlInputValue,
                        downloadStatus = downloadStatus,
                        useSceneview = useSceneviewEnabled,
                        glbModelUrl = glbModelUrlValue,
                        onDensityChange = { viewModel.density.value = it },
                        onAutoRotateToggle = { viewModel.autoRotate.value = it },
                        onLightChange = { viewModel.lightIntensity.value = it },
                        onSpecularChange = { viewModel.specularShininess.value = it },
                        onWireframeToggle = { viewModel.wireframeMode.value = it },
                        onSelectTexture = { viewModel.selectTexture(it) },
                        onLaunchPicker = { photoPickerLauncher.launch("image/*") },
                        onUrlValueChange = { viewModel.urlInputValue.value = it },
                        onDownloadClick = { viewModel.downloadTextureFromUrl(it) },
                        onUseSceneviewToggle = { viewModel.useSceneview.value = it },
                        onGlbModelUrlChange = { viewModel.glbModelUrl.value = it }
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. INTERACTIVE 3D GLOBE RENDERING ENGINE BOX
// ==========================================

@Composable
fun Globe3DInteractiveBox(
    yaw: Float,
    pitch: Float,
    scale: Float,
    density: Int,
    light: Float,
    specular: Float,
    wireframe: Boolean,
    bitmap: Bitmap?,
    autoRotate: Boolean,
    useSceneview: Boolean,
    glbModelUrl: String,
    onYawChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onAutoRotateToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val outlineBorderColor = MaterialTheme.colorScheme.outlineVariant
    val gridLineColor = MaterialTheme.colorScheme.onSurfaceVariant
    var modelNodeRef by remember { mutableStateOf<io.github.sceneview.node.ModelNode?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var sceneViewRef by remember { mutableStateOf<io.github.sceneview.SceneView?>(null) }

    LaunchedEffect(glbModelUrl, sceneViewRef) {
        val view = sceneViewRef ?: run {
            AppLogger.w("SceneViewDebug", "[World] LaunchedEffect: SceneViewRef is null. Still waiting for view initialization.")
            return@LaunchedEffect
        }
        
        AppLogger.d("SceneViewDebug", "[World] LaunchedEffect: Waiting for view.isAttachedToWindow...")
        while (!view.isAttachedToWindow) {
            delay(100)
        }
        AppLogger.d("SceneViewDebug", "[World] LaunchedEffect: View is attached. Waiting a short delay for Filament Engine to settle.")
        delay(300)

        // Retrieve properties on UI Thread (Main) to avoid Thread Safety / null lazy loader issues
        var modelLoaderRefTemp: io.github.sceneview.loaders.ModelLoader? = null
        try {
            modelLoaderRefTemp = view.modelLoader
        } catch (e: Exception) {
            AppLogger.e("SceneViewDebug", "[World] Critical: Failed to retrieve view.modelLoader on UI Thread: ${e.message}", e)
        }

        val loader = modelLoaderRefTemp ?: run {
            AppLogger.e("SceneViewDebug", "[World] Critical: modelLoader is null, aborting.")
            return@LaunchedEffect
        }

        AppLogger.d("SceneViewDebug", "[World] LaunchedEffect triggered: glbModelUrl = $glbModelUrl")
        
        AppLogger.d("SceneViewDebug", "[World] Initiating node cleaning routine. Child count = ${view.childNodes.size}")
        try {
            view.childNodes.toMutableList().forEach { 
                AppLogger.d("SceneViewDebug", "[World] Removing existing child node: $it")
                view.removeChildNode(it) 
            }
        } catch (e: Exception) {
            AppLogger.e("SceneViewDebug", "[World] Error during node cleaning: ${e.message}", e)
        }
        modelNodeRef = null
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val resolvedPath = when {
                    glbModelUrl.contains("Earth/glTF-Binary/Earth.glb") || glbModelUrl.contains("Earth.glb") -> "models/Earth.glb"
                    glbModelUrl.contains("DamagedHelmet/glTF-Binary/DamagedHelmet.glb") || glbModelUrl.contains("DamagedHelmet.glb") -> "models/DamagedHelmet.glb"
                    glbModelUrl.contains("WaterBottle/glTF-Binary/WaterBottle.glb") || glbModelUrl.contains("WaterBottle.glb") -> "models/WaterBottle.glb"
                    glbModelUrl.contains("robot_expressive.glb") || glbModelUrl.contains("robot_expressive") -> "models/robot_expressive.glb"
                    else -> glbModelUrl
                }
                AppLogger.d("SceneViewDebug", "[World] Background Loader: Resolved path = $resolvedPath. Starting ModelLoader.createModel")
                
                val model = loader.createModel(resolvedPath)
                AppLogger.d("SceneViewDebug", "[World] Background Loader: createModel successfully compiled asset structure.")
                
                AppLogger.d("SceneViewDebug", "[World] Background Loader: Starting ModelLoader.createInstance")
                val modelInstance = loader.createInstance(model)
                if (modelInstance == null) {
                    AppLogger.e("SceneViewDebug", "[World] Background Loader failure: createInstance failed.")
                } else {
                    AppLogger.d("SceneViewDebug", "[World] Background Loader: createInstance successfully created 3D instance.")
                }
                
                if (modelInstance != null) {
                    launch(Dispatchers.Main) {
                        try {
                            AppLogger.d("SceneViewDebug", "[World] Main thread: Creating ModelNode.")
                            val modelNode = io.github.sceneview.node.ModelNode(modelInstance = modelInstance).apply {
                                position = io.github.sceneview.math.Position(0.0f, 0.0f, -2.0f)
                                rotation = io.github.sceneview.math.Rotation(y = 180f)
                            }
                            AppLogger.d("SceneViewDebug", "[World] Main thread: Calling view.addChildNode.")
                            view.addChildNode(modelNode)
                            modelNodeRef = modelNode
                            AppLogger.i("SceneViewDebug", "[World] Success! ModelNode successfully integrated into SceneView hierarchy at position (0, 0, -2).")
                        } catch (e2: Exception) {
                            AppLogger.e("SceneViewDebug", "[World] Main thread error attaching ModelNode: ${e2.message}", e2)
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("SceneViewDebug", "[World] Background Loader crashed: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    if (useSceneview) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF03050C))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    AppLogger.d("SceneViewDebug", "[World] AndroidView Factory: Creating new SceneView instance.")
                    try {
                        io.github.sceneview.SceneView(ctx).also {
                            sceneViewRef = it
                            AppLogger.i("SceneViewDebug", "[World] AndroidView Factory: SceneView instantiated successfully. Ref stored.")
                        }
                    } catch (e: Exception) {
                        AppLogger.e("SceneViewDebug", "[World] Exception inside AndroidView factory instantiation: ${e.message}", e)
                        throw e
                    }
                },
                update = { view ->
                    AppLogger.d("SceneViewDebug", "[World] AndroidView update handler triggered.")
                    try {
                        var frameCount = 0
                        view.onFrame = { _ ->
                            if (frameCount == 0 || frameCount % 120 == 0) {
                                AppLogger.d("SceneViewDebug", "[World] onFrame ticker tick count = $frameCount. modelNodeRef loaded = ${modelNodeRef != null}")
                            }
                            frameCount++
                            if (autoRotate) {
                                modelNodeRef?.let { node ->
                                    node.rotation = io.github.sceneview.math.Rotation(
                                        x = node.rotation.x,
                                        y = node.rotation.y + 0.8f,
                                        z = node.rotation.z
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("SceneViewDebug", "[World] Exception inside AndroidView update handler or onFrame handler: ${e.message}", e)
                    }
                }
            )

            // Render HUD overlay indication that Sceneview Active
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                Text(
                    text = "⚡ SCENEVIEW ACTIVE (PBR MODEL)",
                    color = Color(0xFF64FFDA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    } else {
        // Track active drag inputs elegantly inside pointer scopes
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF03050C))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Map Drag values directly to standard rotation orbits
                        val newYaw = (yaw - dragAmount.x * 0.007f) % (2f * PI.toFloat())
                        val newPitch = (pitch - dragAmount.y * 0.007f).coerceIn(-1.4f, 1.4f)
                        onYawChange(newYaw)
                        onPitchChange(newPitch)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap == null) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                // Generate the sphere triangulated coordinate model faces list
                val radius = 180f
                val faces = remember(density) {
                    WorldGlobeGenerator.generateSphereMesh(density, radius)
                }

                // Direction of light source vector
                val lightVector = WorldVector3(-0.4f, -0.8f, -0.6f).normalize()
                val ambientFactor = 0.25f

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    val cX = size.width / 2f
                    val cY = size.height / 2f

                    val cameraZ = 350f
                    val dFactor = 280f // coordinate compression factor to fit perfectly in drawing coordinates

                    // Project 3D vector vertex into 2D Offset points on the Compose DrawScope screen
                    // Inverting rot.y corrects the Cartesian to Screen Y-coordinate projection mapping bug
                    fun projectToScreen(v: WorldVector3): Offset {
                        val rot = v.rotateY(yaw).rotateX(pitch)
                        val denom = rot.z + cameraZ
                        val screenX = cX + (rot.x * dFactor * scale) / if (denom != 0f) denom else 1f
                        val screenY = cY - (rot.y * dFactor * scale) / if (denom != 0f) denom else 1f
                        return Offset(screenX, screenY)
                    }

                    // Gather average depth to apply painter depth-sorting algorithm back to front
                    val facesWithDepth = faces.map { face ->
                        val rotV0 = face.v0.rotateY(yaw).rotateX(pitch)
                        val rotV1 = face.v1.rotateY(yaw).rotateX(pitch)
                        val rotV2 = face.v2.rotateY(yaw).rotateX(pitch)
                        val avgZ = (rotV0.z + rotV1.z + rotV2.z) / 3f
                        Triple(face, avgZ, listOf(rotV0, rotV1, rotV2))
                    }

                    // Painter's logic sorting (draw deeper faces first)
                    val sortedFaces = facesWithDepth.sortedByDescending { it.second }

                    // Iterate over sorted triangulated polygons and make drawing pipelines calls
                    for (entry in sortedFaces) {
                        val avgDepth = entry.second
                        // Backface culling: filter outward pointing triangles that hide on the back hemisphere (Z > 0)
                        if (avgDepth > 4f) continue

                        val item = entry.first
                        val rotPts = entry.third

                        val p0 = projectToScreen(item.v0)
                        val p1 = projectToScreen(item.v1)
                        val p2 = projectToScreen(item.v2)

                        // Compute flat diffuse and specular shading coefficient for this specific face normal
                        val edge1 = item.v1 - item.v0
                        val edge2 = item.v2 - item.v0
                        val normal = edge1.cross(edge2).normalize()
                        val rotNormal = normal.rotateY(yaw).rotateX(pitch)

                        val dotFactor = rotNormal.dot(lightVector)
                        val localLight = (ambientFactor + (1f - ambientFactor) * max(0f, dotFactor)) * light

                        // Render standard textured triangles
                        drawTexturedTriangle(
                            p0 = p0, p1 = p1, p2 = p2,
                            uv0 = item.uv0, uv1 = item.uv1, uv2 = item.uv2,
                            bitmap = bitmap,
                            lightMultiplier = localLight,
                            specular = specular,
                            wireframeOnly = wireframe,
                            gridLineColor = gridLineColor
                        )
                    }

                    // Draw Polar Lines Segment-by-segment on front hemisphere
                    val arcticLatitude = 66.56f * PI.toFloat() / 180f
                    val antarcticLatitude = -66.56f * PI.toFloat() / 180f

                    fun drawPolarLine(latitudeRad: Float, strokeColor: Color) {
                        val steps = 180
                        val points = (0..steps).map { step ->
                            val phi = (step.toFloat() / steps) * 2f * PI.toFloat()
                            val rCircle = radius * cos(latitudeRad)
                            val x = rCircle * cos(phi)
                            val y = radius * sin(latitudeRad)
                            val z = rCircle * sin(phi)
                            WorldVector3(x, y, z)
                        }

                        for (i in 0 until steps) {
                            val pA = points[i]
                            val pB = points[i + 1]

                            val rotA = pA.rotateY(yaw).rotateX(pitch)
                            val rotB = pB.rotateY(yaw).rotateX(pitch)

                            val avgZ = (rotA.z + rotB.z) / 2f
                            if (avgZ <= 0f) { // Visible on front-side hemisphere
                                val screenA = projectToScreen(pA)
                                val screenB = projectToScreen(pB)
                                drawLine(
                                    color = strokeColor,
                                    start = screenA,
                                    end = screenB,
                                    strokeWidth = 3f
                                )
                            }
                        }
                    }

                    drawPolarLine(arcticLatitude, Color(0xFF2979FF)) // blue for North polar line
                    drawPolarLine(antarcticLatitude, Color(0xFFFF1744)) // red for South polar line
                }
            }

            // Action controllers box at bottom-right (unified horizontal Row to clean up bottom-right controls)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto-rotate Pause/Play Button
                IconButton(
                    onClick = { onAutoRotateToggle(!autoRotate) },
                    modifier = Modifier.size(36.dp)
                ) {
                    if (autoRotate) {
                        // Custom Pause Icon: two vertical bars
                        Row(
                            modifier = Modifier.size(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(12.dp)
                                    .background(Color(0xFF64FFDA), RoundedCornerShape(1.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(12.dp)
                                    .background(Color(0xFF64FFDA), RoundedCornerShape(1.dp))
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume Rotation",
                            tint = Color(0xFF64FFDA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Reset Camera Orbit Button
                IconButton(
                    onClick = {
                        onPitchChange(-0.2f)
                        onYawChange(0.0f)
                        onScaleChange(1.0f)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Camera",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Separator line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )

                // Zoom Out Button
                IconButton(
                    onClick = { onScaleChange((scale - 0.15f).coerceIn(0.4f, 2.5f)) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Zoom In Button
                IconButton(
                    onClick = { onScaleChange((scale + 0.15f).coerceIn(0.4f, 2.5f)) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Info marker badge overlays
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text("Altitude Range: 1x", color = Color(0xFF64FFDA), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Yaw: %d°".format(((yaw * 180f / PI).toInt() % 360).let { if (it < 0) it + 360 else it }), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Pitch: %d°".format((pitch * 180f / PI).toInt()), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ==========================================
// 4. FLOATING SETTINGS CONTROLLER DRAWER
// ==========================================

@Composable
fun WorldControlPanel(
    density: Int,
    autoRotate: Boolean,
    lightIntensity: Float,
    specular: Float,
    wireframe: Boolean,
    activeTexture: Int,
    hasUploaded: Boolean,
    urlInputValue: String,
    downloadStatus: String,
    useSceneview: Boolean,
    glbModelUrl: String,
    onDensityChange: (Int) -> Unit,
    onAutoRotateToggle: (Boolean) -> Unit,
    onLightChange: (Float) -> Unit,
    onSpecularChange: (Float) -> Unit,
    onWireframeToggle: (Boolean) -> Unit,
    onSelectTexture: (Int) -> Unit,
    onLaunchPicker: () -> Unit,
    onUrlValueChange: (String) -> Unit,
    onDownloadClick: (String) -> Unit,
    onUseSceneviewToggle: (Boolean) -> Unit,
    onGlbModelUrlChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvancedDialog by remember { mutableStateOf(false) }
    var textureMenuExpanded by remember { mutableStateOf(false) }
    var demoMenuExpanded by remember { mutableStateOf(false) }

    val activeTextureLabel = when (activeTexture) {
        0 -> "Earth Preset"
        1 -> "Neon Cyber Grid"
        2 -> "Custom Texture Map"
        else -> "Select Texture"
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Globe Mapping Configuration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Sceneview Engine Selector Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (useSceneview) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(
                width = 1.5.dp,
                color = if (useSceneview) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sceneview 3D Engine",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (useSceneview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (useSceneview) "Hardware accelerated PBR GLB" else "Pure CPU rendering canvas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useSceneview,
                        onCheckedChange = onUseSceneviewToggle
                    )
                }

                if (useSceneview) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "3D Asset Source URL:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = glbModelUrl,
                        onValueChange = onGlbModelUrlChange,
                        singleLine = true,
                        placeholder = { Text("Enter GLB path or https URL...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Dynamic Preset Selector buttons
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quick 3D Presets",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onGlbModelUrlChange("https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/Earth/glTF-Binary/Earth.glb") },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (glbModelUrl.contains("Earth")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (glbModelUrl.contains("Earth")) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Earth", fontSize = 9.sp)
                        }
                        Button(
                            onClick = { onGlbModelUrlChange("https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/DamagedHelmet/glTF-Binary/DamagedHelmet.glb") },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (glbModelUrl.contains("Helmet")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (glbModelUrl.contains("Helmet")) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Helmet", fontSize = 9.sp)
                        }
                        Button(
                            onClick = { onGlbModelUrlChange("https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/WaterBottle/glTF-Binary/WaterBottle.glb") },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (glbModelUrl.contains("Bottle")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (glbModelUrl.contains("Bottle")) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Bottle", fontSize = 9.sp)
                        }
                        Button(
                            onClick = { onGlbModelUrlChange("https://github.com/applh/kaimera/raw/refs/heads/main/app/src/main/assets/robot_expressive.glb") },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (glbModelUrl.contains("robot_expressive")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (glbModelUrl.contains("robot_expressive")) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Robot", fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        // Dropdown selection for Active Texture Style
        Column {
            Text(
                text = "Active Texture Source",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box {
                Button(
                    onClick = { textureMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("$activeTextureLabel ▾")
                }
                DropdownMenu(
                    expanded = textureMenuExpanded,
                    onDismissRequest = { textureMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    DropdownMenuItem(
                        text = { Text("Earth Preset (Standard Map)") },
                        onClick = {
                            onSelectTexture(0)
                            textureMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Neon Cyber Grid (Procedural)") },
                        onClick = {
                            onSelectTexture(1)
                            textureMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Custom Map (File or URL)") },
                        onClick = {
                            onSelectTexture(2)
                            textureMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Contextual UI depending on active texture (only show for Custom or URL textures)
        if (activeTexture == 2) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Custom Texture Source Settings",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // 1. File Picker Button
                    Button(
                        onClick = onLaunchPicker,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (hasUploaded) "Change Custom Image File" else "Load Custom Photo File", fontSize = 12.sp)
                    }

                    if (hasUploaded) {
                        Text(
                            text = "✓ Custom upload bitmap active",
                            color = Color(0xFF4CAF50),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 2. Direct URL Downloader
                    OutlinedTextField(
                        value = urlInputValue,
                        onValueChange = onUrlValueChange,
                        label = { Text("Texture Image Map URL", fontSize = 11.sp) },
                        placeholder = { Text("https://example.com/texture.jpg", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Button(
                        onClick = { onDownloadClick(urlInputValue) },
                        enabled = urlInputValue.isNotBlank() && downloadStatus != "Downloading...",
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Text("Download & Map URL", fontSize = 12.sp)
                    }

                    // Interactive planetary maps drop-down select to save massive space
                    Column {
                        Text(
                            text = "Or Load Demo Planet Mapping Setup",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box {
                            OutlinedButton(
                                onClick = { demoMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Select Demo Planetary Map ▾", fontSize = 11.sp)
                            }

                            val demoMaps = remember {
                                listOf(
                                    "NASA Earth (2K)" to "https://raw.githubusercontent.com/mrdoob/three.js/master/examples/textures/land_ocean_ice_cloud_2048.jpg",
                                    "Blue Marble (1K)" to "https://upload.wikimedia.org/wikipedia/commons/c/c4/Earthmap1000x500compac.jpg",
                                    "Jupiter Map (1K)" to "https://upload.wikimedia.org/wikipedia/commons/e/e2/Jupiter_Map_Equirectangular.jpg",
                                    "Moon Map (0.4K)" to "https://upload.wikimedia.org/wikipedia/commons/1/10/Lro_nearside_unlabelled_composite_384.jpg",
                                    "Osiris Mars (1K)" to "https://upload.wikimedia.org/wikipedia/commons/0/02/OSIRIS_Mars_true_color.jpg"
                                )
                            }

                            DropdownMenu(
                                expanded = demoMenuExpanded,
                                onDismissRequest = { demoMenuExpanded = false }
                            ) {
                                demoMaps.forEach { (name, link) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            onUrlValueChange(link)
                                            onDownloadClick(link)
                                            demoMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Live download Status messages
                    if (downloadStatus != "Idle") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        downloadStatus == "Downloading..." -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                        downloadStatus == "Success" -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                    }
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = when (downloadStatus) {
                                    "Downloading..." -> "⏳ Downloading map texture..."
                                    "Success" -> "🏆 Map loaded successfully!"
                                    else -> "⚠️ $downloadStatus"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    downloadStatus == "Downloading..." -> MaterialTheme.colorScheme.onSecondaryContainer
                                    downloadStatus == "Success" -> Color(0xFF81C784)
                                    else -> MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Open Advanced Settings Modal Button
        Button(
            onClick = { showAdvancedDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text("Advanced Shader & Mesh Settings ⚙")
        }

        // The Modal Dialog for Advanced Controls to declutter page margins and panels
        if (showAdvancedDialog) {
            AlertDialog(
                onDismissRequest = { showAdvancedDialog = false },
                title = {
                    Text(
                        text = "Advanced Engine Configs",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Custom density slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Mesh Step Density:", style = MaterialTheme.typography.bodySmall)
                                Text("$density steps", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = density.toFloat(),
                                onValueChange = { onDensityChange(it.toInt()) },
                                valueRange = 8f..40f,
                                steps = 32
                            )
                        }

                        // Lambert Light slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Lambert Direct Light:", style = MaterialTheme.typography.bodySmall)
                                Text("%.2f".format(lightIntensity), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = lightIntensity,
                                onValueChange = onLightChange,
                                valueRange = 0.2f..1.2f
                            )
                        }

                        // Specular Highlight slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Specular Reflection:", style = MaterialTheme.typography.bodySmall)
                                Text("%.2f".format(specular), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = specular,
                                onValueChange = onSpecularChange,
                                valueRange = 0.0f..0.8f
                            )
                        }

                        // Switches in columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-Rotate Orbit", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = autoRotate,
                                onCheckedChange = onAutoRotateToggle
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Wireframe Polygon Mode", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = wireframe,
                                onCheckedChange = onWireframeToggle
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAdvancedDialog = false }) {
                        Text("Apply Settings")
                    }
                }
            )
        }
    }
}
