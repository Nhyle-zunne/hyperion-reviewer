package com.nhyle.hyperionmaritime

import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.RetiredQuestionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetiredC10QuestionsTest {
    private val track = Track.ENGINE_MGT
    private val retired = setOf("EM-C10-0182", "EM-C10-0373", "EM-C10-0406")

    @Test
    fun originalRecordsRemainAccessibleButNewSessionsExcludeThree() {
        val original = MaritimeQuestionBank.ALL_QUESTIONS.filter {
            it.track == track && it.competenceId == "C10"
        }
        assertEquals("The original encrypted question bank must not be shortened", 416, original.size)
        assertEquals(retired, original.filter(RetiredQuestionPolicy::isRetired).map { it.id }.toSet())

        val active = MaritimeQuestionBank.getQuestionsByCompetence("C10", track, isProUnlocked = true)
        assertEquals(413, active.size)
        assertEquals(413, active.map { it.id }.distinct().size)
        assertTrue(active.none(RetiredQuestionPolicy::isRetired))
        assertTrue(retired.none { id -> active.any { it.id == id } })

        val aroundDeletion = active.map { it.id }.toSet()
        for (n in listOf(181,183,372,374,405,407,416)) {
            assertTrue("Question $n must retain its original ID",
                "EM-C10-%04d".format(n) in aroundDeletion)
        }
    }

    @Test
    fun freeQuotasFunctionPoolsAndSearchDoNotReintroduceRetiredItems() {
        val free = MaritimeQuestionBank.getQuestionsByCompetence("C10", track, isProUnlocked = false)
        assertEquals("Free C10 quota remains unchanged", 166, free.size)
        assertTrue(free.none(RetiredQuestionPolicy::isRetired))
        val fullTrack = MaritimeQuestionBank.getQuestionsByTrack(track, isProUnlocked = true)
        assertTrue(fullTrack.none(RetiredQuestionPolicy::isRetired))
        val function = MaritimeQuestionBank.getQuestionsByFunction("F4", track, isProUnlocked = true)
        assertTrue(function.none(RetiredQuestionPolicy::isRetired))
        val standard = MaritimeQuestionBank.getNewTrackStandardReviewQuestions(track, isProUnlocked = true)
        assertTrue(standard.none(RetiredQuestionPolicy::isRetired))

        for (id in retired) {
            assertFalse(MaritimeQuestionBank.searchQuestions(id, track).any { it.id == id })
            // The canonical question is intentionally still resolvable for
            // saved-session/history, mistakes and existing bookmark records.
            assertTrue(MaritimeQuestionBank.ALL_QUESTIONS.any { it.id == id })
        }
    }
}
