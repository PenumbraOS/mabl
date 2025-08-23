package com.penumbraos.mabl.aipincore.view.nav.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.Scene
import androidx.navigation3.ui.SceneStrategy

class WithMenuScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val baseEntry: NavEntry<T>,
    val menuEntry: NavEntry<T>
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOf(baseEntry, menuEntry)
    override val content: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {
            baseEntry.Content()
            menuEntry.Content()
        }
    }
}

class WithMenuSceneStrategy<T : Any> : SceneStrategy<T> {
    @Composable
    override fun calculateScene(entries: List<NavEntry<T>>, onBack: (Int) -> Unit): Scene<T>? {
        val lastTwoEntries = entries.takeLast(2)

        return if (lastTwoEntries.size == 2 && lastTwoEntries[1].contentKey == "MenuNav") {
            val baseEntry = lastTwoEntries[0]
            val menuEntry = lastTwoEntries[1]

            val sceneKey = Pair(baseEntry, menuEntry)

            WithMenuScene(sceneKey, entries.dropLast(1), baseEntry, menuEntry)
        } else {
            null
        }
    }
}