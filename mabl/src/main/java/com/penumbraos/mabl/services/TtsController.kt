package com.penumbraos.mabl.services

import android.os.IBinder
import com.penumbraos.mabl.sdk.ITtsCallback
import com.penumbraos.mabl.sdk.ITtsService
import com.penumbraos.mabl.sdk.PluginType

class TtsController(onConnect: () -> Unit) :
    ServiceController<ITtsService>(PluginType.TTS, onConnect) {

    var delegate: ITtsCallback? = null
        set(delegate) {
            if (delegate != null) {
                service?.registerCallback(delegate)
            }
        }

    override fun onServiceConnected(service: ITtsService) {
        if (delegate != null) {
            service.registerCallback(delegate)
        }
        super.onServiceConnected(service)
    }

    override fun castService(service: IBinder): ITtsService {
        return ITtsService.Stub.asInterface(service)
    }
}