package com.nhyle.hyperionmaritime.data.auth

import com.nhyle.hyperionmaritime.data.model.Track
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class OfflineEntitlementLeaseTest {
    private companion object {
        const val ISSUED_AT = 1_798_000_000_000L
        const val NOW = ISSUED_AT + 1_000L
        const val DECK_EXPIRY = ISSUED_AT + 14L * 24L * 60L * 60L * 1_000L
        const val ENGINE_EXPIRY = ISSUED_AT + 30L * 24L * 60L * 60L * 1_000L
        const val USER_ID = "hyperion-account-123"
    }

    @Test
    fun verifiesMergedWebsiteAndGooglePlayTrackGrants() {
        val keyPair = generateSigningKey()
        val token = signedToken(
            keyPair,
            JSONObject()
                .put("v", 1)
                .put("userId", USER_ID)
                .put("issuedAt", ISSUED_AT)
                .put(
                    "tracks",
                    JSONObject()
                        .put("DECK_OIC_NW", DECK_EXPIRY)
                        .put("ENGINE_MGT", ENGINE_EXPIRY)
                )
        )

        val lease = OfflineEntitlementLease.verify(
            token = token,
            expectedUserId = USER_ID,
            nowEpochMs = NOW,
            verificationKey = keyPair.public
        )

        assertNotNull(lease)
        assertEquals(USER_ID, lease?.userId)
        assertEquals(ISSUED_AT, lease?.issuedAtEpochMs)
        assertEquals(DECK_EXPIRY, lease?.expiryByTrack?.get(Track.DECK_OIC_NW))
        assertEquals(ENGINE_EXPIRY, lease?.expiryByTrack?.get(Track.ENGINE_MGT))
        assertEquals(0L, lease?.expiryByTrack?.get(Track.GMDSS_GOC))
    }

    @Test
    fun rejectsLeaseIssuedForDifferentHyperionAccount() {
        val keyPair = generateSigningKey()
        val token = signedToken(
            keyPair,
            JSONObject()
                .put("v", 1)
                .put("userId", USER_ID)
                .put("issuedAt", ISSUED_AT)
                .put("tracks", JSONObject().put("DECK_OIC_NW", DECK_EXPIRY))
        )

        assertNull(
            OfflineEntitlementLease.verify(
                token = token,
                expectedUserId = "another-hyperion-account",
                nowEpochMs = NOW,
                verificationKey = keyPair.public
            )
        )
    }

    @Test
    fun rejectsTamperedPayloadOrSignature() {
        val keyPair = generateSigningKey()
        val token = signedToken(
            keyPair,
            JSONObject()
                .put("v", 1)
                .put("userId", USER_ID)
                .put("issuedAt", ISSUED_AT)
                .put("tracks", JSONObject().put("DECK_OIC_NW", DECK_EXPIRY))
        )
        val pieces = token.split('.')
        val alteredPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"v":1,"userId":"$USER_ID","issuedAt":$ISSUED_AT,"tracks":{"DECK_OIC_NW":$ENGINE_EXPIRY}}"""
                .toByteArray(StandardCharsets.UTF_8)
        )
        val alteredSignature = pieces[1].dropLast(2) + "AA"

        assertNull(
            OfflineEntitlementLease.verify(
                token = "$alteredPayload.${pieces[1]}",
                expectedUserId = USER_ID,
                nowEpochMs = NOW,
                verificationKey = keyPair.public
            )
        )
        assertNull(
            OfflineEntitlementLease.verify(
                token = "${pieces[0]}.$alteredSignature",
                expectedUserId = USER_ID,
                nowEpochMs = NOW,
                verificationKey = keyPair.public
            )
        )
    }

    @Test
    fun expiredTrackIsNotRestoredEvenWhenSignatureIsValid() {
        val keyPair = generateSigningKey()
        val expired = ISSUED_AT + 500L
        val token = signedToken(
            keyPair,
            JSONObject()
                .put("v", 1)
                .put("userId", USER_ID)
                .put("issuedAt", ISSUED_AT)
                .put("tracks", JSONObject().put("DECK_OIC_NW", expired))
        )

        val lease = OfflineEntitlementLease.verify(
            token = token,
            expectedUserId = USER_ID,
            nowEpochMs = NOW,
            verificationKey = keyPair.public
        )

        assertNotNull(lease)
        assertEquals(0L, lease?.expiryByTrack?.get(Track.DECK_OIC_NW))
    }

    private fun generateSigningKey(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()

    private fun signedToken(keyPair: KeyPair, claims: JSONObject): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            claims.toString().toByteArray(StandardCharsets.UTF_8)
        )
        val derSignature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(payload.toByteArray(StandardCharsets.UTF_8))
            sign()
        }
        val joseSignature = derEcdsaToRaw(derSignature)
        return "$payload.${Base64.getUrlEncoder().withoutPadding().encodeToString(joseSignature)}"
    }

    /** Converts a JCA DER ECDSA signature to WebCrypto's fixed-width JOSE r||s form. */
    private fun derEcdsaToRaw(der: ByteArray): ByteArray {
        require(der.size >= 8 && der[0] == 0x30.toByte())
        var offset = 2
        require(der[offset++] == 0x02.toByte())
        val rLength = der[offset++].toInt() and 0xff
        val r = der.copyOfRange(offset, offset + rLength)
        offset += rLength
        require(der[offset++] == 0x02.toByte())
        val sLength = der[offset++].toInt() and 0xff
        val s = der.copyOfRange(offset, offset + sLength)
        require(offset + sLength == der.size)

        fun fixedWidth(value: ByteArray): ByteArray {
            val magnitude = value.dropWhile { it == 0.toByte() }.toByteArray()
            require(magnitude.size <= 32)
            return ByteArray(32).also { magnitude.copyInto(it, 32 - magnitude.size) }
        }

        return fixedWidth(r) + fixedWidth(s)
    }
}
