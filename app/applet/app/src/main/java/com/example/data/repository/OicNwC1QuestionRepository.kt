package com.example.data.repository

import android.content.Context
import com.example.data.model.Difficulty
import com.example.data.model.Question
import com.example.data.model.Track
import java.io.File
import java.io.InputStream

object OicNwC1QuestionRepository {

    private var cachedQuestions: List<Question>? = null

    fun getQuestions(context: Context? = null): List<Question> {
        cachedQuestions?.let { return it }
        val questions = loadQuestionsFromCsv(context)
        if (questions.isNotEmpty()) {
            cachedQuestions = questions
        }
        return questions
    }

    fun clearCache() {
        cachedQuestions = null
    }

    private fun loadQuestionsFromCsv(context: Context?): List<Question> {
        val stream = getCsvInputStream(context) ?: return emptyList()
        val text = try {
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            return emptyList()
        }
        
        val rows = parseCsvText(text)
        if (rows.isEmpty()) return emptyList()

        val result = mutableListOf<Question>()

        for (row in rows) {
            if (row.isEmpty()) continue
            val firstCol = row[0].trim()
            if (firstCol.equals("No.", ignoreCase = true) || firstCol.equals("No", ignoreCase = true)) {
                continue
            }

            val qNum = firstCol.toIntOrNull() ?: continue
            if (row.size < 7) continue

            val qText = row[1].trim()
            val optA = row[2].trim()
            val optB = row[3].trim()
            val optC = row[4].trim()
            val optD = row[5].trim()
            val ansRaw = row[6].trim().uppercase()

            if (qText.isEmpty() || optA.isEmpty()) continue

            val ansIndex = when {
                ansRaw.startsWith("A") -> 0
                ansRaw.startsWith("B") -> 1
                ansRaw.startsWith("C") -> 2
                ansRaw.startsWith("D") -> 3
                else -> (ansRaw.toIntOrNull() ?: 1) - 1
            }.coerceIn(0, 3)

            val topic = deriveTopic(qText)
            val difficulty = when (qNum % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }

            val question = Question(
                id = "OIC-NW-C1-$qNum",
                track = Track.DECK_OIC_NW,
                functionId = "F1",
                competenceId = "C1",
                topic = topic,
                questionText = qText,
                options = listOf(optA, optB, optC, optD),
                correctAnswerIndex = ansIndex,
                explanation = "STCW Competence 1: Plan and conduct a passage and determine position (Official MARINA Reviewer).",
                difficulty = difficulty
            )
            result.add(question)
        }

        return result
    }

    private fun getCsvInputStream(context: Context?): InputStream? {
        val path = "questions/deck/oic_nw_c1.csv"
        if (context != null) {
            try {
                return context.assets.open(path)
            } catch (_: Exception) {}
        }
        val classLoader = OicNwC1QuestionRepository::class.java.classLoader
        return classLoader?.getResourceAsStream("assets/$path")
            ?: classLoader?.getResourceAsStream(path)
            ?: File("app/src/main/assets/$path").takeIf { it.exists() }?.inputStream()
            ?: File("src/main/assets/$path").takeIf { it.exists() }?.inputStream()
            ?: File("/app/applet/app/src/main/assets/$path").takeIf { it.exists() }?.inputStream()
    }

    private fun deriveTopic(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("radar") || t.contains("arpa") || t.contains("ebl") || t.contains("vrm") -> "Radar & ARPA"
            t.contains("compass") || t.contains("gyro") || t.contains("variation") || t.contains("deviation") || t.contains("magnetic") -> "Compass & Steering"
            t.contains("tide") || t.contains("current") || t.contains("drift") || t.contains("depth") || t.contains("sounder") -> "Tides & Tidal Stream"
            t.contains("cloud") || t.contains("fog") || t.contains("barometer") || t.contains("weather") || t.contains("front") || t.contains("wind") || t.contains("hurricane") || t.contains("storm") || t.contains("humidity") -> "Meteorology & Weather"
            t.contains("celestial") || t.contains("star") || t.contains("sun") || t.contains("moon") || t.contains("polaris") || t.contains("sextant") || t.contains("zenith") || t.contains("altitude") -> "Celestial Navigation"
            t.contains("gps") || t.contains("dgps") || t.contains("satellite") || t.contains("ais") -> "Electronic Navigation & GNSS"
            t.contains("buoy") || t.contains("mark") || t.contains("iala") || t.contains("light") -> "Buoyage & Aids to Navigation"
            t.contains("chart") || t.contains("mercator") || t.contains("sailing directions") || t.contains("pilot") -> "Charts & Publications"
            else -> "Terrestrial Navigation & Plotting"
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
}
