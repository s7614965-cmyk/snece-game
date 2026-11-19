package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * Responsive 360-degree virtual thumbstick optimized for multi-touch mobile gameplay.
 */
@Composable
fun TouchJoystick(
    onDirectionChanged: (dx: Float, dy: Float, active: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var isActive by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .size(136.dp)
            .testTag("touch_joystick")
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.width * 0.36f

                    fun update(pos: Offset) {
                        val delta = pos - center
                        val dist = hypot(delta.x, delta.y)
                        val clamped = if (dist > maxRadius && dist > 0.001f) {
                            Offset((delta.x / dist) * maxRadius, (delta.y / dist) * maxRadius)
                        } else {
                            delta
                        }
                        knobOffset = clamped
                        isActive = true
                        if (dist > 6f) {
                            onDirectionChanged(clamped.x / maxRadius, clamped.y / maxRadius, true)
                        }
                    }

                    update(down.position)
                    down.consume()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            knobOffset = Offset.Zero
                            isActive = false
                            onDirectionChanged(0f, 0f, false)
                            break
                        }
                        update(change.position)
                        change.consume()
                    }
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.width * 0.44f
        val knobRadius = size.width * 0.20f

        // Outer translucent ring
        drawCircle(
            color = if (isActive) Color(0x4400E5FF) else Color(0x33163E7A),
            radius = baseRadius,
            center = center
        )
        drawCircle(
            color = if (isActive) Color(0xCC00E5FF) else Color(0x7789B4F8),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 3.5f)
        )

        // Center crosshair ticks
        drawCircle(
            color = Color(0x33FFFFFF),
            radius = baseRadius * 0.42f,
            center = center,
            style = Stroke(width = 2f)
        )

        // Draggable glowing knob
        val knobCenter = center + knobOffset
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF70F5FF), Color(0xFF00B4D8)),
                center = knobCenter,
                radius = knobRadius
            ),
            radius = knobRadius,
            center = knobCenter
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = knobRadius,
            center = knobCenter,
            style = Stroke(width = 3f)
        )
    }
}
