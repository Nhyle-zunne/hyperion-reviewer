package com.nhyle.hyperionmaritime.data.repository

import android.content.Context
import com.nhyle.hyperionmaritime.data.model.Question

object MaritimeQuestionBankGmdss {

    private var cachedQuestions: List<Question>? = null

    fun getQuestions(context: Context? = null): List<Question> {
        cachedQuestions?.let { return it }
        val list = mutableListOf<Question>()

        // C1: 10 parts (1,911 total questions)
        for (part in 1..10) {
            val proQuestions = ProQuestionRepository.loadRawPartQuestions(context, "C1", part)
            list.addAll(proQuestions.map { it.toQuestion() })
        }

        // C2: 2 parts (349 total questions)
        for (part in 1..2) {
            val proQuestions = ProQuestionRepository.loadRawPartQuestions(context, "C2", part)
            list.addAll(proQuestions.map { it.toQuestion() })
        }

        if (list.isNotEmpty()) {
            cachedQuestions = list
        }
        return list
    }

    fun clearCache() {
        cachedQuestions = null
    }
}
