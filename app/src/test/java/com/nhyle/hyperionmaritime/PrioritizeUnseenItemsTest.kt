package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.ActiveSessionEntity
import com.nhyle.hyperionmaritime.data.local.DrillSessionEntity
import com.nhyle.hyperionmaritime.data.local.ExamRecordEntity
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.QuizMode
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.QuestionSelectionDispatcher
import com.nhyle.hyperionmaritime.data.repository.ReviewerRepository
import com.nhyle.hyperionmaritime.viewmodel.DrillConfiguration
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class PrioritizeUnseenItemsTest {

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var database: HyperionDatabase
    private lateinit var repository: ReviewerRepository
    private lateinit var viewModel: ReviewerViewModel

    private fun createDummyQuestion(
        id: String,
        track: Track = Track.DECK_OIC_NW,
        competenceId: String = "C1"
    ): Question {
        return Question(
            id = id,
            track = track,
            functionId = if (track == Track.GMDSS_GOC) "GMDSS" else "F1",
            competenceId = competenceId,
            topic = "Topic $competenceId",
            questionText = "Question Text $id",
            options = listOf("Alpha", "Bravo", "Charlie", "Delta"),
            correctAnswerIndex = 0,
            explanation = "Explanation for $id",
            difficulty = Difficulty.EASY
        )
    }

    private fun generatePool(count: Int, prefix: String = "Q", track: Track = Track.DECK_OIC_NW, competenceId: String = "C1"): List<Question> {
        return (1..count).map { i ->
            createDummyQuestion(id = "$prefix-$i", track = track, competenceId = competenceId)
        }
    }

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        database = HyperionDatabase.getInstance(context)
        database.examRecordDao().clearHistory()
        database.drillSessionDao().clearAllDrillSessions()
        repository = ReviewerRepository(database)
        viewModel = ReviewerViewModel(application)
        viewModel.clearAllSeenQuestions()
    }

    // ------------------------------------------------------------------------
    // SECTION 9.A: QUESTION-SELECTION UNIT TESTS
    // ------------------------------------------------------------------------

    @Test
    fun test1_noHistory_returnsRequestedCountOfUniqueQuestions() {
        val pool = generatePool(100)
        val selected = QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = pool,
            requestedCount = 20,
            seenQuestionIds = emptySet(),
            prioritizeUnseen = true
        )

        assertEquals("Must return 20 questions", 20, selected.size)
        assertEquals("All returned questions must be unique", 20, selected.map { it.id }.toSet().size)
    }

    @Test
    fun test2_sufficientUnseenQuestions_returnsAllUnseen() {
        val pool = generatePool(100)
        val seenIds = (1..70).map { "Q-$it" }.toSet() // 70 seen, 30 unseen

        val selected = QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = pool,
            requestedCount = 20,
            seenQuestionIds = seenIds,
            prioritizeUnseen = true
        )

        assertEquals("Must return exactly 20 questions", 20, selected.size)
        assertEquals("Must have 20 unique questions", 20, selected.map { it.id }.toSet().size)
        assertTrue("Every selected question must be unseen", selected.all { it.id !in seenIds })
    }

    @Test
    fun test3_insufficientUnseenQuestions_includesAllUnseenAndBackfillsFromSeen() {
        val pool = generatePool(100)
        // 92 seen, 8 unseen (Q-93 .. Q-100)
        val seenIds = (1..92).map { "Q-$it" }.toSet()
        val expectedUnseenIds = (93..100).map { "Q-$it" }.toSet()

        val selected = QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = pool,
            requestedCount = 20,
            seenQuestionIds = seenIds,
            prioritizeUnseen = true
        )

        assertEquals("Must return exactly 20 questions", 20, selected.size)
        assertEquals("Must have 20 unique questions", 20, selected.map { it.id }.toSet().size)

        val selectedIds = selected.map { it.id }.toSet()
        assertTrue("All 8 unseen questions must be included", selectedIds.containsAll(expectedUnseenIds))

        val seenCount = selected.count { it.id in seenIds }
        val unseenCount = selected.count { it.id !in seenIds }
        assertEquals("Must have all 8 available unseen questions", 8, unseenCount)
        assertEquals("Must backfill 12 questions from seen pool", 12, seenCount)
    }

    @Test
    fun test4_allQuestionsSeen_returnsRequestedCountFromSeenPool() {
        val pool = generatePool(100)
        val seenIds = (1..100).map { "Q-$it" }.toSet()

        val selected = QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = pool,
            requestedCount = 20,
            seenQuestionIds = seenIds,
            prioritizeUnseen = true
        )

        assertEquals("Must return 20 questions", 20, selected.size)
        assertEquals("Must have 20 unique questions", 20, selected.map { it.id }.toSet().size)
        assertTrue("All returned questions come from seen pool safely", selected.all { it.id in seenIds })
    }

    @Test
    fun test5_entirePoolRequested_returnsEntirePool() {
        val pool = generatePool(30)
        val seenIds = setOf("Q-1", "Q-2", "Q-3")

        val selected = QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = pool,
            requestedCount = 30,
            seenQuestionIds = seenIds,
            prioritizeUnseen = true
        )

        assertEquals("Must return all 30 questions", 30, selected.size)
        assertEquals("Must have all 30 unique questions", 30, selected.map { it.id }.toSet().size)
    }

    @Test
    fun test6_requestExceedsPoolSize_safelyClampsToPoolSize() {
        val pool = generatePool(10)
        val selected = QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = pool,
            requestedCount = 20,
            seenQuestionIds = emptySet(),
            prioritizeUnseen = true
        )

        assertEquals("Must safely clamp to 10 questions", 10, selected.size)
        assertEquals("Must contain all 10 pool questions", 10, selected.map { it.id }.toSet().size)
    }

    @Test
    fun test7_emptyEligiblePool_returnsEmptyList() {
        val selected = QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = emptyList(),
            requestedCount = 20,
            seenQuestionIds = setOf("Q-1"),
            prioritizeUnseen = true
        )

        assertTrue("Empty pool must return empty list without error", selected.isEmpty())
    }

    @Test
    fun test8_duplicateHistoryRecords_deduplicatesSafely() {
        val pool = generatePool(20)
        val duplicateHistory = listOf("Q-1", "Q-1", "Q-2", "Q-2", "Q-2").toSet()

        val selected = QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = pool,
            requestedCount = 10,
            seenQuestionIds = duplicateHistory,
            prioritizeUnseen = true
        )

        assertEquals(10, selected.size)
        // Since Q-1 and Q-2 are seen, unseen pool has 18 questions.
        // We requested 10, so all 10 must be unseen.
        assertTrue("Q-1 must not be in selected unseen", selected.none { it.id == "Q-1" })
        assertTrue("Q-2 must not be in selected unseen", selected.none { it.id == "Q-2" })
    }

    @Test
    fun test9_trackIsolation_unrelatedTrackHistoryDoesNotAffectCurrentTrack() {
        val deckPool = (1..20).map { createDummyQuestion("ITEM-$it", Track.DECK_OIC_NW, "C1") }
        // Suppose GMDSS history has recorded ITEM-1..ITEM-10 with identical string format
        // In repository query, track filter isolates history to Track.DECK_OIC_NW
        runBlocking {
            database.examRecordDao().insertRecord(
                ExamRecordEntity(
                    id = "gmdss_rec_1",
                    timestamp = System.currentTimeMillis(),
                    track = Track.GMDSS_GOC.name,
                    mode = "PRACTICE",
                    rawScore = 10,
                    maxScore = 10,
                    percentage = 100.0,
                    isPassed = true,
                    totalQuestions = 10,
                    correctCount = 10,
                    detailsSummary = "COMPLETED • Score 10/10",
                    questionIds = (1..10).joinToString(",") { "ITEM-$it" }
                )
            )

            // When querying encountered questions for DECK_OIC_NW:
            val deckSeenIds = repository.getEncounteredQuestionIds(Track.DECK_OIC_NW)
            assertTrue("Deck seen IDs must be empty because records belong to GMDSS", deckSeenIds.isEmpty())

            // And when prioritizing unseen on deck, all 20 are treated as unseen
            val selected = QuestionSelectionDispatcher.selectQuestions(
                eligiblePool = deckPool,
                requestedCount = 10,
                seenQuestionIds = deckSeenIds,
                prioritizeUnseen = true
            )
            assertEquals(10, selected.size)
            assertEquals(10, selected.map { it.id }.toSet().size)
        }
    }

    @Test
    fun test10_competenceIsolation_unrelatedCompetenceRecordsDoNotExcludeSelectedCompetence() {
        val c1Pool = (1..20).map { createDummyQuestion("C1-$it", Track.DECK_OIC_NW, "C1") }
        // History contains records for C2 questions
        val c2SeenIds = (1..20).map { "C2-$it" }.toSet()

        val selected = QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = c1Pool,
            requestedCount = 10,
            seenQuestionIds = c2SeenIds,
            prioritizeUnseen = true
        )

        assertEquals("Must return 10 questions", 10, selected.size)
        assertTrue("All selected questions belong to C1 pool", selected.all { it.id.startsWith("C1-") })
    }

    @Test
    fun test11_featureDisabled_preservesStandardSelectionBehavior() {
        val pool = generatePool(50)
        val seenIds = (1..40).map { "Q-$it" }.toSet()

        // When prioritizeUnseen is false, standard sequential order (if shuffle = false) is preserved
        val selected = QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = pool,
            requestedCount = 10,
            seenQuestionIds = seenIds,
            prioritizeUnseen = false,
            shuffleQuestions = false
        )

        assertEquals(10, selected.size)
        assertEquals("Q-1", selected[0].id)
        assertEquals("Q-2", selected[1].id)
        assertEquals("Q-10", selected[9].id)
    }

    @Test
    fun test12_randomization_noDuplicatesAndUnseenItemsPrioritized() {
        val pool = generatePool(50)
        val seenIds = (1..30).map { "Q-$it" }.toSet() // 20 unseen: Q-31 .. Q-50

        val selected = QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = pool,
            requestedCount = 15,
            seenQuestionIds = seenIds,
            prioritizeUnseen = true,
            shuffleQuestions = false
        )

        assertEquals(15, selected.size)
        assertEquals("Must have 15 unique questions", 15, selected.map { it.id }.toSet().size)
        assertTrue("All 15 selected questions must be unseen", selected.all { it.id !in seenIds })
    }

    // ------------------------------------------------------------------------
    // SECTION 9.B: DATABASE AND INTEGRATION TESTS
    // ------------------------------------------------------------------------

    @Test
    fun testDatabase_completedSessionBecomesVisibleToNextUnseenQuery() = runBlocking {
        val track = Track.DECK_OIC_NW

        // Initially no history
        val initialSeen = repository.getEncounteredQuestionIds(track)
        assertTrue("Initial history must be empty", initialSeen.isEmpty())

        // Save a completed exam record
        val record = ExamRecordEntity(
            id = "test_exam_completed_1",
            timestamp = System.currentTimeMillis(),
            track = track.name,
            mode = "PRACTICE",
            rawScore = 5,
            maxScore = 5,
            percentage = 100.0,
            isPassed = true,
            totalQuestions = 5,
            correctCount = 5,
            detailsSummary = "COMPLETED • Score: 5/5 (100.0%)",
            questionIds = "D-F1-C1-001,D-F1-C1-002,D-F1-C1-003"
        )
        database.examRecordDao().insertRecord(record)

        val updatedSeen = repository.getEncounteredQuestionIds(track)
        assertEquals(3, updatedSeen.size)
        assertTrue(updatedSeen.contains("D-F1-C1-001"))
        assertTrue(updatedSeen.contains("D-F1-C1-002"))
        assertTrue(updatedSeen.contains("D-F1-C1-003"))
    }

    @Test
    fun testDrillConfiguration_modelDefaultsAndStateFlow() {
        val defaultModel = DrillConfiguration()
        assertFalse("prioritizeUnseen must default to false", defaultModel.prioritizeUnseen)

        viewModel.setPrioritizeUnseen(true)
        assertTrue("Setting prioritizeUnseen must update StateFlow", viewModel.drillConfiguration.value.prioritizeUnseen)

        viewModel.setPrioritizeUnseen(false)
        assertFalse("Toggling off must update StateFlow", viewModel.drillConfiguration.value.prioritizeUnseen)
    }

    @Test
    fun testSessionGeneration_startConfiguredPracticeWithUnseenPriority() {
        // Unlock Deck track for testing
        com.nhyle.hyperionmaritime.data.util.LicenseManager.setOicNwProUnlocked(context, true)
        com.nhyle.hyperionmaritime.data.util.LicenseManager.init(context)

        val seenSubset = setOf("D-F1-C1-001", "D-F1-C1-002", "D-F1-C1-003")

        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 10,
            prioritizeUnseen = true,
            seenQuestionIds = seenSubset
        )

        assertEquals("Screen must navigate to Quiz", Screen.Quiz, viewModel.currentScreen.value)
        val activeQuestions = viewModel.activeQuestions.value
        assertTrue("Active questions must not be empty", activeQuestions.isNotEmpty())
        assertEquals("Active questions size must match requested count", 10, activeQuestions.size)

        // The first questions should not be in seenSubset if sufficient unseen questions exist in C1
        val firstQuestion = activeQuestions.first()
        assertFalse("Unseen items must be served first", seenSubset.contains(firstQuestion.id))
    }

    @Test
    fun test13_shuffleModePreservesUnseenPriorityBeforeSeen() {
        val pool = generatePool(50)
        // 40 seen, 10 unseen: Q-41..Q-50
        val seenIds = (1..40).map { "Q-$it" }.toSet()
        val expectedUnseenIds = (41..50).map { "Q-$it" }.toSet()

        val selected = QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = pool,
            requestedCount = 20,
            seenQuestionIds = seenIds,
            prioritizeUnseen = true,
            shuffleQuestions = true
        )

        assertEquals("Must return 20 questions", 20, selected.size)
        // The first 10 questions MUST ALL be unseen!
        val first10 = selected.take(10).map { it.id }.toSet()
        val last10 = selected.drop(10).map { it.id }.toSet()

        assertEquals("First 10 items must be all 10 unseen questions", expectedUnseenIds, first10)
        assertTrue("Last 10 items must come from seen pool", last10.all { it in seenIds })
    }

    @Test
    fun test14_markQuestionSeen_trackIsolationAndPersistence() {
        viewModel.markQuestionSeen(Track.DECK_OIC_NW, "D-001")
        viewModel.markQuestionSeen(Track.GMDSS_GOC, "G-001")

        val deckSeen = viewModel.getSeenQuestionIds(Track.DECK_OIC_NW)
        val gmdssSeen = viewModel.getSeenQuestionIds(Track.GMDSS_GOC)

        assertTrue("Deck seen must contain D-001", deckSeen.contains("D-001"))
        assertFalse("Deck seen must NOT contain G-001", deckSeen.contains("G-001"))

        assertTrue("GMDSS seen must contain G-001", gmdssSeen.contains("G-001"))
        assertFalse("GMDSS seen must NOT contain D-001", gmdssSeen.contains("D-001"))
    }

    @Test
    fun test15_confirmOrNextQuestion_marksQuestionSeenImmediately() = runBlocking {
        com.nhyle.hyperionmaritime.data.util.LicenseManager.setOicNwProUnlocked(context, true)
        com.nhyle.hyperionmaritime.data.util.LicenseManager.init(context)

        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            prioritizeUnseen = false
        )

        val firstQ = viewModel.activeQuestions.value.first()
        viewModel.selectOption(0, firstQ.correctAnswerIndex)
        viewModel.confirmOrNextQuestion()

        val seenIds = viewModel.getSeenQuestionIds(Track.DECK_OIC_NW)
        assertTrue("Answered question must be immediately recorded in seen IDs", seenIds.contains(firstQ.id))
    }

    @Test
    fun test16_clearHistory_clearsPersistentSeenData() = runBlocking {
        viewModel.markQuestionSeen(Track.DECK_OIC_NW, "D-TEST-1")
        viewModel.markQuestionSeen(Track.GMDSS_GOC, "G-TEST-1")

        viewModel.clearHistory()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val deckSeen = viewModel.getSeenQuestionIds(Track.DECK_OIC_NW)
        val gmdssSeen = viewModel.getSeenQuestionIds(Track.GMDSS_GOC)

        assertTrue("Deck seen must be cleared", deckSeen.isEmpty())
        assertTrue("GMDSS seen must be cleared", gmdssSeen.isEmpty())
    }

    @Test
    fun test17_rangeScoping_doesNotSelectOutsideRange() {
        com.nhyle.hyperionmaritime.data.util.LicenseManager.setOicNwProUnlocked(context, true)
        com.nhyle.hyperionmaritime.data.util.LicenseManager.init(context)

        // Select items from range 10 to 20
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            startItem = 10,
            endItem = 20,
            questionCount = 11,
            prioritizeUnseen = true,
            seenQuestionIds = emptySet()
        )

        val questions = viewModel.activeQuestions.value
        assertEquals(11, questions.size)

        val allC1Questions = com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank.getQuestionsByCompetence("C1", Track.DECK_OIC_NW, true)
        val expectedRange = allC1Questions.subList(9, 20).map { it.id }.toSet()

        assertTrue("All selected questions must come strictly from the range 10..20", questions.all { it.id in expectedRange })
    }

    @Test
    fun test18_interruptedSession_questionAnsweredNotPrioritizedInNextSession() = runBlocking {
        com.nhyle.hyperionmaritime.data.util.LicenseManager.setOicNwProUnlocked(context, true)
        com.nhyle.hyperionmaritime.data.util.LicenseManager.init(context)
        viewModel.clearHistory()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // Session 1: Start 10 items
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 10,
            prioritizeUnseen = true,
            seenQuestionIds = null
        )

        val session1Questions = viewModel.activeQuestions.value
        val answeredQ1 = session1Questions[0]
        val answeredQ2 = session1Questions[1]

        // Candidate answers first two questions
        viewModel.selectOption(0, answeredQ1.correctAnswerIndex)
        viewModel.confirmOrNextQuestion() // Evaluate Q1 (marks seen)
        viewModel.confirmOrNextQuestion() // Advance to Q2

        viewModel.selectOption(1, answeredQ2.correctAnswerIndex)
        viewModel.confirmOrNextQuestion() // Evaluate Q2 (marks seen)

        // Session is INTERRUPTED: candidate leaves/exits without finishing or submitting
        viewModel.exitQuiz()

        // Session 2: Start new configured practice session with unseen prioritization enabled
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 10,
            prioritizeUnseen = true,
            seenQuestionIds = null
        )

        val session2Questions = viewModel.activeQuestions.value
        assertEquals(10, session2Questions.size)

        // Acceptance condition: answeredQ1 and answeredQ2 must NOT be prioritized as unseen!
        // The first questions of Session 2 must be new unseen questions (starting with question 3)
        val firstTwoInSession2 = session2Questions.take(2).map { it.id }
        assertFalse("Answered Q1 must not appear at the beginning of Session 2", firstTwoInSession2.contains(answeredQ1.id))
        assertFalse("Answered Q2 must not appear at the beginning of Session 2", firstTwoInSession2.contains(answeredQ2.id))
        assertEquals("Session 2 should start with Q3 from Session 1", session1Questions[2].id, session2Questions[0].id)
    }

    @Test
    fun test19_incompleteExamRecord_doesNotTreatUnansweredAsSeen() = runBlocking {
        val track = Track.DECK_OIC_NW
        viewModel.clearHistory()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // Insert an incomplete exam record with 5 question IDs in questionIds, but only 1 answer in answersJson
        val answersMap = mapOf(
            "D-F1-C1-001" to com.nhyle.hyperionmaritime.data.model.UserQuestionState(
                questionId = "D-F1-C1-001",
                selectedIndex = 0,
                isSubmitted = true
            )
        )
        val record = com.nhyle.hyperionmaritime.data.local.ExamRecordEntity(
            id = "inc_test_123",
            timestamp = System.currentTimeMillis(),
            track = track.name,
            mode = "MOCK_EXAM",
            rawScore = 1,
            maxScore = 5,
            percentage = 20.0,
            isPassed = false,
            totalQuestions = 5,
            correctCount = 1,
            detailsSummary = "INCOMPLETE • Answered 1/5",
            questionIds = "D-F1-C1-001,D-F1-C1-002,D-F1-C1-003,D-F1-C1-004,D-F1-C1-005",
            answersJson = com.nhyle.hyperionmaritime.data.local.SessionJsonHelper.serializeAnswers(answersMap)
        )
        database.examRecordDao().insertRecord(record)

        val seen = repository.getEncounteredQuestionIds(track)
        assertEquals("Only answered question should be seen", 1, seen.size)
        assertTrue("D-F1-C1-001 should be seen", seen.contains("D-F1-C1-001"))
        assertFalse("Unanswered D-F1-C1-002 must NOT be marked seen", seen.contains("D-F1-C1-002"))
    }

    @Test
    fun test20_disabledModePreservesOriginalOrder() = runBlocking {
        com.nhyle.hyperionmaritime.data.util.LicenseManager.setOicNwProUnlocked(context, true)
        com.nhyle.hyperionmaritime.data.util.LicenseManager.init(context)

        // Mark some items seen
        viewModel.markQuestionSeen(Track.DECK_OIC_NW, "D-F1-C1-001")

        // Start with prioritizeUnseen = false
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            prioritizeUnseen = false,
            shuffleQuestions = false
        )

        val questions = viewModel.activeQuestions.value
        assertEquals("D-F1-C1-001", questions[0].id)
    }
}
