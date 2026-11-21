package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.backup.model.BackupPayload
import com.example.backup.serializer.BackupSerializer
import com.example.data.local.entity.CardEntity
import com.example.data.repository.AuthMethods
import com.example.data.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityAndBackupRobolectricTest {

    @Test
    fun authMethodsNormalizationAndHelpers() {
        assertEquals(AuthMethods.FINGERPRINT_PIN, AuthMethods.normalize("FINGERPRINT"))
        assertEquals(AuthMethods.FINGERPRINT_PIN, AuthMethods.normalize("BIOMETRIC_PIN"))
        assertEquals(AuthMethods.FINGERPRINT_PASSWORD, AuthMethods.normalize("BIOMETRIC_PASSWORD"))
        assertEquals(AuthMethods.PIN_ONLY, AuthMethods.normalize("PIN"))
        assertEquals(AuthMethods.PASSWORD_ONLY, AuthMethods.normalize("PASSWORD"))

        assertTrue(AuthMethods.usesBiometric("FINGERPRINT_PIN"))
        assertTrue(AuthMethods.usesBiometric("FINGERPRINT_PASSWORD"))
        assertFalse(AuthMethods.usesBiometric("PIN_ONLY"))
        assertFalse(AuthMethods.usesBiometric("PASSWORD_ONLY"))

        assertTrue(AuthMethods.usesPin("FINGERPRINT_PIN"))
        assertTrue(AuthMethods.usesPin("PIN_ONLY"))
        assertFalse(AuthMethods.usesPin("PASSWORD_ONLY"))

        assertTrue(AuthMethods.usesPassword("FINGERPRINT_PASSWORD"))
        assertTrue(AuthMethods.usesPassword("PASSWORD_ONLY"))
        assertFalse(AuthMethods.usesPassword("PIN_ONLY"))
    }

    @Test
    fun settingsRepository_rejectsBlankCredentialsAndHashesPinPasswordAndRecovery() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SettingsRepository(context)

        // Fresh state: no credentials configured -> blank inputs MUST return false (no bypass!)
        assertFalse(repo.verifyPin(""))
        assertFalse(repo.verifyPin("1234"))
        assertFalse(repo.verifyPassword(""))
        assertFalse(repo.verifySecretAnswer(""))

        // Configure PIN + recovery question
        repo.setSecurityPinAndQuestion("258099", "First pet?", "Milo")
        assertTrue(repo.settings.value.hasSecurityPin)
        assertTrue(repo.settings.value.hasSecurityAnswer)
        assertTrue(repo.verifyPin("258099"))
        assertFalse(repo.verifyPin("258098"))
        assertTrue(repo.verifySecretAnswer("  milo "))
        assertFalse(repo.verifySecretAnswer("wrong"))

        // Configure password
        repo.setSecurityPassword("CampusVault#2026")
        assertTrue(repo.settings.value.hasSecurityPassword)
        assertTrue(repo.verifyPassword("CampusVault#2026"))
        assertFalse(repo.verifyPassword("wrongpassword"))

        // Persistent lockout after 5 failed attempts
        repo.resetFailedAuthAttempts()
        repeat(4) {
            val (_, lockSecs) = repo.recordFailedAuthAttempt()
            assertEquals(0, lockSecs)
        }
        val (_, fifthLockSecs) = repo.recordFailedAuthAttempt()
        assertTrue("5th failed attempt must trigger lockout", fifthLockSecs > 0)
        assertTrue(repo.getRemainingLockoutSeconds() > 0)

        repo.resetFailedAuthAttempts()
        assertEquals(0, repo.getRemainingLockoutSeconds())
    }

    @Test
    fun backupSerializer_payloadRoundtripAndSafeEmptyDefaults() {
        val payload = BackupPayload(
            cards = listOf(
                CardEntity(
                    id = 42L,
                    uid = "04:AA:BB:CC:DD:EE:FF",
                    nickname = "Library Pass",
                    manufacturer = "NXP Semiconductors",
                    technology = "MIFARE DESFire",
                    cardStandard = "ISO/IEC 14443-4",
                    timestamp = 1700000000000L,
                    isFavorite = true,
                    isArchived = false,
                    notes = "Main entrance",
                    rawDataJson = """{"uid":"04:AA:BB:CC:DD:EE:FF"}""",
                    encryptedData = null
                )
            ),
            attendanceConfigs = listOf(AttendanceConfigEntity())
        )

        val json = BackupSerializer.serializePayload(payload)
        val restored = BackupSerializer.deserializePayload(json)

        assertEquals(1, restored.cards.size)
        assertEquals("Library Pass", restored.cards[0].nickname)
        assertEquals("04:AA:BB:CC:DD:EE:FF", restored.cards[0].uid)
        assertEquals(1, restored.attendanceConfigs.size)
        // Verify default config has no hardcoded credentials or fake connected state
        assertEquals("", restored.attendanceConfigs[0].apiKey)
        assertEquals("", restored.attendanceConfigs[0].serverUrl)
        assertFalse(restored.attendanceConfigs[0].isConnected)
    }
}
