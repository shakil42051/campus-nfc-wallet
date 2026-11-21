package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    fun getCardById(id: Long): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE isFavorite = 1 AND isArchived = 0 ORDER BY timestamp DESC")
    fun getFavoriteCards(): Flow<List<CardEntity>>

    @Query("""
        SELECT * FROM cards 
        WHERE (nickname LIKE '%' || :query || '%' 
           OR uid LIKE '%' || :query || '%' 
           OR manufacturer LIKE '%' || :query || '%' 
           OR technology LIKE '%' || :query || '%')
          AND isArchived = 0
        ORDER BY timestamp DESC
    """)
    fun searchCards(query: String): Flow<List<CardEntity>>

    @Query("""
        SELECT * FROM cards 
        WHERE (nickname LIKE '%' || :query || '%' 
           OR uid LIKE '%' || :query || '%' 
           OR manufacturer LIKE '%' || :query || '%' 
           OR technology LIKE '%' || :query || '%')
          AND isArchived = 1
        ORDER BY timestamp DESC
    """)
    fun searchArchivedCards(query: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY timestamp DESC")
    suspend fun getAllCardsList(): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>): List<Long>

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    @Query("DELETE FROM cards")
    suspend fun clearAllCards()

    @Query("SELECT COUNT(*) FROM cards")
    fun getCardCount(): Flow<Int>
}
