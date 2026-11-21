package com.example.backup.service

import com.example.BuildConfig
import com.example.backup.crypto.BackupCryptoService
import com.example.backup.migration.BackupMigrationManager
import com.example.backup.model.BackupEnvelope
import com.example.backup.model.BackupHistoryItem
import com.example.backup.model.BackupOperationStatus
import com.example.backup.model.BackupOperationType
import com.example.backup.model.BackupRecordCounts
import com.example.backup.repository.BackupHistoryRepository
import com.example.backup.repository.BackupRepository
import com.example.backup.serializer.BackupSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PreparedBackupArtifact(
    val bytes: ByteArray,
    val fileName: String,
    val createdAt: Long,
    val recordCounts: BackupRecordCounts
)

class BackupService(
    private val backupRepository: BackupRepository,
    private val backupHistoryRepository: BackupHistoryRepository,
    private val internalAutoBackupManager: InternalAutoBackupManager
) {

    private suspend fun buildEncryptedArtifact(
        password: String,
        fileName: String
    ): PreparedBackupArtifact = withContext(Dispatchers.Default) {
        val payload = backupRepository.getFullPayload()

        val recordCounts = BackupRecordCounts(
            cards = payload.cards.size,
            transactions = payload.transactions.size,
            attendance = payload.attendanceRecords.size,
            configs = payload.attendanceConfigs.size,
            logs = payload.attendanceLogs.size
        )

        val createdAt = System.currentTimeMillis()
        val schemaVersion = BackupMigrationManager.CURRENT_SCHEMA_VERSION
        val aad = BackupCryptoService.buildHeaderAad(
            formatIdentifier = "CARD_VAULT_BACKUP",
            schemaVersion = schemaVersion,
            createdAt = createdAt,
            algorithm = BackupCryptoService.ALGORITHM_NAME,
            keyDerivation = BackupCryptoService.KEY_DERIVATION_NAME,
            iterations = BackupCryptoService.ITERATION_COUNT
        )

        val payloadJson = BackupSerializer.serializePayload(payload)
        val encryptedBundle = BackupCryptoService.encrypt(payloadJson, password, aad)

        val envelope = BackupEnvelope(
            formatIdentifier = "CARD_VAULT_BACKUP",
            schemaVersion = schemaVersion,
            appVersion = BuildConfig.VERSION_NAME,
            createdAt = createdAt,
            recordCounts = recordCounts,
            encryption = encryptedBundle.metadata,
            ciphertext = encryptedBundle.ciphertextBase64,
            authenticationTag = encryptedBundle.authTagBase64
        )

        val envelopeJson = BackupSerializer.serializeEnvelope(envelope)
        val backupBytes = envelopeJson.toByteArray(Charsets.UTF_8)

        PreparedBackupArtifact(
            bytes = backupBytes,
            fileName = fileName,
            createdAt = createdAt,
            recordCounts = recordCounts
        )
    }

    suspend fun createEncryptedBackupBytes(password: String, fileName: String): PreparedBackupArtifact {
        return buildEncryptedArtifact(password, fileName)
    }

    suspend fun recordExternalBackupCompleted(
        artifact: PreparedBackupArtifact,
        saveInternalAutoCopy: Boolean = true
    ) = withContext(Dispatchers.IO) {
        if (saveInternalAutoCopy) {
            internalAutoBackupManager.saveAutoBackup(artifact.bytes, artifact.recordCounts)
        }
        backupHistoryRepository.addHistoryItem(
            BackupHistoryItem(
                fileName = artifact.fileName,
                timestamp = artifact.createdAt,
                fileSizeBytes = artifact.bytes.size.toLong(),
                cardsCount = artifact.recordCounts.cards,
                transactionsCount = artifact.recordCounts.transactions,
                attendanceCount = artifact.recordCounts.attendance,
                operationType = BackupOperationType.CREATE_BACKUP,
                status = BackupOperationStatus.SUCCESS
            )
        )
    }

    suspend fun createInternalSnapshot(password: String, label: String): ByteArray {
        val artifact = buildEncryptedArtifact(password, label)
        withContext(Dispatchers.IO) {
            internalAutoBackupManager.saveUserSnapshot(label, artifact.bytes, artifact.recordCounts)
        }
        return artifact.bytes
    }

    suspend fun getInternalSnapshots(): List<java.io.File> = withContext(Dispatchers.IO) {
        val userList = internalAutoBackupManager.getUserSnapshotsList()
        val autoList = internalAutoBackupManager.getAutoBackupsList()
        (userList + autoList).distinctBy { it.name }.sortedByDescending { it.lastModified() }
    }

    suspend fun deleteSnapshot(fileName: String): Boolean = withContext(Dispatchers.IO) {
        internalAutoBackupManager.deleteSnapshot(fileName)
    }

    suspend fun readSnapshotBytes(fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        internalAutoBackupManager.readSnapshotBytes(fileName)
    }
}
