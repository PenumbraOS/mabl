package com.penumbraos.mabl.types

sealed class Error(val message: String) {
    class TtsError(message: String) : Error(message)
    class SttError(message: String) : Error(message)
    class LlmError(message: String) : Error(message)
    class FlowError(message: String) : Error(message)
}
