package com.nhyle.hyperionmaritime.data.model

enum class Track(val title: String, val shortName: String, val mockQuestions: Int, val timeMinutes: Int) {
    DECK_OIC_NW("Deck Operational Level (OIC-NW)", "Deck OIC-NW", 200, 200),
    GMDSS_GOC("GMDSS Radio Operator (GOC)", "GMDSS (GOC)", 100, 100),
    ENGINE_OIC_EW("Engine Operational Level (OIC-EW)", "Engine OIC-EW", 200, 200),
    DECK_MGT("Deck Management Level", "Deck MGT", 200, 200),
    ENGINE_MGT("Engine Management Level", "Engine MGT", 200, 200);

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

fun shuffleOptionsStable(q: Question, seed: String): Question {
    if (q.options.size < 2) return q
    val hasNonShufflableOption = q.options.any { opt ->
        val trimmed = opt.trim().lowercase()
        trimmed.contains("all of the above") ||
        trimmed.contains("none of the above") ||
        trimmed.contains("none of these") ||
        trimmed.contains("all of these") ||
        trimmed.contains("both a and b") ||
        trimmed.contains("a and b") ||
        trimmed.contains("a and c") ||
        trimmed.contains("b and c") ||
        trimmed.contains("neither")
    }
    if (hasNonShufflableOption) return q

    val originalCorrectText = q.options.getOrNull(q.correctAnswerIndex) ?: return q
    val rng = java.util.Random((q.id + seed).hashCode().toLong())
    val shuffledOpts = q.options.toMutableList()
    java.util.Collections.shuffle(shuffledOpts, rng)
    val newIndex = shuffledOpts.indexOf(originalCorrectText)
    return q.copy(
        options = shuffledOpts,
        correctAnswerIndex = if (newIndex >= 0) newIndex else q.correctAnswerIndex
    )
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

fun classifyQuestionDifficulty(qText: String, options: List<String>, fallbackNo: Int = 0): Difficulty {
    val lower = qText.lowercase()
    val totalOptionLen = options.sumOf { it.length }

    // 1. Calculations, formulas, quantitative navigation, and stability
    val isCalculationOrFormula = lower.contains("calculate") ||
            lower.contains("formula") ||
            lower.contains("degrees") ||
            lower.contains("knots") ||
            lower.contains("bearing") ||
            lower.contains("draft") ||
            lower.contains("displacement") ||
            lower.contains("stability") ||
            lower.contains("metacentric") ||
            Regex("""\b\d+(\.\d+)?\s*(m|ft|rpm|nm|kts|deg|°)\b""").containsMatchIn(lower)

    // 2. High-level analysis / master decision making / emergency protocols
    val isEvaluation = lower.contains("immediate action") ||
            lower.contains("in accordance with solas") ||
            lower.contains("master's responsibility") ||
            lower.contains("salvage") ||
            lower.contains("severe damage") ||
            lower.contains("distress alert") ||
            lower.contains("mayday") ||
            lower.contains("search and rescue")

    if (isCalculationOrFormula || isEvaluation || (qText.length > 220 && totalOptionLen > 180)) {
        return Difficulty.DIFFICULT
    }

    // 3. Situational procedures, rules, and operations
    val isApplication = lower.contains("action to be taken") ||
            lower.contains("what should you do") ||
            lower.contains("in the event of") ||
            lower.contains("colregs rule") ||
            lower.contains("procedure") ||
            lower.contains("precaution") ||
            lower.contains("during cargo") ||
            lower.contains("prior to") ||
            lower.contains("radio watch") ||
            lower.contains("dsc call")

    if (isApplication || qText.length > 130 || totalOptionLen > 120) {
        return Difficulty.MODERATE
    }

    // 4. Default to Easy for definitions, factual recall, and terminology
    return Difficulty.EASY
}
