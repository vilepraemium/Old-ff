package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CatalogItem
import com.example.model.GameCatalog
import com.example.model.OwnedItem
import com.example.model.RoyaleType
import com.example.model.SpinOutcome
import com.example.model.UserProfile
import com.example.ui.components.OldFfTopCurrencyBar
import com.example.ui.components.RarityChip
import com.example.ui.components.formatNumber
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SteelSlate
import com.example.ui.theme.TacticalObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LuckRoyaleScreen(
    profile: UserProfile,
    ownedItems: List<OwnedItem>,
    isBusy: Boolean,
    onPerformSpin: (RoyaleType, isTenPlusOne: Boolean, onResult: (Result<SpinOutcome>) -> Unit) -> Unit,
    onOpenTopUp: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var selectedRoyale by remember { mutableStateOf(RoyaleType.DIAMOND_ROYALE) }
    var isAnimatingSpin by remember { mutableStateOf(false) }
    var highlightedSlot by remember { mutableIntStateOf(-1) }
    var revealedOutcome by remember { mutableStateOf<SpinOutcome?>(null) }

    val scope = rememberCoroutineScope()
    val ownedIds = remember(ownedItems) { ownedItems.map { it.itemId }.toSet() }
    val pool = remember(selectedRoyale) {
        GameCatalog.poolForRoyale(selectedRoyale).take(12)
    }

    val accent = if (selectedRoyale.usesDiamonds) DiamondCyan else AmberGold

    fun triggerSpin(isTenPlusOne: Boolean) {
        if (isAnimatingSpin || isBusy) return
        isAnimatingSpin = true
        onPerformSpin(selectedRoyale, isTenPlusOne) { result ->
            result.onSuccess { outcome ->
                scope.launch {
                    val targetItem = outcome.drops.firstOrNull { it.item != null }?.item
                    val targetIdx = pool.indexOfFirst { it.id == targetItem?.id }.let {
                        if (it >= 0) it else 0
                    }
                    val steps = 16 + targetIdx
                    for (step in 0..steps) {
                        highlightedSlot = step % pool.size.coerceAtLeast(1)
                        val delayMs = when {
                            step < 10 -> 55L
                            step < 14 -> 95L
                            else -> 145L
                        }
                        delay(delayMs)
                    }
                    isAnimatingSpin = false
                    revealedOutcome = outcome
                }
            }.onFailure {
                isAnimatingSpin = false
                highlightedSlot = -1
            }
        }
    }

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
            screenTitle = "LUCK ROYALE",
        )

        TabRow(
            selectedTabIndex = selectedRoyale.ordinal,
            containerColor = CharcoalPanel,
            contentColor = accent,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedRoyale.ordinal]),
                    color = accent,
                )
            },
        ) {
            RoyaleType.entries.forEach { royale ->
                Tab(
                    selected = selectedRoyale == royale,
                    onClick = {
                        if (!isAnimatingSpin) {
                            selectedRoyale = royale
                            highlightedSlot = -1
                        }
                    },
                    text = {
                        Text(
                            text = royale.title.replace(" ROYALE", ""),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        }

        // Royale Banner & Luck Pity Bar
        Surface(
            color = CharcoalPanel,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedRoyale.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = accent,
                        )
                        Text(
                            text = selectedRoyale.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LUCK ${profile.luckPoints}/100",
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberGold,
                        )
                        Text(
                            text = "100 = GUARANTEED GRAND",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { (profile.luckPoints / 100f).coerceIn(0f, 1f) },
                    color = AmberGold,
                    trackColor = SteelSlate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
            }
        }

        // Prize Roulette Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 112.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(pool, key = { _, item -> item.id }) { index, item ->
                val isActive = index == highlightedSlot
                val isOwned = item.id in ownedIds
                RoyalePrizeTile(
                    item = item,
                    isActive = isActive,
                    isOwned = isOwned,
                )
            }
        }

        // Bottom Spin Action Buttons (1 SPIN & 10+1 SPINS)
        Surface(
            color = CharcoalPanel,
            border = BorderStroke(1.dp, SteelBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { triggerSpin(isTenPlusOne = false) },
                    enabled = !isAnimatingSpin && !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SteelSlate,
                        contentColor = TextPrimary,
                    ),
                    border = BorderStroke(1.5.dp, accent),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("spin_one_button"),
                ) {
                    Icon(
                        imageVector = if (selectedRoyale.usesDiamonds) Icons.Filled.Diamond else Icons.Filled.MonetizationOn,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1 SPIN (${formatNumber(selectedRoyale.singleCost)})",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Button(
                    onClick = { triggerSpin(isTenPlusOne = true) },
                    enabled = !isAnimatingSpin && !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = TacticalObsidian,
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("spin_ten_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "10+1 (${formatNumber(selectedRoyale.tenCost)})",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }

    val outcome = revealedOutcome
    if (outcome != null) {
        AlertDialog(
            onDismissRequest = { revealedOutcome = null },
            containerColor = CharcoalPanel,
            title = {
                Text(
                    text = if (outcome.drops.size > 1) "10+1 ROYALE REWARDS!" else "ROYALE REWARD!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AmberGold,
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (outcome.drops.size > 3) 260.dp else 130.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(outcome.drops) { drop ->
                        Surface(
                            color = SteelSlate,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, drop.rarity.color.copy(alpha = 0.7f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = drop.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                    )
                                    Text(
                                        text = when {
                                            drop.item == null -> "Currency Bonus"
                                            drop.wasDuplicate -> "Duplicate -> Converted to Gold"
                                            else -> "NEW! Unlocked & Equipped"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (drop.item != null && !drop.wasDuplicate) EmeraldSafe else AmberGold,
                                    )
                                }
                                RarityChip(drop.rarity)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { revealedOutcome = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGold,
                        contentColor = TacticalObsidian,
                    ),
                    modifier = Modifier.testTag("spin_reward_ok_button"),
                ) {
                    Text("COLLECT ALL", style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

@Composable
private fun RoyalePrizeTile(
    item: CatalogItem,
    isActive: Boolean,
    isOwned: Boolean,
) {
    Surface(
        color = if (isActive) AmberGold.copy(alpha = 0.25f) else CharcoalPanel,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            width = if (isActive) 2.5.dp else 1.dp,
            color = if (isActive) AmberGold else item.rarity.color.copy(alpha = 0.55f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RarityChip(item.rarity)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(item.primaryColor)
                    .border(1.dp, item.accentColor, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.styleTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (isOwned) "OWNED" else item.category.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isOwned) EmeraldSafe else TextSecondary,
            )
        }
    }
}
