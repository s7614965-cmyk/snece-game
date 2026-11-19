package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val playerName: String = "Hero",
    val coins: Int = 150,
    val gems: Int = 5,
    val equippedSkinId: String = "emerald_dash",
    val unlockedSkinsCsv: String = "emerald_dash",
    val selectedModeName: String = "CLASSIC_SURVIVAL",
    val totalGamesPlayed: Int = 0,
    val totalKills: Int = 0,
    val totalFoodEaten: Int = 0,
    val highestScore: Int = 0,
    val longestSnake: Int = 12,
    val totalTimeSurvivedSec: Int = 0,
    val lastDailyClaimEpochDay: Long = -1L,
    val dailyStreak: Int = 0,
    val magnetLevel: Int = 1,
    val shieldLevel: Int = 1,
    val boostLevel: Int = 1,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val joystickSensitivity: Float = 1.0f
) {
    fun unlockedSkinSet(): Set<String> =
        unlockedSkinsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}

@Entity(tableName = "leaderboard_entries")
data class LeaderboardEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerName: String,
    val isPlayer: Boolean,
    val modeName: String,
    val score: Int,
    val length: Int,
    val kills: Int,
    val durationSec: Int,
    val skinId: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
