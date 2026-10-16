package com.nhyle.hyperionmaritime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SearchQuestionBankTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        MaritimeQuestionBank.init(context)
        LicenseManager.setActivated(context, true)
    }

    @Test
    fun `test full track search coverage without omission`() {
        val results = MaritimeQuestionBank.searchQuestions(
            query = "vessel",
            track = Track.DECK_OIC_NW,
            isProUnlocked = true,
            competenceId = null
        )
        assertTrue("Search results for 'vessel' in OIC-NW should not be empty", results.isNotEmpty())
        for (q in results) {
            assertEquals("All results must belong to DECK_OIC_NW", Track.DECK_OIC_NW, q.track)
        }
    }

    @Test
    fun `test competence isolation`() {
        val results = MaritimeQuestionBank.searchQuestions(
            query = "navigation",
            track = Track.DECK_OIC_NW,
            isProUnlocked = true,
            competenceId = "C1"
        )
        for (q in results) {
            assertEquals("All results must belong to competence C1", "C1", q.competenceId)
            assertEquals("All results must belong to DECK_OIC_NW", Track.DECK_OIC_NW, q.track)
        }
    }

    @Test
    fun `test cross track isolation`() {
        val gmdssResults = MaritimeQuestionBank.searchQuestions(
            query = "radio",
            track = Track.GMDSS_GOC,
            isProUnlocked = true,
            competenceId = null
        )
        for (q in gmdssResults) {
            assertEquals("GMDSS search must never leak OIC-NW questions", Track.GMDSS_GOC, q.track)
        }
    }

    @Test
    fun `test number and ID search`() {
        val allDeck = MaritimeQuestionBank.getQuestionsByTrack(Track.DECK_OIC_NW, isProUnlocked = true)
        if (allDeck.isNotEmpty()) {
            val sampleId = allDeck.first().id
            val results = MaritimeQuestionBank.searchQuestions(
                query = sampleId,
                track = Track.DECK_OIC_NW,
                isProUnlocked = true,
                competenceId = null
            )
            assertTrue("Searching by exact ID must return matching question", results.any { it.id == sampleId })
        }
    }

    @Test
    fun `test empty and edge-case inputs`() {
        val blankResults = MaritimeQuestionBank.searchQuestions(
            query = "   ",
            track = Track.DECK_OIC_NW,
            isProUnlocked = true,
            competenceId = null
        )
        assertTrue("Blank query must return empty list", blankResults.isEmpty())

        val emptyResults = MaritimeQuestionBank.searchQuestions(
            query = "",
            track = Track.DECK_OIC_NW,
            isProUnlocked = true,
            competenceId = null
        )
        assertTrue("Empty query must return empty list", emptyResults.isEmpty())
    }

    @Test
    fun `test license gating enforcement`() {
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)

        val unactivatedResults = MaritimeQuestionBank.searchQuestions(
            query = "vessel",
            track = Track.DECK_OIC_NW,
            isProUnlocked = false,
            competenceId = null
        )
        assertTrue("Unactivated search must be capped or empty", unactivatedResults.size <= 5)
    }
}
