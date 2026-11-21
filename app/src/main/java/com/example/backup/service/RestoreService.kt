package com.example.backup.service

import com.example.backup.crypto.BackupCryptoService
import com.example.backup.migration.BackupMigrationManager
import com.example.backup.model.BackupHistoryItem
import com.example.backup.model.BackupOperationStatus
import com.example.backup.model.BackupOperationType
import com.example.backup.model.BackupRecordCounts
import com.example.backup.model.RestoreMode
import com.example.backup.model.RestorePreview
import com.example.backup.model.RestoreResult
import com.example.backup.repository.BackupHistoryRepository
import com.example.backup.serializer.BackupSerializer
import com.example.backup.transaction.RestoreTransactionManager
import com.example.backup.validator.BackupValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RestoreService(
    private val restoreTransactionManager: RestoreTransactionManager,
    private val backupHistoryRepository: BackupHistoryRepository
) {

    suspend fun parseAndDecryptBackup(
        fileBytes: ByteArray,
        password: String,
        fileSizeBytes: Long
    ): RestorePreview = withContext(Dispatchers.Default) {
        val jsonEnvelopeStr = String(fileBytes, Charsets.UTF_8)

        // 1. Parse Envelope
        val envelope = BackupSerializer.deserializeEnvelope(jsonEnvelopeStr)

        // 2. Validate Envelope & Crypto Metadata Bounds
        BackupValidator.validateEnvelope(envelope)

        // 3. Check Schema Compatibility
        BackupMigrationManager.checkSchemaCompatibility(envelope.schemaVersion)

        // 4. Decrypt Payload (v2 authenticates envelope header via GCM AAD; v1 remains backward compatible)
        val aad = if (envelope.schemaVersion >= 2) {
            BackupCryptoService.buildHeaderAad(
                formatIdentifier = envelope.formatIdentifier,
                schemaVersion = envelope.schemaVersion,
                createdAt = envelope.createdAt,
                algorithm = envelope.encryption.algorithm,
                keyDerivation = envelope.encryption.keyDerivation,
                iterations = envelope.encryption.iterations
            )
        } else {
            null
        }

        val payloadJson = BackupCryptoService.decrypt(
            ciphertextBase64 = envelope.ciphertext,
            authTagBase64 = envelope.authenticationTag,
            password = password,
            metadata = envelope.encryption,
            aad = aad
        )

        // 5. Schema Migration if needed
        val migratedPayloadJson = BackupMigrationManager.migrateIfNeeded(payloadJson, envelope.schemaVersion)

        // 6. Deserialize Payload
        val payload = BackupSerializer.deserializePayload(migratedPayloadJson)

        // 7. Validate Payload
        BackupValidator.validatePayload(payload)

        // 8. Compute verified record counts directly from the decrypted payload (never trust plaintext header counts)
        val verifiedCounts = BackupRecordCounts(
            cards = payload.cards.size,
            transactions = payload.transactions.size,
            attendance = payload.attendanceRecords.size,
            configs = payload.attendanceConfigs.size,
            logs = payload.attendanceLogs.size
        )

        RestorePreview(
            createdAt = envelope.createdAt,
            schemaVersion = envelope.schemaVersion,
            appVersion = envelope.appVersion,
            fileSizeBytes = fileSizeBytes,
            recordCounts = verifiedCounts,
            payload = payload
        )
    }

    suspend fun executeRestore(
        preview: RestorePreview,
        mode: RestoreMode,
        selectedCategories: Set<String>,
        fileName: String
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val result = restoreTransactionManager.executeRestore(
                payload = preview.payload,
                mode = mode,
                selectedCategories = selectedCategories
            )

            backupHistoryRepository.addHistoryItem(
                BackupHistoryItem(
                    fileName = fileName,
                    timestamp = System.currentTimeMillis(),
                    fileSizeBytes = preview.fileSizeBytes,
                    cardsCount = result.restoredCardsCount,
                    transactionsCount = result.restoredTransactionsCount,
                    attendanceCount = result.restoredAttendanceCount,
                    operationType = BackupOperationType.RESTORE,
                    status = BackupOperationStatus.SUCCESS
                )
            )

            result
        } catch (e: Exception) {
            backupHistoryRepository.addHistoryItem(
                BackupHistoryItem(
                    fileName = fileName,
                    timestamp = System.currentTimeMillis(),
                    fileSizeBytes = preview.fileSizeBytes,
                    operationType = BackupOperationType.RESTORE,
                    status = BackupOperationStatus.FAILED,
                    errorMessage = e.localizedMessage
                )
            )
            throw e
        }
    }
}
