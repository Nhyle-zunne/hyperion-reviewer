package com.nhyle.hyperionmaritime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.QuestionImageRef
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.QuestionMediaException
import com.nhyle.hyperionmaritime.data.repository.QuestionMediaFailure
import com.nhyle.hyperionmaritime.data.repository.VerifiedQuestionMediaStore
import com.nhyle.hyperionmaritime.data.util.AssetCrypto
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BundledQuestionMediaStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val track = Track.DECK_OIC_NW
    private val owner = "11111111-1111-4111-8111-111111111111"
    // The store validates file signatures and hashes; bitmap decoding is covered by the gallery tests.
    private val png = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0, 0, 0, 0, 0)
    private val ref get() = QuestionImageRef(hash(png), "image/png", "question", "Bundled chart")

    @Before fun setup() {
        val accounts = context.getSharedPreferences("bundled_media_test_accounts", Context.MODE_PRIVATE)
        accounts.edit().clear().commit()
        LicenseManager.setAccountEntitlementPreferencesForTesting(accounts)
        context.getSharedPreferences(LicenseManager.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(LicenseManager.SECURE_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        LicenseManager.setCurrentAccountUserId(context, null)
        LicenseManager.setOicNwProUnlocked(context, false)
    }

    @After fun cleanup() {
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setCurrentAccountUserId(context, null)
        LicenseManager.clearAccountEntitlements(context)
        LicenseManager.setAccountEntitlementPreferencesForTesting(null)
    }

    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun encrypt(bytes: ByteArray = png): ByteArray {
        val keyGetter = AssetCrypto::class.java.getDeclaredMethod("getKeyBytes").apply { isAccessible = true }
        val key = keyGetter.invoke(AssetCrypto) as ByteArray
        val nonce = ByteArray(12) { it.toByte() }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return nonce + cipher.doFinal(bytes)
    }

    private fun expectFailure(reason: QuestionMediaFailure, action: () -> Unit) {
        try { action(); fail("Expected $reason") }
        catch (error: QuestionMediaException) { assertEquals(reason, error.reason) }
    }

    private fun syntheticFreeQuestion() = Question(
        id = "synthetic-free-q1", track = track, functionId = "F1", competenceId = "C1",
        topic = "Practice", questionText = "What does the diagram show?",
        options = listOf("A", "B"), correctAnswerIndex = 0,
        explanation = "Synthetic explanation", difficulty = Difficulty.EASY,
        imageAttachments = listOf(ref)
    )

    @Test fun evenAnOfficialFreeReviewerImageIsLockedWithoutItsTrackLicense() {
        val freeQuestion = syntheticFreeQuestion()
        assertTrue(freeQuestion.imageAttachments.contains(ref))
        var opened = false
        expectFailure(QuestionMediaFailure.LICENSE_REQUIRED) {
            runBlocking {
                VerifiedQuestionMediaStore.loadForDisplay(context, track, ref) {
                    opened = true
                    ByteArrayInputStream(encrypt())
                }
            }
        }
        assertFalse("Free Reviewer image references must never open encrypted assets without a license", opened)
    }

    @Test fun anotherTrackLicenseDoesNotUnlockThisTracksImages() {
        LicenseManager.setEngineMgtProUnlocked(context, true)
        var opened = false
        expectFailure(QuestionMediaFailure.LICENSE_REQUIRED) {
            runBlocking {
                VerifiedQuestionMediaStore.loadForDisplay(context, track, ref) {
                    opened = true
                    ByteArrayInputStream(encrypt())
                }
            }
        }
        assertFalse("A management license must not unlock OIC-NW question diagrams", opened)
        LicenseManager.setEngineMgtProUnlocked(context, false)
    }

    @Test fun deviceLicenseLoadsBundledImageWithoutAccountOrNetwork() = runBlocking {
        LicenseManager.setOicNwProUnlocked(context, true)
        assertNull(LicenseManager.getCurrentAccountUserId(context))
        var opens = 0
        val result = VerifiedQuestionMediaStore.loadForDisplay(context, track, ref) { path ->
            opens++
            assertEquals("question-media/DECK_OIC_NW/${ref.sha256}.png.enc", path)
            ByteArrayInputStream(encrypt())
        }
        assertArrayEquals(png, result)
        assertEquals(1, opens)
    }

    @Test fun verifiedSignedInAdminLoadsBundledImage() = runBlocking {
        LicenseManager.setCurrentAccountUserId(context, owner)
        LicenseManager.setVerifiedDeveloperAdminAccess(context, owner, true)
        assertArrayEquals(png, VerifiedQuestionMediaStore.loadForDisplay(context, track, ref) {
            ByteArrayInputStream(encrypt())
        })
    }

    @Test fun unlicensedTrackNeverOpensAsset() {
        var opened = false
        expectFailure(QuestionMediaFailure.LICENSE_REQUIRED) {
            runBlocking { VerifiedQuestionMediaStore.loadForDisplay(context, track, ref) {
                opened = true
                throw AssertionError("Unlicensed asset read")
            } }
        }
        assertFalse(opened)
    }

    @Test fun corruptCiphertextHashAndMimeAreRejected() {
        val damaged = encrypt().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }
        for ((reference, bytes) in listOf(ref to damaged,
            ref.copy(sha256 = "a".repeat(64)) to encrypt(),
            ref.copy(mime = "image/jpeg") to encrypt())) {
            expectFailure(QuestionMediaFailure.BUNDLE_DAMAGED) {
                VerifiedQuestionMediaStore.readBundled(track, reference) { ByteArrayInputStream(bytes) }
            }
        }
        expectFailure(QuestionMediaFailure.INTEGRITY_FAILED) {
            VerifiedQuestionMediaStore.readBundled(track, ref.copy(mime = "image/svg+xml")) {
                throw AssertionError("Invalid mime must not open an asset")
            }
        }
    }

    @Test fun oversizeAndUnreadableBundleProduceTypedFailure() {
        expectFailure(QuestionMediaFailure.BUNDLE_DAMAGED) {
            VerifiedQuestionMediaStore.readBundled(track, ref) { ByteArrayInputStream(ByteArray(524288 + 29)) }
        }
        expectFailure(QuestionMediaFailure.BUNDLE_DAMAGED) {
            VerifiedQuestionMediaStore.readBundled(track, ref) { throw IOException("Asset unavailable") }
        }
    }

    @Test fun missingLocalAssetRequiresAppUpdateForUnsignedAndSignedInUsers() {
        assertNull(VerifiedQuestionMediaStore.readBundled(track, ref) { throw FileNotFoundException() })
        LicenseManager.setOicNwProUnlocked(context, true)
        for (account in listOf(null, owner)) {
            LicenseManager.setCurrentAccountUserId(context, account)
            expectFailure(QuestionMediaFailure.NOT_BUNDLED) {
                runBlocking { VerifiedQuestionMediaStore.loadForDisplay(context, track, ref) { throw FileNotFoundException() } }
            }
        }
    }

    @Test fun cancellationFromOpeningAndReadingIsPreserved() {
        val cancelled = CancellationException("Synthetic cancellation")
        val openers: List<(String) -> InputStream> = listOf(
            { throw cancelled },
            { object : InputStream() { override fun read(): Int = throw cancelled } },
        )
        for (open in openers) {
            try { VerifiedQuestionMediaStore.readBundled(track, ref, open); fail("Expected cancellation") }
            catch (error: CancellationException) { assertSame(cancelled, error) }
        }
    }

    @Test fun ownerChangeAfterReadPreventsReturningBytes() {
        LicenseManager.setOicNwProUnlocked(context, true)
        expectFailure(QuestionMediaFailure.ACCOUNT_CHANGED) {
            runBlocking { VerifiedQuestionMediaStore.loadForDisplay(context, track, ref) {
                object : ByteArrayInputStream(encrypt()) {
                    override fun close() {
                        super.close()
                        LicenseManager.setCurrentAccountUserId(context, owner)
                    }
                }
            } }
        }
    }

    @Test fun revokedLicenseAfterReadPreventsReturningBytes() {
        LicenseManager.setOicNwProUnlocked(context, true)
        expectFailure(QuestionMediaFailure.ACCOUNT_CHANGED) {
            runBlocking { VerifiedQuestionMediaStore.loadForDisplay(context, track, ref) {
                object : ByteArrayInputStream(encrypt()) {
                    override fun close() {
                        super.close()
                        LicenseManager.setOicNwProUnlocked(context, false)
                    }
                }
            } }
        }
    }
}
