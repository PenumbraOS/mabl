package com.penumbraos.mabl.types

import com.penumbraos.mabl.sdk.ILlmService
import com.penumbraos.mabl.sdk.ISttService
import com.penumbraos.mabl.sdk.ITtsService

// TODO: Unused
data class ServiceBundle(
    val stt: ISttService,
    val tts: ITtsService,
    val llm: ILlmService
)
