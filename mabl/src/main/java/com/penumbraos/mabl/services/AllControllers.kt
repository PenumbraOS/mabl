package com.penumbraos.mabl.services

import android.content.Context
import android.util.Log
import com.penumbraos.mabl.BuildConfig
import com.penumbraos.mabl.data.AppDatabase
import com.penumbraos.mabl.data.ConversationRepository
import com.penumbraos.mabl.interaction.InteractionFlowManager
import com.penumbraos.mabl.interaction.ConversationSessionManager
import kotlinx.coroutines.CompletableDeferred

private const val TAG = "AllControllers"

// TODO: Find a better name
class AllControllers {
    lateinit var llm: LlmController
    lateinit var stt: SttController
    lateinit var tts: TtsController
    lateinit var toolOrchestrator: ToolOrchestrator
    lateinit var interactionFlowManager: InteractionFlowManager
    lateinit var conversationSessionManager: ConversationSessionManager
    lateinit var conversationRepository: ConversationRepository

    val allLoaded = CompletableDeferred<Unit>()

    private fun checkAllConnected() {
        if (llm.isConnected && stt.isConnected && tts.isConnected) {
            allLoaded.complete(Unit)
        }
    }

    fun initialize(context: Context) {
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
        
        interactionFlowManager = InteractionFlowManager(this)
        conversationSessionManager = ConversationSessionManager(this, conversationRepository)
    }

    suspend fun connectAll(context: Context) {
        // TODO: These packages shouldn't be hardcoded
        if (BuildConfig.IS_SIMULATOR) {
            // In simulator mode, use more resilient connection approach
            // Only connect to services that are known to work
            try {
                llm.connect(context, "com.penumbraos.plugins.openai")
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
                stt.connect(context, context.packageName) // Connect to our own package where SimulatorSttService is registered
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
            llm.connect(context, "com.penumbraos.plugins.openai")
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