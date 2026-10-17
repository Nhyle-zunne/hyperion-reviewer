package com.nhyle.hyperionmaritime.data.util

import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Decrypts question files stored as "<name>.csv.enc".
 * File layout: 12-byte nonce, then AES-256-GCM ciphertext with its 16-byte tag.
 */
object AssetCrypto {
    private val a = intArrayOf(186, 37, 40, 56, 208, 70, 77, 77, 49, 140, 108, 238, 27, 237, 38, 131)
    private val b = intArrayOf(54, 195, 118, 105, 37, 110, 229, 154, 71, 100, 190, 64, 176, 245, 192, 212)

    private val keyBytes: ByteArray by lazy {
        val all = a + b
        ByteArray(all.size) { i ->
            val m = ((i * 37 + 11) xor (i shl 3)) and 0xFF
            (all[i] xor m xor 0x5A).toByte()
        }
    }

    fun decrypt(encrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            GCMParameterSpec(128, encrypted, 0, 12)
        )
        return cipher.doFinal(encrypted, 12, encrypted.size - 12)
    }

    fun decryptStream(stream: InputStream): InputStream {
        val bytes = stream.use { it.readBytes() }
        return ByteArrayInputStream(decrypt(bytes))
    }
}