package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PlayerProfileEntity
import com.example.model.GameMode
import com.example.model.SnakeCatalog
import com.example.ui.components.SnakePreviewCanvas
import com.example.ui.theme.AmethystGem
import com.example.ui.theme.ArenaAzureCard
import com.example.ui.theme.ArenaDeepNavy
import com.example.ui.theme.ArenaRoyalBlue
import com.example.ui.theme.ArenaSapphire
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldAction
import com.example.ui.theme.SoftIceBlue
import com.example.ui.theme.SunburstGold

@Composable
fun SubScreenHeader(
    title: String,
    profile: PlayerProfileEntity,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp).testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = CrispWhite
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = CrispWhite
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WalletChip(Icons.Rounded.MonetizationOn, profile.coins, SunburstGold, "header_coins")
            WalletChip(Icons.Rounded.Diamond, profile.gems, AmethystGem, "header_gems")
        }
    }
}

@Composable
fun GameModesScreen(
    profile: PlayerProfileEntity,
    onSelectMode: (GameMode) -> Unit,
    onPlayMode: (GameMode) -> Unit,
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
        SubScreenHeader("Game Modes", profile, onBack)

        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(GameMode.entries) { mode ->
                val selected = profile.selectedModeName == mode.name
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) ArenaSapphire else ArenaAzureCard
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) mode.badgeColor else Color(0x3389B4F8),
                            shape = MaterialTheme.shapes.large
                        )
                        .clickable { onSelectMode(mode) }
                        .testTag("mode_card_${mode.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = CrispWhite
                            )
                            Surface(
                                color = mode.badgeColor.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = mode.durationSec?.let { "${it}s Match" } ?: "Unlimited",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = mode.badgeColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = mode.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftIceBlue
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSelectMode(mode) },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                if (selected) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = mode.badgeColor, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("SELECTED", color = mode.badgeColor)
                                } else {
                                    Text("SELECT")
                                }
                            }
                            Button(
                                onClick = { onPlayMode(mode) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldAction,
                                    contentColor = ArenaDeepNavy
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("play_mode_${mode.name.lowercase()}")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("PLAY NOW")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectSnakeScreen(
    profile: PlayerProfileEntity,
    onEquipOrBuy: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val unlockedSet = profile.unlockedSkinSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArenaDeepNavy, ArenaRoyalBlue)))
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubScreenHeader("Select Snake", profile, onBack)

        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(SnakeCatalog.skins, key = { it.id }) { skin ->
                val isUnlocked = unlockedSet.contains(skin.id)
                val isEquipped = profile.equippedSkinId == skin.id

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEquipped) ArenaSapphire else ArenaAzureCard
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isEquipped) 2.dp else 1.dp,
                            color = if (isEquipped) EmeraldAction else Color(0x3389B4F8),
                            shape = MaterialTheme.shapes.large
                        )
                        .testTag("snake_card_${skin.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = skin.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = CrispWhite
                            )
                            Text(
                                text = skin.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftIceBlue
                            )
                            Spacer(Modifier.height(4.dp))

                            Button(
                                onClick = { onEquipOrBuy(skin.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isEquipped -> EmeraldAction
                                        isUnlocked -> Color(0xFF2B6CB0)
                                        skin.gemPrice > 0 -> AmethystGem
                                        else -> SunburstGold
                                    },
                                    contentColor = if (isEquipped || !isUnlocked) ArenaDeepNavy else CrispWhite
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("snake_action_${skin.id}")
                            ) {
                                when {
                                    isEquipped -> {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("EQUIPPED")
                                    }
                                    isUnlocked -> Text("EQUIP SNAKE")
                                    skin.gemPrice > 0 -> {
                                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("UNLOCK ${skin.gemPrice} GEMS")
                                    }
                                    else -> {
                                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("UNLOCK ${skin.coinPrice} COINS")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                        SnakePreviewCanvas(
                            skin = skin,
                            modifier = Modifier.size(width = 132.dp, height = 84.dp)
                        )
                    }
                }
            }
        }
    }
}
