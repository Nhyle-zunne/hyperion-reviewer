package com.nhyle.hyperionmaritime.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["track", "competence"]),
        Index(value = ["track"])
    ]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val track: String,
    val competence: String,
    val functionId: String = "",
    val topic: String = "",
    val questionText: String = "",
    val optionsJson: String = "",
    val correctAnswerIndex: Int = 0,
    val explanation: String = "",
    val difficulty: String = "MODERATE"
)
