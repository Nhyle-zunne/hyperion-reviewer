package com.example.data.repository

import com.example.data.model.Question
import com.example.data.model.Track

object MaritimeQuestionBank {

    val ALL_QUESTIONS: List<Question> by lazy {
        MaritimeQuestionBankDeckF1.getQuestions() +
        MaritimeQuestionBankDeckF2F3.getQuestions() +
        MaritimeQuestionBankGmdss.getQuestions()
    }

    fun getQuestionsByTrack(track: Track, isProUnlocked: Boolean = true): List<Question> {
        val list = ALL_QUESTIONS.filter { it.track == track }
        return if (isProUnlocked) list else list.take(5)
    }

    fun getQuestionsByCompetence(competenceId: String, track: Track? = null, isProUnlocked: Boolean = true): List<Question> {
        val list = ALL_QUESTIONS.filter { (track == null || it.track == track) && it.competenceId == competenceId }
        return if (isProUnlocked) list else list.take(5)
    }

    fun getQuestionsByFunction(functionId: String, track: Track? = null, isProUnlocked: Boolean = true): List<Question> {
        val list = ALL_QUESTIONS.filter { (track == null || it.track == track) && it.functionId == functionId }
        return if (isProUnlocked) list else list.take(5)
    }

    /**
     * Dedicated query for full mock examination.
     * If unactivated, returns an empty list to prevent unauthorized access.
     */
    fun getMockExamDataset(track: Track, isProUnlocked: Boolean): List<Question> {
        if (!isProUnlocked) return emptyList()
        return getQuestionsByTrack(track, isProUnlocked = true)
    }

    fun searchQuestions(query: String, track: Track? = null, isProUnlocked: Boolean = true): List<Question> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val list = ALL_QUESTIONS.filter { item ->
            (track == null || item.track == track) && (
                item.questionText.lowercase().contains(q) ||
                item.topic.lowercase().contains(q) ||
                item.explanation.lowercase().contains(q) ||
                item.competenceId.lowercase().contains(q) ||
                item.options.any { it.lowercase().contains(q) }
            )
        }
        return if (isProUnlocked) list else list.take(5)
    }
}
