package com.example.data.local

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(mistake: MistakeEntity)

    @Query("DELETE FROM mistakes WHERE questionId = :questionId")
    suspend fun deleteMistake(questionId: String)

    @Query("DELETE FROM mistakes")
    suspend fun clearAll()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY addedTimestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE questionId = :questionId)")
    fun isBookmarked(questionId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE questionId = :questionId")
    suspend fun removeBookmark(questionId: String)
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

    @Query("SELECT * FROM exam_records WHERE track LIKE :track COLLATE NOCASE ORDER BY timestamp DESC")
    fun getSessionsByTrack(track: String): Flow<List<ExamRecordEntity>>

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

    @Query("DELETE FROM exam_records WHERE id IN (:ids)")
    suspend fun deleteSessionsByIds(ids: List<String>)

    @Query("DELETE FROM exam_records WHERE track LIKE :track COLLATE NOCASE")
    suspend fun clearAllSessionsByTrack(track: String)

    @Query("DELETE FROM exam_records WHERE id LIKE 'inc_%' OR detailsSummary LIKE 'INCOMPLETE%'")
    suspend fun purgeIncompleteRecords()

    @Query("DELETE FROM exam_records")
    suspend fun clearHistory()
}
