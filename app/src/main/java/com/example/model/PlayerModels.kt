package com.example.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.RarityCommon
import com.example.ui.theme.RarityEpic
import com.example.ui.theme.RarityLegendary
import com.example.ui.theme.RarityRare
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ItemCategory(val label: String) {
    HAT("Headgear"),
    MASK("Mask"),
    TOP("Tops"),
    BOTTOM("Bottoms"),
    SHOES("Shoes"),
    WEAPON_SKIN("Weapons");

    companion object {
        fun safeValueOf(raw: String): ItemCategory =
            entries.firstOrNull { it.name == raw } ?: TOP
    }
}

enum class Rarity(val label: String, val color: Color) {
    COMMON("Common", RarityCommon),
    RARE("Rare", RarityRare),
    EPIC("Epic", RarityEpic),
    LEGENDARY("Legendary", RarityLegendary);

    companion object {
        fun safeValueOf(raw: String): Rarity =
            entries.firstOrNull { it.name == raw } ?: COMMON
    }
}

enum class GameMode(val title: String, val subtitle: String, val mapTag: String) {
    BERMUDA_SOLO("BERMUDA", "Classic Island • 20 Survivors", "CLASSIC"),
    PURGATORY_SOLO("PURGATORY", "Highland Rivers • 20 Survivors", "CLASSIC");

    companion object {
        fun safeValueOf(raw: String): GameMode =
            entries.firstOrNull { it.name == raw } ?: BERMUDA_SOLO
    }
}

enum class RoyaleType(val title: String, val subtitle: String, val usesDiamonds: Boolean, val singleCost: Long, val tenCost: Long) {
    DIAMOND_ROYALE("DIAMOND ROYALE", "Classic Bundles & Rare Outfits", true, 60L, 600L),
    GOLD_ROYALE("GOLD ROYALE", "Classic Street Gear & Coins", false, 300L, 3000L),
    WEAPON_ROYALE("WEAPON ROYALE", "Iconic Gun Skins + Damage Flair", true, 40L, 400L),
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val callsign: String = "SURVIVOR",
    val goldCoins: Long = 2500L,
    val diamonds: Long = 300L,
    val level: Long = 1L,
    val xp: Long = 0L,
    val kills: Long = 0L,
    val booyahs: Long = 0L,
    val matchesPlayed: Long = 0L,
    val luckPoints: Long = 0L,
    val equippedHatId: String = "hat_red_bandana",
    val equippedMaskId: String = "mask_none",
    val equippedTopId: String = "top_classic_tank",
    val equippedBottomId: String = "bottom_cargo_khaki",
    val equippedShoesId: String = "shoes_combat_boots",
    val equippedWeaponSkinId: String = "skin_default",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class OwnedItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemId: String = "",
    val category: String = ItemCategory.TOP.name,
    val name: String = "",
    val rarity: String = Rarity.COMMON.name,
    val acquiredVia: String = "STARTER",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class MatchRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = GameMode.BERMUDA_SOLO.name,
    val placement: Long = 20L,
    val kills: Long = 0L,
    val damageDealt: Long = 0L,
    val survivalSeconds: Long = 0L,
    val goldEarned: Long = 0L,
    val xpEarned: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CurrencyTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val txType: String = "TOPUP_PACK",
    val currency: String = "DIAMONDS",
    val amountDelta: Long = 0L,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

data class CatalogItem(
    val id: String,
    val name: String,
    val description: String,
    val category: ItemCategory,
    val rarity: Rarity,
    val diamondPrice: Long?, // null if not sold for diamonds
    val goldPrice: Long?,    // null if not sold for gold
    val primaryColor: Color,
    val accentColor: Color,
    val styleTag: String,
    val isStarter: Boolean = false,
)

data class CurrencyPack(
    val id: String,
    val title: String,
    val amount: Long,
    val bonusLabel: String,
    val isDiamonds: Boolean,
    val priceTag: String,
    val goldPackCostInDiamonds: Long = 0L,
)

data class SpinDrop(
    val item: CatalogItem?,
    val bonusGold: Long = 0L,
    val wasDuplicate: Boolean = false,
    val label: String,
    val rarity: Rarity,
)

data class SpinOutcome(
    val drops: List<SpinDrop>,
    val newLuckPoints: Long,
    val spentCurrency: String,
    val spentAmount: Long,
)

data class MatchRewardSummary(
    val placement: Int,
    val kills: Int,
    val damageDealt: Int,
    val survivalSeconds: Int,
    val goldEarned: Long,
    val xpEarned: Long,
    val isBooyah: Boolean,
)
