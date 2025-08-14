package com.penumbraos.mabl.interaction

import com.penumbraos.mabl.conversation.ConversationManager

interface IInteractionFlowManager {
    fun startListening()
    fun startConversationFromInput(userInput: String)
    fun cancelCurrentFlow()
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
    fun onProcessingStarted()
    fun onProcessingStopped()
    fun onSpeakingStarted()
    fun onSpeakingStopped()
    fun onFlowCancelled()
    fun onError(error: String)
}

interface InteractionContentCallback {
    fun onPartialTranscription(text: String)
    fun onFinalTranscription(text: String)
    fun onPartialResponse(token: String)
    fun onFinalResponse(response: String)
}