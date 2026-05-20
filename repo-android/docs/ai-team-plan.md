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

## Technical Details
- **Dependencies**: Networking (Retrofit/OkHttp or `google-ai-client`), Image Loading (Coil-Compose).
- **Permissions**: `INTERNET`, Storage access (if required based on API level and scoped storage preferences).
- **Navigation**: Route `ai_team` in the standard Jetpack Compose navigation host.
