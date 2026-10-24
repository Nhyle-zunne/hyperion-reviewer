package com.nhyle.hyperionmaritime

import org.junit.Test
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility test to encrypt question CSV files into .csv.enc assets
 * using the Hyperion AssetCrypto AES-256-GCM standard.
 */
class QuestionEncryptionToolTest {

    private val a = intArrayOf(186, 37, 40, 56, 208, 70, 77, 77, 49, 140, 108, 238, 27, 237, 38, 131)
    private val b = intArrayOf(54, 195, 118, 105, 37, 110, 229, 154, 71, 100, 190, 64, 176, 245, 192, 212)

    private val keyBytes: ByteArray by lazy {
        val all = a + b
        ByteArray(all.size) { i ->
            val m = ((i * 37 + 11) xor (i shl 3)) and 0xFF
            (all[i] xor m xor 0x5A).toByte()
        }
    }

    @Test
    fun encryptAllCsvQuestionsInAssets() {
        // Find assets/questions directory regardless of run directory
        val candidates = listOf(
            File("src/main/assets/questions"),
            File("app/src/main/assets/questions")
        )
        val questionsDir = candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: throw IllegalStateException("Questions directory not found! Checked: $candidates")

        println("Found questions directory at: ${questionsDir.absolutePath}")

        var encryptedCount = 0

        // Recursively find every .csv file that is NOT already .enc
        questionsDir.walkTopDown().forEach { file ->
            if (file.isFile && file.name.endsWith(".csv", ignoreCase = true)) {
                val encFile = File(file.parentFile, "${file.name}.enc")
                
                // 1. Read plain CSV
                val plainBytes = file.readBytes()
                
                // 2. Generate 12-byte random IV/nonce
                val nonce = ByteArray(12).apply { SecureRandom().nextBytes(this) }
                
                // 3. Encrypt using AES-256-GCM
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, nonce))
                val ciphertext = cipher.doFinal(plainBytes)
                
                // 4. Write: [12-byte nonce] + [ciphertext with 16-byte tag]
                encFile.writeBytes(nonce + ciphertext)
                println(" [ENCRYPTED] ${file.name} -> ${encFile.name} (${encFile.length()} bytes)")
                
                // 5. Delete raw unencrypted CSV so it doesn't get bundled into the APK
                val deleted = file.delete()
                if (deleted) {
                    println(" [PROTECTED] Deleted plain unencrypted ${file.name}")
                }
                
                encryptedCount++
            }
        }

        if (encryptedCount == 0) {
            println("No raw .csv files found to encrypt. Make sure your new .csv file is in: ${questionsDir.path}")
        } else {
            println("SUCCESS: Encrypted and secured $encryptedCount question file(s)!")
        }
    }
}