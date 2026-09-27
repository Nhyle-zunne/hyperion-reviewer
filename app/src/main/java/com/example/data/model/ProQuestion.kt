package com.example.data.model

/**
 * Dedicated schema for Pro / Core Tier GMDSS Competency review questions.
 * Strictly isolated from Free Tier modules.
 */
data class ProQuestion(
    val id: String,
    val competence: String, // "C1" or "C2"
    val part: Int,          // 1 to 10 for C1; 1 to 2 for C2
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)
