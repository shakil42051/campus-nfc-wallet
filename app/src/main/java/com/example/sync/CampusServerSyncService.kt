package com.example.sync

import com.example.attendance.data.dao.AttendanceConfigDao
import com.example.attendance.data.dao.AttendanceLogDao
import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.attendance.data.entity.AttendanceLogEntity
import com.example.attendance.service.AuthenticationService
import com.example.attendance.service.SyncService
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.UsageTransactionEntity
import com.example.data.model.NfcScanResult
import com.example.data.model.SecurityAnalysis
import com.example.data.repository.CardRepository
import com.example.security.CampusRoles
import com.example.security.RoleAccessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

data class ServerAuthResult(
    val isSuccess: Boolean,
    val role: String = CampusRoles.STUDENT,
    val username: String = "",
    val displayName: String = "",
    val message: String = "",
    val httpStatus: Int = 0
)

data class WalletSyncResult(
    val isSuccess: Boolean,
    val cardsPushed: Int = 0,
    val cardsPulled: Int = 0,
    val transactionsPushed: Int = 0,
    val attendanceSynced: Int = 0,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Unified Campus Server Link & Wallet Synchronization Engine.
 *
 * Connects the local encrypted wallet (Cards + Usage Transactions) and Attendance module
 * to a real campus REST server over HTTPS/HTTP:
 *  - POST /api/v1/auth/login            -> Server-verified account login & role issuance (STUDENT/TEACHER/ADMIN)
 *  - GET  /api/v1/health                -> Connectivity & credential verification
 *  - POST /api/v1/wallet/sync/cards     -> Syncs wallet cards up & pulls server-provisioned campus passes down
 *  - POST /api/v1/wallet/sync/transactions -> Uploads card usage activity logs
 *  - SyncService.performSync(...)       -> Flushes pending attendance punches & pulls attendance history
 *
 * Never fabricates success when offline or when a server rejects requests.
 */
class CampusServerSyncService(
    private val cardRepository: CardRepository,
    private val configDao: AttendanceConfigDao,
    private val logDao: AttendanceLogDao,
    private val authService: AuthenticationService,
    private val attendanceSyncService: SyncService,
    private val roleAccessManager: RoleAccessManager
) {

    companion object {
        const val LOGIN_PATH = "/api/v1/auth/login"
        const val HEALTH_PATH = "/api/v1/health"
        const val WALLET_CARDS_SYNC_PATH = "/api/v1/wallet/sync/cards"
        const val WALLET_TX_SYNC_PATH = "/api/v1/wallet/sync/transactions"
        private const val USER_AGENT = "CampusNFCWallet/1.0"
    }

    internal fun buildEndpointUrl(serverUrl: String, port: Int, useHttps: Boolean, path: String): String? {
        val rawHost = serverUrl.trim()
        if (rawHost.isBlank() || port !in 1..65535) return null
        val scheme = if (useHttps) "https" else "http"
        val cleanHost = rawHost
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
        if (cleanHost.isBlank() || cleanHost.contains(" ")) return null
        val defaultPort = if (useHttps) 443 else 80
        val portSuffix = if (port == defaultPort) "" else ":$port"
        return "$scheme://$cleanHost$portSuffix$path"
    }

    private fun openConnection(
        fullUrl: String,
        method: String,
        useHttps: Boolean,
        config: AttendanceConfigEntity? = null
    ): HttpURLConnection {
        val url = URL(fullUrl)
        val conn = (if (useHttps) {
            url.openConnection() as HttpsURLConnection
        } else {
            url.openConnection() as HttpURLConnection
        }).apply {
            requestMethod = method
            connectTimeout = 5000
            readTimeout = 7000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            if (method == "POST") {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            if (config != null) {
                authService.applyAuthHeaders(this, config)
            }
        }
        return conn
    }

    private fun readBodySafely(conn: HttpURLConnection): String {
        val stream = try {
            if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        } catch (_: Exception) {
            conn.errorStream
        } ?: return ""
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }.trim()
    }

    /**
     * Authenticates a user (Student, Teacher, or Admin) against the campus server.
     * Persists the Keystore-encrypted JWT token and applies the server-verified role to RoleAccessManager.
     */
    suspend fun authenticateWithServer(
        serverUrl: String,
        port: Int,
        useHttps: Boolean,
        username: String,
        password: String
    ): ServerAuthResult = withContext(Dispatchers.IO) {
        val trimmedUser = username.trim()
        if (trimmedUser.isBlank() || password.isBlank()) {
            return@withContext ServerAuthResult(
                isSuccess = false,
                message = "Username/email and password are required."
            )
        }
        val loginUrl = buildEndpointUrl(serverUrl, port, useHttps, LOGIN_PATH)
            ?: return@withContext ServerAuthResult(
                isSuccess = false,
                message = "Invalid server host or port (1–65535)."
            )

        val currentRaw = configDao.getConfig()
        val currentConfig = if (currentRaw != null) {
            authService.decryptConfigSecrets(currentRaw)
        } else {
            AttendanceConfigEntity()
        }

        var conn: HttpURLConnection? = null
        try {
            val requestBody = JSONObject().apply {
                put("username", trimmedUser)
                put("password", password)
                put("deviceId", currentConfig.deviceId.ifBlank { "ANDROID-WALLET" })
            }.toString()

            conn = openConnection(loginUrl, "POST", useHttps)
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val code = conn.responseCode
            val body = readBodySafely(conn)

            if (code !in 200..299) {
                val serverErr = runCatching { JSONObject(body).optString("message", "") }.getOrDefault("")
                val msg = when (code) {
                    401, 403 -> serverErr.ifBlank { "Invalid username or password (HTTP $code)." }
                    404 -> "Login endpoint $LOGIN_PATH not found on server (HTTP 404)."
                    else -> serverErr.ifBlank { "Server authentication failed (HTTP $code)." }
                }
                logDao.insertLog(
                    AttendanceLogEntity(
                        logType = "AUTH",
                        provider = currentConfig.providerType,
                        message = "Server login rejected for '$trimmedUser' (HTTP $code)",
                        details = msg,
                        statusCode = code,
                        isSuccess = false
                    )
                )
                return@withContext ServerAuthResult(isSuccess = false, message = msg, httpStatus = code)
            }

            val json = JSONObject(body)
            val token = json.optString("token").ifBlank { json.optString("accessToken") }
            if (token.isBlank()) {
                return@withContext ServerAuthResult(
                    isSuccess = false,
                    message = "Server response missing authentication token.",
                    httpStatus = code
                )
            }

            val serverRole = CampusRoles.normalize(json.optString("role", CampusRoles.STUDENT))
            val userId = json.optString("userId", json.optString("studentId", currentConfig.activeStudentId))
            val displayName = json.optString("name", currentConfig.activeStudentName.ifBlank { trimmedUser })
            val department = json.optString("department", currentConfig.department)
            val email = json.optString("email", if (trimmedUser.contains("@")) trimmedUser else currentConfig.email)
            val campusId = json.optString("campusId", currentConfig.campusId)
            val orgId = json.optString("organizationId", currentConfig.organizationId)
            val expiresAt = json.optLong("expiresAt", System.currentTimeMillis() + 24L * 60L * 60L * 1000L)

            val updatedConfig = currentConfig.copy(
                serverUrl = serverUrl.trim(),
                port = port,
                useHttps = useHttps,
                authType = "JWT",
                authToken = token,
                tokenExpiryTimestamp = expiresAt,
                username = trimmedUser,
                userRole = serverRole,
                activeStudentId = userId,
                activeStudentName = displayName,
                department = department,
                email = email,
                campusId = campusId,
                organizationId = orgId,
                isConnected = true,
                lastTestTimestamp = System.currentTimeMillis()
            )
            configDao.saveConfig(authService.encryptConfigSecrets(updatedConfig))
            roleAccessManager.applyServerVerifiedSession(serverRole, displayName.ifBlank { trimmedUser })

            logDao.insertLog(
                AttendanceLogEntity(
                    logType = "AUTH",
                    provider = updatedConfig.providerType,
                    message = "Authenticated with campus server as $serverRole ($trimmedUser)",
                    details = "Endpoint: $loginUrl | Role verified by server",
                    statusCode = code,
                    isSuccess = true
                )
            )

            ServerAuthResult(
                isSuccess = true,
                role = serverRole,
                username = trimmedUser,
                displayName = displayName,
                message = "Signed in as $serverRole ($displayName) and linked to server.",
                httpStatus = code
            )
        } catch (e: Exception) {
            val errMsg = "Could not reach server ($loginUrl): ${e.localizedMessage ?: e.javaClass.simpleName}"
            logDao.insertLog(
                AttendanceLogEntity(
                    logType = "AUTH",
                    provider = currentConfig.providerType,
                    message = "Server login network error",
                    details = errMsg,
                    statusCode = 503,
                    isSuccess = false
                )
            )
            ServerAuthResult(isSuccess = false, message = errMsg, httpStatus = 503)
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Links the wallet server using an API Key / Bearer Token (Admin-configured) and verifies connectivity.
     */
    suspend fun linkWithApiKey(
        serverUrl: String,
        port: Int,
        useHttps: Boolean,
        authType: String,
        apiKeyOrToken: String,
        providerType: String,
        campusId: String,
        deviceId: String
    ): WalletSyncResult = withContext(Dispatchers.IO) {
        val healthUrl = buildEndpointUrl(serverUrl, port, useHttps, HEALTH_PATH)
            ?: return@withContext WalletSyncResult(
                isSuccess = false,
                message = "Enter a valid Server Host and Port (1–65535)."
            )

        val currentRaw = configDao.getConfig()
        val currentConfig = if (currentRaw != null) authService.decryptConfigSecrets(currentRaw) else AttendanceConfigEntity()
        val candidate = currentConfig.copy(
            serverUrl = serverUrl.trim(),
            port = port,
            useHttps = useHttps,
            authType = authType,
            apiKey = if (authType == "API_KEY") apiKeyOrToken.trim() else currentConfig.apiKey,
            authToken = if (authType != "API_KEY") apiKeyOrToken.trim() else currentConfig.authToken,
            providerType = providerType,
            campusId = campusId.trim(),
            deviceId = deviceId.trim()
        )

        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(healthUrl, "GET", useHttps, candidate)
            val code = conn.responseCode
            val reachable = code in 200..299
            val saved = candidate.copy(
                isConnected = reachable,
                lastTestTimestamp = System.currentTimeMillis()
            )
            configDao.saveConfig(authService.encryptConfigSecrets(saved))

            val message = if (reachable) {
                "Linked to campus server ($healthUrl • HTTP $code)."
            } else {
                "Saved server config, but health check returned HTTP $code. Verify credentials and endpoint."
            }
            logDao.insertLog(
                AttendanceLogEntity(
                    logType = "CONNECTION",
                    provider = saved.providerType,
                    message = if (reachable) "Wallet server linked" else "Wallet server health check failed (HTTP $code)",
                    details = message,
                    statusCode = code,
                    isSuccess = reachable
                )
            )
            WalletSyncResult(isSuccess = reachable, message = message)
        } catch (e: Exception) {
            val saved = candidate.copy(
                isConnected = false,
                lastTestTimestamp = System.currentTimeMillis()
            )
            configDao.saveConfig(authService.encryptConfigSecrets(saved))
            val msg = "Server saved, but unreachable ($healthUrl): ${e.localizedMessage ?: e.javaClass.simpleName}"
            logDao.insertLog(
                AttendanceLogEntity(
                    logType = "CONNECTION",
                    provider = saved.providerType,
                    message = "Wallet server unreachable",
                    details = msg,
                    statusCode = 503,
                    isSuccess = false
                )
            )
            WalletSyncResult(isSuccess = false, message = msg)
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Unlinks the server session, clears stored API tokens, and drops elevated staff role back to STUDENT.
     */
    suspend fun unlinkServer() = withContext(Dispatchers.IO) {
        val currentRaw = configDao.getConfig()
        val current = if (currentRaw != null) authService.decryptConfigSecrets(currentRaw) else AttendanceConfigEntity()
        val cleared = current.copy(
            authToken = "",
            tokenExpiryTimestamp = 0L,
            isConnected = false,
            autoSyncEnabled = false,
            userRole = CampusRoles.STUDENT
        )
        configDao.saveConfig(authService.encryptConfigSecrets(cleared))
        roleAccessManager.lockElevatedSession()
        logDao.insertLog(
            AttendanceLogEntity(
                logType = "AUTH",
                provider = current.providerType,
                message = "Unlinked campus server session",
                details = "Cleared server session token and reset active role to STUDENT.",
                statusCode = 200,
                isSuccess = true
            )
        )
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        val currentRaw = configDao.getConfig()
        val current = if (currentRaw != null) authService.decryptConfigSecrets(currentRaw) else AttendanceConfigEntity()
        configDao.saveConfig(authService.encryptConfigSecrets(current.copy(autoSyncEnabled = enabled)))
    }

    /**
     * Runs a full wallet + attendance synchronization against the linked campus server.
     */
    suspend fun syncAllToServer(): WalletSyncResult = withContext(Dispatchers.IO) {
        val rawConfig = configDao.getConfig()
            ?: return@withContext WalletSyncResult(
                isSuccess = false,
                message = "No campus server linked yet. Link your server in Settings to enable cloud sync."
            )
        val config = authService.decryptConfigSecrets(rawConfig)

        if (config.serverUrl.isBlank()) {
            return@withContext WalletSyncResult(
                isSuccess = false,
                message = "No campus server URL configured. Link your server in Settings."
            )
        }
        if (!authService.hasValidCredentials(config)) {
            return@withContext WalletSyncResult(
                isSuccess = false,
                message = "Server credentials missing or session expired. Sign in again to sync."
            )
        }

        val activeCards = cardRepository.allCards.first()
        val archivedCards = cardRepository.archivedCards.first()
        val allCards = activeCards + archivedCards
        val allTransactions = cardRepository.allTransactions.first()

        var cardsPushed = 0
        var cardsPulled = 0
        var txPushed = 0
        val errors = mutableListOf<String>()

        // 1. Sync Wallet Cards (push local cards + pull server-provisioned campus passes)
        val cardsUrl = buildEndpointUrl(config.serverUrl, config.port, config.useHttps, WALLET_CARDS_SYNC_PATH)
        if (cardsUrl != null) {
            var conn: HttpURLConnection? = null
            try {
                val payload = JSONObject().apply {
                    put("deviceId", config.deviceId.ifBlank { "ANDROID-WALLET" })
                    put("campusId", config.campusId)
                    put("studentId", config.activeStudentId)
                    put("cards", JSONArray().apply {
                        allCards.forEach { card ->
                            put(JSONObject().apply {
                                put("uid", card.uid)
                                put("nickname", card.nickname)
                                put("manufacturer", card.manufacturer)
                                put("technology", card.technology)
                                put("cardStandard", card.cardStandard)
                                put("timestamp", card.timestamp)
                                put("isFavorite", card.isFavorite)
                                put("isArchived", card.isArchived)
                                put("notes", card.notes)
                            })
                        }
                    })
                }.toString()

                conn = openConnection(cardsUrl, "POST", config.useHttps, config)
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                    it.write(payload)
                    it.flush()
                }
                val code = conn.responseCode
                val body = readBodySafely(conn)
                if (code in 200..299) {
                    cardsPushed = allCards.size
                    cardsPulled = importServerCardsIfPresent(body, allCards)
                } else {
                    errors.add("Cards sync HTTP $code")
                }
            } catch (e: Exception) {
                errors.add("Cards sync error: ${e.localizedMessage ?: e.javaClass.simpleName}")
            } finally {
                conn?.disconnect()
            }
        }

        // 2. Sync Usage Transactions
        val txUrl = buildEndpointUrl(config.serverUrl, config.port, config.useHttps, WALLET_TX_SYNC_PATH)
        if (txUrl != null) {
            var conn: HttpURLConnection? = null
            try {
                val payload = JSONObject().apply {
                    put("deviceId", config.deviceId.ifBlank { "ANDROID-WALLET" })
                    put("campusId", config.campusId)
                    put("studentId", config.activeStudentId)
                    put("transactions", JSONArray().apply {
                        allTransactions.forEach { tx ->
                            put(JSONObject().apply {
                                put("id", tx.id)
                                put("cardUid", tx.cardUid)
                                put("cardNickname", tx.cardNickname)
                                put("timestamp", tx.timestamp)
                                put("usageType", tx.usageType)
                                put("purposeOrAction", tx.purposeOrAction)
                                put("status", tx.status)
                                put("terminalLocation", tx.terminalLocation)
                            })
                        }
                    })
                }.toString()

                conn = openConnection(txUrl, "POST", config.useHttps, config)
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                    it.write(payload)
                    it.flush()
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    txPushed = allTransactions.size
                } else {
                    errors.add("Transactions sync HTTP $code")
                }
            } catch (e: Exception) {
                errors.add("Transactions sync error: ${e.localizedMessage ?: e.javaClass.simpleName}")
            } finally {
                conn?.disconnect()
            }
        }

        // 3. Sync Pending Attendance Records
        val attendanceResult = attendanceSyncService.performSync(isManual = true)
        if (!attendanceResult.isSuccess && attendanceResult.failedCount > 0) {
            errors.add(attendanceResult.message)
        }

        val overallSuccess = errors.isEmpty()
        val now = System.currentTimeMillis()
        configDao.saveConfig(
            authService.encryptConfigSecrets(
                config.copy(
                    isConnected = overallSuccess,
                    lastSyncTimestamp = if (overallSuccess) now else config.lastSyncTimestamp
                )
            )
        )

        val summary = if (overallSuccess) {
            "Wallet synced: $cardsPushed cards pushed, $cardsPulled pulled, $txPushed logs & ${attendanceResult.syncedCount} attendance synced."
        } else {
            "Sync incomplete: ${errors.joinToString("; ")}"
        }

        logDao.insertLog(
            AttendanceLogEntity(
                logType = "SYNC",
                provider = config.providerType,
                message = if (overallSuccess) "Full Wallet & Attendance Sync Completed" else "Wallet Sync Failed",
                details = summary,
                statusCode = if (overallSuccess) 200 else 503,
                isSuccess = overallSuccess
            )
        )

        WalletSyncResult(
            isSuccess = overallSuccess,
            cardsPushed = cardsPushed,
            cardsPulled = cardsPulled,
            transactionsPushed = txPushed,
            attendanceSynced = attendanceResult.syncedCount,
            message = summary,
            timestamp = now
        )
    }

    /**
     * Triggers a background sync silently if the user enabled autoSync and has a live server link.
     */
    suspend fun syncIfAutoEnabled() {
        val raw = configDao.getConfig() ?: return
        val cfg = authService.decryptConfigSecrets(raw)
        if (cfg.autoSyncEnabled && cfg.isConnected && cfg.serverUrl.isNotBlank() && authService.hasValidCredentials(cfg)) {
            runCatching { syncAllToServer() }
        }
    }

    private suspend fun importServerCardsIfPresent(responseBody: String, existingCards: List<CardEntity>): Int {
        if (responseBody.isBlank()) return 0
        return try {
            val root = JSONObject(responseBody)
            val serverCards = root.optJSONArray("serverCards") ?: return 0
            val existingUids = existingCards.map { it.uid.uppercase(Locale.US) }.toMutableSet()
            var imported = 0
            for (i in 0 until serverCards.length()) {
                val obj = serverCards.optJSONObject(i) ?: continue
                val uid = obj.optString("uid").trim()
                if (uid.isBlank() || existingUids.contains(uid.uppercase(Locale.US))) continue
                val nickname = obj.optString("nickname", "Campus Pass").ifBlank { "Campus Pass" }
                val manufacturer = obj.optString("manufacturer", "Campus Issued")
                val tech = obj.optString("technology", "Campus Server Pass")
                val standard = obj.optString("cardStandard", "ISO/IEC 14443-A")
                val notes = obj.optString("notes", "Provisioned by Campus Server")

                val scan = NfcScanResult(
                    uid = uid,
                    uidHex = uid.replace(":", "").uppercase(Locale.US),
                    uidAscii = uid,
                    uidBinary = "",
                    uidLengthBytes = uid.toByteArray(Charsets.UTF_8).size,
                    manufacturer = manufacturer,
                    primaryTechnology = tech,
                    cardStandard = standard,
                    technologies = listOf(tech),
                    atqa = "N/A",
                    sak = "N/A",
                    ats = "N/A",
                    historicalBytes = "SERVER_PROVISIONED",
                    memorySizeBytes = "N/A",
                    blockCount = "N/A",
                    pageCount = "N/A",
                    isNdefSupported = false,
                    isNdefWritable = false,
                    isNdefReadOnly = true,
                    ndefMaxSizeBytes = 0,
                    ndefRecords = emptyList(),
                    memorySectors = emptyList(),
                    securityAnalysis = SecurityAnalysis(
                        encryptionType = "Server Provisioned Digital Pass",
                        supportsCloning = false,
                        cloneRiskLevel = "Low (Server Issued)",
                        explanation = "Synchronized directly from your linked campus server.",
                        hardwareSecurityFeatures = listOf("Server Provisioned", "Encrypted Local Storage")
                    ),
                    rawDataDumpHex = obj.optString("rawMetadata", "")
                )
                cardRepository.saveCardFromScan(scan, nickname, notes)
                existingUids.add(uid.uppercase(Locale.US))
                imported++
            }
            imported
        } catch (_: Exception) {
            0
        }
    }
}
