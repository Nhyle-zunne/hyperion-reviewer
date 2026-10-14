package com.nhyle.hyperionmaritime.data.studytime

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyTimeSegmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: StudyTimeSegmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<StudyTimeSegmentEntity>)

    @Query("SELECT * FROM study_time_segments WHERE sessionId = :sessionId ORDER BY startEpochMs ASC")
    fun getSegmentsForSession(sessionId: String): Flow<List<StudyTimeSegmentEntity>>

    @Query("SELECT * FROM study_time_segments ORDER BY startEpochMs ASC")
    fun getAllSegments(): Flow<List<StudyTimeSegmentEntity>>

    @Query("SELECT * FROM study_time_segments ORDER BY startEpochMs ASC")
    suspend fun getAllSegmentsOnce(): List<StudyTimeSegmentEntity>

    @Query("SELECT * FROM study_time_segments WHERE startEpochMs >= :startEpoch AND endEpochMs <= :endEpoch ORDER BY startEpochMs ASC")
    fun getSegmentsBetweenEpochs(startEpoch: Long, endEpoch: Long): Flow<List<StudyTimeSegmentEntity>>

    @Query("SELECT * FROM study_time_segments WHERE trackId = :trackId ORDER BY startEpochMs ASC")
    fun getSegmentsByTrack(trackId: String): Flow<List<StudyTimeSegmentEntity>>

    @Query("SELECT SUM(durationMs) FROM study_time_segments WHERE sessionId = :sessionId")
    suspend fun getTotalDurationForSessionOnce(sessionId: String): Long?

    @Query("SELECT SUM(durationMs) FROM study_time_segments WHERE sessionId = :sessionId")
    fun observeTotalDurationForSession(sessionId: String): Flow<Long?>

    @Query("DELETE FROM study_time_segments WHERE sessionId = :sessionId")
    suspend fun deleteSegmentsForSession(sessionId: String)

    @Query("DELETE FROM study_time_segments")
    suspend fun clearAllSegments()
}
