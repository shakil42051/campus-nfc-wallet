package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_transactions",
    indices = [Index(value = ["cardId"])]
)
data class UsageTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val cardNickname: String,
    val cardUid: String,
    val timestamp: Long = System.currentTimeMillis(),
    val usageType: String,
    val purposeOrAction: String,
    val status: String = "SUCCESS",
    val terminalLocation: String = "NFC Terminal"
)
