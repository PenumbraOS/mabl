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
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ToolOrchestrator"

class ToolOrchestrator(
    private val context: Context,
    allControllers: AllControllers
) {
    private val pluginManager = PluginManager(context)
    private val serviceControllers = ConcurrentHashMap<String, ToolController>()
    private val serviceInfoMap = ConcurrentHashMap<String, PluginService>()
    private val toolToServiceMap = ConcurrentHashMap<String, IToolService>()
    private var connectedServicesCount = 0
    private var allConnected = kotlinx.coroutines.CompletableDeferred<Unit>()
    private val systemServiceRegistry = SystemServiceRegistry(allControllers)

    fun initialize() {
        allConnected = kotlinx.coroutines.CompletableDeferred<Unit>()
        connectedServicesCount = 0

        // Discover all tool services at startup
        val toolServices = pluginManager.discoverServices(PluginType.TOOL)

        for (serviceInfo in toolServices) {
            val controller = ToolController {
                onToolServiceConnected(serviceInfo.packageName)
            }
            serviceControllers[serviceInfo.packageName] = controller
            serviceInfoMap[serviceInfo.packageName] = serviceInfo
        }
    }

    suspend fun connectAll() {
        if (serviceControllers.isEmpty()) {
            allConnected.complete(Unit)
            return
        }

        for ((packageName, controller) in serviceControllers) {
            controller.connect(context, packageName)
        }

        allConnected.await()
    }

    private fun onToolServiceConnected(packageName: String) {
        val controller = serviceControllers[packageName]
        controller?.service?.setSystemServices(systemServiceRegistry)

        connectedServicesCount++
        if (connectedServicesCount >= serviceControllers.size) {
            allConnected.complete(Unit)
        }
    }

    fun getAvailableToolDefinitions(): Array<ToolDefinition> {
        val allDefinitions = mutableListOf<ToolDefinition>()

        // Reset existing tool mappings
        toolToServiceMap.clear()

        for ((packageName, controller) in serviceControllers) {
            controller.service?.let { service ->
                try {
                    val definitions = service.toolDefinitions
                    allDefinitions.addAll(definitions)

                    definitions.forEach { toolDef ->
                        toolToServiceMap[toolDef.name] = service
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting tool definitions from $packageName", e)
                }
            }
        }

        Log.d(TAG, "Available tools: ${allDefinitions.map { it.name }}")

        return allDefinitions.toTypedArray()
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
    }
}