package com.penumbraos.mabl.ui.interfaces

interface IConversationRenderer {
    fun showMessage(message: String, isUser: Boolean)
    fun showTranscription(text: String)
    fun showListening(isListening: Boolean)
    fun showError(error: String)
    fun clearConversation()
}