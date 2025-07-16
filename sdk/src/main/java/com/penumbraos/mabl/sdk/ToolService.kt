package com.penumbraos.mabl.sdk

import android.content.Intent
import android.os.IBinder
import org.json.JSONObject

abstract class ToolService(name: String) : MablService(name) {
    private var systemServices: ISystemServiceRegistry? = null

    private val binder = object : IToolService.Stub() {
        override fun executeTool(
            call: ToolCall,
            callback: IToolCallback
        ) {
            val params = if (call.parameters != "") {
                JSONObject(call.parameters)
            } else {
                null
            }

            executeTool(call, params, callback)
        }

        override fun getToolDefinitions(): Array<out ToolDefinition?>? {
            return this@ToolService.getToolDefinitions()
        }

        override fun setSystemServices(systemServices: ISystemServiceRegistry?) {
            this@ToolService.systemServices = systemServices
        }
    }

    override fun onBind(intent: Intent?): IBinder? = binder

    abstract fun executeTool(call: ToolCall, params: JSONObject?, callback: IToolCallback)
    abstract fun getToolDefinitions(): Array<ToolDefinition>
}