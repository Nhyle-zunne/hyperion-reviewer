package com.nhyle.hyperionmaritime.data.util

import android.util.Base64
import java.math.BigInteger
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Verifies HYPE2 signed keys (ECDSA P-256). The app holds only the PUBLIC key,
 * so keys cannot be made from the app. Input must already be trimmed and uppercased.
 */
object SignedLicenseVerifier {

    private const val PUBLIC_KEY_B64 =
        "BLQK/KdMlP/WSh7NMOOnXPPC7/eJzTU9x4fzIb0ha6M8jgSFBkCWg8G88fYDqSfv0+tgSmuVD9Kzk2fqxIMNLiA="

    private const val PREFIX = "HYPE2-"
    private const val DAY_MS = 86_400_000L
    private const val EPOCH_2020_DAYS = 18262L // days from 1970-01-01 to 2020-01-01

    private val PLANS = arrayOf("", "ALL", "OIC", "GOC", "EW", "DM", "EM")
    private val DURATIONS = arrayOf("", "1W", "1M", "1Y", "EXP")

    class SignedKey(
        val tier: String,
        val duration: String,
        val issueEpoch: Long,
        val expiryEpoch: Long
    )

    fun isSignedFormat(cleanInput: String): Boolean = cleanInput.startsWith(PREFIX)

    /** Returns the key details if the signature, format and device all match, otherwise null. */
    fun verify(cleanInput: String, deviceToken: String): SignedKey? {
        return try {
            if (!isSignedFormat(cleanInput)) return null
            val bytes = decodeBase32(cleanInput.removePrefix(PREFIX).replace("-", "")) ?: return null
            if (bytes.size != 77) return null

            val payload = bytes.copyOfRange(0, 13)
            val signature = bytes.copyOfRange(13, 77)
            if (!signatureIsValid(payload, signature)) return null
            if (payload[0].toInt() != 2) return null

            val plan = payload[1].toInt() and 0xFF
            val duration = payload[2].toInt() and 0xFF
            if (plan !in 1..6 || duration !in 1..4) return null

            val issueDay = ((payload[3].toInt() and 0xFF) shl 8) or (payload[4].toInt() and 0xFF)
            val expiryDay = ((payload[5].toInt() and 0xFF) shl 8) or (payload[6].toInt() and 0xFF)
            if (expiryDay <= issueDay) return null

            val deviceHex = (7..12).joinToString("") { "%02X".format(payload[it].toInt() and 0xFF) }
            if (deviceHex != deviceToken.trim().uppercase()) return null

            SignedKey(
                tier = PLANS[plan],
                duration = DURATIONS[duration],
                issueEpoch = (issueDay + EPOCH_2020_DAYS) * DAY_MS,
                // The key works through the very end of the end date (UTC)
                expiryEpoch = (expiryDay + EPOCH_2020_DAYS + 1) * DAY_MS - 1
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun signatureIsValid(payload: ByteArray, rawSignature: ByteArray): Boolean {
        val point = Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT)
        val spkiHeader = byteArrayOf(
            0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d,
            0x02, 0x01, 0x06, 0x08, 0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d, 0x03, 0x01,
            0x07, 0x03, 0x42, 0x00
        )
        val publicKey = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(spkiHeader + point))
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(payload)
        return verifier.verify(rawToDer(rawSignature))
    }

    // The key tool makes a raw 64-byte signature; Android expects the DER format.
    private fun rawToDer(raw: ByteArray): ByteArray {
        val r = BigInteger(1, raw.copyOfRange(0, 32)).toByteArray()
        val s = BigInteger(1, raw.copyOfRange(32, 64)).toByteArray()
        val body = byteArrayOf(0x02, r.size.toByte()) + r + byteArrayOf(0x02, s.size.toByte()) + s
        return byteArrayOf(0x30, body.size.toByte()) + body
    }

    private fun decodeBase32(text: String): ByteArray? {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val out = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (c in text) {
            val v = alphabet.indexOf(c)
            if (v < 0) return null
            buffer = (buffer shl 5) or v
            bits += 5
            if (bits >= 8) {
                out.write((buffer shr (bits - 8)) and 0xFF)
                bits -= 8
            }
            buffer = buffer and ((1 shl bits) - 1)
        }
        return out.toByteArray()
    }
}