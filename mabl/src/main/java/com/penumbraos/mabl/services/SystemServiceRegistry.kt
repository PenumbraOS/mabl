package com.penumbraos.mabl.services

import com.penumbraos.mabl.sdk.ISystemServiceRegistry
import com.penumbraos.mabl.sdk.ITtsService

class SystemServiceRegistry(
    private val allControllers: AllControllers
) : ISystemServiceRegistry.Stub() {
    
    override fun getTtsService(): ITtsService? {
        return allControllers.tts.service
    }
}