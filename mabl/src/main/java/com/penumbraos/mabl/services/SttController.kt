package com.penumbraos.mabl.services

import android.os.IBinder
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.ISttService
import com.penumbraos.mabl.sdk.PluginType

class SttController(onConnect: () -> Unit) :
    ServiceController<ISttService>(PluginType.STT, onConnect) {

    var delegate: ISttCallback? = null

    fun startListening() {
        if (service == null) {
            throw IllegalStateException("STT service not connected")
        } else if (delegate == null) {
            throw IllegalStateException("STT delegate not set")
        }
        service?.startListening(delegate)
    }

    fun stopListening() {
        if (service == null) {
            throw IllegalStateException("STT service not connected")
        }
        service?.stopListening()
    }

    override fun castService(service: IBinder): ISttService {
        return ISttService.Stub.asInterface(service)
    }
}