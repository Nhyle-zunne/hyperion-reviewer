package com.nhyle.hyperionmaritime.data.auth

import com.nhyle.hyperionmaritime.data.model.Track
import org.json.JSONObject
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.util.Base64

internal data class VerifiedOfflineEntitlementLease(
    val userId: String,
    val issuedAtEpochMs: Long,
    val expiryByTrack: Map<Track, Long>
)

/**
 * Verifies the server's signed, account-bound offline lease. Both the PWA and
 * Android consume the same question-bank offline-license response, so website
 * and Google Play purchases have identical offline behavior.
 */
internal object OfflineEntitlementLease {
    private const val PUBLIC_KEY_X = "rRSoQJxUoGwX8-g5IwWOnDik-l_HYqJrN01fOg4vzOY"
    private const val PUBLIC_KEY_Y = "IEysYJ20UAhm1-CrPoqVTW0ODu5nfa4qBZT1nCM5ZjU"

    private val pinnedPublicKey: PublicKey by lazy {
        val parameters = AlgorithmParameters.getInstance("EC").apply {
            init(ECGenParameterSpec("secp256r1"))
        }.getParameterSpec(ECParameterSpec::class.java)
        val x = BigInteger(1, Base64.getUrlDecoder().decode(PUBLIC_KEY_X))
        val y = BigInteger(1, Base64.getUrlDecoder().decode(PUBLIC_KEY_Y))
        KeyFactory.getInstance("EC").generatePublic(
            ECPublicKeySpec(ECPoint(x, y), parameters)
        )
    }

    fun verify(
        token: String,
        expectedUserId: String,
        nowEpochMs: Long,
        verificationKey: PublicKey = pinnedPublicKey
    ): VerifiedOfflineEntitlementLease? {
        return try {
            if (token.isBlank() || token.length > 12_000 || expectedUserId.isBlank() || nowEpochMs <= 0L) {
                return null
            }
            val parts = token.split('.')
            if (parts.size != 2) return null
            val payloadBytes = Base64.getUrlDecoder().decode(parts[0])
            val rawSignature = Base64.getUrlDecoder().decode(parts[1])
            if (payloadBytes.size > 8_192 || rawSignature.size != 64) return null

            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(verificationKey)
            verifier.update(parts[0].toByteArray(StandardCharsets.UTF_8))
            if (!verifier.verify(rawEcdsaSignatureToDer(rawSignature))) return null

            val claims = JSONObject(String(payloadBytes, StandardCharsets.UTF_8))
            if (claims.optInt("v", -1) != 1 || claims.optString("userId") != expectedUserId) {
                return null
            }
            val issuedAtValue = claims.optDouble("issuedAt", Double.NaN)
            if (!issuedAtValue.isFinite() || issuedAtValue <= 0.0) return null
            val issuedAt = issuedAtValue.toLong()
            val rawTracks = claims.optJSONObject("tracks") ?: return null
            val effectiveNow = maxOf(nowEpochMs, issuedAt)
            val expiryByTrack = Track.values().associateWith { 0L }.toMutableMap()
            val keys = rawTracks.keys()
            while (keys.hasNext()) {
                val trackId = keys.next()
                val track = when (trackId) {
                    "DECK_OIC_NW" -> Track.DECK_OIC_NW
                    "GMDSS_GOC" -> Track.GMDSS_GOC
                    "ENGINE_OIC_EW" -> Track.ENGINE_OIC_EW
                    "DECK_MGT" -> Track.DECK_MGT
                    "ENGINE_MGT" -> Track.ENGINE_MGT
                    else -> return null
                }
                val expiryValue = rawTracks.optDouble(trackId, Double.NaN)
                if (!expiryValue.isFinite() || expiryValue <= issuedAt.toDouble()) return null
                val expiry = expiryValue.toLong()
                if (expiry > effectiveNow) expiryByTrack[track] = expiry
            }
            VerifiedOfflineEntitlementLease(
                userId = expectedUserId,
                issuedAtEpochMs = issuedAt,
                expiryByTrack = expiryByTrack
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Converts the server's JOSE raw r||s signature into the DER form required by JCA. */
    private fun rawEcdsaSignatureToDer(raw: ByteArray): ByteArray {
        require(raw.size == 64)
        fun integer(offset: Int): ByteArray {
            var first = offset
            val end = offset + 32
            while (first < end - 1 && raw[first] == 0.toByte()) first++
            val magnitude = raw.copyOfRange(first, end)
            val prefix = if ((magnitude[0].toInt() and 0x80) != 0) byteArrayOf(0) else byteArrayOf()
            val value = prefix + magnitude
            return byteArrayOf(0x02, value.size.toByte()) + value
        }
        val r = integer(0)
        val s = integer(32)
        val body = r + s
        return byteArrayOf(0x30, body.size.toByte()) + body
    }
}
