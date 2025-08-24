package com.penumbraos.mabl.aipincore.view.model

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

data object HomeNav
data object MenuNav
data object ConversationsNav

class NavViewModel() : ViewModel() {
    val backStack = mutableStateListOf<Any>(HomeNav)

    fun pushView(view: Any) {
        backStack.add(view)
    }

    fun replaceLastView(view: Any) {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
        backStack.add(view)
    }

    fun jumpHome() {
        backStack.clear()
        backStack.add(HomeNav)
    }
}
