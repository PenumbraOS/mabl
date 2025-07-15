package com.penumbraos.mabl.services

import android.content.Context
import android.util.Log
import com.penumbraos.mabl.discovery.PluginManager
import com.penumbraos.mabl.discovery.PluginService
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.IToolService
import com.penumbraos.mabl.sdk.PluginType
import com.penumbraos.mabl.sdk.ToolCall
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ToolOrchestrator"

class ToolOrchestrator(private val context: Context) {
    private val pluginManager = PluginManager(context)
    private val serviceControllers = ConcurrentHashMap<String, ToolController>()
    private val serviceInfoMap = ConcurrentHashMap<String, PluginService>()
    private val toolToServiceMap = ConcurrentHashMap<String, IToolService>()
    private var connectedServicesCount = 0
    private var allConnected = kotlinx.coroutines.CompletableDeferred<Unit>()

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
        val serviceInfo = serviceInfoMap[packageName]

        // Map each tool to this service
        serviceInfo?.tools?.forEach { toolName ->
            if (controller?.service != null) {
                toolToServiceMap[toolName.trim()] = controller.service!!
            }
        }

        connectedServicesCount++
        if (connectedServicesCount >= serviceControllers.size) {
            allConnected.complete(Unit)
        }
    }

    fun getAvailableToolDefinitions(): Array<com.penumbraos.mabl.sdk.ToolDefinition> {
        val allDefinitions = mutableListOf<com.penumbraos.mabl.sdk.ToolDefinition>()

        for ((_, controller) in serviceControllers) {
            controller.service?.let { service ->
                try {
                    val definitions = service.toolDefinitions
                    allDefinitions.addAll(definitions)
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting tool definitions from service", e)
                }
            }
        }

        return allDefinitions.toTypedArray()
    }

    fun executeTool(toolCall: ToolCall, callback: IToolCallback) {
        Log.d(TAG, "Executing tool: ${toolCall.name}")

        val service = toolToServiceMap[toolCall.name]
        if (service != null) {
            service.executeTool(toolCall, callback)
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