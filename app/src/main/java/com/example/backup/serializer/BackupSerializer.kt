package com.example.backup.serializer

import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.attendance.data.entity.AttendanceEntity
import com.example.attendance.data.entity.AttendanceLogEntity
import com.example.backup.model.BackupEnvelope
import com.example.backup.model.BackupException
import com.example.backup.model.BackupPayload
import com.example.backup.model.BackupRecordCounts
import com.example.backup.model.EncryptionMetadata
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.UsageTransactionEntity
import org.json.JSONArray
import org.json.JSONObject

object BackupSerializer {

    fun serializeEnvelope(envelope: BackupEnvelope): String {
        val root = JSONObject()
        root.put("formatIdentifier", envelope.formatIdentifier)
        root.put("schemaVersion", envelope.schemaVersion)
        root.put("appVersion", envelope.appVersion)
        root.put("createdAt", envelope.createdAt)

        val counts = JSONObject()
        counts.put("cards", envelope.recordCounts.cards)
        counts.put("transactions", envelope.recordCounts.transactions)
        counts.put("attendance", envelope.recordCounts.attendance)
        counts.put("configs", envelope.recordCounts.configs)
        counts.put("logs", envelope.recordCounts.logs)
        root.put("recordCounts", counts)

        val enc = JSONObject()
        enc.put("algorithm", envelope.encryption.algorithm)
        enc.put("keyDerivation", envelope.encryption.keyDerivation)
        enc.put("iterations", envelope.encryption.iterations)
        enc.put("salt", envelope.encryption.salt)
        enc.put("nonce", envelope.encryption.nonce)
        root.put("encryption", enc)

        root.put("ciphertext", envelope.ciphertext)
        root.put("authenticationTag", envelope.authenticationTag)

        return root.toString(2)
    }

    fun deserializeEnvelope(jsonStr: String): BackupEnvelope {
        return try {
            val root = JSONObject(jsonStr)

            val countsObj = root.optJSONObject("recordCounts") ?: JSONObject()
            val counts = BackupRecordCounts(
                cards = countsObj.optInt("cards", 0),
                transactions = countsObj.optInt("transactions", 0),
                attendance = countsObj.optInt("attendance", 0),
                configs = countsObj.optInt("configs", 0),
                logs = countsObj.optInt("logs", 0)
            )

            val encObj = root.getJSONObject("encryption")
            val enc = EncryptionMetadata(
                algorithm = encObj.optString("algorithm", "AES-256-GCM"),
                keyDerivation = encObj.optString("keyDerivation", "PBKDF2-HMAC-SHA-256"),
                iterations = encObj.optInt("iterations", 10000),
                salt = encObj.getString("salt"),
                nonce = encObj.getString("nonce")
            )

            BackupEnvelope(
                formatIdentifier = root.optString("formatIdentifier", ""),
                schemaVersion = root.optInt("schemaVersion", 1),
                appVersion = root.optString("appVersion", "1.0.0"),
                createdAt = root.optLong("createdAt", System.currentTimeMillis()),
                recordCounts = counts,
                encryption = enc,
                ciphertext = root.getString("ciphertext"),
                authenticationTag = root.getString("authenticationTag")
            )
        } catch (e: Exception) {
            throw BackupException.InvalidFormat("Failed to parse backup envelope JSON: ${e.localizedMessage}")
        }
    }

    fun serializePayload(payload: BackupPayload): String {
        val root = JSONObject()

        // Cards
        val cardsArr = JSONArray()
        payload.cards.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("uid", c.uid)
            obj.put("nickname", c.nickname)
            obj.put("manufacturer", c.manufacturer)
            obj.put("technology", c.technology)
            obj.put("cardStandard", c.cardStandard)
            obj.put("timestamp", c.timestamp)
            obj.put("isFavorite", c.isFavorite)
            obj.put("isArchived", c.isArchived)
            obj.put("notes", c.notes)
            obj.put("rawDataJson", c.rawDataJson)
            obj.put("encryptedData", c.encryptedData ?: JSONObject.NULL)
            cardsArr.put(obj)
        }
        root.put("cards", cardsArr)

        // Transactions
        val txArr = JSONArray()
        payload.transactions.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("cardId", t.cardId)
            obj.put("cardNickname", t.cardNickname)
            obj.put("cardUid", t.cardUid)
            obj.put("timestamp", t.timestamp)
            obj.put("usageType", t.usageType)
            obj.put("purposeOrAction", t.purposeOrAction)
            obj.put("status", t.status)
            obj.put("terminalLocation", t.terminalLocation)
            txArr.put(obj)
        }
        root.put("transactions", txArr)

        // Attendance
        val attArr = JSONArray()
        payload.attendanceRecords.forEach { a ->
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("studentId", a.studentId)
            obj.put("studentName", a.studentName)
            obj.put("timestamp", a.timestamp)
            obj.put("usageType", a.usageType)
            obj.put("status", a.status)
            obj.put("providerType", a.providerType)
            obj.put("syncStatus", a.syncStatus)
            obj.put("location", a.location)
            obj.put("deviceId", a.deviceId)
            obj.put("campusId", a.campusId)
            obj.put("cardUid", a.cardUid)
            obj.put("cardNickname", a.cardNickname)
            obj.put("rawPayload", a.rawPayload)
            obj.put("retryCount", a.retryCount)
            obj.put("lastSyncError", a.lastSyncError ?: JSONObject.NULL)
            attArr.put(obj)
        }
        root.put("attendanceRecords", attArr)

        // Attendance Configs
        val configArr = JSONArray()
        payload.attendanceConfigs.forEach { cfg ->
            val obj = JSONObject()
            obj.put("id", cfg.id)
            obj.put("serverUrl", cfg.serverUrl)
            obj.put("port", cfg.port)
            obj.put("useHttps", cfg.useHttps)
            obj.put("authType", cfg.authType)
            obj.put("apiKey", cfg.apiKey)
            obj.put("authToken", cfg.authToken)
            obj.put("tokenExpiryTimestamp", cfg.tokenExpiryTimestamp)
            obj.put("username", cfg.username)
            obj.put("organizationId", cfg.organizationId)
            obj.put("campusId", cfg.campusId)
            obj.put("deviceId", cfg.deviceId)
            obj.put("providerType", cfg.providerType)
            obj.put("autoSyncEnabled", cfg.autoSyncEnabled)
            obj.put("syncIntervalMinutes", cfg.syncIntervalMinutes)
            obj.put("userRole", cfg.userRole)
            obj.put("activeStudentId", cfg.activeStudentId)
            obj.put("activeStudentName", cfg.activeStudentName)
            obj.put("department", cfg.department)
            obj.put("email", cfg.email)
            obj.put("linkedCardUid", cfg.linkedCardUid)
            obj.put("linkedCardNickname", cfg.linkedCardNickname)
            obj.put("isConnected", cfg.isConnected)
            obj.put("lastTestTimestamp", cfg.lastTestTimestamp)
            obj.put("lastSyncTimestamp", cfg.lastSyncTimestamp)
            configArr.put(obj)
        }
        root.put("attendanceConfigs", configArr)

        // Attendance Logs
        val logsArr = JSONArray()
        payload.attendanceLogs.forEach { l ->
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("timestamp", l.timestamp)
            obj.put("logType", l.logType)
            obj.put("provider", l.provider)
            obj.put("message", l.message)
            obj.put("details", l.details)
            obj.put("statusCode", l.statusCode)
            obj.put("isSuccess", l.isSuccess)
            logsArr.put(obj)
        }
        root.put("attendanceLogs", logsArr)

        // Settings metadata
        val settingsObj = JSONObject()
        payload.settingsMetadata.forEach { (k, v) ->
            settingsObj.put(k, v)
        }
        root.put("settingsMetadata", settingsObj)

        return root.toString()
    }

    fun deserializePayload(jsonStr: String): BackupPayload {
        return try {
            val root = JSONObject(jsonStr)

            // Cards
            val cardsList = mutableListOf<CardEntity>()
            val cardsArr = root.optJSONArray("cards") ?: JSONArray()
            for (i in 0 until cardsArr.length()) {
                val obj = cardsArr.getJSONObject(i)
                cardsList.add(
                    CardEntity(
                        id = obj.optLong("id", 0L),
                        uid = obj.getString("uid"),
                        nickname = obj.optString("nickname", ""),
                        manufacturer = obj.optString("manufacturer", "Unknown"),
                        technology = obj.optString("technology", "NFC"),
                        cardStandard = obj.optString("cardStandard", "ISO/IEC 14443-A"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        notes = obj.optString("notes", ""),
                        rawDataJson = obj.optString("rawDataJson", "{}"),
                        encryptedData = if (obj.isNull("encryptedData")) null else obj.optString("encryptedData")
                    )
                )
            }

            // Transactions
            val txList = mutableListOf<UsageTransactionEntity>()
            val txArr = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until txArr.length()) {
                val obj = txArr.getJSONObject(i)
                txList.add(
                    UsageTransactionEntity(
                        id = obj.optLong("id", 0L),
                        cardId = obj.optLong("cardId", 0L),
                        cardNickname = obj.optString("cardNickname", "NFC Card"),
                        cardUid = obj.optString("cardUid", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        usageType = obj.optString("usageType", "Usage"),
                        purposeOrAction = obj.optString("purposeOrAction", "Action"),
                        status = obj.optString("status", "SUCCESS"),
                        terminalLocation = obj.optString("terminalLocation", "NFC Terminal")
                    )
                )
            }

            // Attendance
            val attList = mutableListOf<AttendanceEntity>()
            val attArr = root.optJSONArray("attendanceRecords") ?: JSONArray()
            for (i in 0 until attArr.length()) {
                val obj = attArr.getJSONObject(i)
                attList.add(
                    AttendanceEntity(
                        id = obj.optLong("id", 0L),
                        studentId = obj.optString("studentId", ""),
                        studentName = obj.optString("studentName", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        usageType = obj.optString("usageType", "PUNCH_IN"),
                        status = obj.optString("status", "PRESENT"),
                        providerType = obj.optString("providerType", "JKTECO"),
                        syncStatus = obj.optString("syncStatus", "SYNCED"),
                        location = obj.optString("location", ""),
                        deviceId = obj.optString("deviceId", ""),
                        campusId = obj.optString("campusId", ""),
                        cardUid = obj.optString("cardUid", ""),
                        cardNickname = obj.optString("cardNickname", ""),
                        rawPayload = obj.optString("rawPayload", ""),
                        retryCount = obj.optInt("retryCount", 0),
                        lastSyncError = if (obj.isNull("lastSyncError")) null else obj.optString("lastSyncError")
                    )
                )
            }

            // Attendance Configs
            val configList = mutableListOf<AttendanceConfigEntity>()
            val configArr = root.optJSONArray("attendanceConfigs") ?: JSONArray()
            for (i in 0 until configArr.length()) {
                val obj = configArr.getJSONObject(i)
                configList.add(
                    AttendanceConfigEntity(
                        id = obj.optInt("id", 1),
                        serverUrl = obj.optString("serverUrl", ""),
                        port = obj.optInt("port", 443),
                        useHttps = obj.optBoolean("useHttps", true),
                        authType = obj.optString("authType", "API_KEY"),
                        apiKey = obj.optString("apiKey", ""),
                        authToken = obj.optString("authToken", ""),
                        tokenExpiryTimestamp = obj.optLong("tokenExpiryTimestamp", 0L),
                        username = obj.optString("username", ""),
                        organizationId = obj.optString("organizationId", ""),
                        campusId = obj.optString("campusId", ""),
                        deviceId = obj.optString("deviceId", ""),
                        providerType = obj.optString("providerType", "JKTECO"),
                        autoSyncEnabled = obj.optBoolean("autoSyncEnabled", false),
                        syncIntervalMinutes = obj.optInt("syncIntervalMinutes", 15),
                        userRole = obj.optString("userRole", "STUDENT"),
                        activeStudentId = obj.optString("activeStudentId", ""),
                        activeStudentName = obj.optString("activeStudentName", ""),
                        department = obj.optString("department", ""),
                        email = obj.optString("email", ""),
                        linkedCardUid = obj.optString("linkedCardUid", ""),
                        linkedCardNickname = obj.optString("linkedCardNickname", ""),
                        isConnected = obj.optBoolean("isConnected", false),
                        lastTestTimestamp = obj.optLong("lastTestTimestamp", 0L),
                        lastSyncTimestamp = obj.optLong("lastSyncTimestamp", 0L)
                    )
                )
            }

            // Attendance Logs
            val logsList = mutableListOf<AttendanceLogEntity>()
            val logsArr = root.optJSONArray("attendanceLogs") ?: JSONArray()
            for (i in 0 until logsArr.length()) {
                val obj = logsArr.getJSONObject(i)
                logsList.add(
                    AttendanceLogEntity(
                        id = obj.optLong("id", 0L),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        logType = obj.optString("logType", "ACTIVITY"),
                        provider = obj.optString("provider", "JKTECO"),
                        message = obj.optString("message", ""),
                        details = obj.optString("details", ""),
                        statusCode = obj.optInt("statusCode", 200),
                        isSuccess = obj.optBoolean("isSuccess", true)
                    )
                )
            }

            val settingsMap = mutableMapOf<String, String>()
            val settingsObj = root.optJSONObject("settingsMetadata")
            settingsObj?.keys()?.forEach { k ->
                settingsMap[k] = settingsObj.getString(k)
            }

            BackupPayload(
                cards = cardsList,
                transactions = txList,
                attendanceRecords = attList,
                attendanceConfigs = configList,
                attendanceLogs = logsList,
                settingsMetadata = settingsMap
            )
        } catch (e: Exception) {
            throw BackupException.CorruptedPayload(e)
        }
    }
}
