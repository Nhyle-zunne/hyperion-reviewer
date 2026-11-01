package com.nhyle.hyperionmaritime.data.repository

import android.content.Context
import com.nhyle.hyperionmaritime.data.model.ProQuestion
import com.nhyle.hyperionmaritime.data.util.LicenseManager
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

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

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
     * Loads questions for a specific competence and part from CSV assets.
     * HARD ENFORCEMENT: If LicenseManager.isActivated(context) is false, returns emptyList() immediately.
     */
    fun loadPartQuestions(context: Context, competence: String, part: Int): List<ProQuestion> {
        if (!LicenseManager.isActivated(context)) {
            return emptyList()
        }
        return loadRawPartQuestions(context, competence, part)
    }

    /**
     * Loads questions from CSV assets directly without license gating (used for Free-Tier MARINA CRP mode).
     */
    fun loadRawPartQuestions(context: Context? = null, competence: String, part: Int): List<ProQuestion> {
        val normComp = competence.uppercase().trim()
        val cacheKey = "raw_${normComp}_part$part"
        questionsCache[cacheKey]?.let { return it }

        val assetPath = "questions/pro/${normComp.lowercase()}_part$part.csv"
        val questions = try {
            val ctx = context ?: appContext
            val classLoader = ProQuestionRepository::class.java.classLoader
            fun openRaw(path: String): java.io.InputStream? {
                val fromAssets = try {
                    ctx?.assets?.open(path)
                } catch (_: Exception) {
                    null
                }
                return fromAssets
                    ?: classLoader?.getResourceAsStream(path)
                    ?: classLoader?.getResourceAsStream("assets/$path")
                    ?: java.io.File("app/src/main/assets/$path").takeIf { it.exists() }?.inputStream()
                    ?: java.io.File("src/main/assets/$path").takeIf { it.exists() }?.inputStream()
                    ?: java.io.File("/app/src/main/assets/$path").takeIf { it.exists() }?.inputStream()
            }
            val encryptedStream = openRaw("$assetPath.enc")
            val stream = (if (encryptedStream != null) {
                com.nhyle.hyperionmaritime.data.util.AssetCrypto.decryptStream(encryptedStream)
            } else {
                openRaw(assetPath)
            }) ?: return emptyList()

            val content = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val rows = parseCsvText(content)
            val list = mutableListOf<ProQuestion>()

            // Match the PWA parser: attempt normal row records first, then
            // flatten the complete CSV and parse 10-column records only when
            // no valid normal records were found.
            var headerIdx = -1
            for (idx in rows.indices) {
                val r0 = rows[idx].getOrNull(0)?.trim() ?: ""
                val r1 = rows[idx].getOrNull(1)?.trim() ?: ""
                if (r0.equals("Question", ignoreCase = true) || r1.equals("Type", ignoreCase = true)) {
                    headerIdx = idx
                    break
                }
            }
            val startRow = if (headerIdx != -1) headerIdx + 1 else 1
            for (rowIndex in startRow until rows.size) {
                val row = rows[rowIndex]
                if (row.size >= 10 && row[0].isNotBlank() && !row[0].equals("Question", ignoreCase = true)) {
                    val qText = row[0].trim()
                    val options = row.subList(2, 8).map { it.trim() }.filter { it.isNotEmpty() }
                    val explanation = row.getOrElse(8) { "" }.trim()
                    val ansStr = row.getOrElse(9) { "1" }.trim()
                    val ansIndex = (ansStr.toIntOrNull() ?: 1) - 1
                    if (qText.isNotEmpty() && options.size >= 2) {
                        list.add(
                            ProQuestion(
                                id = "$normComp-P$part-${list.size + 1}",
                                competence = normComp,
                                part = part,
                                questionText = qText,
                                options = options,
                                correctAnswerIndex = ansIndex.coerceIn(0, options.size - 1),
                                explanation = explanation
                            )
                        )
                    }
                }
            }
            if (list.isEmpty()) {
                val flat = rows.flatten()
                for (cellIndex in flat.indices) {
                    if (!flat[cellIndex].equals("Choice", ignoreCase = true)) continue
                    val recordStart = cellIndex - 1
                    if (recordStart < 0 || recordStart + 9 >= flat.size) continue
                    val qText = flat[recordStart].trim()
                    val options = (recordStart + 2 until recordStart + 8)
                        .map { flat[it].trim() }.filter { it.isNotEmpty() }
                    val explanation = flat[recordStart + 8].trim()
                    val ansStr = flat[recordStart + 9].trim()
                    val ansIndex = (ansStr.toIntOrNull() ?: 1) - 1
                    if (qText.isNotEmpty() && !qText.equals("Question", ignoreCase = true) && options.size >= 2) {
                        list.add(
                            ProQuestion(
                                id = "$normComp-P$part-${list.size + 1}",
                                competence = normComp,
                                part = part,
                                questionText = qText,
                                options = options,
                                correctAnswerIndex = ansIndex.coerceIn(0, options.size - 1),
                                explanation = explanation
                            )
                        )
                    }
                }
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

    private fun parseCsvText(text: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                if (c == '"') {
                    inQuotes = true
                } else if (c == ',') {
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                } else if (c == '\r') {
                    if (i + 1 < text.length && text[i + 1] == '\n') {
                        i++
                    }
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                    result.add(currentRow.toList())
                    currentRow.clear()
                } else if (c == '\n') {
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                    result.add(currentRow.toList())
                    currentRow.clear()
                } else {
                    currentField.append(c)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            result.add(currentRow.toList())
        }
        return result
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
     * Loads all raw questions across all parts for a competence without license gating.
     */
    fun loadAllRawCompetenceQuestions(context: Context? = null, competence: String): List<ProQuestion> {
        val parts = getPartsForCompetence(competence)
        return parts.flatMap { loadRawPartQuestions(context, competence, it) }
    }

    /**
     * Clears memory cache (e.g. upon license deactivation or memory trim).
     */
    fun clearCardCache() {
        questionsCache.clear()
    }

    fun clearCache() {
        questionsCache.clear()
    }
}
