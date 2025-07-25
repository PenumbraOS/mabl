package com.penumbraos.mabl.ui.interfaces

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.sdk.ISttService

interface IInputHandler {
    fun setup(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        sttService: ISttService?,
        sttCallback: ISttCallback,
        conversationRenderer: IConversationRenderer
    )

    fun onVoiceInput(callback: (String) -> Unit)
    fun onTextInput(callback: (String) -> Unit)
    fun startListening()
    fun stopListening()
}