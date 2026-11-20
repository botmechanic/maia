package com.example.feature.memoryview

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultCrypto {
    private const val HEADER = "MAIAVAULT1"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    fun encryptVault(data: String, passphrase: CharSequence): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toString().toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val headerBytes = HEADER.toByteArray(Charsets.UTF_8)

        val result = ByteArray(headerBytes.size + salt.size + iv.size + ciphertext.size)
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.size)
        System.arraycopy(salt, 0, result, headerBytes.size, salt.size)
        System.arraycopy(iv, 0, result, headerBytes.size + salt.size, iv.size)
        System.arraycopy(ciphertext, 0, result, headerBytes.size + salt.size + iv.size, ciphertext.size)
        return result
    }

    fun decryptVault(bytes: ByteArray, passphrase: CharSequence): String {
        val headerBytes = HEADER.toByteArray(Charsets.UTF_8)
        if (bytes.size < headerBytes.size + SALT_LENGTH + IV_LENGTH) {
            throw IllegalArgumentException("Invalid vault file: payload too short.")
        }

        val fileHeader = String(bytes, 0, headerBytes.size, Charsets.UTF_8)
        if (fileHeader != HEADER) {
            throw IllegalArgumentException("Invalid vault file header: $fileHeader")
        }

        var offset = headerBytes.size
        val salt = ByteArray(SALT_LENGTH)
        System.arraycopy(bytes, offset, salt, 0, SALT_LENGTH)
        offset += SALT_LENGTH

        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(bytes, offset, iv, 0, IV_LENGTH)
        offset += IV_LENGTH

        val ciphertext = ByteArray(bytes.size - offset)
        System.arraycopy(bytes, offset, ciphertext, 0, ciphertext.size)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toString().toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}

