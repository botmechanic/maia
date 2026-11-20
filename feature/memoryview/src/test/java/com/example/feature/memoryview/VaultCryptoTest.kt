package com.example.feature.memoryview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultCryptoTest {

    @Test
    fun roundtripEncryptionAndDecryptionSucceeds() {
        val sampleVault = "# Maia Encrypted Memory Vault\n\n- Favorite Coffee: Oat Cortado\n- Partner: Jordan"
        val password = "SuperSecretVaultPassphrase123!"

        val encrypted = VaultCrypto.encryptVault(sampleVault, password)
        assertTrue(encrypted.size > sampleVault.length)

        val decrypted = VaultCrypto.decryptVault(encrypted, password)
        assertEquals(sampleVault, decrypted)
    }

    @Test
    fun wrongPasswordFailsDecryption() {
        val sampleVault = "Confidential User Data"
        val password = "CorrectPassword123"
        val wrongPassword = "WrongPassword456"

        val encrypted = VaultCrypto.encryptVault(sampleVault, password)

        assertThrows(Exception::class.java) {
            VaultCrypto.decryptVault(encrypted, wrongPassword)
        }
    }

    @Test
    fun differentSaltsProduceDifferentCiphertexts() {
        val sampleVault = "Same content"
        val password = "SamePassword"

        val enc1 = VaultCrypto.encryptVault(sampleVault, password)
        val enc2 = VaultCrypto.encryptVault(sampleVault, password)

        // Random salt + IV means ciphertexts must differ
        assertNotEquals(enc1.toList(), enc2.toList())
    }
}

