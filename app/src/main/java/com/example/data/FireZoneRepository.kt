package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CatalogItem
import com.example.model.CurrencyTransaction
import com.example.model.GameCatalog
import com.example.model.GameMode
import com.example.model.ItemCategory
import com.example.model.MatchRecord
import com.example.model.MatchRewardSummary
import com.example.model.OwnedItem
import com.example.model.Rarity
import com.example.model.RoyaleType
import com.example.model.SpinDrop
import com.example.model.SpinOutcome
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class FireZoneRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        }
        return uid
    }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeOwnedItems(userId: String): Flow<List<OwnedItem>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/ownedItems"
        emitAll(
            db.collection("users").document(uid).collection("ownedItems")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        OwnedItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMatchHistory(userId: String): Flow<List<MatchRecord>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/matchHistory"
        emitAll(
            db.collection("users").document(uid).collection("matchHistory")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatchRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTransactions(userId: String): Flow<List<CurrencyTransaction>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CurrencyTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureStarterProfile(userId: String, displayName: String?): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val userRef = db.collection("users").document(uid)
            val existing = userRef.get().await()
            if (!existing.exists()) {
                val safeCallsign = (displayName ?: "SURVIVOR")
                    .trim()
                    .replace(Regex("\\s+"), " ")
                    .ifBlank { "SURVIVOR" }
                    .take(24)
                    .let { if (it.length < 2) "SURVIVOR" else it }

                val batch = db.batch()
                val profilePayload = mapOf(
                    "userId" to uid,
                    "callsign" to safeCallsign,
                    "goldCoins" to 2500L,
                    "diamonds" to 300L,
                    "level" to 1L,
                    "xp" to 0L,
                    "kills" to 0L,
                    "booyahs" to 0L,
                    "matchesPlayed" to 0L,
                    "luckPoints" to 0L,
                    "equippedHatId" to "hat_red_bandana",
                    "equippedMaskId" to "mask_none",
                    "equippedTopId" to "top_classic_tank",
                    "equippedBottomId" to "bottom_cargo_khaki",
                    "equippedShoesId" to "shoes_combat_boots",
                    "equippedWeaponSkinId" to "skin_default",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                batch.set(userRef, profilePayload)

                GameCatalog.items.filter { it.isStarter }.forEach { item ->
                    val itemRef = userRef.collection("ownedItems").document(item.id)
                    batch.set(
                        itemRef,
                        mapOf(
                            "userId" to uid,
                            "itemId" to item.id,
                            "category" to item.category.name,
                            "name" to item.name,
                            "rarity" to item.rarity.name,
                            "acquiredVia" to "STARTER",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun grantCurrencyPack(
        userId: String,
        isDiamonds: Boolean,
        amount: Long,
        isCustomHoldGrant: Boolean,
        note: String,
        diamondCostForGoldPack: Long = 0L,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            require(amount in 1L..999_999_999L) { "Amount must be between 1 and 999,999,999." }
            require(diamondCostForGoldPack >= 0L) { "Invalid cost." }

            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("transactions").document()
            val safeNote = note.trim().ifBlank { "Currency Grant" }.take(100)

            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                if (!snap.exists()) error("Player profile not initialized yet.")

                val currentDiamonds = snap.getLong("diamonds") ?: 0L
                val currentGold = snap.getLong("goldCoins") ?: 0L

                if (!isDiamonds && diamondCostForGoldPack > 0L) {
                    if (currentDiamonds < diamondCostForGoldPack) {
                        error("Not enough Diamonds (need $diamondCostForGoldPack).")
                    }
                }

                val updates = mutableMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (isDiamonds) {
                    updates["diamonds"] = (currentDiamonds + amount).coerceAtMost(999_999_999L)
                } else {
                    updates["goldCoins"] = (currentGold + amount).coerceAtMost(999_999_999L)
                    if (diamondCostForGoldPack > 0L) {
                        updates["diamonds"] = (currentDiamonds - diamondCostForGoldPack).coerceAtLeast(0L)
                    }
                }
                tx.update(userRef, updates)

                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to if (isCustomHoldGrant) "CUSTOM_GRANT" else "TOPUP_PACK",
                        "currency" to if (isDiamonds) "DIAMONDS" else "GOLD",
                        "amountDelta" to amount,
                        "note" to safeNote,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun purchaseCatalogItem(
        userId: String,
        item: CatalogItem,
        useDiamonds: Boolean,
    ): Result<Unit> {
        val path = "users/$userId/ownedItems/${item.id}"
        return try {
            val uid = requireUserId(userId)
            val price = if (useDiamonds) item.diamondPrice else item.goldPrice
            require(price != null && price >= 0L) { "${item.name} is not available for this currency." }

            val userRef = db.collection("users").document(uid)
            val ownedRef = userRef.collection("ownedItems").document(item.id)
            val txRef = userRef.collection("transactions").document()

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) error("Player profile not ready.")
                val ownedSnap = tx.get(ownedRef)
                if (ownedSnap.exists()) error("You already own ${item.name}!")

                val currentDiamonds = userSnap.getLong("diamonds") ?: 0L
                val currentGold = userSnap.getLong("goldCoins") ?: 0L

                val updates = mutableMapOf<String, Any>(
                    equipFieldFor(item.category) to item.id,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )

                if (useDiamonds) {
                    if (currentDiamonds < price) error("Need $price Diamonds! Hold a Diamond Pack for 3s in Top-Up to grant more.")
                    updates["diamonds"] = currentDiamonds - price
                } else {
                    if (currentGold < price) error("Need $price Gold Coins!")
                    updates["goldCoins"] = currentGold - price
                }

                tx.update(userRef, updates)
                tx.set(
                    ownedRef,
                    mapOf(
                        "userId" to uid,
                        "itemId" to item.id,
                        "category" to item.category.name,
                        "name" to item.name,
                        "rarity" to item.rarity.name,
                        "acquiredVia" to "STORE",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "STORE_PURCHASE",
                        "currency" to if (useDiamonds) "DIAMONDS" else "GOLD",
                        "amountDelta" to -price,
                        "note" to "Bought ${item.name}".take(100),
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun equipItem(userId: String, item: CatalogItem): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid).update(
                mapOf(
                    equipFieldFor(item.category) to item.id,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun performLuckSpin(
        userId: String,
        royaleType: RoyaleType,
        isTenPlusOne: Boolean,
    ): Result<SpinOutcome> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val spinDraws = if (isTenPlusOne) 11 else 1
            val cost = if (isTenPlusOne) royaleType.tenCost else royaleType.singleCost
            val pool = GameCatalog.poolForRoyale(royaleType)
            require(pool.isNotEmpty()) { "Royale pool is empty." }

            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("transactions").document()

            val outcome = db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) error("Player profile not loaded.")

                val currentDiamonds = userSnap.getLong("diamonds") ?: 0L
                val currentGold = userSnap.getLong("goldCoins") ?: 0L
                var luck = userSnap.getLong("luckPoints") ?: 0L

                if (royaleType.usesDiamonds) {
                    if (currentDiamonds < cost) {
                        error("Need $cost Diamonds for ${royaleType.title}. Visit Top-Up!")
                    }
                } else {
                    if (currentGold < cost) {
                        error("Need $cost Gold Coins for ${royaleType.title}!")
                    }
                }

                // Read ownership state of all items in this pool BEFORE any writes
                val ownedInPool = mutableSetOf<String>()
                for (candidate in pool) {
                    val snap = tx.get(userRef.collection("ownedItems").document(candidate.id))
                    if (snap.exists()) {
                        ownedInPool.add(candidate.id)
                    }
                }

                val drops = mutableListOf<SpinDrop>()
                val newlyWonItems = mutableListOf<CatalogItem>()
                var bonusGoldEarned = 0L

                val highTierPool = pool.filter { it.rarity == Rarity.LEGENDARY || it.rarity == Rarity.EPIC }
                    .ifEmpty { pool }

                repeat(spinDraws) {
                    val roll = Random.nextInt(100)
                    val guaranteedGrand = luck >= 95L || (isTenPlusOne && it == spinDraws - 1 && Random.nextInt(100) < 45)
                    val wonItem: CatalogItem? = when {
                        guaranteedGrand -> {
                            luck = 0L
                            val unowned = highTierPool.filter { item -> item.id !in ownedInPool }
                            (unowned.ifEmpty { highTierPool }).random()
                        }
                        roll < 18 -> {
                            luck = (luck + 8L).coerceAtMost(100L)
                            val unowned = highTierPool.filter { item -> item.id !in ownedInPool }
                            (unowned.ifEmpty { highTierPool }).random()
                        }
                        roll < 62 -> {
                            luck = (luck + 6L).coerceAtMost(100L)
                            val unowned = pool.filter { item -> item.id !in ownedInPool }
                            (unowned.ifEmpty { pool }).random()
                        }
                        else -> {
                            luck = (luck + 5L).coerceAtMost(100L)
                            null
                        }
                    }

                    if (wonItem != null) {
                        if (wonItem.id in ownedInPool) {
                            val dupGold = when (wonItem.rarity) {
                                Rarity.LEGENDARY -> 600L
                                Rarity.EPIC -> 350L
                                Rarity.RARE -> 200L
                                Rarity.COMMON -> 100L
                            }
                            bonusGoldEarned += dupGold
                            drops.add(
                                SpinDrop(
                                    item = wonItem,
                                    bonusGold = dupGold,
                                    wasDuplicate = true,
                                    label = "${wonItem.name} (+$dupGold Gold)",
                                    rarity = wonItem.rarity,
                                )
                            )
                        } else {
                            ownedInPool.add(wonItem.id)
                            newlyWonItems.add(wonItem)
                            drops.add(
                                SpinDrop(
                                    item = wonItem,
                                    wasDuplicate = false,
                                    label = wonItem.name,
                                    rarity = wonItem.rarity,
                                )
                            )
                        }
                    } else {
                        val goldCoinDrop = if (royaleType.usesDiamonds) {
                            listOf(150L, 250L, 400L).random()
                        } else {
                            listOf(100L, 180L, 300L).random()
                        }
                        bonusGoldEarned += goldCoinDrop
                        drops.add(
                            SpinDrop(
                                item = null,
                                bonusGold = goldCoinDrop,
                                wasDuplicate = false,
                                label = "+$goldCoinDrop Gold Coins",
                                rarity = Rarity.RARE,
                            )
                        )
                    }
                }

                val newDiamonds = if (royaleType.usesDiamonds) currentDiamonds - cost else currentDiamonds
                val newGold = ((if (royaleType.usesDiamonds) currentGold else currentGold - cost) + bonusGoldEarned)
                    .coerceIn(0L, 999_999_999L)

                val profileUpdates = mutableMapOf<String, Any>(
                    "diamonds" to newDiamonds,
                    "goldCoins" to newGold,
                    "luckPoints" to luck,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                // Auto-equip the highest rarity newly unlocked item
                newlyWonItems.maxByOrNull { it.rarity.ordinal }?.let { best ->
                    profileUpdates[equipFieldFor(best.category)] = best.id
                }
                tx.update(userRef, profileUpdates)

                val acquiredVia = when (royaleType) {
                    RoyaleType.DIAMOND_ROYALE -> "DIAMOND_ROYALE"
                    RoyaleType.GOLD_ROYALE -> "GOLD_ROYALE"
                    RoyaleType.WEAPON_ROYALE -> "WEAPON_ROYALE"
                }

                newlyWonItems.forEach { item ->
                    tx.set(
                        userRef.collection("ownedItems").document(item.id),
                        mapOf(
                            "userId" to uid,
                            "itemId" to item.id,
                            "category" to item.category.name,
                            "name" to item.name,
                            "rarity" to item.rarity.name,
                            "acquiredVia" to acquiredVia,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }

                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "LUCK_SPIN",
                        "currency" to if (royaleType.usesDiamonds) "DIAMONDS" else "GOLD",
                        "amountDelta" to -cost,
                        "note" to "${royaleType.title} x$spinDraws".take(100),
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )

                SpinOutcome(
                    drops = drops,
                    newLuckPoints = luck,
                    spentCurrency = if (royaleType.usesDiamonds) "DIAMONDS" else "GOLD",
                    spentAmount = cost,
                )
            }.await()

            Result.success(outcome)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordMatchResult(
        userId: String,
        mode: GameMode,
        placement: Int,
        kills: Int,
        damageDealt: Int,
        survivalSeconds: Int,
    ): Result<MatchRewardSummary> {
        val path = "users/$userId/matchHistory"
        return try {
            val uid = requireUserId(userId)
            val clampedPlacement = placement.coerceIn(1, 20)
            val clampedKills = kills.coerceIn(0, 50)
            val clampedDamage = damageDealt.coerceIn(0, 50000)
            val clampedSurvival = survivalSeconds.coerceIn(0, 3600)
            val isBooyah = clampedPlacement == 1

            val goldEarned = (if (isBooyah) 350L else ((21 - clampedPlacement) * 14L)) + (clampedKills * 35L)
            val xpEarned = (if (isBooyah) 500L else ((21 - clampedPlacement) * 18L)) + (clampedKills * 45L)

            val userRef = db.collection("users").document(uid)
            val matchRef = userRef.collection("matchHistory").document()
            val txRef = userRef.collection("transactions").document()

            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                if (!snap.exists()) error("Player profile not ready.")

                val currentGold = snap.getLong("goldCoins") ?: 0L
                val currentXp = snap.getLong("xp") ?: 0L
                val currentKills = snap.getLong("kills") ?: 0L
                val currentBooyahs = snap.getLong("booyahs") ?: 0L
                val currentMatches = snap.getLong("matchesPlayed") ?: 0L

                val newXp = (currentXp + xpEarned).coerceAtMost(999_999_999L)
                val newLevel = ((newXp / 500L) + 1L).coerceIn(1L, 100L)

                tx.update(
                    userRef,
                    mapOf(
                        "goldCoins" to (currentGold + goldEarned).coerceAtMost(999_999_999L),
                        "xp" to newXp,
                        "level" to newLevel,
                        "kills" to (currentKills + clampedKills).coerceAtMost(9_999_999L),
                        "booyahs" to (currentBooyahs + if (isBooyah) 1L else 0L).coerceAtMost(9_999_999L),
                        "matchesPlayed" to (currentMatches + 1L).coerceAtMost(9_999_999L),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                tx.set(
                    matchRef,
                    mapOf(
                        "userId" to uid,
                        "mode" to mode.name,
                        "placement" to clampedPlacement,
                        "kills" to clampedKills,
                        "damageDealt" to clampedDamage,
                        "survivalSeconds" to clampedSurvival,
                        "goldEarned" to goldEarned,
                        "xpEarned" to xpEarned,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "MATCH_REWARD",
                        "currency" to "GOLD",
                        "amountDelta" to goldEarned,
                        "note" to (if (isBooyah) "BOOYAH! #1 ($clampedKills Kills)" else "Match #$clampedPlacement ($clampedKills Kills)").take(100),
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )
            }.await()

            Result.success(
                MatchRewardSummary(
                    placement = clampedPlacement,
                    kills = clampedKills,
                    damageDealt = clampedDamage,
                    survivalSeconds = clampedSurvival,
                    goldEarned = goldEarned,
                    xpEarned = xpEarned,
                    isBooyah = isBooyah,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private fun equipFieldFor(category: ItemCategory): String = when (category) {
        ItemCategory.HAT -> "equippedHatId"
        ItemCategory.MASK -> "equippedMaskId"
        ItemCategory.TOP -> "equippedTopId"
        ItemCategory.BOTTOM -> "equippedBottomId"
        ItemCategory.SHOES -> "equippedShoesId"
        ItemCategory.WEAPON_SKIN -> "equippedWeaponSkinId"
    }
}
