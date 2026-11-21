package com.example.backup.model

import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.attendance.data.entity.AttendanceEntity
import com.example.attendance.data.entity.AttendanceLogEntity
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.UsageTransactionEntity

/**
 * Standard envelope format for encrypted Card Vault backups (.cardvaultbackup).
 */
data class BackupEnvelope(
    val formatIdentifier: String = "CARD_VAULT_BACKUP",
    val schemaVersion: Int = 2,
    val appVersion: String = "1.0.0",
    val createdAt: Long = System.currentTimeMillis(),
    val recordCounts: BackupRecordCounts,
    val encryption: EncryptionMetadata,
    val ciphertext: String,
    val authenticationTag: String
)

data class BackupRecordCounts(
    val cards: Int = 0,
    val transactions: Int = 0,
    val attendance: Int = 0,
    val configs: Int = 0,
    val logs: Int = 0
)

data class EncryptionMetadata(
    val algorithm: String = "AES-256-GCM",
    val keyDerivation: String = "PBKDF2-HMAC-SHA-256",
    val iterations: Int = 210_000,
    val salt: String, // Base64
    val nonce: String  // Base64
)

data class BackupPayload(
    val cards: List<CardEntity> = emptyList(),
    val transactions: List<UsageTransactionEntity> = emptyList(),
    val attendanceRecords: List<AttendanceEntity> = emptyList(),
    val attendanceConfigs: List<AttendanceConfigEntity> = emptyList(),
    val attendanceLogs: List<AttendanceLogEntity> = emptyList(),
    val settingsMetadata: Map<String, String> = emptyMap()
)

enum class RestoreMode {
    MERGE,
    REPLACE
}

data class RestorePreview(
    val createdAt: Long,
    val schemaVersion: Int,
    val appVersion: String,
    val fileSizeBytes: Long,
    val recordCounts: BackupRecordCounts,
    val payload: BackupPayload
)

data class RestoreResult(
    val restoredCardsCount: Int,
    val restoredTransactionsCount: Int,
    val restoredAttendanceCount: Int,
    val restoredConfigsCount: Int,
    val restoredLogsCount: Int,
    val isSuccess: Boolean = true,
    val message: String = "Restore completed successfully"
)

enum class BackupOperationType {
    CREATE_BACKUP,
    RESTORE,
    AUTO_BACKUP
}

enum class BackupOperationStatus {
    SUCCESS,
    FAILED,
    CANCELLED
}

data class BackupHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val cardsCount: Int = 0,
    val transactionsCount: Int = 0,
    val attendanceCount: Int = 0,
    val operationType: BackupOperationType,
    val status: BackupOperationStatus,
    val errorMessage: String? = null
)

sealed class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidFormat(details: String = "") : BackupException("This is not a supported Card Vault backup file. $details")
    class UnsupportedVersion(version: Int) : BackupException("This backup was created by an unsupported app version (schema v$version).")
    class IncorrectPassword : BackupException("The backup password is incorrect.")
    class CorruptedPayload(cause: Throwable? = null) : BackupException("This backup is corrupted or has been modified.", cause)
    class ReadFailed(reason: String, cause: Throwable? = null) : BackupException("The selected backup file could not be read: $reason", cause)
    class WriteFailed(reason: String, cause: Throwable? = null) : BackupException("The backup could not be saved: $reason", cause)
    class RestoreFailed(reason: String, cause: Throwable? = null) : BackupException("The restore failed. Your existing data was not changed: $reason", cause)
}
