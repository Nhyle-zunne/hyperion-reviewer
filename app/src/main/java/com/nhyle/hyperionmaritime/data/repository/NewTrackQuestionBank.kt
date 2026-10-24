package com.nhyle.hyperionmaritime.data.repository

import android.util.Log
import com.nhyle.hyperionmaritime.data.model.CompetenceInfo
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track

object NewTrackQuestionBank {
    private const val TAG = "NewTrackQuestionBank"

    private val cachedQuestions: List<Question> by lazy {
        loadAllQuestions()
    }

    fun getQuestions(): List<Question> = cachedQuestions

    private fun loadAllQuestions(): List<Question> {
        val result = mutableListOf<Question>()
        val tracks = listOf(
            Triple(Track.ENGINE_OIC_EW, "engine_ew", "ew"),
            Triple(Track.DECK_MGT, "deck_mgt", "dm"),
            Triple(Track.ENGINE_MGT, "engine_mgt", "em")
        )

        for ((track, folder, prefix) in tracks) {
            val competences = MaritimeCompetenceRegistry.getCompetencesForTrack(track)
            val idPrefix = prefix.uppercase() // EW, DM, EM
            for (comp in competences) {
                val compNum = comp.competenceId.removePrefix("C")
                val assetPath = "questions/$folder/${prefix}_c$compNum.csv"
                val questionsForComp = loadCompetenceCsv(
                    assetPath = assetPath,
                    competence = comp,
                    idPrefix = idPrefix
                )
                result.addAll(questionsForComp)
            }
        }
        return result
    }

    private fun loadCompetenceCsv(
        assetPath: String,
        competence: CompetenceInfo,
        idPrefix: String
    ): List<Question> {
        return try {
            val stream = MaritimeQuestionBankDeckF2F3.openAssetStream(assetPath) ?: return emptyList()
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                .replace("&#39;", "'")
                .replace("/'", "'")
                .replace("&amp;", "&")
                .replace("\uFFFD", "'")

            val rows = parseCsvText(text)
            if (rows.isEmpty()) return emptyList()

            // Header row is the one whose first cell equals "No." (ignore case)
            var headerIndex = -1
            for (idx in rows.indices) {
                val r = rows[idx]
                if (r.isNotEmpty() && r[0].trim().equals("No.", ignoreCase = true)) {
                    headerIndex = idx
                    break
                }
            }
            if (headerIndex == -1) {
                Log.w(TAG, "Header row 'No.' not found in $assetPath")
                return emptyList()
            }

            val seenNumbers = mutableSetOf<Int>()
            val questions = mutableListOf<Question>()

            for (rowIdx in (headerIndex + 1) until rows.size) {
                val row = rows[rowIdx]
                if (row.isEmpty() || row.all { it.isBlank() }) continue

                val noStr = row.getOrNull(0)?.trim() ?: ""
                val no = noStr.toIntOrNull()
                if (no == null) {
                    Log.w(TAG, "Invalid row in $assetPath at row $rowIdx: 'No.' is not an integer ('$noStr')")
                    continue
                }

                if (no in seenNumbers) {
                    Log.w(TAG, "Duplicate row in $assetPath at row $rowIdx: 'No.' $no repeated")
                    continue
                }

                val qText = row.getOrNull(1)?.trim() ?: ""
                if (qText.isEmpty()) {
                    Log.w(TAG, "Invalid row in $assetPath at row $rowIdx: question text is empty")
                    continue
                }

                val optA = row.getOrNull(2)?.trim() ?: ""
                val optB = row.getOrNull(3)?.trim() ?: ""
                val optC = row.getOrNull(4)?.trim() ?: ""
                val optD = row.getOrNull(5)?.trim() ?: ""
                val options = listOf(optA, optB, optC, optD)

                val nonEmptyCount = options.count { it.isNotEmpty() }
                if (nonEmptyCount < 2) {
                    Log.w(TAG, "Invalid row in $assetPath at row $rowIdx: fewer than 2 non-empty options")
                    continue
                }

                val ansStr = row.getOrNull(6)?.trim() ?: ""
                val correctIndex = when (ansStr.uppercase()) {
                    "A", "1" -> 0
                    "B", "2" -> 1
                    "C", "3" -> 2
                    "D", "4" -> 3
                    else -> -1
                }

                if (correctIndex !in 0..3 || options[correctIndex].isEmpty()) {
                    Log.w(TAG, "Invalid row in $assetPath at row $rowIdx: correct answer '$ansStr' does not point to a non-empty option")
                    continue
                }

                seenNumbers.add(no)

                val diff = classifyQuestionDifficulty(qText, options, no)

                val qId = String.format("%s-%s-%04d", idPrefix, competence.competenceId, no)
                questions.add(
                    Question(
                        id = qId,
                        track = competence.track,
                        functionId = competence.functionId,
                        competenceId = competence.competenceId,
                        topic = competence.title,
                        questionText = qText,
                        options = options,
                        correctAnswerIndex = correctIndex,
                        explanation = "",
                        difficulty = diff
                    )
                )
            }
            questions
        } catch (e: Exception) {
            Log.w(TAG, "Error loading CSV from $assetPath", e)
            emptyList()
        }
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
                } else if (c == '\n' || c == '\r') {
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                    if (currentRow.any { it.isNotEmpty() }) {
                        result.add(currentRow.toList())
                    }
                    currentRow.clear()
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') {
                        i++
                    }
                } else {
                    currentField.append(c)
                }
            }
            i++
        }
        if (currentRow.isNotEmpty() || currentField.isNotEmpty()) {
            currentRow.add(currentField.toString())
            if (currentRow.any { it.isNotEmpty() }) {
                result.add(currentRow.toList())
            }
        }
        return result
    }

    private fun classifyQuestionDifficulty(qText: String, options: List<String>, fallbackNo: Int): Difficulty {
        val lower = qText.lowercase()
        val totalOptionLen = options.sumOf { it.length }

        // 1. Calculations, formulas, quantitative navigation, and stability
        val isCalculationOrFormula = lower.contains("calculate") ||
                lower.contains("formula") ||
                lower.contains("degrees") ||
                lower.contains("knots") ||
                lower.contains("bearing") ||
                lower.contains("draft") ||
                lower.contains("displacement") ||
                lower.contains("stability") ||
                lower.contains("metacentric") ||
                Regex("""\b\d+(\.\d+)?\s*(m|ft|rpm|nm|kts|deg|°)\b""").containsMatchIn(lower)

        // 2. High-level analysis / master decision making
        val isEvaluation = lower.contains("immediate action") ||
                lower.contains("in accordance with solas") ||
                lower.contains("master's responsibility") ||
                lower.contains("salvage") ||
                lower.contains("severe damage")

        if (isCalculationOrFormula || isEvaluation || (qText.length > 220 && totalOptionLen > 180)) {
            return Difficulty.DIFFICULT
        }

        // 3. Situational procedures and rules application
        val isApplication = lower.contains("action to be taken") ||
                lower.contains("what should you do") ||
                lower.contains("in the event of") ||
                lower.contains("colregs rule") ||
                lower.contains("procedure") ||
                lower.contains("precaution") ||
                lower.contains("during cargo") ||
                lower.contains("prior to")

        if (isApplication || qText.length > 130 || totalOptionLen > 120) {
            return Difficulty.MODERATE
        }

        // 4. Default to Easy for definitions, factual recall, and terminology
        return Difficulty.EASY
    }
}
