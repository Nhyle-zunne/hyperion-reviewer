package com.nhyle.hyperionmaritime.data.studytime

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_time_segments",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["trackId"]),
        Index(value = ["startEpochMs"]),
        Index(value = ["endEpochMs"])
    ]
)
data class StudyTimeSegmentEntity(
    @PrimaryKey val segmentId: String,
    val sessionId: String,
    val trackId: String,
    val competence: String? = null,
    val part: String? = null, // GMDSS only; null otherwise
    val mode: String, // EXAM or DRILL
    val drillFormat: String? = null, // STANDARD, INTENSIVE, or null
    val startEpochMs: Long, // UTC
    val endEpochMs: Long, // UTC
    val durationMs: Long // from SystemClock.elapsedRealtime()
)
