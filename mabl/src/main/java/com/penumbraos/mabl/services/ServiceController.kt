package com.penumbraos.mabl.services

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.penumbraos.mabl.sdk.PluginType

abstract class ServiceController<T>(
    private val pluginType: PluginType,
    private val onConnect: () -> Unit
) {
    internal var service: T? = null

    val isConnected: Boolean
        get() = service != null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d("MainActivity", "onServiceConnected: ${pluginType.name}")
            if (binder == null) {
                Log.e("MainActivity", "Service binder for ${pluginType.name} is null")
                return
            }
            service = castService(binder)
            onServiceConnected(service!!)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            onServiceDisconnected()
        }
    }

    abstract fun castService(service: IBinder): T

    open fun onServiceConnected(service: T) {
        onConnect()
    }

    open fun onServiceDisconnected() {

    }

    fun connect(context: Context, packageName: String, className: String? = null) {
        val intent = Intent(pluginType.action).apply {
            if (className != null) {
                setClassName(packageName, className)
            } else {
                setPackage(packageName)
            }
        }

        // Force services to be active using foreground service
        context.startForegroundService(intent)

        if (!context.bindService(
                intent,
                connection,
                BIND_AUTO_CREATE
            )
        ) {
            Log.e("MainActivity", "Could not set up binding for ${pluginType.name} service")
        }
    }

    fun shutdown(context: Context) {
        if (service != null) {
            context.unbindService(connection)
        }
    }
}