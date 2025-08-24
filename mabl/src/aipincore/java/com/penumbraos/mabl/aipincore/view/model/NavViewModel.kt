package com.penumbraos.mabl.aipincore.view.model

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

data object HomeNav
data object MenuNav
data object ConversationsNav
data object SettingsNav
data object DummyNav

class NavViewModel() : ViewModel() {
    val backStack = mutableStateListOf<Any>(HomeNav)

    fun pushView(view: Any) {
        backStack.add(view)
    }

    fun popView() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    fun replaceLastView(view: Any) {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
        backStack.add(view)
    }

    fun jumpHome() {
        backStack.clear()
        backStack.add(HomeNav)
    }
}
