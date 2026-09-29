package com.example.data.repository

import android.content.Context
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

    /**
     * One-time static randomized pass for Pietro's CRP pools (2,065 total items across 12 Deck competencies).
     * Applies a deterministic one-time shuffle upon initial access so question order differs from raw database
     * sequence while maintaining sequential consistency during free practice drills.
     */
    private val MARINA_CRP_POOLS_BY_COMPETENCE: Map<String, List<Question>> by lazy {
        val allDeckQuestions = ALL_QUESTIONS.filter { it.track == Track.DECK_OIC_NW }
        val rng = java.util.Random(2065L)
        MaritimeCompetenceRegistry.PIETRO_CRP_DECK_DISTRIBUTION.mapValues { (compId, quota) ->
            val compQuestions = allDeckQuestions.filter { it.competenceId == compId }.shuffled(rng)
            compQuestions.take(quota)
        }
    }

    private val MARINA_CRP_ALL_QUESTIONS: List<Question> by lazy {
        val list = mutableListOf<Question>()
        for ((_, pool) in MARINA_CRP_POOLS_BY_COMPETENCE) {
            list.addAll(pool)
        }
        list
    }

    /**
     * Dedicated query for Free-Tier MARINA CRP-Based Practice.
     * Enforces Pietro's exact restricted question distribution across the 12 competencies (2,065 total items)
     * with a one-time static randomization pass so questions appear scrambled while maintaining sequential order.
     * Bypasses Pro license requirements.
     */
    fun getMarinaCrpQuestions(competenceId: String? = null): List<Question> {
        return if (competenceId != null) {
            MARINA_CRP_POOLS_BY_COMPETENCE[competenceId] ?: emptyList()
        } else {
            MARINA_CRP_ALL_QUESTIONS
        }
    }

    // Cache for GMDSS Free-Tier MARINA CRP Pools (1,200 items: C1 Part 1 [500], C1 Part 2 [500], C2 [200])
    private var gmdssCrpC1Part1: List<Question>? = null
    private var gmdssCrpC1Part2: List<Question>? = null
    private var gmdssCrpC2Part1: List<Question>? = null
    private var gmdssCrpAll: List<Question>? = null

    /**
     * Dedicated query for Free-Tier MARINA CRP-Based Practice for GMDSS Pro.
     * Enforces exact restricted distribution (C1 Part 1: 500 items, C1 Part 2: 500 items, C2: 200 items; Total: 1,200 items)
     * with a one-time static deterministic shuffle so question presentation order differs from default database view.
     * Bypasses Pro license requirements.
     */
    @Synchronized
    fun getMarinaCrpGmdssQuestions(
        context: Context,
        competenceId: String? = null,
        selectedPart: Int? = null
    ): List<Question> {
        if (gmdssCrpAll == null) {
            val allC1 = ProQuestionRepository.loadAllRawCompetenceQuestions(context, "C1").map { it.toQuestion() }
            val allC2 = ProQuestionRepository.loadAllRawCompetenceQuestions(context, "C2").map { it.toQuestion() }

            val fallbackC1 = ALL_QUESTIONS.filter { it.track == Track.GMDSS_GOC && it.competenceId == "C1" }
            val fallbackC2 = ALL_QUESTIONS.filter { it.track == Track.GMDSS_GOC && it.competenceId == "C2" }

            val c1Merged = if (allC1.size >= 1000) allC1 else (allC1 + fallbackC1).distinctBy { it.id }
            val c2Merged = if (allC2.size >= 200) allC2 else (allC2 + fallbackC2).distinctBy { it.id }

            val p1Raw = c1Merged.take(500)
            val p2Raw = if (c1Merged.size >= 1000) c1Merged.subList(500, 1000) else c1Merged.drop(500).take(500)
            val c2Raw = c2Merged.take(200)

            val rng1 = java.util.Random(1001L)
            val rng2 = java.util.Random(1002L)
            val rng3 = java.util.Random(2001L)

            gmdssCrpC1Part1 = p1Raw.shuffled(rng1)
            gmdssCrpC1Part2 = p2Raw.shuffled(rng2)
            gmdssCrpC2Part1 = c2Raw.shuffled(rng3)
            gmdssCrpAll = gmdssCrpC1Part1!! + gmdssCrpC1Part2!! + gmdssCrpC2Part1!!
        }

        return when (competenceId) {
            "C1" -> when (selectedPart) {
                1 -> gmdssCrpC1Part1 ?: emptyList()
                2 -> gmdssCrpC1Part2 ?: emptyList()
                else -> (gmdssCrpC1Part1.orEmpty() + gmdssCrpC1Part2.orEmpty())
            }
            "C2" -> gmdssCrpC2Part1 ?: emptyList()
            null -> gmdssCrpAll ?: emptyList()
            else -> gmdssCrpAll ?: emptyList()
        }
    }

    fun searchQuestions(query: String, track: Track? = null, isProUnlocked: Boolean = true): List<Question> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val list = ALL_QUESTIONS.filter { item ->
            (track == null || item.track == track) && (
                item.questionText.lowercase().contains(q) ||
                item.options.any { it.lowercase().contains(q) } ||
                item.explanation.lowercase().contains(q) ||
                item.topic.lowercase().contains(q) ||
                item.competenceId.lowercase().contains(q)
            )
        }
        return if (isProUnlocked) list else list.take(5)
    }
}
