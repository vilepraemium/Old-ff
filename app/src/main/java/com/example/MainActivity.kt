package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.FireZoneRepository
import com.example.model.UserProfile
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.BattleRoyaleScreen
import com.example.ui.screens.CareerScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.LuckRoyaleScreen
import com.example.ui.screens.StoreScreen
import com.example.ui.screens.TopUpScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.TacticalObsidian
import com.example.ui.theme.FireZoneTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.FireZoneViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

enum class AppScreen {
    LOBBY,
    STORE,
    LUCK_ROYALE,
    VAULT,
    TOP_UP,
    CAREER,
    MATCH,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FireZoneTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        key(user.uid) {
            FireZoneAuthenticatedRoot(
                currentUserId = user.uid,
                displayName = user.displayName,
            )
        }
    }
}

@Composable
private fun FireZoneAuthenticatedRoot(
    currentUserId: String,
    displayName: String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: FireZoneViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FireZoneViewModel(
                    repository = FireZoneRepository(db),
                    currentUserId = currentUserId,
                    displayName = displayName,
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val ownedItemsState by viewModel.ownedItemsState.collectAsStateWithLifecycle()
    val matchHistoryState by viewModel.matchHistoryState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf(AppScreen.LOBBY) }

    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(3200L)
            viewModel.clearBanner()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalObsidian)
    ) {
        when (val pState = profileState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(color = AmberGold)
                        Text(
                            text = "LOADING SURVIVOR PROFILE...",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AmberGold,
                        )
                    }
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = CharcoalPanel,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, CrimsonAlert),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "SYNC ERROR",
                                style = MaterialTheme.typography.headlineMedium,
                                color = CrimsonAlert,
                            )
                            Text(
                                text = pState.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                            )
                            Button(
                                onClick = {
                                    signOutUser(credentialManager, scope)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberGold,
                                    contentColor = TacticalObsidian,
                                ),
                            ) {
                                Text("SIGN OUT & RETRY", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            is UiState.Success<UserProfile> -> {
                val profile = pState.data
                val ownedItems = (ownedItemsState as? UiState.Success)?.data ?: emptyList()
                val matchHistory = (matchHistoryState as? UiState.Success)?.data ?: emptyList()
                val transactions = (transactionsState as? UiState.Success)?.data ?: emptyList()

                when (currentScreen) {
                    AppScreen.LOBBY -> {
                        LobbyScreen(
                            profile = profile,
                            selectedMode = selectedMode,
                            ownedCount = ownedItems.size,
                            onSelectMode = { viewModel.selectMode(it) },
                            onOpenStore = { currentScreen = AppScreen.STORE },
                            onOpenLuckRoyale = { currentScreen = AppScreen.LUCK_ROYALE },
                            onOpenVault = { currentScreen = AppScreen.VAULT },
                            onOpenTopUp = { currentScreen = AppScreen.TOP_UP },
                            onOpenCareer = { currentScreen = AppScreen.CAREER },
                            onStartMatch = { currentScreen = AppScreen.MATCH },
                            onSignOut = {
                                signOutUser(credentialManager, scope)
                            },
                        )
                    }

                    AppScreen.STORE -> {
                        StoreScreen(
                            profile = profile,
                            ownedItems = ownedItems,
                            isBusy = isBusy,
                            onPurchase = { item, useDiamonds -> viewModel.purchaseItem(item, useDiamonds) },
                            onEquip = { item -> viewModel.equipItem(item) },
                            onOpenTopUp = { currentScreen = AppScreen.TOP_UP },
                            onBack = { currentScreen = AppScreen.LOBBY },
                        )
                    }

                    AppScreen.VAULT -> {
                        VaultScreen(
                            profile = profile,
                            ownedItems = ownedItems,
                            onEquip = { item -> viewModel.equipItem(item) },
                            onOpenStore = { currentScreen = AppScreen.STORE },
                            onOpenTopUp = { currentScreen = AppScreen.TOP_UP },
                            onBack = { currentScreen = AppScreen.LOBBY },
                        )
                    }

                    AppScreen.LUCK_ROYALE -> {
                        LuckRoyaleScreen(
                            profile = profile,
                            ownedItems = ownedItems,
                            isBusy = isBusy,
                            onPerformSpin = { royale, isTenPlusOne, onResult ->
                                viewModel.performLuckSpin(royale, isTenPlusOne, onResult)
                            },
                            onOpenTopUp = { currentScreen = AppScreen.TOP_UP },
                            onBack = { currentScreen = AppScreen.LOBBY },
                        )
                    }

                    AppScreen.TOP_UP -> {
                        TopUpScreen(
                            profile = profile,
                            transactions = transactions,
                            isBusy = isBusy,
                            onGrantPack = { isDiamonds, amount, isCustomHoldGrant, note, diamondCost ->
                                viewModel.grantCurrencyPack(
                                    isDiamonds = isDiamonds,
                                    amount = amount,
                                    isCustomHoldGrant = isCustomHoldGrant,
                                    note = note,
                                    diamondCostForGoldPack = diamondCost,
                                )
                            },
                            onBack = { currentScreen = AppScreen.LOBBY },
                        )
                    }

                    AppScreen.CAREER -> {
                        CareerScreen(
                            profile = profile,
                            matchHistory = matchHistory,
                            onOpenTopUp = { currentScreen = AppScreen.TOP_UP },
                            onBack = { currentScreen = AppScreen.LOBBY },
                        )
                    }

                    AppScreen.MATCH -> {
                        BattleRoyaleScreen(
                            profile = profile,
                            mode = selectedMode,
                            onMatchFinished = { mode, placement, kills, damage, seconds, onRecorded ->
                                viewModel.recordMatchResult(
                                    mode = mode,
                                    placement = placement,
                                    kills = kills,
                                    damageDealt = damage,
                                    survivalSeconds = seconds,
                                    onRecorded = onRecorded,
                                )
                            },
                            onExitToLobby = { currentScreen = AppScreen.LOBBY },
                        )
                    }
                }
            }
        }

        // Floating Tactical Status Toast
        AnimatedVisibility(
            visible = statusBanner != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp, start = 20.dp, end = 20.dp),
        ) {
            Surface(
                color = CharcoalPanel.copy(alpha = 0.96f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.5.dp, AmberGold),
                tonalElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = statusBanner ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberGold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}
