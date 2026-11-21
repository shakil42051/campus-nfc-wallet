package com.example.backup.service

import android.content.Context
import com.example.backup.model.BackupHistoryItem
import com.example.backup.model.BackupOperationStatus
import com.example.backup.model.BackupOperationType
import com.example.backup.model.BackupRecordCounts
import com.example.backup.repository.BackupHistoryRepository
import java.io.File

class InternalAutoBackupManager(
    private val context: Context,
    private val backupHistoryRepository: BackupHistoryRepository
) {

    private val autoBackupDir: File
        get() = File(context.filesDir, "backups/auto").apply {
            if (!exists()) {
                mkdirs()
            }
        }

    private val userSnapshotsDir: File
        get() = File(context.filesDir, "backups/snapshots").apply {
            if (!exists()) {
                mkdirs()
            }
        }

    private fun resolveSafeChild(dir: File, fileName: String): File? {
        if (fileName.isBlank() || fileName.contains('/') || fileName.contains('\\') || fileName.contains("..")) {
            return null
        }
        val candidate = File(dir, fileName)
        return try {
            val basePath = dir.canonicalPath + File.separator
            if (candidate.canonicalPath.startsWith(basePath)) candidate else null
        } catch (e: Exception) {
            null
        }
    }

    fun saveAutoBackup(encryptedBytes: ByteArray, counts: BackupRecordCounts = BackupRecordCounts()): File? {
        return try {
            val fileName = "CardVault_AutoBackup_${System.currentTimeMillis()}.cardvaultbackup"
            val file = File(autoBackupDir, fileName)
            file.writeBytes(encryptedBytes)

            rotateOldAutoBackups()

            backupHistoryRepository.addHistoryItem(
                BackupHistoryItem(
                    fileName = fileName,
                    timestamp = System.currentTimeMillis(),
                    fileSizeBytes = encryptedBytes.size.toLong(),
                    cardsCount = counts.cards,
                    transactionsCount = counts.transactions,
                    attendanceCount = counts.attendance,
                    operationType = BackupOperationType.AUTO_BACKUP,
                    status = BackupOperationStatus.SUCCESS
                )
            )

            file
        } catch (e: Exception) {
            null
        }
    }

    fun saveUserSnapshot(
        label: String,
        encryptedBytes: ByteArray,
        counts: BackupRecordCounts = BackupRecordCounts()
    ): File? {
        return try {
            val cleanLabel = label.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32).ifBlank { "Snapshot" }
            val fileName = "Snapshot_${cleanLabel}_${System.currentTimeMillis()}.cardvaultbackup"
            val file = File(userSnapshotsDir, fileName)
            file.writeBytes(encryptedBytes)

            backupHistoryRepository.addHistoryItem(
                BackupHistoryItem(
                    fileName = fileName,
                    timestamp = System.currentTimeMillis(),
                    fileSizeBytes = encryptedBytes.size.toLong(),
                    cardsCount = counts.cards,
                    transactionsCount = counts.transactions,
                    attendanceCount = counts.attendance,
                    operationType = BackupOperationType.CREATE_BACKUP,
                    status = BackupOperationStatus.SUCCESS
                )
            )

            file
        } catch (e: Exception) {
            null
        }
    }

    fun getAutoBackupsList(): List<File> {
        val files = autoBackupDir.listFiles { _, name -> name.endsWith(".cardvaultbackup") }
        return files?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun getUserSnapshotsList(): List<File> {
        val files = userSnapshotsDir.listFiles { _, name -> name.endsWith(".cardvaultbackup") }
        return files?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteSnapshot(fileName: String): Boolean {
        val userFile = resolveSafeChild(userSnapshotsDir, fileName)
        if (userFile != null && userFile.exists()) {
            return userFile.delete()
        }
        val autoFile = resolveSafeChild(autoBackupDir, fileName)
        if (autoFile != null && autoFile.exists()) {
            return autoFile.delete()
        }
        return false
    }

    fun readSnapshotBytes(fileName: String): ByteArray? {
        val userFile = resolveSafeChild(userSnapshotsDir, fileName)
        if (userFile != null && userFile.exists()) {
            return userFile.readBytes()
        }
        val autoFile = resolveSafeChild(autoBackupDir, fileName)
        if (autoFile != null && autoFile.exists()) {
            return autoFile.readBytes()
        }
        return null
    }

    private fun rotateOldAutoBackups() {
        val list = getAutoBackupsList()
        if (list.size > 8) {
            list.drop(8).forEach { oldFile ->
                oldFile.delete()
            }
        }
    }
}
