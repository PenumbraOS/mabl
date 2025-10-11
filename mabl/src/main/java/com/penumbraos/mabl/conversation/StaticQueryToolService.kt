package com.penumbraos.mabl.conversation

import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.sdk.ToolService
import com.penumbraos.mabl.services.AllControllers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class StaticQueryToolService(
    private val allControllers: AllControllers,
    val coroutineScope: CoroutineScope
) : ToolService("StaticQueryToolService") {
    override fun executeTool(
        call: ToolCall,
        params: JSONObject?,
        callback: IToolCallback
    ) {
        when (call.name) {
            "new_conversation" -> {
                coroutineScope.launch {
                    allControllers.conversationManager.startNewConversation()

                    callback.onSuccess("Created new conversation")
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
                name = "new_conversation"
                description = "new conversation"
            }
        )
    }
}