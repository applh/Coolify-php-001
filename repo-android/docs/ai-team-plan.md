# AI Team Applet - Implementation Plan

## Objective
Add a new "AI Team" applet to the Multi-App Hub in `repo-android`. This applet will leverage the Gemini API to provide a chat interface capable of generating text and images. All inputs and outputs will be saved to the local file system so they can be seamlessly managed by the existing File Explorer applet.

## Features & Scope

1. **Simple Chat Interface**
   - A Compose-based chat screen where users can send prompts to the AI.
   - Text generation capabilities (e.g., Markdown, code, plain text).
   - Image generation capabilities (producing image files based on prompts).

2. **User Settings Integration**
   - Integrate with the existing Shared Settings applet.
   - Allow users to input and securely store their `GEMINI_API_KEY` (using Android `DataStore` or `SharedPreferences`).
   - Ensure the AI applet gracefully handles missing or invalid API keys.

3. **File System Integration**
   - **Persistence**: Save generated text responses as `.txt` or `.md` files.
   - **Image Storage**: Save generated images as `.jpg` or `.png` files.
   - **Interoperability**: Store these files in a designated directory accessible by the File Explorer applet (e.g., `Documents/AITeam/`).
   - Save user prompts alongside the generated results or as metadata so the context is preserved.

## Phase 1: Configuration & Settings
- Update `SettingsScreen.kt` and `AppPreferences.kt` to include an input field for the `GEMINI_API_KEY`.
- Provide a mechanism to retrieve this key within the AI applet.
- Add necessary network permissions (`android.permission.INTERNET`) to `AndroidManifest.xml` if not already present.
- Add dependecies for AI integration (e.g., Retrofit or Google AI client SDK) and image processing (e.g., Coil).

## Phase 2: Chat UI Scaffolding
- Create `AITeamScreen.kt` featuring:
  - A scrollable conversational view (messages/outputs).
  - A bottom input bar for entering text prompts.
  - A submit button.
- Integrate the `AITeamScreen` destination into the central `MainActivity` navigation graph.

## Phase 3: Gemini API Integration
- Implement an API service layer to interface with the Gemini API.
- Support `generateContent` for text.
- Parse the responses into structured display formats (Text, Code Snippet, Image URL).
- *Note: Image generation might require a specific model endpoint or proxy, depending on the exact Gemini API capabilities available at runtime.*

## Phase 4: IO & Persistence
- Build a utility class to handle writing Strings (text/markdown) and ByteArrays (images) to local storage.
- On successful API response:
  1. Display the result in the Compose UI.
  2. Write the file to disk asynchronously.
- Ensure proper integration and refresh mechanisms so the newly created files populate instantaneously in the `ExplorerScreen.kt` applet.

## Enhanced AI Team Applet Specifications

### 1. Unified Conversational Workspace (Text by Default)
- **Primary Modality**: The core screen interaction changes from a standalone image generator to a unified multi-modal thread structure (`AITeamScreen.kt`).
- **Input Stream**: Flat text queries are taken as input. Upon submission, the app queries the Gemini API (e.g., standard `gemini-2.5-flash` model endpoint) for a corresponding markdown text response.
- **Output Stream**: Responses are formatted dynamically using scrollable Markdown/rich text components. Conversation threads are serialized to the local file system under `Documents/AITeam/` as `.md` or `.txt` content blocks so the File Explorer can read and display them.

### 2. Checkbox-Triggered Image Generation
- **User Interface Alignment**: Place a custom-styled Checkbox or Switch directly to the side of the text prompt field in the bottom input pane (e.g. `[ ] Generate Image`).
- **Conditional Dispatch Logic**:
  - **Checkbox Unchecked**: Default behavior. Calls standard chat model to obtain response dialogue structure, generating text answers.
  - **Checkbox Checked**: Intercepts the request and directs the prompt to an image-generating model instance (e.g., Gemini Image or a relative REST endpoint). The resulting Base64 payload is parsed, decoded into a clean Bitmap, displayed on-thread as an inline media asset, and logged into the active session gallery.

### 3. Smart Session Continuity (Last Session Active & Restart Control)
- **Default Load Behavior**: When booting the AI App, the ViewModel scans local metadata files (such as `sessions.json` or `session_manifests/`) to locate the last used session context (the entry with the highest timestamp). If found, it recovers and displays all historical conversation messages, attachments, and visual gallery items automatically.
- **"New Session" Lifecycle Trigger**: Add a clear, easily accessible "Start New Session" control inside the primary header toolbar (represented as a plus `+` button or a styled "New Chat" icon). Upon press:
  - Generates a new unique `UUID` for the current thread index.
  - Formally writes any pending active session records to the file system.
  - Clears the active chat bubble states from the layout, welcoming the user with a fresh prompt workspace.

### 4. Interactive Image Gallery Lightbox (Multitouch Zoom & Pan)
- **Dismissible Overlay**: Tapping on any image preview within the active session stream or the "Session Highlights Gallery" launches a high-resolution, full-screen Overlay Card or Dialog Modal acting as a Lightbox.
- **Gestural Transformation Controller**:
  - Maintain mutable states inside the Lightbox composable for scale factor (e.g., `scale: Float = 1.0f`) and layout tracking (e.g., `offset: Offset = Offset.Zero`).
  - Listen for multi-touch inputs by utilizing Jetpack Compose's `Modifier.pointerInput` with `detectTransformGestures`.
  - Process touch zoom/pan inputs at the hardware-accelerated rendering layer by binding the values directly:
    ```kotlin
    Modifier.graphicsLayer(
        scaleX = scale,
        scaleY = scale,
        translationX = offset.x,
        translationY = offset.y
    )
    ```
  - Provide a floating "Close Lightbox" option (or drag/swipe-to-dismiss gesture) which automatically resets scale and panning values to zero before removing the viewport overlay.

## Technical Details
- **Dependencies**: Networking (Retrofit/OkHttp or `google-ai-client`), Image Loading (Coil-Compose).
- **Permissions**: `INTERNET`, Storage access (if required based on API level and scoped storage preferences).
- **Navigation**: Route `ai_team` in the standard Jetpack Compose navigation host.
