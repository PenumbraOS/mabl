package com.penumbraos.mabl.conversation

import android.content.Context
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.services.AllControllers
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class StaticQueryManager(
    allControllers: AllControllers,
    context: Context,
    coroutineScope: CoroutineScope
) {
    private val toolService = StaticQueryToolService(allControllers, context, coroutineScope)

    private var toolMap: Map<String, ToolDefinition>? = null

    suspend fun evaluateStaticQuery(query: String): String? {
        buildQueryMap()

        val query = query.lowercase().trim()

        val tool = toolMap!![query]
        if (tool != null) {
            try {
                return suspendCoroutine { continuation ->
                    toolService.executeTool(ToolCall().apply {
                        name = tool.name
                    }, null, object : IToolCallback.Stub() {
                        override fun onSuccess(result: String?) {
                            continuation.resume(result)
                        }

                        override fun onError(error: String?) {
                            continuation.resumeWithException(Error(error))
                        }
                    })
                }
            } catch (e: Exception) {
                return null
            }
        }

        return null
    }

    private fun buildQueryMap() {
        if (toolMap != null) {
            return
        }

        val tools = toolService.getToolDefinitions()
        val map = mutableMapOf<String, ToolDefinition>()
        for (tool in tools) {
            for (staticQuery in tool.examples ?: emptyArray()) {
                map[staticQuery] = tool
            }
        }
        toolMap = map
    }
}