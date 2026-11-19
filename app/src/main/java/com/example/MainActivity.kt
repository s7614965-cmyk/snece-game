package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppScreen
import com.example.model.GameMode
import com.example.ui.GameViewModel
import com.example.ui.screens.GameModesScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.RewardsScreen
import com.example.ui.screens.SelectSnakeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.theme.ArenaDeepNavy
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldAction
import com.example.ui.theme.SnakeLegendsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnakeLegendsTheme {
                SnakeLegendsApp()
            }
        }
    }
}

@Composable
fun SnakeLegendsApp(
    viewModel: GameViewModel = viewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val claimedRewardPopup by viewModel.claimedRewardPopup.collectAsStateWithLifecycle()
    val isNewRecord by viewModel.lastNewRecord.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    val activeMode = remember(profile.selectedModeName) {
        GameMode.entries.firstOrNull { it.name == profile.selectedModeName } ?: GameMode.CLASSIC_SURVIVAL
    }
    val canClaimDaily = remember(profile.lastDailyClaimEpochDay) {
        viewModel.repository.canClaimDaily(profile)
    }
    val nextDayNumber = remember(profile.lastDailyClaimEpochDay, profile.dailyStreak) {
        viewModel.repository.nextDailyDay(profile)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = currentScreen,
            animationSpec = tween(durationMillis = 220),
            label = "screen_crossfade"
        ) { screen ->
            when (screen) {
                AppScreen.MAIN_MENU -> MainMenuScreen(
                    profile = profile,
                    canClaimDaily = canClaimDaily,
                    onPlayClicked = { viewModel.startPlay() },
                    onNavigate = { viewModel.navigateTo(it) }
                )

                AppScreen.GAME_MODES -> GameModesScreen(
                    profile = profile,
                    onSelectMode = { viewModel.selectMode(it) },
                    onPlayMode = { viewModel.startPlay(it) },
                    onBack = { viewModel.navigateBackToMenu() }
                )

                AppScreen.SELECT_SNAKE -> SelectSnakeScreen(
                    profile = profile,
                    onEquipOrBuy = { viewModel.equipOrBuySkin(it) },
                    onBack = { viewModel.navigateBackToMenu() }
                )

                AppScreen.SHOP -> ShopScreen(
                    profile = profile,
                    onBuyOrEquipSkin = { viewModel.equipOrBuySkin(it) },
                    onBuyUpgrade = { viewModel.buyUpgrade(it) },
                    onBack = { viewModel.navigateBackToMenu() }
                )

                AppScreen.REWARDS -> RewardsScreen(
                    profile = profile,
                    canClaimToday = canClaimDaily,
                    nextDayNumber = nextDayNumber,
                    claimedRewardPopup = claimedRewardPopup,
                    onClaimDaily = { viewModel.claimDailyReward() },
                    onDismissPopup = { viewModel.dismissClaimedRewardPopup() },
                    onBack = { viewModel.navigateBackToMenu() }
                )

                AppScreen.LEADERBOARD -> LeaderboardScreen(
                    profile = profile,
                    entries = leaderboard,
                    onBack = { viewModel.navigateBackToMenu() }
                )

                AppScreen.SETTINGS -> SettingsScreen(
                    profile = profile,
                    onUpdateName = { viewModel.updatePlayerName(it) },
                    onToggleSound = { viewModel.toggleSound(it) },
                    onToggleHaptics = { viewModel.toggleHaptics(it) },
                    onSensitivityChanged = { viewModel.setJoystickSensitivity(it) },
                    onResetStats = { viewModel.resetStats() },
                    onBack = { viewModel.navigateBackToMenu() }
                )

                AppScreen.PLAYING -> GameScreen(
                    profile = profile,
                    mode = activeMode,
                    audioManager = viewModel.audioManager,
                    isNewRecord = isNewRecord,
                    onMatchCompleted = { summary ->
                        viewModel.recordMatchCompletion(
                            mode = summary.mode,
                            score = summary.score,
                            length = summary.length,
                            kills = summary.kills,
                            coinsEarned = summary.coinsEarned,
                            gemsEarned = summary.gemsEarned,
                            foodEaten = summary.foodEaten,
                            durationSec = summary.durationSec
                        )
                    },
                    onToggleSound = { viewModel.toggleSound(it) },
                    onExitToMenu = { viewModel.navigateBackToMenu() }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = EmeraldAction,
                contentColor = ArenaDeepNavy,
                actionColor = CrispWhite
            )
        }
    }
}
