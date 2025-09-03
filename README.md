# MABL launcher for PenumbraOS

This is primary user entrypoint app for [PenumbraOS](https://github.com/PenumbraOS/), the full development platform for the late Humane Ai Pin.

> [!CAUTION]
> This is extremely experimental and currently is usable by developers only.

MABL is a central orchestrator app, running as a normal unprivileged `untrusted` app, which exposes a plugin archecture built around the Android SDK ecosystem to enable users to install different implementations of LLMs (local, remote, realtime audio), TTS and STT, separate apps (like a timer or notes app), etc.

## Features

- Hold a single finger to talk to LLM
- Hold two fingers to talk to LLM with a picture
- Conversation persistence (ask "what did you just say" or similar)
- Rough laser ink projected display
  - Date and time home screen
  - Conversation display
  - Navigation and menuing
- Dynamic tool calling
  - Cosign similiarity between tool description embedding and user query selects the top `n` (currently 6) tools
  - Only these tools, plus any "persistent" ones, are passed to the LLM
  - Optimized for faster processing and local LLMs
- Plugable tool providers. Existing tool providers are:
  - OpenAI compatible LLM (provided API keys)
  - Humane speech to text
  - Generic Android text to speech
  - Google search (provided API keys)
  - Generic Android system operations (volume, more to come)
  - Ai Pin specific operations (timers, more to come)

---

## Architecture

### Overview

- MABL is installed as a single app, configured as the Android launcher. Its responsibilities are:
  - Rendering the main UI and any custom UI supplied by plugins
  - Discover and manage plugins (potentially including installation through a separate `installd` project)
  - Provides the core logic for choosing plugins to execute, e.g. choosing which TTS provider is going to be routed to and taking an LLM response with tool calls and actually calling the plugin code that corresponds to that call
- Plugin APKs are APKs that contain a plugin's code, but does not contain an app itself and is not launched by the Android ecosystem
  - Registers as a provider of a number of core features (LLM, TTS, STT, initial prompt building, etc.) and can register tool calls for custom functionality
  - Any code in this process can request rendering UI. This UI will be dynamically added to the classpath of MABL and rendered from within the main app. This guarantees MABL is in control of the display at all times

### Discovery

Plugin discovery uses the normal SDK provisions of `PackageManager`. Plugins register for pre-defined action names, such as `com.penumbraos.intent.action.PROVIDE_STT` (this name may change), which are detected at install/uninstall.

**Example:**

```xml
<!-- In the plugin's AndroidManifest.xml -->
<service android:name=".MySttService" android:exported="true">
    <intent-filter>
        <action android:name="com.penumbraos.intent.action.PROVIDE_STT" />
    </intent-filter>
    <meta-data
        android:name="com.penumbraos.metadata.DISPLAY_NAME"
        android:value="My Custom STT" />
</service>
```

### Communication

#### Logic

Logic runs in the plugin's own process, like a normal Android app. Each plugin binds its own variant of the provider services it implements. `untrusted_app` should be able to register services usable by other `untrusted_app` based on the SELinux rules. Each plugin maintains its own access to the [PenumbraOS SDK](https://github.com/penumbraOS/sdk), and thus can access the network and priviledged operations like changing settings.

#### Rendering

All UI runs within the MABL process. The plugin logic can request UI display at any point using APIs exposed by MABL. MABL ultimately decides whether that UI is allowed to be displayed. If the rendering is allowed, MABL reaches into the plugin APK and grabs the embedded UI mode DEX (implementation details to be determined), loading it via `DexClassLoader`. Care must be taken to prevent crashing of the entire MABL system in the result of a UI exception.

### State

- MABL maintains core system properties
  - Currently active providers
  - Action log
  - May expose shared settings API to plugins (not sure if this is good/necessary)
- Plugins store their own local state
  - Example: Timer plugin storing the active timers. Notes provider storing notes

### Example flow

1. **Input**: User taps touchpad/otherwise causes a speech activation event. MABL routes the audio to the active STT provider service
2. **Transcription**: The STT service transcribes the audio and uses an AIDL callback to return the text "Set a timer for 25 minutes" to MABL
3. **Orchestration**: MABL receives the text and forwards it to the active LLM provider service
4. **Generation**: The LLM service processes the text and returns the answer "[TOOL_CALL] timer 25 minutes" to MABL
5. **Tool call**: MABL forwards the tool call to the timer plugin's tool call provider service
6. **Tool action**: Timer is created. Timer plugin sends success status and success text to MABL, and requests rendering of custom UI that depicts the newly created timer
7. **Output**: MABL receives the response and sends it to the active TTS provider service. If the screen turns on within a certain time period (10s?), MABL loads the UI code from the timer plugin and renders it with the configured data
