package com.example.attendance.domain.model

data class SyncResult(
    val isSuccess: Boolean,
    val syncedCount: Int,
    val failedCount: Int,
    val message: String,
    val latencyMs: Long = 0L,
    val provider: String = "JKTECO"
)
