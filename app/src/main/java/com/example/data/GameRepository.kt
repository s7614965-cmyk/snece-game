package com.example.data

import com.example.data.local.GameDao
import com.example.data.local.LeaderboardEntryEntity
import com.example.data.local.PlayerProfileEntity
import com.example.model.DailyRewardSchedule
import com.example.model.DailyRewardTier
import com.example.model.GameMode
import com.example.model.SnakeCatalog
import com.example.model.UpgradeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class GameRepository(private val dao: GameDao) {

    val profileFlow: Flow<PlayerProfileEntity> = dao.observeProfile().map { it ?: PlayerProfileEntity() }

    val leaderboardFlow: Flow<List<LeaderboardEntryEntity>> = dao.observeTopLeaderboard()

    suspend fun ensureInitialized() {
        if (dao.getProfile() == null) {
            dao.upsertProfile(PlayerProfileEntity())
        }
        if (dao.getLeaderboardCount() == 0) {
            val now = System.currentTimeMillis()
            val rivals = listOf(
                LeaderboardEntryEntity(
                    playerName = "SapphireFang",
                    isPlayer = false,
                    modeName = GameMode.CLASSIC_SURVIVAL.name,
                    score = 2450,
                    length = 68,
                    kills = 9,
                    durationSec = 185,
                    skinId = "frost_wyrm",
                    timestampMillis = now - 86_400_000L
                ),
                LeaderboardEntryEntity(
                    playerName = "CrownViper",
                    isPlayer = false,
                    modeName = GameMode.ENDLESS_ARENA.name,
                    score = 1980,
                    length = 54,
                    kills = 7,
                    durationSec = 160,
                    skinId = "royal_viper",
                    timestampMillis = now - 72_000_000L
                ),
                LeaderboardEntryEntity(
                    playerName = "BlazeTail",
                    isPlayer = false,
                    modeName = GameMode.TIME_CHALLENGE.name,
                    score = 1520,
                    length = 44,
                    kills = 6,
                    durationSec = 120,
                    skinId = "coral_blaze",
                    timestampMillis = now - 45_000_000L
                ),
                LeaderboardEntryEntity(
                    playerName = "AstraKoi",
                    isPlayer = false,
                    modeName = GameMode.TEAM_BATTLE.name,
                    score = 1180,
                    length = 38,
                    kills = 5,
                    durationSec = 150,
                    skinId = "sakura_koi",
                    timestampMillis = now - 28_000_000L
                ),
                LeaderboardEntryEntity(
                    playerName = "NeonGlide",
                    isPlayer = false,
                    modeName = GameMode.CLASSIC_SURVIVAL.name,
                    score = 780,
                    length = 29,
                    kills = 3,
                    durationSec = 95,
                    skinId = "cyber_pulse",
                    timestampMillis = now - 14_000_000L
                )
            )
            dao.insertLeaderboardEntries(rivals)
        }
    }

    suspend fun selectMode(mode: GameMode) {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        dao.upsertProfile(current.copy(selectedModeName = mode.name))
    }

    suspend fun equipSkin(skinId: String): Boolean {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        if (!current.unlockedSkinSet().contains(skinId)) return false
        dao.upsertProfile(current.copy(equippedSkinId = skinId))
        return true
    }

    suspend fun purchaseSkin(skinId: String): Boolean {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        val skin = SnakeCatalog.byId(skinId)
        val unlocked = current.unlockedSkinSet()
        if (unlocked.contains(skin.id)) {
            dao.upsertProfile(current.copy(equippedSkinId = skin.id))
            return true
        }
        if (skin.coinPrice > 0 && current.coins < skin.coinPrice) return false
        if (skin.gemPrice > 0 && current.gems < skin.gemPrice) return false

        val newUnlocked = (unlocked + skin.id).joinToString(",")
        dao.upsertProfile(
            current.copy(
                coins = current.coins - skin.coinPrice,
                gems = current.gems - skin.gemPrice,
                unlockedSkinsCsv = newUnlocked,
                equippedSkinId = skin.id
            )
        )
        return true
    }

    suspend fun purchaseUpgrade(upgrade: UpgradeType): Boolean {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        val level = when (upgrade) {
            UpgradeType.MAGNET_DURATION -> current.magnetLevel
            UpgradeType.SHIELD_DURATION -> current.shieldLevel
            UpgradeType.BOOST_EFFICIENCY -> current.boostLevel
        }
        if (level >= upgrade.maxLevel) return false
        val cost = upgrade.baseCost * level
        if (current.coins < cost) return false

        val updated = when (upgrade) {
            UpgradeType.MAGNET_DURATION -> current.copy(coins = current.coins - cost, magnetLevel = level + 1)
            UpgradeType.SHIELD_DURATION -> current.copy(coins = current.coins - cost, shieldLevel = level + 1)
            UpgradeType.BOOST_EFFICIENCY -> current.copy(coins = current.coins - cost, boostLevel = level + 1)
        }
        dao.upsertProfile(updated)
        return true
    }

    fun currentEpochDay(): Long {
        val cal = Calendar.getInstance()
        return (cal.get(Calendar.YEAR) * 366L) + cal.get(Calendar.DAY_OF_YEAR)
    }

    fun canClaimDaily(profile: PlayerProfileEntity): Boolean {
        return profile.lastDailyClaimEpochDay < currentEpochDay()
    }

    fun nextDailyDay(profile: PlayerProfileEntity): Int {
        val today = currentEpochDay()
        val next = when {
            profile.lastDailyClaimEpochDay < 0L -> 1
            profile.lastDailyClaimEpochDay == today -> profile.dailyStreak.coerceIn(1, 7)
            profile.lastDailyClaimEpochDay == today - 1L -> (profile.dailyStreak % 7) + 1
            else -> 1
        }
        return next
    }

    suspend fun claimDailyReward(): DailyRewardTier? {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        val today = currentEpochDay()
        if (current.lastDailyClaimEpochDay >= today) return null

        val dayIndex = nextDailyDay(current)
        val tier = DailyRewardSchedule.tiers.first { it.day == dayIndex }
        dao.upsertProfile(
            current.copy(
                coins = current.coins + tier.coins,
                gems = current.gems + tier.gems,
                lastDailyClaimEpochDay = today,
                dailyStreak = dayIndex
            )
        )
        return tier
    }

    suspend fun recordMatchResult(
        mode: GameMode,
        score: Int,
        length: Int,
        kills: Int,
        coinsEarned: Int,
        gemsEarned: Int,
        foodEaten: Int,
        durationSec: Int
    ): Boolean {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        val isNewRecord = score > current.highestScore
        dao.upsertProfile(
            current.copy(
                coins = current.coins + coinsEarned,
                gems = current.gems + gemsEarned,
                totalGamesPlayed = current.totalGamesPlayed + 1,
                totalKills = current.totalKills + kills,
                totalFoodEaten = current.totalFoodEaten + foodEaten,
                highestScore = maxOf(current.highestScore, score),
                longestSnake = maxOf(current.longestSnake, length),
                totalTimeSurvivedSec = current.totalTimeSurvivedSec + durationSec
            )
        )
        dao.insertLeaderboardEntry(
            LeaderboardEntryEntity(
                playerName = current.playerName.ifBlank { "Hero" },
                isPlayer = true,
                modeName = mode.name,
                score = score,
                length = length,
                kills = kills,
                durationSec = durationSec,
                skinId = current.equippedSkinId
            )
        )
        return isNewRecord
    }

    suspend fun updateSettings(
        playerName: String? = null,
        soundEnabled: Boolean? = null,
        hapticsEnabled: Boolean? = null,
        joystickSensitivity: Float? = null
    ) {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        dao.upsertProfile(
            current.copy(
                playerName = playerName?.trim()?.take(16)?.ifEmpty { "Hero" } ?: current.playerName,
                soundEnabled = soundEnabled ?: current.soundEnabled,
                hapticsEnabled = hapticsEnabled ?: current.hapticsEnabled,
                joystickSensitivity = joystickSensitivity ?: current.joystickSensitivity
            )
        )
    }

    suspend fun resetPlayerStats() {
        val current = dao.getProfile() ?: PlayerProfileEntity()
        dao.upsertProfile(
            current.copy(
                totalGamesPlayed = 0,
                totalKills = 0,
                totalFoodEaten = 0,
                highestScore = 0,
                longestSnake = 12,
                totalTimeSurvivedSec = 0
            )
        )
        dao.clearPlayerLeaderboardEntries()
    }
}
