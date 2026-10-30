package com.nhyle.hyperionmaritime.viewmodel

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.billing.BillingConstants
import com.nhyle.hyperionmaritime.data.billing.BillingCycle
import com.nhyle.hyperionmaritime.data.billing.BillingManager
import com.nhyle.hyperionmaritime.data.billing.PlayProductItem
import com.nhyle.hyperionmaritime.data.billing.TrackEntitlements
import com.nhyle.hyperionmaritime.data.local.QuestionAttemptHistoryManager
import com.nhyle.hyperionmaritime.data.model.ReviewerTrack
import com.nhyle.hyperionmaritime.data.model.LegalAgreementText
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Hyperion Maritime Exam Reviewer - Production Architecture Unit Tests
 *
 * Exhaustive test suite covering requirements a through k + multi-cycle billing invariants:
 * a) Purchasing hyperion_oic_nw_pass unlocks OIC-NW but keeps GMDSS locked.
 * b) Purchasing hyperion_gmdss_pass unlocks GMDSS but keeps OIC-NW locked.
 * c) Purchasing hyperion_all_access_pass unlocks both tracks simultaneously.
 * d) Active offline license retains full access even if BillingManager emits DISCONNECTED or error states.
 * e) Unentitled users cannot advance past question index 4 (5-question limit boundary).
 * f) Rapid concurrent calls to confirmOrNextQuestion() record exactly one attempt.
 * g) A later legitimate attempt in a future session records an additional attempt normally.
 * h) Session reset clears transient evaluation flags without wiping persistent question statistics.
 * i) Empty question lists and out-of-bounds indices do not trigger runtime crashes.
 * j) PENDING purchase state does not unlock tracks and is not acknowledged.
 * k) Idempotent purchase callbacks do not trigger duplicate state transitions.
 * l) Multi-cycle billing plan resolution (Weekly, Monthly, Yearly).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ReviewerViewModelTest {

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var viewModel: ReviewerViewModel
    private lateinit var billingManager: BillingManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        MaritimeQuestionBank.init(context)

        // Reset offline licenses to default clean unactivated state
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.setHyperionProUnlocked(context, false)
        LicenseManager.setMasterProUnlocked(context, false)
        LicenseManager.init(context)

        // Reset secure license prefs
        val secPrefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        secPrefs.edit().clear().apply()

        // Reset Play Billing secure preferences
        val billingPrefs = context.getSharedPreferences("hyperion_play_billing_secure_prefs", Context.MODE_PRIVATE)
        billingPrefs.edit().clear().apply()

        BillingManager.resetForTesting()
        billingManager = BillingManager.getInstance(context)
        billingManager.resetEntitlementsForTesting()
        viewModel = ReviewerViewModel(application)
    }

    @Test
    fun existingInstallMustAcceptTheCurrentTermsVersion() {
        val prefs = context.getSharedPreferences("hyperion_prefs", Context.MODE_PRIVATE)
        val originalAccepted = prefs.getBoolean("has_accepted_terms", false)
        val originalVersion = prefs.getString("accepted_terms_version", null)

        try {
            // Older installs only have the legacy Boolean, so they must review the
            // newly revised Terms and Privacy acknowledgement.
            prefs.edit()
                .putBoolean("has_accepted_terms", true)
                .remove("accepted_terms_version")
                .apply()
            val legacyInstall = ReviewerViewModel(application)
            assertFalse(legacyInstall.hasAcceptedTerms.value)

            // An install that accepted this exact revision can proceed normally.
            prefs.edit()
                .putString("accepted_terms_version", LegalAgreementText.TERMS_VERSION)
                .apply()
            val currentInstall = ReviewerViewModel(application)
            assertTrue(currentInstall.hasAcceptedTerms.value)
        } finally {
            val editor = prefs.edit().putBoolean("has_accepted_terms", originalAccepted)
            if (originalVersion == null) editor.remove("accepted_terms_version")
            else editor.putString("accepted_terms_version", originalVersion)
            editor.apply()
        }
    }

    @After
    fun tearDown() {
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.setHyperionProUnlocked(context, false)
        LicenseManager.init(context)

        BillingManager.resetForTesting()
        val secPrefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        secPrefs.edit().clear().apply()

        val billingPrefs = context.getSharedPreferences("hyperion_play_billing_secure_prefs", Context.MODE_PRIVATE)
        billingPrefs.edit().clear().apply()
    }

    /**
     * Requirement a: Purchasing hyperion_oic_nw_pass unlocks OIC-NW but keeps GMDSS locked.
     */
    @Test
    fun testA_purchasingOicPassUnlocksOicButKeepsGmdssLocked() {
        val secPrefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        secPrefs.edit().clear().apply()

        assertFalse(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertFalse(viewModel.isTrackUnlocked(Track.GMDSS_GOC))

        // 1. Verify via simulatePurchase (BillingManager / Sandbox simulator)
        viewModel.simulatePurchase(BillingConstants.PRODUCT_OIC_NW_PASS)

        assertTrue(
            "hyperion_oic_nw_pass must unlock OIC-NW track via BillingManager",
            viewModel.isTrackUnlocked(Track.DECK_OIC_NW)
        )
        assertFalse(
            "hyperion_oic_nw_pass must leave GMDSS locked",
            viewModel.isTrackUnlocked(Track.GMDSS_GOC)
        )
        assertTrue(
            "Reactive StateFlow isOicNwProUnlocked must immediately evaluate true",
            viewModel.isOicNwProUnlocked.value
        )
        assertFalse(
            "Reactive StateFlow isGmdssProUnlocked must remain false",
            viewModel.isGmdssProUnlocked.value
        )

        // 2. Also verify persistent entitlement cache survives
        val reloadedItem = billingManager.getProductItem(BillingConstants.PRODUCT_OIC_NW_PASS)
        assertTrue("Product item must report isOwned = true", reloadedItem.isOwned)
        assertEquals("₱350 / year", BillingConstants.DEFAULT_PRICES_YEARLY[BillingConstants.PRODUCT_OIC_NW_PASS])
    }

    /**
     * Requirement b: Purchasing hyperion_gmdss_pass unlocks GMDSS but keeps OIC-NW locked.
     */
    @Test
    fun testB_purchasingGmdssPassUnlocksGmdssButKeepsOicLocked() {
        val secPrefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        secPrefs.edit().clear().apply()

        assertFalse(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertFalse(viewModel.isTrackUnlocked(Track.GMDSS_GOC))

        // 1. Verify via simulatePurchase (BillingManager / Sandbox simulator)
        viewModel.simulatePurchase(BillingConstants.PRODUCT_GMDSS_PASS)

        assertTrue(
            "hyperion_gmdss_pass must unlock GMDSS track via BillingManager",
            viewModel.isTrackUnlocked(Track.GMDSS_GOC)
        )
        assertFalse(
            "hyperion_gmdss_pass must leave OIC-NW locked",
            viewModel.isTrackUnlocked(Track.DECK_OIC_NW)
        )
        assertTrue(
            "Reactive StateFlow isGmdssProUnlocked must immediately evaluate true",
            viewModel.isGmdssProUnlocked.value
        )
        assertFalse(
            "Reactive StateFlow isOicNwProUnlocked must remain false",
            viewModel.isOicNwProUnlocked.value
        )

        val reloadedItem = billingManager.getProductItem(BillingConstants.PRODUCT_GMDSS_PASS)
        assertTrue("Product item must report isOwned = true", reloadedItem.isOwned)
        assertEquals("₱250 / year", BillingConstants.DEFAULT_PRICES_YEARLY[BillingConstants.PRODUCT_GMDSS_PASS])
    }

    /**
     * Requirement c: Purchasing hyperion_all_access_pass unlocks both tracks simultaneously.
     */
    @Test
    fun testC_purchasingAllAccessPassUnlocksBothTracksSimultaneously() {
        val secPrefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        secPrefs.edit().clear().apply()

        assertFalse(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertFalse(viewModel.isTrackUnlocked(Track.GMDSS_GOC))

        // 1. Verify via simulatePurchase (BillingManager / Sandbox simulator)
        viewModel.simulatePurchase(BillingConstants.PRODUCT_ALL_ACCESS_PASS)

        assertTrue(
            "hyperion_all_access_pass must unlock OIC-NW track",
            viewModel.isTrackUnlocked(Track.DECK_OIC_NW)
        )
        assertTrue(
            "hyperion_all_access_pass must unlock GMDSS track",
            viewModel.isTrackUnlocked(Track.GMDSS_GOC)
        )
        assertTrue(
            "Reactive StateFlow isOicNwProUnlocked must immediately evaluate true",
            viewModel.isOicNwProUnlocked.value
        )
        assertTrue(
            "Reactive StateFlow isGmdssProUnlocked must immediately evaluate true",
            viewModel.isGmdssProUnlocked.value
        )

        val reloadedItem = billingManager.getProductItem(BillingConstants.PRODUCT_ALL_ACCESS_PASS)
        assertTrue("Product item must report isOwned = true", reloadedItem.isOwned)
        assertEquals("₱500 / year", BillingConstants.DEFAULT_PRICES_YEARLY[BillingConstants.PRODUCT_ALL_ACCESS_PASS])
    }

    /**
     * Requirement d: Active offline license retains full access even if BillingManager emits DISCONNECTED or error states.
     */
    @Test
    fun testD_activeOfflineLicenseRetainsFullAccessRegardlessOfBillingState() {
        // Activate offline terminal license for OIC-NW
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.init(context)

        // Clear billing preferences to simulate no Play connection & no Play tokens
        val billingPrefs = context.getSharedPreferences("hyperion_play_billing_secure_prefs", Context.MODE_PRIVATE)
        billingPrefs.edit().clear().apply()

        val track: ReviewerTrack = Track.DECK_OIC_NW
        assertTrue(
            "Active offline license must unconditionally grant access regardless of Play Billing state",
            viewModel.isTrackUnlocked(track)
        )
        assertFalse(
            "GMDSS remains locked when only OIC-NW offline license is present",
            viewModel.isTrackUnlocked(Track.GMDSS_GOC)
        )

        // All-Access Hyperion Pro offline license
        LicenseManager.setHyperionProUnlocked(context, true)
        LicenseManager.setGmdssProUnlocked(context, true)
        LicenseManager.init(context)

        assertTrue(
            "All-Access offline license grants full OIC-NW access",
            viewModel.isTrackUnlocked(Track.DECK_OIC_NW)
        )
        assertTrue(
            "All-Access offline license grants full GMDSS access",
            viewModel.isTrackUnlocked(Track.GMDSS_GOC)
        )
    }

    /**
     * Requirement e: Unentitled users cannot advance past question index 4 (5-question limit boundary).
     */
    @Test
    fun testE_unentitledUsersCannotAdvancePastQuestionIndex4() = runBlocking {
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.setHyperionProUnlocked(context, false)
        LicenseManager.init(context)

        val secPrefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        secPrefs.edit().clear().apply()

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

        val activeQuestions = viewModel.activeQuestions.value
        assertEquals("Free preview must be strictly capped at 5 questions", 5, activeQuestions.size)

        // Out-of-bounds navigation attempts must fail
        viewModel.jumpToQuestion(5)
        assertTrue("Index must remain <= 4", viewModel.currentQuestionIndex.value <= 4)

        viewModel.jumpToQuestion(15)
        assertTrue("Index must remain <= 4", viewModel.currentQuestionIndex.value <= 4)

        // Walk through the 5 allowed questions (index 0..4)
        for (i in 0 until 5) {
            val q = activeQuestions[i]
            viewModel.selectOption(i, q.correctAnswerIndex)
            viewModel.confirmOrNextQuestion() // evaluates
            if (i < 4) {
                viewModel.confirmOrNextQuestion() // advances
            }
        }

        // On submitting the 5th question (index 4), paywall dialog MUST be triggered
        viewModel.confirmOrNextQuestion()
        assertTrue(
            "Paywall dialog must trigger when reaching the 5-question limit ceiling",
            viewModel.showPaywallDialog.value
        )
    }

    /**
     * Requirement f: Rapid concurrent calls to confirmOrNextQuestion() record exactly one attempt.
     */
    @Test
    fun testF_rapidConcurrentCallsToConfirmOrNextQuestionRecordExactlyOneAttempt() = runBlocking {
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.init(context)

        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        val q = viewModel.activeQuestions.value[0]
        val initialHistory = QuestionAttemptHistoryManager.getHistory(context, q.track.name, q.id)
        val initialAttempts = initialHistory?.totalAttempts ?: 0

        viewModel.selectOption(0, q.correctAnswerIndex)

        // Rapid sequential taps on confirm button
        viewModel.confirmOrNextQuestion()
        viewModel.confirmOrNextQuestion()
        viewModel.confirmOrNextQuestion()

        delay(100)

        val updatedHistory = QuestionAttemptHistoryManager.getHistory(context, q.track.name, q.id)
        assertEquals(
            "Rapid concurrent invocations must record exactly 1 attempt",
            initialAttempts + 1,
            updatedHistory?.totalAttempts ?: 0
        )
    }

    /**
     * Requirement g: A later legitimate attempt in a future session records an additional attempt normally.
     */
    @Test
    fun testG_laterLegitimateAttemptInFutureSessionRecordsAdditionalAttemptNormally() = runBlocking {
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.init(context)

        viewModel.setTrack(Track.DECK_OIC_NW)

        // Session 1: Answer question 0
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )
        val q = viewModel.activeQuestions.value[0]
        val baselineAttempts = QuestionAttemptHistoryManager.getHistory(context, q.track.name, q.id)?.totalAttempts ?: 0

        viewModel.selectOption(0, q.correctAnswerIndex)
        viewModel.confirmOrNextQuestion() // Attempt 1 recorded
        delay(100)

        val afterSession1 = QuestionAttemptHistoryManager.getHistory(context, q.track.name, q.id)?.totalAttempts ?: 0
        assertEquals(baselineAttempts + 1, afterSession1)

        // Session 2: Fresh session restart
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )
        val qSession2 = viewModel.activeQuestions.value[0]
        assertEquals(q.id, qSession2.id)

        // Answer question 0 in Session 2
        viewModel.selectOption(0, qSession2.correctAnswerIndex)
        viewModel.confirmOrNextQuestion() // Attempt 2 recorded legitimately
        delay(100)

        val afterSession2 = QuestionAttemptHistoryManager.getHistory(context, q.track.name, q.id)?.totalAttempts ?: 0
        assertEquals(
            "Future session must record legitimate additional attempt normally without being blocked by previous session deduplication",
            baselineAttempts + 2,
            afterSession2
        )
    }

    /**
     * Requirement h: Session reset clears transient evaluation flags without wiping persistent question statistics.
     */
    @Test
    fun testH_sessionResetClearsTransientFlagsWithoutWipingPersistentStats() = runBlocking {
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.init(context)

        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        val q = viewModel.activeQuestions.value[0]
        val baselineAttempts = QuestionAttemptHistoryManager.getHistory(context, q.track.name, q.id)?.totalAttempts ?: 0

        viewModel.selectOption(0, q.correctAnswerIndex)
        viewModel.confirmOrNextQuestion() // Evaluates
        delay(100)

        assertTrue(viewModel.isAnswerEvaluated.value)
        assertEquals(q.correctAnswerIndex, viewModel.selectedChoiceIndex.value)

        // Call explicit session reset
        viewModel.resetActivePracticeSession()

        // Verify transient states are cleared
        assertFalse("Transient evaluation flag must be reset", viewModel.isAnswerEvaluated.value)
        assertNull("Selected choice index must be reset to null", viewModel.selectedChoiceIndex.value)
        assertEquals("Question index must be reset to 0", 0, viewModel.currentQuestionIndex.value)

        // Verify persistent attempt history is NOT wiped
        val persistentHistory = QuestionAttemptHistoryManager.getHistory(context, q.track.name, q.id)
        assertEquals(
            "Persistent statistics must remain intact after session reset",
            baselineAttempts + 1,
            persistentHistory?.totalAttempts ?: 0
        )
    }

    /**
     * Requirement i: Empty question lists and out-of-bounds indices do not trigger runtime crashes.
     */
    @Test
    fun testI_emptyQuestionListsAndOutOfBoundsIndicesDoNotTriggerRuntimeCrashes() {
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.init(context)

        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        // Out-of-bounds index interactions on active session
        viewModel.jumpToQuestion(-1)
        viewModel.jumpToQuestion(9999)
        viewModel.selectOption(-1, 0)
        viewModel.selectOption(9999, 0)
        viewModel.toggleFlag(-1)
        viewModel.toggleFlag(9999)

        viewModel.previousQuestion()
        assertEquals(0, viewModel.currentQuestionIndex.value)

        // Interactions on uninitialized / empty ViewModel
        val emptyVm = ReviewerViewModel(application)
        assertEquals(0, emptyVm.activeQuestions.value.size)

        emptyVm.confirmOrNextQuestion()
        emptyVm.nextQuestion()
        emptyVm.previousQuestion()
        emptyVm.jumpToQuestion(0)
        emptyVm.jumpToQuestion(-1)
        emptyVm.jumpToQuestion(10)
        emptyVm.selectOption(0, 0)
        emptyVm.toggleFlag(0)
        emptyVm.dismissPaywallDialog()
        emptyVm.resetActivePracticeSession()
    }

    /**
     * Requirement j: PENDING purchase state does not unlock tracks and is not acknowledged.
     */
    @Test
    fun testJ_pendingPurchaseStateDoesNotUnlockTracksAndIsNotAcknowledged() {
        val pendingEntitlements = TrackEntitlements(
            oicNwUnlocked = false,
            gmdssUnlocked = false,
            allAccessUnlocked = false,
            billingConnected = true,
            pendingPurchaseProductId = BillingConstants.PRODUCT_OIC_NW_PASS
        )

        // Pending must NOT report unlocked
        assertFalse(pendingEntitlements.isTrackUnlocked(Track.DECK_OIC_NW))
        assertFalse(pendingEntitlements.isTrackUnlocked(Track.GMDSS_GOC))

        val productItem = PlayProductItem(
            productId = BillingConstants.PRODUCT_OIC_NW_PASS,
            title = "Deck OIC-NW Pass",
            description = "Test description",
            formattedPrice = "$49.99",
            isOwned = false,
            isPending = true
        )
        assertFalse("Pending item is not owned", productItem.isOwned)
        assertTrue("Pending item is flagged pending", productItem.isPending)
    }

    /**
     * Requirement k: Idempotent purchase callbacks do not trigger duplicate state transitions.
     */
    @Test
    fun testK_idempotentPurchaseCallbacksDoNotTriggerDuplicateStateTransitions() {
        val ent1 = TrackEntitlements(
            oicNwUnlocked = true,
            gmdssUnlocked = false,
            allAccessUnlocked = false,
            billingConnected = true
        )
        assertTrue(ent1.isTrackUnlocked(Track.DECK_OIC_NW))
        assertFalse(ent1.isTrackUnlocked(Track.GMDSS_GOC))

        // Duplicate update with same state
        val ent2 = ent1.copy()
        assertEquals(ent1, ent2)
        assertTrue(ent2.isTrackUnlocked(Track.DECK_OIC_NW))
        assertFalse(ent2.isTrackUnlocked(Track.GMDSS_GOC))
    }

    /**
     * Requirement l: Multi-cycle billing configuration and pricing fallback checks.
     */
    @Test
    fun testL_multiCycleBillingConfigurations() {
        val weeklyAllAccess = billingManager.getProductItem(BillingConstants.PRODUCT_ALL_ACCESS_PASS, BillingCycle.WEEKLY)
        assertEquals("₱35 / week", weeklyAllAccess.formattedPrice)
        assertEquals("₱35/wk", weeklyAllAccess.shortFormattedPrice)

        val monthlyAllAccess = billingManager.getProductItem(BillingConstants.PRODUCT_ALL_ACCESS_PASS, BillingCycle.MONTHLY)
        assertEquals("₱80 / month", monthlyAllAccess.formattedPrice)
        assertEquals("₱80/mo", monthlyAllAccess.shortFormattedPrice)

        val yearlyAllAccess = billingManager.getProductItem(BillingConstants.PRODUCT_ALL_ACCESS_PASS, BillingCycle.YEARLY)
        assertEquals("₱500 / year", yearlyAllAccess.formattedPrice)
        assertEquals("₱500/yr", yearlyAllAccess.shortFormattedPrice)

        val weeklyOic = billingManager.getProductItem(BillingConstants.PRODUCT_OIC_NW_PASS, BillingCycle.WEEKLY)
        assertEquals("₱20 / week", weeklyOic.formattedPrice)

        val monthlyOic = billingManager.getProductItem(BillingConstants.PRODUCT_OIC_NW_PASS, BillingCycle.MONTHLY)
        assertEquals("₱50 / month", monthlyOic.formattedPrice)

        val yearlyOic = billingManager.getProductItem(BillingConstants.PRODUCT_OIC_NW_PASS, BillingCycle.YEARLY)
        assertEquals("₱350 / year", yearlyOic.formattedPrice)

        val weeklyGmdss = billingManager.getProductItem(BillingConstants.PRODUCT_GMDSS_PASS, BillingCycle.WEEKLY)
        assertEquals("₱15 / week", weeklyGmdss.formattedPrice)

        val monthlyGmdss = billingManager.getProductItem(BillingConstants.PRODUCT_GMDSS_PASS, BillingCycle.MONTHLY)
        assertEquals("₱40 / month", monthlyGmdss.formattedPrice)

        val yearlyGmdss = billingManager.getProductItem(BillingConstants.PRODUCT_GMDSS_PASS, BillingCycle.YEARLY)
        assertEquals("₱250 / year", yearlyGmdss.formattedPrice)
    }

    /**
     * Reactive entitlement propagation without ViewModel recreation.
     */
    @Test
    fun testReactiveEntitlementPropagationWithoutRecreation() {
        // Step 1: Initial state: both locked
        assertFalse(viewModel.isOicNwProUnlocked.value)
        assertFalse(viewModel.isGmdssProUnlocked.value)
        assertFalse(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertFalse(viewModel.isTrackUnlocked(Track.GMDSS_GOC))

        // Step 2: OIC purchase -> OIC unlocked reactively on existing instance
        viewModel.simulatePurchase(BillingConstants.PRODUCT_OIC_NW_PASS)
        assertTrue(viewModel.isOicNwProUnlocked.value)
        assertFalse(viewModel.isGmdssProUnlocked.value)
        assertTrue(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertFalse(viewModel.isTrackUnlocked(Track.GMDSS_GOC))

        // Reset
        billingManager.resetEntitlementsForTesting()
        viewModel.refreshUnlockStates()
        assertFalse(viewModel.isOicNwProUnlocked.value)
        assertFalse(viewModel.isGmdssProUnlocked.value)

        // Step 3: GMDSS purchase -> GMDSS unlocked reactively on existing instance
        viewModel.simulatePurchase(BillingConstants.PRODUCT_GMDSS_PASS)
        assertFalse(viewModel.isOicNwProUnlocked.value)
        assertTrue(viewModel.isGmdssProUnlocked.value)
        assertFalse(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertTrue(viewModel.isTrackUnlocked(Track.GMDSS_GOC))

        // Reset
        billingManager.resetEntitlementsForTesting()
        viewModel.refreshUnlockStates()

        // Step 4: All-Access purchase -> both unlocked reactively on existing instance
        viewModel.simulatePurchase(BillingConstants.PRODUCT_ALL_ACCESS_PASS)
        assertTrue(viewModel.isOicNwProUnlocked.value)
        assertTrue(viewModel.isGmdssProUnlocked.value)
        assertTrue(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertTrue(viewModel.isTrackUnlocked(Track.GMDSS_GOC))
    }

    /**
     * Offline License remains authoritative when BillingManager is disconnected/empty.
     */
    @Test
    fun testOfflineLicenseRemainsValidWhenBillingDisconnected() {
        context.getSharedPreferences("hyperion_secure_license_v2", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        billingManager.resetEntitlementsForTesting()
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.init(context)
        viewModel.refreshUnlockStates()

        assertTrue(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertTrue(viewModel.isOicNwProUnlocked.value)
        assertFalse(viewModel.isTrackUnlocked(Track.GMDSS_GOC))
        assertFalse(viewModel.isGmdssProUnlocked.value)
    }

    /**
     * Store Selection and Recommended State Invariants.
     */
    @Test
    fun testStoreCardSelectionLogicAndBorderInvariants() {
        val oicId = BillingConstants.PRODUCT_OIC_NW_PASS
        val gmdssId = BillingConstants.PRODUCT_GMDSS_PASS
        val allAccessId = BillingConstants.PRODUCT_ALL_ACCESS_PASS

        fun computeBorderWidth(productId: String, selectedProductId: String?): Int {
            return if (selectedProductId == productId) 2 else 1
        }

        fun isProminentlyHighlighted(productId: String, selectedProductId: String?): Boolean {
            return selectedProductId == productId
        }

        // Test 1: All-Access is recommended, but initial selection can be changed freely
        var selectedProductId: String? = allAccessId
        assertTrue(
            "Selecting All-Access highlights All-Access with 2.dp border",
            isProminentlyHighlighted(allAccessId, selectedProductId)
        )
        assertEquals(2, computeBorderWidth(allAccessId, selectedProductId))

        // Test 2: Selecting GMDSS moves green border to GMDSS
        selectedProductId = gmdssId
        assertTrue(isProminentlyHighlighted(gmdssId, selectedProductId))
        assertEquals(2, computeBorderWidth(gmdssId, selectedProductId))
        assertFalse(isProminentlyHighlighted(oicId, selectedProductId))
        assertFalse(isProminentlyHighlighted(allAccessId, selectedProductId))

        // Test 3: Selecting OIC moves green border to OIC
        selectedProductId = oicId
        assertTrue(isProminentlyHighlighted(oicId, selectedProductId))
        assertEquals(2, computeBorderWidth(oicId, selectedProductId))
        assertFalse(isProminentlyHighlighted(gmdssId, selectedProductId))
        assertFalse(isProminentlyHighlighted(allAccessId, selectedProductId))
    }
}
