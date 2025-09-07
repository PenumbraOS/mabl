package com.penumbraos.mabl.ui.interfaces

import com.penumbraos.mabl.types.Error

interface IConversationRenderer {
    fun showMessage(message: String, isUser: Boolean)
    fun showTranscription(text: String)
    fun showListening(isListening: Boolean)
    fun showError(error: Error)
}