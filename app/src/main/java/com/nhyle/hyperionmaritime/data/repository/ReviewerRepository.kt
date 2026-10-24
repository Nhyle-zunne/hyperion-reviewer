package com.nhyle.hyperionmaritime.data.repository

import com.nhyle.hyperionmaritime.data.local.ActiveSessionEntity
import com.nhyle.hyperionmaritime.data.local.BookmarkEntity
import com.nhyle.hyperionmaritime.data.local.ExamRecordEntity
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.local.MistakeEntity
import com.nhyle.hyperionmaritime.data.local.SessionJsonHelper
import com.nhyle.hyperionmaritime.data.model.ExamResult
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.QuizMode
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.model.UserQuestionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID

class ReviewerRepository(private val database: HyperionDatabase) {

    private val mistakeDao = database.mistakeDao()
    private val bookmarkDao = database.bookmarkDao()
    private val activeSessionDao = database.activeSessionDao()
    private val examRecordDao = database.examRecordDao()
    private val questionDao = database.questionDao()
    private val drillSessionDao = database.drillSessionDao()
    private val studyTimeSegmentDao = database.studyTimeSegmentDao()

    fun getQuestionDao() = questionDao
    fun getDrillSessionDao() = drillSessionDao
    fun getStudyTimeSegmentDao() = studyTimeSegmentDao

    fun getRecentDrillSessions(track: String, competence: String?): Flow<List<com.nhyle.hyperionmaritime.data.local.DrillSessionEntity>> {
        return drillSessionDao.getRecentSessions(track, competence).flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    suspend fun getRecentDrillSessionsOnce(track: String, competence: String?): List<com.nhyle.hyperionmaritime.data.local.DrillSessionEntity> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            drillSessionDao.getRecentSessionsOnce(track, competence)
        }

    suspend fun saveDrillSession(session: com.nhyle.hyperionmaritime.data.local.DrillSessionEntity) =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val existing = drillSessionDao.getSessionById(session.id)
            val mergedSession = session.copy(
                competence = session.competence?.takeIf { it.isNotBlank() } ?: existing?.competence,
                part = session.part?.takeIf { it.isNotBlank() } ?: existing?.part,
                mode = session.mode.takeIf { it.isNotBlank() } ?: existing?.mode ?: "PRACTICE",
                deliveryMode = session.deliveryMode.takeIf { it.isNotBlank() } ?: existing?.deliveryMode ?: "STANDARD"
            )
            drillSessionDao.saveDrillSession(mergedSession)
        }

    suspend fun getDrillSessionById(id: String): com.nhyle.hyperionmaritime.data.local.DrillSessionEntity? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            drillSessionDao.getSessionById(id)
        }

    suspend fun markDrillSessionCompleted(id: String) =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val existing = drillSessionDao.getSessionById(id)
            if (existing != null) {
                val updated = existing.copy(
                    isCompleted = true,
                    lastUpdated = System.currentTimeMillis()
                )
                drillSessionDao.saveDrillSession(updated)
            } else {
                drillSessionDao.markCompleted(id)
            }
        }

    suspend fun deleteDrillSession(id: String) =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            drillSessionDao.deleteSessionById(id)
            examRecordDao.deleteSessionById(id)
        }

    suspend fun clearAllDrillSessions() =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            drillSessionDao.clearAllDrillSessions()
        }

    fun observeBookmarkIds(): Flow<Set<String>> {
        return bookmarkDao.getAllBookmarks().map { list ->
            list.map { it.questionId }.toSet()
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    suspend fun toggleBookmark(questionId: String, currentBookmarked: Boolean) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (currentBookmarked) {
            bookmarkDao.removeBookmark(questionId)
        } else {
            bookmarkDao.insertBookmark(BookmarkEntity(questionId = questionId))
        }
    }

    suspend fun removeBookmark(questionId: String): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        bookmarkDao.removeBookmark(questionId)
    }

    suspend fun clearAllBookmarks(): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        bookmarkDao.clearAllBookmarks()
    }

    suspend fun removeMistake(questionId: String): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        mistakeDao.removeMistake(questionId)
    }

    suspend fun clearAllMistakes(): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        mistakeDao.clearAllMistakes()
    }

    fun getQuestionCountForCompetence(track: String, competence: String): Int {
        val t = when {
            track.equals(Track.ENGINE_OIC_EW.name, ignoreCase = true) || track.contains("ENGINE_OIC_EW", ignoreCase = true) || track.contains("OIC_EW", ignoreCase = true) -> Track.ENGINE_OIC_EW
            track.equals(Track.DECK_MGT.name, ignoreCase = true) || track.contains("DECK_MGT", ignoreCase = true) -> Track.DECK_MGT
            track.equals(Track.ENGINE_MGT.name, ignoreCase = true) || track.contains("ENGINE_MGT", ignoreCase = true) -> Track.ENGINE_MGT
            track.contains("GMDSS", ignoreCase = true) -> Track.GMDSS_GOC
            else -> Track.DECK_OIC_NW
        }
        return MaritimeCompetenceRegistry.getDynamicQuestionCount(competence, t, isCrpMode = false)
    }

    suspend fun getDbQuestionCountForCompetence(track: String, competence: String): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        questionDao.getQuestionCountForCompetence(track, competence)
    }

    suspend fun getDbTotalQuestionCountForTrack(track: String): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        questionDao.getTotalQuestionCountForTrack(track)
    }

    fun observeMistakes(): Flow<List<MistakeEntity>> {
        return mistakeDao.getAllMistakes().flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    suspend fun recordQuestionAttempt(questionId: String, isCorrect: Boolean, requiredPasses: Int = 2) {
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
                if (newConsecutive >= requiredPasses) {
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
        val isMistakes = mode == QuizMode.MISTAKES || competenceId == "MISTAKES"
        val answeredCount = answers.values.count { it.selectedIndex != null }
        val session = ActiveSessionEntity(
            id = if (isMistakes) "session_${track.name}_MISTAKES" else "session_${track.name}",
            track = if (isMistakes) "${track.name}_MISTAKES" else track.name,
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

    suspend fun clearActiveMistakesSession(track: Track) {
        activeSessionDao.clearActiveSessionForTrack("${track.name}_MISTAKES")
    }

    suspend fun getActiveSessionForTrackOnce(trackName: String): ActiveSessionEntity? {
        return activeSessionDao.getActiveSessionForTrackOnce(trackName)
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
        examRecordDao.deleteSessionById(result.examId)
        examRecordDao.deleteInprogressSessionsForTrackAndScope(result.track.name, result.competence, result.part)
        val existing = examRecordDao.getExamRecordById(result.examId)
        val finalComp = result.competence?.takeIf { it.isNotBlank() } ?: existing?.competence
        val finalPart = result.part?.takeIf { it.isNotBlank() } ?: existing?.part
        val finalDrillTitle = result.drillTitle?.takeIf { it.isNotBlank() } ?: existing?.drillTitle

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
            competenceBreakdownJson = SessionJsonHelper.serializeCompetenceScores(result.competenceScores),
            drillTitle = finalDrillTitle,
            competence = finalComp,
            part = finalPart
        )
        examRecordDao.insertRecord(record)
    }

    suspend fun insertOrUpdateExamRecord(record: ExamRecordEntity) {
        val existing = examRecordDao.getExamRecordById(record.id)
        val merged = record.copy(
            competence = record.competence?.takeIf { it.isNotBlank() } ?: existing?.competence,
            part = record.part?.takeIf { it.isNotBlank() } ?: existing?.part,
            drillTitle = record.drillTitle?.takeIf { it.isNotBlank() } ?: existing?.drillTitle
        )
        examRecordDao.insertRecord(merged)
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

    suspend fun clearHistory() {
        examRecordDao.clearHistory()
        drillSessionDao.clearAllDrillSessions()
    }

    suspend fun deleteExamRecord(id: String) {
        examRecordDao.deleteSessionById(id)
        drillSessionDao.deleteSessionById(id)
    }

    suspend fun deleteExamRecordsByIds(ids: List<String>) {
        examRecordDao.deleteSessionsByIds(ids)
        ids.forEach { drillSessionDao.deleteSessionById(it) }
    }

    suspend fun clearExamRecordsByTrack(track: Track) {
        examRecordDao.clearAllSessionsByTrack(track.name)
        drillSessionDao.clearSessionsForTrack(track.name)
    }

    suspend fun deleteActiveSessionByTrack(trackId: String) {
        activeSessionDao.deleteActiveSessionByTrack(trackId)
    }

    suspend fun deleteIncompleteSessionsByTrack(trackId: String) {
        examRecordDao.deleteIncompleteSessionsByTrack(trackId)
    }

    suspend fun clearAllByTrack(track: String) {
        examRecordDao.clearAllByTrack(track)
        drillSessionDao.clearSessionsForTrack(track)
    }

    suspend fun purgeIncompleteRecords() {
        examRecordDao.purgeIncompleteRecords()
    }

    suspend fun getEncounteredQuestionIds(track: Track, competenceId: String? = null): Set<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val seenIds = mutableSetOf<String>()
            val records = examRecordDao.getSessionsByTrackOnce(track.name)
            for (record in records) {
                if (record.answersJson.isNotBlank()) {
                    val answers = SessionJsonHelper.deserializeAnswers(record.answersJson)
                    for ((qid, state) in answers) {
                        if (state.selectedIndex != null && qid.isNotBlank()) {
                            seenIds.add(qid.trim())
                        }
                    }
                } else {
                    val isIncomplete = record.id.startsWith("inc_") ||
                            record.detailsSummary.contains("INCOMPLETE", ignoreCase = true) ||
                            record.detailsSummary.contains("IN_PROGRESS", ignoreCase = true)
                    if (!isIncomplete && record.questionIds.isNotBlank()) {
                        for (rawId in record.questionIds.split(",")) {
                            val trimmed = rawId.trim()
                            if (trimmed.isNotEmpty()) {
                                seenIds.add(trimmed)
                            }
                        }
                    }
                }
            }

            val drillSessions = drillSessionDao.getAllSessionsForTrackOnce(track.name)
            for (drill in drillSessions) {
                if (drill.answersJson.isNotBlank()) {
                    val answers = SessionJsonHelper.deserializeAnswers(drill.answersJson)
                    for ((qid, state) in answers) {
                        if (state.selectedIndex != null && qid.isNotBlank()) {
                            seenIds.add(qid.trim())
                        }
                    }
                } else if (drill.isCompleted && drill.questionIds.isNotBlank()) {
                    for (rawId in drill.questionIds.split(",")) {
                        val trimmed = rawId.trim()
                        if (trimmed.isNotEmpty()) {
                            seenIds.add(trimmed)
                        }
                    }
                }
            }
            seenIds
        } catch (e: Exception) {
            android.util.Log.e("ReviewerRepository", "Failed to retrieve encountered question IDs: ${e.message}", e)
            emptySet()
        }
    }

    fun getQuestionsForSession(
        track: Track,
        mode: QuizMode,
        functionId: String? = null,
        competenceId: String? = null,
        vaultQuestionIds: Set<String> = emptySet(),
        bookmarkIds: Set<String> = emptySet(),
        isProUnlocked: Boolean = false,
        prioritizeUnseen: Boolean = false,
        seenQuestionIds: Set<String> = emptySet(),
        selectedFunctions: Set<String>? = null
    ): List<Question> {
        // Deep defense: Unactivated mock exam returns empty list immediately
        if (mode == QuizMode.MOCK_EXAM && !isProUnlocked) {
            return emptyList()
        }

        val allTrackQuestions = MaritimeQuestionBank.getQuestionsByTrack(track, isProUnlocked = isProUnlocked)

        val filtered = when (mode) {
            QuizMode.PRACTICE -> {
                if (prioritizeUnseen) {
                    QuestionSelectionDispatcher.selectQuestions(
                        eligiblePool = allTrackQuestions,
                        requestedCount = allTrackQuestions.size,
                        seenQuestionIds = seenQuestionIds,
                        prioritizeUnseen = true,
                        shuffleQuestions = true
                    )
                } else {
                    allTrackQuestions.shuffled()
                }
            }
            QuizMode.MOCK_EXAM -> {
                // Official MARINA permit breakdown: filtered to selected functions if specified
                val mockDataset = MaritimeQuestionBank.getMockExamDataset(
                    track = track,
                    isProUnlocked = isProUnlocked,
                    selectedFunctions = selectedFunctions
                )
                if (prioritizeUnseen) {
                    QuestionSelectionDispatcher.selectQuestions(
                        eligiblePool = mockDataset,
                        requestedCount = mockDataset.size,
                        seenQuestionIds = seenQuestionIds,
                        prioritizeUnseen = true,
                        shuffleQuestions = true
                    )
                } else {
                    mockDataset
                }
            }
            QuizMode.COMPETENCE_DRILL -> {
                val pool = if (competenceId != null) {
                    allTrackQuestions.filter { it.competenceId == competenceId }
                } else if (functionId != null) {
                    allTrackQuestions.filter { it.functionId == functionId }
                } else {
                    allTrackQuestions
                }
                if (prioritizeUnseen) {
                    QuestionSelectionDispatcher.selectQuestions(
                        eligiblePool = pool,
                        requestedCount = pool.size,
                        seenQuestionIds = seenQuestionIds,
                        prioritizeUnseen = true,
                        shuffleQuestions = true
                    )
                } else {
                    pool.shuffled()
                }
            }
            QuizMode.VAULT_DRILL, QuizMode.MISTAKES -> {
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
