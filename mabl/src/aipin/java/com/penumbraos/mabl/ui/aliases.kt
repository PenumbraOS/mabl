package com.penumbraos.mabl.ui

import androidx.compose.runtime.Composable

typealias ConversationRenderer = com.penumbraos.mabl.aipincore.ConversationRenderer
typealias InputHandler = com.penumbraos.mabl.aipincore.InputHandler

@Composable
fun PlatformUI(uiComponents: UIComponents) =
    com.penumbraos.mabl.aipincore.InternalPlatformUI(uiComponents)
typealias UIFactory = com.penumbraos.mabl.aipincore.UIFactory