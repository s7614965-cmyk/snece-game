package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.LeaderboardEntryEntity
import com.example.data.local.PlayerProfileEntity
import com.example.model.GameMode
import com.example.model.SnakeCatalog
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
fun LeaderboardScreen(
    profile: PlayerProfileEntity,
    entries: List<LeaderboardEntryEntity>,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var selectedModeFilter by remember { mutableStateOf<GameMode?>(null) }
    val filtered = remember(entries, selectedModeFilter) {
        val base = if (selectedModeFilter == null) entries else entries.filter { it.modeName == selectedModeFilter?.name }
        base.sortedByDescending { it.score }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArenaDeepNavy, ArenaRoyalBlue)))
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubScreenHeader("Leaderboard & Stats", profile, onBack)

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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("PLAYER STATISTICS", style = MaterialTheme.typography.labelSmall, color = ArenaBrightCyan)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCell("HIGH SCORE", "${profile.highestScore}", SunburstGold)
                            StatCell("LONGEST", "${profile.longestSnake}", EmeraldAction)
                            StatCell("TOTAL KILLS", "${profile.totalKills}", CoralStrike)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCell("BATTLES", "${profile.totalGamesPlayed}", CrispWhite)
                            StatCell("FOOD EATEN", "${profile.totalFoodEaten}", ArenaBrightCyan)
                            StatCell("TIME", "${profile.totalTimeSurvivedSec}s", SoftIceBlue)
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedModeFilter == null,
                            onClick = { selectedModeFilter = null },
                            label = { Text("All Modes") }
                        )
                    }
                    items(GameMode.entries) { mode ->
                        FilterChip(
                            selected = selectedModeFilter == mode,
                            onClick = { selectedModeFilter = mode },
                            label = { Text(mode.title) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ArenaAzureCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = SunburstGold)
                            Spacer(Modifier.height(6.dp))
                            Text("No scores recorded in this mode yet — jump into battle!", color = SoftIceBlue)
                        }
                    }
                }
            } else {
                itemsIndexed(filtered) { index, item ->
                    val skin = SnakeCatalog.byId(item.skinId)
                    val modeTitle = GameMode.entries.firstOrNull { it.name == item.modeName }?.title ?: item.modeName
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isPlayer) ArenaSapphire else ArenaAzureCard
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (item.isPlayer) 2.dp else 1.dp,
                                color = if (item.isPlayer) EmeraldAction else Color(0x2889B4F8),
                                shape = MaterialTheme.shapes.medium
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = when (index) {
                                        0 -> SunburstGold
                                        1 -> Color(0xFFE2E8F0)
                                        2 -> Color(0xFFF59E0B)
                                        else -> SoftIceBlue
                                    },
                                    modifier = Modifier.width(38.dp)
                                )
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.playerName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (item.isPlayer) EmeraldAction else CrispWhite
                                        )
                                        if (item.isPlayer) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(color = EmeraldAction, shape = RoundedCornerShape(50)) {
                                                Text(
                                                    "YOU",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ArenaDeepNavy,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "$modeTitle • ${skin.name} • ${item.kills} KO",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SoftIceBlue
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${item.score}", style = MaterialTheme.typography.titleLarge, color = SunburstGold)
                                Text("Len ${item.length}", style = MaterialTheme.typography.labelSmall, color = SoftIceBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SoftIceBlue)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
    }
}

@Composable
fun SettingsScreen(
    profile: PlayerProfileEntity,
    onUpdateName: (String) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onSensitivityChanged: (Float) -> Unit,
    onResetStats: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var nameDraft by remember(profile.playerName) { mutableStateOf(profile.playerName) }
    var sensitivityDraft by remember(profile.joystickSensitivity) { mutableFloatStateOf(profile.joystickSensitivity) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArenaDeepNavy, ArenaRoyalBlue)))
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubScreenHeader("Settings", profile, onBack)

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Player Name Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ArenaAzureCard),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Arena Callsign", style = MaterialTheme.typography.titleLarge, color = CrispWhite)
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { if (it.length <= 16) nameDraft = it },
                        singleLine = true,
                        label = { Text("Player Name") },
                        modifier = Modifier.fillMaxWidth().testTag("player_name_input")
                    )
                    Button(
                        onClick = { onUpdateName(nameDraft) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAction, contentColor = ArenaDeepNavy),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_name_button")
                    ) {
                        Text("SAVE CALLSIGN")
                    }
                }
            }

            // Audio & Controls Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ArenaAzureCard),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Audio & Controls", style = MaterialTheme.typography.titleLarge, color = CrispWhite)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Cartoon Sound Effects", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                            Text("Arcade chimes for pickups, bursts & KOs", style = MaterialTheme.typography.bodyMedium, color = SoftIceBlue)
                        }
                        Switch(
                            checked = profile.soundEnabled,
                            onCheckedChange = onToggleSound,
                            modifier = Modifier.testTag("sound_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Haptic Feedback", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                            Text("Tactile vibration on abilities & collisions", style = MaterialTheme.typography.bodyMedium, color = SoftIceBlue)
                        }
                        Switch(
                            checked = profile.hapticsEnabled,
                            onCheckedChange = onToggleHaptics,
                            modifier = Modifier.testTag("haptics_switch")
                        )
                    }

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Steering Turn Sensitivity", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                            Text("${String.format("%.1f", sensitivityDraft)}x", style = MaterialTheme.typography.labelLarge, color = ArenaBrightCyan)
                        }
                        Slider(
                            value = sensitivityDraft,
                            onValueChange = { sensitivityDraft = it },
                            onValueChangeFinished = { onSensitivityChanged(sensitivityDraft) },
                            valueRange = 0.7f..1.5f
                        )
                    }
                }
            }

            // Controls Guide Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ArenaSapphire),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Touch & Keyboard Controls", style = MaterialTheme.typography.titleLarge, color = SunburstGold)
                    Text("• Touch: Drag the bottom-left joystick for 360° steering.", style = MaterialTheme.typography.bodyMedium, color = CrispWhite)
                    Text("• Boost: Hold the orange BOOST button (or Space / Shift).", style = MaterialTheme.typography.bodyMedium, color = CrispWhite)
                    Text("• Ability: Tap BURST when charged (or E / Enter) for Shield + Magnet.", style = MaterialTheme.typography.bodyMedium, color = CrispWhite)
                    Text("• Keyboard: Steer with W/A/S/D or Arrow keys; P / Esc pauses.", style = MaterialTheme.typography.bodyMedium, color = CrispWhite)
                }
            }

            OutlinedButton(
                onClick = onResetStats,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralStrike),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("reset_stats_button")
            ) {
                Text("RESET PLAYER STATISTICS")
            }
        }
    }
}
