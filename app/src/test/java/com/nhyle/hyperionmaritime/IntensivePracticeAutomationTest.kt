package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.viewmodel.DrillDeliveryMode
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.practiceAutoAdvanceDelayMillis
import org.junit.After
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
class IntensivePracticeAutomationTest {
    private lateinit var application: Application
    private lateinit var viewModel: ReviewerViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        MaritimeQuestionBank.init(application)
        LicenseManager.setOicNwProUnlocked(application, true)
        LicenseManager.init(application)
        viewModel = ReviewerViewModel(application)
        viewModel.toggleDirectAnswer(false)
        viewModel.toggleAutoNextCorrect(false)
        viewModel.toggleAutoNextWrong(false)
        viewModel.setAutoNextDelayMillis(true, 300)
        viewModel.setAutoNextDelayMillis(false, 500)
        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            deliveryMode = DrillDeliveryMode.INTENSIVE,
            shuffleQuestions = false,
            shuffleChoices = false
        )
        assertTrue("Test requires an intensive queue", viewModel.intensiveQueue.value.size > 1)
    }

    @After
    fun tearDown() {
        viewModel.toggleDirectAnswer(false)
        viewModel.toggleAutoNextCorrect(false)
        viewModel.toggleAutoNextWrong(false)
        viewModel.setAutoNextDelayMillis(true, 300)
        viewModel.setAutoNextDelayMillis(false, 500)
        LicenseManager.setOicNwProUnlocked(application, false)
        LicenseManager.init(application)
    }

    @Test
    fun directAnswerEvaluatesOnTapWithoutCheckAndOnlyOnce() {
        val first = viewModel.intensiveQueue.value.first()
        viewModel.toggleDirectAnswer(true)

        viewModel.selectIntensiveChoice(first.correctAnswerIndex)

        assertTrue("Direct Answer must evaluate the tapped option", viewModel.isAnswerEvaluated.value)
        val committed = viewModel.userQuestionStates.value[first.id]
        assertEquals(first.correctAnswerIndex, committed?.selectedIndex)
        assertTrue(committed?.isSubmitted == true)
        assertTrue(committed?.isCorrect == true)

        val otherOption = if (first.correctAnswerIndex == 0) 1 else 0
        viewModel.selectIntensiveChoice(otherOption)
        viewModel.confirmIntensiveAnswer()
        assertEquals("Evaluation must remain locked after confirmation",
            first.correctAnswerIndex, viewModel.userQuestionStates.value[first.id]?.selectedIndex)

        viewModel.advanceIntensiveQuestion()
        assertEquals("Correct answers leave the intensive queue", 1, viewModel.intensiveClearedCount.value)
        assertFalse("The next question must begin unevaluated", viewModel.isAnswerEvaluated.value)
    }

    @Test
    fun withoutDirectAnswerOptionSelectionWaitsForCheckAndWrongAnswerRecycles() {
        val initial = viewModel.intensiveQueue.value
        val first = initial.first()
        val wrong = if (first.correctAnswerIndex == 0) 1 else 0

        viewModel.selectIntensiveChoice(wrong)
        assertFalse("Selecting alone must not submit when Direct Answer is off",
            viewModel.isAnswerEvaluated.value)
        assertFalse(viewModel.userQuestionStates.value[first.id]?.isSubmitted ?: true)

        viewModel.advanceIntensiveQuestion()
        assertEquals("Unconfirmed answers must never move the queue", first.id,
            viewModel.intensiveQueue.value.first().id)

        viewModel.confirmIntensiveAnswer()
        assertTrue(viewModel.isAnswerEvaluated.value)
        assertFalse(viewModel.userQuestionStates.value[first.id]?.isCorrect ?: true)
        viewModel.advanceIntensiveQuestion()

        assertEquals("Wrong answers stay in the queue", initial.size, viewModel.intensiveQueue.value.size)
        assertEquals(first.id, viewModel.intensiveQueue.value.last().id)
        assertEquals(0, viewModel.intensiveClearedCount.value)
    }

    @Test
    fun autoNextDelaysStartAtPreviousDefaultsAndCanBeChangedIndependently() {
        assertEquals(300, viewModel.autoNextCorrectDelayMs.value)
        assertEquals(500, viewModel.autoNextWrongDelayMs.value)
        assertEquals(300L, practiceAutoAdvanceDelayMillis(isCorrect = true))
        assertEquals(500L, practiceAutoAdvanceDelayMillis(isCorrect = false))

        viewModel.adjustAutoNextDelayMillis(isCorrect = true, increment = 1)
        viewModel.adjustAutoNextDelayMillis(isCorrect = false, increment = -1)

        assertEquals(400, viewModel.autoNextCorrectDelayMs.value)
        assertEquals(400, viewModel.autoNextWrongDelayMs.value)
        assertEquals(400L, practiceAutoAdvanceDelayMillis(
            isCorrect = true,
            correctDelayMs = viewModel.autoNextCorrectDelayMs.value,
            wrongDelayMs = viewModel.autoNextWrongDelayMs.value
        ))

        viewModel.setAutoNextDelayMillis(isCorrect = true, delayMs = 300)
        viewModel.setAutoNextDelayMillis(isCorrect = false, delayMs = 500)
        assertEquals(300, viewModel.autoNextCorrectDelayMs.value)
        assertEquals(500, viewModel.autoNextWrongDelayMs.value)
    }

    @Test
    fun autoNextDelayClampsAndPersistsLocallyAcrossViewModels() {
        viewModel.setAutoNextDelayMillis(isCorrect = true, delayMs = 10)
        viewModel.setAutoNextDelayMillis(isCorrect = false, delayMs = 9000)
        viewModel.adjustAutoNextDelayMillis(isCorrect = true, increment = -1)
        viewModel.adjustAutoNextDelayMillis(isCorrect = false, increment = 1)

        assertEquals(100, viewModel.autoNextCorrectDelayMs.value)
        assertEquals(5000, viewModel.autoNextWrongDelayMs.value)
        val prefs = application.getSharedPreferences("hyperion_prefs", Context.MODE_PRIVATE)
        assertEquals(100, prefs.getInt("drill_auto_next_correct_delay_ms", -1))
        assertEquals(5000, prefs.getInt("drill_auto_next_wrong_delay_ms", -1))

        val reloaded = ReviewerViewModel(application)
        assertEquals(100, reloaded.autoNextCorrectDelayMs.value)
        assertEquals(5000, reloaded.autoNextWrongDelayMs.value)
        assertEquals(100L, practiceAutoAdvanceDelayMillis(true, 0, 5000))
        assertEquals(5000L, practiceAutoAdvanceDelayMillis(false, 100, 10000))
    }
}
