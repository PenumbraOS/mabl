package com.penumbraos.mabl.aipincore

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.types.Error
import com.penumbraos.mabl.ui.interfaces.IConversationRenderer
import java.io.File

private const val TAG = "AiPinConversationRenderer"

class ConversationRenderer(
    private val context: Context,
    private val controllers: AllControllers,
    private val statusBroadcaster: SettingsStatusBroadcaster? = null
) : IConversationRenderer {
    private val listeningMediaPlayer = MediaPlayer()

    @SuppressLint("SdCardPath")
    private val listeningSoundEffectFile = File("/sdcard/penumbra/mabl/sounds/listening.mp3")

    //    val penumbraClient = PenumbraClient(context)

    init {
        try {
            if (listeningSoundEffectFile.exists()) {
                listeningMediaPlayer.setDataSource(listeningSoundEffectFile.absolutePath)
                listeningMediaPlayer.prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load listening sound effect", e)
        }
    }

//    init {
//        CoroutineScope(Dispatchers.Default).launch {
//            penumbraClient.waitForBridge()
//            penumbraClient.handTracking.acquireHATSLock()
//            Log.d(TAG, "Hand tracking stopped")
//            penumbraClient.handTracking.releaseHATSLock()
//            delay(1000)
//            penumbraClient.handTracking.acquireHATSLock()
//            Log.d(TAG, "Hand tracking stopped v2")
//        }
//    }

    override fun showMessage(message: String, isUser: Boolean) {
        Log.d(TAG, "Message: $message (isUser: $isUser)")

        if (isUser) {
            statusBroadcaster?.sendUserMessageEvent(message)
            statusBroadcaster?.sendAIThinkingStatus(message)
        } else {
            statusBroadcaster?.sendAIResponseEvent(message, false)
            statusBroadcaster?.sendIdleStatus(message)
        }
    }

    override fun showTranscription(text: String) {
        Log.d(TAG, "Transcription: $text")
        statusBroadcaster?.sendTranscribingStatus(text)
    }

    override fun showListening(isListening: Boolean) {
        Log.d(TAG, "Listening: $isListening")
        if (isListening && listeningSoundEffectFile.exists()) {
            listeningMediaPlayer.start()
        }
    }

    override fun showError(error: Error) {
        // TODO: Display onscreen
        when (error) {
            is Error.TtsError -> {}
            is Error.SttError -> {
                val lastListenDuration = controllers.stt.lastListenDuration()
                if (lastListenDuration != null && lastListenDuration < 2000) {
                    // Assume this wasn't a real query
                    return
                }
                controllers.tts.service?.speakImmediately("Sorry, I could not hear you")
                statusBroadcaster?.sendSTTErrorEvent(error.message, "conversationRenderer")
            }

            is Error.LlmError -> {
                controllers.tts.service?.speakImmediately("Failed to talk to LLM")
                statusBroadcaster?.sendLLMErrorEvent(error.message)
                statusBroadcaster?.sendErrorStatus(error.message)
            }

            is Error.FlowError -> {
                controllers.tts.service?.speakImmediately("Failed to talk to LLM")
                statusBroadcaster?.sendLLMErrorEvent(error.message)
                statusBroadcaster?.sendErrorStatus(error.message)
            }
        }
    }
}