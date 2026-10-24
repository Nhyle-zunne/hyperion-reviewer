package com.nhyle.hyperionmaritime.data.repository

import android.content.Context
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.model.classifyQuestionDifficulty

import kotlin.math.roundToInt

object MaritimeQuestionBank {

    var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        ProQuestionRepository.init(context)
    }

    @Volatile
    private var _cachedQuestions: List<Question>? = null

    val isLoaded: Boolean
        get() = _cachedQuestions != null

    val ALL_QUESTIONS: List<Question>
        get() {
            _cachedQuestions?.let { return it }
            return synchronized(this) {
                _cachedQuestions?.let { return it }
                val lists = (MaritimeQuestionBankDeckF1.getQuestions() +
                        MaritimeQuestionBankDeckF2F3.getQuestions() +
                        MaritimeQuestionBankGmdss.getQuestions(appContext) +
                        NewTrackQuestionBank.getQuestions())
                val result = lists.filter { q ->
                    val text = q.questionText.trim()
                    val isHeader = text.equals("Question", ignoreCase = true) || text.isBlank()
                    val isDummyOptions = q.options.size == 4 &&
                            q.options[0].trim() == "A" &&
                            q.options[1].trim() == "B" &&
                            q.options[2].trim() == "C" &&
                            q.options[3].trim() == "D"
                    !isHeader && !isDummyOptions
                }.map { q ->
                    q.copy(difficulty = classifyQuestionDifficulty(q.questionText, q.options))
                }
                _cachedQuestions = result
                result
            }
        }

    fun getQuestionsByTrack(track: Track, isProUnlocked: Boolean = true): List<Question> {
        val list = ALL_QUESTIONS.filter { it.track == track }
        return if (isProUnlocked) list else list.take(5)
    }

    fun getQuestionsByCompetence(competenceId: String, track: Track? = null, isProUnlocked: Boolean = true): List<Question> {
        val list = ALL_QUESTIONS.filter { (track == null || it.track == track) && it.competenceId == competenceId }
        if (isProUnlocked) return list
        
        // Unlicensed Quota
        val resolvedTrack = track ?: list.firstOrNull()?.track
        return if (resolvedTrack == Track.ENGINE_OIC_EW || resolvedTrack == Track.DECK_MGT || resolvedTrack == Track.ENGINE_MGT) {
            val quota = (list.size * 0.40).roundToInt().coerceAtLeast(1)
            list.take(quota)
        } else {
            list.take(5)
        }
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
    fun getMockExamDataset(
        track: Track,
        isProUnlocked: Boolean,
        selectedFunctions: Set<String>? = null
    ): List<Question> {
        if (!isProUnlocked) return emptyList()
        val allTrackQuestions = getQuestionsByTrack(track, isProUnlocked = true)
        val competences = MaritimeCompetenceRegistry.getCompetencesForTrack(track)
        val allowedCompIds = if (!selectedFunctions.isNullOrEmpty()) {
            competences.filter { it.functionId in selectedFunctions }.map { it.competenceId }.toSet()
        } else {
            null
        }

        val distribution = MaritimeCompetenceRegistry.getPermitDistribution(track)
        val filteredDistribution = if (allowedCompIds != null) {
            distribution.filterKeys { it in allowedCompIds }
        } else {
            distribution
        }

        val sampled = mutableListOf<Question>()
        for ((compId, quota) in filteredDistribution) {
            val compQuestions = allTrackQuestions.filter { it.competenceId == compId }.shuffled()
            sampled.addAll(compQuestions.take(quota))
        }
        return sampled
    }

    /**
     * One-time static randomized pass for CRP pools (2,065 total items across 12 Deck competencies).
     * Applies a deterministic one-time shuffle upon initial access so question order differs from raw database
     * sequence while maintaining sequential consistency during free practice drills.
     */
    private val MARINA_CRP_POOLS_BY_COMPETENCE: Map<String, List<Question>> by lazy {
        val allDeckQuestions = ALL_QUESTIONS.filter { it.track == Track.DECK_OIC_NW }
        val rng = java.util.Random(2065L)
        MaritimeCompetenceRegistry.CRP_DECK_DISTRIBUTION.mapValues { (compId, quota) ->
            val compQuestions = allDeckQuestions.filter { it.competenceId == compId }
            if (compId == "C17" || compId == "C13" || compId == "C9" || compId == "C10" || compId == "C7") {
                // Ensure Pietro's exact base questions (first quota items: 97 for C17, 122 for C13, 176 for C9, 109 for C10, 196 for C7) are accessible for CRP practice
                compQuestions.take(quota)
            } else {
                compQuestions.shuffled(rng).take(quota)
            }
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

    /**
     * Dedicated query for GMDSS Review Questions.
     * C1: 1,911 items across 10 parts
     * C2: 349 items across 2 parts
     * Total: 2,260 items
     */
    @Synchronized
    fun getMarinaCrpGmdssQuestions(
        context: Context,
        competenceId: String? = null,
        selectedPart: Int? = null,
        isProUnlocked: Boolean = true
    ): List<Question> {
        val rawC1 = ProQuestionRepository.loadAllRawCompetenceQuestions(context, "C1").map { it.toQuestion() }
        val allC1 = if (isProUnlocked) rawC1 else rawC1.take(1000)

        val rawC2 = ProQuestionRepository.loadAllRawCompetenceQuestions(context, "C2").map { it.toQuestion() }
        val allC2 = if (isProUnlocked) rawC2 else rawC2.take(150)

        return when (competenceId) {
            "C1" -> if (selectedPart != null) {
                val partQuestions = ProQuestionRepository.loadRawPartQuestions(context, "C1", selectedPart).map { it.toQuestion() }
                if (isProUnlocked) partQuestions else {
                    val allowedIds = allC1.map { it.id }.toSet()
                    partQuestions.filter { allowedIds.contains(it.id) }
                }
            } else {
                allC1
            }
            "C2" -> if (selectedPart != null) {
                val partQuestions = ProQuestionRepository.loadRawPartQuestions(context, "C2", selectedPart).map { it.toQuestion() }
                if (isProUnlocked) partQuestions else {
                    val allowedIds = allC2.map { it.id }.toSet()
                    partQuestions.filter { allowedIds.contains(it.id) }
                }
            } else {
                allC2
            }
            else -> allC1 + allC2
        }
    }

    fun getNewTrackStandardReviewQuestions(track: Track, isProUnlocked: Boolean): List<Question> {
        val allTrackQuestions = ALL_QUESTIONS.filter { it.track == track }
        if (isProUnlocked) return allTrackQuestions
        
        val result = mutableListOf<Question>()
        val competences = allTrackQuestions.groupBy { it.competenceId }
        for ((_, questions) in competences) {
            val quota = (questions.size * 0.40).roundToInt().coerceAtLeast(1)
            result.addAll(questions.take(quota))
        }
        return result
    }

    fun searchQuestions(query: String, track: Track? = null, isProUnlocked: Boolean = true, competenceId: String? = null): List<Question> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val list = ALL_QUESTIONS.filter { item ->
            (track == null || item.track == track) &&
            (competenceId == null || competenceId == "ALL" || item.competenceId.equals(competenceId, ignoreCase = true)) &&
            (
                item.questionText.lowercase().contains(q) ||
                item.options.any { it.lowercase().contains(q) } ||
                item.explanation.lowercase().contains(q) ||
                item.topic.lowercase().contains(q) ||
                item.competenceId.lowercase().contains(q) ||
                item.id.lowercase().contains(q)
            )
        }
        return if (isProUnlocked) list else list.take(5)
    }
}
