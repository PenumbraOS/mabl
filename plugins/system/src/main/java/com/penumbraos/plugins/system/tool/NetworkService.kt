package com.penumbraos.plugins.system.tool

import android.Manifest
import android.net.ConnectivityManager
import android.util.Log
import androidx.annotation.RequiresPermission
import com.penumbraos.mabl.sdk.IToolCallback
import com.penumbraos.mabl.sdk.ToolCall
import com.penumbraos.mabl.sdk.ToolDefinition
import com.penumbraos.mabl.sdk.ToolService
import org.json.JSONObject


private const val GET_IP = "get_ip"

private val IPv4_REGEX = """(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(/\d{1,2})?""".toRegex()

class NetworkService : ToolService("NetworkService") {
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun executeTool(
        call: ToolCall,
        params: JSONObject?,
        callback: IToolCallback
    ) {
        when (call.name) {
            GET_IP -> {
                val connectivityManager =
                    getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?

                if (connectivityManager == null) {
                    callback.onError("Failed to get network status")
                    return
                }

                val linkProperties =
                    connectivityManager.getLinkProperties(connectivityManager.activeNetwork)

                if (linkProperties == null) {
                    callback.onError("Failed to get network status")
                    return
                }

                Log.d(
                    "NetworkService",
                    "Link properties: ${linkProperties.linkAddresses.map { it.toString() }}"
                )

                val address =
                    linkProperties.linkAddresses.map {
                        val result = IPv4_REGEX.matchEntire(it.toString())
                        result?.groups[1]?.value
                    }.firstOrNull()

                if (address == null) {
                    callback.onError("Could not identify IP address")
                    return
                }

                callback.onSuccess("My IP address is $address")
            }
        }
    }

    override fun getToolDefinitions(): Array<ToolDefinition> {
        return arrayOf(
            ToolDefinition().apply {
                name = GET_IP
                description = "Get the IP address of the device"
                examples = arrayOf(
                    "what is your IP address",
                    "what is your address",
                    "IP address",
                    "internet address",
                    "what is the IP"
                )
            }
        )
    }
}