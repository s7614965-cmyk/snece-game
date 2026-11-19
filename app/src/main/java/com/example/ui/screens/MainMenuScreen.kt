package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PlayerProfileEntity
import com.example.model.AppScreen
import com.example.model.GameMode
import com.example.model.SnakeCatalog
import com.example.ui.components.SnakePreviewCanvas
import com.example.ui.theme.AmethystGem
import com.example.ui.theme.ArenaAzureCard
import com.example.ui.theme.ArenaBrightCyan
import com.example.ui.theme.ArenaDeepNavy
import com.example.ui.theme.ArenaRoyalBlue
import com.example.ui.theme.ArenaSapphire
import com.example.ui.theme.CoralStrike
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.EmeraldAction
import com.example.ui.theme.SoftIceBlue
import com.example.ui.theme.SunburstGold

@Composable
fun MainMenuScreen(
    profile: PlayerProfileEntity,
    canClaimDaily: Boolean,
    onPlayClicked: () -> Unit,
    onNavigate: (AppScreen) -> Unit
) {
    val activeSkin = remember(profile.equippedSkinId) { SnakeCatalog.byId(profile.equippedSkinId) }
    val activeMode = remember(profile.selectedModeName) {
        GameMode.entries.firstOrNull { it.name == profile.selectedModeName } ?: GameMode.CLASSIC_SURVIVAL
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ArenaDeepNavy, ArenaRoyalBlue, Color(0xFF091B3D))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Wallet & Player Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WELCOME, ${profile.playerName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArenaBrightCyan
                        )
                        Text(
                            text = "BEST: ${profile.highestScore}",
                            style = MaterialTheme.typography.titleMedium,
                            color = CrispWhite
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WalletChip(
                            icon = Icons.Rounded.MonetizationOn,
                            amount = profile.coins,
                            color = SunburstGold,
                            tag = "wallet_coins"
                        )
                        WalletChip(
                            icon = Icons.Rounded.Diamond,
                            amount = profile.gems,
                            color = AmethystGem,
                            tag = "wallet_gems"
                        )
                    }
                }

                // Hero Illustration Banner Card
                Card(
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(176.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_arena_hero),
                            contentDescription = stringResource(R.string.cd_hero_banner),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x3307132B),
                                            Color(0xCC07132B)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Surface(
                                color = EmeraldAction,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "2D BATTLE ARENA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArenaDeepNavy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Snake Legends",
                                style = MaterialTheme.typography.displayLarge,
                                color = CrispWhite
                            )
                            Text(
                                text = "Slither, outsmart rivals & collect arena treasure!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftIceBlue
                            )
                        }
                    }
                }

                // Active Snake & Mode Quick Preview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = ArenaSapphire),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x4456B4FF), MaterialTheme.shapes.medium)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EQUIPPED FIGHTER",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArenaBrightCyan
                            )
                            Text(
                                text = activeSkin.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = CrispWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = activeMode.badgeColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable { onNavigate(AppScreen.GAME_MODES) }
                            ) {
                                Text(
                                    text = "Mode: ${activeMode.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeMode.badgeColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        SnakePreviewCanvas(
                            skin = activeSkin,
                            modifier = Modifier.size(width = 148.dp, height = 74.dp)
                        )
                    }
                }

                // Primary Play Button
                Button(
                    onClick = onPlayClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAction,
                        contentColor = ArenaDeepNavy
                    ),
                    shape = MaterialTheme.shapes.large,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .testTag("play_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.menu_play),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 2x3 Navigation Grid covering all Menu Destinations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuTileButton(
                        title = "Select Snake",
                        subtitle = activeSkin.name,
                        icon = Icons.Rounded.Palette,
                        accent = ArenaBrightCyan,
                        tag = "menu_select_snake_button",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.SELECT_SNAKE) }
                    )
                    MenuTileButton(
                        title = "Game Modes",
                        subtitle = activeMode.title,
                        icon = Icons.Rounded.GridView,
                        accent = EmeraldAction,
                        tag = "menu_game_modes_button",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.GAME_MODES) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuTileButton(
                        title = "Shop",
                        subtitle = "Skins & Upgrades",
                        icon = Icons.Rounded.ShoppingBag,
                        accent = SunburstGold,
                        tag = "menu_shop_button",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.SHOP) }
                    )
                    MenuTileButton(
                        title = "Rewards",
                        subtitle = if (canClaimDaily) "Ready to Claim!" else "Daily Streak",
                        icon = Icons.Rounded.CardGiftcard,
                        accent = CoralStrike,
                        showBadge = canClaimDaily,
                        tag = "menu_rewards_button",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.REWARDS) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuTileButton(
                        title = "Leaderboard",
                        subtitle = "Ranks & Stats",
                        icon = Icons.Rounded.EmojiEvents,
                        accent = AmethystGem,
                        tag = "menu_leaderboard_button",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.LEADERBOARD) }
                    )
                    MenuTileButton(
                        title = "Settings",
                        subtitle = "Controls & Audio",
                        icon = Icons.Rounded.Settings,
                        accent = SoftIceBlue,
                        tag = "menu_settings_button",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.SETTINGS) }
                    )
                }
            }
        }
    }
}

@Composable
fun WalletChip(
    icon: ImageVector,
    amount: Int,
    color: Color,
    tag: String
) {
    Surface(
        color = ArenaSapphire,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                text = amount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = CrispWhite
            )
        }
    }
}

@Composable
private fun MenuTileButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ArenaAzureCard),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BadgedBox(
                badge = {
                    if (showBadge) {
                        Badge(containerColor = CoralStrike)
                    }
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f))
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = CrispWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (showBadge) SunburstGold else SoftIceBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
