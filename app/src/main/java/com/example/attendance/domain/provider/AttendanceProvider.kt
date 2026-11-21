package com.example.attendance.domain.provider

import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.attendance.data.entity.AttendanceEntity
import com.example.attendance.domain.model.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

interface AttendanceProvider {
    val providerName: String

    suspend fun testConnection(config: AttendanceConfigEntity): SyncResult

    suspend fun fetchAttendanceHistory(
        config: AttendanceConfigEntity,
        studentId: String
    ): List<AttendanceEntity>

    suspend fun recordAttendancePunch(
        config: AttendanceConfigEntity,
        record: AttendanceEntity
    ): SyncResult

    suspend fun getDeviceStatus(config: AttendanceConfigEntity): String
}

/**
 * Shared real HTTP/HTTPS client helper for all attendance server providers.
 * Always reports honest connection/sync failures instead of fabricating success when offline.
 */
abstract class BaseHttpAttendanceProvider(
    override val providerName: String,
    private val healthPath: String,
    private val punchPath: String,
    private val historyPathPrefix: String,
    private val userAgent: String
) : AttendanceProvider {

    protected fun buildEndpointUrl(config: AttendanceConfigEntity, path: String): String? {
        val rawHost = config.serverUrl.trim()
        if (rawHost.isBlank()) return null
        if (config.port !in 1..65535) return null
        val scheme = if (config.useHttps) "https" else "http"
        val cleanHost = rawHost
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
        if (cleanHost.isBlank() || cleanHost.contains(" ")) return null
        val defaultPort = if (config.useHttps) 443 else 80
        val portSuffix = if (config.port == defaultPort) "" else ":${config.port}"
        return "$scheme://$cleanHost$portSuffix$path"
    }

    private fun openConfiguredConnection(
        fullUrl: String,
        method: String,
        config: AttendanceConfigEntity
    ): HttpURLConnection {
        val url = URL(fullUrl)
        val conn = (if (config.useHttps) {
            url.openConnection() as HttpsURLConnection
        } else {
            url.openConnection() as HttpURLConnection
        }).apply {
            requestMethod = method
            connectTimeout = 5000
            readTimeout = 6000
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
            if (method == "POST") {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            when (config.authType.uppercase()) {
                "API_KEY" -> if (config.apiKey.isNotBlank()) setRequestProperty("X-API-KEY", config.apiKey)
                "JWT", "OAUTH2" -> {
                    val token = config.authToken.ifBlank { config.apiKey }
                    if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
                }
                "SESSION" -> {
                    val token = config.authToken.ifBlank { config.apiKey }
                    if (token.isNotBlank()) setRequestProperty("X-Session-Token", token)
                }
                else -> if (config.apiKey.isNotBlank()) setRequestProperty("X-API-KEY", config.apiKey)
            }
        }
        return conn
    }

    override suspend fun testConnection(config: AttendanceConfigEntity): SyncResult = withContext(Dispatchers.IO) {
        val fullUrl = buildEndpointUrl(config, healthPath)
            ?: return@withContext SyncResult(
                isSuccess = false,
                syncedCount = 0,
                failedCount = 0,
                message = "Invalid server URL or port. Enter a valid host and port (1–65535) in Server Config.",
                latencyMs = 0L,
                provider = providerName
            )

        val startTime = System.currentTimeMillis()
        var conn: HttpURLConnection? = null
        try {
            conn = openConfiguredConnection(fullUrl, "GET", config)
            val responseCode = conn.responseCode
            val latency = System.currentTimeMillis() - startTime
            if (responseCode in 200..299) {
                SyncResult(
                    isSuccess = true,
                    syncedCount = 0,
                    failedCount = 0,
                    message = "$providerName server reachable (HTTP $responseCode) in ${latency}ms.",
                    latencyMs = latency,
                    provider = providerName
                )
            } else {
                SyncResult(
                    isSuccess = false,
                    syncedCount = 0,
                    failedCount = 0,
                    message = "$providerName server responded with HTTP $responseCode. Verify endpoint path and credentials.",
                    latencyMs = latency,
                    provider = providerName
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            SyncResult(
                isSuccess = false,
                syncedCount = 0,
                failedCount = 0,
                message = "Could not reach $providerName server ($fullUrl): ${e.localizedMessage ?: e.javaClass.simpleName}",
                latencyMs = latency,
                provider = providerName
            )
        } finally {
            conn?.disconnect()
        }
    }

    override suspend fun recordAttendancePunch(
        config: AttendanceConfigEntity,
        record: AttendanceEntity
    ): SyncResult = withContext(Dispatchers.IO) {
        val fullUrl = buildEndpointUrl(config, punchPath)
            ?: return@withContext SyncResult(
                isSuccess = false,
                syncedCount = 0,
                failedCount = 1,
                message = "No server configured — punch saved locally as PENDING.",
                latencyMs = 0L,
                provider = providerName
            )

        val startTime = System.currentTimeMillis()
        var conn: HttpURLConnection? = null
        try {
            val payload = JSONObject().apply {
                put("studentId", record.studentId)
                put("studentName", record.studentName)
                put("timestamp", record.timestamp)
                put("usageType", record.usageType)
                put("status", record.status)
                put("location", record.location)
                put("deviceId", record.deviceId)
                put("campusId", record.campusId)
                put("cardUid", record.cardUid)
                put("provider", providerName)
            }.toString()

            conn = openConfiguredConnection(fullUrl, "POST", config)
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload)
                writer.flush()
            }

            val responseCode = conn.responseCode
            val latency = System.currentTimeMillis() - startTime
            if (responseCode in 200..299) {
                SyncResult(
                    isSuccess = true,
                    syncedCount = 1,
                    failedCount = 0,
                    message = "Punch synced to $providerName server (HTTP $responseCode).",
                    latencyMs = latency,
                    provider = providerName
                )
            } else {
                SyncResult(
                    isSuccess = false,
                    syncedCount = 0,
                    failedCount = 1,
                    message = "$providerName server rejected punch (HTTP $responseCode). Saved locally for retry.",
                    latencyMs = latency,
                    provider = providerName
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            SyncResult(
                isSuccess = false,
                syncedCount = 0,
                failedCount = 1,
                message = "Server unreachable (${e.localizedMessage ?: e.javaClass.simpleName}). Saved locally as PENDING.",
                latencyMs = latency,
                provider = providerName
            )
        } finally {
            conn?.disconnect()
        }
    }

    override suspend fun fetchAttendanceHistory(
        config: AttendanceConfigEntity,
        studentId: String
    ): List<AttendanceEntity> = withContext(Dispatchers.IO) {
        if (studentId.isBlank()) return@withContext emptyList()
        val encodedId = java.net.URLEncoder.encode(studentId, "UTF-8")
        val fullUrl = buildEndpointUrl(config, "$historyPathPrefix?studentId=$encodedId") ?: return@withContext emptyList()

        var conn: HttpURLConnection? = null
        try {
            conn = openConfiguredConnection(fullUrl, "GET", config)
            if (conn.responseCode !in 200..299) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.trim()
            if (body.isBlank()) return@withContext emptyList()

            val arr = if (body.startsWith("[")) {
                JSONArray(body)
            } else {
                val root = JSONObject(body)
                root.optJSONArray("records") ?: root.optJSONArray("data") ?: JSONArray()
            }

            val parsed = mutableListOf<AttendanceEntity>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                parsed.add(
                    AttendanceEntity(
                        studentId = obj.optString("studentId", studentId),
                        studentName = obj.optString("studentName", config.activeStudentName),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        usageType = obj.optString("usageType", "PUNCH_IN"),
                        status = obj.optString("status", "PRESENT"),
                        providerType = providerName,
                        syncStatus = "SYNCED",
                        location = obj.optString("location", "Remote Terminal"),
                        deviceId = obj.optString("deviceId", config.deviceId),
                        campusId = obj.optString("campusId", config.campusId),
                        cardUid = obj.optString("cardUid", config.linkedCardUid),
                        cardNickname = obj.optString("cardNickname", config.linkedCardNickname)
                    )
                )
            }
            parsed
        } catch (e: Exception) {
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

    override suspend fun getDeviceStatus(config: AttendanceConfigEntity): String {
        val endpoint = config.serverUrl.ifBlank { "Not configured" }
        val status = if (config.isConnected) "Connected" else "Offline / Local Mode"
        return "$providerName (${config.deviceId.ifBlank { "Local" }}) • $status • $endpoint:${config.port}"
    }
}
