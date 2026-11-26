package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.model.GameMode
import com.example.model.UserProfile
import com.example.ui.components.RetroLobbyUi

@Composable
fun LobbyScreen(
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
) {
    RetroLobbyUi(
        profile = profile,
        selectedMode = selectedMode,
        ownedCount = ownedCount,
        onSelectMode = onSelectMode,
        onOpenStore = onOpenStore,
        onOpenLuckRoyale = onOpenLuckRoyale,
        onOpenVault = onOpenVault,
        onOpenTopUp = onOpenTopUp,
        onOpenCareer = onOpenCareer,
        onStartMatch = onStartMatch,
        onSignOut = onSignOut,
    )
}
