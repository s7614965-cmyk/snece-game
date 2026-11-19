package com.example.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.model.HeadAccessory
import com.example.model.SkinPattern
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

object ArenaRenderer {

    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 26f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(180, 0, 0, 0))
    }

    private val popupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(5f, 0f, 2f, android.graphics.Color.argb(200, 0, 0, 0))
    }

    fun DrawScope.renderArena(engine: ArenaEngine) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val zoom = engine.camZoom
        val camX = engine.camX
        val camY = engine.camY

        // 1. Deep Blue Fantasy Backdrop
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF143873), Color(0xFF0A1D45), Color(0xFF050E24)),
                center = Offset(cx, cy),
                radius = maxOf(w, h) * 0.85f
            )
        )

        withTransform({
            translate(left = cx, top = cy)
            scale(scaleX = zoom, scaleY = zoom, pivot = Offset.Zero)
            translate(left = -camX, top = -camY)
        }) {
            val halfW = (w * 0.55f) / zoom
            val halfH = (h * 0.55f) / zoom
            val viewLeft = camX - halfW
            val viewRight = camX + halfW
            val viewTop = camY - halfH
            val viewBottom = camY + halfH

            // 2. Fantasy Arena Floor & Arcane Hex/Tile Grid
            drawFantasyFloor(engine.arenaRadius, viewLeft, viewRight, viewTop, viewBottom, engine.worldTime)

            // 3. Collectibles (cull outside viewport for smooth 60fps on low-end phones)
            for (orb in engine.orbs) {
                if (orb.x < viewLeft - 40f || orb.x > viewRight + 40f ||
                    orb.y < viewTop - 40f || orb.y > viewBottom + 40f
                ) continue
                drawOrb(orb, engine.worldTime)
            }

            // 4. Particles
            for (p in engine.particles) {
                val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.radius * (0.4f + 0.6f * alpha),
                    center = Offset(p.x, p.y)
                )
            }

            // 5. Snakes (AI first, Player on top)
            for (snake in engine.snakes) {
                if (!snake.isAlive || snake.isPlayer) continue
                if (snake.maxX < viewLeft - 60f || snake.minX > viewRight + 60f ||
                    snake.maxY < viewTop - 60f || snake.minY > viewBottom + 60f
                ) continue
                drawSnake(snake, engine.worldTime)
            }
            if (engine.player.isAlive) {
                drawSnake(engine.player, engine.worldTime)
            }

            // 6. Floating Score/Reward Popups
            for (pop in engine.popups) {
                val alpha = ((pop.life / pop.maxLife) * 255).toInt().coerceIn(0, 255)
                popupPaint.color = pop.color.toArgb()
                popupPaint.alpha = alpha
                drawContext.canvas.nativeCanvas.drawText(pop.text, pop.x, pop.y, popupPaint)
            }
        }
    }

    private fun DrawScope.drawFantasyFloor(
        arenaRadius: Float,
        viewLeft: Float,
        viewRight: Float,
        viewTop: Float,
        viewBottom: Float,
        worldTime: Float
    ) {
        // Outer danger zone glow
        drawCircle(
            color = Color(0xFF1A0D2E),
            radius = arenaRadius + 160f,
            center = Offset.Zero
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF17458B), Color(0xFF0E2C5E), Color(0xFF081B3F)),
                center = Offset.Zero,
                radius = arenaRadius
            ),
            radius = arenaRadius,
            center = Offset.Zero
        )

        // Glowing fantasy grid lines inside viewport
        val step = 120f
        val startX = floor(viewLeft / step) * step
        val startY = floor(viewTop / step) * step
        val lineColor = Color(0x2856B4FF)

        var gx = startX
        while (gx <= viewRight) {
            if (kotlin.math.abs(gx) < arenaRadius) {
                val maxSpan = kotlin.math.sqrt((arenaRadius * arenaRadius - gx * gx).coerceAtLeast(0f))
                val y0 = maxOf(viewTop, -maxSpan)
                val y1 = minOf(viewBottom, maxSpan)
                if (y1 > y0) {
                    drawLine(lineColor, Offset(gx, y0), Offset(gx, y1), strokeWidth = 2f)
                }
            }
            gx += step
        }

        var gy = startY
        while (gy <= viewBottom) {
            if (kotlin.math.abs(gy) < arenaRadius) {
                val maxSpan = kotlin.math.sqrt((arenaRadius * arenaRadius - gy * gy).coerceAtLeast(0f))
                val x0 = maxOf(viewLeft, -maxSpan)
                val x1 = minOf(viewRight, maxSpan)
                if (x1 > x0) {
                    drawLine(lineColor, Offset(x0, gy), Offset(x1, gy), strokeWidth = 2f)
                }
            }
            gy += step
        }

        // Subtle glowing fantasy center crest
        if (viewLeft < 320f && viewRight > -320f && viewTop < 320f && viewBottom > -320f) {
            val pulse = 0.85f + 0.15f * sin(worldTime * 2f)
            drawCircle(
                color = Color(0x2000E5FF),
                radius = 190f * pulse,
                center = Offset.Zero,
                style = Stroke(width = 6f)
            )
            drawCircle(
                color = Color(0x14FFD166),
                radius = 110f,
                center = Offset.Zero,
                style = Stroke(width = 4f)
            )
        }

        // Glowing Arena Barrier Ring
        drawCircle(
            color = Color(0x55FF4D6D),
            radius = arenaRadius + 10f,
            center = Offset.Zero,
            style = Stroke(width = 24f)
        )
        drawCircle(
            color = Color(0xFFFF5E7E),
            radius = arenaRadius,
            center = Offset.Zero,
            style = Stroke(width = 8f)
        )
    }

    private fun DrawScope.drawOrb(orb: ArenaOrb, worldTime: Float) {
        val bobY = sin(worldTime * 3.8f + orb.bobPhase) * 3.5f
        val cx = orb.x
        val cy = orb.y + bobY
        val r = orb.radius

        when (orb.kind) {
            OrbKind.COIN -> {
                val spinScale = 0.65f + 0.35f * kotlin.math.abs(cos(worldTime * 4f + orb.bobPhase))
                drawCircle(Color(0x40FFD166), radius = r * 1.65f, center = Offset(cx, cy))
                drawOval(
                    color = Color(0xFFE09F1F),
                    topLeft = Offset(cx - r * spinScale, cy - r),
                    size = Size(r * 2f * spinScale, r * 2f)
                )
                drawOval(
                    color = Color(0xFFFFD166),
                    topLeft = Offset(cx - r * 0.78f * spinScale, cy - r * 0.78f),
                    size = Size(r * 1.56f * spinScale, r * 1.56f)
                )
                drawCircle(
                    color = Color(0xFFFFF8D6),
                    radius = r * 0.28f,
                    center = Offset(cx - r * 0.2f * spinScale, cy - r * 0.28f)
                )
            }
            OrbKind.GEM -> {
                val pulse = 1f + 0.08f * sin(worldTime * 5f + orb.bobPhase)
                val gr = r * pulse
                drawCircle(Color(0x48C07BFF), radius = gr * 1.75f, center = Offset(cx, cy))
                val diamond = Path().apply {
                    moveTo(cx, cy - gr * 1.15f)
                    lineTo(cx + gr * 0.95f, cy)
                    lineTo(cx, cy + gr * 1.15f)
                    lineTo(cx - gr * 0.95f, cy)
                    close()
                }
                drawPath(diamond, color = Color(0xFF9D4EDD))
                val inner = Path().apply {
                    moveTo(cx, cy - gr * 0.7f)
                    lineTo(cx + gr * 0.55f, cy)
                    lineTo(cx, cy + gr * 0.7f)
                    lineTo(cx - gr * 0.55f, cy)
                    close()
                }
                drawPath(inner, color = Color(0xFFE0AAFF))
            }
            OrbKind.POWER_MAGNET, OrbKind.POWER_SHIELD, OrbKind.POWER_SURGE -> {
                val pulse = 1f + 0.14f * sin(worldTime * 6f + orb.bobPhase)
                drawCircle(orb.color.copy(alpha = 0.26f), radius = r * 1.85f * pulse, center = Offset(cx, cy))
                drawCircle(orb.color, radius = r, center = Offset(cx, cy))
                drawCircle(Color.White, radius = r, center = Offset(cx, cy), style = Stroke(width = 3f))
                val symbol = when (orb.kind) {
                    OrbKind.POWER_MAGNET -> "U"
                    OrbKind.POWER_SHIELD -> "+"
                    else -> "⚡"
                }
                popupPaint.color = android.graphics.Color.WHITE
                popupPaint.alpha = 255
                popupPaint.textSize = 22f
                drawContext.canvas.nativeCanvas.drawText(symbol, cx, cy + 8f, popupPaint)
                popupPaint.textSize = 34f
            }
            OrbKind.LOOT -> {
                drawCircle(orb.color.copy(alpha = 0.35f), radius = r * 1.6f, center = Offset(cx, cy))
                drawCircle(orb.color, radius = r, center = Offset(cx, cy))
                drawCircle(Color.White.copy(alpha = 0.75f), radius = r * 0.38f, center = Offset(cx - r * 0.25f, cy - r * 0.25f))
            }
            else -> {
                // Cartoon fruit
                drawCircle(orb.color.copy(alpha = 0.22f), radius = r * 1.45f, center = Offset(cx, cy))
                drawCircle(orb.color, radius = r, center = Offset(cx, cy))
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = r * 0.32f,
                    center = Offset(cx - r * 0.3f, cy - r * 0.3f)
                )
                // Little leaf
                drawOval(
                    color = Color(0xFF4ADE80),
                    topLeft = Offset(cx - 1f, cy - r - 4f),
                    size = Size(7f, 5f)
                )
            }
        }
    }

    private fun DrawScope.drawSnake(snake: SnakeEntity, worldTime: Float) {
        val alpha = if (snake.spawnInvulnTimer > 0f && ((worldTime * 14f).toInt() % 2 == 0)) 0.65f else 1f
        val r = snake.bodyRadius
        val segs = snake.segments
        if (segs.isEmpty()) return

        // Magnet ring aura around player/snake
        if (snake.magnetTimer > 0f) {
            val wave = (worldTime * 1.8f) % 1f
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = (1f - wave) * 0.45f),
                radius = r * 2.5f + wave * 120f,
                center = Offset(snake.x, snake.y),
                style = Stroke(width = 4f)
            )
        }

        // Draw body from tail to head
        for (i in segs.lastIndex downTo 1) {
            val pt = segs[i]
            val tailFactor = if (i > segs.size - 5) {
                (segs.size - i) / 5f * 0.45f + 0.55f
            } else 1f
            val segR = r * tailFactor

            // Soft drop shadow
            drawCircle(
                color = Color(0x33000000),
                radius = segR,
                center = Offset(pt.x + 3f, pt.y + 5f)
            )

            // Patterned cartoon segment color
            val segColor = when (snake.skin.pattern) {
                SkinPattern.STRIPED -> if (i % 3 == 0) snake.skin.secondaryColor else snake.skin.primaryColor
                SkinPattern.CHEVRON -> if ((i % 4) < 2) snake.skin.primaryColor else snake.skin.secondaryColor
                SkinPattern.SPOTTED -> snake.skin.primaryColor
                SkinPattern.DIAMOND -> if (i % 2 == 0) snake.skin.primaryColor else snake.skin.secondaryColor
                SkinPattern.CIRCUIT -> snake.skin.primaryColor
                SkinPattern.STARLIGHT -> snake.skin.primaryColor
            }.copy(alpha = alpha)

            drawCircle(color = segColor, radius = segR, center = Offset(pt.x, pt.y))

            // Outline / boost glow
            if (snake.isBoosting || snake.surgeTimer > 0f) {
                drawCircle(
                    color = snake.skin.secondaryColor.copy(alpha = 0.5f * alpha),
                    radius = segR + 3f,
                    center = Offset(pt.x, pt.y),
                    style = Stroke(width = 2.5f)
                )
            }

            // Secondary pattern spot/ring
            when (snake.skin.pattern) {
                SkinPattern.SPOTTED -> if (i % 2 == 0) {
                    drawCircle(
                        color = snake.skin.secondaryColor.copy(alpha = alpha),
                        radius = segR * 0.42f,
                        center = Offset(pt.x, pt.y)
                    )
                }
                SkinPattern.CIRCUIT -> if (i % 3 == 0) {
                    drawCircle(
                        color = snake.skin.secondaryColor.copy(alpha = alpha),
                        radius = segR * 0.55f,
                        center = Offset(pt.x, pt.y),
                        style = Stroke(width = 2.5f)
                    )
                }
                SkinPattern.STARLIGHT -> if (i % 2 == 1) {
                    drawCircle(
                        color = snake.skin.secondaryColor.copy(alpha = alpha),
                        radius = segR * 0.28f,
                        center = Offset(pt.x, pt.y)
                    )
                }
                else -> {}
            }
        }

        // Team Battle indicator ring around head
        if (snake.team != Team.NONE) {
            val teamColor = if (snake.team == Team.BLUE) Color(0xFF00E5FF) else Color(0xFFFF4D6D)
            drawCircle(
                color = teamColor.copy(alpha = 0.65f),
                radius = r + 7f,
                center = Offset(snake.x, snake.y),
                style = Stroke(width = 4f)
            )
        }

        // Cute Cartoon Snake Head
        drawSnakeHead(snake, r * 1.12f, worldTime, alpha)

        // Shield Bubble Dome
        if (snake.shieldTimer > 0f) {
            val shieldPulse = 1f + 0.06f * sin(worldTime * 8f)
            drawCircle(
                color = Color(0x382EE59D),
                radius = r * 2.15f * shieldPulse,
                center = Offset(snake.x, snake.y)
            )
            drawCircle(
                color = Color(0xFF2EE59D),
                radius = r * 2.15f * shieldPulse,
                center = Offset(snake.x, snake.y),
                style = Stroke(width = 4f)
            )
        }

        // Name tag above snake
        namePaint.color = if (snake.isPlayer) android.graphics.Color.rgb(255, 224, 102) else android.graphics.Color.WHITE
        drawContext.canvas.nativeCanvas.drawText(
            snake.name,
            snake.x,
            snake.y - r - 18f,
            namePaint
        )
    }

    private fun DrawScope.drawSnakeHead(
        snake: SnakeEntity,
        headR: Float,
        worldTime: Float,
        alpha: Float
    ) {
        val deg = snake.angle * (180f / PI.toFloat())
        rotate(degrees = deg, pivot = Offset(snake.x, snake.y)) {
            val hx = snake.x
            val hy = snake.y

            // Cute flickering forked tongue!
            val tongueCycle = (worldTime * 2.4f + snake.id) % 3.2f
            if (tongueCycle < 0.75f) {
                val len = headR * 0.78f * sin((tongueCycle / 0.75f) * PI.toFloat())
                val tipX = hx + headR + len
                drawLine(Color(0xFFFF4D6D), Offset(hx + headR * 0.7f, hy), Offset(tipX, hy), strokeWidth = 3.5f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFF4D6D), Offset(tipX, hy), Offset(tipX + 6f, hy - 5f), strokeWidth = 2.8f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFF4D6D), Offset(tipX, hy), Offset(tipX + 6f, hy + 5f), strokeWidth = 2.8f, cap = StrokeCap.Round)
            }

            // Head base & subtle outline
            drawCircle(
                color = Color(0x44000000),
                radius = headR + 2f,
                center = Offset(hx + 2f, hy + 4f)
            )
            drawCircle(
                color = snake.skin.primaryColor.copy(alpha = alpha),
                radius = headR,
                center = Offset(hx, hy)
            )
            drawCircle(
                color = snake.skin.secondaryColor.copy(alpha = alpha),
                radius = headR,
                center = Offset(hx, hy),
                style = Stroke(width = 3.5f)
            )

            // Cute Cartoon Eyes (with periodic blinking!)
            val blinkPhase = (worldTime * 1.1f + snake.id * 0.7f) % 4.0f
            val isBlinking = blinkPhase > 3.85f
            val eyeX = hx + headR * 0.34f
            val eyeOffsetY = headR * 0.48f
            val eyeR = headR * 0.34f

            if (isBlinking) {
                drawLine(Color(0xFF0A1B3D), Offset(eyeX - eyeR * 0.6f, hy - eyeOffsetY), Offset(eyeX + eyeR * 0.6f, hy - eyeOffsetY), strokeWidth = 3.2f, cap = StrokeCap.Round)
                drawLine(Color(0xFF0A1B3D), Offset(eyeX - eyeR * 0.6f, hy + eyeOffsetY), Offset(eyeX + eyeR * 0.6f, hy + eyeOffsetY), strokeWidth = 3.2f, cap = StrokeCap.Round)
            } else {
                // White sclera
                drawCircle(Color.White.copy(alpha = alpha), radius = eyeR, center = Offset(eyeX, hy - eyeOffsetY))
                drawCircle(Color.White.copy(alpha = alpha), radius = eyeR, center = Offset(eyeX, hy + eyeOffsetY))
                // Expressive pupils looking forward
                val pupilR = eyeR * 0.58f
                val pupilShift = eyeR * 0.28f
                drawCircle(Color(0xFF0F172A).copy(alpha = alpha), radius = pupilR, center = Offset(eyeX + pupilShift, hy - eyeOffsetY))
                drawCircle(Color(0xFF0F172A).copy(alpha = alpha), radius = pupilR, center = Offset(eyeX + pupilShift, hy + eyeOffsetY))
                // Eye sparkle
                drawCircle(Color.White, radius = pupilR * 0.35f, center = Offset(eyeX + pupilShift - 1.5f, hy - eyeOffsetY - 1.5f))
                drawCircle(Color.White, radius = pupilR * 0.35f, center = Offset(eyeX + pupilShift - 1.5f, hy + eyeOffsetY - 1.5f))
            }

            // Skin Head Accessories
            when (snake.skin.accessory) {
                HeadAccessory.CROWN -> {
                    val crownPath = Path().apply {
                        moveTo(hx - headR * 0.4f, hy - headR * 0.55f)
                        lineTo(hx - headR * 0.5f, hy - headR * 1.1f)
                        lineTo(hx - headR * 0.15f, hy - headR * 0.8f)
                        lineTo(hx, hy - headR * 1.2f)
                        lineTo(hx + headR * 0.15f, hy - headR * 0.8f)
                        lineTo(hx + headR * 0.45f, hy - headR * 1.05f)
                        lineTo(hx + headR * 0.35f, hy - headR * 0.55f)
                        close()
                    }
                    drawPath(crownPath, color = Color(0xFFFFD166))
                }
                HeadAccessory.HORNS -> {
                    drawLine(Color(0xFFFFF3C4), Offset(hx - 4f, hy - headR * 0.7f), Offset(hx - 14f, hy - headR * 1.25f), strokeWidth = 5f, cap = StrokeCap.Round)
                    drawLine(Color(0xFFFFF3C4), Offset(hx - 4f, hy + headR * 0.7f), Offset(hx - 14f, hy + headR * 1.25f), strokeWidth = 5f, cap = StrokeCap.Round)
                }
                HeadAccessory.VISOR -> {
                    drawRect(
                        color = Color(0xCC00F5D4),
                        topLeft = Offset(eyeX - eyeR * 0.4f, hy - eyeOffsetY - eyeR * 0.7f),
                        size = Size(eyeR * 1.4f, (eyeOffsetY + eyeR * 0.7f) * 2f)
                    )
                }
                HeadAccessory.BLUSH -> {
                    drawCircle(Color(0x88FF4D6D), radius = headR * 0.22f, center = Offset(hx + 2f, hy - headR * 0.72f))
                    drawCircle(Color(0x88FF4D6D), radius = headR * 0.22f, center = Offset(hx + 2f, hy + headR * 0.72f))
                }
                HeadAccessory.HALO -> {
                    drawCircle(
                        color = Color(0xFFFDE047),
                        radius = headR * 0.65f,
                        center = Offset(hx - headR * 0.45f, hy),
                        style = Stroke(width = 3.5f)
                    )
                }
                HeadAccessory.NONE -> {}
            }
        }
    }
}
