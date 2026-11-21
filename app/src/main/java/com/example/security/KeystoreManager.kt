package com.example.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface CryptoProvider {
    fun encrypt(plainText: String): String
    fun decrypt(cipherTextBase64: String): String
}

class KeystoreManager : CryptoProvider {

    companion object {
        private const val KEY_ALIAS = "CampusNfcWalletMasterKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    @Synchronized
    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val specBuilder = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                specBuilder.setUnlockedDeviceRequired(true)
            }

            keyGenerator.init(specBuilder.build())
            keyGenerator.generateKey()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            ?: throw SecurityException("AndroidKeyStore master key entry missing or invalid.")
        return entry.secretKey
    }

    override fun encrypt(plainText: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
            val iv = cipher.iv
            if (iv == null || iv.size != GCM_IV_LENGTH) {
                throw SecurityException("Unexpected GCM IV size from AndroidKeyStore.")
            }
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt payload with AndroidKeyStore: ${e.message}", e)
        }
    }

    override fun decrypt(cipherTextBase64: String): String {
        if (cipherTextBase64.isBlank()) {
            throw SecurityException("Ciphertext is blank.")
        }
        try {
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH + (GCM_TAG_LENGTH / 8)) {
                throw SecurityException("Ciphertext payload is truncated or malformed.")
            }
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encryptedBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), spec)

            return String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            throw SecurityException("Failed to decrypt payload with AndroidKeyStore: ${e.message}", e)
        }
    }
}
