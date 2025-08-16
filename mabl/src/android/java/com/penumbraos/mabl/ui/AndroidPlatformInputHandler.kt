package com.penumbraos.mabl.ui

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import androidx.lifecycle.LifecycleCoroutineScope
import com.penumbraos.mabl.interaction.IInteractionFlowManager
import com.penumbraos.mabl.ui.interfaces.IPlatformInputHandler

private const val TAG = "AndroidPlatformInputHandler"

class AndroidPlatformInputHandler : IPlatformInputHandler {
    
    override fun setup(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        interactionFlowManager: IInteractionFlowManager
    ) {}

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "Android key event received: keyCode=$keyCode")
        return false
    }
}