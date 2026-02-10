package com.penumbraos.mabl.services

import android.content.Context
import android.util.Log
import com.penumbraos.mabl.BuildConfig
import com.penumbraos.mabl.aipincore.view.model.PlatformViewModel
import com.penumbraos.mabl.conversation.ConversationManager
import com.penumbraos.mabl.data.AppDatabase
import com.penumbraos.mabl.data.repository.ConversationImageRepository
import com.penumbraos.mabl.data.repository.ConversationRepository
import com.penumbraos.mabl.interaction.InteractionFlowManager
import com.penumbraos.mabl.sound.SoundEffectManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import java.io.File

private const val TAG = "AllControllers"

// TODO: Find a better name
class AllControllers(coroutineScope: CoroutineScope, private val context: Context) {
    val viewModel =
        PlatformViewModel(coroutineScope, context, AppDatabase.getDatabase(context))

    lateinit var llm: LlmController
    lateinit var stt: SttController
    lateinit var tts: TtsController
    lateinit var toolOrchestrator: ToolOrchestrator
    lateinit var interactionFlowManager: InteractionFlowManager
    lateinit var conversationManager: ConversationManager
    lateinit var conversationRepository: ConversationRepository
    lateinit var conversationImageRepository: ConversationImageRepository
    lateinit var cameraRollService: CameraRollService

    val soundEffectManager = SoundEffectManager()

    val allLoaded = CompletableDeferred<Unit>()

    private fun checkAllConnected() {
        if (llm.isConnected && stt.isConnected && tts.isConnected) {
            allLoaded.complete(Unit)
        }
    }

    suspend fun initialize() {
        llm = LlmController { checkAllConnected() }
        stt = SttController { checkAllConnected() }
        tts = TtsController { checkAllConnected() }
        toolOrchestrator = ToolOrchestrator(context, this)
        toolOrchestrator.initialize()

        val database = AppDatabase.getDatabase(context)
        conversationRepository = ConversationRepository(
            database.conversationDao(),
            database.conversationMessageDao()
        )
        conversationImageRepository =
            ConversationImageRepository(context, database.conversationImageDao())

        cameraRollService = CameraRollService(context)

        conversationManager =
            ConversationManager(this, context, conversationRepository, conversationImageRepository)
        interactionFlowManager = InteractionFlowManager(this, context)

        connectAll(context)
    }

    private fun useOpenClaw(): Boolean =
        File("/sdcard/penumbra/etc/mabl/openclaw.json").exists()

    private suspend fun connectAll(context: Context) {
        // TODO: These packages shouldn't be hardcoded
        if (BuildConfig.IS_SIMULATOR) {
            // In simulator mode, use more resilient connection approach
            // Only connect to services that are known to work
            try {
                llm.connect(context, "com.penumbraos.mabl.pinsim")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to connect LLM service in simulator: $e")
            }

            try {
                tts.connect(context, "com.penumbraos.plugins.demo")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to connect TTS service in simulator: $e")
            }

            // STT is handled by simulator service - bind to internal simulator service
            try {
                stt.connect(
                    context,
                    context.packageName
                )
                Log.d(TAG, "Connected to simulator STT service")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to connect to simulator STT service: $e")
            }

            try {
                toolOrchestrator.connectAll()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to connect tool orchestrator in simulator: $e")
            }
        } else {
            // Normal mode - connect to all external services
            if (useOpenClaw()) {
                Log.w(TAG, "OpenClaw config found, using OpenClawLlmService")
                llm.connect(
                    context,
                    context.packageName,
                    "com.penumbraos.mabl.plugins.llm.OpenClawLlmService"
                )
            } else {
                llm.connect(context, "com.penumbraos.mabl.pin")
            }
            stt.connect(context, "com.penumbraos.plugins.demo")
            tts.connect(context, "com.penumbraos.plugins.demo")
            toolOrchestrator.connectAll()
        }

        allLoaded.await()
        Log.d(TAG, "All services connected")
    }

    fun shutdown(context: Context) {
        Log.w(TAG, "Shutting down all controllers")
        llm.shutdown(context)
        stt.shutdown(context)
        tts.shutdown(context)
        toolOrchestrator.shutdown()
    }
}