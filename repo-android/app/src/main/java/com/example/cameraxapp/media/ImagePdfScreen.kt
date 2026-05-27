package com.example.cameraxapp.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class UriMediaItem(
    val uri: Uri,
    val name: String,
    val byteSize: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePdfScreen(
    baseFolder: File,
    onBack: () -> Unit,
    triggerNotification: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Standalone state inputs
    val selectedMediaItems = remember { mutableStateListOf<UriMediaItem>() }
    var quality by remember { mutableStateOf(75f) }
    var scale by remember { mutableStateOf(80f) }
    var selectedFormat by remember { mutableStateOf(ImageReducerEngine.OutputFormat.JPEG) }

    var pdfPageSize by remember { mutableStateOf(PdfCompilationEngine.PageSize.A4) }
    var pdfOrientation by remember { mutableStateOf(PdfCompilationEngine.PageOrientation.PORTRAIT) }
    var pdfMargin by remember { mutableStateOf(36) } // Default 36px/points (~0.5 inch)

    // Operation status
    var isProcessing by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<String>() }

    fun addLog(msg: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add("[$stamp] $msg")
    }

    // PhotoPicker Launcher setup
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { uri ->
                    val resolved = resolveUriDetails(context, uri)
                    selectedMediaItems.add(resolved)
                    addLog("Imported visual asset: ${resolved.name} (${formatByteSize(resolved.byteSize)})")
                }
                triggerNotification("Successfully imported ${uris.size} assets")
            }
        }
    )

    // Layout view computations
    val totalOriginalBytes = remember(selectedMediaItems.size) {
        selectedMediaItems.sumOf { it.byteSize }
    }

    val estimatedSavingPercentage = remember(quality, scale, selectedFormat) {
        val scaleFactor = (scale / 100f) * (scale / 100f)
        val qualityFactor = (quality / 100f)
        val formatRatio = when (selectedFormat) {
            ImageReducerEngine.OutputFormat.JPEG -> 0.15f
            ImageReducerEngine.OutputFormat.PNG -> 0.65f
            ImageReducerEngine.OutputFormat.WEBP_LOSSY -> 0.10f
            ImageReducerEngine.OutputFormat.WEBP_LOSSLESS -> 0.45f
        }
        val pct = (1.0f - (scaleFactor * qualityFactor * formatRatio)).coerceIn(0.1f, 0.95f) * 100f
        pct.toInt()
    }

    val estimatedCompressedBytes = remember(totalOriginalBytes, estimatedSavingPercentage) {
        val ratio = (100 - estimatedSavingPercentage) / 100f
        (totalOriginalBytes * ratio).toLong()
    }

    LaunchedEffect(Unit) {
        addLog("Media Toolkit initial states stabilized.")
        addLog("Ready to import items via System visual picker contract.")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media & Document Toolkit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        selectedMediaItems.clear()
                        logs.clear()
                        addLog("Toolkit selections and processing feedback cleared.")
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset workspace")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Selected Images Horiz Queue
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected Assets Queue (${selectedMediaItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Photos", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedMediaItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "No images imported yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(selectedMediaItems) { index, item ->
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = item.uri),
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Quick sorting shift-arrows and delete overlays
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(horizontal = 2.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row {
                                                if (index > 0) {
                                                    IconButton(
                                                        onClick = {
                                                            val tmp = selectedMediaItems[index]
                                                            selectedMediaItems.removeAt(index)
                                                            selectedMediaItems.add(index - 1, tmp)
                                                        },
                                                        modifier = Modifier.size(18.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = "Move Left", tint = Color.White, modifier = Modifier.size(10.dp).graphicsLayer(rotationZ = 180f))
                                                    }
                                                }
                                                if (index < selectedMediaItems.size - 1) {
                                                    IconButton(
                                                        onClick = {
                                                            val tmp = selectedMediaItems[index]
                                                            selectedMediaItems.removeAt(index)
                                                            selectedMediaItems.add(index + 1, tmp)
                                                        },
                                                        modifier = Modifier.size(18.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = "Move Right", tint = Color.White, modifier = Modifier.size(10.dp))
                                                    }
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    selectedMediaItems.removeAt(index)
                                                    addLog("Removed visual item from compiled queue: ${item.name}")
                                                },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }

                                    // Item label sizes overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = formatByteSize(item.byteSize),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Real-time storage impact simulator block
            if (selectedMediaItems.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Live Workspace Compression Predictor",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Estimated storage space impact: Original ~${formatByteSize(totalOriginalBytes)} ➜ Reduced ~${formatByteSize(estimatedCompressedBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Approximate Byte Decreased Rate: ~$estimatedSavingPercentage% Savings",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Section 3: Fine-grained Compress Parameters Card Controls
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "⚡ FINE-GRAINED FILE SIZE REDUCTION SETTINGS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Target format buttons
                    Text("Target Format Transmuxer", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ImageReducerEngine.OutputFormat.values().forEach { fmt ->
                            val isPicked = selectedFormat == fmt
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFormat = fmt },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                color = if (isPicked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = fmt.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPicked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Quality slider input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Compression Quality", style = MaterialTheme.typography.bodySmall)
                        Text("${quality.toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = quality,
                        onValueChange = { quality = it },
                        valueRange = 1f..100f,
                        steps = 99,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Resolution multiplier scale slider input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Resolution Scale Multiplier", style = MaterialTheme.typography.bodySmall)
                        Text("${scale.toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 10f..100f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (selectedMediaItems.isEmpty()) {
                                triggerNotification("Please add images to compress")
                                return@Button
                            }
                            isProcessing = true
                            logs.clear()
                            addLog("Starting image size reduction tasks on Dispatchers.Default background loop...")
                            scope.launch {
                                var count = 0
                                var totalSaved = 0L
                                val destFolder = File(baseFolder, "Pictures/Compressed").apply { mkdirs() }
                                
                                selectedMediaItems.forEachIndexed { index, mediaItem ->
                                    try {
                                        addLog("Processing image [${index + 1}/${selectedMediaItems.size}]: ${mediaItem.name}...")
                                        val conf = ImageReducerEngine.CompressionConfig(
                                            quality = quality.toInt(),
                                            scale = scale / 100f,
                                            targetFormat = selectedFormat
                                        )
                                        val result = ImageReducerEngine.compressAndResizeImage(
                                            context = context,
                                            inputUri = mediaItem.uri,
                                            outputDirectory = destFolder,
                                            config = conf
                                        )
                                        addLog("Done: Saved in Pictures/Compressed/ -> ${File(result.outputPath).name}. Size: ${formatByteSize(result.compressedSize)}. saved ~${result.savedBytes / 1024} KB.")
                                        totalSaved += result.savedBytes
                                        count++
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        addLog("Error converting ${mediaItem.name}: ${e.message}")
                                    }
                                }
                                addLog("Success: In-place Matrix scaling finished. Processed $count elements. Total saved storage: ${formatByteSize(totalSaved)}.")
                                isProcessing = false
                                triggerNotification("Successfully processed $count photos!")
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing Image Conversions...")
                        } else {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RUN COMPACT IMAGE REDUCTION")
                        }
                    }
                }
            }

            // Section 4: PDF Compilation Board Card Controls
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "📄 PDF DOCUMENT COMPILATION OPTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // Page dimensions config options
                    Text("Printed Page Size", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PdfCompilationEngine.PageSize.values().forEach { sizeOpt ->
                            val isChosen = pdfPageSize == sizeOpt
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { pdfPageSize = sizeOpt },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isChosen) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                color = if (isChosen) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = sizeOpt.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Printed Page bounds orientation options
                    Text("Page Orientation Layout", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PdfCompilationEngine.PageOrientation.values().forEach { orientOpt ->
                            val isChosen = pdfOrientation == orientOpt
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { pdfOrientation = orientOpt },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isChosen) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                color = if (isChosen) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = orientOpt.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Canvas fits margins configs option
                    Text("Printed Canvas Margins Fittings", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "0.5 inch (36pt)" to 36,
                            "1.0 inch (72pt)" to 72,
                            "Borderless (0pt)" to 0
                        ).forEach { (label, marginValue) ->
                            val isChosen = pdfMargin == marginValue
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { pdfMargin = marginValue },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isChosen) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                color = if (isChosen) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isChosen) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (selectedMediaItems.isEmpty()) {
                                triggerNotification("Please add images to compile")
                                return@Button
                            }
                            isProcessing = true
                            addLog("Starting Image-to-PDF compilation stream...")
                            scope.launch {
                                val documentsFolder = File(baseFolder, "Documents/CompiledPDFs").apply { mkdirs() }
                                val outFilename = "CompiledDoc_${System.currentTimeMillis()}.pdf"
                                val docOutput = File(documentsFolder, outFilename)
                                
                                val compileConfig = PdfCompilationEngine.PdfConfig(
                                    pageSize = pdfPageSize,
                                    orientation = pdfOrientation,
                                    marginPixels = pdfMargin
                                )
                                
                                val uris = selectedMediaItems.map { it.uri }
                                addLog("Drawing image canvases into standard coordinates grid...")
                                val didSucceed = PdfCompilationEngine.compileImagesToPdfFromUris(
                                    context = context,
                                    imageUris = uris,
                                    outputFile = docOutput,
                                    config = compileConfig
                                )
                                
                                if (didSucceed && docOutput.exists()) {
                                    addLog("Completed PDF Building: Saved in Documents/CompiledPDFs/${outFilename}")
                                    addLog("Created File Size: ${formatByteSize(docOutput.length())}")
                                    triggerNotification("PDF Compiled successfully!")
                                } else {
                                    addLog("Error: Canvas rendering returned invalid results.")
                                    triggerNotification("Failed to build PDF")
                                }
                                isProcessing = false
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compiling PDF Document...")
                        } else {
                            Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COMPILE UNIFIED PDF BOOK")
                        }
                    }
                }
            }

            // Section 5: Processing Logging Live terminal Feed
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .height(180.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "📊 ACTIVE COMPRESSION TERMINAL",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFF00FF00),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (logs.isEmpty()) {
                        Text(
                            "> Pipeline listening...",
                            color = Color.LightGray.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    } else {
                        logs.forEach { log ->
                            Text(
                                log,
                                color = Color.Green,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveUriDetails(context: Context, uri: Uri): UriMediaItem {
    var name = "Image_${System.currentTimeMillis()}"
    var size = 0L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex)
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return UriMediaItem(uri, name, size)
}

private fun formatByteSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes Bytes"
    val kb = bytes / 1024f
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024f
    return String.format(Locale.getDefault(), "%.1f MB", mb)
}
