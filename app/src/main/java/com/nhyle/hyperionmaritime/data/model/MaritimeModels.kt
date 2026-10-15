package com.nhyle.hyperionmaritime.data.model

enum class Track(val title: String, val shortName: String, val mockQuestions: Int, val timeMinutes: Int) {
    DECK_OIC_NW("Deck Operational Level (OIC-NW)", "Deck OIC-NW", 200, 200),
    GMDSS_GOC("GMDSS Radio Operator (GOC)", "GMDSS (GOC)", 100, 100),
    ENGINE_OIC_EW("Engine Operational Level (OIC-EW)", "Engine OIC-EW", 150, 150);

    companion object {
        val OIC_NW: Track get() = DECK_OIC_NW
        val GMDSS: Track get() = GMDSS_GOC
        val ENGINE: Track get() = ENGINE_OIC_EW
    }
}

typealias ReviewTrack = Track
typealias ReviewerTrack = Track

enum class Difficulty(val points: Int, val label: String, val bloomLevel: String) {
    EASY(3, "Easy (+3 pts)", "Remembering & Understanding"),
    MODERATE(6, "Moderate (+6 pts)", "Applying & Analyzing"),
    DIFFICULT(9, "Difficult (+9 pts)", "Evaluating & Creating")
}

data class CompetenceInfo(
    val competenceId: String,
    val functionId: String,
    val functionName: String,
    val track: Track,
    val code: String,
    val title: String,
    val officialTOSItems: Int,
    val description: String
) {
    val id: String get() = competenceId
}

data class Question(
    val id: String,
    val track: Track,
    val functionId: String,       // "F1", "F2", "F3", or "GMDSS"
    val competenceId: String,     // "C1", "C2", "C3", etc.
    val topic: String,            // "COLREGs", "ARPA", "DSC", "MARPOL", etc.
    val questionText: String,
    val options: List<String>,    // 4 choices
    val correctAnswerIndex: Int,  // 0..3
    val explanation: String,
    val difficulty: Difficulty,
    val isBookmarked: Boolean = false,
    val isWrongInVault: Boolean = false
) {
    val rationale: String get() = explanation
}

enum class QuizMode {
    PRACTICE,        // Instant feedback, explanations immediately shown
    MOCK_EXAM,       // Timed, locked answers until submission, Question Palette
    COMPETENCE_DRILL,// Specific competence drill
    VAULT_DRILL,     // Practice questions from the Mistakes Notebook
    BOOKMARKS_DRILL, // Practice bookmarked questions
    MISTAKES         // Vault redemption practice session
}

data class UserQuestionState(
    val questionId: String,
    val selectedIndex: Int? = null,
    val isFlagged: Boolean = false,
    val isSubmitted: Boolean = false,
    val isCorrect: Boolean = false
)

data class CurrentQuestionStats(
    val questionId: String,
    val track: String,
    val allTimeCorrect: Int,
    val allTimeIncorrect: Int,
    val recentAttempts: List<Boolean>,
    val isLoaded: Boolean = true
) {
    val totalAttempts: Int get() = allTimeCorrect + allTimeIncorrect

    fun formattedSummary(): String {
        val correctUnit = if (allTimeCorrect == 1) "time" else "times"
        val incorrectUnit = if (allTimeIncorrect == 1) "time" else "times"
        return "Correct $allTimeCorrect $correctUnit, incorrect $allTimeIncorrect $incorrectUnit"
    }
}

data class QuestionResult(
    val questionId: String,
    val competenceId: String,
    val questionText: String,
    val selectedOptionIndex: Int? = null,
    val selectedOptionText: String? = null,
    val correctOptionIndex: Int = 0,
    val correctOptionText: String = "",
    val isCorrect: Boolean = false,
    val rationale: String? = null
)

data class CompetenceScore(
    val competenceId: String,
    val competenceTitle: String,
    val functionId: String,
    val questionCount: Int,
    val correctCount: Int,
    val rawPointsEarned: Int,
    val maxPointsPossible: Int,
    val percentage: Double,
    val isPassed: Boolean // >= 70.0%
) {
    val title: String get() = competenceTitle
    val totalQuestions: Int get() = questionCount
    val score: Int get() = correctCount
    val weightedScore: Double get() = rawPointsEarned.toDouble()
    val totalPossibleWeightedScore: Double get() = maxPointsPossible.toDouble()
}

data class FunctionScore(
    val functionId: String,
    val functionTitle: String,
    val competences: List<CompetenceScore>,
    val rawPointsEarned: Int,
    val maxPointsPossible: Int,
    val percentage: Double,
    val isPassed: Boolean // Passes ONLY if all individual competences >= 70.0%
)

data class ExamResult(
    val examId: String,
    val timestamp: Long,
    val track: Track,
    val mode: QuizMode,
    val totalQuestions: Int,
    val correctCount: Int,
    val rawPointsEarned: Int,
    val maxPointsPossible: Int,
    val overallPercentage: Double,
    val isOverallPassed: Boolean, // Candidate passes ONLY if >= 70.0% in EVERY competence
    val functionScores: List<FunctionScore>,
    val competenceScores: List<CompetenceScore>,
    val questionStates: Map<String, UserQuestionState>,
    val questionResults: List<QuestionResult> = emptyList(),
    val drillTitle: String? = null,
    val competence: String? = null,
    val part: String? = null
) {
    val isPassed: Boolean get() = isOverallPassed
    val score: Int get() = correctCount
    val percentage: Double get() = overallPercentage
    val weightedScore: Double get() = rawPointsEarned.toDouble()
    val totalPossibleWeightedScore: Double get() = maxPointsPossible.toDouble()
    val competenceBreakdown: List<CompetenceScore> get() = competenceScores
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
