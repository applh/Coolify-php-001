# Android & Gemini API Integration Guide

This guide details the integration architecture between the Android application (`repo-android`) and the Google Gemini API. It highlights direct client-to-API communication, specific workarounds for image synthesis over REST/Retrofit (sidestepping standard SDK limitations), decoding payload mechanics, writing to local storage under modern Android Scoped Storage policies, and secure system sharing via `FileProvider`.

---

## 1. Architectural Approach

The system uses a **Bifurcated Secret-Handling Model** to handle Gemini API communication safely depending on the client platform:

1. **Web (Vue.js)**: Utilizes an intermediate backend proxy server (`server.ts`) to avoid exposing sensitive keys over the network to browser client apps.
2. **Android Mobile (`repo-android`)**: Executes a direct client-to-API architecture leveraging local client configurations.
   - *Why direct?* Mobile binaries handle keys through secure local storage or build-time property injection (`local.properties`), eliminating the need for intermediary middleware servers, lowering latency, and simplifying mobile-applet execution.

---

## 2. Text Generation via Official SDK vs. Image Generation via Retrofit/REST

While text generation fits perfectly within the official Kotlin SDK, image generation requires a structured fallback due to current SDK API constraints.

### A. Core Text Generation (Official SDK)
For standard text generation, chat conversations, and basic query tasks, the app relies on the official **Google Generative AI Android Client SDK** (`com.google.ai.client.generativeai`).

**Gradle Dependency Configuration (`app/build.gradle`)**:
```kotlin
dependencies {
    // Official Google Generative AI Android client SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0") // or latest stable
    
    // Kotlin Serialization dependency
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

**Kotlin Usage Pattern**:
```kotlin
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

// Standard initialization with modern model aliases
val generativeModel = GenerativeModel(
    modelName = "gemini-2.5-flash",
    apiKey = geminiApiKey
)

suspend fun generatePrompt(promptText: String): String? {
    return try {
        val response = generativeModel.generateContent(promptText)
        response.text
    } catch (e: Exception) {
        Log.e("GeminiIntegration", "Generation failed", e)
        null
    }
}
```

### B. Core Image Generation (Bypassing SDK constraints via Retrofit/REST)
Currently, the official Android client SDK focuses primarily on text/multimodal text-input paths and does not directly support high-level image output synthesis configurations (such as standard aspect-ratio parameters or output image sizes natively via the `generateContent` API). 

To overcome this constraint, **Lumina AI** inside `repo-android` communicates directly with Google's REST interface (`https://generativelanguage.googleapis.com/`) using **Retrofit**, **Moshi Serialization**, and **OkHttp**.

#### 1. REST Request Specification
* **Endpoint Path**: `POST v1beta/models/{model}:generateContent`
* **Query Parameter**: `key={API_KEY}`
* **Active Models**: `gemini-2.5-flash-image` (Standard) or `gemini-3.1-flash-image-preview` (High-Detail)
* **Request JSON Payload**:
  ```json
  {
    "contents": [
      {
        "parts": [
          { "text": "Mechanical paint cat on glowing neon street, synthwave retro" }
        ]
      }
    ],
    "generationConfig": {
      "imageConfig": {
        "aspectRatio": "1:1",
        "imageSize": "1K"
      },
      "responseModalities": [ "TEXT", "IMAGE" ]
    }
  }
  ```

#### 2. Retrofit API Declaration (`AITeamScreen.kt`)
The REST request is mapped directly to a type-safe Kotlin interface:
```kotlin
interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
```

#### 3. Moshi Request/Response Schema Mapping
```kotlin
data class GeminiRequest(
    val contents: List<ContentPartList>,
    val generationConfig: GenerationConfig
)

data class ContentPartList(
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String
)

data class GenerationConfig(
    val imageConfig: ImageConfig,
    val responseModalities: List<String> = listOf("TEXT", "IMAGE")
)

data class ImageConfig(
    val aspectRatio: String,
    val imageSize: String? = null
)

// Response classes mapping base64 payload
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val content: ResponseContent? = null
)

data class ResponseContent(
    val parts: List<ResponsePart>? = null
)

data class ResponsePart(
    val text: String? = null,
    val inlineData: ResponseInlineData? = null
)

data class ResponseInlineData(
    val mimeType: String? = null,
    val data: String? = null
)
```

---

## 3. Image Synthesis Output Extraction & Base64 Decoding

When the API successfully processes the request, the response candidates contain a list of output parts. Unlike text, the image output is stored within a sibling part labeled with `inlineData` instead of raw `text`.

### A. Response Extraction Mechanism
Because the model can emit multiple media parts (e.g., descriptive text metadata coupled with raw image frames), the client must proactively iterate across parts to locate the `inlineData` part containing the base64 encoded payload:

```kotlin
val contentParts = response.candidates?.firstOrNull()?.content?.parts
val inlinePart = contentParts?.firstOrNull { it.inlineData != null && it.inlineData.data != null }
```

### B. Safe Base64-to-Bitmap Transcoding (`StorageUtils.kt`)
Parsing blocks of text to raw pixels requires sanitizing the stream (cleaning out formatting characters like `\n` or `\r`) and translating bytes inside a non-blocking background context (`Dispatchers.IO`) to protect UI fluidity:

```kotlin
fun base64ToBitmap(base64Str: String): Bitmap? {
    return try {
        // Sanitize stream from potential padding/newlines
        val cleanBase64 = base64Str.trim()
            .replace("\n", "")
            .replace("\r", "")
        
        // Decode raw bytes via Android Util library
        val decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
        
        // Construct native Bitmap instance
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
```

---

## 4. Local Storage Architecture as Media Files

Once a dynamic bitmap is processed, keeping it in memory is unsafe due to typical process death. The app persistently archives the generated image into the device's secondary storage. 

To satisfy the Android ecosystem's strict target security requirements, the app operates a multi-tier pipeline using **Scoped Storage** for modern versions (API 29+) with a **Legacy File Fallback** for backward compatibility (API 28-).

### A. Modern Scoped Storage Path (Android 10 / API 29+)
On modern Android devices, app sandboxing prevents arbitrary file writes to root public storage directories without intensive system permissions. Instead, apps write directly to standard public media paths (like `/Pictures/`) using the native `MediaStore` API. 

This workflow operates without requesting any storage permissions like `WRITE_EXTERNAL_STORAGE`:

```kotlin
val displayName = "Lumina_${System.currentTimeMillis()}.jpg"
val mimeType = "image/jpeg"
val resolver = context.contentResolver

val contentValues = ContentValues().apply {
    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
    // Save image under custom folder inside Pictures directory
    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GeminiCanvas")
    // Target IS_PENDING = 1 to freeze indexing during active lock/write
    put(MediaStore.MediaColumns.IS_PENDING, 1)
}

val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
if (imageUri != null) {
    try {
        resolver.openOutputStream(imageUri).use { os ->
            if (os != null) {
                // Compress the raw bitmap natively to Jpeg with 95% quality metrics
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, os)
            }
        }
        // Commit and inform systems of available media by flipping the pending state
        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(imageUri, contentValues, null, null)
    } catch (e: Exception) {
        // Safe delete cleanups on channel errors
        resolver.delete(imageUri, null, null)
    }
}
```

#### Why `IS_PENDING` Is Essential
* When `IS_PENDING` is set to `1`, the image is owned selectively by the writing application. Third-party applications, scanning workers, and custom file systems cannot see the incomplete byte stream.
* Once the write transaction completes cleanly, updating `IS_PENDING` to `0` immediately lets the Android Media Store system index and broadcast the new file for other gallery viewers.

### B. Legacy Storage Fallback Path (Android 9 / API 28 and Lower)
On ancestral APIs, Scoped Storage mechanisms are not available. The application writes directly to external public media paths and manually registers the files with the media indexing daemon:

```kotlin
val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
val appDir = File(picturesDir, "GeminiCanvas")
if (!appDir.exists()) {
    appDir.mkdirs()
}
val imageFile = File(appDir, displayName)

try {
    FileOutputStream(imageFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
    }
    
    // Explicitly scan the file on disk to register it within MediaStore indexes
    MediaScannerConnection.scanFile(
        context,
        arrayOf(imageFile.absolutePath),
        arrayOf(mimeType),
        null
    )
} catch (e: Exception) {
    e.printStackTrace()
}
```

---

## 5. Secure Cross-Application Sharing Framework

Exposing raw file URLs (`file:///...`) directly via standard intents has been forbidden since Android 7.0 (Nougat) and triggers a `FileUriExposedException`. To securely share generated media with messaging apps or email clients, the application implements the **FileProvider Sandbox** protocol:

1. **Write to App Sandbox Directory**: Compress and save the bitmap as a temporary file in the application's secure private cache space (`cacheDir/images`).
2. **Convert to Provider URI**: Build an isolated authority URI using `FileProvider.getUriForFile`.
3. **Intent Configuration**: Grant temporary, read-only permissions explicitly to the recipient package via `Intent.FLAG_GRANT_READ_URI_PERMISSION`.

```kotlin
// Retrieve private sandbox cache dir of the app
val cachePath = File(context.cacheDir, "images")
cachePath.mkdirs()
val file = File(cachePath, "lumina_shared.jpg")

FileOutputStream(file).use { stream ->
    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
}

val authority = "${context.packageName}.provider"
val contentUri = FileProvider.getUriForFile(context, authority, file)

if (contentUri != null) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant strict temporary access
        setDataAndType(contentUri, context.contentResolver.getType(contentUri))
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_TEXT, "✨ Generated image using Lumina AI!\nPrompt: \"$prompt\"")
        type = "image/jpeg"
    }
    val chooserIntent = Intent.createChooser(shareIntent, "Share Masterpiece Canvas")
    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooserIntent)
}
```

---

## 6. Curated Error Trapping Checklist

If you encounter pipeline interruptions during Android development or testing, use this checklist to diagnose standard behaviors:

1. **`404 NOT_FOUND` for Model Identifier**:
   - *Message*: `models/gemini-1.5-flash-001 is not found...`
   - *Fix*: Standardize model configurations on the latest stable REST endpoints, specifically `gemini-2.5-flash-image` or `gemini-3.1-flash-image-preview`.
2. **`MissingFieldException` during Error Serialization**:
   - *Message*: `Field 'details' is required for type with serial name 'com.google.ai.client.generativeai.common.server.GRpcError'...`
   - *Fix*: This occurs when the client deserializes error envelopes. Bypassing other baseline exceptions (e.g. model name) resolves the serializer pipeline naturally.
3. **Empty Canvas or Zero Byte Responses**:
   - *Message*: `Fermion Collision` error.
   - *Fix*: Ensure both `generationConfig.responseModalities` is specifically parsed with `["TEXT", "IMAGE"]` and that you are scanning the `parts` array recursively to locate the correct part containing the base64 string bytes under `inlineData`.

---

## 7. Official Resources & Documentation Links

Observe standard configurations directly via official platforms:

* **Official Android Quickstart**:
  👉 [Get started with the Gemini API in Android apps](https://ai.google.dev/gemini-api/docs/quickstart?lang=android)
* **Official SDK Repository**:
  👉 [Google Generative AI SDK for Android on GitHub](https://github.com/google/generative-ai-android)
* **Android Scoped Storage Guidelist**:
  👉 [Android Storage training and platform media rules](https://developer.android.com/training/data-storage)
