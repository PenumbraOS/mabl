package com.penumbraos.mabl.ui.interfaces

interface IPlatformCapabilities {
    /**
     * Get platform-specific view model if available. Returns null for platforms without view models.
     */
    fun getViewModel(): Any? {
        return null
    }
}