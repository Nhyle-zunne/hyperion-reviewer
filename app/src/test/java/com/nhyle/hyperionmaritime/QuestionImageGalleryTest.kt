package com.nhyle.hyperionmaritime

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.model.*
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.ui.components.QuestionImageGallery
import com.nhyle.hyperionmaritime.data.repository.VerifiedQuestionMediaStore
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuestionImageGalleryTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val owner = "11111111-1111-4111-8111-111111111111"
    private lateinit var ref: QuestionImageRef
    private lateinit var bytes: ByteArray
    private val cache get() = File(context.filesDir, "verified-question-media-v1")

    @Before fun setup() {
        val prefs = context.getSharedPreferences("image_gallery_test_accounts", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        LicenseManager.setAccountEntitlementPreferencesForTesting(prefs)
        LicenseManager.setCurrentAccountUserId(context, owner)
        LicenseManager.setVerifiedDeveloperAdminAccess(context, owner, true)
        cache.deleteRecursively()
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        bitmap.recycle()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        ref = QuestionImageRef(hash, "image/png", "question", "Synthetic chart")
        save()
    }

    @After fun cleanup() {
        cache.deleteRecursively()
        LicenseManager.clearAccountEntitlements(context)
        LicenseManager.setAccountEntitlementPreferencesForTesting(null)
    }

    private fun save() {
        File(cache, "$owner/DECK_OIC_NW/${ref.sha256}.png").apply {
            parentFile!!.mkdirs()
            writeBytes(bytes)
        }
    }
    private fun question() = Question("image-test", Track.DECK_OIC_NW, "F1", "C1", "Charts",
        "Read chart", listOf("A", "B"), 0, "Explanation", Difficulty.EASY, imageAttachments = listOf(ref))
    private fun awaitImage() { compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription(ref.alt).fetchSemanticsNodes().isNotEmpty() } }

    @Test fun ownerChangeRemovesImageAndOpenZoom() {
        compose.setContent { MaterialTheme { QuestionImageGallery(question(), "question") } }
        awaitImage()
        compose.onNodeWithContentDescription(ref.alt).performClick()
        compose.onNodeWithText("Close").assertExists()
        compose.runOnIdle { LicenseManager.setCurrentAccountUserId(context, "22222222-2222-4222-8222-222222222222") }
        compose.onNodeWithText("Close").assertDoesNotExist()
        compose.onAllNodesWithContentDescription(ref.alt).assertCountEquals(0)
    }

    @Test fun revocationRemovesImageAndOpenZoom() {
        compose.setContent { MaterialTheme { QuestionImageGallery(question(), "question") } }
        awaitImage()
        compose.onNodeWithContentDescription(ref.alt).performClick()
        compose.runOnIdle { LicenseManager.setVerifiedDeveloperAdminAccess(context, owner, false) }
        compose.onNodeWithText("Close").assertDoesNotExist()
        compose.onAllNodesWithContentDescription(ref.alt).assertCountEquals(0)
    }

    @Test fun revisedReferenceClosesZoomAndTrackChangeDoesNotReuseBitmap() {
        var current by mutableStateOf(question())
        compose.setContent { MaterialTheme { QuestionImageGallery(current, "question") } }
        awaitImage()
        compose.onNodeWithContentDescription(ref.alt).performClick()
        compose.runOnIdle { current = current.copy(imageAttachments = listOf(ref.copy(alt = "Revised chart"))) }
        compose.onNodeWithText("Close").assertDoesNotExist()
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Revised chart").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { current = current.copy(track = Track.ENGINE_MGT) }
        compose.onAllNodesWithContentDescription("Revised chart").assertCountEquals(0)
    }

    @Test fun retryLoadsRepairedLocalCacheWithoutQuestionNavigation() {
        cache.deleteRecursively()
        compose.setContent { MaterialTheme { QuestionImageGallery(question(), "question") } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Diagram unavailable.", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { save() }
        compose.onNodeWithText("Retry").performClick()
        awaitImage()
        compose.onNodeWithText("Diagram unavailable.", substring = true).assertDoesNotExist()
    }

    @Test fun verifiedCacheRejectsTamperedBytesAndOtherOwner() = runBlocking {
        Assert.assertArrayEquals(bytes, VerifiedQuestionMediaStore.load(context, Track.DECK_OIC_NW, ref))
        LicenseManager.setCurrentAccountUserId(context, "22222222-2222-4222-8222-222222222222")
        Assert.assertNull(VerifiedQuestionMediaStore.load(context, Track.DECK_OIC_NW, ref))
        LicenseManager.setCurrentAccountUserId(context, owner)
        LicenseManager.setVerifiedDeveloperAdminAccess(context, owner, true)
        File(cache, "$owner/DECK_OIC_NW/${ref.sha256}.png").appendBytes(byteArrayOf(1))
        Assert.assertNull(VerifiedQuestionMediaStore.load(context, Track.DECK_OIC_NW, ref))
        Assert.assertFalse(File(cache, "$owner/DECK_OIC_NW/${ref.sha256}.png").exists())
    }

    @Test fun sameHashCanRenderAtBothPlacements() {
        val current = question().copy(imageAttachments = listOf(ref, ref.copy(placement = "explanation", alt = "Explanation chart")))
        compose.setContent { MaterialTheme { androidx.compose.foundation.layout.Column {
            QuestionImageGallery(current, "question")
            QuestionImageGallery(current, "explanation")
        } } }
        awaitImage()
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Explanation chart").fetchSemanticsNodes().isNotEmpty() }
    }
}
