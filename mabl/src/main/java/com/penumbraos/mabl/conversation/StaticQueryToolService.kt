package com.penumbraos.mabl.conversation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.sdk.ToolService
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.sdk.PenumbraClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val NEW_CONVERSATION = "new_conversation"
private const val OPEN_SETTINGS = "open_settings"
private const val REBOOT_NOW = "reboot_now"

class StaticQueryToolService(
    private val allControllers: AllControllers,
    private val context: Context,
    val coroutineScope: CoroutineScope
) : ToolService("StaticQueryToolService") {
    // TODO: This should work on non-Pin
    private val client = PenumbraClient(context)

    override fun executeTool(
        call: ToolCall,
        params: JSONObject?,
        callback: IToolCallback
    ) {
        when (call.name) {
            NEW_CONVERSATION -> {
                coroutineScope.launch {
                    allControllers.conversationManager.startNewConversation()

                    callback.onSuccess("Created new conversation")
                }
            }

            OPEN_SETTINGS -> {
                coroutineScope.launch {
                    // TODO: This should work on non-Pin
                    val intent = Intent().apply {
                        component = ComponentName(
                            "humane.experience.settings",
                            "humane.experience.settings.SettingsExperience"
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }

                    try {
                        context.startActivity(intent)
                        callback.onSuccess("Opened settings")
                    } catch (e: Exception) {
                        Log.e("Settings", "Failed to start settings", e)
                        callback.onSuccess("Failed to open settings")
                    }
                }
            }

            REBOOT_NOW -> {
                coroutineScope.launch {
                    try {
                        client.shell.executeCommand("reboot")

                        callback.onSuccess("Rebooting")
                    } catch (e: Exception) {
                        callback.onError("Failed to reboot: ${e.message}")
                    }
                }
            }

            else -> {
                callback.onError("Unknown tool: ${call.name}")
            }
        }
    }

    override fun getToolDefinitions(): Array<ToolDefinition> {
        return arrayOf(
            ToolDefinition().apply {
                name = NEW_CONVERSATION
                examples = arrayOf(
                    "new conversation",
                    "new chat"
                )
            },
            ToolDefinition().apply {
                name = OPEN_SETTINGS
                examples = arrayOf(
                    "open settings",
                    "open system settings",
                    "open human settings",
                    "launch settings"
                )
            },
            ToolDefinition().apply {
                name = REBOOT_NOW
                examples = arrayOf(
                    "reboot now",
                    "emergency reboot"
                )
            }
        )
    }
}
