package com.penumbraos.mabl.aipincore.view.model

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.penumbraos.mabl.data.AppDatabase

data object HomeNav
data object MenuNav

class NavViewModel(val database: AppDatabase) : ViewModel() {
    val backStack = mutableStateListOf<Any>(HomeNav)
}
