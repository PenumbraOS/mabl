package com.penumbraos.mabl.simulation

import android.view.MotionEvent
import android.view.KeyEvent
import androidx.compose.foundation.background
import com.penumbraos.mabl.ui.SimulatorEventRouter
import com.penumbraos.mabl.ui.SimulatorKeyEventRouter
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SimulatedTouchpad(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }
    var touchStartTime by remember { mutableLongStateOf(0L) }
    var lastGestureText by remember { mutableStateOf("Tap to interact") }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Touchpad simulation area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    2.dp, 
                    if (isPressed) Color.Blue else Color.Gray, 
                    RoundedCornerShape(8.dp)
                )
                .background(
                    if (isPressed) Color.Blue.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.05f)
                )
                .pointerInput("gestures") {
                    detectTapGestures(
                        onPress = { offset ->
                            isPressed = true
                            touchStartTime = System.currentTimeMillis()
                            lastGestureText = "Pressing..."
                            
                            // Create synthetic MotionEvent for ACTION_DOWN
                            val downEvent = MotionEvent.obtain(
                                touchStartTime,
                                touchStartTime,
                                MotionEvent.ACTION_DOWN,
                                offset.x,
                                offset.y,
                                0
                            )
                            
                            // Route to simulator's input handler
                            SimulatorEventRouter.instance?.onSimulatorTouchpadEvent(downEvent)
                            downEvent.recycle()
                            
                            // Wait for release
                            val released = tryAwaitRelease()
                            val touchEndTime = System.currentTimeMillis()
                            val touchDuration = touchEndTime - touchStartTime
                            
                            isPressed = false
                            
                            // Create synthetic MotionEvent for ACTION_UP
                            val upEvent = MotionEvent.obtain(
                                touchStartTime,
                                touchEndTime,
                                MotionEvent.ACTION_UP,
                                offset.x,
                                offset.y,
                                0
                            )
                            
                            // Route to simulator's input handler
                            SimulatorEventRouter.instance?.onSimulatorTouchpadEvent(upEvent)
                            upEvent.recycle()
                            
                            // Update gesture feedback
                            lastGestureText = when {
                                touchDuration < 200 -> "Single tap (${touchDuration}ms)"
                                touchDuration < 1000 -> "Long press (${touchDuration}ms)"
                                else -> "Hold gesture (${touchDuration}ms)"
                            }
                        }
                    )
                }
                .pointerInput("drags") {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Only handle drags if not already pressed (to avoid conflicts)
                            if (!isPressed) {
                                isPressed = true
                                touchStartTime = System.currentTimeMillis()
                                lastGestureText = "Dragging..."
                                
                                // Create synthetic MotionEvent for ACTION_DOWN
                                val downEvent = MotionEvent.obtain(
                                    touchStartTime,
                                    touchStartTime,
                                    MotionEvent.ACTION_DOWN,
                                    offset.x,
                                    offset.y,
                                    0
                                )
                                
                                // Route to simulator's input handler
                                SimulatorEventRouter.instance?.onSimulatorTouchpadEvent(downEvent)
                                downEvent.recycle()
                            }
                        },
                        onDrag = { change, dragAmount ->
                            lastGestureText = "Dragging... (${dragAmount.x.toInt()}, ${dragAmount.y.toInt()})"
                            
                            // Create synthetic MotionEvent for ACTION_MOVE
                            val moveEvent = MotionEvent.obtain(
                                touchStartTime,
                                System.currentTimeMillis(),
                                MotionEvent.ACTION_MOVE,
                                change.position.x,
                                change.position.y,
                                0
                            )
                            
                            // Route to simulator's input handler
                            SimulatorEventRouter.instance?.onSimulatorTouchpadEvent(moveEvent)
                            moveEvent.recycle()
                        },
                        onDragEnd = {
                            val touchEndTime = System.currentTimeMillis()
                            val touchDuration = touchEndTime - touchStartTime
                            isPressed = false
                            
                            // Create synthetic MotionEvent for ACTION_UP
                            val upEvent = MotionEvent.obtain(
                                touchStartTime,
                                touchEndTime,
                                MotionEvent.ACTION_UP,
                                0f,
                                0f,
                                0
                            )
                            
                            // Route to simulator's input handler
                            SimulatorEventRouter.instance?.onSimulatorTouchpadEvent(upEvent)
                            upEvent.recycle()
                            
                            lastGestureText = "Drag completed (${touchDuration}ms)"
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Touchpad",
                    modifier = Modifier.size(48.dp),
                    tint = if (isPressed) Color.Blue else Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = lastGestureText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPressed) Color.Blue else Color.Gray
                )
                
                if (isPressed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        modifier = Modifier.width(100.dp),
                        color = Color.Blue
                    )
                }
            }
        }
        
        // Control buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back button
            Button(
                onClick = {
                    // Send back button key event (key code 36)
                    val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, 36)
                    SimulatorKeyEventRouter.instance?.onSimulatorKeyEvent(36, keyEvent)
                },
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Menu button
            Button(
                onClick = {
                    // Send menu button key event (key code 54)
                    val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, 54)
                    SimulatorKeyEventRouter.instance?.onSimulatorKeyEvent(54, keyEvent)
                },
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Gesture instructions
        Text(
            text = "• Tap: Voice activation\n• Hold: Long press\n• Drag: Swipe gesture",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp, start = 8.dp, end = 8.dp)
        )
    }
}

