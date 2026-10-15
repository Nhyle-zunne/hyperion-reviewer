package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.ActiveSessionSnapshotManager
import com.nhyle.hyperionmaritime.data.local.DrillSessionFormatter
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.model.QuizMode
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.ReviewerRepository
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
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
@Config(sdk = [34])
class MistakesSessionIsolationTest {

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

    @Test
    fun testLabelFormatterMistakes() {
        // Label formatter shows "Mistakes" with no part number
        val label = DrillSessionFormatter.formatScopeLabel("MISTAKES", null, Track.DECK_OIC_NW.name)
        assertEquals("Mistakes", label)

        val compLabel = DrillSessionFormatter.formatCompetenceLabel("MISTAKES", null, Track.DECK_OIC_NW.name)
        assertEquals("Mistakes", compLabel)

        val compLabelWithPart = DrillSessionFormatter.formatCompetenceLabel("MISTAKES", "Part 1", Track.DECK_OIC_NW.name)
        assertEquals("Mistakes", compLabelWithPart)
    }

    @Test
    fun testRegularSessionAndMistakesSessionCoexistAndResume() = runBlocking {
        viewModel.setTrack(Track.DECK_OIC_NW)

        // 1. Start a regular competence session
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            selectedPart = 1,
            questionCount = 10
        )
        // Answer question 0
        viewModel.selectOption(0, 0)
        viewModel.confirmOrNextQuestion()
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        viewModel.exitQuizToHome()

        // Verify regular session snapshot exists
        val regSnap = ActiveSessionSnapshotManager.loadSnapshot(application, Track.DECK_OIC_NW.name)
        assertNotNull(regSnap)
        assertEquals("C1", regSnap?.competenceOrPart)

        // 2. Start Vault practice (Mistakes session)
        val sampleQuestions = MaritimeQuestionBank.getQuestionsByTrack(Track.DECK_OIC_NW, isProUnlocked = true).take(5)
        viewModel.startMistakesVaultPractice(sampleQuestions)
        assertEquals(QuizMode.MISTAKES, viewModel.activeQuizMode.value)
        assertEquals("MISTAKES", viewModel.activeCompetenceId.value)

        // Answer question 0
        viewModel.selectOption(0, 0)
        viewModel.confirmOrNextQuestion()
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        viewModel.exitQuiz()

        // 3. Verify BOTH sessions exist in snapshots
        val regSnapAfter = ActiveSessionSnapshotManager.loadSnapshot(application, Track.DECK_OIC_NW.name)
        assertNotNull("Regular session must survive starting Vault practice", regSnapAfter)
        assertEquals("C1", regSnapAfter?.competenceOrPart)

        val mistakesSnap = ActiveSessionSnapshotManager.loadMistakesSnapshot(application, Track.DECK_OIC_NW.name)
        assertNotNull("Mistakes session must exist in its separate slot", mistakesSnap)
        assertEquals("MISTAKES", mistakesSnap?.competenceOrPart)
        assertEquals("MISTAKES", mistakesSnap?.mode)

        // 4. Verify in drill_sessions DAO (waiting for Dispatchers.IO persistence)
        var attempts = 0
        var recentSessions = repository.getRecentDrillSessions(Track.DECK_OIC_NW.name, null).first()
        while (recentSessions.none { it.competence == "MISTAKES" || it.mode == "MISTAKES" } && attempts < 20) {
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            kotlinx.coroutines.delay(50)
            recentSessions = repository.getRecentDrillSessions(Track.DECK_OIC_NW.name, null).first()
            attempts++
        }
        val mistakesDbSession = recentSessions.find { it.competence == "MISTAKES" || it.mode == "MISTAKES" }
        val regDbSession = recentSessions.find { it.competence == "C1" }
        assertNotNull("Mistakes session must be in drill_sessions", mistakesDbSession)
        assertNotNull("Regular session must be in drill_sessions", regDbSession)
        assertEquals("Mistakes", mistakesDbSession?.scopeLabel)

        // 5. Resume Mistakes session and verify
        viewModel.resumeDrillSession(mistakesDbSession!!)
        assertEquals(QuizMode.MISTAKES, viewModel.activeQuizMode.value)
        assertEquals("MISTAKES", viewModel.activeCompetenceId.value)
        assertEquals(sampleQuestions.size, viewModel.activeQuestions.value.size)

        // 6. Complete the Mistakes session
        viewModel.submitExam()
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Verify Mistakes session is marked completed or cleared from active slot
        val mistakesSnapAfterSubmit = ActiveSessionSnapshotManager.loadMistakesSnapshot(application, Track.DECK_OIC_NW.name)
        assertTrue(mistakesSnapAfterSubmit == null || mistakesSnapAfterSubmit.isCompleted)

        // Verify regular session is UNCHANGED
        val regSnapStillThere = ActiveSessionSnapshotManager.loadSnapshot(application, Track.DECK_OIC_NW.name)
        assertNotNull("Regular session must remain untouched after completing Mistakes session", regSnapStillThere)
        assertEquals("C1", regSnapStillThere?.competenceOrPart)
        assertFalse(regSnapStillThere!!.isCompleted)

        // 7. Resume regular session and verify
        viewModel.resumeSession(Track.DECK_OIC_NW.name)
        assertEquals(QuizMode.PRACTICE, viewModel.activeQuizMode.value)
        assertEquals("C1", viewModel.activeCompetenceId.value)
    }

    @Test
    fun testRestartAppBothSessionsPersist() = runBlocking {
        viewModel.setTrack(Track.DECK_OIC_NW)

        // Start regular session
        viewModel.startConfiguredPractice(competenceId = "C1", selectedPart = 1, questionCount = 10)
        viewModel.selectOption(0, 0)
        viewModel.confirmOrNextQuestion()
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        viewModel.exitQuizToHome()

        // Start Mistakes session
        val sampleQuestions = MaritimeQuestionBank.getQuestionsByTrack(Track.DECK_OIC_NW, isProUnlocked = true).take(5)
        viewModel.startMistakesVaultPractice(sampleQuestions)
        viewModel.selectOption(0, 0)
        viewModel.confirmOrNextQuestion()
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        viewModel.exitQuizToHome()

        // Simulate app restart by creating a brand-new ViewModel
        val newViewModel = ReviewerViewModel(application)
        newViewModel.setTrack(Track.DECK_OIC_NW)

        // Check recent drill sessions
        var attempts = 0
        var recent = repository.getRecentDrillSessions(Track.DECK_OIC_NW.name, null).first()
        while ((!recent.any { it.competence == "MISTAKES" } || !recent.any { it.competence == "C1" }) && attempts < 20) {
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            kotlinx.coroutines.delay(50)
            recent = repository.getRecentDrillSessions(Track.DECK_OIC_NW.name, null).first()
            attempts++
        }
        val hasMistakes = recent.any { it.competence == "MISTAKES" && !it.isCompleted }
        val hasRegular = recent.any { it.competence == "C1" && !it.isCompleted }
        assertTrue("Mistakes session persists after restart", hasMistakes)
        assertTrue("Regular session persists after restart", hasRegular)
    }
}
