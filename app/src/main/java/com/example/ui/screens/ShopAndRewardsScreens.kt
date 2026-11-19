package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.PlayerProfileEntity
import com.example.model.DailyRewardSchedule
import com.example.model.DailyRewardTier
import com.example.model.SnakeCatalog
import com.example.model.UpgradeType
import com.example.ui.components.SnakePreviewCanvas
import com.example.ui.theme.AmethystGem
import com.example.ui.theme.ArenaAzureCard
import com.example.ui.theme.ArenaBrightCyan
import com.example.ui.theme.ArenaDeepNavy
import com.example.ui.theme.ArenaRoyalBlue
import com.example.ui.theme.ArenaSapphire
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldAction
import com.example.ui.theme.SoftIceBlue
import com.example.ui.theme.SunburstGold

@Composable
fun ShopScreen(
    profile: PlayerProfileEntity,
    onBuyOrEquipSkin: (String) -> Unit,
    onBuyUpgrade: (UpgradeType) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var tabIndex by remember { mutableIntStateOf(0) }
    val unlocked = profile.unlockedSkinSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArenaDeepNavy, ArenaRoyalBlue)))
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubScreenHeader("Arena Shop", profile, onBack)

        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = ArenaSapphire,
            contentColor = CrispWhite,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
        ) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                text = { Text("SNAKE SKINS") },
                modifier = Modifier.testTag("shop_tab_skins")
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                text = { Text("POWER UPGRADES") },
                modifier = Modifier.testTag("shop_tab_upgrades")
            )
        }

        if (tabIndex == 0) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(SnakeCatalog.skins, key = { it.id }) { skin ->
                    val isUnlocked = unlocked.contains(skin.id)
                    val isEquipped = profile.equippedSkinId == skin.id
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ArenaAzureCard),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SnakePreviewCanvas(
                                skin = skin,
                                modifier = Modifier.size(width = 112.dp, height = 70.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(skin.name, style = MaterialTheme.typography.titleLarge, color = CrispWhite)
                                Text(skin.description, style = MaterialTheme.typography.bodyMedium, color = SoftIceBlue)
                                Spacer(Modifier.height(6.dp))
                                Button(
                                    onClick = { onBuyOrEquipSkin(skin.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when {
                                            isEquipped -> EmeraldAction
                                            isUnlocked -> Color(0xFF2B6CB0)
                                            skin.gemPrice > 0 -> AmethystGem
                                            else -> SunburstGold
                                        },
                                        contentColor = if (isEquipped || !isUnlocked) ArenaDeepNavy else CrispWhite
                                    ),
                                    modifier = Modifier.height(48.dp).testTag("shop_skin_${skin.id}")
                                ) {
                                    Text(
                                        when {
                                            isEquipped -> "EQUIPPED"
                                            isUnlocked -> "EQUIP"
                                            skin.gemPrice > 0 -> "BUY ${skin.gemPrice} GEMS"
                                            else -> "BUY ${skin.coinPrice} COINS"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(UpgradeType.entries) { upgrade ->
                    val level = when (upgrade) {
                        UpgradeType.MAGNET_DURATION -> profile.magnetLevel
                        UpgradeType.SHIELD_DURATION -> profile.shieldLevel
                        UpgradeType.BOOST_EFFICIENCY -> profile.boostLevel
                    }
                    val isMax = level >= upgrade.maxLevel
                    val cost = upgrade.baseCost * level
                    val icon = when (upgrade) {
                        UpgradeType.MAGNET_DURATION -> Icons.Rounded.AutoAwesome
                        UpgradeType.SHIELD_DURATION -> Icons.Rounded.Shield
                        UpgradeType.BOOST_EFFICIENCY -> Icons.Rounded.Bolt
                    }
                    val accent = when (upgrade) {
                        UpgradeType.MAGNET_DURATION -> ArenaBrightCyan
                        UpgradeType.SHIELD_DURATION -> EmeraldAction
                        UpgradeType.BOOST_EFFICIENCY -> SunburstGold
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = ArenaAzureCard),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(upgrade.title, style = MaterialTheme.typography.titleLarge, color = CrispWhite)
                                    Text(upgrade.description, style = MaterialTheme.typography.bodyMedium, color = SoftIceBlue)
                                }
                                Surface(
                                    color = accent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "Lv.$level/${upgrade.maxLevel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { level.toFloat() / upgrade.maxLevel },
                                color = accent,
                                trackColor = Color(0x33FFFFFF),
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            )

                            Button(
                                onClick = { onBuyUpgrade(upgrade) },
                                enabled = !isMax,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SunburstGold,
                                    contentColor = ArenaDeepNavy
                                ),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("upgrade_${upgrade.name.lowercase()}")
                            ) {
                                Text(if (isMax) "MAX LEVEL" else "UPGRADE FOR $cost COINS")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RewardsScreen(
    profile: PlayerProfileEntity,
    canClaimToday: Boolean,
    nextDayNumber: Int,
    claimedRewardPopup: DailyRewardTier?,
    onClaimDaily: () -> Unit,
    onDismissPopup: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArenaDeepNavy, ArenaRoyalBlue)))
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubScreenHeader("Daily Rewards", profile, onBack)

        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ArenaSapphire),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = SunburstGold, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("7-Day Arena Streak", style = MaterialTheme.typography.titleLarge, color = CrispWhite)
                                Text(
                                    text = if (canClaimToday) "Day $nextDayNumber treasure is ready to claim!" else "Today's reward claimed. Current streak: ${profile.dailyStreak} day(s)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (canClaimToday) EmeraldAction else SoftIceBlue
                                )
                            }
                        }

                        Button(
                            onClick = onClaimDaily,
                            enabled = canClaimToday,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldAction,
                                contentColor = ArenaDeepNavy
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("claim_daily_reward_button")
                        ) {
                            Text(if (canClaimToday) "CLAIM DAY $nextDayNumber REWARD" else "ALREADY CLAIMED TODAY")
                        }
                    }
                }
            }

            items(DailyRewardSchedule.tiers) { tier ->
                val isClaimed = !canClaimToday && tier.day <= profile.dailyStreak || (canClaimToday && tier.day < nextDayNumber)
                val isCurrent = canClaimToday && tier.day == nextDayNumber

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) ArenaSapphire else ArenaAzureCard
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = if (isCurrent) SunburstGold else Color(0x2889B4F8),
                            shape = MaterialTheme.shapes.medium
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Day ${tier.day} • ${tier.label}", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                            Text(
                                text = "+${tier.coins} Coins & +${tier.gems} Gems",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SunburstGold
                            )
                        }
                        when {
                            isClaimed -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Claimed", tint = EmeraldAction)
                            isCurrent -> Surface(color = SunburstGold, shape = RoundedCornerShape(50)) {
                                Text(
                                    "TODAY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArenaDeepNavy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            else -> Text("UPCOMING", style = MaterialTheme.typography.labelSmall, color = SoftIceBlue)
                        }
                    }
                }
            }
        }
    }

    if (claimedRewardPopup != null) {
        AlertDialog(
            onDismissRequest = onDismissPopup,
            containerColor = ArenaRoyalBlue,
            title = {
                Text("Day ${claimedRewardPopup.day} Claimed!", color = SunburstGold)
            },
            text = {
                Text(
                    "You received +${claimedRewardPopup.coins} Coins and +${claimedRewardPopup.gems} Gems from ${claimedRewardPopup.label}!",
                    color = CrispWhite
                )
            },
            confirmButton = {
                Button(
                    onClick = onDismissPopup,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAction, contentColor = ArenaDeepNavy)
                ) {
                    Text("AWESOME!")
                }
            }
        )
    }
}
