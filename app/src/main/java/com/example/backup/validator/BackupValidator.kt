package com.example.backup.validator

import com.example.backup.crypto.BackupCryptoService
import com.example.backup.model.BackupEnvelope
import com.example.backup.model.BackupException
import com.example.backup.model.BackupPayload

object BackupValidator {

    private const val EXPECTED_FORMAT_IDENTIFIER = "CARD_VAULT_BACKUP"

    fun validateEnvelope(envelope: BackupEnvelope) {
        if (envelope.formatIdentifier != EXPECTED_FORMAT_IDENTIFIER) {
            throw BackupException.InvalidFormat("Format identifier '${envelope.formatIdentifier}' is invalid. Expected '$EXPECTED_FORMAT_IDENTIFIER'.")
        }

        if (envelope.encryption.algorithm != BackupCryptoService.ALGORITHM_NAME) {
            throw BackupException.InvalidFormat("Unsupported algorithm '${envelope.encryption.algorithm}'.")
        }

        if (envelope.encryption.keyDerivation != BackupCryptoService.KEY_DERIVATION_NAME) {
            throw BackupException.InvalidFormat("Unsupported key derivation '${envelope.encryption.keyDerivation}'.")
        }

        if (envelope.encryption.iterations !in BackupCryptoService.MIN_ITERATIONS..BackupCryptoService.MAX_ITERATIONS) {
            throw BackupException.InvalidFormat("PBKDF2 iteration count ${envelope.encryption.iterations} is outside allowed safety bounds.")
        }

        if (envelope.ciphertext.isBlank()) {
            throw BackupException.InvalidFormat("Backup envelope contains empty ciphertext.")
        }

        if (envelope.authenticationTag.isBlank()) {
            throw BackupException.InvalidFormat("Backup envelope contains missing GCM authentication tag.")
        }

        if (envelope.encryption.salt.isBlank() || envelope.encryption.nonce.isBlank()) {
            throw BackupException.InvalidFormat("Backup envelope contains invalid encryption salt or nonce.")
        }
    }

    fun validatePayload(payload: BackupPayload) {
        payload.cards.forEach { card ->
            if (card.uid.isBlank()) {
                throw BackupException.CorruptedPayload(IllegalArgumentException("Card record missing UID."))
            }
        }
    }
}
