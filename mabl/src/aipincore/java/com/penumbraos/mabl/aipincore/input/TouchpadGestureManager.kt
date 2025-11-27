package com.penumbraos.mabl.aipincore.input

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.InputEvent
import android.view.MotionEvent
import androidx.lifecycle.LifecycleCoroutineScope
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.api.types.TouchpadInputReceiver
import kotlinx.coroutines.launch

private const val TAG = "TouchpadGestureListener"

private const val MIN_GESTURE_SEPARATION_MS = 500L
private const val MIN_HOLD_TIME_MS = 200L

class TouchpadGestureManager(
    context: Context,
    lifecycleScope: LifecycleCoroutineScope,
    private val client: PenumbraClient,
    private val delegate: ITouchpadGestureDelegate,
) {
    private val gestureDetector: GestureDetector =
        GestureDetector(context, InnerTouchpadGestureListener())
    private var isHolding = false
    private var holdStartTime = 0L
    private var twoFingerHoldHandler: Handler? = null
    private var singleFingerHoldHandler: Handler? = null
    private val activePointers = mutableSetOf<Int>()

    private var lastEventTime: Long = 0

    init {
        lifecycleScope.launch {
            client.waitForBridge()

            client.touchpad.register(object : TouchpadInputReceiver {
                override fun onInputEvent(event: InputEvent) {
                    if (event !is MotionEvent) {
                        return
                    }
                    processTouchpadEvent(event)
                }
            })
        }
    }

    private inner class InnerTouchpadGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            sendEventIfAllowed(e) { TouchpadGesture(TouchpadGestureKind.SINGLE_TAP, 0, 1) }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            sendEventIfAllowed(e) { TouchpadGesture(TouchpadGestureKind.DOUBLE_TAP, 0, 1) }
            return true
        }
    }

    fun processTouchpadEvent(event: MotionEvent) {
        if (event.pointerCount == 1) {
            gestureDetector.onTouchEvent(event)
        }

        // Handle multi-touch and holds
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                activePointers.add(event.getPointerId(0))

                sendEventIfAllowed(event, updateLastEventTime = false) {
                    TouchpadGesture(
                        TouchpadGestureKind.FINGER_DOWN,
                        0,
                        activePointers.size
                    )
                }

                if (activePointers.size == 1) {
                    holdStartTime = event.eventTime
                    singleFingerHoldHandler = Handler(Looper.getMainLooper())
                    singleFingerHoldHandler?.postDelayed({
                        if (!isHolding && activePointers.size == 1) {
                            sendEventIfAllowed(event) {
                                isHolding = true
                                TouchpadGesture(TouchpadGestureKind.HOLD_START, 0, 1)
                            }
                        }
                    }, MIN_HOLD_TIME_MS)
                }
            }

            MotionEvent.ACTION_UP -> {
                activePointers.remove(event.getPointerId(0))

                // Cancel any pending single finger hold
                val wasPendingHold = singleFingerHoldHandler != null
                singleFingerHoldHandler?.removeCallbacksAndMessages(null)
                singleFingerHoldHandler = null

                val duration = event.eventTime - holdStartTime

                // Handle hold end
                if (isHolding) {
                    delegate.onGesture(TouchpadGesture(TouchpadGestureKind.HOLD_END, duration, 1))
                    isHolding = false
                } else if (wasPendingHold && activePointers.isEmpty()) {
                    // Finger was lifted before any gesture started
                    // Only send if we didn't just send a recognized gesture
                    sendEventIfAllowed(event, updateLastEventTime = false) {
                        TouchpadGesture(
                            TouchpadGestureKind.GESTURE_CANCEL,
                            duration,
                            1
                        )
                    }
                }
            }
            // A non-primary touch has changed
            MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                activePointers.add(event.getPointerId(pointerIndex))

                if (activePointers.size == 2) {
                    // Cancel single finger hold when second finger comes down
                    singleFingerHoldHandler?.removeCallbacksAndMessages(null)
                    singleFingerHoldHandler = null

                    holdStartTime = event.eventTime

                    twoFingerHoldHandler = Handler(Looper.getMainLooper())
                    twoFingerHoldHandler?.postDelayed({
                        if (!isHolding && activePointers.size == 2) {
                            sendEventIfAllowed(event) {
                                isHolding = true
                                TouchpadGesture(
                                    TouchpadGestureKind.HOLD_START,
                                    0,
                                    2
                                )
                            }
                        }
                    }, MIN_HOLD_TIME_MS)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                activePointers.remove(event.getPointerId(pointerIndex))

                // We've reduced ourselves to one finger down
                if (activePointers.size == 1) {
                    // Cancel any pending hold
                    twoFingerHoldHandler?.removeCallbacksAndMessages(null)
                    twoFingerHoldHandler = null

                    // This should always beat the ACTION_UP event
                    handleTwoFingerUp(event)
                }
            }
        }
    }

    /**
     * Send TouchpadGesture if allowed based on time since last event. Specifically to prevent sending gesture start events too close together
     */
    private fun sendEventIfAllowed(
        event: MotionEvent,
        updateLastEventTime: Boolean = true,
        lambda: () -> TouchpadGesture,
    ) {
        if (event.eventTime < lastEventTime + MIN_GESTURE_SEPARATION_MS) {
            return
        }

        if (updateLastEventTime) {
            lastEventTime = event.eventTime
        }
        delegate.onGesture(lambda())
    }

    private fun handleTwoFingerUp(event: MotionEvent) {
        val duration = event.eventTime - holdStartTime

        if (isHolding) {
            delegate.onGesture(TouchpadGesture(TouchpadGestureKind.HOLD_END, duration, 2))
            isHolding = false
        } else if (duration < 200) {
            delegate.onGesture(TouchpadGesture(TouchpadGestureKind.SINGLE_TAP, duration, 2))
        } else {
            // Finger was lifted before any gesture completed
            // Only send if we didn't just send a recognized gesture
            sendEventIfAllowed(event, updateLastEventTime = false) {
                TouchpadGesture(TouchpadGestureKind.GESTURE_CANCEL, duration, 2)
            }
        }
    }
}