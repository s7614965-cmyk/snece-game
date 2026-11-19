package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.audio.GameAudioManager
import com.example.data.local.PlayerProfileEntity
import com.example.game.ArenaHudState
import com.example.game.ArenaRenderer.renderArena
import com.example.game.MatchSummary
import com.example.game.Team
import com.example.model.GameMode
import com.example.ui.BattleViewModel
import com.example.ui.components.ArenaMiniMap
import com.example.ui.components.TouchJoystick
import com.example.ui.theme.ArenaBrightCyan
import com.example.ui.theme.ArenaDeepNavy
import com.example.ui.theme.ArenaRoyalBlue
import com.example.ui.theme.CoralStrike
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldAction
import com.example.ui.theme.SoftIceBlue
import com.example.ui.theme.SunburstGold
import com.example.ui.theme.TranslucentHud
import java.util.Locale

@Composable
fun GameScreen(
    profile: PlayerProfileEntity,
    mode: GameMode,
    audioManager: GameAudioManager,
    isNewRecord: Boolean,
    onMatchCompleted: (MatchSummary) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onExitToMenu: () -> Unit,
    battleViewModel: BattleViewModel = viewModel()
) {
    val view = LocalView.current
    val battleState by battleViewModel.uiState.collectAsStateWithLifecycle()
    val hudState = battleState.hud
    val isPaused = battleState.isPaused
    val frameTick = battleState.frameTick

    var gameSessionId by remember { mutableIntStateOf(0) }
    var hasRecordedResult by remember(gameSessionId) { mutableStateOf(false) }

    val pressedKeys = remember { mutableSetOf<Key>() }
    val focusRequester = remember { FocusRequester() }

    // Start the ViewModel coroutine game loop for each battle session
    LaunchedEffect(gameSessionId, mode, profile.equippedSkinId) {
        battleViewModel.startBattle(
            profile = profile,
            mode = mode,
            audioManager = audioManager,
            onHapticEvent = {
                if (profile.hapticsEnabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
            }
        )
        runCatching { focusRequester.requestFocus() }
    }

    // Stop the ViewModel coroutine loop when leaving GameScreen
    DisposableEffect(Unit) {
        onDispose {
            battleViewModel.stopBattle()
        }
    }

    // Record match result once when game ends
    LaunchedEffect(hudState.isGameOver) {
        val summary = hudState.matchSummary
        if (hudState.isGameOver && summary != null && !hasRecordedResult) {
            hasRecordedResult = true
            onMatchCompleted(summary)
        }
    }

    BackHandler {
        if (hudState.isGameOver) {
            onExitToMenu()
        } else {
            battleViewModel.togglePause()
        }
    }

    fun updateKeyboardSteer() {
        var kx = 0f
        var ky = 0f
        if (Key.A in pressedKeys || Key.DirectionLeft in pressedKeys) kx -= 1f
        if (Key.D in pressedKeys || Key.DirectionRight in pressedKeys) kx += 1f
        if (Key.W in pressedKeys || Key.DirectionUp in pressedKeys) ky -= 1f
        if (Key.S in pressedKeys || Key.DirectionDown in pressedKeys) ky += 1f
        if (kx != 0f || ky != 0f) {
            battleViewModel.setSteerInput(kx, ky, true)
        } else {
            battleViewModel.setSteerInput(0f, 0f, false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaDeepNavy)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        when (event.key) {
                            Key.W, Key.A, Key.S, Key.D,
                            Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight -> {
                                pressedKeys.add(event.key)
                                updateKeyboardSteer()
                                true
                            }
                            Key.Spacebar, Key.ShiftLeft, Key.ShiftRight -> {
                                battleViewModel.setBoostPressed(true)
                                true
                            }
                            Key.E, Key.Enter -> {
                                battleViewModel.activateSpecialAbility()
                                true
                            }
                            Key.P, Key.Escape -> {
                                battleViewModel.togglePause()
                                true
                            }
                            else -> false
                        }
                    }
                    KeyEventType.KeyUp -> {
                        when (event.key) {
                            Key.W, Key.A, Key.S, Key.D,
                            Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight -> {
                                pressedKeys.remove(event.key)
                                updateKeyboardSteer()
                                true
                            }
                            Key.Spacebar, Key.ShiftLeft, Key.ShiftRight -> {
                                battleViewModel.setBoostPressed(false)
                                true
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
    ) {
        // 1. Live 2D Arena Canvas subscribed to ViewModel's frameTick StateFlow + Touch drag steering
        val activeEngine = battleViewModel.engine
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var cumulativeDrag = Offset.Zero
                    detectDragGestures(
                        onDragStart = { cumulativeDrag = Offset.Zero },
                        onDragEnd = { battleViewModel.onTouchDragEnd() },
                        onDragCancel = { battleViewModel.onTouchDragEnd() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            cumulativeDrag += dragAmount
                            if (cumulativeDrag.getDistance() > 12f) {
                                battleViewModel.onTouchDragSteer(cumulativeDrag.x, cumulativeDrag.y)
                            }
                        }
                    )
                }
                .testTag("game_arena_canvas")
        ) {
            val _tick = frameTick
            if (activeEngine != null) {
                renderArena(activeEngine)
            }
        }

        // 2. Top Gameplay HUD (Safe Drawing Insets)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left HUD: Score, Length, Coins, Time / Mode
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HudStatPill(
                            label = "SCORE",
                            value = hudState.score.toString(),
                            accent = EmeraldAction
                        )
                        HudStatPill(
                            label = "LENGTH",
                            value = hudState.length.toString(),
                            accent = ArenaBrightCyan
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HudIconPill(
                            icon = Icons.Rounded.MonetizationOn,
                            value = "+${hudState.coinsEarned}",
                            tint = SunburstGold
                        )
                        HudIconPill(
                            icon = Icons.Rounded.Diamond,
                            value = "+${hudState.gemsEarned}",
                            tint = Color(0xFFC07BFF)
                        )
                        HudIconPill(
                            icon = Icons.Rounded.Timer,
                            value = formatMatchTimer(hudState),
                            tint = if ((hudState.remainingTimeSec ?: 99) <= 15) CoralStrike else CrispWhite
                        )
                    }

                    // Team Battle Scoreboard OR Difficulty Tier Pill
                    if (mode == GameMode.TEAM_BATTLE) {
                        Surface(
                            color = TranslucentHud,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "BLUE ${hudState.blueTeamScore}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArenaBrightCyan
                                )
                                Text("vs", style = MaterialTheme.typography.labelSmall, color = SoftIceBlue)
                                Text(
                                    text = "RED ${hudState.crimsonTeamScore}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CoralStrike
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = TranslucentHud,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Tier ${hudState.difficultyTier} • KO: ${hudState.kills}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftIceBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Active Power-Up Timers
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (hudState.shieldTimeLeft > 0f) {
                            ActiveBuffChip("SHIELD ${hudState.shieldTimeLeft.toInt() + 1}s", EmeraldAction)
                        }
                        if (hudState.magnetTimeLeft > 0f) {
                            ActiveBuffChip("MAGNET ${hudState.magnetTimeLeft.toInt() + 1}s", ArenaBrightCyan)
                        }
                        if (hudState.surgeTimeLeft > 0f) {
                            ActiveBuffChip("2X SURGE ${hudState.surgeTimeLeft.toInt() + 1}s", SunburstGold)
                        }
                    }
                }

                // Right HUD: Pause + Mini-Map + Live Enemy Leaderboard
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeEngine != null) {
                            ArenaMiniMap(engine = activeEngine, frameTick = frameTick)
                        }

                        FilledIconButton(
                            onClick = { battleViewModel.pauseGame() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = TranslucentHud,
                                contentColor = CrispWhite
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = stringResource(R.string.cd_pause_game)
                            )
                        }
                    }

                    // Enemy Leaderboard Card
                    Surface(
                        color = TranslucentHud,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.width(154.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            Text(
                                text = "ARENA TOP (#${hudState.playerRank}/${hudState.totalSnakes})",
                                style = MaterialTheme.typography.labelSmall,
                                color = SunburstGold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            hudState.topSnakes.forEachIndexed { idx, row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${row.name}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = when {
                                            row.isPlayer -> EmeraldAction
                                            row.team == Team.CRIMSON -> CoralStrike
                                            else -> CrispWhite
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${row.score}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (row.isPlayer) EmeraldAction else SoftIceBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Bottom Touch Controls (Joystick + Boost + Special Ability)
        if (!hudState.isGameOver && !isPaused) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                TouchJoystick(
                    onDirectionChanged = { dx, dy, active ->
                        battleViewModel.onTouchJoystickMove(dx, dy, active)
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Special Ability Button (Shield + Magnet Burst)
                    val isReady = hudState.abilityCharge >= 100f
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (isReady) {
                                    Brush.radialGradient(listOf(ArenaBrightCyan, Color(0xFF0077B6)))
                                } else {
                                    Brush.radialGradient(listOf(Color(0xAA163E7A), Color(0xAA0B1B3B)))
                                }
                            )
                            .border(
                                width = 2.5.dp,
                                color = if (isReady) CrispWhite else Color(0x5589B4F8),
                                shape = CircleShape
                            )
                            .testTag("special_ability_button")
                            .pointerInput(isReady) {
                                detectTapGestures(
                                    onTap = {
                                        battleViewModel.activateSpecialAbility()
                                    }
                                )
                            }
                    ) {
                        CircularProgressIndicator(
                            progress = { (hudState.abilityCharge / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.size(66.dp),
                            color = if (isReady) SunburstGold else ArenaBrightCyan,
                            trackColor = Color(0x33FFFFFF),
                            strokeWidth = 4.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = stringResource(R.string.cd_special_ability),
                                tint = if (isReady) CrispWhite else SoftIceBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (isReady) "BURST" else "${hudState.abilityCharge.toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = CrispWhite
                            )
                        }
                    }

                    // Hold-to-Boost Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFFB703), Color(0xFFE85D04))
                                )
                            )
                            .border(3.dp, CrispWhite, CircleShape)
                            .testTag("boost_button")
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        battleViewModel.setBoostPressed(true)
                                        tryAwaitRelease()
                                        battleViewModel.setBoostPressed(false)
                                    }
                                )
                            }
                    ) {
                        CircularProgressIndicator(
                            progress = { (hudState.boostEnergy / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.size(76.dp),
                            color = CrispWhite,
                            trackColor = Color(0x44000000),
                            strokeWidth = 4.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = stringResource(R.string.cd_boost),
                                tint = CrispWhite,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "BOOST",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = CrispWhite
                            )
                        }
                    }
                }
            }
        }

        // 4. Pause Overlay
        if (isPaused && !hudState.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB8050E24)),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ArenaRoyalBlue),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .padding(24.dp)
                            .widthIn(max = 400.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "GAME PAUSED",
                                style = MaterialTheme.typography.headlineMedium,
                                color = SunburstGold
                            )
                            Text(
                                text = "${mode.title} • Score ${hudState.score}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftIceBlue
                            )
                            Button(
                                onClick = { battleViewModel.resumeGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAction, contentColor = ArenaDeepNavy),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("resume_button")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("RESUME BATTLE", style = MaterialTheme.typography.labelLarge)
                            }
                            OutlinedButton(
                                onClick = { gameSessionId++ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("pause_restart_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("RESTART")
                            }
                            OutlinedButton(
                                onClick = { onToggleSound(!profile.soundEnabled) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("pause_sound_button")
                            ) {
                                Icon(
                                    if (profile.soundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (profile.soundEnabled) "SOUND: ON" else "SOUND: OFF")
                            }
                            OutlinedButton(
                                onClick = onExitToMenu,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralStrike),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("pause_menu_button")
                            ) {
                                Icon(Icons.Rounded.Home, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("EXIT TO MENU")
                            }
                        }
                    }
                }
            }
        }

        // 5. Game Over / Match Victory Summary Overlay
        val summary = hudState.matchSummary
        if (hudState.isGameOver && summary != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xC8050E24)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ArenaRoyalBlue),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .padding(20.dp)
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isNewRecord) {
                            Surface(
                                color = SunburstGold,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = ArenaDeepNavy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "NEW HIGH SCORE!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ArenaDeepNavy
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = if (summary.victory) SunburstGold else CoralStrike,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = if (summary.victory) "ARENA VICTORY!" else "BATTLE ENDED",
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (summary.victory) SunburstGold else CrispWhite
                        )
                        Text(
                            text = summary.deathReason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftIceBlue
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SummaryStatBox("SCORE", "${summary.score}", EmeraldAction)
                            SummaryStatBox("LENGTH", "${summary.length}", ArenaBrightCyan)
                            SummaryStatBox("KILLS", "${summary.kills}", CoralStrike)
                            SummaryStatBox("TIME", "${summary.durationSec}s", CrispWhite)
                        }

                        Surface(
                            color = Color(0x55081838),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.MonetizationOn, contentDescription = null, tint = SunburstGold)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "+${summary.coinsEarned} Coins",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SunburstGold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Diamond, contentDescription = null, tint = Color(0xFFC07BFF))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "+${summary.gemsEarned} Gems",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFC07BFF)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Button(
                            onClick = { gameSessionId++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldAction,
                                contentColor = ArenaDeepNavy
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("play_again_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("PLAY AGAIN", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = onExitToMenu,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("back_to_menu_button")
                        ) {
                            Icon(Icons.Rounded.Home, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("MAIN MENU")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudStatPill(label: String, value: String, accent: Color) {
    Surface(
        color = TranslucentHud,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SoftIceBlue)
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HudIconPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    tint: Color
) {
    Surface(
        color = TranslucentHud,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(text = value, style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}

@Composable
private fun ActiveBuffChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.25f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.border(1.dp, color, RoundedCornerShape(50))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CrispWhite,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SummaryStatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SoftIceBlue)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
    }
}

private fun formatMatchTimer(hud: ArenaHudState): String {
    val sec = hud.remainingTimeSec ?: hud.elapsedTimeSec
    val m = sec / 60
    val s = sec % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
