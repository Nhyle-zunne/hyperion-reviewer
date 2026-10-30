package com.nhyle.hyperionmaritime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MistakeDao {
    @Query("SELECT * FROM mistakes ORDER BY lastFailedTimestamp DESC")
    fun getAllMistakes(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes WHERE questionId = :questionId LIMIT 1")
    suspend fun getMistake(questionId: String): MistakeEntity?

    @Query("SELECT * FROM mistakes ORDER BY lastFailedTimestamp DESC")
    suspend fun getAllMistakesOnce(): List<MistakeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mistakes: List<MistakeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(mistake: MistakeEntity)

    @Query("DELETE FROM mistakes WHERE questionId = :questionId")
    suspend fun deleteMistake(questionId: String)

    @Query("DELETE FROM mistakes WHERE questionId = :questionId")
    suspend fun removeMistake(questionId: String): Int

    @Query("DELETE FROM mistakes")
    suspend fun clearAll()

    @Query("DELETE FROM mistakes")
    suspend fun clearAllMistakes(): Int
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY addedTimestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY addedTimestamp DESC")
    suspend fun getAllBookmarksOnce(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE questionId = :questionId)")
    fun isBookmarked(questionId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE questionId = :questionId")
    suspend fun removeBookmark(questionId: String): Int

    @Query("DELETE FROM bookmarks")
    suspend fun clearAll()

    @Query("DELETE FROM bookmarks")
    suspend fun clearAllBookmarks(): Int
}

@Dao
interface ActiveSessionDao {
    @Query("SELECT * FROM active_session WHERE track = :trackName LIMIT 1")
    fun getActiveSessionForTrack(trackName: String): Flow<ActiveSessionEntity?>

    @Query("SELECT * FROM active_session WHERE track = :trackName LIMIT 1")
    suspend fun getActiveSessionForTrackOnce(trackName: String): ActiveSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveSession(session: ActiveSessionEntity)

    @Query("DELETE FROM active_session WHERE track = :trackName")
    suspend fun clearActiveSessionForTrack(trackName: String)

    @Query("DELETE FROM active_session WHERE track = :trackId")
    suspend fun deleteActiveSessionByTrack(trackId: String)

    @Query("DELETE FROM active_session")
    suspend fun clearAllActiveSessions()
}

@Dao
interface ExamRecordDao {
    @Query("SELECT * FROM exam_records ORDER BY timestamp DESC")
    fun getAllExamRecords(): Flow<List<ExamRecordEntity>>

    @Query("SELECT * FROM exam_records ORDER BY timestamp DESC")
    suspend fun getAllExamRecordsOnce(): List<ExamRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<ExamRecordEntity>)

    @Query("SELECT * FROM exam_records WHERE track LIKE :track COLLATE NOCASE ORDER BY timestamp DESC")
    fun getSessionsByTrack(track: String): Flow<List<ExamRecordEntity>>

    @Query("SELECT * FROM exam_records WHERE track LIKE :track COLLATE NOCASE")
    suspend fun getSessionsByTrackOnce(track: String): List<ExamRecordEntity>

    @Query("SELECT * FROM exam_records WHERE id = :id LIMIT 1")
    suspend fun getExamRecordById(id: String): ExamRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ExamRecordEntity)

    @Query("DELETE FROM exam_records WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("DELETE FROM exam_records WHERE track = :track")
    suspend fun clearAllByTrack(track: String)

    @Query("DELETE FROM exam_records WHERE track = :trackId AND (id LIKE 'inc_%' OR detailsSummary LIKE 'INCOMPLETE%')")
    suspend fun deleteIncompleteSessionsByTrack(trackId: String)

    @Query("""
        DELETE FROM exam_records 
        WHERE track = :track 
        AND detailsSummary LIKE 'IN_PROGRESS%' 
        AND (
            (:competence = 'MISTAKES' AND (competence = 'MISTAKES' OR mode = 'MISTAKES'))
            OR
            ((:competence IS NULL OR :competence != 'MISTAKES') AND (competence IS NULL OR competence != 'MISTAKES') AND mode != 'MISTAKES' AND (:competence IS NULL OR competence = :competence) AND (:part IS NULL OR part = :part))
        )
    """)
    suspend fun deleteInprogressSessionsForTrackAndScope(track: String, competence: String?, part: String?)

    @Query("DELETE FROM exam_records WHERE id IN (:ids)")
    suspend fun deleteSessionsByIds(ids: List<String>)

    @Query("DELETE FROM exam_records WHERE track LIKE :track COLLATE NOCASE")
    suspend fun clearAllSessionsByTrack(track: String)

    @Query("DELETE FROM exam_records WHERE id LIKE 'inc_%' OR detailsSummary LIKE 'INCOMPLETE%'")
    suspend fun purgeIncompleteRecords()

    @Query("SELECT questionIds FROM exam_records WHERE track LIKE :track COLLATE NOCASE AND detailsSummary NOT LIKE 'IN_PROGRESS%'")
    suspend fun getRecordedQuestionIdsByTrack(track: String): List<String>

    @Query("DELETE FROM exam_records")
    suspend fun clearHistory()
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: String): QuestionEntity?

    @Query("SELECT COUNT(*) FROM questions WHERE track = :track AND competence = :competence")
    suspend fun getQuestionCountForCompetence(track: String, competence: String): Int

    @Query("SELECT COUNT(*) FROM questions WHERE track = :track")
    suspend fun getTotalQuestionCountForTrack(track: String): Int

    @Query("SELECT * FROM questions WHERE track = :track AND competence = :competence")
    suspend fun getQuestionsByCompetence(track: String, competence: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE track = :track")
    suspend fun getQuestionsByTrack(track: String): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions")
    suspend fun clearAllQuestions()
}

@Dao
interface DrillSessionDao {
    @Query("""
        SELECT * FROM drill_sessions 
        WHERE track = :track 
        AND (:competence IS NULL OR competence = :competence)
        ORDER BY isCompleted ASC, lastUpdated DESC 
        LIMIT 5
    """)
    fun getRecentSessions(track: String, competence: String?): Flow<List<DrillSessionEntity>>

    @Query("""
        SELECT * FROM drill_sessions 
        WHERE track = :track 
        AND (:competence IS NULL OR competence = :competence)
        ORDER BY isCompleted ASC, lastUpdated DESC 
        LIMIT 5
    """)
    suspend fun getRecentSessionsOnce(track: String, competence: String?): List<DrillSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDrillSession(session: DrillSessionEntity)

    @Query("SELECT * FROM drill_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): DrillSessionEntity?

    @Query("UPDATE drill_sessions SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: String)

    @Query("DELETE FROM drill_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("DELETE FROM drill_sessions WHERE track = :track")
    suspend fun clearSessionsForTrack(track: String)

    @Query("SELECT questionIds FROM drill_sessions WHERE track = :track AND (isCompleted = 1 OR answeredCount > 0)")
    suspend fun getRecordedQuestionIdsForTrack(track: String): List<String>

    @Query("SELECT * FROM drill_sessions WHERE track = :track")
    suspend fun getAllSessionsForTrackOnce(track: String): List<DrillSessionEntity>

    @Query("DELETE FROM drill_sessions")
    suspend fun clearAllDrillSessions()
}


