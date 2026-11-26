package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.game.BattleEngine
import com.example.game.BattleHudSnapshot
import com.example.game.LootKind
import com.example.game.ObstacleKind
import com.example.model.GameCatalog
import com.example.model.GameMode
import com.example.model.MatchRewardSummary
import com.example.model.UserProfile
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.FieryOrange
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SteelSlate
import com.example.ui.theme.TacticalObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun BattleRoyaleScreen(
    profile: UserProfile,
    mode: GameMode,
    onMatchFinished: (
        mode: GameMode,
        placement: Int,
        kills: Int,
        damageDealt: Int,
        survivalSeconds: Int,
        onRecorded: (Result<MatchRewardSummary>) -> Unit,
    ) -> Unit,
    onExitToLobby: () -> Unit,
) {
    var showQuitDialog by remember { mutableStateOf(false) }

    val hatColor = GameCatalog.findItem(profile.equippedHatId)?.primaryColor ?: Color(0xFFDC2626)
    val topColor = GameCatalog.findItem(profile.equippedTopId)?.primaryColor ?: Color(0xFFE2E8F0)
    val pantsColor = GameCatalog.findItem(profile.equippedBottomId)?.primaryColor ?: Color(0xFF78716C)
    val weaponTint = GameCatalog.findItem(profile.equippedWeaponSkinId)?.accentColor ?: Color(0xFFFFD54F)

    val engine = remember(mode) {
        BattleEngine(
            mode = mode,
            playerHatColor = hatColor,
            playerTopColor = topColor,
            playerPantsColor = pantsColor,
            weaponTintColor = weaponTint,
        )
    }

    var hud by remember { mutableStateOf(engine.hudSnapshot()) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var rewardSummary by remember { mutableStateOf<MatchRewardSummary?>(null) }
    var hasSubmittedMatch by remember { mutableStateOf(false) }

    // Dual joystick input state
    var moveX by remember { mutableFloatStateOf(0f) }
    var moveY by remember { mutableFloatStateOf(0f) }
    var aimX by remember { mutableFloatStateOf(0f) }
    var aimY by remember { mutableFloatStateOf(0f) }
    var isManualFiring by remember { mutableStateOf(false) }
    var isAutoFiring by remember { mutableStateOf(false) }

    BackHandler {
        if (!hud.matchEnded) {
            showQuitDialog = true
        } else {
            onExitToLobby()
        }
    }

    // 60fps open-world simulation loop
    LaunchedEffect(engine) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos == 0L) lastNanos = now
                val dt = (now - lastNanos) / 1_000_000_000f
                lastNanos = now
                engine.step(
                    dtRaw = dt,
                    moveX = moveX,
                    moveY = moveY,
                    aimX = aimX,
                    aimY = aimY,
                    isManualFiring = isManualFiring,
                    autoFireTap = isAutoFiring,
                )
                frameTick = now
                hud = engine.hudSnapshot()
            }
        }
    }

    // Record match result once when match ends
    LaunchedEffect(hud.matchEnded) {
        if (hud.matchEnded && !hasSubmittedMatch) {
            hasSubmittedMatch = true
            onMatchFinished(
                mode,
                hud.placement,
                hud.kills,
                hud.damageDealt,
                hud.survivalSeconds,
            ) { res ->
                res.onSuccess { rewardSummary = it }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalObsidian),
    ) {
        // Open-World Top-Down / 2.5D Battleground Canvas
        OpenWorldBattleCanvas(
            engine = engine,
            frameTick = frameTick,
            modifier = Modifier.fillMaxSize(),
        )

        // Classic Old FF Combat HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Row: Minimap + Kill Feed (left) & Alive/Kills/Zone/Exit (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MinimapCanvas(
                        engine = engine,
                        frameTick = frameTick,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.5.dp, AmberGold.copy(alpha = 0.8f), RoundedCornerShape(6.dp)),
                    )
                    // Kill feed
                    hud.killFeed.forEach { entry ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (entry.isPlayerInvolved) AmberGold.copy(alpha = 0.85f)
                                    else TacticalObsidian.copy(alpha = 0.72f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "${entry.killer} [${entry.weaponName}] ${entry.victim}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.isPlayerInvolved) TacticalObsidian else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HudBadge("ALIVE", "${hud.aliveCount}", AmberGold)
                        HudBadge("KILLS", "${hud.kills}", CrimsonAlert)
                        IconButton(
                            onClick = { showQuitDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CharcoalPanel.copy(alpha = 0.85f))
                                .border(1.dp, SteelBorder, RoundedCornerShape(4.dp))
                                .testTag("match_exit_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Leave match",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Surface(
                        color = if (hud.inStorm) CrimsonAlert.copy(alpha = 0.9f) else CharcoalPanel.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, if (hud.inStorm) Color.White else DiamondCyan.copy(alpha = 0.6f)),
                    ) {
                        Text(
                            text = if (hud.inStorm) "OUTSIDE SAFE ZONE!" else "ZONE ${hud.zonePhase} • ${hud.zoneTimerSeconds}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // Bottom Controls: Left Move Joystick, Center HP/Weapon Cards, Right Aim/Fire + Gloo/Medkit/Reload
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Contextual Loot Swap Prompt
                hud.nearestLootPrompt?.let { prompt ->
                    Button(
                        onClick = { engine.swapNearestLootWeapon() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = TacticalObsidian,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .height(38.dp)
                            .testTag("swap_loot_button"),
                    ) {
                        Text(prompt, style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Weapon Slots + HP/Armor Bar
                Surface(
                    color = CharcoalPanel.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, SteelBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            hud.weaponSlots.forEachIndexed { idx, slot ->
                                val isSelected = idx == hud.activeSlotIndex
                                Surface(
                                    onClick = { engine.switchWeapon(idx) },
                                    color = if (isSelected) slot.type.badgeColor.copy(alpha = 0.25f) else SteelSlate,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) slot.type.badgeColor else SteelBorder,
                                    ),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = slot.type.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${slot.ammoInMag}/${slot.reserveAmmo}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberGold,
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "HP ${hud.hp.toInt()}/${hud.maxHp.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSafe,
                            )
                            LinearProgressIndicator(
                                progress = { (hud.hp / hud.maxHp).coerceIn(0f, 1f) },
                                color = if (hud.hp > 60f) EmeraldSafe else CrimsonAlert,
                                trackColor = SteelSlate,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                            )
                            Text(
                                text = "SHD ${hud.armor.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DiamondCyan,
                            )
                        }

                        if (hud.isReloading || hud.isHealing) {
                            Text(
                                text = if (hud.isReloading) "RELOADING..." else "USING MEDKIT...",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberGold,
                            )
                        }
                    }
                }

                // Dual Joystick & Action Cluster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    // Left Movement Joystick
                    VirtualJoystick(
                        label = "MOVE",
                        accent = TextPrimary,
                        onVectorChanged = { x, y ->
                            moveX = x
                            moveY = y
                        },
                        modifier = Modifier
                            .size(118.dp)
                            .testTag("move_joystick"),
                    )

                    // Center Tactical Buttons: Medkit, Gloo Wall, Reload
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TacticalHudActionBtn(
                            icon = Icons.Filled.LocalHospital,
                            countText = "${hud.medkits}",
                            tint = EmeraldSafe,
                            onClick = { engine.startMedkit() },
                            testTag = "medkit_button",
                        )
                        TacticalHudActionBtn(
                            icon = Icons.Filled.Shield,
                            countText = "${hud.glooWalls}",
                            tint = DiamondCyan,
                            onClick = { engine.deployGlooWall() },
                            testTag = "gloo_wall_button",
                        )
                        TacticalHudActionBtn(
                            icon = Icons.Filled.Refresh,
                            countText = "RLD",
                            tint = AmberGold,
                            onClick = { engine.startReload() },
                            testTag = "reload_button",
                        )
                    }

                    // Right Aim & Fire Pad (Drag to aim+shoot, or hold Auto-Aim Fire button)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        VirtualJoystick(
                            label = "AIM/FIRE",
                            accent = FieryOrange,
                            onVectorChanged = { x, y ->
                                aimX = x
                                aimY = y
                                isManualFiring = hypot(x, y) > 0.25f
                            },
                            onTapFire = { active ->
                                isAutoFiring = active
                            },
                            modifier = Modifier
                                .size(118.dp)
                                .testTag("fire_joystick"),
                        )
                    }
                }
            }
        }

        // End of Match BOOYAH! / Elimination Overlay
        if (hud.matchEnded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TacticalObsidian.copy(alpha = 0.86f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = CharcoalPanel,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        2.dp,
                        if (hud.placement == 1) AmberGold else CrimsonAlert,
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (hud.placement == 1) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                        Text(
                            text = if (hud.placement == 1) "BOOYAH!" else "BETTER LUCK NEXT TIME",
                            style = MaterialTheme.typography.displayLarge,
                            color = if (hud.placement == 1) AmberGold else CrimsonAlert,
                        )
                        Text(
                            text = "RANK #${hud.placement} / 20 • ${mode.title}",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            SummaryStat("KILLS", "${hud.kills}", CrimsonAlert)
                            SummaryStat("DAMAGE", "${hud.damageDealt}", AmberGold)
                            SummaryStat("TIME", "${hud.survivalSeconds}s", DiamondCyan)
                        }

                        val summary = rewardSummary
                        if (summary != null) {
                            Surface(
                                color = SteelSlate,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    Text(
                                        text = "+${summary.goldEarned} GOLD",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = AmberGold,
                                    )
                                    Text(
                                        text = "+${summary.xpEarned} XP",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = EmeraldSafe,
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Crediting match rewards to your account...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }

                        Button(
                            onClick = onExitToLobby,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGold,
                                contentColor = TacticalObsidian,
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("match_return_lobby_button"),
                        ) {
                            Text("RETURN TO LOBBY", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            containerColor = CharcoalPanel,
            title = {
                Text("EXIT MATCH?", style = MaterialTheme.typography.headlineMedium, color = AmberGold)
            },
            text = {
                Text(
                    "Leave the current open-world island and return to the Lobby?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitDialog = false
                        onExitToLobby()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonAlert,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("EXIT", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text("STAY", color = TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun OpenWorldBattleCanvas(
    engine: BattleEngine,
    frameTick: Long,
    modifier: Modifier = Modifier,
) {
    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(180, 255, 255, 255)
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
    }
    val botNamePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 20f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val popupPaint = remember {
        android.graphics.Paint().apply {
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier) {
        // Use frameTick so Compose invalidates every frame
        if (frameTick == -1L) return@Canvas

        val camX = engine.playerX - size.width / 2f
        val camY = engine.playerY - size.height / 2f

        val grassBase = if (engine.mode == GameMode.BERMUDA_SOLO) Color(0xFF2E4E34) else Color(0xFF47432B)
        val gridColor = if (engine.mode == GameMode.BERMUDA_SOLO) Color(0xFF26422C) else Color(0xFF3C3824)
        val roadColor = Color(0xFF57534E)
        val riverColor = Color(0xFF0369A1)

        // Terrain background
        drawRect(color = grassBase)

        // Grid lines
        val step = 120f
        val startX = (-camX % step)
        val startY = (-camY % step)
        var gx = startX
        while (gx < size.width) {
            drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 2f)
            gx += step
        }
        var gy = startY
        while (gy < size.height) {
            drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 2f)
            gy += step
        }

        // Cross-island dirt roads & river
        drawRect(
            color = riverColor.copy(alpha = 0.75f),
            topLeft = Offset(0f - camX, 920f - camY),
            size = Size(engine.worldSize, 70f),
        )
        drawRect(
            color = roadColor,
            topLeft = Offset(1170f - camX, 0f - camY),
            size = Size(60f, engine.worldSize),
        )
        drawRect(
            color = roadColor,
            topLeft = Offset(0f - camX, 1170f - camY),
            size = Size(engine.worldSize, 60f),
        )

        // POI labels
        engine.pois.forEach { poi ->
            val sx = poi.x - camX
            val sy = poi.y - camY
            if (sx in -200f..(size.width + 200f) && sy in -200f..(size.height + 200f)) {
                drawContext.canvas.nativeCanvas.drawText(poi.name, sx, sy - 6f, labelPaint)
            }
        }

        // Ground Loot
        engine.groundLoot.forEach { loot ->
            val sx = loot.x - camX
            val sy = loot.y - camY
            if (sx in -60f..(size.width + 60f) && sy in -60f..(size.height + 60f)) {
                val col = when (loot.kind) {
                    LootKind.WEAPON -> loot.weapon?.badgeColor ?: AmberGold
                    LootKind.MEDKIT -> EmeraldSafe
                    LootKind.ARMOR_VEST -> DiamondCyan
                    LootKind.GLOO_WALL -> Color(0xFF7DD3FC)
                    LootKind.AMMO -> Color(0xFFFACC15)
                }
                drawCircle(col.copy(alpha = 0.25f), radius = 16f, center = Offset(sx, sy))
                drawRoundRect(
                    color = col,
                    topLeft = Offset(sx - 8f, sy - 8f),
                    size = Size(16f, 16f),
                    cornerRadius = CornerRadius(4f),
                )
            }
        }

        // Obstacles (Walls, Crates, Trees, Barrels, Gloo Walls)
        engine.obstacles.forEach { obs ->
            val sx = obs.x - camX
            val sy = obs.y - camY
            if (sx + obs.width >= 0f && sx <= size.width && sy + obs.height >= 0f && sy <= size.height) {
                when (obs.kind) {
                    ObstacleKind.BUILDING_WALL -> {
                        drawRect(Color(0xFF64748B), Offset(sx, sy), Size(obs.width, obs.height))
                        drawRect(Color(0xFF334155), Offset(sx, sy), Size(obs.width, obs.height), style = Stroke(2f))
                    }
                    ObstacleKind.CRATE -> {
                        drawRoundRect(
                            color = Color(0xFF92400E),
                            topLeft = Offset(sx, sy),
                            size = Size(obs.width, obs.height),
                            cornerRadius = CornerRadius(4f),
                        )
                        drawLine(Color(0xFF451A03), Offset(sx, sy), Offset(sx + obs.width, sy + obs.height), 3f)
                    }
                    ObstacleKind.TREE -> {
                        drawCircle(
                            color = Color(0xFF14532D),
                            radius = obs.width / 2f,
                            center = Offset(sx + obs.width / 2f, sy + obs.height / 2f),
                        )
                        drawCircle(
                            color = Color(0xFF166534),
                            radius = obs.width / 3f,
                            center = Offset(sx + obs.width / 2f - 4f, sy + obs.height / 2f - 4f),
                        )
                    }
                    ObstacleKind.BARREL -> {
                        drawCircle(
                            color = Color(0xFFDC2626),
                            radius = obs.width / 2f,
                            center = Offset(sx + obs.width / 2f, sy + obs.height / 2f),
                        )
                        drawCircle(
                            color = AmberGold,
                            radius = obs.width / 4f,
                            center = Offset(sx + obs.width / 2f, sy + obs.height / 2f),
                        )
                    }
                    ObstacleKind.GLOO_WALL -> {
                        drawRoundRect(
                            color = DiamondCyan.copy(alpha = 0.85f),
                            topLeft = Offset(sx, sy),
                            size = Size(obs.width, obs.height),
                            cornerRadius = CornerRadius(4f),
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(sx, sy),
                            size = Size(obs.width, obs.height),
                            cornerRadius = CornerRadius(4f),
                            style = Stroke(2f),
                        )
                    }
                }
            }
        }

        // Bots
        engine.bots.forEach { bot ->
            if (!bot.alive) return@forEach
            val sx = bot.x - camX
            val sy = bot.y - camY
            if (sx in -80f..(size.width + 80f) && sy in -80f..(size.height + 80f)) {
                // Gun barrel
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(sx, sy),
                    end = Offset(sx + cos(bot.angle) * 24f, sy + sin(bot.angle) * 24f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round,
                )
                // Body & hat
                drawCircle(bot.shirtColor, radius = 17f, center = Offset(sx, sy))
                drawCircle(bot.hatColor, radius = 10f, center = Offset(sx, sy))
                // HP bar above bot
                val hpFrac = (bot.hp / bot.maxHp).coerceIn(0f, 1f)
                drawRect(Color.Black.copy(alpha = 0.65f), Offset(sx - 18f, sy - 27f), Size(36f, 5f))
                drawRect(CrimsonAlert, Offset(sx - 18f, sy - 27f), Size(36f * hpFrac, 5f))
                drawContext.canvas.nativeCanvas.drawText(bot.name, sx, sy - 33f, botNamePaint)
            }
        }

        // Player Character (wearing equipped FireZone colors!)
        if (engine.playerAlive) {
            val px = size.width / 2f
            val py = size.height / 2f
            drawCircle(AmberGold.copy(alpha = 0.25f), radius = 24f, center = Offset(px, py))
            drawLine(
                color = engine.weaponTintColor,
                start = Offset(px, py),
                end = Offset(px + cos(engine.playerAngle) * 28f, py + sin(engine.playerAngle) * 28f),
                strokeWidth = 7f,
                cap = StrokeCap.Round,
            )
            drawCircle(engine.playerPantsColor, radius = 19f, center = Offset(px, py))
            drawCircle(engine.playerTopColor, radius = 16f, center = Offset(px, py))
            drawCircle(engine.playerHatColor, radius = 9.5f, center = Offset(px, py))
        }

        // Bullets
        engine.bullets.forEach { b ->
            val sx = b.x - camX
            val sy = b.y - camY
            if (sx in -20f..(size.width + 20f) && sy in -20f..(size.height + 20f)) {
                drawCircle(b.color, radius = 4.5f, center = Offset(sx, sy))
            }
        }

        // Safe Zone Circle
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = engine.zoneRadius,
            center = Offset(engine.zoneCenterX - camX, engine.zoneCenterY - camY),
            style = Stroke(
                width = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)),
            ),
        )
        drawCircle(
            color = DiamondCyan.copy(alpha = 0.2f),
            radius = engine.zoneRadius + 1400f,
            center = Offset(engine.zoneCenterX - camX, engine.zoneCenterY - camY),
            style = Stroke(width = 2800f),
        )

        // Floating Damage & Headshot Popups
        engine.popups.forEach { pop ->
            val sx = pop.x - camX
            val sy = pop.y - camY
            popupPaint.color = android.graphics.Color.argb(
                255,
                (pop.color.red * 255).toInt(),
                (pop.color.green * 255).toInt(),
                (pop.color.blue * 255).toInt(),
            )
            drawContext.canvas.nativeCanvas.drawText(pop.text, sx, sy, popupPaint)
        }
    }
}

@Composable
private fun MinimapCanvas(
    engine: BattleEngine,
    frameTick: Long,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.background(Color(0xFF142419))) {
        if (frameTick == -1L) return@Canvas
        val scale = size.width / engine.worldSize

        // Roads & river
        drawRect(Color(0xFF0369A1), Offset(0f, 920f * scale), Size(size.width, 70f * scale))
        drawRect(Color(0xFF57534E), Offset(1170f * scale, 0f), Size(60f * scale, size.height))

        // POIs
        engine.pois.forEach { poi ->
            drawCircle(
                color = AmberGold.copy(alpha = 0.7f),
                radius = 3f,
                center = Offset(poi.x * scale, poi.y * scale),
            )
        }

        // Safe zone ring
        drawCircle(
            color = Color.White,
            radius = engine.zoneRadius * scale,
            center = Offset(engine.zoneCenterX * scale, engine.zoneCenterY * scale),
            style = Stroke(width = 1.5f),
        )

        // Nearby firing/bot dots within 550 units
        engine.bots.forEach { bot ->
            if (bot.alive && hypot(bot.x - engine.playerX, bot.y - engine.playerY) < 550f) {
                drawCircle(
                    color = CrimsonAlert,
                    radius = 2.5f,
                    center = Offset(bot.x * scale, bot.y * scale),
                )
            }
        }

        // Player arrow dot
        drawCircle(
            color = EmeraldSafe,
            radius = 4.2f,
            center = Offset(engine.playerX * scale, engine.playerY * scale),
        )
    }
}

@Composable
private fun VirtualJoystick(
    label: String,
    accent: Color,
    onVectorChanged: (Float, Float) -> Unit,
    onTapFire: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .pointerInput(label) {
                val maxRadius = size.width / 2.6f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onTapFire?.invoke(true)
                    val center = Offset(size.width / 2f, size.height / 2f)

                    fun updateFromPos(pos: Offset) {
                        val delta = pos - center
                        val dist = hypot(delta.x, delta.y)
                        val clamped = if (dist > maxRadius && dist > 0f) {
                            Offset(delta.x / dist * maxRadius, delta.y / dist * maxRadius)
                        } else delta
                        knobOffset = clamped
                        onVectorChanged(
                            (clamped.x / maxRadius).coerceIn(-1f, 1f),
                            (clamped.y / maxRadius).coerceIn(-1f, 1f),
                        )
                    }

                    updateFromPos(down.position)
                    drag(down.id) { change ->
                        change.consume()
                        updateFromPos(change.position)
                    }
                    knobOffset = Offset.Zero
                    onVectorChanged(0f, 0f)
                    onTapFire?.invoke(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f - 4f
            drawCircle(
                color = CharcoalPanel.copy(alpha = 0.6f),
                radius = r,
            )
            drawCircle(
                color = accent.copy(alpha = 0.55f),
                radius = r,
                style = Stroke(width = 2.5f),
            )
            drawCircle(
                color = accent.copy(alpha = 0.85f),
                radius = r * 0.42f,
                center = Offset(size.width / 2f + knobOffset.x, size.height / 2f + knobOffset.y),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TacticalObsidian,
        )
    }
}

@Composable
private fun TacticalHudActionBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    countText: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        onClick = onClick,
        color = CharcoalPanel.copy(alpha = 0.86f),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, tint),
        modifier = Modifier
            .size(50.dp)
            .testTag(testTag),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = countText, tint = tint, modifier = Modifier.size(18.dp))
            Text(countText, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
    }
}

@Composable
private fun HudBadge(label: String, value: String, color: Color) {
    Surface(
        color = CharcoalPanel.copy(alpha = 0.85f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.displayMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
