package com.penumbraos.mabl.interaction

import android.util.Log
import com.penumbraos.mabl.conversation.ConversationManager
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.services.AllControllers

private const val TAG = "InteractionFlowManager"

class InteractionFlowManager(
    private val allControllers: AllControllers
) : IInteractionFlowManager {

    private var currentState = InteractionFlowState.IDLE
    private var conversationManager: ConversationManager? = null
    private var stateCallback: InteractionStateCallback? = null
    private var contentCallback: InteractionContentCallback? = null

    private val sttCallback = object : ISttCallback.Stub() {
        override fun onPartialTranscription(partialText: String) {
            Log.d(TAG, "STT partial transcription: $partialText")
            contentCallback?.onPartialTranscription(partialText)
        }

        override fun onFinalTranscription(finalText: String) {
            Log.d(TAG, "STT final transcription: $finalText")
            setState(InteractionFlowState.PROCESSING)
            contentCallback?.onFinalTranscription(finalText)

            // Start conversation with the transcribed text
            startConversationFromInput(finalText)
        }

        override fun onError(errorMessage: String) {
            Log.e(TAG, "STT Error: $errorMessage")
            setState(InteractionFlowState.IDLE)
            stateCallback?.onError("Speech recognition error: $errorMessage")
        }
    }

    init {
        // Set the STT callback on the controller
        allControllers.stt.delegate = sttCallback
    }

    override fun startListening() {
        if (currentState != InteractionFlowState.IDLE) {
            Log.w(TAG, "Cannot start listening, current state: $currentState")
            return
        }

        try {
            allControllers.stt.startListening()
            setState(InteractionFlowState.LISTENING)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}")
            stateCallback?.onError("Failed to start listening: ${e.message}")
        }
    }

    override fun startConversationFromInput(userInput: String) {
        if (conversationManager == null) {
            Log.w(TAG, "No conversation manager set, cannot process input: $userInput")
            stateCallback?.onError("No active conversation")
            return
        }

        setState(InteractionFlowState.PROCESSING)

        conversationManager!!.processUserMessage(
            userInput,
            object : ConversationManager.ConversationCallback {
                override fun onPartialResponse(newToken: String) {
                    Log.d(TAG, "LLM partial response: $newToken")
                    contentCallback?.onPartialResponse(newToken)

                    if (currentState == InteractionFlowState.PROCESSING) {
                        setState(InteractionFlowState.SPEAKING)
                    }
                    allControllers.tts.service?.speakIncremental(newToken)
                }

                override fun onCompleteResponse(finalResponse: String) {
                    Log.d(TAG, "LLM complete response: $finalResponse")
                    contentCallback?.onFinalResponse(finalResponse)
                    setState(InteractionFlowState.IDLE)
                }

                override fun onError(error: String) {
                    Log.e(TAG, "Conversation error: $error")
                    setState(InteractionFlowState.IDLE)
                    stateCallback?.onError("Conversation error: $error")
                }
            }
        )
    }

    override fun cancelCurrentFlow() {
        Log.d(TAG, "Cancelling current flow, state: $currentState")

        setState(InteractionFlowState.CANCELLING)

        allControllers.stt.cancelListening()
        allControllers.tts.service?.stopSpeaking()

        setState(InteractionFlowState.IDLE)
        stateCallback?.onFlowCancelled()
    }

    override fun isFlowActive(): Boolean {
        return currentState != InteractionFlowState.IDLE
    }

    override fun getCurrentFlowState(): InteractionFlowState {
        return currentState
    }

    override fun setConversationManager(conversationManager: ConversationManager?) {
        this.conversationManager = conversationManager
        Log.d(TAG, "Conversation manager set: ${conversationManager != null}")
    }

    override fun setStateCallback(callback: InteractionStateCallback?) {
        this.stateCallback = callback
    }

    override fun setContentCallback(callback: InteractionContentCallback?) {
        this.contentCallback = callback
    }

    private fun setState(newState: InteractionFlowState) {
        if (currentState == newState) return

        Log.d(TAG, "State transition: $currentState -> $newState")
        currentState = newState

        // Notify state callback
        when (newState) {
            InteractionFlowState.IDLE -> {
                // Don't send specific callback for IDLE, let completion/cancellation callbacks handle it
            }

            InteractionFlowState.LISTENING -> stateCallback?.onListeningStarted()
            InteractionFlowState.PROCESSING -> {
                stateCallback?.onListeningStopped()
                stateCallback?.onProcessingStarted()
            }

            InteractionFlowState.SPEAKING -> {
                stateCallback?.onProcessingStopped()
                stateCallback?.onSpeakingStarted()
            }

            InteractionFlowState.CANCELLING -> {
                // Cancellation will trigger onFlowCancelled
            }
        }
    }
}