# Hub Launcher UX Upgrades: Drag & Drop, Circular Cards, & Startup Routing

This document defines the technical architecture and step-by-step implementation plan to upgrade the **🍓 FRAISE Hub Launcher** within the `repo-android` codebase. These upgrades modernize the hub layout, provide customizable grid reordering, enable selectively turning off secondary applets, and introduce personalized startup routing.

---

## 1. Feature Specifications

### 🟡 Circular Icon Applet Cards
- **OS-Style Aesthetics:** Transition from bulky, rectangular cards to clean, circular launchers resembling modern operating system desktops (e.g., Android Pixel Launcher, iOS).
- **Unique Applet Color Accents:** Assign a distinct, dedicated background color block to each applet card (e.g., Warm Rose for Camera, Deep Blue for Explorer, Mystic Purple for AI Team, Amber for Cronjobs, etc.) to enhance recognition and cognitive processing speed.
- **Micro-Scale Target Circles:** Scale the circle diameter down to exactly **48.dp** (with 24.dp vector icons inside) and set compact item padding and captions using tiny, high-contrast typography (`labelSmall`, `10.sp`) to maximize screen area utility.
- **Ultra-High Grid Density:** Utilize a 5-column grid on standard viewports (or dynamic adaptive sizing `GridCells.Adaptive(minSize = 64.dp)`) with tight `8.dp` gutters. This layout ensures up to **20 applets** are visible concurrently in a single screen area without requiring any scrolling.
- **Separated Captions:** Positional labels (applet names) are shifted beneath the circular boundaries in clean, high-contrast, center-aligned typography with subtle letter-spacing.

### 🔄 Hover & Dynamic Drag-and-Drop Reordering
- **Tactile Long-Press Gestures:** Initiate reordering on long-press. Activating this state triggers a gentle scale animation (scale up 1.05x) and subtle vibration feedback.
- **Direct Coordinate Collision Checking:** Allow the user to drag any circular card in real-time. As the dragged circle floats over neighboring item slots, the layout dynamically shifts them around using smooth transition keyframes to preview the final layout.
- **Persistent Positional Sorting:** Swapping coordinate indices triggers an asynchronous block saving the newly ordered route sequence directly into standard persistent storage.

### ⚙️ Multi-Select Active Applet Toggles
- **Interface Control Hub:** Add an adjustable manager list inside `SettingsScreen.kt` showing each available applet with dedicated switches.
- **Dynamically Redrawn Desktops:** Disabling a switch excludes the respective applet from the launcher grid and navigation drawers immediately.
- **Fallback Recovery:** The **Settings** applet is hardcoded to remain permanently active to prevent users from accidentally locking themselves out of the system config dashboard.

### 🚀 Custom Starting Destination Route
- **Default Application Startup:** Introduce an interactive Dropdown/Radio panel allowing users to set where the app directs cold launches (e.g., straight to Camera, Agenda, Browser, or the default Main Hub launcher).
- **Transactional Redirection Routing:** When the MainActivity boots, a side-effect executes immediately within the Compose lifecycle, loading this startup parameter. If set to a designated applet, the app seamlessly redirects from `"hub"` to that route, preserving back navigation triggers to return to the launcher easily.

---

## 2. Technical Architecture & Datastore Schemas

In adherence to modern architecture guidelines outlined in **Rule 13** and **Rule 14**, we write this implementation plan using the actual system configurations in Jetpack Preferences DataStore.

### A. DataStore Keys & Preferences Keys Configuration
We declare these three keys inside `/repo-android/app/src/main/java/com/example/cameraxapp/AppPreferences.kt` matching the standard preferences paradigm:

```kotlin
object AppPreferences {
    // ... Existing keys ...

    // Serialized JSON Array string tracking applet grid hierarchy (e.g., "[\"camera\", \"explorer\", \"ai_team\"]")
    val LAUNCHER_APPLET_ORDER = stringPreferencesKey("launcher_applet_order")

    // Serialized JSON Array string storing only active routes (e.g., "[\"camera\", \"explorer\", \"settings\"]")
    val LAUNCHER_ACTIVE_APPLETS = stringPreferencesKey("launcher_active_applets")

    // Target route to open on cold start. Defaults to "hub"
    val STARTUP_DEFAULT_APPLET = stringPreferencesKey("startup_default_applet")
}
```

### B. SettingsRepository Mapping
In `SettingsRepository`, we expose reactive `Flow` containers and write suspend setter functions to modify configs asynchronously:

```kotlin
class SettingsRepository(private val context: Context) {
    // ... Existing properties ...

    val launcherAppletOrder: Flow<String> = context.dataStore.data.map { 
        it[AppPreferences.LAUNCHER_APPLET_ORDER] ?: "" 
    }

    val launcherActiveApplets: Flow<String> = context.dataStore.data.map { 
        it[AppPreferences.LAUNCHER_ACTIVE_APPLETS] ?: "" 
    }

    val startupDefaultRoute: Flow<String> = context.dataStore.data.map { 
        it[AppPreferences.STARTUP_DEFAULT_APPLET] ?: "hub" 
    }

    suspend fun setLauncherAppletOrder(orderJson: String) {
        context.dataStore.edit { it[AppPreferences.LAUNCHER_APPLET_ORDER] = orderJson }
    }

    suspend fun setLauncherActiveApplets(activeJson: String) {
        context.dataStore.edit { it[AppPreferences.LAUNCHER_ACTIVE_APPLETS] = activeJson }
    }

    suspend fun setStartupDefaultRoute(route: String) {
        context.dataStore.edit { it[AppPreferences.STARTUP_DEFAULT_APPLET] = route }
    }
}
```

---

## 3. Step-by-Step Implementation Blueprint

### Phase 1: Core Datastore Integration & Infrastructure Extenders
- Define the keys and flow structures in `AppPreferences.kt`.
- Write helper methods inside `SettingsRepository` to handle standard serialization/deserialization logic. Since Kotlin standardizes on dynamic parsing, we can utilize native `JSONArray` methods:
  ```kotlin
  fun parseRoutes(json: String): List<String> {
      if (json.isEmpty()) return emptyList()
      val list = mutableListOf<String>()
      val array = org.json.JSONArray(json)
      for (i in 0 until array.length()) {
          list.add(array.getString(i))
      }
      return list
  }
  ```

### Phase 2: Circular Cards Draw & Desk Layout
Re-implement `AppletCard` inside `MainActivity.kt` to present a 48.dp circular icon structure, distinct soft pastel/neon backgrounds mapped uniquely per applet, and a high-density column grid enabling over 20 concurrent screen items:

```kotlin
// Helper function to resolve dynamic distinct background colors for each applet card route
@Composable
fun getAppletAccentColor(route: String): androidx.compose.ui.graphics.Color {
    return when (route) {
        "camera" -> androidx.compose.ui.graphics.Color(0xFFFF8A80) // Soft Coral/Rose
        "explorer" -> androidx.compose.ui.graphics.Color(0xFF82B1FF) // Sky Blue
        "ai_team" -> androidx.compose.ui.graphics.Color(0xFFEA80FC) // Violet/Purple
        "cronjobs" -> androidx.compose.ui.graphics.Color(0xFFFFD180) // Light Amber
        "db" -> androidx.compose.ui.graphics.Color(0xFF84FFFF) // Soft Cyan
        "agenda" -> androidx.compose.ui.graphics.Color(0xFFB9F6CA) // Mint Green
        "wallpaper" -> androidx.compose.ui.graphics.Color(0xFFFF80AB) // Bright pink
        "backup" -> androidx.compose.ui.graphics.Color(0xFFA7FFEB) // Soft Teal
        "settings" -> androidx.compose.ui.graphics.Color(0xFFCFD8DC) // Cool gray/charcoal
        "browser" -> androidx.compose.ui.graphics.Color(0xFFFFE082) // Soft yellow/gold
        else -> androidx.compose.ui.graphics.Color(0xFFE0E0E0) // Fallback default grey
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircularAppletCard(
    applet: AppletInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBackground = getAppletAccentColor(applet.route)
    
    Column(
        modifier = modifier
            .width(64.dp) // Tight width constraint for dense spacing
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High-density 48.dp Circular Container for Applet Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            cardBackground,
                            cardBackground.copy(alpha = 0.7f)
                        )
                    )
                )
                .border(1.5.dp, cardBackground.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = applet.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = androidx.compose.ui.graphics.Color.Black // High-contrast icon on soft pastel shapes
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // High-density label mapping
        Text(
            text = applet.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = androidx.compose.ui.unit.sp(10),
                letterSpacing = androidx.compose.ui.unit.sp(0.5)
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
```

```kotlin
// Hub Screen Grid Setup inside MainActivity.kt
// This layout scales parameters into standard 5 columns to host 20+ icons in portrait state area without scroll offsets
@Composable
fun HighDensityHubGrid(
    applets: List<AppletInfo>,
    onAppletClicked: (AppletInfo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5), // 5 columns ensures up to 20 applets fit compactly
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(applets) { applet ->
            CircularAppletCard(applet = applet, onClick = { onAppletClicked(applet) })
        }
    }
}
```

### Phase 3: Gestural Drag-and-Drop Reordering Engine
To avoid heavy external library dependencies and ensure maximum compilation speed and compatibility, we build a lightweight, self-contained reordering canvas wrapper in Compose. 

We write a utility `reorderable` helper tracking item displacement and item swaps inside our custom list:

```kotlin
@Composable
fun ReorderableGridContainer(
    applets: List<AppletInfo>,
    onOrderChanged: (List<AppletInfo>) -> Unit,
    content: @Composable (List<AppletInfo>, draggingIndex: Int?, offset: androidx.compose.ui.geometry.Offset?) -> Unit
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val listState = remember { mutableStateListOf<AppletInfo>().apply { addAll(applets) } }
    
    // Sync external order changes
    LaunchedEffect(applets) {
        if (listState.toList() != applets) {
            listState.clear()
            listState.addAll(applets)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        content(listState.toList(), draggingIndex, dragOffset)
    }
}
```

When dragging initiates, coordinate systems calculate the bounding box swaps. On release, `onOrderChanged` is fired, which serializes the new route order list into JSON and passes it over to context-aware repo routines:

```kotlin
coroutineScope.launch {
    val routeOrder = newList.map { it.route }
    val json = org.json.JSONArray(routeOrder).toString()
    repository.setLauncherAppletOrder(json)
}
```

### Phase 4: Dynamic Desktop Filters (Active Applets Routing)
The Hub Screen and Nav Drawers must filter the master applet list dynamically:

```kotlin
val activeAppletJson by repository.launcherActiveApplets.collectAsState(initial = "")
val activeApplets = remember(activeAppletJson, allAppletsList) {
    if (activeAppletJson.isEmpty()) {
        allAppletsList // Fallback to displaying all applets by default
    } else {
        val activeRoutes = parseRoutes(activeAppletJson)
        // Ensure "settings" and "hub" are always reachable as fallbacks
        allAppletsList.filter { it.route == "settings" || it.route == "hub" || activeRoutes.contains(it.route) }
    }
}
```

### Phase 5: Cold Start Redirect Setup
In `MainActivity.kt` constructor triggers, we resolve the first starting route parameter. If the setting differs from `"hub"`, navigation directs are fired immediately post initialization:

```kotlin
// Check inside MainActivity -> setContent Block
val defaultRoute by repository.startupDefaultRoute.collectAsState(initial = "hub")
var hasRedirected by remember { mutableStateOf(false) }

LaunchedEffect(defaultRoute) {
    if (!hasRedirected && defaultRoute != "hub") {
        hasRedirected = true
        navController.navigate(defaultRoute) {
            popUpTo("hub") {
                saveState = true
            }
            launchSingleTop = true
        }
    }
}
```

---

## 4. Upgrading `SettingsScreen.kt` UI Panels

To configure these custom properties, we append three interactive configurator controls at the bottom of the Settings list screen:

### A. Default Starting Destination Control
Displays the currently selected startup applet. Tapping this reveals a dropdown overlay listing active destination names, saving selections to the Datastore dynamically.

### B. Applet Visibility Switch Desk
An action item opening a dedicated full-screen drawer or Dialog overlay showing a togglable switch for each route:
- 🟢 **Camera** Toggle Switch
- 🟢 **Explorer** Toggle Switch
- 🟢 **AI Team** Toggle Switch
- 🟢 **Settings** (Permanently Checked & Disabled to avoid local system lock-out)

### C. Reset Desktop Layout Button
Add an exclusive, styled action button at the bottom of the Settings panels to restore default settings:
- Clears the saved `LAUNCHER_APPLET_ORDER` sequence, instantly reverting the landing dashboard order to the default out-of-the-box launcher state.
- Resets the `LAUNCHER_ACTIVE_APPLETS` configuration back to empty (all applets active by default).
- Resets cold start routes (`STARTUP_DEFAULT_APPLET`) to `"hub"`.
- Displays a custom, non-blocking toast/popup notification to confirm default configurations have been restored.

---

## 5. Proposing Next-Generation Features

Beyond the core upgrades requested, we propose the following user experience upgrades to elevate the system launcher's capabilities:

1. 🌌 **Dynamic Desktop Folders (Bento-Grouping Mode):**
   - Allows users to drag one circular card on top of another to automatically create an expandable Folder container.
   - Folders are assigned customizable names, with groups saved inside Datastore as categorized JSON tree blocks.

2. 🎭 **Quick-Action Micro Gestures (Applet Shortcuts):**
   - Double-tapping any Circular Card shows a contextual pop-up menu container triggering instantaneous actions (e.g., jump-scan directly in Camera, clear browser cache, or backup database now) without booting the full applet canvas.

3. 🕯️ **Interactive Live Widgets (Mini Status Circular Badges):**
   - Enhances circular cards with live notification counts or status pulses (e.g., active background cron alerts, next programmed alarm timeline count, or active folder size indicator).

---

## 6. Feedback & Architectural Alignments

During interactive alignment iterations, the following target parameters were determined and finalized:

1. **Drag Animation Speed (Animated Swap-on-Hover):**
   - *Final Decision:* **Yes.** The system uses rich, dynamic swap animations where cards floatingly slide out of the way as another item is dragged over their slots. Dragging is highly aesthetic and kinetic rather than triggering instant static jumps.

2. **Circular Size Constraints (Structured Desktop):**
   - *Final Decision:* **No.** The system rejects loose floating/freeform positioning. Icons are locked to a high-density, structured, responsive 5-column vertical grid to keep layout scaling clean, organized, and perfectly legible across varying portrait aspect ratios.

3. **Reset Shortcuts (Settings Only):**
   - *Final Decision:* **Ok for settings only.** A direct "Reset Desktop Layout" function is added exclusively as an option inside the settings manager dashboard, avoiding launcher menu clutter while maintaining easy desktop restore capabilities.
