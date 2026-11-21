package com.example.data.repository

import com.example.data.local.dao.CardDao
import com.example.data.local.dao.UsageTransactionDao
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.UsageTransactionEntity
import com.example.data.model.NfcScanResult
import com.example.security.CryptoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CardRepository(
    private val cardDao: CardDao,
    private val transactionDao: UsageTransactionDao,
    private val cryptoProvider: CryptoProvider
) {
    val allCards: Flow<List<CardEntity>> = cardDao.getAllCards().map { list -> list.map(::decryptCard) }
    val archivedCards: Flow<List<CardEntity>> = cardDao.getArchivedCards().map { list -> list.map(::decryptCard) }
    val favoriteCards: Flow<List<CardEntity>> = cardDao.getFavoriteCards().map { list -> list.map(::decryptCard) }

    val allTransactions: Flow<List<UsageTransactionEntity>> = transactionDao.getAllTransactions()

    private fun decryptCard(entity: CardEntity): CardEntity {
        val cipherText = entity.encryptedData
        if (!cipherText.isNullOrBlank()) {
            val decrypted = runCatching { cryptoProvider.decrypt(cipherText) }.getOrNull()
            if (!decrypted.isNullOrBlank()) {
                return entity.copy(rawDataJson = decrypted)
            }
        }
        // Fallback for pre-encryption legacy rows where rawDataJson still holds JSON
        return entity
    }

    fun encryptCardForStorage(card: CardEntity): CardEntity {
        val plainJson = card.rawDataJson
        val encrypted = if (plainJson.isNotBlank()) {
            cryptoProvider.encrypt(plainJson)
        } else {
            card.encryptedData
        }
        return card.copy(
            rawDataJson = "",
            encryptedData = encrypted
        )
    }

    fun getTransactionsForCard(cardId: Long): Flow<List<UsageTransactionEntity>> {
        return transactionDao.getTransactionsForCard(cardId)
    }

    fun searchTransactions(query: String): Flow<List<UsageTransactionEntity>> {
        return transactionDao.searchTransactions(query)
    }

    suspend fun logTransaction(
        cardId: Long,
        cardNickname: String,
        cardUid: String,
        usageType: String,
        purposeOrAction: String,
        status: String = "SUCCESS",
        terminalLocation: String = "NFC Terminal"
    ): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(
            UsageTransactionEntity(
                cardId = cardId,
                cardNickname = cardNickname,
                cardUid = cardUid,
                timestamp = System.currentTimeMillis(),
                usageType = usageType,
                purposeOrAction = purposeOrAction,
                status = status,
                terminalLocation = terminalLocation
            )
        )
    }

    suspend fun restoreTransaction(transaction: UsageTransactionEntity): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(id)
    }

    suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
        transactionDao.clearAllTransactions()
    }

    fun searchCards(query: String): Flow<List<CardEntity>> {
        return cardDao.searchCards(query).map { list -> list.map(::decryptCard) }
    }

    fun searchArchivedCards(query: String): Flow<List<CardEntity>> {
        return cardDao.searchArchivedCards(query).map { list -> list.map(::decryptCard) }
    }

    fun getCardById(id: Long): Flow<CardEntity?> {
        return cardDao.getCardById(id).map { it?.let(::decryptCard) }
    }

    suspend fun saveCardFromScan(
        scanResult: NfcScanResult,
        nickname: String = "Campus Card",
        notes: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val jsonPayload = scanResult.toJsonString()
        val encryptedPayload = cryptoProvider.encrypt(jsonPayload)

        val entity = CardEntity(
            uid = scanResult.uid,
            nickname = nickname.ifBlank { "Campus Badge (${scanResult.uid.takeLast(5)})" },
            manufacturer = scanResult.manufacturer,
            technology = scanResult.primaryTechnology,
            cardStandard = scanResult.cardStandard,
            timestamp = System.currentTimeMillis(),
            isFavorite = false,
            notes = notes,
            rawDataJson = "",
            encryptedData = encryptedPayload
        )

        cardDao.insertCard(entity)
    }

    suspend fun updateSavedCard(
        existingCard: CardEntity,
        scanResult: NfcScanResult,
        nickname: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val jsonPayload = scanResult.toJsonString()
        val encryptedPayload = cryptoProvider.encrypt(jsonPayload)

        val updatedEntity = existingCard.copy(
            uid = scanResult.uid,
            nickname = nickname.ifBlank { "Campus Badge (${scanResult.uid.takeLast(5)})" },
            manufacturer = scanResult.manufacturer,
            technology = scanResult.primaryTechnology,
            cardStandard = scanResult.cardStandard,
            notes = notes,
            rawDataJson = "",
            encryptedData = encryptedPayload
        )
        cardDao.updateCard(updatedEntity)
    }

    suspend fun updateCard(card: CardEntity) = withContext(Dispatchers.IO) {
        cardDao.updateCard(encryptCardForStorage(card))
    }

    suspend fun toggleFavorite(card: CardEntity) = withContext(Dispatchers.IO) {
        val updated = encryptCardForStorage(card.copy(isFavorite = !card.isFavorite))
        cardDao.updateCard(updated)
    }

    suspend fun toggleArchive(card: CardEntity) = withContext(Dispatchers.IO) {
        val updated = encryptCardForStorage(card.copy(isArchived = !card.isArchived))
        cardDao.updateCard(updated)
    }

    suspend fun deleteCard(card: CardEntity) = withContext(Dispatchers.IO) {
        cardDao.deleteCardById(card.id)
    }

    suspend fun deleteCardById(id: Long) = withContext(Dispatchers.IO) {
        cardDao.deleteCardById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        cardDao.clearAllCards()
    }

    suspend fun getAllCardsList(): List<CardEntity> = withContext(Dispatchers.IO) {
        cardDao.getAllCardsList().map(::decryptCard)
    }

    suspend fun getAllTransactionsList(): List<UsageTransactionEntity> = withContext(Dispatchers.IO) {
        transactionDao.getAllTransactionsList()
    }

    suspend fun restoreCards(cards: List<CardEntity>): Int = withContext(Dispatchers.IO) {
        if (cards.isEmpty()) return@withContext 0
        val encrypted = cards.map(::encryptCardForStorage)
        val ids = cardDao.insertCards(encrypted)
        ids.size
    }

    fun getCardCount(): Flow<Int> = cardDao.getCardCount()
}
