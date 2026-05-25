# Implementation Plan: Agenda Maps Integration & Custom Colorpicker

**Objective**: Extend the local offline-first Android Agenda applet to support immersive geographic planning and styling. This addon introduces a fully functional Google Maps Jetpack Compose component, bidirectional event scheduling (via calendar views or spatial map drop-pins), location-to-address geocoding lookups, and a responsive, validated color tag picker.

---

## 1. Architectural Overview & State Flow

The expanded system links the existing SQLite database context with the Android Location Framework and Google Play Services Maps SDK.

```
                  ┌─────────────────────────────┐
                  │    AgendaViewModel (State)  │
                  └──────────────┬──────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         ▼                                               ▼
┌──────────────────┐                            ┌──────────────────┐
│  Calendar Screen │                            │ Map Module Screen│
└────────┬─────────┘                            └────────┬─────────┘
         │                                               │
         │ (Launches)                                    │ (Long-press Map drops pin)
         ▼                                               ▼
┌──────────────────────────────────────────────────────────────────┐
│               Unified Event Creator Editor Sheet                 │
│  - Form Details (Title, Description, Duration)                   │
│  - Custom Interactive Colorpicker Panel (Preset Chips & Hex Code) │
│  - Geocoded Physical Location Panel (Lat/Lng & human Address)    │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. Dynamic Database Schema Migrations (v2)

To accommodate location attributes without losing existing calendar events, we will upgrade the underlying SQLiteHelper database.

### Table Schema Alteration
```sql
ALTER TABLE agenda_events ADD COLUMN latitude REAL DEFAULT NULL;
ALTER TABLE agenda_events ADD COLUMN longitude REAL DEFAULT NULL;
ALTER TABLE agenda_events ADD COLUMN location_name TEXT DEFAULT NULL;
```

### Migration Code Block (`AgendaDatabaseHelper.kt`)
Within our SQLite Open Helper, modify `DATABASE_VERSION` to `2` and implement standard safe schema upgrade logic:

```kotlin
override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (oldVersion < 2) {
        db.execSQL("ALTER TABLE agenda_events ADD COLUMN latitude REAL DEFAULT NULL;")
        db.execSQL("ALTER TABLE agenda_events ADD COLUMN longitude REAL DEFAULT NULL;")
        db.execSQL("ALTER TABLE agenda_events ADD COLUMN location_name TEXT DEFAULT NULL;")
    }
}
```

---

## 3. High-Fidelity Maps Module Design

The Map view utilizes `com.google.maps.android:maps-compose` to construct beautiful, hardware-accelerated interactive maps.

### A. UI/UX Elements
- **Adaptive Map Layers Controller**: A neat floating overlay button that toggles between `MapType.NORMAL`, `MapType.SATELLITE`, and `MapType.HYBRID`.
- **Zoom HUD & Compass**: Visible physical controls matching standard Google Maps UI guidelines, incorporating gesture locks for precision panning.
- **Interactive Sliding Panel (Bottom Sheet)**:
  - Collapsed state: Summarizes events scheduled on the selected day that are associated with coordinates.
  - Expanded state: List of all geographic coordinates registered in the database, allowing users to scroll and tap an event card to auto-center the map viewport with a stylish camera tilt action.

### B. Compose Implementation Pattern
```kotlin
@Composable
fun AgendaMapView(
    events: List<AgendaEvent>,
    onSelectLocation: (LatLng) -> Unit,
    onAddEventAtCoordinates: (LatLng, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(48.8566, 2.3522), 12f) // Default Paris anchor
    }
    
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var selectedGeopoint by remember { mutableStateOf<LatLng?>(null) }
    var resolvedAddress by remember { mutableStateOf<String?>(null) }
    
    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = mapType),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false, // We render custom minimal high-contrast buttons
                compassEnabled = true
            ),
            onMapLongClick = { latLng ->
                selectedGeopoint = latLng
                // Reverse-geocode in a background thread
                resolvedAddress = getAddressFromCoordinates(context, latLng)
            }
        ) {
            // Render active events as Advanced Markers
            events.filter { it.latitude != null && it.longitude != null }.forEach { event ->
                val pos = LatLng(event.latitude!!, event.longitude!!)
                Marker(
                    state = rememberMarkerState(position = pos),
                    title = event.title,
                    snippet = event.locationName ?: "Saved Location",
                    icon = rememberCustomMarkerIcon(event.colorTag)
                )
            }
            
            // Render draft/new event pin selection candidate
            selectedGeopoint?.let { gp ->
                Marker(
                    state = rememberMarkerState(position = gp),
                    title = "New Event Coordinates",
                    snippet = resolvedAddress ?: "Loading location address...",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
            }
        }
        
        // Custom Zoom HUD Overlays
        ZoomControlsColumn(
            onZoomIn = { cameraPositionState.move(CameraUpdateFactory.zoomIn()) },
            onZoomOut = { cameraPositionState.move(CameraUpdateFactory.zoomOut()) },
            onToggleMapType = { mapType = if (mapType == MapType.NORMAL) MapType.SATELLITE else MapType.NORMAL },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        )
        
        // Sliding Location Banner overlay
        selectedGeopoint?.let { gp ->
            LocationDraftPanel(
                address = resolvedAddress ?: "Fetching address details...",
                coordinates = gp,
                onComposeEvent = { onAddEventAtCoordinates(gp, resolvedAddress ?: "") },
                onDismiss = { selectedGeopoint = null },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }
    }
}
```

---

## 4. Dual Entry-Point Scheduling Paradigm

To guarantee a flexible UX workflow, events can be initialized via two separate pathways:

### Option A: The Calendar Entry Route
- **Flow**: User taps a day on the calendar grid → Opens standard Event Form → Clicks on **"Pin a Spot on Map"** button.
- **Action**: Launches a full-screen Modal location selector map. Tapping or dragging the pin selects coordinates. Clicking "Save Coordinates" reverse-geocodes the details and automatically updates the Event Creator form fields with Lat/Lng and Location Name, returning securely to the form layout.

### Option B: The Map Entry Route
- **Flow**: User navigates to the Maps Tab → Explores the geographical map → Long-presses on any coordinates.
- **Action**: Dropping a draft orange pin brings up an immersive, floating action overlay card: **"Schedule event at [Address]?"**. Clicking this card slides up the pre-populated Event Creator panel. The Date, Latitude, Longitude, and Location strings are pre-configured into the ViewModel state, leaving only details (Title, Notes, customized Color) to be written.

---

## 5. Modern Custom Jetpack Compose Colorpicker

To upgrade from standard text descriptors ("Primary", "Secondary") to vibrant spatial themes, a state-of-the-art interactive color picker panel is embedded directly inside the creation sheets.

```
┌──────────────────────────────────────────────┐
│  COLOR CONFIGURATION                         │
│  (●)  ( )  ( )  ( )  ( )  ( )  ( )  ( )     │
│  Teal Rose Lava Mint Plum Sand Gold Slate    │
│  Hex Code: [ #008080             ] ✔ Valid   │
└──────────────────────────────────────────────┘
```

### A. UI Components
- **Color Chips Palette**: Row containing 8 circle chips wrapped in soft, high-contrast borders. The currently selected color glows with a centered minimalist checkmark icon overlay.
- **Preset Color Map**:
  - Teal: `#008080`
  - Rose: `#E91E63`
  - Lava: `#D32F2F`
  - Mint: `#4CAF50`
  - Plum: `#7B1FA2`
  - Sand: `#F4D03F`
  - Gold: `#F39C12`
  - Slate: `#475569`
- **Dynamic Hex Input Field**: 
  - Allows power-users to paste standard HEX values.
  - Implements real-time regex validation (`^#[0-9A-Fa-f]{6}$`).
  - Active visual feedback: Changes status to a green checkmark check if the code is valid, and scales a preview box filled with the custom color.

### B. Compose Composable Skeleton
```kotlin
@Composable
fun AgendaColorPicker(
    selectedColorHex: String,
    onColorChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = remember {
        listOf(
            "Teal" to "#008080", "Rose" to "#E91E63", "Lava" to "#D32F2F", "Mint" to "#4CAF50",
            "Plum" to "#7B1FA2", "Sand" to "#F4D03F", "Gold" to "#F39C12", "Slate" to "#475569"
        )
    }
    
    var hexInputText by remember { mutableStateOf(selectedColorHex) }
    val isHexValid = remember(hexInputText) {
        val regex = "^#[0-9A-Fa-f]{6}$".toRegex()
        regex.matches(hexInputText)
    }
    
    Column(modifier = modifier.fillMaxWidth().padding(12.dp)) {
        Text(
            text = "Select Event Tag Color",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Preset Chips Row
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presets.forEach { (_, hex) ->
                val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                        .clickable {
                            onColorChanged(hex)
                            hexInputText = hex
                        }
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Hex Code Text input
        OutlinedTextField(
            value = hexInputText,
            onValueChange = { input ->
                if (input.length <= 7) {
                    hexInputText = input
                    if (input.matches("^#[0-9A-Fa-f]{6}$".toRegex())) {
                        onColorChanged(input)
                    }
                }
            },
            label = { Text("Custom Color HEX (e.g., #008080)") },
            isError = !isHexValid && hexInputText.isNotEmpty(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (isHexValid) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Valid Color",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        )
    }
}
```

---

## 6. Persisting Map Preferences in Shared Settings

To guarantee full customizability, we will integrate map defaults and configuration states into the existing `AppPreferences` Jetpack DataStore engine.

### A. New Keys configuration (`AppPreferences.kt`)
Add key definitions to store custom target locations, initial user zoom levels, and the last used map layer preference:

```kotlin
object AppPreferences {
    // ... Existing keys ...
    
    val MAP_DEFAULT_LATITUDE = stringPreferencesKey("map_default_latitude")    // Default fallback: "48.8566"
    val MAP_DEFAULT_LONGITUDE = stringPreferencesKey("map_default_longitude")  // Default fallback: "2.3522"
    val MAP_DEFAULT_ZOOM = floatPreferencesKey("map_default_zoom")             // Default zoom level: 12.0f
    val MAP_LAST_LAYER_TYPE = intPreferencesKey("map_last_layer_type")         // 1: Normal, 2: Satellite, 3: Hybrid
}
```

### B. Adding Storage Access in SettingsRepository
```kotlin
class SettingsRepository(private val context: Context) {
    // ... Existing properties ...

    val mapDefaultLatitude: Flow<Double> = context.dataStore.data.map { 
        (it[AppPreferences.MAP_DEFAULT_LATITUDE] ?: "48.8566").toDoubleOrNull() ?: 48.8566 
    }
    val mapDefaultLongitude: Flow<Double> = context.dataStore.data.map { 
        (it[AppPreferences.MAP_DEFAULT_LONGITUDE] ?: "2.3522").toDoubleOrNull() ?: 2.3522 
    }
    val mapDefaultZoom: Flow<Float> = context.dataStore.data.map { 
        it[AppPreferences.MAP_DEFAULT_ZOOM] ?: 12.0f 
    }
    val mapLastLayerType: Flow<Int> = context.dataStore.data.map { 
        it[AppPreferences.MAP_LAST_LAYER_TYPE] ?: 1 // Default to MapType.NORMAL
    }

    suspend fun setMapDefaultCoordinates(lat: Double, lng: Double) {
        context.dataStore.edit { prefs ->
            prefs[AppPreferences.MAP_DEFAULT_LATITUDE] = lat.toString()
            prefs[AppPreferences.MAP_DEFAULT_LONGITUDE] = lng.toString()
        }
    }

    suspend fun setMapDefaultZoom(zoom: Float) {
        context.dataStore.edit { prefs ->
            prefs[AppPreferences.MAP_DEFAULT_ZOOM] = zoom
        }
    }

    suspend fun setMapLastLayerType(type: Int) {
        context.dataStore.edit { prefs ->
            prefs[AppPreferences.MAP_LAST_LAYER_TYPE] = type
        }
    }
}
```

---

## 7. Dependencies Additions (`repo-android/app/build.gradle`)

To build and compile these structures, we will append the standard Google Maps and Location Play Services modules:

```groovy
// Google Play Services Maps SDK
implementation 'com.google.android.gms:play-services-maps:18.2.0'

// Google Maps Compose Platform Widgets
implementation 'com.google.maps.android:maps-compose:4.3.3'

// Play Services Location (for GPS / Geo tracking coordinate queries)
implementation 'com.google.android.gms:play-services-location:21.2.0'
```

We will also update standard Permissions in `AndroidManifest.xml` to request location access:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 8. APK App Size Impact Analysis & Optimization Techniques

When deploying Location-based and Google Maps capabilities inside mobile applications, keeping binary sizes optimized is a core developmental priority. 

### A. Real Footprint Assessment
1. **The Dynamic Play Services Paradigm**:
   - Google Maps for Android utilizes **Client-Side Thin Hooks**. The heavy map rendering engine, global map tile rendering resources, location caching system, and imagery assets actually reside within the standard **Google Play Services on-device system application**.
   - Because the bulk of the runtime implementation is shared on-device, importing `play-services-maps` and `play-services-location` libraries adds **less than ~1.5 MB** to the compressed user-installed APK footprint.
2. **Maps-Compose Footprint**:
   - Tacking on the standard Jetpack Compose maps component wrapping classes adds a minor footprint of **~250 KB**.

### B. Size Optimization Best-Practices
The total transitive code layer of Play Services SDK contains several translation sets, geocoding helpers, and debugging telemetry interfaces. To compress these elements, you should enforce strict **Pruning Rules** in your Gradle environment:

1. **Gradle Resource and Code Shrinking**:
   Ensure `minifyEnabled` and `shrinkResources` are true in `build.gradle` for release pipelines. This allows the compiler to strip out any unused classes from the dependency tree.
   ```groovy
   android {
       buildTypes {
           release {
               minifyEnabled true
               shrinkResources true
               proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
           }
       }
   }
   ```

2. **Refining Resource Packaging Configurations**:
   By default, the Play Services library contains translations for dozens of global locales. If you are targeting a subset of languages, you can instruct Gradle to build with only the specific locale values you need. In `app/build.gradle`:
   ```groovy
   android {
       defaultConfig {
           // Restrict strings packaging to English, French, Spanish, etc.
           resConfigs "en", "fr", "es"
       }
   }
   ```
   This simple configuration strips out foreign localization resource structures, saving **up to ~1.2 MB** of compressed string resources.

3. **Standard ProGuard Code Guardrails (`proguard-rules.pro`)**:
   Add compiler rules to prevent the obfuscator from stripping away necessary on-demand binder classes or custom serializable schemas utilized within Play Services:
   ```pro
   # Retain all necessary Play Services components
   -keep class com.google.android.gms.maps.** { *; }
   -keep interface com.google.android.gms.maps.** { *; }
   -keep class com.google.android.gms.common.api.** { *; }
   
   # Prevent stripping reflective geocoder structures Used during address lookups
   -keep class android.location.Geocoder { *; }
   ```

---

## 9. Implementation Roadmap & Milestones

1. **Milestone 1: Schema Updates & Dependencies Installation**
   - Import Play Services Maps and Maps Compose in `build.gradle`.
   - Update `AgendaDatabaseHelper.kt` database version, add coordinates/address columns, and implement non-blocking schema migration loops.
2. **Milestone 2: Coordinate Reverse Geocoder & Colorpicker Panel**
   - Embed the `AgendaColorPicker` composable inside the event builder dialog, and link standard theme chips to the state flows.
   - Implement `getAddressFromCoordinates` utilizing modern Android `Geocoder` utilities to resolve textual addresses asynchronously in a high-efficiency background thread.
3. **Milestone 3: Immersive Compose Map View & Settings Configurations**
   - Create the maps canvas inside `AgendaScreen.kt`, mapping events data into visual map markers.
   - Bind default camera positions, layout layers, and coordinate presets to the `SettingsRepository` reactive flows.
4. **Milestone 4: Dual entry scheduling verification**
   - Unify the VM state routines to pre-fill coordinates on long-press map interactions, and handle opening coordinate selectors directly from the calendar forms.

---

## 10. Dual-Engine Map Architecture (Leaflet vs. Google Maps JS in WebView)

To solve rendering blank map views across varying device support contexts, the system implements a modern, togglable Dual Map-Engine architecture inside `AgendaScreen.kt`.

### A. Architectural Philosophy
Instead of native maps-compose crashing in environments without Google Play services, maps are loaded inside a fully configured, secure `WebView`. The system provides two dynamic map templates:
1. **Engine 0: OpenStreetMap (OSM) via Leaflet JS**: A fully standalone web-GIS setup using public Leaflet maps requiring no API key.
2. **Engine 1: Google Maps JavaScript API**: An interactive web-GIS canvas built directly on Google Maps JS capabilities, initialized via a user-defined API key.

```
                    ┌───────────────────────────────┐
                    │      UserSettings / Settings  │
                    └───────────────┬───────────────┘
                                    │ (mapEngineType Flow: 0 / 1)
                                    ▼
                    ┌───────────────────────────────┐
                    │       LeafletMapViewPane      │
                    └───────────────┬───────────────┘
                                    │
            ┌───────────────────────┴───────────────────────┐
            ▼ (Engine 0)                                    ▼ (Engine 1)
┌─────────────────────────────────┐             ┌─────────────────────────────────┐
│  OpenStreetMap + Leaflet JS     │             │    Google Maps JS API Engine    │
│  - Public OSM Tile Layers       │             │    - Custom Colored Pins Symbol │
│  - Lightweight, Keyless         │             │    - Google Maps Tiles Sets     │
│  - Leaflet Map Click Handlers   │             │    - Places JS integration stub │
└─────────────────────────────────┘             └─────────────────────────────────┘
```

### B. Secure Configurations Management
1. **Map Engine Selection**: Handled inside `SettingsScreen` and persisted dynamically inside Preference DataStore as `MAP_ENGINE_TYPE`.
2. **Google Maps API Key**: Users can save a customized GMP API Key inside Preference DataStore under `GOOGLE_MAPS_API_KEY`, secured with input masks in the Android settings view.
3. **Javascript Bridge Communication**: Both map rendering pipelines stream callbacks to Android native components via `AndroidBridge` interface (e.g. `addEventAt(lat, lng)`, `editEvent(id)`). This guarantees identical UX flows regardless of the chosen engine underneath.
