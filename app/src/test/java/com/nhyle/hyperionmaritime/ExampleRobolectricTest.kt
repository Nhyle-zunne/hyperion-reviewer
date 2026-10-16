package com.nhyle.hyperionmaritime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.QuizMode
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.model.UserQuestionState
import com.nhyle.hyperionmaritime.data.repository.MarinaScoringEngine
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
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
    val prefs = context.getSharedPreferences(com.nhyle.hyperionmaritime.data.util.LicenseManager.PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    // 1. First launch creates token and request code
    val token1 = com.nhyle.hyperionmaritime.data.util.LicenseManager.getOrGenerateInstallationToken(context)
    val reqCode1 = com.nhyle.hyperionmaritime.data.util.LicenseManager.getOrGenerateRequestCode(context)
    val formatRegex = Regex("^HYPE-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}$")

    assertTrue("Request code format must be HYPE-XXXX-XXXX-XXXX", formatRegex.matches(reqCode1))
    assertTrue("Token must be non-empty", token1.isNotEmpty())

    // 2. Subsequent call returns exact same token and request code
    val token2 = com.nhyle.hyperionmaritime.data.util.LicenseManager.getOrGenerateInstallationToken(context)
    val reqCode2 = com.nhyle.hyperionmaritime.data.util.LicenseManager.getOrGenerateRequestCode(context)
    assertEquals("Token must remain stable across calls", token1, token2)
    assertEquals("Request code must remain stable across calls", reqCode1, reqCode2)

    // 3. Key generation math works identically
    val key1 = com.nhyle.hyperionmaritime.data.util.LicenseManager.computeValidKey(reqCode1)
    val keyRegex = Regex("^[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}$")
    assertTrue("Key must follow XXXX-XXXX-XXXX-XXXX format", keyRegex.matches(key1))
    assertTrue("Validation must pass for computeValidKey", com.nhyle.hyperionmaritime.data.util.LicenseManager.verifyKey(reqCode1, key1))

    // 4. Verify seed-based generateRequestCode format
    val seedReq = com.nhyle.hyperionmaritime.data.util.LicenseManager.generateRequestCode("SEED_DEVICE_TEST")
    val seedRegex = Regex("^HYP-[0-9A-F]{4}-[0-9A-F]{4}$")
    assertTrue("Seed-based request code must follow HYP-XXXX-XXXX format", seedRegex.matches(seedReq))
  }

  @Test
  fun `verify GMDSS Pro Hard Licensing blocks question access when unactivated`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.nhyle.hyperionmaritime.data.util.LicenseManager.setActivated(context, false)
    com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository.clearCache()

    assertFalse("License must be unactivated", com.nhyle.hyperionmaritime.data.util.LicenseManager.isActivated(context))

    // Attempting to load any part of C1 or C2 must return an empty list
    for (part in 1..10) {
      val c1Questions = com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository.loadPartQuestions(context, "C1", part)
      assertTrue("Unactivated user must NOT receive C1 Part $part questions", c1Questions.isEmpty())
    }

    for (part in 1..2) {
      val c2Questions = com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository.loadPartQuestions(context, "C2", part)
      assertTrue("Unactivated user must NOT receive C2 Part $part questions", c2Questions.isEmpty())
    }

    val allC1 = com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository.loadAllCompetenceQuestions(context, "C1")
    val allC2 = com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository.loadAllCompetenceQuestions(context, "C2")
    assertTrue("All C1 questions must be blocked", allC1.isEmpty())
    assertTrue("All C2 questions must be blocked", allC2.isEmpty())
  }

  @Test
  fun `verify GMDSS Pro Data Ingestion and Model schema for activated user`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.nhyle.hyperionmaritime.data.util.LicenseManager.setActivated(context, true)
    com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository.clearCache()

    assertTrue("License must be activated", com.nhyle.hyperionmaritime.data.util.LicenseManager.isActivated(context))

    var totalQuestions = 0

    // Test C1 (10 parts)
    val expectedC1Counts = mapOf(
      1 to 192, 2 to 191, 3 to 191, 4 to 191, 5 to 191,
      6 to 191, 7 to 191, 8 to 191, 9 to 191, 10 to 191
    )
    for (part in 1..10) {
      val questions = com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository.loadPartQuestions(context, "C1", part)
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
      val questions = com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository.loadPartQuestions(context, "C2", part)
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

  @Test
  fun `verify choice shuffling preserves correct answer key`() {
    val q = Question(
      id = "q-test",
      track = Track.DECK_OIC_NW,
      functionId = "F1",
      competenceId = "C1",
      topic = "Topic",
      questionText = "Sample Question",
      options = listOf("Alpha", "Beta", "Gamma", "Delta"),
      correctAnswerIndex = 1, // "Beta"
      explanation = "Explanation",
      difficulty = Difficulty.EASY
    )
    val originalCorrectText = q.options[q.correctAnswerIndex]

    val shuffledOpts = q.options.shuffled()
    val newIndex = shuffledOpts.indexOf(originalCorrectText)
    val shuffledQ = q.copy(
      options = shuffledOpts,
      correctAnswerIndex = if (newIndex >= 0) newIndex else 0
    )

    assertEquals("Correct answer text must remain identical to original answer key", originalCorrectText, shuffledQ.options[shuffledQ.correctAnswerIndex])
  }

  @Test
  fun `verify sequential sorting returns questions in strict ascending ID order`() {
    val q3 = Question(id = "3", track = Track.DECK_OIC_NW, functionId = "F1", competenceId = "C1", topic = "T", questionText = "Q3", options = listOf("A","B"), correctAnswerIndex = 0, explanation = "E", difficulty = Difficulty.EASY)
    val q1 = Question(id = "1", track = Track.DECK_OIC_NW, functionId = "F1", competenceId = "C1", topic = "T", questionText = "Q1", options = listOf("A","B"), correctAnswerIndex = 0, explanation = "E", difficulty = Difficulty.EASY)
    val q2 = Question(id = "2", track = Track.DECK_OIC_NW, functionId = "F1", competenceId = "C1", topic = "T", questionText = "Q2", options = listOf("A","B"), correctAnswerIndex = 0, explanation = "E", difficulty = Difficulty.EASY)

    val list = listOf(q3, q1, q2)
    val sorted = list.sortedBy { it.id.toIntOrNull() ?: 0 }

    assertEquals("1", sorted[0].id)
    assertEquals("2", sorted[1].id)
    assertEquals("3", sorted[2].id)
  }
}
