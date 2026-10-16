package com.nhyle.hyperionmaritime

import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import org.junit.Test

class CountBeforeTest {
    @Test
    fun countBeforeQuestions() {
        val all = MaritimeQuestionBank.ALL_QUESTIONS
        val deckQuestions = all.filter { it.track == Track.DECK_OIC_NW }
        val gmdssQuestions = all.filter { it.track == Track.GMDSS_GOC }

        println("=== BEFORE COUNTS ===")
        println("DECK_OIC_NW TOTAL: ${deckQuestions.size}")
        val deckByComp = deckQuestions.groupBy { it.competenceId }.toSortedMap()
        deckByComp.forEach { (comp, list) ->
            println("DECK $comp: ${list.size}")
        }

        println("GMDSS_GOC TOTAL: ${gmdssQuestions.size}")
        val gmdssByComp = gmdssQuestions.groupBy { it.competenceId }.toSortedMap()
        gmdssByComp.forEach { (comp, list) ->
            println("GMDSS $comp: ${list.size}")
        }
        println("=====================")
    }
}
