package com.example.data.repository

import android.content.Context
import com.example.data.model.ProQuestion
import com.example.data.util.LicenseManager
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap

data class ProCompetenceMeta(
    val id: String,
    val title: String,
    val description: String,
    val partsCount: Int,
    val totalQuestions: Int,
    val partQuestionCounts: Map<Int, Int>
)

/**
 * Isolated offline repository for GMDSS Pro Tier Review Questions.
 * Strictly enforces LicenseManager.isActivated(context) at every data access point.
 * Never leaks questions if the user is unactivated.
 */
object ProQuestionRepository {

    private val questionsCache = ConcurrentHashMap<String, List<ProQuestion>>()

    val COMPETENCE_C1 = ProCompetenceMeta(
        id = "C1",
        title = "GMDSS Subsystems & Operational Procedures",
        description = "Transmit and receive information using GMDSS subsystems and equipment and fulfilling the functional requirements of GMDSS.",
        partsCount = 10,
        totalQuestions = 1911,
        partQuestionCounts = mapOf(
            1 to 192, 2 to 191, 3 to 191, 4 to 191, 5 to 191,
            6 to 191, 7 to 191, 8 to 191, 9 to 191, 10 to 191
        )
    )

    val COMPETENCE_C2 = ProCompetenceMeta(
        id = "C2",
        title = "Emergency Radio Communications & SAR Procedures",
        description = "Provide radio services in emergencies, distress alerting, search and rescue operations, and MSI broadcast.",
        partsCount = 2,
        totalQuestions = 349,
        partQuestionCounts = mapOf(
            1 to 175, 2 to 174
        )
    )

    fun getAllCompetences(): List<ProCompetenceMeta> = listOf(COMPETENCE_C1, COMPETENCE_C2)

    fun getCompetenceMeta(competence: String): ProCompetenceMeta? = when (competence.uppercase()) {
        "C1" -> COMPETENCE_C1
        "C2" -> COMPETENCE_C2
        else -> null
    }

    fun getPartsForCompetence(competence: String): List<Int> = when (competence.uppercase()) {
        "C1" -> (1..10).toList()
        "C2" -> (1..2).toList()
        else -> emptyList()
    }

    fun getExpectedQuestionCount(competence: String, part: Int): Int {
        val meta = getCompetenceMeta(competence) ?: return 0
        return meta.partQuestionCounts[part] ?: 0
    }

    /**
     * Loads questions for a specific competence and part.
     * HARD ENFORCEMENT: If LicenseManager.isActivated(context) is false, returns emptyList() immediately.
     */
    fun loadPartQuestions(context: Context, competence: String, part: Int): List<ProQuestion> {
        val normComp = competence.uppercase().trim()
        if (!LicenseManager.isActivated(context)) {
            return emptyList()
        }

        val cacheKey = "${normComp}_part$part"
        questionsCache[cacheKey]?.let { return it }

        val assetPath = "questions/pro/${normComp.lowercase()}_part$part.json"
        val questions = try {
            val stream = try {
                context.assets.open(assetPath)
            } catch (_: Exception) {
                val classLoader = ProQuestionRepository::class.java.classLoader
                classLoader?.getResourceAsStream(assetPath)
                    ?: classLoader?.getResourceAsStream("assets/$assetPath")
                    ?: java.io.File("app/src/main/assets/$assetPath").takeIf { it.exists() }?.inputStream()
                    ?: java.io.File("src/main/assets/$assetPath").takeIf { it.exists() }?.inputStream()
                    ?: java.io.File("/app/src/main/assets/$assetPath").takeIf { it.exists() }?.inputStream()
            } ?: return emptyList()

            val jsonString = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ProQuestion>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "$normComp-P$part-${i + 1}")
                val comp = obj.optString("competence", normComp)
                val partNum = obj.optInt("part", part)
                val qText = obj.optString("questionText", "")
                val optionsArray = obj.optJSONArray("options")
                val optionsList = mutableListOf<String>()
                if (optionsArray != null) {
                    for (j in 0 until optionsArray.length()) {
                        optionsList.add(optionsArray.optString(j, ""))
                    }
                }
                val ansIndex = obj.optInt("correctAnswerIndex", 0)
                val explanation = obj.optString("explanation", "")

                list.add(
                    ProQuestion(
                        id = id,
                        competence = comp,
                        part = partNum,
                        questionText = qText,
                        options = optionsList,
                        correctAnswerIndex = ansIndex,
                        explanation = explanation
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }

        if (questions.isNotEmpty()) {
            questionsCache[cacheKey] = questions
        }
        return questions
    }

    /**
     * Loads all questions across all parts for a competence.
     * HARD ENFORCEMENT: If unactivated, returns emptyList().
     */
    fun loadAllCompetenceQuestions(context: Context, competence: String): List<ProQuestion> {
        if (!LicenseManager.isActivated(context)) return emptyList()
        val parts = getPartsForCompetence(competence)
        return parts.flatMap { loadPartQuestions(context, competence, it) }
    }

    /**
     * Clears memory cache (e.g. upon license deactivation or memory trim).
     */
    fun clearCache() {
        questionsCache.clear()
    }
}
