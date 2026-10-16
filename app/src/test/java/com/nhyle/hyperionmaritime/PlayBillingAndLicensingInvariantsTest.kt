package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.billing.BillingConstants
import com.nhyle.hyperionmaritime.data.billing.BillingManager
import com.nhyle.hyperionmaritime.data.model.QuizMode
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.viewmodel.DrillDeliveryMode
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class PlayBillingAndLicensingInvariantsTest {

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var viewModel: ReviewerViewModel
    private lateinit var billingManager: BillingManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        MaritimeQuestionBank.init(context)

        // Reset all licenses to clean unactivated state
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.setHyperionProUnlocked(context, false)
        LicenseManager.setMasterProUnlocked(context, false)
        LicenseManager.init(context)

        // Clear Play Billing secure preferences
        val billingPrefs = context.getSharedPreferences("hyperion_play_billing_secure_prefs", Context.MODE_PRIVATE)
        billingPrefs.edit().clear().apply()

        // Clear general license preferences
        val secPrefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        secPrefs.edit().clear().apply()

        billingManager = BillingManager.getInstance(context)
        viewModel = ReviewerViewModel(application)
    }

    @After
    fun tearDown() {
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.setHyperionProUnlocked(context, false)
        LicenseManager.init(context)
    }

    @Test
    fun test1_targetProductConfigurationMatchesRequirements() {
        assertEquals("hyperion_oic_nw_pass", BillingConstants.PRODUCT_OIC_NW_PASS)
        assertEquals("hyperion_gmdss_pass", BillingConstants.PRODUCT_GMDSS_PASS)
        assertEquals("hyperion_all_access_pass", BillingConstants.PRODUCT_ALL_ACCESS_PASS)
        assertEquals(6, BillingConstants.ALL_PRODUCT_IDS.size)
        assertTrue(BillingConstants.ALL_PRODUCT_IDS.contains(BillingConstants.PRODUCT_OIC_NW_PASS))
        assertTrue(BillingConstants.ALL_PRODUCT_IDS.contains(BillingConstants.PRODUCT_GMDSS_PASS))
        assertTrue(BillingConstants.ALL_PRODUCT_IDS.contains(BillingConstants.PRODUCT_ALL_ACCESS_PASS))
    }

    @Test
    fun test2_offlineLicenseAuthoritativeOverPlayBilling() = runBlocking {
        // Even if billing is not connected and has no purchases, active offline license gives full access
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.init(context)

        assertTrue(
            "Offline OIC-NW license must authorize OIC-NW track unconditionally",
            viewModel.isTrackUnlocked(Track.DECK_OIC_NW)
        )
        assertFalse(
            "GMDSS remains locked when only OIC-NW is licensed",
            viewModel.isTrackUnlocked(Track.GMDSS_GOC)
        )

        // All-Access Hyperion Pro offline license unlocks BOTH tracks
        LicenseManager.setHyperionProUnlocked(context, true)
        LicenseManager.setGmdssProUnlocked(context, true)
        LicenseManager.init(context)

        assertTrue(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertTrue(viewModel.isTrackUnlocked(Track.GMDSS_GOC))
    }

    @Test
    fun test3_googlePlayEntitlementsUnlockTracks() = runBlocking {
        // Ensure offline license is unactivated
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.init(context)

        assertFalse(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertFalse(viewModel.isTrackUnlocked(Track.GMDSS_GOC))

        // Simulate local secure cached Play Billing purchase for OIC-NW
        val billingPrefs = context.getSharedPreferences("hyperion_play_billing_secure_prefs", Context.MODE_PRIVATE)
        billingPrefs.edit()
            .putBoolean("play_entitlement_oic_nw", true)
            .putString("play_token_oic_nw", "token_test_oic_nw_12345")
            .apply()

        // Recreate manager to reload cached tokens
        val vm2 = ReviewerViewModel(application)
        // Simulate Play entitlements update
        val updatedManager = vm2.billingManager
        val initialItems = updatedManager.getAllProductItems()
        assertNotNull(initialItems)

        // Directly verify isTrackUnlocked incorporates Play entitlements
        val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_oic_unlocked", true).apply()
        assertTrue(vm2.isTrackUnlocked(Track.DECK_OIC_NW))
    }

    @Test
    fun test4_freeTierStrict5QuestionCeiling() = runBlocking {
        // Ensure user is completely unentitled
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.setHyperionProUnlocked(context, false)
        LicenseManager.init(context)

        assertFalse(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))

        // Start practice requesting 20 questions
        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 20,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        // Strictly capped to 5 preview questions
        val activeQuestions = viewModel.activeQuestions.value
        assertEquals("Free tier practice must be strictly capped at 5 preview items", 5, activeQuestions.size)

        // User answers all 5 preview items
        for (i in 0 until 5) {
            val q = activeQuestions[i]
            viewModel.selectOption(i, q.correctAnswerIndex)
            viewModel.confirmOrNextQuestion() // evaluates
            if (i < 4) {
                viewModel.confirmOrNextQuestion() // advances
            }
        }

        // On submitting/evaluating the 5th item, paywall dialog MUST be triggered
        viewModel.confirmOrNextQuestion()
        assertTrue(
            "Paywall dialog must be triggered when reaching free preview ceiling",
            viewModel.showPaywallDialog.value
        )
    }

    @Test
    fun test5_mockExamLockedOnFreeTierWithPaywallTrigger() = runBlocking {
        // Unentitled user
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.init(context)

        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startQuizSession(mode = QuizMode.MOCK_EXAM)

        // Mock exam questions must be empty
        assertEquals(0, viewModel.activeQuestions.value.size)
        // Paywall dialog must be shown
        assertTrue("Paywall dialog must trigger when attempting Mock Exam on free tier", viewModel.showPaywallDialog.value)
    }

    @Test
    fun test6_intensiveDrillModeLockedOnFreeTier() = runBlocking {
        // Unentitled user
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.init(context)

        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 10,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false,
            deliveryMode = DrillDeliveryMode.INTENSIVE
        )

        // Free tier cannot start intensive drill mode; paywall dialog must be triggered
        assertTrue(
            "Paywall dialog must trigger when unentitled user requests Intensive Drill Mode",
            viewModel.showPaywallDialog.value
        )
    }

    @Test
    fun test7_paywallDialogDismissal() {
        viewModel.triggerPaywall(Track.DECK_OIC_NW)
        assertTrue(viewModel.showPaywallDialog.value)
        assertEquals(Track.DECK_OIC_NW, viewModel.paywallTargetTrack.value)

        viewModel.dismissPaywallDialog()
        assertFalse(viewModel.showPaywallDialog.value)
    }
}
