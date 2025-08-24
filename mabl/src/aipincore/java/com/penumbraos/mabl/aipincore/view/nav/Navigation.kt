package com.penumbraos.mabl.aipincore.view.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.open.pin.ui.components.text.PinText
import com.penumbraos.mabl.aipincore.view.model.ConversationsNav
import com.penumbraos.mabl.aipincore.view.model.HomeNav
import com.penumbraos.mabl.aipincore.view.model.MenuNav
import com.penumbraos.mabl.aipincore.view.model.NavViewModel
import com.penumbraos.mabl.aipincore.view.nav.util.WithMenuSceneStrategy

val animationSpec = tween<Any>(durationMillis = 300)

@Suppress("UNCHECKED_CAST")
@Composable
fun Navigation(navViewModel: NavViewModel = viewModel<NavViewModel>()) {
    NavDisplay(
        backStack = navViewModel.backStack,
        onBack = { navViewModel.backStack.removeLastOrNull() },
        sceneStrategy = WithMenuSceneStrategy<Any>(),
        entryProvider = { key ->
            when (key) {
                HomeNav -> NavEntry(key) {
                    Home()
                }

                MenuNav -> NavEntry(key, metadata = NavDisplay.transitionSpec {
                    // TODO: This fade in doesn't seem to work right
                    fadeIn(animationSpec = tween(500)) togetherWith ExitTransition.KeepUntilTransitionsFinished
                } + NavDisplay.popTransitionSpec {
                    EnterTransition.None togetherWith fadeOut(animationSpec = animationSpec as TweenSpec<Float>)
                }) {
                    // We can only reliably detect when the view first starts to render
                    val menuVisible = remember { mutableStateOf(false) }
                    // We check if we have removed the menu from the stack to start animating out
                    val isMenuInBackStack = navViewModel.backStack.contains(MenuNav)

                    val animatedRadius by animateDpAsState(
                        targetValue = if (menuVisible.value && isMenuInBackStack) 150.dp else 300.dp,
                        animationSpec = animationSpec as TweenSpec<Dp>,
                        label = "animatedRadius",
                    )

                    LaunchedEffect(Unit) {
                        menuVisible.value = true
                    }

                    Menu(animatedRadius = animatedRadius)
                }

                ConversationsNav -> NavEntry(key) {
                    Conversations()
                }

                else -> NavEntry(Unit) {
                    PinText("Unknown route")
                }
            }
        }
    )
}
