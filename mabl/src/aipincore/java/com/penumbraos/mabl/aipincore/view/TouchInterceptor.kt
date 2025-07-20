package com.penumbraos.mabl.aipincore.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.open.pin.ui.utils.modifiers.SnapCoordinator

@SuppressLint("ViewConstructor")
class TouchInterceptor(
    val snapCoordinator: SnapCoordinator,
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {

    init {
        // Configure view to be interactable, but invisible
        isClickable = true
        isFocusable = false
        background = null
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            snapCoordinator.processTouchEvent(event)
        }
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            snapCoordinator.processMotionEvent(event)
        }
        return super.dispatchGenericMotionEvent(event)
    }
}