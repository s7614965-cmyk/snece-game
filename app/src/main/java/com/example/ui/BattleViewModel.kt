package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameAudioManager
import com.example.data.local.PlayerProfileEntity
import com.example.game.ArenaEngine
import com.example.game.ArenaHudState
import com.example.game.SnakeEntity
import com.example.model.GameMode
import com.example.model.SnakeCatalog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class SnakeDirection(val angleRad: Float) {
    RIGHT(0f),
    DOWN(PI.toFloat() / 2f),
    LEFT(PI.toFloat()),
    UP(-PI.toFloat() / 2f)
}

enum class CollisionType {
    NONE,
    WALL_BOUNDARY,
    SELF_BODY,
    ENEMY_BODY
}

data class CollisionOutcome(
    val type: CollisionType = CollisionType.NONE,
    val reason: String = "",
    val shieldAbsorbed: Boolean = false
)

data class BattleUiState(
    val hud: ArenaHudState = ArenaHudState(),
    val isPaused: Boolean = false,
    val frameTick: Long = 0L,
    val isRunning: Boolean = false,
    val targetDirectionAngleRad: Float? = null,
    val isTouchSteering: Boolean = false,
    val lastCollision: CollisionType = CollisionType.NONE
)

/**
 * ViewModel that drives the real-time 2D snake game loop via a coroutine on viewModelScope,
 * handles touch/keyboard directional inputs with anti-180-degree reversal protection,
 * runs collision detection (wall boundaries & self-body segments), and emits StateFlow updates.
 */
class BattleViewModel : ViewModel() {

    var engine: ArenaEngine? = null
        private set

    private val _uiState = MutableStateFlow(BattleUiState())
    val uiState: StateFlow<BattleUiState> = _uiState.asStateFlow()

    private var gameLoopJob: Job? = null
    private var selfShieldGraceTicks: Int = 0

    fun startBattle(
        profile: PlayerProfileEntity,
        mode: GameMode,
        audioManager: GameAudioManager,
        onHapticEvent: () -> Unit
    ) {
        gameLoopJob?.cancel()
        val skin = SnakeCatalog.byId(profile.equippedSkinId)
        val newEngine = ArenaEngine(
            mode = mode,
            playerName = profile.playerName,
            playerSkin = skin,
            magnetLevel = profile.magnetLevel,
            shieldLevel = profile.shieldLevel,
            boostLevel = profile.boostLevel,
            joystickSensitivity = profile.joystickSensitivity,
            audioManager = audioManager,
            onHapticEvent = onHapticEvent
        )
        engine = newEngine
        selfShieldGraceTicks = 0
        _uiState.value = BattleUiState(
            hud = newEngine.buildHudState(),
            isPaused = false,
            frameTick = 0L,
            isRunning = true,
            targetDirectionAngleRad = newEngine.player.angle,
            isTouchSteering = false,
            lastCollision = CollisionType.NONE
        )
        launchGameLoop()
    }

    private fun launchGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastNanos = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val rawDt = (now - lastNanos) / 1_000_000_000f
                lastNanos = now

                val activeEngine = engine
                val currentState = _uiState.value
                if (activeEngine != null && !currentState.isPaused && !currentState.hud.isGameOver) {
                    if (selfShieldGraceTicks > 0) selfShieldGraceTicks--
                    val steppedHud = activeEngine.step(rawDt)
                    val collision = checkCollisions()
                    val latestHud = if (collision.type != CollisionType.NONE && !collision.shieldAbsorbed) {
                        activeEngine.buildHudState()
                    } else {
                        steppedHud ?: currentState.hud
                    }

                    _uiState.update { state ->
                        state.copy(
                            hud = latestHud,
                            frameTick = state.frameTick + 1L,
                            isRunning = !latestHud.isGameOver,
                            lastCollision = if (collision.type != CollisionType.NONE) collision.type else state.lastCollision
                        )
                    }
                }
                delay(16L) // ~60 FPS tick interval
            }
        }
    }

    /**
     * Evaluates player collisions against the arena wall boundaries and the snake's own body segments.
     * Ends the game immediately if a lethal collision occurs.
     */
    fun checkCollisions(): CollisionOutcome {
        val activeEngine = engine ?: return CollisionOutcome()
        val player = activeEngine.player

        // 1. Check Arena Wall Boundary collision
        val wallOutcome = checkWallCollision(player, activeEngine.arenaRadius)
        if (wallOutcome.type == CollisionType.WALL_BOUNDARY) {
            if (!wallOutcome.shieldAbsorbed) {
                endGameFromCollision(CollisionType.WALL_BOUNDARY, wallOutcome.reason)
            }
            return wallOutcome
        }

        // 2. Check Self-Body Segment collision (skip head & immediate neck segments)
        val selfOutcome = checkSelfCollision(player)
        if (selfOutcome.type == CollisionType.SELF_BODY) {
            if (!selfOutcome.shieldAbsorbed) {
                endGameFromCollision(CollisionType.SELF_BODY, selfOutcome.reason)
            }
            return selfOutcome
        }

        // 3. Reflect enemy collision if ArenaEngine ended the match during step()
        if (!player.isAlive || activeEngine.buildHudState().isGameOver) {
            val hud = activeEngine.buildHudState()
            _uiState.update { it.copy(hud = hud, isRunning = false, lastCollision = CollisionType.ENEMY_BODY) }
            return CollisionOutcome(CollisionType.ENEMY_BODY, hud.matchSummary?.deathReason ?: "Defeated")
        }

        return CollisionOutcome()
    }

    private fun checkWallCollision(player: SnakeEntity, arenaRadius: Float): CollisionOutcome {
        val distFromCenter = hypot(player.x, player.y)
        val hitWall = distFromCenter + player.bodyRadius * 0.7f >= arenaRadius
        if (!hitWall) return CollisionOutcome()

        if (player.isAlive && player.shieldTimer > 0f && distFromCenter > 0.001f) {
            player.shieldTimer = 0f
            val inward = atan2(-player.y, -player.x)
            player.angle = inward
            player.targetAngle = inward
            val clamped = arenaRadius - player.bodyRadius - 24f
            player.x = (player.x / distFromCenter) * clamped
            player.y = (player.y / distFromCenter) * clamped
            return CollisionOutcome(
                type = CollisionType.WALL_BOUNDARY,
                reason = "Shield absorbed wall impact",
                shieldAbsorbed = true
            )
        }

        return CollisionOutcome(
            type = CollisionType.WALL_BOUNDARY,
            reason = "Crashed into the Arena Wall"
        )
    }

    private fun checkSelfCollision(player: SnakeEntity): CollisionOutcome {
        if (selfShieldGraceTicks > 0) return CollisionOutcome()
        val segments = player.segments
        if (segments.size <= SELF_COLLISION_START_INDEX) return CollisionOutcome()

        val hitRadius = player.bodyRadius * 1.1f
        val hitRadiusSq = hitRadius * hitRadius
        for (i in SELF_COLLISION_START_INDEX until segments.size) {
            val seg = segments[i]
            val dx = player.x - seg.x
            val dy = player.y - seg.y
            if (dx * dx + dy * dy < hitRadiusSq) {
                if (player.isAlive && player.shieldTimer > 0f) {
                    player.shieldTimer = 0f
                    selfShieldGraceTicks = 45
                    return CollisionOutcome(
                        type = CollisionType.SELF_BODY,
                        reason = "Shield absorbed self-collision",
                        shieldAbsorbed = true
                    )
                }
                return CollisionOutcome(
                    type = CollisionType.SELF_BODY,
                    reason = "Collided with your own body"
                )
            }
        }
        return CollisionOutcome()
    }

    private fun endGameFromCollision(type: CollisionType, reason: String) {
        val activeEngine = engine ?: return
        activeEngine.triggerPlayerDefeat(reason)
        val endedHud = activeEngine.buildHudState()
        _uiState.update { state ->
            state.copy(
                hud = endedHud,
                isRunning = false,
                isTouchSteering = false,
                lastCollision = type
            )
        }
    }

    /**
     * Touch-based virtual joystick handler.
     * Prevents instant 180-degree reversal into the snake's own neck by rejecting vectors
     * inside the rear reversal cone relative to the snake's current heading.
     */
    fun onTouchJoystickMove(dx: Float, dy: Float, active: Boolean): Boolean {
        val activeEngine = engine ?: return false
        if (!active || hypot(dx, dy) < TOUCH_DEADZONE) {
            activeEngine.clearSteerInput()
            _uiState.update { it.copy(isTouchSteering = false) }
            return false
        }
        return applySafeDirectionAngle(atan2(dy, dx), isTouch = true)
    }

    /**
     * Touch-based arena drag/swipe steering handler.
     * Ignores tiny jitter and blocks instant 180-degree reversals.
     */
    fun onTouchDragSteer(dragDx: Float, dragDy: Float): Boolean {
        if (hypot(dragDx, dragDy) < DRAG_MIN_THRESHOLD) return false
        return applySafeDirectionAngle(atan2(dragDy, dragDx), isTouch = true)
    }

    fun onTouchDragEnd() {
        engine?.clearSteerInput()
        _uiState.update { it.copy(isTouchSteering = false) }
    }

    /**
     * Cardinal touch swipe handler (UP, DOWN, LEFT, RIGHT).
     * Explicitly prevents reversing 180 degrees opposite to the snake's current heading.
     */
    fun onTouchSwipeDirection(direction: SnakeDirection): Boolean {
        return applySafeDirectionAngle(direction.angleRad, isTouch = true)
    }

    /**
     * Shared directional steering handler used by keyboard or touch inputs.
     */
    fun setSteerInput(dx: Float, dy: Float, active: Boolean) {
        onTouchJoystickMove(dx, dy, active)
    }

    private fun applySafeDirectionAngle(requestedAngleRad: Float, isTouch: Boolean): Boolean {
        val activeEngine = engine ?: return false
        if (_uiState.value.isPaused || _uiState.value.hud.isGameOver) return false

        val currentHeading = activeEngine.player.angle
        val angleDelta = abs(normalizeAngle(requestedAngleRad - currentHeading))

        if (angleDelta >= ArenaEngine.REVERSE_BLOCK_THRESHOLD_RAD) {
            return false
        }

        val accepted = activeEngine.setSteerAngle(requestedAngleRad, preventInstantReverse = true)
        if (accepted) {
            val normalized = normalizeAngle(requestedAngleRad)
            _uiState.update {
                it.copy(
                    targetDirectionAngleRad = normalized,
                    isTouchSteering = isTouch
                )
            }
        }
        return accepted
    }

    fun setBoostPressed(pressed: Boolean) {
        engine?.setBoostPressed(pressed)
    }

    fun activateSpecialAbility() {
        engine?.activateSpecialAbility()
    }

    fun pauseGame() {
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeGame() {
        _uiState.update { it.copy(isPaused = false) }
    }

    fun togglePause() {
        _uiState.update { state ->
            if (state.hud.isGameOver) state else state.copy(isPaused = !state.isPaused)
        }
    }

    fun stopBattle() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        _uiState.update { it.copy(isRunning = false, isTouchSteering = false) }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }

    private fun normalizeAngle(a: Float): Float {
        var r = a
        while (r > PI.toFloat()) r -= (2f * PI.toFloat())
        while (r < -PI.toFloat()) r += (2f * PI.toFloat())
        return r
    }

    companion object {
        private const val TOUCH_DEADZONE = 0.08f
        private const val DRAG_MIN_THRESHOLD = 4f
        private const val SELF_COLLISION_START_INDEX = 6
    }
}
