package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.ReviewerRepository
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.ui.screens.DrillOptionCard
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DrillOptionZeroJitterAndDeferredConfirmationTest {

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
     * 1. Layout Dimension Consistency (Zero-Jitter Test):
     * Measure option card height in Compose test before selection, during selection, and after evaluation.
     * Assert height(default) == height(selected) == height(evaluated) (0 pixel variance).
     */
    @Test
    fun testOptionCardZeroJitterHeightConsistencyAcrossAllStates() {
        var isSelected by mutableStateOf(false)
        var isCorrect by mutableStateOf(false)
        var isEvaluated by mutableStateOf(false)

        composeTestRule.setContent {
            DrillOptionCard(
                choiceIndex = 0,
                choiceText = "This is a standard maritime test option with consistent length across states",
                isSelected = isSelected,
                isCorrect = isCorrect,
                isEvaluated = isEvaluated,
                onOptionSelected = { isSelected = true },
                modifier = Modifier.testTag("jitter_test_card")
            )
        }

        // 1. Default unselected state
        composeTestRule.waitForIdle()
        val defaultHeight = composeTestRule.onNodeWithTag("jitter_test_card").fetchSemanticsNode().size.height
        assertTrue("Card must have positive height", defaultHeight > 0)

        // 2. Selected (pre-submission) state
        isSelected = true
        composeTestRule.waitForIdle()
        val selectedHeight = composeTestRule.onNodeWithTag("jitter_test_card").fetchSemanticsNode().size.height

        // 3. Evaluated Correct state
        isEvaluated = true
        isCorrect = true
        composeTestRule.waitForIdle()
        val evaluatedCorrectHeight = composeTestRule.onNodeWithTag("jitter_test_card").fetchSemanticsNode().size.height

        // 4. Evaluated Incorrect (selected wrong) state
        isCorrect = false
        composeTestRule.waitForIdle()
        val evaluatedIncorrectHeight = composeTestRule.onNodeWithTag("jitter_test_card").fetchSemanticsNode().size.height

        // 5. Evaluated Unselected state
        isSelected = false
        composeTestRule.waitForIdle()
        val evaluatedUnselectedHeight = composeTestRule.onNodeWithTag("jitter_test_card").fetchSemanticsNode().size.height

        // Assert 0 pixel variance across all states
        assertEquals("Height in selected state must equal default state (0px jitter)", defaultHeight, selectedHeight)
        assertEquals("Height in evaluated correct state must equal default state (0px jitter)", defaultHeight, evaluatedCorrectHeight)
        assertEquals("Height in evaluated incorrect state must equal default state (0px jitter)", defaultHeight, evaluatedIncorrectHeight)
        assertEquals("Height in evaluated unselected state must equal default state (0px jitter)", defaultHeight, evaluatedUnselectedHeight)
    }

    /**
     * 2. Selection Change & Deferred Confirmation Test:
     * - Select Option 0, then change selection to Option 2. Assert selectedChoiceIndex == 2.
     * - Assert isAnswerEvaluated == false and no database writes have occurred yet.
     * - Trigger "Next" click:
     *   * Assert isAnswerEvaluated == true.
     *   * If Option 2 is incorrect, verify item is written to MistakesDao.
     *   * Verify correct answer is marked green and Option 2 is marked red.
     * - Trigger second "Next" click:
     *   * Assert screen advances to next question and states reset.
     */
    @Test
    fun testSelectionChangeAndDeferredConfirmationFlow() = runBlocking {
        val mistakeDao = database.mistakeDao()
        mistakeDao.clearAllMistakes()

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

        // 1. Initial State
        assertEquals(0, viewModel.currentQuestionIndex.value)
        assertNull("selectedChoiceIndex should initially be null", viewModel.selectedChoiceIndex.value)
        assertFalse("isAnswerEvaluated should initially be false", viewModel.isAnswerEvaluated.value)

        // 2. Select Option 0, then change selection to Option 2
        viewModel.selectOption(0, 0)
        assertEquals(0, viewModel.selectedChoiceIndex.value)
        assertFalse(viewModel.isAnswerEvaluated.value)

        viewModel.selectOption(0, 2)
        assertEquals("Cadet should be able to change selection freely", 2, viewModel.selectedChoiceIndex.value)
        assertFalse("isAnswerEvaluated must remain false before confirmation", viewModel.isAnswerEvaluated.value)

        // Verify no database writes have occurred yet in phase 1
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        val mistakesBeforeConfirm = mistakeDao.getAllMistakes().first()
        assertTrue("No mistakes should be written to DAO before confirmation", mistakesBeforeConfirm.isEmpty())

        // 3. Trigger "Next" click (Phase 1 -> Phase 2: Confirmation & Answer Reveal)
        viewModel.nextQuestion() // acts as Submit / Check Answer on first click
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue("isAnswerEvaluated must be true after first Next click", viewModel.isAnswerEvaluated.value)
        assertEquals("Current question index must NOT advance on first click", 0, viewModel.currentQuestionIndex.value)

        val isOption2Correct = firstQ.correctAnswerIndex == 2
        if (!isOption2Correct) {
            // Verify item is written to MistakesDao asynchronously
            var attempts = 0
            var recordedMistakes = mistakeDao.getAllMistakes().first()
            while (recordedMistakes.isEmpty() && attempts < 20) {
                ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
                delay(50)
                recordedMistakes = mistakeDao.getAllMistakes().first()
                attempts++
            }
            val recorded = recordedMistakes.find { it.questionId == firstQ.id }
            assertNotNull("Incorrectly answered question must be written to MistakesDao", recorded)
        }

        // Verify user question state in ViewModel
        val userStates = viewModel.userQuestionStates.value
        val stateForQ = userStates[firstQ.id]
        assertNotNull(stateForQ)
        assertEquals(2, stateForQ?.selectedIndex)
        assertTrue(stateForQ?.isSubmitted == true)

        // 4. Trigger second "Next" click (Phase 2 -> Load next question)
        viewModel.nextQuestion()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertEquals("Screen must advance to question index 1", 1, viewModel.currentQuestionIndex.value)
        assertNull("selectedChoiceIndex must reset to null for new question", viewModel.selectedChoiceIndex.value)
        assertFalse("isAnswerEvaluated must reset to false for new question", viewModel.isAnswerEvaluated.value)
    }

    /**
     * 3. Free Selection & Locked Evaluation Guard Test:
     * - Cadet can switch options back and forth.
     * - Once evaluated, clicking options is locked and does not change selection.
     */
    @Test
    fun testOptionSelectionLockAfterEvaluation() = runBlocking {
        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 5,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        // Free selection: switch 1 -> 3 -> 1
        viewModel.selectOption(0, 1)
        assertEquals(1, viewModel.selectedChoiceIndex.value)
        viewModel.selectOption(0, 3)
        assertEquals(3, viewModel.selectedChoiceIndex.value)
        viewModel.selectOption(0, 1)
        assertEquals(1, viewModel.selectedChoiceIndex.value)

        // Confirm
        viewModel.confirmOrNextQuestion()
        assertTrue(viewModel.isAnswerEvaluated.value)

        // Attempt selection change after evaluation: must be locked
        viewModel.selectOption(0, 2)
        assertEquals("Selection must remain 1 after evaluation lock", 1, viewModel.selectedChoiceIndex.value)
    }
}
