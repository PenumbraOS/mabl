package com.penumbraos.mabl.interaction

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Environment
import android.os.IBinder
import android.util.Log
import com.penumbraos.mabl.conversation.ConversationManager
import com.penumbraos.mabl.sdk.ISttCallback
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.mabl.services.CameraService
import com.penumbraos.mabl.types.Error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "InteractionFlowManager"

enum class InteractionFlowModality {
    Speech,
    Vision
}

class InteractionFlowManager
    (
    private val allControllers: AllControllers,
    private val context: Context,
) : IInteractionFlowManager {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var currentState = InteractionFlowState.IDLE
    private var currentModality = InteractionFlowModality.Speech

    private var conversationManager: ConversationManager? = null
    private var stateCallback: InteractionStateCallback? = null
    private var contentCallback: InteractionContentCallback? = null

    private var cameraService: CameraService? = null
    private var isCameraServiceBound = false

    private val cameraServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CameraService.CameraBinder
            cameraService = binder.getService()
            isCameraServiceBound = true
            Log.d(TAG, "Camera service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cameraService = null
            isCameraServiceBound = false
            Log.d(TAG, "Camera service disconnected")
        }
    }

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
            stateCallback?.onError(Error.SttError(errorMessage))
        }
    }

    init {
        // Set the STT callback on the controller
        allControllers.stt.delegate = sttCallback

        // Start and bind to camera service
        val intent = Intent(context, CameraService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, cameraServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun startListening(requestImage: Boolean) {
        if (currentState != InteractionFlowState.IDLE) {
            Log.w(TAG, "Cannot start listening, current state: $currentState")
            return
        }

        try {
            allControllers.stt.startListening()
            setState(InteractionFlowState.LISTENING)
            currentModality =
                if (requestImage) InteractionFlowModality.Vision else InteractionFlowModality.Speech
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}")
            stateCallback?.onError(Error.SttError("Failed to start listening: ${e.message}"))
        }
    }

    override fun startConversationFromInput(userInput: String) {
        if (conversationManager == null) {
            Log.w(TAG, "No conversation manager set, cannot process input: $userInput")
            stateCallback?.onError(Error.FlowError("No active conversation"))
            return
        }

        setState(InteractionFlowState.PROCESSING)

        coroutineScope.launch {
            var imageInput: File? = null

            if (currentModality == InteractionFlowModality.Vision) {
                Log.d(TAG, "Starting conversation with grounding image")
                imageInput = takeGroundingImage()
            }

            conversationManager!!.startOrContinueConversationWithMessage(
                userInput,
                imageInput,
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
                        stateCallback?.onError(Error.LlmError("Conversation error: $error"))
                    }
                }
            )
        }
    }

    override fun finishListening() {
        Log.d(TAG, "Stopping listening, state: $currentState")
        setState(InteractionFlowState.CANCELLING)

        allControllers.stt.cancelListening()
        allControllers.tts.service?.stopSpeaking()

        setState(InteractionFlowState.IDLE)
        stateCallback?.onUserFinished()
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

    private suspend fun takeGroundingImage(): File? {
        if (isCameraServiceBound && cameraService != null) {
            val imageData = cameraService!!.takePicture()
            if (imageData != null) {
                Log.d(TAG, "Grounding image captured, size: ${imageData.size}")

                val imageId = UUID.randomUUID().toString()

                val imageFile = File(
                    context.cacheDir,
                    "${conversationManager!!.currentConversationId}/$imageId.jpg"
                )
                imageFile.parentFile?.mkdirs()

                FileOutputStream(imageFile).use { fos ->
                    fos.write(imageData)
                }

                // TODO: For debugging only
                // Save image to Pictures directory
                try {
                    val picturesDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val mablDir = File(picturesDir, "MABL")
                    mablDir.mkdirs()

                    val timestamp =
                        SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(
                            Date()
                        )
                    val imageFile = File(mablDir, "grounding_image_$timestamp.jpg")

                    FileOutputStream(imageFile).use { fos ->
                        fos.write(imageData)
                    }

                    Log.d(TAG, "Grounding image saved to: ${imageFile.absolutePath}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save grounding image", e)
                }

                return imageFile
            }
        } else {
            Log.e(TAG, "Camera service not available")
        }

        return null
    }

//    fun cleanup() {
//        if (isCameraServiceBound) {
//            context.unbindService(cameraServiceConnection)
//            isCameraServiceBound = false
//        }
//    }
}