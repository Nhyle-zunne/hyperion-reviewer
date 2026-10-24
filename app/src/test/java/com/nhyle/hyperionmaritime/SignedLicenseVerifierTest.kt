package com.nhyle.hyperionmaritime

import com.nhyle.hyperionmaritime.data.util.SignedLicenseVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SignedLicenseVerifierTest {

    // Test key made for the fake device A1B2C3D4E5F6: plan EW, 2026-01-01 to 2099-12-31
    private val validKey =
        "HYPE2-AICAICEQ-OIR2DMWD-2TS7NBVE-Q77RFD7J-4IRMKCP7-5ZS5MA37-MGY7NXGQ-" +
            "72FQWY3V-3J4Y3IZ6-P3NMTE23-BNL4AENG-BEBDMGHP-OXLZ6K6E-GV7LZQ32-5ODGQ4A6-67WA"
    private val device = "A1B2C3D4E5F6"

    @Test
    fun validKeyPassesForTheRightDevice() {
        val key = SignedLicenseVerifier.verify(validKey, device)
        assertNotNull(key)
        assertEquals("EW", key!!.tier)
        assertEquals("EXP", key.duration)
        assertEquals(1767225600000L, key.issueEpoch)
        assertEquals(4102444799999L, key.expiryEpoch)
    }

    @Test
    fun wrongDeviceIsRejected() {
        assertNull(SignedLicenseVerifier.verify(validKey, "A1B2C3D4E5F7"))
    }

    @Test
    fun changedCharacterIsRejected() {
        val index = 20
        val original = validKey[index]
        val replacement = if (original == 'A') 'B' else 'A'
        val changed = validKey.substring(0, index) + replacement + validKey.substring(index + 1)
        assertNull(SignedLicenseVerifier.verify(changed, device))
    }

    @Test
    fun shortenedKeyIsRejected() {
        assertNull(SignedLicenseVerifier.verify(validKey.dropLast(10), device))
    }

    @Test
    fun oldFormatKeyIsNotTreatedAsSigned() {
        assertFalse(SignedLicenseVerifier.isSignedFormat("HYPE-EW-1M-1A2B-3C4D-5E6F"))
        assertTrue(SignedLicenseVerifier.isSignedFormat(validKey))
    }
}