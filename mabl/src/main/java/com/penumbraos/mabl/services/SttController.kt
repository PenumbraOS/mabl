package com.penumbraos.mabl.services

import android.os.IBinder
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.ISttService
import com.penumbraos.mabl.sdk.PluginType

class SttController(onConnect: () -> Unit) :
    ServiceController<ISttService>(PluginType.STT, onConnect) {

    var delegate: ISttCallback? = null
    private var isListening = false

    var startTime: Long? = null
        private set
    var endTime: Long? = null
        private set

    fun startListening() {
        if (service == null) {
            throw IllegalStateException("STT service not connected")
        } else if (delegate == null) {
            throw IllegalStateException("STT delegate not set")
        }
        service?.startListening(delegate)
        startTime = System.currentTimeMillis()
        endTime = null
        isListening = true
    }

    fun stopListening() {
        if (service == null) {
            throw IllegalStateException("STT service not connected")
        }
        service?.stopListening()
        endTime = System.currentTimeMillis()
        isListening = false
    }

    fun cancelListening() {
        if (isListening) {
            stopListening()
        }
    }

    fun isCurrentlyListening(): Boolean = isListening

    fun lastListenDuration(): Long? {
        if (startTime == null || endTime == null) {
            return null
        }
        return endTime!! - startTime!!
    }

    override fun castService(service: IBinder): ISttService {
        return ISttService.Stub.asInterface(service)
    }
}