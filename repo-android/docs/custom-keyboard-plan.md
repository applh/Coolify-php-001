# Android System Custom Keyboard Implementation Plan

## Objective
Design and implement a system-wide, custom software keyboard (Input Method Editor - IME) for Android device users. The keyboard will be built using Kotlin, utilizing modern Jetpack Compose for its layout and interface, and will integrate seamlessly with the existing application database, preferences, and the Gemini AI system-prompter to provide real-time writing assistance, template insertion, and clipboard management across any application on the device.

---

## Background & Constraints

Implementing an Android keyboard differs fundamentally from standard app screens:
1. **Service-Based Lifecycle:** Unlike standard Compose layouts driven by an `Activity`, a custom keyboard is a service of type `android.inputmethodservice.InputMethodService`. 
2. **Hosting Compose in a Service:** Since Jetpack Compose depends on a regular Activity lifecycle, a Custom Service must manually establish a `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner` on a custom wrapper view (e.g., `ComposeView`) to render Compose elements securely without crashing.
3. **Strict OS Security Constraints:** Keyboards run with elevated trust. Because they can capture credentials, Android restricts internet access or clipboard monitoring on some devices unless permissions are explicitly handled, and password fields require traditional character mapping without autocorrect/AI prompts triggered.
4. **Hardware Performance Requirements:** Keyboards must render with sub-10ms response times. Zero-gc memory overhead during keystroke inputs is essential to avoid keyboard stuttering.

---

## Requirements & Features

### 1. Unified Material 3 Design
* **Adaptive Cyber-Neon Theme:** Matches the user's preferred layout profile saved in `AppPreferences` (such as Mono-Red or Aurora).
* **Tactile Haptic Feedback:** Vibrates subtly on keypresses based on system haptic preferences.
* **Responsive Layouts:** Adjusts gracefully between portrait keys grid and landscape wide formats.

### 2. High-Performance Input Engine
* **Standard QWERTY Layout:** Comprehensive character keys, modifier keys (Shift, Caps Lock), backspace, space, and a dedicated return action.
* **Dynamic Action Keys:** Adapts the "Enter" key symbol matching standard Android `EditorInfo` input types (e.g., Search, SEND, Done, Go).
* **Multi-Language Subtype Support:** Declares English (US) and custom locales inside system resource files.

### 3. Integrated AI Writing Co-Pilot (The Generative Assistant)
* **Smart Rewriter:** A subtle toolbar sitting above the character keyboard that prompts the user's typed text. Tap "AI Rewrite" to automatically rephrase, expand, or summarize text on the fly.
* **Contextual Prompter:** Queries our established `AITeamScreen` or server-side Gemini endpoints using the active developer key to translate, fix grammar, or change tone (e.g., Professional, Casual, Academic) in real time.
* **Direct Input Injection:** Directly streams or replaces text selection within the current target text editor.

### 4. Interactive Clipboard & Utility Panel
* **Dynamic Clipboard Manager:** Allows users to access historical text copies saved in our sqlite db and tap to paste them immediately.
* **Quick Templates Bin:** Inserts pre-defined text structures (e.g., contact cards, links, standard emails) saved in `AppPreferences`.
* **Sovereign Shortcuts:** Fast redirection toggles to open the main launcher apps (Fraise workspace, Roguelike deck, Blackjack applet) with a single click.

---

## Technical Approach

### 1. Android Manifest Registration
We must declare our custom input service in `AndroidManifest.xml` with specialized permissions and an intent-filter:

```xml
<service
    android:name=".keyboard.FraiseInputMethodService"
    android:label="@string/keyboard_label"
    android:permission="android.permission.BIND_INPUT_METHOD"
    android:exported="true">
    <intent-filter>
        <action android:name="android.view.InputMethod" />
    </intent-filter>
    <meta-data
        android:name="android.view.im"
        android:resource="@xml/method" />
</service>
```

### 2. Resource XML Declarations
Declare keyboard subtype configurations inside `res/xml/method.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<input-method xmlns:android="http://schemas.android.com/apk/res/android"
    android:settingsActivity="com.example.cameraxapp.MainActivity">
    <subtype
        android:icon="@drawable/ic_keyboard_subtype"
        android:label="@string/subtype_en_us"
        android:imeSubtypeLocale="en_US"
        android:imeSubtypeMode="keyboard" />
</input-method>
```

### 3. Hosting Jetpack Compose inside `InputMethodService`
Create a custom `InputMethodService` class and override `onCreateInputView()` to instantiate and return a `ComposeView`. By configuring a custom Lifecycle Owner and parent composition, we can leverage Composable keys layouts seamlessly:

```kotlin
package com.example.cameraxapp.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

class FraiseInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val store = ViewModelStore()
    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        
        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FraiseInputMethodService)
            setViewTreeViewModelStoreOwner(this@FraiseInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@FraiseInputMethodService)
            
            setContent {
                KeyboardLayout(
                    onKeyPress = { code -> handleKeystroke(code) },
                    onActionPress = { performEditorAction() },
                    onAiRewrite = { triggerAiTextEnhancement() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    private fun handleKeystroke(code: Int) {
        val ic = currentInputConnection ?: return
        when (code) {
            -1 -> ic.deleteSurroundingText(1, 0) // Backspace
            else -> ic.commitText(code.toChar().toString(), 1)
        }
    }

    private fun performEditorAction() {
        val ic = currentInputConnection ?: return
        ic.performEditorAction(currentInputEditorInfo.actionId)
    }
    
    private fun triggerAiTextEnhancement() {
        val ic = currentInputConnection ?: return
        // Fetch selected text, run it through the AI agent, and replace selection
        val selectedText = ic.getSelectedText(0) ?: ""
        if (selectedText.isNotEmpty()) {
            // Asynchronously dispatch to Gemini Client ...
        }
    }
}
```

---

## Step-by-Step Implementation Plan

### Phase 1: Lifecycle & Manifest Setup
1. **Declare Service:** Map `FraiseInputMethodService` in `AndroidManifest.xml` with `BIND_INPUT_METHOD` permissions.
2. **Create XML Metadata:** Create `res/xml/method.xml` defining the subtype locales. Add basic localized strings to `strings.xml`.
3. **Build Core Lifecycle Scaffold:** Implement the custom service class handling synthetic view lifecycle trees correctly.

### Phase 2: Jetpack Compose Keyboard Layouts
1. **Design Key Caps:** Create circular/rounded responsive key Composables with hover highlights and touch targets satisfying high contrast targets.
2. **Handle Special Modes:** Implement dynamic QWERTY-to-Symbols sheets switching, Shift states tracking, and Caps Lock locking.
3. **Optimized Kinematics:** Render soft visual trails or popping previews above pressed key caps to provide interactive validation.

### Phase 3: Text Editing Bridge
1. **InputConnection Wrapper:** Connect keystroke triggers to the active input text buffer using standard ASCII and Unicode maps.
2. **Keyboard Options Alignment:** Parse XML layout settings (`KeyboardOptions` / `ImeAction`) to adapt key modifiers and action structures.

### Phase 4: AI Co-Pilot & Clipboard Integration
1. **Toolbar Panel:** Add an expandable utility ribbon on top of regular keys displaying Clipboard, AI, and Shortcuts options.
2. **Gemini SDK Connector:** Connect the AI utility button to the Generative AI client-side or server API. Integrate background Coroutine tasks to fetch responses without freezes.
3. **Direct SQLite Clipboard Hook:** Query the local clipboard manager table to display dynamic, swipe-to-delete copied snips directly in the keyboard utility strip.

---

## Any Questions or Suggestions?

Before moving from planning to active file compilation, I would welcome your thoughts on the following core considerations:

1. **AI Processing Location (Cloud vs. Local):**
   * *Suggestion:* Use the existing server-side Gemini Proxy or client-side SDK with standard model identifiers (such as `gemini-2.5-flash`) for real-time grammar checks and rewrites. Would you like us to implement local caching for these AI rewrites to save api tokens?
2. **Data-Store & Preferences Synchronization:**
   * *Suggestion:* Bind the keyboard style directly to `AppPreferences` and the clipboard database. Since the keyboard runs as a separate service process, we will utilize highly-safe database transactional writes and shared settings. Is there a specific keyboard layout (e.g., German QWERTZ, French AZERTY) you'd like us to support other than English US QWERTY?
3. **Keyboard Launch Guide UI:**
   * *Suggestion:* Inside our settings dashboard in standard `MainActivity.kt`, we can add a visual step-by-step assistant guiding device users on how to enable "Fraise Keyboard" inside standard Android system settings and select it as their active system keyboard. Should we develop this launch guide modal inside `SettingsScreen.kt`?
