package com.example.data.repository

import com.example.data.model.CompetenceScore
import com.example.data.model.ExamResult
import com.example.data.model.FunctionScore
import com.example.data.model.Question
import com.example.data.model.QuizMode
import com.example.data.model.Track
import com.example.data.model.UserQuestionState
import java.util.UUID

object MarinaScoringEngine {

    const val PASSING_PERCENTAGE = 70.0

    fun evaluateExam(
        track: Track,
        mode: QuizMode,
        questions: List<Question>,
        answers: Map<String, UserQuestionState>
    ): ExamResult {
        var totalRawPoints = 0
        var totalMaxPoints = 0
        var totalCorrect = 0

        // Group by competence
        val competenceGroups = questions.groupBy { it.competenceId }
        val competenceScores = mutableListOf<CompetenceScore>()

        for ((compId, compQuestions) in competenceGroups) {
            val compInfo = MaritimeCompetenceRegistry.getCompetenceById(compId, track)
            var compRaw = 0
            var compMax = 0
            var compCorrect = 0

            for (q in compQuestions) {
                val pts = q.difficulty.points
                compMax += pts
                val userState = answers[q.id]
                if (userState?.selectedIndex == q.correctAnswerIndex) {
                    compRaw += pts
                    compCorrect++
                }
            }

            val compPct = if (compMax > 0) (compRaw.toDouble() / compMax) * 100.0 else 0.0
            val isPassed = compPct >= PASSING_PERCENTAGE

            competenceScores.add(
                CompetenceScore(
                    competenceId = compId,
                    competenceTitle = compInfo?.title ?: compQuestions.firstOrNull()?.topic ?: compId,
                    functionId = compInfo?.functionId ?: compQuestions.firstOrNull()?.functionId ?: "F1",
                    questionCount = compQuestions.size,
                    correctCount = compCorrect,
                    rawPointsEarned = compRaw,
                    maxPointsPossible = compMax,
                    percentage = compPct,
                    isPassed = isPassed
                )
            )

            totalRawPoints += compRaw
            totalMaxPoints += compMax
            totalCorrect += compCorrect
        }

        // Group by function
        val functionGroups = competenceScores.groupBy { it.functionId }
        val functionScores = mutableListOf<FunctionScore>()

        for ((funcId, compScoreList) in functionGroups) {
            val funcRaw = compScoreList.sumOf { it.rawPointsEarned }
            val funcMax = compScoreList.sumOf { it.maxPointsPossible }
            val funcPct = if (funcMax > 0) (funcRaw.toDouble() / funcMax) * 100.0 else 0.0

            // Official MARINA Policy: Candidate passes ONLY if they obtain >= 70.0% in EVERY competence
            val allCompetencesPassed = compScoreList.all { it.isPassed }

            functionScores.add(
                FunctionScore(
                    functionId = funcId,
                    functionTitle = MaritimeCompetenceRegistry.getFunctionTitle(funcId),
                    competences = compScoreList,
                    rawPointsEarned = funcRaw,
                    maxPointsPossible = funcMax,
                    percentage = funcPct,
                    isPassed = allCompetencesPassed
                )
            )
        }

        val overallPct = if (totalMaxPoints > 0) (totalRawPoints.toDouble() / totalMaxPoints) * 100.0 else 0.0
        val isOverallPassed = competenceScores.isNotEmpty() && competenceScores.all { it.isPassed }

        return ExamResult(
            examId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            track = track,
            mode = mode,
            totalQuestions = questions.size,
            correctCount = totalCorrect,
            rawPointsEarned = totalRawPoints,
            maxPointsPossible = totalMaxPoints,
            overallPercentage = overallPct,
            isOverallPassed = isOverallPassed,
            functionScores = functionScores,
            competenceScores = competenceScores,
            questionStates = answers
        )
    }
}
