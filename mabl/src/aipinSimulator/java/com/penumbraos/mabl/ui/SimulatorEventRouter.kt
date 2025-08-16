package com.penumbraos.mabl.ui

import android.view.MotionEvent
import android.view.KeyEvent

object SimulatorEventRouter {
    var instance: TouchpadEventHandler? = null

    interface TouchpadEventHandler {
        fun onSimulatorTouchpadEvent(event: MotionEvent)
    }
}

object SimulatorKeyEventRouter {
    var instance: KeyEventHandler? = null

    interface KeyEventHandler {
        fun onSimulatorKeyEvent(keyCode: Int, event: KeyEvent?)
    }
}

object SimulatorSttRouter {
    var instance: SttEventHandler? = null

    interface SttEventHandler {
        fun onSimulatorManualInput(text: String)
        fun onSimulatorStartListening()
        fun onSimulatorStopListening()
    }
}