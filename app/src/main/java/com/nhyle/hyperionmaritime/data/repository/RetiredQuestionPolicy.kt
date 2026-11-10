package com.nhyle.hyperionmaritime.data.repository

import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track

/**
 * Retired questions are excluded only from NEW practice, Quick Scan, mock,
 * flashcard, and search selections. Their original encrypted CSV rows and
 * Question IDs stay in ALL_QUESTIONS so previously saved bookmarks, mistakes,
 * progress, session history and highlights remain resolvable.
 *
 * C10 row numbers are stable 1–416; NewTrackQuestionBank produces 4-digit
 * Android identifiers. PWA uses the same original row numbers with 3 digits.
 */
object RetiredQuestionPolicy {
    val engineManagementC10Numbers: Set<Int> = setOf(182, 373, 406)
    val engineManagementC10Ids: Set<String> =
        engineManagementC10Numbers.map { "EM-C10-%04d".format(it) }.toSet()

    fun isRetired(question: Question): Boolean =
        question.isRetired || (question.track == Track.ENGINE_MGT &&
            question.competenceId == "C10" &&
            question.id in engineManagementC10Ids)

    /** CSV row lifecycle marker. Parsing is read-only and never drops the row. */
    fun retiredFromCsv(row: List<String>, rows: List<List<String>>): Boolean {
        val header = rows.take(15).firstOrNull { cells ->
            cells.any { it.trim().equals("HYPERION_STATUS", ignoreCase = true) }
        } ?: return false
        val statusColumn = header.indexOfFirst { it.trim().equals("HYPERION_STATUS", ignoreCase = true) }
        return statusColumn >= 0 && row.getOrNull(statusColumn)?.trim()?.equals("RETIRED", ignoreCase = true) == true
    }

    fun active(questions: List<Question>): List<Question> =
        questions.filterNot(::isRetired)
}
