package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey val questionId: String,
    val consecutiveCorrect: Int = 0, // Cleared once reaches 2
    val timesFailed: Int = 1,
    val lastFailedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val questionId: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey val id: String = "current_session",
    val track: String,
    val mode: String,
    val functionId: String?,
    val competenceId: String?,
    val currentQuestionIndex: Int,
    val timeRemainingSeconds: Int,
    val questionIds: String, // comma-separated question IDs
    val answersJson: String, // JSON string for answer choices & flags
    val totalQuestions: Int,
    val answeredCount: Int,
    val completedCompetences: String = "",
    val updatedTimestamp: Long = System.currentTimeMillis()
)

val ActiveSessionEntity.answeredQuestions: Int
    get() = answeredCount

val ActiveSessionEntity.isCompleted: Boolean
    get() = false

@Entity(tableName = "exam_records")
data class ExamRecordEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val track: String,
    val mode: String,
    val rawScore: Int,
    val maxScore: Int,
    val percentage: Double,
    val isPassed: Boolean,
    val totalQuestions: Int,
    val correctCount: Int,
    val detailsSummary: String,
    val questionIds: String = "",
    val answersJson: String = "",
    val competenceBreakdownJson: String = ""
)

val ExamRecordEntity.score: Int
    get() = correctCount

val ExamRecordEntity.dateFormatted: String
    get() {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
