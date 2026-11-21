package com.example.attendance.repository

import com.example.attendance.data.dao.AttendanceConfigDao
import com.example.attendance.data.dao.AttendanceDao
import com.example.attendance.data.dao.AttendanceLogDao
import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.attendance.data.entity.AttendanceEntity
import com.example.attendance.data.entity.AttendanceLogEntity
import com.example.attendance.domain.model.AttendanceStats
import com.example.attendance.domain.model.SyncResult
import com.example.attendance.service.AuthenticationService
import com.example.attendance.service.DeviceService
import com.example.attendance.service.SyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val configDao: AttendanceConfigDao,
    private val logDao: AttendanceLogDao,
    private val deviceService: DeviceService,
    private val authService: AuthenticationService,
    private val syncService: SyncService
) {

    val configFlow: Flow<AttendanceConfigEntity> = configDao.getConfigFlow()
        .map { saved ->
            if (saved != null) authService.decryptConfigSecrets(saved) else AttendanceConfigEntity()
        }

    val allRecordsFlow: Flow<List<AttendanceEntity>> = attendanceDao.getAllRecords()

    val allLogsFlow: Flow<List<AttendanceLogEntity>> = logDao.getAllLogs()

    val attendanceStatsFlow: Flow<AttendanceStats> = combine(
        allRecordsFlow,
        configFlow
    ) { records, config ->
        val scopedRecords = if (config.activeStudentId.isNotBlank()) {
            records.filter { it.studentId == config.activeStudentId }
        } else {
            records
        }
        val present = scopedRecords.count { it.status == "PRESENT" || it.status == "ON_TIME" }
        val late = scopedRecords.count { it.status == "LATE" }
        val sick = scopedRecords.count { it.status == "SICK" }
        val absent = scopedRecords.count { it.status == "ABSENT" }
        val excused = scopedRecords.count { it.status == "EXCUSED" || it.status == "LEAVE" }
        val total = present + late + sick + absent + excused
        val pending = scopedRecords.count { it.syncStatus == "PENDING" || it.syncStatus == "FAILED" }
        val pct = if (total == 0) {
            0f
        } else {
            (((present + excused + sick) + late * 0.7f) / total.toFloat()) * 100f
        }

        AttendanceStats(
            presentCount = present,
            lateCount = late,
            sickCount = sick,
            absentCount = absent,
            excusedCount = excused,
            totalDays = total,
            attendancePercentage = pct.coerceIn(0f, 100f),
            syncPendingCount = pending
        )
    }

    suspend fun saveServerConfig(config: AttendanceConfigEntity) = withContext(Dispatchers.IO) {
        val secured = authService.encryptConfigSecrets(config)
        configDao.saveConfig(secured)
        logDao.insertLog(
            AttendanceLogEntity(
                logType = "CONNECTION",
                provider = config.providerType,
                message = "Server configuration updated (${config.providerType})",
                details = "Endpoint: ${config.serverUrl.ifBlank { "Local Only" }}:${config.port} | Auth: ${config.authType} | Device: ${config.deviceId.ifBlank { "Unset" }}"
            )
        )
    }

    suspend fun testServerConnection(config: AttendanceConfigEntity): SyncResult = withContext(Dispatchers.IO) {
        val provider = deviceService.getProvider(config.providerType)
        val result = provider.testConnection(config)

        logDao.insertLog(
            AttendanceLogEntity(
                logType = "CONNECTION",
                provider = config.providerType,
                message = "Test Connection (${config.providerType}): ${if (result.isSuccess) "Success" else "Failed"}",
                details = result.message,
                statusCode = if (result.isSuccess) 200 else 503,
                isSuccess = result.isSuccess
            )
        )

        val updatedConfig = config.copy(
            isConnected = result.isSuccess,
            lastTestTimestamp = System.currentTimeMillis()
        )
        configDao.saveConfig(authService.encryptConfigSecrets(updatedConfig))

        result
    }

    suspend fun punchAttendance(
        usageType: String,
        location: String = "Main Gate #1 Turnstile",
        cardUid: String? = null,
        cardNickname: String? = null,
        status: String = "PRESENT"
    ): SyncResult = withContext(Dispatchers.IO) {
        val rawConfig = configDao.getConfig()
        val config = if (rawConfig != null) authService.decryptConfigSecrets(rawConfig) else AttendanceConfigEntity()
        val provider = deviceService.getProvider(config.providerType)

        val activeUid = cardUid?.ifBlank { null } ?: config.linkedCardUid.ifBlank { "LOCAL-PASS" }
        val activeNickname = cardNickname?.ifBlank { null } ?: config.linkedCardNickname.ifBlank { "Campus Pass" }
        val activeStudentName = config.activeStudentName.ifBlank { "Student" }

        // Save locally as PENDING first; only mark SYNCED if server actually accepts the punch
        val pendingRecord = AttendanceEntity(
            studentId = config.activeStudentId,
            studentName = activeStudentName,
            timestamp = System.currentTimeMillis(),
            usageType = usageType,
            status = status,
            providerType = config.providerType,
            syncStatus = "PENDING",
            location = location,
            deviceId = config.deviceId.ifBlank { "LOCAL-DEVICE" },
            campusId = config.campusId,
            cardUid = activeUid,
            cardNickname = activeNickname
        )

        val recordId = attendanceDao.insertRecord(pendingRecord)
        val insertedRecord = pendingRecord.copy(id = recordId)

        val syncResult = if (config.serverUrl.isNotBlank() && authService.hasValidCredentials(config)) {
            provider.recordAttendancePunch(config, insertedRecord)
        } else {
            SyncResult(
                isSuccess = false,
                syncedCount = 0,
                failedCount = 0,
                message = "Punch recorded locally (queued as PENDING for server sync).",
                provider = config.providerType
            )
        }

        val finalSyncStatus = if (syncResult.isSuccess) "SYNCED" else "PENDING"
        attendanceDao.updateRecord(
            insertedRecord.copy(
                syncStatus = finalSyncStatus,
                lastSyncError = if (syncResult.isSuccess) null else syncResult.message
            )
        )

        logDao.insertLog(
            AttendanceLogEntity(
                logType = "ACTIVITY",
                provider = config.providerType,
                message = "Recorded $usageType ($status) with '$activeNickname' [$activeUid]",
                details = "Location: $location | Sync: $finalSyncStatus | ${syncResult.message}",
                statusCode = if (syncResult.isSuccess) 200 else 202,
                isSuccess = true
            )
        )

        syncResult
    }

    suspend fun updateRecordStatus(id: Long, newStatus: String) = withContext(Dispatchers.IO) {
        attendanceDao.updateRecordStatus(id, newStatus)
        val config = configDao.getConfig() ?: AttendanceConfigEntity()
        logDao.insertLog(
            AttendanceLogEntity(
                logType = "ACTIVITY",
                provider = config.providerType,
                message = "Updated attendance record #$id status to $newStatus",
                details = "Manual status adjustment",
                isSuccess = true
            )
        )
    }

    suspend fun performManualSync(): SyncResult = withContext(Dispatchers.IO) {
        syncService.performSync(isManual = true)
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        logDao.clearAllLogs()
    }

    suspend fun clearRecords() = withContext(Dispatchers.IO) {
        attendanceDao.clearAllRecords()
    }
}
