package com.penumbraos.mabl.ui

import android.view.MotionEvent

object SimulatorEventRouter {
    var instance: TouchpadEventHandler? = null
    
    interface TouchpadEventHandler {
        fun onSimulatorTouchpadEvent(event: MotionEvent)
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