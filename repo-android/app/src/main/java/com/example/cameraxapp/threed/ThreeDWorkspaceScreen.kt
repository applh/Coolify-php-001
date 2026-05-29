package com.example.cameraxapp.threed

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

// ==========================================
// 1. ADVANCED 3D VECTOR & MATHEMATICS ENGINE
// ==========================================

data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(factor: Float) = Vector3(x * factor, y * factor, z * factor)
    
    fun length() = sqrt(x * x + y * y + z * z)
    
    fun normalize(): Vector3 {
        val len = length()
        return if (len > 0f) Vector3(x / len, y / len, z / len) else this
    }
    
    fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z
    
    fun cross(other: Vector3) = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun rotateX(ang: Float): Vector3 {
        val cos = cos(ang)
        val sin = sin(ang)
        return Vector3(x, y * cos - z * sin, y * sin + z * cos)
    }

    fun rotateY(ang: Float): Vector3 {
        val cos = cos(ang)
        val sin = sin(ang)
        return Vector3(x * cos + z * sin, y, -x * sin + z * cos)
    }

    fun rotateZ(ang: Float): Vector3 {
        val cos = cos(ang)
        val sin = sin(ang)
        return Vector3(x * cos - y * sin, x * sin + y * cos, z)
    }
}

// Visual drawing structures for rendering pipeline sorting
sealed class RenderItem3D {
    abstract val depth: Float

    data class Polygon(
        val pts: List<Vector3>,
        val color: Color,
        val outlineColor: Color = Color.Black.copy(alpha = 0.4f),
        val isFilled: Boolean = true,
        val strokeWidth: Float = 1.5f,
        val tag: String = "",
        override val depth: Float
    ) : RenderItem3D()

    data class Sphere(
        val center: Vector3,
        val radius: Float,
        val color: Color,
        val label: String = "",
        val labelColor: Color = Color.White,
        override val depth: Float
    ) : RenderItem3D()

    data class Line(
        val start: Vector3,
        val end: Vector3,
        val color: Color,
        val strokeWidth: Float = 2f,
        override val depth: Float
    ) : RenderItem3D()
}

// Simulation selection modes
enum class SimulationMode {
    MOLECULE,
    ARCH_CAD,
    TOPO_TERRAIN,
    GEAR_MOTOR,
    RUBIK_CUBE
}

// 3D scene options
data class SceneOptions(
    val renderingStyle: String = "shaded", // shaded, wireframe, lines
    val lightIntensity: Float = 0.8f,
    val backfaceCulling: Boolean = true,
    val showGrid: Boolean = true,
    val autoRotateY: Boolean = false,
    val autoRotateSpeed: Float = 0.01f
)

// ==========================================
// 2. MAIN 3D WORKSPACE SCREEN IMPLEMENTATION
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeDWorkspaceScreen(
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenRightDrawer: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Core Navigation & Camera values
    var currentMode by remember { mutableStateOf(SimulationMode.MOLECULE) }
    var scaleFactor by remember { mutableStateOf(1.0f) }
    var pitchAngle by remember { mutableStateOf(-0.4f) } // vertical rotation
    var yawAngle by remember { mutableStateOf(0.6f) }   // horizontal rotation
    var cameraDistance by remember { mutableStateOf(400f) }
    var focalLength by remember { mutableStateOf(500f) }
    
    var options by remember { mutableStateOf(SceneOptions()) }
    
    // Automatic rotation helper loop
    LaunchedEffect(options.autoRotateY) {
        if (options.autoRotateY) {
            while (true) {
                yawAngle = (yawAngle + options.autoRotateSpeed) % (2 * PI.toFloat())
                delay(16)
            }
        }
    }

    // 1. Molecular state
    var selectedMolecule by remember { mutableStateOf("Caffeine") }
    var chemistryStyle by remember { mutableStateOf("ball_stick") } // ball_stick, space_filling, wireframe
    var hoveredAtomName by remember { mutableStateOf<String?>(null) }

    // 2. CAD Architectural House state
    var isRoofDetached by remember { mutableStateOf(false) }
    var explodeFactor by remember { mutableStateOf(0.0f) } // 0f to 1f (slides upper levels and roof apart)
    var isDoorOpen by remember { mutableStateOf(false) }
    var houseLightingMode by remember { mutableStateOf("Day") } // Day, Sunset, Night

    // 3. Terrain State
    var waterLevelHeight by remember { mutableStateOf(-8f) } // custom water levels
    var terrainMeshSize by remember { mutableStateOf(16) } // Size of grid N x N
    var scaleElevation by remember { mutableStateOf(20f) } // elevation scaling
    var animatedGliderTime by remember { mutableStateOf(0f) }
    LaunchedEffect(currentMode) {
        if (currentMode == SimulationMode.TOPO_TERRAIN) {
            while (currentMode == SimulationMode.TOPO_TERRAIN) {
                animatedGliderTime += 0.02f
                delay(20)
            }
        }
    }

    // 4. Gear system state
    var gearVelocity by remember { mutableStateOf(1.0f) }
    var gearRotationAnimationAngle by remember { mutableStateOf(0f) }
    var engineeringExplodeFactor by remember { mutableStateOf(0.0f) } // pushes gears axial
    LaunchedEffect(currentMode, gearVelocity) {
        if (currentMode == SimulationMode.GEAR_MOTOR) {
            while (currentMode == SimulationMode.GEAR_MOTOR) {
                gearRotationAnimationAngle += (0.02f * gearVelocity)
                delay(16)
            }
        }
    }

    // 5. Rubik's Cube State
    var rubikCubeState by remember { mutableStateOf(initRubikCube()) }
    var isScrambling by remember { mutableStateOf(false) }
    var rubikMovesCount by remember { mutableStateOf(0) }
    var rubikElapsedSeconds by remember { mutableStateOf(0) }
    var isRubikTimerActive by remember { mutableStateOf(false) }
    var isRubikSolved by remember { mutableStateOf(false) }

    // Timer ticking loop
    LaunchedEffect(isRubikTimerActive) {
        if (isRubikTimerActive) {
            while (isRubikTimerActive) {
                delay(1000)
                rubikElapsedSeconds++
            }
        }
    }
    
    fun rotateCubeSlice(axis: String, sliceIndex: Int, directionClockwise: Boolean) {
        rubikCubeState = rotateRubikSlice(rubikCubeState, axis, sliceIndex, directionClockwise)
        if (!isScrambling && !isRubikSolved) {
            rubikMovesCount++
            isRubikTimerActive = true
            isRubikSolved = checkIfRubikSolved(rubikCubeState)
            if (isRubikSolved) {
                isRubikTimerActive = false
            }
        }
    }

    fun triggerScramble() {
        if (isScrambling) return
        isScrambling = true
        isRubikSolved = false
        rubikMovesCount = 0
        rubikElapsedSeconds = 0
        isRubikTimerActive = false
        coroutineScope.launch {
            val axes = listOf("X", "Y", "Z")
            val slices = listOf(-1, 0, 1)
            repeat(15) {
                val axis = axes.random()
                val slice = slices.random()
                val dir = listOf(true, false).random()
                rotateCubeSlice(axis, slice, dir)
                delay(150)
            }
            isScrambling = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "3D Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                "3D Workspace & Simulation",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Sovereign 3D Vector Rendering Engine",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Drawer Left")
                    }
                    IconButton(onClick = onOpenRightDrawer) {
                        Icon(Icons.Default.Settings, contentDescription = "Quick Tools")
                    }
                }
            )
        }
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT || configuration.screenWidthDp < 600

        if (isPortrait) {
            // ==========================================
            // PORTRAIT SMARTPHONE LAYOUT (Snug & Scrollable)
            // ==========================================
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(Color(0xFF0F111A))
            ) {
                // 1. 3D Viewport on top (fixed or weighted height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.3f)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF07080E))
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                yawAngle = (yawAngle + dragAmount.x * 0.007f) % (2 * PI.toFloat())
                                pitchAngle = (pitchAngle - dragAmount.y * 0.007f).coerceIn(-1.5f, 1.5f)
                            }
                        }
                ) {
                    Canvas3DViewport(
                        mode = currentMode,
                        yaw = yawAngle,
                        pitch = pitchAngle,
                        scale = scaleFactor,
                        options = options,
                        moleculeName = selectedMolecule,
                        chemistryStyle = chemistryStyle,
                        onHoverAtom = { hoveredAtomName = it },
                        explodeFactor = explodeFactor,
                        isDoorOpen = isDoorOpen,
                        lightingPreset = houseLightingMode,
                        waterHeight = waterLevelHeight,
                        terrainMeshSize = terrainMeshSize,
                        elevationScale = scaleElevation,
                        gliderTime = animatedGliderTime,
                        gearRotation = gearRotationAnimationAngle,
                        engineeringExplode = engineeringExplodeFactor,
                        rubikCube = rubikCubeState
                    )

                    // Overlay HUD Indicators
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                "MODE: ${currentMode.name}",
                                color = Color(0xFF4AF2A1),
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            "Yaw: ${(yawAngle * 180 / PI).toInt()}° | Pitch: ${(pitchAngle * 180 / PI).toInt()}°",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }

                    // Hovered details
                    if (currentMode == SimulationMode.MOLECULE && hoveredAtomName != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color.Red))
                                Spacer(Modifier.width(4.dp))
                                Text(hoveredAtomName!!, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Rubik's HUD tracking stats
                    if (currentMode == SimulationMode.RUBIK_CUBE) {
                        RubikHudStatsOverlay(
                            moves = rubikMovesCount,
                            seconds = rubikElapsedSeconds,
                            isSolved = isRubikSolved
                        )
                    }

                    // Rubik solved congratulations overlay
                    if (currentMode == SimulationMode.RUBIK_CUBE && isRubikSolved) {
                        RubikSolvedCelebrationCard(
                            moves = rubikMovesCount,
                            seconds = rubikElapsedSeconds,
                            onPlayAgain = {
                                rubikCubeState = initRubikCube()
                                isRubikSolved = false
                                rubikMovesCount = 0
                                rubikElapsedSeconds = 0
                                isRubikTimerActive = false
                            },
                            onScramble = {
                                isRubikSolved = false
                                rubikMovesCount = 0
                                rubikElapsedSeconds = 0
                                triggerScramble()
                            }
                        )
                    }
                }

                // 2. Quick Simulations Mode Selector (Scrollable horizontally)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            "SELECT WORKBENCH 3D SIMULATION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = currentMode == SimulationMode.MOLECULE,
                                    onClick = { currentMode = SimulationMode.MOLECULE },
                                    label = { Text("Molecules", fontSize = 10.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = currentMode == SimulationMode.ARCH_CAD,
                                    onClick = { currentMode = SimulationMode.ARCH_CAD },
                                    label = { Text("CAD House", fontSize = 10.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = currentMode == SimulationMode.TOPO_TERRAIN,
                                    onClick = { currentMode = SimulationMode.TOPO_TERRAIN },
                                    label = { Text("Terrain", fontSize = 10.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = currentMode == SimulationMode.GEAR_MOTOR,
                                    onClick = { currentMode = SimulationMode.GEAR_MOTOR },
                                    label = { Text("Gearbox", fontSize = 10.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = currentMode == SimulationMode.RUBIK_CUBE,
                                    onClick = { currentMode = SimulationMode.RUBIK_CUBE },
                                    label = { Text("Rubik 3D", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                // 3. Bottom controls column (Weighted to take remaining space)
                Card(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                AnimatedContent(
                                    targetState = currentMode,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                                ) { targetMode ->
                                    when (targetMode) {
                                        SimulationMode.MOLECULE -> {
                                            MoleculeBottomControls(
                                                selected = selectedMolecule,
                                                onSelect = { selectedMolecule = it },
                                                style = chemistryStyle,
                                                onStyleSelect = { chemistryStyle = it }
                                            )
                                        }
                                        SimulationMode.ARCH_CAD -> {
                                            ArchitecturalBottomControls(
                                                explode = explodeFactor,
                                                onExplodeChange = { explodeFactor = it },
                                                isDoorOpen = isDoorOpen,
                                                onDoorToggle = { isDoorOpen = it },
                                                lightingPreset = houseLightingMode,
                                                onLightingChange = { houseLightingMode = it }
                                            )
                                        }
                                        SimulationMode.TOPO_TERRAIN -> {
                                            TopographyBottomControls(
                                                water = waterLevelHeight,
                                                onWaterChange = { waterLevelHeight = it },
                                                gridSize = terrainMeshSize,
                                                onGridSizeChange = { terrainMeshSize = it },
                                                heightScale = scaleElevation,
                                                onHeightScaleChange = { scaleElevation = it }
                                            )
                                        }
                                        SimulationMode.GEAR_MOTOR -> {
                                            MechanicalBottomControls(
                                                velocity = gearVelocity,
                                                onVelocityChange = { gearVelocity = it },
                                                explode = engineeringExplodeFactor,
                                                onExplodeChange = { engineeringExplodeFactor = it }
                                            )
                                        }
                                        SimulationMode.RUBIK_CUBE -> {
                                            RubikBottomControls(
                                                isScrambling = isScrambling,
                                                onScramble = { triggerScramble() },
                                                onReset = {
                                                    rubikCubeState = initRubikCube()
                                                    isRubikSolved = false
                                                    rubikMovesCount = 0
                                                    rubikElapsedSeconds = 0
                                                    isRubikTimerActive = false
                                                },
                                                onRotate = { axis, slice, dir ->
                                                    rotateCubeSlice(axis, slice, dir)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(Modifier.height(4.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("FAST VIEWPORT CONFIG", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Scale: ${(scaleFactor * 100).toInt()}%", modifier = Modifier.weight(1f), fontSize = 11.sp)
                                            Slider(
                                                value = scaleFactor,
                                                onValueChange = { scaleFactor = it },
                                                valueRange = 0.4f..2.5f,
                                                modifier = Modifier.weight(2.5f).height(24.dp)
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Auto Orbit", modifier = Modifier.weight(1f), fontSize = 11.sp)
                                            Switch(
                                                checked = options.autoRotateY,
                                                onCheckedChange = { options = options.copy(autoRotateY = it) },
                                                modifier = Modifier.scale(0.7f)
                                            )
                                            Button(
                                                onClick = {
                                                    pitchAngle = -0.4f
                                                    yawAngle = 0.6f
                                                    scaleFactor = 1.0f
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("Reset Cam", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // LANDSCAPE DESKTOP/TABLET LAYOUT (Split Frame)
            // ==========================================
            Row(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                // Left sidebar: Control workspace
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text(
                        "CHOOSE SIMULATION",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            SimulationSelectorCard(
                                title = "Molecular Analytics",
                                desc = "High-definition structural bonds, atomic scales.",
                                icon = Icons.Default.Star,
                                isSelected = currentMode == SimulationMode.MOLECULE,
                                onClick = { currentMode = SimulationMode.MOLECULE }
                            )
                        }
                        item {
                            SimulationSelectorCard(
                                title = "CAD Architecture",
                                desc = "Smart-Home modular layout construction & lighting.",
                                icon = Icons.Default.Home,
                                isSelected = currentMode == SimulationMode.ARCH_CAD,
                                onClick = { currentMode = SimulationMode.ARCH_CAD }
                            )
                        }
                        item {
                            SimulationSelectorCard(
                                title = "Topographic Elevation",
                                desc = "Procedural terrain voxels, water shifts & radar flight.",
                                icon = Icons.Default.LocationOn,
                                isSelected = currentMode == SimulationMode.TOPO_TERRAIN,
                                onClick = { currentMode = SimulationMode.TOPO_TERRAIN }
                            )
                        }
                        item {
                            SimulationSelectorCard(
                                title = "Gear & Piston Kinematics",
                                desc = "Interdependent mechanical transmissions with axial split.",
                                icon = Icons.Default.Refresh,
                                isSelected = currentMode == SimulationMode.GEAR_MOTOR,
                                onClick = { currentMode = SimulationMode.GEAR_MOTOR }
                            )
                        }
                        item {
                            SimulationSelectorCard(
                                title = "3D Puzzle Sandbox",
                                desc = "Interactable 3x3 Rubik's model with face layers.",
                                icon = Icons.Default.PlayArrow,
                                isSelected = currentMode == SimulationMode.RUBIK_CUBE,
                                onClick = { currentMode = SimulationMode.RUBIK_CUBE }
                            )
                        }

                        item {
                            Spacer(Modifier.height(16.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                "CAMERA CONTROLS",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Global Camera Toggles & sliders
                        item {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text("Zoom & Scale: ${(scaleFactor * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = scaleFactor,
                                    onValueChange = { scaleFactor = it },
                                    valueRange = 0.4f..2.5f,
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        item {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Orbit Y Lock", style = MaterialTheme.typography.bodySmall)
                                    Switch(
                                        checked = options.autoRotateY,
                                        onCheckedChange = { options = options.copy(autoRotateY = it) },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                            }
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                Button(
                                    onClick = {
                                        pitchAngle = -0.4f
                                        yawAngle = 0.6f
                                        scaleFactor = 1.0f
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reset View", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        options = options.copy(showGrid = !options.showGrid)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (options.showGrid) "Hide Grid" else "Show Grid", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    
                    // Active configuration / details panel inside sidebar bottom
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "Projections Detail",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Focal Length: ${focalLength.toInt()}px\nRender Mode: Perspective\nDepth Sort: Painters Alg.",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Right region: Main 3D Canvas viewport & context controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF0F111A)) // Deep space charcoal theme
                        .padding(16.dp)
                ) {
                    // Live Viewport Panel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF07080E)) // Cosmic Black viewport
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    yawAngle = (yawAngle + dragAmount.x * 0.007f) % (2 * PI.toFloat())
                                    pitchAngle = (pitchAngle - dragAmount.y * 0.007f).coerceIn(-1.5f, 1.5f)
                                }
                            }
                    ) {
                        Canvas3DViewport(
                            mode = currentMode,
                            yaw = yawAngle,
                            pitch = pitchAngle,
                            scale = scaleFactor,
                            options = options,
                            moleculeName = selectedMolecule,
                            chemistryStyle = chemistryStyle,
                            onHoverAtom = { hoveredAtomName = it },
                            explodeFactor = explodeFactor,
                            isDoorOpen = isDoorOpen,
                            lightingPreset = houseLightingMode,
                            waterHeight = waterLevelHeight,
                            terrainMeshSize = terrainMeshSize,
                            elevationScale = scaleElevation,
                            gliderTime = animatedGliderTime,
                            gearRotation = gearRotationAnimationAngle,
                            engineeringExplode = engineeringExplodeFactor,
                            rubikCube = rubikCubeState
                        )

                        // Overlay HUD Indicators
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    "MODE: ${currentMode.name}",
                                    color = Color(0xFF4AF2A1),
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                "Yaw: ${(yawAngle * 180 / PI).toInt()}° | Pitch: ${(pitchAngle * 180 / PI).toInt()}°",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }

                        // Hovered details
                        if (currentMode == SimulationMode.MOLECULE && hoveredAtomName != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                            ) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color.Red))
                                    Spacer(Modifier.width(6.dp))
                                    Text(hoveredAtomName!!, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Rubik's HUD tracking stats
                        if (currentMode == SimulationMode.RUBIK_CUBE) {
                            RubikHudStatsOverlay(
                                moves = rubikMovesCount,
                                seconds = rubikElapsedSeconds,
                                isSolved = isRubikSolved
                            )
                        }

                        // Rubik solved congratulations overlay
                        if (currentMode == SimulationMode.RUBIK_CUBE && isRubikSolved) {
                            RubikSolvedCelebrationCard(
                                moves = rubikMovesCount,
                                seconds = rubikElapsedSeconds,
                                onPlayAgain = {
                                    rubikCubeState = initRubikCube()
                                    isRubikSolved = false
                                    rubikMovesCount = 0
                                    rubikElapsedSeconds = 0
                                    isRubikTimerActive = false
                                },
                                onScramble = {
                                    isRubikSolved = false
                                    rubikMovesCount = 0
                                    rubikElapsedSeconds = 0
                                    triggerScramble()
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Bottom Controls customized according to selected mode
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        AnimatedContent(
                            targetState = currentMode,
                            transitionSpec = { fadeIn() togetherWith fadeOut() }
                        ) { targetMode ->
                            Column(modifier = Modifier.padding(14.dp)) {
                                when (targetMode) {
                                    SimulationMode.MOLECULE -> {
                                        MoleculeBottomControls(
                                            selected = selectedMolecule,
                                            onSelect = { selectedMolecule = it },
                                            style = chemistryStyle,
                                            onStyleSelect = { chemistryStyle = it }
                                        )
                                    }
                                    SimulationMode.ARCH_CAD -> {
                                        ArchitecturalBottomControls(
                                            explode = explodeFactor,
                                            onExplodeChange = { explodeFactor = it },
                                            isDoorOpen = isDoorOpen,
                                            onDoorToggle = { isDoorOpen = it },
                                            lightingPreset = houseLightingMode,
                                            onLightingChange = { houseLightingMode = it }
                                        )
                                    }
                                    SimulationMode.TOPO_TERRAIN -> {
                                        TopographyBottomControls(
                                            water = waterLevelHeight,
                                            onWaterChange = { waterLevelHeight = it },
                                            gridSize = terrainMeshSize,
                                            onGridSizeChange = { terrainMeshSize = it },
                                            heightScale = scaleElevation,
                                            onHeightScaleChange = { scaleElevation = it }
                                        )
                                    }
                                    SimulationMode.GEAR_MOTOR -> {
                                        MechanicalBottomControls(
                                            velocity = gearVelocity,
                                            onVelocityChange = { gearVelocity = it },
                                            explode = engineeringExplodeFactor,
                                            onExplodeChange = { engineeringExplodeFactor = it }
                                        )
                                    }
                                    SimulationMode.RUBIK_CUBE -> {
                                        RubikBottomControls(
                                            isScrambling = isScrambling,
                                            onScramble = { triggerScramble() },
                                            onReset = {
                                                rubikCubeState = initRubikCube()
                                                isRubikSolved = false
                                                rubikMovesCount = 0
                                                rubikElapsedSeconds = 0
                                                isRubikTimerActive = false
                                            },
                                            onRotate = { axis, slice, dir ->
                                                rotateCubeSlice(axis, slice, dir)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. INTERNAL COMPOSABLE SUB-COMPONENTS
// ==========================================

@Composable
fun SimulationSelectorCard(
    title: String,
    desc: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

// ==========================================
// 4. MODULE-SPECIFIC BOTTOM PANEL CONTROLS
// ==========================================

@Composable
fun MoleculeBottomControls(
    selected: String,
    onSelect: (String) -> Unit,
    style: String,
    onStyleSelect: (String) -> Unit
) {
    Column {
        Text("Molecular Select", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Caffeine", "DNA Helix", "Water", "Buckyball C60").forEach { mol ->
                FilterChip(
                    selected = selected == mol,
                    onClick = { onSelect(mol) },
                    label = { Text(mol) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Visual Matrix Style", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mapOf("ball_stick" to "Ball & Stick", "space_filling" to "Space Filling", "wireframe" to "Wireframe Bonds").forEach { (key, display) ->
                ElevatedFilterChip(
                    selected = style == key,
                    onClick = { onStyleSelect(key) },
                    label = { Text(display) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ArchitecturalBottomControls(
    explode: Float,
    onExplodeChange: (Float) -> Unit,
    isDoorOpen: Boolean,
    onDoorToggle: (Boolean) -> Unit,
    lightingPreset: String,
    onLightingChange: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("CAD Structural Explode (Floors Split)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.width(8.dp))
            Text("${(explode * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = explode,
            onValueChange = onExplodeChange,
            modifier = Modifier.height(28.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Smart Entrance Gate", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isDoorOpen) "🚪 Door: OPENED (Rotated 90°)" else "🚪 Door: LOCKED (Static Plane)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            Switch(checked = isDoorOpen, onCheckedChange = onDoorToggle)
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text("Thermal Shadow Shader", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Day", "Sunset", "Night").forEach { mode ->
                        FilterChip(
                            selected = lightingPreset == mode,
                            onClick = { onLightingChange(mode) },
                            label = { Text(mode, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopographyBottomControls(
    water: Float,
    onWaterChange: (Float) -> Unit,
    gridSize: Int,
    onGridSizeChange: (Int) -> Unit,
    heightScale: Float,
    onHeightScaleChange: (Float) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Simulation Sea Level: ${water.toInt()}m", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Slider(value = water, onValueChange = onWaterChange, valueRange = -18f..18f)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Alps Elevation Lift: ${heightScale.toInt()}x", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Slider(value = heightScale, onValueChange = onHeightScaleChange, valueRange = 4f..40f)
        }
        Column(modifier = Modifier.weight(0.8f)) {
            Text("Voxel Mesh density", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                listOf(10, 16, 22).forEach { size ->
                    FilterChip(
                        selected = gridSize == size,
                        onClick = { onGridSizeChange(size) },
                        label = { Text("${size}x${size}", fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
fun MechanicalBottomControls(
    velocity: Float,
    onVelocityChange: (Float) -> Unit,
    explode: Float,
    onExplodeChange: (Float) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Motor Crank Velocity", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.width(6.dp))
                Text("${velocity.toInt()}x rpm", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Slider(value = velocity, onValueChange = onVelocityChange, valueRange = 0f..4f)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Axial Exploded Diagram", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.width(6.dp))
                Text("${(explode * 100).toInt()}% dist", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Slider(value = explode, onValueChange = onExplodeChange, valueRange = 0.0f..1.0f)
        }
    }
}

@Composable
fun RubikBottomControls(
    isScrambling: Boolean,
    onScramble: () -> Unit,
    onReset: () -> Unit,
    onRotate: (String, Int, Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Rubik's 3D Core", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text("Click layers buttons to rotate slices in real-time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onScramble, enabled = !isScrambling, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isScrambling) "Scrambling..." else "Scramble")
                }
                OutlinedButton(onClick = onReset) {
                    Text("Reset Rubik")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        
        // Manual rotations list
        Text("Slice Rotators (CCW / CW):", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            item {
                RubikButton(label = "U (Top-CCW)", onClick = { onRotate("Y", 1, false) })
                RubikButton(label = "U' (Top-CW)", onClick = { onRotate("Y", 1, true) })
            }
            item {
                RubikButton(label = "D (Bottom-CCW)", onClick = { onRotate("Y", -1, false) })
                RubikButton(label = "D' (Bottom-CW)", onClick = { onRotate("Y", -1, true) })
            }
            item {
                RubikButton(label = "L (Left-CCW)", onClick = { onRotate("X", -1, false) })
                RubikButton(label = "L' (Left-CW)", onClick = { onRotate("X", -1, true) })
            }
            item {
                RubikButton(label = "R (Right-CCW)", onClick = { onRotate("X", 1, false) })
                RubikButton(label = "R' (Right-CW)", onClick = { onRotate("X", 1, true) })
            }
            item {
                RubikButton(label = "F (Front-CCW)", onClick = { onRotate("Z", 1, false) })
                RubikButton(label = "F' (Front-CW)", onClick = { onRotate("Z", 1, true) })
            }
        }
    }
}

@Composable
fun RubikButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.padding(end = 4.dp).height(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}


// ==========================================
// 5. VIEWPORT 3D PAINTER PIPELINE CANVAS
// ==========================================

@Composable
fun Canvas3DViewport(
    mode: SimulationMode,
    yaw: Float,
    pitch: Float,
    scale: Float,
    options: SceneOptions,
    
    // Mode parameters
    moleculeName: String,
    chemistryStyle: String,
    onHoverAtom: (String?) -> Unit,
    explodeFactor: Float,
    isDoorOpen: Boolean,
    lightingPreset: String,
    waterHeight: Float,
    terrainMeshSize: Int,
    elevationScale: Float,
    gliderTime: Float,
    gearRotation: Float,
    engineeringExplode: Float,
    rubikCube: List<RubikMinisc>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val cX = width / 2f
        val cY = height / 2f
        
        // Define base camera distance & projection metrics
        val cameraZ = 350f
        val dFactor = 400f // coordinate compression factor to fit nicely on Canvas coordinates

        // Projection mapping helper
        fun projectPoint(v: Vector3): Offset {
            // Apply camera rotations Yaw & Pitch
            val rot = v.rotateY(yaw).rotateX(pitch)
            // Perspective transform
            val denom = rot.z + cameraZ
            val screenX = cX + (rot.x * dFactor * scale) / if (denom != 0f) denom else 1f
            val screenY = cY + (rot.y * dFactor * scale) / if (denom != 0f) denom else 1f
            return Offset(screenX, screenY)
        }

        fun getRotatedZDeep(v: Vector3): Float {
            return v.rotateY(yaw).rotateX(pitch).z
        }

        // Draw basic background space grid
        if (options.showGrid) {
            val gridColor = Color(0xFF1E2235).copy(alpha = 0.4f)
            val step = 40f
            // Draw an elegant perspective look floor plane
            for (i in -6..6) {
                // Horizontal lines on grid floor Y = 100
                val pStart1 = projectPoint(Vector3((i * step), 100f, -240f))
                val pEnd1 = projectPoint(Vector3((i * step), 100f, 240f))
                drawLine(gridColor, pStart1, pEnd1, strokeWidth = 1f)

                val pStart2 = projectPoint(Vector3(-240f, 100f, (i * step)))
                val pEnd2 = projectPoint(Vector3(240f, 100f, (i * step)))
                drawLine(gridColor, pStart2, pEnd2, strokeWidth = 1f)
            }
        }

        // Generate 3D pipeline primitives list depending on simulation mode
        val drawPipeline = mutableListOf<RenderItem3D>()

        when (mode) {
            SimulationMode.MOLECULE -> {
                generateMoleculePrimitives(
                    moleculeName = moleculeName,
                    style = chemistryStyle,
                    drawPipeline = drawPipeline
                )
            }
            SimulationMode.ARCH_CAD -> {
                generateArchitecturalPrimitives(
                    explode = explodeFactor,
                    isDoorOpen = isDoorOpen,
                    preset = lightingPreset,
                    drawPipeline = drawPipeline
                )
            }
            SimulationMode.TOPO_TERRAIN -> {
                generateTopographicPrimitives(
                    gridSize = terrainMeshSize,
                    water = waterHeight,
                    scaleZ = elevationScale,
                    time = gliderTime,
                    drawPipeline = drawPipeline
                )
            }
            SimulationMode.GEAR_MOTOR -> {
                generateMechanicalGearsAndPiston(
                    rot = gearRotation,
                    explode = engineeringExplode,
                    drawPipeline = drawPipeline
                )
            }
            SimulationMode.RUBIK_CUBE -> {
                generateRubikCubePrimitives(
                    cubes = rubikCube,
                    drawPipeline = drawPipeline
                )
            }
        }

        // Apply Painter's Algorithm: Sort items by depth (back to front, Z positive is deeper/further away)
        // In our rotation, Z closer to us is negative, further away is positive. So we sort from highest Z (furthest) to lowest Z (closest).
        val sortedItems = drawPipeline.sortedByDescending { item ->
            when (item) {
                is RenderItem3D.Polygon -> {
                    // Average depth coordinates of all vertices
                    var sumZ = 0f
                    item.pts.forEach { sumZ += getRotatedZDeep(it) }
                    sumZ / item.pts.size
                }
                is RenderItem3D.Sphere -> {
                    getRotatedZDeep(item.center)
                }
                is RenderItem3D.Line -> {
                    (getRotatedZDeep(item.start) + getRotatedZDeep(item.end)) / 2f
                }
            }
        }

        // Render actual projected primitives onto canvas
        sortedItems.forEach { item ->
            when (item) {
                is RenderItem3D.Line -> {
                    val p1 = projectPoint(item.start)
                    val p2 = projectPoint(item.end)
                    drawLine(
                        color = item.color,
                        start = p1,
                        end = p2,
                        strokeWidth = item.strokeWidth * scale
                    )
                }
                is RenderItem3D.Sphere -> {
                    val pCenter = projectPoint(item.center)
                    // Scale radius with camera distance
                    val rotZ = getRotatedZDeep(item.center)
                    val denom = rotZ + cameraZ
                    val radiusRatio = if (denom > 0f) dFactor / denom else 1f
                    val screenRadius = item.radius * scale * radiusRatio

                    if (screenRadius > 0.5f) {
                        // Draw radial shade gradient to look nicely spherical 3D!
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.9f), item.color, item.color.copy(alpha = 0.6f)),
                                center = pCenter - Offset(screenRadius * 0.3f, screenRadius * 0.3f), // offset highlight
                                radius = screenRadius
                            ),
                            center = pCenter,
                            radius = screenRadius
                        )
                        // Thin outline
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.3f),
                            center = pCenter,
                            radius = screenRadius,
                            style = Stroke(width = 1f * scale)
                        )
                        // Label overlay if any
                        if (item.label.isNotEmpty() && screenRadius > 14f) {
                            // Center label simple drawing text omitted to maintain Canvas standard, or could use text draw outline
                        }
                    }
                }
                is RenderItem3D.Polygon -> {
                    if (item.pts.size >= 3) {
                        val screenPolygonPoints = item.pts.map { projectPoint(it) }
                        
                        // Check normal representation for Backface culling
                        // Calculate face normal in 3D: (P1-P0) x (P2-P0)
                        val v0 = item.pts[0]
                        val v1 = item.pts[1]
                        val v2 = item.pts[2]
                        val normal = (v1 - v0).cross(v2 - v0).normalize()
                        val normalRotated = normal.rotateY(yaw).rotateX(pitch)
                        
                        // In perspective, if normal points away from camera, we can skip rendering if backface culling enabled
                        // Camera looks along +Z axis, so if normalRotated.z > 0, it points away
                        if (!options.backfaceCulling || normalRotated.z < 0) {
                            val path = Path().apply {
                                moveTo(screenPolygonPoints[0].x, screenPolygonPoints[0].y)
                                for (idx in 1 until screenPolygonPoints.size) {
                                    lineTo(screenPolygonPoints[idx].x, screenPolygonPoints[idx].y)
                                }
                                close()
                            }
                            
                            // Apply cosine light shading depending on normal matching a simulated light source angle (from top-left)
                            val lightSourceVector = Vector3(-0.5f, -1.0f, -0.6f).normalize()
                            val dotProduct = normal.dot(lightSourceVector).coerceIn(0.1f, 1.0f)
                            
                            val rawColor = item.color
                            val shadedColor = Color(
                                red = (rawColor.red * dotProduct).coerceIn(0f, 1f),
                                green = (rawColor.green * dotProduct).coerceIn(0f, 1f),
                                blue = (rawColor.blue * dotProduct).coerceIn(0f, 1f),
                                alpha = rawColor.alpha
                            )

                            if (item.isFilled) {
                                drawPath(
                                    path = path,
                                    color = shadedColor
                                )
                            }
                            drawPath(
                                path = path,
                                color = item.outlineColor,
                                style = Stroke(width = item.strokeWidth * scale)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. PIPELINE MOLECULAR 3D GENERATOR
// ==========================================

fun generateMoleculePrimitives(
    moleculeName: String,
    style: String,
    drawPipeline: MutableList<RenderItem3D>
) {
    val atoms = mutableListOf<Atom3D>()
    val bonds = mutableListOf<Bond3D>()

    when (moleculeName) {
        "Water" -> {
            // Oxygen is center
            atoms.add(Atom3D("O", Color(0xFFE57373), Vector3(0f, 0f, 0f), 20f))
            // Hydrogens
            atoms.add(Atom3D("H", Color(0xFFF5F5F5), Vector3(38f, 26f, 0f), 13f))
            atoms.add(Atom3D("H", Color(0xFFF5F5F5), Vector3(-38f, 26f, 0f), 13f))
            
            bonds.add(Bond3D(0, 1))
            bonds.add(Bond3D(0, 2))
        }
        "Caffeine" -> {
            // Seed coordinates approximation for caffeine (Purine double cluster ring)
            // Carbon atoms (Grey)
            atoms.add(Atom3D("C", Color(0xFF424242), Vector3(-30f, -30f, 0f), 16f)) // 0
            atoms.add(Atom3D("C", Color(0xFF424242), Vector3(15f, -40f, 5f), 16f))  // 1
            atoms.add(Atom3D("C", Color(0xFF424242), Vector3(45f, -10f, 0f), 16f))  // 2
            atoms.add(Atom3D("C", Color(0xFF424242), Vector3(25f, 25f, -5f), 16f))  // 3
            atoms.add(Atom3D("C", Color(0xFF424242), Vector3(-15f, 25f, 0f), 16f))  // 4
            atoms.add(Atom3D("C", Color(0xFF424242), Vector3(-45f, -5f, -5f), 16f)) // 5
            
            // Nitrogens (Blue)
            atoms.add(Atom3D("N", Color(0xFF2196F3), Vector3(-35f, -65f, 5f), 16f)) // 6
            atoms.add(Atom3D("N", Color(0xFF2196F3), Vector3(60f, 15f, -5f), 16f))   // 7
            atoms.add(Atom3D("N", Color(0xFF2196F3), Vector3(10f, 55f, 0f), 16f))   // 8
            atoms.add(Atom3D("N", Color(0xFF2196F3), Vector3(-60f, 20f, 5f), 16f))   // 9
            
            // Oxygens (Red)
            atoms.add(Atom3D("O", Color(0xFFEF5350), Vector3(-55f, -50f, 0f), 18f)) // 10
            atoms.add(Atom3D("O", Color(0xFFEF5350), Vector3(35f, 55f, 0f), 18f))   // 11
            
            // Methyl groups (Light carbon representers with small hydrogen rings)
            atoms.add(Atom3D("C", Color(0xFF757575), Vector3(-20f, -95f, 10f), 12f)) // 12 (attached 6)
            atoms.add(Atom3D("C", Color(0xFF757575), Vector3(80f, 35f, -10f), 12f))  // 13 (attached 7)
            atoms.add(Atom3D("C", Color(0xFF757575), Vector3(-80f, 40f, 10f), 12f))  // 14 (attached 9)
            
            // Interring Bonds
            bonds.add(Bond3D(0, 1))
            bonds.add(Bond3D(1, 2, 2))
            bonds.add(Bond3D(2, 3))
            bonds.add(Bond3D(3, 4, 2))
            bonds.add(Bond3D(4, 5))
            bonds.add(Bond3D(5, 0))

            bonds.add(Bond3D(0, 6))
            bonds.add(Bond3D(6, 12))
            bonds.add(Bond3D(2, 7))
            bonds.add(Bond3D(7, 13))
            bonds.add(Bond3D(3, 8))
            bonds.add(Bond3D(5, 9))
            bonds.add(Bond3D(9, 14))
            
            bonds.add(Bond3D(0, 10, 2))
            bonds.add(Bond3D(4, 11, 2))
        }
        "DNA Helix" -> {
            // Spiral helix generation
            for (i in -8..8) {
                val baseAngle = i * 0.45f
                val h = i * 14f
                val r = 45f
                
                // Sugar-Phosphate spine node A (Cyan)
                val posA = Vector3(r * cos(baseAngle), h, r * sin(baseAngle))
                atoms.add(Atom3D("SpineA", Color(0xFF00ACC1), posA, 13f))
                
                // Spine node B (Maggie Pink) - 180 deg offset
                val posB = Vector3(r * cos(baseAngle + PI.toFloat()), h, r * sin(baseAngle + PI.toFloat()))
                atoms.add(Atom3D("SpineB", Color(0xFFD81B60), posB, 13f))
                
                val idxA = atoms.size - 2
                val idxB = atoms.size - 1
                
                // Add outer base links to connect spine sequential elements
                if (i > -8) {
                    val prevIdxA = idxA - 2
                    val prevIdxB = idxB - 2
                    bonds.add(Bond3D(prevIdxA, idxA, 2))
                    bonds.add(Bond3D(prevIdxB, idxB, 2))
                }
                
                // Nitrogen base pairs (Adenine-Thymine or Guanine-Cytosine)
                // We add two bridge base spheres in the middle of the cord
                val baseColor1 = if (i % 2 == 0) Color(0xFF4CAF50) else Color(0xFFFFEB3B) // Adenine (Green) / Thymine (Yellow)
                val baseColor2 = if (i % 2 == 0) Color(0xFFFFEB3B) else Color(0xFF4CAF50)
                
                val midBase1 = posA * 0.6f + posB * 0.4f
                val midBase2 = posA * 0.4f + posB * 0.6f
                
                atoms.add(Atom3D("Base1", baseColor1, midBase1, 9f))
                atoms.add(Atom3D("Base2", baseColor2, midBase2, 9f))
                
                val idxBase1 = atoms.size - 2
                val idxBase2 = atoms.size - 1
                
                // Base bonds
                bonds.add(Bond3D(idxA, idxBase1))
                bonds.add(Bond3D(idxBase1, idxBase2, 1)) // Hydrogen ladder bonds
                bonds.add(Bond3D(idxBase2, idxB))
            }
        }
        "Buckyball C60" -> {
            // Procedurally build structured geodesic Buckyball configuration (Truncated Icosahedron approximation)
            // Mathematical coordinates for perfect C60 points are permutations of Golden ratio
            val phi = (1f + sqrt(5f)) / 2f
            
            val baseVerts = mutableListOf<Vector3>()
            // Generate standard vertices
            listOf(-1f, 1f).forEach { x ->
                listOf(-1f, 1f).forEach { y ->
                    listOf(-phi, phi).forEach { z ->
                        baseVerts.add(Vector3(x, y, z))
                        baseVerts.add(Vector3(y, z, x))
                        baseVerts.add(Vector3(z, x, y))
                    }
                }
            }
            // Normalize all generated spherical vertices to correct molecule bounds
            val radiusScale = 65f
            val c60Atoms = baseVerts.distinctBy {
                "${(it.x * 100).toInt()},${(it.y * 100).toInt()},${(it.z * 100).toInt()}"
            }.map { it.normalize() * radiusScale }
            
            c60Atoms.forEachIndexed { index, pos ->
                atoms.add(Atom3D("C", Color(0xFF8D6E63), pos, 10f))
            }
            
            // Build covalent carbon-carbon graph connection links
            // Connect atoms if they are extremely close in distance (the hexagonal/pentagonal face vertices)
            val thresholdDistance = 40f
            for (m in 0 until atoms.size) {
                for (n in m + 1 until atoms.size) {
                    val dist = (atoms[m].pos - atoms[n].pos).length()
                    if (dist < thresholdDistance) {
                        bonds.add(Bond3D(m, n))
                    }
                }
            }
        }
    }

    // Convert atom structures & bonds to 3D Viewport Drawing Primitives
    // Draw wireframe/stick lines first
    if (style != "space_filling") {
        bonds.forEach { bond ->
            val origin = atoms[bond.atom1].pos
            val target = atoms[bond.atom2].pos
            val lineDeepVal = (origin.z + target.z) / 2f
            
            val col = if (bond.bondOrder == 2) Color(0xFFEF6C00) else Color(0x66FFFFFF)
            drawPipeline.add(
                RenderItem3D.Line(
                    start = origin,
                    end = target,
                    color = col,
                    strokeWidth = if (bond.bondOrder == 2) 4.5f else 3.0f,
                    depth = lineDeepVal
                )
            )
        }
    }

    // Draw solid atomic elements
    atoms.forEach { atom ->
        val rad = if (style == "space_filling") atom.radius * 1.5f else if (style == "wireframe") 6f else atom.radius
        drawPipeline.add(
            RenderItem3D.Sphere(
                center = atom.pos,
                radius = rad,
                color = atom.color,
                label = atom.symbol,
                depth = atom.pos.z
            )
        )
    }
}

data class Atom3D(val symbol: String, val color: Color, val pos: Vector3, val radius: Float)
data class Bond3D(val atom1: Int, val atom2: Int, val bondOrder: Int = 1)

// ==========================================
// 7. PIPELINE CAD ARCHITECTURAL GENERATOR
// ==========================================

fun generateArchitecturalPrimitives(
    explode: Float,
    isDoorOpen: Boolean,
    preset: String,
    drawPipeline: MutableList<RenderItem3D>
) {
    // Select color palette corresponding to chosen time shadows
    val (wallColor, groundColor, roofColor, windowColor) = when (preset) {
        "Sunset" -> listOf(Color(0xFFCE93D8), Color(0xFFD7CCC8), Color(0xFFE64A19), Color(0x99FFCC80))
        "Night" -> listOf(Color(0xFF37474F), Color(0xFF263238), Color(0xFF1A237E), Color(0xAA90CAF9))
        else -> listOf(Color(0xFFEEEEEE), Color(0xFFCFD8DC), Color(0xFFC62828), Color(0xAA80DEEA)) // Day default
    }

    val explodedGroundY = -35f
    val explodedFirstFloorY = 15f + (60f * explode)
    val explodedRoofY = 65f + (140f * explode)

    // A. Ground Floor Slab (Tiled wooden porch polygon)
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-90f, explodedGroundY, -90f),
                Vector3(90f, explodedGroundY, -90f),
                Vector3(90f, explodedGroundY, 90f),
                Vector3(-90f, explodedGroundY, 90f)
            ),
            color = groundColor,
            depth = explodedGroundY
        )
    )

    // B. Ground floor structural walls
    val wH = 45f
    // Back Wall
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-80f, explodedGroundY, -80f),
                Vector3(80f, explodedGroundY, -80f),
                Vector3(80f, explodedGroundY + wH, -80f),
                Vector3(-80f, explodedGroundY + wH, -80f)
            ),
            color = wallColor.copy(alpha = 0.95f),
            depth = -80f
        )
    )

    // Left outer wall
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-80f, explodedGroundY, 80f),
                Vector3(-80f, explodedGroundY, -80f),
                Vector3(-80f, explodedGroundY + wH, -80f),
                Vector3(-80f, explodedGroundY + wH, 80f)
            ),
            color = wallColor.copy(alpha = 0.95f),
            depth = -0.5f
        )
    )

    // Right Wall with translucent Glass Sliding screen
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(80f, explodedGroundY, -80f),
                Vector3(80f, explodedGroundY, 80f),
                Vector3(80f, explodedGroundY + wH, 80f),
                Vector3(80f, explodedGroundY + wH, -80f)
            ),
            color = wallColor.copy(alpha = 0.9f),
            depth = 0.5f
        )
    )

    // Front Wall with door aperture details
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-80f, explodedGroundY, 80f),
                Vector3(-25f, explodedGroundY, 80f),
                Vector3(-25f, explodedGroundY + wH, 80f),
                Vector3(-80f, explodedGroundY + wH, 80f)
            ),
            color = wallColor,
            depth = 80f
        )
    )
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(25f, explodedGroundY, 80f),
                Vector3(80f, explodedGroundY, 80f),
                Vector3(80f, explodedGroundY + wH, 80f),
                Vector3(25f, explodedGroundY + wH, 80f)
            ),
            color = wallColor,
            depth = 80f
        )
    )
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-25f, explodedGroundY + wH - 12f, 80f),
                Vector3(25f, explodedGroundY + wH - 12f, 80f),
                Vector3(25f, explodedGroundY + wH, 80f),
                Vector3(-25f, explodedGroundY + wH, 80f)
            ),
            color = wallColor,
            depth = 80f
        )
    )

    // C. Interactive Door rotation plane
    val doorAngle = if (isDoorOpen) PI.toFloat() / 2.2f else 0f
    val doorOriginX = -25f
    val doorOriginZ = 80f
    val doorWidth = 50f
    
    // Rotate door points around origin hinge-point
    val doorP1 = Vector3(doorOriginX, explodedGroundY, doorOriginZ)
    val doorP2 = Vector3(doorOriginX + doorWidth * cos(doorAngle), explodedGroundY, doorOriginZ - doorWidth * sin(doorAngle))
    val doorP3 = Vector3(doorOriginX + doorWidth * cos(doorAngle), explodedGroundY + wH - 12f, doorOriginZ - doorWidth * sin(doorAngle))
    val doorP4 = Vector3(doorOriginX, explodedGroundY + wH - 12f, doorOriginZ)

    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(doorP1, doorP2, doorP3, doorP4),
            color = Color(0xFF8D6E63), // Mahogany wood door
            outlineColor = Color(0xFF3E2723),
            depth = (doorP1.z + doorP2.z) / 2f
        )
    )

    // D. Exploded First Floor Level Slab
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-85f, explodedFirstFloorY, -85f),
                Vector3(85f, explodedFirstFloorY, -85f),
                Vector3(85f, explodedFirstFloorY, 85f),
                Vector3(-85f, explodedFirstFloorY, 85f)
            ),
            color = groundColor.copy(red = groundColor.red * 1.1f),
            depth = explodedFirstFloorY
        )
    )

    // Upper Balcony Railing (Cyan Glass)
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-85f, explodedFirstFloorY, 85f),
                Vector3(85f, explodedFirstFloorY, 85f),
                Vector3(85f, explodedFirstFloorY + 14f, 85f),
                Vector3(-85f, explodedFirstFloorY + 14f, 85f)
            ),
            color = windowColor,
            outlineColor = Color.White.copy(alpha = 0.5f),
            depth = 85f
        )
    )

    // E. Gable Pitched Roof (Apex roof)
    val roofH = 35f
    
    // Back Roof triangular gable
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-80f, explodedRoofY, -80f),
                Vector3(80f, explodedRoofY, -80f),
                Vector3(0f, explodedRoofY + roofH, -80f)
            ),
            color = wallColor,
            depth = -80f
        )
    )

    // Front Roof triangular gable
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-80f, explodedRoofY, 80f),
                Vector3(80f, explodedRoofY, 80f),
                Vector3(0f, explodedRoofY + roofH, 80f)
            ),
            color = wallColor,
            depth = 80f
        )
    )

    // Left pitch slate plane
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-80f, explodedRoofY, -85f),
                Vector3(0f, explodedRoofY + roofH, -85f),
                Vector3(0f, explodedRoofY + roofH, 85f),
                Vector3(-80f, explodedRoofY, 85f)
            ),
            color = roofColor,
            depth = -0.5f
        )
    )

    // Right pitch slate plane
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(80f, explodedRoofY, -85f),
                Vector3(0f, explodedRoofY + roofH, -85f),
                Vector3(0f, explodedRoofY + roofH, 85f),
                Vector3(80f, explodedRoofY, 85f)
            ),
            color = roofColor,
            depth = 0.5f
        )
    )

    // F. Cute interior modular furniture inside CAD drawing
    // Master bed base box on floor
    val bY = explodedGroundY + 1f
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-60f, bY, -60f),
                Vector3(-25f, bY, -60f),
                Vector3(-25f, bY + 10f, -60f),
                Vector3(-60f, bY + 10f, -60f)
            ),
            color = Color(0xFFB0BEC5),
            depth = -60f
        )
    )
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-60f, bY, -30f),
                Vector3(-25f, bY, -30f),
                Vector3(-25f, bY, -60f),
                Vector3(-60f, bY, -60f)
            ),
            color = Color(0xFFA1887F), // wooden mattress frame
            depth = -45f
        )
    )
}

// ==========================================
// 8. PIPELINE TOPOGRAPHY RADAR GENERATOR
// ==========================================

fun generateTopographicPrimitives(
    gridSize: Int,
    water: Float,
    scaleZ: Float,
    time: Float,
    drawPipeline: MutableList<RenderItem3D>
) {
    val bounds = 120f
    val step = (bounds * 2) / gridSize
    
    // Hold mathematical elevation array to calculate vertices
    val elevGrid = Array(gridSize + 1) { FloatArray(gridSize + 1) }
    
    for (r in 0..gridSize) {
        val gx = -bounds + r * step
        for (c in 0..gridSize) {
            val gz = -bounds + c * step
            
            // Procedural mountain peaks using double sine frequency
            val baseVal = sin(gx * 0.024f) * cos(gz * 0.024f) * scaleZ
            val wrinkle = sin(gx * 0.09f + gz * 0.06f) * (scaleZ * 0.25f)
            
            elevGrid[r][c] = baseVal + wrinkle + 15f
        }
    }

    // A. Generate surface terrain polygons
    for (r in 0 until gridSize) {
        val gx1 = -bounds + r * step
        val gx2 = gx1 + step
        for (c in 0 until gridSize) {
            val gz1 = -bounds + c * step
            val gz2 = gz1 + step
            
            val y00 = elevGrid[r][c]
            val y10 = elevGrid[r + 1][c]
            val y11 = elevGrid[r + 1][c + 1]
            val y01 = elevGrid[r][c + 1]
            
            val avgAltitude = (y00 + y10 + y11 + y01) / 4f
            
            // Terrain coloration depending on altitude mapping layers
            val landColor = when {
                avgAltitude > scaleZ * 0.65f + 10f -> Color(0xFFECEFF1) // Snow Peak
                avgAltitude > scaleZ * 0.35f -> Color(0xFF7D6B5D)       // Rock hill
                avgAltitude > water + 2f -> Color(0xFF4CAF50)          // Meadow grassland
                else -> Color(0xFFC2B280)                               // Sandy beach under-water edge
            }

            // Draw elevation grids as quadrilaterals (split to triangular polygons to avoid warp rendering limits)
            drawPipeline.add(
                RenderItem3D.Polygon(
                    pts = listOf(
                        Vector3(gx1, y00, gz1),
                        Vector3(gx2, y10, gz1),
                        Vector3(gx2, y11, gz2)
                    ),
                    color = landColor,
                    outlineColor = Color.Black.copy(alpha = 0.15f),
                    depth = (gz1 + gz2) / 2f
                )
            )
            drawPipeline.add(
                RenderItem3D.Polygon(
                    pts = listOf(
                        Vector3(gx1, y00, gz1),
                        Vector3(gx2, y11, gz2),
                        Vector3(gx1, y01, gz2)
                    ),
                    color = landColor,
                    outlineColor = Color.Black.copy(alpha = 0.15f),
                    depth = (gz1 + gz2) / 2f
                )
            )
        }
    }

    // B. Floating horizontal translucent water plane
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                Vector3(-bounds, water, -bounds),
                Vector3(bounds, water, -bounds),
                Vector3(bounds, water, bounds),
                Vector3(-bounds, water, bounds)
            ),
            color = Color(0x7F03A9F4), // Water alpha layer
            outlineColor = Color(0xFF0288D1).copy(alpha = 0.3f),
            depth = 0f
        )
    )

    // C. Animated Flying Glider Drone above the Swiss peaks!
    val radius = 68f
    val fX = radius * cos(time)
    val fZ = radius * sin(time)
    val fY = 32f + 14f * sin(time * 3f)
    
    // Flying forward vector direction
    val fVelX = -radius * sin(time)
    val fVelZ = radius * cos(time)
    val heading = Vector3(fVelX, -4f * cos(time * 3f), fVelZ).normalize()
    val rightWing = heading.cross(Vector3(0f, 1f, 0f)).normalize() * 15f
    val upFin = heading.cross(rightWing).normalize() * 6f

    val planeCenter = Vector3(fX, fY, fZ)
    val nose = planeCenter + heading * 12f
    val tail = planeCenter - heading * 10f
    
    // Fuselage model shapes
    drawPipeline.add(
        RenderItem3D.Line(
            start = nose,
            end = tail,
            color = Color(0xFFFFEB3B), // Yellow cockpit line
            strokeWidth = 5f,
            depth = fZ
        )
    )
    // Wings
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                planeCenter + heading * 3f,
                planeCenter - rightWing,
                planeCenter - heading * 3f,
                planeCenter + rightWing
            ),
            color = Color(0xFFE91E63), // Pink Wings
            outlineColor = Color.Black.copy(alpha = 0.5f),
            depth = fZ
        )
    )
    // Tail Stabilizer
    drawPipeline.add(
        RenderItem3D.Line(
            start = tail,
            end = tail + upFin,
            color = Color.White,
            strokeWidth = PI.toFloat(),
            depth = fZ
        )
    )
}

// ==========================================
// 9. PIPELINE KINEMATIC MOTORS & GEARS
// ==========================================

fun generateMechanicalGearsAndPiston(
    rot: Float,
    explode: Float,
    drawPipeline: MutableList<RenderItem3D>
) {
    // A. Drawing three intermeshing extruded 3D Spurred Gears!
    // Centering alignments
    val offsetGearZ = 50f * explode
    
    // GEAR 1: Large Red Gear
    drawGearSpurs(
        center = Vector3(-45f, -20f, -offsetGearZ),
        radius = 36f,
        teethCount = 14,
        angle = rot,
        color = Color(0xFFE53935),
        drawPipeline = drawPipeline
    )

    // GEAR 2: Medium Cyan Gear (rotating opposing direction)
    drawGearSpurs(
        center = Vector3(18f, -20f, 0f),
        radius = 27f,
        teethCount = 10,
        angle = -1.4f * rot - 0.22f, // mesh phase offset matches teeth ratio
        color = Color(0xFF00ACC1),
        drawPipeline = drawPipeline
    )

    // GEAR 3: Small Belt bevel core Gear
    drawGearSpurs(
        center = Vector3(18f, 24f, offsetGearZ * 1.5f),
        radius = 18f,
        teethCount = 7,
        angle = 2f * rot,
        color = Color(0xFFFFB300),
        drawPipeline = drawPipeline
    )

    // B. Interactive Vertical Cylinder Piston Slider!
    // Rotates with large shaft
    val pistonCrankRadius = 16f
    val rodLength = 48f
    
    val crankPinH = Vector3(-45f + pistonCrankRadius * cos(rot), -20f + pistonCrankRadius * sin(rot), -offsetGearZ - 10f)
    
    // Piston slider vertical y coordinate: Simple trigonometric slide
    val pistonJointY = (-20f + pistonCrankRadius * sin(rot)) + sqrt(rodLength * rodLength - pistonCrankRadius * pistonCrankRadius * cos(rot) * cos(rot))
    val pistonHeaderCenter = Vector3(-45f, pistonJointY, -offsetGearZ - 10f)

    // Draw crankshaft driving line
    drawPipeline.add(
        RenderItem3D.Line(
            start = Vector3(-45f, -20f, -offsetGearZ - 10f),
            end = crankPinH,
            color = Color.LightGray,
            strokeWidth = 3f,
            depth = -offsetGearZ
        )
    )

    // Draw connecting arm
    drawPipeline.add(
        RenderItem3D.Line(
            start = crankPinH,
            end = pistonHeaderCenter,
            color = Color(0xFF81C784), // Pale Green piston sleeve
            strokeWidth = 3.5f,
            depth = -offsetGearZ
        )
    )

    // Draw piston head solid block cylinder
    val pH = 12f
    val pW = 16f
    drawPipeline.add(
        RenderItem3D.Polygon(
            pts = listOf(
                pistonHeaderCenter + Vector3(-pW, -pH, 0f),
                pistonHeaderCenter + Vector3(pW, -pH, 0f),
                pistonHeaderCenter + Vector3(pW, pH, 0f),
                pistonHeaderCenter + Vector3(-pW, pH, 0f)
            ),
            color = Color(0xFF78909C), // Slate grey alloy piston
            outlineColor = Color.Black.copy(alpha = 0.5f),
            depth = -offsetGearZ - 10f
        )
    )

    // Semi-translucent housing guidelines for Piston Cylinder chamber
    drawPipeline.add(
        RenderItem3D.Line(
            start = Vector3(-45f - pW - 2f, 0f, -offsetGearZ - 10f),
            end = Vector3(-45f - pW - 2f, 65f, -offsetGearZ - 10f),
            color = Color.DarkGray.copy(alpha = 0.4f),
            strokeWidth = 2f,
            depth = -offsetGearZ - 10f
        )
    )
    drawPipeline.add(
        RenderItem3D.Line(
            start = Vector3(-45f + pW + 2f, 0f, -offsetGearZ - 10f),
            end = Vector3(-45f + pW + 2f, 65f, -offsetGearZ - 10f),
            color = Color.DarkGray.copy(alpha = 0.4f),
            strokeWidth = 2f,
            depth = -offsetGearZ - 10f
        )
    )
}

// Draw mechanical spurred cog lines & blocks
private fun drawGearSpurs(
    center: Vector3,
    radius: Float,
    teethCount: Int,
    angle: Float,
    color: Color,
    drawPipeline: MutableList<RenderItem3D>
) {
    val toothWidthAngle = (2 * PI / teethCount).toFloat()
    
    // Draw 3D center hub
    drawPipeline.add(
        RenderItem3D.Sphere(
            center = center,
            radius = radius * 0.25f,
            color = Color.Gray,
            depth = center.z + 2f
        )
    )

    // Assemble outer polygon segments for teeth
    for (i in 0 until teethCount) {
        val currA = angle + i * toothWidthAngle
        val nextA = currA + toothWidthAngle
        
        val pInnerLeft = center + Vector3(radius * 0.75f * cos(currA), radius * 0.75f * sin(currA), 0f)
        val pInnerRight = center + Vector3(radius * 0.75f * cos(currA + toothWidthAngle * 0.4f), radius * 0.75f * sin(currA + toothWidthAngle * 0.4f), 0f)
        val pOuterLeft = center + Vector3(radius * cos(currA + toothWidthAngle * 0.1f), radius * sin(currA + toothWidthAngle * 0.1f), 0f)
        val pOuterRight = center + Vector3(radius * cos(currA + toothWidthAngle * 0.3f), radius * sin(currA + toothWidthAngle * 0.3f), 0f)
        
        // Solid Cog face
        drawPipeline.add(
            RenderItem3D.Polygon(
                pts = listOf(center, pInnerLeft, pOuterLeft, pOuterRight, pInnerRight),
                color = color,
                outlineColor = Color.Black.copy(alpha = 0.25f),
                depth = center.z
            )
        )
        
        // Add extruded line thickness to give it 3D width!
        val thicknessZ = -8f
        val pOutExtLeft = pOuterLeft + Vector3(0f, 0f, thicknessZ)
        val pOutExtRight = pOuterRight + Vector3(0f, 0f, thicknessZ)
        
        drawPipeline.add(
            RenderItem3D.Polygon(
                pts = listOf(pOuterLeft, pOuterRight, pOutExtRight, pOutExtLeft),
                color = color.copy(red = color.red * 0.82f),
                outlineColor = Color.Black.copy(alpha = 0.3f),
                depth = center.z + thicknessZ / 2f
            )
        )
    }
}

// ==========================================
// 10. INTERACTIVE RUBIK'S CUBE MODEL ENGINE
// ==========================================

data class RubikMinisc(
    val gridX: Int, // -1, 0, 1
    val gridY: Int, // -1, 0, 1
    val gridZ: Int, // -1, 0, 1
    // Face colors mapping (Right, Left, Top, Bottom, Front, Back)
    val colors: List<Color>
)

fun initRubikCube(): List<RubikMinisc> {
    val list = mutableListOf<RubikMinisc>()
    for (x in -1..1) {
        for (y in -1..1) {
            for (z in -1..1) {
                // Determine face colors (only outer faces show standard colors)
                val rightCol = if (x == 1) Color(0xFFD84315) else Color(0xFF212121)  // Orange
                val leftCol = if (x == -1) Color(0xFFC62828) else Color(0xFF212121)   // Red
                val topCol = if (y == 1) Color(0xFFECEFF1) else Color(0xFF212121)     // White
                val bottomCol = if (y == -1) Color(0xFFFBC02D) else Color(0xFF212121) // Yellow
                val frontCol = if (z == 1) Color(0xFF2E7D32) else Color(0xFF212121)   // Green
                val backCol = if (z == -1) Color(0xFF1565C0) else Color(0xFF212121)   // Blue
                
                list.add(
                    RubikMinisc(
                        gridX = x,
                        gridY = y,
                        gridZ = z,
                        colors = listOf(rightCol, leftCol, topCol, bottomCol, frontCol, backCol)
                    )
                )
            }
        }
    }
    return list
}

fun rotateRubikSlice(
    cube: List<RubikMinisc>,
    axis: String,
    sliceIndex: Int,
    clockwise: Boolean
): List<RubikMinisc> {
    return cube.map { minisc ->
        val match = when (axis) {
            "X" -> minisc.gridX == sliceIndex
            "Y" -> minisc.gridY == sliceIndex
            "Z" -> minisc.gridZ == sliceIndex
            else -> false
        }
        
        if (match) {
            // Apply coordinates rotation mapping indices & swap face color segments
            when (axis) {
                "Y" -> {
                    // Yaw rotation in grid coordinates
                    val nx = if (clockwise) -minisc.gridZ else minisc.gridZ
                    val nz = if (clockwise) minisc.gridX else -minisc.gridX
                    
                    // Remap face colors: Y axis retains top/bottom, swaps R, L, F, B
                    // Colors sequence: 0:Right(+X), 1:Left(-X), 2:Top(+Y), 3:Bottom(-Y), 4:Front(+Z), 5:Back(-Z)
                    val r = minisc.colors[0]
                    val l = minisc.colors[1]
                    val t = minisc.colors[2]
                    val b = minisc.colors[3]
                    val f = minisc.colors[4]
                    val k = minisc.colors[5]
                    
                    val newColors = if (clockwise) {
                        listOf(k, f, t, b, r, l) // rotate Right -> Back -> Left -> Front -> Right
                    } else {
                        listOf(f, k, t, b, l, r)
                    }
                    minisc.copy(gridX = nx, gridZ = nz, colors = newColors)
                }
                "X" -> {
                    // Pitch rotation around X axis
                    val ny = if (clockwise) minisc.gridZ else -minisc.gridZ
                    val nz = if (clockwise) -minisc.gridY else minisc.gridY
                    
                    val r = minisc.colors[0]
                    val l = minisc.colors[1]
                    val t = minisc.colors[2]
                    val b = minisc.colors[3]
                    val f = minisc.colors[4]
                    val k = minisc.colors[5]
                    
                    // Rotates T, B, F, B - retains Right, Left
                    val newColors = if (clockwise) {
                        listOf(r, l, f, k, b, t)
                    } else {
                        listOf(r, l, k, f, t, b)
                    }
                    minisc.copy(gridY = ny, gridZ = nz, colors = newColors)
                }
                "Z" -> {
                    // Roll rotation around Z axis
                    val nx = if (clockwise) minisc.gridY else -minisc.gridY
                    val ny = if (clockwise) -minisc.gridX else minisc.gridX
                    
                    val r = minisc.colors[0]
                    val l = minisc.colors[1]
                    val t = minisc.colors[2]
                    val b = minisc.colors[3]
                    val f = minisc.colors[4]
                    val k = minisc.colors[5]
                    
                    // Rotates R, L, T, B - retains Front, Back
                    val newColors = if (clockwise) {
                        listOf(b, t, r, l, f, k)
                    } else {
                        listOf(t, b, l, r, f, k)
                    }
                    minisc.copy(gridX = nx, gridY = ny, colors = newColors)
                }
                else -> minisc
            }
        } else {
            minisc
        }
    }
}

fun generateRubikCubePrimitives(
    cubes: List<RubikMinisc>,
    drawPipeline: MutableList<RenderItem3D>
) {
    val step = 32f // dimensional box size
    
    cubes.forEach { minisc ->
        // Continuous physical rendering center coordinate
        val c = Vector3(
            x = minisc.gridX * step,
            y = minisc.gridY * step,
            z = minisc.gridZ * step
        )
        
        val h = step / 2f
        
        // Define 8 vertices of the cubelet relative to centre
        val v000 = c + Vector3(-h, -h, -h)
        val v100 = c + Vector3(h, -h, -h)
        val v110 = c + Vector3(h, h, -h)
        val v010 = c + Vector3(-h, h, -h)
        
        val v001 = c + Vector3(-h, -h, h)
        val v101 = c + Vector3(h, -h, h)
        val v111 = c + Vector3(h, h, h)
        val v011 = c + Vector3(-h, h, h)

        val outCol = Color.Black.copy(alpha = 0.9f)
        val strokeW = 2.5f

        // Create 6 Polygon planes with respective face indices
        // index mapping options: 0:Right(+X), 1:Left(-X), 2:Top(+Y), 3:Bottom(-Y), 4:Front(+Z), 5:Back(-Z)
        
        // 1. Right (+X)
        drawPipeline.add(
            RenderItem3D.Polygon(
                pts = listOf(v100, v110, v111, v101),
                color = minisc.colors[0],
                outlineColor = outCol,
                strokeWidth = strokeW,
                depth = c.z
            )
        )
        // 2. Left (-X)
        drawPipeline.add(
            RenderItem3D.Polygon(
                pts = listOf(v000, v001, v011, v010),
                color = minisc.colors[1],
                outlineColor = outCol,
                strokeWidth = strokeW,
                depth = c.z
            )
        )
        // 3. Top (+Y)
        drawPipeline.add(
            RenderItem3D.Polygon(
                pts = listOf(v010, v011, v111, v110),
                color = minisc.colors[2],
                outlineColor = outCol,
                strokeWidth = strokeW,
                depth = c.z
            )
        )
        // 4. Bottom (-Y)
        drawPipeline.add(
            RenderItem3D.Polygon(
                pts = listOf(v000, v100, v101, v001),
                color = minisc.colors[3],
                outlineColor = outCol,
                strokeWidth = strokeW,
                depth = c.z
            )
        )
        // 5. Front (+Z)
        drawPipeline.add(
            RenderItem3D.Polygon(
                pts = listOf(v001, v101, v111, v011),
                color = minisc.colors[4],
                outlineColor = outCol,
                strokeWidth = strokeW,
                depth = c.z
            )
        )
        // 6. Back (-Z)
        drawPipeline.add(
            RenderItem3D.Polygon(
                pts = listOf(v000, v010, v110, v100),
                color = minisc.colors[5],
                outlineColor = outCol,
                strokeWidth = strokeW,
                depth = c.z
            )
        )
    }
}

// Check if all faces on each of the six planes of Rubik's Cube have homogeneous colors
fun checkIfRubikSolved(cube: List<RubikMinisc>): Boolean {
    // Check Right Face (+X = 1)
    val rightCubes = cube.filter { it.gridX == 1 }
    if (rightCubes.isEmpty()) return false
    val rightColor = rightCubes.first().colors[0]
    if (!rightCubes.all { it.colors[0] == rightColor }) return false

    // Check Left Face (-X = -1)
    val leftCubes = cube.filter { it.gridX == -1 }
    if (leftCubes.isEmpty()) return false
    val leftColor = leftCubes.first().colors[1]
    if (!leftCubes.all { it.colors[1] == leftColor }) return false

    // Check Top Face (+Y = 1)
    val topCubes = cube.filter { it.gridY == 1 }
    if (topCubes.isEmpty()) return false
    val topColor = topCubes.first().colors[2]
    if (!topCubes.all { it.colors[2] == topColor }) return false

    // Check Bottom Face (-Y = -1)
    val bottomCubes = cube.filter { it.gridY == -1 }
    if (bottomCubes.isEmpty()) return false
    val bottomColor = bottomCubes.first().colors[3]
    if (!bottomCubes.all { it.colors[3] == bottomColor }) return false

    // Check Front Face (+Z = 1)
    val frontCubes = cube.filter { it.gridZ == 1 }
    if (frontCubes.isEmpty()) return false
    val frontColor = frontCubes.first().colors[4]
    if (!frontCubes.all { it.colors[4] == frontColor }) return false

    // Check Back Face (-Z = -1)
    val backCubes = cube.filter { it.gridZ == -1 }
    if (backCubes.isEmpty()) return false
    val backColor = backCubes.first().colors[5]
    if (!backCubes.all { it.colors[5] == backColor }) return false

    return true
}

@Composable
fun RubikHudStatsOverlay(
    moves: Int,
    seconds: Int,
    isSolved: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.border(1.dp, Color(0xFF4AF2A1).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "📊 PUZZLE PROGRESS",
                    color = Color(0xFF4AF2A1),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Moves Made: $moves",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
                val min = seconds / 60
                val sec = seconds % 60
                val minStr = if (min < 10) "0$min" else "$min"
                val secStr = if (sec < 10) "0$sec" else "$sec"
                val timeString = "$minStr:$secStr"
                Text(
                    "Time elapsed: $timeString",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
                if (isSolved) {
                    Text(
                        "🎉 SOLVED!",
                        color = Color(0xFF4AF2A1),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RubikSolvedCelebrationCard(
    moves: Int,
    seconds: Int,
    onPlayAgain: () -> Unit,
    onScramble: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp)
                .border(2.dp, Color(0xFF4AF2A1), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🏆 Congratulations!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF4AF2A1)),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "You solved the 3D Rubik's Cube!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                
                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Moves", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("$moves", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val min = seconds / 60
                        val sec = seconds % 60
                        val minStr = if (min < 10) "0$min" else "$min"
                        val secStr = if (sec < 10) "0$sec" else "$sec"
                        val timeString = "$minStr:$secStr"
                        Text("Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(timeString, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace))
                    }
                }
                
                Spacer(Modifier.height(18.dp))
                
                Button(
                    onClick = onScramble,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4AF2A1), contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Scramble & Replay")
                }
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Board")
                }
            }
        }
    }
}
