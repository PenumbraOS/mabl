package com.penumbraos.mabl.interaction

import com.penumbraos.mabl.conversation.ConversationManager
import com.penumbraos.mabl.types.Error

interface IInteractionFlowManager {
    fun startListening(requestImage: Boolean = false)
    fun startConversationFromInput(userInput: String)
    fun finishListening()
    fun isFlowActive(): Boolean
    fun getCurrentFlowState(): InteractionFlowState

    fun setConversationManager(conversationManager: ConversationManager?)
    fun setStateCallback(callback: InteractionStateCallback?)
    fun setContentCallback(callback: InteractionContentCallback?)
}

enum class InteractionFlowState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    CANCELLING
}

interface InteractionStateCallback {
    fun onListeningStarted()
    fun onListeningStopped()
    fun onUserFinished()
    fun onProcessingStarted()
    fun onProcessingStopped()
    fun onSpeakingStarted()
    fun onSpeakingStopped()
    fun onError(error: Error)
}

interface InteractionContentCallback {
    fun onPartialTranscription(text: String)
    fun onFinalTranscription(text: String)
    fun onPartialResponse(token: String)
    fun onFinalResponse(response: String)
}