package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.DrillSessionEntity
import com.nhyle.hyperionmaritime.data.local.DrillSessionFormatter
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.local.QuestionAttemptHistoryManager
import com.nhyle.hyperionmaritime.data.local.QuestionHistoryEntry
import com.nhyle.hyperionmaritime.data.local.SessionJsonHelper
import com.nhyle.hyperionmaritime.data.model.CurrentQuestionStats
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.model.UserQuestionState
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.ReviewerRepository
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class QuestionAttemptStatsTest {

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
        LicenseManager.setHyperionProUnlocked(context, true)
        LicenseManager.init(context)

        database = HyperionDatabase.getInstance(context)
        repository = ReviewerRepository(database)
        viewModel = ReviewerViewModel(application)

        QuestionAttemptHistoryManager.clearAll(context)
        QuestionAttemptHistoryManager.resetCache()
    }

    @After
    fun tearDown() {
        QuestionAttemptHistoryManager.clearAll(context)
        QuestionAttemptHistoryManager.resetCache()
    }

    @Test
    fun test1_confirmingAnswerRecordsExactlyOneAttempt() = runBlocking {
        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        val questions = viewModel.activeQuestions.value
        assertTrue(questions.isNotEmpty())
        val q0 = questions[0]

        // Select correct answer
        viewModel.selectOption(0, q0.correctAnswerIndex)
        assertEquals(q0.correctAnswerIndex, viewModel.selectedChoiceIndex.value)
        assertFalse(viewModel.isAnswerEvaluated.value)

        // Confirm
        viewModel.confirmOrNextQuestion()
        assertTrue(viewModel.isAnswerEvaluated.value)

        val history = QuestionAttemptHistoryManager.getHistory(context, q0.track.name, q0.id)
        assertNotNull("History entry should be created", history)
        assertEquals("Should have exactly 1 attempt", 1, history?.totalAttempts)
        assertEquals("Should have 1 correct", 1, history?.allTimeCorrect)
        assertEquals("Should have 0 incorrect", 0, history?.allTimeIncorrect)

        val stats = viewModel.currentQuestionStats.value
        assertNotNull("Current stats should be published", stats)
        assertEquals(q0.id, stats?.questionId)
        assertEquals(q0.track.name, stats?.track)
        assertEquals(1, stats?.allTimeCorrect)
        assertEquals(0, stats?.allTimeIncorrect)
        assertEquals(listOf(true), stats?.recentAttempts)
    }

    @Test
    fun test2_repeatedConfirmationDoesNotCreateDuplicate() = runBlocking {
        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        val q0 = viewModel.activeQuestions.value[0]

        viewModel.selectOption(0, q0.correctAnswerIndex)
        viewModel.confirmOrNextQuestion()

        val historyAfterFirst = QuestionAttemptHistoryManager.getHistory(context, q0.track.name, q0.id)
        assertEquals(1, historyAfterFirst?.totalAttempts)

        // Rapid second invocation for the exact same evaluation
        viewModel.confirmOrNextQuestion()

        val historyAfterSecond = QuestionAttemptHistoryManager.getHistory(context, q0.track.name, q0.id)
        assertEquals("Repeated confirmation must not create a duplicate", 1, historyAfterSecond?.totalAttempts)
    }

    @Test
    fun test3_newLegitimateAttemptAtSameQuestionIsRecorded() = runBlocking {
        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        val q0 = viewModel.activeQuestions.value[0]
        val wrongAnswer = if (q0.correctAnswerIndex == 0) 1 else 0

        // 1st Attempt: Wrong
        viewModel.selectOption(0, wrongAnswer)
        viewModel.confirmOrNextQuestion()

        val h1 = QuestionAttemptHistoryManager.getHistory(context, q0.track.name, q0.id)
        assertEquals(1, h1?.totalAttempts)
        assertEquals(0, h1?.allTimeCorrect)
        assertEquals(1, h1?.allTimeIncorrect)

        // Start a brand new practice session including q0
        val vm2 = ReviewerViewModel(application)
        vm2.setTrack(Track.DECK_OIC_NW)
        vm2.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        // 2nd Attempt: Correct
        vm2.selectOption(0, q0.correctAnswerIndex)
        vm2.confirmOrNextQuestion()

        val h2 = QuestionAttemptHistoryManager.getHistory(context, q0.track.name, q0.id)
        assertEquals("Legitimate subsequent attempt must be recorded", 2, h2?.totalAttempts)
        assertEquals(1, h2?.allTimeCorrect)
        assertEquals(1, h2?.allTimeIncorrect)
        assertEquals(listOf(false, true), h2?.latest24Attempts)
    }

    @Test
    fun test4_intensiveModeRecordsOnlyCompletedEvaluations() = runBlocking {
        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = true,
            deliveryMode = com.nhyle.hyperionmaritime.viewmodel.DrillDeliveryMode.INTENSIVE
        )

        val headQ = viewModel.intensiveQueue.value.first()

        // 1. Selecting an option choice does NOT record attempt
        viewModel.selectIntensiveChoice(headQ.correctAnswerIndex)
        val historyBeforeConfirm = QuestionAttemptHistoryManager.getHistory(context, headQ.track.name, headQ.id)
        assertNull("Selecting an option must NOT record an attempt in history", historyBeforeConfirm)

        // 2. Calling confirmIntensiveAnswer() evaluates and commits
        viewModel.confirmIntensiveAnswer()
        val historyAfterConfirm = QuestionAttemptHistoryManager.getHistory(context, headQ.track.name, headQ.id)
        assertNotNull("Confirming intensive answer must record attempt", historyAfterConfirm)
        assertEquals(1, historyAfterConfirm?.totalAttempts)
        assertEquals(1, historyAfterConfirm?.allTimeCorrect)

        // 3. Repeated confirmation does not duplicate
        viewModel.confirmIntensiveAnswer()
        assertEquals(1, QuestionAttemptHistoryManager.getHistory(context, headQ.track.name, headQ.id)?.totalAttempts)
    }

    @Test
    fun test5_switchingQuestionsNeverDisplaysPreviousQuestionStats() = runBlocking {
        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        val q0 = viewModel.activeQuestions.value[0]
        val q1 = viewModel.activeQuestions.value[1]

        // Answer Q0
        viewModel.selectOption(0, q0.correctAnswerIndex)
        viewModel.confirmOrNextQuestion()

        val statsQ0 = viewModel.currentQuestionStats.value
        assertNotNull(statsQ0)
        assertEquals(q0.id, statsQ0?.questionId)

        // Advance to Q1
        viewModel.nextQuestion()
        assertEquals(1, viewModel.currentQuestionIndex.value)

        // Q1 is not evaluated yet: stats must be invalidated (null or matching Q1, NOT Q0)
        val statsAfterSwitch = viewModel.currentQuestionStats.value
        assertFalse("Previous question stats must not be displayed for new question",
            statsAfterSwitch != null && statsAfterSwitch.questionId == q0.id)
    }

    @Test
    fun test6_restoringSessionDoesNotRecordExtraAttempt() = runBlocking {
        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        val q0 = viewModel.activeQuestions.value[0]
        viewModel.selectOption(0, q0.correctAnswerIndex)
        viewModel.confirmOrNextQuestion()

        val initialHistory = QuestionAttemptHistoryManager.getHistory(context, q0.track.name, q0.id)
        assertEquals(1, initialHistory?.totalAttempts)

        // Simulate session restoration
        val answersMap = mapOf(q0.id to UserQuestionState(questionId = q0.id, selectedIndex = q0.correctAnswerIndex, isSubmitted = true))
        val sessionEntity = DrillSessionEntity(
            id = "restore_test_session",
            track = Track.DECK_OIC_NW.name,
            competence = "C1",
            part = null,
            isCompleted = false,
            lastUpdated = System.currentTimeMillis(),
            correctCount = 1,
            answeredCount = 1,
            totalItems = 5,
            currentQuestionIndex = 0,
            questionIds = viewModel.activeQuestions.value.map { it.id }.joinToString(","),
            answersJson = SessionJsonHelper.serializeAnswers(answersMap),
            isShuffled = false,
            deliveryMode = "STANDARD",
            mode = "PRACTICE"
        )

        val newVm = ReviewerViewModel(application)
        newVm.resumeDrillSession(sessionEntity)

        val historyAfterRestore = QuestionAttemptHistoryManager.getHistory(context, q0.track.name, q0.id)
        assertEquals("Session restoration must NOT record an extra attempt", 1, historyAfterRestore?.totalAttempts)
    }

    @Test
    fun test7_failureSafePersistenceAndMalformedJson() {
        val file = QuestionAttemptHistoryManager.getHistoryFile(context)

        // Record a valid attempt
        val entry1 = QuestionAttemptHistoryManager.recordAttempt(context, "DECK_OIC_NW", "D-F1-C1-001", true)
        assertEquals(1, entry1.totalAttempts)
        assertTrue(file.exists())
        val validContent = file.readText()

        // Intentionally corrupt the file with invalid JSON
        file.writeText("{ corrupted_json_content [")
        QuestionAttemptHistoryManager.resetCache()

        // Attempting to load from corrupted file must NOT overwrite original file with empty content
        val loaded = QuestionAttemptHistoryManager.getHistory(context, "DECK_OIC_NW", "D-F1-C1-001")
        // It either restores from .bak or preserves file without wiping it
        assertTrue("Corrupted file must not be deleted or wiped", file.exists())
        val afterLoadContent = file.readText()
        assertFalse("Malformed JSON must not cause immediate overwrite with empty JSON", afterLoadContent.trim() == "{}")
    }

    @Test
    fun test8_formatAttemptSummarySingularAndPlural() {
        val e1 = QuestionHistoryEntry(
            questionId = "Q1",
            track = "DECK_OIC_NW",
            allTimeCorrect = 1,
            allTimeIncorrect = 1,
            attempts = listOf()
        )
        assertEquals("Correct 1 time, incorrect 1 time", e1.formattedSummary())

        val e2 = QuestionHistoryEntry(
            questionId = "Q2",
            track = "DECK_OIC_NW",
            allTimeCorrect = 5,
            allTimeIncorrect = 2,
            attempts = listOf()
        )
        assertEquals("Correct 5 times, incorrect 2 times", e2.formattedSummary())

        val e3 = QuestionHistoryEntry(
            questionId = "Q3",
            track = "DECK_OIC_NW",
            allTimeCorrect = 0,
            allTimeIncorrect = 1,
            attempts = listOf()
        )
        assertEquals("Correct 0 times, incorrect 1 time", e3.formattedSummary())

        // Test 24 attempt bar limit preserves all-time totals
        val attemptRecords = (1..30).map { i ->
            com.nhyle.hyperionmaritime.data.local.QuestionAttemptRecord(timestamp = i.toLong(), isCorrect = i % 2 == 0)
        }
        val e4 = QuestionHistoryEntry(
            questionId = "Q4",
            track = "DECK_OIC_NW",
            allTimeCorrect = 15,
            allTimeIncorrect = 15,
            attempts = attemptRecords
        )
        assertEquals(30, e4.totalAttempts)
        assertEquals(24, e4.latest24Attempts.size)
        assertEquals("Correct 15 times, incorrect 15 times", e4.formattedSummary())
    }

    @Test
    fun test9_trackSeparationForIdenticalQuestionIds() {
        // Same question ID "OVERLAP_Q1" on two different tracks
        QuestionAttemptHistoryManager.recordAttempt(context, "DECK_OIC_NW", "OVERLAP_Q1", true)
        QuestionAttemptHistoryManager.recordAttempt(context, "GMDSS_GOC", "OVERLAP_Q1", false)

        val deckHistory = QuestionAttemptHistoryManager.getHistory(context, "DECK_OIC_NW", "OVERLAP_Q1")
        val gmdssHistory = QuestionAttemptHistoryManager.getHistory(context, "GMDSS_GOC", "OVERLAP_Q1")

        assertNotNull(deckHistory)
        assertNotNull(gmdssHistory)

        assertEquals(1, deckHistory?.allTimeCorrect)
        assertEquals(0, deckHistory?.allTimeIncorrect)

        assertEquals(0, gmdssHistory?.allTimeCorrect)
        assertEquals(1, gmdssHistory?.allTimeIncorrect)
    }
}
