package com.example.attendance.service

import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.security.CryptoProvider
import java.net.HttpURLConnection

/**
 * Handles Android Keystore encryption/decryption for attendance server credentials
 * and builds HTTP authorization headers for configured attendance providers.
 */
class AuthenticationService(private val cryptoProvider: CryptoProvider) {

    fun encryptConfigSecrets(config: AttendanceConfigEntity): AttendanceConfigEntity {
        val encKey = if (config.apiKey.isNotBlank()) {
            cryptoProvider.encrypt(config.apiKey)
        } else {
            ""
        }
        val encToken = if (config.authToken.isNotBlank()) {
            cryptoProvider.encrypt(config.authToken)
        } else {
            ""
        }
        return config.copy(apiKey = encKey, authToken = encToken)
    }

    fun decryptConfigSecrets(config: AttendanceConfigEntity): AttendanceConfigEntity {
        val plainKey = if (config.apiKey.isNotBlank()) {
            runCatching { cryptoProvider.decrypt(config.apiKey) }.getOrDefault(config.apiKey)
        } else {
            ""
        }
        val plainToken = if (config.authToken.isNotBlank()) {
            runCatching { cryptoProvider.decrypt(config.authToken) }.getOrDefault(config.authToken)
        } else {
            ""
        }
        return config.copy(apiKey = plainKey, authToken = plainToken)
    }

    fun applyAuthHeaders(connection: HttpURLConnection, config: AttendanceConfigEntity) {
        when (config.authType.uppercase()) {
            "API_KEY" -> {
                if (config.apiKey.isNotBlank()) {
                    connection.setRequestProperty("X-API-KEY", config.apiKey)
                }
            }
            "JWT", "OAUTH2" -> {
                val token = config.authToken.ifBlank { config.apiKey }
                if (token.isNotBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }
            }
            "SESSION" -> {
                val token = config.authToken.ifBlank { config.apiKey }
                if (token.isNotBlank()) {
                    connection.setRequestProperty("X-Session-Token", token)
                }
            }
            else -> {
                if (config.apiKey.isNotBlank()) {
                    connection.setRequestProperty("X-API-KEY", config.apiKey)
                }
            }
        }
    }

    fun hasValidCredentials(config: AttendanceConfigEntity): Boolean {
        return when (config.authType.uppercase()) {
            "API_KEY" -> config.apiKey.isNotBlank()
            "JWT", "OAUTH2", "SESSION" -> {
                val hasToken = config.authToken.isNotBlank() || config.apiKey.isNotBlank()
                val notExpired = config.tokenExpiryTimestamp == 0L || config.tokenExpiryTimestamp > System.currentTimeMillis()
                hasToken && notExpired
            }
            else -> true
        }
    }
}
