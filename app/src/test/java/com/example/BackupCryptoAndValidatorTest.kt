package com.example

import com.example.backup.crypto.BackupCryptoService
import com.example.backup.model.BackupEnvelope
import com.example.backup.model.BackupException
import com.example.backup.model.BackupRecordCounts
import com.example.backup.model.EncryptionMetadata
import com.example.backup.validator.BackupValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoAndValidatorTest {

    @Test
    fun encryptAndDecrypt_withAadRoundtripSucceeds() {
        val plaintext = """{"cards":[{"uid":"04:A1:B2:C3:D4:E5:F6"}]}"""
        val password = "StrongVaultPassword#2026"
        val aad = BackupCryptoService.buildHeaderAad(
            formatIdentifier = "CARD_VAULT_BACKUP",
            schemaVersion = 2,
            createdAt = 1700000000000L,
            algorithm = BackupCryptoService.ALGORITHM_NAME,
            keyDerivation = BackupCryptoService.KEY_DERIVATION_NAME,
            iterations = BackupCryptoService.ITERATION_COUNT
        )

        val bundle = BackupCryptoService.encrypt(plaintext, password, aad)
        assertEquals(BackupCryptoService.ITERATION_COUNT, bundle.metadata.iterations)

        val decrypted = BackupCryptoService.decrypt(
            ciphertextBase64 = bundle.ciphertextBase64,
            authTagBase64 = bundle.authTagBase64,
            password = password,
            metadata = bundle.metadata,
            aad = aad
        )
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun decrypt_wrongPasswordThrowsIncorrectPassword() {
        val bundle = BackupCryptoService.encrypt("""{"ok":true}""", "CorrectPass123")
        assertThrows(BackupException.IncorrectPassword::class.java) {
            BackupCryptoService.decrypt(
                ciphertextBase64 = bundle.ciphertextBase64,
                authTagBase64 = bundle.authTagBase64,
                password = "WrongPassword123",
                metadata = bundle.metadata
            )
        }
    }

    @Test
    fun decrypt_tamperedHeaderAadIsRejected() {
        val originalAad = BackupCryptoService.buildHeaderAad(
            formatIdentifier = "CARD_VAULT_BACKUP",
            schemaVersion = 2,
            createdAt = 1700000000000L,
            algorithm = BackupCryptoService.ALGORITHM_NAME,
            keyDerivation = BackupCryptoService.KEY_DERIVATION_NAME,
            iterations = BackupCryptoService.ITERATION_COUNT
        )
        val tamperedAad = BackupCryptoService.buildHeaderAad(
            formatIdentifier = "CARD_VAULT_BACKUP",
            schemaVersion = 1, // tampered version
            createdAt = 1700000000000L,
            algorithm = BackupCryptoService.ALGORITHM_NAME,
            keyDerivation = BackupCryptoService.KEY_DERIVATION_NAME,
            iterations = BackupCryptoService.ITERATION_COUNT
        )

        val bundle = BackupCryptoService.encrypt("""{"secret":"data"}""", "MyPassphrase99", originalAad)
        assertThrows(BackupException.IncorrectPassword::class.java) {
            BackupCryptoService.decrypt(
                ciphertextBase64 = bundle.ciphertextBase64,
                authTagBase64 = bundle.authTagBase64,
                password = "MyPassphrase99",
                metadata = bundle.metadata,
                aad = tamperedAad
            )
        }
    }

    @Test
    fun validator_rejectsUnsafeIterationCountsAndInvalidAlgorithms() {
        val validBase = BackupEnvelope(
            formatIdentifier = "CARD_VAULT_BACKUP",
            schemaVersion = 2,
            appVersion = "1.0.0",
            createdAt = 1700000000000L,
            recordCounts = BackupRecordCounts(),
            encryption = EncryptionMetadata(
                algorithm = BackupCryptoService.ALGORITHM_NAME,
                keyDerivation = BackupCryptoService.KEY_DERIVATION_NAME,
                iterations = BackupCryptoService.ITERATION_COUNT,
                salt = "AAAAAAAAAAAAAAAAAAAAAA==",
                nonce = "AAAAAAAAAAAAAAAA"
            ),
            ciphertext = "AAAA",
            authenticationTag = "AAAAAAAAAAAAAAAAAAAAAA=="
        )
        // Valid passes without throwing
        BackupValidator.validateEnvelope(validBase)

        // DoS huge iterations rejected
        val dosEnvelope = validBase.copy(
            encryption = validBase.encryption.copy(iterations = 50_000_000)
        )
        assertThrows(BackupException.InvalidFormat::class.java) {
            BackupValidator.validateEnvelope(dosEnvelope)
        }

        // Weak iterations rejected
        val weakEnvelope = validBase.copy(
            encryption = validBase.encryption.copy(iterations = 100)
        )
        assertThrows(BackupException.InvalidFormat::class.java) {
            BackupValidator.validateEnvelope(weakEnvelope)
        }

        // Unknown algorithm rejected
        val badAlgEnvelope = validBase.copy(
            encryption = validBase.encryption.copy(algorithm = "AES-128-ECB")
        )
        assertThrows(BackupException.InvalidFormat::class.java) {
            BackupValidator.validateEnvelope(badAlgEnvelope)
        }
    }
}
