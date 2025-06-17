# MABL Implementation Plan

This document outlines a proposed implementation plan for MABL. This plan is designed to be incremental, with small, demonstrable steps.

### Guiding Principles

- **Simplicity and Flexibility:** Start with the simplest possible implementations and add complexity only when necessary. Interfaces should be flexible to accommodate future needs.
- **Interface-Driven Development:** Define clear interfaces (using Android's AIDL for inter-process communication) between MABL and its plugins first. This allows for parallel development and easy mocking.
- **Incremental Builds:** Each phase builds upon the previous one, resulting in a demonstrable feature.

---

### High-Level Architecture

Here's a Mermaid diagram illustrating the high-level architecture described in the `README.md`:

```mermaid
graph TD
    subgraph User Input
        A[Touchpad/Voice] --> B{MABL Core};
    end

    subgraph MABL Core
        B -- Audio Stream --> C[Active STT Plugin];
        C -- Transcribed Text --> B;
        B -- Text --> D[Active LLM Plugin];
        D -- Tool Call/Response --> B;
        B -- Tool Call --> E[App Plugin e.g., Timer];
        E -- Result/UI Request --> B;
        B -- Text to Speak --> F[Active TTS Plugin];
        F -- Synthesized Audio --> B;
        B -- Render UI --> G[MABL UI];
    end

    subgraph Plugins (Separate APKs/Processes)
        C; D; E; F;
    end

    style MABL Core fill:#f9f,stroke:#333,stroke-width:2px
```

---

### Incremental Implementation Plan

#### Phase 1: Core Application and Plugin Discovery

- **Goal:** Establish the foundational MABL application and its ability to discover plugin **services** using Android's `Intent` filter system.
- **APIs/Components:**
  - **MABL Core App:** A basic Android application set as the launcher.
  - **Plugin Discovery (MABL-side):** MABL will use `PackageManager.queryIntentServices()` to find services that can handle specific actions (e.g., `com.penumbraos.mabl.sdk.ACTION_STT_SERVICE`). This replaces the need to scan for applications with a special metadata tag.
  - **Capability Declaration (Plugin-side):**
    - Each service a plugin offers (STT, TTS, LLM, Tool) will be declared as a `<service>` in the `AndroidManifest.xml`.
    - Each `<service>` tag will contain an `<intent-filter>` specifying the action it handles.
    - Specific capabilities, like the name of a tool, can be declared via `<meta-data>` tags directly within the `<service>` block.
  - **Capability Parsing (MABL-side):**
    - MABL's `PluginManager` will query the `PackageManager` for each service type.
    - It will parse the `ResolveInfo` object for each discovered service to get its component name and read any associated `<meta-data>`. This provides a structured model of the plugin's offerings.
- **SDK (`PluginConstants.kt`):**

  - This file is central to the discovery process. It will define the standard intent actions and metadata keys.
  - **Example Constants:**

    ```kotlin
    // Intent Actions for Service Discovery
    const val ACTION_STT_SERVICE = "com.penumbraos.mabl.sdk.action.STT_SERVICE"
    const val ACTION_TTS_SERVICE = "com.penumbraos.mabl.sdk.action.TTS_SERVICE"
    const val ACTION_LLM_SERVICE = "com.penumbraos.mabl.sdk.action.LLM_SERVICE"
    const val ACTION_TOOL_SERVICE = "com.penumbraos.mabl.sdk.action.TOOL_SERVICE"

    // Metadata Keys for Capability Declaration
    const val METADATA_TOOLS = "com.penumbraos.mabl.sdk.metadata.TOOLS" // e.g. "create_timer,get_weather"
    ```

- **Directory Structure:**

  ```
  mabl/                # Main MABL application
  ├── src/main/
  │   ├── java/com/penumbraos/mabl/
  │   │   ├── MablApplication.kt
  │   │   ├── MainActivity.kt
  │   │   └── discovery/
  │   │       └── PluginManager.kt
  └── build.gradle

  sdk/                # Shared SDK for plugins
  ├── src/main/
  │   └── java/com/penumbraos/mabl/sdk/
  │       └── PluginConstants.kt # Intent actions, metadata keys
  └── build.gradle
  ```

- **Demo:** Launch the MABL app. It will query the `PackageManager` and display a list of "discovered" mock plugins. No actual communication yet.

#### Phase 2: Basic STT/TTS Communication

- **Goal:** Enable one-way communication from a mock STT plugin to MABL, and from MABL to a mock TTS plugin.
- **APIs/Components:**
  - **AIDL Interfaces:**
    - `ISttService.aidl`: Defines a method like `startListening(ISttCallback callback)` and `stopListening()`.
    - `ISttCallback.aidl`: Defines a callback like `onTranscription(String text)`.
    - `ITtsService.aidl`: Defines a method like `speak(String text, ITtsCallback callback)`.
    - `ITtsCallback.aidl`: Defines callbacks like `onSpeechStarted()` and `onSpeechFinished()`.
  - **Mock Plugins:**
    - **STT Demo Plugin:** A plugin with a service implementing `ISttService`. `startListening` will immediately return "hello world" via the callback.
    - **TTS Demo Plugin:** A plugin with a service implementing `ITtsService`. `speak` will log the received text.
- **Directory Structure (New):**
  ```
  plugins/
  ├── stt-demo/
  │   ├── src/main/
  │   │   ├── java/com/demo/stt/
  │   │   │   └── SttService.kt
  │   │   └── aidl/com/penumbraos/mabl/sdk/ # Copied or referenced AIDL
  │   └── AndroidManifest.xml
  └── tts-demo/
      ├── src/main/
      │   ├── java/com/demo/tts/
      │   │   └── TtsService.kt
      │   └── aidl/com/penumbraos/mabl/sdk/
      └── AndroidManifest.xml
  ```
- **Demo:** A button in MABL triggers the STT service. The text "hello world" appears in MABL's UI. Another button sends this text to the TTS service, and a log message confirms it was received.

#### Phase 3: LLM Plugin Integration

- **Goal:** Introduce an LLM plugin that can receive text and return a structured response.
- **APIs/Components:**
  - **AIDL Interface:**
    - `ILlmService.aidl`: Defines `generateResponse(String prompt, ILlmCallback callback)`.
    - `ILlmCallback.aidl`: Defines `onResponse(LlmResponse response)`.
  - **Data Structure:**
    - `LlmResponse.java` (or `.kt`): A `Parcelable` class that can contain plain text and/or a list of tool calls.
  - **LLM Demo Plugin:** Implements `ILlmService`. When it receives text, it returns a hardcoded `LlmResponse` (e.g., if it sees "timer", it returns a tool call for a timer).
- **Directory Structure (New):**
  ```
  plugins/
  └── llm-demo/
      └── # ... similar to other plugins
  ```
- **Demo:** The text from the STT demo is now forwarded to the LLM demo. MABL's UI displays the structured response received from the LLM (e.g., "TOOL_CALL: create_timer, parameters: { duration: '25 minutes' }").

#### Phase 4: Tool Call Handling & App Plugin

- **Goal:** MABL can now parse tool calls from the LLM and route them to the appropriate plugin.
- **APIs/Components:**
  - **AIDL Interface:**
    - `IToolService.aidl`: Defines `executeTool(ToolCall call, IToolCallback callback)`.
    - `IToolCallback.aidl`: Defines `onToolResult(ToolResult result)`.
  - **Data Structures:** `ToolCall` and `ToolResult` `Parcelable`s.
  - **MABL Core Logic:** A new component in MABL, the `Orchestrator`, which takes the `LlmResponse`, checks for tool calls, and uses the `PluginManager` to find a plugin that can handle the tool.
  - **Timer App Plugin:** A plugin that registers to handle the `create_timer` tool.
- **Directory Structure (New):**
  ```
  plugins/
  └── timer-app/
      └── # ... similar to other plugins
  ```
- **Demo:**
  1.  User triggers STT: "Set a timer for 25 minutes".
  2.  STT -> "Set a timer for 25 minutes".
  3.  LLM -> `TOOL_CALL: create_timer...`
  4.  MABL's `Orchestrator` sees the tool call, finds the `timer-app` plugin, and calls `executeTool`.
  5.  The `timer-app` logs that it created the timer.
  6.  The result is sent back to MABL and displayed in the UI.

#### Phase 5: Dynamic UI Rendering

- **Goal:** Allow a plugin to request that MABL render a piece of UI.
- **APIs/Components:**
  - **AIDL Interface:**
    - An addition to `IToolCallback` or a new `IUIManager.aidl` service in MABL that plugins can call. Let's go with a new service for separation of concerns: `requestUi(UiRequest request)`.
  - **UI Loading:** MABL will use `DexClassLoader` to load a view from the plugin's APK. This is complex and needs careful security considerations (sandboxing, resource loading).
  - **UI Request:** The `timer-app` will, after creating a timer, call `requestUi` with information about the UI to show (e.g., the name of the `View` class in its APK and data to populate it).
- **Demo:** After setting a timer, a simple UI showing the timer countdown appears in the MABL app. This UI is defined in the `timer-app`'s APK but rendered by MABL.

#### Phase 6: Advanced Features &amp; Refinement

- **Goal:** Implement state management, settings, and improve robustness.
- **APIs/Components:**
  - **Settings Provider:** A new plugin type or a core MABL feature to manage which plugins are active.
  - **State Management:** A mechanism for MABL to persist its state (e.g., active plugins, action log).
  - **Error Handling:** Improve error handling for plugin crashes, especially during UI rendering.
- **Demo:** Show a settings screen in MABL that allows the user to switch between different STT/TTS/LLM plugins. Demonstrate that the setting persists across app restarts.
