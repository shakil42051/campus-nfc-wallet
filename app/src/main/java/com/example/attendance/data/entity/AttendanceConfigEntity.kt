package com.example.attendance.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_config")
data class AttendanceConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val serverUrl: String = "",
    val port: Int = 443,
    val useHttps: Boolean = true,
    val authType: String = "API_KEY", // API_KEY, JWT, OAUTH2, SESSION
    val apiKey: String = "",
    val authToken: String = "",
    val tokenExpiryTimestamp: Long = 0L,
    val username: String = "",
    val organizationId: String = "",
    val campusId: String = "",
    val deviceId: String = "",
    val providerType: String = "JKTECO", // JKTECO, SUPREMA, HID, GALLAGHER, CUSTOM
    val autoSyncEnabled: Boolean = false,
    val syncIntervalMinutes: Int = 15,
    val userRole: String = "STUDENT", // STUDENT, TEACHER, ADMIN
    val activeStudentId: String = "",
    val activeStudentName: String = "",
    val department: String = "",
    val email: String = "",
    val linkedCardUid: String = "",
    val linkedCardNickname: String = "",
    val isConnected: Boolean = false,
    val lastTestTimestamp: Long = 0L,
    val lastSyncTimestamp: Long = 0L
)
