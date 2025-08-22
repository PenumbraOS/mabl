package com.penumbraos.mabl.aipincore.view

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlatformViewModel : ViewModel() {
    private val _menuVisibleState = MutableStateFlow(false)
    val menuVisibleState = _menuVisibleState.asStateFlow()

    fun backGesture() {
        Log.d("PlatformViewModel", "Back gesture received")
        if (_menuVisibleState.value) {
            _menuVisibleState.update { false }
        }
    }

    fun toggleMenuVisible() {
        Log.d("PlatformViewModel", "Toggling menu visibility")
        _menuVisibleState.update { currentVisible -> !currentVisible }
    }
}