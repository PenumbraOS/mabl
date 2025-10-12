package com.penumbraos.mabl.aipincore.view.model

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

data object HomeNav
data object MenuNav
data object ConversationsNav
data class ConversationDisplayNav(val conversationId: String)
data object SettingsNav
data object DummyNav

class NavViewModel() : ViewModel() {
    val backStack = mutableStateListOf<Any>(HomeNav)

    val isMenuOpen = derivedStateOf {
        backStack.lastOrNull() == MenuNav
    }

    fun pushView(view: Any) {
        val last = backStack.lastOrNull()

        if (last == view) {
            return
        }

        if (last != null && last::class == view::class) {
            // Same class, but not same identity, means this has fields that are likely different
            // Replace this element
            backStack.removeLastOrNull()
        }

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
