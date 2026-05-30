package com.example.cameraxapp.draw

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawScreen(
    viewModel: DrawViewModel,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenRightDrawer: () -> Unit
) {
    val context = LocalContext.current
    var showLayersPanel by remember { mutableStateOf(true) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showUrlImportDialog by remember { mutableStateOf(false) }

    // Floating temporary coordinates to represent strokes actively drawn by pointer
    val currentPoints = remember { mutableStateListOf<Offset>() }

    // Sourcing launchers
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    viewModel.addLocalDeviceImage(bitmap)
                } else {
                    Toast.makeText(context, "Unsupported file format", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Capture Toast alerts pushed by ViewModel
    LaunchedEffect(viewModel.actionFeedbackMessage.value) {
        viewModel.actionFeedbackMessage.value?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.actionFeedbackMessage.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🖌️ Studio Draw", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Navigation Drawer")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Undo")
                    }
                    IconButton(onClick = { showLayersPanel = !showLayersPanel }) {
                        Icon(
                            imageVector = if (showLayersPanel) Icons.Default.Check else Icons.Default.List,
                            contentDescription = "Toggle Layers Stack Manager",
                            tint = if (showLayersPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Compile & Export masterpiece compilation")
                    }
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Left main segment: Canvas workspace with drawing brushes properties
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Workspace canvas frame
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF2B2B2B), shape = RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFF424242), shape = RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Compose Interactive Canvas
                    Canvas(
                        modifier = Modifier
                            .aspectRatio(viewModel.canvasWidth.value.toFloat() / viewModel.canvasHeight.value.toFloat())
                            .fillMaxSize()
                            .background(Color.White)
                            .pointerInput(viewModel.activeLayerId.value) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val active = viewModel.getActiveLayer()
                                        if (active != null && !active.isLocked && active.isVisible && active.type == LayerType.VECTOR_INK) {
                                            viewModel.saveToUndoStack()
                                            currentPoints.clear()
                                            currentPoints.add(offset)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val active = viewModel.getActiveLayer()
                                        if (active != null && !active.isLocked && active.isVisible && active.type == LayerType.VECTOR_INK) {
                                            currentPoints.add(change.position)
                                        }
                                    },
                                    onDragEnd = {
                                        val active = viewModel.getActiveLayer()
                                        if (active != null && !active.isLocked && active.isVisible && active.type == LayerType.VECTOR_INK && currentPoints.isNotEmpty()) {
                                            val stroke = DrawnStroke(
                                                points = currentPoints.toList(),
                                                color = if (viewModel.activeTool.value == "Eraser") android.graphics.Color.WHITE else viewModel.currentBrushColor.value,
                                                strokeWidth = viewModel.currentBrushSize.value,
                                                isEraser = viewModel.activeTool.value == "Eraser"
                                            )
                                            active.strokes.add(stroke)
                                            currentPoints.clear()
                                        }
                                    }
                                )
                            }
                    ) {
                        // Render persistent layer stacks sequentially
                        for (layer in viewModel.layers) {
                            if (!layer.isVisible) continue

                            if (layer.type == LayerType.RASTER_IMAGE) {
                                layer.bitmap?.let { bmp ->
                                    drawImage(
                                        image = bmp.asImageBitmap(),
                                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                        alpha = layer.opacity,
                                        blendMode = layer.blendMode
                                    )
                                }
                            } else {
                                // Redraw ink strokes inside active vector bounds
                                for (stroke in layer.strokes) {
                                    if (stroke.points.size > 1) {
                                        val path = androidx.compose.ui.graphics.Path()
                                        path.moveTo(stroke.points[0].x, stroke.points[0].y)
                                        for (i in 1 until stroke.points.size) {
                                            val p = stroke.points[i]
                                            path.lineTo(p.x, p.y)
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color(stroke.color),
                                            alpha = layer.opacity,
                                            style = Stroke(
                                                width = stroke.strokeWidth,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            ),
                                            blendMode = layer.blendMode
                                        )
                                    }
                                }
                            }
                        }

                        // Render dynamic active drag preview stroke
                        if (currentPoints.size > 1) {
                            val activePath = androidx.compose.ui.graphics.Path()
                            activePath.moveTo(currentPoints[0].x, currentPoints[0].y)
                            for (i in 1 until currentPoints.size) {
                                activePath.lineTo(currentPoints[i].x, currentPoints[i].y)
                            }
                            drawPath(
                                path = activePath,
                                color = if (viewModel.activeTool.value == "Eraser") Color.White else Color(viewModel.currentBrushColor.value),
                                style = Stroke(
                                    width = viewModel.currentBrushSize.value,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Brush settings, size configurations and background sourcing triggers
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        // Brush stroke configuration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tool Mode:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row {
                                FilterChip(
                                    selected = viewModel.activeTool.value == "Pen",
                                    onClick = { viewModel.activeTool.value = "Pen" },
                                    label = { Text("✍️ Brush", fontSize = 11.sp) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(
                                    selected = viewModel.activeTool.value == "Eraser",
                                    onClick = { viewModel.activeTool.value = "Eraser" },
                                    label = { Text("🧼 Eraser", fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Thickness slide slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Size: ${viewModel.currentBrushSize.value.toInt()}px", fontSize = 12.sp, modifier = Modifier.width(72.dp))
                            Slider(
                                value = viewModel.currentBrushSize.value,
                                onValueChange = { viewModel.currentBrushSize.value = it },
                                valueRange = 1f..100f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Colors Palette
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Colors:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            val colorsList = listOf(
                                android.graphics.Color.BLACK,
                                android.graphics.Color.DKGRAY,
                                android.graphics.Color.RED,
                                android.graphics.Color.BLUE,
                                android.graphics.Color.GREEN,
                                android.graphics.Color.YELLOW,
                                android.graphics.Color.parseColor("#FF6F00"), // Orange
                                android.graphics.Color.parseColor("#9C27B0"), // Purple
                                android.graphics.Color.parseColor("#00E5FF")  // Cyan
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                colorsList.forEach { col ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(col))
                                            .border(
                                                width = if (viewModel.currentBrushColor.value == col) 2.dp else 1.dp,
                                                color = if (viewModel.currentBrushColor.value == col) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.currentBrushColor.value = col }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Background layer addition triggers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { filePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Device Image", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { showUrlImportDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download Web Image", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Right main segment: GIMP-style layers stacked management panel
            if (showLayersPanel) {
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Layers Stack",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 6.dp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Layers Stack management utilities
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { viewModel.addBlankInkLayer() }) {
                                Icon(Icons.Default.Add, contentDescription = "Add blank vector ink layer")
                            }
                            IconButton(onClick = { viewModel.duplicateActiveLayer() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Duplicate current layer")
                            }
                            IconButton(onClick = { viewModel.mergeActiveLayerDown() }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Merge current active layer down")
                            }
                            IconButton(onClick = { viewModel.clearActiveLayer() }) {
                                Icon(Icons.Default.Close, contentDescription = "Reset active layer content")
                            }
                            IconButton(onClick = { viewModel.deleteActiveLayer() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete layer from composite", tint = Color.Red)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        // Scrollable list of active layers representing ordered stack indexes
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Render from top down (highest stack indices drawn last, so list reversed displays actual view layers layout)
                            val reversedLayersList = viewModel.layers.asReversed()
                            items(reversedLayersList, key = { it.id }) { layer ->
                                val isActive = viewModel.activeLayerId.value == layer.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectLayer(layer.id) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Title indicator
                                            Text(
                                                text = if (layer.type == LayerType.RASTER_IMAGE) "🖼️ ${layer.name}" else "✍️ ${layer.name}",
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )

                                            // Visibility view and locked configuration toggles uses emojis for clean contrast
                                            Text(
                                                text = if (layer.isVisible) "👁️" else "🚫",
                                                modifier = Modifier
                                                    .clickable { viewModel.updateLayerVisibility(layer.id, !layer.isVisible) }
                                                    .padding(horizontal = 4.dp),
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (layer.isLocked) "🔒" else "🔓",
                                                modifier = Modifier
                                                    .clickable { viewModel.updateLayerLocked(layer.id, !layer.isLocked) }
                                                    .padding(horizontal = 4.dp),
                                                fontSize = 14.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Layer controls opacity adjustments slider
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Alpha: ${(layer.opacity * 100).toInt()}%", fontSize = 10.sp, modifier = Modifier.width(60.dp))
                                            Slider(
                                                value = layer.opacity,
                                                onValueChange = { viewModel.updateLayerOpacity(layer.id, it) },
                                                valueRange = 0f..1f,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        // Blend mode drop selector triggers
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Blending:", fontSize = 10.sp)
                                            var showBlendPopup by remember { mutableStateOf(false) }
                                            Box {
                                                Text(
                                                    text = layer.blendMode.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .clickable { showBlendPopup = true }
                                                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                                DropdownMenu(
                                                    expanded = showBlendPopup,
                                                    onDismissRequest = { showBlendPopup = false }
                                                ) {
                                                    val blendModes = listOf(
                                                        androidx.compose.ui.graphics.BlendMode.SrcOver,
                                                        androidx.compose.ui.graphics.BlendMode.Multiply,
                                                        androidx.compose.ui.graphics.BlendMode.Screen,
                                                        androidx.compose.ui.graphics.BlendMode.Plus,
                                                        androidx.compose.ui.graphics.BlendMode.SrcAtop,
                                                        androidx.compose.ui.graphics.BlendMode.DstOver
                                                    )
                                                    blendModes.forEach { mode ->
                                                        DropdownMenuItem(
                                                            text = { Text(mode.name, fontSize = 12.sp) },
                                                            onClick = {
                                                                viewModel.updateLayerBlendMode(layer.id, mode)
                                                                showBlendPopup = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Stack ordering adjusters
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.moveLayerUp(layer.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Layer Up", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.moveLayerDown(layer.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Layer Down", modifier = Modifier.size(16.dp))
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
    }

    // URL image downloader Dialog Card
    if (showUrlImportDialog) {
        var importUrlStr by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showUrlImportDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Download Background Online", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importUrlStr,
                        onValueChange = { importUrlStr = it },
                        label = { Text("Image HTTP Stream URL") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://example.com/art.jpg") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (viewModel.isDownloading.value) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showUrlImportDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(onClick = {
                                if (importUrlStr.isNotBlank()) {
                                    viewModel.downloadUrlImage(context, importUrlStr)
                                    showUrlImportDialog = false
                                }
                            }) {
                                Text("Download")
                            }
                        }
                    }
                }
            }
        }
    }

    // Professional Master Compile Dialog Card
    if (showExportDialog) {
        var selectedFormat by remember { mutableStateOf("PNG") }
        var selectedScale by remember { mutableStateOf(1.0f) }
        var qualityVal by remember { mutableStateOf(95f) }

        Dialog(onDismissRequest = { showExportDialog = false }) {
            Card(
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Render masterpiece compiler", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Format Preset:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("PNG", "JPEG", "WEBP").forEach { fmt ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFormat = fmt },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedFormat == fmt, onClick = { selectedFormat = fmt })
                                Text(fmt, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Dimension Scale Factor:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(1.0f to "1x Standard", 2.0f to "2x HD", 4.0f to "4x Studio").forEach { (sc, scName) ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedScale = sc },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedScale == sc, onClick = { selectedScale = sc })
                                Text(scName, fontSize = 11.sp)
                            }
                        }
                    }

                    if (selectedFormat != "PNG") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quality: ${qualityVal.toInt()}%", fontSize = 11.sp, modifier = Modifier.width(72.dp))
                            Slider(
                                value = qualityVal,
                                onValueChange = { qualityVal = it },
                                valueRange = 10f..100f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showExportDialog = false }) {
                            Text("Dismiss")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = {
                            viewModel.saveCompositeArtwork(
                                context = context,
                                format = selectedFormat,
                                quality = qualityVal.toInt(),
                                dimensionScale = selectedScale
                            )
                            showExportDialog = false
                        }) {
                            Text("Compile & Save")
                        }
                    }
                }
            }
        }
    }
}
