package com.example.model

import androidx.compose.ui.graphics.Color

enum class GameMode(
    val title: String,
    val subtitle: String,
    val arenaRadius: Float,
    val botCount: Int,
    val durationSec: Int?,
    val badgeColor: Color
) {
    CLASSIC_SURVIVAL(
        title = "Classic Survival",
        subtitle = "Outgrow 14 rivals as difficulty rises every 30s",
        arenaRadius = 2100f,
        botCount = 14,
        durationSec = null,
        badgeColor = Color(0xFF2EE59D)
    ),
    TIME_CHALLENGE(
        title = "Time Challenge",
        subtitle = "2-minute frenzy with 2x treasure & fast respawns",
        arenaRadius = 1850f,
        botCount = 14,
        durationSec = 120,
        badgeColor = Color(0xFFFFD166)
    ),
    TEAM_BATTLE(
        title = "Team Battle",
        subtitle = "Blue Legends (5) vs Crimson Raiders (6) – 2:30 match",
        arenaRadius = 2100f,
        botCount = 10,
        durationSec = 150,
        badgeColor = Color(0xFF00E5FF)
    ),
    ENDLESS_ARENA(
        title = "Endless Arena",
        subtitle = "Massive fantasy realm with rich gems & endless glory",
        arenaRadius = 2650f,
        botCount = 18,
        durationSec = null,
        badgeColor = Color(0xFFC07BFF)
    )
}

enum class SkinPattern {
    STRIPED,
    SPOTTED,
    CHEVRON,
    DIAMOND,
    CIRCUIT,
    STARLIGHT
}

enum class HeadAccessory {
    NONE,
    CROWN,
    HORNS,
    VISOR,
    BLUSH,
    HALO
}

data class SnakeSkin(
    val id: String,
    val name: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val bellyColor: Color,
    val pattern: SkinPattern,
    val accessory: HeadAccessory,
    val coinPrice: Int = 0,
    val gemPrice: Int = 0
)

object SnakeCatalog {
    val skins: List<SnakeSkin> = listOf(
        SnakeSkin(
            id = "emerald_dash",
            name = "Emerald Dash",
            description = "Nimble forest champion with bright lime stripes",
            primaryColor = Color(0xFF2EE59D),
            secondaryColor = Color(0xFF0FA968),
            bellyColor = Color(0xFFA7F9D7),
            pattern = SkinPattern.STRIPED,
            accessory = HeadAccessory.NONE,
            coinPrice = 0,
            gemPrice = 0
        ),
        SnakeSkin(
            id = "coral_blaze",
            name = "Coral Blaze",
            description = "Fiery sunset chevrons built for bold ambushes",
            primaryColor = Color(0xFFFF5E7E),
            secondaryColor = Color(0xFFFF9F43),
            bellyColor = Color(0xFFFFD8BE),
            pattern = SkinPattern.CHEVRON,
            accessory = HeadAccessory.NONE,
            coinPrice = 120,
            gemPrice = 0
        ),
        SnakeSkin(
            id = "frost_wyrm",
            name = "Frost Wyrm",
            description = "Crystal-scaled ice serpent of the northern arena",
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFF2979FF),
            bellyColor = Color(0xFFD6F9FF),
            pattern = SkinPattern.DIAMOND,
            accessory = HeadAccessory.NONE,
            coinPrice = 240,
            gemPrice = 0
        ),
        SnakeSkin(
            id = "royal_viper",
            name = "Royal Viper",
            description = "Crowned amethyst sovereign with golden regalia",
            primaryColor = Color(0xFFB36BFF),
            secondaryColor = Color(0xFF7B2CBF),
            bellyColor = Color(0xFFFFD166),
            pattern = SkinPattern.SPOTTED,
            accessory = HeadAccessory.CROWN,
            coinPrice = 400,
            gemPrice = 0
        ),
        SnakeSkin(
            id = "solar_dragon",
            name = "Solar Dragon",
            description = "Radiant amber dragon scales with golden crest horns",
            primaryColor = Color(0xFFFFD166),
            secondaryColor = Color(0xFFF77F00),
            bellyColor = Color(0xFFFFF3C4),
            pattern = SkinPattern.CHEVRON,
            accessory = HeadAccessory.HORNS,
            coinPrice = 650,
            gemPrice = 0
        ),
        SnakeSkin(
            id = "cyber_pulse",
            name = "Cyber Pulse",
            description = "Neon-lit tech serpent sporting a holographic visor",
            primaryColor = Color(0xFF1E3A8A),
            secondaryColor = Color(0xFF00F5D4),
            bellyColor = Color(0xFF72EFDD),
            pattern = SkinPattern.CIRCUIT,
            accessory = HeadAccessory.VISOR,
            coinPrice = 0,
            gemPrice = 15
        ),
        SnakeSkin(
            id = "sakura_koi",
            name = "Sakura Koi",
            description = "Blossom-spotted spirit with rosy cartoon cheeks",
            primaryColor = Color(0xFFFF8FAB),
            secondaryColor = Color(0xFFFFE5EC),
            bellyColor = Color(0xFFFFFFFF),
            pattern = SkinPattern.SPOTTED,
            accessory = HeadAccessory.BLUSH,
            coinPrice = 0,
            gemPrice = 25
        ),
        SnakeSkin(
            id = "starlight_legend",
            name = "Starlight Legend",
            description = "Cosmic mythic guardian crowned by a floating halo",
            primaryColor = Color(0xFF4338CA),
            secondaryColor = Color(0xFFFDE047),
            bellyColor = Color(0xFFC7D2FE),
            pattern = SkinPattern.STARLIGHT,
            accessory = HeadAccessory.HALO,
            coinPrice = 0,
            gemPrice = 40
        )
    )

    fun byId(id: String): SnakeSkin = skins.firstOrNull { it.id == id } ?: skins.first()
}

enum class PowerUpType(val label: String, val durationSec: Float, val color: Color) {
    SPEED_SURGE("Turbo Surge", 6f, Color(0xFFFFD166)),
    MAGNET("Magnet Aura", 8f, Color(0xFF00E5FF)),
    SHIELD("Arcane Shield", 6f, Color(0xFF2EE59D))
}

data class DailyRewardTier(
    val day: Int,
    val coins: Int,
    val gems: Int,
    val label: String
)

object DailyRewardSchedule {
    val tiers: List<DailyRewardTier> = listOf(
        DailyRewardTier(1, 100, 2, "Starter Pouch"),
        DailyRewardTier(2, 150, 3, "Bronze Chest"),
        DailyRewardTier(3, 220, 4, "Sapphire Cache"),
        DailyRewardTier(4, 300, 6, "Champion Loot"),
        DailyRewardTier(5, 400, 8, "Dragon Hoard"),
        DailyRewardTier(6, 550, 12, "Royal Vault"),
        DailyRewardTier(7, 800, 20, "Legend Jackpot")
    )
}

enum class UpgradeType(val title: String, val description: String, val maxLevel: Int, val baseCost: Int) {
    MAGNET_DURATION("Magnet Mastery", "+1.5s Magnet Aura duration per level", 5, 120),
    SHIELD_DURATION("Shield Fortress", "+1.2s Arcane Shield duration per level", 5, 150),
    BOOST_EFFICIENCY("Turbo Thrusters", "Reduces boost energy drain by 12% per level", 5, 130)
}

enum class AppScreen {
    MAIN_MENU,
    GAME_MODES,
    SELECT_SNAKE,
    SHOP,
    REWARDS,
    LEADERBOARD,
    SETTINGS,
    PLAYING
}
