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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

        setContent {
            val repository = remember { SettingsRepository(this) }
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
                
                val applets = listOf(
                    AppletInfo("Home", "hub", Icons.Default.Home, "Main Hub"),
                    AppletInfo("Camera", "camera", Icons.Default.PlayArrow, "Capture photos with CameraX"),
                    AppletInfo("Explorer", "explorer", Icons.Default.Menu, "Browse local files"),
                    AppletInfo("AI Team", "ai_team", Icons.Default.Create, "AI chat and generation"),
                    AppletInfo("DB SQLite", "db", Icons.AutoMirrored.Filled.List, "Inspect and edit database files"),
                    AppletInfo("Agenda", "agenda", Icons.Default.DateRange, "Calendar planner and alarm schedules"),
                    AppletInfo("Wallpaper", "wallpaper", Icons.Default.Star, "Manage auto-rotating wallpapers"),
                    AppletInfo("Settings", "settings", Icons.Default.Settings, "Global app configuration")
                )

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
                            applets.forEach { applet ->
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
                                        applets.forEach { applet ->
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
                                                HubScreen(navController, onOpenDrawer = { scope.launch { leftDrawerState.open() } })
                                            }
                                            composable("camera") {
                                                if (isGranted) {
                                                    CameraScreen(onBack = { navController.popBackStack() }, onOpenDrawer = { scope.launch { leftDrawerState.open() } })
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
                                                        onOpenDrawer = { scope.launch { leftDrawerState.open() } }
                                                    )
                                                }
                                            }
                                            composable("explorer") {
                                                ExplorerScreen(onBack = { navController.popBackStack() }, onOpenDrawer = { scope.launch { leftDrawerState.open() } })
                                            }
                                            composable("ai_team") {
                                                AITeamScreen(onBack = { navController.popBackStack() }, onOpenDrawer = { scope.launch { leftDrawerState.open() } })
                                            }
                                            composable("db") {
                                                DBScreen(onBack = { navController.popBackStack() }, onOpenDrawer = { scope.launch { leftDrawerState.open() } })
                                            }
                                            composable("agenda") {
                                                AgendaScreen(onBack = { navController.popBackStack() }, onOpenDrawer = { scope.launch { leftDrawerState.open() } })
                                            }
                                            composable("wallpaper") {
                                                WallpaperScreen(onBack = { navController.popBackStack() }, onOpenDrawer = { scope.launch { leftDrawerState.open() } })
                                            }
                                            composable("settings") {
                                                SettingsScreen(onBack = { navController.popBackStack() }, onOpenDrawer = { scope.launch { leftDrawerState.open() } })
                                            }
                                        }
                                        
                                        // Floating Action Button on Center Right
                                        SmallFloatingActionButton(
                                            onClick = { scope.launch { rightDrawerState.open() } },
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 0.dp),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                                topStart = 8.dp,
                                                bottomStart = 8.dp,
                                                topEnd = 0.dp,
                                                bottomEnd = 0.dp
                                            )
                                        ) {
                                            Icon(Icons.Default.Menu, contentDescription = "Quick Tools")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(navController: NavController, onOpenDrawer: () -> Unit) {
    val applets = listOf(
        AppletInfo("Camera", "camera", Icons.Default.PlayArrow, "Capture photos with CameraX"),
        AppletInfo("Explorer", "explorer", Icons.Default.Menu, "Browse local files"),
        AppletInfo("AI Team", "ai_team", Icons.Default.Create, "AI chat and generation"),
        AppletInfo("DB SQLite", "db", Icons.AutoMirrored.Filled.List, "Inspect and edit database files"),
        AppletInfo("Agenda", "agenda", Icons.Default.DateRange, "Calendar planner and alarm schedules"),
        AppletInfo("Wallpaper", "wallpaper", Icons.Default.Star, "Manage auto-rotating wallpapers"),
        AppletInfo("Settings", "settings", Icons.Default.Settings, "Global app configuration")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🍓FRAISE") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.padding(padding).fillMaxSize()) {
            val cols = if (maxWidth > 600.dp) 3 else 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(applets) { applet ->
                    AppletCard(applet) {
                        navController.navigate(applet.route)
                    }
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
fun PermissionRequestScreen(onRequestPermission: () -> Unit, onOpenDrawer: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera Permission") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
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
