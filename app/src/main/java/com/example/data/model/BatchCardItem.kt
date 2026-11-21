package com.example.data.model

import java.util.UUID

data class BatchCardItem(
    val id: String = UUID.randomUUID().toString(),
    val scanResult: NfcScanResult,
    val nickname: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
