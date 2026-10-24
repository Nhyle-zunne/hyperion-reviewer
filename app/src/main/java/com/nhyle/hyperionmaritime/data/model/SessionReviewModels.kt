package com.nhyle.hyperionmaritime.data.model

data class SessionReviewItem(
    val question: Question,
    val selectedIndex: Int?,
    val isCorrect: Boolean
)

data class SessionReviewData(
    val title: String,
    val scoreText: String,
    val items: List<SessionReviewItem>
)