package com.penumbraos.mabl.discovery

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.penumbraos.mabl.sdk.PluginConstants
import com.penumbraos.mabl.sdk.PluginType

data class PluginService(
    val packageName: String,
    val className: String,
    val type: PluginType,
    val displayName: String?,
    val description: String?,
    val tools: List<String>?
)

class PluginManager(private val context: Context) {
    fun discoverPlugins(): List<PluginService> {
        return PluginType.entries.flatMap { discoverServiceType(it) }
    }

    private fun discoverServiceType(
        type: PluginType
    ): List<PluginService> {
        val pm = context.packageManager
        val intent = Intent(type.action)
        val services = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)

        return services.mapNotNull { resolveInfo ->
            val serviceInfo = resolveInfo.serviceInfo
            val metadata = serviceInfo.metaData

            PluginService(
                packageName = serviceInfo.packageName,
                className = serviceInfo.name,
                type = type,
                displayName = metadata?.getString(PluginConstants.METADATA_DISPLAY_NAME),
                description = metadata?.getString(PluginConstants.METADATA_DESCRIPTION),
                tools = metadata?.getString(PluginConstants.METADATA_TOOLS)?.split(",")
            )
        }
    }
}