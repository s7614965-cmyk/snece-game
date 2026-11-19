package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.HeadAccessory
import com.example.model.SkinPattern
import com.example.model.SnakeSkin
import kotlin.math.sin

/**
 * Animated miniature cartoon snake preview used in Main Menu, Skin Selector, and Shop cards.
 */
@Composable
fun SnakePreviewCanvas(
    skin: SnakeSkin,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "snake_preview")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = 10
        val segRadius = (minOf(w, h) * 0.22f).coerceIn(8f, 22f)
        val startX = w * 0.16f
        val endX = w * 0.78f
        val stepX = (endX - startX) / (count - 1)

        // Draw body segments tail -> head
        for (i in 0 until count - 1) {
            val idxFromHead = count - 1 - i
            val px = startX + i * stepX
            val py = h * 0.54f + sin(phase - i * 0.55f) * (h * 0.14f)
            val taper = if (i < 3) 0.6f + i * 0.13f else 1f
            val r = segRadius * taper

            drawCircle(Color(0x33000000), radius = r, center = Offset(px + 2f, py + 4f))

            val color = when (skin.pattern) {
                SkinPattern.STRIPED -> if (idxFromHead % 3 == 0) skin.secondaryColor else skin.primaryColor
                SkinPattern.CHEVRON -> if (idxFromHead % 4 < 2) skin.primaryColor else skin.secondaryColor
                SkinPattern.DIAMOND -> if (idxFromHead % 2 == 0) skin.primaryColor else skin.secondaryColor
                else -> skin.primaryColor
            }
            drawCircle(color = color, radius = r, center = Offset(px, py))

            when (skin.pattern) {
                SkinPattern.SPOTTED -> if (idxFromHead % 2 == 0) {
                    drawCircle(skin.secondaryColor, radius = r * 0.42f, center = Offset(px, py))
                }
                SkinPattern.CIRCUIT -> if (idxFromHead % 3 == 0) {
                    drawCircle(skin.secondaryColor, radius = r * 0.55f, center = Offset(px, py), style = Stroke(2f))
                }
                SkinPattern.STARLIGHT -> if (idxFromHead % 2 == 1) {
                    drawCircle(skin.secondaryColor, radius = r * 0.28f, center = Offset(px, py))
                }
                else -> {}
            }
        }

        // Draw cute head at front
        val hx = endX
        val hy = h * 0.54f + sin(phase - (count - 1) * 0.55f) * (h * 0.14f)
        val headR = segRadius * 1.16f

        // Tongue
        if (sin(phase * 1.5f) > 0.2f) {
            val tx = hx + headR + headR * 0.55f
            drawLine(Color(0xFFFF4D6D), Offset(hx + headR * 0.6f, hy), Offset(tx, hy), strokeWidth = 2.8f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFF4D6D), Offset(tx, hy), Offset(tx + 4f, hy - 3.5f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFF4D6D), Offset(tx, hy), Offset(tx + 4f, hy + 3.5f), strokeWidth = 2.2f, cap = StrokeCap.Round)
        }

        drawCircle(skin.primaryColor, radius = headR, center = Offset(hx, hy))
        drawCircle(skin.secondaryColor, radius = headR, center = Offset(hx, hy), style = Stroke(2.5f))

        val eyeX = hx + headR * 0.34f
        val eyeY = headR * 0.44f
        val eyeR = headR * 0.34f
        drawCircle(Color.White, radius = eyeR, center = Offset(eyeX, hy - eyeY))
        drawCircle(Color.White, radius = eyeR, center = Offset(eyeX, hy + eyeY))
        drawCircle(Color(0xFF0F172A), radius = eyeR * 0.55f, center = Offset(eyeX + eyeR * 0.22f, hy - eyeY))
        drawCircle(Color(0xFF0F172A), radius = eyeR * 0.55f, center = Offset(eyeX + eyeR * 0.22f, hy + eyeY))
        drawCircle(Color.White, radius = eyeR * 0.2f, center = Offset(eyeX + eyeR * 0.1f, hy - eyeY - 1f))
        drawCircle(Color.White, radius = eyeR * 0.2f, center = Offset(eyeX + eyeR * 0.1f, hy + eyeY - 1f))

        when (skin.accessory) {
            HeadAccessory.CROWN -> {
                val p = Path().apply {
                    moveTo(hx - headR * 0.4f, hy - headR * 0.5f)
                    lineTo(hx - headR * 0.5f, hy - headR * 1.1f)
                    lineTo(hx, hy - headR * 1.25f)
                    lineTo(hx + headR * 0.45f, hy - headR * 1.05f)
                    lineTo(hx + headR * 0.3f, hy - headR * 0.5f)
                    close()
                }
                drawPath(p, Color(0xFFFFD166))
            }
            HeadAccessory.HORNS -> {
                drawLine(Color(0xFFFFF3C4), Offset(hx - 2f, hy - headR * 0.7f), Offset(hx - 10f, hy - headR * 1.2f), strokeWidth = 3.5f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFFF3C4), Offset(hx - 2f, hy + headR * 0.7f), Offset(hx - 10f, hy + headR * 1.2f), strokeWidth = 3.5f, cap = StrokeCap.Round)
            }
            HeadAccessory.VISOR -> {
                drawRect(
                    color = Color(0xCC00F5D4),
                    topLeft = Offset(eyeX - eyeR * 0.35f, hy - eyeY - eyeR * 0.65f),
                    size = Size(eyeR * 1.25f, (eyeY + eyeR * 0.65f) * 2f)
                )
            }
            HeadAccessory.BLUSH -> {
                drawCircle(Color(0x88FF4D6D), radius = headR * 0.22f, center = Offset(hx, hy - headR * 0.72f))
                drawCircle(Color(0x88FF4D6D), radius = headR * 0.22f, center = Offset(hx, hy + headR * 0.72f))
            }
            HeadAccessory.HALO -> {
                drawCircle(Color(0xFFFDE047), radius = headR * 0.65f, center = Offset(hx - headR * 0.45f, hy), style = Stroke(2.8f))
            }
            HeadAccessory.NONE -> {}
        }
    }
}
