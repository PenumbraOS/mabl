package com.penumbraos.mabl.services

import android.os.IBinder
import com.penumbraos.mabl.sdk.ILlmService
import com.penumbraos.mabl.sdk.PluginType

class LlmController(onConnect: () -> Unit) : ServiceController<ILlmService>(
    PluginType.LLM,
    onConnect
) {
    override fun castService(service: IBinder): ILlmService {
        return ILlmService.Stub.asInterface(service)
    }
}