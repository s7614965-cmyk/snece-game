package com.example

import com.example.audio.GameAudioManager
import com.example.game.ArenaEngine
import com.example.game.ArenaOrb
import com.example.game.OrbKind
import com.example.game.Team
import com.example.model.GameMode
import com.example.model.SnakeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.Color

class ArenaEngineTest {

    private fun createEngine(mode: GameMode = GameMode.CLASSIC_SURVIVAL): ArenaEngine {
        val audio = GameAudioManager().apply { soundEnabled = false }
        return ArenaEngine(
            mode = mode,
            playerName = "Tester",
            playerSkin = SnakeCatalog.byId("emerald_dash"),
            magnetLevel = 1,
            shieldLevel = 1,
            boostLevel = 1,
            joystickSensitivity = 1f,
            audioManager = audio,
            onHapticEvent = {}
        )
    }

    @Test
    fun playerSteersAndMovesSmoothly() {
        val engine = createEngine()
        val startX = engine.player.x
        val startY = engine.player.y
        engine.setJoystickInput(1f, 0f, true)
        repeat(20) {
            engine.step(0.016f)
        }
        assertTrue(engine.player.x > startX + 10f || engine.player.y != startY)
    }

    @Test
    fun playerCollectsFoodAndCoinsAndGrows() {
        val engine = createEngine()
        val initialScore = engine.player.score
        engine.orbs.add(
            ArenaOrb(
                id = 99999,
                x = engine.player.x,
                y = engine.player.y,
                kind = OrbKind.COIN,
                radius = 15f,
                scoreValue = 25,
                growthValue = 1.5f,
                color = Color.Yellow,
                bobPhase = 0f
            )
        )
        engine.step(0.016f)
        assertTrue(engine.player.score >= initialScore + 25)
        assertEquals(1, engine.playerCoinsEarned)
    }

    @Test
    fun specialAbilityGrantsShieldAndMagnet() {
        val engine = createEngine()
        assertTrue(engine.activateSpecialAbility())
        assertTrue(engine.player.shieldTimer > 0f)
        assertTrue(engine.player.magnetTimer > 0f)
        assertEquals(0f, engine.abilityCharge, 0.01f)
    }

    @Test
    fun arenaBoundaryCollisionEndsMatchWhenUnshielded() {
        val engine = createEngine()
        engine.player.shieldTimer = 0f
        engine.player.x = engine.arenaRadius + 5f
        engine.player.y = 0f
        engine.step(0.016f)
        val hud = engine.buildHudState()
        assertFalse(engine.player.isAlive)
        assertTrue(hud.isGameOver)
    }

    @Test
    fun teamBattleAssignsBlueAndCrimsonTeams() {
        val engine = createEngine(GameMode.TEAM_BATTLE)
        assertEquals(Team.BLUE, engine.player.team)
        assertTrue(engine.snakes.any { !it.isPlayer && it.team == Team.BLUE })
        assertTrue(engine.snakes.any { !it.isPlayer && it.team == Team.CRIMSON })
    }
}
