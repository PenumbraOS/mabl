package com.penumbraos.mabl.ui.interfaces

interface InputHandler {
    fun onVoiceInput(callback: (String) -> Unit)
    fun onTextInput(callback: (String) -> Unit)
    fun startListening()
    fun stopListening()
}