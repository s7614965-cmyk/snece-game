package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlayerProfileEntity::class, LeaderboardEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SnakeLegendsDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: SnakeLegendsDatabase? = null

        fun getInstance(context: Context): SnakeLegendsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SnakeLegendsDatabase::class.java,
                    "snake_legends.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
