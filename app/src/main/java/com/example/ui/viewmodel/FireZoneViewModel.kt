package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FireZoneRepository
import com.example.model.CatalogItem
import com.example.model.CurrencyTransaction
import com.example.model.GameMode
import com.example.model.MatchRecord
import com.example.model.MatchRewardSummary
import com.example.model.OwnedItem
import com.example.model.RoyaleType
import com.example.model.SpinOutcome
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class FireZoneViewModel(
    private val repository: FireZoneRepository,
    private val currentUserId: String,
    private val displayName: String?,
) : ViewModel() {

    val profileState: StateFlow<UiState<UserProfile>> = repository.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            if (profile != null) {
                UiState.Success(profile)
            } else {
                UiState.Loading
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load survivor account"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val ownedItemsState: StateFlow<UiState<List<OwnedItem>>> = repository.observeOwnedItems(currentUserId)
        .map<List<OwnedItem>, UiState<List<OwnedItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing owned items", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load vault items"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val matchHistoryState: StateFlow<UiState<List<MatchRecord>>> = repository.observeMatchHistory(currentUserId)
        .map<List<MatchRecord>, UiState<List<MatchRecord>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing match history", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load match history"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val transactionsState: StateFlow<UiState<List<CurrencyTransaction>>> = repository.observeTransactions(currentUserId)
        .map<List<CurrencyTransaction>, UiState<List<CurrencyTransaction>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing transactions", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load transactions"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    private val _selectedMode = MutableStateFlow(GameMode.BERMUDA_SOLO)
    val selectedMode: StateFlow<GameMode> = _selectedMode.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureStarterProfile(currentUserId, displayName)
                .onFailure { e ->
                    _statusBanner.value = e.message ?: "Failed to initialize survivor account."
                }
        }
    }

    fun selectMode(mode: GameMode) {
        _selectedMode.value = mode
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun grantCurrencyPack(
        isDiamonds: Boolean,
        amount: Long,
        isCustomHoldGrant: Boolean,
        note: String,
        diamondCostForGoldPack: Long = 0L,
        onComplete: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.grantCurrencyPack(
                userId = currentUserId,
                isDiamonds = isDiamonds,
                amount = amount,
                isCustomHoldGrant = isCustomHoldGrant,
                note = note,
                diamondCostForGoldPack = diamondCostForGoldPack,
            )
            _isBusy.value = false
            result.onSuccess {
                val unit = if (isDiamonds) "Diamonds" else "Gold Coins"
                _statusBanner.value = "+$amount $unit credited to your account!"
                onComplete(true)
            }.onFailure { e ->
                _statusBanner.value = e.message ?: "Currency grant failed."
                onComplete(false)
            }
        }
    }

    fun purchaseItem(item: CatalogItem, useDiamonds: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.purchaseCatalogItem(currentUserId, item, useDiamonds)
            _isBusy.value = false
            result.onSuccess {
                _statusBanner.value = "Unlocked & equipped ${item.name}!"
            }.onFailure { e ->
                _statusBanner.value = e.message ?: "Purchase failed."
            }
        }
    }

    fun equipItem(item: CatalogItem) {
        viewModelScope.launch {
            val result = repository.equipItem(currentUserId, item)
            result.onSuccess {
                _statusBanner.value = "Equipped ${item.name}"
            }.onFailure { e ->
                _statusBanner.value = e.message ?: "Could not equip item."
            }
        }
    }

    fun performLuckSpin(
        royaleType: RoyaleType,
        isTenPlusOne: Boolean,
        onResult: (Result<SpinOutcome>) -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.performLuckSpin(currentUserId, royaleType, isTenPlusOne)
            _isBusy.value = false
            result.onFailure { e ->
                _statusBanner.value = e.message ?: "Spin failed."
            }
            onResult(result)
        }
    }

    fun recordMatchResult(
        mode: GameMode,
        placement: Int,
        kills: Int,
        damageDealt: Int,
        survivalSeconds: Int,
        onRecorded: (Result<MatchRewardSummary>) -> Unit,
    ) {
        viewModelScope.launch {
            val result = repository.recordMatchResult(
                userId = currentUserId,
                mode = mode,
                placement = placement,
                kills = kills,
                damageDealt = damageDealt,
                survivalSeconds = survivalSeconds,
            )
            onRecorded(result)
        }
    }

    private companion object {
        const val TAG = "FireZoneVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
