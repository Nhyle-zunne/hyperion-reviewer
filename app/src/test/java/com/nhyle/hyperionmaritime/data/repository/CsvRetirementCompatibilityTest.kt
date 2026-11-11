package com.nhyle.hyperionmaritime.data.repository

import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track
import org.junit.Assert.*
import org.junit.Test

/** Retire only new quiz delivery; never re-number or delete saved questions. */
class CsvRetirementCompatibilityTest {
    private fun question(id: String, retired: Boolean) = Question(
        id = id,
        track = Track.DECK_OIC_NW,
        functionId = "F1",
        competenceId = "C1",
        topic = "Safety",
        questionText = "Synthetic test item?",
        options = listOf("A", "B", "C", "D"),
        correctAnswerIndex = 0,
        explanation = "",
        difficulty = Difficulty.EASY,
        isRetired = retired
    )

    @Test fun statusColumnIsResolvedByNameAndOnlyRetiredIsFiltered() {
        val header = listOf("No.", "Question", "A", "B", "C", "D", "Answer", "Explanation", "HYPERION_STATUS")
        val active = listOf("1", "One?", "A", "B", "C", "D", "A", "Rationale", "ACTIVE")
        val retired = listOf("2", "Two?", "A", "B", "C", "D", "B", "Rationale", " retired ")
        val rows = listOf(header, active, retired)
        assertFalse(RetiredQuestionPolicy.retiredFromCsv(active, rows))
        assertTrue(RetiredQuestionPolicy.retiredFromCsv(retired, rows))
        assertFalse(RetiredQuestionPolicy.retiredFromCsv(retired.dropLast(1), rows))
        assertFalse(RetiredQuestionPolicy.retiredFromCsv(retired, listOf(header.dropLast(1), retired)))

        val original = listOf(question("D-F1-C1-001", false), question("D-F1-C1-002", true))
        assertEquals(listOf("D-F1-C1-001"), RetiredQuestionPolicy.active(original).map { it.id })
        assertEquals(2, original.size)
        assertEquals("D-F1-C1-002", original[1].id)
    }

    @Test fun savedQuestionFlagsDoNotChangeIdentifiersOrOptions() {
        val original = question("D-F1-C1-002", false)
        val retired = original.copy(isRetired = true)
        assertEquals(original.id, retired.id)
        assertEquals(original.correctAnswerIndex, retired.correctAnswerIndex)
        assertEquals(original.options, retired.options)
        assertTrue(RetiredQuestionPolicy.isRetired(retired))
        assertFalse(RetiredQuestionPolicy.isRetired(original))
    }
}
