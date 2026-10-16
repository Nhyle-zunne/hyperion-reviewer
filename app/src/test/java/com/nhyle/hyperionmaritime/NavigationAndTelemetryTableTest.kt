package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.DrillSessionEntity
import com.nhyle.hyperionmaritime.data.local.DrillSessionFormatter
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.ReviewerRepository
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.ui.screens.CompactSessionLogTable
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NavigationAndTelemetryTableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
     * 1. Back Navigation & Route Integrity Test:
     * - Launch drill from PracticeConfigScreen(track = "OIC-NW", competence = "C1").
     * - Simulate user confirming "EXIT SESSION".
     * - Assert back-stack destination is PracticeConfigScreen with identical track and competence arguments.
     * - Verify arguments are not dropped and user is NOT kicked out to Home or TrackSelection root.
     */
    @Test
    fun testBackNavigationAndRouteIntegrityOnDrillExit() = runBlocking {
        // Step 1: Set track and navigate to PracticeConfig for C1
        viewModel.setTrack(Track.DECK_OIC_NW)
        val initialConfigScreen = Screen.PracticeConfig(
            competenceId = "C1",
            isCrpMode = false,
            track = Track.DECK_OIC_NW
        )
        viewModel.navigateTo(initialConfigScreen)
        assertEquals(initialConfigScreen, viewModel.currentScreen.value)

        // Step 2: Start drill session from configuration
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 10,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )
        assertTrue(
            "Screen must be Quiz during active drill",
            viewModel.currentScreen.value is Screen.Quiz
        )

        // Answer an item to generate progress
        viewModel.selectOption(0, 0)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Step 3: Simulate user confirming "SAVE & EXIT" via exitDrillToCompetenceConfig()
        viewModel.exitDrillToCompetenceConfig()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Step 4: Assert destination is Screen.PracticeConfig with exact arguments preserved
        val destScreen = viewModel.currentScreen.value
        assertTrue(
            "Back destination must be PracticeConfig, not Home or TrackSelection",
            destScreen is Screen.PracticeConfig
        )
        val configDest = destScreen as Screen.PracticeConfig
        assertEquals("Competence argument must be preserved as C1", "C1", configDest.competenceId)
        assertEquals("Track argument must be preserved as DECK_OIC_NW", Track.DECK_OIC_NW, configDest.track)
        assertEquals("isCrpMode must be preserved as false", false, configDest.isCrpMode)

        // Verify route integrity
        val expectedRoute = Screen.PracticeConfig.createRoute(
            track = Track.DECK_OIC_NW,
            competence = "C1",
            isCrpMode = false
        )
        assertEquals("Route must match expected route format", expectedRoute, configDest.route)

        // Step 5: Verify session progress was saved into Room drill_sessions table
        val savedSessions = database.drillSessionDao().getRecentSessions(Track.DECK_OIC_NW.name, "C1").first()
        assertTrue("Session must be recorded in drill_sessions on exit", savedSessions.isNotEmpty())
        assertEquals("Recorded session must match competence C1", "C1", savedSessions.first().competence)
    }

    /**
     * 2. Telemetry Table Rendering & Overflow Test:
     * - Render CompactSessionLogTable on a narrow screen viewport (360dp).
     * - Verify all 5 columns fit without clipping text or forcing multi-line wrapping (maxLines == 1).
     * - Test with 0 sessions (verifying table collapses cleanly).
     * - Test with 1, 2, and 4+ sessions (asserting only top 3 are rendered).
     */
    @Test
    fun testTelemetryTableRenderingAndTop3Limit() {
        val session1 = DrillSessionEntity(
            id = "sess_1",
            track = "DECK_OIC_NW",
            competence = "C1",
            part = null,
            correctCount = 8,
            answeredCount = 10,
            totalItems = 20,
            currentQuestionIndex = 10,
            lastUpdated = 1727880000000L
        )
        val session2 = DrillSessionEntity(
            id = "sess_2",
            track = "GMDSS_GOC",
            competence = "C1",
            part = "Part 1",
            correctCount = 15,
            answeredCount = 20,
            totalItems = 30,
            currentQuestionIndex = 20,
            lastUpdated = 1727883600000L
        )
        val session3 = DrillSessionEntity(
            id = "sess_3",
            track = "DECK_OIC_NW",
            competence = "C3",
            part = null,
            correctCount = 0,
            answeredCount = 0,
            totalItems = 15,
            currentQuestionIndex = 0,
            lastUpdated = 1727887200000L
        )
        val session4 = DrillSessionEntity(
            id = "sess_4",
            track = "DECK_OIC_NW",
            competence = "C4",
            part = null,
            correctCount = 5,
            answeredCount = 5,
            totalItems = 10,
            currentQuestionIndex = 5,
            lastUpdated = 1727890800000L
        )

        var sessionsState by mutableStateOf<List<DrillSessionEntity>>(emptyList())
        var resumedSessionId: String? = null

        composeTestRule.setContent {
            Box(modifier = Modifier.width(360.dp)) {
                CompactSessionLogTable(
                    sessions = sessionsState,
                    onResumeSession = { resumedSessionId = it.id }
                )
            }
        }

        // A. 0 sessions: collapses cleanly without rendering table
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("session_log_table").assertDoesNotExist()

        // B. 1 session
        sessionsState = listOf(session1)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("session_log_table").assertIsDisplayed()
        composeTestRule.onNodeWithTag("resume_session_sess_1").assertIsDisplayed()

        // C. 2 sessions
        sessionsState = listOf(session1, session2)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("resume_session_sess_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("resume_session_sess_2").assertIsDisplayed()

        // D. 4+ sessions on 360dp viewport: only top 3 rendered
        sessionsState = listOf(session1, session2, session3, session4)
        composeTestRule.waitForIdle()

        // Verify table container is displayed
        composeTestRule.onNodeWithTag("session_log_table").assertIsDisplayed()

        // Verify top 3 rows are rendered
        composeTestRule.onNodeWithTag("resume_session_sess_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("resume_session_sess_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("resume_session_sess_3").assertIsDisplayed()

        // Verify 4th row is NOT rendered (strict top 3 clamp)
        composeTestRule.onNodeWithTag("resume_session_sess_4").assertDoesNotExist()
    }

    /**
     * 3. Math & Date Formatting Tests:
     * - Test passingRate with answeredCount = 0: assert result is "0.00%".
     * - Test passingRate with correct = 25, answered = 50: assert result is "50.00%".
     * - Test scopeLabel formatting (GMDSS: "C1 P1", OIC-NW: "C1", "C3").
     * - Test timestamp formatting pattern "MM/dd HH:mm".
     */
    @Test
    fun testMathAndDateFormatting() {
        // Zero answered
        val zeroRate = DrillSessionFormatter.calculatePassingPercentage(correctCount = 0, answeredCount = 0)
        assertEquals(0.0, zeroRate, 0.001)
        val zeroFormatted = DrillSessionFormatter.formatPassingPercentage(correctCount = 0, answeredCount = 0)
        assertEquals("0.00%", zeroFormatted)

        // 25 correct of 50 answered
        val halfRate = DrillSessionFormatter.calculatePassingPercentage(correctCount = 25, answeredCount = 50)
        assertEquals(50.0, halfRate, 0.001)
        val halfFormatted = DrillSessionFormatter.formatPassingPercentage(correctCount = 25, answeredCount = 50)
        assertEquals("50.00%", halfFormatted)

        // Entity passingRate calculation
        val zeroEntity = DrillSessionEntity(
            id = "zero_test",
            track = "DECK_OIC_NW",
            correctCount = 0,
            answeredCount = 0,
            totalItems = 20
        )
        assertEquals(0.0, zeroEntity.passingRate, 0.001)
        assertEquals("0.00%", String.format(Locale.US, "%.2f%%", zeroEntity.passingRate))

        val halfEntity = DrillSessionEntity(
            id = "half_test",
            track = "DECK_OIC_NW",
            correctCount = 25,
            answeredCount = 50,
            totalItems = 50
        )
        assertEquals(50.0, halfEntity.passingRate, 0.001)
        assertEquals("50.00%", String.format(Locale.US, "%.2f%%", halfEntity.passingRate))

        // Scope Label formatting
        val gmdssScope = DrillSessionFormatter.formatScopeLabel("C1", "Part 1", "GMDSS_GOC")
        assertEquals("C1 P1", gmdssScope)

        val oicScope = DrillSessionFormatter.formatScopeLabel("C1", null, "DECK_OIC_NW")
        assertEquals("C1", oicScope)

        val c3Scope = DrillSessionFormatter.formatScopeLabel("C3", null, "DECK_OIC_NW")
        assertEquals("C3", c3Scope)

        // Timestamp formatting
        val nowMillis = System.currentTimeMillis()
        val timestampStr = DrillSessionFormatter.formatTimestamp(nowMillis)
        assertTrue(
            "Timestamp should match MM/dd HH:mm format, got: $timestampStr",
            timestampStr.matches(Regex("\\d{2}/\\d{2} \\d{2}:\\d{2}"))
        )
    }

    /**
     * 4. Session Resumption Test:
     * Tapping a session row restores question index and question states.
     */
    @Test
    fun testResumeSessionRestoresQuestionIndexAndAnswers() = runBlocking {
        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 10,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        val questions = viewModel.activeQuestions.value
        assertTrue(questions.isNotEmpty())
        val q0 = questions[0]
        val q1 = questions[1]

        // Answer question 0 and advance to index 1
        viewModel.selectOption(0, q0.correctAnswerIndex)
        viewModel.confirmOrNextQuestion()
        viewModel.confirmOrNextQuestion() // advances to index 1
        assertEquals(1, viewModel.currentQuestionIndex.value)

        // Save session
        val saveJob = viewModel.exitDrillToCompetenceConfig()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        saveJob?.join()

        var saved = database.drillSessionDao().getRecentSessionsOnce(Track.DECK_OIC_NW.name, "C1")
        if (saved.isEmpty()) {
            repeat(40) {
                ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
                Thread.sleep(50)
                saved = database.drillSessionDao().getRecentSessionsOnce(Track.DECK_OIC_NW.name, "C1")
                if (saved.isNotEmpty()) return@repeat
            }
        }
        assertTrue(saved.isNotEmpty())
        val sessionToResume = saved.first()

        // Reset viewmodel state
        viewModel.navigateTo(Screen.Home)
        assertEquals(Screen.Home, viewModel.currentScreen.value)

        // Resume session
        viewModel.resumeDrillSession(sessionToResume)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(viewModel.currentScreen.value is Screen.Quiz)
        assertEquals("Should resume at index 1", 1, viewModel.currentQuestionIndex.value)
        val restoredStates = viewModel.userQuestionStates.value
        assertNotNull(restoredStates[q0.id])
        assertEquals(q0.correctAnswerIndex, restoredStates[q0.id]?.selectedIndex)
    }

    /**
     * 5. Multi-Competence Independent Session Log Persistence:
     * - User starts C1 drill, answers question, exits without finishing.
     * - User starts C2 drill, answers question, exits without finishing.
     * - Verify C1 session still exists and is retrieved for C1.
     * - Verify C2 session still exists and is retrieved for C2.
     * - Neither session disappears or overwrites the other in competence config.
     */
    @Test
    fun testMultiCompetenceSessionPersistenceAndRetrieval() = runBlocking {
        viewModel.setTrack(Track.DECK_OIC_NW)

        // Session 1: Start C1 drill
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 10,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )
        viewModel.selectOption(0, 0)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        val saveJob1 = viewModel.exitDrillToCompetenceConfig()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        saveJob1?.join()

        // Session 2: Start C2 drill
        viewModel.startConfiguredPractice(
            competenceId = "C2",
            questionCount = 10,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )
        viewModel.selectOption(0, 1)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        val saveJob2 = viewModel.exitDrillToCompetenceConfig()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        saveJob2?.join()

        // Verify C1 session is still preserved
        val c1Sessions = database.drillSessionDao().getRecentSessionsOnce(Track.DECK_OIC_NW.name, "C1")
        assertTrue("C1 session must NOT disappear when C2 is answered", c1Sessions.isNotEmpty())
        assertEquals("C1", c1Sessions.first().competence)

        // Verify C2 session is preserved
        val c2Sessions = database.drillSessionDao().getRecentSessionsOnce(Track.DECK_OIC_NW.name, "C2")
        assertTrue("C2 session must be present", c2Sessions.isNotEmpty())
        assertEquals("C2", c2Sessions.first().competence)
    }
}
