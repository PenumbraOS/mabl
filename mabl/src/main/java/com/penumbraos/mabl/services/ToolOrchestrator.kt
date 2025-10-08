package com.penumbraos.mabl.services

import android.content.Context
import android.util.Log
import com.penumbraos.mabl.discovery.PluginManager
import com.penumbraos.mabl.discovery.PluginService
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.IToolService
import com.penumbraos.mabl.sdk.PluginType
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ToolOrchestrator"

class ToolOrchestrator(
    private val context: Context,
    allControllers: AllControllers
) {
    val allTools = mutableListOf<ToolDefinition>()

    private val pluginManager = PluginManager(context)
    private val serviceControllers = ConcurrentHashMap<String, ToolController>()
    private val serviceInfoMap = ConcurrentHashMap<String, PluginService>()
    private val toolToServiceMap = ConcurrentHashMap<String, IToolService>()
    private var connectedServicesCount = 0
    private var allConnected = kotlinx.coroutines.CompletableDeferred<Unit>()
    private val systemServiceRegistry = SystemServiceRegistry(allControllers)
    private val toolSimilarityService = ToolSimilarityService()

    fun initialize() {
        allConnected = kotlinx.coroutines.CompletableDeferred<Unit>()
        connectedServicesCount = 0

        // Discover all tool services at startup
        val toolServices = pluginManager.discoverServices(PluginType.TOOL)

        for (serviceInfo in toolServices) {
            val serviceKey = "${serviceInfo.packageName}/${serviceInfo.className}"
            val controller = ToolController {
                onToolServiceConnected(serviceKey)
            }
            serviceControllers[serviceKey] = controller
            serviceInfoMap[serviceKey] = serviceInfo
        }
    }

    suspend fun connectAll() {
        if (serviceControllers.isEmpty()) {
            allConnected.complete(Unit)
            return
        }

        for ((serviceKey, controller) in serviceControllers) {
            val serviceInfo = serviceInfoMap[serviceKey] ?: continue
            controller.connect(context, serviceInfo.packageName, serviceInfo.className)
        }

        allConnected.await()

        try {
            val outputStream = ByteArrayOutputStream()
            context.assets.open("minilm-l6-v2-qint8-arm64.onnx").copyTo(outputStream)
            toolSimilarityService.initialize(outputStream.toByteArray())

            // Precalculate embeddings for all available tools
            buildToolDefinitionsMap()
            toolSimilarityService.precalculateToolEmbeddings(allTools)

            Log.d(
                TAG,
                "Tool similarity service initialized successfully with ${allTools.size} tool embeddings precalculated"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize similarity service: ${e.message}")
        }
    }

    private fun onToolServiceConnected(serviceKey: String) {
        val controller = serviceControllers[serviceKey]
        controller?.service?.setSystemServices(systemServiceRegistry)

        connectedServicesCount++
        if (connectedServicesCount >= serviceControllers.size) {
            allConnected.complete(Unit)
        }
    }

    fun buildToolDefinitionsMap() {
        allTools.clear()
        toolToServiceMap.clear()

        for ((serviceKey, controller) in serviceControllers) {
            controller.service?.let { service ->
                try {
                    val definitions = service.toolDefinitions
                    allTools.addAll(definitions)

                    definitions.forEach { toolDef ->
                        toolToServiceMap[toolDef.name] = service
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting tool definitions from $serviceKey", e)
                }
            }
        }

        Log.d(TAG, "Available tools: ${allTools.map { it.name }}")
    }

    suspend fun getFilteredToolDefinitions(
        userQuery: String,
        maxTools: Int = 6
    ): List<ToolDefinition> {
        return try {
            val filteredTools =
                toolSimilarityService.filterToolsByRelevance(allTools, userQuery, maxTools)
            val priorityTools = allTools.filter { it.isPriority && !filteredTools.contains(it) }
            filteredTools + priorityTools
        } catch (e: Exception) {
            Log.w(TAG, "Failed to filter tools by similarity, returning all: ${e.message}")
            allTools
        }
    }

    fun executeTool(toolCall: ToolCall, callback: IToolCallback) {
        Log.d(TAG, "Executing tool: ${toolCall.name}")

        val service = toolToServiceMap[toolCall.name]
        if (service != null) {
            try {
                service.executeTool(toolCall, callback)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing tool: ${toolCall.name}", e)
                callback.onError("Error executing tool: ${toolCall.name}")
            }
        } else {
            callback.onError("No service found for tool: ${toolCall.name}")
        }
    }

    fun shutdown() {
        serviceControllers.values.forEach { it.shutdown(context) }
        serviceControllers.clear()
        serviceInfoMap.clear()
        toolToServiceMap.clear()
        connectedServicesCount = 0
        toolSimilarityService.close()
    }
}