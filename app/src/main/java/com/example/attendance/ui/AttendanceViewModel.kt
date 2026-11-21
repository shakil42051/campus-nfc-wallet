package com.example.attendance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.attendance.data.entity.AttendanceEntity
import com.example.attendance.data.entity.AttendanceLogEntity
import com.example.attendance.domain.model.AttendanceStats
import com.example.attendance.domain.model.SyncResult
import com.example.attendance.repository.AttendanceRepository
import com.example.data.local.entity.CardEntity
import com.example.data.repository.CardRepository
import com.example.security.RoleAccessManager
import com.example.security.RoleAccessState
import com.example.security.RoleAuthResult
import com.example.sync.CampusServerSyncService
import com.example.sync.ServerAuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class AttendanceViewModel(
    private val repository: AttendanceRepository,
    private val cardRepository: CardRepository,
    private val roleAccessManager: RoleAccessManager,
    private val campusServerSyncService: CampusServerSyncService
) : ViewModel() {

    val config: StateFlow<AttendanceConfigEntity> = repository.configFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AttendanceConfigEntity()
        )

    val roleState: StateFlow<RoleAccessState> = roleAccessManager.state

    val savedCards: StateFlow<List<CardEntity>> = cardRepository.allCards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val records: StateFlow<List<AttendanceEntity>> = repository.allRecordsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val logs: StateFlow<List<AttendanceLogEntity>> = repository.allLogsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val stats: StateFlow<AttendanceStats> = repository.attendanceStatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AttendanceStats()
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _isAuthenticatingServer = MutableStateFlow(false)
    val isAuthenticatingServer: StateFlow<Boolean> = _isAuthenticatingServer.asStateFlow()

    private val _testResult = MutableStateFlow<SyncResult?>(null)
    val testResult: StateFlow<SyncResult?> = _testResult.asStateFlow()

    private val _syncFeedback = MutableStateFlow<SyncResult?>(null)
    val syncFeedback: StateFlow<SyncResult?> = _syncFeedback.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow("ALL") // TODAY, WEEK, MONTH, ALL
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    val filteredRecords: StateFlow<List<AttendanceEntity>> = combine(
        records,
        config,
        selectedTimeRange
    ) { allRecords, currentConfig, range ->
        val studentScoped = if (currentConfig.activeStudentId.isNotBlank()) {
            allRecords.filter { it.studentId == currentConfig.activeStudentId }
        } else {
            allRecords
        }
        val cutoffMs = computeRangeCutoffMs(range)
        if (cutoffMs == 0L) studentScoped else studentScoped.filter { it.timestamp >= cutoffMs }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun computeRangeCutoffMs(range: String): Long {
        if (range == "ALL") return 0L
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return when (range) {
            "TODAY" -> cal.timeInMillis
            "WEEK" -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.timeInMillis
            }
            "MONTH" -> {
                cal.add(Calendar.DAY_OF_YEAR, -29)
                cal.timeInMillis
            }
            else -> 0L
        }
    }

    private fun emitUnauthorized(requiredRole: String) {
        _syncFeedback.value = SyncResult(
            isSuccess = false,
            syncedCount = 0,
            failedCount = 0,
            message = "Access denied: $requiredRole authentication is required for this action.",
            provider = config.value.providerType
        )
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
    }

    fun clearSyncFeedback() {
        _syncFeedback.value = null
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    fun getRemainingRoleLockoutSeconds(): Int = roleAccessManager.getRemainingLockoutSeconds()

    fun dropToStudentRole() {
        roleAccessManager.lockElevatedSession()
        _syncFeedback.value = SyncResult(
            isSuccess = true,
            syncedCount = 0,
            failedCount = 0,
            message = "Staff/Admin session locked. Returned to Student mode.",
            provider = config.value.providerType
        )
    }

    fun initializeAdminPasscode(
        adminPasscode: String,
        teacherPasscode: String?,
        onResult: (Result<Unit>) -> Unit
    ) {
        val result = roleAccessManager.initializeAdminPasscode(adminPasscode, teacherPasscode)
        onResult(result)
    }

    fun unlockLocalRole(
        targetRole: String,
        passcode: String,
        onResult: (RoleAuthResult) -> Unit
    ) {
        val result = roleAccessManager.unlockLocalRole(targetRole, passcode)
        onResult(result)
    }

    fun setTeacherPasscode(newPasscode: String, onResult: (Result<Unit>) -> Unit) {
        if (!roleState.value.canManageServerAndRoles) {
            onResult(Result.failure(SecurityException("Administrator access required.")))
            return
        }
        onResult(roleAccessManager.setTeacherPasscode(newPasscode))
    }

    fun rotateAdminPasscode(
        currentPasscode: String,
        newPasscode: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (!roleState.value.canManageServerAndRoles) {
            onResult(Result.failure(SecurityException("Administrator access required.")))
            return
        }
        onResult(roleAccessManager.rotateAdminPasscode(currentPasscode, newPasscode))
    }

    fun authenticateWithServer(
        serverUrl: String,
        port: Int,
        useHttps: Boolean,
        username: String,
        password: String,
        onResult: (ServerAuthResult) -> Unit
    ) {
        viewModelScope.launch {
            _isAuthenticatingServer.value = true
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
                _isAuthenticatingServer.value = false
            }
        }
    }

    fun unlinkServerSession() {
        viewModelScope.launch {
            campusServerSyncService.unlinkServer()
            _syncFeedback.value = SyncResult(
                isSuccess = true,
                syncedCount = 0,
                failedCount = 0,
                message = "Server unlinked and elevated role cleared.",
                provider = config.value.providerType
            )
        }
    }

    fun punchAttendance(
        usageType: String = "PUNCH_IN",
        location: String = "Main Gate #1 Turnstile",
        status: String = "PRESENT"
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val currentConfig = config.value
                val result = repository.punchAttendance(
                    usageType = usageType,
                    location = location,
                    cardUid = currentConfig.linkedCardUid,
                    cardNickname = currentConfig.linkedCardNickname,
                    status = status
                )
                _syncFeedback.value = result
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun punchWithCard(
        cardUid: String,
        cardNickname: String,
        usageType: String = "PUNCH_IN",
        location: String = "Main Gate #1 Turnstile",
        status: String = "PRESENT"
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val currentConfig = config.value
                val result = repository.punchAttendance(
                    usageType = usageType,
                    location = location,
                    cardUid = cardUid,
                    cardNickname = cardNickname,
                    status = status
                )
                _syncFeedback.value = result

                val matchedCardId = savedCards.value.find {
                    it.uid.equals(cardUid, ignoreCase = true)
                }?.id ?: 0L

                cardRepository.logTransaction(
                    cardId = matchedCardId,
                    cardNickname = cardNickname.ifBlank { "Campus Pass" },
                    cardUid = cardUid.ifBlank { "LOCAL-PASS" },
                    usageType = "Attendance $usageType ($status)",
                    purposeOrAction = "${currentConfig.providerType} Attendance Punch ($location)",
                    status = if (result.isSuccess) "SUCCESS" else "PENDING",
                    terminalLocation = location
                )
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Teacher or Admin only: enforces RBAC at ViewModel level.
     */
    fun updateRecordStatus(recordId: Long, newStatus: String) {
        if (!roleState.value.canEditAttendanceRecords) {
            emitUnauthorized("Teacher or Administrator")
            return
        }
        viewModelScope.launch {
            repository.updateRecordStatus(recordId, newStatus)
        }
    }

    /**
     * Admin only: enforces RBAC at ViewModel level.
     */
    fun testServerConnection(configToTest: AttendanceConfigEntity) {
        if (!roleState.value.canManageServerAndRoles) {
            emitUnauthorized("Administrator")
            return
        }
        viewModelScope.launch {
            _testResult.value = null
            _isTestingConnection.value = true
            try {
                val result = repository.testServerConnection(configToTest)
                _testResult.value = result
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    /**
     * Admin only: enforces RBAC at ViewModel level.
     */
    fun saveServerConfig(newConfig: AttendanceConfigEntity) {
        if (!roleState.value.canManageServerAndRoles) {
            emitUnauthorized("Administrator")
            return
        }
        viewModelScope.launch {
            repository.saveServerConfig(newConfig.copy(userRole = config.value.userRole))
        }
    }

    /**
     * Updates student profile & linked card without allowing self-escalation of roles.
     */
    fun updateProfile(
        studentName: String,
        studentId: String,
        department: String,
        email: String,
        cardUid: String,
        cardNickname: String
    ) {
        viewModelScope.launch {
            val current = config.value
            val updated = current.copy(
                activeStudentName = studentName.trim(),
                activeStudentId = studentId.trim(),
                department = department.trim(),
                email = email.trim(),
                linkedCardUid = cardUid.trim(),
                linkedCardNickname = cardNickname.trim()
            )
            repository.saveServerConfig(updated)
        }
    }

    /**
     * Unified sync: syncs wallet cards, usage logs, and pending attendance punches to the linked campus server.
     */
    fun performManualSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val walletResult = campusServerSyncService.syncAllToServer()
                _syncFeedback.value = SyncResult(
                    isSuccess = walletResult.isSuccess,
                    syncedCount = walletResult.cardsPushed + walletResult.transactionsPushed + walletResult.attendanceSynced,
                    failedCount = if (walletResult.isSuccess) 0 else 1,
                    message = walletResult.message,
                    provider = config.value.providerType
                )
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Admin only: enforces RBAC at ViewModel level.
     */
    fun clearLogs() {
        if (!roleState.value.canClearLogsOrRecords) {
            emitUnauthorized("Administrator")
            return
        }
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    /**
     * Admin only: enforces RBAC at ViewModel level.
     */
    fun clearRecords() {
        if (!roleState.value.canClearLogsOrRecords) {
            emitUnauthorized("Administrator")
            return
        }
        viewModelScope.launch {
            repository.clearRecords()
        }
    }

    class Factory(
        private val repository: AttendanceRepository,
        private val cardRepository: CardRepository,
        private val roleAccessManager: RoleAccessManager,
        private val campusServerSyncService: CampusServerSyncService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
                return AttendanceViewModel(
                    repository = repository,
                    cardRepository = cardRepository,
                    roleAccessManager = roleAccessManager,
                    campusServerSyncService = campusServerSyncService
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
