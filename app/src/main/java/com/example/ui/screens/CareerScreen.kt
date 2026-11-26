package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.GameMode
import com.example.model.MatchRecord
import com.example.model.UserProfile
import com.example.ui.components.OldFfTopCurrencyBar
import com.example.ui.components.formatNumber
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.TacticalObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CareerScreen(
    profile: UserProfile,
    matchHistory: List<MatchRecord>,
    onOpenTopUp: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalObsidian)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        OldFfTopCurrencyBar(
            profile = profile,
            onOpenTopUp = onOpenTopUp,
            onBack = onBack,
            screenTitle = "CAREER & STATS",
        )

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            item {
                Surface(
                    color = CharcoalPanel,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.6f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatCell("MATCHES", formatNumber(profile.matchesPlayed), TextPrimary)
                        StatCell("BOOYAH!", formatNumber(profile.booyahs), AmberGold)
                        StatCell("KILLS", formatNumber(profile.kills), DiamondCyan)
                        StatCell("LEVEL", "${profile.level}", EmeraldSafe)
                    }
                }
            }

            item {
                Text(
                    text = "RECENT OPEN-WORLD MATCHES",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AmberGold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            if (matchHistory.isEmpty()) {
                item {
                    Surface(
                        color = CharcoalPanel,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SteelBorder),
                    ) {
                        Text(
                            text = "No matches played yet. Tap START in the lobby to drop onto Bermuda or Purgatory!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                items(matchHistory, key = { it.id }) { match ->
                    val isBooyah = match.placement == 1L
                    val modeLabel = GameMode.safeValueOf(match.mode).title
                    Surface(
                        color = CharcoalPanel,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isBooyah) AmberGold else SteelBorder,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBooyah) "BOOYAH! #1 • $modeLabel" else "RANK #${match.placement}/20 • $modeLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isBooyah) AmberGold else TextPrimary,
                                )
                                Text(
                                    text = "${match.kills} Kills • ${match.damageDealt} DMG • ${match.survivalSeconds}s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+${match.goldEarned} Gold",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AmberGold,
                                )
                                Text(
                                    text = "+${match.xpEarned} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSafe,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.displayMedium, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
