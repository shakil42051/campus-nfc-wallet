package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.attendance.repository.AttendanceRepository
import com.example.backup.gateway.BackupFileGateway
import com.example.backup.model.BackupHistoryItem
import com.example.backup.model.RestoreMode
import com.example.backup.model.RestorePreview
import com.example.backup.model.RestoreResult
import com.example.backup.repository.BackupHistoryRepository
import com.example.backup.service.BackupService
import com.example.backup.service.PreparedBackupArtifact
import com.example.backup.service.RestoreService
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.UsageTransactionEntity
import com.example.data.model.BatchCardItem
import com.example.data.model.NfcScanResult
import com.example.data.repository.AppSettings
import com.example.data.repository.CardRepository
import com.example.data.repository.SettingsRepository
import com.example.security.RoleAccessManager
import com.example.security.RoleAccessState
import com.example.sync.CampusServerSyncService
import com.example.sync.ServerAuthResult
import com.example.sync.WalletSyncResult
import com.example.utils.FeedbackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class CardFilter {
    ALL, FAVORITES, ARCHIVED, ISO14443, MIFARE, DESFIRE
}

enum class SortOrder {
    NEWEST, OLDEST, NAME, MANUFACTURER
}

data class DashboardStats(
    val totalCards: Int = 0,
    val totalFavorites: Int = 0,
    val totalArchived: Int = 0,
    val uniqueManufacturers: Int = 0,
    val desfireCount: Int = 0,
    val mifareCount: Int = 0
)

data class BackupOverviewState(
    val snapshots: List<File> = emptyList(),
    val history: List<BackupHistoryItem> = emptyList(),
    val lastBackupTimestamp: Long? = null,
    val lastRestoreTimestamp: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val cardRepository: CardRepository,
    private val settingsRepository: SettingsRepository,
    private val attendanceRepository: AttendanceRepository,
    private val campusServerSyncService: CampusServerSyncService,
    private val roleAccessManager: RoleAccessManager,
    private val feedbackManager: FeedbackManager,
    private val backupService: BackupService,
    private val restoreService: RestoreService,
    private val backupHistoryRepository: BackupHistoryRepository,
    private val backupFileGateway: BackupFileGateway
) : ViewModel() {

    // Server Link & Wallet Sync state
    val serverConfig: StateFlow<AttendanceConfigEntity> = attendanceRepository.configFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AttendanceConfigEntity()
        )

    val roleAccessState: StateFlow<RoleAccessState> = roleAccessManager.state

    private val _isWalletSyncing = MutableStateFlow(false)
    val isWalletSyncing: StateFlow<Boolean> = _isWalletSyncing.asStateFlow()

    private val _walletSyncMessage = MutableStateFlow<String?>(null)
    val walletSyncMessage: StateFlow<String?> = _walletSyncMessage.asStateFlow()

    fun clearWalletSyncMessage() {
        _walletSyncMessage.value = null
    }

    fun syncWalletWithServer() {
        viewModelScope.launch {
            _isWalletSyncing.value = true
            try {
                val result = campusServerSyncService.syncAllToServer()
                _walletSyncMessage.value = result.message
            } finally {
                _isWalletSyncing.value = false
            }
        }
    }

    fun authenticateWalletServer(
        serverUrl: String,
        port: Int,
        useHttps: Boolean,
        username: String,
        password: String,
        onResult: (ServerAuthResult) -> Unit
    ) {
        viewModelScope.launch {
            _isWalletSyncing.value = true
            try {
                val res = campusServerSyncService.authenticateWithServer(
                    serverUrl = serverUrl,
                    port = port,
                    useHttps = useHttps,
                    username = username,
                    password = password
                )
                onResult(res)
            } finally {
                _isWalletSyncing.value = false
            }
        }
    }

    fun linkWalletServerWithApiKey(
        serverUrl: String,
        port: Int,
        useHttps: Boolean,
        authType: String,
        apiKeyOrToken: String,
        providerType: String,
        campusId: String,
        deviceId: String,
        onResult: (WalletSyncResult) -> Unit
    ) {
        viewModelScope.launch {
            _isWalletSyncing.value = true
            try {
                val res = campusServerSyncService.linkWithApiKey(
                    serverUrl = serverUrl,
                    port = port,
                    useHttps = useHttps,
                    authType = authType,
                    apiKeyOrToken = apiKeyOrToken,
                    providerType = providerType,
                    campusId = campusId,
                    deviceId = deviceId
                )
                onResult(res)
            } finally {
                _isWalletSyncing.value = false
            }
        }
    }

    fun unlinkWalletServer(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            campusServerSyncService.unlinkServer()
            _walletSyncMessage.value = "Server unlinked. Wallet is now in Local Vault mode."
            onDone()
        }
    }

    fun updateAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            campusServerSyncService.setAutoSyncEnabled(enabled)
        }
    }

    // Scan state
    private val _latestScan = MutableStateFlow<NfcScanResult?>(null)
    val latestScan: StateFlow<NfcScanResult?> = _latestScan.asStateFlow()

    private val _latestScanAutoSaved = MutableStateFlow(false)
    val latestScanAutoSaved: StateFlow<Boolean> = _latestScanAutoSaved.asStateFlow()

    private val _isScanningActive = MutableStateFlow(true)
    val isScanningActive: StateFlow<Boolean> = _isScanningActive.asStateFlow()

    private val _scanErrorMessage = MutableStateFlow<String?>(null)
    val scanErrorMessage: StateFlow<String?> = _scanErrorMessage.asStateFlow()

    // Batch Processing State
    private val _isBatchMode = MutableStateFlow(false)
    val isBatchMode: StateFlow<Boolean> = _isBatchMode.asStateFlow()

    private val _batchQueue = MutableStateFlow<List<BatchCardItem>>(emptyList())
    val batchQueue: StateFlow<List<BatchCardItem>> = _batchQueue.asStateFlow()

    // Dashboard search & filter state
    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow(CardFilter.ALL)
    val selectedSortOrder = MutableStateFlow(SortOrder.NEWEST)

    // Transaction search state
    val transactionSearchQuery = MutableStateFlow("")

    val transactions: StateFlow<List<UsageTransactionEntity>> = transactionSearchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            cardRepository.allTransactions
        } else {
            cardRepository.searchTransactions(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Settings state
    val settings: StateFlow<AppSettings> = settingsRepository.settings

    // App Lock State (uses monotonic elapsedRealtime to resist clock tampering)
    private val _isUnlocked = MutableStateFlow(!settingsRepository.settings.value.isAppLockEnabled)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var lastPauseElapsedMs: Long = 0L
    private var suppressAutoLockUntilElapsedMs: Long = 0L

    fun unlockApp() {
        settingsRepository.resetFailedAuthAttempts()
        _isUnlocked.value = true
    }

    fun lockApp() {
        if (settings.value.isAppLockEnabled) {
            _isUnlocked.value = false
        }
    }

    /**
     * Suppresses immediate auto-lock when launching expected external activities
     * (e.g. SAF document picker, system share sheet, NFC settings) for up to 2 minutes.
     */
    fun suppressNextAutoLock(windowMs: Long = 120_000L) {
        suppressAutoLockUntilElapsedMs = SystemClock.elapsedRealtime() + windowMs
    }

    fun onAppPaused(isChangingConfigurations: Boolean = false) {
        if (isChangingConfigurations) {
            // Do not lock on screen rotation or dark-mode config changes
            return
        }
        lastPauseElapsedMs = SystemClock.elapsedRealtime()
    }

    fun onAppResumed() {
        val currentSettings = settings.value
        if (!currentSettings.isAppLockEnabled) {
            _isUnlocked.value = true
            return
        }

        val nowElapsed = SystemClock.elapsedRealtime()
        if (nowElapsed < suppressAutoLockUntilElapsedMs) {
            suppressAutoLockUntilElapsedMs = 0L
            return
        }

        if (lastPauseElapsedMs == 0L) {
            // Fresh cold start with App Lock enabled -> keep locked until verified
            if (!_isUnlocked.value) return
            return
        }

        val elapsedSeconds = ((nowElapsed - lastPauseElapsedMs).coerceAtLeast(0L)) / 1000L
        val timeoutSeconds = currentSettings.autoLockTimeoutSeconds

        if (elapsedSeconds >= timeoutSeconds) {
            _isUnlocked.value = false
        }
        lastPauseElapsedMs = 0L
    }

    // Unfiltered active cards (for History, Card Switchers, and Detail Screen — immune to Dashboard filters)
    val allActiveCards: StateFlow<List<CardEntity>> = cardRepository.allCards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun isDesfireOrIsoDep(card: CardEntity): Boolean {
        return card.technology.contains("DESFire", ignoreCase = true) ||
            card.technology.contains("IsoDep", ignoreCase = true) ||
            card.cardStandard.contains("14443-4", ignoreCase = true)
    }

    // Dashboard Filtered & Sorted Cards List
    val cardList: StateFlow<List<CardEntity>> = combine(
        searchQuery,
        selectedFilter,
        selectedSortOrder
    ) { query, filter, sort ->
        Triple(query, filter, sort)
    }.flatMapLatest { (query, filter, sort) ->
        val baseFlow = if (query.isBlank()) {
            when (filter) {
                CardFilter.ARCHIVED -> cardRepository.archivedCards
                CardFilter.FAVORITES -> cardRepository.favoriteCards
                else -> cardRepository.allCards
            }
        } else {
            if (filter == CardFilter.ARCHIVED) {
                cardRepository.searchArchivedCards(query)
            } else {
                cardRepository.searchCards(query)
            }
        }
        baseFlow.map { list ->
            val filtered = list.filter { card ->
                when (filter) {
                    CardFilter.ALL -> true
                    CardFilter.FAVORITES -> card.isFavorite
                    CardFilter.ARCHIVED -> card.isArchived
                    CardFilter.ISO14443 -> card.cardStandard.contains("14443", ignoreCase = true)
                    CardFilter.MIFARE -> card.technology.contains("MIFARE", ignoreCase = true)
                    CardFilter.DESFIRE -> isDesfireOrIsoDep(card)
                }
            }
            when (sort) {
                SortOrder.NEWEST -> filtered.sortedByDescending { it.timestamp }
                SortOrder.OLDEST -> filtered.sortedBy { it.timestamp }
                SortOrder.NAME -> filtered.sortedBy { it.nickname.lowercase(Locale.US) }
                SortOrder.MANUFACTURER -> filtered.sortedBy { it.manufacturer.lowercase(Locale.US) }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active Selected Card State
    val activeCard: StateFlow<CardEntity?> = combine(
        cardRepository.allCards,
        settingsRepository.settings
    ) { cards, currentSettings ->
        val selectedId = currentSettings.activeCardId
        val found = if (selectedId != null) cards.find { it.id == selectedId } else null
        found ?: cards.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun selectActiveCard(cardId: Long?) {
        settingsRepository.updateActiveCardId(cardId)
    }

    fun selectActiveCard(card: CardEntity?) {
        if (card != null && card.isArchived) return
        settingsRepository.updateActiveCardId(card?.id)
    }

    // Statistics state
    val stats: StateFlow<DashboardStats> = combine(
        cardRepository.allCards,
        cardRepository.archivedCards
    ) { activeCards, archivedCards ->
        DashboardStats(
            totalCards = activeCards.size,
            totalFavorites = activeCards.count { it.isFavorite },
            totalArchived = archivedCards.size,
            uniqueManufacturers = (activeCards + archivedCards).map { it.manufacturer }.distinct().size,
            desfireCount = activeCards.count { isDesfireOrIsoDep(it) },
            mifareCount = activeCards.count { it.technology.contains("MIFARE", ignoreCase = true) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    fun setBatchMode(enabled: Boolean) {
        _isBatchMode.value = enabled
    }

    fun addToBatchQueue(scanResult: NfcScanResult, nicknameOverride: String? = null, notes: String = "") {
        val count = _batchQueue.value.size + 1
        val defaultName = nicknameOverride.takeUnless { it.isNullOrBlank() }
            ?: "Batch ID #$count (${scanResult.uid.takeLast(4)})"
        val newItem = BatchCardItem(
            scanResult = scanResult,
            nickname = defaultName,
            notes = notes
        )
        _batchQueue.value = _batchQueue.value + newItem
    }

    fun removeFromBatchQueue(itemId: String) {
        _batchQueue.value = _batchQueue.value.filter { it.id != itemId }
    }

    fun updateBatchItemNickname(itemId: String, newNickname: String) {
        _batchQueue.value = _batchQueue.value.map { item ->
            if (item.id == itemId) item.copy(nickname = newNickname) else item
        }
    }

    fun clearBatchQueue() {
        _batchQueue.value = emptyList()
    }

    fun saveBatchQueue(onSuccess: (count: Int) -> Unit, onError: (String) -> Unit = {}) {
        val snapshot = _batchQueue.value.toList()
        if (snapshot.isEmpty()) return
        viewModelScope.launch {
            try {
                val savedIds = mutableSetOf<String>()
                snapshot.forEach { item ->
                    cardRepository.saveCardFromScan(item.scanResult, item.nickname, item.notes)
                    savedIds.add(item.id)
                }
                // Remove only items that were actually saved (preserves any items scanned during save)
                _batchQueue.value = _batchQueue.value.filterNot { it.id in savedIds }
                onSuccess(savedIds.size)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to save batch queue")
            }
        }
    }

    fun onTagScanned(scanResult: NfcScanResult) {
        if (!_isScanningActive.value) return
        if (settings.value.isAppLockEnabled && !_isUnlocked.value) return

        _scanErrorMessage.value = null

        val currentSettings = settings.value
        feedbackManager.triggerScanFeedback(
            enableVibration = currentSettings.enableVibration,
            enableSound = currentSettings.enableSound
        )

        if (_isBatchMode.value) {
            addToBatchQueue(scanResult)
        } else {
            _latestScan.value = scanResult
            if (currentSettings.autoExportOnScan) {
                _latestScanAutoSaved.value = true
                viewModelScope.launch {
                    cardRepository.saveCardFromScan(
                        scanResult = scanResult,
                        nickname = "Auto-Saved (${scanResult.uid.takeLast(5)})"
                    )
                }
            } else {
                _latestScanAutoSaved.value = false
            }
        }
    }

    fun clearLatestScan() {
        _latestScan.value = null
        _latestScanAutoSaved.value = false
        _scanErrorMessage.value = null
    }

    fun onScanError(message: String) {
        _scanErrorMessage.value = message
    }

    fun clearScanError() {
        _scanErrorMessage.value = null
    }

    fun saveCard(scanResult: NfcScanResult, nickname: String, notes: String = "") {
        viewModelScope.launch {
            cardRepository.saveCardFromScan(scanResult, nickname, notes)
            campusServerSyncService.syncIfAutoEnabled()
        }
    }

    fun updateCardDetails(existingCard: CardEntity, scanResult: NfcScanResult, nickname: String, notes: String) {
        viewModelScope.launch {
            cardRepository.updateSavedCard(existingCard, scanResult, nickname, notes)
            campusServerSyncService.syncIfAutoEnabled()
        }
    }

    fun toggleFavorite(card: CardEntity) {
        viewModelScope.launch {
            cardRepository.toggleFavorite(card)
        }
    }

    fun toggleArchive(card: CardEntity) {
        viewModelScope.launch {
            val willBeArchived = !card.isArchived
            cardRepository.toggleArchive(card)
            if (willBeArchived && settings.value.activeCardId == card.id) {
                selectActiveCard(null as Long?)
            }
        }
    }

    var lastDeletedCard: CardEntity? = null
        private set

    var lastDeletedTransaction: UsageTransactionEntity? = null
        private set

    fun deleteCard(card: CardEntity) {
        lastDeletedCard = card
        viewModelScope.launch {
            cardRepository.deleteCard(card)
            if (settings.value.activeCardId == card.id) {
                selectActiveCard(null as Long?)
            }
        }
    }

    fun restoreLastDeletedCard() {
        val cardToRestore = lastDeletedCard ?: return
        viewModelScope.launch {
            cardRepository.restoreCards(listOf(cardToRestore))
            lastDeletedCard = null
        }
    }

    fun clearAllCards() {
        viewModelScope.launch {
            cardRepository.clearAll()
            selectActiveCard(null as Long?)
        }
    }

    fun setScanningActive(active: Boolean) {
        _isScanningActive.value = active
    }

    fun getCardById(id: Long): Flow<CardEntity?> = cardRepository.getCardById(id)

    fun getTransactionsForCard(cardId: Long): Flow<List<UsageTransactionEntity>> =
        cardRepository.getTransactionsForCard(cardId)

    fun logTransaction(
        cardId: Long,
        cardNickname: String,
        cardUid: String,
        usageType: String,
        purposeOrAction: String,
        status: String = "SUCCESS",
        terminalLocation: String = "NFC Terminal"
    ) {
        viewModelScope.launch {
            cardRepository.logTransaction(
                cardId = cardId,
                cardNickname = cardNickname,
                cardUid = cardUid,
                usageType = usageType,
                purposeOrAction = purposeOrAction,
                status = status,
                terminalLocation = terminalLocation
            )
            campusServerSyncService.syncIfAutoEnabled()
        }
    }

    fun deleteTransaction(id: Long) {
        val txToDelete = transactions.value.find { it.id == id }
        lastDeletedTransaction = txToDelete
        viewModelScope.launch {
            cardRepository.deleteTransaction(id)
        }
    }

    fun restoreLastDeletedTransaction() {
        val txToRestore = lastDeletedTransaction ?: return
        viewModelScope.launch {
            cardRepository.restoreTransaction(txToRestore)
            lastDeletedTransaction = null
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            cardRepository.clearAllTransactions()
        }
    }

    // Settings Updaters
    fun updateVibration(enabled: Boolean) = settingsRepository.updateVibration(enabled)
    fun updateSound(enabled: Boolean) = settingsRepository.updateSound(enabled)
    fun updateAutoExport(enabled: Boolean) = settingsRepository.updateAutoExport(enabled)
    fun updateDarkMode(enabled: Boolean) = settingsRepository.updateDarkMode(enabled)
    fun updateDeveloperMode(enabled: Boolean) = settingsRepository.updateDeveloperMode(enabled)
    fun updateBiometricEnabled(enabled: Boolean) = settingsRepository.updateBiometricEnabled(enabled)
    fun updateRequireAuthForBackupRestore(enabled: Boolean) = settingsRepository.updateRequireAuthForBackupRestore(enabled)

    fun updateAppLockEnabled(enabled: Boolean) {
        settingsRepository.updateAppLockEnabled(enabled)
        // Stay unlocked during the current session while configuring settings; locks on next pause/cold start
        _isUnlocked.value = true
    }

    fun updateAutoLockTimeoutSeconds(seconds: Int) = settingsRepository.updateAutoLockTimeoutSeconds(seconds)
    fun updateAuthMethod(method: String) = settingsRepository.updateAuthMethod(method)
    fun setSecurityPassword(password: String) = settingsRepository.setSecurityPassword(password)
    fun setSecurityPinAndQuestion(pin: String, question: String, answer: String) =
        settingsRepository.setSecurityPinAndQuestion(pin, question, answer)
    fun setSecurityQuestionAndAnswer(question: String, answer: String) =
        settingsRepository.setSecurityQuestionAndAnswer(question, answer)
    fun clearSecurityPin() = settingsRepository.clearSecurityPin()
    fun verifyPin(pin: String): Boolean = settingsRepository.verifyPin(pin)
    fun verifyPassword(password: String): Boolean = settingsRepository.verifyPassword(password)
    fun verifySecretAnswer(answer: String): Boolean = settingsRepository.verifySecretAnswer(answer)
    fun recordFailedAuthAttempt(): Pair<Int, Int> = settingsRepository.recordFailedAuthAttempt()
    fun resetFailedAuthAttempts() = settingsRepository.resetFailedAuthAttempts()
    fun getRemainingLockoutSeconds(): Int = settingsRepository.getRemainingLockoutSeconds()

    // Backup & Restore System Methods (all off Main Thread)
    private val _backupOverview = MutableStateFlow(BackupOverviewState())
    val backupOverview: StateFlow<BackupOverviewState> = _backupOverview.asStateFlow()

    fun refreshBackupOverview() {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                BackupOverviewState(
                    snapshots = backupService.getInternalSnapshots(),
                    history = backupHistoryRepository.getHistory(),
                    lastBackupTimestamp = backupHistoryRepository.getLastSuccessfulBackupTimestamp(),
                    lastRestoreTimestamp = backupHistoryRepository.getLastSuccessfulRestoreTimestamp()
                )
            }
            _backupOverview.value = state
        }
    }

    fun createEncryptedBackup(password: String, fileName: String, callback: (Result<PreparedBackupArtifact>) -> Unit) {
        viewModelScope.launch {
            try {
                val artifact = backupService.createEncryptedBackupBytes(password, fileName)
                callback(Result.success(artifact))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    fun recordExternalBackupCompleted(artifact: PreparedBackupArtifact, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            backupService.recordExternalBackupCompleted(artifact)
            refreshBackupOverview()
            onDone()
        }
    }

    fun createInternalSnapshot(password: String, label: String, callback: (Result<ByteArray>) -> Unit) {
        viewModelScope.launch {
            try {
                val bytes = backupService.createInternalSnapshot(password, label)
                refreshBackupOverview()
                callback(Result.success(bytes))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    fun deleteSnapshotAsync(fileName: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val deleted = backupService.deleteSnapshot(fileName)
            refreshBackupOverview()
            callback(deleted)
        }
    }

    fun readSnapshotBytesAsync(fileName: String, callback: (ByteArray?) -> Unit) {
        viewModelScope.launch {
            val bytes = backupService.readSnapshotBytes(fileName)
            callback(bytes)
        }
    }

    fun parseAndDecryptBackup(
        fileBytes: ByteArray,
        password: String,
        fileSizeBytes: Long,
        callback: (Result<RestorePreview>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val preview = restoreService.parseAndDecryptBackup(fileBytes, password, fileSizeBytes)
                callback(Result.success(preview))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    fun executeRestore(
        preview: RestorePreview,
        mode: RestoreMode,
        selectedCategories: Set<String>,
        fileName: String,
        callback: (Result<RestoreResult>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = restoreService.executeRestore(preview, mode, selectedCategories, fileName)
                refreshBackupOverview()
                callback(Result.success(result))
            } catch (e: Exception) {
                refreshBackupOverview()
                callback(Result.failure(e))
            }
        }
    }

    fun readBytesFromUriAsync(context: Context, uri: Uri, callback: (Result<ByteArray>) -> Unit) {
        viewModelScope.launch {
            try {
                val bytes = backupFileGateway.readBytesFromUri(context, uri)
                callback(Result.success(bytes))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    fun writeBytesToUriAsync(context: Context, uri: Uri, bytes: ByteArray, callback: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                backupFileGateway.writeBytesToUri(context, uri, bytes)
                callback(Result.success(Unit))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    fun buildDiagnosticsString(exception: Exception): String =
        backupFileGateway.buildDiagnosticsString(exception)

    override fun onCleared() {
        super.onCleared()
        feedbackManager.release()
    }
}
