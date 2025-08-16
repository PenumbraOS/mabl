package com.penumbraos.mabl.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlatformViewModel : ViewModel() {
    private val _menuVisibleState = MutableStateFlow(false)
    val menuVisibleState = _menuVisibleState.asStateFlow()

    fun toggleMenuVisible() {
        _menuVisibleState.update { currentVisible -> !currentVisible }
    }
}