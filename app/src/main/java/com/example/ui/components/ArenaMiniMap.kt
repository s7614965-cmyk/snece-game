package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.ArenaEngine
import com.example.game.Team
import com.example.model.GameMode

/**
 * Compact circular radar mini-map displaying player, teammates, and enemy snake positions.
 */
@Composable
fun ArenaMiniMap(
    engine: ArenaEngine,
    frameTick: Long,
    modifier: Modifier = Modifier
) {
    // Reading frameTick triggers lightweight redraw during active gameplay
    val tick = frameTick

    Canvas(
        modifier = modifier
            .size(86.dp)
            .testTag("arena_minimap")
    ) {
        val _unused = tick
        val r = size.minDimension * 0.46f
        val center = Offset(size.width / 2f, size.height / 2f)
        val scale = r / engine.arenaRadius

        drawCircle(Color(0xB8081838), radius = r, center = center)
        drawCircle(Color(0xAA4CC9F0), radius = r, center = center, style = Stroke(width = 2.5f))

        // Draw rival & teammate blips
        for (snake in engine.snakes) {
            if (!snake.isAlive || snake.isPlayer) continue
            val sx = center.x + snake.x * scale
            val sy = center.y + snake.y * scale
            val blipColor = when {
                engine.mode == GameMode.TEAM_BATTLE && snake.team == Team.BLUE -> Color(0xFF00E5FF)
                else -> Color(0xFFFF5E7E)
            }
            drawCircle(color = blipColor, radius = 3.2f, center = Offset(sx, sy))
        }

        // Draw player blip prominently on top
        if (engine.player.isAlive) {
            val px = center.x + engine.player.x * scale
            val py = center.y + engine.player.y * scale
            drawCircle(color = Color(0x552EE59D), radius = 8f, center = Offset(px, py))
            drawCircle(color = Color(0xFF2EE59D), radius = 4.6f, center = Offset(px, py))
            drawCircle(color = Color.White, radius = 4.6f, center = Offset(px, py), style = Stroke(width = 1.4f))
        }
    }
}
