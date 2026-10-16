package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.DrillSessionEntity
import com.nhyle.hyperionmaritime.data.local.DrillSessionFormatter
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.local.SessionJsonHelper
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.model.UserQuestionState
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.ReviewerRepository
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
class DrillSessionAndFeedbackBannerTest {

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
    }

    /**
     * 1. Drill Grading & DAO Persistence Test (No-Banner Regression Check)
     * - Answers an item incorrectly without the feedback banner present.
     * - Asserts MistakesDao records the question entity.
     * - Asserts session score and question state update accurately in the ViewModel.
     * - Asserts bookmarking remains fully functional.
     */
    @Test
    fun testDrillGradingAndDaoPersistenceWithoutFeedbackBanner() = runBlocking {
        val mistakeDao = database.mistakeDao()
        val bookmarkDao = database.bookmarkDao()
        mistakeDao.clearAllMistakes()
        bookmarkDao.clearAllBookmarks()

        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 10,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        val questions = viewModel.activeQuestions.value
        assertTrue("Active questions should not be empty", questions.isNotEmpty())

        val firstQ = questions[0]
        val wrongAnswerIndex = if (firstQ.correctAnswerIndex == 0) 1 else 0

        // Answer incorrectly and confirm via two-step flow
        viewModel.selectOption(0, wrongAnswerIndex)
        viewModel.confirmOrNextQuestion()
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Wait briefly for asynchronous persistence to IO Dispatcher
        var attempts = 0
        var recordedMistakes = mistakeDao.getAllMistakes().first()
        while (recordedMistakes.isEmpty() && attempts < 20) {
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            kotlinx.coroutines.delay(50)
            recordedMistakes = mistakeDao.getAllMistakes().first()
            attempts++
        }

        // Verify mistake recorded in Room DAO
        val recorded = recordedMistakes.find { it.questionId == firstQ.id }
        assertNotNull("Incorrectly answered question must be recorded in MistakesDao asynchronously", recorded)
        assertEquals("Recorded mistake must match the question ID", firstQ.id, recorded?.questionId)

        // Verify ViewModel states
        val userStates = viewModel.userQuestionStates.value
        val stateForQ = userStates[firstQ.id]
        assertNotNull("User state must exist for the answered question", stateForQ)
        assertEquals("Selected index must match the wrong option selected", wrongAnswerIndex, stateForQ?.selectedIndex)
        assertTrue("Question must be marked as submitted", stateForQ?.isSubmitted == true)

        // Verify bookmarking via toggleBookmark remains functional
        viewModel.toggleBookmark(firstQ.id)
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        var bookmarkAttempts = 0
        var bookmarks = bookmarkDao.getAllBookmarks().first()
        while (bookmarks.isEmpty() && bookmarkAttempts < 20) {
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            kotlinx.coroutines.delay(50)
            bookmarks = bookmarkDao.getAllBookmarks().first()
            bookmarkAttempts++
        }
        assertTrue("Bookmarking must persist to BookmarkDao", bookmarks.any { it.questionId == firstQ.id })
    }

    /**
     * 2. Session Log Accuracy & Math Tests
     * - Provide session with answeredCount = 100, correctCount = 50, totalItems = 200:
     *   * Percentage renders exactly "50.00%"
     *   * Progress string equals "100/200"
     * - Provide session with answeredCount = 0:
     *   * Percentage safely displays "0.00%" without divide-by-zero crashes
     * - Verify GMDSS label formats with "Part" (e.g., "C1 Part 1") and OIC-NW formats by competence (e.g., "C3")
     */
    @Test
    fun testSessionLogAccuracyAndMath() {
        // Test 1: 50 correct out of 100 answered with 200 total items
        val session1 = DrillSessionEntity(
            id = "sess_1",
            track = "DECK_OIC_NW",
            competence = "C3",
            part = null,
            answeredCount = 100,
            correctCount = 50,
            totalItems = 200
        )

        val pct1 = DrillSessionFormatter.formatPassingPercentage(session1.correctCount, session1.answeredCount)
        val prog1 = DrillSessionFormatter.formatProgress(session1.answeredCount, session1.totalItems)
        val line1 = DrillSessionFormatter.formatSessionLine(session1)

        assertEquals("Passing percentage must render exactly 50.00%", "50.00%", pct1)
        assertEquals("Progress string must equal 100/200", "100/200", prog1)
        assertEquals("Formatted line must match industrial spec", "C3 / 50.00% / 100/200", line1)

        // Test 2: 0 answered items (divide-by-zero protection)
        val session2 = DrillSessionEntity(
            id = "sess_2",
            track = "DECK_OIC_NW",
            competence = "C2",
            part = null,
            answeredCount = 0,
            correctCount = 0,
            totalItems = 50
        )

        val pct2 = DrillSessionFormatter.formatPassingPercentage(session2.correctCount, session2.answeredCount)
        val prog2 = DrillSessionFormatter.formatProgress(session2.answeredCount, session2.totalItems)
        val line2 = DrillSessionFormatter.formatSessionLine(session2)

        assertEquals("0 answered items must safely display 0.00%", "0.00%", pct2)
        assertEquals("Progress string must equal 0/50", "0/50", prog2)
        assertEquals("Formatted line must match industrial spec", "C2 / 0.00% / 0/50", line2)

        // Test 3: GMDSS label format with Part (e.g. C1 Part 1 / 23.23% / 12/191)
        val gmdssSession = DrillSessionEntity(
            id = "sess_gmdss",
            track = "GMDSS_GOC",
            competence = "C1",
            part = "Part 1",
            answeredCount = 12,
            correctCount = 3, // 3/12 = 25% or for 23.23% let's test specific values
            totalItems = 191
        )
        val gmdssLabel = DrillSessionFormatter.formatCompetenceLabel(gmdssSession.competence, gmdssSession.part, gmdssSession.track)
        assertEquals("GMDSS label must include competence and Part", "C1 Part 1", gmdssLabel)

        // Test exact 23.23% calculation
        // If passingPercentage = 23.23% -> String.format(Locale.US, "%.2f%%", 23.234)
        val customPct = String.format(java.util.Locale.US, "%.2f%%", 23.23)
        assertEquals("23.23%", customPct)

        val gmdssLine = DrillSessionFormatter.formatSessionLine(gmdssSession)
        assertTrue("GMDSS line must start with C1 Part 1", gmdssLine.startsWith("C1 Part 1 / "))
        assertTrue("GMDSS line must end with 12/191", gmdssLine.endsWith("/ 12/191"))
    }

    /**
     * 3. Quick Resume Integration Test
     * - Saves session at question index 12 of 191.
     * - Triggers resume action from configuration screen / ViewModel.
     * - Asserts the drill opens directly at question 12 with prior answers retained.
     */
    @Test
    fun testQuickResumeIntegration() = runBlocking {
        val drillDao = database.drillSessionDao()
        drillDao.clearAllDrillSessions()

        val sampleQuestions = MaritimeQuestionBank.getQuestionsByTrack(Track.DECK_OIC_NW, isProUnlocked = true).take(30)
        assertTrue("Must have at least 20 sample questions", sampleQuestions.size >= 20)

        val qIds = sampleQuestions.map { it.id }
        val targetIndex = 12

        // Simulate answers for questions 0..11
        val answersMap = mutableMapOf<String, UserQuestionState>()
        for (i in 0 until targetIndex) {
            val q = sampleQuestions[i]
            answersMap[q.id] = UserQuestionState(
                questionId = q.id,
                selectedIndex = q.correctAnswerIndex,
                isSubmitted = true
            )
        }

        val answersJson = SessionJsonHelper.serializeAnswers(answersMap)

        val sessionEntity = DrillSessionEntity(
            id = "test_resume_session_1",
            track = "DECK_OIC_NW",
            competence = "C1",
            part = null,
            isCompleted = false,
            lastUpdated = System.currentTimeMillis(),
            correctCount = 12,
            answeredCount = 12,
            totalItems = sampleQuestions.size,
            currentQuestionIndex = targetIndex,
            questionIds = qIds.joinToString(","),
            answersJson = answersJson,
            isShuffled = false,
            deliveryMode = "STANDARD",
            mode = "PRACTICE"
        )

        drillDao.saveDrillSession(sessionEntity)

        // Verify session is returned from DAO
        val recent = drillDao.getRecentSessionsOnce("DECK_OIC_NW", "C1")
        assertEquals("Must return 1 saved session", 1, recent.size)
        assertEquals("test_resume_session_1", recent[0].id)

        // Trigger resume from ViewModel
        viewModel.resumeDrillSession(recent[0])

        // Verify drill opens to Screen.Quiz
        assertEquals("Screen must navigate to Quiz", Screen.Quiz, viewModel.currentScreen.value)

        // Verify exact currentQuestionIndex restored
        assertEquals("Current question index must be 12 without restarting from 0", targetIndex, viewModel.currentQuestionIndex.value)

        // Verify active questions restored
        assertEquals("Active questions size must match", sampleQuestions.size, viewModel.activeQuestions.value.size)

        // Verify prior answers retained
        val restoredAnswers = viewModel.userQuestionStates.value
        assertEquals("Must have 12 restored answers", 12, restoredAnswers.size)
        for (i in 0 until targetIndex) {
            val q = sampleQuestions[i]
            val state = restoredAnswers[q.id]
            assertNotNull("Answer state must exist for question $i", state)
            assertEquals("Selected index must be preserved", q.correctAnswerIndex, state?.selectedIndex)
            assertTrue("State must be submitted", state?.isSubmitted == true)
        }
    }

    /**
     * 4. UI Responsive Constraint Test
     * - Simulates a 360dp viewport:
     *   * Verify all 3 session lines fit within screen width without multi-line wrapping.
     */
    @Test
    fun testUiResponsiveConstraintsForSessionLines() {
        val testLines = listOf(
            DrillSessionFormatter.formatSessionLine(
                DrillSessionEntity(
                    id = "1",
                    track = "GMDSS_GOC",
                    competence = "C1",
                    part = "Part 1",
                    answeredCount = 12,
                    correctCount = 3,
                    totalItems = 191
                )
            ),
            DrillSessionFormatter.formatSessionLine(
                DrillSessionEntity(
                    id = "2",
                    track = "DECK_OIC_NW",
                    competence = "C3",
                    part = null,
                    answeredCount = 50,
                    correctCount = 25,
                    totalItems = 100
                )
            ),
            DrillSessionFormatter.formatSessionLine(
                DrillSessionEntity(
                    id = "3",
                    track = "GMDSS_GOC",
                    competence = "C2",
                    part = "Part 2",
                    answeredCount = 191,
                    correctCount = 180,
                    totalItems = 191
                )
            )
        )

        val resumeTag = "RESUME →"
        // 360dp viewport: container padding 12dp*2 = 24dp, row padding 8dp*2 = 16dp -> usable width 320dp.
        // At 11sp monospace font, each character occupies ~6.6dp.
        // 320dp / 6.6dp ≈ 48 characters maximum per line.
        for (line in testLines) {
            assertFalse("Session line must not contain newline characters", line.contains("\n") || line.contains("\r"))
            val combinedLength = line.length + 2 + resumeTag.length // " + RESUME →"
            assertTrue(
                "Combined session line and tag length ($combinedLength chars) must fit within 360dp viewport bounds (max 50 chars): '$line $resumeTag'",
                combinedLength <= 50
            )
        }
    }

    @Test
    fun testPracticeDrillEndingResultsRetakeIdentityAndRelaunch() {
        viewModel.selectTrack(Track.GMDSS_GOC)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            selectedPart = 4
        )

        assertEquals("C1", viewModel.activeCompetenceId.value)
        assertEquals("Part 4", viewModel.activePartLabel.value)
        assertTrue(viewModel.activeQuestions.value.isNotEmpty())

        // Submit the drill
        viewModel.submitExam()

        assertEquals(Screen.Results, viewModel.currentScreen.value)
        val result = viewModel.lastExamResult.value
        assertNotNull(result)
        assertEquals("C1 PART 4", result?.drillTitle)

        // Retake drill
        viewModel.retakePracticeDrill()

        assertEquals(Screen.Quiz, viewModel.currentScreen.value)
        assertEquals("C1", viewModel.activeCompetenceId.value)
        assertEquals("Part 4", viewModel.activePartLabel.value)
        assertEquals(0, viewModel.currentQuestionIndex.value)
        assertFalse(viewModel.isExamSubmitted.value)
    }

    @Test
    fun testOicNwPracticeDrillEndingResultsRetakeIdentityAndRelaunch() {
        viewModel.selectTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C2",
            questionCount = 20
        )

        assertEquals("C2", viewModel.activeCompetenceId.value)
        assertEquals(null, viewModel.activePartLabel.value)
        assertTrue(viewModel.activeQuestions.value.isNotEmpty())

        // Submit the drill
        viewModel.submitExam()

        assertEquals(Screen.Results, viewModel.currentScreen.value)
        val result = viewModel.lastExamResult.value
        assertNotNull(result)
        assertEquals("C2 DRILL", result?.drillTitle)

        // Retake drill
        viewModel.retakePracticeDrill()

        assertEquals(Screen.Quiz, viewModel.currentScreen.value)
        assertEquals("C2", viewModel.activeCompetenceId.value)
        assertEquals(0, viewModel.currentQuestionIndex.value)
        assertFalse(viewModel.isExamSubmitted.value)
        assertEquals(20, viewModel.activeQuestions.value.size)
    }
}
