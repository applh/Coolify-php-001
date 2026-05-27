package com.example.cameraxapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cameraxapp.ui.theme.CameraXAppTheme
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.launch

import com.example.cameraxapp.core.di.AppDependencyContainer
import com.example.cameraxapp.core.framework.AppletRegistry
import com.example.cameraxapp.core.framework.impl.BrowserApplet
import com.example.cameraxapp.core.framework.impl.FilesApplet
import com.example.cameraxapp.core.framework.impl.DebugApplet
import com.example.cameraxapp.core.framework.impl.BlackjackApplet
import com.example.cameraxapp.core.framework.impl.RoguelikeApplet

class MainActivity : ComponentActivity() {

    private lateinit var appDependencyContainer: AppDependencyContainer

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else {
            true
        }
        permissionsGranted.value = cameraGranted && audioGranted && notificationsGranted
    }

    private val permissionsGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.init(applicationContext)

        appDependencyContainer = AppDependencyContainer(applicationContext)
        AppletRegistry.register(BrowserApplet(appDependencyContainer))
        AppletRegistry.register(FilesApplet())
        AppletRegistry.register(DebugApplet())
        AppletRegistry.register(BlackjackApplet())
        AppletRegistry.register(RoguelikeApplet())

        updatePermissionState()
        
        // Enforce AlarmManager scheduling for seeded items locally on boot
        Thread {
            try {
                val dbHelper = AgendaDatabaseHelper(applicationContext)
                val cronJobs = dbHelper.getAllCronJobs()
                for (cron in cronJobs) {
                    if (cron.isActive) {
                        var intervalMinutes = 15L
                        if (cron.cronExpression.startsWith("*/")) {
                            val mins = cron.cronExpression.substringAfter("*/").substringBefore(" ").toLongOrNull()
                            if (mins != null && mins >= 1L) {
                                intervalMinutes = mins
                            }
                        }
                        CronScheduler.scheduleExact(applicationContext, cron.id, intervalMinutes)
                    }
                }
                
                // Sync user-managed cron jobs
                com.example.cameraxapp.cronjob.CronJobScheduler.syncJobsFromDatabase(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

        setContent {
            val repository = remember { appDependencyContainer.settingsRepository }
            val themeMode by repository.themeMode.collectAsState(initial = 0)
            val colorTheme by repository.colorTheme.collectAsState(initial = 0)
            val useDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            CameraXAppTheme(darkTheme = useDarkTheme, colorTheme = colorTheme) {
                val navController = rememberNavController()
                val isGranted by permissionsGranted
                val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "hub"
                
                val activeAppletJson by repository.launcherActiveApplets.collectAsState(initial = "")
                val orderJson by repository.launcherAppletOrder.collectAsState(initial = "")
                val startupDefaultRoute by repository.startupDefaultRoute.collectAsState(initial = "hub")

                val allMasterApplets = remember {
                    listOf(
                        AppletInfo("Camera", "camera", Icons.Default.PlayArrow, "Capture photos with CameraX"),
                        AppletInfo("Files", "files", Icons.Default.Menu, "Browse local files"),
                        AppletInfo("AI Team", "ai_team", Icons.Default.Create, "AI chat and generation"),
                        AppletInfo("Cronjobs", "cronjobs", Icons.Default.DateRange, "Manage background cron tasks"),
                        AppletInfo("DB SQLite", "db", Icons.AutoMirrored.Filled.List, "Inspect and edit database files"),
                        AppletInfo("Agenda", "agenda", Icons.Default.DateRange, "Calendar planner and alarm schedules"),
                        AppletInfo("Wallpaper", "wallpaper", Icons.Default.Star, "Manage auto-rotating wallpapers"),
                        AppletInfo("Backup Manager", "backup", Icons.Default.Refresh, "Secure system state saves and database ZIP packing"),
                        AppletInfo("Settings", "settings", Icons.Default.Settings, "Global app configuration"),
                        AppletInfo("Browser", "browser", Icons.Default.Search, "Web tools with safe JS sandbox script injection"),
                        AppletInfo("Debug Logs", "debug", Icons.Default.Build, "View system logs, WebView errors, exceptions and diagnostics"),
                        AppletInfo("Blackjack", "blackjack", Icons.Default.Star, "Vegas-style cards game with strategy helper advisor"),
                        AppletInfo("RogueCompose", "roguecompose", Icons.Default.Star, "Symmetrical turn-based procedural rogue-like RPG adventure with SQLite persistent saving")
                    )
                }

                val currentOrderedAndFilteredApplets = remember(activeAppletJson, orderJson) {
                    getOrderedAndFilteredApplets(allMasterApplets, activeAppletJson, orderJson)
                }

                val drawerApplets = remember(currentOrderedAndFilteredApplets) {
                    listOf(AppletInfo("Home", "hub", Icons.Default.Home, "Main Hub")) + currentOrderedAndFilteredApplets
                }

                var hasRedirectedByStartupKey by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(startupDefaultRoute) {
                    if (!hasRedirectedByStartupKey && startupDefaultRoute.isNotEmpty() && startupDefaultRoute != "hub") {
                        hasRedirectedByStartupKey = true
                        navController.navigate(startupDefaultRoute) {
                            popUpTo("hub") {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                }

                ModalNavigationDrawer(
                    drawerState = leftDrawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "🍓FRAISE",
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(8.dp))
                            drawerApplets.forEach { applet ->
                                NavigationDrawerItem(
                                    icon = { Icon(applet.icon, contentDescription = null) },
                                    label = { Text(applet.name) },
                                    selected = currentRoute == applet.route,
                                    onClick = {
                                        scope.launch { leftDrawerState.close() }
                                        if (currentRoute != applet.route) {
                                            if (applet.route == "hub") {
                                                navController.popBackStack("hub", inclusive = false)
                                            } else {
                                                navController.navigate(applet.route) {
                                                    popUpTo("hub") {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    }
                ) {
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                        ModalNavigationDrawer(
                            drawerState = rightDrawerState,
                            drawerContent = {
                                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                                    ModalDrawerSheet(
                                        modifier = Modifier.width(300.dp)
                                    ) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = "Quick Tools",
                                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        drawerApplets.forEach { applet ->
                                            NavigationDrawerItem(
                                                icon = { Icon(applet.icon, contentDescription = null) },
                                                label = { Text(applet.name) },
                                                selected = currentRoute == applet.route,
                                                onClick = {
                                                    scope.launch { rightDrawerState.close() }
                                                    if (currentRoute != applet.route) {
                                                        if (applet.route == "hub") {
                                                            navController.popBackStack("hub", inclusive = false)
                                                        } else {
                                                            navController.navigate(applet.route) {
                                                                popUpTo("hub") {
                                                                    saveState = true
                                                                }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        NavHost(navController = navController, startDestination = "hub", modifier = Modifier.fillMaxSize()) {
                                            composable("hub") {
                                                HubScreen(
                                                    navController = navController,
                                                    repository = repository,
                                                    onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                                                    onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
                                                )
                                            }
                                            composable("camera") {
                                                if (isGranted) {
                                                    CameraScreen(
                                                        onBack = { navController.popBackStack() },
                                                        onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                                                        onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
                                                    )
                                                } else {
                                                    PermissionRequestScreen(
                                                        onRequestPermission = {
                                                            val perms = mutableListOf(
                                                                Manifest.permission.CAMERA,
                                                                Manifest.permission.RECORD_AUDIO
                                                            )
                                                            if (android.os.Build.VERSION.SDK_INT >= 33) {
                                                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                                            }
                                                            permissionsLauncher.launch(perms.toTypedArray())
                                                        },
                                                        onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                                                        onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
                                                    )
                                                }
                                            }
                                            // Dynamic loop handling for registered platform plugin applets
                                            com.example.cameraxapp.core.framework.AppletRegistry.registeredApplets.forEach { applet ->
                                                composable(applet.id) {
                                                    applet.Content(
                                                        navController = navController,
                                                        onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                                                        onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
                                                    )
                                                }
                                            }
                                            composable("ai_team") {
                                                AITeamScreen(
                                                    onBack = { navController.popBackStack() },
                                                    onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                                                    onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
                                                )
                                            }
                                            composable("cronjobs") {
                                                com.example.cameraxapp.cronjob.CronJobManagerScreen(
                                                    onBack = { navController.popBackStack() },
                                                    onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                                                    onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
                                                )
                                            }
                                            composable("db") {
                                                DBScreen(
                                                    onBack = { navController.popBackStack() },
                                                    onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                                                    onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
                                                )
                                            }
                                            composable("agenda") {
                                                AgendaScreen(
                                                    onBack = { navController.popBackStack() },
                                                    onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                                                    onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
                                                )
                                            }
                                            composable("wallpaper") {
                                                WallpaperScreen(
                                                    onBack = { navController.popBackStack() },
                                                    onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                                                    onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
                                                )
                                            }
                                            composable("backup") {
                                                BackupScreen(
                                                    onBack = { navController.popBackStack() },
                                                    onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                                                    onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
                                                )
                                            }
                                            composable("settings") {
                                                SettingsScreen(
                                                    onBack = { navController.popBackStack() },
                                                    onOpenDrawer = { scope.launch { leftDrawerState.open() } },
                                                    onOpenRightDrawer = { scope.launch { rightDrawerState.open() } }
                                                )
                                            }
                                            // Static fallback browser route removed and delegated fully to AppletRegistry plugins pipeline
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

    private fun updatePermissionState() {
        val cameraGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        permissionsGranted.value = cameraGranted && audioGranted && notificationsGranted
    }
}

data class AppletInfo(
    val name: String,
    val route: String,
    val icon: ImageVector,
    val description: String
)

@Composable
fun getAppletColor(route: String): androidx.compose.ui.graphics.Color {
    return when (route) {
        "camera" -> androidx.compose.ui.graphics.Color(0xFFFF8A80)      // Soft Red
        "files" -> androidx.compose.ui.graphics.Color(0xFF82B1FF)       // Soft Blue
        "ai_team" -> androidx.compose.ui.graphics.Color(0xFFEA80FC)     // Soft Violet
        "cronjobs" -> androidx.compose.ui.graphics.Color(0xFFFFD180)    // Soft Orange
        "db" -> androidx.compose.ui.graphics.Color(0xFF84FFFF)          // Soft Cyan
        "agenda" -> androidx.compose.ui.graphics.Color(0xFFB9F6CA)      // Soft Green
        "wallpaper" -> androidx.compose.ui.graphics.Color(0xFFFF80AB)   // Soft Pink
        "backup" -> androidx.compose.ui.graphics.Color(0xFFA7FFEB)      // Soft Teal
        "settings" -> androidx.compose.ui.graphics.Color(0xFFCFD8DC)    // Blue Grey
        "browser" -> androidx.compose.ui.graphics.Color(0xFFFFE082)     // Soft Gold/Yellow
        "blackjack" -> androidx.compose.ui.graphics.Color(0xFF80C080)   // Casino Light Felt Green
        else -> androidx.compose.ui.graphics.Color(0xFFCFD8DC)          // Fallback
    }
}

fun getOrderedAndFilteredApplets(
    allApplets: List<AppletInfo>,
    activeJson: String,
    orderJson: String
): List<AppletInfo> {
    val activeRoutes = if (activeJson.isEmpty()) {
        allApplets.map { it.route }.toSet()
    } else {
        try {
            val arr = org.json.JSONArray(activeJson)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                set.add(arr.getString(i))
            }
            set.add("settings") // Settings is protected, can never lock out!
            set
        } catch (e: Exception) {
            allApplets.map { it.route }.toSet()
        }
    }

    val filteredApplets = allApplets.filter { activeRoutes.contains(it.route) }

    if (orderJson.isEmpty()) {
        return filteredApplets
    } else {
        try {
            val arr = org.json.JSONArray(orderJson)
            val orderList = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                orderList.add(arr.getString(i))
            }
            val activeOrdered = orderList.filter { activeRoutes.contains(it) }
            val orderedFiltered = mutableListOf<AppletInfo>()
            activeOrdered.forEach { route ->
                val applet = filteredApplets.find { it.route == route }
                if (applet != null) {
                    orderedFiltered.add(applet)
                }
            }
            filteredApplets.forEach { applet ->
                if (!orderedFiltered.contains(applet)) {
                    orderedFiltered.add(applet)
                }
            }
            return orderedFiltered
        } catch (e: Exception) {
            return filteredApplets
        }
    }
}

@Composable
fun CircularAppletCard(
    applet: AppletInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = getAppletColor(applet.route)
    Column(
        modifier = modifier
            .width(68.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accentColor)
                .border(1.5.dp, accentColor.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = applet.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = androidx.compose.ui.graphics.Color.Black
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = applet.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.3.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    navController: NavController,
    repository: SettingsRepository,
    onOpenDrawer: () -> Unit,
    onOpenRightDrawer: () -> Unit
) {
    val activeAppletJson by repository.launcherActiveApplets.collectAsState(initial = "")
    val orderJson by repository.launcherAppletOrder.collectAsState(initial = "")
    val scope = rememberCoroutineScope()

    val allApplets = remember {
        listOf(
            AppletInfo("Camera", "camera", Icons.Default.PlayArrow, "Capture photos with CameraX"),
            AppletInfo("Files", "files", Icons.Default.Menu, "Browse local files"),
            AppletInfo("AI Team", "ai_team", Icons.Default.Create, "AI chat and generation"),
            AppletInfo("Cronjobs", "cronjobs", Icons.Default.DateRange, "Manage background cron tasks"),
            AppletInfo("DB SQLite", "db", Icons.AutoMirrored.Filled.List, "Inspect and edit database files"),
            AppletInfo("Agenda", "agenda", Icons.Default.DateRange, "Calendar planner and alarm schedules"),
            AppletInfo("Wallpaper", "wallpaper", Icons.Default.Star, "Manage auto-rotating wallpapers"),
            AppletInfo("Backup Manager", "backup", Icons.Default.Refresh, "Secure system state saves and database ZIP packing"),
            AppletInfo("Settings", "settings", Icons.Default.Settings, "Global app configuration"),
            AppletInfo("Browser", "browser", Icons.Default.Search, "Web tools with safe JS sandbox script injection"),
            AppletInfo("Debug Logs", "debug", Icons.Default.Build, "View system logs, WebView errors, exceptions and diagnostics"),
            AppletInfo("Blackjack", "blackjack", Icons.Default.Star, "Vegas-style cards game with strategy helper advisor")
        )
    }

    val currentSet = remember(activeAppletJson, orderJson) {
        getOrderedAndFilteredApplets(allApplets, activeAppletJson, orderJson)
    }

    val mutableAppletsList = remember { mutableStateListOf<AppletInfo>() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    LaunchedEffect(currentSet) {
        if (draggingIndex == null) {
            mutableAppletsList.clear()
            mutableAppletsList.addAll(currentSet)
        }
    }

    val itemPositions = remember { mutableStateMapOf<Int, androidx.compose.ui.layout.LayoutCoordinates>() }

    Scaffold(
        bottomBar = {
            TopAppBar(
                title = { Text("🍓FRAISE") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenRightDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Quick Tools")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val cols = if (maxWidth > 600.dp) 8 else 5
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = mutableAppletsList,
                    key = { _, item -> item.route }
                ) { index, applet ->
                    val dragging = draggingIndex == index
                    val cardOffset = if (dragging) dragOffset else androidx.compose.ui.geometry.Offset.Zero

                    val itemModifier = if (dragging) {
                        Modifier
                    } else {
                        @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                        @Suppress("DEPRECATION")
                        Modifier.animateItemPlacement()
                    }

                    CircularAppletCard(
                        applet = applet,
                        onClick = {
                            if (draggingIndex == null) {
                                navController.navigate(applet.route)
                            }
                        },
                        modifier = itemModifier
                            .onGloballyPositioned { coordinates ->
                                itemPositions[index] = coordinates
                            }
                            .graphicsLayer {
                                translationX = cardOffset.x
                                translationY = cardOffset.y
                                scaleX = if (dragging) 1.2f else 1.0f
                                scaleY = if (dragging) 1.2f else 1.0f
                                alpha = if (dragging) 0.85f else 1.0f
                                shadowElevation = if (dragging) 8f else 0f
                            }
                            .pointerInput(index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { _ ->
                                        draggingIndex = index
                                        dragOffset = androidx.compose.ui.geometry.Offset.Zero
                                    },
                                    onDragEnd = {
                                        val finalIndex = draggingIndex
                                        draggingIndex = null
                                        dragOffset = androidx.compose.ui.geometry.Offset.Zero
                                        if (finalIndex != null) {
                                            scope.launch {
                                                val orderRoutes = mutableAppletsList.map { it.route }
                                                val jsonArray = org.json.JSONArray(orderRoutes)
                                                repository.setLauncherAppletOrder(jsonArray.toString())
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragOffset = androidx.compose.ui.geometry.Offset.Zero
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount

                                        val dragIdx = draggingIndex
                                        if (dragIdx != null && dragIdx in 0..mutableAppletsList.lastIndex) {
                                            val currentBounds = itemPositions[dragIdx]?.boundsInWindow()
                                            if (currentBounds != null) {
                                                val centerX = currentBounds.left + currentBounds.width / 2f + dragOffset.x
                                                val centerY = currentBounds.top + currentBounds.height / 2f + dragOffset.y

                                                var targetIdx = -1
                                                for (i in 0..mutableAppletsList.lastIndex) {
                                                    if (i != dragIdx) {
                                                        val bounds = itemPositions[i]?.boundsInWindow()
                                                        if (bounds != null && bounds.contains(
                                                                androidx.compose.ui.geometry.Offset(centerX, centerY)
                                                            )
                                                        ) {
                                                            targetIdx = i
                                                            break
                                                        }
                                                    }
                                                }

                                                if (targetIdx != -1) {
                                                    val oldC = itemPositions[dragIdx]?.positionInRoot()
                                                    val newC = itemPositions[targetIdx]?.positionInRoot()

                                                    val temp = mutableAppletsList[dragIdx]
                                                    mutableAppletsList[dragIdx] = mutableAppletsList[targetIdx]
                                                    mutableAppletsList[targetIdx] = temp

                                                    if (oldC != null && newC != null) {
                                                        dragOffset = androidx.compose.ui.geometry.Offset(
                                                            x = dragOffset.x + (oldC.x - newC.x),
                                                            y = dragOffset.y + (oldC.y - newC.y)
                                                        )
                                                    }
                                                    draggingIndex = targetIdx
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppletCard(applet: AppletInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = applet.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = applet.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = applet.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit, onOpenDrawer: () -> Unit, onOpenRightDrawer: () -> Unit) {
    Scaffold(
        bottomBar = {
            TopAppBar(
                title = { Text("Camera Permission") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenRightDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Quick Tools")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Camera, Audio, and Notification permissions are required to use this app.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}
