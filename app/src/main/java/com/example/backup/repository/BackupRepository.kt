package com.example.backup.repository

import com.example.attendance.data.dao.AttendanceConfigDao
import com.example.attendance.data.dao.AttendanceDao
import com.example.attendance.data.dao.AttendanceLogDao
import com.example.backup.model.BackupPayload
import com.example.data.local.dao.CardDao
import com.example.data.local.dao.UsageTransactionDao
import com.example.security.CryptoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupRepository(
    private val cardDao: CardDao,
    private val transactionDao: UsageTransactionDao,
    private val attendanceDao: AttendanceDao,
    private val attendanceConfigDao: AttendanceConfigDao,
    private val attendanceLogDao: AttendanceLogDao,
    private val cryptoProvider: CryptoProvider
) {

    suspend fun getFullPayload(): BackupPayload = withContext(Dispatchers.IO) {
        // Decrypt device-bound Keystore ciphertext so backups are portable across devices
        // (the entire backup payload is encrypted with the user's password via AES-256-GCM).
        val cards = cardDao.getAllCardsList().map { card ->
            val plainRaw = if (!card.encryptedData.isNullOrBlank()) {
                runCatching { cryptoProvider.decrypt(card.encryptedData) }.getOrElse { card.rawDataJson }
            } else {
                card.rawDataJson
            }
            card.copy(rawDataJson = plainRaw, encryptedData = null)
        }

        val txs = transactionDao.getAllTransactionsList()
        val records = attendanceDao.getAllRecordsList()
        val rawCfg = attendanceConfigDao.getConfig()
        val portableCfg = rawCfg?.let { cfg ->
            cfg.copy(
                apiKey = if (cfg.apiKey.isNotBlank()) runCatching { cryptoProvider.decrypt(cfg.apiKey) }.getOrDefault(cfg.apiKey) else "",
                authToken = if (cfg.authToken.isNotBlank()) runCatching { cryptoProvider.decrypt(cfg.authToken) }.getOrDefault(cfg.authToken) else ""
            )
        }
        val logs = attendanceLogDao.getAllLogsList()

        BackupPayload(
            cards = cards,
            transactions = txs,
            attendanceRecords = records,
            attendanceConfigs = if (portableCfg != null) listOf(portableCfg) else emptyList(),
            attendanceLogs = logs,
            settingsMetadata = mapOf(
                "exported_at" to System.currentTimeMillis().toString(),
                "cards_count" to cards.size.toString(),
                "transactions_count" to txs.size.toString()
            )
        )
    }
}
