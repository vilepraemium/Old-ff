package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.model.ItemCategory
import com.example.model.OwnedItem
import com.example.model.UserProfile
import com.example.ui.components.CharacterPreviewCanvas
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

@Composable
fun StoreScreen(
    profile: UserProfile,
    ownedItems: List<OwnedItem>,
    isBusy: Boolean,
    onPurchase: (CatalogItem, useDiamonds: Boolean) -> Unit,
    onEquip: (CatalogItem) -> Unit,
    onOpenTopUp: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val ownedIds = remember(ownedItems) { ownedItems.map { it.itemId }.toSet() }
    var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }
    var previewItem by remember { mutableStateOf<CatalogItem?>(null) }

    val catalogList = remember(selectedCategory) {
        GameCatalog.items.filter { !it.isStarter && (selectedCategory == null || it.category == selectedCategory) }
    }

    val previewHat = if (previewItem?.category == ItemCategory.HAT) previewItem!!.id else profile.equippedHatId
    val previewMask = if (previewItem?.category == ItemCategory.MASK) previewItem!!.id else profile.equippedMaskId
    val previewTop = if (previewItem?.category == ItemCategory.TOP) previewItem!!.id else profile.equippedTopId
    val previewBottom = if (previewItem?.category == ItemCategory.BOTTOM) previewItem!!.id else profile.equippedBottomId
    val previewShoes = if (previewItem?.category == ItemCategory.SHOES) previewItem!!.id else profile.equippedShoesId
    val previewWeapon = if (previewItem?.category == ItemCategory.WEAPON_SKIN) previewItem!!.id else profile.equippedWeaponSkinId

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
            screenTitle = "CLASSIC STORE",
        )

        // Live Character Outfit Preview Strip
        Surface(
            color = CharcoalPanel,
            border = BorderStroke(1.dp, SteelBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CharacterPreviewCanvas(
                    hatId = previewHat,
                    maskId = previewMask,
                    topId = previewTop,
                    bottomId = previewBottom,
                    shoesId = previewShoes,
                    weaponSkinId = previewWeapon,
                    modifier = Modifier
                        .width(128.dp)
                        .fillMaxSize(),
                )
                Spacer(modifier = Modifier.width(12.dp))
                val focused = previewItem ?: catalogList.firstOrNull()
                if (focused != null) {
                    val isOwned = focused.id in ownedIds
                    val isEquipped = isEquippedInProfile(profile, focused)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            RarityChip(focused.rarity)
                            Text(
                                text = focused.category.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                        Text(
                            text = focused.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = focused.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isOwned) {
                            Button(
                                onClick = { onEquip(focused) },
                                enabled = !isEquipped && !isBusy,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldSafe,
                                    contentColor = TacticalObsidian,
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(38.dp),
                            ) {
                                Text(
                                    text = if (isEquipped) "EQUIPPED" else "EQUIP NOW",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                focused.diamondPrice?.let { price ->
                                    Button(
                                        onClick = { onPurchase(focused, true) },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DiamondCyan,
                                            contentColor = TacticalObsidian,
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(38.dp)
                                            .testTag("buy_diamond_${focused.id}"),
                                    ) {
                                        Icon(Icons.Filled.Diamond, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(formatNumber(price), style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                                focused.goldPrice?.let { price ->
                                    Button(
                                        onClick = { onPurchase(focused, false) },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AmberGold,
                                            contentColor = TacticalObsidian,
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(38.dp)
                                            .testTag("buy_gold_${focused.id}"),
                                    ) {
                                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(formatNumber(price), style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        CategoryFilterStrip(
            selected = selectedCategory,
            onSelect = { selectedCategory = it },
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 152.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(catalogList, key = { it.id }) { item ->
                val isOwned = item.id in ownedIds
                val isSelected = previewItem?.id == item.id
                OutfitItemCard(
                    item = item,
                    isOwned = isOwned,
                    isEquipped = isEquippedInProfile(profile, item),
                    isHighlighted = isSelected,
                    onClick = { previewItem = item },
                )
            }
        }
    }
}

@Composable
fun VaultScreen(
    profile: UserProfile,
    ownedItems: List<OwnedItem>,
    onEquip: (CatalogItem) -> Unit,
    onOpenStore: () -> Unit,
    onOpenTopUp: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }
    val ownedCatalogItems = remember(ownedItems, selectedCategory) {
        val ids = ownedItems.map { it.itemId }.toSet() + GameCatalog.starterItemIds
        GameCatalog.items.filter {
            it.id in ids && (selectedCategory == null || it.category == selectedCategory)
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
            screenTitle = "FASHION VAULT",
        )

        Surface(
            color = CharcoalPanel,
            border = BorderStroke(1.dp, SteelBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CharacterPreviewCanvas(
                    hatId = profile.equippedHatId,
                    maskId = profile.equippedMaskId,
                    topId = profile.equippedTopId,
                    bottomId = profile.equippedBottomId,
                    shoesId = profile.equippedShoesId,
                    weaponSkinId = profile.equippedWeaponSkinId,
                    modifier = Modifier
                        .width(128.dp)
                        .fillMaxSize(),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "EQUIPPED LOADOUT",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AmberGold,
                    )
                    Text(
                        text = "Hat: ${GameCatalog.findItem(profile.equippedHatId)?.name ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Top: ${GameCatalog.findItem(profile.equippedTopId)?.name ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Pants: ${GameCatalog.findItem(profile.equippedBottomId)?.name ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Button(
                        onClick = onOpenStore,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SteelSlate,
                            contentColor = AmberGold,
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Text("GET MORE CLOTHES", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        CategoryFilterStrip(
            selected = selectedCategory,
            onSelect = { selectedCategory = it },
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 152.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(ownedCatalogItems, key = { it.id }) { item ->
                OutfitItemCard(
                    item = item,
                    isOwned = true,
                    isEquipped = isEquippedInProfile(profile, item),
                    isHighlighted = isEquippedInProfile(profile, item),
                    onClick = { onEquip(item) },
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterStrip(
    selected: ItemCategory?,
    onSelect: (ItemCategory?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("ALL") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AmberGold.copy(alpha = 0.25f),
                selectedLabelColor = AmberGold,
            ),
        )
        ItemCategory.entries.forEach { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(cat) },
                label = { Text(cat.label.uppercase()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AmberGold.copy(alpha = 0.25f),
                    selectedLabelColor = AmberGold,
                ),
            )
        }
    }
}

@Composable
private fun OutfitItemCard(
    item: CatalogItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        isEquipped -> EmeraldSafe
        isHighlighted -> AmberGold
        else -> item.rarity.color.copy(alpha = 0.45f)
    }

    Surface(
        color = CharcoalPanel,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (isHighlighted || isEquipped) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("item_card_${item.id}"),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RarityChip(item.rarity)
                if (isEquipped) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Equipped",
                        tint = EmeraldSafe,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Color swatch preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(item.primaryColor.takeIf { it != Color.Transparent } ?: SteelSlate)
                    .border(1.dp, item.accentColor.copy(alpha = 0.8f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.styleTag,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (item.primaryColor == Color(0xFFF8FAFC) || item.primaryColor == Color(0xFFE2E8F0)) {
                        TacticalObsidian
                    } else {
                        Color.White
                    },
                )
            }

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (isOwned) {
                Text(
                    text = if (isEquipped) "EQUIPPED" else "OWNED • TAP TO EQUIP",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isEquipped) EmeraldSafe else AmberGold,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item.diamondPrice?.let {
                        Text(
                            text = "${formatNumber(it)} 💎",
                            style = MaterialTheme.typography.labelSmall,
                            color = DiamondCyan,
                        )
                    }
                    item.goldPrice?.let {
                        Text(
                            text = "${formatNumber(it)} 🪙",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberGold,
                        )
                    }
                }
            }
        }
    }
}

private fun isEquippedInProfile(profile: UserProfile, item: CatalogItem): Boolean = when (item.category) {
    ItemCategory.HAT -> profile.equippedHatId == item.id
    ItemCategory.MASK -> profile.equippedMaskId == item.id
    ItemCategory.TOP -> profile.equippedTopId == item.id
    ItemCategory.BOTTOM -> profile.equippedBottomId == item.id
    ItemCategory.SHOES -> profile.equippedShoesId == item.id
    ItemCategory.WEAPON_SKIN -> profile.equippedWeaponSkinId == item.id
}
