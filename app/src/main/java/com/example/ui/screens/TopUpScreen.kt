package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.CurrencyPack
import com.example.model.CurrencyTransaction
import com.example.model.GameCatalog
import com.example.model.UserProfile
import com.example.ui.components.OldFfTopCurrencyBar
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
fun TopUpScreen(
    profile: UserProfile,
    transactions: List<CurrencyTransaction>,
    isBusy: Boolean,
    onGrantPack: (
        isDiamonds: Boolean,
        amount: Long,
        isCustomHoldGrant: Boolean,
        note: String,
        diamondCostForGoldPack: Long,
    ) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var pendingPackConfirm by remember { mutableStateOf<CurrencyPack?>(null) }
    var customGrantInitialDiamonds by remember { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalObsidian)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        OldFfTopCurrencyBar(
            profile = profile,
            onOpenTopUp = {},
            onBack = onBack,
            screenTitle = "DIAMOND & GOLD TOP-UP",
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    color = CharcoalPanel,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DiamondCyan.copy(alpha = 0.6f)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(28.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TAP TO BUY • HOLD 3 SECONDS FOR CUSTOM AMOUNT",
                                style = MaterialTheme.typography.titleMedium,
                                color = AmberGold,
                            )
                            Text(
                                text = "Press and hold any Diamond or Gold pack for 3.0s to unlock the Custom Amount Agreement Vault.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "DIAMOND PACKS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DiamondCyan,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            items(GameCatalog.diamondPacks, key = { it.id }) { pack ->
                HoldableCurrencyPackCard(
                    pack = pack,
                    isBusy = isBusy,
                    onNormalTap = { pendingPackConfirm = pack },
                    onThreeSecondHold = { customGrantInitialDiamonds = true },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "GOLD COIN PACKS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AmberGold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(GameCatalog.coinPacks, key = { it.id }) { pack ->
                HoldableCurrencyPackCard(
                    pack = pack,
                    isBusy = isBusy,
                    onNormalTap = { pendingPackConfirm = pack },
                    onThreeSecondHold = { customGrantInitialDiamonds = false },
                )
            }

            if (transactions.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "RECENT ACCOUNT LEDGER",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                items(transactions.take(6), key = { it.id }) { tx ->
                    Surface(
                        color = CharcoalPanel,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, SteelBorder),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.note,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = tx.txType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                )
                            }
                            val sign = if (tx.amountDelta >= 0) "+" else ""
                            val col = if (tx.currency == "DIAMONDS") DiamondCyan else AmberGold
                            Text(
                                text = "$sign${formatNumber(tx.amountDelta)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = col,
                            )
                        }
                    }
                }
            }
        }
    }

    // Standard Tap Purchase Dialog
    val pack = pendingPackConfirm
    if (pack != null) {
        AlertDialog(
            onDismissRequest = { pendingPackConfirm = null },
            containerColor = CharcoalPanel,
            titleContentColor = if (pack.isDiamonds) DiamondCyan else AmberGold,
            title = {
                Text(
                    text = "CONFIRM ${pack.title.uppercase()}",
                    style = MaterialTheme.typography.headlineMedium,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (pack.isDiamonds) {
                            "Acquire ${formatNumber(pack.amount)} Diamonds (${pack.priceTag}) and credit them directly to your FireZone account?"
                        } else if (pack.goldPackCostInDiamonds > 0L) {
                            "Exchange ${pack.goldPackCostInDiamonds} Diamonds for ${formatNumber(pack.amount)} Gold Coins?"
                        } else {
                            "Claim ${formatNumber(pack.amount)} Gold Coins into your FireZone account?"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Tip: Hold any pack for 3 seconds to enter a custom diamond or coin amount!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onGrantPack(
                            pack.isDiamonds,
                            pack.amount,
                            false,
                            "Pack: ${pack.title}",
                            pack.goldPackCostInDiamonds,
                        )
                        pendingPackConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pack.isDiamonds) DiamondCyan else AmberGold,
                        contentColor = TacticalObsidian,
                    ),
                ) {
                    Text("CONFIRM", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPackConfirm = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
        )
    }

    // 3-Second Hold Custom Amount & Agreement Dialog
    val initialDiamondsMode = customGrantInitialDiamonds
    if (initialDiamondsMode != null) {
        CustomAmountAgreementDialog(
            initialIsDiamonds = initialDiamondsMode,
            onDismiss = { customGrantInitialDiamonds = null },
            onAgreeAndGrant = { isDiamonds, amount ->
                val unit = if (isDiamonds) "Diamonds" else "Gold Coins"
                onGrantPack(
                    isDiamonds,
                    amount,
                    true,
                    "3s Hold Custom Grant: +$amount $unit",
                    0L,
                )
                customGrantInitialDiamonds = null
            },
        )
    }
}

@Composable
private fun HoldableCurrencyPackCard(
    pack: CurrencyPack,
    isBusy: Boolean,
    onNormalTap: () -> Unit,
    onThreeSecondHold: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    val accent = if (pack.isDiamonds) DiamondCyan else AmberGold

    Surface(
        color = CharcoalPanel,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = if (isHolding) 2.dp else 1.dp,
            color = if (isHolding) AmberGold else accent.copy(alpha = 0.55f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pack_${pack.id}")
            .pointerInput(pack.id, isBusy) {
                if (isBusy) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isHolding = true
                    holdProgress = 0f
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val startMs = System.currentTimeMillis()
                    var triggeredHold = false

                    val holdJob = scope.launch {
                        while (true) {
                            val elapsed = System.currentTimeMillis() - startMs
                            val progress = (elapsed / 3000f).coerceIn(0f, 1f)
                            holdProgress = progress
                            if (elapsed >= 3000L) {
                                triggeredHold = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isHolding = false
                                holdProgress = 0f
                                onThreeSecondHold()
                                break
                            }
                            delay(32L)
                        }
                    }

                    waitForUpOrCancellation()
                    val elapsedOnUp = System.currentTimeMillis() - startMs
                    holdJob.cancel()
                    isHolding = false
                    holdProgress = 0f

                    if (!triggeredHold && elapsedOnUp < 450L) {
                        onNormalTap()
                    }
                }
            },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = pack.bonusLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                    )
                }
                Text(
                    text = "HOLD 3s",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }

            Icon(
                imageVector = if (pack.isDiamonds) Icons.Filled.Diamond else Icons.Filled.MonetizationOn,
                contentDescription = pack.title,
                tint = accent,
                modifier = Modifier.size(38.dp),
            )

            Text(
                text = formatNumber(pack.amount),
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary,
            )
            Text(
                text = if (pack.isDiamonds) "DIAMONDS" else "GOLD COINS",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )

            if (isHolding) {
                LinearProgressIndicator(
                    progress = { holdProgress },
                    color = AmberGold,
                    trackColor = SteelSlate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
                val sec = (holdProgress * 3.0f)
                Text(
                    text = "HOLDING ${"%.1f".format(sec)}s / 3.0s",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberGold,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(SteelSlate)
                        .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = pack.priceTag,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomAmountAgreementDialog(
    initialIsDiamonds: Boolean,
    onDismiss: () -> Unit,
    onAgreeAndGrant: (isDiamonds: Boolean, amount: Long) -> Unit,
) {
    var isDiamonds by remember { mutableStateOf(initialIsDiamonds) }
    var amountText by remember { mutableStateOf("10000") }
    var agreed by remember { mutableStateOf(true) }

    val parsedAmount = amountText.toLongOrNull()?.coerceIn(0L, 999_999_999L) ?: 0L
    val accent = if (isDiamonds) DiamondCyan else AmberGold

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalPanel,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (isDiamonds) Icons.Filled.Diamond else Icons.Filled.MonetizationOn,
                    contentDescription = null,
                    tint = accent,
                )
                Text(
                    text = "CUSTOM VAULT GRANT",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AmberGold,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "3-Second Hold Unlocked! Choose currency and enter any amount to add to your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isDiamonds,
                        onClick = { isDiamonds = true },
                        label = { Text("DIAMONDS") },
                        leadingIcon = {
                            Icon(Icons.Filled.Diamond, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DiamondCyan.copy(alpha = 0.25f),
                            selectedLabelColor = DiamondCyan,
                        ),
                        modifier = Modifier.testTag("custom_grant_diamonds_tab"),
                    )
                    FilterChip(
                        selected = !isDiamonds,
                        onClick = { isDiamonds = false },
                        label = { Text("GOLD COINS") },
                        leadingIcon = {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberGold.copy(alpha = 0.25f),
                            selectedLabelColor = AmberGold,
                        ),
                        modifier = Modifier.testTag("custom_grant_gold_tab"),
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        amountText = raw.filter { it.isDigit() }.take(9)
                    },
                    label = { Text("Enter Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = SteelBorder,
                        focusedLabelColor = accent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_amount_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("1000", "10000", "50000", "99999").forEach { preset ->
                        OutlinedButton(
                            onClick = { amountText = preset },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "+${preset.take(2)}k".let { if (preset == "1000") "+1k" else if (preset == "99999") "99k" else "+${preset.dropLast(3)}k" },
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = agreed,
                        onCheckedChange = { agreed = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = EmeraldSafe,
                            uncheckedColor = TextSecondary,
                        ),
                        modifier = Modifier.testTag("custom_grant_agree_checkbox"),
                    )
                    Text(
                        text = "I agree to credit ${formatNumber(parsedAmount)} ${if (isDiamonds) "Diamonds" else "Gold Coins"} to my account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAgreeAndGrant(isDiamonds, parsedAmount) },
                enabled = agreed && parsedAmount > 0L,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldSafe,
                    contentColor = TacticalObsidian,
                ),
                modifier = Modifier.testTag("custom_grant_confirm_button"),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AGREE & ADD", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        },
    )
}
