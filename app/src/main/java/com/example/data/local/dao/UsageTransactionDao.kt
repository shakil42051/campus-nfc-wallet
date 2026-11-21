package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UsageTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageTransactionDao {
    @Query("SELECT * FROM usage_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<UsageTransactionEntity>>

    @Query("SELECT * FROM usage_transactions WHERE cardId = :cardId ORDER BY timestamp DESC")
    fun getTransactionsForCard(cardId: Long): Flow<List<UsageTransactionEntity>>

    @Query("""
        SELECT * FROM usage_transactions 
        WHERE cardNickname LIKE '%' || :query || '%'
           OR cardUid LIKE '%' || :query || '%'
           OR usageType LIKE '%' || :query || '%'
           OR purposeOrAction LIKE '%' || :query || '%'
           OR terminalLocation LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchTransactions(query: String): Flow<List<UsageTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: UsageTransactionEntity): Long

    @Query("SELECT * FROM usage_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<UsageTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<UsageTransactionEntity>): List<Long>

    @Query("DELETE FROM usage_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM usage_transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT COUNT(*) FROM usage_transactions")
    suspend fun getTransactionCount(): Int
}
