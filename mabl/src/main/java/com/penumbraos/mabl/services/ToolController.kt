package com.penumbraos.mabl.services

import android.os.IBinder
import com.penumbraos.mabl.sdk.IToolService
import com.penumbraos.mabl.sdk.PluginType

class ToolController(onConnect: () -> Unit) : ServiceController<IToolService>(
    PluginType.TOOL,
    onConnect
) {
    override fun castService(service: IBinder): IToolService {
        return IToolService.Stub.asInterface(service)
    }
}