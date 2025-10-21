package com.penumbraos.mabl.aipincore.view.model

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.penumbraos.mabl.aipincore.SETTING_APP_ID
import com.penumbraos.mabl.aipincore.SETTING_DEBUG_CATEGORY
import com.penumbraos.mabl.aipincore.SETTING_DEBUG_CURSOR
import com.penumbraos.mabl.data.AppDatabase
import com.penumbraos.mabl.data.repository.ConversationRepository
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.api.BooleanSettingListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PlatformViewModel(
    coroutineScope: CoroutineScope,
    context: Context,
    val database: AppDatabase
) : ViewModel() {
    val navViewModel = NavViewModel()

    var appIsForeground: Boolean = false

    private val _backGestureChannel = Channel<Unit>(Channel.RENDEZVOUS)
    val backGestureEvent = _backGestureChannel.receiveAsFlow()

    private val _openCurrentConversationChannel = Channel<Unit>(Channel.RENDEZVOUS)
    val openCurrentConversationEvent = _openCurrentConversationChannel.receiveAsFlow()

    private val _debugChannel = Channel<Boolean>()
    val debugChannel = _debugChannel.receiveAsFlow()

    val conversationRepository = ConversationRepository(
        database.conversationDao(),
        database.conversationMessageDao()
    )

    init {
        coroutineScope.launch {
            val client = PenumbraClient(context)
            client.waitForBridge()
            client.settings.addBooleanListener(
                SETTING_APP_ID,
                SETTING_DEBUG_CATEGORY,
                SETTING_DEBUG_CURSOR,
                object : BooleanSettingListener {
                    override fun onSettingChanged(value: Boolean) {
                        Log.d("PlatformViewModel", "Debug cursor setting changed to $value")
                        _debugChannel.trySend(value)
                    }
                })
        }
    }

    fun openCurrentConversation() {
        Log.d("PlatformViewModel", "Opening conversation")
        _openCurrentConversationChannel.trySend(Unit)
    }

    fun backGesture() {
        if (!appIsForeground) {
            return
        }
        
        Log.d("PlatformViewModel", "Back gesture received")
        _backGestureChannel.trySend(Unit)
    }

    fun toggleMenuVisible() {
        if (!appIsForeground) {
            return
        }

        if (!closeMenu()) {
            Log.d("PlatformViewModel", "Showing menu")
            navViewModel.backStack.add(MenuNav)
        }
    }

    fun closeMenu(): Boolean {
        Log.d("PlatformViewModel", "Closing menu")
        if (navViewModel.backStack.lastOrNull() == MenuNav) {
            navViewModel.backStack.removeLastOrNull()
            return true
        }

        return false
    }
}