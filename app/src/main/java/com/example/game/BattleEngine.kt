package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.model.GameMode
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

enum class WeaponType(
    val displayName: String,
    val damage: Float,
    val fireInterval: Float,
    val range: Float,
    val magSize: Int,
    val reloadSeconds: Float,
    val bulletSpeed: Float,
    val pellets: Int,
    val spreadRad: Float,
    val badgeColor: Color,
) {
    G18("G18 Pistol", 16f, 0.28f, 340f, 15, 1.2f, 820f, 1, 0.05f, Color(0xFF94A3B8)),
    MP40("MP40 SMG", 19f, 0.09f, 360f, 24, 1.4f, 920f, 1, 0.09f, Color(0xFF38BDF8)),
    M4A1("M4A1 Rifle", 26f, 0.12f, 520f, 30, 1.7f, 1080f, 1, 0.04f, Color(0xFF22C55E)),
    SCAR("SCAR Titan", 28f, 0.13f, 510f, 30, 1.7f, 1050f, 1, 0.045f, Color(0xFFA855F7)),
    AK47("AK47 Dragon", 35f, 0.15f, 540f, 30, 1.9f, 1120f, 1, 0.07f, Color(0xFFEF4444)),
    M1014("M1014 Shotgun", 19f, 0.62f, 240f, 6, 2.1f, 880f, 5, 0.24f, Color(0xFFF97316)),
    AWM("AWM Sniper", 112f, 0.95f, 820f, 5, 2.3f, 1650f, 1, 0.008f, Color(0xFFFFB300)),
}

enum class LootKind {
    WEAPON,
    AMMO,
    MEDKIT,
    ARMOR_VEST,
    GLOO_WALL,
}

data class WorldPoi(
    val name: String,
    val x: Float,
    val y: Float,
)

data class Obstacle(
    val id: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val kind: ObstacleKind,
    var hp: Float = 9999f,
    var maxHp: Float = 9999f,
)

enum class ObstacleKind {
    BUILDING_WALL,
    CRATE,
    TREE,
    BARREL,
    GLOO_WALL,
}

data class GroundLoot(
    val id: Int,
    val x: Float,
    val y: Float,
    val kind: LootKind,
    val weapon: WeaponType? = null,
    val amount: Int = 1,
)

data class WeaponSlot(
    val type: WeaponType,
    var ammoInMag: Int,
    var reserveAmmo: Int,
)

data class BotEntity(
    val id: Int,
    val name: String,
    var x: Float,
    var y: Float,
    var angle: Float,
    var hp: Float,
    val maxHp: Float = 180f,
    var armor: Float = 40f,
    var weapon: WeaponType,
    var fireCooldown: Float = 0f,
    var aiTargetX: Float,
    var aiTargetY: Float,
    var aiRepathTimer: Float = 0f,
    var alive: Boolean = true,
    val hatColor: Color,
    val shirtColor: Color,
)

data class Bullet(
    val id: Int,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    val ownerId: Int, // 0 = player, >0 = bot id
    var distanceTraveled: Float = 0f,
    val maxRange: Float,
    val color: Color,
    val isHeadshotRoll: Boolean,
)

data class FloatingPopup(
    val id: Int,
    var x: Float,
    var y: Float,
    val text: String,
    val color: Color,
    var ttl: Float = 0.85f,
)

data class KillFeedEntry(
    val id: Int,
    val killer: String,
    val victim: String,
    val weaponName: String,
    val isPlayerInvolved: Boolean,
    var ttl: Float = 4.5f,
)

data class BattleHudSnapshot(
    val hp: Float = 200f,
    val maxHp: Float = 200f,
    val armor: Float = 50f,
    val aliveCount: Int = 20,
    val totalSurvivors: Int = 20,
    val kills: Int = 0,
    val damageDealt: Int = 0,
    val survivalSeconds: Int = 0,
    val medkits: Int = 2,
    val glooWalls: Int = 2,
    val activeSlotIndex: Int = 0,
    val weaponSlots: List<WeaponSlot> = emptyList(),
    val isReloading: Boolean = false,
    val isHealing: Boolean = false,
    val inStorm: Boolean = false,
    val zonePhase: Int = 1,
    val zoneTimerSeconds: Int = 25,
    val killFeed: List<KillFeedEntry> = emptyList(),
    val matchEnded: Boolean = false,
    val placement: Int = 20,
    val nearestLootPrompt: String? = null,
)

class BattleEngine(
    val mode: GameMode,
    val playerHatColor: Color,
    val playerTopColor: Color,
    val playerPantsColor: Color,
    val weaponTintColor: Color,
) {
    val worldSize = 2400f
    val playerRadius = 18f

    var playerX = 1200f
    var playerY = 1200f
    var playerAngle = 0f
    var playerHp = 200f
    val playerMaxHp = 200f
    var playerArmor = 50f
    var playerAlive = true
    var playerKills = 0
    var playerDamage = 0
    var elapsedSeconds = 0f

    var medkits = 2
    var glooWalls = 3
    var activeSlotIndex = 0
    val weaponSlots = mutableListOf(
        WeaponSlot(WeaponType.G18, WeaponType.G18.magSize, 60),
    )

    var fireCooldown = 0f
    var reloadTimer = 0f
    var healTimer = 0f
    var nextEntityId = 100

    // Safe zone state
    var zoneCenterX = 1200f
    var zoneCenterY = 1200f
    var zoneRadius = 1320f
    var targetZoneRadius = 900f
    var zonePhase = 1
    var zoneTimer = 24f

    val pois: List<WorldPoi>
    val obstacles = mutableListOf<Obstacle>()
    val groundLoot = mutableListOf<GroundLoot>()
    val bots = mutableListOf<BotEntity>()
    val bullets = mutableListOf<Bullet>()
    val popups = mutableListOf<FloatingPopup>()
    val killFeed = mutableListOf<KillFeedEntry>()

    var matchEnded = false
    var finalPlacement = 20
    var nearestLootPrompt: String? = null

    init {
        pois = if (mode == GameMode.BERMUDA_SOLO) {
            listOf(
                WorldPoi("CLOCK TOWER", 1200f, 1200f),
                WorldPoi("PEAK", 1220f, 520f),
                WorldPoi("POCHINOK", 520f, 1680f),
                WorldPoi("FACTORY", 1840f, 1660f),
                WorldPoi("MILL", 560f, 540f),
                WorldPoi("CAPE TOWN", 1880f, 580f),
            )
        } else {
            listOf(
                WorldPoi("BRASILIA", 1200f, 1200f),
                WorldPoi("MARBLEWORKS", 560f, 620f),
                WorldPoi("FIELDS", 1820f, 600f),
                WorldPoi("QUARRY", 580f, 1740f),
                WorldPoi("CROSSROADS", 1800f, 1740f),
                WorldPoi("FORGE", 1220f, 480f),
            )
        }

        buildOpenWorld()
        spawnLootField()
        spawnPlayerSafe()
        spawnBots(19)
    }

    private fun buildOpenWorld() {
        var obsId = 1
        // Build compounds around POIs
        for (poi in pois) {
            val px = poi.x
            val py = poi.y
            // Four-corner tactical buildings with open doorways
            obstacles.add(Obstacle(obsId++, px - 140f, py - 130f, 110f, 22f, ObstacleKind.BUILDING_WALL))
            obstacles.add(Obstacle(obsId++, px - 140f, py - 130f, 22f, 95f, ObstacleKind.BUILDING_WALL))
            obstacles.add(Obstacle(obsId++, px + 40f, py - 130f, 110f, 22f, ObstacleKind.BUILDING_WALL))
            obstacles.add(Obstacle(obsId++, px + 128f, py - 130f, 22f, 95f, ObstacleKind.BUILDING_WALL))
            obstacles.add(Obstacle(obsId++, px - 140f, py + 108f, 105f, 22f, ObstacleKind.BUILDING_WALL))
            obstacles.add(Obstacle(obsId++, px + 45f, py + 108f, 105f, 22f, ObstacleKind.BUILDING_WALL))
            // Tactical crates inside POI
            obstacles.add(Obstacle(obsId++, px - 36f, py - 32f, 34f, 34f, ObstacleKind.CRATE))
            obstacles.add(Obstacle(obsId++, px + 24f, py + 18f, 34f, 34f, ObstacleKind.CRATE))
        }

        // Scatter trees, crates, and explosive red barrels across the open world
        val rng = Random(if (mode == GameMode.BERMUDA_SOLO) 2017 else 2018)
        repeat(65) {
            val x = rng.nextInt(140, 2260).toFloat()
            val y = rng.nextInt(140, 2260).toFloat()
            if (pois.none { hypot(x - it.x, y - it.y) < 185f }) {
                val roll = rng.nextInt(10)
                when {
                    roll < 6 -> obstacles.add(Obstacle(obsId++, x, y, 42f, 42f, ObstacleKind.TREE))
                    roll < 9 -> obstacles.add(Obstacle(obsId++, x, y, 36f, 36f, ObstacleKind.CRATE))
                    else -> obstacles.add(
                        Obstacle(obsId++, x, y, 28f, 28f, ObstacleKind.BARREL, hp = 80f, maxHp = 80f)
                    )
                }
            }
        }
    }

    private fun spawnLootField() {
        var lootId = 1
        val weapons = listOf(
            WeaponType.MP40,
            WeaponType.M4A1,
            WeaponType.AK47,
            WeaponType.SCAR,
            WeaponType.M1014,
            WeaponType.AWM,
        )
        // High-density loot at every POI
        for (poi in pois) {
            repeat(4) { idx ->
                val angle = idx * (PI.toFloat() / 2f) + 0.3f
                val dist = 55f + (idx % 2) * 35f
                groundLoot.add(
                    GroundLoot(
                        id = lootId++,
                        x = poi.x + cos(angle) * dist,
                        y = poi.y + sin(angle) * dist,
                        kind = LootKind.WEAPON,
                        weapon = weapons.random(),
                        amount = 1,
                    )
                )
            }
            groundLoot.add(GroundLoot(lootId++, poi.x, poi.y - 52f, LootKind.MEDKIT, amount = 1))
            groundLoot.add(GroundLoot(lootId++, poi.x + 60f, poi.y, LootKind.ARMOR_VEST, amount = 45))
            groundLoot.add(GroundLoot(lootId++, poi.x - 62f, poi.y + 42f, LootKind.GLOO_WALL, amount = 2))
        }

        // Open-field loot
        repeat(45) {
            val x = Random.nextInt(180, 2220).toFloat()
            val y = Random.nextInt(180, 2220).toFloat()
            val kindRoll = Random.nextInt(100)
            when {
                kindRoll < 35 -> groundLoot.add(
                    GroundLoot(lootId++, x, y, LootKind.WEAPON, weapon = weapons.random())
                )
                kindRoll < 60 -> groundLoot.add(GroundLoot(lootId++, x, y, LootKind.AMMO, amount = 30))
                kindRoll < 78 -> groundLoot.add(GroundLoot(lootId++, x, y, LootKind.MEDKIT, amount = 1))
                kindRoll < 90 -> groundLoot.add(GroundLoot(lootId++, x, y, LootKind.ARMOR_VEST, amount = 40))
                else -> groundLoot.add(GroundLoot(lootId++, x, y, LootKind.GLOO_WALL, amount = 1))
            }
        }
    }

    private fun spawnPlayerSafe() {
        // Drop near a random POI clearing so combat & looting start fast
        val startPoi = pois.first()
        playerX = startPoi.x + 195f
        playerY = startPoi.y + 45f
    }

    private fun spawnBots(count: Int) {
        val botNames = listOf(
            "BOT_Shadow", "BOT_Raistar", "BOT_Viper", "BOT_Cobra", "BOT_Blaze",
            "BOT_B2K", "BOT_Ghost", "BOT_Skylord", "BOT_Titan", "BOT_Kira",
            "BOT_SniperX", "BOT_Rusher", "BOT_Bermuda", "BOT_Sakura", "BOT_Alpha",
            "BOT_Zero", "BOT_Falcon", "BOT_Criminal", "BOT_Legend"
        )
        val botColors = listOf(
            Color(0xFFEF4444) to Color(0xFF991B1B),
            Color(0xFF3B82F6) to Color(0xFF1D4ED8),
            Color(0xFFA855F7) to Color(0xFF6B21A8),
            Color(0xFF22C55E) to Color(0xFF15803D),
            Color(0xFFF97316) to Color(0xFF9A3412),
        )
        val botWeapons = listOf(WeaponType.G18, WeaponType.MP40, WeaponType.M4A1, WeaponType.AK47, WeaponType.SCAR)

        for (i in 0 until count) {
            var bx: Float
            var by: Float
            var tries = 0
            do {
                bx = Random.nextInt(200, 2200).toFloat()
                by = Random.nextInt(200, 2200).toFloat()
                tries++
            } while (tries < 20 && (hypot(bx - playerX, by - playerY) < 320f || collidesWithObstacle(bx, by, playerRadius)))

            val palette = botColors[i % botColors.size]
            bots.add(
                BotEntity(
                    id = i + 1,
                    name = botNames[i % botNames.size],
                    x = bx,
                    y = by,
                    angle = Random.nextFloat() * (2f * PI.toFloat()),
                    hp = 160f + Random.nextInt(0, 40),
                    weapon = botWeapons.random(),
                    aiTargetX = bx + Random.nextInt(-250, 250),
                    aiTargetY = by + Random.nextInt(-250, 250),
                    hatColor = palette.first,
                    shirtColor = palette.second,
                )
            )
        }
    }

    fun step(
        dtRaw: Float,
        moveX: Float,
        moveY: Float,
        aimX: Float,
        aimY: Float,
        isManualFiring: Boolean,
        autoFireTap: Boolean,
    ) {
        if (matchEnded) return
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        elapsedSeconds += dt

        // Update Zone shrink
        zoneTimer -= dt
        if (zoneTimer <= 0f) {
            zonePhase++
            targetZoneRadius = (targetZoneRadius * 0.68f).coerceAtLeast(140f)
            zoneTimer = 22f
        }
        if (zoneRadius > targetZoneRadius) {
            zoneRadius = (zoneRadius - 22f * dt).coerceAtLeast(targetZoneRadius)
        }

        // Update player movement
        if (playerAlive) {
            val moveMag = hypot(moveX, moveY)
            if (moveMag > 0.08f) {
                val speed = if (healTimer > 0f) 115f else 225f
                val nx = moveX / moveMag
                val ny = moveY / moveMag
                moveEntityWithCollision(playerX, playerY, nx * speed * dt, ny * speed * dt, playerRadius).let {
                    playerX = it.first
                    playerY = it.second
                }
                if (hypot(aimX, aimY) <= 0.15f) {
                    playerAngle = atan2(ny, nx)
                }
            }

            // Aim & Fire
            val aimMag = hypot(aimX, aimY)
            if (aimMag > 0.15f) {
                playerAngle = atan2(aimY, aimX)
            } else if (autoFireTap) {
                val nearest = findNearestBotInView(playerX, playerY, activeWeapon().type.range)
                if (nearest != null) {
                    playerAngle = atan2(nearest.y - playerY, nearest.x - playerX)
                }
            }

            if (reloadTimer > 0f) {
                reloadTimer -= dt
                if (reloadTimer <= 0f) {
                    finishReload()
                }
            }

            if (healTimer > 0f) {
                healTimer -= dt
                if (healTimer <= 0f) {
                    playerHp = (playerHp + 75f).coerceAtMost(playerMaxHp)
                    popups.add(FloatingPopup(nextEntityId++, playerX, playerY - 26f, "+75 HP", Color(0xFF22C55E)))
                }
            }

            if (fireCooldown > 0f) fireCooldown -= dt
            if ((isManualFiring || autoFireTap) && reloadTimer <= 0f && fireCooldown <= 0f) {
                firePlayerWeapon()
            }

            // Auto-pickup ammo, medkits, gloo walls, or second weapon slot
            handlePlayerLootPickup()

            // Storm damage
            if (hypot(playerX - zoneCenterX, playerY - zoneCenterY) > zoneRadius) {
                playerHp -= (5f + zonePhase * 2.5f) * dt
                if (playerHp <= 0f) {
                    playerHp = 0f
                    eliminatePlayer("Blue Storm")
                }
            }
        }

        // Update AI Bots
        updateBots(dt)

        // Update Bullets
        updateBullets(dt)

        // Update Popups & Kill Feed
        popups.forEach {
            it.y -= 32f * dt
            it.ttl -= dt
        }
        popups.removeAll { it.ttl <= 0f }

        killFeed.forEach { it.ttl -= dt }
        killFeed.removeAll { it.ttl <= 0f }

        // Check Victory / Booyah
        val aliveBots = bots.count { it.alive }
        if (playerAlive && aliveBots == 0) {
            matchEnded = true
            finalPlacement = 1
        }
    }

    fun activeWeapon(): WeaponSlot = weaponSlots[activeSlotIndex.coerceIn(0, weaponSlots.lastIndex)]

    fun switchWeapon(slotIndex: Int) {
        if (slotIndex in weaponSlots.indices && slotIndex != activeSlotIndex) {
            activeSlotIndex = slotIndex
            reloadTimer = 0f
        }
    }

    fun startReload() {
        val slot = activeWeapon()
        if (reloadTimer <= 0f && slot.ammoInMag < slot.type.magSize && slot.reserveAmmo > 0) {
            reloadTimer = slot.type.reloadSeconds
        }
    }

    private fun finishReload() {
        val slot = activeWeapon()
        val needed = slot.type.magSize - slot.ammoInMag
        val load = minOf(needed, slot.reserveAmmo)
        slot.ammoInMag += load
        slot.reserveAmmo -= load
    }

    fun startMedkit() {
        if (medkits > 0 && healTimer <= 0f && playerHp < playerMaxHp) {
            medkits--
            healTimer = 1.6f
        }
    }

    fun deployGlooWall() {
        if (glooWalls <= 0 || !playerAlive) return
        glooWalls--
        val gx = (playerX + cos(playerAngle) * 56f).coerceIn(60f, worldSize - 60f)
        val gy = (playerY + sin(playerAngle) * 56f).coerceIn(60f, worldSize - 60f)
        val isHorizontal = kotlin.math.abs(sin(playerAngle)) > kotlin.math.abs(cos(playerAngle))
        val w = if (isHorizontal) 78f else 20f
        val h = if (isHorizontal) 20f else 78f
        obstacles.add(
            Obstacle(
                id = nextEntityId++,
                x = gx - w / 2f,
                y = gy - h / 2f,
                width = w,
                height = h,
                kind = ObstacleKind.GLOO_WALL,
                hp = 260f,
                maxHp = 260f,
            )
        )
        popups.add(FloatingPopup(nextEntityId++, gx, gy - 18f, "GLOO WALL", Color(0xFF38BDF8)))
    }

    fun swapNearestLootWeapon() {
        val near = groundLoot.firstOrNull {
            it.kind == LootKind.WEAPON && hypot(it.x - playerX, it.y - playerY) < 48f
        } ?: return
        val newWeapon = near.weapon ?: return
        if (weaponSlots.size < 2) {
            weaponSlots.add(WeaponSlot(newWeapon, newWeapon.magSize, newWeapon.magSize * 2))
            activeSlotIndex = weaponSlots.lastIndex
        } else {
            weaponSlots[activeSlotIndex] = WeaponSlot(newWeapon, newWeapon.magSize, newWeapon.magSize * 2)
        }
        groundLoot.remove(near)
        reloadTimer = 0f
        popups.add(FloatingPopup(nextEntityId++, playerX, playerY - 26f, "EQUIPPED ${newWeapon.displayName}", newWeapon.badgeColor))
    }

    private fun handlePlayerLootPickup() {
        val iterator = groundLoot.iterator()
        var prompt: String? = null
        while (iterator.hasNext()) {
            val item = iterator.next()
            val dist = hypot(item.x - playerX, item.y - playerY)
            if (dist < 38f) {
                when (item.kind) {
                    LootKind.AMMO -> {
                        activeWeapon().reserveAmmo += item.amount
                        popups.add(FloatingPopup(nextEntityId++, playerX, playerY - 20f, "+${item.amount} AMMO", Color(0xFFFACC15)))
                        iterator.remove()
                    }
                    LootKind.MEDKIT -> {
                        if (medkits < 5) {
                            medkits++
                            popups.add(FloatingPopup(nextEntityId++, playerX, playerY - 20f, "+1 MEDKIT", Color(0xFF22C55E)))
                            iterator.remove()
                        }
                    }
                    LootKind.ARMOR_VEST -> {
                        if (playerArmor < 100f) {
                            playerArmor = (playerArmor + item.amount).coerceAtMost(100f)
                            popups.add(FloatingPopup(nextEntityId++, playerX, playerY - 20f, "+ARMOR", Color(0xFF38BDF8)))
                            iterator.remove()
                        }
                    }
                    LootKind.GLOO_WALL -> {
                        if (glooWalls < 5) {
                            glooWalls += item.amount
                            popups.add(FloatingPopup(nextEntityId++, playerX, playerY - 20f, "+GLOO WALL", Color(0xFF38BDF8)))
                            iterator.remove()
                        }
                    }
                    LootKind.WEAPON -> {
                        val w = item.weapon
                        if (w != null && weaponSlots.size < 2 && weaponSlots.none { it.type == w }) {
                            weaponSlots.add(WeaponSlot(w, w.magSize, w.magSize * 2))
                            activeSlotIndex = weaponSlots.lastIndex
                            popups.add(FloatingPopup(nextEntityId++, playerX, playerY - 24f, "+${w.displayName}", w.badgeColor))
                            iterator.remove()
                        } else if (w != null) {
                            prompt = "SWAP: ${w.displayName}"
                        }
                    }
                }
            }
        }
        nearestLootPrompt = prompt
    }

    private fun firePlayerWeapon() {
        val slot = activeWeapon()
        if (slot.ammoInMag <= 0) {
            startReload()
            return
        }
        slot.ammoInMag--
        fireCooldown = slot.type.fireInterval
        healTimer = 0f

        val muzzleX = playerX + cos(playerAngle) * 22f
        val muzzleY = playerY + sin(playerAngle) * 22f
        repeat(slot.type.pellets) {
            val spread = (Random.nextFloat() - 0.5f) * 2f * slot.type.spreadRad
            val angle = playerAngle + spread
            val isHeadshot = Random.nextInt(100) < 22
            bullets.add(
                Bullet(
                    id = nextEntityId++,
                    x = muzzleX,
                    y = muzzleY,
                    vx = cos(angle) * slot.type.bulletSpeed,
                    vy = sin(angle) * slot.type.bulletSpeed,
                    damage = if (isHeadshot) slot.type.damage * 1.55f else slot.type.damage,
                    ownerId = 0,
                    maxRange = slot.type.range,
                    color = weaponTintColor,
                    isHeadshotRoll = isHeadshot,
                )
            )
        }
        if (slot.ammoInMag == 0 && slot.reserveAmmo > 0) {
            startReload()
        }
    }

    private fun findNearestBotInView(x: Float, y: Float, maxRange: Float): BotEntity? {
        return bots
            .filter { it.alive }
            .map { it to hypot(it.x - x, it.y - y) }
            .filter { it.second <= maxRange }
            .minByOrNull { it.second }
            ?.first
    }

    private fun updateBots(dt: Float) {
        for (bot in bots) {
            if (!bot.alive) continue
            if (bot.fireCooldown > 0f) bot.fireCooldown -= dt

            // Bot storm check
            val distFromCenter = hypot(bot.x - zoneCenterX, bot.y - zoneCenterY)
            if (distFromCenter > zoneRadius) {
                bot.hp -= (4.5f + zonePhase * 2f) * dt
                if (bot.hp <= 0f) {
                    eliminateBot(bot, "Blue Storm", "Zone", false)
                    continue
                }
            }

            // Look for nearest enemy (player or another bot)
            var targetX = 0f
            var targetY = 0f
            var targetDist = Float.MAX_VALUE
            var hasTarget = false

            if (playerAlive) {
                val d = hypot(playerX - bot.x, playerY - bot.y)
                if (d < 440f) {
                    targetDist = d
                    targetX = playerX
                    targetY = playerY
                    hasTarget = true
                }
            }
            for (other in bots) {
                if (other.id == bot.id || !other.alive) continue
                val d = hypot(other.x - bot.x, other.y - bot.y)
                if (d < 360f && d < targetDist) {
                    targetDist = d
                    targetX = other.x
                    targetY = other.y
                    hasTarget = true
                }
            }

            if (hasTarget) {
                bot.angle = atan2(targetY - bot.y, targetX - bot.x)
                // Strafe or close in
                val moveDirAngle = if (targetDist > bot.weapon.range * 0.72f) {
                    bot.angle
                } else {
                    bot.angle + (if (bot.id % 2 == 0) 1.3f else -1.3f)
                }
                val speed = 155f
                moveEntityWithCollision(
                    bot.x,
                    bot.y,
                    cos(moveDirAngle) * speed * dt,
                    sin(moveDirAngle) * speed * dt,
                    playerRadius,
                ).let {
                    bot.x = it.first
                    bot.y = it.second
                }

                // Bot fire
                if (targetDist <= bot.weapon.range * 0.92f && bot.fireCooldown <= 0f) {
                    bot.fireCooldown = bot.weapon.fireInterval * 2.1f
                    val spread = (Random.nextFloat() - 0.5f) * 0.22f
                    val a = bot.angle + spread
                    bullets.add(
                        Bullet(
                            id = nextEntityId++,
                            x = bot.x + cos(a) * 20f,
                            y = bot.y + sin(a) * 20f,
                            vx = cos(a) * bot.weapon.bulletSpeed * 0.85f,
                            vy = sin(a) * bot.weapon.bulletSpeed * 0.85f,
                            damage = bot.weapon.damage * 0.55f,
                            ownerId = bot.id,
                            maxRange = bot.weapon.range,
                            color = Color(0xFFFCA5A5),
                            isHeadshotRoll = false,
                        )
                    )
                }
            } else {
                bot.aiRepathTimer -= dt
                if (bot.aiRepathTimer <= 0f || hypot(bot.aiTargetX - bot.x, bot.aiTargetY - bot.y) < 35f) {
                    bot.aiRepathTimer = 2.8f + Random.nextFloat() * 2f
                    if (distFromCenter > zoneRadius * 0.82f) {
                        bot.aiTargetX = zoneCenterX + Random.nextInt(-180, 180)
                        bot.aiTargetY = zoneCenterY + Random.nextInt(-180, 180)
                    } else {
                        bot.aiTargetX = (bot.x + Random.nextInt(-280, 280)).coerceIn(100f, worldSize - 100f)
                        bot.aiTargetY = (bot.y + Random.nextInt(-280, 280)).coerceIn(100f, worldSize - 100f)
                    }
                }
                val a = atan2(bot.aiTargetY - bot.y, bot.aiTargetX - bot.x)
                bot.angle = a
                moveEntityWithCollision(
                    bot.x,
                    bot.y,
                    cos(a) * 145f * dt,
                    sin(a) * 145f * dt,
                    playerRadius,
                ).let {
                    bot.x = it.first
                    bot.y = it.second
                }
            }
        }
    }

    private fun updateBullets(dt: Float) {
        val bulletIterator = bullets.iterator()
        val destroyedBarrels = mutableListOf<Obstacle>()
        while (bulletIterator.hasNext()) {
            val b = bulletIterator.next()
            val dx = b.vx * dt
            val dy = b.vy * dt
            b.x += dx
            b.y += dy
            b.distanceTraveled += hypot(dx, dy)

            if (b.distanceTraveled >= b.maxRange || b.x < 0f || b.x > worldSize || b.y < 0f || b.y > worldSize) {
                bulletIterator.remove()
                continue
            }

            // Obstacle hit check
            val hitObs = obstacles.firstOrNull { obs ->
                b.x >= obs.x && b.x <= obs.x + obs.width && b.y >= obs.y && b.y <= obs.y + obs.height
            }
            if (hitObs != null) {
                if (hitObs.kind == ObstacleKind.BARREL || hitObs.kind == ObstacleKind.GLOO_WALL) {
                    hitObs.hp -= b.damage
                    if (hitObs.hp <= 0f) {
                        if (hitObs.kind == ObstacleKind.BARREL) {
                            destroyedBarrels.add(hitObs)
                        } else {
                            obstacles.remove(hitObs)
                        }
                    }
                }
                bulletIterator.remove()
                continue
            }

            // Player hit check
            if (b.ownerId != 0 && playerAlive && hypot(b.x - playerX, b.y - playerY) <= playerRadius) {
                val absorbed = (b.damage * 0.35f).coerceAtMost(playerArmor)
                playerArmor -= absorbed
                val net = b.damage - absorbed
                playerHp -= net
                popups.add(FloatingPopup(nextEntityId++, playerX, playerY - 22f, "-${net.toInt()}", Color(0xFFEF4444)))
                if (playerHp <= 0f) {
                    playerHp = 0f
                    val killerName = bots.firstOrNull { it.id == b.ownerId }?.name ?: "Enemy"
                    eliminatePlayer(killerName)
                }
                bulletIterator.remove()
                continue
            }

            // Bot hit check
            val hitBot = bots.firstOrNull { bot ->
                bot.alive && bot.id != b.ownerId && hypot(b.x - bot.x, b.y - bot.y) <= playerRadius
            }
            if (hitBot != null) {
                val absorbed = (b.damage * 0.25f).coerceAtMost(hitBot.armor)
                hitBot.armor -= absorbed
                val net = b.damage - absorbed
                hitBot.hp -= net

                if (b.ownerId == 0) {
                    playerDamage += net.toInt()
                    val label = if (b.isHeadshotRoll) "HEAD ${net.toInt()}" else "${net.toInt()}"
                    val col = if (b.isHeadshotRoll) Color(0xFFEF4444) else Color(0xFFFFD54F)
                    popups.add(FloatingPopup(nextEntityId++, hitBot.x, hitBot.y - 24f, label, col))
                }

                if (hitBot.hp <= 0f) {
                    val killerName = if (b.ownerId == 0) "YOU" else bots.firstOrNull { it.id == b.ownerId }?.name ?: "Bot"
                    val weaponName = if (b.ownerId == 0) activeWeapon().type.displayName else (bots.firstOrNull { it.id == b.ownerId }?.weapon?.displayName ?: "Rifle")
                    eliminateBot(hitBot, killerName, weaponName, b.ownerId == 0)
                }
                bulletIterator.remove()
            }
        }

        // Handle barrel explosions
        for (barrel in destroyedBarrels) {
            obstacles.remove(barrel)
            val bx = barrel.x + barrel.width / 2f
            val by = barrel.y + barrel.height / 2f
            popups.add(FloatingPopup(nextEntityId++, bx, by - 16f, "BOOM!", Color(0xFFFF6D00)))
            if (playerAlive && hypot(playerX - bx, playerY - by) < 120f) {
                playerHp -= 55f
                if (playerHp <= 0f) {
                    playerHp = 0f
                    eliminatePlayer("Red Barrel")
                }
            }
            for (bot in bots) {
                if (bot.alive && hypot(bot.x - bx, bot.y - by) < 120f) {
                    bot.hp -= 85f
                    if (bot.hp <= 0f) {
                        eliminateBot(bot, "YOU", "Red Barrel", true)
                    }
                }
            }
        }
    }

    private fun eliminateBot(bot: BotEntity, killer: String, weapon: String, byPlayer: Boolean) {
        if (!bot.alive) return
        bot.alive = false
        if (byPlayer) {
            playerKills++
            popups.add(FloatingPopup(nextEntityId++, bot.x, bot.y - 34f, "ELIMINATED!", Color(0xFFFFB300)))
        }
        // Drop loot crate
        groundLoot.add(GroundLoot(nextEntityId++, bot.x, bot.y, LootKind.WEAPON, weapon = bot.weapon))
        groundLoot.add(GroundLoot(nextEntityId++, bot.x + 16f, bot.y, LootKind.MEDKIT, amount = 1))
        groundLoot.add(GroundLoot(nextEntityId++, bot.x - 16f, bot.y, LootKind.AMMO, amount = 30))

        killFeed.add(0, KillFeedEntry(nextEntityId++, killer, bot.name, weapon, byPlayer))
        if (killFeed.size > 4) killFeed.removeAt(killFeed.lastIndex)
    }

    private fun eliminatePlayer(killer: String) {
        if (!playerAlive) return
        playerAlive = false
        val remainingBots = bots.count { it.alive }
        finalPlacement = (remainingBots + 1).coerceIn(2, 20)
        matchEnded = true
        killFeed.add(0, KillFeedEntry(nextEntityId++, killer, "YOU", "Eliminated", true))
    }

    private fun moveEntityWithCollision(
        x: Float,
        y: Float,
        dx: Float,
        dy: Float,
        radius: Float,
    ): Pair<Float, Float> {
        var nx = (x + dx).coerceIn(radius + 16f, worldSize - radius - 16f)
        if (collidesWithObstacle(nx, y, radius)) {
            nx = x
        }
        var ny = (y + dy).coerceIn(radius + 16f, worldSize - radius - 16f)
        if (collidesWithObstacle(nx, ny, radius)) {
            ny = y
        }
        return nx to ny
    }

    private fun collidesWithObstacle(cx: Float, cy: Float, r: Float): Boolean {
        return obstacles.any { obs ->
            val closestX = cx.coerceIn(obs.x, obs.x + obs.width)
            val closestY = cy.coerceIn(obs.y, obs.y + obs.height)
            hypot(cx - closestX, cy - closestY) < r
        }
    }

    fun hudSnapshot(): BattleHudSnapshot {
        val aliveBots = bots.count { it.alive }
        val inStorm = hypot(playerX - zoneCenterX, playerY - zoneCenterY) > zoneRadius
        return BattleHudSnapshot(
            hp = playerHp,
            maxHp = playerMaxHp,
            armor = playerArmor,
            aliveCount = aliveBots + if (playerAlive) 1 else 0,
            totalSurvivors = 20,
            kills = playerKills,
            damageDealt = playerDamage,
            survivalSeconds = elapsedSeconds.toInt(),
            medkits = medkits,
            glooWalls = glooWalls,
            activeSlotIndex = activeSlotIndex,
            weaponSlots = weaponSlots.map { it.copy() },
            isReloading = reloadTimer > 0f,
            isHealing = healTimer > 0f,
            inStorm = inStorm,
            zonePhase = zonePhase,
            zoneTimerSeconds = zoneTimer.toInt().coerceAtLeast(0),
            killFeed = killFeed.toList(),
            matchEnded = matchEnded,
            placement = finalPlacement,
            nearestLootPrompt = nearestLootPrompt,
        )
    }
}
