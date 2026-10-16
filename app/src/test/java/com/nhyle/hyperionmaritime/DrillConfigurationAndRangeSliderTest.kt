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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DrillConfigurationAndRangeSliderTest {

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
        LicenseManager.setActivated(context, true)
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.setGmdssProUnlocked(context, true)

        database = HyperionDatabase.getInstance(context)
        repository = ReviewerRepository(database)
        viewModel = ReviewerViewModel(application)
    }

    @Test
    fun testDynamicQuestionCountsForOicNwCompetencies() {
        val competencies = MaritimeCompetenceRegistry.ALL_DECK_COMPETENCES
        assertEquals("There should be 12 OIC-NW Deck competencies", 12, competencies.size)

        for (comp in competencies) {
            val dynamicCount = MaritimeCompetenceRegistry.getDynamicQuestionCount(
                competenceId = comp.id,
                track = Track.DECK_OIC_NW,
                isCrpMode = false
            )
            assertTrue("Dynamic count for ${comp.id} must be positive", dynamicCount > 0)

            val repoCount = repository.getQuestionCountForCompetence("DECK_OIC_NW", comp.id)
            assertEquals("Repo count must match registry dynamic count for ${comp.id}", dynamicCount, repoCount)
        }

        val totalDynamic = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.DECK_OIC_NW, isCrpMode = false)
        assertTrue("Total dynamic question count for OIC-NW must exceed CRP count (2,065)", totalDynamic >= 2065)
    }

    @Test
    fun testMarinaCrpModePreservationCounts() {
        // C1 Deck in MARINA CRP must be exactly 420
        val c1Crp = MaritimeCompetenceRegistry.getOfficialCrpQuestionCount("C1", Track.DECK_OIC_NW)
        assertEquals("C1 CRP count must be exactly 420", 420, c1Crp)

        // C2 Deck in MARINA CRP must be exactly 221
        val c2Crp = MaritimeCompetenceRegistry.getOfficialCrpQuestionCount("C2", Track.DECK_OIC_NW)
        assertEquals("C2 CRP count must be exactly 221", 221, c2Crp)

        // Total Deck CRP distribution sum across 12 competencies must be exactly 2,065
        val sumDeckCrp = MaritimeCompetenceRegistry.ALL_DECK_COMPETENCES.sumOf {
            MaritimeCompetenceRegistry.getOfficialCrpQuestionCount(it.id, Track.DECK_OIC_NW)
        }
        assertEquals("Total Deck CRP question count must be exactly 2,065", 2065, sumDeckCrp)

        val totalCrpTrack = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.DECK_OIC_NW, isCrpMode = true)
        assertEquals("getTotalTrackQuestionCount in CRP mode must return 2,065", 2065, totalCrpTrack)
    }

    @Test
    fun testGmdssCompetenceCounts() {
        val c1Gmdss = MaritimeCompetenceRegistry.getDynamicQuestionCount("C1", Track.GMDSS_GOC, isCrpMode = false)
        assertEquals("GMDSS C1 must have 1,911 items", 1911, c1Gmdss)

        val c2Gmdss = MaritimeCompetenceRegistry.getDynamicQuestionCount("C2", Track.GMDSS_GOC, isCrpMode = false)
        assertEquals("GMDSS C2 must have 349 items", 349, c2Gmdss)

        val totalGmdss = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.GMDSS_GOC, isCrpMode = false)
        assertEquals("GMDSS Total must be 2,260 items", 2260, totalGmdss)
    }

    @Test
    fun testRangeSliderClampingAndCountMath() {
        val maxAvailable = 100

        // Test standard range
        val start1 = 1
        val end1 = 50
        val count1 = (end1 - start1 + 1).coerceAtLeast(1)
        assertEquals("Range 1 to 50 should have 50 items", 50, count1)

        // Test subrange
        val start2 = 25
        val end2 = 50
        val count2 = (end2 - start2 + 1).coerceAtLeast(1)
        assertEquals("Range 25 to 50 should have 26 items", 26, count2)

        // Test clamping: negative start clamped to 1
        val rawNegativeStart = -5
        val clampedStart = rawNegativeStart.coerceIn(1, maxAvailable)
        assertEquals("Negative start must clamp to 1", 1, clampedStart)

        // Test clamping: end larger than max clamped to maxAvailable
        val rawLargeEnd = 250
        val clampedEnd = rawLargeEnd.coerceIn(clampedStart, maxAvailable)
        assertEquals("End exceeding max must clamp to 100", 100, clampedEnd)

        // Test clamping: start cannot exceed end
        val startExcess = 80
        val endExcess = 60
        val clampedStartExcess = startExcess.coerceIn(1, endExcess)
        assertEquals("Start exceeding end must be clamped to end", 60, clampedStartExcess)
    }

    @Test
    fun testConfiguredPracticeQuestionSlicingSequential() {
        LicenseManager.setMasterProUnlocked(ApplicationProvider.getApplicationContext(), true)
        viewModel.setTrack(Track.DECK_OIC_NW)
        val allC1Questions = MaritimeQuestionBank.getQuestionsByCompetence("C1", Track.DECK_OIC_NW, isProUnlocked = true)
        assertTrue("C1 should have questions", allC1Questions.size >= 20)

        // Slice questions from item #5 to item #15 (11 questions total)
        val startItem = 5
        val endItem = 15
        val expectedCount = endItem - startItem + 1

        viewModel.startConfiguredPractice(
            competenceId = "C1",
            deliveryMode = DrillDeliveryMode.STANDARD,
            startItem = startItem,
            endItem = endItem,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        val activeQuestions = viewModel.activeQuestions.value
        assertEquals("Active questions size must equal sliced range size", expectedCount, activeQuestions.size)

        // Verify sequential alignment (1-based to 0-based index)
        for (i in 0 until expectedCount) {
            val expectedQuestion = allC1Questions[startItem - 1 + i]
            assertEquals(
                "Question at index $i must match sequential slice",
                expectedQuestion.id,
                activeQuestions[i].id
            )
        }
    }

    @Test
    fun testConfiguredPracticeQuestionSlicingWithShuffle() {
        LicenseManager.setMasterProUnlocked(ApplicationProvider.getApplicationContext(), true)
        viewModel.setTrack(Track.DECK_OIC_NW)
        val allC2Questions = MaritimeQuestionBank.getQuestionsByCompetence("C2", Track.DECK_OIC_NW, isProUnlocked = true)
        assertTrue("C2 should have questions", allC2Questions.size >= 10)

        val startItem = 1
        val endItem = 10
        val expectedCount = 10

        viewModel.startConfiguredPractice(
            competenceId = "C2",
            deliveryMode = DrillDeliveryMode.STANDARD,
            startItem = startItem,
            endItem = endItem,
            shuffleQuestions = true,
            shuffleChoices = false,
            isCrpMode = false
        )

        val activeQuestions = viewModel.activeQuestions.value
        assertEquals("Active questions size must be 10", expectedCount, activeQuestions.size)

        val allowedIds = allC2Questions.subList(0, 10).map { it.id }.toSet()
        for (q in activeQuestions) {
            assertTrue("Every question must come from the selected range slice", allowedIds.contains(q.id))
        }
    }

    @Test
    fun testConfiguredPracticeMarinaCrpModePreservation() {
        viewModel.setTrack(Track.DECK_OIC_NW)

        viewModel.startConfiguredPractice(
            competenceId = "C1",
            deliveryMode = DrillDeliveryMode.STANDARD,
            startItem = 1,
            endItem = 10, // In CRP mode, endItem range slicing is bypassed; delivers full pool
            shuffleQuestions = false,
            shuffleChoices = true,
            isCrpMode = true
        )

        assertTrue("CRP mode flag must be active", viewModel.isCrpMode.value)
        val activeQuestions = viewModel.activeQuestions.value
        // In CRP mode, C1 delivers the official 420 questions
        assertEquals("CRP mode must preserve full pool of 420 questions for C1", 420, activeQuestions.size)
    }
}
