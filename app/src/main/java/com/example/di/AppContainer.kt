package com.example.di

import android.content.Context
import com.example.attendance.repository.AttendanceRepository
import com.example.attendance.service.AuthenticationService
import com.example.attendance.service.DeviceService
import com.example.attendance.service.SyncService
import com.example.backup.gateway.AndroidBackupFileGateway
import com.example.backup.gateway.BackupFileGateway
import com.example.backup.repository.BackupHistoryRepository
import com.example.backup.repository.BackupRepository
import com.example.backup.service.BackupService
import com.example.backup.service.InternalAutoBackupManager
import com.example.backup.service.RestoreService
import com.example.backup.transaction.RestoreTransactionManager
import com.example.data.local.database.CardDatabase
import com.example.data.repository.CardRepository
import com.example.data.repository.SettingsRepository
import com.example.security.KeystoreManager
import com.example.security.RoleAccessManager
import com.example.sync.CampusServerSyncService
import com.example.utils.FeedbackManager

interface AppContainer {
    val cardRepository: CardRepository
    val settingsRepository: SettingsRepository
    val attendanceRepository: AttendanceRepository
    val roleAccessManager: RoleAccessManager
    val campusServerSyncService: CampusServerSyncService
    val keystoreManager: KeystoreManager
    val feedbackManager: FeedbackManager
    val cardDatabase: CardDatabase
    val backupRepository: BackupRepository
    val backupHistoryRepository: BackupHistoryRepository
    val backupService: BackupService
    val restoreService: RestoreService
    val backupFileGateway: BackupFileGateway
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val keystoreManager: KeystoreManager by lazy {
        KeystoreManager()
    }

    override val cardDatabase: CardDatabase by lazy {
        CardDatabase.getDatabase(context)
    }

    override val cardRepository: CardRepository by lazy {
        CardRepository(cardDatabase.cardDao(), cardDatabase.transactionDao(), keystoreManager)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    override val roleAccessManager: RoleAccessManager by lazy {
        RoleAccessManager(context)
    }

    override val backupRepository: BackupRepository by lazy {
        BackupRepository(
            cardDao = cardDatabase.cardDao(),
            transactionDao = cardDatabase.transactionDao(),
            attendanceDao = cardDatabase.attendanceDao(),
            attendanceConfigDao = cardDatabase.attendanceConfigDao(),
            attendanceLogDao = cardDatabase.attendanceLogDao(),
            cryptoProvider = keystoreManager
        )
    }

    override val backupHistoryRepository: BackupHistoryRepository by lazy {
        BackupHistoryRepository(context)
    }

    private val internalAutoBackupManager: InternalAutoBackupManager by lazy {
        InternalAutoBackupManager(context, backupHistoryRepository)
    }

    private val restoreTransactionManager: RestoreTransactionManager by lazy {
        RestoreTransactionManager(cardDatabase, keystoreManager)
    }

    override val backupService: BackupService by lazy {
        BackupService(backupRepository, backupHistoryRepository, internalAutoBackupManager)
    }

    override val restoreService: RestoreService by lazy {
        RestoreService(restoreTransactionManager, backupHistoryRepository)
    }

    override val backupFileGateway: BackupFileGateway by lazy {
        AndroidBackupFileGateway()
    }

    private val deviceService: DeviceService by lazy {
        DeviceService()
    }

    private val authService: AuthenticationService by lazy {
        AuthenticationService(keystoreManager)
    }

    private val syncService: SyncService by lazy {
        SyncService(
            attendanceDao = cardDatabase.attendanceDao(),
            configDao = cardDatabase.attendanceConfigDao(),
            logDao = cardDatabase.attendanceLogDao(),
            deviceService = deviceService,
            authService = authService
        )
    }

    override val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepository(
            cardDatabase.attendanceDao(),
            cardDatabase.attendanceConfigDao(),
            cardDatabase.attendanceLogDao(),
            deviceService,
            authService,
            syncService
        )
    }

    override val campusServerSyncService: CampusServerSyncService by lazy {
        CampusServerSyncService(
            cardRepository = cardRepository,
            configDao = cardDatabase.attendanceConfigDao(),
            logDao = cardDatabase.attendanceLogDao(),
            authService = authService,
            attendanceSyncService = syncService,
            roleAccessManager = roleAccessManager
        )
    }

    override val feedbackManager: FeedbackManager by lazy {
        FeedbackManager(context)
    }
}
