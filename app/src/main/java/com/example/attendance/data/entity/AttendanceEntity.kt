package com.example.attendance.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: String,
    val studentName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val usageType: String = "PUNCH_IN", // PUNCH_IN, PUNCH_OUT, NFC_TAP, GATE_ACCESS
    val status: String = "PRESENT", // PRESENT, LATE, ABSENT, EXCUSED
    val providerType: String = "JKTECO", // JKTECO, SUPREMA, HID, GALLAGHER, CUSTOM
    val syncStatus: String = "SYNCED", // SYNCED, PENDING, FAILED
    val location: String = "Main Gate #1",
    val deviceId: String = "JKT-NEO9-01",
    val campusId: String = "CAMPUS-MAIN",
    val cardUid: String = "04:A2:8B:1F:90",
    val cardNickname: String = "Student ID Badge",
    val rawPayload: String = "",
    val retryCount: Int = 0,
    val lastSyncError: String? = null
)
