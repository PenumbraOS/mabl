package com.penumbraos.mabl.aipincore.input

interface ITouchpadGestureDelegate {
    fun onGesture(gesture: TouchpadGesture)
}

data class TouchpadGesture(val kind: TouchpadGestureKind, val duration: Long, val fingerCount: Int)

enum class TouchpadGestureKind {
    FINGER_DOWN,
    GESTURE_CANCEL,
    SINGLE_TAP,
    DOUBLE_TAP,
    HOLD_START,
    HOLD_END,
}