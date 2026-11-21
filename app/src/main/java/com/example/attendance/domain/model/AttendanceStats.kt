package com.example.attendance.domain.model

data class AttendanceStats(
    val presentCount: Int = 0,
    val lateCount: Int = 0,
    val sickCount: Int = 0,
    val absentCount: Int = 0,
    val excusedCount: Int = 0,
    val totalDays: Int = 0,
    val attendancePercentage: Float = 0f,
    val syncPendingCount: Int = 0
)
