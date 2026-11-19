package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameAudioManager
import com.example.audio.SoundEffect
import com.example.data.GameRepository
import com.example.data.local.LeaderboardEntryEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.SnakeLegendsDatabase
import com.example.model.AppScreen
import com.example.model.DailyRewardTier
import com.example.model.GameMode
import com.example.model.UpgradeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SnakeLegendsDatabase.getInstance(application)
    val repository = GameRepository(database.gameDao())
    val audioManager = GameAudioManager()

    val profile: StateFlow<PlayerProfileEntity> = repository.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerProfileEntity())

    val leaderboard: StateFlow<List<LeaderboardEntryEntity>> = repository.leaderboardFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _claimedRewardPopup = MutableStateFlow<DailyRewardTier?>(null)
    val claimedRewardPopup: StateFlow<DailyRewardTier?> = _claimedRewardPopup.asStateFlow()

    private val _lastNewRecord = MutableStateFlow(false)
    val lastNewRecord: StateFlow<Boolean> = _lastNewRecord.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            profile.collect { p ->
                audioManager.soundEnabled = p.soundEnabled
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        audioManager.play(SoundEffect.BUTTON_TAP)
        _currentScreen.value = screen
    }

    fun navigateBackToMenu() {
        audioManager.play(SoundEffect.BUTTON_TAP)
        _currentScreen.value = AppScreen.MAIN_MENU
    }

    fun startPlay(mode: GameMode? = null) {
        audioManager.play(SoundEffect.BUTTON_TAP)
        _lastNewRecord.value = false
        if (mode != null) {
            viewModelScope.launch { repository.selectMode(mode) }
        }
        _currentScreen.value = AppScreen.PLAYING
    }

    fun selectMode(mode: GameMode) {
        audioManager.play(SoundEffect.BUTTON_TAP)
        viewModelScope.launch {
            repository.selectMode(mode)
        }
    }

    fun equipOrBuySkin(skinId: String) {
        viewModelScope.launch {
            val current = profile.value
            val unlocked = current.unlockedSkinSet()
            if (unlocked.contains(skinId)) {
                repository.equipSkin(skinId)
                audioManager.play(SoundEffect.BUTTON_TAP)
                _bannerMessage.value = "Equipped new snake!"
            } else {
                val ok = repository.purchaseSkin(skinId)
                if (ok) {
                    audioManager.play(SoundEffect.REWARD_CLAIM)
                    _bannerMessage.value = "Skin unlocked & equipped!"
                } else {
                    _bannerMessage.value = "Not enough Coins or Gems yet!"
                }
            }
        }
    }

    fun buyUpgrade(upgrade: UpgradeType) {
        viewModelScope.launch {
            val ok = repository.purchaseUpgrade(upgrade)
            if (ok) {
                audioManager.play(SoundEffect.REWARD_CLAIM)
                _bannerMessage.value = "${upgrade.title} upgraded!"
            } else {
                _bannerMessage.value = "Need more Coins or already at Max Level!"
            }
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val reward = repository.claimDailyReward()
            if (reward != null) {
                audioManager.play(SoundEffect.REWARD_CLAIM)
                _claimedRewardPopup.value = reward
            } else {
                _bannerMessage.value = "Today's reward already claimed! Come back tomorrow."
            }
        }
    }

    fun dismissClaimedRewardPopup() {
        _claimedRewardPopup.value = null
    }

    fun dismissBanner() {
        _bannerMessage.value = null
    }

    fun recordMatchCompletion(
        mode: GameMode,
        score: Int,
        length: Int,
        kills: Int,
        coinsEarned: Int,
        gemsEarned: Int,
        foodEaten: Int,
        durationSec: Int
    ) {
        viewModelScope.launch {
            val isNew = repository.recordMatchResult(
                mode = mode,
                score = score,
                length = length,
                kills = kills,
                coinsEarned = coinsEarned,
                gemsEarned = gemsEarned,
                foodEaten = foodEaten,
                durationSec = durationSec
            )
            _lastNewRecord.value = isNew
        }
    }

    fun updatePlayerName(name: String) {
        viewModelScope.launch {
            repository.updateSettings(playerName = name)
            _bannerMessage.value = "Player name updated!"
        }
    }

    fun toggleSound(enabled: Boolean) {
        audioManager.soundEnabled = enabled
        viewModelScope.launch {
            repository.updateSettings(soundEnabled = enabled)
        }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(hapticsEnabled = enabled)
        }
    }

    fun setJoystickSensitivity(value: Float) {
        viewModelScope.launch {
            repository.updateSettings(joystickSensitivity = value)
        }
    }

    fun resetStats() {
        viewModelScope.launch {
            repository.resetPlayerStats()
            _bannerMessage.value = "Player statistics cleared."
        }
    }
}
