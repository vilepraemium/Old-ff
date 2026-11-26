package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.FieryOrange
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.TacticalObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCheckingAutoSignIn by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAutoSignIn = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingAutoSignIn = false
            },
            scope = scope,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalObsidian)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_drop_banner),
            contentDescription = stringResource(R.string.auth_banner_desc),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            TacticalObsidian.copy(alpha = 0.55f),
                            TacticalObsidian.copy(alpha = 0.45f),
                            TacticalObsidian.copy(alpha = 0.94f),
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Classic Season Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(CharcoalPanel.copy(alpha = 0.85f))
                    .border(1.dp, AmberGold.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = AmberGold,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "SEASON 1 CLASSIC EDITION • OPEN WORLD",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberGold,
                )
            }

            // Bottom Login Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                color = CharcoalPanel.copy(alpha = 0.92f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, AmberGold.copy(alpha = 0.7f)),
                tonalElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "FIREZONE",
                        style = MaterialTheme.typography.displayLarge,
                        color = AmberGold,
                    )
                    Text(
                        text = "Classic Bermuda Battlegrounds • Luck Royale • Vault & Diamond Store",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        FeaturePill(Icons.Filled.Shield, "20-Bot Island", AmberGold)
                        FeaturePill(Icons.Filled.Diamond, "Diamonds & Gold", DiamondCyan)
                    }

                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(CrimsonAlert.copy(alpha = 0.16f))
                                .border(1.dp, CrimsonAlert, RoundedCornerShape(4.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFCA5A5),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            errorMessage = null
                            isSigningIn = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isSigningIn = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isSigningIn = false
                                },
                            )
                        },
                        enabled = !isSigningIn && !isCheckingAutoSignIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGold,
                            contentColor = TacticalObsidian,
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button"),
                    ) {
                        if (isSigningIn || isCheckingAutoSignIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = TacticalObsidian,
                                strokeWidth = 2.5.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isCheckingAutoSignIn) "CHECKING SURVIVOR ID..." else "SIGNING IN...",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Login,
                                contentDescription = stringResource(R.string.sign_in_google),
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SIGN IN WITH GOOGLE",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    Text(
                        text = "Your diamonds, gold coins, clothes vault, and battle stats sync automatically to your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TacticalObsidian.copy(alpha = 0.7f))
            .border(1.dp, SteelBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
    }
}
