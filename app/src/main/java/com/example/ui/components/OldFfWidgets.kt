package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Rarity
import com.example.model.UserProfile
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SteelSlate
import com.example.ui.theme.TacticalObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

fun formatNumber(value: Long): String =
    NumberFormat.getNumberInstance(Locale.US).format(value)

@Composable
fun OldFfTopCurrencyBar(
    profile: UserProfile,
    onOpenTopUp: () -> Unit,
    onSignOut: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    screenTitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        TacticalObsidian.copy(alpha = 0.92f),
                        CharcoalPanel.copy(alpha = 0.85f),
                    )
                )
            )
            .border(1.dp, SteelBorder.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (onBack != null && screenTitle != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("top_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = AmberGold,
                    )
                }
                Text(
                    text = screenTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = AmberGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AmberGold.copy(alpha = 0.2f))
                        .border(1.5.dp, AmberGold, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MilitaryTech,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = profile.callsign.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "LVL ${profile.level} • ${profile.booyahs} BOOYAH",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberGold,
                        maxLines = 1,
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CurrencyPill(
                icon = Icons.Filled.MonetizationOn,
                amount = formatNumber(profile.goldCoins),
                tint = AmberGold,
                onClick = onOpenTopUp,
                testTag = "top_gold_pill",
            )
            CurrencyPill(
                icon = Icons.Filled.Diamond,
                amount = formatNumber(profile.diamonds),
                tint = DiamondCyan,
                onClick = onOpenTopUp,
                testTag = "top_diamond_pill",
            )
            if (onSignOut != null) {
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .size(40.dp)
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
}

@Composable
fun CurrencyPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    amount: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        onClick = onClick,
        color = SteelSlate,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.6f)),
        modifier = Modifier
            .height(34.dp)
            .testTag(testTag),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 6.dp, end = 4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add currency",
                    tint = TacticalObsidian,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
fun RarityChip(rarity: Rarity) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(rarity.color.copy(alpha = 0.22f))
            .border(1.dp, rarity.color.copy(alpha = 0.8f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = rarity.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = rarity.color,
        )
    }
}
