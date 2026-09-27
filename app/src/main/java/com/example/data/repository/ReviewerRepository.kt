package com.example.data.repository

import com.example.data.local.ActiveSessionEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.ExamRecordEntity
import com.example.data.local.HyperionDatabase
import com.example.data.local.MistakeEntity
import com.example.data.local.SessionJsonHelper
import com.example.data.model.ExamResult
import com.example.data.model.Question
import com.example.data.model.QuizMode
import com.example.data.model.Track
import com.example.data.model.UserQuestionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ReviewerRepository(private val database: HyperionDatabase) {

    private val mistakeDao = database.mistakeDao()
    private val bookmarkDao = database.bookmarkDao()
    private val activeSessionDao = database.activeSessionDao()
    private val examRecordDao = database.examRecordDao()

    fun observeBookmarkIds(): Flow<Set<String>> {
        return bookmarkDao.getAllBookmarks().map { list ->
            list.map { it.questionId }.toSet()
        }
    }

    suspend fun toggleBookmark(questionId: String, currentBookmarked: Boolean) {
        if (currentBookmarked) {
            bookmarkDao.removeBookmark(questionId)
        } else {
            bookmarkDao.insertBookmark(BookmarkEntity(questionId = questionId))
        }
    }

    fun observeMistakes(): Flow<List<MistakeEntity>> {
        return mistakeDao.getAllMistakes()
    }

    suspend fun recordQuestionAttempt(questionId: String, isCorrect: Boolean) {
        val existing = mistakeDao.getMistake(questionId)
        if (!isCorrect) {
            val newTimes = (existing?.timesFailed ?: 0) + 1
            mistakeDao.insertOrUpdate(
                MistakeEntity(
                    questionId = questionId,
                    consecutiveCorrect = 0,
                    timesFailed = newTimes,
                    lastFailedTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            if (existing != null) {
                val newConsecutive = existing.consecutiveCorrect + 1
                if (newConsecutive >= 2) {
                    mistakeDao.deleteMistake(questionId)
                } else {
                    mistakeDao.insertOrUpdate(
                        existing.copy(consecutiveCorrect = newConsecutive)
                    )
                }
            }
        }
    }

    fun observeActiveSessionForTrack(track: Track): Flow<ActiveSessionEntity?> {
        return activeSessionDao.getActiveSessionForTrack(track.name)
    }

    suspend fun saveActiveSession(
        track: Track,
        mode: QuizMode,
        functionId: String?,
        competenceId: String?,
        currentQuestionIndex: Int,
        timeRemainingSeconds: Int,
        questionIds: List<String>,
        answers: Map<String, UserQuestionState>,
        completedCompetences: Set<String> = emptySet()
    ) {
        val answeredCount = answers.values.count { it.selectedIndex != null }
        val session = ActiveSessionEntity(
            id = "session_${track.name}",
            track = track.name,
            mode = mode.name,
            functionId = functionId,
            competenceId = competenceId,
            currentQuestionIndex = currentQuestionIndex,
            timeRemainingSeconds = timeRemainingSeconds,
            questionIds = questionIds.joinToString(","),
            answersJson = SessionJsonHelper.serializeAnswers(answers),
            totalQuestions = questionIds.size,
            answeredCount = answeredCount,
            completedCompetences = completedCompetences.joinToString(","),
            updatedTimestamp = System.currentTimeMillis()
        )
        activeSessionDao.saveActiveSession(session)
    }

    suspend fun clearActiveSession(track: Track) {
        activeSessionDao.clearActiveSessionForTrack(track.name)
    }

    suspend fun saveIncompleteSession(
        track: Track,
        mode: QuizMode,
        questions: List<Question>,
        answers: Map<String, UserQuestionState>
    ) {
        if (questions.isEmpty()) return
        val result = MarinaScoringEngine.evaluateExam(track, mode, questions, answers)
        val answeredCount = answers.values.count { it.selectedIndex != null }
        val isMock = mode == QuizMode.MOCK_EXAM
        val summary = if (isMock) {
            "INCOMPLETE • Answered $answeredCount/${questions.size} • Score: ${result.rawPointsEarned}/${result.maxPointsPossible} (${String.format("%.1f", result.overallPercentage)}%)"
        } else {
            "INCOMPLETE • Score: ${result.correctCount}/$answeredCount (${String.format("%.1f", result.overallPercentage)}%)"
        }
        val record = ExamRecordEntity(
            id = "inc_${UUID.randomUUID()}",
            timestamp = System.currentTimeMillis(),
            track = track.name,
            mode = mode.name,
            rawScore = result.correctCount,
            maxScore = if (isMock) result.maxPointsPossible else answeredCount,
            percentage = result.overallPercentage,
            isPassed = false,
            totalQuestions = questions.size,
            correctCount = result.correctCount,
            detailsSummary = summary,
            questionIds = questions.map { it.id }.joinToString(","),
            answersJson = SessionJsonHelper.serializeAnswers(answers),
            competenceBreakdownJson = SessionJsonHelper.serializeCompetenceScores(result.competenceScores)
        )
        examRecordDao.insertRecord(record)
    }

    suspend fun saveExamRecord(result: ExamResult, questionIds: List<String>) {
        val isMock = result.mode == QuizMode.MOCK_EXAM
        val summary = if (isMock) {
            "COMPLETED • Score: ${result.rawPointsEarned}/${result.maxPointsPossible} (${String.format("%.1f", result.overallPercentage)}%) - ${if (result.isOverallPassed) "PASSED" else "FAILED"}"
        } else {
            "COMPLETED • Score: ${result.correctCount}/${result.maxPointsPossible} (${String.format("%.1f", result.overallPercentage)}%)"
        }
        val record = ExamRecordEntity(
            id = result.examId,
            timestamp = result.timestamp,
            track = result.track.name,
            mode = result.mode.name,
            rawScore = result.correctCount,
            maxScore = result.maxPointsPossible,
            percentage = result.overallPercentage,
            isPassed = result.isOverallPassed,
            totalQuestions = result.totalQuestions,
            correctCount = result.correctCount,
            detailsSummary = summary,
            questionIds = questionIds.joinToString(","),
            answersJson = SessionJsonHelper.serializeAnswers(result.questionStates),
            competenceBreakdownJson = SessionJsonHelper.serializeCompetenceScores(result.competenceScores)
        )
        examRecordDao.insertRecord(record)
    }

    fun observeExamRecords(): Flow<List<ExamRecordEntity>> {
        return examRecordDao.getAllExamRecords()
    }

    fun observeExamRecordsForTrack(track: Track): Flow<List<ExamRecordEntity>> {
        return examRecordDao.getSessionsByTrack(track.name)
    }

    suspend fun getExamRecordById(id: String): ExamRecordEntity? {
        return examRecordDao.getExamRecordById(id)
    }

    suspend fun deleteExamRecord(id: String) {
        examRecordDao.deleteSessionById(id)
    }

    suspend fun deleteExamRecordsByIds(ids: List<String>) {
        examRecordDao.deleteSessionsByIds(ids)
    }

    suspend fun clearExamRecordsByTrack(track: Track) {
        examRecordDao.clearAllSessionsByTrack(track.name)
    }

    fun getQuestionsForSession(
        track: Track,
        mode: QuizMode,
        functionId: String? = null,
        competenceId: String? = null,
        vaultQuestionIds: Set<String> = emptySet(),
        bookmarkIds: Set<String> = emptySet(),
        isProUnlocked: Boolean = false
    ): List<Question> {
        // Deep defense: Unactivated mock exam returns empty list immediately
        if (mode == QuizMode.MOCK_EXAM && !isProUnlocked) {
            return emptyList()
        }

        val allTrackQuestions = MaritimeQuestionBank.getQuestionsByTrack(track, isProUnlocked = isProUnlocked)

        val filtered = when (mode) {
            QuizMode.PRACTICE -> {
                allTrackQuestions.shuffled()
            }
            QuizMode.MOCK_EXAM -> {
                // Must strictly contain ALL questions for the track (Deck: 200 items, GMDSS: 100 items)
                allTrackQuestions.shuffled()
            }
            QuizMode.COMPETENCE_DRILL -> {
                if (competenceId != null) {
                    allTrackQuestions.filter { it.competenceId == competenceId }.shuffled()
                } else if (functionId != null) {
                    allTrackQuestions.filter { it.functionId == functionId }.shuffled()
                } else {
                    allTrackQuestions.shuffled()
                }
            }
            QuizMode.VAULT_DRILL -> {
                MaritimeQuestionBank.ALL_QUESTIONS.filter { it.id in vaultQuestionIds }.shuffled()
            }
            QuizMode.BOOKMARKS_DRILL -> {
                MaritimeQuestionBank.ALL_QUESTIONS.filter { it.id in bookmarkIds }.shuffled()
            }
        }

        val result = if (filtered.isEmpty()) {
            allTrackQuestions
        } else {
            filtered
        }

        // Secondary defense check: If unactivated, truncate dataset to the first 5 questions
        return if (isProUnlocked) result else result.take(5)
    }
}
