package com.example

import com.example.audio.GameAudioManager
import com.example.data.local.PlayerProfileEntity
import com.example.model.GameMode
import com.example.ui.BattleViewModel
import com.example.ui.CollisionType
import com.example.ui.SnakeDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI

@OptIn(ExperimentalCoroutinesApi::class)
class BattleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun coroutineGameLoopUpdatesSnakePositionAndFrameTick() = runTest(testDispatcher) {
        val vm = BattleViewModel()
        val audio = GameAudioManager().apply { soundEnabled = false }
        vm.startBattle(
            profile = PlayerProfileEntity(),
            mode = GameMode.CLASSIC_SURVIVAL,
            audioManager = audio,
            onHapticEvent = {}
        )

        val initialX = vm.engine!!.player.x
        val initialY = vm.engine!!.player.y
        vm.onTouchJoystickMove(1f, 0f, true)

        advanceTimeBy(200)
        runCurrent()

        assertTrue(vm.uiState.value.frameTick > 0L)
        assertTrue(vm.engine!!.player.x != initialX || vm.engine!!.player.y != initialY)
        vm.stopBattle()
    }

    @Test
    fun pauseAndResumeControlsCoroutineLoopUpdates() = runTest(testDispatcher) {
        val vm = BattleViewModel()
        val audio = GameAudioManager().apply { soundEnabled = false }
        vm.startBattle(
            profile = PlayerProfileEntity(),
            mode = GameMode.CLASSIC_SURVIVAL,
            audioManager = audio,
            onHapticEvent = {}
        )

        advanceTimeBy(64)
        runCurrent()
        vm.pauseGame()
        assertTrue(vm.uiState.value.isPaused)
        val pausedTick = vm.uiState.value.frameTick

        advanceTimeBy(160)
        runCurrent()
        assertEquals(pausedTick, vm.uiState.value.frameTick)

        vm.resumeGame()
        assertFalse(vm.uiState.value.isPaused)
        advanceTimeBy(80)
        runCurrent()
        assertTrue(vm.uiState.value.frameTick > pausedTick)

        vm.stopBattle()
        assertFalse(vm.uiState.value.isRunning)
    }

    @Test
    fun touchDirectionHandlersBlockInstant180DegreeReversal() = runTest(testDispatcher) {
        val vm = BattleViewModel()
        val audio = GameAudioManager().apply { soundEnabled = false }
        vm.startBattle(
            profile = PlayerProfileEntity(),
            mode = GameMode.CLASSIC_SURVIVAL,
            audioManager = audio,
            onHapticEvent = {}
        )

        val startAngle = vm.engine!!.player.angle
        assertEquals(-PI.toFloat() / 2f, startAngle, 0.01f)

        val reversedByJoystick = vm.onTouchJoystickMove(0f, 1f, true)
        assertFalse(reversedByJoystick)
        assertEquals(startAngle, vm.engine!!.player.targetAngle, 0.01f)

        val reversedBySwipe = vm.onTouchSwipeDirection(SnakeDirection.DOWN)
        assertFalse(reversedBySwipe)

        val turnedRight = vm.onTouchSwipeDirection(SnakeDirection.RIGHT)
        assertTrue(turnedRight)
        assertTrue(vm.uiState.value.isTouchSteering)
        assertEquals(SnakeDirection.RIGHT.angleRad, vm.engine!!.player.targetAngle, 0.01f)

        advanceTimeBy(450)
        runCurrent()

        vm.engine!!.player.angle = 0f
        val reversedByDrag = vm.onTouchDragSteer(-50f, 0f)
        assertFalse(reversedByDrag)

        vm.stopBattle()
    }

    @Test
    fun wallBoundaryCollisionEndsGameInViewModel() = runTest(testDispatcher) {
        val vm = BattleViewModel()
        val audio = GameAudioManager().apply { soundEnabled = false }
        vm.startBattle(
            profile = PlayerProfileEntity(),
            mode = GameMode.CLASSIC_SURVIVAL,
            audioManager = audio,
            onHapticEvent = {}
        )

        val engine = vm.engine!!
        engine.player.shieldTimer = 0f
        engine.player.x = engine.arenaRadius - 2f
        engine.player.y = 0f

        val outcome = vm.checkCollisions()
        assertEquals(CollisionType.WALL_BOUNDARY, outcome.type)
        assertFalse(engine.player.isAlive)
        assertTrue(vm.uiState.value.hud.isGameOver)
        assertFalse(vm.uiState.value.isRunning)
        assertEquals(CollisionType.WALL_BOUNDARY, vm.uiState.value.lastCollision)

        vm.stopBattle()
    }

    @Test
    fun selfBodySegmentCollisionEndsGameInViewModel() = runTest(testDispatcher) {
        val vm = BattleViewModel()
        val audio = GameAudioManager().apply { soundEnabled = false }
        vm.startBattle(
            profile = PlayerProfileEntity(),
            mode = GameMode.CLASSIC_SURVIVAL,
            audioManager = audio,
            onHapticEvent = {}
        )

        val engine = vm.engine!!
        engine.player.shieldTimer = 0f

        // Normal starting posture must not trigger false self-collision
        val safeOutcome = vm.checkCollisions()
        assertEquals(CollisionType.NONE, safeOutcome.type)
        assertFalse(vm.uiState.value.hud.isGameOver)

        // Curve tail segment (index >= 6) back onto the snake's head position
        val tailSeg = engine.player.segments[8]
        tailSeg.x = engine.player.x
        tailSeg.y = engine.player.y

        val selfOutcome = vm.checkCollisions()
        assertEquals(CollisionType.SELF_BODY, selfOutcome.type)
        assertFalse(engine.player.isAlive)
        assertTrue(vm.uiState.value.hud.isGameOver)
        assertFalse(vm.uiState.value.isRunning)
        assertEquals(CollisionType.SELF_BODY, vm.uiState.value.lastCollision)

        vm.stopBattle()
    }
}
