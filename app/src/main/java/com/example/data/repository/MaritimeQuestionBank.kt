package com.example.data.repository

import com.example.data.model.Question
import com.example.data.model.Track

object MaritimeQuestionBank {

    val ALL_QUESTIONS: List<Question> by lazy {
        val lists = (MaritimeQuestionBankDeckF1.getQuestions() +
                MaritimeQuestionBankDeckF2F3.getQuestions() +
                MaritimeQuestionBankGmdss.getQuestions())
        lists.filter { q ->
            val text = q.questionText.trim()
            val isHeader = text.equals("Question", ignoreCase = true) || text.isBlank()
            val isDummyOptions = q.options.size == 4 &&
                    q.options[0].trim() == "A" &&
                    q.options[1].trim() == "B" &&
                    q.options[2].trim() == "C" &&
                    q.options[3].trim() == "D"
            !isHeader && !isDummyOptions
        }
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
     * Enforces the official MARINA permit distribution (Deck: exactly 200 items, GMDSS: 100 items).
     * If unactivated, returns an empty list to prevent unauthorized access.
     */
    fun getMockExamDataset(track: Track, isProUnlocked: Boolean): List<Question> {
        if (!isProUnlocked) return emptyList()
        val allTrackQuestions = getQuestionsByTrack(track, isProUnlocked = true)
        val distribution = if (track == Track.DECK_OIC_NW) {
            MaritimeCompetenceRegistry.OFFICIAL_DECK_PERMIT_DISTRIBUTION
        } else {
            MaritimeCompetenceRegistry.OFFICIAL_GMDSS_PERMIT_DISTRIBUTION
        }

        val sampled = mutableListOf<Question>()
        for ((compId, quota) in distribution) {
            val compQuestions = allTrackQuestions.filter { it.competenceId == compId }.shuffled()
            sampled.addAll(compQuestions.take(quota))
        }
        return sampled
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
