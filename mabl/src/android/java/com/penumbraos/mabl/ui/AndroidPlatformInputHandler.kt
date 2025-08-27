package com.penumbraos.mabl.ui

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.penumbraos.mabl.interaction.IInteractionFlowManager
import com.penumbraos.mabl.ui.interfaces.IPlatformInputHandler

private const val TAG = "AndroidPlatformInputHandler"

class AndroidPlatformInputHandler : IPlatformInputHandler {
    
    override fun setup(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        interactionFlowManager: IInteractionFlowManager
    ) {
    }
}