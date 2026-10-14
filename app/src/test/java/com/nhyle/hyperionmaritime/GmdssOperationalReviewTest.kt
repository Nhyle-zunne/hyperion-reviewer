package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GmdssOperationalReviewTest {

    @Test
    fun testC1AndC2UnshuffledPools() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val c1Raw = ProQuestionRepository.loadAllRawCompetenceQuestions(context, "C1").map { it.toQuestion() }
        val c2Raw = ProQuestionRepository.loadAllRawCompetenceQuestions(context, "C2").map { it.toQuestion() }

        val c1Pool = c1Raw.take(MaritimeCompetenceRegistry.OPERATIONAL_REVIEW_LIMIT_C1)
        val c2Pool = c2Raw.take(MaritimeCompetenceRegistry.OPERATIONAL_REVIEW_LIMIT_C2)

        assertEquals(900, c1Pool.size)
        assertEquals(130, c2Pool.size)

        // First and last question IDs
        val c1First = c1Pool.first().id
        val c1Last = c1Pool.last().id
        val c2First = c2Pool.first().id
        val c2Last = c2Pool.last().id

        assertEquals("C1-P1-1", c1First)
        assertEquals("C1-P5-135", c1Last)
        assertEquals("C2-P1-1", c2First)
        assertEquals("C2-P1-130", c2Last)
    }

    @Test
    fun testShuffledPoolIntegrityAndSeedPersistence() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ReviewerViewModel(app)

        val c1Shuffled = viewModel.getSeededShuffledGmdssPool(app, "C1")
        val c2Shuffled = viewModel.getSeededShuffledGmdssPool(app, "C2")

        assertEquals(900, c1Shuffled.size)
        assertEquals(130, c2Shuffled.size)

        // Verify set of IDs matches unshuffled pool exactly (no missing, no duplicates)
        val c1RawIds = ProQuestionRepository.loadAllRawCompetenceQuestions(app, "C1").map { it.toQuestion() }.take(900).map { it.id }.toSet()
        val c2RawIds = ProQuestionRepository.loadAllRawCompetenceQuestions(app, "C2").map { it.toQuestion() }.take(130).map { it.id }.toSet()

        assertEquals(900, c1Shuffled.map { it.id }.toSet().size)
        assertEquals(130, c2Shuffled.map { it.id }.toSet().size)
        assertEquals(c1RawIds, c1Shuffled.map { it.id }.toSet())
        assertEquals(c2RawIds, c2Shuffled.map { it.id }.toSet())

        // Verify two calls with same seed produce identical order
        val c1ShuffledAgain = viewModel.getSeededShuffledGmdssPool(app, "C1")
        assertEquals(c1Shuffled.map { it.id }, c1ShuffledAgain.map { it.id })

        // Verify seed is created once and reused
        val seed = viewModel.getOrGenerateGmdssSeed("C1")
        val seedAgain = viewModel.getOrGenerateGmdssSeed("C1")
        assertEquals(seed, seedAgain)
    }

    @Test
    fun testPracticeAllQuestionsAndDeckPoolsUnchanged() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // GMDSS Practice All Questions total track count
        val totalGmdssCount = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.GMDSS_GOC)
        assertEquals(2260, totalGmdssCount)

        // Deck pool size
        val deckQuestions = MaritimeQuestionBank.getQuestionsByTrack(Track.DECK_OIC_NW)
        assertTrue(deckQuestions.isNotEmpty())
    }

    @Test
    fun testIsolateOperationalReviewFromPracticeAllQuestions() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ReviewerViewModel(app)

        // 1. Verify Operational Review limits
        val opReviewC1 = MaritimeCompetenceRegistry.getDynamicQuestionCount("C1", Track.GMDSS_GOC, isOperationalReview = true)
        val opReviewC2 = MaritimeCompetenceRegistry.getDynamicQuestionCount("C2", Track.GMDSS_GOC, isOperationalReview = true)
        val opReviewTotal = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.GMDSS_GOC, isOperationalReview = true)
        assertEquals(900, opReviewC1)
        assertEquals(130, opReviewC2)
        assertEquals(1030, opReviewTotal)

        // 2. Verify Practice All Questions full question bank (no 900/130 caps)
        val practiceAllC1 = MaritimeCompetenceRegistry.getDynamicQuestionCount("C1", Track.GMDSS_GOC, isOperationalReview = false)
        val practiceAllC2 = MaritimeCompetenceRegistry.getDynamicQuestionCount("C2", Track.GMDSS_GOC, isOperationalReview = false)
        val practiceAllTotal = MaritimeCompetenceRegistry.getTotalTrackQuestionCount(Track.GMDSS_GOC, isOperationalReview = false)
        assertEquals(1911, practiceAllC1)
        assertEquals(349, practiceAllC2)
        assertEquals(2260, practiceAllTotal)

        // 3. Verify Part Selection availability and counts for GMDSS competencies
        val c1Parts = ProQuestionRepository.getPartsForCompetence("C1")
        val c2Parts = ProQuestionRepository.getPartsForCompetence("C2")
        assertEquals(10, c1Parts.size)
        assertEquals((1..10).toList(), c1Parts)
        assertEquals(2, c2Parts.size)
        assertEquals((1..2).toList(), c2Parts)

        val c1PartsTotal = c1Parts.sumOf { ProQuestionRepository.getExpectedQuestionCount("C1", it) }
        val c2PartsTotal = c2Parts.sumOf { ProQuestionRepository.getExpectedQuestionCount("C2", it) }
        assertEquals(1911, c1PartsTotal)
        assertEquals(349, c2PartsTotal)

        // 4. Verify ViewModel practice session pools for both modes
        // Unlock track for test
        com.nhyle.hyperionmaritime.data.util.LicenseManager.setGmdssProUnlocked(app, true)
        viewModel.selectTrack(Track.GMDSS_GOC)

        // Mode A: GMDSS Operational Review session -> exactly 900 questions
        viewModel.startConfiguredPracticeInternal(
            competenceId = "C1",
            isOperationalReview = true
        )
        assertEquals(900, viewModel.activeQuestions.value.size)

        // Mode B: Practice All Questions session -> full 1911 questions
        viewModel.startConfiguredPracticeInternal(
            competenceId = "C1",
            isOperationalReview = false
        )
        assertEquals(1911, viewModel.activeQuestions.value.size)

        // Mode C: Practice All Questions with Part Selection (e.g. Part 1) -> 192 questions
        viewModel.startConfiguredPracticeInternal(
            competenceId = "C1",
            selectedPart = 1,
            isOperationalReview = false
        )
        assertEquals(192, viewModel.activeQuestions.value.size)
    }
}
