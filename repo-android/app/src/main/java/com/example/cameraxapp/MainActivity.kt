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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.navigation.compose.rememberNavController
import com.example.cameraxapp.ui.theme.CameraXAppTheme

class MainActivity : ComponentActivity() {

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        cameraPermissionGranted.value = isGranted
    }

    private val cameraPermissionGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updatePermissionState()

        setContent {
            val repository = remember { SettingsRepository(this) }
            val themeMode by repository.themeMode.collectAsState(initial = 0)
            val useDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            CameraXAppTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                val isGranted by cameraPermissionGranted

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "hub") {
                        composable("hub") {
                            HubScreen(navController)
                        }
                        composable("camera") {
                            if (isGranted) {
                                CameraScreen(onBack = { navController.popBackStack() })
                            } else {
                                PermissionRequestScreen(
                                    onRequestPermission = {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                )
                            }
                        }
                        composable("explorer") {
                            ExplorerScreen(onBack = { navController.popBackStack() })
                        }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    private fun updatePermissionState() {
        cameraPermissionGranted.value = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
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
fun HubScreen(navController: NavController) {
    val applets = listOf(
        AppletInfo("Camera", "camera", Icons.Default.PlayArrow, "Capture photos with CameraX"),
        AppletInfo("Explorer", "explorer", Icons.AutoMirrored.Filled.List, "Browse local files"),
        AppletInfo("Settings", "settings", Icons.Default.Settings, "Global app configuration")
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Multi-App Hub") })
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
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

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera permission is required to use this app.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}
