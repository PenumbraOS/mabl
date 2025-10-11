package com.penumbraos.mabl.conversation

import android.content.Context
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
private const val REBOOT_NOW = "reboot_now"

class StaticQueryToolService(
    private val allControllers: AllControllers,
    context: Context,
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
                description = "new conversation"
            },
            ToolDefinition().apply {
                name = REBOOT_NOW
                description = "reboot now,emergency reboot"
            }
        )
    }
}