package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfile(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM leaderboard_entries ORDER BY score DESC LIMIT 50")
    fun observeTopLeaderboard(): Flow<List<LeaderboardEntryEntity>>

    @Query("SELECT * FROM leaderboard_entries WHERE modeName = :modeName ORDER BY score DESC LIMIT 30")
    fun observeLeaderboardByMode(modeName: String): Flow<List<LeaderboardEntryEntity>>

    @Query("SELECT COUNT(*) FROM leaderboard_entries")
    suspend fun getLeaderboardCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntry(entry: LeaderboardEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntries(entries: List<LeaderboardEntryEntity>)

    @Query("DELETE FROM leaderboard_entries WHERE isPlayer = 1")
    suspend fun clearPlayerLeaderboardEntries()
}
