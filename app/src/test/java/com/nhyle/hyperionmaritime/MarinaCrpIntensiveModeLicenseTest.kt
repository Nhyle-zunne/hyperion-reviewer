package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.ReviewerRepository
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.viewmodel.DrillDeliveryMode
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen
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
@Config(sdk = [36])
class MarinaCrpIntensiveModeLicenseTest {

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var database: HyperionDatabase
    private lateinit var repository: ReviewerRepository
    private lateinit var viewModel: ReviewerViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        MaritimeQuestionBank.init(context)

        // Reset license state before each test
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.init(context)

        database = HyperionDatabase.getInstance(context)
        repository = ReviewerRepository(database)
        viewModel = ReviewerViewModel(application)
    }

    @Test
    fun testMarinaCrpIntensiveModeDynamicallyUnlockedWhenLicensedOicNw() {
        viewModel.setTrack(Track.DECK_OIC_NW)

        // Initially unlicensed
        assertFalse("OIC-NW should initially be unlicensed", viewModel.isTrackProUnlocked(Track.DECK_OIC_NW))

        // Grant OIC-NW license
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.init(context)
        assertTrue("OIC-NW must now report unlocked", viewModel.isTrackProUnlocked(Track.DECK_OIC_NW))

        // Launch MARINA CRP Practice in Intensive Mode for C1 (420 questions)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            deliveryMode = DrillDeliveryMode.INTENSIVE,
            shuffleQuestions = false,
            shuffleChoices = true,
            isCrpMode = true
        )

        assertTrue("CRP mode must be active", viewModel.isCrpMode.value)
        assertEquals("Active drill delivery mode must be INTENSIVE", DrillDeliveryMode.INTENSIVE, viewModel.activeDrillDeliveryMode.value)
        assertEquals("Screen must navigate to Quiz", Screen.Quiz, viewModel.currentScreen.value)

        val queue = viewModel.intensiveQueue.value
        assertEquals("Intensive queue must be loaded with all 420 C1 CRP questions", 420, queue.size)
        assertEquals("Initial total must be 420", 420, viewModel.intensiveInitialTotal.value)
        assertEquals("Initial cleared count must be 0", 0, viewModel.intensiveClearedCount.value)
        assertFalse("Intensive completed must be false at start", viewModel.intensiveCompleted.value)
    }

    @Test
    fun testMarinaCrpIntensiveModeDynamicallyUnlockedWhenLicensedGmdss() {
        viewModel.setTrack(Track.GMDSS_GOC)

        // Grant GMDSS license
        LicenseManager.setGmdssProUnlocked(context, true)
        LicenseManager.init(context)
        assertTrue("GMDSS must report unlocked", viewModel.isTrackProUnlocked(Track.GMDSS_GOC))

        // Launch MARINA CRP Practice in Intensive Mode for GMDSS C1
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            deliveryMode = DrillDeliveryMode.INTENSIVE,
            shuffleQuestions = false,
            shuffleChoices = true,
            isCrpMode = true
        )

        assertTrue("CRP mode must be active", viewModel.isCrpMode.value)
        assertEquals("Active delivery mode must be INTENSIVE", DrillDeliveryMode.INTENSIVE, viewModel.activeDrillDeliveryMode.value)
        assertTrue("Intensive queue must contain questions", viewModel.intensiveQueue.value.isNotEmpty())
        assertEquals("Cleared count starts at 0", 0, viewModel.intensiveClearedCount.value)
    }

    @Test
    fun testMarinaCrpIntensiveModeDynamicallyUnlockedWhenAllAccessLicensed() {
        viewModel.setTrack(Track.DECK_OIC_NW)

        // Grant Full Activation / All-Access
        LicenseManager.setActivated(context, true)
        LicenseManager.init(context)
        assertTrue("Track must be unlocked when All-Access is active", viewModel.isTrackProUnlocked(Track.DECK_OIC_NW))

        viewModel.startConfiguredPractice(
            competenceId = "C2",
            deliveryMode = DrillDeliveryMode.INTENSIVE,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = true
        )

        assertEquals("Delivery mode must be INTENSIVE", DrillDeliveryMode.INTENSIVE, viewModel.activeDrillDeliveryMode.value)
        // C2 Deck in CRP has 221 questions
        val expectedC2Count = MaritimeCompetenceRegistry.getOfficialCrpQuestionCount("C2", Track.DECK_OIC_NW)
        assertEquals(221, expectedC2Count)
        assertEquals("Queue must have 221 questions for C2", 221, viewModel.intensiveQueue.value.size)
    }

    @Test
    fun testIntensiveQueueRecyclingLogicInMarinaCrpMode() {
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.init(context)
        viewModel.setTrack(Track.DECK_OIC_NW)

        viewModel.startConfiguredPractice(
            competenceId = "C1",
            deliveryMode = DrillDeliveryMode.INTENSIVE,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = true
        )

        val initialQueue = viewModel.intensiveQueue.value
        val initialSize = initialQueue.size
        assertTrue("Queue must have items", initialSize > 2)

        val firstQuestion = initialQueue[0]
        val secondQuestion = initialQueue[1]

        // 1. Answer first question correctly
        viewModel.selectIntensiveOption(firstQuestion.correctAnswerIndex)
        val stateAfterSelect = viewModel.intensiveCurrentQuestionState.value
        assertNotNull(stateAfterSelect)
        assertEquals(firstQuestion.correctAnswerIndex, stateAfterSelect?.selectedIndex)
        assertTrue(stateAfterSelect?.isSubmitted == true)

        viewModel.advanceIntensiveQuestion()

        // Verify correct answer removes item from queue and increments cleared count
        assertEquals("Queue size should decrease by 1", initialSize - 1, viewModel.intensiveQueue.value.size)
        assertEquals("Cleared count should be 1", 1, viewModel.intensiveClearedCount.value)
        assertEquals("New head of queue must be second question", secondQuestion.id, viewModel.intensiveQueue.value.first().id)

        // 2. Answer new head question INCORRECTLY
        val currentHead = viewModel.intensiveQueue.value.first()
        val wrongOption = if (currentHead.correctAnswerIndex == 0) 1 else 0
        viewModel.selectIntensiveOption(wrongOption)

        viewModel.advanceIntensiveQuestion()

        // Verify wrong answer recycles item to the end of the queue without incrementing cleared count
        assertEquals("Queue size should remain same after wrong answer", initialSize - 1, viewModel.intensiveQueue.value.size)
        assertEquals("Cleared count should still be 1", 1, viewModel.intensiveClearedCount.value)
        assertEquals("Recycled question must be placed at the very end of queue", currentHead.id, viewModel.intensiveQueue.value.last().id)
    }

    @Test
    fun testStandardDrillPreservedInMarinaCrpMode() {
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.init(context)
        viewModel.setTrack(Track.DECK_OIC_NW)

        // Launch standard mode in CRP
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            deliveryMode = DrillDeliveryMode.STANDARD,
            shuffleQuestions = false,
            shuffleChoices = true,
            isCrpMode = true
        )

        assertEquals(DrillDeliveryMode.STANDARD, viewModel.activeDrillDeliveryMode.value)
        assertEquals("Standard delivery has all 420 C1 questions in activeQuestions", 420, viewModel.activeQuestions.value.size)
        assertTrue("User question states map must be populated for standard drill", viewModel.userQuestionStates.value.size == 420)
    }

    @Test
    fun testUnlicensedStatePreventsIntensiveQueueInitialization() {
        // Ensure user is completely unactivated
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.init(context)
        viewModel.setTrack(Track.DECK_OIC_NW)

        assertFalse("User is unlicensed", viewModel.isTrackProUnlocked(Track.DECK_OIC_NW))

        // Attempting to start configured practice with intensive mode while unlicensed
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            deliveryMode = DrillDeliveryMode.INTENSIVE,
            isCrpMode = true
        )

        // Since !isPro, intensive queue is NOT initialized
        assertTrue("Intensive queue must be empty for unlicensed user", viewModel.intensiveQueue.value.isEmpty())
        // Instead falls back to activeQuestions for reference delivery
        assertEquals(420, viewModel.activeQuestions.value.size)
    }
}
