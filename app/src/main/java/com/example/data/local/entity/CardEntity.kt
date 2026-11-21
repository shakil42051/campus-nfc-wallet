package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cards",
    indices = [Index(value = ["uid"])]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uid: String,
    val nickname: String,
    val manufacturer: String,
    val technology: String,
    val cardStandard: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val notes: String = "",
    val rawDataJson: String,
    val encryptedData: String? = null
)
