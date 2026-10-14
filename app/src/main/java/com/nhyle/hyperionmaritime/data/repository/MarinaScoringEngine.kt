package com.nhyle.hyperionmaritime.data.repository

import com.nhyle.hyperionmaritime.data.model.CompetenceScore
import com.nhyle.hyperionmaritime.data.model.ExamResult
import com.nhyle.hyperionmaritime.data.model.FunctionScore
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.QuestionResult
import com.nhyle.hyperionmaritime.data.model.QuizMode
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.model.UserQuestionState
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

        val isMockExam = mode == QuizMode.MOCK_EXAM

        // Group by competence
        val competenceGroups = questions.groupBy { it.competenceId }
        val competenceScores = mutableListOf<CompetenceScore>()

        for ((compId, compQuestions) in competenceGroups) {
            val compInfo = MaritimeCompetenceRegistry.getCompetenceById(compId, track)
            var compRaw = 0
            var compMax = 0
            var compCorrect = 0

            for (q in compQuestions) {
                val userState = answers[q.id]
                val isAnswered = userState?.selectedIndex != null

                if (isMockExam) {
                    val pts = q.difficulty.points
                    compMax += pts
                    if (userState?.selectedIndex == q.correctAnswerIndex) {
                        compRaw += pts
                        compCorrect++
                    }
                } else {
                    if (isAnswered) {
                        compMax += 1
                        if (userState?.selectedIndex == q.correctAnswerIndex) {
                            compRaw += 1
                            compCorrect++
                        }
                    }
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
        val isOverallPassed = if (isMockExam) {
            competenceScores.isNotEmpty() && competenceScores.all { it.isPassed }
        } else {
            overallPct >= PASSING_PERCENTAGE
        }

        val questionResults = questions.map { q ->
            val state = answers[q.id]
            val selectedIdx = state?.selectedIndex
            val selectedText = selectedIdx?.let { if (it in q.options.indices) q.options[it] else null }
            val correctText = if (q.correctAnswerIndex in q.options.indices) q.options[q.correctAnswerIndex] else ""
            val isCorrect = selectedIdx == q.correctAnswerIndex
            QuestionResult(
                questionId = q.id,
                competenceId = q.competenceId,
                questionText = q.questionText,
                selectedOptionIndex = selectedIdx,
                selectedOptionText = selectedText,
                correctOptionIndex = q.correctAnswerIndex,
                correctOptionText = correctText,
                isCorrect = isCorrect,
                rationale = q.explanation
            )
        }

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
            questionStates = answers,
            questionResults = questionResults
        )
    }
}
