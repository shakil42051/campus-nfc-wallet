package com.example.attendance.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_logs")
data class AttendanceLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val logType: String = "ACTIVITY", // ACTIVITY, SYNC, CONNECTION, ERROR
    val provider: String = "JKTECO",
    val message: String,
    val details: String = "",
    val statusCode: Int = 200,
    val isSuccess: Boolean = true
)
