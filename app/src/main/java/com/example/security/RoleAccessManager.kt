package com.example.security

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

object CampusRoles {
    const val STUDENT = "STUDENT"
    const val TEACHER = "TEACHER"
    const val ADMIN = "ADMIN"

    fun normalize(raw: String?): String = when (raw?.trim()?.uppercase(Locale.US)) {
        ADMIN, "ADMINISTRATOR", "SUPERADMIN" -> ADMIN
        TEACHER, "FACULTY", "INSTRUCTOR", "STAFF" -> TEACHER
        else -> STUDENT
    }
}

enum class RoleSessionSource {
    LOCAL_STUDENT,
    LOCAL_PASSCODE,
    SERVER_VERIFIED
}

data class RoleAccessState(
    val activeRole: String = CampusRoles.STUDENT,
    val sessionSource: RoleSessionSource = RoleSessionSource.LOCAL_STUDENT,
    val authenticatedSubject: String = "",
    val hasAdminPasscode: Boolean = false,
    val hasTeacherPasscode: Boolean = false,
    val failedAttempts: Int = 0,
    val lockoutUntilWallMs: Long = 0L
) {
    val isStudent: Boolean get() = activeRole == CampusRoles.STUDENT
    val isTeacher: Boolean get() = activeRole == CampusRoles.TEACHER
    val isAdmin: Boolean get() = activeRole == CampusRoles.ADMIN
    val canEditAttendanceRecords: Boolean get() = activeRole == CampusRoles.TEACHER || activeRole == CampusRoles.ADMIN
    val canViewAuditLogs: Boolean get() = activeRole == CampusRoles.TEACHER || activeRole == CampusRoles.ADMIN
    val canManageServerAndRoles: Boolean get() = activeRole == CampusRoles.ADMIN
    val canClearLogsOrRecords: Boolean get() = activeRole == CampusRoles.ADMIN
}

sealed class RoleAuthResult {
    data class Success(val role: String) : RoleAuthResult()
    data class InvalidCredentials(val remainingAttempts: Int) : RoleAuthResult()
    data class LockedOut(val remainingSeconds: Int) : RoleAuthResult()
    data class NotConfigured(val message: String) : RoleAuthResult()
}

/**
 * Enforces Role-Based Access Control (RBAC) for Student, Teacher, and Administrator roles.
 *
 * Security guarantees:
 * - Least privilege on cold start: always initializes to STUDENT until a server-verified session
 *   or valid PBKDF2-hashed staff/admin passcode elevates the session.
 * - Passcodes are stored exclusively as salted PBKDF2-HMAC-SHA256 hashes (120,000 iterations)
 *   and verified in constant time.
 * - Persistent 5-attempt / 30-second anti-brute-force lockout survives app restarts.
 */
class RoleAccessManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "campus_role_rbac_prefs"
        private const val KEY_ADMIN_HASH = "admin_passcode_hash"
        private const val KEY_TEACHER_HASH = "teacher_passcode_hash"
        private const val KEY_FAILED_ATTEMPTS = "rbac_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "rbac_lockout_until_wall_ms"

        private const val PBKDF2_PREFIX = "pbkdf2"
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 120_000
        private const val PBKDF2_KEY_BITS = 256
        private const val SALT_BYTES = 16

        const val MIN_ADMIN_PASSCODE_LENGTH = 8
        const val MIN_TEACHER_PASSCODE_LENGTH = 6
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<RoleAccessState> = _state.asStateFlow()

    private fun loadInitialState(): RoleAccessState {
        return RoleAccessState(
            activeRole = CampusRoles.STUDENT,
            sessionSource = RoleSessionSource.LOCAL_STUDENT,
            authenticatedSubject = "",
            hasAdminPasscode = !prefs.getString(KEY_ADMIN_HASH, "").isNullOrBlank(),
            hasTeacherPasscode = !prefs.getString(KEY_TEACHER_HASH, "").isNullOrBlank(),
            failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0),
            lockoutUntilWallMs = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        )
    }

    private fun refreshMetadata(
        activeRole: String = _state.value.activeRole,
        source: RoleSessionSource = _state.value.sessionSource,
        subject: String = _state.value.authenticatedSubject
    ) {
        _state.value = RoleAccessState(
            activeRole = activeRole,
            sessionSource = source,
            authenticatedSubject = subject,
            hasAdminPasscode = !prefs.getString(KEY_ADMIN_HASH, "").isNullOrBlank(),
            hasTeacherPasscode = !prefs.getString(KEY_TEACHER_HASH, "").isNullOrBlank(),
            failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0),
            lockoutUntilWallMs = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        )
    }

    fun getRemainingLockoutSeconds(): Int {
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val remainingMs = until - System.currentTimeMillis()
        return if (remainingMs > 0L) ((remainingMs + 999L) / 1000L).toInt() else 0
    }

    /**
     * First-time Admin setup when no Admin passcode exists yet.
     * Immediately elevates the current session to ADMIN upon valid creation.
     */
    fun initializeAdminPasscode(
        adminPasscode: String,
        teacherPasscode: String? = null
    ): Result<Unit> {
        if (_state.value.hasAdminPasscode && !_state.value.isAdmin) {
            return Result.failure(SecurityException("Administrator access is already initialized. Sign in as Admin to change passcodes."))
        }
        if (adminPasscode.length < MIN_ADMIN_PASSCODE_LENGTH) {
            return Result.failure(IllegalArgumentException("Admin passcode must be at least $MIN_ADMIN_PASSCODE_LENGTH characters."))
        }
        if (!teacherPasscode.isNullOrBlank() && teacherPasscode.length < MIN_TEACHER_PASSCODE_LENGTH) {
            return Result.failure(IllegalArgumentException("Teacher passcode must be at least $MIN_TEACHER_PASSCODE_LENGTH characters."))
        }

        val editor = prefs.edit()
            .putString(KEY_ADMIN_HASH, hashWithPbkdf2(adminPasscode))
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)

        if (!teacherPasscode.isNullOrBlank()) {
            editor.putString(KEY_TEACHER_HASH, hashWithPbkdf2(teacherPasscode))
        }
        editor.apply()

        refreshMetadata(
            activeRole = CampusRoles.ADMIN,
            source = RoleSessionSource.LOCAL_PASSCODE,
            subject = "Local Administrator"
        )
        return Result.success(Unit)
    }

    /**
     * Admin-only: sets or rotates the Teacher passcode.
     */
    fun setTeacherPasscode(newTeacherPasscode: String): Result<Unit> {
        if (!_state.value.canManageServerAndRoles) {
            return Result.failure(SecurityException("Only an authenticated Administrator can manage staff passcodes."))
        }
        if (newTeacherPasscode.length < MIN_TEACHER_PASSCODE_LENGTH) {
            return Result.failure(IllegalArgumentException("Teacher passcode must be at least $MIN_TEACHER_PASSCODE_LENGTH characters."))
        }
        prefs.edit().putString(KEY_TEACHER_HASH, hashWithPbkdf2(newTeacherPasscode)).apply()
        refreshMetadata()
        return Result.success(Unit)
    }

    /**
     * Admin-only: rotates the Administrator passcode after verifying the current one (if locally set).
     */
    fun rotateAdminPasscode(currentAdminPasscode: String, newAdminPasscode: String): Result<Unit> {
        if (!_state.value.canManageServerAndRoles) {
            return Result.failure(SecurityException("Only an authenticated Administrator can change the Admin passcode."))
        }
        if (newAdminPasscode.length < MIN_ADMIN_PASSCODE_LENGTH) {
            return Result.failure(IllegalArgumentException("New Admin passcode must be at least $MIN_ADMIN_PASSCODE_LENGTH characters."))
        }
        val existingHash = prefs.getString(KEY_ADMIN_HASH, "") ?: ""
        if (existingHash.isNotBlank() && !verifyPbkdf2(currentAdminPasscode, existingHash)) {
            return Result.failure(SecurityException("Current Admin passcode is incorrect."))
        }
        prefs.edit().putString(KEY_ADMIN_HASH, hashWithPbkdf2(newAdminPasscode)).apply()
        refreshMetadata()
        return Result.success(Unit)
    }

    /**
     * Elevates session to TEACHER or ADMIN using a locally stored PBKDF2 passcode.
     */
    fun unlockLocalRole(targetRole: String, passcode: String): RoleAuthResult {
        val normalizedTarget = CampusRoles.normalize(targetRole)
        if (normalizedTarget == CampusRoles.STUDENT) {
            lockElevatedSession()
            return RoleAuthResult.Success(CampusRoles.STUDENT)
        }

        val lockoutRemaining = getRemainingLockoutSeconds()
        if (lockoutRemaining > 0) {
            return RoleAuthResult.LockedOut(lockoutRemaining)
        }

        val adminHash = prefs.getString(KEY_ADMIN_HASH, "") ?: ""
        val teacherHash = prefs.getString(KEY_TEACHER_HASH, "") ?: ""

        if (normalizedTarget == CampusRoles.ADMIN && adminHash.isBlank()) {
            return RoleAuthResult.NotConfigured("No Admin passcode is configured yet. Initialize Administrator access first.")
        }
        if (normalizedTarget == CampusRoles.TEACHER && teacherHash.isBlank() && adminHash.isBlank()) {
            return RoleAuthResult.NotConfigured("No Teacher or Admin passcode is configured yet. Ask an Administrator to configure staff access.")
        }
        if (passcode.isBlank()) {
            return recordFailure()
        }

        val verified = when (normalizedTarget) {
            CampusRoles.ADMIN -> verifyPbkdf2(passcode, adminHash)
            CampusRoles.TEACHER -> {
                (teacherHash.isNotBlank() && verifyPbkdf2(passcode, teacherHash)) ||
                    (adminHash.isNotBlank() && verifyPbkdf2(passcode, adminHash))
            }
            else -> false
        }

        return if (verified) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            refreshMetadata(
                activeRole = normalizedTarget,
                source = RoleSessionSource.LOCAL_PASSCODE,
                subject = if (normalizedTarget == CampusRoles.ADMIN) "Local Administrator" else "Verified Teacher"
            )
            RoleAuthResult.Success(normalizedTarget)
        } else {
            recordFailure()
        }
    }

    /**
     * Applies a role authenticated directly by the campus server (/api/v1/auth/login).
     * Students authenticated by the server cannot self-escalate to Teacher/Admin.
     */
    fun applyServerVerifiedSession(serverRole: String, subject: String) {
        val normalized = CampusRoles.normalize(serverRole)
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
        refreshMetadata(
            activeRole = normalized,
            source = RoleSessionSource.SERVER_VERIFIED,
            subject = subject.ifBlank { "Campus Account" }
        )
    }

    /**
     * Immediately drops any Teacher/Admin privilege back to standard Student mode.
     */
    fun lockElevatedSession() {
        refreshMetadata(
            activeRole = CampusRoles.STUDENT,
            source = RoleSessionSource.LOCAL_STUDENT,
            subject = ""
        )
    }

    private fun recordFailure(): RoleAuthResult {
        val nextAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        return if (nextAttempts >= MAX_FAILED_ATTEMPTS) {
            val lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
                .apply()
            refreshMetadata()
            RoleAuthResult.LockedOut((LOCKOUT_DURATION_MS / 1000L).toInt())
        } else {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, nextAttempts).apply()
            refreshMetadata()
            RoleAuthResult.InvalidCredentials((MAX_FAILED_ATTEMPTS - nextAttempts).coerceAtLeast(1))
        }
    }

    private fun hashWithPbkdf2(input: String): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        val derived = derivePbkdf2(input, salt, PBKDF2_ITERATIONS)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(derived, Base64.NO_WRAP)
        return "$PBKDF2_PREFIX:$PBKDF2_ITERATIONS:$saltB64:$hashB64"
    }

    private fun derivePbkdf2(input: String, salt: ByteArray, iterations: Int): ByteArray {
        val chars = input.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, PBKDF2_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun verifyPbkdf2(input: String, stored: String): Boolean {
        if (input.isBlank() || stored.isBlank()) return false
        val parts = stored.split(":")
        if (parts.size != 4 || parts[0] != PBKDF2_PREFIX) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        return try {
            val salt = Base64.decode(parts[2], Base64.NO_WRAP)
            val expectedHash = Base64.decode(parts[3], Base64.NO_WRAP)
            val actualHash = derivePbkdf2(input, salt, iterations)
            MessageDigest.isEqual(actualHash, expectedHash)
        } catch (_: Exception) {
            false
        }
    }
}
