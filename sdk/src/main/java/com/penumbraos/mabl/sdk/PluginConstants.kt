package com.penumbraos.mabl.sdk

enum class PluginType(val action: String) {
    STT(PluginConstants.ACTION_STT_SERVICE),
    TTS(PluginConstants.ACTION_TTS_SERVICE),
    LLM(PluginConstants.ACTION_LLM_SERVICE),
    TOOL(PluginConstants.ACTION_TOOL_SERVICE);

    companion object {
        fun fromAction(action: String): PluginType? {
            return entries.firstOrNull { it.action == action }
        }
    }
}

object PluginConstants {
    // Intent Actions for Service Discovery
    const val ACTION_STT_SERVICE = "com.penumbraos.mabl.sdk.action.STT_SERVICE"
    const val ACTION_TTS_SERVICE = "com.penumbraos.mabl.sdk.action.TTS_SERVICE"
    const val ACTION_LLM_SERVICE = "com.penumbraos.mabl.sdk.action.LLM_SERVICE"
    const val ACTION_TOOL_SERVICE = "com.penumbraos.mabl.sdk.action.TOOL_SERVICE"

    // Metadata Keys for Capability Declaration
    const val METADATA_DISPLAY_NAME = "com.penumbraos.mabl.sdk.metadata.DISPLAY_NAME"
    const val METADATA_DESCRIPTION = "com.penumbraos.mabl.sdk.metadata.DESCRIPTION"
}