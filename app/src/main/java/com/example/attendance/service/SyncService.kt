package com.example.attendance.service

import com.example.attendance.data.dao.AttendanceConfigDao
import com.example.attendance.data.dao.AttendanceDao
import com.example.attendance.data.dao.AttendanceLogDao
import com.example.attendance.data.entity.AttendanceLogEntity
import com.example.attendance.domain.model.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncService(
    private val attendanceDao: AttendanceDao,
    private val configDao: AttendanceConfigDao,
    private val logDao: AttendanceLogDao,
    private val deviceService: DeviceService,
    private val authService: AuthenticationService
) {

    suspend fun performSync(isManual: Boolean = false): SyncResult = withContext(Dispatchers.IO) {
        val encryptedConfig = configDao.getConfig()
        val config = encryptedConfig?.let { authService.decryptConfigSecrets(it) }

        if (config == null || config.serverUrl.isBlank()) {
            val msg = "No attendance server configured. Open Server Config (Admin) to configure an endpoint."
            logDao.insertLog(
                AttendanceLogEntity(
                    logType = "SYNC",
                    provider = config?.providerType ?: "LOCAL",
                    message = msg,
                    details = "Manual: $isManual | Offline / Local Mode",
                    statusCode = 0,
                    isSuccess = false
                )
            )
            return@withContext SyncResult(
                isSuccess = false,
                syncedCount = 0,
                failedCount = 0,
                message = msg,
                provider = config?.providerType ?: "LOCAL"
            )
        }

        val startTime = System.currentTimeMillis()
        val provider = deviceService.getProvider(config.providerType)

        // 1. Verify connectivity before attempting batch upload
        val healthCheck = provider.testConnection(config)
        if (!healthCheck.isSuccess) {
            val latency = System.currentTimeMillis() - startTime
            configDao.saveConfig(encryptedConfig.copy(isConnected = false, lastTestTimestamp = System.currentTimeMillis()))
            logDao.insertLog(
                AttendanceLogEntity(
                    logType = "SYNC",
                    provider = config.providerType,
                    message = "Sync aborted: server unreachable",
                    details = healthCheck.message,
                    statusCode = 503,
                    isSuccess = false
                )
            )
            return@withContext SyncResult(
                isSuccess = false,
                syncedCount = 0,
                failedCount = attendanceDao.getUnsyncedRecords().size,
                message = healthCheck.message,
                latencyMs = latency,
                provider = provider.providerName
            )
        }

        // 2. Push unsynced local records
        val unsynced = attendanceDao.getUnsyncedRecords()
        var syncedCount = 0
        var failedCount = 0

        for (record in unsynced) {
            val punchResult = provider.recordAttendancePunch(config, record)
            if (punchResult.isSuccess) {
                syncedCount++
                attendanceDao.updateRecord(
                    record.copy(
                        syncStatus = "SYNCED",
                        retryCount = record.retryCount + 1,
                        lastSyncError = null
                    )
                )
            } else {
                failedCount++
                attendanceDao.updateRecord(
                    record.copy(
                        syncStatus = "FAILED",
                        retryCount = record.retryCount + 1,
                        lastSyncError = punchResult.message
                    )
                )
            }
        }

        // 3. Pull remote records and deduplicate before inserting
        val remoteRecords = provider.fetchAttendanceHistory(config, config.activeStudentId)
        var newlyPulledCount = 0
        if (remoteRecords.isNotEmpty()) {
            val existingKeys = attendanceDao.getAllRecordsList().map { existing ->
                "${existing.studentId}|${existing.timestamp}|${existing.usageType}|${existing.location}"
            }.toHashSet()

            val uniqueRemote = remoteRecords.filter { remote ->
                val key = "${remote.studentId}|${remote.timestamp}|${remote.usageType}|${remote.location}"
                existingKeys.add(key)
            }
            if (uniqueRemote.isNotEmpty()) {
                attendanceDao.insertRecords(uniqueRemote)
                newlyPulledCount = uniqueRemote.size
            }
        }

        val latency = System.currentTimeMillis() - startTime
        val isAllOk = failedCount == 0
        val message = when {
            unsynced.isEmpty() && newlyPulledCount == 0 ->
                "Sync complete via ${provider.providerName}. All records up to date (${latency}ms)."
            failedCount == 0 ->
                "Synced $syncedCount local and pulled $newlyPulledCount remote record(s) via ${provider.providerName}."
            else ->
                "Synced $syncedCount record(s), $failedCount failed. Pending items will retry on next sync."
        }

        logDao.insertLog(
            AttendanceLogEntity(
                logType = "SYNC",
                provider = config.providerType,
                message = message,
                details = "Manual: $isManual | Latency: ${latency}ms | Provider: ${provider.providerName}",
                statusCode = if (isAllOk) 200 else 207,
                isSuccess = isAllOk
            )
        )

        configDao.saveConfig(
            encryptedConfig.copy(
                isConnected = true,
                lastSyncTimestamp = System.currentTimeMillis()
            )
        )

        SyncResult(
            isSuccess = isAllOk,
            syncedCount = syncedCount + newlyPulledCount,
            failedCount = failedCount,
            message = message,
            latencyMs = latency,
            provider = provider.providerName
        )
    }
}
