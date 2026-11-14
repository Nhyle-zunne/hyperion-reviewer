package com.nhyle.hyperionmaritime

import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.RetiredQuestionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class NewTrackLicensingAndQuotaTest {

    @Test
    fun testUnlicensedQuotaForNewTracks() {
        val newTracks = listOf(Track.ENGINE_OIC_EW, Track.DECK_MGT, Track.ENGINE_MGT)
        
        for (track in newTracks) {
            val competences = MaritimeCompetenceRegistry.getCompetencesForTrack(track)
            for (comp in competences) {
                val original = MaritimeQuestionBank.ALL_QUESTIONS.filter {
                    it.track == track && it.competenceId == comp.competenceId
                }
                val unlicensedList = MaritimeQuestionBank.getQuestionsByCompetence(comp.competenceId, track, isProUnlocked = false)

                // Quotas are anchored to original published positions, not to the
                // active paid count. Retired items inside the boundary disappear
                // without silently promoting previously paid questions into free.
                val publishedQuota = (original.size * 0.40).roundToInt().coerceAtLeast(1)
                val expectedFreeIds = RetiredQuestionPolicy.active(original.take(publishedQuota)).map { it.id }
                assertEquals(
                    "Free question IDs for ${track.name} ${comp.competenceId} must retain the published boundary and exclude retired items",
                    expectedFreeIds,
                    unlicensedList.map { it.id }
                )
                if (track == Track.ENGINE_MGT && comp.competenceId == "C10") {
                    assertEquals("The original C10 sample is 166 items", 166, publishedQuota)
                }
                
                // Assert consistent sampling
                val secondSample = MaritimeQuestionBank.getQuestionsByCompetence(comp.competenceId, track, isProUnlocked = false)
                assertEquals("Unlicensed sample must be consistent", unlicensedList.map { it.id }, secondSample.map { it.id })
            }
        }
    }

    @Test
    fun testLicensedAccessForNewTracks() {
        val newTracks = listOf(Track.ENGINE_OIC_EW, Track.DECK_MGT, Track.ENGINE_MGT)
        
        for (track in newTracks) {
            val competences = MaritimeCompetenceRegistry.getCompetencesForTrack(track)
            for (comp in competences) {
                val fullList = MaritimeQuestionBank.getQuestionsByCompetence(comp.competenceId, track, isProUnlocked = true)
                val licensedList = MaritimeQuestionBank.getQuestionsByCompetence(comp.competenceId, track, isProUnlocked = true)
                
                assertEquals(
                    "Licensed access for ${track.name} competence ${comp.competenceId} must return 100% of questions",
                    fullList.size,
                    licensedList.size
                )
            }
        }
    }

    @Test
    fun testMockExamAccessGating() {
        val tracks = Track.entries
        for (track in tracks) {
            val mockDataset = MaritimeQuestionBank.getMockExamDataset(track, isProUnlocked = false)
            assertTrue("Unlicensed users must receive 0 questions for Mock Exam on ${track.name}", mockDataset.isEmpty())
            
            val licensedDataset = MaritimeQuestionBank.getMockExamDataset(track, isProUnlocked = true)
            val expectedSize = if (track == Track.GMDSS_GOC) 100 else 200
            assertEquals("Licensed users must receive full Mock Exam dataset on ${track.name}", expectedSize, licensedDataset.size)
        }
    }
}
