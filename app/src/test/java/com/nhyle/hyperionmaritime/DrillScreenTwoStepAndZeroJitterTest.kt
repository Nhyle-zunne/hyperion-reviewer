package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DrillScreenTwoStepAndZeroJitterTest {

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
     * 1. Layout Dimension Consistency (Zero-Jitter Test)
     * Measure option card height in Compose test before selection, during selection,
     * and after evaluation.
     * Assert height(default) == height(selected) == height(evaluated) (0 pixel variance).
     */
    @Test
    fun testLayoutDimensionConsistencyZeroJitter() {
        var isSelected by mutableStateOf(false)
        var isEvaluated by mutableStateOf(false)
        var isCorrect by mutableStateOf(false)

        composeTestRule.setContent {
            Box(modifier = Modifier.width(360.dp)) {
                DrillOptionCard(
                    choiceIndex = 0,
                    choiceText = "Standard Maritime VHF Channel 16 distress communication",
                    isSelected = isSelected,
                    isCorrect = isCorrect,
                    isEvaluated = isEvaluated,
                    onOptionSelected = {},
                    modifier = Modifier.testTag("test_option_card")
                )
            }
        }

        // 1. Measure default height
        composeTestRule.waitForIdle()
        val defaultHeight = composeTestRule.onNodeWithTag("test_option_card").fetchSemanticsNode().size.height

        // 2. Select option (Phase 1: Free Selection)
        isSelected = true
        composeTestRule.waitForIdle()
        val selectedHeight = composeTestRule.onNodeWithTag("test_option_card").fetchSemanticsNode().size.height

        // 3. Evaluate option as incorrect (Phase 2: Evaluated Incorrect)
        isEvaluated = true
        isCorrect = false
        composeTestRule.waitForIdle()
        val evaluatedWrongHeight = composeTestRule.onNodeWithTag("test_option_card").fetchSemanticsNode().size.height

        // 4. Evaluate option as correct (Phase 2: Evaluated Correct)
        isCorrect = true
        composeTestRule.waitForIdle()
        val evaluatedCorrectHeight = composeTestRule.onNodeWithTag("test_option_card").fetchSemanticsNode().size.height

        assertEquals(
            "Selected height must equal default height (0 px variance)",
            defaultHeight,
            selectedHeight
        )
        assertEquals(
            "Evaluated wrong height must equal default height (0 px variance)",
            defaultHeight,
            evaluatedWrongHeight
        )
        assertEquals(
            "Evaluated correct height must equal default height (0 px variance)",
            defaultHeight,
            evaluatedCorrectHeight
        )
    }

    /**
     * 2. Selection Change & Deferred Confirmation Test
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
    fun testSelectionChangeAndDeferredConfirmation() = runBlocking {
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
        val correctIdx = firstQ.correctAnswerIndex

        // Choose option indices: option 0 and option 2 (or non-correct indices)
        val firstChoice = if (correctIdx == 0) 1 else 0
        val secondChoice = if (correctIdx == 2) 1 else 2

        // Phase 1: Free Selection & Editing
        // Select first choice
        viewModel.selectOption(firstChoice)
        assertEquals("selectedChoiceIndex must reflect first choice", firstChoice, viewModel.selectedChoiceIndex.value)
        assertFalse("isAnswerEvaluated must remain false during selection", viewModel.isAnswerEvaluated.value)

        // Change selection to second choice
        viewModel.selectOption(secondChoice)
        assertEquals("selectedChoiceIndex must change to second choice", secondChoice, viewModel.selectedChoiceIndex.value)
        assertFalse("isAnswerEvaluated must remain false when selection changes", viewModel.isAnswerEvaluated.value)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        val initialMistakes = mistakeDao.getAllMistakes().first()
        assertTrue("No database writes should occur before answer evaluation", initialMistakes.isEmpty())

        // Phase 2: Confirmation & Answer Reveal
        // Trigger "Next" click (first click = confirm / evaluate)
        viewModel.confirmOrNextQuestion()
        assertTrue("isAnswerEvaluated must become true after confirmation", viewModel.isAnswerEvaluated.value)

        // Wait for asynchronous persistence to IO Dispatcher
        var attempts = 0
        var recordedMistakes = mistakeDao.getAllMistakes().first()
        while (recordedMistakes.isEmpty() && attempts < 20) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            delay(50)
            recordedMistakes = mistakeDao.getAllMistakes().first()
            attempts++
        }

        // Verify mistake written to MistakesDao
        val mistakeItem = recordedMistakes.find { it.questionId == firstQ.id }
        assertNotNull("Incorrectly selected option must be saved to MistakesDao", mistakeItem)
        assertEquals(firstQ.id, mistakeItem?.questionId)

        // Verify state is submitted and selected index matches second choice
        val userStates = viewModel.userQuestionStates.value
        val stateForQ = userStates[firstQ.id]
        assertNotNull("User state must exist for the evaluated question", stateForQ)
        assertEquals(secondChoice, stateForQ?.selectedIndex)
        assertTrue("Question must be marked as submitted after evaluation", stateForQ?.isSubmitted == true)

        // Trigger second "Next" click
        val initialQuestionIndex = viewModel.currentQuestionIndex.value
        viewModel.confirmOrNextQuestion()

        // Assert screen advances to next question and states reset
        assertEquals(
            "Screen must advance to next question index",
            initialQuestionIndex + 1,
            viewModel.currentQuestionIndex.value
        )
        assertEquals(
            "selectedChoiceIndex must reset to null for new unvisited question",
            null,
            viewModel.selectedChoiceIndex.value
        )
        assertFalse(
            "isAnswerEvaluated must reset to false for new unvisited question",
            viewModel.isAnswerEvaluated.value
        )
    }
}
