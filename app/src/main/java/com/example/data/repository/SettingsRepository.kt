package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AuthMethods {
    const val FINGERPRINT_PIN = "FINGERPRINT_PIN"
    const val FINGERPRINT_PASSWORD = "FINGERPRINT_PASSWORD"
    const val PIN_ONLY = "PIN_ONLY"
    const val PASSWORD_ONLY = "PASSWORD_ONLY"

    fun normalize(raw: String?): String = when (raw?.uppercase(Locale.US)) {
        "FINGERPRINT_PIN", "BIOMETRIC_PIN", "FINGERPRINT" -> FINGERPRINT_PIN
        "FINGERPRINT_PASSWORD", "BIOMETRIC_PASSWORD" -> FINGERPRINT_PASSWORD
        "PIN_ONLY", "PIN" -> PIN_ONLY
        "PASSWORD_ONLY", "PASSWORD" -> PASSWORD_ONLY
        else -> FINGERPRINT_PIN
    }

    fun usesBiometric(method: String): Boolean {
        val norm = normalize(method)
        return norm == FINGERPRINT_PIN || norm == FINGERPRINT_PASSWORD
    }

    fun usesPin(method: String): Boolean {
        val norm = normalize(method)
        return norm == FINGERPRINT_PIN || norm == PIN_ONLY
    }

    fun usesPassword(method: String): Boolean {
        val norm = normalize(method)
        return norm == FINGERPRINT_PASSWORD || norm == PASSWORD_ONLY
    }

    fun displayLabel(method: String): String = when (normalize(method)) {
        FINGERPRINT_PIN -> "Fingerprint + PIN (Recommended)"
        FINGERPRINT_PASSWORD -> "Fingerprint + Password"
        PIN_ONLY -> "PIN Only (4–6 Digits)"
        PASSWORD_ONLY -> "Master Password (8+ Chars)"
        else -> "Fingerprint + PIN (Recommended)"
    }
}

data class AppSettings(
    val enableVibration: Boolean = true,
    val enableSound: Boolean = true,
    val autoExportOnScan: Boolean = false,
    val isDarkMode: Boolean = false,
    val isDeveloperMode: Boolean = false,
    val activeCardId: Long? = null,
    val isBiometricEnabled: Boolean = false,
    val hasSecurityPin: Boolean = false,
    val hasSecurityPassword: Boolean = false,
    val hasSecurityAnswer: Boolean = false,
    val securityQuestion: String = "",
    val requireAuthForBackupRestore: Boolean = true,
    val isAppLockEnabled: Boolean = false,
    val authMethod: String = AuthMethods.FINGERPRINT_PIN,
    val autoLockTimeoutSeconds: Int = 30,
    val failedAuthAttempts: Int = 0,
    val lockoutUntilWallMs: Long = 0L
) {
    val hasAnyCredential: Boolean
        get() = hasSecurityPin || hasSecurityPassword
}

class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "campus_nfc_prefs"
        private const val PBKDF2_PREFIX = "pbkdf2"
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 120_000
        private const val PBKDF2_KEY_BITS = 256
        private const val SALT_BYTES = 16
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 6
        const val MIN_PASSWORD_LENGTH = 8
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        migrateLegacyPlaintextRecoveryAnswerIfNeeded()
    }

    private fun migrateLegacyPlaintextRecoveryAnswerIfNeeded() {
        val legacyAnswer = prefs.getString("security_answer", "") ?: ""
        val hashedAnswer = prefs.getString("security_answer_hash", "") ?: ""
        if (legacyAnswer.isNotBlank() && hashedAnswer.isBlank()) {
            val normalized = legacyAnswer.trim().lowercase(Locale.US)
            val newHash = hashWithPbkdf2(normalized)
            prefs.edit()
                .putString("security_answer_hash", newHash)
                .remove("security_answer")
                .apply()
            _settings.value = loadSettings()
        } else if (legacyAnswer.isNotBlank()) {
            prefs.edit().remove("security_answer").apply()
        }
    }

    private fun hashWithPbkdf2(input: String): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        val derived = derivePbkdf2(input, salt, PBKDF2_ITERATIONS)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(derived, Base64.NO_WRAP)
        return "$PBKDF2_PREFIX\$$PBKDF2_ITERATIONS\$$saltB64\$$hashB64"
    }

    private fun derivePbkdf2(input: String, salt: ByteArray, iterations: Int): ByteArray {
        val chars = input.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, PBKDF2_KEY_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun verifyStoredHash(input: String, storedValue: String): Pair<Boolean, Boolean> {
        if (storedValue.isBlank() || input.isEmpty()) return Pair(false, false)

        if (storedValue.startsWith("$PBKDF2_PREFIX\$")) {
            val parts = storedValue.split("$")
            if (parts.size != 4) return Pair(false, false)
            val iterations = parts[1].toIntOrNull() ?: return Pair(false, false)
            if (iterations !in 10_000..1_000_000) return Pair(false, false)
            return try {
                val salt = Base64.decode(parts[2], Base64.NO_WRAP)
                val expectedHash = Base64.decode(parts[3], Base64.NO_WRAP)
                val actualHash = derivePbkdf2(input, salt, iterations)
                Pair(MessageDigest.isEqual(expectedHash, actualHash), false)
            } catch (e: Exception) {
                Pair(false, false)
            }
        }

        // Legacy single-SHA-256 verification for existing installs; signals upgrade needed
        val legacySalt = prefs.getString("security_salt", "") ?: ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val candidateHex = digest
                .digest((legacySalt + input).toByteArray(Charsets.UTF_8))
                .joinToString("") { String.format(Locale.US, "%02x", it) }
            val matched = MessageDigest.isEqual(
                storedValue.toByteArray(Charsets.UTF_8),
                candidateHex.toByteArray(Charsets.UTF_8)
            )
            Pair(matched, matched)
        } catch (e: Exception) {
            Pair(false, false)
        }
    }

    private fun loadSettings(): AppSettings {
        val savedId = prefs.getLong("active_card_id", -1L)
        val hasPin = !(prefs.getString("security_pin", "").isNullOrBlank())
        val hasPassword = !(prefs.getString("security_password", "").isNullOrBlank())
        val hasAnswer = !(prefs.getString("security_answer_hash", "").isNullOrBlank()) ||
            !(prefs.getString("security_answer", "").isNullOrBlank())
        val normalizedMethod = AuthMethods.normalize(prefs.getString("auth_method", AuthMethods.FINGERPRINT_PIN))
        val rawLockEnabled = prefs.getBoolean("app_lock_enabled", false)
        // Guard against impossible lock state with zero credentials configured
        val effectiveLockEnabled = rawLockEnabled && (hasPin || hasPassword)

        return AppSettings(
            enableVibration = prefs.getBoolean("enable_vibration", true),
            enableSound = prefs.getBoolean("enable_sound", true),
            autoExportOnScan = prefs.getBoolean("auto_export", false),
            isDarkMode = prefs.getBoolean("dark_mode", false),
            isDeveloperMode = prefs.getBoolean("dev_mode", false),
            activeCardId = if (savedId != -1L) savedId else null,
            isBiometricEnabled = prefs.getBoolean("biometric_enabled", false),
            hasSecurityPin = hasPin,
            hasSecurityPassword = hasPassword,
            hasSecurityAnswer = hasAnswer,
            securityQuestion = prefs.getString("security_question", "") ?: "",
            requireAuthForBackupRestore = prefs.getBoolean("require_auth_backup", true),
            isAppLockEnabled = effectiveLockEnabled,
            authMethod = normalizedMethod,
            autoLockTimeoutSeconds = prefs.getInt("auto_lock_timeout_seconds", 30),
            failedAuthAttempts = prefs.getInt("failed_auth_attempts", 0),
            lockoutUntilWallMs = prefs.getLong("lockout_until_wall_ms", 0L)
        )
    }

    fun updateAutoLockTimeoutSeconds(seconds: Int) {
        val safeSeconds = seconds.coerceAtLeast(0)
        prefs.edit().putInt("auto_lock_timeout_seconds", safeSeconds).apply()
        _settings.value = _settings.value.copy(autoLockTimeoutSeconds = safeSeconds)
    }

    fun updateAuthMethod(method: String) {
        val normalized = AuthMethods.normalize(method)
        prefs.edit().putString("auth_method", normalized).apply()
        _settings.value = _settings.value.copy(authMethod = normalized)
    }

    fun updateAppLockEnabled(enabled: Boolean) {
        val canEnable = !enabled || _settings.value.hasAnyCredential
        if (!canEnable) return
        prefs.edit().putBoolean("app_lock_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isAppLockEnabled = enabled)
    }

    fun setSecurityPassword(password: String) {
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "Password must be at least $MIN_PASSWORD_LENGTH characters."
        }
        val hashed = hashWithPbkdf2(password)
        prefs.edit().putString("security_password", hashed).apply()
        _settings.value = _settings.value.copy(hasSecurityPassword = true)
    }

    fun verifyPassword(inputPassword: String): Boolean {
        if (inputPassword.isBlank()) return false
        val storedPassword = prefs.getString("security_password", "") ?: ""
        if (storedPassword.isBlank()) return false
        val (matched, needsUpgrade) = verifyStoredHash(inputPassword, storedPassword)
        if (matched && needsUpgrade) {
            prefs.edit().putString("security_password", hashWithPbkdf2(inputPassword)).apply()
        }
        return matched
    }

    fun updateActiveCardId(id: Long?) {
        if (id == null) {
            prefs.edit().remove("active_card_id").apply()
        } else {
            prefs.edit().putLong("active_card_id", id).apply()
        }
        _settings.value = _settings.value.copy(activeCardId = id)
    }

    fun updateVibration(enabled: Boolean) {
        prefs.edit().putBoolean("enable_vibration", enabled).apply()
        _settings.value = _settings.value.copy(enableVibration = enabled)
    }

    fun updateSound(enabled: Boolean) {
        prefs.edit().putBoolean("enable_sound", enabled).apply()
        _settings.value = _settings.value.copy(enableSound = enabled)
    }

    fun updateAutoExport(enabled: Boolean) {
        prefs.edit().putBoolean("auto_export", enabled).apply()
        _settings.value = _settings.value.copy(autoExportOnScan = enabled)
    }

    fun updateDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        _settings.value = _settings.value.copy(isDarkMode = enabled)
    }

    fun updateDeveloperMode(enabled: Boolean) {
        prefs.edit().putBoolean("dev_mode", enabled).apply()
        _settings.value = _settings.value.copy(isDeveloperMode = enabled)
    }

    fun setSecurityQuestionAndAnswer(question: String, answer: String) {
        val cleanedQuestion = question.trim()
        val cleanedAnswer = answer.trim().lowercase(Locale.US)
        require(cleanedQuestion.isNotBlank() && cleanedAnswer.isNotBlank()) {
            "Recovery question and answer must not be blank."
        }
        val hashedAnswer = hashWithPbkdf2(cleanedAnswer)
        prefs.edit()
            .putString("security_question", cleanedQuestion)
            .putString("security_answer_hash", hashedAnswer)
            .remove("security_answer")
            .apply()
        _settings.value = _settings.value.copy(
            securityQuestion = cleanedQuestion,
            hasSecurityAnswer = true
        )
    }

    fun setSecurityPinAndQuestion(pin: String, question: String, answer: String) {
        require(pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }) {
            "PIN must be $MIN_PIN_LENGTH–$MAX_PIN_LENGTH digits."
        }
        val hashedPin = hashWithPbkdf2(pin)
        val editor = prefs.edit().putString("security_pin", hashedPin)

        val cleanedQuestion = question.trim()
        val cleanedAnswer = answer.trim().lowercase(Locale.US)
        val updatedHasAnswer = if (cleanedQuestion.isNotBlank() && cleanedAnswer.isNotBlank()) {
            editor
                .putString("security_question", cleanedQuestion)
                .putString("security_answer_hash", hashWithPbkdf2(cleanedAnswer))
                .remove("security_answer")
            true
        } else {
            _settings.value.hasSecurityAnswer
        }
        editor.apply()

        _settings.value = _settings.value.copy(
            hasSecurityPin = true,
            securityQuestion = if (cleanedQuestion.isNotBlank() && cleanedAnswer.isNotBlank()) cleanedQuestion else _settings.value.securityQuestion,
            hasSecurityAnswer = updatedHasAnswer
        )
    }

    fun updateBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isBiometricEnabled = enabled)
    }

    fun updateRequireAuthForBackupRestore(enabled: Boolean) {
        prefs.edit().putBoolean("require_auth_backup", enabled).apply()
        _settings.value = _settings.value.copy(requireAuthForBackupRestore = enabled)
    }

    fun clearSecurityPin() {
        val current = _settings.value
        val hasPassword = current.hasSecurityPassword
        val newMethod = if (AuthMethods.usesPin(current.authMethod)) {
            if (hasPassword) {
                if (AuthMethods.usesBiometric(current.authMethod)) AuthMethods.FINGERPRINT_PASSWORD else AuthMethods.PASSWORD_ONLY
            } else {
                AuthMethods.FINGERPRINT_PIN
            }
        } else {
            current.authMethod
        }
        val newLockEnabled = current.isAppLockEnabled && hasPassword

        val editor = prefs.edit()
            .remove("security_pin")
            .putString("auth_method", newMethod)
            .putBoolean("app_lock_enabled", newLockEnabled)

        if (!hasPassword) {
            editor
                .remove("security_question")
                .remove("security_answer")
                .remove("security_answer_hash")
                .putBoolean("biometric_enabled", false)
        }
        editor.apply()

        _settings.value = current.copy(
            hasSecurityPin = false,
            securityQuestion = if (hasPassword) current.securityQuestion else "",
            hasSecurityAnswer = if (hasPassword) current.hasSecurityAnswer else false,
            isBiometricEnabled = if (hasPassword) current.isBiometricEnabled else false,
            authMethod = newMethod,
            isAppLockEnabled = newLockEnabled
        )
    }

    fun verifyPin(inputPin: String): Boolean {
        if (inputPin.isBlank()) return false
        val storedPin = prefs.getString("security_pin", "") ?: ""
        if (storedPin.isBlank()) return false
        val (matched, needsUpgrade) = verifyStoredHash(inputPin, storedPin)
        if (matched && needsUpgrade) {
            prefs.edit().putString("security_pin", hashWithPbkdf2(inputPin)).apply()
        }
        return matched
    }

    fun verifySecretAnswer(inputAnswer: String): Boolean {
        val cleanedInput = inputAnswer.trim().lowercase(Locale.US)
        if (cleanedInput.isBlank()) return false
        val storedHash = prefs.getString("security_answer_hash", "") ?: ""
        if (storedHash.isBlank()) return false
        val (matched, _) = verifyStoredHash(cleanedInput, storedHash)
        return matched
    }

    fun getRemainingLockoutSeconds(nowWallMs: Long = System.currentTimeMillis()): Int {
        val until = _settings.value.lockoutUntilWallMs
        if (until <= nowWallMs) return 0
        return (((until - nowWallMs) + 999L) / 1000L).toInt().coerceAtLeast(0)
    }

    fun recordFailedAuthAttempt(nowWallMs: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val nextCount = _settings.value.failedAuthAttempts + 1
        return if (nextCount >= MAX_FAILED_ATTEMPTS) {
            val lockoutUntil = nowWallMs + LOCKOUT_DURATION_MS
            prefs.edit()
                .putInt("failed_auth_attempts", 0)
                .putLong("lockout_until_wall_ms", lockoutUntil)
                .apply()
            _settings.value = _settings.value.copy(
                failedAuthAttempts = 0,
                lockoutUntilWallMs = lockoutUntil
            )
            Pair(0, (LOCKOUT_DURATION_MS / 1000L).toInt())
        } else {
            prefs.edit().putInt("failed_auth_attempts", nextCount).apply()
            _settings.value = _settings.value.copy(failedAuthAttempts = nextCount)
            Pair(MAX_FAILED_ATTEMPTS - nextCount, 0)
        }
    }

    fun resetFailedAuthAttempts() {
        prefs.edit()
            .putInt("failed_auth_attempts", 0)
            .putLong("lockout_until_wall_ms", 0L)
            .apply()
        _settings.value = _settings.value.copy(
            failedAuthAttempts = 0,
            lockoutUntilWallMs = 0L
        )
    }
}
