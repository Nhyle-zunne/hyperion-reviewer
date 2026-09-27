package com.example.data.model

enum class Track(val title: String, val shortName: String, val mockQuestions: Int, val timeMinutes: Int) {
    DECK_OIC_NW("Deck Operational Level (OIC-NW)", "Deck OIC-NW", 200, 200),
    GMDSS_GOC("GMDSS Radio Operator (GOC)", "GMDSS (GOC)", 100, 100)
}

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
)

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
)

enum class QuizMode {
    PRACTICE,        // Instant feedback, explanations immediately shown
    MOCK_EXAM,       // Timed, locked answers until submission, Question Palette
    COMPETENCE_DRILL,// Specific competence drill
    VAULT_DRILL,     // Practice questions from the Mistakes Notebook
    BOOKMARKS_DRILL  // Practice bookmarked questions
}

data class UserQuestionState(
    val questionId: String,
    val selectedIndex: Int? = null,
    val isFlagged: Boolean = false,
    val isSubmitted: Boolean = false
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
)

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
    val questionStates: Map<String, UserQuestionState>
)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
