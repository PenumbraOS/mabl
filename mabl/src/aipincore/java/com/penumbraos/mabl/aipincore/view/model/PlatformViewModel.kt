package com.penumbraos.mabl.aipincore.view.model

import android.util.Log
import androidx.lifecycle.ViewModel
import com.penumbraos.mabl.data.AppDatabase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class PlatformViewModel(val database: AppDatabase) : ViewModel() {
    val navViewModel = NavViewModel()

    private val _backChannel = Channel<Unit>(Channel.RENDEZVOUS)
    val backEvent = _backChannel.receiveAsFlow()

    fun backGesture() {
        Log.d("PlatformViewModel", "Back gesture received")
        _backChannel.trySend(Unit)
    }

    fun toggleMenuVisible() {
        Log.d("PlatformViewModel", "Toggling menu visibility")
        if (navViewModel.backStack.lastOrNull() == MenuNav) {
            navViewModel.backStack.removeLastOrNull()
        } else {
            navViewModel.backStack.add(MenuNav)
        }
    }
}