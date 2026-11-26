package com.nhyle.hyperionmaritime.data.repository

import com.nhyle.hyperionmaritime.data.local.MistakeEntry
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.QuestionMedia
import com.nhyle.hyperionmaritime.data.model.Track

internal object MistakePracticePool {
    fun resolve(
        entries: List<MistakeEntry>,
        bank: List<Question>,
        track: Track,
        competenceId: String? = null
    ): List<Question> {
        val bankById = bank.filter { it.track == track }.associateBy { it.id }
        return entries.mapNotNull { entry ->
            val question = bankById[entry.questionId] ?: entry.question
            val competence = entry.competenceId.ifBlank { question.competenceId }.ifBlank { "GENERAL" }
            if (question.id != entry.questionId || question.track != track ||
                (competenceId != null && !competenceId.equals(competence, ignoreCase = true) &&
                    !competenceId.equals(question.competenceId, ignoreCase = true)) ||
                RetiredQuestionPolicy.isRetired(question) ||
                !QuestionMedia.hasPrompt(question.questionText, question.imageAttachments) ||
                question.options.count { it.isNotBlank() } < 2 ||
                question.options.getOrNull(question.correctAnswerIndex).isNullOrBlank()
            ) {
                null
            } else {
                question
            }
        }.distinctBy { it.id }
    }
}
