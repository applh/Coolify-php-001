# Android & Gemini API Integration Guide

This guide details the integration architecture between the Android application (`repo-android`) and the Google Gemini API, including an explanation of critical runtime errors resolved during development, best practices, and curated references to official documentation and SDK resources.

---

## 1. Architectural Approach

The system uses a **Bifurcated Secret-Handling Model** to communicate with the Gemini API depending on the client platform:

1. **Web (Vue.js)**: Uses an intermediate backend proxy server (`server.ts`) to avoid exposing sensitive keys over the network to browser client applications.
2. **Android Mobile (`repo-android`)**: Direct client-to-API architecture leveraging the official **Google Generative AI SDK for Android** (`com.google.ai.client.generativeai`).
   - *Why direct?* Mobile binaries handle keys through secure property/CI-CD compilation injection, eliminating the need for a secondary middleware server, lowering latency, and simplifying mobile-applet execution.

---

## 2. Resolving Key API & SDK Errors

A series of client-side-blocking errors were identified and corrected in the mobile module when utilizing the Kotlin SDK:

### Error A: `404 NOT_FOUND` for Model Identifiers
* **Exception Message**:
  ```json
  {
    "error": {
      "code": 404,
      "message": "models/gemini-1.5-flash-001 is not found for API version v1beta, or is not supported for generateContent. Call ModelService.ListModels to see the list of available models and their supported methods.",
      "status": "NOT_FOUND"
    }
  }
  ```
* **Root Cause**: The SDK was configured to use `gemini-1.5-flash-001`. Under the Gemini API `v1beta` endpoint version, specific model definitions like `-001` or raw unstable aliases might fail to resolve or are not supported on standard content generation paths.
* **Resolution**: The model name has been standardized to the fully supported modern alias `gemini-2.5-flash` in the standard model initialization inside `AITeamScreen.kt`.

### Error B: Kotlinx Serialization `MissingFieldException` on Errors
* **Exception Message**:
  ```
  kotlinx.serialization.MissingFieldException: Field 'details' is required for type with serial name 'com.google.ai.client.generativeai.common.server.GRpcError', but it was missing at path: $.error
  ```
* **Root Cause**: The Google Generative AI Kotlin SDK utilizes `kotlinx.serialization` to deserialize JSON errors from the Google backend. The `GRpcError` class in the SDK defines `details` as a non-optional (required) property without a default fallback. When the backend returned a general error (e.g., 404) that did *not* include a `details` array inside the `error` block, the deserialization step crashed before the SDK could propagate the underlying HTTP/REST error.
* **Resolution**: Resolved immediately once the root model naming mismatch was corrected, which bypassed the REST error payload altogether and let the JSON pipeline complete normally. Correcting the model identifier prevents the error response format that triggers the deserializer crash.

---

## 3. Best Practices in Kotlin & Android

### Correct SDK Model Instantiation

To instantiate and use the Gemini API on Android, ensure you use the official Gradle dependencies and construct your model correctly.

**Gradle Dependency Configuration (`app/build.gradle`)**:
```kotlin
dependencies {
    // Official Google Generative AI Android client SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0") // or latest stable
    
    // Kotlin Serialization dependency
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

**Kotlin Implementation (`AITeamScreen.kt`)**:
```kotlin
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

// Correct instantiation pattern using standard models
val generativeModel = GenerativeModel(
    modelName = "gemini-2.5-flash",
    apiKey = geminiApiKey
)

suspend fun generatePrompt(promptText: String): String? {
    return try {
        val response = generativeModel.generateContent(promptText)
        response.text
    } catch (e: Exception) {
        // Capture network or parsing errors gracefully
        Log.e("GeminiIntegration", "Generation failed", e)
        null
    }
}
```

---

## 4. Official Resources & Documentation Links

Always consult the official and verified Google Developer resources for latest patterns:

* **Official Android Quickstart**:
  Learn how to set up the SDK, configure your key, and perform standard and streaming calls in Jetpack Compose:
  👉 [Get started with the Gemini API in Android apps](https://ai.google.dev/gemini-api/docs/quickstart?lang=android)

* **Official SDK Repository**:
  The GitHub repository containing the complete Kotlin code, issue trackers, and advanced multi-turn chat examples:
  👉 [Google Generative AI SDK for Android on GitHub](https://github.com/google/generative-ai-android)

* **Gemini Models Directory**:
  An up-to-date look at available model identifiers, input modalities, output thresholds, and processing guidelines:
  👉 [Official Gemini Models Guide](https://ai.google.dev/gemini-api/docs/models/gemini)
