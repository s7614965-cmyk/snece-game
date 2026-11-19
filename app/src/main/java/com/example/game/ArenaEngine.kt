package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.audio.GameAudioManager
import com.example.audio.SoundEffect
import com.example.model.GameMode
import com.example.model.PowerUpType
import com.example.model.SnakeCatalog
import com.example.model.SnakeSkin
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class Vec2(var x: Float, var y: Float)

enum class Team {
    NONE,
    BLUE,
    CRIMSON
}

enum class OrbKind {
    APPLE,
    BERRY,
    ORANGE,
    STARFRUIT,
    COIN,
    GEM,
    LOOT,
    POWER_MAGNET,
    POWER_SHIELD,
    POWER_SURGE
}

data class ArenaOrb(
    val id: Int,
    var x: Float,
    var y: Float,
    val kind: OrbKind,
    val radius: Float,
    val scoreValue: Int,
    val growthValue: Float,
    val color: Color,
    val bobPhase: Float,
    var isLoot: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color
)

data class FloatingPopup(
    var x: Float,
    var y: Float,
    val text: String,
    val color: Color,
    var life: Float,
    val maxLife: Float = 0.95f
)

class SnakeEntity(
    val id: Int,
    val name: String,
    val isPlayer: Boolean,
    val skin: SnakeSkin,
    val team: Team,
    var x: Float,
    var y: Float,
    var angle: Float,
    initialLength: Int
) {
    var targetAngle: Float = angle
    var score: Int = initialLength * 10
    var kills: Int = 0
    var growthAccumulator: Float = 0f
    var desiredSegments: Int = initialLength
    var isAlive: Boolean = true
    var isBoosting: Boolean = false
    var boostEnergy: Float = 100f
    var shieldTimer: Float = 0f
    var magnetTimer: Float = 0f
    var surgeTimer: Float = 0f
    var spawnInvulnTimer: Float = if (isPlayer) 2.2f else 1.5f
    var aiThinkTimer: Float = 0f
    var aiWanderAngle: Float = angle
    var aiAggression: Float = Random.nextFloat()

    // Bounding box updated each tick for O(1) broad-phase collision rejection
    var minX: Float = x
    var maxX: Float = x
    var minY: Float = y
    var maxY: Float = y

    val trail: MutableList<Vec2> = ArrayList(256)
    val segments: MutableList<Vec2> = ArrayList(initialLength + 8)

    val bodyRadius: Float
        get() = (15f + min(15f, segments.size * 0.13f))

    init {
        val step = 6f
        val backX = -cos(angle)
        val backY = -sin(angle)
        for (i in 0 until initialLength * 3) {
            trail.add(Vec2(x + backX * i * step, y + backY * i * step))
        }
        rebuildSegments()
    }

    fun rebuildSegments() {
        val spacing = bodyRadius * 0.72f
        segments.clear()
        segments.add(Vec2(x, y))
        var neededDist = spacing
        var accum = 0f
        var prev = Vec2(x, y)

        for (i in 1 until trail.size) {
            val cur = trail[i]
            val segLen = hypot(cur.x - prev.x, cur.y - prev.y)
            if (segLen <= 0.001f) continue
            while (accum + segLen >= neededDist && segments.size < desiredSegments) {
                val t = (neededDist - accum) / segLen
                val px = prev.x + (cur.x - prev.x) * t
                val py = prev.y + (cur.y - prev.y) * t
                segments.add(Vec2(px, py))
                neededDist += spacing
            }
            accum += segLen
            prev = cur
            if (segments.size >= desiredSegments) break
        }
        while (segments.size < desiredSegments && trail.isNotEmpty()) {
            val last = trail.last()
            segments.add(Vec2(last.x, last.y))
        }

        // Trim unused excess trail memory
        val maxTrailPoints = (desiredSegments * 5).coerceAtLeast(60)
        while (trail.size > maxTrailPoints) {
            trail.removeAt(trail.lastIndex)
        }

        // Update bounding box
        val pad = bodyRadius + 6f
        var bx0 = x
        var bx1 = x
        var by0 = y
        var by1 = y
        for (s in segments) {
            if (s.x < bx0) bx0 = s.x
            if (s.x > bx1) bx1 = s.x
            if (s.y < by0) by0 = s.y
            if (s.y > by1) by1 = s.y
        }
        minX = bx0 - pad
        maxX = bx1 + pad
        minY = by0 - pad
        maxY = by1 + pad
    }
}

data class LeaderboardRow(
    val name: String,
    val score: Int,
    val isPlayer: Boolean,
    val team: Team
)

data class MatchSummary(
    val mode: GameMode,
    val score: Int,
    val length: Int,
    val kills: Int,
    val coinsEarned: Int,
    val gemsEarned: Int,
    val foodEaten: Int,
    val durationSec: Int,
    val victory: Boolean,
    val deathReason: String,
    val blueTeamScore: Int = 0,
    val crimsonTeamScore: Int = 0
)

data class ArenaHudState(
    val score: Int = 0,
    val length: Int = 14,
    val kills: Int = 0,
    val coinsEarned: Int = 0,
    val gemsEarned: Int = 0,
    val remainingTimeSec: Int? = null,
    val elapsedTimeSec: Int = 0,
    val difficultyTier: Int = 1,
    val boostEnergy: Float = 100f,
    val abilityCharge: Float = 100f,
    val magnetTimeLeft: Float = 0f,
    val shieldTimeLeft: Float = 0f,
    val surgeTimeLeft: Float = 0f,
    val blueTeamScore: Int = 0,
    val crimsonTeamScore: Int = 0,
    val topSnakes: List<LeaderboardRow> = emptyList(),
    val playerRank: Int = 1,
    val totalSnakes: Int = 1,
    val isGameOver: Boolean = false,
    val matchSummary: MatchSummary? = null
)

class ArenaEngine(
    val mode: GameMode,
    private val playerName: String,
    private val playerSkin: SnakeSkin,
    private val magnetLevel: Int,
    private val shieldLevel: Int,
    private val boostLevel: Int,
    private val joystickSensitivity: Float,
    private val audioManager: GameAudioManager,
    private val onHapticEvent: () -> Unit
) {
    val arenaRadius: Float = mode.arenaRadius
    val snakes: MutableList<SnakeEntity> = mutableListOf()
    val orbs: MutableList<ArenaOrb> = mutableListOf()
    val particles: MutableList<Particle> = mutableListOf()
    val popups: MutableList<FloatingPopup> = mutableListOf()

    var player: SnakeEntity
        private set

    var camX: Float = 0f
    var camY: Float = 0f
    var camZoom: Float = 1f
    var worldTime: Float = 0f
        private set

    var playerCoinsEarned: Int = 0
        private set
    var playerGemsEarned: Int = 0
        private set
    var playerFoodEaten: Int = 0
        private set
    var abilityCharge: Float = 100f
        private set

    private var nextOrbId = 1
    private var nextBotId = 2
    private var botRespawnTimer = 0f
    private var powerOrbTimer = 4f
    private var steerX = 0f
    private var steerY = 0f
    private var hasSteerInput = false
    private var playerBoostPressed = false
    private var isEnded = false
    private var matchSummary: MatchSummary? = null
    private var hudAccum = 0f

    private val botNames = listOf(
        "CobaltFang", "MochiTail", "Zapper", "RubyCoil", "Vortex",
        "NoodleKing", "Sparky", "JadeWing", "Comet", "FizzPop",
        "PixelWyrm", "NovaBite", "Pebble", "Tangerine", "FrostBite",
        "AuraSerpent", "Glimmer", "Blitz"
    )

    init {
        val playerTeam = if (mode == GameMode.TEAM_BATTLE) Team.BLUE else Team.NONE
        player = SnakeEntity(
            id = 1,
            name = playerName.ifBlank { "Hero" },
            isPlayer = true,
            skin = playerSkin,
            team = playerTeam,
            x = 0f,
            y = 0f,
            angle = -PI.toFloat() / 2f,
            initialLength = 14
        )
        snakes.add(player)

        // Spawn AI enemies (and Blue teammates in Team Battle)
        repeat(mode.botCount) { idx ->
            val team = if (mode == GameMode.TEAM_BATTLE) {
                if (idx < 4) Team.BLUE else Team.CRIMSON
            } else {
                Team.NONE
            }
            spawnBot(team = team, initialScale = Random.nextInt(12, 26))
        }

        // Populate initial collectibles
        val targetOrbs = when (mode) {
            GameMode.ENDLESS_ARENA -> 340
            GameMode.TIME_CHALLENGE -> 300
            else -> 260
        }
        repeat(targetOrbs) {
            spawnRandomOrb()
        }
    }

    fun setJoystickInput(dx: Float, dy: Float, active: Boolean) {
        if (active && hypot(dx, dy) > 0.08f) {
            setSteerAngle(atan2(dy, dx), preventInstantReverse = true)
        } else if (!active) {
            hasSteerInput = false
        }
    }

    fun setSteerAngle(targetAngleRad: Float, preventInstantReverse: Boolean = true): Boolean {
        val normalizedTarget = normalizeAngle(targetAngleRad)
        if (preventInstantReverse) {
            val delta = kotlin.math.abs(normalizeAngle(normalizedTarget - player.angle))
            if (delta >= REVERSE_BLOCK_THRESHOLD_RAD) {
                return false
            }
        }
        steerX = cos(normalizedTarget)
        steerY = sin(normalizedTarget)
        hasSteerInput = true
        player.targetAngle = normalizedTarget
        return true
    }

    fun clearSteerInput() {
        hasSteerInput = false
    }

    fun setBoostPressed(pressed: Boolean) {
        playerBoostPressed = pressed
    }

    companion object {
        // ~160 degrees: blocks direct 180-degree reversal while allowing smooth 360-degree curved turns
        const val REVERSE_BLOCK_THRESHOLD_RAD: Float = 2.79f
    }

    fun activateSpecialAbility(): Boolean {
        if (isEnded || !player.isAlive || abilityCharge < 100f) return false
        abilityCharge = 0f
        val shieldBonus = 5.0f + (shieldLevel - 1) * 1.2f
        val magnetBonus = 6.5f + (magnetLevel - 1) * 1.5f
        player.shieldTimer = max(player.shieldTimer, shieldBonus)
        player.magnetTimer = max(player.magnetTimer, magnetBonus)
        player.boostEnergy = min(100f, player.boostEnergy + 45f)

        audioManager.play(SoundEffect.POWER_UP)
        onHapticEvent()
        spawnBurst(player.x, player.y, Color(0xFF00E5FF), 24)
        addPopup(player.x, player.y - 36f, "LEGEND BURST!", Color(0xFF00E5FF))

        // Gentle knockback pulse to nearby rival heads
        for (other in snakes) {
            if (other === player || !other.isAlive) continue
            if (mode == GameMode.TEAM_BATTLE && other.team == player.team) continue
            val dx = other.x - player.x
            val dy = other.y - player.y
            val dist = hypot(dx, dy)
            if (dist in 1f..220f) {
                val pushAngle = atan2(dy, dx)
                other.targetAngle = pushAngle
                other.x += cos(pushAngle) * 28f
                other.y += sin(pushAngle) * 28f
            }
        }
        return true
    }

    fun step(rawDt: Float): ArenaHudState? {
        if (isEnded) return buildHudState()
        val dt = rawDt.coerceIn(0.001f, 0.05f)
        worldTime += dt

        val difficultyTier = (1 + (worldTime / 30f).toInt()).coerceAtMost(6)

        // Check timed modes
        val modeDuration = mode.durationSec
        if (modeDuration != null && worldTime >= modeDuration) {
            finishTimedMatch()
            return buildHudState()
        }

        // 1. Update player input
        if (player.isAlive) {
            if (hasSteerInput) {
                player.targetAngle = atan2(steerY, steerX)
            }
            val canBoost = (playerBoostPressed || player.surgeTimer > 0f) &&
                (player.boostEnergy > 4f || player.surgeTimer > 0f)
            player.isBoosting = canBoost
        }

        // 2. Update AI decisions and move all snakes
        for (snake in snakes) {
            if (!snake.isAlive) continue
            if (snake.spawnInvulnTimer > 0f) snake.spawnInvulnTimer = max(0f, snake.spawnInvulnTimer - dt)
            if (snake.shieldTimer > 0f) snake.shieldTimer = max(0f, snake.shieldTimer - dt)
            if (snake.magnetTimer > 0f) snake.magnetTimer = max(0f, snake.magnetTimer - dt)
            if (snake.surgeTimer > 0f) snake.surgeTimer = max(0f, snake.surgeTimer - dt)

            if (!snake.isPlayer) {
                updateBotAi(snake, dt, difficultyTier)
            }

            moveSnake(snake, dt, difficultyTier)
        }

        // 3. Magnet attraction & Orb pickup
        updateOrbs(dt)

        // 4. Collisions (boundary + snake-to-snake)
        handleCollisions()

        // 5. Respawn bots & orbs to keep arena vibrant
        maintainPopulation(dt, difficultyTier)

        // 6. Update particles & popups
        updateEffects(dt)

        // 7. Smooth camera tracking
        val targetZoom = (1.02f - min(0.32f, (player.segments.size - 14) * 0.0042f)).coerceIn(0.68f, 1.05f)
        camX += (player.x - camX) * min(1f, dt * 8.5f)
        camY += (player.y - camY) * min(1f, dt * 8.5f)
        camZoom += (targetZoom - camZoom) * min(1f, dt * 3.0f)

        hudAccum += dt
        return if (hudAccum >= 0.09f || isEnded) {
            hudAccum = 0f
            buildHudState()
        } else {
            null
        }
    }

    private fun moveSnake(snake: SnakeEntity, dt: Float, difficultyTier: Int) {
        val baseTurnRate = if (snake.isPlayer) {
            4.9f * joystickSensitivity.coerceIn(0.7f, 1.5f)
        } else {
            3.6f + difficultyTier * 0.22f
        }

        val angleDiff = normalizeAngle(snake.targetAngle - snake.angle)
        val maxStep = baseTurnRate * dt
        snake.angle = if (kotlin.math.abs(angleDiff) <= maxStep) {
            snake.targetAngle
        } else {
            normalizeAngle(snake.angle + kotlin.math.sign(angleDiff) * maxStep)
        }

        val tierBonus = if (snake.isPlayer) 0f else difficultyTier * 7f
        val baseSpeed = 205f + tierBonus
        val speed = when {
            snake.surgeTimer > 0f -> baseSpeed * 1.85f
            snake.isBoosting && snake.boostEnergy > 2f -> {
                val drainScale = if (snake.isPlayer) (1f - (boostLevel - 1) * 0.12f).coerceAtLeast(0.45f) else 1f
                snake.boostEnergy = max(0f, snake.boostEnergy - 24f * drainScale * dt)
                if (snake.boostEnergy <= 1f) snake.isBoosting = false
                if (Random.nextFloat() < 0.45f && particles.size < 160) {
                    particles.add(
                        Particle(
                            x = snake.x - cos(snake.angle) * snake.bodyRadius,
                            y = snake.y - sin(snake.angle) * snake.bodyRadius,
                            vx = (Random.nextFloat() - 0.5f) * 45f,
                            vy = (Random.nextFloat() - 0.5f) * 45f,
                            radius = Random.nextFloat() * 4f + 3f,
                            life = 0.28f,
                            maxLife = 0.28f,
                            color = snake.skin.secondaryColor
                        )
                    )
                }
                baseSpeed * 1.72f
            }
            else -> {
                snake.isBoosting = false
                snake.boostEnergy = min(100f, snake.boostEnergy + 14f * dt)
                baseSpeed
            }
        }

        snake.x += cos(snake.angle) * speed * dt
        snake.y += sin(snake.angle) * speed * dt

        // Record spine point when moved enough
        val last = snake.trail.firstOrNull()
        if (last == null || hypot(snake.x - last.x, snake.y - last.y) >= 6f) {
            snake.trail.add(0, Vec2(snake.x, snake.y))
        } else {
            snake.trail[0] = Vec2(snake.x, snake.y)
        }
        snake.rebuildSegments()
    }

    private fun updateBotAi(bot: SnakeEntity, dt: Float, difficultyTier: Int) {
        bot.aiThinkTimer -= dt
        val distFromCenter = hypot(bot.x, bot.y)

        // Immediate arena boundary avoidance
        if (distFromCenter > arenaRadius - 230f) {
            val inwardAngle = atan2(-bot.y, -bot.x)
            bot.targetAngle = inwardAngle
            bot.isBoosting = false
            return
        }

        // Raycast obstacle check against other snake bodies
        var dangerDetected = false
        var bestClearAngle = bot.angle
        val lookAhead = 155f + difficultyTier * 10f

        if (isPointDangerous(bot, bot.x + cos(bot.angle) * lookAhead, bot.y + sin(bot.angle) * lookAhead)) {
            dangerDetected = true
            val leftAngle = normalizeAngle(bot.angle - 0.85f)
            val rightAngle = normalizeAngle(bot.angle + 0.85f)
            val leftDanger = isPointDangerous(bot, bot.x + cos(leftAngle) * lookAhead, bot.y + sin(leftAngle) * lookAhead)
            val rightDanger = isPointDangerous(bot, bot.x + cos(rightAngle) * lookAhead, bot.y + sin(rightAngle) * lookAhead)
            bestClearAngle = when {
                !leftDanger && rightDanger -> leftAngle
                !rightDanger && leftDanger -> rightAngle
                else -> normalizeAngle(bot.angle + if (bot.id % 2 == 0) 1.45f else -1.45f)
            }
        }

        if (dangerDetected) {
            bot.targetAngle = bestClearAngle
            bot.isBoosting = bot.aiAggression > 0.55f && bot.boostEnergy > 35f
            return
        }

        if (bot.aiThinkTimer <= 0f) {
            bot.aiThinkTimer = 0.24f + Random.nextFloat() * 0.28f

            // Look for valuable nearby orb
            var bestOrb: ArenaOrb? = null
            var bestScore = -1f
            for (i in orbs.indices) {
                val orb = orbs[i]
                val dx = orb.x - bot.x
                val dy = orb.y - bot.y
                val dSq = dx * dx + dy * dy
                if (dSq > 360f * 360f) continue
                val priority = (if (orb.isLoot) 3.2f else 1f) * orb.scoreValue / (sqrt(dSq) + 30f)
                if (priority > bestScore) {
                    bestScore = priority
                    bestOrb = orb
                }
            }

            if (bestOrb != null) {
                bot.targetAngle = atan2(bestOrb.y - bot.y, bestOrb.x - bot.x)
                bot.isBoosting = bestOrb.isLoot && bot.boostEnergy > 50f && difficultyTier >= 2
            } else {
                bot.aiWanderAngle = normalizeAngle(bot.aiWanderAngle + (Random.nextFloat() - 0.5f) * 1.1f)
                bot.targetAngle = bot.aiWanderAngle
                bot.isBoosting = false
            }
        }
    }

    private fun isPointDangerous(self: SnakeEntity, px: Float, py: Float): Boolean {
        if (hypot(px, py) > arenaRadius - 90f) return true
        val checkRadiusSq = 54f * 54f
        for (other in snakes) {
            if (!other.isAlive || other === self) continue
            if (mode == GameMode.TEAM_BATTLE && other.team == self.team) continue
            if (px < other.minX - 40f || px > other.maxX + 40f || py < other.minY - 40f || py > other.maxY + 40f) {
                continue
            }
            val step = if (other.segments.size > 40) 2 else 1
            var idx = 0
            while (idx < other.segments.size) {
                val seg = other.segments[idx]
                val dx = px - seg.x
                val dy = py - seg.y
                if (dx * dx + dy * dy < checkRadiusSq) return true
                idx += step
            }
        }
        return false
    }

    private fun updateOrbs(dt: Float) {
        val magnetRadius = 240f + (magnetLevel - 1) * 30f
        val magnetRadiusSq = magnetRadius * magnetRadius
        val iterator = orbs.iterator()

        while (iterator.hasNext()) {
            val orb = iterator.next()

            // Magnet pull for player (or any snake with magnet)
            if (player.isAlive && player.magnetTimer > 0f) {
                val dx = player.x - orb.x
                val dy = player.y - orb.y
                val dSq = dx * dx + dy * dy
                if (dSq in 1f..magnetRadiusSq) {
                    val d = sqrt(dSq)
                    val pullSpeed = 480f * dt
                    orb.x += (dx / d) * pullSpeed
                    orb.y += (dy / d) * pullSpeed
                }
            }

            var consumed = false
            for (snake in snakes) {
                if (!snake.isAlive) continue
                val pickupR = snake.bodyRadius + orb.radius + 8f
                val dx = snake.x - orb.x
                val dy = snake.y - orb.y
                if (dx * dx + dy * dy <= pickupR * pickupR) {
                    applyOrbToSnake(snake, orb)
                    consumed = true
                    break
                }
            }
            if (consumed) {
                iterator.remove()
            }
        }
    }

    private fun applyOrbToSnake(snake: SnakeEntity, orb: ArenaOrb) {
        val multiplier = when {
            snake.surgeTimer > 0f -> 2
            mode == GameMode.TIME_CHALLENGE -> 2
            else -> 1
        }
        val pts = orb.scoreValue * multiplier
        snake.score += pts
        snake.growthAccumulator += orb.growthValue
        while (snake.growthAccumulator >= 1f) {
            snake.growthAccumulator -= 1f
            snake.desiredSegments = (snake.desiredSegments + 1).coerceAtMost(220)
        }
        snake.boostEnergy = min(100f, snake.boostEnergy + 6f)

        if (snake.isPlayer) {
            playerFoodEaten++
            abilityCharge = min(100f, abilityCharge + when (orb.kind) {
                OrbKind.GEM -> 14f
                OrbKind.COIN -> 8f
                OrbKind.LOOT -> 7f
                else -> 4.5f
            })

            when (orb.kind) {
                OrbKind.COIN -> {
                    playerCoinsEarned += 1
                    audioManager.play(SoundEffect.PICKUP_COIN)
                    addPopup(orb.x, orb.y - 16f, "+1 Coin", Color(0xFFFFD166))
                }
                OrbKind.GEM -> {
                    playerGemsEarned += 1
                    audioManager.play(SoundEffect.PICKUP_GEM)
                    addPopup(orb.x, orb.y - 18f, "+1 Gem", Color(0xFFC07BFF))
                }
                OrbKind.POWER_MAGNET -> {
                    snake.magnetTimer = max(snake.magnetTimer, PowerUpType.MAGNET.durationSec + (magnetLevel - 1) * 1.5f)
                    audioManager.play(SoundEffect.POWER_UP)
                    addPopup(orb.x, orb.y - 20f, "MAGNET!", PowerUpType.MAGNET.color)
                }
                OrbKind.POWER_SHIELD -> {
                    snake.shieldTimer = max(snake.shieldTimer, PowerUpType.SHIELD.durationSec + (shieldLevel - 1) * 1.2f)
                    audioManager.play(SoundEffect.POWER_UP)
                    addPopup(orb.x, orb.y - 20f, "SHIELD!", PowerUpType.SHIELD.color)
                }
                OrbKind.POWER_SURGE -> {
                    snake.surgeTimer = max(snake.surgeTimer, PowerUpType.SPEED_SURGE.durationSec)
                    audioManager.play(SoundEffect.POWER_UP)
                    addPopup(orb.x, orb.y - 20f, "TURBO 2X!", PowerUpType.SPEED_SURGE.color)
                }
                else -> {
                    audioManager.play(SoundEffect.EAT_FRUIT)
                    if (orb.isLoot) {
                        addPopup(orb.x, orb.y - 14f, "+$pts", orb.color)
                    }
                }
            }
            spawnBurst(orb.x, orb.y, orb.color, 6)
        } else {
            when (orb.kind) {
                OrbKind.POWER_MAGNET -> snake.magnetTimer = max(snake.magnetTimer, 6f)
                OrbKind.POWER_SHIELD -> snake.shieldTimer = max(snake.shieldTimer, 5f)
                OrbKind.POWER_SURGE -> snake.surgeTimer = max(snake.surgeTimer, 5f)
                else -> {}
            }
        }
    }

    private fun handleCollisions() {
        for (snake in snakes) {
            if (!snake.isAlive) continue

            // 1. Arena wall check
            val distFromCenter = hypot(snake.x, snake.y)
            if (distFromCenter + snake.bodyRadius * 0.7f >= arenaRadius) {
                if (snake.shieldTimer > 0f) {
                    // Bounce safely inward if shield is active
                    snake.shieldTimer = 0f
                    val inward = atan2(-snake.y, -snake.x)
                    snake.angle = inward
                    snake.targetAngle = inward
                    val clamped = arenaRadius - snake.bodyRadius - 24f
                    snake.x = (snake.x / distFromCenter) * clamped
                    snake.y = (snake.y / distFromCenter) * clamped
                    if (snake.isPlayer) {
                        audioManager.play(SoundEffect.SHIELD_BLOCK)
                        onHapticEvent()
                        addPopup(snake.x, snake.y - 24f, "SHIELD SAVED!", Color(0xFF2EE59D))
                    }
                } else {
                    defeatSnake(snake, killer = null, reason = "Crashed into the Arena Barrier")
                    continue
                }
            }

            if (snake.spawnInvulnTimer > 0f) continue

            // 2. Snake head vs other snake bodies
            for (other in snakes) {
                if (!other.isAlive || other === snake) continue
                if (other.spawnInvulnTimer > 0f) continue
                if (mode == GameMode.TEAM_BATTLE && snake.team == other.team) continue

                // Broad-phase AABB check
                val hitMargin = snake.bodyRadius + other.bodyRadius
                if (snake.x < other.minX - hitMargin || snake.x > other.maxX + hitMargin ||
                    snake.y < other.minY - hitMargin || snake.y > other.maxY + hitMargin
                ) {
                    continue
                }

                // Skip segment 0 only when checking head-to-head tie below
                var collided = false
                for (i in other.segments.indices) {
                    val seg = other.segments[i]
                    val segScale = if (i == other.segments.lastIndex) 0.7f else 0.9f
                    val r = snake.bodyRadius * 0.72f + other.bodyRadius * segScale
                    val dx = snake.x - seg.x
                    val dy = snake.y - seg.y
                    if (dx * dx + dy * dy < r * r) {
                        collided = true
                        break
                    }
                }

                if (collided) {
                    if (snake.shieldTimer > 0f) {
                        snake.shieldTimer = 0f
                        snake.spawnInvulnTimer = 0.9f
                        val push = atan2(snake.y - other.y, snake.x - other.x)
                        snake.x += cos(push) * 34f
                        snake.y += sin(push) * 34f
                        if (snake.isPlayer) {
                            audioManager.play(SoundEffect.SHIELD_BLOCK)
                            onHapticEvent()
                            spawnBurst(snake.x, snake.y, Color(0xFF2EE59D), 16)
                            addPopup(snake.x, snake.y - 24f, "SHIELD BLOCKED!", Color(0xFF2EE59D))
                        }
                    } else {
                        defeatSnake(snake, killer = other, reason = "Defeated by ${other.name}")
                    }
                    break
                }
            }
        }
    }

    private fun defeatSnake(victim: SnakeEntity, killer: SnakeEntity?, reason: String) {
        if (!victim.isAlive) return
        victim.isAlive = false
        spawnBurst(victim.x, victim.y, victim.skin.primaryColor, 26)

        // Drop juicy loot orbs & coins along victim's body!
        for (i in victim.segments.indices step 2) {
            val seg = victim.segments[i]
            val jitterX = (Random.nextFloat() - 0.5f) * 20f
            val jitterY = (Random.nextFloat() - 0.5f) * 20f
            val isCoin = i % 6 == 0
            orbs.add(
                ArenaOrb(
                    id = nextOrbId++,
                    x = seg.x + jitterX,
                    y = seg.y + jitterY,
                    kind = if (isCoin) OrbKind.COIN else OrbKind.LOOT,
                    radius = if (isCoin) 13f else 12f,
                    scoreValue = if (isCoin) 30 else 25,
                    growthValue = 0.65f,
                    color = if (isCoin) Color(0xFFFFD166) else victim.skin.primaryColor,
                    bobPhase = Random.nextFloat() * 6.28f,
                    isLoot = true
                )
            )
        }

        if (killer != null) {
            killer.kills += 1
            killer.score += 120
            if (killer.isPlayer) {
                playerCoinsEarned += 8
                abilityCharge = min(100f, abilityCharge + 28f)
                audioManager.play(SoundEffect.KILL_ENEMY)
                onHapticEvent()
                addPopup(victim.x, victim.y - 24f, "KO ${victim.name}! +120", Color(0xFFFFD166))
            }
        }

        if (victim.isPlayer) {
            audioManager.play(SoundEffect.GAME_OVER)
            onHapticEvent()
            endMatch(victory = false, reason = reason)
        }
    }

    fun triggerPlayerDefeat(reason: String) {
        if (isEnded) return
        if (player.isAlive) {
            defeatSnake(player, killer = null, reason = reason)
        } else {
            endMatch(victory = false, reason = reason)
        }
    }

    private fun finishTimedMatch() {
        val blueScore = snakes.filter { it.isAlive && it.team == Team.BLUE }.sumOf { it.score }
        val crimsonScore = snakes.filter { it.isAlive && it.team == Team.CRIMSON }.sumOf { it.score }
        val won = if (mode == GameMode.TEAM_BATTLE) blueScore >= crimsonScore else true
        if (won) {
            playerCoinsEarned += 60
            playerGemsEarned += 2
        }
        audioManager.play(SoundEffect.REWARD_CLAIM)
        endMatch(
            victory = won,
            reason = if (mode == GameMode.TEAM_BATTLE) {
                if (won) "Blue Legends Victory!" else "Crimson Raiders Won"
            } else {
                "Time Challenge Complete!"
            },
            blueScore = blueScore,
            crimsonScore = crimsonScore
        )
    }

    private fun endMatch(
        victory: Boolean,
        reason: String,
        blueScore: Int = 0,
        crimsonScore: Int = 0
    ) {
        if (isEnded) return
        isEnded = true
        val elapsed = worldTime.toInt().coerceAtLeast(1)
        val finalBlue = if (blueScore > 0) blueScore else snakes.filter { it.team == Team.BLUE }.sumOf { it.score }
        val finalCrimson = if (crimsonScore > 0) crimsonScore else snakes.filter { it.team == Team.CRIMSON }.sumOf { it.score }
        matchSummary = MatchSummary(
            mode = mode,
            score = player.score,
            length = player.segments.size,
            kills = player.kills,
            coinsEarned = playerCoinsEarned + (player.score / 80),
            gemsEarned = playerGemsEarned,
            foodEaten = playerFoodEaten,
            durationSec = elapsed,
            victory = victory,
            deathReason = reason,
            blueTeamScore = finalBlue,
            crimsonTeamScore = finalCrimson
        )
    }

    private fun maintainPopulation(dt: Float, difficultyTier: Int) {
        snakes.removeAll { !it.isAlive && !it.isPlayer }

        botRespawnTimer -= dt
        val desiredTotalBots = mode.botCount
        val currentBots = snakes.count { !it.isPlayer }
        if (botRespawnTimer <= 0f && currentBots < desiredTotalBots) {
            botRespawnTimer = if (mode == GameMode.TIME_CHALLENGE) 0.6f else 1.2f
            val team = if (mode == GameMode.TEAM_BATTLE) {
                val blueCount = snakes.count { !it.isPlayer && it.team == Team.BLUE }
                if (blueCount < 4) Team.BLUE else Team.CRIMSON
            } else {
                Team.NONE
            }
            val startLen = (12 + difficultyTier * 3 + Random.nextInt(0, 14)).coerceAtMost(48)
            spawnBot(team = team, initialScale = startLen)
        }

        val targetOrbs = when (mode) {
            GameMode.ENDLESS_ARENA -> 340
            GameMode.TIME_CHALLENGE -> 300
            else -> 260
        }
        var spawnCount = 0
        while (orbs.size < targetOrbs && spawnCount < 10) {
            spawnRandomOrb()
            spawnCount++
        }

        powerOrbTimer -= dt
        if (powerOrbTimer <= 0f) {
            powerOrbTimer = if (mode == GameMode.TIME_CHALLENGE) 7f else 10f
            if (orbs.count {
                    it.kind == OrbKind.POWER_MAGNET || it.kind == OrbKind.POWER_SHIELD || it.kind == OrbKind.POWER_SURGE
                } < 6
            ) {
                spawnPowerOrb()
            }
        }
    }

    private fun spawnBot(team: Team, initialScale: Int) {
        var sx = 0f
        var sy = 0f
        repeat(10) {
            val r = Random.nextFloat() * (arenaRadius - 320f) + 180f
            val theta = Random.nextFloat() * 2f * PI.toFloat()
            val tx = cos(theta) * r
            val ty = sin(theta) * r
            if (hypot(tx - player.x, ty - player.y) > 420f) {
                sx = tx
                sy = ty
                return@repeat
            }
            sx = tx
            sy = ty
        }
        val skinPool = SnakeCatalog.skins
        val chosenSkin = skinPool[Random.nextInt(skinPool.size)]
        val botName = botNames[Random.nextInt(botNames.size)]
        snakes.add(
            SnakeEntity(
                id = nextBotId++,
                name = botName,
                isPlayer = false,
                skin = chosenSkin,
                team = team,
                x = sx,
                y = sy,
                angle = Random.nextFloat() * 2f * PI.toFloat(),
                initialLength = initialScale
            )
        )
    }

    private fun spawnRandomOrb() {
        val r = sqrt(Random.nextFloat()) * (arenaRadius - 100f)
        val theta = Random.nextFloat() * 2f * PI.toFloat()
        val x = cos(theta) * r
        val y = sin(theta) * r

        val roll = Random.nextFloat()
        val gemThreshold = if (mode == GameMode.ENDLESS_ARENA || mode == GameMode.TIME_CHALLENGE) 0.045f else 0.025f
        val coinThreshold = gemThreshold + if (mode == GameMode.TIME_CHALLENGE) 0.14f else 0.09f

        val kind: OrbKind
        val radius: Float
        val score: Int
        val growth: Float
        val color: Color

        when {
            roll < gemThreshold -> {
                kind = OrbKind.GEM
                radius = 13f
                score = 50
                growth = 1.4f
                color = Color(0xFFC07BFF)
            }
            roll < coinThreshold -> {
                kind = OrbKind.COIN
                radius = 12f
                score = 25
                growth = 0.6f
                color = Color(0xFFFFD166)
            }
            else -> {
                when (Random.nextInt(4)) {
                    0 -> {
                        kind = OrbKind.APPLE
                        radius = 10.5f
                        score = 12
                        growth = 0.45f
                        color = Color(0xFFFF5E7E)
                    }
                    1 -> {
                        kind = OrbKind.BERRY
                        radius = 10f
                        score = 12
                        growth = 0.45f
                        color = Color(0xFF4CC9F0)
                    }
                    2 -> {
                        kind = OrbKind.ORANGE
                        radius = 10.5f
                        score = 14
                        growth = 0.5f
                        color = Color(0xFFFF9F43)
                    }
                    else -> {
                        kind = OrbKind.STARFRUIT
                        radius = 11.5f
                        score = 18
                        growth = 0.65f
                        color = Color(0xFFA3F77B)
                    }
                }
            }
        }

        orbs.add(
            ArenaOrb(
                id = nextOrbId++,
                x = x,
                y = y,
                kind = kind,
                radius = radius,
                scoreValue = score,
                growthValue = growth,
                color = color,
                bobPhase = Random.nextFloat() * 6.28f
            )
        )
    }

    private fun spawnPowerOrb() {
        val r = sqrt(Random.nextFloat()) * (arenaRadius - 260f)
        val theta = Random.nextFloat() * 2f * PI.toFloat()
        val x = cos(theta) * r
        val y = sin(theta) * r
        val kind = when (Random.nextInt(3)) {
            0 -> OrbKind.POWER_MAGNET
            1 -> OrbKind.POWER_SHIELD
            else -> OrbKind.POWER_SURGE
        }
        val color = when (kind) {
            OrbKind.POWER_MAGNET -> PowerUpType.MAGNET.color
            OrbKind.POWER_SHIELD -> PowerUpType.SHIELD.color
            else -> PowerUpType.SPEED_SURGE.color
        }
        orbs.add(
            ArenaOrb(
                id = nextOrbId++,
                x = x,
                y = y,
                kind = kind,
                radius = 18f,
                scoreValue = 35,
                growthValue = 0.5f,
                color = color,
                bobPhase = Random.nextFloat() * 6.28f
            )
        )
    }

    private fun spawnBurst(x: Float, y: Float, color: Color, count: Int) {
        if (particles.size > 180) return
        repeat(count) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val spd = 65f + Random.nextFloat() * 190f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * spd,
                    vy = sin(angle) * spd,
                    radius = 3.5f + Random.nextFloat() * 4.5f,
                    life = 0.45f,
                    maxLife = 0.45f,
                    color = color
                )
            )
        }
    }

    private fun addPopup(x: Float, y: Float, text: String, color: Color) {
        if (popups.size > 14) popups.removeAt(0)
        popups.add(FloatingPopup(x = x, y = y, text = text, color = color, life = 0.95f))
    }

    private fun updateEffects(dt: Float) {
        val pIt = particles.iterator()
        while (pIt.hasNext()) {
            val p = pIt.next()
            p.life -= dt
            if (p.life <= 0f) {
                pIt.remove()
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vx *= 0.94f
                p.vy *= 0.94f
            }
        }
        val tIt = popups.iterator()
        while (tIt.hasNext()) {
            val f = tIt.next()
            f.life -= dt
            f.y -= 42f * dt
            if (f.life <= 0f) tIt.remove()
        }
    }

    fun buildHudState(): ArenaHudState {
        val sorted = snakes.filter { it.isAlive }.sortedByDescending { it.score }
        val rank = (sorted.indexOfFirst { it.isPlayer } + 1).let { if (it <= 0) sorted.size + 1 else it }
        val remaining = mode.durationSec?.let { max(0, it - worldTime.toInt()) }
        val tier = (1 + (worldTime / 30f).toInt()).coerceAtMost(6)
        val blueScore = snakes.filter { it.isAlive && it.team == Team.BLUE }.sumOf { it.score }
        val crimsonScore = snakes.filter { it.isAlive && it.team == Team.CRIMSON }.sumOf { it.score }

        return ArenaHudState(
            score = player.score,
            length = player.segments.size,
            kills = player.kills,
            coinsEarned = playerCoinsEarned,
            gemsEarned = playerGemsEarned,
            remainingTimeSec = remaining,
            elapsedTimeSec = worldTime.toInt(),
            difficultyTier = tier,
            boostEnergy = player.boostEnergy,
            abilityCharge = abilityCharge,
            magnetTimeLeft = player.magnetTimer,
            shieldTimeLeft = player.shieldTimer,
            surgeTimeLeft = player.surgeTimer,
            blueTeamScore = blueScore,
            crimsonTeamScore = crimsonScore,
            topSnakes = sorted.take(4).map { LeaderboardRow(it.name, it.score, it.isPlayer, it.team) },
            playerRank = rank,
            totalSnakes = sorted.size.coerceAtLeast(1),
            isGameOver = isEnded,
            matchSummary = matchSummary
        )
    }

    private fun normalizeAngle(a: Float): Float {
        var r = a
        while (r > PI.toFloat()) r -= (2f * PI.toFloat())
        while (r < -PI.toFloat()) r += (2f * PI.toFloat())
        return r
    }
}
