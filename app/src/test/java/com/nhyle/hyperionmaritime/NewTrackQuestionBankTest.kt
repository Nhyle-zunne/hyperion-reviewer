package com.nhyle.hyperionmaritime

import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.NewTrackQuestionBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NewTrackQuestionBankTest {

    @Test
    fun testNewTrackQuestionLoading() {
        val newQuestions = NewTrackQuestionBank.getQuestions()

        val allQuestions = MaritimeQuestionBank.ALL_QUESTIONS
        val ewQuestions = allQuestions.filter { it.track == Track.ENGINE_OIC_EW }
        val dmQuestions = allQuestions.filter { it.track == Track.DECK_MGT }
        val emQuestions = allQuestions.filter { it.track == Track.ENGINE_MGT }

        // Dynamic CSV verification for ENGINE_OIC_EW, DECK_MGT, and ENGINE_MGT
        val tracksToVerify = listOf(
            Triple(Track.ENGINE_OIC_EW, "engine_ew", "ew"),
            Triple(Track.DECK_MGT, "deck_mgt", "dm"),
            Triple(Track.ENGINE_MGT, "engine_mgt", "em")
        )

        for ((track, folder, prefix) in tracksToVerify) {
            val trackQuestions = newQuestions.filter { it.track == track }
            val questionsByComp = trackQuestions.groupBy { it.competenceId }
            val competences = MaritimeCompetenceRegistry.getCompetencesForTrack(track)

            for (comp in competences) {
                val compNum = comp.competenceId.removePrefix("C")
                val relativePath = "questions/$folder/${prefix}_c$compNum.csv.enc"
                val csvFile = File("app/src/main/assets/$relativePath").let {
                    if (it.exists()) it else File("src/main/assets/$relativePath")
                }

                assertTrue("CSV file $relativePath must exist", csvFile.exists())
                val validQuestionCount = countValidQuestionRows(csvFile)
                val loadedCount = questionsByComp[comp.competenceId]?.size ?: 0

                assertTrue(
                    "Competence ${comp.competenceId} in ${track.name} must contain more than one valid question",
                    validQuestionCount > 1
                )
                assertEquals(
                    "Competence ${comp.competenceId} in ${track.name} must count only valid parsed questions",
                    validQuestionCount,
                    loadedCount
                )
                assertEquals(
                    "Track total for ${track.name} must equal the count of parsed question objects",
                    trackQuestions.size,
                    MaritimeCompetenceRegistry.getTotalTrackQuestionCount(track)
                )
                assertEquals(
                    "Competence ${comp.competenceId} count must equal the parsed question objects",
                    loadedCount,
                    MaritimeCompetenceRegistry.getDynamicQuestionCount(comp.competenceId, track, isProUnlocked = true)
                )
            }
        }

        // Assert every question ID starts with its prefix
        ewQuestions.forEach { q ->
            assertTrue("EW question ID ${q.id} must start with EW-", q.id.startsWith("EW-"))
        }
        dmQuestions.forEach { q ->
            assertTrue("DM question ID ${q.id} must start with DM-", q.id.startsWith("DM-"))
        }
        emQuestions.forEach { q ->
            assertTrue("EM question ID ${q.id} must start with EM-", q.id.startsWith("EM-"))
        }

        // Assert every question ID in ALL_QUESTIONS is unique across all tracks
        val allIds = allQuestions.map { it.id }
        val distinctIds = allIds.distinct()
        assertEquals("Every question ID in ALL_QUESTIONS must be unique across all tracks", allIds.size, distinctIds.size)

        // Assert Deck and GMDSS before/after numbers are strictly identical
        val deckQuestions = allQuestions.filter { it.track == Track.DECK_OIC_NW }
        assertEquals("DECK_OIC_NW total must remain 6448", 6448, deckQuestions.size)
        val deckByComp = deckQuestions.groupBy { it.competenceId }
        val expectedDeck = mapOf(
            "C1" to 831,
            "C2" to 573,
            "C3" to 442,
            "C4" to 307,
            "C5" to 356,
            "C7" to 533,
            "C9" to 508,
            "C10" to 610,
            "C11" to 421,
            "C12" to 351,
            "C13" to 888,
            "C17" to 628
        )
        expectedDeck.forEach { (comp, expectedCount) ->
            assertEquals("DECK competence $comp count must match before count", expectedCount, deckByComp[comp]?.size ?: 0)
        }

        val gmdssQuestions = allQuestions.filter { it.track == Track.GMDSS_GOC }
        assertEquals("GMDSS_GOC total must remain 2260", 2260, gmdssQuestions.size)
        val gmdssByComp = gmdssQuestions.groupBy { it.competenceId }
        assertEquals("GMDSS C1 count must match before count", 1911, gmdssByComp["C1"]?.size ?: 0)
        assertEquals("GMDSS C2 count must match before count", 349, gmdssByComp["C2"]?.size ?: 0)
    }

    private fun countValidQuestionRows(file: File): Int {
        val text = com.nhyle.hyperionmaritime.data.util.AssetCrypto.decrypt(file.readBytes()).toString(Charsets.UTF_8)
            .replace("&#39;", "'")
            .replace("/'", "'")
            .replace("&amp;", "&")
            .replace("\uFFFD", "'")

        val rows = parseCsvText(text)
        val headerIndex = rows.indexOfFirst { row ->
            row.isNotEmpty() && row[0].trim().equals("No.", ignoreCase = true)
        }
        if (headerIndex < 0) return 0

        val seenNumbers = mutableSetOf<Int>()
        var count = 0
        for (row in rows.drop(headerIndex + 1)) {
            if (row.isEmpty() || row.all { it.isBlank() }) continue
            val number = row.getOrNull(0)?.trim()?.toIntOrNull() ?: continue
            if (number <= 0 || number in seenNumbers) continue

            val question = row.getOrNull(1)?.trim().orEmpty()
            if (question.isEmpty() || question.equals("Question", ignoreCase = true)) continue

            val options = (2..5).map { row.getOrNull(it)?.trim().orEmpty() }
            if (options.count { it.isNotEmpty() } < 2) continue
            if (options == listOf("A", "B", "C", "D")) continue

            val answerIndex = when (row.getOrNull(6)?.trim()?.uppercase()) {
                "A", "1" -> 0
                "B", "2" -> 1
                "C", "3" -> 2
                "D", "4" -> 3
                else -> -1
            }
            if (answerIndex !in 0..3 || options[answerIndex].isEmpty()) continue

            seenNumbers.add(number)
            count++
        }
        return count
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
}
