package com.nhyle.hyperionmaritime

import com.nhyle.hyperionmaritime.data.local.MistakeEntry
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MistakePracticePool
import org.junit.Assert.assertEquals
import org.junit.Test

class MistakePracticePoolTest {
    private fun question(id: String, track: Track = Track.GMDSS_GOC, competence: String = "C1") =
        Question(id, track, "F1", competence, "Radio", "Question $id?", listOf("First", "Second"), 0, "", Difficulty.EASY)

    private fun saved(question: Question, track: String = question.track.name) =
        MistakeEntry(question.id, track, question.competenceId, question = question)

    @Test
    fun all37GmdssMistakesResolveWhenOnlyOneIdExistsInTheCurrentBank() {
        val savedQuestions = (1..37).map { question("gmdss-$it", competence = if (it == 37) "C2" else "C1") }
        val updated = savedQuestions.first().copy(explanation = "Current bank explanation")
        val entries = savedQuestions.map { saved(it, "GMDSS") }

        val pool = MistakePracticePool.resolve(entries, listOf(updated), Track.GMDSS_GOC)

        assertEquals(listOf(updated) + savedQuestions.drop(1), pool)
        assertEquals(listOf(savedQuestions.last()), MistakePracticePool.resolve(entries, listOf(updated), Track.GMDSS_GOC, "C2"))
    }

    @Test
    fun excludesRetiredUnavailableAndOtherTrackQuestions() {
        val retired = question("retired")
        val unavailable = question("unavailable").copy(options = emptyList())
        val otherTrack = question("other", Track.DECK_OIC_NW)
        val valid = question("valid")
        val entries = listOf(saved(retired), saved(unavailable), saved(otherTrack, "GMDSS"), saved(valid))

        assertEquals(
            listOf(valid),
            MistakePracticePool.resolve(entries, listOf(retired.copy(isRetired = true)), Track.GMDSS_GOC)
        )
    }
}
