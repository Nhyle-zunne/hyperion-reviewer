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
    fun autoNextUsesBriefFeedbackInBothDrillModes() {
        assertEquals(300L, practiceAutoAdvanceDelayMillis(isCorrect = true))
        assertEquals(500L, practiceAutoAdvanceDelayMillis(isCorrect = false))
    }
}
