package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Difficulty
import com.example.data.model.Question
import com.example.data.model.QuizMode
import com.example.data.model.Track
import com.example.data.model.UserQuestionState
import com.example.data.repository.MarinaScoringEngine
import com.example.data.repository.MaritimeCompetenceRegistry
import com.example.data.repository.MaritimeQuestionBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Hyperion", appName)
  }

  @Test
  fun `verify question bank contains Deck and GMDSS questions`() {
    val deckQuestions = MaritimeQuestionBank.getQuestionsByTrack(Track.DECK_OIC_NW)
    val gmdssQuestions = MaritimeQuestionBank.getQuestionsByTrack(Track.GMDSS_GOC)
    assertTrue(deckQuestions.isNotEmpty())
    assertTrue(gmdssQuestions.isNotEmpty())
  }

  @Test
  fun `verify MARINA scoring engine enforces 70 percent passing rule per competence`() {
    val q1 = Question(
      id = "test-1",
      track = Track.DECK_OIC_NW,
      functionId = "F1",
      competenceId = "C1",
      topic = "Topic 1",
      questionText = "Question 1",
      options = listOf("A", "B", "C", "D"),
      correctAnswerIndex = 0,
      explanation = "Exp 1",
      difficulty = Difficulty.EASY // 3 pts
    )
    val q2 = Question(
      id = "test-2",
      track = Track.DECK_OIC_NW,
      functionId = "F1",
      competenceId = "C2",
      topic = "Topic 2",
      questionText = "Question 2",
      options = listOf("A", "B", "C", "D"),
      correctAnswerIndex = 1,
      explanation = "Exp 2",
      difficulty = Difficulty.MODERATE // 6 pts
    )

    // User gets C1 right (100%), but C2 wrong (0%)
    val answers = mapOf(
      "test-1" to UserQuestionState(questionId = "test-1", selectedIndex = 0),
      "test-2" to UserQuestionState(questionId = "test-2", selectedIndex = 2) // Wrong
    )

    val result = MarinaScoringEngine.evaluateExam(
      track = Track.DECK_OIC_NW,
      mode = QuizMode.MOCK_EXAM,
      questions = listOf(q1, q2),
      answers = answers
    )

    // Even though raw score is 3/9 = 33.3%, C2 is < 70%, so overall should FAIL
    assertFalse(result.isOverallPassed)
  }

  @Test
  fun `verify installation-bound token is stable across calls and unique upon fresh install`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences(com.example.data.util.LicenseManager.PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    // 1. First launch after install creates token and request code
    val token1 = com.example.data.util.LicenseManager.getOrGenerateInstallationToken(context)
    val reqCode1 = com.example.data.util.LicenseManager.getOrGenerateRequestCode(context)
    val formatRegex = Regex("^HYP-[0-9A-F]{4}-[0-9A-F]{4}$")

    assertTrue("Request code format must be HYP-XXXX-XXXX", formatRegex.matches(reqCode1))
    assertTrue("Token must be non-empty", token1.isNotEmpty())

    // 2. Subsequent call returns exact same token and request code
    val token2 = com.example.data.util.LicenseManager.getOrGenerateInstallationToken(context)
    val reqCode2 = com.example.data.util.LicenseManager.getOrGenerateRequestCode(context)
    assertEquals("Token must remain stable across calls", token1, token2)
    assertEquals("Request code must remain stable across calls", reqCode1, reqCode2)

    // 3. Key generation math works identically
    val key1 = com.example.data.util.LicenseManager.computeValidKey(reqCode1)
    val keyRegex = Regex("^ACTV-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}$")
    assertTrue("Key must follow ACTV-XXXX-XXXX-XXXX format", keyRegex.matches(key1))
    assertTrue("Validation must pass for computeValidKey", com.example.data.util.LicenseManager.verifyKey(reqCode1, key1))

    // 4. Simulate uninstall or app data wipe
    prefs.edit().clear().commit()
    Thread.sleep(10) // Ensure timestamp advances

    val token3 = com.example.data.util.LicenseManager.getOrGenerateInstallationToken(context)
    val reqCode3 = com.example.data.util.LicenseManager.getOrGenerateRequestCode(context)

    // New token has new timestamp, so it is different from token1
    assertTrue("Token after clear data must differ from original token", token1 != token3)
    assertTrue("Request code format must be HYP-XXXX-XXXX", formatRegex.matches(reqCode3))
    val key3 = com.example.data.util.LicenseManager.computeValidKey(reqCode3)
    assertTrue(com.example.data.util.LicenseManager.verifyKey(reqCode3, key3))
  }

  @Test
  fun `verify GMDSS Pro Hard Licensing blocks question access when unactivated`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.util.LicenseManager.setActivated(context, false)
    com.example.data.repository.ProQuestionRepository.clearCache()

    assertFalse("License must be unactivated", com.example.data.util.LicenseManager.isActivated(context))

    // Attempting to load any part of C1 or C2 must return an empty list
    for (part in 1..10) {
      val c1Questions = com.example.data.repository.ProQuestionRepository.loadPartQuestions(context, "C1", part)
      assertTrue("Unactivated user must NOT receive C1 Part $part questions", c1Questions.isEmpty())
    }

    for (part in 1..2) {
      val c2Questions = com.example.data.repository.ProQuestionRepository.loadPartQuestions(context, "C2", part)
      assertTrue("Unactivated user must NOT receive C2 Part $part questions", c2Questions.isEmpty())
    }

    val allC1 = com.example.data.repository.ProQuestionRepository.loadAllCompetenceQuestions(context, "C1")
    val allC2 = com.example.data.repository.ProQuestionRepository.loadAllCompetenceQuestions(context, "C2")
    assertTrue("All C1 questions must be blocked", allC1.isEmpty())
    assertTrue("All C2 questions must be blocked", allC2.isEmpty())
  }

  @Test
  fun `verify GMDSS Pro Data Ingestion and Model schema for activated user`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.util.LicenseManager.setActivated(context, true)
    com.example.data.repository.ProQuestionRepository.clearCache()

    assertTrue("License must be activated", com.example.data.util.LicenseManager.isActivated(context))

    var totalQuestions = 0

    // Test C1 (10 parts)
    val expectedC1Counts = mapOf(
      1 to 192, 2 to 191, 3 to 191, 4 to 191, 5 to 191,
      6 to 191, 7 to 191, 8 to 191, 9 to 191, 10 to 191
    )
    for (part in 1..10) {
      val questions = com.example.data.repository.ProQuestionRepository.loadPartQuestions(context, "C1", part)
      val expected = expectedC1Counts[part]!!
      assertEquals("C1 Part $part item count must match expected", expected, questions.size)
      totalQuestions += questions.size

      for (q in questions) {
        assertEquals("C1", q.competence)
        assertEquals(part, q.part)
        assertTrue("Question text must be non-empty", q.questionText.isNotBlank())
        assertTrue("Options must have at least 2 choices", q.options.size >= 2)
        assertTrue("Correct answer index must be valid", q.correctAnswerIndex in 0 until q.options.size)
      }
    }

    // Test C2 (2 parts)
    val expectedC2Counts = mapOf(1 to 175, 2 to 174)
    for (part in 1..2) {
      val questions = com.example.data.repository.ProQuestionRepository.loadPartQuestions(context, "C2", part)
      val expected = expectedC2Counts[part]!!
      assertEquals("C2 Part $part item count must match expected", expected, questions.size)
      totalQuestions += questions.size

      for (q in questions) {
        assertEquals("C2", q.competence)
        assertEquals(part, q.part)
        assertTrue("Question text must be non-empty", q.questionText.isNotBlank())
        assertTrue("Options must have at least 2 choices", q.options.size >= 2)
        assertTrue("Correct answer index must be valid", q.correctAnswerIndex in 0 until q.options.size)
      }
    }

    // Total must be 2,260 items
    assertEquals("Total GMDSS Pro review items must be 2,260", 2260, totalQuestions)
  }
}
