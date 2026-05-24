# Android Media & File Manager Implementation Plan

This document details the architectural evaluation, technical designs, and step-by-step phased roadmap for integrating a professional-grade **Media & File Manager** in `repo-android`. 

Our current implementation includes a basic `ExplorerScreen.kt` that supports flat-listing of selective directories, image zoom gestures, local text viewing, and simple ExoPlayer playback. This plan evaluates two primary paths:
1. **Option A (In-Place Upgrade):** Upgrading the existing `ExplorerScreen` to a unified, multi-category, directory-browsing **File & Media Manager**.
2. **Option B (Separate Applet):** Keeping the simple `ExplorerScreen` as a low-level diagnostic utility and engineering a dedicated, feature-rich **Media Manager Applet** (`com.example.cameraxapp.mediamanager`) complete with system-level `MediaStore` querying, secure private vault directories, and AI-powered categorization.

---

## 1. Comparative Evaluation & Trade-offs

| Criterion | Option A: In-Place Upgrade of `ExplorerScreen` | Option B: Dedicated Media Manager Applet |
| :--- | :--- | :--- |
| **Scope & Focus** | Unified tool for all system files, documents, downloads, and key media files. Broad and utility-oriented. | Narrowly optimized for visual imagery, raw audio, video albums, metadata indexing, and playback HUDs. |
| **Architectural Impact** | Zero database modifications unless directory caching is needed. Extends current screens linearly. | Requires dedicated SQLite schema, background Worker services for content indexing, and private folder system keys. |
| **User Experience (UX)** | Highly cohesive for power-users who want to view, rename, move, and edit all data in a unified tree layout. | Streamlined, simplified layout focusing solely on visual content streams, albums, favorites, and secure locks. |
| **APK Footprint Increment** | Practically 0KB (reuses coil, media3, compose, and preferences already in dependencies). | Mini-increment (< 15KB) for database helpers and isolated worker components. |
| **Performance Overhead** | High performance inside directory nodes; can slow down when walking deep file trees recursively without database caching. | Exceptional performance; queries cached indices indexed natively via `MediaStore` and asynchronous Room/SQLite tables. |
| **Resource Isolation** | Simple UI thread file loaders; risks of frame drops on large folders without paging. | High isolation. Backed by separate view-models, background scanners (`MediaSynchronizerWorker`), and segmented caches. |

### 🧭 Recommendation Strategy
* **Recommendation:** A **Hybrid Component Architecture** is selected. We will upgrade the existing **Explorer Screen** into a full-scale **Unified File & Directory Manager** to handle traditional disk-based operations (moving, deleting, nested directories, files category views). Meanwhile, we lay down a modular **Media Core API** that can feed into a future specialized **Media Manager Applet** (focusing on secure vault encryption, media indexing, and AI metadata tag mapping).

---

## 2. Option A Blueprint: Unified File & Media Manager
This track upgrades `ExplorerScreen.kt` to support folder tree hierarchies, category-segmented shortcuts, file manipulation queries (move/copy), and multi-selection operations.

### A. Extended Navigation Architecture & Folder Trees
Our current `ExplorerScreen` filters static root directories into a flat list. To support general storage access, we introduce a recursive directory state engine:

```kotlin
// In com.example.cameraxapp.explorer
data class DirectoryState(
    val currentDirectory: File,
    val parentFoldersStack: List<File> = emptyList(),
    val isPagingActive: Boolean = false,
    val offset: Int = 0
)

class ExplorerViewModel(private val rootLocation: File) : ViewModel() {
    private val _state = MutableStateFlow(DirectoryState(rootLocation))
    val state: StateFlow<DirectoryState> = _state

    fun navigateToFolder(subFolder: File) {
        if (subFolder.isDirectory && subFolder.exists()) {
            val currentStack = _state.value.parentFoldersStack.toMutableList()
            currentStack.add(_state.value.currentDirectory)
            _state.value = _state.value.copy(
                currentDirectory = subFolder,
                parentFoldersStack = currentStack
            )
        }
    }

    fun navigateBack(): Boolean {
        val stack = _state.value.parentFoldersStack.toMutableList()
        if (stack.isNotEmpty()) {
            val previousFolder = stack.removeAt(stack.size - 1)
            _state.value = _state.value.copy(
                currentDirectory = previousFolder,
                parentFoldersStack = stack
            )
            return true // Navigation handled
        }
        return false // Exits explorer
    }
}
```

```
+--------------------------------------------------------------------------+
|  [Explorer Core] :: Directory Path: /storage/emulated/0/Documents/       |
+--------------------------------------------------------------------------+
| [Category Shortcuts Router Hub - Tap to filter globally]                 |
|  📁 All Files (243)  |  🖼️ Images (112)  |  🎥 Videos (14)  |  📄 Docs (117) |
+--------------------------------------------------------------------------+
| ⇅ Name (A-Z)                                                     🔍 Search |
|                                                                          |
|  [📁 Backup DBs]         [📁 Camera Raw]            [📁 Downloads]         |
|  Modified: Today, 14:10  Modified: May 20, 18:22    Modified: May 12, 09:00|
|                                                                          |
|  [🖼️ photo_001.jpg]      [📄 config.json]           [🎥 clip_rec.mp4]      |
|  Size: 2.1 MB            Size: 12 KB                Size: 45 MB            |
|                                                                          |
+--------------------------------------------------------------------------+
|  📊 Disk Space Widget: [████████████░░░░░░░░░░] 54% Used (32GB Free of 64GB)|
+--------------------------------------------------------------------------+
```

### B. Disk Manipulation Engine (Non-Blocking Channel Streams)
To perform file transfers (Copy, Move, Bulk Renaming) without dropping UI frames, the engine employs cooperative Kotlin coroutines with pre-allocated 16KB stream buffers:

```kotlin
package com.example.cameraxapp.explorer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileOperatorEngine {

    /**
     * Cooperatively copies a file with progress updates.
     * Prevents UI stutter by operating on Dispatchers.IO.
     */
    fun copyFile(source: File, destination: File): Flow<Float> = flow {
        if (!source.exists()) throw IllegalArgumentException("Source file does not exist")
        
        withContext(Dispatchers.IO) {
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    val totalBytes = source.length()
                    val buffer = ByteArray(16384) // 16KB high-speed page-size buffer
                    var bytesRead: Int
                    var totalCopied = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalCopied += bytesRead
                        if (totalBytes > 0) {
                            emit(totalCopied.toFloat() / totalBytes)
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Executes bulk file transfers matching specific filters.
     */
    suspend fun moveFiles(files: List<File>, targetDirectory: File, onProgress: (Int, Int) -> Unit) {
        withContext(Dispatchers.IO) {
            if (!targetDirectory.exists()) targetDirectory.mkdirs()
            
            files.forEachIndexed { index, file ->
                val destFile = File(targetDirectory, file.name)
                // Attempt direct file-system atomic rename first, fallback to copy-delete
                val success = file.renameTo(destFile)
                if (!success) {
                    copyFile(file, destFile).collect {}
                    file.delete()
                }
                onProgress(index + 1, files.size)
            }
        }
    }
}
```

---

## 3. Option B Blueprint: Separate Media Manager Applet
This track designs a fully isolated screen and service namespace mapping local media properties (images/videos/audios) into a clean, indices-backed album catalog.

### A. Media Database Schema (`MediaManagerDatabase.kt`)
To persist categorization, secure pins, favorites, and Gemini-compiled semantic descriptors, we establish a robust multi-table schema:

```sql
-- Track and group media albums dynamically, independent of absolute folder paths
CREATE TABLE IF NOT EXISTS local_albums (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    album_name TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL,
    accent_color TEXT DEFAULT "#1E1E2C"
);

-- Media Metadata Catalog Index
CREATE TABLE IF NOT EXISTS offline_media_items (
    id TEXT PRIMARY KEY,               -- SHA-256 or MD5 file checksum hash
    absolute_path TEXT NOT NULL,
    filename TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    size_bytes INTEGER NOT NULL,
    duration_ms INTEGER DEFAULT 0,       -- Videos and audio files only
    captured_at INTEGER NOT NULL,
    album_id INTEGER DEFAULT NULL,
    is_favorite INTEGER DEFAULT 0,
    is_vault_secured INTEGER DEFAULT 0, -- Set to 1 if encrypted inside private vault
    ai_generated_tags TEXT DEFAULT NULL,-- JSON array representing recognized semantic tags
    FOREIGN KEY(album_id) REFERENCES local_albums(id) ON DELETE SET NULL
);

-- Vault Password and Device Pin Keys
CREATE TABLE IF NOT EXISTS secure_vault_config (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    salt TEXT NOT NULL,
    verifier_hash TEXT NOT NULL,       -- PBKDF2 representation of authentication pin
    setup_date INTEGER NOT NULL,
    recovery_phrase_encrypted TEXT NOT NULL
);
```

### B. High-Performance Media Synchronizer Background Worker
We avoid freezing interactions during large-folder audits. A custom `CoroutineWorker` schedules background scans detecting changes in defined directories, computing image hashes, and updating our local database:

```kotlin
package com.example.cameraxapp.mediamanager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okio.HashingSink
import okio.blackholeSink
import okio.buffer
import okio.source
import java.io.File
import java.security.MessageDigest

class MediaSynchronizerWorker(
    context: Context,
    params: WorkerParameters,
    private val dbHelper: MediaManagerDatabaseHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i("MediaSyncWorker", "Executing scheduled incremental media storage scans...")
        
        return try {
            val monitoredDirectories = listOf(
                applicationContext.filesDir,
                applicationContext.getExternalFilesDir(null)
            ).filterNotNull()

            monitoredDirectories.forEach { root ->
                scanMediaFilesRecursively(root)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("MediaSyncWorker", "Incremental synchronization broken: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun scanMediaFilesRecursively(directory: File) {
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanMediaFilesRecursively(file)
            } else {
                val extension = file.extension.lowercase()
                if (extension in listOf("jpg", "png", "mp4", "mp3", "wav")) {
                    processMediaEntity(file)
                }
            }
        }
    }

    private fun processMediaEntity(file: File) {
        val path = file.absolutePath
        // Check if file is already tracked by checking modification values or checksum
        if (!dbHelper.isPathIndexed(path, file.lastModified())) {
            val fileHash = generateSHA256Checksum(file)
            val mimeType = when (file.extension.lowercase()) {
                "mp4" -> "video/mp4"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "png" -> "image/png"
                else -> "image/jpeg"
            }
            
            dbHelper.registerMediaItem(
                id = fileHash,
                path = path,
                name = file.name,
                mimeType = mimeType,
                size = file.length(),
                modifiedStamp = file.lastModified()
            )
            Log.d("MediaSyncWorker", "Newly indexed media entity: ${file.name}")
        }
    }

    private fun generateSHA256Checksum(file: File): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                messageDigest.update(buffer, 0, bytesRead)
            }
        }
        return messageDigest.digest().joinToString("") { "%02x".format(it) }
    }
}
```

### C. AI Intelligent Smart Tagging Workspace
To offer Google Photos-style smart search ("Sunset", "Receipt", "Document", "Dog"), we compile high-level prompts and coordinate analysis via the Gemini SDK.

```kotlin
package com.example.cameraxapp.mediamanager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaTaggingEngine(private val apiKey: String) {

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash", // Modern, rapid multi-modal model
        apiKey = apiKey
    )

    /**
     * Analyzes image content using Gemini Multi-Modal and extracts a semantic CSV array of tags.
     */
    suspend fun generateAutotags(imageFile: File): List<String> = withContext(Dispatchers.IO) {
        try {
            // Resize image dynamically to fit input tokens and reduce data transfers
            val rawBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, 512, 512, true)
            rawBitmap.recycle()

            val aiPrompt = content {
                image(scaledBitmap)
                text(
                    "Analyze this image carefully. Generate exactly 5 metadata keywords that describe the scenery, " +
                    "primary objects, colors, or categorization (e.g. Doc, Portrait, Sunset, Code, Invoice, Outdoor). " +
                    "Return ONLY a flat, comma-separated list. No preambles, no quotes, no conversational text."
                )
            }

            val response = model.generateContent(aiPrompt)
            scaledBitmap.recycle()

            response.text?.split(",")
                ?.map { it.trim().lowercase() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

        } catch (e: Exception) {
            listOf("uncategorized", "error_${e.localizedMessage?.take(10)}")
        }
    }
}
```

### D. Secure Storage Cryptographic Vault
Safeguarding physical assets inside an encrypted sandbox folder using standard `AES/GCM/NoPadding` transformations:

```kotlin
package com.example.cameraxapp.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptographicVault {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val VAULT_ALIAS = "AppletMediaVaultKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(VAULT_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(VAULT_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(VAULT_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun encryptMediaFile(plainFile: File, encryptedFile: File) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        
        val iv = cipher.iv
        FileOutputStream(encryptedFile).use { out ->
            // Prepend the 12-byte initialization vector (IV) at the body start to facilitate decryption
            out.write(iv)
            
            FileInputStream(plainFile).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val outputBytes = cipher.update(buffer, 0, bytesRead)
                    if (outputBytes != null) out.write(outputBytes)
                }
                val finalBytes = cipher.doFinal()
                if (finalBytes != null) out.write(finalBytes)
            }
        }
    }

    fun decryptMediaFile(encryptedFile: File, plainFile: File) {
        FileInputStream(encryptedFile).use { input ->
            val iv = ByteArray(12)
            input.read(iv) // Raw cipher starts after the vector
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            FileOutputStream(plainFile).use { out ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val decryptedBytes = cipher.update(buffer, 0, bytesRead)
                    if (decryptedBytes != null) out.write(decryptedBytes)
                }
                val finalBytes = cipher.doFinal()
                if (finalBytes != null) out.write(finalBytes)
            }
        }
    }
}
```

---

## 4. UI/UX Interface Rendering Optimization
High-density grid renders risk severe OOM anomalies on Android if not optimized. The following guidelines regulate bitmap handling:

* **Hardware Bitmaps Configuration:** Enable `Bitmap.Config.HARDWARE` inside Coil loaders to keep pixel data inside GPU memory, instantly avoiding JVM Heap allocation leaks.
* **Lazy Columns Preheating:** Set a standard caching limit inside grids:
  ```kotlin
  LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 100.dp),
      // Prevent off-screen rendering allocation spikes by loading limited items on memory
      modifier = Modifier.fillMaxSize()
  )
  ```
* **ExoPlayer Release Cycle:** Ensure seamless integration with lifecycle listeners. Instantly release ExoPlayer nodes during `onDispose` to keep background memory clear:
  ```kotlin
  DisposableEffect(file) {
      onDispose {
          exoPlayer.stop()
          exoPlayer.release()
      }
  }
  ```

---

## 5. Security & Permission Profile Specifications
These properties ensure safe storage interaction under Android Scoped Storage guides.

### Custom Provider Rules (`AndroidManifest.xml` / `provider_paths.xml`)
Secure programmatic content integration using standard FileProviders blocks external context lookups:

```xml
<!-- Under AndroidManifest.xml application tags -->
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.provider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/provider_paths" />
</provider>
```

```xml
<!-- Inside com.example.cameraxapp/app/src/main/res/xml/provider_paths.xml -->
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/of/android">
    <!-- Grants outer sharing privileges solely for designated directories -->
    <files-path name="internal_files" path="." />
    <external-files-path name="external_files" path="." />
</paths>
```

---

## 6. Staged Implementation & Rollout Schedule

### 🔋 Phase 1: Upgrade Directory Parsing & Workspace Paths
1. Standardize recursive directories stack tracking in `ExplorerScreen.kt`.
2. Add horizontal categories panel shortcuts (Icons, Videos, Docs, Text) to filter overall flat models immediately.
3. Integrate directory back stack mechanisms mapping Compose triggers perfectly back to the hub.

### 💾 Phase 2: Create Multi-Selection Operations HUD
1. Connect multi-selection controllers allowing clients to tap and highlight multiple folders/files.
2. Build action overlays containing buttons for Copy, Move, and Bulk Renaming operations.
3. Implement `FileOperatorEngine` using buffered stream builders on background coroutine pools.

### 🔐 Phase 3: Setup Isolated Cryptographic Vault Enclosure
1. Initialize the `AndroidKeyStore` secret generator configuration.
2. Develop the local encrypter routines wrapping user items inside private isolated folder directories.
3. Build the Dialog PIN entry window to guard secure access with customized states.

### 🤖 Phase 4: Enable AI Smart Catalog & Automated Indexers
1. Create SQLite tables keeping track of index markers, metadata properties, and Gemini autotags.
2. Add the background `MediaSynchronizerWorker` task scheduler tracking files additions/modifications.
3. Implement the multi-modal auto-tagger calling the `gemini-2.5-flash` model structure.

### 🧪 Phase 5: Verification & Production Run compiles
1. Validate strict compile schemas by running `compile_applet`.
2. Clean memory profiles, double-checking correct Coil bitmap recycles.
