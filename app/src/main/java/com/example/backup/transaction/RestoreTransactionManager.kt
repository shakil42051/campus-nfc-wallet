package com.example.backup.transaction

import androidx.room.withTransaction
import com.example.backup.model.BackupException
import com.example.backup.model.BackupPayload
import com.example.backup.model.RestoreMode
import com.example.backup.model.RestoreResult
import com.example.data.local.database.CardDatabase
import com.example.security.CryptoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class RestoreTransactionManager(
    private val cardDatabase: CardDatabase,
    private val cryptoProvider: CryptoProvider
) {

    suspend fun executeRestore(
        payload: BackupPayload,
        mode: RestoreMode,
        selectedCategories: Set<String> = setOf("cards", "transactions", "attendance")
    ): RestoreResult = withContext(Dispatchers.IO) {
        val restoreCards = selectedCategories.contains("cards")
        val restoreTx = selectedCategories.contains("transactions")
        val restoreAtt = selectedCategories.contains("attendance")

        var cardsRestoredCount = 0
        var txRestoredCount = 0
        var attRestoredCount = 0
        var cfgRestoredCount = 0
        var logsRestoredCount = 0

        try {
            cardDatabase.withTransaction {
                val cardDao = cardDatabase.cardDao()
                val txDao = cardDatabase.transactionDao()
                val attDao = cardDatabase.attendanceDao()
                val cfgDao = cardDatabase.attendanceConfigDao()
                val logDao = cardDatabase.attendanceLogDao()

                if (mode == RestoreMode.REPLACE) {
                    if (restoreCards) cardDao.clearAllCards()
                    if (restoreTx) txDao.clearAllTransactions()
                    if (restoreAtt) {
                        attDao.clearAllRecords()
                        logDao.clearAllLogs()
                        cfgDao.clearConfig()
                    }
                }

                // Track old cardId -> new cardId and normalized UID -> new cardId for transaction remapping
                val oldCardIdToNewId = mutableMapOf<Long, Long>()
                val uidToNewCardId = mutableMapOf<String, Long>()

                // Pre-populate existing cards map so transactions in MERGE mode or card-excluded restores still link cleanly
                cardDao.getAllCardsList().forEach { existing ->
                    uidToNewCardId[existing.uid.uppercase(Locale.US).trim()] = existing.id
                }

                if (restoreCards && payload.cards.isNotEmpty()) {
                    for (card in payload.cards) {
                        val normalizedUid = card.uid.uppercase(Locale.US).trim()
                        if (mode == RestoreMode.MERGE && uidToNewCardId.containsKey(normalizedUid)) {
                            // Map old backup cardId to already-existing local cardId
                            oldCardIdToNewId[card.id] = uidToNewCardId[normalizedUid]!!
                            continue
                        }

                        val plainJson = card.rawDataJson
                        val reencrypted = if (plainJson.isNotBlank()) {
                            cryptoProvider.encrypt(plainJson)
                        } else {
                            null
                        }
                        val insertedId = cardDao.insertCard(
                            card.copy(
                                id = 0,
                                rawDataJson = "",
                                encryptedData = reencrypted
                            )
                        )
                        oldCardIdToNewId[card.id] = insertedId
                        uidToNewCardId[normalizedUid] = insertedId
                        cardsRestoredCount++
                    }
                }

                if (restoreTx && payload.transactions.isNotEmpty()) {
                    val existingTxKeys = if (mode == RestoreMode.MERGE) {
                        txDao.getAllTransactionsList().map { tx ->
                            "${tx.cardUid.uppercase(Locale.US).trim()}|${tx.timestamp}|${tx.usageType}|${tx.purposeOrAction}"
                        }.toHashSet()
                    } else {
                        HashSet()
                    }

                    val remappedTxs = payload.transactions.mapNotNull { tx ->
                        val dedupKey = "${tx.cardUid.uppercase(Locale.US).trim()}|${tx.timestamp}|${tx.usageType}|${tx.purposeOrAction}"
                        if (mode == RestoreMode.MERGE && !existingTxKeys.add(dedupKey)) {
                            return@mapNotNull null
                        }
                        val resolvedCardId = oldCardIdToNewId[tx.cardId]
                            ?: uidToNewCardId[tx.cardUid.uppercase(Locale.US).trim()]
                            ?: tx.cardId
                        tx.copy(id = 0, cardId = resolvedCardId)
                    }

                    if (remappedTxs.isNotEmpty()) {
                        txDao.insertTransactions(remappedTxs)
                        txRestoredCount = remappedTxs.size
                    }
                }

                if (restoreAtt) {
                    if (payload.attendanceRecords.isNotEmpty()) {
                        val existingAttKeys = if (mode == RestoreMode.MERGE) {
                            attDao.getAllRecordsList().map { a ->
                                "${a.studentId}|${a.timestamp}|${a.usageType}|${a.location}"
                            }.toHashSet()
                        } else {
                            HashSet()
                        }

                        val newAtts = payload.attendanceRecords.mapNotNull { a ->
                            val key = "${a.studentId}|${a.timestamp}|${a.usageType}|${a.location}"
                            if (mode == RestoreMode.MERGE && !existingAttKeys.add(key)) {
                                null
                            } else {
                                a.copy(id = 0)
                            }
                        }

                        if (newAtts.isNotEmpty()) {
                            attDao.insertRecords(newAtts)
                            attRestoredCount = newAtts.size
                        }
                    }

                    if (payload.attendanceConfigs.isNotEmpty()) {
                        val cfg = payload.attendanceConfigs.first()
                        val securedCfg = cfg.copy(
                            id = 1,
                            apiKey = if (cfg.apiKey.isNotBlank()) cryptoProvider.encrypt(cfg.apiKey) else "",
                            authToken = if (cfg.authToken.isNotBlank()) cryptoProvider.encrypt(cfg.authToken) else ""
                        )
                        cfgDao.saveConfig(securedCfg)
                        cfgRestoredCount = 1
                    }

                    if (payload.attendanceLogs.isNotEmpty()) {
                        val existingLogKeys = if (mode == RestoreMode.MERGE) {
                            logDao.getAllLogsList().map { l ->
                                "${l.timestamp}|${l.logType}|${l.message}"
                            }.toHashSet()
                        } else {
                            HashSet()
                        }

                        val newLogs = payload.attendanceLogs.mapNotNull { l ->
                            val key = "${l.timestamp}|${l.logType}|${l.message}"
                            if (mode == RestoreMode.MERGE && !existingLogKeys.add(key)) {
                                null
                            } else {
                                l.copy(id = 0)
                            }
                        }

                        if (newLogs.isNotEmpty()) {
                            logDao.insertLogs(newLogs)
                            logsRestoredCount = newLogs.size
                        }
                    }
                }
            }

            RestoreResult(
                restoredCardsCount = cardsRestoredCount,
                restoredTransactionsCount = txRestoredCount,
                restoredAttendanceCount = attRestoredCount,
                restoredConfigsCount = cfgRestoredCount,
                restoredLogsCount = logsRestoredCount,
                isSuccess = true,
                message = "Successfully restored $cardsRestoredCount cards, $txRestoredCount transactions, and $attRestoredCount attendance records."
            )
        } catch (e: Exception) {
            throw BackupException.RestoreFailed("Database transaction failed and was completely rolled back: ${e.localizedMessage}", e)
        }
    }
}
