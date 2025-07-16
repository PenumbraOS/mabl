package com.penumbraos.mabl.discovery

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.penumbraos.mabl.sdk.PluginConstants
import com.penumbraos.mabl.sdk.PluginType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class PluginService(
    val packageName: String,
    val className: String,
    val type: PluginType,
    val displayName: String?,
    val description: String?
)

class PluginManager(private val context: Context) {
    fun discoverPlugins(): List<PluginService> {
        return PluginType.entries.flatMap { discoverServiceType(it) }
    }
    
    fun discoverServices(type: PluginType): List<PluginService> {
        return discoverServiceType(type)
    }

    suspend fun <T> connectToService(
        pluginService: PluginService,
        serviceCast: (IBinder) -> T
    ): T? = suspendCoroutine { continuation ->
        val intent = Intent().apply {
            component = ComponentName(pluginService.packageName, pluginService.className)
        }
        
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (service != null) {
                    val castedService = serviceCast(service)
                    continuation.resume(castedService)
                } else {
                    continuation.resume(null)
                }
            }
            
            override fun onServiceDisconnected(name: ComponentName?) {
                // Handle disconnection if needed
            }
        }
        
        val bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        if (!bound) {
            continuation.resume(null)
        }
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
                description = metadata?.getString(PluginConstants.METADATA_DESCRIPTION)
            )
        }
    }
}