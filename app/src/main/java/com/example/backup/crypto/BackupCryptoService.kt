package com.example.backup.crypto

import com.example.backup.model.BackupException
import com.example.backup.model.EncryptionMetadata
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedBundle(
    val ciphertextBase64: String,
    val authTagBase64: String,
    val metadata: EncryptionMetadata
)

object BackupCryptoService {

    const val ALGORITHM_NAME = "AES-256-GCM"
    const val KEY_DERIVATION_NAME = "PBKDF2-HMAC-SHA-256"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    const val ITERATION_COUNT = 210_000
    const val MIN_ITERATIONS = 10_000
    const val MAX_ITERATIONS = 1_000_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8
    private const val GCM_NONCE_LENGTH_BYTES = 12
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    fun buildHeaderAad(
        formatIdentifier: String,
        schemaVersion: Int,
        createdAt: Long,
        algorithm: String,
        keyDerivation: String,
        iterations: Int
    ): ByteArray {
        return "$formatIdentifier|v$schemaVersion|$createdAt|$algorithm|$keyDerivation|$iterations"
            .toByteArray(Charsets.UTF_8)
    }

    fun encrypt(
        payloadJson: String,
        password: String,
        aad: ByteArray? = null
    ): EncryptedBundle {
        try {
            val salt = ByteArray(SALT_LENGTH_BYTES)
            secureRandom.nextBytes(salt)

            val nonce = ByteArray(GCM_NONCE_LENGTH_BYTES)
            secureRandom.nextBytes(nonce)

            val secretKey = deriveKey(password, salt, ITERATION_COUNT)

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            if (aad != null && aad.isNotEmpty()) {
                cipher.updateAAD(aad)
            }

            val encryptedBytes = cipher.doFinal(payloadJson.toByteArray(Charsets.UTF_8))

            val ciphertextLength = encryptedBytes.size - GCM_TAG_LENGTH_BYTES
            val cipherOnly = encryptedBytes.copyOfRange(0, ciphertextLength)
            val authTagOnly = encryptedBytes.copyOfRange(ciphertextLength, encryptedBytes.size)

            val encoder = Base64.getEncoder()
            val metadata = EncryptionMetadata(
                algorithm = ALGORITHM_NAME,
                keyDerivation = KEY_DERIVATION_NAME,
                iterations = ITERATION_COUNT,
                salt = encoder.encodeToString(salt),
                nonce = encoder.encodeToString(nonce)
            )

            return EncryptedBundle(
                ciphertextBase64 = encoder.encodeToString(cipherOnly),
                authTagBase64 = encoder.encodeToString(authTagOnly),
                metadata = metadata
            )
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException.WriteFailed("Failed to encrypt backup payload: ${e.localizedMessage}", e)
        }
    }

    fun decrypt(
        ciphertextBase64: String,
        authTagBase64: String,
        password: String,
        metadata: EncryptionMetadata,
        aad: ByteArray? = null
    ): String {
        if (metadata.algorithm != ALGORITHM_NAME) {
            throw BackupException.InvalidFormat("Unsupported encryption algorithm: ${metadata.algorithm}")
        }
        if (metadata.keyDerivation != KEY_DERIVATION_NAME) {
            throw BackupException.InvalidFormat("Unsupported key derivation function: ${metadata.keyDerivation}")
        }
        if (metadata.iterations !in MIN_ITERATIONS..MAX_ITERATIONS) {
            throw BackupException.InvalidFormat("Unsupported PBKDF2 iteration count: ${metadata.iterations}")
        }

        try {
            val decoder = Base64.getMimeDecoder()
            val salt = decoder.decode(metadata.salt.trim())
            val nonce = decoder.decode(metadata.nonce.trim())
            val cipherOnly = decoder.decode(ciphertextBase64.trim())
            val authTag = decoder.decode(authTagBase64.trim())

            if (salt.size !in 16..64) {
                throw BackupException.InvalidFormat("Invalid encryption salt length.")
            }
            if (nonce.size != GCM_NONCE_LENGTH_BYTES) {
                throw BackupException.InvalidFormat("Invalid GCM nonce length.")
            }
            if (authTag.size != GCM_TAG_LENGTH_BYTES) {
                throw BackupException.InvalidFormat("Invalid GCM authentication tag length.")
            }

            val combinedEncrypted = ByteArray(cipherOnly.size + authTag.size)
            System.arraycopy(cipherOnly, 0, combinedEncrypted, 0, cipherOnly.size)
            System.arraycopy(authTag, 0, combinedEncrypted, cipherOnly.size, authTag.size)

            val secretKey = deriveKey(password, salt, metadata.iterations)

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            if (aad != null && aad.isNotEmpty()) {
                cipher.updateAAD(aad)
            }

            val decryptedBytes = cipher.doFinal(combinedEncrypted)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: BackupException) {
            throw e
        } catch (e: javax.crypto.AEADBadTagException) {
            throw BackupException.IncorrectPassword()
        } catch (e: javax.crypto.BadPaddingException) {
            throw BackupException.IncorrectPassword()
        } catch (e: Exception) {
            if (e.localizedMessage?.contains("mac check failed", ignoreCase = true) == true ||
                e.localizedMessage?.contains("tag mismatch", ignoreCase = true) == true
            ) {
                throw BackupException.IncorrectPassword()
            }
            throw BackupException.CorruptedPayload(e)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): SecretKey {
        val chars = password.toCharArray()
        val keySpec = PBEKeySpec(chars, salt, iterations, KEY_LENGTH_BITS)
        return try {
            val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val secretKeyBytes = keyFactory.generateSecret(keySpec).encoded
            SecretKeySpec(secretKeyBytes, "AES")
        } finally {
            keySpec.clearPassword()
            chars.fill('\u0000')
        }
    }
}
