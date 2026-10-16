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

        val ewQuestions = newQuestions.filter { it.track == Track.ENGINE_OIC_EW }
        val dmQuestions = newQuestions.filter { it.track == Track.DECK_MGT }
        val emQuestions = newQuestions.filter { it.track == Track.ENGINE_MGT }

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
                val relativePath = "questions/$folder/${prefix}_c$compNum.csv"
                val csvFile = File("app/src/main/assets/$relativePath").let {
                    if (it.exists()) it else File("src/main/assets/$relativePath")
                }

                assertTrue("CSV file $relativePath must exist", csvFile.exists())
                val csvDataRowCount = countCsvDataRows(csvFile)
                val loadedCount = questionsByComp[comp.competenceId]?.size ?: 0

                assertTrue(
                    "Competence ${comp.competenceId} in ${track.name} CSV row count ($csvDataRowCount) must be > 1",
                    csvDataRowCount > 1
                )
                assertEquals(
                    "Competence ${comp.competenceId} in ${track.name} loaded question count must equal CSV data row count",
                    csvDataRowCount,
                    loadedCount
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
        val allQuestions = MaritimeQuestionBank.ALL_QUESTIONS
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

    private fun countCsvDataRows(file: File): Int {
        val text = file.readText(Charsets.UTF_8)
            .replace("&#39;", "'")
            .replace("/'", "'")
            .replace("&amp;", "&")
            .replace("\uFFFD", "'")

        val rows = parseCsvText(text)
        if (rows.isEmpty()) return 0

        var headerIndex = -1
        for (idx in rows.indices) {
            val r = rows[idx]
            if (r.isNotEmpty() && r[0].trim().equals("No.", ignoreCase = true)) {
                headerIndex = idx
                break
            }
        }
        if (headerIndex == -1) return 0

        var count = 0
        for (rowIdx in (headerIndex + 1) until rows.size) {
            val row = rows[rowIdx]
            if (row.isEmpty() || row.all { it.isBlank() }) continue
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
