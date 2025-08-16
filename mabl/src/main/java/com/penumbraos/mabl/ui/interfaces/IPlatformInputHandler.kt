package com.penumbraos.mabl.ui.interfaces

import android.content.Context
import android.view.KeyEvent
import androidx.lifecycle.LifecycleCoroutineScope
import com.penumbraos.mabl.interaction.IInteractionFlowManager

/**
 * Platform-specific input handling.
 */
interface IPlatformInputHandler {
    fun setup(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        interactionFlowManager: IInteractionFlowManager
    )

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean
}