package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GameCatalog
import com.example.model.GameMode
import com.example.model.UserProfile
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.FieryOrange
import com.example.ui.theme.FireZoneTheme
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SteelSlate
import com.example.ui.theme.TacticalObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class RetroLobbyTab(val label: String) {
    LOBBY("LOBBY"),
    EVENTS("EVENTS"),
    RANKED("RANKED"),
}

private val ChamferButtonShape = GenericShape { size, _ ->
    val cut = 14f
    moveTo(cut, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width - cut, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
fun RetroLobbyUi(
    profile: UserProfile,
    selectedMode: GameMode,
    ownedCount: Int,
    onSelectMode: (GameMode) -> Unit,
    onOpenStore: () -> Unit,
    onOpenLuckRoyale: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenTopUp: () -> Unit,
    onOpenCareer: () -> Unit,
    onStartMatch: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf(RetroLobbyTab.LOBBY) }

    val equippedHat = GameCatalog.findItem(profile.equippedHatId)?.name ?: "OG Red Bandana"
    val equippedTop = GameCatalog.findItem(profile.equippedTopId)?.name ?: "OG Survivor Tank"
    val equippedWeapon = GameCatalog.findItem(profile.equippedWeaponSkinId)?.name ?: "Standard Steel"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TacticalObsidian)
    ) {
        // Classic 2017 Hangar / Cliffside Sunset Backdrop
        Image(
            painter = painterResource(id = R.drawable.img_lobby_backdrop),
            contentDescription = stringResource(R.string.cd_lobby_backdrop),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Retro atmospheric vignette + subtle tactical scanlines
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            TacticalObsidian.copy(alpha = 0.72f),
                            TacticalObsidian.copy(alpha = 0.25f),
                            TacticalObsidian.copy(alpha = 0.90f),
                        )
                    )
                )
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
                y += 6f
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // 1. Top Currency Counters & Survivor Profile Header
            RetroTopCurrencyBar(
                profile = profile,
                onOpenTopUp = onOpenTopUp,
                onSignOut = onSignOut,
            )

            // 2. Classic 2017 Slanted Navigation Tabs
            RetroNavigationTabStrip(
                activeTab = activeTab,
                onSelectTab = { activeTab = it },
            )

            // 3. Main Lobby Stage: Left Side Rail + Center/Right Display
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                val isWide = maxWidth >= 600.dp

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Left Vertical Navigation Rail (Store, Luck Royale, Vault, Top-Up, Career)
                    Column(
                        modifier = Modifier
                            .width(if (isWide) 178.dp else 134.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RetroSideMenuTab(
                            icon = Icons.Filled.ShoppingBag,
                            title = "STORE",
                            subtitle = "Clothes & Skins",
                            accent = AmberGold,
                            onClick = onOpenStore,
                            testTag = "nav_store",
                        )
                        RetroSideMenuTab(
                            icon = Icons.Filled.Casino,
                            title = "LUCK ROYALE",
                            subtitle = "${profile.luckPoints}/100 Luck",
                            accent = Color(0xFFA855F7),
                            onClick = onOpenLuckRoyale,
                            testTag = "nav_luck_royale",
                        )
                        RetroSideMenuTab(
                            icon = Icons.Filled.Backpack,
                            title = "VAULT",
                            subtitle = "$ownedCount Items",
                            accent = EmeraldSafe,
                            onClick = onOpenVault,
                            testTag = "nav_vault",
                        )
                        RetroSideMenuTab(
                            icon = Icons.Filled.Diamond,
                            title = "TOP-UP",
                            subtitle = "Hold 3s Secret",
                            accent = DiamondCyan,
                            onClick = onOpenTopUp,
                            testTag = "nav_topup",
                        )
                        RetroSideMenuTab(
                            icon = Icons.Filled.History,
                            title = "CAREER",
                            subtitle = "${profile.kills} Eliminations",
                            accent = FieryOrange,
                            onClick = onOpenCareer,
                            testTag = "nav_career",
                        )
                    }

                    // Center / Right Stage switched by active RetroLobbyTab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AnimatedContent(
                            targetState = activeTab,
                            label = "retroLobbyTabContent",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) { tab ->
                            when (tab) {
                                RetroLobbyTab.LOBBY -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        // Equipped Loadout Ribbon
                                        Surface(
                                            color = CharcoalPanel.copy(alpha = 0.84f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.55f)),
                                            onClick = onOpenVault,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "$equippedHat • $equippedTop",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = "WEAPON: $equippedWeapon",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = AmberGold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(AmberGold.copy(alpha = 0.18f))
                                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "2017 OG",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = AmberGold,
                                                    )
                                                }
                                            }
                                        }

                                        // Dynamic Equipped Survivor Display
                                        CharacterPreviewCanvas(
                                            hatId = profile.equippedHatId,
                                            maskId = profile.equippedMaskId,
                                            topId = profile.equippedTopId,
                                            bottomId = profile.equippedBottomId,
                                            shoesId = profile.equippedShoesId,
                                            weaponSkinId = profile.equippedWeaponSkinId,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                        )
                                    }
                                }

                                RetroLobbyTab.EVENTS -> {
                                    RetroEventsPanel(
                                        onOpenLuckRoyale = onOpenLuckRoyale,
                                        onOpenTopUp = onOpenTopUp,
                                        onOpenStore = onOpenStore,
                                    )
                                }

                                RetroLobbyTab.RANKED -> {
                                    RetroRankedPanel(
                                        profile = profile,
                                        onOpenCareer = onOpenCareer,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bottom Map Mode Card + Classic Chamfered START Button
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                onClick = {
                                    val next = if (selectedMode == GameMode.BERMUDA_SOLO) {
                                        GameMode.PURGATORY_SOLO
                                    } else {
                                        GameMode.BERMUDA_SOLO
                                    }
                                    onSelectMode(next)
                                },
                                color = CharcoalPanel.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, SteelBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mode_selector_card"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Map,
                                            contentDescription = null,
                                            tint = AmberGold,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = selectedMode.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary,
                                            )
                                            Text(
                                                text = selectedMode.subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(SteelSlate)
                                            .border(1.dp, AmberGold.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "SWITCH",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberGold,
                                        )
                                    }
                                }
                            }

                            // Classic 2017 Chamfered Amber-to-Orange START Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(ChamferButtonShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(AmberGold, BrightGold, FieryOrange)
                                        )
                                    )
                                    .border(1.5.dp, Color(0xFFFFF8E1), ChamferButtonShape)
                                    .clickable(onClick = onStartMatch)
                                    .testTag("start_match_button"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = TacticalObsidian,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "START",
                                        style = MaterialTheme.typography.displayMedium,
                                        color = TacticalObsidian,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RetroTopCurrencyBar(
    profile: UserProfile,
    onOpenTopUp: () -> Unit,
    onSignOut: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        TacticalObsidian.copy(alpha = 0.96f),
                        CharcoalPanel.copy(alpha = 0.90f),
                    )
                )
            )
            .border(1.dp, SteelBorder.copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Player Badge & XP Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AmberGold.copy(alpha = 0.18f))
                    .border(1.5.dp, AmberGold, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MilitaryTech,
                    contentDescription = null,
                    tint = AmberGold,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.padding(end = 6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldSafe)
                    )
                    Text(
                        text = profile.callsign.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val xpFraction = ((profile.xp % 500L) / 500f).coerceIn(0.05f, 1f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "LV.${profile.level}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberGold,
                    )
                    LinearProgressIndicator(
                        progress = { xpFraction },
                        color = AmberGold,
                        trackColor = SteelSlate,
                        modifier = Modifier
                            .width(52.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                }
            }
        }

        // Top Gold & Diamond Counters + Sign Out
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RetroCurrencyCounter(
                icon = Icons.Filled.MonetizationOn,
                amount = formatNumber(profile.goldCoins),
                tint = AmberGold,
                onClick = onOpenTopUp,
                testTag = "top_gold_pill",
            )
            RetroCurrencyCounter(
                icon = Icons.Filled.Diamond,
                amount = formatNumber(profile.diamonds),
                tint = DiamondCyan,
                onClick = onOpenTopUp,
                testTag = "top_diamond_pill",
            )
            IconButton(
                onClick = onSignOut,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("sign_out_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(R.string.cd_sign_out),
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun RetroCurrencyCounter(
    icon: ImageVector,
    amount: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        onClick = onClick,
        color = SteelSlate,
        shape = ChamferButtonShape,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.7f)),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .height(34.dp)
            .testTag(testTag),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add currency",
                    tint = TacticalObsidian,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun RetroNavigationTabStrip(
    activeTab: RetroLobbyTab,
    onSelectTab: (RetroLobbyTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalPanel.copy(alpha = 0.85f))
            .border(1.dp, SteelBorder.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RetroLobbyTab.entries.forEach { tab ->
            val selected = tab == activeTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(ChamferButtonShape)
                    .background(
                        if (selected) AmberGold else SteelSlate.copy(alpha = 0.7f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) BrightGold else SteelBorder,
                        shape = ChamferButtonShape,
                    )
                    .clickable { onSelectTab(tab) }
                    .testTag("retro_tab_${tab.name.lowercase()}"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) TacticalObsidian else TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun RetroSideMenuTab(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        onClick = onClick,
        color = CharcoalPanel.copy(alpha = 0.90f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RetroEventsPanel(
    onOpenLuckRoyale: () -> Unit,
    onOpenTopUp: () -> Unit,
    onOpenStore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = CharcoalPanel.copy(alpha = 0.92f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, AmberGold),
            onClick = onOpenLuckRoyale,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Casino, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("SEASON 1 LUCK ROYALE", style = MaterialTheme.typography.titleMedium, color = AmberGold)
                    Text("Spin for Red Criminal, Sakura & Flaming Dragon AK!", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        Surface(
            color = CharcoalPanel.copy(alpha = 0.92f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, DiamondCyan),
            onClick = onOpenTopUp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = DiamondCyan, modifier = Modifier.size(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("3s HOLD SECRET VAULT", style = MaterialTheme.typography.titleMedium, color = DiamondCyan)
                    Text("Hold any Diamond or Coin pack for 3s in Top-Up to add custom amounts.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        Surface(
            color = CharcoalPanel.copy(alpha = 0.92f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, EmeraldSafe),
            onClick = onOpenStore,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = EmeraldSafe, modifier = Modifier.size(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("OG CLOTHES BOUTIQUE", style = MaterialTheme.typography.titleMedium, color = EmeraldSafe)
                    Text("Buy classic Jazz Pants, Hip-Hop Hoodie & Arctic Blue.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun RetroRankedPanel(
    profile: UserProfile,
    onOpenCareer: () -> Unit,
) {
    val tierName = when {
        profile.booyahs >= 10 -> "HEROIC"
        profile.booyahs >= 5 -> "DIAMOND"
        profile.booyahs >= 2 -> "PLATINUM"
        profile.kills >= 10 -> "GOLD"
        else -> "BRONZE"
    }
    val rankColor = when (tierName) {
        "HEROIC" -> FieryOrange
        "DIAMOND" -> DiamondCyan
        "PLATINUM" -> EmeraldSafe
        "GOLD" -> AmberGold
        else -> Color(0xFFCD7F32)
    }

    Surface(
        color = CharcoalPanel.copy(alpha = 0.92f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.5.dp, rankColor),
        onClick = onOpenCareer,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = rankColor,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "TIER: $tierName",
                style = MaterialTheme.typography.displayMedium,
                color = rankColor,
            )
            Text(
                text = "${profile.booyahs} BOOYAHS • ${profile.kills} KILLS • ${profile.matchesPlayed} MATCHES",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to view full Career match log",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun RetroLobbyUiPreview() {
    FireZoneTheme {
        RetroLobbyUi(
            profile = UserProfile(
                callsign = "ALPHABERM",
                goldCoins = 18500L,
                diamonds = 2450L,
                level = 14L,
                xp = 320L,
                booyahs = 7L,
                kills = 46L,
                luckPoints = 64L,
                equippedHatId = "hat_criminal_mask",
                equippedTopId = "top_criminal_red",
                equippedBottomId = "bottom_criminal_pants",
                equippedWeaponSkinId = "skin_ak_dragon",
            ),
            selectedMode = GameMode.BERMUDA_SOLO,
            ownedCount = 12,
            onSelectMode = {},
            onOpenStore = {},
            onOpenLuckRoyale = {},
            onOpenVault = {},
            onOpenTopUp = {},
            onOpenCareer = {},
            onStartMatch = {},
            onSignOut = {},
        )
    }
}
